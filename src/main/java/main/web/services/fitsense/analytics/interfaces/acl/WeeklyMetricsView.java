package main.web.services.fitsense.analytics.interfaces.acl;

import main.web.services.fitsense.shared.domain.model.valueobjects.EvidenceRule;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Lo que adaptation necesita para decidir el ajuste: el porcentaje que dispara
 * la regla y la causa que la modula.
 */
public record WeeklyMetricsView(
        Long weeklyMetricId,
        Long planId,
        short weekNumber,
        LocalDate weekStartDate,
        boolean hasActivePlan,
        BigDecimal weightedAdherencePct,
        BigDecimal averageSessionRpe,
        int consecutiveSkips,
        String dominantSkipReason,
        String riskLevel,
        boolean dropout,
        int scheduledWorkouts,
        int assignedExercises,
        // V30. Lo que necesita el decisor para separar "no sabemos" de "no
        // entreno". weightedAdherencePct sigue siendo la primaria.
        Integer recordedWorkouts,
        BigDecimal observedAdherencePct,
        BigDecimal registrationCoveragePct
) {
    /** Sin plan o sin adherencia calculable no hay nada que ajustar. */
    public boolean isActionable() {
        return hasActivePlan && weightedAdherencePct != null && planId != null;
    }

    /**
     * V30. isActionable dice que la semana se puede medir; esto dice que se
     * puede DECIDIR sobre ella. El umbral de cobertura es provisional y entra
     * como parametro, no escrito aqui.
     */
    public boolean hasEnoughEvidence(double coberturaMinimaPct) {
        return isActionable() && EvidenceRule.enoughEvidence(
                observedAdherencePct, registrationCoveragePct, coberturaMinimaPct);
    }
}
