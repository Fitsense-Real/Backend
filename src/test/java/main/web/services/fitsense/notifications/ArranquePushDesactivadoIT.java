package main.web.services.fitsense.notifications;

import main.web.services.fitsense.notifications.infrastructure.PushConfiguracion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.*;

/** ESCENARIO 1 · Spring Boot REAL con push DESACTIVADO (valor por omision): inicia normalmente y no envia nada. */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
@DisplayName("Arranque con push DESACTIVADO: inicia normalmente, sin eventos ni envios")
class ArranquePushDesactivadoIT extends ArranquePushBase {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Test
    @DisplayName("el contexto arranca con el transporte apagado; el registro de FID funciona")
    void arrancaApagado() throws Exception {
        assertInstanceOf(PushConfiguracion.TransporteNoDisponible.class, transporte);
        assertEquals("apagado", ((PushConfiguracion.TransporteNoDisponible) transporte).motivo());
        var c = cuenta("arranque-apagado@pruebas.invalid");
        assertEquals(201, registrarDispositivo(c));
        planGeneradoAsincrono(c);
        assertEquals(0, detector.detectar());
        assertEquals(0, emisor.procesar());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM notification_events", Integer.class));
    }
}
