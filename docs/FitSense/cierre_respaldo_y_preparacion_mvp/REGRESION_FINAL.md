# Regresión final

**Entorno de todas las ejecuciones:**
- PostgreSQL 16 efímero (Testcontainers);
- IA simulada o apagada: **0 llamadas a Replicate**;
- sin Neon ni Railway.

**Omitidas:** son *opt-in* o IA real y **no cuentan como aprobadas**.

**Una incidencia de entorno:** el primer intento de la cadena del PR #17 encontró Docker caído (todas las fases en 1 y «DOCKER_CAIDO»). Se reinició Docker y la cadena se repitió **entera** (`/tmp/claude-0/mvp17b`). No se reutilizó ningún resultado del primer intento.

## 1. `generation` 8f112d5 (tras fusionar el PR #20)

| Ejecución | Resultado |
|---|---|
| CI `verify` (`push`, #109) | **success** |
| CI del PR #1 (#110) | **success** |

## 2. PR de D1: Fitsense-Real/Backend#21 (`fix/asercion-respaldo-dolor`, e033025)

| Ejecución | Contabilizadas | Ejecutadas | Aprobadas | Fallidas | Omitidas |
|---|---|---|---|---|---|
| Unitarias (`mvn -o verify -Pintegration`) | 1.028 | 1.028 | 1.028 | 0 | 0 |
| Integración (la misma ejecución) | 472 | 436 | 436 | 0 | **36** |
| `ValidacionMultisemanaIT` + `RespaldoConDolorIT`, semana 2026-10-26 | 60 | 58 | 58 | 0 | 2 |
| Lo mismo, semana 2026-11-09 | 60 | 58 | 58 | 0 | 2 |
| Lo mismo, semana 2026-11-02 | 60 | 58 | 58 | 0 | 2 |
| CI `verify` (e033025) | | | | | **success** |

**Integración frente a `generation`:**
- +3 ejecutadas: `RespaldoConDolorIT`;
- +3 omitidas: los 2 tests del laboratorio 1373 y el diagnóstico del respaldo, todos *opt-in*.

## 3. PR #17 actualizado (cc97045)

| Ejecución | Contabilizadas | Ejecutadas | Aprobadas | Fallidas | Omitidas |
|---|---|---|---|---|---|
| Unitarias | 1.046 | 1.046 | 1.046 | 0 | 0 |
| Integración | 493 | 438 | 438 | 0 | **55** |
| Batería de 15 clases, `PLAN-HIST-1` | 225 | 223 | 223 | 0 | 2 |
| Batería, `PLAN-C2D-0.1` | 225 | 223 | 223 | 0 | 2 |
| Batería, `PLAN-C2D-EF-0.1` | 225 | 223 | 223 | 0 | 2 |
| Batería, `PLAN-C2D-V4-EF-0.1` | 225 | 223 | 223 | 0 | 2 |
| Seis `CicloReal*IT` | 6 | 6 | 6 | 0 | 0 |
| `ComparacionPerfilesIT` (960 × 5 políticas) | 1 | 1 | 1 | 0 | 0 |
| REPLAY de 100 semanas (`CampanaP123Cohorte02IT`) | 2 | 1 | 1 | 0 | 1 |
| CI `verify` | cc97045, c00e6e7 y 379b768: **success** | | | | |

**Omitidas:**
- integración: 55 = las 54 de antes más `DiagnosticoRespaldoMultisemanaIT` (*opt-in*, del PR #20);
- baterías: 2 cada una, las de siempre;
- campaña: 1 (`inspeccionN04`).

## 4. Grupos que pide la orden (fase 5)

| Grupo | Dónde | Resultado |
|---|---|---|
| Unitarias e integración | §2 y §3 | 0 fallos |
| HIST, C2D, C2D-EF, C2D-V4-EF | §3 | 223/225 cada una |
| Pruebas de RESPALDO | `RespaldoConDolorIT` (3), `ValidacionMultisemanaIT` (57, tres semanas) y la REPLAY (3 RESPALDO históricos idénticos) | 0 fallos |
| P-02 y P-03 | `PoliticasP123IT` (40), `RespaldoConDolorIT`, en la completa y en las 4 baterías | 0 fallos |
| V4-C | `TrazaV4CTest`, `V4CValidadorRealTest`, `BrechaRedondeoV4Test`, `PoliticaV4CCicloIT`, `TrazaHistoricaIT` | 0 fallos |
| V8C y V24 | `ConservationValidator*Test`, `HerenciaD8IT`, `AdaptacionCompletaIT` y las baterías | 0 fallos |
| Progresión y reincorporación | `ProgresionIT`, `ProgresionCienPorCienIT`, `Reincorporacion4DTest` y la REPLAY (65 progresiones, 1 reincorporación autorizada) | 0 fallos |
| Concurrencia e idempotencia | `CicloConcurrenteIT`, `PoliticaV4CCicloIT` y los seis `CicloReal*IT` (ciclos repetidos) | 0 fallos |
| 960 perfiles | §5 | Idénticos |
| REPLAY de 100 semanas | §5 | Idéntica salvo marcas de tiempo |

## 5. Comportamiento conservado

| Verificación | Resultado | Evidencia |
|---|---|---|
| Sin cambios inesperados de ejercicios ni dosis | **Sí** | 960 × 5: `planes.csv` SHA-256 `c582fce2…74ee2` y `colocaciones.csv` `ee9d2111…aa3d9`, **idénticos byte a byte** a la evidencia anterior |
| Sin restauraciones automáticas | **Sí** | `RespaldoConDolorIT` B (la semana siguiente no reincorpora); la REPLAY solo tiene la reincorporación autorizada de N08 |
| Sin incrementos de volumen no autorizados | **Sí** | Invariantes de trabajo en las pruebas de respaldo; P-03 rechaza los aumentos (caso 1373) |
| Sin publicaciones inválidas | **Sí** | Caso C: NO_FACTIBLE sin publicar |
| Política histórica sin diferencias | **Sí** | HIST idéntica a `generation` (`src/main` de `generation` sin cambios desde bfd5756) |
| Sin cambios de producción | **Sí** | `src/main` sin cambios en el PR #20 (fusionado), en el PR #21 y en el PR #17 desde 73b7008 |
| Sin fallos por fechas o vencimientos | **Sí** | 0 errores «ya venció»; tres semanas probadas en D1 |
| REPLAY | **Idéntica** | Solo difieren `recorded_at` y `reactivated_at` de `exclusiones_dolor.csv`; normalizadas, iguales. Revertido |
