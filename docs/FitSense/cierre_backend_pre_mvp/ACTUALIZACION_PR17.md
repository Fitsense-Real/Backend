# Actualización del PR #17 desde `generation` (después del PR #21)

**PR:** Fitsense-Real/Backend#17, rama `feature/c2d-v4c-versionado`.

**Estado:** **borrador, sin fusionar.** La orden no autoriza fusionarlo.

## 1. Fusión

| | Valor |
|---|---|
| Cabeza anterior | 8f02860 |
| `generation` incorporada | **7c19703** (fusión del PR #21) |
| Método | **Merge**, sin *rebase* ni reescritura de historia |
| Commit de fusión | **51a311f** |
| Conflictos | **Ninguno** |
| Qué entra | `InvariantesRespaldo`, `RespaldoConDolorIT`, `AnalisisSustitucion1373IT` (*opt-in*) y la corrección de `ValidacionMultisemanaIT` |
| `src/main` | **Sin cambios**: `git diff 8f02860 51a311f -- src/main` vacío |
| Commits posteriores | Solo documentación (`docs/FitSense/cierre_backend_pre_mvp/`) |

## 2. Funcionalidad conservada (sin cambios)

| Elemento | Estado |
|---|---|
| V44 | Un único archivo; SHA-256 `640b0169d20f24c0…` (igual que antes) |
| V45 | Sin cambios |
| C2-D, EF-0.1, LW-PREF-0.2, REP-SUST-0.1, NOFACT-0.1 | Intactos (`src/main` sin cambios) |
| Política por defecto | `PLAN-HIST-1`; `allow-candidates=false` |
| Indicadores (`duracion-estimada`, `metricas.aerobica`, `no-factible-estructurado`) | Apagados |
| Borrador | Sí |

## 3. CI

| Commit | `verify` |
|---|---|
| 51a311f (fusión) | **success** (ejecución 118) |
| 7baeb66 (documentación) | **success** |

**Regresión local completa:** `REGRESION_BACKEND_FINAL.md`.
