package main.web.services.fitsense.planning.infrastructure.generation.ai;

import main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static main.web.services.fitsense.planning.infrastructure.generation.ai.SessionFixPromptV2Test.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * H12-A (prototipo, apagado): con {@code fitsense.adaptation.variante.policy=reduccion-demostrable} el prompt solo
 * ofrece, en VARIANTE, sustitutos de MENOR dificultad. Con la politica historica el texto es identico byte a byte.
 */
@DisplayName("H12-A · prompt: sustitutos de menor dificultad (prototipo) e identidad con la politica historica")
class PromptVarianteH12Test {

    static final SessionFixPromptBuilder V1_HIST = new SessionFixPromptBuilder(JSON, "v1", "historica");
    static final SessionFixPromptBuilder V1_A = new SessionFixPromptBuilder(JSON, "v1", "reduccion-demostrable");
    static final SessionFixPromptBuilder V2_HIST = new SessionFixPromptBuilder(JSON, "v2", "historica");
    static final SessionFixPromptBuilder V2_A = new SessionFixPromptBuilder(JSON, "v2", "reduccion-demostrable");

    @Test
    @DisplayName("1. Politica historica: el prompt (V1 y V2) es identico byte a byte al de siempre")
    void historicaIdentica() {
        for (var e : java.util.List.of(u04(), u09(), u19(), u03(), u08(), u18())) {
            assertEquals(texto(V1, e, null), texto(V1_HIST, e, null));
            assertEquals(texto(V2, e, null), texto(V2_HIST, e, null));
        }
    }

    @Test
    @DisplayName("2. U04 (GM intermedio): con H12-A solo quedan sustitutos de menor dificultad (72 por 380); 496 (dif. 1) sin ninguno")
    void u04SoloMenor() {
        var hist = FactibilidadAdaptacion.calcular(u04().ctx(), u04().der(), false).get(0);
        var a = FactibilidadAdaptacion.calcular(u04().ctx(), u04().der(), true).get(0);
        assertTrue(hist.sustitutos().stream().anyMatch(s -> s.reemplaza() == 496L), hist.toString());
        assertTrue(a.sustitutos().stream().noneMatch(s -> s.reemplaza() == 496L), a.toString());
        assertTrue(a.sustitutos().stream().anyMatch(s -> s.reemplaza() == 380L && s.exerciseId() == 72L), a.toString());
        assertTrue(a.hayCambioFactible());
        assertTrue(plano(texto(V1_A, u04(), null)).contains("SOLO de MENOR dificultad"));
        assertFalse(plano(texto(V1, u04(), null)).contains("SOLO de MENOR dificultad"));
    }

    @Test
    @DisplayName("3. GM principiante (todo dificultad 1): con H12-A no hay sustituto ni dosis -> V2 dice que se omita")
    void gmPrincipianteSinSolucion() {
        var e = escenario("GAIN_MUSCLE", "BEGINNER", 45, catalogoAg(), null,
                110L, TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA, sesionAg());
        var hist = FactibilidadAdaptacion.calcular(e.ctx(), e.der(), false).get(0);
        var a = FactibilidadAdaptacion.calcular(e.ctx(), e.der(), true).get(0);
        assertTrue(a.sustitutos().isEmpty(), a.toString());
        assertTrue(a.opciones().isEmpty(), "GM: 4D sin reducciones");
        assertFalse(a.hayCambioFactible());
        assertTrue(plano(texto(V2_A, e, null)).contains("OMITELA"), "V2 indica conservar");
        assertTrue(hist.sustitutos().size() >= a.sustitutos().size());
    }

    @Test
    @DisplayName("4. El filtro solo toca VARIANTE: INVESTIGAR y FATIGA no cambian con H12-A")
    void soloVariante() {
        for (var e : java.util.List.of(u08(), u03(), u18())) {
            var hist = FactibilidadAdaptacion.calcular(e.ctx(), e.der(), false);
            var a = FactibilidadAdaptacion.calcular(e.ctx(), e.der(), true);
            assertEquals(hist, a);
        }
    }
}
