-- V63 · Registro trazable de consentimientos (preparacion; la exigencia queda apagada hasta aprobar documentos).
--
-- Separa tres cosas que hasta ahora vivian juntas en users.consent_*:
--   STUDY_PARTICIPATION  consentimiento informado para participar en la investigacion;
--   TERMS_OF_SERVICE     aceptacion de las condiciones de uso de la app;
--   PRIVACY_POLICY       aceptacion de la politica de privacidad.
-- Cada fila es una aceptacion concreta: tipo, version del documento, momento, accion afirmativa (que hizo la
-- persona) y origen. La retirada no borra la fila: marca withdrawn_at, para poder demostrar que hubo consentimiento y
-- cuando dejo de haberlo. Las columnas users.consent_* se conservan (compatibilidad); la fuente de verdad pasa a ser
-- esta tabla.
CREATE TABLE user_consents (
    consent_id          BIGSERIAL    PRIMARY KEY,
    user_id             BIGINT       NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    consent_type        VARCHAR(30)  NOT NULL,
    document_version    VARCHAR(40)  NOT NULL,
    granted_at          TIMESTAMPTZ  NOT NULL,
    affirmative_action  VARCHAR(40)  NOT NULL,
    source              VARCHAR(10)  NOT NULL,
    withdrawn_at        TIMESTAMPTZ,
    CONSTRAINT ck_uc_tipo CHECK (consent_type IN ('STUDY_PARTICIPATION', 'TERMS_OF_SERVICE', 'PRIVACY_POLICY')),
    CONSTRAINT ck_uc_origen CHECK (source IN ('APP', 'WEB', 'MIGRACION')),
    CONSTRAINT ck_uc_retirada CHECK (withdrawn_at IS NULL OR withdrawn_at >= granted_at)
);
CREATE UNIQUE INDEX ux_uc_vigente ON user_consents (user_id, consent_type, document_version)
    WHERE withdrawn_at IS NULL;
CREATE INDEX ix_uc_usuario ON user_consents (user_id, consent_type);

-- Las participaciones ya registradas pasan a la tabla tal como estaban: misma fecha, misma version y la retirada si
-- la hubo. La accion queda marcada como anterior a V63: no se puede reconstruir mas detalle del que habia.
INSERT INTO user_consents (user_id, consent_type, document_version, granted_at, affirmative_action, source,
                           withdrawn_at)
SELECT user_id, 'STUDY_PARTICIPATION', COALESCE(consent_version, 'DESCONOCIDA'), consent_granted_at,
       'REGISTRO_ANTERIOR_A_V63', 'MIGRACION',
       CASE WHEN withdrawn_at IS NULL THEN NULL ELSE GREATEST(withdrawn_at, consent_granted_at) END
FROM users
WHERE is_study_participant AND consent_granted_at IS NOT NULL;
