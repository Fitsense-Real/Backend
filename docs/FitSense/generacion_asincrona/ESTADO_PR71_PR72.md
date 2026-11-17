# Estado de las PR #71 y #72

Fecha: 16/11/2026. No se ha hecho ningún merge ni ningún despliegue.

## 1. Ramas y dependencias

| | Rama | Base | Cabeza | Estado |
|---|---|---|---|---|
| `generation` | `generation` | — | `2578719` (merge de #70) | Sin cambios desde que se abrió #71 |
| **#71** | `claude/async-first-plan-generation` | `generation` (`2578719`) | `12a01c6` | Abierta, no es borrador, `mergeable_state = clean`. CI `verify` en verde el 15/11 (19,5 min). |
| **#72** | `fix/async-generacion-resiliente` | `claude/async-first-plan-generation` (`12a01c6`) | La última cabeza de la rama: los commits de V59, la regresión y la validación final | Borrador, sin conflictos. El CI de la cabeza anterior (`93549d5`) pasó el 15/11 (26 min). |

#72 depende de #71: se construye encima de sus 4 commits y no los reescribe. Ninguna de las dos se aparta de
`generation`, así que no hay conflictos.

## 2. Cambios

### #71 (V58): la generación inicial como operación asíncrona

- Operación persistente (`plan_generation_operations`), worker, controlador y recurso REST.
- Clasificación de fallos y apagado ordenado.
- Pruebas: `GeneracionAsincronaIT` y las unitarias.

### #72 (V59): corrige los defectos de #71 sin cambiar el contrato REST

| Componente | Cambio |
|---|---|
| `V59__generacion_resiliente_lease_y_predicciones.sql` | Lease (`lease_owner`, `lease_expires_at`, `first_started_at`, `ck_pgo_lease`) y la tabla `plan_generation_predictions` |
| `PlanGenerationOperationService` | Lease con propietario, fencing, barrido por lease vencido o nulo, tiempo máximo desde la primera ejecución, recuento de predicciones |
| `PlanGenerationWorker` | Hilos propios para el ciclo y el latido; barrido cada `recover-ms` |
| `PipelineFirstPlanGenerationRunner` | Propiedad y publicación en una sola transacción |
| `DiarioDePredicciones`, `JdbcDiarioDePredicciones` | Libro de predicciones |
| `ReplicateTrainingPlanGenerator` | Camino con libro, solo dentro de una operación asíncrona: POST sin `Prefer: wait`, reanudación por id, `NO_CONFIRMADA`, adopción y carrera. El camino síncrono no cambia. |
| `PoliticaTiemposGeneracion` | Comprobación al arrancar |
| `PlanGenerationOperation` | Columnas de lease |
| `PlanGenerationPipeline` | Solo `AI_ATTEMPTS` (de privada a pública) |
| `application.properties` | Las propiedades `fitsense.plan-generation.*` |
| Pruebas | `DiarioDePrediccionesReplicateTest`, `GeneracionAsincronaResilienciaIT`, `ReanudacionConReplicateSimuladoIT`, `MigracionV58aV59IT`, la adaptación de `GeneracionAsincronaIT` y V59 en `EsquemaVigenteSobreCatalogoAnterior` |
| Documentación | Esta carpeta y `docs/generacion-inicial-asincrona.md` |

## 3. Migraciones

V58 viene de #71 y no se ha modificado. V59 viene de #72 y es aditiva. Las dos se prueban desde una base vacía y
la actualización V58 → V59 con datos (`MIGRACIONES_V58_V59.md`). No hay ninguna V60: las correcciones de esta
orden son de código.

## 4. Problemas que impiden la integración

| Problema | ¿Bloquea? | Detalle |
|---|---|---|
| #71 sola tiene los defectos conocidos: 600 s, latido sin hilo, predicciones repetidas al reanudar | **Sí, si se despliega sin #72** | No debe desplegarse una versión intermedia (ver el orden) |
| `ReparacionLinajeIT` y `ReparacionLinajeRev2IT` fallan desde el 16/11 | **El CI de cualquier rama queda en rojo** mientras dure, también `generation` | Fallo ajeno a #71 y #72: se reproduce en `generation` sin cambios. Depende de la fecha (P-P2/H6, progresión). Requiere una investigación aparte (`RIESGOS_RESIDUALES.md`, R16). |
| La prueba con Replicate real está pendiente | No para revisar; sí para producción | `DECISION_PREPARACION_IA_REAL.md` |

## 5. Orden de integración propuesto

1. Revisar #71 y #72 juntas.
2. Fusionar **#71** en `generation` y, **sin desplegar**, fusionar **#72**. Al fusionar #71, GitHub cambia
   automáticamente la base de #72 a `generation` (la rama de #71 deja de existir si se borra). Si no la cambia,
   hay que cambiarla a mano: el diff de #72 es el mismo.
   - Alternativa equivalente: fusionar #72 en la rama de #71 y después #71 (ya corregida) en `generation`.
3. Desplegar solo con las dos dentro.

Ninguna PR del Mobile necesita cambios (`COMPATIBILIDAD_FLUTTER.md`).
