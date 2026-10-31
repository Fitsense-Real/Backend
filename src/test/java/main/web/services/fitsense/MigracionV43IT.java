package main.web.services.fitsense;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AUDITORIA DE V43 (orden de cierre del PR #16): compatibilidad sobre un esquema HISTORICO.
 * <p>
 * Sobre un PostgreSQL efimero y sin Spring: Flyway hasta V42, datos historicos representativos (usuarios, perfiles
 * congelados fuera del nuevo rango de P-01, un plan con sus sesiones y ejercicios, una medicion y una evidencia V32),
 * y despues V43. Se comprueba que V43 no toca nada de lo anterior (filas, columnas y restricciones de las tablas
 * existentes), que crea sus dos tablas vacias con sus restricciones, que un segundo arranque no aplica nada y que la
 * validacion de Flyway pasa. La migracion completa desde una base vacia la ejercen todas las demas IT.
 */
@Testcontainers
@DisplayName("V43 · migracion sobre un esquema historico (V42) y reinicio")
class MigracionV43IT {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    private Flyway flyway(String target) {
        var c = Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration").placeholderReplacement(false);
        if (target != null) c = c.target(target);
        return c.load();
    }

    @Test
    void historicoV42MasV43() {
        var jdbc = new JdbcTemplate(new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(),
                postgres.getPassword()));
        assertTrue(postgres.getJdbcUrl().startsWith("jdbc:postgresql://localhost:"), "solo base efimera local");

        flyway("42").migrate();
        assertEquals("42", jdbc.queryForObject("SELECT max(version::int)::text FROM flyway_schema_history"
                + " WHERE success", String.class));
        cargarHistorico(jdbc);
        var antes = huella(jdbc);

        var r = flyway(null).migrate();
        assertEquals(1, r.migrationsExecuted, "solo V43");
        assertEquals("43", r.targetSchemaVersion);
        assertEquals(antes, huella(jdbc), "V43 no altera tablas ni datos historicos");
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM user_safety_exclusions", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM safe_regeneration_attempts", Integer.class));
        assertEquals(List.of(15, 180), jdbc.queryForList("SELECT session_minutes FROM user_profiles ORDER BY user_id",
                Integer.class), "los perfiles congelados no se corrigen");

        // Restricciones de V43 sobre datos reales del historico.
        long u = jdbc.queryForObject("SELECT min(user_id) FROM users", Long.class);
        long plan = jdbc.queryForObject("SELECT min(plan_id) FROM weekly_training_plans", Long.class);
        long w = jdbc.queryForObject("SELECT min(planned_workout_id) FROM planned_workouts", Long.class);
        long ej = jdbc.queryForObject("SELECT min(exercise_id) FROM planned_workout_exercises", Long.class);
        String insertar = "INSERT INTO user_safety_exclusions (user_id, exercise_id, reason, source_plan_id,"
                + " source_planned_workout_id, event_date, recorded_by) VALUES (?, ?, 'DOLOR_IDENTIFICADO', ?, ?,"
                + " '2026-11-02', 'SESION') ON CONFLICT (user_id, exercise_id, source_planned_workout_id) DO NOTHING";
        assertEquals(1, jdbc.update(insertar, u, ej, plan, w));
        assertEquals(0, jdbc.update(insertar, u, ej, plan, w), "el mismo evento no se duplica");
        assertThrows(Exception.class, () -> jdbc.update("INSERT INTO user_safety_exclusions (user_id, exercise_id,"
                + " reason, source_plan_id, source_planned_workout_id, event_date, recorded_by) VALUES (?, ?,"
                + " 'DOLOR_IDENTIFICADO', ?, ?, '2026-11-02', 'SESION')", u, 99_999_999L, plan, w), "FK al ejercicio");
        assertThrows(Exception.class, () -> jdbc.update("UPDATE user_safety_exclusions SET status = 'REACTIVADA'"),
                "reactivar sin evidencia viola ck_use_reactivacion");
        assertThrows(Exception.class, () -> jdbc.update("INSERT INTO safe_regeneration_attempts (user_id,"
                + " weekly_metric_id, measured_plan_id, new_week_start_date, excluded_exercise_ids, outcome)"
                + " VALUES (?, (SELECT min(weekly_metric_id) FROM weekly_user_metrics), ?, '2026-11-09', '{}',"
                + " 'PUBLICADO')", u, plan), "PUBLICADO sin plan viola ck_sra_plan");

        // Reinicio con V43 ya aplicada: nada que aplicar y la validacion pasa.
        var otra = flyway(null);
        assertEquals(0, otra.migrate().migrationsExecuted);
        assertDoesNotThrow(otra::validate);
    }

    /** Tablas y columnas existentes con sus tipos, restricciones y numero de filas (excepto las nuevas de V43). */
    private static Map<String, Object> huella(JdbcTemplate jdbc) {
        var columnas = jdbc.queryForList("SELECT table_name, column_name, data_type, is_nullable, column_default"
                + " FROM information_schema.columns WHERE table_schema = 'public' AND table_name NOT IN"
                + " ('user_safety_exclusions', 'safe_regeneration_attempts', 'flyway_schema_history')"
                + " ORDER BY table_name, column_name");
        var restricciones = jdbc.queryForList("SELECT conrelid::regclass::text t, conname, pg_get_constraintdef(oid) d"
                + " FROM pg_constraint WHERE connamespace = 'public'::regnamespace AND conrelid::regclass::text NOT IN"
                + " ('user_safety_exclusions', 'safe_regeneration_attempts') ORDER BY 1, 2");
        var filas = new java.util.TreeMap<String, Integer>();
        for (var t : jdbc.queryForList("SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'"
                + " AND table_type = 'BASE TABLE' AND table_name NOT IN ('user_safety_exclusions',"
                + " 'safe_regeneration_attempts', 'flyway_schema_history')", String.class))
            filas.put(t, jdbc.queryForObject("SELECT count(*) FROM public." + t, Integer.class));
        return Map.of("columnas", columnas, "restricciones", restricciones, "filas", filas);
    }

    /** Un historico pequeno pero completo: dos participantes congelados, un plan medido y su evidencia. */
    private static void cargarHistorico(JdbcTemplate jdbc) {
        for (int i = 1; i <= 2; i++)
            jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                    + " VALUES (?, 'x', 'Historico', now(), now())", "historico" + i + "@pruebas.invalid");
        var ids = jdbc.queryForList("SELECT user_id FROM users ORDER BY user_id", Long.class);
        int[] minutos = {15, 180};
        for (int i = 0; i < 2; i++)
            jdbc.update("INSERT INTO user_profiles (user_id, birth_date, height_cm, current_weight_kg, fitness_level,"
                    + " goal_type, goal_text, training_location, days_per_week, available_days, session_minutes,"
                    + " equipment_codes, blocked_exercise_ids) VALUES (?, '1990-01-01', 170, 70, 'INTERMEDIATE',"
                    + " 'GAIN_MUSCLE', 'historico', 'GYM', 3, '{1,3,5}', ?, '{dumbbell}', '{}')", ids.get(i), minutos[i]);
        long u = ids.get(0);
        jdbc.update("INSERT INTO weekly_training_plans (user_id, week_number, week_start_date, week_end_date,"
                + " planned_days_count, status, generation_source, input_snapshot) VALUES (?, 1, '2026-10-26',"
                + " '2026-11-01', 1, 'COMPLETED', 'RULE_ENGINE', '{}'::jsonb)", u);
        long plan = jdbc.queryForObject("SELECT plan_id FROM weekly_training_plans WHERE user_id = ?", Long.class, u);
        jdbc.update("INSERT INTO planned_workouts (plan_id, scheduled_date, focus_code, workout_name,"
                + " expected_duration_minutes, display_order, status, expires_at) VALUES (?, '2026-10-26', 'FULL_BODY',"
                + " 'historica', 15, 1, 'COMPLETED', now())", plan);
        long w = jdbc.queryForObject("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ?", Long.class, plan);
        long ej = jdbc.queryForObject("SELECT min(exercise_id) FROM exercises WHERE is_active", Long.class);
        jdbc.update("INSERT INTO planned_workout_exercises (planned_workout_id, exercise_id, exercise_order,"
                + " prescription_type, planned_sets, planned_reps, rest_seconds) VALUES (?, ?, 1, 'SETS_REPS', 3, 10, 90)",
                w, ej);
        jdbc.update("INSERT INTO weekly_user_metrics (user_id, week_number, week_start_date, week_end_date,"
                + " has_active_plan, calculation_version) VALUES (?, 1, '2026-10-26', '2026-11-01', true, 'v1')", u);
    }
}
