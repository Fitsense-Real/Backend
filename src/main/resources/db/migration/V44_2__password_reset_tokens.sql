-- =====================================================================
-- V44_2 · Recuperacion de contrasena (cierre definitivo del backend, fase 2.2,
-- 02/11/2026).
--
-- Solo CREA una tabla nueva. No modifica datos existentes.
--
-- El token NO se guarda: solo su SHA-256 (token_hash). Un solo uso (used_at),
-- caducidad corta (expires_at) e indice por usuario y fecha para el limite de
-- solicitudes. Version 44.2: despues de V44_1 (PR del NO_FACTIBLE estructurado)
-- y antes de la V45 candidata del PR #17; ninguna aplicada en Neon ni Railway.
-- =====================================================================

CREATE TABLE public.password_reset_tokens (
    token_id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id      BIGINT       NOT NULL REFERENCES public.users (user_id),
    token_hash   VARCHAR(64)  NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    expires_at   TIMESTAMPTZ  NOT NULL,
    used_at      TIMESTAMPTZ,
    CONSTRAINT uq_prt_token_hash UNIQUE (token_hash),
    CONSTRAINT ck_prt_hash_sha256 CHECK (token_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_prt_caducidad CHECK (expires_at > created_at),
    CONSTRAINT ck_prt_uso CHECK (used_at IS NULL OR used_at >= created_at)
);

CREATE INDEX ix_prt_user_created ON public.password_reset_tokens (user_id, created_at);

COMMENT ON TABLE public.password_reset_tokens IS
    'Recuperacion de contrasena: hash SHA-256 del token de un solo uso; nunca el token en claro.';
