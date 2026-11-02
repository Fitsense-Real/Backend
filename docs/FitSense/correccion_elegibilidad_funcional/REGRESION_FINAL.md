# Regresión final

**Entorno:**
- `mvn -o verify -Pintegration` en `feature/c2d-v4c-versionado`.
- PostgreSQL 16 efímero (Testcontainers), Flyway hasta **V45**.
- `fitsense.ai.enabled=false` con dobles de IA: **0 llamadas a Replicate**.
- Recuentos de los informes de surefire y failsafe.

## 1. Recuentos

| Batería | Orden anterior | Esta orden | Diferencia |
|---|---|---|---|
| Unitarias | 983, 0 fallos | **992: 0 fallos, 0 errores, 0 omitidas** | +9: `ElegibilidadFuncionalTest` 8, `ContratoV4CIATest.elegibilidadFuncionalSoloConEF` 1 |
| Integración (contabilizadas) | 466, 39 omitidas | **473: 0 fallos, 0 errores, 45 omitidas** | +1 `MigracionV45IT` (ejecutada) y +6 laboratorios opt-in omitidos: `CorreccionFuncionalMatrizIT` ×4, `CicloRealC2DEFIT`, `CicloRealC2DV4EFIT` |
| **Integración ejecutadas** | 427 | **428** | +1 `MigracionV45IT` |

**Las omitidas no se cuentan como ejecutadas.**

**Laboratorios opt-in (aparte), todos sin fallos:**

| Laboratorio | Pruebas |
|---|---|
| `CorreccionFuncionalMatrizIT` | 4 |
| `CicloRealHistoricaIT` | 1 |
| `CicloRealC2DIT` | 1 |
| `CicloRealC2DEFIT` | 1 |
| `CicloRealC2DV4EFIT` | 1 |
| **Total** | **8/8** |

**Baterías con la política candidata activa** (`PRUEBAS_IA_D8_ACORTAR_C2D.md`):
- 225 pruebas con `PLAN-C2D-0.1` y 225 con `PLAN-C2D-EF-0.1`.
- En cada una: 222 pasan, **1 falla (esperada: semántica histórica del piso de V20)** y 2 se omiten.

## 2. Incidencias durante el trabajo

| Incidencia | Causa | Arreglo |
|---|---|---|
| `MigracionV44IT` habría ejecutado 2 migraciones con V45 | Migraba hasta la última versión | Destino fijado en `44`. `MigracionV45IT` audita V45 sobre V44 |
| Primera matriz EF: 9 planes de gimnasio o mancuernas inválidos por VEF | Sesiones LOSE_WEIGHT de 30 min solo de cardio en una división PUSH/PULL/LEGS, más un constructor que no elegía el ancla | VEF exige ancla solo con trabajo resistido propio, y el selector elige primero el ancla si existe. Gimnasio y mancuernas: 0 pérdidas |
| Segunda matriz: 291 planes de C2-D ya funcionalmente correctos cambiaban de composición | Al quitar candidatos, cambian los sorteos de la semilla | El generador conserva el plan de siempre si cumple VEF. **0 planes correctos alterados** |
| Recuento LW de la orden anterior | «upper legs» se contaba como dos grupos | Recalculado (`LOSE_WEIGHT_COBERTURA_REVISADA.md` §3) |

## 3. Comparación de los 960 perfiles (5 configuraciones)

| # | Configuración | Válidos | Fuente |
|---|---|---|---|
| 1 | HIST, catálogo original | **875** (idéntica a la matriz anterior, perfil a perfil) | `MATRIZ_COMPARACION_FUNCIONAL.csv` |
| 2 | C2-D, catálogo original | **951** (idéntica a la anterior) | Ídem |
| 3 | C2-D con corrección funcional (`PLAN-C2D-EF-0.1`) | **902** | Ídem |
| 4 | C2-D + V4-C combinada: sin EF (`PLAN-C2D-V4-0.1`) / con EF (`PLAN-C2D-V4-EF-0.1`) | **951 / 902** | Ídem |
| 5 | Mejora LW (`LW-PREF-0.2`, solo laboratorio) | 59 de los 148 LW con grupos a cero mejoran; 0 empeoran | `LOSE_WEIGHT_PREFERENCIA_LW02.csv` |

**Desgloses** (válidos HIST / C2D / EF / C2D-EF):

| Dimensión | Valor | HIST | C2D | EF | C2D-EF |
|---|---|---|---|---|---|
| Objetivo | GAIN_MUSCLE | 172 | 240 | 168 | 224 |
| | GENERAL_FITNESS | 237 | 237 | 220 | 220 |
| | INCREASE_STRENGTH | 236 | 240 | 224 | 224 |
| | LOSE_WEIGHT | 230 | 234 | 230 | 234 |
| Nivel | Principiante | 293 | 317 | 292 | 316 |
| | Intermedio | 291 | 317 | 275 | 293 |
| | Avanzado | 291 | 317 | 275 | 293 |
| Equipamiento | Gimnasio | 357 | 381 | 357 | 381 |
| | Mancuernas y bandas | 265 | 285 | 265 | 285 |
| | Solo peso corporal | 253 | 285 | 220 | **236** |
| Días | 1 | 126 | 135 | 126 | 135 |
| | 2 | 135 | 144 | 135 | 144 |
| | 3 | 225 | 240 | 225 | 240 |
| | 4 | 133 | 144 | 133 | 144 |
| | 6 | 129 | 144 | 112 | **119** |
| | 7 | 127 | 144 | 111 | **120** |
| Minutos | 30 | 240 | 240 | 228 | 228 |
| | 45 | 230 | 234 | 222 | 222 |
| | 60 | 236 | 240 | 228 | 228 |
| | 75 | 169 | 237 | 164 | 224 |

**Desglose por patrón, capacidad y dosis.**
- Columnas `patrones` y `series_sem` de la matriz.
- En los válidos de C2-D-EF caen los patrones no pertinentes: TRUNK_ROTATION 496 → 167 colocaciones, ELBOW_FLEXION 331 → 145.
- La media de series semanales pasa de 57,3 a 54,7.
- **Las dosis 4D no cambian.**

**Motivos de rechazo:** `INTERACCION_C2D_CATALOGO.md` §1.

## 4. Campaña de 100 semanas (REPLAY, todo desactivado)

| Comprobación | Resultado |
|---|---|
| Ejecución | `CampanaP123Cohorte02IT` con `-Dfitsense.campana.p123=true` y la política por defecto (`PLAN-HIST-1`): **2 contabilizadas, 1 ejecutada sin fallos, 1 omitida** (la misma de siempre) |
| `planes.csv`, `perfiles.csv`, `generacion.csv`, `historial.csv`, `metricas.csv`, `regeneraciones.csv`, `v32.csv` | **Sin cambios** |
| `exclusiones_dolor.csv` | 2 líneas cambiadas, **idénticas al quitar las marcas de tiempo** (`recorded_at` y `reactivated_at`). Evidencia revertida |
| Replicate | 0 llamadas |

## 5. Neon, Railway y Replicate

| Servicio | Uso |
|---|---|
| **Neon y Railway** | **No se han tocado.** Ninguna conexión. V44 y V45, solo en PostgreSQL efímero |
| **Replicate** | **0 llamadas.** Todas las pruebas con `fitsense.ai.enabled=false` y dobles. `A01AdaptacionRealIT`, que usa IA real, está excluida |
