package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.PrescriptionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static main.web.services.fitsense.planning.domain.services.ImpactoPolicy.Nivel.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("P-C2: impacto en tres niveles, independiente de la dificultad")
class ImpactoPolicyTest {

    static CandidateExercise c(long id, String nombre, Boolean alto, int dificultad) {
        return new CandidateExercise(id, nombre, "cardio", "body weight", dificultad, PrescriptionType.DURATION,
                "cardiovascular system", "LOCOMOTION_CARDIO", alto);
    }

    @Test
    @DisplayName("el caso observado en R': cross trainer (bajo, dif 2) -> jumping jacks (moderado, dif 1) SUBE el impacto")
    void casoJ07() {
        var cross = c(532, "Ciclo cross trainer", false, 2);
        var jacks = c(53, "Saltos con las piernas separadas", true, 1);
        assertEquals(BAJO, ImpactoPolicy.nivel(cross));
        assertEquals(MODERADO, ImpactoPolicy.nivel(jacks));
        assertTrue(ImpactoPolicy.sube(cross, jacks), "mas facil por dificultad, pero de mas impacto");
        assertFalse(ImpactoPolicy.sube(jacks, cross));
    }

    @Test
    @DisplayName("saltos, burpees y sprints: ALTO; trote y skipping: MODERADO; marcha, bici y fuerza sin salto: BAJO")
    void niveles() {
        assertEquals(ALTO, ImpactoPolicy.nivel(c(1, "Sentadilla con salto", true, 2)));
        assertEquals(ALTO, ImpactoPolicy.nivel(c(2, "Zancada con salto", true, 2)));
        assertEquals(ALTO, ImpactoPolicy.nivel(c(3, "Burpee", true, 2)));
        assertEquals(MODERADO, ImpactoPolicy.nivel(c(4, "Trote en el sitio", false, 1)));
        assertEquals(MODERADO, ImpactoPolicy.nivel(c(5, "Skipping", true, 1)));
        assertEquals(BAJO, ImpactoPolicy.nivel(c(6, "Marcha en el sitio", false, 1)));
        assertEquals(BAJO, ImpactoPolicy.nivel(c(7, "Bicicleta estatica", false, 1)));
        assertEquals(BAJO, ImpactoPolicy.nivel(c(8, "Sentadilla con peso corporal", false, 1)));
    }

    @Test
    @DisplayName("sin dato del catalogo: DESCONOCIDO, nunca bajo; solo se admite un sustituto BAJO")
    void desconocido() {
        var sinDato = c(9, "Ejercicio X", null, 1);
        assertEquals(DESCONOCIDO, ImpactoPolicy.nivel(sinDato));
        assertTrue(ImpactoPolicy.sube(c(10, "Marcha", false, 1), sinDato));
        assertFalse(ImpactoPolicy.sube(sinDato, c(11, "Marcha", false, 1)));
        assertTrue(ImpactoPolicy.sube(sinDato, c(12, "Trote", false, 1)));
    }

    @Test
    @org.junit.jupiter.api.DisplayName("cierre fase 5: polea con cuerda es BAJO; saltar la cuerda y la comba siguen MODERADO")
    void cuerdaDePolea() {
        assertEquals(BAJO, ImpactoPolicy.nivel(c(901, "Remo sentado en polea con cuerda", false, 1)));
        assertEquals(BAJO, ImpactoPolicy.nivel(c(902, "Extension de triceps con cuerda", false, 1)));
        assertEquals(MODERADO, ImpactoPolicy.nivel(c(903, "Saltar la cuerda", false, 1)));
        assertEquals(MODERADO, ImpactoPolicy.nivel(c(904, "Comba basica", false, 1)));
    }
}
