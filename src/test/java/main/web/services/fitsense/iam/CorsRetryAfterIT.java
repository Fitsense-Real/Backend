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

import static org.junit.jupiter.api.Assertions.*;

/**
 * C7 · el navegador de la app (Mobile en web) no podia leer {@code Retry-After} de un 429: no estaba en
 * {@code Access-Control-Expose-Headers}. Con servidor HTTP real, como lo vera un navegador. Solo los origenes
 * configurados lo reciben; un origen ajeno no recibe ninguna cabecera CORS.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.cors.allowed-origins=https://panel.pruebas.invalid",
        "fitsense.auth-rate-limit.register-per-ip=1"
})
@DisplayName("C7 · CORS: Retry-After legible por el navegador solo para los origenes configurados")
class CorsRetryAfterIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    static final String PROPIO = "https://panel.pruebas.invalid";

    @LocalServerPort int puerto;
    final HttpClient http = HttpClient.newHttpClient();

    HttpResponse<String> registrar(String origen) throws Exception {
        var cuerpo = "{\"email\":\"cors-" + UUID.randomUUID() + "@pruebas.invalid\",\"password\":\"Clave-cors-1\","
                + "\"firstName\":\"Sintetico\",\"timezone\":\"America/Lima\",\"acceptsStudyParticipation\":false}";
        return http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + puerto + "/api/v1/auth/register"))
                .header("Origin", origen).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo)).build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    @DisplayName("429 desde un origen autorizado: Retry-After presente y expuesto; el cuerpo lleva el codigo")
    void retryAfterExpuesto() throws Exception {
        assertEquals(201, registrar(PROPIO).statusCode());
        var r = registrar(PROPIO);
        assertEquals(429, r.statusCode(), r.body());
        assertTrue(r.headers().firstValue("Retry-After").isPresent());
        assertEquals(PROPIO, r.headers().firstValue("Access-Control-Allow-Origin").orElse(null));
        assertTrue(r.headers().allValues("Access-Control-Expose-Headers").stream()
                .anyMatch(v -> v.toLowerCase().contains("retry-after")), r.headers().map().toString());
        // Sin poder leer la cabecera, la app aun tiene un mensaje seguro y el codigo.
        assertTrue(r.body().contains("DEMASIADAS_SOLICITUDES"));
    }

    @Test
    @DisplayName("origen ajeno: rechazado sin ninguna cabecera CORS (no se abre a cualquiera)")
    void origenAjeno() throws Exception {
        var r = registrar("https://ajeno.pruebas.invalid");
        assertEquals(403, r.statusCode());
        assertTrue(r.headers().firstValue("Access-Control-Allow-Origin").isEmpty());
        assertTrue(r.headers().firstValue("Access-Control-Expose-Headers").isEmpty());
    }

    @Test
    @DisplayName("preflight del origen autorizado: metodos y cabeceras de siempre, sin credenciales")
    void preflight() throws Exception {
        var r = http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + puerto + "/api/v1/auth/register"))
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody()).header("Origin", PROPIO)
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type").build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(PROPIO, r.headers().firstValue("Access-Control-Allow-Origin").orElse(null));
        assertTrue(r.headers().firstValue("Access-Control-Allow-Credentials").map(v -> !v.equals("true")).orElse(true));
    }
}
