-- =====================================================================================================
-- DATASET DEMOSTRATIVO FITSENSE / KORA · 06 · Verificacion (SOLO LECTURA)
-- Cada consulta empieza con una linea «-- ###». Ejecutelas una a una (DBeaver: Ctrl+Enter sobre cada una;
-- pgAdmin: seleccionar y F5). Ninguna modifica datos.
-- Lo que la app muestra se comprueba con la API (README_EJECUCION.md, paso 5); estas consultas comprueban
-- las tablas que esos endpoints leen.
-- =====================================================================================================

-- ### 1. Cuenta demo (sin hash de contrasena). Esperado: 1 fila, ACTIVE, America/Lima, sin participacion.
SELECT u.user_id, u.email, u.first_name, u.last_name, u.timezone, u.status, u.role,
       u.is_study_participant, u.withdrawn_at, u.created_at, u.last_login_at
  FROM users u
 WHERE lower(u.email) = lower('usuario.demo.kora@example.com');

-- ### 2. Perfil y objetivo. Esperado: GAIN_MUSCLE, INTERMEDIATE, 5 dias (1-5), 60 min, GYM, sin restricciones.
SELECT p.user_id, p.goal_type, p.goal_text, p.fitness_level, p.training_location, p.days_per_week,
       p.available_days, p.session_minutes, p.equipment_codes, p.blocked_exercise_ids, p.health_notes,
       p.allow_high_impact, p.allow_floor_exercises, p.allow_kneeling_exercises, p.allow_hanging_exercises,
       p.allow_overhead_exercises, p.created_at
  FROM user_profiles p JOIN users u ON u.user_id = p.user_id
 WHERE lower(u.email) = lower('usuario.demo.kora@example.com');

-- ### 3. Planes semanales (lo que lista GET /users/me/plans). Esperado: 4 planes; 3 COMPLETED y 1 ACTIVE;
-- ninguno con origen AI ni modelo.
SELECT p.plan_id, p.week_number, p.week_start_date, p.week_end_date, p.plan_version, p.status,
       p.generation_source, p.model_name, p.planning_policy, p.planned_days_count, p.parent_plan_id,
       p.activated_at, p.created_at
  FROM weekly_training_plans p JOIN users u ON u.user_id = p.user_id
 WHERE lower(u.email) = lower('usuario.demo.kora@example.com')
 ORDER BY p.week_start_date, p.plan_id;

-- ### 4. Plan vigente HOY en Lima (lo que devuelve GET /users/me/plan/current). Esperado: exactamente 1 fila.
SELECT p.plan_id, p.week_start_date, p.week_end_date, p.status,
       (now() AT TIME ZONE 'America/Lima')::date AS hoy_lima
  FROM weekly_training_plans p JOIN users u ON u.user_id = p.user_id
 WHERE lower(u.email) = lower('usuario.demo.kora@example.com')
   AND p.status = 'ACTIVE'
   AND (now() AT TIME ZONE 'America/Lima')::date BETWEEN p.week_start_date AND p.week_end_date;

-- ### 5. Sesiones programadas por plan (estado y numero de ejercicios).
SELECT p.plan_id, p.week_start_date, pw.planned_workout_id, pw.display_order, pw.scheduled_date,
       pw.workout_name, pw.focus_code, pw.expected_duration_minutes, pw.status, pw.skip_reason,
       pw.source_planned_workout_id, count(pwe.planned_exercise_id) AS ejercicios
  FROM weekly_training_plans p
  JOIN users u ON u.user_id = p.user_id
  JOIN planned_workouts pw ON pw.plan_id = p.plan_id
  LEFT JOIN planned_workout_exercises pwe ON pwe.planned_workout_id = pw.planned_workout_id
 WHERE lower(u.email) = lower('usuario.demo.kora@example.com')
 GROUP BY p.plan_id, p.week_start_date, pw.planned_workout_id
 ORDER BY pw.scheduled_date, pw.display_order;

-- ### 6. Ejercicios, series, repeticiones y duraciones del plan vigente (catalogo real).
SELECT pw.scheduled_date, pw.workout_name, pwe.exercise_order, e.exercise_id, e.name_es, e.name_en,
       pwe.prescription_type, pwe.planned_sets, pwe.planned_reps, pwe.planned_duration_seconds,
       pwe.rest_seconds, pwe.target_load_kg
  FROM weekly_training_plans p
  JOIN users u ON u.user_id = p.user_id
  JOIN planned_workouts pw ON pw.plan_id = p.plan_id
  JOIN planned_workout_exercises pwe ON pwe.planned_workout_id = pw.planned_workout_id
  JOIN exercises e ON e.exercise_id = pwe.exercise_id
 WHERE lower(u.email) = lower('usuario.demo.kora@example.com') AND p.status = 'ACTIVE'
 ORDER BY pw.scheduled_date, pwe.exercise_order;

-- ### 7. Historial de entrenamiento: sesiones registradas (completas, parciales) y omitidas.
SELECT pw.scheduled_date, p.plan_id, pw.workout_name, pw.status AS estado_programado, pw.skip_reason AS omision,
       s.session_id, s.status AS estado_sesion, s.completion_percentage, s.session_rpe, s.satisfaction,
       s.active_minutes, s.started_at, s.ended_at, s.source,
       count(wse.session_exercise_id) AS ejercicios_registrados,
       count(*) FILTER (WHERE wse.status = 'COMPLETED') AS ejercicios_completos
  FROM weekly_training_plans p
  JOIN users u ON u.user_id = p.user_id
  JOIN planned_workouts pw ON pw.plan_id = p.plan_id
  LEFT JOIN workout_sessions s ON s.planned_workout_id = pw.planned_workout_id
  LEFT JOIN workout_session_exercises wse ON wse.session_id = s.session_id
 WHERE lower(u.email) = lower('usuario.demo.kora@example.com') AND p.status <> 'ACTIVE'
 GROUP BY pw.planned_workout_id, p.plan_id, s.session_id
 ORDER BY pw.scheduled_date;

-- ### 8. Ejercicios no hechos o a medias, con su causa (fatiga, falta de tiempo, dolor).
SELECT pw.scheduled_date, e.name_es, pwe.planned_sets, pwe.planned_reps, wse.actual_sets, wse.actual_reps_total,
       wse.completion_percentage, wse.status, wse.skip_reason
  FROM workout_session_exercises wse
  JOIN workout_sessions s ON s.session_id = wse.session_id
  JOIN users u ON u.user_id = s.user_id
  JOIN planned_workout_exercises pwe ON pwe.planned_exercise_id = wse.planned_exercise_id
  JOIN planned_workouts pw ON pw.planned_workout_id = pwe.planned_workout_id
  JOIN exercises e ON e.exercise_id = pwe.exercise_id
 WHERE lower(u.email) = lower('usuario.demo.kora@example.com') AND wse.skip_reason IS NOT NULL
 ORDER BY pw.scheduled_date, pwe.exercise_order;

-- ### 9. Adherencia semanal (GET /users/me/metrics/weekly) y el recalculo desde las sesiones.
-- adherencia_recalculada = media de min(100, cumplimiento de la sesion que cuenta) sobre TODOS los previstos
-- (sin sesion = 0), la regla de WeeklyMetricsCalculator. Esperado: coincide = true en todas las filas.
WITH demo AS (SELECT user_id FROM users WHERE lower(email) = lower('usuario.demo.kora@example.com')),
     recalculo AS (
       SELECT pw.plan_id, count(*) AS previstos,
              round(avg(LEAST(100, coalesce(s.completion_percentage, 0))), 2) AS adherencia,
              sum(s.active_minutes) AS minutos, round(avg(s.session_rpe), 2) AS rpe,
              round(avg(s.satisfaction), 2) AS satisfaccion
         FROM planned_workouts pw
         JOIN weekly_training_plans p ON p.plan_id = pw.plan_id
         JOIN demo d ON d.user_id = p.user_id
         LEFT JOIN workout_sessions s ON s.planned_workout_id = pw.planned_workout_id AND s.counts_toward_adherence
        GROUP BY pw.plan_id)
SELECT m.week_start_date, m.plan_id, m.scheduled_workouts, m.valid_workouts, m.completed_workouts,
       m.skipped_workouts, m.recorded_workouts, m.assigned_exercises, m.completed_exercises,
       m.weighted_adherence_pct, m.observed_adherence_pct, m.frequency_adherence_pct, m.workout_adherence_pct,
       m.exercise_adherence_pct, m.total_training_minutes, m.average_session_rpe, m.average_satisfaction,
       m.dominant_skip_reason, m.risk_level, m.calculation_version, m.calculated_at,
       r.adherencia AS adherencia_recalculada,
       (m.weighted_adherence_pct = r.adherencia AND m.scheduled_workouts = r.previstos
        AND m.total_training_minutes = r.minutos AND m.average_session_rpe = r.rpe
        AND m.average_satisfaction = r.satisfaccion) AS coincide
  FROM weekly_user_metrics m
  JOIN demo d ON d.user_id = m.user_id
  JOIN recalculo r ON r.plan_id = m.plan_id
 ORDER BY m.week_start_date;

-- ### 10. Adaptaciones (GET /users/me/plan/adaptations): plan anterior, motivo, decision y plan resultante.
SELECT a.attempt_id, m.week_start_date AS semana_medida, a.source_plan_id AS plan_anterior,
       a.new_week_start_date AS semana_nueva, a.resulting_plan_id AS plan_resultante, a.decision, a.status,
       a.generation_source, a.ai_calls, a.observed_adherence_pct, a.targeted_interventions, a.reason_codes,
       a.adaptation_rule_version, a.conservation_report -> 'sustituciones' AS sustituciones,
       a.conservation_report -> 'pasos' AS pasos, a.conservation_report -> 'correcciones_del_backend' AS correcciones,
       a.created_at
  FROM plan_adaptation_attempts a
  JOIN weekly_user_metrics m ON m.weekly_metric_id = a.weekly_metric_id
  JOIN users u ON u.user_id = a.user_id
 WHERE lower(u.email) = lower('usuario.demo.kora@example.com')
 ORDER BY a.new_week_start_date;

-- ### 11. Diferencias original -> adaptado (misma sesion y posicion en el plan anterior y en el resultante).
-- Esperado: semana 2: 1 sustitucion por dolor (SUST-SEG-1); semana 3: sin cambios; semana 4: 1 progresion.
WITH demo AS (SELECT user_id FROM users WHERE lower(email) = lower('usuario.demo.kora@example.com')),
     presc AS (
       SELECT pw.plan_id, pw.display_order, pwe.exercise_order, pwe.exercise_id, e.name_es,
              pwe.planned_sets, pwe.planned_reps, pwe.planned_duration_seconds, pwe.rest_seconds
         FROM planned_workouts pw
         JOIN planned_workout_exercises pwe ON pwe.planned_workout_id = pw.planned_workout_id
         JOIN exercises e ON e.exercise_id = pwe.exercise_id),
     posiciones AS (
       SELECT DISTINCT a.attempt_id, x.display_order, x.exercise_order
         FROM plan_adaptation_attempts a
         JOIN demo d ON d.user_id = a.user_id
         JOIN presc x ON x.plan_id IN (a.source_plan_id, a.resulting_plan_id))
SELECT a.source_plan_id AS plan_anterior, a.resulting_plan_id AS plan_resultante, a.decision, a.status,
       ps.display_order AS sesion, ps.exercise_order AS orden,
       o.name_es AS antes, o.planned_sets || 'x' || coalesce(o.planned_reps::text, o.planned_duration_seconds || 's') AS dosis_antes,
       n.name_es AS despues, n.planned_sets || 'x' || coalesce(n.planned_reps::text, n.planned_duration_seconds || 's') AS dosis_despues
  FROM posiciones ps
  JOIN plan_adaptation_attempts a ON a.attempt_id = ps.attempt_id
  LEFT JOIN presc o ON o.plan_id = a.source_plan_id AND o.display_order = ps.display_order
                   AND o.exercise_order = ps.exercise_order
  LEFT JOIN presc n ON n.plan_id = a.resulting_plan_id AND n.display_order = ps.display_order
                   AND n.exercise_order = ps.exercise_order
 WHERE o.exercise_id IS DISTINCT FROM n.exercise_id OR o.planned_sets IS DISTINCT FROM n.planned_sets
    OR o.planned_reps IS DISTINCT FROM n.planned_reps
    OR o.planned_duration_seconds IS DISTINCT FROM n.planned_duration_seconds
    OR o.rest_seconds IS DISTINCT FROM n.rest_seconds
 ORDER BY a.new_week_start_date, sesion, orden;

-- ### 12. Intervenciones registradas por el cierre semanal (GET /users/me/interventions).
SELECT ui.intervention_id, ui.source_plan_id, ui.resulting_plan_id, ui.trigger_adherence_pct, ui.trigger_skip_reason,
       ui.adjustment_types, ui.previous_week_volume, ui.resulting_week_volume, ui.actual_volume_change_pct,
       ui.message_shown, ui.rule_version, ui.outcome, ui.adherence_after_pct, ui.created_at
  FROM user_interventions ui JOIN users u ON u.user_id = ui.user_id
 WHERE lower(u.email) = lower('usuario.demo.kora@example.com')
 ORDER BY ui.created_at;

-- ### 13. Exclusion por dolor (GET /users/me/pain-exclusions y /plan/limitations).
SELECT x.exclusion_id, x.exercise_id, e.name_es, x.reason, x.status, x.event_date, x.recorded_by, x.recorded_at,
       x.source_plan_id, x.source_planned_workout_id
  FROM user_safety_exclusions x
  JOIN users u ON u.user_id = x.user_id
  JOIN exercises e ON e.exercise_id = x.exercise_id
 WHERE lower(u.email) = lower('usuario.demo.kora@example.com');

-- ### 14. Integridad. Esperado: ok = true en TODAS las filas.
WITH demo AS (SELECT user_id FROM users WHERE lower(email) = lower('usuario.demo.kora@example.com')),
     planes AS (SELECT p.* FROM weekly_training_plans p JOIN demo d ON d.user_id = p.user_id)
SELECT 'un solo plan ACTIVE y cubre hoy en Lima' AS comprobacion,
       (SELECT count(*) FROM planes WHERE status = 'ACTIVE') = 1
       AND EXISTS (SELECT 1 FROM planes WHERE status = 'ACTIVE'
                   AND (now() AT TIME ZONE 'America/Lima')::date BETWEEN week_start_date AND week_end_date) AS ok
UNION ALL
SELECT 'ningun plan con origen AI ni modelo (rutinas del motor de reglas y del cierre sin IA)',
       NOT EXISTS (SELECT 1 FROM planes WHERE generation_source = 'AI' OR model_name IS NOT NULL)
UNION ALL
SELECT 'ningun intento de adaptacion con llamadas a la IA',
       NOT EXISTS (SELECT 1 FROM plan_adaptation_attempts a JOIN demo d ON d.user_id = a.user_id WHERE a.ai_calls > 0)
UNION ALL
SELECT 'semanas sin solapes (un plan por semana)',
       (SELECT count(*) FROM planes) = (SELECT count(DISTINCT week_start_date) FROM planes)
       AND NOT EXISTS (SELECT 1 FROM planes a JOIN planes b ON a.plan_id < b.plan_id
                       AND a.week_start_date <= b.week_end_date AND b.week_start_date <= a.week_end_date)
UNION ALL
SELECT 'cada sesion es de un entrenamiento de un plan de la cuenta',
       NOT EXISTS (SELECT 1 FROM workout_sessions s JOIN demo d ON d.user_id = s.user_id
                   LEFT JOIN planned_workouts pw ON pw.planned_workout_id = s.planned_workout_id
                   LEFT JOIN planes p ON p.plan_id = pw.plan_id
                   WHERE p.plan_id IS NULL OR p.plan_id <> s.plan_id)
UNION ALL
SELECT 'ninguna sesion abierta',
       NOT EXISTS (SELECT 1 FROM workout_sessions s JOIN demo d ON d.user_id = s.user_id WHERE s.ended_at IS NULL)
UNION ALL
SELECT 'todos los ejercicios existen en el catalogo y estan activos',
       NOT EXISTS (SELECT 1 FROM planned_workout_exercises pwe
                   JOIN planned_workouts pw ON pw.planned_workout_id = pwe.planned_workout_id
                   JOIN planes p ON p.plan_id = pw.plan_id
                   LEFT JOIN exercises e ON e.exercise_id = pwe.exercise_id
                   WHERE e.exercise_id IS NULL OR NOT e.is_active)
UNION ALL
SELECT 'adaptaciones encadenadas: cada una parte del plan medido y publica el de la semana siguiente',
       NOT EXISTS (SELECT 1 FROM plan_adaptation_attempts a JOIN demo d ON d.user_id = a.user_id
                   JOIN planes o ON o.plan_id = a.source_plan_id
                   JOIN planes r ON r.plan_id = a.resulting_plan_id
                   JOIN weekly_user_metrics m ON m.weekly_metric_id = a.weekly_metric_id
                   WHERE m.plan_id <> o.plan_id OR r.week_start_date <> o.week_start_date + 7
                      OR a.new_week_start_date <> r.week_start_date)
UNION ALL
SELECT 'una metrica por semana cerrada (las 3 historicas)',
       (SELECT count(*) FROM weekly_user_metrics m JOIN demo d ON d.user_id = m.user_id) = 3
UNION ALL
SELECT 'filas del dataset en el rango de ids reservado (910000000-910999999)',
       NOT EXISTS (SELECT 1 FROM planes WHERE plan_id NOT BETWEEN 910000000 AND 910999999);
