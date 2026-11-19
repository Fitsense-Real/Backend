package main.web.services.fitsense.analytics;

import main.web.services.fitsense.adaptation.interfaces.acl.AdaptationContextFacade;
import main.web.services.fitsense.analytics.interfaces.acl.AnalyticsContextFacade;
import main.web.services.fitsense.analytics.interfaces.acl.WeeklyMetricsView;
import main.web.services.fitsense.execution.domain.model.commands.FinishWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.model.commands.RecordSessionExerciseCommand;
import main.web.services.fitsense.execution.domain.model.commands.StartWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.services.WorkoutSessionCommandService;
import main.web.services.fitsense.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import main.web.services.fitsense.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.interfaces.acl.PlanningContextFacade;
import main.web.services.fitsense.planning.interfaces.rest.TrainingPlanController;
import main.web.services.fitsense.planning.interfaces.rest.resources.SkipWorkoutResource;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.BiologicalSex;
import main.web.services.fitsense.profiling.domain.model.valueobjects.FitnessLevel;
import main.web.services.fitsense.profiling.domain.model.valueobjects.GoalType;
import main.web.services.fitsense.profiling.domain.model.valueobjects.TrainingLocation;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import main.web.services.fitsense.shared.domain.model.valueobjects.SkipReason;
import main.web.services.fitsense.shared.domain.model.valueobjects.TrainingWeek;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RIESGO DE BAJA ADHERENCIA en dos semanas seguidas, con la base y la
 * configuracion reales (MVP-1.8): la caida se mide contra la semana anterior
 * guardada, y la adaptacion se decide con la observada. Esperados a mano.
 * <p>
 * PostgreSQL 16 efimero con V1-V33, motor de reglas, sin Replicate. Las
 * sesiones se fechan por SQL en el dia de su entrenamiento para que los dias
 * sin entrenar sean los de una persona real.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
class AdherenciaRiesgoIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired PlanningContextFacade planning;
    @Autowired AnalyticsContextFacade analytics;
    @Autowired AdaptationContextFacade adaptation;
    @Autowired UserProfileCommandService perfiles;
    @Autowired WorkoutSessionCommandService sesiones;
    @Autowired TrainingPlanController planes;
    @Autowired UserRepository usuarios;
    @Autowired JdbcTemplate jdbc;
    @Autowired WebApplicationContext contexto;

    MockMvc http;

    static final AtomicInteger SECUENCIA = new AtomicInteger();
    static final LocalDate S1 = TrainingWeek.containing(LocalDate.now()).startDate().plusWeeks(1);
    static final LocalDate S2 = S1.plusWeeks(1);
    static final List<String> EQUIPAMIENTO = List.of("dumbbell", "barbell", "cable", "leverage machine", "bench", "band");

    @BeforeEach
    void http() {
        http = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    /** Lo que se hace en cada uno de los tres dias: HECHO al 100 %, OMITIDO con causa, o NADA registrado. */
    enum Dia { HECHO, OMITIDO, NADA }

    private long participante() {
        int n = SECUENCIA.incrementAndGet();
        String email = "riesgo" + n + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Prueba', now(), now())", email);
        long userId = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(userId, LocalDate.of(1992, 5, 10), BiologicalSex.FEMALE,
                new BigDecimal("165"), new BigDecimal("68"), null, FitnessLevel.INTERMEDIATE, GoalType.GAIN_MUSCLE,
                "prueba de riesgo", TrainingLocation.GYM, (short) 3,
                List.of((short) 1, (short) 3, (short) 5), (short) 45, EQUIPAMIENTO,
                List.of(), "Ninguna", true, true, true, true, true));
        return userId;
    }

    /** Genera la semana, la reduce a un ejercicio de 3x10 por dia y la ejecuta. */
    private void semana(long userId, LocalDate lunes, Dia... dias) {
        long plan = planning.generateWeeklyPlan(userId, lunes, PlanAdjustment.none(), null, null).orElseThrow();
        var entrenamientos = jdbc.queryForList("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ?"
                + " ORDER BY scheduled_date", Long.class, plan);
        assertEquals(3, entrenamientos.size(), "premisa: tres entrenamientos");
        for (int i = 0; i < 3; i++) {
            long pw = entrenamientos.get(i);
            var ejercicios = jdbc.queryForList("SELECT planned_exercise_id FROM planned_workout_exercises"
                    + " WHERE planned_workout_id = ? ORDER BY exercise_order", Long.class, pw);
            for (int k = 1; k < ejercicios.size(); k++)
                jdbc.update("DELETE FROM planned_workout_exercises WHERE planned_exercise_id = ?", ejercicios.get(k));
            jdbc.update("UPDATE planned_workout_exercises SET prescription_type = 'SETS_REPS', planned_sets = 3,"
                    + " planned_reps = 10, planned_duration_seconds = NULL WHERE planned_exercise_id = ?", ejercicios.get(0));
            switch (dias[i]) {
                case HECHO -> {
                    long s = sesiones.handle(new StartWorkoutSessionCommand(userId, pw)).orElseThrow().getId();
                    sesiones.handle(new RecordSessionExerciseCommand(userId, s, ejercicios.get(0), (short) 3, 30,
                            null, null, null));
                    sesiones.handle(new FinishWorkoutSessionCommand(userId, s, (short) 6, null, (short) 40));
                    // La sesion, el dia de su entrenamiento a las 18:00.
                    jdbc.update("UPDATE workout_sessions SET started_at = pw.scheduled_date + time '18:00',"
                            + " ended_at = pw.scheduled_date + time '18:40' FROM planned_workouts pw"
                            + " WHERE pw.planned_workout_id = workout_sessions.planned_workout_id"
                            + " AND workout_sessions.session_id = ?", s);
                }
                case OMITIDO -> planes.skip(principal(userId), pw, new SkipWorkoutResource(SkipReason.LACK_OF_TIME));
                case NADA -> { }
            }
        }
    }

    private UserDetailsImpl principal(long userId) {
        return UserDetailsImpl.build(usuarios.findById(userId).orElseThrow());
    }

    private Map<String, Object> fila(long userId, LocalDate lunes) {
        return jdbc.queryForMap("SELECT weighted_adherence_pct, observed_adherence_pct, registration_coverage_pct,"
                + " consecutive_skips, risk_score, risk_level, risk_factors::text AS factores, is_dropout,"
                + " days_since_last_workout FROM weekly_user_metrics WHERE user_id = ? AND week_start_date = ?",
                userId, lunes);
    }

    private static void igual(String esperado, Object real) {
        assertNotNull(real, "esperado " + esperado);
        assertEquals(0, new BigDecimal(esperado).compareTo((BigDecimal) real), "esperado " + esperado + ", llego " + real);
    }

    private void tablaActiva() {
        var r = jdbc.queryForObject("SELECT params->'risk' FROM calculation_configs WHERE is_active", String.class)
                .replace(" ", "");
        // La misma tabla que suponen las cuentas a mano (y RiesgoEscenariosTest).
        for (var trozo : List.of("{\"points\":10,\"min_pct\":60.0}", "{\"points\":20,\"min_pct\":40.0}",
                "{\"points\":30,\"min_pct\":0.0}", "{\"points\":15,\"min_count\":2}", "{\"points\":25,\"min_count\":3}",
                "{\"points\":15,\"min_drop_pp\":25.0}", "\"HIGH\":50.0", "\"MODERATE\":25.0", "\"CRITICAL\":70.0"))
            assertTrue(r.contains(trozo), trozo + " en " + r);
    }

    @Test
    @DisplayName("de 100 % a 33 % con dos omisiones declaradas: 30 + 15 + 15 = 60, HIGH; la adaptacion actua")
    void caidaConOmisiones() throws Exception {
        tablaActiva();
        long u = participante();
        semana(u, S1, Dia.HECHO, Dia.HECHO, Dia.HECHO);
        semana(u, S2, Dia.HECHO, Dia.OMITIDO, Dia.OMITIDO);

        // Semana 1: 100 %, sin factores, LOW. La adaptacion conserva.
        analytics.calculateWeek(u, S1).orElseThrow();
        var f1 = fila(u, S1);
        igual("100.00", f1.get("weighted_adherence_pct"));
        igual("0.00", f1.get("risk_score"));
        assertEquals("LOW", f1.get("risk_level"));
        assertNull(f1.get("factores"));
        assertEquals("NO_CHANGE", adaptation.decideForWeek(u, S1).orElseThrow().outcome());

        // Semana 2: (1 + 0 + 0) / 3 = 33,33 -> 30; racha 2 -> 15; caida 66,67 pp -> 15. Total 60, HIGH.
        analytics.calculateWeek(u, S2).orElseThrow();
        var f2 = fila(u, S2);
        igual("33.33", f2.get("weighted_adherence_pct"));
        igual("33.33", f2.get("observed_adherence_pct"));
        igual("100.00", f2.get("registration_coverage_pct"));
        assertEquals(2, ((Number) f2.get("consecutive_skips")).intValue());
        igual("60.00", f2.get("risk_score"));
        assertEquals("HIGH", f2.get("risk_level"));
        var factores = String.valueOf(f2.get("factores")).replace(" ", "");
        assertTrue(factores.contains("\"low_adherence\":30") && factores.contains("\"consecutive_skips\":15")
                && factores.contains("\"adherence_drop\":15"), factores);
        assertEquals(false, f2.get("is_dropout"));

        // La app ve lo mismo.
        http.perform(get("/api/v1/users/me/metrics/weekly/" + S2).with(user(principal(u))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskLevel").value("HIGH"))
                .andExpect(jsonPath("$.riskScore").value(60.0))
                .andExpect(jsonPath("$.consecutiveSkips").value(2));

        // Observada 33,33 < 50 con cobertura 100: ampliada sobre las dos omitidas.
        var orden = adaptation.decideForWeek(u, S2).orElseThrow();
        assertEquals("ADAPT_EXPANDED", orden.outcome());
        assertEquals(2, orden.targetedSessions().size());
    }

    @Test
    @DisplayName("100 % dos semanas: sin riesgo y sin adaptacion")
    void estable() {
        long u = participante();
        semana(u, S1, Dia.HECHO, Dia.HECHO, Dia.HECHO);
        semana(u, S2, Dia.HECHO, Dia.HECHO, Dia.HECHO);
        analytics.calculateWeek(u, S1);
        analytics.calculateWeek(u, S2);
        var f2 = fila(u, S2);
        igual("100.00", f2.get("weighted_adherence_pct"));
        igual("0.00", f2.get("risk_score"));
        assertEquals("LOW", f2.get("risk_level"));
        assertEquals("NO_CHANGE", adaptation.decideForWeek(u, S2).orElseThrow().outcome());
    }

    @Test
    @DisplayName("dos dias sin registrar: el riesgo lo ve (60, HIGH) pero la adaptacion no decide sin evidencia")
    void sinRegistroNoSeAdapta() {
        long u = participante();
        semana(u, S1, Dia.HECHO, Dia.HECHO, Dia.HECHO);
        semana(u, S2, Dia.HECHO, Dia.NADA, Dia.NADA);
        analytics.calculateWeek(u, S1);
        analytics.calculateWeek(u, S2);
        var f2 = fila(u, S2);
        // Oficial: (1 + 0 + 0) / 3 = 33,33 -> mismo riesgo que con omisiones: 60, HIGH.
        igual("33.33", f2.get("weighted_adherence_pct"));
        igual("60.00", f2.get("risk_score"));
        assertEquals("HIGH", f2.get("risk_level"));
        // Observada: 1 / 1 = 100; cobertura 1 / 3 = 33,33 < 50 -> no hay evidencia.
        igual("100.00", f2.get("observed_adherence_pct"));
        igual("33.33", f2.get("registration_coverage_pct"));
        assertEquals("INSUFFICIENT_DATA", adaptation.decideForWeek(u, S2).orElseThrow().outcome());
    }

    @Test
    @DisplayName("una caida leve (100 -> 66,67 con una omision al final): 10 + 5 + 15 = 30, MODERATE; localizada")
    void caidaModerada() {
        long u = participante();
        semana(u, S1, Dia.HECHO, Dia.HECHO, Dia.HECHO);
        semana(u, S2, Dia.HECHO, Dia.HECHO, Dia.OMITIDO);
        analytics.calculateWeek(u, S1);
        analytics.calculateWeek(u, S2);
        var f2 = fila(u, S2);
        // (1 + 1 + 0) / 3 = 66,67 -> 10; racha 1 -> 5; caida 33,33 pp -> 15. Total 30.
        igual("66.67", f2.get("weighted_adherence_pct"));
        igual("30.00", f2.get("risk_score"));
        assertEquals("MODERATE", f2.get("risk_level"));
        // Observada 66,67: entre 50 y 80 -> se corrige solo la peor sesion.
        var orden = adaptation.decideForWeek(u, S2).orElseThrow();
        assertEquals("ADAPT_LOCALIZED", orden.outcome());
        assertEquals(1, orden.targetedSessions().size());
    }

    @Test
    @DisplayName("la caida depende de que la semana anterior este calculada: sin ella no puntua; al recalcular, si")
    void ordenDelCalculo() {
        long u = participante();
        semana(u, S1, Dia.HECHO, Dia.HECHO, Dia.HECHO);
        semana(u, S2, Dia.HECHO, Dia.OMITIDO, Dia.OMITIDO);
        // Semana 2 antes que la 1: 30 + 15 = 45, sin caida.
        analytics.calculateWeek(u, S2);
        igual("45.00", fila(u, S2).get("risk_score"));
        // Calculada la 1, recalcular la 2 la deja en 60 (idempotente: sobrescribe la fila).
        analytics.calculateWeek(u, S1);
        analytics.calculateWeek(u, S2);
        igual("60.00", fila(u, S2).get("risk_score"));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM weekly_user_metrics WHERE user_id = ?"
                + " AND week_start_date = ?", Integer.class, u, S2));
    }
}
