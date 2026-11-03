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
        boolean requiresInversion
) {}
