# 04 · Pruebas sin coste (unitarias, integración y Replicate simulado)

No hubo ninguna predicción pagada. Todo usa PostgreSQL 16 con el catálogo real, la política por omisión `PLAN-REV-2.0` y los validadores de `generation`.

## 1. PR-1.7 no cambia

| Prueba | Resultado |
|---|---|
| `H2v2PrimeraPropuestaIT`, modo `PROMPTS` (26 casos) | El tamaño de la petición PR-1.7 (prompt más instrucción de sistema) es **idéntico en los 26 casos** al registrado en la evaluación de H2 (`cierre_gpt5_mini_h2/evidencia/h2/prompts_y_cotas_26_casos.csv`) |
| `ContratoH2v2IT` §1 (26 perfiles) | La instantánea PR-1.7 no trae ningún campo nuevo, y es igual a la de `PlanInputSnapshot.of(context)`. El prompt por omisión es PR-1.7 y no contiene ninguna frase de PR-2.0 |
| Regresión completa | Todas las pruebas de prompt existentes sin cambios (huellas, capacidades, V4-C, REV2) |

## 2. PR-2.0 dice exactamente lo que verifica el backend (`ContratoH2v2IT`, 26 perfiles)

Los 26 perfiles cubren los cuatro objetivos, 3–6 días, casa y gimnasio, principiante e intermedio, con y sin restricciones.

| Comprobación | Resultado |
|---|---|
| **Tabla de la mitad** en cada sesión | Igual a `ContratoReparto.tablaMitad` |
| **`body_parts_con_material`** | Los grupos que V15 cuenta como disponibles |
| **`min_body_parts`** | `min(2, con material)` |
| **Techos de V26** | Los de `DosisSemanalPolicy` para el nivel, en la instantánea y en el texto de la regla 21 |
| Sin «más 15 %» | Comprobado en los 26 |
| Sin frases de llenar tiempo | Comprobado en los 26 |
| Sin el mínimo de duración | Comprobado en los 26 |
| Sin la serie adicional de 4D | Comprobado en los 26 |
| Comprobación del reparto | Va **después** de los datos |
| Reintento | Lleva las pistas de V15 y V26, «reglas 1 a 21» y la forma de reponer que no contradice V15 ni V26 |
| **Factibilidad:** la rutina válida del motor de reglas cumple todas las cuotas | **26/26** (tabla de la mitad, techo de sesión y techo semanal). Ninguna instrucción de PR-2.0 es imposible con el catálogo y la disponibilidad de esos perfiles |
| **Exactitud V26:** una serie más que el techo en una sesión da exactamente «V26: … suma N+1 series directas de G y el techo por sesión es N» | **26/26** |
| **Exactitud V15:** un ejercicio más que el tope de la mitad da exactamente «V15: … dedica tope+1 de n ejercicios a G» | **25/26**. En el que falta, ninguna sesión tenía material para la mutación; no es un fallo |

## 3. El pipeline con PR-2.0 activo (`H2v2PipelineSimuladoIT`, Replicate simulado en el endpoint de gpt-5-mini)

| Caso | Resultado |
|---|---|
| Sin salida válida, con 5 y con 6 días | 3 intentos y respaldo `RULE_ENGINE` con `model_name` nulo. El plan guarda la instantánea `PR-2.0` |
| Propuesta válida de 5 y de 6 días | Publicada como `AI` con `model_name = gpt-5-mini`, en 1 predicción |
| Una sesión por encima del techo de V26 | Rechazada. El 2.º intento lleva el motivo V26, la pista de V26 y «reglas 1 a 21». Tras 3 intentos, `RULE_ENGINE` con `model_name` nulo: **una generación fallida no consta como IA**. El libro registra las 3 predicciones |
| Dolor (O20) en un ejercicio que trae la propuesta | No está en `available_exercises` de ninguna petición. La propuesta se rechaza y lo publicado no lo trae |
| En todas las peticiones | Formato directo, `reasoning_effort=medium`, `prompt_version` PR-2.0, regla 21, tabla de la mitad y comprobación del reparto, sin «más 15 %» |

## 4. Los validadores no cambian: revalidación de las 26 salidas reales ya pagadas

`H2v2PrimeraPropuestaIT` en modo `VALIDAR`, sin Replicate y sin coste:
- **Qué hace:** vuelve a pasar las **26 salidas reales** de la evaluación anterior de H2 (13 de PR-1.7 y 13 de PR-1.9) por el ensamblador, el normalizador y el validador de esta rama.
- **Resultado:** **26/26 veredictos idénticos** a los registrados (`evidencia/revalidacion_26_salidas_reales.csv`).
- **Alcance:** no mide PR-2.0, porque esas salidas se pidieron con otros prompts. Demuestra que nada en esta rama cambia lo que el backend acepta o rechaza.

**Lectura de esos rechazos frente a PR-2.0.** Los 7 rechazos de la evaluación real se reparten así:

| Categoría | Casos | ¿Lo comunica PR-1.7? | ¿Lo comunica PR-2.0? |
|---|---|---|---|
| V15 de la mitad | G14-A, G18-A, G08-B | No: `max_per_body_part` es nulo | Sí |
| V15 de cobertura | G05-B | No | Sí: `body_parts_con_material` |
| V26 | G05-B, G08-B, G14-B, G18-A | No | Sí: regla 21 y comprobación final |
| Fuera del alcance de H2 | G06-A (V23, slots), G20-A (V1, ejercicio inexistente), G14-B (V24, dosis) | Sí | Sí |

## 5. Unitarias

| Prueba | Qué cubre |
|---|---|
| `ContratoRepartoTest` (3) | Regla de la mitad (3+ grupos, 4+ de trabajo, división entera); techos por nivel; versión (`PR-1.7` por omisión, `PR-2.0`; `PR-1.9` y cualquier otro valor rechazados) |
