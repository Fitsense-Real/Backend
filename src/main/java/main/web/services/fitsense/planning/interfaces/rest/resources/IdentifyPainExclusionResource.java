package main.web.services.fitsense.planning.interfaces.rest.resources;

import jakarta.validation.constraints.NotNull;

/**
 * O20: la accion explicita de identificar DESPUES que ejercicio de una sesion con dolor fue el que dolio.
 * {@code confirmPain} tiene que ser true. {@code plannedWorkoutId} es opcional: la sesion de la que se habla; sin el,
 * se confirman todas las filas aun sin identificar de ese ejercicio.
 */
public record IdentifyPainExclusionResource(@NotNull Boolean confirmPain, Long plannedWorkoutId) {}
