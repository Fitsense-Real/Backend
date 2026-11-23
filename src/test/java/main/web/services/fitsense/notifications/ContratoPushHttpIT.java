package main.web.services.fitsense.notifications;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import main.web.services.fitsense.notifications.domain.MensajePush;
import main.web.services.fitsense.notifications.domain.TransportePush;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * CONTRATO HTTP de las notificaciones push, tal como lo vera Mobile: rutas, cabecera JWT, campos JSON exactos,
 * codigos de estado y errores, y el comportamiento en cierre de sesion, cambio de cuenta y eliminacion de cuenta.
 * <p>
 * Cada intercambio queda escrito en {@code target/contrato_push_ejemplos.md} (con el JWT sustituido por
 * {@code <JWT>}): de ahi salen los ejemplos reales de {@code docs/FitSense/notificaciones_push/CONTRATO_PUSH_MOBILE.md}.
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
@Import(ContratoPushHttpIT.Simulado.class)
@DisplayName("Notificaciones push: contrato HTTP exacto para Mobile")
class ContratoPushHttpIT {

    @TestConfiguration
    static class Simulado {
        @Bean
        @Primary
        TransportePush transporteSimulado() {
            return new TransportePush() {
                @Override
                public boolean disponible() {
                    return true;
                }

                @Override
                public Resultado enviar(String fid, MensajePush m) {
                    return Resultado.enviado("projects/kora-fitpe/messages/simulado");
                }
            };
        }
    }

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    static final ObjectMapper JSON = new ObjectMapper();
    static final String FID = "cR_9l0aBcD3fGh1jK2lM4n";
    static final String TOKEN_FCM = "dXy7:APA91bHPRgkF3JUikC4ENAHEeMrd41Zxv3hVZjC9KtT8OvPVGJ-hQMRKRrZuJAEcl7B338qju59zJMjw2DELjzEvxwYv7hH5Ynpc1ODQ0aT4U4OFEeco8ohsN5PjL1iC2dNtk2BAokeMCg2ZXKqpc8FXKmhX94kIxQ";
    static final Set<String> CAMPOS_DISPOSITIVO = Set.of("deviceId", "installationIdHint", "platform", "appVersion",
            "registeredAt", "lastSeenAt", "receivesNotifications");
    static final Set<String> CAMPOS_ERROR = Set.of("timestamp", "status", "error", "message", "details");

    @Autowired WebApplicationContext contexto;
    @Autowired JdbcTemplate jdbc;
    MockMvc http;
    final StringBuilder ejemplos = new StringBuilder("# Ejemplos reales del contrato push (generados por ContratoPushHttpIT)\n");

    record Cuenta(long userId, String email, String token) {}

    @Test
    @DisplayName("rutas, JWT, campos, errores, cierre de sesion, cambio de cuenta y eliminacion")
    void contrato() throws Exception {
        http = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        var a = cuenta("contrato-a@pruebas.invalid");
        var b = cuenta("contrato-b@pruebas.invalid");

        seccion("1. Registro de dispositivo");
        var r = llamar(a, put("/api/v1/users/me/push-devices"), """
                {"installationId":"%s","platform":"ANDROID","appId":"pe.fit.kora","appVersion":"1.0.0+12"}""".formatted(FID));
        assertEquals(201, r.getResponse().getStatus());
        assertEquals(CAMPOS_DISPOSITIVO, campos(json(r)));
        assertEquals("cR_9…", json(r).get("installationIdHint").asText());
        assertTrue(json(r).get("receivesNotifications").asBoolean());

        seccion("2. Registro repetido (renovacion o arranque): idempotente");
        var r2 = llamar(a, put("/api/v1/users/me/push-devices"), """
                {"installationId":"%s","platform":"ANDROID","appId":"pe.fit.kora"}""".formatted(FID));
        assertEquals(200, r2.getResponse().getStatus());
        assertEquals(json(r).get("deviceId"), json(r2).get("deviceId"));
        assertTrue(json(r2).get("appVersion").isNull(), "appVersion es opcional");

        seccion("3. Errores del registro");
        error(llamar(a, put("/api/v1/users/me/push-devices"), """
                {"installationId":"%s","platform":"ANDROID","appId":"pe.fit.kora"}""".formatted(TOKEN_FCM)),
                400, "IDENTIFICADOR_PUSH_INVALIDO");
        error(llamar(a, put("/api/v1/users/me/push-devices"), """
                {"installationId":"%s","platform":"ANDROID","appId":"com.example.fitsense_mobile"}""".formatted(FID)),
                400, "APLICACION_NO_ADMITIDA");
        error(llamar(a, put("/api/v1/users/me/push-devices"), """
                {"installationId":"%s","platform":"IOS","appId":"pe.fit.kora"}""".formatted(FID)),
                400, "PLATAFORMA_NO_ADMITIDA");
        var faltan = llamar(a, put("/api/v1/users/me/push-devices"), "{\"installationId\":\"%s\"}".formatted(FID));
        assertEquals(400, faltan.getResponse().getStatus());
        assertEquals(CAMPOS_ERROR, campos(json(faltan)));
        assertTrue(json(faltan).get("details").toString().contains("platform"));
        var sinJwt = llamar(null, put("/api/v1/users/me/push-devices"), """
                {"installationId":"%s","platform":"ANDROID","appId":"pe.fit.kora"}""".formatted(FID));
        assertEquals(401, sinJwt.getResponse().getStatus());

        seccion("4. Consulta");
        var lista = llamar(a, get("/api/v1/users/me/push-devices"), null);
        assertEquals(200, lista.getResponse().getStatus());
        assertEquals(1, json(lista).size());
        assertEquals(CAMPOS_DISPOSITIVO, campos(json(lista).get(0)));
        assertFalse(lista.getResponse().getContentAsString().contains(FID), "el FID completo nunca vuelve");

        seccion("5. Preferencias");
        var pref = llamar(a, get("/api/v1/users/me/notification-preferences"), null);
        assertEquals(Set.of("planUpdates", "accountNotices"), campos(json(pref)));
        assertTrue(json(pref).get("planUpdates").asBoolean());
        var pref2 = llamar(a, put("/api/v1/users/me/notification-preferences"),
                "{\"planUpdates\":false,\"accountNotices\":true}");
        assertEquals(200, pref2.getResponse().getStatus());
        assertFalse(json(pref2).get("planUpdates").asBoolean());
        var prefMal = llamar(a, put("/api/v1/users/me/notification-preferences"), "{\"planUpdates\":false}");
        assertEquals(400, prefMal.getResponse().getStatus());

        seccion("6. Cambio de cuenta en el mismo telefono");
        assertEquals(201, llamar(b, put("/api/v1/users/me/push-devices"), """
                {"installationId":"%s","platform":"ANDROID","appId":"pe.fit.kora","appVersion":"1.0.0+12"}"""
                .formatted(FID)).getResponse().getStatus());
        var listaA = llamar(a, get("/api/v1/users/me/push-devices"), null);
        assertEquals(0, json(listaA).size(), "A ya no tiene ese dispositivo");

        seccion("7. Cierre de sesion: desvinculacion por ruta y por cuerpo (las dos son validas e idempotentes)");
        assertEquals(204, llamar(b, delete("/api/v1/users/me/push-devices/" + FID), null).getResponse().getStatus());
        assertEquals(204, llamar(b, delete("/api/v1/users/me/push-devices"),
                "{\"installationId\":\"%s\"}".formatted(FID)).getResponse().getStatus());
        assertEquals(0, json(llamar(b, get("/api/v1/users/me/push-devices"), null)).size());
        assertEquals(400, llamar(b, delete("/api/v1/users/me/push-devices"), "{}").getResponse().getStatus());
        assertEquals(400, llamar(b, delete("/api/v1/users/me/push-devices"), "").getResponse().getStatus(),
                "DELETE sin cuerpo ni FID en la ruta");

        seccion("8. Eliminacion de cuenta: el JWT deja de valer y no quedan dispositivos");
        llamar(b, put("/api/v1/users/me/push-devices"), """
                {"installationId":"%s","platform":"ANDROID","appId":"pe.fit.kora"}""".formatted(FID));
        var eliminar = llamar(b, delete("/api/v1/users/me"),
                "{\"password\":\"clave-sintetica-123\",\"confirm\":true}");
        assertEquals(200, eliminar.getResponse().getStatus(), eliminar.getResponse().getContentAsString());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM push_devices WHERE user_id = ?",
                Integer.class, b.userId()), "ningun dispositivo de la cuenta eliminada");
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM push_devices WHERE installation_id = ?"
                + " AND revoked_at IS NULL", Integer.class, FID), "el FID ya no esta vinculado a nadie");
        assertEquals(401, llamar(b, get("/api/v1/users/me/push-devices"), null).getResponse().getStatus());

        Files.createDirectories(Path.of("target"));
        Files.writeString(Path.of("target/contrato_push_ejemplos.md"), ejemplos.toString());
    }

    // ================================================================ apoyo

    void seccion(String t) {
        ejemplos.append("\n## ").append(t).append('\n');
    }

    MvcResult llamar(Cuenta c, MockHttpServletRequestBuilder p, String cuerpo) throws Exception {
        if (c != null) p = p.header("Authorization", "Bearer " + c.token());
        if (cuerpo != null) p = p.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
        var r = http.perform(p).andReturn();
        var req = r.getRequest();
        ejemplos.append("\n```http\n").append(req.getMethod()).append(' ').append(req.getRequestURI()).append('\n');
        if (c != null) ejemplos.append("Authorization: Bearer <JWT>\n");
        if (cuerpo != null) ejemplos.append("Content-Type: application/json\n\n").append(cuerpo.strip()).append('\n');
        ejemplos.append("```\n\n```http\nHTTP ").append(r.getResponse().getStatus()).append('\n');
        var s = r.getResponse().getContentAsString();
        if (!s.isBlank()) ejemplos.append('\n').append(JSON.writerWithDefaultPrettyPrinter()
                .writeValueAsString(JSON.readTree(s))).append('\n');
        ejemplos.append("```\n");
        return r;
    }

    static JsonNode json(MvcResult r) throws Exception {
        return JSON.readTree(r.getResponse().getContentAsString());
    }

    static Set<String> campos(JsonNode n) {
        var s = new TreeSet<String>();
        n.fieldNames().forEachRemaining(s::add);
        return s;
    }

    static void error(MvcResult r, int estado, String codigo) throws Exception {
        assertEquals(estado, r.getResponse().getStatus(), r.getResponse().getContentAsString());
        assertEquals(CAMPOS_ERROR, campos(json(r)));
        assertEquals(codigo, json(r).get("details").get(0).asText());
    }

    Cuenta cuenta(String email) throws Exception {
        http.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"%s","password":"clave-sintetica-123","firstName":"Sintetico","lastName":"Contrato",
                 "timezone":"America/Lima","acceptsStudyParticipation":true}""".formatted(email)));
        String res = http.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"clave-sintetica-123\"}".formatted(email)))
                .andReturn().getResponse().getContentAsString();
        Matcher m = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"").matcher(res);
        assertTrue(m.find(), res);
        return new Cuenta(jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email), email,
                m.group(1));
    }
}
