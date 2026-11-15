# Regresión integrada final (v1.1)

## 1. Resultado

| | Commit **`12c6897028fa1af183596d973961b8c39768a981`** |
|---|---|
| Unitarias (surefire) | **1540 · 0 fallos · 0 errores** · 1 omitida |
| Integración (failsafe, PostgreSQL 16 en Testcontainers) | **754 · 0 fallos · 0 errores** · 57 omitidas (opt-in) |
| Resultado de Maven | **BUILD SUCCESS**, 31:39 min (04:16–04:48 UTC, 15/11/2026) |
| Árbol de trabajo | Sin cambios antes, después de la regresión y al terminar las pruebas opt-in (`git status --porcelain` vacío) |
| Pruebas nuevas respecto a v1 | 10 unitarias (`ComparacionDosisPorColocacionTest`) y 1 de integración (`P03ComparacionPorColocacionIT#cambioDeDia`) |

**Condiciones:**

- Worktree aislado (`wt_cons_reg`, HEAD desacoplado en `12c6897`), en el que no se compiló ni ejecutó nada más.
- Orden: `sh mvnw -o verify -Pintegration`.
- Configuración de la candidata: P-03 por colocación **encendida** en todas las pruebas (`src/test/resources/config/application.properties`).
- **Migraciones desde cero:** cada clase de integración arranca un PostgreSQL 16 vacío y aplica Flyway V1…V56 completo, con Hibernate `ddl-auto=validate`.
- La huella permanente del catálogo (`CatalogoCalidadIT`, 1.376 filas) pasa.

**Evidencia** (`evidencia/regresion/`):

- `resumen_regresion_12c6897.txt`;
- `surefire_por_clase.tsv` (143 clases);
- `failsafe_por_clase.tsv` (157 clases).

Las clases `@Nested` publican informes propios con su nombre visible, así que **el total que cuenta es el de Maven**.

## 2. Pruebas opt-in sobre el mismo commit

| Prueba | Resultado | Comparación con v1 (`836a1f5`) | Evidencia |
|---|---|---|---|
| Rejilla de 168, contención **encendida** | 168/168 PLAN_VALIDO; 0 contenidos en los planes | **Idéntica fila a fila** | `evidencia/rejilla_168/` |
| Rejilla de 168, contención **apagada** | 168/168 | **Idéntica fila a fila**. `impacto_168.tsv` es idéntico: 20 planes usarían identidad no determinada, 9 apoyo suspendido y 7 el 1113 en principiantes | `evidencia/rejilla_168/` |
| Rejilla de 312 de hipertrofia (`Hipertrofia4Dv34IT`) | 298 VALIDO, 14 FALLO (V20, los 14 de peso corporal del 775), `violaciones.txt` vacío | **Idéntica** | `evidencia/rejilla_312/` |
| Referencia de 72 conservadora (`GeneracionReferenciaConservadoraIT`) | 72/72, igual a `generacion_referencia_conservador_v1.txt` | Igual. Las referencias históricas V35–V40 también pasan | regresión |
| Cuatro objetivos | Cubiertos por las rejillas 168 y 72, `GeneracionPorObjetivoIT` y `HG9GeneralFitnessIT` | Sin cambios | regresión |
| Volcado congelado ×2 (`CatalogoCongeladoIT`) | **REPRODUCIBLE** entre las dos reconstrucciones, y **idéntico a v1**, huella por huella | Catálogo y Flyway sin cambios | `evidencia/congelado/` |
| 40 escenarios HALL-GM-01, P-03 por colocación encendida | 35 publicadas sin aumentos, 5 NO_FACTIBLE, `trabajo_tras_dolor = 0` y nada reaparece | **Idéntico** al de v1 con las protecciones encendidas | `evidencia/hall_gm_01/` |

## 3. Matrices de seguridad y comportamiento (todas en verde en la regresión)

| Área | Pruebas (clases) |
|---|---|
| V15 | `V15CambioDeGrupoIT`, `CatalogoV15CoherenciaTest` (19), `V15GruposPrescribiblesTest` (20) |
| V20 | `V20PisoExactoTest` (16), `V20ComposicionCasosIT`, `GeneracionR0548V20IT`, `HerenciaReduccionV20Test` (14) |
| V24 | `ProgresionV24V8CTest` (18), `SustitutoDosisCapacidadTest` (9), `SustitutoGmSinMasTrabajoTest` (4) |
| V25 y banderas (impacto, inversión, apoyo suspendido) | `CatalogoIntegralSeguridadIT` (42), `CatalogoElegibleIT` (3), `ContencionTextoBanderaIT` (15) y `ContencionTextoBanderaTest` (11), `ImpactoPolicyTest`, `SafetyProfileDeclaradasTest` |
| V8C | `V8CRetiradasYDescansoTest` (25), `V8CDiagnosticoSustitucionesTest` (11), `DolorSinMasTrabajoV8CTest` (6), `RetiradaLocalizadaV8CTest` (13) |
| D8 | `HerenciaD8IT` (6) |
| Contención INC-CAT-CONS-v1, C39 y 1374 | `ExternalCatalogServiceContencionConservadoraTest`, `…ContencionC39Test` (17), `…Contencion1374Test` (9), `Plan78ContencionReducidaIT` (4) |
| Apoyo suspendido (V53 A/B, apagadas) | `ApoyoSuspendidoInterruptorIT` (5), `PermisoApoyoSuspendidoIT` (9), `PermisoApoyoSuspendidoResolucionTest` (22, incluida la exclusión A+B) |
| Dolor | `ProteccionPlanesPublicadosIT` (12), `PublishedPlanSafetyServiceDolorTest` (7), `RespaldoConDolorIT`, `SinSustitutoDolorLimite*IT`, `LimiteDificultadDolorTest` (22), `O12DolorProgresion*IT` |
| Adaptación localizada, expandida y NO_FACTIBLE | `RetiradaLocalizada*IT` (6 clases), `AdaptacionCompletaIT` (22), `PoliticasP123IT` (39), `RegeneracionPorColocacionRev2IT` |
| P-03 nivel 2 | `P03ComparacionPorColocacionIT` (2), `ComparacionDosisPorColocacionTest` (10), `ProgresionIT#p03Nivel2*` |
| Progresión y reincorporación | `ProgresionIT` (24), `ProgresionRev2IT` (24), `ProgresionCienPorCienIT` (5), `PendientesProgresion*IT`, `Reincorporacion4DTest`, `ReincorporacionTrasProgresionTest` |
| Planes publicados | `ProteccionPlanesPublicadosIT`, `Plan78ContencionReducidaIT`, `ReparacionLinajeRev2IT`, `LinajeIntegridad*IT` |

## 4. Diferencias de comportamiento respecto a v1

**Hay una sola.** En la auditoría `p03Nivel2TrasProgresion`, que se ejecuta en `ProgresionIT` y en `ProgresionRev2IT`, la regeneración P-03 nivel 2 del usuario 17:

- en v1 se **publicaba**;
- en v1.1 queda **NO_FACTIBLE** por la comparación por colocación.

El motivo: el 253 (3x120 s) ocupa el puesto del 295 retirado (2x12), con más series. La prueba admite los dos desenlaces y pasa. Análisis completo en `VALIDACION_P03_POR_COLOCACION.md` §3; registros en `evidencia/p03/regeneraciones_v1_vs_v11.txt`.

| Regeneraciones P-03 (ejecuciones en la regresión) | v1 | v1.1 |
|---|---|---|
| Publicadas | 10 (7 escenarios) | 8 (6 escenarios) |
| NO_FACTIBLE | 15 | 17 |
| De NO_FACTIBLE a publicada | — | 0 |

**Ninguna otra salida cambia:**

- las rejillas 168 y 312;
- la referencia de 72;
- HALL-GM-01;
- el volcado congelado;
- la huella.

## 5. Criterios de éxito

| Criterio | Resultado |
|---|---|
| Cero fallos | ✅ 0 fallos y 0 errores en 2294 pruebas (58 opt-in omitidas en la ejecución general; las relevantes se ejecutaron aparte, §2) |
| Cero regresiones sin explicación | ✅ El único cambio está explicado (§4) |
| Cero aumentos o restauraciones no autorizados | ✅ Ninguno publicado; los dos contraejemplos se bloquean |
| Cero ejercicios contenidos recomendados | ✅ Rejilla 168 encendida: 0. HALL-GM-01: nada reaparece |
| Cero cambios de política no autorizados | ✅ Solo P-03 por colocación, autorizada para la candidata y sus pruebas |
| Cero modificaciones retroactivas | ✅ La protección de planes publicados pasa; ninguna sesión histórica cambia |
| Cero activaciones accidentales | ✅ En producción, P-03 por colocación, V53 A, V53-B y #64 están apagadas. `PermisoApoyoSuspendidoResolucionTest` impide A+B juntas |

**Dictamen de la regresión: aprobada** sobre `12c6897`, sin interrupciones.
