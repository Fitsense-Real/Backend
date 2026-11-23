# 01 · Diagnóstico del error E002 (`openai/gpt-5-structured`)

Fecha: 23/11/2026. Base: `generation` en `e46dbe18` (fusión de la PR #82).

## 1. Hechos comprobados

### Antecedentes (17–19/11/2026)

Fuente: rama `validacion/replicate-real`, `docs/FitSense/generacion_asincrona/validacion_replicate_real/DIAGNOSTICO_E002.md` y `CONCILIACION_REPLICATE.md`.

- **13 predicciones** a `openai/gpt-5-structured` fallaron entre el 17/11 16:47 y el 19/11 17:00 UTC.
  - Todas dieron el mismo error: `An error occurred while processing your request (E002) (1cah9wlWR9)`.
  - En todas, `logs` = `No sid` y `output` = `null`.
  - `metrics` solo traía `predict_time` (0,2–0,3 s). Algunas quedaron de 47 a 115 s en cola antes de fallar.
- **Las dos del backend llevaban el prompt real** de FitSense (177.198 caracteres, `gpt-5-mini`, `medium`, esquema del plan). **Las otras 11 eran sondas triviales** («Devuelve el objeto JSON {"ok": true}.», con un esquema de una propiedad). Todas fallaron igual.
- **Token y cuenta:** `GET /v1/account` daba 200 y los POST daban 201: el token era válido y Replicate aceptaba las peticiones.
- **Última predicción correcta de la cuenta con ese modelo:** 12/11/2026 20:31 UTC, con la misma versión publicada `81cb6179…` (del 10/11).
- **Otros usuarios:** el `run_count` del modelo seguía creciendo, es decir, otros lo seguían ejecutando.

### Comprobaciones de hoy (23/11/2026)

Son sondas mínimas, todas fuera del backend.

| # | Prediction id | Endpoint | Entrada | Resultado |
|---|---|---|---|---|
| A | `td61xmrp0xrmt0d216d800cdxr` | `openai/gpt-5-structured` | `model=gpt-5-mini`, `prompt` de una línea, `reasoning_effort=minimal`, **sin `json_schema`** | **failed**, `E002 (1cah9wlWR9)`, en 0,08 s |
| B | `xq60m68te9rzw0d216db1mpmrr` | `openai/gpt-5-mini` | `prompt` de una línea, `reasoning_effort=minimal`, `verbosity=low` | **succeeded** («OK»), en 1,6 s |
| C | `4z7pe5b0r5rzw0d216etaj3vbg` | `openai/gpt-5-mini` | `system_prompt` («solo JSON»), `prompt`, `reasoning_effort=low`, `verbosity=low`, `max_completion_tokens=2000` | **succeeded**, JSON válido, en 3,9 s |

### Esquema de entrada de `openai/gpt-5-mini`

Leído con `GET /v1/models/openai/gpt-5-mini`; versión `6b1fccb7…`.

- **Parámetros:** `prompt`, `system_prompt`, `messages`, `image_input`, `reasoning_effort` (`minimal`/`low`/`medium`/`high`; por omisión `minimal`), `verbosity` (`low`/`medium`/`high`; por omisión `medium`) y `max_completion_tokens` (sin valor por omisión).
- **No** admite `model`, `json_schema` ni `simple_schema`.
- `messages`, si se envía, hace que se ignoren `prompt` y `system_prompt`.

### Métricas de `openai/gpt-5-mini`

- Las predicciones B y C, y todas las de las pruebas reales posteriores (`03`), informan solo `predict_time` y `total_time`.
- **No informan `token_input_count` ni `token_output_count`**, a diferencia de `gpt-5-structured`.

## 2. Conclusión, separando hechos de hipótesis

| Afirmación | Tipo | Base |
|---|---|---|
| El E002 **no lo causa la petición de FitSense** (prompt, esquema ni parámetros) | **Hecho** | Falla igual con una línea de texto. La sonda A falla incluso **sin `json_schema`** |
| El E002 **no es un problema de credencial** | **Hecho** | 200 en `/v1/account` y 201 en los POST, el 17/11. Hoy, la misma credencial (inyectada por el proxy) ejecuta `openai/gpt-5-mini` |
| El E002 **no es un bloqueo general de la cuenta** para los modelos oficiales de OpenAI (saldo, facturación, acceso) | **Hecho** (descarta la hipótesis H1 del 18/11) | Hoy la misma cuenta ejecuta `openai/gpt-5-mini`, otro modelo oficial de OpenAI facturado por tokens, en el mismo minuto en que `gpt-5-structured` falla |
| El E002 está **ligado al endpoint `openai/gpt-5-structured`** para esta cuenta | **Hecho** | El único factor que cambia entre A y B es el endpoint, con la misma cuenta, la misma credencial y la misma hora |
| Causa interna en el envoltorio `gpt-5-structured` de Replicate, en su versión `81cb6179…` o en cómo abre la sesión con OpenAI para esta cuenta («No sid») | **Hipótesis** | Encaja con todo lo anterior, pero «E002» y «No sid» son códigos internos sin documentación pública. Solo Replicate puede confirmarlo |
| Por qué otros usuarios sí lo ejecutan | **Sin verificar** | El `run_count` cuenta también las predicciones fallidas |

## 3. Diferencias de entrada y salida entre los dos modelos

| | `openai/gpt-5-structured` (anterior) | `openai/gpt-5-mini` (nuevo) |
|---|---|---|
| Endpoint | `/v1/models/openai/gpt-5-structured/predictions` | `/v1/models/openai/gpt-5-mini/predictions` |
| Modelo | Campo `model` (`gpt-5-mini`) | Lo fija el endpoint; no hay campo `model` |
| Forma de la salida | `json_schema` con `{"format": {"type": "json_schema", "name", "schema"}}`, que el proveedor impone (salida estructurada estricta) | No hay parámetro de esquema. Se pide por instrucción (`system_prompt`) |
| Razonamiento | `reasoning_effort` (FitSense: `medium`) | `reasoning_effort` (G5M: `low`) |
| Verbosidad | — | `verbosity` (G5M: `low`) |
| Tope de salida | — | `max_completion_tokens` (G5M: 16.000), **incluye el razonamiento** |
| Salida | Objeto `{"json_output": …}` | Cadena o arreglo de fragmentos de texto |
| Métricas | `predict_time`, `token_input_count`, `token_output_count` | Solo `predict_time` y `total_time` |

**Consecuencia para la validación.** Con `gpt-5-structured`, el proveedor garantizaba la *forma* del JSON. Con `gpt-5-mini` directo no hay esa garantía. Por eso el backend:
- trata cualquier salida que no sea el JSON pedido (texto suelto, JSON cortado, respuesta vacía) como **intento rechazado**, nunca como plan;
- y mantiene íntegra la validación posterior (ensamblador y V1–V27 en la generación; V8C y el resto en la adaptación).

## 4. Revisión obligatoria del cliente

| Componente | Estado en `e46dbe18` | Cambio en G5M |
|---|---|---|
| `ReplicateProperties` | URL fija de `gpt-5-structured`, `model`, `reasoning_effort` | Añade `api-format`, `verbosity` y `max-completion-tokens`, con gpt-5-mini por omisión (`02`) |
| Construcción de la petición | En dos sitios: `entrada()`, para el libro V59, y una copia a mano en `callMedido`. Ambos con `model` y `json_schema` | Un solo constructor, `entrada()`, según el formato |
| Procesamiento de la respuesta | `joinedOutput()` ya une texto, `json_output` y arreglos de fragmentos | Sin cambios, con prueba de la salida fragmentada |
| Métricas | No se leían | Se leen (`metrics`) y se registran en `REPLICATE_CONSUMO` |
| Reintentos (3), retroalimentación, cortacircuitos, cancelación (R-2), límites HTTP (R-3), libro V59 | Funcionan | **Sin cambios** |
| Respaldo determinista (`RULE_ENGINE`) | Funciona | **Sin cambios** |
| Ensambladores (`PlanDraftFromJsonAssembler`, `SessionFixFromJsonAssembler`) | Quitan las vallas ```` ``` ```` y rechazan un JSON inválido | **Sin cambios** |
| Validadores V1–V27, V8C, V24 | — | **Sin cambios** |

**`generation` contiene los cambios aprobados.** `e46dbe18` es la fusión de la PR #82. Su CI estaba en verde, y la rama G5M sale de ese commit.
