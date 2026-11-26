package main.web.services.fitsense.planning.infrastructure.generation.ai;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import main.web.services.fitsense.planning.application.internal.outboundservices.acl.ExternalConfigurationService;
import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanGenerationContext;
import main.web.services.fitsense.planning.domain.services.FactibilidadGeneracionIA;
import main.web.services.fitsense.planning.domain.services.PlanDraftNormalizer;
import main.web.services.fitsense.planning.domain.services.PlanDraftValidator;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.*;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import main.web.services.fitsense.shared.infrastructure.json.JsonSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * H2-v2 (PR-2.0) con gpt-5-mini directo · COMPARACION PAREADA DE LA PRIMERA PROPUESTA. OPT-IN. Diseno y presupuesto en
 * docs/FitSense/h2_v2/03_DISENO_EVALUACION_Y_PRESUPUESTO.md.
 * <p>
 * Es el arnes de la evaluacion de H2 (docs/FitSense/cierre_gpt5_mini_h2/evidencia/H2PrimeraPropuestaIT.java.txt) con
 * los MISMOS casos, perfiles, catalogo, politica, modelo y validadores; solo cambia el tratamiento: A = PR-1.7 (el
 * vigente) y B = PR-2.0 ({@code fitsense.ai.generacion.prompt-version}), que cambia el prompt y la instantanea
 * exactamente como en produccion. Orden alterno (impar A-B, par B-A). Ademas de V15 cuenta V26 y el resto por codigo.
 * <p>
 * Cada propuesta pasa por el MISMO camino que en el pipeline (sin publicar): ensamblador, PlanDraftNormalizer y
 * PlanDraftValidator.validate. Antes de llamar se aplica la comprobacion previa de factibilidad del pipeline.
 * <p>
 * Modos ({@code -Dfitsense.h2v2.modo}):
 * <ul>
 *   <li>sin la propiedad: se omite (CI);</li>
 *   <li>{@code PROMPTS}: SIN Replicate. Construye las dos peticiones de cada caso y calcula su tamano y su cota de coste
 *   (la misma formula que REPLICATE_CONSUMO). Sirve para elegir los casos y fijar el presupuesto antes de gastar;</li>
 *   <li>{@code REAL}: Replicate real. Exige {@code autorizado=SI} y {@code tope-usd}. ANTES de cada pareja se calcula la
 *   cota EXACTA de sus dos peticiones; si no caben las dos, no se empieza (parejas completas) y se detiene.</li>
 * </ul>
 * No modifica perfiles, reglas, validadores, el oraculo ni la evaluacion congelada de la PR #69.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=true",
        "fitsense.ai.api-token=${fitsense.h2v2.token:proxy}",
        "fitsense.ai.reasoning-effort=medium",
        "fitsense.ai.prefer-wait-seconds=${fitsense.h2v2.espera:25}",
        "fitsense.ai.generate.max-per-hour=100"
})
@DisplayName("H2-v2 con gpt-5-mini · primera propuesta pareada PR-1.7 vs PR-2.0 (opt-in; PROMPTS sin coste, VALIDAR o REAL con tope)")
class H2v2PrimeraPropuestaIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired ReplicateTrainingPlanGenerator generador;
    @Autowired ReplicateProperties propiedades;
    @Autowired PlanPromptBuilder promptBuilder;
    @Autowired JsonSupport json;
    @Autowired WeeklyTrainingPlanCommandService planComandos;
    @Autowired PlanDraftNormalizer normalizador;
    @Autowired PlanDraftValidator validador;
    @Autowired ExternalConfigurationService configuracion;
    @Autowired UserProfileCommandService perfiles;
    @Autowired JdbcTemplate jdbc;

    static final String MODO = System.getProperty("fitsense.h2v2.modo", "");
    /**
     * Opcion C de H2-v4: replicas por perfil (0 = modo historico, una pareja por perfil y nombres sin replica). Con 1 o 2,
     * el plan, las claves y el registro de {@link PlanEjecucionH2}.
     */
    static final int REPLICAS = Integer.getInteger("fitsense.h2v2.replicas", 0);
    /** Solo esa replica (0 = todas): parte la corrida sin cambiar el orden fijado. */
    static final int SOLO_REPLICA = Integer.getInteger("fitsense.h2v2.solo-replica", 0);
    /** SIMULADO: Replicate local (sin coste), para probar plan, reanudacion, reservas y corte de punta a punta. */
    static final boolean SIMULADO = "SIMULADO".equals(MODO);
    /** SIMULADO: tras N predicciones de esta corrida se interrumpe (para probar la reanudacion). 0 = no. */
    static final int DETENER_TRAS = Integer.getInteger("fitsense.h2v2.detener-tras", 0);
    static final java.util.concurrent.atomic.AtomicInteger POSTS_SIM = new java.util.concurrent.atomic.AtomicInteger();
    static final com.sun.net.httpserver.HttpServer SIM;

    static {
        com.sun.net.httpserver.HttpServer s = null;
        if (SIMULADO) {
            try {
                s = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
            } catch (java.io.IOException e) {
                throw new IllegalStateException(e);
            }
            s.createContext("/v1/models/", x -> {
                x.getRequestBody().readAllBytes();
                responderSim(x, 201, prediccionSim("sim" + POSTS_SIM.incrementAndGet(), "starting"));
            });
            s.createContext("/v1/predictions/", x -> responderSim(x, 200, prediccionSim(
                    x.getRequestURI().getPath().substring("/v1/predictions/".length()), "succeeded")));
            s.start();
        }
        SIM = s;
    }

    static String baseSim() {
        return "http://127.0.0.1:" + SIM.getAddress().getPort();
    }

    static String prediccionSim(String id, String estado) {
        return "{\"id\":\"" + id + "\",\"status\":\"" + estado + "\",\"output\":" + (estado.equals("succeeded")
                ? "\"{}\"" : "null") + ",\"error\":null,\"urls\":{\"get\":\"" + baseSim() + "/v1/predictions/" + id
                + "\",\"cancel\":\"" + baseSim() + "/v1/predictions/" + id + "/cancel\"}}";
    }

    static void responderSim(com.sun.net.httpserver.HttpExchange x, int codigo, String cuerpo) throws java.io.IOException {
        byte[] b = cuerpo.getBytes(StandardCharsets.UTF_8);
        x.getResponseHeaders().add("Content-Type", "application/json");
        x.sendResponseHeaders(codigo, b.length);
        try (var o = x.getResponseBody()) {
            o.write(b);
        }
    }

    @org.springframework.test.context.DynamicPropertySource
    static void replicateSimulado(org.springframework.test.context.DynamicPropertyRegistry r) {
        if (SIMULADO) {
            r.add("fitsense.ai.base-url", () -> baseSim() + "/v1/models/openai/gpt-5-mini/predictions");
            r.add("fitsense.ai.poll-interval-millis", () -> "50");
        }
    }

    @org.junit.jupiter.api.AfterAll
    static void pararSim() {
        if (SIM != null) SIM.stop(0);
    }
    /**
     * Version del brazo B (tratamiento). PR-2.0 por omision, la de la evaluacion de H2-v2 (su evidencia sigue siendo
     * reproducible); {@code -Dfitsense.h2v2.version-b=PR-2.1} para H2-v3. El brazo A es siempre PR-1.7.
     */
    static final String VERSION_B = PlanPromptBuilder.versionValida(System.getProperty("fitsense.h2v2.version-b",
            PlanPromptBuilder.VERSION_H2V2));
    /**
     * VALIDAR (sin Replicate, sin coste): vuelve a pasar por el ensamblador, el normalizador y el validador las salidas
     * REALES ya obtenidas ({@code <caso>_<brazo>.json} en {@code salidas-dir}), con el mismo perfil y el mismo
     * contexto. Sirve para no repetir predicciones ya pagadas cuando la corrida REAL se interrumpe (reinicio del
     * contenedor del 23/11/2026) o cuando una salida se recupero por su id de prediccion (GET, sin coste).
     * {@code registro} (opcional): fichero {@code caso;brazo;prediction_id;ms;origen} con lo medido en la corrida REAL.
     */
    static final boolean VALIDAR = "VALIDAR".equals(MODO);
    static final Path SALIDAS_DIR = Path.of(System.getProperty("fitsense.h2v2.salidas-dir", "."));
    static final String REGISTRO = System.getProperty("fitsense.h2v2.registro", "");
    static final boolean REAL = "REAL".equals(MODO);
    static final double TOPE_USD = Double.parseDouble(System.getProperty("fitsense.h2v2.tope-usd", "0"));
    static final double USD_ENTRADA = 0.25, USD_SALIDA = 2.00;
    static final String CASOS = System.getProperty("fitsense.h2v2.casos", "");
    static final Path SALIDA = Path.of(System.getProperty("fitsense.h2v2.salida", "target/h2v2-primera"));
    static final LocalDate SEMANA = LocalDate.of(2026, 11, 23);
    /** Libro comun a las dos etapas (opcion A, USD 7,50 en total): el limite no se reinicia entre ejecuciones. */
    static final String ETAPA = "E1";
    /** Tope total del libro (7,50 el de H2-v2; el de H2-v3 se abrio con 7,60). Lo fija el libro al crearse. */
    static final double TOPE_TOTAL = Double.parseDouble(System.getProperty("fitsense.h2v2.tope-total-usd",
            String.valueOf(LibroPresupuestoH2v2.TOPE_TOTAL_USD)));
    /** El libro real: solo en modo REAL (SIMULADO usa uno aparte; PROMPTS y VALIDAR no reservan). */
    final LibroPresupuestoH2v2 libro = !REAL ? null : new LibroPresupuestoH2v2(Path.of(System.getProperty(
            "fitsense.h2v2.libro", "docs/FitSense/h2_v2/evaluacion_real/libro_presupuesto.csv")), TOPE_TOTAL);

    /** Una linea por prediccion recibida, al momento (para reanudar sin repetir ni pagar dos veces). */
    private void anotarRegistro(String linea) {
        try {
            var reg = SALIDA.resolve("registro.csv");
            if (!Files.exists(reg)) Files.writeString(reg, "caso;brazo;prediction_id;ms;origen\n");
            Files.writeString(reg, linea + "\n", java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static final List<String> GIMNASIO = List.of("body weight", "dumbbell", "barbell", "cable", "leverage machine",
            "bench", "band", "stationary bike", "treadmill", "kettlebell", "ez barbell", "smith machine", "pull-up bar");
    static final List<String> CASA = List.of("body weight", "dumbbell", "band");

    record Caso(String id, GoalType objetivo, FitnessLevel nivel, TrainingLocation lugar, List<Short> dias,
                short minutos, boolean restringido, String origen) {}

    /**
     * G01-G24: los 24 casos de la evaluacion congelada de la PR #69 ({@code EvaluacionPareadaH2IT.casos()}), copiados sin
     * cambios. G25-G26: GAIN_MUSCLE de 60 minutos con 5-6 dias, pedidos por la orden (problemas de factibilidad
     * observados en campana_mvp/INFORME_U06_4D_v3_2.md: en casa, intermedio, el motor de reglas no tiene plan valido).
     */
    static List<Caso> casos() {
        var l = new ArrayList<Caso>();
        int n = 1;
        for (var o : List.of(GoalType.GAIN_MUSCLE, GoalType.LOSE_WEIGHT, GoalType.INCREASE_STRENGTH,
                GoalType.GENERAL_FITNESS)) {
            l.add(new Caso(id(n++), o, FitnessLevel.BEGINNER, TrainingLocation.GYM, dias(1, 3, 5), (short) 45, false, "PR69"));
            l.add(new Caso(id(n++), o, FitnessLevel.BEGINNER, TrainingLocation.HOME, dias(1, 2, 4, 5), (short) 60, false, "PR69"));
            l.add(new Caso(id(n++), o, FitnessLevel.INTERMEDIATE, TrainingLocation.GYM, dias(1, 2, 4, 5), (short) 60, false, "PR69"));
            l.add(new Caso(id(n++), o, FitnessLevel.INTERMEDIATE, TrainingLocation.HOME, dias(1, 3, 5), (short) 45, false, "PR69"));
            l.add(new Caso(id(n++), o, FitnessLevel.INTERMEDIATE, TrainingLocation.GYM, dias(1, 2, 3, 5, 6), (short) 60, true, "PR69"));
            l.add(new Caso(id(n++), o, FitnessLevel.BEGINNER, TrainingLocation.GYM, dias(1, 4, 5, 7), (short) 60, false, "PR69"));
        }
        l.add(new Caso("G25", GoalType.GAIN_MUSCLE, FitnessLevel.INTERMEDIATE, TrainingLocation.GYM,
                dias(1, 2, 3, 4, 5, 6), (short) 60, false, "ORDEN_GM_5_6_DIAS_60"));
        l.add(new Caso("G26", GoalType.GAIN_MUSCLE, FitnessLevel.INTERMEDIATE, TrainingLocation.HOME,
                dias(1, 2, 3, 5, 6), (short) 60, false, "ORDEN_GM_5_6_DIAS_60"));
        if (CASOS.isBlank()) return l;
        var orden = List.of(CASOS.split(","));
        return orden.stream().map(id -> l.stream().filter(c -> c.id().equals(id)).findFirst().orElseThrow()).toList();
    }

    private static String id(int n) { return "G%02d".formatted(n); }

    private static List<Short> dias(int... d) { return Arrays.stream(d).mapToObj(x -> (short) x).toList(); }

    /** Una peticion ya construida: lo que se enviaria a Replicate. */
    record Peticion(String version, String prompt, String sistema, long bytes, double cota) {}

    record Fila(Caso caso, String brazo, int orden, String estado, String version, long caracteres, double cota,
                String predictionId, long ms, int caracteresSalida, String validacion, int v15, int v15Mitad,
                String otros, String detalle) {}

    final List<String> lineas = Collections.synchronizedList(new ArrayList<>());

    @Test
    void primeraPropuestaPareada() throws Exception {
        assumeTrue(MODO.equals("PROMPTS") || REAL || VALIDAR || SIMULADO,
                "opt-in: -Dfitsense.h2v2.modo=PROMPTS|REAL|VALIDAR|SIMULADO");
        if (REPLICAS > 0 || SIMULADO) {
            pareadaConReplicas();
            return;
        }
        var registro = new HashMap<String, String[]>();
        if (VALIDAR && !REGISTRO.isBlank())
            for (var l : Files.readAllLines(Path.of(REGISTRO)))
                if (!l.isBlank() && !l.startsWith("caso;")) {
                    var x = l.split(";");
                    registro.put(x[0] + "_" + x[1], x);
                }
        if (REAL) {
            assertEquals("SI", System.getProperty("fitsense.h2v2.autorizado"), "REAL exige autorizado=SI");
            assertTrue(TOPE_USD > 0, "REAL exige tope-usd");
        }
        assertEquals(ReplicateProperties.Formato.DIRECT, propiedades.formato(), "la evaluacion es con gpt-5-mini directo");
        assertEquals("medium", propiedades.reasoningEffort());
        Files.createDirectories(SALIDA.resolve("salidas"));
        escuchar();

        var filas = new ArrayList<Fila>();
        double gastado = 0;
        var hechos = new HashMap<String, String[]>();
        if (REAL) {
            assertTrue(TOPE_USD <= libro.topeTotal(), "el tope de la etapa no supera el total autorizado");
            var reg = SALIDA.resolve("registro.csv");
            if (Files.exists(reg))
                for (var l : Files.readAllLines(reg))
                    if (!l.isBlank() && !l.startsWith("caso;")) {
                        var x = l.split(";");
                        if (Files.exists(SALIDA.resolve("salidas").resolve(x[0] + "_" + x[1] + ".json")))
                            hechos.put(x[0] + "_" + x[1], x);
                    }
            System.out.println("H2V2_LIBRO reservado_total=" + libro.reservado(null) + " reservado_" + ETAPA + "="
                    + libro.reservado(ETAPA) + " saldo=" + libro.saldo() + " ya_hechas=" + hechos.keySet());
        }
        String corte = null;
        var lista = casos();
        for (int i = 0; i < lista.size(); i++) {
            var c = lista.get(i);
            long u = participante(c);
            PlanGenerationContext ctx;
            try {
                ctx = planComandos.prepare(GenerateWeeklyPlanCommand.firstPlan(u, SEMANA)).context();
                assertEquals("PLAN-REV-2.0", ctx.policy().id(), "la politica por omision, igual en los dos brazos");
            } catch (RuntimeException e) {
                for (var b : List.of("A", "B"))
                    filas.add(new Fila(c, b, 0, "NO_FACTIBLE_PREPARE", "-", 0, 0, "-", 0, 0, "-", 0, 0, "-",
                            e.getClass().getSimpleName() + ": " + e.getMessage()));
                continue;
            }
            var inviable = FactibilidadGeneracionIA.evaluar(ctx, validador.coverageEnforcement(),
                    validador.capabilityEnforcement());
            var orden = i % 2 == 0 ? List.of("A", "B") : List.of("B", "A");
            var peticiones = new LinkedHashMap<String, Peticion>();
            for (var b : orden) peticiones.put(b, construir(ctx, b.equals("B")));
            if (inviable.isPresent()) {
                for (var b : orden)
                    filas.add(new Fila(c, b, 0, "NO_INVOCADA_INVIABLE", peticiones.get(b).version(),
                            peticiones.get(b).prompt().length(), peticiones.get(b).cota(), "-", 0, 0, "-", 0, 0, "-",
                            inviable.get().describe()));
                continue;
            }
            if (VALIDAR) {
                int k = 0;
                for (var b : orden) {
                    k++;
                    var fichero = SALIDAS_DIR.resolve(c.id() + "_" + b + ".json");
                    var p = peticiones.get(b);
                    if (!Files.exists(fichero)) {
                        filas.add(new Fila(c, b, k, "NO_DISPONIBLE", p.version(), p.prompt().length() + p.sistema().length(),
                                p.cota(), "-", 0, 0, "-", 0, 0, "-", "sin salida real guardada"));
                        continue;
                    }
                    var r = registro.getOrDefault(c.id() + "_" + b, new String[]{c.id(), b, "desconocido", "0", "-"});
                    filas.add(validarSalida(c, b, k, ctx, p, Files.readString(fichero), r[2], Long.parseLong(r[3]),
                            r.length > 4 ? r[4] : "-"));
                }
                continue;
            }
            if (!REAL) {
                // Diagnostico sin coste: el texto exacto de cada peticion (prompt e instruccion de sistema).
                if ("SI".equals(System.getProperty("fitsense.h2v2.volcar-prompts")))
                    for (var b : orden)
                        guardar(c.id() + "_" + b + "_prompt.txt", peticiones.get(b).sistema() + "\n\n=====\n\n"
                                + peticiones.get(b).prompt());
                for (var b : orden)
                    filas.add(new Fila(c, b, 0, "SOLO_PROMPT", peticiones.get(b).version(),
                            peticiones.get(b).prompt().length() + peticiones.get(b).sistema().length(),
                            peticiones.get(b).cota(), "-", 0, 0, "-", 0, 0, "-", ""));
                continue;
            }
            // REAL. Reanudacion sin duplicar: una salida ya guardada en esta carpeta (con su linea en registro.csv) no se
            // vuelve a pedir; se valida otra vez sin coste. Solo se piden los brazos que faltan.
            var faltan = new ArrayList<String>();
            for (var b : orden) if (!hechos.containsKey(c.id() + "_" + b)) faltan.add(b);
            double pareja = faltan.stream().mapToDouble(b -> peticiones.get(b).cota()).sum();
            // Parejas completas: si no caben los brazos que faltan (en el tope de la etapa y en el total), no se empieza.
            if (corte == null && !faltan.isEmpty() && !libro.cabe(ETAPA, TOPE_USD, pareja))
                corte = "no cabe la pareja " + c.id() + ": cota " + "%.4f".formatted(pareja) + " + reservado etapa "
                        + "%.4f".formatted(libro.reservado(ETAPA)) + " (tope etapa " + TOPE_USD + "), reservado total "
                        + "%.4f".formatted(libro.reservado(null)) + " (tope total " + LibroPresupuestoH2v2.TOPE_TOTAL_USD + ")";
            if (corte != null && !faltan.isEmpty()) {
                for (var b : orden)
                    filas.add(new Fila(c, b, 0, "NO_EJECUTADO_PRESUPUESTO", peticiones.get(b).version(),
                            peticiones.get(b).prompt().length(), peticiones.get(b).cota(), "-", 0, 0, "-", 0, 0, "-",
                            corte));
                continue;
            }
            int k = 0;
            for (var b : orden) {
                k++;
                var p = peticiones.get(b);
                var previo = hechos.get(c.id() + "_" + b);
                if (previo != null) {
                    filas.add(validarSalida(c, b, k, ctx, p, Files.readString(SALIDA.resolve("salidas")
                            .resolve(c.id() + "_" + b + ".json")), previo[2], Long.parseLong(previo[3]), "REANUDADA"));
                    continue;
                }
                if (!libro.reservar(ETAPA, TOPE_USD, c.id(), b, 1, p.cota(), p.version()))
                    throw new IllegalStateException("la reserva no cabe tras comprobar la pareja: " + c.id() + b);
                gastado += p.cota();
                var f = llamar(c, b, k, ctx, p);
                filas.add(f);
                libro.resultado(ETAPA, c.id(), b, 1, f.predictionId(), f.estado() + " " + f.validacion());
                if (f.estado().equals("RESPUESTA"))
                    anotarRegistro(c.id() + ";" + b + ";" + f.predictionId() + ";" + f.ms() + ";REAL");
            }
        }
        escribir(filas, gastado, corte);
    }

    /**
     * OPCION C (H2-v4): parejas con REPLICAS, en el orden fijado de {@link PlanEjecucionH2}. REAL (con coste, libro y tope),
     * SIMULADO (Replicate local, libro aparte) o VALIDAR (sin coste, sobre las salidas guardadas). Reanuda sin repetir: lo
     * que ya esta en el registro con su salida no se vuelve a pedir.
     */
    private void pareadaConReplicas() throws Exception {
        int replicas = Math.max(1, REPLICAS);
        if (REAL) {
            assertEquals("SI", System.getProperty("fitsense.h2v2.autorizado"), "REAL exige autorizado=SI");
            assertTrue(TOPE_USD > 0, "REAL exige tope-usd");
        }
        assertEquals(ReplicateProperties.Formato.DIRECT, propiedades.formato(), "la evaluacion es con gpt-5-mini directo");
        assertEquals("medium", propiedades.reasoningEffort());
        assertEquals("low", propiedades.verbosity());
        assertEquals(16_000, propiedades.maxCompletionTokens());
        Files.createDirectories(SALIDA.resolve("salidas"));
        escuchar();
        var libroUsado = SIMULADO ? new LibroPresupuestoH2v2(SALIDA.resolve("libro_simulado.csv"), TOPE_TOTAL) : libro;
        if (REAL || SIMULADO) assertTrue(TOPE_USD <= libroUsado.topeTotal(), "el tope de la etapa no supera el total");
        var reg = new PlanEjecucionH2.Registro(SALIDA.resolve("registro_replicas.csv"), SALIDA.resolve("salidas"));
        var hechas = reg.hechas();
        // VALIDAR no reserva ni abre libro (sin coste).
        System.out.println(libroUsado == null ? "H2V2_LIBRO ninguno (VALIDAR, sin coste) ya_hechas=" + hechas.size()
                : "H2V2_LIBRO reservado_total=" + libroUsado.reservado(null) + " reservado_" + ETAPA + "="
                + libroUsado.reservado(ETAPA) + " saldo=" + libroUsado.saldo() + " ya_hechas=" + hechas.size());
        var todos = casos();
        var ids = todos.stream().map(Caso::id).toList();
        var contextos = new HashMap<String, PlanGenerationContext>();
        var peticiones = new HashMap<String, Peticion>();
        var filas = new ArrayList<Map.Entry<Integer, Fila>>();
        double gastado = 0;
        int nuevas = 0;
        String corte = null;
        for (var paso : PlanEjecucionH2.plan(ids, replicas, SOLO_REPLICA)) {
            var c = todos.get(ids.indexOf(paso.caso()));
            int r = paso.replica();
            PlanGenerationContext ctx;
            try {
                ctx = contextos.computeIfAbsent(c.id(), k -> {
                    var x = planComandos.prepare(GenerateWeeklyPlanCommand.firstPlan(participante(c), SEMANA)).context();
                    assertEquals("PLAN-REV-2.0", x.policy().id(), "la politica por omision, igual en los dos brazos");
                    return x;
                });
            } catch (RuntimeException e) {
                for (var b : paso.orden())
                    filas.add(Map.entry(r, new Fila(c, b, 0, "NO_FACTIBLE_PREPARE", "-", 0, 0, "-", 0, 0, "-", 0, 0, "-",
                            e.getClass().getSimpleName() + ": " + e.getMessage())));
                continue;
            }
            for (var b : paso.orden())
                peticiones.computeIfAbsent(c.id() + "_" + b, k -> construir(ctx, b.equals("B")));
            var inviable = FactibilidadGeneracionIA.evaluar(ctx, validador.coverageEnforcement(),
                    validador.capabilityEnforcement());
            if (inviable.isPresent()) {
                for (var b : paso.orden()) {
                    var p = peticiones.get(c.id() + "_" + b);
                    filas.add(Map.entry(r, new Fila(c, b, 0, "NO_INVOCADA_INVIABLE", p.version(), p.prompt().length(),
                            p.cota(), "-", 0, 0, "-", 0, 0, "-", inviable.get().describe())));
                }
                continue;
            }
            var faltan = paso.orden().stream().filter(b -> !hechas.containsKey(paso.clave(b))).toList();
            if (VALIDAR) {
                int k = 0;
                for (var b : paso.orden()) {
                    k++;
                    var p = peticiones.get(c.id() + "_" + b);
                    var previo = hechas.get(paso.clave(b));
                    if (previo == null) {
                        filas.add(Map.entry(r, new Fila(c, b, k, "NO_DISPONIBLE", p.version(), p.prompt().length()
                                + p.sistema().length(), p.cota(), "-", 0, 0, "-", 0, 0, "-", "sin salida guardada")));
                        continue;
                    }
                    filas.add(Map.entry(r, validarSalida(c, b, k, ctx, p, Files.readString(reg.salida(paso.clave(b))),
                            previo[3], Long.parseLong(previo[4]), "VALIDAR", paso.clave(b))));
                }
                continue;
            }
            double pareja = faltan.stream().mapToDouble(b -> peticiones.get(c.id() + "_" + b).cota()).sum();
            if (corte == null && !faltan.isEmpty() && !libroUsado.cabe(ETAPA, TOPE_USD, pareja))
                corte = "no cabe la pareja " + c.id() + " r" + r + ": cota " + "%.4f".formatted(pareja)
                        + " + reservado etapa " + "%.4f".formatted(libroUsado.reservado(ETAPA)) + " (tope etapa " + TOPE_USD
                        + "), reservado total " + "%.4f".formatted(libroUsado.reservado(null)) + " (tope total "
                        + libroUsado.topeTotal() + ")";
            if (corte != null && !faltan.isEmpty()) {
                for (var b : paso.orden()) {
                    var p = peticiones.get(c.id() + "_" + b);
                    filas.add(Map.entry(r, new Fila(c, b, 0, "NO_EJECUTADO_PRESUPUESTO", p.version(), p.prompt().length(),
                            p.cota(), "-", 0, 0, "-", 0, 0, "-", corte)));
                }
                continue;
            }
            int k = 0;
            for (var b : paso.orden()) {
                k++;
                var p = peticiones.get(c.id() + "_" + b);
                var previo = hechas.get(paso.clave(b));
                if (previo != null) {
                    filas.add(Map.entry(r, validarSalida(c, b, k, ctx, p, Files.readString(reg.salida(paso.clave(b))),
                            previo[3], Long.parseLong(previo[4]), "REANUDADA", paso.clave(b))));
                    continue;
                }
                if (!libroUsado.reservar(ETAPA, TOPE_USD, c.id() + "#r" + r, b, 1, p.cota(), p.version()))
                    throw new IllegalStateException("la reserva no cabe tras comprobar la pareja: " + paso.clave(b));
                gastado += p.cota();
                var f = llamar(c, b, k, ctx, p, paso.clave(b));
                filas.add(Map.entry(r, f));
                libroUsado.resultado(ETAPA, c.id() + "#r" + r, b, 1, f.predictionId(), f.estado() + " " + f.validacion());
                if (f.estado().equals("RESPUESTA")) {
                    reg.anotar(c.id(), r, b, f.predictionId(), f.ms(), SIMULADO ? "SIMULADO" : "REAL",
                            Files.readString(reg.salida(paso.clave(b))));
                    nuevas++;
                    if (DETENER_TRAS > 0 && nuevas >= DETENER_TRAS) {
                        escribirReplicas(filas, gastado, "interrupcion simulada tras " + nuevas, libroUsado);
                        throw new IllegalStateException("H2V2_INTERRUPCION_SIMULADA tras " + nuevas + " predicciones");
                    }
                }
            }
        }
        System.out.println("H2V2_REPLICAS nuevas=" + nuevas + " posts_simulados=" + POSTS_SIM.get()
                + " reservado_total=" + (libroUsado == null ? 0.0 : libroUsado.reservado(null)));
        escribirReplicas(filas, gastado, corte, libroUsado);
    }

    private void escribirReplicas(List<Map.Entry<Integer, Fila>> filas, double gastado, String corte,
                                  LibroPresupuestoH2v2 libroUsado) throws Exception {
        var sb = new StringBuilder("modo;caso;origen_caso;objetivo;nivel;lugar;dias;minutos;restringido;brazo;orden;"
                + "estado;prompt_version;caracteres_entrada;cota_usd;prediction_id;ms;caracteres_salida;validacion;"
                + "rechazos_v15;rechazos_v15_mitad;otros;detalle;replica\n");
        for (var e : filas) {
            var f = e.getValue();
            var c = f.caso();
            sb.append(String.join(";", MODO, c.id(), c.origen(), c.objetivo().name(), c.nivel().name(), c.lugar().name(),
                    String.valueOf(c.dias().size()), String.valueOf(c.minutos()), String.valueOf(c.restringido()),
                    f.brazo(), String.valueOf(f.orden()), f.estado(), f.version(), String.valueOf(f.caracteres()),
                    String.format(Locale.ROOT, "%.6f", f.cota()), f.predictionId(), String.valueOf(f.ms()),
                    String.valueOf(f.caracteresSalida()), f.validacion(), String.valueOf(f.v15()),
                    String.valueOf(f.v15Mitad()), f.otros(), f.detalle().replace(';', ',').replace('\n', ' '),
                    String.valueOf(e.getKey()))).append('\n');
        }
        String nombre = SOLO_REPLICA == 0 ? "primera_propuesta.csv" : "primera_propuesta_r" + SOLO_REPLICA + ".csv";
        Files.writeString(SALIDA.resolve(nombre), sb.toString());
        Files.write(SALIDA.resolve("consumo_replicate" + (SOLO_REPLICA == 0 ? "" : "_r" + SOLO_REPLICA) + ".log"),
                List.copyOf(lineas));
        Files.writeString(SALIDA.resolve("99_resumen" + (SOLO_REPLICA == 0 ? "" : "_r" + SOLO_REPLICA) + ".txt"),
                "modo=" + MODO + " replicas=" + REPLICAS + " solo_replica=" + SOLO_REPLICA + " version_b=" + VERSION_B
                + "\nllamadas_proveedor_esta_corrida=" + filas.stream().filter(x -> x.getValue().estado().equals("RESPUESTA")
                && !x.getValue().detalle().equals("REANUDADA") || x.getValue().estado().equals("ERROR_PROVEEDOR")).count()
                + "\nusd_reservado_esta_corrida=" + String.format(Locale.ROOT, "%.4f", gastado)
                + (libroUsado == null ? "\nusd_reservado_total_libro=- (VALIDAR, sin libro)"
                : "\nusd_reservado_total_libro=" + String.format(Locale.ROOT, "%.4f", libroUsado.reservado(null))
                + " (COTA superior por prediccion; tope " + libroUsado.topeTotal() + ")")
                + "\ncorte=" + (corte == null ? "ninguno" : corte) + "\nmodelo=" + propiedades.modeloEfectivo()
                + " formato=" + propiedades.formato().etiqueta() + " reasoning_effort=" + propiedades.reasoningEffort()
                + " verbosity=" + propiedades.verbosity() + " max_completion_tokens=" + propiedades.maxCompletionTokens()
                + "\n");
    }


    /** Construye la peticion de un brazo exactamente como requestOutput (mismo prompt, misma instruccion de sistema). */
    private Peticion construir(PlanGenerationContext ctx, boolean h2v2) {
        fijarInterruptor(h2v2);
        var snapshot = json.write(PlanInputSnapshot.of(ctx, promptBuilder.version()));
        var prompt = promptBuilder.build(ctx, snapshot, AiRetryFeedback.none());
        var sistema = ReplicateTrainingPlanGenerator.instruccionDeSistema(
                ReplicateTrainingPlanGenerator.Peticion.deSemana(ctx.effectiveDaysPerWeek(), promptBuilder.version()));
        long bytes = prompt.getBytes(StandardCharsets.UTF_8).length + sistema.getBytes(StandardCharsets.UTF_8).length;
        // La misma cota que REPLICATE_CONSUMO: 1 token por byte + 256 de formato, y el tope de salida.
        double cota = (bytes + 256) / 1e6 * USD_ENTRADA + propiedades.maxCompletionTokens() / 1e6 * USD_SALIDA;
        return new Peticion(promptBuilder.version(), prompt, sistema, bytes, cota);
    }

    /** A = PR-1.7, B = PR-2.0: el prompt y la instantanea que guarda el plan, como en produccion. */
    private void fijarInterruptor(boolean h2v2) {
        String v = h2v2 ? VERSION_B : PlanPromptBuilder.VERSION;
        ReflectionTestUtils.invokeMethod((Object) AopTestUtils.getUltimateTargetObject(promptBuilder), "setPromptVersion",
                v);
        ReflectionTestUtils.invokeMethod((Object) AopTestUtils.getUltimateTargetObject(planComandos),
                "setPromptVersionGeneracion", v);
        assertEquals(h2v2, promptBuilder.h2v2());
    }

    private static final Pattern MITAD = Pattern.compile("V15: el entrenamiento del \\S+ dedica \\d+ de \\d+ ejercicios");

    /** VALIDAR: la misma evaluacion que {@link #llamar}, sobre una salida real ya obtenida. */
    private Fila validarSalida(Caso c, String brazo, int orden, PlanGenerationContext ctx, Peticion p, String salida,
                               String predictionId, long ms, String origen) {
        return validarSalida(c, brazo, orden, ctx, p, salida, predictionId, ms, origen, c.id() + "_" + brazo);
    }

    private Fila validarSalida(Caso c, String brazo, int orden, PlanGenerationContext ctx, Peticion p, String salida,
                               String predictionId, long ms, String origen, String nombre) {
        fijarInterruptor(brazo.equals("B"));
        List<String> problemas;
        try {
            var borrador = normalizador.normalize(generador.toDraft(salida), ctx).draft();
            validador.validate(borrador, ctx, configuracion.durationToRepsDivisor());
            problemas = List.of();
        } catch (InvalidPlanDraftException e) {
            problemas = e.problems();
        }
        guardar(nombre + "_problemas.txt", String.join("\n", problemas));
        int v15 = (int) problemas.stream().filter(x -> x.startsWith("V15")).count();
        int mitad = (int) problemas.stream().filter(x -> MITAD.matcher(x).find()).count();
        var otros = problemas.stream().map(H2v2PrimeraPropuestaIT::codigo).filter(x -> !x.equals("V15"))
                .collect(Collectors.groupingBy(x -> x, TreeMap::new, Collectors.counting())).toString();
        return new Fila(c, brazo, orden, "RESPUESTA", p.version(), p.prompt().length() + p.sistema().length(), p.cota(),
                predictionId, ms, salida.length(), problemas.isEmpty() ? "VALIDA" : "RECHAZADA", v15, mitad, otros,
                origen);
    }

    private Fila llamar(Caso c, String brazo, int orden, PlanGenerationContext ctx, Peticion p) {
        return llamar(c, brazo, orden, ctx, p, c.id() + "_" + brazo);
    }

    /** @param nombre el nombre de la salida guardada (con replicas, la clave de {@link PlanEjecucionH2}). */
    private Fila llamar(Caso c, String brazo, int orden, PlanGenerationContext ctx, Peticion p, String nombre) {
        fijarInterruptor(brazo.equals("B"));
        int antes = lineas.size();
        long t0 = System.nanoTime();
        String salida;
        try {
            salida = generador.requestOutput(ctx, AiRetryFeedback.none(), 1);
        } catch (RuntimeException e) {
            long ms = (System.nanoTime() - t0) / 1_000_000;
            return new Fila(c, brazo, orden, "ERROR_PROVEEDOR", p.version(), p.prompt().length(), p.cota(),
                    predictionIdDesde(antes), ms, 0, "-", 0, 0, "-", e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        long ms = (System.nanoTime() - t0) / 1_000_000;
        guardar(nombre + ".json", salida);
        var consumo = lineas.subList(antes, lineas.size()).stream().filter(x -> x.startsWith("REPLICATE_CONSUMO"))
                .reduce((a, b2) -> b2).orElse("");
        assertTrue(consumo.contains("caracteres_entrada=" + (p.prompt().length() + p.sistema().length())),
                "la peticion enviada es la construida para la cota: " + consumo);
        List<String> problemas;
        try {
            var borrador = normalizador.normalize(generador.toDraft(salida), ctx).draft();
            validador.validate(borrador, ctx, configuracion.durationToRepsDivisor());
            problemas = List.of();
        } catch (InvalidPlanDraftException e) {
            problemas = e.problems();
        }
        int v15 = (int) problemas.stream().filter(x -> x.startsWith("V15")).count();
        int mitad = (int) problemas.stream().filter(x -> MITAD.matcher(x).find()).count();
        var otros = problemas.stream().map(H2v2PrimeraPropuestaIT::codigo).filter(x -> !x.equals("V15"))
                .collect(Collectors.groupingBy(x -> x, TreeMap::new, Collectors.counting())).toString();
        guardar(nombre + "_problemas.txt", String.join("\n", problemas));
        return new Fila(c, brazo, orden, "RESPUESTA", p.version(), p.prompt().length() + p.sistema().length(), p.cota(),
                predictionIdDesde(antes), ms, salida.length(), problemas.isEmpty() ? "VALIDA" : "RECHAZADA", v15, mitad, otros, "");
    }

    private static final Pattern CODIGO = Pattern.compile("^(V\\d+[A-Z]?)\\b");

    private static String codigo(String problema) {
        var m = CODIGO.matcher(problema.strip());
        return m.find() ? m.group(1) : "CONTRATO/OTRO";
    }

    private String predictionIdDesde(int desde) {
        var m = Pattern.compile("REPLICATE_TIMING .*prediction_id=(\\S+)");
        for (var x : lineas.subList(desde, lineas.size())) {
            var r = m.matcher(x);
            if (r.find()) return r.group(1);
        }
        return "desconocido";
    }

    private void escuchar() {
        var a = new AppenderBase<ILoggingEvent>() {
            @Override
            protected void append(ILoggingEvent e) {
                var t = e.getFormattedMessage();
                if (t != null && (t.startsWith("REPLICATE_TIMING") || t.startsWith("REPLICATE_CONSUMO"))) lineas.add(t);
            }
        };
        a.start();
        ((Logger) LoggerFactory.getLogger(ReplicateTrainingPlanGenerator.class)).addAppender(a);
    }

    /** Mismo participante sintetico que EvaluacionPareadaH2IT (uno por caso: el contexto es el mismo en los dos brazos). */
    private long participante(Caso c) {
        String email = ("h2v2-" + MODO + "-" + c.id() + "@pruebas.invalid").toLowerCase();
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Sintetico', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        boolean gym = c.lugar() == TrainingLocation.GYM;
        boolean r = c.restringido();
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1990, 3, 10), BiologicalSex.FEMALE,
                new BigDecimal("165"), new BigDecimal("72"), null, c.nivel(), c.objetivo(), "evaluacion pareada H2",
                c.lugar(), (short) c.dias().size(), c.dias(), c.minutos(), gym ? GIMNASIO : CASA, List.of(),
                r ? "Restricciones declaradas" : "Ninguna", !r, true, !r, !r, !r));
        return u;
    }

    private void guardar(String nombre, String contenido) {
        try {
            Files.writeString(SALIDA.resolve("salidas").resolve(nombre), contenido == null ? "" : contenido);
        } catch (Exception ignored) {
        }
    }

    private void escribir(List<Fila> filas, double gastado, String corte) throws Exception {
        var sb = new StringBuilder("modo;caso;origen_caso;objetivo;nivel;lugar;dias;minutos;restringido;brazo;orden;"
                + "estado;prompt_version;caracteres_entrada;cota_usd;prediction_id;ms;caracteres_salida;validacion;"
                + "rechazos_v15;rechazos_v15_mitad;otros;detalle\n");
        for (var f : filas) {
            var c = f.caso();
            sb.append(String.join(";", MODO, c.id(), c.origen(), c.objetivo().name(), c.nivel().name(), c.lugar().name(),
                    String.valueOf(c.dias().size()), String.valueOf(c.minutos()), String.valueOf(c.restringido()),
                    f.brazo(), String.valueOf(f.orden()), f.estado(), f.version(), String.valueOf(f.caracteres()),
                    String.format(Locale.ROOT, "%.6f", f.cota()), f.predictionId(), String.valueOf(f.ms()),
                    String.valueOf(f.caracteresSalida()), f.validacion(), String.valueOf(f.v15()),
                    String.valueOf(f.v15Mitad()), f.otros(), f.detalle().replace(';', ',').replace('\n', ' ')))
                    .append('\n');
        }
        Files.writeString(SALIDA.resolve("primera_propuesta.csv"), sb.toString());
        Files.write(SALIDA.resolve("consumo_replicate.log"), List.copyOf(lineas));
        Files.writeString(SALIDA.resolve("99_resumen.txt"), "modo=" + MODO + "\nllamadas_proveedor="
                + filas.stream().filter(f -> f.estado().equals("RESPUESTA") || f.estado().equals("ERROR_PROVEEDOR")).count()
                + "\nusd_medido=" + String.format(Locale.ROOT, "%.4f", gastado) + " (COTA superior por prediccion)"
                + "\ncorte=" + (corte == null ? "ninguno" : corte) + "\nmodelo=" + propiedades.modeloEfectivo()
                + " formato=" + propiedades.formato().etiqueta() + " reasoning_effort=" + propiedades.reasoningEffort()
                + " verbosity=" + propiedades.verbosity() + " max_completion_tokens=" + propiedades.maxCompletionTokens()
                + "\n");
    }
}
