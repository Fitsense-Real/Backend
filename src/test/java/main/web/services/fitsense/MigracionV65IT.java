package main.web.services.fitsense;

import main.web.services.fitsense.planning.application.internal.commandservices.PainMemoryService;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * V65 (O20, identificacion posterior del dolor) sobre una base EXISTENTE y sobre una NUEVA.
 * <p>
 * Existente: la base se migra hasta V64 desde una PostgreSQL vacia, se cargan exclusiones por dolor en todos sus estados
 * (identificada, de sesion sin identificar y reactivada, con su constancia) y despues arranca la aplicacion, cuyo
 * Flyway aplica V65. Comprueba que no se pierde ni cambia ningun dato, que las columnas nuevas quedan NULL, que la
 * restriccion nueva es valida sobre las filas existentes y que la aplicacion las lee como antes.
 * <p>
 * Nueva: una segunda PostgreSQL vacia migrada de una vez hasta la ultima version.
 */
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.plan-generation.worker.enabled=false"
})
@DisplayName("V65 · identificacion posterior del dolor sobre una base existente y una nueva, sin perder datos")
class MigracionV65IT {

    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");
    static final String COLUMNAS = "exclusion_id, user_id, exercise_id, reason, source_plan_id,"
            + " source_planned_workout_id, source_planned_exercise_id, event_date, recorded_by, recorded_at, status,"
            + " reactivated_at, reactivation_reason, reactivation_evidence";
    static long usuario;
    static long ejercicioIdentificado, ejercicioDeSesion, ejercicioReactivado;
    static List<Map<String, Object>> antes;

    static {
        postgres.start();
        migrar(postgres, "64");
        var jdbc = new JdbcTemplate(new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(),
                postgres.getPassword()));
        String email = "v64-dolor-" + UUID.randomUUID() + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'V64', now(), now())", email);
        usuario = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        jdbc.update("INSERT INTO weekly_training_plans (user_id, week_number, week_start_date, week_end_date,"
                + " planned_days_count, status, generation_source, input_snapshot) VALUES (?, 1, '2026-10-26',"
                + " '2026-11-01', 1, 'COMPLETED', 'RULE_ENGINE', '{}'::jsonb)", usuario);
        long plan = jdbc.queryForObject("SELECT plan_id FROM weekly_training_plans WHERE user_id = ?", Long.class,
                usuario);
        jdbc.update("INSERT INTO planned_workouts (plan_id, scheduled_date, focus_code, workout_name,"
                + " expected_duration_minutes, display_order, status, expires_at) VALUES (?, '2026-10-26', 'FULL_BODY',"
                + " 'historica', 15, 1, 'SKIPPED', now())", plan);
        long w = jdbc.queryForObject("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ?", Long.class,
                plan);
        var ejercicios = jdbc.queryForList("SELECT exercise_id FROM exercises WHERE is_active ORDER BY exercise_id"
                + " LIMIT 3", Long.class);
        ejercicioIdentificado = ejercicios.get(0);
        ejercicioDeSesion = ejercicios.get(1);
        ejercicioReactivado = ejercicios.get(2);
        int orden = 1;
        for (var e : ejercicios)
            jdbc.update("INSERT INTO planned_workout_exercises (planned_workout_id, exercise_id, exercise_order,"
                    + " prescription_type, planned_sets, planned_reps, rest_seconds) VALUES (?, ?, ?, 'SETS_REPS', 3,"
                    + " 10, 90)", w, e, orden++);
        var pe = jdbc.queryForList("SELECT planned_exercise_id FROM planned_workout_exercises WHERE planned_workout_id"
                + " = ? ORDER BY exercise_order", Long.class, w);
        jdbc.update("INSERT INTO user_safety_exclusions (user_id, exercise_id, reason, source_plan_id,"
                + " source_planned_workout_id, source_planned_exercise_id, event_date, recorded_by) VALUES (?, ?,"
                + " 'DOLOR_IDENTIFICADO', ?, ?, ?, '2026-10-26', 'CICLO_SEMANAL')", usuario, ejercicioIdentificado, plan,
                w, pe.get(0));
        jdbc.update("INSERT INTO user_safety_exclusions (user_id, exercise_id, reason, source_plan_id,"
                + " source_planned_workout_id, source_planned_exercise_id, event_date, recorded_by) VALUES (?, ?,"
                + " 'DOLOR_SESION_SIN_IDENTIFICAR', ?, ?, ?, '2026-10-26', 'OMISION')", usuario, ejercicioDeSesion,
                plan, w, pe.get(1));
        jdbc.update("INSERT INTO user_safety_exclusions (user_id, exercise_id, reason, source_plan_id,"
                + " source_planned_workout_id, source_planned_exercise_id, event_date, recorded_by, status,"
                + " reactivated_at, reactivation_reason, reactivation_evidence) VALUES (?, ?,"
                + " 'DOLOR_SESION_SIN_IDENTIFICAR', ?, ?, ?, '2026-10-26', 'OMISION', 'REACTIVADA', now(),"
                + " 'ya no duele', 'CONFIRMACION_EXPLICITA_DEL_USUARIO prueba V64')", usuario, ejercicioReactivado, plan,
                w, pe.get(2));
        antes = jdbc.queryForList("SELECT " + COLUMNAS + " FROM user_safety_exclusions ORDER BY exclusion_id");
    }

    static void migrar(PostgreSQLContainer pg, String target) {
        var f = Flyway.configure()
                .dataSource(pg.getJdbcUrl(), pg.getUsername(), pg.getPassword())
                .locations("classpath:db/migration")
                .placeholderReplacement(false);
        if (target != null) f.target(target);
        f.load().migrate();
    }

    @DynamicPropertySource
    static void base(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired PainMemoryService dolor;

    @Test
    @DisplayName("Base existente: V65 se aplica sin perder ni cambiar datos; columnas nuevas NULL; CHECK valido")
    void baseExistente() {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE success AND version = '65'",
                Integer.class)).as("la aplicacion aplico V65 al arrancar").isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE NOT success", Integer.class))
                .isZero();
        assertThat(antes).hasSize(3);
        assertThat(jdbc.queryForList("SELECT " + COLUMNAS + " FROM user_safety_exclusions ORDER BY exclusion_id"))
                .as("ningun dato perdido ni cambiado").isEqualTo(antes);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_safety_exclusions WHERE identified_at IS NOT NULL"
                + " OR identification_evidence IS NOT NULL", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT convalidated FROM pg_constraint WHERE conname = 'ck_use_identificacion'",
                Boolean.class)).as("valida sobre las filas existentes").isTrue();

        // La aplicacion las lee como antes: la identificada, confirmada; la de sesion, sin confirmar; la reactivada, no.
        var efectivas = dolor.exclusionesEfectivas(usuario).stream()
                .filter(x -> x.origen() == PainMemoryService.Origen.DIRECTA)
                .collect(java.util.stream.Collectors.toMap(PainMemoryService.ExclusionEfectiva::exerciseId, x -> x));
        assertThat(efectivas).containsOnlyKeys(ejercicioIdentificado, ejercicioDeSesion);
        assertThat(efectivas.get(ejercicioIdentificado).confirmada()).isTrue();
        assertThat(efectivas.get(ejercicioDeSesion).confirmada()).isFalse();

        // La restriccion nueva: solo se identifica despues lo que llego sin identificar, y con constancia.
        assertThatThrownBy(() -> jdbc.update("UPDATE user_safety_exclusions SET identified_at = now(),"
                + " identification_evidence = 'x' WHERE exercise_id = ?", ejercicioIdentificado))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE user_safety_exclusions SET identified_at = now()"
                + " WHERE exercise_id = ?", ejercicioDeSesion))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);

        // Y el flujo nuevo funciona sobre la fila heredada, conservando el motivo original.
        dolor.identificar(usuario, ejercicioDeSesion, null, true);
        var fila = jdbc.queryForMap("SELECT reason, status, identified_at, identification_evidence FROM"
                + " user_safety_exclusions WHERE exercise_id = ?", ejercicioDeSesion);
        assertThat(fila.get("reason")).isEqualTo("DOLOR_SESION_SIN_IDENTIFICAR");
        assertThat(fila.get("status")).isEqualTo("ACTIVA");
        assertThat(fila.get("identified_at")).isNotNull();
        assertThat(String.valueOf(fila.get("identification_evidence"))).startsWith("IDENTIFICACION_EXPLICITA");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_safety_exclusions", Integer.class)).isEqualTo(3);
    }

    @Test
    @DisplayName("Base nueva: la migracion completa desde cero llega a V65 con las columnas y la restriccion")
    void baseNueva() {
        try (var nueva = new PostgreSQLContainer("postgres:16-alpine")) {
            nueva.start();
            migrar(nueva, null);
            var j = new JdbcTemplate(new DriverManagerDataSource(nueva.getJdbcUrl(), nueva.getUsername(),
                    nueva.getPassword()));
            assertThat(j.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE NOT success", Integer.class))
                    .isZero();
            assertThat(j.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE success AND version = '65'",
                    Integer.class)).isEqualTo(1);
            assertThat(j.queryForList("SELECT column_name FROM information_schema.columns WHERE table_name ="
                    + " 'user_safety_exclusions' AND column_name LIKE 'identifi%' ORDER BY column_name", String.class))
                    .containsExactly("identification_evidence", "identified_at");
            assertThat(j.queryForObject("SELECT count(*) FROM pg_constraint WHERE conname = 'ck_use_identificacion'",
                    Integer.class)).isEqualTo(1);
        }
    }
}
