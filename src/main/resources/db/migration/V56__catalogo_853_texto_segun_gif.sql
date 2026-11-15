-- =====================================================================
-- FitSense - V56: 853 «half knee bends (male)», nombre e instrucciones segun el GIF (cierre tecnico conservador,
-- orden de 15/11/2026, fase 2).
--
-- El 853 estaba excluido temporalmente (INC-CAT-CONS-v1, identidad no determinada): el nombre y las instrucciones
-- vigentes (V49: «Marcha rapida en el sitio», «marcha en el sitio a ritmo vivo») no describian el GIF. Evidencia
-- revisada sobre la animacion COMPLETA (24 fotogramas, docs/FitSense/catalogo_integral/cierre_conservador/REVISION_853.md):
--   f0 y f12  de pie, una rodilla elevada hasta la cadera (alterna), el pie de apoyo en el suelo sobre el antepie;
--   f6 y f18  semisentadilla con los DOS pies apoyados;
--   ningun fotograma sin apoyo. La fase de doble apoyo con flexion de rodillas en cada ciclo descarta un trote.
-- Coincide con R2 (movimiento observado), con V3 (f1/f13 rodilla elevada, f6-f8/f18-f20 ambos pies en el suelo con
-- semiflexion) y con el nombre de la fuente («half knee bends»; el nombre en espanol anterior a V49 era «Media
-- sentadilla»). Se corrigen SOLO el nombre y las instrucciones para que describan ese movimiento y pidan
-- expresamente no saltar.
--
-- NO cambia: zona (cardio), patron (LOCOMOTION_CARDIO), prescripcion (DURATION), dificultad (1), equipamiento (peso
-- corporal), banderas (ninguna), impact_level (BAJO, ya explicito desde V51) ni is_active. No relaja ninguna
-- restriccion. Exige el estado anterior exacto: si no coincide y no esta ya corregido, la migracion FALLA.
-- =====================================================================

DO $v56$ DECLARE n INT; BEGIN
  UPDATE exercises SET
         name_es = 'Media sentadilla con elevación de rodilla alterna',
         instructions_es = 'Ponte de pie con los pies separados al ancho de las caderas. Eleva una rodilla hasta la altura de la cadera, con el pie de apoyo firme en el suelo, y acompaña con los brazos. Baja el pie y, con los dos pies apoyados, flexiona ligeramente las rodillas en una media sentadilla con el tronco erguido. Repite elevando la otra rodilla. Mantén siempre al menos un pie en el suelo: no saltes. Mantén un ritmo constante durante la duración indicada.',
         instruction_steps_es = '["Ponte de pie con los pies separados al ancho de las caderas.", "Eleva una rodilla hasta la altura de la cadera, con el pie de apoyo firme en el suelo, y acompaña con los brazos.", "Baja el pie y, con los dos pies apoyados, flexiona ligeramente las rodillas en una media sentadilla con el tronco erguido.", "Repite elevando la otra rodilla.", "Mantén siempre al menos un pie en el suelo: no saltes.", "Mantén un ritmo constante durante la duración indicada."]'::jsonb
   WHERE exercise_id = 853 AND name_en = 'half knee bends (male)' AND name_es = 'Marcha rápida en el sitio'
     AND instructions_es LIKE 'Ponte de pie con los pies separados al ancho de las caderas. Marcha en el sitio a ritmo vivo%';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE exercise_id = 853
       AND name_es = 'Media sentadilla con elevación de rodilla alterna'
       AND instructions_es LIKE '%no saltes.%') THEN
    RAISE EXCEPTION 'V56: ejercicio 853 no esta en el estado anterior esperado';
  END IF;
  -- Los atributos de seguridad que la revision confirma no cambian; si no son estos, la migracion FALLA.
  IF NOT EXISTS (SELECT 1 FROM exercises e JOIN equipment_types t ON t.equipment_id = e.equipment_id
       WHERE e.exercise_id = 853 AND e.is_active AND e.movement_pattern = 'LOCOMOTION_CARDIO'
         AND e.default_prescription = 'DURATION' AND e.difficulty_level = 1 AND t.code = 'body weight'
         AND NOT e.high_impact AND NOT e.requires_floor AND NOT e.requires_kneeling AND NOT e.requires_hanging
         AND NOT e.requires_overhead AND NOT e.requires_inversion AND NOT e.axial_load
         AND NOT e.requires_suspended_support AND e.impact_level = 'BAJO') THEN
    RAISE EXCEPTION 'V56: los atributos de seguridad del 853 no son los revisados';
  END IF;
END $v56$;
