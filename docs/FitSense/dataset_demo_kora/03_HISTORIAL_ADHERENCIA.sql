-- =====================================================================================================
-- DATASET DEMOSTRATIVO FITSENSE / KORA · 03 · Ejecuciones y adherencia de la semana W-3
-- Generado por DatasetDemoKoraGeneradorIT con los flujos reales del backend (sin IA, sin Replicate).
-- Semana de generacion: W0 = 2026-11-23 (las fechas se trasladan a la semana real al ejecutar).
-- Un solo bloque DO: si algo falla, no se aplica nada. Idempotente: repetirlo no duplica filas.
-- No crea ni modifica usuarios, contrasenas, tokens, catalogo ni esquema.
-- =====================================================================================================
-- Filas: workout_sessions=4, workout_session_exercises=23, weekly_user_metrics=1, user_safety_exclusions=1
DO $demo$
DECLARE
  v_email CONSTANT text := 'usuario.demo.kora@example.com';
  v_user bigint;
  v_w3 date;
  v_sql text;
  v_falta text;
  i int;
BEGIN
  IF NOT EXISTS (SELECT 1 FROM flyway_schema_history WHERE version = '65' AND success) THEN
    RAISE EXCEPTION 'DEMO 03: la base no tiene la migracion V65 aplicada.';
  END IF;
  SELECT user_id INTO v_user FROM users WHERE lower(email) = lower(v_email);
  IF v_user IS NULL THEN
    RAISE EXCEPTION 'DEMO 03: no existe la cuenta %. Registrela por la API y ejecute 01.', v_email;
  END IF;
  -- Idempotencia: si este paso ya se aplico a la cuenta, no se cambia nada.
  IF EXISTS (SELECT 1 FROM workout_sessions WHERE session_id = 910000000) THEN
    IF EXISTS (SELECT 1 FROM workout_sessions WHERE session_id = 910000000 AND user_id = v_user) THEN
      RAISE NOTICE 'DEMO 03: ya aplicado a la cuenta %. No se cambia nada.', v_user;
      RETURN;
    END IF;
    RAISE EXCEPTION 'DEMO 03: el id reservado 910000000 de workout_sessions pertenece a otra cuenta. No se carga nada.';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM weekly_training_plans WHERE plan_id = 910000000 AND user_id = v_user) THEN
    RAISE EXCEPTION 'DEMO: falta el paso anterior. Ejecute antes 02_RUTINA_INICIAL.sql.';
  END IF;
  -- Ancla: la misma semana W-3 con la que se cargo 02 (todas las fechas quedan coherentes).
  SELECT week_start_date INTO v_w3 FROM weekly_training_plans WHERE plan_id = 910000000 AND user_id = v_user;
  -- Ids reservados libres.
  IF EXISTS (SELECT 1 FROM workout_sessions WHERE session_id = ANY('{910000000,910000001,910000002,910000003}'::bigint[])) THEN
    RAISE EXCEPTION 'DEMO 03: ya existen filas con ids reservados en workout_sessions (de otra cuenta). No se carga nada.';
  END IF;
  IF EXISTS (SELECT 1 FROM workout_session_exercises WHERE session_exercise_id = ANY('{910000000,910000001,910000002,910000003,910000004,910000005,910000006,910000007,910000008,910000009,910000010,910000011,910000012,910000013,910000014,910000015,910000016,910000017,910000018,910000019,910000020,910000021,910000022}'::bigint[])) THEN
    RAISE EXCEPTION 'DEMO 03: ya existen filas con ids reservados en workout_session_exercises (de otra cuenta). No se carga nada.';
  END IF;
  IF EXISTS (SELECT 1 FROM weekly_user_metrics WHERE weekly_metric_id = ANY('{910000000}'::bigint[])) THEN
    RAISE EXCEPTION 'DEMO 03: ya existen filas con ids reservados en weekly_user_metrics (de otra cuenta). No se carga nada.';
  END IF;
  IF EXISTS (SELECT 1 FROM user_safety_exclusions WHERE exclusion_id = ANY('{910000000}'::bigint[])) THEN
    RAISE EXCEPTION 'DEMO 03: ya existen filas con ids reservados en user_safety_exclusions (de otra cuenta). No se carga nada.';
  END IF;
  -- Mismo catalogo: cada ejercicio existe con el mismo source_code que en el generador.
  SELECT string_agg(v.id::text, ', ') INTO v_falta FROM (VALUES (1025,'0596')) AS v(id, codigo)
    LEFT JOIN exercises e ON e.exercise_id = v.id WHERE e.source_code IS DISTINCT FROM v.codigo;
  IF v_falta IS NOT NULL THEN
    RAISE EXCEPTION 'DEMO 03: el catalogo no coincide (ejercicios %). No se carga nada.', v_falta;
  END IF;
  v_sql := $q$
INSERT INTO workout_sessions (session_id, user_id, planned_workout_id, plan_id, attempt_number, counts_toward_adherence, started_at, ended_at, active_minutes, completion_percentage, session_rpe, satisfaction, source, status, created_at, updated_at)
OVERRIDING SYSTEM VALUE
VALUES
  (910000000, @@U@@, 910000000, 910000000, 1, TRUE, '@@D0@@ 23:30:00.000000+00'::timestamptz, '@@D1@@ 00:30:00.000000+00'::timestamptz, 55, '100.00'::numeric(5,2), 6, 4, 'APP_TRACKED'::character varying(20), 'COMPLETED'::character varying(20), '@@D0@@ 23:30:00.000000+00'::timestamptz, '@@D1@@ 00:30:00.000000+00'::timestamptz),
  (910000001, @@U@@, 910000001, 910000000, 1, TRUE, '@@D1@@ 23:30:00.000000+00'::timestamptz, '@@D2@@ 00:30:00.000000+00'::timestamptz, 55, '100.00'::numeric(5,2), 7, 4, 'APP_TRACKED'::character varying(20), 'COMPLETED'::character varying(20), '@@D1@@ 23:30:00.000000+00'::timestamptz, '@@D2@@ 00:30:00.000000+00'::timestamptz),
  (910000002, @@U@@, 910000002, 910000000, 1, TRUE, '@@D2@@ 23:30:00.000000+00'::timestamptz, '@@D3@@ 00:05:00.000000+00'::timestamptz, 30, '53.33'::numeric(5,2), 7, 3, 'APP_TRACKED'::character varying(20), 'PARTIAL'::character varying(20), '@@D2@@ 23:30:00.000000+00'::timestamptz, '@@D3@@ 00:05:00.000000+00'::timestamptz),
  (910000003, @@U@@, 910000003, 910000000, 1, TRUE, '@@D3@@ 23:30:00.000000+00'::timestamptz, '@@D4@@ 00:30:00.000000+00'::timestamptz, 55, '85.71'::numeric(5,2), 7, 3, 'APP_TRACKED'::character varying(20), 'COMPLETED'::character varying(20), '@@D3@@ 23:30:00.000000+00'::timestamptz, '@@D4@@ 00:30:00.000000+00'::timestamptz);
INSERT INTO workout_session_exercises (session_exercise_id, session_id, planned_exercise_id, actual_sets, actual_reps_total, actual_duration_seconds, actual_load_kg, completion_percentage, status, skip_reason, created_at, updated_at, system_block_cause)
OVERRIDING SYSTEM VALUE
VALUES
  (910000000, 910000000, 910000000, 3, 30, NULL, NULL, '100.00'::numeric(5,2), 'COMPLETED'::character varying(20), NULL, '@@D0@@ 23:37:51.428000+00'::timestamptz, '@@D0@@ 23:37:51.428000+00'::timestamptz, NULL),
  (910000001, 910000000, 910000001, 3, 30, NULL, NULL, '100.00'::numeric(5,2), 'COMPLETED'::character varying(20), NULL, '@@D0@@ 23:45:42.856000+00'::timestamptz, '@@D0@@ 23:45:42.856000+00'::timestamptz, NULL),
  (910000002, 910000000, 910000002, 3, 30, NULL, NULL, '100.00'::numeric(5,2), 'COMPLETED'::character varying(20), NULL, '@@D0@@ 23:53:34.284000+00'::timestamptz, '@@D0@@ 23:53:34.284000+00'::timestamptz, NULL),
  (910000003, 910000000, 910000003, 3, 30, NULL, NULL, '100.00'::numeric(5,2), 'COMPLETED'::character varying(20), NULL, '@@D1@@ 00:01:25.712000+00'::timestamptz, '@@D1@@ 00:01:25.712000+00'::timestamptz, NULL),
  (910000004, 910000000, 910000004, 3, 30, NULL, NULL, '100.00'::numeric(5,2), 'COMPLETED'::character varying(20), NULL, '@@D1@@ 00:09:17.140000+00'::timestamptz, '@@D1@@ 00:09:17.140000+00'::timestamptz, NULL),
  (910000005, 910000000, 910000005, 3, 30, NULL, NULL, '100.00'::numeric(5,2), 'COMPLETED'::character varying(20), NULL, '@@D1@@ 00:17:08.568000+00'::timestamptz, '@@D1@@ 00:17:08.568000+00'::timestamptz, NULL),
  (910000006, 910000001, 910000006, 3, 30, NULL, NULL, '100.00'::numeric(5,2), 'COMPLETED'::character varying(20), NULL, '@@D1@@ 23:39:10.000000+00'::timestamptz, '@@D1@@ 23:39:10.000000+00'::timestamptz, NULL),
  (910000007, 910000001, 910000007, 2, 20, NULL, NULL, '100.00'::numeric(5,2), 'COMPLETED'::character varying(20), NULL, '@@D1@@ 23:48:20.000000+00'::timestamptz, '@@D1@@ 23:48:20.000000+00'::timestamptz, NULL),
  (910000008, 910000001, 910000008, 3, 30, NULL, NULL, '100.00'::numeric(5,2), 'COMPLETED'::character varying(20), NULL, '@@D1@@ 23:57:30.000000+00'::timestamptz, '@@D1@@ 23:57:30.000000+00'::timestamptz, NULL),
  (910000009, 910000001, 910000009, 3, 30, NULL, NULL, '100.00'::numeric(5,2), 'COMPLETED'::character varying(20), NULL, '@@D2@@ 00:06:40.000000+00'::timestamptz, '@@D2@@ 00:06:40.000000+00'::timestamptz, NULL),
  (910000010, 910000001, 910000010, 3, 30, NULL, NULL, '100.00'::numeric(5,2), 'COMPLETED'::character varying(20), NULL, '@@D2@@ 00:15:50.000000+00'::timestamptz, '@@D2@@ 00:15:50.000000+00'::timestamptz, NULL),
  (910000011, 910000002, 910000011, 3, 30, NULL, NULL, '100.00'::numeric(5,2), 'COMPLETED'::character varying(20), NULL, '@@D2@@ 23:35:00.000000+00'::timestamptz, '@@D2@@ 23:35:00.000000+00'::timestamptz, NULL),
  (910000012, 910000002, 910000012, 3, 30, NULL, NULL, '100.00'::numeric(5,2), 'COMPLETED'::character varying(20), NULL, '@@D2@@ 23:40:00.000000+00'::timestamptz, '@@D2@@ 23:40:00.000000+00'::timestamptz, NULL),
  (910000013, 910000002, 910000013, 2, 20, NULL, NULL, '66.67'::numeric(5,2), 'PARTIAL'::character varying(20), 'FATIGUE'::character varying(40), '@@D2@@ 23:45:00.000000+00'::timestamptz, '@@D2@@ 23:45:00.000000+00'::timestamptz, NULL),
  (910000014, 910000002, 910000014, 0, NULL, NULL, NULL, '0.00'::numeric(5,2), 'SKIPPED'::character varying(20), 'LACK_OF_TIME'::character varying(40), '@@D2@@ 23:50:00.000000+00'::timestamptz, '@@D2@@ 23:50:00.000000+00'::timestamptz, NULL),
  (910000015, 910000002, 910000015, 0, NULL, NULL, NULL, '0.00'::numeric(5,2), 'SKIPPED'::character varying(20), 'LACK_OF_TIME'::character varying(40), '@@D2@@ 23:55:00.000000+00'::timestamptz, '@@D2@@ 23:55:00.000000+00'::timestamptz, NULL),
  (910000016, 910000003, 910000016, 0, NULL, NULL, NULL, '0.00'::numeric(5,2), 'SKIPPED'::character varying(20), 'PAIN_OR_DISCOMFORT'::character varying(40), '@@D3@@ 23:36:52.500000+00'::timestamptz, '@@D3@@ 23:36:52.500000+00'::timestamptz, NULL),
  (910000017, 910000003, 910000017, 3, 30, NULL, NULL, '100.00'::numeric(5,2), 'COMPLETED'::character varying(20), NULL, '@@D3@@ 23:43:45.000000+00'::timestamptz, '@@D3@@ 23:43:45.000000+00'::timestamptz, NULL),
  (910000018, 910000003, 910000018, 3, 30, NULL, NULL, '100.00'::numeric(5,2), 'COMPLETED'::character varying(20), NULL, '@@D3@@ 23:50:37.500000+00'::timestamptz, '@@D3@@ 23:50:37.500000+00'::timestamptz, NULL),
  (910000019, 910000003, 910000019, 2, 20, NULL, NULL, '100.00'::numeric(5,2), 'COMPLETED'::character varying(20), NULL, '@@D3@@ 23:57:30.000000+00'::timestamptz, '@@D3@@ 23:57:30.000000+00'::timestamptz, NULL),
  (910000020, 910000003, 910000020, 3, 30, NULL, NULL, '100.00'::numeric(5,2), 'COMPLETED'::character varying(20), NULL, '@@D4@@ 00:04:22.500000+00'::timestamptz, '@@D4@@ 00:04:22.500000+00'::timestamptz, NULL),
  (910000021, 910000003, 910000021, 3, 30, NULL, NULL, '100.00'::numeric(5,2), 'COMPLETED'::character varying(20), NULL, '@@D4@@ 00:11:15.000000+00'::timestamptz, '@@D4@@ 00:11:15.000000+00'::timestamptz, NULL),
  (910000022, 910000003, 910000022, 3, 30, NULL, NULL, '100.00'::numeric(5,2), 'COMPLETED'::character varying(20), NULL, '@@D4@@ 00:18:07.500000+00'::timestamptz, '@@D4@@ 00:18:07.500000+00'::timestamptz, NULL);
INSERT INTO weekly_user_metrics (weekly_metric_id, user_id, plan_id, week_number, week_start_date, week_end_date, has_active_plan, scheduled_workouts, valid_workouts, completed_workouts, skipped_workouts, assigned_exercises, completed_exercises, weighted_adherence_pct, frequency_adherence_pct, workout_adherence_pct, exercise_adherence_pct, total_training_minutes, average_session_rpe, average_satisfaction, consecutive_skips, days_since_last_workout, dominant_skip_reason, risk_score, risk_level, risk_factors, is_dropout, calculation_version, calculated_at, planned_week_volume, executed_volume, planned_reps, planned_seconds, executed_reps, executed_seconds, recorded_workouts, observed_adherence_pct, registration_coverage_pct)
OVERRIDING SYSTEM VALUE
VALUES
  (910000000, @@U@@, 910000000, 1, '@@D0@@'::date, '@@D6@@'::date, TRUE, 5, 4, 3, 1, 30, 19, '67.81'::numeric(5,2), '80.00'::numeric(5,2), '60.00'::numeric(5,2), '63.33'::numeric(5,2), 195, '6.75'::numeric(4,2), '3.50'::numeric(4,2), 1, NULL, 'LACK_OF_TIME'::character varying(40), '15.00'::numeric(5,2), 'LOW'::character varying(20), '{"low_adherence": 10, "consecutive_skips": 5}'::jsonb, FALSE, 'MVP-1.8'::character varying(30), '@@D7@@ 08:00:00.000000+00'::timestamptz, 842, 570, 842, 0, 570, 0, 5, '67.81'::numeric(5,2), '100.00'::numeric(5,2));
INSERT INTO user_safety_exclusions (exclusion_id, user_id, exercise_id, reason, source_plan_id, source_planned_workout_id, source_planned_exercise_id, event_date, recorded_by, recorded_at, status, reactivated_at, reactivation_reason, reactivation_evidence, identified_at, identification_evidence)
OVERRIDING SYSTEM VALUE
VALUES
  (910000000, @@U@@, 1025, 'DOLOR_IDENTIFICADO'::character varying(40), 910000000, 910000003, 910000016, '@@D3@@'::date, 'CICLO_SEMANAL'::character varying(30), '@@D3@@ 23:36:52.500000+00'::timestamptz, 'ACTIVA'::character varying(20), NULL, NULL, NULL, NULL, NULL);
$q$;
  -- Marcadores: @@Dn@@ = lunes de W-3 + n dias; @@U@@ = la cuenta.
  FOR i IN -35..91 LOOP
    v_sql := replace(v_sql, '@@D' || i || '@@', to_char(v_w3 + i, 'YYYY-MM-DD'));
  END LOOP;
  v_sql := replace(v_sql, '@@U@@', v_user::text);
  IF position('@@' IN v_sql) > 0 THEN
    RAISE EXCEPTION 'DEMO: marcador sin sustituir';
  END IF;
  EXECUTE v_sql;
  RAISE NOTICE 'DEMO 03 aplicado a la cuenta % (workout_sessions=4, workout_session_exercises=23, weekly_user_metrics=1, user_safety_exclusions=1)', v_user;
END
$demo$;
