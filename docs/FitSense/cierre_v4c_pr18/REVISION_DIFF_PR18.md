# Revisión del diff del PR #18 frente a `generation`

**Comparación:** base `generation` (afef98a) frente a la cabeza del PR #18 (a52e35e más la documentación de esta carpeta).

## 1. Alcance

**`src/`:** 32 archivos, 1.877 líneas añadidas y 28 eliminadas.

### Código de producción (17 archivos y la migración)

| Archivo | Tipo | Para qué |
|---|---|---|
| `PlanningPolicy` | Nuevo | Catálogo cerrado: PLAN-HIST-1 (por defecto) y PLAN-V4-0.1 (candidata); `limiteV4Segundos` y `excedeTecho` |
| `PlanningPolicySelector` | Nuevo | Selección explícita; una candidata exige `allow-candidates=true` |
| `PlanningPolicyTrace` | Nuevo | Traza en JSONB, con la corrección de esta orden |
| `PlanGenerationContext` | Modificado | Componente `policy`; el constructor de siempre equivale a la histórica |
| `WeeklyTrainingPlan` | Modificado | Columnas `planning_policy` y `planning_policy_trace`; herencia de la política |
| `WeeklyTrainingPlanCommandService(Impl)`, `SafeRegenerationService`, `PublishedPlanSafetyRepairService`, `ExternalConfigurationService` | Modificados | Registran la política y la traza (generación, adaptación, P-03, reparación) |
| `PlanDraftValidator` | Modificado | Solo la rama V4-C de `validateSessions` |
| `CapabilityWeekBuilder`, `RuleBasedTrainingPlanGenerator` | Modificados | El techo pasa por la política; la histórica conserva la comparación de siempre |
| `PlanInputSnapshot`, `SessionFixInputSnapshot`, `PlanPromptBuilder`, `SessionFixPromptBuilder` | Modificados | `max_session_seconds` y una frase, solo con V4-C |
| `V44__planning_policy_version.sql` | Nueva | Dos columnas NULL y un CHECK |

### Pruebas (14 archivos)

**Modificadas:**
- `EsquemaVigenteSobreCatalogoAnterior` (V44);
- `MigracionV43IT`.

**Nuevas:**
- `MigracionV44IT`;
- `PoliticaPlanificacionTest`, `ContratoV4CIATest`, `V4CValidadorRealTest`, `BrechaRedondeoV4Test`, `TrazaV4CTest`;
- `PoliticaV4CCicloIT`, `GeneracionIAV4CBase`, `GeneracionIAV4CIT`, `GeneracionIAHistoricaIT`, `TrazaHistoricaIT`;
- `ComparacionPerfilesIT` (opt-in).

**Documentación:** `docs/FitSense/cierre_v4c_pr18/` (9 archivos). No toca ningún documento anterior.

## 2. Dependencias del PR #17: ninguna

**Búsqueda en las líneas añadidas del diff:** 0 apariciones de `c2d()`, `SaturacionContenidoV20`, `aplicaC2D`, `ElegibilidadFuncional`, `preferirGrupoNoCubierto`, `reparacionSustitucion`, `DiagnosticoNoFactible`, `LW-PREF`, `REP-SUST` y `NOFACT`.

**Excluido:** C2-D, EF-0.1, V45, LW-PREF-0.2, REP-SUST-0.1, NOFACT-0.1, la API de duración y la métrica aeróbica.

**Comprobaciones:**
- No hay V45 en la rama.
- No se añade ninguna dependencia de Maven: `pom.xml` sin cambios.
- No cambia ninguna configuración (`application*.properties` / `yml`).

## 3. V44 sin modificar

| Rama | SHA-256 de `V44__planning_policy_version.sql` |
|---|---|
| PR #18 | `640b0169d20f24c0ae7a68c2cff99d2c7f24b272910dd5c6ede8acae2e708400` |
| PR #17 | `640b0169d20f24c0ae7a68c2cff99d2c7f24b272910dd5c6ede8acae2e708400` |

**Contenido:** dos columnas NULL y un CHECK con las 4 versiones históricas del catálogo (incluye C2-D, para que la V44 sea la misma que la del PR #17).

**Sin migración nueva:** la corrección de esta orden va dentro del JSONB.

## 4. Ningún indicador candidato activo por defecto

| Propiedad | Valor por defecto | Dónde |
|---|---|---|
| `fitsense.planning.policy` | `PLAN-HIST-1` | `@Value` de `PlanningPolicySelector`; ningún `application*` la sobrescribe |
| `fitsense.planning.policy.allow-candidates` | `false` | Ídem |

**Si no se cumple:** con una candidata y sin permiso, o con una versión desconocida (incluida `PLAN-C2D-0.1`), la aplicación **no arranca** (`PoliticaPlanificacionTest.seleccion`).

## 5. Cambios de comportamiento con la configuración por defecto

| Aspecto | Cambio |
|---|---|
| Generación, validación y adaptación | **Ninguno**: 960/960 perfiles idénticos a `generation` (`COMPARACION_HISTORICA_PR18.md`) |
| Contrato con la IA | **Ninguno**: JSON y prompt idénticos con la histórica (`ContratoV4CIATest`) |
| Esquema | Dos columnas NULL nuevas; los planes anteriores quedan con NULL, que equivale a la histórica, y no se reescriben |
| Persistencia | Cada plan nuevo guarda `planning_policy = PLAN-HIST-1` y su traza |
