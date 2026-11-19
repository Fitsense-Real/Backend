package main.web.services.fitsense.planning.domain.model.valueobjects;

/**
 * Las banderas de seguridad de un ejercicio del catalogo, vistas desde
 * planning. Sirven para decir, con causa, si un ejercicio YA PRESCRITO choca
 * con las restricciones actuales del participante.
 */
public record ExerciseSafetyFacts(
        Long exerciseId,
        boolean active,
        boolean highImpact,
        boolean requiresFloor,
        boolean axialLoad,
        boolean requiresKneeling,
        boolean requiresHanging,
        boolean requiresOverhead,
        boolean requiresInversion,
        /** V53: apoyo suspendido; lo excluye la misma restriccion que colgarse (decision de significado). */
        boolean requiresSuspendedSupport
) {
    /** Compatibilidad: sin el dato de V53. */
    public ExerciseSafetyFacts(Long exerciseId, boolean active, boolean highImpact, boolean requiresFloor,
                               boolean axialLoad, boolean requiresKneeling, boolean requiresHanging,
                               boolean requiresOverhead, boolean requiresInversion) {
        this(exerciseId, active, highImpact, requiresFloor, axialLoad, requiresKneeling, requiresHanging,
                requiresOverhead, requiresInversion, false);
    }
}
