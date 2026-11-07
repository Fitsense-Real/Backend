# 03 · Validadores antes y después de la retirada

Para cada uno de los 21 perfiles: la semana medida copiada, que pasa todos los validadores porque se publicó, frente a la misma copia sin el 135.

Fuente: `evidencia/factibilidad_retirada_{B,C}.csv`, con columnas de ejercicios, minutos, trabajo, V8C y estricta por perfil. B y C son idénticos.

## V8C

| | Antes de implementar (lab, sin retirada declarada) | Después (con `RetiradaSeguridad`) |
|---|---|---|
| Objeción | «la sesión N no estaba señalada y desaparecen los ejercicios [135]» y «declara M minutos en vez de K» | Ninguna, si se cumplen las condiciones del 02 |
| Otros problemas de V8C | Ninguno en los 21 | Ninguno |

## Validación estricta (V1–V25, V20, V24/4D, V23)

| Validador | Perfiles que lo incumplen tras retirar el 135 | Comentario |
|---|---:|---|
| **V20** (duración ≥ 70 % de los minutos del perfil) | **18 de 21**: 16 rechazados por V20, más R0536 y R0545, que además tienen el 135 en una sesión no señalada e incumplen V20 en sus dos sesiones | Único validador estricto que rechaza. Faltan entre 21 s (R0574) y 408 s (R0562). Pasan solo los perfiles de 30 minutos: R0521, R0561, R0573. R0545 (30 min) no pasa porque sus sesiones son más cortas (24 y 26 min) |
| V24 / 4D (dosis por capacidad) | 0 | No cambia ninguna dosis |
| V23 / 4C (cobertura semanal de slots) | 0 | UPPER_PULL sigue cubierto en las sesiones PULL |
| V9, V16, V17 (series, repeticiones, duración declarada) | 0 | Los minutos los calcula el estimador |
| Mínimo de ejercicios por sesión | 0 | La sesión más corta queda con 3 ejercicios |
| V1 (elegibilidad) | 0 | El 135 ya no está |

## Trabajo, dosis y duración

| Medida | Antes → después |
|---|---|
| Repeticiones semanales | bajan entre 18 y 60 (las del 135) |
| Segundos de trabajo | iguales |
| Dosis y descansos de los demás ejercicios | idénticos (0 cambios en los 3 publicados) |
| Duración de la sesión afectada | baja entre 5 y 7 minutos |

## Linaje y publicación

| Configuración | Linaje | Planes activos | Exclusiones |
|---|---|---|---|
| D (los 3 publicados) | 6/6 sesiones con `source_planned_workout_id` | 1 | 135 `ACTIVA` |
| P-03 (13 regenerados con D) | 0/6: regeneración sin linaje V31, comportamiento documentado de P-03 | 1 | 135 `ACTIVA` |

**Ningún validador se ha modificado.** La retirada se publica solo si pasa los mismos que cualquier otra semana.
