package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.MandatorySlot;
import main.web.services.fitsense.planning.domain.model.valueobjects.MvpDosePolicy;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanGenerationContext;
import main.web.services.fitsense.planning.domain.model.valueobjects.PrescriptionType;
import main.web.services.fitsense.planning.domain.model.valueobjects.WeekDerivation;
import main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * FACTIBILIDAD ANTES DE LLAMAR A LA IA (orden de optimizacion del 31/10/2026, fase C).
 * <p>
 * Solo DEMUESTRA imposibilidades; no es un planificador. Si una sesion senalada no tiene ninguna
 * propuesta que el backend pueda aceptar, pedirsela a la IA solo gasta llamadas (U01 S2 y U04 S5
 * de la campana MVP: tres intentos rechazados). Ante cualquier duda devuelve vacio y la IA se llama.
 * <ul>
 *   <li><b>ACORTAR con minutos declarados.</b> La cota inferior de lo que dura la sesion con las
 *       operaciones que V8C permite, cada una por separado (V8C no deja combinarlas): retirar UNA
 *       colocacion -en GAIN_MUSCLE, de relleno-, o bajar la dosis al minimo que admite V24 (2
 *       series, el min_reps de V16, 20 s, sin tocar el descanso). En GAIN_MUSCLE la dosis 4D no se
 *       reduce. Si incluso la cota supera los minutos declarados, ninguna propuesta cabe: la
 *       comprobacion es la misma de V8C sobre la duracion que calcula el backend.</li>
 *   <li><b>REPROGRAMAR sin fecha libre.</b> Ninguna fecha de la semana en available_days queda sin
 *       otra sesion, aparte de la que ya tiene: solo se podria conservar.</li>
 * </ul>
 * La IA solo se evita si TODAS las sesiones que irian a ella son imposibles.
 */
public final class FactibilidadPreviaIA {

    /** Prefijo con que el motivo queda en {@code correcciones_del_backend} de la evidencia V32. */
    public static final String NOTA = main.web.services.fitsense.planning.domain.model.valueobjects.AdaptationOutcome.NOTA_FACTIBILIDAD;

    private static final SessionDurationEstimator ESTIMADOR = new SessionDurationEstimator();
    private static final short SERIES_MIN = 2;
    private static final int SEGUNDOS_MIN = ProgresionDosisPolicy.SEGUNDOS_MIN;

    private FactibilidadPreviaIA() {
    }

    /** El motivo si TODAS las sesiones para la IA son imposibles; vacio si alguna puede salir. */
    public static Optional<String> imposibilidad(PlanGenerationContext context, WeekDerivation derivation) {
        var adjustment = context.adjustment();
        if (adjustment == null || adjustment.sesionesParaIA().isEmpty()) return Optional.empty();
        var motivos = new ArrayList<String>();
        for (var senalada : adjustment.sesionesParaIA()) {
            var motivo = motivo(context, derivation, senalada);
            if (motivo.isEmpty()) return Optional.empty();
            motivos.add(motivo.get());
        }
        return Optional.of(String.join(" | ", motivos));
    }

    static Optional<String> motivo(PlanGenerationContext context, WeekDerivation derivation,
                                   PlanAdjustment.TargetedSession senalada) {
        var derivada = derivation.from(senalada.plannedWorkoutId());
        if (derivada.isEmpty()) return Optional.empty();
        var w = derivada.get().workout();
        if (senalada.intervencion() == TipoIntervencion.ACORTAR_O_REORGANIZAR)
            return acortar(context, w, senalada);
        if (senalada.intervencion() == TipoIntervencion.REPROGRAMAR_DIAS)
            return reprogramar(context, derivation, senalada, w.scheduledDate());
        return Optional.empty();
    }

    private static Optional<String> acortar(PlanGenerationContext context, PlanDraft.DraftWorkout w,
                                            PlanAdjustment.TargetedSession senalada) {
        Integer limite = senalada.minutosDisponibles();
        if (limite == null || context.prescription() == null || context.profile() == null
                || w.exercises().size() < 3) return Optional.empty();
        boolean gm = MvpDosePolicy.aplicaA(context.profile().goalType());
        Map<Long, CandidateExercise> porId = context.availableExercises().stream()
                .collect(Collectors.toMap(CandidateExercise::exerciseId, Function.identity(), (a, b) -> a));

        int minimo = Integer.MAX_VALUE;
        // 1 · Retirar UNA colocacion (en GAIN_MUSCLE, solo de relleno: sin slot de 4C).
        for (int k = 0; k < w.exercises().size(); k++) {
            var c = porId.get(w.exercises().get(k).exerciseId());
            if (gm && (c == null || MandatorySlot.of(c).isPresent())) continue;
            var sin = new ArrayList<>(w.exercises());
            sin.remove(k);
            minimo = Math.min(minimo, minutos(w, sin, context));
        }
        // 2 · Bajar la dosis al minimo admitido, sin retirar nada (no en GAIN_MUSCLE).
        if (!gm) {
            var limites = context.prescription().repLimits();
            var bajadas = new ArrayList<PlanDraft.DraftExercise>();
            for (var e : w.exercises()) {
                var c = porId.get(e.exerciseId());
                int minReps = limites == null || c == null ? 1 : limites.minRepsFor(c.bodyPartCode(),
                        context.profile().fitnessLevel());
                short series = e.plannedSets() == null ? 1 : (short) Math.min(e.plannedSets(), SERIES_MIN);
                Short reps = e.plannedReps() == null ? null : (short) Math.min(e.plannedReps(), Math.max(1, minReps));
                Integer seg = e.plannedDurationSeconds() == null ? null
                        : Math.min(e.plannedDurationSeconds(), SEGUNDOS_MIN);
                bajadas.add(new PlanDraft.DraftExercise(e.exerciseId(), e.prescriptionType(),
                        e.plannedSets() == null ? null : series,
                        e.prescriptionType() == PrescriptionType.SETS_REPS ? reps : e.plannedReps(),
                        seg, e.targetLoadKg(), e.restSeconds(), e.notes()));
            }
            minimo = Math.min(minimo, minutos(w, bajadas, context));
        }
        if (minimo == Integer.MAX_VALUE || minimo <= limite) return Optional.empty();
        return Optional.of(("ACORTAR de la sesion %d: con lo que V8C permite (%s) no baja de %d min y la persona"
                + " declaro %d").formatted(senalada.plannedWorkoutId(),
                gm ? "en GAIN_MUSCLE, solo retirar un relleno" : "retirar una colocacion o bajar la dosis al minimo",
                minimo, limite));
    }

    private static Optional<String> reprogramar(PlanGenerationContext context, WeekDerivation derivation,
                                                PlanAdjustment.TargetedSession senalada, LocalDate actual) {
        if (context.profile() == null || context.profile().availableDays() == null) return Optional.empty();
        var ocupadas = derivation.workouts().stream()
                .filter(d -> !senalada.plannedWorkoutId().equals(d.sourcePlannedWorkoutId()))
                .map(d -> d.workout().scheduledDate()).collect(Collectors.toSet());
        List<LocalDate> libres = new ArrayList<>();
        for (var f = context.weekStartDate(); !f.isAfter(context.weekEndDate()); f = f.plusDays(1))
            if (!f.equals(actual) && !ocupadas.contains(f)
                    && context.profile().availableDays().contains((short) f.getDayOfWeek().getValue()))
                libres.add(f);
        if (!libres.isEmpty()) return Optional.empty();
        return Optional.of("REPROGRAMAR de la sesion %d: ninguna otra fecha disponible de la semana esta libre"
                .formatted(senalada.plannedWorkoutId()));
    }

    private static int minutos(PlanDraft.DraftWorkout w, List<PlanDraft.DraftExercise> ejercicios,
                               PlanGenerationContext context) {
        return ESTIMADOR.estimateMinutes(new PlanDraft.DraftWorkout(w.scheduledDate(), w.focus(), w.name(),
                w.expectedDurationMinutes(), ejercicios), context.prescription());
    }
}
