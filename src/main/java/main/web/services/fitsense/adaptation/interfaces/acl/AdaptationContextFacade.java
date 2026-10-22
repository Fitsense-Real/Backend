package main.web.services.fitsense.adaptation.interfaces.acl;

import main.web.services.fitsense.adaptation.domain.model.commands.DecideWeeklyAdjustmentCommand;
import main.web.services.fitsense.adaptation.domain.model.valueobjects.AdaptationOutcome;
import main.web.services.fitsense.adaptation.domain.model.commands.LinkResultingPlanCommand;
import main.web.services.fitsense.adaptation.domain.model.commands.RecordInterventionOutcomeCommand;
import main.web.services.fitsense.adaptation.domain.services.UserInterventionCommandService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Unico punto de entrada a adaptation. Lo consume la tarea semanal: decide el
 * ajuste con las metricas ya cerradas y, una vez generado el plan, registra que
 * salio de la orden.
 */
@Service
public class AdaptationContextFacade {

    private final UserInterventionCommandService commandService;

    public AdaptationContextFacade(UserInterventionCommandService commandService) {
        this.commandService = commandService;
    }

    /** Vacio si no habia nada que ajustar: primera semana o semana sin plan. */
    @Transactional
    public Optional<AdjustmentOrderView> decideForWeek(Long userId, LocalDate measuredWeekStartDate) {
        return commandService.handle(new DecideWeeklyAdjustmentCommand(userId, measuredWeekStartDate))
                .map(AdaptationContextFacade::toView);
    }

    @Transactional
    public void linkResultingPlan(Long interventionId, Long resultingPlanId,
                                  LocalDate resultingWeekStart) {
        commandService.handle(new LinkResultingPlanCommand(
                interventionId, resultingPlanId, resultingWeekStart));
    }

    @Transactional
    public void recordOutcome(Long measuredPlanId, BigDecimal adherenceAfterPct) {
        commandService.handle(new RecordInterventionOutcomeCommand(
                measuredPlanId, adherenceAfterPct));
    }

    /**
     * PROYECCION, no copia. La orden de dentro lleva seis campos por sesion
     * senalada; cruzan dos. Los otros cuatro -fecha, cumplimiento, motivo y
     * RPE- son del contexto de adaptacion, y planning los obtiene de
     * {@code context.previousWeek()} por el mismo identificador cuando los
     * necesita para el prompt.
     */
    private static AdjustmentOrderView toView(UserInterventionCommandService.AdjustmentResult result) {
        var intervention = result.intervention();
        var decision = result.decision();
        var orden = result.orden();

        var senaladas = orden == null ? List.<AdjustmentOrderView.TargetedSessionView>of()
                : orden.sessions().stream()
                .map(s -> new AdjustmentOrderView.TargetedSessionView(
                        s.plannedWorkoutId(), s.intervencion()))
                .toList();

        return new AdjustmentOrderView(
                intervention.getId(),
                decision.typeNames(),
                decision.primaryType(),
                decision.targetVolume(),
                decision.targetVolumeMin(),
                decision.targetVolumeMax(),
                decision.targetVolumeChangePct(),
                decision.loadChangePct(),
                decision.forcedDaysPerWeek(),
                decision.forcedSessionMinutes(),
                decision.forcedMaxDifficulty(),
                decision.reason(),
                intervention.getTriggerSkipReason() == null ? null
                        : intervention.getTriggerSkipReason().name(),
                decision.distributionHint(),
                decision.message(),
                orden == null ? AdaptationOutcome.NO_CHANGE.name() : orden.outcome().name(),
                senaladas);
    }
}
