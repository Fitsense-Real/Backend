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
     * O20 (opcion A): por cada ejercicio pedido, los ids de su familia de movimiento equivalente
     * ({@link main.web.services.fitsense.catalog.domain.services.FamiliaDeMovimientoEquivalente}), activos o no, SIN
     * incluir el propio. Un id que no existe o no tiene variantes no aparece.
     */
    java.util.Map<Long, java.util.Set<Long>> handle(
            main.web.services.fitsense.catalog.domain.model.queries.GetEquivalentVariantsQuery query);
}
