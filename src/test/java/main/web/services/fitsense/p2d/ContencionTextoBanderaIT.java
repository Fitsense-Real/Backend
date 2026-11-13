package main.web.services.fitsense.p2d;

import main.web.services.fitsense.execution.domain.exceptions.WorkoutNotAvailableException;
import main.web.services.fitsense.execution.domain.model.commands.RecordSessionExerciseCommand;
import main.web.services.fitsense.execution.domain.model.commands.StartWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.services.WorkoutSessionCommandService;
import main.web.services.fitsense.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import main.web.services.fitsense.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import main.web.services.fitsense.planning.application.internal.commandservices.PublishedPlanSafetyRepairService.Estado;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.domain.model.valueobjects.SafetyProfile;
import main.web.services.fitsense.planning.domain.services.ContencionTextoBandera;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
import main.web.services.fitsense.planning.infrastructure.generation.ai.PlanInputSnapshot;
import main.web.services.fitsense.planning.interfaces.acl.PlanningContextFacade;
import main.web.services.fitsense.planning.interfaces.rest.TrainingPlanController;
import main.web.services.fitsense.planning.interfaces.rest.resources.PlannedWorkoutResource;
import main.web.services.fitsense.planning.interfaces.rest.resources.WeeklyTrainingPlanResource;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.BiologicalSex;
import main.web.services.fitsense.profiling.domain.model.valueobjects.FitnessLevel;
import main.web.services.fitsense.profiling.domain.model.valueobjects.GoalType;
import main.web.services.fitsense.profiling.domain.model.valueobjects.TrainingLocation;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import main.web.services.fitsense.shared.domain.model.valueobjects.SkipReason;
import main.web.services.fitsense.shared.domain.model.valueobjects.TrainingWeek;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * INC-CAT-C39 · CONTENCION POR TEXTO DEL CATALOGO CONTRA BANDERA, con el catalogo real (V1-V50: incluye las
 * correcciones del catalogo integral, PR #56-#59), PostgreSQL efimero,
 * motor de reglas y la instantanea que recibe la IA; sin llamadas a Replicate. Los planes publicados se manipulan
 * SOLO en esta base aislada (copias controladas), nunca en datos reales.
 * <p>
 * A colgarse, B brazos arriba, C ambas (y suelo), D catalogo de la IA, E motor de reglas, F plan publicado con una
 * sesion pendiente, G sesion en curso, H factibilidad sin compensar con prohibidos, I coherencia texto-bandera,
 * L liberados por V49 (vuelven solo porque su texto ya no contradice su bandera) y R los dos perfiles LOSE_WEIGHT
 * que recuperan su semana con la contencion reducida a 8.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
@DisplayName("INC-CAT-C39 · contencion por texto del catalogo contra bandera (catalogo real)")
class ContencionTextoBanderaIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired PlanningContextFacade planning;
    @Autowired WeeklyTrainingPlanCommandService planComandos;
    @Autowired UserProfileCommandService perfiles;
    @Autowired WorkoutSessionCommandService sesiones;
    @Autowired TrainingPlanController planes;
    @Autowired UserRepository usuarios;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transacciones;

    static final AtomicInteger SECUENCIA = new AtomicInteger();
    static final LocalDate HOY = LocalDate.now();
    static final LocalDate SEMANA = TrainingWeek.containing(HOY).startDate().plusWeeks(1);

    /** El gimnasio del usuario 39 (los 29 codigos reales): incluye paralelas, barra, correas y silla romana. */
    static final List<String> GIMNASIO = List.of("assisted", "band", "barbell", "bench", "body weight", "cable",
            "captains chair", "dumbbell", "ez barbell", "ghd bench", "gymnastic rings", "hyperextension bench",
            "landmine", "leverage machine", "olympic barbell", "other", "parallel bars", "preacher bench",
            "pull-up bar", "rope", "sled machine", "smith machine", "squat rack", "stationary bike", "step platform",
            "suspension straps", "treadmill", "vertical pole", "weighted");
    static final List<String> CASA = List.of("body weight", "dumbbell", "band");

    /** Restricciones declarables (suelo, colgarse, brazos arriba); impacto, rodillas permitidos. */
    record Restricciones(boolean sinSuelo, boolean sinColgarse, boolean sinBrazosArriba) {
        SafetyProfile seguridad() {
            return new SafetyProfile(false, sinSuelo, false, false, sinColgarse, sinBrazosArriba, false);
        }
    }

    static final Restricciones LIBRE = new Restricciones(false, false, false);
    static final Restricciones SIN_COLGARSE = new Restricciones(false, true, false);
    static final Restricciones SIN_CABEZA = new Restricciones(false, false, true);
    static final Restricciones SIN_AMBAS = new Restricciones(false, true, true);
    static final Restricciones SIN_NADA = new Restricciones(true, true, true);

    private long usuario(GoalType objetivo, FitnessLevel nivel, TrainingLocation lugar, List<String> equipo,
                         Restricciones r) {
        int n = SECUENCIA.incrementAndGet();
        String email = "c39-" + n + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'C39', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        // allowHighImpact, allowFloor, allowKneeling, allowHanging, allowOverhead
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1985, 6, 1), BiologicalSex.MALE,
                new BigDecimal("180"), new BigDecimal("100"), new BigDecimal("80"), nivel, objetivo,
                "Quiero Correr un maraton", lugar, (short) 4, List.of((short) 1, (short) 4, (short) 5, (short) 7),
                (short) 60, equipo, List.of(), null, true, !r.sinSuelo(), true, !r.sinColgarse(),
                !r.sinBrazosArriba()));
        return u;
    }

    private Set<Long> elegibles(long u) {
        return planComandos.prepare(GenerateWeeklyPlanCommand.firstPlan(u, SEMANA)).context().availableExercises()
                .stream().map(CandidateExercise::exerciseId).collect(Collectors.toCollection(TreeSet::new));
    }

    private Set<Long> enviadosALaIA(long u) {
        var ctx = planComandos.prepare(GenerateWeeklyPlanCommand.firstPlan(u, SEMANA)).context();
        return PlanInputSnapshot.of(ctx).availableExercises().stream().map(PlanInputSnapshot.AvailableExercise::exerciseId)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private long generar(long u) {
        return planning.generateWeeklyPlan(u, SEMANA, PlanAdjustment.none(), null, null)
                .orElseThrow(() -> new AssertionError("no se genero la semana"));
    }

    private List<Long> ejerciciosDelPlan(long planId) {
        return jdbc.queryForList("SELECT pwe.exercise_id FROM planned_workout_exercises pwe JOIN planned_workouts pw"
                + " ON pw.planned_workout_id = pwe.planned_workout_id WHERE pw.plan_id = ?", Long.class, planId);
    }

    /** Ids del conjunto con una bandera dada a true (lo que el filtro de catalogo ya debia excluir). */
    private Set<Long> conBandera(Collection<Long> ids, String columna) {
        if (ids.isEmpty()) return Set.of();
        return new TreeSet<>(jdbc.queryForList("SELECT exercise_id FROM exercises WHERE (" + columna
                + ") AND exercise_id = ANY (?)", Long.class, (Object) ids.toArray(Long[]::new)));
    }

    private static Set<Long> interseccion(Collection<Long> a, Collection<Long> b) {
        var r = new TreeSet<>(a);
        r.retainAll(b);
        return r;
    }

    // =====================================================================

    @Nested
    @DisplayName("A-E · elegibilidad, catalogo de la IA y motor de reglas")
    class Elegibilidad {

        @Test
        @DisplayName("A · sin colgarse: ni bandera de colgarse ni texto que lo pida; el resto de contenidos sigue")
        void a_sinColgarse() {
            long u = usuario(GoalType.GENERAL_FITNESS, FitnessLevel.BEGINNER, TrainingLocation.GYM, GIMNASIO, SIN_COLGARSE);
            var e = elegibles(u);
            assertTrue(conBandera(e, "requires_hanging").isEmpty());
            assertTrue(interseccion(e, ContencionTextoBandera.contenidos(SIN_COLGARSE.seguridad())).isEmpty(), e.toString());
            assertTrue(interseccion(e, Set.of(1153L, 1301L, 1303L, 1304L, 1306L)).isEmpty(), e.toString());
            // Lo contenido solo por brazos arriba sigue llegando: lo que el perfil libre recibe de 131/400/448
            // (en este gimnasio y nivel, al menos el 448) llega igual a quien solo prohibe colgarse.
            var libre = elegibles(usuario(GoalType.GENERAL_FITNESS, FitnessLevel.BEGINNER, TrainingLocation.GYM, GIMNASIO, LIBRE));
            var soloCabeza = interseccion(libre, Set.of(131L, 400L, 448L));
            assertTrue(soloCabeza.contains(448L), soloCabeza.toString());
            assertTrue(e.containsAll(soloCabeza), soloCabeza + " " + e);
        }

        @Test
        @DisplayName("B · sin brazos arriba: ni bandera ni texto; colgarse sigue permitido para lo que no pide brazos arriba")
        void b_sinBrazosArriba() {
            long u = usuario(GoalType.GENERAL_FITNESS, FitnessLevel.BEGINNER, TrainingLocation.GYM, GIMNASIO, SIN_CABEZA);
            var e = elegibles(u);
            assertTrue(conBandera(e, "requires_overhead").isEmpty());
            assertTrue(interseccion(e, ContencionTextoBandera.contenidos(SIN_CABEZA.seguridad())).isEmpty(), e.toString());
            assertTrue(interseccion(e, Set.of(131L, 400L, 448L)).isEmpty(), e.toString());
            assertTrue(e.contains(1306L), "1306 solo se contiene por colgarse");
            assertTrue(e.contains(1301L), "1301 ya no se contiene por brazos arriba: V49 corrigio su texto");
        }

        @Test
        @DisplayName("C · sin colgarse, sin brazos arriba y sin suelo: la union, nada ambiguo de esas tres restricciones")
        void c_combinadas() {
            long u = usuario(GoalType.GENERAL_FITNESS, FitnessLevel.BEGINNER, TrainingLocation.GYM, GIMNASIO, SIN_NADA);
            var e = elegibles(u);
            assertTrue(conBandera(e, "requires_hanging OR requires_overhead OR requires_floor").isEmpty());
            assertTrue(interseccion(e, ContencionTextoBandera.registro().keySet()).isEmpty(), e.toString());
        }

        @Test
        @DisplayName("sin restricciones: los contenidos siguen elegibles (no se excluye de mas)")
        void libre() {
            long u = usuario(GoalType.GENERAL_FITNESS, FitnessLevel.BEGINNER, TrainingLocation.GYM, GIMNASIO, LIBRE);
            var e = elegibles(u);
            assertTrue(e.containsAll(List.of(316L, 1301L, 1303L, 1306L, 1152L, 1309L, 1322L, 448L, 1093L)), e.toString());
        }

        @Test
        @DisplayName("D · IA: ningun contenido en el catalogo que se envia a Replicate (instantanea real del prompt)")
        void d_catalogoDeLaIA() {
            for (var r : List.of(SIN_COLGARSE, SIN_CABEZA, SIN_AMBAS, SIN_NADA)) {
                long u = usuario(GoalType.GENERAL_FITNESS, FitnessLevel.BEGINNER, TrainingLocation.GYM, GIMNASIO, r);
                var ia = enviadosALaIA(u);
                assertFalse(ia.isEmpty());
                assertTrue(interseccion(ia, ContencionTextoBandera.contenidos(r.seguridad())).isEmpty(), r + " " + ia);
            }
        }

        @Test
        @DisplayName("E · motor de reglas: la semana publicada no contiene ningun contenido ni bandera restringida")
        void e_motorDeReglas() {
            for (var r : List.of(SIN_COLGARSE, SIN_CABEZA, SIN_NADA)) {
                long u = usuario(GoalType.GENERAL_FITNESS, FitnessLevel.BEGINNER, TrainingLocation.GYM, GIMNASIO, r);
                var plan = ejerciciosDelPlan(generar(u));
                assertFalse(plan.isEmpty());
                assertTrue(interseccion(plan, ContencionTextoBandera.contenidos(r.seguridad())).isEmpty(), r + " " + plan);
                if (r.sinColgarse()) assertTrue(conBandera(plan, "requires_hanging").isEmpty());
                if (r.sinBrazosArriba()) assertTrue(conBandera(plan, "requires_overhead").isEmpty());
                if (r.sinSuelo()) assertTrue(conBandera(plan, "requires_floor").isEmpty());
            }
        }
    }

    /**
     * Perfiles de esta rejilla sin plan. Con la contencion de 26 sobre el catalogo antiguo, LOSE_WEIGHT principiante
     * en casa quedaba sin plan: su cardio de pie (1093) tenia instrucciones de flexion en el suelo. V49 alineo ese
     * texto con el GIF (elevacion de rodilla de pie) y la contencion reducida a 8 ya no lo retiene, asi que el perfil
     * recupera su semana sin que se relaje V25 ni ninguna bandera (prueba R). Ninguno queda sin plan: cualquier
     * NO_FACTIBLE nuevo es una regresion.
     */
    static final Set<String> SIN_PLAN_ESPERADO = Set.of();

    @Test
    @DisplayName("H · factibilidad: 4 objetivos x 2 niveles x gimnasio/casa con las 3 restricciones; nunca un prohibido")
    void h_factibilidad() {
        var fallos = new ArrayList<String>();
        var sinPlan = new TreeSet<String>();
        for (var objetivo : GoalType.values())
            for (var nivel : List.of(FitnessLevel.BEGINNER, FitnessLevel.ADVANCED))
                for (var lugar : List.of(TrainingLocation.GYM, TrainingLocation.HOME)) {
                    String clave = objetivo + "/" + nivel + "/" + lugar;
                    long u = usuario(objetivo, nivel, lugar, lugar == TrainingLocation.GYM ? GIMNASIO : CASA, SIN_NADA);
                    List<Long> plan;
                    try {
                        plan = ejerciciosDelPlan(generar(u));
                    } catch (main.web.services.fitsense.planning.domain.exceptions.PlanGenerationFailedException ex) {
                        // NO_FACTIBLE explicito: la semana no se publica. Nunca un plan con un prohibido.
                        sinPlan.add(clave);
                        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ?",
                                Integer.class, u), "sin plan no deja nada a medias");
                        continue;
                    }
                    if (!interseccion(plan, ContencionTextoBandera.registro().keySet()).isEmpty()
                            || !conBandera(plan, "requires_hanging OR requires_overhead OR requires_floor").isEmpty())
                        fallos.add(clave + " contiene prohibidos: " + plan);
                }
        assertTrue(fallos.isEmpty(), String.join("\n", fallos));
        assertEquals(SIN_PLAN_ESPERADO, sinPlan, "solo el NO_FACTIBLE documentado; cualquier otro es una regresion");
    }

    // =====================================================================

    @Nested
    @DisplayName("F-G · planes ya publicados (copias controladas en la base aislada)")
    class Publicados {

        record Participante(long userId, long planId) {}

        private Participante publicadoSinRestricciones() {
            long u = usuario(GoalType.GENERAL_FITNESS, FitnessLevel.BEGINNER, TrainingLocation.GYM, GIMNASIO, LIBRE);
            return new Participante(u, generar(u));
        }

        /** Un ejercicio de cintura de una sesion pasa a ser el 1303, como si se hubiera publicado antes. */
        private long con1303(Participante p, Long sesion) {
            var fila = jdbc.queryForMap("SELECT pwe.planned_exercise_id AS pe, pw.planned_workout_id AS pw"
                    + " FROM planned_workout_exercises pwe JOIN planned_workouts pw ON pw.planned_workout_id = pwe.planned_workout_id"
                    + " JOIN exercises x ON x.exercise_id = pwe.exercise_id JOIN body_parts bp ON bp.body_part_id = x.body_part_id"
                    + " WHERE pw.plan_id = ? AND pwe.prescription_type = 'SETS_REPS' AND bp.code = 'waist'"
                    + (sesion == null ? "" : " AND pw.planned_workout_id = " + sesion)
                    + " ORDER BY pw.scheduled_date, pwe.exercise_order LIMIT 1", p.planId());
            jdbc.update("UPDATE planned_workout_exercises SET exercise_id = 1303 WHERE planned_exercise_id = ?",
                    ((Number) fila.get("pe")).longValue());
            return ((Number) fila.get("pw")).longValue();
        }

        private void prohibirColgarse(Participante p) {
            jdbc.update("UPDATE user_profiles SET allow_hanging_exercises = false WHERE user_id = ?", p.userId());
        }

        private WeeklyTrainingPlanResource leer(Participante p, long planId) {
            var principal = UserDetailsImpl.build(usuarios.findById(p.userId()).orElseThrow());
            return new TransactionTemplate(transacciones).execute(tx -> planes.byId(principal, planId).getBody());
        }

        private PlannedWorkoutResource sesion(WeeklyTrainingPlanResource plan, long id) {
            return plan.workouts().stream().filter(w -> w.plannedWorkoutId() == id).findFirst().orElseThrow();
        }

        private List<Map<String, Object>> contenido(long planId) {
            return jdbc.queryForList("SELECT pw.planned_workout_id, pwe.exercise_order, pwe.exercise_id,"
                    + " pwe.planned_sets, pwe.planned_reps, pwe.planned_duration_seconds, pwe.rest_seconds"
                    + " FROM planned_workouts pw JOIN planned_workout_exercises pwe ON pwe.planned_workout_id = pw.planned_workout_id"
                    + " WHERE pw.plan_id = ? ORDER BY pw.scheduled_date, pwe.exercise_order", planId);
        }

        @Test
        @DisplayName("F · sesion pendiente con un contenido: se marca INC-CAT-C39, no se inicia, las demas si")
        void f_sesionPendienteBloqueada() {
            var p = publicadoSinRestricciones();
            long afectada = con1303(p, null);
            assertFalse(sesion(leer(p, p.planId()), afectada).safetyBlocked(), "sin restriccion, compatible");

            prohibirColgarse(p);
            var w = sesion(leer(p, p.planId()), afectada);
            assertTrue(w.safetyBlocked());
            var inseguros = w.exercises().stream().filter(e -> e.safetyBlockReason() != null).toList();
            assertEquals(1, inseguros.size());
            assertEquals(1303L, inseguros.get(0).exerciseId());
            assertEquals("INC-CAT-C39", inseguros.get(0).safetyBlockReason());

            var error = assertThrows(WorkoutNotAvailableException.class,
                    () -> sesiones.handle(new StartWorkoutSessionCommand(p.userId(), afectada)));
            assertTrue(error.getMessage().contains("INC-CAT-C39"), error.getMessage());
            assertEquals("SCHEDULED", jdbc.queryForObject("SELECT status FROM planned_workouts WHERE planned_workout_id = ?",
                    String.class, afectada));
            long otra = jdbc.queryForObject("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ?"
                    + " AND planned_workout_id <> ? ORDER BY scheduled_date LIMIT 1", Long.class, p.planId(), afectada);
            assertTrue(sesiones.handle(new StartWorkoutSessionCommand(p.userId(), otra)).isPresent());
        }

        @Test
        @DisplayName("F · reparacion explicita sin sesiones empezadas: version 2 sin el contenido; version 1 intacta")
        void f_reparacionPorVersionNueva() {
            var p = publicadoSinRestricciones();
            con1303(p, null);
            prohibirColgarse(p);
            var historico = contenido(p.planId());

            var r = planning.repairPublishedPlanForSafety(p.userId(), SEMANA, HOY);
            assertEquals(Estado.REPARADA, r.estado(), r.toString());
            assertEquals(historico, contenido(p.planId()), "la version 1 no se toca");
            assertEquals("REPLACED", jdbc.queryForObject("SELECT status FROM weekly_training_plans WHERE plan_id = ?",
                    String.class, p.planId()));
            long v2 = jdbc.queryForObject("SELECT plan_id FROM weekly_training_plans WHERE user_id = ? AND status = 'ACTIVE'",
                    Long.class, p.userId());
            var nuevos = ejerciciosDelPlan(v2);
            assertFalse(nuevos.contains(1303L));
            assertTrue(interseccion(nuevos, ContencionTextoBandera.contenidos(SIN_COLGARSE.seguridad())).isEmpty());
            assertTrue(conBandera(nuevos, "requires_hanging").isEmpty());
        }

        @Test
        @DisplayName("G · sesion en curso: no se sustituye nada, el registro existente se conserva y no se versiona")
        void g_sesionEnCurso() {
            var p = publicadoSinRestricciones();
            long enCurso = jdbc.queryForObject("SELECT pw.planned_workout_id FROM planned_workouts pw"
                    + " JOIN planned_workout_exercises pwe ON pwe.planned_workout_id = pw.planned_workout_id"
                    + " JOIN exercises x ON x.exercise_id = pwe.exercise_id JOIN body_parts bp ON bp.body_part_id = x.body_part_id"
                    + " WHERE pw.plan_id = ? AND pwe.prescription_type = 'SETS_REPS' AND bp.code = 'waist'"
                    + " ORDER BY pw.scheduled_date LIMIT 1", Long.class, p.planId());
            long sesion = sesiones.handle(new StartWorkoutSessionCommand(p.userId(), enCurso)).orElseThrow().getId();
            // Un ejercicio ya registrado en la sesion abierta (el primero que no se va a tocar)
            long registrado = jdbc.queryForObject("SELECT planned_exercise_id FROM planned_workout_exercises"
                    + " WHERE planned_workout_id = ? ORDER BY exercise_order LIMIT 1", Long.class, enCurso);
            con1303(p, enCurso);
            long pe1303 = jdbc.queryForObject("SELECT planned_exercise_id FROM planned_workout_exercises"
                    + " WHERE planned_workout_id = ? AND exercise_id = 1303", Long.class, enCurso);
            if (registrado != pe1303)
                sesiones.handle(new RecordSessionExerciseCommand(p.userId(), sesion, registrado, (short) 2, 10, null,
                        null, null));
            int registrosAntes = jdbc.queryForObject("SELECT count(*) FROM workout_session_exercises WHERE session_id = ?",
                    Integer.class, sesion);
            prohibirColgarse(p);
            var antes = contenido(p.planId());

            var error = assertThrows(WorkoutNotAvailableException.class, () -> sesiones.handle(
                    new RecordSessionExerciseCommand(p.userId(), sesion, pe1303, (short) 2, 10, null, null, null)));
            assertTrue(error.getMessage().contains("Solo puede registrarse como omitido"), error.getMessage());
            assertTrue(sesiones.handle(new RecordSessionExerciseCommand(p.userId(), sesion, pe1303, (short) 0, 0, null,
                    null, SkipReason.OTHER)).isPresent(), "omitirlo si se admite");

            var r = planning.repairPublishedPlanForSafety(p.userId(), SEMANA, HOY);
            assertEquals(Estado.SESION_EN_CURSO, r.estado(), r.toString());
            assertEquals(antes, contenido(p.planId()), "nada se sustituye en silencio");
            assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ?",
                    Integer.class, p.userId()));
            assertTrue(jdbc.queryForObject("SELECT count(*) FROM workout_session_exercises WHERE session_id = ?",
                    Integer.class, sesion) >= registrosAntes, "no se pierde ningun registro");
        }
    }

    // =====================================================================

    /**
     * I · COHERENCIA TEXTO-BANDERA en todo el catalogo activo. Si el nombre o las instrucciones PIDEN colgarse, brazos
     * por encima de la cabeza o tumbarse/sentarse en el suelo, la bandera lo declara o el ejercicio esta retenido o
     * contenido para esa restriccion. Una migracion futura que relaje una bandera sin alinear el texto rompe esta
     * prueba en vez de llegar a un participante.
     */
    @Test
    @DisplayName("I · ningun ejercicio activo pide en su texto una accion restringida sin bandera ni contencion")
    void i_coherenciaTextoBandera() {
        var colgarse = Pattern.compile("\\bcu[eé]lgate\\b|\\bcolg[aá]ndote\\b", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        var dominadas = Pattern.compile("dominad", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        var cabeza = Pattern.compile("por encima de (la|tu) cabeza|sobre la cabeza", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        var suelo = Pattern.compile("(t[uú]mbate|acu[eé]state|si[eé]ntate|tumbad[oa]|sentad[oa]) (boca (arriba|abajo) |de lado |de espaldas )?"
                + "(en|sobre) (el suelo|el piso|una esterilla|la esterilla|una colchoneta)|posici[oó]n de (flexi[oó]n|plancha)",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        var conAparato = Pattern.compile("banco|bench|m[aá]quina|polea|aparato|bal[oó]n|pelota|step|plataforma|silla|caj[oó]n|soporte",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        var retenidos = Set.of(813L, 858L, 861L, 873L, 885L, 1192L, 1298L, 1374L);

        var incoherentes = new ArrayList<String>();
        jdbc.query("SELECT exercise_id, coalesce(name_es,'') n, coalesce(instructions_es,'') i, requires_hanging h,"
                + " requires_overhead o, requires_floor f FROM exercises WHERE is_active", rs -> {
            long id = rs.getLong(1);
            String nombre = rs.getString(2), ins = rs.getString(3);
            var c = ContencionTextoBandera.registro().get(id);
            if (retenidos.contains(id)) return;
            if ((colgarse.matcher(ins).find() || dominadas.matcher(nombre).find()) && !rs.getBoolean(4)
                    && (c == null || !c.restricciones().contains(ContencionTextoBandera.Restriccion.COLGARSE)))
                incoherentes.add(id + " colgarse: " + nombre);
            if ((cabeza.matcher(ins).find() || cabeza.matcher(nombre).find()) && !rs.getBoolean(5)
                    && (c == null || !c.restricciones().contains(ContencionTextoBandera.Restriccion.SOBRE_LA_CABEZA)))
                incoherentes.add(id + " brazos arriba: " + nombre);
            if (suelo.matcher(ins).find() && !conAparato.matcher(ins).find() && !rs.getBoolean(6)
                    && (c == null || !c.restricciones().contains(ContencionTextoBandera.Restriccion.SUELO)))
                incoherentes.add(id + " suelo: " + nombre);
        });
        assertTrue(incoherentes.isEmpty(), String.join("\n", incoherentes));
    }

    @Test
    @DisplayName("I · la contencion no modifica el catalogo: las banderas de los 8 contenidos siguen sin la restriccion")
    void i_sinCambiosDeDatos() {
        // Las banderas actuales de los contenidos son las que dejo V38 (ninguna relajada mas, ninguna endurecida).
        var filas = jdbc.queryForList("SELECT exercise_id, requires_hanging, requires_overhead, requires_floor, is_active"
                + " FROM exercises WHERE exercise_id = ANY (?)",
                (Object) ContencionTextoBandera.registro().keySet().toArray(Long[]::new));
        assertEquals(8, filas.size());
        for (var f : filas) {
            long id = ((Number) f.get("exercise_id")).longValue();
            var r = ContencionTextoBandera.registro().get(id).restricciones();
            if (r.contains(ContencionTextoBandera.Restriccion.COLGARSE)) assertEquals(false, f.get("requires_hanging"), "id " + id);
            if (r.contains(ContencionTextoBandera.Restriccion.SOBRE_LA_CABEZA)) assertEquals(false, f.get("requires_overhead"), "id " + id);
            if (r.contains(ContencionTextoBandera.Restriccion.SUELO)) assertEquals(false, f.get("requires_floor"), "id " + id);
            assertEquals(true, f.get("is_active"), "no se desactiva ni se borra: " + id);
        }
    }

    // =====================================================================

    /** Patrones de texto que PIDEN la accion restringida (los mismos de la prueba I). */
    static final Pattern COLGARSE_TXT = Pattern.compile("\\bcu[eé]lgate\\b|\\bcolg[aá]ndote\\b|\\bcolgad[oa]\\b|dominad",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    static final Pattern CABEZA_TXT = Pattern.compile("por encima de (la|tu) cabeza|sobre la cabeza",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    static final Pattern SUELO_TXT = Pattern.compile("(t[uú]mbate|acu[eé]state|si[eé]ntate|tumbad[oa]|sentad[oa])"
            + " (boca (arriba|abajo) |de lado |de espaldas )?(en|sobre) (el suelo|el piso|una esterilla|la esterilla|"
            + "una colchoneta)|posici[oó]n de (flexi[oó]n|plancha)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    /** La contencion que cada id tenia en la PR #55 original (26), para comprobar por que se libero. */
    static final Map<Long, Set<ContencionTextoBandera.Restriccion>> ORIGINAL = Map.ofEntries(
            Map.entry(316L, EnumSet.of(ContencionTextoBandera.Restriccion.COLGARSE, ContencionTextoBandera.Restriccion.SOBRE_LA_CABEZA)),
            Map.entry(1152L, EnumSet.of(ContencionTextoBandera.Restriccion.COLGARSE)),
            Map.entry(1272L, EnumSet.of(ContencionTextoBandera.Restriccion.SOBRE_LA_CABEZA)),
            Map.entry(1309L, EnumSet.of(ContencionTextoBandera.Restriccion.SOBRE_LA_CABEZA)),
            Map.entry(1322L, EnumSet.of(ContencionTextoBandera.Restriccion.SOBRE_LA_CABEZA)),
            Map.entry(55L, EnumSet.of(ContencionTextoBandera.Restriccion.SUELO)),
            Map.entry(224L, EnumSet.of(ContencionTextoBandera.Restriccion.SUELO)),
            Map.entry(637L, EnumSet.of(ContencionTextoBandera.Restriccion.SUELO)),
            Map.entry(741L, EnumSet.of(ContencionTextoBandera.Restriccion.SUELO)),
            Map.entry(875L, EnumSet.of(ContencionTextoBandera.Restriccion.SUELO)),
            Map.entry(1062L, EnumSet.of(ContencionTextoBandera.Restriccion.SUELO)),
            Map.entry(1063L, EnumSet.of(ContencionTextoBandera.Restriccion.SUELO)),
            Map.entry(1093L, EnumSet.of(ContencionTextoBandera.Restriccion.SUELO)),
            Map.entry(1095L, EnumSet.of(ContencionTextoBandera.Restriccion.SUELO)),
            Map.entry(1156L, EnumSet.of(ContencionTextoBandera.Restriccion.SUELO)),
            Map.entry(1181L, EnumSet.of(ContencionTextoBandera.Restriccion.SUELO)),
            Map.entry(1193L, EnumSet.of(ContencionTextoBandera.Restriccion.SUELO)),
            Map.entry(1311L, EnumSet.of(ContencionTextoBandera.Restriccion.SUELO)),
            // Liberados en parte: la restriccion que se quita (la que se mantiene sigue en el registro)
            Map.entry(1301L, EnumSet.of(ContencionTextoBandera.Restriccion.SOBRE_LA_CABEZA)),
            Map.entry(1303L, EnumSet.of(ContencionTextoBandera.Restriccion.SUELO)),
            Map.entry(1304L, EnumSet.of(ContencionTextoBandera.Restriccion.SUELO)));

    /**
     * L · POR QUE SE LIBERA CADA ID. Para cada restriccion que la PR #55 original aplicaba y la reducida ya no aplica,
     * el texto actual (nombre e instrucciones, tras V49) ya no pide esa accion, o la bandera del catalogo ya la declara
     * (y entonces el filtro de seguridad normal la excluye). Si no, la liberacion no estaria justificada y esta prueba
     * falla: ningun id sale porque haga falta para generar un plan.
     */
    @Test
    @DisplayName("L · liberados: el texto corregido por V49 ya no pide la accion restringida (o la bandera la declara)")
    void l_liberadosJustificados() {
        assertEquals(ContencionTextoBandera.LIBERADOS_POR_V49.size() + 3, ORIGINAL.size());
        assertTrue(ORIGINAL.keySet().containsAll(ContencionTextoBandera.LIBERADOS_POR_V49));
        assertTrue(interseccion(ContencionTextoBandera.LIBERADOS_POR_V49, ContencionTextoBandera.registro().keySet()).isEmpty());
        var injustificados = new ArrayList<String>();
        ORIGINAL.forEach((id, quitadas) -> {
            var f = jdbc.queryForMap("SELECT coalesce(name_es,'') n, coalesce(instructions_es,'') i, requires_hanging h,"
                    + " requires_overhead o, requires_floor s, is_active a FROM exercises WHERE exercise_id = ?", id);
            String texto = f.get("n") + " " + f.get("i");
            assertEquals(true, f.get("a"), "no se desactiva: " + id);
            var mantiene = Optional.ofNullable(ContencionTextoBandera.registro().get(id))
                    .map(ContencionTextoBandera.Contencion::restricciones).orElse(Set.of());
            for (var r : quitadas) {
                assertFalse(mantiene.contains(r), id + " sigue contenido por " + r);
                boolean pide = switch (r) {
                    case COLGARSE -> COLGARSE_TXT.matcher(texto).find();
                    case SOBRE_LA_CABEZA -> CABEZA_TXT.matcher(texto).find();
                    case SUELO -> SUELO_TXT.matcher(texto).find();
                };
                boolean declarada = switch (r) {
                    case COLGARSE -> (Boolean) f.get("h");
                    case SOBRE_LA_CABEZA -> (Boolean) f.get("o");
                    case SUELO -> (Boolean) f.get("s");
                };
                if (pide && !declarada) injustificados.add(id + " " + r + ": " + texto);
            }
        });
        assertTrue(injustificados.isEmpty(), String.join("\n", injustificados));
    }

    /**
     * L · LOS LIBERADOS VUELVEN DE VERDAD, Y SOLO SEGUN SUS BANDERAS. Para cada restriccion, todo liberado que el
     * perfil sin restricciones recibe llega tambien al perfil restringido, salvo si su bandera declara esa restriccion
     * (entonces lo excluye el filtro de seguridad normal, no INC-CAT-C39).
     */
    @Test
    @DisplayName("L · liberados: vuelven al conjunto elegible salvo que su propia bandera los excluya")
    void l_liberadosVuelven() {
        var libre = elegibles(usuario(GoalType.GENERAL_FITNESS, FitnessLevel.BEGINNER, TrainingLocation.GYM, GIMNASIO, LIBRE));
        var candidatos = interseccion(libre, ContencionTextoBandera.LIBERADOS_POR_V49);
        assertFalse(candidatos.isEmpty(), "el gimnasio del caso 39 recibe liberados");
        for (var r : List.of(SIN_COLGARSE, SIN_CABEZA, new Restricciones(true, false, false), SIN_NADA)) {
            var e = elegibles(usuario(GoalType.GENERAL_FITNESS, FitnessLevel.BEGINNER, TrainingLocation.GYM, GIMNASIO, r));
            var esperados = new TreeSet<>(candidatos);
            if (r.sinColgarse()) esperados.removeAll(conBandera(candidatos, "requires_hanging"));
            if (r.sinBrazosArriba()) esperados.removeAll(conBandera(candidatos, "requires_overhead"));
            if (r.sinSuelo()) esperados.removeAll(conBandera(candidatos, "requires_floor"));
            var faltan = new TreeSet<>(esperados);
            faltan.removeAll(e);
            assertTrue(faltan.isEmpty(), r + " no recibe liberados sin bandera: " + faltan);
        }
    }

    /**
     * R · LOS DOS PERFILES QUE LA PR #55 ORIGINAL DEJABA SIN PLAN (LOSE_WEIGHT principiante en casa, «sin suelo» y
     * «sin las tres»). Con la contencion reducida publican su semana: el validador obligatorio (V1-V25) la acepta, no
     * contiene nada contenido ni con bandera restringida, y cada sesion lleva cardio (V25 en LOSE_WEIGHT), que viene
     * de ejercicios de pie sin bandera de suelo.
     */
    @Test
    @DisplayName("R · LOSE_WEIGHT principiante en casa (sin suelo / sin las tres): semana valida sin prohibidos")
    void r_perfilesRecuperados() {
        for (var r : List.of(new Restricciones(true, false, false), SIN_NADA)) {
            long u = usuario(GoalType.LOSE_WEIGHT, FitnessLevel.BEGINNER, TrainingLocation.HOME, CASA, r);
            long plan = generar(u);
            assertEquals("ACTIVE", jdbc.queryForObject("SELECT status FROM weekly_training_plans WHERE plan_id = ?",
                    String.class, plan));
            var ids = ejerciciosDelPlan(plan);
            assertTrue(interseccion(ids, ContencionTextoBandera.contenidos(r.seguridad())).isEmpty(), r + " " + ids);
            assertTrue(conBandera(ids, "requires_floor").isEmpty(), r + " " + ids);
            if (r.sinColgarse()) assertTrue(conBandera(ids, "requires_hanging").isEmpty());
            if (r.sinBrazosArriba()) assertTrue(conBandera(ids, "requires_overhead").isEmpty());
            var sesionesSinCardio = jdbc.queryForList("SELECT pw.planned_workout_id FROM planned_workouts pw WHERE pw.plan_id = ?"
                    + " AND NOT EXISTS (SELECT 1 FROM planned_workout_exercises pwe JOIN exercises x ON x.exercise_id = pwe.exercise_id"
                    + " WHERE pwe.planned_workout_id = pw.planned_workout_id AND x.movement_pattern = 'LOCOMOTION_CARDIO')",
                    Long.class, plan);
            assertTrue(sesionesSinCardio.isEmpty(), r + " sesiones sin cardio: " + sesionesSinCardio);
        }
    }
}
