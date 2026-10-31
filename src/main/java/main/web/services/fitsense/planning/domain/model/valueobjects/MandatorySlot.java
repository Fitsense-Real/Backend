package main.web.services.fitsense.planning.domain.model.valueobjects;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Los cuatro slots obligatorios del Step 4C, y el mapa que traduce a ellos las
 * 24 clases de {@code exercises.movement_pattern} (V26).
 * <p>
 * EL MAPA ES EL DE 4C v4 §1, LITERAL. No se amplia aqui: si una clase de
 * movimiento deberia contar para un slot y no cuenta, eso se discute en 4C y se
 * abre una version del criterio, no se parchea este enum. Las 17 clases que no
 * aparecen -{@code ELBOW_FLEXION}, {@code CORE_STABILITY},
 * {@code LOCOMOTION_CARDIO}, {@code MOBILITY}...- no pertenecen a ningun slot
 * obligatorio, y eso es una decision del criterio, no un olvido.
 * <p>
 * Vive en planning y no en catalog porque es vocabulario de 4C, no del
 * catalogo: el catalogo solo sabe de patrones de movimiento.
 */
public enum MandatorySlot {

    /** Dominante de rodilla: sentadillas, zancadas, prensa. */
    LOWER_KNEE("KNEE_DOMINANT"),

    /**
     * Cadena posterior. Dos patrones distintos cuentan para el mismo slot, y es
     * deliberado: {@code HIP_HINGE} es la bisagra cargada (peso muerto) y
     * {@code HIP_EXTENSION} la extension sin bisagra (puente de gluteo). 4C los
     * trata como el mismo hueco semanal.
     */
    LOWER_HIP("HIP_HINGE", "HIP_EXTENSION"),

    /** Empuje, en cualquiera de sus dos planos. */
    UPPER_PUSH("HORIZONTAL_PUSH", "VERTICAL_PUSH"),

    /** Traccion, en cualquiera de sus dos planos. */
    UPPER_PULL("HORIZONTAL_PULL", "VERTICAL_PULL");

    private final Set<String> movementPatterns;

    MandatorySlot(String... movementPatterns) {
        this.movementPatterns = Set.of(movementPatterns);
    }

    /** Las clases de movement_pattern que caen en este slot. */
    public Set<String> movementPatterns() {
        return movementPatterns;
    }

    /** Orden fijo para informes y mensajes, para que dos salidas se comparen. */
    public static final List<MandatorySlot> ALL =
            List.of(LOWER_KNEE, LOWER_HIP, UPPER_PUSH, UPPER_PULL);

    /**
     * El slot de un patron de movimiento, si pertenece a alguno.
     *
     * @param movementPattern valor de {@code exercises.movement_pattern}; puede
     *                        ser {@code null} (ejercicio sin clasificar) y
     *                        entonces no pertenece a ningun slot.
     */
    public static Optional<MandatorySlot> ofMovementPattern(String movementPattern) {
        if (movementPattern == null || movementPattern.isBlank()) return Optional.empty();
        var normalizado = movementPattern.trim().toUpperCase(java.util.Locale.ROOT);
        return Arrays.stream(values())
                .filter(slot -> slot.movementPatterns.contains(normalizado))
                .findFirst();
    }

    /** El slot de un candidato, si su patron pertenece a alguno. */
    public static Optional<MandatorySlot> of(CandidateExercise candidate) {
        return candidate == null ? Optional.empty()
                : ofMovementPattern(candidate.movementPattern());
    }
}
