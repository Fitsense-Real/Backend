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
-- (docs/FitSense/operaciones/cambiar_rol.sql), nunca desde la API: ninguna ruta
-- ni ningun codigo de la aplicacion escribe esta columna.
--
-- role_updated_at y role_updated_by registran quien y cuando cambio el rol por
-- ultima vez. ck_users_admin_auditado obliga a que toda cuenta ADMIN los tenga:
-- una promocion sin rastro la rechaza la propia base.
-- =============================================================================

ALTER TABLE users
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER';

ALTER TABLE users
    ADD COLUMN role_updated_at TIMESTAMPTZ,
    ADD COLUMN role_updated_by VARCHAR(100);

ALTER TABLE users
    ADD CONSTRAINT ck_users_role CHECK (role IN ('USER', 'ADMIN')),
    ADD CONSTRAINT ck_users_admin_auditado CHECK (
        role = 'USER' OR (role_updated_at IS NOT NULL AND role_updated_by IS NOT NULL));

COMMENT ON COLUMN users.role IS
    'USER: solo sus propios datos. ADMIN: ademas, la administracion global (/api/v1/admin). '
    'Se lee de la base en cada peticion; el JWT no lo transporta.';

COMMENT ON COLUMN users.role_updated_by IS
    'Responsable del ultimo cambio de rol (nombre o identificador del operador, nunca un secreto).';
