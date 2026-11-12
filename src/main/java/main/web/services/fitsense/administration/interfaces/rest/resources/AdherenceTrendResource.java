package main.web.services.fitsense.administration.interfaces.rest.resources;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Serie semanal agregada de todos los participantes que cumplen los filtros. */
public record AdherenceTrendResource(AnalyticsFilters filters, List<Week> weeks) {

    public record Week(LocalDate weekStartDate, long participantsMeasured, long weeksWithPlan,
                       BigDecimal meanOfficialAdherencePct, BigDecimal medianOfficialAdherencePct,
                       BigDecimal meanRegistrationCoveragePct, Map<String, Long> riskLevels,
                       long dropouts) {}
}
