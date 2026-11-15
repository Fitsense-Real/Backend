# Protección por dolor en sesiones ya publicadas (P-DOLOR-PUB)

## 1. El defecto que corrige

P-02 registra un ejercicio con dolor como exclusión ACTIVA (`user_safety_exclusions`). Antes de esta versión, esa exclusión **solo** se aplicaba a las semanas **nuevas**.

Si el ejercicio se repetía en otra sesión **ya publicada y abierta** de la misma semana, seguía visible y se podía registrar con trabajo. También pasaba cuando la adaptación quedaba NO_FACTIBLE y el plan seguía vigente.

Medición en los 40 escenarios HALL-GM-01, con dolor el lunes:

- Con la protección **apagada**, **7 de 40** participantes registran trabajo en un ejercicio que les dolió, en una sesión posterior de la misma semana. Son 9714, 9725 y 9730 (1 registro cada uno); 9703 y 9721 (1 cada uno, con NO_FACTIBLE); 9716 y 9719 (2 cada uno, con NO_FACTIBLE).
- Con la protección **encendida**: **0 de 40**.

Fuente: `evidencia/hall_gm_01/*.csv`, columna `trabajo_tras_dolor`.

## 2. Qué hace

`PublishedPlanSafetyService` añade las exclusiones activas por dolor (`PainMemoryService.excluidosActivos`) a los conflictos de seguridad de las sesiones que admiten registros, con la causa `dolor_registrado`. Esas sesiones son SCHEDULED, IN_PROGRESS y, para el registro a posteriori, SKIPPED.

| Efecto | Detalle |
|---|---|
| El ejercicio que dolió | Solo se puede registrar como **omitido** (`rechazarSiInseguro`). Un registro con series, repeticiones o segundos se rechaza con `dolor_registrado` |
| La sesión | **No se bloquea entera** por el dolor (`PlannedWorkoutView.blockedForSafety` ignora el conflicto que es solo de dolor). Se puede empezar y los demás ejercicios se registran con normalidad |
| La API del plan | La sesión no aparece bloqueada (`safetyBlocked = false`) y lleva el aviso `AVISO_DOLOR`: «Esta sesión incluye un ejercicio que reportaste con dolor. Omítelo: no lo hagas hasta que tu plan se ajuste.» |
| Otras restricciones | Se **suman**, no se sustituyen. Si el ejercicio ya chocaba con otra causa, la causa queda `otra+dolor_registrado` y la sesión **sigue bloqueada** por esa otra causa |
| El plan publicado | No cambia: sin sustitutos, sin dosis nuevas y sin sesiones nuevas |
| Sesiones terminadas | COMPLETED, PARTIAL y REPLACED son historia: no se marcan ni admiten registros |
| Interruptor | `fitsense.seguridad.dolor-en-planes-publicados` (encendido en la candidata; `false` vuelve al comportamiento anterior) |

## 3. Los 10 casos obligatorios

Todas las pruebas se ejecutan sobre el commit candidato. Resultado: ver `REGRESION_FINAL.md`.

| # | Caso | Prueba | Qué se comprueba |
|---|---|---|---|
| 1 | Ejercicio repetido en otra sesión | `RegeneracionPorColocacionRev2IT#dolorMarcadoEnSesionesAbiertas` | Las prescripciones de las sesiones posteriores con el ejercicio del lunes quedan marcadas con `dolor_registrado` |
| 2 | Ejercicio que no se repite | ídem, y `PublishedPlanSafetyServiceDolorTest#seSumaALaOtraCausa` | Se marcan **exactamente** las prescripciones cuyo ejercicio dolió, y ninguna otra (comparación de mapas completos) |
| 3 | Sesión con varios ejercicios, uno afectado | ídem (IT) | La sesión tiene más de un ejercicio y no se bloquea entera; los demás se registran con trabajo; el afectado, solo omitido |
| 4 | Sesión completada | ídem (IT, tras terminar la sesión en curso) y `PublishedPlanSafetyServiceDolorTest#terminadas` (COMPLETED, PARTIAL, REPLACED) | Terminada, no se marca; lo registrado (omitido con su causa) queda tal cual |
| 5 | Sesión en curso | ídem (IT): la sesión se **empieza antes** de registrar el dolor | Al llegar el dolor, el ejercicio de la sesión en curso ya no admite trabajo. La prueba unitaria `enCursoYOmitida` comprueba que en curso se marca igual que programada |
| 6 | Sesión futura | ídem (IT): otra sesión todavía programada | Marcada, no bloqueada, con el aviso en la API; se puede empezar; el ejercicio con dolor solo admite omitido |
| 7 | Adaptación con éxito | `RegeneracionPorColocacionRev2IT#dolorTrasMantenimiento` y `#sustitutoQueExisteReducidoOtroDia` (`comprobar`) | La semana publicada no trae lo que dolió (`reaparecen = []`), ninguna colocación sube, y en la semana medida no se hizo trabajo en lo que dolió (`trabajoTrasDolor = 0`) |
| 8 | Adaptación NO_FACTIBLE | `#halGm01ConContencion` (9717) y `#sustitutoDeMantenimientoSinMasTrabajo` (9703, sin contención) | No se publica nada, la exclusión sigue activa y en la semana medida, que sigue vigente, no se hizo trabajo en lo que dolió |
| 9 | Ejercicio ya bloqueado por otra causa | `PublishedPlanSafetyServiceDolorTest#seSumaALaOtraCausa` y `#apagada` | Causa `requires_floor+dolor_registrado` y sesión bloqueada; con la protección apagada, exactamente las causas de antes |
| 10 | Reintentos y operaciones repetidas | IT: el registro con trabajo se reintenta y se rechaza igual; el omitido se registra dos veces y queda **una** fila; leer varias veces no añade ni quita exclusiones. Unitaria `#idempotente` | Sin duplicados (también `uq_wse_session_exercise`); la lectura no escribe |

## 4. Lo que la protección no hace

- **No desactiva otras restricciones.** Las causas del perfil siguen calculándose igual (`SafetyConflictChecker.motivos`). La de dolor se suma.
- **No omite la sesión entera.** Solo el ejercicio afectado queda restringido a «omitido». El contrato no pide omitir la sesión, y la prueba comprueba que se puede empezar.
- **No cambia la fórmula de adherencia.** El ejercicio omitido cuenta como hasta ahora en el porcentaje de la sesión (regla 17.2). No hay ningún cambio en `WorkoutSession`, en la métrica semanal ni en el decisor de adaptación.

## 5. ¿Basta `OTHER` como motivo?

Cuando la persona omite el ejercicio marcado, elige ella el motivo: la app envía el que se seleccione, sea PAIN_OR_DISCOMFORT, OTHER u otro. La prueba usa OTHER (sesión en curso) y PAIN_OR_DISCOMFORT (sesión futura).

**Hecho:** el motivo de la fila, por sí solo, no distingue una omisión voluntaria de una que impuso el sistema.

**Hecho:** la trazabilidad sí es completa por reconstrucción. Mientras la exclusión está ACTIVA, el sistema **solo** admite registrar ese ejercicio como omitido. Por tanto, **toda** omisión de un ejercicio con exclusión activa registrada antes de la omisión la impuso el sistema, sea cual sea el motivo elegido.

La prueba lo comprueba con esta consulta, que devuelve exactamente la omisión impuesta:

```sql
SELECT wse.planned_exercise_id
  FROM workout_session_exercises wse
  JOIN workout_sessions ws ON ws.session_id = wse.session_id
  JOIN planned_workout_exercises pwe ON pwe.planned_exercise_id = wse.planned_exercise_id
  JOIN user_safety_exclusions x ON x.user_id = ws.user_id AND x.exercise_id = pwe.exercise_id
                                AND x.status = 'ACTIVA' AND x.recorded_at <= wse.created_at
 WHERE ws.user_id = ? AND wse.status = 'SKIPPED';
```

Si la exclusión se reactivara después, habría que usar también `reactivated_at`.

**Conclusión:** `OTHER` mantiene una trazabilidad **suficiente para la evaluación**, siempre que el análisis aplique esta reconstrucción.

Una causa específica (por ejemplo, `SISTEMA_SEGURIDAD`) daría trazabilidad **directa**, pero:

- cambia el enum `SkipReason`, compartido por cuatro tablas con el mismo CHECK;
- cambia el insumo del modificador 18.3, la tabla de decisiones de adaptación y los textos.

Es un cambio metodológico y **no se implementa sin aprobación**. Queda como recomendación, junto con la constancia de V54 para las sesiones bloqueadas que vencen.

**Para la tesis:** las omisiones impuestas por el sistema cuentan en la adherencia como cualquier omisión. Si se quiere excluirlas del indicador, hay que hacerlo en el análisis con la consulta anterior y declararlo.

## 6. HALL-GM-01 con las protecciones apagadas y encendidas

Ver `VALIDACION_HALL_GM_01.md`. Con las dos protecciones encendidas, ningún participante registra trabajo en lo que dolió (0/40) y ninguna semana publicada aumenta trabajo.
