# Diagnóstico del selector de ejercicios

**Alcance:** cómo decide el motor qué ejercicio entra en cada sesión y por qué una sesión PULL sin material puede pasar por válida sin ninguna tracción. Todo sale de leer el código de la rama `feature/c2d-v4c-versionado` (a partir de `6b6ff55`). Primero se diagnosticó; el código cambió después, en `7f92121` y siguientes.

## 1. Pregunta de la orden

> ¿Cómo puede el motor considerar suficientemente cubierta una sesión PULL si los ejercicios seleccionados son empujes, movimientos de tronco o flexiones de codo sin una tracción real?

**Respuesta corta.** Porque ninguna de las piezas que intervienen compara el **patrón de movimiento** con la **función de la sesión**. Se combinan cuatro decisiones, cada una correcta en su contexto:

1. El selector decide la pertenencia a la sesión por la **zona** (`body_part`).
2. La clasificación de capacidad acepta como «resistido» cualquier patrón que no sea cardio, movilidad ni equilibrio.
3. V23 y V25 solo exigen lo que el catálogo elegible **puede** cubrir.
4. El catálogo tiene registros cuya zona no coincide con su patrón.

**No es un fallo de una sola pieza.** Es una combinación de catálogo, selector y validador (§5).

## 2. Cómo decide el selector (motor de reglas)

| Paso | Código | Qué mira | Qué NO mira |
|---|---|---|---|
| Zonas de la sesión | `WorkoutFocus.bodyPartCodes()` — PULL = `back`, `upper arms` | La zona | El patrón |
| Grupo de candidatos | `ExerciseSelector.pool(bodyPartCode, focus)` → filtra `byBodyPart` con `allowedIn` | Zona + V22 | El patrón |
| V22 | `ExerciseSelector.allowedIn` | En `upper arms`, PULL solo admite bíceps y PUSH solo tríceps | Cualquier otra zona |
| Preferencia estructural | `preferirEstructurales` + `deSlot(slot)` | Si el patrón pertenece a un slot de 4C **pendiente en la semana** | Si ese slot es el de la sesión |
| Constructor por capacidades | `CapabilityWeekBuilder`: `resistidos` = clase ESTRUCTURAL o RESISTIDO; complementos y rellenos con `enfoque.accepts(zona)` y `allowedIn` (líneas ~432, 668, 723–741 y 1175) | Zona, V18, V22 | El patrón |
| GAIN_MUSCLE | `RuleBasedTrainingPlanGenerator`: `seleccionables` → `ExerciseSelector.pick`, `pickAdditional`, `estructuralQueSustituyaA` | Zona, dificultad, hipertrofia demostrable | El patrón |

**Clasificación (`CapabilityPrescriptionPolicy.claseDe`):**
- `LOCOMOTION_CARDIO` → CARDIO.
- `MOBILITY` → MOVILIDAD.
- `BALANCE_CONTROL` → EQUILIBRIO.
- Patrón de slot (rodilla, cadera, empuje, tracción) → **ESTRUCTURAL** (puede acreditar STRENGTH).
- **Cualquier otro patrón** (TRUNK_FLEXION, TRUNK_ROTATION, ELBOW_FLEXION…) → **RESISTIDO** (puede acreditar HYPERTROPHY o MUSCULAR_ENDURANCE).
- `null` u `OTHER` → SIN_CAPACIDAD.

**Slots (`MandatorySlot`):**
- HORIZONTAL_PUSH y VERTICAL_PUSH → UPPER_PUSH.
- HORIZONTAL_PULL y VERTICAL_PULL → UPPER_PULL.

El slot depende **solo del patrón**, no de la sesión en la que se coloca.

## 3. Lo que ocurre en el PULL con peso corporal

Datos de la auditoría anterior (`implementacion_controlada_c2d_v4c/AUDITORIA_CATALOGO_CASOS_EXTREMOS.md`), reproducidos ahora en `CASOS_EXTREMOS_ELEGIBLES.csv`:

- Con equipo `body weight`, las zonas `back` y `upper arms` (bíceps) tienen **19 candidatos elegibles y ninguno HORIZONTAL_PULL ni VERTICAL_PULL**.
- Entran en el PULL:
  - **775** «elevación de codo y push-up invertido»: zona `back` con patrón **HORIZONTAL_PUSH**. Es ESTRUCTURAL y acredita UPPER_PUSH y STRENGTH.
  - **1066** (extensión lumbar, TRUNK_EXTENSION).
  - **1259** (rotación de tronco sin carga).
  - **1261** y **1308** (TRUNK_FLEXION), los tres últimos con zona `back`. Todos son RESISTIDO, admisibles como hipertrofia.
  - **320** (flexión de codo sin carga externa) y **315** (equipo «peso corporal», pero las instrucciones piden una mancuerna y un banco).
- En R0800, la única dosis de fuerza del PULL es **775, un empuje**.

## 4. Por qué los validadores no lo detectan

| Regla | Qué comprueba | Por qué no salta |
|---|---|---|
| V15 | Cada zona de la sesión tiene un ejercicio de trabajo | Comprueba **zonas**: `back` está cubierta por 775, 1066… |
| V22 | Bíceps y tríceps en PUSH y PULL | Solo `upper arms` |
| V23 (4C) | La semana toca los slots obligatorios | 1) Exige solo los slots **factibles con el conjunto elegible** (`SlotCoveragePolicy.requiredOf(ofEligible)`): sin tracción elegible, UPPER_PULL **no se exige** y nadie lo dice. 2) Es **semanal**, no por sesión. 3) Con el modo de producción `ENFORCE_AI_ONLY` **no bloquea los borradores del motor de reglas**. 4) No se aplica a LOSE_WEIGHT (4A v6). |
| V25 (4D) | Las capacidades principales factibles tienen al menos una colocación con su dosis | STRENGTH queda acreditada por **cualquier ESTRUCTURAL**, también un empuje colocado en PULL. HYPERTROPHY, por cualquier RESISTIDO, también un patrón de tronco |
| V20 / C2-D | Duración mínima / saturación | Miden tiempo y contenido **admisible**. Si lo admisible no es pertinente, la saturación se demuestra igual: C2-D lo publicaba |

## 5. Atribución del defecto

| Pieza | ¿Defecto? | Detalle |
|---|---|---|
| **Catálogo** | **Sí, en parte** | Registros con zona y patrón incoherentes (775), patrones de tronco o movilidad con zona `back` (1259, 1261, 1308), equipo incoherente con las instrucciones (315). Ver `MATRIZ_CATALOGO_INCONSISTENCIAS.csv` |
| **Selector** | **Sí** | Pertenencia por zona sin compatibilidad de patrón. Con un catálogo perfecto, un empuje bien etiquetado en `back` seguiría entrando en PULL |
| **V23** | **Diseño, no error** | Exigir solo lo factible evita pedir lo imposible. Lo que falta es **decir** que el slot no es factible y que la sesión no tiene su función, en lugar de callarlo |
| **V25** | **Diseño insuficiente** | Acredita la capacidad por la clase del patrón, sin mirar en qué sesión se coloca |
| **C2-D** | **Hereda el defecto** | Decide sobre la saturación del contenido admisible; no lo crea, pero lo publicaba |

**Conclusión: la causa es combinada.**
- La causa raíz de la «falsa tracción» es la **ausencia de compatibilidad patrón-enfoque** en el selector y en el validador.
- El catálogo la agrava con registros incoherentes.
- Corregir solo el catálogo no basta: aunque 775 se reetiquetara, el PULL sin material seguiría **sin ninguna tracción**, porque el catálogo no la tiene con ese equipo.

## 6. Qué se cambió (después del diagnóstico)

**La capa experimental EF-0.1** (`ElegibilidadFuncional`, ver `ELEGIBILIDAD_FUNCIONAL_EXPERIMENTAL.md`):
- Solo actúa con `PLAN-EF-0.1`, `PLAN-C2D-EF-0.1` o `PLAN-C2D-V4-EF-0.1`.
- Está **desactivada por defecto**.
- **No cambia el catálogo** ni las dosis 4D.

**Lo que hace:**
1. **Compatibilidad patrón-enfoque** en el selector, en el constructor por capacidades y en el validador (VEF).
2. **Ancla obligatoria**: PULL necesita una tracción, PUSH un empuje y LEGS un patrón de rodilla o cadera. Si el catálogo elegible no la tiene, el validador lo dice con un **NO_FACTIBLE explícito por catálogo o equipamiento insuficiente**, y nada se rellena con otro movimiento.
3. **Patrones de tronco con zona distinta de `waist`** y el **registro 315** no cuentan como trabajo resistido.
4. **C2-D no exime VEF**: solo actúa si todos los problemas son de V20.

**Lo que no se cambió:**
- V23 y V25 globalmente: ver `AUDITORIA_V23_V25.md` para el análisis de dependencias.
