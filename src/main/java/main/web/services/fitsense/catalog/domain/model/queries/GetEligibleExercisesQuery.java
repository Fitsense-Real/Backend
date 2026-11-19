package main.web.services.fitsense.catalog.domain.model.queries;

import java.util.List;

/**
 * El conjunto elegible: lo unico que el generador puede proponer.
 * <p>
 * Si el generador devuelve un exercise_id fuera de este conjunto, la validacion
 * 1 de la seccion 19 rechaza el plan. Por eso las restricciones de seguridad se
 * aplican AQUI y no como instruccion en el prompt: la IA ni siquiera ve lo que
 * no le corresponde al participante.
 */
public record GetEligibleExercisesQuery(
        List<String> equipmentCodes,
        boolean excludeGymOnly,
        int maxDifficultyLevel,
        List<Long> blockedExerciseIds,

        /** Fuera saltos, pliometria y olimpicos. */
        boolean excludeHighImpact,

        /** Fuera lo que exige bajar al suelo y levantarse. Es barrera de movilidad. */
        boolean excludeFloorWork,

        /**
         * Fuera carga sobre columna o cuello. NO incluye los invertidos: para eso
         * esta excludeInversion. El comentario anterior decia "carga axial e
         * invertidos" y el SQL solo comprobaba axial_load; medido sobre el
         * catalogo activo, de los 33 ejercicios con requires_inversion ninguno
         * tiene axial_load, asi que la promesa no se cumplia en ningun caso.
         */
        boolean excludeAxialLoad,

        /** Fuera lo que exige apoyar las rodillas. Declarado por el perfil (V25). */
        boolean excludeKneeling,

        /** Fuera lo que exige suspenderse. Declarado por el perfil (V25). */
        boolean excludeHanging,

        /** Fuera lo que exige los brazos sobre la cabeza. Declarado por el perfil (V25). */
        boolean excludeOverhead,

        /** Fuera las posiciones con la cabeza por debajo del corazon (V25). */
        boolean excludeInversion
) {
    /** Sin restricciones de seguridad. Es el caso de la mayoria de perfiles. */
    public static GetEligibleExercisesQuery unrestricted(List<String> equipmentCodes,
                                                         boolean excludeGymOnly,
                                                         int maxDifficultyLevel,
                                                         List<Long> blockedExerciseIds) {
        return new GetEligibleExercisesQuery(equipmentCodes, excludeGymOnly, maxDifficultyLevel,
                blockedExerciseIds, false, false, false, false, false, false, false);
    }
}
