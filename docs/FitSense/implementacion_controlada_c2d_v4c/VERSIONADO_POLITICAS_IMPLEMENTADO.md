# Versionado de políticas implementado

## 1. Catálogo (`PlanningPolicy`, lista cerrada)

| Id | Candidata | C2-D | V4-C | Descripción |
|---|---|---|---|---|
| `PLAN-HIST-1` | No | No | No | V20 fijo al 70 %; V4 histórico (minutos declarados frente a `round(M × 1,15)`); V24 y 4D vigentes |
| `PLAN-C2D-0.1` | Sí | **Sí** | No | Histórica + excepción de V20 por saturación de contenido |
| `PLAN-V4-0.1` | Sí | No | **Sí** | Histórica + V4 en segundos estimados |
| `PLAN-C2D-V4-0.1` | Sí | Sí | Sí | **Combinación explícita** de las dos. Elegir una no activa la otra |

**Reglas del catálogo**
- `PLAN-C2-0.1` **no se reutiliza**: en la verificación anterior designaba la variante C2-C, y reutilizarlo sería ambiguo. Es una versión desconocida y produce error.
- El modelo E no tiene versión: no se puede activar.
- **Versión desconocida → `PoliticaDesconocidaException`.** Nunca hay vuelta silenciosa a la histórica, ni al leer un plan ni al configurar el contexto.

## 2. Selección (`PlanningPolicySelector`)

- **Planes nuevos:** la versión de `fitsense.planning.policy` (por defecto `PLAN-HIST-1`). Una candidata exige además `fitsense.planning.policy.allow-candidates=true`; sin ese permiso, **el contexto no arranca**.
- **Nueva versión de la misma semana** (regeneración, reparación de seguridad): **hereda la versión del plan que reemplaza** (`WeeklyTrainingPlanCommandServiceImpl.prepare` con `previousActive`, y `WeeklyTrainingPlan.nextVersionOf`). Lo ya publicado no se reinterpreta.
- **Semana nueva del ciclo** (adaptación o P-03 de la semana siguiente): la versión activa.
- **Una sola versión por operación.** La política viaja en `PlanGenerationContext.policy()`. El generador, el validador, el contexto de la IA, la adaptación, P-03 y la progresión leen esa misma instancia; no hay un segundo lugar donde decidirla.

## 3. Persistencia (V44)

| Columna | Contenido |
|---|---|
| `planning_policy` | Id de la versión, con `CHECK` sobre la lista cerrada. `NULL` = histórica |
| `planning_policy_trace` | JSON con política, `version_calculo` (`calculation_configs.version`), `parametros`, `minutos_disponibles`, `origen`, `contexto` y una entrada por sesión |

**Contenido de `parametros`**
- `c2d` y `v4` (`HISTORICO` o `SEGUNDOS`);
- `piso_v20_pct` y `piso_v20_segundos`;
- `tolerancia_v4_pct`;
- `limite_v4`: minutos declarados (histórica) o segundos.

**Valores de `contexto`:** `GENERACION`, `ADAPTACION` o `REGENERACION_P03`.

**Entrada por sesión**
- `fecha`, `segundos_estimados`, `minutos_declarados`, `pct_de_m`;
- `bajo_piso_v20`, `exenta_v8c`;
- **`excepcion_c2d`**: la sesión está bajo el piso, no está exenta por V8C y la versión tiene C2-D. Es la marca de una aceptación por excepción.

Además, `excepciones_c2d` totaliza las excepciones del plan.

**Dónde se registra**

| Operación | Registro |
|---|---|
| Generación inicial | `persist` con contexto `GENERACION` |
| P-03 | `persist(..., "REGENERACION_P03")` |
| Semanas adaptadas | `publishAdapted`, con las fechas exentas por V8C de su `ConservationReport` |
| Reparación de seguridad | Hereda versión y traza del plan reparado |

## 4. Compatibilidad histórica

| Registro | Qué pasa |
|---|---|
| Planes anteriores a V44 | `planning_policy = NULL` → `PLAN-HIST-1`. No se tocan |
| Semanas publicadas, dosis autorizadas, reducciones por dolor, sesiones acortadas, progresiones, reincorporaciones, linaje (V31) | Sin cambios de datos ni de significado. V44 no reescribe filas |
| Revalidación de una semana existente | Con su propia versión (`WeeklyTrainingPlan.politica()`) |
| V8C y linaje | Sin cambios. C2-D **no actúa** cuando hay autorizaciones o reducciones de V8C (condición estricta del validador) |

## 5. Pruebas

| Prueba | Qué comprueba |
|---|---|
| `PoliticaPlanificacionTest.catalogo` | Lista cerrada; `null` es la histórica; desconocida, error; `PLAN-C2-0.1` y E rechazados; componentes independientes |
| `PoliticaPlanificacionTest.seleccion` | Histórica por defecto; una candidata sin permiso no arranca |
| `PoliticaPlanificacionTest.contexto` | El constructor de siempre es histórico; `conPolitica` solo cambia la política |
| `PoliticaPlanificacionTest.entidad` | Sin política es histórica; una desconocida no se registra; la versión siguiente la hereda |
| `PoliticaPlanificacionTest.traza` | Segundos por sesión; V8C frente a C2-D; contexto |
| IT `CicloReal*IT` | Cada plan publicado en el ciclo real lleva su versión y su traza (columnas `politica_plan` y `contexto_traza` de los CSV longitudinales) |
| Regresión completa | Con la política histórica, todas las pruebas de `generation` pasan sin cambios (`REGRESION_COMPLETA.md`) |
