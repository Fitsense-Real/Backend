# Regresión del cierre del PR #17

**Entorno:**
- `mvn -o verify -Pintegration` en `feature/c2d-v4c-versionado`, después de todas las correcciones de esta orden.
- PostgreSQL 16 efímero (Testcontainers) y Flyway hasta V45.
- `fitsense.ai.enabled=false` y dobles de IA: **0 llamadas a Replicate**.
- Sin conexión a Neon ni a Railway.

## 1. Recuentos

| Batería | Orden anterior | **Esta orden** | Diferencia |
|---|---|---|---|
| Unitarias | 992, 0 fallos | **995: 0 fallos, 0 errores, 0 omitidas** | +3: `DiagnosticoNoFactibleTest` |
| Integración (contabilizadas) | 473, 45 omitidas | **483: 0 fallos, 0 errores, 53 omitidas** | +2 ejecutadas (`NoFactibleApiEstructuradoIT`, `NoFactibleApiApagadoIT`) y +8 opt-in omitidas (`CierrePR17LabIT` ×7, `CicloRealC2DEFLW02IT` ×1) |
| **Integración ejecutadas** | 428 | **430** | +2 |

**Las 53 omitidas** son laboratorios opt-in o pruebas que exigen IA real. Esta orden no añadió ninguna omisión: las 45 anteriores siguen igual, más las 8 nuevas de laboratorio. **No se cuentan como ejecutadas.**

**Laboratorios opt-in de esta orden** (aparte de la regresión), sin fallos:

| Laboratorio | Pruebas | Ejecución |
|---|---|---|
| `CierrePR17LabIT` | 6 + 3 | En dos ejecuciones; `base` se repite en la segunda |
| `CicloRealHistoricaIT`, `CicloRealC2DIT`, `CicloRealC2DEFIT`, `CicloRealC2DV4EFIT`, `CicloRealC2DEFLW02IT` | 5 | — |

**Baterías con políticas candidatas** (`PRUEBAS_CANDIDATAS_VERDES.md`):

| Política | Contabilizadas | Aprobadas | Fallidas | Omitidas |
|---|---|---|---|---|
| PLAN-HIST-1 | 225 | 223 | 0 | 2 |
| PLAN-C2D-0.1 | 225 | 223 | 0 | 2 |
| PLAN-C2D-EF-0.1 | 225 | 223 | 0 | 2 |

## 2. Las 15 comprobaciones obligatorias

| # | Comprobación | Resultado | Evidencia |
|---:|---|---|---|
| 1 | La política histórica permanece idéntica | **Sí:** la matriz de 960 da los mismos recuentos que la anterior (HIST 875, C2-D 951, EF 842 y C2-D-EF 902; en esta orden se compararon los recuentos, no cada perfil). Los cambios de esta orden están detrás de indicadores apagados. El ciclo histórico es igual fila a fila y la campaña, idéntica | `LONGITUDINAL_CIERRE.csv`, §4 |
| 2 | V4-C rechaza tiempos fuera del 115 % | **Sí** | `V4CValidadorRealTest` (19); 288 de 288 alternativas dentro del 115 % |
| 3 | C2-D no modifica dosis ni ejercicios por sí solo | **Sí:** 951 de 951 válidos de C2-D idénticos con y sin REP-SUST; C2-D solo decide la publicación | `REPARACION_SUSTITUCION_IMPACTO.csv`; matrices anteriores |
| 4 | EF no admite una falsa cobertura de patrón | **Sí:** 0 colocaciones incompatibles en los planes válidos de C2-D-EF | `FAMILIAS_EF_USO.csv` |
| 5 | No se publican PULL obligatorios sin tracción cuando EF la exige | **Sí** | `PoliticasP123IT` (C2-D-EF); ciclos L4 y L8; matriz |
| 6 | Los planes sin material suficiente reciben el motivo correcto | **Sí:** VEF «NO_FACTIBLE por catálogo o equipamiento insuficiente», y NOFACT-0.1 `PULL_NO_DISPONIBLE` | `NoFactibleApiEstructuradoIT` |
| 7 | No aparecen V15 nuevas | **Sí:** 0 en EF × LW | `MATRIZ_EF_LW02.csv` |
| 8 | LW-PREF-0.2 no cambia planes sin brecha | **Sí:** 0 en 4 combinaciones | Ídem |
| 9 | D8, ACORTAR y P-03 funcionan con C2-D activo | **Sí** | Baterías ×3 |
| 10 | La prueba del piso histórico pasa con todas las versiones, cada una con su expectativa | **Sí** | `PoliticasP123IT` ×3 |
| 11 | Concurrencia e idempotencia protegidas | **Sí:** `CicloConcurrenteIT` 9/9 ×3; ciclos idempotentes con ≤ 1 activo | Baterías; ciclos |
| 12 | La progresión no supera los límites | **Sí:** volumen semanal con paso máximo +0,5 % y acumulado +1,0 % | `LONGITUDINAL_CIERRE.csv` (`paso_vol_pct`, `acum_vol_pct`) |
| 13 | La reincorporación no restaura ejercicios automáticamente | **Sí:** 0 excluidos en planes; la reactivación es explícita | Ciclos |
| 14 | Versiones y trazas coherentes | **Sí:** política del plan = configurada en los 5 ciclos; `parametros.elegibilidad_funcional = EF-0.1` solo en EF; `opciones` solo con su indicador | `TRAZA_OPCIONES_*.csv` |
| 15 | La campaña REPLAY de 100 semanas es idéntica con todo desactivado | **Sí** (§4) | — |

## 3. Clasificación de resultados (sin cifra objetivo)

| Categoría | Cantidad | Detalle |
|---|---|---|
| Planes válidos (C2-D-EF) | 902 de 960 | Igual que en la orden anterior |
| Planes **recuperados** | 1 | R0808, solo con REP-SUST-0.1 (apagada; no se recomienda) |
| Planes **corregidos** | 42 + 31 parciales (LW-PREF-0.2 con EF) | En el laboratorio; apagado |
| Planes **rechazados correctamente** | 48 + 1 | Los 48 de peso corporal sin tracción, y R0808 sin REP-SUST |
| Planes **aún no factibles**, con alternativa | 48 | Con material declarado: 48 de 48 |
| Sin alternativa funcional | R0040, R0328, R0616 | La única sustitución cambia fuerza por cardio |
| **Regresiones** | **0** | — |
| Pendientes de revisión profesional | 36 elementos | 34 planes y 2 fichas |

## 4. Campaña de 100 semanas (REPLAY, todo desactivado)

| Comprobación | Resultado |
|---|---|
| Ejecución | `CampanaP123Cohorte02IT` (`-Dfitsense.campana.p123=true`), política por defecto: **2 contabilizadas, 1 ejecutada sin fallos, 1 omitida** (la de siempre) |
| Evidencia | `planes.csv`, `perfiles.csv`, `generacion.csv`, `historial.csv`, `metricas.csv`, `regeneraciones.csv` y `v32.csv` **sin cambios**. `exclusiones_dolor.csv` cambia solo en las marcas de tiempo (comparado con ellas normalizadas: idéntico). Evidencia revertida |
| Replicate | 0 llamadas |

## 5. Correcciones de esta orden sobre trabajo anterior

| Elemento | Problema | Corrección |
|---|---|---|
| `correccion_elegibilidad_funcional/LONGITUDINAL_C2D_CORREGIDO.csv` | La columna `motivo_sin_plan` quedó **vacía en las 240 filas**: el ciclo marca las semanas sin plan como `SIN_PLAN` y el filtro no lo contemplaba | `LONGITUDINAL_CIERRE.csv` la sustituye con los motivos correctos. El archivo anterior se conserva sin cambios |
| `PoliticasP123IT` (primera versión parametrizada) | `Set.of` con patrones repetidos: 12 errores con C2-D-EF | `HashSet`; 0 errores |
| Prototipo de REP-SUST-0.1 | Validaba sin V25 y aceptaba cambiar fuerza por cardio | Validador completo y conservación de la clase |
