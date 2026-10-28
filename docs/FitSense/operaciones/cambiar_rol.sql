-- =============================================================================
-- CAMBIO DE ROL DE UNA CUENTA · procedimiento de ejecucion unica (V35)
-- =============================================================================
-- Promueve una cuenta existente a ADMIN o revoca ADMIN (vuelve a USER). Es la
-- UNICA forma prevista de cambiar el rol: la aplicacion no tiene ninguna ruta
-- ni ningun proceso de arranque que lo haga.
--
-- Quien lo ejecuta: el responsable del despliegue, con las credenciales de la
-- base (en Neon, el editor SQL del proyecto). Tener esas credenciales ya es la
-- autorizacion; el script no abre ninguna otra.
--
-- Antes de ejecutarlo:
--   1. La persona se registro con la app y INICIO SESION al menos una vez (asi
--      demuestra que controla la cuenta y su contrasena).
--   2. El responsable confirmo por un canal propio (en persona, correo
--      institucional) que esa cuenta es de quien dice ser.
--   3. Se leyo su user_id:  SELECT user_id, status, role, last_login_at
--                             FROM users WHERE email = lower('<correo>');
--
-- Como se usa: editar las CINCO constantes de abajo y ejecutar el bloque entero.
-- Es atomico: si algo no cuadra, lanza un error y no cambia nada. Despues,
-- guardar la salida (NOTICE) y volver a dejar las constantes vacias.
--
-- Efecto: inmediato. El backend lee el rol en cada peticion; no hace falta un
-- token nuevo ni reiniciar.
-- =============================================================================
DO $$
DECLARE
    v_email     text   := '';  -- correo de la cuenta
    v_user_id   bigint := 0;   -- su user_id (comprobado en el paso 3)
    v_rol       text   := '';  -- ADMIN para promover, USER para revocar
    v_operador  text   := '';  -- nombre del responsable que lo ejecuta (queda registrado)
    v_confirma  text   := '';  -- escribir exactamente: CAMBIAR <user_id> A <rol>
    r           record;
BEGIN
    IF v_rol NOT IN ('USER', 'ADMIN') THEN
        RAISE EXCEPTION 'v_rol debe ser ADMIN o USER';
    END IF;
    IF btrim(v_operador) = '' THEN
        RAISE EXCEPTION 'v_operador es obligatorio: identifica a quien hace el cambio';
    END IF;
    IF v_confirma IS DISTINCT FROM format('CAMBIAR %s A %s', v_user_id, v_rol) THEN
        RAISE EXCEPTION 'Confirmacion ausente o incorrecta: se esperaba «CAMBIAR % A %»', v_user_id, v_rol;
    END IF;

    SELECT user_id, status, role, last_login_at INTO r
      FROM users
     WHERE user_id = v_user_id AND email = lower(btrim(v_email))
       FOR UPDATE;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'No hay una cuenta con user_id % y ese correo', v_user_id;
    END IF;

    IF v_rol = 'ADMIN' THEN
        IF r.status <> 'ACTIVE' THEN
            RAISE EXCEPTION 'La cuenta % no esta activa (%): no se promueve', v_user_id, r.status;
        END IF;
        IF r.last_login_at IS NULL THEN
            RAISE EXCEPTION 'La cuenta % nunca inicio sesion: falta la prueba de titularidad', v_user_id;
        END IF;
    END IF;

    IF r.role = v_rol THEN
        RAISE NOTICE 'SIN_CAMBIOS user_id=% ya tiene el rol %', v_user_id, v_rol;
        RETURN;
    END IF;

    UPDATE users
       SET role = v_rol, role_updated_at = now(), role_updated_by = btrim(v_operador), updated_at = now()
     WHERE user_id = v_user_id;

    RAISE NOTICE 'ROL_CAMBIADO user_id=% de % a % por % el %', v_user_id, r.role, v_rol, btrim(v_operador), now();
END
$$;
