# Regresión completa

**Entorno**
- `mvn -o verify -Pintegration` sobre la rama `feature/c2d-v4c-versionado`.
- PostgreSQL 16 efímero (Testcontainers) y Flyway hasta V44.
- `fitsense.ai.enabled=false` y dobles de IA: **0 llamadas a Replicate**.
- Recuentos sacados de los informes de surefire y failsafe.

## 1. Recuentos

| Batería | `generation` (referencia) | Esta rama | Diferencia |
|---|---|---|---|
| Unitarias | 939, 0 fallos | **983, 0 fallos, 0 errores, 0 omitidas** | +44 nuevas: `PoliticaPlanificacionTest` 21, `V4CValidadorRealTest` 19, `ContratoV4CIATest` 2, `DuracionYAerobicaAssemblerTest` 2 |
| Integración (contabilizadas) | 458, 32 omitidas | **466, 39 omitidas, 0 fallos, 0 errores** | +1 `MigracionV44IT` (ejecutada); +7 opt-in omitidas: `ImplementacionC2DV4CMatrizIT` ×4 y `CicloReal{Historica,C2D,C2DV4}IT` ×3 |
| **Integración ejecutadas** | 426 | **427** | +1: `MigracionV44IT` |
| Laboratorios opt-in (aparte) | — | **7/7 sin fallos**: matriz 4, ciclo histórico 1, ciclo C2-D 1, ciclo combinado 1 | — |

**Las omitidas no se cuentan como ejecutadas.**

## 2. Incidencias durante la regresión

| Incidencia | Causa | Arreglo |
|---|---|---|
| `PublishAdaptedTest` (6 errores, primera unitaria) | Una prueba publica con un contexto nulo y la traza lo leía | Si no hay contexto (solo en pruebas hechas a mano), no se registra la política. En producción, `prepare` siempre la fija |
| `MigracionV43IT.historicoV42MasV43` (1 fallo, primera integración) | Migraba hasta la última versión y esperaba solo V43; con V44 se ejecutaban 2 | Se fija el destino en `43`, que es lo que audita. **`MigracionV44IT`** audita V44 sobre un histórico V43 |

## 3. Diferencias funcionales frente a `generation` (con la política histórica)

| Ámbito | Diferencia |
|---|---|
| Generación y validación | **Ninguna:** los 960 perfiles dan resultado, segundos e identidad iguales que en la matriz anterior |
| Contrato de la IA | **Ninguno:** el JSON y el prompt históricos son idénticos (`ContratoV4CIATest`) |
| API | **Ninguna** con los indicadores apagados (campos nuevos con `NON_NULL`) |
| Persistencia | **Sí, solo de registro:** los planes nuevos guardan `planning_policy = 'PLAN-HIST-1'` y una traza JSON (V44). Los antiguos quedan en `NULL` |
| Campaña de 100 semanas (REPLAY) | **Idéntica:** 1 ejecutada y 1 omitida, 0 fallos. Las 2 líneas cambiadas de `exclusiones_dolor.csv` coinciden si se ignoran `recorded_at` y `reactivated_at` (marcas de tiempo de la ejecución). Evidencia revertida |

**Evidencia de la campaña:**

| Comparado | Resultado |
|---|---|
| `planes.csv`, `perfiles.csv`, `generacion.csv`, `historial.csv`, `metricas.csv`, `regeneraciones.csv`, `v32.csv` | Sin cambios |
| `exclusiones_dolor.csv` | Solo las marcas de tiempo |

La campaña no exporta la nueva columna de política. Que todos los planes nuevos queden como `PLAN-HIST-1` se comprueba en el ciclo real.

## 4. Pruebas con las políticas activas (solo en laboratorio)

| Escenario | Prueba |
|---|---|
| Generación inicial y validación real con las 4 versiones | `ImplementacionC2DV4CMatrizIT.matriz` (960 perfiles) |
| Borradores de IA (V4-C) | `V4CValidadorRealTest` (validador real; el 142 % y los bordes) |
| Publicación, P-02, P-03, ACORTAR, fatiga, baja adherencia, reincorporación, idempotencia y reintento del mismo ciclo | `CicloRealC2DIT`, `CicloRealC2DV4IT` (y `CicloRealHistoricaIT` como referencia) |
| Los 6 perfiles de V15 | En la matriz: siguen rechazados con C2-D |
| Perfiles congelados | No se modifican (las pruebas no tocan el perfil ni M) |
| D8 y C-2 con IA simulada; concurrencia | Baterías existentes en la regresión, con la política histórica. **No se ejercitaron con C2-D activo** |
