-- =====================================================================
-- FitSense - V53: APOYO SUSPENDIDO como atributo explicito del catalogo (cierre del catalogo)
--
-- DECISION DE SIGNIFICADO PENDIENTE DE APROBACION (ver docs/FitSense/catalogo_integral/cierre/VALIDACION_SEGURIDAD.md).
-- Hasta ahora «no colgarse» (user_profiles.allow_hanging_exercises = false) excluia solo requires_hanging: el cuerpo
-- cuelga del agarre. Con esta migracion excluye tambien el APOYO SUSPENDIDO: el cuerpo, sin los pies en el suelo, se
-- sostiene SOBRE brazos extendidos (fondos en paralelas o anillas), antebrazos (silla romana o del capitan) o con los
-- pies en correas o anillas. Carga el hombro con todo el peso corporal, como colgarse o mas, y la caida es posible.
-- El apoyo parcial en maquina (rodillas o pies en la plataforma de asistencia) NO se marca: no hay suspension.
--
-- Sustituye, con un dato comprobable, la contencion por lista de ids de INC-CAT-C39 para colgarse.
-- =====================================================================

ALTER TABLE exercises ADD COLUMN requires_suspended_support BOOLEAN NOT NULL DEFAULT FALSE;
COMMENT ON COLUMN exercises.requires_suspended_support IS
    'El cuerpo, sin los pies en el suelo, se sostiene sobre brazos extendidos, antebrazos o con los pies en correas. '
    'Lo excluye la restriccion «no colgarse» (decision de significado del cierre del catalogo, V53).';
