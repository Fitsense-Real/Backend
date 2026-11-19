-- COMBINACION (medicion; NO aprobada): CF3a (curls nordicos fuera de principiantes) + CF6 (sissy fuera de principiantes).
UPDATE exercises SET difficulty_level = 2 WHERE exercise_id IN (1195, 1196, 1197, 327, 915, 916) AND difficulty_level = 1;
UPDATE exercises SET difficulty_level = 2 WHERE exercise_id = 1205 AND difficulty_level = 1;
