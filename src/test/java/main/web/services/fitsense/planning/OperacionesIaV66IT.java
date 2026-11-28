package main.web.services.fitsense.planning;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import main.web.services.fitsense.execution.domain.model.commands.FinishWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.model.commands.RecordSessionExerciseCommand;
import main.web.services.fitsense.execution.domain.model.commands.StartWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.services.WorkoutSessionCommandService;
import main.web.services.fitsense.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import main.web.services.fitsense.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import main.web.services.fitsense.orchestration.application.internal.WeeklyCycleService;
import main.web.services.fitsense.orchestration.infrastructure.locking.CicloSemanalBloqueo;
import main.web.services.fitsense.planning.application.internal.commandservices.GeneracionInicialService;
import main.web.services.fitsense.planning.application.internal.commandservices.PlanGenerationPipeline;
import main.web.services.fitsense.planning.application.internal.generation.SeguimientoOperacionesIa;
import main.web.services.fitsense.planning.domain.exceptions.GeneracionEnCursoException;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
import main.web.services.fitsense.planning.infrastructure.generation.ai.AirCircuitBreaker;
import main.web.services.fitsense.planning.infrastructure.generation.ai.JdbcDiarioDePredicciones;
import main.web.services.fitsense.planning.interfaces.acl.PlanningContextFacade;
import main.web.services.fitsense.planning.interfaces.rest.TrainingPlanController;
import main.web.services.fitsense.planning.interfaces.rest.resources.SkipWorkoutResource;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.BiologicalSex;
import main.web.services.fitsense.profiling.domain.model.valueobjects.FitnessLevel;
import main.web.services.fitsense.profiling.domain.model.valueobjects.GoalType;
import main.web.services.fitsense.profiling.domain.model.valueobjects.TrainingLocation;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import main.web.services.fitsense.shared.domain.model.valueobjects.SkipReason;
import main.web.services.fitsense.shared.domain.model.valueobjects.TrainingWeek;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * V66 · EL SEGUIMIENTO V59 EN LA GENERACION SINCRONA Y EN EL CIERRE SEMANAL (generacion y adaptacion), de extremo a
 * extremo: pipeline REAL, validadores, respaldo, publicacion y libro de predicciones sobre PostgreSQL efimero, contra un
 * Replicate FALSO local. Ninguna llamada sale a la red; el token es ficticio. ESTO ES UNA SIMULACION.
 * <p>
 * EL «REINICIO» se simula como lo ve la base cuando el proceso muere con una prediccion en curso:
 * <ul>
 *   <li>el lease de su operacion vence y deja de ser suyo (otro propietario);</li>
 *   <li>PostgreSQL suelta su bloqueo consultivo al cerrarse su conexion (aqui, {@code pg_terminate_backend});</li>
 *   <li>su hilo sigue bloqueado en el GET de la prediccion (un ZOMBI): al soltarlo al final se comprueba que el fencing
 *   le impide enviar o publicar nada.</li>
 * </ul>
 * Ademas, {@link #v59_03b_reinicioRealDelContexto} arranca un SEGUNDO contexto de Spring (otro proceso: otro propietario,
 * memoria vacia) sobre la misma base para recuperar el cierre semanal interrumpido.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=true",
        "fitsense.ai.api-token=token-ficticio-de-prueba",
        "fitsense.ai.poll-interval-millis=100",
        "fitsense.ai.generate.max-per-hour=100",
        "fitsense.plan-generation.worker.enabled=false",
        "fitsense.ai-operations.recovery.enabled=false"
})
@DisplayName("V66 · seguimiento V59 en la generacion sincrona y el cierre semanal (Replicate simulado)")
class OperacionesIaV66IT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    // ================================================================== Replicate falso
    static final HttpServer REPLICATE;
    static final ObjectMapper JSON = new ObjectMapper();
    /** Predicciones CREADAS (las que Replicate cobraria). */
    static final AtomicInteger POSTS = new AtomicInteger();
    static final AtomicInteger RECHAZADOS = new AtomicInteger();
    static final AtomicInteger CANCELACIONES = new AtomicInteger();
    static final Map<String, AtomicInteger> GETS = new ConcurrentHashMap<>();
    static final AtomicInteger GET_ESPERANDO = new AtomicInteger();
    static final AtomicInteger POST_ESPERANDO = new AtomicInteger();
    /** La prediccion se crea y la conexion se corta sin responder: el id nunca llega. */
    static final AtomicBoolean CORTAR_TRAS_ACEPTAR = new AtomicBoolean();
    static final AtomicBoolean RECHAZAR_POST = new AtomicBoolean();
    /** La proxima prediccion creada queda BLOQUEADA en sus GET hasta {@link #SOLTAR}. */
    static final AtomicBoolean BLOQUEAR_SIGUIENTE = new AtomicBoolean();
    /** El POST se crea y se queda esperando sin responder hasta {@link #SOLTAR} (el id no llega mientras tanto). */
    static final AtomicBoolean RETENER_POST = new AtomicBoolean();
    static final Set<String> BLOQUEADAS = ConcurrentHashMap.newKeySet();
    static volatile CountDownLatch SOLTAR = new CountDownLatch(0);
    static final AtomicInteger EN_CURSO_RESTANTES = new AtomicInteger();
    /** Cuantos GET mas responden 503 antes de responder bien. */
    static final AtomicInteger GET_FALLOS = new AtomicInteger();
    static volatile String ESTADO_FINAL = "succeeded";
    /** Salida (valor JSON del campo output). Por omision, una que el contrato rechaza. */
    static volatile String SALIDA = "\"{}\"";
    static final List<String> IDS = new CopyOnWriteArrayList<>();

    static {
        try {
            REPLICATE = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        REPLICATE.setExecutor(Executors.newCachedThreadPool(r -> {
            var t = new Thread(r, "replicate-falso-v66");
            t.setDaemon(true);
            return t;
        }));
        REPLICATE.createContext("/v1/models/", OperacionesIaV66IT::crear);
        REPLICATE.createContext("/v1/predictions/", OperacionesIaV66IT::consultar);
        REPLICATE.start();
    }

    static String base() {
        return "http://127.0.0.1:" + REPLICATE.getAddress().getPort();
    }

    private static void crear(HttpExchange x) throws IOException {
        x.getRequestBody().readAllBytes();
        if (RECHAZAR_POST.get()) {
            RECHAZADOS.incrementAndGet();
            responder(x, 422, "{\"detail\":\"entrada invalida\"}");
            return;
        }
        String id = "v" + POSTS.incrementAndGet() + "x" + UUID.randomUUID().toString().substring(0, 6);
        IDS.add(id);
        if (BLOQUEAR_SIGUIENTE.getAndSet(false)) BLOQUEADAS.add(id);
        if (CORTAR_TRAS_ACEPTAR.get()) {
            x.close();
            return;
        }
        if (RETENER_POST.getAndSet(false)) {
            POST_ESPERANDO.incrementAndGet();
            esperar();
            POST_ESPERANDO.decrementAndGet();
            x.close();       // y la respuesta nunca llega
            return;
        }
        responder(x, 201, prediccion(id, "starting", null));
    }

    private static void consultar(HttpExchange x) throws IOException {
        String id = x.getRequestURI().getPath().substring("/v1/predictions/".length());
        if (id.endsWith("/cancel")) {
            CANCELACIONES.incrementAndGet();
            responder(x, 200, prediccion(id.substring(0, id.length() - "/cancel".length()), "canceled", null));
            return;
        }
        GETS.computeIfAbsent(id, k -> new AtomicInteger()).incrementAndGet();
        if (BLOQUEADAS.contains(id)) {
            GET_ESPERANDO.incrementAndGet();
            esperar();
            GET_ESPERANDO.decrementAndGet();
        }
        if (GET_FALLOS.getAndUpdate(n -> Math.max(0, n - 1)) > 0) {
            responder(x, 503, "{\"detail\":\"no disponible\"}");
            return;
        }
        if (EN_CURSO_RESTANTES.getAndUpdate(n -> Math.max(0, n - 1)) > 0) {
            responder(x, 200, prediccion(id, "processing", null));
            return;
        }
        responder(x, 200, prediccion(id, ESTADO_FINAL, "succeeded".equals(ESTADO_FINAL) ? SALIDA : null));
    }

    private static void esperar() {
        try {
            SOLTAR.await(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String prediccion(String id, String estado, String salida) {
        return "{\"id\":\"" + id + "\",\"status\":\"" + estado + "\",\"output\":" + (salida == null ? "null" : salida)
                + ",\"error\":null,\"urls\":{\"get\":\"" + base() + "/v1/predictions/" + id + "\",\"cancel\":\""
                + base() + "/v1/predictions/" + id + "/cancel\"}}";
    }

    private static void responder(HttpExchange x, int codigo, String cuerpo) throws IOException {
        byte[] b = cuerpo.getBytes(StandardCharsets.UTF_8);
        x.getResponseHeaders().add("Content-Type", "application/json");
        x.sendResponseHeaders(codigo, b.length);
        try (var o = x.getResponseBody()) {
            o.write(b);
        }
    }

    @DynamicPropertySource
    static void replicateLocal(DynamicPropertyRegistry r) {
        r.add("fitsense.ai.base-url", () -> base() + "/v1/models/openai/gpt-5-mini/predictions");
    }

    @AfterAll
    static void parar() {
        SOLTAR.countDown();
        REPLICATE.stop(0);
    }

    // ================================================================== contexto
    static final AtomicInteger SECUENCIA = new AtomicInteger();
    static final LocalDate S1 = TrainingWeek.containing(LocalDate.now()).startDate().plusWeeks(1);
    static final List<String> GIMNASIO = List.of("body weight", "dumbbell", "barbell", "bench", "cable",
            "leverage machine", "band", "pull-up bar");

    @Autowired GeneracionInicialService generacion;
    @Autowired WeeklyTrainingPlanCommandService planes;
    @Autowired WeeklyCycleService ciclo;
    @Autowired PlanningContextFacade planning;
    @Autowired UserProfileCommandService perfiles;
    @Autowired WorkoutSessionCommandService sesiones;
    @Autowired TrainingPlanController controlador;
    @Autowired UserRepository usuarios;
    @Autowired SeguimientoOperacionesIa seguimiento;
    @Autowired JdbcDiarioDePredicciones libro;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectProvider<Clock> reloj;
    @Autowired PlanGenerationPipeline pipeline;

    final ExecutorService hilos = Executors.newCachedThreadPool();
    final List<Future<?>> zombis = new ArrayList<>();

    @BeforeEach
    void preparar() {
        POSTS.set(0);
        RECHAZADOS.set(0);
        CANCELACIONES.set(0);
        GETS.clear();
        GET_ESPERANDO.set(0);
        POST_ESPERANDO.set(0);
        CORTAR_TRAS_ACEPTAR.set(false);
        RECHAZAR_POST.set(false);
        BLOQUEAR_SIGUIENTE.set(false);
        RETENER_POST.set(false);
        BLOQUEADAS.clear();
        SOLTAR = new CountDownLatch(1);
        EN_CURSO_RESTANTES.set(0);
        GET_FALLOS.set(0);
        ESTADO_FINAL = "succeeded";
        SALIDA = "\"{}\"";
        IDS.clear();
        abrirCorte();
    }

    @AfterEach
    void soltarZombis() throws Exception {
        SOLTAR.countDown();
        for (var z : zombis) {
            try {
                z.get(60, TimeUnit.SECONDS);
            } catch (ExecutionException ignorada) {
                // el zombi termina con la excepcion del fencing: es lo esperado
            }
        }
        hilos.shutdownNow();
    }

    /** El corte del proveedor (en memoria) se abre tras 3 fallos seguidos: cada prueba empieza con el proveedor sano. */
    private void abrirCorte() {
        var corte = (AirCircuitBreaker) ReflectionTestUtils.getField(pipeline, "breaker");
        ReflectionTestUtils.setField(corte, "reabrirDespuesDe", Instant.EPOCH);
        corte.registrarRespuestaDelProveedor();
    }

    long participante() {
        String email = "v66-" + SECUENCIA.incrementAndGet() + "-" + UUID.randomUUID() + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'V66', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1990, 3, 10), BiologicalSex.FEMALE,
                new BigDecimal("164"), new BigDecimal("63"), null, FitnessLevel.INTERMEDIATE, GoalType.GENERAL_FITNESS,
                "Seguimiento V66", TrainingLocation.GYM, (short) 2, List.of((short) 2, (short) 4), (short) 55,
                GIMNASIO, List.of(), "Ninguna", true, true, true, false, false));
        return u;
    }

    // ------------------------------------------------------------------ ayudantes

    private GenerateWeeklyPlanCommand primerPlan(long u) {
        return GenerateWeeklyPlanCommand.firstPlan(u, planes.firstPlanWeekStart(u));
    }

    /** La generacion SINCRONA del primer plan (POST /users/me/plan/generate), en otro hilo. */
    private Future<?> generarEnOtroHilo(long u) {
        var f = hilos.submit(() -> generacion.generar(primerPlan(u)));
        zombis.add(f);
        return f;
    }

    private Future<?> cicloEnOtroHilo(long u, LocalDate medida, LocalDate nueva) {
        var f = hilos.submit(() -> ciclo.ejecutar(u, medida, nueva));
        zombis.add(f);
        return f;
    }

    private static void esperarA(java.util.function.BooleanSupplier cond, String que) throws InterruptedException {
        long fin = System.nanoTime() + TimeUnit.SECONDS.toNanos(60);
        while (!cond.getAsBoolean()) {
            if (System.nanoTime() > fin) throw new AssertionError("no llego: " + que);
            Thread.sleep(50);
        }
    }

    /**
     * El proceso MUERE: su conexion de bloqueo se cierra (PostgreSQL suelta el bloqueo consultivo) y su operacion deja de
     * latir (lease vencido, propietario ya inexistente). Su hilo queda bloqueado como zombi.
     */
    private void morir(long u) {
        jdbc.queryForList("SELECT pg_terminate_backend(pid) FROM pg_stat_activity"
                + " WHERE application_name = 'fitsense-ciclo-semanal-bloqueo'");
        int n = jdbc.update("UPDATE plan_generation_operations SET lease_owner = 'proceso-muerto',"
                + " lease_expires_at = now() - interval '1 second' WHERE user_id = ? AND status = 'PROCESSING'"
                + " AND kind <> 'FIRST_PLAN'", u);
        assertThat(n).as("habia una operacion en curso que muere").isEqualTo(1);
    }

    private Map<String, Object> operacion(long u, String kind) {
        return jdbc.queryForMap("SELECT operation_id, status, reason_code, plan_id, executions, operation_key"
                + " FROM plan_generation_operations WHERE user_id = ? AND kind = ? ORDER BY created_at DESC LIMIT 1",
                u, kind);
    }

    private List<Map<String, Object>> predicciones(Object operacion) {
        return jdbc.queryForList("SELECT attempt, status, replicate_prediction_id, reused_from_row_id"
                + " FROM plan_generation_predictions WHERE operation_id = ? ORDER BY prediction_row_id", operacion);
    }

    private int planesDe(long u) {
        return jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ?", Integer.class, u);
    }

    private int planesActivosDe(long u, LocalDate semana) {
        return jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ? AND week_start_date = ?"
                + " AND status = 'ACTIVE'", Integer.class, u, semana);
    }

    private String origenDe(long u) {
        return jdbc.queryForObject("SELECT generation_source FROM weekly_training_plans WHERE user_id = ?"
                + " ORDER BY plan_id DESC LIMIT 1", String.class, u);
    }

    /** Una salida VALIDA de la IA para un perfil identico: la rutina que el motor de reglas publica para el modelo. */
    private String salidaValidaPara(LocalDate semana) throws Exception {
        long modelo = participante();
        SALIDA = "\"{}\"";
        generacion.generar(GenerateWeeklyPlanCommand.firstPlan(modelo, semana));
        long plan = jdbc.queryForObject("SELECT plan_id FROM weekly_training_plans WHERE user_id = ?", Long.class,
                modelo);
        var sesionesJson = new ArrayList<Map<String, Object>>();
        for (var w : jdbc.queryForList("SELECT planned_workout_id, scheduled_date, focus_code, workout_name,"
                + " expected_duration_minutes FROM planned_workouts WHERE plan_id = ? ORDER BY display_order", plan)) {
            var ejercicios = new ArrayList<Map<String, Object>>();
            for (var e : jdbc.queryForList("SELECT exercise_id, exercise_order, prescription_type, planned_sets,"
                    + " planned_reps, planned_duration_seconds, target_load_kg, rest_seconds, notes"
                    + " FROM planned_workout_exercises WHERE planned_workout_id = ? ORDER BY exercise_order",
                    w.get("planned_workout_id")))
                ejercicios.add(new LinkedHashMap<>(e));
            var s = new LinkedHashMap<String, Object>();
            s.put("scheduled_date", w.get("scheduled_date").toString());
            s.put("focus_code", w.get("focus_code"));
            s.put("workout_name", w.get("workout_name"));
            s.put("expected_duration_minutes", w.get("expected_duration_minutes"));
            s.put("exercises", ejercicios);
            sesionesJson.add(s);
        }
        var p = new LinkedHashMap<String, Object>();
        p.put("schema_version", PlanDraft.SCHEMA_VERSION);
        p.put("plan_name", "Plan simulado");
        p.put("total_volume", 0);
        p.put("rationale", "Salida simulada para la prueba V66 (no es una respuesta real de la IA).");
        p.put("workouts", sesionesJson);
        POSTS.set(0);
        IDS.clear();
        GETS.clear();
        abrirCorte();
        return JSON.writeValueAsString(JSON.writeValueAsString(p));
    }

    /** Un participante con la semana S1 publicada y ejecutada con una omision por fatiga: el ciclo ADAPTA con IA. */
    private long conSemanaParaAdaptar() {
        long u = participante();
        SALIDA = "\"{}\"";
        long plan = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        var ids = jdbc.queryForList("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ? ORDER BY"
                + " scheduled_date", Long.class, plan);
        hacer(u, ids.get(0));
        controlador.skip(UserDetailsImpl.build(usuarios.findById(u).orElseThrow()), ids.get(1),
                new SkipWorkoutResource(SkipReason.FATIGUE));
        POSTS.set(0);
        IDS.clear();
        GETS.clear();
        abrirCorte();
        return u;
    }

    private void hacer(long u, long w) {
        long s = sesiones.handle(new StartWorkoutSessionCommand(u, w)).orElseThrow().getId();
        for (var f : jdbc.queryForList("SELECT planned_exercise_id, prescription_type, planned_sets, planned_reps,"
                + " planned_duration_seconds, target_load_kg FROM planned_workout_exercises WHERE planned_workout_id = ?",
                w)) {
            boolean duracion = "DURATION".equals(f.get("prescription_type"));
            int series = f.get("planned_sets") == null ? 1 : ((Number) f.get("planned_sets")).intValue();
            int total = series * ((Number) f.get(duracion ? "planned_duration_seconds" : "planned_reps")).intValue();
            sesiones.handle(new RecordSessionExerciseCommand(u, s, ((Number) f.get("planned_exercise_id")).longValue(),
                    (short) series, duracion ? null : total, duracion ? total : null,
                    (BigDecimal) f.get("target_load_kg"), null));
        }
        sesiones.handle(new FinishWorkoutSessionCommand(u, s, (short) 6, (short) 4, (short) 45));
    }

    private int evidenciasConPlan(long u) {
        return jdbc.queryForObject("SELECT count(*) FROM plan_adaptation_attempts WHERE user_id = ?"
                + " AND resulting_plan_id IS NOT NULL", Integer.class, u);
    }

    // ================================================================== generacion SINCRONA

    @Test
    @DisplayName("V59-01/V59-04 · reinicio durante la generacion sincrona con la prediccion en curso: la peticion"
            + " repetida ADOPTA la operacion y sigue esa prediccion por su id; 1 POST, 1 plan de la IA")
    void v59_01_reinicioSincronoPrediccionEnCurso() throws Exception {
        long u = participante();
        SALIDA = salidaValidaPara(planes.firstPlanWeekStart(u));
        BLOQUEAR_SIGUIENTE.set(true);
        generarEnOtroHilo(u);
        esperarA(() -> GET_ESPERANDO.get() > 0, "la prediccion en curso");
        var op = operacion(u, "FIRST_PLAN_SYNC");
        assertThat(predicciones(op.get("operation_id"))).singleElement()
                .satisfies(p -> assertThat(p.get("status")).isEqualTo("ENVIADA"));
        morir(u);
        // La prediccion sigue viva en Replicate: 2 consultas mas en curso y termina bien.
        BLOQUEADAS.clear();
        EN_CURSO_RESTANTES.set(2);

        var plan = generacion.generar(primerPlan(u));

        assertThat(plan).isPresent();
        assertThat(POSTS.get()).as("ningun POST nuevo: se siguio la misma prediccion").isEqualTo(1);
        assertThat(planesDe(u)).isEqualTo(1);
        assertThat(origenDe(u)).isEqualTo("AI");
        var despues = operacion(u, "FIRST_PLAN_SYNC");
        assertThat(despues.get("operation_id")).as("la misma operacion, adoptada").isEqualTo(op.get("operation_id"));
        assertThat(despues.get("status")).isEqualTo("COMPLETED");
        assertThat(((Number) despues.get("executions")).intValue()).isEqualTo(2);
        // El zombi, al despertar, no envia ni publica nada (fencing).
        SOLTAR.countDown();
        for (var z : zombis) assertThatThrownBy(() -> z.get(60, TimeUnit.SECONDS)).isInstanceOf(ExecutionException.class);
        assertThat(POSTS.get()).isEqualTo(1);
        assertThat(planesDe(u)).isEqualTo(1);
    }

    @Test
    @DisplayName("V59-05 · la prediccion termino durante la caida: se recupera por su id, se VALIDA y se publica; 0 POST nuevos")
    void v59_05_terminoDuranteLaCaida() throws Exception {
        long u = participante();
        SALIDA = salidaValidaPara(planes.firstPlanWeekStart(u));
        BLOQUEAR_SIGUIENTE.set(true);
        generarEnOtroHilo(u);
        esperarA(() -> GET_ESPERANDO.get() > 0, "la prediccion en curso");
        morir(u);
        BLOQUEADAS.clear();    // ya termino en Replicate

        generacion.generar(primerPlan(u));

        assertThat(POSTS.get()).isEqualTo(1);
        assertThat(origenDe(u)).isEqualTo("AI");
        var op = operacion(u, "FIRST_PLAN_SYNC");
        assertThat(predicciones(op.get("operation_id"))).singleElement()
                .satisfies(p -> assertThat(p.get("status")).isEqualTo("SUCCEEDED"));
    }

    @Test
    @DisplayName("V59-06 · POST con resultado incierto (el proceso muere sin recibir el id): la reanudacion NO lo reenvia;"
            + " queda NO_CONFIRMADA y se publica el respaldo validado")
    void v59_06_postInciertoNoSeReenvia() throws Exception {
        long u = participante();
        RETENER_POST.set(true);
        generarEnOtroHilo(u);
        esperarA(() -> POST_ESPERANDO.get() > 0, "el POST sin respuesta");
        var op = operacion(u, "FIRST_PLAN_SYNC");
        assertThat(predicciones(op.get("operation_id"))).singleElement()
                .satisfies(p -> assertThat(p.get("status")).isEqualTo("SOLICITANDO"));
        morir(u);

        generacion.generar(primerPlan(u));

        assertThat(POSTS.get()).as("solo el del proceso muerto").isEqualTo(1);
        assertThat(origenDe(u)).isEqualTo("RULE_ENGINE");
        assertThat(predicciones(op.get("operation_id"))).singleElement()
                .satisfies(p -> {
                    assertThat(p.get("status")).isEqualTo("NO_CONFIRMADA");
                    assertThat(p.get("replicate_prediction_id")).isNull();
                });
        assertThat(operacion(u, "FIRST_PLAN_SYNC").get("status")).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("V59-07 · dos usuarios con perfil identico: el envio incierto de A no consulta ni adopta la prediccion de B")
    void v59_07_dosUsuariosNoIntercambian() throws Exception {
        long b = participante();
        long a = participante();
        SALIDA = salidaValidaPara(planes.firstPlanWeekStart(b));
        generacion.generar(primerPlan(b));
        assertThat(origenDe(b)).isEqualTo("AI");
        var deB = List.copyOf(IDS);
        var getsDeB = deB.stream().mapToInt(id -> GETS.getOrDefault(id, new AtomicInteger()).get()).sum();

        CORTAR_TRAS_ACEPTAR.set(true);
        generacion.generar(primerPlan(a));

        assertThat(origenDe(a)).isEqualTo("RULE_ENGINE");
        assertThat(deB.stream().mapToInt(id -> GETS.getOrDefault(id, new AtomicInteger()).get()).sum())
                .as("A ni siquiera consulta las predicciones de B").isEqualTo(getsDeB);
        var deA = predicciones(operacion(a, "FIRST_PLAN_SYNC").get("operation_id"));
        assertThat(deA).allSatisfy(p -> assertThat(deB).doesNotContain((String) p.get("replicate_prediction_id")));
    }

    @Test
    @DisplayName("V59-08 · dos ejecuciones VIVAS de la misma operacion (dos instancias): la segunda no hace nada (409),"
            + " y una huerfana la adopta una sola")
    void v59_08_dosInstancias() throws Exception {
        long u = participante();
        BLOQUEAR_SIGUIENTE.set(true);
        generarEnOtroHilo(u);
        esperarA(() -> GET_ESPERANDO.get() > 0, "la prediccion en curso");
        // Otra instancia con otro propietario sobre la misma base: la operacion esta viva (lease vigente).
        var otra = new SeguimientoOperacionesIa(jdbc, reloj, 90, 3, 24, 1800);
        assertThatThrownBy(() -> otra.ejecutar(SeguimientoOperacionesIa.Clave.primerPlanSincrono(u,
                planes.firstPlanWeekStart(u)), x -> null, g -> "no deberia correr"))
                .isInstanceOf(SeguimientoOperacionesIa.EnCursoEnOtroProceso.class);
        // Muere: dos instancias intentan adoptarla a la vez; una sola lo consigue.
        morir(u);
        var clave = SeguimientoOperacionesIa.Clave.primerPlanSincrono(u, planes.firstPlanWeekStart(u));
        var tercera = new SeguimientoOperacionesIa(jdbc, reloj, 90, 3, 24, 1800);
        var inicio = new CyclicBarrier(2);
        var ganadas = new AtomicInteger();
        var rechazadas = new AtomicInteger();
        var listo = new CountDownLatch(1);
        Callable<Void> adoptar = () -> {
            var s = inicio.await() == 0 ? otra : tercera;
            try {
                s.ejecutar(clave, x -> null, g -> {
                    ganadas.incrementAndGet();
                    try {
                        listo.await(10, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return null;
                });
            } catch (SeguimientoOperacionesIa.EnCursoEnOtroProceso e) {
                rechazadas.incrementAndGet();
                listo.countDown();
            }
            return null;
        };
        var f1 = hilos.submit(adoptar);
        var f2 = hilos.submit(adoptar);
        f1.get(30, TimeUnit.SECONDS);
        f2.get(30, TimeUnit.SECONDS);
        assertThat(ganadas.get()).isEqualTo(1);
        assertThat(rechazadas.get()).isEqualTo(1);
        otra.parar();
        tercera.parar();
    }

    @Test
    @DisplayName("V59-11 · la prediccion recuperada trae una rutina invalida: los validadores la rechazan y se reintenta;"
            + " nunca se publica como IA")
    void v59_11_resultadoInvalido() throws Exception {
        long u = participante();
        SALIDA = "\"{}\"";
        BLOQUEAR_SIGUIENTE.set(true);
        generarEnOtroHilo(u);
        esperarA(() -> GET_ESPERANDO.get() > 0, "la prediccion en curso");
        morir(u);
        BLOQUEADAS.clear();

        generacion.generar(primerPlan(u));

        assertThat(origenDe(u)).isEqualTo("RULE_ENGINE");
        assertThat(POSTS.get()).as("intento 1 recuperado (invalido) + los 2 reintentos de siempre").isEqualTo(3);
        assertThat(planesDe(u)).isEqualTo(1);
    }

    @Test
    @DisplayName("V59-12 · O20: la prediccion recuperada trae un ejercicio bloqueado por dolor: no se publica")
    void v59_12_o20() throws Exception {
        long u = participante();
        SALIDA = salidaValidaPara(planes.firstPlanWeekStart(u));
        // El primer ejercicio de la salida valida queda bloqueado por dolor para u.
        long bloqueado = JSON.readTree(JSON.readValue(SALIDA, String.class)).get("workouts").get(0).get("exercises")
                .get(0).get("exercise_id").asLong();
        long planModelo = jdbc.queryForObject("SELECT min(plan_id) FROM weekly_training_plans", Long.class);
        long sesionModelo = jdbc.queryForObject("SELECT min(planned_workout_id) FROM planned_workouts WHERE plan_id = ?",
                Long.class, planModelo);
        jdbc.update("INSERT INTO user_safety_exclusions (user_id, exercise_id, reason, source_plan_id,"
                + " source_planned_workout_id, event_date, recorded_by) VALUES (?, ?, 'DOLOR_IDENTIFICADO', ?, ?,"
                + " current_date, 'CICLO_SEMANAL')", u, bloqueado, planModelo, sesionModelo);
        BLOQUEAR_SIGUIENTE.set(true);
        generarEnOtroHilo(u);
        esperarA(() -> GET_ESPERANDO.get() > 0, "la prediccion en curso");
        morir(u);
        BLOQUEADAS.clear();

        generacion.generar(primerPlan(u));

        long plan = jdbc.queryForObject("SELECT plan_id FROM weekly_training_plans WHERE user_id = ?", Long.class, u);
        assertThat(jdbc.queryForList("SELECT pwe.exercise_id FROM planned_workout_exercises pwe JOIN planned_workouts pw"
                + " ON pw.planned_workout_id = pwe.planned_workout_id WHERE pw.plan_id = ?", Long.class, plan))
                .as("lo publicado nunca trae lo excluido por dolor").doesNotContain(bloqueado);
    }

    @Test
    @DisplayName("V59-13/V59-14 · error de Replicate (4xx) o prediccion cancelada: no es un exito de la IA; respaldo"
            + " validado y la operacion queda cerrada")
    void v59_13_14_erroresYCancelacion() throws Exception {
        long rechazada = participante();
        RECHAZAR_POST.set(true);
        generacion.generar(primerPlan(rechazada));
        assertThat(POSTS.get()).isZero();
        assertThat(origenDe(rechazada)).isEqualTo("RULE_ENGINE");
        assertThat(predicciones(operacion(rechazada, "FIRST_PLAN_SYNC").get("operation_id"))).singleElement()
                .satisfies(p -> assertThat(p.get("status")).isEqualTo("ERROR_ENVIO"));
        RECHAZAR_POST.set(false);

        long cancelada = participante();
        abrirCorte();
        ESTADO_FINAL = "canceled";
        generacion.generar(primerPlan(cancelada));
        assertThat(origenDe(cancelada)).isEqualTo("RULE_ENGINE");
        assertThat(predicciones(operacion(cancelada, "FIRST_PLAN_SYNC").get("operation_id"))).singleElement()
                .satisfies(p -> assertThat(p.get("status")).isEqualTo("CANCELED"));
        assertThat(operacion(cancelada, "FIRST_PLAN_SYNC").get("status")).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("V59-16 · GET con fallos transitorios: se toleran (sin cancelar) y la salida valida se publica como IA")
    void v59_16_getTransitorio() throws Exception {
        long u = participante();
        SALIDA = salidaValidaPara(planes.firstPlanWeekStart(u));
        GET_FALLOS.set(2);
        generacion.generar(primerPlan(u));
        assertThat(origenDe(u)).isEqualTo("AI");
        assertThat(CANCELACIONES.get()).isZero();
        assertThat(POSTS.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("V59-17 · reinicios repetidos durante la recuperacion: cada adopcion sigue la MISMA prediccion; al"
            + " agotar las adopciones se cierra y no se paga ninguna de mas")
    void v59_17_reiniciosRepetidos() throws Exception {
        long u = participante();
        SALIDA = salidaValidaPara(planes.firstPlanWeekStart(u));
        BLOQUEAR_SIGUIENTE.set(true);
        generarEnOtroHilo(u);
        esperarA(() -> GET_ESPERANDO.get() == 1, "la prediccion en curso (1)");
        morir(u);
        // Segunda ejecucion (adopcion 2), que tambien muere con la prediccion en curso.
        generarEnOtroHilo(u);
        esperarA(() -> GET_ESPERANDO.get() == 2, "la prediccion en curso (2)");
        assertThat(((Number) operacion(u, "FIRST_PLAN_SYNC").get("executions")).intValue()).isEqualTo(2);
        morir(u);
        // Tercera (adopcion 3): la prediccion termina; se publica.
        BLOQUEADAS.clear();
        generacion.generar(primerPlan(u));
        assertThat(POSTS.get()).as("siempre la misma prediccion").isEqualTo(1);
        assertThat(origenDe(u)).isEqualTo("AI");
        assertThat(((Number) operacion(u, "FIRST_PLAN_SYNC").get("executions")).intValue()).isEqualTo(3);

        // Otra persona agota las adopciones: la operacion se cierra EJECUCION_INTERRUMPIDA y no se reanuda mas.
        long v = participante();
        jdbc.update("INSERT INTO plan_generation_operations (operation_id, user_id, kind, status, week_start_date,"
                        + " operation_key, executions, created_at, started_at, updated_at, lease_owner, lease_expires_at,"
                        + " first_started_at) VALUES (?, ?, 'FIRST_PLAN_SYNC', 'PROCESSING', ?, ?, 3, now(), now(), now(),"
                        + " 'proceso-muerto', now() - interval '1 second', now())", UUID.randomUUID(), v,
                planes.firstPlanWeekStart(v), SeguimientoOperacionesIa.Clave.primerPlanSincrono(v,
                        planes.firstPlanWeekStart(v)).texto());
        assertThat(seguimiento.cerrarVencidas()).isGreaterThanOrEqualTo(1);
        assertThat(operacion(v, "FIRST_PLAN_SYNC").get("reason_code")).isEqualTo("EJECUCION_INTERRUMPIDA");
    }

    // ================================================================== CIERRE SEMANAL (adaptacion con IA)

    @Test
    @DisplayName("V59-02/V59-03 · reinicio durante la ADAPTACION del cierre semanal: el barrido relanza el ciclo, adopta"
            + " su operacion y sigue la prediccion por su id; las mismas predicciones y UNA semana publicada")
    void v59_02_03_reinicioDuranteLaAdaptacion() throws Exception {
        long u = conSemanaParaAdaptar();
        BLOQUEAR_SIGUIENTE.set(true);
        cicloEnOtroHilo(u, S1, S1.plusWeeks(1));
        esperarA(() -> GET_ESPERANDO.get() > 0, "la prediccion de la adaptacion en curso");
        var op = operacion(u, "WEEKLY_CYCLE");
        assertThat(op.get("operation_key")).isEqualTo("WEEKLY_CYCLE:u=" + u + ":medida=" + S1 + ":semana="
                + S1.plusWeeks(1));
        morir(u);
        BLOQUEADAS.clear();

        assertThat(ciclo.recuperarInterrumpidos()).isEqualTo(1);

        assertThat(POSTS.get()).as("las 3 de una adaptacion sin caida (la 1.a seguida por su id), ni una mas")
                .isEqualTo(3);
        assertThat(planesActivosDe(u, S1.plusWeeks(1))).isEqualTo(1);
        assertThat(evidenciasConPlan(u)).isEqualTo(1);
        var despues = operacion(u, "WEEKLY_CYCLE");
        assertThat(despues.get("operation_id")).isEqualTo(op.get("operation_id"));
        assertThat(despues.get("status")).isEqualTo("COMPLETED");
        // V59-09: una recuperacion repetida y el zombi no publican otra vez.
        assertThat(ciclo.recuperarInterrumpidos()).isZero();
        assertThat(ciclo.ejecutar(u, S1, S1.plusWeeks(1)).estado()).isEqualTo(CicloSemanalBloqueo.Estado.EJECUTADO);
        SOLTAR.countDown();
        for (var z : zombis) z.get(60, TimeUnit.SECONDS);
        assertThat(POSTS.get()).isEqualTo(3);
        assertThat(planesActivosDe(u, S1.plusWeeks(1))).isEqualTo(1);
        assertThat(evidenciasConPlan(u)).isEqualTo(1);
    }

    @Test
    @DisplayName("V59-03b · REINICIO REAL: un segundo contexto de Spring (otro proceso, memoria vacia) recupera el cierre"
            + " semanal interrumpido sobre la misma base; sin predicciones de mas ni doble publicacion")
    void v59_03b_reinicioRealDelContexto() throws Exception {
        long u = conSemanaParaAdaptar();
        BLOQUEAR_SIGUIENTE.set(true);
        cicloEnOtroHilo(u, S1, S1.plusWeeks(1));
        esperarA(() -> GET_ESPERANDO.get() > 0, "la prediccion de la adaptacion en curso");
        morir(u);
        BLOQUEADAS.clear();
        // Argumentos de linea de comandos: ganan a las propiedades de sistema de failsafe (que bloquean la base real).
        try (var otroProceso = new SpringApplicationBuilder(main.web.services.fitsense.FitsenseApplication.class)
                .web(WebApplicationType.NONE)
                .run("--spring.datasource.url=" + postgres.getJdbcUrl(),
                        "--spring.datasource.username=" + postgres.getUsername(),
                        "--spring.datasource.password=" + postgres.getPassword(),
                        "--spring.flyway.url=" + postgres.getJdbcUrl(),
                        "--spring.flyway.user=" + postgres.getUsername(),
                        "--spring.flyway.password=" + postgres.getPassword(),
                        "--authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
                        "--fitsense.ai.enabled=true",
                        "--fitsense.ai.api-token=token-ficticio-de-prueba",
                        "--fitsense.ai.base-url=" + base() + "/v1/models/openai/gpt-5-mini/predictions",
                        "--fitsense.ai.poll-interval-millis=100",
                        "--fitsense.ai.generate.max-per-hour=100",
                        "--fitsense.plan-generation.worker.enabled=false",
                        "--fitsense.ai-operations.recovery.enabled=false")) {
            var otroSeguimiento = otroProceso.getBean(SeguimientoOperacionesIa.class);
            assertThat(otroSeguimiento.propietario()).isNotEqualTo(seguimiento.propietario());
            assertThat(otroProceso.getBean(WeeklyCycleService.class).recuperarInterrumpidos()).isEqualTo(1);
        }
        assertThat(POSTS.get()).isEqualTo(3);
        assertThat(planesActivosDe(u, S1.plusWeeks(1))).isEqualTo(1);
        assertThat(evidenciasConPlan(u)).isEqualTo(1);
        assertThat(operacion(u, "WEEKLY_CYCLE").get("status")).isEqualTo("COMPLETED");
        SOLTAR.countDown();
        for (var z : zombis) z.get(60, TimeUnit.SECONDS);
        assertThat(planesActivosDe(u, S1.plusWeeks(1))).isEqualTo(1);
        assertThat(POSTS.get()).isEqualTo(3);
    }

    @Test
    @DisplayName("V59-10 · el plan medido cambio durante la espera: la prediccion antigua NO se aplica (su peticion ya no"
            + " es la vigente: DESCARTADA) y la semana se decide y valida sobre el estado actual")
    void v59_10_planCambiado() throws Exception {
        long u = conSemanaParaAdaptar();
        BLOQUEAR_SIGUIENTE.set(true);
        cicloEnOtroHilo(u, S1, S1.plusWeeks(1));
        esperarA(() -> GET_ESPERANDO.get() > 0, "la prediccion de la adaptacion en curso");
        var op = operacion(u, "WEEKLY_CYCLE").get("operation_id");
        morir(u);
        // Mientras el proceso estaba caido, cambia lo que la adaptacion corrige (la sesion senalada de la semana medida).
        var ids = jdbc.queryForList("SELECT planned_workout_id FROM planned_workouts pw JOIN weekly_training_plans p"
                + " ON p.plan_id = pw.plan_id WHERE p.user_id = ? AND p.week_start_date = ? ORDER BY scheduled_date",
                Long.class, u, S1);
        var antes = predicciones(op);
        // Menos descanso en la sesion senalada: la peticion de correccion cambia y la sesion no se alarga (V4).
        jdbc.update("UPDATE planned_workout_exercises SET rest_seconds = GREATEST(rest_seconds - 15, 30)"
                + " WHERE planned_workout_id = ? AND rest_seconds > 30", ids.get(1));
        BLOQUEADAS.clear();

        ciclo.recuperarInterrumpidos();

        var filas = predicciones(op);
        assertThat(antes).singleElement().satisfies(p -> assertThat(p.get("status")).isEqualTo("ENVIADA"));
        assertThat(filas.get(0).get("status")).as("la prediccion de la peticion antigua no se reutiliza: DESCARTADA")
                .isEqualTo("DESCARTADA");
        assertThat(POSTS.get()).as("la del proceso muerto + las 3 de la peticion vigente; filas " + filas)
                .isEqualTo(4);
        // La semana se decide sobre el estado ACTUAL y pasa por los validadores: con este cambio de descanso la copia ya
        // no lleva una dosis autorizada (V24) y la semana queda NO_FACTIBLE. Ni la propuesta antigua ni una invalida
        // se publican.
        assertThat(planesActivosDe(u, S1.plusWeeks(1))).isZero();
        assertThat(jdbc.queryForObject("SELECT status FROM plan_adaptation_attempts WHERE user_id = ? ORDER BY"
                + " attempt_id DESC LIMIT 1", String.class, u)).isEqualTo("NO_FACTIBLE");
        assertThat(operacion(u, "WEEKLY_CYCLE").get("status")).isEqualTo("NO_FACTIBLE");
        SOLTAR.countDown();
        for (var z : zombis) z.get(60, TimeUnit.SECONDS);
        assertThat(planesActivosDe(u, S1.plusWeeks(1))).as("tampoco el zombi publica la adaptacion antigua").isZero();
    }

    @Test
    @DisplayName("V59-18 · barrido con varios participantes: cada cierre interrumpido se relanza aislado; uno que falla no"
            + " afecta a los demas ni a un ciclo sano")
    void v59_18_variosParticipantes() throws Exception {
        long a = conSemanaParaAdaptar();
        long b = conSemanaParaAdaptar();
        for (long u : List.of(a, b)) {
            BLOQUEAR_SIGUIENTE.set(true);
            int previas = GET_ESPERANDO.get();
            cicloEnOtroHilo(u, S1, S1.plusWeeks(1));
            esperarA(() -> GET_ESPERANDO.get() > previas, "prediccion en curso de " + u);
            morir(u);
        }
        BLOQUEADAS.clear();
        // Un tercero con una operacion huerfana de un ciclo que ya no se puede relanzar (fuera de su ventana).
        long c = participante();
        jdbc.update("INSERT INTO plan_generation_operations (operation_id, user_id, kind, status, week_start_date,"
                        + " measured_week_start, operation_key, executions, created_at, started_at, updated_at, lease_owner,"
                        + " lease_expires_at, first_started_at) VALUES (?, ?, 'WEEKLY_CYCLE', 'PROCESSING', ?, ?, ?, 1,"
                        + " now() - interval '3 days', now() - interval '3 days', now() - interval '3 days',"
                        + " 'proceso-muerto', now() - interval '3 days', now() - interval '3 days')", UUID.randomUUID(),
                c, S1.plusWeeks(1), S1, SeguimientoOperacionesIa.Clave.cicloSemanal(c, S1, S1.plusWeeks(1)).texto());

        assertThat(ciclo.recuperarInterrumpidos()).isEqualTo(2);

        for (long u : List.of(a, b)) {
            assertThat(planesActivosDe(u, S1.plusWeeks(1))).as("participante " + u).isEqualTo(1);
            assertThat(evidenciasConPlan(u)).isEqualTo(1);
            assertThat(operacion(u, "WEEKLY_CYCLE").get("status")).isEqualTo("COMPLETED");
        }
        assertThat(operacion(c, "WEEKLY_CYCLE").get("reason_code")).as("fuera de ventana: no se publica tarde")
                .isEqualTo("TIEMPO_EXCEDIDO");
        assertThat(planesDe(c)).isZero();
        assertThat(POSTS.get()).as("3 por participante, ninguna de mas").isEqualTo(6);
    }

    @Test
    @DisplayName("Aislamiento V66: la API de la generacion asincrona (GET /generation/latest) no ve las operaciones nuevas")
    void aislamientoDeLaApiAsincrona() {
        long u = participante();
        generacion.generar(primerPlan(u));
        assertThat(operacion(u, "FIRST_PLAN_SYNC").get("status")).isEqualTo("COMPLETED");
        assertThat(operacionesAsincronas.latest(u)).as("la operacion sincrona no aparece en la API asincrona").isEmpty();
    }

    @Autowired main.web.services.fitsense.planning.application.internal.generation.PlanGenerationOperationService
            operacionesAsincronas;
}
