# Anexo · rectificación del ejercicio 776 frente a V28 (propuesta)

**Fecha:** 29/10/2026.

**Estado:** propuesta para aprobación del responsable del proyecto. Sigue el mismo esquema que `ANEXO_872_V28.md`.

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

## 3. Texto de rectificación propuesto

> Durante la auditoría integral del catálogo se identificó una discrepancia de identidad funcional en el ejercicio 776 (`source_code 3292`). La evaluación histórica de V28 lo aprobó con nivel 1 dentro de `LOWER_HIP`, como `HIP_HINGE`, a partir de instrucciones textuales que describen una bisagra de cadera de pie. La animación original muestra una suspensión invertida en anillas de gimnasia, que exige colgarse, elevar los brazos e invertir el cuerpo, y corresponde a una habilidad avanzada. En consecuencia, se propone adoptar para la generación futura las banderas de suspensión, brazos arriba e inversión, el nivel 3 y las anillas como implemento. Se conservan íntegros los registros de V28 como trazabilidad histórica, pero no se interpretan como aprobación de la identidad observada ni como evidencia vigente de que exista un ejercicio de `LOWER_HIP` sin suelo para perfiles de casa. Esta corrección no altera retroactivamente las valoraciones de los revisores.

## 4. Cómo se aplicaría (propuesta V37, bloque A)

| Condición | Cómo se cumple |
|---|---|
| V28 intacta: migración, evento, revisores y resultados | La propuesta V37 no toca V28 ni `exercise_difficulty_approvals`. La base de prueba tiene los mismos 4 eventos antes y después |
| Nivel operativo | `difficulty_level` de 776 pasa de 1 a 3 en `exercises`. El evento de V28 conserva su nivel 1 como registro histórico |
| No transferir la aprobación a la nueva identidad | El filtro global de aprobación humana sigue **apagado**: el evento de V28 no tiene efecto en la selección |
| Seguridad | Con V37, 776 deja de ser elegible para perfiles sin suspensión, sin brazos arriba, sin anillas o de nivel 1–2. Lo comprueban `ImpactoRestriccionesIT` y `Usuario31ReproduccionIT` |

## 5. Decisión pendiente: patrón de 776

V37 **no** cambia el patrón, porque la clasificación correcta no es única:

| Opción | Consecuencia |
|---|---|
| a) Mantener `HIP_HINGE` | Sigue contando para `LOWER_HIP` en perfiles avanzados con anillas y suspensión, aunque no es una bisagra |
| b) `OTHER` | No cuenta para ningún slot ni capacidad. Es la opción más prudente para un movimiento gimnástico sin patrón de fuerza claro |
| c) `VERTICAL_PULL` | El tirón en suspensión cuenta como `UPPER_PULL` para avanzados |
| d) Desactivarlo (como 1267) | Desaparece del catálogo. Coherente con el precedente de habilidades gimnásticas avanzadas sin progresión |

**Recomendación técnica:** opción d) o b). Lo decide el responsable del proyecto.

**Impacto en `LOWER_HIP`:** con el bloque A, 776 sale de la cobertura de todos los perfiles sin suspensión, sin anillas o de nivel 1–2. V28 lo aprobó precisamente por ser «el único sin suelo». La simulación (§ correspondiente del informe) mide cuántos perfiles quedan con `LOWER_HIP` frágil o vacío.

## 6. Pendiente, sin inventar evidencia

- **Revisión humana independiente de la identidad observada:** PENDIENTE.
- **Condición para encender en el futuro el filtro de aprobaciones:** igual que con 872. Antes habrá que registrar, con el contrato de V27 y la aprobación del equipo, una revisión de 776 o un evento `REVOKED`. Hoy no se registra nada.
