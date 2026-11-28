package main.web.services.fitsense.analytics.application.internal.commandservices;

import main.web.services.fitsense.analytics.application.internal.outboundservices.acl.ExternalConfigurationService;
import main.web.services.fitsense.analytics.application.internal.outboundservices.acl.ExternalExecutionService;
import main.web.services.fitsense.analytics.application.internal.outboundservices.acl.ExternalIamService;
import main.web.services.fitsense.analytics.application.internal.outboundservices.acl.ExternalPlanningService;
import main.web.services.fitsense.analytics.domain.model.aggregates.WeeklyUserMetrics;
import main.web.services.fitsense.analytics.domain.model.commands.CalculateWeeklyMetricsCommand;
import main.web.services.fitsense.analytics.domain.services.WeeklyMetricsCalculator;
import main.web.services.fitsense.analytics.domain.services.WeeklyUserMetricsCommandService;
import main.web.services.fitsense.analytics.infrastructure.persistence.jpa.repositories.WeeklyUserMetricsRepository;
import main.web.services.fitsense.shared.domain.model.valueobjects.TrainingWeek;
import main.web.services.fitsense.shared.infrastructure.json.JsonSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class WeeklyUserMetricsCommandServiceImpl implements WeeklyUserMetricsCommandService {

    private final WeeklyUserMetricsRepository metricsRepository;
    private final WeeklyMetricsCalculator calculator;
    private final ExternalPlanningService externalPlanningService;
    private final ExternalExecutionService externalExecutionService;
    private final ExternalConfigurationService externalConfigurationService;
    private final ExternalIamService externalIamService;
    private final JsonSupport jsonSupport;
    /** C4: null solo en pruebas unitarias que construyen el servicio a mano. */
    private main.web.services.fitsense.analytics.infrastructure.persistence.jdbc.HistorialDeRecalculos historial;

    public WeeklyUserMetricsCommandServiceImpl(WeeklyUserMetricsRepository metricsRepository,
                                               WeeklyMetricsCalculator calculator,
                                               ExternalPlanningService externalPlanningService,
                                               ExternalExecutionService externalExecutionService,
                                               ExternalConfigurationService externalConfigurationService,
                                               ExternalIamService externalIamService,
                                               JsonSupport jsonSupport) {
        this.metricsRepository = metricsRepository;
        this.calculator = calculator;
        this.externalPlanningService = externalPlanningService;
        this.externalExecutionService = externalExecutionService;
        this.externalConfigurationService = externalConfigurationService;
        this.externalIamService = externalIamService;
        this.jsonSupport = jsonSupport;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void historial(main.web.services.fitsense.analytics.infrastructure.persistence.jdbc.HistorialDeRecalculos historial) {
        this.historial = historial;
    }

    @Override
    @Transactional
    public Optional<WeeklyUserMetrics> handle(CalculateWeeklyMetricsCommand command) {
        var week = TrainingWeek.containing(command.weekStartDate());
        var configuration = externalConfigurationService.fetchActive();

        // Mismo divisor de 18.1 para lo prescrito y lo ejecutado: si cada lado
        // usara el suyo, las dos cifras no serian comparables entre si.
        int divisor = configuration.params().adjustment().durationToRepsDivisor();

        var plan = externalPlanningService.fetchWeekPlan(command.userId(), week.startDate());
        int plannedWeekVolume = externalPlanningService.weekVolume(
                command.userId(), week.startDate(), divisor);

        // Componentes crudos del volumen prescrito (V17): repeticiones y segundos
        // por separado, sin el factor de conversion. El equivalente se sigue
        // guardando, pero con estos se puede rehacer la cuenta con otro divisor
        // y reportar que proporcion del volumen viene de ejercicios de duracion.
        var breakdown = externalPlanningService.weekBreakdown(
                command.userId(), week.startDate());

        var sessions = externalExecutionService.fetchWeekSessions(plan, divisor);
        // Solo sesiones validas (>= 30 %), contadas en el dia local del participante.
        var daysSinceLastWorkout = externalExecutionService.daysSinceLastWorkout(
                command.userId(), week.endDate(),
                configuration.params().adherence().sessionValidThresholdPct(),
                externalIamService.timezoneOf(command.userId()));

        // La caida se mide contra la semana anterior YA CALCULADA, no contra la
        // que se este calculando ahora: comparar con una fila a medio escribir
        // haria que el riesgo dependa del orden en que corre la tarea.
        var previousAdherence = metricsRepository
                .findByUserIdAndWeekStartDate(command.userId(), week.previous().startDate())
                .map(WeeklyUserMetrics::getWeightedAdherencePct)
                .orElse(null);

        var calculation = calculator.calculate(plan, sessions, previousAdherence,
                daysSinceLastWorkout, plannedWeekVolume,
                breakdown.reps(), breakdown.seconds(), configuration.params());

        var riskFactorsJson = calculation.riskFactors() == null || calculation.riskFactors().isEmpty()
                ? null
                : jsonSupport.write(calculation.riskFactors());

        var existing = metricsRepository.findByUserIdAndWeekStartDate(
                command.userId(), week.startDate());

        var metrics = existing
                .map(found -> {
                    // C4: lo que habia antes de sobrescribir. La formula y calculated_at no cambian.
                    var antes = found.valoresMedidos();
                    var versionAnterior = found.getCalculationVersion();
                    found.recalculate(calculation, riskFactorsJson, configuration.version());
                    if (historial != null)
                        historial.registrar(found, antes, versionAnterior,
                                main.web.services.fitsense.analytics.infrastructure.persistence.jdbc
                                        .HistorialDeRecalculos.CIERRE_REPETIDO,
                                main.web.services.fitsense.analytics.infrastructure.persistence.jdbc
                                        .HistorialDeRecalculos.PROCESO_CIERRE_SEMANAL);
                    return found;
                })
                .orElseGet(() -> new WeeklyUserMetrics(command.userId(), week.startDate(),
                        week.endDate(), calculation, riskFactorsJson, configuration.version()));

        return Optional.of(metricsRepository.save(metrics));
    }
}