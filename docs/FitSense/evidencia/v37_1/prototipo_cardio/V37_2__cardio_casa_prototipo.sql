-- PROTOTIPO (solo base aislada): 1117 quick feet v. 2 se prescribe por tiempo y es cardio.
UPDATE exercises SET default_prescription = 'DURATION', body_part_id = (SELECT body_part_id FROM body_parts WHERE code = 'cardio')
 WHERE exercise_id = 1117 AND default_prescription = 'SETS_REPS';
