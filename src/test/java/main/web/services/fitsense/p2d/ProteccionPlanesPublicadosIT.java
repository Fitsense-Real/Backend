package main.web.services.fitsense.p2d;

import main.web.services.fitsense.execution.domain.exceptions.WorkoutNotAvailableException;
import main.web.services.fitsense.execution.domain.model.commands.RecordSessionExerciseCommand;
import main.web.services.fitsense.execution.domain.model.commands.StartWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.services.WorkoutSessionCommandService;
import main.web.services.fitsense.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import main.web.services.fitsense.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import main.web.services.fitsense.planning.application.internal.commandservices.PublishedPlanSafetyRepairService;
import main.web.services.fitsense.planning.application.internal.commandservices.PublishedPlanSafetyRepairService.Estado;
import main.web.services.fitsense.planning.domain.model.valueobjects.MandatorySlot;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PLANES YA PUBLICADOS con un ejercicio que hoy no es seguro: lectura marcada,
 * ejecucion rechazada y reparacion por version nueva sin tocar el historico.
 * <p>
 * PostgreSQL 16 efimero con V1-V33, flujo real, motor de reglas (IA
 * desactivada), sin llamadas a Replicate. El 1374 se inyecta en la semana como
 * si se hubiera publicado antes de la contencion.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
class ProteccionPlanesPublicadosIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired PlanningContextFacade planning;
    @Autowired UserProfileCommandService perfiles;
    @Autowired WorkoutSessionCommandService sesiones;
    @Autowired TrainingPlanController planes;
    @Autowired UserRepository usuarios;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transacciones;

    static final AtomicInteger SECUENCIA = new AtomicInteger();
    static final LocalDate HOY = LocalDate.now();
    /** La semana que viene: todas sus sesiones estan abiertas y sin vencer. */
    static final LocalDate SEMANA = TrainingWeek.containing(HOY).startDate().plusWeeks(1);

    /** Solo rellenos: ningun patron que cuente como slot 4C. */
    static final String SIN_SLOT = " AND x.movement_pattern NOT IN ('KNEE_DOMINANT','HIP_HINGE','HIP_EXTENSION',"
            + "'HORIZONTAL_PUSH','VERTICAL_PUSH','HORIZONTAL_PULL','VERTICAL_PULL')";

    static final List<String> EQUIPAMIENTO = List.of("dumbbell", "barbell", "cable", "leverage machine", "bench", "band");

    record Participante(long userId, long planId) {}

    private Participante participante(LocalDate semana, Boolean permiteSuelo, List<String> equipamiento) {
        int n = SECUENCIA.incrementAndGet();
        String email = "publicados" + n + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Prueba', now(), now())", email);
        long userId = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(userId, LocalDate.of(1992, 5, 10), BiologicalSex.FEMALE,
                new BigDecimal("165"), new BigDecimal("68"), null, FitnessLevel.INTERMEDIATE, GoalType.GAIN_MUSCLE,
                "prueba de planes publicados", TrainingLocation.GYM, (short) 3,
                List.of((short) 1, (short) 3, (short) 5), (short) 45, equipamiento,
                List.of(), "Ninguna", true, permiteSuelo, true, true, true));
        long plan = planning.generateWeeklyPlan(userId, semana, PlanAdjustment.none(), null, null)
                .orElseThrow(() -> new AssertionError("no se genero la semana"));
        return new Participante(userId, plan);
    }

    private Participante participante() {
        return participante(SEMANA, true, EQUIPAMIENTO);
    }

    private Participante conBarra() {
        var con = new ArrayList<>(EQUIPAMIENTO);
        con.add("pull-up bar");
        return participante(SEMANA, true, con);
    }

    /** Un relleno del plan (de cintura si hay) pasa a ser el 1374, como si se hubiera publicado antes de contenerlo. */
    private long con1374(Participante p) {
        var fila = jdbc.queryForMap("SELECT pwe.planned_exercise_id AS pe, pw.planned_workout_id AS pw"
                + " FROM planned_workout_exercises pwe"
                + " JOIN planned_workouts pw ON pw.planned_workout_id = pwe.planned_workout_id"
                + " JOIN exercises x ON x.exercise_id = pwe.exercise_id"
                + " JOIN body_parts bp ON bp.body_part_id = x.body_part_id"
                + " WHERE pw.plan_id = ? AND pwe.prescription_type = 'SETS_REPS'" + SIN_SLOT
                + " ORDER BY (bp.code = 'waist') DESC, pw.scheduled_date, pwe.exercise_order LIMIT 1", p.planId());
        long pe = ((Number) fila.get("pe")).longValue();
        assertFalse(esSlot(jdbc.queryForObject("SELECT x.movement_pattern FROM planned_workout_exercises pwe JOIN"
                + " exercises x ON x.exercise_id = pwe.exercise_id WHERE pwe.planned_exercise_id = ?",
                String.class, pe)), "premisa: un relleno");
        jdbc.update("UPDATE planned_workout_exercises SET exercise_id = 1374 WHERE planned_exercise_id = ?", pe);
        return ((Number) fila.get("pw")).longValue();
    }

    private static boolean esSlot(String patron) {
        return MandatorySlot.ofMovementPattern(patron).isPresent();
    }

    /** El contenido de un plan, fila a fila: lo que no debe cambiar al repararlo. */
    private List<Map<String, Object>> contenido(long planId) {
        return jdbc.queryForList("SELECT pw.planned_workout_id, pw.scheduled_date, pw.focus_code,"
                + " pw.expected_duration_minutes, pwe.exercise_order, pwe.exercise_id, pwe.prescription_type,"
                + " pwe.planned_sets, pwe.planned_reps, pwe.planned_duration_seconds, pwe.rest_seconds"
                + " FROM planned_workouts pw JOIN planned_workout_exercises pwe"
                + " ON pwe.planned_workout_id = pw.planned_workout_id WHERE pw.plan_id = ?"
                + " ORDER BY pw.scheduled_date, pwe.exercise_order", planId);
    }

    private WeeklyTrainingPlanResource leer(Participante p, long planId) {
        var principal = UserDetailsImpl.build(usuarios.findById(p.userId()).orElseThrow());
        // La app lo lee en una peticion web, con sesion abierta; aqui, en una transaccion.
        return new TransactionTemplate(transacciones).execute(tx -> planes.byId(principal, planId).getBody());
    }

    private PlannedWorkoutResource sesion(WeeklyTrainingPlanResource plan, long plannedWorkoutId) {
        return plan.workouts().stream().filter(w -> w.plannedWorkoutId() == plannedWorkoutId).findFirst().orElseThrow();
    }

    private PublishedPlanSafetyRepairService.Resultado reparar(Participante p) {
        return planning.repairPublishedPlanForSafety(p.userId(), SEMANA, HOY);
    }

    private int versiones(Participante p, LocalDate semana) {
        return jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ? AND week_start_date = ?",
                Integer.class, p.userId(), semana);
    }

    private int activas(Participante p, LocalDate semana) {
        return jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ? AND week_start_date = ?"
                + " AND status = 'ACTIVE'", Integer.class, p.userId(), semana);
    }

    private long activa(Participante p) {
        return jdbc.queryForObject("SELECT plan_id FROM weekly_training_plans WHERE user_id = ? AND week_start_date = ?"
                + " AND status = 'ACTIVE'", Long.class, p.userId(), SEMANA);
    }

    // =====================================================================

    @Nested
    @DisplayName("1 · lectura y ejecucion: una sesion con un ejercicio inseguro se marca y no se puede hacer")
    class Proteccion {

        @Test
        @DisplayName("1374 sin barra: la sesion sale bloqueada con su causa; las demas, no")
        void lecturaMarcada() {
            var p = participante();
            long afectada = con1374(p);
            var plan = leer(p, p.planId());

            for (var w : plan.workouts()) {
                if (w.plannedWorkoutId() == afectada) {
                    assertTrue(w.safetyBlocked());
                    assertNotNull(w.safetyBlockMessage());
                    var inseguros = w.exercises().stream().filter(e -> e.safetyBlockReason() != null).toList();
                    assertEquals(1, inseguros.size());
                    assertEquals(1374L, inseguros.get(0).exerciseId());
                    assertEquals("INC-CAT-1374", inseguros.get(0).safetyBlockReason());
                } else {
                    assertFalse(w.safetyBlocked(), "sesion " + w.plannedWorkoutId());
                    assertTrue(w.exercises().stream().allMatch(e -> e.safetyBlockReason() == null));
                }
            }
        }

        @Test
        @DisplayName("1374 sin barra: iniciar la sesion se rechaza en el backend; otra sesion se inicia normal")
        void inicioRechazado() {
            var p = participante();
            long afectada = con1374(p);
            var error = assertThrows(WorkoutNotAvailableException.class,
                    () -> sesiones.handle(new StartWorkoutSessionCommand(p.userId(), afectada)));
            assertTrue(error.getMessage().startsWith("SESION_BLOQUEADA_POR_SEGURIDAD"), error.getMessage());
            assertTrue(error.getMessage().contains("INC-CAT-1374"), error.getMessage());
            assertEquals("SCHEDULED", jdbc.queryForObject("SELECT status FROM planned_workouts WHERE planned_workout_id = ?",
                    String.class, afectada), "no se marco en curso");
            assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM workout_sessions WHERE planned_workout_id = ?",
                    Integer.class, afectada));

            long otra = jdbc.queryForObject("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ?"
                    + " AND planned_workout_id <> ? ORDER BY scheduled_date LIMIT 1", Long.class, p.planId(), afectada);
            assertTrue(sesiones.handle(new StartWorkoutSessionCommand(p.userId(), otra)).isPresent());
        }

        @Test
        @DisplayName("sesion abierta ANTES del bloqueo: registrar el ejercicio inseguro como hecho se rechaza; omitirlo, no")
        void registroDeUnaSesionYaAbierta() {
            var p = participante();
            long sesionPlanificada = jdbc.queryForObject("SELECT pw.planned_workout_id FROM planned_workouts pw"
                    + " JOIN planned_workout_exercises pwe ON pwe.planned_workout_id = pw.planned_workout_id"
                    + " JOIN exercises x ON x.exercise_id = pwe.exercise_id JOIN body_parts bp ON bp.body_part_id = x.body_part_id"
                    + " WHERE pw.plan_id = ? AND pwe.prescription_type = 'SETS_REPS'" + SIN_SLOT
                    + " ORDER BY (bp.code = 'waist') DESC, pw.scheduled_date, pwe.exercise_order LIMIT 1", Long.class, p.planId());
            long sesion = sesiones.handle(new StartWorkoutSessionCommand(p.userId(), sesionPlanificada)).orElseThrow().getId();
            assertEquals(sesionPlanificada, con1374(p));
            long pe = jdbc.queryForObject("SELECT planned_exercise_id FROM planned_workout_exercises"
                    + " WHERE planned_workout_id = ? AND exercise_id = 1374", Long.class, sesionPlanificada);

            var error = assertThrows(WorkoutNotAvailableException.class, () -> sesiones.handle(
                    new RecordSessionExerciseCommand(p.userId(), sesion, pe, (short) 2, 26, null, null, null)));
            assertTrue(error.getMessage().contains("Solo puede registrarse como omitido"), error.getMessage());
            assertTrue(sesiones.handle(new RecordSessionExerciseCommand(p.userId(), sesion, pe, (short) 0, 0, null,
                    null, SkipReason.OTHER)).isPresent());
        }

        @Test
        @DisplayName("1374 con barra y sin restricciones: nada bloqueado, se inicia, y la reparacion no hace nada")
        void compatibleNoSeToca() {
            var p = conBarra();
            long afectada = con1374(p);
            var antes = contenido(p.planId());
            assertFalse(sesion(leer(p, p.planId()), afectada).safetyBlocked());

            var r = reparar(p);
            assertEquals(Estado.NADA_QUE_REPARAR, r.estado(), r.toString());
            assertEquals(1, versiones(p, SEMANA));
            assertEquals(antes, contenido(p.planId()));
            assertTrue(sesiones.handle(new StartWorkoutSessionCommand(p.userId(), afectada)).isPresent());
        }
    }

    @Nested
    @DisplayName("2 · reparacion por version nueva")
    class Reparacion {

        @Test
        @DisplayName("1374 sin barra: version 2 con el 1374 sustituido; la version 1 intacta y REPLACED; una sola activa")
        void sustitucionExitosa() {
            var p = participante();
            long afectada = con1374(p);
            var historico = contenido(p.planId());

            var r = reparar(p);
            assertEquals(Estado.REPARADA, r.estado(), r.toString());
            long nuevo = r.planNuevo();
            assertEquals(1, r.sustituciones().size());
            var s = r.sustituciones().get(0);
            assertEquals(1374L, s.quitado());
            assertEquals("INC-CAT-1374", s.motivo());
            assertEquals("SUST-SEG-1", s.regla());

            // Historico: la version 1 conserva su contenido exacto, con el 1374.
            assertEquals(historico, contenido(p.planId()), "la version anterior se modifico");
            assertEquals("REPLACED", jdbc.queryForObject("SELECT status FROM weekly_training_plans WHERE plan_id = ?",
                    String.class, p.planId()));
            assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM planned_workouts WHERE plan_id = ?"
                    + " AND status <> 'REPLACED'", Integer.class, p.planId()));

            // Version nueva: hija de la anterior, una sola activa, misma semana.
            var fila = jdbc.queryForMap("SELECT plan_version, parent_plan_id, status, adjustment_applied,"
                    + " adjustment_reason, output_snapshot::text AS salida FROM weekly_training_plans WHERE plan_id = ?", nuevo);
            assertEquals(2, ((Number) fila.get("plan_version")).intValue());
            assertEquals(p.planId(), ((Number) fila.get("parent_plan_id")).longValue());
            assertEquals("ACTIVE", fila.get("status"));
            assertEquals("SAFETY_REPAIR", fila.get("adjustment_applied"));
            assertTrue(String.valueOf(fila.get("adjustment_reason")).contains("1374 por " + s.puesto()));
            var salida = String.valueOf(fila.get("salida"));
            assertTrue(salida.contains("SAFETY_REPAIR_V1") && salida.contains("SUST-SEG-1")
                    && salida.contains("\"repaired_plan_id\": " + p.planId()), salida);
            assertEquals(1, activas(p, SEMANA));
            assertEquals(nuevo, activa(p));

            // Contenido: identico salvo la posicion del 1374.
            var nueva = contenido(nuevo);
            assertEquals(historico.size(), nueva.size());
            for (int i = 0; i < historico.size(); i++) {
                var a = new HashMap<>(historico.get(i));
                var b = new HashMap<>(nueva.get(i));
                a.remove("planned_workout_id");
                b.remove("planned_workout_id");
                if (((Number) a.get("exercise_id")).longValue() == 1374L) {
                    assertEquals(s.puesto(), ((Number) b.get("exercise_id")).longValue());
                    assertFalse(esSlot(jdbc.queryForObject("SELECT movement_pattern FROM exercises WHERE exercise_id = ?",
                            String.class, s.puesto())), "un relleno por un relleno");
                    assertEquals(a.get("scheduled_date"), b.get("scheduled_date"));
                    assertEquals(a.get("exercise_order"), b.get("exercise_order"));
                } else {
                    assertEquals(a, b, "cambio fuera de la sustitucion");
                }
            }

            // Lectura y ejecucion de la version nueva: nada bloqueado.
            var plan = leer(p, nuevo);
            assertTrue(plan.workouts().stream().noneMatch(PlannedWorkoutResource::safetyBlocked));
            long reparada = jdbc.queryForObject("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ?"
                    + " AND scheduled_date = (SELECT scheduled_date FROM planned_workouts WHERE planned_workout_id = ?)",
                    Long.class, nuevo, afectada);
            assertTrue(sesiones.handle(new StartWorkoutSessionCommand(p.userId(), reparada)).isPresent());
            // La version 1 sigue consultable, tal cual.
            assertEquals(p.planId(), leer(p, p.planId()).planId());
        }

        @Test
        @DisplayName("reintento duplicado: la segunda reparacion no encuentra nada y no crea otra version")
        void reintentoDuplicado() {
            var p = participante();
            con1374(p);
            assertEquals(Estado.REPARADA, reparar(p).estado());
            var r = reparar(p);
            assertEquals(Estado.NADA_QUE_REPARAR, r.estado(), r.toString());
            assertEquals(2, versiones(p, SEMANA));
            assertEquals(1, activas(p, SEMANA));
        }

        @Test
        @DisplayName("dos reparaciones simultaneas: una publica, la otra no; nunca dos versiones activas")
        void reparacionesSimultaneas() throws Exception {
            var p = participante();
            con1374(p);
            var pool = Executors.newFixedThreadPool(2);
            var salida = new CountDownLatch(1);
            try {
                var a = pool.submit(() -> { salida.await(); return reparar(p); });
                var b = pool.submit(() -> { salida.await(); return reparar(p); });
                salida.countDown();
                var estados = List.of(a.get(120, TimeUnit.SECONDS).estado(), b.get(120, TimeUnit.SECONDS).estado());
                assertEquals(1, estados.stream().filter(e -> e == Estado.REPARADA).count(), estados.toString());
                assertTrue(estados.stream().allMatch(e -> e == Estado.REPARADA || e == Estado.CAMBIO_CONCURRENTE
                        || e == Estado.NADA_QUE_REPARAR), estados.toString());
            } finally {
                pool.shutdownNow();
            }
            assertEquals(2, versiones(p, SEMANA));
            assertEquals(1, activas(p, SEMANA));
        }

        @Test
        @DisplayName("sin sustituto seguro: no se publica nada; la sesion sigue bloqueada y lo dice")
        void sinSustituto() {
            var p = participante(SEMANA, false, EQUIPAMIENTO);
            var fila = jdbc.queryForMap("SELECT pw.planned_workout_id AS pw, pwe.exercise_id AS x, ex.movement_pattern AS patron"
                    + " FROM planned_workouts pw JOIN planned_workout_exercises pwe ON pwe.planned_workout_id = pw.planned_workout_id"
                    + " JOIN exercises ex ON ex.exercise_id = pwe.exercise_id WHERE pw.plan_id = ?"
                    + " AND ex.movement_pattern IN ('KNEE_DOMINANT','HIP_HINGE','HIP_EXTENSION','HORIZONTAL_PUSH','VERTICAL_PUSH',"
                    + "'HORIZONTAL_PULL','VERTICAL_PULL') ORDER BY pw.scheduled_date, pwe.exercise_order LIMIT 1", p.planId());
            long afectada = ((Number) fila.get("pw")).longValue();
            var slot = MandatorySlot.ofMovementPattern((String) fila.get("patron")).orElseThrow();
            var patrones = jdbc.queryForList("SELECT DISTINCT movement_pattern FROM exercises WHERE movement_pattern IS NOT NULL",
                    String.class).stream().filter(x -> MandatorySlot.ofMovementPattern(x).equals(Optional.of(slot))).toList();
            var tocados = jdbc.queryForList("SELECT exercise_id FROM exercises WHERE movement_pattern = ANY(?)"
                    + " AND requires_floor IS DISTINCT FROM TRUE", Long.class, (Object) patrones.toArray(String[]::new));
            var historico = contenido(p.planId());
            jdbc.update("UPDATE exercises SET requires_floor = TRUE WHERE exercise_id = ANY(?)",
                    (Object) tocados.toArray(Long[]::new));
            try {
                var r = reparar(p);
                assertEquals(Estado.SIN_SUSTITUTO, r.estado(), r.toString());
                assertTrue(r.problemas().get(0).startsWith("SEGURIDAD: el ejercicio "), r.problemas().toString());
                assertNull(r.planNuevo());
                assertEquals(1, versiones(p, SEMANA));
                assertEquals(p.planId(), activa(p));
                assertEquals(historico, contenido(p.planId()));
                var w = sesion(leer(p, p.planId()), afectada);
                assertTrue(w.safetyBlocked());
                assertTrue(w.exercises().stream().anyMatch(e -> "suelo".equals(e.safetyBlockReason())));
                assertThrows(WorkoutNotAvailableException.class,
                        () -> sesiones.handle(new StartWorkoutSessionCommand(p.userId(), afectada)));
            } finally {
                jdbc.update("UPDATE exercises SET requires_floor = FALSE WHERE exercise_id = ANY(?)",
                        (Object) tocados.toArray(Long[]::new));
            }
        }

        @Test
        @DisplayName("semana ya empezada (una sesion terminada): no se versiona; queda bloqueada y requiere decision")
        void semanaEmpezada() {
            var p = participante();
            long afectada = con1374(p);
            jdbc.update("UPDATE planned_workouts SET status = 'COMPLETED' WHERE plan_id = ? AND planned_workout_id <> ?"
                    + " AND scheduled_date = (SELECT min(scheduled_date) FROM planned_workouts WHERE plan_id = ?"
                    + " AND planned_workout_id <> ?)", p.planId(), afectada, p.planId(), afectada);
            var historico = contenido(p.planId());

            var r = reparar(p);
            assertEquals(Estado.SEMANA_EMPEZADA, r.estado(), r.toString());
            assertEquals(1, versiones(p, SEMANA));
            assertEquals(historico, contenido(p.planId()));
            assertTrue(sesion(leer(p, p.planId()), afectada).safetyBlocked());
        }

        @Test
        @DisplayName("sesion en curso: no se versiona hasta que termine")
        void sesionEnCurso() {
            var p = participante();
            long afectada = con1374(p);
            long otra = jdbc.queryForObject("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ?"
                    + " AND planned_workout_id <> ? ORDER BY scheduled_date LIMIT 1", Long.class, p.planId(), afectada);
            sesiones.handle(new StartWorkoutSessionCommand(p.userId(), otra));
            assertEquals(Estado.SESION_EN_CURSO, reparar(p).estado());
            assertEquals(1, versiones(p, SEMANA));
        }

        @Test
        @DisplayName("semana pasada: es historico; no se repara y se sigue consultando tal cual")
        void semanaPasada() {
            var pasada = TrainingWeek.containing(HOY).startDate().minusWeeks(2);
            var p = participante(pasada, true, EQUIPAMIENTO);
            con1374(p);
            var historico = contenido(p.planId());
            var r = planning.repairPublishedPlanForSafety(p.userId(), pasada, HOY);
            assertEquals(Estado.SEMANA_PASADA, r.estado(), r.toString());
            assertEquals(1, versiones(p, pasada));
            assertEquals(historico, contenido(p.planId()));
            assertEquals(p.planId(), leer(p, p.planId()).planId());
        }
    }

    @Nested
    @DisplayName("3 · la consulta de exposicion de solo lectura mide lo que el backend bloquea")
    class Exposicion {

        /** Las consultas del archivo, en orden, sin BEGIN/ROLLBACK. */
        private List<String> consultas() throws Exception {
            var sql = java.nio.file.Files.readString(
                    java.nio.file.Path.of("docs/FitSense/consultas/exposicion_planes_publicados.sql"));
            var limpio = Arrays.stream(sql.split("\n")).filter(l -> !l.trim().startsWith("--"))
                    .collect(java.util.stream.Collectors.joining("\n"));
            return Arrays.stream(limpio.split(";")).map(String::trim)
                    .filter(q -> q.startsWith("WITH") || q.startsWith("SELECT")).toList();
        }

        @Test
        @DisplayName("1374 sin barra cuenta como incompatible ACTIVO_O_FUTURO; con barra, no; sin escribir nada")
        void mideLaExposicion() throws Exception {
            var incompatible = participante();
            long sesionInsegura = con1374(incompatible);
            var compatible = conBarra();
            con1374(compatible);
            var q = consultas();
            assertEquals(6, q.size(), "el archivo cambio de forma");

            var detalle = jdbc.queryForList(q.get(1));
            assertTrue(detalle.stream().anyMatch(f -> ((Number) f.get("user_id")).longValue() == incompatible.userId()
                    && ((Number) f.get("planned_workout_id")).longValue() == sesionInsegura
                    && "sin_barra".equals(f.get("motivo"))), detalle.toString());
            assertTrue(detalle.stream().noneMatch(f -> ((Number) f.get("user_id")).longValue() == compatible.userId()),
                    detalle.toString());

            // La misma sesion que la consulta cuenta es la que el backend bloquea.
            assertTrue(sesion(leer(incompatible, incompatible.planId()), sesionInsegura).safetyBlocked());

            var resumen = jdbc.queryForList(q.get(0)).stream()
                    .filter(f -> "ACTIVO_O_FUTURO".equals(f.get("alcance"))).findFirst().orElseThrow();
            assertTrue(((Number) resumen.get("participantes_incompatibles")).intValue()
                    < ((Number) resumen.get("participantes_con_1374")).intValue(), resumen.toString());
            assertTrue(jdbc.queryForList(q.get(5)).isEmpty(), "dos versiones activas de una semana");
            for (var consulta : q) jdbc.queryForList(consulta);
        }
    }
}
