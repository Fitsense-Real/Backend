-- CONTRAFACTUAL E5/E6-b (medicion; NO aprobada): los curls nordicos declaran el anclaje que muestra el GIF.
--   1195 (tobillos en un marco): 'anchor point' O 'wall bars'.
--   1196 (tobillos bajo un banco): 'bench' O 'anchor point'.
--   1197 ya exige 'step platform'; 915 ya exige 'bench'; 327 y 916 ya exigen 'cable' (con almohadilla de maquina).
-- Ningun perfil del barrido declara 'anchor point' ni 'wall bars': en casa con peso corporal dejan de ser elegibles.
-- Como en V39 (p. ej. 1072: '1:body weight,2:anchor point'): el trigger diferido de V23 exige que el implemento
-- principal este entre los requisitos. Si el ejercicio de peso corporal aun no tiene filas, primero el grupo 1 =
-- 'body weight' (todos los perfiles lo tienen) y despues el requisito nuevo como grupo aparte (AND).
INSERT INTO exercise_equipment_requirements (exercise_id, group_no, equipment_id)
SELECT e.exercise_id, 1, e.equipment_id FROM exercises e JOIN equipment_types q ON q.equipment_id = e.equipment_id
 WHERE q.code = 'body weight' AND e.exercise_id IN (1195, 1196)
   AND NOT EXISTS (SELECT 1 FROM exercise_equipment_requirements r WHERE r.exercise_id = e.exercise_id);
INSERT INTO exercise_equipment_requirements (exercise_id, group_no, equipment_id)
SELECT x.exercise_id, COALESCE((SELECT max(group_no) FROM exercise_equipment_requirements r
                                 WHERE r.exercise_id = x.exercise_id), 0) + 1, q.equipment_id
  FROM (VALUES (1195, 'anchor point'), (1195, 'wall bars'), (1196, 'bench'), (1196, 'anchor point')) AS x(exercise_id, code)
  JOIN equipment_types q ON q.code = x.code;
