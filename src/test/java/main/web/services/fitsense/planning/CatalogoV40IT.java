package main.web.services.fitsense.planning;

import main.web.services.fitsense.EsquemaVigenteSobreCatalogoAnterior;
import org.springframework.context.annotation.Import;
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
 * V40 (decision C4/C6 del 30/10/2026, DIAGNOSTICO_C4_C6.md): siete ejercicios con el grupo, el
 * patron y el slot coherentes con su movimiento predominante, sin tocar dificultad, equipamiento
 * ni dosis; y la consecuencia que se buscaba: ningun ejercicio de tronco acredita un slot de
 * fuerza en los planes de los perfiles que cambian.
 */
@Testcontainers
@Import(EsquemaVigenteSobreCatalogoAnterior.class)
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        // Evidencia del catalogo V40: V41 cambia la familia de arrancadas (674) y otros cruces.
        "spring.flyway.target=40"
})
@DisplayName("V40: coherencia grupo, patron y slot (C4/C6)")
class CatalogoV40IT {

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
            new String[]{"1225", "waist", "abs", "TRUNK_FLEXION", "2", "1:smith machine"},
            new String[]{"1223", "chest", "serratus anterior", "SHOULDER_RAISE", "2", "1:smith machine,2:bench"},
            new String[]{"439", "upper legs", "quads", "KNEE_DOMINANT", "2", "1:cable"},
            new String[]{"959", "waist", "abs", "TRUNK_ROTATION_LATERAL_FLEXION", "3", "1:kettlebell"},
            new String[]{"962", "upper legs", "glutes", "KNEE_DOMINANT", "3", "1:kettlebell"},
            new String[]{"927", "waist", "abs", "TRUNK_ROTATION_LATERAL_FLEXION", "3", "1:kettlebell"},
            new String[]{"953", "upper legs", "glutes", "HIP_HINGE", "3", "1:kettlebell"},
            // Se mantienen (decision): 674 y 162.
            new String[]{"674", "upper legs", "glutes", "VERTICAL_PUSH", "3", "1:dumbbell"},
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
                + " (1225, 1223, 439, 959, 962, 927, 953)", Integer.class), "sin aprobaciones de dificultad nuevas");
    }

    @Test
    @DisplayName("2 · clase y slot: los de tronco ya no acreditan fuerza ni slot; los de pierna, el suyo")
    void claseYSlot() {
        long u = participante(GoalType.INCREASE_STRENGTH, FitnessLevel.ADVANCED, 3);
        Map<Long, CandidateExercise> el = planComandos.prepare(new GenerateWeeklyPlanCommand(u, S1, null, true, null, null))
                .context().availableExercises().stream()
                .collect(Collectors.toMap(CandidateExercise::exerciseId, Function.identity(), (a, b) -> a));
        for (long id : List.of(1225L, 1223L, 959L, 927L)) {
            assertNotNull(el.get(id), id + " elegible para avanzado de gimnasio");
            assertEquals(Clase.RESISTIDO, CapabilityPrescriptionPolicy.claseDe(el.get(id)), String.valueOf(id));
            assertTrue(MandatorySlot.of(el.get(id)).isEmpty(), id + " no acredita slot");
        }
        assertEquals(Optional.of(MandatorySlot.LOWER_KNEE), MandatorySlot.of(el.get(439L)));
        assertEquals(Optional.of(MandatorySlot.LOWER_KNEE), MandatorySlot.of(el.get(962L)));
        assertEquals(Optional.of(MandatorySlot.LOWER_HIP), MandatorySlot.of(el.get(953L)));
        for (long id : List.of(439L, 962L, 953L)) {
            assertEquals("upper legs", el.get(id).bodyPartCode(), String.valueOf(id));
            assertEquals(Clase.ESTRUCTURAL, CapabilityPrescriptionPolicy.claseDe(el.get(id)), String.valueOf(id));
        }
    }

    @Test
    @DisplayName("3 · planes de los perfiles que cambian: ningun ejercicio de tronco acredita un slot de fuerza")
    void troncoSinSlotDeFuerza() {
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
                         WHERE w.plan_id = ? AND bp.code = 'waist'
                           AND e.movement_pattern IN ('KNEE_DOMINANT', 'HIP_HINGE', 'HIP_EXTENSION', 'HORIZONTAL_PUSH',
                                                      'VERTICAL_PUSH', 'HORIZONTAL_PULL', 'VERTICAL_PULL')""",
                        String.class, plan);
                if (!cruzados.isEmpty()) fallos.add(objetivo + "/" + nivel + ": " + cruzados);
                if (jdbc.queryForObject("SELECT count(*) FROM planned_workouts w JOIN planned_workout_exercises x"
                        + " ON x.planned_workout_id = w.planned_workout_id WHERE w.plan_id = ? AND x.exercise_id = 262",
                        Integer.class, plan) > 0) con262.add(objetivo + "/" + nivel);
            }
        // 262 (barbell skier, shoulders/HIP_HINGE) queda vigilado: se informa, no se corrige en V40.
        System.out.println("V40: planes con 262 (vigilado, sin corregir): " + con262);
        assertEquals(List.of(), fallos);
    }

    private long participante(GoalType objetivo, FitnessLevel nivel, int dias) {
        String email = "v40-" + (++secuencia) + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Prueba', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1992, 1, 15), BiologicalSex.FEMALE,
                new BigDecimal("165"), new BigDecimal("70"), null, nivel, objetivo, "v40", TrainingLocation.GYM,
                (short) dias, List.of((short) 1, (short) 3, (short) 5), (short) 45, GYM, List.of(), "Ninguna",
                true, true, true, true, true));
        return u;
    }
}
