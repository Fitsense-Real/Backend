package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.planning.domain.model.valueobjects.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SUSTITUCION OBLIGATORIA POR SEGURIDAD en una semana derivada (P2-D).
 * <p>
 * Cuando un ejercicio HEREDADO de la semana medida dejo de ser compatible por
 * seguridad con el perfil actual -{@link ExclusionesDeSeguridad}-, conservarlo
 * publicaria algo inseguro y rechazar la semana deja al participante sin plan.
 * Esto busca, de forma determinista, un sustituto que mantenga lo que la semana
 * necesita, y si no lo hay lo dice.
 * <p>
 * <b>Que se sustituye y que no.</b> Solo ejercicios que (1) estaban en esa misma
 * sesion de la semana medida y (2) estan en {@link ExclusionesDeSeguridad}. No se
 * toca ningun otro ejercicio, ni dia, ni enfoque, ni duracion declarada. Un
 * ejercicio que trajo la IA y no es elegible NO se repara: es un error de la
 * propuesta y V1 lo rechaza, como siempre.
 * <p>
 * <b>Regla {@value #REGLA}</b>, el sustituto:
 * <ol>
 *   <li>es elegible para el perfil actual (equipamiento, dificultad,
 *       restricciones, bloqueos: todo lo que ya filtra el catalogo);</li>
 *   <li>tiene la misma clase de slot (4C): un estructural se sustituye por uno
 *       del MISMO slot, un relleno por un relleno;</li>
 *   <li>el mismo tipo de prescripcion (V7) y la misma condicion de complemento;</li>
 *   <li>una zona que el enfoque admite (V15), o la misma zona;</li>
 *   <li>en PUSH/PULL, el musculo de brazo que admite el enfoque (V22);</li>
 *   <li>no esta en revision de clasificacion, ni ya en la sesion, ni en una
 *       sesion de un dia contiguo (V18);</li>
 *   <li>orden: misma zona, mismo patron, mismo musculo objetivo, id.</li>
 * </ol>
 * <b>La dosis se recalcula</b>, no se copia a ciegas: en {@code GAIN_MUSCLE}, la
 * de 4D para la clase del sustituto; en los demas objetivos, series y descanso
 * del original con las repeticiones llevadas al rango de la zona del sustituto
 * (V16). El peso sugerido se deja en null: no hay registro de carga del
 * sustituto (V11).
 * <p>
 * Despues, la semana entera pasa por V8C -que solo admite exactamente estas
 * sustituciones- y por la validacion estricta. Dominio puro.
 */
public final class SafetySubstitutionPolicy {

    public static final String REGLA = "SUST-SEG-1";

    private SafetySubstitutionPolicy() {}

    public record Resultado(List<DerivedWorkout> semana,
                            List<SustitucionSeguridad> sustituciones,
                            List<String> problemas) {
        public Resultado {
            semana = List.copyOf(semana);
            sustituciones = List.copyOf(sustituciones);
            problemas = List.copyOf(problemas);
        }
    }

    public static Resultado reparar(List<DerivedWorkout> semana,
                                    PreviousWeekSummary previa,
                                    ExclusionesDeSeguridad exclusiones,
                                    Map<Long, CandidateExercise> elegibles,
                                    PlanGenerationContext context) {
        if (exclusiones == null || exclusiones.vacias() || semana == null)
            return new Resultado(semana == null ? List.of() : semana, List.of(), List.of());

        var heredados = heredadosPorSesion(previa);
        var idsPorFecha = new HashMap<LocalDate, Set<Long>>();
        for (var d : semana)
            idsPorFecha.computeIfAbsent(d.workout().scheduledDate(), k -> new HashSet<>())
                    .addAll(d.workout().exercises().stream().map(PlanDraft.DraftExercise::exerciseId).toList());

        var nuevaSemana = new ArrayList<DerivedWorkout>();
        var sustituciones = new ArrayList<SustitucionSeguridad>();
        var problemas = new ArrayList<String>();

        for (var derivada : semana) {
            var sesion = derivada.workout();
            var heredadosDeEsta = heredados.getOrDefault(derivada.sourcePlannedWorkoutId(), Set.of());
            var enSesion = sesion.exercises().stream().map(PlanDraft.DraftExercise::exerciseId)
                    .collect(Collectors.toCollection(HashSet::new));
            var contiguos = new HashSet<Long>();
            if (sesion.scheduledDate() != null) {
                contiguos.addAll(idsPorFecha.getOrDefault(sesion.scheduledDate().minusDays(1), Set.of()));
                contiguos.addAll(idsPorFecha.getOrDefault(sesion.scheduledDate().plusDays(1), Set.of()));
            }

            var ejercicios = new ArrayList<PlanDraft.DraftExercise>();
            boolean cambio = false;
            for (var e : sesion.exercises()) {
                if (!exclusiones.incluye(e.exerciseId()) || !heredadosDeEsta.contains(e.exerciseId())) {
                    ejercicios.add(e);
                    continue;
                }
                var original = exclusiones.originales().get(e.exerciseId());
                var sustituto = elegir(original, sesion.focus(), elegibles, enSesion, contiguos, context);
                if (sustituto.isEmpty()) {
                    problemas.add(("SEGURIDAD: el ejercicio %d de la sesion %d (%s) dejo de ser compatible "
                            + "por seguridad (%s) y no hay sustituto elegible con la regla %s: mismo slot (%s), "
                            + "tipo %s y zona que admita el enfoque %s. Requiere intervencion; no se publica "
                            + "una sustitucion insegura.")
                            .formatted(e.exerciseId(), derivada.sourcePlannedWorkoutId(), sesion.scheduledDate(),
                                    exclusiones.motivos().get(e.exerciseId()), REGLA,
                                    MandatorySlot.of(original).map(Enum::name).orElse("relleno"),
                                    original.defaultPrescription(), sesion.focus()));
                    ejercicios.add(e);
                    continue;
                }
                var c = sustituto.get();
                var dosis = dosis(c, e, context);
                ejercicios.add(dosis);
                enSesion.add(c.exerciseId());
                cambio = true;
                sustituciones.add(new SustitucionSeguridad(derivada.sourcePlannedWorkoutId(),
                        sesion.scheduledDate(), e.exerciseId(), original.movementPattern(), c.exerciseId(),
                        exclusiones.motivos().get(e.exerciseId()), REGLA, describir(e), describir(dosis)));
            }
            nuevaSemana.add(!cambio ? derivada : new DerivedWorkout(derivada.sourcePlannedWorkoutId(),
                    new PlanDraft.DraftWorkout(sesion.scheduledDate(), sesion.focus(), sesion.name(),
                            sesion.expectedDurationMinutes(), ejercicios)));
        }
        return new Resultado(nuevaSemana, sustituciones, problemas);
    }

    static Optional<CandidateExercise> elegir(CandidateExercise original, WorkoutFocus enfoque,
                                              Map<Long, CandidateExercise> elegibles, Set<Long> enSesion,
                                              Set<Long> contiguos, PlanGenerationContext context) {
        if (original == null || elegibles == null) return Optional.empty();
        String objetivo = context == null || context.profile() == null ? null : context.profile().goalType();
        var slot = MandatorySlot.of(original);
        boolean complemento = CatalogEligibilityPolicy.esComplemento(original, objetivo);
        return elegibles.values().stream()
                .filter(c -> c.exerciseId() != null && !c.exerciseId().equals(original.exerciseId()))
                .filter(c -> MandatorySlot.of(c).equals(slot))
                .filter(c -> c.defaultPrescription() == original.defaultPrescription())
                .filter(c -> CatalogEligibilityPolicy.esComplemento(c, objetivo) == complemento)
                .filter(c -> enfoque == null || enfoque.accepts(c.bodyPartCode())
                        || Objects.equals(c.bodyPartCode(), original.bodyPartCode()))
                .filter(c -> admiteBrazo(enfoque, c))
                .filter(c -> !GoalFeasibilityPolicy.enRevision(c))
                .filter(c -> !enSesion.contains(c.exerciseId()) && !contiguos.contains(c.exerciseId()))
                .sorted(Comparator.comparing((CandidateExercise c) -> !Objects.equals(c.bodyPartCode(), original.bodyPartCode()))
                        .thenComparing(c -> !Objects.equals(c.movementPattern(), original.movementPattern()))
                        .thenComparing(c -> !Objects.equals(c.targetMuscle(), original.targetMuscle()))
                        .thenComparing(CandidateExercise::exerciseId))
                .findFirst();
    }

    /** V22: en PUSH no biceps, en PULL no triceps, dentro de upper arms. */
    private static boolean admiteBrazo(WorkoutFocus enfoque, CandidateExercise c) {
        if (enfoque != WorkoutFocus.PUSH && enfoque != WorkoutFocus.PULL) return true;
        if (!"upper arms".equals(c.bodyPartCode()) || c.targetMuscle() == null) return true;
        String prohibido = enfoque == WorkoutFocus.PUSH ? "biceps" : "triceps";
        return !c.targetMuscle().toLowerCase(Locale.ROOT).contains(prohibido);
    }

    /** La dosis del sustituto, recalculada para sus caracteristicas. */
    static PlanDraft.DraftExercise dosis(CandidateExercise c, PlanDraft.DraftExercise original,
                                         PlanGenerationContext context) {
        if (c.defaultPrescription() == PrescriptionType.DURATION)
            return new PlanDraft.DraftExercise(c.exerciseId(), PrescriptionType.DURATION, original.plannedSets(),
                    null, original.plannedDurationSeconds(), null, original.restSeconds(), null);

        var perfil = context == null ? null : context.profile();
        if (perfil != null && MvpDosePolicy.aplicaA(perfil.goalType())) {
            var d = MvpDosePolicy.dosisPara(MandatorySlot.of(c), c.bodyPartCode()).orElseThrow();
            return new PlanDraft.DraftExercise(c.exerciseId(), PrescriptionType.SETS_REPS, d.series(), d.reps(),
                    null, null, d.descansoSegundos(), null);
        }

        short reps = original.plannedReps() == null ? 12 : original.plannedReps();
        var limites = context == null || context.prescription() == null ? null : context.prescription().repLimits();
        if (limites != null && limites.isComplete()) {
            int minimo = limites.minRepsFor(c.bodyPartCode(), perfil == null ? null : perfil.fitnessLevel());
            reps = (short) Math.min(limites.maxReps(), Math.max(minimo, reps));
        }
        return new PlanDraft.DraftExercise(c.exerciseId(), PrescriptionType.SETS_REPS, original.plannedSets(), reps,
                null, null, original.restSeconds(), null);
    }

    private static Map<Long, Set<Long>> heredadosPorSesion(PreviousWeekSummary previa) {
        var mapa = new HashMap<Long, Set<Long>>();
        if (previa == null || previa.prescriptions() == null) return mapa;
        for (var p : previa.prescriptions())
            if (p.plannedWorkoutId() != null && p.exerciseId() != null)
                mapa.computeIfAbsent(p.plannedWorkoutId(), k -> new HashSet<>()).add(p.exerciseId());
        return mapa;
    }

    private static String describir(PlanDraft.DraftExercise e) {
        return e.prescriptionType() == PrescriptionType.DURATION
                ? "%sx%ss descanso %s".formatted(e.plannedSets(), e.plannedDurationSeconds(), e.restSeconds())
                : "%sx%s descanso %s".formatted(e.plannedSets(), e.plannedReps(), e.restSeconds());
    }
}
