-- =====================================================================
-- FitSense — V31 · DERIVACION DE ENTRENAMIENTOS ENTRE SEMANAS
-- 17/10/2026
--
-- MOTIVO
--
-- La validacion de conservacion que sustituye a V8 tiene que comparar la
-- semana derivada con la original: comprobar que las sesiones NO senaladas
-- quedaron identicas y que dentro de las senaladas solo ocurrio lo
-- permitido. Para eso necesita saber de que sesion anterior proviene cada
-- sesion nueva, y hoy no hay forma de saberlo.
--
-- El linaje existente no sirve: parent_plan_id y plan_version, en
-- weekly_training_plans, relacionan VERSIONES DE LA MISMA SEMANA, no una
-- semana con la siguiente.
--
-- Y las claves que parecian servir, no sirven:
--
--   display_order  se desplaza en cuanto REDUCE_DAYS quita un dia: de tres
--                  sesiones a dos, el orden 3 desaparece y los demas se
--                  mueven.
--   focus_code     se repite: una semana con tres dias FULL_BODY da tres
--                  candidatos indistinguibles.
--   scheduled_date cambia entre semanas por definicion, y encima el esquema
--                  NO impide dos entrenamientos el mismo dia -la unica
--                  restriccion de unicidad es uq_planned_workout_order, por
--                  display_order-, asi que la fecha no identifica
--                  univocamente ni dentro de la misma semana.
--
-- Las tres emparejarian mal EN SILENCIO, que es el peor fallo posible en una
-- validacion de conservacion: daria por conservado algo que cambio, y el
-- criterio de exito -cambio la parte problematica y el resto se conservo-
-- quedaria sin demostrar aunque la validacion pasara.
--
-- QUE HACE
--
-- Una columna que el constructor del plan derivado rellena AL COPIAR cada
-- sesion anterior. La relacion se construye, no se deduce al final sobre un
-- plan generado desde cero: por eso vale aunque cambien la fecha, el orden y
-- el enfoque.
--
-- A nivel de ejercicio no hace falta nada: dentro de una sesion ya emparejada
-- los ejercicios se reconocen por exercise_id, que es el del catalogo.
--
-- QUE TOCA
--
--   1. DDL sobre planned_workouts: una columna, tres restricciones y un
--      trigger de restriccion. No modifica ninguna fila.
--   2. Nada mas. No toca el catalogo, ni la configuracion, ni ninguna otra
--      tabla.
--
-- La columna es NULL-able y queda NULL en todo lo existente: ningun plan
-- anterior a V31 deriva de nada, y eso es cierto, no un hueco.
-- =====================================================================

ALTER TABLE planned_workouts
    ADD COLUMN source_planned_workout_id BIGINT;

COMMENT ON COLUMN planned_workouts.source_planned_workout_id IS
    'Entrenamiento de la semana anterior del que proviene este. NULL en un plan no derivado. Lo rellena el constructor del plan derivado al copiar, no se deduce por fecha ni por orden.';

ALTER TABLE planned_workouts
    ADD CONSTRAINT fk_pw_source
        FOREIGN KEY (source_planned_workout_id)
            REFERENCES planned_workouts (planned_workout_id),

    -- Dos sesiones del mismo plan no pueden derivar de la misma sesion
    -- anterior: la correspondencia es uno a uno. En PostgreSQL los NULL son
    -- distintos entre si, asi que un plan sin derivacion -todas NULL- no
    -- choca con esto.
    ADD CONSTRAINT uq_pw_source_per_plan
        UNIQUE (plan_id, source_planned_workout_id),

    ADD CONSTRAINT ck_pw_source_not_self
        CHECK (source_planned_workout_id IS NULL
               OR source_planned_workout_id <> planned_workout_id);

-- ---------------------------------------------------------------------
-- Coherencia de la derivacion: mismo usuario, y la semana origen ANTERIOR
-- a la derivada.
--
-- Va en un trigger de restriccion y no en un CHECK porque un CHECK no puede
-- mirar otras tablas, y los dos datos -usuario y semana- viven en
-- weekly_training_plans. DEFERRABLE INITIALLY DEFERRED, como
-- tg_exercises_principal de V23: se evalua al COMMIT, de modo que insertar el
-- plan y sus entrenamientos en la misma transaccion funciona sin depender del
-- orden de las sentencias.
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_derivacion_coherente() RETURNS TRIGGER AS $$
DECLARE
usuario_origen   BIGINT;
    usuario_derivado BIGINT;
    semana_origen    DATE;
    semana_derivada  DATE;
BEGIN
    IF NEW.source_planned_workout_id IS NULL THEN
        RETURN NULL;
END IF;

SELECT p.user_id, p.week_start_date INTO usuario_derivado, semana_derivada
FROM weekly_training_plans p
WHERE p.plan_id = NEW.plan_id;

SELECT p.user_id, p.week_start_date INTO usuario_origen, semana_origen
FROM planned_workouts w
         JOIN weekly_training_plans p ON p.plan_id = w.plan_id
WHERE w.planned_workout_id = NEW.source_planned_workout_id;

IF usuario_origen IS NULL THEN
        RAISE EXCEPTION 'V31: el entrenamiento % dice derivar del % y ese origen no existe',
            NEW.planned_workout_id, NEW.source_planned_workout_id;
END IF;

    IF usuario_origen <> usuario_derivado THEN
        RAISE EXCEPTION 'V31: el entrenamiento % (usuario %) no puede derivar del % (usuario %)',
            NEW.planned_workout_id, usuario_derivado,
            NEW.source_planned_workout_id, usuario_origen;
END IF;

    IF semana_origen >= semana_derivada THEN
        RAISE EXCEPTION 'V31: el entrenamiento % (semana del %) no puede derivar del % '
            'porque su semana (%) no es anterior',
            NEW.planned_workout_id, semana_derivada,
            NEW.source_planned_workout_id, semana_origen;
END IF;

RETURN NULL;
END $$ LANGUAGE plpgsql;

COMMENT ON FUNCTION fn_derivacion_coherente() IS
    'V31: una derivacion solo vale entre entrenamientos del mismo usuario y desde una semana anterior.';

CREATE CONSTRAINT TRIGGER tg_pw_derivacion
    AFTER INSERT OR UPDATE ON planned_workouts
                                              DEFERRABLE INITIALLY DEFERRED
                                              FOR EACH ROW EXECUTE FUNCTION fn_derivacion_coherente();

-- ---------------------------------------------------------------------
-- Comprobacion: la columna, las tres restricciones y el trigger existen.
-- ---------------------------------------------------------------------
DO $$
DECLARE
columnas     INTEGER;
    restricciones INTEGER;
    triggers      INTEGER;
BEGIN
SELECT COUNT(*) INTO columnas FROM information_schema.columns
WHERE table_name = 'planned_workouts'
  AND column_name = 'source_planned_workout_id';
IF columnas <> 1 THEN
        RAISE EXCEPTION 'V31: la columna no quedo creada';
END IF;

SELECT COUNT(*) INTO restricciones FROM pg_constraint
WHERE conrelid = 'planned_workouts'::regclass
   AND conname IN ('fk_pw_source', 'uq_pw_source_per_plan', 'ck_pw_source_not_self');
IF restricciones <> 3 THEN
        RAISE EXCEPTION 'V31: se esperaban 3 restricciones y hay %', restricciones;
END IF;

SELECT COUNT(*) INTO triggers FROM pg_trigger
WHERE tgrelid = 'planned_workouts'::regclass
   AND tgname = 'tg_pw_derivacion'
   AND tgdeferrable AND tginitdeferred;
IF triggers <> 1 THEN
        RAISE EXCEPTION 'V31: el trigger diferido no quedo creado';
END IF;
END $$;
