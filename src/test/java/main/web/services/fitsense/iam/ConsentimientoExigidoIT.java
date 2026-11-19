package main.web.services.fitsense.iam;

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

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * Lo que pasaria al encender la exigencia, con versiones SINTETICAS de prueba (no son documentos reales ni
 * aprobados): el registro exige aceptar las versiones vigentes y guarda las tres filas en la misma transaccion.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.consent.terms-version=PRUEBA-T1",
        "fitsense.consent.privacy-version=PRUEBA-P1",
        "fitsense.consent.enforce-on-signup=true"
})
@DisplayName("Consentimientos con la exigencia encendida (versiones sinteticas de prueba)")
class ConsentimientoExigidoIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired WebApplicationContext contexto;
    @Autowired JdbcTemplate jdbc;
    MockMvc http;

    @BeforeEach
    void http() {
        http = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    int registrar(String email, String extra) throws Exception {
        return http.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"%s","password":"Clave-exigida-1","firstName":"Sintetico","timezone":"America/Lima",
                 "acceptsStudyParticipation":true%s}""".formatted(email, extra))).andReturn().getResponse().getStatus();
    }

    @Test
    @DisplayName("sin aceptar, o con una version que no es la vigente: 422 y ninguna cuenta; con ambas: 201 y 3 filas")
    void registroExigido() throws Exception {
        assertEquals(422, registrar("e1@pruebas.invalid", ""));
        assertEquals(422, registrar("e2@pruebas.invalid", ",\"acceptedTermsVersion\":\"PRUEBA-T1\""));
        assertEquals(422, registrar("e3@pruebas.invalid",
                ",\"acceptedTermsVersion\":\"PRUEBA-T0\",\"acceptedPrivacyVersion\":\"PRUEBA-P1\""));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM users WHERE email LIKE 'e_@pruebas.invalid'",
                Integer.class), "ninguna cuenta a medias");

        assertEquals(201, registrar("e4@pruebas.invalid",
                ",\"acceptedTermsVersion\":\"PRUEBA-T1\",\"acceptedPrivacyVersion\":\"PRUEBA-P1\""));
        var tipos = jdbc.queryForList("SELECT consent_type || ':' || document_version FROM user_consents c JOIN users u"
                + " USING (user_id) WHERE u.email = 'e4@pruebas.invalid' ORDER BY 1", String.class);
        assertEquals(java.util.List.of("PRIVACY_POLICY:PRUEBA-P1", "STUDY_PARTICIPATION:MVP-1.0",
                "TERMS_OF_SERVICE:PRUEBA-T1"), tipos);
        assertTrue(new ObjectMapper().readTree(http.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"e4@pruebas.invalid\",\"password\":\"Clave-exigida-1\"}")).andReturn()
                .getResponse().getContentAsString()).has("token"));
    }
}
