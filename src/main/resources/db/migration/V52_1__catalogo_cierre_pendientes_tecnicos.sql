-- =====================================================================
-- FitSense - V52.1: cierre de pendientes tecnicos y contencion de seguridad (orden de 14/11/2026, fase 2-3)
--
-- Cada cambio se comprobo contra la animacion COMPLETA del GIF original (hojas de fotogramas f1..fN), con la segunda
-- revision (R2) y la verificacion independiente (V3) como apoyo, y contra los precedentes del propio catalogo.
-- Evidencia: docs/FitSense/catalogo_integral/CIERRE_PENDIENTES.md y cierre/pendientes/.
--
--  231  El GIF es un press de banca con barra y los codos pegados: f1 brazos verticales sobre el pecho, f7 barra en el
--       pecho; la barra nunca pasa por encima ni detras de la cabeza (no hay pullover). Clasificacion como los otros
--       press de banca con agarre cerrado del catalogo (169, 201, 246, 851, 1239): upper arms / triceps, HORIZONTAL_PUSH.
--       La bandera OH se CONSERVA (conservador): quitarla relajaria una restriccion y queda para revision profesional.
--  367  El GIF es una extension de hombro con el brazo casi estirado en polea baja: el brazo va de colgar delante del
--       muslo a quedar en linea con el tronco, sin flexo-extension de codo, con el deltoides posterior resaltado.
--       shoulders / delts, SHOULDER_EXTENSION (como 262). El texto (V52) ya describe ese movimiento.
--  1323 Las instrucciones mandaban sentarse "con los muslos bajo los rodillos": el GIF muestra un banco plano entre dos
--       torres, sin rodillos. Se corrige solo esa frase.
--  1182 El equipo "plyo box / step platform" no existe en el GIF: los pies estan en el suelo (lo que parecia un cajon
--       son los propios pies vistos de frente, V3 con ampliacion de f1 y f13). Peso corporal; FL se conserva.
--  1017 Prensa de piernas: en f1 y f7 se resaltan tambien los gemelos. Se anaden como secundarios.
--  620  El GIF es una extension de hombro tumbado boca abajo: el brazo recto va de colgar hacia el suelo a quedar atras,
--       a lo largo del cuerpo, con la mancuerna a la altura de la cadera; no se separa hacia el lado (no es una
--       elevacion lateral). Nombre, patron SHOULDER_EXTENSION e instrucciones (V3 V07 + revision de esta orden).
--  868  Pike colgado: en f5-f8 las piernas pasan por encima de la barra y la cadera queda por encima de la cabeza.
--       Se ANADE requires_inversion (contencion conservadora; R2 lo habia propuesto). Solo restringe mas.
--
-- NO baja ninguna dificultad, NO quita ninguna bandera de seguridad, NO activa ni desactiva ejercicios y NO cambia el
-- significado de ninguna restriccion del perfil. Cada ejercicio exige su estado anterior exacto: si no coincide y no
-- esta ya corregido, la migracion FALLA.
-- =====================================================================

-- 231 barbell pullover to press
DO $v521$ DECLARE n INT; BEGIN
  UPDATE exercises SET name_es = 'Press de banca con barra y codos pegados',
         body_part_id = (SELECT body_part_id FROM body_parts WHERE code = 'upper arms'),
         target_muscle = 'triceps', synergist_muscle = 'chest', secondary_muscles = ARRAY['chest','shoulders']::varchar[],
         movement_pattern = 'HORIZONTAL_PUSH'
   WHERE exercise_id = 231 AND name_en = 'barbell pullover to press' AND name_es = 'Press desde pull-over con barra'
     AND body_part_id = (SELECT body_part_id FROM body_parts WHERE code = 'back') AND target_muscle = 'lats'
     AND movement_pattern = 'VERTICAL_PUSH';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 231 AND name_es = 'Press de banca con barra y codos pegados'
       AND target_muscle = 'triceps' AND movement_pattern = 'HORIZONTAL_PUSH') THEN
    RAISE EXCEPTION 'V52.1: ejercicio 231 no esta en el estado anterior esperado';
  END IF;
END $v521$;

-- 367 cable kickback
DO $v521$ DECLARE n INT; BEGIN
  UPDATE exercises SET name_es = 'Extensión de hombro con brazo estirado en polea baja',
         body_part_id = (SELECT body_part_id FROM body_parts WHERE code = 'shoulders'),
         target_muscle = 'delts', synergist_muscle = 'triceps', secondary_muscles = ARRAY['triceps']::varchar[],
         movement_pattern = 'SHOULDER_EXTENSION'
   WHERE exercise_id = 367 AND name_en = 'cable kickback' AND name_es = 'Patada de tríceps en cable'
     AND body_part_id = (SELECT body_part_id FROM body_parts WHERE code = 'upper arms') AND target_muscle = 'triceps'
     AND movement_pattern = 'ELBOW_EXTENSION';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 367
       AND name_es = 'Extensión de hombro con brazo estirado en polea baja' AND movement_pattern = 'SHOULDER_EXTENSION') THEN
    RAISE EXCEPTION 'V52.1: ejercicio 367 no esta en el estado anterior esperado';
  END IF;
END $v521$;

-- 1323 twin handle parallel grip lat pulldown
DO $v521$ DECLARE n INT; BEGIN
  UPDATE exercises SET
         instructions_es = replace(instructions_es, 'Siéntate en la máquina de polea alta con los muslos bajo los rodillos.',
                                   'Siéntate en el banco situado entre las dos poleas altas, con los pies apoyados en el suelo.'),
         instruction_steps_es = replace(instruction_steps_es::text, 'Siéntate en la máquina de polea alta con los muslos bajo los rodillos.',
                                   'Siéntate en el banco situado entre las dos poleas altas, con los pies apoyados en el suelo.')::jsonb
   WHERE exercise_id = 1323 AND name_en = 'twin handle parallel grip lat pulldown'
     AND instructions_es LIKE 'Siéntate en la máquina de polea alta con los muslos bajo los rodillos.%'
     AND instruction_steps_es::text LIKE '%Siéntate en la máquina de polea alta con los muslos bajo los rodillos.%';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1323
       AND instructions_es LIKE 'Siéntate en el banco situado entre las dos poleas altas, con los pies apoyados en el suelo.%'
       AND instructions_es NOT LIKE '%rodillos%') THEN
    RAISE EXCEPTION 'V52.1: ejercicio 1323 no esta en el estado anterior esperado';
  END IF;
END $v521$;

-- 1182 shoulder tap: peso corporal, sin requisitos
DO $v521$ BEGIN
  IF (SELECT coalesce(string_agg(r.group_no || ':' || t.code, ',' ORDER BY r.group_no || ':' || t.code), '') FROM exercise_equipment_requirements r JOIN equipment_types t ON t.equipment_id = r.equipment_id WHERE r.exercise_id = 1182) = ''
     AND (SELECT t.code FROM exercises e JOIN equipment_types t ON t.equipment_id = e.equipment_id WHERE e.exercise_id = 1182) = 'body weight' THEN RETURN; END IF;
  IF (SELECT coalesce(string_agg(r.group_no || ':' || t.code, ',' ORDER BY r.group_no || ':' || t.code), '') FROM exercise_equipment_requirements r JOIN equipment_types t ON t.equipment_id = r.equipment_id WHERE r.exercise_id = 1182) <> '1:plyo box,1:step platform'
     OR NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1182 AND name_en = 'shoulder tap') THEN
    RAISE EXCEPTION 'V52.1: requisitos de 1182 distintos de los esperados';
  END IF;
  DELETE FROM exercise_equipment_requirements WHERE exercise_id = 1182;
  UPDATE exercises SET equipment_id = (SELECT equipment_id FROM equipment_types WHERE code = 'body weight') WHERE exercise_id = 1182;
END $v521$;

-- 1017 lever seated squat calf raise on leg press machine: gemelos como secundarios
DO $v521$ DECLARE n INT; BEGIN
  UPDATE exercises SET secondary_muscles = secondary_muscles || ARRAY['calves']::varchar[]
   WHERE exercise_id = 1017 AND name_en = 'lever seated squat calf raise on leg press machine'
     AND NOT ('calves' = ANY(secondary_muscles)) AND movement_pattern = 'KNEE_DOMINANT';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 1017 AND 'calves' = ANY(secondary_muscles)) THEN
    RAISE EXCEPTION 'V52.1: ejercicio 1017 no esta en el estado anterior esperado';
  END IF;
END $v521$;

-- 620 dumbbell lying one arm rear lateral raise
DO $v521$ DECLARE n INT; BEGIN
  UPDATE exercises SET name_es = 'Extensión de hombro a una mano tumbado boca abajo con mancuerna',
         movement_pattern = 'SHOULDER_EXTENSION',
         instructions_es = 'Túmbate boca abajo en un banco plano con una mancuerna en una mano y el brazo colgando hacia el suelo. Con el brazo recto, llévalo hacia atrás, a lo largo del cuerpo, hasta que la mancuerna quede a la altura de la cadera. Haz una pausa breve y baja la mancuerna despacio a la posición inicial. Completa las repeticiones y cambia de brazo. Repite el número de repeticiones indicado.',
         instruction_steps_es = '["Túmbate boca abajo en un banco plano con una mancuerna en una mano y el brazo colgando hacia el suelo.", "Con el brazo recto, llévalo hacia atrás, a lo largo del cuerpo, hasta que la mancuerna quede a la altura de la cadera.", "Haz una pausa breve y baja la mancuerna despacio a la posición inicial.", "Completa las repeticiones y cambia de brazo.", "Repite el número de repeticiones indicado."]'::jsonb
   WHERE exercise_id = 620 AND name_en = 'dumbbell lying one arm rear lateral raise'
     AND name_es = 'Elevación lateral trasera a una mano acostado con mancuerna' AND movement_pattern = 'SHOULDER_RAISE'
     AND instructions_es LIKE 'Túmbate boca abajo en un banco plano con una mancuerna en una mano, colgando hacia el suelo.%'
     AND instructions_es LIKE '%hacia el lado%';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 620
       AND name_es = 'Extensión de hombro a una mano tumbado boca abajo con mancuerna' AND movement_pattern = 'SHOULDER_EXTENSION'
       AND instructions_es = 'Túmbate boca abajo en un banco plano con una mancuerna en una mano y el brazo colgando hacia el suelo. Con el brazo recto, llévalo hacia atrás, a lo largo del cuerpo, hasta que la mancuerna quede a la altura de la cadera. Haz una pausa breve y baja la mancuerna despacio a la posición inicial. Completa las repeticiones y cambia de brazo. Repite el número de repeticiones indicado.') THEN
    RAISE EXCEPTION 'V52.1: ejercicio 620 no esta en el estado anterior esperado';
  END IF;
END $v521$;

-- 868 hanging pike: inversion (contencion conservadora)
DO $v521$ DECLARE n INT; BEGIN
  UPDATE exercises SET requires_inversion = TRUE
   WHERE exercise_id = 868 AND name_en = 'hanging pike' AND requires_inversion = FALSE AND requires_hanging = TRUE;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 868 AND requires_inversion = TRUE) THEN
    RAISE EXCEPTION 'V52.1: ejercicio 868 no esta en el estado anterior esperado';
  END IF;
END $v521$;
