# Validación del lease, el latido y la propiedad de las operaciones

## 1. Diseño

| Pieza | Comportamiento |
|---|---|
| **Reclamo** | Un solo `UPDATE … WHERE operation_id = (SELECT … FOR UPDATE SKIP LOCKED LIMIT 1)`. Fija `lease_owner = <host>-<uuid8>` (uno por proceso), `lease_expires_at = now + 90 s` y `first_started_at` (solo la primera vez). |
| **Latido** | Hilo propio `generacion-inicial-latido`, cada 20 s. Renueva `lease_expires_at` **solo** de las operaciones cuyo `lease_owner` es este proceso. |
| **Ciclo** | Hilo propio `generacion-inicial-ciclo`: reclama cada `poll-ms` (2 s) mientras haya hueco (`max-concurrent=2`) y barre cada `recover-ms` (30 s). En #71 barría en cada ciclo. |
| **Huérfana** | `PROCESSING` con el lease **vencido o nulo**. El nulo solo puede ser una fila anterior a V59; ck_pgo_lease lo impide en cualquier escritura nueva. |
| **Fencing** | Al empezar cada intento de IA, antes de leer el libro, reanudar o enviar, se comprueba que la operación sigue `PROCESSING` y es de este proceso. Si no, el worker no envía, no sondea y no toca el libro. |
| **Publicación** | La comprobación de propiedad (`SELECT … FOR UPDATE`) y `persist` van en la **misma transacción** corta (`PipelineFirstPlanGenerationRunner`). Nadie puede quitarle el lease al worker entre la comprobación y el commit. |
| **Cierre** | `finish` solo aplica si la operación sigue `PROCESSING` y es del propietario. Toda transición suelta el lease (ck_pgo_lease). |
| **Apagado (SIGTERM)** | Devuelve a la cola solo **sus** operaciones, sin gastar una ejecución. |

## 2. Problema 2 de la orden: el latido bloqueado

En #71 el latido era un `@Scheduled`. Spring Boot tiene **un solo hilo** para todas las tareas `@Scheduled` (no hay
`spring.task.scheduling.*` configurado) y lo comparte con:

- `WeeklyCycleScheduler`, el cierre semanal, que llama a la IA usuario por usuario durante minutos;
- `OverdueWorkoutScheduler`;
- `StaleSessionScheduler`.

**Prueba R02** (`GeneracionAsincronaResilienciaIT.elCierreSemanalNoBloqueaElLatido`):

1. Arranca el worker real con sus hilos, escalado a 50 ms de ciclo y 100 ms de latido.
2. Reclama una operación y la deja generando.
3. Ocupa 3 s el único hilo del planificador de Spring con un «cierre semanal».
4. Durante el bloqueo:
   - el `lease_expires_at` de la operación avanza: el latido propio sigue;
   - una tarea programada en el planificador de Spring, como el latido de #71, **no** se ejecuta;
   - se comprueba que ese planificador tiene un solo hilo.
5. El barrido no la reencola. Termina `COMPLETED` con 1 plan y 1 ejecución.

Resultado: **PASS**.

## 3. Problema 3 de la orden: la duplicación

| Garantía | Mecanismo | Prueba | Resultado |
|---|---|---|---|
| Dos workers no ejecutan a la vez la misma operación | `SKIP LOCKED`: de 4 reclamos simultáneos, uno gana | `dosWorkersUnaOperacion` | PASS |
| Una operación activa no vuelve a la cola por retrasos del planificador | Lease, no `updated_at` | `latidoAtrasadoConLeaseVigenteNoReencola`, R02 | PASS |
| Un worker antiguo no publica tras perder la propiedad | Fencing en la transacción de publicación | `sinLeaseNoPublica`, `dosInstanciasNoDuplicanElTrabajo` | PASS |
| Un worker antiguo no paga más ni toca el libro | Fencing al empezar cada intento | `sinPropiedadNoSeEnvia`, `sinPropiedadNoSeTocaElLibro`, `dosInstanciasNoDuplicanElTrabajo` (0 `NO_CONFIRMADA`) | PASS |
| Una operación completada no se vuelve a ejecutar | `execute` solo actúa sobre `PROCESSING` | `caminoNormalYRepeticiones` (1 ejecución del pipeline tras 3 llamadas a `execute`) | PASS |
| No hay planes duplicados | `ux_plan_active (user_id, week_start_date)` más fencing y `PlanAlreadyExistsException` | todas las IT: `count(weekly_training_plans) = 1` | PASS |
| Un reintento HTTP recupera la operación | `ux_pgo_una_activa_por_usuario`; un `POST` tras `COMPLETED` devuelve la misma | `solicitudesSimultaneas` (8 → 1), `appCerradaYReabierta`, `variosUsuariosConElWorkerReal` | PASS |
| El apagado no toca lo ajeno | Filtro por `lease_owner` | `apagadoSoloLasPropias` | PASS |

**Escenario de dos instancias** (`ReanudacionConReplicateSimuladoIT.dosInstanciasNoDuplicanElTrabajo`):

1. La instancia A sondea la predicción p1.
2. Su lease vence (pausa simulada).
3. La instancia B barre, reclama y sigue **la misma** p1 por su id.
4. Se liberan las dos.
5. B hace los intentos 2 y 3 y publica. A no publica, no paga y no toca el libro.

Resultado: **3 POST en total, 1 plan**, lease suelto y 0 filas `NO_CONFIRMADA`.

## 4. Carga y base de datos (R20)

| Aspecto | #71 | V59 |
|---|---|---|
| Barrido (3 `UPDATE`) | En cada ciclo: cada 2 s por instancia | Cada 30 s (`recover-ms`). Prueba `PlanGenerationWorkerTest.elBarridoNoCorreEnCadaCiclo`: 20 ciclos, 1 barrido. |
| Reclamo | Cada 2 s si hay hueco | Igual: una sentencia indexada (`ix_pgo_pendientes`) |
| Latido | Un `UPDATE` cada 20 s por operación en curso | Igual, filtrado por propietario |
| Hilos | Pool de generación más el planificador compartido | Pool (`max-concurrent`), un hilo de ciclo y uno de latido. Nada crece sin límite. |
| Transacciones | Ninguna abierta durante la IA | Igual. El libro va en autocommit; la publicación es corta. |

**Prueba R15/R20** (`variosUsuariosConElWorkerReal`):

- 10 usuarios piden a la vez, con doble toque (20 peticiones), y el worker real los procesa.
- Resultado: 10 operaciones (una por usuario), 10 planes, 1 ejecución cada una y como máximo 2 en paralelo.
- Terminan en unos 1,6 s (con 150 ms simulados por generación).

**CI.** El worker sigue apagado en surefire y failsafe (`fitsense.plan-generation.worker.enabled=false`). Las pruebas
que lo arrancan lo paran en `finally`. El problema de #71, el CI cancelado a los 45 min, no se reproduce: el CI de
#72 tardó unos 26 min, como la regresión local (`REGRESION_FINAL.md`).
