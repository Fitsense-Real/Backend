package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.planning.domain.model.valueobjects.*;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;

/**
 * C2-D · ¿ESTA SATURADA LA SEMANA? Dominio puro. Decide si las sesiones que no llegan al piso de V20 ya tienen todo el
 * contenido que las reglas vigentes permiten, comprobandolo con el MISMO validador.
 * <p>
 * Movimientos que se evaluan en cada sesion corta (todos con la dosis BASE aprobada de 4D o de
 * {@link CapabilityPrescriptionPolicy}; nunca se inventa una dosis, nunca se alarga un descanso):
 * <ol>
 *   <li><b>ANADIR</b> un ejercicio elegible (dificultad efectiva, no repetido en el dia ni en el dia pegado, del
 *       enfoque si es resistido) mientras quepa en el tope de colocaciones de V24 y en el techo de V4.</li>
 *   <li><b>SERIE</b>: una serie mas (hasta 3) a un ejercicio de series y repeticiones.</li>
 *   <li><b>SUSTITUIR</b> un ejercicio de la sesion por otro elegible cuya dosis base dura mas.</li>
 *   <li><b>REDISTRIBUIR</b> un ejercicio de otra sesion de la semana a la sesion corta, si con ello baja el numero de
 *       sesiones por debajo del piso.</li>
 * </ol>
 * Un movimiento es una MEJORA si el validador no encuentra ningun problema distinto de V20 y la sesion corta dura mas
 * (o, en REDISTRIBUIR, hay menos sesiones cortas). Si existe alguna mejora, la semana NO esta saturada y C2-D no
 * actua: el plan sigue rechazado por V20. Si se agota el presupuesto de evaluaciones o faltan datos, el estado es
 * NO_DEMOSTRADA y tampoco actua.
 * <p>
 * ALCANCE: es una SATURACION LOCAL COMPROBADA (ningun movimiento UNICO de los cuatro tipos mejora). No demuestra la
 * optimalidad global: no prueba combinaciones de varios movimientos ni recomposiciones completas de la semana.
 */
public final class SaturacionContenidoV20 {

    public enum Estado { SATURADA, NO_SATURADA, NO_DEMOSTRADA, NO_APLICA }

    public record Resultado(Estado estado, List<LocalDate> sesionesCortas, String motivo, int evaluaciones,
                            Map<String, Integer> descartes) {
        public boolean saturada() {
            return estado == Estado.SATURADA;
        }
    }

    /** Evaluaciones del validador por semana antes de declarar NO_DEMOSTRADA. */
    public static final int PRESUPUESTO = 4000;

    private final PlanDraft original;
    private final PlanGenerationContext ctx;
    private final Function<PlanDraft, List<String>> problemas;
    private final SessionDurationEstimator estimador;
    private final int presupuesto;
    private final Map<String, Integer> descartes = new TreeMap<>();
    private int evaluaciones;

    private SaturacionContenidoV20(PlanDraft d, PlanGenerationContext ctx, Function<PlanDraft, List<String>> problemas,
                                   SessionDurationEstimator estimador, int presupuesto) {
        this.original = d;
        this.ctx = ctx;
        this.problemas = problemas;
        this.estimador = estimador;
        this.presupuesto = presupuesto;
    }

    public static Resultado evaluar(PlanDraft d, PlanGenerationContext ctx, Function<PlanDraft, List<String>> problemas,
                                    SessionDurationEstimator estimador, int presupuesto) {
        return new SaturacionContenidoV20(d, ctx, problemas, estimador, presupuesto).evaluar();
    }

    private static final class Presupuesto extends RuntimeException {
        Presupuesto() {
            super(null, null, false, false);
        }
    }

    private Resultado evaluar() {
        if (original == null || original.workouts() == null || ctx.prescription() == null || ctx.profile() == null)
            return new Resultado(Estado.NO_DEMOSTRADA, List.of(), "faltan datos (borrador o prescripcion)", 0, descartes);
        long piso = PlanDraftValidator.minimumSessionSeconds(ctx);
        if (piso <= 0) return new Resultado(Estado.NO_APLICA, List.of(), "V20 no aplica en esta semana", 0, descartes);
        var porId = new HashMap<Long, CandidateExercise>();
        ctx.availableExercises().forEach(c -> porId.putIfAbsent(c.exerciseId(), c));
        for (var w : original.workouts())
            for (var e : w.exercises())
                if (!porId.containsKey(e.exerciseId()))
                    return new Resultado(Estado.NO_DEMOSTRADA, List.of(),
                            "el borrador trae el ejercicio " + e.exerciseId() + " fuera del conjunto elegible", 0,
                            descartes);
        var cortas = cortas(original, piso);
        if (cortas.isEmpty())
            return new Resultado(Estado.NO_APLICA, List.of(), "ninguna sesion bajo el piso", 0, descartes);
        var fechas = cortas.stream().map(i -> original.workouts().get(i).scheduledDate()).toList();
        var pool = ctx.availableExercises().stream().filter(c -> c.difficulty() <= ctx.effectiveMaxDifficulty())
                .sorted(Comparator.comparing(CandidateExercise::exerciseId)).toList();
        String obj = ctx.profile().goalType();
        int tope = MvpDosePolicy.aplicaA(obj) ? MvpDosePolicy.maxColocaciones(ctx.effectiveSessionMinutes())
                : CapabilityPrescriptionPolicy.MAX_COLOC_DIA;
        try {
            for (int i : cortas) {
                var m = mejoraEnSesion(i, piso, pool, tope, porId);
                if (m != null)
                    return new Resultado(Estado.NO_SATURADA, fechas, m, evaluaciones, descartes);
            }
        } catch (Presupuesto p) {
            return new Resultado(Estado.NO_DEMOSTRADA, fechas,
                    "presupuesto de " + presupuesto + " evaluaciones agotado sin demostrar la saturacion", evaluaciones,
                    descartes);
        }
        return new Resultado(Estado.SATURADA, fechas,
                "ningun movimiento unico (anadir, serie, sustituir, redistribuir) mejora sin romper otra regla",
                evaluaciones, descartes);
    }

    private String mejoraEnSesion(int i, long piso, List<CandidateExercise> pool, int tope,
                                  Map<Long, CandidateExercise> porId) {
        var w = original.workouts().get(i);
        long antes = seg(w);
        var enDia = ids(w);
        var vecinos = vecinos(original, i);
        String obj = ctx.profile().goalType(), niv = ctx.profile().fitnessLevel();

        // 1 · ANADIR
        if (w.exercises().size() < tope)
            for (var c : pool) {
                if (!admisible(c, w, enDia, vecinos)) continue;
                var dosis = dosisBase(c, obj, niv, ctx.prescription());
                if (dosis == null) { contar("sin_dosis_base"); continue; }
                var nuevo = conSesion(original, i, mas(w.exercises(), dosis));
                if (excede(nuevo.workouts().get(i))) { contar("techo_v4"); continue; }
                if (mejora(nuevo, i, antes)) return "ANADIR " + c.exerciseId() + " el " + w.scheduledDate();
            }
        else contar("tope_v24");

        // 2 · SERIE
        for (int j = 0; j < w.exercises().size(); j++) {
            var e = w.exercises().get(j);
            if (e.prescriptionType() != PrescriptionType.SETS_REPS || e.plannedSets() == null || e.plannedSets() >= 3) {
                contar("serie_maxima_o_duracion");
                continue;
            }
            var mas = new PlanDraft.DraftExercise(e.exerciseId(), e.prescriptionType(), (short) (e.plannedSets() + 1),
                    e.plannedReps(), e.plannedDurationSeconds(), e.targetLoadKg(), e.restSeconds(), e.notes());
            var nuevo = conSesion(original, i, reemplazo(w.exercises(), j, mas));
            if (excede(nuevo.workouts().get(i))) { contar("techo_v4"); continue; }
            if (mejora(nuevo, i, antes)) return "SERIE +1 al ejercicio " + e.exerciseId() + " el " + w.scheduledDate();
        }

        // 3 · SUSTITUIR por algo mas largo
        for (int j = 0; j < w.exercises().size(); j++) {
            var e = w.exercises().get(j);
            long sj = segEjercicio(e);
            for (var c : pool) {
                if (!admisible(c, w, enDia, vecinos)) continue;
                var dosis = dosisBase(c, obj, niv, ctx.prescription());
                if (dosis == null) continue;
                if (segEjercicio(dosis) <= sj) continue;
                var nuevo = conSesion(original, i, reemplazo(w.exercises(), j, dosis));
                if (excede(nuevo.workouts().get(i))) { contar("techo_v4"); continue; }
                if (mejora(nuevo, i, antes))
                    return "SUSTITUIR " + e.exerciseId() + " por " + c.exerciseId() + " el " + w.scheduledDate();
            }
        }

        // 4 · REDISTRIBUIR desde otra sesion
        if (w.exercises().size() < tope) {
            int cortasAntes = cortas(original, piso).size();
            for (int k = 0; k < original.workouts().size(); k++) {
                if (k == i) continue;
                var otra = original.workouts().get(k);
                if (otra.exercises().size() <= 2) continue;
                for (int j = 0; j < otra.exercises().size(); j++) {
                    var e = otra.exercises().get(j);
                    var c = porId.get(e.exerciseId());
                    if (c == null || !admisible(c, w, enDia, vecinos)) continue;
                    var sin = new ArrayList<>(otra.exercises());
                    sin.remove(j);
                    var nuevo = conSesion(conSesion(original, k, sin), i, mas(w.exercises(), e));
                    if (excede(nuevo.workouts().get(i))) { contar("techo_v4"); continue; }
                    var rr = evaluarProblemas(nuevo);
                    if (rr.stream().allMatch(x -> x.startsWith("V20:")) && cortas(nuevo, piso).size() < cortasAntes)
                        return "REDISTRIBUIR " + e.exerciseId() + " de " + otra.scheduledDate() + " a " + w.scheduledDate();
                    contar(rr.stream().filter(x -> !x.startsWith("V20:")).findFirst().map(SaturacionContenidoV20::codigo)
                            .orElse("V20_SIN_MEJORA"));
                }
            }
        }
        return null;
    }

    /** Filtros baratos antes del validador: lo que este descartaria seguro. */
    private boolean admisible(CandidateExercise c, PlanDraft.DraftWorkout w, Set<Long> enDia, Set<Long> vecinos) {
        if (enDia.contains(c.exerciseId())) { contar("ya_en_el_dia"); return false; }
        if (vecinos.contains(c.exerciseId())) { contar("dia_pegado_v18"); return false; }
        var clase = CapabilityPrescriptionPolicy.claseDe(c);
        boolean resistido = clase == CapabilityPrescriptionPolicy.Clase.ESTRUCTURAL
                || clase == CapabilityPrescriptionPolicy.Clase.RESISTIDO;
        if (resistido && !(w.focus() == WorkoutFocus.FULL_BODY || w.focus().accepts(c.bodyPartCode()))) {
            contar("fuera_del_enfoque");
            return false;
        }
        return true;
    }

    private boolean mejora(PlanDraft nuevo, int i, long antes) {
        var rr = evaluarProblemas(nuevo);
        var otros = rr.stream().filter(x -> !x.startsWith("V20:")).toList();
        if (!otros.isEmpty()) {
            contar(codigo(otros.get(0)));
            return false;
        }
        if (seg(nuevo.workouts().get(i)) <= antes) {
            contar("no_alarga");
            return false;
        }
        return true;
    }

    private List<String> evaluarProblemas(PlanDraft nuevo) {
        if (++evaluaciones > presupuesto) throw new Presupuesto();
        return problemas.apply(nuevo);
    }

    private boolean excede(PlanDraft.DraftWorkout w) {
        return ctx.policy().excedeTecho(seg(w), estimador.estimateMinutes(w, ctx.prescription()),
                ctx.profile().sessionMinutes());
    }

    private void contar(String motivo) {
        descartes.merge(motivo, 1, Integer::sum);
    }

    private List<Integer> cortas(PlanDraft d, long piso) {
        var out = new ArrayList<Integer>();
        for (int i = 0; i < d.workouts().size(); i++) if (seg(d.workouts().get(i)) < piso) out.add(i);
        return out;
    }

    private long seg(PlanDraft.DraftWorkout w) {
        return estimador.estimateSeconds(w, ctx.prescription());
    }

    /** Trabajo + descansos de un ejercicio, sin calentamiento ni transicion. */
    private long segEjercicio(PlanDraft.DraftExercise e) {
        var w = new PlanDraft.DraftWorkout(LocalDate.MIN, WorkoutFocus.FULL_BODY, "x", 0, List.of(e));
        return seg(w) - ctx.prescription().warmupMinutesOrDefault() * 60L;
    }

    private static Set<Long> ids(PlanDraft.DraftWorkout w) {
        var s = new HashSet<Long>();
        w.exercises().forEach(e -> s.add(e.exerciseId()));
        return s;
    }

    static Set<Long> vecinos(PlanDraft d, int i) {
        var out = new HashSet<Long>();
        var w0 = d.workouts().get(i);
        for (int j : new int[]{i - 1, i + 1})
            if (j >= 0 && j < d.workouts().size()
                    && Math.abs(d.workouts().get(j).scheduledDate().toEpochDay() - w0.scheduledDate().toEpochDay()) == 1)
                d.workouts().get(j).exercises().forEach(e -> out.add(e.exerciseId()));
        return out;
    }

    private static List<PlanDraft.DraftExercise> mas(List<PlanDraft.DraftExercise> l, PlanDraft.DraftExercise e) {
        var x = new ArrayList<>(l);
        x.add(e);
        return x;
    }

    private static List<PlanDraft.DraftExercise> reemplazo(List<PlanDraft.DraftExercise> l, int j,
                                                           PlanDraft.DraftExercise e) {
        var x = new ArrayList<>(l);
        x.set(j, e);
        return x;
    }

    private PlanDraft conSesion(PlanDraft d, int i, List<PlanDraft.DraftExercise> ejercicios) {
        var ws = new ArrayList<>(d.workouts());
        var w = ws.get(i);
        var nueva = new PlanDraft.DraftWorkout(w.scheduledDate(), w.focus(), w.name(), 0, List.copyOf(ejercicios));
        ws.set(i, new PlanDraft.DraftWorkout(w.scheduledDate(), w.focus(), w.name(),
                estimador.estimateMinutes(nueva, ctx.prescription()), List.copyOf(ejercicios)));
        return new PlanDraft(d.source(), d.modelName(), d.planName(), d.declaredTotalVolume(), d.rationale(), ws);
    }

    static String codigo(String problema) {
        var p = problema.trim();
        int i = p.indexOf(':');
        return i > 0 && i < 8 ? p.substring(0, i) : "OTRO";
    }

    /** La dosis BASE aprobada (4D en GAIN_MUSCLE; la primera capacidad admisible con dosis en los demas). */
    public static PlanDraft.DraftExercise dosisBase(CandidateExercise c, String obj, String niv,
                                                    main.web.services.fitsense.configuration.domain.model.valueobjects
                                                            .PrescriptionParams pres) {
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
}
