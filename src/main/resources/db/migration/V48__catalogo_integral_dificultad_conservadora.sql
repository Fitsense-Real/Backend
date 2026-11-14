-- V48 - Catalogo integral (auditoria visual 2026-11): CONTENCION CONSERVADORA de dificultad.
-- SOLO sube difficulty_level de habilidades claramente avanzadas que estaban en d1 (principiante). Nunca la baja:
-- las bajadas propuestas (curls en d3, etc.) quedan para revision profesional (V27: la dificultad la aprueban personas).
-- difficulty_level no es una aprobacion humana (V27); aqui actua como limite de elegibilidad por nivel. Esta subida es
-- una medida preventiva PENDIENTE DE CONFIRMACION PROFESIONAL: un profesional puede fijar otro nivel con una migracion
-- posterior. Evidencia: GIF original revisado por el revisor del lote y verificado. Detalle: docs/FitSense/catalogo_integral/.
-- Cada fila exige el estado anterior exacto (nombre de la fuente y difficulty_level = 1).

-- 464 chin-ups (narrow parallel grip): dominada completa con agarre paralelo estrecho; la dominada estandar 1109 ya es d3. d1 -> d3.
UPDATE exercises SET difficulty_level = 3
 WHERE exercise_id = 464 AND name_en = 'chin-ups (narrow parallel grip)' AND difficulty_level = 1;
-- 1108 pull up (neutral grip): dominada completa con agarre neutro; la dominada estandar 1109 ya es d3. d1 -> d3.
UPDATE exercises SET difficulty_level = 3
 WHERE exercise_id = 1108 AND name_en = 'pull up (neutral grip)' AND difficulty_level = 1;
-- 1191 side-to-side chin: dominada con desplazamiento lateral del cuerpo en la parte alta. d1 -> d3.
UPDATE exercises SET difficulty_level = 3
 WHERE exercise_id = 1191 AND name_en = 'side-to-side chin' AND difficulty_level = 1;
-- 862 gorilla chin: dominada gorila: dominada con elevacion de rodillas. d1 -> d3.
UPDATE exercises SET difficulty_level = 3
 WHERE exercise_id = 862 AND name_en = 'gorilla chin' AND difficulty_level = 1;
-- 874 gironda sternum chin: dominada Gironda llevando el esternon a la barra. d1 -> d3.
UPDATE exercises SET difficulty_level = 3
 WHERE exercise_id = 874 AND name_en = 'gironda sternum chin' AND difficulty_level = 1;
-- 868 hanging pike: pies a la barra con piernas rectas colgado. d1 -> d3.
UPDATE exercises SET difficulty_level = 3
 WHERE exercise_id = 868 AND name_en = 'hanging pike' AND difficulty_level = 1;
-- 880 glute-ham raise: elevacion gluteo-isquiotibial completa en GHD. d1 -> d3.
UPDATE exercises SET difficulty_level = 3
 WHERE exercise_id = 880 AND name_en = 'glute-ham raise' AND difficulty_level = 1;
-- 915 inverse leg curl (bench support): curl nordico (descenso excentrico del tronco con tobillos fijados). d1 -> d3.
UPDATE exercises SET difficulty_level = 3
 WHERE exercise_id = 915 AND name_en = 'inverse leg curl (bench support)' AND difficulty_level = 1;
-- 916 inverse leg curl (on pull-up cable machine): curl nordico anclado en maquina de poleas. d1 -> d3.
UPDATE exercises SET difficulty_level = 3
 WHERE exercise_id = 916 AND name_en = 'inverse leg curl (on pull-up cable machine)' AND difficulty_level = 1;
-- 1192 skin the cat: skin the cat en anillas (ademas retenido para todos por INC-4C-03). d1 -> d3.
UPDATE exercises SET difficulty_level = 3
 WHERE exercise_id = 1192 AND name_en = 'skin the cat' AND difficulty_level = 1;
-- 821 exercise ball pike push up: flexion en pica con los pies sobre un balon de estabilidad. d1 -> d2.
UPDATE exercises SET difficulty_level = 2
 WHERE exercise_id = 821 AND name_en = 'exercise ball pike push up' AND difficulty_level = 1;
-- 1201 side plank hip adduction: plancha lateral de Copenhague con el pie de arriba sobre un banco. d1 -> d2.
UPDATE exercises SET difficulty_level = 2
 WHERE exercise_id = 1201 AND name_en = 'side plank hip adduction' AND difficulty_level = 1;
