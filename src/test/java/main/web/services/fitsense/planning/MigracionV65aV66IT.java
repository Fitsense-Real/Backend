package main.web.services.fitsense.planning;

import main.web.services.fitsense.planning.application.internal.generation.PlanGenerationOperationService;
import main.web.services.fitsense.planning.application.internal.generation.SeguimientoOperacionesIa;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * V59-15 · V65 -> V66 CON DATOS DE LA VERSION ANTERIOR, como en un despliegue: la base se migra hasta V65, se cargan
 * operaciones de la generacion inicial asincrona en todos sus estados (una PROCESSING huerfana con su prediccion ENVIADA
 * en el libro, una PENDING y una FAILED) y despues arranca la aplicacion, cuyo Flyway aplica V66.
 * <p>
 * Comprueba que V66 no reescribe filas, que sus restricciones se validan sobre los datos existentes, que la generacion
 * asincrona sigue recuperando y reclamando SUS operaciones como antes, y que las nuevas no se mezclan con ellas.
 */
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.plan-generation.worker.enabled=false",
        "fitsense.ai-operations.recovery.enabled=false"
})
@DisplayName("V59-15 · V65 -> V66 con operaciones y predicciones de la version anterior")
class MigracionV65aV66IT {

    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");
    static final UUID HUERFANA = UUID.randomUUID();
    static final UUID PENDIENTE = UUID.randomUUID();
    static final UUID FALLIDA = UUID.randomUUID();
    static long usuarioHuerfana;
    static long usuarioPendiente;

    static {
        postgres.start();
        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .placeholderReplacement(false)
                .target("65")
                .load()
                .migrate();
        var jdbc = new JdbcTemplate(new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(),
                postgres.getPassword()));
        usuarioHuerfana = usuario(jdbc, "v65-huerfana");
        usuarioPendiente = usuario(jdbc, "v65-pendiente");
        long usuarioFallido = usuario(jdbc, "v65-fallida");
        // Esquema V65: kind FIRST_PLAN implicito, sin operation_key ni measured_week_start.
        jdbc.update("INSERT INTO plan_generation_operations (operation_id, user_id, status, week_start_date, executions,"
                + " created_at, started_at, updated_at, lease_owner, lease_expires_at, first_started_at) VALUES (?, ?,"
                + " 'PROCESSING', current_date, 1, now() - interval '5 minutes', now() - interval '5 minutes',"
                + " now() - interval '5 minutes', 'proceso-anterior', now() - interval '3 minutes',"
                + " now() - interval '5 minutes')", HUERFANA, usuarioHuerfana);
        jdbc.update("INSERT INTO plan_generation_predictions (operation_id, user_id, attempt, request_hash, status,"
                + " replicate_prediction_id, requested_at, sent_at) VALUES (?, ?, 1, ?, 'ENVIADA', 'previo1', now(),"
                + " now())", HUERFANA, usuarioHuerfana, "a".repeat(64));
        jdbc.update("INSERT INTO plan_generation_operations (operation_id, user_id, status, week_start_date, executions,"
                + " created_at, updated_at) VALUES (?, ?, 'PENDING', current_date, 0, now(), now())", PENDIENTE,
                usuarioPendiente);
        jdbc.update("INSERT INTO plan_generation_operations (operation_id, user_id, status, week_start_date, reason_code,"
                + " retryable, executions, created_at, started_at, finished_at, updated_at) VALUES (?, ?, 'FAILED',"
                + " current_date, 'ERROR_TECNICO', TRUE, 1, now() - interval '1 day', now() - interval '1 day',"
                + " now() - interval '1 day', now() - interval '1 day')", FALLIDA, usuarioFallido);
    }

    private static long usuario(JdbcTemplate jdbc, String nombre) {
        String email = nombre + "-" + UUID.randomUUID() + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'V65', now(), now())", email);
        return jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
    }

    @DynamicPropertySource
    static void base(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired PlanGenerationOperationService asincrona;
    @Autowired SeguimientoOperacionesIa seguimiento;

    private String estado(UUID id) {
        return jdbc.queryForObject("SELECT status FROM plan_generation_operations WHERE operation_id = ?",
                String.class, id);
    }

    @Test
    @DisplayName("V66 se aplica sobre datos V65 sin reescribirlos; sus restricciones quedan validadas; la asincrona"
            + " recupera y reclama sus operaciones como antes; los tipos nuevos no se mezclan")
    void actualizacionConDatos() {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE success AND version = '66'",
                Integer.class)).as("la aplicacion aplico V66 al arrancar").isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE NOT success",
                Integer.class)).isZero();
        // Filas intactas: tipo FIRST_PLAN, sin clave logica; la prediccion del libro sigue ENVIADA con su id.
        assertThat(jdbc.queryForObject("SELECT count(*) FROM plan_generation_operations WHERE kind = 'FIRST_PLAN'"
                + " AND operation_key IS NULL AND measured_week_start IS NULL", Integer.class)).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT status || ':' || replicate_prediction_id FROM plan_generation_predictions"
                + " WHERE operation_id = ?", String.class, HUERFANA)).isEqualTo("ENVIADA:previo1");
        for (var c : new String[]{"ck_pgo_kind", "ck_pgo_clave", "ck_pgo_ciclo_semana"})
            assertThat(jdbc.queryForObject("SELECT convalidated FROM pg_constraint WHERE conname = ?", Boolean.class, c))
                    .as("%s validada sobre los datos existentes", c).isTrue();

        // La asincrona: su huerfana vuelve a la cola y se reclama; la API la sigue viendo.
        assertThat(asincrona.recoverStale()).isGreaterThanOrEqualTo(1);
        assertThat(estado(HUERFANA)).isEqualTo("PENDING");
        assertThat(asincrona.latest(usuarioPendiente)).get().extracting(o -> o.getId()).isEqualTo(PENDIENTE);
        // Su unicidad sigue: una segunda PENDING de la asincrona para el mismo usuario se rechaza.
        assertThatThrownBy(() -> jdbc.update("INSERT INTO plan_generation_operations (operation_id, user_id, status,"
                + " week_start_date, executions, created_at, updated_at) VALUES (?, ?, 'PENDING', current_date, 0, now(),"
                + " now())", UUID.randomUUID(), usuarioPendiente))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        // Una operacion sincrona del mismo usuario convive con su PENDING asincrona y no aparece en la API asincrona.
        var clave = SeguimientoOperacionesIa.Clave.primerPlanSincrono(usuarioPendiente, LocalDate.now());
        assertThat(seguimiento.<String>ejecutar(clave, x -> null, g -> "ok")).isEqualTo("ok");
        assertThat(asincrona.latest(usuarioPendiente)).get().extracting(o -> o.getId()).isEqualTo(PENDIENTE);
        // Las restricciones nuevas: un tipo nuevo sin clave, o un ciclo sin semana medida, se rechazan.
        assertThatThrownBy(() -> jdbc.update("INSERT INTO plan_generation_operations (operation_id, user_id, kind, status,"
                + " week_start_date, executions, created_at, updated_at, finished_at) VALUES (?, ?, 'FIRST_PLAN_SYNC',"
                + " 'FAILED', current_date, 0, now(), now(), now())", UUID.randomUUID(), usuarioPendiente))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO plan_generation_operations (operation_id, user_id, kind, status,"
                + " week_start_date, operation_key, executions, created_at, updated_at, finished_at) VALUES (?, ?,"
                + " 'WEEKLY_CYCLE', 'FAILED', current_date, 'x', 0, now(), now(), now())", UUID.randomUUID(),
                usuarioPendiente)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        // El barrido de las nuevas no toca las de la asincrona.
        assertThat(seguimiento.cerrarVencidas()).isZero();
        assertThat(estado(FALLIDA)).isEqualTo("FAILED");
    }
}
