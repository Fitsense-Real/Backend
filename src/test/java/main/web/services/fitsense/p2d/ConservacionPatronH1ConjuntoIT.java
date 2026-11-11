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
 * COMPATIBILIDAD: la regla historica {@code conjunto}, fijada EXPLICITAMENTE (es la de antes de H1 y la de la
 * reversion). Documenta, de extremo a extremo, el hueco que H1 cierra: con {@code conjunto}, el plan que pierde un
 * patron se publica.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        // Valores historicos fijados explicitamente al activar la configuracion evaluada (11/11/2026): esta
        // prueba se escribio sobre los valores por defecto anteriores y comprueba ese comportamiento.
        "fitsense.planning.policy=PLAN-HIST-1",
        "fitsense.ai.adaptation.prompt-version=v1",
        "fitsense.adaptation.progression.enabled=false",
        "fitsense.adaptation.difficulty-guard=off",
        "fitsense.adaptation.pain-localized-removal=off",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.adaptation.v8c.pattern-rule=conjunto"
})
@DisplayName("H1 · compatibilidad: regla historica CONJUNTO explicita (reversion)")
class ConservacionPatronH1ConjuntoIT extends ConservacionPatronH1Base {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Test
    @DisplayName("con pattern-rule=conjunto la regla es CONJUNTO")
    void reglaExplicita() {
        assertEquals(ReglaPatron.CONJUNTO, v8c.reglaPatron());
    }

    @Test
    @DisplayName("comportamiento historico: la distribucion incorrecta se publica (el hueco que H1 corrige)")
    void distribucionIncorrectaPublicadaConConjunto() {
        var c = caso();
        long nuevo = proponer(c, c.a1(), c.a2())
                .orElseThrow(() -> new AssertionError("no publico: " + intento(c.p())));
        var publicada = senaladaPublicada(c, nuevo);
        assertTrue(publicada.ejercicios().stream().anyMatch(e -> e.id() == c.a2()));
        assertNotEquals(distribucion(c.sesion()), distribucion(publicada), "con conjunto se pierde el patron de B");
        assertEquals("AI", intento(c.p()).get("generation_source"));
        assertNoSenaladasIntactas(c, nuevo);
    }

    @Test
    @DisplayName("la distribucion correcta tambien se publica con conjunto")
    void distribucionCorrectaPublicadaConConjunto() {
        var c = caso();
        long nuevo = proponer(c, c.a1(), c.b1())
                .orElseThrow(() -> new AssertionError("no publico: " + intento(c.p())));
        assertEquals(distribucion(c.sesion()), distribucion(senaladaPublicada(c, nuevo)));
    }
}
