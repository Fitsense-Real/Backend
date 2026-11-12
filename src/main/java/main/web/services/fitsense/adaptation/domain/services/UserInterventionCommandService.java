package main.web.services.fitsense.adaptation.domain.services;

import main.web.services.fitsense.adaptation.domain.model.aggregates.UserIntervention;
import main.web.services.fitsense.adaptation.domain.model.commands.DecideWeeklyAdjustmentCommand;
import main.web.services.fitsense.adaptation.domain.model.commands.LinkResultingPlanCommand;
import main.web.services.fitsense.adaptation.domain.model.commands.RecordInterventionOutcomeCommand;
import main.web.services.fitsense.adaptation.domain.model.valueobjects.AdaptationOrder;
import main.web.services.fitsense.adaptation.domain.model.valueobjects.AdjustmentDecision;

import java.util.Optional;

public interface UserInterventionCommandService {

    /**
     * La decision viaja junto a la fila guardada porque la orden completa (dias,
     * minutos y dificultad forzados) es mas rica que lo que la tabla conserva:
     * la tabla guarda deltas para el analisis, el generador necesita valores
     * absolutos y el rango de volumen.
     *
     * @param decision lo que se persiste en {@code user_interventions}. Desde la
     *                 activacion de la orden localizada es SIEMPRE
     *                 {@code NONE / 0}: esa tabla registra el mecanismo
     *                 porcentual, y ese mecanismo ya no actua. Decir ahi que
     *                 hubo una reduccion del 15 % cuando lo que ocurrio fue una
     *                 correccion de dos sesiones seria registrar un hecho falso.
     * @param orden    QUE SESIONES FALLARON Y QUE SE PERMITE HACER EN CADA UNA.
     *                 Es lo que decide de verdad, y hasta ahora se calculaba y
     *                 se tiraba: el decisor la producia, nadie la transportaba y
     *                 {@code PlanAdjustment.targetedSessions} llegaba vacio
     *                 siempre, asi que {@code adapta()} era falso siempre y el
     *                 camino de adaptacion era inalcanzable desde el ciclo
     *                 semanal.
     */
    record AdjustmentResult(UserIntervention intervention,
                            AdjustmentDecision decision,
                            AdaptationOrder orden) {}

    Optional<AdjustmentResult> handle(DecideWeeklyAdjustmentCommand command);
    Optional<UserIntervention> handle(LinkResultingPlanCommand command);
    void handle(RecordInterventionOutcomeCommand command);
}
