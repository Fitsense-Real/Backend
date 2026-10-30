-- =====================================================================
-- FitSense - V42: falta de tiempo con contexto y CONSERVAR sin IA (30/10/2026)
--
-- Implementa las decisiones aprobadas de DISENO_ADAPTACION_CONTEXTUAL.md v0.3
-- (D1, D2, D6 y D7) que estaban pendientes, sin la restauracion automatica (§3),
-- sustituida por DISENO_PROGRESION_Y_REINCORPORACION.md:
--   * planned_workouts: respuesta opcional al omitir por falta de tiempo y los
--     minutos que la persona suele tener para ESE entrenamiento. Opcionales:
--     las omisiones existentes quedan con NULL, que se lee como "sin respuesta".
--   * plan_adaptation_attempts.targeted_interventions admite CONSERVAR.
--   * plan_adaptation_attempts.adaptation_rule_version: ADAPT-1 para las filas
--     existentes (la logica de A01) y ADAPT-2 para las nuevas.
-- No toca planes, sesiones ni metricas existentes.
-- =====================================================================

ALTER TABLE public.planned_workouts
    ADD COLUMN lack_of_time_context VARCHAR(30),
    ADD COLUMN declared_available_minutes SMALLINT;

ALTER TABLE public.planned_workouts
    ADD CONSTRAINT ck_pw_lack_of_time_context CHECK (lack_of_time_context IS NULL OR lack_of_time_context IN
        ('IMPREVISTO', 'SIN_TIEMPO_ESE_DIA', 'MENOS_TIEMPO_HABITUAL')),
    -- El contexto solo existe con una omision por falta de tiempo.
    ADD CONSTRAINT ck_pw_lack_of_time_coherente CHECK (lack_of_time_context IS NULL
        OR (status = 'SKIPPED' AND skip_reason = 'LACK_OF_TIME')),
    -- Los minutos, solo (y siempre) con MENOS_TIEMPO_HABITUAL, en el rango del perfil.
    ADD CONSTRAINT ck_pw_minutos_declarados CHECK (
        (lack_of_time_context = 'MENOS_TIEMPO_HABITUAL'
            AND declared_available_minutes BETWEEN 15 AND 180)
        OR (lack_of_time_context IS DISTINCT FROM 'MENOS_TIEMPO_HABITUAL'
            AND declared_available_minutes IS NULL));

ALTER TABLE public.plan_adaptation_attempts DROP CONSTRAINT ck_paa_intervenciones;
ALTER TABLE public.plan_adaptation_attempts
    ADD CONSTRAINT ck_paa_intervenciones CHECK (targeted_interventions <@ ARRAY[
        'REPROGRAMAR_DIAS', 'ACORTAR_O_REORGANIZAR', 'VARIANTE_MAS_SENCILLA_O_CARGA',
        'SEGURIDAD_ANTE_DOLOR', 'REVISAR_CARGA_Y_RECUPERACION',
        'INVESTIGAR_CAMBIO_MINIMO', 'CAUSA_NO_REGISTRADA', 'CONSERVAR']::VARCHAR(40)[]);

-- D7: las filas existentes son ADAPT-1; las nuevas toman ADAPT-2 por defecto.
ALTER TABLE public.plan_adaptation_attempts
    ADD COLUMN adaptation_rule_version VARCHAR(10) NOT NULL DEFAULT 'ADAPT-1'
        CONSTRAINT ck_paa_adaptation_rule_version CHECK (adaptation_rule_version IN ('ADAPT-1', 'ADAPT-2'));
ALTER TABLE public.plan_adaptation_attempts ALTER COLUMN adaptation_rule_version SET DEFAULT 'ADAPT-2';
