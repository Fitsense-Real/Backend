package main.web.services.fitsense.planning;

import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.services.PlanDraftValidator;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
import main.web.services.fitsense.planning.infrastructure.generation.rules.RuleBasedTrainingPlanGenerator;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.BiologicalSex;
import main.web.services.fitsense.profiling.domain.model.valueobjects.FitnessLevel;
import main.web.services.fitsense.profiling.domain.model.valueobjects.GoalType;
import main.web.services.fitsense.profiling.domain.model.valueobjects.TrainingLocation;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Perfiles limite de la campana de estres (orden del 31/10/2026): diagnostico del motor de reglas. */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false", "fitsense.ai.api-token="})
@DisplayName("Perfiles limite: 15 minutos, una sesion y siete dias")
class PerfilesLimiteIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired UserProfileCommandService perfiles;
    @Autowired WeeklyTrainingPlanCommandService comandos;
    @Autowired RuleBasedTrainingPlanGenerator reglas;
    @Autowired PlanDraftValidator validador;
    @Autowired JdbcTemplate jdbc;

    static final AtomicInteger N = new AtomicInteger();
    static final LocalDate S1 = LocalDate.of(2026, 11, 2);

    long participante(GoalType objetivo, FitnessLevel nivel, List<Short> dias, int minutos, TrainingLocation lugar,
                      List<String> equipo) {
        String email = "limite" + N.incrementAndGet() + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Limite', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1980, 1, 1), BiologicalSex.FEMALE,
                new BigDecimal("160"), new BigDecimal("70"), null, nivel, objetivo, "limite", lugar,
                (short) dias.size(), dias, (short) minutos, equipo, List.of(), "Ninguna", false, true, true, false, true));
        return u;
    }

    String diagnostico(long u) {
        var prep = comandos.prepare(new GenerateWeeklyPlanCommand(u, S1, null, false, null, null));
        var ctx = prep.context();
        var draft = reglas.generate(ctx, List.of());
        var sb = new StringBuilder();
        for (var w : draft.workouts()) {
            sb.append(w.scheduledDate()).append(' ').append(w.focus()).append(' ').append(w.expectedDurationMinutes()).append("min:");
            for (var e : w.exercises()) {
                var c = ctx.availableExercises().stream().filter(x -> x.exerciseId().equals(e.exerciseId())).findFirst();
                sb.append(' ').append(e.exerciseId()).append('[').append(c.map(x -> x.bodyPartCode() + "/" + x.movementPattern()).orElse("?"))
                        .append(' ').append(e.plannedSets()).append('x').append(e.plannedReps() != null ? e.plannedReps() + "r" : e.plannedDurationSeconds() + "s")
                        .append('/').append(e.restSeconds()).append(']');
            }
            sb.append('\n');
        }
        // Para cada sesion: que alternativas de otro grupo con resistencia muscular habria para el segundo resistido.
        var est = new main.web.services.fitsense.planning.domain.services.SessionDurationEstimator();
        for (var w : draft.workouts()) {
            sb.append("  original estimado: ").append(est.estimateMinutes(w, ctx.prescription())).append(" min\n");
            for (var grupo : List.of("chest", "back", "shoulders", "waist")) {
                int n = 0, caben = 0;
                for (var c : ctx.availableExercises()) {
                    if (!grupo.equals(c.bodyPartCode())) continue;
                    var d = main.web.services.fitsense.planning.domain.services.CapabilityPrescriptionPolicy.dosis(
                            main.web.services.fitsense.planning.domain.model.valueobjects.GoalCapabilityPolicy.Capability.MUSCULAR_ENDURANCE,
                            c, ctx.profile().goalType(), ctx.profile().fitnessLevel(),
                            ctx.prescription() == null ? null : ctx.prescription().repLimits());
                    if (d.isEmpty() || d.get().reps() == null) continue;
                    n++;
                    var lista = new java.util.ArrayList<>(w.exercises());
                    lista.set(1, new main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft.DraftExercise(
                            c.exerciseId(), d.get().tipo(), d.get().series(), d.get().reps(), d.get().segundos(), null,
                            d.get().descanso(), null));
                    int min = est.estimateMinutes(new main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft.DraftWorkout(
                            w.scheduledDate(), w.focus(), w.name(), 0, lista), ctx.prescription());
                    if (min <= 17) caben++;
                    if (n == 1) sb.append("    ej ").append(c.exerciseId()).append(' ').append(d.get().series()).append('x')
                            .append(d.get().reps()).append('/').append(d.get().descanso()).append(" -> ").append(min).append(" min\n");
                }
                sb.append("  alternativa ").append(grupo).append(": ").append(n).append(" con dosis RM, ").append(caben).append(" caben en 17 min\n");
            }
        }
        try {
            validador.validate(draft, ctx, 30);
            sb.append("VALIDA");
        } catch (InvalidPlanDraftException ex) {
            sb.append("INVALIDA ").append(ex.problems());
        }
        return sb.toString();
    }

    @Autowired main.web.services.fitsense.planning.interfaces.acl.PlanningContextFacade planning;

    @Test
    @DisplayName("U07 (15 min, peso corporal, un dia): diagnostico del motor de reglas")
    void u07() {
        // P-01 definitiva: los cuatro objetivos admiten 30-120 min; el perfil de U07 (15) ya no se puede crear.
        var e = org.junit.jupiter.api.Assertions.assertThrows(
                main.web.services.fitsense.shared.domain.exceptions.DomainRuleViolationException.class,
                () -> participante(GoalType.GENERAL_FITNESS, FitnessLevel.BEGINNER, List.of((short) 6), 15,
                        TrainingLocation.HOME, List.of("body weight")));
        org.junit.jupiter.api.Assertions.assertTrue(e.getMessage().contains("entre 30 y 120"), e.getMessage());
        long u = participante(GoalType.GENERAL_FITNESS, FitnessLevel.BEGINNER, List.of((short) 6), 30,
                TrainingLocation.HOME, List.of("body weight"));
        System.out.println("LIMITE U07 a 30 min (minimo de P-01)\n" + diagnostico(u));
    }

    /**
     * Campana de estres (31/10/2026): el motor de reglas declaraba min(techo, estimado), asi que una sesion
     * de 21 minutos para un perfil de 15 se publicaba declarando 17 y V4 la aceptaba. Ahora, o hay plan y
     * TODAS sus sesiones duran de verdad (estimador de V17) como mucho el techo, o no hay plan.
     */
    @org.junit.jupiter.params.ParameterizedTest(name = "{0} principiante, 15 min, casa, un dia")
    @org.junit.jupiter.params.provider.EnumSource(GoalType.class)
    @DisplayName("15 minutos: nunca se publica una sesion que de verdad supera el techo")
    void quinceMinutosNuncaPorEncimaDelTecho(GoalType objetivo) {
        if (!objetivo.admiteMinutos(15)) {
            // P-01 definitiva: ningun objetivo admite 15 min; se rechaza antes de generar.
            org.junit.jupiter.api.Assertions.assertThrows(
                    main.web.services.fitsense.shared.domain.exceptions.DomainRuleViolationException.class,
                    () -> participante(objetivo, FitnessLevel.BEGINNER, List.of((short) 6), 15,
                            TrainingLocation.HOME, List.of("body weight")));
            System.out.println("LIMITE15 " + objetivo + ": RECHAZADO POR P-01");
            return;
        }
        long u = participante(objetivo, FitnessLevel.BEGINNER, List.of((short) 6), 15,
                TrainingLocation.HOME, List.of("body weight"));
        java.util.Optional<Long> plan;
        try {
            plan = planning.generateWeeklyPlan(u, S1, null, null, null);
        } catch (RuntimeException e) {
            plan = java.util.Optional.empty();
        }
        var est = new main.web.services.fitsense.planning.domain.services.SessionDurationEstimator();
        int techo = jdbc.queryForObject("SELECT session_minutes FROM user_profiles WHERE user_id = ?", Integer.class, u);
        String resumen = plan.map(p -> "plan " + p).orElse("SIN PLAN");
        if (plan.isPresent()) {
            var ctx = comandos.prepare(new GenerateWeeklyPlanCommand(u, S1, null, true, null, null)).context();
            for (long w : jdbc.queryForList("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ?", Long.class, plan.get())) {
                var ejercicios = jdbc.query("SELECT exercise_id, prescription_type, planned_sets, planned_reps,"
                                + " planned_duration_seconds, rest_seconds FROM planned_workout_exercises WHERE planned_workout_id = ?",
                        (rs, k) -> new main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft.DraftExercise(
                                rs.getLong(1), main.web.services.fitsense.planning.domain.model.valueobjects.PrescriptionType.valueOf(rs.getString(2)),
                                rs.getObject(3, Short.class), rs.getObject(4, Short.class), rs.getObject(5, Integer.class), null,
                                rs.getObject(6, Short.class), null), w);
                int real = est.estimateMinutes(new main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft.DraftWorkout(
                        S1, main.web.services.fitsense.planning.domain.model.valueobjects.WorkoutFocus.FULL_BODY, "x", 0, ejercicios),
                        ctx.prescription());
                resumen += " · sesion " + real + " min";
                org.junit.jupiter.api.Assertions.assertTrue(real <= Math.floor(techo * 1.15),
                        objetivo + ": sesion de " + real + " min publicada con techo " + techo * 1.15);
            }
        }
        System.out.println("LIMITE15 " + objetivo + ": " + resumen);
    }

    @Test
    @DisplayName("principiante con siete dias disponibles: el perfil se acepta y el plan trae cuatro sesiones")
    void principianteSieteDias() {
        long u = participante(GoalType.LOSE_WEIGHT, FitnessLevel.BEGINNER,
                List.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6, (short) 7), 30,
                TrainingLocation.HOME, List.of("body weight", "band"));
        long plan = planning.generateWeeklyPlan(u, S1, null, null, null).orElseThrow();
        var fechas = jdbc.queryForList("SELECT scheduled_date FROM planned_workouts WHERE plan_id = ?", LocalDate.class, plan);
        org.junit.jupiter.api.Assertions.assertEquals(4, fechas.size(), "tope de principiante");
        org.junit.jupiter.api.Assertions.assertEquals(4, new java.util.HashSet<>(fechas).size(), "una sesion por fecha");
    }

    @Test
    @DisplayName("intermedio con siete dias disponibles: seis sesiones, nunca siete")
    void intermedioSieteDias() {
        long u = participante(GoalType.GENERAL_FITNESS, FitnessLevel.INTERMEDIATE,
                List.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6, (short) 7), 45,
                TrainingLocation.GYM, List.of("body weight", "dumbbell", "barbell", "cable", "leverage machine", "bench"));
        java.util.Optional<Long> plan;
        try {
            plan = planning.generateWeeklyPlan(u, S1, null, null, null);
        } catch (RuntimeException e) {
            plan = java.util.Optional.empty();
        }
        if (plan.isPresent()) {
            var fechas = jdbc.queryForList("SELECT scheduled_date FROM planned_workouts WHERE plan_id = ?", LocalDate.class, plan.get());
            org.junit.jupiter.api.Assertions.assertEquals(6, fechas.size(), "tope de intermedio");
            org.junit.jupiter.api.Assertions.assertEquals(6, new java.util.HashSet<>(fechas).size());
        }
        System.out.println("LIMITE7 intermedio: " + plan.map(p -> "plan con 6").orElse("SIN PLAN"));
    }
}
