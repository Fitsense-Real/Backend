package main.web.services.fitsense.notifications;

import main.web.services.fitsense.notifications.infrastructure.FirebaseTransportePush;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ESCENARIO 3 · Spring Boot REAL con push ACTIVADO y una credencial SINTETICA estructuralmente valida. Es el
 * escenario que fallaba con firebase-admin 9.11.0 ({@code NoClassDefFoundError: JacksonFactory}). Todas las llamadas
 * a Google las intercepta {@link GoogleSimulado}: ninguna sale de la JVM y no se usa ninguna credencial real.
 * <p>
 * Recorre de punta a punta el camino de produccion con el SDK de verdad: registro del FID, evento PLAN_GENERATED,
 * emisor, token OAuth y POST a FCM v1 con {@code fid}.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.push.enabled=true",
        "fitsense.push.detector-initial-delay-ms=86400000",
        "fitsense.push.sender-initial-delay-ms=86400000"
})
@Import(ArranquePushActivoIT.GoogleBloqueado.class)
@DisplayName("Arranque con push ACTIVADO y credencial sintetica: Spring Boot inicia y envia por FID (Google simulado)")
class ArranquePushActivoIT extends ArranquePushBase {

    static final GoogleSimulado GOOGLE = new GoogleSimulado();

    @TestConfiguration
    static class GoogleBloqueado {
        @Bean
        com.google.api.client.http.HttpTransport googleSimulado() {
            return GOOGLE;
        }
    }

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @DynamicPropertySource
    static void credencial(DynamicPropertyRegistry r) {
        r.add("fitsense.push.credentials-base64", GoogleSimulado::credencialSinteticaBase64);
    }

    @Test
    @DisplayName("el contexto arranca con el transporte Firebase real y el envio sale por FID al Google simulado")
    void arrancaYEnvia() throws Exception {
        assertInstanceOf(FirebaseTransportePush.class, transporte, "el SDK debe cargarse: " + transporte);
        assertTrue(transporte.disponible());
        assertEquals(0, GOOGLE.llamadas.size(), "arrancar no llama a Google");

        var c = cuenta("arranque-activo@pruebas.invalid");
        assertEquals(201, registrarDispositivo(c));
        long plan = planGeneradoAsincrono(c);
        assertEquals(1, detector.detectar());
        assertEquals(1, emisor.procesar());

        assertEquals("ENVIADO", jdbc.queryForObject("SELECT status FROM notification_events WHERE resource_id = ?",
                String.class, plan));
        assertEquals("projects/kora-fitpe/messages/0:1", jdbc.queryForObject("SELECT fcm_message_id FROM"
                + " notification_deliveries WHERE user_id = ?", String.class, c.userId()));
        var fcm = GOOGLE.llamadas.stream().filter(l -> l.url().startsWith(GoogleSimulado.URL_FCM)).toList();
        assertEquals(1, fcm.size());
        assertTrue(fcm.get(0).cuerpo().contains("\"fid\":\"" + FID + "\""), fcm.get(0).cuerpo());
        assertFalse(fcm.get(0).cuerpo().contains("\"token\""), fcm.get(0).cuerpo());
        assertTrue(GOOGLE.llamadas.stream().allMatch(l -> l.url().startsWith(GoogleSimulado.URL_TOKEN)
                || l.url().startsWith(GoogleSimulado.URL_FCM)), "solo token y FCM, interceptados");
    }
}
