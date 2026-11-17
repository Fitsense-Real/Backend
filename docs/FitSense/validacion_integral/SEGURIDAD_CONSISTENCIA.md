# Seguridad y consistencia

Fecha: 16/11/2026. Base revisada: `generation` (`2578719`) con #73. Lo asíncrono (V58 y V59) se revisa sobre #72
(`8658089`). No se ha tocado ninguna base real: todo se ha hecho con Testcontainers. `BaseDeProduccionBloqueada`
impide que una prueba apunte a otra base.

> **Actualización 17/11/2026.** #73, #71 y #72 ya están en `generation` (`b9c51ef`): la generación asíncrona (V58 y V59) es parte de la base. Las referencias a #71/#72 de este documento describen dónde se probó cada cosa antes de integrarla.

**Leyenda:** **OK** = garantizado y probado. **Riesgo** = funciona, con una condición que hay que conocer. **Falta** = no
existe.

## 1. Aislamiento entre usuarios y roles

| Garantía | Mecanismo | Evidencia | Estado |
|---|---|---|---|
| Un usuario no ve ni modifica recursos de otro | Todos los endpoints de usuario son `/users/me/...` o comprueban el propietario. Un recurso ajeno da **404**, sin revelar que existe. | `ContratoErroresApiIT.recursosAjenos` (omitir, informar, terminar); `CicloCompletoHttpIT.cumpleTodo` (plan y omisión ajenos: 404) | OK |
| Panel solo para ADMIN | `/api/v1/admin/**` → `hasRole(ADMIN)` en `WebSecurityConfiguration` y `@PreAuthorize` en los dos controladores | `SeguridadRolesIT` (jerarquía por método); `CicloCompletoHttpIT` (USER → 403; ADMIN ve la adherencia) | OK |
| El rol no se cambia por la API | Solo con el script `docs/FitSense/operaciones/cambiar_rol.sql` | `SeguridadRolesIT` | OK (no hay endpoint, a propósito) |
| JWT | Secreto obligatorio (`JWT_SECRET`, sin valor por defecto), 7 días de validez | `ContratoErroresApiIT.tokensInvalidos` (caducado, otra firma, mal formado o ausente: 401), `caducidadConfigurada` | OK |
| Login sin enumeración de cuentas | Mismo 401 y mismo mensaje para un correo desconocido y para una contraseña incorrecta | `ContratoErroresApiIT.loginFallido` | OK |
| Superficie de desarrollo cerrada por defecto | Swagger apagado (`FITSENSE_API_DOCS_ENABLED=false`); `/cycle/run-for-me` con `denyAll` (`FITSENSE_MANUAL_CYCLE_ENABLED=false`); CORS vacío por defecto | `CorsOriginsTest`, `SeguridadRolesIT` | OK |
| Recuperación de contraseña | Token con hash (`uq_prt_token_hash`), 30 min de validez y 3 solicitudes por hora | `RecuperacionContrasenaIT`, `PasswordResetNotifiersTest` | **Falta el proveedor:** `delivery=none`, no se envía ningún correo en producción |

## 2. Unicidad: sin operaciones ni planes duplicados

| Garantía | Restricción en la base | Evidencia | Estado |
|---|---|---|---|
| Un plan ACTIVE por usuario y semana | `ux_plan_active (user_id, week_start_date) WHERE status='ACTIVE'` | `GeneracionInicialConcurrenteIT`, `CicloConcurrenteIT` | OK |
| Versiones de una semana sin colisión | `uq_plan_user_week_version` | `ReparacionLinajeIT` (la reparación crea una versión y la anterior queda REPLACED) | OK |
| Una sola sesión en curso por usuario | `ux_session_active (user_id) WHERE status='IN_PROGRESS'` | `EjecucionEntrenamientosIT` | OK |
| Un solo intento cuenta para la adherencia | `ux_session_counted (planned_workout_id) WHERE counts_toward_adherence` | `EjecucionEntrenamientosIT`, `AdherenciaIT` | OK |
| Un intento por número | `uq_session_attempt` | `EjecucionEntrenamientosIT` | OK |
| Una métrica por usuario y semana; una intervención por métrica | `uq_wum_user_week`; `uq_ui_metric` | `CicloConcurrenteIT`, `AdherenciaIT` | OK |
| Un evento de dolor por ejercicio y sesión | `ux_use_evento` | `PoliticasP123IT` | OK |
| Una configuración de cálculo activa | `ux_config_active` | `ConfiguracionEvaluadaTest` | OK |
| Una generación asíncrona activa por usuario (#71/#72) | `ux_pgo_una_activa_por_usuario`; libro con `ux_pgp_intento_vigente` | `GeneracionAsincronaIT`, `GeneracionAsincronaResilienciaIT` (20 escenarios) | OK (en `generation`) |
| Límite de generaciones y gasto | `DemasiadasGeneraciones` → 429; presupuesto total | `LimiteDeGeneracionesTest`, `PresupuestoGeneracionTest` | OK |

## 3. Referencias e historial

| Garantía | Mecanismo | Evidencia | Estado |
|---|---|---|---|
| Integridad referencial | FK en planes → sesiones → ejercicios → ejecución; `uq_planned_workout_order`, `uq_pwe_order`, `uq_wse_session_exercise` | Migraciones V4, V5 y siguientes; `EsquemaVigenteSobreCatalogoAnterior` | OK |
| Una semana publicada nunca se edita: se versiona | `PlanStatus`: ACTIVE → COMPLETED / REPLACED. La reparación y P-03 crean una versión nueva. | `ReparacionLinajeIT`, `LinajeIntegridadIT`, `PoliticasP123IT` | OK |
| Historial completo visible | `/users/me/plans`, `/plan/{id}`, `/plan/adaptations`, `skipReason` y exclusiones por dolor conservados | `CicloCompletoHttpIT` (3 planes legibles, ≥ 2 decisiones, causas de omisión conservadas) | OK |
| Coherencia de las adaptaciones | `tg_paa_coherencia` rechaza al COMMIT | `P2D` (2 pruebas, opt-in) | OK |
| Trazabilidad de causas (V57) | Causas por semana medida | `AdaptacionPorCausasIT`, `TrazaHistoricaIT` | OK |

## 4. Estados terminales y recuperabilidad

| Elemento | Estados terminales | Evidencia | Estado |
|---|---|---|---|
| Sesión planificada / intento | `planned_workouts`: COMPLETED, PARTIAL, SKIPPED, REPLACED. Un intento abandonado no cuenta para la adherencia. | `EjecucionEntrenamientosIT`, `AbandonoIT`, `EjecucionReglasTest` | OK |
| Adaptación | APLICADA, SIN_EFECTO, RECHAZADA, RESPALDO, NO_FACTIBLE, SIN_ORDEN | `EstadoAdaptacionSinIaTest`, `AdaptationOutcomeTest` | OK |
| Generación asíncrona | COMPLETED / FAILED / NO_FACTIBLE, con lease, fencing y recuperación por lease vencido | #72: `GeneracionAsincronaResilienciaIT`, `ReanudacionConReplicateSimuladoIT`, `MigracionV58aV59IT` | OK (en `generation`) |
| Ciclo semanal | Idempotente por semana (`uq_wum_user_week`). Reejecutarlo no duplica nada. El planificador corre cada hora y usa la zona horaria de cada usuario (`users.timezone`, por defecto America/Lima). | `CicloConcurrenteIT` | OK |

## 5. Transacciones

- **Publicación de la generación asíncrona.** La propiedad y la publicación van en una sola transacción
  (`PipelineFirstPlanGenerationRunner`, #72).
- **Plan nuevo.** El ciclo semanal publica el plan nuevo y su métrica en la misma unidad. La concurrencia la cubre
  `CicloConcurrenteIT`.
- **Reparación de seguridad.** Si es NO_VALIDA, no se escribe nada: el número de planes no cambia
  (`ReparacionLinajeIT.sinSustitutoConLaMismaDosisNoPublicaNada`).

## 6. Migraciones y PostgreSQL

- **Migraciones.** 60 en `generation` (hasta V57, con las puntuales V44_1, V44_2, etc.); V58 (#71) y V59 (#72), integradas el 17/11/2026.
- **Pruebas de migración.** Todas se aplican desde una base vacía en cada IT. Las de actualización con datos son:
  - `MigracionV43IT`;
  - `MigracionV44IT`, `MigracionV44_1IT` y `MigracionV44_2IT`;
  - `MigracionPropuesta1374IT`;
  - `MigracionV58aV59IT` (#72).
- **PostgreSQL 16.** Es la versión que se prueba. Los índices parciales y `cardinality()` son específicos de
  PostgreSQL. No hay soporte de H2, ni hace falta: H2 no se activa.
- **Ninguna migración nueva** en esta orden.

## 7. Riesgos y defectos encontrados

No se ha encontrado ningún defecto de seguridad o consistencia que bloquee el flujo principal. Hay riesgos a conocer
(detalle en `PENDIENTES_PRIORIZADOS.md`):

| Id | Riesgo | Impacto | Tipo |
|---|---|---|---|
| S-1 | **Interruptor de la IA en memoria.** `AirCircuitBreaker` vive en cada instancia. Con varias réplicas, cada una abre el suyo. | Más llamadas a Replicate de las previstas cuando falla | Riesgo de despliegue (no se prepara Railway todavía) |
| S-2 | La recuperación de contraseña no envía correos (`delivery=none`) | El usuario no puede recuperar la cuenta sin intervención | Falta funcional |
| S-3 | **Cambio de rol solo por SQL.** Exige acceso a la base. | Operativo, no de seguridad | Decisión de diseño |
| S-4 | **38 clases IT calculan la semana con `LocalDate.now()`.** Es el mismo patrón que rompió el CI el 16/11. | CI rojo intermitente según el día | Riesgo de pruebas (no de producto) |
