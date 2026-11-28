# 02 · Solución: recuperar solo la predicción propia, sin duplicar ni atribuir ajenas

**Archivo de producción modificado: uno,** `planning/infrastructure/generation/ai/ReplicateTrainingPlanGenerator.java`.

No cambia nada de esto:
- el modelo, los parámetros (`medium`, `low`, 16.000), los prompts ni las versiones (PR-1.7 por omisión);
- los validadores (V8C, V15, V20, V23–V26, V65), las políticas, O20, la adaptación, la progresión ni la reincorporación;
- el catálogo, Firebase, Mobile, la base de datos, las migraciones ni las propiedades.

El libro V59 se usa **tal como está**.

> **Revisión de seguridad (orden «VALIDACIÓN FINAL Y MERGE CONDICIONAL DE PR #88»).**
> - La primera versión recuperaba buscando en el listado de Replicate una predicción con la misma entrada. Eso **podía atribuir a una petición el resultado de otra**: dos usuarios, o dos operaciones, con la misma entrada.
> - Se demostró contra el código de `f3786e60` (`03` §2): el usuario A **publicaba como IA** la salida del usuario B.
> - Esa búsqueda **se eliminó**. Lo que sigue es el diseño final.

## 1. Cómo se identifica una predicción

**El contrato de Replicate** (OpenAPI 1.0.0-a1, `01` §4):
- no ofrece cabecera con el id: ni `Location` ni otra;
- no ofrece clave de idempotencia ni metadatos de predicción.

**La única prueba de que una predicción es la de este envío es el `id` que Replicate escribe en la respuesta de ESTE POST, en esta conexión.** Por tanto:

| Evidencia | ¿Prueba identidad? | Uso |
|---|---|---|
| `id` de la respuesta completa del POST | Sí, como siempre | Se anota (V59: `ENVIADA`) y se sigue por ese id |
| `id` de **primer nivel**, **completo**, al principio de un cuerpo **cortado** (G08) | Sí, porque es la misma respuesta. Se confirma además por GET (§3) | Se confirma y se sigue |
| `id` a medias (el corte cae dentro), o un `"id"` anidado (por ejemplo en `input`) | **No** | Nunca se completa ni se adivina: incierta |
| Cabecera `Location` | **No documentada** | Se ignora (`s03LocationNoDocumentadoSeIgnora`) |
| Una predicción del listado con la **misma entrada** | **No**: dos peticiones idénticas son indistinguibles | **No se busca** |
| Misma entrada **y** una sola coincidencia | **No**: la propia puede no existir y la que aparece ser ajena (S01) | **No se busca** |

**Peticiones idénticas** (dos usuarios, o dos operaciones del mismo usuario): cada una solo puede seguir la predicción cuyo id le devolvió **su** POST.
- La reutilización deliberada del libro V59 (`reutilizable`: mismo usuario, misma huella, otra operación vencida) **no cambia**. Usa los registros propios de la base, no el contenido de Replicate.

## 2. Clasificación del POST (`enviar`)

El cuerpo se lee a mano (`exchange`) para **conservar lo recibido** si la conexión se corta. Antes, `retrieve().body(String)` lo perdía todo.

| Resultado | Clasificación | Qué se hace |
|---|---|---|
| Fallo de **conexión** | **No enviada** | Fallo del proveedor; con libro, `ERROR_ENVIO`. No hay predicción |
| **4xx** | **Rechazada** | Igual: definitivo |
| **5xx**, o un código fuera de 2xx | **Incierta** | `PREDICCION_NO_CONFIRMADA`, sin reenviar |
| Corte o límite de lectura **antes** del cuerpo | **Incierta** | Igual |
| **2xx con el cuerpo cortado** (G08) | **Incierta, con un posible id** | Si lo leído trae el `id` completo de primer nivel, se confirma (§3); si no, `PREDICCION_NO_CONFIRMADA` |
| 2xx entero pero ilegible (JSON cortado con cierre normal) | Igual | Igual: se busca el id de primer nivel en lo leído |
| 2xx legible sin `id` y sin éxito | **Incierta** | `PREDICCION_NO_CONFIRMADA`. Nunca se inventa |
| 2xx con `id` válido | **Creada** | Se sigue por su id |

El POST pide `Accept: application/json`, y el registro del corte dice el `Content-Type` real: «ausente» en G08.

## 3. Confirmación por id (`verificarPorId`)

Un solo `GET /v1/predictions/{id}`, gratuito. Se adopta **solo si**:
1. responde 2xx con un JSON cuyo `id` es ese;
2. **no** tiene `data_removed`;
3. tiene una `input` no vacía;
4. esa `input` es **igual, campo por campo,** a la enviada: `system_prompt`, `prompt`, `reasoning_effort`, `verbosity` y `max_completion_tokens`.

Si falla cualquiera, no se adopta. Queda `REPLICATE_RECUPERACION resultado=CONSULTA_FALLIDA|IDENTIDAD_DISTINTA|NO_VERIFICABLE_DATOS_BORRADOS|NO_VERIFICABLE_SIN_ENTRADA|CONSULTA_ILEGIBLE`, y la petición pasa a **`PREDICCION_NO_CONFIRMADA`**.

Si se adopta: `resultado=RECUPERADA_POR_ID`.
- Con libro, la fila pasa de `SOLICITANDO` a `ENVIADA` con el id, y luego a `SUCCEEDED`.
- Después, el sondeo de siempre.

**Sin ventana de tiempo:** la identidad no depende de los relojes (`s07DesfaseDeReloj`).

## 4. Cuándo queda sin confirmar

Queda `PREDICCION_NO_CONFIRMADA` en todo envío incierto **sin un id de su propia respuesta confirmado**:
- 5xx;
- conexión cortada sin respuesta;
- límite de lectura sin cuerpo;
- 2xx sin id;
- id a medias o anidado;
- id cuyo GET falla, no existe, tiene otra entrada o tiene los datos borrados;
- reanudación de un intento `SOLICITANDO`.

**Cómo se trata:**
- **Mensaje:** `AiProviderUnavailableException("PREDICCION_NO_CONFIRMADA: <motivo> (no se puede confirmar a qué predicción corresponde[; id X sin verificar]; no se reenvía)")`. Nunca dice «no existe».
- **Con libro:** `NO_CONFIRMADA`, que se repite al reanudar sin pedir otra.
- **Pipeline:** corta los intentos de IA y publica `RULE_ENGINE`, como cualquier fallo del proveedor. **No se crea otra predicción.**

## 5. Reanudación tras un reinicio (V59)

| Estado del intento | Comportamiento (igual que en `generation`) |
|---|---|
| `SUCCEEDED` | Reutiliza la salida, sin llamar |
| `ENVIADA` | Sigue la predicción por su id: **ahora también si ese id vino de una respuesta cortada** y se confirmó |
| `SOLICITANDO` | `NO_CONFIRMADA`. Sin id no hay identidad. La primera versión buscaba por contenido; se eliminó |
| Resto | Se repite el desenlace |

**Caminos sin libro** (endpoint síncrono, ciclo semanal, adaptación): no sobreviven a un reinicio con un POST en curso. Es una limitación conocida (`04`).

## 6. Sondeo: un fallo pasajero ya no cancela (`awaitCompletion`)

- Un fallo de red, un 5xx, un corte o un cuerpo ilegible al consultar se reintenta en la vuelta siguiente, dentro del **mismo límite total**.
- Termina con **3 fallos seguidos** (`SONDEO_FALLOS_SEGUIDOS_MAX`) o con un **4xx**.
- Si se acaba el tiempo con la última consulta fallida, el desenlace es `POLL_ERROR`.
- **Antes:** el primer fallo cancelaba una predicción que podía terminar bien.

## 7. Reintentos: qué es cada uno

| Mecanismo | Qué hace | ¿Crea predicciones? |
|---|---|---|
| Reintento HTTP del cliente | `HttpURLConnection` **no reenvía el POST**, porque Spring 7.0.9 usa modo *streaming* (`t05CerradaSinRespuesta`, `s09PostSinRespuesta`). Un **GET** sí puede repetirse una vez al cerrarse la conexión: es idempotente y gratuito | No |
| Consulta de una predicción existente | GET por id, en la confirmación (1) y en el sondeo (con tolerancia) | No |
| Reintento por propuesta inválida | Hasta 3 intentos del pipeline (`AI_ATTEMPTS`), sin cambios. Una salida recuperada que no pasa el contrato o el validador cuenta como intento inválido (T11, S14) | Sí, como siempre |
| Reanudación | Libro V59 (§5) | No: reutiliza o sigue por id |
| Respaldo `RULE_ENGINE` | Sin cambios. Lo incierto acaba aquí (S15) | No |

**El máximo de predicciones por generación no sube.** Lo único nuevo es **un GET** gratuito, y solo cuando una respuesta cortada traía el id.

## 8. Seguridad

- Una predicción recuperada **se valida exactamente igual** que una recibida (T11, S14): ensamblador, normalizador, V1–V27 y V8C, O20 y V65. No se publica nada que no los pase.
- **Nunca** se adopta una predicción sin el id de la propia respuesta. Tampoco una cuya entrada no sea idéntica, ni una con los datos borrados.
- Los registros llevan ids y motivos, sin prompt, token ni datos del participante.
