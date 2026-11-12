-- =====================================================================
-- FitSense - V38 (CANDIDATA, NO APLICADA): clasificacion funcional, cardio de casa y equipamiento real
--
-- Correcciones del catalogo con evidencia del GIF (DIAGNOSTICO_V37_1.md y decisiones
-- D1-D7 del 30/10/2026). Generada por docs/FitSense/auditoria_catalogo/v38/generar_v38.py
-- desde cambios_v38.tsv, etapas ABC: 54 cambios de columna en 43 ejercicios.
-- Comprueba el estado anterior de cada columna antes de cambiar nada y el final al terminar.
-- No toca exercise_difficulty_approvals (V27/V28), planes, adaptacion ni progresion.
-- Revision visual asistida por IA; la doble revision humana sigue PENDIENTE. La dificultad
-- 2 de 1106 es PROVISIONAL (D7), no una evaluacion validada con el instrumento.
-- =====================================================================

-- Implementos que el GIF exige y el catalogo no podia expresar (D3).
INSERT INTO public.equipment_types (code, name_es, requires_gym, display_order) VALUES
    ('wall bars', 'Espaldera', FALSE, 32),
    ('anchor point', 'Punto de anclaje (puerta o poste)', FALSE, 33),
    ('ghd bench', 'Banco GHD', TRUE, 34),
    ('hyperextension bench', 'Banco de hiperextension (romano)', TRUE, 35),
    ('squat rack', 'Rack de sentadilla', TRUE, 36),
    ('preacher bench', 'Banco Scott (predicador)', TRUE, 37)
ON CONFLICT (code) DO NOTHING;

CREATE TEMP TABLE v38_cambios (exercise_id BIGINT, source_code TEXT, name_en TEXT, columna TEXT,
                               antes TEXT, despues TEXT);
INSERT INTO v38_cambios VALUES
    (1117, '3552', 'quick feet v. 2', 'default_prescription', 'SETS_REPS', 'DURATION'),
    (1117, '3552', 'quick feet v. 2', 'body_part', 'upper legs', 'cardio'),
    (1117, '3552', 'quick feet v. 2', 'target_muscle', 'quads', 'cardiovascular system'),
    (1102, '1687', 'posterior step to overhead reach', 'movement_pattern', 'KNEE_DOMINANT', 'MOBILITY'),
    (1106, '0664', 'push-up to side plank', 'body_part', 'waist', 'chest'),
    (1106, '0664', 'push-up to side plank', 'target_muscle', 'abs', 'pectorals'),
    (1106, '0664', 'push-up to side plank', 'default_prescription', 'DURATION', 'SETS_REPS'),
    (1106, '0664', 'push-up to side plank', 'difficulty_level', '1', '2'),
    (1111, '0642', 'outside leg kick push-up', 'body_part', 'upper legs', 'chest'),
    (1111, '0642', 'outside leg kick push-up', 'target_muscle', 'glutes', 'pectorals'),
    (1113, '3662', 'pike-to-cobra push-up', 'body_part', 'upper legs', 'chest'),
    (1113, '3662', 'pike-to-cobra push-up', 'target_muscle', 'glutes', 'pectorals'),
    (1115, '0661', 'push-up inside leg kick', 'body_part', 'upper legs', 'chest'),
    (1115, '0661', 'push-up inside leg kick', 'target_muscle', 'glutes', 'pectorals'),
    (1262, '0778', 'spider crawl push up', 'body_part', 'upper legs', 'chest'),
    (1262, '0778', 'spider crawl push up', 'target_muscle', 'glutes', 'pectorals'),
    (558, '1655', 'dumbbell biceps curl squat', 'body_part', 'upper arms', 'upper legs'),
    (558, '1655', 'dumbbell biceps curl squat', 'target_muscle', 'biceps', 'quads'),
    (567, '1732', 'dumbbell forward lunge triceps extension', 'body_part', 'upper arms', 'upper legs'),
    (567, '1732', 'dumbbell forward lunge triceps extension', 'target_muscle', 'triceps', 'quads'),
    (301, '3168', 'bodyweight squatting row', 'requisitos', '', '1:body weight,2:wall bars'),
    (302, '3167', 'bodyweight squatting row (with towel)', 'requisitos', '', '1:body weight,2:wall bars'),
    (303, '3156', 'bodyweight standing close-grip one arm row', 'requisitos', '', '1:body weight,2:wall bars'),
    (304, '3158', 'bodyweight standing close-grip row', 'requisitos', '', '1:body weight,2:wall bars'),
    (305, '3162', 'bodyweight standing one arm row', 'requisitos', '', '1:body weight,2:wall bars'),
    (306, '3161', 'bodyweight standing one arm row (with towel)', 'requisitos', '', '1:body weight,2:wall bars'),
    (307, '3166', 'bodyweight standing row', 'requisitos', '', '1:body weight,2:wall bars'),
    (308, '3165', 'bodyweight standing row (with towel)', 'requisitos', '', '1:body weight,2:wall bars'),
    (813, '3303', 'flag', 'requisitos', '', '1:body weight,2:wall bars'),
    (1072, '1773', 'one arm towel row', 'requisitos', '', '1:body weight,2:anchor point'),
    (880, '3193', 'glute-ham raise', 'requisitos', '', '1:body weight,2:ghd bench'),
    (58, '0002', '45° side bend', 'requisitos', '', '1:body weight,2:hyperextension bench'),
    (235, '0074', 'barbell rack pull', 'requisitos', '1:barbell', '1:barbell,2:squat rack'),
    (1253, '1490', 'standing calf raise (on a staircase)', 'requisitos', '', '1:body weight,2:step platform'),
    (244, '0070', 'barbell preacher curl', 'requisitos', '1:barbell', '1:barbell,2:preacher bench'),
    (249, '0081', 'barbell reverse preacher curl', 'requisitos', '1:barbell', '1:barbell,2:preacher bench'),
    (397, '1633', 'cable one arm preacher curl', 'requisitos', '1:cable', '1:cable,2:preacher bench'),
    (398, '1635', 'cable one arm reverse preacher curl', 'requisitos', '1:cable', '1:cable,2:preacher bench'),
    (403, '0195', 'cable preacher curl', 'requisitos', '1:cable', '1:cable,2:preacher bench'),
    (411, '0209', 'cable reverse preacher curl', 'requisitos', '1:cable', '1:cable,2:preacher bench'),
    (441, '1639', 'cable rope hammer preacher curl', 'requisitos', '1:cable', '1:cable,2:preacher bench'),
    (445, '1640', 'cable rope one arm hammer preacher curl', 'requisitos', '1:cable', '1:cable,2:preacher bench'),
    (522, '1646', 'dumbbell alternate hammer preacher curl', 'requisitos', '1:dumbbell,2:bench', '1:dumbbell,2:preacher bench'),
    (523, '1647', 'dumbbell alternate preacher curl', 'requisitos', '1:dumbbell,2:bench', '1:dumbbell,2:preacher bench'),
    (648, '1663', 'dumbbell one arm hammer preacher curl', 'requisitos', '1:dumbbell,2:bench', '1:dumbbell,2:preacher bench'),
    (676, '1414', 'dumbbell one arm reverse preacher curl', 'requisitos', '1:dumbbell,2:bench', '1:dumbbell,2:preacher bench'),
    (687, '0370', 'dumbbell peacher hammer curl', 'requisitos', '1:dumbbell', '1:dumbbell,2:preacher bench'),
    (688, '0372', 'dumbbell preacher curl', 'requisitos', '1:dumbbell,2:bench', '1:dumbbell,2:preacher bench'),
    (792, '0428', 'dumbbell standing preacher curl', 'requisitos', '1:dumbbell,2:bench', '1:dumbbell,2:preacher bench'),
    (795, '2293', 'dumbbell standing zottman preacher curl', 'requisitos', '1:dumbbell,2:bench', '1:dumbbell,2:preacher bench'),
    (802, '2294', 'dumbbell zottman preacher curl', 'requisitos', '1:dumbbell,2:bench', '1:dumbbell,2:preacher bench'),
    (837, '1627', 'ez barbell close grip preacher curl', 'requisitos', '1:ez barbell', '1:ez barbell,2:preacher bench'),
    (845, '0452', 'ez barbell reverse grip preacher curl', 'requisitos', '1:ez barbell', '1:ez barbell,2:preacher bench'),
    (849, '1628', 'ez barbell spider curl', 'requisitos', '1:ez barbell', '1:ez barbell,2:preacher bench');

CREATE TEMP VIEW v38_valor AS
SELECT e.exercise_id, x.columna, x.valor
  FROM public.exercises e
  JOIN public.body_parts bp ON bp.body_part_id = e.body_part_id
 CROSS JOIN LATERAL (VALUES
       ('difficulty_level', e.difficulty_level::text), ('movement_pattern', e.movement_pattern),
       ('target_muscle', e.target_muscle), ('body_part', bp.code),
       ('default_prescription', e.default_prescription::text),
       ('requisitos', coalesce((SELECT string_agg(r.group_no || ':' || q.code, ',' ORDER BY r.group_no || ':' || q.code)
                                  FROM public.exercise_equipment_requirements r
                                  JOIN public.equipment_types q ON q.equipment_id = r.equipment_id
                                 WHERE r.exercise_id = e.exercise_id), ''))) AS x(columna, valor);

DO $$
DECLARE distintos TEXT;
BEGIN
    SELECT string_agg(c.exercise_id || ' ' || c.columna || ' = ' || coalesce(v.valor, 'NULL') || ' (se esperaba '
                      || c.antes || ')', '; ' ORDER BY c.exercise_id, c.columna)
      INTO distintos
      FROM v38_cambios c
      LEFT JOIN pg_temp.v38_valor v ON v.exercise_id = c.exercise_id AND v.columna = c.columna
     WHERE v.valor IS DISTINCT FROM c.antes
        OR NOT EXISTS (SELECT 1 FROM public.exercises e WHERE e.exercise_id = c.exercise_id
                          AND e.source_code = c.source_code AND e.name_en = c.name_en AND e.is_active);
    IF distintos IS NOT NULL THEN
        RAISE EXCEPTION 'V38: estado anterior inesperado -> %', distintos;
    END IF;
END $$;

UPDATE public.exercises e SET movement_pattern = c.despues FROM v38_cambios c
 WHERE c.exercise_id = e.exercise_id AND c.columna = 'movement_pattern';
UPDATE public.exercises e SET target_muscle = c.despues FROM v38_cambios c
 WHERE c.exercise_id = e.exercise_id AND c.columna = 'target_muscle';
UPDATE public.exercises e SET difficulty_level = c.despues::smallint FROM v38_cambios c
 WHERE c.exercise_id = e.exercise_id AND c.columna = 'difficulty_level';
UPDATE public.exercises e SET default_prescription = c.despues FROM v38_cambios c
 WHERE c.exercise_id = e.exercise_id AND c.columna = 'default_prescription';
UPDATE public.exercises e SET body_part_id = bp.body_part_id FROM v38_cambios c
  JOIN public.body_parts bp ON bp.code = c.despues
 WHERE c.exercise_id = e.exercise_id AND c.columna = 'body_part';
DELETE FROM public.exercise_equipment_requirements r USING v38_cambios c
 WHERE c.exercise_id = r.exercise_id AND c.columna = 'requisitos';
INSERT INTO public.exercise_equipment_requirements (exercise_id, group_no, equipment_id)
SELECT c.exercise_id, split_part(x, ':', 1)::smallint, q.equipment_id
  FROM v38_cambios c CROSS JOIN LATERAL unnest(string_to_array(nullif(c.despues, ''), ',')) AS x
  JOIN public.equipment_types q ON q.code = split_part(x, ':', 2)
 WHERE c.columna = 'requisitos';

DO $$
DECLARE distintos TEXT;
BEGIN
    SELECT string_agg(c.exercise_id || ' ' || c.columna || ' = ' || coalesce(v.valor, 'NULL'), '; '
                      ORDER BY c.exercise_id, c.columna)
      INTO distintos
      FROM v38_cambios c
      LEFT JOIN pg_temp.v38_valor v ON v.exercise_id = c.exercise_id AND v.columna = c.columna
     WHERE v.valor IS DISTINCT FROM c.despues;
    IF distintos IS NOT NULL THEN
        RAISE EXCEPTION 'V38: estado final inesperado -> %', distintos;
    END IF;
END $$;

-- Flyway ejecuta las migraciones en la misma sesion: los objetos temporales se borran aqui.
DROP VIEW pg_temp.v38_valor;
DROP TABLE pg_temp.v38_cambios;
