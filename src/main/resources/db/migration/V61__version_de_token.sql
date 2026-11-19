-- V61 · Version de credenciales para invalidar sesiones.
--
-- Cada JWT lleva la version vigente al emitirse (claim "tv"). El filtro solo acepta el token si coincide con
-- users.token_version, y cambiar la contrasena (recuperacion por correo) la incrementa: todos los tokens anteriores
-- dejan de servir en la siguiente peticion. Los tokens emitidos antes de V61 no tienen "tv" y cuentan como 0, que es
-- el valor inicial: siguen validos hasta su caducidad o hasta el primer cambio de contrasena.
ALTER TABLE users ADD COLUMN token_version INTEGER NOT NULL DEFAULT 0;
ALTER TABLE users ADD CONSTRAINT ck_users_token_version CHECK (token_version >= 0);
