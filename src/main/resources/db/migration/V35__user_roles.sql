-- =============================================================================
-- V35 · Rol de la cuenta (RBAC: USER y ADMIN)
-- =============================================================================
-- Siguiente numero libre: db/migration llega a V34 y ninguna otra rama usa el 35.
-- Las dos V35 anteriores (b6fd9ae restauracion, 4801e58 progresion) se retiraron
-- antes de aplicarse en Neon (Flyway en V33 el 27/10) y la V34 las sustituyo.
--
-- Solo agrega. Cada cuenta existente queda como USER con sus datos intactos; el
-- registro publico no fija el rol, asi que toda cuenta nueva tambien es USER.
-- El primer ADMIN se promueve con el procedimiento de ejecucion unica
-- docs/FitSense/operaciones/cambiar_rol.sql, nunca desde la API: ninguna ruta
-- ni ningun codigo de la aplicacion escribe esta columna.
-- =============================================================================

ALTER TABLE users
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER';

ALTER TABLE users
    ADD CONSTRAINT ck_users_role CHECK (role IN ('USER', 'ADMIN'));

COMMENT ON COLUMN users.role IS
    'USER: solo sus propios datos. ADMIN: ademas, la analitica global (/api/v1/admin). '
    'Se lee de la base en cada peticion; el JWT no lo transporta.';
