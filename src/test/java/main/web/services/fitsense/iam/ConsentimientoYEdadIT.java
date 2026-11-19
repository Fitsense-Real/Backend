package main.web.services.fitsense.iam;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
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

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * Edad minima (18) validada en el backend y consentimientos versionados con la configuracion por omision: sin
 * documentos aprobados (sin versiones de condiciones ni privacidad) y sin exigencia en el registro.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
@DisplayName("Edad minima y consentimientos (configuracion por omision: exigencia apagada)")
class ConsentimientoYEdadIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    static final String CLAVE = "Clave-consent-123";
    static final AtomicInteger SECUENCIA = new AtomicInteger();
    static final ObjectMapper JSON = new ObjectMapper();

    @Autowired WebApplicationContext contexto;
    @Autowired JdbcTemplate jdbc;
    MockMvc http;

    record Cuenta(long id, String email, String token) {}

    @BeforeEach
    void http() {
        http = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    static MvcResult hacer(MockMvc http, MockHttpServletRequestBuilder p) throws Exception {
        return http.perform(p).andReturn();
    }

    static Cuenta cuenta(MockMvc http, JdbcTemplate jdbc, boolean participa, String extra) throws Exception {
        String email = "consent" + SECUENCIA.incrementAndGet() + System.nanoTime() % 1000 + "@pruebas.invalid";
        var r = hacer(http, post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"%s","password":"%s","firstName":"Sintetico","timezone":"America/Lima",
                 "acceptsStudyParticipation":%s%s}""".formatted(email, CLAVE, participa, extra)));
        if (r.getResponse().getStatus() != 201) return new Cuenta(-r.getResponse().getStatus(), email, null);
        var l = hacer(http, post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, CLAVE)));
        Matcher m = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"").matcher(l.getResponse().getContentAsString());
        assertTrue(m.find());
        return new Cuenta(jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email), email,
                m.group(1));
    }

    MvcResult como(Cuenta c, MockHttpServletRequestBuilder p) throws Exception {
        return hacer(http, p.header("Authorization", "Bearer " + c.token()));
    }

    MvcResult perfil(Cuenta c, LocalDate nacimiento) throws Exception {
        return como(c, post("/api/v1/users/me/profile").contentType(MediaType.APPLICATION_JSON).content("""
                {"birthDate":"%s","biologicalSex":"FEMALE","heightCm":165,"currentWeightKg":68,
                 "fitnessLevel":"INTERMEDIATE","goalType":"GENERAL_FITNESS","goalText":"sintetico",
                 "trainingLocation":"HOME","daysPerWeek":3,"availableDays":[1,3,5],"sessionMinutes":45,
                 "equipmentCodes":[],"blockedExerciseIds":[],"healthNotes":"",
                 "allowHighImpact":true,"allowFloorExercises":true,"allowKneelingExercises":true,
                 "allowHangingExercises":false,"allowOverheadExercises":true}""".formatted(nacimiento)));
    }

    @Test
    @DisplayName("edad: con 17 anos, 422 y sin perfil; con 18 cumplidos hoy, 201")
    void edadMinima() throws Exception {
        var menor = cuenta(http, jdbc, true, "");
        var r = perfil(menor, LocalDate.now().minusYears(18).plusDays(1));
        assertEquals(422, r.getResponse().getStatus(), r.getResponse().getContentAsString());
        assertTrue(r.getResponse().getContentAsString().contains("al menos 18"));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM user_profiles WHERE user_id = ?", Integer.class,
                menor.id()));
        var adulta = cuenta(http, jdbc, true, "");
        assertEquals(201, perfil(adulta, LocalDate.now().minusYears(18)).getResponse().getStatus());
    }

    @Test
    @DisplayName("registro: la participacion queda registrada con version, accion y origen; sin participar, nada")
    void registroDeParticipacion() throws Exception {
        var c = cuenta(http, jdbc, true, "");
        var estado = JSON.readTree(como(c, get("/api/v1/users/me/consents")).getResponse().getContentAsString());
        assertFalse(estado.get("enforcedOnSignup").asBoolean());
        assertTrue(estado.get("currentVersions").get("TERMS_OF_SERVICE").isNull(), "sin documento aprobado");
        assertTrue(estado.get("currentVersions").get("PRIVACY_POLICY").isNull());
        JsonNode h = estado.get("history");
        assertEquals(1, h.size());
        assertEquals("STUDY_PARTICIPATION", h.get(0).get("type").asText());
        assertEquals("MVP-1.0", h.get(0).get("documentVersion").asText());
        assertEquals("CASILLA_REGISTRO", h.get(0).get("affirmativeAction").asText());
        assertEquals("APP", h.get(0).get("source").asText());
        assertTrue(h.get(0).get("withdrawnAt").isNull());

        var no = cuenta(http, jdbc, false, "");
        assertEquals(0, JSON.readTree(como(no, get("/api/v1/users/me/consents")).getResponse().getContentAsString())
                .get("history").size());
    }

    @Test
    @DisplayName("sin version aprobada no se acepta nada: ni en el registro ni despues (422)")
    void sinDocumentoAprobado() throws Exception {
        var rechazada = cuenta(http, jdbc, true, ",\"acceptedTermsVersion\":\"v1\"");
        assertEquals(-422, rechazada.id(), "no se registra una aceptacion de un documento que no existe");
        var c = cuenta(http, jdbc, true, "");
        assertEquals(422, como(c, post("/api/v1/users/me/consents").contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"TERMS_OF_SERVICE\",\"documentVersion\":\"v1\"}")).getResponse().getStatus());
        assertEquals(422, como(c, post("/api/v1/users/me/consents").contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"STUDY_PARTICIPATION\",\"documentVersion\":\"MVP-1.0\"}")).getResponse()
                .getStatus(), "la participacion solo se da al registrarse");
    }

    @Test
    @DisplayName("retirada: queda en el historial (withdrawn_at) y aplica la retirada del estudio; los dos caminos")
    void retirada() throws Exception {
        var c = cuenta(http, jdbc, true, "");
        assertEquals(204, como(c, post("/api/v1/users/me/consents/STUDY_PARTICIPATION/withdraw")).getResponse()
                .getStatus());
        assertNotNull(jdbc.queryForObject("SELECT withdrawn_at FROM users WHERE user_id = ?", Object.class, c.id()));
        assertNotNull(jdbc.queryForObject("SELECT withdrawn_at FROM user_consents WHERE user_id = ?", Object.class,
                c.id()), "la fila se conserva marcada, no se borra");
        assertEquals(422, como(c, post("/api/v1/users/me/consents/TERMS_OF_SERVICE/withdraw")).getResponse()
                .getStatus(), "condiciones y privacidad no se retiran: se elimina la cuenta");

        var d = cuenta(http, jdbc, true, "");
        assertEquals(200, como(d, post("/api/v1/users/me/withdraw")).getResponse().getStatus());
        assertNotNull(jdbc.queryForObject("SELECT withdrawn_at FROM user_consents WHERE user_id = ?", Object.class,
                d.id()), "el endpoint anterior tambien deja la retirada en el historial");
    }

    @Test
    @DisplayName("eliminar la cuenta borra tambien sus consentimientos (alternativa A)")
    void eliminacion() throws Exception {
        var c = cuenta(http, jdbc, true, "");
        assertEquals(200, como(c, delete("/api/v1/users/me").contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\":\"%s\",\"confirm\":true}".formatted(CLAVE))).getResponse().getStatus());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM user_consents WHERE user_id = ?", Integer.class,
                c.id()));
    }
}
