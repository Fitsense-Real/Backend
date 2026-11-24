package main.web.services.fitsense.catalog.domain.services;

import main.web.services.fitsense.catalog.domain.model.aggregates.Exercise;
import main.web.services.fitsense.catalog.domain.model.queries.GetActiveExercisesQuery;
import main.web.services.fitsense.catalog.domain.model.queries.GetEligibleExercisesQuery;
import main.web.services.fitsense.catalog.domain.model.queries.GetExerciseByIdQuery;

import java.util.List;
import java.util.Optional;

public interface ExerciseQueryService {
    List<Exercise> handle(GetActiveExercisesQuery query);
    List<Exercise> handle(GetEligibleExercisesQuery query);
    Optional<Exercise> handle(GetExerciseByIdQuery query);

    /**
     * O20: por cada ejercicio pedido, sus equivalentes (variantes del mismo ejercicio y, si esta en la lista explicita
     * de {@link main.web.services.fitsense.catalog.domain.services.FamiliaDeMovimientoEquivalente}, los de su grupo),
     * activos o no, SIN incluir el propio, cada uno con su nivel y justificacion. Sin equivalentes, no aparece.
     */
    java.util.Map<Long, java.util.List<main.web.services.fitsense.catalog.domain.model.valueobjects.EquivalentExercise>> handle(
            main.web.services.fitsense.catalog.domain.model.queries.GetEquivalentVariantsQuery query);
}
