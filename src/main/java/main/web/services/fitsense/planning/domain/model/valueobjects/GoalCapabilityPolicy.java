package main.web.services.fitsense.planning.domain.model.valueobjects;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Matriz 4A v6: que capacidades de entrenamiento son PRINCIPALES y cuales
 * SECUNDARIAS para cada objetivo del MVP.
 * <p>
 * Dominio PURO: sin Spring, sin base, sin configuracion. La matriz vive aqui y
 * en ningun otro sitio, igual que la dosis vive en {@link MvpDosePolicy}.
 * <p>
 * <b>Adenda 4A v6 (23/10/2026).</b> Sobre 4A v5, que NO se modifica: para
 * {@code LOSE_WEIGHT} la unica capacidad principal es {@code CARDIO}. La
 * preservacion de masa magra sigue siendo una consideracion relevante, pero deja
 * de ser principal obligatoria; {@code STRENGTH} queda como secundaria.
 * El texto completo esta en {@code docs/FitSense/ADENDA_4A_v6.md}.
 * <p>
 * <b>REGLA METODOLOGICA.</b> Una capacidad secundaria nunca hace fallar un plan
 * que cumple sus principales. {@link Assessment#primariesSatisfied()} solo mira
 * las principales, y es la unica respuesta de aceptacion que da esta clase.
 * <p>
 * <b>SOLO OBSERVA.</b> En el Paso 1 la politica se registra en el log y no
 * participa en ninguna validacion. Saber que capacidades cubre un plan concreto
 * (Pasos 3 y 4) todavia no esta implementado: {@link #evaluate} recibe ese
 * conjunto hecho y no lo calcula.
 */
public final class GoalCapabilityPolicy {

    /** Version del criterio. Viaja en el log para atar cada linea a su matriz. */
    public static final String VERSION = "4A-v6";

    private GoalCapabilityPolicy() {}

    /**
     * Capacidades de 4A. Los cuatro primeros nombres coinciden con
     * {@link MvpDosePolicy.Capacidad}, que solo modela las capacidades con dosis
     * de resistencia; aqui estan todas.
     */
    public enum Capability {
        HYPERTROPHY,
        STRENGTH,
        POWER,
        MUSCULAR_ENDURANCE,
        CARDIO,
        MOBILITY,
        BALANCE
    }

    public enum Role { PRIMARY, SECONDARY }

    /**
     * Cuando aplica una capacidad. Solo tiene sentido en las secundarias: una
     * principal siempre es {@link #ALWAYS}.
     */
    public enum Condition {
        /** Aplica siempre. */
        ALWAYS,
        /**
         * {@code POWER} en {@code INCREASE_STRENGTH}: no aplica a principiantes.
         * 4D la declara estructuralmente inalcanzable para ese nivel, y su
         * ausencia no debe invalidar el objetivo.
         */
        CONDITIONAL,
        /**
         * {@code BALANCE} en {@code GENERAL_FITNESS}: se incluye si el conjunto
         * elegible ofrece una ruta compatible; si no, no se echa en falta.
         */
        OPPORTUNISTIC
    }

    public record Requirement(Capability capability, Role role, Condition condition) {
        public Requirement {
            if (role == Role.PRIMARY && condition != Condition.ALWAYS)
                throw new IllegalArgumentException(
                        "Una capacidad principal no puede ser condicional: " + capability);
        }

        /** Si la capacidad aplica a un participante de este nivel. */
        public boolean appliesTo(String fitnessLevel) {
            return condition != Condition.CONDITIONAL || !"BEGINNER".equals(fitnessLevel);
        }
    }

    public record GoalCapabilities(String goalType, List<Requirement> requirements) {

        public Set<Capability> primaries() {
            return withRole(Role.PRIMARY);
        }

        public Set<Capability> secondaries() {
            return withRole(Role.SECONDARY);
        }

        /** Secundarias que aplican al nivel dado: POWER sale para principiantes. */
        public Set<Capability> secondariesFor(String fitnessLevel) {
            return requirements.stream()
                    .filter(r -> r.role() == Role.SECONDARY && r.appliesTo(fitnessLevel))
                    .map(Requirement::capability)
                    .collect(Collectors.toCollection(() -> EnumSet.noneOf(Capability.class)));
        }

        public Optional<Requirement> requirementOf(Capability capability) {
            return requirements.stream().filter(r -> r.capability() == capability).findFirst();
        }

        /**
         * Compara la matriz con las capacidades que un plan cubre. Las
         * secundarias ausentes se informan, pero no cambian
         * {@link Assessment#primariesSatisfied()}.
         */
        public Assessment evaluate(Set<Capability> covered, String fitnessLevel) {
            var cubiertas = covered == null ? Set.<Capability>of() : covered;
            var principalesFaltan = EnumSet.noneOf(Capability.class);
            primaries().stream().filter(c -> !cubiertas.contains(c)).forEach(principalesFaltan::add);
            var secundariasFaltan = EnumSet.noneOf(Capability.class);
            secondariesFor(fitnessLevel).stream()
                    .filter(c -> !cubiertas.contains(c)).forEach(secundariasFaltan::add);
            return new Assessment(goalType, principalesFaltan, secundariasFaltan);
        }

        /** Una linea para el log. */
        public String describe(String fitnessLevel) {
            return "version=%s objetivo=%s principales=%s secundarias=%s".formatted(
                    VERSION, goalType, primaries(), requirements.stream()
                            .filter(r -> r.role() == Role.SECONDARY)
                            .map(r -> r.condition() == Condition.ALWAYS ? r.capability().name()
                                    : "%s(%s%s)".formatted(r.capability(), r.condition(),
                                    r.appliesTo(fitnessLevel) ? "" : ",no_aplica_" + fitnessLevel))
                            .toList());
        }

        private Set<Capability> withRole(Role role) {
            return requirements.stream()
                    .filter(r -> r.role() == role)
                    .map(Requirement::capability)
                    .collect(Collectors.toCollection(() -> EnumSet.noneOf(Capability.class)));
        }
    }

    /**
     * @param missingPrimaries   principales que el plan no cubre
     * @param missingSecondaries secundarias aplicables que el plan no cubre; solo
     *                           informativo
     */
    public record Assessment(String goalType, Set<Capability> missingPrimaries,
                             Set<Capability> missingSecondaries) {
        /** La unica respuesta de aceptacion: depende SOLO de las principales. */
        public boolean primariesSatisfied() {
            return missingPrimaries.isEmpty();
        }
    }

    private static Requirement primary(Capability c) {
        return new Requirement(c, Role.PRIMARY, Condition.ALWAYS);
    }

    private static Requirement secondary(Capability c, Condition condition) {
        return new Requirement(c, Role.SECONDARY, condition);
    }

    /** La matriz, literal. Las claves son los valores de {@code GoalType}. */
    private static final Map<String, GoalCapabilities> MATRIZ = Map.of(
            "GAIN_MUSCLE", new GoalCapabilities("GAIN_MUSCLE", List.of(
                    primary(Capability.HYPERTROPHY),
                    secondary(Capability.STRENGTH, Condition.ALWAYS))),
            "INCREASE_STRENGTH", new GoalCapabilities("INCREASE_STRENGTH", List.of(
                    primary(Capability.STRENGTH),
                    secondary(Capability.HYPERTROPHY, Condition.ALWAYS),
                    secondary(Capability.POWER, Condition.CONDITIONAL))),
            "LOSE_WEIGHT", new GoalCapabilities("LOSE_WEIGHT", List.of(
                    primary(Capability.CARDIO),
                    secondary(Capability.STRENGTH, Condition.ALWAYS))),
            "GENERAL_FITNESS", new GoalCapabilities("GENERAL_FITNESS", List.of(
                    primary(Capability.STRENGTH),
                    primary(Capability.MUSCULAR_ENDURANCE),
                    primary(Capability.CARDIO),
                    primary(Capability.MOBILITY),
                    secondary(Capability.HYPERTROPHY, Condition.ALWAYS),
                    secondary(Capability.BALANCE, Condition.OPPORTUNISTIC))));

    /** Objetivos que la matriz conoce. */
    public static Set<String> goals() {
        return MATRIZ.keySet();
    }

    /**
     * Capacidades del objetivo. Vacio para un objetivo desconocido o nulo: no se
     * inventa una matriz por omision.
     */
    public static Optional<GoalCapabilities> forGoal(String goalType) {
        if (goalType == null || goalType.isBlank()) return Optional.empty();
        return Optional.ofNullable(MATRIZ.get(goalType.trim().toUpperCase(Locale.ROOT)));
    }
}
