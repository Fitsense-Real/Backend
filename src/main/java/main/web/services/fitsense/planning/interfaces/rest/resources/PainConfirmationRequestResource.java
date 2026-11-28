package main.web.services.fitsense.planning.interfaces.rest.resources;

import java.time.LocalDate;
import java.util.List;

/**
 * O20 (cierre final): una sesion con dolor SIN IDENTIFICAR que espera la confirmacion de la persona: que ejercicio fue
 * y cuales no tuvieron que ver. Mientras no conteste, todo lo de la sesion sigue excluido.
 *
 * @param confirmationRequired true cuando ya pasaron dos semanas con plan: es el momento de pedirla de forma activa.
 * @param exercises            los ejercicios de la sesion aun sin confirmar (excluidos).
 */
public record PainConfirmationRequestResource(Long plannedWorkoutId, LocalDate eventDate, boolean confirmationRequired,
                                              List<Exercise> exercises) {
    public record Exercise(Long exerciseId, String exerciseName) {}
}
