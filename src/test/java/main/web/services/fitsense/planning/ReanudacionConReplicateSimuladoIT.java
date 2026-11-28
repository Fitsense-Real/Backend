package main.web.services.fitsense.planning;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import main.web.services.fitsense.planning.application.internal.generation.FirstPlanGenerationRunner;
import main.web.services.fitsense.planning.application.internal.generation.PipelineFirstPlanGenerationRunner;
import main.web.services.fitsense.planning.application.internal.generation.PlanGenerationOperationService;
import main.web.services.fitsense.planning.application.internal.outboundservices.acl.ExternalIamService;
import main.web.services.fitsense.planning.application.internal.outboundservices.acl.ExternalProfilingService;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
import main.web.services.fitsense.planning.infrastructure.generation.ai.DiarioDePredicciones;
import main.web.services.fitsense.planning.infrastructure.generation.ai.JdbcDiarioDePredicciones;
import main.web.services.fitsense.planning.infrastructure.persistence.jpa.repositories.PlanGenerationOperationRepository;
import main.web.services.fitsense.planning.infrastructure.persistence.jpa.repositories.WeeklyTrainingPlanRepository;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.*;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

/**
 * V59 · REANUDACION DE EXTREMO A EXTREMO con el pipeline REAL (IA encendida, validadores, respaldo y publicacion)
 * contra un Replicate FALSO local (com.sun.net.httpserver en un puerto efimero). No sale nada a la red y no se gasta
 * ningun credito: el token es ficticio y la URL es localhost. ESTO ES UNA SIMULACION, no una ejecucion con Replicate.
 * <p>
 * El falso cuenta los POST aceptados (= predicciones que Replicate cobraria) y, por omision, devuelve una salida que
 * los validadores rechazan, para que el pipeline haga sus 3 intentos y publique el respaldo determinista. Puede
 * ademas: bloquear los GET (prediccion en curso), responder "processing" N veces, cortar el POST tras aceptarlo,
 * rechazarlo (4xx), fallar los GET (503) o devolver una salida valida.
 * <p>
 * El «reinicio» se simula como lo ve la base tras la muerte del proceso: el lease vence (o lo tiene otro proceso) y el
 * barrido la devuelve a la cola. «Otra instancia» es un segundo PlanGenerationOperationService con otro propietario
 * sobre la misma base y el mismo Replicate falso.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=true",
        "fitsense.ai.api-token=token-ficticio-de-prueba",
        "fitsense.ai.poll-interval-millis=250",
        "fitsense.ai.generate.max-per-hour=100",
        "fitsense.plan-generation.worker.enabled=false"
})
@DisplayName("V59 · reanudacion con Replicate simulado: el pipeline real no paga dos veces")
class ReanudacionConReplicateSimuladoIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    // ------------------------------------------------------------ Replicate falso
    static final HttpServer REPLICATE;
    /** POST aceptados: predicciones creadas (y cobradas) en Replicate. */
    static final AtomicInteger POSTS = new AtomicInteger();
    /** POST rechazados con 4xx: Replicate no crea nada. */
    static final AtomicInteger RECHAZADOS = new AtomicInteger();
    static final AtomicInteger GETS = new AtomicInteger();
    static final AtomicInteger GET_ESPERANDO = new AtomicInteger();
    /** POST /cancel recibidos: predicciones que el backend pidio cancelar. */
    static final AtomicInteger CANCELACIONES = new AtomicInteger();
    /** Acepta la prediccion (la «cobra») y corta la conexion sin responder: el id nunca llega al backend. */
    static final AtomicBoolean CORTAR_TRAS_ACEPTAR = new AtomicBoolean();
    static final AtomicBoolean RECHAZAR_POST = new AtomicBoolean();
    static final AtomicBoolean GET_FALLA = new AtomicBoolean();
    /** Cuantos GET mas responden "processing" antes de "succeeded" (una prediccion larga). */
    static final AtomicInteger EN_CURSO_RESTANTES = new AtomicInteger();
    /** Mientras no sea null, cada GET espera aqui: la prediccion sigue en curso en Replicate. */
    static volatile CountDownLatch BLOQUEO_GET;
    /** Valor JSON del campo output: por omision, una cadena que el contrato rechaza. */
    static volatile String SALIDA = "\"{}\"";

    static {
        try {
            REPLICATE = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        // Varias peticiones a la vez (dos instancias sondeando): sin ejecutor propio, un GET bloqueado pararia todo.
        REPLICATE.setExecutor(Executors.newCachedThreadPool(r -> {
            var t = new Thread(r, "replicate-falso");
            t.setDaemon(true);
            return t;
        }));
        REPLICATE.createContext("/v1/models/", ReanudacionConReplicateSimuladoIT::crear);
        REPLICATE.createContext("/v1/predictions/", ReanudacionConReplicateSimuladoIT::consultar);
        REPLICATE.start();
    }

    static String base() {
        return "http://127.0.0.1:" + REPLICATE.getAddress().getPort();
    }

    private static void crear(HttpExchange x) throws IOException {
        x.getRequestBody().readAllBytes();
        if (!"POST".equals(x.getRequestMethod())) {
            responder(x, 405, "{}");
            return;
        }
        if (RECHAZAR_POST.get()) {
            RECHAZADOS.incrementAndGet();
            responder(x, 422, "{\"detail\":\"entrada invalida\"}");
            return;
        }
        int n = POSTS.incrementAndGet();
        if (CORTAR_TRAS_ACEPTAR.get()) {
            x.close();          // aceptada y cobrada, pero sin respuesta
            return;
        }
        responder(x, 201, prediccion("p" + n, "starting", null));
    }

    private static void consultar(HttpExchange x) throws IOException {
        GETS.incrementAndGet();
        String id = x.getRequestURI().getPath().substring("/v1/predictions/".length());
        if (id.endsWith("/cancel")) {
            CANCELACIONES.incrementAndGet();
            responder(x, 200, prediccion(id.substring(0, id.length() - "/cancel".length()), "canceled", null));
            return;
        }
        if (GET_FALLA.get()) {
            responder(x, 503, "{\"detail\":\"no disponible\"}");
            return;
        }
        var bloqueo = BLOQUEO_GET;
        if (bloqueo != null) {
            GET_ESPERANDO.incrementAndGet();
            try {
                bloqueo.await(20, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                GET_ESPERANDO.decrementAndGet();
            }
        }
        if (EN_CURSO_RESTANTES.getAndDecrement() > 0) {
            responder(x, 200, prediccion(id, "processing", null));
            return;
        }
        responder(x, 200, prediccion(id, "succeeded", SALIDA));
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
        // G5M (23/11/2026): el formato de produccion es gpt-5-mini directo. El libro V59 y la reanudacion se prueban con
        // esa peticion (system_prompt + prompt + reasoning_effort/verbosity/max_completion_tokens); structured sigue
        // cubierto en DiarioDePrediccionesReplicateTest y ReplicateSchemaRoutingTest.
        r.add("fitsense.ai.base-url", () -> base() + "/v1/models/openai/gpt-5-mini/predictions");
    }

    @AfterAll
    static void parar() {
        REPLICATE.stop(0);
    }

    // ------------------------------------------------------------ contexto
    static final AtomicInteger SECUENCIA = new AtomicInteger();

    @Autowired PlanGenerationOperationService service;
    @Autowired UserProfileCommandService perfiles;
    @Autowired WeeklyTrainingPlanCommandService planes;
    @Autowired JdbcTemplate jdbc;
    @Autowired JdbcDiarioDePredicciones libro;
    @Autowired PlatformTransactionManager transacciones;
    @Autowired PlanGenerationOperationRepository operaciones;
    @Autowired WeeklyTrainingPlanRepository planesRepo;
    @Autowired ExternalIamService iam;
    @Autowired ExternalProfilingService perfilesExternos;
    @Autowired ObjectProvider<Clock> reloj;
    @Autowired main.web.services.fitsense.planning.application.internal.commandservices.PlanGenerationPipeline pipeline;
    @MockitoSpyBean FirstPlanGenerationRunner runner;

    /** Cuantas ejecuciones pierden el lease justo antes de publicar (el proceso «muere» ahi). */
    final AtomicInteger morirAntesDePublicar = new AtomicInteger();
    /** La siguiente ejecucion «muere» DENTRO de la transaccion de publicacion, despues de escribir el plan. */
    final AtomicBoolean morirDentroDeLaPublicacion = new AtomicBoolean();
    /** El proceso ya murio (R16): solo es verdadero desde la muerte, no antes (antes envia y publica con normalidad). */
    final AtomicBoolean procesoMuerto = new AtomicBoolean();
    final AtomicInteger ejecucionesDelPipeline = new AtomicInteger();

    @BeforeEach
    void preparar() {
        POSTS.set(0);
        RECHAZADOS.set(0);
        GETS.set(0);
        GET_ESPERANDO.set(0);
        CANCELACIONES.set(0);
        CORTAR_TRAS_ACEPTAR.set(false);
        RECHAZAR_POST.set(false);
        GET_FALLA.set(false);
        EN_CURSO_RESTANTES.set(0);
        BLOQUEO_GET = null;
        SALIDA = "\"{}\"";
        morirAntesDePublicar.set(0);
        morirDentroDeLaPublicacion.set(false);
        procesoMuerto.set(false);
        ejecucionesDelPipeline.set(0);
        // El corte del proveedor (R-1, en memoria) se abre tras 3 fallos seguidos: las pruebas de errores del
        // proveedor lo abririan para las siguientes. Cada prueba empieza con el proveedor disponible.
        var corte = (main.web.services.fitsense.planning.infrastructure.generation.ai.AirCircuitBreaker)
                org.springframework.test.util.ReflectionTestUtils.getField(pipeline, "breaker");
        org.springframework.test.util.ReflectionTestUtils.setField(corte, "reabrirDespuesDe", Instant.EPOCH);
        corte.registrarRespuestaDelProveedor();
        jdbc.update("UPDATE plan_generation_operations SET status = 'FAILED', reason_code = 'LIMPIEZA_DE_PRUEBA',"
                + " finished_at = now(), updated_at = now(), lease_owner = NULL, lease_expires_at = NULL"
                + " WHERE status IN ('PENDING', 'PROCESSING')");
        var real = new PipelineFirstPlanGenerationRunner(planes, transacciones);
        doAnswer(inv -> {
            ejecucionesDelPipeline.incrementAndGet();
            Long u = inv.getArgument(0);
            Runnable antesDePublicar = inv.getArgument(1);
            if (morirDentroDeLaPublicacion.getAndSet(false)) return publicarYMorir(u, antesDePublicar);
            return real.run(u, () -> {
                // Otro proceso se queda con la operacion. Va por otra conexion (otro hilo, autocommit): la
                // transaccion de publicacion no puede deshacerlo.
                if (morirAntesDePublicar.getAndDecrement() > 0)
                    CompletableFuture.runAsync(() -> jdbc.update("UPDATE plan_generation_operations"
                            + " SET lease_owner = 'otro-proceso' WHERE user_id = ? AND status = 'PROCESSING'", u)).join();
                antesDePublicar.run();
            });
        }).when(runner).run(any(), any());
    }

    /** R16: el mismo pipeline, pero el proceso muere tras escribir el plan y antes del commit (se deshace todo). */
    private Long publicarYMorir(Long u, Runnable antesDePublicar) {
        var command = GenerateWeeklyPlanCommand.firstPlan(u, planes.firstPlanWeekStart(u));
        var prepared = planes.prepare(command);
        var generated = planes.generate(prepared);
        return new TransactionTemplate(transacciones).execute(s -> {
            antesDePublicar.run();
            planes.persist(prepared, generated);
            procesoMuerto.set(true);
            throw new IllegalStateException("el proceso muere a mitad del commit de la publicacion");
        });
    }

    long participante() {
        String email = "v59e2e-" + SECUENCIA.incrementAndGet() + "-" + UUID.randomUUID() + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'V59', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1990, 3, 10), BiologicalSex.FEMALE,
                new BigDecimal("164"), new BigDecimal("63"), null, FitnessLevel.INTERMEDIATE, GoalType.GENERAL_FITNESS,
                "Generacion asincrona V59", TrainingLocation.GYM, (short) 2, List.of((short) 2, (short) 4), (short) 55,
                List.of("body weight", "dumbbell", "barbell", "bench", "cable", "leverage machine", "band",
                        "pull-up bar"), List.of(), "Ninguna", true, true, true, false, false));
        return u;
    }

    /** Otra instancia del backend: otro propietario, misma base, mismo Replicate. */
    PlanGenerationOperationService otraInstancia() {
        var s = new PlanGenerationOperationService(operaciones, planesRepo, planes, iam, perfilesExternos, runner, jdbc,
                transacciones, reloj, 1800, 1800, 3, 90);
        s.setDiario(libro);
        assertThat(s.owner()).isNotEqualTo(service.owner());
        return s;
    }

    private String estado(UUID id) {
        return jdbc.queryForObject("SELECT status FROM plan_generation_operations WHERE operation_id = ?",
                String.class, id);
    }

    private String motivo(UUID id) {
        return jdbc.queryForObject("SELECT reason_code FROM plan_generation_operations WHERE operation_id = ?",
                String.class, id);
    }

    private int planesDe(long u) {
        return jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ?", Integer.class, u);
    }

    private String origenDelPlan(long u) {
        return jdbc.queryForObject("SELECT generation_source FROM weekly_training_plans WHERE user_id = ?",
                String.class, u);
    }

    private int ejecuciones(UUID id) {
        return jdbc.queryForObject("SELECT executions FROM plan_generation_operations WHERE operation_id = ?",
                Integer.class, id);
    }

    /** El proceso murio: lease vencido; el barrido la devuelve a la cola y se vuelve a reclamar. */
    private void reiniciar(UUID id) {
        jdbc.update("UPDATE plan_generation_operations SET lease_expires_at = now() - interval '1 second'"
                + " WHERE operation_id = ?", id);
        service.recoverStale();
        assertThat(estado(id)).isEqualTo("PENDING");
        assertThat(service.claimNext()).contains(id);
    }

    private static void esperarHasta(BooleanSupplier condicion, String que) {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(60);
        while (!condicion.getAsBoolean()) {
            if (System.nanoTime() > limite) throw new AssertionError("no ocurrio a tiempo: " + que);
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError(e);
            }
        }
    }

    // ================================================================ reinicios (R05, R06, R10, R16)

    @Test
    @DisplayName("R05/R06 · muere antes de publicar con todas las predicciones pagadas: la reanudacion no crea ninguna")
    void reanudacionNoPagaOtraVez() {
        long u = participante();
        var id = service.request(u).operation().getId();
        morirAntesDePublicar.set(1);
        assertThat(service.claimNext()).contains(id);
        service.execute(id);

        int pagadas = POSTS.get();
        assertThat(pagadas).as("la primera ejecucion hace todos los intentos de IA").isEqualTo(3);
        assertThat(estado(id)).as("perdio el lease: no publica ni cierra").isEqualTo("PROCESSING");
        assertThat(planesDe(u)).isZero();

        reiniciar(id);
        service.execute(id);

        assertThat(POSTS.get()).as("la reanudacion reutiliza las 3 salidas del libro: 0 POST nuevos").isEqualTo(3);
        assertThat(estado(id)).isEqualTo("COMPLETED");
        assertThat(planesDe(u)).isEqualTo(1);
        assertThat(ejecuciones(id)).isEqualTo(2);
        var r = libro.recuento(id);
        assertThat(r.iniciadas()).isEqualTo(3);
        assertThat(r.completadas()).isEqualTo(3);
        assertThat(r.reutilizadas()).isZero();
    }

    @Test
    @DisplayName("dos reinicios seguidos: siguen siendo 3 predicciones y 1 plan (en #71: 6 y FAILED sin plan)")
    void dosReiniciosSiguenSiendoTres() {
        long u = participante();
        var id = service.request(u).operation().getId();
        morirAntesDePublicar.set(2);
        assertThat(service.claimNext()).contains(id);
        service.execute(id);
        reiniciar(id);
        service.execute(id);
        assertThat(estado(id)).isEqualTo("PROCESSING");
        reiniciar(id);
        service.execute(id);

        assertThat(POSTS.get()).isEqualTo(3);
        assertThat(estado(id)).isEqualTo("COMPLETED");
        assertThat(planesDe(u)).isEqualTo(1);
        assertThat(ejecuciones(id)).isEqualTo(3);
    }

    @Test
    @DisplayName("R16 · el proceso muere DENTRO de la publicacion: se deshace entera; la reanudacion publica UNA, 0 POST nuevos")
    void muereDuranteLaPublicacion() {
        long u = participante();
        var id = service.request(u).operation().getId();
        morirDentroDeLaPublicacion.set(true);
        assertThat(service.claimNext()).contains(id);
        // stopping = true desde la muerte: la excepcion es la muerte del proceso, no un resultado (nadie la registra).
        service.execute(id, procesoMuerto::get);

        assertThat(planesDe(u)).as("rollback: ni plan ni sesiones a medias").isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM planned_workouts w JOIN weekly_training_plans p"
                + " ON p.plan_id = w.plan_id WHERE p.user_id = ?", Integer.class, u)).isZero();
        assertThat(estado(id)).isEqualTo("PROCESSING");
        assertThat(POSTS.get()).isEqualTo(3);

        reiniciar(id);
        service.execute(id);
        assertThat(estado(id)).isEqualTo("COMPLETED");
        assertThat(planesDe(u)).isEqualTo(1);
        assertThat(POSTS.get()).as("lo pagado se lee del libro").isEqualTo(3);
    }

    @Test
    @DisplayName("R21 · apagado ordenado (SIGTERM) con la prediccion en curso: no la cancela, no publica el respaldo y"
            + " vuelve a la cola; la reanudacion sigue la misma prediccion por su id (3 POST, no 4)")
    void apagadoOrdenadoConPrediccionEnCurso() throws Exception {
        long u = participante();
        var id = service.request(u).operation().getId();
        EN_CURSO_RESTANTES.set(1_000_000);         // la prediccion 1 sigue "processing" en Replicate
        assertThat(service.claimNext()).contains(id);
        var parando = new AtomicBoolean();
        var hilo = new Thread(() -> service.execute(id, parando::get), "generacion-que-se-apaga");
        hilo.start();
        esperarHasta(() -> GETS.get() >= 2, "la prediccion 1 se esta sondeando");
        assertThat(POSTS.get()).isEqualTo(1);

        // Lo que hace PlanGenerationWorker.stop() al recibir SIGTERM: marca el apagado e interrumpe el hilo.
        parando.set(true);
        hilo.interrupt();
        hilo.join(TimeUnit.SECONDS.toMillis(30));
        assertThat(hilo.isAlive()).isFalse();

        assertThat(CANCELACIONES.get()).as("la prediccion pagada no se cancela por el apagado").isZero();
        assertThat(POSTS.get()).as("ni se envia otra").isEqualTo(1);
        assertThat(planesDe(u)).as("el apagado no publica el respaldo del motor de reglas").isZero();
        assertThat(estado(id)).isEqualTo("PROCESSING");
        assertThat(jdbc.queryForObject("SELECT status FROM plan_generation_predictions WHERE operation_id = ?"
                + " AND attempt = 1", String.class, id)).as("su id queda en el libro para reanudarla").isEqualTo("ENVIADA");
        assertThat(service.requeueOnShutdown(List.of(id))).isEqualTo(1);
        assertThat(estado(id)).isEqualTo("PENDING");

        // La instancia siguiente la reclama y sigue la MISMA prediccion por su id.
        EN_CURSO_RESTANTES.set(0);
        assertThat(service.claimNext()).contains(id);
        service.execute(id);
        assertThat(estado(id)).isEqualTo("COMPLETED");
        assertThat(planesDe(u)).isEqualTo(1);
        assertThat(POSTS.get()).as("intento 1 reanudado por su id + intentos 2 y 3").isEqualTo(3);
        assertThat(libro.recuento(id).iniciadas()).isEqualTo(3);
        assertThat(libro.recuento(id).canceladas()).isZero();
    }

    // ================================================================ dos instancias (R03, R04, R17)

    @Test
    @DisplayName("R17/R04/R05 · dos instancias: A se queda sin lease con la prediccion en curso; B la sigue por su id y"
            + " publica; A no publica ni paga mas. 3 POST, 1 plan")
    void dosInstanciasNoDuplicanElTrabajo() throws Exception {
        long u = participante();
        var id = service.request(u).operation().getId();
        BLOQUEO_GET = new CountDownLatch(1);
        assertThat(service.claimNext()).contains(id);
        var a = CompletableFuture.runAsync(() -> service.execute(id));
        esperarHasta(() -> GET_ESPERANDO.get() >= 1, "A sondea su prediccion");
        assertThat(POSTS.get()).isEqualTo(1);

        // A deja de latir (pausa larga, particion de red): su lease vence y la instancia B barre y reclama.
        jdbc.update("UPDATE plan_generation_operations SET lease_expires_at = now() - interval '1 second'"
                + " WHERE operation_id = ?", id);
        var b = otraInstancia();
        b.recoverStale();
        assertThat(estado(id)).isEqualTo("PENDING");
        assertThat(b.claimNext()).contains(id);
        var fb = CompletableFuture.runAsync(() -> b.execute(id));
        esperarHasta(() -> GET_ESPERANDO.get() >= 2, "B sigue LA MISMA prediccion por su id");
        assertThat(POSTS.get()).as("B no crea otra prediccion para el intento en curso").isEqualTo(1);

        var bloqueo = BLOQUEO_GET;
        BLOQUEO_GET = null;
        bloqueo.countDown();
        a.get(120, TimeUnit.SECONDS);
        fb.get(120, TimeUnit.SECONDS);

        assertThat(POSTS.get()).as("intento 1 compartido + intentos 2 y 3 solo de B").isEqualTo(3);
        assertThat(estado(id)).isEqualTo("COMPLETED");
        assertThat(planesDe(u)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT lease_owner FROM plan_generation_operations WHERE operation_id = ?",
                String.class, id)).isNull();
        assertThat(libro.recuento(id).iniciadas()).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM plan_generation_predictions WHERE operation_id = ?"
                + " AND status = 'NO_CONFIRMADA'", Integer.class, id)).as("A no estropeo el libro de B").isZero();
    }

    // ================================================================ limites (R01, R14, R18, R10)

    @Test
    @DisplayName("R14/R10 · vence de verdad con la prediccion en curso: FAILED, sin publicar ni pagar mas; el reintento"
            + " es OTRA operacion y pide sus propias predicciones (revision de identidad)")
    void venceDeVerdadDuranteUnaPrediccion() throws Exception {
        long u = participante();
        var id = service.request(u).operation().getId();
        BLOQUEO_GET = new CountDownLatch(1);
        assertThat(service.claimNext()).contains(id);
        var a = CompletableFuture.runAsync(() -> service.execute(id));
        esperarHasta(() -> GET_ESPERANDO.get() >= 1, "prediccion en curso");

        jdbc.update("UPDATE plan_generation_operations SET first_started_at = now() - interval '1801 seconds'"
                + " WHERE operation_id = ?", id);
        service.recoverStale();
        assertThat(estado(id)).isEqualTo("FAILED");
        assertThat(motivo(id)).isEqualTo("TIEMPO_EXCEDIDO");

        var bloqueo = BLOQUEO_GET;
        BLOQUEO_GET = null;
        bloqueo.countDown();
        a.get(120, TimeUnit.SECONDS);

        assertThat(POSTS.get()).as("tras vencer no se crea ninguna prediccion mas").isEqualTo(1);
        assertThat(estado(id)).as("el resultado tardio no la reabre").isEqualTo("FAILED");
        assertThat(planesDe(u)).isZero();
        assertThat(libro.recuento(id).completadas()).as("la salida tardia queda en el libro").isEqualTo(1);

        // R10: el usuario reintenta (FAILED reintentable). Antes de la revision de integracion (27/11/2026) la nueva
        // operacion ADOPTABA la prediccion de la vencida por usuario y huella. El contenido no prueba identidad: ahora
        // es otra operacion y pide las suyas. Se acepta el coste (lo vigila el limite por usuario de B5).
        var segunda = service.request(u).operation().getId();
        assertThat(service.claimNext()).contains(segunda);
        service.execute(segunda);
        assertThat(estado(segunda)).isEqualTo("COMPLETED");
        assertThat(planesDe(u)).isEqualTo(1);
        assertThat(POSTS.get()).as("la de la vencida no se hereda: 1 + 3 propias").isEqualTo(4);
        assertThat(libro.recuento(segunda).reutilizadas()).isZero();
    }

    @Test
    @DisplayName("R01/R18 · una generacion que lleva 1.000 s viva recibe tras una espera larga una salida VALIDA: se"
            + " publica como IA (en #71: FAILED a los 600 s y descartada)")
    void salidaValidaTrasUnaEsperaLarga() throws Exception {
        // La salida valida se construye con la rutina que el motor de reglas publica para un perfil identico.
        long modelo = participante();
        var opModelo = service.request(modelo).operation().getId();
        assertThat(service.claimNext()).contains(opModelo);
        service.execute(opModelo);
        assertThat(estado(opModelo)).isEqualTo("COMPLETED");
        String salida = salidaDesdePlan(modelo);
        POSTS.set(0);

        long u = participante();
        SALIDA = new ObjectMapper().writeValueAsString(salida);
        EN_CURSO_RESTANTES.set(8);                 // ~2 s de "processing" antes de terminar
        var id = service.request(u).operation().getId();
        assertThat(service.claimNext()).contains(id);
        jdbc.update("UPDATE plan_generation_operations SET first_started_at = now() - interval '1000 seconds',"
                + " started_at = now() - interval '1000 seconds' WHERE operation_id = ?", id);
        var a = CompletableFuture.runAsync(() -> service.execute(id));
        while (!a.isDone()) {                      // el barrido corre mientras tanto: no la da por vencida
            service.recoverStale();
            service.heartbeat(List.of(id));
            Thread.sleep(100);
        }
        a.get();

        assertThat(estado(id)).isEqualTo("COMPLETED");
        assertThat(planesDe(u)).isEqualTo(1);
        assertThat(origenDelPlan(u)).as("se aprovecho la salida de la IA").isEqualTo("AI");
        assertThat(POSTS.get()).isEqualTo(1);
        assertThat(libro.recuento(id).completadas()).isEqualTo(1);
    }

    /** La rutina publicada de {@code u}, en el formato de salida de la IA (19.2). */
    private String salidaDesdePlan(long u) throws Exception {
        long plan = jdbc.queryForObject("SELECT plan_id FROM weekly_training_plans WHERE user_id = ?", Long.class, u);
        var sesiones = new ArrayList<Map<String, Object>>();
        for (var w : jdbc.queryForList("SELECT planned_workout_id, scheduled_date, focus_code, workout_name,"
                + " expected_duration_minutes FROM planned_workouts WHERE plan_id = ? ORDER BY display_order", plan)) {
            var ejercicios = new ArrayList<Map<String, Object>>();
            for (var e : jdbc.queryForList("SELECT exercise_id, exercise_order, prescription_type, planned_sets,"
                    + " planned_reps, planned_duration_seconds, target_load_kg, rest_seconds, notes"
                    + " FROM planned_workout_exercises WHERE planned_workout_id = ? ORDER BY exercise_order",
                    w.get("planned_workout_id"))) {
                var m = new LinkedHashMap<String, Object>(e);
                ejercicios.add(m);
            }
            var s = new LinkedHashMap<String, Object>();
            s.put("scheduled_date", w.get("scheduled_date").toString());
            s.put("focus_code", w.get("focus_code"));
            s.put("workout_name", w.get("workout_name"));
            s.put("expected_duration_minutes", w.get("expected_duration_minutes"));
            s.put("exercises", ejercicios);
            sesiones.add(s);
        }
        var p = new LinkedHashMap<String, Object>();
        p.put("schema_version", PlanDraft.SCHEMA_VERSION);
        p.put("plan_name", "Plan simulado");
        p.put("total_volume", 0);
        p.put("rationale", "Salida simulada para la prueba de reanudacion (no es una respuesta real de la IA).");
        p.put("workouts", sesiones);
        return new ObjectMapper().writeValueAsString(p);
    }

    // ================================================================ incertidumbre y errores (R07, R11, R12)

    @Test
    @DisplayName("R07 · Replicate acepta pero el id no llega: NO_CONFIRMADA, no se reenvia, respaldo validado")
    void aceptadaSinIdNoSeReenvia() {
        long u = participante();
        var id = service.request(u).operation().getId();
        CORTAR_TRAS_ACEPTAR.set(true);
        morirAntesDePublicar.set(1);
        assertThat(service.claimNext()).contains(id);
        service.execute(id);

        int aceptadas = POSTS.get();
        var r = libro.recuento(id);
        // Un fallo del proveedor no se reintenta con la IA (R-1): una sola prediccion aceptada, NO_CONFIRMADA.
        assertThat(aceptadas).as("el POST aceptado sin id queda NO_CONFIRMADA y no se reenvia").isEqualTo(1);
        assertThat(r.noConfirmadas()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM plan_generation_predictions WHERE operation_id = ?"
                + " AND replicate_prediction_id IS NOT NULL", Integer.class, id)).isZero();

        // Reinicio: la reanudacion repite NO_CONFIRMADA sin otro POST, y publica el respaldo validado.
        reiniciar(id);
        service.execute(id);
        assertThat(POSTS.get()).as("ningun POST nuevo tras el reinicio").isEqualTo(aceptadas);
        assertThat(estado(id)).isEqualTo("COMPLETED");
        assertThat(planesDe(u)).isEqualTo(1);
        assertThat(libro.recuento(id).noConfirmadas()).isEqualTo(aceptadas);
    }

    @Test
    @DisplayName("R11 · error temporal del proveedor al sondear (503): se cancela, sin reintentos sin fin; respaldo validado")
    void errorTemporalAlSondear() {
        long u = participante();
        var id = service.request(u).operation().getId();
        GET_FALLA.set(true);
        assertThat(service.claimNext()).contains(id);
        service.execute(id);

        assertThat(POSTS.get()).isEqualTo(1);
        assertThat(estado(id)).isEqualTo("COMPLETED");
        assertThat(planesDe(u)).isEqualTo(1);
        assertThat(origenDelPlan(u)).isEqualTo("RULE_ENGINE");
        var r = libro.recuento(id);
        assertThat(r.canceladas() + r.vencidas()).as("queda registrada con su id para conciliar").isEqualTo(1);
    }

    @Test
    @DisplayName("R12 · error definitivo del proveedor (4xx): ERROR_ENVIO, ninguna prediccion creada; respaldo validado")
    void errorDefinitivoDelProveedor() {
        long u = participante();
        var id = service.request(u).operation().getId();
        RECHAZAR_POST.set(true);
        assertThat(service.claimNext()).contains(id);
        service.execute(id);

        assertThat(POSTS.get()).isZero();
        assertThat(RECHAZADOS.get()).isEqualTo(1);
        assertThat(estado(id)).isEqualTo("COMPLETED");
        assertThat(origenDelPlan(u)).isEqualTo("RULE_ENGINE");
        assertThat(libro.recuento(id).fallidas()).isEqualTo(1);
    }

    // ================================================================ camino normal y repeticiones (R09, R19)

    @Test
    @DisplayName("R19 · camino normal: 3 intentos una vez y publica; una ejecucion o un resultado repetidos despues no hacen nada")
    void caminoNormalYRepeticiones() {
        long u = participante();
        var id = service.request(u).operation().getId();
        assertThat(service.claimNext()).contains(id);
        service.execute(id);
        assertThat(POSTS.get()).isEqualTo(3);
        assertThat(estado(id)).isEqualTo("COMPLETED");
        assertThat(planesDe(u)).isEqualTo(1);
        assertThat(ejecuciones(id)).isEqualTo(1);

        // Otra ejecucion de la misma operacion (otro worker, otro sondeo): ya no esta PROCESSING, no hace nada.
        service.execute(id);
        otraInstancia().execute(id);
        assertThat(ejecucionesDelPipeline.get()).isEqualTo(1);
        // Un resultado que llega otra vez para una prediccion ya cerrada no cambia nada.
        long fila = jdbc.queryForObject("SELECT prediction_row_id FROM plan_generation_predictions"
                + " WHERE operation_id = ? AND attempt = 1", Long.class, id);
        libro.terminada(fila, DiarioDePredicciones.Estado.FAILED, null, "resultado repetido", Instant.now());
        assertThat(jdbc.queryForObject("SELECT status FROM plan_generation_predictions WHERE prediction_row_id = ?",
                String.class, fila)).isEqualTo("SUCCEEDED");
        assertThat(POSTS.get()).isEqualTo(3);
        assertThat(planesDe(u)).isEqualTo(1);
        assertThat(service.request(u).operation().getId()).as("un POST repetido devuelve la misma").isEqualTo(id);
    }

    @Test
    @DisplayName("E15 (revision de identidad) · reintento manual tras un fallo con la misma peticion: es OTRA operacion;"
            + " no adopta las salidas de la anterior y pide las suyas")
    void reintentoManualAdopta() {
        long u = participante();
        var primera = service.request(u).operation().getId();
        morirAntesDePublicar.set(1);
        assertThat(service.claimNext()).contains(primera);
        service.execute(primera);
        assertThat(POSTS.get()).isEqualTo(3);
        // La operacion se cierra sin plan (por ejemplo, el barrido por tiempo maximo) y el usuario reintenta.
        jdbc.update("UPDATE plan_generation_operations SET status = 'FAILED', reason_code = 'TIEMPO_EXCEDIDO',"
                + " finished_at = now(), lease_owner = NULL, lease_expires_at = NULL WHERE operation_id = ?", primera);

        var segunda = service.request(u).operation().getId();
        assertThat(segunda).isNotEqualTo(primera);
        assertThat(service.claimNext()).contains(segunda);
        service.execute(segunda);

        // Antes: "las 3 predicciones ya pagadas se adoptan" (0 POST). Ahora cada operacion paga las suyas.
        assertThat(POSTS.get()).as("3 de la primera + 3 propias de la segunda").isEqualTo(6);
        assertThat(estado(segunda)).isEqualTo("COMPLETED");
        assertThat(planesDe(u)).isEqualTo(1);
        assertThat(libro.recuento(segunda).reutilizadas()).isZero();
        assertThat(libro.recuento(segunda).iniciadas()).isEqualTo(3);
    }
}
