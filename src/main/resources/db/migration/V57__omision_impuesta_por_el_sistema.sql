-- =====================================================================
-- FitSense - V57: constancia de la omision impuesta por el sistema (PROPUESTA, orden premerge final, fase 4).
--
-- Un ejercicio marcado por seguridad en una sesion publicada (PublishedPlanSafetyService: dolor registrado,
-- restriccion del perfil, contencion) SOLO puede registrarse como omitido. El motivo que guarda la fila
-- (skip_reason) lo elige la persona y no distingue esa omision impuesta de una voluntaria. Hasta ahora la causa se
-- podia reconstruir con las exclusiones por dolor, pero no la de las demas causas ni la configuracion de cada momento.
--
-- Esta columna SOLO deja constancia, en el momento del registro, de la causa de seguridad vigente sobre ese
-- ejercicio (la misma cadena que devuelve la API, p. ej. 'dolor_registrado' o 'requires_floor+dolor_registrado').
-- NULL si el ejercicio no estaba marcado. No cambia el estado, no cambia skip_reason, no entra en ningun calculo:
-- adherencia, cobertura, riesgo y adaptacion leen exactamente lo mismo que antes. Las filas existentes quedan NULL
-- (no se reclasifica nada sin evidencia).
-- =====================================================================
ALTER TABLE workout_session_exercises ADD COLUMN system_block_cause VARCHAR(200);
COMMENT ON COLUMN workout_session_exercises.system_block_cause IS
  'Causa de seguridad vigente cuando se registro el ejercicio (solo podia omitirse). NULL si no estaba marcado o si la fila es anterior a V57.';
