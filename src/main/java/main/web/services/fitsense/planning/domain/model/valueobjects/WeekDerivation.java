package main.web.services.fitsense.planning.domain.model.valueobjects;

import java.util.List;

/**
 * V31. El resultado de derivar una semana de la anterior.
 *
 * @param workouts             las sesiones nuevas, cada una con su origen.
 * @param sourcesWithoutDerived las sesiones anteriores que NO tienen derivada.
 *                              Ocurre cuando la semana nueva tiene menos dias
 *                              -una reduccion de dias-, y se devuelve explicito
 *                              en vez de quedar implicito en una resta, porque
 *                              la validacion de conservacion necesita distinguir
 *                              "esta sesion se elimino por orden" de "esta
 *                              sesion deberia estar y falta".
 */
public record WeekDerivation(List<DerivedWorkout> workouts, List<Long> sourcesWithoutDerived) {

    public WeekDerivation {
        workouts = workouts == null ? List.of() : List.copyOf(workouts);
        sourcesWithoutDerived = sourcesWithoutDerived == null
                ? List.of() : List.copyOf(sourcesWithoutDerived);
    }

    /** La sesion derivada de un origen dado, si sobrevivio. */
    public java.util.Optional<DerivedWorkout> from(Long sourcePlannedWorkoutId) {
        return workouts.stream()
                .filter(d -> d.sourcePlannedWorkoutId().equals(sourcePlannedWorkoutId))
                .findFirst();
    }

    public boolean conservaTodas() {
        return sourcesWithoutDerived.isEmpty();
    }
}
