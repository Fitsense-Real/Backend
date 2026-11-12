package main.web.services.fitsense.planning;

import com.fasterxml.jackson.databind.ObjectMapper;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.interfaces.acl.PlanningContextFacade;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.BiologicalSex;
import main.web.services.fitsense.profiling.domain.model.valueobjects.FitnessLevel;
import main.web.services.fitsense.profiling.domain.model.valueobjects.GoalType;
import main.web.services.fitsense.profiling.domain.model.valueobjects.TrainingLocation;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Zona horaria del usuario (cierre definitivo, fase 2.4): el «hoy» de GET /plan/current es el dia LOCAL del usuario
 * y el vencimiento de las sesiones (23:59 local) respeta los cambios de horario de verano. Reloj controlado por la
 * prueba; PostgreSQL efimero; IA apagada.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
@Import(ZonaHorariaUsuarioIT.Reloj.class)
@DisplayName("Zona horaria del usuario: plan vigente segun su dia local y vencimiento con horario de verano")
class ZonaHorariaUsuarioIT {

    /** Reloj movil: cada prueba fija su instante. */
    static final AtomicReference<Instant> AHORA = new AtomicReference<>(Instant.parse("2026-11-04T12:00:00Z"));

    @TestConfiguration
    static class Reloj {
        @Bean
        Clock reloj() {
            return new Clock() {
                @Override public ZoneId getZone() { return ZoneOffset.UTC; }
                @Override public Clock withZone(ZoneId zone) { return Clock.fixed(AHORA.get(), zone); }
                @Override public Instant instant() { return AHORA.get(); }
            };
        }
    }

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    static final AtomicInteger SECUENCIA = new AtomicInteger();
    static final ObjectMapper JSON = new ObjectMapper();
    static final LocalDate SEMANA = LocalDate.of(2026, 11, 2);

    @Autowired WebApplicationContext contexto;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserProfileCommandService perfiles;
    @Autowired PlanningContextFacade planning;
    MockMvc http;

    record Cuenta(long userId, String token) {}

    @BeforeEach
    void http() {
        http = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    @Test
    @DisplayName("Lima, domingo 23:30 local (lunes 04:30 UTC): /plan/current devuelve la semana que termina")
    void limaDomingoNoche() throws Exception {
        var c = usuario("America/Lima");
        long plan = plan(c, SEMANA, List.of((short) 1, (short) 3, (short) 5));
        AHORA.set(Instant.parse("2026-11-09T04:30:00Z"));
        actual(c).andExpect(status().isOk()).andExpect(jsonPath("$.planId").value(plan));
        AHORA.set(Instant.parse("2026-11-09T05:30:00Z")); // lunes 00:30 en Lima: ya es la semana siguiente
        actual(c).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Tokio, lunes 01:30 local (domingo 16:30 UTC): la semana anterior ya no es la vigente")
    void tokioLunesMadrugada() throws Exception {
        var c = usuario("Asia/Tokyo");
        long plan = plan(c, SEMANA, List.of((short) 1, (short) 3, (short) 5));
        AHORA.set(Instant.parse("2026-11-08T14:00:00Z")); // domingo 23:00 en Tokio
        actual(c).andExpect(status().isOk()).andExpect(jsonPath("$.planId").value(plan));
        AHORA.set(Instant.parse("2026-11-08T16:30:00Z")); // lunes 01:30 en Tokio
        actual(c).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("vencimiento 23:59 local con cambio de horario: Madrid (25-oct) y Nueva York (1-nov)")
    void horarioDeVerano() throws Exception {
        verificarVencimientos("Europe/Madrid", LocalDate.of(2026, 10, 19));
        verificarVencimientos("America/New_York", LocalDate.of(2026, 10, 26));
    }

    private void verificarVencimientos(String zona, LocalDate lunes) throws Exception {
        var c = usuario(zona);
        long plan = plan(c, lunes, List.of((short) 1, (short) 3, (short) 7));
        var filas = jdbc.queryForList("SELECT scheduled_date, expires_at FROM planned_workouts WHERE plan_id = ?"
                + " ORDER BY scheduled_date", plan);
        assertFalse(filas.isEmpty());
        boolean domingo = false;
        for (var f : filas) {
            var dia = ((java.sql.Date) f.get("scheduled_date")).toLocalDate();
            var vence = ((java.sql.Timestamp) f.get("expires_at")).toInstant();
            var esperado = dia.atTime(23, 59).atZone(ZoneId.of(zona)).toInstant();
            assertEquals(esperado, vence, zona + " " + dia + ": 23:59 local con el desfase de ESE dia");
            domingo |= dia.getDayOfWeek() == DayOfWeek.SUNDAY;
        }
        assertTrue(domingo, "premisa: hay sesion el domingo del cambio de hora en " + zona);
        var lunesOffset = lunes.atTime(23, 59).atZone(ZoneId.of(zona)).getOffset();
        var domingoOffset = lunes.plusDays(6).atTime(23, 59).atZone(ZoneId.of(zona)).getOffset();
        assertNotEquals(lunesOffset, domingoOffset, "premisa: la semana cruza el cambio de hora en " + zona);
    }

    // ----------------------------------------------------------------- apoyo

    private org.springframework.test.web.servlet.ResultActions actual(Cuenta c) throws Exception {
        return http.perform(get("/api/v1/users/me/plan/current").header("Authorization", "Bearer " + c.token()));
    }

    private Cuenta usuario(String zona) throws Exception {
        String email = "zona" + SECUENCIA.incrementAndGet() + "@pruebas.invalid";
        http.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"%s","password":"clave-de-prueba-123","firstName":"Prueba","lastName":"Zona",
                 "timezone":"%s","acceptsStudyParticipation":true}
                """.formatted(email, zona))).andExpect(status().isCreated());
        var res = http.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"clave-de-prueba-123\"}".formatted(email)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long id = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        assertEquals(zona, jdbc.queryForObject("SELECT timezone FROM users WHERE user_id = ?", String.class, id));
        return new Cuenta(id, JSON.readTree(res).get("token").asText());
    }

    private long plan(Cuenta c, LocalDate lunes, List<Short> dias) {
        perfiles.handle(new CreateUserProfileCommand(c.userId(), LocalDate.of(1992, 5, 10), BiologicalSex.FEMALE,
                new BigDecimal("165"), new BigDecimal("68"), null, FitnessLevel.INTERMEDIATE,
                GoalType.GENERAL_FITNESS, "zona horaria", TrainingLocation.GYM, (short) dias.size(), dias, (short) 45,
                List.of("dumbbell", "barbell", "cable", "leverage machine", "bench", "band"),
                List.of(), "Ninguna", true, true, true, true, true));
        return planning.generateWeeklyPlan(c.userId(), lunes, PlanAdjustment.none(), null, null).orElseThrow();
    }
}
