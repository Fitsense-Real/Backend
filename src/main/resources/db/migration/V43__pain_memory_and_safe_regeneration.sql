-- =====================================================================
-- FitSense - V43: memoria persistente del dolor (P-02) y regeneracion
-- segura en el mismo ciclo (P-03). Politicas aprobadas el 31/10/2026.
--
-- * user_safety_exclusions: un ejercicio que provoco dolor queda excluido
--   para ESE usuario hasta una reactivacion explicita. No caduca. Es
--   independiente del perfil congelado: no toca blocked_exercise_ids.
--   Una fila por (usuario, ejercicio, sesion de origen): el evento original
--   es la identidad, asi que releer la misma semana no duplica ni resucita
--   una exclusion ya reactivada.
-- * safe_regeneration_attempts: la traza de P-03. Una fila por medicion
--   semanal (weekly_metric_id UNIQUE), que es tambien la llave de
--   idempotencia: repetir el ciclo no regenera dos veces.
-- Solo crea tablas nuevas. No modifica datos existentes.
-- =====================================================================

CREATE TABLE public.user_safety_exclusions (
    exclusion_id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id                    BIGINT       NOT NULL REFERENCES public.users (user_id),
    exercise_id                BIGINT       NOT NULL,
    -- DOLOR_IDENTIFICADO: la persona marco ESE ejercicio con dolor.
    -- DOLOR_SESION_SIN_IDENTIFICAR: dolor de sesion sin decir cual; se excluyen
    -- provisionalmente todos los de la sesion (la misma regla que V8C).
    reason                     VARCHAR(40)  NOT NULL,
    source_plan_id             BIGINT       NOT NULL,
    source_planned_workout_id  BIGINT       NOT NULL,
    source_planned_exercise_id BIGINT,
    event_date                 DATE         NOT NULL,
    -- Por donde llego el registro: al omitir la sesion o al cerrar el ciclo.
    recorded_by                VARCHAR(30)  NOT NULL,
    recorded_at                TIMESTAMPTZ  NOT NULL DEFAULT now(),
    status                     VARCHAR(20)  NOT NULL DEFAULT 'ACTIVA',
    reactivated_at             TIMESTAMPTZ,
    reactivation_reason        VARCHAR(300),
    reactivation_evidence      VARCHAR(500),
    CONSTRAINT ck_use_reason CHECK (reason IN ('DOLOR_IDENTIFICADO', 'DOLOR_SESION_SIN_IDENTIFICAR')),
    CONSTRAINT ck_use_recorded_by CHECK (recorded_by IN ('OMISION', 'CICLO_SEMANAL', 'SESION')),
    CONSTRAINT ck_use_status CHECK (status IN ('ACTIVA', 'REACTIVADA')),
    -- Reactivar exige constancia de la accion explicita; activa, ninguna.
    CONSTRAINT ck_use_reactivacion CHECK (
        (status = 'ACTIVA' AND reactivated_at IS NULL AND reactivation_evidence IS NULL)
        OR (status = 'REACTIVADA' AND reactivated_at IS NOT NULL AND reactivation_evidence IS NOT NULL)),
    CONSTRAINT ux_use_evento UNIQUE (user_id, exercise_id, source_planned_workout_id)
);

CREATE INDEX ix_use_user_activas ON public.user_safety_exclusions (user_id) WHERE status = 'ACTIVA';

CREATE TABLE public.safe_regeneration_attempts (
    regeneration_id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id                  BIGINT       NOT NULL REFERENCES public.users (user_id),
    weekly_metric_id         BIGINT       NOT NULL REFERENCES public.weekly_user_metrics (weekly_metric_id),
    measured_plan_id         BIGINT       NOT NULL,
    new_week_start_date      DATE         NOT NULL,
    -- El intento V32 NO_FACTIBLE que la origina.
    adaptation_attempt_id    BIGINT       REFERENCES public.plan_adaptation_attempts (attempt_id),
    adaptation_rejection     TEXT,
    excluded_exercise_ids    BIGINT[]     NOT NULL,
    -- Siempre determinista: P-05 no autoriza llamadas pagadas aqui.
    source                   VARCHAR(20)  NOT NULL DEFAULT 'RULES',
    outcome                  VARCHAR(20)  NOT NULL,
    resulting_plan_id        BIGINT       REFERENCES public.weekly_training_plans (plan_id),
    validation_problems      TEXT,
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_sra_source CHECK (source = 'RULES'),
    CONSTRAINT ck_sra_outcome CHECK (outcome IN ('PUBLICADO', 'NO_FACTIBLE')),
    CONSTRAINT ck_sra_plan CHECK ((outcome = 'PUBLICADO') = (resulting_plan_id IS NOT NULL)),
    CONSTRAINT ux_sra_medicion UNIQUE (weekly_metric_id)
);
