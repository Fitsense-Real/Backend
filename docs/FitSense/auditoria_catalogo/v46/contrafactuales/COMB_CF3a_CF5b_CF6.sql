-- COMBINACION (medicion; NO aprobada): CF3a + CF5b (anclaje declarado para 1195/1196) + CF6.
UPDATE exercises SET difficulty_level = 2 WHERE exercise_id IN (1195, 1196, 1197, 327, 915, 916) AND difficulty_level = 1;
UPDATE exercises SET difficulty_level = 2 WHERE exercise_id = 1205 AND difficulty_level = 1;
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
