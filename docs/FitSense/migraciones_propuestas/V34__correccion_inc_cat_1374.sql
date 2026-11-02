-- =====================================================================
-- FitSense MVP 1.0 - V34 (PROPUESTA, NO DESPLEGADA): correccion INC-CAT-1374
--
-- ESTADO: propuesta. Vive en docs/FitSense/migraciones_propuestas/ y NO en
-- db/migration, para que Flyway no la aplique. Para desplegarla hay que
-- copiarla a src/main/resources/db/migration/ con el siguiente numero libre,
-- despues de:
--   1. medir la exposicion real con las consultas de solo lectura de
--      docs/FitSense/INC_CAT_1374.md;
--   2. decidir el tratamiento de P2-D de las semanas que la contienen.
-- Se prueba en PostgreSQL efimero con MigracionPropuesta1374IT.
--
-- QUE CORRIGE
-- 1374 «wind sprints» (source_code 0858). Su GIF muestra una elevacion
-- alterna de rodillas COLGADO de una barra de dominadas; sus instrucciones
-- describian un sprint en pista y su registro decia que no exige colgarse,
-- ni brazos arriba, ni barra, y que es de alto impacto. Con ese dato los
-- filtros de seguridad no lo ven
-- (docs/FitSense/REVISION_CATALOGO_CARDIO.md).
--
-- Valores, los mismos que los ejercicios colgados equivalentes 61, 62 y 867:
--   movement_pattern   LOCOMOTION_CARDIO -> TRUNK_FLEXION (61 y 62)
--   requires_hanging   false -> true
--   requires_overhead  false -> true  (ck_exercises_hanging_is_overhead)
--   high_impact        true  -> false
--   equipment_id       body weight -> pull-up bar
--   requisitos         ninguno -> grupo 1: pull-up bar
--   instrucciones      reescritas segun el GIF
-- NO cambia: dificultad, zona, musculo objetivo, prescripcion, actividad.
--
-- COMO LO HACE
-- Igual que V21, V24 y V26: identifica el ejercicio por id, source_code y
-- name_en, y aborta sin tocar nada si algo no coincide. Reaplicarla no cambia
-- nada. tg_exercises_principal (V23) es diferido: el implemento principal y
-- su requisito se comprueban juntos al COMMIT.
-- =====================================================================

-- Requisitos: V26 (movement_pattern) y V25 (banderas) aplicadas, y el codigo
-- de equipamiento que se usa existe.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                    WHERE table_name = 'exercises' AND column_name = 'movement_pattern') THEN
        RAISE EXCEPTION 'INC-CAT-1374: falta exercises.movement_pattern. ¿Se aplico V26?';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM equipment_types WHERE code = 'pull-up bar') THEN
        RAISE EXCEPTION 'INC-CAT-1374: falta el codigo de equipamiento pull-up bar. ¿Se aplico V22?';
    END IF;
END $$;

-- Identidad del ejercicio: si no es el que se reviso, no se toca nada.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM exercises
                    WHERE exercise_id = 1374 AND source_code = '0858' AND name_en = 'wind sprints') THEN
        RAISE EXCEPTION 'INC-CAT-1374: el ejercicio 1374 no es wind sprints (0858). No se aplica nada.';
    END IF;
END $$;

UPDATE exercises
   SET movement_pattern   = 'TRUNK_FLEXION',
       is_isolation       = FALSE,
       requires_hanging   = TRUE,
       requires_overhead  = TRUE,
       high_impact        = FALSE,
       equipment_id       = (SELECT equipment_id FROM equipment_types WHERE code = 'pull-up bar'),
       instructions_en    = 'Hang from a pull-up bar with an overhand grip, hands shoulder-width apart and arms fully extended. Keep your torso still and your core braced. Bring one knee up towards your chest, then lower it under control while you raise the other knee, alternating sides in a steady rhythm. Avoid swinging your body. Continue for the desired number of repetitions.',
       instructions_es    = 'Cuélgate de una barra de dominadas con agarre prono, las manos a la anchura de los hombros y los brazos completamente extendidos. Mantén el torso quieto y el abdomen activo. Lleva una rodilla hacia el pecho y bájala de forma controlada mientras elevas la otra, alternando los lados con un ritmo constante. Evita balancear el cuerpo. Continúa durante el número de repeticiones deseado.',
       instruction_steps_es = '["Cuélgate de una barra de dominadas con agarre prono, las manos a la anchura de los hombros y los brazos completamente extendidos.", "Mantén el torso quieto y el abdomen activo.", "Lleva una rodilla hacia el pecho y bájala de forma controlada mientras elevas la otra, alternando los lados con un ritmo constante.", "Evita balancear el cuerpo.", "Continúa durante el número de repeticiones deseado."]'::JSONB
 WHERE exercise_id = 1374;

-- Requisitos reescritos: solo la barra.
DELETE FROM exercise_equipment_requirements WHERE exercise_id = 1374;
INSERT INTO exercise_equipment_requirements (exercise_id, group_no, equipment_id)
SELECT 1374, 1, equipment_id FROM equipment_types WHERE code = 'pull-up bar';

-- Verificacion final: si algo no quedo como se pretende, aborta todo.
DO $$
DECLARE
    e RECORD;
    requisitos TEXT;
BEGIN
    SELECT movement_pattern, requires_hanging, requires_overhead, high_impact,
           (SELECT code FROM equipment_types t WHERE t.equipment_id = x.equipment_id) AS principal
      INTO e FROM exercises x WHERE exercise_id = 1374;
    SELECT string_agg(r.group_no || ':' || t.code, ',' ORDER BY r.group_no, t.code) INTO requisitos
      FROM exercise_equipment_requirements r JOIN equipment_types t ON t.equipment_id = r.equipment_id
     WHERE r.exercise_id = 1374;
    IF e.movement_pattern <> 'TRUNK_FLEXION' OR NOT e.requires_hanging OR NOT e.requires_overhead
       OR e.high_impact OR e.principal <> 'pull-up bar' OR requisitos IS DISTINCT FROM '1:pull-up bar' THEN
        RAISE EXCEPTION 'INC-CAT-1374: verificacion fallida (%, requisitos %)', e, requisitos;
    END IF;
    RAISE NOTICE 'INC-CAT-1374 aplicada: 1374 colgado, brazos arriba, sin alto impacto, barra obligatoria.';
END $$;
