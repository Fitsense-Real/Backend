package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Las validaciones de 19.3 (1-17) y las de coherencia 18-21, aplicadas al
 * borrador antes de persistir.
 * <p>
 * Servicio de dominio puro: no consulta la base. Todo lo que necesita viaja en
 * el contexto, que es exactamente lo que se guarda en input_snapshot, asi que
 * una validacion puede reproducirse meses despues.
 * <p>
 * Devuelve TODOS los problemas, no el primero: esa lista es el insumo del
 * segundo intento de la IA.
 */
@Service
public class PlanDraftValidator {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(PlanDraftValidator.class);

    /** Pisos de 18.4. No dependen de configuracion porque son limites de seguridad. */
    private static final int MIN_EXERCISES_PER_SESSION = 2;
    private static final int MIN_SETS = 2;
    private static final int MIN_REPS = 6;
    private static final int MIN_DURATION_SECONDS = 20;

    private final SessionDurationEstimator durationEstimator;

    /**
     * Como se trata un incumplimiento de cobertura 4C. Propiedad, no despliegue:
     * el codigo entra entero y esto decide si bloquea. Ver
     * {@link SlotCoverageEnforcement}.
     */
    private final SlotCoverageEnforcement coverageEnforcement;

    /**
     * Como se trata un incumplimiento de la dosis de 4D v2 en {@code GAIN_MUSCLE}.
     * Propiedad, no despliegue, igual que la de cobertura. Ver
     * {@link DoseEnforcement}.
     */
    private final DoseEnforcement doseEnforcement;

    /**
     * 4D v4 / 4A v6 · como se tratan la dosis por capacidad de los objetivos
     * distintos de GAIN_MUSCLE (V24 extendida) y la cobertura de las capacidades
     * principales (V25). Propiedad propia, separada de la de 4D v3: la
     * adaptacion P2-D construye su validador sin ella (queda en OFF) porque las
     * semanas que conserva se publicaron con la dosis anterior y exigirles la
     * nueva las dejaria sin plan.
     */
    private final DoseEnforcement capabilityEnforcement;

    /**
     * La anotacion Autowired de abajo es obligatoria, no decoracion: con dos
     * constructores publicos y ninguno marcado, Spring no sabe cual usar y el
     * contexto NO arranca.
     */
    @org.springframework.beans.factory.annotation.Autowired
    public PlanDraftValidator(
            SessionDurationEstimator durationEstimator,
            @org.springframework.beans.factory.annotation.Value(
                    "${fitsense.coverage.enforcement:OBSERVE}")
            String coverageEnforcement,
            @org.springframework.beans.factory.annotation.Value(
                    "${fitsense.dose.enforcement:OBSERVE}")
            String doseEnforcement,
            @org.springframework.beans.factory.annotation.Value(
                    "${fitsense.capability.enforcement:ENFORCE}")
            String capabilityEnforcement) {
        this.durationEstimator = durationEstimator;
        this.coverageEnforcement = SlotCoverageEnforcement.parse(coverageEnforcement);
        this.doseEnforcement = DoseEnforcement.parse(doseEnforcement);
        this.capabilityEnforcement = DoseEnforcement.parse(capabilityEnforcement);
    }

    /**
     * Para pruebas y para construirlo sin contenedor. Deja la dosis en
     * {@link DoseEnforcement#OFF} para que las pruebas anteriores a 4D sigan
     * comprobando exactamente lo que comprobaban.
     */
    public PlanDraftValidator(SessionDurationEstimator durationEstimator,
                              SlotCoverageEnforcement coverageEnforcement) {
        this(durationEstimator, coverageEnforcement, DoseEnforcement.OFF);
    }

    /** Para probar la dosis sin contenedor. */
    public PlanDraftValidator(SessionDurationEstimator durationEstimator,
                              SlotCoverageEnforcement coverageEnforcement,
                              DoseEnforcement doseEnforcement) {
        this(durationEstimator, coverageEnforcement, doseEnforcement, DoseEnforcement.OFF);
    }

    /** Con la dosis por capacidad y la cobertura de capacidades (V24 extendida, V25). */
    public PlanDraftValidator(SessionDurationEstimator durationEstimator,
                              SlotCoverageEnforcement coverageEnforcement,
                              DoseEnforcement doseEnforcement,
                              DoseEnforcement capabilityEnforcement) {
        this.durationEstimator = durationEstimator;
        this.coverageEnforcement = coverageEnforcement == null
                ? SlotCoverageEnforcement.OBSERVE : coverageEnforcement;
        this.doseEnforcement = doseEnforcement == null
                ? DoseEnforcement.OBSERVE : doseEnforcement;
        this.capabilityEnforcement = capabilityEnforcement == null
                ? DoseEnforcement.OFF : capabilityEnforcement;
    }

    public DoseEnforcement capabilityEnforcement() {
        return capabilityEnforcement;
    }

    public DoseEnforcement doseEnforcement() {
        return doseEnforcement;
    }

    public SlotCoverageEnforcement coverageEnforcement() {
        return coverageEnforcement;
    }

    public void validate(PlanDraft draft, PlanGenerationContext context, int durationToRepsDivisor) {
        validate(draft, context, durationToRepsDivisor, ConservationReport.vacio());
    }

    /**
     * V8C: exencion de V20 por sesion para fechas autorizadas a acortarse.
     * El productor incorpora los problemas de ConservationReport por separado.
     * La validacion inicial sigue usando la sobrecarga de tres argumentos.
     */
    public void validate(PlanDraft draft, PlanGenerationContext context,
                         int durationToRepsDivisor, ConservationReport conservation) {
        var problems = new ArrayList<String>();

        var eligible = context.availableExercises().stream()
                .collect(Collectors.toMap(CandidateExercise::exerciseId, candidate -> candidate,
                        (a, b) -> a));

        validateExercisesAndDays(draft, context, eligible, problems);
        validateFocusCoverage(draft, context, eligible, problems);
        validateWeeklySlotCoverage(draft, context, eligible, problems);
        validateSessions(draft, context, problems);
        validateVolumeTarget(draft, context, durationToRepsDivisor, problems);
        validateLoads(draft, context, problems);
        validatePrescriptionRanges(draft, context, eligible, problems);
        validateRecovery(draft, eligible, problems);
        validateArmSplit(draft, eligible, problems);
        appendSplitSuggestion(context, problems);
        validateReductionDoesNotIncrease(draft, context, eligible, problems);
        validateMinimumDurationAndRest(draft, context, conservation, problems);
        validateMvpDose(draft, context, eligible, conservation, problems);
        validateCapabilityDose(draft, context, eligible, conservation, problems);
        // Una colocacion con dosis autorizada sigue acreditando la capacidad de su
        // dosis base (diseno §4.4). En la generacion no hay autorizaciones: el
        // borrador se evalua tal cual.
        validateCapabilityCoverage(conservation.dosisAutorizadas().isEmpty() ? draft
                : conDosisBase(draft, context, eligible, conservation), context, eligible, problems);

        if (!problems.isEmpty()) throw new InvalidPlanDraftException(problems);
    }

    /**
     * El borrador con cada dosis AUTORIZADA y dentro de rango sustituida por la
     * dosis base que la admite. Solo para evaluar la cobertura (V25); la dosis
     * que se publica no cambia. Lo que no esta autorizado queda igual.
     */
    private PlanDraft conDosisBase(PlanDraft draft, PlanGenerationContext context,
                                   Map<Long, CandidateExercise> eligible, ConservationReport conservation) {
        if (context.profile() == null || draft == null || draft.workouts() == null) return draft;
        String objetivo = context.profile().goalType();
        String nivel = context.profile().fitnessLevel();
        var limites = context.prescription() == null ? null : context.prescription().repLimits();
        var sesiones = new ArrayList<PlanDraft.DraftWorkout>();
        for (var w : draft.workouts()) {
            if (w == null || w.exercises() == null) {
                sesiones.add(w);
                continue;
            }
            var ejercicios = new ArrayList<PlanDraft.DraftExercise>();
            for (var e : w.exercises()) {
                var candidato = e == null ? null : eligible.get(e.exerciseId());
                var origen = candidato == null ? Optional.<DosisAutorizada.Origen>empty()
                        : conservation.autorizacionDe(w.scheduledDate(), e);
                PlanDraft.DraftExercise base = null;
                if (origen.isPresent() && MvpDosePolicy.aplicaA(objetivo)) {
                    var exigida = MvpDosePolicy.dosisPara(MandatorySlot.of(candidato), candidato.bodyPartCode());
                    if (exigida.isPresent() && ProgresionDosisPolicy.admite4D(exigida.get(), e, origen.get()))
                        base = new PlanDraft.DraftExercise(e.exerciseId(), e.prescriptionType(), exigida.get().series(),
                                exigida.get().reps(), e.plannedDurationSeconds(), e.targetLoadKg(),
                                exigida.get().descansoSegundos(), e.notes());
                } else if (origen.isPresent()) {
                    int minReps = limites == null ? 0 : limites.minRepsFor(candidato.bodyPartCode(), nivel);
                    int maxReps = limites == null || limites.maxReps() == null ? Integer.MAX_VALUE : limites.maxReps();
                    base = CapabilityPrescriptionPolicy.admisibles(candidato, objetivo, nivel).stream()
                            .map(c -> CapabilityPrescriptionPolicy.dosis(c, candidato, objetivo, nivel, limites))
                            .flatMap(Optional::stream)
                            .filter(d -> ProgresionDosisPolicy.admiteCapacidad(d, e, origen.get(), minReps, maxReps))
                            .findFirst()
                            .map(d -> new PlanDraft.DraftExercise(e.exerciseId(), d.tipo(), d.series(), d.reps(),
                                    d.segundos(), e.targetLoadKg(), d.descanso(), e.notes()))
                            .orElse(null);
                }
                ejercicios.add(base == null ? e : base);
            }
            sesiones.add(new PlanDraft.DraftWorkout(w.scheduledDate(), w.focus(), w.name(),
                    w.expectedDurationMinutes(), ejercicios));
        }
        return new PlanDraft(draft.source(), draft.modelName(), draft.planName(), draft.declaredTotalVolume(),
                draft.rationale(), sesiones);
    }

    /**
     * V24 EXTENDIDA (4D v4) · dosis por capacidad para INCREASE_STRENGTH,
     * LOSE_WEIGHT y GENERAL_FITNESS. GAIN_MUSCLE sigue en
     * {@link #validateMvpDose}, sin cambios.
     * <p>
     * Cada colocacion tiene que demostrar una capacidad del objetivo con su
     * dosis EXACTA ({@link CapabilityPrescriptionPolicy}). La capacidad se
     * deduce de la clase del ejercicio en el catalogo y de la dosis; nunca se
     * lee del borrador. Una colocacion que no aporta ninguna capacidad del
     * objetivo -un abdominal en LOSE_WEIGHT, un ejercicio en revision de
     * clasificacion- se rechaza: no se anaden ejercicios solo para llenar
     * minutos. Tope de {@value CapabilityPrescriptionPolicy#MAX_COLOC_DIA}
     * colocaciones por dia.
     * <p>
     * No comprueba la intensidad del cardio ni el RIR: no hay columna.
     */
    private void validateCapabilityDose(PlanDraft draft, PlanGenerationContext context,
                                        Map<Long, CandidateExercise> eligible, ConservationReport conservation,
                                        List<String> problems) {
        if (!capabilityEnforcement.mide() || context.profile() == null) return;
        String objetivo = context.profile().goalType();
        if (MvpDosePolicy.aplicaA(objetivo)) {
            validateHypertrophyEvidence(draft, context, eligible, problems);
            return;
        }
        if (!CapabilityPrescriptionPolicy.gobiernaGeneracion(objetivo)) return;
        String nivel = context.profile().fitnessLevel();
        var limites = context.prescription() == null ? null : context.prescription().repLimits();

        var incumplimientos = new ArrayList<String>();
        for (var workout : draft.workouts()) {
            if (workout == null || workout.exercises() == null) continue;
            if (workout.exercises().size() > CapabilityPrescriptionPolicy.MAX_COLOC_DIA)
                incumplimientos.add("V24: el entrenamiento del %s tiene %d colocaciones y el maximo es %d."
                        .formatted(workout.scheduledDate(), workout.exercises().size(),
                                CapabilityPrescriptionPolicy.MAX_COLOC_DIA));
            for (var exercise : workout.exercises()) {
                var candidate = exercise == null ? null : eligible.get(exercise.exerciseId());
                if (candidate == null) continue;   // V1
                var admisibles = CapabilityPrescriptionPolicy.admisibles(candidate, objetivo, nivel);
                if (admisibles.isEmpty()) {
                    incumplimientos.add(("V24: el ejercicio %d del %s no aporta ninguna capacidad de %s (clase %s). "
                            + "Usa un ejercicio de una capacidad del objetivo.")
                            .formatted(exercise.exerciseId(), workout.scheduledDate(), objetivo,
                                    CapabilityPrescriptionPolicy.claseDe(candidate)));
                    continue;
                }
                if (CapabilityPrescriptionPolicy.acreditada(exercise, candidate, objetivo, nivel, limites).isPresent())
                    continue;
                // Una dosis distinta de la exacta SOLO si V8C la autorizo en esta colocacion
                // (progresion, reduccion o herencia) y cae en su rango. La generacion no trae
                // autorizaciones: alli esto nunca acepta nada.
                var origenAutorizado = conservation.autorizacionDe(workout.scheduledDate(), exercise);
                if (origenAutorizado.isPresent() && admisibles.stream()
                        .map(c -> CapabilityPrescriptionPolicy.dosis(c, candidate, objetivo, nivel, limites))
                        .flatMap(Optional::stream)
                        .anyMatch(d -> ProgresionDosisPolicy.admiteCapacidad(d, exercise, origenAutorizado.get(),
                                limites == null ? 0 : limites.minRepsFor(candidate.bodyPartCode(), nivel),
                                limites == null || limites.maxReps() == null ? Integer.MAX_VALUE : limites.maxReps())))
                    continue;
                var opciones = admisibles.stream()
                        .map(c -> CapabilityPrescriptionPolicy.dosis(c, candidate, objetivo, nivel, limites))
                        .flatMap(Optional::stream).map(CapabilityPrescriptionPolicy.Dosis::describe).toList();
                incumplimientos.add(("V24: el ejercicio %d del %s no lleva la dosis de ninguna capacidad de %s. "
                        + "Usa exactamente una de: %s.").formatted(exercise.exerciseId(), workout.scheduledDate(),
                        objetivo, opciones.isEmpty() ? "(ninguna aplicable a su tipo)" : opciones));
            }
        }
        if (incumplimientos.isEmpty()) return;
        if (!capabilityEnforcement.bloquea()) {
            log.warn("Dosis por capacidad (4D v4) incumplida para el usuario {} y NO se rechaza (modo {}): {}",
                    context.userId(), capabilityEnforcement, incumplimientos);
            return;
        }
        problems.addAll(incumplimientos);
    }

    /**
     * V24 EN GAIN_MUSCLE (4D v3.1) · solo cuenta como trabajo de hipertrofia lo
     * que puede demostrarla: un ejercicio estructural o resistido verificado.
     * Movilidad, cardio, equilibrio, patron {@code OTHER} o sin patron, los de
     * DURACION (4D v3 no tiene dosis de hipertrofia para ellos) y los
     * ejercicios en revision de clasificacion no acreditan la capacidad
     * principal y se rechazan: no pueden ocupar una colocacion de la sesion de
     * hipertrofia. La dosis la sigue exigiendo {@link #validateMvpDose}.
     * <p>
     * Bajo el interruptor de capacidades, asi que la adaptacion P2-D, que
     * conserva semanas anteriores, no lo aplica.
     */
    private void validateHypertrophyEvidence(PlanDraft draft, PlanGenerationContext context,
                                             Map<Long, CandidateExercise> eligible, List<String> problems) {
        var incumplimientos = new ArrayList<String>();
        for (var workout : draft.workouts()) {
            if (workout == null || workout.exercises() == null) continue;
            for (var exercise : workout.exercises()) {
                var candidate = exercise == null ? null : eligible.get(exercise.exerciseId());
                if (candidate == null) continue;   // V1
                var clase = CapabilityPrescriptionPolicy.claseDe(candidate);
                boolean repeticiones = candidate.defaultPrescription() != PrescriptionType.DURATION;
                if (repeticiones && (clase == CapabilityPrescriptionPolicy.Clase.ESTRUCTURAL
                        || clase == CapabilityPrescriptionPolicy.Clase.RESISTIDO)) continue;
                incumplimientos.add(("V24: el ejercicio %d del %s no acredita hipertrofia (clase %s, patron %s%s). "
                        + "Usa un ejercicio de fuerza estructural o resistido de repeticiones.")
                        .formatted(exercise.exerciseId(), workout.scheduledDate(), clase, candidate.movementPattern(),
                                repeticiones ? "" : ", de duracion: 4D v3 no tiene dosis de hipertrofia para ella"));
            }
        }
        if (incumplimientos.isEmpty()) return;
        if (!capabilityEnforcement.bloquea()) {
            log.warn("Evidencia de hipertrofia incumplida para el usuario {} y NO se rechaza (modo {}): {}",
                    context.userId(), capabilityEnforcement, incumplimientos);
            return;
        }
        problems.addAll(incumplimientos);
    }

    /**
     * V25 (4A v6) · LAS CAPACIDADES PRINCIPALES FACTIBLES ESTAN EN LA SEMANA.
     * <p>
     * Si el conjunto elegible permite prescribir una principal con su dosis y la
     * semana no la representa con ninguna colocacion acreditada, se rechaza:
     * una secundaria no desplaza a una principal factible. Las principales NO
     * factibles no se exigen: la semana sale PARCIAL y lo dice. Las secundarias
     * no se exigen nunca. Es la misma regla con la que genera el motor de
     * reglas ({@link CapabilityCoveragePolicy}).
     */
    private void validateCapabilityCoverage(PlanDraft draft, PlanGenerationContext context,
                                            Map<Long, CandidateExercise> eligible, List<String> problems) {
        if (!capabilityEnforcement.mide() || context.profile() == null) return;
        var limites = context.prescription() == null ? null : context.prescription().repLimits();
        var cobertura = CapabilityCoveragePolicy.evaluar(context.profile().goalType(),
                context.profile().fitnessLevel(), draft, eligible.values(), limites);
        log.info("COBERTURA_CAPACIDADES usuario {} semana {} origen {}: {}", context.userId(),
                context.weekNumber(), draft.source(), cobertura.describe());
        var faltas = new ArrayList<String>(cobertura.omitidas().stream()
                .map(c -> "V25: la semana omite la capacidad principal %s, que es factible con el conjunto elegible. "
                        .formatted(c) + "Incluye al menos una colocacion con su dosis antes de completar con secundarias.")
                .toList());
        faltas.addAll(cardioPorSesion(draft, context, eligible, limites));
        faltas.addAll(slotsConFuerza(draft, context, eligible, limites));
        if (faltas.isEmpty()) return;
        if (!capabilityEnforcement.bloquea()) {
            log.warn("Cobertura de capacidades incumplida para el usuario {} y NO se rechaza (modo {}): {}",
                    context.userId(), capabilityEnforcement, faltas);
            return;
        }
        problems.addAll(faltas);
    }

    /** Zonas con descanso entre dias seguidos: las mismas que respeta el motor de reglas. */
    private static final Set<String> ZONAS_CON_RECUPERACION = Set.of("chest", "back", "shoulders", "upper legs");

    /**
     * V25 · LOSE_WEIGHT: CARDIO EN CADA SESION en la que sea factible.
     * <p>
     * Decision del 26/10/2026, tras el plan 55 (una sola colocacion de CARDIO
     * en la semana). Es la regla con la que ya construye el motor de reglas
     * ({@code CapabilityWeekBuilder}: con LOSE_WEIGHT, cardio en cada sesion);
     * los dos bloques por sesion siguen siendo una preferencia, no se validan.
     * Solo para LOSE_WEIGHT: los demas objetivos siguen con la regla semanal.
     * <p>
     * FACTIBLE EN ESA SESION: hay un ejercicio elegible con dosis de CARDIO que
     * esa sesion admite, con el mismo criterio de enfoque y recuperacion que
     * usa el motor de reglas. Repetir un ejercicio en dias no consecutivos
     * esta permitido, asi que con uno solo basta. No se fuerza nada: si no hay
     * cardio que la sesion admita, esa sesion no se exige.
     */
    private List<String> cardioPorSesion(PlanDraft draft, PlanGenerationContext context,
                                         Map<Long, CandidateExercise> eligible,
                                         main.web.services.fitsense.configuration.domain.model.valueobjects.PrescriptionParams.RepLimits limites) {
        String objetivo = context.profile().goalType();
        if (!"LOSE_WEIGHT".equals(objetivo) || draft == null || draft.workouts() == null) return List.of();
        String nivel = context.profile().fitnessLevel();
        var cardio = eligible.values().stream()
                .filter(c -> CapabilityPrescriptionPolicy.admisibles(c, objetivo, nivel)
                        .contains(GoalCapabilityPolicy.Capability.CARDIO))
                .filter(c -> CapabilityPrescriptionPolicy.dosis(GoalCapabilityPolicy.Capability.CARDIO, c, objetivo,
                        nivel, limites).isPresent())
                .toList();
        if (cardio.isEmpty()) return List.of();

        var faltas = new ArrayList<String>();
        for (var workout : draft.workouts()) {
            if (workout == null || workout.exercises() == null) continue;
            boolean tieneCardio = workout.exercises().stream().anyMatch(e -> {
                var c = e == null ? null : eligible.get(e.exerciseId());
                return c != null && CapabilityPrescriptionPolicy.acreditada(e, c, objetivo, nivel, limites)
                        .filter(cap -> cap == GoalCapabilityPolicy.Capability.CARDIO).isPresent();
            });
            if (tieneCardio) continue;
            var zonasVecinas = zonasDeDiasVecinos(draft, workout, eligible);
            boolean factible = cardio.stream().anyMatch(c -> !ZONAS_CON_RECUPERACION.contains(c.bodyPartCode())
                    || ((workout.focus() == null || workout.focus().accepts(c.bodyPartCode()))
                    && !zonasVecinas.contains(c.bodyPartCode())));
            if (factible)
                faltas.add(("V25: el entrenamiento del %s no incluye CARDIO, la capacidad principal de LOSE_WEIGHT, "
                        + "y en esa sesion es factible. Pon al menos una colocacion de CARDIO (mejor dos) con su "
                        + "dosis antes que la fuerza; un ejercicio de cardio puede repetirse en dias no consecutivos.")
                        .formatted(workout.scheduledDate()));
        }
        return faltas;
    }

    /**
     * V25 · INCREASE_STRENGTH: LOS CUATRO SLOTS DE 4C CON DOSIS DE FUERZA EN LA SEMANA.
     * <p>
     * Decision del 26/10/2026, tras el plan 58 (fuerza solo en tres slots, la
     * bisagra de cadera con dosis de hipertrofia). Es la regla con la que
     * construye el motor de reglas: los slots que la semana aun no cubre van
     * primero, con dosis de FUERZA. No se valida cuantos por sesion ni
     * ninguna proporcion frente a la hipertrofia.
     * <p>
     * FACTIBILIDAD CONJUNTA. Un slot se exige si hay un ejercicio elegible de
     * ese slot con dosis de STRENGTH (el conjunto elegible ya aplica
     * equipamiento, seguridad y bloqueos). Y la semana tiene que poder alojar
     * todos los exigibles: si sus sesiones admiten menos colocaciones de
     * fuerza que slots factibles (3 por sesion de menos de 30 minutos, 4 si
     * no, como el motor de reglas), solo se exigen tantos slots distintos
     * como quepan. Dos slots pueden ir en la misma sesion, asi que la
     * recuperacion entre dias no impide cubrirlos.
     */
    private List<String> slotsConFuerza(PlanDraft draft, PlanGenerationContext context,
                                        Map<Long, CandidateExercise> eligible,
                                        main.web.services.fitsense.configuration.domain.model.valueobjects.PrescriptionParams.RepLimits limites) {
        String objetivo = context.profile().goalType();
        if (!"INCREASE_STRENGTH".equals(objetivo) || draft == null || draft.workouts() == null) return List.of();
        String nivel = context.profile().fitnessLevel();
        var fuerza = GoalCapabilityPolicy.Capability.STRENGTH;

        var factibles = EnumSet.noneOf(MandatorySlot.class);
        for (var c : eligible.values())
            if (CapabilityPrescriptionPolicy.admisibles(c, objetivo, nivel).contains(fuerza)
                    && CapabilityPrescriptionPolicy.dosis(fuerza, c, objetivo, nivel, limites).isPresent())
                MandatorySlot.of(c).ifPresent(factibles::add);
        if (factibles.isEmpty()) return List.of();

        var cubiertos = EnumSet.noneOf(MandatorySlot.class);
        int sesiones = 0;
        for (var workout : draft.workouts()) {
            if (workout == null || workout.exercises() == null) continue;
            sesiones++;
            for (var e : workout.exercises()) {
                var c = e == null ? null : eligible.get(e.exerciseId());
                if (c != null && CapabilityPrescriptionPolicy.acreditada(e, c, objetivo, nivel, limites)
                        .filter(cap -> cap == fuerza).isPresent())
                    MandatorySlot.of(c).ifPresent(cubiertos::add);
            }
        }
        int porSesion = context.effectiveSessionMinutes() < 30 ? 3 : 4;
        int exigibles = Math.min(factibles.size(), sesiones * porSesion);
        var cubiertosFactibles = EnumSet.copyOf(cubiertos);
        cubiertosFactibles.retainAll(factibles);
        if (cubiertosFactibles.size() >= exigibles) return List.of();

        var faltan = EnumSet.copyOf(factibles);
        faltan.removeAll(cubiertos);
        return faltan.stream()
                .map(slot -> ("V25: el slot %s no tiene ninguna colocacion con dosis de STRENGTH en la semana, y es "
                        + "factible. Cubrelo con un ejercicio de ese slot con dosis STRENGTH, no HYPERTROPHY: un "
                        + "slot trabajado solo con dosis de hipertrofia no cuenta como fuerza.").formatted(slot))
                .toList();
    }

    /** Zonas con recuperacion trabajadas el dia anterior o el siguiente. */
    private static Set<String> zonasDeDiasVecinos(PlanDraft draft, PlanDraft.DraftWorkout workout,
                                                  Map<Long, CandidateExercise> eligible) {
        var zonas = new HashSet<String>();
        if (workout.scheduledDate() == null) return zonas;
        for (var otro : draft.workouts()) {
            if (otro == null || otro == workout || otro.scheduledDate() == null || otro.exercises() == null) continue;
            long distancia = Math.abs(otro.scheduledDate().toEpochDay() - workout.scheduledDate().toEpochDay());
            if (distancia != 1) continue;
            for (var e : otro.exercises()) {
                var c = e == null ? null : eligible.get(e.exerciseId());
                if (c != null && ZONAS_CON_RECUPERACION.contains(c.bodyPartCode())) zonas.add(c.bodyPartCode());
            }
        }
        return zonas;
    }

    /**
     * V24 · DOSIS Y TIEMPO DE 4D v2, solo para {@code GAIN_MUSCLE}.
     * <p>
     * Tres comprobaciones, las tres simetricas: valen igual para un borrador de
     * IA y para uno del motor de reglas.
     * <ol>
     *   <li><b>Dosis por capacidad.</b> La capacidad la asigna
     *       {@link MvpDosePolicy} desde el {@code movement_pattern} del catalogo,
     *       NO desde las repeticiones del borrador.</li>
     *   <li><b>Maximo de colocaciones por dia</b> -seis, por
     *       {@code MvpDosePolicy.MAX_COLOC_DIA}- aunque el tiempo sobre.</li>
     *   <li><b>Presupuesto del dia</b> con la formula de 4D v2 §5, que NO es la
     *       de {@link SessionDurationEstimator}: 4D v2 cobra el descanso despues
     *       de cada serie y el estimador solo entre series. Son dos preguntas
     *       distintas y las dos se quedan. V17 pregunta si la IA declaro
     *       honestamente; esta pregunta si cabe en el presupuesto metodologico.</li>
     * </ol>
     * LO QUE NO COMPRUEBA, y conviene que se vea:
     * <ul>
     *   <li><b>El reparto del 25 % de fuerza secundaria.</b> 4D v2 §6 da la
     *       fraccion pero no su denominador. Es objetivo de prescripcion, no
     *       condicion de rechazo, y de un plan que pase esto NO se puede afirmar
     *       que lo cumplio.</li>
     *   <li><b>El RIR.</b> No hay columna donde guardarlo.</li>
     *   <li><b>Los ejercicios por lado.</b> El detector de 4D v2 §5.2 esta
     *       declarado provisional, asi que el coste se calcula SIN duplicar la
     *       ejecucion. El presupuesto queda por tanto OPTIMISTA: un ejercicio
     *       por lado no detectado cuesta mas de lo que esta cuenta dice.</li>
     *   <li><b>Las colocaciones de DURACION.</b> 4D v2 §4 deja
     *       {@code MOBILITY} y {@code LOCOMOTOR} fuera del MVP y su regla 4 es
     *       una abstencion: sin unidad fiable no se prescribe automaticamente.
     *       Aqui se cuentan y se registran, no se les inventa una dosis.</li>
     * </ul>
     */
    private void validateMvpDose(PlanDraft draft, PlanGenerationContext context,
                                 Map<Long, CandidateExercise> eligible, ConservationReport conservation,
                                 List<String> problems) {
        if (!doseEnforcement.mide()) return;
        if (context.profile() == null || !MvpDosePolicy.aplicaA(context.profile().goalType())) return;
        if (draft == null || draft.workouts() == null) return;

        // La duracion efectiva, la misma con la que se construye el prompt: una
        // orden que fuerza sesiones mas cortas baja el tope y el presupuesto.
        // Con el perfil, una orden de 45 en un perfil de 60 dejaba pasar siete
        // colocaciones que el prompt habia limitado a seis.
        int minutos = context.effectiveSessionMinutes();
        double presupuesto = MvpDosePolicy.presupuestoDiarioSegundos(minutos);
        var incumplimientos = new ArrayList<String>();
        int abstenciones = 0;
        int comprobadas = 0;

        for (var workout : draft.workouts()) {
            if (workout == null || workout.exercises() == null) continue;

            // 2: tope de colocaciones, antes que el tiempo. Nueve ejercicios en
            // un dia no se aceptan ni aunque quepan.
            int colocaciones = workout.exercises().size();
            int tope = MvpDosePolicy.maxColocaciones(minutos);
            if (colocaciones > tope)
                incumplimientos.add(("V24: el entrenamiento del %s tiene %d colocaciones y el maximo "
                        + "de 4D v3.1 para sesiones de %d minutos es %d.")
                        .formatted(workout.scheduledDate(), colocaciones, minutos, tope));

            double costeDia = 0;
            for (var exercise : workout.exercises()) {
                if (exercise == null) continue;

                // Abstencion de unidad: 4D v2 §4, regla 4.
                if (exercise.prescriptionType() != PrescriptionType.SETS_REPS) {
                    abstenciones++;
                    continue;
                }

                var candidate = eligible == null ? null : eligible.get(exercise.exerciseId());
                var dosis = candidate == null ? Optional.<MvpDosePolicy.Dosis>empty()
                        : MvpDosePolicy.dosisPara(MandatorySlot.of(candidate),
                        candidate.bodyPartCode());
                if (dosis.isEmpty()) {
                    // Sin patron no hay capacidad asignable. La validacion 1 ya
                    // rechaza un ejercicio que no este en el conjunto elegible;
                    // aqui no se duplica el motivo ni se inventa una dosis.
                    abstenciones++;
                    continue;
                }

                var exigida = dosis.get();
                comprobadas++;

                short series = exercise.plannedSets() == null ? -1 : exercise.plannedSets();
                short reps = exercise.plannedReps() == null ? -1 : exercise.plannedReps();
                short descanso = exercise.restSeconds() == null ? -1 : exercise.restSeconds();

                // Progresion autorizada por V8C en esta colocacion y dentro de su rango (D-P3).
                // La generacion no trae autorizaciones. El presupuesto se cobra con la dosis REAL.
                var origenAutorizado = conservation.autorizacionDe(workout.scheduledDate(), exercise);
                boolean autorizada = origenAutorizado.isPresent()
                        && ProgresionDosisPolicy.admite4D(exigida, exercise, origenAutorizado.get());
                costeDia += autorizada
                        ? new MvpDosePolicy.Dosis(series, reps, descanso, exigida.capacidad(), exigida.relleno())
                        .costeSegundos(false)
                        : exigida.costeSegundos(false);

                if (!autorizada && (series != exigida.series() || reps != exigida.reps()
                        || descanso != exigida.descansoSegundos()))
                    incumplimientos.add(("V24: el ejercicio %d del %s lleva %dx%d con %d s de descanso "
                            + "y 4D v2 exige %s. Ajusta la dosis exactamente a la de su capacidad; "
                            + "si no cabe en el dia, quita una colocacion en vez de recortar la dosis.")
                            .formatted(exercise.exerciseId(), workout.scheduledDate(),
                                    series, reps, descanso, exigida.describe()));
            }

            // 3: presupuesto del dia con la formula de 4D v2 §5.
            if (costeDia > presupuesto)
                incumplimientos.add(("V24: el entrenamiento del %s cuesta %.1f minutos por la formula "
                        + "de 4D v2 §5 y el presupuesto del dia es %.1f (%.0f %% de %d minutos). "
                        + "Quita una colocacion.")
                        .formatted(workout.scheduledDate(), costeDia / 60.0, presupuesto / 60.0,
                                MvpDosePolicy.FRACCION_UTIL * 100, minutos));
        }

        // Visible siempre que mida, como la linea de cobertura 4C: un filtro que
        // no se ve cuesta una medicion entera (INC-4C-03).
        log.info("DOSIS_4D usuario {} semana {}: objetivo {} · {} comprobadas · {} abstenciones "
                        + "(duracion o sin patron) · {} incumplimientos · modo {} · {}",
                context.userId(), context.weekNumber(), context.profile().goalType(),
                comprobadas, abstenciones, incumplimientos.size(), doseEnforcement,
                MvpDosePolicy.describeTabla(minutos));

        if (incumplimientos.isEmpty()) return;

        if (!doseEnforcement.bloquea()) {
            log.warn("Dosis de 4D v2 incumplida para el usuario {} y NO se rechaza (modo {}, "
                            + "origen {}): {}",
                    context.userId(), doseEnforcement, draft.source(), incumplimientos);
            return;
        }
        problems.addAll(incumplimientos);
    }

    /** Validaciones 1, 2, 3, 6 y 12. */
    private void validateExercisesAndDays(PlanDraft draft, PlanGenerationContext context,
                                          Map<Long, CandidateExercise> eligible,
                                          List<String> problems) {
        // 1: solo se puede indicar lo que esta en available_exercises.
        // 6: ningun ejercicio supera la dificultad maxima efectiva.
        int maxDifficulty = context.effectiveMaxDifficulty();
        draft.workouts().stream()
                .flatMap(workout -> workout.exercises().stream())
                .forEach(exercise -> {
                    var candidate = eligible.get(exercise.exerciseId());
                    if (candidate == null)
                        problems.add("V1: el ejercicio %d no esta en available_exercises."
                                .formatted(exercise.exerciseId()));
                    else {
                        if (candidate.difficulty() > maxDifficulty)
                            problems.add("V6: el ejercicio %d tiene dificultad %d y el maximo es %d."
                                    .formatted(exercise.exerciseId(), candidate.difficulty(), maxDifficulty));

                        // 7 (migracion V18, principio 7): el tipo lo fija el catalogo, no
                        // el generador. Sin esto llegaron un estiramiento a 3x7
                        // y un planche a 2x60 s. El motor de reglas cumple por
                        // construccion: copia defaultPrescription.
                        if (exercise.prescriptionType() != null
                                && candidate.defaultPrescription() != null
                                && exercise.prescriptionType() != candidate.defaultPrescription())
                            problems.add("V7: el ejercicio %d se prescribio como %s y debe ir como %s."
                                    .formatted(exercise.exerciseId(), exercise.prescriptionType(),
                                            candidate.defaultPrescription()));
                    }
                });

        // 2: la cantidad de dias coincide con days_per_week.
        int expectedDays = context.effectiveDaysPerWeek();
        if (draft.workouts().size() != expectedDays)
            problems.add("V2: se esperaban %d entrenamientos y el plan trae %d."
                    .formatted(expectedDays, draft.workouts().size()));

        // 3: las fechas caen en available_days y dentro de la semana.
        // 12: no hay dos sesiones el mismo dia.
        var available = new HashSet<>(context.profile().availableDays());
        var seenDates = new HashSet<LocalDate>();
        for (var workout : draft.workouts()) {
            var date = workout.scheduledDate();
            if (!available.contains((short) date.getDayOfWeek().getValue()))
                problems.add("V3: el dia %s no esta en available_days.".formatted(date));
            if (date.isBefore(context.weekStartDate()) || date.isAfter(context.weekEndDate()))
                problems.add("V3: el dia %s cae fuera de la semana planificada.".formatted(date));
            if (!seenDates.add(date))
                problems.add("V12: hay dos entrenamientos el mismo dia (%s).".formatted(date));
        }

        validateConsecutiveFocus(draft, problems);
    }

    /**
     * Validacion 13: no se repite el mismo enfoque en dias consecutivos.
     * <p>
     * Se compara por fecha, no por posicion en la lista: dos sesiones UPPER_BODY
     * el lunes y el jueves son correctas; lunes y martes, no.
     */
    private void validateConsecutiveFocus(PlanDraft draft, List<String> problems) {
        var ordered = draft.workouts().stream()
                .sorted(Comparator.comparing(PlanDraft.DraftWorkout::scheduledDate))
                .toList();

        for (int i = 1; i < ordered.size(); i++) {
            var previous = ordered.get(i - 1);
            var current = ordered.get(i);
            boolean consecutiveDays = previous.scheduledDate().plusDays(1)
                    .equals(current.scheduledDate());
            if (consecutiveDays && previous.focus() == current.focus())
                problems.add("V13: el enfoque %s se repite en dias consecutivos (%s y %s)."
                        .formatted(current.focus(), previous.scheduledDate(), current.scheduledDate()));
        }
    }

    /**
     * Validacion 15: cada entrenamiento debe cubrir de verdad su enfoque.
     * <p>
     * El 20.3 pide "al menos uno de cada body_part del enfoque". El motor de
     * reglas lo cumple por construccion, asi que nunca se convirtio en
     * validacion, y la IA no tenia nada que la obligara: en la primera prueba
     * con el catalogo completo devolvio un FULL_BODY con seis ejercicios de
     * biceps y triceps. Paso las trece validaciones porque ninguna miraba la
     * distribucion.
     * <p>
     * Esto importa mas alla de la calidad del entrenamiento. Un plan que no
     * encaja con lo que el participante espera baja su adherencia, y el sistema
     * interpreta esa caida como falta de compromiso: reduce volumen cuando el
     * problema era la seleccion. La validacion 8 mide si el generador OBEDECE
     * la orden de volumen; esta mide si lo que produce tiene sentido.
     * <p>
     * El minimo se calcula contra lo que el conjunto elegible permite: si el
     * usuario solo tiene material para dos grupos del enfoque, exigir tres
     * dejaria la semana sin plan.
     * <p>
     * PASO 2: los COMPLEMENTOS DE CAPACIDAD -cardio y movilidad que solo entran
     * por 4A v6, ver {@link CatalogEligibilityPolicy}- quedan fuera de V15: no
     * son grupo ajeno de ningun enfoque y tampoco cuentan para cubrirlo. Asi el
     * catalogo y V15 dicen lo mismo, y una sesion de estiramientos no pasa por
     * trabajo de piernas. Todo lo que ya era elegible se trata igual que antes.
     */
    private void validateFocusCoverage(PlanDraft draft, PlanGenerationContext context,
                                       Map<Long, CandidateExercise> eligible,
                                       List<String> problems) {
        var complementos = CatalogEligibilityPolicy.complementos(eligible.values(),
                context.profile() == null ? null : context.profile().goalType());

        for (var workout : draft.workouts()) {
            var esperados = workout.focus().bodyPartCodes();

            // Cuantos grupos del enfoque tienen material disponible.
            long disponibles = esperados.stream()
                    .filter(code -> eligible.values().stream()
                            .filter(candidate -> !complementos.contains(candidate.exerciseId()))
                            .anyMatch(candidate -> code.equals(candidate.bodyPartCode())))
                    .count();

            var deTrabajo = workout.exercises().stream()
                    .filter(exercise -> !complementos.contains(exercise.exerciseId()))
                    .toList();

            var cubiertos = deTrabajo.stream()
                    .map(exercise -> eligible.get(exercise.exerciseId()))
                    .filter(java.util.Objects::nonNull)
                    .map(CandidateExercise::bodyPartCode)
                    .filter(esperados::contains)
                    .collect(Collectors.toSet());

            // Cubrir el enfoque no basta si ademas se cuelan grupos ajenos. En
            // la prueba salio un PULL con dos ejercicios de pecho, que es
            // empuje y no traccion: la comprobacion de cobertura no lo veia
            // porque solo contaba cuantos grupos del enfoque estaban presentes.
            //
            // FULL_BODY queda exento: su lista es orientativa y no tiene sentido
            // rechazar un cuerpo completo por incluir gemelos o antebrazos.
            if (workout.focus() != WorkoutFocus.FULL_BODY) {
                var ajenos = deTrabajo.stream()
                        .map(exercise -> eligible.get(exercise.exerciseId()))
                        .filter(java.util.Objects::nonNull)
                        .map(CandidateExercise::bodyPartCode)
                        .filter(code -> !esperados.contains(code))
                        .distinct()
                        .toList();

                if (!ajenos.isEmpty())
                    problems.add(("V15: el entrenamiento del %s tiene enfoque %s pero incluye "
                            + "ejercicios de %s. Ese enfoque solo admite: %s.")
                            .formatted(workout.scheduledDate(), workout.focus(),
                                    ajenos, esperados));
            }

            // Se pide cubrir todo lo que se pueda, con tope en 3: exigir los
            // cinco grupos de FULL_BODY obligaria a sesiones de cinco
            // ejercicios minimo, y con REDUCE_VOLUME puede haber solo dos.
            int minimo = (int) Math.min(2, Math.min(disponibles, deTrabajo.size()));

            if (cubiertos.size() < minimo)
                problems.add(("V15: el entrenamiento del %s tiene enfoque %s y solo trabaja %s. "
                        + "Debe cubrir al menos %d grupos distintos de: %s.")
                        .formatted(workout.scheduledDate(), workout.focus(),
                                cubiertos.isEmpty() ? "ningun grupo del enfoque" : cubiertos,
                                minimo, esperados));

            // Ademas, ningun grupo puede acaparar mas de la mitad de la sesion
            // cuando el enfoque abarca varios.
            if (esperados.size() >= 3 && deTrabajo.size() >= 4) {
                var porGrupo = deTrabajo.stream()
                        .map(exercise -> eligible.get(exercise.exerciseId()))
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.groupingBy(CandidateExercise::bodyPartCode,
                                Collectors.counting()));

                porGrupo.forEach((grupo, cuantos) -> {
                    if (cuantos > deTrabajo.size() / 2)
                        problems.add(("V15: el entrenamiento del %s dedica %d de %d ejercicios a %s. "
                                + "Reparte entre los grupos del enfoque %s.")
                                .formatted(workout.scheduledDate(), cuantos,
                                        deTrabajo.size(), grupo, workout.focus()));
                });
            }
        }
    }

    /**
     * V23: COBERTURA SEMANAL DE LOS CUATRO SLOTS OBLIGATORIOS (Step 4C §5).
     * <p>
     * Es una comprobacion de SEMANA, no de sesion, y por eso no cabe dentro de
     * V15: V15 pregunta si cada entrenamiento respeta su enfoque y lo hace con
     * {@code body_part}; esta pregunta si la semana entera toca los cuatro
     * patrones de 4C y lo hace con {@code movement_pattern}. Son dos datos
     * distintos y una no implica la otra: un FULL_BODY con sentadilla, press y
     * remo pasa V15 y se deja LOWER_HIP sin tocar.
     * <p>
     * NO MIRA LA DOSIS. Ni series, ni repeticiones, ni descanso, ni carga: solo
     * si cada slot aparece al menos una vez en la semana.
     * <p>
     * LO QUE SE EXIGE NO ES SIEMPRE LOS CUATRO. Se exige la interseccion de los
     * cuatro slots con los que el conjunto elegible puede cubrir. Exigir un slot
     * que el conjunto elegible no contiene rechazaria el plan por algo que el
     * generador no puede arreglar, y el final de ese camino es
     * {@code PlanGenerationFailedException}: semana sin plan y adherencia NULL.
     * El propio pipeline dice que esa perdida de datos es irrecuperable. Que un
     * slot este vacio es un hallazgo de la comprobacion PREVIA, en
     * {@code prepare()}, no un motivo para dejar al participante sin semana.
     * <p>
     * El modo lo decide {@code fitsense.coverage.enforcement}. En OBSERVE la
     * cuenta se registra y no se rechaza nada, que es como hay que medir esto
     * durante la primera ronda antes de activarlo.
     */
    private void validateWeeklySlotCoverage(PlanDraft draft, PlanGenerationContext context,
                                            Map<Long, CandidateExercise> eligible,
                                            List<String> problems) {
        if (!coverageEnforcement.mide()) return;
        // 4A v6: LOSE_WEIGHT tiene CARDIO como unica principal; sus cuatro
        // slots resistidos no se le exigen. Los objetivos cuya principal es
        // estructural (fuerza o hipertrofia) siguen igual.
        if (context.profile() != null
                && !GoalFeasibilityPolicy.exigeSlotsResistidos(context.profile().goalType())) {
            log.info("Cobertura 4C del usuario {} semana {}: no se exige a {} (4A v6)",
                    context.userId(), context.weekNumber(), context.profile().goalType());
            return;
        }

        var disponible = SlotCoveragePolicy.ofEligible(context.availableExercises());
        var cubierto = SlotCoveragePolicy.ofDraft(draft, eligible);
        var exigibles = SlotCoveragePolicy.requiredOf(disponible);

        var faltan = exigibles.stream().filter(slot -> cubierto.count(slot) == 0).toList();

        // Si la semana no tiene posiciones para todos los slots exigibles, no se
        // puede cumplir por aritmetica, no por mala eleccion. REDUCE_VOLUME con
        // REDUCE_DAYS deja dos sesiones de dos ejercicios: cuatro posiciones.
        int posiciones = SlotCoveragePolicy.exercisePositions(draft);
        boolean caben = posiciones >= exigibles.size();

        log.info("Cobertura 4C del usuario {} semana {}: elegible [{}] · borrador [{}] · "
                        + "exigibles {} · faltan {} · posiciones {} · modo {}",
                context.userId(), context.weekNumber(), disponible.describe(),
                cubierto.describe(), exigibles, faltan, posiciones, coverageEnforcement);

        // DESGLOSE POR DIA. Va DESPUES de generar y solo despues: antes de
        // generar no existe reparto semanal que medir.
        //
        // Es observacion pura y no entra en `problems`: que un slot se
        // concentre en un solo dia NO es un incumplimiento de 4C. Cubrir los
        // cuatro patrones y repartirlos bien son dos cosas distintas, y hasta
        // reunir varias generaciones no hay con que decidir si la segunda
        // merece una regla.
        //
        // Lo motiva el plan 4 del 12/10/2026: LOWER_HIP con 2 colocaciones, las
        // dos el mismo dia. La linea de arriba lo daba por bueno y habia que
        // cruzar el JSON a mano para verlo.
        log.info("Usuario {} · {}", context.userId(),
                SlotCoveragePolicy.distributionOf(draft, eligible)
                        .describe(context.weekNumber(), coverageEnforcement.name()));

        if (faltan.isEmpty()) return;

        boolean bloquea = coverageEnforcement.bloqueaA(draft.source())
                && (caben || coverageEnforcement == SlotCoverageEnforcement.ENFORCE_ALWAYS);

        if (!bloquea) {
            log.warn("Cobertura 4C incumplida para el usuario {} y NO se rechaza "
                            + "(modo {}, origen {}, caben={}): faltan {}. "
                            + "Slots disponibles en el conjunto elegible: {}.",
                    context.userId(), coverageEnforcement, draft.source(), caben, faltan,
                    disponible.describe());
            return;
        }

        problems.add(("V23: la semana no cubre %s. Step 4C exige que la semana toque los cuatro "
                + "patrones obligatorios cuando hay ejercicios para ellos. Hay %s elegibles para "
                + "esos patrones. Cambia algun ejercicio por uno de ese patron sin tocar series, "
                + "repeticiones ni descansos: el volumen total no debe cambiar.")
                .formatted(
                        faltan.stream().map(Enum::name).collect(Collectors.joining(", ")),
                        faltan.stream()
                                .map(slot -> slot.name() + "=" + disponible.count(slot))
                                .collect(Collectors.joining(", "))));
    }

    /** Validaciones 4, 5, 7, 9 y 10. */
    private void validateSessions(PlanDraft draft, PlanGenerationContext context,
                                  List<String> problems) {
        int ceiling = context.profile().maxSessionMinutes();

        for (var workout : draft.workouts()) {
            // 4: ninguna sesion excede session_minutes + 15 %.
            if (workout.expectedDurationMinutes() > ceiling)
                problems.add("V4: el entrenamiento del %s dura %d minutos y el techo es %d."
                        .formatted(workout.scheduledDate(), workout.expectedDurationMinutes(), ceiling));

            // 5: ninguna sesion tiene menos de 2 ejercicios.
            if (workout.exercises().size() < MIN_EXERCISES_PER_SESSION)
                problems.add("V5: el entrenamiento del %s trae %d ejercicios y el minimo es %d."
                        .formatted(workout.scheduledDate(), workout.exercises().size(),
                                MIN_EXERCISES_PER_SESSION));

            for (var exercise : workout.exercises()) {
                validatePrescription(exercise, problems);
            }
        }
    }

    private void validatePrescription(PlanDraft.DraftExercise exercise, List<String> problems) {
        if (exercise.prescriptionType() == null) {
            problems.add("V7: el ejercicio %d no declara prescription_type."
                    .formatted(exercise.exerciseId()));
            return;
        }

        if (exercise.prescriptionType() == PrescriptionType.SETS_REPS) {
            // 7: campos obligatorios del tipo. 9: pisos de series y repeticiones.
            if (exercise.plannedSets() == null || exercise.plannedReps() == null) {
                problems.add("V7: el ejercicio %d es SETS_REPS y le faltan series o repeticiones."
                        .formatted(exercise.exerciseId()));
                return;
            }
            if (exercise.plannedSets() < MIN_SETS)
                problems.add("V9: el ejercicio %d baja de %d series."
                        .formatted(exercise.exerciseId(), MIN_SETS));
            if (exercise.plannedReps() < MIN_REPS)
                problems.add("V9: el ejercicio %d baja de %d repeticiones."
                        .formatted(exercise.exerciseId(), MIN_REPS));
            return;
        }

        // 10: piso de los ejercicios de duracion.
        if (exercise.plannedDurationSeconds() == null)
            problems.add("V7: el ejercicio %d es DURATION y no declara segundos."
                    .formatted(exercise.exerciseId()));
        else if (exercise.plannedDurationSeconds() < MIN_DURATION_SECONDS)
            problems.add("V10: el ejercicio %d baja de %d segundos."
                    .formatted(exercise.exerciseId(), MIN_DURATION_SECONDS));
    }

    /**
     * Validacion 8: si hay ajuste, el volumen semanal cae entre target_volume_min
     * y target_volume_max.
     * <p>
     * Es la validacion que sostiene la tesis. El volumen se RECALCULA sumando las
     * prescripciones; el total_volume que declara la IA no se usa mas que para
     * detectar que se equivoco al contar.
     */
    private void validateVolumeTarget(PlanDraft draft, PlanGenerationContext context,
                                      int durationToRepsDivisor, List<String> problems) {
        var adjustment = context.adjustment();
        if (adjustment == null || !adjustment.isActive()) return;

        int actual = draft.volume(durationToRepsDivisor);
        if (actual < adjustment.targetVolumeMin() || actual > adjustment.targetVolumeMax())
            problems.add("V8: el volumen semanal es %d y debe caer entre %d y %d (objetivo %d)."
                    .formatted(actual, adjustment.targetVolumeMin(),
                            adjustment.targetVolumeMax(), adjustment.targetVolume()));

        if (draft.declaredTotalVolume() != null && draft.declaredTotalVolume() != actual)
            problems.add("V8: total_volume declarado %d no coincide con el real %d."
                    .formatted(draft.declaredTotalVolume(), actual));
    }

    /**
     * Validacion 11 (principio 8, P-1.2): el peso SUGERIDO.
     * <p>
     * target_load_kg es solo una sugerencia para el participante. No entra en la
     * adherencia, el volumen ni el analisis: esta validacion existe para que la
     * sugerencia sea segura, no porque el peso se mida.
     * <ul>
     *   <li>Sin peso anotado la semana anterior, va null: sin dato real, un kilo
     *       prescrito es falsa precision.</li>
     *   <li>Mantener o bajar el peso usado: siempre permitido.</li>
     *   <li>Subir: solo si hizo TODAS las repeticiones, como maximo
     *       max_load_increase_pct (10 %, ACSM 2009), y sin subir series ni
     *       repeticiones de ese ejercicio.</li>
     *   <li>LOWER_LOAD: ningun peso puede superar el usado.</li>
     *   <li>Ejercicios por duracion: sin peso.</li>
     * </ul>
     * El RPE NO se valida aqui: la escala no esta instrumentada y queda como
     * instruccion al modelo.
     */
    private void validateLoads(PlanDraft draft, PlanGenerationContext context,
                               List<String> problems) {
        var previousWeek = context.previousWeek();
        boolean lowerLoad = context.adjustment() != null && context.adjustment().clearsLoad();
        int maxIncreasePct = context.prescription() == null ? 10
                : context.prescription().maxLoadIncreasePctOrDefault();

        draft.workouts().stream()
                .flatMap(workout -> workout.exercises().stream())
                .filter(exercise -> exercise.targetLoadKg() != null)
                .forEach(exercise -> {
                    long id = exercise.exerciseId();
                    BigDecimal target = exercise.targetLoadKg();

                    if (exercise.prescriptionType() == PrescriptionType.DURATION) {
                        problems.add("V11: el ejercicio %d es por duracion y no lleva peso.".formatted(id));
                        return;
                    }

                    var reference = previousWeek == null ? Optional.<PreviousWeekSummary.LoadReference>empty()
                            : previousWeek.lastLoadFor(id);
                    if (reference.isEmpty()) {
                        problems.add(("V11: el ejercicio %d sugiere %s kg y la persona no anoto peso "
                                + "la semana anterior; debe ir en null.").formatted(id, target));
                        return;
                    }

                    BigDecimal used = reference.get().loadKg();
                    if (target.compareTo(used) <= 0) return;

                    if (lowerLoad) {
                        problems.add("V11: el ajuste pide bajar carga y el ejercicio %d sube de %s a %s kg."
                                .formatted(id, used, target));
                        return;
                    }
                    if (!reference.get().fullyCompleted())
                        problems.add(("V11: el ejercicio %d sube de %s a %s kg y la semana anterior no "
                                + "hizo todas las repeticiones.").formatted(id, used, target));

                    BigDecimal ceiling = used.multiply(BigDecimal.valueOf(100 + maxIncreasePct))
                            .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
                    if (target.compareTo(ceiling) > 0)
                        problems.add("V11: el ejercicio %d sube de %s a %s kg; el maximo es %s kg (+%d %%)."
                                .formatted(id, used, target, ceiling, maxIncreasePct));

                    boolean moreSets = exercise.plannedSets() != null && reference.get().sets() != null
                            && exercise.plannedSets() > reference.get().sets();
                    boolean moreReps = exercise.plannedReps() != null && reference.get().reps() != null
                            && exercise.plannedReps() > reference.get().reps();
                    if (moreSets || moreReps)
                        problems.add(("V11: el ejercicio %d sube el peso y tambien series o repeticiones; "
                                + "solo puede subir una cosa a la vez.").formatted(id));
                });
    }

    /**
     * Validaciones 16 y 17: las repeticiones deben caer en el limite amplio de
     * P-1.0 (antes, en el rango del objetivo), y la duracion declarada debe
     * corresponder al contenido.
     * <p>
     * V17 CAMBIO RESPECTO AL §19.3. Antes exigia que la duracion declarada no
     * bajara del 70 % de session_minutes. Esa regla castigaba al generador que
     * estima con honestidad y premiaba al que copia el numero del perfil: con un
     * ajuste de -30 %, la IA declaro 28 minutos para un contenido de ~25 y fue
     * rechazada, mientras el motor de reglas declaro 45 para un contenido de ~27
     * y paso.
     * <p>
     * El piso desaparece por completo, no solo cuando hay ajuste: la
     * prescripcion por defecto del §20.4 (5 ejercicios de 3x12 con 60 s de
     * descanso) rinde unos 28 minutos, asi que un piso del 70 % sobre 45 dejaria
     * sin plan valido al motor de reglas, que es el respaldo de ultima
     * instancia. Que la prescripcion por defecto no llene la sesion declarada es
     * un problema real, pero se resuelve revisando el §20.4 con el asesor, no
     * bloqueando la generacion.
     * <p>
     * Lo que impide un plan ridiculamente corto sigue siendo la validacion 5
     * (minimo 2 ejercicios), la 9 (minimo 2 series y 6 repeticiones, piso de
     * seguridad del backend; a la IA se le da el min_reps de cada ejercicio) y, cuando
     * hay ajuste, la 8 (el volumen objetivo).
     * <p>
     * Se salta entera si la configuracion activa no trae el bloque prescription.
     * Mejor no validar que inventar limites.
     */
    private void validatePrescriptionRanges(PlanDraft draft, PlanGenerationContext context,
                                            Map<Long, CandidateExercise> eligible,
                                            List<String> problems) {
        var prescription = context.prescription();
        if (prescription == null) return;

        // 16 (migracion V18, principios P-1.0): limite amplio por ejercicio. Las
        // repeticiones las decide el generador; aqui solo se rechaza lo absurdo.
        // Ya no hay rango por objetivo: 15 elevaciones de talon para un perfil
        // de fuerza son coherentes y el rango 4-10 las rechazaba.
        var limites = prescription.repLimits();
        if (limites != null && limites.isComplete()) {
            draft.workouts().stream()
                    .flatMap(workout -> workout.exercises().stream())
                    .filter(exercise -> exercise.prescriptionType() == PrescriptionType.SETS_REPS)
                    .filter(exercise -> exercise.plannedReps() != null)
                    .forEach(exercise -> {
                        int reps = exercise.plannedReps();
                        var candidate = eligible.get(exercise.exerciseId());
                        String zona = candidate == null ? null : candidate.bodyPartCode();
                        // Minimo por zona (migracion V20, provisional): sin esto la IA
                        // puso los 12 ejercicios de la semana a 3x10, gemelos incluidos.
                        int minimo = limites.minRepsFor(zona, context.profile().fitnessLevel());
                        if (reps < minimo || reps > limites.maxReps())
                            // Mensaje en forma de instruccion (plan 29): el reintento
                            // lo lee junto a su propuesta y tiene que saber que poner.
                            problems.add(("V16: el ejercicio %d (%s) lleva %d repeticiones y el limite "
                                    + "es de %d a %d. Ponle entre %d y %d (su min_reps es %d).")
                                    .formatted(exercise.exerciseId(), zona, reps,
                                            minimo, limites.maxReps(), minimo, limites.maxReps(), minimo));
                    });
        }

        // 16 antiguo: rango por objetivo. Solo aplica con configuraciones
        // anteriores a MVP-1.5, para revalidar planes historicos con sus reglas.
        var rango = limites != null && limites.isComplete() ? null
                : prescription.forGoal(context.profile().goalType());
        if (rango != null && rango.minReps() != null && rango.maxReps() != null) {
            draft.workouts().stream()
                    .flatMap(workout -> workout.exercises().stream())
                    .filter(exercise -> exercise.prescriptionType() == PrescriptionType.SETS_REPS)
                    .filter(exercise -> exercise.plannedReps() != null)
                    .forEach(exercise -> {
                        int reps = exercise.plannedReps();
                        if (reps < rango.minReps() || reps > rango.maxReps())
                            problems.add(("V16: el ejercicio %d lleva %d repeticiones y el objetivo "
                                    + "%s exige entre %d y %d.")
                                    .formatted(exercise.exerciseId(), reps,
                                            context.profile().goalType(),
                                            rango.minReps(), rango.maxReps()));
                    });
        }

        // 17: coherencia entre lo declarado y el contenido.
        int tolerancia = prescription.durationTolerancePctOrDefault();
        for (var workout : draft.workouts()) {
            int estimada = durationEstimator.estimateMinutes(workout, prescription);
            if (estimada <= 0) continue;

            double desvio = Math.abs(workout.expectedDurationMinutes() - estimada) * 100.0 / estimada;
            if (desvio > tolerancia)
                problems.add(("V17: el entrenamiento del %s declara %d minutos pero su contenido "
                        + "dura unos %d (desvio de %.0f %%, maximo %d %%). Declara lo que dure el "
                        + "contenido final, despues de corregir lo demas.")
                        .formatted(workout.scheduledDate(), workout.expectedDurationMinutes(),
                                estimada, desvio, tolerancia));
        }
    }

    /**
     * Grupos que necesitan recuperacion entre sesiones. Abdomen, brazos y
     * gemelos quedan fuera: body_part es demasiado grueso (biceps y triceps
     * comparten "upper arms") y son grupos que toleran dias seguidos. Criterio
     * de diseno declarado.
     */
    private static final Set<String> RECOVERY_BODY_PARTS =
            Set.of("chest", "back", "shoulders", "upper legs");

    /**
     * Validacion 18: recuperacion entre dias consecutivos.
     * <ul>
     *   <li>El mismo ejercicio no se repite en dias consecutivos.</li>
     *   <li>Pecho, espalda, hombros y piernas no se trabajan dos dias seguidos.</li>
     * </ul>
     * V13 solo comparaba el NOMBRE del enfoque: FULL_BODY el lunes y UPPER_BODY
     * el martes pasaban aunque repitieran pecho, espalda y hombros, y en la
     * prueba el mismo press y el mismo remo aparecieron lunes y martes.
     */
    private void validateRecovery(PlanDraft draft, Map<Long, CandidateExercise> eligible,
                                  List<String> problems) {
        var ordered = draft.workouts().stream()
                .sorted(Comparator.comparing(PlanDraft.DraftWorkout::scheduledDate))
                .toList();

        for (int i = 1; i < ordered.size(); i++) {
            var previous = ordered.get(i - 1);
            var current = ordered.get(i);
            if (!previous.scheduledDate().plusDays(1).equals(current.scheduledDate())) continue;

            var previousIds = previous.exercises().stream()
                    .map(PlanDraft.DraftExercise::exerciseId).collect(Collectors.toSet());
            current.exercises().stream()
                    .map(PlanDraft.DraftExercise::exerciseId)
                    .filter(previousIds::contains)
                    .distinct()
                    .forEach(id -> problems.add(("V18: el ejercicio %d se repite en dias consecutivos "
                            + "(%s y %s).").formatted(id, previous.scheduledDate(), current.scheduledDate())));

            var previousGroups = recoveryGroupsOf(previous, eligible);
            var shared = recoveryGroupsOf(current, eligible).stream()
                    .filter(previousGroups::contains)
                    .sorted()
                    .toList();
            if (!shared.isEmpty())
                problems.add(("V18: %s se trabaja en dias consecutivos (%s y %s). Alterna tren superior "
                        + "e inferior, o empuje y traccion.")
                        .formatted(shared, previous.scheduledDate(), current.scheduledDate()));
        }
    }

    private static Set<String> recoveryGroupsOf(PlanDraft.DraftWorkout workout,
                                                Map<Long, CandidateExercise> eligible) {
        return workout.exercises().stream()
                .map(exercise -> eligible.get(exercise.exerciseId()))
                .filter(Objects::nonNull)
                .map(CandidateExercise::bodyPartCode)
                .filter(RECOVERY_BODY_PARTS::contains)
                .collect(Collectors.toSet());
    }

    /**
     * Validacion 19: si la orden es REDUCE_VOLUME, un ejercicio que se repite de
     * la semana anterior no sube series ni repeticiones. En la prueba, con -20 %
     * ordenado, la IA subio el press de 10 a 13 y el crunch de 12 a 16.
     * <p>
     * El tope de repeticiones es el mayor entre lo prescrito antes y el minimo
     * de su zona: si el minimo por zona es nuevo, subir hasta el no es progresar.
     */
    private void validateReductionDoesNotIncrease(PlanDraft draft, PlanGenerationContext context,
                                                  Map<Long, CandidateExercise> eligible,
                                                  List<String> problems) {
        var caps = reductionCaps(context);
        if (caps.isEmpty()) return;

        draft.workouts().stream()
                .flatMap(workout -> workout.exercises().stream())
                .filter(exercise -> exercise.prescriptionType() == PrescriptionType.SETS_REPS)
                .forEach(exercise -> {
                    var cap = caps.get(exercise.exerciseId());
                    if (cap == null) return;
                    long id = exercise.exerciseId();
                    if (exercise.plannedSets() != null && exercise.plannedSets() > cap.maxSets())
                        problems.add(("V19: el ejercicio %d lleva %d series y su tope es %d (%s). "
                                + "Maximo: %d series y %d repeticiones.")
                                .formatted(id, exercise.plannedSets(), cap.maxSets(), capReason(cap),
                                        cap.maxSets(), cap.maxReps()));
                    if (exercise.plannedReps() != null && exercise.plannedReps() > cap.maxReps())
                        problems.add(("V19: el ejercicio %d lleva %d repeticiones y su tope es %d (%s). "
                                + "Maximo: %d series y %d repeticiones.")
                                .formatted(id, exercise.plannedReps(), cap.maxReps(), capReason(cap),
                                        cap.maxSets(), cap.maxReps()));
                });
    }

    private static String capReason(ReductionCap cap) {
        return "HOLD".equals(cap.reason())
                ? "no lo completo, no hay registro o el esfuerzo fue alto: no puede subir"
                : "lo completo: puede progresar un poco";
    }

    /**
     * Tope de un ejercicio que se repite en una semana de reduccion.
     *
     * @param reason COMPLETED (lo hizo completo y sin esfuerzo alto: puede
     *               progresar un poco) o HOLD (parcial, saltado, sin registro o
     *               RPE alto: no puede subir)
     */
    public record ReductionCap(long exerciseId, int maxSets, int maxReps, String reason) {}

    /** Progresion permitida en un ejercicio completado durante una semana de reduccion. */
    private static final int REDUCTION_EXTRA_SETS = 1;
    private static final int REDUCTION_EXTRA_REPS = 2;
    private static final int HIGH_SESSION_RPE = 8;

    /**
     * Topes de V19, calculados una sola vez para el validador y para el
     * snapshot (constraints.reduce_volume_caps).
     * <p>
     * AFINADO CON EL DESEMPENO (principio 9). La version anterior prohibia subir
     * en CUALQUIER ejercicio repetido. En la prueba la IA fallo dos veces solo por
     * eso: para llegar al volumen reutilizaba ejercicios de la semana anterior, y
     * sin margen rompia otra regla. Lo coherente depende de como le fue:
     * <ul>
     *   <li>HOLD: no lo completo (parcial o saltado), no hay registro, o ese dia
     *       reporto RPE 8 o mas. No puede subir series ni repeticiones.</li>
     *   <li>COMPLETED: lo hizo completo y sin esfuerzo alto. Puede mantener o
     *       progresar un poco: +1 serie y +2 repeticiones como maximo. Criterio de
     *       diseno: sobrecarga pequena y gradual. El total igual debe bajar (V8).</li>
     * </ul>
     * Si el ejercicio aparecio varias veces, basta una ocurrencia en HOLD para
     * que quede en HOLD. Repeticiones: nunca por debajo del minimo de zona y nivel.
     */
    public static Map<Long, ReductionCap> reductionCaps(PlanGenerationContext context) {
        var adjustment = context.adjustment();
        if (adjustment == null || !adjustment.has(AdjustmentType.REDUCE_VOLUME)) return Map.of();
        var previous = context.previousWeek();
        if (previous == null || previous.prescriptions() == null) return Map.of();

        Map<LocalDate, Short> rpeByDate = new HashMap<>();
        if (previous.workouts() != null)
            previous.workouts().forEach(w -> { if (w.scheduledDate() != null && w.sessionRpe() != null)
                rpeByDate.put(w.scheduledDate(), w.sessionRpe()); });

        Map<Long, Integer> sets = new HashMap<>();
        Map<Long, Integer> reps = new HashMap<>();
        Map<Long, Boolean> hold = new HashMap<>();
        for (var outcome : previous.prescriptions()) {
            if (outcome.exerciseId() == null || !"SETS_REPS".equals(outcome.prescriptionType())) continue;
            long id = outcome.exerciseId();
            if (outcome.sets() != null) sets.merge(id, (int) outcome.sets(), Math::max);
            if (outcome.reps() != null) reps.merge(id, (int) outcome.reps(), Math::max);

            Short rpe = outcome.scheduledDate() == null ? null : rpeByDate.get(outcome.scheduledDate());
            boolean completedEasy = "COMPLETED".equals(outcome.exerciseStatus())
                    && (rpe == null || rpe < HIGH_SESSION_RPE);
            hold.merge(id, !completedEasy, Boolean::logicalOr);
        }

        var bodyParts = new HashMap<Long, String>();
        context.availableExercises().forEach(c -> bodyParts.put(c.exerciseId(), c.bodyPartCode()));
        var limites = context.prescription() == null ? null : context.prescription().repLimits();
        String level = context.profile() == null ? null : context.profile().fitnessLevel();

        var caps = new TreeMap<Long, ReductionCap>();
        for (var id : sets.keySet()) {
            if (!reps.containsKey(id)) continue;
            boolean isHold = hold.getOrDefault(id, true);
            int floor = limites == null ? 0 : limites.minRepsFor(bodyParts.get(id), level);
            int maxSets = sets.get(id) + (isHold ? 0 : REDUCTION_EXTRA_SETS);
            int maxReps = Math.max(reps.get(id) + (isHold ? 0 : REDUCTION_EXTRA_REPS), floor);
            caps.put(id, new ReductionCap(id, maxSets, maxReps, isHold ? "HOLD" : "COMPLETED"));
        }
        return caps;
    }

    /**
     * Validaciones 20 y 21: duracion minima y descanso maximo.
     * <p>
     * 20. Sin orden de volumen (primera semana), el contenido de cada sesion
     * debe llegar al session_minutes_floor_pct (70 %) de los minutos que pidio la
     * persona. El parametro existia desde V12 y ninguna validacion lo usaba: en
     * la prueba, 45 minutos pedidos dieron sesiones de 22-27. Cuando hay orden
     * de volumen NO se aplica: el volumen manda y exigir ademas un piso de tiempo
     * solo podria cumplirse inflando descansos.
     * <p>
     * 21. Ningun descanso supera max_rest_seconds (180 s), para que la duracion
     * se consiga con trabajo y no con pausas.
     */
    private void validateMinimumDurationAndRest(PlanDraft draft, PlanGenerationContext context,
                                                ConservationReport conservation,
                                                List<String> problems) {
        var prescription = context.prescription();
        if (prescription == null) return;

        int maxRest = prescription.maxRestSecondsOrDefault();
        draft.workouts().stream()
                .flatMap(workout -> workout.exercises().stream())
                .filter(exercise -> exercise.restSeconds() != null && exercise.restSeconds() > maxRest)
                .forEach(exercise -> problems.add("V21: el ejercicio %d descansa %d s y el maximo es %d s."
                        .formatted(exercise.exerciseId(), exercise.restSeconds(), maxRest)));

        int floor = minimumSessionMinutes(context);
        if (floor <= 0) return;
        for (var workout : draft.workouts()) {
            // V8C: solo las sesiones autorizadas a acortarse quedan exentas de V20.
            if (conservation.fechasConDuracionReducible().contains(workout.scheduledDate()))
                continue;

            int estimada = durationEstimator.estimateMinutes(workout, prescription);
            if (estimada < floor)
                problems.add(("V20: el entrenamiento del %s dura unos %d minutos y el minimo es %d "
                        + "(%d %% de %d): le faltan unos %d minutos. Agrega un ejercicio del enfoque "
                        + "o una serie a los que ya tiene, no descanso.")
                        .formatted(workout.scheduledDate(), estimada, floor,
                                prescription.floorPct(), context.effectiveSessionMinutes(),
                                floor - estimada));
        }
    }

    /** 0 cuando no aplica: hay orden de volumen o no hay configuracion. */
    public static int minimumSessionMinutes(PlanGenerationContext context) {
        if (context.prescription() == null) return 0;
        if (context.adjustment() != null && context.adjustment().isActive()) return 0;
        return context.effectiveSessionMinutes() * context.prescription().floorPct() / 100;
    }

    /**
     * Validacion 22: en "upper arms", PUSH solo admite triceps y PULL solo
     * biceps. body_part no distingue uno de otro; target_muscle si. Con
     * target_muscle desconocido no se rechaza.
     */
    private void validateArmSplit(PlanDraft draft, Map<Long, CandidateExercise> eligible,
                                  List<String> problems) {
        for (var workout : draft.workouts()) {
            if (workout.focus() != WorkoutFocus.PUSH && workout.focus() != WorkoutFocus.PULL) continue;
            String forbidden = workout.focus() == WorkoutFocus.PUSH ? "biceps" : "triceps";
            workout.exercises().stream()
                    .map(exercise -> eligible.get(exercise.exerciseId()))
                    .filter(Objects::nonNull)
                    .filter(candidate -> "upper arms".equals(candidate.bodyPartCode()))
                    .filter(candidate -> candidate.targetMuscle() != null
                            && candidate.targetMuscle().toLowerCase(Locale.ROOT).contains(forbidden))
                    .forEach(candidate -> problems.add(("V22: el entrenamiento %s del %s incluye el ejercicio %d "
                            + "de %s; %s admite solo %s.")
                            .formatted(workout.focus(), workout.scheduledDate(), candidate.exerciseId(),
                                    forbidden, workout.focus(),
                                    workout.focus() == WorkoutFocus.PUSH ? "triceps" : "biceps")));
        }
    }

    /**
     * Si hubo errores de division (V13 o V18), se agrega la division que cumple:
     * "alterna tren superior e inferior" no le dice al modelo que dia es cual,
     * y en la prueba lo corrigio moviendo el problema al tren inferior.
     */
    private void appendSplitSuggestion(PlanGenerationContext context, List<String> problems) {
        boolean splitProblem = problems.stream().anyMatch(p -> p.startsWith("V13") || p.startsWith("V18"));
        if (!splitProblem || context.profile() == null || context.profile().availableDays() == null) return;
        var split = context.suggestedSplit();
        if (split.isEmpty()) return;
        problems.add("DIVISION: usa estas fechas y enfoques, que cumplen V13 y V18: "
                + WeeklySplitPlanner.describe(split) + ".");
    }
}