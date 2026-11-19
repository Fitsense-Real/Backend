# Conciliación con Replicate

Fuentes:

- **Replicate:** `GET /v1/predictions` y `GET /v1/predictions/{id}`, gratuitos y de solo lectura, con la credencial
  que inyecta el proxy;
- **FitSense:** `plan_generation_predictions` (libro V59) y `plan_generation_operations` de la base de pruebas
  `fitsense_vr`;
- **sondas manuales:** se registran aparte en `PREDICCIONES_Y_COSTOS.csv`.

## 1. Punto de partida

Antes de la campaña, la última predicción de la cuenta era del **12/11/2026 a las 20:34 UTC**. Entre esa fecha y el
inicio de G1 (17/11/2026 16:47 UTC) no había **ninguna** predicción. Por eso cualquier predicción posterior es de esta
campaña, y se comprobó que todas lo son.

## 2. Conciliación por predicción

| # | Id en Replicate | Creada (UTC) | Replicate | Libro V59 | Origen | Operación |
|---|---|---|---|---|---|---|
| 1 | `7dxtdb57znrmy0d1x9kvp0dppc` | 16:47:04 | failed E002 | intento 1, FAILED, mismo id | backend | `d13991ff…` (G1) |
| 2 | `1chmxnn085rmy0d1x9mbgxdhrg` | 16:48:07 | failed E002 | intento 1, FAILED, mismo id | backend | `05b47d64…` (G1, repetición) |
| 3 | `cc8tx5wdsxrmr0d1x9mrkr9kqg` | 16:49:08 | failed E002 | — (no es del backend) | sonda manual de diagnóstico | — |
| 4 | `ydpzjayh7xrmr0d1xa2rtkz8gr` | 17:20:00 | failed E002 | — (no es del backend) | sonda manual de diagnóstico | — |
| 5 | `e23edtt1h9rmr0d1xh09hsd94c` | 18/11 01:23:16 | failed E002 | — (no es del backend) | sonda de reanudación (fase 5) | — |
| 6 | `6cb86gw095rmy0d1xk69hkra7g` | 18/11 03:56:27 | failed E002 (tras 114 s en cola) | — (no es del backend) | sonda tras la incidencia resuelta | — |
| 7 | `y27xr1snzdrmt0d1xvtsmsqepw` | 18/11 14:00:09 | failed E002 | — (no es del backend) | sonda a petición del usuario | — |
| 8 | `eta2a81ek5rmr0d1xwd98ngd48` | 18/11 14:40:32 | failed E002 | — (no es del backend) | sonda a petición del usuario | — |
| 9 | `11y682362nrmw0d1y7csf1q86w` | 19/11 03:28:38 | failed E002 | — (no es del backend) | sonda a petición del usuario | — |
| 10 | `afrn5j7ab9rmy0d1ya0rperk4g` | 19/11 06:32:42 | failed E002 | — (no es del backend) | intento programado 1/3 | — |
| 11 | `k2hevnyq3nrmw0d1yck8sd8gj4` | 19/11 09:32:51 | failed E002 (47 s en cola) | — (no es del backend) | intento programado 2/3 | — |

## 3. Comprobaciones

| Comprobación | Resultado |
|---|---|
| Predicciones nuevas en Replicate | **11** (4 el 17/11, 4 el 18/11 y 3 el 19/11) |
| Filas del libro (backend) + sondas registradas | 2 + 9 = **11**. Coinciden una a una por id. |
| Predicciones sin identificar | 0 |
| NO_CONFIRMADA | 0 |
| Predicciones duplicadas (dos POST para el mismo intento) | 0 |
| Cancelaciones | 0 |
| Tokens facturables (`metrics`) | 0 de entrada y 0 de salida en las 11. Solo hay `predict_time` (0,14–0,65 s). |
| Coste estimado | **USD 0,00**: tokens × tarifa (0,25 / 2,00 USD por millón) = 0 |
| Cargo real | **No verificable desde aquí.** La API pública no expone la facturación y `replicate.com` no está en la red permitida. **Pendiente de confirmar** en el panel de facturación de Replicate. Al ser un modelo oficial que se factura por tokens y no haber ninguno, lo esperado es USD 0,00. |
| Configuración de facturación | No se consultó ni se modificó: no hay API para ello. No se activaron recargas. |

## 4. El error

Las 4 predicciones fallaron igual:

- error `An error occurred while processing your request (E002) (1cah9wlWR9)`, con **la misma referencia** en las 4;
- log `No sid`;
- de 0,22 a 0,28 s de ejecución y 0 tokens.

Falló igual el prompt real de FitSense (177.198 caracteres, ~50.000 tokens) que una sonda de una línea con un esquema
de una propiedad, con `gpt-5-mini` en los dos casos. Y siguió fallando 31 minutos después.

Así que **no depende de la entrada de FitSense**. Es un fallo persistente del modelo `openai/gpt-5-structured` en
Replicate o de la cuenta del lado de Replicate. Desde esta sesión no se puede distinguir entre los dos:

- Una sonda con otra variante (`gpt-5-nano`) habría ayudado a separarlos, pero el control de permisos de la sesión la
  bloqueó y no se intentó por otra vía.
- El esquema de entrada publicado por el modelo (`GET /v1/models/openai/gpt-5-structured`, versión `81cb6179…`) admite
  exactamente los campos que envía FitSense: `model`, `prompt`, `reasoning_effort` y `json_schema`.
