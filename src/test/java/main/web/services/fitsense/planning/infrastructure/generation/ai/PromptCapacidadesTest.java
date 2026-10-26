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
        // GEN-IN-1.12: la dosis viaja una vez, en capacidades.dosis, y el ejercicio cita su codigo.
        var texto = new JsonSupport().write(s);
        assertFalse(texto.contains("\"dosis_por_capacidad\""), "la lista repetida ya no viaja");
        assertEquals(List.of("CARDIO_1"), bici.dosis());
        var cardio1 = cap.dosis().stream().filter(d -> d.codigo().equals("CARDIO_1")).findFirst().orElseThrow();
        assertEquals("CARDIO", cardio1.capacidad());
        assertEquals(Integer.valueOf(120), cardio1.segundos());
        assertTrue(texto.contains("\"dosis\":[\"CARDIO_1\"]"), texto);
        assertEquals(List.of(8L), cap.candidatosEscasos().get("CARDIO"), "pocos candidatos: viajan sus ids");
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

    @Test
    @DisplayName("PR-1.5: la regla prioritaria abre el prompt, nombra las principales y la comprobacion va tras los datos")
    void reglaPrioritaria() {
        var prompt = new PlanPromptBuilder(new JsonSupport()).build(contexto("LOSE_WEIGHT"), "{\"datos\":1}", null);
        int regla = prompt.indexOf("REGLA PRIORITARIA DE ESTA SEMANA");
        assertTrue(regla > 0 && regla < prompt.indexOf("Reglas que el backend verifica"), "va antes de las reglas");
        assertTrue(prompt.contains("Capacidades principales factibles para esta persona: CARDIO."), prompt);
        assertTrue(prompt.contains("dos bloques de CARDIO por sesion"), "unica principal CARDIO");
        assertTrue(prompt.contains("Cada sesion tiene al menos una colocacion de cada una de CARDIO"), prompt);
        assertEquals(Integer.valueOf(8), PlanInputSnapshot.of(contexto("LOSE_WEIGHT")).constraints().maxColocacionesDia(),
                "el tope de V24 viaja");
        assertTrue(prompt.contains("CARDIO: [8]"), "los candidatos escasos se nombran");
        int datos = prompt.indexOf("{\"datos\":1}");
        assertTrue(prompt.indexOf("ANTES DE ESCRIBIR EL JSON, COMPRUEBA") > datos, "la comprobacion va despues de los datos");
    }

    @Test
    @DisplayName("PR-1.5: GAIN_MUSCLE no recibe la regla prioritaria ni la comprobacion")
    void ganarMusculoSinReglaPrioritaria() {
        var prompt = new PlanPromptBuilder(new JsonSupport()).build(contexto("GAIN_MUSCLE"), "{}", null);
        assertFalse(prompt.contains("REGLA PRIORITARIA"));
        assertFalse(prompt.contains("ANTES DE ESCRIBIR EL JSON"));
    }

    @Test
    @DisplayName("PR-1.5: en el reintento, el nombre del plan no menciona la correccion")
    void reintentoSinCorregidaEnElNombre() {
        var prompt = new PlanPromptBuilder(new JsonSupport()).build(contexto("LOSE_WEIGHT"), "{}",
                new AiRetryFeedback("{\"plan_name\":\"x\"}", List.of("V25: falta CARDIO"), List.of()));
        assertTrue(prompt.contains("plan_name y rationale no mencionan el rechazo ni la correccion"));
    }
}
