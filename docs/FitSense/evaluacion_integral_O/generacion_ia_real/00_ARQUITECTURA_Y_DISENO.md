# GRA · 00 · Revisión de la arquitectura de la generación inicial (antes de modificar)

**Orden:** «FitSense: generación inicial y adaptación completa con Replicate real» (06/11/2026), §2.

## 1. Qué existe hoy

| Pregunta de la orden | Respuesta (código de producción, versión A `f1c6c02`) |
|---|---|
| ¿Existe un flujo de generación inicial con Replicate? | **Sí, completo y en producción**, aunque **apagado por defecto** (`fitsense.ai.enabled=${FITSENSE_AI_ENABLED:false}`). Es `PlanGenerationPipeline.run` con `ReplicateTrainingPlanGenerator` (modelo `openai/gpt-5-structured`, `gpt-5-mini`, prompt **PR-1.7**). No hay que implementarlo: hay que **habilitarlo y verificarlo** |
| ¿Por qué no participó en R_O y R′_O? | Lo **apagaba el arnés**: `EvaluacionK20IT` forzaba `isEnabled=false` en la generación del modo integral (comentario: «mismo plan inicial que D»). La IA solo intervenía en la adaptación |
| ¿Cómo se seleccionan los ejercicios hoy (determinista)? | `RuleBasedTrainingPlanGenerator`, con `CapabilityWeekBuilder` para INCREASE_STRENGTH, LOSE_WEIGHT y GENERAL_FITNESS (capacidades 4A/4D) y las clases de dosis 4D para GAIN_MUSCLE. Usa una semilla fija por usuario y semana, `userId × 1000003 + epochDay`. Parte del conjunto **elegible** ya filtrado: equipamiento, restricciones, edad, nivel, bloqueados y exclusiones por dolor |
| ¿Qué decide el generador determinista? | Reparto de sesiones (S-MAX: min(días, tope del nivel, disponibles)), enfoque de cada sesión, slots obligatorios (empuje, tracción, rodilla, cadera), ejercicios por slot y clase, dosis por clase (series, repeticiones o segundos, descanso), orden y duración |
| ¿Qué decide la IA en el flujo existente? | **Lo mismo:** organización de las sesiones, ejercicios del catálogo permitido (solo `available_exercises`, filtrado por el backend), distribución, series, repeticiones o segundos y descansos. Devuelve un JSON con esquema |
| ¿Qué conserva el backend? | Factibilidad previa (`CoberturaFuerzaTiempo` → `FUERZA_SIN_TIEMPO` **antes** de cualquier llamada), conjunto elegible, normalización del tipo de prescripción (dato del catálogo), **validación completa con `PlanDraftValidator` (V1–V27, incluidas V24 y V25)**, la misma que el motor de reglas, publicación, persistencia y trazabilidad (`generation_source`, `generation_attempts`, `model_name`, `input_snapshot`, `output_snapshot`) |
| Política de fallo (19.4) | Hasta **3 propuestas** de la IA. Cada reintento recibe su propuesta rechazada y los motivos. Si las 3 fallan, o el proveedor está caído (`AiProviderUnavailableException`), se aplica el **respaldo por reglas**, identificado como `generation_source = RULE_ENGINE` |
| V20 / V24 / V25 / V8C | **V20:** piso de minutos; PLAN-REV-2.0 no tiene piso (`pisoV20=false`). **V24:** dosis exacta de una capacidad o clase por colocación. **V25:** capacidades principales factibles presentes. Las tres se aplican **igual** a la IA y a las reglas. **V8C** es de la adaptación (`ConservationValidator`), no de la generación |
| Datos que recibe la IA | `PlanInputSnapshot` (GEN-IN-1.x) dentro del prompt: perfil (objetivo, nivel, edad, sexo, altura, peso), restricciones, días disponibles, minutos, equipamiento, `available_exercises` (filtrados, con patrón, grupo, dificultad, tipo de prescripción y dosis admitidas), `dosis_por_clase` o `capacidades`, `suggested_split`, `max_colocaciones_dia`, `min_exercises_per_session`, `max_rest_seconds` y la regla prioritaria de capacidades (PR-1.5 a PR-1.7) |
| Reutilizable desde la adaptación | Cliente Replicate, control de tiempos (`REPLICATE_TIMING`), medición de tokens y coste, cortacircuitos, normalizador y validador |

## 2. Diferencias entre generación y adaptación

| | Generación inicial | Adaptación |
|---|---|---|
| Qué produce la IA | La semana completa desde cero | Cambios sobre sesiones señaladas de una semana existente |
| Validación | `PlanDraftValidator` (V1–V27) | `PlanDraftValidator` + **V8C** (conservación: sesiones no señaladas idénticas, sin más trabajo, patrones…) |
| Intentos | 3 + respaldo por reglas | 3 + respaldo determinista o conservación |
| Prompt | PR-1.7 (`PlanPromptBuilder`) | V2.1 `FIX-PR-2.1-EXP` (`SessionFixPromptBuilder`) |
| Factibilidad previa | `FUERZA_SIN_TIEMPO` (O11) antes de llamar | Sonda de factibilidad del arnés (solo mide) |

## 3. Decisión

**No se modifica el código de producción.**
- La capacidad pedida, que en la orden se llama `INITIAL_GENERATION_REPLICATE`, ya existe. Su control explícito es `fitsense.ai.enabled`, apagado por defecto.
- El generador determinista **no se reemplaza**: sigue siendo el respaldo y la vía cuando la IA está apagada.
- No se toca V20, V24, V25, V8C, HIST, el prompt ni la política.

**Cambios solo en el arnés de pruebas** (`EvaluacionK20IT`, commit `c8ff1bf`):

| Cambio | Detalle |
|---|---|
| `-Dfitsense.evaluacion.k20.generacion-ia=SI` | Exige el modo integral, REPLICATE y su autorización. Deja de forzar el motor de reglas en la generación inicial, que pasa por el pipeline real |
| Intercepción de `requestOutput(context, feedback, attempt)` | **Hallazgo:** el pipeline llama a la variante de 3 argumentos y el arnés solo interceptaba la de 2. Sin este cambio, las llamadas de generación habrían escapado al control del presupuesto y a la traza |
| Prompts de generación | Se guardan tal cual los arma el backend |
| `generacion_ia.csv` | Una fila por llamada: resultado del proveedor, predicción, si el backend la rechazó y con qué motivos (WARN del pipeline), `generation_source`, `generation_attempts` y modelo del plan publicado |
| Con el interruptor apagado | Comportamiento idéntico (comprobado con huellas) |

**Oráculo v1.4** = v1.3 + R-2, solo en corridas `generacion_ia: true`:
- Un plan con `generation_source = AI` exige una propuesta de generación válida.
- El respaldo se informa como tal y **nunca** cuenta como generación de la IA.
- A1 sigue juzgando la validez técnica del plan publicado con reglas independientes, sean de la IA o del respaldo.
