package main.web.services.fitsense.planning.domain.model.valueobjects;

/**
 * V31. Una sesion del plan derivado, con la identidad de la sesion anterior de
 * la que proviene.
 * <p>
 * Es un envoltorio exclusivo del flujo de adaptacion, a proposito: asi
 * {@link PlanDraft} y {@link PlanDraft.DraftWorkout} quedan intactos. Entre los
 * dos tienen diecisiete sitios de construccion en el modulo de planificacion, y
 * anadirles un componente habria obligado a tocarlos todos mas los de las
 * pruebas, que es como se rompio la suite con el parche de las banderas
 * {@code allow_*}.
 * <p>
 * La relacion se construye AL COPIAR la sesion anterior. No se deduce al final
 * sobre un plan generado desde cero, y por eso sigue siendo valida aunque
 * cambien la fecha, el orden y el enfoque.
 *
 * @param sourcePlannedWorkoutId de que sesion anterior proviene. Nunca nulo:
 *                               una sesion sin origen no es derivada y no se
 *                               envuelve en esto.
 */
public record DerivedWorkout(Long sourcePlannedWorkoutId, PlanDraft.DraftWorkout workout) {

    public DerivedWorkout {
        if (sourcePlannedWorkoutId == null)
            throw new IllegalArgumentException(
                    "Una sesion derivada tiene que declarar de cual proviene.");
        if (workout == null)
            throw new IllegalArgumentException("La sesion derivada no puede ser nula.");
    }
}
