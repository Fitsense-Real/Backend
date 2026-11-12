package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.GoalCapabilityPolicy;
import main.web.services.fitsense.planning.domain.model.valueobjects.GoalCapabilityPolicy.Capability;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Paso 2 · FILTRO FUNCIONAL del conjunto elegible: lo que se aplica DESPUES de
 * los filtros de seguridad, nivel, equipamiento, bloqueos y retenidos, que
 * siguen en la consulta y en {@code ExternalCatalogService} sin ningun cambio.
 * <p>
 * Dominio PURO. Es la unica decision de este tipo: la usan el ACL del catalogo,
 * el motor de reglas y V15, para que lo que entra en el conjunto elegible y lo
 * que el validador acepta no puedan divergir.
 * <p>
 * <b>Tres familias de exclusion</b>, que antes estaban mezcladas en el ACL:
 * <ol>
 *   <li>Seguridad (impacto, suelo, carga axial, rodillas, suspension, brazos
 *       arriba, inversion), dificultad, equipamiento, bloqueos individuales y
 *       retenidos INC-4C-03. NO SE TOCAN y no estan aqui.</li>
 *   <li>Preferencias: hoy solo {@code blockedExerciseIds}, en la consulta.</li>
 *   <li>Funcionales: las de esta clase. Zonas que ningun {@code WorkoutFocus}
 *       admite y estiramientos fuera del trabajo principal.</li>
 * </ol>
 * <p>
 * <b>La regla.</b> Las exclusiones funcionales de siempre se mantienen
 * ({@link #exclusionHeredada}), con una sola excepcion: si el
 * {@code movement_pattern} del ejercicio es la ruta de una capacidad que el
 * objetivo exige (4A v6), entra como {@link Decision#ADMITIDO_POR_CAPACIDAD}:
 * <ul>
 *   <li>{@code LOCOMOTION_CARDIO} → {@code CARDIO} ({@code LOSE_WEIGHT},
 *       {@code GENERAL_FITNESS});</li>
 *   <li>{@code MOBILITY} → {@code MOBILITY} ({@code GENERAL_FITNESS}).</li>
 * </ul>
 * La admision la decide el PATRON, nunca el nombre ni la zona.
 * <p>
 * <b>Por que solo los antes excluidos.</b> Todo ejercicio que ya era elegible
 * sigue siendolo y se trata igual que antes, en todos los objetivos: asi no
 * cambia ninguna semana existente ni el camino de adaptacion (P2-D), cuyo V1
 * rechazaria una semana conservada con un ejercicio que dejara de ser
 * elegible. Que 8 ejercicios {@code LOCOMOTION_CARDIO} de {@code upper legs} y
 * {@code waist}, y 8 {@code MOBILITY} sin «estiramiento» en el nombre, sigan
 * entrando como trabajo normal en todos los objetivos es una inconsistencia
 * conocida entre nombre/zona y patron, pendiente de decision: no se corrige en
 * silencio aqui.
 * <p>
 * Un ejercicio {@link Decision#ADMITIDO_POR_CAPACIDAD} es un COMPLEMENTO: el
 * motor de reglas no lo elige como trabajo de su enfoque y V15 no lo cuenta ni
 * lo trata como grupo ajeno.
 */
public final class CatalogEligibilityPolicy {

    private CatalogEligibilityPolicy() {}

    /**
     * Zonas que ningun enfoque admite (WorkoutFocus). Antes solo entraban por la
     * exencion de FULL_BODY y engordaban el prompt.
     */
    public static final Set<String> ZONAS_FUERA_DE_ENFOQUE = Set.of("neck", "lower arms", "cardio");

    /** Patron de catalogo (V26) que acredita cada capacidad como ruta directa. */
    public static final Map<String, Capability> RUTAS = Map.of(
            "LOCOMOTION_CARDIO", Capability.CARDIO,
            "MOBILITY", Capability.MOBILITY);

    public enum Decision {
        /** Entra igual que antes de este paso. */
        ADMITIDO,
        /** Entra solo porque su patron es la ruta de una capacidad del objetivo. */
        ADMITIDO_POR_CAPACIDAD,
        /** Zona que ningun enfoque admite, y el objetivo no exige su ruta. */
        EXCLUIDO_FUERA_DE_ENFOQUE,
        /** Estiramiento fuera del trabajo principal, y el objetivo no exige movilidad. */
        EXCLUIDO_ESTIRAMIENTO;

        public boolean admite() {
            return this == ADMITIDO || this == ADMITIDO_POR_CAPACIDAD;
        }
    }

    /** La capacidad cuya ruta directa es el patron del ejercicio, si la hay. */
    public static Optional<Capability> rutaDe(CandidateExercise candidate) {
        if (candidate == null || candidate.movementPattern() == null) return Optional.empty();
        return Optional.ofNullable(RUTAS.get(candidate.movementPattern().trim().toUpperCase(Locale.ROOT)));
    }

    /** Las exclusiones funcionales anteriores al Paso 2, sin cambios. */
    public static Optional<Decision> exclusionHeredada(CandidateExercise candidate) {
        if (candidate.bodyPartCode() != null && ZONAS_FUERA_DE_ENFOQUE.contains(candidate.bodyPartCode()))
            return Optional.of(Decision.EXCLUIDO_FUERA_DE_ENFOQUE);
        if (candidate.name() != null
                && candidate.name().toLowerCase(Locale.ROOT).contains("estiramiento"))
            return Optional.of(Decision.EXCLUIDO_ESTIRAMIENTO);
        return Optional.empty();
    }

    /** Capacidades del objetivo, principales y secundarias. Vacio si no hay matriz. */
    public static Set<Capability> capacidadesDe(String goalType) {
        return GoalCapabilityPolicy.forGoal(goalType)
                .map(g -> {
                    var todas = new java.util.HashSet<>(g.primaries());
                    todas.addAll(g.secondaries());
                    return Set.copyOf(todas);
                })
                .orElse(Set.of());
    }

    public static Decision decidir(CandidateExercise candidate, String goalType) {
        var heredada = exclusionHeredada(candidate);
        if (heredada.isEmpty()) return Decision.ADMITIDO;
        var ruta = rutaDe(candidate);
        if (ruta.isPresent() && capacidadesDe(goalType).contains(ruta.get()))
            return Decision.ADMITIDO_POR_CAPACIDAD;
        return heredada.get();
    }

    /** Si el ejercicio solo es elegible como complemento de una capacidad. */
    public static boolean esComplemento(CandidateExercise candidate, String goalType) {
        return candidate != null && decidir(candidate, goalType) == Decision.ADMITIDO_POR_CAPACIDAD;
    }

    /** Los complementos de un conjunto elegible, por id. */
    public static Set<Long> complementos(Collection<CandidateExercise> elegibles, String goalType) {
        return elegibles == null ? Set.of() : elegibles.stream()
                .filter(c -> esComplemento(c, goalType))
                .map(CandidateExercise::exerciseId)
                .collect(Collectors.toUnmodifiableSet());
    }

    /** Recuento por decision, para el log del ACL. */
    public record Informe(Map<Decision, Integer> porDecision, Map<Capability, Integer> porCapacidad) {
        public String describe() {
            return "decisiones=%s complementos_por_capacidad=%s".formatted(porDecision, porCapacidad);
        }
    }

    public static Informe informe(List<CandidateExercise> candidatos, String goalType) {
        var porDecision = new EnumMap<Decision, Integer>(Decision.class);
        for (var d : Decision.values()) porDecision.put(d, 0);
        var porCapacidad = new EnumMap<Capability, Integer>(Capability.class);
        for (var c : candidatos) {
            var d = decidir(c, goalType);
            porDecision.merge(d, 1, Integer::sum);
            if (d == Decision.ADMITIDO_POR_CAPACIDAD)
                rutaDe(c).ifPresent(cap -> porCapacidad.merge(cap, 1, Integer::sum));
        }
        return new Informe(porDecision, porCapacidad);
    }
}
