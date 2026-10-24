package main.web.services.fitsense.planning.interfaces.rest.resources;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Semana PARCIAL propuesta y NO activada (4A v6): la app la muestra como
 * alternativa, con lo que falta, y el participante decide. No tiene planId
 * porque no se guardo.
 *
 * @param status siempre {@code REQUIERE_ACEPTACION}
 */
public record PartialPlanProposalResource(
        String status,
        LocalDate weekStartDate,
        CapabilityCoverageResource capabilityCoverage,
        List<ProposedWorkout> workouts
) {
    public record ProposedWorkout(LocalDate scheduledDate, String focusCode, String workoutName,
                                  int expectedDurationMinutes, List<ProposedExercise> exercises) {}

    public record ProposedExercise(Long exerciseId, String exerciseName, String prescriptionType, Short plannedSets,
                                   Short plannedReps, Integer plannedDurationSeconds, BigDecimal targetLoadKg,
                                   Short restSeconds, String notes) {}
}
