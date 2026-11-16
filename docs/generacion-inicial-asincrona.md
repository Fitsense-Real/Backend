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
- **Reinicios (V59):** el worker (`PlanGenerationWorker`, hilos propios, por defecto 2 en paralelo)
  reclama con `FOR UPDATE SKIP LOCKED` y toma un **lease con propietario** (`lease_owner`,
  `lease_expires_at`, 90 s). Mientras ejecuta, renueva su lease cada 20 s desde un hilo propio (no el
  planificador compartido de Spring). Una PROCESSING con el **lease vencido** quedó huérfana: vuelve a
  la cola hasta `max-executions` (3) y después se cierra como `FAILED/EJECUCION_INTERRUMPIDA`
  reintentable. Publicar (en la misma transacción que la comprobación de propiedad, con `FOR UPDATE`), cerrar,
  latir y empezar cada intento de IA exigen ser el propietario. El barrido corre cada `recover-ms` (30 s). Al reanudarse, el pipeline lee
  las predicciones de Replicate ya pedidas en `plan_generation_predictions` en lugar de pagarlas otra
  vez. Detalle en `docs/FitSense/generacion_asincrona/`.
- **Apagado ordenado (despliegue, SIGTERM):** el worker deja de reclamar, interrumpe sus
  generaciones sin registrarlas como fallidas y las devuelve a PENDING sin gastar un intento; la
  siguiente instancia las retoma al arrancar. Si el proceso muere sin apagado ordenado (SIGKILL,
  caída), la operación deja de latir y el barrido la recupera.
- **Tiempo máximo (V59):** una PROCESSING de más de 30 min desde su **primera** ejecución
  (`max-processing-seconds`, comprobado al arrancar contra el peor caso del pipeline, ~1.113 s), o una
  PENDING de más de 30 min, se cierra como
  `FAILED/TIEMPO_EXCEDIDO` reintentable. Si la generación seguía viva, antes de publicar comprueba
  que la operación siga PROCESSING y descarta su resultado. La app nunca espera para siempre.

## Configuración

| Propiedad | Por defecto |
|---|---|
| `fitsense.plan-generation.worker.enabled` | `true` |
| `fitsense.plan-generation.poll-ms` | `2000` |
| `fitsense.plan-generation.max-concurrent` | `2` |
| `fitsense.plan-generation.heartbeat-ms` | `20000` |
| `fitsense.plan-generation.lease-seconds` | `90` (V59; sin renovar durante este tiempo = huérfana; sustituye a `stale-seconds`) |
| `fitsense.plan-generation.max-processing-seconds` | `1800` (V59; antes 600, menor que el peor caso con IA de ~1.113 s) |
| `fitsense.plan-generation.max-queued-seconds` | `1800` |
| `fitsense.plan-generation.max-executions` | `3` (V59; antes 2) |
| `fitsense.plan-generation.reuse-window-hours` | `24` (V59; reutilizar una predicción de otra operación del mismo usuario y la misma petición) |
| `fitsense.plan-generation.recover-ms` | `30000` (V59; barrido de huérfanas y vencidas; el reclamo sigue cada `poll-ms`) |

Las pruebas (surefire y failsafe) apagan el worker: cada contexto de Spring en caché tendría el suyo
sondeando cada 2 s una base inexistente o ya detenida (eso agotaba el CI a los 45 min).

## Pruebas

- Unitarias: `GenerationFailureClassifierTest`, `PlanGenerationOperationTest`, `PlanGenerationWorkerTest`.
- Integración (`GeneracionAsincronaIT`, IA apagada, base migrada V1..V59). V59 añade
  `GeneracionAsincronaResilienciaIT` y `ReanudacionConReplicateSimuladoIT` (Testcontainers, corren en el CI):

```
./mvnw -Pintegration verify -Dit.test=GeneracionAsincronaIT \
  -Dfitsense.pruebas.url=jdbc:postgresql://localhost:5432/fitsense_pruebas \
  -Dfitsense.pruebas.usuario=… -Dfitsense.pruebas.clave=…
```
