package main.web.services.fitsense.administration.interfaces.rest.resources;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Resumen global, solo cifras agregadas y sin datos personales.
 *
 * @param enrollment          inscripcion actual (no depende del periodo)
 * @param measurement         filas de weekly_user_metrics del periodo
 * @param officialAdherence   A_w = weighted_adherence_pct (ADHERENCIA.md), en semanas con plan
 * @param meanRegistrationCoveragePct media de registration_coverage_pct
 * @param latestWeek          la ultima semana medida de cada participante dentro del periodo
 */
public record AnalyticsOverviewResource(
        AnalyticsFilters filters,
        Enrollment enrollment,
        Measurement measurement,
        Summary officialAdherence,
        BigDecimal meanRegistrationCoveragePct,
        LatestWeek latestWeek
) {
    public record Enrollment(long enrolled, long active, long withdrawn) {}

    public record Measurement(long participantsMeasured, long weeksMeasured, long weeksWithPlan,
                              long weeksWithAdherence) {}

    public record Summary(BigDecimal meanPct, BigDecimal medianPct) {}

    /** riskLevels: LOW, MODERATE, HIGH, CRITICAL y NONE (semana sin nivel: sin plan). */
    public record LatestWeek(long participants, Map<String, Long> riskLevels, long dropouts) {}
}
