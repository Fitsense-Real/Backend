package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.MandatorySlot;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.model.valueobjects.SlotCoverage;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Las dos cuentas de cobertura de 4C, y nada mas.
 * <p>
 * Servicio de dominio PURO: sin Spring, sin base de datos, sin configuracion.
 * No decide que hacer con el resultado -eso es de quien llama- porque la misma
 * cuenta sirve para rechazar, para observar y para informar.
 * <p>
 * NO TOCA LA DOSIS. No lee series, repeticiones, descansos ni carga. Solo mira
 * {@code movement_pattern}.
 */
public final class SlotCoveragePolicy {

    private SlotCoveragePolicy() {}

    /**
     * COMPROBACION PREVIA. Cuantos ejercicios ELEGIBLES hay por slot.
     * <p>
     * Se cuenta sobre el conjunto que de verdad llego del catalogo, despues de
     * equipamiento, dificultad, bloqueos y restricciones de seguridad: contarlo
     * antes seria contar ejercicios que el participante no va a recibir.
     */
    public static SlotCoverage ofEligible(List<CandidateExercise> eligible) {
        var counts = nuevoMapa();
        int fuera = 0;
        int sinPatron = 0;

        for (var candidate : eligible == null ? List.<CandidateExercise>of() : eligible) {
            if (candidate == null) continue;
            if (candidate.movementPattern() == null || candidate.movementPattern().isBlank()) {
                sinPatron++;
                continue;
            }
            var slot = MandatorySlot.of(candidate);
            if (slot.isPresent()) counts.merge(slot.get(), 1, Integer::sum);
            else fuera++;
        }
        return new SlotCoverage(Map.copyOf(counts), fuera, sinPatron);
    }

    /**
     * COMPROBACION POSTERIOR. Cuantos ejercicios PRESCRITOS en la semana caen en
     * cada slot.
     * <p>
     * Cuenta POSICIONES, no ejercicios distintos: si el mismo ejercicio aparece
     * el lunes y el viernes, cuenta dos. Para «¿se cubrio el slot?» da igual, y
     * para leer el log es mas informativo.
     * <p>
     * Un {@code exerciseId} que no este en el conjunto elegible se cuenta como
     * sin clasificar en vez de ignorarse: ya hay otra validacion (la 1) que
     * rechaza eso, y aqui lo que importa es no fingir cobertura que no se puede
     * justificar.
     */
    public static SlotCoverage ofDraft(PlanDraft draft, Map<Long, CandidateExercise> eligible) {
        var counts = nuevoMapa();
        int fuera = 0;
        int sinPatron = 0;

        if (draft != null && draft.workouts() != null) {
            for (var workout : draft.workouts()) {
                if (workout == null || workout.exercises() == null) continue;
                for (var exercise : workout.exercises()) {
                    if (exercise == null) continue;
                    var candidate = eligible == null ? null : eligible.get(exercise.exerciseId());
                    if (candidate == null
                            || candidate.movementPattern() == null
                            || candidate.movementPattern().isBlank()) {
                        sinPatron++;
                        continue;
                    }
                    var slot = MandatorySlot.of(candidate);
                    if (slot.isPresent()) counts.merge(slot.get(), 1, Integer::sum);
                    else fuera++;
                }
            }
        }
        return new SlotCoverage(Map.copyOf(counts), fuera, sinPatron);
    }

    /**
     * Los slots que se le pueden EXIGIR a un borrador, dado el conjunto
     * elegible.
     * <p>
     * Es la pieza que evita el fallo que ya cometio V15 y que su propio
     * comentario documenta: exigir cobertura de algo que el conjunto elegible no
     * contiene rechaza el plan por un motivo que el generador no puede arreglar,
     * y el resultado es una semana sin plan -perdida de datos irrecuperable-, no
     * un plan mejor.
     * <p>
     * Por eso lo exigible es la INTERSECCION: los cuatro slots de 4C §5 que
     * ademas tienen al menos un ejercicio elegible. Si un slot esta a cero, eso
     * es un hallazgo de la comprobacion PREVIA, no un rechazo del borrador.
     */
    public static List<MandatorySlot> requiredOf(SlotCoverage eligibleCoverage) {
        return eligibleCoverage == null ? List.of() : eligibleCoverage.coveredSlots();
    }

    /**
     * Cuantas posiciones de ejercicio tiene la semana. Se usa para saber si
     * exigir los slots es siquiera aritmeticamente posible: con REDUCE_VOLUME y
     * REDUCE_DAYS una semana puede quedarse en dos sesiones de dos ejercicios, y
     * cuatro slots no caben en cuatro posiciones sin dejar la sesion sin nada
     * mas.
     */
    public static int exercisePositions(PlanDraft draft) {
        if (draft == null || draft.workouts() == null) return 0;
        return draft.workouts().stream()
                .filter(Objects::nonNull)
                .filter(workout -> workout.exercises() != null)
                .mapToInt(workout -> workout.exercises().size())
                .sum();
    }

    private static Map<MandatorySlot, Integer> nuevoMapa() {
        var counts = new EnumMap<MandatorySlot, Integer>(MandatorySlot.class);
        MandatorySlot.ALL.forEach(slot -> counts.put(slot, 0));
        return counts;
    }
}
