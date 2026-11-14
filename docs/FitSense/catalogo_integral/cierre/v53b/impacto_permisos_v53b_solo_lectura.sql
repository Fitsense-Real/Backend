-- =====================================================================
-- V53-B · alcance del permiso propio «apoyo suspendido» sobre los PERFILES YA GUARDADOS. SOLO LECTURA.
--
-- No modifica nada (solo SELECT, dentro de una transaccion READ ONLY). Requiere V55 aplicada (columnas
-- user_profiles.allow_suspended_support y suspended_support_confirmed_at). Lo ejecuta el propietario contra la base que
-- decida; en esta propuesta NO se ha ejecutado contra datos reales de usuarios. Complementa, sin sustituirlo, a
-- cierre/v53/impacto_permisos_v53_solo_lectura.sql (opcion A).
--
-- Regla que se mide (opcion B encendida): el apoyo suspendido queda excluido cuando
--   coalesce(allow_suspended_support, allow_hanging_exercises) = false
-- es decir, sin responder hereda «no colgarse»; respondido, manda el permiso propio.
-- =====================================================================
BEGIN TRANSACTION READ ONLY;

WITH apoyo(exercise_id) AS (VALUES (457),(458),(459),(462),(777),(920),(934),(1069),(1153),(1184),(1303),(1304),
                                   (1306),(1317),(1335)),
     excluidos AS (SELECT user_id FROM user_profiles
                    WHERE coalesce(allow_suspended_support, allow_hanging_exercises) = false)
SELECT 'perfiles_total' AS medida, count(*) AS n FROM user_profiles
UNION ALL
SELECT 'perfiles_no_colgarse', count(*) FROM user_profiles WHERE allow_hanging_exercises = false
UNION ALL
SELECT 'perfiles_permiso_sin_responder', count(*) FROM user_profiles WHERE allow_suspended_support IS NULL
UNION ALL
SELECT 'perfiles_pendientes_de_confirmar', count(*) FROM user_profiles WHERE suspended_support_confirmed_at IS NULL
UNION ALL
SELECT 'perfiles_confirmados_admite', count(*) FROM user_profiles WHERE allow_suspended_support = true
UNION ALL
SELECT 'perfiles_confirmados_excluye', count(*) FROM user_profiles WHERE allow_suspended_support = false
UNION ALL
SELECT 'perfiles_excluidos_con_B', count(*) FROM excluidos
UNION ALL
SELECT 'perfiles_excluidos_con_B_con_apoyo_en_sesiones_pendientes', count(DISTINCT p.user_id)
  FROM excluidos x
  JOIN weekly_training_plans p ON p.user_id = x.user_id AND p.status = 'ACTIVE'
  JOIN planned_workouts w ON w.plan_id = p.plan_id AND w.status IN ('SCHEDULED', 'IN_PROGRESS')
  JOIN planned_workout_exercises e ON e.planned_workout_id = w.planned_workout_id
  JOIN apoyo a ON a.exercise_id = e.exercise_id
UNION ALL
SELECT 'sesiones_pendientes_afectadas_con_B', count(DISTINCT w.planned_workout_id)
  FROM excluidos x
  JOIN weekly_training_plans p ON p.user_id = x.user_id AND p.status = 'ACTIVE'
  JOIN planned_workouts w ON w.plan_id = p.plan_id AND w.status IN ('SCHEDULED', 'IN_PROGRESS')
  JOIN planned_workout_exercises e ON e.planned_workout_id = w.planned_workout_id
  JOIN apoyo a ON a.exercise_id = e.exercise_id;

-- Detalle de las sesiones pendientes afectadas con la opcion B (sin datos personales: solo ids internos).
WITH apoyo(exercise_id) AS (VALUES (457),(458),(459),(462),(777),(920),(934),(1069),(1153),(1184),(1303),(1304),
                                   (1306),(1317),(1335))
SELECT p.plan_id, w.planned_workout_id, w.scheduled_date, w.status, e.exercise_id,
       up.allow_suspended_support IS NULL AS heredado
  FROM user_profiles up
  JOIN weekly_training_plans p ON p.user_id = up.user_id AND p.status = 'ACTIVE'
  JOIN planned_workouts w ON w.plan_id = p.plan_id AND w.status IN ('SCHEDULED', 'IN_PROGRESS')
  JOIN planned_workout_exercises e ON e.planned_workout_id = w.planned_workout_id
  JOIN apoyo a ON a.exercise_id = e.exercise_id
 WHERE coalesce(up.allow_suspended_support, up.allow_hanging_exercises) = false
 ORDER BY p.plan_id, w.scheduled_date, e.exercise_id;

ROLLBACK;
