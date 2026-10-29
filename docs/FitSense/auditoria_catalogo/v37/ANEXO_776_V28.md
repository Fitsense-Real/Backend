# Anexo · rectificación del ejercicio 776 frente a V28 (propuesta)

**Fecha:** 29/10/2026.

**Estado:** **aprobado** por el responsable del proyecto en la orden «Aprobación condicionada de V37» (29/10/2026), que **desactiva 776** en V37. Sigue el mismo esquema que `ANEXO_872_V28.md`.

**Alcance:** documental.
- No modifica V28.
- No crea, revoca ni reescribe eventos en `exercise_difficulty_approvals`.
- No inventa revisiones humanas.

## 1. Lo que registró V28 (se conserva íntegro)

`V28__approve_lower_hip_difficulty.sql` registró un evento `APPROVED` para el ejercicio **776**:
- `source_code` 3292, `name_en` «elevator»;
- nivel **1**, dentro del grupo `LOWER_HIP`, patrón `HIP_HINGE`;
- revisores **Juan y Daniel**, el 10/10/2026, con la nota «Único de los cuatro que NO exige trabajo en el suelo».

El material de la revisión era **texto** (`revision-LOWER_HIP.txt`, SHA-256 `816efc48…3ed0`). Las instrucciones del catálogo de 776 describen una **bisagra de pie**: «ponte de pie con las rodillas ligeramente flexionadas, inclina el tronco hacia delante desde la cintura y vuelve». Con ese texto, el nivel 1 y `HIP_HINGE` eran coherentes. Los revisores valoraron lo que se les dio.

## 2. Lo que muestra el GIF original

`videos/3292-zYmNaoY.gif` (18 fotogramas), revisado en la auditoría del catálogo. Hoja de fotogramas: `revision_visual/hoja_fotogramas.py … 776`.

**Descripción:**
- La persona está **suspendida en anillas de gimnasia**, con los brazos por encima de la cabeza y los pies sin apoyo.
- Pasa a una posición **invertida**, con la cabeza por debajo de la cadera, y vuelve.
- En ningún fotograma hay apoyo de los pies ni bisagra de pie.

**Clasificación según las definiciones vigentes:**

| Atributo | V36 | Según el GIF | Definición vigente |
|---|---|---|---|
| `requires_hanging` | false | **true** | V25: «el cuerpo cuelga del agarre, pies sin apoyo» |
| `requires_overhead` | false | **true** | V25: brazos por encima de la cabeza; además, suspensión implica brazos arriba (`ck_exercises_hanging_is_overhead`) |
| `requires_inversion` | false | **true** | V25: «cabeza claramente por debajo de la cadera con el peso en brazos, hombros o cabeza» |
| `difficulty_level` | 1 | **3** | Criterio v5: habilidad gimnástica en anillas, con suspensión invertida y control corporal avanzado (mismo razonamiento que 1267, retirado en V36) |
| Implemento | peso corporal | **anillas** (`gymnastic rings`) | Se ven en todos los fotogramas |
| `movement_pattern` | `HIP_HINGE` | **pendiente de criterio** | No es una bisagra de pie. Las opciones están en el §5 |

## 3. Texto de rectificación

> Durante la auditoría integral del catálogo se identificó una discrepancia de identidad funcional en el ejercicio 776 (`source_code 3292`). La evaluación histórica de V28 lo aprobó con nivel 1 dentro de `LOWER_HIP`, como `HIP_HINGE`, a partir de instrucciones textuales que describen una bisagra de cadera de pie. La animación original muestra una suspensión invertida en anillas de gimnasia, que exige colgarse, elevar los brazos e invertir el cuerpo, y corresponde a una habilidad avanzada. En consecuencia, el ejercicio se desactiva y deja de estar disponible para la generación. Su registro se corrige para describir el movimiento real: banderas de suspensión, brazos arriba e inversión, nivel 3 y anillas como implemento. Se conservan íntegros los registros de V28 como trazabilidad histórica, pero no se interpretan como aprobación de la identidad observada ni como evidencia vigente de que exista un ejercicio de `LOWER_HIP` sin suelo para perfiles de casa. Esta corrección no altera retroactivamente las valoraciones de los revisores.

## 4. Cómo se aplica (V37)

| Condición | Cómo se cumple | Comprobación |
|---|---|---|
| 776 desactivado | `is_active` pasa de true a false (bloque A5 de `correcciones_v37.tsv`) | `CatalogoV37IT` |
| Características reales documentadas | Además de desactivarlo, V37 registra suspensión, brazos arriba e inversión en `true`, nivel 3 y anillas (`gymnastic rings`) como implemento y requisito | `CatalogoV37IT` |
| No mantenerlo activo solo con nivel 3 | Está inactivo: no es elegible para ningún perfil, de ningún nivel | `Usuario31V37IT`, `ImpactoRestriccionesIT` |
| V28 intacta: migración, evento, revisores y resultados | V37 no toca V28 ni `exercise_difficulty_approvals`. La tabla es idéntica antes y después, con los 4 eventos `APPROVED` (122, 776, 872, 878) | `CatalogoV37IT` |
| No transferir la aprobación a la nueva identidad | El filtro global de aprobación humana sigue **apagado**. Con 776 inactivo, el evento de V28 no tiene efecto | — |
| Sin eventos falsos | Ninguna revisión, consenso, revocación ni aprobación nueva | — |

## 5. Patrón

Con la desactivación, el patrón de 776 deja de usarse. V37 conserva `HIP_HINGE` en el registro inactivo para no reescribir la identidad que evaluó V28. Si alguna vez se reactivara, haría falta una revisión nueva del patrón: el GIF no es una bisagra.

**Impacto en `LOWER_HIP`:** 776 sale de la cobertura de todos los perfiles. V28 lo había aprobado por ser «el único sin suelo». La simulación definitiva (`resultados_simulacion.md`) mide cuántos perfiles quedan con `LOWER_HIP` frágil o vacío.

## 6. Pendiente, sin inventar evidencia

- **Revisión humana independiente de la identidad observada:** PENDIENTE.
- **Condición para encender en el futuro el filtro de aprobaciones:** 776 está inactivo y la selección solo considera ejercicios activos, así que su evento de V28 no lo vuelve elegible. Aun así, conviene registrar más adelante, con el contrato de V27 y la aprobación del equipo, una revisión de 776 o un evento `REVOKED`. Hoy no se registra nada.
