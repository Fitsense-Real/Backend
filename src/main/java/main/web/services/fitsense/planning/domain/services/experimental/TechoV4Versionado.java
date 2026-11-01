package main.web.services.fitsense.planning.domain.services.experimental;

/**
 * V4 VERSIONADO (EXPERIMENTAL, NO CABLEADO). Formas de interpretar "ninguna sesion excede M + 15 %".
 * <ul>
 *   <li>{@link #HISTORICO}: la del validador vigente. Minutos DECLARADOS (redondeados al minuto por el estimador) frente
 *       a {@code Math.round(M x 1,15)} ({@code PlanningProfile.maxSessionMinutes}). Se conserva para los planes ya
 *       publicados.</li>
 *   <li>{@link #TRUNCADO} (V4-B): minutos declarados frente a {@code floor(M x 1,15)}, el techo que ya expone el perfil
 *       ({@code UserProfile.maxSessionMinutesAllowed}).</li>
 *   <li>{@link #SEGUNDOS} (V4-C): segundos estimados, sin redondear, frente a {@code M x 60 x 1,15}, en aritmetica
 *       entera ({@code segundos x 100 <= M x 60 x 115}).</li>
 * </ul>
 * Los minutos declarados son {@code max(1, round(segundos / 60))}, igual que {@code SessionDurationEstimator}.
 */
public enum TechoV4Versionado {
    HISTORICO, TRUNCADO, SEGUNDOS;

    public static final int TOLERANCIA_PCT = 15;

    /** Techo en minutos de la version (para mostrar). En SEGUNDOS es el limite exacto en segundos / 60. */
    public double techoMinutos(int minutos) {
        return switch (this) {
            case HISTORICO -> Math.round(minutos * 1.15);
            case TRUNCADO -> Math.floor(minutos * 1.15);
            case SEGUNDOS -> minutos * 60L * (100 + TOLERANCIA_PCT) / 100.0 / 60.0;
        };
    }

    /** Limite en segundos exacto de la tolerancia (sin redondeos). */
    public static long limiteSegundosExacto(int minutos) {
        return minutos * 60L * (100 + TOLERANCIA_PCT) / 100;
    }

    public static int minutosDeclarados(long segundos) {
        return (int) Math.max(1, Math.round(segundos / 60.0));
    }

    public boolean admite(long segundosEstimados, int minutos) {
        return switch (this) {
            case HISTORICO -> minutosDeclarados(segundosEstimados) <= (int) Math.round(minutos * 1.15);
            case TRUNCADO -> minutosDeclarados(segundosEstimados) <= (int) Math.floor(minutos * 1.15);
            case SEGUNDOS -> segundosEstimados * 100 <= minutos * 60L * (100 + TOLERANCIA_PCT);
        };
    }

    /** Mayor duracion en segundos que la version acepta para M minutos (busqueda directa; M en 1..300). */
    public long maximoAdmitido(int minutos) {
        long hi = minutos * 60L * 2;
        long s = 0;
        for (long x = 0; x <= hi; x++) if (admite(x, minutos)) s = x;
        return s;
    }
}
