-- =====================================================================
-- FitSense - V41: coherencia grupo/patron/slot, segunda fase de C4/C6 (30/10/2026)
--
-- Orden de trabajo integral del 30/10/2026 (autoriza correcciones de catalogo sustentadas
-- con el criterio aprobado en V40: el grupo principal y el slot que acredita un ejercicio
-- deben ser coherentes con su movimiento predominante). Revision visual asistida por IA de
-- los GIF; no es doble revision humana. Evidencia: docs/FitSense/evidencia/v41/.
--
--   262 barbell skier             HIP_HINGE -> SHOULDER_EXTENSION (tronco quieto, la barra la
--                                 mueve el hombro; acreditaba LOWER_HIP en 11 referencias)
--   267, 952 thrusters            shoulders/delts -> upper legs/quads (sentadilla frontal
--                                 cargada que mueve el cuerpo; precedente C4)
--   796 step up + curl            upper arms/biceps -> upper legs/quads (precedente C4/V36)
--   301, 302 remos en sentadilla  KNEE_DOMINANT -> HORIZONTAL_PULL (la sentadilla se mantiene
--                                 quieta y lo que se mueve es el remo; evaluados aparte de 439)
--   671 pullover en balon         HIP_EXTENSION -> SHOULDER_EXTENSION (el puente es isometrico)
--   824 elevacion prona en balon  back/spine -> upper legs/glutes (extension de cadera)
--   922, 930, 971 hang cleans     HIP_HINGE -> ELBOW_FLEXION (en el GIF no hay bisagra: el
--                                 brazo lleva la kettlebell al rack)
--   674, 175, 942, 947 arrancadas familia: upper legs/glutes con HIP_HINGE, como las cargadas
--                                 (el impulso es de cadera; la recepcion arriba no es un press)
--
-- Se mantienen: 162 (cargada + press estricto, dos fases con carga; excepcion documentada),
-- 862, 1086 y los cardios 54, 1174 y 1295 (pendientes de decision, ver INFORME).
-- No toca dificultad, equipamiento, dosis ni exercise_difficulty_approvals.
-- Comprueba el estado anterior de cada columna y aborta sin cambios si difiere.
-- =====================================================================

CREATE TEMP TABLE v41_cambios (exercise_id BIGINT, source_code TEXT, name_en TEXT, columna TEXT,
                               antes TEXT, despues TEXT);
INSERT INTO v41_cambios VALUES
    (262, '0100', 'barbell skier', 'movement_pattern', 'HIP_HINGE', 'SHOULDER_EXTENSION'),
    (267, '3305', 'barbell thruster', 'body_part', 'shoulders', 'upper legs'),
    (267, '3305', 'barbell thruster', 'target_muscle', 'delts', 'quads'),
    (952, '0550', 'kettlebell thruster', 'body_part', 'shoulders', 'upper legs'),
    (952, '0550', 'kettlebell thruster', 'target_muscle', 'delts', 'quads'),
    (796, '1684', 'dumbbell step up single leg balance with bicep curl', 'body_part', 'upper arms', 'upper legs'),
    (796, '1684', 'dumbbell step up single leg balance with bicep curl', 'target_muscle', 'biceps', 'quads'),
    (301, '3168', 'bodyweight squatting row', 'movement_pattern', 'KNEE_DOMINANT', 'HORIZONTAL_PULL'),
    (302, '3167', 'bodyweight squatting row (with towel)', 'movement_pattern', 'KNEE_DOMINANT', 'HORIZONTAL_PULL'),
    (671, '1294', 'dumbbell pullover hip extension on exercise ball', 'movement_pattern', 'HIP_EXTENSION', 'SHOULDER_EXTENSION'),
    (824, '1343', 'exercise ball prone leg raise', 'body_part', 'back', 'upper legs'),
    (824, '1343', 'exercise ball prone leg raise', 'target_muscle', 'spine', 'glutes'),
    (922, '0518', 'kettlebell alternating hang clean', 'movement_pattern', 'HIP_HINGE', 'ELBOW_FLEXION'),
    (930, '0525', 'kettlebell bottoms up clean from the hang position', 'movement_pattern', 'HIP_HINGE', 'ELBOW_FLEXION'),
    (971, '0526', 'kettlebell double alternating hang clean', 'movement_pattern', 'HIP_HINGE', 'ELBOW_FLEXION'),
    (674, '3888', 'dumbbell one arm snatch', 'movement_pattern', 'VERTICAL_PUSH', 'HIP_HINGE'),
    (175, '0067', 'barbell one arm snatch', 'body_part', 'shoulders', 'upper legs'),
    (175, '0067', 'barbell one arm snatch', 'target_muscle', 'delts', 'glutes'),
    (175, '0067', 'barbell one arm snatch', 'movement_pattern', 'VERTICAL_PUSH', 'HIP_HINGE'),
    (942, '0529', 'kettlebell double snatch', 'body_part', 'shoulders', 'upper legs'),
    (942, '0529', 'kettlebell double snatch', 'target_muscle', 'delts', 'glutes'),
    (942, '0529', 'kettlebell double snatch', 'movement_pattern', 'VERTICAL_PUSH', 'HIP_HINGE'),
    (947, '0542', 'kettlebell one arm snatch', 'body_part', 'shoulders', 'upper legs'),
    (947, '0542', 'kettlebell one arm snatch', 'target_muscle', 'delts', 'glutes'),
    (947, '0542', 'kettlebell one arm snatch', 'movement_pattern', 'VERTICAL_PUSH', 'HIP_HINGE');

CREATE TEMP VIEW v41_valor AS
SELECT e.exercise_id, x.columna, x.valor
  FROM public.exercises e
  JOIN public.body_parts bp ON bp.body_part_id = e.body_part_id
 CROSS JOIN LATERAL (VALUES ('movement_pattern', e.movement_pattern),
                            ('target_muscle', e.target_muscle),
                            ('body_part', bp.code)) AS x(columna, valor);

DO $$
DECLARE distintos TEXT;
BEGIN
    SELECT string_agg(c.exercise_id || ' ' || c.columna || ' = ' || coalesce(v.valor, 'NULL') || ' (se esperaba '
                      || c.antes || ')', '; ' ORDER BY c.exercise_id, c.columna)
      INTO distintos
      FROM v41_cambios c
      LEFT JOIN pg_temp.v41_valor v ON v.exercise_id = c.exercise_id AND v.columna = c.columna
     WHERE v.valor IS DISTINCT FROM c.antes
        OR NOT EXISTS (SELECT 1 FROM public.exercises e WHERE e.exercise_id = c.exercise_id
                          AND e.source_code = c.source_code AND e.name_en = c.name_en AND e.is_active);
    IF distintos IS NOT NULL THEN
        RAISE EXCEPTION 'V41: estado anterior inesperado -> %', distintos;
    END IF;
END $$;

UPDATE public.exercises e SET movement_pattern = c.despues FROM v41_cambios c
 WHERE c.exercise_id = e.exercise_id AND c.columna = 'movement_pattern';
UPDATE public.exercises e SET target_muscle = c.despues FROM v41_cambios c
 WHERE c.exercise_id = e.exercise_id AND c.columna = 'target_muscle';
UPDATE public.exercises e SET body_part_id = bp.body_part_id FROM v41_cambios c
  JOIN public.body_parts bp ON bp.code = c.despues
 WHERE c.exercise_id = e.exercise_id AND c.columna = 'body_part';

DO $$
DECLARE distintos TEXT;
BEGIN
    SELECT string_agg(c.exercise_id || ' ' || c.columna || ' = ' || coalesce(v.valor, 'NULL'), '; '
                      ORDER BY c.exercise_id, c.columna)
      INTO distintos
      FROM v41_cambios c
      LEFT JOIN pg_temp.v41_valor v ON v.exercise_id = c.exercise_id AND v.columna = c.columna
     WHERE v.valor IS DISTINCT FROM c.despues;
    IF distintos IS NOT NULL THEN
        RAISE EXCEPTION 'V41: estado final inesperado -> %', distintos;
    END IF;
END $$;

-- Flyway ejecuta las migraciones en la misma sesion: los objetos temporales se borran aqui.
DROP VIEW pg_temp.v41_valor;
DROP TABLE pg_temp.v41_cambios;
