package main.web.services.fitsense.planning.infrastructure.generation.ai;

import main.web.services.fitsense.configuration.domain.model.valueobjects.PrescriptionParams;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.shared.infrastructure.json.JsonSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** El techo de V4-C viaja a la IA SOLO con esa politica; con la historica, snapshot y texto no cambian. */
@DisplayName("Contrato IA y V4-C: max_session_seconds solo con V4-C; historica identica")
class ContratoV4CIATest {

    static final PrescriptionParams P = new PrescriptionParams(70, 3, 60, 5, 60, 20, null, null, 10, 180);

    static PlanGenerationContext ctx(PlanningPolicy politica) {
        var perfil = new PlanningProfile(7L, 35, "FEMALE", new BigDecimal("165"), new BigDecimal("70"), null,
                "INTERMEDIATE", "LOSE_WEIGHT", "x", "Ninguna", "GYM", 3, List.of((short) 1, (short) 3, (short) 5), 30,
                List.of("body weight"), List.of(), SafetyProfile.Declaradas.sinLimitaciones());
        return new PlanGenerationContext(7L, (short) 1, PromptCapacidadesTest.LUNES,
                PromptCapacidadesTest.LUNES.plusDays(6), perfil, PlanAdjustment.none(), PreviousWeekSummary.empty(),
                PromptCapacidadesTest.ELEGIBLES, SafetyProfile.none(), P, politica);
    }

    @Test
    void snapshot() {
        var json = new JsonSupport();
        var hist = json.write(PlanInputSnapshot.of(ctx(PlanningPolicy.HISTORICA)));
        var c2d = json.write(PlanInputSnapshot.of(ctx(PlanningPolicy.C2D)));
        var v4c = json.write(PlanInputSnapshot.of(ctx(PlanningPolicy.V4C)));
        assertFalse(hist.contains("max_session_seconds"), "la historica no cambia el contrato");
        assertEquals(hist, c2d, "C2-D no cambia lo que ve la IA");
        assertTrue(v4c.contains("\"max_session_seconds\":2070"), v4c);
        assertEquals(hist, v4c.replace(",\"max_session_seconds\":2070", ""), "es el unico campo nuevo");
        var fix = json.write(SessionFixInputSnapshot.of(PlanInputSnapshot.of(ctx(PlanningPolicy.V4C))));
        assertTrue(fix.contains("\"max_session_seconds\":2070"), "tambien en la correccion de sesiones");
        assertFalse(json.write(SessionFixInputSnapshot.of(PlanInputSnapshot.of(ctx(PlanningPolicy.HISTORICA))))
                .contains("max_session_seconds"));
    }

    @Test
    void textoDelPrompt() {
        var hist = PlanPromptBuilder.duracionCalculada(ctx(PlanningPolicy.HISTORICA));
        var v4c = PlanPromptBuilder.duracionCalculada(ctx(PlanningPolicy.V4C));
        assertFalse(hist.contains("2070"));
        String frase = " Ninguna sesion puede pasar de 2070 s (constraints.max_session_seconds): el backend los calcula"
                + " a partir de los ejercicios y rechaza la sesion aunque declares menos minutos.";
        assertEquals(hist, v4c.replace(frase, ""), "solo se anade una frase");
        assertTrue(v4c.contains("2070 s") && v4c.contains("aunque declares menos"), v4c);
    }
}
