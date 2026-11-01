package main.web.services.fitsense.planning.api;

import main.web.services.fitsense.planning.infrastructure.generation.ai.ReplicateSessionFixGenerator;
import main.web.services.fitsense.planning.infrastructure.generation.ai.ReplicateTrainingPlanGenerator;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.*;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Un participante de gimnasio, 3 dias de 60 min, GENERAL_FITNESS intermedio: genera siempre una semana valida. */
abstract class ContratoDuracionApiBase {

    static final String CLAVE = "ClaveSegura-123";

    @MockitoBean ReplicateSessionFixGenerator ia;
    @MockitoBean ReplicateTrainingPlanGenerator iaPlan;
    @Autowired WebApplicationContext contexto;
    @Autowired UserProfileCommandService perfiles;
    @Autowired JdbcTemplate jdbc;

    MockMvc http;

    void preparar() {
        when(ia.isEnabled()).thenReturn(false);
        when(iaPlan.isEnabled()).thenReturn(false);
        http = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    String participanteGimnasio(String email) throws Exception {
        http.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"%s","password":"%s","firstName":"Prueba","lastName":"Duracion",
                 "timezone":"America/Lima","acceptsStudyParticipation":true}""".formatted(email, CLAVE)))
                .andExpect(status().isCreated());
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1990, 6, 15), BiologicalSex.FEMALE,
                new BigDecimal("168"), new BigDecimal("68"), null, FitnessLevel.INTERMEDIATE, GoalType.GENERAL_FITNESS,
                "duracion", TrainingLocation.GYM, (short) 3, List.of((short) 1, (short) 3, (short) 5), (short) 60,
                List.of("body weight", "dumbbell", "barbell", "cable", "leverage machine", "bench", "band"), List.of(),
                "Ninguna", true, true, true, true, true));
        var res = http.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, CLAVE)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var m = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"").matcher(res);
        assertTrue(m.find(), res);
        return m.group(1);
    }
}
