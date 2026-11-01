package main.web.services.fitsense.orchestation;

import main.web.services.fitsense.execution.domain.model.commands.FinishWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.model.commands.RecordSessionExerciseCommand;
import main.web.services.fitsense.execution.domain.model.commands.StartWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.services.WorkoutSessionCommandService;
import main.web.services.fitsense.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import main.web.services.fitsense.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import main.web.services.fitsense.orchestration.application.internal.WeeklyCycleService;
import main.web.services.fitsense.planning.application.internal.commandservices.PlanningPolicySelector;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanningPolicy;
import main.web.services.fitsense.planning.infrastructure.generation.ai.ReplicateSessionFixGenerator;
import main.web.services.fitsense.planning.interfaces.acl.PlanningContextFacade;
import main.web.services.fitsense.planning.interfaces.rest.TrainingPlanController;
import main.web.services.fitsense.planning.interfaces.rest.resources.SkipWorkoutResource;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.*;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import main.web.services.fitsense.shared.domain.model.valueobjects.ContextoFaltaDeTiempo;
import main.web.services.fitsense.shared.domain.model.valueobjects.SkipReason;
import main.web.services.fitsense.shared.infrastructure.json.JsonSupport;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * V4-C de extremo a extremo con la politica PLAN-V4-0.1 configurada explicitamente (candidata permitida solo en esta
 * prueba, base efimera, IA apagada): generacion inicial, adaptacion con ACORTAR, dolor y fatiga, idempotencia del
 * ciclo y planes historicos sin reinterpretar. Cada sesion publicada cabe en {@code M x 60 x 115 %} segundos
 * estimados, para 30, 45, 60 y 75 minutos.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.adaptation.progression.enabled=true",
        "fitsense.planning.policy=PLAN-V4-0.1",
        "fitsense.planning.policy.allow-candidates=true"
})
@DisplayName("PLAN-V4-0.1 en el ciclo real: techo en segundos estimados, persistencia y traza")
class PoliticaV4CCicloIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @MockitoBean ReplicateSessionFixGenerator ia;

    @Autowired UserProfileCommandService perfiles;
    @Autowired PlanningContextFacade planning;
    @Autowired WeeklyCycleService ciclo;
    @Autowired WorkoutSessionCommandService sesiones;
    @Autowired TrainingPlanController planes;
    @Autowired UserRepository usuarios;
    @Autowired PlanningPolicySelector selector;
    @Autowired JdbcTemplate jdbc;

    static final LocalDate SEMANA = LocalDate.of(2026, 11, 2);
    static final JsonSupport JSON = new JsonSupport();
    static final Map<Integer, List<Short>> DIAS = Map.of(
            3, List.of((short) 1, (short) 3, (short) 5), 4, List.of((short) 1, (short) 2, (short) 4, (short) 5));
    static final List<String> GIMNASIO = List.of("body weight", "dumbbell", "barbell", "cable", "leverage machine",
            "bench", "band", "stationary bike", "treadmill", "kettlebell", "ez barbell", "smith machine");

    record Caso(GoalType objetivo, FitnessLevel nivel, int dias, int minutos) {}

    static final List<Caso> CASOS = List.of(
            new Caso(GoalType.GENERAL_FITNESS, FitnessLevel.BEGINNER, 3, 30),
            new Caso(GoalType.GAIN_MUSCLE, FitnessLevel.INTERMEDIATE, 3, 45),
            new Caso(GoalType.LOSE_WEIGHT, FitnessLevel.INTERMEDIATE, 4, 60),
            new Caso(GoalType.INCREASE_STRENGTH, FitnessLevel.ADVANCED, 4, 75));

    static int usuario = 0;

    @BeforeEach
    void antes() {
        when(ia.isEnabled()).thenReturn(false);
        assertTrue(postgres.getJdbcUrl().startsWith("jdbc:postgresql://localhost:"), "solo base efimera");
        assertSame(PlanningPolicy.V4C, selector.activa(), "la politica la fija la configuracion, nada la cambia sola");
    }

    @Test
    @DisplayName("generacion inicial y 3 semanas de adaptacion (ACORTAR, dolor, fatiga): todo plan es PLAN-V4-0.1 y cabe")
    void cicloConV4C() {
        for (var c : CASOS) {
            long u = crear(c);
            long plan = planning.generateWeeklyPlan(u, SEMANA, PlanAdjustment.none(), null, null).orElseThrow();
            comprobar(plan, c.minutos(), "GENERACION");
            String[] guiones = {"ACORTAR", "DOLOR", "FATIGA_RPE9"};
            for (int s = 0; s < guiones.length; s++) {
                var inicio = SEMANA.plusWeeks(s);
                guion(u, plan, guiones[s], c.minutos());
                var siguiente = ciclo.runFor(u, inicio, inicio.plusWeeks(1));
                var repetido = ciclo.runFor(u, inicio, inicio.plusWeeks(1));
                assertEquals(siguiente, repetido, "el ciclo es idempotente");
                assertTrue(siguiente.isPresent(), c + " sin plan en la semana " + (s + 2));
                plan = siguiente.get();
                int activos = jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ? AND"
                        + " week_start_date = ? AND status = 'ACTIVE'", Integer.class, u, inicio.plusWeeks(1));
                assertEquals(1, activos, "un solo plan activo por semana");
                comprobar(plan, c.minutos(), null);
                int excluidos = jdbc.queryForObject("SELECT count(*) FROM planned_workout_exercises pwe JOIN"
                        + " planned_workouts pw ON pw.planned_workout_id = pwe.planned_workout_id JOIN"
                        + " user_safety_exclusions x ON x.user_id = ? AND x.exercise_id = pwe.exercise_id AND x.status ="
                        + " 'ACTIVA' WHERE pw.plan_id = ?", Integer.class, u, plan);
                assertEquals(0, excluidos, "ningun ejercicio excluido por dolor vuelve al plan");
            }
        }
        var piso = jdbc.queryForObject("SELECT params->'prescription'->>'session_minutes_floor_pct' FROM"
                + " calculation_configs WHERE is_active", String.class);
        assertEquals("70", piso, "V4-C no toca calculation_configs");
    }

    @Test
    @DisplayName("planes historicos: un plan sin politica (anterior a V44) no se reinterpreta; el siguiente nace con V4-C")
    void planesHistoricosIntactos() {
        var c = new Caso(GoalType.GAIN_MUSCLE, FitnessLevel.INTERMEDIATE, 3, 60);
        long u = crear(c);
        long plan = planning.generateWeeklyPlan(u, SEMANA, PlanAdjustment.none(), null, null).orElseThrow();
        jdbc.update("UPDATE weekly_training_plans SET planning_policy = NULL, planning_policy_trace = NULL WHERE"
                + " plan_id = ?", plan);
        guion(u, plan, "COMPLETA", c.minutos());
        long siguiente = ciclo.runFor(u, SEMANA, SEMANA.plusWeeks(1)).orElseThrow();
        var antiguo = jdbc.queryForMap("SELECT planning_policy, planning_policy_trace FROM weekly_training_plans WHERE"
                + " plan_id = ?", plan);
        assertNull(antiguo.get("planning_policy"), "el plan publicado antes no se reescribe");
        assertNull(antiguo.get("planning_policy_trace"));
        comprobar(siguiente, c.minutos(), null);
    }

    @SuppressWarnings("unchecked")
    void comprobar(long plan, int minutos, String contexto) {
        var p = jdbc.queryForMap("SELECT planning_policy, planning_policy_trace::text traza FROM weekly_training_plans"
                + " WHERE plan_id = ?", plan);
        assertEquals("PLAN-V4-0.1", p.get("planning_policy"), "plan " + plan);
        var t = JSON.read(String.valueOf(p.get("traza")), Map.class);
        assertEquals("PLAN-V4-0.1", t.get("politica"));
        if (contexto != null) assertEquals(contexto, t.get("contexto"));
        var params = (Map<String, Object>) t.get("parametros");
        assertEquals("SEGUNDOS", params.get("v4"));
        int m = ((Number) t.get("minutos_disponibles")).intValue();
        assertTrue(m <= minutos, "minutos disponibles " + m);
        long limite = (long) m * 60 * 115 / 100;
        assertEquals(limite, ((Number) ((Map<String, Object>) params.get("limite_v4")).get("segundos")).longValue());
        var ses = (List<Map<String, Object>>) t.get("sesiones");
        assertFalse(ses.isEmpty());
        int enBase = jdbc.queryForObject("SELECT count(*) FROM planned_workouts WHERE plan_id = ?", Integer.class, plan);
        assertEquals(enBase, ses.size(), "la traza cubre todas las sesiones publicadas");
        for (var s : ses) {
            long seg = ((Number) s.get("segundos_estimados")).longValue();
            assertTrue(seg * 100 <= (long) m * 60 * 115, "plan " + plan + " sesion " + s.get("fecha") + ": " + seg
                    + " s > " + limite + " s (" + m + " min + 15 %)");
        }
    }

    long crear(Caso c) {
        String email = "v4c" + (++usuario) + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'V4C', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1990, 6, 15), BiologicalSex.FEMALE,
                new BigDecimal("168"), new BigDecimal("68"), null, c.nivel(), c.objetivo(), "V4-C",
                TrainingLocation.GYM, (short) c.dias(), DIAS.get(c.dias()), (short) c.minutos(), GIMNASIO, List.of(),
                "Ninguna", true, true, true, true, true));
        return u;
    }

    void guion(long u, long plan, String guion, int minutos) {
        var ws = jdbc.queryForList("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ? ORDER BY"
                + " scheduled_date", Long.class, plan);
        var user = UserDetailsImpl.build(usuarios.findById(u).orElseThrow());
        for (int k = 0; k < ws.size(); k++) {
            long w = ws.get(k);
            switch (guion) {
                case "DOLOR" -> {
                    if (k == 0) planes.skip(user, w, new SkipWorkoutResource(SkipReason.PAIN_OR_DISCOMFORT));
                    else hacer(u, w, (short) 6);
                }
                case "ACORTAR" -> {
                    if (k == 0) planes.skip(user, w, new SkipWorkoutResource(SkipReason.LACK_OF_TIME,
                            ContextoFaltaDeTiempo.MENOS_TIEMPO_HABITUAL, Math.max(15, minutos / 2)));
                    else hacer(u, w, (short) 6);
                }
                case "FATIGA_RPE9" -> hacer(u, w, (short) 9);
                default -> hacer(u, w, (short) 6);
            }
        }
    }

    void hacer(long u, long w, short rpe) {
        long sesion = sesiones.handle(new StartWorkoutSessionCommand(u, w)).orElseThrow().getId();
        for (var f : jdbc.queryForList("SELECT planned_exercise_id, prescription_type, planned_sets, planned_reps,"
                + " planned_duration_seconds FROM planned_workout_exercises WHERE planned_workout_id = ?"
                + " ORDER BY exercise_order", w)) {
            long pe = ((Number) f.get("planned_exercise_id")).longValue();
            int series = f.get("planned_sets") == null ? 1 : ((Number) f.get("planned_sets")).intValue();
            boolean dur = "DURATION".equals(f.get("prescription_type"));
            int reps = f.get("planned_reps") == null ? 0 : ((Number) f.get("planned_reps")).intValue();
            int seg = f.get("planned_duration_seconds") == null ? 0 : ((Number) f.get("planned_duration_seconds")).intValue();
            sesiones.handle(new RecordSessionExerciseCommand(u, sesion, pe, (short) series, dur ? null : series * reps,
                    dur ? series * seg : null, null, null));
        }
        sesiones.handle(new FinishWorkoutSessionCommand(u, sesion, rpe, (short) 4, (short) 45));
    }
}
