package main.web.services.fitsense.planning.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** NOFACT-0.1 APAGADO (por defecto), misma politica: la respuesta es la de siempre (sin diagnostico). */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.planning.policy=PLAN-C2D-EF-0.1",
        "fitsense.planning.policy.allow-candidates=true"
})
@DisplayName("NOFACT-0.1 apagado: 422 de siempre, sin diagnostico")
class NoFactibleApiApagadoIT extends NoFactibleApiBase {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Test
    void respuestaDeSiempre() throws Exception {
        preparar();
        String token = participanteSinTraccion("nofact-off@pruebas.invalid");
        http.perform(post("/api/v1/users/me/plan/generate").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.diagnosis").doesNotExist())
                .andExpect(jsonPath("$.message").exists());
    }
}
