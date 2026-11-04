package main.web.services.fitsense.p2d;

import main.web.services.fitsense.planning.domain.services.ConservationValidator.ReglaPatron;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * H1 con la configuracion de PRODUCCION (sin fijar la propiedad): la regla por defecto es MULTICONJUNTO. El plan que
 * pierde un patron se rechaza en los tres intentos y la semana se conserva; el plan que conserva la distribucion se
 * publica.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
@DisplayName("H1 · V8C multiconjunto (por defecto): el caso U04 de extremo a extremo")
class ConservacionPatronH1IT extends ConservacionPatronH1Base {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Test
    @DisplayName("la regla de produccion por defecto es MULTICONJUNTO")
    void reglaPorDefecto() {
        assertEquals(ReglaPatron.MULTICONJUNTO, v8c.reglaPatron());
        assertFalse(v8c.limitaDificultad(), "el limite de dificultad candidato esta APAGADO por defecto");
    }

    @Test
    @DisplayName("distribucion incorrecta (B sustituido por otro ejercicio del patron de A): rechazada y conservada")
    void distribucionIncorrectaRechazada() {
        var c = caso();
        long nuevo = proponer(c, c.a1(), c.a2())
                .orElseThrow(() -> new AssertionError("sin respaldo: " + intento(c.p())));
        var fb = feedbacks(3);
        assertTrue(fb.get(1).toString().contains("solo retira 1 de ese patron"), fb.get(1).toString());
        assertCopiaExacta(c.p(), nuevo);
        assertEquals("CONSERVED", intento(c.p()).get("generation_source"));
        assertEquals(3, ((Number) intento(c.p()).get("ai_calls")).intValue());
        assertEquals(distribucion(c.sesion()), distribucion(senaladaPublicada(c, nuevo)));
    }

    @Test
    @DisplayName("justificacion verificable: el adjustment_reason publicado describe el cambio real (menor, igual o mayor dificultad registrada)")
    void justificacionVerificable() {
        var c = caso();
        long nuevo = proponer(c, c.a1(), c.b1())
                .orElseThrow(() -> new AssertionError("no publico: " + intento(c.p())));
        String texto = jdbc.queryForObject("SELECT adjustment_reason FROM weekly_training_plans WHERE plan_id = ?",
                String.class, nuevo);
        assertFalse(texto.toLowerCase(java.util.Locale.ROOT).contains("bajamos la exigencia"), texto);
        assertTrue(texto.contains("Nos dijiste que una sesión te resultó difícil."), texto);
        int dA = dificultad(c.a1()) < dificultad(c.a().id()) ? 1 : 0, dB = dificultad(c.b1()) < dificultad(c.b().id()) ? 1 : 0;
        int mA = dificultad(c.a1()) > dificultad(c.a().id()) ? 1 : 0, mB = dificultad(c.b1()) > dificultad(c.b().id()) ? 1 : 0;
        assertFalse(texto.contains("Adherencia observada"), texto);
        if (dA + dB > 0) assertTrue(texto.contains("usamos una variante registrada como de menor dificultad"), texto);
        if (mA + mB > 0) assertTrue(texto.contains("registrado como de mayor dificultad")
                || texto.contains("registrados como de mayor dificultad"), texto);
        if (dA + dB + mA + mB < 2) assertTrue(texto.contains("con la misma dificultad registrada: es una alternativa, no"
                + " necesariamente más fácil"), texto);
    }

    private int dificultad(long id) {
        return jdbc.queryForObject("SELECT difficulty_level FROM exercises WHERE exercise_id = ?", Integer.class, id);
    }

    @Test
    @DisplayName("distribucion correcta (cada sustituto de su patron): publicada; no senaladas intactas")
    void distribucionCorrectaPublicada() {
        var c = caso();
        long nuevo = proponer(c, c.a1(), c.b1())
                .orElseThrow(() -> new AssertionError("no publico: " + intento(c.p())));
        var publicada = senaladaPublicada(c, nuevo);
        assertTrue(publicada.ejercicios().stream().anyMatch(e -> e.id() == c.a1()));
        assertTrue(publicada.ejercicios().stream().anyMatch(e -> e.id() == c.b1()));
        assertEquals(distribucion(c.sesion()), distribucion(publicada), "la distribucion de patrones se conserva");
        assertEquals("AI", intento(c.p()).get("generation_source"));
        assertEquals(1, ((Number) intento(c.p()).get("ai_calls")).intValue());
        assertNoSenaladasIntactas(c, nuevo);
        // Ninguna dosis sube: los sustitutos llevan la dosis del original.
        for (int i = 0; i < c.sesion().ejercicios().size(); i++) {
            var antes = c.sesion().ejercicios().get(i);
            var despues = publicada.ejercicios().get(i);
            assertEquals(antes.series(), despues.series());
            assertEquals(antes.reps(), despues.reps());
            assertEquals(antes.descanso(), despues.descanso());
        }
    }
}
