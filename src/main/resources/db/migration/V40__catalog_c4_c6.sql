-- =====================================================================
-- FitSense - V40: coherencia entre grupo, patron y slot (C4/C6), 30/10/2026
--
-- Decision del responsable del proyecto del 30/10/2026 sobre DIAGNOSTICO_C4_C6.md
-- (revision visual asistida por IA de los GIF; no es doble revision humana).
-- Criterio aprobado: el grupo principal y el slot que acredita un ejercicio deben ser
-- coherentes con su movimiento predominante. No es una reclasificacion masiva: solo
-- estos siete ejercicios. 674 y 162 se mantienen; 262, 301, 302 y el resto de cruces
-- (docs/FitSense/evidencia/c4_c6/cruces_grupo_slot_v39.tsv) quedan para otra fase.
--
--   1225 smith hip raise               HIP_EXTENSION -> TRUNK_FLEXION (crunch invertido; no acredita LOWER_HIP)
--   1223 smith incline shoulder raises HORIZONTAL_PULL -> SHOULDER_RAISE (protraccion; no acredita UPPER_PULL)
--    439 cable squat row               back/lats -> upper legs/quads (KNEE_DOMINANT, precedente C4)
--    959 kettlebell figure 8           HIP_HINGE -> TRUNK_ROTATION_LATERAL_FLEXION
--    962 kettlebell sumo high pull     back/traps -> upper legs/glutes (KNEE_DOMINANT; HIP_HINGE pendiente)
--    927 kettlebell bent press         VERTICAL_PUSH -> TRUNK_ROTATION_LATERAL_FLEXION (el brazo llega
--                                      arriba porque el tronco se inclina por debajo; ver el diagnostico)
--    953 kettlebell two arm clean      shoulders/delts -> upper legs/glutes (HIP_HINGE)
--
-- No toca dificultad, equipamiento, dosis, exercise_difficulty_approvals ni V38/V39.
-- Comprueba el estado anterior de cada columna y aborta sin cambios si difiere; comprueba
-- el estado final antes de terminar.
-- =====================================================================

CREATE TEMP TABLE v40_cambios (exercise_id BIGINT, source_code TEXT, name_en TEXT, columna TEXT,
                               antes TEXT, despues TEXT);
INSERT INTO v40_cambios VALUES
    (1225, '0756', 'smith hip raise', 'movement_pattern', 'HIP_EXTENSION', 'TRUNK_FLEXION'),
    (1223, '0759', 'smith incline shoulder raises', 'movement_pattern', 'HORIZONTAL_PULL', 'SHOULDER_RAISE'),
    (439, '1717', 'cable squat row (with rope attachment)', 'body_part', 'back', 'upper legs'),
    (439, '1717', 'cable squat row (with rope attachment)', 'target_muscle', 'lats', 'quads'),
    (959, '0532', 'kettlebell figure 8', 'movement_pattern', 'HIP_HINGE', 'TRUNK_ROTATION_LATERAL_FLEXION'),
    (962, '0548', 'kettlebell sumo high pull', 'body_part', 'back', 'upper legs'),
    (962, '0548', 'kettlebell sumo high pull', 'target_muscle', 'traps', 'glutes'),
    (927, '0524', 'kettlebell bent press', 'movement_pattern', 'VERTICAL_PUSH', 'TRUNK_ROTATION_LATERAL_FLEXION'),
    (953, '0552', 'kettlebell two arm clean', 'body_part', 'shoulders', 'upper legs'),
    (953, '0552', 'kettlebell two arm clean', 'target_muscle', 'delts', 'glutes');

CREATE TEMP VIEW v40_valor AS
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
      FROM v40_cambios c
      LEFT JOIN pg_temp.v40_valor v ON v.exercise_id = c.exercise_id AND v.columna = c.columna
     WHERE v.valor IS DISTINCT FROM c.antes
        OR NOT EXISTS (SELECT 1 FROM public.exercises e WHERE e.exercise_id = c.exercise_id
                          AND e.source_code = c.source_code AND e.name_en = c.name_en AND e.is_active);
    IF distintos IS NOT NULL THEN
        RAISE EXCEPTION 'V40: estado anterior inesperado -> %', distintos;
    END IF;
END $$;

UPDATE public.exercises e SET movement_pattern = c.despues FROM v40_cambios c
 WHERE c.exercise_id = e.exercise_id AND c.columna = 'movement_pattern';
UPDATE public.exercises e SET target_muscle = c.despues FROM v40_cambios c
 WHERE c.exercise_id = e.exercise_id AND c.columna = 'target_muscle';
UPDATE public.exercises e SET body_part_id = bp.body_part_id FROM v40_cambios c
  JOIN public.body_parts bp ON bp.code = c.despues
 WHERE c.exercise_id = e.exercise_id AND c.columna = 'body_part';

DO $$
DECLARE distintos TEXT;
BEGIN
    SELECT string_agg(c.exercise_id || ' ' || c.columna || ' = ' || coalesce(v.valor, 'NULL'), '; '
                      ORDER BY c.exercise_id, c.columna)
      INTO distintos
      FROM v40_cambios c
      LEFT JOIN pg_temp.v40_valor v ON v.exercise_id = c.exercise_id AND v.columna = c.columna
     WHERE v.valor IS DISTINCT FROM c.despues;
    IF distintos IS NOT NULL THEN
        RAISE EXCEPTION 'V40: estado final inesperado -> %', distintos;
    END IF;
END $$;

-- Flyway ejecuta las migraciones en la misma sesion: los objetos temporales se borran aqui.
DROP VIEW pg_temp.v40_valor;
DROP TABLE pg_temp.v40_cambios;
