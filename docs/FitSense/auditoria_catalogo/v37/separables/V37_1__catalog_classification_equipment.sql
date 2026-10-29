-- =====================================================================
-- FitSense MVP 1.0 - V37.1: grupo, patron, implemento y restricciones de mas (PROPUESTA)
--
-- ESTADO: PROPUESTA. NO esta en src/main/resources/db/migration y NO se ha
-- aplicado en Neon. Solo se ejecuta en bases PostgreSQL aisladas para simular
-- su impacto. Generada por docs/FitSense/auditoria_catalogo/v37/generar_v37.py
-- desde revision_visual/matriz_revision_visual_completa.tsv (solo CONFIRMADO).
-- Revision visual asistida por IA; la doble revision humana sigue PENDIENTE.
--
-- 154 ejercicios, 208 cambios de columna. Detalle con evidencia:
-- docs/FitSense/auditoria_catalogo/v37/correcciones_v37.tsv
--
-- Comprueba el estado anterior de CADA columna (y source_code y name_en) antes
-- de cambiar nada, y el estado final al terminar. Si algo no coincide, aborta.
-- No toca exercise_difficulty_approvals (V27/V28), planes, adaptacion ni Java.
-- =====================================================================

CREATE TEMP TABLE v37_cambios (exercise_id BIGINT, source_code TEXT, name_en TEXT, columna TEXT,
                               antes TEXT, despues TEXT) ON COMMIT DROP;
INSERT INTO v37_cambios VALUES
    (55, '1368', 'ankle circles', 'requires_floor', 'true', 'false'),
    (59, '0003', 'air bike', 'movement_pattern', 'LOCOMOTION_CARDIO', 'TRUNK_ROTATION_LATERAL_FLEXION'),
    (66, '1405', 'back pec stretch', 'equipment', 'body weight', 'bench'),
    (66, '1405', 'back pec stretch', 'requisitos', '', '1:bench'),
    (96, '0999', 'band single leg calf raise', 'requisitos', '1:band', '1:band,2:step platform'),
    (97, '1000', 'band single leg reverse calf raise', 'requisitos', '1:band', '1:band,2:step platform'),
    (124, '0987', 'band one arm single leg split squat', 'requisitos', '1:band', '1:band,2:bench,2:plyo box'),
    (127, '1001', 'band single leg split squat', 'requisitos', '1:band', '1:band,2:bench,2:plyo box'),
    (131, '0986', 'band one arm overhead biceps curl', 'requires_overhead', 'true', 'false'),
    (142, '1010', 'band straight leg deadlift', 'body_part', 'back', 'upper legs'),
    (142, '1010', 'band straight leg deadlift', 'target_muscle', 'spine', 'hamstrings'),
    (153, '1255', 'barbell decline pullover', 'requisitos', '1:barbell', '1:barbell,2:bench'),
    (154, '0036', 'barbell decline wide-grip press', 'requisitos', '1:barbell', '1:barbell,2:bench'),
    (155, '0040', 'barbell front raise and pullover', 'requisitos', '1:barbell', '1:barbell,2:bench'),
    (156, '1316', 'barbell bent arm pullover', 'requisitos', '1:barbell', '1:barbell,2:bench'),
    (158, '0034', 'barbell decline bent arm pullover', 'requisitos', '1:barbell', '1:barbell,2:bench'),
    (159, '0037', 'barbell decline wide-grip pullover', 'requisitos', '1:barbell', '1:barbell,2:bench'),
    (160, '0024', 'barbell bench front squat', 'requisitos', '1:barbell', '1:barbell,2:bench'),
    (161, '0026', 'barbell bench squat', 'requisitos', '1:barbell', '1:barbell,2:bench'),
    (171, '0035', 'barbell decline close grip to skull press', 'requisitos', '1:barbell', '1:barbell,2:bench'),
    (178, '0050', 'barbell incline shoulder raise', 'requisitos', '1:barbell', '1:barbell,2:bench'),
    (179, '0049', 'barbell incline row', 'requisitos', '1:barbell', '1:barbell,2:bench'),
    (196, '0058', 'barbell lying lifting (on hip)', 'requisitos', '1:barbell', '1:barbell,2:bench'),
    (199, '0068', 'barbell one leg squat', 'requisitos', '1:barbell', '1:barbell,2:bench'),
    (202, '0048', 'barbell incline reverse-grip press', 'requisitos', '1:barbell', '1:barbell,2:bench'),
    (204, '1720', 'barbell lying back of the head tricep extension', 'requisitos', '1:barbell', '1:barbell,2:bench'),
    (205, '0055', 'barbell lying close-grip press', 'requisitos', '1:barbell', '1:barbell,2:bench'),
    (206, '0056', 'barbell lying close-grip triceps extension', 'requisitos', '1:barbell', '1:barbell,2:bench'),
    (207, '0057', 'barbell lying extension', 'requisitos', '1:barbell', '1:barbell,2:bench'),
    (208, '0059', 'barbell lying preacher curl', 'requisitos', '1:barbell', '1:barbell,2:bench'),
    (209, '0061', 'barbell lying triceps extension', 'requisitos', '1:barbell', '1:barbell,2:bench'),
    (210, '0060', 'barbell lying triceps extension skull crusher', 'requisitos', '1:barbell', '1:barbell,2:bench'),
    (212, '1751', 'barbell pin presses', 'requisitos', '1:barbell', '1:barbell,2:bench'),
    (214, '0082', 'barbell reverse wrist curl', 'requisitos', '1:barbell', '1:barbell,2:bench'),
    (224, '0071', 'barbell press sit-up', 'requires_floor', 'true', 'false'),
    (224, '0071', 'barbell press sit-up', 'requisitos', '1:barbell', '1:barbell,2:bench'),
    (230, '0073', 'barbell pullover', 'requisitos', '1:barbell', '1:barbell,2:bench'),
    (231, '0022', 'barbell pullover to press', 'requisitos', '1:barbell', '1:barbell,2:bench'),
    (237, '0077', 'barbell rear lunge v. 2', 'requisitos', '1:barbell', '1:barbell,2:step platform'),
    (243, '0099', 'barbell single leg split squat', 'requisitos', '1:barbell', '1:barbell,2:bench'),
    (245, '0072', 'barbell prone incline curl', 'requisitos', '1:barbell', '1:barbell,2:bench'),
    (248, '1721', 'barbell reverse grip skullcrusher', 'requisitos', '1:barbell', '1:barbell,2:bench'),
    (257, '0126', 'barbell wrist curl', 'requisitos', '1:barbell', '1:barbell,2:bench'),
    (260, '0108', 'barbell standing leg calf raise', 'requisitos', '1:barbell', '1:barbell,2:step platform'),
    (280, '2798', 'barbell squat jump step rear lunge', 'requisitos', '1:barbell', '1:barbell,2:step platform'),
    (295, '1374', 'box jump down with one leg stabilization', 'equipment', 'plyo box', 'bench'),
    (295, '1374', 'box jump down with one leg stabilization', 'requisitos', '1:plyo box', '1:bench'),
    (296, '3544', 'bodyweight incline side plank', 'equipment', 'body weight', 'bench'),
    (296, '3544', 'bodyweight incline side plank', 'requisitos', '', '1:bench'),
    (314, '1399', 'bench dip on floor', 'equipment', 'body weight', 'bench'),
    (314, '1399', 'bench dip on floor', 'requisitos', '', '1:bench'),
    (316, '0139', 'biceps narrow pull-ups', 'movement_pattern', 'VERTICAL_PULL', 'HORIZONTAL_PULL'),
    (316, '0139', 'biceps narrow pull-ups', 'requires_hanging', 'true', 'false'),
    (316, '0139', 'biceps narrow pull-ups', 'requires_overhead', 'true', 'false'),
    (323, '0151', 'cable bench press', 'requisitos', '1:cable', '1:cable,2:bench'),
    (338, '0158', 'cable decline fly', 'requisitos', '1:cable', '1:cable,2:bench'),
    (339, '1260', 'cable decline one arm press', 'requisitos', '1:cable', '1:cable,2:bench'),
    (340, '1261', 'cable decline press', 'requisitos', '1:cable', '1:cable,2:bench'),
    (342, '0171', 'cable incline fly', 'requisitos', '1:cable', '1:cable,2:bench'),
    (345, '0185', 'cable lying fly', 'requisitos', '1:cable', '1:cable,2:bench'),
    (353, '0172', 'cable incline pushdown', 'requisitos', '1:cable', '1:cable,2:bench'),
    (358, '0184', 'cable lying extension pullover (with rope attachment)', 'requisitos', '1:cable', '1:cable,2:bench'),
    (366, '0173', 'cable incline triceps extension', 'requisitos', '1:cable', '1:cable,2:bench'),
    (369, '1634', 'cable lying bicep curl', 'requisitos', '1:cable', '1:cable,2:bench'),
    (370, '0182', 'cable lying close-grip curl', 'requisitos', '1:cable', '1:cable,2:bench'),
    (377, '1262', 'cable one arm decline chest fly', 'requisitos', '1:cable', '1:cable,2:bench'),
    (380, '1265', 'cable one arm incline press', 'requisitos', '1:cable', '1:cable,2:bench'),
    (382, '0191', 'cable one arm lateral bent-over', 'body_part', 'chest', 'shoulders'),
    (382, '0191', 'cable one arm lateral bent-over', 'movement_pattern', 'TRUNK_ROTATION_LATERAL_FLEXION', 'SHOULDER_RAISE'),
    (382, '0191', 'cable one arm lateral bent-over', 'target_muscle', 'pectorals', 'delts'),
    (391, '0199', 'cable pushdown (straight arm) v. 2', 'movement_pattern', 'ELBOW_EXTENSION', 'SHOULDER_EXTENSION'),
    (393, '0208', 'cable reverse-grip straight back seated high row', 'movement_pattern', 'HORIZONTAL_PULL', 'VERTICAL_PULL'),
    (427, '0221', 'cable side bend crunch (bosu ball)', 'requisitos', '1:cable', '1:cable,2:bosu ball'),
    (443, '1725', 'cable rope incline tricep extension', 'requisitos', '1:cable', '1:cable,2:bench'),
    (449, '1644', 'cable squatting curl', 'movement_pattern', 'KNEE_DOMINANT', 'ELBOW_FLEXION'),
    (468, '0248', 'cambered bar lying row', 'requisitos', '1:barbell', '1:barbell,2:bench'),
    (471, '0240', 'cable supine reverse fly', 'requisitos', '1:cable', '1:cable,2:bench'),
    (493, '0284', 'donkey calf raise', 'equipment', 'body weight', 'bench'),
    (493, '0284', 'donkey calf raise', 'requisitos', '', '1:bench,2:step platform'),
    (495, '0279', 'decline push-up', 'equipment', 'body weight', 'bench'),
    (495, '0279', 'decline push-up', 'requisitos', '', '1:bench'),
    (502, '3016', 'curl-up', 'movement_pattern', 'ELBOW_FLEXION', 'TRUNK_FLEXION'),
    (528, '1652', 'dumbbell bicep curl on exercise ball with leg raised', 'movement_pattern', 'TRUNK_FLEXION', 'ELBOW_FLEXION'),
    (560, '1731', 'dumbbell close grip press', 'requisitos', '1:dumbbell', '1:dumbbell,2:bench'),
    (561, '0296', 'dumbbell close-grip press', 'requisitos', '1:dumbbell', '1:dumbbell,2:bench'),
    (606, '0333', 'dumbbell kickback', 'requisitos', '1:dumbbell', '1:dumbbell,2:bench'),
    (617, '0863', 'dumbbell lying external shoulder rotation', 'requisitos', '1:dumbbell,2:bench', '1:dumbbell'),
    (633, '1290', 'dumbbell one arm press on exercise ball', 'movement_pattern', 'VERTICAL_PUSH', 'HORIZONTAL_PUSH'),
    (633, '1290', 'dumbbell one arm press on exercise ball', 'requires_overhead', 'true', 'false'),
    (636, '0292', 'dumbbell one arm bent-over row', 'requisitos', '1:dumbbell', '1:dumbbell,2:bench'),
    (637, '0339', 'dumbbell lying femoral', 'requires_floor', 'true', 'false'),
    (637, '0339', 'dumbbell lying femoral', 'requisitos', '1:dumbbell', '1:dumbbell,2:bench'),
    (649, '1621', 'dumbbell one arm hammer press on exercise ball', 'movement_pattern', 'VERTICAL_PUSH', 'HORIZONTAL_PUSH'),
    (649, '1621', 'dumbbell one arm hammer press on exercise ball', 'requires_overhead', 'true', 'false'),
    (667, '1622', 'dumbbell one arm reverse grip press', 'movement_pattern', 'VERTICAL_PUSH', 'HORIZONTAL_PUSH'),
    (667, '1622', 'dumbbell one arm reverse grip press', 'requires_overhead', 'true', 'false'),
    (667, '1622', 'dumbbell one arm reverse grip press', 'requisitos', '1:dumbbell', '1:dumbbell,2:bench'),
    (677, '1667', 'dumbbell one arm reverse spider curl', 'requisitos', '1:dumbbell', '1:dumbbell,2:bench'),
    (684, '0365', 'dumbbell over bench neutral wrist curl', 'body_part', 'upper arms', 'lower arms'),
    (684, '0365', 'dumbbell over bench neutral wrist curl', 'target_muscle', 'biceps', 'forearms'),
    (685, '0366', 'dumbbell over bench one arm neutral wrist curl', 'body_part', 'upper arms', 'lower arms'),
    (685, '0366', 'dumbbell over bench one arm neutral wrist curl', 'target_muscle', 'biceps', 'forearms'),
    (690, '0373', 'dumbbell pronate-grip triceps extension', 'requisitos', '1:dumbbell', '1:dumbbell,2:bench'),
    (696, '0377', 'dumbbell rear delt row_shoulder', 'requisitos', '1:dumbbell', '1:dumbbell,2:bench'),
    (699, '0379', 'dumbbell rear lateral raise (support head)', 'requisitos', '1:dumbbell', '1:dumbbell,2:bench'),
    (700, '0383', 'dumbbell reverse fly', 'requisitos', '1:dumbbell', '1:dumbbell,2:bench'),
    (701, '0386', 'dumbbell rotation reverse fly', 'requisitos', '1:dumbbell', '1:dumbbell,2:bench'),
    (718, '1675', 'dumbbell reverse spider curl', 'requisitos', '1:dumbbell', '1:dumbbell,2:bench'),
    (730, '0397', 'dumbbell seated neutral wrist curl', 'body_part', 'upper arms', 'lower arms'),
    (730, '0397', 'dumbbell seated neutral wrist curl', 'target_muscle', 'biceps', 'forearms'),
    (738, '0417', 'dumbbell standing calf raise', 'requisitos', '1:dumbbell', '1:dumbbell,2:step platform'),
    (741, '0408', 'dumbbell side lying one hand raise', 'requires_floor', 'true', 'false'),
    (741, '0408', 'dumbbell side lying one hand raise', 'requisitos', '1:dumbbell', '1:dumbbell,2:bench'),
    (753, '0410', 'dumbbell single leg split squat', 'requisitos', '1:dumbbell', '1:dumbbell,2:bench'),
    (777, '3287', 'elbow dips', 'equipment', 'body weight', 'parallel bars'),
    (777, '3287', 'elbow dips', 'requisitos', '', '1:parallel bars'),
    (786, '2796', 'dumbbell step-up lunge', 'requisitos', '1:dumbbell,2:plyo box,2:step platform', '1:dumbbell,2:bench,2:plyo box,2:step platform'),
    (791, '2803', 'dumbbell supported squat', 'requisitos', '1:dumbbell', '1:dumbbell,2:bench,2:plyo box'),
    (797, '0436', 'dumbbell tate press', 'requisitos', '1:dumbbell', '1:dumbbell,2:bench'),
    (816, '1382', 'exercise ball on the wall calf raise', 'requisitos', '1:stability ball', '1:stability ball,2:dumbbell'),
    (817, '3241', 'exercise ball on the wall calf raise (tennis ball between ankles)', 'requisitos', '1:stability ball', '1:stability ball,2:dumbbell'),
    (818, '3240', 'exercise ball on the wall calf raise (tennis ball between knees)', 'requisitos', '1:stability ball', '1:stability ball,2:dumbbell'),
    (830, '0445', 'ez barbell anti gravity press', 'requisitos', '1:ez barbell', '1:ez barbell,2:bench'),
    (831, '3010', 'ez bar lying bent arms pullover', 'requisitos', '1:ez barbell', '1:ez barbell,2:bench'),
    (834, '1748', 'ez bar lying close grip triceps extension behind head', 'requisitos', '1:ez barbell', '1:ez barbell,2:bench'),
    (840, '0448', 'ez barbell decline close grip face press', 'requisitos', '1:ez barbell', '1:ez barbell,2:bench'),
    (841, '2186', 'ez barbell decline triceps extension', 'requisitos', '1:ez barbell', '1:ez barbell,2:bench'),
    (842, '0449', 'ez barbell incline triceps extension', 'requisitos', '1:ez barbell', '1:ez barbell,2:bench'),
    (848, '0454', 'ez barbell spider curl', 'requisitos', '1:ez barbell', '1:ez barbell,2:bench'),
    (853, '3221', 'half knee bends (male)', 'movement_pattern', 'KNEE_DOMINANT', 'LOCOMOTION_CARDIO'),
    (875, '0459', 'flutter kicks', 'equipment', 'body weight', 'bench'),
    (875, '0459', 'flutter kicks', 'requires_floor', 'true', 'false'),
    (875, '0459', 'flutter kicks', 'requisitos', '', '1:bench'),
    (884, '1418', 'hug keens to chest', 'movement_pattern', 'KNEE_DOMINANT', 'MOBILITY'),
    (895, '0492', 'incline push up depth jump', 'equipment', 'body weight', 'bench'),
    (895, '0492', 'incline push up depth jump', 'requisitos', '', '1:bench,2:step platform'),
    (896, '0493', 'incline push-up', 'equipment', 'body weight', 'step platform'),
    (896, '0493', 'incline push-up', 'requisitos', '', '1:step platform'),
    (898, '0494', 'incline reverse grip push-up', 'equipment', 'body weight', 'smith machine'),
    (898, '0494', 'incline reverse grip push-up', 'requisitos', '', '1:smith machine'),
    (921, '0490', 'incline close-grip push-up', 'equipment', 'body weight', 'smith machine'),
    (921, '0490', 'incline close-grip push-up', 'requisitos', '', '1:smith machine'),
    (977, '3418', 'l-pull-up', 'requisitos', '1:pull-up bar', '1:captains chair,1:pull-up bar'),
    (981, '0574', 'lever bent over row', 'requisitos', '1:barbell', '1:barbell,2:landmine'),
    (1019, '0590', 'lever one arm shoulder press', 'equipment', 'leverage machine', 'barbell'),
    (1019, '0590', 'lever one arm shoulder press', 'requisitos', '1:leverage machine', '1:barbell,2:landmine'),
    (1022, '0603', 'lever shoulder press', 'equipment', 'leverage machine', 'barbell'),
    (1022, '0603', 'lever shoulder press', 'requisitos', '1:leverage machine', '1:barbell,2:landmine'),
    (1056, '1386', 'one leg donkey calf raise', 'equipment', 'body weight', 'bench'),
    (1056, '1386', 'one leg donkey calf raise', 'requisitos', '', '1:bench'),
    (1062, '0865', 'lying leg-hip raise', 'equipment', 'body weight', 'bench'),
    (1062, '0865', 'lying leg-hip raise', 'requires_floor', 'true', 'false'),
    (1062, '0865', 'lying leg-hip raise', 'requisitos', '', '1:bench'),
    (1063, '0634', 'negative crunch', 'equipment', 'body weight', 'bench'),
    (1063, '0634', 'negative crunch', 'requires_floor', 'true', 'false'),
    (1063, '0634', 'negative crunch', 'requisitos', '', '1:bench'),
    (1064, '1495', 'oblique crunch v. 2', 'equipment', 'body weight', 'bench'),
    (1064, '1495', 'oblique crunch v. 2', 'requisitos', '', '1:bench'),
    (1078, '0639', 'one arm dip', 'equipment', 'body weight', 'bench'),
    (1078, '0639', 'one arm dip', 'requisitos', '', '1:bench'),
    (1093, '3638', 'push to run', 'requires_floor', 'true', 'false'),
    (1095, '1689', 'push and pull bodyweight', 'requires_floor', 'true', 'false'),
    (1103, '3119', 'potty squat', 'body_part', 'waist', 'upper legs'),
    (1103, '3119', 'potty squat', 'target_muscle', 'abs', 'quads'),
    (1110, '1476', 'one leg squat', 'equipment', 'body weight', 'bench'),
    (1110, '1476', 'one leg squat', 'requisitos', '', '1:bench,1:plyo box'),
    (1134, '0684', 'run (equipment)', 'equipment', 'body weight', 'treadmill'),
    (1134, '0684', 'run (equipment)', 'requisitos', '', '1:treadmill'),
    (1142, '0689', 'seated leg raise', 'equipment', 'body weight', 'bench'),
    (1142, '0689', 'seated leg raise', 'requisitos', '', '1:bench'),
    (1146, '3012', 'scapula dips', 'equipment', 'body weight', 'bench'),
    (1146, '3012', 'scapula dips', 'requisitos', '', '1:bench'),
    (1152, '0672', 'reverse dip', 'equipment', 'parallel bars', 'body weight'),
    (1152, '0672', 'reverse dip', 'requisitos', '1:parallel bars', ''),
    (1156, '3144', 'resistance band seated straight back row', 'requires_floor', 'true', 'false'),
    (1181, '0691', 'seated side crunch (wall)', 'requires_floor', 'true', 'false'),
    (1182, '3699', 'shoulder tap', 'equipment', 'body weight', 'plyo box'),
    (1182, '3699', 'shoulder tap', 'requisitos', '', '1:plyo box,1:step platform'),
    (1193, '2567', 'seated piriformis stretch', 'requires_floor', 'true', 'false'),
    (1197, '0696', 'self assisted inverse leg curl (on floor)', 'equipment', 'body weight', 'step platform'),
    (1197, '0696', 'self assisted inverse leg curl (on floor)', 'requisitos', '', '1:step platform'),
    (1214, '0763', 'smith reverse calf raises', 'requisitos', '1:smith machine', '1:smith machine,2:step platform'),
    (1220, '0754', 'smith decline reverse-grip press', 'requisitos', '1:smith machine', '1:smith machine,2:bench'),
    (1222, '0758', 'smith incline reverse-grip press', 'requisitos', '1:smith machine', '1:smith machine,2:bench'),
    (1223, '0759', 'smith incline shoulder raises', 'requisitos', '1:smith machine', '1:smith machine,2:bench'),
    (1242, '1752', 'smith machine incline tricep extension', 'requisitos', '1:smith machine', '1:smith machine,2:bench'),
    (1255, '1398', 'standing calves calf stretch', 'equipment', 'body weight', 'bench'),
    (1255, '1398', 'standing calves calf stretch', 'requisitos', '', '1:bench'),
    (1278, '1396', 'smith toe raise', 'axial_load', 'true', 'false'),
    (1284, '0764', 'smith reverse-grip press', 'requisitos', '1:smith machine', '1:smith machine,2:bench'),
    (1288, '0768', 'smith single leg split squat', 'requisitos', '1:smith machine', '1:smith machine,2:bench'),
    (1298, '3314', 'straddle maltese', 'requires_hanging', 'true', 'false'),
    (1298, '3314', 'straddle maltese', 'requires_overhead', 'true', 'false'),
    (1301, '0807', 'suspended reverse crunch', 'requires_hanging', 'true', 'false'),
    (1301, '0807', 'suspended reverse crunch', 'requires_overhead', 'true', 'false'),
    (1303, '2802', 'twisted leg raise', 'equipment', 'body weight', 'parallel bars'),
    (1303, '2802', 'twisted leg raise', 'requires_floor', 'true', 'false'),
    (1303, '2802', 'twisted leg raise', 'requisitos', '', '1:captains chair,1:parallel bars'),
    (1304, '2801', 'twisted leg raise (female)', 'equipment', 'body weight', 'parallel bars'),
    (1304, '2801', 'twisted leg raise (female)', 'requires_floor', 'true', 'false'),
    (1304, '2801', 'twisted leg raise (female)', 'requisitos', '', '1:captains chair,1:parallel bars'),
    (1309, '1365', 'upper back stretch', 'requires_overhead', 'true', 'false'),
    (1311, '1427', 'straight leg outer hip abductor', 'requires_floor', 'true', 'false'),
    (1317, '0814', 'triceps dip', 'equipment', 'body weight', 'parallel bars'),
    (1317, '0814', 'triceps dip', 'requisitos', '', '1:parallel bars'),
    (1321, '0816', 'triceps press', 'equipment', 'body weight', 'smith machine'),
    (1321, '0816', 'triceps press', 'requisitos', '', '1:smith machine'),
    (1322, '0817', 'triceps stretch', 'requires_overhead', 'true', 'false');

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
        RAISE EXCEPTION 'V37.1: estado anterior inesperado -> %', distintos;
    END IF;
END $$;

UPDATE public.exercises e
   SET requires_floor = coalesce((SELECT c.despues::boolean FROM v37_cambios c WHERE c.exercise_id = e.exercise_id AND c.columna = 'requires_floor'), e.requires_floor),
       requires_hanging = coalesce((SELECT c.despues::boolean FROM v37_cambios c WHERE c.exercise_id = e.exercise_id AND c.columna = 'requires_hanging'), e.requires_hanging),
       requires_overhead = coalesce((SELECT c.despues::boolean FROM v37_cambios c WHERE c.exercise_id = e.exercise_id AND c.columna = 'requires_overhead'), e.requires_overhead),
       axial_load = coalesce((SELECT c.despues::boolean FROM v37_cambios c WHERE c.exercise_id = e.exercise_id AND c.columna = 'axial_load'), e.axial_load)
 WHERE e.exercise_id IN (SELECT exercise_id FROM v37_cambios WHERE columna IN ('requires_floor', 'requires_hanging', 'requires_overhead', 'axial_load'));
UPDATE public.exercises e SET movement_pattern = c.despues FROM v37_cambios c
 WHERE c.exercise_id = e.exercise_id AND c.columna = 'movement_pattern';
UPDATE public.exercises e SET target_muscle = c.despues FROM v37_cambios c
 WHERE c.exercise_id = e.exercise_id AND c.columna = 'target_muscle';
UPDATE public.exercises e SET body_part_id = bp.body_part_id FROM v37_cambios c
  JOIN public.body_parts bp ON bp.code = c.despues
 WHERE c.exercise_id = e.exercise_id AND c.columna = 'body_part';
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
        RAISE EXCEPTION 'V37.1: estado final inesperado -> %', distintos;
    END IF;
    IF EXISTS (SELECT 1 FROM public.exercises WHERE requires_hanging AND NOT requires_overhead) THEN
        RAISE EXCEPTION 'V37.1: hay ejercicios con suspension y sin brazos arriba';
    END IF;
END $$;
