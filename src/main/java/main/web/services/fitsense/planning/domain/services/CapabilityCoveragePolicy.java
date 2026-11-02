package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.configuration.domain.model.valueobjects.PrescriptionParams;
import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.GoalCapabilityPolicy;
import main.web.services.fitsense.planning.domain.model.valueobjects.GoalCapabilityPolicy.Capability;
import main.web.services.fitsense.planning.domain.model.valueobjects.MandatorySlot;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;

import java.util.*;
import java.util.stream.Collectors;

/**
 * COBERTURA DE CAPACIDADES DE UNA SEMANA (4A v6 + 4D v4). Responde dos
 * preguntas con la MISMA regla que usa el motor de reglas, para que generador y
 * validador no puedan discrepar:
 * <ol>
 *   <li><b>Factibilidad</b>: con el conjunto elegible del participante, ¿cada
 *       capacidad tiene al menos un ejercicio VERIFICADO con el que se pueda
 *       prescribir su dosis? Ya respeta nivel, edad, equipamiento, ubicacion,
 *       restricciones y bloqueos, porque el conjunto elegible los aplica.</li>
 *   <li><b>Cobertura</b>: ¿la semana la REPRESENTA de verdad? Una capacidad se
 *       cubre solo con una colocacion cuya dosis la acredita
 *       ({@link CapabilityPrescriptionPolicy#acreditada}); tener ejercicios
 *       disponibles no cuenta.</li>
 * </ol>
 * El resultado ({@link Resultado}) es solo DESCRIPTIVO: va al log y no decide
 * la publicacion. Lo unico que decide es {@link Cobertura#omitidas()}: una
 * principal factible ausente de la semana, que V25 rechaza.
 */
public final class CapabilityCoveragePolicy {

    private CapabilityCoveragePolicy() {}

    public enum Resultado { COMPLETO, PARCIAL, NO_FACTIBLE }

    /** Factibilidad de una capacidad con el conjunto elegible. */
    public record Factibilidad(Capability capacidad, boolean factible, int candidatos, String motivo) {}

    public static Map<Capability, Factibilidad> factibilidad(String goalType, String fitnessLevel,
                                                             Collection<CandidateExercise> elegibles,
                                                             PrescriptionParams.RepLimits limites) {
        var resultado = new EnumMap<Capability, Factibilidad>(Capability.class);
        var g = GoalCapabilityPolicy.forGoal(goalType);
        if (g.isEmpty()) return resultado;
        var lista = elegibles == null ? List.<CandidateExercise>of()
                : elegibles.stream().filter(Objects::nonNull).filter(c -> c.exerciseId() != null).toList();

        for (var req : g.get().requirements()) {
            var cap = req.capability();
            if (!req.mayApplyTo(fitnessLevel)) {
                resultado.put(cap, new Factibilidad(cap, false, 0, "no aplica a nivel " + fitnessLevel));
                continue;
            }
            if (!CapabilityPrescriptionPolicy.asignables(goalType, fitnessLevel).contains(cap)) {
                resultado.put(cap, new Factibilidad(cap, false, 0, cap == Capability.POWER
                        ? "no verificable: el catalogo no clasifica ejercicios explosivos"
                        : "no se asigna en este objetivo (4D v3)"));
                continue;
            }
            int n = (int) lista.stream()
                    .filter(c -> CapabilityPrescriptionPolicy.admisibles(c, goalType, fitnessLevel).contains(cap))
                    .filter(c -> CapabilityPrescriptionPolicy.dosis(cap, c, goalType, fitnessLevel, limites).isPresent())
                    .map(CandidateExercise::exerciseId).distinct().count();
            int enRevision = (int) lista.stream()
                    .filter(c -> CapabilityPrescriptionPolicy.claseDe(c) == CapabilityPrescriptionPolicy.Clase.EN_REVISION)
                    .count();
            resultado.put(cap, new Factibilidad(cap, n > 0, n, n > 0 ? n + " candidatos verificados"
                    : "sin ejercicios verificados compatibles con las condiciones del participante"
                    + (cap == Capability.CARDIO ? " (un circuito no acredita cardio)" : "")
                    + (enRevision > 0 ? "; los ejercicios en revision de clasificacion no cuentan" : "")));
        }
        return resultado;
    }

    /** Las principales factibles. */
    public static Set<Capability> principalesFactibles(Map<Capability, Factibilidad> f, String goalType) {
        var principales = GoalCapabilityPolicy.forGoal(goalType).map(GoalCapabilityPolicy.GoalCapabilities::primaries)
                .orElse(Set.of());
        return principales.stream().filter(c -> f.containsKey(c) && f.get(c).factible())
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(Capability.class)));
    }

    /**
     * La cobertura de una semana.
     *
     * @param colocaciones por capacidad acreditada, cuantas colocaciones
     * @param noCubiertas  principales que faltan, con su motivo
     * @param omitidas     principales FACTIBLES que la semana no representa: es
     *                     un error del generador (V25), no una limitacion
     * @param slotsSinCubrir slots de 4C sin colocacion acreditada de la capacidad
     *                       estructural principal (informativo; V23 decide)
     */
    public record Cobertura(String objetivo, String version, Resultado resultado,
                            Set<Capability> principalesCubiertas, Map<Capability, String> noCubiertas,
                            Set<Capability> omitidas, Set<Capability> secundariasIncorporadas,
                            Map<Capability, Integer> colocaciones, Set<MandatorySlot> slotsSinCubrir) {

        public String describe() {
            return "version=%s objetivo=%s resultado=%s cubiertas=%s no_cubiertas=%s omitidas=%s secundarias=%s colocaciones=%s slots_sin_cubrir=%s"
                    .formatted(version, objetivo, resultado, principalesCubiertas, noCubiertas, omitidas,
                            secundariasIncorporadas, colocaciones, slotsSinCubrir);
        }

    }

    public static Cobertura evaluar(String goalType, String fitnessLevel, PlanDraft borrador,
                                    Collection<CandidateExercise> elegibles,
                                    PrescriptionParams.RepLimits limites) {
        var g = GoalCapabilityPolicy.forGoal(goalType).orElse(null);
        String objetivo = g == null ? goalType : g.goalType();
        var porId = new HashMap<Long, CandidateExercise>();
        if (elegibles != null) elegibles.stream().filter(Objects::nonNull).filter(c -> c.exerciseId() != null)
                .forEach(c -> porId.putIfAbsent(c.exerciseId(), c));

        var colocaciones = new EnumMap<Capability, Integer>(Capability.class);
        var slotsAcreditados = EnumSet.noneOf(MandatorySlot.class);
        var estructuralPrincipal = g == null ? null : g.primaries().stream()
                .filter(c -> c == Capability.STRENGTH || c == Capability.HYPERTROPHY).findFirst().orElse(null);
        if (borrador != null && borrador.workouts() != null)
            for (var w : borrador.workouts()) {
                if (w == null || w.exercises() == null) continue;
                for (var e : w.exercises()) {
                    var c = e == null ? null : porId.get(e.exerciseId());
                    if (c == null) continue;
                    var cap = CapabilityPrescriptionPolicy.acreditada(e, c, goalType, fitnessLevel, limites);
                    if (cap.isEmpty()) continue;
                    colocaciones.merge(cap.get(), 1, Integer::sum);
                    if (cap.get() == estructuralPrincipal) MandatorySlot.of(c).ifPresent(slotsAcreditados::add);
                }
            }

        var factibilidad = factibilidad(goalType, fitnessLevel, porId.values(), limites);
        var principales = g == null ? Set.<Capability>of() : g.primaries();
        var cubiertas = EnumSet.noneOf(Capability.class);
        var noCubiertas = new LinkedHashMap<Capability, String>();
        var omitidas = EnumSet.noneOf(Capability.class);
        for (var cap : principales) {
            if (colocaciones.containsKey(cap)) { cubiertas.add(cap); continue; }
            var f = factibilidad.get(cap);
            if (f != null && f.factible()) {
                omitidas.add(cap);
                noCubiertas.put(cap, "factible y omitida por la semana");
            } else {
                noCubiertas.put(cap, f == null ? "sin datos" : "no factible: " + f.motivo());
            }
        }
        var secundarias = EnumSet.noneOf(Capability.class);
        colocaciones.keySet().stream().filter(c -> !principales.contains(c)).forEach(secundarias::add);

        // Slots: solo los que el conjunto elegible puede cubrir.
        var slotsSinCubrir = EnumSet.noneOf(MandatorySlot.class);
        if (estructuralPrincipal != null)
            for (var slot : MandatorySlot.ALL) {
                boolean hay = porId.values().stream().anyMatch(c -> MandatorySlot.of(c).filter(s -> s == slot).isPresent()
                        && CapabilityPrescriptionPolicy.claseDe(c) == CapabilityPrescriptionPolicy.Clase.ESTRUCTURAL);
                if (hay && !slotsAcreditados.contains(slot)) slotsSinCubrir.add(slot);
            }

        var resultado = principales.isEmpty() || cubiertas.isEmpty() ? Resultado.NO_FACTIBLE
                : cubiertas.containsAll(principales) ? Resultado.COMPLETO : Resultado.PARCIAL;
        return new Cobertura(objetivo, CapabilityPrescriptionPolicy.VERSION, resultado, cubiertas,
                noCubiertas, omitidas, secundarias, colocaciones, slotsSinCubrir);
    }
}
