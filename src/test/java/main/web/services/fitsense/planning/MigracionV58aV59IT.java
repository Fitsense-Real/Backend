package main.web.services.fitsense.planning;

import main.web.services.fitsense.planning.application.internal.generation.PlanGenerationOperationService;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.*;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * V58 -> V59 · ACTUALIZACION con datos, como en un despliegue real: la base se migra primero hasta V58 (el esquema de
 * #71) desde una PostgreSQL vacia, se cargan operaciones en todos los estados (incluida una PROCESSING que dejo un
 * proceso muerto, sin lease porque V58 no lo tenia) y despues arranca la aplicacion, cuyo Flyway aplica V59.
 * <p>
 * Comprueba que V59 se aplica sobre datos existentes sin fallar, que no reescribe filas, que ck_pgo_lease queda NOT
 * VALID (las filas antiguas no la cumplen) y que la PROCESSING heredada se recupera: un lease nulo cuenta como vencido.
 */
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.plan-generation.worker.enabled=false"
})
@DisplayName("V58 -> V59 · actualizacion con operaciones existentes")
class MigracionV58aV59IT {

    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");
    static final UUID EN_CURSO = UUID.randomUUID();
    static final UUID PENDIENTE = UUID.randomUUID();
    static final UUID FALLIDA = UUID.randomUUID();
    static long usuarioEnCurso;
    static long usuarioPendiente;

    static {
        postgres.start();
        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .placeholderReplacement(false)
                .target("58")
                .load()
                .migrate();
        var jdbc = new JdbcTemplate(new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(),
                postgres.getPassword()));
        usuarioEnCurso = usuario(jdbc, "v58-en-curso");
        usuarioPendiente = usuario(jdbc, "v58-pendiente");
        long usuarioFallido = usuario(jdbc, "v58-fallida");
        // Esquema V58: sin lease_owner, lease_expires_at ni first_started_at.
        jdbc.update("INSERT INTO plan_generation_operations (operation_id, user_id, status, week_start_date, executions,"
                + " created_at, started_at, updated_at) VALUES (?, ?, 'PROCESSING', current_date, 1,"
                + " now() - interval '12 minutes', now() - interval '10 minutes', now() - interval '10 minutes')",
                EN_CURSO, usuarioEnCurso);
        jdbc.update("INSERT INTO plan_generation_operations (operation_id, user_id, status, week_start_date, executions,"
                + " created_at, updated_at) VALUES (?, ?, 'PENDING', current_date, 0, now() - interval '1 minute',"
                + " now() - interval '1 minute')", PENDIENTE, usuarioPendiente);
        jdbc.update("INSERT INTO plan_generation_operations (operation_id, user_id, status, week_start_date, reason_code,"
                + " retryable, executions, created_at, started_at, finished_at, updated_at) VALUES (?, ?, 'FAILED',"
                + " current_date, 'ERROR_TECNICO', TRUE, 1, now() - interval '1 day', now() - interval '1 day',"
                + " now() - interval '1 day', now() - interval '1 day')", FALLIDA, usuarioFallido);
    }

    private static long usuario(JdbcTemplate jdbc, String nombre) {
        String email = nombre + "-" + UUID.randomUUID() + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'V58', now(), now())", email);
        return jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
    }

    @DynamicPropertySource
    static void base(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired PlanGenerationOperationService service;
    @Autowired UserProfileCommandService perfiles;

    private String estado(UUID id) {
        return jdbc.queryForObject("SELECT status FROM plan_generation_operations WHERE operation_id = ?",
                String.class, id);
    }

    @Test
    @DisplayName("V59 se aplica sobre datos V58 sin reescribirlos; la PROCESSING huerfana sin lease se recupera y se"
            + " completa; ck_pgo_lease se puede validar despues")
    void actualizacionConDatos() {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE success AND version = '59'",
                Integer.class)).as("la aplicacion aplico V59 al arrancar").isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE NOT success",
                Integer.class)).isZero();
        // V59 no toca filas: la PROCESSING heredada sigue sin lease ni first_started_at.
        var heredada = jdbc.queryForMap("SELECT status, lease_owner, lease_expires_at, first_started_at, executions"
                + " FROM plan_generation_operations WHERE operation_id = ?", EN_CURSO);
        assertThat(heredada.get("status")).isEqualTo("PROCESSING");
        assertThat(heredada.get("lease_owner")).isNull();
        assertThat(heredada.get("lease_expires_at")).isNull();
        assertThat(heredada.get("first_started_at")).isNull();
        assertThat(jdbc.queryForObject("SELECT convalidated FROM pg_constraint WHERE conname = 'ck_pgo_lease'",
                Boolean.class)).as("NOT VALID: las filas antiguas no la cumplen").isFalse();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM plan_generation_predictions", Integer.class)).isZero();
        // Una escritura nueva SI la cumple: PROCESSING sin lease se rechaza.
        assertThatThrownBy(() -> jdbc.update("UPDATE plan_generation_operations SET status = 'PROCESSING', started_at ="
                + " now() WHERE operation_id = ?", PENDIENTE))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);

        // El barrido de V59 trata el lease nulo como vencido: la heredada vuelve a la cola (1 ejecucion < 3).
        service.recoverStale();
        assertThat(estado(EN_CURSO)).isEqualTo("PENDING");
        assertThat(estado(PENDIENTE)).isEqualTo("PENDING");
        assertThat(estado(FALLIDA)).as("lo terminado no se toca").isEqualTo("FAILED");
        jdbc.execute("ALTER TABLE plan_generation_operations VALIDATE CONSTRAINT ck_pgo_lease");

        // Se reclama (la mas antigua: la heredada) con lease y primera ejecucion, y se completa con su plan.
        perfiles.handle(new CreateUserProfileCommand(usuarioEnCurso, LocalDate.of(1990, 3, 10), BiologicalSex.FEMALE,
                new BigDecimal("164"), new BigDecimal("63"), null, FitnessLevel.INTERMEDIATE, GoalType.GENERAL_FITNESS,
                "Actualizacion V58 a V59", TrainingLocation.GYM, (short) 2, List.of((short) 2, (short) 4), (short) 55,
                List.of("body weight", "dumbbell", "barbell", "bench", "cable", "leverage machine", "band",
                        "pull-up bar"), List.of(), "Ninguna", true, true, true, false, false));
        assertThat(service.claimNext()).contains(EN_CURSO);
        var reclamada = jdbc.queryForMap("SELECT lease_owner, lease_expires_at, first_started_at, executions"
                + " FROM plan_generation_operations WHERE operation_id = ?", EN_CURSO);
        assertThat(reclamada.get("lease_owner")).isEqualTo(service.owner());
        assertThat(reclamada.get("lease_expires_at")).isNotNull();
        assertThat(reclamada.get("first_started_at")).isNotNull();
        assertThat(((Number) reclamada.get("executions")).intValue()).isEqualTo(2);
        service.execute(EN_CURSO);
        assertThat(estado(EN_CURSO)).isEqualTo("COMPLETED");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ?", Integer.class,
                usuarioEnCurso)).isEqualTo(1);
    }
}
