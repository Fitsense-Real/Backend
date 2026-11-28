package main.web.services.fitsense.analytics;

import com.fasterxml.jackson.databind.ObjectMapper;
import main.web.services.fitsense.RelojPruebas;
import main.web.services.fitsense.analytics.interfaces.acl.AnalyticsContextFacade;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * C4 (P1-09, PROPUESTA pendiente de validacion metodologica) · cada recalculo de una medicion semanal deja constancia en
 * {@code weekly_metric_recalculations} (V67), en la misma transaccion. {@code calculated_at} sigue siendo la fecha del
 * primer calculo y la formula no cambia: la medicion recalculada es la de siempre.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.adaptation.progression.enabled=false"
})
@DisplayName("C4 · historial de recalculos: primer calculo intacto, antes y despues, campos, motivo y actor")
class HistorialDeRecalculosIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @MockitoBean main.web.services.fitsense.planning.infrastructure.scheduling.OverdueWorkoutScheduler vencidas;

    @Autowired JdbcTemplate jdbc;
    @Autowired UserProfileCommandService perfiles;
    @Autowired PlanningContextFacade planning;
    @Autowired AnalyticsContextFacade analitica;

    static final LocalDate S1 = RelojPruebas.SEMANA_REFERENCIA;
    static final ObjectMapper JSON = new ObjectMapper();

    long conPlan() {
        String email = "c4-" + UUID.randomUUID() + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, timezone, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Sintetico', 'America/Lima', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1990, 3, 10), BiologicalSex.FEMALE,
                new BigDecimal("164"), new BigDecimal("63"), null, FitnessLevel.BEGINNER, GoalType.GENERAL_FITNESS,
                "Sintetico", TrainingLocation.HOME, (short) 3, List.of((short) 1, (short) 3, (short) 5),
                (short) 45, List.of("body weight"), List.of(), null, true, true, true, false, false));
        planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        return u;
    }

    List<Map<String, Object>> historial(long u) {
        return jdbc.queryForList("SELECT * FROM weekly_metric_recalculations WHERE user_id = ? ORDER BY recalculation_id",
                u);
    }

    OffsetDateTime calculadoEn(long u) {
        return jdbc.queryForObject("SELECT calculated_at FROM weekly_user_metrics WHERE user_id = ? AND week_start_date = ?",
                OffsetDateTime.class, u, S1);
    }

    @Test
    @DisplayName("el primer calculo no deja historial; un recalculo igual deja una fila sin campos cambiados")
    void recalculoSinCambios() throws Exception {
        long u = conPlan();
        analitica.calculateWeek(u, S1);
        assertTrue(historial(u).isEmpty(), "el primer calculo es la propia fila");
        var primero = calculadoEn(u);

        analitica.calculateWeek(u, S1);
        var filas = historial(u);
        assertEquals(1, filas.size());
        var f = filas.get(0);
        assertEquals("CIERRE_REPETIDO", f.get("reason"));
        assertEquals("PROCESO:CIERRE_SEMANAL", f.get("actor"));
        assertEquals("SESIONES_REGISTRADAS", f.get("data_source"));
        assertEquals(f.get("previous_calculation_version"), f.get("calculation_version"));
        var campos = List.of((Object[]) ((java.sql.Array) f.get("changed_fields")).getArray());
        assertEquals(List.of(), campos, "un recalculo con los mismos datos no cambia nada (tampoco risk_factors en jsonb)");
        assertEquals(JSON.readTree(f.get("previous_values").toString()), JSON.readTree(f.get("recalculated_values").toString()));
        assertEquals(primero, calculadoEn(u), "calculated_at sigue siendo el del primer calculo");
        assertEquals(primero.toInstant(), ((java.sql.Timestamp) f.get("first_calculated_at")).toInstant());
    }

    @Test
    @DisplayName("si el recalculo cambia la medicion: valor anterior, valor recalculado y el campo que cambio")
    void recalculoConCambios() throws Exception {
        long u = conPlan();
        analitica.calculateWeek(u, S1);
        var antes = jdbc.queryForObject("SELECT skipped_workouts FROM weekly_user_metrics WHERE user_id = ? AND"
                + " week_start_date = ?", Integer.class, u, S1);
        // Una sesion omitida despues del primer calculo (lo que haria cambiar un cierre repetido).
        jdbc.update("UPDATE planned_workouts SET status = 'SKIPPED', skip_reason = 'LACK_OF_TIME' WHERE planned_workout_id ="
                + " (SELECT w.planned_workout_id FROM planned_workouts w JOIN weekly_training_plans p USING (plan_id)"
                + " WHERE p.user_id = ? ORDER BY w.scheduled_date LIMIT 1)", u);
        analitica.calculateWeek(u, S1);

        var f = historial(u).get(0);
        var campos = List.of((Object[]) ((java.sql.Array) f.get("changed_fields")).getArray());
        assertTrue(campos.contains("skipped_workouts"), campos.toString());
        assertEquals(antes, JSON.readTree(f.get("previous_values").toString()).get("skipped_workouts").asInt());
        int ahora = jdbc.queryForObject("SELECT skipped_workouts FROM weekly_user_metrics WHERE user_id = ? AND"
                + " week_start_date = ?", Integer.class, u, S1);
        assertEquals(ahora, JSON.readTree(f.get("recalculated_values").toString()).get("skipped_workouts").asInt(),
                "lo registrado como recalculado es exactamente lo que quedo en la medicion");
        assertNotEquals(antes, ahora);
    }

    @Test
    @DisplayName("solo insercion: un recalculo registrado no se puede modificar")
    void soloInsercion() {
        long u = conPlan();
        analitica.calculateWeek(u, S1);
        analitica.calculateWeek(u, S1);
        var e = assertThrows(org.springframework.dao.DataAccessException.class, () -> jdbc.update(
                "UPDATE weekly_metric_recalculations SET reason = 'RECALCULO_MANUAL' WHERE user_id = ?", u));
        assertTrue(e.getMessage().contains("solo insercion"), e.getMessage());
    }
}
