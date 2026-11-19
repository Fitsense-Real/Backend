-- =====================================================================
-- FitSense - V54: constancia del bloqueo de seguridad al vencer una sesion (PROPUESTA, orden de cierre de
-- pendientes de 14/11/2026, fase 9).
--
-- Problema: una sesion publicada que hoy esta bloqueada por seguridad (PublishedPlanSafetyService: un ejercicio
-- choca con la seguridad del participante, p. ej. INC-CAT-C39 en el 1303 del plan 78) no se puede iniciar. Si nadie
-- la repara, la tarea de vencimiento la cierra como SKIPPED con skip_reason NULL, y la causa (el sistema la bloqueo)
-- se pierde: el bloqueo se calcula al leer y no se guarda.
--
-- Esta columna SOLO deja constancia de la causa en el momento del vencimiento. No cambia el estado, no rellena
-- skip_reason (la omision sigue sin declarar) y no entra en ningun calculo: adherencia, cobertura, rachas, riesgo y
-- adaptacion leen exactamente lo mismo que antes. Excluir estas sesiones del denominador, o informar de ellas a la
-- adaptacion, es un cambio metodologico que necesita la aprobacion del propietario (docs/FitSense/catalogo_integral/
-- cierre/pendientes/SESIONES_BLOQUEADAS.md).
-- =====================================================================
ALTER TABLE planned_workouts ADD COLUMN system_block_cause VARCHAR(200);
COMMENT ON COLUMN planned_workouts.system_block_cause IS
  'Causa del bloqueo de seguridad vigente cuando la tarea de vencimiento cerro la sesion (ejercicio=causa; ...). NULL si no estaba bloqueada.';
