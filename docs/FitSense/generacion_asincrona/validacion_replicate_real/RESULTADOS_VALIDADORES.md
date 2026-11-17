# Resultados de los validadores

## 1. Con Replicate real

**No hubo ninguna propuesta de la IA que validar.** Las 2 predicciones del backend fallaron en el proveedor (E002)
antes de generar, así que el pipeline no llegó a normalizar ni a validar ninguna salida de la IA.

| Validador | Propuestas de la IA evaluadas | Rechazos |
|---|---|---|
| V1–V27, V15, V20, V24/4D, V8C, cobertura 4C | 0 | 0 |

## 2. Planes publicados en la campaña

Los dos planes son del **motor de reglas** (respaldo por PROVEEDOR) y los dos pasaron la validación completa del
pipeline antes de publicarse:

| Plan | Usuario | Origen | Validación |
|---|---|---|---|
| 1 | sintético g1 | `RULE_ENGINE` | `DOSIS_4D`: 0 incumplimientos (modo ENFORCE). `COBERTURA_CAPACIDADES`: COMPLETO (HYPERTROPHY). Cobertura 4C del borrador dentro de lo elegible. |
| 2 | sintético g1b | `RULE_ENGINE` | igual; 3 sesiones |

No se relajó ningún validador ni se modificaron V15, V24 o V8C.

## 3. En el ensayo sin coste

Las salidas simuladas son deliberadamente inválidas y los validadores las rechazaron todas:

- es lo esperado;
- en cada generación se ejercitaron los 3 intentos y el respaldo;
- no se publicó ninguna salida inválida.
