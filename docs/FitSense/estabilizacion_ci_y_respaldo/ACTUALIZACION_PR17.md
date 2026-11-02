# Actualización del PR #17 desde `generation`

**PR:** Fitsense-Real/Backend#17 (`feature/c2d-v4c-versionado`), en **borrador**, sin fusionar.

## 1. Fusión

| | Valor |
|---|---|
| Cabeza anterior del PR | 73b7008 |
| `generation` fusionada | **dd61837** (fusión del PR #19) |
| Método | **Merge** (`git merge --no-ff origin/generation`), sin *rebase* ni reescritura de historia |
| Commit de fusión | **bbf4e16** |
| Conflictos | **Ninguno** |
| Cambios que entran | Los 8 archivos del PR #19: `RelojPruebas`, `TrainingWeekTest`, `GeneracionReferenciaBase`, `Usuario31Base`, `ValidacionMultisemanaIT`, `C2ReproduccionN03IT`, `PoliticaV4CCicloIT` y el informe `correccion_pruebas_fecha/INFORME_PRUEBAS_FECHA.md` |
| `src/main` | **Sin cambios** por la fusión |

**Por qué no hubo conflictos:** el PR #19 solo toca `src/test` y `docs`. Ninguno de sus archivos había cambiado en el PR #17 desde la fusión anterior (1e42519). `PoliticaV4CCicloIT` ya era la versión de `generation`.

## 2. Commit adicional, solo de pruebas: 7b6f666

**`CicloRealPoliticaBase`** es la base de los seis `CicloReal*IT` del PR #17: `Historica`, `C2D`, `C2DV4`, `C2DEF`, `C2DV4EF` y `C2DEFLW02`. Todos son *opt-in* y no corren en CI.

**El problema:**
- usa la semana fija 2026-11-02 y **ejecuta sesiones**;
- es el mismo riesgo que el PR #19 corrigió en `PoliticaV4CCicloIT` y `C2ReproduccionN03IT`: en cuanto venza esa semana, `StartWorkoutSessionCommand` rechazaría las sesiones con «ya venció».

**El cambio (5 líneas, el mismo mecanismo):**
- `SEMANA = RelojPruebas.SEMANA_REFERENCIA` (el mismo valor, 2026-11-02);
- `RelojPruebas.abrirVentanaDeEjecucion(jdbc, u)` antes de cada sesión;
- `@MockitoBean OverdueWorkoutScheduler`.

**Qué no cambia:** ninguna aserción, ningún valor esperado y nada de `src/main`. Su efecto se comprueba en `REGRESION_PR17_ACTUALIZADO.md` (los seis ciclos reales, sin fallos).

## 2 bis. Segundo commit de pruebas: 97befb6 (encontrado durante la regresión)

**Síntoma:**
- La batería de 15 clases con `PLAN-C2D-EF-0.1` y con `PLAN-C2D-V4-EF-0.1` dio **1 fallo** en `LinajeIntegridadIT.linaje`: «premisa: el viernes acortado (32) queda bajo el piso 31».
- El 1 de noviembre, con el mismo código, las dos baterías daban 223/225.

**Causa:**
- `LinajeIntegridadIT` (de `generation`) calcula su semana con `LocalDate.now()` + 1 semana.
- Hasta el domingo 1-nov eso era la semana del 2-nov; desde hoy es la del 9-nov, y cambia la semilla.
- Con la generación EF, el viernes de ese plan acortado queda en 32 min y no por debajo del piso de 31 que la prueba necesita **como premisa**.
- **No es un cambio de comportamiento:** es otra prueba que depende de la fecha, del mismo tipo que corrigió el PR #19. Con HIST y C2-D la premisa se sigue cumpliendo con esa semilla, por eso el CI no lo veía.

**Cambio, solo de pruebas:**
- `LinajeIntegridadIT` y `PoliticasP123IT` (esta última, con el mismo cálculo por `LocalDate.now()`) pasan a `RelojPruebas.SEMANA_REFERENCIA`, abren la ventana de ejecución y usan el doble del barrido de vencidos.
- No cambia ninguna aserción ni ningún valor esperado.

**Comprobación:** las dos clases dan **40/40** con las cuatro políticas (HIST, C2-D, C2-D-EF y C2-D-V4-EF) y **40/40 en `generation`**.

**El mismo cambio, para `generation`,** está en el PR de laboratorios Fitsense-Real/Backend#20 (d4717b9). Se aplica sin conflictos.

## 3. Lo que se conserva

| Elemento | Estado | Cómo se comprueba |
|---|---|---|
| Campos de traza de V4-C (`minutos_perfil`, `minutos_efectivos`, `limite_v4_aplicado`, `limite_v4_minutos_base`, `dentro_limite_v4_aplicado`) | Intactos | `PlanningPolicyTrace` no cambia en bbf4e16 ni en 7b6f666. Lo verifican `TrazaV4CTest` y `TrazaHistoricaIT` |
| C2-D, EF-0.1, LW-PREF-0.2, REP-SUST-0.1, NOFACT-0.1 | Intactos | `src/main` del PR #17 idéntico a 73b7008 (`git diff 73b7008 7b6f666 -- src/main` vacío) |
| V44 | Un único archivo, SHA-256 `640b0169…e2708400` | Sin cambios |
| V45 | Solo en el PR #17 | Sin cambios |
| `PLAN-HIST-1` por defecto, `allow-candidates=false` | Sí | Configuración sin cambios |
| Indicadores apagados (`fitsense.api.duracion-estimada.enabled`, `fitsense.api.no-factible-estructurado.enabled`, LW-PREF, aeróbica) | Sí | Configuración sin cambios |
| PR en borrador | Sí | No se ha cambiado su estado |

**Ninguna regla funcional cambia:** el `src/main` del PR #17 es el mismo antes y después de esta actualización (`git diff 73b7008 97befb6 -- src/main` vacío). **Cabeza final del PR #17:** 97befb6 más los commits de documentación de esta orden.
