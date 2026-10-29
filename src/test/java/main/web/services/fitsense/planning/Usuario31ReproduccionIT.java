package main.web.services.fitsense.planning;

import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.domain.services.PlanDraftValidator;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
import main.web.services.fitsense.planning.interfaces.acl.PlanningContextFacade;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.FitnessLevel;
import main.web.services.fitsense.profiling.domain.model.valueobjects.GoalType;
import main.web.services.fitsense.profiling.domain.model.valueobjects.TrainingLocation;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import main.web.services.fitsense.shared.domain.model.valueobjects.TrainingWeek;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Usuario 31 (29/10/2026): tres propuestas de Replicate rechazadas y plan 63 por
 * RULE_ENGINE. Se reproducen SIN llamar a Replicate, con las respuestas guardadas
 * en el registro y el perfil exacto (sin relajar ninguna restriccion), por el
 * validador de produccion:
 *   1. con el catalogo actual (V1-V36);
 *   2. con las correcciones CONFIRMADAS en la auditoria visual que tocan estos
 *      ejercicios, aplicadas solo a esta base desechable. No es una migracion.
 * El contenedor es compartido: el escenario corregido va despues del actual.
 * Las fechas se desplazan a una semana futura; el validador no depende de la
 * fecha absoluta, solo de los dias (sabado y domingo consecutivos).
 */
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
class Usuario31ReproduccionIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired PlanningContextFacade planning;
    @Autowired WeeklyTrainingPlanCommandService planes;
    @Autowired UserProfileCommandService perfiles;
    @Autowired PlanDraftValidator validador;
    @Autowired JdbcTemplate jdbc;

    static final LocalDate LUNES = TrainingWeek.containing(LocalDate.now()).startDate().plusWeeks(1);
    static final LocalDate SABADO = LUNES.plusDays(5), DOMINGO = LUNES.plusDays(6);

    static final List<String> EQUIPO = List.of("ab wheel", "balance board", "band", "barbell", "bench", "body weight",
            "bosu ball", "cable", "captains chair", "dumbbell", "gymnastic rings", "kettlebell", "landmine",
            "leverage machine", "olympic barbell", "parallel bars", "pull-up bar", "smith machine", "stationary bike",
            "suspension straps", "treadmill");

    private long usuario(String email) {
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Prueba', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(2005, 5, 12), null, new BigDecimal("190"),
                new BigDecimal("90"), new BigDecimal("80"), FitnessLevel.BEGINNER, GoalType.GENERAL_FITNESS, "TODO",
                TrainingLocation.MIXED, (short) 2, List.of((short) 6, (short) 7), (short) 45, EQUIPO, List.of(), null,
                false, false, true, false, true));
        return u;
    }

    static PlanDraft.DraftExercise sr(long id, int series, int reps, int descanso) {
        return new PlanDraft.DraftExercise(id, PrescriptionType.SETS_REPS, (short) series, (short) reps, null, null,
                (short) descanso, null);
    }

    static PlanDraft.DraftExercise du(long id, int series, int segundos, int descanso) {
        return new PlanDraft.DraftExercise(id, PrescriptionType.DURATION, (short) series, null, segundos, null,
                (short) descanso, null);
    }

    /** Sabado identico en los intentos 2 y 3; el 1 lleva 1325 en lugar de 136. */
    static List<PlanDraft.DraftExercise> sabadoIa(boolean intento1) {
        return List.of(sr(548, 2, 8, 120), sr(436, 2, 15, 45), intento1 ? du(1325, 3, 90, 60) : sr(136, 2, 8, 120),
                du(1137, 2, 30, 15), sr(341, 2, 10, 90), sr(393, 2, 10, 90), sr(1052, 2, 10, 90), sr(727, 2, 10, 90));
    }

    static PlanDraft ia(int intento) {
        var domingo = switch (intento) {
            case 1 -> List.of(sr(791, 2, 8, 120), sr(1254, 2, 15, 45), du(1325, 3, 90, 60), du(1150, 2, 30, 15),
                    sr(1003, 2, 10, 90), sr(1015, 2, 13, 90), sr(1185, 2, 13, 90), du(94, 2, 30, 30));
            case 2 -> List.of(sr(791, 2, 8, 120), sr(1254, 2, 15, 45), du(1325, 3, 90, 60), du(1150, 2, 30, 15),
                    sr(1003, 2, 10, 90), sr(1015, 2, 13, 90), sr(1004, 2, 10, 90), du(94, 2, 30, 30));
            default -> List.of(sr(309, 2, 8, 120), sr(1254, 2, 15, 45), du(1325, 3, 90, 60), du(1150, 2, 30, 15),
                    sr(1003, 2, 10, 90), sr(505, 2, 13, 90), sr(1004, 2, 10, 90), du(1255, 2, 30, 15));
        };
        return new PlanDraft(GenerationSource.AI, "replay", "intento " + intento, intento == 1 ? 268 : 269, "r", List.of(
                new PlanDraft.DraftWorkout(SABADO, WorkoutFocus.UPPER_BODY, "sabado", 32, sabadoIa(intento == 1)),
                new PlanDraft.DraftWorkout(DOMINGO, WorkoutFocus.LOWER_BODY, "domingo", 36, domingo)));
    }

    /** El plan 63 tal como se publico (dosis y minutos del registro). */
    static PlanDraft plan63() {
        var sabado = List.of(sr(301, 2, 8, 120), sr(776, 2, 8, 120), sr(992, 2, 8, 120), sr(135, 2, 8, 120),
                sr(1320, 2, 15, 45), du(1325, 3, 90, 60), du(456, 2, 30, 15));
        var domingo = List.of(sr(144, 2, 10, 90), sr(1103, 2, 13, 90), sr(1004, 2, 10, 90), sr(294, 2, 13, 90),
                du(1077, 3, 40, 45), sr(985, 2, 15, 45), sr(139, 2, 15, 45), du(1178, 2, 30, 15));
        return new PlanDraft(GenerationSource.RULE_ENGINE, null, "plan 63", null, "r", List.of(
                new PlanDraft.DraftWorkout(SABADO, WorkoutFocus.UPPER_BODY, "Tren superior", 32, sabado),
                new PlanDraft.DraftWorkout(DOMINGO, WorkoutFocus.LOWER_BODY, "Tren inferior", 32, domingo)));
    }

    private String problemas(PlanDraft draft, PlanGenerationContext ctx) {
        try {
            validador.validate(draft, ctx, 30);
            return "VALIDA";
        } catch (InvalidPlanDraftException e) {
            return String.join(" ; ", e.problems().stream().map(p -> p.length() > 160 ? p.substring(0, 160) : p)
                    .sorted().toList());
        }
    }

    private String elegibles(PlanGenerationContext ctx, long... ids) {
        var dentro = ctx.availableExercises().stream().map(c -> c.exerciseId()).collect(java.util.stream.Collectors.toSet());
        var sb = new StringBuilder();
        for (long id : ids) sb.append(id).append(dentro.contains(id) ? "=si " : "=NO ");
        return sb.toString().trim();
    }

    private String generar(long u) {
        long plan = planning.generateWeeklyPlan(u, LUNES, PlanAdjustment.none(), null, null).orElse(-1L);
        if (plan < 0) return "SIN_PLAN";
        var sb = new StringBuilder();
        for (var f : jdbc.queryForList("SELECT pw.planned_workout_id, pw.scheduled_date, pw.focus_code"
                + " FROM planned_workouts pw WHERE pw.plan_id = ? ORDER BY pw.scheduled_date", plan)) {
            sb.append("| ").append(((java.sql.Date) f.get("scheduled_date")).toLocalDate().getDayOfWeek()).append(' ')
                    .append(f.get("focus_code"));
            for (var e : jdbc.queryForList("SELECT pwe.exercise_id, bp.code AS bp, e.movement_pattern AS mp"
                    + " FROM planned_workout_exercises pwe JOIN exercises e ON e.exercise_id = pwe.exercise_id"
                    + " JOIN body_parts bp ON bp.body_part_id = e.body_part_id"
                    + " WHERE pwe.planned_workout_id = ? ORDER BY pwe.exercise_order", f.get("planned_workout_id")))
                sb.append(' ').append(e.get("exercise_id")).append('[').append(e.get("bp")).append('/')
                        .append(e.get("mp")).append(']');
            sb.append(' ');
        }
        return sb.toString().trim();
    }

    private List<String> informe(String escenario) {
        long u = usuario(escenario + "@pruebas.invalid");
        var ctx = planes.prepare(GenerateWeeklyPlanCommand.firstPlan(u, LUNES)).context();
        var out = new ArrayList<String>();
        out.add(escenario + " elegibles: " + elegibles(ctx, 776, 1320, 1185, 309, 1103, 301));
        for (int i = 1; i <= 3; i++) out.add(escenario + " intento " + i + ": " + problemas(ia(i), ctx));
        out.add(escenario + " plan 63: " + problemas(plan63(), ctx));
        out.add(escenario + " RULE_ENGINE: " + generar(u));
        out.forEach(System.out::println);
        return out;
    }

    static void contiene(String texto, String... partes) {
        for (var parte : partes) assertTrue(texto.contains(parte), "falta \"" + parte + "\" en: " + texto);
    }

    /** Los rechazos de la IA no dependen de estos ejercicios: se repiten igual con y sin correcciones. */
    static void rechazosDeLaIa(List<String> r, int lowerHip) {
        contiene(r.get(1), "V18: el ejercicio 1325 se repite en dias consecutivos",
                "V23: la semana no cubre LOWER_HIP", "LOWER_HIP=" + lowerHip + " elegibl");
        assertFalse(r.get(1).contains("V15"), r.get(1));
        contiene(r.get(2), "V15: el entrenamiento del " + DOMINGO + " dedica 4 de 6 ejercicios a upper legs");
        assertEquals(1, r.get(2).split(" ; ").length, r.get(2));
        contiene(r.get(3), "V15: el entrenamiento del " + DOMINGO + " dedica 3 de 5 ejercicios a upper legs");
        assertEquals(1, r.get(3).split(" ; ").length, r.get(3));
    }

    @Test
    @Order(1)
    @DisplayName("catalogo actual: se reproducen los tres rechazos, el plan 63 es valido y 776 y 1320 siguen elegibles")
    void catalogoActual() {
        var r = informe("actual");
        assertEquals("actual elegibles: 776=si 1320=si 1185=si 309=si 1103=si 301=si", r.get(0));
        rechazosDeLaIa(r, 13);
        assertEquals("actual plan 63: VALIDA", r.get(4));
        contiene(r.get(5), " 776[back/HIP_HINGE]", " 1320[upper arms/HORIZONTAL_PUSH]");
    }

    @Test
    @Order(2)
    @DisplayName("catalogo corregido: mismos rechazos de la IA; el plan 63 ya no es publicable; el motor sigue publicando")
    void catalogoCorregido() {
        // 776: GIF = suspension invertida en anillas.  1320: sentado en el suelo.
        // 1103: sentadilla (upper legs/quads).  1185: brazos por encima de la cabeza.
        // 309: una rodilla apoyada en el banco.
        jdbc.update("UPDATE exercises SET requires_hanging = true, requires_overhead = true,"
                + " requires_inversion = true, difficulty_level = 3 WHERE exercise_id = 776");
        jdbc.update("UPDATE exercises SET requires_floor = true WHERE exercise_id = 1320");
        jdbc.update("UPDATE exercises SET body_part_id = (SELECT body_part_id FROM body_parts WHERE code = 'upper legs'),"
                + " target_muscle = 'quads' WHERE exercise_id = 1103");
        jdbc.update("UPDATE exercises SET requires_overhead = true WHERE exercise_id = 1185");
        jdbc.update("UPDATE exercises SET requires_kneeling = true WHERE exercise_id = 309");
        var r = informe("corregido");
        assertEquals("corregido elegibles: 776=NO 1320=NO 1185=si 309=si 1103=si 301=si", r.get(0));
        rechazosDeLaIa(r, 12);
        contiene(r.get(4), "V1: el ejercicio 776 no esta en available_exercises",
                "V1: el ejercicio 1320 no esta en available_exercises",
                "V15: el entrenamiento del " + DOMINGO + " dedica 4 de 7 ejercicios a upper legs");
        assertTrue(r.get(5).startsWith("corregido RULE_ENGINE: | SATURDAY UPPER_BODY"), r.get(5));
        assertFalse(r.get(5).contains(" 776[") || r.get(5).contains(" 1320["), r.get(5));
    }
}
