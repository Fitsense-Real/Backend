package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanGenerationContext;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanningPolicy;
import main.web.services.fitsense.planning.domain.model.valueobjects.WorkoutFocus;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
     * Los problemas VEF de un borrador (los usa el validador y el generador, para que sean la misma regla):
     * <ol>
     *   <li>cada ejercicio resistido que no es complemento tiene un patron compatible con el enfoque;</li>
     *   <li>una sesion PUSH, PULL o LEGS con trabajo resistido propio tiene al menos un ejercicio de su patron
     *       principal. Si el catalogo elegible no tiene ninguno, se dice (NO_FACTIBLE por catalogo o
     *       equipamiento); no se rellena con otro movimiento. Una sesion solo de cardio o movilidad (LOSE_WEIGHT
     *       corto) no afirma esa funcion.</li>
     * </ol>
     */
    public static List<String> problemas(PlanDraft draft, PlanGenerationContext context,
                                         Map<Long, CandidateExercise> eligible) {
        var problems = new ArrayList<String>();
        if (draft == null || draft.workouts() == null || context == null) return problems;
        var porId = eligible != null ? eligible : context.availableExercises().stream()
                .collect(Collectors.toMap(CandidateExercise::exerciseId, c -> c, (a, b) -> a));
        var goal = context.profile() == null ? null : context.profile().goalType();
        var complementos = CatalogEligibilityPolicy.complementos(context.availableExercises(), goal);
        int maxDificultad = context.effectiveMaxDifficulty();
        for (var workout : draft.workouts()) {
            var enfoque = workout.focus();
            if (enfoque == null) continue;
            var propios = workout.exercises().stream()
                    .map(exercise -> porId.get(exercise.exerciseId()))
                    .filter(Objects::nonNull)
                    .filter(candidate -> !complementos.contains(candidate.exerciseId()))
                    .toList();
            for (var candidate : propios) {
                if (compatible(enfoque, candidate)) continue;
                var motivo = excluidoGlobal(candidate)
                        .orElse("patron " + candidate.movementPattern() + " fuera de la funcion de " + enfoque);
                problems.add("VEF: el entrenamiento %s del %s incluye el ejercicio %d (%s), que no cuenta como trabajo de esa sesion: %s."
                        .formatted(enfoque, workout.scheduledDate(), candidate.exerciseId(), candidate.name(), motivo));
            }
            if (ancla(enfoque).isEmpty() || propios.stream().noneMatch(ElegibilidadFuncional::esResistido)
                    || propios.stream().anyMatch(c -> esAncla(enfoque, c))) continue;
            long disponibles = context.availableExercises().stream()
                    .filter(c -> c.difficulty() <= maxDificultad)
                    .filter(c -> !complementos.contains(c.exerciseId()))
                    .filter(c -> esAncla(enfoque, c))
                    .count();
            problems.add((disponibles == 0
                    ? "VEF: el entrenamiento %s del %s no tiene ningun ejercicio de %s y el catalogo elegible con el equipamiento declarado no tiene ninguno (NO_FACTIBLE por catalogo o equipamiento insuficiente; no se sustituye por otro movimiento)."
                    : "VEF: el entrenamiento %s del %s no tiene ningun ejercicio de %s; hay %d candidatos elegibles de ese patron.")
                    .formatted(enfoque, workout.scheduledDate(), nombreAncla(enfoque), disponibles));
        }
        return problems;
    }

    /** La misma version sin EF (para generar primero como siempre y conservar el plan si ya cumple VEF). */
    public static PlanningPolicy sinElegibilidad(PlanningPolicy p) {
        return new PlanningPolicy(p.id(), p.candidata(), p.c2d(), p.v4Segundos(), false, p.descripcion());
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

    public static boolean esResistido(CandidateExercise c) {
        var clase = CapabilityPrescriptionPolicy.claseDe(c);
        return clase == CapabilityPrescriptionPolicy.Clase.ESTRUCTURAL || clase == CapabilityPrescriptionPolicy.Clase.RESISTIDO;
    }

    static String patron(CandidateExercise c) {
        return c == null || c.movementPattern() == null || c.movementPattern().isBlank() ? null
                : c.movementPattern().trim().toUpperCase(Locale.ROOT);
    }
}
