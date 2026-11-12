package main.web.services.fitsense.orchestation;

import main.web.services.fitsense.planning.domain.services.AdaptedWeekProducer;
import main.web.services.fitsense.planning.domain.services.ConservationValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sin fijar la propiedad: la retirada localizada esta ENCENDIDA por defecto desde la activacion de la configuracion
 * evaluada (11/11/2026; antes, APAGADA). Actua porque el limite de dificultad tambien esta encendido por defecto
 * (preferencia). Interruptor: FITSENSE_PAIN_LOCALIZED_REMOVAL=off.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
@DisplayName("Retirada localizada: ENCENDIDA por defecto (configuracion evaluada)")
class RetiradaLocalizadaPorDefectoIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired AdaptedWeekProducer producer;

    @Test
    @DisplayName("fitsense.adaptation.pain-localized-removal=on por defecto")
    void encendidaPorDefecto() {
        assertTrue(producer.retiradaSolicitada(), "la retirada localizada se pide por defecto");
        assertTrue(producer.retiradaLocalizada(), "y actua: el limite de dificultad esta encendido por defecto");
    }
}
