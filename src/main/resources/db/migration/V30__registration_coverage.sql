-- =====================================================================
-- FitSense — V30 · COBERTURA DE REGISTRO
-- 17/10/2026
--
-- MOTIVO
--
-- Hasta aqui, una sesion SIN REGISTRO se contaba igual que una omitida.
-- WeeklyMetricsCalculator hacia:
--
--     double completion = session == null ? 0.0 : session.completion();
--     weightedNumerator += workout.expectedDurationMinutes() * completion;
--     ...
--     } else { skipped++; }
--
-- O sea: la sesion desconocida entraba como 0,0 en el numerador de la
-- adherencia ponderada y se sumaba a skipped_workouts. Consecuencia: un
-- participante que NO REGISTRO sus entrenamientos era indistinguible de uno
-- que los OMITIO TODOS, y la decision de adaptacion no podia separarlos.
-- Son los dos casos que hay que tratar al contrario: el primero es falta de
-- evidencia y el segundo es adherencia real del 0 %.
--
-- LO QUE ESTA MIGRACION NO HACE
--
-- NO redefine weighted_adherence_pct. Es la metrica primaria del estudio
-- ("primary_metric": "weighted_adherence_pct" en la configuracion sembrada
-- en V7) y cambiarle el denominador a mitad del piloto invalidaria la
-- comparacion entre semanas. Se queda exactamente igual: denominador = los
-- minutos indicados de TODOS los entrenamientos previstos.
--
-- Tampoco toca consecutive_skips ni risk_score, que dependen de ella.
--
-- LO QUE ANADE
--
-- Dos medidas nuevas al lado de la primaria, mas la cuenta que las sostiene:
--
--   recorded_workouts          cuantos de los previstos tienen registro
--   observed_adherence_pct     cumplimiento SOLO sobre lo que tiene registro
--   registration_coverage_pct  que parte de la semana tiene registro
--
-- La decision de adaptacion usara estas dos. El estudio sigue informando la
-- primaria sin cambio de definicion.
--
-- QUE CUENTA COMO "CON REGISTRO"
--
-- Hay registro si existe una sesion, O si el entrenamiento previsto quedo en
-- estado SKIPPED. Una omision DECLARADA es informacion: la persona nos dijo
-- que no entreno. Un entrenamiento que sigue en SCHEDULED y sin sesion no
-- informa de nada.
--
-- POR QUE LAS COLUMNAS SON NULL-ABLES
--
-- Las filas que ya existan se calcularon con la definicion anterior, en la
-- que no se distinguia lo desconocido. Rellenarlas afirmaria algo que no
-- sabemos: que aquellas semanas tenian cobertura completa. Se quedan en NULL,
-- que es la lectura correcta —"no calculado con esta definicion"— y obliga a
-- recalcular la semana antes de decidir sobre ella.
--
-- QUE TOCA ESTA MIGRACION, exactamente
--
--   1. DDL sobre weekly_user_metrics: tres columnas y seis restricciones.
--      No modifica ni una fila de esa tabla.
--   2. DML sobre calculation_configs: desactiva la version activa e inserta
--      una nueva con los MISMOS parametros, para sellar el cambio de calculo
--      (ver la seccion VERSION DE CALCULO al final). Es el mecanismo de V19.
--
-- NO toca el catalogo de ejercicios ni ninguna otra tabla.
-- =====================================================================

ALTER TABLE weekly_user_metrics
    ADD COLUMN recorded_workouts         SMALLINT,
    ADD COLUMN observed_adherence_pct    NUMERIC(5,2),
    ADD COLUMN registration_coverage_pct NUMERIC(5,2);

COMMENT ON COLUMN weekly_user_metrics.recorded_workouts IS
    'Entrenamientos previstos que tienen registro: existe sesion, o el previsto quedo en SKIPPED. NULL = calculado con la definicion anterior a V30.';
COMMENT ON COLUMN weekly_user_metrics.observed_adherence_pct IS
    'Cumplimiento ponderado SOLO sobre los entrenamientos con registro. NULL si ninguno lo tiene. NO es la metrica primaria del estudio.';
COMMENT ON COLUMN weekly_user_metrics.registration_coverage_pct IS
    'recorded_workouts / scheduled_workouts en porcentaje. Es lo que decide si hay evidencia suficiente para adaptar.';

-- ---------------------------------------------------------------------
-- Restricciones. Todas con guarda IS NULL para que las filas anteriores a
-- V30 sigan siendo validas sin inventarles valores.
-- ---------------------------------------------------------------------
ALTER TABLE weekly_user_metrics
    ADD CONSTRAINT ck_wum_observed
        CHECK (observed_adherence_pct IS NULL OR observed_adherence_pct BETWEEN 0 AND 100),

    ADD CONSTRAINT ck_wum_coverage
        CHECK (registration_coverage_pct IS NULL OR registration_coverage_pct BETWEEN 0 AND 100),

    ADD CONSTRAINT ck_wum_recorded_range
        CHECK (recorded_workouts IS NULL
               OR (recorded_workouts >= 0 AND recorded_workouts <= scheduled_workouts)),

    -- valid_workouts y skipped_workouts cuentan ahora SOLO lo que tiene
    -- registro, asi que su suma es exactamente recorded_workouts. Antes de
    -- V30 sumaban scheduled_workouts porque lo desconocido caia en skipped.
    -- Esta restriccion es la que impide volver a confundirlos.
    ADD CONSTRAINT ck_wum_recorded_partition
        CHECK (recorded_workouts IS NULL
               OR valid_workouts + skipped_workouts = recorded_workouts),

    -- Sin un solo registro no hay denominador observado: NULL, no 0.
    ADD CONSTRAINT ck_wum_observed_needs_record
        CHECK (observed_adherence_pct IS NULL
               OR (recorded_workouts IS NOT NULL AND recorded_workouts > 0)),

    -- Misma regla que la primaria: sin plan activo no hay porcentaje (19.4).
    ADD CONSTRAINT ck_wum_observed_null_when_no_plan
        CHECK (has_active_plan OR observed_adherence_pct IS NULL);

-- =====================================================================
-- VERSION DE CALCULO
--
-- V30 cambia el SIGNIFICADO de skipped_workouts: antes incluia los
-- entrenamientos sin registro y ahora cuenta solo los que lo tienen. Ese
-- numero se guarda y se publica, asi que hay que poder distinguir las
-- metricas calculadas antes de V30 de las calculadas despues.
--
-- El mecanismo ya existe y es el que usa V19: toda metrica guarda en
-- calculation_version la version de la configuracion activa. Se crea una
-- version nueva con los MISMOS parametros -no se toca ni un umbral- y se
-- activa. A partir de aqui, toda semana calculada o recalculada queda
-- sellada con la version nueva.
--
-- No se recalcula ninguna semana historica: sus filas conservan su
-- calculation_version anterior y sus tres columnas nuevas en NULL, que es
-- precisamente como se reconocen.
-- =====================================================================
DO $$
DECLARE
activa_version  VARCHAR(30);
    activa_params   JSONB;
    nueva_version   VARCHAR(30);
BEGIN
SELECT version, params INTO activa_version, activa_params
FROM calculation_configs WHERE is_active;

IF activa_version IS NULL THEN
        RAISE EXCEPTION 'V30: no hay configuracion activa que versionar';
END IF;

    -- Misma numeracion que V19: MVP-1.N -> MVP-1.(N+1).
SELECT 'MVP-1.' || (COALESCE(MAX(substring(version FROM '^MVP-1\.(\d+)$')::INTEGER), 0) + 1)
INTO nueva_version FROM calculation_configs;

UPDATE calculation_configs SET is_active = FALSE WHERE is_active;

INSERT INTO calculation_configs (version, description, params, is_active)
VALUES (nueva_version,
        'Mismos parametros que ' || activa_version || '. Version nueva solo para '
            || 'sellar el cambio de calculo de V30: skipped_workouts pasa a contar '
            || 'unicamente los entrenamientos CON registro, y se anaden '
            || 'recorded_workouts, observed_adherence_pct y '
            || 'registration_coverage_pct.',
        activa_params, TRUE);

RAISE NOTICE 'V30: % desactivada, % creada y activa (parametros sin cambio)',
        activa_version, nueva_version;
END $$;

-- ---------------------------------------------------------------------
-- Comprobacion: la version nueva tiene EXACTAMENTE los mismos parametros
-- que la anterior. Esta migracion no calibra nada, y si alguien colase un
-- cambio de umbral aqui, esto lo rechaza.
-- ---------------------------------------------------------------------
DO $$
DECLARE
distintas INTEGER;
    activas   INTEGER;
BEGIN
SELECT COUNT(*) INTO activas FROM calculation_configs WHERE is_active;
IF activas <> 1 THEN
        RAISE EXCEPTION 'V30: deberia haber exactamente una configuracion activa y hay %', activas;
END IF;

SELECT COUNT(*) INTO distintas FROM (
                                        SELECT params FROM calculation_configs WHERE is_active
                                        EXCEPT
                                        SELECT params FROM calculation_configs
                                        WHERE NOT is_active
                                          AND version = (SELECT version FROM calculation_configs WHERE NOT is_active
                                                         ORDER BY config_id DESC LIMIT 1)) d;
IF distintas <> 0 THEN
        RAISE EXCEPTION 'V30: los parametros de la version nueva NO coinciden con los de la anterior';
END IF;
END $$;
