package main.web.services.fitsense.administration.interfaces.rest.resources;

import java.math.BigDecimal;
import java.util.List;

/**
 * Adherencia oficial por segmento. {@code by} es GOAL_TYPE, FITNESS_LEVEL,
 * TRAINING_LOCATION (perfil actual) o RISK_LEVEL (el nivel de cada semana). Un
 * segmento con un solo participante identifica a esa persona: la interfaz debe
 * tratar los segmentos pequenos con cuidado.
 */
public record SegmentsResource(AnalyticsFilters filters, String by, List<Segment> segments) {

    /** value NONE: sin perfil (GOAL_TYPE...) o semana sin nivel de riesgo (RISK_LEVEL). */
    public record Segment(String value, long participants, long weeksWithPlan,
                          BigDecimal meanOfficialAdherencePct, BigDecimal medianOfficialAdherencePct,
                          BigDecimal meanRegistrationCoveragePct) {}
}
