# Actualización del PR #17 desde `generation` (después del PR #20)

**PR:** Fitsense-Real/Backend#17 (`feature/c2d-v4c-versionado`). Sigue **en borrador** y sin fusionar.

## 1. Fusión

| | Valor |
|---|---|
| Cabeza anterior | 69b08a5 |
| `generation` fusionada | **8f112d5** (fusión del PR #20) |
| Método | **Merge** (`git merge --no-ff`), sin *rebase* |
| Commit de fusión | **cc97045** |
| Conflictos | **Ninguno** |
| Entra | `RelojPruebas` con la propiedad de diagnóstico, `CampanaP123Cohorte02IT`, `CampanaMvp8UsuariosIT` y `ReincorporacionIaRealIT` con el reloj, y `DiagnosticoRespaldoMultisemanaIT`. `LinajeIntegridadIT` y `PoliticasP123IT` ya llevaban el mismo cambio (97befb6): git lo resolvió sin conflicto |
| `src/main` | **Sin cambios** (`git diff 69b08a5 cc97045 -- src/main` vacío) |

**El PR de D1** (Fitsense-Real/Backend#21) **no se ha incorporado**, porque está pendiente de tu aprobación y sin fusionar. Se incorporará con otro *merge* de `generation` cuando se fusione.

## 2. Funcionalidad conservada

| Elemento | Estado |
|---|---|
| C2-D, EF-0.1, LW-PREF-0.2, REP-SUST-0.1, NOFACT-0.1 y V45 | Intactos (`src/main` sin cambios) |
| Campos de traza de V4-C, C2-D y EF en `PlanningPolicyTrace` | Intactos |
| V44 | Un único archivo, SHA-256 `640b0169…e2708400` |
| Política por defecto | `PLAN-HIST-1`, `allow-candidates=false` |
| Indicadores | Todos apagados |
| Borrador | Sí |

## 3. Pruebas y CI

| | Resultado |
|---|---|
| CI `verify` | **success** en cc97045, c00e6e7 y 379b768 |
| Regresión local (cc97045) | 1.046 unitarias; integración 493/438/55; 4 baterías 223/225; 6 ciclos reales; 960 perfiles idénticos; REPLAY idéntica (`REGRESION_FINAL.md`) |
