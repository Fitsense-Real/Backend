package main.web.services.fitsense.orchestation;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import main.web.services.fitsense.execution.domain.model.commands.FinishWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.model.commands.RecordSessionExerciseCommand;
import main.web.services.fitsense.execution.domain.model.commands.StartWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.services.WorkoutSessionCommandService;
import main.web.services.fitsense.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import main.web.services.fitsense.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import main.web.services.fitsense.orchestration.application.internal.WeeklyCycleService;
import main.web.services.fitsense.planning.application.internal.commandservices.AdaptedWeekCommandService;
import main.web.services.fitsense.planning.application.internal.commandservices.PlanGenerationPipeline;
import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.domain.services.PlanDraftValidator;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
import main.web.services.fitsense.planning.infrastructure.generation.ai.AiRetryFeedback;
import main.web.services.fitsense.planning.infrastructure.generation.ai.ReplicateSessionFixGenerator;
import main.web.services.fitsense.planning.infrastructure.generation.ai.ReplicateTrainingPlanGenerator;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;

/**
 * CAMPANA CON REPLICATE REAL (autorizada el 27/10/2026, sin tope fijo de llamadas
 * ni de presupuesto). Llama a Replicate y cuesta dinero: solo con
 * {@code -Dfitsense.replicate.real=true}.
 * <p>
 * Base PostgreSQL efimera (Testcontainers), nunca Neon; participantes
 * sinteticos. Nada simulado en la generacion, la decision, la correccion ni la
 * validacion: la generacion inicial y el ciclo son los de produccion, con la IA
 * real y el maximo de tres intentos del sistema. Los espias solo COPIAN lo que
 * entra y sale de Replicate para la evidencia.
 * <p>
 * Dos modos, uno por ejecucion:
 * <ul>
 *   <li>{@code -Dfitsense.replicate.progresion=false} (por omision): la
 *       configuracion de produccion. Guion de adaptaciones: fatiga, tiempo,
 *       carga, dificultad, baja motivacion (dos sesiones), dolor.</li>
 *   <li>{@code -Dfitsense.replicate.progresion=true}: progresion encendida SOLO
 *       en este contexto aislado; el guion de diez semanas de la evaluacion
 *       limpia, con fatiga y acortamiento reales.</li>
 * </ul>
 * Comprobaciones independientes de los validadores, en cada ciclo: la IA no sube
 * ninguna dosis publicada; las sesiones no senaladas salen identicas (salvo un
 * paso de progresion o una sustitucion por seguridad declarados); ai_calls de
 * V32 coincide con las llamadas contadas; nunca mas de tres intentos; los ciclos
 * deterministas no llaman a la IA; los pasos respetan 5 %, 10 % y el techo; y la
 * consulta de linaje V31/V32 no encuentra violaciones.
 * <p>
 * Parada: cinco fallos seguidos del proveedor, o mas de tres llamadas en un
 * ciclo. Informe en {@code target/replicate-real/<modo>/}.
 */
@Testcontainers
@EnabledIfSystemProperty(named = "fitsense.replicate.real", matches = "true")
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=true",
        // El proxy del entorno pone el token real; el backend solo exige que no este vacio.
        "fitsense.ai.api-token=proxy",
        "fitsense.ai.model=gpt-5-mini",
        "fitsense.ai.reasoning-effort=medium",
        // El proxy de este entorno corta a los 30 s las peticiones abiertas: el POST
        // vuelve a los 20 s con la prediccion en curso y el sondeo de siempre la recoge.
        "fitsense.ai.prefer-wait-seconds=${fitsense.replicate.espera:20}",
        "fitsense.adaptation.progression.enabled=${fitsense.replicate.progresion:false}"
})
@DisplayName("Campana con Replicate real · generacion y adaptacion en los cuatro objetivos")
class CampanaReplicateRealIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @MockitoSpyBean ReplicateSessionFixGenerator correcciones;
    @MockitoSpyBean ReplicateTrainingPlanGenerator generador;

    @Autowired WeeklyCycleService ciclo;
    @Autowired PlanningContextFacade planning;
    @Autowired WeeklyTrainingPlanCommandService planComandos;
    @Autowired PlanDraftValidator validador;
    @Autowired UserProfileCommandService perfiles;
    @Autowired WorkoutSessionCommandService sesiones;
    @Autowired TrainingPlanController planes;
    @Autowired UserRepository usuarios;
    @Autowired JdbcTemplate jdbc;

    static final boolean PROGRESION = Boolean.getBoolean("fitsense.replicate.progresion");
    static final String MODO = PROGRESION ? "progresion" : "adaptacion";
    static final Path SALIDA = Path.of("target", "replicate-real", MODO);
    static final int HILOS = Integer.getInteger("fitsense.replicate.hilos", 4);
    static final double USD_ENTRADA = Double.parseDouble(System.getProperty("fitsense.replicate.usd.entrada", "0.25"));
    static final double USD_SALIDA = Double.parseDouble(System.getProperty("fitsense.replicate.usd.salida", "2.00"));

    static final LocalDate S1 = TrainingWeek.containing(LocalDate.now()).startDate().plusWeeks(1);
    static final List<String> GIMNASIO = List.of("body weight", "dumbbell", "barbell", "cable", "leverage machine",
            "bench", "band", "stationary bike", "treadmill", "kettlebell", "ez barbell", "smith machine");
    static final List<String> CASA = List.of("body weight", "dumbbell", "band");

    enum Dia { HECHO, FATIGA, TIEMPO, PARCIAL_CARGA, DIFICIL, MOTIVACION, DOLOR }

    static final Dia H = Dia.HECHO;

    /** Guion sin progresion: cada fila es la semana medida (lunes, miercoles, viernes). */
    static final List<Dia[]> GUION_ADAPTACION = List.of(
            new Dia[]{H, H, Dia.FATIGA},                 // REVISAR_CARGA_Y_RECUPERACION
            new Dia[]{H, H, H},                          // copia
            new Dia[]{H, H, Dia.TIEMPO},                 // ACORTAR_O_REORGANIZAR
            new Dia[]{H, H, H},
            new Dia[]{H, H, Dia.PARCIAL_CARGA},          // REVISAR_CARGA con 20 kg prescritos
            new Dia[]{H, Dia.DIFICIL, H},                // VARIANTE_MAS_SENCILLA_O_CARGA
            new Dia[]{H, Dia.MOTIVACION, Dia.MOTIVACION},// INVESTIGAR_CAMBIO_MINIMO, dos senaladas
            new Dia[]{H, H, Dia.DOLOR},                  // SEGURIDAD_ANTE_DOLOR
            new Dia[]{H, H, H});

    /** Guion con progresion: el de la evaluacion limpia (fatiga en la 4, tiempo en la 6). */
    static final List<Dia[]> GUION_PROGRESION = List.of(
            new Dia[]{H, H, H}, new Dia[]{H, H, H}, new Dia[]{H, H, H},
            new Dia[]{H, H, Dia.FATIGA}, new Dia[]{H, H, H}, new Dia[]{H, H, Dia.TIEMPO},
            new Dia[]{H, H, H}, new Dia[]{H, H, H}, new Dia[]{H, H, H}, new Dia[]{H, H, H});

    // -----------------------------------------------------------------------
    // Registro de lo que entra y sale de Replicate
    // -----------------------------------------------------------------------

    /** Etiqueta del hilo: perfil y ciclo en curso. Cada perfil corre entero en un hilo. */
    static final ThreadLocal<String> ETIQUETA = new ThreadLocal<>();

    record Llamada(String etiqueta, String tipo, long usuario, int intento, long ms, List<String> problemasPrevios,
                   String salida, String error) {}

    record Evento(String etiqueta, long usuario, String nivel, String texto) {}

    final Queue<Llamada> llamadas = new ConcurrentLinkedQueue<>();
    final Queue<Evento> eventos = new ConcurrentLinkedQueue<>();
    final Queue<String> tiempos = new ConcurrentLinkedQueue<>();
    final AtomicInteger fallosSeguidos = new AtomicInteger();
    final AtomicBoolean abortar = new AtomicBoolean();
    volatile String motivoParada;

    // -----------------------------------------------------------------------
    // Resultado de cada ciclo
    // -----------------------------------------------------------------------

    record Ciclo(String perfil, GoalType objetivo, int semana, String guion, String decision, String status,
                 String intervenciones, int aiCallsV32, int llamadasContadas, String tipoPaso, String motivo,
                 List<String> violaciones, String antes, String despues, List<String> observaciones) {}

    /** Hallazgos del piloto que se cuantifican sin tratarlos como violacion (no hay regla que los prohiba). */
    static final String SOLO_MINUTOS = "APLICADA_SOLO_POR_MINUTOS";
    static final String ANADE_MAS = "ANADE_MAS_DE_LO_QUE_RETIRA";

    record Inicial(String perfil, GoalType objetivo, String origen, int intentos, String validacion,
                   List<String> problemas) {}

    record Perfil(String nombre, GoalType objetivo, Inicial inicial, List<Ciclo> ciclos, String corte) {}

    @Test
    void campana() throws Exception {
        Files.createDirectories(SALIDA);
        espiar();
        long inicio = System.currentTimeMillis();

        var lista = definirPerfiles();
        var pool = Executors.newFixedThreadPool(HILOS);
        var futuros = new ArrayList<Future<Perfil>>();
        for (int i = 0; i < lista.size(); i++) {
            int n = i + 1;
            var p = lista.get(i);
            futuros.add(pool.submit(() -> recorrer(n, p)));
        }
        var resultados = new ArrayList<Perfil>();
        for (var f : futuros) resultados.add(f.get(6, TimeUnit.HOURS));
        pool.shutdown();

        var metricas = metricasDeReplicate();
        escribir(resultados, metricas, System.currentTimeMillis() - inicio);
    }

    // =======================================================================
    // Espias (copian; no cambian nada) y captura del log
    // =======================================================================

    private void espiar() {
        doAnswer(inv -> registrar("ADAPTACION", ((PlanGenerationContext) inv.getArgument(0)).userId(),
                inv.getArgument(4), inv.getArgument(3), () -> {
                    var r = (ReplicateSessionFixGenerator.RawFix) inv.callRealMethod();
                    return new Object[]{r, r.output()};
                })).when(correcciones).pedirRespuesta(any(), any(), any(), any(), anyInt());

        doAnswer(inv -> registrar("GENERACION", ((PlanGenerationContext) inv.getArgument(0)).userId(),
                inv.getArgument(2), inv.getArgument(1), () -> {
                    var r = (String) inv.callRealMethod();
                    return new Object[]{r, r};
                })).when(generador).requestOutput(any(), any(), anyInt());

        var appender = new AppenderBase<ILoggingEvent>() {
            @Override
            protected void append(ILoggingEvent e) {
                String t = e.getFormattedMessage();
                if (t.startsWith("REPLICATE_TIMING")) {
                    tiempos.add(t);
                    if (t.contains("resultado=SUCCEEDED")) fallosSeguidos.set(0);
                    else if (fallosSeguidos.incrementAndGet() >= 5) parar("cinco fallos seguidos del proveedor: " + t);
                }
                if (e.getLevel().isGreaterOrEqual(ch.qos.logback.classic.Level.WARN) || t.startsWith("PROGRESION")
                        || t.startsWith("Adaptacion del usuario"))
                    eventos.add(new Evento(ETIQUETA.get(), usuarioDe(t), e.getLevel().toString(), t));
            }
        };
        appender.start();
        for (var c : List.of(ReplicateTrainingPlanGenerator.class, PlanGenerationPipeline.class,
                AdaptedWeekCommandService.class))
            ((Logger) LoggerFactory.getLogger(c)).addAppender(appender);
    }

    @FunctionalInterface
    interface Llamar { Object[] llamar() throws Throwable; }

    private Object registrar(String tipo, Long usuario, Object intento, Object feedback, Llamar real) throws Throwable {
        if (abortar.get()) throw new IllegalStateException("campana detenida: " + motivoParada);
        var previos = feedback instanceof AiRetryFeedback f ? f.rejectionProblems() : List.<String>of();
        long t0 = System.nanoTime();
        try {
            var r = real.llamar();
            llamadas.add(new Llamada(ETIQUETA.get(), tipo, usuario, (Integer) intento,
                    (System.nanoTime() - t0) / 1_000_000, previos, (String) r[1], null));
            return r[0];
        } catch (Throwable e) {
            llamadas.add(new Llamada(ETIQUETA.get(), tipo, usuario, (Integer) intento,
                    (System.nanoTime() - t0) / 1_000_000, previos, null, e.toString()));
            throw e;
        }
    }

    private void parar(String motivo) {
        if (abortar.compareAndSet(false, true)) motivoParada = motivo;
    }

    private static final Pattern USUARIO = Pattern.compile("(?:usuario |user=)(\\d+)");

    private static long usuarioDe(String t) {
        var m = USUARIO.matcher(t);
        return m.find() ? Long.parseLong(m.group(1)) : -1;
    }

    // =======================================================================
    // Perfiles y recorrido
    // =======================================================================

    record Def(GoalType objetivo, FitnessLevel nivel, int minutos, String lugar) {
        String nombre() { return objetivo + "/" + nivel + "/" + minutos + "/" + lugar; }
    }

    /** Por omision, los casos mas restrictivos de cada objetivo. {@code -Dfitsense.replicate.perfiles} los cambia. */
    private static List<Def> definirPerfiles() {
        var texto = System.getProperty("fitsense.replicate.perfiles", "");
        if (!texto.isBlank())
            return Arrays.stream(texto.split(",")).map(String::strip).map(s -> s.split("/"))
                    .map(p -> new Def(GoalType.valueOf(p[0]), FitnessLevel.valueOf(p[1]), Integer.parseInt(p[2]), p[3]))
                    .toList();
        var l = new ArrayList<Def>();
        for (var objetivo : GoalType.values()) {
            l.add(new Def(objetivo, FitnessLevel.BEGINNER, 30, "CASA"));
            l.add(new Def(objetivo, FitnessLevel.INTERMEDIATE, 45, "GYM"));
            l.add(new Def(objetivo, FitnessLevel.ADVANCED, 60, "GYM"));
        }
        return l;
    }

    private Perfil recorrer(int n, Def d) {
        String nombre = d.nombre();
        var ciclos = new ArrayList<Ciclo>();
        long u = participante(n, d);
        ETIQUETA.set(nombre + " · inicial");
        int antesInicial = contar(u);
        Optional<Long> plan;
        try {
            plan = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null);
        } catch (RuntimeException e) {
            return new Perfil(nombre, d.objetivo(), new Inicial(nombre, d.objetivo(), "SIN_PLAN", contar(u) - antesInicial,
                    e.getClass().getSimpleName() + ": " + e.getMessage(), List.of()), ciclos, "sin plan inicial");
        }
        if (plan.isEmpty())
            return new Perfil(nombre, d.objetivo(), new Inicial(nombre, d.objetivo(), "SIN_PLAN", 0, "-", List.of()),
                    ciclos, "sin plan inicial");
        var inicial = inicial(nombre, d.objetivo(), u, plan.get(), contar(u) - antesInicial);

        var guion = PROGRESION ? GUION_PROGRESION : GUION_ADAPTACION;
        for (int k = 0; k < guion.size(); k++) {
            if (abortar.get()) return new Perfil(nombre, d.objetivo(), inicial, ciclos, "campana detenida: " + motivoParada);
            var dias = guion.get(k);
            var semana = S1.plusWeeks(k);
            ETIQUETA.set(nombre + " · s" + (k + 2));
            if (Arrays.asList(dias).contains(Dia.PARCIAL_CARGA)) cargaPrescrita(plan.get());
            var medidas = sesionesDe(plan.get());
            ejecutar(u, plan.get(), dias);
            int antes = contar(u);
            int filasAntes = filasV32(u);
            Optional<Long> siguiente;
            try {
                siguiente = ciclo.runFor(u, semana, semana.plusWeeks(1));
            } catch (RuntimeException e) {
                ciclos.add(new Ciclo(nombre, d.objetivo(), k + 2, describir(dias), "-", "EXCEPCION", "-", 0,
                        contar(u) - antes, null, e.toString(), List.of(), null, null, List.of()));
                return new Perfil(nombre, d.objetivo(), inicial, ciclos, "excepcion en la semana " + (k + 2));
            }
            int contadas = contar(u) - antes;
            if (contadas > 3) parar("mas de tres llamadas en un ciclo (" + nombre + ", s" + (k + 2) + ")");
            if (filasV32(u) == filasAntes) {
                // El ciclo no dejo evidencia V32 (fallo al publicar): no se lee la fila de la semana anterior.
                ciclos.add(new Ciclo(nombre, d.objetivo(), k + 2, describir(dias), "-", "SIN_EVIDENCIA", "-", 0,
                        contadas, null, "el ciclo no registro intento V32" + (siguiente.isEmpty() ? " ni publico semana" : ""),
                        List.of("ciclo sin evidencia V32 con " + contadas + " llamadas a la IA"), null, null, List.of()));
                if (siguiente.isEmpty())
                    return new Perfil(nombre, d.objetivo(), inicial, ciclos, "sin semana publicada en la " + (k + 2));
                plan = siguiente;
                continue;
            }
            ciclos.add(evaluarCiclo(nombre, d.objetivo(), k + 2, dias, u, medidas, siguiente, contadas));
            if (siguiente.isEmpty())
                return new Perfil(nombre, d.objetivo(), inicial, ciclos, "sin semana publicada en la " + (k + 2));
            plan = siguiente;
        }
        return new Perfil(nombre, d.objetivo(), inicial, ciclos, null);
    }

    private int filasV32(long u) {
        return jdbc.queryForObject("SELECT count(*) FROM plan_adaptation_attempts WHERE user_id = ?", Integer.class, u);
    }

    private int contar(long u) {
        return (int) llamadas.stream().filter(l -> l.usuario() == u).count();
    }

    // =======================================================================
    // Evaluacion de cada ciclo, independiente de los validadores
    // =======================================================================

    record Ej(long id, String tipo, Short series, Short reps, Integer segundos, Short descanso, BigDecimal carga) {
        String dosis() {
            return id + ":" + (series == null ? "-" : series) + "x" + (reps != null ? reps + "r" : segundos + "s")
                    + "/" + descanso + "s" + (carga == null ? "" : "@" + carga.stripTrailingZeros().toPlainString() + "kg");
        }
    }

    record Ses(long id, Long origen, LocalDate fecha, int minutos, List<Ej> ejercicios) {}

    private Ciclo evaluarCiclo(String nombre, GoalType objetivo, int semana, Dia[] dias, long u, List<Ses> medidas,
                               Optional<Long> publicado, int contadas) {
        var ev = jdbc.queryForMap("SELECT decision, status, ai_calls, targeted_sessions::text AS senaladas,"
                + " targeted_interventions::text AS intervenciones, progressed_sessions::text AS progresadas,"
                + " conservation_report::text AS informe, rejection_reason FROM plan_adaptation_attempts"
                + " WHERE user_id = ? ORDER BY attempt_id DESC LIMIT 1", u);
        String informe = String.valueOf(ev.get("informe"));
        var violaciones = new ArrayList<String>();
        int aiCalls = ((Number) ev.get("ai_calls")).intValue();
        String decision = (String) ev.get("decision");
        boolean determinista = "NO_CHANGE".equals(decision) || "INSUFFICIENT_DATA".equals(decision);

        if (aiCalls != contadas) violaciones.add("ai_calls V32 = " + aiCalls + ", llamadas contadas = " + contadas);
        if (contadas > 3) violaciones.add("mas de tres llamadas en el ciclo: " + contadas);
        if (determinista && contadas > 0) violaciones.add("ciclo determinista con " + contadas + " llamadas a la IA");

        var senaladas = ids((String) ev.get("senaladas"));
        var progresadas = ids((String) ev.get("progresadas"));
        var sustituidas = new HashSet<Long>();
        var m = Pattern.compile("\"sesion_origen\"\\s*:\\s*(\\d+)").matcher(informe);
        while (m.find()) sustituidas.add(Long.parseLong(m.group(1)));

        String antes = null, despues = null;
        var observaciones = new ArrayList<String>();
        if (publicado.isPresent()) {
            var nuevas = sesionesDe(publicado.get()).stream().filter(s -> s.origen() != null)
                    .collect(Collectors.toMap(Ses::origen, s -> s, (a, b) -> a));
            var a = new StringBuilder();
            var b = new StringBuilder();
            for (var s : medidas) {
                var n = nuevas.get(s.id());
                if (n == null) {
                    violaciones.add("la sesion " + s.id() + " no tiene derivada con linaje");
                    continue;
                }
                if (senaladas.contains(s.id())) {
                    a.append(s.id()).append(" ").append(s.minutos()).append("min ")
                            .append(s.ejercicios().stream().map(Ej::dosis).toList()).append(" ");
                    b.append(n.id()).append(" ").append(n.minutos()).append("min ")
                            .append(n.ejercicios().stream().map(Ej::dosis).toList()).append(" ");
                    subidas(s, n, violaciones);
                    var idsAntes = s.ejercicios().stream().map(Ej::id).collect(Collectors.toSet());
                    var idsDespues = n.ejercicios().stream().map(Ej::id).collect(Collectors.toSet());
                    long anadidos = idsDespues.stream().filter(x -> !idsAntes.contains(x)).count();
                    long quitados = idsAntes.stream().filter(x -> !idsDespues.contains(x)).count();
                    // Correcciones 4 y 5 (27/10/2026): ya son reglas, asi que cuentan como violacion.
                    if (anadidos > quitados) {
                        observaciones.add(ANADE_MAS + " sesion " + s.id() + ": anade " + anadidos + ", retira " + quitados);
                        if (String.valueOf(ev.get("intervenciones")).contains("SEGURIDAD_ANTE_DOLOR"))
                            violaciones.add("ante dolor anade " + anadidos + " y retira " + quitados + " en la sesion " + s.id());
                    }
                    if (String.valueOf(ev.get("intervenciones")).contains("SEGURIDAD_ANTE_DOLOR")
                            && volumen(n.ejercicios()) > volumen(s.ejercicios()))
                        violaciones.add("ante dolor el volumen sube de " + volumen(s.ejercicios()) + " a "
                                + volumen(n.ejercicios()) + " en la sesion " + s.id());
                    if ("APLICADA".equals(ev.get("status"))
                            && s.ejercicios().stream().map(Ej::dosis).toList()
                            .equals(n.ejercicios().stream().map(Ej::dosis).toList())
                            && s.fecha().plusWeeks(1).equals(n.fecha()) && s.minutos() != n.minutos()) {
                        observaciones.add(SOLO_MINUTOS + " sesion " + s.id() + ": " + s.minutos() + " -> " + n.minutos() + " min");
                        violaciones.add("APLICADA solo por minutos en la sesion " + s.id());
                    }
                } else if (!progresadas.contains(s.id()) && !sustituidas.contains(s.id())
                        && !identicas(s, n)) {
                    violaciones.add("la sesion NO senalada " + s.id() + " cambio: " + s.ejercicios().stream()
                            .map(Ej::dosis).toList() + " -> " + n.ejercicios().stream().map(Ej::dosis).toList());
                }
            }
            antes = a.toString().strip();
            despues = b.toString().strip();
        }
        String tipoPaso = pasos(informe, violaciones);
        return new Ciclo(nombre, objetivo, semana, describir(dias), decision, (String) ev.get("status"),
                (String) ev.get("intervenciones"), aiCalls, contadas, tipoPaso, (String) ev.get("rejection_reason"),
                violaciones, antes, despues, observaciones);
    }

    /** La IA no puede subir ninguna dosis de una sesion senalada (series, repeticiones, segundos, carga). */
    private static void subidas(Ses medida, Ses nueva, List<String> violaciones) {
        var previas = medida.ejercicios().stream().collect(Collectors.toMap(Ej::id, e -> e, (x, y) -> x));
        for (var e : nueva.ejercicios()) {
            var p = previas.get(e.id());
            if (p == null) continue;   // sustitucion o reincorporacion: lo juzga V8C/V25
            if (mayor(e.series(), p.series()) || mayor(e.reps(), p.reps()) || mayor(e.segundos(), p.segundos())
                    || (e.carga() != null && (p.carga() == null || e.carga().compareTo(p.carga()) > 0)))
                violaciones.add("dosis subida en la sesion senalada " + medida.id() + ": " + p.dosis() + " -> " + e.dosis());
        }
    }

    /** El volumen de 18.1 con el divisor por omision (30), el de la configuracion de las pruebas. */
    private static int volumen(List<Ej> l) {
        return l.stream().mapToInt(e -> {
            int series = e.series() == null ? 1 : e.series();
            return "DURATION".equals(e.tipo()) ? Math.round((float) (series * (e.segundos() == null ? 0 : e.segundos())) / 30)
                    : series * (e.reps() == null ? 0 : e.reps());
        }).sum();
    }

    private static boolean mayor(Number a, Number b) {
        return a != null && b != null && a.intValue() > b.intValue();
    }

    private static boolean identicas(Ses a, Ses b) {
        if (a.ejercicios().size() != b.ejercicios().size()) return false;
        for (int i = 0; i < a.ejercicios().size(); i++) {
            var x = a.ejercicios().get(i);
            var y = b.ejercicios().get(i);
            if (x.id() != y.id() || !Objects.equals(x.series(), y.series()) || !Objects.equals(x.reps(), y.reps())
                    || !Objects.equals(x.segundos(), y.segundos()) || !Objects.equals(x.descanso(), y.descanso()))
                return false;
        }
        return true;
    }

    /** Pasos de progresion declarados: 5 % (progresion), 10 % (recuperacion), techo <= 1,10 x base. */
    private static String pasos(String informe, List<String> violaciones) {
        if (informe == null || informe.equals("null")) return null;
        try {
            var raiz = new ObjectMapper().readTree(informe);
            var pasos = raiz.path("pasos");
            String tipo = null;
            for (JsonNode p : pasos) {
                tipo = p.path("tipo").asText();
                int va = p.path("volumen_antes").asInt(), vd = p.path("volumen_despues").asInt();
                int base = p.path("volumen_base").asInt(), techo = p.path("techo").asInt();
                if (techo > Math.floor(base * 1.10)) violaciones.add("techo " + techo + " > 1,10 x " + base);
                if (!tipo.equals("VUELTA_ATRAS") && vd > techo)
                    violaciones.add(tipo + " deja el volumen " + vd + " por encima del techo " + techo);
                if (tipo.equals("PROGRESION") && (vd - va) * 100.0 > 5.0 * va)
                    violaciones.add("PROGRESION de " + va + " a " + vd + " supera el 5 %");
                if (tipo.equals("RECUPERACION") && (vd - va) * 100.0 > 10.0 * va)
                    violaciones.add("RECUPERACION de " + va + " a " + vd + " supera el 10 %");
            }
            return tipo;
        } catch (Exception e) {
            violaciones.add("informe de conservacion ilegible: " + e.getMessage());
            return null;
        }
    }

    private static Set<Long> ids(String arr) {
        var s = new LinkedHashSet<Long>();
        if (arr == null) return s;
        for (var p : arr.replaceAll("[{}\\s]", "").split(",")) if (!p.isEmpty()) s.add(Long.parseLong(p));
        return s;
    }

    private List<Ses> sesionesDe(long plan) {
        return jdbc.query("SELECT planned_workout_id, source_planned_workout_id, scheduled_date,"
                        + " expected_duration_minutes FROM planned_workouts WHERE plan_id = ? ORDER BY scheduled_date",
                (rs, i) -> new Ses(rs.getLong(1), rs.getObject(2, Long.class), rs.getDate(3).toLocalDate(),
                        rs.getInt(4), ejercicios(rs.getLong(1))), plan);
    }

    private List<Ej> ejercicios(long sesion) {
        return jdbc.query("SELECT exercise_id, prescription_type, planned_sets, planned_reps, planned_duration_seconds,"
                        + " rest_seconds, target_load_kg FROM planned_workout_exercises WHERE planned_workout_id = ?"
                        + " ORDER BY exercise_order",
                (rs, i) -> new Ej(rs.getLong(1), rs.getString(2), rs.getObject(3, Short.class),
                        rs.getObject(4, Short.class), rs.getObject(5, Integer.class), rs.getObject(6, Short.class),
                        rs.getBigDecimal(7)), sesion);
    }

    /** La semana inicial con el validador de la generacion (V24 y V25 completas). */
    private Inicial inicial(String nombre, GoalType objetivo, long u, long plan, int intentos) {
        String origen = jdbc.queryForObject("SELECT generation_source FROM weekly_training_plans WHERE plan_id = ?",
                String.class, plan);
        var borrador = new ArrayList<PlanDraft.DraftWorkout>();
        for (var f : jdbc.queryForList("SELECT planned_workout_id, scheduled_date, focus_code, workout_name,"
                + " expected_duration_minutes FROM planned_workouts WHERE plan_id = ? ORDER BY scheduled_date", plan)) {
            long id = ((Number) f.get("planned_workout_id")).longValue();
            var l = ejercicios(id).stream().map(e -> new PlanDraft.DraftExercise(e.id(),
                    PrescriptionType.valueOf(e.tipo()), e.series(), e.reps(), e.segundos(), e.carga(), e.descanso(),
                    null)).toList();
            borrador.add(new PlanDraft.DraftWorkout(((java.sql.Date) f.get("scheduled_date")).toLocalDate(),
                    WorkoutFocus.valueOf((String) f.get("focus_code")), (String) f.get("workout_name"),
                    ((Number) f.get("expected_duration_minutes")).intValue(), l));
        }
        var ctx = planComandos.prepare(new GenerateWeeklyPlanCommand(u, S1, null, true, null, null)).context();
        var problemas = llamadas.stream().filter(x -> x.usuario() == u && x.tipo().equals("GENERACION"))
                .flatMap(x -> x.problemasPrevios().stream()).toList();
        try {
            validador.validate(new PlanDraft(GenerationSource.valueOf(origen), null, "inicial", null, "r", borrador),
                    ctx, 30);
            return new Inicial(nombre, objetivo, origen, intentos, "VALIDA", problemas);
        } catch (InvalidPlanDraftException e) {
            return new Inicial(nombre, objetivo, origen, intentos, "INVALIDA " + codigos(e.problems()), problemas);
        }
    }

    // =======================================================================
    // Material
    // =======================================================================

    private long participante(int n, Def d) {
        String email = "replicate-" + MODO + "-" + n + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Sintetico', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        boolean gym = "GYM".equals(d.lugar());
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1992, 1, 15), BiologicalSex.FEMALE,
                new BigDecimal("165"), new BigDecimal("70"), null, d.nivel(), d.objetivo(), "campana replicate real",
                gym ? TrainingLocation.GYM : TrainingLocation.HOME, (short) 3,
                List.of((short) 1, (short) 3, (short) 5), (short) d.minutos(), gym ? GIMNASIO : CASA,
                List.of(), "Ninguna", true, true, true, true, true));
        return u;
    }

    /** V11: solo hay carga sugerida si se anoto peso. Fija 20 kg en el primer ejercicio por repeticiones del viernes. */
    private void cargaPrescrita(long plan) {
        var viernes = jdbc.queryForList("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ?"
                + " ORDER BY scheduled_date DESC LIMIT 1", Long.class, plan).get(0);
        jdbc.update("UPDATE planned_workout_exercises SET target_load_kg = 20.00 WHERE planned_exercise_id ="
                + " (SELECT planned_exercise_id FROM planned_workout_exercises WHERE planned_workout_id = ?"
                + " AND prescription_type = 'SETS_REPS' ORDER BY exercise_order LIMIT 1)", viernes);
    }

    private void ejecutar(long u, long planId, Dia[] dias) {
        var ids = jdbc.queryForList("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ?"
                + " ORDER BY scheduled_date", Long.class, planId);
        for (int i = 0; i < ids.size(); i++) {
            long w = ids.get(i);
            var dia = i < dias.length ? dias[i] : H;
            switch (dia) {
                case HECHO -> hecha(u, w);
                case FATIGA -> omitir(u, w, SkipReason.FATIGUE);
                case TIEMPO -> omitir(u, w, SkipReason.LACK_OF_TIME);
                case DIFICIL -> omitir(u, w, SkipReason.TOO_DIFFICULT);
                case MOTIVACION -> omitir(u, w, SkipReason.LACK_OF_MOTIVATION);
                case DOLOR -> omitir(u, w, SkipReason.PAIN_OR_DISCOMFORT);
                case PARCIAL_CARGA -> parcialConCarga(u, w);
            }
        }
    }

    private void omitir(long u, long w, SkipReason motivo) {
        planes.skip(UserDetailsImpl.build(usuarios.findById(u).orElseThrow()), w, new SkipWorkoutResource(motivo));
    }

    private void hecha(long u, long w) {
        long s = sesiones.handle(new StartWorkoutSessionCommand(u, w)).orElseThrow().getId();
        for (var f : jdbc.queryForList("SELECT planned_exercise_id, prescription_type, planned_sets, planned_reps,"
                + " planned_duration_seconds, target_load_kg FROM planned_workout_exercises"
                + " WHERE planned_workout_id = ?", w)) {
            boolean duracion = "DURATION".equals(f.get("prescription_type"));
            int series = f.get("planned_sets") == null ? 1 : ((Number) f.get("planned_sets")).intValue();
            int total = series * ((Number) f.get(duracion ? "planned_duration_seconds" : "planned_reps")).intValue();
            sesiones.handle(new RecordSessionExerciseCommand(u, s, ((Number) f.get("planned_exercise_id")).longValue(),
                    (short) series, duracion ? null : total, duracion ? total : null,
                    (BigDecimal) f.get("target_load_kg"), null));
        }
        sesiones.handle(new FinishWorkoutSessionCommand(u, s, (short) 6, (short) 4, (short) 45));
    }

    /** El ejercicio con 20 kg hecho con 20 kg; el resto, dejado por fatiga (RPE 8). */
    private void parcialConCarga(long u, long w) {
        long s = sesiones.handle(new StartWorkoutSessionCommand(u, w)).orElseThrow().getId();
        boolean primero = true;
        for (var f : jdbc.queryForList("SELECT planned_exercise_id, prescription_type, planned_sets, planned_reps,"
                + " planned_duration_seconds, target_load_kg FROM planned_workout_exercises"
                + " WHERE planned_workout_id = ? ORDER BY exercise_order", w)) {
            long pe = ((Number) f.get("planned_exercise_id")).longValue();
            if (primero && f.get("target_load_kg") != null) {
                int series = ((Number) f.get("planned_sets")).intValue();
                sesiones.handle(new RecordSessionExerciseCommand(u, s, pe, (short) series,
                        series * ((Number) f.get("planned_reps")).intValue(), null, new BigDecimal("20.00"), null));
                primero = false;
            } else {
                sesiones.handle(new RecordSessionExerciseCommand(u, s, pe, (short) 0, null, null, null,
                        SkipReason.FATIGUE));
            }
        }
        sesiones.handle(new FinishWorkoutSessionCommand(u, s, (short) 8, (short) 4, (short) 20));
    }

    private static String describir(Dia[] dias) {
        return Arrays.stream(dias).map(Dia::name).collect(Collectors.joining(","));
    }

    // =======================================================================
    // Metricas de Replicate (consulta gratuita de cada prediccion)
    // =======================================================================

    record Metrica(String id, String estado, long entrada, long salida, double segundos) {}

    private List<Metrica> metricasDeReplicate() {
        var ids = new LinkedHashSet<String>();
        var m = Pattern.compile("prediction_id=(\\S+)");
        for (var t : tiempos) {
            var x = m.matcher(t);
            if (x.find() && !x.group(1).equals("desconocido")) ids.add(x.group(1));
        }
        var http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
        var json = new ObjectMapper();
        var lista = new ArrayList<Metrica>();
        for (var id : ids) {
            try {
                var r = http.send(HttpRequest.newBuilder(URI.create("https://api.replicate.com/v1/predictions/" + id))
                        .header("Authorization", "Bearer proxy").timeout(Duration.ofSeconds(30)).GET().build(),
                        HttpResponse.BodyHandlers.ofString());
                var p = json.readTree(r.body());
                var mt = p.path("metrics");
                lista.add(new Metrica(id, p.path("status").asText(), mt.path("token_input_count").asLong(),
                        mt.path("token_output_count").asLong(), mt.path("predict_time").asDouble()));
            } catch (Exception e) {
                lista.add(new Metrica(id, "sin metricas: " + e.getClass().getSimpleName(), 0, 0, 0));
            }
        }
        return lista;
    }

    // =======================================================================
    // Informe
    // =======================================================================

    private static final Pattern CODIGO = Pattern.compile("^(V\\d+[A-Z]?)\\b");

    private static String codigo(String problema) {
        var m = CODIGO.matcher(problema.strip());
        return m.find() ? m.group(1) : "CONTRATO/OTRO";
    }

    private static String codigos(Collection<String> problemas) {
        return problemas.stream().map(CampanaReplicateRealIT::codigo)
                .collect(Collectors.toCollection(TreeSet::new)).toString();
    }

    private void escribir(List<Perfil> perfiles, List<Metrica> metricas, long ms) throws Exception {
        var json = new ObjectMapper();
        try (var w = Files.newBufferedWriter(SALIDA.resolve("llamadas.jsonl"))) {
            for (var l : llamadas) w.write(json.writeValueAsString(l) + "\n");
        }
        try (var w = Files.newBufferedWriter(SALIDA.resolve("ciclos.jsonl"))) {
            for (var p : perfiles) {
                w.write(json.writeValueAsString(p.inicial()) + "\n");
                for (var c : p.ciclos()) w.write(json.writeValueAsString(c) + "\n");
            }
        }
        try (var w = Files.newBufferedWriter(SALIDA.resolve("eventos.log"))) {
            for (var e : eventos) w.write(e.etiqueta() + " | " + e.nivel() + " | " + e.texto() + "\n");
            for (var t : tiempos) w.write(t + "\n");
        }

        long entrada = metricas.stream().mapToLong(Metrica::entrada).sum();
        long salida = metricas.stream().mapToLong(Metrica::salida).sum();
        double usd = entrada / 1e6 * USD_ENTRADA + salida / 1e6 * USD_SALIDA;

        var sb = new StringBuilder("# Campaña con Replicate real · modo ").append(MODO).append("\n\n");
        sb.append("Duración ").append(ms / 60000).append(" min · ").append(perfiles.size()).append(" perfiles · ")
                .append(llamadas.size()).append(" llamadas reales · ").append(metricas.size())
                .append(" predicciones con métricas · tokens ").append(entrada).append(" de entrada y ").append(salida)
                .append(" de salida · **coste estimado US$ ").append(String.format(Locale.ROOT, "%.2f", usd))
                .append("** (a ").append(USD_ENTRADA).append(" / ").append(USD_SALIDA)
                .append(" US$ por millón; la cifra oficial es la del panel de Replicate)\n\n");
        if (motivoParada != null) sb.append("**CAMPAÑA DETENIDA:** ").append(motivoParada).append("\n\n");

        sb.append("## Generación inicial\n\n| Objetivo | Perfiles | Origen IA | Origen reglas (respaldo) | Llamadas | Válida V24+V25 | Rechazos por regla (intentos fallidos) |\n|---|---:|---:|---:|---:|---:|---|\n");
        for (var o : GoalType.values()) {
            var l = perfiles.stream().filter(p -> p.objetivo() == o).map(Perfil::inicial).toList();
            if (l.isEmpty()) continue;
            sb.append("| ").append(o).append(" | ").append(l.size())
                    .append(" | ").append(l.stream().filter(i -> "AI".equals(i.origen())).count())
                    .append(" | ").append(l.stream().filter(i -> "RULE_ENGINE".equals(i.origen())).count())
                    .append(" | ").append(l.stream().mapToInt(Inicial::intentos).sum())
                    .append(" | ").append(l.stream().filter(i -> "VALIDA".equals(i.validacion())).count())
                    .append(" | ").append(frecuencias(l.stream().flatMap(i -> i.problemas().stream()).toList()))
                    .append(" |\n");
        }

        sb.append("\n## Ciclos semanales\n\n| Objetivo | Ciclos | Con IA | Llamadas | APLICADA | SIN_EFECTO | RESPALDO | RECHAZADA | NO_FACTIBLE | SIN_ORDEN | Con reintento | Pasos (prog/recup/reinc/vuelta) | Rechazos por regla (intentos fallidos) | Violaciones |\n|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|---|---:|\n");
        for (var o : GoalType.values()) {
            var c = perfiles.stream().filter(p -> p.objetivo() == o).flatMap(p -> p.ciclos().stream()).toList();
            if (c.isEmpty()) continue;
            var etiquetas = perfiles.stream().filter(p -> p.objetivo() == o).map(Perfil::nombre).collect(Collectors.toSet());
            var rechazos = llamadas.stream().filter(l -> l.tipo().equals("ADAPTACION") && l.etiqueta() != null
                            && etiquetas.contains(l.etiqueta().split(" · ")[0]))
                    .flatMap(l -> l.problemasPrevios().stream()).toList();
            sb.append("| ").append(o).append(" | ").append(c.size())
                    .append(" | ").append(c.stream().filter(x -> x.llamadasContadas() > 0).count())
                    .append(" | ").append(c.stream().mapToInt(Ciclo::llamadasContadas).sum());
            for (var st : List.of("APLICADA", "SIN_EFECTO", "RESPALDO", "RECHAZADA", "NO_FACTIBLE", "SIN_ORDEN"))
                sb.append(" | ").append(c.stream().filter(x -> st.equals(x.status())).count());
            sb.append(" | ").append(c.stream().filter(x -> x.llamadasContadas() > 1).count())
                    .append(" | ").append(c.stream().filter(x -> "PROGRESION".equals(x.tipoPaso())).count())
                    .append("/").append(c.stream().filter(x -> "RECUPERACION".equals(x.tipoPaso())).count())
                    .append("/").append(c.stream().filter(x -> "REINCORPORACION".equals(x.tipoPaso())).count())
                    .append("/").append(c.stream().filter(x -> "VUELTA_ATRAS".equals(x.tipoPaso())).count())
                    .append(" | ").append(frecuencias(rechazos))
                    .append(" | ").append(c.stream().mapToInt(x -> x.violaciones().size()).sum()).append(" |\n");
        }

        sb.append("\n## Por intervención (ciclos con IA)\n\n| Intervención | Ciclos | APLICADA | SIN_EFECTO | RESPALDO | NO_FACTIBLE/RECHAZADA | Llamadas |\n|---|---:|---:|---:|---:|---:|---:|\n");
        var todos = perfiles.stream().flatMap(p -> p.ciclos().stream()).filter(x -> x.llamadasContadas() > 0).toList();
        todos.stream().collect(Collectors.groupingBy(x -> String.valueOf(x.intervenciones()), TreeMap::new,
                Collectors.toList())).forEach((k, v) -> sb.append("| ").append(k).append(" | ").append(v.size())
                .append(" | ").append(v.stream().filter(x -> "APLICADA".equals(x.status())).count())
                .append(" | ").append(v.stream().filter(x -> "SIN_EFECTO".equals(x.status())).count())
                .append(" | ").append(v.stream().filter(x -> "RESPALDO".equals(x.status())).count())
                .append(" | ").append(v.stream().filter(x -> List.of("NO_FACTIBLE", "RECHAZADA").contains(x.status())).count())
                .append(" | ").append(v.stream().mapToInt(Ciclo::llamadasContadas).sum()).append(" |\n"));

        long latMedia = (long) llamadas.stream().mapToLong(Llamada::ms).average().orElse(0);
        long latMax = llamadas.stream().mapToLong(Llamada::ms).max().orElse(0);
        sb.append("\nLatencia por llamada: media ").append(latMedia / 1000).append(" s, máxima ")
                .append(latMax / 1000).append(" s. Llamadas con error del proveedor: ")
                .append(llamadas.stream().filter(l -> l.error() != null).count()).append(".\n");

        var conViolaciones = perfiles.stream().flatMap(p -> p.ciclos().stream())
                .filter(x -> !x.violaciones().isEmpty()).toList();
        sb.append("\n## Violaciones de las comprobaciones independientes (").append(conViolaciones.size()).append(")\n\n");
        if (conViolaciones.isEmpty()) sb.append("Ninguna.\n");
        conViolaciones.forEach(x -> sb.append("- ").append(x.perfil()).append(" s").append(x.semana()).append(": ")
                .append(x.violaciones()).append("\n"));

        var observadas = perfiles.stream().flatMap(p -> p.ciclos().stream())
                .filter(x -> !x.observaciones().isEmpty()).toList();
        sb.append("\n## Hallazgos cuantificados (desde las correcciones 4 y 5 tambien cuentan como violacion)\n\n");
        for (var tipo : List.of(SOLO_MINUTOS, ANADE_MAS))
            sb.append("- ").append(tipo).append(": ").append(observadas.stream().filter(x -> x.observaciones().stream()
                    .anyMatch(o -> o.startsWith(tipo))).count()).append(" ciclos\n");
        observadas.forEach(x -> sb.append("  - ").append(x.perfil()).append(" s").append(x.semana()).append(" ")
                .append(x.intervenciones()).append(": ").append(x.observaciones()).append("\n"));

        var cortes = perfiles.stream().filter(p -> p.corte() != null).toList();
        sb.append("\n## Perfiles interrumpidos (").append(cortes.size()).append(")\n\n");
        if (cortes.isEmpty()) sb.append("Ninguno.\n");
        cortes.forEach(p -> sb.append("- ").append(p.nombre()).append(": ").append(p.corte()).append("\n"));

        sb.append("\n## Detalle por perfil\n\n");
        for (var p : perfiles) {
            sb.append("### ").append(p.nombre()).append("\n\nInicial: ").append(p.inicial().origen()).append(", ")
                    .append(p.inicial().intentos()).append(" llamadas, ").append(p.inicial().validacion()).append("\n\n");
            for (var c : p.ciclos()) {
                sb.append("- s").append(c.semana()).append(" [").append(c.guion()).append("] ")
                        .append(c.decision()).append("/").append(c.status());
                if (c.intervenciones() != null && !"{}".equals(c.intervenciones())) sb.append(" ").append(c.intervenciones());
                sb.append(" · llamadas ").append(c.llamadasContadas());
                if (c.tipoPaso() != null) sb.append(" · paso ").append(c.tipoPaso());
                if (c.motivo() != null) sb.append(" · motivo: ").append(c.motivo());
                if (c.antes() != null && !c.antes().isEmpty())
                    sb.append("\n  - antes: `").append(c.antes()).append("`\n  - después: `").append(c.despues()).append("`");
                if (!c.violaciones().isEmpty()) sb.append("\n  - **violaciones:** ").append(c.violaciones());
                sb.append("\n");
            }
            sb.append("\n");
        }

        // La consulta de linaje V31/V32 y las restricciones de V34, sobre toda la base efimera.
        var texto = Files.readString(Path.of("docs/FitSense/consultas/linaje_v31_v32.sql")).lines()
                .filter(l -> !l.stripLeading().startsWith("--")).collect(Collectors.joining("\n"));
        var linaje = jdbc.queryForList(texto.split(";")[0].strip());
        sb.append("\n## Integridad V31–V34\n\nConsulta de linaje: ").append(linaje.size()).append(" violaciones.\n");

        Files.writeString(SALIDA.resolve("informe.md"), sb.toString());
        System.out.println(sb);
        org.junit.jupiter.api.Assertions.assertEquals(List.of(), linaje, "violaciones de linaje V31/V32");
        org.junit.jupiter.api.Assertions.assertNull(motivoParada, "campana detenida");
        org.junit.jupiter.api.Assertions.assertEquals(List.of(), conViolaciones.stream()
                .map(x -> x.perfil() + " s" + x.semana() + " " + x.violaciones()).toList());
    }

    private static String frecuencias(List<String> problemas) {
        if (problemas.isEmpty()) return "—";
        return problemas.stream().collect(Collectors.groupingBy(CampanaReplicateRealIT::codigo, TreeMap::new,
                Collectors.counting())).entrySet().stream().map(e -> e.getKey() + "×" + e.getValue())
                .collect(Collectors.joining(", "));
    }
}
