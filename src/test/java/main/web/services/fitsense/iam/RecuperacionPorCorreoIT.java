package main.web.services.fitsense.iam;

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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * Recuperacion de contrasena con entrega real por SMTP contra un servidor simulado (GreenMail en un puerto local):
 * ningun proveedor real. Cubre el correo recibido, la invalidacion de sesiones (V61), la contrasena antigua, el uso
 * unico, la no enumeracion, el limite por IP y que ni el correo ni el codigo lleguen al log.
 */
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.password-reset.delivery=email",
        "fitsense.mail.transport=smtp",
        "fitsense.mail.from=no-reply@pruebas.invalid",
        "fitsense.public-rate-limit.max-per-window=6"
})
@DisplayName("Recuperacion por correo (SMTP simulado): entrega, sesiones invalidadas, uso unico, sin enumeracion")
class RecuperacionPorCorreoIT {

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

    static final String CLAVE = "Clave-original-123";
    static final String NUEVA = "Clave-nueva-456";
    static final AtomicInteger SECUENCIA = new AtomicInteger();
    static final AtomicInteger IP = new AtomicInteger(10);

    @Autowired WebApplicationContext contexto;
    @Autowired JdbcTemplate jdbc;
    @Autowired EnvioDeCorreo envio;
    MockMvc http;
    String ip;

    @BeforeEach
    void http() throws Exception {
        http = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        ip = "198.51.100." + IP.incrementAndGet();
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
        String email = "recupera" + SECUENCIA.incrementAndGet() + "@pruebas.invalid";
        assertEquals(201, json(post("/api/v1/auth/register"), """
                {"email":"%s","password":"%s","firstName":"Sintetico","lastName":"Correo",
                 "timezone":"America/Lima","acceptsStudyParticipation":false}""".formatted(email, CLAVE))
                .getResponse().getStatus());
        return email;
    }

    String login(String email, String clave) throws Exception {
        var r = json(post("/api/v1/auth/login"), "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, clave));
        if (r.getResponse().getStatus() != 200) return null;
        Matcher m = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"").matcher(r.getResponse().getContentAsString());
        assertTrue(m.find());
        return m.group(1);
    }

    int me(String token) throws Exception {
        return http.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token)).andReturn()
                .getResponse().getStatus();
    }

    MvcResult solicitar(String email) throws Exception {
        return json(post("/api/v1/auth/password-reset/request"), "{\"email\":\"%s\"}".formatted(email));
    }

    MimeMessage[] esperarCorreos(int n) throws Exception {
        assertTrue(envio.esperarVacio(10_000), "la cola de envio se vacia");
        SMTP.waitForIncomingEmail(5_000, n);
        return SMTP.getReceivedMessages();
    }

    static String codigo(MimeMessage m) throws Exception {
        Matcher c = Pattern.compile("codigo en la app:\\s+([A-Za-z0-9_-]{43})").matcher((String) m.getContent());
        assertTrue(c.find(), "el correo lleva el codigo");
        return c.group(1);
    }

    @Test
    @DisplayName("el correo llega con el codigo; confirmar cambia la contrasena, invalida los JWT anteriores y el"
            + " codigo es de un solo uso")
    void flujoCompleto(CapturedOutput salida) throws Exception {
        String email = cuenta();
        String antiguo = login(email, CLAVE);
        assertEquals(200, me(antiguo));

        assertEquals(202, solicitar(email).getResponse().getStatus());
        var correos = esperarCorreos(1);
        assertEquals(1, correos.length);
        var m = correos[0];
        assertEquals(email, m.getAllRecipients()[0].toString());
        assertTrue(m.getFrom()[0].toString().contains("no-reply@pruebas.invalid"));
        assertEquals("Kora: restablece tu contrasena", m.getSubject());
        String texto = (String) m.getContent();
        assertFalse(texto.contains(CLAVE), "el correo nunca lleva la contrasena");
        String token = codigo(m);
        String hash = jdbc.queryForObject("SELECT token_hash FROM password_reset_tokens WHERE user_id ="
                + " (SELECT user_id FROM users WHERE email = ?)", String.class, email);
        assertNotEquals(token, hash, "en la base solo esta el hash");

        assertEquals(204, json(post("/api/v1/auth/password-reset/confirm"),
                "{\"token\":\"%s\",\"newPassword\":\"%s\"}".formatted(token, NUEVA)).getResponse().getStatus());
        assertEquals(401, me(antiguo), "V61: el JWT emitido antes del cambio deja de servir");
        assertNull(login(email, CLAVE), "la contrasena antigua ya no entra");
        String nuevo = login(email, NUEVA);
        assertNotNull(nuevo);
        assertEquals(200, me(nuevo));
        assertEquals(400, json(post("/api/v1/auth/password-reset/confirm"),
                "{\"token\":\"%s\",\"newPassword\":\"Otra-clave-789\"}".formatted(token)).getResponse().getStatus(),
                "un solo uso");
        assertNotNull(login(email, NUEVA), "el segundo intento no cambio nada");

        // Las lineas de GreenMail (el servidor simulado, no la aplicacion) nombran el buzon: se excluyen.
        String log = salida.getAll().lines().filter(l -> !l.contains("c.icegreen.greenmail"))
                .collect(java.util.stream.Collectors.joining("\n"));
        assertTrue(log.contains("CORREO_ENVIADO tipo=RECUPERACION_CONTRASENA transporte=smtp"), "se registra el envio");
        assertFalse(log.contains(token), "el codigo no llega al log");
        assertFalse(log.contains(email), "el correo no llega al log");
        assertFalse(log.contains(NUEVA) || log.contains(CLAVE), "ninguna contrasena llega al log");
    }

    @Test
    @DisplayName("correo inexistente: la misma respuesta y ningun correo")
    void sinEnumeracion() throws Exception {
        String email = cuenta();
        var existe = solicitar(email);
        var noExiste = solicitar("nadie" + SECUENCIA.incrementAndGet() + "@pruebas.invalid");
        assertEquals(existe.getResponse().getStatus(), noExiste.getResponse().getStatus());
        assertEquals(existe.getResponse().getContentAsString(), noExiste.getResponse().getContentAsString());
        assertEquals(1, esperarCorreos(1).length);
        Thread.sleep(500);
        assertEquals(1, SMTP.getReceivedMessages().length, "solo la cuenta real recibe correo");
    }

    @Test
    @DisplayName("cuenta eliminada (PR A): no recibe codigo")
    void cuentaEliminada() throws Exception {
        String email = cuenta();
        String token = login(email, CLAVE);
        assertEquals(200, http.perform(desde(delete("/api/v1/users/me")).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"%s\",\"confirm\":true}".formatted(CLAVE)))
                .andReturn().getResponse().getStatus());
        assertEquals(202, solicitar(email).getResponse().getStatus());
        assertTrue(envio.esperarVacio(5_000));
        Thread.sleep(500);
        assertEquals(0, SMTP.getReceivedMessages().length);
    }

    @Test
    @DisplayName("limite por IP: a la septima solicitud desde la misma IP, 429 con Retry-After; otra IP sigue")
    void limitePorIp() throws Exception {
        for (int i = 0; i < 6; i++)
            assertEquals(202, solicitar("nadie" + i + "@pruebas.invalid").getResponse().getStatus());
        var bloqueada = solicitar("nadie7@pruebas.invalid");
        assertEquals(429, bloqueada.getResponse().getStatus());
        assertNotNull(bloqueada.getResponse().getHeader("Retry-After"));
        assertTrue(bloqueada.getResponse().getContentAsString().contains("DEMASIADAS_SOLICITUDES"));
        ip = "198.51.100.250";
        assertEquals(202, solicitar("nadie8@pruebas.invalid").getResponse().getStatus());
    }
}
