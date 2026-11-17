-- =====================================================================
-- FitSense — V58 · GENERACION INICIAL ASINCRONA
--
-- MOTIVO
--
-- POST /users/me/plan/generate genera el primer plan dentro de la peticion
-- HTTP: con IA tarda mas de un minuto (hasta tres intentos de ~90 s mas el
-- motor de reglas). La app no puede aprovechar ese tiempo y, si la conexion
-- se corta, no sabe si el plan se publico.
--
-- Esta tabla registra cada solicitud como una OPERACION persistente que un
-- worker del backend ejecuta con el mismo pipeline de siempre (preparar,
-- generar, validar, publicar). La app guarda el operation_id y consulta el
-- estado; la generacion sigue aunque la app se cierre.
--
-- LO QUE NO CAMBIA
--
-- Ni el generador, ni los validadores, ni las politicas deportivas, ni el
-- catalogo, ni la formula de adherencia. El plan solo se publica si es
-- valido, en la misma transaccion corta de siempre (persist).
--
-- IDEMPOTENCIA
--
-- ux_pgo_una_activa_por_usuario: como mucho UNA operacion PENDING o
-- PROCESSING por usuario. Pulsar dos veces, o que la app reintente una
-- peticion cuya respuesta se perdio, devuelve la misma operacion: no hay
-- una segunda llamada a la IA.
-- =====================================================================

CREATE TABLE plan_generation_operations (
    operation_id     UUID         PRIMARY KEY,
    user_id          BIGINT       NOT NULL REFERENCES users (user_id),
    kind             VARCHAR(20)  NOT NULL DEFAULT 'FIRST_PLAN',
    status           VARCHAR(20)  NOT NULL,
    week_start_date  DATE,
    plan_id          BIGINT       REFERENCES weekly_training_plans (plan_id),
    reason_code      VARCHAR(40),
    message          VARCHAR(500),
    retryable        BOOLEAN      NOT NULL DEFAULT FALSE,
    executions       SMALLINT     NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ  NOT NULL,
    started_at       TIMESTAMPTZ,
    finished_at      TIMESTAMPTZ,
    updated_at       TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ck_pgo_kind CHECK (kind IN ('FIRST_PLAN')),
    CONSTRAINT ck_pgo_status CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'NO_FACTIBLE')),
    -- Un COMPLETED siempre apunta al plan publicado: nunca se informa un exito sin plan.
    CONSTRAINT ck_pgo_completed_has_plan CHECK (status <> 'COMPLETED' OR plan_id IS NOT NULL),
    -- Fin registrado si y solo si la operacion termino.
    CONSTRAINT ck_pgo_finished CHECK ((status IN ('COMPLETED', 'FAILED', 'NO_FACTIBLE')) = (finished_at IS NOT NULL)),
    -- Solo un fallo o un no factible llevan codigo de motivo.
    CONSTRAINT ck_pgo_reason CHECK (reason_code IS NULL OR status IN ('FAILED', 'NO_FACTIBLE')),
    CONSTRAINT ck_pgo_executions CHECK (executions >= 0)
);

CREATE UNIQUE INDEX ux_pgo_una_activa_por_usuario
    ON plan_generation_operations (user_id)
    WHERE status IN ('PENDING', 'PROCESSING');

CREATE INDEX ix_pgo_usuario_reciente ON plan_generation_operations (user_id, created_at DESC);

CREATE INDEX ix_pgo_pendientes ON plan_generation_operations (created_at) WHERE status = 'PENDING';

COMMENT ON TABLE plan_generation_operations IS
    'Generacion del primer plan como operacion asincrona y persistente (V58). El plan se publica con el pipeline y los validadores de siempre.';
COMMENT ON COLUMN plan_generation_operations.executions IS
    'Veces que un worker empezo a ejecutarla. Una ejecucion interrumpida por un reinicio se reintenta una sola vez.';
