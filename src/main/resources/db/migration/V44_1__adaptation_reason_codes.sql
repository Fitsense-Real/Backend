-- =====================================================================
-- V44_1 · Codigo estable del NO_FACTIBLE de la adaptacion (cierre definitivo
-- del backend, fase 2.1, 02/11/2026).
--
-- Solo AÑADE dos columnas NULL. No toca datos existentes ni ninguna regla:
-- las filas anteriores quedan con NULL y la API las sigue devolviendo como
-- siempre (sin codigo).
--
-- * plan_adaptation_attempts.reason_codes: los NoFactibleReason que puso la
--   regla que decidio el NO_FACTIBLE (el primero es el de fondo).
-- * safe_regeneration_attempts.reason_code: el del NO_FACTIBLE de P-03 nivel 2.
--
-- Version 44.1: entre V44 (generation) y la V45 candidata del PR #17, para que
-- el orden de Flyway sea el mismo en las dos ramas. Ninguna de las dos se ha
-- aplicado en Neon ni en Railway.
-- =====================================================================

ALTER TABLE public.plan_adaptation_attempts
    ADD COLUMN reason_codes VARCHAR(40)[];

ALTER TABLE public.plan_adaptation_attempts
    ADD CONSTRAINT ck_paa_reason_codes_solo_sin_plan
        CHECK (reason_codes IS NULL OR resulting_plan_id IS NULL);

ALTER TABLE public.safe_regeneration_attempts
    ADD COLUMN reason_code VARCHAR(40);

ALTER TABLE public.safe_regeneration_attempts
    ADD CONSTRAINT ck_sra_reason_code_solo_no_factible
        CHECK (reason_code IS NULL OR outcome = 'NO_FACTIBLE');

COMMENT ON COLUMN public.plan_adaptation_attempts.reason_codes IS
    'NoFactibleReason de la regla que decidio el NO_FACTIBLE (el primero es el de fondo). NULL en filas anteriores a V44_1 y en los estados que publican.';
COMMENT ON COLUMN public.safe_regeneration_attempts.reason_code IS
    'NoFactibleReason del NO_FACTIBLE de P-03 nivel 2. NULL si se publico o en filas anteriores a V44_1.';
