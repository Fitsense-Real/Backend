package main.web.services.fitsense.planning.application.internal.generation;

import main.web.services.fitsense.planning.domain.model.aggregates.WeeklyTrainingPlan;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
import org.springframework.stereotype.Component;

/**
 * Exactamente lo que hace POST /users/me/plan/generate, sin la peticion HTTP:
 * prepare (transaccion de lectura, conjunto elegible y cobertura), generate
 * (IA y motor de reglas, sin transaccion, con todos los validadores) y persist
 * (transaccion corta que publica solo un plan valido). No hay un camino
 * alternativo que pueda saltarse una regla.
 */
@Component
public class PipelineFirstPlanGenerationRunner implements FirstPlanGenerationRunner {

    private final WeeklyTrainingPlanCommandService commandService;

    public PipelineFirstPlanGenerationRunner(WeeklyTrainingPlanCommandService commandService) {
        this.commandService = commandService;
    }

    @Override
    public Long run(Long userId, Runnable beforePersist) {
        var monday = commandService.firstPlanWeekStart(userId);
        var command = GenerateWeeklyPlanCommand.firstPlan(userId, monday);
        var prepared = commandService.prepare(command);
        var generated = commandService.generate(prepared);
        beforePersist.run();
        return commandService.persist(prepared, generated)
                .map(WeeklyTrainingPlan::getId)
                .orElseThrow(() -> new IllegalStateException(
                        "La publicacion no devolvio plan para el usuario " + userId));
    }
}
