# Actualización del PR #17 desde `generation`

**Rama:** `feature/c2d-v4c-versionado`. **Antes:** 6d6e522. **Fusión:** commit **1e42519**, que incorpora `origin/generation` (bfd5756).

**Método:** `git merge --no-ff`. **Sin rebase y sin reescritura de historia.**

## 1. Conflictos (13) y resolución

**Criterio:**
- Para cada archivo se comprobó si el PR #18 lo había cambiado **después** del port original (6aec7e0 → fa7402b).
- Si no lo había cambiado, la versión del PR #17 ya contiene todo lo del PR #18 y es su superconjunto (C2-D, EF y opciones).

| Archivo | ¿Cambió en el PR #18 tras el port? | Resolución |
|---|---|---|
| `PlanningPolicy.java` | No | PR #17 (7 versiones, indicadores c2d y EF) |
| `PlanningPolicySelector.java` | No | PR #17 |
| `WeeklyTrainingPlan.java` | No | PR #17 |
| `WeeklyTrainingPlanCommandServiceImpl.java` | No | PR #17 (opciones LW-PREF-0.2 y REP-SUST-0.1) |
| `SessionFixPromptBuilder.java` | No | PR #17 (regla EF para la IA) |
| `RuleBasedTrainingPlanGenerator.java` | No | PR #17 (REP-SUST-0.1) |
| `EsquemaVigenteSobreCatalogoAnterior.java` | No | PR #17 (incluye V45) |
| `ComparacionPerfilesIT.java` | No | PR #17 (5 políticas) |
| `PoliticaPlanificacionTest.java` | No | PR #17 (cubre también C2-D y EF) |
| `ContratoV4CIATest.java` | No | PR #17 |
| `V4CValidadorRealTest.java` | No | PR #17 |
| `PoliticaV4CCicloIT.java` | **Sí** (+70 líneas: `acortarTrazaDiferenciada`) | **La de `generation`**: la del PR #17 era idéntica a la de d1de25c; la de `generation` es esa más la prueba nueva |
| **`PlanningPolicyTrace.java`** | **Sí** (a52e35e) | **Integración manual**, ver `VERIFICACION_TRAZABILIDAD.md` |

**Sin conflicto, añadidos desde `generation`:**
- `TrazaV4CTest.java`;
- `TrazaHistoricaIT.java`;
- `docs/FitSense/cierre_v4c_pr18/` (9 documentos).

**Sin conflicto y sin duplicar:** V44 (mismo archivo y checksum) y los archivos idénticos en las dos ramas (`GeneracionIAV4C*`, `BrechaRedondeoV4Test`, etc.).

## 2. Resultado frente al PR #17 anterior (6d6e522)

**En `src/`, solo cambian 4 archivos:**

| Archivo | Cambio |
|---|---|
| `PlanningPolicyTrace.java` | Campos de V4-C añadidos |
| `PoliticaV4CCicloIT.java` | Prueba de ACORTAR |
| `TrazaV4CTest.java` | Nuevo |
| `TrazaHistoricaIT.java` | Nuevo |

**No cambia ningún otro archivo de código:** C2-D, EF-0.1, V45, LW-PREF-0.2, REP-SUST-0.1, NOFACT-0.1 y los contratos de duración y aeróbica quedan exactamente como estaban.

## 3. Comprobaciones de integridad

| Comprobación | Resultado |
|---|---|
| V44 sin duplicar | Un único `V44__planning_policy_version.sql`, SHA-256 `640b0169…` |
| V45 después de V44 | V40 … V43, **V44, V45** |
| Métodos duplicados | `CapabilityWeekBuilder.excede(...)`: 1. `RuleBasedTrainingPlanGenerator.excedeTecho(...)`: 1 |
| Marcas de conflicto | Ninguna en `src/` ni `docs/` |
| Compilación | `mvn test-compile` correcto |
| Pruebas perdidas | Ninguna: se conservan todas las del PR #17 y se añaden las 2 nuevas del PR #18 |
| Política por defecto | `PLAN-HIST-1`, `allow-candidates=false`; ningún indicador del PR #17 cambia (todos siguen en `false`) |
| Contratos históricos | Sin cambios; ver la regresión, `ContratoDuracionApagadoIT` y `NoFactibleApiApagadoIT` |

**Sin conflictos de resultado dudoso:** todos se justificaron con el criterio del §1 y con la comparación de cada archivo frente a su versión anterior.
