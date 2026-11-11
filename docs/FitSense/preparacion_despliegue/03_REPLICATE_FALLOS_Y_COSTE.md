# 03 · Replicate: comportamiento ante fallos y control del gasto

**Alcance:**
- Análisis estático del código de `generation`.
- No se ha usado ninguna credencial ni hecho ninguna llamada a Replicate (**USD 0**).

**Abreviaturas:**
- `RTPG` = `planning/infrastructure/generation/ai/ReplicateTrainingPlanGenerator.java`;
- `PGP` = `planning/application/internal/commandservices/PlanGenerationPipeline.java`;
- `AWCS` = `…/AdaptedWeekCommandService.java`.

**Transporte compartido:** la adaptación usa el mismo transporte que la generación (`ReplicateSessionFixGenerator` llama a `RTPG.call`).

## 1. Comportamiento por situación

| Situación | Generación inicial | Adaptación semanal |
|---|---|---|
| **Sin token** | `isUsable()` es falso y **no se llama** al proveedor. Motor de reglas directamente (RULE_ENGINE) | No se llama. Respaldo determinista: conserva la semana, o SUST-SEG-1 si hay dolor; si no es posible, NO_FACTIBLE y regeneración segura P-03 sin IA. Evidencia `RESPALDO_SIN_IA`, `ai_calls=0` |
| **Timeout** (la predicción sigue en curso tras `Prefer: wait` de 60 s más 150 s de sondeo cada 3 s) | `AiProviderUnavailableException`. Cuenta para el *circuit breaker*, no se reintenta y pasa al motor de reglas | Igual, y pasa al respaldo determinista |
| **Error de comunicación** (red, 4xx incluidos 401 y 429, 5xx) | `AiProviderUnavailableException` al hacer el POST. No se reintenta y cuenta para el *circuit breaker*. Motor de reglas | Igual, con respaldo determinista |
| **Estado final `failed`/`canceled`** | Igual que un error de comunicación | Igual |
| **Salida del modelo inválida** (JSON del plan ilegible, forma o versión de esquema incorrecta) | Error del modelo: reinicia el contador del *circuit breaker* y **reintenta con realimentación** (hasta 3 intentos) | Igual (hasta 3 intentos, una predicción por intento) |
| **Propuesta válida pero rechazada** (V1–V27; en adaptación también V8C) | Reintento con los motivos acumulados y la propuesta rechazada. Tras 3: motor de reglas; con REV2, hasta 4 variantes deterministas; si todo falla, semana sin plan (`PlanGenerationFailedException`) | Reintento con los motivos. Tras 3: respaldo determinista (RESPALDO) o NO_FACTIBLE con P-03 si es por dolor |
| **Circuit breaker abierto** (3 fallos de proveedor seguidos; 10 min) | No se llama. Motor de reglas, con aviso `IA_NO_INVOCADA` | No se llama. Respaldo determinista |

- **El circuit breaker no se comparte.** Generación y adaptación tienen cada una el suyo, en memoria y por instancia. Se reinicia al reiniciar la aplicación.

## 2. Hallazgos de esta revisión (no corregidos; fuera del alcance de la orden)

| Id | Hallazgo | Riesgo | Impacto |
|---|---|---|---|
| R-1 | **Sobre de Replicate ilegible en el POST.** `RTPG` lee la respuesta con `jsonSupport.read` fuera de cualquier `try` (≈ l. 204), y un cuerpo no JSON con código 2xx lanza `IllegalStateException`. Ni `PGP` ni `AWCS` la capturan: **no hay reintento ni respaldo**. En la generación la semana queda sin plan; en la adaptación no se escribe la evidencia y no se ejecuta P-03. Un cuerpo ilegible en el sondeo sí se trata como fallo de proveedor | Bajo: exige un 2xx con cuerpo no JSON, por ejemplo una página de un proxy | Medio: un usuario sin plan esa semana |
| R-2 | **Las predicciones con timeout no se cancelan.** `urls.cancel` se lee pero no se usa, así que la predicción sigue ejecutándose en Replicate y **puede facturarse** aunque el backend ya haya pasado al respaldo | Medio | Coste |
| R-3 | **Sin timeouts del cliente HTTP.** `RestClient.builder().build()` sin fábrica configurada; una conexión colgada en el POST o en un GET de sondeo no queda limitada por la aplicación | Bajo | Disponibilidad |
| R-4 | **Evidencia de adaptación engañosa en una caída.** El contador de llamadas se incrementa antes de llamar, así que un fallo de proveedor en el intento 1 queda como `ai_calls=1` y `RESPALDO_PROPUESTA_IA_INVALIDA` en lugar de `RESPALDO_SIN_IA` | Bajo | Trazabilidad |
| R-5 | **Sin límite de peticiones en `POST /users/me/plan/generate`.** Se puede repetir mientras no hay plan, y dos peticiones simultáneas pueden gastar hasta 3 llamadas cada una antes de que la persistencia detecte el duplicado | Bajo/medio | Coste |

Corregir R-1 a R-5 son cambios de código que esta orden no autoriza. Quedan propuestos para una orden posterior.

## 3. Cuántas llamadas puede hacer

- **Ciclo semanal:** cada usuario recibe o una adaptación o una generación por semana. Como máximo, **3 predicciones por usuario y semana**, y solo si las respuestas llegan pero se rechazan. Un fallo de proveedor corta tras 1.
- **Progresión, recuperación, reincorporación y P-03:** 0 llamadas.
- **Generación inicial por la API:** hasta 3 por petición, repetible (R-5).
- **H-G9** (GENERAL_FITNESS intermedio o avanzado con 1×30–35 min): las 3 llamadas de la generación inicial se pierden siempre antes del motor de reglas (`auditoria_activacion_ia/03`).
- **Coste de referencia (GRA, con versiones anteriores del modelo):** unos USD 0,023 por llamada de generación y unos USD 0,013 por llamada de adaptación. Una semana con N usuarios cuesta, en el peor caso, unos **3 × N × USD 0,023**.
- **Tres intentos no son un límite de gasto.** Limitan las llamadas por petición, pero no el número de peticiones, ni de usuarios, ni de reintentos de la API, ni las predicciones que siguen en curso (R-2).

## 4. Recomendación operativa para la cuenta de Replicate

1. **Cuenta o token exclusivos del entorno de pruebas.** Así el gasto queda separado y se puede revocar sin afectar a otros entornos.
2. **Límite de gasto mensual** en la facturación de Replicate, si la cuenta lo ofrece. Si no, saldo prepagado bajo, del orden de la prueba prevista: con 20 usuarios y 4 semanas, ≈ 3 × 20 × 4 × USD 0,023 ≈ USD 5,5 en el peor caso.
3. **Alertas de facturación** por correo a umbrales intermedios, por ejemplo el 50 % y el 80 % del límite.
4. **Revisión diaria** del panel de predicciones durante la prueba. Buscar predicciones largas o canceladas (R-2) y repeticiones por usuario (R-5).
5. **Interruptor de emergencia:** `FITSENSE_AI_ENABLED=false`, más reiniciar o redesplegar el servicio. El backend sigue funcionando con el motor de reglas y el respaldo determinista.
6. **Revocar el token** al terminar la prueba.
