package main.web.services.fitsense.analytics.application.internal.queryservices;

import main.web.services.fitsense.analytics.application.internal.outboundservices.acl.ExternalConfigurationService;
import main.web.services.fitsense.analytics.application.internal.outboundservices.acl.ExternalExecutionService;
import main.web.services.fitsense.analytics.application.internal.outboundservices.acl.ExternalPlanningService;
import main.web.services.fitsense.analytics.domain.model.queries.GetSessionOutcomesQuery;
import main.web.services.fitsense.analytics.domain.model.valueobjects.SessionOutcome;
import main.web.services.fitsense.analytics.domain.services.SessionOutcomeIdentifier;
import main.web.services.fitsense.analytics.domain.services.SessionOutcomeQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Reune el plan previsto, las sesiones que cuentan y los umbrales activos, y
 * deja que el identificador -que es puro- decida cual fue un problema.
 * <p>
 * Usa las mismas tres fachadas que el calculo semanal, asi que no abre ninguna
 * dependencia nueva entre modulos.
 */
@Service
public class SessionOutcomeQueryServiceImpl implements SessionOutcomeQueryService {

    private final ExternalPlanningService externalPlanningService;
    private final ExternalExecutionService externalExecutionService;
    private final ExternalConfigurationService externalConfigurationService;
    private final SessionOutcomeIdentifier identifier;

    public SessionOutcomeQueryServiceImpl(ExternalPlanningService externalPlanningService,
                                          ExternalExecutionService externalExecutionService,
                                          ExternalConfigurationService externalConfigurationService,
                                          SessionOutcomeIdentifier identifier) {
        this.externalPlanningService = externalPlanningService;
        this.externalExecutionService = externalExecutionService;
        this.externalConfigurationService = externalConfigurationService;
        this.identifier = identifier;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SessionOutcome> handle(GetSessionOutcomesQuery query) {
        var params = externalConfigurationService.fetchActive().params();
        var plan = externalPlanningService.fetchWeekPlan(query.userId(), query.weekStartDate());
        if (plan == null || !plan.hasActivePlan()) return List.of();

        var sessions = externalExecutionService.fetchWeekSessions(plan,
                params.adjustment().durationToRepsDivisor());

        return identifier.identify(plan, sessions, params);
    }
}
