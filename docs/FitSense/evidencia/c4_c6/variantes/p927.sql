-- PROTOTIPO C4/C6 (no oficial): p927
DO $$ BEGIN
IF NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id=927 AND movement_pattern='VERTICAL_PUSH') THEN RAISE EXCEPTION 'previo 927 movement_pattern'; END IF;
UPDATE exercises SET movement_pattern='TRUNK_ROTATION_LATERAL_FLEXION' WHERE exercise_id=927;
END $$;
