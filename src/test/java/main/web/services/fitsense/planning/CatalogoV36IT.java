package main.web.services.fitsense.planning;

import main.web.services.fitsense.orchestration.application.internal.WeeklyCycleService;
import main.web.services.fitsense.planning.application.internal.outboundservices.acl.ExternalCatalogService;
import main.web.services.fitsense.planning.application.internal.queryservices.PublishedPlanSafetyService;
import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.MandatorySlot;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanningProfile;
import main.web.services.fitsense.planning.domain.model.valueobjects.SafetyProfile;
import main.web.services.fitsense.planning.infrastructure.persistence.jpa.repositories.WeeklyTrainingPlanRepository;
import main.web.services.fitsense.planning.infrastructure.generation.ai.ReplicateSessionFixGenerator;
import main.web.services.fitsense.planning.interfaces.acl.PlanningContextFacade;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.BiologicalSex;
import main.web.services.fitsense.profiling.domain.model.valueobjects.FitnessLevel;
import main.web.services.fitsense.profiling.domain.model.valueobjects.GoalType;
import main.web.services.fitsense.profiling.domain.model.valueobjects.TrainingLocation;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import main.web.services.fitsense.shared.domain.model.valueobjects.TrainingWeek;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * V36: correccion de nueve ejercicios del catalogo.
 * <p>
 * PostgreSQL 16 efimero con Flyway SOLO hasta V35; se publican planes reales
 * con el catalogo anterior y V36 se aplica despues, con Flyway y la misma
 * configuracion que la aplicacion, como ocurriria en un despliegue. Motor de
 * reglas, IA desactivada y simulada: nunca se llama a Replicate.
 */
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "spring.flyway.target=35"
})
@DisplayName("V36: correccion de nueve ejercicios del catalogo")
class CatalogoV36IT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @MockitoBean ReplicateSessionFixGenerator ia;

    @Autowired Flyway flyway;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlanningContextFacade planning;
    @Autowired UserProfileCommandService perfiles;
    @Autowired ExternalCatalogService catalogo;
    @Autowired PublishedPlanSafetyService seguridadPublicados;
    @Autowired WeeklyTrainingPlanRepository planes;
    @Autowired WeeklyCycleService ciclo;
    @Autowired PlatformTransactionManager tm;

    static final List<Long> NUEVE = List.of(527L, 609L, 725L, 760L, 872L, 1059L, 1267L, 1296L, 1333L);
    static final String NUEVE_SQL = "527, 609, 725, 760, 872, 1059, 1267, 1296, 1333";
    static final LocalDate S1 = TrainingWeek.containing(LocalDate.now()).startDate().plusWeeks(1);
    static final List<String> GIMNASIO = GeneracionReferenciaIT.GIMNASIO;
    static final List<String> CASA = GeneracionReferenciaIT.CASA;
    static final AtomicInteger SECUENCIA = new AtomicInteger();
    /** Si la correccion del respaldo de 5-6 dias (PR #3) esta en el arbol: su prueba existe. */
    static final boolean RESPALDO_INTEGRADO = java.nio.file.Files.exists(java.nio.file.Path.of(
            "src/test/java/main/web/services/fitsense/planning/RespaldoCincoSeisDiasIT.java"));

    /** Lo capturado antes de V36 y comparado despues. */
    static String huellaResto;
    static String huellaRequisitos;
    static String huellaAprobaciones;
    static String huellaPlanes;
    /** Participantes con un plan publicado ANTES de V36 que contiene el ejercicio. */
    static long con1267, con872, con1059, con1296, conTiron;
    static long plan1267, plan872, plan1059, plan1296, planTiron;

    // =======================================================================
    // Antes de V36
    // =======================================================================

    @Test
    @Order(1)
    @DisplayName("antes de V36: la clasificacion anterior y planes publicados que la usan")
    void antesDeV36() {
        assertEquals(List.of("35"), jdbc.queryForList("SELECT version FROM flyway_schema_history"
                + " WHERE success ORDER BY installed_rank DESC LIMIT 1", String.class));
        assertEquals(1, cuenta("SELECT count(*) FROM exercises WHERE exercise_id = 1267 AND is_active"));
        assertEquals("HIP_EXTENSION", patron(872));

        // Planes reales publicados con el catalogo anterior que contienen 1267 y 872:
        // principiantes de 4 dias (tren superior / inferior), donde el barrido los encontro.
        var objetivos = GoalType.values();
        for (int intento = 0; intento < 96 && (con1267 == 0 || con872 == 0 || con1059 == 0); intento++) {
            var objetivo = objetivos[intento % objetivos.length];
            String lugar = (intento / objetivos.length) % 2 == 0 ? "GYM" : "CASA";
            int dias = (intento / (2 * objetivos.length)) % 2 == 0 ? 4 : 3;
            long u = participante(objetivo, FitnessLevel.BEGINNER, dias, lugar);
            long plan = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElse(-1L);
            if (plan < 0) continue;
            var ids = ejerciciosDe(plan);
            // Sin 1267 en los de 872 y 1059, para separar las causas en la semana siguiente.
            boolean a = ids.contains(1267L), b = ids.contains(872L), c = ids.contains(1059L);
            if (con1267 == 0 && a) { con1267 = u; plan1267 = plan; }
            else if (con872 == 0 && b && !a) { con872 = u; plan872 = plan; }
            else if (con1059 == 0 && c && !a && !b) { con1059 = u; plan1059 = plan; }
        }
        // Un combinado (527, 609, 725, 760) en una sesion PULL: rutinas de 6 dias.
        var ppl = new GoalType[]{GoalType.GAIN_MUSCLE, GoalType.INCREASE_STRENGTH};
        var niveles = new FitnessLevel[]{FitnessLevel.ADVANCED, FitnessLevel.INTERMEDIATE};
        for (int intento = 0; intento < 48 && conTiron == 0; intento++) {
            long u = participante(ppl[intento % 2], niveles[(intento / 2) % 2], 6, (intento / 4) % 2 == 0 ? "GYM" : "CASA");
            long plan = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElse(-1L);
            if (plan < 0 || ejerciciosDe(plan).contains(1267L)) continue;
            if (cuenta("SELECT count(*) FROM planned_workouts pw JOIN planned_workout_exercises pwe"
                    + " ON pwe.planned_workout_id = pw.planned_workout_id WHERE pw.plan_id = " + plan
                    + " AND pw.focus_code = 'PULL' AND pwe.exercise_id IN (527, 609, 725, 760)") > 0) {
                conTiron = u;
                planTiron = plan;
            }
        }
        assertNotEquals(0, conTiron, "ningun plan del catalogo anterior lleva un combinado en PULL");
        assertNotEquals(0, con1267, "ningun plan del catalogo anterior contiene 1267");
        assertNotEquals(0, con872, "ningun plan del catalogo anterior contiene 872");
        assertNotEquals(0, con1059, "ningun plan del catalogo anterior contiene 1059");

        // 1296 en el plan de un principiante, como si se hubiera publicado con el
        // nivel 1 anterior: sustituye un empuje horizontal con la misma dosis.
        con1296 = participante(GoalType.GENERAL_FITNESS, FitnessLevel.BEGINNER, 3, "GYM");
        plan1296 = planning.generateWeeklyPlan(con1296, S1, PlanAdjustment.none(), null, null).orElseThrow();
        long fila = jdbc.queryForObject("SELECT pwe.planned_exercise_id FROM planned_workout_exercises pwe"
                + " JOIN planned_workouts pw ON pw.planned_workout_id = pwe.planned_workout_id"
                + " JOIN exercises x ON x.exercise_id = pwe.exercise_id"
                + " WHERE pw.plan_id = ? AND x.movement_pattern = 'HORIZONTAL_PUSH'"
                + " ORDER BY pw.scheduled_date, pwe.exercise_order LIMIT 1", Long.class, plan1296);
        jdbc.update("UPDATE planned_workout_exercises SET exercise_id = 1296 WHERE planned_exercise_id = ?", fila);
        assertEquals(Map.of(), conflictos(plan1296),
                "con el catalogo anterior 1296 no choca con nada");

        huellaResto = huellaResto();
        huellaRequisitos = huellaRequisitos();
        huellaAprobaciones = huellaAprobaciones();
        huellaPlanes = huellaPlanes();
    }

    // =======================================================================
    // V36
    // =======================================================================

    @Test
    @Order(2)
    @DisplayName("V36 aplica solo los cambios aprobados y no toca nada mas")
    void aplicaV36() {
        var resultado = Flyway.configure().configuration(flyway.getConfiguration()).target("36").load().migrate();
        assertTrue(resultado.success);
        assertEquals(1, resultado.migrationsExecuted);
        assertEquals("36", jdbc.queryForObject("SELECT version FROM flyway_schema_history WHERE success"
                + " ORDER BY installed_rank DESC LIMIT 1", String.class));

        // Matriz despues (V36): las columnas aprobadas, ejercicio por ejercicio.
        assertEquals(
                "527|t|2|f|f|f|KNEE_DOMINANT|upper legs|dumbbell|quads|biceps|Curl de bíceps con zancada y lanzamiento\n"
                + "609|t|2|f|f|f|KNEE_DOMINANT|upper legs|dumbbell|quads|biceps|Curl de bíceps en zancada con mancuerna\n"
                + "725|t|1|f|f|t|VERTICAL_PUSH|shoulders|dumbbell|delts|biceps|Curl de bíceps sentado con press de hombro\n"
                + "760|t|2|f|f|t|VERTICAL_PUSH|shoulders|dumbbell|delts|biceps|Curl martillo alterno de pie y press\n"
                + "872|t|1|f|t|f|TRUNK_FLEXION|waist|body weight|abs|glutes|Encogimiento invertido\n"
                + "1059|t|1|f|f|f|KNEE_DOMINANT|upper legs|body weight|quads|abs|Zancada con giro\n"
                + "1267|f|1|f|f|t|KNEE_DOMINANT|upper arms|body weight|triceps|shoulders|Stalder press\n"
                + "1296|t|3|t|t|t|HORIZONTAL_PUSH|chest|body weight|pectorals|core|Push-up de Superman\n"
                + "1333|t|1|t|f|f|LOCOMOTION_CARDIO|cardio|body weight|cardiovascular system|quadriceps|"
                + "Carrera con zancadas amplias",
                String.join("\n", jdbc.queryForList("SELECT e.exercise_id || '|' || CASE WHEN e.is_active THEN 't'"
                        + " ELSE 'f' END || '|' || e.difficulty_level || '|' || CASE WHEN e.high_impact THEN 't' ELSE"
                        + " 'f' END || '|' || CASE WHEN e.requires_floor THEN 't' ELSE 'f' END || '|' || CASE WHEN"
                        + " e.requires_overhead THEN 't' ELSE 'f' END || '|' || e.movement_pattern || '|' || bp.code"
                        + " || '|' || eq.code || '|' || e.target_muscle || '|' || e.synergist_muscle || '|' || e.name_es"
                        + " FROM exercises e JOIN body_parts bp ON bp.body_part_id = e.body_part_id"
                        + " JOIN equipment_types eq ON eq.equipment_id = e.equipment_id"
                        + " WHERE e.exercise_id IN (" + NUEVE_SQL + ") ORDER BY e.exercise_id", String.class)));
        assertEquals(0, cuenta("SELECT count(*) FROM exercise_equipment_requirements WHERE exercise_id = 1333"),
                "1333 ya no exige rueda abdominal");
        assertEquals(9, cuenta("SELECT count(*) FROM exercises WHERE exercise_id IN (" + NUEVE_SQL + ")"),
                "ningun registro se elimina");

        assertEquals(huellaResto, huellaResto(), "el resto del catalogo no cambia");
        assertEquals(huellaRequisitos, huellaRequisitos(), "los requisitos del resto no cambian");
        assertEquals(huellaAprobaciones, huellaAprobaciones(), "el historial de V27/V28 no cambia");
        assertEquals(huellaPlanes, huellaPlanes(), "los planes publicados no se alteran");
    }

    // =======================================================================
    // Planes publicados antes de V36
    // =======================================================================

    @Test
    @Order(3)
    @DisplayName("planes publicados: 1296 se marca por alto impacto; 1267 sigue visible y no es causa de seguridad")
    void planesPublicados() {
        var conflictos1296 = conflictos(plan1296);
        assertEquals(List.of("alto_impacto"), List.copyOf(new LinkedHashSet<>(conflictos1296.values())),
                "la proteccion vigente marca 1296 en las sesiones abiertas del principiante");
        assertEquals(Set.of(1296L), idsDe(conflictos1296.keySet()));

        // Inactivo NO es causa de seguridad (SafetyConflictPolicy): 1267 se sigue mostrando.
        var conflictos1267 = conflictos(plan1267);
        assertFalse(idsDe(conflictos1267.keySet()).contains(1267L));
        assertTrue(ejerciciosDe(plan1267).contains(1267L));
        assertTrue(ejerciciosDe(plan872).contains(872L));
    }

    /**
     * INCOMPATIBILIDAD REPORTADA, no corregida: la copia semanal (V31) de una
     * semana publicada ANTES de V36 se valida con el catalogo corregido. Un
     * ejercicio que deja de ser elegible sin causa de seguridad (1267 inactivo)
     * o que cambia de grupo dentro de una sesion PULL hace la copia no factible
     * y, por 19.4, la semana nueva queda sin plan. 1296 en un principiante si
     * tiene causa (alto impacto) y la sustitucion obligatoria SUST-SEG-1 lo
     * resuelve. 872 y 1059 se conservan. Ver CATALOGO_V36.md, seccion 8.
     */
    @Test
    @Order(4)
    @DisplayName("semana siguiente de planes publicados antes de V36: lo que hoy hace la copia semanal")
    void semanaSiguiente() {
        var r1267 = cicloSiguiente(con1267);
        assertTrue(r1267.plan().isEmpty(), "1267: " + r1267);
        assertEquals("NO_FACTIBLE", r1267.estado());
        assertTrue(r1267.motivo().contains("V1: el ejercicio 1267 no esta en available_exercises"), r1267.motivo());

        var rTiron = cicloSiguiente(conTiron);
        assertTrue(rTiron.plan().isEmpty(), "combinado en PULL: " + rTiron);
        assertEquals("NO_FACTIBLE", rTiron.estado());
        assertTrue(rTiron.motivo().contains("tiene enfoque PULL pero incluye ejercicios de [upper legs]")
                || rTiron.motivo().contains("tiene enfoque PULL pero incluye ejercicios de [shoulders]"), rTiron.motivo());

        var r1296 = cicloSiguiente(con1296);
        assertTrue(r1296.plan().isPresent(), "1296: " + r1296);
        assertEquals("CONSERVED", r1296.origen());
        assertTrue(r1296.informe().contains("\"quitado\": 1296") && r1296.informe().contains("SUST-SEG-1")
                && r1296.informe().contains("alto_impacto"), r1296.informe());
        assertFalse(ejerciciosDe(r1296.plan().get()).contains(1296L));

        var r872 = cicloSiguiente(con872);
        assertTrue(r872.plan().isPresent(), "872: " + r872);
        assertEquals("CONSERVED", r872.origen());
        assertTrue(ejerciciosDe(r872.plan().get()).contains(872L));
        var r1059 = cicloSiguiente(con1059);
        assertTrue(r1059.plan().isPresent(), "1059: " + r1059);
        assertEquals("CONSERVED", r1059.origen());
        verify(ia, never()).pedirRespuesta(any(), any(), any(), any(), anyInt());
    }

    record Ciclo(Optional<Long> plan, String estado, String origen, String motivo, String informe) {}

    /** El ciclo real de la semana S1 a la S1+1, sin registros de ejecucion (INSUFFICIENT_DATA). */
    private Ciclo cicloSiguiente(long u) {
        var plan = ciclo.runFor(u, S1, S1.plusWeeks(1));
        var f = jdbc.queryForMap("SELECT status, generation_source, coalesce(rejection_reason, '') AS motivo,"
                + " conservation_report::text AS informe FROM plan_adaptation_attempts WHERE user_id = ?"
                + " ORDER BY attempt_id DESC LIMIT 1", u);
        return new Ciclo(plan, (String) f.get("status"), (String) f.get("generation_source"),
                (String) f.get("motivo"), (String) f.get("informe"));
    }

    // =======================================================================
    // Conjunto elegible despues de V36
    // =======================================================================

    @Test
    @Order(5)
    @DisplayName("conjunto elegible: 1267 nunca; 1296 solo avanzado con impacto; 1333 sin rueda y sin principiantes")
    void elegibilidad() {
        var libre = SafetyProfile.Declaradas.sinLimitaciones();
        var sinImpacto = new SafetyProfile.Declaradas(false, true, true, true, true);
        var sinBrazosArriba = new SafetyProfile.Declaradas(true, true, true, true, false);
        var todo = new ArrayList<>(GIMNASIO);
        todo.add("ab wheel");
        for (var nivel : List.of("BEGINNER", "INTERMEDIATE", "ADVANCED"))
            for (var equipo : List.of(todo, GIMNASIO, CASA, List.of("body weight"))) {
                var ids = elegibles(nivel, 30, equipo, libre);
                assertFalse(ids.containsKey(1267L), "1267 para " + nivel);
                assertEquals("ADVANCED".equals(nivel), ids.containsKey(1296L), "1296 para " + nivel);
                assertEquals(!"BEGINNER".equals(nivel), ids.containsKey(1333L), "1333 para " + nivel + " " + equipo);
            }
        assertFalse(elegibles("ADVANCED", 30, GIMNASIO, sinImpacto).containsKey(1296L), "declaro sin impacto");
        assertFalse(elegibles("ADVANCED", 30, GIMNASIO, sinImpacto).containsKey(1333L), "declaro sin impacto");
        assertFalse(elegibles("ADVANCED", SafetyProfile.EDAD_SIN_IMPACTO, GIMNASIO, libre).containsKey(1296L),
                "edad sin impacto");

        var avanzado = elegibles("ADVANCED", 30, GIMNASIO, libre);
        var c872 = avanzado.get(872L);
        assertEquals("TRUNK_FLEXION", c872.movementPattern());
        assertEquals(Optional.empty(), MandatorySlot.of(c872), "872 ya no cubre extension de cadera");
        assertEquals("upper legs", avanzado.get(1059L).bodyPartCode());
        assertEquals(Optional.of(MandatorySlot.LOWER_KNEE), MandatorySlot.of(avanzado.get(1059L)));
        for (long id : List.of(527L, 609L)) {
            assertEquals("upper legs", avanzado.get(id).bodyPartCode());
            assertEquals(Optional.of(MandatorySlot.LOWER_KNEE), MandatorySlot.of(avanzado.get(id)));
        }
        for (long id : List.of(725L, 760L)) {
            assertEquals("shoulders", avanzado.get(id).bodyPartCode());
            assertEquals(Optional.of(MandatorySlot.UPPER_PUSH), MandatorySlot.of(avanzado.get(id)));
            assertFalse(elegibles("ADVANCED", 30, GIMNASIO, sinBrazosArriba).containsKey(id),
                    id + " respeta la restriccion de brazos arriba");
        }
    }

    // =======================================================================
    // Generacion despues de V36
    // =======================================================================

    @Test
    @Order(6)
    @DisplayName("planes nuevos: ningun 1267; 527/609/725/760 nunca en PULL; 1296 y 1333 solo donde se admiten")
    void generacion() {
        int planesGenerados = 0;
        for (var objetivo : GoalType.values())
            for (var nivel : FitnessLevel.values())
                for (int dias : new int[]{4, 6})
                    for (var lugar : List.of("GYM", "CASA")) {
                        // GENERAL_FITNESS y LOSE_WEIGHT de 6 dias dependen de la correccion del
                        // respaldo (PR #3): se validan en la ejecucion combinada.
                        if (dias == 6 && !RESPALDO_INTEGRADO
                                && (objetivo == GoalType.GENERAL_FITNESS || objetivo == GoalType.LOSE_WEIGHT))
                            continue;
                        long u = participante(objetivo, nivel, dias, lugar);
                        long plan = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null)
                                .orElseThrow(() -> new AssertionError("sin plan: " + objetivo + "/" + nivel));
                        planesGenerados++;
                        for (var f : jdbc.queryForList("SELECT pw.focus_code, pwe.exercise_id FROM planned_workouts pw"
                                + " JOIN planned_workout_exercises pwe ON pwe.planned_workout_id = pw.planned_workout_id"
                                + " WHERE pw.plan_id = ?", plan)) {
                            long id = ((Number) f.get("exercise_id")).longValue();
                            String foco = (String) f.get("focus_code");
                            String perfil = objetivo + "/" + nivel + "/" + dias + "d/" + lugar + " " + foco;
                            assertNotEquals(1267L, id, perfil);
                            if (List.of(527L, 609L, 725L, 760L).contains(id))
                                assertNotEquals("PULL", foco, id + " en " + perfil);
                            if (id == 1296L) assertEquals(FitnessLevel.ADVANCED, nivel, perfil);
                            if (id == 1333L) assertNotEquals(FitnessLevel.BEGINNER, nivel, perfil);
                        }
                    }
        assertEquals(RESPALDO_INTEGRADO ? 48 : 36, planesGenerados);
        verify(ia, never()).pedirRespuesta(any(), any(), any(), any(), anyInt());
    }

    // =======================================================================
    // Material
    // =======================================================================

    private long participante(GoalType objetivo, FitnessLevel nivel, int dias, String lugar) {
        String email = "catalogo36-" + SECUENCIA.incrementAndGet() + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Prueba', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        var disponibles = new ArrayList<Short>();
        if (dias == 3) disponibles.addAll(List.of((short) 1, (short) 3, (short) 5));
        else for (short d = 1; d <= dias; d++) disponibles.add(d);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1992, 1, 15), BiologicalSex.FEMALE,
                new BigDecimal("165"), new BigDecimal("70"), null, nivel, objetivo, "catalogo v36",
                "GYM".equals(lugar) ? TrainingLocation.GYM : TrainingLocation.HOME, (short) dias, disponibles,
                (short) 45, "GYM".equals(lugar) ? GIMNASIO : CASA, List.of(), "Ninguna", true, true, true, true, true));
        return u;
    }

    private Map<Long, String> conflictos(long plan) {
        return new TransactionTemplate(tm).execute(s -> seguridadPublicados.conflictos(planes.findById(plan).orElseThrow()));
    }

    private Map<Long, CandidateExercise> elegibles(String nivel, int edad, List<String> equipo,
                                                  SafetyProfile.Declaradas declaradas) {
        var p = new PlanningProfile(99L, edad, "FEMALE", new BigDecimal("165"), new BigDecimal("70"), null, nivel,
                "GENERAL_FITNESS", "x", "Ninguna", "GYM", 3, List.of((short) 1, (short) 3, (short) 5), 45,
                equipo, List.of(), declaradas);
        var mapa = new HashMap<Long, CandidateExercise>();
        catalogo.fetchEligibleFor(p, p.maxDifficultyLevel(), SafetyProfile.forAgeLevelAndDeclared(edad, nivel, declaradas))
                .forEach(c -> mapa.put(c.exerciseId(), c));
        return mapa;
    }

    private List<Long> ejerciciosDe(long plan) {
        return jdbc.queryForList("SELECT pwe.exercise_id FROM planned_workout_exercises pwe JOIN planned_workouts pw"
                + " ON pw.planned_workout_id = pwe.planned_workout_id WHERE pw.plan_id = ?"
                + " ORDER BY pw.scheduled_date, pwe.exercise_order", Long.class, plan);
    }

    private Set<Long> idsDe(Set<Long> plannedExerciseIds) {
        var ids = new TreeSet<Long>();
        for (long pe : plannedExerciseIds)
            ids.add(jdbc.queryForObject("SELECT exercise_id FROM planned_workout_exercises WHERE planned_exercise_id = ?",
                    Long.class, pe));
        return ids;
    }

    private String patron(long id) {
        return jdbc.queryForObject("SELECT movement_pattern FROM exercises WHERE exercise_id = ?", String.class, id);
    }

    private int cuenta(String sql) {
        return jdbc.queryForObject(sql, Integer.class);
    }

    private String huellaResto() {
        return jdbc.queryForObject("SELECT count(*) || ':' || md5(string_agg(e::text, '|' ORDER BY exercise_id))"
                + " FROM exercises e WHERE exercise_id NOT IN (" + NUEVE_SQL + ")", String.class);
    }

    private String huellaRequisitos() {
        return jdbc.queryForObject("SELECT count(*) || ':' || md5(string_agg(r::text, '|' ORDER BY r::text))"
                + " FROM exercise_equipment_requirements r WHERE exercise_id <> 1333", String.class);
    }

    private String huellaAprobaciones() {
        return jdbc.queryForObject("SELECT count(*) || ':' || coalesce(md5(string_agg(a::text, '|' ORDER BY event_id)), '-')"
                + " FROM exercise_difficulty_approvals a", String.class);
    }

    private String huellaPlanes() {
        return jdbc.queryForObject("SELECT (SELECT count(*) FROM planned_workout_exercises) || ':'"
                + " || md5(coalesce((SELECT string_agg(x::text, '|' ORDER BY planned_exercise_id)"
                + " FROM planned_workout_exercises x), '') || coalesce((SELECT string_agg(w::text, '|'"
                + " ORDER BY planned_workout_id) FROM planned_workouts w), '') || coalesce((SELECT"
                + " string_agg(p::text, '|' ORDER BY plan_id) FROM weekly_training_plans p), ''))", String.class);
    }
}
