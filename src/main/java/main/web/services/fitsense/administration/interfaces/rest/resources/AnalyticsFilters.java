package main.web.services.fitsense.administration.interfaces.rest.resources;

import main.web.services.fitsense.profiling.domain.model.valueobjects.FitnessLevel;
import main.web.services.fitsense.profiling.domain.model.valueobjects.GoalType;
import main.web.services.fitsense.profiling.domain.model.valueobjects.TrainingLocation;

import java.time.LocalDate;

/**
 * Filtros comunes de la analitica de ADMIN, todos opcionales. {@code from} y
 * {@code to} acotan {@code week_start_date} (inclusive). El objetivo, el nivel y
 * el lugar son los del perfil ACTUAL del participante: el perfil se puede editar
 * y no se versiona por semana.
 */
public record AnalyticsFilters(
        LocalDate from,
        LocalDate to,
        GoalType goalType,
        FitnessLevel fitnessLevel,
        TrainingLocation trainingLocation
) {
    public static AnalyticsFilters period(LocalDate from, LocalDate to) {
        return new AnalyticsFilters(from, to, null, null, null);
    }

    public void validate() {
        if (from != null && to != null && from.isAfter(to))
            throw new IllegalArgumentException("'from' no puede ser posterior a 'to'.");
    }
}
