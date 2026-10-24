-- =====================================================================
-- FitSense · ADHERENCIA HISTORICA recalculada sin pesos
-- SOLO LECTURA. No modifica weekly_user_metrics, user_interventions ni los
-- registros de ejecucion.
--
--   psql "$URL_DE_LECTURA" -v ON_ERROR_STOP=1 -f adherencia_recalculada.sql
--
-- Hasta 11b5aae, weighted_adherence_pct y observed_adherence_pct se
-- ponderaban por planned_workouts.expected_duration_minutes, y las sesiones se
-- atribuian a la semana por started_at con corte a medianoche UTC. Esta
-- consulta rehace cada semana guardada con la definicion corregida:
--
--   A_w = sum(A_s / 100) / m x 100
--
-- con A_s = workout_sessions.completion_percentage de la sesion que cuenta
-- (topado a 100) y 0 si no la hay, y m = previstos no REPLACED de la semana,
-- uno por dia entre versiones (preferencia: con sesion que cuenta, cerrado,
-- version mas reciente), igual que WeekPlanInput.unoPorDia.
--
-- A_s no cambia: execution ya lo calculaba como el promedio sin pesos de los
-- ejercicios indicados. Lo unico que se rehace es la agregacion semanal.
--
-- Columnas:
--   duraciones_distintas  si la semana mezcla duraciones. Si es false y no hay
--                         sesiones fuera de su semana, la cifra no cambia.
--   diferencia_pp         recalculada - guardada, en puntos porcentuales.
-- =====================================================================

BEGIN TRANSACTION READ ONLY;

WITH previstos AS (
    SELECT pw.planned_workout_id, pw.scheduled_date, pw.status, pw.expected_duration_minutes,
           p.user_id, p.week_start_date, p.plan_version,
           ws.completion_percentage AS cumplimiento
    FROM planned_workouts pw
    JOIN weekly_training_plans p ON p.plan_id = pw.plan_id
    LEFT JOIN workout_sessions ws
           ON ws.planned_workout_id = pw.planned_workout_id AND ws.counts_toward_adherence
    WHERE pw.status <> 'REPLACED'
),
uno_por_dia AS (
    -- Dentro de una misma version no se descarta nada; entre versiones, uno por dia.
    SELECT *
    FROM (
        SELECT pr.*,
               rank() OVER (PARTITION BY user_id, week_start_date, scheduled_date
                            ORDER BY (cumplimiento IS NOT NULL) DESC,
                                     (status IN ('COMPLETED','PARTIAL','SKIPPED')) DESC,
                                     plan_version DESC) AS orden
        FROM previstos pr
    ) x
    WHERE orden = 1
),
semana AS (
    SELECT user_id, week_start_date,
           count(*)                                                  AS previstos,
           round(avg(LEAST(COALESCE(cumplimiento, 0), 100)), 2)      AS adherencia_sin_pesos,
           count(DISTINCT expected_duration_minutes) > 1             AS duraciones_distintas
    FROM uno_por_dia
    GROUP BY user_id, week_start_date
)
SELECT m.user_id, m.week_start_date, m.calculation_version,
       m.weighted_adherence_pct                       AS guardada,
       s.adherencia_sin_pesos                         AS recalculada,
       s.adherencia_sin_pesos - m.weighted_adherence_pct AS diferencia_pp,
       m.scheduled_workouts                           AS previstos_guardados,
       s.previstos                                    AS previstos_recalculados,
       s.duraciones_distintas
FROM weekly_user_metrics m
JOIN semana s ON s.user_id = m.user_id AND s.week_start_date = m.week_start_date
WHERE m.has_active_plan
ORDER BY abs(s.adherencia_sin_pesos - m.weighted_adherence_pct) DESC NULLS LAST,
         m.week_start_date, m.user_id;

-- Resumen: cuantas semanas cambian y cuanto.
WITH previstos AS (
    SELECT pw.planned_workout_id, pw.scheduled_date, pw.status,
           p.user_id, p.week_start_date, p.plan_version,
           ws.completion_percentage AS cumplimiento
    FROM planned_workouts pw
    JOIN weekly_training_plans p ON p.plan_id = pw.plan_id
    LEFT JOIN workout_sessions ws
           ON ws.planned_workout_id = pw.planned_workout_id AND ws.counts_toward_adherence
    WHERE pw.status <> 'REPLACED'
),
uno_por_dia AS (
    SELECT * FROM (
        SELECT pr.*, rank() OVER (PARTITION BY user_id, week_start_date, scheduled_date
                                  ORDER BY (cumplimiento IS NOT NULL) DESC,
                                           (status IN ('COMPLETED','PARTIAL','SKIPPED')) DESC,
                                           plan_version DESC) AS orden
        FROM previstos pr) x
    WHERE orden = 1
),
semana AS (
    SELECT user_id, week_start_date, round(avg(LEAST(COALESCE(cumplimiento, 0), 100)), 2) AS recalculada
    FROM uno_por_dia GROUP BY user_id, week_start_date
)
SELECT count(*)                                                        AS semanas_con_plan,
       count(*) FILTER (WHERE s.recalculada <> m.weighted_adherence_pct) AS semanas_que_cambian,
       max(abs(s.recalculada - m.weighted_adherence_pct))               AS mayor_diferencia_pp,
       count(DISTINCT m.user_id) FILTER (WHERE s.recalculada <> m.weighted_adherence_pct)
                                                                       AS participantes_afectados
FROM weekly_user_metrics m
JOIN semana s ON s.user_id = m.user_id AND s.week_start_date = m.week_start_date
WHERE m.has_active_plan;

ROLLBACK;
