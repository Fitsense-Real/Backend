-- =====================================================================================================
-- DATASET DEMOSTRATIVO FITSENSE / KORA · 07 · Eliminar la demo
-- Borra SOLO los datos de la cuenta demo (usuario.demo.kora@example.com): perfil, planes, sesiones, metricas,
-- adaptaciones, intervenciones, exclusiones y avisos generados para ella, incluidas las sesiones que se hagan en
-- directo durante la presentacion. No toca ninguna otra cuenta, el catalogo ni el esquema.
--
-- Guardas: (1) la cuenta se busca por su correo exacto; (2) solo se borra si esa cuenta es la duena del plan
-- marcador 910000000 del dataset (una cuenta real con ese correo, sin el dataset, no se toca); (3) un solo
-- bloque DO: si algo falla, no se borra nada.
--
-- v_eliminar_cuenta = false (por omision): se conserva la cuenta (usuario, consentimientos, dispositivos y
-- preferencias), de modo que se puede volver a cargar 01-05. Con true se borra tambien la cuenta.
-- =====================================================================================================
DO $demo$
DECLARE
  v_email CONSTANT text := 'usuario.demo.kora@example.com';
  v_eliminar_cuenta CONSTANT boolean := false;
  v_user bigint;
  v_n bigint;
  v_resumen text := '';
BEGIN
  SELECT user_id INTO v_user FROM users WHERE lower(email) = lower(v_email);
  IF v_user IS NULL THEN
    RAISE NOTICE 'DEMO 07: no existe la cuenta %. No hay nada que borrar.', v_email;
    RETURN;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM weekly_training_plans WHERE plan_id = 910000000 AND user_id = v_user) THEN
    IF EXISTS (SELECT 1 FROM weekly_training_plans WHERE user_id = v_user)
       OR EXISTS (SELECT 1 FROM workout_sessions WHERE user_id = v_user) THEN
      RAISE EXCEPTION 'DEMO 07: la cuenta % tiene datos pero no el dataset demo (plan 910000000). No se borra nada.', v_user;
    END IF;
    IF NOT v_eliminar_cuenta THEN
      -- Solo pudo quedar el perfil de 01 (sin 02): se borra si es el de la demo.
      DELETE FROM user_profiles WHERE user_id = v_user AND goal_type = 'GAIN_MUSCLE'
        AND goal_text = 'Ganar masa muscular (usuario de demostracion de Kora)';
      GET DIAGNOSTICS v_n = ROW_COUNT;
      RAISE NOTICE 'DEMO 07: la cuenta % no tiene el dataset cargado; perfil demo borrado: %.', v_user, v_n;
      RETURN;
    END IF;
  END IF;

  -- Avisos generados para la cuenta (si el envio push esta activo en el entorno).
  DELETE FROM notification_deliveries WHERE user_id = v_user;
  GET DIAGNOSTICS v_n = ROW_COUNT; v_resumen := v_resumen || 'notification_deliveries=' || v_n;
  DELETE FROM notification_events WHERE user_id = v_user;
  GET DIAGNOSTICS v_n = ROW_COUNT; v_resumen := v_resumen || ', notification_events=' || v_n;

  -- Trazabilidad de la adaptacion (antes que metricas y planes, que la referencian).
  DELETE FROM user_interventions WHERE user_id = v_user;
  GET DIAGNOSTICS v_n = ROW_COUNT; v_resumen := v_resumen || ', user_interventions=' || v_n;
  DELETE FROM safe_regeneration_attempts WHERE user_id = v_user;
  GET DIAGNOSTICS v_n = ROW_COUNT; v_resumen := v_resumen || ', safe_regeneration_attempts=' || v_n;
  DELETE FROM plan_adaptation_attempts WHERE user_id = v_user;
  GET DIAGNOSTICS v_n = ROW_COUNT; v_resumen := v_resumen || ', plan_adaptation_attempts=' || v_n;
  DELETE FROM user_safety_exclusions WHERE user_id = v_user;
  GET DIAGNOSTICS v_n = ROW_COUNT; v_resumen := v_resumen || ', user_safety_exclusions=' || v_n;

  -- Ejecucion.
  DELETE FROM workout_session_exercises
   WHERE session_id IN (SELECT session_id FROM workout_sessions WHERE user_id = v_user);
  GET DIAGNOSTICS v_n = ROW_COUNT; v_resumen := v_resumen || ', workout_session_exercises=' || v_n;
  DELETE FROM workout_sessions WHERE user_id = v_user;
  GET DIAGNOSTICS v_n = ROW_COUNT; v_resumen := v_resumen || ', workout_sessions=' || v_n;
  DELETE FROM weekly_user_metrics WHERE user_id = v_user;
  GET DIAGNOSTICS v_n = ROW_COUNT; v_resumen := v_resumen || ', weekly_user_metrics=' || v_n;

  -- Generacion (solo si alguien la pidio desde la cuenta durante la demo).
  DELETE FROM plan_generation_predictions WHERE user_id = v_user;
  GET DIAGNOSTICS v_n = ROW_COUNT; v_resumen := v_resumen || ', plan_generation_predictions=' || v_n;
  DELETE FROM plan_generation_operations WHERE user_id = v_user;
  GET DIAGNOSTICS v_n = ROW_COUNT; v_resumen := v_resumen || ', plan_generation_operations=' || v_n;

  -- Planes (un solo DELETE por tabla: las referencias internas entre filas borradas no bloquean).
  DELETE FROM planned_workout_exercises
   WHERE planned_workout_id IN (SELECT pw.planned_workout_id FROM planned_workouts pw
                                  JOIN weekly_training_plans p ON p.plan_id = pw.plan_id WHERE p.user_id = v_user);
  GET DIAGNOSTICS v_n = ROW_COUNT; v_resumen := v_resumen || ', planned_workout_exercises=' || v_n;
  DELETE FROM planned_workouts
   WHERE plan_id IN (SELECT plan_id FROM weekly_training_plans WHERE user_id = v_user);
  GET DIAGNOSTICS v_n = ROW_COUNT; v_resumen := v_resumen || ', planned_workouts=' || v_n;
  DELETE FROM weekly_training_plans WHERE user_id = v_user;
  GET DIAGNOSTICS v_n = ROW_COUNT; v_resumen := v_resumen || ', weekly_training_plans=' || v_n;

  DELETE FROM user_profiles WHERE user_id = v_user;
  GET DIAGNOSTICS v_n = ROW_COUNT; v_resumen := v_resumen || ', user_profiles=' || v_n;

  IF v_eliminar_cuenta THEN
    DELETE FROM push_devices WHERE user_id = v_user;
    DELETE FROM notification_preferences WHERE user_id = v_user;
    DELETE FROM password_reset_tokens WHERE user_id = v_user;
    DELETE FROM account_deletion_tokens WHERE user_id = v_user;
    DELETE FROM user_consents WHERE user_id = v_user;
    DELETE FROM users WHERE user_id = v_user;
    v_resumen := v_resumen || ', cuenta=borrada';
  END IF;

  RAISE NOTICE 'DEMO 07: datos de la cuenta % borrados (%).', v_user, v_resumen;
END
$demo$;
