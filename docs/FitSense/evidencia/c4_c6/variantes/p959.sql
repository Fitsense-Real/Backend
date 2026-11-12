-- PROTOTIPO C4/C6 (no oficial): p959
DO $$ BEGIN
IF NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id=959 AND movement_pattern='HIP_HINGE') THEN RAISE EXCEPTION 'previo 959 movement_pattern'; END IF;
UPDATE exercises SET movement_pattern='TRUNK_ROTATION_LATERAL_FLEXION' WHERE exercise_id=959;
END $$;
