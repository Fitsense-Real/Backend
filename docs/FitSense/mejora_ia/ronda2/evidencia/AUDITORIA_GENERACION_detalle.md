# Auditoría (solo lectura) · Generación INICIAL del plan semanal bajo `PLAN-REV-2.0`

Repositorio: `/home/user/wt_cat` (rama `experimento/mejora-ia`, HEAD `42d9dc1`). Raíz del código citado:
`src/main/java/main/web/services/fitsense/planning/` (en adelante se omite).

## 0. Alcance, versión auditada y supuestos

- **Validador, políticas, snapshot y motor de reglas**: sin cambios locales; se citan con líneas del árbol actual.
- **`PlanPromptBuilder.java` tiene cambios SIN COMMIT** (otra sesión añadió las variantes `pr-1.8a/b/c`; el archivo
  pasó de 551 a 667 líneas durante esta auditoría). Para tener una referencia estable se audita el texto de **HEAD**
  (copia en `scratchpad/r2/snap/PlanPromptBuilder_HEAD.java`). Las líneas `PlanPromptBuilder:NNN` se refieren a esa
  copia. Las variantes de la ronda 2 quedan fuera de alcance; solo se comentan al final.
- **La política REV2 no es la de por defecto.** `PlanningPolicySelector:28` usa por defecto `PLAN-HIST-1`. REV2
  rige solo con `fitsense.planning.policy=PLAN-REV-2.0` y `allow-candidates=true`, y únicamente para usuarios sin
  planes (después manda el linaje: `WeeklyTrainingPlanCommandServiceImpl` ~líneas 203-213). El informe da REV2 por
  activa, como pide el encargo.
- **Modos de aplicación vigentes** (`src/main/resources/application.properties`): cobertura 4C
  `ENFORCE_AI_ONLY` (:89), dosis 4D `ENFORCE` (:102), capacidades `ENFORCE` (:106). En consecuencia, **V23 solo
  rechaza borradores de la IA**; el del motor de reglas solo se registra en el log (`SlotCoverageEnforcement.bloqueaA`).
- Flags de REV2 (`PlanningPolicy:71`): `v4Segundos`, `Duracion.DOSIS_SEMANAL` (sin piso V20), `v4Maximo`
  (tolerancia 0 %), `volumenSemanal` (V26), `pullReal` (V27), `impacto`, `alternativasDeterministas`, `fatigaGM`,
  `sustitucionDesmotivacion`, `reparaCatalogoInactivo`.
- No he ejecutado Maven ni pruebas. Las cifras de duración que aparecen son cálculos hechos a mano con
  `SessionDurationEstimator` y los parámetros por defecto (calentamiento 300 s, transición 60 s, 3 s/rep;
  `PrescriptionParams:105-108`) y los límites de repeticiones de las migraciones V18/V20 (min_reps 6, máximo 30;
  `lower legs` 12, `waist` 10; principiante ≥ 8, `PrescriptionParams:84-89`).

## 1. Recorrido común a los cuatro objetivos

### 1.1 Pipeline de generación inicial
`application/internal/commandservices/PlanGenerationPipeline.java`:
1. Hasta 3 intentos de IA. Cada salida pasa por `PlanDraftNormalizer.normalize` (corrige el tipo de prescripción y
   **recalcula `expected_duration_minutes`**, `PlanDraftNormalizer:83-93`) y después por
   `validator.validate(draft, context, divisor)` (:127).
2. Si los tres fallan: motor de reglas (:157-158). Con REV2, si también falla, se prueban hasta 4 semillas
   deterministas (D-G3, :159-166).
3. Para un primer plan de `INCREASE_STRENGTH`, antes de generar se ejecuta `CoberturaFuerzaTiempo.evaluar`
   (`WeeklyTrainingPlanCommandServiceImpl.generate`, ~:240-252), que puede lanzar `FuerzaSinTiempoException`.

**La generación inicial usa siempre la sobrecarga de 3 argumentos**, que pasa `ConservationReport.vacio()`
(`PlanDraftValidator:128-130`, `ConservationReport:177`). Por tanto **no hay ninguna `DosisAutorizada`**:
`conservation.autorizacionDe(...)` devuelve siempre `Optional.empty()` (`ConservationReport:173-175`).

### 1.2 Orden de V1–V27 en `PlanDraftValidator.validate` (:132-169)
V1/V6/V7/V2/V3/V12/V13 (`validateExercisesAndDays` :685) → V15 (:787) → V23 (:948) → V4/V5/V7/V9/V10 (:1021) →
V8 (:1105) → V11 (:1140) → V16/V17 (:1234) → V18 (:1322) → V22 (:1661) → sugerencia de división → V19 (:1372) →
V20/V21 (:1492) → V24-GM (`validateMvpDose` :535) → V24 por capacidad / evidencia de hipertrofia (:237/:310) →
V26 (:1577) → V27 (:1638) → V25 (:347). Se devuelven todos los problemas a la vez (sirven de entrada al reintento).

### 1.3 Reglas idénticas para los cuatro objetivos

| Aspecto | Regla | Código |
|---|---|---|
| Frecuencia | `effectiveDaysPerWeek = min(días pedidos, 4 BEGINNER / 6 resto)` | `PlanGenerationContext:154`, `FrecuenciaSemanal` |
| Días disponibles ≥ días/semana | Garantizado por el perfil y la base de datos | `UserProfile.applyAvailability`, `V1__users_and_profiles.sql:78` |
| Minutos de sesión | 30–75 para todos los objetivos | `GoalType.admiteMinutos` |
| División | Independiente del objetivo: 1 día FULL_BODY; 2 FB/FB o UB/LB; 3 FB×3 o U/L/U; 4 U/L/U/L; 5 PUSH/PULL/LEGS/UB/LB; 6 PPL×2; sin tracción y ≥ 5 días → U/L alternos (P-G3) | `WeeklySplitPlanner:58-84`, `PlanGenerationContext:174` |
| Dificultad máxima | 1/2/3 según nivel (o la del ajuste); filtrada en la consulta y verificada por V6 | `PlanningProfile:32`, `PlanDraftValidator:688-697` |
| Equipamiento, lugar, seguridad, bloqueos, dolor | Solo mediante el conjunto elegible (consulta + ACL) → V1 | `ExternalCatalogService:120-164`, `WeeklyTrainingPlanCommandServiceImpl:162-177` |
| Filtro funcional | Zonas `neck/lower arms/cardio` y nombres con "estiramiento" excluidos, salvo que el patrón sea ruta de una capacidad del objetivo (LOCOMOTION_CARDIO→CARDIO en LW y GF; MOBILITY→MOBILITY en GF): entran como *complementos* | `CatalogEligibilityPolicy:68-71,130-140` |
| V4 (REV2) | Segundos estimados del contenido `> M×60` → rechazo (tolerancia 0 %), declare lo que declare la sesión | `PlanDraftValidator:1029-1038`, `PlanningPolicy:98,117` |
| V20 (REV2) | No aplica: `minimumSessionMinutes = 0` | `PlanDraftValidator:1540` |
| V17 | Tolerancia del 20 %, pero el normalizador ya fija lo declarado = estimado (ver H14) | `PlanDraftValidator:1287-1299`, `PlanDraftNormalizer:83-93` |
| V21 | Descanso ≤ 180 s | `PlanDraftValidator:1497-1502` |
| V16 | Repeticiones entre min_reps (zona y nivel) y 30 | `PlanDraftValidator:1243-1262` |
| V26 (REV2) | Techo de series directas por grupo, **igual para todos los objetivos**: sesión 6 (BEGINNER) / 10; semana 12 / 20 (INTERMEDIATE) / 24 (ADVANCED). Solo cuentan las clases ESTRUCTURAL y RESISTIDO, sin complementos | `DosisSemanalPolicy:34-45`, `PlanDraftValidator:1577-1630` |
| V27 (REV2) | Una sesión PULL debe tener un HORIZONTAL_PULL o VERTICAL_PULL si hay alguno elegible | `PlanDraftValidator:1638-1653` |
| V15 | Grupos ajenos (salvo FULL_BODY); mínimo `min(2, grupos prescribibles, ejercicios de trabajo)`; regla de la mitad si el enfoque tiene ≥ 3 grupos y la sesión ≥ 4 ejercicios de trabajo | `PlanDraftValidator:787-872` |

### 1.4 Clase y capacidad de cada colocación
`CapabilityPrescriptionPolicy.claseDe` (:73-86) clasifica por patrón de movimiento: los cuatro slots de 4C →
ESTRUCTURAL; otro patrón de fuerza → RESISTIDO; LOCOMOTION_CARDIO → CARDIO; MOBILITY; BALANCE_CONTROL; OTHER o nulo
→ SIN_CAPACIDAD; ids en revisión → EN_REVISION. La capacidad se **acredita por la dosis exacta**
(`acreditada`, :254-261; `Dosis.coincideCon`, :160-167: series entre `series` y `seriesMax`, descanso exacto y
repeticiones o segundos exactos).

Dosis por capacidad, fuera de GAIN_MUSCLE (`CapabilityPrescriptionPolicy.dosis` :189-236), una vez aplicado el
mínimo de repeticiones de V16:

| Capacidad | No principiante | BEGINNER | Rango de series admitido |
|---|---|---|---|
| STRENGTH (solo ESTRUCTURAL, SETS_REPS) | 3×**6** (base 5, sube al mín. 6; lower legs 12, waist 10) / 150 s | 2×8 / 120 s | `series..3` (`ampliable`, :238-241) |
| HYPERTROPHY (no GM) | estructural 3×10 / 90; relleno 2×10 / 90; zona pequeña +3 reps | series limitadas a 2 | `series..3` |
| MUSCULAR_ENDURANCE | 3×18 / 45 (DURATION isométrico: 3×40 s / 45) | 2×15 / 45 | `series..3` (en DURATION, fijo) |
| CARDIO (solo DURATION) | 3×120 s / 60 | 3×90 s / 60 | fijo |
| MOBILITY | DURATION 2×30 s / 15; SETS_REPS 2×10 / 30 | igual | fijo |
| BALANCE | DURATION 2×30 s / 30; SETS_REPS 2×10 / 30 | igual | fijo |
| POWER | nunca se prescribe | — | — |

Tope de colocaciones por día fuera de GM: `MAX_COLOC_DIA = 8` (:60), verificado en V24 (`PlanDraftValidator:253`).

---

## 2. Análisis por objetivo

### 2.1 GAIN_MUSCLE (GM) · régimen 4D (dosis fijas por clase)

**Capacidades.** Principal HYPERTROPHY; STRENGTH declarada como secundaria pero no asignable
(`GoalCapabilityPolicy` MATRIZ; `CapabilityPrescriptionPolicy.asignables:113`). El 25 % de fuerza secundaria no se
valida (`MvpDosePolicy`, javadoc).

**Selección.** Solo cuentan ejercicios ESTRUCTURAL o RESISTIDO de repeticiones. V24 rechaza todo lo demás:
DURATION, movilidad, cardio, OTHER y ejercicios en revisión (`validateHypertrophyEvidence`, `PlanDraftValidator:310-338`).
El snapshot ya no ofrece esos ejercicios (`PlanInputSnapshot:629`) y el motor de reglas los excluye
(`RuleBasedTrainingPlanGenerator`, filtro `puedeDemostrarHipertrofia`). Tampoco hay complementos: cardio y movilidad no
son capacidades de GM.

**Dosis fija por clase** (`MvpDosePolicy.ClaseDosis.dosis`, :257-264; `HYPERTROPHY(3,10,90)`, :191):

| Clase | Dosis | `series_max` |
|---|---|---|
| ESTRUCTURAL_ESTANDAR (slot 4C) | 3×10 / 90 s | 3 |
| ESTRUCTURAL_ZONA_PEQUENA | 3×13 / 90 s | 3 |
| RELLENO_ESTANDAR (sin slot) | 2×10 / 90 s | 2 (3 en PULL + upper arms, 4D v3.3, `seriesMaxEn` :178) |
| RELLENO_ZONA_PEQUENA | 2×13 / 90 s | 3 (4D v3.2) |

V24 (`validateMvpDose`, :535-681) exige la dosis exacta, el tope de colocaciones `maxColocaciones(min)` (6 hasta
52 min, 7 de 53 a 60, 8 de 61 a 68, 9 de 69 a 75; `MvpDosePolicy:88`) y un presupuesto diario
`min × 0,85 × 60` calculado con la fórmula 4D, que cobra el descanso tras cada serie (:671-676). **Con REV2 la serie
adicional de 4D v3.2/v3.3 se rechaza siempre** (:626-634):
```java
// P-G1: sin piso de duracion la serie adicional NUNCA es necesaria, asi que V24 la rechaza siempre
if (!ampliadas.isEmpty() && context.policy() != null && !context.policy().pisoV20())
```

**Volumen.** El objetivo semanal por grupo grande es 6 (BEGINNER), 10 (INTERMEDIATE) o 12 (ADVANCED); la mitad en
zonas pequeñas (`DosisSemanalPolicy.objetivoSeriesSemana:54-66`). No se valida: solo se usa en `CalidadDosis` (traza y
aviso `DOSIS_LIMITADA`) y como objetivo de relleno del motor de reglas (`RuleBasedTrainingPlanGenerator:247`,
`rellenarPorDosis`). El techo de V26 sí se valida.

**Slots 4C.** V23 se aplica (`exigeSlotsResistidos(GM)=true`, `GoalFeasibilityPolicy:389-393`) solo a borradores de
la IA y solo si caben por número de posiciones. Mira solo el patrón, no la dosis.

**V25.** HYPERTROPHY al menos una vez en la semana. Es redundante con V24, que ya exige la dosis exacta.

**Duración.** V4 al 100 % en segundos y, además, el presupuesto 4D del 85 %. Sin V20.

**Qué se envía a Replicate.** `dosis_por_clase` (`PlanInputSnapshot:556`), `max_colocaciones_dia = maxColocaciones`,
`minutos_utiles_dia` (:558-563), `slots_exigibles` (:566), y en cada ejercicio `slot` y `clase_dosis`
(`etiquetasDe`). `capacidades` = null. En el texto: el punto 10 de los principios (`PrescriptionPrinciples:164-189`),
la regla 16 con la cesión ante `clase_dosis` y los costes por clase en DURACIÓN CALCULADA (`PlanPromptBuilder:529-539`).
**No reciben** `reglaPrioritaria` ni `BLOQUE_CAPACIDADES` (`PlanPromptBuilder:152-160,292-293`).

**Interacción V15 / V24 / V26 (pregunta del encargo).** Con 3×10 fijo y techo de sesión 6 (BEGINNER), un grupo
admite como máximo **2 colocaciones estructurales por sesión** (6 series), o 1 estructural + 1 relleno (5), o
3 rellenos (6). V15 con 6 colocaciones permitiría 3 del mismo grupo; por tanto, en principiantes V26 es más
restrictivo que V15. Con techo semanal 12 caben como mucho 4 estructurales del grupo en la semana (frente a un objetivo
de 6 series = 2 estructurales). En INTERMEDIATE y ADVANCED (techo 10) caben 3 estructurales (9). En LEGS y PULL
(2 grupos) la mitad de V15 no aplica y V26 es el único límite. **No hay contradicción que haga imposible un plan**:
sin piso V20, quitar colocaciones siempre está permitido (V5 exige solo 2). El riesgo real es que el modelo no conoce
V26 en PR-1.7 (H3).

### 2.2 INCREASE_STRENGTH (IS) · por capacidades

**Capacidades.** Principal STRENGTH; secundarias HYPERTROPHY y POWER (condicional y nunca prescrita).

**Estructura (motor de reglas).** `CapabilityWeekBuilder`: abre cada sesión con 4 colocaciones resistidas
principales (3 si la sesión dura menos de 30 min, lo que nunca ocurre con un perfil de 30–75 min), las estructurales a
STRENGTH y el resto a HYPERTROPHY (`resistidosPrincipales:604-611`, `capacidadesResistidasPrincipales:628-632`). Da
prioridad a los slots pendientes y completa por dosis (REV2). La última sesión cierra los slots que falten (:251-266).

**Dosis.** STRENGTH 3×6/150 (BEGINNER 2×8/120, admite hasta 3 series); HYPERTROPHY estructural 3×10/90 (BEGINNER 2),
relleno 2×10/90. Ejercicios RESISTIDOS solo con HYPERTROPHY. ME, CARDIO, MOBILITY y BALANCE no son asignables, así que
V24 rechaza los ejercicios de esas clases y el snapshot no los ofrece (`PlanInputSnapshot:625`).

**Validación.** V24 por capacidad (:237-300): dosis exacta y ≤ 8 colocaciones. V25: STRENGTH semanal + `slotsConFuerza`
(:442-479), que exige cada slot factible con dosis STRENGTH en la semana. `exigibles = min(factibles, sesiones×4)` no
limita nada en la práctica porque siempre hay ≥ 1 sesión × 4. V23 exige cobertura por patrón con cualquier dosis, solo a
la IA. Pre-chequeo `CoberturaFuerzaTiempo` (primer plan, :35-63): para 1 día × 30 min, INTERMEDIATE/ADVANCED necesita
4×354 + 300 + 180 = 1.896 s > 1.800 s → `FuerzaSinTiempoException`.

**Qué se envía.** `capacidades` (principales, secundarias, `principales_factibles`, `no_factibles`, `dosis` codificadas,
`candidatos_escasos`), `max_colocaciones_dia = 8`, `slots_exigibles`, y en cada ejercicio `slot` y `dosis` (códigos).
Texto: `reglaPrioritaria` + **`reglaDeFuerza`** (solo IS, `PlanPromptBuilder:353-364`: "Referencia: 4 por sesión",
cuatro slots con STRENGTH), `BLOQUE_CAPACIDADES`, `comprobacionFinal` + `comprobacionDeFuerza`.
`target_reps/target_sets = 6/4` (`TargetPrescription:25`), cifras que contradicen el máximo de 3 series de V24 (H6).

### 2.3 LOSE_WEIGHT (LW) · por capacidades

**Capacidades.** Principal CARDIO (adenda 4A v6); secundaria STRENGTH.

**Estructura (motor de reglas).** Dos bloques de CARDIO por sesión si hay ejercicios (`CapabilityWeekBuilder:232-239`).
No abre colocaciones resistidas principales (:610). Con REV2 rellena primero el trabajo resistido mínimo de los grupos
(4 series/semana por grupo grande; 0 en zonas pequeñas, `DosisSemanalPolicy:65`) con STRENGTH y después más cardio,
hacia un objetivo aeróbico de 45 min (BEGINNER) o 150 min (resto) de trabajo (`DosisSemanalPolicy:79`). El techo lo
pone V4.

**Dosis.** CARDIO 3×120 s/60 (BEGINNER 3×90); STRENGTH solo estructurales 3×6/150 (BEGINNER 2×8/120, hasta 3 series).
RESISTIDO, MOBILITY y BALANCE no aportan ninguna capacidad de LW: V24 los rechaza ("no aporta ninguna capacidad", :259-264).

**Validación.** V24 por capacidad; V25: CARDIO semanal y **CARDIO en cada sesión donde sea factible**
(`cardioPorSesion`, :388-421; el segundo bloque es solo preferencia); V23 **no aplica** (`exigeSlotsResistidos(LW)=false`,
:956-961), coherente con que `slots_exigibles` viaje a null (`PlanInputSnapshot:564-566`). V15: los cardios de zona
"cardio" son complementos y no cuentan.

**Qué se envía.** `capacidades` (con CARDIO como única principal), `max_colocaciones_dia = 8`; sin `slot`, sin
`slots_exigibles`. Texto: `reglaPrioritaria` con "pon dos bloques de CARDIO por sesión" (:332-334),
`BLOQUE_CAPACIDADES`, `comprobacionFinal`. `target 3×15/45`.

### 2.4 GENERAL_FITNESS (GF) · por capacidades

**Capacidades.** Cuatro principales: STRENGTH, MUSCULAR_ENDURANCE, CARDIO y MOBILITY. Secundarias HYPERTROPHY y
BALANCE (oportunista).

**Estructura (motor de reglas).** 3 resistidas principales por sesión (2 si < 30 min); las estructurales que cubren un
slot pendiente van a STRENGTH y una por sesión a ME (:607-650); además, 1 CARDIO y 1 MOBILITY por sesión (:232-243) y
BALANCE una vez por semana si cabe (:345-356).

**Dosis.** Ver la tabla de §1.4. ME admite isométricos (DURATION 3×40 s).

**Validación.** V24 por capacidad; V25: cada principal factible **al menos una vez en la semana** (no por sesión);
V23: los 4 slots por patrón, con cualquier dosis, solo a la IA. V25 no exige STRENGTH en cada slot: el reparto
"slots con fuerza" lo hace solo el motor de reglas.

**Qué se envía.** `capacidades`, `max_colocaciones_dia = 8`, `slots_exigibles` y el `slot` de cada ejercicio, pero
**ningún texto del prompt PR-1.7 menciona los slots para GF** (H5). `reglaPrioritaria` pide las 4 principales "en ESA
sesión". `target 3×12/60`.

---

## 3. Comprobaciones específicas

### 3.1 V15: regla de la mitad y `max_per_body_part`
- Cálculo (`PlanDraftValidator:855-871`):
  ```java
  if (esperados.size() >= 3 && deTrabajo.size() >= 4 && mitadAplicable) { ... if (cuantos > deTrabajo.size() / 2) ...
  ```
  `esperados` son los grupos del enfoque (FULL_BODY 5, UPPER_BODY 4, LOWER_BODY 3, PUSH 3; PULL y LEGS 2; CORE 1).
  `deTrabajo` son las colocaciones sin complementos. División entera: 4 o 5 ejercicios → máximo 2 por grupo; 6 o 7 →
  máximo 3. Se cuentan todos los grupos de trabajo, incluidos los ajenos en FULL_BODY. `mitadAplicable` vale siempre
  `true` (P-G2 apagado por defecto, `LaboratorioAuditoria`).
- El mínimo de grupos es `min(2, disponibles prescribibles, deTrabajo)` (:842), con `prescribibleParaV15` (:905-925).
  El comentario de :839 dice "tope en 3" y el código usa 2 (documentación desfasada).
- **`max_per_body_part` siempre es null con REV2.** `PlanInputSnapshot.suggestedSession` (:681-686) calcula
  `tope = minExercises/2` solo si `minExercises != null`. `minExercises` sale de `minSessionMinutesOrNull` (:856), que
  usa `PlanDraftValidator.minimumSessionMinutes`, y esta devuelve 0 cuando `!pisoV20()` (:1540). Con REV2 es null para
  todos los objetivos.
- **Diagnóstico: es un problema del prompt y del snapshot, no del validador.** El validador cuenta sobre los ejercicios
  reales y coincide con el motor de reglas (`ExerciseSelector.halfCap`, `CapabilityWeekBuilder.respetaMitad:1007`).
  El snapshot derivaba la cuota de un dato de V20 (incluso en HIST era una aproximación sobre
  `min_exercises_per_session`, no sobre el número real), y PR-1.7 remite a ese campo nulo (regla 15,
  `PlanPromptBuilder:197-204`; `comprobacionFinal:384`). PR-1.8 lo corrige con texto propio (:457-460), que es correcto.

### 3.2 V23: qué objetivos exigen la cobertura 4C y cuándo
- `GoalFeasibilityPolicy.exigeSlotsResistidos` (:389) es true si alguna principal tiene la ruta SLOTS_RESISTIDOS:
  **GM (HYPERTROPHY), IS (STRENGTH) y GF (STRENGTH) sí; LW no.** El javadoc de :385-387 ("V23 los exige hoy a TODOS")
  está desfasado.
- Lo exigido es la intersección de los 4 slots con los que tienen candidatos en `context.availableExercises()`
  (`SlotCoveragePolicy.requiredOf`). Solo bloquea si `posiciones >= exigibles` y el borrador viene de la IA
  (`ENFORCE_AI_ONLY`, :972, :997-998). No mira la dosis: en IS la exigencia de fuerza por slot la añade V25
  (`slotsConFuerza`), en GF nadie la añade.
- El snapshot envía `slots_exigibles` a GM, IS y GF (:564-566) con el mismo cálculo que V23, así que son coherentes
  entre sí. Ver H10 (el conjunto sobre el que se calcula) y H5 (en GF no se le explica al modelo).

### 3.3 V24: dosis en la generación inicial y autorizaciones
- GM: dosis fijas por clase (§2.1), sin serie adicional con REV2 (:626-634).
- IS, LW y GF: dosis exacta de una capacidad admisible (:237-300); `series` puede ir hasta `seriesMax = max(series,3)`
  (ver H8).
- **P-A1 (mantenimiento por fatiga), progresión, reducción y herencia NO relajan V24 en la generación inicial.** Ruta
  de código:
  `PlanGenerationPipeline:127/158/191` → `validate(draft, ctx, divisor)` → `validate(..., ConservationReport.vacio())`
  (`PlanDraftValidator:128-130`) → `conservation.autorizacionDe(...)` vacío → en GM `autorizada=false`
  (:599-602, y `ProgresionDosisPolicy.admite4D` ni se llega a llamar); en capacidades la rama `origenAutorizado.isPresent()`
  es falsa (:273-279). `MANTENIMIENTO` exige además `fatigaGM && origen == MANTENIMIENTO`
  (`ProgresionDosisPolicy.admite4D`), y ese origen solo existe en la adaptación (V8C). V25 evalúa el borrador tal cual
  cuando no hay autorizaciones (:164-165).

### 3.4 V25 y V26: comportamiento real
- **V25** (`validateCapabilityCoverage` :347-375): principales factibles omitidas en la semana
  (`CapabilityCoveragePolicy.evaluar:107-160`), más CARDIO por sesión en LW y slots con STRENGTH en IS. La
  factibilidad no tiene en cuenta el tiempo (V4); ver H9.
- **V26** (:1577-1630): suma `plannedSets` por `bodyPartCode` de las colocaciones ESTRUCTURAL o RESISTIDO (cualquier
  tipo de prescripción, isométricos incluidos), excluye complementos y compara con `max(techo, previo)` por sesión y
  por semana. La relajación por "semana previa" depende de que `context.previousWeek().prescriptions()` traiga datos,
  no de la vía de generación (H16). En la semana 1 el techo se aplica tal cual.
- **Relación con V15 y V24**: §2.1 (GM). En LW o IS principiante, la fuerza 2×8 (hasta 3 series) con techo 6 admite
  3 colocaciones de 2 series o 2 de 3. Las tres reglas no producen imposibilidad con REV2 porque no hay piso de
  duración.

### 3.5 Duración (V4, V17, V20) · Frecuencia (V2, V3, V12, V13, V18) · Restricciones (V1, V6)
- V4: correcto en el validador y en ambos generadores (`CapabilityWeekBuilder.excede:1440`,
  `RuleBasedTrainingPlanGenerator.excedeTecho:782`). Si `prescription == null`, cae al techo histórico del 115 % aunque
  rija REV2 (:1039). Es un caso límite. El prompt se contradice (H1).
- V17: redundante tras el normalizador (H14). V20: desactivado con REV2 (correcto según P-G1).
- V2 = `effectiveDaysPerWeek`, igual que `days_per_week` del snapshot. V3, V12 y V13 se verifican por fecha. V18:
  mismo ejercicio o grupos chest/back/shoulders/upper legs en días consecutivos. El enfoque que declara la IA no tiene
  que coincidir con `suggested_split`; solo lo limitan V13, V18 y V27 (observación).
- V1: el conjunto elegible ya aplica seguridad, equipamiento, dificultad, bloqueos, retenidos y dolor. V6: dificultad
  efectiva. En GM y en los objetivos por capacidades, V1 acepta ejercicios elegibles que no se ofrecieron al modelo,
  pero V24 los rechaza.

### 3.6 `CalidadDosis` (INSUFICIENTE) y el perfil O10
- `CalidadDosis.evaluar` (:31-97) **no rechaza nada**. Lo llama solo `PlanningPolicyTrace:91-93` (traza del plan); el
  `estado` DOSIS_LIMITADA se convierte en aviso al usuario (`PlanLimitationsService:83-90`). La clase de sesión
  INSUFICIENTE (`trabajo < 3 || efectivas < 6`, :58) **no la consume ningún validador ni aviso**: es solo traza.
- **O10 (LW, ADVANCED, casa, peso corporal, 1 día × 30 min):**
  - División: 1 FULL_BODY. V2 = 1. V4 ≤ 1.800 s estimados. Sin V20 ni V23.
  - V25: CARDIO en la semana y en la sesión, si existe un LOCOMOTION_CARDIO DURATION elegible. Si no existe, CARDIO
    queda como `no_factibles` y no se exige nada.
  - V24: cada colocación debe ser CARDIO 3×120 s/60 o STRENGTH estructural 3×6/150 (3 series exactas en ADVANCED).
    RESISTIDO y movilidad, rechazados. Máximo 8 colocaciones.
  - V26: techo de 10 series por grupo en la sesión.
  - V15: los cardios de zona "cardio" son complementos; con 0 ejercicios de trabajo el mínimo de grupos es 0.
  - Capacidad de tiempo: 2 cardios = 300 + 2×480 + 60 = 1.320 s; 2 cardios + 1 fuerza = 1.734 s; 3 cardios =
    1.860 s (excede V4). El motor de reglas llegaría a 2 cardios + 1 fuerza (fuerza antes que más cardio,
    `siguientePorDosis:766-776`).
  - **Una sesión de 2 ejercicios / 6 series (p. ej., 2 cardios de 3×120 s) es VÁLIDA para el backend**: V5 (≥ 2),
    V24, V25, V15 (mínimo 0), V4 (1.320 s) y V26 (0). CalidadDosis la marcaría como INSUFICIENTE (solo traza) y la
    semana como DOSIS_LIMITADA (12 min aeróbicos frente a 150). Es un caso aceptado con reparo de calidad, no un
    defecto técnico.
  - El validador tampoco rechaza el mismo `exercise_id` repetido dentro de una sesión (no hay comprobación ni
    restricción en la base: `V4__planning.sql` solo tiene `uq_pwe_order`). El motor de reglas sí lo evita
    (`elegirComplemento:953-955`).

---

## 4. Hallazgos

Gravedad: **A** = puede dejar planes sin publicar por la IA o rompe la coherencia entre prompt y validador de forma
sistemática; **M** = incoherencia relevante; **B** = menor, documental o caso límite. Las observaciones de calidad
deportiva se etiquetan aparte y no son defectos.

**H1 (M · prompt) V4 se describe con dos reglas distintas.** `PlanPromptBuilder:173`:
`4.  expected_duration_minutes no puede superar session_minutes mas 15 %.` Con REV2, V4 rechaza por segundos estimados
`> M×60` (`PlanningPolicy:98,117`). El bloque DURACIÓN CALCULADA da el número correcto (:523-526), así que el modelo
recibe dos límites distintos (115 % frente a 100 %). Además, el campo que menciona la regla 4 lo recalcula el
normalizador.

**H2 (M · prompt/snapshot) La mitad de V15 no se comunica en PR-1.7 con REV2.** `PlanInputSnapshot:681-682`:
```java
Integer tope = (minExercises == null || grupos.size() < 3 || minExercises < 4) ? null : minExercises / 2;
```
`minExercises` es siempre null con REV2 (sin V20). Las reglas 15 y la comprobación final remiten a ese campo nulo. El
validador es correcto. Solo PR-1.8 lo arregla (:457).

**H3 (M · prompt) V26 no se comunica en PR-1.7** (solo en PR-1.8, :464-470). Afecta a los 4 objetivos, en especial a
GM BEGINNER (máximo 2 estructurales 3×10 por grupo y sesión).

**H4 (M · prompt) V27 no se comunica ni en PR-1.7 ni en PR-1.8.** La regla 20 solo habla de bíceps y tríceps. Una sesión
PULL con espalda sin patrón de tracción (p. ej., una extensión de cadera clasificada en "back") pasa la regla 15 del
prompt y la rechaza V27.

**H5 (M · prompt) En GF, V23 no se comunica.** `slots_exigibles` y `slot` viajan (`PlanInputSnapshot:564-566,
635-637`), pero el único texto sobre slots es el punto 10 (solo GM, `PrescriptionPrinciples.textFor`) y `reglaDeFuerza`
(solo IS, `fuerzaPrincipal` en `PlanPromptBuilder:347-350`). La IA de GF se rechaza por V23 sin haber recibido la instrucción.

**H6 (M · prompt) Las reglas genéricas contradicen las dosis exactas de V24 en IS, LW y GF.** La regla 16
(`PlanPromptBuilder:212-218`) manda "Parte de constraints.target_reps y target_sets … No pongas la misma prescripción a
todos los ejercicios" y solo cede ante `clase_dosis`, que es null en estos objetivos. La regla 14 (:196) pide "menos
series, más descanso". Los principios 2 (:72-76), 3 (IS "2-3 series"), 5 ("ADVANCED: 2-3 o más series") y 6 (casa: "sube
repeticiones") empujan en el mismo sentido. `target_sets = 4` en IS y GM (`TargetPrescription:24-25`). Todo esto choca
con `BLOQUE_CAPACIDADES` ("EXACTAMENTE una de esas dosis") y con V24. La regla 14 también contradice la dosis fija de GM.

**H7 (B · prompt) Requisitos que solo existen en el prompt.**
- `BLOQUE_CAPACIDADES` (:105-106, "al menos una vez en la semana") contradice a `reglaPrioritaria` (:322, "en ESA sesión")
  y a `comprobacionFinal` (:379, "Cada sesión tiene al menos una colocación de cada una de …").
- El backend solo exige principales por sesión para el CARDIO de LW. GF (4 principales por sesión), IS (STRENGTH por
  sesión), "dos bloques de CARDIO" en LW y "Referencia: 4 por sesión" en IS no se verifican.
- El orden "empieza por estructurales", el RIR y el ejercicio prioritario al inicio tampoco se verifican.

**H8 (M · coherencia de política) La serie adicional se trata distinto en GM y en los demás objetivos con REV2.**
`CapabilityPrescriptionPolicy:132-138` la justifica "SOLO para llegar al mínimo de duración (V20)", pero
`ampliable` (:238-241) da siempre `seriesMax = max(series, 3)`, y V24 (`coincideCon`) la acepta sin mirar V20. Con REV2,
un BEGINNER de IS, LW o GF puede llevar 3 series en fuerza, hipertrofia o ME, mientras que en GM V24 rechaza siempre la
serie adicional (:628). Es un caso aceptado. Se comunica al modelo (`series_max`), pero no es coherente con su
justificación ni con "BEGINNER: 2 series" (principio 5 y `TargetPrescription.MAX_SETS_BEGINNER`).

**H9 (A · factibilidad, derivado por cálculo) GF INTERMEDIATE/ADVANCED, 1 sesión de 30–35 min: V23 + V25 + V4 no se
pueden cumplir a la vez.** Mínimo para cubrir 4 slots (1 STRENGTH 354 s + 3 ME de 252 s, o 210 s si hay isométricos
estructurales) + CARDIO 480 + MOBILITY 75 + calentamiento 300 + 5 transiciones 300: 2.139–2.265 s, por encima de
1.800 s (o 2.100 s con 35 min). Los 3 intentos de IA se rechazan siempre (por V23 si omite slots, por V4 si los
cubre). Publica el motor de reglas, porque V23 no lo bloquea en `ENFORCE_AI_ONLY`. A diferencia de IS
(`CoberturaFuerzaTiempo`), GF no tiene comprobación previa. BEGINNER sí cabe (≈ 1.638 s).

**H10 (M · condicional al catálogo) "Exigible" se calcula sobre todo el conjunto elegible y no sobre lo prescribible.**
V23 (`ofEligible(context.availableExercises())`, :962-964), V27 (`boolean hay = eligible...`, :1643) y
`sinTraccionReal` (`PlanGenerationContext:174-178`) cuentan también ejercicios que el objetivo no puede prescribir
(DURATION con patrón 4C en GM o IS) y que el snapshot no ofrece (`PlanInputSnapshot:625,629`). V15 sí filtra
(`prescribibleParaV15`) y V25 también (dosis). Si un slot o la tracción solo tienen candidatos de ese tipo (perfiles muy
restringidos), V23 o V27 exigirían algo que V24 prohíbe. No he podido verificarlo con datos reales del catálogo.

**H11 (M · trazabilidad) Con `pr-1.8`, el plan queda registrado como PR-1.7.** `PlanInputSnapshot:665` escribe
`PlanPromptBuilder.VERSION` en `input_snapshot.prompt_version` y `ReplicateTrainingPlanGenerator:132` etiqueta la
petición con `PlanPromptBuilder.VERSION`, aunque exista `promptBuilder.version()` = "PR-1.8-EXP". Resultado: la
evidencia de un plan generado con el bloque experimental dice PR-1.7.

**H12 (B · prompt) La pista de reintento de V24 es la de GM para todos los objetivos.** `PistasDeReintento:41`: "usa
exactamente la dosis de clase_dosis … no lo uses en GAIN_MUSCLE". En IS, LW y GF `clase_dosis` es null y la dosis está
en `capacidades.dosis`.

**H13 (B · PR-1.8) La dosis orientativa es ambigua.** `PlanPromptBuilder:499`: "acercate a ella con los ejercicios y
series que caben". En GM las series son fijas (V24). La propia ronda 2, sin commit, reconoce que este texto provocó un
rechazo V24 en GM y lo retira en pr-1.8a.

**H14 (B · redundancia) V17 nunca rechaza un borrador de la IA**, porque `PlanDraftNormalizer.conDuracionCalculada`
(:83-93) sustituye lo declarado por el estimado antes de validar. El motor de reglas hace lo mismo
(`withEstimatedDurations`).

**H15 (calidad, no defecto) Sesiones "INSUFICIENTES" aceptadas.** INSUFICIENTE (CalidadDosis) es solo traza (§3.6, O10:
2 ejercicios / 6 series es válido). El validador no rechaza un `exercise_id` repetido dentro de una misma sesión.

**H16 (B) La relajación de V26 depende de los datos, no de la vía.** El comentario de :1584-1586 dice que solo se relaja
en semanas derivadas. El código relaja siempre que `previousWeek.prescriptions()` traiga datos, también en una
generación por pipeline con una semana previa con plan (regeneración por API, por ejemplo). En la semana 1 real no
ocurre.

**H17 (B · residuo HIST) El tope de colocaciones de GM sigue derivándose del piso V20.** `MvpDosePolicy.maxColocaciones`
(:88-98) sube a 8 o 9 entre 61 y 75 min por `topeDerivadoDeV20`, cuyo motivo (llegar al 70 %) no existe en REV2. Sigue
acotado por el presupuesto 4D y por V4.

**H18 (B · documentación)** V15 dice "tope en 3" y el código usa 2 (:839-842). El javadoc de `exigeSlotsResistidos` está
desfasado. `CapabilityPrescriptionPolicy` habla de "fuerza 3x5", pero con min_reps 6 se prescribe y exige 3×6
(coherente entre prompt y validador; solo el texto está desfasado).

**Observaciones de calidad deportiva (no son defectos técnicos):**
- P-C2 (impacto) es solo una preferencia del motor de reglas (`CapabilityWeekBuilder:963-968`); la IA no recibe el
  impacto.
- El 25 % de fuerza secundaria en GM y el RIR no se validan, como declara el diseño.
- LW 1×30 queda muy por debajo del objetivo aeróbico (150 min).
- Los topes de grupos pequeños del selector (lower legs 1, waist 2, upper arms 2) son solo del motor de reglas.

**Lo que sí está bien:**
- V24 no se relaja en la generación inicial.
- V25 del CARDIO por sesión en LW coincide entre prompt, motor de reglas y validador.
- V23 no se aplica a LW y el snapshot es coherente con ello.
- `dosis_por_clase` y `capacidades.dosis` salen de la misma fuente que el validador.
- `max_colocaciones_dia`, `min_reps`/`max_reps`, `max_rest_seconds`, `days_per_week`, `max_difficulty_level` y
  `max_session_seconds` son coherentes con V24, V16, V21, V2, V6 y V4.
- V20 se desactiva de forma coherente en el validador y en el texto ("sin minimo de duracion", :516).

## 5. Qué se envía a Replicate (resumen por objetivo)

Petición: `model`, `prompt`, `reasoning_effort`, `json_schema` (`ReplicateTrainingPlanGenerator:180-183`). El prompt
contiene el JSON de `PlanInputSnapshot.of(context)`.

| Elemento | GM | IS | LW | GF |
|---|---|---|---|---|
| `user` (edad, sexo, talla, peso, peso objetivo, nivel, objetivo, texto, salud, equipamiento) | ✓ | ✓ | ✓ | ✓ |
| `constraints` base (fechas, días, `session_minutes`, lugar, dificultad, `max_reps`, `target_reps/sets`, `max_rest_seconds`, `suggested_split`) | ✓ (4×10) | ✓ (4×6) | ✓ (3×15) | ✓ (3×12) |
| `min_session_minutes`, `min_exercises_per_session`, `max_per_body_part` | null | null | null | null |
| `max_session_seconds` (REV2 = M×60) | ✓ | ✓ | ✓ | ✓ |
| `dosis_por_clase`, `minutos_utiles_dia`, `clase_dosis` | ✓ | — | — | — |
| `max_colocaciones_dia` | 6–9 | 8 | 8 | 8 |
| `slots_exigibles`, `slot` | ✓ | ✓ | — | ✓ |
| `capacidades` + `dosis` (códigos) en cada ejercicio | — | ✓ | ✓ | ✓ |
| Filtro de `available_exercises` | solo ESTR/RESIST de repeticiones | con dosis de capacidad | con dosis | con dosis |
| Texto: reglas 1–20, principios P-1.3, DURACIÓN CALCULADA | ✓ (+ punto 10 y costes por clase) | ✓ | ✓ | ✓ |
| `reglaPrioritaria`, `BLOQUE_CAPACIDADES`, `comprobacionFinal` | — | ✓ (+ `reglaDeFuerza`, `comprobacionDeFuerza`) | ✓ ("dos bloques") | ✓ |
| PR-1.8 `coherenciaConValidadores` (solo con `prompt-version=pr-1.8`) | V15, V26, dosis orientativa | V15, V26, dosis | V15, V26, dosis + aeróbico | V15, V26, dosis + aeróbico |

## 6. Matriz de verificación

| Componente | Política vigente (REV2) | Implementación (archivo:método) | Cómo se verifica | Resultado |
|---|---|---|---|---|
| Frecuencia | min(días, 4/6 por nivel); fechas en días disponibles | `PlanGenerationContext.effectiveDaysPerWeek:154`; `WeeklySplitPlanner.plan` | V2, V3, V12 (`validateExercisesAndDays:685`) | Correcto |
| División y recuperación | Mismo enfoque y mismos grupos de recuperación no en días seguidos | `WeeklySplitPlanner.focusesFor:58` | V13 (:745), V18 (:1322) | Correcto (el enfoque de la IA no tiene que seguir `suggested_split`: observación) |
| Cobertura del enfoque | Mínimo 2 grupos prescribibles; mitad con ≥ 3 grupos y ≥ 4 ejercicios | `validateFocusCoverage:787`, `prescribibleParaV15:905` | V15 | Validador correcto; **Hallazgo H2**: PR-1.7 envía `max_per_body_part=null` |
| Slots 4C | GM, IS, GF (no LW); solo a la IA; sin mirar dosis | `validateWeeklySlotCoverage:948`, `GoalFeasibilityPolicy.exigeSlotsResistidos:389` | V23 | Correcto para LW; **H5** (GF sin instrucción), **H9** (GF 1×30 infactible), **H10** (exigible sobre conjunto completo) |
| Dosis GM | Clases 4D fijas, ≤ tope de colocaciones, presupuesto 85 %, sin serie extra | `validateMvpDose:535`, `MvpDosePolicy.ClaseDosis.dosis:257` | V24 | Correcto; **H17** (tope derivado de V20) |
| Evidencia de hipertrofia GM | Solo ESTR/RESIST de repeticiones | `validateHypertrophyEvidence:310` | V24 | Correcto (coherente con el filtro del snapshot :629) |
| Dosis IS/LW/GF | Dosis exacta de una capacidad admisible; ≤ 8 colocaciones | `validateCapabilityDose:237`, `CapabilityPrescriptionPolicy.dosis:189` | V24 | Correcto en validador; **H6** (prompt contradictorio), **H8** (series hasta 3 sin V20), **H12** (pista de reintento) |
| Autorizaciones (P-A1, progresión) | No aplican en la generación inicial | `PlanDraftValidator.validate:128-130` → `ConservationReport.vacio:177` | V24 sin relajación | Correcto |
| Principales factibles | Semanales; LW CARDIO por sesión; IS 4 slots con STRENGTH | `validateCapabilityCoverage:347`, `cardioPorSesion:388`, `slotsConFuerza:442` | V25 | Correcto; **H7** (el prompt exige por sesión en GF/IS) |
| Techo de volumen | 6/10 por sesión; 12/20/24 por semana; ESTR+RESIST sin complementos | `validateWeeklyVolume:1577`, `DosisSemanalPolicy:34-45` | V26 | Correcto; **H3** (no comunicado en PR-1.7), **H16** (relajación por datos) |
| PULL real | PULL con tracción si la hay | `validateRealPull:1638`, `sinTraccionReal:174` | V27 | **H4** (no comunicado), **H10** |
| Duración máxima | Segundos estimados ≤ M×60 | `validateSessions:1021`, `PlanningPolicy.excedeTecho:117` | V4 | Validador correcto; **H1** (regla 4 del prompt dice 115 %) |
| Duración mínima | Sin piso (P-G1) | `minimumSessionMinutes:1539-1542` | V20 inactivo | Correcto |
| Declarado frente a contenido | Tolerancia 20 % | `validatePrescriptionRanges:1287`; `PlanDraftNormalizer:83` | V17 | **H14** (redundante) |
| Repeticiones y descanso | min_reps por zona y nivel, máximo 30; descanso ≤ 180 | `validatePrescriptionRanges:1243`, `validateMinimumDurationAndRest:1497` | V16, V21 | Correcto |
| Series y segundos mínimos | ≥ 2 series, ≥ 6 repeticiones, ≥ 20 s | `validatePrescription` | V9, V10, V7 | Correcto |
| Restricciones y elegibles | Seguridad, equipamiento, dificultad, bloqueos, dolor, filtro funcional | `ExternalCatalogService.elegibles:120`, `CatalogEligibilityPolicy.decidir` | V1, V6 | Correcto |
| Calidad de sesión | INSUFICIENTE si < 3 ejercicios o < 6 series | `CalidadDosis.evaluar:31` | Ninguno (traza) | **H15** (aceptado; O10 2/6 válido) |
| Traza del prompt | La versión registrada debe ser la enviada | `PlanInputSnapshot.of:665`, `ReplicateTrainingPlanGenerator:132` | — | **H11** |

## 7. Nota sobre la ronda 2 sin commit (fuera de alcance)
`pr-1.8c` (`estructuraDelObjetivo`, en el árbol de trabajo) dice "Principales factibles … (al menos una colocacion en
cada sesion)". Para GF e IS repite el requisito que solo existe en el prompt (H7).
