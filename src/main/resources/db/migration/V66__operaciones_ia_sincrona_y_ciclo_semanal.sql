-- =====================================================================
-- FitSense - V66: el seguimiento V59 en TODOS los caminos que llaman a la IA (sobre V65).
--
-- MOTIVO
--
-- V59 solo protegia la generacion inicial ASINCRONA (kind FIRST_PLAN): su operacion persistente, con propietario y
-- lease, abre el libro de predicciones (plan_generation_predictions) y una reanudacion sigue cada prediccion por su id
-- sin pagar otra. El endpoint SINCRONO del primer plan (POST /users/me/plan/generate) y el CIERRE SEMANAL (generacion
-- de la semana nueva y adaptacion con IA) llamaban a Replicate sin operacion ni libro: si el proceso moria con una
-- prediccion en curso, no quedaba rastro, la siguiente ejecucion pedia otra y el cierre semanal no se recuperaba.
--
-- QUE CAMBIA (solo esquema, sin datos)
--
-- 1. Dos tipos nuevos de operacion, con el MISMO mecanismo de V59 (propietario, lease, latido, fencing, libro):
--      FIRST_PLAN_SYNC: una peticion sincrona del primer plan.
--      WEEKLY_CYCLE:    el cierre semanal de un participante (cerrar, medir, decidir y generar o adaptar).
-- 2. operation_key: la IDENTIDAD LOGICA de la operacion (tipo + usuario + semanas). Tras un reinicio, la siguiente
--    ejecucion de la MISMA clave adopta la operacion huerfana (lease vencido) y su libro: las predicciones ya pedidas
--    se siguen por su id o se reutilizan; ninguna se vuelve a pedir. FIRST_PLAN no la usa (V58 sigue igual).
-- 3. measured_week_start: la semana medida del ciclo (week_start_date es la semana nueva), para relanzar un cierre
--    semanal interrumpido.
--
-- LO QUE NO CAMBIA
--
-- La generacion inicial asincrona (FIRST_PLAN): sus filas, sus restricciones y su unicidad por usuario, que ahora se
-- limita EXPLICITAMENTE a su tipo (antes era el unico). El libro de predicciones, sus estados y su regla de que nunca
-- se recupera una prediccion sin el id de su propia respuesta. Ni el catalogo, ni planes, ni metricas.
-- =====================================================================

ALTER TABLE plan_generation_operations DROP CONSTRAINT ck_pgo_kind;
ALTER TABLE plan_generation_operations
    ADD CONSTRAINT ck_pgo_kind CHECK (kind IN ('FIRST_PLAN', 'FIRST_PLAN_SYNC', 'WEEKLY_CYCLE'));

ALTER TABLE plan_generation_operations
    ADD COLUMN operation_key       VARCHAR(160),
    ADD COLUMN measured_week_start DATE;

-- Las operaciones nuevas siempre llevan su clave logica; la asincrona de V58 nunca (sus filas existentes cumplen).
ALTER TABLE plan_generation_operations
    ADD CONSTRAINT ck_pgo_clave CHECK ((kind = 'FIRST_PLAN') = (operation_key IS NULL));

-- El ciclo semanal siempre sabe que semana mide.
ALTER TABLE plan_generation_operations
    ADD CONSTRAINT ck_pgo_ciclo_semana CHECK (kind <> 'WEEKLY_CYCLE'
        OR (measured_week_start IS NOT NULL AND week_start_date IS NOT NULL));

-- Como mucho UNA operacion en curso por clave logica: dos peticiones, dos instancias o una recuperacion repetida no
-- abren dos operaciones (ni dos libros) para la misma generacion.
CREATE UNIQUE INDEX ux_pgo_clave_en_curso
    ON plan_generation_operations (operation_key)
    WHERE status IN ('PENDING', 'PROCESSING') AND operation_key IS NOT NULL;

-- La unicidad de V58 es de la generacion inicial ASINCRONA: una peticion sincrona o un cierre semanal en curso no
-- deben ocupar el hueco de la operacion que la app consulta (GET /users/me/plan/generation/latest).
DROP INDEX ux_pgo_una_activa_por_usuario;
CREATE UNIQUE INDEX ux_pgo_una_activa_por_usuario
    ON plan_generation_operations (user_id)
    WHERE status IN ('PENDING', 'PROCESSING') AND kind = 'FIRST_PLAN';

-- Barrido de recuperacion de las operaciones nuevas huerfanas (lease vencido).
CREATE INDEX ix_pgo_huerfanas_v66 ON plan_generation_operations (kind, lease_expires_at)
    WHERE status = 'PROCESSING' AND kind <> 'FIRST_PLAN';

COMMENT ON COLUMN plan_generation_operations.operation_key IS
    'V66: identidad logica (tipo, usuario, semanas) de FIRST_PLAN_SYNC y WEEKLY_CYCLE. Una ejecucion de la misma clave adopta la operacion huerfana y su libro de predicciones; nunca se recupera una prediccion por contenido.';
COMMENT ON COLUMN plan_generation_operations.measured_week_start IS
    'V66: semana medida de un WEEKLY_CYCLE (week_start_date es la semana nueva), para relanzar el cierre interrumpido.';
