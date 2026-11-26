# 01 · Diagnóstico: el caso G08 y el transporte de Replicate

**Orden:** «ORDEN DE TRABAJO — CIERRE H2-v4 Y CORRECCIÓN DE TRANSPORTE REPLICATE».

**Sin coste.** Ninguna llamada pagada.
- La reproducción usa un servidor local sobre un socket real, con el cliente HTTP de producción.
- Los datos de Replicate salen de consultas `GET` gratuitas, hechas el 26/11/2026 durante el cierre de H2-v4.

## 1. Qué pasó

En la corrida REAL de H2-v4 (26/11/2026, réplica 2, G08, brazo B · PR-2.2) ocurrió esto (`evidencia/g08_registro_real.txt`):

| Hora (UTC) | Dónde | Qué |
|---|---|---|
| 20:58:30.117 | Libro de la evaluación | Reserva de la predicción. Empieza el POST |
| 20:58:30.597 | Replicate | **Se crea la predicción** `g40bmfgq8nrzr0d236ktnedbfm` |
| 20:58:31.851 | Backend | `Replicate no respondio correctamente: Error while extracting response for type [java.lang.String] and content type [application/octet-stream]` |
| 20:58:31.852 | Backend | `REPLICATE_TIMING … prediction_id=desconocido post_ms=1729 … resultado=POST_ERROR`. La predicción se da por fallo del proveedor |
| 20:59:25.591 | Replicate | **La predicción termina:** `succeeded`, `predict_time` 54,96 s. Su salida, recuperada después por GET, es un plan válido |

**Consecuencias:**
- Una predicción terminada, y probablemente facturada, quedó sin leer.
- **Pipeline de producción:** un `AiProviderUnavailableException` corta los intentos de IA y publica el respaldo `RULE_ENGINE`. El usuario habría recibido el motor de reglas en lugar del plan válido de la IA, que ya estaba pagado.
- **Reintentos de la operación:** una nueva ejecución de la generación (sin el libro V59) habría pedido una predicción nueva.

## 2. Por qué no se pudo leer la respuesta

### 2.1 Qué dice el mensaje

`Error while extracting response for type [...] and content type [...]` lo produce Spring (`DefaultRestClient.readWithMessageConverters`, Spring 7.0.9) cuando **falla la LECTURA del cuerpo**. Envuelve el `IOException` en un `RestClientException`.

Tres hechos se deducen del mensaje:
1. **El código era 2xx.** Con 4xx o 5xx, `retrieve()` lanza `RestClientResponseException` antes de leer el cuerpo.
2. **La respuesta no traía `Content-Type`.** Spring usa `application/octet-stream` **por omisión** cuando falta la cabecera. No era un cuerpo binario: era una cabecera ausente.
3. **Leer el cuerpo lanzó un `IOException`.**

### 2.2 Reproducción (`ReplicateRecuperacionTransporteTest`)

| Respuesta del servidor local | Resultado en el cliente de producción (`ReplicateHttp.post`) |
|---|---|
| `201` **sin** `Content-Type`; cuerpo cortado por un **reinicio de la conexión (RST)** | `RestClientException: Error while extracting response for type [java.lang.String] and content type [application/octet-stream]`, con causa `IOException`. **El mensaje exacto de G08** (`g08ReproducidoConElClienteDeProduccion`) |
| `201` **con** `Content-Type: application/json`; el mismo corte | El mismo error, pero con `[application/json]` (`elTipoOctetStreamSaleDeLaFaltaDeCabecera`) |
| `201`; cuerpo cortado con un **cierre normal (FIN)** | **No hay error.** `HttpURLConnection` entrega el cuerpo incompleto como si estuviera entero, y llega un JSON ilegible (`t02TruncadaSeRecupera`) |
| `201`; cuerpo a medias y luego nada | Error al agotar el límite de lectura (`t04AMediasSeRecupera`) |

**Conclusión, con evidencia:** G08 recibió **cabeceras 2xx sin `Content-Type`** y **la conexión se reinició a mitad del cuerpo**, 1,7 s después de enviar. El límite de lectura del POST es de 90 s, así que no fue un límite.

**Lo que no se puede demostrar sin gasto:** quién quitó la cabecera y cortó.
- Las respuestas de Replicate llevan `Content-Type: application/json`, como se comprobó en el listado gratuito.
- Apunta a un intermediario o a la red: la evaluación salía por el proxy del entorno.
- No se repitió el POST para comprobarlo: costaría una predicción.

### 2.3 Por qué el backend lo trató como fallo definitivo

La llamada de la evaluación, igual que el endpoint síncrono, el ciclo semanal y la adaptación, **no usa el libro V59** (`DiarioDePredicciones`). Solo la generación asíncrona lo abre. Va por `callMedido`.
- Su `post()` capturaba **cualquier** excepción y lanzaba `AiProviderUnavailableException(e.getMessage())`, sin distinguir si la predicción existía.
- El `RestClientException` de G08 no es `ResourceAccessException` ni `RestClientResponseException`: es una tercera clase.
- **Sin identificador no había forma de seguir ni de cancelar la predicción.**

**El camino con libro** (generación asíncrona) ya lo clasificaba como incierto: `NO_CONFIRMADA`, sin reenviar. Pero **tampoco lo recuperaba**: la predicción también se perdía.

## 3. Otros riesgos del mismo transporte, encontrados al revisarlo

| # | Punto revisado | Antes | Riesgo |
|---|---|---|---|
| 1 | Cliente HTTP | `SimpleClientHttpRequestFactory` (`HttpURLConnection`), con límites de conexión y lectura (R-3) | — |
| 2 | `Content-Type` y `Accept` | El POST mandaba `Content-Type: application/json` y ningún `Accept` | Ahora pide `Accept: application/json`. No cambia lo que Replicate responde |
| 3–4 | `application/json` frente a `application/octet-stream` | Se lee como `String` y se interpreta con Jackson. El tipo no se usa para decidir | El tipo no era el problema: lo era el corte. **No se interpretan como JSON bytes arbitrarios:** un cuerpo ilegible sigue siendo ilegible |
| 5–6 | Respuesta de creación e identificador | Sin libro: un 2xx sin id y sin éxito se daba por fallo. Con libro: `NO_CONFIRMADA` | Predicción creada y perdida |
| 7–8 | Seguimiento y consulta por id | **El primer GET de sondeo que fallaba dejaba de esperar y CANCELABA la predicción (R-2)** | Un fallo pasajero de red cancelaba una predicción que iba a terminar bien, y ya estaba en parte facturada |
| 9 | Timeouts | Conexión 10 s, lectura del POST 90 s, sondeo 30 s y límite total | Sin cambios |
| 10 | **Reintentos del cliente HTTP** | `HttpURLConnection` **reenvía un POST** si el servidor cierra sin responder, salvo con `sun.net.http.retryPost=false` o en modo *streaming*. Comprobado con un servidor local: 2 peticiones con `HttpURLConnection` directo (`evidencia/ReintentoPostHttpURLConnection.java.txt`) | **Duplicado silencioso.** Spring 7.0.9 pone el modo *streaming* (`setFixedLengthStreamingMode` o `setChunkedStreamingMode`), así que el cliente de FitSense **no** reenvía: **1 POST** (`t05CerradaSinRespuesta`). La prueba lo vigila por si cambia la fábrica |
| 11 | Deserialización | El sobre ilegible ya iba al respaldo (R-1) | Sin el id, la predicción se perdía |
| 12 | Clasificación de errores | Sin libro, todo era «proveedor caído». Con libro: conexión o 4xx = no existe; el resto = incierto | Dos criterios distintos según el camino |

**Relación con los reintentos del pipeline** (`PlanGenerationPipeline`):
- Un `AiProviderUnavailableException` **corta** los intentos de IA y publica `RULE_ENGINE`; no se pide otra predicción dentro de la misma generación.
- Un plan inválido (`InvalidPlanDraftException`) sí se reintenta, hasta 3 veces.
- Por tanto, el daño de G08 en producción es **perder la predicción buena**, más una predicción nueva si se vuelve a lanzar la operación.

## 4. Contrato de Replicate que se usa

Solo API documentada, sin nada no documentado:

| Llamada | Para qué |
|---|---|
| `POST /v1/models/{owner}/{name}/predictions` | Crear. `Prefer: wait` sin libro; sin espera con libro |
| `GET /v1/predictions/{id}` | Estado y salida. **Trae la `input` completa** (comprobado con `g40bmfgq8nrzr0d236ktnedbfm`) |
| `GET /v1/predictions` | Listado, de la más reciente a la más antigua. **Trae `input` vacía (`{}`)**, comprobado el 26/11 con una consulta gratuita. Para comparar la entrada hay que pedir cada candidata por su id |
| `POST /v1/predictions/{id}/cancel` | Cancelar (R-2) |

Replicate **no documenta una clave de idempotencia**. La única forma documentada de encontrar una predicción cuyo id no llegó es el listado más la consulta por id, comparando la entrada.

**Salidas de gpt-5-mini que se procesan** (`ReplicateResponse.joinedOutput`):
- texto;
- arreglo de fragmentos;
- objeto;
- sin salida todavía (`starting` o `processing`);
- errores (`failed`, `canceled`).

Esto no se toca.
