# Configuración efectiva de GPT-5 Mini y compatibilidad con O20 / V65

**Orden:** «ORDEN DE TRABAJO — MERGE O20 E INTEGRACIÓN GPT-5 MINI».
**Estado:**
- La PR #84 (O20 y V65) está fusionada en `generation` en `b9c197de`.
- Esta rama la incorpora con un commit de merge (`aba6feb9`), sin conflictos y sin reescribir historia.
- **NO MERGE** de esta PR (#83) hasta la autorización final.
- La PR #69 (H2) no está incluida.

**Replicate:** sin predicciones pagadas en este trabajo. Todo lo de abajo se comprueba con pruebas automatizadas y un Replicate simulado.

## 1. Configuración efectiva (`application.properties` y `ReplicateProperties`)

| Aspecto | Valor por omisión de esta PR | Variable para cambiarlo |
|---|---|---|
| Endpoint | `https://api.replicate.com/v1/models/openai/gpt-5-mini/predictions` | `FITSENSE_AI_BASE_URL` |
| Formato | `direct`, deducido del endpoint. Cualquier URL que no contenga `/gpt-5-structured/` es `direct` | `FITSENSE_AI_API_FORMAT` (`direct` \| `structured`) |
| Modelo efectivo | `gpt-5-mini`. En `direct` lo fija el endpoint (`modeloEfectivo()`), no `fitsense.ai.model`, que no se envía. Es lo que se guarda en `weekly_training_plans.model_name` y en la traza | `FITSENSE_AI_MODEL`, solo en `structured` |
| Razonamiento | `reasoning_effort=medium` (validado con resultados medidos; `04_PERFILES_Y_COMPARACION.md`) | `FITSENSE_AI_REASONING_EFFORT` |
| Verbosidad | `verbosity=low` | `FITSENSE_AI_VERBOSITY` |
| Tope de salida | `max_completion_tokens=16000`, razonamiento incluido; acotado a 1.000–128.000 | `FITSENSE_AI_MAX_COMPLETION_TOKENS` |
| Tiempos | POST sin `Prefer: wait` (el id se guarda antes de esperar); sondeo cada 1 s; tope 150 s por intento; lectura 90 s; sondeo 30 s | `FITSENSE_AI_*_TIMEOUT_SECONDS`, `FITSENSE_AI_POLL_INTERVAL_MILLIS` |
| H2 (#69) | **Desactivado:** la PR no está en esta rama. El prompt de adaptación sigue en `v2.1` | — |

**Petición en `direct`:** `{"input": {...}}` con estos campos:
- `system_prompt`: instrucción `SYS-JSON-1.0` con el esquema JSON;
- `prompt`: contexto y `available_exercises`;
- `reasoning_effort`, `verbosity` y `max_completion_tokens`.

**No se envían** `model`, `json_schema` ni `messages`, que son exclusivos de `gpt-5-structured` o anulan `prompt` y `system_prompt`.
- Lo comprueban `ReplicateGpt5MiniDirectoTest` y `ReplicateSchemaRoutingTest`.
- Ahora también cada petición real del pipeline en `Gpt5MiniConO20IT`.

**Respuesta:**
- `output` llega como **texto o como arreglo de fragmentos**. `ReplicateResponse.joinedOutput()` los une antes de interpretar.
- El JSON se valida entero: ensamblador y V1–V27, incluidos V8C, V15, V20, V24 y V26, sin cambios.

**Respaldo:**
- Con tres intentos no válidos, o con el proveedor no disponible, publica el motor de reglas determinista, también validado.
- El origen real queda en `generation_source`:
  - `AI` con `model_name = gpt-5-mini`;
  - o `RULE_ENGINE`, con la causa en la traza.

**Errores HTTP de Replicate:**

| Situación | Tratamiento |
|---|---|
| 4xx al crear (401, 404, 422…) | `ERROR_ENVIO`: no hay predicción creada ni se reintenta en vano; respaldo por reglas (R12) |
| 5xx o respuesta cortada al crear | `EnvioIncierto` → `NO_CONFIRMADA`: Replicate pudo crearla, así que no se reenvía y se publica el respaldo (R07) |
| Fallo de conexión antes de enviar | `ERROR_ENVIO`, como un 4xx: no hay predicción |
| 503 u otro error al sondear | Se cancela la predicción sin reintentos infinitos; respaldo (R11) |
| `failed` (como E002) o `canceled` | `FAILED` o `CANCELED` en el libro de predicciones; respaldo |
| Sin terminar al agotar el tiempo | `TIMEOUT`; se intenta cancelar; respaldo. La reanudación por id no vuelve a pagar (V59) |
| Respuesta tardía (más de 1.000 s) válida | Se aprovecha si llega con la operación aún viva (R01/R18) |

**Cómo saber que una instalación usa GPT-5 Mini y no el endpoint anterior:**
1. Al arrancar se registra `Replicate: modelo gpt-5-mini en formato direct (reasoning_effort=medium, …)`. Con `structured` sale un **WARN** que nombra `gpt-5-structured` y E002.
2. Cada plan publicado por la IA guarda `model_name = gpt-5-mini`. La traza `REPLICATE_CONSUMO` lleva `modelo=gpt-5-mini formato=direct`.
3. **Riesgo de despliegue:** si el entorno define `FITSENSE_AI_BASE_URL` con la URL antigua, esa variable manda. Hay que comprobarla en el entorno antes de desplegar. Desde aquí no se puede ver ni se ha desplegado nada.

## 2. Compatibilidad con O20 y V65

Las exclusiones por dolor se restan del conjunto elegible en `prepare`, que es común a la IA y al motor de reglas:
- el ejercicio que dolió;
- sus variantes equivalentes;
- la precaución por ángulo;
- la confirmación pedida tras un dolor de sesión sin identificar.

Por eso GPT-5 Mini no los recibe en `available_exercises`. Si los propone, V1 rechaza la propuesta sin cambios en el validador.

`Gpt5MiniConO20IT` usa un Replicate local en `/v1/models/openai/gpt-5-mini/predictions` y la salida simulada es la rutina del motor de reglas:

| Caso | Dolor en | La propuesta de GPT-5 Mini trae | Resultado |
|---|---|---|---|
| Sin relación | Un ejercicio sin equivalencia ni ángulo con la rutina | Nada excluido | **Publicada como `AI`**, `model_name = gpt-5-mini`, 1 predicción |
| Directo | 253, de la rutina | 253 | Rechazada en los 3 intentos; **respaldo por reglas** sin 253 |
| Equivalente | 1005 «lever hip extension v. 2» | 440 «cable standing hip extension», de su grupo | Rechazada; respaldo sin 440 |
| Precaución por ángulo | 992 «lever incline chest press v. 2» | 988 «lever chest press» | Rechazada; respaldo sin 988 ni el resto de la familia y las precauciones |

En todos los casos, cada petición:
- va en formato directo;
- lleva `reasoning_effort=medium`;
- no ofrece en `available_exercises` nada excluido.

**V65:** es la migración de #84 y no cambia en esta rama. La aplican y prueban:
- `MigracionV65IT`, sobre una base existente migrada a V64 con datos y sobre una base nueva;
- `EsquemaVigenteSobreCatalogoAnterior`, para las pruebas que fijan un catálogo anterior.

## 3. Regresión y CI

| Ejecución | Commit | Resultado |
|---|---|---|
| `mvn verify -Pintegration` completo, local, con #84 incorporada | `3ac7d70a` (código igual al de `19c815e9`; este solo añade documentación) | Unitarias: **1.630, 0 fallos**, 1 omitida. Integración: **887, 0 fallos**, 72 omitidas (opt-in). **BUILD SUCCESS** (33 min 08 s) |
| CI de GitHub (`verify`) | `19c815e9` | **success** |
| CI de `generation` tras fusionar #84 | `b9c197de` | **success** (dos ejecuciones `verify`) |

La regresión incluye las pruebas de la orden:
- generación inicial con los cuatro objetivos y los validadores;
- duración autorizada;
- adaptación, reducción de carga, progresión y reincorporación;
- dolor, equivalencias, precauciones y NO_FACTIBLE;
- generación asíncrona y reanudación con Replicate simulado;
- notificaciones push (FCM);
- V65 sobre bases existentes y nuevas;
- contrato HTTP para Mobile.

Ningún validador se modificó.

## 4. Problemas pendientes antes del merge de #83

- **Revisión final y autorización** de la persona responsable.
- **Variable `FITSENSE_AI_BASE_URL` del entorno de despliegue:** comprobar que no fija la URL antigua.
- **Coste real por predicción:** Replicate no informa tokens para `gpt-5-mini` (`05` I-3). Revisar el panel de facturación tras las primeras generaciones reales.
- **Ninguna predicción real nueva:** la compatibilidad con O20 se probó con simulación. Una llamada real costaría alrededor de USD 0,02–0,13 por predicción (cota de `05`) y no se hizo, por el presupuesto.
- Siguen vigentes los riesgos ya documentados de `05_INCIDENCIAS_RIESGOS_LIMITACIONES.md`: forma del JSON sin salida estructurada y más respaldo por reglas que con `gpt-5-structured`.
