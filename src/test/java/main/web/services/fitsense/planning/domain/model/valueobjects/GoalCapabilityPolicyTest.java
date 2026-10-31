package main.web.services.fitsense.planning.domain.model.valueobjects;

import main.web.services.fitsense.planning.domain.model.valueobjects.GoalCapabilityPolicy.Capability;
import main.web.services.fitsense.planning.domain.model.valueobjects.GoalCapabilityPolicy.Condition;
import main.web.services.fitsense.profiling.domain.model.valueobjects.GoalType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static main.web.services.fitsense.planning.domain.model.valueobjects.GoalCapabilityPolicy.Capability.*;
import static org.junit.jupiter.api.Assertions.*;

class GoalCapabilityPolicyTest {

    /** La matriz 4A v6 tal como se acordo, escrita aparte de la clase. */
    static Stream<Arguments> matriz() {
        return Stream.of(
                Arguments.of("GAIN_MUSCLE", EnumSet.of(HYPERTROPHY), EnumSet.of(STRENGTH)),
                Arguments.of("INCREASE_STRENGTH", EnumSet.of(STRENGTH), EnumSet.of(HYPERTROPHY, POWER)),
                Arguments.of("LOSE_WEIGHT", EnumSet.of(CARDIO), EnumSet.of(STRENGTH)),
                Arguments.of("GENERAL_FITNESS",
                        EnumSet.of(STRENGTH, MUSCULAR_ENDURANCE, CARDIO, MOBILITY),
                        EnumSet.of(HYPERTROPHY, BALANCE)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("matriz")
    void principalesYSecundariasSonLasAcordadas(String goal, Set<Capability> principales,
                                                Set<Capability> secundarias) {
        var capacidades = GoalCapabilityPolicy.forGoal(goal).orElseThrow();
        assertEquals(principales, capacidades.primaries());
        assertEquals(secundarias, capacidades.secondaries());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("matriz")
    void cubrirLasPrincipalesBastaAunqueFaltenTodasLasSecundarias(String goal,
                                                                  Set<Capability> principales,
                                                                  Set<Capability> secundarias) {
        var evaluacion = GoalCapabilityPolicy.forGoal(goal).orElseThrow()
                .evaluate(principales, "INTERMEDIATE");
        assertTrue(evaluacion.primariesSatisfied());
        assertTrue(evaluacion.missingPrimaries().isEmpty());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("matriz")
    void unaPrincipalAusenteNoSeCompensaConSecundarias(String goal, Set<Capability> principales,
                                                       Set<Capability> secundarias) {
        var capacidades = GoalCapabilityPolicy.forGoal(goal).orElseThrow();
        for (var faltante : principales) {
            var cubiertas = EnumSet.copyOf(secundarias);
            principales.stream().filter(c -> c != faltante).forEach(cubiertas::add);
            var evaluacion = capacidades.evaluate(cubiertas, "ADVANCED");
            assertFalse(evaluacion.primariesSatisfied(), goal + " sin " + faltante);
            assertEquals(Set.of(faltante), evaluacion.missingPrimaries());
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(GoalType.class)
    void todoGoalTypeDelPerfilTieneMatriz(GoalType goal) {
        assertTrue(GoalCapabilityPolicy.forGoal(goal.name()).isPresent(), goal.name());
    }

    @Test
    void laMatrizNoTieneObjetivosQueElPerfilNoConozca() {
        var delPerfil = Arrays.stream(GoalType.values()).map(Enum::name).collect(Collectors.toSet());
        assertEquals(delPerfil, GoalCapabilityPolicy.goals());
    }

    @Test
    void loseWeightTieneSoloCardioComoPrincipal() {
        var capacidades = GoalCapabilityPolicy.forGoal("LOSE_WEIGHT").orElseThrow();
        assertEquals(Set.of(CARDIO), capacidades.primaries());
        assertTrue(capacidades.evaluate(Set.of(CARDIO), "BEGINNER").primariesSatisfied());
        assertFalse(capacidades.evaluate(Set.of(STRENGTH, HYPERTROPHY), "BEGINNER")
                .primariesSatisfied());
    }

    @Test
    void powerEsCondicionalYNoAplicaAPrincipiantes() {
        var capacidades = GoalCapabilityPolicy.forGoal("INCREASE_STRENGTH").orElseThrow();
        assertEquals(Condition.CONDITIONAL, capacidades.requirementOf(POWER).orElseThrow().condition());
        assertEquals(Set.of(HYPERTROPHY), capacidades.candidateSecondariesFor("BEGINNER"));
        assertEquals(Set.of(HYPERTROPHY, POWER), capacidades.candidateSecondariesFor("INTERMEDIATE"));

        var principiante = capacidades.evaluate(Set.of(STRENGTH, HYPERTROPHY), "BEGINNER");
        assertTrue(principiante.primariesSatisfied());
        assertTrue(principiante.missingSecondaries().isEmpty());

        var intermedio = capacidades.evaluate(Set.of(STRENGTH), "INTERMEDIATE");
        assertTrue(intermedio.primariesSatisfied());
        assertEquals(Set.of(HYPERTROPHY, POWER), intermedio.missingSecondaries());
    }

    @Test
    void balanceEsOportunistaYSuAusenciaNoInvalida() {
        var capacidades = GoalCapabilityPolicy.forGoal("GENERAL_FITNESS").orElseThrow();
        assertEquals(Condition.OPPORTUNISTIC, capacidades.requirementOf(BALANCE).orElseThrow().condition());
        var evaluacion = capacidades.evaluate(
                Set.of(STRENGTH, MUSCULAR_ENDURANCE, CARDIO, MOBILITY), "BEGINNER");
        assertTrue(evaluacion.primariesSatisfied());
        assertEquals(Set.of(HYPERTROPHY, BALANCE), evaluacion.missingSecondaries());
    }

    @Test
    void ningunaPrincipalEsCondicional() {
        GoalCapabilityPolicy.goals().forEach(goal ->
                GoalCapabilityPolicy.forGoal(goal).orElseThrow().requirements().stream()
                        .filter(r -> r.role() == GoalCapabilityPolicy.Role.PRIMARY)
                        .forEach(r -> assertEquals(Condition.ALWAYS, r.condition(), goal)));
        assertThrows(IllegalArgumentException.class, () -> new GoalCapabilityPolicy.Requirement(
                CARDIO, GoalCapabilityPolicy.Role.PRIMARY, Condition.OPPORTUNISTIC));
    }

    @Test
    void ningunaCapacidadEsPrincipalYSecundariaALaVez() {
        GoalCapabilityPolicy.goals().forEach(goal -> {
            var capacidades = GoalCapabilityPolicy.forGoal(goal).orElseThrow();
            var cruce = EnumSet.copyOf(capacidades.primaries());
            cruce.retainAll(capacidades.secondaries());
            assertTrue(cruce.isEmpty(), goal + ": " + cruce);
        });
    }

    @Test
    void lasCapacidadesDeDosisExistenEnLaMatriz() {
        for (var capacidad : MvpDosePolicy.Capacidad.values())
            assertDoesNotThrow(() -> Capability.valueOf(capacidad.name()), capacidad.name());
    }

    @Test
    void versionYDescripcionParaElLog() {
        assertEquals("4A-v6", GoalCapabilityPolicy.VERSION);
        var linea = GoalCapabilityPolicy.forGoal("INCREASE_STRENGTH").orElseThrow().describe("BEGINNER");
        assertTrue(linea.startsWith("version=4A-v6 objetivo=INCREASE_STRENGTH"), linea);
        assertTrue(linea.contains("POWER(CONDITIONAL,descartada_por_nivel_BEGINNER)"), linea);
        var intermedio = GoalCapabilityPolicy.forGoal("INCREASE_STRENGTH").orElseThrow()
                .describe("INTERMEDIATE");
        assertTrue(intermedio.contains("POWER(CONDITIONAL,candidata)"), intermedio);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"ENDURANCE", "RUNNING", "   "})
    void objetivoDesconocidoNoRecibeMatrizPorOmision(String goal) {
        assertTrue(GoalCapabilityPolicy.forGoal(goal).isEmpty());
    }

    @Test
    void aceptaMayusculasYEspaciosDelPerfil() {
        assertTrue(GoalCapabilityPolicy.forGoal(" lose_weight ").isPresent());
    }
}
