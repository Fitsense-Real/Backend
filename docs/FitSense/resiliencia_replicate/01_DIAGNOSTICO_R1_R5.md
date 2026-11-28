# 01 · Diagnóstico de R-1 a R-5

**Código auditado:** `generation` `3374213`.

**Abreviaturas:**
- `RTPG` = `planning/infrastructure/generation/ai/ReplicateTrainingPlanGenerator.java`;
- `PGP` = `planning/application/internal/commandservices/PlanGenerationPipeline.java`;
- `AWCS` = `…/AdaptedWeekCommandService.java`.

**Transporte compartido:** la adaptación usa el mismo transporte (`ReplicateSessionFixGenerator` llama a `RTPG.call`).

| | Método responsable | Protección que ya existía | Causa exacta | Reproducción (falla antes) |
|---|---|---|---|---|
| **R-1** | `RTPG.callMedido`: `jsonSupport.read(rawBody, ReplicateResponse.class)` fuera de todo `try` | Los errores HTTP (4xx/5xx, red) ya se convertían en `AiProviderUnavailableException`; un JSON del modelo mal formado, en `InvalidPlanDraftException` (reintento); un cuerpo de sondeo ilegible ya era fallo de consulta | Un **2xx con cuerpo que no es la respuesta de Replicate** lanzaba `IllegalStateException`, que ni `PGP` ni `AWCS` capturan. En generación: semana sin plan. En adaptación: sin evidencia V32 y sin P-03 | `ReplicateRespuestasInvalidasTest` (9 de 14 fallaban); `AdaptedWeekCommandServiceTest.errorInesperadoDeLaIaVaAlRespaldo` |
| **R-2** | `RTPG.awaitCompletion` y el final de `callMedido` | Sondeo con límite total de 150 s y resultado `TIMEOUT` medido. `ReplicateResponse.Urls` ya leía `cancel` | Al dejar de esperar (límite, sondeo fallido, interrupción) **no se cancelaba**: `urls.cancel` nunca se usaba y la predicción seguía en Replicate | `ReplicateCancelacionTest` (con la cancelación desactivada fallan 7 de 10) |
| **R-3** | Constructores de `RTPG`: `RestClient.builder()` sin fábrica | `Prefer: wait` acotado a 60 s y sondeo total de 150 s | Spring 7 elige `JdkClientHttpRequestFactory` **sin límite de conexión ni de lectura**: una conexión colgada en el POST o en un GET no tenía fin | `ReplicateTimeoutsHttpTest` (servidores locales que no responden o cortan a medias) |
| **R-4** | `AWCS.corregir` (`llamadas++` antes de llamar) y `AdaptationOutcome.respaldo(… llamadasIa)` | `RESPALDO_SIN_IA` para la IA apagada o el cortacircuitos abierto (0 llamadas) | Un fallo del proveedor en el intento 1 deja `ai_calls=1` y el resultado se derivaba solo de `ai_calls` → `RESPALDO_PROPUESTA_IA_INVALIDA` | `EstadoAdaptacionSinIaTest` y `AdaptedWeekCommandServiceTest` (4 fallaban) |
| **R-5** | `TrainingPlanController.generate` (`prepare` → `generate` → `persist`) | `prepare` y `persist` rechazan si ya hay plan activo (`PlanAlreadyExistsException`); el ciclo semanal ya tenía un bloqueo por persona (`CicloSemanalBloqueo`) | La petición **no usaba ese bloqueo**: dos simultáneas pasaban `prepare`, pagaban hasta 3 predicciones cada una y la segunda fallaba al persistir. Sin límite de frecuencia tras un fallo | `GeneracionInicialConcurrenteIT` (sin el bloqueo fallan 2 de 3) |

**Efectos comprobados de las correcciones sobre la generación y la adaptación:**
- Ninguna cambia qué se propone, cómo se valida ni qué se publica cuando la IA responde bien.
- Solo cambian los caminos de fallo, y siempre hacia el respaldo existente.
