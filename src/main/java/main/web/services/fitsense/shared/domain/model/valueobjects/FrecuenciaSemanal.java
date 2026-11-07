package main.web.services.fitsense.shared.domain.model.valueobjects;

/**
 * S-MAX · CONTRATO DE DISPONIBILIDAD Y FRECUENCIA (cierre de PLAN-REV-2.0, 05/11/2026). Una sola fuente para las tres
 * cantidades que antes se calculaban por separado (generacion, primera semana y base de REDUCE_DAYS):
 * <ul>
 *   <li><b>Disponibilidad</b> ({@code available_days}): los dias de la semana en que la persona PUEDE entrenar.</li>
 *   <li><b>Frecuencia pedida</b> ({@code days_per_week}): cuantas veces QUIERE entrenar; el perfil exige
 *       {@code days_per_week <= |available_days|} ({@code UserProfile}).</li>
 *   <li><b>Frecuencia planificada</b>: las sesiones que el sistema PROGRAMA =
 *       {@code min(days_per_week, tope del nivel, |available_days|)}. Tope del nivel: principiante 4, intermedio y
 *       avanzado 6; nadie recibe 7 sesiones de fuerza sin descanso (ACSM 2009: 2-3 dias para principiantes).</li>
 * </ul>
 * La adherencia se mide contra las sesiones PROGRAMADAS (WeeklyMetricsCalculator), no contra los dias disponibles:
 * con 5 dias disponibles y 4 sesiones programadas, completar las 4 es el 100 %. Una frecuencia menor solo es legitima
 * por una orden explicita de la adaptacion (REDUCE_DAYS, {@code forcedDaysPerWeek}). Los topes NO cambian aqui.
 */
public final class FrecuenciaSemanal {

    public static final int TOPE_PRINCIPIANTE = 4;
    public static final int TOPE_RESTO = 6;

    private FrecuenciaSemanal() {
    }

    /** Tope de sesiones semanales del nivel (S-MAX). */
    public static int tope(String nivel) {
        return "BEGINNER".equals(nivel) ? TOPE_PRINCIPIANTE : TOPE_RESTO;
    }

    /** Frecuencia pedida con el tope del nivel (sin mirar los dias disponibles). */
    public static int conTope(int diasPorSemana, String nivel) {
        return Math.min(diasPorSemana, tope(nivel));
    }

    /** Sesiones que se programan: {@code min(dias pedidos, tope del nivel, dias disponibles)}. */
    public static int planificadas(int diasPorSemana, String nivel, int diasDisponibles) {
        return Math.min(conTope(diasPorSemana, nivel), diasDisponibles);
    }

    /** ¿El tope del nivel recorta lo que la persona pidio? (para avisarlo, no para cambiarlo). */
    public static boolean recortaPorNivel(int diasPorSemana, String nivel) {
        return diasPorSemana > tope(nivel);
    }
}
