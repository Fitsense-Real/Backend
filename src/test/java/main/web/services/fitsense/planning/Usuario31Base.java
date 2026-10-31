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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Usuario 31 (29/10/2026): tres propuestas de Replicate rechazadas y plan 63 por
 * RULE_ENGINE. Se reproducen SIN llamar a Replicate, con las respuestas guardadas
 * en el registro y el perfil exacto (sin relajar ninguna restriccion), por el
 * validador de produccion. Cada subclase fija el catalogo:
 * {@link Usuario31ReproduccionIT} (V36, evidencia historica) y {@link Usuario31V37IT} (vigente).
 * Las fechas se desplazan a una semana futura; el validador no depende de la
 * fecha absoluta, solo de los dias (sabado y domingo consecutivos).
 */
abstract class Usuario31Base {


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

    long usuario(String email) {
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

    String problemas(PlanDraft draft, PlanGenerationContext ctx) {
        try {
            validador.validate(draft, ctx, 30);
            return "VALIDA";
        } catch (InvalidPlanDraftException e) {
            return String.join(" ; ", e.problems().stream().map(p -> p.length() > 160 ? p.substring(0, 160) : p)
                    .sorted().toList());
        }
    }

    String elegibles(PlanGenerationContext ctx, long... ids) {
        var dentro = ctx.availableExercises().stream().map(c -> c.exerciseId()).collect(java.util.stream.Collectors.toSet());
        var sb = new StringBuilder();
        for (long id : ids) sb.append(id).append(dentro.contains(id) ? "=si " : "=NO ");
        return sb.toString().trim();
    }

    String generar(long u) {
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

    List<String> informe(String escenario) {
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
        // Desde V20 exacto (IMPLEMENTACION_V20.md) el sabado de los intentos 2 y 3, 1.866 s (69,1 %),
        // tambien se rechaza: antes pasaba solo por el redondeo a 31 minutos.
        contiene(r.get(2), "V15: el entrenamiento del " + DOMINGO + " dedica 4 de 6 ejercicios a upper legs",
                "V20: el entrenamiento del " + SABADO + " dura unos 1866 s y el minimo es 1890 s");
        assertEquals(2, r.get(2).split(" ; ").length, r.get(2));
        contiene(r.get(3), "V15: el entrenamiento del " + DOMINGO + " dedica 3 de 5 ejercicios a upper legs",
                "V20: el entrenamiento del " + SABADO + " dura unos 1866 s y el minimo es 1890 s");
        assertEquals(2, r.get(3).split(" ; ").length, r.get(3));
    }
}
