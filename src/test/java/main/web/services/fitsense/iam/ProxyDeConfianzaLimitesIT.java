package main.web.services.fitsense.iam;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * B1.2 · P0-02: el limite por IP NO se esquiva mandando {@code X-Forwarded-For}. Con Tomcat real
 * ({@code server.forward-headers-strategy=native}), la cabecera solo se cree si la peticion llega de un proxy de
 * confianza ({@code server.tomcat.remoteip.internal-proxies}). Aqui el unico proxy de confianza es 192.0.2.10 y las
 * peticiones llegan de 127.0.0.1: cada {@code X-Forwarded-For} inventada se ignora y todas cuentan para 127.0.0.1.
 * El caso contrario (proxy de confianza, como Railway) esta en {@link ProxyDeConfianzaRailwayIT}.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "server.forward-headers-strategy=native",
        "server.tomcat.remoteip.internal-proxies=192\\\\.0\\\\.2\\\\.10",
        "fitsense.auth-rate-limit.register-per-ip=3"
})
@DisplayName("P0-02 · un cliente que no es proxy de confianza no esquiva el limite con X-Forwarded-For")
class ProxyDeConfianzaLimitesIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @LocalServerPort int puerto;

    static int registrar(int puerto, String xff) throws Exception {
        var cuerpo = """
                {"email":"proxy-%s@pruebas.invalid","password":"Clave-de-prueba-123","firstName":"Sintetico",
                 "acceptsStudyParticipation":false}""".formatted(UUID.randomUUID());
        var r = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + puerto
                        + "/api/v1/auth/register")).header("Content-Type", "application/json")
                .header("X-Forwarded-For", xff).header("X-Forwarded-Proto", "https")
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo)).build(), HttpResponse.BodyHandlers.ofString());
        return r.statusCode();
    }

    @Test
    @DisplayName("tres altas con tres X-Forwarded-For distintas agotan el cupo de 127.0.0.1: la cuarta da 429")
    void cabeceraFalsaIgnorada() throws Exception {
        for (int i = 1; i <= 3; i++) assertEquals(201, registrar(puerto, "203.0.113." + i));
        assertEquals(429, registrar(puerto, "203.0.113.99"));
    }
}
