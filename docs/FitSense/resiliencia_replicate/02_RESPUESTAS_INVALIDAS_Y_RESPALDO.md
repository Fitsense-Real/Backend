# 02 · R-1: respuestas inválidas y respaldo

## Clasificación (después de la corrección)

| Respuesta | Clase | Reintento | Cortacircuitos | Destino si se agotan los intentos |
|---|---|---|---|---|
| Éxito con salida válida | — | — | Se reinicia | Se publica la IA |
| **2xx que no es JSON, JSON cortado, `[]`, cuerpo vacío** (sobre de Replicate ilegible) | **Proveedor** (`INVALID_RESPONSE`, nuevo) | No | Cuenta | Motor de reglas / respaldo determinista |
| `{}` o estado no `succeeded` (`failed`, `canceled`) | Proveedor | No | Cuenta | ídem |
| Error de comunicación (red, 4xx, 5xx, 429) | Proveedor | No | Cuenta | ídem |
| Timeout (`TIMEOUT`, ver `03`) | Proveedor | No | Cuenta | ídem |
| JSON del **modelo** mal formado, vacío, con campos que faltan o versión de esquema distinta | **Contrato** (`InvalidPlanDraftException`) | Sí, con la propuesta y los motivos | Se reinicia | ídem |
| Propuesta válida que incumple V1–V27 (generación) o V8C/V1–V27 (adaptación) | **Validación** | Sí, con realimentación | Se reinicia | ídem |
| Cualquier otro error inesperado en el camino de la IA | **Inesperado** (`IA_ERROR_INESPERADO`, ERROR) | No | No cuenta | ídem |

## Garantías

- **Máximo de 3 intentos** (`AI_ATTEMPTS`), sin bucles. Un fallo del proveedor o un error inesperado cortan el bucle tras 1.
- **El respaldo se valida con los mismos validadores.** En generación: `RULE_ENGINE`, con alternativas deterministas REV2. En adaptación: conservar, SUST-SEG-1, o `NO_FACTIBLE` + P-03.
- **Nunca se publica una respuesta inválida.** Si el respaldo tampoco vale, el error sigue siendo explícito: `PlanGenerationFailedException` (semana sin plan, adherencia NULL) o `NO_FACTIBLE` registrado.
- **El origen del plan se conserva:** `RULE_ENGINE` cuando decide el motor de reglas.
- **La causa queda registrada:** los motivos acumulados llegan al motor de reglas, el log `REPLICATE_TIMING` (`resultado=INVALID_RESPONSE`) y `IA_ERROR_INESPERADO`.
- **No se copia el cuerpo crudo de Replicate** al mensaje ni al log: repite el prompt entero.
- **Sin planes duplicados:** la publicación no cambia (ver `05` para las peticiones simultáneas).

## Decisión registrada

`INVALID_RESPONSE` cuenta para el cortacircuitos, como `POST_ERROR`. `ReplicateTimingTest` exige decidirlo al añadir un resultado.
