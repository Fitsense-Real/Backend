package main.web.services.fitsense.planning.domain.services;

/**
 * P-01 en planificacion: el rango de minutos por sesion admitido para cada objetivo.
 * <p>
 * El perfil ya lo exige al darse de alta ({@code GoalType.minSessionMinutes()} en profiling).
 * Aqui se repite para los perfiles HISTORICOS, congelados antes de P-01: no se modifican, pero
 * tampoco se genera para ellos un plan que nunca pudo ser valido, ni se llama a la IA. La
 * duplicacion es deliberada (contextos separados por ACL); {@code DuracionPorObjetivoPolicyTest}
 * comprueba que las dos tablas coinciden. Dominio puro.
 */
public final class DuracionPorObjetivoPolicy {

    public static final int MAXIMO = 180;

    private DuracionPorObjetivoPolicy() {}

    /** Minimo de P-01; 15 para un objetivo desconocido (el minimo historico del perfil). */
    public static int minimo(String goalType) {
        return switch (String.valueOf(goalType)) {
            case "LOSE_WEIGHT", "GENERAL_FITNESS" -> 30;
            default -> 15;
        };
    }

    public static boolean admite(String goalType, int minutos) {
        return minutos >= minimo(goalType) && minutos <= MAXIMO;
    }
}
