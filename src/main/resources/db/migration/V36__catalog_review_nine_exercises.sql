-- =====================================================================
-- FitSense MVP 1.0 - V36: correccion de nueve ejercicios del catalogo
--
-- ESTADO: CANDIDATA A DESPLIEGUE; NO APLICADA EN NEON. Verificada solo en
-- PostgreSQL 16 aislado. La revision humana doble esta PENDIENTE: ver
-- docs/FitSense/CATALOGO_V36.md y docs/FitSense/evidencia/catalogo_v36/.
--
-- QUE HACE
--   Corrige, por exercise_id, las columnas aprobadas de nueve ejercicios cuya
--   clasificacion no corresponde a su GIF original. No borra filas de
--   exercises ni toca planes publicados, adaptacion, progresion ni Java.
--
--   id    columna                 antes                 despues
--   1267  is_active               true                  false
--   1296  difficulty_level        1                     3
--         high_impact             false                 true
--   1333  equipment_id            ab wheel              body weight
--         requisito (grupo 1)     ab wheel              (ninguno: peso corporal)
--         high_impact             false                 true
--         requires_floor          true                  false
--         requires_overhead       true                  false
--         name_es                 Rodar con rueda       Carrera con zancadas amplias
--   872   movement_pattern        HIP_EXTENSION         TRUNK_FLEXION
--         name_es                 Levantamiento de cadera con rodillas flexionadas
--                                                       Encogimiento invertido
--   1059  body_part               waist                 upper legs
--         target_muscle           abs                   quads
--         synergist_muscle        quadriceps            abs        (conserva el giro)
--   527   body_part               upper arms            upper legs
--         target_muscle           biceps                quads
--         synergist_muscle        forearms              biceps     (conserva el curl)
--   609   body_part               upper arms            upper legs
--         target_muscle           biceps                quads
--         synergist_muscle        quadriceps            biceps     (conserva el curl)
--   725   body_part               upper arms            shoulders
--         target_muscle           biceps                delts
--         synergist_muscle        shoulders             biceps     (conserva el curl)
--   760   body_part               upper arms            shoulders
--         target_muscle           biceps                delts
--         synergist_muscle        forearms              biceps     (conserva el curl)
--
--   Se conservan sin cambio: los patrones de 1059, 527 y 609 (KNEE_DOMINANT)
--   y de 725 y 760 (VERTICAL_PUSH), y en los cinco requires_overhead,
--   nombres, instrucciones y secondary_muscles. 1296 conserva
--   HORIZONTAL_PUSH, suelo y brazos arriba. 1333 conserva LOCOMOTION_CARDIO,
--   DURATION y dificultad 1: el impacto alto ya lo excluye de principiantes.
--
-- PRECONDICION ESTRICTA
--   Cada columna que cambia debe tener EXACTAMENTE su valor anterior, y 1333
--   un unico requisito (grupo 1, ab wheel). Si algo difiere -otra migracion o
--   una edicion manual lo cambio-, la migracion se detiene con RAISE EXCEPTION
--   y no aplica nada: hay que revisar el registro, no forzarlo.
--
-- POSCONDICION
--   Se comprueba el estado final de las nueve filas antes del COMMIT. Los
--   disparadores diferidos de V23 (implemento principal en sus requisitos) se
--   comprueban en el COMMIT.
--
-- NO HACE
--   No modifica V24, V25 ni V35. No registra eventos en
--   exercise_difficulty_approvals (V27): la aprobacion de 872 en V28 queda en
--   su historial; la discrepancia se documenta en CATALOGO_V36.md.
--
-- Una vez ejecutada con Flyway, NO modificar ni reformatear este archivo.
-- =====================================================================

DO $$
DECLARE
    distintos TEXT;
    requisitos TEXT;
BEGIN
    WITH esperado (exercise_id, source_code, is_active, difficulty_level, high_impact, requires_floor,
                   requires_overhead, movement_pattern, body_part, equipment, target_muscle, synergist_muscle,
                   name_es) AS (
        VALUES
            (1267::BIGINT, '3291', TRUE, 1::SMALLINT, FALSE, FALSE, TRUE, 'KNEE_DOMINANT', 'upper arms',
             'body weight', 'triceps', 'shoulders', 'Stalder press'),
            (1296, '0803', TRUE, 1, FALSE, TRUE, TRUE, 'HORIZONTAL_PUSH', 'chest',
             'body weight', 'pectorals', 'core', 'Push-up de Superman'),
            (1333, '3637', TRUE, 1, FALSE, TRUE, TRUE, 'LOCOMOTION_CARDIO', 'cardio',
             'ab wheel', 'cardiovascular system', 'quadriceps', 'Rodar con rueda'),
            (872, '0484', TRUE, 1, FALSE, TRUE, FALSE, 'HIP_EXTENSION', 'waist',
             'body weight', 'abs', 'glutes', 'Levantamiento de cadera con rodillas flexionadas'),
            (1059, '1688', TRUE, 1, FALSE, FALSE, FALSE, 'KNEE_DOMINANT', 'waist',
             'body weight', 'abs', 'quadriceps', 'Zancada con giro'),
            (527, '1651', TRUE, 2, FALSE, FALSE, FALSE, 'KNEE_DOMINANT', 'upper arms',
             'dumbbell', 'biceps', 'forearms', 'Curl de bíceps con zancada y lanzamiento'),
            (609, '1658', TRUE, 2, FALSE, FALSE, FALSE, 'KNEE_DOMINANT', 'upper arms',
             'dumbbell', 'biceps', 'quadriceps', 'Curl de bíceps en zancada con mancuerna'),
            (725, '3547', TRUE, 1, FALSE, FALSE, TRUE, 'VERTICAL_PUSH', 'upper arms',
             'dumbbell', 'biceps', 'shoulders', 'Curl de bíceps sentado con press de hombro'),
            (760, '3560', TRUE, 2, FALSE, FALSE, TRUE, 'VERTICAL_PUSH', 'upper arms',
             'dumbbell', 'biceps', 'forearms', 'Curl martillo alterno de pie y press')
    )
    SELECT string_agg(s.exercise_id::TEXT, ', ' ORDER BY s.exercise_id) INTO distintos
    FROM esperado s
    WHERE NOT EXISTS (
        SELECT 1
        FROM public.exercises e
                 JOIN public.body_parts bp ON bp.body_part_id = e.body_part_id
                 JOIN public.equipment_types eq ON eq.equipment_id = e.equipment_id
        WHERE e.exercise_id = s.exercise_id
          AND e.source_code = s.source_code
          AND e.is_active = s.is_active
          AND e.difficulty_level = s.difficulty_level
          AND e.high_impact = s.high_impact
          AND e.requires_floor = s.requires_floor
          AND e.requires_overhead = s.requires_overhead
          AND e.movement_pattern = s.movement_pattern
          AND bp.code = s.body_part
          AND eq.code = s.equipment
          AND e.target_muscle = s.target_muscle
          AND e.synergist_muscle IS NOT DISTINCT FROM s.synergist_muscle
          AND e.name_es IS NOT DISTINCT FROM s.name_es
    );
    IF distintos IS NOT NULL THEN
        RAISE EXCEPTION 'V36: estos ejercicios no tienen la clasificacion anterior esperada: %. '
            'Revisar el registro antes de aplicar la correccion.', distintos;
    END IF;

    SELECT string_agg(r.group_no || ':' || eq.code, ',' ORDER BY r.group_no, eq.code) INTO requisitos
    FROM public.exercise_equipment_requirements r
             JOIN public.equipment_types eq ON eq.equipment_id = r.equipment_id
    WHERE r.exercise_id = 1333;
    IF requisitos IS DISTINCT FROM '1:ab wheel' THEN
        RAISE EXCEPTION 'V36: los requisitos de 1333 son % y se esperaba 1:ab wheel', coalesce(requisitos, '(ninguno)');
    END IF;
END $$;

-- 1267 Stalder press: habilidad gimnastica con inversion. Fuera del catalogo
-- elegible; la fila y su historial se conservan.
UPDATE public.exercises SET is_active = FALSE WHERE exercise_id = 1267;

-- 1296 Superman push-up: flexion pliometrica.
UPDATE public.exercises SET difficulty_level = 3, high_impact = TRUE WHERE exercise_id = 1296;

-- 1333 Wheel run: el GIF es una carrera con zancadas amplias, sin rueda.
DELETE FROM public.exercise_equipment_requirements WHERE exercise_id = 1333;
UPDATE public.exercises
SET equipment_id      = (SELECT equipment_id FROM public.equipment_types WHERE code = 'body weight'),
    high_impact       = TRUE,
    requires_floor    = FALSE,
    requires_overhead = FALSE,
    name_es           = 'Carrera con zancadas amplias'
WHERE exercise_id = 1333;

-- 872: el GIF es un encogimiento invertido (flexion de tronco).
UPDATE public.exercises
SET movement_pattern = 'TRUNK_FLEXION',
    name_es          = 'Encogimiento invertido'
WHERE exercise_id = 872;

-- 1059: zancada; el giro queda como sinergista.
UPDATE public.exercises
SET body_part_id     = (SELECT body_part_id FROM public.body_parts WHERE code = 'upper legs'),
    target_muscle    = 'quads',
    synergist_muscle = 'abs'
WHERE exercise_id = 1059;

-- 527 y 609: zancada con curl; el curl queda como sinergista.
UPDATE public.exercises
SET body_part_id     = (SELECT body_part_id FROM public.body_parts WHERE code = 'upper legs'),
    target_muscle    = 'quads',
    synergist_muscle = 'biceps'
WHERE exercise_id IN (527, 609);

-- 725 y 760: curl seguido de press por encima de la cabeza.
UPDATE public.exercises
SET body_part_id     = (SELECT body_part_id FROM public.body_parts WHERE code = 'shoulders'),
    target_muscle    = 'delts',
    synergist_muscle = 'biceps'
WHERE exercise_id IN (725, 760);

-- POSCONDICION
DO $$
DECLARE
    distintos TEXT;
BEGIN
    WITH esperado (exercise_id, is_active, difficulty_level, high_impact, requires_floor, requires_overhead,
                   movement_pattern, body_part, equipment, target_muscle, synergist_muscle, name_es) AS (
        VALUES
            (1267::BIGINT, FALSE, 1::SMALLINT, FALSE, FALSE, TRUE, 'KNEE_DOMINANT', 'upper arms',
             'body weight', 'triceps', 'shoulders', 'Stalder press'),
            (1296, TRUE, 3, TRUE, TRUE, TRUE, 'HORIZONTAL_PUSH', 'chest',
             'body weight', 'pectorals', 'core', 'Push-up de Superman'),
            (1333, TRUE, 1, TRUE, FALSE, FALSE, 'LOCOMOTION_CARDIO', 'cardio',
             'body weight', 'cardiovascular system', 'quadriceps', 'Carrera con zancadas amplias'),
            (872, TRUE, 1, FALSE, TRUE, FALSE, 'TRUNK_FLEXION', 'waist',
             'body weight', 'abs', 'glutes', 'Encogimiento invertido'),
            (1059, TRUE, 1, FALSE, FALSE, FALSE, 'KNEE_DOMINANT', 'upper legs',
             'body weight', 'quads', 'abs', 'Zancada con giro'),
            (527, TRUE, 2, FALSE, FALSE, FALSE, 'KNEE_DOMINANT', 'upper legs',
             'dumbbell', 'quads', 'biceps', 'Curl de bíceps con zancada y lanzamiento'),
            (609, TRUE, 2, FALSE, FALSE, FALSE, 'KNEE_DOMINANT', 'upper legs',
             'dumbbell', 'quads', 'biceps', 'Curl de bíceps en zancada con mancuerna'),
            (725, TRUE, 1, FALSE, FALSE, TRUE, 'VERTICAL_PUSH', 'shoulders',
             'dumbbell', 'delts', 'biceps', 'Curl de bíceps sentado con press de hombro'),
            (760, TRUE, 2, FALSE, FALSE, TRUE, 'VERTICAL_PUSH', 'shoulders',
             'dumbbell', 'delts', 'biceps', 'Curl martillo alterno de pie y press')
    )
    SELECT string_agg(s.exercise_id::TEXT, ', ' ORDER BY s.exercise_id) INTO distintos
    FROM esperado s
    WHERE NOT EXISTS (
        SELECT 1
        FROM public.exercises e
                 JOIN public.body_parts bp ON bp.body_part_id = e.body_part_id
                 JOIN public.equipment_types eq ON eq.equipment_id = e.equipment_id
        WHERE e.exercise_id = s.exercise_id
          AND e.is_active = s.is_active
          AND e.difficulty_level = s.difficulty_level
          AND e.high_impact = s.high_impact
          AND e.requires_floor = s.requires_floor
          AND e.requires_overhead = s.requires_overhead
          AND e.movement_pattern = s.movement_pattern
          AND bp.code = s.body_part
          AND eq.code = s.equipment
          AND e.target_muscle = s.target_muscle
          AND e.synergist_muscle IS NOT DISTINCT FROM s.synergist_muscle
          AND e.name_es IS NOT DISTINCT FROM s.name_es
    );
    IF distintos IS NOT NULL THEN
        RAISE EXCEPTION 'V36: estado final inesperado en: %', distintos;
    END IF;
    IF EXISTS (SELECT 1 FROM public.exercise_equipment_requirements WHERE exercise_id = 1333) THEN
        RAISE EXCEPTION 'V36: 1333 conserva requisitos de equipamiento';
    END IF;
END $$;
