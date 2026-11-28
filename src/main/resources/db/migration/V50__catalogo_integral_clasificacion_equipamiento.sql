-- V50 - Catalogo integral (auditoria visual 2026-11): CLASIFICACION y EQUIPAMIENTO verificados con el GIF.
-- Clasificacion: 9 ejercicios cuyo patron (o grupo) contradice el movimiento del GIF, con el mismo criterio que sus
-- analogos del catalogo. Equipamiento: 17 ejercicios a los que les faltaba un implemento imprescindible o tenian uno
-- que no es el del GIF. NO se anade «bench» a los ejercicios que solo se hacen sentados (sirve cualquier asiento).
-- Cada cambio exige el estado anterior (nombre de la fuente y valor previo). Detalle: docs/FitSense/catalogo_integral/.

DO $$ BEGIN
  IF (SELECT count(*) FROM equipment_types WHERE code IN ('smith machine','captains chair','gymnastic rings','stationary bike',
      'landmine','barbell','hyperextension bench','pull-up bar','squat rack','bench','dumbbell','leverage machine','ghd bench','other')) <> 14 THEN
    RAISE EXCEPTION 'V50: faltan codigos de equipamiento (V22/V38/V39)';
  END IF;
END $$;

-- ==== CLASIFICACION ====
-- 300 bench pull-ups: colgado de una barra alta con un pie en un banco como asistencia: traccion vertical.
UPDATE exercises SET movement_pattern = 'VERTICAL_PULL'
 WHERE exercise_id = 300 AND name_en = 'bench pull-ups' AND movement_pattern = 'HORIZONTAL_PULL';
-- 322 cable cross-over revers fly: pajaro (apertura posterior) cruzado en polea: abduccion, como las demas aperturas posteriores (100, 217, 416, 419...).
UPDATE exercises SET movement_pattern = 'SHOULDER_RAISE'
 WHERE exercise_id = 322 AND name_en = 'cable cross-over revers fly' AND movement_pattern = 'HORIZONTAL_ADDUCTION';
-- 353 cable incline pushdown: en banco inclinado, brazos casi rectos desde arriba hasta la cadera: extension de hombro (jalon con brazos rectos).
UPDATE exercises SET movement_pattern = 'SHOULDER_EXTENSION'
 WHERE exercise_id = 353 AND name_en = 'cable incline pushdown' AND movement_pattern = 'ELBOW_EXTENSION';
-- 382 cable one arm lateral bent-over: inclinado, el brazo cruza por delante del cuerpo desde el lado: cruce de polea a una mano (aduccion horizontal, pectoral).
UPDATE exercises SET body_part_id = (SELECT body_part_id FROM body_parts WHERE code = 'chest'), target_muscle = 'pectorals', movement_pattern = 'HORIZONTAL_ADDUCTION'
 WHERE exercise_id = 382 AND name_en = 'cable one arm lateral bent-over' AND movement_pattern = 'SHOULDER_RAISE' AND target_muscle = 'delts';
-- 407 cable rear drive: brazo a la altura del hombro, el codo se extiende hacia delante: extension de codo.
UPDATE exercises SET movement_pattern = 'ELBOW_EXTENSION'
 WHERE exercise_id = 407 AND name_en = 'cable rear drive' AND movement_pattern = 'SHOULDER_EXTENSION';
-- 574 dumbbell incline raise: sentado en banco inclinado, elevacion frontal hasta la altura de los hombros (no es un press).
UPDATE exercises SET movement_pattern = 'SHOULDER_RAISE'
 WHERE exercise_id = 574 AND name_en = 'dumbbell incline raise' AND movement_pattern = 'VERTICAL_PUSH';
-- 875 flutter kicks: boca abajo en un banco, patadas alternas de piernas estiradas: extension de cadera (gluteo).
UPDATE exercises SET movement_pattern = 'HIP_EXTENSION'
 WHERE exercise_id = 875 AND name_en = 'flutter kicks' AND movement_pattern = 'TRUNK_FLEXION';
-- 1070 one arm against wall: brazo apoyado en la pared por encima de la cabeza, estiramiento del dorsal.
UPDATE exercises SET movement_pattern = 'MOBILITY'
 WHERE exercise_id = 1070 AND name_en = 'one arm against wall' AND movement_pattern = 'OTHER';
-- 1254 standing calves: manos en la pared, inclinacion con talones en el suelo: estiramiento de gemelo, como 454 y 1255 (MOBILITY, DURATION).
UPDATE exercises SET movement_pattern = 'MOBILITY', default_prescription = 'DURATION', name_es = 'Estiramiento de gemelos inclinado contra la pared'
 WHERE exercise_id = 1254 AND name_en = 'standing calves' AND movement_pattern = 'ANKLE_PLANTAR_FLEXION' AND default_prescription = 'SETS_REPS' AND name_es = 'Gemelos de pie';
-- 1254: con prescripcion DURATION, el ultimo paso de V49 pasa a «Mantén la duración indicada.».
UPDATE exercises SET instructions_es = replace(instructions_es, 'Repite el número de repeticiones indicado.', 'Mantén la duración indicada.'),
       instruction_steps_es = replace(instruction_steps_es::text, 'Repite el número de repeticiones indicado.', 'Mantén la duración indicada.')::jsonb
 WHERE exercise_id = 1254 AND default_prescription = 'DURATION' AND instructions_es LIKE '%Repite el número de repeticiones indicado.';

-- ==== EQUIPAMIENTO: implemento que falta (grupo nuevo = imprescindible; mismo grupo = alternativa) ====
-- 316 biceps narrow pull-ups: remo invertido bajo una barra baja de maquina Smith: alternativa a la barra (como 910).
INSERT INTO exercise_equipment_requirements (exercise_id, group_no, equipment_id)
SELECT 316, 1, (SELECT equipment_id FROM equipment_types WHERE code = 'smith machine') WHERE EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 316 AND name_en = 'biceps narrow pull-ups')
   AND NOT EXISTS (SELECT 1 FROM exercise_equipment_requirements WHERE exercise_id = 316 AND group_no = 1 AND equipment_id = (SELECT equipment_id FROM equipment_types WHERE code = 'smith machine'));
-- 1306 vertical leg raise (on parallel bars): el GIF es una silla romana: alternativa a las paralelas (como 1303 y 1304).
INSERT INTO exercise_equipment_requirements (exercise_id, group_no, equipment_id)
SELECT 1306, 1, (SELECT equipment_id FROM equipment_types WHERE code = 'captains chair') WHERE EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1306 AND name_en = 'vertical leg raise (on parallel bars)')
   AND NOT EXISTS (SELECT 1 FROM exercise_equipment_requirements WHERE exercise_id = 1306 AND group_no = 1 AND equipment_id = (SELECT equipment_id FROM equipment_types WHERE code = 'captains chair'));
-- 1307 suspended row: el GIF usa anillas: alternativa a las correas de suspension.
INSERT INTO exercise_equipment_requirements (exercise_id, group_no, equipment_id)
SELECT 1307, 1, (SELECT equipment_id FROM equipment_types WHERE code = 'gymnastic rings') WHERE EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1307 AND name_en = 'suspended row')
   AND NOT EXISTS (SELECT 1 FROM exercise_equipment_requirements WHERE exercise_id = 1307 AND group_no = 1 AND equipment_id = (SELECT equipment_id FROM equipment_types WHERE code = 'gymnastic rings'));
-- 912 inverted row on bench: remo invertido con los pies en un banco: hace falta una barra baja (Smith).
INSERT INTO exercise_equipment_requirements (exercise_id, group_no, equipment_id)
SELECT 912, 2, (SELECT equipment_id FROM equipment_types WHERE code = 'smith machine') WHERE EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 912 AND name_en = 'inverted row on bench')
   AND NOT EXISTS (SELECT 1 FROM exercise_equipment_requirements WHERE exercise_id = 912 AND group_no = 2 AND equipment_id = (SELECT equipment_id FROM equipment_types WHERE code = 'smith machine'));
-- 912 inverted row on bench: ... o una barra fija baja (alternativa en el mismo grupo).
INSERT INTO exercise_equipment_requirements (exercise_id, group_no, equipment_id)
SELECT 912, 2, (SELECT equipment_id FROM equipment_types WHERE code = 'pull-up bar') WHERE EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 912 AND name_en = 'inverted row on bench')
   AND NOT EXISTS (SELECT 1 FROM exercise_equipment_requirements WHERE exercise_id = 912 AND group_no = 2 AND equipment_id = (SELECT equipment_id FROM equipment_types WHERE code = 'pull-up bar'));
-- 212 barbell pin presses: press desde los topes de seguridad de un rack de potencia.
INSERT INTO exercise_equipment_requirements (exercise_id, group_no, equipment_id)
SELECT 212, 3, (SELECT equipment_id FROM equipment_types WHERE code = 'squat rack') WHERE EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 212 AND name_en = 'barbell pin presses')
   AND NOT EXISTS (SELECT 1 FROM exercise_equipment_requirements WHERE exercise_id = 212 AND group_no = 3 AND equipment_id = (SELECT equipment_id FROM equipment_types WHERE code = 'squat rack'));
-- 496 drop push up: flexion con caida entre dos bancos.
INSERT INTO exercise_equipment_requirements (exercise_id, group_no, equipment_id)
SELECT 496, 1, (SELECT equipment_id FROM equipment_types WHERE code = 'bench') WHERE EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 496 AND name_en = 'drop push up')
   AND NOT EXISTS (SELECT 1 FROM exercise_equipment_requirements WHERE exercise_id = 496 AND group_no = 1 AND equipment_id = (SELECT equipment_id FROM equipment_types WHERE code = 'bench'));
-- 820 exercise ball supine triceps extension: extension de triceps con mancuerna sobre el balon.
INSERT INTO exercise_equipment_requirements (exercise_id, group_no, equipment_id)
SELECT 820, 2, (SELECT equipment_id FROM equipment_types WHERE code = 'dumbbell') WHERE EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 820 AND name_en = 'exercise ball supine triceps extension')
   AND NOT EXISTS (SELECT 1 FROM exercise_equipment_requirements WHERE exercise_id = 820 AND group_no = 2 AND equipment_id = (SELECT equipment_id FROM equipment_types WHERE code = 'dumbbell'));
-- 1195 self assisted inverse leg curl: de rodillas en una estacion con los tobillos sujetos por rodillos.
INSERT INTO exercise_equipment_requirements (exercise_id, group_no, equipment_id)
SELECT 1195, 1, (SELECT equipment_id FROM equipment_types WHERE code = 'leverage machine') WHERE EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1195 AND name_en = 'self assisted inverse leg curl')
   AND NOT EXISTS (SELECT 1 FROM exercise_equipment_requirements WHERE exercise_id = 1195 AND group_no = 1 AND equipment_id = (SELECT equipment_id FROM equipment_types WHERE code = 'leverage machine'));
-- 1195 self assisted inverse leg curl: ... o un banco GHD (alternativa).
INSERT INTO exercise_equipment_requirements (exercise_id, group_no, equipment_id)
SELECT 1195, 1, (SELECT equipment_id FROM equipment_types WHERE code = 'ghd bench') WHERE EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1195 AND name_en = 'self assisted inverse leg curl')
   AND NOT EXISTS (SELECT 1 FROM exercise_equipment_requirements WHERE exercise_id = 1195 AND group_no = 1 AND equipment_id = (SELECT equipment_id FROM equipment_types WHERE code = 'ghd bench'));
-- 1196 self assisted inverse leg curl: tobillos enganchados bajo un banco.
INSERT INTO exercise_equipment_requirements (exercise_id, group_no, equipment_id)
SELECT 1196, 1, (SELECT equipment_id FROM equipment_types WHERE code = 'bench') WHERE EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1196 AND name_en = 'self assisted inverse leg curl')
   AND NOT EXISTS (SELECT 1 FROM exercise_equipment_requirements WHERE exercise_id = 1196 AND group_no = 1 AND equipment_id = (SELECT equipment_id FROM equipment_types WHERE code = 'bench'));
-- 1201 side plank hip adduction: plancha lateral con el pie de arriba sobre un banco.
INSERT INTO exercise_equipment_requirements (exercise_id, group_no, equipment_id)
SELECT 1201, 1, (SELECT equipment_id FROM equipment_types WHERE code = 'bench') WHERE EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1201 AND name_en = 'side plank hip adduction')
   AND NOT EXISTS (SELECT 1 FROM exercise_equipment_requirements WHERE exercise_id = 1201 AND group_no = 1 AND equipment_id = (SELECT equipment_id FROM equipment_types WHERE code = 'bench'));
-- 61 arm slingers hanging bent knee legs: codos en correas para abdominales colgadas de la barra.
INSERT INTO exercise_equipment_requirements (exercise_id, group_no, equipment_id)
SELECT 61, 2, (SELECT equipment_id FROM equipment_types WHERE code = 'other') WHERE EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 61 AND name_en = 'arm slingers hanging bent knee legs')
   AND NOT EXISTS (SELECT 1 FROM exercise_equipment_requirements WHERE exercise_id = 61 AND group_no = 2 AND equipment_id = (SELECT equipment_id FROM equipment_types WHERE code = 'other'));
-- 62 arm slingers hanging straight legs: codos en correas para abdominales colgadas de la barra.
INSERT INTO exercise_equipment_requirements (exercise_id, group_no, equipment_id)
SELECT 62, 2, (SELECT equipment_id FROM equipment_types WHERE code = 'other') WHERE EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 62 AND name_en = 'arm slingers hanging straight legs')
   AND NOT EXISTS (SELECT 1 FROM exercise_equipment_requirements WHERE exercise_id = 62 AND group_no = 2 AND equipment_id = (SELECT equipment_id FROM equipment_types WHERE code = 'other'));
-- 871 hanging straight twisting leg hip raise: brazos en correas para abdominales colgadas de la barra.
INSERT INTO exercise_equipment_requirements (exercise_id, group_no, equipment_id)
SELECT 871, 2, (SELECT equipment_id FROM equipment_types WHERE code = 'other') WHERE EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 871 AND name_en = 'hanging straight twisting leg hip raise')
   AND NOT EXISTS (SELECT 1 FROM exercise_equipment_requirements WHERE exercise_id = 871 AND group_no = 2 AND equipment_id = (SELECT equipment_id FROM equipment_types WHERE code = 'other'));
-- 368 cable kneeling triceps extension: de rodillas detras de un banco con los brazos apoyados en el.
INSERT INTO exercise_equipment_requirements (exercise_id, group_no, equipment_id)
SELECT 368, 2, (SELECT equipment_id FROM equipment_types WHERE code = 'bench') WHERE EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 368 AND name_en = 'cable kneeling triceps extension')
   AND NOT EXISTS (SELECT 1 FROM exercise_equipment_requirements WHERE exercise_id = 368 AND group_no = 2 AND equipment_id = (SELECT equipment_id FROM equipment_types WHERE code = 'bench'));

-- ==== EQUIPAMIENTO: implemento que no es el del GIF (se sustituye) ====
-- 532 cycle cross trainer: bicicleta de aire con brazos moviles, no una maquina de palanca. Requisitos antes: 1:leverage machine.
DELETE FROM exercise_equipment_requirements WHERE exercise_id = 532 AND EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 532 AND name_en = 'cycle cross trainer')
   AND (SELECT string_agg(r.group_no || ':' || t.code, ',' ORDER BY r.group_no, t.code) FROM exercise_equipment_requirements r
        JOIN equipment_types t ON t.equipment_id = r.equipment_id WHERE r.exercise_id = 532) = '1:leverage machine';
INSERT INTO exercise_equipment_requirements (exercise_id, group_no, equipment_id)
SELECT 532, 1, (SELECT equipment_id FROM equipment_types WHERE code = 'stationary bike') WHERE EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 532 AND name_en = 'cycle cross trainer')
   AND NOT EXISTS (SELECT 1 FROM exercise_equipment_requirements WHERE exercise_id = 532);
UPDATE exercises SET equipment_id = (SELECT equipment_id FROM equipment_types WHERE code = 'stationary bike') WHERE exercise_id = 532 AND name_en = 'cycle cross trainer'
   AND equipment_id = (SELECT equipment_id FROM equipment_types WHERE code = 'leverage machine');
-- 997 lever bent-over row with v-bar: remo con el extremo de una barra anclada en landmine. Requisitos antes: 1:leverage machine.
DELETE FROM exercise_equipment_requirements WHERE exercise_id = 997 AND EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 997 AND name_en = 'lever bent-over row with v-bar')
   AND (SELECT string_agg(r.group_no || ':' || t.code, ',' ORDER BY r.group_no, t.code) FROM exercise_equipment_requirements r
        JOIN equipment_types t ON t.equipment_id = r.equipment_id WHERE r.exercise_id = 997) = '1:leverage machine';
INSERT INTO exercise_equipment_requirements (exercise_id, group_no, equipment_id)
SELECT 997, 1, (SELECT equipment_id FROM equipment_types WHERE code = 'landmine') WHERE EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 997 AND name_en = 'lever bent-over row with v-bar')
   AND NOT EXISTS (SELECT 1 FROM exercise_equipment_requirements WHERE exercise_id = 997);
INSERT INTO exercise_equipment_requirements (exercise_id, group_no, equipment_id)
SELECT 997, 2, (SELECT equipment_id FROM equipment_types WHERE code = 'barbell') WHERE EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 997 AND name_en = 'lever bent-over row with v-bar')
   AND (SELECT count(*) FROM exercise_equipment_requirements WHERE exercise_id = 997) = 1;
UPDATE exercises SET equipment_id = (SELECT equipment_id FROM equipment_types WHERE code = 'landmine') WHERE exercise_id = 997 AND name_en = 'lever bent-over row with v-bar'
   AND equipment_id = (SELECT equipment_id FROM equipment_types WHERE code = 'leverage machine');
-- 908 hyperextension: banco de hiperextensiones a 45 grados (un banco plano no sirve). Requisitos antes: 1:body weight,2:bench.
DELETE FROM exercise_equipment_requirements WHERE exercise_id = 908 AND EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 908 AND name_en = 'hyperextension')
   AND (SELECT string_agg(r.group_no || ':' || t.code, ',' ORDER BY r.group_no, t.code) FROM exercise_equipment_requirements r
        JOIN equipment_types t ON t.equipment_id = r.equipment_id WHERE r.exercise_id = 908) = '1:body weight,2:bench';
INSERT INTO exercise_equipment_requirements (exercise_id, group_no, equipment_id)
SELECT 908, 1, (SELECT equipment_id FROM equipment_types WHERE code = 'hyperextension bench') WHERE EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 908 AND name_en = 'hyperextension')
   AND NOT EXISTS (SELECT 1 FROM exercise_equipment_requirements WHERE exercise_id = 908);
UPDATE exercises SET equipment_id = (SELECT equipment_id FROM equipment_types WHERE code = 'hyperextension bench') WHERE exercise_id = 908 AND name_en = 'hyperextension'
   AND equipment_id = (SELECT equipment_id FROM equipment_types WHERE code = 'body weight');

-- ==== IMPLEMENTO PRINCIPAL de los que pasan de peso corporal a exigir un implemento ====
-- tg_requisitos_principal (V23, diferido) exige que el implemento principal este entre los requisitos.
UPDATE exercises SET equipment_id = (SELECT equipment_id FROM equipment_types WHERE code = 'bench')
 WHERE exercise_id IN (496, 1196, 1201)
   AND equipment_id = (SELECT equipment_id FROM equipment_types WHERE code = 'body weight')
   AND EXISTS (SELECT 1 FROM exercise_equipment_requirements r WHERE r.exercise_id = exercises.exercise_id);
UPDATE exercises SET equipment_id = (SELECT equipment_id FROM equipment_types WHERE code = 'leverage machine')
 WHERE exercise_id = 1195
   AND equipment_id = (SELECT equipment_id FROM equipment_types WHERE code = 'body weight')
   AND EXISTS (SELECT 1 FROM exercise_equipment_requirements r WHERE r.exercise_id = 1195);
