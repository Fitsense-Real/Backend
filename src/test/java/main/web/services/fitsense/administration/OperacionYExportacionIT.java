package main.web.services.fitsense.administration;

import com.fasterxml.jackson.databind.ObjectMapper;
import main.web.services.fitsense.RelojPruebas;
import main.web.services.fitsense.administration.application.internal.operations.VigilanteOperativo;
import main.web.services.fitsense.iam.infrastructure.tokens.jwt.services.TokenServiceImpl;
import main.web.services.fitsense.orchestration.application.internal.WeeklyCycleService;
import main.web.services.fitsense.planning.application.internal.generation.PlanGenerationOperationService;
import main.web.services.fitsense.planning.domain.exceptions.DemasiadasGeneracionesException;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.interfaces.acl.PlanningContextFacade;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.*;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * B5 · operacion y trazabilidad: indicadores operativos y alertas (P1-01), exportacion seudonimizada y de solo
 * lectura (P1-05, P1-09), detalle de administracion solo de participantes (P1-05), {@code calculated_at} renovado en
 * cada recalculo (P1-09) y limite por usuario en la generacion asincrona (P1-07). PostgreSQL de Testcontainers, IA
 * apagada.
 */
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(properties = {
        "authorization.jwt.secret=" + OperacionYExportacionIT.SECRETO,
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.ai.generate.max-per-hour=2",
        "fitsense.operations.alerts.enabled=false"
})
@DisplayName("B5 · indicadores operativos, alertas, exportacion seudonimizada y trazabilidad")
class OperacionYExportacionIT {

    static final String SECRETO = "clave-solo-para-pruebas-de-integracion-0123456789abcdef";

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @MockitoBean main.web.services.fitsense.planning.infrastructure.scheduling.OverdueWorkoutScheduler vencidas;

    @Autowired WebApplicationContext contexto;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserProfileCommandService perfiles;
    @Autowired PlanningContextFacade planning;
    @Autowired WeeklyCycleService ciclo;
    @Autowired VigilanteOperativo vigilante;
    @Autowired PlanGenerationOperationService operaciones;
    MockMvc http;

    static final ObjectMapper JSON = new ObjectMapper();
    static final LocalDate S1 = RelojPruebas.SEMANA_REFERENCIA;

    record Cuenta(long id, String email, String token) {}

    @BeforeEach
    void http() {
        http = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    Cuenta cuenta(String rol, boolean participante, boolean conPerfil) {
        String email = "b5-" + UUID.randomUUID() + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, role, is_study_participant, participant_code,"
                        + " consent_granted_at, enrolled_at) VALUES (?, 'x', 'Sintetico', ?, ?, ?, now(), now())",
                email, rol, participante, participante ? "P-B5-" + UUID.randomUUID().toString().substring(0, 6) : null);
        long id = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        if (conPerfil)
            perfiles.handle(new CreateUserProfileCommand(id, LocalDate.of(1990, 3, 10), BiologicalSex.FEMALE,
                    new BigDecimal("164"), new BigDecimal("63"), null, FitnessLevel.BEGINNER, GoalType.GENERAL_FITNESS,
                    "Sintetico", TrainingLocation.HOME, (short) 3, List.of((short) 1, (short) 3, (short) 5),
                    (short) 45, List.of("body weight"), List.of(), null, true, true, true, false, false));
        return new Cuenta(id, email, new TokenServiceImpl(SECRETO, 7).generateToken(id, email, 0));
    }

    org.springframework.test.web.servlet.ResultActions como(Cuenta c, String ruta) throws Exception {
        return http.perform(get(ruta).header("Authorization", "Bearer " + c.token()));
    }

    @Test
    @DisplayName("GET /admin/operations: ADMIN ve recuentos (sin correos); USER, 403")
    void indicadores() throws Exception {
        var admin = cuenta("ADMIN", false, false);
        var sinPlan = cuenta("USER", true, true);
        var r = como(admin, "/api/v1/admin/operations").andReturn().getResponse();
        assertEquals(200, r.getStatus(), r.getContentAsString());
        var cuerpo = JSON.readTree(r.getContentAsString());
        assertTrue(cuerpo.get("participantesSinPlanVigente").asLong() >= 1, r.getContentAsString());
        assertTrue(cuerpo.has("cortacircuitosGeneracionAbierto"));
        assertFalse(r.getContentAsString().contains("@"), "ningun correo");
        assertFalse(r.getContentAsString().contains(String.valueOf(sinPlan.email())));
        assertEquals(403, como(sinPlan, "/api/v1/admin/operations").andReturn().getResponse().getStatus());
    }

    @Test
    @DisplayName("alertas: participante sin plan -> ALERTA_OPERATIVA PARTICIPANTES_SIN_PLAN, sin datos de la persona")
    void alertas(CapturedOutput salida) {
        var sinPlan = cuenta("USER", true, true);
        var alertas = vigilante.revisar();
        assertTrue(alertas.contains("PARTICIPANTES_SIN_PLAN"), alertas.toString());
        assertTrue(salida.getAll().contains("ALERTA_OPERATIVA tipo=PARTICIPANTES_SIN_PLAN"));
        assertFalse(salida.getAll().contains(sinPlan.email()));
    }

    @Test
    @DisplayName("exportacion: solo participantes, seudonimizada (codigo P-xxx), con origen del plan; auditada")
    void exportacion(CapturedOutput salida) throws Exception {
        var admin = cuenta("ADMIN", false, false);
        var participante = cuenta("USER", true, true);
        var noParticipa = cuenta("USER", false, true);
        planning.generateWeeklyPlan(participante.id(), S1, PlanAdjustment.none(), null, null).orElseThrow();
        ciclo.runFor(participante.id(), S1, S1.plusWeeks(1));
        jdbc.update("UPDATE users SET is_study_participant = TRUE WHERE user_id = ?", noParticipa.id());
        planning.generateWeeklyPlan(noParticipa.id(), S1, PlanAdjustment.none(), null, null).orElseThrow();
        ciclo.runFor(noParticipa.id(), S1, S1.plusWeeks(1));
        jdbc.update("UPDATE users SET is_study_participant = FALSE WHERE user_id = ?", noParticipa.id());

        var r = como(admin, "/api/v1/admin/exports/weekly-metrics?from=" + S1 + "&to=" + S1).andReturn().getResponse();
        assertEquals(200, r.getStatus());
        assertTrue(r.getContentType().startsWith("text/csv"), r.getContentType());
        var csv = r.getContentAsString();
        var lineas = csv.lines().toList();
        assertTrue(lineas.get(0).startsWith("participant_code,study_status,week_number,week_start_date"));
        String codigo = jdbc.queryForObject("SELECT participant_code FROM users WHERE user_id = ?", String.class,
                participante.id());
        assertTrue(lineas.stream().anyMatch(l -> l.startsWith(codigo + ",ENROLLED,")), csv);
        assertTrue(lineas.stream().filter(l -> l.startsWith(codigo + ",")).allMatch(l -> l.contains(",RULE_ENGINE,")
                || l.contains(",CONSERVED,") || l.contains(",AI,")), "con el origen del plan medido: " + csv);
        assertFalse(csv.contains("@"), "ningun correo");
        assertFalse(csv.contains("Sintetico"), "ningun nombre");
        assertTrue(lineas.stream().noneMatch(l -> l.startsWith(",")), "el no participante no aparece (sin codigo)");
        assertTrue(salida.getAll().contains("ADMIN_EXPORT adminId=" + admin.id() + " export=weekly-metrics"));
        assertEquals(403, como(participante, "/api/v1/admin/exports/weekly-metrics").andReturn().getResponse()
                .getStatus());
    }

    @Test
    @DisplayName("detalle de administracion: solo de participantes; de un no participante, 404 (sin sus datos)")
    void detalleSoloParticipantes() throws Exception {
        var admin = cuenta("ADMIN", false, false);
        var participante = cuenta("USER", true, false);
        var noParticipa = cuenta("USER", false, false);
        como(admin, "/api/v1/admin/participants/" + participante.id())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
        var r = como(admin, "/api/v1/admin/participants/" + noParticipa.id()).andReturn().getResponse();
        assertEquals(404, r.getStatus());
        assertFalse(r.getContentAsString().contains(noParticipa.email()));
        assertEquals(404, como(admin, "/api/v1/admin/participants/" + noParticipa.id() + "/adherence").andReturn()
                .getResponse().getStatus());
    }

    @Test
    @DisplayName("recalcular una semana CONSERVA calculated_at (fecha del primer calculo, como documenta ADHERENCIA.md)")
    void calculadoEnSeConserva() throws Exception {
        // Revision de integracion (27/11/2026): B5 renovaba calculated_at al recalcular, pero ADHERENCIA.md documenta
        // que es la fecha del PRIMER calculo y los analisis se apoyan en ello. Se conserva; registrar los recalculos
        // necesita una columna propia (recalculated_at), propuesta con migracion posterior a V66 (07, seccion 4).
        var p = cuenta("USER", true, true);
        planning.generateWeeklyPlan(p.id(), S1, PlanAdjustment.none(), null, null).orElseThrow();
        ciclo.runFor(p.id(), S1, S1.plusWeeks(1));
        var primero = jdbc.queryForObject("SELECT calculated_at FROM weekly_user_metrics WHERE user_id = ?",
                OffsetDateTime.class, p.id());
        Thread.sleep(20);
        ciclo.runFor(p.id(), S1, S1.plusWeeks(1));
        var segundo = jdbc.queryForObject("SELECT calculated_at FROM weekly_user_metrics WHERE user_id = ?",
                OffsetDateTime.class, p.id());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM weekly_user_metrics WHERE user_id = ?",
                Integer.class, p.id()), "sigue siendo una fila por semana");
        assertEquals(primero, segundo, "la semantica documentada no cambia");
    }

    @Test
    @DisplayName("generacion asincrona: el limite por usuario (aqui 2 por hora) tambien se aplica a operaciones nuevas")
    void limiteAsincrono() {
        var p = cuenta("USER", true, true);
        for (int i = 0; i < 2; i++) {
            var id = operaciones.request(p.id()).operation().getId();
            operaciones.claimNext();
            operaciones.execute(id);
            // Se borra el resultado para que la siguiente peticion vuelva a generar (como tras una generacion fallida).
            jdbc.update("DELETE FROM plan_generation_operations WHERE user_id = ?", p.id());
            jdbc.update("DELETE FROM planned_workout_exercises WHERE planned_workout_id IN (SELECT planned_workout_id"
                    + " FROM planned_workouts w JOIN weekly_training_plans t USING (plan_id) WHERE t.user_id = ?)", p.id());
            jdbc.update("DELETE FROM planned_workouts WHERE plan_id IN (SELECT plan_id FROM weekly_training_plans"
                    + " WHERE user_id = ?)", p.id());
            jdbc.update("DELETE FROM weekly_training_plans WHERE user_id = ?", p.id());
        }
        assertThrows(DemasiadasGeneracionesException.class, () -> operaciones.request(p.id()));
    }
}
