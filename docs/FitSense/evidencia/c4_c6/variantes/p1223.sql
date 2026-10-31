-- PROTOTIPO C4/C6 (no oficial): p1223
DO $$ BEGIN
IF NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id=1223 AND movement_pattern='HORIZONTAL_PULL') THEN RAISE EXCEPTION 'previo 1223 movement_pattern'; END IF;
UPDATE exercises SET movement_pattern='SHOULDER_RAISE' WHERE exercise_id=1223;
END $$;
