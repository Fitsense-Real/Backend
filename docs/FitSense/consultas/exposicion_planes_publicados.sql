-- =====================================================================
-- FitSense · EXPOSICION REAL de planes publicados a ejercicios inseguros
-- SOLO LECTURA. Pensado para produccion con un usuario de lectura.
--
--   psql "$URL_DE_LECTURA" -v ON_ERROR_STOP=1 -f exposicion_planes_publicados.sql
--
-- La transaccion se abre READ ONLY y se cierra con ROLLBACK: aunque alguien
-- anadiera una escritura por error, PostgreSQL la rechaza.
--
-- Criterios:
--   * Autorizaciones con IS DISTINCT FROM TRUE: un NULL cuenta como NO
--     autorizado (conservador). Hoy las columnas allow_* son NOT NULL DEFAULT
--     TRUE (V25), asi que coincide con la regla de la aplicacion; si algun dia
--     admitieran NULL, esta medicion seguiria siendo la prudente.
--   * Codigos de equipamiento normalizados: lower(btrim(...)), como hace
--     ExternalCatalogService.admite1374.
--   * Se separa ACTIVO O FUTURO (sesiones abiertas de planes activos, desde
--     hoy) de HISTORICO (todo lo demas). El historico se cuenta para
--     trazabilidad; no se repara.
--   * Se cuentan PARTICIPANTES DISTINTOS. Las 32 combinaciones de perfil
--     simuladas no son 32 personas.
--   * La politica por edad y nivel (SafetyProfile.forAgeAndLevel) NO esta en
--     SQL: la bandera 6 solo cubre lo DECLARADO. La comprobacion completa la
--     hace el backend al leer el plan (PublishedPlanSafetyService).
-- =====================================================================

BEGIN TRANSACTION READ ONLY;

-- Las dos CTE que usan todas las consultas (en READ ONLY no se pueden crear
-- vistas temporales): perfil_norm, el perfil normalizado; prescripcion, cada
-- prescripcion clasificada como ACTIVO_O_FUTURO o HISTORICO.

-- 1. INC-CAT-1374: resumen por alcance. "incompatibles" = sin barra, o sin
--    permiso para colgarse o para brazos arriba.
WITH perfil_norm AS (
SELECT up.user_id,
       (up.allow_hanging_exercises  IS DISTINCT FROM TRUE) AS no_colgarse,
       (up.allow_overhead_exercises IS DISTINCT FROM TRUE) AS no_brazos_arriba,
       (up.allow_floor_exercises    IS DISTINCT FROM TRUE) AS no_suelo,
       (up.allow_kneeling_exercises IS DISTINCT FROM TRUE) AS no_rodillas,
       (up.allow_high_impact        IS DISTINCT FROM TRUE) AS no_impacto,
       EXISTS (SELECT 1 FROM unnest(up.equipment_codes) c
                WHERE lower(btrim(c)) = 'pull-up bar')     AS tiene_barra
  FROM user_profiles up
), prescripcion AS (
SELECT p.user_id, p.plan_id, p.plan_version, p.status AS plan_status, p.week_start_date,
       pw.planned_workout_id, pw.scheduled_date, pw.status AS workout_status,
       pwe.planned_exercise_id, pwe.exercise_id,
       CASE WHEN p.status = 'ACTIVE'
                 AND pw.status IN ('SCHEDULED', 'IN_PROGRESS')
                 AND pw.scheduled_date >= current_date
            THEN 'ACTIVO_O_FUTURO' ELSE 'HISTORICO' END AS alcance
  FROM planned_workout_exercises pwe
  JOIN planned_workouts pw ON pw.planned_workout_id = pwe.planned_workout_id
  JOIN weekly_training_plans p ON p.plan_id = pw.plan_id
)
SELECT r.alcance,
       count(*)                                                    AS prescripciones_1374,
       count(*) FILTER (WHERE NOT n.tiene_barra OR n.no_colgarse OR n.no_brazos_arriba)
                                                                   AS prescripciones_incompatibles,
       count(DISTINCT r.user_id)                                   AS participantes_con_1374,
       count(DISTINCT r.user_id) FILTER (WHERE NOT n.tiene_barra OR n.no_colgarse OR n.no_brazos_arriba)
                                                                   AS participantes_incompatibles,
       count(DISTINCT r.planned_workout_id) FILTER (WHERE NOT n.tiene_barra OR n.no_colgarse OR n.no_brazos_arriba)
                                                                   AS sesiones_incompatibles,
       count(DISTINCT r.plan_id) FILTER (WHERE NOT n.tiene_barra OR n.no_colgarse OR n.no_brazos_arriba)
                                                                   AS planes_incompatibles
  FROM prescripcion r
  JOIN perfil_norm n ON n.user_id = r.user_id
 WHERE r.exercise_id = 1374
 GROUP BY r.alcance
 ORDER BY r.alcance;

-- 2. INC-CAT-1374: detalle de lo ACTIVO O FUTURO incompatible, con el motivo.
WITH perfil_norm AS (
SELECT up.user_id,
       (up.allow_hanging_exercises  IS DISTINCT FROM TRUE) AS no_colgarse,
       (up.allow_overhead_exercises IS DISTINCT FROM TRUE) AS no_brazos_arriba,
       (up.allow_floor_exercises    IS DISTINCT FROM TRUE) AS no_suelo,
       (up.allow_kneeling_exercises IS DISTINCT FROM TRUE) AS no_rodillas,
       (up.allow_high_impact        IS DISTINCT FROM TRUE) AS no_impacto,
       EXISTS (SELECT 1 FROM unnest(up.equipment_codes) c
                WHERE lower(btrim(c)) = 'pull-up bar')     AS tiene_barra
  FROM user_profiles up
), prescripcion AS (
SELECT p.user_id, p.plan_id, p.plan_version, p.status AS plan_status, p.week_start_date,
       pw.planned_workout_id, pw.scheduled_date, pw.status AS workout_status,
       pwe.planned_exercise_id, pwe.exercise_id,
       CASE WHEN p.status = 'ACTIVE'
                 AND pw.status IN ('SCHEDULED', 'IN_PROGRESS')
                 AND pw.scheduled_date >= current_date
            THEN 'ACTIVO_O_FUTURO' ELSE 'HISTORICO' END AS alcance
  FROM planned_workout_exercises pwe
  JOIN planned_workouts pw ON pw.planned_workout_id = pwe.planned_workout_id
  JOIN weekly_training_plans p ON p.plan_id = pw.plan_id
)
SELECT r.user_id, r.plan_id, r.plan_version, r.week_start_date, r.scheduled_date, r.workout_status,
       r.planned_workout_id, r.planned_exercise_id,
       concat_ws('+', CASE WHEN NOT n.tiene_barra THEN 'sin_barra' END,
                      CASE WHEN n.no_colgarse THEN 'no_colgarse' END,
                      CASE WHEN n.no_brazos_arriba THEN 'no_brazos_arriba' END) AS motivo,
       -- Si la semana ya tiene sesiones terminadas, la reparacion por version
       -- no aplica todavia (SEMANA_EMPEZADA): solo el bloqueo.
       EXISTS (SELECT 1 FROM planned_workouts t WHERE t.plan_id = r.plan_id
                AND t.status IN ('COMPLETED', 'PARTIAL', 'SKIPPED'))       AS semana_empezada,
       EXISTS (SELECT 1 FROM planned_workouts t WHERE t.plan_id = r.plan_id
                AND t.status = 'IN_PROGRESS')                              AS sesion_en_curso
  FROM prescripcion r
  JOIN perfil_norm n ON n.user_id = r.user_id
 WHERE r.exercise_id = 1374 AND r.alcance = 'ACTIVO_O_FUTURO'
   AND (NOT n.tiene_barra OR n.no_colgarse OR n.no_brazos_arriba)
 ORDER BY r.scheduled_date, r.user_id;

-- 3. INC-CAT-1374: si se EJECUTO alguna vez (sesiones registradas).
SELECT count(DISTINCT s.session_id) AS sesiones_con_1374, count(DISTINCT s.user_id) AS participantes
  FROM workout_sessions s
  JOIN planned_workout_exercises pwe ON pwe.planned_workout_id = s.planned_workout_id
 WHERE pwe.exercise_id = 1374;

-- 4. RETENIDOS INC-4C-03 (813, 858, 861, 873, 885, 1192, 1298) prescritos.
WITH perfil_norm AS (
SELECT up.user_id,
       (up.allow_hanging_exercises  IS DISTINCT FROM TRUE) AS no_colgarse,
       (up.allow_overhead_exercises IS DISTINCT FROM TRUE) AS no_brazos_arriba,
       (up.allow_floor_exercises    IS DISTINCT FROM TRUE) AS no_suelo,
       (up.allow_kneeling_exercises IS DISTINCT FROM TRUE) AS no_rodillas,
       (up.allow_high_impact        IS DISTINCT FROM TRUE) AS no_impacto,
       EXISTS (SELECT 1 FROM unnest(up.equipment_codes) c
                WHERE lower(btrim(c)) = 'pull-up bar')     AS tiene_barra
  FROM user_profiles up
), prescripcion AS (
SELECT p.user_id, p.plan_id, p.plan_version, p.status AS plan_status, p.week_start_date,
       pw.planned_workout_id, pw.scheduled_date, pw.status AS workout_status,
       pwe.planned_exercise_id, pwe.exercise_id,
       CASE WHEN p.status = 'ACTIVE'
                 AND pw.status IN ('SCHEDULED', 'IN_PROGRESS')
                 AND pw.scheduled_date >= current_date
            THEN 'ACTIVO_O_FUTURO' ELSE 'HISTORICO' END AS alcance
  FROM planned_workout_exercises pwe
  JOIN planned_workouts pw ON pw.planned_workout_id = pwe.planned_workout_id
  JOIN weekly_training_plans p ON p.plan_id = pw.plan_id
)
SELECT r.alcance, count(*) AS prescripciones, count(DISTINCT r.user_id) AS participantes,
       array_agg(DISTINCT r.exercise_id ORDER BY r.exercise_id) AS ejercicios
  FROM prescripcion r
 WHERE r.exercise_id IN (813, 858, 861, 873, 885, 1192, 1298)
 GROUP BY r.alcance
 ORDER BY r.alcance;

-- 5. Restricciones DECLARADAS contra las banderas del catalogo, en lo ACTIVO O
--    FUTURO (cualquier ejercicio, no solo el 1374).
WITH perfil_norm AS (
SELECT up.user_id,
       (up.allow_hanging_exercises  IS DISTINCT FROM TRUE) AS no_colgarse,
       (up.allow_overhead_exercises IS DISTINCT FROM TRUE) AS no_brazos_arriba,
       (up.allow_floor_exercises    IS DISTINCT FROM TRUE) AS no_suelo,
       (up.allow_kneeling_exercises IS DISTINCT FROM TRUE) AS no_rodillas,
       (up.allow_high_impact        IS DISTINCT FROM TRUE) AS no_impacto,
       EXISTS (SELECT 1 FROM unnest(up.equipment_codes) c
                WHERE lower(btrim(c)) = 'pull-up bar')     AS tiene_barra
  FROM user_profiles up
), prescripcion AS (
SELECT p.user_id, p.plan_id, p.plan_version, p.status AS plan_status, p.week_start_date,
       pw.planned_workout_id, pw.scheduled_date, pw.status AS workout_status,
       pwe.planned_exercise_id, pwe.exercise_id,
       CASE WHEN p.status = 'ACTIVE'
                 AND pw.status IN ('SCHEDULED', 'IN_PROGRESS')
                 AND pw.scheduled_date >= current_date
            THEN 'ACTIVO_O_FUTURO' ELSE 'HISTORICO' END AS alcance
  FROM planned_workout_exercises pwe
  JOIN planned_workouts pw ON pw.planned_workout_id = pwe.planned_workout_id
  JOIN weekly_training_plans p ON p.plan_id = pw.plan_id
)
SELECT x.exercise_id, count(*) AS prescripciones, count(DISTINCT r.user_id) AS participantes,
       bool_or(n.no_suelo AND x.requires_floor)              AS por_suelo,
       bool_or(n.no_rodillas AND x.requires_kneeling)        AS por_rodillas,
       bool_or(n.no_colgarse AND x.requires_hanging)         AS por_suspension,
       bool_or(n.no_brazos_arriba AND x.requires_overhead)   AS por_brazos_arriba,
       bool_or(n.no_impacto AND x.high_impact)               AS por_impacto
  FROM prescripcion r
  JOIN perfil_norm n ON n.user_id = r.user_id
  JOIN exercises x ON x.exercise_id = r.exercise_id
 WHERE r.alcance = 'ACTIVO_O_FUTURO'
   AND (   (n.no_suelo AND x.requires_floor)
        OR (n.no_rodillas AND x.requires_kneeling)
        OR (n.no_colgarse AND x.requires_hanging)
        OR (n.no_brazos_arriba AND x.requires_overhead)
        OR (n.no_impacto AND x.high_impact))
 GROUP BY x.exercise_id
 ORDER BY participantes DESC, x.exercise_id;

-- 6. Integridad: nunca dos versiones activas de la misma semana.
SELECT user_id, week_start_date, count(*) AS activas
  FROM weekly_training_plans
 WHERE status = 'ACTIVE'
 GROUP BY user_id, week_start_date
HAVING count(*) > 1;

ROLLBACK;
