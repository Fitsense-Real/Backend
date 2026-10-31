package main.web.services.fitsense.lectura;

import main.web.services.fitsense.execution.domain.model.commands.FinishWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.model.commands.RecordSessionExerciseCommand;
import main.web.services.fitsense.execution.domain.model.commands.StartWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.services.WorkoutSessionCommandService;
import main.web.services.fitsense.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import main.web.services.fitsense.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import main.web.services.fitsense.orchestration.application.internal.WeeklyCycleService;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.domain.services.SessionDurationEstimator;
import main.web.services.fitsense.planning.infrastructure.generation.ai.ReplicateSessionFixGenerator;
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
import main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion;
import main.web.services.fitsense.shared.domain.model.valueobjects.TrainingWeek;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * SOLO PARA ENSAYAR {@link EvaluacionLecturaV24IT} ANTES DE USARLA CON NEON.
 * <p>
 * Siembra una base LOCAL vacia (nunca Neon: exige una URL a 127.0.0.1 o
 * localhost) con el ciclo de produccion y la progresion APAGADA, como esta Neon
 * hoy. Escenarios con resultado conocido:
 * <ul>
 *   <li>cuatro objetivos, dos semanas cumplidas: A y B publican;</li>
 *   <li>{@code GENERAL_FITNESS} con una reduccion por fatiga APLICADA (sin la V24 por
 *       capacidades): B la reconoce como heredada con linaje y publica;</li>
 *   <li>{@code GENERAL_FITNESS} y {@code GAIN_MUSCLE} con una dosis puesta a mano sin
 *       paso ni reduccion: GAIN_MUSCLE la rechazan A y B (4D); GENERAL_FITNESS solo B.</li>
 * </ul>
 */
@EnabledIfSystemProperty(named = "fitsense.lectura.semilla.url", matches = "jdbc:postgresql://(127\\.0\\.0\\.1|localhost)[:/].*")
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
class SemillaLecturaIT {

    @DynamicPropertySource
    static void base(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", () -> System.getProperty("fitsense.lectura.semilla.url"));
        r.add("spring.datasource.username", () -> System.getProperty("fitsense.lectura.semilla.usuario", "postgres"));
        r.add("spring.datasource.password", () -> System.getProperty("fitsense.lectura.semilla.clave", "pruebas"));
    }

    @MockitoBean ReplicateSessionFixGenerator ia;

    @Autowired WeeklyCycleService ciclo;
    @Autowired PlanningContextFacade planning;
    @Autowired UserProfileCommandService perfiles;
    @Autowired WorkoutSessionCommandService sesiones;
    @Autowired TrainingPlanController planes;
    @Autowired UserRepository usuarios;
    @Autowired SessionDurationEstimator estimador;
    @Autowired JdbcTemplate jdbc;

    static final LocalDate S1 = TrainingWeek.containing(LocalDate.now()).startDate().plusWeeks(1);

    @Test
    void sembrar() {
        when(ia.isEnabled()).thenReturn(true);
        when(ia.modelName()).thenReturn("simulado");
        when(ia.pedirRespuesta(any(), any(), any(), any(), anyInt()))
                .thenReturn(new ReplicateSessionFixGenerator.RawFix("{}", "salida-simulada"));
        when(ia.interpretar(anyString(), any())).thenAnswer(inv -> reducir(inv.getArgument(1)));

        int n = 0;
        for (var objetivo : GoalType.values()) {
            long u = participante(objetivo, ++n);
            long w1 = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
            hacer(u, w1, false);
            ciclo.runFor(u, S1, S1.plusWeeks(1)).orElseThrow();
        }
        // Reduccion por fatiga aplicada (progresion apagada).
        long f = participante(GoalType.GENERAL_FITNESS, ++n);
        long f1 = planning.generateWeeklyPlan(f, S1, PlanAdjustment.none(), null, null).orElseThrow();
        hacer(f, f1, true);
        ciclo.runFor(f, S1, S1.plusWeeks(1)).orElseThrow();
        assertEquals("APLICADA", jdbc.queryForObject("SELECT status FROM plan_adaptation_attempts WHERE user_id = ?"
                + " ORDER BY attempt_id DESC LIMIT 1", String.class, f));
        // Dosis arbitrarias en la ultima semana publicada.
        for (var objetivo : List.of(GoalType.GENERAL_FITNESS, GoalType.GAIN_MUSCLE)) {
            long u = participante(objetivo, ++n);
            long w1 = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
            jdbc.update("UPDATE planned_workout_exercises SET planned_reps = planned_reps + 1 WHERE planned_exercise_id ="
                    + " (SELECT e.planned_exercise_id FROM planned_workout_exercises e JOIN planned_workouts w"
                    + " ON w.planned_workout_id = e.planned_workout_id WHERE w.plan_id = ? AND e.prescription_type ="
                    + " 'SETS_REPS' ORDER BY w.scheduled_date, e.exercise_order LIMIT 1)", w1);
        }
        System.out.println("SEMILLA: " + n + " participantes");
    }

    private List<CorrectedSession> reducir(PlanGenerationContext ctx) {
        var salida = new ArrayList<CorrectedSession>();
        for (var senalada : ctx.adjustment().targetedSessions()) {
            if (senalada.intervencion() != TipoIntervencion.REVISAR_CARGA_Y_RECUPERACION) continue;
            var f = jdbc.queryForMap("SELECT scheduled_date, focus_code FROM planned_workouts WHERE planned_workout_id = ?",
                    senalada.plannedWorkoutId());
            var lista = new ArrayList<>(jdbc.query("SELECT exercise_id, prescription_type, planned_sets, planned_reps,"
                            + " planned_duration_seconds, rest_seconds FROM planned_workout_exercises"
                            + " WHERE planned_workout_id = ? ORDER BY exercise_order",
                    (rs, k) -> new PlanDraft.DraftExercise(rs.getLong(1), PrescriptionType.valueOf(rs.getString(2)),
                            rs.getObject(3, Short.class), rs.getObject(4, Short.class), rs.getObject(5, Integer.class),
                            null, rs.getObject(6, Short.class), null), senalada.plannedWorkoutId()));
            for (int k = 0; k < lista.size(); k++) {
                var e = lista.get(k);
                if (e.plannedSets() != null && e.plannedSets() > 1 && e.prescriptionType() == PrescriptionType.SETS_REPS) {
                    lista.set(k, new PlanDraft.DraftExercise(e.exerciseId(), e.prescriptionType(),
                            (short) (e.plannedSets() - 1), e.plannedReps(), null, null, e.restSeconds(), null));
                    break;
                }
            }
            var fecha = ((java.sql.Date) f.get("scheduled_date")).toLocalDate().plusWeeks(1);
            int minutos = estimador.estimateMinutes(new PlanDraft.DraftWorkout(fecha,
                    WorkoutFocus.valueOf((String) f.get("focus_code")), "x", 0, lista), ctx.prescription());
            salida.add(new CorrectedSession(senalada.plannedWorkoutId(), fecha, minutos, lista, "reduccion simulada"));
        }
        return salida;
    }

    private long participante(GoalType objetivo, int n) {
        String email = "semilla" + n + "-" + System.nanoTime() + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Prueba', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1992, 5, 10), BiologicalSex.FEMALE,
                new BigDecimal("165"), new BigDecimal("68"), null, FitnessLevel.INTERMEDIATE, objetivo, "semilla",
                TrainingLocation.GYM, (short) 3, List.of((short) 1, (short) 3, (short) 5), (short) 45,
                List.of("dumbbell", "barbell", "cable", "leverage machine", "bench", "band"),
                List.of(), "Ninguna", true, true, true, true, true));
        return u;
    }

    /** Las tres sesiones completadas; con {@code fatigaViernes}, el viernes se omite por fatiga. */
    private void hacer(long u, long planId, boolean fatigaViernes) {
        var ids = jdbc.queryForList("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ?"
                + " ORDER BY scheduled_date", Long.class, planId);
        for (int i = 0; i < ids.size(); i++) {
            long w = ids.get(i);
            if (fatigaViernes && i == ids.size() - 1) {
                planes.skip(UserDetailsImpl.build(usuarios.findById(u).orElseThrow()), w,
                        new SkipWorkoutResource(SkipReason.FATIGUE));
                continue;
            }
            long s = sesiones.handle(new StartWorkoutSessionCommand(u, w)).orElseThrow().getId();
            for (var f : jdbc.queryForList("SELECT planned_exercise_id, prescription_type, planned_sets, planned_reps,"
                    + " planned_duration_seconds FROM planned_workout_exercises WHERE planned_workout_id = ?", w)) {
                boolean duracion = "DURATION".equals(f.get("prescription_type"));
                int series = f.get("planned_sets") == null ? 1 : ((Number) f.get("planned_sets")).intValue();
                int total = series * ((Number) f.get(duracion ? "planned_duration_seconds" : "planned_reps")).intValue();
                sesiones.handle(new RecordSessionExerciseCommand(u, s, ((Number) f.get("planned_exercise_id")).longValue(),
                        (short) series, duracion ? null : total, duracion ? total : null, null, null));
            }
            sesiones.handle(new FinishWorkoutSessionCommand(u, s, (short) 6, (short) 4, (short) 45));
        }
    }
}
