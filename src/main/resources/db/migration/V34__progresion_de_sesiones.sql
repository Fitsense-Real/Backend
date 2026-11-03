-- =============================================================================
-- V34 · Progresion gradual de una sesion (DISENO_PROGRESION_Y_REINCORPORACION)
-- =============================================================================
-- Sustituye a la V35 de restauracion (b6fd9ae), que nunca se aplico en Neon
-- (comprobado el 27/10: Flyway en V33, sin restored_sessions). Usa el V34, el
-- siguiente numero libre: la correccion propuesta del 1374
-- (docs/FitSense/migraciones_propuestas/V34__correccion_inc_cat_1374.sql) no
-- reserva numero -su cabecera dice «con el siguiente numero libre»- y, si se
-- aprueba, entra como V35 o posterior.
--
-- Solo agrega. Las filas existentes quedan con progressed_sessions = '{}'.
--
-- progressed_sessions: sesiones del plan de ORIGEN (como los otros cuatro
-- arreglos) cuya derivada recibio UN paso de progresion propuesto por el
-- backend: recuperacion, progresion ordinaria, reincorporacion o vuelta
-- atras. El detalle del paso (ejercicio, dosis antes y despues, volumenes)
-- va en conservation_report.pasos.
--
-- Invariantes:
--   * sin repetidos y disjunto de targeted, applied, no_effect y conserved;
--   * a lo sumo una por intento: una modificacion funcional por ciclo;
--   * solo con SIN_ORDEN: la progresion no convive con una adaptacion;
--   * solo si se publico un plan;
--   * pertenece al plan de origen (fn_intento_coherente, ampliada).
-- V33 (APLICADA <=> applied_sessions no vacio) no cambia.
-- =============================================================================

ALTER TABLE plan_adaptation_attempts
    ADD COLUMN progressed_sessions BIGINT[] NOT NULL DEFAULT '{}';

COMMENT ON COLUMN plan_adaptation_attempts.progressed_sessions IS
    'V34: sesion del plan de origen cuya derivada recibio un paso de progresion del backend (detalle en conservation_report.pasos). A lo sumo una; solo con SIN_ORDEN.';

ALTER TABLE plan_adaptation_attempts
    ADD CONSTRAINT ck_paa_progresadas_unicas CHECK (fn_sin_duplicados(progressed_sessions)),
    ADD CONSTRAINT ck_paa_progresadas_disjuntas CHECK (
        NOT (progressed_sessions && targeted_sessions)
        AND NOT (progressed_sessions && applied_sessions)
        AND NOT (progressed_sessions && no_effect_sessions)
        AND NOT (progressed_sessions && conserved_sessions)),
    ADD CONSTRAINT ck_paa_un_paso CHECK (cardinality(progressed_sessions) <= 1),
    ADD CONSTRAINT ck_paa_paso_sin_orden CHECK (
        cardinality(progressed_sessions) = 0 OR status = 'SIN_ORDEN'),
    ADD CONSTRAINT ck_paa_paso_publicado CHECK (
        cardinality(progressed_sessions) = 0 OR resulting_plan_id IS NOT NULL);

-- La misma funcion de V32, con progressed_sessions entre los arreglos que deben
-- pertenecer al plan de origen. El resto, identico.
CREATE OR REPLACE FUNCTION fn_intento_coherente() RETURNS TRIGGER AS $$
DECLARE
usuario_metrica   BIGINT;
    semana_metrica    DATE;
    usuario_origen    BIGINT;
    semana_origen     DATE;
    usuario_resultado BIGINT;
    semana_resultado  DATE;
    origen_resultado  VARCHAR(20);
    ajenas            BIGINT[];
BEGIN
SELECT m.user_id, m.week_start_date INTO usuario_metrica, semana_metrica
FROM weekly_user_metrics m
WHERE m.weekly_metric_id = NEW.weekly_metric_id;

IF usuario_metrica IS NULL OR usuario_metrica <> NEW.user_id THEN
        RAISE EXCEPTION
            'V32: el intento dice ser del usuario % y su metrica (%) es del usuario %',
            NEW.user_id, NEW.weekly_metric_id, usuario_metrica;
END IF;

SELECT p.user_id, p.week_start_date INTO usuario_origen, semana_origen
FROM weekly_training_plans p
WHERE p.plan_id = NEW.source_plan_id;

IF usuario_origen IS NULL OR usuario_origen <> NEW.user_id THEN
        RAISE EXCEPTION
            'V32: el intento es del usuario % y su plan de origen (%) es del usuario %',
            NEW.user_id, NEW.source_plan_id, usuario_origen;
END IF;

    -- El plan de origen tiene que ser el de la semana MEDIDA. Cualquier version
    -- de esa semana vale: la unicidad del esquema es por (usuario, semana,
    -- version), asi que comparar la fecha no descarta una revision legitima.
    IF semana_origen <> semana_metrica THEN
        RAISE EXCEPTION
            'V32: el plan de origen % es de la semana del % y la metrica mide la del %',
            NEW.source_plan_id, semana_origen, semana_metrica;
END IF;

    IF NEW.new_week_start_date <= semana_origen THEN
        RAISE EXCEPTION
            'V32: la semana nueva (%) no es posterior a la semana de origen (%)',
            NEW.new_week_start_date, semana_origen;
END IF;

    -- v3 · LAS SESIONES SON DEL PLAN DE ORIGEN.
    --
    -- Los cuatro arreglos guardan identificadores de planned_workouts y un
    -- arreglo NO puede llevar claves foraneas, asi que sin esto se podia
    -- registrar una sesion inexistente, de otro usuario o de otra semana. Las
    -- restricciones de subconjunto no lo cubren: comparan los arreglos entre si,
    -- no contra los entrenamientos reales.
    --
    -- Los cuatro llevan identificadores de la semana MEDIDA -el desenlace se
    -- reporta por origen, como lo hace ConservationReport-, asi que la
    -- condicion es la misma para todos: pertenecer a source_plan_id. Y como
    -- arriba ya se comprobo que ese plan es del usuario y de la semana medida,
    -- esta sola condicion descarta los tres casos a la vez.
SELECT array_agg(DISTINCT s.id ORDER BY s.id) INTO ajenas
FROM unnest(NEW.targeted_sessions || NEW.applied_sessions
    || NEW.no_effect_sessions || NEW.conserved_sessions || NEW.progressed_sessions) AS s(id)
WHERE NOT EXISTS (SELECT 1 FROM planned_workouts w
                  WHERE w.planned_workout_id = s.id
                    AND w.plan_id = NEW.source_plan_id);

IF ajenas IS NOT NULL THEN
        RAISE EXCEPTION
            'V32: las sesiones % no pertenecen al plan de origen % (no existen, o son de otro usuario o de otra semana)',
            ajenas, NEW.source_plan_id;
END IF;

    IF NEW.resulting_plan_id IS NOT NULL THEN
SELECT p.user_id, p.week_start_date, p.generation_source
INTO usuario_resultado, semana_resultado, origen_resultado
FROM weekly_training_plans p
WHERE p.plan_id = NEW.resulting_plan_id;

IF usuario_resultado IS NULL OR usuario_resultado <> NEW.user_id THEN
            RAISE EXCEPTION
                'V32: el intento es del usuario % y su plan resultante (%) es del usuario %',
                NEW.user_id, NEW.resulting_plan_id, usuario_resultado;
END IF;

        IF semana_resultado <> NEW.new_week_start_date THEN
            RAISE EXCEPTION
                'V32: el plan resultante % es de la semana del % y el intento declara la del %',
                NEW.resulting_plan_id, semana_resultado, NEW.new_week_start_date;
END IF;

        -- v3 · EL ORIGEN DE GENERACION NO PUEDE CONTRADECIR AL PLAN PUBLICADO.
        --
        -- Es la razon de ser de CONSERVED: que los indicadores de la tesis no
        -- atribuyan al modelo un plan que solo se copio. Si el intento dice AI y
        -- el plan publicado dice CONSERVED, una de las dos cifras es falsa, y
        -- cual lo sea no se puede averiguar despues.
        IF NEW.generation_source <> origen_resultado THEN
            RAISE EXCEPTION
                'V32: el intento declara origen % y el plan publicado % dice %',
                NEW.generation_source, NEW.resulting_plan_id, origen_resultado;
END IF;
END IF;

RETURN NULL;
END $$ LANGUAGE plpgsql;

COMMENT ON FUNCTION fn_intento_coherente() IS
    'V32 + V34: metrica, plan de origen y plan resultante pertenecen al MISMO usuario; el origen es el de la semana medida; las sesiones de los cinco arreglos pertenecen al plan de origen; el origen de generacion coincide con el del plan publicado; la semana nueva es posterior y coincide con la del plan resultante.';
