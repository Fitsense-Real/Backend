package main.web.services.fitsense.orchestation;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.fasterxml.jackson.databind.ObjectMapper;
import main.web.services.fitsense.planning.application.internal.commandservices.GeneracionInicialService;
import main.web.services.fitsense.planning.application.internal.commandservices.PlanGenerationPipeline;
import main.web.services.fitsense.planning.application.internal.commandservices.WeeklyTrainingPlanCommandServiceImpl;
import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.GenerationSource;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanGenerationContext;
import main.web.services.fitsense.planning.domain.model.valueobjects.PrescriptionType;
import main.web.services.fitsense.planning.domain.model.valueobjects.WorkoutFocus;
import main.web.services.fitsense.planning.domain.services.PlanDraftValidator;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
import main.web.services.fitsense.planning.infrastructure.generation.ai.AiRetryFeedback;
import main.web.services.fitsense.planning.infrastructure.generation.ai.PlanPromptBuilder;
import main.web.services.fitsense.planning.infrastructure.generation.ai.ReplicateTrainingPlanGenerator;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.*;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;

/**
 * H2 · EVALUACION PAREADA A/B DE LA GENERACION INICIAL (orden 3, fases 3-6). OPT-IN.
 * <p>
 * Configuracion A = PR-1.7 (fitsense.ai.generacion.contrato-v15=false, la evaluada). Configuracion B = PR-1.9 (true).
 * Cada perfil se genera DOS veces, con dos participantes sinteticos identicos, uno por brazo, en orden alterno (impar
 * A→B, par B→A). Todo lo demas es igual: catalogo, politica PLAN-REV-2.0, modelo, razonamiento, validadores y el
 * camino de produccion {@link GeneracionInicialService#generar} (prepare → generate con hasta 3 intentos → persist).
 * <p>
 * Se registran por separado las DOS etapas: cada propuesta de la IA (prediccion, intento, tiempo, motivos de rechazo)
 * y el resultado del sistema (publicado por la IA, respaldo del motor de reglas, NO_FACTIBLE o error). Un respaldo
 * siempre queda marcado como tal: nunca se cuenta como propuesta valida de la IA.
 * <p>
 * Modos ({@code -Dfitsense.h2.pareada.modo}):
 * <ul>
 *   <li>sin la propiedad: la prueba se omite (asi corre en el CI sin coste);</li>
 *   <li>{@code ENSAYO}: SIN Replicate. El proveedor se sustituye por una respuesta SIMULADA invalida para ejercitar el
 *   arnes (alternancia de brazos, contador, tope, respaldo, informes). Sus resultados NO son resultados de la IA y se
 *   etiquetan {@code SIMULADO} en todos los ficheros;</li>
 *   <li>{@code REAL}: Replicate real. Exige {@code -Dfitsense.h2.pareada.autorizado=SI}, el tope
 *   {@code -Dfitsense.h2.pareada.tope-usd} y el token en el proxy o en {@code -Dfitsense.h2.pareada.token}.</li>
 * </ul>
 * <b>Tope duro</b>: antes de CADA prediccion, {@link ControlPresupuestoK20#puedeLlamar} con una reserva de peor caso
 * por prediccion ({@code reserva-usd}, 0,05 por omision): n x reserva <= tope y n <= max-predicciones. Si no cabe, la
 * campana se detiene, el caso se marca {@code CORTE_PRESUPUESTO} y no se empieza ningun caso mas. El coste medido por
 * tokens se lee despues de cada prediccion (GET /v1/predictions/{id}, sin coste) y se informa aparte.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=true",
        "fitsense.ai.api-token=${fitsense.h2.pareada.token:proxy}",
        "fitsense.ai.model=gpt-5-mini",
        "fitsense.ai.reasoning-effort=medium",
        "fitsense.ai.prefer-wait-seconds=${fitsense.h2.pareada.espera:20}",
        // Cada participante genera una sola vez; el limite por hora no interviene.
        "fitsense.ai.generate.max-per-hour=100"
})
@DisplayName("H2 · evaluacion pareada PR-1.7 vs PR-1.9 (opt-in; ENSAYO sin coste o REAL con tope)")
class EvaluacionPareadaH2IT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @MockitoSpyBean ReplicateTrainingPlanGenerator generador;
    @Autowired PlanPromptBuilder promptBuilder;
    @Autowired WeeklyTrainingPlanCommandService planComandos;
    @Autowired GeneracionInicialService generacionInicial;
    @Autowired PlanDraftValidator validador;
    @Autowired UserProfileCommandService perfiles;
    @Autowired JdbcTemplate jdbc;

    static final String MODO = System.getProperty("fitsense.h2.pareada.modo", "");
    static final boolean REAL = "REAL".equals(MODO);
    static final double TOPE_USD = Double.parseDouble(System.getProperty("fitsense.h2.pareada.tope-usd", "0"));
    static final double RESERVA_USD = Double.parseDouble(System.getProperty("fitsense.h2.pareada.reserva-usd", "0.05"));
    static final int MAX_PREDICCIONES = Integer.getInteger("fitsense.h2.pareada.max-predicciones", 144);
    static final double USD_ENTRADA = Double.parseDouble(System.getProperty("fitsense.h2.pareada.usd-entrada", "0.25"));
    static final double USD_SALIDA = Double.parseDouble(System.getProperty("fitsense.h2.pareada.usd-salida", "2.00"));
    /** Subconjunto opcional de casos (G01,G05,...) para un piloto. */
    static final String CASOS = System.getProperty("fitsense.h2.pareada.casos", "");
    /** Intentos maximos del sistema por generacion (fitsense: 3). */
    static final int INTENTOS = 3;
    static final Path SALIDA = Path.of("target", "h2-pareada", MODO.isEmpty() ? "omitida" : MODO.toLowerCase());
    static final LocalDate SEMANA = LocalDate.of(2026, 11, 23);
    static final Set<String> NO_FACTIBLE = Set.of("InsufficientSlotCoverageException", "FuerzaSinTiempoException",
            "ProfileOutsideGoalDurationException", "PlanGenerationFailedException");

    static final List<String> GIMNASIO = List.of("body weight", "dumbbell", "barbell", "cable", "leverage machine",
            "bench", "band", "stationary bike", "treadmill", "kettlebell", "ez barbell", "smith machine", "pull-up bar");
    static final List<String> CASA = List.of("body weight", "dumbbell", "band");

    /** Perfil del protocolo congelado (DISENO_EVALUACION_PAREADA.md §2). */
    record Caso(String id, GoalType objetivo, FitnessLevel nivel, TrainingLocation lugar, List<Short> dias,
                short minutos, boolean restringido) {}

    static List<Caso> casos() {
        var l = new ArrayList<Caso>();
        int n = 1;
        for (var o : List.of(GoalType.GAIN_MUSCLE, GoalType.LOSE_WEIGHT, GoalType.INCREASE_STRENGTH,
                GoalType.GENERAL_FITNESS)) {
            l.add(new Caso(id(n++), o, FitnessLevel.BEGINNER, TrainingLocation.GYM, dias(1, 3, 5), (short) 45, false));
            l.add(new Caso(id(n++), o, FitnessLevel.BEGINNER, TrainingLocation.HOME, dias(1, 2, 4, 5), (short) 60, false));
            l.add(new Caso(id(n++), o, FitnessLevel.INTERMEDIATE, TrainingLocation.GYM, dias(1, 2, 4, 5), (short) 60, false));
            l.add(new Caso(id(n++), o, FitnessLevel.INTERMEDIATE, TrainingLocation.HOME, dias(1, 3, 5), (short) 45, false));
            l.add(new Caso(id(n++), o, FitnessLevel.INTERMEDIATE, TrainingLocation.GYM, dias(1, 2, 3, 5, 6), (short) 60, true));
            l.add(new Caso(id(n++), o, FitnessLevel.BEGINNER, TrainingLocation.GYM, dias(1, 4, 5, 7), (short) 60, false));
        }
        if (CASOS.isBlank()) return l;
        var elegidos = Set.of(CASOS.split(","));
        return l.stream().filter(c -> elegidos.contains(c.id())).toList();
    }

    private static String id(int n) { return "G%02d".formatted(n); }

    private static List<Short> dias(int... d) {
        return Arrays.stream(d).mapToObj(x -> (short) x).toList();
    }

    // ---------------------------------------------------------------------------------------------------------
    record Prediccion(String caso, String brazo, long usuario, int intento, String promptVersion, long ms,
                      String error, boolean simulada) {}

    record Resultado(Caso caso, String brazo, int orden, long usuario, String resultado, String origen,
                     int predicciones, boolean validaPrimerIntento, int rechazosV15, String rechazosOtros,
                     long msTotal, Long planId, String validacionFinal, String promptVersion, String detalle) {}

    final List<Prediccion> predicciones = Collections.synchronizedList(new ArrayList<>());
    final List<String> tiempos = Collections.synchronizedList(new ArrayList<>());
    /** Rechazos por usuario e intento, tal como los registra el pipeline. */
    final Map<Long, List<List<String>>> rechazos = Collections.synchronizedMap(new HashMap<>());
    final AtomicInteger contador = new AtomicInteger();
    volatile String corte;
    volatile String casoActual = "-", brazoActual = "-";

    @Test
    void evaluacionPareada() throws Exception {
        assumeTrue(MODO.equals("ENSAYO") || REAL, "opt-in: -Dfitsense.h2.pareada.modo=ENSAYO|REAL");
        if (REAL) {
            assertEquals("SI", System.getProperty("fitsense.h2.pareada.autorizado"),
                    "REAL exige -Dfitsense.h2.pareada.autorizado=SI (autorizacion expresa del propietario)");
            assertTrue(TOPE_USD > 0, "REAL exige -Dfitsense.h2.pareada.tope-usd");
        }
        Files.createDirectories(SALIDA);
        espiar();
        var resultados = new ArrayList<Resultado>();
        var lista = casos();
        for (int i = 0; i < lista.size(); i++) {
            var c = lista.get(i);
            var orden = i % 2 == 0 ? List.of("A", "B") : List.of("B", "A");
            // Parejas completas: no se empieza una pareja si no caben sus 6 predicciones de peor caso (2 x 3).
            if (corte == null && !ControlPresupuestoK20.puedeLlamar(contador.get() + 2 * INTENTOS, MAX_PREDICCIONES, 0,
                    RESERVA_USD, REAL ? TOPE_USD : Double.MAX_VALUE, null))
                corte = "no cabe la pareja " + c.id() + " (" + (contador.get() + 2 * INTENTOS) + " > max "
                        + MAX_PREDICCIONES + " o tope " + TOPE_USD + " USD con reserva " + RESERVA_USD + ")";
            for (int k = 0; k < 2; k++) {
                if (corte != null) {
                    resultados.add(new Resultado(c, orden.get(k), k + 1, -1, "NO_EJECUTADO", "-", 0, false, 0, "-",
                            0, null, "-", "-", corte));
                    continue;
                }
                resultados.add(ejecutar(c, orden.get(k), k + 1));
            }
        }
        var metricas = REAL ? metricasDeReplicate() : List.<String[]>of();
        escribir(resultados, metricas);
        // El arnes nunca deja un plan publicado que el validador independiente rechace.
        for (var r : resultados)
            if ("PUBLICADO".equals(r.resultado()))
                assertEquals("VALIDA", r.validacionFinal(), r.caso().id() + " " + r.brazo() + ": " + r.detalle());
    }

    private Resultado ejecutar(Caso c, String brazo, int orden) {
        boolean contrato = brazo.equals("B");
        // Mismo interruptor en las dos piezas que lo leen en la generacion: prompt e instantanea persistida.
        ReflectionTestUtils.invokeMethod((Object) AopTestUtils.getUltimateTargetObject(promptBuilder), "setContratoV15",
                contrato);
        ReflectionTestUtils.setField((Object) AopTestUtils.getUltimateTargetObject(planComandos), "contratoV15", contrato);
        casoActual = c.id();
        brazoActual = brazo;
        long u = participante(c, brazo);
        int antes = contador.get();
        String corteAntes = corte;
        long t0 = System.currentTimeMillis();
        String resultado, origen = "-", detalle = "";
        Long planId = null;
        try {
            var plan = generacionInicial.generar(GenerateWeeklyPlanCommand.firstPlan(u, SEMANA));
            if (plan.isPresent()) {
                planId = plan.get().getId();
                origen = jdbc.queryForObject("SELECT generation_source FROM weekly_training_plans WHERE plan_id = ?",
                        String.class, planId);
                resultado = "PUBLICADO";
            } else {
                resultado = "SIN_PLAN";
            }
        } catch (RuntimeException e) {
            // NO_FACTIBLE de la generacion inicial: las excepciones de dominio que dicen que no hay semana publicable.
            resultado = NO_FACTIBLE.contains(e.getClass().getSimpleName()) ? "NO_FACTIBLE" : "ERROR";
            detalle = e.getClass().getSimpleName() + ": " + e.getMessage();
        }
        long ms = System.currentTimeMillis() - t0;
        if (corteAntes == null && corte != null) {
            resultado = "CORTE_PRESUPUESTO";
            detalle = corte;
        }
        int n = contador.get() - antes;
        var porIntento = rechazos.getOrDefault(u, List.of());
        int v15 = (int) porIntento.stream().flatMap(List::stream).filter(p -> p.startsWith("V15")).count();
        var otros = porIntento.stream().flatMap(List::stream).map(EvaluacionPareadaH2IT::codigo)
                .filter(x -> !x.equals("V15")).collect(Collectors.toCollection(TreeSet::new)).toString();
        boolean primero = n == 1 && "AI".equals(origen) && porIntento.isEmpty();
        String validacion = planId == null ? "-" : validar(u, planId, origen);
        String version = planId == null ? "-" : jdbc.queryForObject(
                "SELECT input_snapshot->>'prompt_version' FROM weekly_training_plans WHERE plan_id = ?", String.class,
                planId);
        return new Resultado(c, brazo, orden, u, resultado, origen, n, primero, v15, otros, ms, planId, validacion,
                version, detalle);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Espias: copian lo que entra y sale; en ENSAYO sustituyen al proveedor (SIMULADO); en ambos aplican el tope.
    // ---------------------------------------------------------------------------------------------------------
    private void espiar() {
        doAnswer(inv -> {
            var ctx = (PlanGenerationContext) inv.getArgument(0);
            int intento = inv.getArgument(2);
            int siguiente = contador.get() + 1;
            if (corte == null && !ControlPresupuestoK20.puedeLlamar(siguiente, MAX_PREDICCIONES, 0, RESERVA_USD,
                    REAL ? TOPE_USD : Double.MAX_VALUE, null))
                corte = "tope alcanzado antes de la prediccion " + siguiente + " (max " + MAX_PREDICCIONES
                        + ", reserva " + RESERVA_USD + " USD, tope " + TOPE_USD + " USD)";
            if (corte != null) throw new IllegalStateException("CORTE_PRESUPUESTO: " + corte);
            contador.incrementAndGet();
            long t0 = System.nanoTime();
            try {
                String salida = REAL ? (String) inv.callRealMethod()
                        : "{\"simulado\":\"ENSAYO sin Replicate: respuesta invalida a proposito\"}";
                predicciones.add(new Prediccion(casoActual, brazoActual, ctx.userId(), intento,
                        promptBuilder.version(), (System.nanoTime() - t0) / 1_000_000, null, !REAL));
                guardar("salida_" + casoActual + "_" + brazoActual + "_" + intento + ".json", salida);
                return salida;
            } catch (Throwable e) {
                predicciones.add(new Prediccion(casoActual, brazoActual, ctx.userId(), intento,
                        promptBuilder.version(), (System.nanoTime() - t0) / 1_000_000, e.toString(), !REAL));
                throw e;
            }
        }).when(generador).requestOutput(any(), any(), anyInt());

        var rechazo = Pattern.compile("Intento (\\d+) de IA rechazado para el usuario (\\d+): \\[(.*)]$",
                Pattern.DOTALL);
        var appender = new AppenderBase<ILoggingEvent>() {
            @Override
            protected void append(ILoggingEvent e) {
                String t = e.getFormattedMessage();
                if (t.startsWith("REPLICATE_TIMING") || t.startsWith("GENERACION_TIEMPOS")) tiempos.add(t);
                var m = rechazo.matcher(t);
                if (m.find())
                    rechazos.computeIfAbsent(Long.parseLong(m.group(2)), k -> Collections.synchronizedList(
                            new ArrayList<>())).add(List.of(m.group(3).split(", (?=V\\d|CONTRATO|R-)")));
            }
        };
        appender.start();
        for (var c : List.of(ReplicateTrainingPlanGenerator.class, PlanGenerationPipeline.class))
            ((Logger) LoggerFactory.getLogger(c)).addAppender(appender);
    }

    private static final Pattern CODIGO = Pattern.compile("^(V\\d+[A-Z]?)\\b");

    private static String codigo(String problema) {
        var m = CODIGO.matcher(problema.strip());
        return m.find() ? m.group(1) : "CONTRATO/OTRO";
    }

    // ---------------------------------------------------------------------------------------------------------
    /** Validador independiente sobre lo PUBLICADO (lee la base, no la respuesta de la IA). */
    private String validar(long u, long plan, String origen) {
        var borrador = new ArrayList<PlanDraft.DraftWorkout>();
        for (var f : jdbc.queryForList("SELECT planned_workout_id, scheduled_date, focus_code, workout_name,"
                + " expected_duration_minutes FROM planned_workouts WHERE plan_id = ? ORDER BY scheduled_date", plan)) {
            long id = ((Number) f.get("planned_workout_id")).longValue();
            var ej = jdbc.query("SELECT exercise_id, prescription_type, planned_sets, planned_reps,"
                    + " planned_duration_seconds, target_load_kg, rest_seconds FROM planned_workout_exercises"
                    + " WHERE planned_workout_id = ? ORDER BY exercise_order", (rs, i) -> new PlanDraft.DraftExercise(
                    rs.getLong(1), PrescriptionType.valueOf(rs.getString(2)), (Short) rs.getObject(3, Short.class),
                    (Short) rs.getObject(4, Short.class), (Integer) rs.getObject(5, Integer.class),
                    rs.getBigDecimal(6), (Short) rs.getObject(7, Short.class), null), id);
            borrador.add(new PlanDraft.DraftWorkout(((java.sql.Date) f.get("scheduled_date")).toLocalDate(),
                    WorkoutFocus.valueOf((String) f.get("focus_code")), (String) f.get("workout_name"),
                    ((Number) f.get("expected_duration_minutes")).intValue(), ej));
        }
        var ctx = planComandos.prepare(new GenerateWeeklyPlanCommand(u, SEMANA, null, true, null, null)).context();
        try {
            validador.validate(new PlanDraft(GenerationSource.valueOf(origen), null, "pareada", null, "r", borrador),
                    ctx, 30);
            return "VALIDA";
        } catch (InvalidPlanDraftException e) {
            return "INVALIDA " + e.problems().stream().map(EvaluacionPareadaH2IT::codigo)
                    .collect(Collectors.toCollection(TreeSet::new));
        }
    }

    private long participante(Caso c, String brazo) {
        String email = ("h2-" + MODO + "-" + c.id() + "-" + brazo + "@pruebas.invalid").toLowerCase();
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

    // ---------------------------------------------------------------------------------------------------------
    private List<String[]> metricasDeReplicate() {
        var ids = new LinkedHashSet<String>();
        var m = Pattern.compile("prediction_id=(\\S+)");
        for (var t : List.copyOf(tiempos)) {
            var x = m.matcher(t);
            if (x.find() && !x.group(1).equals("desconocido")) ids.add(x.group(1));
        }
        var http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
        var json = new ObjectMapper();
        var token = System.getProperty("fitsense.h2.pareada.token", "proxy");
        var lista = new ArrayList<String[]>();
        for (var id : ids) {
            try {
                var r = http.send(HttpRequest.newBuilder(URI.create("https://api.replicate.com/v1/predictions/" + id))
                        .header("Authorization", "Bearer " + token).timeout(Duration.ofSeconds(30)).GET().build(),
                        HttpResponse.BodyHandlers.ofString());
                var p = json.readTree(r.body());
                var mt = p.path("metrics");
                long ent = mt.path("input_token_count").asLong(mt.path("token_input_count").asLong());
                long sal = mt.path("output_token_count").asLong(mt.path("token_output_count").asLong());
                double usd = ent / 1e6 * USD_ENTRADA + sal / 1e6 * USD_SALIDA;
                lista.add(new String[]{id, p.path("status").asText(), String.valueOf(ent), String.valueOf(sal),
                        String.valueOf(mt.path("predict_time").asDouble()), "%.5f".formatted(usd)});
            } catch (Exception e) {
                lista.add(new String[]{id, "sin metricas: " + e.getClass().getSimpleName(), "", "", "", ""});
            }
        }
        return lista;
    }

    private void guardar(String nombre, String contenido) {
        try {
            Files.createDirectories(SALIDA.resolve("salidas"));
            Files.writeString(SALIDA.resolve("salidas").resolve(nombre), contenido == null ? "" : contenido);
        } catch (Exception ignored) {
        }
    }

    private void escribir(List<Resultado> resultados, List<String[]> metricas) throws Exception {
        String etiqueta = REAL ? "REAL" : "SIMULADO";
        var sb = new StringBuilder("modo;caso;objetivo;nivel;lugar;dias;minutos;restringido;brazo;orden;usuario;"
                + "resultado;origen;predicciones;valida_primer_intento;rechazos_v15;rechazos_otros;ms_total;plan_id;"
                + "validacion_final;prompt_version;detalle\n");
        for (var r : resultados)
            sb.append(etiqueta).append(';').append(r.caso().id()).append(';').append(r.caso().objetivo()).append(';')
                    .append(r.caso().nivel()).append(';').append(r.caso().lugar()).append(';')
                    .append(r.caso().dias().size()).append(';').append(r.caso().minutos()).append(';')
                    .append(r.caso().restringido()).append(';').append(r.brazo()).append(';').append(r.orden())
                    .append(';').append(r.usuario()).append(';').append(r.resultado()).append(';').append(r.origen())
                    .append(';').append(r.predicciones()).append(';').append(r.validaPrimerIntento()).append(';')
                    .append(r.rechazosV15()).append(';').append(r.rechazosOtros()).append(';').append(r.msTotal())
                    .append(';').append(r.planId()).append(';').append(r.validacionFinal()).append(';')
                    .append(r.promptVersion()).append(';').append(r.detalle().replace(';', ',').replace('\n', ' '))
                    .append('\n');
        Files.writeString(SALIDA.resolve("casos.csv"), sb.toString());

        var p = new StringBuilder("modo;caso;brazo;usuario;intento;prompt_version;ms;error\n");
        for (var x : List.copyOf(predicciones))
            p.append(x.simulada() ? "SIMULADO" : "REAL").append(';').append(x.caso()).append(';').append(x.brazo())
                    .append(';').append(x.usuario()).append(';').append(x.intento()).append(';')
                    .append(x.promptVersion()).append(';').append(x.ms()).append(';')
                    .append(x.error() == null ? "" : x.error().replace(';', ',')).append('\n');
        Files.writeString(SALIDA.resolve("predicciones.csv"), p.toString());

        var mt = new StringBuilder("prediction_id;estado;tokens_entrada;tokens_salida;predict_time_s;usd\n");
        for (var x : metricas) mt.append(String.join(";", x)).append('\n');
        Files.writeString(SALIDA.resolve("metricas_replicate.csv"), mt.toString());
        Files.write(SALIDA.resolve("tiempos.log"), List.copyOf(tiempos));

        var res = new StringBuilder("# Evaluacion pareada H2 · resumen (" + etiqueta + ")\n\n");
        if (!REAL) res.append("**ENSAYO SIN REPLICATE: respuestas SIMULADAS. Nada de esto es un resultado de la IA.**\n\n");
        res.append("| Brazo | Casos | Publicados IA | Respaldo | NO_FACTIBLE | Error/corte | Validos 1er intento |"
                + " Predicciones | Rechazos V15 | Validacion final invalida |\n|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|\n");
        for (var b : List.of("A", "B")) {
            var l = resultados.stream().filter(r -> r.brazo().equals(b)).toList();
            res.append("| ").append(b.equals("A") ? "A · PR-1.7" : "B · PR-1.9").append(" | ").append(l.size())
                    .append(" | ").append(l.stream().filter(r -> "AI".equals(r.origen())).count())
                    .append(" | ").append(l.stream().filter(r -> "RULE_ENGINE".equals(r.origen())).count())
                    .append(" | ").append(l.stream().filter(r -> r.resultado().equals("NO_FACTIBLE")).count())
                    .append(" | ").append(l.stream().filter(r -> List.of("ERROR", "CORTE_PRESUPUESTO", "NO_EJECUTADO",
                            "SIN_PLAN").contains(r.resultado())).count())
                    .append(" | ").append(l.stream().filter(Resultado::validaPrimerIntento).count())
                    .append(" | ").append(l.stream().mapToInt(Resultado::predicciones).sum())
                    .append(" | ").append(l.stream().mapToInt(Resultado::rechazosV15).sum())
                    .append(" | ").append(l.stream().filter(r -> r.validacionFinal().startsWith("INVALIDA")).count())
                    .append(" |\n");
        }
        res.append("\nPredicciones: ").append(contador.get()).append(" · tope: ")
                .append(REAL ? TOPE_USD + " USD" : "no aplica (ENSAYO)").append(" · corte: ")
                .append(corte == null ? "no" : corte).append('\n');
        if (REAL) {
            double usd = metricas.stream().filter(x -> !x[5].isEmpty()).mapToDouble(x -> Double.parseDouble(x[5]))
                    .sum();
            res.append("Coste medido por tokens (tarifa ").append(USD_ENTRADA).append('/').append(USD_SALIDA)
                    .append(" USD por millon): ").append("%.4f".formatted(usd)).append(" USD\n");
        }
        Files.writeString(SALIDA.resolve("resumen.md"), res.toString());
    }
}
