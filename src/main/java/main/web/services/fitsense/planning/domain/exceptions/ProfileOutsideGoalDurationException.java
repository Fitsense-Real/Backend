package main.web.services.fitsense.planning.domain.exceptions;

import main.web.services.fitsense.planning.domain.services.DuracionPorObjetivoPolicy;
import main.web.services.fitsense.shared.domain.exceptions.DomainRuleViolationException;

/**
 * P-01 · un perfil congelado ANTES de la politica declara una duracion que su objetivo ya no
 * admite. No se corrige el perfil (es inmutable y P-04 no se implementa): se informa, antes de
 * generar y sin llamar a la IA. HTTP 422, como cualquier regla de dominio.
 */
public class ProfileOutsideGoalDurationException extends DomainRuleViolationException {

    public static final String CODIGO = "PERFIL_FUERA_DE_P01";

    private final Long userId;

    public ProfileOutsideGoalDurationException(Long userId, String goalType, int minutos) {
        super(("%s: el perfil del usuario %d (objetivo %s) declara %d min por sesion y P-01 admite de %d a %d."
                + " El perfil esta congelado y no se modifica; no se genera un plan nuevo para el.")
                .formatted(CODIGO, userId, goalType, minutos, DuracionPorObjetivoPolicy.MINIMO,
                        DuracionPorObjetivoPolicy.MAXIMO));
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }
}
