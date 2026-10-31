package main.web.services.fitsense.planning.domain.services.experimental;

import main.web.services.fitsense.configuration.domain.model.valueobjects.PrescriptionParams;
import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.model.valueobjects.WorkoutFocus;
import main.web.services.fitsense.planning.domain.services.SessionDurationEstimator;

import java.util.*;

/**
 * MODELO E (EXPERIMENTAL, NO CABLEADO). Validador CANDIDATO de las reglas de volumen del modelo E. No sustituye ni se
 * llama desde {@code PlanDraftValidator}: solo lo usan el laboratorio y sus pruebas. Devuelve hallazgos; no lanza.
 * <ul>
 *   <li>E1 · techo de tiempo de planificacion ({@code fraccion x M}); la tolerancia de V4 no es un objetivo.</li>
 *   <li>E2 · series de un grupo en una sesion por encima del techo blando.</li>
 *   <li>E3 · series de un grupo en la semana por encima del techo.</li>
 *   <li>E4 · un grupo grande del enfoque de la semana por debajo del objetivo semanal escalado por dias.</li>
 * </ul>
 */
public final class ValidadorModeloE {

    public record Hallazgo(String codigo, String detalle) {}

    private static final SessionDurationEstimator ESTIMADOR = new SessionDurationEstimator();

    private ValidadorModeloE() {}

    public static List<Hallazgo> evaluar(PlanDraft draft, String goalType, int minutos, PrescriptionParams prescripcion,
                                         Map<Long, CandidateExercise> elegibles, Set<Long> complementos,
                                         ParametrosModeloE p) {
        var out = new ArrayList<Hallazgo>();
        long techo = p.techoSegundos(minutos);
        for (var w : draft.workouts()) {
            long s = ESTIMADOR.estimateSeconds(w, prescripcion);
            if (s > techo)
                out.add(new Hallazgo("E1", "%s: %d s sobre el techo de planificacion %d s".formatted(w.scheduledDate(), s,
                        techo)));
        }
        var vol = VolumenSemanal.medir(draft, elegibles, complementos);
        vol.porSesion().forEach((fecha, grupos) -> grupos.forEach((g, n) -> {
            if (n > p.techoSeriesGrupoSesion())
                out.add(new Hallazgo("E2", "%s: %d series de %s (techo blando %d)".formatted(fecha, n, g,
                        p.techoSeriesGrupoSesion())));
        }));
        vol.porSemana().forEach((g, n) -> {
            if (n > p.techoSeriesGrupoSemana())
                out.add(new Hallazgo("E3", "%d series semanales de %s (techo %d)".formatted(n, g, p.techoSeriesGrupoSemana())));
        });
        var entrenables = new TreeSet<String>();
        for (var w : draft.workouts())
            for (var g : w.focus() == WorkoutFocus.FULL_BODY ? p.gruposGrandes() : new HashSet<>(w.focus().bodyPartCodes()))
                if (p.gruposGrandes().contains(g)) entrenables.add(g);
        int objetivo = p.objetivoSemanal(goalType, draft.workouts().size());
        for (var g : entrenables)
            if (vol.semanal(g) < objetivo)
                out.add(new Hallazgo("E4", "%d series semanales de %s; objetivo escalado %d".formatted(vol.semanal(g), g,
                        objetivo)));
        return out;
    }
}
