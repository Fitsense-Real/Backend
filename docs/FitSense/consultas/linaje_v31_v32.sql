-- =============================================================================
-- Integridad del linaje V31 + V32 (diseño v0.3, §3.2) · SOLO LECTURA
-- =============================================================================
-- Comprueba las premisas de la restauración: que cada derivación publicada
-- tiene su evidencia V32 y que cada paso del linaje se clasifica en un único
-- tipo. No escribe nada. Cada fila de la primera consulta es una violación:
-- sin filas, las premisas se cumplen sobre estos datos.
--
-- Tipos de paso (origen -> derivada), por el intento V32 cuyo
-- resulting_plan_id es el plan de la derivada:
--   ADAPTADA    origen en applied_sessions
--   SIN_EFECTO  origen en no_effect_sessions (contenido conservado)
--   SUSTITUIDA  origen en conserved_sessions con sustitucion de seguridad
--   COPIA       origen en conserved_sessions, sin sustitucion
-- =============================================================================

-- 1. VIOLACIONES -------------------------------------------------------------
WITH derivados AS (
    SELECT d.planned_workout_id AS derivada, d.plan_id AS plan_derivado,
           d.source_planned_workout_id AS origen, o.plan_id AS plan_origen
    FROM planned_workouts d
    JOIN planned_workouts o ON o.planned_workout_id = d.source_planned_workout_id
),
planes AS (
    SELECT DISTINCT plan_derivado, plan_origen FROM derivados
),
intentos AS (
    SELECT a.resulting_plan_id, count(*) AS n, min(a.source_plan_id) AS source_plan_id,
           count(DISTINCT a.source_plan_id) AS origenes
    FROM plan_adaptation_attempts a
    WHERE a.resulting_plan_id IS NOT NULL
    GROUP BY a.resulting_plan_id
),
clasificacion AS (
    SELECT d.derivada, d.origen,
           (d.origen = ANY (a.applied_sessions))::int
         + (d.origen = ANY (a.no_effect_sessions))::int
         + (d.origen = ANY (a.conserved_sessions))::int AS clases
    FROM derivados d
    JOIN plan_adaptation_attempts a ON a.resulting_plan_id = d.plan_derivado
)
-- Plan derivado sin exactamente un intento V32 que lo publique.
SELECT 'PLAN_DERIVADO_SIN_INTENTO_UNICO' AS violacion, p.plan_derivado AS id,
       coalesce(i.n, 0)::text AS detalle
FROM planes p LEFT JOIN intentos i ON i.resulting_plan_id = p.plan_derivado
WHERE coalesce(i.n, 0) <> 1
UNION ALL
-- Plan derivado cuyas sesiones vienen de mas de un plan de origen.
SELECT 'PLAN_DERIVADO_CON_VARIOS_ORIGENES', plan_derivado, count(DISTINCT plan_origen)::text
FROM planes GROUP BY plan_derivado HAVING count(DISTINCT plan_origen) > 1
UNION ALL
-- El intento dice venir de un plan distinto del de las sesiones de origen.
SELECT 'INTENTO_CON_ORIGEN_DISTINTO', p.plan_derivado,
       'intento=' || i.source_plan_id || ' linaje=' || p.plan_origen
FROM planes p JOIN intentos i ON i.resulting_plan_id = p.plan_derivado
WHERE i.origenes = 1 AND i.source_plan_id <> p.plan_origen
UNION ALL
-- Sesion derivada que no cae en exactamente una clase de V32.
SELECT 'SESION_SIN_CLASIFICACION_UNICA', c.derivada, 'origen=' || c.origen || ' clases=' || c.clases
FROM clasificacion c WHERE c.clases <> 1
UNION ALL
-- Plan publicado por un intento con alguna sesion sin origen V31.
SELECT 'SESION_SIN_ORIGEN_EN_PLAN_ADAPTADO', w.planned_workout_id, 'plan=' || w.plan_id
FROM planned_workouts w
JOIN plan_adaptation_attempts a ON a.resulting_plan_id = w.plan_id
WHERE w.source_planned_workout_id IS NULL
ORDER BY 1, 2;

-- 2. CLASIFICACION DE CADA PASO (evidencia) ---------------------------------
SELECT d.planned_workout_id AS derivada, d.source_planned_workout_id AS origen,
       d.plan_id AS plan_derivado, a.attempt_id, a.decision, a.status,
       CASE
           WHEN d.source_planned_workout_id = ANY (a.applied_sessions) THEN 'ADAPTADA:'
               || a.targeted_interventions[array_position(a.targeted_sessions, d.source_planned_workout_id)]
           WHEN d.source_planned_workout_id = ANY (a.no_effect_sessions) THEN 'SIN_EFECTO'
           WHEN d.source_planned_workout_id = ANY (a.conserved_sessions)
                AND EXISTS (SELECT 1 FROM jsonb_array_elements(coalesce(a.conservation_report -> 'sustituciones', '[]'::jsonb)) s
                            WHERE (coalesce(s ->> 'sesion_origen', s ->> 'sesionOrigen'))::bigint = d.source_planned_workout_id)
               THEN 'SUSTITUIDA'
           WHEN d.source_planned_workout_id = ANY (a.conserved_sessions) THEN 'COPIA'
           ELSE 'SIN_CLASIFICAR'
       END AS tipo
FROM planned_workouts d
LEFT JOIN plan_adaptation_attempts a ON a.resulting_plan_id = d.plan_id
WHERE d.source_planned_workout_id IS NOT NULL
ORDER BY d.plan_id, d.planned_workout_id;
