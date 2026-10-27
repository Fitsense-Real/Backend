package main.web.services.fitsense.planning;

import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.domain.services.PlanDraftValidator;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
import main.web.services.fitsense.planning.interfaces.acl.PlanningContextFacade;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.BiologicalSex;
import main.web.services.fitsense.profiling.domain.model.valueobjects.FitnessLevel;
import main.web.services.fitsense.profiling.domain.model.valueobjects.GoalType;
import main.web.services.fitsense.profiling.domain.model.valueobjects.TrainingLocation;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import main.web.services.fitsense.shared.domain.model.valueobjects.TrainingWeek;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * PROTECCION DE LA GENERACION INICIAL. Los planes iniciales de los 72 perfiles
 * de referencia (4 objetivos x 3 niveles x 30/45/60 min x gimnasio/casa) deben
 * ser identicos -ejercicios, dosis, estructura y resultado de la validacion de
 * la generacion- a los guardados en
 * {@code docs/FitSense/evidencia/progresion/generacion_referencia.txt}, que se
 * capturaron ANTES de implementar la progresion.
 * <p>
 * Para regenerar la referencia (solo si se aprueba un cambio de la generacion):
 * {@code -Dfitsense.generacion.capturar=true}.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
class GeneracionReferenciaIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired PlanningContextFacade planning;
    @Autowired WeeklyTrainingPlanCommandService planComandos;
    @Autowired UserProfileCommandService perfiles;
    @Autowired PlanDraftValidator validador;
    @Autowired JdbcTemplate jdbc;

    static final Path REFERENCIA = Path.of("docs/FitSense/evidencia/progresion/generacion_referencia.txt");
    static final List<String> GIMNASIO = List.of("body weight", "dumbbell", "barbell", "cable", "leverage machine",
            "bench", "band", "stationary bike", "treadmill", "kettlebell", "ez barbell", "smith machine");
    static final List<String> CASA = List.of("body weight", "dumbbell", "band");

    @Test
    @DisplayName("los planes iniciales de los 72 perfiles de referencia no cambian")
    void generacionIntacta() throws Exception {
        var semana = TrainingWeek.containing(LocalDate.now()).startDate().plusWeeks(1);
        var lineas = new ArrayList<String>();
        int n = 0;
        for (var objetivo : GoalType.values())
            for (var nivel : FitnessLevel.values())
                for (int minutos : new int[]{30, 45, 60})
                    for (var lugar : List.of("GYM", "CASA")) {
                        n++;
                        String perfil = objetivo + "/" + nivel + "/" + minutos + "/" + lugar;
                        lineas.add(describir(perfil, n, objetivo, nivel, minutos, lugar, semana));
                    }
        String actual = String.join("\n", lineas) + "\n";
        if (Boolean.getBoolean("fitsense.generacion.capturar")) {
            Files.createDirectories(REFERENCIA.getParent());
            Files.writeString(REFERENCIA, actual);
            return;
        }
        var esperado = Files.readAllLines(REFERENCIA);
        var obtenido = actual.lines().toList();
        assertEquals(esperado.size(), obtenido.size(), "numero de perfiles");
        for (int i = 0; i < esperado.size(); i++)
            assertEquals(esperado.get(i), obtenido.get(i), "el plan inicial cambio: " + esperado.get(i).split(" ")[0]);
    }

    /** Una linea por perfil: dias, enfoque, minutos y cada ejercicio con su dosis, y la validacion. */
    private String describir(String perfil, int n, GoalType objetivo, FitnessLevel nivel, int minutos, String lugar,
                             LocalDate semana) {
        String email = "referencia" + n + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Prueba', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1992, 1, 15), BiologicalSex.FEMALE,
                new BigDecimal("165"), new BigDecimal("70"), null, nivel, objetivo, "referencia",
                "GYM".equals(lugar) ? TrainingLocation.GYM : TrainingLocation.HOME, (short) 3,
                List.of((short) 1, (short) 3, (short) 5), (short) minutos, "GYM".equals(lugar) ? GIMNASIO : CASA,
                List.of(), "Ninguna", true, true, true, true, true));
        var sb = new StringBuilder(perfil).append(" ");
        long plan;
        try {
            plan = planning.generateWeeklyPlan(u, semana, PlanAdjustment.none(), null, null).orElse(-1L);
        } catch (RuntimeException e) {
            return sb.append("SIN_PLAN ").append(e.getClass().getSimpleName()).toString();
        }
        if (plan < 0) return sb.append("SIN_PLAN").toString();
        var sesiones = new ArrayList<PlanDraft.DraftWorkout>();
        for (var f : jdbc.queryForList("SELECT planned_workout_id, scheduled_date, focus_code, workout_name,"
                + " expected_duration_minutes FROM planned_workouts WHERE plan_id = ? ORDER BY scheduled_date", plan)) {
            long id = ((Number) f.get("planned_workout_id")).longValue();
            var fecha = ((java.sql.Date) f.get("scheduled_date")).toLocalDate();
            sb.append("| ").append(fecha.getDayOfWeek()).append(' ').append(f.get("focus_code")).append(' ')
                    .append(f.get("expected_duration_minutes")).append("min");
            var ejercicios = jdbc.query("SELECT exercise_id, prescription_type, planned_sets, planned_reps,"
                            + " planned_duration_seconds, target_load_kg, rest_seconds FROM planned_workout_exercises"
                            + " WHERE planned_workout_id = ? ORDER BY exercise_order",
                    (rs, k) -> new PlanDraft.DraftExercise(rs.getLong(1), PrescriptionType.valueOf(rs.getString(2)),
                            rs.getObject(3, Short.class), rs.getObject(4, Short.class), rs.getObject(5, Integer.class),
                            rs.getBigDecimal(6), rs.getObject(7, Short.class), null), id);
            for (var e : ejercicios)
                sb.append(' ').append(e.exerciseId()).append(':').append(e.plannedSets()).append('x')
                        .append(e.plannedReps() != null ? e.plannedReps() + "r" : e.plannedDurationSeconds() + "s")
                        .append('/').append(e.restSeconds());
            sesiones.add(new PlanDraft.DraftWorkout(fecha, WorkoutFocus.valueOf((String) f.get("focus_code")),
                    (String) f.get("workout_name"), ((Number) f.get("expected_duration_minutes")).intValue(), ejercicios));
        }
        var ctx = planComandos.prepare(new GenerateWeeklyPlanCommand(u, semana, null, true, null, null)).context();
        try {
            validador.validate(new PlanDraft(GenerationSource.RULE_ENGINE, null, "referencia", null, "r", sesiones),
                    ctx, 30);
            sb.append(" || VALIDA");
        } catch (InvalidPlanDraftException e) {
            sb.append(" || ").append(String.join(" ; ", e.problems().stream().sorted().toList()));
        }
        return sb.toString();
    }
}
