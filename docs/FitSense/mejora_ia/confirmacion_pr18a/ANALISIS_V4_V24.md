# Análisis de V4 (duración) y V24 (dosis) · pr-1.8a frente a PR-1.7

## V4 · duración por encima del techo

| Campaña (versión del modelo) | PR-1.7 | pr-1.8a | Dónde |
|---|---|---|---|
| **A2** (`81cb6179…`, decide) | 0 | **3** | O06, LOSE_WEIGHT: la sesión del 02/11 dura ~2148 s con un techo de 2100 s |
| A3 incompleta (`81cb6179…`, descriptiva) | 0 | 0 | — |
| A1 (`045f8102…`, descriptiva) | 0 | 3 | LOSE_WEIGHT |
| C1–C3 con pr-1.8c (`045f8102…`, referencia) | 0 | 14 con pr-1.8c | LOSE_WEIGHT |

**Lectura:**
- Con cualquier variante que añada el bloque «COHERENCIA CON LOS VALIDADORES», **los rechazos V4 aparecen en pérdida de peso**, y nunca con PR-1.7. Se ha repetido en dos versiones del modelo y en las rondas A1 y A2.
- **Mecanismo probable:**
  - las líneas de V15 y V26 empujan al modelo a repartir más ejercicios entre grupos;
  - en LOSE_WEIGHT, con sesiones cortas (30–45 min) y bloques de CARDIO largos, una colocación de más supera el techo por pocos segundos (48 s en O06).
- **Efecto en el usuario:** el backend lo rechaza y reintenta, así que no se publica nada inválido. Pero en O06 el plan final de pr-1.8a tiene la mitad de trabajo aeróbico que el de PR-1.7 (18 frente a 36 min).
- No se modificó el límite de duración.

## V24 · dosis de la capacidad o de la clase

| Campaña | PR-1.7 | pr-1.8a | Dónde |
|---|---|---|---|
| **A2** (decide) | 0 | **1** | O04, GENERAL_FITNESS: el ejercicio 1107 sin la dosis de ninguna capacidad del objetivo |
| A3 incompleta (descriptiva) | 1 | 0 | O07 (PR-1.7, INCREASE_STRENGTH) |
| A1 (otra versión) | 0 | 0 | — |
| Ronda 2, R2 + R3 (otra versión) | 2 | 1 | — |

**Lectura:**
- V24 aparece **con los dos prompts y con poca frecuencia** (0–2 por ronda).
- En A2, la ronda que decide, solo aparece con pr-1.8a. Por eso **no se cumple el criterio 2** (V24 de la variante ≤ control).
- No hay evidencia de que pr-1.8a cause más V24 de forma sistemática. Pero tampoco de que lo reduzca.
- **Ningún plan se publicó con V24:** el backend rechaza y reintenta. V24 no se modificó.
- **GAIN_MUSCLE:** ningún hallazgo V24 con ninguno de los dos prompts. La dosis fija por clase no se tocó.

## Fiabilidad del proveedor (efecto práctico del prompt)

| A2 + A3 | PR-1.7 | pr-1.8a |
|---|---|---|
| Respuestas correctas | 53 | 38 |
| Timeouts (> 150 s) | **0** | **5**: O09 cuatro veces, O03 una |
| Predicciones fallidas en el proveedor | 0 | 2 |
| Errores de E/S en el POST (recuperados) | 0 | 1 (O19, válida) |
| Latencia mediana de las correctas | 78,8 s | 88,2 s |

- pr-1.8a tarda más y, en O09, **supera el límite de 150 s del backend de forma sistemática** con la versión actual del modelo.
- En producción, un timeout lleva al plan de respaldo del motor de reglas.
- Los fallos «failed» y el error de E/S son de la infraestructura del proveedor, no del prompt. Que todos cayeran en bloques de pr-1.8a puede deberse al momento en que se ejecutaron.
