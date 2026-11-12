package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.GenerationSource;
import main.web.services.fitsense.planning.domain.model.valueobjects.GoalCapabilityPolicy.Capability;
import main.web.services.fitsense.planning.domain.model.valueobjects.MandatorySlot;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanGenerationContext;
import main.web.services.fitsense.planning.domain.model.valueobjects.WorkoutFocus;

import java.util.*;

/**
 * RENDIMIENTO (12/11/2026) · ¿PUEDE ALGUNA PROPUESTA DE LA IA PASAR EL VALIDADOR? Si se DEMUESTRA que no, pedirla solo
 * gasta hasta tres predicciones (unos 70 s cada una) antes de que publique el motor de reglas: es H-G9
 * (GENERAL_FITNESS intermedio o avanzado con una sesion de 30-35 min, {@code 03_DIAGNOSTICO_HG9.md}).
 * <p>
 * NO ES UNA REGLA NUEVA NI UN PLANIFICADOR. No decide que se publica: solo evita llamadas cuyo rechazo ya es seguro, y
 * el plan sigue saliendo del motor de reglas con la validacion completa, exactamente como tras tres rechazos. Usa las
 * comprobaciones que el validador aplica a un borrador de la IA, con sus mismos datos y funciones:
 * <ul>
 *   <li><b>V2</b>: la semana tiene {@code effectiveDaysPerWeek} sesiones. Solo se evalua con UNA: entonces toda la
 *       cobertura semanal (V23, V25) cae en esa sesion y V4 la mide entera.</li>
 *   <li><b>V24 extendida</b>: cada colocacion lleva la dosis EXACTA de una capacidad admisible
 *       ({@link CapabilityPrescriptionPolicy#acreditada}). Su duracion minima es la de esa dosis con el minimo de
 *       series; nada en la dosis se puede acortar sin dejar de acreditarla.</li>
 *   <li><b>V25</b>: cada principal factible ({@link CapabilityCoveragePolicy}) necesita una colocacion que la
 *       acredite, y cada colocacion acredita una sola.</li>
 *   <li><b>V23</b> (si bloquea borradores de la IA): si la sesion tiene al menos tantas colocaciones como slots
 *       exigibles, los cubre todos ({@link SlotCoveragePolicy}, con {@link CoberturaDePatron#slot}).</li>
 *   <li><b>V5</b>: al menos dos ejercicios.</li>
 *   <li><b>V4</b> (segundos estimados): {@link PlanningPolicy#excedeTecho} sobre el mismo
 *       {@link SessionDurationEstimator}: calentamiento + trabajo y descansos + transiciones.</li>
 * </ul>
 * La COTA INFERIOR es la combinacion mas barata que cumple esas condiciones, buscada de forma exacta sobre el conjunto
 * elegible (programacion dinamica sobre lo que falta por cubrir). Todas las demas reglas (V1, V6, V7, V15, V18, el
 * tope de colocaciones...) se IGNORAN: solo podrian subir la duracion minima, asi que ignorarlas nunca marca como
 * imposible un caso posible. Si la cota supera el techo de V4, ninguna propuesta puede ser valida.
 * <p>
 * No se evalua (devuelve vacio, y la IA se llama como siempre) si falta cualquier dato, con mas de una sesion, en
 * GAIN_MUSCLE (su V24 es otra, 4D v3), sin V4 en segundos o si V24/V25 no bloquean.
 */
public final class FactibilidadGeneracionIA {

    private static final SessionDurationEstimator ESTIMADOR = new SessionDurationEstimator();

    private FactibilidadGeneracionIA() {
    }

    /**
     * @param segundosMinimos la cota inferior; -1 si ninguna combinacion cumple V5 + V23 + V24 + V25
     * @param limiteSegundos  el techo de V4 para los minutos del perfil
     */
    public record Inviabilidad(long segundosMinimos, long limiteSegundos, int minutosPerfil,
                               Set<Capability> capacidadesExigidas, List<MandatorySlot> slotsExigidos,
                               boolean v23Exigida) {

        public String describe() {
            return segundosMinimos < 0
                    ? "ninguna combinacion de ejercicios elegibles cumple V5, V23, V24 y V25 en una sola sesion"
                    : ("una sola sesion con %s y %s con su dosis exacta dura al menos %d s y el techo de V4 es %d s"
                    + " (%d min)").formatted(capacidadesExigidas, v23Exigida ? "los slots " + slotsExigidos
                    : "sin exigir slots", segundosMinimos, limiteSegundos, minutosPerfil);
        }
    }

    public static Optional<Inviabilidad> evaluar(PlanGenerationContext ctx, SlotCoverageEnforcement cobertura,
                                                 DoseEnforcement capacidades) {
        if (ctx == null || ctx.profile() == null || ctx.prescription() == null || ctx.policy() == null
                || ctx.availableExercises() == null || cobertura == null || capacidades == null) return Optional.empty();
        if (ctx.effectiveDaysPerWeek() != 1 || !ctx.policy().v4Segundos() || !capacidades.bloquea())
            return Optional.empty();
        String objetivo = ctx.profile().goalType();
        String nivel = ctx.profile().fitnessLevel();
        if (!CapabilityPrescriptionPolicy.gobiernaGeneracion(objetivo)) return Optional.empty();
        var limites = ctx.prescription().repLimits();

        // El conjunto elegible sin repetidos, como lo ve el validador.
        var porId = new LinkedHashMap<Long, CandidateExercise>();
        ctx.availableExercises().stream().filter(Objects::nonNull).filter(c -> c.exerciseId() != null)
                .forEach(c -> porId.putIfAbsent(c.exerciseId(), c));

        // V25: las principales factibles (las que omitiria una semana vacia).
        var vacio = new PlanDraft(GenerationSource.AI, "cota", "cota", null, "cota", List.of());
        var exigidas = new ArrayList<>(CapabilityCoveragePolicy.evaluar(objetivo, nivel, vacio, porId.values(), limites)
                .omitidas());
        // V23, si bloquea un borrador de la IA en este objetivo.
        boolean v23 = cobertura.bloqueaA(GenerationSource.AI) && GoalFeasibilityPolicy.exigeSlotsResistidos(objetivo);
        boolean v23Siempre = cobertura == SlotCoverageEnforcement.ENFORCE_ALWAYS;
        var slots = v23 ? new ArrayList<>(SlotCoveragePolicy.requiredOf(
                SlotCoveragePolicy.ofEligible(ctx.availableExercises()))) : new ArrayList<MandatorySlot>();

        // Cada colocacion posible (ejercicio, capacidad admisible con dosis) se reduce a lo que aporta -la capacidad
        // exigida que acredita y el slot exigido que toca- y a su duracion minima. Por aportacion basta la mas corta.
        int bitsCap = exigidas.size();
        var masCorta = new HashMap<Integer, Long>();
        for (var c : porId.values()) {
            var slot = CoberturaDePatron.slot(c).orElse(null);
            for (var cap : CapabilityPrescriptionPolicy.admisibles(c, objetivo, nivel)) {
                var d = CapabilityPrescriptionPolicy.dosis(cap, c, objetivo, nivel, limites);
                if (d.isEmpty()) continue;
                int aporte = 0;
                int i = exigidas.indexOf(cap);
                if (i >= 0) aporte |= 1 << i;
                int j = slot == null ? -1 : slots.indexOf(slot);
                if (j >= 0) aporte |= 1 << (bitsCap + j);
                masCorta.merge(aporte, segundosDe(d.get().prescribir(c.exerciseId(), null), ctx), Math::min);
            }
        }

        long cota = cotaInferior(masCorta, bitsCap, slots.size(), v23, v23Siempre,
                (long) ctx.prescription().warmupMinutesOrDefault() * 60, ctx.prescription().transitionSecondsOrDefault());
        int minutos = ctx.profile().sessionMinutes();
        long limite = ctx.policy().limiteV4SegundosDe(minutos);
        if (cota >= 0 && !ctx.policy().excedeTecho(cota, 0, minutos)) return Optional.empty();
        return Optional.of(new Inviabilidad(cota, limite, minutos,
                exigidas.isEmpty() ? EnumSet.noneOf(Capability.class) : EnumSet.copyOf(exigidas),
                List.copyOf(slots), v23));
    }

    /**
     * La sesion valida mas corta: Dijkstra sobre (lo cubierto, cuantas colocaciones), con colocaciones repetibles
     * (relajacion: repetir nunca cubre mas, solo puede hacer exigible V23). -1 si ningun estado cumple.
     */
    static long cotaInferior(Map<Integer, Long> masCorta, int bitsCap, int nSlots, boolean v23, boolean v23Siempre,
                             long warmup, long transicion) {
        int todasCap = (1 << bitsCap) - 1;
        int todosSlots = ((1 << nSlots) - 1) << bitsCap;
        int tope = Math.max(nSlots, 2);   // basta saber si hay >= 2 (V5) y >= nSlots (V23) colocaciones
        int estados = 1 << (bitsCap + nSlots);
        var coste = new long[estados][tope + 1];
        for (var fila : coste) Arrays.fill(fila, Long.MAX_VALUE);
        // El calentamiento va una vez y hay una transicion menos que colocaciones: se descuenta al empezar.
        coste[0][0] = warmup - transicion;
        var cola = new PriorityQueue<long[]>(Comparator.comparingLong(e -> e[0]));
        cola.add(new long[]{coste[0][0], 0, 0});
        while (!cola.isEmpty()) {
            var e = cola.poll();
            int mask = (int) e[1], pos = (int) e[2];
            if (e[0] > coste[mask][pos]) continue;
            boolean capsOk = (mask & todasCap) == todasCap;
            boolean slotsOk = !v23 || (!v23Siempre && pos < nSlots) || (mask & todosSlots) == todosSlots;
            if (pos >= 2 && capsOk && slotsOk) return e[0];
            for (var opcion : masCorta.entrySet()) {
                int m = mask | opcion.getKey();
                int p = Math.min(tope, pos + 1);
                long c = e[0] + opcion.getValue() + transicion;
                if (c < coste[m][p]) {
                    coste[m][p] = c;
                    cola.add(new long[]{c, m, p});
                }
            }
        }
        return -1;
    }

    /** Trabajo y descansos de una colocacion, sin calentamiento ni transicion (el mismo estimador que V4). */
    private static long segundosDe(PlanDraft.DraftExercise e, PlanGenerationContext ctx) {
        var w = new PlanDraft.DraftWorkout(ctx.weekStartDate(), WorkoutFocus.FULL_BODY, "cota", 0, List.of(e));
        return ESTIMADOR.estimateSeconds(w, ctx.prescription())
                - (long) ctx.prescription().warmupMinutesOrDefault() * 60;
    }
}
