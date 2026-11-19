-- V44.3 · Versiones de politica del rediseno controlado tras la auditoria R/R' (05/11/2026).
--
-- Solo amplia la lista cerrada de versiones admitidas en weekly_training_plans.planning_policy. No toca datos,
-- ni el catalogo, ni la version por omision (sigue siendo PLAN-HIST-1, fijada por configuracion):
--   PLAN-DUR-A-0.1  diseno A de duracion (P-G1), candidata, para comparar
--   PLAN-DUR-B-0.1  diseno B de duracion (P-G1), candidata, para comparar
--   PLAN-REV-2.0    version integrada del rediseno, candidata (requiere allow-candidates)
ALTER TABLE weekly_training_plans DROP CONSTRAINT ck_weekly_training_plans_planning_policy;

ALTER TABLE weekly_training_plans
    ADD CONSTRAINT ck_weekly_training_plans_planning_policy
        CHECK (planning_policy IS NULL
            OR planning_policy IN ('PLAN-HIST-1', 'PLAN-C2D-0.1', 'PLAN-V4-0.1', 'PLAN-C2D-V4-0.1',
                                   'PLAN-DUR-A-0.1', 'PLAN-DUR-B-0.1', 'PLAN-REV-2.0'));
