package main.web.services.fitsense.iam;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** C8 · por HTTP: los errores de generacion que antes llevaban el id de la cuenta ya no lo llevan. */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.plan-generation.worker.enabled=false"
})
@DisplayName("C8 · errores de generacion sin el id interno de la cuenta")
class ErroresSinIdInternoIT extends ParticipacionBase {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired WebApplicationContext contexto;
    static final ObjectMapper JSON = new ObjectMapper();

    String jwt(MockMvc http, String email) throws Exception {
        http.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(
                "{\"email\":\"" + email + "\",\"password\":\"Clave-c8-1\",\"firstName\":\"Sintetico\","
                        + "\"timezone\":\"America/Lima\",\"acceptsStudyParticipation\":true}"));
        return JSON.readTree(http.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"Clave-c8-1\"}")).andReturn().getResponse()
                .getContentAsString()).get("token").asText();
    }

    @Test
    @DisplayName("sin perfil (404) y plan ya existente (422): el mensaje no lleva el id de la cuenta")
    void sinId() throws Exception {
        var http = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        String email = "c8-" + UUID.randomUUID() + "@pruebas.invalid";
        String token = jwt(http, email);
        long id = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);

        var sinPerfil = http.perform(post("/api/v1/users/me/plan/generate").header("Authorization", "Bearer " + token))
                .andReturn().getResponse();
        assertEquals(404, sinPerfil.getStatus());
        String m1 = JSON.readTree(sinPerfil.getContentAsString()).get("message").asText();
        assertEquals("Perfil del usuario no encontrado", m1);

        perfil(id);
        assertEquals(201, http.perform(post("/api/v1/users/me/plan/generate").header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getStatus());
        var repetido = http.perform(post("/api/v1/users/me/plan/generate").header("Authorization", "Bearer " + token))
                .andReturn().getResponse();
        String m2 = JSON.readTree(repetido.getContentAsString()).get("message").asText();
        assertTrue(repetido.getStatus() >= 400, String.valueOf(repetido.getStatus()));
        assertFalse(m2.matches("(?is).*\\busuario\\s+" + id + "\\b.*"), m2);
    }
}
