-- =====================================================================
-- V53 · impacto del criterio conservador sobre los PERMISOS YA GUARDADOS. SOLO LECTURA.
--
-- No modifica nada (solo SELECT, dentro de una transaccion READ ONLY). Lo ejecuta el propietario contra la base que
-- decida; en el cierre NO se ha ejecutado contra datos reales de usuarios.
--
-- Pregunta: si se encendiera fitsense.seguridad.apoyo-suspendido-en-no-colgarse, ¿a quien afecta?
--   1. Perfiles con «no colgarse» guardado (allow_hanging_exercises = false): para ellos el permiso pasaria a excluir
--      tambien los 15 ejercicios de apoyo suspendido sobre un aparato. Nadie lo aprobo con ese significado.
--   2. De ellos, los que tienen esos ejercicios en sesiones aun no hechas de un plan activo: el validador de sesiones
--      futuras los bloquearia (causa apoyo_suspendido) y se intentaria la reparacion localizada.
--   3. Los que ya los ejecutaron (historial, solo informativo: no se toca).
-- =====================================================================
BEGIN TRANSACTION READ ONLY;

WITH apoyo(exercise_id) AS (VALUES (457),(458),(459),(462),(777),(920),(934),(1069),(1153),(1184),(1303),(1304),
                                   (1306),(1317),(1335))
SELECT 'perfiles_no_colgarse' AS medida, count(*) AS n
  FROM user_profiles WHERE allow_hanging_exercises = false
UNION ALL
SELECT 'perfiles_no_colgarse_con_apoyo_en_sesiones_pendientes', count(DISTINCT p.user_id)
  FROM user_profiles up
  JOIN weekly_training_plans p ON p.user_id = up.user_id AND p.status = 'ACTIVE'
  JOIN planned_workouts w ON w.plan_id = p.plan_id AND w.status IN ('SCHEDULED', 'IN_PROGRESS')
  JOIN planned_workout_exercises e ON e.planned_workout_id = w.planned_workout_id
  JOIN apoyo a ON a.exercise_id = e.exercise_id
 WHERE up.allow_hanging_exercises = false
UNION ALL
SELECT 'sesiones_pendientes_afectadas', count(DISTINCT w.planned_workout_id)
  FROM user_profiles up
  JOIN weekly_training_plans p ON p.user_id = up.user_id AND p.status = 'ACTIVE'
  JOIN planned_workouts w ON w.plan_id = p.plan_id AND w.status IN ('SCHEDULED', 'IN_PROGRESS')
  JOIN planned_workout_exercises e ON e.planned_workout_id = w.planned_workout_id
  JOIN apoyo a ON a.exercise_id = e.exercise_id
 WHERE up.allow_hanging_exercises = false
UNION ALL
SELECT 'perfiles_no_colgarse_que_ya_ejecutaron_alguno', count(DISTINCT s.user_id)
  FROM user_profiles up
  JOIN workout_sessions s ON s.user_id = up.user_id
  JOIN workout_session_exercises se ON se.session_id = s.session_id
  JOIN planned_workout_exercises pe ON pe.planned_exercise_id = se.planned_exercise_id
  JOIN apoyo a ON a.exercise_id = pe.exercise_id
 WHERE up.allow_hanging_exercises = false;

-- Detalle de las sesiones pendientes afectadas (sin datos personales: solo ids internos).
WITH apoyo(exercise_id) AS (VALUES (457),(458),(459),(462),(777),(920),(934),(1069),(1153),(1184),(1303),(1304),
                                   (1306),(1317),(1335))
SELECT p.plan_id, w.planned_workout_id, w.scheduled_date, w.status, e.exercise_id
  FROM user_profiles up
  JOIN weekly_training_plans p ON p.user_id = up.user_id AND p.status = 'ACTIVE'
  JOIN planned_workouts w ON w.plan_id = p.plan_id AND w.status IN ('SCHEDULED', 'IN_PROGRESS')
  JOIN planned_workout_exercises e ON e.planned_workout_id = w.planned_workout_id
  JOIN apoyo a ON a.exercise_id = e.exercise_id
 WHERE up.allow_hanging_exercises = false
 ORDER BY p.plan_id, w.scheduled_date, e.exercise_id;

ROLLBACK;
