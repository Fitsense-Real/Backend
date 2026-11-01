package main.web.services.fitsense.planning.domain.services;

import java.util.List;
import java.util.Locale;

/**
 * METRICA AEROBICA INFORMATIVA (funcionalidad experimental, desactivada por defecto en la API). Separa lo PROGRAMADO,
 * lo EJECUTADO y lo EXTERNO, y no inventa intensidad:
 * <ul>
 *   <li>{@code minutosTrabajoProgramados}: series x segundos de los ejercicios de cardio del plan;</li>
 *   <li>{@code minutosBloqueProgramados}: lo anterior MAS las pausas entre series. Las pausas NO se cuentan como
 *       minutos aerobicos efectivos: el bloque solo dice cuanto ocupa en la sesion;</li>
 *   <li>{@code minutosEjecutados} y {@code minutosExternos}: solo con dato registrado; null si no lo hay;</li>
 *   <li>{@code minutosEquivalentes}: solo con intensidad conocida (moderada x1, vigorosa x2). Con intensidad
 *       DESCONOCIDA es null: nunca se supone.</li>
 * </ul>
 * El aviso nunca dice que la persona incumple una recomendacion ni promete perdida de peso.
 */
public record MetricaAerobica(double minutosTrabajoProgramados, double minutosBloqueProgramados,
                              Double minutosEjecutados, Double minutosExternos, Intensidad intensidad,
                              Double minutosEquivalentes) {

    public enum Intensidad { DESCONOCIDA, MODERADA, VIGOROSA }

    /** Un bloque de cardio del plan: series, segundos por serie y pausa entre series. */
    public record Bloque(int series, int segundos, int pausa) {}

    public static MetricaAerobica programada(List<Bloque> bloques) {
        double trabajo = 0, bloque = 0;
        for (var b : bloques) {
            int series = Math.max(1, b.series());
            trabajo += series * b.segundos() / 60.0;
            bloque += (series * b.segundos() + Math.max(0, series - 1) * Math.max(0, b.pausa())) / 60.0;
        }
        return new MetricaAerobica(redondear(trabajo), redondear(bloque), null, null, Intensidad.DESCONOCIDA, null);
    }

    /** Con datos registrados. Los equivalentes solo se calculan si la intensidad es conocida. */
    public MetricaAerobica conRegistro(Double ejecutados, Double externos, Intensidad i) {
        Double eq = null;
        if (i != null && i != Intensidad.DESCONOCIDA && ejecutados != null) {
            double total = ejecutados + (externos == null ? 0 : externos);
            eq = i == Intensidad.VIGOROSA ? total * 2 : total;
        }
        return new MetricaAerobica(minutosTrabajoProgramados, minutosBloqueProgramados, ejecutados, externos,
                i == null ? Intensidad.DESCONOCIDA : i, eq);
    }

    public String aviso() {
        return String.format(Locale.ROOT, "Tu plan incluye unos %.0f minutos de actividad aerobica esta semana. "
                + "La actividad que realizas fuera de FitSense (caminar, ir en bicicleta, hacer deporte) tambien cuenta.",
                minutosTrabajoProgramados);
    }

    private static double redondear(double x) {
        return Math.round(x * 10) / 10.0;
    }
}
