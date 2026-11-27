# 03 · Generación, adaptación, validadores, respaldo y seguridad O20

**Rama auditada:** `generation` en `77b9df3`. **Fecha:** 27/11/2026.

**Restricciones respetadas:**
- No se ha hecho ninguna predicción en Replicate.
- No se han tocado los *prompts*, los validadores, las reglas O20 ni el catálogo.
- Las cifras históricas proceden de la documentación de evidencia del repositorio. Se citan con su fichero y **no se han vuelto a medir**.

> Rutas abreviadas: `M/` = `src/main/java/main/web/services/fitsense/`, `R/` = `src/main/resources/`, `D/` = `docs/FitSense/`.
> Tipos de prueba:
> - **U**: unitaria, sin Spring.
> - **IT**: Spring con PostgreSQL de Testcontainers.
> - **SIM**: Replicate simulado (servidor HTTP local, `ReplicateFalso` o `@MockitoBean`).
> - **REAL**: Replicate real; solo se activa con una propiedad y **no** se ha ejecutado.

---

## 1. Flujo de generación inicial

| # | Etapa | Estado | Evidencia |
|---|---|---|---|
| G1 | Datos personales y perfil físico | VERIFICADA | `CreateUserProfileResource`: altura 120–250, peso 30–300, `birthDate @Past`. `UserProfile`: duración por objetivo (P-01), días y notas ≤ 500. Edad mínima de 18 (`ConsentimientoYEdadIT.edadMinima`). El perfil queda congelado tras el primer plan (`UserProfileLockedException`). Pruebas: `PoliticaP01DuracionMinimaTest` (U), `PerfilesLimiteIT` (IT). |
| G2 | Objetivos | VERIFICADA | `GoalType`; `DuracionPorObjetivoPolicy` (30–75 min); `GeneracionPorObjetivoIT` (IT, 20 casos). |
| G3 | Disponibilidad semanal y duración | VERIFICADA | `daysPerWeek` 1–7, `availableDays` ≥ `daysPerWeek`, `sessionMinutes` 15–180 con el rango del objetivo; `RespaldoCincoSeisDiasIT`, `RespaldoSesentaMinutosIT`, `O06LimiteDuracionIT` (IT). |
| G4 | Equipamiento | VERIFICADA | `ExerciseRepository.findEligible` (grupos AND/OR, V23); `LimitacionesPlanIT`, `CatalogoElegibleIT` (IT). `equipmentCodes` no lleva validación en el DTO. |
| G5 | Restricciones (impacto, suelo, rodillas, colgado, por encima de la cabeza) | VERIFICADA | Banderas `allow*` filtradas en SQL (`findEligible`); `SafetyProfile.forAgeLevelAndDeclared`. |
| G6 | Catálogo de ejercicios | VERIFICADA | Tabla `exercises` (V2–V56). Filtro en SQL y después contenciones (`ContencionTextoBandera`, `ContencionConservadora`) y `CatalogEligibilityPolicy` (`ExternalCatalogService:153-240`). `ContencionTextoBanderaIT` (IT). |
| G7 | Contexto de la IA | VERIFICADA | `PlanGenerationContext` (`GEN-IN-1.13`), `PlanInputSnapshot`. La IA solo ve el conjunto elegible ya filtrado, sin las exclusiones por dolor. **Incluye `goal_text` y `health_notes`** en texto libre (P-13). |
| G8 | Solicitud a Replicate | IMPLEMENTADA SIN VALIDACIÓN INTEGRAL en esta auditoría | `ReplicateTrainingPlanGenerator`: modelo `openai/gpt-5-mini` en formato *direct*, `reasoning_effort=medium`, `verbosity=low`, `max_completion_tokens=16000`. Tiempos límite: conexión 10 s, POST 90 s, sondeo 30 s, total 150 s. Recuperación G08 solo por el id de su propia respuesta. Pruebas SIM: `ReplicateRecuperacionTransporteTest` (36), `ReplicateTimeoutsHttpTest`, `ReplicateCancelacionTest`, `ReplicateGpt5MiniDirectoTest`, `Gpt5MiniConO20IT`. Evidencia REAL histórica: §3. |
| G9 | Validación de resultados | VERIFICADA (SIM/U) | `PlanDraftFromJsonAssembler` → `PlanDraftNormalizer` → `PlanDraftValidator.validate` (reglas V1–V27). V1 exige que cada ejercicio pertenezca al conjunto elegible; V6 controla la dificultad. |
| G10 | Reintentos | VERIFICADA (SIM/U) | `PlanGenerationPipeline.AI_ATTEMPTS = 3` (constante), con realimentación del rechazo (`AiRetryFeedback`, `PistasDeReintento`). Un error del proveedor corta el bucle y cuenta para el cortacircuitos (3 fallos → 10 min abierto). Pruebas: `PlanGenerationPipelineCircuitBreakerTest`, `ReintentosYFactibilidadIT`. |
| G11 | Respaldo `RULE_ENGINE` | VERIFICADA | Se usa con la IA apagada o sin token, con el cortacircuitos abierto, cuando el caso es `INVIABLE_PARA_IA`, tras 3 rechazos, ante un error del proveedor o ante un error inesperado (`PlanGenerationPipeline:302-311`). Pasa por **el mismo validador**. Si falla, se prueban variantes deterministas D-G3. Si tampoco sirven, `PlanGenerationFailedException` y no hay plan. Lo ejercitan casi todas las IT con la IA apagada. **Asimetría:** `fitsense.coverage.enforcement=ENFORCE_AI_ONLY`, así que V23 rechaza los borradores de la IA pero no los del motor de reglas. |
| G12 | Persistencia de planes | VERIFICADA | `weekly_training_plans` (V4). Estados `ACTIVE`, `COMPLETED` y `REPLACED`; no existe un estado borrador. Cada cambio crea una versión nueva (`nextVersionOf`). Índices `ux_plan_active` y `uq_plan_user_week_version`. Columnas `generation_source` (`AI`, `RULE_ENGINE`, `CONSERVED`), `generation_attempts`, `model_name` e `input_snapshot`. |
| G13 | Consulta desde Mobile | VERIFICADA | `GET /users/me/plan/current` (semana local del usuario: `ZonaHorariaUsuarioIT`), `/plan/{planId}`, `/plans`, `/plan/limitations`; `CicloCompletoHttpIT` (IT). |
| G14 | Generación asíncrona (V58/V59) | VERIFICADA (SIM) | `POST /users/me/plan/generation` (202/200), `GET /latest` y `GET /{id}`, solo para su dueño. Worker con *lease* de 90 s, latido cada 20 s, `recoverStale` y `requeueOnShutdown`; libro de predicciones. Pruebas: `GeneracionAsincronaResilienciaIT` (24), `ReanudacionConReplicateSimuladoIT` (13), `MigracionV58aV59IT`. `GeneracionAsincronaIT` solo se activa con una propiedad. |
| G15 | Límite de generaciones por usuario | **PARCIAL** | `LimiteDeGeneraciones` (5 por hora, en memoria) solo se aplica en la ruta **síncrona** (`GeneracionInicialService:50-53`). La ruta asíncrona solo limita a una operación activa por usuario. **No existe un tope global de gasto**: las tarifas de `fitsense.ai.cost.*` son informativas. |

### 1.1 Qué *prompts* hay en `generation` y cuáles están activos

| Flujo | Versiones en el código | Activa por omisión | Dónde |
|---|---|---|---|
| Generación inicial | `PR-1.7` (constante, sin propiedad que la cambie). Va con `P-1.3` (principios), `GEN-IN-1.13` (esquema) y `SYS-JSON-1.0` (*prompt* de sistema) | **PR-1.7** | `PlanPromptBuilder.java:93`. En `src/` no existe `PR-1.8`, `PR-1.9` ni `PR-2.2`. H2 (PR-1.9) y H2-v4 (PR-2.2, PR #87 cerrado) solo aparecen en la documentación (`D/cierre_gpt5_mini_h2/`, `D/replicate_transport/05_CIERRE.md`). |
| Adaptación | `FIX-PR-1.3` (v1), `FIX-PR-2.0-EXP` (v2) y `FIX-PR-2.1-EXP` (v2.1) | **v2.1**. Ojo: en el código, la omisión es `v1` (`SessionFixPromptBuilder:93`), pero `application.properties` la fija a `${FITSENSE_AI_ADAPTATION_PROMPT_VERSION:v2.1}` | `SessionFixPromptBuilder.java:57,66,77` |
| Política de planificación | `PLAN-HIST-1`, `PLAN-V4-0.1`, `PLAN-DUR-A-0.1`, `PLAN-DUR-B-0.1` y `PLAN-REV-2.0` | **PLAN-REV-2.0**, que necesita `allow-candidates=true` | `PlanningPolicy.java:46-71`; `application.properties` (`fitsense.planning.policy`). Cada usuario conserva la política con la que empezó. |

**H2-v5 no se ha iniciado**, tal como pide la orden.

---

## 2. Métricas de generación: cuáles se pueden medir y dónde

| Métrica | ¿Se puede consultar en la base? | Fuente |
|---|---|---|
| Tiempo de generación | Solo en la ruta asíncrona (`plan_generation_operations.started_at`/`finished_at`). En el resto, **solo en los logs** (`GENERACION_TIEMPOS`, `REPLICATE_TIMING`) | — |
| Cantidad de predicciones | Asíncrona: `plan_generation_predictions` (estado, sondeos, fechas). Síncrona, ciclo semanal y adaptación: **solo en los logs** (`plan_adaptation_attempts.ai_calls` da el número de llamadas por adaptación) | — |
| Aceptados en el primer intento | Sí: `generation_source='AI' AND generation_attempts=1` | — |
| Aceptados tras reintentos | Sí: `generation_source='AI' AND generation_attempts IN (2,3)` | — |
| Generados por `RULE_ENGINE` | Sí (`generation_source`). **El motivo** (IA apagada, cortacircuitos, proveedor, rechazos, inviable) **no se guarda**; solo aparece en el log `motivo_respaldo` | — |
| No generados | Asíncrona: `FAILED`/`NO_FACTIBLE` con `reason_code`. Síncrona y ciclo: **solo en los logs** | — |
| Errores del proveedor | Asíncrona: `plan_generation_predictions.status` (`FAILED`, `TIMEOUT`, `NO_CONFIRMADA`, `ERROR_ENVIO`). En el resto, solo en los logs | — |
| Planes duplicados | Los impiden `ux_plan_active` y `uq_plan_user_week_version`; se pueden comprobar con SQL | `D/consultas/linaje_v31_v32.sql` |
| Planes publicados incorrectamente | Se pueden comprobar con SQL de solo lectura | `D/consultas/exposicion_planes_publicados.sql` |

El panel de administración **no muestra ninguna de estas métricas de IA**.

### 2.1 Evidencia histórica (no re-medida)

| Campaña | Resultado | Fuente |
|---|---|---|
| gpt-5-mini *direct*, `medium`, 19 perfiles | 11/19 válidos en el primer intento; 18/19 planes de IA y 1 de `RULE_ENGINE`; 33 llamadas; 0 errores del proveedor; mediana de 81,1 s por llamada (máximo 122,9 s); tope de 2,67 USD por 42 llamadas | `D/migracion_gpt5_mini/04_PERFILES_Y_COMPARACION.md:26-43,80` |
| Misma prueba con `low` | 5/19 en el primer intento; 6 de `RULE_ENGINE`; mediana de 29 s | ídem |
| MVP, 8 usuarios × 8 semanas | 7/8 con plan inicial; 14 ciclos de adaptación (7 `APLICADA`, 4 `SIN_EFECTO`, 3 `RESPALDO`); 45 llamadas; media de 59 s, máximo de 172 s; ≈ 0,87 USD | `D/campana_mvp/SIMULACION_MVP_8_USUARIOS_8_SEMANAS.md:21-33` |
| Estrés, 8 × 10 | 7/8 generados (6 por Replicate, 3 de ellos en el primer intento; 1 por `RULE_ENGINE`); 69/80 semanas con plan; 2 adaptaciones reales aplicadas | `D/campana_estres/CIERRE_CAMPANA_ESTRES_MVP.md:20-60` |
| Rendimiento con H2 | Mediana de 81,7 s, máximo de 292,7 s por perfil | `D/cierre_gpt5_mini_h2/03_RENDIMIENTO_Y_MOBILE.md:7-8` |
| Incidencia I-5 | Dos POST cuyo cuerpo no se pudo leer **se cobraron**; la ruta síncrona no tiene libro de predicciones | `D/cierre_gpt5_mini_h2/05_REGRESIONES_PRESUPUESTO_PENDIENTES.md:69` |

**Lectura para el estudio:**
- La latencia típica es de 1,5 a 5 minutos por generación. Esto obliga a que Mobile use la ruta asíncrona.
- En la configuración `medium`, alrededor del 42 % de los planes necesitan reintentos.
- Las tasas proceden de muestras pequeñas (≤ 19 perfiles) y no son extrapolables a la cohorte.

---

## 3. Adaptación semanal

| # | Etapa | Estado | Evidencia |
|---|---|---|---|
| D1 | Sesiones realizadas | VERIFICADA | `POST /workouts/{id}/start`, `PUT /sessions/{id}/exercises/{peId}`, `POST /sessions/{id}/finish`; `EjecucionEntrenamientosIT` (25, IT). |
| D2 | Sesiones parciales | VERIFICADA | `CompletionThresholds`: ≥ 80 % `COMPLETED`, ≥ 30 % `PARTIAL`, por debajo `SKIPPED_LEVEL` (se guarda como `PARTIAL`). Umbrales en V7. `POST /sessions/{id}/abandon`; reporte retroactivo `POST /workouts/{id}/report`. |
| D3 | Sesiones omitidas | VERIFICADA | `POST /workouts/{id}/skip` exige un motivo y registra el dolor (`painMemory.registrarOmision`). La tarea `OverdueWorkoutScheduler` (minuto :30) marca `SKIPPED` lo vencido (V54/V57: omisión impuesta por el sistema). |
| D4 | Cálculo de adherencia | VERIFICADA | `WeeklyMetricsCalculator:54-126`: ponderada, observada y cobertura (V30). `AdherenciaIT` (13), `AdherenciaRiesgoIT`, más pruebas U. Fórmulas en `D/ADHERENCIA.md`. |
| D5 | Identificación de señales | VERIFICADA | `SessionOutcomeIdentifier` (sesión problemática si está registrada y por debajo del 80 %); motivos → `TipoIntervencion.deMotivo`; `SessionOutcomeIdentifierTest`. |
| D6 | Tipo de adaptación | VERIFICADA | `AdaptationOrderDecider:52-161`: el dolor tiene prioridad; con cobertura < 50 %, `INSUFFICIENT_DATA`; con ≥ 80 %, `NO_CHANGE`; entre 50 y 80 %, `ADAPT_LOCALIZED`; por debajo de 50 %, `ADAPT_EXPANDED`. `AdaptationOrderDeciderTest` (19), `AdaptacionPorCausasIT`. La tabla porcentual de V7 está **fuera del flujo** (`SinAjustePorcentualTest`). |
| D7 | Contexto de la IA | VERIFICADA (SIM) | `SessionFixPromptBuilder` v2.1 con la proyección P2-A2; `SessionFixPromptV21Test`. |
| D8 | Generación de la propuesta | VERIFICADA (SIM) / REAL histórico | `AdaptedWeekCommandService.corregir:372-483`. Sin IA cuando no hay sesiones de IA, la IA está apagada, el cortacircuitos está abierto o `FactibilidadPreviaIA` lo descarta. |
| D9 | Validación | VERIFICADA | `AdaptedWeekProducer` (V8C `ConservationValidator` y validación estricta V1–V27); `AdaptedWeekProducerTest` (34), `ConservationValidatorTest` (39). |
| D10 | Reintentos | VERIFICADA (SIM) | `AI_ATTEMPTS=3`. Si se agotan, `RESPALDO` (se mantiene la semana validada). **La adaptación no tiene respaldo de motor de reglas**: se conserva la semana. |
| D11 | Publicación | VERIFICADA | `publishAdapted` publica el plan y la evidencia V32 en una sola transacción; los fallos los registra `AdaptationFailureRecorder` (`REQUIRES_NEW`). |
| D12 | Historial | VERIFICADA | `GET /users/me/plan/adaptations`, `/users/me/interventions`, `/users/me/plans`; `LinajeIntegridadIT`, `ReparacionLinajeIT`. |
| D13 | Notificación al usuario | IMPLEMENTADA SIN VALIDACIÓN INTEGRAL | Push `PLAN_ADAPTED`/`WEEKLY_PLAN_READY` (`DetectorEventosPush`), **apagado por omisión** (`fitsense.push.enabled=false`). Probado con Google simulado. |

### 3.1 Políticas vigentes

| Política | Implementación | Pruebas |
|---|---|---|
| Baja adherencia | `ADAPT_EXPANDED` (< 50 %) o `ADAPT_LOCALIZED` (50–80 %), con la intervención que corresponde al motivo | `AdaptacionPorCausasIT`, `ValidacionMultisemanaIT` |
| Exceso de carga | **No hay una política explícita de "sobrecarga".** La fatiga lleva a `REVISAR_CARGA_Y_RECUPERACION`; "demasiado difícil", a una variante más sencilla (`fitsense.adaptation.difficulty-guard=preferencia`). El RPE ≥ 8 solo se usa en la analítica de riesgo | `RedisenoFatigaGmIT`, `LimiteDificultadDolorTest` |
| Progresión | `ProgresionResolver` (sin IA): solo con `NO_CHANGE` y 100 % durante 2 semanas; paso ordinario ≤ 5 %; techo de 1,10 × V_base | `ProgresionIT` (13), `ProgresionCienPorCienIT`, `CopiaSinIaIT.techo` (17 ciclos) |
| Reincorporación | Solo después de al menos una progresión ordinaria; paso de recuperación ≤ 10 % con 80 %; vuelta atrás si hay fatiga | `CopiaSinIaIT.reincorporacionYMantenimiento`, `ReincorporacionIaRealIT` (REAL, desactivada) |

### 3.2 Duplicados y sobrescritura

- **Una publicación por medición:** `bloquearMedicion` (`FOR UPDATE` sobre `weekly_user_metrics`) junto con `planPublicadoDeLaMedicion`. Además, bloqueo consultivo por usuario (`CicloSemanalBloqueo`), `uq_ui_metric` y `ux_plan_active`. Pruebas: `CicloConcurrenteIT` (9: carreras, dos instancias, conexión del bloqueo perdida durante la IA).
- **Hueco (P1):** no hay control optimista. `PreparedPlanGeneration.previousActivePlanId` se calcula, pero **ni `persist()` ni `publishAdapted()` lo comparan**: en `publishAdapted` se vuelve a leer el plan activo y se versiona encima (`WeeklyTrainingPlanCommandServiceImpl:394-399`). Si otro camino publica una versión mientras la IA tarda varios minutos (por ejemplo, una reparación de seguridad o una generación asíncrona), esa versión se reemplaza sin aviso. Solo `PublishedPlanSafetyRepairService` comprueba el id y la versión (`:318-331`). No hay ninguna `@Version` en el código principal.
- **La ruta asíncrona no toma `CicloSemanalBloqueo`** (`PipelineFirstPlanGenerationRunner`). Puede coincidir con la ruta síncrona o con el ciclo semanal del mismo usuario. Solo se publica un plan, pero **se puede pagar dos veces la IA**.

### 3.3 Qué se ha comprobado con IA real y qué solo con simulación

| Caso | Con IA real (histórico) | Solo simulado o con la IA apagada |
|---|---|---|
| Generación inicial gpt-5-mini | Sí (19 perfiles, campañas MVP y de estrés) | — |
| `REVISAR_CARGA`, `VARIANTE_MAS_SENCILLA`, `INVESTIGAR_CAMBIO_MINIMO`, `SEGURIDAD_ANTE_DOLOR` (×4), `ACORTAR` | Sí, 15 adaptaciones (`D/migracion_gpt5_mini/03_EVIDENCIA_REAL.md` §B) | — |
| `REPROGRAMAR_DIAS`, `CONSERVAR`, `ACORTAR` expandida | Sí (`D/campana_mvp/ADAPTACIONES_IA_REAL.csv`) | — |
| Reincorporación con IA | Sí: la IA adapta, y la progresión y la reincorporación son deterministas (`D/cierre_mvp/REINCORPORACION_CON_IA_REAL.md`) | — |
| Exclusión por familia O20, precauciones por ángulo, identificación posterior V65 | **No** (`D/seguridad_o20/README.md:11` lo indica: "sin llamadas") | `DolorFamiliaEquivalenteIT`, `Gpt5MiniConO20IT` (SIM), `MigracionV65IT` |
| Regeneración segura P-03, progresión y copia sin IA | No aplica (son deterministas) | `PoliticasP123IT`, `ProgresionIT`, `CopiaSinIaIT` |
| Coste duplicado en la ruta asíncrona | No | `D/generacion_asincrona/RIESGO_COSTOS_DUPLICADOS.md` (simulado) |

---

## 4. Seguridad de los entrenamientos (O20)

| # | Control | Estado | Evidencia |
|---|---|---|---|
| S1 | Bloqueo por dolor | VERIFICADA | `PainMemoryService`: el dolor en un ejercicio lo marca `DOLOR_IDENTIFICADO`; una sesión omitida por dolor sin ejercicio concreto marca todos sus ejercicios `DOLOR_SESION_SIN_IDENTIFICAR`. Es idempotente (`ux_use_evento`, V43). Cada `prepare()` resta las exclusiones. Pruebas: `DolorFamiliaEquivalenteIT` (10), `O12DolorProgresion{Apagada,Encendida}IT`. |
| S2 | Restricciones físicas | VERIFICADA | Banderas del perfil y `SafetyProfile` (edad y nivel); filtros SQL. |
| S3 | Equivalencias de ejercicios | VERIFICADA (simulado) | `R/catalog/equivalencias_mismo_gesto.csv` (118 grupos); `FamiliaDeMovimientoEquivalente`; `FamiliaDeMovimientoEquivalenteTest`, `PainMemoryFamiliaTest`. **53 relaciones de sinónimos pendientes no tienen efecto**, y un ejercicio nuevo del catálogo no entra en ningún grupo hasta que se actualiza el CSV (`D/seguridad_o20/README.md:161-170`). |
| S4 | Precauciones por ángulo | VERIFICADA (simulado) | `R/catalog/precauciones_angulo.csv` → `PRECAUCION_OTRO_ANGULO`. |
| S5 | Confirmaciones de seguridad | VERIFICADA | `POST /pain-exclusions/{id}/identification` exige confirmación explícita y bloquea la fila (`PESSIMISTIC_WRITE`). Una exclusión provisional pide confirmación tras 2 semanas con plan. |
| S6 | Reactivaciones rechazadas | VERIFICADA | `reactivar:449-485`: si choca con las restricciones vigentes, `REACTIVACION_RECHAZADA_POR_SEGURIDAD`; `ContratoErroresApiIT.reincorporacionSinConfirmacion`. |
| S7 | Exclusiones persistentes | VERIFICADA | Las derivadas de un dolor confirmado son permanentes; ninguna caduca sola. |
| S8 | Validación de sesiones completas y parciales | VERIFICADA | `PublishedPlanSafetyService` marca los conflictos al leer y bloquea el inicio y el registro (`WorkoutSessionCommandServiceImpl:57-58,191`); `ProteccionPlanesPublicadosIT` (12). |
| S9 | Restricciones durante la adaptación | VERIFICADA | `SafetyExclusionResolver` → `SafetySubstitutionPolicy` (SUST-SEG-1) → V8C y validación estricta, también en las copias `CONSERVED`; `SustitucionSeguridadTest` (22), `RespaldoConDolorIT`, `SinSustitutoDolor*IT`. |
| S10 | Compatibilidad con V65 | VERIFICADA | V65 añade `identified_at`, `identification_evidence` y `ck_use_identificacion`; `MigracionV65IT`. **V65 no es la tabla de precauciones por ángulo**, que es un CSV. |

**¿Puede publicarse una rutina insegura?** No se ha encontrado ningún camino que se salte V1 contra el conjunto elegible ya filtrado: `RULE_ENGINE`, D-G3, P-03, `CONSERVED`, progresión, reparación y ciclo manual pasan todos por la validación. **Riesgos residuales:**

1. Si falla la consulta del catálogo, solo se aplican las exclusiones directas (`DOLOR_FAMILIA_NO_CALCULADA`, `PainMemoryService:285-291`). En esa lectura podrían quedar elegibles las equivalencias.
2. La seguridad depende de interruptores por variable de entorno: `FITSENSE_DIFFICULTY_GUARD`, `FITSENSE_PAIN_LOCALIZED_REMOVAL`, `FITSENSE_DOSE_ENFORCEMENT`, `FITSENSE_CAPABILITY_ENFORCEMENT` y `fitsense.catalogo.contencion-conservadora`. Una mala configuración en Railway los apaga **sin que nada avise**. Hay que fijar sus valores y comprobarlos al arrancar (**P1**, sin cambiar las reglas).
3. Las pruebas activan `fitsense.regeneracion.p03-por-colocacion=true` (`src/test/resources/config/application.properties`), pero en producción vale `false`. Parte de la cobertura de P-03 se ha comprobado con una configuración distinta de la de producción.
4. `health_notes` nunca se interpreta: una lesión escrita ahí **no** genera ninguna restricción.
5. Mobile todavía no tiene la interfaz de exclusiones por dolor (`D/seguridad_o20/README.md` §4).

---

## 5. Cierre semanal automático (resumen; detalle en `05` R-01)

- `WeeklyCycleScheduler.runHourly` (`cron 0 0 * * * *`) solo actúa si en la zona del usuario es **lunes y son las 3 h** (`:54-55`). **No hay recuperación:** si el *tick* de las 03:00 se pierde por un despliegue, un reinicio, la ocupación del hilo o un estado `EN_CURSO`/`SIN_BLOQUEO`, esa semana no se cierra ni se genera hasta el lunes siguiente. El log dice "se reintentara", pero nada reintenta (`:74`).
- Todas las tareas `@Scheduled` comparten **un solo hilo** (no hay `spring.task.scheduling.pool.size`). El ciclo de una zona es secuencial y llama a la IA, con unos 90 s por usuario. Si la cohorte de una zona tarda más de 60 min, las zonas siguientes con la misma diferencia horaria se evalúan a las 4 h y **se saltan**.
- Ninguna prueba cubre la lógica de hora y zona. `SeguridadRolesIT` solo comprueba que `runHourly()` no lanza excepciones.
- La **ampliación V59** (libro de predicciones para la ruta síncrona y el ciclo semanal, rama `feature/v59-recovery-all-ai-flows`, V66, WIP "falta IT") **no está integrada: PENDIENTE**. No se ha tocado.
