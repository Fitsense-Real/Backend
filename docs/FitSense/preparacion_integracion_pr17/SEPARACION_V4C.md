# Separación del versionado y V4-C en una rama aislada

**Rama:** `feature/v4c-versionado-aislado`, creada desde `generation` (afef98a). Commit 6aec7e0.

**Objetivo:** corregir el defecto de V4 sin arrastrar C2-D, EF ni las opciones experimentales del PR #17.

**Método:** port selectivo, archivo a archivo, desde el PR #17 (254a18e), sin `cherry-pick` indiscriminado. Cada archivo se copió del PR #17 y después se le quitó lo que no pertenece a V4-C, o se aplicó a mano solo el fragmento de V4-C.

## 1. Defecto que corrige (revisado en esta orden)

**V4 histórico:**
- compara **minutos** con `round(M × 1,15)`: 35, 52, 69 y 86 para 30, 45, 60 y 75 min;
- esos minutos se **redondean al minuto**;
- **V4-C** compara los **segundos estimados por el contenido** (la fórmula de V17/V20) con `M × 60 × 115 / 100`, con aritmética entera: `segundos × 100 > M × 60 × 115`.

**Qué minutos llegan a V4 en el pipeline real.** Esta orden lo revisó en `generation`:
- **Generación con IA:** `PlanDraftNormalizer` (B2) sustituye los minutos declarados por los **calculados** (`estimateMinutes`) **antes** de validar.
- **Corrección de sesiones:** `AdaptedWeekProducer` hace lo mismo cuando cambia el contenido.
- **Motor de reglas:** usa los minutos estimados.

**Defecto real: la brecha de redondeo.** La histórica admite sesiones de hasta `(round(M × 1,15) + 0,5) × 60 − 1` segundos estimados (`BrechaRedondeoV4Test`):

| M | Límite V4-C | Máximo que admite la histórica | % de M |
|---|---|---|---|
| 30 | 2.070 s | 2.129 s | 118,3 % |
| 45 | 3.105 s | 3.149 s | 116,6 % |
| 60 | 4.140 s | 4.169 s | 115,8 % |
| 75 | 5.175 s | 5.189 s | 115,3 % |

**El caso del 142 %** (2.550 s declarando 35 min en un perfil de 30):

| Dónde | Histórica | V4-C |
|---|---|---|
| Validador llamado directamente con el borrador sin normalizar (`V4CValidadorRealTest.caso142`) | Lo admite | Lo rechaza |
| Pipeline real, IA de generación simulada (`GeneracionIAHistoricaIT` / `GeneracionIAV4CIT`) | **También lo rechaza**, con su V4 de siempre: «dura 45 minutos y el techo es 35», porque el normalizador ya reescribió los minutos | Lo rechaza en segundos: «dura unos N s… techo 2070 s» |

**Corrección respecto a documentos anteriores:**
- `implementacion_controlada_c2d_v4c/IMPLEMENTACION_V4C.md` y `CIERRE_IMPLEMENTACION_CONTROLADA.md` decían que «la histórica lo sigue admitiendo». Eso **solo es cierto a nivel de validador**.
- En el pipeline real, la histórica ya lo rechazaba. Lo que V4-C añade es:
  1. cerrar la brecha de redondeo;
  2. no depender de que todo camino futuro normalice los minutos (defensa en profundidad);
  3. enviar el límite en segundos a la IA (`max_session_seconds`).
- Los documentos anteriores **se conservan sin cambios**.

**Matriz de 960 perfiles:** ningún plan del motor de reglas cae en la brecha (PLAN-HIST-1 = PLAN-V4-0.1 en 960/960). Hoy la brecha solo afecta a propuestas de la IA que caigan entre el 115 % y el redondeo.

## 2. Qué se ha portado

| Área | Archivo | Qué contiene |
|---|---|---|
| Versión de política | `PlanningPolicy` (nuevo, **mínimo**) | Catálogo cerrado con **solo** `PLAN-HIST-1` (por defecto) y `PLAN-V4-0.1` (candidata). Incluye `deRegistro` (NULL o vacío → histórica; desconocida → error), `limiteV4Segundos` y `excedeTecho` |
| Selección explícita | `PlanningPolicySelector` (nuevo) | `fitsense.planning.policy` (por defecto PLAN-HIST-1) y `fitsense.planning.policy.allow-candidates` (por defecto false). Una candidata sin permiso, o una versión desconocida, **impide arrancar** |
| Contexto | `PlanGenerationContext` | Componente 11 `policy`. El constructor de 10 argumentos de siempre equivale a la histórica; `conPolitica` cambia solo la política |
| Persistencia | `WeeklyTrainingPlan` | Columnas `planning_policy` y `planning_policy_trace`, `registrarPolitica` (rechaza versiones desconocidas), `politica()` (NULL → histórica). Las versiones siguientes del mismo plan heredan la política (una reparación no la reinterpreta) |
| Traza | `PlanningPolicyTrace` (nuevo) | Política, versión de cálculo, parámetros (V4 SEGUNDOS/HISTÓRICO, piso V20, límite V4), minutos disponibles, origen, contexto y, por sesión, segundos estimados, minutos declarados, % de M, bajo piso V20 y exenta V8C. **Sin** campos de C2-D |
| Servicios | `WeeklyTrainingPlanCommandServiceImpl`, `WeeklyTrainingPlanCommandService` (`persist` de 3 argumentos), `SafeRegenerationService` (contexto `REGENERACION_P03`), `PublishedPlanSafetyRepairService`, `ExternalConfigurationService` (`versionCalculo`) | Registran la política y la traza en generación, adaptación, P-03 y reparación. **Sin** el bloque de opciones LW-PREF-0.2 / REP-SUST-0.1 |
| Migración | `V44__planning_policy_version.sql` | **Idéntica** a la del PR #17 (SHA-256 `640b0169…e2708400`) |
| Validación V4-C | `PlanDraftValidator` | Solo el fragmento V4-C de `validateSessions`: con `v4Segundos()`, segundos estimados frente al límite y mensaje «V4: … dura unos N s según su contenido y el techo es L s (M minutos + 15 %), declare lo que declare» |
| Límite en los generadores | `CapabilityWeekBuilder` | Las 9 comparaciones `minutos > techo` pasan por `excede(...)`, que con la histórica hace **exactamente** la comparación de siempre. El método está en la **misma posición** que en el PR #17 (§ DEPENDENCIAS, apartado 3) |
| | `RuleBasedTrainingPlanGenerator` | El recorte final usa `excedeTecho(context, workout, techo)`: histórica = minutos; V4-C = segundos |
| Contrato de la IA | `PlanInputSnapshot`, `SessionFixInputSnapshot` | `constraints.max_session_seconds`, **solo** con V4-C. Con la histórica el JSON es idéntico |
| | `PlanPromptBuilder`, `SessionFixPromptBuilder` | Una frase con el techo en segundos, solo con V4-C. **Sin** la regla de elegibilidad funcional (EF) |
| Pruebas | ver `PRUEBAS_V4C_AISLADO.md` | — |

**Tamaño del cambio frente a `generation`:**
- 26 archivos: 1.271 líneas añadidas y 28 eliminadas.
- De ellos, `src/main` son 18 archivos (17 de código y la migración): 426 líneas añadidas y 25 eliminadas.
- El resto son 8 archivos de pruebas.

## 3. Qué se ha excluido

| Componente del PR #17 | Archivos que **no** están en la rama aislada |
|---|---|
| **C2-D** (publicación por saturación de V20) | `SaturacionContenidoV20`, `aplicaC2D` y `evaluarC2D` del validador, `excepcion_c2d`/`excepciones_c2d` en la traza, versiones `PLAN-C2D-*` |
| **EF-0.1** (elegibilidad funcional) | `ElegibilidadFuncional`, VEF del validador, filtros de `CapabilityWeekBuilder` y `ExerciseSelector`, regla para la IA, versiones `*-EF-*` |
| **V45** | `V45__planning_policy_elegibilidad_funcional.sql`. **Queda solo en el PR #17** |
| **LW-PREF-0.2** | Bloque `preferirGrupoNoCubierto` de `CapabilityWeekBuilder` y su `@Value` |
| **REP-SUST-0.1** | Reparación por sustitución de `RuleBasedTrainingPlanGenerator` y su `@Value` |
| **NOFACT-0.1** | `DiagnosticoNoFactible`, `NoFactibleExceptionHandler`, `PlanGenerationFailedException` y `PlanGenerationPipeline` (cambios de diagnóstico) |
| **API de duración y aeróbica** | `DuracionYAerobicaAssembler`, `DurationInfoResource`, `AerobicActivityResource`, `MetricaAerobica` y los cambios de `TrainingPlanController`, `PlannedWorkoutResource` y `WeeklyTrainingPlanResource` |
| Laboratorios del PR #17 | Matrices C2-D/EF, ciclos por política, `CierrePR17LabIT`, muestra profesional |

**Comprobación:**
- en la rama aislada no aparece ninguna referencia a `C2D`, `c2d()`, `SaturacionContenidoV20`, `aplicaC2D`, `ElegibilidadFuncional` ni `_EF`;
- `PlanningPolicy.deRegistro("PLAN-C2D-0.1")` lanza `PoliticaDesconocidaException` (prueba `catalogo`).

## 4. Preservación del histórico

| Garantía | Evidencia |
|---|---|
| La política por defecto es PLAN-HIST-1 | `PlanningPolicySelector`; prueba `seleccion` |
| Con PLAN-HIST-1, los 960 perfiles dan los mismos planes que `generation` | 960/960 idénticos en los 13 campos (`COMPARACION_960_PERFILES.md`) |
| Con PLAN-HIST-1, el contrato de la IA no cambia | `ContratoV4CIATest`: JSON sin `max_session_seconds` y prompt idéntico al del constructor de siempre |
| Los planes anteriores a V44 (`planning_policy` NULL) se leen como histórica y **no se reescriben** | `MigracionV44IT`; `PoliticaV4CCicloIT.planesHistoricosIntactos` |
| No hay cambio automático de política | Solo cambia por configuración. Una candidata exige `allow-candidates=true`. Las versiones siguientes heredan la del plan |
| V4 histórico intacto | `V4CValidadorRealTest.historicaIntacta` (60 > 35 rechaza; 35 con 2.071 s pasa); `PoliticaPlanificacionTest.v4Historico` (30–75 min) |

## 5. Riesgo conocido: CHECK de V44 frente a catálogo mínimo

- V44 es **idéntica** a la del PR #17, así que su CHECK admite `PLAN-HIST-1`, `PLAN-C2D-0.1`, `PLAN-V4-0.1` y `PLAN-C2D-V4-0.1`.
- La rama aislada solo **escribe** PLAN-HIST-1 o PLAN-V4-0.1.
- Si una base tuviera una fila con `PLAN-C2D-*`, la rama aislada la leería con `PoliticaDesconocidaException`. **Solo podría ocurrir** si se hubiera ejecutado el PR #17 contra esa base, cosa que no se ha hecho: no se ha aplicado nada en Neon ni en Railway.
- **Se acepta** a cambio de no tener dos V44 distintas.
