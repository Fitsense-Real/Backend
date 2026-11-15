# Impacto del cierre en los planes ya publicados

No se ha modificado ningún plan real ni se ha consultado la base de producción. Este análisis usa:
- el código: `SafetyConflictPolicy`, `PublishedPlanSafetyService`, la reparación y el vencimiento;
- la copia del plan 78 en `Plan78ContencionReducidaIT`;
- una consulta de solo lectura para identificar los planes afectados cuando se despliegue.

## 1. Qué cambia para un plan publicado

Las migraciones **no tocan** `planned_workout_exercises`. El efecto llega al leer el plan: cada ejercicio publicado se compara con la seguridad **actual** del perfil y con el catálogo **actual**.

| Cambio del cierre | Efecto en un plan publicado |
|---|---|
| Bandera añadida (OH 14, AX 11, INV 4, FL 2, KN 1) | Si el perfil declara esa restricción, el ejercicio pasa a ser causa de seguridad: la sesión queda **bloqueada** y no se puede iniciar. Es lo correcto: el plan pedía algo que el perfil no admite |
| V53 (apoyo suspendido, 15 ejercicios; PR B) | **Ninguno mientras el interruptor siga apagado** (por defecto): los validadores reciben el hecho en `false`. Si se aprueba y se enciende, con «no colgarse» aparece la causa `apoyo_suspendido`, y en el plan 78 el 1303 pasaría a `INC-CAT-C39+apoyo_suspendido`: la misma sesión, bloqueada con dos motivos |
| 1083 con HI (orden de desbloqueo) | Para quien no admite impacto (60 años o más, principiante, o restricción declarada), una sesión publicada con 1083 queda **bloqueada** con la causa `alto_impacto`. Es lo correcto: antes quedaba elegible por la contradicción R03 |
| 775 exige banco (orden de desbloqueo) | El equipo **no es causa de seguridad**: no bloquea ninguna sesión publicada. Pero la reparación y la regeneración revalidan la semana contra el conjunto elegible actual, y si el perfil no declara banco, el 775 ya no está en él |
| Dosis del sustituto (HALL-PROG-01) | La reparación de una sesión bloqueada pone al sustituto una dosis de capacidad válida, nunca con más trabajo que el quitado, en vez de heredar una dosis progresada. Afecta solo a reparaciones nuevas; lo ya publicado no cambia |
| Bandera quitada (29: OH 12, KN 5, HI 5, AX 3, INV 2, FL 2) | Puede **desbloquear** una sesión que solo estaba bloqueada por esa bandera |
| Dificultad subida (63) | **No bloquea** ninguna sesión: la dificultad no es causa de seguridad. Pero la **reparación** revalida la semana entera y rechaza el plan si contiene un ejercicio por encima del nivel |
| Textos, nombre, patrón, prescripción | Se muestra el texto nuevo. No cambia la dosis publicada |

## 2. Plan 78 (usuario 39)

Comprobado en `Plan78ContencionReducidaIT` con el catálogo final:
- **Semana sin empezar:** solo se bloquea el jueves, por el 1303 (`INC-CAT-C39`; con V53 encendido, `INC-CAT-C39+apoyo_suspendido`). Las sesiones del viernes (1152 y 910) y del lunes se pueden iniciar.
- **Reparación:** sigue sin publicar. Además del 916 (dificultad 3 desde V48), **el 910 (remo invertido) pasa a nivel 2 en V52** y la revalidación de la semana entera lo rechaza. La versión 1 no se toca y el jueves sigue bloqueado.
- **Mecanismo de reparación aislado:** sustituyendo el 916 por el 1046 y el 910 por el 433 (remo sentado con cuerda, nivel 1, sin banderas), la reparación publica una versión 2 que **solo cambia el 1303**, sin aumentar la dosis (V8C). Con la corrección de HALL-PROG-01 la prueba sigue pasando igual (PR A y PR B).
- **Semana empezada:** la reparación no versiona (SEMANA_EMPEZADA). Al vencer, la sesión del jueves se cierra como SKIPPED **sin motivo** y cuenta en la adherencia como no hecha. Es una limitación conocida y documentada, que no corresponde al catálogo.

**El 916 no se resuelve relajando su dificultad.** Opciones para decidir:
1. Que la reparación, para los ejercicios que han subido de nivel tras publicarse, sustituya o tolere la diferencia sin revalidar la semana entera. Cambia el contrato de reparación y no se aplica aquí.
2. Una aprobación profesional caso por caso.
3. Regenerar la semana siguiente con el catálogo nuevo.

## 3. Identificación en producción (solo lectura)

```sql
-- Sesiones futuras publicadas que contienen ejercicios cuya seguridad cambia con el cierre.
-- No modifica nada.
WITH cambiados(exercise_id, causa) AS (VALUES
  (105,'OH'),(368,'OH'),(380,'OH'),(466,'OH'),(580,'OH'),(581,'OH'),(814,'OH'),(876,'OH'),(904,'OH'),(1001,'OH'),
  (1031,'OH'),(1242,'OH'),(1260,'OH'),(1314,'OH'),
  (224,'AX'),(228,'AX'),(229,'AX'),(274,'AX'),(321,'AX'),(511,'AX'),(514,'AX'),(705,'AX'),(779,'AX'),(923,'AX'),(927,'AX'),
  (822,'INV'),(857,'INV'),(902,'INV'),(905,'INV'),(559,'FL'),(896,'FL'),(636,'KN'),
  (457,'APOYO'),(458,'APOYO'),(459,'APOYO'),(462,'APOYO'),(777,'APOYO'),(920,'APOYO'),(934,'APOYO'),(1069,'APOYO'),
  (1153,'APOYO'),(1184,'APOYO'),(1303,'APOYO'),(1304,'APOYO'),(1306,'APOYO'),(1317,'APOYO'),(1335,'APOYO'),
  (1083,'HI'))
SELECT p.user_id, pw.planned_workout_id, pw.scheduled_date, pwe.exercise_id, c.causa
  FROM planned_workout_exercises pwe
  JOIN planned_workouts pw ON pw.planned_workout_id = pwe.planned_workout_id
  JOIN weekly_training_plans p ON p.plan_id = pw.plan_id AND p.status = 'ACTIVE'
  JOIN cambiados c ON c.exercise_id = pwe.exercise_id
  JOIN user_profiles up ON up.user_id = p.user_id
 WHERE pw.status = 'SCHEDULED' AND pw.scheduled_date >= CURRENT_DATE
   AND (   (c.causa = 'OH'    AND NOT up.allow_overhead_exercises)
        OR (c.causa = 'FL'    AND NOT up.allow_floor_exercises)
        OR (c.causa = 'KN'    AND NOT up.allow_kneeling_exercises)
        OR (c.causa = 'APOYO' AND NOT up.allow_hanging_exercises)   -- solo si se enciende V53
        OR (c.causa = 'HI'    AND NOT up.allow_high_impact));         -- y además edad >= 60 o principiante: ver SafetyProfile
```

Las columnas de permisos (`allow_overhead_exercises`, `allow_floor_exercises`, `allow_kneeling_exercises` y `allow_hanging_exercises`) están comprobadas contra las migraciones. AX e INV no tienen columna: `SafetyProfile` las deriva de la edad y las condiciones declaradas, así que esos ejercicios hay que cruzarlos con el servicio de seguridad, no con SQL. La consulta se entrega sin ejecutar. Las sesiones que devuelva quedarán bloqueadas al desplegar y habrá que repararlas o regenerarlas.

Recomendación antes de producción:
1. Ejecutar la consulta.
2. Decidir, sesión por sesión, entre reparación y regeneración de la semana siguiente.
3. Revisar el efecto en la adherencia de las sesiones que venzan bloqueadas (SKIPPED sin motivo).
