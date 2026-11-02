# Regresión previa a la integración

**Entorno común:**
- PostgreSQL 16 efímero (Testcontainers);
- `fitsense.ai.enabled=false` y dobles de IA: **0 llamadas a Replicate**;
- **ninguna conexión a Neon ni a Railway**.

**Cada cifra se indica con su rama y su commit. Ninguna prueba se da por ejecutada en una rama donde no se ejecutó.**

## 1. PR #17 (`feature/c2d-v4c-versionado`, commit 361d270)

| Batería | Referencia (orden anterior) | **Esta orden** | Diferencia |
|---|---|---|---|
| Unitarias | 995 | **1.013: 0 fallos, 0 errores, 0 omitidas** | +2 `PaqueteEvaluadorTest`, +16 `BrechaRedondeoV4Test` |
| Integración contabilizadas | 483 (53 omitidas) | **490: 0 fallos, 0 errores, 54 omitidas** | +7 |
| **Integración ejecutadas** | 430 | **436** | +6: `ContratoDuracionApagadoIT`, `ContratoDuracionEncendidoIT`, `PoliticaV4CCicloIT` ×2, `GeneracionIAV4CIT`, `GeneracionIAHistoricaIT` |
| Omitidas | 53 | 54 | +1: `ComparacionPerfilesIT` (laboratorio opt-in). Las 53 anteriores siguen igual; **no se cuentan como ejecutadas** |

**Baterías de 15 clases por política** (adaptación, D8, ACORTAR, C-2, P-02, P-03, progresión, reincorporación, concurrencia, linaje, correcciones con IA simulada):

| Política | Contabilizadas | Aprobadas | Fallidas | Omitidas |
|---|---|---|---|---|
| PLAN-HIST-1 | 225 | 223 | 0 | 2 |
| PLAN-C2D-0.1 | 225 | 223 | 0 | 2 |
| PLAN-C2D-EF-0.1 | 225 | 223 | 0 | 2 |

**Campaña REPLAY de 100 semanas** (`CampanaP123Cohorte02IT`, `-Dfitsense.campana.p123=true`, política por defecto):
- **2 contabilizadas, 1 ejecutada sin fallos, 1 omitida** (la de siempre).
- La evidencia sale **idéntica**, salvo las marcas de tiempo de `exclusiones_dolor.csv`: comparada con las marcas normalizadas, es igual. La evidencia se revirtió.

**Otras ejecuciones del PR #17 en esta orden:**
- `ComparacionPerfilesIT` con las 5 políticas (254a18e y 661c4aa);
- `DuracionYAerobicaAssemblerTest` y `PaqueteEvaluadorTest`.

## 2. Rama aislada (`feature/v4c-versionado-aislado`)

| Ejecución | Commit | Resultado |
|---|---|---|
| Unitarias, `mvn verify` completo | 6aec7e0 | 979, 0 fallos |
| Integración, `mvn verify` completo | 6aec7e0 | **462 contabilizadas, 429 ejecutadas, 33 omitidas, 0 fallos** |
| Unitarias finales | **d1de25c** | **995, 0 fallos** (+16 `BrechaRedondeoV4Test`) |
| `GeneracionIAV4CIT` y `GeneracionIAHistoricaIT` | d1de25c | 2/2 |
| Batería de 15 clases con PLAN-V4-0.1 | 6aec7e0 + pruebas de d1de25c | **225 contabilizadas, 223 aprobadas, 0 fallos, 2 omitidas** |
| CI de GitHub (`verify`, `mvn -B verify -Pintegration`) en el PR borrador Fitsense-Real/Backend#18 | d1de25c | **success** |
| `ComparacionPerfilesIT` (HIST y V4-C) | Antes y después de d1de25c | Idénticos; HIST = `generation` línea a línea |

**`generation` (afef98a)**, medido en esta orden:
- unitarias: **939**, 0 fallos;
- comparación de 960 perfiles con PLAN-HIST-1;
- **no** se ha ejecutado su integración completa en esta orden.

## 3. Las 16 comprobaciones obligatorias

| # | Comprobación | Resultado | Evidencia |
|---:|---|---|---|
| 1 | El histórico no cambia | **Sí** | 960/960 perfiles idénticos plan a plan frente a `generation`, en el PR #17 y en la rama aislada; ciclo real 542/542 filas; campaña REPLAY idéntica; batería HIST 223/225 |
| 2 | V4-C valida en segundos | **Sí** | `BrechaRedondeoV4Test` (16), `V4CValidadorRealTest` (19), `PoliticaPlanificacionTest.v4cBordes` (12), `GeneracionIAV4CIT`, `PoliticaV4CCicloIT` |
| 3 | C2-D no altera los ejercicios | **Sí** | Control HIST frente a C2-D: 79 perfiles distintos, **solo** en validez y estado de C2-D; 0 diferencias en ejercicios, dosis, descansos, composición y duración |
| 4 | EF bloquea la falsa tracción | **Sí** | `PoliticasP123IT` con PLAN-C2D-EF-0.1 (`sinFalsaCobertura`) en la batería; `FAMILIAS_EF_USO.csv` (orden anterior, código sin cambios) |
| 5 | Los 48 perfiles siguen identificados | **Sí** | En el volcado del PR #17 actual: con PLAN-C2D-EF-0.1, los 48 son inválidos por VEF (48/48); con PLAN-C2D-0.1 sin EF se publicarían los 48 |
| 6 | Las restricciones por dolor no cambian | **Sí** | Campo de excluidos por dolor idéntico en las 6.720 comparaciones; `PoliticaV4CCicloIT`: 0 excluidos en los planes; baterías (P-02) |
| 7 | D8 y ACORTAR | **Sí** | `HerenciaD8IT`, `AdaptacionPorCausasIT`, `DuracionForzadaIT` en las 4 baterías (3 del PR #17 + V4-C aislada); ACORTAR en `PoliticaV4CCicloIT` |
| 8 | P-03 | **Sí** | `PoliticasP123IT`, `AdaptacionCompletaIT` y `LinajeIntegridadIT` en las 4 baterías |
| 9 | Límites de progresión | **Sí** | `ProgresionIT` y `ProgresionCienPorCienIT` en las 4 baterías; `LONGITUDINAL_CIERRE.csv` (código sin cambios desde 583b723) |
| 10 | Sin restauración automática | **Sí** | Baterías (reincorporación explícita); `PoliticaV4CCicloIT` (0 excluidos vuelven) |
| 11 | Versiones y trazas coherentes | **Sí** | `PoliticasP123IT` (`planning_policy` = la activa) ×4; `PoliticaV4CCicloIT` (política, contexto, parámetros y límite en cada plan; trazas de todas las sesiones); los planes sin política no se reescriben |
| 12 | Concurrencia e idempotencia | **Sí** | `CicloConcurrenteIT` ×4 baterías; `PoliticaV4CCicloIT` (repetir el ciclo da el mismo plan, 1 activo por semana) |
| 13 | Contrato de duración retrocompatible | **Sí** | `ContratoDuracionApagadoIT` (sin campos nuevos con el indicador apagado); `ContratoDuracionEncendidoIT` (solo se añaden) |
| 14 | Las fichas profesionales pasan la validación de estructura | **Sí** | `PaqueteEvaluadorTest` 2/2: 10/10/10 columnas, 36 códigos, ceguera |
| 15 | Histórico comparado perfil a perfil | **Sí** | `DIFERENCIAS_MATRICES.csv`: 6.720 filas, 0 diferencias |
| 16 | Ninguna base remota | **Sí** | Todas las bases son Testcontainers; las pruebas comprueban `jdbc:postgresql://localhost:` o usan `@ServiceConnection` |

## 4. Errores encontrados y corregidos en esta orden

| Error | Corrección |
|---|---|
| `FICHAS_NO_FACTIBLE.csv` con 11 columnas en D01 y D02 | Escapado RFC 4180 y prueba automática (`CORRECCION_FICHAS_PROFESIONALES.md`) |
| La primera versión de la rama aislada duplicaba `excede(...)` al fusionarse en el PR #17 (git no avisa, y no compila) | Método movido a la misma posición que en el PR #17; simulación repetida y limpia |
| La primera versión de `GeneracionIAHistoricaIT` suponía que la histórica admitía el 142 % en el pipeline real | **Expectativa errónea, no código.** El normalizador B2 ya lo impide; la prueba y la documentación se corrigieron (`SEPARACION_V4C.md` §1) |
| Primeros volcados por perfil sin Docker en marcha | Se reinició el demonio y se repitieron |

**Regresiones: 0.**
