package main.web.services.fitsense.planning.application.internal.generation;

import main.web.services.fitsense.planning.domain.model.aggregates.WeeklyTrainingPlan;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

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
    private final TransactionTemplate tx;

    public PipelineFirstPlanGenerationRunner(WeeklyTrainingPlanCommandService commandService,
                                             PlatformTransactionManager transactionManager) {
        this.commandService = commandService;
        this.tx = new TransactionTemplate(transactionManager);
    }

    @Override
    public Long run(Long userId, Runnable beforePersist) {
        var monday = commandService.firstPlanWeekStart(userId);
        var command = GenerateWeeklyPlanCommand.firstPlan(userId, monday);
        var prepared = commandService.prepare(command);
        var generated = commandService.generate(prepared);
        // V59: la comprobacion de propiedad y la publicacion en la MISMA transaccion corta. beforePersist bloquea la
        // fila de la operacion (FOR UPDATE) y persist se une a esta transaccion: un worker que perdio el lease no
        // puede publicar, y ninguno puede quitarselo mientras publica. Sin llamadas a la IA dentro.
        return tx.execute(s -> {
            beforePersist.run();
            return commandService.persist(prepared, generated)
                    .map(WeeklyTrainingPlan::getId)
                    .orElseThrow(() -> new IllegalStateException(
                            "La publicacion no devolvio plan para el usuario " + userId));
        });
    }
}
