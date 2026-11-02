-- V45 · versiones de politica con elegibilidad funcional (EF-0.1). Solo amplia la lista cerrada de V44: ningun dato
-- cambia y las versiones existentes conservan su significado.
ALTER TABLE weekly_training_plans DROP CONSTRAINT ck_weekly_training_plans_planning_policy;
ALTER TABLE weekly_training_plans
    ADD CONSTRAINT ck_weekly_training_plans_planning_policy
        CHECK (planning_policy IS NULL
            OR planning_policy IN ('PLAN-HIST-1', 'PLAN-C2D-0.1', 'PLAN-V4-0.1', 'PLAN-C2D-V4-0.1',
                                   'PLAN-EF-0.1', 'PLAN-C2D-EF-0.1', 'PLAN-C2D-V4-EF-0.1'));
