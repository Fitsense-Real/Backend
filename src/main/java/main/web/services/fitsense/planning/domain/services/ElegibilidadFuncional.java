package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.WorkoutFocus;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * ELEGIBILIDAD FUNCIONAL (experimental, solo con una version de politica que la incluye; desactivada por defecto).
 * <p>
 * El motor vigente decide si un ejercicio "entra" en un enfoque por su {@code body_part} (y V22 para brazo), y lo
 * clasifica como RESISTIDO por cualquier patron que no sea de slot, cardio, movilidad o equilibrio. Asi una sesion PULL
 * se puede llenar con un empuje etiquetado como espalda, movimientos de tronco y flexiones de codo sin carga, y pasar
 * V15, V23 y V25 (que solo exigen los slots que el catalogo puede cubrir). Esta capa compara el PATRON con la funcion
 * de la sesion, sin tocar 4A, 4C, 4D ni el catalogo:
 * <ol>
 *   <li><b>Compatibilidad patron-enfoque</b>: en PUSH, PULL, LEGS, LOWER_BODY y UPPER_BODY un ejercicio resistido
 *       solo entra si su patron pertenece a la familia del enfoque (un HORIZONTAL_PUSH no entra en PULL).</li>
 *   <li><b>Ancla obligatoria</b> (solo validacion): una sesion PUSH, PULL o LEGS necesita al menos un ejercicio del
 *       patron principal (empuje, traccion, rodilla o cadera). Sin candidatos de ese patron, la semana NO es
 *       factible por catalogo o equipamiento insuficiente; no se rellena con otra cosa.</li>
 *   <li><b>Etiqueta de zona incoherente</b>: un patron de tronco (flexion, rotacion, estabilidad) con zona distinta
 *       de "waist" no cuenta como trabajo resistido de esa zona.</li>
 *   <li><b>Datos incoherentes revisados</b>: lista cerrada de ids con una inconsistencia comprobada en el registro
 *       (p. ej. equipo de peso corporal con instrucciones que piden mancuerna).</li>
 * </ol>
 * Un patron desconocido (null) no se juzga: no hay dato.
 */
public final class ElegibilidadFuncional {

    private ElegibilidadFuncional() {}

    public static final String VERSION = "EF-0.1";

    public static final Set<String> TRACCION = Set.of("HORIZONTAL_PULL", "VERTICAL_PULL");
    public static final Set<String> EMPUJE = Set.of("HORIZONTAL_PUSH", "VERTICAL_PUSH");
    public static final Set<String> PIERNA = Set.of("KNEE_DOMINANT", "HIP_HINGE", "HIP_EXTENSION");

    static final Set<String> FAMILIA_PULL = Set.of("HORIZONTAL_PULL", "VERTICAL_PULL", "SHOULDER_EXTENSION",
            "SCAPULAR_ELEVATION", "ELBOW_FLEXION", "WRIST_FOREARM", "TRUNK_EXTENSION");
    static final Set<String> FAMILIA_PUSH = Set.of("HORIZONTAL_PUSH", "VERTICAL_PUSH", "HORIZONTAL_ADDUCTION",
            "SHOULDER_RAISE", "ELBOW_EXTENSION");
    static final Set<String> FAMILIA_PIERNA = Set.of("KNEE_DOMINANT", "HIP_HINGE", "HIP_EXTENSION",
            "HIP_ABDUCTION_ADDUCTION", "ANKLE_PLANTAR_FLEXION");
    static final Set<String> TRONCO = Set.of("TRUNK_FLEXION", "TRUNK_ROTATION_LATERAL_FLEXION", "CORE_STABILITY",
            "TRUNK_EXTENSION");
    static final Set<String> TRONCO_NO_ZONAL = Set.of("TRUNK_FLEXION", "TRUNK_ROTATION_LATERAL_FLEXION",
            "CORE_STABILITY");

    /** Ids con una inconsistencia COMPROBADA en el registro (no una duda de pertinencia). */
    public static final Map<Long, String> DATOS_INCOHERENTES = Map.of(
            315L, "equipo 'body weight' pero las instrucciones piden sujetar una mancuerna");

    /** Por que un ejercicio no puede contar como trabajo resistido en ningun enfoque; vacio si puede. */
    public static Optional<String> excluidoGlobal(CandidateExercise c) {
        if (c == null) return Optional.empty();
        var dato = DATOS_INCOHERENTES.get(c.exerciseId());
        if (dato != null) return Optional.of("DATOS_INCOHERENTES: " + dato);
        String p = patron(c);
        if (p != null && TRONCO_NO_ZONAL.contains(p) && !"waist".equals(c.bodyPartCode()) && esResistido(c))
            return Optional.of("ZONA_INCOHERENTE: patron de tronco " + p + " etiquetado como " + c.bodyPartCode());
        return Optional.empty();
    }

    /** Si un ejercicio resistido es compatible con la funcion del enfoque. Lo no resistido no se juzga aqui. */
    public static boolean compatible(WorkoutFocus enfoque, CandidateExercise c) {
        if (!esResistido(c)) return true;
        if (excluidoGlobal(c).isPresent()) return false;
        String p = patron(c);
        if (p == null) return true;
        return switch (enfoque) {
            case PULL -> FAMILIA_PULL.contains(p);
            case PUSH -> FAMILIA_PUSH.contains(p);
            case LEGS -> FAMILIA_PIERNA.contains(p);
            case LOWER_BODY -> FAMILIA_PIERNA.contains(p) || TRONCO.contains(p);
            case UPPER_BODY -> FAMILIA_PULL.contains(p) || FAMILIA_PUSH.contains(p);
            case FULL_BODY, CORE -> true;
        };
    }

    /** Los patrones de los que una sesion de ese enfoque necesita al menos un ejercicio; vacio si no hay ancla. */
    public static Set<String> ancla(WorkoutFocus enfoque) {
        return switch (enfoque) {
            case PULL -> TRACCION;
            case PUSH -> EMPUJE;
            case LEGS -> PIERNA;
            default -> Set.of();
        };
    }

    public static String nombreAncla(WorkoutFocus enfoque) {
        return switch (enfoque) {
            case PULL -> "traccion (HORIZONTAL_PULL o VERTICAL_PULL)";
            case PUSH -> "empuje (HORIZONTAL_PUSH o VERTICAL_PUSH)";
            case LEGS -> "pierna (KNEE_DOMINANT, HIP_HINGE o HIP_EXTENSION)";
            default -> "-";
        };
    }

    /**
     * La misma regla que aplica VEF, en el texto que recibe la IA (solo con una version EF). Coincide con
     * {@link #compatible} y {@link #ancla}: lo que este texto permite, el validador lo acepta.
     */
    public static String reglaParaIA() {
        return "\nELEGIBILIDAD FUNCIONAL (backend, " + VERSION + "): en PULL solo cuentan los patrones "
                + String.join(", ", new java.util.TreeSet<>(FAMILIA_PULL)) + "; en PUSH, "
                + String.join(", ", new java.util.TreeSet<>(FAMILIA_PUSH)) + "; en LEGS, "
                + String.join(", ", new java.util.TreeSet<>(FAMILIA_PIERNA)) + " (LOWER_BODY admite ademas tronco;"
                + " UPPER_BODY, empuje y traccion). Cada sesion PULL necesita al menos un ejercicio HORIZONTAL_PULL o"
                + " VERTICAL_PULL; PUSH, uno HORIZONTAL_PUSH o VERTICAL_PUSH; LEGS, uno KNEE_DOMINANT, HIP_HINGE o"
                + " HIP_EXTENSION. Un patron de tronco con zona distinta de waist no cuenta como trabajo resistido."
                + " Si no hay ejercicio del patron principal, no lo sustituyas por otro movimiento: el backend lo"
                + " rechaza.\n";
    }

    public static boolean esAncla(WorkoutFocus enfoque, CandidateExercise c) {
        var a = ancla(enfoque);
        String p = patron(c);
        return p != null && a.contains(p) && esResistido(c) && excluidoGlobal(c).isEmpty();
    }

    static boolean esResistido(CandidateExercise c) {
        var clase = CapabilityPrescriptionPolicy.claseDe(c);
        return clase == CapabilityPrescriptionPolicy.Clase.ESTRUCTURAL || clase == CapabilityPrescriptionPolicy.Clase.RESISTIDO;
    }

    static String patron(CandidateExercise c) {
        return c == null || c.movementPattern() == null || c.movementPattern().isBlank() ? null
                : c.movementPattern().trim().toUpperCase(Locale.ROOT);
    }
}
