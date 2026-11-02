# Regresión del PR #17 actualizado

**Entorno:**
- PostgreSQL 16 efímero (Testcontainers);
- `fitsense.ai.enabled=false` y dobles de IA: **0 llamadas a Replicate**;
- sin Neon ni Railway.

**Cada resultado lleva el commit sobre el que se ejecutó.** El `src/main` es el mismo en 7b6f666 y en 97befb6 (y en 73b7008 y 1e42519). Entre 7b6f666 y 97befb6 solo cambian dos pruebas.

## 1. Resultados

| Ejecución | Commit | Contabilizadas | Ejecutadas | Aprobadas | Fallidas | Omitidas |
|---|---|---|---|---|---|---|
| Unitarias (`mvn -o verify -Pintegration` completa) | 7b6f666 | 1.046 | 1.046 | 1.046 | 0 | 0 |
| Integración (la misma ejecución) | 7b6f666 | 492 | 438 | 438 | 0 | **54** |
| Batería de 15 clases, `PLAN-HIST-1` | 7b6f666 | 225 | 223 | 223 | 0 | 2 |
| Batería, `PLAN-C2D-0.1` | 7b6f666 | 225 | 223 | 223 | 0 | 2 |
| Batería, `PLAN-C2D-EF-0.1` | 7b6f666 | 225 | 223 | 222 | **1** | 2 |
| Batería, `PLAN-C2D-V4-EF-0.1` | 7b6f666 | 225 | 223 | 222 | **1** | 2 |
| `LinajeIntegridadIT` + `PoliticasP123IT`, las 4 políticas | 97befb6 | 4 × 40 | 4 × 40 | 4 × 40 | 0 | 0 |
| **Batería, `PLAN-HIST-1`** | **97befb6** | PENDIENTE_BAT_HIST |
| **Batería, `PLAN-C2D-0.1`** | **97befb6** | PENDIENTE_BAT_C2D |
| **Batería, `PLAN-C2D-EF-0.1`** | **97befb6** | PENDIENTE_BAT_EF |
| **Batería, `PLAN-C2D-V4-EF-0.1`** | **97befb6** | PENDIENTE_BAT_V4EF |
| Seis ciclos reales `CicloReal*IT` (HIST, C2-D, C2-D-V4, C2-D-EF, C2-D-V4-EF, C2-D-EF-LW02) | 7b6f666 | 6 | 6 | 6 | 0 | 0 |
| `ComparacionPerfilesIT` (960 × 5 políticas) | 7b6f666 | 1 | 1 | 1 | 0 | 0 |
| REPLAY de 100 semanas (`CampanaP123Cohorte02IT`) | bc05ec9 (PR #20: `generation` más el reloj; mismo `src/main` que `generation`) | 2 | 1 | 1 | 0 | 1 |

**Batería de 15 clases:** `AdaptacionCompletaIT`, `AdaptacionPorCausasIT`, `AdaptacionRegresionIT`, `HerenciaD8IT`, `PoliticasP123IT`, `ProgresionIT`, `ProgresionCienPorCienIT`, `ProteccionPlanesPublicadosIT`, `CicloConcurrenteIT`, `LinajeIntegridadIT`, `DuracionForzadaIT`, `ValidacionMultisemanaIT`, `AdherenciaIT`, `CorreccionesReplicateEncendidoIT` y `ContinuidadSemanalIT`. Se ejecuta con `-Dfitsense.planning.policy=<P>` y `allow-candidates=true`, **solo en la prueba**.

**Omitidas (no cuentan como aprobadas):**
- **54** en la integración: laboratorios *opt-in* y pruebas con IA real. Son las mismas 54 que antes de la fusión.
- **2** en cada batería: las de siempre.
- **1** en la campaña: `inspeccionN04`.

### El fallo de las baterías EF en 7b6f666

**Qué falló:** `LinajeIntegridadIT.linaje:119`, «premisa: el viernes acortado (32) queda bajo el piso 31».

**Por qué:** no es un fallo del PR. Es una **prueba de `generation` que dependía de la fecha** (`LocalDate.now()` + 1 semana). Desde el 2-nov usa la semilla del 9-nov, y con la generación EF su **premisa** de partida ya no se da.

**Cómo se resolvió:** se corrigió en 97befb6, solo en pruebas (ver `ACTUALIZACION_PR17.md` §2 bis), y las cuatro baterías se repitieron enteras sobre 97befb6.

**Lo que no se hizo:** no se ha relajado ni eliminado ninguna aserción.

**CI de GitHub (`verify`):**
- 7b6f666: ejecución #103, **success**.
- 97befb6: ver `CIERRE_ESTABILIZACION.md` §A.

## 2. Comparación con la regresión anterior (1e42519)

| | Antes | **Ahora** | Diferencia y causa |
|---|---|---|---|
| Unitarias | 1.028 | **1.046** | +18: `TrainingWeekTest` (PR #19) |
| Integración contabilizadas / ejecutadas / omitidas | 492 / 438 / 54 | **492 / 438 / 54** | Sin cambios |
| Baterías | 223/225 cada una | **Ver §1** (97befb6) | — |
| Ciclos reales | (dentro de las campañas anteriores) | **6/6** | Ahora con el reloj de pruebas |
| 960 perfiles | SHA `c582fce2…` / `ee9d2111…` | **Los mismos** | Sin cambios |

## 3. Grupos que pide la orden (fase 3)

| Grupo | Dónde | Resultado |
|---|---|---|
| Unitarias e integración | §1 | 0 fallos |
| HIST, C2D, C2D-EF y C2D-V4-EF | §1 (97befb6) | Ver §1 |
| V4-C | `TrazaV4CTest` (15), `V4CValidadorRealTest`, `BrechaRedondeoV4Test`, `PoliticaV4CCicloIT` (3), `TrazaHistoricaIT` | 0 fallos |
| V20, V24, V8C | `V20*`, `GeneracionReferencia*IT`, `ConservationValidator*Test`, `HerenciaD8IT` y `AdaptacionCompletaIT` en la completa y en las baterías | 0 fallos |
| D8, P-02, P-03 | `HerenciaD8IT`, `PoliticasP123IT` (39 + 1), `DuracionForzadaIT` y `AdaptacionCompletaIT` | 0 fallos |
| Progresión y reincorporación | `ProgresionIT` (24), `ProgresionCienPorCienIT` (5), `Reincorporacion4DTest`, la REPLAY (65 progresiones, 1 reincorporación autorizada) y `ReincorporacionIaRealIT` en modo seco (PR #20) | 0 fallos |
| Concurrencia e idempotencia | `CicloConcurrenteIT`, ciclos repetidos de `PoliticaV4CCicloIT` y los seis `CicloReal*IT` (cada ciclo se repite y debe dar el mismo plan) | 0 fallos |
| 960 perfiles | `COMPARACION_960_PERFILES.md` | Idénticos |

## 4. Condiciones

| Condición | Resultado | Evidencia |
|---|---|---|
| Comportamiento histórico idéntico | **Sí** | 960/960 iguales; REPLAY idéntica; batería HIST |
| 0 cambios inesperados en ejercicios, dosis o descansos | **Sí** | Volcado 960 × 5 idéntico byte a byte |
| Los 48 perfiles siguen identificados con EF | **Sí** | 48/48 inválidos por VEF con las dos políticas EF |
| Sin reincorporaciones automáticas | **Sí** | Solo la de N08, por confirmación explícita (REPLAY); en el diagnóstico, ningún excluido vuelve |
| Sin progresiones duplicadas | **Sí** | `ProgresionIT`, `ProgresionCienPorCienIT`; `assertSinViolaciones` (≤ 1 progresión por intento) en la batería |
| La corrección de fechas no cambia decisiones del motor | **Sí** | Solo `src/test`; volcado idéntico |
