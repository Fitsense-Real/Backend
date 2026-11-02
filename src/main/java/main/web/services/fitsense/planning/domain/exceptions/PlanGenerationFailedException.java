package main.web.services.fitsense.planning.domain.exceptions;

import main.web.services.fitsense.shared.domain.exceptions.DomainRuleViolationException;

/**
 * Fallaron los dos intentos de IA y tambien el motor de reglas (19.4).
 * <p>
 * La semana queda sin plan activo: has_active_plan = false y la adherencia de
 * esa semana sera NULL, no 0. Es un dato del estudio, no un error que ocultar.
 */
public class PlanGenerationFailedException extends DomainRuleViolationException {

    /** Los problemas del ultimo intento (para el diagnostico NOFACT-0.1); vacia si no se conocen. */
    private final transient java.util.List<String> problems;

    public PlanGenerationFailedException(Long userId, String detail) {
        this(userId, detail, java.util.List.of());
    }

    public PlanGenerationFailedException(Long userId, String detail, java.util.List<String> problems) {
        super("No se pudo generar un plan valido para el usuario %d. %s".formatted(userId, detail));
        this.problems = problems == null ? java.util.List.of() : java.util.List.copyOf(problems);
    }

    public java.util.List<String> problems() {
        return problems;
    }
}
