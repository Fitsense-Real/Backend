package main.web.services.fitsense.execution.interfaces.acl;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Una sesion que cuenta para la adherencia, vista desde fuera de execution. */
public record SessionSummaryView(
        Long sessionId,
        Long plannedWorkoutId,
        Long planId,
        OffsetDateTime startedAt,
        BigDecimal completionPercentage,
        String status,
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
    public SessionSummaryView(Long sessionId, Long plannedWorkoutId, Long planId, OffsetDateTime startedAt,
                              BigDecimal completionPercentage, String status, Short activeMinutes, Short sessionRpe,
                              Short satisfaction, int completedExercises, int executedVolume, int executedReps,
                              int executedSeconds, String dominantSkipReason) {
        this(sessionId, plannedWorkoutId, planId, startedAt, completionPercentage, status, activeMinutes, sessionRpe,
                satisfaction, completedExercises, executedVolume, executedReps, executedSeconds, dominantSkipReason,
                false);
    }

    public boolean isCompleted() {
        return "COMPLETED".equals(status);
    }
}