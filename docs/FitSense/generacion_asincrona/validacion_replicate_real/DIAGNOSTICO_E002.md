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

## 7. Actualización del 18/11/2026 (reanudación tras fusionar #76)

**Comprobado, sin consumir predicciones:**

| Comprobación | Resultado |
|---|---|
| `GET /v1/account` | 200: el token es válido y la cuenta es la autorizada |
| `GET /v1/models/openai/gpt-5-structured` | Misma versión `81cb6179…` (10/11). `run_count` 1.139.109 (18/11 00:55), +1.922 desde el 17/11 ~16:45 |
| Predicciones de la cuenta desde el 12/11 20:35 UTC | Solo las de esta campaña. Ninguna con éxito. |

**No se pudo verificar:**

- **Estado oficial e historial de incidencias.** La página de estado de Replicate está ahora en el sitio de estado de
  Cloudflare. Se añadieron `www.cloudflarestatus.com` y `community.cloudflare.com` a la red de la sesión, pero:
  - la consulta directa la denegó el control de permisos de la sesión, y no se intentó por otra vía;
  - la herramienta de lectura web no resolvía esos hosts.
- **Facturación y cargo real.** No existe API pública para ello; solo se ve en el panel.

**Información de terceros (no oficial, no verificada).** Varios agregadores de estado que replican el sitio de
Cloudflare muestran:

| Fecha | Incidencia | Estado según los agregadores |
|---|---|---|
| 10/11 | Fallos 401 en modelos oficiales de OpenAI vía Replicate | resuelta el mismo día |
| 16/11 | «Errors creating predictions for some models» | resuelta |
| 17/11 | «Errors and stalled predictions on some models»: errores 500, predicciones atascadas y errores de validación por falta de versión | abierta según el último dato disponible |

Su inicio es compatible con el primer E002 (17/11 16:47 UTC), pero **no consta que afecte a `openai/gpt-5-structured`
ni que produzca E002**. Los síntomas descritos (500 y predicciones atascadas) no coinciden del todo con los observados
(201 y `failed` en 0,2 s).

**Sonda mínima autorizada** (18/11 01:23 UTC, una sola):
- id `e23edtt1h9rmr0d1xh09hsd94c`;
- `failed`, `An error occurred while processing your request (E002) (1cah9wlWR9)`, log `No sid`;
- 0,14 s y 0 tokens.

Es el mismo error y la misma referencia que las cuatro anteriores. **La campaña se detiene, como manda la orden.**

**Lo que sigue sin conocerse:** la causa.

| Hipótesis | Situación |
|---|---|
| H1: problema de la cuenta en Replicate | Abierta |
| H2: incidencia del proveedor con este modelo | Abierta; la incidencia del 17/11 de los agregadores la hace plausible, pero no la prueba |
| H3: entrada de FitSense | Descartada |

No hay ninguna hipótesis comprobada.

## 8. Actualización del 18/11/2026 a las 03:56 UTC (incidencia del 17/11 dada por resuelta)

**Contexto:**
- El usuario comprobó que la incidencia oficial del 17/11 figura como resuelta.
- Un agregador de terceros la da por resuelta el 18/11 de madrugada; otros todavía la mostraban abierta.
- No pude leer la página oficial desde la sesión.

**Comprobaciones previas sin coste:**
- `generation` = `a1ccf53`;
- `GET /v1/account` responde 200;
- el modelo sigue en la versión `81cb6179…`;
- no había ninguna predicción nueva de la cuenta.

**Sonda única** `6cb86gw095rmy0d1xk69hkra7g`, con el mismo modelo, esquema y prompt mínimo:

| Momento (UTC) | Estado |
|---|---|
| 03:56:27 | creada (201), `starting` |
| ~03:57 | `processing` |
| 03:58:21 | empieza a ejecutarse |
| 03:58:22 | **`failed`**: `E002 (1cah9wlWR9)`, `No sid`, `predict_time` 0,65 s, 0 tokens |

Esta vez se comportó distinto: hubo ~114 s de cola antes de ejecutarse, frente al arranque inmediato de las cinco
anteriores. El desenlace fue el mismo, con la misma referencia de error.

**Conclusión:**
- El problema **persiste después de que se diera por resuelta la incidencia del 17/11**. Esa incidencia no explica,
  al menos sola, el E002 de esta cuenta.
- Gana peso H1 (cuenta) o un fallo específico de `openai/gpt-5-structured` distinto de la incidencia general. Ninguna
  de las dos está comprobada.
- La campaña se detiene y no se hacen más sondas.

**Siguiente paso:** el panel de la cuenta y la solicitud a soporte, ya actualizada con los 6 ids.

## 9. Sonda del 18/11/2026 a las 14:00 UTC (a petición del usuario)

La sonda `y27xr1snzdrmt0d1xvtsmsqepw` arrancó al instante y terminó en `failed`: `E002 (1cah9wlWR9)`, `No sid`,
0,28 s y 0 tokens. El fallo es el mismo **21 horas después del primero**. La campaña sigue detenida.

## 10. Sonda del 18/11/2026 a las 14:40 UTC

`eta2a81ek5rmr0d1xwd98ngd48`: `failed`, `E002 (1cah9wlWR9)`, `No sid`, 0,09 s, 0 tokens. Sin cambios.

## 11. Prueba del usuario en el Playground de Replicate (18/11/2026)

El titular ejecutó `openai/gpt-5-structured` desde el Playground web, con su sesión iniciada:
- variante `gpt-5-mini`;
- prompt de ejemplo del propio Playground;
- sin FitSense, sin el token del entorno y sin el proxy.

Resultado, según la captura de pantalla:
- «Prediction failed. An error occurred while processing your request (E002) (1cah9wlWR9)»;
- «Generated in 0.1 seconds»;
- logs: `No sid`.

El modelo aparece como «Warm», «Official», con 1,1 M de ejecuciones.

**Conclusiones comprobadas:**

1. **FitSense queda descartado por completo:** código, prompt, esquema, token del entorno y proxy. El fallo se
   reproduce fuera de todo ello, desde la interfaz de Replicate.
2. El fallo está **en Replicate**, para esta cuenta, con `openai/gpt-5-structured` y la variante `gpt-5-mini` como
   mínimo.

**Lo que sigue sin distinguirse:**

| Posibilidad | Situación |
|---|---|
| (a) Restricción o problema de la **cuenta** (facturación, crédito, acceso a modelos de OpenAI) | Abierta |
| (b) Fallo del **modelo o la variante** en Replicate que afecta a esta cuenta | Abierta |

**Para separarlas,** el titular puede hacer desde el Playground, con su cuenta y su coste, dos pruebas mínimas:
- el mismo modelo con otra variante (`gpt-5` o `gpt-5-nano`);
- otro modelo oficial que no sea de OpenAI.

Si solo falla `openai/*`, el problema es de la integración o del acceso a OpenAI. Si falla todo, es de la cuenta. No
forma parte del protocolo de FitSense y no cambia el modelo de producción.

**Ya se puede pedir a soporte con evidencia completa.** El fallo se reproduce en su propio Playground.

## 12. Sonda del 19/11/2026 a las 03:28 UTC

`11y682362nrmw0d1y7csf1q86w`: `failed`, `E002 (1cah9wlWR9)`, `No sid`, 0,23 s, 0 tokens. Sigue igual unas 35 h después
del primer fallo.

## 13. Intentos programados del 19/11/2026 (cada 3 h hasta las 9:00 hora Perú)

| Intento | Hora Perú | Id | Resultado |
|---|---|---|---|
| 1/3 | 01:32 | `afrn5j7ab9rmy0d1ya0rperk4g` | `failed`, E002 `1cah9wlWR9`, `No sid`, 0,15 s, 0 tokens |
| 2/3 | 04:32 | `k2hevnyq3nrmw0d1yck8sd8gj4` | `failed`, E002 `1cah9wlWR9`, `No sid`, 47 s en cola y 0,60 s de ejecución, 0 tokens |
| 3/3 | 07:33 | `7bt2dqqn25rmr0d1yf5sv2m330` | `failed`, E002 `1cah9wlWR9`, `No sid`, 0,06 s, 0 tokens |

Los tres intentos de la noche fallaron igual. El error persiste unas 44 h después del primero. No se programan más
intentos: depende de soporte o de que el Playground vuelva a funcionar.

## 14. Sonda del 19/11/2026 a las 17:00 UTC (a petición del usuario)

`pfmy2026hdrmw0d1yk0azmsjcw`: 115 s en cola y después `failed`, `E002 (1cah9wlWR9)`, `No sid`, 0 tokens. Sin cambios.
