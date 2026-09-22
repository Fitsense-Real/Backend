-- =====================================================================
-- FitSense MVP 1.0 - V23: requisitos de equipamiento por ejercicio (N:M)
--
-- POR QUE
-- exercises.equipment_id solo puede guardar un implemento, y 120 ejercicios
-- del catalogo (105 activos) necesitan dos o mas a la vez: "dumbbell bench
-- press" necesita mancuerna Y banco, "cable fly on exercise ball" necesita
-- polea Y balon. Con un solo campo habia que elegir uno, y el otro se perdia:
-- quien declaraba banco sin mancuernas recibia igual el press con mancuerna.
--
-- EL MODELO
-- Cada fila es un implemento aceptado para un grupo de requisitos:
--
--   - grupos DISTINTOS se combinan con AND: hacen falta todos;
--   - implementos del MISMO grupo se combinan con OR: basta uno.
--
--   dumbbell bench press      grupo 1: dumbbell
--                             grupo 2: bench             -> dumbbell AND bench
--
--   inverted row              grupo 1: pull-up bar
--                             grupo 1: suspension straps -> barra O correas
--
-- Un ejercicio sin filas no exige nada: es peso corporal y siempre pasa.
-- Por eso 'body weight' no se siembra como requisito.
--
-- La regla de elegibilidad es:
--   el participante puede hacer el ejercicio si NO existe ningun grupo del
--   que no tenga al menos un implemento.
--
-- QUE HACE ESTA MIGRACION
-- Crea la tabla y la siembra con el equipment_id actual de cada ejercicio, en
-- el grupo 1, salvo peso corporal. Es un REFACTOR PURO: para cualquier perfil,
-- el conjunto elegible que sale de esta tabla es identico al que salia de
-- equipment_id. Los requisitos reales (los segundos grupos, las alternativas
-- OR) los anade V24 desde la revision del catalogo.
--
-- QUE PASA CON exercises.equipment_id
-- Se queda, con otro significado: el implemento PRINCIPAL, el que se muestra en
-- la app y el que lee el prompt. Ya no decide la elegibilidad; eso lo hace
-- esta tabla. Mantenerlo evita tocar el catalogo de la app y el prompt en la
-- misma migracion.
--
-- NO editar ni reformatear este archivo despues de aplicarlo
-- (checksum de Flyway, incidente de V18 y V19).
-- =====================================================================

CREATE TABLE exercise_equipment_requirements (
                                                 exercise_id  BIGINT   NOT NULL,
                                                 group_no     SMALLINT NOT NULL,
                                                 equipment_id SMALLINT NOT NULL,

                                                 CONSTRAINT pk_exercise_equipment_requirements
                                                     PRIMARY KEY (exercise_id, group_no, equipment_id),
                                                 CONSTRAINT fk_eer_exercise
                                                     FOREIGN KEY (exercise_id) REFERENCES exercises (exercise_id) ON DELETE CASCADE,
                                                 CONSTRAINT fk_eer_equipment
                                                     FOREIGN KEY (equipment_id) REFERENCES equipment_types (equipment_id),
                                                 CONSTRAINT ck_eer_group_positive CHECK (group_no >= 1)
);

COMMENT ON TABLE exercise_equipment_requirements IS
    'Requisitos de equipamiento. Grupos distintos = AND; mismo grupo = OR. Sin filas = peso corporal.';
COMMENT ON COLUMN exercises.equipment_id IS
    'Implemento PRINCIPAL, para mostrar. Desde V23 la elegibilidad la decide exercise_equipment_requirements.';

-- Indice de la consulta de elegibilidad: se recorre por ejercicio y grupo.
CREATE INDEX ix_eer_exercise_group ON exercise_equipment_requirements (exercise_id, group_no);


-- SIEMBRA: un grupo por ejercicio con su equipment_id actual. Peso corporal
-- no se siembra: sin filas significa "no exige nada".
INSERT INTO exercise_equipment_requirements (exercise_id, group_no, equipment_id)
SELECT e.exercise_id, 1, e.equipment_id
FROM exercises e
         JOIN equipment_types eq ON eq.equipment_id = e.equipment_id
WHERE eq.code <> 'body weight';


-- INVARIANTE: el implemento principal tiene que ser coherente con los
-- requisitos. Sin esto, dentro de seis meses alguien puede dejar
-- "dumbbell bench press" con requisitos mancuerna + banco y principal
-- kettlebell, y la app mostraria un implemento que el ejercicio no usa.
--
--   - si el ejercicio TIENE requisitos, su equipment_id tiene que ser una de
--     las alternativas de alguno de sus grupos;
--   - si NO tiene requisitos (peso corporal), su equipment_id tiene que ser
--     'body weight'.
--
-- Es un trigger DIFERIDO: se comprueba al final de la transaccion, no en cada
-- sentencia. Asi una migracion puede borrar los requisitos de un ejercicio y
-- escribir los nuevos sin fallar a medio camino. Flyway ejecuta cada
-- migracion en una transaccion; a mano, con psql, hay que usar -1
-- (--single-transaction) o BEGIN/COMMIT.
CREATE FUNCTION fn_principal_en_requisitos() RETURNS trigger
    LANGUAGE plpgsql AS $$
DECLARE
ids       BIGINT[];
    eid       BIGINT;
    principal SMALLINT;
    codigo    VARCHAR(40);
    filas     BOOLEAN;
    esta      BOOLEAN;
BEGIN
    IF TG_TABLE_NAME = 'exercises' THEN
        ids := ARRAY[NEW.exercise_id];
    ELSIF TG_OP = 'DELETE' THEN
        ids := ARRAY[OLD.exercise_id];
    ELSIF TG_OP = 'UPDATE' THEN
        ids := ARRAY[OLD.exercise_id, NEW.exercise_id];
ELSE
        ids := ARRAY[NEW.exercise_id];
END IF;

    FOREACH eid IN ARRAY ids LOOP
SELECT e.equipment_id, eq.code INTO principal, codigo
FROM exercises e
         JOIN equipment_types eq ON eq.equipment_id = e.equipment_id
WHERE e.exercise_id = eid;
CONTINUE WHEN NOT FOUND;   -- el ejercicio se borro en la transaccion

SELECT EXISTS (SELECT 1 FROM exercise_equipment_requirements r
               WHERE r.exercise_id = eid),
       EXISTS (SELECT 1 FROM exercise_equipment_requirements r
               WHERE r.exercise_id = eid AND r.equipment_id = principal)
INTO filas, esta;

IF filas AND NOT esta THEN
            RAISE EXCEPTION 'Ejercicio %: su implemento principal (%) no esta entre sus requisitos',
                eid, codigo;
END IF;
        IF NOT filas AND codigo <> 'body weight' THEN
            RAISE EXCEPTION 'Ejercicio %: no exige ningun implemento pero se muestra como %',
                eid, codigo;
END IF;
END LOOP;
RETURN NULL;
END $$;

CREATE CONSTRAINT TRIGGER tg_requisitos_principal
    AFTER INSERT OR UPDATE OR DELETE ON exercise_equipment_requirements
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION fn_principal_en_requisitos();

CREATE CONSTRAINT TRIGGER tg_exercises_principal
    AFTER INSERT OR UPDATE OF equipment_id ON exercises
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION fn_principal_en_requisitos();


-- COMPROBACION DURA: la siembra tiene que reproducir exactamente el modelo
-- anterior. Cada ejercicio que no es de peso corporal debe tener una y solo
-- una fila, y debe ser su equipment_id.
DO $$
DECLARE
esperados  INTEGER;
    sembrados  INTEGER;
    distintos  INTEGER;
BEGIN
SELECT COUNT(*) INTO esperados
FROM exercises e JOIN equipment_types eq ON eq.equipment_id = e.equipment_id
WHERE eq.code <> 'body weight';

SELECT COUNT(*) INTO sembrados FROM exercise_equipment_requirements;

SELECT COUNT(*) INTO distintos
FROM exercise_equipment_requirements r
         JOIN exercises e ON e.exercise_id = r.exercise_id
WHERE r.equipment_id <> e.equipment_id OR r.group_no <> 1;

IF sembrados <> esperados OR distintos <> 0 THEN
        RAISE EXCEPTION 'V23: siembra incorrecta (esperados %, sembrados %, distintos %)',
            esperados, sembrados, distintos;
END IF;

    RAISE NOTICE 'V23: % requisitos sembrados desde equipment_id', sembrados;
END $$;
