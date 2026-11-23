# 02 · Cambios de la migración a `openai/gpt-5-mini` directo

Rama `feature/replicate-gpt-5-mini`, creada desde `generation` en `e46dbe18`. Sin dependencias nuevas. Sin cambios en los validadores, en las políticas ni en los prompts de negocio (PR-1.7 y V2.1).

## 1. Configuración (`application.properties`)

| Propiedad | Variable de entorno | Antes | Ahora |
|---|---|---|---|
| `fitsense.ai.base-url` | `FITSENSE_AI_BASE_URL` (nueva) | `…/openai/gpt-5-structured/predictions` (fija) | `…/openai/gpt-5-mini/predictions` |
| `fitsense.ai.api-format` (nueva) | `FITSENSE_AI_API_FORMAT` | — | Vacío: se deduce de la URL (`direct`; sería `structured` si la URL contiene `/gpt-5-structured/`) |
| `fitsense.ai.model` | `FITSENSE_AI_MODEL` | `gpt-5-mini` (viajaba en la petición) | `gpt-5-mini`. En `direct` es solo la etiqueta de respaldo: el modelo lo fija la URL |
| `fitsense.ai.reasoning-effort` | `FITSENSE_AI_REASONING_EFFORT` | `medium` | **`low`** |
| `fitsense.ai.verbosity` (nueva) | `FITSENSE_AI_VERBOSITY` | — | **`low`** |
| `fitsense.ai.max-completion-tokens` (nueva) | `FITSENSE_AI_MAX_COMPLETION_TOKENS` | — | **16.000**, acotado a 1.000–128.000 |
| `fitsense.ai.cost.input-usd-per-million` / `output-usd-per-million` (nuevas) | `FITSENSE_AI_COST_*` | — | 0,25 / 2,00. Solo para la estimación del registro: no limita nada |

Los tiempos (150 s por llamada; `Prefer: wait` 60 s), los límites HTTP de R-3, los 3 intentos, el cortacircuitos y el presupuesto de la generación **no cambian**.

**Revertir o comparar** sin tocar código:

```
FITSENSE_AI_BASE_URL=https://api.replicate.com/v1/models/openai/gpt-5-structured/predictions
FITSENSE_AI_REASONING_EFFORT=medium
```

`FITSENSE_AI_API_FORMAT=structured` es opcional, porque se deduce de la URL.

- Con esa configuración, el arranque registra un **WARN**: avisa de que ese endpoint dio E002 y de que solo debe usarse a propósito.
- **Nada ejecuta el endpoint estructurado por sí solo.** No hay conmutación automática entre formatos.

## 2. Petición (`ReplicateTrainingPlanGenerator.entrada`)

Hay **un solo constructor** de la entrada, que usan tanto la llamada directa como el libro V59. Antes, `callMedido` tenía su propia copia.

**Formato `direct`:**

```json
{"input": {
  "system_prompt": "Eres el componente de IA de FitSense. Responde UNICAMENTE con un objeto JSON valido … JSON Schema:\n{…}",
  "prompt": "<el mismo prompt de siempre: PR-1.7 en generación, V2.1 en adaptación>",
  "reasoning_effort": "low",
  "verbosity": "low",
  "max_completion_tokens": 16000
}}
```

- **El esquema es el mismo** que se enviaba en `json_schema`: `PlanJsonSchema` para la semana y `SessionFixJsonSchema` para las correcciones. Se serializa con las claves ordenadas, así que la huella del libro V59 es estable entre arranques. La instrucción de sistema tiene versión: `SYS-JSON-1.0`.
- **No se envían `model`, `json_schema` ni `simple_schema`.** `gpt-5-mini` no los admite y no se trasladaron.
- **No se usa `messages`.** Según Replicate, si se envía se ignoran `prompt` y `system_prompt`, y una petición de un solo turno no gana nada con él.

**Formato `structured`:** la petición anterior, byte a byte (`model`, `prompt`, `reasoning_effort` y `json_schema`).

## 3. Respuesta

- **Salida fragmentada:** `joinedOutput()` concatena en orden los fragmentos de texto (arreglo) o toma la cadena tal cual. Probado en `ReplicateGpt5MiniDirectoTest.salidaFragmentada`.
- **Validación, sin aceptación automática:**
  - el texto pasa por el mismo ensamblador (`PlanDraftFromJsonAssembler` / `SessionFixFromJsonAssembler`), que quita las vallas ```` ``` ```` y exige un JSON del contrato;
  - después pasan V1–V27 (generación) o V8C y los demás controles de la adaptación;
  - un texto que no es JSON, un JSON cortado por el tope o una respuesta vacía (todo el tope gastado en razonamiento) son un **intento rechazado** que va a reintento o a respaldo. Nunca son un plan.
- **`metrics`:** se leen `predict_time`, `token_input_count` y `token_output_count` (`ReplicateResponse.Metrics`).

## 4. Registro por petición: `REPLICATE_CONSUMO`

Se escribe una línea por llamada, junto a `REPLICATE_TIMING`, cuyas claves no cambian:

```
REPLICATE_CONSUMO user=1 week=1 attempt=1 modelo=gpt-5-mini formato=direct prediction_id=… resultado=SUCCEEDED
  origen=nueva predict_time_s=… tokens_entrada=no_reportado tokens_salida=no_reportado coste_usd=no_reportado
  coste_cota_usd=0.038664 caracteres_entrada=26373 caracteres_salida=3697 reasoning_effort=low verbosity=low
  max_completion_tokens=16000
```

- **`coste_usd`:** se calcula por tokens cuando Replicate los informa. Si no los informa, vale `no_reportado`. **gpt-5-mini directo no los informa** (`01` §1).
- **`coste_cota_usd`:** cota **superior** que no depende de Replicate. Se calcula como un token por byte UTF-8 de entrada, más 256 de margen, más `max_completion_tokens` de salida, a la tarifa configurada. En `structured` vale `sin_cota`, porque no hay tope de salida.
- **`origen=reanudada`:** marca que la predicción se reanudó del libro V59 y no se volvió a cobrar.
- **Nunca se registra** el prompt, la credencial ni datos personales: solo longitudes y contadores. Probado en `consumoSinTokens`.

La línea `Salida de la IA …` en INFO se mantiene como estaba: es la evidencia de la tesis sobre qué propuso el modelo. Contiene el JSON del plan y no el prompt.

**Ya existían y no cambian:**
- **Validación y origen en la generación:** `Intento N de IA rechazado … [motivos]` y `GENERACION_TIEMPOS … origen=AI|RULE_ENGINE motivo_respaldo=…`.
- **Adaptación:** `Adaptacion del usuario … la IA …`.

Por tanto, un plan de respaldo **nunca** aparece como éxito de la IA: el origen se guarda con el plan (`generation_source`) y se registra en cada generación.

## 5. Etiqueta del modelo en el plan

`modelName()` y `toDraft(...)` usan `modeloEfectivo()`:
- **En `direct`:** el modelo sale de la URL (`gpt-5-mini`).
- **En `structured`:** sale de `model`.

Así, aunque alguien cambie la etiqueta y no la URL, el plan publicado no puede declarar un modelo que no se llamó.

## 6. Arnés K20 (solo pruebas)

**Nueva política `fitsense.evaluacion.k20.sin-metricas=COTA_POR_TOPE`**, opt-in:
- **Sin tokens informados:** carga la `coste_cota_usd` de la llamada y sigue.
- **Bloquea si:**
  - la cota no cabe en la reserva por predicción;
  - o falta la cota.
- **Con tokens informados,** mide como siempre.
- **Las políticas anteriores** (`BLOQUEAR` y `RESERVA_SIN_BLOQUEO`) ignoran la cota: las corridas anteriores no cambian.

El arnés registra además:
- `modelo_ia` en `00_configuracion.txt`;
- `consumo_replicate.log`.

**No se tocaron** los perfiles, el oráculo, las reglas ni los criterios.

## 7. Pruebas nuevas o ajustadas

| Prueba | Qué fija |
|---|---|
| `ReplicateGpt5MiniDirectoTest` (8) | Claves exactas de la entrada `direct`. El esquema de `system_prompt` es el mismo de `json_schema` y es determinista. Salida fragmentada. Texto, vacío o JSON cortado se rechazan. `REPLICATE_CONSUMO` con y sin tokens, sin prompt ni credencial. Valores por omisión, deducción y forzado del formato. Etiqueta del modelo |
| `ReplicateSchemaRoutingTest` (6) | Generación y adaptación llevan su propio esquema en `direct` (en `system_prompt`) y en `structured` (en `json_schema`) |
| `ControlPresupuestoK20Test` (+2) | `COTA_POR_TOPE` en todos los estados. Cota ausente o mayor que la reserva. Medición por tokens. Las demás políticas no cambian. Lectura de las claves |
