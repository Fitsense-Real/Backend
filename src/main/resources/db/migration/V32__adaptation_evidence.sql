-- =====================================================================
-- FitSense — V32 · EVIDENCIA DE ADAPTACION LOCALIZADA · v4
-- 17/10/2026
--
-- MOTIVO
--
-- Hoy un intento de adaptacion que falla NO DEJA RASTRO. Si la IA se cae, o
-- devuelve algo que las validaciones rechazan, PlanGenerationFailedException
-- escribe en el log y la semana queda sin plan: para la tesis, esa semana es
-- un hueco sin explicacion. Y si el intento sale bien, el unico registro es
-- el plan publicado, que no dice que se intento adaptar ni que parte.
--
-- La tabla que deberia servir, user_interventions, NO SIRVE, y no por una
-- razon sino por tres:
--
--   1. target_volume_change_pct y previous_week_volume son NOT NULL. En
--      adaptacion localizada no existe un cambio porcentual objetivo, asi que
--      habria que escribir ceros inventados en la tabla que sostiene la
--      evidencia de la tesis.
--   2. ck_ui_types_domain solo admite NONE, REDUCE_VOLUME, REDUCE_DURATION,
--      REDUCE_DAYS, LOWER_DIFFICULTY y LOWER_LOAD. La adaptacion localizada no
--      tiene tipo en esa lista.
--   3. uq_ui_metric UNIQUE (weekly_metric_id): UNA fila por semana medida. Un
--      intento con reintento y respaldo son varias cosas distintas.
--
-- Esa tabla se deja INTACTA. Describe el mecanismo anterior y cambiarle el
-- significado perderia la comparabilidad de lo ya registrado.
--
-- QUE TOCA ESTA MIGRACION, exactamente
--
--   1. CREA la tabla plan_adaptation_attempts, dos funciones y un trigger de
--      restriccion. No modifica ninguna fila de ninguna tabla existente.
--   2. AMPLIA ck_plan_source en weekly_training_plans para admitir
--      'CONSERVED'. Esto SI altera una restriccion existente y hay que
--      entenderlo (ver el bloque siguiente). No cambia ninguna fila.
--
-- No toca el catalogo, ni la configuracion, ni las metricas, ni
-- user_interventions.
--
-- POR QUE HACE FALTA 'CONSERVED'
--
-- El respaldo seguro y los desenlaces NO_CHANGE e INSUFFICIENT_DATA publican
-- una semana COPIADA de la anterior con fechas nuevas. Esa semana no la genero
-- la IA ni el motor de reglas: se conservo. Hoy ck_plan_source solo admite
-- 'AI' y 'RULE_ENGINE', asi que habria que declararla RULE_ENGINE, que es
-- falso.
--
-- Y no es un detalle cosmetico: la proporcion de RULE_ENGINE frente a AI es un
-- dato reportable de la tesis (20.6), y sirve para estimar cuanto depende el
-- sistema del modelo. Contar como RULE_ENGINE las semanas conservadas inflaria
-- esa cifra con semanas en las que el motor de reglas no hizo nada.
--
-- ACUMULA, NO REEMPLAZA
--
-- Sin restriccion de unicidad: cada corrida del ciclo deja su fila. El ciclo
-- semanal es idempotente -recalcular sobrescribe la misma fila de
-- weekly_user_metrics-, asi que volver a correr una semana anade intentos en
-- vez de borrar los anteriores. Es la opcion reversible: si despues se quiere
-- lo contrario, se agrega un UNIQUE; al contrario, los datos reemplazados no
-- vuelven. Para saber cual fue el ultimo, created_at y attempt_id ordenan.
--
-- QUE CAMBIA EN LA v4
--
--   G. RESPALDO Y SIN_ORDEN EXIGEN ORIGEN 'CONSERVED'. Los dos son continuidad
--      del plan anterior, no una generacion nueva. Sin esto se registraba un
--      RESPALDO con origen 'AI' sobre un plan que tambien decia 'AI': coherente
--      consigo mismo y falso, porque la tesis leeria como adaptacion del modelo
--      una semana que solo se copio.
--
-- QUE CAMBIA EN LA v3
--
--   D. LAS SESIONES DE LOS CUATRO ARREGLOS PERTENECEN AL PLAN DE ORIGEN. Un
--      arreglo no puede llevar claves foraneas, asi que se podia registrar una
--      sesion senalada inexistente, de otro usuario o de otra semana. Las
--      restricciones de subconjunto no lo cubrian: comparan los arreglos entre
--      si, no contra planned_workouts. Va en el mismo trigger.
--
--   E. EL ORIGEN DE GENERACION COINCIDE CON EL DEL PLAN PUBLICADO. Se podia
--      registrar un intento que dice 'AI' sobre un plan que dice 'CONSERVED'.
--      Eso vacia de sentido a CONSERVED, que se agrego justamente para que los
--      indicadores no atribuyan al modelo un plan copiado.
--
--   F. APLICADA Y SIN EFECTO SON EXCLUYENTES, por CHECK.
--
-- QUE CAMBIA EN LA v2
--
--   A. COHERENCIA ENTRE LAS CUATRO REFERENCIAS. Las cuatro claves foraneas
--      comprueban que lo referenciado EXISTA, no que pertenezca al mismo
--      participante. Sin esto se podia insertar un intento con la metrica de
--      un usuario, el plan de origen de otro y el resultado de un tercero, y
--      la base lo aceptaba. En una bitacora de evidencia eso es un defecto
--      grave, porque una fila incoherente no se distingue de una correcta al
--      consultarla. Lo resuelve fn_intento_coherente() en un trigger diferido,
--      como tg_pw_derivacion de V31.
--
--   B. COMBINACIONES CONTRADICTORIAS DE DECISION Y ESTADO. La v1 validaba
--      decision y status por separado, asi que admitia NO_CHANGE + APLICADA
--      -afirmar que se aplico una adaptacion que nadie pidio- y
--      ADAPT_LOCALIZED + SIN_ORDEN, que se contradice solo. Ahora el estado
--      depende de la decision, y RECHAZADA no puede llevar plan resultante:
--      si se rechazo un intento y luego se publico un respaldo, son DOS filas
--      con desenlaces distintos, que es justo lo que permite acumular.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. UNA FUNCION AUXILIAR PARA LA UNICIDAD DENTRO DEL ARREGLO
--
-- Un CHECK no puede llevar subconsulta, asi que la comprobacion de duplicados
-- va en una funcion IMMUTABLE, que si se puede invocar desde un CHECK. Es
-- deterministica sobre el arreglo, de modo que la inmutabilidad es real y no
-- declarada de conveniencia.
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_sin_duplicados(ids BIGINT[]) RETURNS BOOLEAN
    LANGUAGE sql IMMUTABLE PARALLEL SAFE AS $$
SELECT coalesce(cardinality(ids), 0)
           = (SELECT count(DISTINCT x) FROM unnest(coalesce(ids, '{}'::BIGINT[])) AS x)
           $$;

COMMENT ON FUNCTION fn_sin_duplicados(BIGINT[]) IS
    'V32: cierto si el arreglo no repite ningun identificador. Para CHECKs, que no admiten subconsulta.';

-- ---------------------------------------------------------------------
-- 2. LA TABLA
-- ---------------------------------------------------------------------
CREATE TABLE plan_adaptation_attempts (
                                          attempt_id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    -- Trazabilidad. weekly_metric_id es el ancla: la orden de adaptacion se
    -- decide a partir de la semana MEDIDA, no del plan.
                                          user_id                   BIGINT        NOT NULL,
                                          weekly_metric_id          BIGINT        NOT NULL,
                                          source_plan_id            BIGINT        NOT NULL,
                                          new_week_start_date       DATE          NOT NULL,

    -- NULL cuando no se publico nada. Es el caso que hoy no deja rastro.
                                          resulting_plan_id         BIGINT,

    -- Que decidio el decisor de adaptacion, y como acabo el intento.
                                          decision                  VARCHAR(20)   NOT NULL,
                                          status                    VARCHAR(20)   NOT NULL,
                                          generation_source         VARCHAR(20),
                                          ai_calls                  SMALLINT      NOT NULL DEFAULT 0,

    -- Que intento adaptar, por identificador persistente y no por fecha: con
    -- SCHEDULE_CHANGE la sesion cambia de dia legitimamente.
                                          targeted_sessions         BIGINT[]      NOT NULL DEFAULT '{}',
                                          targeted_interventions    VARCHAR(40)[] NOT NULL DEFAULT '{}',

    -- El desenlace por sesion, tal como lo separa ConservationReport.
                                          applied_sessions          BIGINT[]      NOT NULL DEFAULT '{}',
                                          no_effect_sessions        BIGINT[]      NOT NULL DEFAULT '{}',
                                          conserved_sessions        BIGINT[]      NOT NULL DEFAULT '{}',

    -- El informe completo de V8C, para poder revisar meses despues QUE se
    -- valido y no solo el veredicto.
                                          conservation_report       JSONB,

    -- Por que no se aplico. Obligatorio y NO VACIO cuando se rechazo o no fue
    -- factible: un rechazo sin motivo no es evidencia de nada.
                                          rejection_reason          TEXT,

    -- Las dos cifras con las que se decidio, congeladas aqui porque
    -- weekly_user_metrics es idempotente y un recalculo las cambiaria.
                                          observed_adherence_pct    NUMERIC(5,2),
                                          registration_coverage_pct NUMERIC(5,2),

                                          created_at                TIMESTAMPTZ   NOT NULL DEFAULT now(),

                                          CONSTRAINT fk_paa_user   FOREIGN KEY (user_id)
                                              REFERENCES users (user_id),
                                          CONSTRAINT fk_paa_metric FOREIGN KEY (weekly_metric_id)
                                              REFERENCES weekly_user_metrics (weekly_metric_id),
                                          CONSTRAINT fk_paa_source FOREIGN KEY (source_plan_id)
                                              REFERENCES weekly_training_plans (plan_id),
                                          CONSTRAINT fk_paa_result FOREIGN KEY (resulting_plan_id)
                                              REFERENCES weekly_training_plans (plan_id),

    -- Los cuatro desenlaces de AdaptationOutcome.
                                          CONSTRAINT ck_paa_decision CHECK (decision IN
                                                                            ('NO_CHANGE', 'INSUFFICIENT_DATA', 'ADAPT_LOCALIZED', 'ADAPT_EXPANDED')),

    -- Como acabo el intento:
    --   APLICADA     la IA propuso, se valido y se publico
    --   SIN_EFECTO   las senaladas volvieron iguales; se publico igualmente
    --   RECHAZADA    la propuesta no paso las validaciones y NO se publico
    --   RESPALDO     la IA fallo o no estaba; se publico la continuidad segura
    --   NO_FACTIBLE  no se pudo publicar nada
    --   SIN_ORDEN    NO_CHANGE o INSUFFICIENT_DATA: no habia nada que adaptar
                                          CONSTRAINT ck_paa_status CHECK (status IN
                                                                          ('APLICADA', 'SIN_EFECTO', 'RECHAZADA', 'RESPALDO', 'NO_FACTIBLE', 'SIN_ORDEN')),

    -- v2.B · EL ESTADO DEPENDE DE LA DECISION.
    --
    -- Sin esto se admitia NO_CHANGE + APLICADA, que afirma haber aplicado una
    -- adaptacion que nadie ordeno, y ADAPT_LOCALIZED + SIN_ORDEN, que niega la
    -- orden que la propia fila declara.
    --
    -- NO_FACTIBLE vale para las dos ramas a proposito: conservar el plan
    -- anterior tambien puede fallar -si el conjunto elegible cambio, la copia
    -- puede incumplir 4C o 4D-, y entonces no se publica nada aunque no se
    -- hubiera pedido adaptar.
                                          CONSTRAINT ck_paa_estado_segun_decision CHECK (
                                              (decision IN ('NO_CHANGE', 'INSUFFICIENT_DATA')
                                                  AND status IN ('SIN_ORDEN', 'NO_FACTIBLE'))
                                                  OR (decision IN ('ADAPT_LOCALIZED', 'ADAPT_EXPANDED')
                                                  AND status IN ('APLICADA', 'SIN_EFECTO', 'RECHAZADA',
                                                                 'RESPALDO', 'NO_FACTIBLE'))),

    -- v2.B · EL PLAN RESULTANTE EXISTE EXACTAMENTE CUANDO SE PUBLICO ALGO.
    --
    -- Es una equivalencia, no dos reglas sueltas: antes se podia registrar
    -- RECHAZADA con plan resultante, y esa fila no dice que se publico. Un
    -- intento rechazado y el respaldo que despues se publico son DOS filas.
                                          CONSTRAINT ck_paa_plan_segun_estado CHECK (
                                              (status IN ('APLICADA', 'SIN_EFECTO', 'RESPALDO', 'SIN_ORDEN')
                                                  AND resulting_plan_id IS NOT NULL)
                                                  OR (status IN ('RECHAZADA', 'NO_FACTIBLE')
                                                  AND resulting_plan_id IS NULL)),

    -- Si se publico, consta de donde salio; si no se publico, no hay origen.
    --
    -- El IS NOT NULL explicito NO es redundante: un CHECK se cumple cuando su
    -- expresion vale NULL, asi que sin el, un plan publicado con origen nulo
    -- daria NULL IN (...) = NULL y pasaria. Es la trampa de la logica
    -- trivaluada y hay una prueba para ella.
                                          CONSTRAINT ck_paa_gen_source CHECK (
                                              (resulting_plan_id IS NULL AND generation_source IS NULL)
                                                  OR (resulting_plan_id IS NOT NULL
                                                  AND generation_source IS NOT NULL
                                                  AND generation_source IN ('AI', 'RULE_ENGINE', 'CONSERVED'))),

    -- v4 · LA PROCEDENCIA DE LA CONTINUIDAD SEGURA.
    --
    -- RESPALDO y SIN_ORDEN son, por definicion, continuidad del plan anterior:
    -- no una generacion nueva. Sin esto se podia registrar un RESPALDO con
    -- origen 'AI' sobre un plan publicado que tambien dice 'AI', y el trigger lo
    -- aceptaba porque los dos origenes coinciden. Coherente consigo mismo y
    -- falso: la tesis leeria como adaptacion del modelo una semana copiada.
    --
    -- El IS NOT NULL es deliberado. Sin el, un RESPALDO con origen nulo daria
    -- FALSE OR NULL = NULL y un CHECK se cumple cuando vale NULL. Otras dos
    -- restricciones ya lo cerrarian por separado, pero depender de que
    -- interactuen es justo el descuido que dejo pasar la trampa trivaluada de la
    -- version anterior.
                                          CONSTRAINT ck_paa_procedencia_continuidad CHECK (
                                              status NOT IN ('RESPALDO', 'SIN_ORDEN')
                                                  OR (generation_source IS NOT NULL AND generation_source = 'CONSERVED')),

                                          CONSTRAINT ck_paa_ai_calls CHECK (ai_calls BETWEEN 0 AND 4),

    -- Las dos listas van en paralelo: cada sesion senalada con su tipo. En
    -- PostgreSQL array_length de un arreglo vacio es NULL, de ahi el coalesce.
                                          CONSTRAINT ck_paa_pareja CHECK (
                                              coalesce(array_length(targeted_sessions, 1), 0)
                                                  = coalesce(array_length(targeted_interventions, 1), 0)),

    -- v2.B · Una sesion no puede estar senalada dos veces.
                                          CONSTRAINT ck_paa_senaladas_unicas CHECK (fn_sin_duplicados(targeted_sessions)),

    -- Los siete valores de TipoIntervencion. Si se agrega uno, esta
    -- restriccion es lo que obliga a decidirlo aqui tambien.
                                          CONSTRAINT ck_paa_intervenciones CHECK (targeted_interventions <@ ARRAY[
                                              'REPROGRAMAR_DIAS', 'ACORTAR_O_REORGANIZAR', 'VARIANTE_MAS_SENCILLA_O_CARGA',
    'SEGURIDAD_ANTE_DOLOR', 'REVISAR_CARGA_Y_RECUPERACION',
    'INVESTIGAR_CAMBIO_MINIMO', 'CAUSA_NO_REGISTRADA']::VARCHAR(40)[]),

    -- Si se adapto, hay al menos una sesion senalada. Si no se adapto, no hay
    -- ninguna. Las dos direcciones, porque las dos serian un dato incoherente.
    CONSTRAINT ck_paa_senaladas_coherentes CHECK (
        (decision IN ('ADAPT_LOCALIZED', 'ADAPT_EXPANDED')
             AND coalesce(array_length(targeted_sessions, 1), 0) >= 1)
        OR (decision IN ('NO_CHANGE', 'INSUFFICIENT_DATA')
             AND coalesce(array_length(targeted_sessions, 1), 0) = 0)),

    -- El desenlace por sesion tiene que cuadrar con lo senalado: lo aplicado y
    -- lo que quedo sin efecto son sesiones SENALADAS, y lo conservado son las
    -- que NO se senalaron.
    CONSTRAINT ck_paa_desenlace_subconjunto CHECK (
        targeted_sessions @> (applied_sessions || no_effect_sessions)),
    CONSTRAINT ck_paa_conservadas_disjuntas CHECK (
        NOT (conserved_sessions && targeted_sessions)),

    -- v3 · APLICADA Y SIN EFECTO SON EXCLUYENTES. Una sesion senalada cambio o
    -- no cambio; no puede constar en las dos listas. Sin esto, una fila podia
    -- contarse a la vez como adaptacion lograda y como adaptacion sin efecto, y
    -- eso falsea el recuento del que depende la tesis.
    CONSTRAINT ck_paa_aplicada_o_sin_efecto CHECK (
        NOT (applied_sessions && no_effect_sessions)),

    -- Un rechazo o un no factible sin motivo escrito no es evidencia. Y vacio
    -- no es escrito.
    CONSTRAINT ck_paa_motivo_si_fallo CHECK (
        status NOT IN ('RECHAZADA', 'NO_FACTIBLE')
        OR (rejection_reason IS NOT NULL AND btrim(rejection_reason) <> '')),

    -- La semana derivada es un plan NUEVO, nunca el mismo del que deriva.
    CONSTRAINT ck_paa_resultado_distinto CHECK (
        resulting_plan_id IS NULL OR resulting_plan_id <> source_plan_id),

    CONSTRAINT ck_paa_porcentajes CHECK (
        (observed_adherence_pct IS NULL OR observed_adherence_pct BETWEEN 0 AND 100)
        AND (registration_coverage_pct IS NULL
             OR registration_coverage_pct BETWEEN 0 AND 100))
);

COMMENT ON TABLE plan_adaptation_attempts IS
    'Un intento de adaptacion localizada y como acabo, INCLUIDOS los que no publicaron plan. Acumula: cada corrida del ciclo deja su fila, sin unicidad.';
COMMENT ON COLUMN plan_adaptation_attempts.weekly_metric_id IS
    'La semana MEDIDA de la que salio la orden. Es el ancla del intento.';
COMMENT ON COLUMN plan_adaptation_attempts.resulting_plan_id IS
    'NULL cuando no se publico nada. Es justo el caso que antes no dejaba rastro.';
COMMENT ON COLUMN plan_adaptation_attempts.conservation_report IS
    'El informe de V8C completo: aplicadas, sin efecto, conservadas y problemas.';
COMMENT ON COLUMN plan_adaptation_attempts.observed_adherence_pct IS
    'Congelada aqui a proposito: weekly_user_metrics es idempotente y un recalculo cambiaria la cifra con la que se decidio.';

CREATE INDEX ix_paa_user_week ON plan_adaptation_attempts (user_id, new_week_start_date);
CREATE INDEX ix_paa_metric    ON plan_adaptation_attempts (weekly_metric_id);

-- ---------------------------------------------------------------------
-- 3. v2.A · COHERENCIA ENTRE LAS CUATRO REFERENCIAS
--
-- Las claves foraneas comprueban EXISTENCIA, no pertenencia. Sin esto se podia
-- insertar un intento con la metrica de un usuario, el plan de origen de otro
-- y el resultado de un tercero, y la base lo aceptaba: una fila incoherente
-- que al consultarla no se distingue de una correcta.
--
-- Va en un trigger de restriccion y no en CHECKs porque hay que mirar otras
-- tablas, y DEFERRABLE INITIALLY DEFERRED como tg_pw_derivacion de V31, para
-- que insertar el plan resultante y el intento en la misma transaccion
-- funcione sin depender del orden de las sentencias.
-- ---------------------------------------------------------------------
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
    || NEW.no_effect_sessions || NEW.conserved_sessions) AS s(id)
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
    'V32: metrica, plan de origen y plan resultante pertenecen al MISMO usuario; el origen es el de la semana medida; las sesiones de los cuatro arreglos pertenecen al plan de origen; el origen de generacion coincide con el del plan publicado; la semana nueva es posterior y coincide con la del plan resultante.';

CREATE CONSTRAINT TRIGGER tg_paa_coherencia
    AFTER INSERT OR UPDATE ON plan_adaptation_attempts
                                              DEFERRABLE INITIALLY DEFERRED
                                              FOR EACH ROW EXECUTE FUNCTION fn_intento_coherente();

-- ---------------------------------------------------------------------
-- 4. 'CONSERVED' COMO ORIGEN DE GENERACION
--
-- Una semana copiada de la anterior no la genero nadie. Sin esto habria que
-- declararla RULE_ENGINE, que es falso y ademas infla una cifra reportable.
-- ---------------------------------------------------------------------
ALTER TABLE weekly_training_plans DROP CONSTRAINT ck_plan_source;
ALTER TABLE weekly_training_plans ADD CONSTRAINT ck_plan_source
    CHECK (generation_source IN ('AI', 'RULE_ENGINE', 'CONSERVED'));

COMMENT ON COLUMN weekly_training_plans.generation_source IS
    'AI, RULE_ENGINE o CONSERVED. CONSERVED es una semana copiada de la anterior con fechas nuevas: no la genero ni el modelo ni el motor de reglas.';

-- ---------------------------------------------------------------------
-- 5. COMPROBACION
-- ---------------------------------------------------------------------
DO $$
DECLARE
tabla         INTEGER;
    restricciones INTEGER;
    indices       INTEGER;
    triggers      INTEGER;
    admite        BOOLEAN;
BEGIN
SELECT COUNT(*) INTO tabla FROM information_schema.tables
WHERE table_schema = 'public' AND table_name = 'plan_adaptation_attempts';
IF tabla <> 1 THEN
        RAISE EXCEPTION 'V32: la tabla no quedo creada';
END IF;

SELECT COUNT(*) INTO restricciones FROM pg_constraint
WHERE conrelid = 'plan_adaptation_attempts'::regclass
       AND conname IN ('fk_paa_user', 'fk_paa_metric', 'fk_paa_source', 'fk_paa_result',
                       'ck_paa_decision', 'ck_paa_status', 'ck_paa_gen_source',
                       'ck_paa_ai_calls', 'ck_paa_pareja', 'ck_paa_intervenciones',
                       'ck_paa_senaladas_coherentes', 'ck_paa_senaladas_unicas',
                       'ck_paa_estado_segun_decision', 'ck_paa_plan_segun_estado',
                       'ck_paa_desenlace_subconjunto', 'ck_paa_conservadas_disjuntas',
                       'ck_paa_aplicada_o_sin_efecto', 'ck_paa_procedencia_continuidad',
                       'ck_paa_motivo_si_fallo', 'ck_paa_resultado_distinto',
                       'ck_paa_porcentajes');
IF restricciones <> 21 THEN
        RAISE EXCEPTION 'V32: se esperaban 21 restricciones nombradas y hay %', restricciones;
END IF;

SELECT COUNT(*) INTO indices FROM pg_indexes
WHERE tablename = 'plan_adaptation_attempts'
  AND indexname IN ('ix_paa_user_week', 'ix_paa_metric');
IF indices <> 2 THEN
        RAISE EXCEPTION 'V32: faltan indices, hay %', indices;
END IF;

SELECT COUNT(*) INTO triggers FROM pg_trigger
WHERE tgrelid = 'plan_adaptation_attempts'::regclass
       AND tgname = 'tg_paa_coherencia'
       AND tgdeferrable AND tginitdeferred;
IF triggers <> 1 THEN
        RAISE EXCEPTION 'V32: el trigger diferido de coherencia no quedo creado';
END IF;

    -- Y que NO haya unicidad: la tabla acumula.
SELECT COUNT(*) = 0 INTO admite FROM pg_constraint
WHERE conrelid = 'plan_adaptation_attempts'::regclass AND contype = 'u';
IF NOT admite THEN
        RAISE EXCEPTION 'V32: hay una restriccion de unicidad y la tabla debe acumular';
END IF;

SELECT pg_get_constraintdef(oid) LIKE '%CONSERVED%' INTO admite FROM pg_constraint
WHERE conrelid = 'weekly_training_plans'::regclass AND conname = 'ck_plan_source';
IF NOT admite THEN
        RAISE EXCEPTION 'V32: ck_plan_source no admite CONSERVED';
END IF;
END $$;
