package main.web.services.fitsense.planning;

import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.MandatorySlot;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.domain.services.CapabilityPrescriptionPolicy;
import main.web.services.fitsense.planning.domain.services.CapabilityPrescriptionPolicy.Clase;
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
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * V41 (auditoria integral del 30/10/2026): segunda fase de C4/C6. Quince ejercicios con el grupo,
 * el patron y el slot coherentes con su movimiento predominante, sin tocar dificultad,
 * equipamiento ni dosis; y en los perfiles de gimnasio intermedio y avanzado ningun ejercicio
 * acredita un slot de pierna con un grupo que no es de pierna (262 lo hacia en 11 referencias).
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "spring.flyway.target=41"
})
@DisplayName("V41: coherencia grupo, patron y slot, segunda fase (C4/C6)")
class CatalogoV41IT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired PlanningContextFacade planning;
    @Autowired WeeklyTrainingPlanCommandService planComandos;
    @Autowired UserProfileCommandService perfiles;
    @Autowired JdbcTemplate jdbc;

    static final LocalDate S1 = TrainingWeek.containing(LocalDate.now()).startDate().plusWeeks(1);
    static final List<String> GYM = List.of("body weight", "dumbbell", "barbell", "cable", "leverage machine",
            "bench", "band", "stationary bike", "treadmill", "kettlebell", "ez barbell", "smith machine");
    private static int secuencia;

    /** id, grupo, musculo, patron, dificultad y requisitos esperados tras V40 (los dos ultimos, los de V39). */
    static final List<String[]> ESPERADO = List.of(
            new String[]{"262", "shoulders", "delts", "SHOULDER_EXTENSION", "3", "1:barbell"},
            new String[]{"267", "upper legs", "quads", "KNEE_DOMINANT", "3", "1:barbell"},
            new String[]{"952", "upper legs", "quads", "KNEE_DOMINANT", "3", "1:kettlebell"},
            new String[]{"796", "upper legs", "quads", "KNEE_DOMINANT", "3", "1:dumbbell,2:plyo box,2:step platform"},
            new String[]{"301", "back", "upper back", "HORIZONTAL_PULL", "1", "1:body weight,2:wall bars"},
            new String[]{"302", "back", "upper back", "HORIZONTAL_PULL", "1", "1:body weight,2:wall bars"},
            new String[]{"671", "chest", "pectorals", "SHOULDER_EXTENSION", "2", "1:dumbbell,2:stability ball"},
            new String[]{"824", "upper legs", "glutes", "HIP_EXTENSION", "1", "1:stability ball"},
            new String[]{"922", "lower arms", "forearms", "ELBOW_FLEXION", "3", "1:kettlebell"},
            new String[]{"930", "upper arms", "biceps", "ELBOW_FLEXION", "3", "1:kettlebell"},
            new String[]{"971", "upper arms", "biceps", "ELBOW_FLEXION", "3", "1:kettlebell"},
            new String[]{"674", "upper legs", "glutes", "HIP_HINGE", "3", "1:dumbbell"},
            new String[]{"175", "upper legs", "glutes", "HIP_HINGE", "3", "1:barbell"},
            new String[]{"942", "upper legs", "glutes", "HIP_HINGE", "3", "1:kettlebell"},
            new String[]{"947", "upper legs", "glutes", "HIP_HINGE", "3", "1:kettlebell"},
            // Se mantiene: 162 (excepcion documentada, cargada + press estricto).
            new String[]{"162", "upper legs", "quads", "VERTICAL_PUSH", "3", "1:barbell"});

    @Test
    @DisplayName("1 · atributos: solo grupo, musculo y patron cambian; dificultad y equipamiento como en V39")
    void atributos() {
        for (var e : ESPERADO) {
            var f = jdbc.queryForMap("""
                    SELECT bp.code AS grupo, e.target_muscle AS musculo, e.movement_pattern AS patron,
                           e.difficulty_level AS dificultad,
                           (SELECT string_agg(r.group_no || ':' || q.code, ',' ORDER BY r.group_no, q.code)
                              FROM exercise_equipment_requirements r JOIN equipment_types q
                                ON q.equipment_id = r.equipment_id WHERE r.exercise_id = e.exercise_id) AS req
                      FROM exercises e JOIN body_parts bp ON bp.body_part_id = e.body_part_id
                     WHERE e.exercise_id = ? AND e.is_active""", Long.parseLong(e[0]));
            assertEquals(List.of(e[1], e[2], e[3], e[4], e[5]), List.of(f.get("grupo"), f.get("musculo"),
                    f.get("patron"), String.valueOf(f.get("dificultad")), f.get("req")), e[0]);
        }
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM exercise_difficulty_approvals WHERE exercise_id IN"
                + " (262, 267, 952, 796, 301, 302, 671, 824, 922, 930, 971, 674, 175, 942, 947)", Integer.class), "sin aprobaciones de dificultad nuevas");
    }

    @Test
    @DisplayName("2 · clase y slot: 262 y los hang cleans dejan de acreditar cadera; remos y arrancadas, el suyo")
    void claseYSlot() {
        long u = participante(GoalType.INCREASE_STRENGTH, FitnessLevel.ADVANCED, 3);
        Map<Long, CandidateExercise> el = planComandos.prepare(new GenerateWeeklyPlanCommand(u, S1, null, true, null, null))
                .context().availableExercises().stream()
                .collect(Collectors.toMap(CandidateExercise::exerciseId, Function.identity(), (a, b) -> a));
        for (long id : List.of(262L, 930L, 971L)) {
            if (el.get(id) == null) continue;
            assertEquals(Clase.RESISTIDO, CapabilityPrescriptionPolicy.claseDe(el.get(id)), String.valueOf(id));
            assertTrue(MandatorySlot.of(el.get(id)).isEmpty(), id + " no acredita slot");
        }
        assertNotNull(el.get(262L), "262 elegible para avanzado de gimnasio");
        for (long id : List.of(674L, 175L, 942L, 947L)) {
            if (el.get(id) == null) continue;
            assertEquals(Optional.of(MandatorySlot.LOWER_HIP), MandatorySlot.of(el.get(id)), String.valueOf(id));
            assertEquals("upper legs", el.get(id).bodyPartCode(), String.valueOf(id));
        }
        for (long id : List.of(267L, 952L)) {
            if (el.get(id) == null) continue;
            assertEquals(Optional.of(MandatorySlot.LOWER_KNEE), MandatorySlot.of(el.get(id)), String.valueOf(id));
            assertEquals("upper legs", el.get(id).bodyPartCode(), String.valueOf(id));
        }
    }

    @Test
    @DisplayName("3 · planes de los perfiles que cambian: ningun slot de pierna lo acredita un grupo que no es de pierna")
    void sinCrucesDePierna() {
        var fallos = new ArrayList<String>();
        var con262 = new ArrayList<String>();
        for (var objetivo : GoalType.values())
            for (var nivel : List.of(FitnessLevel.INTERMEDIATE, FitnessLevel.ADVANCED)) {
                long u = participante(objetivo, nivel, 3);
                long plan = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null)
                        .orElseThrow(() -> new AssertionError("sin plan " + objetivo + "/" + nivel));
                var cruzados = jdbc.queryForList("""
                        SELECT e.exercise_id || ' ' || bp.code || '/' || e.movement_pattern
                          FROM planned_workouts w
                          JOIN planned_workout_exercises x ON x.planned_workout_id = w.planned_workout_id
                          JOIN exercises e ON e.exercise_id = x.exercise_id
                          JOIN body_parts bp ON bp.body_part_id = e.body_part_id
                         WHERE w.plan_id = ? AND bp.code NOT IN ('upper legs', 'lower legs')
                           AND e.movement_pattern IN ('KNEE_DOMINANT', 'HIP_HINGE', 'HIP_EXTENSION')""",
                        String.class, plan);
                if (!cruzados.isEmpty()) fallos.add(objetivo + "/" + nivel + ": " + cruzados);
                if (jdbc.queryForObject("SELECT count(*) FROM planned_workouts w JOIN planned_workout_exercises x"
                        + " ON x.planned_workout_id = w.planned_workout_id WHERE w.plan_id = ? AND x.exercise_id = 262",
                        Integer.class, plan) > 0) con262.add(objetivo + "/" + nivel);
            }
        System.out.println("V41: planes con 262 (ahora SHOULDER_EXTENSION, sin slot): " + con262);
        assertEquals(List.of(), fallos);
    }

    private long participante(GoalType objetivo, FitnessLevel nivel, int dias) {
        String email = "v41-" + (++secuencia) + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Prueba', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1992, 1, 15), BiologicalSex.FEMALE,
                new BigDecimal("165"), new BigDecimal("70"), null, nivel, objetivo, "v41", TrainingLocation.GYM,
                (short) dias, List.of((short) 1, (short) 3, (short) 5), (short) 45, GYM, List.of(), "Ninguna",
                true, true, true, true, true));
        return u;
    }
}
