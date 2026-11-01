# Pruebas de la rama aislada (versionado + V4-C)

**Rama:** `feature/v4c-versionado-aislado`.

**Entorno:**
- PostgreSQL 16 efímero (Testcontainers), Flyway hasta **V44**;
- `fitsense.ai.enabled=false` y dobles de IA: **0 llamadas a Replicate**;
- sin conexión a Neon ni a Railway.

**Ejecuciones de esta rama.** Ninguna de las cifras de esta página se ha ejecutado en el PR #17, ni al revés.

| Ejecución | Commit | Resultado |
|---|---|---|
| `mvn -o verify -Pintegration` (completa) | 6aec7e0 | Unitarias **979**: 0 fallos, 0 omitidas. Integración: **462 contabilizadas, 429 ejecutadas, 33 omitidas, 0 fallos** |
| Batería de 15 clases con `PLAN-V4-0.1` | 6aec7e0 + pruebas de d1de25c | **225 contabilizadas, 223 aprobadas, 0 fallos, 2 omitidas** (las mismas 2 de siempre) |
| `GeneracionIAV4CIT`, `GeneracionIAHistoricaIT`, `BrechaRedondeoV4Test` (añadidas en d1de25c) | d1de25c | 2 de integración y 16 unitarias, **0 fallos** |
| Unitarias completas | d1de25c | Ver `REGRESION_PREINTEGRACION.md` §2 |
| `ComparacionPerfilesIT` (960 perfiles, HIST y V4-C) | Antes y después de d1de25c | Volcados idénticos entre sí y, para PLAN-HIST-1, idénticos a `generation` línea a línea |

**Referencia en `generation`** (afef98a, medida en esta orden): **939 unitarias**, 0 fallos. Las +40 de la rama aislada son:

| Prueba | Unitarias |
|---|---|
| `PoliticaPlanificacionTest` | 18 |
| `ContratoV4CIATest` | 3 |
| `V4CValidadorRealTest` | 19 |
| `BrechaRedondeoV4Test` | +16 en d1de25c |

**Las 33 omitidas** son laboratorios opt-in o pruebas que exigen IA real (incluido `ComparacionPerfilesIT`). **No se cuentan como ejecutadas.**

## Casos exigidos por la orden

| Caso | Prueba | Resultado |
|---|---|---|
| 30, 45, 60 y 75 min | `PoliticaPlanificacionTest.v4cBordes` (12), `V4CValidadorRealTest.bordes` (17), `BrechaRedondeoV4Test` (16), `PoliticaV4CCicloIT` (4 perfiles reales) | Correcto |
| Límite exacto del 115 % | 2.070, 3.105, 4.140 y 5.175 s **pasan** | Correcto |
| ±1 s | 2.069 y 2.071, 3.104 y 3.106, 4.139 y 4.141, 5.174 y 5.176: por debajo pasa, por encima **rechaza** | Correcto |
| Declara **menos** de lo estimado | `bordes`: 30 min con 2.071 s declarando 30, y 60 min con 4.141 s declarando 50 → **rechaza** | Correcto |
| Declara **más** de lo estimado | `bordes`: 30 min con 2.069 s declarando 60, y 45 min con 3.105 s declarando 90 → **pasa** (V4-C no mira lo declarado) | Correcto |
| Generación inicial | `PoliticaV4CCicloIT.cicloConV4C`: 4 perfiles (30/45/60/75) con `planning_policy = PLAN-V4-0.1`, contexto `GENERACION`, `parametros.v4 = SEGUNDOS`, `limite_v4.segundos = m × 69` y todas las sesiones ≤ límite | Correcto |
| Adaptación | El mismo ciclo, 3 semanas: ACORTAR (falta de tiempo, M/2), dolor y fatiga RPE 9. Cada semana nueva es PLAN-V4-0.1, cabe en el límite, es idempotente (repetir el ciclo da el mismo plan) y tiene 1 plan activo por semana. Ningún ejercicio excluido por dolor vuelve | Correcto |
| P-03 | Batería con PLAN-V4-0.1: `PoliticasP123IT`, `AdaptacionCompletaIT`, `HerenciaD8IT`, `LinajeIntegridadIT` | 0 fallos |
| Corrección de sesión con IA simulada | `CorreccionesReplicateEncendidoIT` (C1–C5) con PLAN-V4-0.1 en la batería; `ContratoV4CIATest.snapshot` (`max_session_seconds` también en la corrección) | 0 fallos |
| IA que declara 35 min con 42,5 estimados en un perfil de 30 | `V4CValidadorRealTest.caso142`: V4-C **rechaza** («dura unos 2550 s… techo 2070 s»). `GeneracionIAV4CIT`: en el pipeline real, la propuesta de la IA (35 min declarados, > 2.070 s) se rechaza por V4-C en los 3 intentos y se publica el respaldo, RULE_ENGINE, con todas las sesiones ≤ 2.070 s | Correcto |
| Planes históricos | `MigracionV44IT` (planes anteriores a V44 con NULL = histórica); `PoliticaV4CCicloIT.planesHistoricosIntactos` (un plan sin política **no se reescribe** al publicar la semana siguiente) | Correcto |
| La histórica conserva su comportamiento | `V4CValidadorRealTest.historicaIntacta`; `PoliticaPlanificacionTest.v4Historico` (30–75 min); `GeneracionIAHistoricaIT` (rechaza con su V4 de siempre sobre los minutos normalizados); 960/960 perfiles idénticos a `generation`; `ContratoV4CIATest` (JSON y prompt idénticos) | Correcto |
| Sin cambio automático de la política activa | `PoliticaPlanificacionTest.seleccion` (una candidata sin `allow-candidates` no arranca; desconocida o C2-D no arranca); `PoliticaV4CCicloIT` (`selector.activa()` = la configurada en cada prueba); `entidad` (la versión siguiente hereda la política del plan) | Correcto |

## Hallazgo de esta orden sobre el caso del 142 %

- La prueba de control `GeneracionIAHistoricaIT` mostró que, en el pipeline real, la histórica **también** rechaza esa propuesta, porque el normalizador (B2) reescribe los minutos con los calculados.
- El defecto que V4-C corrige en la práctica es la **brecha de redondeo**: hasta el 118,3 % a 30 min. Detalle en `SEPARACION_V4C.md` §1.
- La primera versión de esa prueba esperaba lo contrario y **falló**. Se corrigió la expectativa de la prueba, no el código.

## Migraciones

| Prueba | Qué comprueba |
|---|---|
| `MigracionV43IT` | V43 sobre un esquema V42 y reinicio |
| `MigracionV44IT` | V44 sobre un esquema V43 con un plan existente: solo se aplica V44; no cambian las columnas, restricciones ni filas anteriores; el plan queda con NULL (= histórica); el CHECK rechaza `PLAN-C2-0.1` y `PLAN-E-0.1`; un reinicio no aplica nada y valida |
| `EsquemaVigenteSobreCatalogoAnterior` | Admite V44 |
