-- =====================================================================
-- FitSense MVP 1.0 - V37: correcciones de seguridad y dificultad (PROPUESTA)
--
-- ESTADO: PROPUESTA. NO esta en src/main/resources/db/migration y NO se ha
-- aplicado en Neon. Solo se ejecuta en bases PostgreSQL aisladas para simular
-- su impacto. Generada por docs/FitSense/auditoria_catalogo/v37/generar_v37.py
-- desde revision_visual/matriz_revision_visual_completa.tsv (solo CONFIRMADO).
-- Revision visual asistida por IA; la doble revision humana sigue PENDIENTE.
--
-- 102 ejercicios, 148 cambios de columna. Detalle con evidencia:
-- docs/FitSense/auditoria_catalogo/v37/correcciones_v37.tsv
--
-- Comprueba el estado anterior de CADA columna (y source_code y name_en) antes
-- de cambiar nada, y el estado final al terminar. Si algo no coincide, aborta.
-- No toca exercise_difficulty_approvals (V27/V28), planes, adaptacion ni Java.
-- =====================================================================

CREATE TEMP TABLE v37_cambios (exercise_id BIGINT, source_code TEXT, name_en TEXT, columna TEXT,
                               antes TEXT, despues TEXT) ON COMMIT DROP;
INSERT INTO v37_cambios VALUES
    (68, '3214', 'arms apart circular toe touch (male)', 'requires_overhead', 'false', 'true'),
    (74, '0015', 'assisted parallel close grip pull-up', 'requires_kneeling', 'false', 'true'),
    (75, '0017', 'assisted pull-up', 'requires_kneeling', 'false', 'true'),
    (103, '0989', 'band one arm twisting chest press', 'requires_floor', 'false', 'true'),
    (119, '0990', 'band one arm twisting seated row', 'requires_floor', 'false', 'true'),
    (141, '1015', 'band vertical pallof press', 'requires_overhead', 'false', 'true'),
    (173, '1411', 'barbell palms down wrist curl over a bench', 'requires_floor', 'false', 'true'),
    (173, '1411', 'barbell palms down wrist curl over a bench', 'requires_kneeling', 'false', 'true'),
    (174, '1412', 'barbell palms up wrist curl over a bench', 'requires_floor', 'false', 'true'),
    (174, '1412', 'barbell palms up wrist curl over a bench', 'requires_kneeling', 'false', 'true'),
    (187, '3562', 'barbell glute bridge two legs on bench (male)', 'requires_floor', 'false', 'true'),
    (224, '0071', 'barbell press sit-up', 'requires_overhead', 'false', 'true'),
    (229, '2799', 'barbell sitted alternate leg raise', 'requires_floor', 'false', 'true'),
    (229, '2799', 'barbell sitted alternate leg raise', 'requires_overhead', 'false', 'true'),
    (254, '3212', 'basic toe touch (male)', 'requires_overhead', 'false', 'true'),
    (269, '0119', 'barbell upright row v. 2', 'requires_overhead', 'false', 'true'),
    (270, '0121', 'barbell upright row v. 3', 'requires_overhead', 'false', 'true'),
    (274, '2800', 'barbell sitted alternate leg raise (female)', 'requires_floor', 'false', 'true'),
    (274, '2800', 'barbell sitted alternate leg raise (female)', 'requires_overhead', 'false', 'true'),
    (309, '0130', 'bench hip extension', 'requires_kneeling', 'false', 'true'),
    (316, '0139', 'biceps narrow pull-ups', 'requires_floor', 'false', 'true'),
    (318, '0137', 'body-up', 'requires_floor', 'false', 'true'),
    (327, '3235', 'cable assisted inverse leg curl', 'requires_kneeling', 'false', 'true'),
    (328, '0149', 'cable alternate triceps extension', 'requires_overhead', 'false', 'true'),
    (331, '0152', 'cable concentration extension (on knee)', 'requires_floor', 'false', 'true'),
    (331, '0152', 'cable concentration extension (on knee)', 'requires_kneeling', 'false', 'true'),
    (351, '0167', 'cable high row (kneeling)', 'requires_overhead', 'false', 'true'),
    (353, '0172', 'cable incline pushdown', 'requires_overhead', 'false', 'true'),
    (371, '0186', 'cable lying triceps extension v. 2', 'requires_floor', 'false', 'true'),
    (387, '0193', 'cable one arm straight back high row (kneeling)', 'requires_overhead', 'false', 'true'),
    (393, '0208', 'cable reverse-grip straight back seated high row', 'requires_overhead', 'false', 'true'),
    (417, '0216', 'cable seated shoulder internal rotation', 'requires_floor', 'false', 'true'),
    (427, '0221', 'cable side bend crunch (bosu ball)', 'requires_floor', 'false', 'true'),
    (431, '0230', 'cable standing lift', 'requires_overhead', 'false', 'true'),
    (434, '0213', 'cable seated high row (v-bar)', 'requires_overhead', 'false', 'true'),
    (456, '1271', 'chest and front of shoulder stretch', 'requires_overhead', 'false', 'true'),
    (461, '1273', 'clap push up', 'difficulty_level', '1', '3'),
    (466, '1272', 'chest stretch with exercise ball', 'requires_floor', 'false', 'true'),
    (466, '1272', 'chest stretch with exercise ball', 'requires_kneeling', 'false', 'true'),
    (477, '0862', 'cable twist (up-down)', 'requires_overhead', 'false', 'true'),
    (481, '0237', 'cable straight arm pulldown (with rope)', 'requires_overhead', 'false', 'true'),
    (514, '0290', 'dumbbell bench seated press', 'requires_overhead', 'false', 'true'),
    (573, '0323', 'dumbbell incline one arm lateral raise', 'requires_overhead', 'false', 'true'),
    (605, '1733', 'dumbbell incline two arm extension', 'requires_overhead', 'false', 'true'),
    (606, '0333', 'dumbbell kickback', 'requires_kneeling', 'false', 'true'),
    (607, '1734', 'dumbbell kickbacks on exercise ball', 'requires_floor', 'false', 'true'),
    (607, '1734', 'dumbbell kickbacks on exercise ball', 'requires_kneeling', 'false', 'true'),
    (656, '1441', 'dumbbell over bench one arm reverse wrist curl', 'requires_floor', 'false', 'true'),
    (656, '1441', 'dumbbell over bench one arm reverse wrist curl', 'requires_kneeling', 'false', 'true'),
    (657, '0367', 'dumbbell over bench one arm wrist curl', 'requires_floor', 'false', 'true'),
    (657, '0367', 'dumbbell over bench one arm wrist curl', 'requires_kneeling', 'false', 'true'),
    (658, '0368', 'dumbbell over bench revers wrist curl', 'requires_floor', 'false', 'true'),
    (658, '0368', 'dumbbell over bench revers wrist curl', 'requires_kneeling', 'false', 'true'),
    (659, '0369', 'dumbbell over bench wrist curl', 'requires_floor', 'false', 'true'),
    (659, '0369', 'dumbbell over bench wrist curl', 'requires_kneeling', 'false', 'true'),
    (684, '0365', 'dumbbell over bench neutral wrist curl', 'requires_floor', 'false', 'true'),
    (684, '0365', 'dumbbell over bench neutral wrist curl', 'requires_kneeling', 'false', 'true'),
    (689, '1673', 'dumbbell preacher curl over exercise ball', 'requires_floor', 'false', 'true'),
    (689, '1673', 'dumbbell preacher curl over exercise ball', 'requires_kneeling', 'false', 'true'),
    (776, '3292', 'elevator', 'difficulty_level', '1', '3'),
    (776, '3292', 'elevator', 'equipment', 'body weight', 'gymnastic rings'),
    (776, '3292', 'elevator', 'requires_hanging', 'false', 'true'),
    (776, '3292', 'elevator', 'requires_inversion', 'false', 'true'),
    (776, '3292', 'elevator', 'requires_overhead', 'false', 'true'),
    (776, '3292', 'elevator', 'requisitos', '', '1:gymnastic rings'),
    (782, '0864', 'dumbbell upright shoulder external rotation', 'requires_overhead', 'false', 'true'),
    (804, '1332', 'exercise ball alternating arm ups', 'requires_floor', 'false', 'true'),
    (809, '1338', 'exercise ball hug', 'requires_floor', 'false', 'true'),
    (810, '1339', 'exercise ball lat stretch', 'requires_floor', 'false', 'true'),
    (810, '1339', 'exercise ball lat stretch', 'requires_kneeling', 'false', 'true'),
    (812, '1744', 'exercise ball dip', 'requires_floor', 'false', 'true'),
    (813, '3303', 'flag', 'difficulty_level', '1', '3'),
    (822, '1341', 'exercise ball lower back stretch (pyramid)', 'requires_floor', 'false', 'true'),
    (822, '1341', 'exercise ball lower back stretch (pyramid)', 'requires_kneeling', 'false', 'true'),
    (825, '1416', 'exercise ball one leg prone lower body rotation', 'requires_floor', 'false', 'true'),
    (860, '3315', 'full maltese', 'difficulty_level', '1', '3'),
    (860, '3315', 'full maltese', 'requires_floor', 'false', 'true'),
    (879, '3523', 'glute bridge two legs on bench (male)', 'requires_floor', 'false', 'true'),
    (880, '3193', 'glute-ham raise', 'requires_kneeling', 'false', 'true'),
    (881, '1511', 'hamstring stretch', 'requires_floor', 'false', 'true'),
    (882, '3218', 'hands clasped circular toe touch (male)', 'requires_overhead', 'false', 'true'),
    (884, '1418', 'hug keens to chest', 'requires_floor', 'false', 'true'),
    (888, '0458', 'floor fly (with barbell)', 'requires_kneeling', 'false', 'true'),
    (910, '0499', 'inverted row', 'requires_floor', 'false', 'true'),
    (911, '2300', 'inverted row bent knees', 'requires_floor', 'false', 'true'),
    (913, '0497', 'inverted row v. 2', 'difficulty_level', '1', '3'),
    (913, '0497', 'inverted row v. 2', 'equipment', 'smith machine', 'pull-up bar'),
    (913, '0497', 'inverted row v. 2', 'requires_hanging', 'false', 'true'),
    (913, '0497', 'inverted row v. 2', 'requires_inversion', 'false', 'true'),
    (913, '0497', 'inverted row v. 2', 'requires_overhead', 'false', 'true'),
    (913, '0497', 'inverted row v. 2', 'requisitos', '1:smith machine,1:suspension straps', '1:pull-up bar'),
    (914, '0498', 'inverted row with straps', 'requires_floor', 'false', 'true'),
    (915, '0496', 'inverse leg curl (bench support)', 'requires_kneeling', 'false', 'true'),
    (916, '2400', 'inverse leg curl (on pull-up cable machine)', 'requires_kneeling', 'false', 'true'),
    (937, '0558', 'kipping muscle up', 'difficulty_level', '1', '3'),
    (937, '0558', 'kipping muscle up', 'equipment', 'pull-up bar', 'gymnastic rings'),
    (937, '0558', 'kipping muscle up', 'requisitos', '1:pull-up bar', '1:gymnastic rings'),
    (949, '0546', 'kettlebell seated press', 'requires_floor', 'false', 'true'),
    (974, '3419', 'l-sit on floor', 'difficulty_level', '1', '3'),
    (979, '3237', 'landmine lateral raise', 'requires_overhead', 'false', 'true'),
    (1041, '1313', 'lever unilateral row', 'requires_kneeling', 'false', 'true'),
    (1055, '1421', 'modified push up to lower arms', 'requires_kneeling', 'false', 'true'),
    (1058, '3217', 'modified hindu push-up (male)', 'requires_inversion', 'false', 'true'),
    (1068, '0631', 'muscle up', 'difficulty_level', '1', '3'),
    (1068, '0631', 'muscle up', 'equipment', 'pull-up bar', 'gymnastic rings'),
    (1068, '0631', 'muscle up', 'requisitos', '1:pull-up bar', '1:gymnastic rings'),
    (1070, '1355', 'one arm against wall', 'requires_overhead', 'false', 'true'),
    (1082, '1304', 'medicine ball chest push multiple response', 'requires_floor', 'false', 'true'),
    (1082, '1304', 'medicine ball chest push multiple response', 'requires_kneeling', 'false', 'true'),
    (1083, '1305', 'medicine ball chest push single response', 'requires_floor', 'false', 'true'),
    (1083, '1305', 'medicine ball chest push single response', 'requires_kneeling', 'false', 'true'),
    (1089, '1750', 'medicine ball supine chest throw', 'requires_floor', 'false', 'true'),
    (1123, '0656', 'push-up (on stability ball)', 'requires_floor', 'false', 'true'),
    (1124, '1707', 'prone twist on stability ball', 'requires_floor', 'false', 'true'),
    (1125, '0650', 'pull-in (on stability ball)', 'requires_floor', 'false', 'true'),
    (1136, '1390', 'seated calf stretch (male)', 'requires_floor', 'false', 'true'),
    (1152, '0672', 'reverse dip', 'requires_floor', 'false', 'true'),
    (1161, '0675', 'reverse hyper extension (on stability ball)', 'requires_floor', 'false', 'true'),
    (1185, '3213', 'side-to-side toe touch (male)', 'requires_overhead', 'false', 'true'),
    (1188, '0690', 'seated lower back stretch', 'requires_overhead', 'false', 'true'),
    (1192, '3304', 'skin the cat', 'equipment', 'body weight', 'gymnastic rings'),
    (1192, '3304', 'skin the cat', 'requisitos', '', '1:gymnastic rings'),
    (1195, '0697', 'self assisted inverse leg curl', 'requires_kneeling', 'false', 'true'),
    (1196, '1766', 'self assisted inverse leg curl', 'requires_floor', 'false', 'true'),
    (1196, '1766', 'self assisted inverse leg curl', 'requires_kneeling', 'false', 'true'),
    (1197, '0696', 'self assisted inverse leg curl (on floor)', 'requires_kneeling', 'false', 'true'),
    (1199, '0710', 'side hip abduction', 'requires_floor', 'false', 'true'),
    (1225, '0756', 'smith hip raise', 'requires_floor', 'false', 'true'),
    (1237, '0760', 'smith leg press', 'requires_floor', 'false', 'true'),
    (1294, '3318', 'swing 360', 'difficulty_level', '1', '3'),
    (1294, '3318', 'swing 360', 'equipment', 'body weight', 'pull-up bar'),
    (1294, '3318', 'swing 360', 'requires_hanging', 'false', 'true'),
    (1294, '3318', 'swing 360', 'requires_overhead', 'false', 'true'),
    (1294, '3318', 'swing 360', 'requisitos', '', '1:pull-up bar'),
    (1297, '0806', 'suspended push-up', 'requires_floor', 'false', 'true'),
    (1298, '3314', 'straddle maltese', 'difficulty_level', '1', '3'),
    (1298, '3314', 'straddle maltese', 'requires_floor', 'false', 'true'),
    (1301, '0807', 'suspended reverse crunch', 'equipment', 'pull-up bar', 'suspension straps'),
    (1301, '0807', 'suspended reverse crunch', 'requires_floor', 'false', 'true'),
    (1301, '0807', 'suspended reverse crunch', 'requisitos', '1:pull-up bar', '1:suspension straps'),
    (1308, '3231', 'two toe touch (male)', 'requires_overhead', 'false', 'true'),
    (1320, '0815', 'triceps dips floor', 'requires_floor', 'false', 'true'),
    (1374, '0858', 'wind sprints', 'equipment', 'body weight', 'pull-up bar'),
    (1374, '0858', 'wind sprints', 'high_impact', 'true', 'false'),
    (1374, '0858', 'wind sprints', 'movement_pattern', 'LOCOMOTION_CARDIO', 'TRUNK_FLEXION'),
    (1374, '0858', 'wind sprints', 'requires_hanging', 'false', 'true'),
    (1374, '0858', 'wind sprints', 'requires_overhead', 'false', 'true'),
    (1374, '0858', 'wind sprints', 'requisitos', '', '1:pull-up bar');

-- Valor actual de cada columna afectada, como texto comparable con antes/despues.
CREATE TEMP VIEW v37_valor AS
SELECT e.exercise_id, x.columna, x.valor
  FROM public.exercises e
  JOIN public.body_parts bp ON bp.body_part_id = e.body_part_id
  JOIN public.equipment_types eq ON eq.equipment_id = e.equipment_id
 CROSS JOIN LATERAL (VALUES
       ('requires_floor', e.requires_floor::text), ('requires_kneeling', e.requires_kneeling::text),
       ('requires_hanging', e.requires_hanging::text), ('requires_overhead', e.requires_overhead::text),
       ('requires_inversion', e.requires_inversion::text), ('high_impact', e.high_impact::text),
       ('axial_load', e.axial_load::text), ('difficulty_level', e.difficulty_level::text),
       ('movement_pattern', e.movement_pattern), ('target_muscle', e.target_muscle), ('body_part', bp.code),
       ('equipment', eq.code),
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
      FROM v37_cambios c
      LEFT JOIN pg_temp.v37_valor v ON v.exercise_id = c.exercise_id AND v.columna = c.columna
     WHERE v.valor IS DISTINCT FROM c.antes
        OR NOT EXISTS (SELECT 1 FROM public.exercises e WHERE e.exercise_id = c.exercise_id
                          AND e.source_code = c.source_code AND e.name_en = c.name_en AND e.is_active);
    IF distintos IS NOT NULL THEN
        RAISE EXCEPTION 'V37: estado anterior inesperado -> %', distintos;
    END IF;
END $$;

UPDATE public.exercises e
   SET requires_floor = coalesce((SELECT c.despues::boolean FROM v37_cambios c WHERE c.exercise_id = e.exercise_id AND c.columna = 'requires_floor'), e.requires_floor),
       requires_kneeling = coalesce((SELECT c.despues::boolean FROM v37_cambios c WHERE c.exercise_id = e.exercise_id AND c.columna = 'requires_kneeling'), e.requires_kneeling),
       requires_hanging = coalesce((SELECT c.despues::boolean FROM v37_cambios c WHERE c.exercise_id = e.exercise_id AND c.columna = 'requires_hanging'), e.requires_hanging),
       requires_overhead = coalesce((SELECT c.despues::boolean FROM v37_cambios c WHERE c.exercise_id = e.exercise_id AND c.columna = 'requires_overhead'), e.requires_overhead),
       requires_inversion = coalesce((SELECT c.despues::boolean FROM v37_cambios c WHERE c.exercise_id = e.exercise_id AND c.columna = 'requires_inversion'), e.requires_inversion),
       high_impact = coalesce((SELECT c.despues::boolean FROM v37_cambios c WHERE c.exercise_id = e.exercise_id AND c.columna = 'high_impact'), e.high_impact)
 WHERE e.exercise_id IN (SELECT exercise_id FROM v37_cambios WHERE columna IN ('requires_floor', 'requires_kneeling', 'requires_hanging', 'requires_overhead', 'requires_inversion', 'high_impact'));
UPDATE public.exercises e SET difficulty_level = c.despues::smallint FROM v37_cambios c
 WHERE c.exercise_id = e.exercise_id AND c.columna = 'difficulty_level';
UPDATE public.exercises e SET movement_pattern = c.despues FROM v37_cambios c
 WHERE c.exercise_id = e.exercise_id AND c.columna = 'movement_pattern';
UPDATE public.exercises e SET equipment_id = q.equipment_id FROM v37_cambios c
  JOIN public.equipment_types q ON q.code = c.despues
 WHERE c.exercise_id = e.exercise_id AND c.columna = 'equipment';
DELETE FROM public.exercise_equipment_requirements r USING v37_cambios c
 WHERE c.exercise_id = r.exercise_id AND c.columna = 'requisitos';
INSERT INTO public.exercise_equipment_requirements (exercise_id, group_no, equipment_id)
SELECT c.exercise_id, split_part(x, ':', 1)::smallint, q.equipment_id
  FROM v37_cambios c CROSS JOIN LATERAL unnest(string_to_array(nullif(c.despues, ''), ',')) AS x
  JOIN public.equipment_types q ON q.code = split_part(x, ':', 2)
 WHERE c.columna = 'requisitos';

DO $$
DECLARE distintos TEXT;
BEGIN
    SELECT string_agg(c.exercise_id || ' ' || c.columna || ' = ' || coalesce(v.valor, 'NULL'), '; '
                      ORDER BY c.exercise_id, c.columna)
      INTO distintos
      FROM v37_cambios c
      LEFT JOIN pg_temp.v37_valor v ON v.exercise_id = c.exercise_id AND v.columna = c.columna
     WHERE v.valor IS DISTINCT FROM c.despues;
    IF distintos IS NOT NULL THEN
        RAISE EXCEPTION 'V37: estado final inesperado -> %', distintos;
    END IF;
    IF EXISTS (SELECT 1 FROM public.exercises WHERE requires_hanging AND NOT requires_overhead) THEN
        RAISE EXCEPTION 'V37: hay ejercicios con suspension y sin brazos arriba';
    END IF;
END $$;
