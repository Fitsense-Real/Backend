-- CONTRAFACTUAL P15-B: los 13 aislados KNEE_DOMINANT dejan de cubrir el patron (slot) de rodilla y de recibir dosis de
-- fuerza (ESTRUCTURAL -> RESISTIDO); conservan dosis de hipertrofia/resistencia de su grupo (upper legs).
-- ANKLE_PLANTAR_FLEXION es solo un MARCADOR de patron no estructural: el codigo no lo trata de forma especial y, al ser
-- de otro grupo corporal, no abre sustituciones cruzadas.
UPDATE exercises SET movement_pattern = 'ANKLE_PLANTAR_FLEXION'
 WHERE exercise_id IN (327, 637, 915, 916, 1007, 1008, 1009, 1010, 1046, 1195, 1196, 1197, 1266);
