# 06 · Evaluación de `Cancel-After` (entregable B: investigación, sin cambios funcionales)

**No se activa nada.** No hay ninguna cabecera nueva en el código ni ninguna llamada pagada.

**Fuentes:**
- La especificación oficial `https://api.replicate.com/openapi.json` (*Replicate HTTP API* 1.0.0-a1), obtenida con un GET gratuito (`evidencia/cancel_after_contrato_openapi.txt`). La web de documentación de Replicate no es accesible desde este entorno.
- Las 207 predicciones reales de gpt-5-mini de H2-v2, H2-v3 y H2-v4 (`evidencia/latencias_gpt5mini_h2.txt`).

## 1. Compatibilidad: lo que dice el contrato y lo que no

| # | Pregunta | Respuesta | Evidencia |
|---|---|---|---|
| 1 | ¿Admite `Cancel-After`? | **Sí**, como cabecera de petición | OpenAPI: parámetro `Cancel-After` (`in: header`) |
| 2 | ¿En el endpoint que usamos? | **Sí**: `POST /v1/models/{owner}/{name}/predictions` (`models.predictions.create`). También en `predictions.create` y en *deployments* | OpenAPI |
| 3 | ¿Con `openai/gpt-5-mini`? | **No demostrado.** La cabecera es del endpoint, no del modelo, y la especificación no excluye ningún modelo. gpt-5-mini es un modelo oficial que delega en OpenAI; su comportamiento con `deadline` no se puede comprobar sin una predicción pagada | — |
| 4 | Formatos y límites | Duración con sufijo `s`, `m` o `h`, combinable (`1h30m45s`). Sin sufijo se entiende en segundos. **Mínimo 5 s.** No documenta un máximo | OpenAPI |
| 5 | ¿Desde cuándo cuenta? | **Desde la creación** de la predicción («measured from when the prediction is created»), no desde que empieza a ejecutarse | OpenAPI |
| 6 | ¿Y si termina antes? | Nada: el límite solo actúa «if it has not completed». La predicción expone `deadline`, el instante absoluto de cancelación | Campo `deadline` |
| 7 | ¿Y si vence? | Queda `canceled` si estaba ejecutándose, o **`aborted`** si aún no había empezado | Campo `status` |
| 8 | ¿Se cobra una predicción cancelada? | **No documentado** en la especificación. No se asume ni que sea gratis ni lo contrario | — |
| 9 | ¿Afecta a la síncrona y la asíncrona? | A las dos por igual, porque comparten transporte. En la síncrona, `Prefer: wait` (≤ 60 s) mantiene el POST abierto, pero el límite cuenta igualmente desde la creación | — |
| 10 | ¿Y con V59? | Ver §3 | — |
| 11 | ¿Y con los reintentos por propuesta inválida? | Cada reintento es una predicción **nueva**, con su propio límite. No se acumulan | — |
| 12 | ¿Cambia el cliente HTTP? | Solo una cabecera más en `enviar` (`ReplicateTrainingPlanGenerator`). Además, `aborted` llegaría hoy como estado desconocido: el libro lo registraría `FAILED` y el pipeline publicaría el respaldo. Es correcto, pero conviene nombrarlo si se activa | Código |

## 2. Latencias históricas (`predict_time` de Replicate, en segundos)

| Muestra | n | P50 | P90 | P95 | P99 | Máx. |
|---|---:|---:|---:|---:|---:|---:|
| **PR-1.7 (producción)**, las 3 evaluaciones | 104 | 64,9 | 96,8 | 109,2 | 128,5 | 139,3 |
| Todas (PR-1.7, PR-2.0, PR-2.1 y PR-2.2) | 207 | 69,5 | 107,1 | 122,8 | 141,1 | **177,1** |
| H2-v2, 25/11, PR-1.7 | 26 | 85,8 | 112,6 | 127,5 | 136,6 | 139,3 |
| H2-v3, 26/11 mañana, PR-1.7 | 26 | 54,4 | 75,0 | 82,0 | 92,8 | 95,6 |
| H2-v4, 26/11 tarde, PR-1.7 | 52 | 66,3 | 94,5 | 105,5 | 116,1 | 119,9 |

- **Variación entre días:** la mediana de PR-1.7 fue de 54,4 a 85,8 s (+58 %).
- **Duraciones largas legítimas:** ninguna predicción pasó de 180 s; 2 de 207 (1 %) pasaron de 150 s, ambas de PR-2.0.
- **`total_time`** (con la cola) prácticamente igual a `predict_time`: sin colas apreciables en esas fechas.
- **Datos que faltan:** son predicciones de **generación** con el catálogo completo, en tres días seguidos. **No hay muestra real de las correcciones de la adaptación** con gpt-5-mini, ni de horas de carga distinta.

**Lo que el backend ya espera por cada predicción:**
- `Prefer: wait` de 60 s más 150 s de sondeo (`timeout-seconds`): unos **210 s de vida de la predicción** antes de que el backend la deje de esperar y la cancele él mismo (R-2), si tiene su id.
- La llamada HTTP completa tiene un máximo de unos 291 s (`esperaMaximaSegundos`).

**Predicciones que se cancelarían antes de tiempo** con un límite uniforme:

| Límite | Predicciones de las 207 que lo superan | Además corta lo que el backend aún esperaría |
|---|---:|---|
| 60 s | 141 (68 %) | Sí |
| 90 s | 45 (22 %) | Sí |
| 120 s | 12 (6 %) | Sí |
| 150 s | 2 (1 %) | Sí |
| 180 s | 0 | Sí, entre 180 y 210 s |
| **≥ 240 s** | **0** | **No** |

## 3. Interacción con V59

- **Predicción `ENVIADA` reanudada tras un reinicio:**
  - si el límite venció durante la caída, quedará `canceled`: el libro registra `CANCELED` y se publica el respaldo;
  - si había terminado antes (lo normal, P99 de 141 s), la salida sigue disponible y se reutiliza.
  - Un límite **corto** resta valor a la recuperación; uno **por encima de la espera del backend** no la afecta.
- **Predicción sin id** (`NO_CONFIRMADA`, por ejemplo G08 sin el id recibido): es el caso en que `Cancel-After` **sí aporta**.
  - El backend no puede cancelarla porque no sabe su id. Con un límite, Replicate la cancela sola.
  - Reduce el coste **solo** si una predicción cancelada no se cobra entera, y eso no está documentado (§1, n.º 8).
  - Sin límite, su coste ya está acotado por `max_completion_tokens` (16.000).

## 4. Alternativas

| | **A · No usar `Cancel-After`** (hoy) | **B · Límite uniforme conservador** (≥ la espera del backend, p. ej. `5m`) | **C · Límites por tipo de operación** |
|---|---|---|---|
| Ventajas | Sin riesgo de cancelar nada legítimo. Sin cambios ni pruebas pagadas | Acota en el servidor las predicciones huérfanas (sin id, o un proceso muerto antes de anotarlas). No corta nada que el backend fuera a leer: el máximo observado es de 177 s | Ajustaría la adaptación, de prompts más cortos, a un límite menor |
| Riesgos | Una huérfana corre hasta terminar (máx. observado 177 s) | Un día anormalmente lento con más de 5 min: se cancelaría, pero el backend ya la habría abandonado (210 s). La recuperación V59 tras una caída de más de 5 min encontraría `canceled` en lugar de la salida | Sin datos de latencia de la adaptación con gpt-5-mini: un límite por tipo sería arbitrario |
| Costes | El coste completo de cada huérfana, acotado por 16.000 tokens de salida | Igual o menor, según cómo cobre Replicate una cancelada (no documentado) | Igual que B |
| Experiencia del usuario | Sin cambios | Sin cambios: el backend ya habría pasado al respaldo antes | Igual |
| Reintentos y respaldos | Sin cambios | Sin cambios: el estado `canceled` ya lleva al respaldo | Igual |
| Compatibilidad V59 | Plena | Plena con un límite mayor que la espera del backend y que la recuperación típica (lease de 90 s y barrido de 2 min) | Igual que B |
| Arquitectura | — | Una cabecera en `enviar`, más el tratamiento de `aborted` | Posible técnicamente: la cabecera va por petición, y `Peticion.deSemana` / `deCorrecciones` distinguen el tipo |

## 5. Recomendación

**Todavía no hay evidencia suficiente para activar `Cancel-After`.** Hoy se mantiene la **alternativa A**.

Si se decide limitar las ejecuciones, la candidata fundada es la **B con `Cancel-After: 5m`**:
- está por encima del máximo observado (177 s) y de toda la espera del backend (~210 s de vida, ~291 s de llamada);
- no cancelaría ninguna de las 207 predicciones medidas;
- solo actúa sobre predicciones que el backend ya no va a leer.

**La C se descarta** mientras no haya latencias reales de la adaptación.

**Antes de activar B** hace falta una **verificación con una autorización nueva**: unas pocas predicciones pagadas, del orden de céntimos, que comprueben:
1. que gpt-5-mini acepta la cabecera y devuelve `deadline`;
2. cómo cobra Replicate una predicción `canceled` por vencimiento, en el panel de facturación;
3. que `aborted` aparece y el backend lo trata como fallo del proveedor.

Sin esa verificación no se debe activar.
