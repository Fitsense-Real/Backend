-- CONTRAFACTUAL P15-C / 1205 (medicion; NO aprobada): la sentadilla sissy (1205) deja de ser elegible para principiantes
-- (dificultad 1 -> 2). Motivo de la medicion: con P15-C es el sustituto de rodilla mas usado y su GIF muestra flexion de
-- rodilla muy profunda con los talones elevados y el tronco inclinado hacia atras (carga alta sobre la rodilla).
-- 1076 (sentadilla marcha contra la pared) se mantiene en dificultad 1: el GIF muestra una sentadilla isometrica en pared
-- con elevacion alterna de rodilla, sin impacto (pendiente de validacion profesional).
UPDATE exercises SET difficulty_level = 2 WHERE exercise_id = 1205 AND difficulty_level = 1;
