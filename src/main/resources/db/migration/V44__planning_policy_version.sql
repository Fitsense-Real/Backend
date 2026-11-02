-- V44 · VERSION DE LA POLITICA DE PLANIFICACION (implementacion controlada de C2-D y V4-C).
--
-- Solo esquema: dos columnas NULLABLE en weekly_training_plans. No toca datos ni el catalogo.
--   planning_policy        version con la que se genero y valido el plan. NULL = plan anterior a esta
--                          migracion, que se interpreta SIEMPRE como PLAN-HIST-1 (no se rellena retroactivamente).
--   planning_policy_trace  parametros efectivos: piso de V20, excepciones C2-D por sesion, segundos estimados,
--                          limite de V4, origen y contexto (generacion, adaptacion, P-03, reparacion).
-- La lista cerrada de versiones impide guardar una version desconocida; una version nueva exige otra migracion.
ALTER TABLE weekly_training_plans
    ADD COLUMN planning_policy VARCHAR(32) NULL,
    ADD COLUMN planning_policy_trace JSONB NULL;

ALTER TABLE weekly_training_plans
    ADD CONSTRAINT ck_weekly_training_plans_planning_policy
        CHECK (planning_policy IS NULL
            OR planning_policy IN ('PLAN-HIST-1', 'PLAN-C2D-0.1', 'PLAN-V4-0.1', 'PLAN-C2D-V4-0.1'));

COMMENT ON COLUMN weekly_training_plans.planning_policy IS
    'Version de la politica de planificacion (PlanningPolicy). NULL = PLAN-HIST-1 (planes anteriores a V44).';
COMMENT ON COLUMN weekly_training_plans.planning_policy_trace IS
    'Traza de la politica: parametros efectivos, piso V20, excepciones C2-D, segundos estimados, limite V4, contexto.';
