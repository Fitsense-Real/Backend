package main.web.services.fitsense.planning;

import main.web.services.fitsense.planning.domain.exceptions.PlanGenerationFailedException;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.GoalCapabilityPolicy.Capability;
import main.web.services.fitsense.planning.domain.model.valueobjects.MandatorySlot;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanGenerationContext;
import main.web.services.fitsense.planning.domain.model.valueobjects.PrescriptionType;
import main.web.services.fitsense.planning.domain.services.CapabilityPrescriptionPolicy;
import main.web.services.fitsense.planning.domain.services.CapabilityPrescriptionPolicy.Clase;
import main.web.services.fitsense.planning.domain.services.PlanDraftValidator;
import main.web.services.fitsense.planning.domain.services.SessionDurationEstimator;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
import main.web.services.fitsense.planning.infrastructure.generation.rules.RuleBasedTrainingPlanGenerator;
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
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * V39 (la candidata V38 renumerada; decisiones D1-D7 y F del 30/10/2026) sobre V38 (la V37.1).
 * Comprueba los datos corregidos con las politicas de produccion (no con atributos sueltos),
 * que ningun plan contenga un ejercicio cuyo implemento SEGUN EL GIF no esta declarado, y que
 * los perfiles solo con peso corporal se generan o se declaran sin plan con su motivo, sin
 * inventar alternativas. Con {@code -Dfitsense.v39.dir} escribe el detalle.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
@DisplayName("V39: datos corregidos, equipamiento real y perfiles solo con peso corporal")
class CatalogoV39IT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired PlanningContextFacade planning;
    @Autowired WeeklyTrainingPlanCommandService planComandos;
    @Autowired RuleBasedTrainingPlanGenerator motor;
    @Autowired SessionDurationEstimator estimador;
    @Autowired UserProfileCommandService perfiles;
    @Autowired JdbcTemplate jdbc;

    static final LocalDate S1 = TrainingWeek.containing(LocalDate.now()).startDate().plusWeeks(1);
    static final List<String> CASA = List.of("body weight", "dumbbell", "band");
    static final List<String> SOLO_CUERPO = List.of("body weight");
    static final Set<Long> ESPALDERA = Set.of(301L, 302L, 303L, 304L, 305L, 306L, 307L, 308L, 813L);
    private static int secuencia;

    @Test
    @DisplayName("1 · 853 y 1117: cardio de bajo impacto, dosis vigente 3x90 s (V24) y sin suelo ni saltos")
    void cardio() {
        var ctx = contexto(GoalType.LOSE_WEIGHT, FitnessLevel.BEGINNER, 4, CASA);
        var el = elegibles(ctx);
        for (long id : List.of(853L, 1117L)) {
            var c = el.get(id);
            assertNotNull(c, id + " tiene que ser elegible para principiante en casa");
            assertEquals(Clase.CARDIO, CapabilityPrescriptionPolicy.claseDe(c), id + " ya no esta en revision");
            assertEquals(PrescriptionType.DURATION, c.defaultPrescription());
            var d = CapabilityPrescriptionPolicy.dosis(Capability.CARDIO, c, "LOSE_WEIGHT", "BEGINNER",
                    ctx.prescription().repLimits()).orElseThrow();
            assertEquals(3, d.series());
            assertEquals(90, d.segundos());
            assertEquals(60, d.descanso());
            assertEquals(Boolean.FALSE, jdbc.queryForObject("SELECT high_impact OR requires_floor FROM exercises"
                    + " WHERE exercise_id = ?", Boolean.class, id), id + " sin saltos ni suelo");
        }
        // D2: 298 y 854 siguen fuera.
        assertNotEquals(Clase.CARDIO, Optional.ofNullable(el.get(298L)).map(CapabilityPrescriptionPolicy::claseDe).orElse(Clase.EN_REVISION));
        assertFalse(el.containsKey(854L), "854 sigue marcado de alto impacto: fuera para principiantes");
    }

    @Test
    @DisplayName("2-3 · 1102 deja de ser fuerza de rodilla; flexiones y 1106 son empuje de pecho; 142, 558 y 567 son pierna")
    void clasificacion() {
        var el = elegibles(contexto(GoalType.GENERAL_FITNESS, FitnessLevel.ADVANCED, 4, CASA));
        var c1102 = el.get(1102L);
        assertEquals(Clase.MOVILIDAD, CapabilityPrescriptionPolicy.claseDe(c1102));
        assertTrue(MandatorySlot.of(c1102).isEmpty(), "1102 no cubre LOWER_KNEE");
        for (long id : List.of(1106L, 1111L, 1113L, 1115L, 1262L)) {
            var c = el.get(id);
            assertEquals("chest", c.bodyPartCode(), String.valueOf(id));
            assertEquals(Optional.of(MandatorySlot.UPPER_PUSH), MandatorySlot.of(c), String.valueOf(id));
        }
        assertEquals(PrescriptionType.SETS_REPS, el.get(1106L).defaultPrescription(), "1106 es dinamico");
        assertEquals(2, el.get(1106L).difficulty(), "1106: nivel 2 PROVISIONAL (D7)");
        assertEquals(Optional.of(MandatorySlot.LOWER_HIP), MandatorySlot.of(el.get(142L)));
        assertEquals("upper legs", el.get(142L).bodyPartCode());
        // 558 y 567 son de mancuerna y nivel 2: el avanzado de casa los tiene.
        assertEquals("upper legs", el.get(558L).bodyPartCode());
        assertEquals("upper legs", el.get(567L).bodyPartCode());
        // Principiante: 1106 (nivel 2) ya no le llega.
        assertFalse(elegibles(contexto(GoalType.GENERAL_FITNESS, FitnessLevel.BEGINNER, 4, CASA)).containsKey(1106L));
    }

    @Test
    @DisplayName("4-5 · equipamiento real: sin espaldera no hay remos de espaldera; con ella, si; 1253 exige escalon")
    void equipamiento() {
        var casa = elegibles(contexto(GoalType.GENERAL_FITNESS, FitnessLevel.ADVANCED, 4, CASA));
        assertTrue(Collections.disjoint(casa.keySet(), ESPALDERA), "casa sin espaldera");
        assertFalse(casa.containsKey(1072L), "sin punto de anclaje");
        assertFalse(casa.containsKey(880L), "sin banco GHD");
        assertFalse(casa.containsKey(1253L), "sin escalon declarado");
        var conEspaldera = new ArrayList<>(CASA);
        conEspaldera.addAll(List.of("wall bars", "step platform"));
        var conEl = elegibles(contexto(GoalType.GENERAL_FITNESS, FitnessLevel.ADVANCED, 4, conEspaldera));
        assertTrue(conEl.keySet().containsAll(Set.of(301L, 304L, 307L, 1253L)), "declarando espaldera y step vuelven");
        // Tirones de casa estandar (banda): siguen existiendo.
        assertTrue(casa.values().stream().anyMatch(c -> MandatorySlot.of(c).equals(Optional.of(MandatorySlot.UPPER_PULL))));
    }

    @Test
    @DisplayName("6-8 · solo peso corporal (4 objetivos x 3 niveles): sin tiron superior; plan o motivo, nunca un implemento ausente")
    void soloPesoCorporal() throws Exception {
        var sb = new StringBuilder("perfil\tupper_pull\tlower_hip\tlower_knee\tupper_push\tresultado\n");
        var fallos = new ArrayList<String>();
        for (var objetivo : GoalType.values())
            for (var nivel : FitnessLevel.values()) {
                String perfil = objetivo + "/" + nivel + "/3d/45/SOLO_PESO_CORPORAL";
                long u = participante(objetivo, nivel, 3, SOLO_CUERPO);
                var ctx = planComandos.prepare(new GenerateWeeklyPlanCommand(u, S1, null, true, null, null)).context();
                var el = elegibles(ctx);
                var slots = new EnumMap<MandatorySlot, Long>(MandatorySlot.class);
                for (var s : MandatorySlot.ALL)
                    slots.put(s, el.values().stream().filter(c -> MandatorySlot.of(c).equals(Optional.of(s))).count());
                if (slots.get(MandatorySlot.UPPER_PULL) != 0)
                    fallos.add(perfil + ": sin espaldera ni anclaje no deberia haber tiron superior: " + slots);
                String resultado;
                try {
                    planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
                    resultado = "PUBLICADO";
                    // El borrador del motor es el publicado: cada sesion al 70 % exacto y sin implementos.
                    var borrador = motor.generate(ctx, List.of());
                    long piso = PlanDraftValidator.minimumSessionSeconds(ctx);
                    for (var w : borrador.workouts()) {
                        if (estimador.estimateSeconds(w, ctx.prescription()) < piso)
                            fallos.add(perfil + " " + w.scheduledDate() + " bajo el 70 %");
                        for (var e : w.exercises())
                            if (ESPALDERA.contains(e.exerciseId()) || Set.of(1072L, 880L, 58L, 1253L).contains(e.exerciseId()))
                                fallos.add(perfil + ": " + e.exerciseId() + " exige un implemento no declarado");
                    }
                } catch (PlanGenerationFailedException e) {
                    resultado = "SIN_PLAN " + e.getMessage().replaceAll(".*Ultimos problemas: ", "");
                }
                sb.append(perfil).append('\t').append(slots.get(MandatorySlot.UPPER_PULL)).append('\t')
                        .append(slots.get(MandatorySlot.LOWER_HIP)).append('\t').append(slots.get(MandatorySlot.LOWER_KNEE))
                        .append('\t').append(slots.get(MandatorySlot.UPPER_PUSH)).append('\t').append(resultado).append('\n');
            }
        var dir = System.getProperty("fitsense.v39.dir");
        if (dir != null) {
            Files.createDirectories(Path.of(dir));
            Files.writeString(Path.of(dir, "solo_peso_corporal.tsv"), sb.toString());
        }
        assertEquals(List.of(), fallos, sb.toString());
    }

    @Test
    @DisplayName("9 · dictamen aprobado: soporte real exigido, toalla documentada, 1106 provisional y 1113/849 sin cambios")
    void dictamenAprobado() {
        // 813: poste vertical (GIF 3303), no espaldera; y no se equiparan.
        assertEquals("1:body weight,2:vertical pole", requisitos(813));
        assertEquals(Boolean.TRUE, jdbc.queryForObject("SELECT requires_gym FROM equipment_types WHERE code = 'vertical pole'",
                Boolean.class));
        // Remos con toalla: el soporte sigue siendo obligatorio; nunca solo peso corporal.
        for (long id : List.of(302L, 306L, 308L)) assertEquals("1:body weight,2:wall bars", requisitos(id));
        assertEquals("1:body weight,2:anchor point", requisitos(1072));
        assertTrue(jdbc.queryForObject("SELECT name_es FROM equipment_types WHERE code = 'anchor point'", String.class)
                .contains("firme"));
        for (long id : List.of(302L, 306L, 308L, 1072L)) {
            var f = jdbc.queryForMap("SELECT instruction_steps_es ->> 0 AS paso, instructions_es,"
                    + " (SELECT string_agg(t.p, ' ' ORDER BY t.n) FROM jsonb_array_elements_text(instruction_steps_es)"
                    + " WITH ORDINALITY AS t (p, n)) AS unidos FROM exercises WHERE exercise_id = ?", id);
            String paso = (String) f.get("paso");
            assertTrue(paso.contains("toalla resistente") && paso.toLowerCase().contains("comprueba"),
                    id + ": " + paso);
            assertTrue(paso.contains(id == 1072L ? "anclaje firme" : "espaldera"), id + ": " + paso);
            assertEquals(f.get("unidos"), f.get("instructions_es"), id + ": instrucciones = pasos unidos");
        }
        var bw = elegibles(contexto(GoalType.GENERAL_FITNESS, FitnessLevel.ADVANCED, 4, SOLO_CUERPO));
        assertTrue(Collections.disjoint(bw.keySet(), Set.of(302L, 306L, 308L, 1072L, 813L)), "solo peso corporal");
        // 1106: nivel 2 PROVISIONAL, sin aprobacion formal registrada.
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM exercise_difficulty_approvals WHERE exercise_id = 1106",
                Integer.class));
        // Pendientes de verificacion visual: se quedan como los dejo V39, sin cambios nuevos.
        assertEquals("1:ez barbell,2:preacher bench", requisitos(849));
        assertEquals(1, jdbc.queryForObject("SELECT difficulty_level FROM exercises WHERE exercise_id = 1113", Integer.class));
    }

    private String requisitos(long id) {
        return jdbc.queryForObject("SELECT string_agg(r.group_no || ':' || q.code, ',' ORDER BY r.group_no, q.code)"
                + " FROM exercise_equipment_requirements r JOIN equipment_types q ON q.equipment_id = r.equipment_id"
                + " WHERE r.exercise_id = ?", String.class, id);
    }

    private PlanGenerationContext contexto(GoalType objetivo, FitnessLevel nivel, int dias, List<String> equipo) {
        long u = participante(objetivo, nivel, dias, equipo);
        return planComandos.prepare(new GenerateWeeklyPlanCommand(u, S1, null, true, null, null)).context();
    }

    private static Map<Long, CandidateExercise> elegibles(PlanGenerationContext ctx) {
        return ctx.availableExercises().stream()
                .collect(Collectors.toMap(CandidateExercise::exerciseId, Function.identity(), (a, b) -> a));
    }

    private long participante(GoalType objetivo, FitnessLevel nivel, int dias, List<String> equipo) {
        String email = "v39-" + (++secuencia) + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Prueba', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        var disponibles = new ArrayList<Short>();
        if (dias == 3) disponibles.addAll(List.of((short) 1, (short) 3, (short) 5));
        else for (short d = 1; d <= dias; d++) disponibles.add(d);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1992, 1, 15), BiologicalSex.FEMALE,
                new BigDecimal("165"), new BigDecimal("70"), null, nivel, objetivo, "v39", TrainingLocation.HOME,
                (short) dias, disponibles, (short) 45, equipo, List.of(), "Ninguna", true, true, true, true, true));
        return u;
    }
}
