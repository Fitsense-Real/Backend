package main.web.services.fitsense.analytics.domain.model.valueobjects;

import java.math.BigDecimal;

/**
 * Una sesion que cuenta, traducida al lenguaje de analytics.
 * <p>
 * Solo llegan las que tienen counts_toward_adherence = true: los intentos
 * distintos de un mismo entrenamiento no se suman (17.4, punto 4).
 */
public record WeekSessionInput(
        Long plannedWorkoutId,
        BigDecimal completionPercentage,
        Short activeMinutes,
        Short sessionRpe,
        Short satisfaction,
        int completedExercises,
        int executedVolume,
        int executedReps,
        int executedSeconds,
        String dominantSkipReason,
        /** Algun ejercicio dejado por dolor, aunque no sea la causa dominante (27/10/2026). */
        boolean dolorEnEjercicio
) {
    /** Sin el indicador de dolor por ejercicio. */
    public WeekSessionInput(Long plannedWorkoutId, BigDecimal completionPercentage, Short activeMinutes,
                            Short sessionRpe, Short satisfaction, int completedExercises, int executedVolume,
                            int executedReps, int executedSeconds, String dominantSkipReason) {
        this(plannedWorkoutId, completionPercentage, activeMinutes, sessionRpe, satisfaction, completedExercises,
                executedVolume, executedReps, executedSeconds, dominantSkipReason, false);
    }

    public double completion() {
        return completionPercentage == null ? 0.0 : completionPercentage.doubleValue();
    }
}