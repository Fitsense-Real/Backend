-- CONTRAFACTUAL: todos los ejercicios cuyo grupo recibe dosis sin un patron coherente con el (escaneo S6/S7/S8)
-- y que se usan en REV2: 775, 973, 1261, 1308, 1259, 1146.
UPDATE exercises SET movement_pattern = 'MOBILITY' WHERE exercise_id IN (775, 973, 1261, 1308, 1259, 1146);
