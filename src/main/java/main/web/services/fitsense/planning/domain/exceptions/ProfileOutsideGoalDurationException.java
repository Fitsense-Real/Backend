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
        super(("%s: el perfil del usuario %d declara %d min por sesion y el objetivo %s admite de %d a %d."
                + " El perfil no se modifica; no se genera un plan que no puede cumplir sus reglas.")
                .formatted(CODIGO, userId, minutos, goalType, DuracionPorObjetivoPolicy.minimo(goalType),
                        DuracionPorObjetivoPolicy.MAXIMO));
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }
}
