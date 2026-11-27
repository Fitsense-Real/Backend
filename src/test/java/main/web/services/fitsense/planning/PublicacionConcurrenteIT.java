package main.web.services.fitsense.planning;

import main.web.services.fitsense.RelojPruebas;
import main.web.services.fitsense.planning.domain.exceptions.PlanAlreadyExistsException;
import main.web.services.fitsense.planning.domain.exceptions.PlanCambioDuranteLaGeneracionException;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
import main.web.services.fitsense.planning.interfaces.acl.PlanningContextFacade;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.*;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * B3.4 · P1-02: si otro proceso publica una version de la semana mientras una generacion esta en marcha (la IA tarda
 * minutos), el resultado antiguo NO se publica encima en silencio. Se reproduce con las tres fases reales del servicio
 * (preparar, generar con el motor de reglas, persistir) y otra publicacion entre la primera y la tercera.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
@DisplayName("P1-02 · publicacion concurrente: no se publica encima de un plan que cambio durante la generacion")
class PublicacionConcurrenteIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired WeeklyTrainingPlanCommandService planes;
    @Autowired PlanningContextFacade planning;
    @Autowired UserProfileCommandService perfiles;
    @Autowired JdbcTemplate jdbc;

    static final LocalDate S1 = RelojPruebas.SEMANA_REFERENCIA;

    long participante() {
        String email = "concurrente-" + UUID.randomUUID() + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Sintetico', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1990, 3, 10), BiologicalSex.FEMALE,
                new BigDecimal("164"), new BigDecimal("63"), null, FitnessLevel.BEGINNER, GoalType.GENERAL_FITNESS,
                "Sintetico", TrainingLocation.HOME, (short) 3, List.of((short) 1, (short) 3, (short) 5), (short) 45,
                List.of("body weight"), List.of(), null, true, true, true, false, false));
        return u;
    }

    List<Long> activos(long u) {
        return jdbc.queryForList("SELECT plan_id FROM weekly_training_plans WHERE user_id = ? AND week_start_date = ?"
                + " AND status = 'ACTIVE'", Long.class, u, S1);
    }

    @Test
    @DisplayName("se preparo sin plan y otro proceso publico uno: no se reemplaza; queda la version del otro, sola")
    void aparecioUnPlan() {
        long u = participante();
        var preparado = planes.prepare(new GenerateWeeklyPlanCommand(u, S1, PlanAdjustment.none(), true, null, null));
        var generado = planes.generate(preparado);

        long otro = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();

        var e = assertThrows(PlanCambioDuranteLaGeneracionException.class, () -> planes.persist(preparado, generado));
        assertNull(e.esperado());
        assertEquals(otro, e.encontrado());
        assertEquals(List.of(otro), activos(u), "solo la version que publico el otro proceso");
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ?",
                Integer.class, u), "ninguna version silenciosa");
    }

    @Test
    @DisplayName("se preparo sobre la version 1 y otro proceso publico la 2: no se publica la 3 encima; historial intacto")
    void cambioLaVersion() {
        long u = participante();
        long v1 = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        var preparado = planes.prepare(new GenerateWeeklyPlanCommand(u, S1, PlanAdjustment.none(), true, null, null));
        assertEquals(v1, preparado.previousActivePlanId());
        var generado = planes.generate(preparado);

        // Otro proceso reemplaza la version 1 (como una reparacion de seguridad o una adaptacion).
        var otro = planes.prepare(new GenerateWeeklyPlanCommand(u, S1, PlanAdjustment.none(), true, null, null));
        long v2 = planes.persist(otro, planes.generate(otro)).orElseThrow().getId();

        assertThrows(PlanCambioDuranteLaGeneracionException.class, () -> planes.persist(preparado, generado));
        assertEquals(List.of(v2), activos(u));
        assertEquals(List.of("REPLACED", "ACTIVE"), jdbc.queryForList("SELECT status FROM weekly_training_plans WHERE"
                + " user_id = ? ORDER BY plan_version", String.class, u), "v1 reemplazada por v2; ninguna v3");
    }

    @Test
    @DisplayName("sin carrera, el reemplazo de siempre sigue funcionando (v1 -> v2)")
    void sinCarrera() {
        long u = participante();
        planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        var preparado = planes.prepare(new GenerateWeeklyPlanCommand(u, S1, PlanAdjustment.none(), true, null, null));
        long v2 = planes.persist(preparado, planes.generate(preparado)).orElseThrow().getId();
        assertEquals(List.of(v2), activos(u));
    }

    @Test
    @DisplayName("sin reemplazo pedido, el conflicto sigue siendo PlanAlreadyExists, como antes")
    void sinReemplazo() {
        long u = participante();
        var preparado = planes.prepare(new GenerateWeeklyPlanCommand(u, S1, PlanAdjustment.none(), false, null, null));
        var generado = planes.generate(preparado);
        planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        assertThrows(PlanAlreadyExistsException.class, () -> planes.persist(preparado, generado));
    }
}
