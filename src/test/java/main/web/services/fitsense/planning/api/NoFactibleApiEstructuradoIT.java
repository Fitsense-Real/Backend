package main.web.services.fitsense.planning.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** NOFACT-0.1 ENCENDIDO con PLAN-C2D-EF-0.1: el 422 lleva el diagnostico estructurado, sin texto tecnico. */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.planning.policy=PLAN-C2D-EF-0.1",
        "fitsense.planning.policy.allow-candidates=true",
        "fitsense.api.no-factible-estructurado.enabled=true"
})
@DisplayName("NOFACT-0.1 encendido: 422 con diagnostico de traccion no disponible")
class NoFactibleApiEstructuradoIT extends NoFactibleApiBase {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Test
    void pullSinTraccion() throws Exception {
        preparar();
        String token = participanteSinTraccion("nofact-on@pruebas.invalid");
        http.perform(post("/api/v1/users/me/plan/generate").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.diagnosis.status").value("NO_FACTIBLE"))
                .andExpect(jsonPath("$.diagnosis.reason").value("PULL_NO_DISPONIBLE"))
                .andExpect(jsonPath("$.diagnosis.missingPatterns", contains("HORIZONTAL_PULL", "VERTICAL_PULL")))
                .andExpect(jsonPath("$.diagnosis.equipmentRestriction").value(true))
                .andExpect(jsonPath("$.diagnosis.optionalEquipmentCodes", hasItem("band")))
                .andExpect(jsonPath("$.diagnosis.message", not(containsString("VEF"))))
                .andExpect(jsonPath("$.diagnosis.message", containsString("No necesitas comprar nada")))
                .andExpect(jsonPath("$.message").doesNotExist());
    }
}
