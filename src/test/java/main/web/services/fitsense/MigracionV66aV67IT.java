package main.web.services.fitsense;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * C4 · V66 -> V67 con datos de la version anterior, como en un despliegue: una medicion semanal escrita con el esquema
 * V66 sigue igual, columna a columna, despues de V67; V67 solo crea su tabla, su funcion y su disparador.
 */
@DisplayName("C4 · V66 -> V67: ninguna medicion existente cambia; la tabla nueva empieza vacia")
class MigracionV66aV67IT {

    @Test
    @DisplayName("una medicion de V66 queda identica tras V67, y V67 acepta recalculos que la referencian")
    void compatible() {
        try (var postgres = new PostgreSQLContainer("postgres:16-alpine")) {
            postgres.start();
            var flyway = Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(),
                    postgres.getPassword()).locations("classpath:db/migration").placeholderReplacement(false);
            flyway.target("66").load().migrate();
            var jdbc = new JdbcTemplate(new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(),
                    postgres.getPassword()));
            jdbc.update("INSERT INTO users (email, password_hash, first_name, timezone) VALUES"
                    + " ('v66-c4@pruebas.invalid', 'x', 'Sintetico', 'America/Lima')");
            long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = 'v66-c4@pruebas.invalid'", Long.class);
            jdbc.update("INSERT INTO weekly_user_metrics (user_id, week_number, week_start_date, week_end_date,"
                    + " has_active_plan, calculation_version, weighted_adherence_pct) VALUES (?, 1, DATE '2026-11-02',"
                    + " DATE '2026-11-08', TRUE, 'v-anterior', 61.25)", u);
            Map<String, Object> antes = jdbc.queryForMap("SELECT * FROM weekly_user_metrics WHERE user_id = ?", u);

            flyway.target("67").load().migrate();
            assertEquals(antes, jdbc.queryForMap("SELECT * FROM weekly_user_metrics WHERE user_id = ?", u),
                    "V67 no reescribe ninguna medicion");
            assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM weekly_metric_recalculations", Integer.class));
            assertEquals("67", jdbc.queryForObject("SELECT version FROM flyway_schema_history WHERE success ORDER BY"
                    + " installed_rank DESC LIMIT 1", String.class));
            jdbc.update("INSERT INTO weekly_metric_recalculations (weekly_metric_id, user_id, week_start_date,"
                    + " first_calculated_at, reason, actor, data_source, previous_calculation_version, calculation_version,"
                    + " previous_values, recalculated_values, changed_fields) SELECT weekly_metric_id, user_id,"
                    + " week_start_date, calculated_at, 'CIERRE_REPETIDO', 'PROCESO:CIERRE_SEMANAL', 'SESIONES_REGISTRADAS',"
                    + " 'v-anterior', 'v-anterior', '{}', '{}', '{}' FROM weekly_user_metrics WHERE user_id = ?", u);
            assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> jdbc.update(
                    "INSERT INTO weekly_metric_recalculations (weekly_metric_id, user_id, week_start_date,"
                            + " first_calculated_at, reason, actor, data_source, previous_calculation_version,"
                            + " calculation_version, previous_values, recalculated_values, changed_fields) SELECT"
                            + " weekly_metric_id, user_id, week_start_date, calculated_at, 'OTRO', 'cualquiera', 'X', 'a',"
                            + " 'a', '{}', '{}', '{}' FROM weekly_user_metrics WHERE user_id = ?", u),
                    "motivo, actor y fuente acotados");
        }
    }
}
