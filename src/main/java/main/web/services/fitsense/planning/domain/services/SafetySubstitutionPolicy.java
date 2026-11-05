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
    /**
     * Fase 4 del cierre de PLAN-REV-2.0: sustituto de un heredado DESACTIVADO en el catalogo. Todas las restricciones
     * de {@value #REGLA} y, ademas, el equivalente estricto de {@link PermisosIntervencion#equivalenteEstricto}: mismo
     * patron, misma zona, musculo objetivo compatible, dificultad no mayor e impacto no mayor (con impacto desconocido,
     * ninguno). La desaparicion del ejercicio no autoriza por si sola ningun cambio mas.
     */
    public static final String REGLA_INACTIVO = "SUST-CAT-1";

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
        return reparar(semana, previa, exclusiones, elegibles, context, false);
    }

    /**
     * Igual, con el LIMITE DE DIFICULTAD candidato ({@code fitsense.adaptation.difficulty-guard=on}): el sustituto no
     * puede tener una dificultad registrada mayor que la del original. Sin sustituto que lo cumpla, se informa como
     * cualquier falta de sustituto: no se publica una sustitucion que no cumple la regla.
     */
    public static Resultado reparar(List<DerivedWorkout> semana,
                                    PreviousWeekSummary previa,
                                    ExclusionesDeSeguridad exclusiones,
                                    Map<Long, CandidateExercise> elegibles,
                                    PlanGenerationContext context,
                                    boolean limitaDificultad) {
        return reparar(semana, previa, exclusiones, elegibles, context,
                limitaDificultad ? Estrategia.LIMITE : Estrategia.HISTORICA);
    }

    /** Igual, con la estrategia de dificultad explicita (A, B o C). */
    public static Resultado reparar(List<DerivedWorkout> semana,
                                    PreviousWeekSummary previa,
                                    ExclusionesDeSeguridad exclusiones,
                                    Map<Long, CandidateExercise> elegibles,
                                    PlanGenerationContext context,
                                    Estrategia estrategia) {
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
                boolean inactivo = SafetyConflictPolicy.esCatalogoInactivo(exclusiones.motivos().get(e.exerciseId()));
                var sustituto = inactivo
                        ? elegirEquivalenteEstricto(original, sesion.focus(), elegibles, enSesion, contiguos, context)
                        : elegir(original, sesion.focus(), elegibles, enSesion, contiguos, context, estrategia);
                if (sustituto.isEmpty() && inactivo) {
                    problemas.add(("CATALOGO: el ejercicio %d de la sesion %d (%s) se desactivo en el catalogo y no hay"
                            + " un sustituto equivalente estricto elegible (regla %s: mismo slot %s, patron %s, zona %s,"
                            + " musculo compatible, dificultad e impacto no superiores, tipo %s, zona que admita el"
                            + " enfoque %s). No se publica un ejercicio inactivo ni una sustitucion que no cumple la"
                            + " regla; requiere intervencion.")
                            .formatted(e.exerciseId(), derivada.sourcePlannedWorkoutId(), sesion.scheduledDate(),
                                    REGLA_INACTIVO, MandatorySlot.of(original).map(Enum::name).orElse("relleno"),
                                    original.movementPattern(), original.bodyPartCode(), original.defaultPrescription(),
                                    sesion.focus()));
                    ejercicios.add(e);
                    continue;
                }
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
                        exclusiones.motivos().get(e.exerciseId()), inactivo ? REGLA_INACTIVO : REGLA, describir(e),
                        describir(dosis)));
            }
            nuevaSemana.add(!cambio ? derivada : new DerivedWorkout(derivada.sourcePlannedWorkoutId(),
                    new PlanDraft.DraftWorkout(sesion.scheduledDate(), sesion.focus(), sesion.name(),
                            sesion.expectedDurationMinutes(), ejercicios)));
        }
        return new Resultado(nuevaSemana, sustituciones, problemas);
    }

    /**
     * COMO ELIGE SUST-SEG-1 ANTE LA DIFICULTAD (candidato, {@code fitsense.adaptation.difficulty-guard}).
     * <ul>
     *   <li>{@link #HISTORICA} (A, por defecto): sin limite; orden zona, patron, musculo, id.</li>
     *   <li>{@link #LIMITE} (B, {@code on}): solo candidatos de dificultad registrada &lt;= la del original; mismo
     *       orden.</li>
     *   <li>{@link #LIMITE_Y_PREFERENCIA} (C, {@code preferencia}): el mismo filtro y, como ULTIMO criterio antes del
     *       id, la dificultad mas cercana a la del original sin superarla (igual antes que menor). La preferencia va
     *       detras de zona, patron y musculo: no puede cambiar ninguno de ellos.</li>
     * </ul>
     * Las restricciones de seguridad y elegibilidad ({@link Etapa}) son las mismas en las tres.
     */
    public enum Estrategia {
        HISTORICA, LIMITE, LIMITE_Y_PREFERENCIA;

        public boolean limita() {
            return this != HISTORICA;
        }
    }

    /**
     * Las restricciones OBLIGATORIAS de SUST-SEG-1, en el orden en que se comprueban. Un candidato se descarta por la
     * primera que no cumple. {@link #DIFICULTAD_NO_MAYOR} solo se aplica con {@link Estrategia#limita()}.
     */
    public enum Etapa {
        ES_EL_MISMO, MISMO_SLOT, MISMA_PRESCRIPCION, MISMO_COMPLEMENTO, ZONA_DEL_ENFOQUE, BRAZO_DEL_ENFOQUE,
        NO_EN_REVISION, NO_EN_LA_SESION_NI_CONTIGUO, DIFICULTAD_NO_MAYOR
    }

    static Optional<CandidateExercise> elegir(CandidateExercise original, WorkoutFocus enfoque,
                                              Map<Long, CandidateExercise> elegibles, Set<Long> enSesion,
                                              Set<Long> contiguos, PlanGenerationContext context) {
        return elegir(original, enfoque, elegibles, enSesion, contiguos, context, Estrategia.HISTORICA);
    }

    static Optional<CandidateExercise> elegir(CandidateExercise original, WorkoutFocus enfoque,
                                              Map<Long, CandidateExercise> elegibles, Set<Long> enSesion,
                                              Set<Long> contiguos, PlanGenerationContext context,
                                              boolean limitaDificultad) {
        return elegir(original, enfoque, elegibles, enSesion, contiguos, context,
                limitaDificultad ? Estrategia.LIMITE : Estrategia.HISTORICA);
    }

    static Optional<CandidateExercise> elegir(CandidateExercise original, WorkoutFocus enfoque,
                                              Map<Long, CandidateExercise> elegibles, Set<Long> enSesion,
                                              Set<Long> contiguos, PlanGenerationContext context,
                                              Estrategia estrategia) {
        if (original == null || elegibles == null) return Optional.empty();
        String objetivo = objetivo(context);
        var orden = Comparator.comparing((CandidateExercise c) -> !Objects.equals(c.bodyPartCode(), original.bodyPartCode()))
                .thenComparing(c -> !Objects.equals(c.movementPattern(), original.movementPattern()))
                .thenComparing(c -> !Objects.equals(c.targetMuscle(), original.targetMuscle()));
        if (estrategia == Estrategia.LIMITE_Y_PREFERENCIA)
            orden = orden.thenComparingInt(c -> original.difficulty() - c.difficulty());
        return elegibles.values().stream()
                .filter(c -> descarte(c, original, enfoque, enSesion, contiguos, objetivo, estrategia.limita()) == null)
                .sorted(orden.thenComparing(CandidateExercise::exerciseId))
                .findFirst();
    }

    /**
     * La primera restriccion obligatoria que {@code c} no cumple como sustituto de {@code original}, o {@code null} si
     * las cumple todas. Es la MISMA comprobacion que usa {@link #elegir}: sirve para explicar un descarte.
     */
    static Etapa descarte(CandidateExercise c, CandidateExercise original, WorkoutFocus enfoque, Set<Long> enSesion,
                          Set<Long> contiguos, String objetivo, boolean limitaDificultad) {
        if (c.exerciseId() == null || c.exerciseId().equals(original.exerciseId())) return Etapa.ES_EL_MISMO;
        if (!MandatorySlot.of(c).equals(MandatorySlot.of(original))) return Etapa.MISMO_SLOT;
        if (c.defaultPrescription() != original.defaultPrescription()) return Etapa.MISMA_PRESCRIPCION;
        if (CatalogEligibilityPolicy.esComplemento(c, objetivo) != CatalogEligibilityPolicy.esComplemento(original, objetivo))
            return Etapa.MISMO_COMPLEMENTO;
        if (!(enfoque == null || enfoque.accepts(c.bodyPartCode()) || Objects.equals(c.bodyPartCode(), original.bodyPartCode())))
            return Etapa.ZONA_DEL_ENFOQUE;
        if (!admiteBrazo(enfoque, c)) return Etapa.BRAZO_DEL_ENFOQUE;
        if (GoalFeasibilityPolicy.enRevision(c)) return Etapa.NO_EN_REVISION;
        if (enSesion.contains(c.exerciseId()) || contiguos.contains(c.exerciseId())) return Etapa.NO_EN_LA_SESION_NI_CONTIGUO;
        if (limitaDificultad && c.difficulty() > original.difficulty()) return Etapa.DIFICULTAD_NO_MAYOR;
        return null;
    }

    /** {@value #REGLA_INACTIVO}: las restricciones de siempre (con limite de dificultad) + el equivalente estricto. */
    static Optional<CandidateExercise> elegirEquivalenteEstricto(CandidateExercise original, WorkoutFocus enfoque,
                                                                 Map<Long, CandidateExercise> elegibles,
                                                                 Set<Long> enSesion, Set<Long> contiguos,
                                                                 PlanGenerationContext context) {
        if (original == null || elegibles == null) return Optional.empty();
        String objetivo = objetivo(context);
        var orden = Comparator.comparing((CandidateExercise c) -> !Objects.equals(c.targetMuscle(), original.targetMuscle()))
                .thenComparingInt(c -> original.difficulty() - c.difficulty())
                .thenComparing(CandidateExercise::exerciseId);
        return elegibles.values().stream()
                .filter(c -> descarte(c, original, enfoque, enSesion, contiguos, objetivo, true) == null)
                .filter(c -> PermisosIntervencion.equivalenteEstricto(original, c))
                .sorted(orden)
                .findFirst();
    }

    static String objetivo(PlanGenerationContext context) {
        return context == null || context.profile() == null ? null : context.profile().goalType();
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
