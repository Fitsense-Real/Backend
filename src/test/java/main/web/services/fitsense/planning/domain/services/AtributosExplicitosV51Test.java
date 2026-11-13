package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.PrescriptionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * V51 · el impacto y el estiramiento son ATRIBUTOS EXPLICITOS: renombrar o traducir un ejercicio no cambia su impacto ni
 * su exclusion funcional. La regla por nombre solo actua como respaldo cuando el candidato no trae el dato.
 */
@DisplayName("V51 · impacto y estiramiento explicitos, estables ante cambios de nombre")
class AtributosExplicitosV51Test {

    static CandidateExercise c(String nombre, boolean alto, String impacto, Boolean estiramiento, String patron) {
        return new CandidateExercise(1L, nombre, "cardio", "body weight", 1, PrescriptionType.DURATION,
                "cardiovascular system", patron, alto, false, impacto, estiramiento);
    }

    @ParameterizedTest(name = "«{0}» hi={1} explicito={2} -> {3}")
    @CsvSource({
            // El nombre dice "correr" o "salto", pero el dato explicito manda.
            "Empujar para correr, false, BAJO, BAJO",
            "Elevación de rodilla con patada frontal alterna, false, MODERADO, MODERADO",
            "Saltos con las piernas separadas, true, ALTO, ALTO",
            "Marcha rápida en el sitio, false, MODERADO, MODERADO",
            "Salto al cajón, true, MODERADO, MODERADO"
    })
    void elImpactoExplicitoMandaSobreElNombre(String nombre, boolean alto, String impacto, String esperado) {
        assertEquals(ImpactoPolicy.Nivel.valueOf(esperado), ImpactoPolicy.nivel(c(nombre, alto, impacto, false, "LOCOMOTION_CARDIO")));
    }

    @Test
    @DisplayName("renombrar no cambia el impacto cuando el dato es explicito")
    void renombrarNoCambiaElImpacto() {
        var antes = c("Empujar para correr", false, "BAJO", false, "LOCOMOTION_CARDIO");
        var despues = c("Elevación de rodilla con patada frontal alterna", false, "BAJO", false, "LOCOMOTION_CARDIO");
        assertEquals(ImpactoPolicy.nivel(antes), ImpactoPolicy.nivel(despues));
        assertFalse(ImpactoPolicy.sube(antes, despues));
        // Sin el dato, el respaldo por nombre si cambia: es exactamente la dependencia que V51 elimina.
        var antesSinDato = c("Empujar para correr", false, null, null, "LOCOMOTION_CARDIO");
        var despuesSinDato = c("Elevación de rodilla con patada frontal alterna", false, null, null, "LOCOMOTION_CARDIO");
        assertNotEquals(ImpactoPolicy.nivel(antesSinDato), ImpactoPolicy.nivel(despuesSinDato));
    }

    @Test
    @DisplayName("un valor fuera del dominio se trata como desconocido, nunca como bajo")
    void valorFueraDeDominio() {
        assertEquals(ImpactoPolicy.Nivel.DESCONOCIDO, ImpactoPolicy.nivel(c("Caminar", false, "LEVE", false, "LOCOMOTION_CARDIO")));
        assertTrue(ImpactoPolicy.sube(c("Caminar", false, "BAJO", false, "LOCOMOTION_CARDIO"),
                c("Caminar", false, "LEVE", false, "LOCOMOTION_CARDIO")));
    }

    @Test
    @DisplayName("sin el dato explicito, el respaldo es identico a la regla anterior a V51")
    void respaldoIdenticoAlAnterior() {
        for (var nombre : new String[]{"Trote en el sitio", "Burpee", "Sentadilla", "Jumping jacks", "Salto con comba"})
            for (var hi : new Boolean[]{true, false, null})
                assertEquals(ImpactoPolicy.derivadoDelNombre(nombre, hi),
                        ImpactoPolicy.nivel(new CandidateExercise(1L, nombre, "cardio", "body weight", 1,
                                PrescriptionType.DURATION, "cardiovascular system", "LOCOMOTION_CARDIO", hi, false,
                                null, null)));
    }

    /** Zona dentro de enfoque, para que solo decida el estiramiento (cardio, cuello y antebrazos se excluyen por zona). */
    static CandidateExercise pierna(String nombre, Boolean estiramiento, String patron) {
        return new CandidateExercise(2L, nombre, "lower legs", "body weight", 1, PrescriptionType.DURATION, "calves",
                patron, false, false, "BAJO", estiramiento);
    }

    @Test
    @DisplayName("estiramiento: decide el dato explicito, no la palabra en el nombre")
    void estiramientoExplicito() {
        var conPalabraPeroNo = pierna("Estiramiento dinámico con sentadilla", false, "KNEE_DOMINANT");
        var sinPalabraPeroSi = pierna("Gemelo contra la pared", true, "MOBILITY");
        assertTrue(CatalogEligibilityPolicy.exclusionHeredada(conPalabraPeroNo).isEmpty());
        assertEquals(CatalogEligibilityPolicy.Decision.EXCLUIDO_ESTIRAMIENTO,
                CatalogEligibilityPolicy.exclusionHeredada(sinPalabraPeroSi).orElseThrow());
        // Respaldo sin dato: la palabra en el nombre, como antes.
        var sinDato = pierna("Estiramiento de gemelos", null, "MOBILITY");
        assertEquals(CatalogEligibilityPolicy.Decision.EXCLUIDO_ESTIRAMIENTO,
                CatalogEligibilityPolicy.exclusionHeredada(sinDato).orElseThrow());
    }
}
