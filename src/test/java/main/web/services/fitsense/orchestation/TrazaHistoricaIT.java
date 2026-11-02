package main.web.services.fitsense.orchestation;

import main.web.services.fitsense.planning.application.internal.commandservices.PlanningPolicySelector;
import main.web.services.fitsense.planning.domain.model.valueobjects.AdjustmentType;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanningPolicy;
import main.web.services.fitsense.planning.infrastructure.generation.ai.ReplicateSessionFixGenerator;
import main.web.services.fitsense.planning.interfaces.acl.PlanningContextFacade;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.*;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import main.web.services.fitsense.shared.infrastructure.json.JsonSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Politica por defecto (PLAN-HIST-1) con una orden ACORTAR a 30 minutos sobre un perfil de 60: la traza registra la
 * regla de minutos que aplica el validador historico ({@code round(60 x 1,15) = 69}) y distingue los minutos.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
@DisplayName("PLAN-HIST-1 con ACORTAR: la traza registra la regla historica aplicada y los dos minutos")
class TrazaHistoricaIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @MockitoBean ReplicateSessionFixGenerator ia;
    @Autowired PlanningContextFacade planning;
    @Autowired UserProfileCommandService perfiles;
    @Autowired PlanningPolicySelector selector;
    @Autowired JdbcTemplate jdbc;

    static final LocalDate SEMANA = LocalDate.of(2026, 11, 2);

    @Test
    @SuppressWarnings("unchecked")
    void historicaConAcortar() {
        when(ia.isEnabled()).thenReturn(false);
        assertSame(PlanningPolicy.HISTORICA, selector.activa());
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES ('traza-hist@pruebas.invalid', 'x', 'Hist', now(), now())");
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = 'traza-hist@pruebas.invalid'", Long.class);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1990, 6, 15), BiologicalSex.FEMALE,
                new BigDecimal("168"), new BigDecimal("68"), null, FitnessLevel.INTERMEDIATE, GoalType.GAIN_MUSCLE,
                "historica", TrainingLocation.GYM, (short) 3, List.of((short) 1, (short) 3, (short) 5), (short) 60,
                PoliticaV4CCicloIT.GIMNASIO, List.of(), "Ninguna", true, true, true, true, true));
        var acortar = new PlanAdjustment(List.of(AdjustmentType.REDUCE_DURATION), null, null, null, 0.0, 0.0, null,
                null, 30, "sesiones mas cortas", "LACK_OF_TIME", null);
        long plan = planning.generateWeeklyPlan(u, SEMANA, acortar, null, null).orElseThrow();
        var p = jdbc.queryForMap("SELECT planning_policy, planning_policy_trace::text t FROM weekly_training_plans"
                + " WHERE plan_id = ?", plan);
        assertEquals("PLAN-HIST-1", p.get("planning_policy"));
        var t = new JsonSupport().read(String.valueOf(p.get("t")), Map.class);
        assertEquals(60, t.get("minutos_perfil"));
        assertEquals(30, t.get("minutos_efectivos"));
        assertEquals(30, t.get("minutos_disponibles"));
        var params = (Map<String, Object>) t.get("parametros");
        assertEquals("HISTORICO", params.get("v4"));
        var aplicado = (Map<String, Object>) params.get("limite_v4_aplicado");
        assertEquals("V4_MINUTOS", aplicado.get("regla"));
        assertEquals(69, aplicado.get("minutos_declarados"), "round(60 x 1,15), el techo del validador historico");
        assertEquals(60, aplicado.get("minutos_base"));
        assertFalse(aplicado.containsKey("segundos"));
        assertEquals(Map.of("minutos_declarados", 35), params.get("limite_v4"), "informativo: round(30 x 1,15)");
        for (var s : (List<Map<String, Object>>) t.get("sesiones"))
            assertEquals(true, s.get("dentro_limite_v4_aplicado"), s.toString());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ? AND"
                + " week_start_date = ? AND status = 'ACTIVE'", Integer.class, u, SEMANA));
    }
}
