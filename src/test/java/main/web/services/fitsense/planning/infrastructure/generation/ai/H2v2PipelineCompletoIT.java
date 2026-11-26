package main.web.services.fitsense.planning.infrastructure.generation.ai;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import main.web.services.fitsense.planning.application.internal.commandservices.PlanGenerationPipeline;
import main.web.services.fitsense.planning.application.internal.generation.PlanGenerationOperationService;
import main.web.services.fitsense.planning.application.internal.outboundservices.acl.ExternalConfigurationService;
import main.web.services.fitsense.planning.domain.exceptions.AiProviderUnavailableException;
import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanGenerationContext;
import main.web.services.fitsense.planning.domain.services.PlanDraftNormalizer;
import main.web.services.fitsense.planning.domain.services.PlanDraftValidator;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.*;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import main.web.services.fitsense.shared.infrastructure.json.JsonSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;

/**
 * H2-v2 · ETAPA 2 · PIPELINE COMPLETO PAREADO, PR-1.7 (A) frente a PR-2.0 (B). OPT-IN
 * ({@code -Dfitsense.h2v2.e2.modo=SIMULADO|REAL}); CI lo omite. Diseno y criterios en
 * docs/FitSense/h2_v2/05_DISENO_EVALUACION_Y_PRESUPUESTO.md (opcion A: 13 casos, K3).
 * <p>
 * Por caso y brazo, un participante con el perfil del caso (el mismo que en la etapa 1) y el camino de produccion de
 * la primera generacion: {@code PlanGenerationOperationService.request -> claimNext -> execute} (lo que hace el
 * worker), con el pipeline real: comprobacion previa de factibilidad, hasta {@value PlanGenerationPipeline#AI_ATTEMPTS}
 * intentos de IA con reintento corregido, respaldo del motor de reglas y publicacion. Se registra:
 * <ul>
 *   <li>origen y modelo del plan publicado (un respaldo es {@code RULE_ENGINE} y NUNCA cuenta como exito de la IA);</li>
 *   <li>predicciones, problemas de cada intento (V15 de la mitad, V26 y el resto por codigo) y tiempo total;</li>
 *   <li>REVALIDACION de lo publicado: el plan tal como quedo en la base, convertido al formato de salida, vuelve a
 *   pasar el normalizador y el validador con el mismo contexto (V1-V27), y se comprueba que cada ejercicio publicado
 *   esta en {@code available_exercises} (catalogo, material y exclusiones O20 ya aplicadas).</li>
 * </ul>
 * PRESUPUESTO ({@link LibroPresupuestoH2v2}, comun con la etapa 1, USD 7,50 en total): antes de cada caso se comprueba
 * que cabe el peor caso (3 predicciones por brazo); antes de CADA prediccion se reserva su cota exacta (el mismo prompt
 * que se va a enviar) y, si no cabe, la prediccion no se crea. Con {@code SIMULADO}, Replicate es un servidor local:
 * las reservas van a un libro aparte en la carpeta de salida y no tocan el real.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=true",
        "fitsense.ai.api-token=${fitsense.h2v2.token:proxy}",
        "fitsense.ai.reasoning-effort=medium",
        "fitsense.ai.generate.max-per-hour=100",
        "fitsense.plan-generation.worker.enabled=false"
})
@DisplayName("H2-v2 etapa 2 · pipeline completo pareado PR-1.7 vs PR-2.0 (opt-in; SIMULADO sin coste o REAL con tope)")
class H2v2PipelineCompletoIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    static final String MODO = System.getProperty("fitsense.h2v2.e2.modo", "");
    /**
     * Version del brazo B (tratamiento). PR-2.0 por omision, la de la evaluacion de H2-v2 (su evidencia sigue siendo
     * reproducible); {@code -Dfitsense.h2v2.version-b=PR-2.1} para H2-v3. El brazo A es siempre PR-1.7.
     */
    static final String VERSION_B = PlanPromptBuilder.versionValida(System.getProperty("fitsense.h2v2.version-b",
            PlanPromptBuilder.VERSION_H2V2));
    static final boolean REAL = "REAL".equals(MODO), SIMULADO = "SIMULADO".equals(MODO);
    static final double TOPE_ETAPA = Double.parseDouble(System.getProperty("fitsense.h2v2.tope-usd", "0"));
    static final String ETAPA = "E2";
    /** Los 13 casos de la opcion A (los que fallaron antes: LOWER/UPPER y 5-6 dias), en el orden del diseno. */
    static final List<String> CASOS_E2 = List.of("G25", "G26", "G05", "G06", "G02", "G18", "G14", "G04", "G16", "G12",
            "G08", "G20", "G10");
    static final Path SALIDA = Path.of(System.getProperty("fitsense.h2v2.salida", "target/h2v2-etapa2"));
    /** Margen de bytes de un reintento sobre la primera peticion (propuesta rechazada + motivos + pistas). */
    static final long MARGEN_REINTENTO_BYTES = 60_000;

    // ------------------------------------------------------------------ Replicate simulado (solo SIMULADO)
    static final HttpServer SIM;
    static final AtomicInteger POSTS = new AtomicInteger();
    static final List<String> CUERPOS = new CopyOnWriteArrayList<>();
    /** SIMULADO: que devuelve el servidor (la salida, ya como valor JSON de "output"). */
    static volatile String SALIDA_SIM = "\"{}\"";

    static {
        HttpServer s = null;
        if (SIMULADO) {
            try {
                s = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            s.createContext("/v1/models/", x -> {
                CUERPOS.add(new String(x.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                responder(x, 201, prediccion("sim" + POSTS.incrementAndGet(), "starting", null));
            });
            s.createContext("/v1/predictions/", x -> responder(x, 200, prediccion(
                    x.getRequestURI().getPath().substring("/v1/predictions/".length()), "succeeded", SALIDA_SIM)));
            s.start();
        }
        SIM = s;
    }

    static String base() {
        return "http://127.0.0.1:" + SIM.getAddress().getPort();
    }

    static String prediccion(String id, String estado, String salida) {
        return "{\"id\":\"" + id + "\",\"status\":\"" + estado + "\",\"output\":" + (salida == null ? "null" : salida)
                + ",\"error\":null,\"metrics\":{\"predict_time\":1.0},\"urls\":{\"get\":\"" + base()
                + "/v1/predictions/" + id + "\",\"cancel\":\"" + base() + "/v1/predictions/" + id + "/cancel\"}}";
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
    static void replicate(DynamicPropertyRegistry r) {
        if (SIMULADO) {
            r.add("fitsense.ai.base-url", () -> base() + "/v1/models/openai/gpt-5-mini/predictions");
            r.add("fitsense.ai.poll-interval-millis", () -> "100");
        }
    }

    @AfterAll
    static void parar() {
        if (SIM != null) SIM.stop(0);
    }

    // ------------------------------------------------------------------ backend
    @MockitoSpyBean ReplicateTrainingPlanGenerator generador;
    @Autowired ReplicateProperties propiedades;
    @Autowired PlanPromptBuilder promptBuilder;
    @Autowired JsonSupport json;
    @Autowired PlanGenerationOperationService operaciones;
    @Autowired WeeklyTrainingPlanCommandService planComandos;
    @Autowired PlanDraftNormalizer normalizador;
    @Autowired PlanDraftValidator validador;
    @Autowired ExternalConfigurationService configuracion;
    @Autowired UserProfileCommandService perfiles;
    @Autowired JdbcTemplate jdbc;
    @Autowired main.web.services.fitsense.planning.interfaces.acl.PlanningContextFacade planning;

    static final ObjectMapper JSON = new ObjectMapper();
    final List<String> lineas = new CopyOnWriteArrayList<>();
    /** Problemas de cada intento rechazado, tal como los registra el pipeline (por usuario). */
    final Map<Long, List<List<String>>> rechazos = new java.util.concurrent.ConcurrentHashMap<>();
    LibroPresupuestoH2v2 libro;
    volatile String casoActual = "-", brazoActual = "-";
    volatile boolean cortadoEnCaso;

    record Brazo(String caso, String brazo, int orden, long usuario, LocalDate semana, String estadoOperacion,
                 String origen, String modelo, int intentosPlan, int predicciones, long ms, List<List<String>> problemas,
                 boolean primeraValida, String motivoRespaldo, String revalidacion, String fueraDeCatalogo,
                 List<String> predictionIds, String detalle) {}

    @Test
    void pipelineCompletoPareado() throws Exception {
        assumeTrue(REAL || SIMULADO, "opt-in: -Dfitsense.h2v2.e2.modo=SIMULADO|REAL");
        assertEquals(ReplicateProperties.Formato.DIRECT, propiedades.formato());
        assertEquals("medium", propiedades.reasoningEffort());
        assertEquals("low", propiedades.verbosity());
        assertEquals(16_000, propiedades.maxCompletionTokens());
        assertEquals("PR-1.7", promptBuilder.version(), "PR-2.0 sigue apagado por omision");
        if (REAL) {
            assertEquals("SI", System.getProperty("fitsense.h2v2.autorizado"), "REAL exige autorizado=SI");
            assertTrue(TOPE_ETAPA > 0, "REAL exige tope-usd");
        }
        Files.createDirectories(SALIDA.resolve("salidas"));
        double topeTotal = Double.parseDouble(System.getProperty("fitsense.h2v2.tope-total-usd",
                String.valueOf(LibroPresupuestoH2v2.TOPE_TOTAL_USD)));
        libro = new LibroPresupuestoH2v2(REAL
                ? Path.of(System.getProperty("fitsense.h2v2.libro", "docs/FitSense/h2_v2/evaluacion_real/libro_presupuesto.csv"))
                : SALIDA.resolve("libro_simulado.csv"), REAL ? topeTotal : LibroPresupuestoH2v2.TOPE_TOTAL_USD);
        if (REAL) assertTrue(TOPE_ETAPA <= libro.topeTotal(), "el tope de la etapa no supera el total del libro");
        double tope = REAL || TOPE_ETAPA > 0 ? TOPE_ETAPA : 100.0;
        escuchar();
        interceptar(tope);
        System.out.println("H2V2E2_LIBRO reservado_total=" + libro.reservado(null) + " reservado_E2=" + libro.reservado(ETAPA)
                + " saldo=" + libro.saldo() + " tope_etapa=" + tope);

        var todos = H2v2PrimeraPropuestaIT.casos();
        var ids = System.getProperty("fitsense.h2v2.casos", "").isBlank() ? CASOS_E2
                : List.of(System.getProperty("fitsense.h2v2.casos").split(","));
        var filas = new ArrayList<Brazo>();
        String corte = null;
        for (int i = 0; i < ids.size(); i++) {
            String id = ids.get(i);
            var c = todos.stream().filter(x -> x.id().equals(id)).findFirst().orElseThrow();
            var orden = i % 2 == 0 ? List.of("A", "B") : List.of("B", "A");
            var usuarios = new LinkedHashMap<String, Long>();
            var contextos = new LinkedHashMap<String, PlanGenerationContext>();
            double peor = 0;
            for (var b : orden) {
                long u = participante(c, b);
                usuarios.put(b, u);
                fijarInterruptor(b.equals("B"));
                var semana = planComandos.firstPlanWeekStart(u);
                var ctx = planComandos.prepare(GenerateWeeklyPlanCommand.firstPlan(u, semana)).context();
                contextos.put(b, ctx);
                long bytes = bytes(ctx, AiRetryFeedback.none());
                peor += LibroPresupuestoH2v2.cota(bytes, propiedades.maxCompletionTokens())
                        + 2 * LibroPresupuestoH2v2.cota(bytes + MARGEN_REINTENTO_BYTES, propiedades.maxCompletionTokens());
            }
            if (corte == null && !libro.cabe(ETAPA, tope, peor))
                corte = "no cabe el peor caso de " + c.id() + " (" + "%.4f".formatted(peor) + "): reservado E2 "
                        + "%.4f".formatted(libro.reservado(ETAPA)) + " de " + tope + ", total "
                        + "%.4f".formatted(libro.reservado(null)) + " de " + libro.topeTotal();
            if (corte != null) {
                for (var b : orden)
                    filas.add(new Brazo(c.id(), b, 0, usuarios.get(b), null, "NO_EJECUTADO_PRESUPUESTO", "-", "-", 0, 0,
                            0, List.of(), false, "-", "-", "-", List.of(), corte));
                continue;
            }
            int k = 0;
            for (var b : orden) {
                if (SIMULADO) SALIDA_SIM = salidaSimulada(c.id(), b, contextos.get(b));
                filas.add(generar(c, b, ++k, usuarios.get(b), contextos.get(b)));
                anotarFila(filas.get(filas.size() - 1));
            }
            var semanas = filas.subList(filas.size() - 2, filas.size()).stream().map(Brazo::semana).distinct().count();
            assertEquals(1, semanas, "los dos brazos de " + c.id() + " generan la misma semana");
            if (cortadoEnCaso) {
                corte = "presupuesto agotado durante " + c.id();
                break;
            }
        }
        escribir(filas, corte);
        if (SIMULADO && corte == null) comprobarSimulado(filas);
        assertTrue(libro.reservado(ETAPA) <= tope + 1e-9, "nunca se reserva por encima del tope");
    }

    /** Antes de CADA prediccion: reserva de su cota exacta; si no cabe, no se crea (el pipeline sigue sin IA). */
    private void interceptar(double tope) {
        doAnswer(inv -> {
            PlanGenerationContext ctx = inv.getArgument(0);
            AiRetryFeedback fb = inv.getArgument(1);
            int intento = inv.getArgument(2);
            long bytes = bytes(ctx, fb);
            double cota = LibroPresupuestoH2v2.cota(bytes, propiedades.maxCompletionTokens());
            if (!libro.reservar(ETAPA, tope, casoActual, brazoActual, intento, cota, promptBuilder.version()
                    + " bytes=" + bytes)) {
                cortadoEnCaso = true;
                throw new AiProviderUnavailableException("presupuesto de la evaluacion agotado: no se crea la prediccion");
            }
            int antes = lineas.size();
            try {
                String out = (String) inv.callRealMethod();
                libro.resultado(ETAPA, casoActual, brazoActual, intento, pid(antes), "RESPUESTA");
                comprobarEnviado(antes, bytes);
                return out;
            } catch (RuntimeException e) {
                libro.resultado(ETAPA, casoActual, brazoActual, intento, pid(antes), "ERROR " + e.getClass().getSimpleName());
                throw e;
            }
        }).when(generador).requestOutput(any(), any(), anyInt());
    }

    /** La peticion enviada es la que se reservo (mismos bytes que REPLICATE_CONSUMO). */
    private volatile long caracteresReservados;

    private void comprobarEnviado(int antes, long bytes) {
        var consumo = lineas.subList(antes, lineas.size()).stream().filter(x -> x.startsWith("REPLICATE_CONSUMO"))
                .reduce((a, b) -> b).orElse("");
        assertTrue(consumo.contains("caracteres_entrada=" + caracteresReservados), "lo reservado es lo enviado: " + consumo);
    }

    /** Los bytes que enviara requestOutput: el mismo prompt y la misma instruccion de sistema. */
    private long bytes(PlanGenerationContext ctx, AiRetryFeedback fb) {
        var snapshot = json.write(PlanInputSnapshot.of(ctx, promptBuilder.version()));
        var prompt = promptBuilder.build(ctx, snapshot, fb);
        var sistema = ReplicateTrainingPlanGenerator.instruccionDeSistema(
                ReplicateTrainingPlanGenerator.Peticion.deSemana(ctx.effectiveDaysPerWeek(), promptBuilder.version()));
        caracteresReservados = prompt.length() + sistema.length();
        return prompt.getBytes(StandardCharsets.UTF_8).length + sistema.getBytes(StandardCharsets.UTF_8).length;
    }

    private Brazo generar(H2v2PrimeraPropuestaIT.Caso c, String b, int orden, long u, PlanGenerationContext ctx) {
        fijarInterruptor(b.equals("B"));
        casoActual = c.id();
        brazoActual = b;
        int antes = lineas.size();
        int cuerposAntes = CUERPOS.size();
        long t0 = System.nanoTime();
        String estado, detalle = "";
        try {
            var id = operaciones.request(u).operation().getId();
            assertEquals(Optional.of(id), operaciones.claimNext());
            operaciones.execute(id);
            estado = jdbc.queryForObject("SELECT status FROM plan_generation_operations WHERE operation_id = ?",
                    String.class, id);
            detalle = Objects.toString(jdbc.queryForObject("SELECT coalesce(reason_code, '') FROM plan_generation_operations"
                    + " WHERE operation_id = ?", String.class, id), "");
        } catch (RuntimeException e) {
            estado = "EXCEPCION";
            detalle = e.getClass().getSimpleName() + ": " + e.getMessage();
        }
        long ms = (System.nanoTime() - t0) / 1_000_000;
        var nuevas = List.copyOf(lineas.subList(antes, lineas.size()));
        var ids = nuevas.stream().filter(x -> x.startsWith("REPLICATE_TIMING")).map(H2v2PipelineCompletoIT::pidDe).toList();
        String motivo = nuevas.stream().filter(x -> x.startsWith("GENERACION_TIEMPOS")).map(x -> campo(x, "motivo_respaldo"))
                .reduce((x, y) -> y).orElse("-");
        var problemas = rechazos.getOrDefault(u, List.of());
        if (SIMULADO) {
            var enviados = CUERPOS.subList(cuerposAntes, CUERPOS.size());
            assertEquals(ids.size(), enviados.size(), "una prediccion creada por cada REPLICATE_TIMING");
            String v = b.equals("B") ? VERSION_B : PlanPromptBuilder.VERSION;
            for (var x : enviados)
                assertTrue(x.contains("\\\"prompt_version\\\":\\\"" + v + "\\\""), "el brazo " + b + " envia " + v);
        }
        var plan = jdbc.queryForList("SELECT plan_id, generation_source, model_name, generation_attempts, week_start_date"
                + " FROM weekly_training_plans WHERE user_id = ?", u);
        if (plan.isEmpty())
            return new Brazo(c.id(), b, orden, u, null, estado, "SIN_PLAN", "-", 0, ids.size(), ms, problemas,
                    false, motivo, "-", "-", ids, detalle);
        var p = plan.get(0);
        long planId = ((Number) p.get("plan_id")).longValue();
        String origen = (String) p.get("generation_source");
        boolean primeraValida = "AI".equals(origen) && problemas.isEmpty() && ids.size() == 1;
        guardar(c.id() + "_" + b + "_problemas.txt", problemas.stream().map(l -> String.join("\n", l))
                .collect(Collectors.joining("\n--- intento siguiente ---\n")));
        String publicada = salidaDesdePlan(planId);
        guardar(c.id() + "_" + b + "_publicado.json", publicada);
        return new Brazo(c.id(), b, orden, u, ((java.sql.Date) p.get("week_start_date")).toLocalDate(), estado, origen,
                Objects.toString(p.get("model_name"), "-"), ((Number) p.get("generation_attempts")).intValue(), ids.size(),
                ms, problemas, primeraValida, motivo, revalidar(publicada, ctx),
                fueraDeCatalogo(planId, ctx), ids, detalle);
    }

    /** Lo publicado, otra vez por el normalizador y el validador, con el mismo contexto. */
    private String revalidar(String publicada, PlanGenerationContext ctx) {
        try {
            var borrador = normalizador.normalize(generador.toDraft(publicada), ctx).draft();
            validador.validate(borrador, ctx, configuracion.durationToRepsDivisor());
            return "VALIDA";
        } catch (InvalidPlanDraftException e) {
            return "INVALIDA " + e.problems();
        }
    }

    /** Ejercicios publicados que no estan en available_exercises (catalogo, material y exclusiones O20 ya aplicadas). */
    private String fueraDeCatalogo(long planId, PlanGenerationContext ctx) {
        var ofrecidos = ctx.availableExercises().stream().map(CandidateExercise::exerciseId).collect(Collectors.toSet());
        var fuera = jdbc.queryForList("SELECT pwe.exercise_id FROM planned_workout_exercises pwe JOIN planned_workouts pw"
                + " ON pw.planned_workout_id = pwe.planned_workout_id WHERE pw.plan_id = ?", Long.class, planId)
                .stream().filter(x -> !ofrecidos.contains(x)).toList();
        return fuera.isEmpty() ? "0" : fuera.toString();
    }

    // ------------------------------------------------------------------ simulado

    /**
     * SIMULADO: casos pares, la rutina del motor de reglas de un perfil identico (valida por construccion) en el brazo
     * A y una salida invalida en el B; casos impares al reves. Asi se ejercitan los dos prompts en los dos caminos
     * (publicacion de la IA en 1 prediccion; 3 rechazos y respaldo).
     */
    private String salidaSimulada(String caso, String b, PlanGenerationContext ctx) throws Exception {
        boolean valida = (CASOS_E2.indexOf(caso) % 2 == 0) == b.equals("A");
        if (!valida) return "\"{}\"";
        long u = participante(H2v2PrimeraPropuestaIT.casos().stream().filter(x -> x.id().equals(caso)).findFirst()
                .orElseThrow(), "MODELO" + b);
        // La rutina modelo: con la IA respondiendo algo invalido, el pipeline publica la del motor de reglas.
        SALIDA_SIM = "\"{}\"";
        casoActual = caso;
        brazoActual = "MODELO";
        planning.generateWeeklyPlan(u, ctx.weekStartDate(),
                main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment.none(), null, null);
        long plan = jdbc.queryForObject("SELECT plan_id FROM weekly_training_plans WHERE user_id = ?", Long.class, u);
        return JSON.writeValueAsString(salidaDesdePlan(plan));
    }

    private void comprobarSimulado(List<Brazo> filas) {
        for (var f : filas) {
            boolean valida = (CASOS_E2.indexOf(f.caso()) % 2 == 0) == f.brazo().equals("A");
            if (valida) {
                assertEquals("AI", f.origen(), f.toString());
                assertEquals("gpt-5-mini", f.modelo(), f.toString());
                assertEquals(1, f.predicciones(), f.toString());
                assertTrue(f.primeraValida(), f.toString());
            } else {
                assertEquals("RULE_ENGINE", f.origen(), "un respaldo no consta como IA: " + f);
                assertEquals("-", f.modelo(), f.toString());
                assertEquals(PlanGenerationPipeline.AI_ATTEMPTS, f.predicciones(), f.toString());
                assertEquals(PlanGenerationPipeline.AI_ATTEMPTS, f.problemas().size(), f.toString());
                assertFalse(f.primeraValida());
            }
            assertEquals("VALIDA", f.revalidacion(), f.toString());
            assertEquals("0", f.fueraDeCatalogo(), f.toString());
        }
        // Una reserva por prediccion creada (incluidas las de las rutinas modelo).
        assertEquals(POSTS.get(), lineasLibro("RESERVA"), "una reserva por prediccion");
        assertEquals(POSTS.get(), lineasLibro("RESULTADO"), "un resultado por reserva");
    }

    private long lineasLibro(String tipo) {
        try {
            return Files.readAllLines(SALIDA.resolve("libro_simulado.csv")).stream()
                    .filter(l -> l.split(";").length > 2 && l.split(";")[1].equals(tipo) && l.split(";")[2].equals(ETAPA))
                    .count();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    // ------------------------------------------------------------------ utilidades

    private void fijarInterruptor(boolean h2v2) {
        String v = h2v2 ? VERSION_B : PlanPromptBuilder.VERSION;
        ReflectionTestUtils.invokeMethod((Object) AopTestUtils.getUltimateTargetObject(promptBuilder), "setPromptVersion", v);
        ReflectionTestUtils.invokeMethod((Object) AopTestUtils.getUltimateTargetObject(planComandos),
                "setPromptVersionGeneracion", v);
        assertEquals(h2v2, promptBuilder.h2v2());
    }

    /** El mismo perfil que en la etapa 1 (H2v2PrimeraPropuestaIT.participante); un usuario por caso y brazo. */
    private long participante(H2v2PrimeraPropuestaIT.Caso c, String b) {
        String email = ("h2v2e2-" + MODO + "-" + c.id() + "-" + b + "@pruebas.invalid").toLowerCase();
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at,"
                + " is_study_participant, timezone) VALUES (?, 'x', 'Sintetico', now(), now(), true, 'America/Lima')", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        boolean gym = c.lugar() == TrainingLocation.GYM;
        boolean r = c.restringido();
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1990, 3, 10), BiologicalSex.FEMALE,
                new BigDecimal("165"), new BigDecimal("72"), null, c.nivel(), c.objetivo(), "evaluacion pareada H2",
                c.lugar(), (short) c.dias().size(), c.dias(), c.minutos(),
                gym ? H2v2PrimeraPropuestaIT.GIMNASIO : H2v2PrimeraPropuestaIT.CASA, List.of(),
                r ? "Restricciones declaradas" : "Ninguna", !r, true, !r, !r, !r));
        return u;
    }

    String salidaDesdePlan(long plan) {
        var sesiones = new ArrayList<Map<String, Object>>();
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
            sesiones.add(s);
        }
        var p = new LinkedHashMap<String, Object>();
        p.put("schema_version", PlanDraft.SCHEMA_VERSION);
        p.put("plan_name", "Plan publicado");
        p.put("total_volume", 0);
        p.put("rationale", "Plan publicado, convertido para revalidarlo.");
        p.put("workouts", sesiones);
        try {
            return JSON.writeValueAsString(p);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void escuchar() {
        var a = new AppenderBase<ILoggingEvent>() {
            @Override
            protected void append(ILoggingEvent e) {
                var t = e.getFormattedMessage();
                if (t == null) return;
                if (t.startsWith("REPLICATE_TIMING") || t.startsWith("REPLICATE_CONSUMO") || t.startsWith("GENERACION_TIEMPOS"))
                    lineas.add(t);
                if (e.getMessage() != null && e.getMessage().startsWith("Intento {} de IA rechazado")) {
                    var arg = e.getArgumentArray();
                    long u = ((Number) arg[1]).longValue();
                    @SuppressWarnings("unchecked") var probs = (List<String>) arg[2];
                    rechazos.computeIfAbsent(u, k -> new CopyOnWriteArrayList<>()).add(List.copyOf(probs));
                }
            }
        };
        a.start();
        ((Logger) LoggerFactory.getLogger(ReplicateTrainingPlanGenerator.class)).addAppender(a);
        ((Logger) LoggerFactory.getLogger(PlanGenerationPipeline.class)).addAppender(a);
    }

    private String pid(int desde) {
        for (var x : lineas.subList(desde, lineas.size()))
            if (x.startsWith("REPLICATE_TIMING")) return pidDe(x);
        return "desconocido";
    }

    static String pidDe(String linea) {
        var m = Pattern.compile("prediction_id=(\\S+)").matcher(linea);
        return m.find() ? m.group(1) : "desconocido";
    }

    static String campo(String linea, String clave) {
        var m = Pattern.compile(clave + "=(\\S+)").matcher(linea);
        return m.find() ? m.group(1) : "-";
    }

    private void guardar(String nombre, String contenido) {
        try {
            Files.writeString(SALIDA.resolve("salidas").resolve(nombre), contenido == null ? "" : contenido);
        } catch (Exception ignored) {
        }
    }

    static final String CABECERA = "modo;caso;brazo;orden;prompt_version;semana;estado_operacion;origen;modelo;"
            + "intentos_plan;predicciones;ms_total;primera_valida;rechazos_v15_mitad;rechazos_v26;otros_por_intento;"
            + "motivo_respaldo;revalidacion_publicado;fuera_de_available;prediction_ids;detalle";
    private static final Pattern MITAD = Pattern.compile("V15: el entrenamiento del \\S+ dedica \\d+ de \\d+ ejercicios");
    private static final Pattern CODIGO = Pattern.compile("^(V\\d+[A-Z]?)\\b");

    private void anotarFila(Brazo f) throws IOException {
        var csv = SALIDA.resolve("pipeline_completo.csv");
        if (!Files.exists(csv)) Files.writeString(csv, CABECERA + "\n");
        Files.writeString(csv, linea(f) + "\n", StandardOpenOption.APPEND);
    }

    private String linea(Brazo f) {
        int mitad = (int) f.problemas().stream().flatMap(List::stream).filter(x -> MITAD.matcher(x).find()).count();
        int v26 = (int) f.problemas().stream().flatMap(List::stream).filter(x -> x.startsWith("V26")).count();
        var otros = f.problemas().stream().map(l -> l.stream().map(x -> {
            var m = CODIGO.matcher(x.strip());
            return m.find() ? m.group(1) : "CONTRATO/OTRO";
        }).collect(Collectors.groupingBy(x -> x, TreeMap::new, Collectors.counting())).toString()).toList();
        return String.join(";", MODO, f.caso(), f.brazo(), String.valueOf(f.orden()),
                f.brazo().equals("B") ? VERSION_B : PlanPromptBuilder.VERSION,
                String.valueOf(f.semana()), f.estadoOperacion(), f.origen(), f.modelo(), String.valueOf(f.intentosPlan()),
                String.valueOf(f.predicciones()), String.valueOf(f.ms()), String.valueOf(f.primeraValida()),
                String.valueOf(mitad), String.valueOf(v26), otros.toString().replace(';', ','), f.motivoRespaldo(),
                f.revalidacion().replace(';', ','), f.fueraDeCatalogo(),
                String.join(" ", f.predictionIds()), f.detalle().replace(';', ',').replace('\n', ' '));
    }

    private void escribir(List<Brazo> filas, String corte) throws IOException {
        var sb = new StringBuilder(CABECERA + "\n");
        filas.forEach(f -> sb.append(linea(f)).append('\n'));
        Files.writeString(SALIDA.resolve("pipeline_completo_final.csv"), sb.toString());
        Files.write(SALIDA.resolve("consumo_replicate.log"), List.copyOf(lineas));
        Files.writeString(SALIDA.resolve("99_resumen.txt"), "modo=" + MODO + "\npredicciones="
                + filas.stream().mapToInt(Brazo::predicciones).sum() + "\nreservado_E2_usd="
                + String.format(Locale.ROOT, "%.4f", libro.reservado(ETAPA)) + " (COTA superior por prediccion)"
                + "\nreservado_total_usd=" + String.format(Locale.ROOT, "%.4f", libro.reservado(null))
                + "\ncorte=" + (corte == null ? "ninguno" : corte) + "\nmodelo=" + propiedades.modeloEfectivo()
                + " formato=" + propiedades.formato().etiqueta() + " reasoning_effort=" + propiedades.reasoningEffort()
                + " verbosity=" + propiedades.verbosity() + " max_completion_tokens=" + propiedades.maxCompletionTokens() + "\n");
    }
}
