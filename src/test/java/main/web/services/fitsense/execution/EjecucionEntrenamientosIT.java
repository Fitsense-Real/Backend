package main.web.services.fitsense.execution;

import main.web.services.fitsense.execution.domain.exceptions.SessionAlreadyInProgressException;
import main.web.services.fitsense.execution.domain.exceptions.WorkoutNotAvailableException;
import main.web.services.fitsense.execution.domain.model.commands.*;
import main.web.services.fitsense.execution.domain.services.WorkoutSessionCommandService;
import main.web.services.fitsense.execution.infrastructure.scheduling.StaleSessionScheduler;
import main.web.services.fitsense.execution.interfaces.acl.ExecutionContextFacade;
import main.web.services.fitsense.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import main.web.services.fitsense.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.interfaces.acl.PlanningContextFacade;
import main.web.services.fitsense.planning.interfaces.rest.TrainingPlanController;
import main.web.services.fitsense.planning.interfaces.rest.resources.SkipWorkoutResource;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.BiologicalSex;
import main.web.services.fitsense.profiling.domain.model.valueobjects.FitnessLevel;
import main.web.services.fitsense.profiling.domain.model.valueobjects.GoalType;
import main.web.services.fitsense.profiling.domain.model.valueobjects.TrainingLocation;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import main.web.services.fitsense.shared.domain.exceptions.DomainRuleViolationException;
import main.web.services.fitsense.shared.domain.model.valueobjects.SkipReason;
import main.web.services.fitsense.shared.domain.model.valueobjects.TrainingWeek;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * EJECUCION DE ENTRENAMIENTOS: consultar, iniciar, registrar, cerrar, omitir,
 * interrumpir y reintentar, sobre semanas reales del motor de reglas.
 * <p>
 * PostgreSQL 16 efimero con V1-V33, IA desactivada, sin llamadas a Replicate.
 * Lo que se comprueba es lo que necesitan la adherencia y la adaptacion: lo
 * planificado intacto, lo ejecutado completo, un solo intento que cuenta por
 * entrenamiento y ningun ejercicio inseguro registrado como hecho.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
class EjecucionEntrenamientosIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired PlanningContextFacade planning;
    @Autowired UserProfileCommandService perfiles;
    @Autowired WorkoutSessionCommandService sesiones;
    @Autowired ExecutionContextFacade ejecucion;
    @Autowired StaleSessionScheduler colgadas;
    @Autowired TrainingPlanController planes;
    @Autowired UserRepository usuarios;
    @Autowired JdbcTemplate jdbc;
    @Autowired WebApplicationContext contexto;

    MockMvc http;

    static final AtomicInteger SECUENCIA = new AtomicInteger();
    /** La semana que viene: todas sus sesiones estan abiertas y sin vencer. */
    static final LocalDate SEMANA = TrainingWeek.containing(LocalDate.now()).startDate().plusWeeks(1);
    static final List<String> EQUIPAMIENTO = List.of("dumbbell", "barbell", "cable", "leverage machine", "bench", "band");

    @BeforeEach
    void http() {
        http = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    record Participante(long userId, long planId) {}

    /** Lo indicado de un ejercicio: el denominador de su cumplimiento. */
    record Indicado(long pe, String tipo, int sets, int reps, int segundos) {
        int repsTotal() { return sets * reps; }
        int segundosTotal() { return sets * segundos; }
        boolean duracion() { return "DURATION".equals(tipo); }
    }

    private Participante participante() {
        int n = SECUENCIA.incrementAndGet();
        String email = "ejecucion" + n + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Prueba', now(), now())", email);
        long userId = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(userId, LocalDate.of(1992, 5, 10), BiologicalSex.FEMALE,
                new BigDecimal("165"), new BigDecimal("68"), null, FitnessLevel.INTERMEDIATE, GoalType.GAIN_MUSCLE,
                "prueba de ejecucion", TrainingLocation.GYM, (short) 3,
                List.of((short) 1, (short) 3, (short) 5), (short) 45, EQUIPAMIENTO,
                List.of(), "Ninguna", true, true, true, true, true));
        long plan = planning.generateWeeklyPlan(userId, SEMANA, PlanAdjustment.none(), null, null)
                .orElseThrow(() -> new AssertionError("no se genero la semana"));
        return new Participante(userId, plan);
    }

    private List<Long> entrenamientos(Participante p) {
        return jdbc.queryForList("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ?"
                + " ORDER BY scheduled_date", Long.class, p.planId());
    }

    private List<Indicado> indicados(long plannedWorkoutId) {
        return jdbc.query("SELECT planned_exercise_id, prescription_type, planned_sets, planned_reps,"
                        + " planned_duration_seconds FROM planned_workout_exercises WHERE planned_workout_id = ?"
                        + " ORDER BY exercise_order",
                (rs, i) -> new Indicado(rs.getLong(1), rs.getString(2), rs.getInt(3), rs.getInt(4), rs.getInt(5)),
                plannedWorkoutId);
    }

    /** El contenido planificado, fila a fila: lo que ejecutar no debe tocar. */
    private List<Map<String, Object>> planificado(long planId) {
        return jdbc.queryForList("SELECT pw.planned_workout_id, pw.scheduled_date, pw.focus_code,"
                + " pw.expected_duration_minutes, pw.expires_at, pwe.planned_exercise_id, pwe.exercise_order,"
                + " pwe.exercise_id, pwe.prescription_type, pwe.planned_sets, pwe.planned_reps,"
                + " pwe.planned_duration_seconds, pwe.target_load_kg, pwe.rest_seconds"
                + " FROM planned_workouts pw JOIN planned_workout_exercises pwe"
                + " ON pwe.planned_workout_id = pw.planned_workout_id WHERE pw.plan_id = ?"
                + " ORDER BY pw.scheduled_date, pwe.exercise_order", planId);
    }

    private String estadoEntrenamiento(long plannedWorkoutId) {
        return jdbc.queryForObject("SELECT status FROM planned_workouts WHERE planned_workout_id = ?",
                String.class, plannedWorkoutId);
    }

    private Map<String, Object> fila(long sessionId) {
        return jdbc.queryForMap("SELECT * FROM workout_sessions WHERE session_id = ?", sessionId);
    }

    private int contando(long plannedWorkoutId) {
        return jdbc.queryForObject("SELECT count(*) FROM workout_sessions WHERE planned_workout_id = ?"
                + " AND counts_toward_adherence", Integer.class, plannedWorkoutId);
    }

    private long iniciar(Participante p, long plannedWorkoutId) {
        return sesiones.handle(new StartWorkoutSessionCommand(p.userId(), plannedWorkoutId)).orElseThrow().getId();
    }

    /** Registra una fraccion de lo indicado (1.0 = todo). */
    private void registrar(Participante p, long sessionId, Indicado e, double fraccion) {
        sesiones.handle(new RecordSessionExerciseCommand(p.userId(), sessionId, e.pe(), (short) e.sets(),
                e.duracion() ? null : (int) Math.round(e.repsTotal() * fraccion),
                e.duracion() ? (int) Math.round(e.segundosTotal() * fraccion) : null,
                new BigDecimal("12.50"), fraccion < 1 ? SkipReason.FATIGUE : null));
    }

    private void omitirEjercicio(Participante p, long sessionId, Indicado e, SkipReason causa) {
        sesiones.handle(new RecordSessionExerciseCommand(p.userId(), sessionId, e.pe(), (short) 0,
                e.duracion() ? null : 0, e.duracion() ? 0 : null, null, causa));
    }

    private void terminar(Participante p, long sessionId) {
        sesiones.handle(new FinishWorkoutSessionCommand(p.userId(), sessionId, (short) 7, (short) 4, (short) 41));
    }

    private UserDetailsImpl principal(Participante p) {
        return UserDetailsImpl.build(usuarios.findById(p.userId()).orElseThrow());
    }

    private void omitirEntrenamiento(Participante p, long plannedWorkoutId, SkipReason causa) {
        planes.skip(principal(p), plannedWorkoutId, new SkipWorkoutResource(causa));
    }

    private static <T> List<Object> enParalelo(int n, Callable<T> tarea) throws Exception {
        var pool = Executors.newFixedThreadPool(n);
        var salida = new CountDownLatch(1);
        try {
            var futuros = new ArrayList<Future<Object>>();
            for (int i = 0; i < n; i++)
                futuros.add(pool.submit(() -> {
                    salida.await();
                    try {
                        return tarea.call();
                    } catch (Exception e) {
                        return e;
                    }
                }));
            salida.countDown();
            var resultados = new ArrayList<>();
            for (var f : futuros) resultados.add(f.get(60, TimeUnit.SECONDS));
            return resultados;
        } finally {
            pool.shutdownNow();
        }
    }

    // =====================================================================

    @Nested
    @DisplayName("1 · flujo del MVP")
    class Flujo {

        @Test
        @DisplayName("consultar: la semana trae los entrenamientos con lo indicado y todos SCHEDULED")
        void consultar() throws Exception {
            var p = participante();
            var plan = entrenamientos(p);
            assertEquals(3, plan.size());
            http.perform(get("/api/v1/users/me/plan/" + p.planId()).with(user(principal(p))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.planId").value(p.planId()))
                    .andExpect(jsonPath("$.workouts.length()").value(3))
                    .andExpect(jsonPath("$.workouts[0].status").value("SCHEDULED"))
                    .andExpect(jsonPath("$.workouts[0].exercises[0].plannedExerciseId").isNumber())
                    .andExpect(jsonPath("$.workouts[0].safetyBlocked").value(false));
        }

        @Test
        @DisplayName("completo: iniciar, registrar todo, cerrar con esfuerzo y duracion -> COMPLETED, cuenta, plan intacto")
        void completo() throws Exception {
            var p = participante();
            long pw = entrenamientos(p).get(0);
            var antes = planificado(p.planId());

            long s = iniciar(p, pw);
            assertEquals("IN_PROGRESS", estadoEntrenamiento(pw));
            assertEquals("IN_PROGRESS", fila(s).get("status"));
            http.perform(get("/api/v1/sessions/current").with(user(principal(p))))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.sessionId").value(s));

            var ejercicios = indicados(pw);
            ejercicios.forEach(e -> registrar(p, s, e, 1.0));
            terminar(p, s);

            var f = fila(s);
            assertEquals("COMPLETED", f.get("status"));
            assertEquals(0, new BigDecimal("100.00").compareTo((BigDecimal) f.get("completion_percentage")));
            assertEquals(true, f.get("counts_toward_adherence"));
            assertEquals(7, ((Number) f.get("session_rpe")).intValue());
            assertEquals(4, ((Number) f.get("satisfaction")).intValue());
            assertEquals(41, ((Number) f.get("active_minutes")).intValue());
            assertNotNull(f.get("ended_at"));
            assertEquals("APP_TRACKED", f.get("source"));
            assertEquals(p.planId(), ((Number) f.get("plan_id")).longValue());
            assertEquals("COMPLETED", estadoEntrenamiento(pw));

            // Lo ejecutado, ejercicio por ejercicio, contra lo indicado.
            var filas = jdbc.queryForList("SELECT planned_exercise_id, actual_sets, actual_reps_total,"
                    + " actual_duration_seconds, actual_load_kg, status FROM workout_session_exercises"
                    + " WHERE session_id = ? ORDER BY planned_exercise_id", s);
            assertEquals(ejercicios.size(), filas.size());
            for (var e : ejercicios) {
                var r = filas.stream().filter(x -> ((Number) x.get("planned_exercise_id")).longValue() == e.pe())
                        .findFirst().orElseThrow();
                assertEquals("COMPLETED", r.get("status"));
                if (e.duracion()) assertEquals(e.segundosTotal(), ((Number) r.get("actual_duration_seconds")).intValue());
                else assertEquals(e.repsTotal(), ((Number) r.get("actual_reps_total")).intValue());
            }
            // Lo planificado no cambio salvo el estado.
            assertEquals(antes, planificado(p.planId()));

            // Consultar despues, por HTTP: la sesion con sus ejercicios.
            http.perform(get("/api/v1/sessions/" + s).with(user(principal(p))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("COMPLETED"))
                    .andExpect(jsonPath("$.countsTowardAdherence").value(true))
                    .andExpect(jsonPath("$.exercises.length()").value(ejercicios.size()));
            http.perform(get("/api/v1/sessions/current").with(user(principal(p))))
                    .andExpect(status().isNoContent());
            http.perform(get("/api/v1/users/me/plan/" + p.planId()).with(user(principal(p))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.workouts[0].status").value("COMPLETED"))
                    .andExpect(jsonPath("$.workouts[0].exercises.length()").value(ejercicios.size()));
        }

        @Test
        @DisplayName("parcial: la mitad de los ejercicios -> sesion PARTIAL que cuenta, entrenamiento PARTIAL")
        void parcial() {
            var p = participante();
            long pw = entrenamientos(p).get(0);
            var ejercicios = indicados(pw);
            assertTrue(ejercicios.size() >= 4, "premisa: al menos cuatro ejercicios");
            long s = iniciar(p, pw);
            int hechos = (ejercicios.size() + 1) / 2;
            for (int i = 0; i < ejercicios.size(); i++)
                if (i < hechos) registrar(p, s, ejercicios.get(i), 1.0);
                else omitirEjercicio(p, s, ejercicios.get(i), SkipReason.LACK_OF_TIME);
            terminar(p, s);

            var f = fila(s);
            double pct = ((BigDecimal) f.get("completion_percentage")).doubleValue();
            assertTrue(pct >= 30 && pct < 80, "porcentaje " + pct);
            assertEquals("PARTIAL", f.get("status"));
            assertEquals(true, f.get("counts_toward_adherence"));
            assertEquals("PARTIAL", estadoEntrenamiento(pw));
            assertEquals(ejercicios.size() - hechos, jdbc.queryForObject("SELECT count(*) FROM"
                    + " workout_session_exercises WHERE session_id = ? AND status = 'SKIPPED'"
                    + " AND skip_reason = 'LACK_OF_TIME'", Integer.class, s));
        }

        @Test
        @DisplayName("por debajo del 30 %: la sesion se guarda PARTIAL y cuenta; el entrenamiento se cierra SKIPPED")
        void bajoElMinimo() {
            var p = participante();
            long pw = entrenamientos(p).get(0);
            var ejercicios = indicados(pw);
            assertTrue(ejercicios.size() >= 4, "premisa: al menos cuatro ejercicios");
            long s = iniciar(p, pw);
            registrar(p, s, ejercicios.get(0), 1.0);
            terminar(p, s);
            assertEquals("PARTIAL", fila(s).get("status"));
            assertTrue(((BigDecimal) fila(s).get("completion_percentage")).doubleValue() < 30);
            assertEquals(1, contando(pw));
            assertEquals("SKIPPED", estadoEntrenamiento(pw));
        }

        @Test
        @DisplayName("omitido: con causa -> SKIPPED con la causa, sin sesion; despues no se puede iniciar")
        void omitido() {
            var p = participante();
            long pw = entrenamientos(p).get(1);
            omitirEntrenamiento(p, pw, SkipReason.LACK_OF_TIME);
            assertEquals("SKIPPED", estadoEntrenamiento(pw));
            assertEquals("LACK_OF_TIME", jdbc.queryForObject("SELECT skip_reason FROM planned_workouts"
                    + " WHERE planned_workout_id = ?", String.class, pw));
            assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM workout_sessions WHERE planned_workout_id = ?",
                    Integer.class, pw));
            assertThrows(WorkoutNotAvailableException.class, () -> iniciar(p, pw));
        }

        @Test
        @DisplayName("omitido sin causa: se rechaza")
        void omitidoSinCausa() {
            var p = participante();
            long pw = entrenamientos(p).get(1);
            assertThrows(DomainRuleViolationException.class, () -> omitirEntrenamiento(p, pw, null));
            assertEquals("SCHEDULED", estadoEntrenamiento(pw));
        }

        @Test
        @DisplayName("vencido sin causa: el usuario puede darla despues, y tambien reportarlo como hecho")
        void vencidoConCausaPosterior() {
            var p = participante();
            long pw = entrenamientos(p).get(2);
            jdbc.update("UPDATE planned_workouts SET status = 'SKIPPED', skip_reason = NULL"
                    + " WHERE planned_workout_id = ?", pw);
            omitirEntrenamiento(p, pw, SkipReason.SCHEDULE_CHANGE);
            assertEquals("SCHEDULE_CHANGE", jdbc.queryForObject("SELECT skip_reason FROM planned_workouts"
                    + " WHERE planned_workout_id = ?", String.class, pw));

            var reportados = indicados(pw).stream().map(e -> new ReportWorkoutCommand.ReportedExercise(e.pe(),
                    (short) e.sets(), e.duracion() ? null : e.repsTotal(), e.duracion() ? e.segundosTotal() : null,
                    null, null)).toList();
            var s = sesiones.handle(new ReportWorkoutCommand(p.userId(), pw, OffsetDateTime.now().minusHours(3),
                    (short) 6, null, (short) 38, reportados)).orElseThrow();
            assertEquals("COMPLETED", fila(s.getId()).get("status"));
            assertEquals("USER_REPORTED", fila(s.getId()).get("source"));
            assertEquals("COMPLETED", estadoEntrenamiento(pw));
            assertNull(jdbc.queryForObject("SELECT skip_reason FROM planned_workouts WHERE planned_workout_id = ?",
                    String.class, pw));
        }
    }

    @Nested
    @DisplayName("2 · interrupciones y reintentos")
    class Interrupciones {

        @Test
        @DisplayName("abandonar: no cuenta, el entrenamiento sigue exigible y el nuevo intento es el 2 y el unico que cuenta")
        void abandonarYReintentar() {
            var p = participante();
            long pw = entrenamientos(p).get(0);
            var ejercicios = indicados(pw);
            long s1 = iniciar(p, pw);
            registrar(p, s1, ejercicios.get(0), 1.0);
            sesiones.handle(new AbandonWorkoutSessionCommand(p.userId(), s1));
            assertEquals("ABANDONED", fila(s1).get("status"));
            assertEquals(false, fila(s1).get("counts_toward_adherence"));
            assertEquals("IN_PROGRESS", estadoEntrenamiento(pw));

            long s2 = iniciar(p, pw);
            assertEquals(2, ((Number) fila(s2).get("attempt_number")).intValue());
            ejercicios.forEach(e -> registrar(p, s2, e, 1.0));
            terminar(p, s2);
            assertEquals(1, contando(pw));
            assertEquals(true, fila(s2).get("counts_toward_adherence"));
            assertEquals("COMPLETED", estadoEntrenamiento(pw));
            // El intento abandonado se conserva con lo que registro.
            assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM workout_session_exercises WHERE session_id = ?",
                    Integer.class, s1));
        }

        @Test
        @DisplayName("sesion colgada mas de seis horas: la tarea la cierra ABANDONED y el usuario puede empezar otra")
        void colgada() {
            var p = participante();
            var plan = entrenamientos(p);
            long s = iniciar(p, plan.get(0));
            assertThrows(SessionAlreadyInProgressException.class, () -> iniciar(p, plan.get(1)));
            jdbc.update("UPDATE workout_sessions SET started_at = now() - interval '7 hours' WHERE session_id = ?", s);
            colgadas.abandonStaleSessions();
            assertEquals("ABANDONED", fila(s).get("status"));
            assertEquals(false, fila(s).get("counts_toward_adherence"));
            assertDoesNotThrow(() -> iniciar(p, plan.get(1)));
        }

        @Test
        @DisplayName("reenviar el mismo ejercicio sobrescribe: una fila, los ultimos valores")
        void reenvioDeEjercicio() {
            var p = participante();
            long pw = entrenamientos(p).get(0);
            var e = indicados(pw).get(0);
            long s = iniciar(p, pw);
            registrar(p, s, e, 0.5);
            registrar(p, s, e, 0.5);
            registrar(p, s, e, 1.0);
            var filas = jdbc.queryForList("SELECT status, skip_reason FROM workout_session_exercises"
                    + " WHERE session_id = ?", s);
            assertEquals(1, filas.size());
            assertEquals("COMPLETED", filas.get(0).get("status"));
            assertNull(filas.get(0).get("skip_reason"));
        }

        @Test
        @DisplayName("reintentar el cierre: el segundo se rechaza y nada cambia; reportar encima tambien se rechaza")
        void cierreDuplicado() {
            var p = participante();
            long pw = entrenamientos(p).get(0);
            var ejercicios = indicados(pw);
            long s = iniciar(p, pw);
            ejercicios.forEach(e -> registrar(p, s, e, 1.0));
            terminar(p, s);
            var antes = fila(s);

            assertThrows(DomainRuleViolationException.class, () -> terminar(p, s));
            assertEquals(antes, fila(s));
            var reportados = ejercicios.stream().map(e -> new ReportWorkoutCommand.ReportedExercise(e.pe(),
                    (short) e.sets(), e.duracion() ? null : e.repsTotal(), e.duracion() ? e.segundosTotal() : null,
                    null, null)).toList();
            assertThrows(WorkoutNotAvailableException.class, () -> sesiones.handle(new ReportWorkoutCommand(
                    p.userId(), pw, OffsetDateTime.now(), (short) 7, null, null, reportados)));
            assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM workout_sessions WHERE planned_workout_id = ?",
                    Integer.class, pw));
            assertEquals(1, contando(pw));
        }

        @Test
        @DisplayName("dos inicios simultaneos: una sola sesion en curso; el otro, 422 o 409, nunca dos")
        void iniciosSimultaneos() throws Exception {
            var p = participante();
            var plan = entrenamientos(p);
            var resultados = enParalelo(4, () -> iniciar(p, plan.get(0)));
            long ok = resultados.stream().filter(r -> r instanceof Long).count();
            assertEquals(1, ok, resultados.toString());
            for (var r : resultados)
                if (!(r instanceof Long))
                    assertTrue(r instanceof SessionAlreadyInProgressException
                            || r instanceof DataIntegrityViolationException, String.valueOf(r));
            assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM workout_sessions WHERE user_id = ?"
                    + " AND status = 'IN_PROGRESS'", Integer.class, p.userId()));
        }

        @Test
        @DisplayName("dos cierres simultaneos de la misma sesion: uno gana, una sola sesion que cuenta")
        void cierresSimultaneos() throws Exception {
            var p = participante();
            long pw = entrenamientos(p).get(0);
            long s = iniciar(p, pw);
            indicados(pw).forEach(e -> registrar(p, s, e, 1.0));
            var resultados = enParalelo(3, () -> { terminar(p, s); return "ok"; });
            assertEquals(1, resultados.stream().filter("ok"::equals).count(), resultados.toString());
            resultados.stream().filter(r -> !"ok".equals(r))
                    .forEach(r -> assertInstanceOf(DomainRuleViolationException.class, r));
            assertEquals("COMPLETED", fila(s).get("status"));
            assertEquals(1, contando(pw));
        }

        @Test
        @DisplayName("registro concurrente con el cierre: o entra en el porcentaje o se rechaza, nunca queda fuera")
        void registroConcurrenteConCierre() throws Exception {
            for (int vuelta = 0; vuelta < 3; vuelta++) {
                var p = participante();
                long pw = entrenamientos(p).get(0);
                var ejercicios = indicados(pw);
                long s = iniciar(p, pw);
                for (int i = 0; i < ejercicios.size() - 1; i++) registrar(p, s, ejercicios.get(i), 1.0);
                var ultimo = ejercicios.get(ejercicios.size() - 1);
                var tareas = List.<Callable<Object>>of(
                        () -> { registrar(p, s, ultimo, 1.0); return "registro"; },
                        () -> { terminar(p, s); return "cierre"; });
                var pool = Executors.newFixedThreadPool(2);
                try {
                    for (var f : pool.invokeAll(tareas)) {
                        try { f.get(); } catch (ExecutionException ignored) { /* el registro tardio */ }
                    }
                } finally {
                    pool.shutdownNow();
                }
                // El porcentaje de la sesion es exactamente el de sus filas.
                BigDecimal suma = jdbc.queryForObject("SELECT COALESCE(sum(completion_percentage), 0)"
                        + " FROM workout_session_exercises WHERE session_id = ?", BigDecimal.class, s);
                var esperado = suma.divide(BigDecimal.valueOf(ejercicios.size()), 2, java.math.RoundingMode.HALF_UP);
                assertEquals(0, esperado.compareTo((BigDecimal) fila(s).get("completion_percentage")),
                        "vuelta " + vuelta + ": " + esperado + " vs " + fila(s).get("completion_percentage"));
            }
        }

        @Test
        @DisplayName("abandono de la tarea concurrente con el cierre: no deshace un cierre ya hecho")
        void tareaConcurrenteConCierre() throws Exception {
            var p = participante();
            long pw = entrenamientos(p).get(0);
            long s = iniciar(p, pw);
            indicados(pw).forEach(e -> registrar(p, s, e, 1.0));
            jdbc.update("UPDATE workout_sessions SET started_at = now() - interval '7 hours' WHERE session_id = ?", s);
            var tareas = List.<Callable<Object>>of(
                    () -> { colgadas.abandonStaleSessions(); return "tarea"; },
                    () -> { terminar(p, s); return "cierre"; });
            var pool = Executors.newFixedThreadPool(2);
            List<Future<Object>> futuros;
            try {
                futuros = pool.invokeAll(tareas);
            } finally {
                pool.shutdownNow();
            }
            boolean cerro;
            try { futuros.get(1).get(); cerro = true; } catch (ExecutionException e) { cerro = false; }
            var f = fila(s);
            if (cerro) {
                assertEquals("COMPLETED", f.get("status"));
                assertEquals(true, f.get("counts_toward_adherence"));
                assertEquals("COMPLETED", estadoEntrenamiento(pw));
            } else {
                assertEquals("ABANDONED", f.get("status"));
                assertEquals(false, f.get("counts_toward_adherence"));
                assertEquals("IN_PROGRESS", estadoEntrenamiento(pw));
            }
        }
    }

    @Nested
    @DisplayName("3 · transiciones invalidas e historial")
    class Transiciones {

        @Test
        @DisplayName("una sesion cerrada no admite registros, ni otro cierre, ni abandono")
        void sesionCerrada() {
            var p = participante();
            long pw = entrenamientos(p).get(0);
            var ejercicios = indicados(pw);
            long s = iniciar(p, pw);
            ejercicios.forEach(e -> registrar(p, s, e, 1.0));
            terminar(p, s);
            var filas = jdbc.queryForList("SELECT * FROM workout_session_exercises WHERE session_id = ?"
                    + " ORDER BY session_exercise_id", s);

            assertThrows(DomainRuleViolationException.class, () -> registrar(p, s, ejercicios.get(0), 0.2));
            assertThrows(DomainRuleViolationException.class,
                    () -> sesiones.handle(new AbandonWorkoutSessionCommand(p.userId(), s)));
            assertEquals(filas, jdbc.queryForList("SELECT * FROM workout_session_exercises WHERE session_id = ?"
                    + " ORDER BY session_exercise_id", s));
            assertEquals("COMPLETED", fila(s).get("status"));
        }

        @Test
        @DisplayName("una sesion abandonada no admite registros ni cierre")
        void sesionAbandonada() {
            var p = participante();
            long pw = entrenamientos(p).get(0);
            long s = iniciar(p, pw);
            sesiones.handle(new AbandonWorkoutSessionCommand(p.userId(), s));
            assertThrows(DomainRuleViolationException.class, () -> registrar(p, s, indicados(pw).get(0), 1.0));
            assertThrows(DomainRuleViolationException.class, () -> terminar(p, s));
            assertEquals("ABANDONED", fila(s).get("status"));
        }

        @Test
        @DisplayName("un entrenamiento COMPLETED o PARTIAL no se puede marcar omitido ni volver a iniciar")
        void omitirUnCerrado() {
            var p = participante();
            var plan = entrenamientos(p);
            long pw = plan.get(0);
            long s = iniciar(p, pw);
            indicados(pw).forEach(e -> registrar(p, s, e, 1.0));
            terminar(p, s);
            assertThrows(DomainRuleViolationException.class, () -> omitirEntrenamiento(p, pw, SkipReason.FATIGUE));
            assertEquals("COMPLETED", estadoEntrenamiento(pw));
            assertNull(jdbc.queryForObject("SELECT skip_reason FROM planned_workouts WHERE planned_workout_id = ?",
                    String.class, pw));
            assertThrows(WorkoutNotAvailableException.class, () -> iniciar(p, pw));

            jdbc.update("UPDATE planned_workouts SET status = 'PARTIAL' WHERE planned_workout_id = ?", plan.get(1));
            assertThrows(DomainRuleViolationException.class,
                    () -> omitirEntrenamiento(p, plan.get(1), SkipReason.FATIGUE));
            assertEquals("PARTIAL", estadoEntrenamiento(plan.get(1)));
        }

        @Test
        @DisplayName("un entrenamiento de una version REPLACED no se inicia, ni se omite, ni se reporta")
        void versionReemplazada() {
            var p = participante();
            long pw = entrenamientos(p).get(0);
            jdbc.update("UPDATE planned_workouts SET status = 'REPLACED' WHERE planned_workout_id = ?", pw);
            assertThrows(WorkoutNotAvailableException.class, () -> iniciar(p, pw));
            assertThrows(DomainRuleViolationException.class, () -> omitirEntrenamiento(p, pw, SkipReason.FATIGUE));
            var e = indicados(pw).get(0);
            assertThrows(WorkoutNotAvailableException.class, () -> sesiones.handle(new ReportWorkoutCommand(
                    p.userId(), pw, OffsetDateTime.now(), (short) 7, null, null,
                    List.of(new ReportWorkoutCommand.ReportedExercise(e.pe(), (short) 1, 10, null, null, null)))));
            assertEquals("REPLACED", estadoEntrenamiento(pw));
            assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM workout_sessions WHERE planned_workout_id = ?",
                    Integer.class, pw));
        }

        @Test
        @DisplayName("vencido: no se inicia en vivo; ejercicio ajeno, sesion ajena y RPE ausente se rechazan")
        void rechazosVarios() {
            var p = participante();
            var otro = participante();
            var plan = entrenamientos(p);
            jdbc.update("UPDATE planned_workouts SET expires_at = now() - interval '1 hour'"
                    + " WHERE planned_workout_id = ?", plan.get(2));
            assertThrows(WorkoutNotAvailableException.class, () -> iniciar(p, plan.get(2)));

            long s = iniciar(p, plan.get(0));
            var ajeno = indicados(plan.get(1)).get(0);
            assertThrows(DomainRuleViolationException.class, () -> registrar(p, s, ajeno, 1.0));
            assertThrows(RuntimeException.class, () -> registrar(otro, s, indicados(plan.get(0)).get(0), 1.0));
            assertThrows(RuntimeException.class, () -> iniciar(otro, plan.get(1)));
            assertThrows(DomainRuleViolationException.class, () -> sesiones.handle(
                    new FinishWorkoutSessionCommand(p.userId(), s, null, null, null)));
            assertEquals("IN_PROGRESS", fila(s).get("status"));
            assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM workout_session_exercises WHERE session_id = ?",
                    Integer.class, s));
        }

        @Test
        @DisplayName("historial: el plan con sesiones no se puede borrar y lo ejecutado sigue apuntando a lo indicado")
        void historialProtegido() {
            var p = participante();
            long pw = entrenamientos(p).get(0);
            long s = iniciar(p, pw);
            indicados(pw).forEach(e -> registrar(p, s, e, 1.0));
            terminar(p, s);
            assertThrows(DataIntegrityViolationException.class,
                    () -> jdbc.update("DELETE FROM weekly_training_plans WHERE plan_id = ?", p.planId()));
            assertThrows(DataIntegrityViolationException.class,
                    () -> jdbc.update("DELETE FROM planned_workouts WHERE planned_workout_id = ?", pw));
            // Lo ejecutado y lo indicado se cruzan fila a fila.
            assertEquals(indicados(pw).size(), jdbc.queryForObject("SELECT count(*) FROM workout_session_exercises wse"
                    + " JOIN planned_workout_exercises pwe ON pwe.planned_exercise_id = wse.planned_exercise_id"
                    + " WHERE wse.session_id = ? AND pwe.planned_workout_id = ?", Integer.class, s, pw));
        }
    }

    @Nested
    @DisplayName("4 · seguridad")
    class Seguridad {

        static final String RELLENO = " AND pwe.prescription_type = 'SETS_REPS' AND x.movement_pattern NOT IN"
                + " ('KNEE_DOMINANT','HIP_HINGE','HIP_EXTENSION','HORIZONTAL_PUSH','VERTICAL_PUSH','HORIZONTAL_PULL',"
                + "'VERTICAL_PULL')";

        /** La primera sesion de la semana con un relleno que pueda pasar a ser el 1374. */
        private long conRelleno(Participante p) {
            return jdbc.queryForObject("SELECT pw.planned_workout_id FROM planned_workouts pw"
                    + " JOIN planned_workout_exercises pwe ON pwe.planned_workout_id = pw.planned_workout_id"
                    + " JOIN exercises x ON x.exercise_id = pwe.exercise_id WHERE pw.plan_id = ?" + RELLENO
                    + " ORDER BY pw.scheduled_date LIMIT 1", Long.class, p.planId());
        }

        /** Un relleno de la sesion pasa a ser el 1374, como si se hubiera publicado antes de contenerlo. */
        private long con1374(long plannedWorkoutId) {
            long pe = jdbc.queryForObject("SELECT pwe.planned_exercise_id FROM planned_workout_exercises pwe"
                    + " JOIN exercises x ON x.exercise_id = pwe.exercise_id WHERE pwe.planned_workout_id = ?"
                    + RELLENO + " ORDER BY pwe.exercise_order LIMIT 1", Long.class, plannedWorkoutId);
            jdbc.update("UPDATE planned_workout_exercises SET exercise_id = 1374 WHERE planned_exercise_id = ?", pe);
            return pe;
        }

        @Test
        @DisplayName("sesion bloqueada: no se inicia; la causa llega por HTTP como 422")
        void inicioBloqueado() throws Exception {
            var p = participante();
            long pw = conRelleno(p);
            con1374(pw);
            http.perform(post("/api/v1/workouts/" + pw + "/start").with(user(principal(p))))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.startsWith(
                            "SESION_BLOQUEADA_POR_SEGURIDAD")));
            assertEquals("SCHEDULED", estadoEntrenamiento(pw));
        }

        @Test
        @DisplayName("sesion abierta antes del bloqueo: el inseguro con causa Y repeticiones se rechaza; con causa y cero, no")
        void causaConTrabajo() {
            var p = participante();
            long pw = conRelleno(p);
            long s = iniciar(p, pw);
            long pe = con1374(pw);
            var error = assertThrows(WorkoutNotAvailableException.class, () -> sesiones.handle(
                    new RecordSessionExerciseCommand(p.userId(), s, pe, (short) 3, 30, null, null,
                            SkipReason.PAIN_OR_DISCOMFORT)));
            assertTrue(error.getMessage().contains("Solo puede registrarse como omitido"), error.getMessage());
            assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM workout_session_exercises WHERE session_id = ?",
                    Integer.class, s));

            sesiones.handle(new RecordSessionExerciseCommand(p.userId(), s, pe, (short) 0, 0, null, null,
                    SkipReason.PAIN_OR_DISCOMFORT));
            assertEquals("SKIPPED", jdbc.queryForObject("SELECT status FROM workout_session_exercises"
                    + " WHERE session_id = ? AND planned_exercise_id = ?", String.class, s, pe));
            // El resto de la sesion se puede hacer y cerrar.
            indicados(pw).stream().filter(e -> e.pe() != pe).forEach(e -> registrar(p, s, e, 1.0));
            terminar(p, s);
            assertEquals(1, contando(pw));
        }

        @Test
        @DisplayName("reporte de un vencido con el inseguro hecho: se rechaza; omitiendolo, se registra")
        void reporteDeVencidoInseguro() {
            var p = participante();
            long pw = conRelleno(p);
            long pe = con1374(pw);
            jdbc.update("UPDATE planned_workouts SET status = 'SKIPPED' WHERE planned_workout_id = ?", pw);
            var todos = indicados(pw).stream().map(e -> new ReportWorkoutCommand.ReportedExercise(e.pe(),
                    (short) e.sets(), e.duracion() ? null : e.repsTotal(), e.duracion() ? e.segundosTotal() : null,
                    null, null)).toList();
            assertThrows(WorkoutNotAvailableException.class, () -> sesiones.handle(new ReportWorkoutCommand(
                    p.userId(), pw, OffsetDateTime.now().minusHours(2), (short) 7, null, null, todos)));
            assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM workout_sessions WHERE planned_workout_id = ?",
                    Integer.class, pw));

            var sinInseguro = todos.stream().filter(r -> r.plannedExerciseId() != pe).toList();
            var s = sesiones.handle(new ReportWorkoutCommand(p.userId(), pw, OffsetDateTime.now().minusHours(2),
                    (short) 7, null, null, sinInseguro)).orElseThrow();
            assertEquals(true, fila(s.getId()).get("counts_toward_adherence"));
        }
    }

    @Nested
    @DisplayName("5 · lo que recibe la adherencia")
    class Adherencia {

        @Test
        @DisplayName("una semana con completo, parcial y omitido: solo los intentos que cuentan, con todo lo ejecutado")
        void semanaMedida() {
            var p = participante();
            var plan = entrenamientos(p);

            // 1: abandonado y luego completo.
            long a = iniciar(p, plan.get(0));
            sesiones.handle(new AbandonWorkoutSessionCommand(p.userId(), a));
            long s1 = iniciar(p, plan.get(0));
            indicados(plan.get(0)).forEach(e -> registrar(p, s1, e, 1.0));
            terminar(p, s1);
            // 2: parcial.
            var e2 = indicados(plan.get(1));
            long s2 = iniciar(p, plan.get(1));
            for (int i = 0; i < e2.size(); i++)
                if (i < (e2.size() + 1) / 2) registrar(p, s2, e2.get(i), 1.0);
                else omitirEjercicio(p, s2, e2.get(i), SkipReason.FATIGUE);
            terminar(p, s2);
            // 3: omitido con causa.
            omitirEntrenamiento(p, plan.get(2), SkipReason.LACK_OF_TIME);

            var hoy = LocalDate.now();
            var contadas = ejecucion.fetchCountedSessions(p.userId(), hoy.minusDays(1), hoy.plusDays(1), 3);
            assertEquals(2, contadas.size(), contadas.toString());
            var completa = contadas.stream().filter(v -> v.sessionId() == s1).findFirst().orElseThrow();
            assertEquals("COMPLETED", completa.status());
            assertEquals(p.planId(), completa.planId());
            assertEquals(7, completa.sessionRpe().intValue());
            assertEquals(41, completa.activeMinutes().intValue());
            assertEquals(indicados(plan.get(0)).size(), completa.completedExercises());
            assertTrue(completa.executedVolume() > 0);
            var parcial = contadas.stream().filter(v -> v.sessionId() == s2).findFirst().orElseThrow();
            assertEquals("PARTIAL", parcial.status());
            assertEquals("FATIGUE", parcial.dominantSkipReason());

            var resultados = ejecucion.fetchCountedResults(plan);
            assertEquals(2, resultados.size());
            assertTrue(resultados.stream().allMatch(r -> r.exercises().size() == indicados(r.plannedWorkoutId()).size()));
            assertTrue(ejecucion.fetchLastCountedSessionAt(p.userId()).isPresent());

            // El denominador vive en planning: tres indicados, uno omitido con su causa.
            var estados = jdbc.queryForList("SELECT status, skip_reason FROM planned_workouts WHERE plan_id = ?"
                    + " ORDER BY scheduled_date", p.planId());
            assertEquals(List.of("COMPLETED", "PARTIAL", "SKIPPED"), estados.stream().map(r -> r.get("status")).toList());
            assertEquals("LACK_OF_TIME", estados.get(2).get("skip_reason"));
        }
    }
}
