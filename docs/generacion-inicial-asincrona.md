# Generación inicial asíncrona (V58)

La app pide la primera rutina y sigue usándose (recorrido de bienvenida) mientras el
backend la genera. La generación es la misma de siempre: `prepare` → `generate`
(IA y motor de reglas, todos los validadores) → `persist` (solo publica un plan válido).

## Contrato

Todas las rutas requieren JWT de usuario. Respuesta común (`PlanGenerationOperationResource`):

```json
{
  "operationId": "5b0c…",
  "status": "PENDING | PROCESSING | COMPLETED | FAILED | NO_FACTIBLE",
  "terminal": false,
  "weekStartDate": "2026-11-16",
  "planId": null,
  "reasonCode": null,
  "message": null,
  "retryable": false,
  "createdAt": "…", "startedAt": null, "finishedAt": null,
  "pollAfterSeconds": 3
}
```

| Método y ruta | Respuesta |
|---|---|
| `POST /api/v1/users/me/plan/generation` | **202** operación nueva PENDING. **200** si ya había una PENDING/PROCESSING (la misma) o si el plan ya existe (COMPLETED con `planId`). **422** sin perfil o usuario no elegible. |
| `GET /api/v1/users/me/plan/generation/{operationId}` | **200** estado. **404** si no existe o es de otro usuario. **400** si el id no es un UUID. |
| `GET /api/v1/users/me/plan/generation/latest` | **200** última operación del usuario (recuperación si la app perdió el id). **404** si nunca pidió una. |

`POST /api/v1/users/me/plan/generate` (síncrono) se mantiene sin cambios.

## Estados

| Estado | Significado | `reasonCode` | `retryable` |
|---|---|---|---|
| PENDING | Registrada, esperando al worker | — | — |
| PROCESSING | Un worker la está generando | — | — |
| COMPLETED | Plan publicado (`planId` siempre presente) | — | false |
| NO_FACTIBLE | No existe una semana válida con las condiciones del participante. No se relajan restricciones | `SIN_SEMANA_GENERABLE`, `SEMANA_NO_VALIDA`, `DISPONIBILIDAD_NO_CUMPLIBLE` (`NoFactibleReason`) | false |
| FAILED | Error técnico o regla que impide generar | `ERROR_TECNICO`, `EJECUCION_INTERRUMPIDA`, `TIEMPO_EXCEDIDO` (true); `PERFIL_INCOMPLETO`, `NO_GENERABLE` (false) | según código |

`message` es texto para el participante; el detalle técnico solo va al log.

## Garantías

- **Idempotencia:** índice único parcial `ux_pgo_una_activa_por_usuario` (una PENDING/PROCESSING
  por usuario). Doble toque, reintentos de red o peticiones simultáneas devuelven la misma
  operación: una sola llamada a la IA. Un `POST` después de COMPLETED devuelve la misma operación.
- **Reintento seguro:** después de FAILED, un nuevo `POST` crea otra operación; NO_FACTIBLE no se
  reintenta solo.
- **Propiedad:** toda consulta filtra por el usuario del JWT.
- **Sin planes a medias:** el plan se publica en la transacción corta de `persist`, como siempre;
  ninguna transacción queda abierta durante la llamada a la IA.
- **Reinicios:** el worker (`PlanGenerationWorker`, pool propio, por defecto 2 en paralelo) reclama
  con `FOR UPDATE SKIP LOCKED` y, mientras ejecuta, **late** cada 20 s (`updated_at`). Una
  PROCESSING sin latido durante 3 min quedó huérfana (el proceso murió o se reinició): se vuelve a
  poner en cola una vez; la segunda vez se cierra como `FAILED/EJECUCION_INTERRUMPIDA`
  reintentable. Una generación lenta pero viva sigue latiendo y nunca se reencola ni se duplica.
- **Tiempo máximo:** una PROCESSING de más de 10 min, o una PENDING de más de 30 min, se cierra como
  `FAILED/TIEMPO_EXCEDIDO` reintentable. Si la generación seguía viva, antes de publicar comprueba
  que la operación siga PROCESSING y descarta su resultado. La app nunca espera para siempre.

## Configuración

| Propiedad | Por defecto |
|---|---|
| `fitsense.plan-generation.worker.enabled` | `true` |
| `fitsense.plan-generation.poll-ms` | `2000` |
| `fitsense.plan-generation.max-concurrent` | `2` |
| `fitsense.plan-generation.heartbeat-ms` | `20000` |
| `fitsense.plan-generation.stale-seconds` | `180` (sin latido durante este tiempo = huérfana) |
| `fitsense.plan-generation.max-processing-seconds` | `600` (mayor que el peor caso del pipeline con IA, 150 s por intento) |
| `fitsense.plan-generation.max-queued-seconds` | `1800` |
| `fitsense.plan-generation.max-executions` | `2` |

Las pruebas (surefire y failsafe) apagan el worker: cada contexto de Spring en caché tendría el suyo
sondeando cada 2 s una base inexistente o ya detenida (eso agotaba el CI a los 45 min).

## Pruebas

- Unitarias: `GenerationFailureClassifierTest`, `PlanGenerationOperationTest`, `PlanGenerationWorkerTest`.
- Integración (`GeneracionAsincronaIT`, IA apagada, base migrada V1..V58):

```
./mvnw -Pintegration verify -Dit.test=GeneracionAsincronaIT \
  -Dfitsense.pruebas.url=jdbc:postgresql://localhost:5432/fitsense_pruebas \
  -Dfitsense.pruebas.usuario=… -Dfitsense.pruebas.clave=…
```
