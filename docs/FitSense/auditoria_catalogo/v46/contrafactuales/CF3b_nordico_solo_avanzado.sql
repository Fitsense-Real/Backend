-- CONTRAFACTUAL E3-b (medicion; NO aprobada): dificultad 1 -> 3 para los 6 curls nordicos (solo ADVANCED). Cota superior
-- del impacto; la orden pide no asignar 3 sin justificacion deportiva.
UPDATE exercises SET difficulty_level = 3 WHERE exercise_id IN (1195, 1196, 1197, 327, 915, 916) AND difficulty_level = 1;
