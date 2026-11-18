# Diagnóstico del error E002 de Replicate

Fecha: 18/11/2026. Solo se usaron consultas gratuitas y de solo lectura (`GET`). **No se ha hecho ninguna predicción
nueva** desde la sonda del 17/11 a las 17:20 UTC.

## 1. El error completo

Es idéntico en las cuatro predicciones:

| Campo | Valor |
|---|---|
| `status` | `failed` |
| `error` | `An error occurred while processing your request (E002) (1cah9wlWR9)` |
| `logs` | `No sid` |
| `metrics` | solo `predict_time` (0,22–0,28 s) y `total_time`; **ni `token_input_count` ni `token_output_count`** |
| `output` | `null` |
| `version` | `hidden` (modelo oficial) |
| `source` | `api` |
| `deployment` | ninguno |
| `data_removed` | `false` |

## 2. Las cuatro predicciones

| # | Id | Creada (UTC) | Inicio → fin (UTC) | Entrada |
|---|---|---|---|---|
| 1 | `7dxtdb57znrmy0d1x9kvp0dppc` | 17/11 16:47:04.189 | 16:47:04.233 → 16:47:04.511 | Prompt real de FitSense (177.198 caracteres), `gpt-5-mini`, `medium`, esquema del plan |
| 2 | `1chmxnn085rmy0d1x9mbgxdhrg` | 17/11 16:48:07.745 | 16:48:07.765 → 16:48:08.011 | Igual que la 1 (otro usuario sintético) |
| 3 | `cc8tx5wdsxrmr0d1x9mrkr9kqg` | 17/11 16:49:08.559 | 16:49:08.567 → 16:49:08.840 | Sonda: «Devuelve el objeto JSON {"ok": true}.», `gpt-5-mini`, `minimal`, esquema de una propiedad |
| 4 | `ydpzjayh7xrmr0d1xa2rtkz8gr` | 17/11 17:20:00.831 | 17:20:00.859 → 17:20:01.083 | La misma sonda, 31 min después |

## 3. Lo que se comprobó

| Comprobación | Resultado | Qué indica |
|---|---|---|
| `GET /v1/account` | 200, cuenta personal (`type = user`) | El token es válido y pertenece a la cuenta autorizada. No es un error de autenticación, que daría 401. |
| `POST` de las predicciones | 201 Created | Replicate **aceptó** las cuatro. No hubo rechazo por facturación al crearlas, que daría 402. |
| Esquema de entrada del modelo (`GET /v1/models/openai/gpt-5-structured`) | Versión `81cb6179…` (10/11/2026). Admite `model` ∈ {gpt-5, gpt-5-mini, gpt-5-nano}, `prompt`, `reasoning_effort`, `json_schema` (incluido el formato envuelto `{"format": …}`) | FitSense envía campos y valores válidos. |
| Contenido del prompt | Sin caracteres de control, sustitutos ni fuera del BMP | Descarta una entrada corrupta. |
| Sonda trivial (n.º 3 y 4) | Falla igual, con la misma referencia `1cah9wlWR9` | **El fallo no depende de la entrada de FitSense.** |
| Última predicción correcta de la cuenta | 12/11/2026 20:31 UTC, `gpt-5-mini`, 33.985 tokens de entrada y 11.421 de salida | Con la misma versión del modelo ya publicada (10/11), la cuenta funcionaba hace 5 días. |
| `run_count` del modelo | 1.137.187 (17/11 ~16:45) → 1.138.745 (18/11 00:13): +1.558 ejecuciones | Otros usuarios siguen ejecutándolo. El contador incluye también las fallidas, así que no prueba que funcione, pero lo hace menos probable que una caída general. |
| `GET /v1/deployments` | Vacío | La cuenta no usa despliegues propios; no influyen. |
| Página de estado y panel (`status.replicate.com`, `replicate.com`) | Inaccesibles desde esta sesión: la política de red del entorno no los permite | No se pudo ver el estado oficial ni la facturación. |
| API de facturación | No existe en la API pública de Replicate | El gasto real y la configuración de facturación solo se ven en el panel web con sesión iniciada. |
| Documentación pública de «E002» o «No sid» | Sin resultados (búsqueda web del 18/11) | Código interno de Replicate: solo soporte puede interpretarlo. |

## 4. Hipótesis

| Hipótesis | A favor | En contra | Valoración |
|---|---|---|---|
| **H1 · Problema de la cuenta en Replicate** para los modelos oficiales de OpenAI: saldo o crédito, límite de gasto alcanzado, método de pago, o una restricción de acceso a modelos de terceros | Misma referencia de error en todas las peticiones. Falla al instante, antes de llamar a OpenAI («No sid»: no se abrió sesión). El modelo sigue teniendo uso de otros. La cuenta funcionaba el 12/11. | El POST devuelve 201, no 402. | **La más probable.** Se comprueba en el panel (Billing). |
| **H2 · Fallo de la integración de Replicate con OpenAI** para este modelo o esta variante | «No sid» sugiere que no se obtuvo sesión del proveedor de origen | `run_count` sigue creciendo; dura al menos 31 min | Posible. Se comprueba en `status.replicate.com` o con soporte. |
| **H3 · Entrada de FitSense** | — | Una sonda de una línea falla igual | **Descartada.** |

No se puede distinguir entre H1 y H2 sin el panel o sin otra predicción (por ejemplo, con otra variante), y la orden
prohíbe nuevas llamadas pagadas.

## 5. Gasto real de las cuatro solicitudes

- **Estimado: USD 0,00.** Ninguna tiene tokens de entrada ni de salida, y el modelo se factura por tokens
  (USD 0,25 y 2,00 por millón).
- **Real: no se puede confirmar desde esta sesión.** No hay API de facturación y el panel no es accesible.

Para confirmarlo hay que ir al panel, en Account → Billing / Usage, y buscar las cuatro predicciones o el consumo del
17/11 entre las 16:47 y las 17:21 UTC.

## 6. Lo que no se ha hecho, a propósito

- No se cambió el modelo, la variante, el esfuerzo de razonamiento ni ningún parámetro de producción
  (`application.properties` no se ha modificado).
- No se repitió ninguna predicción. Las dos sondas del 17/11 son las únicas llamadas fuera del backend.
- No se consultó ni se modificó la configuración de facturación, por falta de acceso.
