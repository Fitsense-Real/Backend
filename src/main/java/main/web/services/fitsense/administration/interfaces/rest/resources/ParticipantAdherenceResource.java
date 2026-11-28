package main.web.services.fitsense.administration.interfaces.rest.resources;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Las semanas medidas de un participante, sin datos personales. */
public record ParticipantAdherenceResource(Long participantId, String participantCode,
                                           LocalDate from, LocalDate to, List<Week> weeks) {

    /** officialAdherencePct = weighted_adherence_pct; NULL en una semana sin plan, nunca 100. */
    public record Week(LocalDate weekStartDate, Integer weekNumber, boolean hasActivePlan,
                       int scheduledWorkouts, int completedWorkouts, int skippedWorkouts,
                       Integer recordedWorkouts, BigDecimal officialAdherencePct,
                       BigDecimal frequencyAdherencePct, BigDecimal workoutAdherencePct,
                       BigDecimal exerciseAdherencePct, BigDecimal registrationCoveragePct,
                       int consecutiveSkips, String dominantSkipReason, String riskLevel,
                       boolean isDropout) {}
}
