package main.web.services.fitsense.planning.domain.services;

/**
 * P-01 en planificacion: el rango de minutos por sesion admitido, 30 a 75, IGUAL para los cuatro objetivos
 * (version definitiva, orden de fusion del PR #16).
 * <p>
 * El perfil ya lo exige al darse de alta ({@code GoalType} en profiling). Aqui se repite para los perfiles
 * HISTORICOS, congelados antes de P-01: no se modifican, pero tampoco se genera para ellos un plan nuevo, ni se
 * llama a la IA. La duplicacion es deliberada (contextos separados por ACL);
 * {@code PoliticaP01DuracionMinimaTest} comprueba que las dos tablas coinciden. Dominio puro.
 * <p>
 * No es la duracion real maxima: V4 admite hasta {@code minutos x 1,15}, y D8 permite sesiones acortadas.
 */
public final class DuracionPorObjetivoPolicy {

    public static final int MINIMO = 30;
    public static final int MAXIMO = 75;

    private DuracionPorObjetivoPolicy() {}

    /** Minimo de P-01: el mismo para cualquier objetivo. */
    public static int minimo(String goalType) {
        return MINIMO;
    }

    public static boolean admite(String goalType, int minutos) {
        return minutos >= MINIMO && minutos <= MAXIMO;
    }
}
