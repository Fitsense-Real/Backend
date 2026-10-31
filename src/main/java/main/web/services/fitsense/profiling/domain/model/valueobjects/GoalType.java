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

    /**
     * P-01 DEFINITIVA (orden de cierre del PR #16, 31/10/2026): los cuatro objetivos admiten exactamente el mismo
     * rango de minutos por sesion, 30 a 120, configurado por la persona. Sustituye a la version anterior
     * (15 en GAIN_MUSCLE e INCREASE_STRENGTH, 30 en LOSE_WEIGHT y GENERAL_FITNESS, 180 de maximo).
     * <p>
     * Es un limite de ADMISION de {@code session_minutes}, no un maximo de la duracion real: V4 sigue admitiendo
     * hasta {@code session_minutes x 1,15} (120 -> 138), y D8 sigue permitiendo una sesion legitimamente acortada
     * por debajo de 30. Tampoco garantiza que exista un plan factible para cualquier combinacion de perfil.
     */
    public static final int MIN_SESSION_MINUTES = 30;
    public static final int MAX_SESSION_MINUTES = 120;

    /** P-01: el minimo es el mismo para los cuatro objetivos; el metodo se conserva por claridad del contrato. */
    public int minSessionMinutes() {
        return MIN_SESSION_MINUTES;
    }

    /** Si una duracion declarada cabe en el rango admitido (30-120, igual para todos los objetivos). */
    public boolean admiteMinutos(int minutos) {
        return minutos >= MIN_SESSION_MINUTES && minutos <= MAX_SESSION_MINUTES;
    }

    /** Solo estas dos usan target_weight_kg. */
    public boolean usesTargetWeight() {
        return this == LOSE_WEIGHT || this == GAIN_MUSCLE;
    }
}
