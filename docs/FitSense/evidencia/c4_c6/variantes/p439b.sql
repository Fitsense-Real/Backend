-- PROTOTIPO C4/C6 (no oficial): p439b
DO $$ BEGIN
IF NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id=439 AND movement_pattern='KNEE_DOMINANT') THEN RAISE EXCEPTION 'previo 439 movement_pattern'; END IF;
UPDATE exercises SET movement_pattern='HORIZONTAL_PULL' WHERE exercise_id=439;
END $$;
