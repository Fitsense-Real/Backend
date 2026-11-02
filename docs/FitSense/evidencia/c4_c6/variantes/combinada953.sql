-- PROTOTIPO C4/C6 (no oficial): combinada
DO $$ BEGIN
IF NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id=1225 AND movement_pattern='HIP_EXTENSION') THEN RAISE EXCEPTION 'previo 1225 movement_pattern'; END IF;
UPDATE exercises SET movement_pattern='TRUNK_FLEXION' WHERE exercise_id=1225;
IF NOT EXISTS (SELECT 1 FROM exercises e JOIN body_parts b ON b.body_part_id=e.body_part_id WHERE e.exercise_id=439 AND b.code='back') THEN RAISE EXCEPTION 'previo 439 body_part'; END IF;
UPDATE exercises SET body_part_id=(SELECT body_part_id FROM body_parts WHERE code='upper legs') WHERE exercise_id=439;
IF NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id=439 AND target_muscle='lats') THEN RAISE EXCEPTION 'previo 439 target_muscle'; END IF;
UPDATE exercises SET target_muscle='quads' WHERE exercise_id=439;
IF NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id=959 AND movement_pattern='HIP_HINGE') THEN RAISE EXCEPTION 'previo 959 movement_pattern'; END IF;
UPDATE exercises SET movement_pattern='TRUNK_ROTATION_LATERAL_FLEXION' WHERE exercise_id=959;
IF NOT EXISTS (SELECT 1 FROM exercises e JOIN body_parts b ON b.body_part_id=e.body_part_id WHERE e.exercise_id=962 AND b.code='back') THEN RAISE EXCEPTION 'previo 962 body_part'; END IF;
UPDATE exercises SET body_part_id=(SELECT body_part_id FROM body_parts WHERE code='upper legs') WHERE exercise_id=962;
IF NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id=962 AND target_muscle='traps') THEN RAISE EXCEPTION 'previo 962 target_muscle'; END IF;
UPDATE exercises SET target_muscle='glutes' WHERE exercise_id=962;
IF NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id=927 AND movement_pattern='VERTICAL_PUSH') THEN RAISE EXCEPTION 'previo 927 movement_pattern'; END IF;
UPDATE exercises SET movement_pattern='TRUNK_ROTATION_LATERAL_FLEXION' WHERE exercise_id=927;
IF NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id=1223 AND movement_pattern='HORIZONTAL_PULL') THEN RAISE EXCEPTION 'previo 1223 movement_pattern'; END IF;
UPDATE exercises SET movement_pattern='SHOULDER_RAISE' WHERE exercise_id=1223;

IF NOT EXISTS (SELECT 1 FROM exercises e JOIN body_parts b ON b.body_part_id=e.body_part_id WHERE e.exercise_id=953 AND b.code='shoulders') THEN RAISE EXCEPTION 'previo 953 body_part'; END IF;
UPDATE exercises SET body_part_id=(SELECT body_part_id FROM body_parts WHERE code='upper legs') WHERE exercise_id=953;
IF NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id=953 AND target_muscle='delts') THEN RAISE EXCEPTION 'previo 953 target'; END IF;
UPDATE exercises SET target_muscle='glutes' WHERE exercise_id=953;
END $$;
