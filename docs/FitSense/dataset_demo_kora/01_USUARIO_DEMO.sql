-- =====================================================================================================
-- DATASET DEMOSTRATIVO FITSENSE / KORA · 01 · Perfil del usuario demo (GAIN_MUSCLE, INTERMEDIATE, 5 dias, 60 min, gimnasio)
-- Generado por DatasetDemoKoraGeneradorIT con los flujos reales del backend (sin IA, sin Replicate).
-- Semana de generacion: W0 = 2026-11-23 (las fechas se trasladan a la semana real al ejecutar).
-- Un solo bloque DO: si algo falla, no se aplica nada. Idempotente: repetirlo no duplica filas.
-- No crea ni modifica usuarios, contrasenas, tokens, catalogo ni esquema.
-- =====================================================================================================
-- Requisito: la cuenta ya existe, creada por la API (POST /api/v1/auth/register) con el correo de abajo,
-- zona horaria America/Lima y SIN participacion en el estudio. Este script NO crea usuarios, contrasenas
-- ni tokens; solo anade el perfil (lo mismo que deja POST /api/v1/users/me/profile).
DO $demo$
DECLARE
  v_email CONSTANT text := 'usuario.demo.kora@example.com';
  v_user bigint;
  v_estado text; v_zona text; v_participa boolean; v_retirado timestamptz;
  v_w3 date := date_trunc('week', now() AT TIME ZONE 'America/Lima')::date - 21;
  v_sql text;
  i int;
BEGIN
  IF NOT EXISTS (SELECT 1 FROM flyway_schema_history WHERE version = '65' AND success) THEN
    RAISE EXCEPTION 'DEMO 01: la base no tiene la migracion V65 aplicada; el dataset se preparo para ese esquema.';
  END IF;
  SELECT user_id, status, timezone, is_study_participant, withdrawn_at
    INTO v_user, v_estado, v_zona, v_participa, v_retirado
    FROM users WHERE lower(email) = lower(v_email);
  IF v_user IS NULL THEN
    RAISE EXCEPTION 'DEMO 01: no existe la cuenta %. Registrela antes por la API (ver README_EJECUCION.md).', v_email;
  END IF;
  IF v_estado <> 'ACTIVE' THEN
    RAISE EXCEPTION 'DEMO 01: la cuenta % no esta ACTIVE (estado %).', v_user, v_estado;
  END IF;
  IF v_zona <> 'America/Lima' THEN
    RAISE EXCEPTION 'DEMO 01: la cuenta tiene zona % y el dataset se calcula en America/Lima.', v_zona;
  END IF;
  IF v_participa AND v_retirado IS NULL THEN
    -- Un participante activo entra en el cierre semanal de los lunes, que generaria un plan nuevo (con IA
    -- si esta activada: coste en Replicate) y alteraria la demo.
    RAISE EXCEPTION 'DEMO 01: la cuenta participa en el estudio. Registrela con acceptsStudyParticipation=false.';
  END IF;
  IF EXISTS (SELECT 1 FROM user_profiles WHERE user_id = v_user) THEN
    IF EXISTS (SELECT 1 FROM user_profiles WHERE user_id = v_user AND goal_type = 'GAIN_MUSCLE'
               AND fitness_level = 'INTERMEDIATE' AND days_per_week = 5 AND session_minutes = 60
               AND training_location = 'GYM') THEN
      RAISE NOTICE 'DEMO 01: el perfil de la cuenta % ya existe y coincide. No se cambia nada.', v_user;
      RETURN;
    END IF;
    RAISE EXCEPTION 'DEMO 01: la cuenta % ya tiene un perfil distinto del de la demo. Use una cuenta nueva.', v_user;
  END IF;
  v_sql := $q$
INSERT INTO user_profiles (user_id, birth_date, biological_sex, height_cm, current_weight_kg, target_weight_kg, fitness_level, goal_type, goal_text, training_location, days_per_week, available_days, session_minutes, equipment_codes, blocked_exercise_ids, health_notes, created_at, updated_at, allow_high_impact, allow_floor_exercises, allow_kneeling_exercises, allow_hanging_exercises, allow_overhead_exercises, allow_suspended_support, suspended_support_confirmed_at)
VALUES
  (@@U@@, '1995-04-12'::date, 'MALE'::character varying(20), '176.00'::numeric(5,2), '74.00'::numeric(6,2), NULL, 'INTERMEDIATE'::character varying(20), 'GAIN_MUSCLE'::character varying(30), 'Ganar masa muscular (usuario de demostracion de Kora)'::character varying(500), 'GYM'::character varying(20), 5, '{1,2,3,4,5}'::smallint[], 60, '{"body weight",dumbbell,barbell,cable,"leverage machine",bench,band,"stationary bike",treadmill,kettlebell,"ez barbell","smith machine","pull-up bar"}'::character varying(40)[], '{}'::bigint[], 'Ninguna'::character varying(500), '@@D0@@ 00:00:00.000000+00'::timestamptz, '@@D0@@ 00:00:00.000000+00'::timestamptz, TRUE, TRUE, TRUE, TRUE, TRUE, NULL, NULL);
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
  RAISE NOTICE 'DEMO 01 aplicado: perfil de la cuenta %', v_user;
END
$demo$;
