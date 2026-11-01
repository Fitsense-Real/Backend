package main.web.services.fitsense.planning.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Indicadores de duracion y metrica aerobica ENCENDIDOS con la politica historica: cada sesion lleva
 * {@code durationInfo} con disponibilidad, estimacion, motivo y el mensaje neutral; los campos de siempre no cambian.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.api.duracion-estimada.enabled=true",
        "fitsense.metricas.aerobica.enabled=true"
})
@DisplayName("Contrato de duracion encendido: durationInfo y planningPolicy, sin cambiar los campos de siempre")
class ContratoDuracionEncendidoIT extends ContratoDuracionApiBase {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Test
    void conCamposNuevos() throws Exception {
        preparar();
        String token = participanteGimnasio("duracion-on@pruebas.invalid");
        http.perform(post("/api/v1/users/me/plan/generate").header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());
        http.perform(get("/api/v1/users/me/plan/current").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planningPolicy").value("PLAN-HIST-1"))
                .andExpect(jsonPath("$.workouts[0].expectedDurationMinutes").exists())
                .andExpect(jsonPath("$.workouts[*].durationInfo.availableMinutes", everyItem(is(60))))
                .andExpect(jsonPath("$.workouts[*].durationInfo.estimatedMinutes", everyItem(greaterThan(0))))
                .andExpect(jsonPath("$.workouts[*].durationInfo.policy", everyItem(is("PLAN-HIST-1"))))
                .andExpect(jsonPath("$.workouts[*].durationInfo.reason",
                        everyItem(is(in(new String[]{"DENTRO_DEL_TIEMPO", "DURACION_POR_CONTENIDO",
                                "REDUCCION_AUTORIZADA", "SESION_ACORTADA"})))))
                .andExpect(jsonPath("$.workouts[*].durationInfo.message",
                        everyItem(startsWith("Tu rutina tiene una duración estimada de "))))
                .andExpect(jsonPath("$.aerobicActivity.programmedWorkMinutes").exists());
    }
}
