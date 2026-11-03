package main.web.services.fitsense.analytics.interfaces.acl;

import main.web.services.fitsense.analytics.domain.model.aggregates.WeeklyUserMetrics;
import main.web.services.fitsense.analytics.domain.model.commands.CalculateWeeklyMetricsCommand;
import main.web.services.fitsense.analytics.domain.model.queries.GetSessionOutcomesQuery;
import main.web.services.fitsense.analytics.domain.model.queries.GetWeeklyMetricsByWeekQuery;
import main.web.services.fitsense.analytics.domain.model.valueobjects.SessionOutcome;
import main.web.services.fitsense.analytics.domain.services.SessionOutcomeQueryService;
import main.web.services.fitsense.analytics.domain.services.WeeklyUserMetricsCommandService;
import main.web.services.fitsense.analytics.domain.services.WeeklyUserMetricsQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Unico punto de entrada a analytics. Lo consumen adaptation (para decidir el
 * ajuste) y la tarea semanal (para disparar el cierre).
 */
@Service
public class AnalyticsContextFacade {

    private final WeeklyUserMetricsCommandService commandService;
    private final WeeklyUserMetricsQueryService queryService;
    private final SessionOutcomeQueryService sessionOutcomeQueryService;

    public AnalyticsContextFacade(WeeklyUserMetricsCommandService commandService,
                                  WeeklyUserMetricsQueryService queryService,
                                  SessionOutcomeQueryService sessionOutcomeQueryService) {
        this.commandService = commandService;
        this.queryService = queryService;
        this.sessionOutcomeQueryService = sessionOutcomeQueryService;
    }

    /** Cierra la semana. Idempotente: recalcular sobrescribe. */
    @Transactional
    public Optional<WeeklyMetricsView> calculateWeek(Long userId, LocalDate weekStartDate) {
        return commandService.handle(new CalculateWeeklyMetricsCommand(userId, weekStartDate))
                .map(AnalyticsContextFacade::toView);
    }

    @Transactional(readOnly = true)
    public Optional<WeeklyMetricsView> fetchWeek(Long userId, LocalDate weekStartDate) {
        return queryService.handle(new GetWeeklyMetricsByWeekQuery(userId, weekStartDate))
                .map(AnalyticsContextFacade::toView);
    }

    /**
     * Lo que paso en cada sesion de la semana, con las senaladas ya marcadas.
     * <p>
     * No lee de weekly_user_metrics: se resuelve en el momento a partir del plan
     * previsto, las sesiones que cuentan y los umbrales activos. Asi cambiar un
     * umbral no obliga a recalcular nada, que es la misma razon por la que el
     * calculo semanal es un servicio puro.
     */
    @Transactional(readOnly = true)
    public List<SessionOutcomeView> fetchSessionOutcomes(Long userId, LocalDate weekStartDate) {
        return sessionOutcomeQueryService
                .handle(new GetSessionOutcomesQuery(userId, weekStartDate)).stream()
                .map(AnalyticsContextFacade::toView)
                .toList();
    }

    private static SessionOutcomeView toView(SessionOutcome outcome) {
        return new SessionOutcomeView(
                outcome.plannedWorkoutId(), outcome.scheduledDate(),
                outcome.status(), outcome.recorded(), outcome.completionPct(),
                outcome.skipReason(), outcome.sessionRpe(), outcome.problematic());
    }

    private static WeeklyMetricsView toView(WeeklyUserMetrics metrics) {
        return new WeeklyMetricsView(
                metrics.getId(),
                metrics.getPlanId(),
                metrics.getWeekNumber(),
                metrics.getWeekStartDate(),
                metrics.isHasActivePlan(),
                metrics.getWeightedAdherencePct(),
                metrics.getAverageSessionRpe(),
                metrics.getConsecutiveSkips(),
                metrics.getDominantSkipReason() == null ? null : metrics.getDominantSkipReason().name(),
                metrics.getRiskLevel() == null ? null : metrics.getRiskLevel().name(),
                metrics.isDropout(),
                metrics.getScheduledWorkouts(),
                metrics.getAssignedExercises(),
                // V30. Van en Integer y no en int: en las filas anteriores a
                // V30 esto es NULL, y un 0 ahi afirmaria cobertura cero cuando
                // lo que ocurre es que no se calculo.
                metrics.getRecordedWorkouts() == null ? null
                        : metrics.getRecordedWorkouts().intValue(),
                metrics.getObservedAdherencePct(),
                metrics.getRegistrationCoveragePct());
    }
}
