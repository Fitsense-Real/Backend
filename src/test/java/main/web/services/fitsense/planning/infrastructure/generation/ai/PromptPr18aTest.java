package main.web.services.fitsense.planning.infrastructure.generation.ai;

import main.web.services.fitsense.planning.domain.model.valueobjects.PlanGenerationContext;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanningPolicy;
import main.web.services.fitsense.shared.infrastructure.json.JsonSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Confirmacion final de pr-1.8a (10/11/2026): variante EXPERIMENTAL y apagada por defecto del prompt de generacion.
 * Solo anade, antes de los datos de entrada, las dos lineas con los numeros de V15 y V26 que el backend ya usa.
 */
class PromptPr18aTest {

    static final JsonSupport JSON = new JsonSupport();

    static PlanGenerationContext conPrescripcion(String objetivo) {
        var c = PromptCapacidadesTest.contexto(objetivo);
        return new PlanGenerationContext(c.userId(), c.weekNumber(), c.weekStartDate(), c.weekEndDate(), c.profile(),
                c.adjustment(), c.previousWeek(), c.availableExercises(), c.safety(), SessionFixPromptV2Test.P)
                .conPolitica(PlanningPolicy.REV2);
    }

    @Test
    @DisplayName("por defecto PR-1.7, identico al explicito y sin el bloque de coherencia")
    void pr17PorDefecto() {
        for (var objetivo : List.of("GAIN_MUSCLE", "LOSE_WEIGHT", "INCREASE_STRENGTH", "GENERAL_FITNESS")) {
            var ctx = conPrescripcion(objetivo);
            var defecto = new PlanPromptBuilder(JSON).build(ctx, "{}", null);
            assertEquals(defecto, new PlanPromptBuilder(JSON, "pr-1.7").build(ctx, "{}", null), objetivo);
            assertFalse(defecto.contains("COHERENCIA CON LOS VALIDADORES"), objetivo);
        }
        assertEquals(PlanPromptBuilder.VERSION, new PlanPromptBuilder(JSON).version());
    }

    @Test
    @DisplayName("pr-1.8a = PR-1.7 + las lineas V15/V26 antes de los datos; sin dosis orientativa ni estructura")
    void pr18aSoloV15V26() {
        for (var objetivo : List.of("GAIN_MUSCLE", "LOSE_WEIGHT", "INCREASE_STRENGTH", "GENERAL_FITNESS")) {
            var ctx = conPrescripcion(objetivo);
            var base = new PlanPromptBuilder(JSON).build(ctx, "{}", null);
            var a = new PlanPromptBuilder(JSON, "pr-1.8a").build(ctx, "{}", null);
            int corte = base.indexOf("\nDatos de entrada:");
            assertTrue(a.startsWith(base.substring(0, corte)), objetivo);
            assertTrue(a.endsWith(base.substring(corte)), "despues del bloque, lo mismo que PR-1.7: " + objetivo);
            assertTrue(a.contains("COHERENCIA CON LOS VALIDADORES (PR-1.8a-EXP)"), objetivo);
            assertTrue(a.contains("- REPARTO (V15)"), objetivo);
            assertTrue(a.contains("- VOLUMEN POR GRUPO (V26)"), objetivo);
            assertFalse(a.contains("DOSIS ORIENTATIVA"), objetivo);
            assertFalse(a.contains("ESTRUCTURA DE TU OBJETIVO"), objetivo);
        }
        assertEquals(PlanPromptBuilder.VERSION_V18A, new PlanPromptBuilder(JSON, " PR-1.8a ").version());
    }

    @Test
    @DisplayName("solo se admiten pr-1.7 y pr-1.8a: pr-1.8, pr-1.8b y pr-1.8c no existen en esta rama")
    void otrasVariantesRechazadas() {
        for (var v : List.of("pr-1.8", "pr-1.8b", "pr-1.8c", "pr-9"))
            assertThrows(IllegalArgumentException.class, () -> new PlanPromptBuilder(JSON, v), v);
    }
}
