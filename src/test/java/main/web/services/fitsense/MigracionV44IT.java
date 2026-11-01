package main.web.services.fitsense;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * V44 (version de la politica de planificacion) sobre un esquema HISTORICO (V43) con un plan publicado. Sin Spring, en
 * PostgreSQL efimero: V44 solo anade dos columnas nullable; no toca filas, columnas ni restricciones anteriores; el
 * plan historico queda con politica NULL (= PLAN-HIST-1); la lista de versiones es cerrada; un reinicio no aplica nada.
 */
@Testcontainers
@DisplayName("V44 · version de politica sobre un esquema historico (V43) y reinicio")
class MigracionV44IT {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    private Flyway flyway(String target) {
        var c = Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration").placeholderReplacement(false);
        if (target != null) c = c.target(target);
        return c.load();
    }

    @Test
    void historicoV43MasV44() {
        var jdbc = new JdbcTemplate(new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(),
                postgres.getPassword()));
        assertTrue(postgres.getJdbcUrl().startsWith("jdbc:postgresql://localhost:"), "solo base efimera local");

        flyway("43").migrate();
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES ('h44@pruebas.invalid', 'x', 'Historico', now(), now())");
        long u = jdbc.queryForObject("SELECT user_id FROM users", Long.class);
        jdbc.update("INSERT INTO weekly_training_plans (user_id, week_number, week_start_date, week_end_date,"
                + " planned_days_count, status, generation_source, input_snapshot) VALUES (?, 1, '2026-10-26',"
                + " '2026-11-01', 1, 'COMPLETED', 'RULE_ENGINE', '{}'::jsonb)", u);
        var antes = huella(jdbc);

        var r = flyway("44").migrate();
        assertEquals(1, r.migrationsExecuted, "solo V44");
        assertEquals("44", r.targetSchemaVersion);
        assertEquals(antes, huella(jdbc), "V44 no altera columnas, restricciones ni filas anteriores");

        assertNull(jdbc.queryForObject("SELECT planning_policy FROM weekly_training_plans", String.class),
                "el plan historico no se rellena: NULL = PLAN-HIST-1");
        assertNull(jdbc.queryForObject("SELECT planning_policy_trace FROM weekly_training_plans", String.class));
        assertEquals(1, jdbc.update("UPDATE weekly_training_plans SET planning_policy = 'PLAN-C2D-0.1'"));
        assertThrows(Exception.class, () -> jdbc.update("UPDATE weekly_training_plans SET planning_policy ="
                + " 'PLAN-C2-0.1'"), "lista cerrada: una version desconocida no se guarda");
        assertThrows(Exception.class, () -> jdbc.update("UPDATE weekly_training_plans SET planning_policy ="
                + " 'PLAN-E-0.1'"), "el modelo E no tiene version");

        var otra = flyway("44");
        assertEquals(0, otra.migrate().migrationsExecuted);
        assertDoesNotThrow(otra::validate);
    }

    /** Columnas, restricciones y filas, sin las dos columnas y la restriccion nuevas de V44. */
    private static Map<String, Object> huella(JdbcTemplate jdbc) {
        var columnas = jdbc.queryForList("SELECT table_name, column_name, data_type, is_nullable, column_default"
                + " FROM information_schema.columns WHERE table_schema = 'public' AND table_name <> 'flyway_schema_history'"
                + " AND NOT (table_name = 'weekly_training_plans' AND column_name IN ('planning_policy',"
                + " 'planning_policy_trace')) ORDER BY table_name, column_name");
        var restricciones = jdbc.queryForList("SELECT conrelid::regclass::text t, conname, pg_get_constraintdef(oid) d"
                + " FROM pg_constraint WHERE connamespace = 'public'::regnamespace AND conname <>"
                + " 'ck_weekly_training_plans_planning_policy' ORDER BY 1, 2");
        var filas = new java.util.TreeMap<String, Integer>();
        for (var t : jdbc.queryForList("SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'"
                + " AND table_type = 'BASE TABLE' AND table_name <> 'flyway_schema_history'", String.class))
            filas.put(t, jdbc.queryForObject("SELECT count(*) FROM public." + t, Integer.class));
        return Map.of("columnas", columnas, "restricciones", restricciones, "filas", filas);
    }
}
