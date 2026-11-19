package main.web.services.fitsense.p2d;

import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
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
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * INC-CAT-C39 · COMPARACION ANTES/DESPUES (opt-in, solo evidencia). Genera la misma rejilla de perfiles con el motor
 * de reglas y escribe, por perfil, el conjunto elegible, el desenlace y los ejercicios de la semana. No referencia
 * ninguna clase de la contencion: se ejecuta igual sobre {@code generation} (antes) y sobre la rama (despues), y la
 * comparacion de los dos CSV dice que cambio y por que. La semana es fija: con usuarios creados en el mismo orden, la
 * semilla del motor de reglas coincide entre las dos ejecuciones.
 * <p>
 * {@code -Dfitsense.c39.comparacion=true -Dfitsense.c39.salida=<csv>}
 */
@Testcontainers
@EnabledIfSystemProperty(named = "fitsense.c39.comparacion", matches = "true")
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
@DisplayName("INC-CAT-C39 · comparacion antes/despues sobre una rejilla de perfiles (opt-in)")
class ContencionC39ComparacionIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired PlanningContextFacade planning;
    @Autowired WeeklyTrainingPlanCommandService planComandos;
    @Autowired UserProfileCommandService perfiles;
    @Autowired JdbcTemplate jdbc;

    static final LocalDate SEMANA = LocalDate.of(2026, 11, 16);
    static final List<String> GIMNASIO = List.of("assisted", "band", "barbell", "bench", "body weight", "cable",
            "captains chair", "dumbbell", "ez barbell", "ghd bench", "gymnastic rings", "hyperextension bench",
            "landmine", "leverage machine", "olympic barbell", "other", "parallel bars", "preacher bench",
            "pull-up bar", "rope", "sled machine", "smith machine", "squat rack", "stationary bike", "step platform",
            "suspension straps", "treadmill", "vertical pole", "weighted");
    static final List<String> CASA = List.of("body weight", "dumbbell", "band");

    /** nombre, sinSuelo, sinColgarse, sinBrazosArriba */
    static final Object[][] RESTRICCIONES = {
            {"LIBRE", false, false, false}, {"SIN_COLGARSE", false, true, false}, {"SIN_CABEZA", false, false, true},
            {"SIN_SUELO", true, false, false}, {"SIN_TRES", true, true, true}};

    @Test
    void rejilla() throws Exception {
        var sb = new StringBuilder("clave;elegibles;desenlace;ejercicios\n");
        long id = 5000;
        for (var objetivo : GoalType.values())
            for (var nivel : FitnessLevel.values())
                for (var lugar : List.of(TrainingLocation.GYM, TrainingLocation.HOME))
                    for (var r : RESTRICCIONES) {
                        long u = ++id;
                        jdbc.update("INSERT INTO users (user_id, email, password_hash, first_name, consent_granted_at,"
                                + " enrolled_at) OVERRIDING SYSTEM VALUE VALUES (?, ?, 'x', 'C39', now(), now())",
                                u, "cmp" + u + "@pruebas.invalid");
                        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1985, 6, 1), BiologicalSex.MALE,
                                new BigDecimal("180"), new BigDecimal("100"), new BigDecimal("80"), nivel, objetivo,
                                "comparacion", lugar, (short) 4, List.of((short) 1, (short) 4, (short) 5, (short) 7),
                                (short) 60, lugar == TrainingLocation.GYM ? GIMNASIO : CASA, List.of(), null, true,
                                !(boolean) r[1], true, !(boolean) r[2], !(boolean) r[3]));
                        String clave = objetivo + "/" + nivel + "/" + lugar + "/" + r[0];
                        String elegibles;
                        try {
                            elegibles = planComandos.prepare(GenerateWeeklyPlanCommand.firstPlan(u, SEMANA)).context()
                                    .availableExercises().stream().map(c -> String.valueOf(c.exerciseId()))
                                    .sorted().collect(Collectors.joining(","));
                        } catch (RuntimeException e) {
                            elegibles = "ERROR:" + e.getClass().getSimpleName();
                        }
                        String desenlace, ejercicios = "";
                        try {
                            long plan = planning.generateWeeklyPlan(u, SEMANA, PlanAdjustment.none(), null, null)
                                    .orElseThrow();
                            desenlace = "PLAN";
                            ejercicios = jdbc.queryForList("SELECT pw.scheduled_date || ':' || pwe.exercise_order || ':'"
                                    + " || pwe.exercise_id || ':' || coalesce(pwe.planned_sets,0) || 'x' ||"
                                    + " coalesce(pwe.planned_reps,0) || '/' || coalesce(pwe.planned_duration_seconds,0)"
                                    + " FROM planned_workout_exercises pwe JOIN planned_workouts pw ON"
                                    + " pw.planned_workout_id = pwe.planned_workout_id WHERE pw.plan_id = ?"
                                    + " ORDER BY pw.scheduled_date, pwe.exercise_order", String.class, plan)
                                    .stream().collect(Collectors.joining(" "));
                        } catch (RuntimeException e) {
                            desenlace = "SIN_PLAN:" + e.getClass().getSimpleName();
                        }
                        sb.append(clave).append(';').append(elegibles).append(';').append(desenlace).append(';')
                                .append(ejercicios).append('\n');
                    }
        var salida = Path.of(System.getProperty("fitsense.c39.salida", "target/c39-comparacion.csv"));
        Files.createDirectories(salida.toAbsolutePath().getParent());
        Files.writeString(salida, sb.toString(), StandardCharsets.UTF_8);
    }
}
