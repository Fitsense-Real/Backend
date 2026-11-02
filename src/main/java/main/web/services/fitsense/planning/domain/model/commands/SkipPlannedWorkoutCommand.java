package main.web.services.fitsense.planning.domain.model.commands;

import main.web.services.fitsense.shared.domain.model.valueobjects.ContextoFaltaDeTiempo;
import main.web.services.fitsense.shared.domain.model.valueobjects.SkipReason;

/**
 * Salto declarado por el usuario. La causa es obligatoria: sin ella el ajuste de
 * la semana siguiente solo podria responder al porcentaje, que es justo lo que
 * el modificador de 18.3 quiere evitar.
 *
 * @param lackOfTimeContext  opcional, solo con LACK_OF_TIME (DISENO_ADAPTACION_CONTEXTUAL v0.3)
 * @param availableMinutes   opcional, solo con MENOS_TIEMPO_HABITUAL (15 a 180)
 */
public record SkipPlannedWorkoutCommand(Long userId, Long plannedWorkoutId, SkipReason reason,
                                        ContextoFaltaDeTiempo lackOfTimeContext, Integer availableMinutes) {

    public SkipPlannedWorkoutCommand(Long userId, Long plannedWorkoutId, SkipReason reason) {
        this(userId, plannedWorkoutId, reason, null, null);
    }
}
