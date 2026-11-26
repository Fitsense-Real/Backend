# 02 · Solución: recuperar la predicción existente, sin duplicar

**Archivo de producción modificado: uno,** `planning/infrastructure/generation/ai/ReplicateTrainingPlanGenerator.java`.

No cambia nada de esto:
- el modelo, los parámetros (`medium`, `low`, 16.000), los prompts ni las versiones (PR-1.7 por omisión);
- los validadores (V8C, V15, V20, V23–V26, V65), las políticas, O20, la adaptación, la progresión ni la reincorporación;
- el catálogo, Firebase, Mobile, la base de datos ni las migraciones;
- las propiedades de configuración.

El libro V59 (`plan_generation_predictions`) se usa **tal como está**.

## 1. Una sola clasificación del POST (`enviar`)

Los dos caminos, con libro (generación asíncrona) y sin él (endpoint síncrono, ciclo semanal, adaptación y arneses), pasan por la misma función:

| Resultado del POST | Clasificación | Por qué |
|---|---|---|
| Fallo de **conexión** (`ConnectException`, `UnknownHost`, `NoRouteToHost`, límite de conexión) | **No enviada** | El cuerpo no salió: no hay predicción |
| **4xx** | **Rechazada** | Replicate no la creó: es definitivo |
| **5xx** | **Incierta** | Pudo crearse. Ante un 502 o un 504 de una pasarela, no se sabe |
| Corte, reinicio o límite de lectura | **Incierta** | La petición llegó |
| Fallo al **leer** la respuesta, como en G08 (`RestClientException` «Error while extracting response…») | **Incierta** | Era la tercera clase que antes no se distinguía |
| 2xx ilegible (vacío, HTML, JSON cortado) o 2xx sin `id` válido y sin éxito | **Incierta** | Nunca se inventa un id |
| 2xx con `id` válido | **Creada** | Se sigue por su id, como siempre |

Además, el POST pide `Accept: application/json`.

## 2. Estado incierto: buscar la predicción existente (`recuperar`)

Solo con la API documentada y solo con `GET`, que son gratuitos e idempotentes:
1. `GET /v1/predictions`: las más recientes.
2. **Candidatas:** las creadas desde el inicio del envío menos 2 minutos de margen de reloj, del mismo modelo (`openai/gpt-5-mini`, sacado de la URL) y de origen `api`. Como mucho 20.
3. `GET /v1/predictions/{id}` de cada candidata, para comparar su `input` **campo por campo** con lo enviado: `system_prompt`, `prompt`, `reasoning_effort`, `verbosity` y `max_completion_tokens`. En el formato `structured`, también `model` y `json_schema`.
4. **Decisión:**
   - **exactamente una** coincide: se adopta. Se sigue esa predicción, en curso o terminada, por el sondeo de siempre;
   - **ninguna** coincide: se espera una pausa y se repite una vez más (2 rondas), por si tarda en aparecer;
   - **varias** coinciden (ambigua), o falla una consulta: **no se adopta ninguna**.

Cada intento queda en una línea `REPLICATE_RECUPERACION resultado=RECUPERADA|SIN_COINCIDENCIA|AMBIGUA|LISTADO_FALLIDO|CONSULTA_FALLIDA`, con el motivo del envío.
- Solo lleva ids, sin prompt ni token.
- `REPLICATE_TIMING` y `REPLICATE_CONSUMO` llevan el id recuperado.

**Si no se recupera:** `AiProviderUnavailableException("PREDICCION_NO_CONFIRMADA: <motivo> (no se encontró en Replicate una predicción con esta misma entrada; no se reenvía)")`.
- El pipeline corta los intentos de IA y publica `RULE_ENGINE`, como cualquier fallo del proveedor.
- **No se crea otra predicción.**
- Con libro, la fila queda `NO_CONFIRMADA`, como antes.

## 3. Comportamiento por caso (orden §4)

| Caso | Antes | Ahora |
|---|---|---|
| **A. Rechazada antes de crearse** (conexión o 4xx) | Fallo del proveedor | **Igual:** fallo del proveedor; con libro, `ERROR_ENVIO`. No se busca nada |
| **B. Creada con su id** | Se sigue por su id | **Igual.** No se crea otra mientras sigue en curso |
| **C. Completada con la respuesta ilegible** (G08) | **Sin libro:** fallo del proveedor y predicción perdida. **Con libro:** `NO_CONFIRMADA`, también perdida | **Se busca y se adopta,** y se usa su salida. Con libro, la fila pasa a `ENVIADA` y luego a `SUCCEEDED` con su id. **Ningún POST nuevo** |
| **D. Resultado final inválido** | El validador lo rechaza y se reintenta | **Igual.** La salida recuperada pasa por **los mismos** `toDraft`, normalizador y validador (`t11SalidaInvalidaRecuperada`) |
| **E. Estado desconocido** (no aparece, ambigua o la búsqueda falla) | Sin libro: fallo del proveedor; un reintento de la operación pagaba otra | **`PREDICCION_NO_CONFIRMADA`,** explícita. **No se reenvía:** el pipeline va al respaldo |

## 4. Reanudación tras un reinicio (libro V59)

| Estado del intento | Antes | Ahora |
|---|---|---|
| `SUCCEEDED` | Reutiliza la salida, sin llamar | Igual |
| `ENVIADA` | Sigue la predicción por su id | Igual |
| `SOLICITANDO` (el proceso murió entre el envío y la confirmación) | `NO_CONFIRMADA` directamente | **Se busca la misma petición en Replicate** (ventana de 60 minutos; Replicate borra la entrada de las predicciones de la API a la hora). Si está, se sigue por su id, **0 POST**. Si no, `NO_CONFIRMADA` como antes |
| Resto de finales | Se repite el desenlace | Igual |

**Carrera entre dos ejecuciones** de la misma operación:
- El índice único `ux_pgp_intento_vigente` hace que la segunda reanude la fila de la primera (`REPLICATE_CARRERA`).
- Si la primera aún está enviando, la segunda busca la predicción y la sigue si ya existe, o queda `NO_CONFIRMADA`.
- **Nunca se envían dos** (`t12Concurrencia`: 1 POST).

## 5. Sondeo: un fallo pasajero ya no cancela (`awaitCompletion`)

**Antes:** el primer `GET` de consulta que fallaba terminaba el sondeo, y la predicción se **cancelaba** (R-2) aunque fuera a terminar bien.

**Ahora:**
- Un fallo de red, un 5xx, un corte o un cuerpo ilegible al consultar **se reintenta en la vuelta siguiente**, dentro del **mismo límite total** (`timeout-seconds` y el plazo de la generación). Es un `GET` gratuito sobre la misma predicción.
- Termina como antes con **3 fallos seguidos** (`SONDEO_FALLOS_SEGUIDOS_MAX`) o con un **4xx** (no existe o sin permiso).
- Si se acaba el tiempo con la última consulta fallida, el desenlace es `FALLO_LA_CONSULTA` (`POLL_ERROR`), no `TIMEOUT`.

## 6. Reintentos: qué no cambia (orden §8)

| Reintento | Qué es | Cambio |
|---|---|---|
| Del cliente HTTP | `HttpURLConnection` no reenvía el POST (modo *streaming* de Spring) | Ninguno. Ahora lo vigila `t05CerradaSinRespuesta` |
| De la consulta | `GET` sobre la misma predicción | Tolera fallos pasajeros (§5). **No crea predicciones** |
| Del pipeline por plan inválido | Hasta 3 intentos (`AI_ATTEMPTS`) | Ninguno |
| Por fallo del proveedor | Corta y va a `RULE_ENGINE` | Ninguno. Lo incierto sigue ese camino y **no consume un intento nuevo** |
| Respaldo `RULE_ENGINE` | Determinista y validado | Ninguno |

**El número máximo de predicciones por generación no sube.** Lo que se añade son `GET` gratuitos: como mucho, 2 listados y 20 consultas por ronda, y solo en un envío incierto.

## 7. Seguridad

- Una predicción recuperada **se valida exactamente igual** que una recibida: mismo ensamblador, mismo normalizador, mismos V1–V27 y V8C, O20 y V65. No se publica nada que no los pase.
- No se adopta **nunca** una predicción cuya entrada no sea idéntica, ni una de otro modelo, ni una elegida entre varias.
- Los registros llevan ids y motivos, sin prompt, token ni datos del participante.
