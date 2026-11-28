-- =====================================================================
-- FitSense - V67: historial de RECALCULOS de weekly_user_metrics (sobre V66).
--
-- PROPUESTA C4 (P1-09), PENDIENTE DE VALIDACION METODOLOGICA. No se fusiona sin esa aprobacion.
--
-- MOTIVO
--
-- weekly_user_metrics guarda UNA fila por participante y semana, y un recalculo (un cierre semanal repetido: relanzado,
-- recuperado por B3 o por V66) la SOBRESCRIBE. calculated_at es la fecha del PRIMER calculo (ADHERENCIA.md) y no se
-- toca. Hasta ahora no quedaba constancia de que una semana se recalculo, cuando, que valores tenia antes ni quien lo
-- hizo.
--
-- QUE CAMBIA (solo esquema, sin datos)
--
-- Una tabla nueva, de solo insercion: una fila por cada recalculo de una medicion que ya existia, con la fecha del
-- primer calculo, la del recalculo, los valores anteriores y los recalculados, los campos que cambiaron, el motivo, la
-- version de umbrales de antes y de despues, la fuente de los datos y el proceso o la persona que lo ejecuto.
--
-- LO QUE NO CAMBIA
--
-- weekly_user_metrics: ni columnas, ni restricciones, ni calculated_at, ni la formula de adherencia. Ninguna fila
-- existente se reescribe. Las metricas previas no se borran. Compatible con V66 (no toca sus tablas).
-- =====================================================================

CREATE TABLE weekly_metric_recalculations (
    recalculation_id             BIGSERIAL    PRIMARY KEY,
    weekly_metric_id             BIGINT       NOT NULL,
    user_id                      BIGINT       NOT NULL,
    week_start_date              DATE         NOT NULL,
    first_calculated_at          TIMESTAMPTZ  NOT NULL,
    recalculated_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    reason                       VARCHAR(40)  NOT NULL,
    actor                        VARCHAR(60)  NOT NULL,
    data_source                  VARCHAR(40)  NOT NULL,
    previous_calculation_version VARCHAR(30)  NOT NULL,
    calculation_version          VARCHAR(30)  NOT NULL,
    previous_values              JSONB        NOT NULL,
    recalculated_values          JSONB        NOT NULL,
    changed_fields               TEXT[]       NOT NULL,
    CONSTRAINT fk_wmr_metric FOREIGN KEY (weekly_metric_id) REFERENCES weekly_user_metrics (weekly_metric_id),
    CONSTRAINT fk_wmr_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT ck_wmr_reason CHECK (reason IN ('CIERRE_REPETIDO', 'RECALCULO_MANUAL')),
    CONSTRAINT ck_wmr_actor CHECK (actor ~ '^(PROCESO|ADMIN):[A-Za-z0-9_.:-]{1,50}$'),
    CONSTRAINT ck_wmr_source CHECK (data_source IN ('SESIONES_REGISTRADAS')),
    CONSTRAINT ck_wmr_orden CHECK (recalculated_at >= first_calculated_at)
);

CREATE INDEX ix_wmr_metrica ON weekly_metric_recalculations (weekly_metric_id, recalculated_at);
CREATE INDEX ix_wmr_usuario_semana ON weekly_metric_recalculations (user_id, week_start_date);

-- Solo insercion: un recalculo registrado no se corrige. (DELETE queda permitido: la eliminacion de cuenta borra los
-- datos de la persona, y este historial lo es.)
CREATE FUNCTION fn_wmr_solo_insercion() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'weekly_metric_recalculations es de solo insercion: un recalculo registrado no se modifica';
END;
$$;

CREATE TRIGGER tg_wmr_solo_insercion BEFORE UPDATE ON weekly_metric_recalculations
    FOR EACH ROW EXECUTE FUNCTION fn_wmr_solo_insercion();

COMMENT ON TABLE weekly_metric_recalculations IS
    'C4 (P1-09): un registro por cada recalculo de una medicion semanal ya existente. calculated_at de weekly_user_metrics sigue siendo la fecha del primer calculo.';
COMMENT ON COLUMN weekly_metric_recalculations.reason IS
    'CIERRE_REPETIDO: el cierre semanal se volvio a ejecutar para la misma semana medida. RECALCULO_MANUAL: reservado para un procedimiento manual aprobado (no existe todavia).';
COMMENT ON COLUMN weekly_metric_recalculations.changed_fields IS
    'Columnas de weekly_user_metrics cuyo valor cambio; vacio si el recalculo dio lo mismo.';
