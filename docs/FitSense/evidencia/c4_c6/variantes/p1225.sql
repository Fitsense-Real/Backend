-- PROTOTIPO C4/C6 (no oficial): p1225
DO $$ BEGIN
IF NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id=1225 AND movement_pattern='HIP_EXTENSION') THEN RAISE EXCEPTION 'previo 1225 movement_pattern'; END IF;
UPDATE exercises SET movement_pattern='TRUNK_FLEXION' WHERE exercise_id=1225;
END $$;
