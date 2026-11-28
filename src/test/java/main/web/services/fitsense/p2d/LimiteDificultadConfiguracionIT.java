package main.web.services.fitsense.p2d;

import main.web.services.fitsense.planning.domain.services.ConservationValidator;
import main.web.services.fitsense.planning.domain.services.ConservationValidator.ReglaPatron;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * El limite de dificultad candidato se activa SOLO con la propiedad explicita, y no cambia la regla de patron (H1).
 * El valor por defecto (apagado) lo comprueba ConservacionPatronH1IT.reglaPorDefecto.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.adaptation.difficulty-guard=on"
})
@DisplayName("Limite de dificultad (candidato): solo con difficulty-guard=on, sin tocar la regla de patron")
class LimiteDificultadConfiguracionIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired ConservationValidator v8c;

    @Test
    @DisplayName("difficulty-guard=on activa el limite; la regla de patron sigue siendo MULTICONJUNTO")
    void activoConPropiedad() {
        assertTrue(v8c.limitaDificultad());
        assertEquals(ReglaPatron.MULTICONJUNTO, v8c.reglaPatron());
    }
}
