# 02 · Diseño de la recuperación

## 1. Principio: reutilizar V59, no duplicarlo

Los caminos nuevos usan **la misma tabla de operaciones**, **el mismo propietario con lease y latido**, **el mismo fencing** y **el mismo libro de predicciones** que la generación inicial asíncrona.

Lo único nuevo es la **identidad lógica** de la operación. Una petición síncrona o un cierre semanal no tienen una operación que la app consulte, así que su identidad es la de lo que se genera.

| Tipo (`kind`) | Camino | Clave lógica (`operation_key`) |
|---|---|---|
| `FIRST_PLAN` (V58, sin cambios) | Generación inicial asíncrona | — (la del UUID que ve la app) |
| `FIRST_PLAN_SYNC` (nuevo) | `POST /users/me/plan/generate` | `FIRST_PLAN_SYNC:u=<usuario>:semana=<lunes>` |
| `WEEKLY_CYCLE` (nuevo) | Cierre semanal: generación **o** adaptación de la semana nueva | `WEEKLY_CYCLE:u=<usuario>:medida=<lunes medido>:semana=<lunes nuevo>` |

**¿Por qué un solo tipo para el cierre semanal?**
- Un ciclo hace generación **o** adaptación, nunca las dos.
- Relanzarlo exige repetir el ciclo entero: cerrar y medir son idempotentes, y decidir y comprobar «ya publicado» también.
- La adaptación solo se ejecuta dentro del ciclo (o en su relanzamiento manual).
- Así cubre los caminos 3, 4 y 6 de `01`.

## 2. Ciclo de vida de una operación nueva

```
            reclamar(clave)
  (nada) ─────────────────────► PROCESSING (lease de ESTE proceso, latido cada lease/3)
                                    │
   ┌────────────────────────────────┼──────────────────────────────┬───────────────────────────┐
   │ termina con plan               │ termina sin plan             │ excepcion (no fencing)    │ el proceso muere
   ▼                                ▼                              ▼                           ▼
COMPLETED (plan_id)          NO_FACTIBLE (SIN_PLAN)       FAILED/NO_FACTIBLE (clasificador)   PROCESSING sin latido
                                                                                               (lease vencido = huerfana)
                                                                                                    │
            ┌───────────────────────────────────────────────────────────────────────────────────────┤
            │ misma clave otra vez (la persona repite; el barrido relanza el ciclo)                 │ barrido (cerrarVencidas)
            ▼                                                                                       ▼
   ADOPCION atomica: lease nuevo, executions+1, MISMA operacion y MISMO libro        FAILED EJECUCION_INTERRUMPIDA (sin adopciones)
   (si executions < max-executions)                                                  FAILED TIEMPO_EXCEDIDO (fuera de ventana)
```

**Estados:** son los de V58/V59 (`PENDING`, `PROCESSING`, `COMPLETED`, `FAILED`, `NO_FACTIBLE`). **No hace falta ninguno nuevo.**

| Lo que pide la orden (§3.3) | Cómo se representa |
|---|---|
| Operación registrada / envío iniciado | Operación `PROCESSING` y fila del libro `SOLICITANDO` |
| Predicción confirmada con id / en ejecución | Fila `ENVIADA`, con `replicate_prediction_id` |
| Resultado recibido | Fila `SUCCEEDED`, con su `output` |
| Resultado validado / operación completada | Operación `COMPLETED` con `plan_id`. Solo se publica lo que pasa los validadores |
| Error definitivo | Operación `FAILED` o `NO_FACTIBLE` con `reason_code`. Filas `FAILED`, `ERROR_ENVIO` o `CANCELED` |
| Predicción no confirmada | Fila `NO_CONFIRMADA`: nunca se reenvía |
| Recuperación pendiente | Operación `PROCESSING` con el lease vencido (huérfana) |
| Respaldo `RULE_ENGINE` | El plan publicado lleva `generation_source = RULE_ENGINE`. La operación termina `COMPLETED` igual: hay plan, y la semántica `AI`/`RULE_ENGINE` no cambia |

## 3. Persistencia (V66, la única migración)

| Cambio | Para qué |
|---|---|
| `ck_pgo_kind` admite `FIRST_PLAN_SYNC` y `WEEKLY_CYCLE` | Los dos tipos nuevos |
| Columnas `operation_key` y `measured_week_start` | La identidad lógica, y la semana medida para relanzar el ciclo |
| `ck_pgo_clave` | Los tipos nuevos siempre llevan clave; `FIRST_PLAN` nunca |
| `ck_pgo_ciclo_semana` | Un `WEEKLY_CYCLE` siempre sabe qué semana mide |
| `ux_pgo_clave_en_curso` (único parcial) | **Una sola operación en curso por clave** |
| `ux_pgo_una_activa_por_usuario` limitado a `FIRST_PLAN` | Una petición síncrona o un ciclo no ocupan el hueco de la operación que consulta la app |
| `ix_pgo_huerfanas_v66` | El barrido |

El libro (`plan_generation_predictions`) **no cambia**: sigue apuntando a la operación por `operation_id`.

## 4. Transacciones

**Ninguna transacción abierta mientras se espera a Replicate**, igual que en V59:
- el reclamo, el latido, el cierre y el libro son sentencias con autocommit;
- la publicación síncrona abre **una transacción corta** en la que primero se comprueba el propietario (`SELECT … FOR UPDATE`) y después se persiste el plan;
- el ciclo semanal publica con sus propias transacciones cortas (`publishAdapted`, `persist`), como antes.

## 5. Concurrencia

| Riesgo | Protección |
|---|---|
| Dos ejecuciones simultáneas de la misma persona | El bloqueo consultivo de siempre (`CicloSemanalBloqueo`) |
| Dos instancias con la misma clave (por ejemplo, la conexión del bloqueo se cayó) | `ux_pgo_clave_en_curso`: una sola en curso. La otra recibe `EnCursoEnOtroProceso`, y se responde 409 o `EN_CURSO` |
| Dos instancias adoptando la misma huérfana | La adopción es **un solo `UPDATE … WHERE lease vencido RETURNING`**: gana una |
| Un proceso que perdió el lease y sigue vivo (zombi) | **Fencing:** antes de cada predicción (el libro llama a la guarda) y dentro de la transacción de la publicación síncrona. El cierre de la operación exige ser el propietario |
| Una publicación del zombi en el ciclo | El ciclo no tiene fencing en su publicación, pero `publishAdapted` y «ciclo repetido» impiden la doble publicación (probado en V59-02/09) |
| Un `Error` (no `RuntimeException`) que sale del ciclo | Cierra también la operación. Si la operación es de este mismo proceso pero ya no la ejecuta nadie, se adopta |

## 6. Idempotencia

- **Predicciones:** el libro, por operación e intento, con la huella de la petición.
  - Misma petición: se reanuda o se reutiliza.
  - Petición distinta (el contexto cambió): la fila antigua queda `DESCARTADA` y **no se aplica** (V59-10).
- **Publicación:**
  - en la vía síncrona, `prepare` y `persist` rechazan un segundo primer plan, y el fencing se comprueba en la misma transacción;
  - en el ciclo, «ya publicado» y `publishAdapted`.
- **Operaciones completadas:** una nueva ejecución de la misma clave abre otra operación; no reprocesa la terminada.
  - En el ciclo, «ya publicado» devuelve el plan existente sin llamar a la IA.
  - En la síncrona, `prepare` rechaza.

## 7. Recuperación tras un reinicio

| Camino | Quién reanuda | Cómo |
|---|---|---|
| Síncrono | **La siguiente petición** de la misma persona y semana (la app reintenta) | Adopta la huérfana y su libro: sigue la predicción por su id o reutiliza su salida. El contrato HTTP no cambia |
| Ciclo semanal | El **barrido** `WeeklyCycleRecoveryScheduler`, en un hilo propio, cada 2 min | `recuperarInterrumpidos()` cierra las vencidas y relanza cada ciclo huérfano con `WeeklyCycleService.ejecutar`: el mismo bloqueo, la misma comprobación de «ya publicado» y la adopción con el libro |

**Límites, para no esperar nunca para siempre:**
- `max-executions` (3) adopciones; después, `EJECUCION_INTERRUMPIDA`;
- ventana del ciclo: `weekly-recovery-hours` (24 h). Después, `TIEMPO_EXCEDIDO`: no se publica una semana tarde;
- ventana de una huérfana síncrona: `sync-orphan-seconds` (30 min).

**Por qué la vía síncrona no se reanuda en segundo plano:**
- El contrato del endpoint es síncrono: la persona espera la respuesta.
- Publicar en segundo plano un plan que nadie pidió de nuevo cambiaría ese contrato, porque la siguiente petición recibiría 409 «ya existe».
- Por eso se reanuda cuando la persona repite. Si no repite, la operación se cierra a los 30 min y su libro queda. **Revisión de integración (27/11/2026):** una petición nueva es otra operación y pide su propia predicción; ya no se adopta la de otra operación por persona y huella (el contenido no prueba identidad). Ver `docs/FitSense/cierre_preproduccion/11_INTEGRACION_FINAL_Y_MERGES.md`.

## 8. Predicciones sin identificador (orden §4)

- Una fila `SOLICITANDO` o `NO_CONFIRMADA` **nunca se reenvía**, **nunca se busca por contenido** y **nunca se presenta como éxito de IA**.
- Se publica el respaldo permitido, según la política de siempre.
- Lo incierto es terminal en el libro, así que no hay espera infinita.
- Queda en `OPERACION_IA_PREDICCIONES … no_confirmadas=N`, en `REPLICATE_NO_CONFIRMADA` y en la fila del libro.
