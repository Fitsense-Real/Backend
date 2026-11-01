package main.web.services.fitsense.planning.infrastructure.generation.experimental;

import main.web.services.fitsense.configuration.domain.model.valueobjects.PrescriptionParams;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.domain.services.CapabilityPrescriptionPolicy;
import main.web.services.fitsense.planning.domain.services.CatalogEligibilityPolicy;
import main.web.services.fitsense.planning.domain.services.SessionDurationEstimator;
import main.web.services.fitsense.planning.domain.services.TrainingPlanGenerator;
import main.web.services.fitsense.planning.domain.services.experimental.ParametrosModeloE;
import main.web.services.fitsense.planning.domain.services.experimental.VolumenSemanal;

import java.util.*;
import java.util.stream.Collectors;

/**
 * MODELO E (EXPERIMENTAL, NO CABLEADO). Constructor candidato de semana completa por volumen. No es un bean ni lo usa
 * la ruta de generacion, adaptacion o regeneracion: solo el laboratorio ({@code LaboratorioModeloEIT}) lo instancia, y
 * se niega a construir con parametros deshabilitados salvo en modo laboratorio declarado.
 * <ol>
 *   <li>Parte del motor de reglas VIGENTE con el piso de V20 a 0 (copia del contexto; la configuracion activa no se
 *       toca): conserva 4A, 4C, 4D, V15, V18, V22, V23 y V25 tal como estan.</li>
 *   <li>Mientras un grupo grande entrenable del enfoque este por debajo del objetivo semanal ESCALADO por dias, anade
 *       al dia con menos series de ese grupo un ejercicio de ese grupo con su DOSIS BASE aprobada (4D o
 *       {@link CapabilityPrescriptionPolicy}), sin repetir en el dia ni en el dia pegado, con V22, sin pasar del techo
 *       por grupo y sesion, del techo semanal ni del techo de tiempo ({@code fraccion x M}).</li>
 *   <li>No sube series, no alarga descansos y no anade nada para rellenar minutos.</li>
 * </ol>
 */
public final class ConstructorSemanaModeloE {

    private static final SessionDurationEstimator ESTIMADOR = new SessionDurationEstimator();

    private final TrainingPlanGenerator reglas;

    public ConstructorSemanaModeloE(TrainingPlanGenerator reglas) {
        this.reglas = reglas;
    }

    public PlanDraft construir(PlanGenerationContext ctx, int divisor, ParametrosModeloE p, boolean laboratorio) {
        if (!p.habilitado() && !laboratorio)
            throw new IllegalStateException("Modelo E " + p.version() + " deshabilitado: solo laboratorio.");
        var sinPiso = sinPisoV20(ctx);
        var base = reglas.generate(sinPiso, List.of());
        return extender(sinPiso, divisor, base, p);
    }

    /** Copia del contexto con {@code session_minutes_floor_pct = 0}: solo en memoria, nada persistente. */
    public static PlanGenerationContext sinPisoV20(PlanGenerationContext c) {
        var x = c.prescription();
        var p = x == null ? null : new PrescriptionParams(0, x.secondsPerRep(), x.transitionSeconds(), x.warmupMinutes(),
                x.defaultRestSeconds(), x.durationTolerancePct(), x.byGoal(), x.repLimits(), x.maxLoadIncreasePct(),
                x.maxRestSeconds());
        return new PlanGenerationContext(c.userId(), c.weekNumber(), c.weekStartDate(), c.weekEndDate(), c.profile(),
                c.adjustment(), c.previousWeek(), c.availableExercises(), c.safety(), p);
    }

    PlanDraft extender(PlanGenerationContext ctx, int divisor, PlanDraft base, ParametrosModeloE p) {
        var pres = ctx.prescription();
        String obj = ctx.profile().goalType(), niv = ctx.profile().fitnessLevel();
        int minutos = ctx.effectiveSessionMinutes();
        long techo = p.techoSegundos(minutos);
        var porId = ctx.availableExercises().stream()
                .collect(Collectors.toMap(CandidateExercise::exerciseId, c -> c, (a, b) -> a));
        var complementos = CatalogEligibilityPolicy.complementos(ctx.availableExercises(), obj);
        var pool = ctx.availableExercises().stream()
                .filter(c -> !complementos.contains(c.exerciseId()))
                .filter(c -> c.difficulty() <= ctx.effectiveMaxDifficulty())
                .filter(c -> CapabilityPrescriptionPolicy.claseDe(c) == CapabilityPrescriptionPolicy.Clase.ESTRUCTURAL
                        || CapabilityPrescriptionPolicy.claseDe(c) == CapabilityPrescriptionPolicy.Clase.RESISTIDO)
                .sorted(Comparator.comparing(CandidateExercise::exerciseId)).toList();
        var dias = new ArrayList<List<PlanDraft.DraftExercise>>();
        for (var w : base.workouts()) dias.add(new ArrayList<>(w.exercises()));
        int objetivo = p.objetivoSemanal(obj, dias.size());

        for (int vuelta = 0; vuelta < 200; vuelta++) {
            var vol = VolumenSemanal.medir(armar(base, dias), porId, complementos);
            String falta = null;
            for (var g : entrenables(base, p))
                if (vol.semanal(g) < objetivo && (falta == null || vol.semanal(g) < vol.semanal(falta))) falta = g;
            if (falta == null) break;
            if (!anadir(base, dias, falta, vol, pool, obj, niv, pres, techo, p)) {
                // El grupo no cabe: se registra como deficit (el validador candidato lo informa) y se sigue con otros.
                var resto = new ArrayList<>(entrenables(base, p));
                resto.remove(falta);
                boolean alguno = false;
                for (var g : resto)
                    if (vol.semanal(g) < objetivo && anadir(base, dias, g, vol, pool, obj, niv, pres, techo, p)) {
                        alguno = true;
                        break;
                    }
                if (!alguno) break;
            }
        }
        var w = armar(base, dias);
        var conDuracion = w.workouts().stream().map(x -> new PlanDraft.DraftWorkout(x.scheduledDate(), x.focus(), x.name(),
                ESTIMADOR.estimateMinutes(x, pres), x.exercises())).toList();
        var sin = new PlanDraft(base.source(), base.modelName(), base.planName(), null, base.rationale(), conDuracion);
        return new PlanDraft(base.source(), base.modelName(), base.planName(), sin.volume(divisor), base.rationale(),
                conDuracion);
    }

    private boolean anadir(PlanDraft base, List<List<PlanDraft.DraftExercise>> dias, String grupo, VolumenSemanal.Resultado vol,
                           List<CandidateExercise> pool, String obj, String niv, PrescriptionParams pres, long techo,
                           ParametrosModeloE p) {
        // Dias que admiten el grupo, del que menos series tiene de ese grupo al que mas.
        var orden = new ArrayList<Integer>();
        for (int i = 0; i < dias.size(); i++) {
            var foco = base.workouts().get(i).focus();
            if (foco == WorkoutFocus.FULL_BODY || foco.accepts(grupo)) orden.add(i);
        }
        orden.sort(Comparator.comparingInt((Integer i) -> vol.porSesion()
                .getOrDefault(base.workouts().get(i).scheduledDate(), Map.of()).getOrDefault(grupo, 0)).thenComparing(i -> i));
        for (int i : orden) {
            var w0 = base.workouts().get(i);
            var dia = dias.get(i);
            int enSesion = vol.porSesion().getOrDefault(w0.scheduledDate(), Map.of()).getOrDefault(grupo, 0);
            var ids = dia.stream().map(PlanDraft.DraftExercise::exerciseId).collect(Collectors.toSet());
            var vecinos = new HashSet<Long>();
            for (int j : new int[]{i - 1, i + 1})
                if (j >= 0 && j < dias.size() && Math.abs(base.workouts().get(j).scheduledDate().toEpochDay()
                        - w0.scheduledDate().toEpochDay()) == 1)
                    dias.get(j).forEach(e -> vecinos.add(e.exerciseId()));
            for (var c : pool) {
                if (!grupo.equals(c.bodyPartCode()) || ids.contains(c.exerciseId()) || vecinos.contains(c.exerciseId()))
                    continue;
                if (!v22(w0.focus(), c)) continue;
                var dosis = dosisBase(c, obj, niv, pres);
                if (dosis == null) continue;
                int series = dosis.plannedSets() == null ? 1 : dosis.plannedSets();
                if (enSesion + series > p.techoSeriesGrupoSesion()) continue;
                if (vol.semanal(grupo) + series > p.techoSeriesGrupoSemana()) continue;
                var con = new ArrayList<>(dia);
                con.add(dosis);
                if (ESTIMADOR.estimateSeconds(new PlanDraft.DraftWorkout(w0.scheduledDate(), w0.focus(), w0.name(), 0, con),
                        pres) > techo) continue;
                dia.add(dosis);
                return true;
            }
        }
        return false;
    }

    private static Set<String> entrenables(PlanDraft base, ParametrosModeloE p) {
        var out = new TreeSet<String>();
        for (var w : base.workouts())
            for (var g : p.gruposGrandes())
                if (w.focus() == WorkoutFocus.FULL_BODY || w.focus().accepts(g)) out.add(g);
        return out;
    }

    /** La dosis BASE aprobada de la capacidad del objetivo (4D en GAIN_MUSCLE; la primera admisible en los demas). */
    public static PlanDraft.DraftExercise dosisBase(CandidateExercise c, String obj, String niv, PrescriptionParams pres) {
        if (MvpDosePolicy.aplicaA(obj)) {
            if (c.defaultPrescription() == PrescriptionType.DURATION) return null;
            return MvpDosePolicy.dosisPara(MandatorySlot.of(c), c.bodyPartCode())
                    .map(d -> new PlanDraft.DraftExercise(c.exerciseId(), PrescriptionType.SETS_REPS, d.series(), d.reps(),
                            null, null, d.descansoSegundos(), null)).orElse(null);
        }
        for (var cap : CapabilityPrescriptionPolicy.admisibles(c, obj, niv)) {
            var d = CapabilityPrescriptionPolicy.dosis(cap, c, obj, niv, pres == null ? null : pres.repLimits());
            if (d.isPresent()) return d.get().prescribir(c.exerciseId(), null);
        }
        return null;
    }

    private static boolean v22(WorkoutFocus foco, CandidateExercise c) {
        if (!"upper arms".equals(c.bodyPartCode()) || c.targetMuscle() == null) return true;
        String m = c.targetMuscle().toLowerCase(Locale.ROOT);
        if (foco == WorkoutFocus.PUSH) return !m.contains("biceps");
        if (foco == WorkoutFocus.PULL) return !m.contains("triceps");
        return true;
    }

    private static PlanDraft armar(PlanDraft base, List<List<PlanDraft.DraftExercise>> dias) {
        var ws = new ArrayList<PlanDraft.DraftWorkout>();
        for (int i = 0; i < dias.size(); i++) {
            var w = base.workouts().get(i);
            ws.add(new PlanDraft.DraftWorkout(w.scheduledDate(), w.focus(), w.name(), w.expectedDurationMinutes(), dias.get(i)));
        }
        return new PlanDraft(base.source(), base.modelName(), base.planName(), null, base.rationale(), ws);
    }
}
