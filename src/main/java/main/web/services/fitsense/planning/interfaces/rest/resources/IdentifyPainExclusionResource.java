package main.web.services.fitsense.planning.interfaces.rest.resources;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * O20: la accion explicita de identificar DESPUES que ejercicio de una sesion con dolor fue el que dolio.
 * {@code confirmPain} tiene que ser true. {@code plannedWorkoutId} es opcional: la sesion de la que se habla; sin el,
 * se confirman todas las filas aun sin identificar de ese ejercicio. {@code unrelatedExerciseIds} es opcional: los
 * demas ejercicios de esa sesion que la persona confirma que NO tuvieron que ver; solo esos se levantan.
 */
public record IdentifyPainExclusionResource(@NotNull Boolean confirmPain, Long plannedWorkoutId,
                                            @Size(max = 50) List<Long> unrelatedExerciseIds) {}
