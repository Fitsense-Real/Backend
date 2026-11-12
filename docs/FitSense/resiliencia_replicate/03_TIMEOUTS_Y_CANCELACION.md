# 03 · R-3 (timeouts HTTP) y R-2 (cancelación)

## R-3 · Límites HTTP

**Antes:** el cliente del JDK sin límites (elección por defecto de `RestClient.builder()` en Spring 7).

**Ahora:** `ReplicateHttp` crea dos clientes con `SimpleClientHttpRequestFactory`, sin dependencias nuevas:
- **POST:** conectar 10 s; leer 90 s, como mínimo `Prefer: wait` + 2.
- **Sondeo y cancelación:** conectar 10 s; leer 30 s.

| Propiedad | Variable | Por omisión | Rango |
|---|---|---|---|
| `fitsense.ai.connect-timeout-seconds` | `FITSENSE_AI_CONNECT_TIMEOUT_SECONDS` | 10 | 1–60 |
| `fitsense.ai.read-timeout-seconds` | `FITSENSE_AI_READ_TIMEOUT_SECONDS` | 90 | `Prefer: wait` + 2 … 300 |
| `fitsense.ai.poll-read-timeout-seconds` | `FITSENSE_AI_POLL_READ_TIMEOUT_SECONDS` | 30 | 2–120 |
| `fitsense.ai.timeout-seconds` (sondeo total) | — | **150 (sin cambios)** | — |

- Los valores fuera de rango se **acotan**, igual que los límites que ya existían.
- Al arrancar se registra la línea `Replicate HTTP: …` con los valores efectivos y la **espera máxima por llamada**: unos 293 s con los valores por omisión (10 + 90 + 150 + 3 + 10 + 30).

**Por qué un límite de lectura del socket y no el del cliente del JDK:**
- El límite de petición del JDK solo cubre hasta las cabeceras, así que un cuerpo que se queda a medias esperaría sin fin.
- Con el límite del socket, una respuesta parcial que se interrumpe también termina.
- **Limitación:** un servidor que enviara un byte cada pocos segundos para siempre no quedaría acotado en el POST. El sondeo sí tiene límite total.

## R-2 · Cancelación de predicciones abandonadas

**Antes:** al dejar de esperar, la predicción seguía en Replicate sin que nadie la leyera.

**Ahora,** si la predicción sigue **activa** (`starting` o `processing`) al terminar el sondeo, por límite agotado, sondeo fallido o sin `urls.get`, o hilo interrumpido:
1. se conserva su identificador y su último estado;
2. se pide **una** cancelación a `urls.cancel` o, si falta, a `POST /v1/predictions/{id}/cancel` en el mismo servidor que `fitsense.ai.base-url`. El identificador se valida: solo alfanumérico;
3. se registra `REPLICATE_CANCEL prediction_id=… estado_previo=… motivo=… resultado=… estado_tras_cancelar=…`.

| Resultado | Significado |
|---|---|
| `CANCELADA` | Replicate confirmó `canceled` |
| `SOLICITADA` | Aceptada, aún en transición |
| `YA_TERMINADA` | Había terminado (`succeeded`/`failed`) antes de cancelar. Si fue `succeeded`, **se usa su salida**: todavía no se ha publicado nada |
| `RECHAZADA` | Replicate respondió 4xx o 5xx |
| `ERROR_DE_RED` | Sin respuesta (red o límite de lectura) |
| `SIN_IDENTIFICADOR` | Ni id ni `urls.cancel` |

**Garantías:**
- **No se cancela** una predicción terminada (`succeeded`, `failed`).
- **Una sola petición de cancelación.**
- **El desenlace no cambia:** fallo del proveedor, sin reintento, y respaldo.
- **Sin respuestas tardías:** una predicción abandonada **nunca se vuelve a leer**. No hay escucha asíncrona ni *webhooks*, así que no puede publicar después de que se haya publicado el respaldo. Comprobado con `ReplicateCancelacionTest.sinRespuestaTardia`: ninguna petición HTTP inesperada.

**Limitaciones del proveedor (no garantizadas por el backend):**
- **Cancelar no implica coste cero:** Replicate puede facturar lo ya procesado.
- Si el POST se corta antes de devolver el identificador (límite de lectura del POST, proxy), **no hay a quién pedir la cancelación**. La predicción puede seguir; solo se ve en el panel de Replicate.
- Si la aplicación se detiene en mitad de un sondeo, la cancelación se intenta igual (`INTERRUMPIDO`), pero puede no llegar a enviarse.
