-- V46 (rama aislada investigacion/catalogo-v46; NO desplegada) - Auditoria del catalogo v46, correcciones OBJETIVAS.
-- SOLO cambia name_es (lo que ve la persona y el texto de los prompts). No cambia patron, grupo, musculos, dificultad,
-- equipamiento, prescripcion, instrucciones ni actividad: la seleccion de ejercicios no cambia (barridos de regresion).
-- Cada fila exige el valor anterior exacto: si el catalogo no es el esperado, no se toca nada.
--
-- 973 «left hook. boxing»: «Giro izquierdo. boxeo» es una traduccion erronea de «hook» (gancho). Evidencia: nombre
--     original, instrucciones («punching motion») y GIF (gancho de boxeo) coinciden.
UPDATE exercises SET name_es = 'Gancho de izquierda (boxeo)'
 WHERE exercise_id = 973 AND name_en = 'left hook. boxing' AND name_es = 'Giro izquierdo. boxeo';
-- 1263 «split squats»: «Sentadillas búlgaras» exige el pie trasero ELEVADO; el nombre original, las instrucciones (el pie
--     trasero en el suelo, la rodilla «hovering just above the ground») y el GIF describen una sentadilla dividida.
UPDATE exercises SET name_es = 'Sentadilla dividida (zancada estática)'
 WHERE exercise_id = 1263 AND name_en = 'split squats' AND name_es = 'Sentadillas búlgaras';
