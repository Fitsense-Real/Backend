package main.web.services.fitsense.catalog.interfaces.acl;

/**
 * Vista de un ejercicio para los demas contextos.
 * <p>
 * Lleva lo que el generador necesita para elegir y lo que la app necesita para
 * pintarlo. Las instrucciones NO viajan: multiplicarian el tamano del contexto
 * enviado a la IA sin cambiar la seleccion, y la app las pide aparte con
 * GET /api/v1/exercises/{id}.
 */
public record EligibleExerciseView(
        Long exerciseId,
        String nameEs,
        String bodyPartCode,
        String equipmentCode,
        String targetMuscle,
        short difficultyLevel,
        String defaultPrescription,
        String gifPath,
        String imagePath,
        String mediaAttribution,
        /**
         * Patron de movimiento del Step 3A (V26), una de 24 clases cerradas.
         * <p>
         * Viaja porque planificacion lo necesita para la cobertura de los cuatro
         * slots de 4C, y es dato del catalogo: traducirlo a slots es cosa de
         * planning (ver {@code MandatorySlot}), no del catalogo. El catalogo no
         * sabe que es un slot y es mejor que siga sin saberlo.
         * <p>
         * {@code null} solo en inactivos, que el conjunto elegible no trae.
         */
        String movementPattern,
        /**
         * {@code exercises.high_impact} (V10): saltos, pliometria, levantamientos olimpicos. Viaja para P-C2 (una
         * sustitucion "mas sencilla" no puede subir el impacto). {@code null} si el origen no lo informa.
         */
        Boolean highImpact,
        /** V26 is_isolation (auditoria v46, P15-C experimental). {@code null} si el origen no lo informa. */
        Boolean isolation,
        /** V51 impact_level explicito (BAJO, MODERADO, ALTO). {@code null} si el origen no lo informa. */
        String impactLevel,
        /** V51 is_stretch explicito. {@code null} si el origen no lo informa. */
        Boolean stretch
) {
    /** Compatibilidad: sin los atributos explicitos de V51. */
    public EligibleExerciseView(Long exerciseId, String nameEs, String bodyPartCode, String equipmentCode,
                                String targetMuscle, short difficultyLevel, String defaultPrescription,
                                String gifPath, String imagePath, String mediaAttribution, String movementPattern,
                                Boolean highImpact, Boolean isolation) {
        this(exerciseId, nameEs, bodyPartCode, equipmentCode, targetMuscle, difficultyLevel, defaultPrescription,
                gifPath, imagePath, mediaAttribution, movementPattern, highImpact, isolation, null, null);
    }

    /** Compatibilidad: sin el dato de aislamiento. */
    public EligibleExerciseView(Long exerciseId, String nameEs, String bodyPartCode, String equipmentCode,
                                String targetMuscle, short difficultyLevel, String defaultPrescription,
                                String gifPath, String imagePath, String mediaAttribution, String movementPattern,
                                Boolean highImpact) {
        this(exerciseId, nameEs, bodyPartCode, equipmentCode, targetMuscle, difficultyLevel, defaultPrescription,
                gifPath, imagePath, mediaAttribution, movementPattern, highImpact, null);
    }

    /** Compatibilidad: sin el dato de impacto. */
    public EligibleExerciseView(Long exerciseId, String nameEs, String bodyPartCode, String equipmentCode,
                                String targetMuscle, short difficultyLevel, String defaultPrescription,
                                String gifPath, String imagePath, String mediaAttribution, String movementPattern) {
        this(exerciseId, nameEs, bodyPartCode, equipmentCode, targetMuscle, difficultyLevel, defaultPrescription,
                gifPath, imagePath, mediaAttribution, movementPattern, null);
    }

    public boolean isDurationBased() {
        return "DURATION".equals(defaultPrescription);
    }

    /**
     * Constructor de compatibilidad para los usos que no necesitan el patron
     * (detalle con media para pintar el plan, nombres). Evita tocar llamadas
     * que no tienen nada que ver con 4C.
     */
    public EligibleExerciseView(Long exerciseId, String nameEs, String bodyPartCode,
                                String equipmentCode, String targetMuscle, short difficultyLevel,
                                String defaultPrescription, String gifPath, String imagePath,
                                String mediaAttribution) {
        this(exerciseId, nameEs, bodyPartCode, equipmentCode, targetMuscle, difficultyLevel,
                defaultPrescription, gifPath, imagePath, mediaAttribution, null);
    }
}
