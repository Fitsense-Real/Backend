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

/**
 * ESCENARIO 2 · Spring Boot REAL con push ACTIVADO pero sin credencial valida (Base64 que no es una cuenta de
 * servicio). Comportamiento seguro documentado: arranca, el envio queda apagado de hecho y NO se acumulan eventos
 * imposibles de enviar; los endpoints de dispositivos siguen funcionando.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.push.enabled=true",
        // {"type":"service_account"}: estructura incompleta, sin clave.
        "fitsense.push.credentials-base64=eyJ0eXBlIjoic2VydmljZV9hY2NvdW50In0=",
        "fitsense.push.detector-initial-delay-ms=86400000",
        "fitsense.push.sender-initial-delay-ms=86400000"
})
@DisplayName("Arranque con push ACTIVADO sin credencial valida: inicia, envio apagado, sin eventos acumulados")
class ArranquePushSinCredencialesIT extends ArranquePushBase {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Test
    @DisplayName("el contexto arranca; no se detectan ni envian eventos; el registro de FID funciona")
    void arrancaSeguro() throws Exception {
        assertInstanceOf(PushConfiguracion.TransporteNoDisponible.class, transporte);
        assertEquals("sin credenciales", ((PushConfiguracion.TransporteNoDisponible) transporte).motivo());
        var c = cuenta("arranque-sin-cred@pruebas.invalid");
        assertEquals(201, registrarDispositivo(c), "la app puede registrarse antes de activar el envio");
        planGeneradoAsincrono(c);
        assertEquals(0, detector.detectar());
        assertEquals(0, emisor.procesar());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM notification_events", Integer.class),
                "ningun evento imposible de enviar");
    }
}
