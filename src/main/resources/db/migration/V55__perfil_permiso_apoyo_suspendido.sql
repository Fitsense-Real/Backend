-- =====================================================================
-- FitSense - V55: PERMISO PROPIO «APOYO SUSPENDIDO» en el perfil (propuesta V53-B)
--
-- SOLO ESQUEMA. No reescribe ningun dato: las dos columnas nacen NULL en todos los perfiles existentes y en los nuevos.
-- El COMPORTAMIENTO esta APAGADO hasta la aprobacion del propietario: mientras
-- fitsense.seguridad.permiso-apoyo-suspendido sea false (valor por defecto), ninguna clase usa estas columnas para
-- decidir nada y la generacion, la adaptacion y los planes publicados se comportan exactamente igual que sin V55.
--
-- Opcion B de docs/FitSense/catalogo_integral/cierre/V53_DEFINITIVO.md §2: un permiso nuevo que excluye la situacion 2
-- (los 15 ejercicios con exercises.requires_suspended_support, V53) SIN cambiar el significado de «no colgarse»
-- (allow_hanging_exercises). Es excluyente con la opcion A (fitsense.seguridad.apoyo-suspendido-en-no-colgarse): con
-- los dos interruptores encendidos la aplicacion no arranca.
--
-- Semantica con el interruptor B encendido:
--   allow_suspended_support NULL  -> sin responder: HEREDA «no colgarse» (lo conservador). Quien declaro «no colgarse»
--                                    tampoco recibe el apoyo suspendido; quien no lo declaro, si.
--   allow_suspended_support TRUE  -> admite el apoyo suspendido aunque haya declarado «no colgarse». No levanta
--                                    ninguna otra restriccion (requires_hanging, INC-CAT-C39 siguen igual).
--   allow_suspended_support FALSE -> excluye el apoyo suspendido aunque pueda colgarse.
--   suspended_support_confirmed_at NULL -> pendiente de confirmacion explicita (la pregunta de la app, futura).
-- Sin backfill: la herencia se CALCULA al leer el perfil, no se copia a la columna. Nada se reinterpreta en silencio.
-- Detalle: docs/FitSense/catalogo_integral/cierre/v53b/PROPUESTA_V53B.md
-- =====================================================================

ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS allow_suspended_support BOOLEAN NULL;
ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS suspended_support_confirmed_at TIMESTAMPTZ NULL;

COMMENT ON COLUMN user_profiles.allow_suspended_support IS
    'Propuesta V53-B (apagada por defecto: fitsense.seguridad.permiso-apoyo-suspendido=false). false = excluir requires_suspended_support; true = admitirlo; NULL = sin responder, hereda allow_hanging_exercises. No cambia el significado de allow_hanging_exercises.';
COMMENT ON COLUMN user_profiles.suspended_support_confirmed_at IS
    'Cuando la persona confirmo explicitamente allow_suspended_support. NULL = pendiente de confirmar (sin backfill: los perfiles existentes quedan pendientes).';
