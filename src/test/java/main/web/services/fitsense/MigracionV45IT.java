package main.web.services.fitsense;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * V45 (versiones con elegibilidad funcional) sobre un esquema V44 con planes historico y C2-D. Solo amplia la lista
 * cerrada: los valores guardados no cambian; las versiones EF se pueden guardar; una desconocida sigue rechazada.
 */
@Testcontainers
@DisplayName("V45 · lista de versiones con EF sobre un esquema V44")
class MigracionV45IT {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    private Flyway flyway(String target) {
        var c = Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration").placeholderReplacement(false);
        if (target != null) c = c.target(target);
        return c.load();
    }

    @Test
    void v44MasV45() {
        var jdbc = new JdbcTemplate(new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(),
                postgres.getPassword()));
        assertTrue(postgres.getJdbcUrl().startsWith("jdbc:postgresql://localhost:"), "solo base efimera local");
        flyway("44").migrate();
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES ('h45@pruebas.invalid', 'x', 'Historico', now(), now())");
        long u = jdbc.queryForObject("SELECT user_id FROM users", Long.class);
        for (int semana = 1; semana <= 2; semana++)
            jdbc.update("INSERT INTO weekly_training_plans (user_id, week_number, week_start_date, week_end_date,"
                    + " planned_days_count, status, generation_source, input_snapshot, planning_policy) VALUES (?, ?,"
                    + " DATE '2026-10-26' + 7 * ?, DATE '2026-11-01' + 7 * ?, 1, 'COMPLETED', 'RULE_ENGINE',"
                    + " '{}'::jsonb, ?)", u, semana, semana, semana, semana == 1 ? null : "PLAN-C2D-0.1");
        var antes = jdbc.queryForList("SELECT plan_id, planning_policy FROM weekly_training_plans ORDER BY 1");

        var r = flyway(null).migrate();
        assertEquals(1, r.migrationsExecuted, "solo V45");
        assertEquals("45", r.targetSchemaVersion);
        assertEquals(antes, jdbc.queryForList("SELECT plan_id, planning_policy FROM weekly_training_plans ORDER BY 1"),
                "V45 no cambia ningun valor guardado");
        for (var v : new String[]{"PLAN-EF-0.1", "PLAN-C2D-EF-0.1", "PLAN-C2D-V4-EF-0.1", "PLAN-HIST-1"})
            assertEquals(1, jdbc.update("UPDATE weekly_training_plans SET planning_policy = ? WHERE week_number = 2", v));
        assertThrows(Exception.class, () -> jdbc.update("UPDATE weekly_training_plans SET planning_policy ="
                + " 'PLAN-EF-0.2'"), "lista cerrada");
        assertThrows(Exception.class, () -> jdbc.update("UPDATE weekly_training_plans SET planning_policy ="
                + " 'PLAN-C2-0.1'"), "PLAN-C2-0.1 no se reutiliza");
        assertEquals(0, flyway(null).migrate().migrationsExecuted);
    }
}
