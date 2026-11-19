package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.configuration.domain.model.valueobjects.PrescriptionParams;
import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.GoalCapabilityPolicy;
import main.web.services.fitsense.planning.domain.model.valueobjects.GoalCapabilityPolicy.Capability;
import main.web.services.fitsense.planning.domain.model.valueobjects.GoalCapabilityPolicy.Role;
import main.web.services.fitsense.planning.domain.model.valueobjects.MandatorySlot;
import main.web.services.fitsense.planning.domain.model.valueobjects.MvpDosePolicy;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.model.valueobjects.PrescriptionType;

import java.util.*;

/**
 * 4D v4 · QUE CAPACIDAD DEMUESTRA CADA COLOCACION Y CON QUE DOSIS, para los
 * cuatro objetivos. Es la UNICA fuente: la usan el motor de reglas para
 * prescribir, V24 para validar, V25 para acreditar la cobertura y el prompt para
 * describirla. Dominio puro.
 * <p>
 * <b>Dos reglas, y las dos salen del catalogo, no del borrador:</b>
 * <ol>
 *   <li><b>La clase de la colocacion</b> sale del {@code movement_pattern} (V26):
 *       estructural (uno de los cuatro slots de 4C), resistido (otro patron de
 *       fuerza), cardio ({@code LOCOMOTION_CARDIO}), movilidad, equilibrio. Un
 *       ejercicio en revision de clasificacion
 *       ({@link GoalFeasibilityPolicy#CLASIFICACION_EN_REVISION}) o sin patron
 *       util no demuestra ninguna capacidad.</li>
 *   <li><b>La capacidad la demuestra la DOSIS</b>, no el ejercicio: una
 *       sentadilla a 3x5 con 150 s es fuerza; a 3x18 con 45 s, resistencia
 *       muscular. Una colocacion se acredita a la capacidad -admisible para su
 *       clase y su objetivo- cuya dosis coincide EXACTAMENTE con la prescrita.</li>
 * </ol>
 * <b>GAIN_MUSCLE NO CAMBIA.</b> Su dosis sigue siendo la de {@link MvpDosePolicy}
 * (4D v3) y su unica capacidad asignable sigue siendo {@code HYPERTROPHY}: 4D v3
 * declara {@code STRENGTH} secundaria pero no la asigna mientras no se defina el
 * denominador de su 25 %. Esta clase lo respeta en vez de decidirlo.
 * <p>
 * <b>Dosis de los demas objetivos: criterio de diseno PROVISIONAL</b>, tomado de
 * la tabla de 4D v2 §2 donde existe (fuerza 3x5/150 s, resistencia 3x18/45 s) y
 * de ACSM 2009 para principiantes (8-12 repeticiones, 1-3 series: fuerza 2x8 y
 * resistencia 2x15; el tope de dos series de {@code TargetPrescription} se
 * mantiene fuera de GAIN_MUSCLE). Cardio, movilidad y equilibrio son
 * {@code DURATION}; su intensidad se indica en la nota y NO se valida: no hay
 * columna para medirla, igual que el RIR.
 * <p>
 * <b>POWER no se prescribe.</b> Ni el catalogo ni el plan permiten acreditar
 * potencia (no hay clasificacion de ejercicios explosivos ni forma de verificar
 * la intencion de velocidad), asi que sus condiciones no se pueden comprobar y,
 * por la regla de 4A v6, su ausencia no invalida el objetivo.
 * <p>
 * Las repeticiones nunca bajan del minimo de zona y nivel de V16 ni pasan del
 * maximo: la dosis de fuerza de un ejercicio de gemelos sale a 12, no a 5.
 */
public final class CapabilityPrescriptionPolicy {

    public static final String VERSION = "4D-v4";

    /** Tope de colocaciones por dia fuera de GAIN_MUSCLE (el de V20 del motor de reglas). */
    public static final int MAX_COLOC_DIA = 8;

    /** Nota de intensidad del cardio. Orientativa: no se valida. */
    public static final String NOTA_CARDIO =
            "Intensidad moderada: RPE 5-6, deberias poder hablar con frases cortas.";
    public static final String NOTA_MOVILIDAD = "Sin rebotes, hasta tension leve, respirando.";
    public static final String NOTA_EQUILIBRIO = "Cerca de un apoyo estable.";

    private CapabilityPrescriptionPolicy() {}

    /** Clase de una colocacion, por su patron. */
    public enum Clase {
        ESTRUCTURAL, RESISTIDO, CARDIO, MOVILIDAD, EQUILIBRIO,
        /** Patron sin capacidad asignable ({@code OTHER}, nulo). */
        SIN_CAPACIDAD,
        /** En revision de clasificacion: no acredita nada. */
        EN_REVISION
    }

    public static Clase claseDe(CandidateExercise c) {
        if (c == null) return Clase.SIN_CAPACIDAD;
        if (GoalFeasibilityPolicy.enRevision(c))
            return Clase.EN_REVISION;
        String patron = c.movementPattern() == null ? null : c.movementPattern().trim().toUpperCase(Locale.ROOT);
        if (patron == null || patron.isBlank() || "OTHER".equals(patron)) return Clase.SIN_CAPACIDAD;
        return switch (patron) {
            case "LOCOMOTION_CARDIO" -> Clase.CARDIO;
            case "MOBILITY" -> Clase.MOVILIDAD;
            case "BALANCE_CONTROL" -> Clase.EQUILIBRIO;
            default -> MandatorySlot.ofMovementPattern(patron).isPresent() ? Clase.ESTRUCTURAL : Clase.RESISTIDO;
        };
    }

    /** Lo que cada clase PUEDE demostrar, por orden de preferencia. */
    public static List<Capability> demostrablesPor(Clase clase) {
        return switch (clase) {
            case ESTRUCTURAL -> List.of(Capability.STRENGTH, Capability.HYPERTROPHY, Capability.MUSCULAR_ENDURANCE);
            case RESISTIDO -> List.of(Capability.HYPERTROPHY, Capability.MUSCULAR_ENDURANCE);
            case CARDIO -> List.of(Capability.CARDIO);
            case MOVILIDAD -> List.of(Capability.MOBILITY);
            case EQUILIBRIO -> List.of(Capability.BALANCE);
            case SIN_CAPACIDAD, EN_REVISION -> List.of();
        };
    }

    /**
     * Capacidades del objetivo que se pueden ASIGNAR a una colocacion: las de la
     * matriz (principales y secundarias aplicables al nivel), menos POWER, y en
     * GAIN_MUSCLE solo HYPERTROPHY (4D v3).
     */
    public static Set<Capability> asignables(String goalType, String fitnessLevel) {
        var g = GoalCapabilityPolicy.forGoal(goalType);
        if (g.isEmpty()) return Set.of();
        if (MvpDosePolicy.aplicaA(g.get().goalType())) return EnumSet.of(Capability.HYPERTROPHY);
        var todas = EnumSet.noneOf(Capability.class);
        todas.addAll(g.get().primaries());
        todas.addAll(g.get().candidateSecondariesFor(fitnessLevel));
        todas.remove(Capability.POWER);
        return todas;
    }

    public static Optional<Role> papel(String goalType, Capability capacidad) {
        return GoalCapabilityPolicy.forGoal(goalType).flatMap(g -> g.requirementOf(capacidad))
                .map(GoalCapabilityPolicy.Requirement::role);
    }

    /** Las capacidades admisibles para ESTA colocacion, en orden: clase ∩ objetivo. */
    public static List<Capability> admisibles(CandidateExercise c, String goalType, String fitnessLevel) {
        var asignables = asignables(goalType, fitnessLevel);
        return demostrablesPor(claseDe(c)).stream().filter(asignables::contains).toList();
    }

    /**
     * Series hasta las que se puede subir una dosis de fuerza, hipertrofia o
     * resistencia SOLO para llegar al minimo de duracion (V20). Es la regla que
     * el motor de reglas ya aplicaba (ACSM 2009: 1-3 series), ahora declarada en
     * la politica para que V24 la acepte y no la invente nadie mas.
     */
    public static final short SERIES_MAX_AMPLIACION = 3;

    /**
     * Una dosis: los numeros que se prescriben y que se exigen.
     *
     * @param seriesMax hasta cuantas series admite (igual a {@code series} salvo
     *                  la ampliacion para el minimo de duracion)
     */
    public record Dosis(Capability capacidad, PrescriptionType tipo, short series, short seriesMax, Short reps,
                        Integer segundos, short descanso, String nota) {

        Dosis(Capability capacidad, PrescriptionType tipo, short series, Short reps, Integer segundos,
              short descanso, String nota) {
            this(capacidad, tipo, series, series, reps, segundos, descanso, nota);
        }

        /** La misma dosis con {@code n} series, si esta dentro de su rango. */
        public Optional<Dosis> conSeries(short n) {
            return n < series || n > seriesMax ? Optional.empty()
                    : Optional.of(new Dosis(capacidad, tipo, n, n, reps, segundos, descanso, nota));
        }

        public boolean coincideCon(PlanDraft.DraftExercise e) {
            if (e == null || e.prescriptionType() != tipo) return false;
            if (e.plannedSets() == null || e.plannedSets() < series || e.plannedSets() > seriesMax
                    || !Objects.equals(e.restSeconds(), descanso)) return false;
            return tipo == PrescriptionType.SETS_REPS ? Objects.equals(e.plannedReps(), reps)
                    : Objects.equals(e.plannedDurationSeconds(), segundos);
        }

        public PlanDraft.DraftExercise prescribir(Long exerciseId, java.math.BigDecimal cargaSugerida) {
            return tipo == PrescriptionType.SETS_REPS
                    ? new PlanDraft.DraftExercise(exerciseId, tipo, series, reps, null, cargaSugerida, descanso, nota)
                    : new PlanDraft.DraftExercise(exerciseId, tipo, series, null, segundos, null, descanso, nota);
        }

        public String describe() {
            String s = series == seriesMax ? String.valueOf(series) : series + "-" + seriesMax;
            return tipo == PrescriptionType.SETS_REPS
                    ? "%s %sx%d, descanso %d s".formatted(capacidad, s, reps, descanso)
                    : "%s %sx%d s, descanso %d s".formatted(capacidad, s, segundos, descanso);
        }
    }

    /**
     * La dosis de una capacidad para un ejercicio concreto. Vacio si esa
     * capacidad no se puede prescribir con el: clase o tipo que no la admiten
     * (fuerza en un ejercicio de duracion, cardio en uno de repeticiones).
     */
    public static Optional<Dosis> dosis(Capability capacidad, CandidateExercise c, String goalType,
                                        String fitnessLevel, PrescriptionParams.RepLimits limites) {
        if (c == null || capacidad == null || !demostrablesPor(claseDe(c)).contains(capacidad))
            return Optional.empty();
        boolean principiante = "BEGINNER".equals(fitnessLevel);
        var tipo = c.defaultPrescription() == null ? PrescriptionType.SETS_REPS : c.defaultPrescription();

        if (tipo == PrescriptionType.DURATION) {
            return switch (capacidad) {
                case CARDIO -> Optional.of(new Dosis(capacidad, tipo, (short) 3, null, principiante ? 90 : 120,
                        (short) 60, NOTA_CARDIO));
                case MOBILITY -> Optional.of(new Dosis(capacidad, tipo, (short) 2, null, 30, (short) 15, NOTA_MOVILIDAD));
                case BALANCE -> Optional.of(new Dosis(capacidad, tipo, (short) 2, null, 30, (short) 30, NOTA_EQUILIBRIO));
                // Isometricos resistidos: la dosis de duracion de siempre (20.4).
                case MUSCULAR_ENDURANCE -> Optional.of(new Dosis(capacidad, tipo, PlanDraftNormalizer.DURATION_SETS,
                        null, PlanDraftNormalizer.DURATION_SECONDS, PlanDraftNormalizer.DURATION_REST_SECONDS, null));
                default -> Optional.empty();
            };
        }

        // SETS_REPS
        if (capacidad == Capability.CARDIO) return Optional.empty();   // sin dosis verificable
        if (capacidad == Capability.MOBILITY || capacidad == Capability.BALANCE)
            return Optional.of(new Dosis(capacidad, tipo, (short) 2, reps(10, c, fitnessLevel, limites), null,
                    (short) 30, capacidad == Capability.MOBILITY ? NOTA_MOVILIDAD : NOTA_EQUILIBRIO));

        if (capacidad == Capability.HYPERTROPHY) {
            var d = MvpDosePolicy.dosisPara(MandatorySlot.of(c), c.bodyPartCode()).orElseThrow();
            // GAIN_MUSCLE: 4D v3 tal cual; desde 4D v3.2, solo el relleno de zona pequena admite una
            // serie mas (seriesMax), y solo la usa la ampliacion para el minimo de V20. Los demas
            // objetivos conservan el tope de dos series para principiantes.
            if (MvpDosePolicy.aplicaA(goalType))
                return Optional.of(new Dosis(capacidad, tipo, d.series(), d.seriesMax(), d.reps(), null,
                        d.descansoSegundos(), null));
            short series = principiante ? (short) Math.min(d.series(), 2) : d.series();
            return Optional.of(ampliable(capacidad, tipo, series, reps(d.reps(), c, fitnessLevel, limites),
                    d.descansoSegundos()));
        }
        if (capacidad == Capability.STRENGTH) {
            if (claseDe(c) != Clase.ESTRUCTURAL) return Optional.empty();
            return Optional.of(principiante
                    ? ampliable(capacidad, tipo, (short) 2, reps(8, c, fitnessLevel, limites), (short) 120)
                    : ampliable(capacidad, tipo, (short) 3, reps(5, c, fitnessLevel, limites), (short) 150));
        }
        if (capacidad == Capability.MUSCULAR_ENDURANCE)
            return Optional.of(principiante
                    ? ampliable(capacidad, tipo, (short) 2, reps(15, c, fitnessLevel, limites), (short) 45)
                    : ampliable(capacidad, tipo, (short) 3, reps(18, c, fitnessLevel, limites), (short) 45));
        return Optional.empty();
    }

    private static Dosis ampliable(Capability capacidad, PrescriptionType tipo, short series, Short reps,
                                   short descanso) {
        return new Dosis(capacidad, tipo, series, (short) Math.max(series, SERIES_MAX_AMPLIACION), reps, null,
                descanso, null);
    }

    /** Repeticiones dentro de V16: nunca bajo el minimo de zona y nivel ni sobre el maximo. */
    private static Short reps(int base, CandidateExercise c, String fitnessLevel, PrescriptionParams.RepLimits limites) {
        if (limites == null || !limites.isComplete()) return (short) base;
        int minimo = limites.minRepsFor(c.bodyPartCode(), fitnessLevel);
        return (short) Math.min(limites.maxReps(), Math.max(base, minimo));
    }

    /**
     * La capacidad que una colocacion DEMUESTRA: la primera admisible cuya dosis
     * coincide exactamente. Vacio si ninguna coincide.
     */
    public static Optional<Capability> acreditada(PlanDraft.DraftExercise e, CandidateExercise c, String goalType,
                                                  String fitnessLevel, PrescriptionParams.RepLimits limites) {
        for (var capacidad : admisibles(c, goalType, fitnessLevel)) {
            var d = dosis(capacidad, c, goalType, fitnessLevel, limites);
            if (d.isPresent() && d.get().coincideCon(e)) return Optional.of(capacidad);
        }
        return Optional.empty();
    }

    /** Si este objetivo se prescribe por capacidades (todos menos GAIN_MUSCLE, que sigue con 4D v3). */
    public static boolean gobiernaGeneracion(String goalType) {
        return GoalCapabilityPolicy.forGoal(goalType).isPresent() && !MvpDosePolicy.aplicaA(goalType);
    }
}
