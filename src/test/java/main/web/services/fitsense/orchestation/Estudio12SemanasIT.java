package main.web.services.fitsense.orchestation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import main.web.services.fitsense.RelojPruebas;
import main.web.services.fitsense.execution.domain.model.commands.FinishWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.model.commands.RecordSessionExerciseCommand;
import main.web.services.fitsense.execution.domain.model.commands.StartWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.services.WorkoutSessionCommandService;
import main.web.services.fitsense.iam.application.internal.commandservices.PasswordResetService;
import main.web.services.fitsense.iam.application.internal.consent.ConsentimientoService;
import main.web.services.fitsense.iam.application.internal.outboundservices.notification.PasswordResetNotifier;
import main.web.services.fitsense.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import main.web.services.fitsense.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import main.web.services.fitsense.iam.interfaces.acl.IamContextFacade;
import main.web.services.fitsense.notifications.application.DetectorEventosPush;
import main.web.services.fitsense.notifications.application.DispositivosPushService;
import main.web.services.fitsense.notifications.application.EmisorPush;
import main.web.services.fitsense.notifications.domain.MensajePush;
import main.web.services.fitsense.notifications.domain.TransportePush;
import main.web.services.fitsense.orchestration.application.internal.WeeklyCycleService;
import main.web.services.fitsense.orchestration.infrastructure.scheduling.CiclosSemanalesPendientes;
import main.web.services.fitsense.orchestration.infrastructure.scheduling.WeeklyCycleScheduler;
import main.web.services.fitsense.planning.application.internal.commandservices.PainMemoryService;
import main.web.services.fitsense.planning.application.internal.generation.PlanGenerationOperationService;
import main.web.services.fitsense.planning.domain.exceptions.PlanCambioDuranteLaGeneracionException;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
import main.web.services.fitsense.planning.interfaces.acl.PlanningContextFacade;
import main.web.services.fitsense.planning.interfaces.rest.TrainingPlanController;
import main.web.services.fitsense.planning.interfaces.rest.resources.SkipWorkoutResource;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.*;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import main.web.services.fitsense.shared.domain.model.valueobjects.SkipReason;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * B6 · SIMULACION DE 12 SEMANAS (E12-01 a E12-18) con participantes SINTETICOS, de extremo a extremo y sin coste:
 * <ul>
 *   <li>PostgreSQL efimero (Testcontainers); ningun dato real; nunca el usuario demo 48;</li>
 *   <li>el PLANIFICADOR REAL ({@link WeeklyCycleScheduler}) con un reloj fijado en cada lunes a las 03:00 de cada zona,
 *       en lugar de llamar a {@code runFor} con fechas;</li>
 *   <li>Replicate SIMULADO: un servidor HTTP local con el cliente de produccion. Devuelve salidas que el validador
 *       rechaza, asi que los planes salen del motor de reglas tras los reintentos; en E12-11 responde 500 sin id y en
 *       E12-14 corta la conexion. <b>Ninguna parte de esta prueba es IA real</b>;</li>
 *   <li>Firebase SIMULADO ({@link TransportePush} {@code @Primary}) y el correo de recuperacion en un buzon en
 *       memoria.</li>
 * </ul>
 * Comprobaciones cada semana y al final: ninguna semana perdida, a lo sumo un plan activo por persona y semana,
 * ninguna prediccion atribuida a otra persona, ningun plan con ejercicios excluidos por dolor ni inactivos, una
 * medicion por semana y un historial reconstruible (se escribe en {@code target/estudio_12_semanas/semanas.csv}).
 * Se ejecuta en la suite por omision ({@code mvn verify -Pintegration}).
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=true",
        "fitsense.ai.api-token=token-ficticio-de-prueba",
        "fitsense.ai.poll-interval-millis=50",
        "fitsense.ai.generate.max-per-hour=100",
        "fitsense.ai.generate.feasibility-precheck=false",
        "fitsense.plan-generation.worker.enabled=false",
        "fitsense.adaptation.progression.enabled=true",
        "fitsense.push.enabled=true",
        "fitsense.push.detector-initial-delay-ms=86400000",
        "fitsense.push.sender-initial-delay-ms=86400000",
        "fitsense.password-reset.max-requests-per-hour=10"
})
@Import(Estudio12SemanasIT.Simulados.class)
@DisplayName("B6 · 12 semanas simuladas (E12-01..E12-18): sin semanas perdidas, sin duplicados, historial reconstruible")
class Estudio12SemanasIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    // ------------------------------------------------------------------ Replicate y Firebase simulados

    enum ModoReplicate { RECHAZABLE, SIN_ID, CORTE }

    static volatile ModoReplicate MODO = ModoReplicate.RECHAZABLE;
    static final AtomicInteger POSTS = new AtomicInteger();
    static final HttpServer REPLICATE;

    static {
        try {
            REPLICATE = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        REPLICATE.createContext("/v1/models/", x -> {
            x.getRequestBody().readAllBytes();
            int n = POSTS.incrementAndGet();
            switch (MODO) {
                case SIN_ID -> responder(x, 500, "{\"detail\":\"error simulado sin id\"}");
                case CORTE -> x.close();
                default -> responder(x, 201, prediccion("e12x" + n, "starting", null));
            }
        });
        REPLICATE.createContext("/v1/predictions/", x -> {
            String id = x.getRequestURI().getPath().substring("/v1/predictions/".length());
            // Una salida JSON valida que el contrato rechaza: los planes salen del motor de reglas.
            responder(x, 200, prediccion(id, "succeeded", "\"{\\\"simulada\\\":true}\""));
        });
        REPLICATE.start();
    }

    static String base() {
        return "http://127.0.0.1:" + REPLICATE.getAddress().getPort();
    }

    static String prediccion(String id, String estado, String salida) {
        return "{\"id\":\"" + id + "\",\"status\":\"" + estado + "\",\"output\":" + (salida == null ? "null" : salida)
                + ",\"error\":null,\"urls\":{\"get\":\"" + base() + "/v1/predictions/" + id + "\",\"cancel\":\""
                + base() + "/v1/predictions/" + id + "/cancel\"}}";
    }

    static void responder(HttpExchange x, int codigo, String cuerpo) throws IOException {
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
        REPLICATE.stop(0);
    }

    static final List<String[]> ENVIOS_PUSH = new CopyOnWriteArrayList<>();
    static final Map<String, List<String>> CORREOS = new ConcurrentHashMap<>();

    @TestConfiguration
    static class Simulados {
        @Bean
        @Primary
        TransportePush firebaseSimulado() {
            return new TransportePush() {
                @Override
                public boolean disponible() {
                    return true;
                }

                @Override
                public Resultado enviar(String fid, MensajePush m) {
                    ENVIOS_PUSH.add(new String[]{fid, m.datos().get("eventType"), m.datos().get("eventId")});
                    return Resultado.enviado("projects/simulado/messages/" + UUID.randomUUID());
                }
            };
        }

        @Bean
        @Primary
        PasswordResetNotifier buzonSimulado() {
            return new PasswordResetNotifier() {
                @Override
                public void enviar(String email, String token, OffsetDateTime caduca) {
                    CORREOS.computeIfAbsent(email, k -> new CopyOnWriteArrayList<>()).add(token);
                }

                @Override
                public boolean entregaReal() {
                    return false;
                }
            };
        }
    }

    // ------------------------------------------------------------------ dependencias

    /** El reloj de vencimientos lo controla la prueba (RelojPruebas.abrirVentanaDeEjecucion). */
    @MockitoBean main.web.services.fitsense.planning.infrastructure.scheduling.OverdueWorkoutScheduler vencidas;
    /** E12-10: se interrumpe UNA adaptacion para simular la caida a mitad del cierre. */
    @MockitoSpyBean PlanningContextFacade planning;

    @Autowired WebApplicationContext contexto;
    @Autowired JdbcTemplate jdbc;
    @Autowired IamContextFacade iam;
    @Autowired WeeklyCycleService ciclo;
    @Autowired CiclosSemanalesPendientes pendientes;
    @Autowired UserProfileCommandService perfiles;
    @Autowired WorkoutSessionCommandService sesiones;
    @Autowired TrainingPlanController planes;
    @Autowired UserRepository usuarios;
    @Autowired PainMemoryService dolor;
    @Autowired ConsentimientoService consentimientos;
    @Autowired PasswordResetService recuperacion;
    @Autowired PlanGenerationOperationService operaciones;
    @Autowired WeeklyTrainingPlanCommandService servicioPlanes;
    @Autowired DispositivosPushService dispositivos;
    @Autowired DetectorEventosPush detector;
    @Autowired EmisorPush emisor;

    static final ObjectMapper JSON = new ObjectMapper();
    static final LocalDate S1 = RelojPruebas.SEMANA_REFERENCIA;
    static final int SEMANAS = 12;
    static final String CLAVE = "Clave-sintetica-123";
    static final String LIMA = "America/Lima", MADRID = "Europe/Madrid", TOKIO = "Asia/Tokyo";
    /** Orden en que llegan los lunes 03:00 locales (UTC+9, UTC+1, UTC-5). */
    static final List<String> ZONAS = List.of(TOKIO, MADRID, LIMA);

    /** Que hace cada participante cada semana. */
    enum Accion { TODO, PARCIAL, OMITIR, NADA, DOLOR, FATIGA }

    record Participante(String id, String escenario, String zona, long userId, String email,
                        java.util.function.IntFunction<Accion> patron) {}

    final List<Participante> cohorte = new ArrayList<>();
    final List<String> fallos = new ArrayList<>();
    final Map<String, Long> doloridos = new HashMap<>();
    MockMvc http;

    // ------------------------------------------------------------------ la prueba

    @Test
    @DisplayName("E12-01..E12-18 en una cohorte sintetica de 12 participantes y 3 zonas horarias")
    void doceSemanas() throws Exception {
        http = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        jdbc.update("UPDATE users SET status = 'INACTIVE'");   // solo la cohorte de esta prueba en el planificador

        alta("U01", "E12-01/E12-08 adherencia alta (sin cambios, progresion)", LIMA, k -> Accion.TODO);
        alta("U02", "E12-02 adherencia baja y recuperacion", LIMA, k -> k <= 4 ? Accion.OMITIR : Accion.TODO);
        alta("U03", "E12-03/E12-07 parciales y omitidas (adaptaciones sucesivas)", MADRID,
                k -> k % 2 == 0 ? Accion.PARCIAL : Accion.OMITIR);
        alta("U04", "E12-04 semanas sin actividad (6 y 7) e interrupcion temporal", MADRID,
                k -> k == 6 || k == 7 ? Accion.NADA : Accion.TODO);
        alta("U05", "E12-05 dolor en la semana 3 y exclusion O20", TOKIO, k -> k == 3 ? Accion.DOLOR : Accion.TODO);
        alta("U06", "E12-06 progresion, fatiga y reincorporacion", MADRID, k -> k == 5 ? Accion.FATIGA : Accion.TODO);
        alta("U07", "E12-16 retirada en la semana 6", LIMA, k -> Accion.TODO);
        alta("U08", "E12-17 recuperacion de cuenta en la semana 8", LIMA, k -> Accion.TODO);
        alta("U09", "E12-10 interrupcion durante la adaptacion (semana 7)", LIMA, k -> Accion.PARCIAL);
        alta("U10", "E12-13 cambio de version durante la generacion (semana 11)", TOKIO, k -> Accion.TODO);
        dispositivos.registrar(usuario("U01"), "fidSintetico0000U01", "ANDROID", "pe.fit.kora", "1.0.0");

        var interrumpir = new AtomicBoolean(false);
        doAnswer(inv -> {
            if (interrumpir.get() && inv.getArgument(0).equals(usuario("U09"))) {
                interrumpir.set(false);
                throw new IllegalStateException("E12-10: caida simulada a mitad de la adaptacion");
            }
            return inv.callRealMethod();
        }).when(planning).adaptWeeklyPlan(anyLong(), any(), any(), any(), any(), any());

        for (int k = 1; k <= SEMANAS; k++) {
            var semana = S1.plusWeeks(k - 1);
            for (var p : cohorte) if (activo(p)) ejecutarSemana(p, k, semana);

            if (k == 5) lateJoiner("U11", "E12-14 perdida de conexion con Replicate al generar el primer plan",
                    LIMA, semana, ModoReplicate.CORTE);
            if (k == 6) consentimientos.retirar(usuario("U07"), ConsentimientoService.Tipo.STUDY_PARTICIPATION);
            if (k == 7) interrumpir.set(true);
            if (k == 8) recuperarCuenta("U08");
            if (k == 9) lateJoiner("U12", "E12-11 prediccion sin identificador confirmado (ruta asincrona)",
                    MADRID, semana, ModoReplicate.SIN_ID);

            var lunes = semana.plusWeeks(1);
            if (k == 4) {
                // E12-09: reinicio antes del cierre: en Lima el tick de las 03:00 no llega; vuelve a las 05:00.
                tick(lunes, TOKIO, 3);
                tick(lunes, MADRID, 3);
                comprobar("E12-09", pendientes.de(ids(LIMA), semana, lunes).size() > 0,
                        "premisa: sin el tick de Lima la semana queda pendiente");
                tick(lunes, LIMA, 5);
            } else if (k == 10) {
                // E12-12: dos instancias cierran a la vez cada zona.
                for (var z : ZONAS) concurrentes(lunes, z);
            } else {
                for (var z : ZONAS) tick(lunes, z, 3);
            }
            if (k == 7) {
                comprobar("E12-10", activos(usuario("U09"), lunes) == 0,
                        "premisa: la adaptacion interrumpida no publico la semana 8");
                tick(lunes, LIMA, 4);   // el siguiente tick de la ventana la recupera
            }
            if (k == 11) cambioDeVersionDuranteLaGeneracion("U10", lunes);

            detector.detectar();
            emisor.procesar();
            invariantesSemanales(k, semana, lunes);
        }

        comprobacionesFinales();
        var csv = reconstruir();
        if (!fallos.isEmpty()) fail("Escenarios con fallos:\n - " + String.join("\n - ", fallos) + "\n\n" + csv);
    }

    // ------------------------------------------------------------------ cohorte

    long usuario(String id) {
        return cohorte.stream().filter(p -> p.id().equals(id)).findFirst().orElseThrow().userId();
    }

    List<Long> ids(String zona) {
        return cohorte.stream().filter(p -> p.zona().equals(zona)).map(Participante::userId).toList();
    }

    boolean activo(Participante p) {
        return jdbc.queryForObject("SELECT status = 'ACTIVE' AND withdrawn_at IS NULL FROM users WHERE user_id = ?",
                Boolean.class, p.userId());
    }

    /** Registro por la API real (participacion explicita), perfil y plan de la semana 1. */
    Participante alta(String id, String escenario, String zona, java.util.function.IntFunction<Accion> patron)
            throws Exception {
        String email = "e12-" + id.toLowerCase() + "-" + UUID.randomUUID() + "@pruebas.invalid";
        int codigo = http.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"%s","password":"%s","firstName":"Sintetico","timezone":"%s","acceptsStudyParticipation":true}
                """.formatted(email, CLAVE, zona))).andReturn().getResponse().getStatus();
        assertEquals(201, codigo, "alta de " + id);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1990, 3, 10), BiologicalSex.FEMALE,
                new BigDecimal("164"), new BigDecimal("63"), null, FitnessLevel.BEGINNER, GoalType.GENERAL_FITNESS,
                "Sintetico", TrainingLocation.HOME, (short) 3, List.of((short) 1, (short) 3, (short) 5), (short) 45,
                List.of("body weight"), List.of(), null, true, true, true, false, false));
        var p = new Participante(id, escenario, zona, u, email, patron);
        cohorte.add(p);
        if (!id.equals("U11") && !id.equals("U12"))
            planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        return p;
    }

    /** E12-11 y E12-14: alta tardia con el primer plan por la ruta ASINCRONA (libro V59) y Replicate fallando. */
    void lateJoiner(String id, String escenario, String zona, LocalDate semana, ModoReplicate modo) throws Exception {
        var p = alta(id, escenario, zona, k -> Accion.TODO);
        MODO = modo;
        int antes = POSTS.get();
        try {
            var op = operaciones.request(p.userId()).operation().getId();
            operaciones.claimNext();
            operaciones.execute(op);
            var fila = jdbc.queryForMap("SELECT status, plan_id FROM plan_generation_operations WHERE operation_id = ?",
                    op);
            comprobar(id, "COMPLETED".equals(fila.get("status")), "el primer plan se publica con el respaldo: " + fila);
            comprobar(id, "RULE_ENGINE".equals(jdbc.queryForObject("SELECT generation_source FROM"
                    + " weekly_training_plans WHERE plan_id = ?", String.class, fila.get("plan_id"))), "origen");
            if (modo == ModoReplicate.SIN_ID) {
                comprobar("E12-11", POSTS.get() - antes == 1,
                        "una prediccion sin id NO se reenvia: POST = " + (POSTS.get() - antes));
                comprobar("E12-11", jdbc.queryForObject("SELECT count(*) FROM plan_generation_predictions WHERE"
                        + " operation_id = ? AND status = 'NO_CONFIRMADA'", Integer.class, op) == 1,
                        "queda anotada como NO_CONFIRMADA en el libro");
            }
        } finally {
            MODO = ModoReplicate.RECHAZABLE;
        }
    }

    // ------------------------------------------------------------------ una semana de una persona

    void ejecutarSemana(Participante p, int k, LocalDate semana) {
        var plan = jdbc.queryForList("SELECT plan_id FROM weekly_training_plans WHERE user_id = ? AND week_start_date = ?"
                + " AND status = 'ACTIVE'", Long.class, p.userId(), semana);
        if (plan.isEmpty()) return;   // alta tardia (aun no tiene la semana) o retirada
        RelojPruebas.abrirVentanaDeEjecucion(jdbc, p.userId());
        var entrenamientos = jdbc.queryForList("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ?"
                + " AND status = 'SCHEDULED' ORDER BY scheduled_date", Long.class, plan.get(0));
        var accion = p.patron().apply(k);
        for (int i = 0; i < entrenamientos.size(); i++) {
            long w = entrenamientos.get(i);
            switch (accion) {
                case NADA -> { }
                case TODO -> hacer(p, w, 1.0, -1, (short) 6);
                case PARCIAL -> {
                    if (i == 0) omitir(p, w, SkipReason.LACK_OF_TIME);
                    else hacer(p, w, 0.5, -1, (short) 7);
                }
                case OMITIR -> {
                    if (i == 0) hacer(p, w, 1.0, -1, (short) 6);
                    else omitir(p, w, SkipReason.LACK_OF_MOTIVATION);
                }
                case FATIGA -> hacer(p, w, 0.6, -1, (short) 9);
                case DOLOR -> {
                    if (i == 0) {
                        long dolio = jdbc.queryForObject("SELECT exercise_id FROM planned_workout_exercises WHERE"
                                + " planned_workout_id = ? ORDER BY exercise_order LIMIT 1", Long.class, w);
                        doloridos.put(p.id(), dolio);
                        hacer(p, w, 1.0, dolio, (short) 6);
                    } else hacer(p, w, 1.0, -1, (short) 6);
                }
            }
        }
    }

    void omitir(Participante p, long w, SkipReason motivo) {
        planes.skip(UserDetailsImpl.build(usuarios.findById(p.userId()).orElseThrow()), w,
                new SkipWorkoutResource(motivo));
    }

    void hacer(Participante p, long w, double fraccion, long conDolor, short rpe) {
        long u = p.userId();
        long s = sesiones.handle(new StartWorkoutSessionCommand(u, w)).orElseThrow().getId();
        for (var f : jdbc.queryForList("SELECT planned_exercise_id, exercise_id, prescription_type, planned_sets,"
                + " planned_reps, planned_duration_seconds FROM planned_workout_exercises WHERE planned_workout_id = ?"
                + " ORDER BY exercise_order", w)) {
            long pe = ((Number) f.get("planned_exercise_id")).longValue();
            if (((Number) f.get("exercise_id")).longValue() == conDolor) {
                sesiones.handle(new RecordSessionExerciseCommand(u, s, pe, (short) 0, null, null, null,
                        SkipReason.PAIN_OR_DISCOMFORT));
                continue;
            }
            boolean duracion = "DURATION".equals(f.get("prescription_type"));
            int series = f.get("planned_sets") == null ? 1 : ((Number) f.get("planned_sets")).intValue();
            int total = (int) Math.max(1, Math.round(series * fraccion
                    * ((Number) f.get(duracion ? "planned_duration_seconds" : "planned_reps")).intValue()));
            sesiones.handle(new RecordSessionExerciseCommand(u, s, pe, (short) series, duracion ? null : total,
                    duracion ? total : null, null, null));
        }
        sesiones.handle(new FinishWorkoutSessionCommand(u, s, rpe, (short) 4, (short) 40));
    }

    // ------------------------------------------------------------------ cierre semanal real

    WeeklyCycleScheduler planificador(Instant instante) {
        return new WeeklyCycleScheduler(iam, ciclo, pendientes, 24, 3, Optional.of(Clock.fixed(instante, ZoneOffset.UTC)));
    }

    void tick(LocalDate lunes, String zona, int hora) {
        planificador(lunes.atTime(hora, 0).atZone(ZoneId.of(zona)).toInstant()).runHourly();
    }

    void concurrentes(LocalDate lunes, String zona) throws Exception {
        var instante = lunes.atTime(3, 0).atZone(ZoneId.of(zona)).toInstant();
        var salida = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        for (int i = 0; i < 2; i++)
            pool.submit(() -> {
                salida.await();
                planificador(instante).runHourly();
                return null;
            });
        salida.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(5, TimeUnit.MINUTES));
        // La que encontro EN_CURSO deja el cierre a la otra; si alguno quedo a medias, el tick de las 04:00 lo acaba.
        tick(lunes, zona, 4);
    }

    // ------------------------------------------------------------------ escenarios puntuales

    void recuperarCuenta(String id) throws Exception {
        var p = cohorte.stream().filter(x -> x.id().equals(id)).findFirst().orElseThrow();
        String anterior = JSON.readTree(http.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(p.email(), CLAVE))).andReturn().getResponse()
                .getContentAsString()).get("token").asText();
        recuperacion.solicitar(p.email());
        var token = CORREOS.get(p.email()).get(0);
        recuperacion.confirmar(token, "Clave-nueva-sintetica-456");
        int conAnterior = http.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + anterior))
                .andReturn().getResponse().getStatus();
        comprobar("E12-17", conAnterior == 401, "el JWT anterior a la recuperacion deja de valer: " + conAnterior);
        int nuevo = http.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"Clave-nueva-sintetica-456\"}".formatted(p.email())))
                .andReturn().getResponse().getStatus();
        comprobar("E12-17", nuevo == 200, "se entra con la contrasena nueva");
    }

    /** E12-13: mientras se prepara una version, otro proceso publica otra; la antigua no se publica encima. */
    void cambioDeVersionDuranteLaGeneracion(String id, LocalDate semana) {
        long u = usuario(id);
        var preparado = servicioPlanes.prepare(new GenerateWeeklyPlanCommand(u, semana, PlanAdjustment.none(), true,
                null, null));
        var generado = servicioPlanes.generate(preparado);
        var otro = servicioPlanes.prepare(new GenerateWeeklyPlanCommand(u, semana, PlanAdjustment.none(), true, null,
                null));
        servicioPlanes.persist(otro, servicioPlanes.generate(otro));
        try {
            servicioPlanes.persist(preparado, generado);
            fallos.add("E12-13: se publico encima de una version que cambio durante la generacion");
        } catch (PlanCambioDuranteLaGeneracionException esperado) {
            comprobar("E12-13", activos(u, semana) == 1, "una sola version activa");
        }
    }

    // ------------------------------------------------------------------ invariantes

    void comprobar(String escenario, boolean ok, String que) {
        if (!ok) fallos.add(escenario + ": " + que);
    }

    int activos(long u, LocalDate semana) {
        return jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ? AND week_start_date = ?"
                + " AND status = 'ACTIVE'", Integer.class, u, semana);
    }

    void invariantesSemanales(int k, LocalDate semana, LocalDate lunes) {
        for (var p : cohorte) {
            boolean retirado = p.id().equals("U07") && k >= 6;
            // Las altas tardias (E12-11, E12-14) reciben el primer plan en la semana REAL de hoy, no en la simulada:
            // solo se comprueba su primer plan (en lateJoiner), no su calendario.
            if (p.id().equals("U11") || p.id().equals("U12")) continue;
            int act = activos(p.userId(), lunes);
            if (retirado) {
                comprobar("E12-16", act == 0, "semana " + (k + 1) + ": tras retirarse no recibe planes (" + act + ")");
                continue;
            }
            comprobar(p.id(), act == 1, "semana " + (k + 1) + ": un plan activo y solo uno (hay " + act + ")");
            comprobar(p.id(), jdbc.queryForObject("SELECT count(*) FROM weekly_user_metrics WHERE user_id = ? AND"
                    + " week_start_date = ?", Integer.class, p.userId(), semana) == 1,
                    "semana " + k + ": una medicion");
        }
        // Ninguna prediccion atribuida a otra persona; ningun plan con un ejercicio inactivo.
        comprobar("integridad", jdbc.queryForObject("SELECT count(*) FROM plan_generation_predictions p JOIN"
                + " plan_generation_operations o USING (operation_id) WHERE p.user_id <> o.user_id", Integer.class) == 0,
                "semana " + k + ": prediccion de otra persona");
        comprobar("integridad", jdbc.queryForObject("SELECT count(*) FROM planned_workout_exercises pe JOIN"
                + " exercises e USING (exercise_id) JOIN planned_workouts w USING (planned_workout_id) JOIN"
                + " weekly_training_plans t USING (plan_id) WHERE t.status <> 'REPLACED' AND NOT e.is_active",
                Integer.class) == 0, "semana " + k + ": ejercicio inactivo publicado");
        // E12-05: lo que dolio (y sus equivalentes) no vuelve en ninguna semana posterior al dolor.
        if (doloridos.containsKey("U05") && k >= 3) {
            long u = usuario("U05");
            var excluidos = dolor.excluidosActivos(u);
            comprobar("E12-05", excluidos.contains(doloridos.get("U05")), "el ejercicio que dolio quedo excluido");
            var publicados = jdbc.queryForList("SELECT DISTINCT pe.exercise_id FROM planned_workout_exercises pe JOIN"
                    + " planned_workouts w USING (planned_workout_id) JOIN weekly_training_plans t USING (plan_id)"
                    + " WHERE t.user_id = ? AND t.week_start_date > ? AND t.status <> 'REPLACED'", Long.class, u,
                    S1.plusWeeks(2));
            var vuelve = new TreeSet<>(publicados);
            vuelve.retainAll(excluidos);
            comprobar("E12-05", vuelve.isEmpty(), "semana " + (k + 1) + ": vuelve lo excluido por dolor " + vuelve);
        }
    }

    void comprobacionesFinales() {
        // E12-06: hubo progresion en algun momento (adherencia alta sostenida).
        comprobar("E12-06", jdbc.queryForObject("SELECT count(*) FROM plan_adaptation_attempts WHERE user_id IN (?, ?)"
                + " AND progressed_sessions IS NOT NULL AND progressed_sessions::text <> '{}'", Integer.class,
                usuario("U01"), usuario("U06")) > 0, "ninguna progresion en 12 semanas de adherencia alta");
        // E12-07: adaptaciones sucesivas en U03.
        comprobar("E12-07", jdbc.queryForObject("SELECT count(DISTINCT new_week_start_date) FROM"
                + " plan_adaptation_attempts WHERE user_id = ? AND decision IN ('ADAPT_LOCALIZED', 'ADAPT_EXPANDED')",
                Integer.class, usuario("U03")) >= 3, "menos de 3 semanas adaptadas con adherencia baja sostenida");
        // E12-08: semanas sin cambios en U01.
        comprobar("E12-08", jdbc.queryForObject("SELECT count(*) FROM plan_adaptation_attempts WHERE user_id = ?"
                + " AND decision = 'NO_CHANGE'", Integer.class, usuario("U01")) >= 1, "ninguna semana sin cambios");
        // E12-16: los datos del retirado se conservan.
        comprobar("E12-16", jdbc.queryForObject("SELECT count(*) FROM weekly_user_metrics WHERE user_id = ?",
                Integer.class, usuario("U07")) >= 5, "las semanas anteriores a la retirada se conservan");
        // E12-15: tres zonas, cada una cerrada; ninguna semana perdida en ninguna.
        for (var z : ZONAS)
            comprobar("E12-15", cohorte.stream().filter(p -> p.zona().equals(z)).allMatch(p ->
                    jdbc.queryForObject("SELECT count(*) FROM weekly_user_metrics WHERE user_id = ?", Integer.class,
                            p.userId()) > 0), "zona " + z + " sin cierres");
        // Firebase simulado: un aviso por plan semanal publicado, sin duplicados (dedupe por evento).
        var u01 = String.valueOf(usuario("U01"));
        long eventos = jdbc.queryForObject("SELECT count(*) FROM notification_events WHERE user_id = ?", Long.class,
                usuario("U01"));
        long distintos = ENVIOS_PUSH.stream().filter(e -> e[0].equals("fidSintetico0000U01")).map(e -> e[2]).distinct()
                .count();
        long envios = ENVIOS_PUSH.stream().filter(e -> e[0].equals("fidSintetico0000U01")).count();
        comprobar("push", eventos >= SEMANAS - 1 && envios == distintos, "U01 " + u01 + ": eventos=" + eventos
                + " envios=" + envios + " distintos=" + distintos);
        // E12-12: ninguna version duplicada en la semana de los cierres concurrentes.
        comprobar("E12-12", jdbc.queryForObject("SELECT count(*) FROM (SELECT user_id, week_start_date, count(*) n FROM"
                + " weekly_training_plans WHERE status = 'ACTIVE' GROUP BY 1, 2 HAVING count(*) > 1) d",
                Integer.class) == 0, "dos planes activos en la misma semana");
    }

    /** E12-18: reconstruccion del historial en solo lectura y CSV para cotejar entre ejecuciones. */
    String reconstruir() throws IOException {
        var csv = new StringBuilder("participante,escenario,semana,origen_plan,versiones,adherencia_ponderada,"
                + "decision,estado_adaptacion\n");
        for (var p : cohorte) {
            for (int k = 1; k <= SEMANAS + 1; k++) {
                var semana = S1.plusWeeks(k - 1);
                var filas = jdbc.queryForList("""
                        SELECT (SELECT generation_source FROM weekly_training_plans WHERE user_id = :u
                                  AND week_start_date = :s AND status <> 'REPLACED' LIMIT 1) AS origen,
                               (SELECT count(*) FROM weekly_training_plans WHERE user_id = :u AND week_start_date = :s) AS versiones,
                               (SELECT weighted_adherence_pct FROM weekly_user_metrics WHERE user_id = :u
                                  AND week_start_date = :s) AS adherencia,
                               (SELECT decision FROM plan_adaptation_attempts WHERE user_id = :u
                                  AND new_week_start_date = :s ORDER BY attempt_id DESC LIMIT 1) AS decision,
                               (SELECT status FROM plan_adaptation_attempts WHERE user_id = :u
                                  AND new_week_start_date = :s ORDER BY attempt_id DESC LIMIT 1) AS estado
                        """.replace(":u", String.valueOf(p.userId())).replace(":s", "'" + semana + "'"));
                var f = filas.get(0);
                csv.append(p.id()).append(',').append('"').append(p.escenario()).append('"').append(',').append(k)
                        .append(',').append(f.get("origen")).append(',').append(f.get("versiones")).append(',')
                        .append(f.get("adherencia")).append(',').append(f.get("decision")).append(',')
                        .append(f.get("estado")).append('\n');
            }
        }
        // Linaje: toda version 2+ apunta a una version anterior de la misma persona y semana.
        comprobar("E12-18", jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans h LEFT JOIN"
                + " weekly_training_plans p ON p.plan_id = h.parent_plan_id WHERE h.plan_version > 1 AND (p.plan_id IS NULL"
                + " OR p.user_id <> h.user_id OR p.week_start_date <> h.week_start_date)", Integer.class) == 0,
                "linaje roto");
        var salida = Path.of("target", "estudio_12_semanas");
        Files.createDirectories(salida);
        Files.writeString(salida.resolve("semanas.csv"), csv);
        System.out.println("E12_RESUMEN\n" + csv);
        return csv.toString();
    }
}
