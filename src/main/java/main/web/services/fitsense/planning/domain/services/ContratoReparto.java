package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.GoalCapabilityPolicy;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanGenerationContext;
import main.web.services.fitsense.planning.domain.model.valueobjects.WorkoutFocus;

import java.util.*;

/**
 * H2-v2 (PR-2.0): el reparto que verifican V15 y V26, en una sola clase, para que lo que se le comunica a la IA sea
 * EXACTAMENTE lo que despues comprueba el validador. No cambia V15 ni V26: los replica. {@code ContratoRepartoIT}
 * comprueba con el validador real que coinciden (si alguien cambia V15 o V26, esa prueba avisa).
 * <ul>
 *   <li><b>V15, regla de la mitad</b> ({@code PlanDraftValidator.validateFocusCoverage}): si el enfoque abarca 3 o mas
 *   grupos y la sesion tiene 4 o mas ejercicios DE TRABAJO (sin los complementos de capacidad), ningun grupo puede
 *   tener mas de la mitad (division entera) de esos ejercicios. Ademas, la sesion cubre al menos
 *   {@code min(2, grupos con material, ejercicios de trabajo)} grupos del enfoque y no incluye grupos ajenos (salvo
 *   FULL_BODY).</li>
 *   <li><b>V26, techo de volumen</b> ({@code PlanDraftValidator.validateWeeklyVolume}, solo con
 *   {@code policy.volumenSemanal()}): las series DIRECTAS de un grupo (ejercicios ESTRUCTURAL o RESISTIDO que no son
 *   complemento) no pasan de {@link DosisSemanalPolicy#techoSeriesSesion} en una sesion ni de
 *   {@link DosisSemanalPolicy#techoSeriesSemana} en la semana.</li>
 * </ul>
 * La regla de la mitad depende del numero de ejercicios de trabajo, que decide la IA: por eso se comunica como tabla.
 */
public final class ContratoReparto {

    private ContratoReparto() {}

    /** Ejercicios de trabajo a partir de los que aplica la regla de la mitad. */
    public static final int MIN_TRABAJO = 4;
    /** Grupos del enfoque a partir de los que aplica la regla de la mitad. */
    public static final int MIN_GRUPOS = 3;

    /** Cuantos ejercicios del mismo grupo admite V15 con {@code nTrabajo} de trabajo; {@code null} si no aplica. */
    public static Integer topeMitad(int gruposDelFoco, int nTrabajo) {
        if (gruposDelFoco < MIN_GRUPOS || nTrabajo < MIN_TRABAJO) return null;
        return nTrabajo / 2;
    }

    /** La tabla n -> tope para n = 4..maxTrabajo; vacia si la regla no aplica. Claves de texto (objeto JSON). */
    public static Map<String, Integer> tablaMitad(int gruposDelFoco, int maxTrabajo) {
        if (gruposDelFoco < MIN_GRUPOS || maxTrabajo < MIN_TRABAJO) return Map.of();
        var tabla = new LinkedHashMap<String, Integer>();
        for (int n = MIN_TRABAJO; n <= maxTrabajo; n++) tabla.put(Integer.toString(n), topeMitad(gruposDelFoco, n));
        return Collections.unmodifiableMap(tabla);
    }

    /**
     * Si un candidato cuenta para que su grupo este DISPONIBLE en V15: replica de
     * {@code PlanDraftValidator.prescribibleParaV15} (mismo orden de comprobaciones).
     */
    public static boolean prescribibleParaV15(CandidateExercise candidate, WorkoutFocus focus,
                                              PlanGenerationContext context) {
        if (context.profile() == null) return true;
        String objetivo = context.profile().goalType();
        if (GoalCapabilityPolicy.forGoal(objetivo).isEmpty()) return true;
        if (candidate.difficulty() > context.effectiveMaxDifficulty()) return false;
        if ("upper arms".equals(candidate.bodyPartCode()) && candidate.targetMuscle() != null) {
            String musculo = candidate.targetMuscle().toLowerCase(Locale.ROOT);
            if (focus == WorkoutFocus.PUSH && musculo.contains("biceps")) return false;
            if (focus == WorkoutFocus.PULL && musculo.contains("triceps")) return false;
        }
        String nivel = context.profile().fitnessLevel();
        var limites = context.prescription() == null ? null : context.prescription().repLimits();
        return CapabilityPrescriptionPolicy.admisibles(candidate, objetivo, nivel).stream()
                .anyMatch(cap -> CapabilityPrescriptionPolicy.dosis(cap, candidate, objetivo, nivel, limites).isPresent());
    }

    /** Los complementos de capacidad (cardio y movilidad) que V15 y V26 no cuentan. */
    public static Set<Long> complementos(PlanGenerationContext context) {
        return CatalogEligibilityPolicy.complementos(context.availableExercises(),
                context.profile() == null ? null : context.profile().goalType());
    }

    /** Los grupos del enfoque con material de trabajo PRESCRIBIBLE (lo que V15 llama disponibles), en su orden. */
    public static List<String> gruposConMaterial(WorkoutFocus focus, PlanGenerationContext context,
                                                 Set<Long> complementos) {
        return focus.bodyPartCodes().stream()
                .filter(code -> context.availableExercises().stream()
                        .filter(c -> !complementos.contains(c.exerciseId()))
                        .filter(c -> prescribibleParaV15(c, focus, context))
                        .anyMatch(c -> code.equals(c.bodyPartCode())))
                .toList();
    }

    /**
     * Si V15 aplica la regla de la mitad a una sesion con {@code nTrabajo} de trabajo: replica de
     * {@code mitadAplicable} (con el prototipo de laboratorio apagado, que es el valor por omision, siempre).
     */
    public static boolean mitadAplicable(WorkoutFocus focus, int gruposConMaterial, int nTrabajo) {
        boolean laboratorio = !LaboratorioAuditoria.v15MitadSobrePrescribibles() || gruposConMaterial >= 2;
        return focus.bodyPartCodes().size() >= MIN_GRUPOS && nTrabajo >= MIN_TRABAJO && laboratorio;
    }

    /** Si las series de un ejercicio cuentan como DIRECTAS para V26. */
    public static boolean cuentaParaVolumen(CandidateExercise c, Set<Long> complementos) {
        if (c == null || c.bodyPartCode() == null || complementos.contains(c.exerciseId())) return false;
        var clase = CapabilityPrescriptionPolicy.claseDe(c);
        return clase == CapabilityPrescriptionPolicy.Clase.ESTRUCTURAL
                || clase == CapabilityPrescriptionPolicy.Clase.RESISTIDO;
    }

    /** Los techos de V26 de esta generacion, o {@code null} si V26 no aplica (politica sin volumen semanal). */
    public static Volumen volumen(PlanGenerationContext context) {
        if (context.policy() == null || !context.policy().volumenSemanal() || context.profile() == null) return null;
        String nivel = context.profile().fitnessLevel();
        String objetivo = context.profile().goalType();
        var objetivos = new TreeMap<String, Integer>();
        context.availableExercises().stream().map(CandidateExercise::bodyPartCode).filter(Objects::nonNull)
                .distinct().forEach(g -> objetivos.put(g, DosisSemanalPolicy.objetivoSeriesSemana(objetivo, nivel, g)));
        var complementos = complementos(context);
        var noCuentan = context.availableExercises().stream()
                .filter(c -> !complementos.contains(c.exerciseId()) && !cuentaParaVolumen(c, complementos))
                .map(CandidateExercise::exerciseId).sorted().toList();
        return new Volumen(DosisSemanalPolicy.techoSeriesSesion(nivel), DosisSemanalPolicy.techoSeriesSemana(nivel),
                Collections.unmodifiableMap(objetivos), noCuentan);
    }

    /**
     * Los techos de V26 y lo que no cuenta.
     *
     * @param objetivoSemana orientacion por grupo de {@link DosisSemanalPolicy#objetivoSeriesSemana} (no se valida).
     * @param noCuentan      ejercicios de trabajo (no complementos) cuyas series NO cuentan como directas.
     */
    public record Volumen(int techoSesion, int techoSemana, Map<String, Integer> objetivoSemana, List<Long> noCuentan) {}
}
