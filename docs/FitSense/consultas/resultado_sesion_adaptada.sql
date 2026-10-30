-- =====================================================================
-- Resultado por sesion adaptada (DISENO_ADAPTACION_CONTEXTUAL.md v0.3, §4)
--
-- Pregunta: ¿la persona realizo la sesion concreta que FitSense modifico?
-- Unidad: cada sesion ADAPTADA PUBLICADA, es decir, la derivada (V31) cuyo
-- origen esta en applied_sessions del intento V32 que publico su plan.
-- Solo lectura: V32, el linaje V31, planned_workouts y workout_sessions.
-- No crea tablas ni columnas.
--
-- A_s = completion_percentage / 100 de la sesion que cuenta para la
-- adherencia (counts_toward_adherence). Clases, con los umbrales existentes
-- (0,80 completada; 0,30 valida):
--   COMPLETADA          A_s >= 0,80
--   PARCIAL_VALIDA      0,30 <= A_s < 0,80
--   PARCIAL_NO_VALIDA   0 < A_s < 0,30 (cuenta con su A_s real)
--   OMITIDA             omitida con causa, o sesion registrada con A_s = 0
--   SIN_INFORMACION     sin sesion y sin omision declarada (no es incumplimiento)
-- misma_causa: la derivada se omitio por la MISMA causa que la sesion de origen.
--
-- NO ES UNA PRUEBA CAUSAL: que una sesion adaptada se complete es un resultado
-- observado, no demuestra que la intervencion lo produjera (no hay grupo de control).
-- =====================================================================
WITH aplicadas AS (
    SELECT a.attempt_id, a.user_id, a.resulting_plan_id, o.origen,
           a.targeted_interventions[array_position(a.targeted_sessions, o.origen)] AS intervencion
      FROM plan_adaptation_attempts a
     CROSS JOIN LATERAL unnest(a.applied_sessions) AS o(origen)
     WHERE a.resulting_plan_id IS NOT NULL
), derivadas AS (
    SELECT ap.*, d.planned_workout_id AS derivada, d.scheduled_date, d.status AS estado,
           d.skip_reason, po.skip_reason AS causa_origen
      FROM aplicadas ap
      JOIN planned_workouts d ON d.plan_id = ap.resulting_plan_id AND d.source_planned_workout_id = ap.origen
      JOIN planned_workouts po ON po.planned_workout_id = ap.origen
), resultado AS (
    SELECT dv.*, ws.completion_percentage / 100.0 AS a_s
      FROM derivadas dv
      LEFT JOIN workout_sessions ws ON ws.planned_workout_id = dv.derivada AND ws.counts_toward_adherence
)
SELECT user_id, attempt_id, intervencion, origen, derivada, scheduled_date, a_s,
       CASE
           WHEN a_s IS NOT NULL THEN CASE
               WHEN a_s >= 0.80 THEN 'COMPLETADA'
               WHEN a_s >= 0.30 THEN 'PARCIAL_VALIDA'
               WHEN a_s > 0 THEN 'PARCIAL_NO_VALIDA'
               ELSE 'OMITIDA' END
           WHEN estado = 'SKIPPED' AND skip_reason IS NOT NULL THEN 'OMITIDA'
           ELSE 'SIN_INFORMACION'
       END AS resultado,
       (estado = 'SKIPPED' AND skip_reason IS NOT NULL AND skip_reason = causa_origen) AS misma_causa
  FROM resultado
 ORDER BY user_id, scheduled_date;

-- Indicadores (§4): n = sesiones adaptadas; n_r = con registro (resultado <> SIN_INFORMACION).
-- cumplimiento oficial  = sum(coalesce(a_s, 0)) / n * 100
-- cumplimiento observado = sum(a_s) / n_r * 100
-- tasa de finalizacion  = COMPLETADA / n_r * 100
-- tasa de realizacion   = (COMPLETADA + PARCIAL_VALIDA) / n_r * 100
-- repeticion            = OMITIDA con misma_causa / n_r * 100
-- cobertura             = n_r / n * 100
-- Con denominador 0, el indicador no es calculable (NULL).
