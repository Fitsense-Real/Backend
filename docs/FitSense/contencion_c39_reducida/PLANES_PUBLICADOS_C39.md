# Planes publicados con la contención reducida a 8

No se modificó ningún plan real. Todo lo que sigue procede de copias controladas en una base PostgreSQL efímera (`p2d/Plan78ContencionReducidaIT`, `p2d/ContencionTextoBanderaIT`, grupo F-G) y del código.

## 1. Qué sesiones quedan bloqueadas

Una sesión **abierta** (SCHEDULED o IN_PROGRESS) de un plan ACTIVE queda bloqueada si contiene uno de los 8 ejercicios y el participante declara la restricción correspondiente:

| Restricción declarada | Columna del perfil | Ejercicios |
|---|---|---|
| Sin colgarse | `user_profiles.allow_hanging_exercises = false` | 1153, 1301, 1303, 1304, 1306 |
| Sin brazos sobre la cabeza | `user_profiles.allow_overhead_exercises = false` | 131, 400, 448 |

- Estas dos restricciones solo vienen de lo que declara el perfil. La edad no las impone (`SafetyProfile.forAgeLevelAndDeclared`).
- La causa del bloqueo es `INC-CAT-C39` (`SafetyConflictPolicy`). Se ve en la lectura del plan: `safetyBlocked` en la sesión y `safetyBlockReason` en el ejercicio.
- Ejecución rechaza iniciar la sesión con `SESION_BLOQUEADA_POR_SEGURIDAD`.
- En una sesión ya en curso, el ejercicio bloqueado solo puede registrarse como omitido.

**Frente a la PR #55 original**, dejan de bloquearse las sesiones cuyo único conflicto era uno de los 18 ejercicios liberados (por ejemplo 1152, 1093 o 316) o el suelo de 1303 y 1304. Siguen sin bloquearse las sesiones terminadas y las REPLACED, que son historia.

## 2. Cómo identificarlas (solo lectura, para ejecutar en producción)

```sql
-- Sesiones abiertas de planes activos que la contención reducida (INC-CAT-C39, 8 ids) bloquearía.
SELECT p.user_id, p.plan_id, p.week_start_date, pw.planned_workout_id, pw.scheduled_date, pw.status,
       pwe.exercise_id, x.name_es,
       CASE WHEN pwe.exercise_id IN (1153,1301,1303,1304,1306) THEN 'colgarse' ELSE 'brazos sobre la cabeza' END AS restriccion,
       EXISTS (SELECT 1 FROM planned_workouts o WHERE o.plan_id = p.plan_id
               AND o.status IN ('COMPLETED','PARTIAL','SKIPPED')) AS semana_empezada
FROM weekly_training_plans p
JOIN user_profiles up            ON up.user_id = p.user_id
JOIN planned_workouts pw         ON pw.plan_id = p.plan_id AND pw.status IN ('SCHEDULED','IN_PROGRESS')
JOIN planned_workout_exercises pwe ON pwe.planned_workout_id = pw.planned_workout_id
JOIN exercises x                 ON x.exercise_id = pwe.exercise_id
WHERE p.status = 'ACTIVE'
  AND (   (pwe.exercise_id IN (1153,1301,1303,1304,1306) AND up.allow_hanging_exercises  = false)
       OR (pwe.exercise_id IN (131,400,448)              AND up.allow_overhead_exercises = false))
ORDER BY pw.scheduled_date, p.user_id;
```

- `semana_empezada = true` indica que la reparación no versionará esa semana (§3).
- La consulta está probada: `Plan78ContencionReducidaIT` la ejecuta sobre la copia del plan 78 y devuelve exactamente la sesión del jueves con el 1303.
- La consulta no cubre otras causas de bloqueo (INC-4C-03, INC-CAT-1374, banderas). Para esas, la lectura del plan por la API es la fuente completa.

## 3. Plan 78 (usuario 39)

Copia controlada del plan 78 reproducido por la auditoría del caso 39. Coincide con el real en fechas, enfoques, duraciones, estructura, 1303 y 910; la identidad ejercicio a ejercicio sigue pendiente de una consulta sobre datos reales. Perfil real: sin colgarse y sin brazos sobre la cabeza.

| Sesión | Ejercicios relevantes | PR #55 original (26) | Reducida (8) |
|---|---|---|---|
| Lunes | 1093 (liberado; el perfil no restringe el suelo) | no bloquea | no bloquea |
| Jueves | **1303** (apoyo suspendido), 916 | **bloqueada** (1303) | **bloqueada** (1303) |
| Viernes | 1152 (liberado), 910 (nunca contenido) | **bloqueada** (1152) | no bloquea |
| Domingo | — | no bloquea | no bloquea |

La columna «26» procede de la misma prueba ejecutada sobre la PR #55 original con la pila (`evidencia/plan78_26.csv`). La columna «8» procede de `evidencia/plan78_8.csv`.

### Reparación

| Escenario | Resultado | Comprobado en |
|---|---|---|
| Semana sin empezar, plan 78 tal cual | **NO_VALIDA**: no se publica nada y el jueves sigue bloqueado | `semanaSinEmpezar` |
| Semana sin empezar, sin el 916 | **REPARADA**: versión 2 idéntica salvo el 1303, sustituido por un ejercicio seguro, sin más series, repeticiones ni duración (V8C); versión 1 intacta y REPLACED | `reparacionLocalizada` |
| Semana empezada (una sesión COMPLETED) | **SEMANA_EMPEZADA**: no se versiona; el jueves sigue bloqueado y al vencer pasa a SKIPPED sin motivo | `semanaEmpezada` |
| Sesión en curso | **SESION_EN_CURSO**: no se sustituye nada en silencio | `ContencionTextoBanderaIT.g_sesionEnCurso` |

**Por qué el plan 78 tal cual no se repara.** La reparación revalida la semana entera contra el catálogo actual. La misma sesión del jueves lleva el **916** (curl de piernas invertido en polea), que V48 subió de dificultad 1 a 3, así que ya no es elegible para un principiante. V1 rechaza la versión nueva: «el ejercicio 916 no está en available_exercises».

La dificultad no es causa de seguridad (`SafetyConflictPolicy`). Por eso el 916, por sí solo, no bloquea ni señala ninguna sesión publicada; solo impide repararla. Esto no lo introduce la reducción: con la contención de 26 ocurre lo mismo en la sesión del jueves.

## 4. Limitaciones actuales (documentadas, no corregidas aquí)

1. **Reparación bloqueada por cambios del catálogo ajenos a la seguridad.**
   - V48 (dificultad) y V50 (equipamiento) pueden dejar fuera del conjunto elegible un ejercicio publicado.
   - Si ese ejercicio está en una semana que necesita reparación por seguridad, la reparación da NO_VALIDA y la sesión insegura sigue bloqueada sin alternativa.
   - Afecta al plan 78.
   - La consulta de §2, cruzada con los ids de V48 (464, 821, 862, 868, 874, 880, 915, 916, 1108, 1191, 1192, 1201) y de V50 (equipamiento), mide la exposición.
2. **Semana empezada: no hay reparación por sesión.**
   - Con una sesión terminada no se versiona la semana (diseño de `PublishedPlanSafetyRepairService`: la adaptación, V31 y V32 leen solo la última versión).
   - La sesión bloqueada se queda bloqueada hasta que vence.
3. **Adherencia.**
   - Al vencer, la tarea de vencimiento (`ExpireOverdueWorkoutsCommand`) marca la sesión bloqueada como **SKIPPED sin motivo**.
   - No existe un `SkipReason` de seguridad, así que cuenta en la adherencia como no hecha aunque el participante no podía hacerla.
   - Puede afectar a la adaptación siguiente: umbral de adherencia y motivos.
4. **IN_PROGRESS pegado (H6 de la auditoría del caso 39).**
   - Una sesión abandonada en IN_PROGRESS impide la reparación (SESION_EN_CURSO) hasta que la libera la tarea de sesiones obsoletas.
   - Además, hoy se puede iniciar una sesión de una fecha futura.
5. **Sin aviso proactivo.**
   - El bloqueo se ve al leer el plan o al intentar iniciar la sesión.
   - No hay notificación al participante ni al equipo, ni reparación automática: la reparación es una llamada explícita (`repairPublishedPlanForSafety`).

Ninguna de estas limitaciones se corrige en esta PR: la orden pide documentarlas. Las propuestas están en `RIESGOS_Y_ORDEN_DE_FUSION.md`.
