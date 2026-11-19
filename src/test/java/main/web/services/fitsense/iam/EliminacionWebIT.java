package main.web.services.fitsense.iam;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.internet.MimeMessage;
import main.web.services.fitsense.shared.infrastructure.mail.EnvioDeCorreo;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * Backend de la pagina web de eliminacion: solicitud por correo, token de un solo uso, mismo procedimiento de
 * borrado que la app y estado por referencia. SMTP simulado (GreenMail), PostgreSQL efimero, sin Replicate.
 */
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.mail.transport=smtp",
        "fitsense.mail.from=no-reply@pruebas.invalid",
        "fitsense.public-rate-limit.max-per-window=6",
        "fitsense.account-deletion.web.link-template=https://web.pruebas.invalid/eliminar-cuenta?token={token}"
})
@DisplayName("Eliminacion web: correo verificado, un solo uso, mismo borrado, sin enumeracion")
class EliminacionWebIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    static final GreenMail SMTP = new GreenMail(ServerSetupTest.SMTP.dynamicPort());

    static {
        SMTP.start();
    }

    @DynamicPropertySource
    static void smtp(DynamicPropertyRegistry r) {
        r.add("spring.mail.host", () -> "127.0.0.1");
        r.add("spring.mail.port", () -> SMTP.getSmtp().getPort());
    }

    @AfterAll
    static void parar() {
        SMTP.stop();
    }

    static final String CLAVE = "Clave-web-123";
    static final AtomicInteger SECUENCIA = new AtomicInteger();
    static final AtomicInteger IP = new AtomicInteger(10);
    static final ObjectMapper JSON = new ObjectMapper();

    @Autowired WebApplicationContext contexto;
    @Autowired JdbcTemplate jdbc;
    @Autowired EnvioDeCorreo envio;
    MockMvc http;
    String ip;

    @BeforeEach
    void http() throws Exception {
        http = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        ip = "203.0.113." + IP.incrementAndGet();
        SMTP.purgeEmailFromAllMailboxes();
    }

    MockHttpServletRequestBuilder desde(MockHttpServletRequestBuilder p) {
        return p.with(r -> {
            r.setRemoteAddr(ip);
            return r;
        });
    }

    MvcResult json(MockHttpServletRequestBuilder p, String cuerpo) throws Exception {
        return http.perform(desde(p).contentType(MediaType.APPLICATION_JSON).content(cuerpo)).andReturn();
    }

    String cuenta() throws Exception {
        String email = "web" + SECUENCIA.incrementAndGet() + "@pruebas.invalid";
        assertEquals(201, json(post("/api/v1/auth/register"), """
                {"email":"%s","password":"%s","firstName":"Sintetico","lastName":"Web",
                 "timezone":"America/Lima","acceptsStudyParticipation":true}""".formatted(email, CLAVE))
                .getResponse().getStatus());
        return email;
    }

    String login(String email) throws Exception {
        var r = json(post("/api/v1/auth/login"), "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, CLAVE));
        if (r.getResponse().getStatus() != 200) return null;
        Matcher m = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"").matcher(r.getResponse().getContentAsString());
        assertTrue(m.find());
        return m.group(1);
    }

    MvcResult solicitar(String email) throws Exception {
        return json(post("/api/v1/account-deletion/requests"), "{\"email\":\"%s\"}".formatted(email));
    }

    MvcResult confirmar(String token, Boolean confirm) throws Exception {
        return json(post("/api/v1/account-deletion/confirm"),
                confirm == null ? "{\"token\":\"%s\"}".formatted(token)
                        : "{\"token\":\"%s\",\"confirm\":%s}".formatted(token, confirm));
    }

    MimeMessage[] correos(int n) throws Exception {
        assertTrue(envio.esperarVacio(10_000));
        SMTP.waitForIncomingEmail(5_000, n);
        return SMTP.getReceivedMessages();
    }

    static String token(MimeMessage m) throws Exception {
        Matcher t = Pattern.compile("eliminar-cuenta\\?token=([A-Za-z0-9_-]{43})").matcher((String) m.getContent());
        assertTrue(t.find(), "el correo lleva el enlace");
        return t.group(1);
    }

    int existe(String email) {
        return jdbc.queryForObject("SELECT count(*) FROM users WHERE email = ?", Integer.class, email);
    }

    @Test
    @DisplayName("sin app ni contrasena: correo con enlace, confirmacion explicita, cuenta eliminada con el mismo"
            + " procedimiento, estado por referencia sin datos, token de un solo uso")
    void flujoCompleto(CapturedOutput salida) throws Exception {
        String email = cuenta();
        String jwt = login(email);
        var r = solicitar(email);
        assertEquals(202, r.getResponse().getStatus());
        var m = correos(1)[0];
        assertEquals(email, m.getAllRecipients()[0].toString());
        assertEquals("Kora: confirma la eliminacion de tu cuenta", m.getSubject());
        String token = token(m);
        assertNotEquals(token, jdbc.queryForObject("SELECT token_hash FROM account_deletion_tokens WHERE user_id ="
                + " (SELECT user_id FROM users WHERE email = ?)", String.class, email), "solo el hash");

        // Abrir el enlace (GET) no borra nada: no hay endpoint GET de confirmacion.
        int get = http.perform(desde(get("/api/v1/account-deletion/confirm").param("token", token))).andReturn()
                .getResponse().getStatus();
        assertTrue(get == 405 || get == 404 || get == 401 || get == 403, "GET no confirma: " + get);
        assertEquals(1, existe(email));
        // Sin confirm=true tampoco.
        var sinConfirmar = confirmar(token, null);
        assertEquals(400, sinConfirmar.getResponse().getStatus());
        assertTrue(sinConfirmar.getResponse().getContentAsString().contains("CONFIRMACION_REQUERIDA"));
        assertEquals(1, existe(email));

        var hecho = confirmar(token, true);
        assertEquals(200, hecho.getResponse().getStatus(), hecho.getResponse().getContentAsString());
        JsonNode cuerpo = JSON.readTree(hecho.getResponse().getContentAsString());
        assertEquals("DELETED", cuerpo.get("status").asText());
        assertFalse(hecho.getResponse().getContentAsString().contains(email));
        assertEquals(0, existe(email));
        assertEquals("WEB", jdbc.queryForObject("SELECT origin FROM account_deletions WHERE deletion_id = ?",
                String.class, UUID.fromString(cuerpo.get("deletionId").asText())));
        assertNull(login(email), "ya no puede entrar");
        assertEquals(401, http.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + jwt)).andReturn()
                .getResponse().getStatus(), "la sesion de la app queda revocada");

        var estado = http.perform(desde(get("/api/v1/account-deletion/requests/{r}", cuerpo.get("deletionId")
                .asText()))).andReturn();
        assertEquals(200, estado.getResponse().getStatus());
        var e = JSON.readTree(estado.getResponse().getContentAsString());
        assertEquals("DELETED", e.get("status").asText());
        assertNotNull(e.get("completedAt"));
        assertFalse(estado.getResponse().getContentAsString().contains("@"), "el estado no expone datos");

        assertEquals(400, confirmar(token, true).getResponse().getStatus(), "un solo uso");

        String log = salida.getAll().lines().filter(l -> !l.contains("c.icegreen.greenmail"))
                .collect(Collectors.joining("\n"));
        assertTrue(log.contains("CORREO_ENVIADO tipo=ELIMINACION_CUENTA_WEB"));
        assertFalse(log.contains(token), "el token no llega al log");
        assertFalse(log.contains(email), "el correo no llega al log");
    }

    @Test
    @DisplayName("correo inexistente: la misma respuesta y ningun correo; referencia desconocida: 404")
    void sinEnumeracion() throws Exception {
        String email = cuenta();
        var si = solicitar(email);
        var no = solicitar("nadie" + SECUENCIA.incrementAndGet() + "@pruebas.invalid");
        assertEquals(si.getResponse().getStatus(), no.getResponse().getStatus());
        assertEquals(si.getResponse().getContentAsString(), no.getResponse().getContentAsString());
        assertEquals(1, correos(1).length);
        Thread.sleep(500);
        assertEquals(1, SMTP.getReceivedMessages().length);
        assertEquals(404, http.perform(desde(get("/api/v1/account-deletion/requests/{r}", UUID.randomUUID())))
                .andReturn().getResponse().getStatus());
        assertEquals(400, http.perform(desde(get("/api/v1/account-deletion/requests/no-es-uuid"))).andReturn()
                .getResponse().getStatus());
        assertEquals(1, existe(email), "solicitar no borra nada");
    }

    @Test
    @DisplayName("token caducado o inventado: 400 con el mismo mensaje y la cuenta sigue")
    void tokenNoValido() throws Exception {
        String email = cuenta();
        solicitar(email);
        String token = token(correos(1)[0]);
        jdbc.update("UPDATE account_deletion_tokens SET created_at = now() - interval '2 hours',"
                + " expires_at = now() - interval '1 minute'");
        var caducado = confirmar(token, true);
        var inventado = confirmar("A".repeat(43), true);
        assertEquals(400, caducado.getResponse().getStatus());
        assertEquals(400, inventado.getResponse().getStatus());
        assertEquals(JSON.readTree(caducado.getResponse().getContentAsString()).get("message"),
                JSON.readTree(inventado.getResponse().getContentAsString()).get("message"));
        assertEquals(1, existe(email));
    }

    @Test
    @DisplayName("limite por cuenta: como mucho 3 correos por hora; la respuesta no cambia")
    void limitePorCuenta() throws Exception {
        String email = cuenta();
        for (int i = 0; i < 4; i++) {
            ip = "203.0.113." + IP.incrementAndGet();
            assertEquals(202, solicitar(email).getResponse().getStatus());
        }
        correos(3);
        Thread.sleep(500);
        assertEquals(3, SMTP.getReceivedMessages().length);
    }

    @Test
    @DisplayName("limite por IP: 429 con Retry-After en solicitud y confirmacion")
    void limitePorIp() throws Exception {
        for (int i = 0; i < 6; i++) assertEquals(202, solicitar("x" + i + "@pruebas.invalid").getResponse().getStatus());
        var r = solicitar("x7@pruebas.invalid");
        assertEquals(429, r.getResponse().getStatus());
        assertNotNull(r.getResponse().getHeader("Retry-After"));
        for (int i = 0; i < 6; i++) assertEquals(400, confirmar("B".repeat(43), true).getResponse().getStatus());
        assertEquals(429, confirmar("B".repeat(43), true).getResponse().getStatus(), "frena la fuerza bruta");
    }
}
