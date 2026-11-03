-- PROTOTIPO C4/C6 (no oficial): p962
DO $$ BEGIN
IF NOT EXISTS (SELECT 1 FROM exercises e JOIN body_parts b ON b.body_part_id=e.body_part_id WHERE e.exercise_id=962 AND b.code='back') THEN RAISE EXCEPTION 'previo 962 body_part'; END IF;
UPDATE exercises SET body_part_id=(SELECT body_part_id FROM body_parts WHERE code='upper legs') WHERE exercise_id=962;
IF NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id=962 AND target_muscle='traps') THEN RAISE EXCEPTION 'previo 962 target_muscle'; END IF;
UPDATE exercises SET target_muscle='glutes' WHERE exercise_id=962;
END $$;
