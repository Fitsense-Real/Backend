-- V64 - Notificaciones push con Firebase Cloud Messaging (FCM), identificadas por Firebase Installation ID (FID).
--
-- Solo esquema: cuatro tablas nuevas, sin datos y sin tocar ninguna tabla existente.
-- Contrato completo: docs/FitSense/notificaciones_push/CONTRATO_PUSH_MOBILE.md.
--
--   push_devices              instalaciones de la app vinculadas a una cuenta (un FID activo pertenece a UNA cuenta)
--   notification_preferences  preferencias por categoria; sin fila = valores por omision (todo activado)
--   notification_events       bandeja de salida (outbox): un evento por plan publicado o aviso de cuenta, ya
--                             persistido; el emisor solo lee filas confirmadas
--   notification_deliveries   cada intento de envio a un dispositivo, para no repetir un envio ya aceptado
--
-- Todas tienen user_id: la eliminacion de cuenta las borra (EliminacionDeCuentaService.TABLAS_EN_ORDEN).

CREATE TABLE push_devices (
    device_id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id          BIGINT       NOT NULL REFERENCES users (user_id),
    -- Firebase Installation ID. NO se admiten tokens de registro FCM (contrato: un solo tipo de identificador).
    installation_id  VARCHAR(64)  NOT NULL,
    identifier_type  VARCHAR(10)  NOT NULL DEFAULT 'FID',
    platform         VARCHAR(10)  NOT NULL,
    app_id           VARCHAR(100) NOT NULL,
    app_version      VARCHAR(40),
    -- users.token_version al registrar: si la contrasena cambia, el dispositivo deja de recibir hasta que la app
    -- vuelva a registrarse con una sesion nueva.
    token_version    INTEGER      NOT NULL,
    registered_at    TIMESTAMPTZ  NOT NULL,
    last_seen_at     TIMESTAMPTZ  NOT NULL,
    revoked_at       TIMESTAMPTZ,
    revoke_reason    VARCHAR(30),
    CONSTRAINT ck_pd_identifier_type CHECK (identifier_type = 'FID'),
    CONSTRAINT ck_pd_platform CHECK (platform IN ('ANDROID')),
    CONSTRAINT ck_pd_installation_id CHECK (installation_id ~ '^[A-Za-z0-9_-]{16,64}$'),
    CONSTRAINT ck_pd_token_version CHECK (token_version >= 0),
    CONSTRAINT ck_pd_revocado CHECK ((revoked_at IS NULL) = (revoke_reason IS NULL)),
    CONSTRAINT ck_pd_revoke_reason CHECK (revoke_reason IS NULL OR revoke_reason IN
        ('DESVINCULADO', 'CAMBIO_DE_CUENTA', 'NO_REGISTRADO_EN_FCM', 'REEMPLAZADO'))
);

-- Un FID activo pertenece a una sola cuenta: registrarlo en otra revoca el anterior (cambio de cuenta).
CREATE UNIQUE INDEX ux_pd_fid_activo ON push_devices (installation_id) WHERE revoked_at IS NULL;
CREATE INDEX ix_pd_usuario_activo ON push_devices (user_id) WHERE revoked_at IS NULL;

CREATE TABLE notification_preferences (
    user_id          BIGINT       PRIMARY KEY REFERENCES users (user_id),
    plan_updates     BOOLEAN      NOT NULL DEFAULT TRUE,
    account_notices  BOOLEAN      NOT NULL DEFAULT TRUE,
    updated_at       TIMESTAMPTZ  NOT NULL
);

CREATE TABLE notification_events (
    event_id              UUID         PRIMARY KEY,
    user_id               BIGINT       NOT NULL REFERENCES users (user_id),
    event_type            VARCHAR(30)  NOT NULL,
    notice_code           VARCHAR(30),
    resource_type         VARCHAR(20),
    resource_id           BIGINT,
    target_screen         VARCHAR(30)  NOT NULL,
    -- De donde salio el evento (tabla de origen), para trazarlo sin guardar datos de salud.
    source                VARCHAR(40)  NOT NULL,
    -- Antiduplicado: un plan se notifica una sola vez, aunque varios procesos lo publiquen o lo detecten.
    dedupe_key            VARCHAR(120) NOT NULL,
    -- Solo avisos de cuenta: la version de credenciales de los dispositivos a los que va dirigido.
    target_token_version  INTEGER,
    status                VARCHAR(20)  NOT NULL,
    status_reason         VARCHAR(40),
    attempts              SMALLINT     NOT NULL DEFAULT 0,
    next_attempt_at       TIMESTAMPTZ  NOT NULL,
    lease_until           TIMESTAMPTZ,
    created_at            TIMESTAMPTZ  NOT NULL,
    recorded_at           TIMESTAMPTZ  NOT NULL,
    finished_at           TIMESTAMPTZ,
    CONSTRAINT ux_ne_dedupe UNIQUE (dedupe_key),
    CONSTRAINT ck_ne_type CHECK (event_type IN ('PLAN_GENERATED', 'PLAN_ADAPTED', 'WEEKLY_PLAN_READY', 'ACCOUNT_NOTICE')),
    CONSTRAINT ck_ne_notice CHECK ((event_type = 'ACCOUNT_NOTICE') = (notice_code IS NOT NULL)),
    CONSTRAINT ck_ne_notice_code CHECK (notice_code IS NULL OR notice_code IN ('PASSWORD_CHANGED')),
    CONSTRAINT ck_ne_resource CHECK ((resource_type IS NULL) = (resource_id IS NULL)),
    CONSTRAINT ck_ne_plan_resource CHECK (event_type = 'ACCOUNT_NOTICE' OR (resource_type = 'PLAN' AND resource_id IS NOT NULL)),
    CONSTRAINT ck_ne_target_version CHECK ((event_type = 'ACCOUNT_NOTICE') = (target_token_version IS NOT NULL)),
    CONSTRAINT ck_ne_screen CHECK (target_screen IN ('WEEKLY_PLAN', 'ACCOUNT_SECURITY')),
    CONSTRAINT ck_ne_status CHECK (status IN
        ('PENDIENTE', 'EN_ENVIO', 'ENVIADO', 'SIN_DESTINO', 'DESCARTADO', 'FALLIDO', 'CADUCADO')),
    CONSTRAINT ck_ne_terminado CHECK ((status IN ('PENDIENTE', 'EN_ENVIO')) = (finished_at IS NULL)),
    CONSTRAINT ck_ne_lease CHECK ((status = 'EN_ENVIO') = (lease_until IS NOT NULL)),
    CONSTRAINT ck_ne_attempts CHECK (attempts >= 0)
);

CREATE INDEX ix_ne_por_enviar ON notification_events (next_attempt_at) WHERE status IN ('PENDIENTE', 'EN_ENVIO');
CREATE INDEX ix_ne_usuario ON notification_events (user_id);

CREATE TABLE notification_deliveries (
    delivery_id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_id         UUID         NOT NULL REFERENCES notification_events (event_id) ON DELETE CASCADE,
    device_id        BIGINT       NOT NULL REFERENCES push_devices (device_id) ON DELETE CASCADE,
    user_id          BIGINT       NOT NULL REFERENCES users (user_id),
    status           VARCHAR(20)  NOT NULL,
    fcm_message_id   VARCHAR(200),
    error_code       VARCHAR(40),
    attempted_at     TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ck_nd_status CHECK (status IN ('ENVIADO', 'ERROR_TEMPORAL', 'ERROR_PERMANENTE')),
    CONSTRAINT ck_nd_enviado CHECK ((status = 'ENVIADO') = (fcm_message_id IS NOT NULL)),
    CONSTRAINT ck_nd_error CHECK ((status = 'ENVIADO') = (error_code IS NULL))
);

-- Un envio aceptado por FCM no se repite para el mismo dispositivo.
CREATE UNIQUE INDEX ux_nd_enviado ON notification_deliveries (event_id, device_id) WHERE status = 'ENVIADO';
CREATE INDEX ix_nd_usuario ON notification_deliveries (user_id);
