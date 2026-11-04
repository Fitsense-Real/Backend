package main.web.services.fitsense.orchestation;

import main.web.services.fitsense.RelojPruebas;
import main.web.services.fitsense.execution.domain.model.commands.FinishWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.model.commands.RecordSessionExerciseCommand;
import main.web.services.fitsense.execution.domain.model.commands.StartWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.services.WorkoutSessionCommandService;
import main.web.services.fitsense.orchestration.application.internal.WeeklyCycleService;
import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.infrastructure.generation.ai.ReplicateSessionFixGenerator;
import main.web.services.fitsense.planning.interfaces.acl.PlanningContextFacade;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.BiologicalSex;
import main.web.services.fitsense.profiling.domain.model.valueobjects.FitnessLevel;
import main.web.services.fitsense.profiling.domain.model.valueobjects.GoalType;
import main.web.services.fitsense.profiling.domain.model.valueobjects.TrainingLocation;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import main.web.services.fitsense.shared.domain.model.valueobjects.SkipReason;
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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * EL OTRO LADO DE {@code RespaldoConDolorIT.conDolorSinSustitutoNoPublicaNadaInvalido} (cierre del PR #39). Aquella
 * prueba fija el comportamiento HISTORICO con la configuracion por defecto: el 1373 (circulos de muneca, relleno, sin
 * sustituto SUST-SEG-1) deja la semana NO_FACTIBLE. Esta lo fija con la configuracion EXPERIMENTAL (limite
 * {@code preferencia} + retirada localizada): la misma situacion -respaldo forzado, dolor identificado en el 1373- se
 * publica conservando la semana sin el 1373. Las dos pruebas corren en CI con su propia configuracion, asi que ninguna
 * diferencia queda oculta: cada comportamiento tiene su prueba.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.adaptation.progression.enabled=false",
        "fitsense.adaptation.difficulty-guard=preferencia",
        "fitsense.adaptation.pain-localized-removal=on"
})
@DisplayName("Retirada localizada (configuracion experimental): el 1373 sin sustituto se retira y la semana se publica")
class RetiradaLocalizada1373IT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @MockitoBean ReplicateSessionFixGenerator ia;
    @MockitoBean main.web.services.fitsense.planning.infrastructure.scheduling.OverdueWorkoutScheduler vencidas;

    @Autowired WeeklyCycleService ciclo;
    @Autowired PlanningContextFacade planning;
    @Autowired UserProfileCommandService perfiles;
    @Autowired WorkoutSessionCommandService sesiones;
    @Autowired JdbcTemplate jdbc;

    static final LocalDate S1 = RelojPruebas.SEMANA_REFERENCIA;
    static final List<String> GIMNASIO = List.of("body weight", "dumbbell", "barbell", "cable", "leverage machine",
            "bench", "band", "stationary bike", "treadmill", "kettlebell", "ez barbell", "smith machine");
    static final long SIN_SUSTITUTO = 1373L;

    @BeforeEach
    void iaQueSiempreFalla() {
        reset(ia);
        when(ia.isEnabled()).thenReturn(true);
        when(ia.modelName()).thenReturn("simulado");
        when(ia.pedirRespuesta(any(), any(), any(), any(), anyInt()))
                .thenReturn(new ReplicateSessionFixGenerator.RawFix("{}", "salida-simulada"));
        when(ia.interpretar(anyString(), any())).thenThrow(new InvalidPlanDraftException(
                List.of("PRUEBA: propuesta rechazada de forma controlada para forzar el respaldo")));
    }

    @Test
    @DisplayName("respaldo con dolor en el 1373: se retira solo el 1373, la semana se publica y la exclusion sigue activa")
    void retiraElRelleno() {
        long u = -1, plan = -1;
        int sesion = -1;
        for (int intento = 0; intento < 60 && sesion < 0; intento++) {
            u = participante(intento);
            plan = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
            var ids = sesionesDe(plan);
            for (int i = 0; i < ids.size() && sesion < 0; i++)
                if (ejerciciosDe(ids.get(i)).contains(SIN_SUSTITUTO)) sesion = i;
        }
        assertTrue(sesion >= 0, "premisa: algun participante lleva el 1373 en la semana de referencia");
        var ids = sesionesDe(plan);
        for (int i = 0; i < ids.size(); i++) hacer(u, ids.get(i), i == sesion ? SIN_SUSTITUTO : -1L);

        var publicado = ciclo.runFor(u, S1, S1.plusWeeks(1));
        var ev = jdbc.queryForMap("SELECT status, generation_source, conservation_report::text AS informe FROM"
                + " plan_adaptation_attempts WHERE user_id = ? ORDER BY attempt_id DESC LIMIT 1", u);
        System.out.println("RETIRADA_1373 u=" + u + " publicado=" + publicado + " ev=" + ev);
        assertTrue(publicado.isPresent(), "con la retirada, la semana se publica: " + ev);
        assertEquals("RESPALDO", ev.get("status"));
        assertTrue(String.valueOf(ev.get("informe")).contains("RETIRADA_SEGURA"), String.valueOf(ev.get("informe")));
        assertFalse(contiene(publicado.get(), SIN_SUSTITUTO), "lo que dolio no vuelve");
        assertTrue(jdbc.queryForObject("SELECT count(*) FROM user_safety_exclusions WHERE user_id = ? AND exercise_id = ?"
                + " AND status = 'ACTIVA'", Integer.class, u, SIN_SUSTITUTO) > 0, "la exclusion no se levanta");
        // Las demas sesiones, identicas a la semana medida (ejercicios y orden).
        var nuevas = sesionesDe(publicado.get());
        assertEquals(ids.size(), nuevas.size());
        for (int i = 0; i < ids.size(); i++) {
            var esperado = new java.util.ArrayList<>(ejerciciosDe(ids.get(i)));
            if (i == sesion) esperado.remove(SIN_SUSTITUTO);
            assertEquals(esperado, ejerciciosDe(nuevas.get(i)), "sesion " + i);
        }
    }

    private long participante(int n) {
        String email = "retirada-1373-" + n + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Prueba', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1992, 1, 15), BiologicalSex.FEMALE,
                new BigDecimal("165"), new BigDecimal("70"), null, FitnessLevel.INTERMEDIATE, GoalType.GENERAL_FITNESS,
                "respaldo", TrainingLocation.GYM, (short) 3, List.of((short) 1, (short) 3, (short) 5), (short) 45,
                GIMNASIO, List.of(), "Ninguna", true, true, true, true, true));
        return u;
    }

    private List<Long> sesionesDe(long plan) {
        return jdbc.queryForList("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ? ORDER BY scheduled_date",
                Long.class, plan);
    }

    private List<Long> ejerciciosDe(long plannedWorkoutId) {
        return jdbc.queryForList("SELECT exercise_id FROM planned_workout_exercises WHERE planned_workout_id = ?"
                + " ORDER BY exercise_order", Long.class, plannedWorkoutId);
    }

    private void hacer(long u, long w, long conDolor) {
        RelojPruebas.abrirVentanaDeEjecucion(jdbc, u);
        long s = sesiones.handle(new StartWorkoutSessionCommand(u, w)).orElseThrow().getId();
        for (var f : jdbc.queryForList("SELECT pwe.planned_exercise_id, pwe.exercise_id, pwe.prescription_type,"
                + " pwe.planned_sets, pwe.planned_reps, pwe.planned_duration_seconds FROM planned_workout_exercises pwe"
                + " WHERE pwe.planned_workout_id = ? ORDER BY pwe.exercise_order", w)) {
            long pe = ((Number) f.get("planned_exercise_id")).longValue();
            if (((Number) f.get("exercise_id")).longValue() == conDolor) {
                sesiones.handle(new RecordSessionExerciseCommand(u, s, pe, (short) 0, null, null, null,
                        SkipReason.PAIN_OR_DISCOMFORT));
                continue;
            }
            boolean duracion = "DURATION".equals(f.get("prescription_type"));
            int series = f.get("planned_sets") == null ? 1 : ((Number) f.get("planned_sets")).intValue();
            int total = series * ((Number) f.get(duracion ? "planned_duration_seconds" : "planned_reps")).intValue();
            sesiones.handle(new RecordSessionExerciseCommand(u, s, pe, (short) series, duracion ? null : total,
                    duracion ? total : null, null, null));
        }
        sesiones.handle(new FinishWorkoutSessionCommand(u, s, (short) 6, (short) 4, (short) 45));
    }

    private boolean contiene(long plan, long ejercicio) {
        return jdbc.queryForObject("SELECT count(*) FROM planned_workout_exercises pwe JOIN planned_workouts pw"
                + " ON pw.planned_workout_id = pwe.planned_workout_id WHERE pw.plan_id = ? AND pwe.exercise_id = ?",
                Integer.class, plan, ejercicio) > 0;
    }
}
