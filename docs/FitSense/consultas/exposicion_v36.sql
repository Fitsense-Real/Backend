-- =====================================================================
-- FitSense · V36 · planes publicados que contienen los nueve ejercicios
-- SOLO LECTURA. Se ejecuta ANTES de aplicar V36, con un usuario de lectura:
--
--   psql "$URL_DE_LECTURA" -v ON_ERROR_STOP=1 -f exposicion_v36.sql
--
-- La transaccion se abre READ ONLY y se cierra con ROLLBACK.
--
-- ESTADO: OPCIONAL. Los planes existentes son sinteticos y el responsable del
-- proyecto decidio no desarrollar compatibilidad retroactiva (orden de cierre de
-- V36). La consulta queda como herramienta de diagnostico.
--
-- Para que sirve: la copia semanal (V31) de una semana publicada antes de V36
-- se valida con el catalogo corregido. CatalogoV36IT reproduce que la copia NO
-- es factible, y la semana nueva queda sin plan, cuando la semana medida
-- contiene 1267 (inactivo) o un combinado (527, 609, 725, 760) en una sesion
-- PULL. 1296 no cambia en V36. Esta
-- consulta cuenta cuantos participantes reales estan en cada caso. No cuenta
-- la politica por edad (SafetyProfile), que solo aplica el backend.
-- =====================================================================

BEGIN TRANSACTION READ ONLY;

-- 1. Por caso: planes ACTIVOS (los que el proximo ciclo copiara) y participantes.
WITH prescripcion AS (
SELECT p.user_id, p.plan_id, p.week_start_date, pw.focus_code, pwe.exercise_id, up.fitness_level
  FROM planned_workout_exercises pwe
  JOIN planned_workouts pw ON pw.planned_workout_id = pwe.planned_workout_id
  JOIN weekly_training_plans p ON p.plan_id = pw.plan_id
  JOIN user_profiles up ON up.user_id = p.user_id
 WHERE p.status = 'ACTIVE'
   AND pwe.exercise_id IN (527, 609, 725, 760, 872, 1059, 1267, 1333)
), caso AS (
SELECT user_id, plan_id, week_start_date,
       CASE
           WHEN exercise_id = 1267 THEN 'A 1267 inactivo: copia NO factible'
           WHEN exercise_id IN (527, 609, 725, 760) AND focus_code = 'PULL'
               THEN 'B combinado en PULL: copia NO factible'
           WHEN exercise_id = 1333 AND fitness_level = 'BEGINNER'
               THEN 'E 1333 principiante: SUST-SEG-1 por alto impacto'
           WHEN exercise_id IN (872, 1059, 527, 609, 725, 760)
               THEN 'F reclasificado: revisar la dosis (V24) y el reparto (V15) en el detalle 2b'
           ELSE 'G 1333 en intermedio o avanzado: copia esperada factible'
       END AS caso
  FROM prescripcion
)
SELECT caso,
       count(DISTINCT user_id) AS participantes,
       count(DISTINCT plan_id) AS planes_activos,
       count(*)                AS prescripciones,
       min(week_start_date)    AS primera_semana,
       max(week_start_date)    AS ultima_semana
  FROM caso
 GROUP BY caso
 ORDER BY caso;

-- 2. Detalle de los casos A y B (los que hoy dejarian la semana sin plan):
--    participante, semana y sesion. Solo identificadores internos.
SELECT p.user_id, p.plan_id, p.week_start_date, pw.scheduled_date, pw.focus_code, pwe.exercise_id,
       up.fitness_level
  FROM planned_workout_exercises pwe
  JOIN planned_workouts pw ON pw.planned_workout_id = pwe.planned_workout_id
  JOIN weekly_training_plans p ON p.plan_id = pw.plan_id
  JOIN user_profiles up ON up.user_id = p.user_id
 WHERE p.status = 'ACTIVE'
   AND (pwe.exercise_id = 1267
        OR (pwe.exercise_id IN (527, 609, 725, 760) AND pw.focus_code = 'PULL'))
 ORDER BY p.user_id, p.week_start_date, pw.scheduled_date;

-- 2b. Reclasificados en planes ACTIVOS, con su dosis. La copia falla por V24 si
--     la dosis publicada no es la de su nueva clase: el plan 60 llevaba 872 como
--     estructural a 3x13 y con V36 se le exige 2x13 (relleno). Y por V15 si era el
--     unico ejercicio de su grupo en la sesion (1059 contaba como waist).
SELECT p.user_id, p.plan_id, p.week_start_date, pw.scheduled_date, pw.focus_code, pwe.exercise_id,
       pwe.prescription_type, pwe.planned_sets, pwe.planned_reps, pwe.rest_seconds, up.goal_type, up.fitness_level
  FROM planned_workout_exercises pwe
  JOIN planned_workouts pw ON pw.planned_workout_id = pwe.planned_workout_id
  JOIN weekly_training_plans p ON p.plan_id = pw.plan_id
  JOIN user_profiles up ON up.user_id = p.user_id
 WHERE p.status = 'ACTIVE'
   AND pwe.exercise_id IN (872, 1059, 527, 609, 725, 760)
   AND NOT (pwe.exercise_id IN (527, 609, 725, 760) AND pw.focus_code = 'PULL')
 ORDER BY p.user_id, p.week_start_date, pw.scheduled_date;

-- 3. Historico (planes COMPLETED o REPLACED): solo trazabilidad, no se repara.
SELECT pwe.exercise_id, count(*) AS prescripciones, count(DISTINCT p.user_id) AS participantes
  FROM planned_workout_exercises pwe
  JOIN planned_workouts pw ON pw.planned_workout_id = pwe.planned_workout_id
  JOIN weekly_training_plans p ON p.plan_id = pw.plan_id
 WHERE p.status <> 'ACTIVE'
   AND pwe.exercise_id IN (527, 609, 725, 760, 872, 1059, 1267, 1333)
 GROUP BY pwe.exercise_id
 ORDER BY pwe.exercise_id;

ROLLBACK;
