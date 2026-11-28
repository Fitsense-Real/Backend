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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Configuracion de publicacion con un servidor HTTP REAL (no MockMvc): solo asi pasa la peticion por el RemoteIpValve
 * de Tomcat que interpreta X-Forwarded-Proto/For como lo hara detras del proxy de Railway. La peticion llega de
 * 127.0.0.1, que Tomcat considera proxy de confianza, igual que la red interna de Railway.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        // El perfil prod exige estas variables (sin valor por omision); @ServiceConnection pone la base real.
        "SPRING_DATASOURCE_URL=jdbc:postgresql://no-se-usa.invalid/x",
        "SPRING_DATASOURCE_USERNAME=no-se-usa",
        "DB_PASSWORD=no-se-usa",
        "fitsense.cors.allowed-origins=https://panel.pruebas.invalid"
})
@org.springframework.test.context.ActiveProfiles("prod")
@org.springframework.context.annotation.Import(DespliegueHttpsIT.Sonda.class)
@DisplayName("Despliegue · HTTPS obligatorio tras proxy, cabeceras, health y CORS (servidor real)")
class DespliegueHttpsIT {

    /** Solo en esta prueba: devuelve la IP que ve la aplicacion (la que usan los limites por IP). */
    @org.springframework.boot.test.context.TestConfiguration
    @org.springframework.web.bind.annotation.RestController
    static class Sonda {
        @org.springframework.web.bind.annotation.GetMapping("/api/v1/auth/prueba-ip")
        String ip(jakarta.servlet.http.HttpServletRequest r) {
            return r.getRemoteAddr() + "|" + r.isSecure();
        }
    }

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @LocalServerPort int puerto;
    final HttpClient http = HttpClient.newHttpClient();

    HttpResponse<String> pedir(String metodo, String ruta, String cuerpo, String... cabeceras) throws Exception {
        var b = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + puerto + ruta))
                .method(metodo, cuerpo == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(cuerpo));
        if (cuerpo != null) b.header("Content-Type", "application/json");
        if (cabeceras.length > 0) b.headers(cabeceras);
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    static final String LOGIN = "{\"email\":\"nadie@pruebas.invalid\",\"password\":\"Clave-incorrecta-1\"}";

    @Test
    @DisplayName("HTTP en claro: 403 HTTPS_REQUERIDO sin tocar la API; health exento y sin detalles")
    void httpRechazado() throws Exception {
        var r = pedir("POST", "/api/v1/auth/login", LOGIN);
        assertEquals(403, r.statusCode(), r.body());
        assertTrue(r.body().contains("HTTPS_REQUERIDO"));
        var sinProxy = pedir("POST", "/api/v1/auth/login", LOGIN, "X-Forwarded-Proto", "http");
        assertEquals(403, sinProxy.statusCode(), "el proxy dice http: rechazado");

        var salud = pedir("GET", "/actuator/health", null);
        assertEquals(200, salud.statusCode(), salud.body());
        assertTrue(salud.body().contains("\"status\":\"UP\""), salud.body());
        assertFalse(salud.body().contains("components") || salud.body().contains("details")
                || salud.body().contains("db"), "sin componentes ni detalles: " + salud.body());
    }

    @Test
    @DisplayName("HTTPS segun el proxy: la API responde y lleva HSTS, nosniff, DENY, no-referrer, CSP y no-store")
    void httpsTrasProxy() throws Exception {
        var r = pedir("POST", "/api/v1/auth/login", LOGIN, "X-Forwarded-Proto", "https",
                "X-Forwarded-For", "198.51.100.7");
        assertEquals(401, r.statusCode(), "llega a la API (credenciales incorrectas): " + r.body());
        var h = r.headers();
        assertEquals("max-age=31536000 ; includeSubDomains",
                h.firstValue("Strict-Transport-Security").orElse(null));
        assertEquals("nosniff", h.firstValue("X-Content-Type-Options").orElse(null));
        assertEquals("DENY", h.firstValue("X-Frame-Options").orElse(null));
        assertEquals("no-referrer", h.firstValue("Referrer-Policy").orElse(null));
        assertEquals("default-src 'none'; frame-ancestors 'none'",
                h.firstValue("Content-Security-Policy").orElse(null));
        assertTrue(h.firstValue("Cache-Control").orElse("").contains("no-store"));
        assertTrue(h.allValues("Set-Cookie").isEmpty(), "API sin sesion ni cookies");
    }

    @Test
    @DisplayName("actuator: solo health; env, info, beans y metrics no se exponen")
    void actuatorCerrado() throws Exception {
        for (var ruta : new String[]{"/actuator", "/actuator/env", "/actuator/info", "/actuator/beans",
                "/actuator/metrics", "/actuator/health/db"}) {
            var r = pedir("GET", ruta, null, "X-Forwarded-Proto", "https");
            assertTrue(r.statusCode() == 401 || r.statusCode() == 403 || r.statusCode() == 404,
                    ruta + " -> " + r.statusCode() + " " + r.body());
            assertFalse(r.body().contains("jdbc") || r.body().contains("password"), ruta);
        }
    }

    @Test
    @DisplayName("CORS: solo el origen configurado; un origen ajeno no recibe Access-Control-Allow-Origin")
    void cors() throws Exception {
        var propio = pedir("OPTIONS", "/api/v1/auth/login", null, "X-Forwarded-Proto", "https",
                "Origin", "https://panel.pruebas.invalid", "Access-Control-Request-Method", "POST");
        assertEquals("https://panel.pruebas.invalid",
                propio.headers().firstValue("Access-Control-Allow-Origin").orElse(null));
        var ajeno = pedir("OPTIONS", "/api/v1/auth/login", null, "X-Forwarded-Proto", "https",
                "Origin", "https://ajeno.pruebas.invalid", "Access-Control-Request-Method", "POST");
        assertTrue(ajeno.headers().firstValue("Access-Control-Allow-Origin").isEmpty());
        assertNotEquals(200, ajeno.statusCode());
    }

    @Test
    @DisplayName("perfil prod: la IP del cliente sale de X-Forwarded-For y el esquema de X-Forwarded-Proto")
    void ipYEsquemaDelProxy() throws Exception {
        var r = pedir("GET", "/api/v1/auth/prueba-ip", null, "X-Forwarded-Proto", "https",
                "X-Forwarded-For", "198.51.100.7");
        assertEquals(200, r.statusCode(), r.body());
        assertEquals("198.51.100.7|true", r.body());
    }

    @Test
    @DisplayName("perfil prod: sin documentacion publica de la API")
    void sinSwagger() throws Exception {
        for (var ruta : new String[]{"/v3/api-docs", "/swagger-ui/index.html"}) {
            var r = pedir("GET", ruta, null, "X-Forwarded-Proto", "https");
            assertNotEquals(200, r.statusCode(), ruta);
        }
    }
}
