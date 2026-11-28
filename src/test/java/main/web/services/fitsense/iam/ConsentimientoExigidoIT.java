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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * Consentimientos con la exigencia ENCENDIDA (B2.3), con documentos SINTETICOS de prueba declarados en
 * {@code fitsense.legal-documents} (no son documentos aprobados: la referencia de aprobacion lo dice). El registro
 * exige condiciones y privacidad vigentes y, si la persona participa, la version vigente del consentimiento
 * informado; todo se guarda en la misma transaccion y un rechazo no deja ninguna cuenta a medias.
 * <p>
 * Desde B2 (27/11/2026) la exigencia encendida requiere tambien un consentimiento informado PUBLICADO: antes bastaban
 * condiciones y privacidad y la participacion quedaba con la version fija MVP-1.0.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.consent.enforce-on-signup=true",
        "fitsense.legal-documents.items[0].type=TERMS_OF_SERVICE",
        "fitsense.legal-documents.items[0].version=PRUEBA-T0",
        "fitsense.legal-documents.items[0].title=Condiciones (sintetico, anterior)",
        "fitsense.legal-documents.items[0].url=https://documentos.pruebas.invalid/t0",
        "fitsense.legal-documents.items[0].status=PUBLISHED",
        "fitsense.legal-documents.items[0].effective-from=2026-01-01",
        "fitsense.legal-documents.items[0].approval-reference=SINTETICO-SOLO-PRUEBAS",
        "fitsense.legal-documents.items[1].type=TERMS_OF_SERVICE",
        "fitsense.legal-documents.items[1].version=PRUEBA-T1",
        "fitsense.legal-documents.items[1].title=Condiciones (sintetico)",
        "fitsense.legal-documents.items[1].url=https://documentos.pruebas.invalid/t1",
        "fitsense.legal-documents.items[1].status=PUBLISHED",
        "fitsense.legal-documents.items[1].effective-from=2026-06-01",
        "fitsense.legal-documents.items[1].approval-reference=SINTETICO-SOLO-PRUEBAS",
        "fitsense.legal-documents.items[2].type=PRIVACY_POLICY",
        "fitsense.legal-documents.items[2].version=PRUEBA-P1",
        "fitsense.legal-documents.items[2].title=Privacidad (sintetico)",
        "fitsense.legal-documents.items[2].url=https://documentos.pruebas.invalid/p1",
        "fitsense.legal-documents.items[2].status=PUBLISHED",
        "fitsense.legal-documents.items[2].effective-from=2026-06-01",
        "fitsense.legal-documents.items[2].approval-reference=SINTETICO-SOLO-PRUEBAS",
        "fitsense.legal-documents.items[3].type=STUDY_PARTICIPATION",
        "fitsense.legal-documents.items[3].version=PRUEBA-CI1",
        "fitsense.legal-documents.items[3].title=Consentimiento informado (sintetico)",
        "fitsense.legal-documents.items[3].url=https://documentos.pruebas.invalid/ci1",
        "fitsense.legal-documents.items[3].status=PUBLISHED",
        "fitsense.legal-documents.items[3].effective-from=2026-06-01",
        "fitsense.legal-documents.items[3].approval-reference=SINTETICO-SOLO-PRUEBAS",
        "fitsense.legal-documents.items[4].type=STUDY_PARTICIPATION",
        "fitsense.legal-documents.items[4].version=BORRADOR-CI2",
        "fitsense.legal-documents.items[4].title=Consentimiento informado (borrador)",
        "fitsense.legal-documents.items[4].status=DRAFT"
})
@DisplayName("Consentimientos con la exigencia encendida (documentos sinteticos de prueba)")
class ConsentimientoExigidoIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired WebApplicationContext contexto;
    @Autowired JdbcTemplate jdbc;
    MockMvc http;
    static final ObjectMapper JSON = new ObjectMapper();

    @BeforeEach
    void http() {
        http = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    int registrar(String email, boolean participa, String extra) throws Exception {
        return http.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"%s","password":"Clave-exigida-1","firstName":"Sintetico","timezone":"America/Lima",
                 "acceptsStudyParticipation":%s%s}""".formatted(email, participa, extra))).andReturn().getResponse()
                .getStatus();
    }

    int cuentas(String patron) {
        return jdbc.queryForObject("SELECT count(*) FROM users WHERE email LIKE ?", Integer.class, patron);
    }

    List<String> filas(String email) {
        return jdbc.queryForList("SELECT consent_type || ':' || document_version || ':' || affirmative_action FROM"
                + " user_consents c JOIN users u USING (user_id) WHERE u.email = ? ORDER BY 1", String.class, email);
    }

    static final String TODO = ",\"acceptedTermsVersion\":\"PRUEBA-T1\",\"acceptedPrivacyVersion\":\"PRUEBA-P1\"";

    @Test
    @DisplayName("sin condiciones, sin privacidad o con una version desactualizada: 422 y ninguna cuenta")
    void registroExigido() throws Exception {
        assertEquals(422, registrar("e1@pruebas.invalid", false, ""));
        assertEquals(422, registrar("e2@pruebas.invalid", false, ",\"acceptedTermsVersion\":\"PRUEBA-T1\""));
        assertEquals(422, registrar("e3@pruebas.invalid", false,
                ",\"acceptedTermsVersion\":\"PRUEBA-T0\",\"acceptedPrivacyVersion\":\"PRUEBA-P1\""),
                "PRUEBA-T0 es una version anterior: ya no es la vigente");
        assertEquals(0, cuentas("e_@pruebas.invalid"), "ninguna cuenta a medias");
    }

    @Test
    @DisplayName("participar sin version del consentimiento informado, con otra version o con el borrador: 422")
    void participacionExigeConsentimientoInformado() throws Exception {
        assertEquals(422, registrar("ci1@pruebas.invalid", true, TODO));
        assertEquals(422, registrar("ci2@pruebas.invalid", true, TODO + ",\"acceptedStudyConsentVersion\":\"OTRA\""));
        assertEquals(422, registrar("ci3@pruebas.invalid", true,
                TODO + ",\"acceptedStudyConsentVersion\":\"BORRADOR-CI2\""), "un borrador nunca se acepta");
        assertEquals(422, registrar("ci4@pruebas.invalid", false,
                TODO + ",\"acceptedStudyConsentVersion\":\"PRUEBA-CI1\""), "version sin aceptar participar");
        assertEquals(0, cuentas("ci_@pruebas.invalid"), "ninguna cuenta a medias");
    }

    @Test
    @DisplayName("con todo correcto: 201, tres filas con las versiones enviadas y evidencia valida; login posible")
    void registroCompleto() throws Exception {
        assertEquals(201, registrar("e4@pruebas.invalid", true,
                TODO + ",\"acceptedStudyConsentVersion\":\"PRUEBA-CI1\""));
        assertEquals(List.of("PRIVACY_POLICY:PRUEBA-P1:CASILLA_REGISTRO",
                "STUDY_PARTICIPATION:PRUEBA-CI1:CASILLA_REGISTRO", "TERMS_OF_SERVICE:PRUEBA-T1:CASILLA_REGISTRO"),
                filas("e4@pruebas.invalid"));
        assertEquals("PRUEBA-CI1", jdbc.queryForObject("SELECT consent_version FROM users WHERE email = ?",
                String.class, "e4@pruebas.invalid"), "la columna anterior de users lleva la misma version");
        assertTrue(new ObjectMapper().readTree(http.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"e4@pruebas.invalid\",\"password\":\"Clave-exigida-1\"}")).andReturn()
                .getResponse().getContentAsString()).has("token"));
    }

    @Test
    @DisplayName("sin participar: condiciones y privacidad bastan; no queda ninguna fila de investigacion")
    void sinParticipar() throws Exception {
        assertEquals(201, registrar("np1@pruebas.invalid", false, TODO));
        assertEquals(List.of("PRIVACY_POLICY:PRUEBA-P1:CASILLA_REGISTRO", "TERMS_OF_SERVICE:PRUEBA-T1:CASILLA_REGISTRO"),
                filas("np1@pruebas.invalid"));
        assertFalse(jdbc.queryForObject("SELECT is_study_participant FROM users WHERE email = ?", Boolean.class,
                "np1@pruebas.invalid"), "la participacion nunca se marca sola");
    }

    @Test
    @DisplayName("GET /api/v1/legal-documents sin autenticacion: solo los vigentes publicados, nunca el borrador")
    void documentosPublicos() throws Exception {
        var r = http.perform(get("/api/v1/legal-documents")).andReturn().getResponse();
        assertEquals(200, r.getStatus(), r.getContentAsString());
        JsonNode cuerpo = JSON.readTree(r.getContentAsString());
        assertTrue(cuerpo.get("enforcedOnSignup").asBoolean());
        var versiones = new java.util.TreeMap<String, String>();
        cuerpo.get("documents").forEach(d -> {
            versiones.put(d.get("type").asText(), d.get("version").asText());
            assertEquals("PUBLISHED", d.get("status").asText());
            assertTrue(d.get("url").asText().startsWith("https://"));
        });
        assertEquals(java.util.Map.of("PRIVACY_POLICY", "PRUEBA-P1", "STUDY_PARTICIPATION", "PRUEBA-CI1",
                "TERMS_OF_SERVICE", "PRUEBA-T1"), versiones, "la mas reciente en vigor de cada tipo; sin borradores");
        assertFalse(r.getContentAsString().contains("BORRADOR") || r.getContentAsString().contains("SINTETICO"),
                "ni borradores ni la referencia interna de aprobacion");
    }
}
