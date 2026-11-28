package main.web.services.fitsense.profiling.domain.exceptions;

import main.web.services.fitsense.shared.domain.exceptions.DomainRuleViolationException;

/**
 * Regla del estudio (30/10/2026): lo que la persona declaro al registrarse se mantiene durante todo
 * el estudio. Desde su primer plan, el perfil completo queda fijo. Asi la semana copiada nunca
 * deja de cumplir los dias o el equipamiento del perfil (V3/V1).
 */
public class UserProfileLockedException extends DomainRuleViolationException {
    public UserProfileLockedException(Long userId) {
        super("El perfil del usuario " + userId + " no se puede modificar: ya tiene un plan de "
                + "entrenamiento y los datos declarados se mantienen durante todo el estudio.");
    }
}
