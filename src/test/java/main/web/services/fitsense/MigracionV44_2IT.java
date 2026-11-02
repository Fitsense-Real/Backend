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
 * V44_2 (recuperacion de contrasena) sobre un esquema HISTORICO (V44) con datos. Sin Spring, en PostgreSQL efimero:
 * solo crea la tabla password_reset_tokens; no toca filas, columnas ni restricciones anteriores; un reinicio no aplica
 * nada. Mismo patron que MigracionV43IT y MigracionV44IT.
 */
@Testcontainers
@DisplayName("V44_2 · recuperacion de contrasena sobre un esquema historico (V44) y reinicio")
class MigracionV44_2IT {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    private Flyway flyway(String target) {
        return Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration").placeholderReplacement(false).target(target).load();
    }

    @Test
    void historicoV44MasV44_2() {
        var jdbc = new JdbcTemplate(new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(),
                postgres.getPassword()));
        assertTrue(postgres.getJdbcUrl().startsWith("jdbc:postgresql://localhost:"), "solo base efimera local");

        flyway("44").migrate();
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES ('h442@pruebas.invalid', 'x', 'Historico', now(), now())");
        long u = jdbc.queryForObject("SELECT user_id FROM users", Long.class);
        jdbc.update("INSERT INTO weekly_training_plans (user_id, week_number, week_start_date, week_end_date,"
                + " planned_days_count, status, generation_source, input_snapshot) VALUES (?, 1, '2026-10-26',"
                + " '2026-11-01', 1, 'COMPLETED', 'RULE_ENGINE', '{}'::jsonb)", u);
        var antes = huella(jdbc);

        var r = flyway("44.2").migrate();
        assertEquals(1, r.migrationsExecuted, "solo V44_2");
        assertEquals("44.2", r.targetSchemaVersion);
        assertEquals(antes, huella(jdbc), "V44_2 no altera columnas, restricciones ni filas anteriores");
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM password_reset_tokens", Integer.class));
        assertThrows(Exception.class, () -> jdbc.update("INSERT INTO password_reset_tokens (user_id, token_hash,"
                + " expires_at) VALUES (?, 'no-es-un-hash', now() + interval '30 minutes')", u),
                "solo se admite un SHA-256 en hexadecimal");

        var otra = flyway("44.2");
        assertEquals(0, otra.migrate().migrationsExecuted, "un reinicio no aplica nada");
        assertDoesNotThrow(otra::validate);
    }

    private static Map<String, Object> huella(JdbcTemplate jdbc) {
        var columnas = jdbc.queryForList("SELECT table_name, column_name, data_type, is_nullable, column_default"
                + " FROM information_schema.columns WHERE table_schema = 'public' AND table_name <> 'flyway_schema_history'"
                + " AND table_name <> 'password_reset_tokens'"
                + " ORDER BY table_name, column_name");
        var restricciones = jdbc.queryForList("SELECT conrelid::regclass::text t, conname, pg_get_constraintdef(oid) d"
                + " FROM pg_constraint WHERE connamespace = 'public'::regnamespace AND conrelid::regclass::text"
                + " <> 'password_reset_tokens' ORDER BY 1, 2");
        var filas = new java.util.TreeMap<String, Integer>();
        for (var t : jdbc.queryForList("SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'"
                + " AND table_type = 'BASE TABLE' AND table_name NOT IN ('flyway_schema_history', 'password_reset_tokens')", String.class))
            filas.put(t, jdbc.queryForObject("SELECT count(*) FROM public." + t, Integer.class));
        return Map.of("columnas", columnas, "restricciones", restricciones, "filas", filas);
    }
}
