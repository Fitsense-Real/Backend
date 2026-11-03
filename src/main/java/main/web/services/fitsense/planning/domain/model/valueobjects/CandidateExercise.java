package main.web.services.fitsense.planning.domain.model.valueobjects;

/**
 * Un ejercicio del conjunto elegible, traducido al lenguaje de planning.
 * <p>
 * No se usa el tipo del catalogo a proposito: si manana el catalogo agrega
 * campos, el cambio se absorbe en ExternalCatalogService y no llega al
 * generador ni al validador.
 */
public record CandidateExercise(
        Long exerciseId,
        String name,
        String bodyPartCode,
        String equipmentCode,
        int difficulty,
        PrescriptionType defaultPrescription,
        /**
         * Musculo objetivo del catalogo (biceps, triceps, pectorals...). Separa lo
         * que body_part mezcla: "upper arms" es tanto biceps como triceps, y
         * PUSH solo admite triceps y PULL solo biceps.
         */
        String targetMuscle,

        /**
         * Patron de movimiento del Step 3A (V26). Es lo que decide a que slot de
         * 4C pertenece el ejercicio; la traduccion esta en {@code MandatorySlot}.
         * <p>
         * Es un dato DISTINTO de bodyPartCode y no lo sustituye: «upper legs»
         * mezcla sentadilla (KNEE_DOMINANT) con peso muerto (HIP_HINGE), que son
         * dos slots obligatorios diferentes, y «back» mezcla remo
         * (HORIZONTAL_PULL) con hiperextension (TRUNK_EXTENSION), que no es
         * ningun slot. Por eso la cobertura de 4C no se puede calcular con la
         * zona corporal, que es lo que ya usan V15 y el selector de reglas.
         * <p>
         * {@code null} posible: ejercicio sin clasificar. No se interpreta como
         * «no cuenta para ningun slot» sino que se cuenta aparte, porque son dos
         * cosas distintas y confundirlas infla la cobertura.
         */
        String movementPattern
) {
    public CandidateExercise(Long exerciseId, String name, String bodyPartCode, String equipmentCode,
                             int difficulty, PrescriptionType defaultPrescription) {
        this(exerciseId, name, bodyPartCode, equipmentCode, difficulty, defaultPrescription,
                null, null);
    }

    public CandidateExercise(Long exerciseId, String name, String bodyPartCode, String equipmentCode,
                             int difficulty, PrescriptionType defaultPrescription,
                             String targetMuscle) {
        this(exerciseId, name, bodyPartCode, equipmentCode, difficulty, defaultPrescription,
                targetMuscle, null);
    }
}