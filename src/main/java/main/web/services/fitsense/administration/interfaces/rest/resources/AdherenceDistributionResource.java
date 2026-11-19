package main.web.services.fitsense.administration.interfaces.rest.resources;

import java.math.BigDecimal;
import java.util.List;

/**
 * Distribucion de los indicadores semanales ya guardados, uno por columna. Las
 * estadisticas descriptivas (media, mediana, cuartiles, extremos) se calculan
 * sobre las semanas con valor; ninguna semana sin plan entra, porque ahi todas
 * las columnas son NULL. La adherencia observada no aparece: es interna de la
 * adaptacion y no es la adherencia de la tesis (ADHERENCIA.md).
 */
public record AdherenceDistributionResource(AnalyticsFilters filters, List<Indicator> indicators) {

    /**
     * @param indicator OFFICIAL_ADHERENCE, FREQUENCY_ADHERENCE, WORKOUT_ADHERENCE,
     *                  EXERCISE_ADHERENCE o REGISTRATION_COVERAGE
     * @param column    la columna de weekly_user_metrics de la que sale
     * @param weeks     semanas con valor (n)
     */
    public record Indicator(String indicator, String column, long weeks, long participants,
                            BigDecimal meanPct, BigDecimal medianPct, BigDecimal p25Pct,
                            BigDecimal p75Pct, BigDecimal minPct, BigDecimal maxPct) {}
}
