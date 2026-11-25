package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.planning.domain.model.valueobjects.WorkoutFocus;
import main.web.services.fitsense.planning.infrastructure.generation.ai.PlanPromptBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** H2-v2: la aritmetica del contrato (la comparacion con el validador real esta en ContratoH2v2IT). */
@DisplayName("H2-v2: regla de la mitad (V15), techos de V26 y version del prompt")
class ContratoRepartoTest {

    @Test
    @DisplayName("V15: la mitad solo con 3+ grupos y 4+ ejercicios de trabajo, division entera")
    void mitad() {
        assertNull(ContratoReparto.topeMitad(2, 6), "LEGS (2 grupos): no aplica");
        assertNull(ContratoReparto.topeMitad(3, 3), "3 de trabajo: no aplica");
        assertEquals(2, ContratoReparto.topeMitad(3, 4));
        assertEquals(2, ContratoReparto.topeMitad(3, 5));
        assertEquals(3, ContratoReparto.topeMitad(5, 6));
        assertEquals(Map.of("4", 2, "5", 2, "6", 3), ContratoReparto.tablaMitad(3, 6));
        assertEquals(Map.of(), ContratoReparto.tablaMitad(WorkoutFocus.LEGS.bodyPartCodes().size(), 8));
        assertEquals(Map.of(), ContratoReparto.tablaMitad(3, 3));
        assertEquals(3, WorkoutFocus.LOWER_BODY.bodyPartCodes().size(), "LOWER_BODY: upper legs, lower legs, waist");
    }

    @Test
    @DisplayName("V26: los techos de DosisSemanalPolicy por nivel")
    void techos() {
        assertEquals(6, DosisSemanalPolicy.techoSeriesSesion("BEGINNER"));
        assertEquals(10, DosisSemanalPolicy.techoSeriesSesion("INTERMEDIATE"));
        assertEquals(12, DosisSemanalPolicy.techoSeriesSemana("BEGINNER"));
        assertEquals(20, DosisSemanalPolicy.techoSeriesSemana("INTERMEDIATE"));
        assertEquals(24, DosisSemanalPolicy.techoSeriesSemana("ADVANCED"));
    }

    @Test
    @DisplayName("Version: PR-1.7 por omision, PR-2.0 explicita, cualquier otra es un error")
    void version() {
        assertFalse(PlanPromptBuilder.esH2v2(null));
        assertFalse(PlanPromptBuilder.esH2v2(""));
        assertFalse(PlanPromptBuilder.esH2v2("PR-1.7"));
        assertTrue(PlanPromptBuilder.esH2v2(" PR-2.0 "));
        assertThrows(IllegalArgumentException.class, () -> PlanPromptBuilder.esH2v2("PR-1.9"),
                "PR-1.9 (#69) no esta en esta rama");
        assertThrows(IllegalArgumentException.class, () -> PlanPromptBuilder.esH2v2("pr-2.0"));
    }
}
