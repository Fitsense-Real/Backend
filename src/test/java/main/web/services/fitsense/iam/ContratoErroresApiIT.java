package main.web.services.fitsense.iam;

import main.web.services.fitsense.iam.infrastructure.tokens.jwt.services.TokenServiceImpl;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.interfaces.acl.PlanningContextFacade;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.BiologicalSex;
import main.web.services.fitsense.profiling.domain.model.valueobjects.FitnessLevel;
import main.web.services.fitsense.profiling.domain.model.valueobjects.GoalType;
import main.web.services.fitsense.profiling.domain.model.valueobjects.TrainingLocation;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import main.web.services.fitsense.shared.domain.model.valueobjects.TrainingWeek;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Contrato de errores y riesgos de seguridad demostrados en la revision del cierre definitivo (fase 3.2). Solo cubre
 * los huecos sin prueba: lo ya probado (aislamiento de plan/sesion, roles, cuenta inactiva) sigue en
 * {@link SeguridadRolesIT}. PostgreSQL efimero, IA apagada.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=" + ContratoErroresApiIT.SECRETO,
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
@DisplayName("Contrato de errores de la API: 401/400/404 con el cuerpo ApiErrorResource")
class ContratoErroresApiIT {

    static final String SECRETO = "clave-solo-para-pruebas-de-integracion-0123456789abcdef";
    static final String CLAVE = "clave-de-prueba-123";
    static final AtomicInteger SECUENCIA = new AtomicInteger();
    static final ObjectMapper JSON = new ObjectMapper();

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired WebApplicationContext contexto;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserProfileCommandService perfiles;
    @Autowired PlanningContextFacade planning;

    MockMvc http;

    record Cuenta(long userId, String email, String token) {}

    @BeforeEach
    void http() {
        http = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    // ----------------------------------------------------------------- autenticacion

    @Test
    @DisplayName("login fallido: 401 con el mismo mensaje para correo desconocido y contrasena incorrecta")
    void loginFallido() throws Exception {
        var c = usuario();
        var incorrecta = login(c.email(), "otra-clave-999").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andReturn().getResponse().getContentAsString();
        var desconocido = login("nadie-" + SECUENCIA.incrementAndGet() + "@pruebas.invalid", CLAVE)
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
        assertEquals(JSON.readTree(incorrecta).get("message"), JSON.readTree(desconocido).get("message"),
                "no se revela si el correo existe");
        assertFalse(incorrecta.contains(c.email()), "la respuesta no repite el correo");
    }

    @Test
    @DisplayName("JWT caducado, con otra firma, mal formado o ausente: 401")
    void tokensInvalidos() throws Exception {
        var c = usuario();
        String caducado = new TokenServiceImpl(SECRETO, -1).generateToken(c.userId(), c.email());
        String otraFirma = new TokenServiceImpl("otra-clave-distinta-para-firmar-0123456789abcdefgh", 7)
                .generateToken(c.userId(), c.email());
        // Carga alterada (otro usuario) con la firma original: suplantacion.
        var partes = c.token().split("\\.");
        var carga = JSON.readTree(new String(Base64.getUrlDecoder().decode(partes[1]), StandardCharsets.UTF_8));
        ((com.fasterxml.jackson.databind.node.ObjectNode) carga).put("sub", String.valueOf(c.userId() + 1));
        String alterado = partes[0] + "." + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(JSON.writeValueAsBytes(carga)) + "." + partes[2];
        var casos = new java.util.LinkedHashMap<String, String>();
        casos.put("caducado", caducado);
        casos.put("otra firma", otraFirma);
        casos.put("mal formado", "no.es.un.jwt");
        casos.put("carga alterada", alterado);
        for (var caso : casos.entrySet())
            assertEquals(401, http.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + caso.getValue()))
                    .andReturn().getResponse().getStatus(), caso.getKey());
        http.perform(get("/api/v1/users/me")).andExpect(status().isUnauthorized());
        como(c, get("/api/v1/users/me")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("la caducidad del JWT sale de authorization.jwt.expiration.days (7 dias)")
    void caducidadConfigurada() throws Exception {
        var c = usuario();
        var payload = JSON.readTree(new String(Base64.getUrlDecoder().decode(c.token().split("\\.")[1]),
                StandardCharsets.UTF_8));
        assertEquals(7L * 24 * 3600, payload.get("exp").asLong() - payload.get("iat").asLong());
    }

    // ----------------------------------------------------------------- entrada invalida

    @Test
    @DisplayName("identificador no numerico y JSON mal formado: 400 con ApiErrorResource")
    void entradaMalFormada() throws Exception {
        var c = usuario();
        como(c, get("/api/v1/sessions/abc")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("El parametro sessionId no tiene un formato valido."));
        http.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El cuerpo de la solicitud no es valido."));
    }

    @Test
    @DisplayName("reincorporacion por dolor sin confirmacion o sin cuerpo: 400; nunca se reincorpora")
    void reincorporacionSinConfirmacion() throws Exception {
        var c = usuario();
        como(c, post("/api/v1/users/me/pain-exclusions/755/reactivation").contentType(MediaType.APPLICATION_JSON)
                .content("{\"confirmNoPain\":null}")).andExpect(status().isBadRequest());
        como(c, post("/api/v1/users/me/pain-exclusions/755/reactivation").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // ----------------------------------------------------------------- recursos ajenos

    @Test
    @DisplayName("omitir, informar y terminar recursos de otro usuario: 404 sin revelar nada")
    void recursosAjenos() throws Exception {
        var a = usuario();
        var b = usuario();
        long planA = plan(a);
        var ws = jdbc.queryForList("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ?"
                + " ORDER BY scheduled_date", Long.class, planA);
        long w = ws.get(0);

        como(b, post("/api/v1/workouts/" + ws.get(1) + "/skip").contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"FATIGUE\"}")).andExpect(status().isNotFound());
        long pe = jdbc.queryForObject("SELECT planned_exercise_id FROM planned_workout_exercises"
                + " WHERE planned_workout_id = ? ORDER BY exercise_order LIMIT 1", Long.class, w);
        como(b, post("/api/v1/workouts/" + w + "/report").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"performedAt":"%s","sessionRpe":6,
                         "exercises":[{"plannedExerciseId":%d,"actualSets":1,"actualRepsTotal":5}]}
                        """.formatted(OffsetDateTime.now().minusHours(1), pe)))
                .andExpect(status().isNotFound());

        var res = como(a, post("/api/v1/workouts/" + w + "/start")).andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();
        var m = Pattern.compile("\"sessionId\"\\s*:\\s*(\\d+)").matcher(res);
        assertTrue(m.find(), res);
        long sesion = Long.parseLong(m.group(1));
        como(b, post("/api/v1/sessions/" + sesion + "/finish").contentType(MediaType.APPLICATION_JSON)
                .content("{\"sessionRpe\":6}")).andExpect(status().isNotFound());
        assertEquals("IN_PROGRESS", jdbc.queryForObject("SELECT status FROM workout_sessions WHERE session_id = ?",
                String.class, sesion), "la sesion ajena no se toca");
        assertEquals("SCHEDULED", jdbc.queryForObject("SELECT status FROM planned_workouts"
                + " WHERE planned_workout_id = ?", String.class, ws.get(1)), "el entrenamiento ajeno no se omite");
    }

    // ----------------------------------------------------------------- apoyo

    private ResultActions login(String email, String clave) throws Exception {
        return http.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, clave)));
    }

    private Cuenta usuario() throws Exception {
        String email = "errores" + SECUENCIA.incrementAndGet() + "@pruebas.invalid";
        http.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email":"%s","password":"%s","firstName":"Prueba","lastName":"Errores",
                         "timezone":"America/Lima","acceptsStudyParticipation":true}
                        """.formatted(email, CLAVE)))
                .andExpect(status().isCreated());
        long id = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        var res = login(email, CLAVE).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return new Cuenta(id, email, JSON.readTree(res).get("token").asText());
    }

    private ResultActions como(Cuenta c, MockHttpServletRequestBuilder peticion) throws Exception {
        return http.perform(peticion.header("Authorization", "Bearer " + c.token()));
    }

    private long plan(Cuenta c) {
        perfiles.handle(new CreateUserProfileCommand(c.userId(), LocalDate.of(1992, 5, 10), BiologicalSex.FEMALE,
                new BigDecimal("165"), new BigDecimal("68"), null, FitnessLevel.INTERMEDIATE, GoalType.GAIN_MUSCLE,
                "contrato de errores", TrainingLocation.GYM, (short) 3, List.of((short) 1, (short) 3, (short) 5),
                (short) 45, List.of("dumbbell", "barbell", "cable", "leverage machine", "bench", "band"),
                List.of(), "Ninguna", true, true, true, true, true));
        var semana = TrainingWeek.containing(LocalDate.now()).startDate().plusWeeks(1);
        return planning.generateWeeklyPlan(c.userId(), semana, PlanAdjustment.none(), null, null).orElseThrow();
    }
}
