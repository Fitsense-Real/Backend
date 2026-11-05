-- CONTRAFACTUAL E5/E6-a (medicion; NO aprobada): los 75 ejercicios usados cuyas instrucciones exigen sentarse o apoyarse
-- en un banco o silla (escaneo S1; sin 1320, cuyo GIF es en el suelo) pasan a exigir un grupo nuevo «banco O silla».
-- 'chair' es un tipo nuevo que ningun perfil del barrido declara: es el PEOR caso (nadie tiene silla). El gimnasio sigue
-- teniendo banco. Sin filas = peso corporal: el resto de ejercicios no cambia.
INSERT INTO equipment_types (code, name_es, requires_gym, display_order) VALUES ('chair', 'Silla estable', FALSE, 90)
ON CONFLICT (code) DO NOTHING;
WITH ids(exercise_id) AS (VALUES (115),(119),(130),(215),(216),(219),(220),(221),(227),(229),(239),(250),(251),(252),
  (274),(315),(327),(330),(331),(349),(371),(416),(421),(435),(447),(448),(512),(513),(524),(562),(660),(679),(695),(702),
  (703),(704),(705),(706),(707),(708),(709),(721),(722),(723),(725),(726),(727),(728),(729),(732),(734),(735),(736),(739),
  (740),(757),(758),(759),(783),(803),(835),(846),(847),(898),(916),(939),(949),(950),(1103),(1136),(1154),(1155),(1159),
  (1160),(1188)),
 g AS (SELECT i.exercise_id, COALESCE((SELECT max(group_no) FROM exercise_equipment_requirements r
                                         WHERE r.exercise_id = i.exercise_id), 0) + 1 AS grupo FROM ids i)
INSERT INTO exercise_equipment_requirements (exercise_id, group_no, equipment_id)
SELECT g.exercise_id, g.grupo, q.equipment_id FROM g JOIN equipment_types q ON q.code IN ('bench', 'chair');
