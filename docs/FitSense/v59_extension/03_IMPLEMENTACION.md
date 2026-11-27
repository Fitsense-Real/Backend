# 03 · Implementación

**Rama:** `feature/v59-recovery-all-ai-flows`, desde `generation` (`77b9df3d`).

## 1. Archivos de producción

| Archivo | Cambio |
|---|---|
| `db/migration/V66__operaciones_ia_sincrona_y_ciclo_semanal.sql` | **Única migración.** Tipos `FIRST_PLAN_SYNC` y `WEEKLY_CYCLE`, `operation_key`, `measured_week_start`, restricciones `ck_pgo_clave` y `ck_pgo_ciclo_semana`, `ux_pgo_clave_en_curso`, `ux_pgo_una_activa_por_usuario` limitado a `FIRST_PLAN` e índice del barrido. Solo esquema, sin datos |
| `planning/application/internal/generation/SeguimientoOperacionesIa.java` | **Nuevo.** Reclamar o adoptar por clave, latido en un hilo propio, fencing, cierre de la operación, libro de predicciones y consultas del barrido (`ciclosHuerfanos`, `cerrarVencidas`) |
| `planning/application/internal/commandservices/GeneracionInicialService.java` | `generate` y `persist` dentro de una operación `FIRST_PLAN_SYNC`. El fencing va en la misma transacción que la publicación. `EnCursoEnOtroProceso` → `GeneracionEnCursoException` (409, el de siempre). El constructor de 3 argumentos se mantiene para las pruebas sin base |
| `planning/interfaces/acl/PlanningContextFacade.java` | `cicloConSeguimiento`, `ciclosSemanalesHuerfanos` y `cerrarOperacionesIaVencidas`. Orchestration sigue hablando solo con el facade |
| `orchestration/application/internal/WeeklyCycleService.java` | El ciclo, dentro del bloqueo de siempre, corre en su operación `WEEKLY_CYCLE`. `recuperarInterrumpidos()` relanza los ciclos huérfanos. `CicloEnCurso` → `EN_CURSO` |
| `orchestration/infrastructure/scheduling/WeeklyCycleRecoveryScheduler.java` | **Nuevo.** Barrido en un hilo propio, condicionado por `fitsense.ai-operations.recovery.enabled` (por omisión `true`) |
| `planning/application/internal/generation/PlanGenerationOperationService.java` | La generación asíncrona solo reclama, barre y consulta `FIRST_PLAN`, con el filtro `kind` en SQL y en el repositorio |
| `planning/infrastructure/persistence/jpa/repositories/PlanGenerationOperationRepository.java` | Consultas por tipo: `findByIdAndUserIdAndKind`, `findFirstByUserIdAndKindAndStatusIn` y `findFirstByUserIdAndKindOrderByCreatedAtDesc` |
| `planning/domain/model/aggregates/PlanGenerationOperation.java` | Constantes de los tipos y mapeo de las dos columnas nuevas |
| `notifications/application/DetectorEventosPush.java` | `PLAN_GENERATED` solo para operaciones `FIRST_PLAN`, como hasta ahora |
| `resources/application.properties` | `fitsense.ai-operations.*`: ventanas y barrido |
| `pom.xml` | `fitsense.ai-operations.recovery.enabled=false` en surefire y failsafe, igual que el worker V59 |

## 2. Compatibilidad con los flujos existentes

| Flujo | Qué cambia |
|---|---|
| Generación inicial asíncrona (`FIRST_PLAN`) | **Nada visible.** Sus consultas filtran ahora por tipo, para no ver las operaciones nuevas. Probado: V58→V59, V65→V66, `GeneracionAsincrona*IT` y `ReanudacionConReplicateSimuladoIT` |
| `POST /users/me/plan/generate` | **El contrato no cambia** (201 con el plan, 409 en curso, 429 por límite). Por dentro deja una operación `FIRST_PLAN_SYNC` y un libro |
| Mobile | `GET /api/v1/users/me/plan/generation/latest` y `/{id}` **solo ven `FIRST_PLAN`** (prueba `aislamientoDeLaApiAsincrona`). Mobile no se toca |
| Notificaciones push | Sin cambios: `PLAN_GENERATED` sigue saliendo solo de `FIRST_PLAN`; las semanales salen de `plan_adaptation_attempts` |
| Eliminación de cuentas | Sin cambios: cancela todas las operaciones en curso del usuario, también las nuevas, y el fencing las detiene |
| Cierre semanal | El mismo ciclo, el mismo bloqueo y la misma idempotencia. Además, deja la operación, el libro y se relanza si se interrumpe |
| Validadores, O20, V65, progresión, reincorporación, políticas, catálogo, adherencia, prompts y modelo | **Sin cambios.** El diff de producción no los toca |
| `AI` / `RULE_ENGINE` | Sin cambios de semántica |

## 3. Observabilidad

| Línea de registro | Cuándo se escribe |
|---|---|
| `OPERACION_IA_ABIERTA` | Una operación nueva |
| `OPERACION_IA_REANUDADA` | Una adopción tras un reinicio |
| `OPERACION_IA_CARRERA` | Dos ejecuciones abren la misma clave a la vez |
| `OPERACION_IA_CERRADA` | Una operación cerrada, con su estado, plan y motivo |
| `OPERACION_IA_NO_CERRADA` | El cierre falla porque la operación ya no era de esta ejecución |
| `OPERACION_IA_SIN_CERRAR` | Apagado, o una operación que ya no es propia |
| `OPERACION_IA_PREDICCIONES` | El recuento del libro por operación: iniciadas, reutilizadas, completadas, fallidas, canceladas, vencidas, no confirmadas y descartadas |
| `OPERACION_IA_VENCIDAS` | Operaciones cerradas por el barrido |
| `CICLO_SEMANAL_RECUPERACION` | Un ciclo relanzado |

Además siguen las de siempre: `REPLICATE_TIMING`, `REPLICATE_CONSUMO`, `REPLICATE_NO_CONFIRMADA` y `REPLICATE_REANUDADA`.
