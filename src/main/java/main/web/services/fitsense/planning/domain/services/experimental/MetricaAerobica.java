package main.web.services.fitsense.planning.domain.services.experimental;

import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.services.CapabilityPrescriptionPolicy;

import java.util.Locale;
import java.util.Map;

/**
 * METRICA AEROBICA (EXPERIMENTAL, INFORMATIVA, NO CABLEADA). Separa lo que FitSense PROGRAMA de lo que la persona
 * EJECUTA y de lo que hace FUERA de la app, y no inventa intensidad:
 * <ul>
 *   <li>{@code minutosTrabajoProgramados}: series x segundos de los ejercicios de clase CARDIO del plan.</li>
 *   <li>{@code minutosBloqueProgramados}: lo anterior mas las pausas entre series de cardio (el bloque completo).</li>
 *   <li>{@code minutosEjecutados} y {@code minutosExternos}: solo si se registran; null si no hay dato.</li>
 *   <li>{@code minutosEquivalentes}: SOLO con intensidad conocida (moderada x1, vigorosa x2, OMS 2020); con intensidad
 *       DESCONOCIDA es null, nunca se supone.</li>
 * </ul>
 * El aviso es informativo y nunca dice que la persona incumple una recomendacion ni promete perdida de peso.
 */
public record MetricaAerobica(double minutosTrabajoProgramados, double minutosBloqueProgramados, Double minutosEjecutados,
                              Double minutosExternos, Intensidad intensidad, Double minutosEquivalentes) {

    public enum Intensidad { DESCONOCIDA, MODERADA, VIGOROSA }

    public static MetricaAerobica programada(PlanDraft d, Map<Long, CandidateExercise> elegibles) {
        double trabajo = 0, bloque = 0;
        for (var w : d.workouts())
            for (var e : w.exercises()) {
                var c = elegibles.get(e.exerciseId());
                if (c == null || CapabilityPrescriptionPolicy.claseDe(c) != CapabilityPrescriptionPolicy.Clase.CARDIO) continue;
                int series = e.plannedSets() == null ? 1 : e.plannedSets();
                int seg = e.plannedDurationSeconds() == null ? 0 : e.plannedDurationSeconds();
                int pausa = e.restSeconds() == null ? 0 : e.restSeconds();
                trabajo += series * seg / 60.0;
                bloque += (series * seg + Math.max(0, series - 1) * pausa) / 60.0;
            }
        return new MetricaAerobica(trabajo, bloque, null, null, Intensidad.DESCONOCIDA, null);
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

    /** Texto informativo. No afirma incumplimiento ni promete resultados. */
    public String aviso() {
        return String.format(Locale.ROOT, "Tu plan incluye unos %.0f minutos de actividad aerobica esta semana. "
                + "La actividad que realizas fuera de FitSense (caminar, ir en bicicleta, hacer deporte) tambien cuenta.",
                minutosBloqueProgramados);
    }
}
