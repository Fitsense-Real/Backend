-- V62 · Solicitud de eliminacion de cuenta desde la web (sin app ni contrasena).
--
-- La persona escribe su correo; si corresponde a una cuenta activa, recibe un enlace con un token de un solo uso.
-- Igual que password_reset_tokens: solo se guarda el SHA-256 del token, caduca pronto y se marca usado. La FK
-- cae en cascada con users, y ademas EliminacionDeCuentaService la borra de forma explicita.
CREATE TABLE account_deletion_tokens (
    token_id    BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    token_hash  CHAR(64)     NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL,
    expires_at  TIMESTAMPTZ  NOT NULL,
    used_at     TIMESTAMPTZ,
    CONSTRAINT uq_adt_hash UNIQUE (token_hash),
    CONSTRAINT ck_adt_caducidad CHECK (expires_at > created_at)
);
CREATE INDEX ix_adt_usuario ON account_deletion_tokens (user_id, created_at);
