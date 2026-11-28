package main.web.services.fitsense.planning.interfaces.rest.resources;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record WeeklyTrainingPlanResource(
        Long planId,
        short weekNumber,
        LocalDate weekStartDate,
        LocalDate weekEndDate,
        short planVersion,
        Long parentPlanId,
        short plannedDaysCount,
        String status,
        String generationSource,
        String modelName,
        String adjustmentApplied,
        String adjustmentReason,
        OffsetDateTime activatedAt,
        List<PlannedWorkoutResource> workouts,
        /** Con que esfuerzo hacer las repeticiones. Sin esto el numero fijo no se entiende. */
        EffortGuidanceResource effortGuidance,
        /**
         * Que paso con la adaptacion que publico esta semana (V32), o null en una generacion inicial.
         * {@code adjustmentApplied} es el nombre de la DECISION y no dice si algo cambio: el cliente debe
         * leer {@code adaptation.resultado} (orden de cierre del MVP, 31/10/2026).
         */
        main.web.services.fitsense.planning.domain.model.valueobjects.AdaptationOutcome adaptation
) {}