-- V60 · Eliminacion de cuentas (preparacion para Google Play).
--
-- 1) account_deletions: registro minimo de cada eliminacion. Permite reanudar una eliminacion interrumpida y
--    demostrar que se ejecuto. NO guarda correo, nombre, codigo de participante ni ningun dato del perfil: solo el
--    identificador interno mientras la eliminacion esta PENDIENTE (hace falta para reanudarla; se pone a NULL al
--    completarse), el origen, el estado, las fechas y el recuento de filas borradas por tabla. Sin FK a users a
--    proposito: la fila de users se borra.
-- 2) participant_code_seq: los codigos P-xxx dejan de calcularse con count(*)+1. Con eliminaciones, count(*) baja y
--    el siguiente codigo colisionaria con uno existente (uq_users_participant_code) o reutilizaria el de una persona
--    eliminada. La secuencia nunca reutiliza un codigo.

CREATE TABLE account_deletions (
    deletion_id    UUID         NOT NULL PRIMARY KEY,
    user_id        BIGINT,
    origin         VARCHAR(10)  NOT NULL,
    status         VARCHAR(12)  NOT NULL,
    requested_at   TIMESTAMPTZ  NOT NULL,
    completed_at   TIMESTAMPTZ,
    attempts       INTEGER      NOT NULL DEFAULT 0,
    last_error     VARCHAR(300),
    rows_deleted   JSONB,
    CONSTRAINT ck_ad_origin CHECK (origin IN ('APP', 'WEB')),
    CONSTRAINT ck_ad_status CHECK (status IN ('PENDIENTE', 'COMPLETADA')),
    CONSTRAINT ck_ad_completada CHECK ((status = 'COMPLETADA') = (completed_at IS NOT NULL)),
    CONSTRAINT ck_ad_pendiente_con_usuario CHECK (status <> 'PENDIENTE' OR user_id IS NOT NULL),
    CONSTRAINT ck_ad_completada_sin_usuario CHECK (status <> 'COMPLETADA' OR user_id IS NULL)
);

-- Una sola eliminacion en curso por cuenta: dos solicitudes simultaneas no crean dos procesos.
CREATE UNIQUE INDEX ux_ad_una_pendiente_por_usuario ON account_deletions (user_id) WHERE status = 'PENDIENTE';
CREATE INDEX ix_ad_pendientes ON account_deletions (requested_at) WHERE status = 'PENDIENTE';

COMMENT ON TABLE account_deletions IS
    'Registro minimo de eliminaciones de cuenta (sin datos personales). Ver docs/FitSense/preparacion_google_play_backend.';

CREATE SEQUENCE participant_code_seq START WITH 1;
SELECT setval('participant_code_seq',
              COALESCE((SELECT MAX(CAST(substring(participant_code FROM 3) AS INTEGER))
                        FROM users WHERE participant_code ~ '^P-[0-9]+$'), 0) + 1,
              false);
