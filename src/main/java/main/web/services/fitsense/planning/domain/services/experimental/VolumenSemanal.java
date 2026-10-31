package main.web.services.fitsense.planning.domain.services.experimental;

import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.model.valueobjects.PrescriptionType;
import main.web.services.fitsense.planning.domain.services.CapabilityPrescriptionPolicy;

import java.time.LocalDate;
import java.util.*;

/**
 * MODELO E (EXPERIMENTAL). Contabiliza el volumen de una semana: series DIRECTAS por grupo muscular ({@code body_part})
 * y sesion, por grupo y semana, y minutos de cardio. Dominio puro, sin estado.
 * <p>
 * Limitaciones declaradas:
 * <ul>
 *   <li>Solo series directas: el catalogo no registra musculos secundarios, asi que no se puede aplicar el conteo
 *       fraccional (0,5 por serie indirecta) que mejor predice en Pelland et al. (2026).</li>
 *   <li>Cuenta las series de SETS_REPS de ejercicios de trabajo (ni complementos ni DURATION); el cardio se mide en
 *       minutos de trabajo (series x segundos), sin intensidad: no hay columna para medirla.</li>
 * </ul>
 */
public final class VolumenSemanal {

    public record Resultado(Map<LocalDate, Map<String, Integer>> porSesion, Map<String, Integer> porSemana,
                            double minutosCardio) {

        public int maximoPorGrupoYSesion() {
            return porSesion.values().stream().flatMap(m -> m.values().stream()).mapToInt(i -> i).max().orElse(0);
        }

        public int semanal(String grupo) {
            return porSemana.getOrDefault(grupo, 0);
        }
    }

    private VolumenSemanal() {}

    public static Resultado medir(PlanDraft draft, Map<Long, CandidateExercise> elegibles, Set<Long> complementos) {
        var porSesion = new LinkedHashMap<LocalDate, Map<String, Integer>>();
        var semana = new TreeMap<String, Integer>();
        double cardio = 0;
        for (var w : draft.workouts()) {
            var sesion = new TreeMap<String, Integer>();
            for (var e : w.exercises()) {
                var c = elegibles.get(e.exerciseId());
                if (c == null) continue;
                int series = e.plannedSets() == null ? 1 : e.plannedSets();
                if (CapabilityPrescriptionPolicy.claseDe(c) == CapabilityPrescriptionPolicy.Clase.CARDIO) {
                    cardio += series * (e.plannedDurationSeconds() == null ? 0 : e.plannedDurationSeconds()) / 60.0;
                    continue;
                }
                if (complementos.contains(c.exerciseId()) || e.prescriptionType() != PrescriptionType.SETS_REPS) continue;
                sesion.merge(c.bodyPartCode(), series, Integer::sum);
                semana.merge(c.bodyPartCode(), series, Integer::sum);
            }
            porSesion.merge(w.scheduledDate(), sesion, (a, b) -> {
                b.forEach((g, n) -> a.merge(g, n, Integer::sum));
                return a;
            });
        }
        return new Resultado(porSesion, semana, cardio);
    }
}
