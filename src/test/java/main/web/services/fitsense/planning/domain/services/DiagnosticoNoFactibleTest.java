package main.web.services.fitsense.planning.domain.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** NOFACT-0.1: el diagnostico sale SOLO de un VEF NO_FACTIBLE y no expone el texto tecnico del validador. */
@DisplayName("NOFACT-0.1 · diagnostico estructurado de NO_FACTIBLE")
class DiagnosticoNoFactibleTest {

    static final String PULL_NF = "VEF: el entrenamiento PULL del 2026-11-03 no tiene ningun ejercicio de traccion"
            + " (HORIZONTAL_PULL o VERTICAL_PULL) y el catalogo elegible con el equipamiento declarado no tiene ninguno"
            + " (NO_FACTIBLE por catalogo o equipamiento insuficiente; no se sustituye por otro movimiento).";

    @Test
    void pullSinTraccion() {
        var d = DiagnosticoNoFactible.de(List.of("V20: corta", PULL_NF, PULL_NF.replace("11-03", "11-06"))).orElseThrow();
        assertEquals("NO_FACTIBLE", d.status());
        assertEquals("PULL_NO_DISPONIBLE", d.reason());
        assertEquals(List.of("HORIZONTAL_PULL", "VERTICAL_PULL"), d.missingPatterns());
        assertTrue(d.equipmentRestriction());
        assertEquals(DiagnosticoNoFactible.MATERIAL_TRACCION_VERIFICADO, d.optionalEquipmentCodes());
        for (var t : List.of("VEF", "V20", "catalogo elegible", "NO_FACTIBLE", "HORIZONTAL"))
            assertFalse(d.message().contains(t), "sin texto tecnico: " + t);
        assertTrue(d.message().contains("No necesitas comprar nada"));
    }

    @Test
    void otrosFallosNoSonDiagnostico() {
        assertTrue(DiagnosticoNoFactible.de(List.of("V20: corta", "V15: grupo")).isEmpty());
        assertTrue(DiagnosticoNoFactible.de(List.of("VEF: el entrenamiento PULL del x no tiene ningun ejercicio de"
                + " traccion; hay 3 candidatos elegibles de ese patron.")).isEmpty(), "con candidatos no es NO_FACTIBLE");
        assertTrue(DiagnosticoNoFactible.de(null).isEmpty());
    }

    @Test
    void variosPatronesSinMaterialSugerido() {
        var d = DiagnosticoNoFactible.de(List.of(PULL_NF, PULL_NF.replace("PULL del", "PUSH del"))).orElseThrow();
        assertEquals("PATRONES_NO_DISPONIBLES", d.reason());
        assertTrue(d.optionalEquipmentCodes().isEmpty(), "solo se sugiere material cuando esta demostrado");
    }
}
