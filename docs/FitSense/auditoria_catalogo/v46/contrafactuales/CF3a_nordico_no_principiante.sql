-- CONTRAFACTUAL E3-a (medicion; NO es una correccion aprobada): los 6 curls nordicos dejan de ser elegibles para
-- principiantes. Dificultad 1 -> 2: con el limite por nivel (BEGINNER 1, INTERMEDIATE 2, ADVANCED 3) solo cambia la
-- eleccion para principiantes. Los planes publicados no se tocan (la base es efimera; ninguna migracion).
UPDATE exercises SET difficulty_level = 2 WHERE exercise_id IN (1195, 1196, 1197, 327, 915, 916) AND difficulty_level = 1;
