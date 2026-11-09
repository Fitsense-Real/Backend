# Auditoría del flujo de ADAPTACIÓN, PROGRESIÓN, RECUPERACIÓN y REINCORPORACIÓN (política PLAN-REV-2.0)

- Repositorio: `/home/user/wt_cat` (worktree, rama `experimento/mejora-ia`), commit auditado `42d9dc1`.
- Modo: SOLO LECTURA. Sin Maven, sin ediciones, sin commits. Todas las referencias son `archivo:línea` del commit `42d9dc1`
  (las de `SessionFixPromptBuilder.java` son de `git show HEAD:`; ver nota de WIP al final).
- Política activa auditada: `PlanningPolicy.REV2` (`PLAN-REV-2.0`): `Duracion.DOSIS_SEMANAL` (sin piso V20), `v4Maximo`
  (V4 al 100 %), `volumenSemanal` (V26), `pullReal`, `impacto` (P-C2), `alternativasDeterministas`, `fatigaGM` (P-A1),
  `sustitucionDesmotivacion` (P-A2), `reparaCatalogoInactivo`
  (`planning/domain/model/valueobjects/PlanningPolicy.java:71-74`).
- Configuración por defecto relevante (`src/main/resources/application.properties`): `v8c.pattern-rule=multiconjunto` (l.48),
  `difficulty-guard=off` (l.52), `pain-localized-removal=off` (l.57), `ai.adaptation.prompt-version=v1` → FIX-PR-1.3 (l.60),
  `adaptation.progression.enabled=false` (l.117), `adaptation.capability-validation.enabled=false` (l.120),
  `min-registration-coverage-pct` = 50.0 por `@Value` (`adaptation/.../UserInterventionCommandServiceImpl.java:55`).

Prefijos abreviados: `A/` = `src/main/java/main/web/services/fitsense/adaptation/`, `P/` = `.../fitsense/planning/`,
`S/` = `.../fitsense/shared/`.

---

## 0. Resumen ejecutivo

El flujo está bien acotado: la IA solo puede proponer; V8C (`ConservationValidator`) + la validación estricta
(`PlanDraftValidator`) deciden; tres rechazos terminan en el respaldo conservador; nunca se publica una propuesta rechazada.
La progresión/recuperación/reincorporación es determinista (backend), un paso por ciclo, con techo acumulado 1,10×V_base
y nunca relaja V24 en la generación inicial.

Hallazgos principales (detalle en §6 y §8):

| # | Severidad | Hallazgo |
|---|---|---|
| H1 | Media | V8C acepta en `VARIANTE_MAS_SENCILLA_O_CARGA` (TOO_DIFFICULT) **retiradas puras** (sin sustituto), incluso varias y estructurales; la política de la intervención no las autoriza. |
| H2 | Media | V8C no impide **cambiar el descanso** de un ejercicio que se queda en una sesión señalada con permiso de dosis (fuera de GAIN_MUSCLE y con `capability-validation=false`, que es el defecto). El prompt lo prohíbe. |
| H3 | Media-baja | Factibilidad V2.2: la retirada de relleno se marca `[cabe]` ignorando el **tope de minutos declarado** (M) que V8C sí exige; queda una incoherencia residual en ACORTAR con `MENOS_TIEMPO_HABITUAL`. (O18 no se ve afectado: era parcial, sin M.) |
| H4 | Baja | Contradicciones de texto en FIX-PR-1.3 frente a V8C/REV2: regla 11 (+15 %) vs V4 al 100 %; regla 13 («sube ≤10 %») vs V8C que rechaza cualquier subida de carga; línea DOSIS en GAIN_MUSCLE («Para acortar solo puedes retirar una colocación de relleno») impresa también para INVESTIGAR/VARIANTE/SEGURIDAD. |
| H5 | Baja | V2/V2.1: el bloque de factibilidad contradice al propio prompt en GAIN_MUSCLE+ACORTAR (B «No sustituyas ni retires», D «ninguna», F «OMITELA») — confirmado y corregido por V2.2 (`calcular(..., conRetirada=true)`). |
| H6 | Baja / observación | V26 se relaja con `context.previousWeek()` no vacío, no con «semana derivada»: también aplica a la regeneración determinista P-03 (`SafeRegenerationService`). V24 NO se relaja en ningún camino de generación. |
| H7 | Observación (calidad) | Aceptados técnicamente que merecen revisión: varias bajadas de dosis en INVESTIGAR (el «cambio mínimo» solo lo pide el prompt), magnitud de reducción por fatiga sin cota fuera de GM, reescritura completa de una sesión con dolor de sesión (O12: 8→6 ejercicios, patrones nuevos), y métricas distintas de «no más trabajo» entre la vía IA (volumen 18.1) y P-03 (reps y segundos por separado). |
| H8 | Instrumento | Oráculo K20: `comparar_sesion` empareja por posición cuando las longitudes coinciden; en O04 eso fabrica tres «sustituto de otro patrón» que V8C (por identidad) no ve. Artefacto del instrumento, no defecto del backend. |

---

## 1. Flujo extremo a extremo (con referencias)

1. **Registro de ejecución y motivos.** `S/domain/model/valueobjects/SkipReason.java` (LACK_OF_TIME, FATIGUE,
   LACK_OF_MOTIVATION, TOO_DIFFICULT, PAIN_OR_DISCOMFORT, SCHEDULE_CHANGE, OTHER). `analytics/domain/services/SessionOutcomeIdentifier.java:77-94`:
   una sesión se señala si tiene registro y cumplimiento < umbral de completada; el motivo de la sesión manda **salvo dolor
   en algún ejercicio**, que fuerza PAIN_OR_DISCOMFORT (l.91-94). Al omitir por dolor sin identificar ejercicio,
   `P/application/internal/commandservices/PainMemoryService.java:121-131` (`registrarOmision`) excluye todos los ejercicios
   de la sesión (`DOLOR_SESION_SIN_IDENTIFICAR`); al cerrar el ciclo, `registrarPlan` (l.93-115) hace lo mismo y además
   `DOLOR_IDENTIFICADO` por ejercicio.
2. **Adherencia y cobertura.** `S/domain/model/valueobjects/EvidenceRule.java:45-51`: evidencia = `observada != null` y
   `cobertura >= mínimo` (50 % por defecto). El decisor usa la adherencia OBSERVADA, no la ponderada
   (`A/domain/services/AdaptationOrderDecider.java:36-40`).
3. **Decisión** (`AdaptationOrderDecider.decide`, l.52-161):
   - l.68-83 **dolor primero**: si consta dolor y la semana NO ordenaría adaptar por adherencia (sin evidencia o adherencia
     ≥ buena), `ADAPT_LOCALIZED` solo con las sesiones del dolor y `SEGURIDAD_ANTE_DOLOR`.
   - l.89-95 `INSUFFICIENT_DATA` (cobertura insuficiente); l.98-103 `NO_CHANGE` (≥ umbral bueno, «y NO se progresa»);
     l.120-126 `INSUFFICIENT_DATA` si hay baja adherencia sin sesiones señaladas.
   - l.131-141 alcance: `ADAPT_EXPANDED` (observada < moderado) = todas las señaladas + las de dolor; `ADAPT_LOCALIZED` =
     las de dolor si las hay, si no la peor señalada.
   - l.147 intervención por sesión: `TipoIntervencion.deMotivo`.
4. **Intervención por causa.** `S/domain/model/valueobjects/TipoIntervencion.java:33-102` (permisos sustituir/dosis/revisión
   de seguridad) + permisos derivados: `permiteReprogramar` (l.158, solo REPROGRAMAR_DIAS), `permiteReducirDuracion` (l.175,
   solo ACORTAR), `permiteQuitarEjercicio` (l.184, solo ACORTAR), `permiteReducirCarga` (l.193, REVISAR y VARIANTE),
   `requiereIA` (l.201, no CONSERVAR ni CAUSA_NO_REGISTRADA). La política añade `P/domain/services/PermisosIntervencion.java`:
   P-A2 `sustitucionUnica` (l.27), P-A1 `mantenimientoGM` (l.37), `admiteComoSustituto` (l.60, mismo criterio que V8C).
   Afinado de la falta de tiempo: `P/domain/services/FaltaDeTiempoPolicy.java:36-48` (omitida sin contexto o con M que ya
   cabe → CONSERVAR sin IA; con M menor → ACORTAR con tope M; parcial → ACORTAR sin tope).
5. **Prompt a Replicate.** `P/infrastructure/generation/ai/SessionFixPromptBuilder.java` (HEAD): `VERSION = "FIX-PR-1.3"`
   (l.57, por defecto), V2/V2.1/V2.2 experimentales (l.66, l.77, l.89). Con V2+ se añade el bloque A–F de
   `FactibilidadAdaptacion` (l.348: `FactibilidadAdaptacion.calcular(context, derivation, v22)`).
6. **Validación.** `P/domain/services/AdaptedWeekProducer.produce` (l.267-324): aplica las correcciones, recalcula minutos
   por contenido (l.537-577), repara heredados inseguros (SUST-SEG-1), V8C (`conservation.validate`, l.307) y la validación
   estricta (`revisarYCerrar`, l.637-667 → `StrictValidationAdapter.revisar` → `PlanDraftValidator.validate` 4 args).
7. **Bucle de intentos y publicación.** `P/application/internal/commandservices/AdaptedWeekCommandService.corregir`
   (l.371-470): hasta 3 intentos con realimentación; si ninguno es publicable → `sinPropuestaUsable` (l.469) = respaldo.
   Publica solo `resultado.publicable()`.
8. **SIN_EFECTO / RESPALDO.** `AdaptedWeekProducer.java:316-319`: sin ninguna sesión «aplicada» → `CONSERVED` +
   `Motivo.SIN_EFECTO`. `respaldo` (l.334-385): con dolor → P-03 determinista si todo lo que dolió está en la memoria
   del dolor, si no `NO_FACTIBLE (DOLOR_SIN_RESOLUCION_SEGURA)`; sin dolor → copia (continuidad) validada con
   `validateContinuidad`.

---

## 2. Verificación 1: tipos de adaptación y qué comprueba V8C

### 2.1 Localizada frente a ampliada
Correcto (`AdaptationOrderDecider.java:131-141`). La diferencia está en cuántas sesiones se señalan; el productor las trata
igual (una sola solicitud). Tests: `AdaptationOrderDeciderTest.localizadaTocaUnaSola`, `localizadaEligeLaPeor`,
`ampliadaTocaTodas`, `dolorAntesQueLaPeor`, `dolorConAdherenciaBuena`.

### 2.2 Qué valida V8C por intervención (`P/domain/services/ConservationValidator.java`)

Comprobaciones comunes a toda sesión señalada (`revisar`, l.352-618):
- dolor ANTES de clasificar y sin mirar la intervención (l.446-466 → `revisarDolor`, l.1092-1141);
- «LA IA NUNCA SUBE DOSIS» en series/reps/segundos (`revisarDosisDeLaIa`, l.691-722, rechazo en l.704-709);
- carga: subir o poner carga nueva siempre rechazado; bajar solo si `permiteReducirCarga` (`revisarCarga`, l.1038-1066);
- día solo con `permiteReprogramar`; dosis y minutos solo con `permiteCambiarDosis`; enfoque nunca (`revisarSenalada`,
  l.954-1018);
- tope de minutos declarado M (l.542-552);
- partición: ninguna sesión desaparece sin declararse (`revisarParticion`, l.1487-1535);
- no señaladas: idénticas en día, ejercicios, dosis, enfoque y minutos (`revisarNoSenalada`, l.909-950).

| Intervención (motivo) | Sustituir | Retirar | Dosis | Día | Carga | Regla específica en V8C |
|---|---|---|---|---|---|---|
| REPROGRAMAR_DIAS (SCHEDULE_CHANGE) | No | No | No | Sí | No | solo mover el día |
| ACORTAR_O_REORGANIZAR (LACK_OF_TIME) | No | **1 relleno** | Sí (bajar) | No | No | l.1156-1245: sin añadidos; ≤1 retirada; el retirado debe estar en el catálogo elegible; `MandatorySlot.of(c).isEmpty()`; si se retira, ninguna otra dosis cambia. Exención V20 por fecha (`fechasReducibles`). Tope M (l.542). |
| VARIANTE_MAS_SENCILLA_O_CARGA (TOO_DIFFICULT) | Sí, mismo patrón | (ver H1) | Sí | No | Bajar | mismo patrón (l.1279-1311), multiconjunto H1 (l.1314, l.1442-1460), P-C2 impacto no sube (l.1319-1331), dificultad solo con guard (off). |
| SEGURIDAD_ANTE_DOLOR (PAIN) | Sí, cualquier patrón | Sí | Sí (bajar) | No | **No** | rama de dolor l.1262-1277: añadidos ≤ retirados, volumen 18.1 no sube, dificultad solo con guard; lo que dolió no reaparece; dolor de sesión → ninguno de los de antes sigue (l.1126-1138). |
| REVISAR_CARGA_Y_RECUPERACION (FATIGUE) | No | No | Sí (bajar) | No | Bajar | reducción marcada MANTENIMIENTO para V24 (l.716-719); P-A1 en GM: solo `mantenimiento4D` (3x10→2x10). Avisos FATIGA_REPETIDA/PERSISTENTE (l.600-615). |
| INVESTIGAR_CAMBIO_MINIMO (LACK_OF_MOTIVATION) | P-A2: 1 lateral | No | Sí (bajar) | No | No | `revisarSustitucionPorDesmotivacion` (l.1345-1395): exactamente 1×1, sin cambio de dosis, equivalente estricto (patrón, slot 4C, grupo, músculo, dificultad e impacto no superiores). |
| CAUSA_NO_REGISTRADA / CONSERVAR | No | No | No | No | No | no van a la IA (`sesionesParaIA`); cualquier cambio rechazado; idéntica → sin efecto. |

### 2.3 Puntos concretos solicitados
- **Fatiga:** solo dosis a la baja; en GAIN_MUSCLE solo la dosis de mantenimiento P-A1
  (`P/domain/services/ProgresionDosisPolicy.java:50-64` y `mantenimiento4D` l.73-78). Correcto. Fuera de GM la magnitud
  de la reducción no tiene cota propia (solo pisos V9/V16) → H7.
- **Dificultad:** sustitución del mismo patrón y multiconjunto; impacto no sube con REV2; dificultad no comprobada por
  defecto (guard off: «candidato, pendiente de revisión», l.99-104). Retirada pura aceptada → **H1**.
- **Falta de tiempo:** retirada de exactamente una colocación de relleno; «relleno» = sin `MandatorySlot`
  (ver §3). Correcto. Tope M: correcto en V8C; incoherencia residual en la factibilidad V2.2 → **H3**.
- **Dolor:** ver §4.
- **Restricciones personales:** se aplican en el conjunto elegible (`WeeklyTrainingPlanCommandServiceImpl.java:164-176`:
  `fetchEligibleFor(profile, maxDifficulty, safety)` y resta de exclusiones por dolor); V1/V6/V7 rechazan lo no elegible;
  los heredados que dejaron de ser seguros solo se sustituyen con causa verificada (`neutralizarSustituciones`,
  l.752-830; SUST-SEG-1 en `SafetySubstitutionPolicy`). Correcto.
- **Reducción de dosis:** emitida como `DosisAutorizada.REDUCCION`/`MANTENIMIENTO` solo si mismo tipo y descanso
  (l.712-719); V24/4D la acepta solo en su rango (`ProgresionDosisPolicy`). Correcto, salvo descanso → **H2**.
- **Sustitución de ejercicio:** ver tabla. Correcto.
- **Retirada permitida (ACORTAR):** exactamente una colocación sin `MandatorySlot` y con ficha en el catálogo
  (l.1173-1222). Tests: `ConservationValidatorQuitarRellenoTest.retirarRellenoSeAutoriza`, `retirarEstructuralSeRechaza`,
  `retirarDosRellenosSeRechaza`, `retirarYCambiarDosisSeRechaza`, `retirarYAnadirSeRechaza`, `otraIntervencionNoGanaElPermiso`.
- **Conservación de no señaladas:** `revisarNoSenalada` + `revisarParticion`. Correcto.
- **SIN_EFECTO:** sesión señalada que vuelve igual = `sinEfecto` (l.1016-1017); semana sin ninguna aplicada →
  `SIN_EFECTO` (`AdaptedWeekProducer.java:316-319`). Una sesión con cualquier problema no se clasifica (l.1010).
- **Solo se publica lo autorizado:** `revisarYCerrar` exige cero problemas de V8C y de la validación estricta;
  tras 3 rechazos se publica el respaldo (copia validada con `validateContinuidad`). Correcto.

---

## 3. Caso O18 (GAIN_MUSCLE, ACORTAR_O_REORGANIZAR)

**Backend (V8C) permite retirar UNA colocación de relleno:** `TipoIntervencion.permiteQuitarEjercicio()` (l.184-186) y
`ConservationValidator.revisarSustitucion` l.1156-1245 (relleno = `MandatorySlot.of(candidato).isEmpty()`, l.1207-1219;
sin otra dosis cambiada, l.1233-1240). En GAIN_MUSCLE las demás conservan su dosis 4D exacta, así que V24 no objeta, y
REV2 no tiene piso V20 (además ACORTAR está exento por `permiteReducirDuracion`). **Confirmado.**

**V2/V2.1 NO ofrecían la opción** (`FactibilidadAdaptacion.java`, sin `conRetirada`):
- `dosisReducible = permiteCambiarDosis && (!gm || mantenimiento)` → false en GM+ACORTAR (l.137-139, `porQue = "en GAIN_MUSCLE la dosis 4D no admite reducciones"`);
- no hay sustitutos (ACORTAR no sustituye), no hay cargas (ACORTAR no reduce carga), `retiradas` vacía;
- B (l.394-403) → `"No sustituyas ni retires ejercicios."`
- D (l.410-412) → `"D. OPCIONES: ninguna. No se puede bajar la dosis: ..."`
- E (l.466) → `"; sustituir o retirar ejercicios"` prohibido
- F (l.473-474) → `"OMITELA del arreglo: es la respuesta correcta."`

Y a la vez el contrato de la sesión (todas las versiones) decía lo contrario
(`SessionFixPromptBuilder.java` HEAD l.451-454): `"DOSIS: en GAIN_MUSCLE la dosis de 4D NO se reduce ni se sube ... Para acortar solo puedes retirar una colocacion de relleno."`, más la regla 15 (l.221-247). El test
`MejoraIaPromptsTest.v21SinOpcionDeRetirar` reproduce exactamente el defecto. **Confirmado (H5).**

**V2.2 (`calcular(context, derivation, true)`, l.104 y l.204-219) lo ofrece de forma coherente con V8C:**
- solo con `intervencion.permiteQuitarEjercicio()` (= ACORTAR) y `size > MIN_EJERCICIOS (2)` (coherente con V5);
- omite ejercicios sin ficha en el catálogo (`c == null`, igual que V8C l.1194-1199) y los estructurales
  (`MandatorySlot.of(c).isPresent()`, igual que V8C l.1207);
- el catálogo es el mismo: `context.availableExercises()` = `elegibles` que recibe V8C (`AdaptedWeekCommandService.java:176`);
- B (l.400-402): «No sustituyas ni añadas ejercicios. Puedes retirar UNA colocación de relleno de las de D; si la retiras,
  no cambies la dosis de las que se quedan»; E (l.463-465) prohíbe retirar más de una, una que no esté en D, o retirar y
  además cambiar dosis — exactamente las tres reglas de V8C;
- `hayCambioFactible` incluye `retiradas` (l.87-92) → F ya no manda omitir. Test: `MejoraIaPromptsTest.v22OfreceRetirarElRelleno`, `v22SoloEnAcortar`.

**Incoherencias restantes:**
1. **(H3) Tope M ignorado.** Para ACORTAR `margen` es siempre `null` (l.130: `permiteReducirDuracion() ... ? null`), así que
   toda retirada sale `[cabe]` (l.217). Pero V8C rechaza una sesión aplicada que siga durando más que M
   (`ConservationValidator.java:542-552`) y los minutos los fija el backend por contenido
   (`AdaptedWeekProducer.minutosSegunContenido`, l.550-563). Con una omisión por falta de tiempo con
   `MENOS_TIEMPO_HABITUAL` (que siempre trae M, `FaltaDeTiempoPolicy.java:43-47`), si retirar el relleno no basta para
   bajar de M, el prompt V2.2 dice «retirar UNA es un cambio válido» y V8C lo rechaza. `FactibilidadPreviaIA.acortar`
   (`P/domain/services/FactibilidadPreviaIA.java:79-115`) sí compara con M, pero solo evita la llamada si TODAS las sesiones
   son imposibles. O18 (sesión parcial, sin M; `01_COHORTE_O_MANIFIESTO.md:65` «P») no se ve afectado.
2. **Sin prueba extremo a extremo** que pase la retirada ofrecida por V2.2 por `ConservationValidator`: los tests del
   prompt (`MejoraIaPromptsTest`) y los de V8C (`ConservationValidatorQuitarRellenoTest`) comprueban cada lado por separado.
3. **Redacción de `TipoIntervencion.ACORTAR_O_REORGANIZAR`** (l.44-48): «Puedes bajar la dosis **y** retirar UNA colocación»
   sugiere ambas a la vez; la frase siguiente lo matiza y V8C exige «una cosa o la otra». Menor.
4. `FactibilidadPreviaIA` fuera de GM calcula la cota retirando cualquier colocación (también estructurales, l.89-95), más
   laxa que V8C. Es una cota inferior: solo provoca llamadas innecesarias, nunca rechaza mal. Menor.

---

## 4. Caso O12 (dolor de sesión sin ejercicio identificado, `DOLOR_SESION_SIN_IDENTIFICAR`)

**Política real implementada:**
1. **Exclusión de todos los ejercicios de la sesión.** `PainMemoryService.registrarPlan` l.106-109 (sesión con
   `PAIN_OR_DISCOMFORT` y ningún ejercicio identificado → todos `DOLOR_SESION_SIN_IDENTIFICAR`) y `registrarOmision`
   l.121-131. Tabla `user_safety_exclusions` (`V43__pain_memory_and_safe_regeneration.sql`, `ck_use_reason`). La exclusión
   **no caduca** (Javadoc l.44-46); solo `reactivar` por acción explícita y con control de seguridad (l.180-197). Se resta
   del conjunto elegible en toda preparación (`WeeklyTrainingPlanCommandServiceImpl.java:166-176`), así que la IA no los ve
   y V1 rechaza que vuelvan.
2. **V8C, rama de dolor.**
   - `revisarDolor` l.1126: `generalizar = dolorDeSesion || (preventivo && dolieron.isEmpty())` → ninguno de los ejercicios
     de la sesión medida puede seguir (l.1128-1138). Cambiar fecha, duración o carga no resuelve (tests
     `ConservationValidatorTest.dolorMoverElDiaNoResuelve`, `dolorAcortarNoResuelve`, `dolorSinEjercicioConocido`).
   - `revisarSustitucion` l.1262-1277: **no más añadidos que retirados**; **volumen 18.1 no sube** (`despues > antes`);
     dificultad solo si `limitaDificultad` (guard **off** por defecto) ; **patrones nuevos permitidos** (no se exige patrón,
     l.1249-1261 y regla 9 del prompt); retirar sin reemplazo permitido (l.1247; test `dolorRetiraSinReemplazo`).
   - Dosis: no sube (`revisarDosisDeLaIa`); carga: SEGURIDAD_ANTE_DOLOR **no** puede bajar la carga
     (`permiteReducirCarga` l.193-195) → bajar la carga en una sesión de dolor se rechaza.
   - Lo que limita el contenido nuevo: V1 (elegible, ya sin los excluidos), V6 (dificultad máxima del perfil), V5 (≥2),
     V24/4D (GM), V26 y cobertura 4C semanal.
3. **Respaldo P-03** (si la IA falla): `AdaptedWeekProducer.cubiertoPorMemoriaDelDolor` (l.75-95) exige que todos los
   ejercicios de la sesión estén excluidos; SUST-SEG-1 (mismo slot, patrón, zona…, dosis recalculada) y
   `masTrabajoQueLaMedida` (l.100-125, repeticiones totales y segundos totales por separado).

**Técnicamente permitido (hoy):** reemplazar todos los ejercicios de la sesión por otros elegibles de cualquier patrón,
en igual o menor número y sin subir el volumen 18.1; dificultad mayor permitida dentro de V6 (guard off). En la corrida
R′_O la IA cambió 8 ejercicios por 6 y V8C lo aceptó (descrito en `docs/FitSense/evaluacion_integral_O/herramientas/pruebas_oraculo_v1_3.py:4-6`).

**Pendiente de revisión profesional (declarado en el código):**
- la exclusión de todos es **provisional** (`PainMemoryService.java:34-36`: «se excluyen PROVISIONALMENTE todos»);
- el límite de dificultad ante dolor es **candidato, apagado, pendiente de revisión** (`ConservationValidator.java:99-104`;
  `application.properties:49-52`);
- la retirada localizada por dolor es **candidata, apagada** (`application.properties:53-57`) y solo actúa con el guard encendido
  (`AdaptedWeekProducer.java:205-210`);
- catálogo con «revisión de catálogo pendiente» (nórdicos/pantalla) que el oráculo marca aparte.

**Observación (H7):** la vía IA mide «no más trabajo» con el volumen 18.1 (`DraftExercise.volume(divisor)`, mezcla reps y
segundos/divisor) y P-03 con reps y segundos por separado: dos criterios distintos para la misma regla de política.

---

## 5. Progresión, recuperación y reincorporación

Clase principal: `P/application/internal/commandservices/ProgresionResolver.java` (V34). Se invoca solo sin orden de adaptar y
con `NO_CHANGE` (`AdaptedWeekCommandService.continuidad`, l.271-326). Con `progression.enabled=false` (defecto) y REV2
(`fatigaGM`), **solo** se proponen pasos de RECUPERACIÓN (`soloRecuperacion`, l.281-289).

| Regla | Implementación | Estado |
|---|---|---|
| Cumplimiento sostenido antes de progresar: **dos semanas al 100 % real** | `UMBRAL_PROGRESION = 100.0` (l.122); `dosSemanasPerfectas` (l.333-366): adherencia y cobertura 100 % y todas las sesiones y ejercicios completados en la semana medida y en la anterior | Correcto (`ProgresionCienPorCienIT`) |
| Criterio común (80 %, sin fatiga ni dolor, O→S copia, decisión NO_CHANGE, ejercicios elegibles y sin exclusión) | `cumple` (l.300-325) | Correcto |
| Aumento gradual por paso | progresión: +1 repetición, y +1 serie solo con reps en su máximo (l.592-605); paso de sesión ≤ `progression_increase_pct` (defecto 5, l.611); rango D-P3 (`ProgresionDosisPolicy.admite4D`/`admiteCapacidad`, +2 reps máx.; estructurales GM sin series extra) | Correcto |
| **Techo acumulado 1,10×** | `TECHO = 1.10` (l.74); `base` = volumen de la sesión raíz del linaje (l.219); `techo = floor(base*1.10)` (l.220); `nv > tope → descarta` (l.615) | Correcto (`ProgresionReservaIT`) |
| **Máximo un paso por ciclo** | `continuidad` publica el primer candidato que pasa (l.293-312); `validateContinuidad` admite un único `PasoProgresion`; `pasoEstrecho` (l.625-660) exige que solo cambie su ejercicio; `produce` lanza si hay paso con orden (`AdaptedWeekProducer.java:285`) | Correcto (`ProgresionV24V8CTest.unaModificacionPorCiclo`, `pasoDeDosEjercicios`) |
| Recuperación de dosis dentro de límites | `referenciaDeReduccion` (reducción APLICADA de REVISAR o INVESTIGAR); `recuperacion`/`haciaReferencia` (l.524-570): una dimensión, hacia la referencia **sin superarla**, ≤ `recovery_step_pct` (defecto 10) | Correcto; nota: no se compara con `techo` (está acotada por la referencia) (`RedisenoFatigaGmIT`, `ProgresionRev2IT`) |
| Reincorporación solo tras una retirada previa | `retirado` (l.400-416): tramo ACORTAR APLICADO con exactamente 1 retirado, 0 añadidos, sin sustitución; nunca lo retirado por dolor | Correcto |
| Progresión antes de reincorporar | `pasosTrasRetirada(linaje) == 0 → mantener` (l.264); `progresionesTrasAcortar` (l.474-486) solo cuenta PROGRESION (no recuperación) posterior al último ACORTAR; además no debe caber otra progresión dentro de la reserva (l.258) | Correcto (`ReincorporacionTrasProgresionTest.recuperacionNoHabilita`, `progresionPosteriorHabilita`, `desdeElUltimoAcortamiento`) |
| Reincorporación con dosis inicial exacta, al final, dentro del techo y de M | `exacta` (l.670-695), `v + vRet <= techo` (l.267), `mVigente`/`minutosCon` (l.243-246); V8C exige solo añadir ese ejercicio y sin autorización de dosis (`pasoEstrecho` l.645-647; l.488-491) | Correcto (`Reincorporacion4DTest`) |
| Sin restauración automática | exclusión por dolor no caduca; reactivar ≠ reincorporar (`PainMemoryService.java:171-179`); recuperación paso a paso; `NO_CHANGE` no progresa por sí mismo (`AdaptationOrderDecider.java:97`) | Correcto |
| Vuelta atrás por fatiga tras un paso | `deLaSesion` l.207-212, `vueltaAtras` | Correcto (`ProgresionV24V8CTest.dp7VueltaAtrasConservaExencion`) |

Nota: la recuperación y la reincorporación usan el umbral del 80 %, no el 100 % (Javadoc l.114-121, decisión documentada).

---

## 6. Verificación 5: las autorizaciones de adaptación/progresión NO relajan V24 en la generación inicial

- La generación (inicial, semanal sin orden, P-03) pasa por `PlanGenerationPipeline` con la sobrecarga de 3 argumentos
  (`P/application/internal/commandservices/PlanGenerationPipeline.java:127, 158, 191, 225, 244, 281`), que delega con
  `ConservationReport.vacio()` (`P/domain/services/PlanDraftValidator.java:128-129`).
- V24 en GM solo acepta una dosis distinta de la exacta si `conservation.autorizacionDe(fecha, ejercicio)` existe
  (`PlanDraftValidator.java:599-603`); en capacidades, l.273. Con `vacio()` no hay ninguna, así que V24 aplica la regla
  original. La serie adicional 4D v3.2 se rechaza siempre sin piso V20 («V24 de la generación inicial no se relaja», l.627-633).
- Las autorizaciones (`HEREDADA`, `PASO`, `REDUCCION`, `MANTENIMIENTO`) solo las emite V8C sobre una `WeekDerivation`
  (`ConservationValidator.revisar`), y `DosisAutorizada` lo declara (Javadoc l.11-13: «La construye V8C … nunca la generación»).
  `MANTENIMIENTO` además exige `fatigaGM` (`ProgresionDosisPolicy.java:54-56`).
- La exención de V20 (`fechasConDuracionReducible`) también viene solo del informe de V8C (l.1509).
- Tests: `ProgresionV24V8CTest.generacionRechazaDosisNoExacta`, `generacionSinPermisos`; `RedisenoAdaptacionTest.generacionIntacta`.

**Resultado V24: Correcto.**

**H6 (V26):** `validateWeeklyVolume` (l.1577-1631) toma como techo `max(techo, volumen de previousWeek)` (l.1618, l.1626) siempre
que `context.previousWeek()` traiga prescripciones (l.1589-1603). La condición no es «semana derivada» sino «existe semana
previa»: también afecta a generaciones completas con semana previa, en concreto la regeneración determinista P-03
(`SafeRegenerationService.java:103-105`, `prepare` construye `previousWeek` en `WeeklyTrainingPlanCommandServiceImpl.java:199-201`).
El comentario (l.1584-1586) habla de «semana derivada»/«generación inicial sin semana previa». En el ciclo semanal sin orden
la semana previa suele estar vacía, así que el impacto práctico se limita a P-03. Test existente: `PoliticaRedisenoTest.v26SemanaDerivada`
(no cubre P-03).

---

## 7. Verificación 6: contradicciones, redundancias, aceptados dudosos y rechazos que faltan

### 7.1 Contradicciones prompt ↔ V8C/REV2 (`SessionFixPromptBuilder.java`, líneas de HEAD)
1. **Regla 11 vs V4 REV2.** l.209-210: «no superar session_minutes más 15 %». Con REV2 `toleranciaV4Pct()` = 0
   (`PlanningPolicy.java:98-100`). El mismo prompt añade después el techo correcto (l.288-291, «TECHO DE DURACION … max_session_seconds»).
   Dos techos distintos en el mismo texto. (H4)
2. **Regla 13 vs `revisarCarga`.** l.216-217: «target_load_kg … nunca sube más de un 10 % sobre el usado» sugiere subidas
   admisibles; V8C rechaza cualquier subida o carga nueva en una señalada (`ConservationValidator.java:1048-1053`). La nota
   «EN UNA CORRECCION NO SE PROGRESA … ni peso» (l.277-283) lo corrige: redundancia contradictoria. (H4)
3. **Línea DOSIS en GAIN_MUSCLE** (l.449-454): se imprime para toda intervención con `permiteCambiarDosis` que no sea P-A1
   (VARIANTE, SEGURIDAD, INVESTIGAR y REVISAR con la histórica):
   ```java
   ? "  DOSIS: en GAIN_MUSCLE la dosis de 4D NO se reduce ni se sube: ... Para acortar solo puedes retirar"
     + " una colocacion de relleno.\n"
   ```
   En INVESTIGAR (REV2, P-A2 exige 1×1) una retirada pura se rechaza (l.1367-1371) y «QUE PUEDES CAMBIAR AQUI» dice
   «No retirar ejercicios». (H4)
4. **Regla 9 / V2.2 «DOLOR» vs bloque B de V2 para dolor.** Regla 9 (l.202-207) y V8C permiten otro patrón ante dolor; el bloque
   B de la factibilidad (`FactibilidadAdaptacion.java:395-396`) dice «solo por los SUSTITUTOS ADMITIDOS», lista que solo
   contiene el mismo patrón y dificultad ≤ (`sustitutosAdmitidos`, l.257-262). V2.2 (l.310-312) dice «un patrón nuevo solo si
   la lista no ofrece ninguno», pero la lista nunca ofrece patrones nuevos. Más estricto que V8C (no inseguro), pero incoherente.
5. **V2/V2.1, GM+ACORTAR**: ver §3 (H5).

### 7.2 Redundancias
- La regla «no subir dosis» aparece en la regla 7, en la nota «EN UNA CORRECCION NO SE PROGRESA», en DOSIS y en E de la
  factibilidad; V8C la aplica una sola vez (`revisarDosisDeLaIa`). Aceptable, pero cuatro textos que mantener.
- Regla 15 repite dos veces «conserva exactamente la dosis de las que se quedan» (l.224-231).
- La lista de sustitutos se calcula dos veces (`SessionFixPromptBuilder.contratoDeLaSesion` y
  `FactibilidadAdaptacion.sustitutosAdmitidos`) con el mismo filtro duplicado; hay test de coincidencia (Javadoc l.226-229).

### 7.3 Aceptados que merecen revisión de calidad (no violan la política escrita)
- INVESTIGAR: V8C acepta varias bajadas de dosis en la misma sesión; «UNA sola opción sobre UN ejercicio» solo lo pide
  V2.1 (G, `FactibilidadAdaptacion.java:352-353`) y la preferencia P-A2 «sustituir antes que bajar dosis» tampoco se comprueba.
- REVISAR fuera de GM: cualquier bajada hasta los pisos V9/V16 (p. ej. 4x10→2x6) se acepta.
- VARIANTE: sustitución + bajada de dosis en la misma sesión se acepta (ACORTAR y P-A2 sí lo prohíben).
- Dolor de sesión: reescritura completa con patrones nuevos (O12).
- SEGURIDAD_ANTE_DOLOR no puede bajar la carga sugerida (más conservador de lo necesario; provoca reintentos).

### 7.4 Deberían rechazarse según las políticas existentes y no se rechazan
- **H1 · Retirada pura en VARIANTE_MAS_SENCILLA_O_CARGA.** `TipoIntervencion` (l.50-53) solo autoriza «variante más sencilla
  del mismo patrón, o ajustar la carga» y `permiteQuitarEjercicio()` es exclusivo de ACORTAR (l.184-186). Pero para toda
  intervención con `permiteSustituirEjercicio()`:
  ```java
  if (d.anadidos().isEmpty()) return;   // solo se retiraron: nada que emparejar   (ConservationValidator.java:1247)
  ```
  Así se aceptan, en VARIANTE, quitar uno o varios ejercicios sin sustituto (también estructurales si otra sesión cubre el slot,
  porque la cobertura 4C es semanal: `PlanDraftValidator.validateWeeklySlotCoverage`, l.948-966), o quitar 3 y añadir 1 del mismo
  patrón (multiconjunto solo limita añadidos ≤ retirados por patrón). Con REV2 no hay V20 que lo frene; solo V5 (≥2). No hay test
  que lo cubra (búsqueda en `ConservationValidatorTest`, `V8CConservacionPatronH1Test`). Para SEGURIDAD_ANTE_DOLOR la retirada sí
  está autorizada («retirarlo sin reemplazo también vale», `TipoIntervencion.java:66-68`).
- **H2 · Cambio de descanso.** El prompt dice «El descanso (rest_seconds) de un ejercicio que se queda no se cambia nunca» (HEAD l.282-283),
  pero V8C solo rechaza subidas de series/reps/segundos:
  ```java
  if (mayor(antes.series(), despues.series()) || mayor(antes.reps(), despues.reps())
          || mayor(antes.segundos(), despues.segundos())) { ... }        // l.704-709
  ...
  if (!d.dosisCambiada().isEmpty() && !intervencion.permiteCambiarDosis())   // l.984
  ```
  Un cambio solo de `restSeconds` entra en `dosisCambiada`, no es «subida» y se acepta si la intervención permite dosis
  (ACORTAR sin retirada, VARIANTE, SEGURIDAD, REVISAR, INVESTIGAR sin P-A2). En GAIN_MUSCLE lo para V24/4D; fuera de GM la V24
  por capacidades está apagada en la adaptación por defecto (`StrictValidationAdapter.java:56-71`,
  `capability-validation.enabled=false`), y V21 solo comprueba el máximo. No hay test (`DosisIaV8CTest` no tiene un caso de
  solo descanso).

---

## 8. Verificación 7: nota sobre el oráculo (código de prueba, no se modifica)

`docs/FitSense/short_paper_ieee/k20_adaptacion_real/herramientas/oraculo_k20.py:170-215` (`comparar_sesion`):
```python
if len(antes) == len(despues):
    pares = list(zip(antes, despues))      # l.177-178: emparejamiento POR POSICION
```
Cada par con id distinto cuenta como «sustitución» (l.187-194) y la dosis de ese par no se compara. El oráculo integral lo
reutiliza (`docs/FitSense/evaluacion_integral_O/herramientas/oraculo_integral_O_v1_4.py:576`) y marca
`"sustituto de otro patron o grupo"` cuando el patrón o grupo del par difiere (l.597-598).

**O04 (VARIANTE_MAS_SENCILLA_O_CARGA):** antes `[119, 520, 1054, 1309, 555]`, después `[119, 128, 315, 1054, 1309]` (5 y 5).
Patrones según el catálogo exportado (`docs/FitSense/auditoria_post_rrp/evidencia/k20/Dp_K/tablas/exercises.csv`):
119 HORIZONTAL_PULL; 520, 555, 128, 315 ELBOW_FLEXION (dif. 2, 2, 1, 1); 1054 LOCOMOTION_CARDIO; 1309 MOBILITY.

| Par por posición | Lectura del oráculo | Realidad |
|---|---|---|
| 520→128 | sustitución ELBOW_FLEXION→ELBOW_FLEXION | sustitución real (dif. 2→1) |
| 1054→315 | «otro patrón» (CARDIO→ELBOW_FLEXION) | 1054 se queda (reordenado, con menos series) |
| 1309→1054 | «otro patrón» (MOBILITY→CARDIO) | 1309 se queda (reordenado, con menos series) |
| 555→1309 | «otro patrón» (ELBOW_FLEXION→MOBILITY) | sustitución real 555→315 (dif. 2→1) |

V8C compara por identidad (`ConservationValidator.comparar`, l.1574-1610: conjuntos de ids; «el orden … no cuenta», Javadoc
l.55-58): añadidos {128, 315}, retirados {520, 555}, todos ELBOW_FLEXION → mismo patrón, multiconjunto 2≤2, dificultad
menor, y las bajadas de series de 1054/1309 están permitidas en VARIANTE. Los tres «sustituto de otro patrón» y la pérdida de
las dos reducciones de dosis son un **artefacto del instrumento** (emparejar por posición ante un reordenamiento). El total de
trabajo (`trabajo_antes/despues`) sí es correcto porque se suma por sesión. No se modifica el oráculo.
(El caso D5 de `pruebas_oraculo_v1_3.py:101-110` es una mutación sintética en la misma posición y sí es un verdadero positivo.)

---

## 9. Matriz final

| Componente | Política vigente | Implementación (archivo:método) | Cómo se verifica (tests en src/test) | Resultado |
|---|---|---|---|---|
| Registro y motivo de sesión | dolor en un ejercicio ⇒ la sesión se trata por seguridad | `analytics/domain/services/SessionOutcomeIdentifier.identify` (l.77-94) | `AdaptationOrderDeciderTest.dolorExigeRevisionDeSeguridad`, `AdaptacionCompletaIT` | Correcto |
| Evidencia / cobertura | observada≠null y cobertura ≥50 % | `S/.../EvidenceRule.enoughEvidence`; `A/.../UserInterventionCommandServiceImpl` (l.55) | `EvidenceRuleTest`, `AdaptationOrderDeciderTest.coberturaInsuficiente`, `sinRegistrosNoHayEvidencia` | Correcto |
| Decisión: dolor primero | dolor resuelto aunque la adherencia sea buena; el resto se conserva | `A/.../AdaptationOrderDecider.decide` (l.68-83, l.137-138) | `AdaptationOrderDeciderTest.dolorConAdherenciaBuena`, `dolorAntesQueLaPeor` | Correcto |
| INSUFFICIENT_DATA / NO_CHANGE | conservar; sin progresión automática | `AdaptationOrderDecider.decide` (l.89-103, l.120-126) | `AdaptationOrderDeciderTest.adherenciaAdecuadaConserva`, `bajaSinSenaladasNoAdapta` | Correcto |
| Localizada vs ampliada | <moderado: todas; entre moderado y bueno: la peor | `AdaptationOrderDecider.decide` (l.131-141) | `AdaptationOrderDeciderTest.localizadaTocaUnaSola`, `ampliadaTocaTodas` | Correcto |
| Permisos por causa | tabla motivo→intervención | `S/.../TipoIntervencion` (l.33-226); `P/.../PermisosIntervencion` | `TipoIntervencionTextoCoherenteTest`, `AdmiteComoSustitutoTest`, `AdaptationOrderDeciderTest.horarioNoPermiteSustituir` | Correcto |
| Conservación de no señaladas | idénticas; ninguna desaparece | `ConservationValidator.revisarNoSenalada`, `revisarParticion` | `ConservationValidatorTest.noSenalada*`, `sesionDesaparecidaSinDeclarar` | Correcto |
| IA no sube dosis ni carga | solo el backend progresa | `ConservationValidator.revisarDosisDeLaIa`, `revisarCarga` | `DosisIaV8CTest`, `CargaV8CTest`, `ProgresionV24V8CTest.iaNoSubeDosis` | Correcto |
| Descanso inmutable | «el descanso no se cambia nunca» (prompt) | `revisarDosisDeLaIa` (l.704-719), `revisarSenalada` (l.984) | ninguno | **Hallazgo H2** |
| ACORTAR: 1 relleno | exactamente una colocación sin `MandatorySlot`, resto intacto | `ConservationValidator.revisarSustitucion` (l.1156-1245) | `ConservationValidatorQuitarRellenoTest` (10 casos) | Correcto |
| ACORTAR: tope M | duración ≤ M declarada | `ConservationValidator.revisar` (l.542-552); `FaltaDeTiempoPolicy.afinar` | `FaltaDeTiempoPolicyTest`, `AdaptacionCompletaIT` | Correcto (V8C) |
| Fatiga (P-A1) | GM: solo mantenimiento 3x10→2x10; avisos 2/3 semanas | `revisarDosisDeLaIa` (l.716-719); `ProgresionDosisPolicy.admite4D`/`mantenimiento4D` | `RedisenoAdaptacionTest.mantenimientoAceptado`, `reduccionesArbitrarias`, `fatigaPersistente`, `RedisenoFatigaGmIT` | Correcto (H7 fuera de GM) |
| Dificultad (VARIANTE) | variante del mismo patrón o carga; impacto no sube | `revisarSustitucion` (l.1279-1331), `revisarMulticonjunto` | `ConservationValidatorTest.dificultadMismoPatron`, `dificultadOtroPatron`, `V8CConservacionPatronH1Test`, `RedisenoAdaptacionTest.varianteNoSubeImpacto` | **Hallazgo H1** (retirada pura aceptada) |
| Desmotivación (P-A2) | una sustitución lateral estricta, sin dosis | `ConservationValidator.revisarSustitucionPorDesmotivacion` | `RedisenoAdaptacionTest.sustitucionEquivalente`, `dosSustituciones`, `sustitucionMasDosis` | Correcto (H7: varias bajadas de dosis) |
| Dolor identificado / de sesión | lo que dolió no vuelve; dolor de sesión: ninguno de antes; no más trabajo | `ConservationValidator.revisarDolor` (l.1092-1141), rama dolor (l.1262-1277); `PainMemoryService.registrarPlan`/`registrarOmision` | `ConservationValidatorTest.dolor*`, `DolorSinMasTrabajoV8CTest`, `PoliticasP123IT`, `RespaldoConDolorIT`, `SinSustitutoDolor24CasosIT` | Correcto técnicamente; pendiente de revisión profesional |
| Restricciones personales | conjunto elegible + sustitución con causa verificada | `WeeklyTrainingPlanCommandServiceImpl.prepare` (l.164-176); `ConservationValidator.neutralizarSustituciones`; `SafetySubstitutionPolicy` | `SinSustitutoDolorLimite*IT`, `PoliticasP123IT` | Correcto |
| SIN_EFECTO | señalada sin cambios = sin efecto; semana sin aplicadas = CONSERVED | `ConservationValidator.revisarSenalada` (l.1016); `AdaptedWeekProducer.produce` (l.316-319) | `ConservationValidatorTest.senaladaSinCambios`, `AdaptedWeekProducerTest`, `AdaptationOutcomeTest` | Correcto |
| RESPALDO | sin IA: copia; con dolor: P-03 o NO_FACTIBLE | `AdaptedWeekProducer.respaldo` (l.334-385); `AdaptedWeekCommandService.corregir` (l.469) | `RespaldoConDolorIT`, `AdaptedWeekProducerTest`, `CopiaSinIaRev2IT` | Correcto |
| Prompt FIX-PR-1.3 | coherente con V8C | `SessionFixPromptBuilder.build` / `contratoDeLaSesion` | `SessionFixPromptBuilderTest`, `PromptRev2Test` | **Hallazgo H4** |
| Factibilidad V2/V2.1 | describe lo que V8C acepta | `FactibilidadAdaptacion.calcular`/`texto` | `SessionFixPromptV2Test`, `SessionFixPromptV21Test`, `MejoraIaPromptsTest.v21SinOpcionDeRetirar` | **Hallazgo H5** (corregido en V2.2) |
| Factibilidad V2.2 | ofrece retirar 1 relleno en ACORTAR | `FactibilidadAdaptacion.calcular(..., true)` (l.204-219) | `MejoraIaPromptsTest.v22OfreceRetirarElRelleno`, `v22SoloEnAcortar` | Correcto, con **Hallazgo H3** (M) |
| Progresión: 2 semanas al 100 % | cumplimiento real 100 % en dos semanas | `ProgresionResolver.dosSemanasPerfectas` | `ProgresionCienPorCienIT`, `ProgresionIT` | Correcto |
| Techo 1,10× y paso ≤5 % | acumulado sobre V_base; paso por sesión | `ProgresionResolver.deLaSesion` (l.219-220), `progresiones` (l.611-615) | `ProgresionReservaIT`, `ProgresionFactibilidadIT` | Correcto |
| Un paso por ciclo | a lo sumo una modificación funcional | `AdaptedWeekCommandService.continuidad`; `ConservationValidator.pasoEstrecho` | `ProgresionV24V8CTest.unaModificacionPorCiclo`, `pasoDeDosEjercicios`, `derivadaDistintaDelPaso` | Correcto |
| Recuperación | gradual (≤10 %), hasta la referencia, 80 % | `ProgresionResolver.recuperacion`/`haciaReferencia`; `soloRecuperacion` (REV2) | `RedisenoFatigaGmIT`, `ProgresionRev2IT`, `PendientesProgresionRev2IT` | Correcto |
| Reincorporación | solo tras retirada ACORTAR, tras ≥1 progresión, dosis inicial exacta, dentro de techo y M | `ProgresionResolver.retirado`, `progresionesTrasAcortar`, `exacta`, `mVigente` | `ReincorporacionTrasProgresionTest`, `Reincorporacion4DTest`, `ReincorporacionIaRealIT` | Correcto |
| V24 en generación inicial | no se relaja nunca | `PlanDraftValidator.validate` 3 args → `vacio()` (l.128-129); `validateMvpDose` (l.599-633) | `ProgresionV24V8CTest.generacionRechazaDosisNoExacta`, `generacionSinPermisos`, `RedisenoAdaptacionTest.generacionIntacta` | Correcto |
| V26 y semana previa | no obligar a recortar lo heredado en semanas derivadas | `PlanDraftValidator.validateWeeklyVolume` (l.1584-1626) | `PoliticaRedisenoTest.v26`, `v26SemanaDerivada` | **Hallazgo menor H6** (también en P-03) |
| Oráculo K20 | instrumento de medida | `oraculo_k20.py:comparar_sesion` (l.177-178); `oraculo_integral_O_v1_4.py` (l.597-598) | `test_oraculo_k20.py`, `pruebas_oraculo_v1_3.py` | **Hallazgo de instrumento H8** (no se modifica) |

---

## 10. Nota de entorno (WIP concurrente)

Durante la auditoría aparecieron cambios **sin commit** de otra sesión en `SessionFixPromptBuilder.java` y `PlanPromptBuilder.java`
(`git status`: ` M`), con variantes `v2.2a`/`v2.4` y un `resumenDelCaso`. No se auditaron. Tal como estaban, referencian
`PlanAdjustment.SesionSenalada` y `PreviousWeekSummary.SessionOutcome`, tipos que no existen en HEAD (los existentes son
`PlanAdjustment.TargetedSession` y `PreviousWeekSummary.WorkoutOutcome`), así que ese WIP probablemente no compila. Este informe
se refiere al commit `42d9dc1`.
