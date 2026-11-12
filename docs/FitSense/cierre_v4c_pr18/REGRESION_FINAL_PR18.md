# Regresión final del PR #18

**Código probado:** commit **a52e35e** (`feature/v4c-versionado-aislado`).

**Commits posteriores:** solo añaden documentación en `docs/FitSense/cierre_v4c_pr18/`. **No cambian código ni pruebas** (`git diff a52e35e..HEAD -- src` vacío). Su resultado de CI se indica aparte.

**Entorno:**
- PostgreSQL 16 efímero (Testcontainers);
- `fitsense.ai.enabled=false` y dobles de IA: **0 llamadas a Replicate**;
- sin conexión a Neon ni a Railway.

## 1. Ejecuciones sobre a52e35e

| Ejecución | Contabilizadas | Ejecutadas | Aprobadas | Fallidas | Omitidas |
|---|---|---|---|---|---|
| Unitarias (`mvn -o verify -Pintegration`, completa) | 1.010 | 1.010 | 1.010 | 0 | 0 |
| Integración (la misma ejecución completa) | 466 | 433 | 433 | 0 | **33** |
| Batería de 15 clases con `PLAN-V4-0.1` | 225 | 223 | 223 | 0 | **2** |
| `ComparacionPerfilesIT` (opt-in, 960 perfiles × HIST y V4-C) | 1 | 1 | 1 | 0 | 0 |

**Las omitidas no se cuentan como aprobadas.** Son:
- **33** en la ejecución completa: laboratorios opt-in (incluido `ComparacionPerfilesIT`) y pruebas que exigen IA real, las mismas de antes;
- **2** en la batería: las mismas de siempre.

## 2. Comparación con la referencia anterior

| | Referencia | **a52e35e** | Diferencia y causa |
|---|---|---|---|
| Unitarias | 995 (d1de25c, solo unitarias) | **1.010** | +15: `TrazaV4CTest` |
| Integración ejecutadas | 429 (6aec7e0, ejecución completa) | **433** | +4: `GeneracionIAV4CIT` y `GeneracionIAHistoricaIT` (añadidas en d1de25c, antes ejecutadas aparte), `PoliticaV4CCicloIT.acortarTrazaDiferenciada` y `TrazaHistoricaIT` (nuevas) |
| Integración omitidas | 33 | **33** | Sin cambios |
| Batería con PLAN-V4-0.1 | 225 / 223 / 2 | **225 / 223 / 2** | Sin cambios |

**Es la primera ejecución completa** que incluye a la vez las pruebas de d1de25c y las de esta orden. En órdenes anteriores las pruebas de d1de25c se ejecutaron por separado.

## 3. Pruebas pedidas por la orden (todas dentro de la ejecución completa de a52e35e)

| Grupo | Pruebas | Resultado |
|---|---|---|
| V4-C | `PoliticaPlanificacionTest` (18), `V4CValidadorRealTest` (19), `BrechaRedondeoV4Test` (16), `ContratoV4CIATest` (3), `TrazaV4CTest` (15) | 0 fallos |
| Traza en el ciclo real | `PoliticaV4CCicloIT` (3), `TrazaHistoricaIT` (1) | 0 fallos |
| Normalización de la duración de la IA | `PlanDraftNormalizerDuracionTest` (2), `GeneracionIAV4CIT`, `GeneracionIAHistoricaIT` | 0 fallos |
| Migración V44 | `MigracionV44IT`, `MigracionV43IT`, `EsquemaVigenteSobreCatalogoAnterior` | 0 fallos |
| Adaptación y P-03 | En la ejecución completa (configuración histórica) y en la batería con PLAN-V4-0.1: `AdaptacionCompletaIT`, `AdaptacionPorCausasIT`, `AdaptacionRegresionIT`, `HerenciaD8IT`, `PoliticasP123IT`, `LinajeIntegridadIT`, `CicloConcurrenteIT`, `ProgresionIT`… | 0 fallos |
| 960 perfiles | `ComparacionPerfilesIT` | Ver `COMPARACION_HISTORICA_PR18.md` |

## 4. CI de GitHub

| Commit | Contenido | `verify` (`mvn -B -ntp verify -Pintegration`) |
|---|---|---|
| d1de25c | Pruebas de IA y redondeo | success |
| **0931ee5** | a52e35e (código) + documentación | **success** (19:18 UTC) |
| Commit final de documentación | Este informe y el cierre | Se indica en `CIERRE_PR18.md` |
