-- =====================================================================
-- FitSense - V59: generacion asincrona resiliente (sobre V58).
--
-- 1. LEASE CON PROPIETARIO en plan_generation_operations. Una operacion PROCESSING pertenece al worker que la
--    reclamo (lease_owner) mientras su lease no venza (lease_expires_at). El latido renueva el lease SOLO si el
--    propietario coincide, y cerrar o publicar exige ser el propietario (fencing): un worker que perdio su lease no
--    publica nada. first_started_at fija el inicio de la PRIMERA ejecucion para el tiempo maximo de la operacion,
--    que ya no se reinicia en cada reencolado.
--
-- 2. LIBRO DE PREDICCIONES (plan_generation_predictions). Una operacion de FitSense puede necesitar varias
--    predicciones de Replicate (reintentos tras un rechazo). Cada una se anota ANTES de enviarla (SOLICITANDO) y se
--    confirma con su identificador en cuanto Replicate lo devuelve (ENVIADA). Al reanudar tras un reinicio, el
--    pipeline se repite y cada intento consulta primero su fila: reutiliza la salida, sigue la prediccion por su id o,
--    si el envio no llego a confirmarse, NO reenvia (NO_CONFIRMADA). Sin secretos: ni token ni cabeceras; el prompt
--    solo como huella SHA-256 (request_hash).
--
-- Solo esquema, sin datos. No toca el catalogo ni V57.
-- =====================================================================

ALTER TABLE plan_generation_operations
    ADD COLUMN lease_owner      VARCHAR(80),
    ADD COLUMN lease_expires_at TIMESTAMPTZ,
    ADD COLUMN first_started_at TIMESTAMPTZ;

-- Una PROCESSING siempre tiene propietario y lease; fuera de PROCESSING no hay lease.
ALTER TABLE plan_generation_operations
    ADD CONSTRAINT ck_pgo_lease CHECK ((status = 'PROCESSING') = (lease_owner IS NOT NULL AND lease_expires_at IS NOT NULL))
    NOT VALID;

CREATE INDEX ix_pgo_lease_vencido ON plan_generation_operations (lease_expires_at) WHERE status = 'PROCESSING';

COMMENT ON COLUMN plan_generation_operations.lease_owner IS
    'Worker (instancia) que ejecuta la operacion PROCESSING. Solo el propietario renueva el lease, cierra o publica.';
COMMENT ON COLUMN plan_generation_operations.lease_expires_at IS
    'Hasta cuando la operacion es del propietario sin latir. Vencido: el proceso murio y la operacion se recupera.';
COMMENT ON COLUMN plan_generation_operations.first_started_at IS
    'Inicio de la primera ejecucion. El tiempo maximo de la operacion se mide desde aqui, aunque se reencole.';

CREATE TABLE plan_generation_predictions (
    prediction_row_id       BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    operation_id            UUID         NOT NULL REFERENCES plan_generation_operations (operation_id),
    user_id                 BIGINT       NOT NULL REFERENCES users (user_id),
    attempt                 SMALLINT     NOT NULL,
    request_hash            CHAR(64)     NOT NULL,
    status                  VARCHAR(20)  NOT NULL,
    replicate_prediction_id VARCHAR(64),
    reused_from_row_id      BIGINT       REFERENCES plan_generation_predictions (prediction_row_id),
    output                  TEXT,
    error                   VARCHAR(500),
    requested_at            TIMESTAMPTZ  NOT NULL,
    sent_at                 TIMESTAMPTZ,
    last_polled_at          TIMESTAMPTZ,
    finished_at             TIMESTAMPTZ,
    polls                   INTEGER      NOT NULL DEFAULT 0,
    CONSTRAINT ck_pgp_status CHECK (status IN ('SOLICITANDO', 'ENVIADA', 'SUCCEEDED', 'FAILED', 'CANCELED', 'TIMEOUT',
                                               'NO_CONFIRMADA', 'ERROR_ENVIO', 'DESCARTADA')),
    CONSTRAINT ck_pgp_attempt CHECK (attempt >= 0),
    -- Con identificador desde que Replicate lo devolvio; nunca SUCCEEDED sin salida.
    CONSTRAINT ck_pgp_enviada_con_id CHECK (status NOT IN ('ENVIADA', 'SUCCEEDED') OR replicate_prediction_id IS NOT NULL),
    CONSTRAINT ck_pgp_succeeded_con_salida CHECK (status <> 'SUCCEEDED' OR output IS NOT NULL)
);

-- Un solo intento vigente por operacion y numero de intento (los DESCARTADA quedan como historial).
CREATE UNIQUE INDEX ux_pgp_intento_vigente
    ON plan_generation_predictions (operation_id, attempt)
    WHERE status <> 'DESCARTADA';

-- Reutilizacion entre operaciones del MISMO usuario con la MISMA peticion.
CREATE INDEX ix_pgp_reutilizable ON plan_generation_predictions (user_id, request_hash, requested_at DESC)
    WHERE status IN ('SUCCEEDED', 'ENVIADA');

CREATE INDEX ix_pgp_operacion ON plan_generation_predictions (operation_id);

COMMENT ON TABLE plan_generation_predictions IS
    'V59: cada prediccion de Replicate pedida por una operacion de generacion. Permite reanudar sin pedir otra y contabilizar iniciadas, reutilizadas, completadas, fallidas, canceladas y no confirmadas. Sin secretos.';
COMMENT ON COLUMN plan_generation_predictions.request_hash IS
    'SHA-256 de modelo + razonamiento + esquema + prompt. Solo se reutiliza una prediccion de la misma peticion.';
COMMENT ON COLUMN plan_generation_predictions.status IS
    'SOLICITANDO: anotada, envio sin confirmar. ENVIADA: Replicate devolvio el id. NO_CONFIRMADA: el envio pudo aceptarse sin que se guardara el id; no se reenvia automaticamente. DESCARTADA: la peticion cambio.';
