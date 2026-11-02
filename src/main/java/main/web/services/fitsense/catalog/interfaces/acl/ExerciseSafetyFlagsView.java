package main.web.services.fitsense.catalog.interfaces.acl;

/**
 * Las banderas de seguridad de UN ejercicio (V10, V25), tal como estan en el
 * catalogo, activo o no.
 * <p>
 * Existe para comprobar un ejercicio YA PRESCRITO contra las restricciones
 * actuales del participante, con la causa exacta. El conjunto elegible no
 * sirve para eso: dice que un ejercicio no esta, pero no por que.
 */
public record ExerciseSafetyFlagsView(
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
