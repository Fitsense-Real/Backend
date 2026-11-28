-- =====================================================================
-- FitSense - V65: identificacion POSTERIOR del ejercicio que dolio (O20,
-- ajuste final del 24/11/2026).
--
-- Un dolor de sesion sin decir cual (DOLOR_SESION_SIN_IDENTIFICAR) excluye
-- todos los ejercicios de esa sesion, como hasta ahora. Es un reporte
-- GENERAL: no confirma que ejercicio fue. La persona puede decirlo despues;
-- esa fila queda entonces CONFIRMADA (identified_at), con la constancia de
-- la accion explicita. El motivo original no se reescribe: el historial
-- sigue mostrando que llego como dolor de sesion.
--
-- Solo anade dos columnas que admiten NULL. No modifica datos existentes.
-- =====================================================================

ALTER TABLE public.user_safety_exclusions
    ADD COLUMN identified_at           TIMESTAMPTZ,
    ADD COLUMN identification_evidence VARCHAR(500);

ALTER TABLE public.user_safety_exclusions
    -- Solo se identifica despues lo que llego sin identificar; con constancia.
    ADD CONSTRAINT ck_use_identificacion CHECK (
        (identified_at IS NULL AND identification_evidence IS NULL)
        OR (identified_at IS NOT NULL AND identification_evidence IS NOT NULL
            AND reason = 'DOLOR_SESION_SIN_IDENTIFICAR'));
