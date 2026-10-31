package main.web.services.fitsense.profiling.domain.model.valueobjects;

/**
 * Cuatro categorias. Las metas de carrera quedan fuera del MVP porque el
 * catalogo no tiene ejercicios de carrera continua ni intervalos (seccion 2.6).
 */
public enum GoalType {
    LOSE_WEIGHT,
    GAIN_MUSCLE,
    INCREASE_STRENGTH,
    GENERAL_FITNESS;

    /** P-01: duracion maxima por sesion, igual para los cuatro objetivos. */
    public static final int MAX_SESSION_MINUTES = 180;

    /**
     * P-01 (politica aprobada, 31/10/2026): duracion MINIMA por sesion segun el objetivo.
     * <p>
     * Es un limite de ADMISION del perfil, no una garantia de factibilidad: un perfil admitido
     * puede seguir sin plan por su equipamiento, sus restricciones o sus dias. Se fijo despues de
     * la campana de estres, donde GENERAL_FITNESS y LOSE_WEIGHT a 15 min no caben en V4 con la
     * composicion que exigen V15, V20 y V25. Los validadores de planificacion no cambian.
     */
    public int minSessionMinutes() {
        return switch (this) {
            case GAIN_MUSCLE, INCREASE_STRENGTH -> 15;
            case LOSE_WEIGHT, GENERAL_FITNESS -> 30;
        };
    }

    /** Si una duracion declarada cabe en el rango admitido para este objetivo. */
    public boolean admiteMinutos(int minutos) {
        return minutos >= minSessionMinutes() && minutos <= MAX_SESSION_MINUTES;
    }

    /** Solo estas dos usan target_weight_kg. */
    public boolean usesTargetWeight() {
        return this == LOSE_WEIGHT || this == GAIN_MUSCLE;
    }
}
