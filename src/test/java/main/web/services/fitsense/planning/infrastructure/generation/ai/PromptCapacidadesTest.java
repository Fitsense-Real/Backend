package main.web.services.fitsense.planning.infrastructure.generation.ai;

import main.web.services.fitsense.configuration.domain.model.valueobjects.PrescriptionParams;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.shared.infrastructure.json.JsonSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GEN-IN-1.11 / PR-1.4: la IA recibe las mismas capacidades y dosis que exigen
 * V24 y V25, y GAIN_MUSCLE recibe exactamente lo de antes. Sin llamadas.
 */
class PromptCapacidadesTest {

    static final LocalDate LUNES = LocalDate.of(2026, 10, 26);

    static final List<CandidateExercise> ELEGIBLES = List.of(
            new CandidateExercise(1L, "Sentadilla", "upper legs", "body weight", 1, PrescriptionType.SETS_REPS, null, "KNEE_DOMINANT"),
            new CandidateExercise(2L, "Remo", "back", "band", 1, PrescriptionType.SETS_REPS, null, "HORIZONTAL_PULL"),
            new CandidateExercise(3L, "Crunch", "waist", "body weight", 1, PrescriptionType.SETS_REPS, null, "TRUNK_FLEXION"),
            new CandidateExercise(8L, "Bicicleta", "cardio", "stationary bike", 1, PrescriptionType.DURATION, null, "LOCOMOTION_CARDIO"),
            new CandidateExercise(1374L, "Sprints con viento", "waist", "body weight", 1, PrescriptionType.SETS_REPS, null, "LOCOMOTION_CARDIO"));

    static PlanGenerationContext contexto(String objetivo) {
        var perfil = new PlanningProfile(7L, 35, "FEMALE", new BigDecimal("165"), new BigDecimal("70"), null,
                "INTERMEDIATE", objetivo, "x", "Ninguna", "GYM", 3, List.of((short) 1, (short) 3, (short) 5), 45,
                List.of("body weight"), List.of(), SafetyProfile.Declaradas.sinLimitaciones());
        return new PlanGenerationContext(7L, (short) 1, LUNES, LUNES.plusDays(6), perfil, PlanAdjustment.none(),
                PreviousWeekSummary.empty(), ELEGIBLES, SafetyProfile.none(), (PrescriptionParams) null);
    }

    @Test
    @DisplayName("LOSE_WEIGHT: capacidades y dosis por ejercicio; sin el abdominal ni el 1374, que no aportan nada")
    void perderPeso() {
        var s = PlanInputSnapshot.of(contexto("LOSE_WEIGHT"));
        var cap = s.constraints().capacidades();
        assertNotNull(cap);
        assertEquals(List.of("CARDIO"), cap.principales());
        assertEquals(List.of("CARDIO"), cap.principalesFactibles());
        assertTrue(cap.secundarias().contains("STRENGTH"));
        var ids = s.availableExercises().stream().map(PlanInputSnapshot.AvailableExercise::exerciseId).toList();
        assertEquals(List.of(1L, 2L, 8L), ids.stream().sorted().toList(), "ni el crunch ni el 1374 se ofrecen");
        var bici = s.availableExercises().stream().filter(e -> e.exerciseId() == 8L).findFirst().orElseThrow();
        assertEquals("CARDIO", bici.dosisPorCapacidad().get(0).capacidad());
        assertEquals(Integer.valueOf(120), bici.dosisPorCapacidad().get(0).segundos());
        assertTrue(new JsonSupport().write(s).contains("\"dosis_por_capacidad\""));
        assertTrue(new PlanPromptBuilder(new JsonSupport()).build(contexto("LOSE_WEIGHT"), "{}", null)
                .contains("CAPACIDADES DEL OBJETIVO"));
    }

    @Test
    @DisplayName("GAIN_MUSCLE: sin capacidades ni dosis por capacidad, y el prompt sin el bloque nuevo")
    void ganarMusculoSinCambios() {
        var s = PlanInputSnapshot.of(contexto("GAIN_MUSCLE"));
        assertNull(s.constraints().capacidades());
        assertTrue(s.availableExercises().stream().allMatch(e -> e.dosisPorCapacidad() == null));
        assertEquals(ELEGIBLES.size(), s.availableExercises().size(), "GAIN_MUSCLE recibe el conjunto de siempre");
        assertFalse(new PlanPromptBuilder(new JsonSupport()).build(contexto("GAIN_MUSCLE"), "{}", null)
                .contains("CAPACIDADES DEL OBJETIVO"));
    }
}
