package main.web.services.fitsense.planning.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Retrocompatibilidad: con los indicadores de duracion y metrica aerobica APAGADOS (por defecto), el plan no lleva
 * {@code planningPolicy}, {@code aerobicActivity} ni {@code durationInfo}: el JSON es el de siempre.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
@DisplayName("Contrato de duracion apagado: el recurso del plan es el de siempre")
class ContratoDuracionApagadoIT extends ContratoDuracionApiBase {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Test
    void sinCamposNuevos() throws Exception {
        preparar();
        String token = participanteGimnasio("duracion-off@pruebas.invalid");
        http.perform(post("/api/v1/users/me/plan/generate").header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.workouts[0].expectedDurationMinutes").exists())
                .andExpect(jsonPath("$.planningPolicy").doesNotExist())
                .andExpect(jsonPath("$.aerobicActivity").doesNotExist())
                .andExpect(jsonPath("$.workouts[*].durationInfo").isEmpty());
        http.perform(get("/api/v1/users/me/plan/current").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planningPolicy").doesNotExist())
                .andExpect(jsonPath("$.workouts[*].durationInfo").isEmpty());
    }
}
