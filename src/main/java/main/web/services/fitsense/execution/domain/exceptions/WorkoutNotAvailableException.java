package main.web.services.fitsense.execution.domain.exceptions;

import main.web.services.fitsense.shared.domain.exceptions.DomainRuleViolationException;

public class WorkoutNotAvailableException extends DomainRuleViolationException {

    public static WorkoutNotAvailableException expired(Long plannedWorkoutId) {
        return new WorkoutNotAvailableException(
                ("El entrenamiento %d ya vencio. Si lo hiciste, registralo con el reporte "
                        + "retroactivo en vez de abrir una sesion nueva.").formatted(plannedWorkoutId));
    }

    public static WorkoutNotAvailableException closed(Long plannedWorkoutId, String status) {
        return new WorkoutNotAvailableException(
                "El entrenamiento %d ya se cerro como %s.".formatted(plannedWorkoutId, status));
    }

    /**
     * La sesion contiene ejercicios que HOY chocan con la seguridad del
     * participante (planes publicados antes de una correccion). No se ejecuta:
     * necesita una version corregida o una intervencion.
     */
    public static WorkoutNotAvailableException unsafe(Long plannedWorkoutId, java.util.Map<Long, String> conflictos) {
        return new WorkoutNotAvailableException(("SESION_BLOQUEADA_POR_SEGURIDAD: el entrenamiento %d contiene "
                + "ejercicios que ya no son seguros para ti %s (ejercicio prescrito: causa). Requiere una "
                + "correccion antes de hacerlo.").formatted(plannedWorkoutId, new java.util.TreeMap<>(conflictos)));
    }

    /** Un ejercicio concreto de la sesion, en el mismo caso. Omitirlo si se puede. */
    public static WorkoutNotAvailableException unsafeExercise(Long plannedExerciseId, String causa) {
        return new WorkoutNotAvailableException(("SESION_BLOQUEADA_POR_SEGURIDAD: el ejercicio prescrito %d ya no "
                + "es seguro para ti (%s). Solo puede registrarse como omitido.").formatted(plannedExerciseId, causa));
    }

    private WorkoutNotAvailableException(String message) {
        super(message);
    }
}
