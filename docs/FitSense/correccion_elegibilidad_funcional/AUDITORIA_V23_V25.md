# Auditoría de V23 y V25 frente a la cobertura real

**Pregunta:** ¿puede un HORIZONTAL_PUSH satisfacer en silencio UPPER_PULL, o un patrón de tronco satisfacer HYPERTROPHY en una sesión PULL? ¿Hay que cambiar V23 o V25?

**Conclusión:**
- V23 **no confunde** un empuje con una tracción.
- V23 y V25 **sí permiten** que una sesión PULL sin ninguna tracción pase por válida.
- La corrección se hace **sin cambiar V23 ni V25 globalmente**, con una regla nueva y versionada (VEF, EF-0.1).

## 1. Lo que cubre cada capa

Se distinguen seis cosas, que el motor vigente mezcla:

| Concepto | Qué es | Quién lo comprueba hoy |
|---|---|---|
| Zona | `body_part` (back, chest…) | V15 (por sesión), el selector |
| Patrón real | `movement_pattern` | V23 (solo por slot y por **semana**) |
| Capacidad | STRENGTH, HYPERTROPHY… derivada de la clase del patrón | V24 (dosis) y V25 (cobertura semanal) |
| Equipamiento | `equipment` frente a lo declarado | El filtro de elegibles (catálogo) |
| Dosis | series × repeticiones, descanso | V24, 4D |
| **Función de la sesión** | Que un PULL tracciona, un PUSH empuja y LEGS trabaja rodilla o cadera | **Nadie** antes de EF-0.1 |

## 2. V23 (slots 4C)

**Código:** `PlanDraftValidator.validateWeeklySlotCoverage`, `SlotCoveragePolicy`, `MandatorySlot`.

| Pregunta | Respuesta | Evidencia |
|---|---|---|
| ¿Un HORIZONTAL_PUSH cuenta como UPPER_PULL? | **No.** El slot sale del patrón: 775 cuenta como UPPER_PUSH | `MandatorySlot.ofMovementPattern` |
| ¿Se exige UPPER_PULL sin tracción elegible? | **No.** `requiredOf(ofEligible(...))` exige solo los slots con algún candidato | `SlotCoveragePolicy.requiredOf` |
| ¿Se dice que UPPER_PULL no es factible? | **Solo en el registro** (log `Cobertura 4C … elegible [...]`). No llega al plan, a la API ni al usuario | — |
| ¿Es por sesión? | **No: es semanal.** Una tracción el lunes «cubre» un PULL del jueves sin tracción | — |
| ¿Bloquea al motor de reglas? | **No en producción.** `fitsense.coverage.enforcement=ENFORCE_AI_ONLY` solo bloquea los borradores de la IA | `SlotCoverageEnforcement.bloqueaA` |
| ¿LOSE_WEIGHT? | No se exige (4A v6: su principal es CARDIO) | `GoalFeasibilityPolicy.exigeSlotsResistidos` |

## 3. V25 (capacidades 4D)

**Código:** `validateCapabilityCoverage`, `CapabilityCoveragePolicy.evaluar` y `factibilidad`.

| Pregunta | Respuesta |
|---|---|
| ¿Qué acredita STRENGTH? | **Cualquier colocación ESTRUCTURAL** con dosis de fuerza, sea cual sea la sesión. En R0800, 775 (un empuje en PULL) acredita la fuerza de la semana |
| ¿Qué acredita HYPERTROPHY? | Cualquier ESTRUCTURAL o **RESISTIDO**: también TRUNK_FLEXION o TRUNK_ROTATION con zona `back` (1261, 1308, 1259) |
| ¿Es por sesión? | **No: semanal y por capacidad**, no por patrón ni por función |
| ¿Exige lo no factible? | No: `factibilidad` retira la capacidad si no hay candidatos con dosis |

## 4. Dependencias de V23 y V25 (por qué no se cambian globalmente)

| Componente | Usa | Efecto de cambiar la regla global |
|---|---|---|
| `SlotCoverageGate` y `PlanLimitationsService` | `SlotCoveragePolicy` | Cambian los avisos de limitaciones y la factibilidad previa, que también ven los planes históricos |
| `PlanInputSnapshot` (contrato de la IA) | Slots elegibles y capacidades factibles | Cambia el JSON que recibe la IA con la política histórica. `ContratoV4CIATest` exige que sea idéntico |
| `PrescriptionPrinciples` | Slots | Cambia el texto del prompt |
| `GoalFeasibilityPolicy` | Slots y capacidades | Cambia qué objetivos son factibles con cada equipamiento |
| `CapabilityWeekBuilder` y `PlanGenerationPipeline` | `CapabilityCoveragePolicy` | Cambia **la generación** de los objetivos por capacidades |
| Planes ya publicados | Se revalidan con su versión | Un cambio global los reinterpretaría (contra el versionado V44) |

**Decisión:**
- No se cambian V23 ni V25.
- Se añade **VEF**, que solo existe en las versiones `*-EF-0.1`:
  - **por sesión**: compatibilidad patrón-enfoque y ancla obligatoria;
  - con el número de candidatos del ancla, para que la falta de tracción **se diga** (NO_FACTIBLE por catálogo o equipamiento) en lugar de callarse.
- V23 y V25 siguen como están en todas las versiones.
- Con la histórica, el JSON y el prompt de la IA no cambian (el texto de EF solo se añade con una versión EF).

## 5. Lo que queda abierto (requiere decisión)

1. **¿V23 por sesión?** Con EF, el ancla cubre la parte esencial (PULL con tracción) sin tocar la semana. Hacer V23 por sesión cambiaría la generación de todos los objetivos.
2. **¿V25 con función?** Que la STRENGTH de un PULL deba venir de una tracción ya lo garantizan EF y su ancla en el PULL. Que la fuerza **semanal** deba cubrir cada patrón es una decisión profesional, no técnica.
3. **`ENFORCE_AI_ONLY`**: V23 no bloquea al motor de reglas en producción. Es una decisión previa (documentada en `SlotCoverageEnforcement`) que esta orden no cambia.
