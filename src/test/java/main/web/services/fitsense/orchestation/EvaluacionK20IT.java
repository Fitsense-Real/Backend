package main.web.services.fitsense.orchestation;

import main.web.services.fitsense.RelojPruebas;
import main.web.services.fitsense.execution.domain.model.commands.FinishWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.model.commands.RecordSessionExerciseCommand;
import main.web.services.fitsense.execution.domain.model.commands.StartWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.services.WorkoutSessionCommandService;
import main.web.services.fitsense.iam.domain.model.commands.SignUpCommand;
import main.web.services.fitsense.iam.domain.services.UserCommandService;
import main.web.services.fitsense.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import main.web.services.fitsense.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import main.web.services.fitsense.orchestration.application.internal.WeeklyCycleService;
import main.web.services.fitsense.planning.application.internal.commandservices.SafetyExclusionResolver;
import main.web.services.fitsense.planning.domain.model.valueobjects.AdaptedWeekResult;
import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanGenerationContext;
import main.web.services.fitsense.planning.domain.model.valueobjects.PrescriptionType;
import main.web.services.fitsense.planning.domain.model.valueobjects.WeekDerivation;
import main.web.services.fitsense.planning.domain.services.AdaptedWeekProducer;
import main.web.services.fitsense.planning.domain.services.ConservationValidator;
import main.web.services.fitsense.planning.domain.services.PlanDraftValidator;
import main.web.services.fitsense.planning.infrastructure.generation.ai.ReplicateSessionFixGenerator;
import main.web.services.fitsense.planning.infrastructure.generation.ai.ReplicateTrainingPlanGenerator;
import main.web.services.fitsense.planning.infrastructure.generation.ai.SessionFixPromptBuilder;
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
import main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * EVALUACION FINAL K20 del short paper IEEE: los 20 usuarios sinteticos U01-U20 (perfiles historicos, sin cambios) de
 * extremo a extremo sobre la VERSION CANDIDATA de {@code eval/final-ieee-k20}:
 * perfil -> generacion inicial -> ejecucion -> adherencia -> decision -> adaptacion -> validacion -> publicacion ->
 * semana siguiente (continuidad) -> segundo ciclo.
 * <p>
 * Solo de prueba (ningun cambio en src/main). Opt-in:
 * <pre>
 *   -Dfitsense.evaluacion.k20=true
 *   -Dfitsense.evaluacion.k20.modo=DETERMINISTA | SIMULADA | REPLICATE
 *   -Dfitsense.evaluacion.k20.salida=docs/FitSense/short_paper_ieee/evaluacion_final_k20/evidencia/&lt;carpeta nueva&gt;
 *   -Dfitsense.evaluacion.k20.commit=$(git rev-parse HEAD)
 * </pre>
 * <ul>
 *   <li><b>DETERMINISTA</b> (D, D'): IA desactivada; el nucleo determinista (metricas, decision, respaldo, seguridad).</li>
 *   <li><b>SIMULADA</b> (preparacion): el camino de la IA es el real -orden, contexto, prompt V1, interpretacion, V8C,
 *       validacion estricta, V32- pero la respuesta la escribe el laboratorio, sin red. Intento 1: una propuesta que el
 *       backend DEBE rechazar (sube dosis, toca una sesion no senalada o pone un sustituto mas dificil). Intento 2:
 *       la mejor modificacion que la sonda de factibilidad encontro valida; intento 3: solo la primera sesion. NO es
 *       un resultado de IA: sirve para comprobar el recorrido y el rechazo.</li>
 *   <li><b>REPLICATE</b> (R, R'): exige {@code -Dfitsense.evaluacion.k20.autorizacion-replicate=SI}, credencial y el
 *       limite de predicciones y dolares. No se ejecuta sin autorizacion expresa.</li>
 * </ul>
 * La SONDA DE FACTIBILIDAD (modos SIMULADA y REPLICATE, antes del intento 1 y fuera de la cuenta de llamadas)
 * enumera las modificaciones simples que la intervencion permite en cada sesion senalada (una serie menos, una o dos
 * repeticiones menos, menos segundos, menos carga, o un sustituto del mismo patron) y las juzga con el productor y
 * los validadores reales. Solo mide: no cambia lo que se publica.
 * <p>
 * Configuracion de la candidata FIJADA aqui (no se puede cambiar con -D): limite de dificultad {@code preferencia}
 * (C), retirada localizada {@code on} (solo actua con el limite), progresion apagada, V8C multiconjunto (H1, por
 * defecto), prompt V1 (el unico de la rama), politica PLAN-HIST-1 (por defecto).
 * <p>
 * Al final se exportan TODAS las tablas de la base efimera a {@code tablas/} para el oraculo independiente
 * ({@code herramientas/oraculo_k20.py}), que no usa funciones del backend. La carpeta de salida debe no existir o
 * estar vacia: una ejecucion nunca sobrescribe otra.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.adaptation.progression.enabled=false",
        "fitsense.adaptation.difficulty-guard=preferencia",
        "fitsense.adaptation.pain-localized-removal=on"
})
@EnabledIfSystemProperty(named = "fitsense.evaluacion.k20", matches = "true")
@DisplayName("Evaluacion final K20: 20 usuarios de extremo a extremo sobre la version candidata")
class EvaluacionK20IT {

    static final String MODO = System.getProperty("fitsense.evaluacion.k20.modo", "DETERMINISTA");
    static final boolean REPLICATE = "REPLICATE".equals(MODO);
    static final boolean SIMULADA = "SIMULADA".equals(MODO);
    static final boolean IA_ACTIVA = REPLICATE || SIMULADA;
    static final boolean GEMELOS = Boolean.parseBoolean(System.getProperty("fitsense.evaluacion.k20.gemelos", "false"));
    static final boolean CREDENCIAL_PROXY = "proxy".equals(System.getProperty("fitsense.evaluacion.k20.credencial", ""));
    static final int MAX_LLAMADAS = Integer.getInteger("fitsense.evaluacion.k20.max-llamadas", 80);
    static final double COSTE_MAX_POR_LLAMADA_USD = Double.parseDouble(
            System.getProperty("fitsense.evaluacion.k20.coste-por-llamada", "0.05"));
    static final double MAX_USD = Double.parseDouble(System.getProperty("fitsense.evaluacion.k20.max-usd", "4.0"));
    static final String PREFER_WAIT = System.getProperty("fitsense.evaluacion.k20.prefer-wait", "25");
    /** Candidata posterior (eval/final-ieee-k20-v2): v1 | v2. Vacio = la del backend (v1). */
    static final String PROMPT = System.getProperty("fitsense.evaluacion.k20.prompt", "");

    static final Path K20 = Path.of("docs/FitSense/short_paper_ieee/evaluacion_final_k20");
    /** Por defecto los 20 historicos; con -Dfitsense.evaluacion.k20.perfiles, otra cohorte (casos reservados). */
    static final String PERFILES_OTROS = System.getProperty("fitsense.evaluacion.k20.perfiles", "");
    static final Path PERFILES = PERFILES_OTROS.isBlank() ? K20.resolve("entrada/03_PERFILES_U01_U20.csv")
            : Path.of(PERFILES_OTROS);
    /** SHA-256 del CSV historico (blob 95c2839 de lab/experimento-kora-20usuarios): los perfiles no cambian. */
    static final String SHA256_PERFILES = "584e2204ec8eb41ef95bb61b7fe6a6f6c90164a36fe920d1097dcae250ae2307";
    static final Path SALIDA = Path.of(System.getProperty("fitsense.evaluacion.k20.salida", "target/k20"));
    static final LocalDate S1 = LocalDate.of(2026, 11, 2);
    static final LocalDate S2 = S1.plusWeeks(1);
    static final LocalDate S3 = S2.plusWeeks(1);

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @DynamicPropertySource
    static void ia(DynamicPropertyRegistry r) {
        r.add("fitsense.ai.enabled", () -> String.valueOf(IA_ACTIVA));
        if (!PROMPT.isBlank()) r.add("fitsense.ai.adaptation.prompt-version", () -> PROMPT);
        if (REPLICATE) r.add("fitsense.ai.prefer-wait-seconds", () -> PREFER_WAIT);
        if (SIMULADA) r.add("fitsense.ai.base-url", () -> "http://127.0.0.1:9/ia-simulada-sin-red");
        r.add("fitsense.ai.api-token", () -> SIMULADA ? "ia-simulada-sin-red" : !REPLICATE ? ""
                : CREDENCIAL_PROXY ? "marcador-credencial-inyectada-por-el-proxy"
                : Objects.requireNonNullElse(System.getenv("REPLICATE_API_TOKEN"), ""));
    }

    /** El reloj lo controla el laboratorio: sin barrido horario. */
    @MockitoBean main.web.services.fitsense.planning.infrastructure.scheduling.OverdueWorkoutScheduler vencidas;
    @MockitoSpyBean PlanDraftValidator validador;
    @MockitoSpyBean ReplicateTrainingPlanGenerator generador;
    @MockitoSpyBean ReplicateSessionFixGenerator correcciones;
    @MockitoSpyBean AdaptedWeekProducer productor;

    @Autowired SessionFixPromptBuilder promptBuilder;
    @Autowired ConservationValidator conservacion;
    @Autowired SafetyExclusionResolver exclusionesResolver;
    @Autowired UserCommandService cuentas;
    @Autowired UserProfileCommandService perfiles;
    @Autowired PlanningContextFacade planning;
    @Autowired WorkoutSessionCommandService sesiones;
    @Autowired TrainingPlanController planes;
    @Autowired UserRepository usuarios;
    @Autowired WeeklyCycleService ciclo;
    @Autowired JdbcTemplate jdbc;
    @Autowired WebApplicationContext contexto;
    MockMvc http;
    static final ObjectMapper JSON = new ObjectMapper();
    @Autowired main.web.services.fitsense.planning.application.internal.queryservices.AdaptationOutcomeQueryService resultadosApi;

    static final AtomicInteger validaciones = new AtomicInteger();
    static final List<String> rechazosValidacion = Collections.synchronizedList(new ArrayList<>());
    static final AtomicInteger llamadas = new AtomicInteger();
    static volatile String etiquetaActual = "";
    /** Mientras la sonda juzga candidatos, los espias no cuentan nada. */
    static volatile boolean sondeando = false;
    static final List<String> trazasIa = Collections.synchronizedList(new ArrayList<>());
    static final List<Map<String, Object>> intentos = Collections.synchronizedList(new ArrayList<>());
    static final List<Map<String, Object>> factibilidad = Collections.synchronizedList(new ArrayList<>());
    static final Map<String, Map<Long, List<Candidata>>> validasPorEtiqueta = new HashMap<>();
    static final AtomicInteger intentoActual = new AtomicInteger();
    /** Rota la clase de propuesta invalida del intento 1 (subir una serie, sesion no senalada, sustituto mas dificil). */
    static final AtomicInteger invalidas = new AtomicInteger();

    record Perfil(Map<String, String> c) {
        String get(String k) { return c.get(k); }
    }

    /** Una modificacion simple de UNA sesion senalada, con su clase. */
    record Candidata(Long sesion, String clase, String descripcion, List<PlanDraft.DraftExercise> ejercicios,
                     LocalDate fecha) {}

    @Test
    void evaluacion() throws Exception {
        // ------------------------------------------------------------ guardas previas
        String url = postgres.getJdbcUrl();
        assertTrue(url.startsWith("jdbc:postgresql://localhost:") || url.startsWith("jdbc:postgresql://127.0.0.1:"),
                "solo base efimera local: " + url);
        if (PERFILES_OTROS.isBlank())
            assertEquals(SHA256_PERFILES, sha256(Files.readAllBytes(PERFILES)), "los 20 perfiles son los historicos");
        assertTrue(conservacion.limitaDificultad(), "candidata: limite de dificultad encendido (preferencia)");
        assertTrue(productor.retiradaLocalizada(), "candidata: retirada localizada activa (con el limite)");
        int cuenta = -1;
        if (REPLICATE) {
            assertEquals("SI", System.getProperty("fitsense.evaluacion.k20.autorizacion-replicate", ""),
                    "BLOQUEADO: R/R' exige autorizacion expresa (ninguna llamada realizada)");
            if (CREDENCIAL_PROXY) {
                var resp = java.net.http.HttpClient.newHttpClient().send(java.net.http.HttpRequest.newBuilder(
                                java.net.URI.create("https://api.replicate.com/v1/account")).GET().build(),
                        java.net.http.HttpResponse.BodyHandlers.discarding());
                cuenta = resp.statusCode();
                assertEquals(200, cuenta, "BLOQUEADO: el proxy no autentica en Replicate (ninguna llamada realizada)");
            } else assertFalse(Objects.requireNonNullElse(System.getenv("REPLICATE_API_TOKEN"), "").isBlank(),
                    "BLOQUEADO: sin credencial (ninguna llamada realizada)");
        }
        if (Files.exists(SALIDA)) try (var s = Files.list(SALIDA)) {
            assertTrue(s.findAny().isEmpty(), "la carpeta " + SALIDA + " ya tiene evidencia: no se sobrescribe");
        }
        Files.createDirectories(SALIDA);
        http = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        espiar();

        var lista = leerPerfiles();
        var cfg = new LinkedHashMap<String, Object>();
        cfg.put("evaluacion", "K20 final IEEE");
        cfg.put("modo", MODO);
        cfg.put("commit", System.getProperty("fitsense.evaluacion.k20.commit", "no indicado"));
        cfg.put("fecha_ejecucion_utc", java.time.Instant.now().toString());
        cfg.put("base", "PostgreSQL efimero " + jdbc.queryForObject("SHOW server_version", String.class));
        cfg.put("flyway_version", jdbc.queryForObject("SELECT version FROM flyway_schema_history WHERE success"
                + " ORDER BY installed_rank DESC LIMIT 1", String.class));
        cfg.put("migraciones", jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE success", Integer.class));
        cfg.put("checksum_migraciones", jdbc.queryForObject("SELECT md5(string_agg(coalesce(checksum::text,'-'), ','"
                + " ORDER BY installed_rank)) FROM flyway_schema_history WHERE success", String.class));
        cfg.put("ejercicios_activos", jdbc.queryForObject("SELECT count(*) FROM exercises WHERE is_active", Integer.class));
        cfg.put("hash_catalogo", jdbc.queryForObject("SELECT md5(string_agg(exercise_id || ':' || is_active || ':'"
                + " || coalesce(difficulty_level::text,'-') || ':' || coalesce(movement_pattern,'-'), ','"
                + " ORDER BY exercise_id)) FROM exercises", String.class));
        cfg.put("ia_habilitada", generador.isEnabled());
        cfg.put("modelo", generador.modelName());
        cfg.put("prompt_adaptacion", promptBuilder.version());
        cfg.put("v8c_regla_patron", conservacion.reglaPatron());
        cfg.put("limite_dificultad", conservacion.estrategiaSustitucion() + " (limita=" + conservacion.limitaDificultad() + ")");
        cfg.put("retirada_localizada", "solicitada=" + productor.retiradaSolicitada() + " activa=" + productor.retiradaLocalizada());
        cfg.put("progresion", "false");
        cfg.put("politica_por_defecto", "PLAN-HIST-1");
        cfg.put("perfiles", PERFILES + " sha256=" + sha256(Files.readAllBytes(PERFILES)));
        cfg.put("semanas", "S1 " + S1 + " (medida, patron del perfil) -> S2 " + S2 + " (continuidad: todo hecho) -> S3 " + S3);
        cfg.put("gemelos_generacion_ia", REPLICATE && GEMELOS);
        cfg.put("credencial", SIMULADA ? "ninguna (IA simulada, sin red)" : !REPLICATE ? "ninguna"
                : CREDENCIAL_PROXY ? "secreto de red inyectado por el proxy (no visible)" : "variable REPLICATE_API_TOKEN");
        cfg.put("comprobacion_cuenta_http", cuenta);
        cfg.put("limite_ia", REPLICATE ? MAX_LLAMADAS + " predicciones / USD " + MAX_USD + " (supuesto " + COSTE_MAX_POR_LLAMADA_USD
                + " por prediccion)" : "no aplica");
        escribir("00_configuracion.txt", cfg.entrySet().stream().map(e -> e.getKey() + ": " + e.getValue()).toList());

        var res = new ArrayList<Map<String, Object>>();
        var incid = new ArrayList<String>();
        var usuariosIds = new LinkedHashMap<String, Long>();
        for (var p : lista) ejecutarUsuario(p, res, incid, usuariosIds);
        if (REPLICATE && GEMELOS) for (var p : lista) ejecutarGemelo(p, incid);

        csv("resultados_20.csv", res);
        csv("intentos_ia.csv", intentos);
        csv("factibilidad.csv", factibilidad);
        csv("coherencia_v2.csv", coherencia);
        escribir("usuarios.csv", new ArrayList<>(List.of("id,user_id")) {{
            usuariosIds.forEach((k, v) -> add(k + "," + v));
        }});
        escribir("incidencias.log", incid);
        escribir("trazas_ia.jsonl", trazasIa);
        exportarTablas();
        escribir("99_resumen.txt", List.of("usuarios=" + res.size(), "llamadas_proveedor=" + llamadas.get(),
                "incidencias=" + incid.size()));
        System.out.println("K20 " + MODO + " -> " + SALIDA + " usuarios=" + res.size() + " llamadas=" + llamadas.get()
                + " incidencias=" + incid.size());
    }

    // ================================================================== un usuario

    private void ejecutarUsuario(Perfil p, List<Map<String, Object>> res, List<String> incid,
                                 Map<String, Long> usuariosIds) {
        String id = p.get("id");
        var r = new LinkedHashMap<String, Object>();
        r.put("id", id);
        r.put("objetivo", p.get("goal_type"));
        r.put("patron", p.get("patron") + " " + p.get("secuencia"));
        long u;
        try {
            u = alta(p, id.toLowerCase());
        } catch (RuntimeException e) {
            incid.add(id + " ALTA ERROR " + e);
            r.put("alta", "ERROR: " + e.getMessage());
            res.add(r);
            return;
        }
        usuariosIds.put(id, u);
        r.put("user_id", u);
        r.put("semilla", u * 1_000_003L + S1.toEpochDay());

        // ---------------------------------------------------------- generacion inicial (siempre motor de reglas)
        etiquetaActual = id + "/generacion";
        validaciones.set(0);
        rechazosValidacion.clear();
        Optional<Long> plan;
        String errorGen = null;
        if (IA_ACTIVA) doReturn(false).when(generador).isEnabled(); // mismo plan inicial que D
        try {
            plan = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null);
        } catch (RuntimeException e) {
            plan = Optional.empty();
            errorGen = e.getClass().getSimpleName() + ": " + e.getMessage();
        } finally {
            if (IA_ACTIVA) doCallRealMethod().when(generador).isEnabled();
        }
        r.put("generacion_validaciones", validaciones.get());
        r.put("generacion_rechazos", rechazosValidacion.size());
        if (plan.isEmpty()) {
            incid.add(id + " GENERACION sin plan: " + errorGen);
            r.put("generacion", errorGen == null ? "SIN_PLAN" : "ERROR: " + errorGen);
            res.add(r);
            return;
        }
        long planS1 = plan.get();
        r.put("generacion", "PUBLICADO");
        r.put("plan_s1", planS1);
        var medidas = sesionesDe(planS1);

        // ---------------------------------------------------------- semana 1: el patron del perfil
        String[] patron = p.get("secuencia").split("/");
        String[] motivos = new String[Math.max(medidasCount(p), 3)];
        for (int i = 0; i < motivos.length; i++)
            motivos[i] = Objects.requireNonNullElse(p.c().get("motivo_omision_s" + (i + 1)), "");
        RelojPruebas.abrirVentanaDeEjecucion(jdbc, u);
        for (int i = 0; i < medidas.size(); i++) {
            String paso = i < patron.length ? patron[i] : "N";
            try {
                ejecutar(u, medidas.get(i), paso, i < motivos.length ? motivos[i] : "");
            } catch (RuntimeException e) {
                incid.add(id + " EJECUCION S1 sesion " + (i + 1) + " " + paso + " ERROR " + e);
            }
        }

        // ---------------------------------------------------------- ciclo S1 -> S2
        etiquetaActual = id + "/ciclo1";
        intentoActual.set(0);
        int llamadasAntes = llamadas.get();
        Optional<Long> planS2 = Optional.empty();
        try {
            planS2 = ciclo.runFor(u, S1, S2);
        } catch (RuntimeException e) {
            incid.add(id + " CICLO1 ERROR " + e);
            r.put("ciclo1_error", e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        r.put("ciclo1_llamadas_ia", llamadas.get() - llamadasAntes);
        r.put("plan_s2", planS2.orElse(null));
        // Idempotencia: repetir el ciclo no publica otra version ni otra fila V32.
        int v32Antes = jdbc.queryForObject("SELECT count(*) FROM plan_adaptation_attempts WHERE user_id = ?", Integer.class, u);
        try {
            var repetido = ciclo.runFor(u, S1, S2);
            r.put("ciclo1_repetido_plan", repetido.orElse(null));
        } catch (RuntimeException e) {
            r.put("ciclo1_repetido_plan", "ERROR " + e.getClass().getSimpleName());
        }
        r.put("ciclo1_repetido_v32_nuevas",
                jdbc.queryForObject("SELECT count(*) FROM plan_adaptation_attempts WHERE user_id = ?", Integer.class, u) - v32Antes);
        api(u, r, "ciclo1");
        if (planS2.isEmpty()) {
            res.add(r);
            return;
        }

        // ---------------------------------------------------------- continuidad: recuperar S2 por la API (HTTP)
        String token = null;
        try {
            token = token(id.toLowerCase());
            var resp = http.perform(get("/api/v1/users/me/plan/" + planS2.get()).header("Authorization", "Bearer " + token))
                    .andReturn().getResponse();
            r.put("s2_api_http", resp.getStatus());
            var nodo = JSON.readTree(resp.getContentAsString());
            r.put("s2_api_sesiones", nodo.has("workouts") ? nodo.get("workouts").size() : -1);
            var actual = http.perform(get("/api/v1/users/me/plan/current").header("Authorization", "Bearer " + token))
                    .andReturn().getResponse();
            r.put("s2_api_current_http", actual.getStatus());
        } catch (Exception e) {
            r.put("s2_api_http", "ERROR " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        var semana2 = sesionesDe(planS2.get());
        RelojPruebas.abrirVentanaDeEjecucion(jdbc, u);
        int registradas = 0;
        for (long w : semana2) {
            try {
                ejecutar(u, w, "H", "");
                registradas++;
            } catch (RuntimeException e) {
                incid.add(id + " EJECUCION S2 " + w + " ERROR " + e);
            }
        }
        r.put("s2_sesiones_registradas", registradas + "/" + semana2.size());
        etiquetaActual = id + "/ciclo2";
        intentoActual.set(0);
        Optional<Long> planS3 = Optional.empty();
        try {
            planS3 = ciclo.runFor(u, S2, S3);
        } catch (RuntimeException e) {
            incid.add(id + " CICLO2 ERROR " + e);
            r.put("ciclo2_error", e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        r.put("plan_s3", planS3.orElse(null));
        api(u, r, "ciclo2");
        try {
            if (token != null) {
                var resp = http.perform(get("/api/v1/users/me/plan/adaptations").header("Authorization", "Bearer " + token))
                        .andReturn().getResponse();
                r.put("api_adaptaciones_http", resp.getStatus());
                var arr = JSON.readTree(resp.getContentAsString());
                var res2 = new ArrayList<String>();
                for (var n : arr) res2.add(n.path("weekStartDate").asText() + ":" + n.path("resultado").asText());
                r.put("api_adaptaciones", String.join("|", res2));
            }
        } catch (Exception e) {
            r.put("api_adaptaciones_http", "ERROR " + e.getClass().getSimpleName());
        }
        res.add(r);
    }

    /** El resultado de la adaptacion del ciclo (V32 via el servicio de la API) cuya semana medida es la indicada. */
    private static int medidasCount(Perfil p) {
        return p.get("secuencia").split("/").length;
    }

    private void api(long u, Map<String, Object> r, String clave) {
        LocalDate semana = "ciclo1".equals(clave) ? S1 : S2;
        try {
            var hist = resultadosApi.history(u);
            r.put(clave + "_api_filas", hist.size());
            for (var o : hist) {
                if (!semana.equals(o.weekStartDate()) && !semana.plusWeeks(1).equals(o.weekStartDate())) continue;
                r.put(clave + "_api_semana", o.weekStartDate());
                r.put(clave + "_api_resultado", o.resultado());
                r.put(clave + "_api_estado", o.estado());
                r.put(clave + "_api_motivo", o.motivoCodigo());
                if (semana.equals(o.weekStartDate())) break;
            }
        } catch (RuntimeException e) {
            r.put(clave + "_api_resultado", "ERROR " + e.getClass().getSimpleName());
        }
    }

    private String token(String sufijo) throws Exception {
        var res = http.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"k20-" + sufijo + "@pruebas.invalid\",\"password\":\"clave-de-laboratorio-123\"}"))
                .andReturn().getResponse();
        if (res.getStatus() != 200) throw new IllegalStateException("login " + res.getStatus());
        return JSON.readTree(res.getContentAsString()).get("token").asText();
    }

    /** Medicion de la generacion inicial con IA (bloque separado, solo REPLICATE con gemelos). */
    private void ejecutarGemelo(Perfil p, List<String> incid) {
        String id = p.get("id");
        etiquetaActual = id + "G/generacion";
        try {
            long u = alta(p, id.toLowerCase() + "-gemelo");
            planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null);
        } catch (RuntimeException e) {
            incid.add(id + "G GENERACION_IA " + e);
        }
    }

    // ================================================================== acciones

    private void ejecutar(long u, long w, String paso, String motivo) {
        switch (paso) {
            case "H" -> hacer(u, w, 1.0, (short) 6, (short) 4);
            case "P40" -> hacer(u, w, 0.4, (short) 5, (short) 3);
            case "O" -> planes.skip(UserDetailsImpl.build(usuarios.findById(u).orElseThrow()), w,
                    new SkipWorkoutResource(SkipReason.valueOf(motivo)));
            case "N" -> { }
            default -> throw new IllegalStateException("patron desconocido " + paso);
        }
    }

    private long alta(Perfil p, String sufijo) {
        var user = cuentas.handle(new SignUpCommand("k20-" + sufijo + "@pruebas.invalid", "clave-de-laboratorio-123",
                "Sintetico", p.get("id"), p.get("zona_horaria"), true)).orElseThrow();
        long u = user.getId();
        var dias = Arrays.stream(p.get("dias_disponibles").split(" ")[0].split("\\|")).map(Short::valueOf).toList();
        String objetivoPeso = p.get("peso_objetivo_kg");
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.parse(p.get("nacimiento")),
                BiologicalSex.valueOf(p.get("sexo")), new BigDecimal(p.get("altura_cm")), new BigDecimal(p.get("peso_kg")),
                objetivoPeso == null || objetivoPeso.isBlank() ? null : new BigDecimal(objetivoPeso),
                FitnessLevel.valueOf(p.get("nivel")), GoalType.valueOf(p.get("goal_type")), "Evaluacion K20 " + p.get("id"),
                TrainingLocation.valueOf(p.get("ubicacion")), Short.valueOf(p.get("dias_por_semana")), dias,
                Short.valueOf(p.get("minutos_sesion")), List.of(p.get("equipamiento").split("\\|")), List.of(),
                p.get("notas_salud"), true, true, true, true, true));
        return u;
    }

    /** Registra cada ejercicio con la fraccion indicada de su dosis total (redondeo half-up), regla historica. */
    private void hacer(long u, long w, double fraccion, short rpe, short satisfaccion) {
        long s = sesiones.handle(new StartWorkoutSessionCommand(u, w)).orElseThrow().getId();
        for (var f : jdbc.queryForList("SELECT planned_exercise_id, prescription_type, planned_sets, planned_reps,"
                + " planned_duration_seconds FROM planned_workout_exercises WHERE planned_workout_id = ?"
                + " ORDER BY exercise_order", w)) {
            boolean duracion = "DURATION".equals(f.get("prescription_type"));
            int series = f.get("planned_sets") == null ? 1 : ((Number) f.get("planned_sets")).intValue();
            int total = series * ((Number) f.get(duracion ? "planned_duration_seconds" : "planned_reps")).intValue();
            int hecho = (int) Math.round(total * fraccion);
            short seriesHechas = (short) Math.max(1, (int) Math.round(series * fraccion));
            sesiones.handle(new RecordSessionExerciseCommand(u, s, ((Number) f.get("planned_exercise_id")).longValue(),
                    seriesHechas, duracion ? null : hecho, duracion ? hecho : null, null, null));
        }
        int minutos = jdbc.queryForObject("SELECT expected_duration_minutes FROM planned_workouts WHERE"
                + " planned_workout_id = ?", Integer.class, w);
        sesiones.handle(new FinishWorkoutSessionCommand(u, s, rpe, satisfaccion, (short) Math.round(minutos * fraccion)));
    }

    private List<Long> sesionesDe(long planId) {
        return jdbc.queryForList("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ? ORDER BY scheduled_date",
                Long.class, planId);
    }

    // ================================================================== espias

    private void espiar() {
        doAnswer(inv -> {
            if (sondeando) return inv.callRealMethod();
            validaciones.incrementAndGet();
            try {
                return inv.callRealMethod();
            } catch (RuntimeException e) {
                rechazosValidacion.add(e.getMessage() == null ? e.getClass().getSimpleName()
                        : e.getMessage().replaceAll("\\s+", " "));
                throw e;
            }
        }).when(validador).validate(any(), any(), anyInt(), any());
        // Cada propuesta de la IA que juzga el productor (no la sonda): publicable o no, y por que.
        doAnswer(inv -> {
            Object salida = inv.callRealMethod();
            if (!sondeando && inv.getArgument(2) != null) registrarIntento((AdaptedWeekResult) salida);
            return salida;
        }).when(productor).produce(any(), any(), any(), any(), any());
        // (la variante de 4 argumentos delega en la de 5, que es la espiada: cada juicio se registra una vez)
        // Una respuesta que incumple el contrato (sesion no senalada, ejercicio no elegible...) no llega al productor:
        // se registra aqui como intento rechazado por CONTRATO.
        doAnswer(inv -> {
            try {
                return inv.callRealMethod();
            } catch (RuntimeException e) {
                if (!sondeando) {
                    var m = new LinkedHashMap<String, Object>();
                    m.put("etiqueta", etiquetaActual);
                    m.put("intento", intentoActual.get());
                    m.put("publicable", false);
                    m.put("motivo", "CONTRATO");
                    m.put("source", "");
                    m.put("efecto", false);
                    m.put("aplicadas", "");
                    m.put("problemas", String.valueOf(e.getMessage()).replaceAll("\\s+", " "));
                    intentos.add(m);
                }
                throw e;
            }
        }).when(correcciones).interpretar(any(), any());
        if (SIMULADA) {
            doThrow(new IllegalStateException("SIMULADA: la generacion inicial no llama a ningun proveedor"))
                    .when(generador).requestOutput(any(), any());
            doAnswer(this::simulada).when(correcciones).pedirRespuesta(any(), any(), any(), any(), anyInt());
            return;
        }
        if (!REPLICATE) return;
        doAnswer(inv -> proveedor(inv, "GENERACION")).when(generador).requestOutput(any(), any());
        doAnswer(inv -> {
            int intento = inv.getArgument(4);
            if (intento == 1) sondear(inv.getArgument(0), inv.getArgument(1));
            return proveedor(inv, "ADAPTACION");
        }).when(correcciones).pedirRespuesta(any(), any(), any(), any(), anyInt());
    }

    private void registrarIntento(AdaptedWeekResult r) {
        var m = new LinkedHashMap<String, Object>();
        m.put("etiqueta", etiquetaActual);
        m.put("intento", intentoActual.get());
        m.put("publicable", r.publicable());
        m.put("motivo", r.motivo());
        m.put("source", r.source());
        m.put("efecto", r.conservacion() != null && r.conservacion().huboAdaptacionEfectiva());
        m.put("aplicadas", r.conservacion() == null ? "" : r.conservacion().aplicadas());
        m.put("problemas", String.join(" || ", r.problemas()));
        intentos.add(m);
    }

    /** Una prediccion real, contada antes de hacerse: al llegar al limite no se llama (se registra). */
    private Object proveedor(org.mockito.invocation.InvocationOnMock inv, String tipo) throws Throwable {
        var traza = new LinkedHashMap<String, Object>();
        traza.put("etiqueta", etiquetaActual);
        traza.put("tipo", tipo);
        String intento = String.valueOf((Object) inv.getArgument(inv.getArguments().length - 1));
        traza.put("intento", intento);
        if ("ADAPTACION".equals(tipo)) {
            intentoActual.set(Integer.parseInt(intento));
            guardarPrompt(inv, traza);
        }
        int n = llamadas.incrementAndGet();
        if (n > MAX_LLAMADAS || n * COSTE_MAX_POR_LLAMADA_USD > MAX_USD + 1e-9) {
            llamadas.decrementAndGet();
            traza.put("resultado", "PRESUPUESTO_AGOTADO_SIN_LLAMADA");
            trazasIa.add(json(traza));
            throw new main.web.services.fitsense.planning.domain.exceptions.AiProviderUnavailableException(
                    "PRESUPUESTO_AGOTADO del laboratorio: no se llama a Replicate");
        }
        traza.put("n", n);
        long t0 = System.nanoTime();
        try {
            Object salida = inv.callRealMethod();
            traza.put("resultado", "RESPUESTA");
            traza.put("salida", String.valueOf(salida));
            return salida;
        } catch (Throwable e) {
            traza.put("resultado", "ERROR_PROVEEDOR");
            traza.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
            throw e;
        } finally {
            traza.put("ms", (System.nanoTime() - t0) / 1_000_000);
            trazasIa.add(json(traza));
        }
    }

    private void guardarPrompt(org.mockito.invocation.InvocationOnMock inv, Map<String, Object> traza) {
        try {
            var prompt = promptBuilder.build(inv.getArgument(0), inv.getArgument(1), inv.getArgument(2),
                    inv.getArgument(3));
            traza.put("prompt_chars", prompt.text().length());
            traza.put("prompt_sha256_16", sha256(prompt.text().getBytes(StandardCharsets.UTF_8)).substring(0, 16));
            var dir = SALIDA.resolve("prompts");
            Files.createDirectories(dir);
            Files.writeString(dir.resolve(etiquetaActual.replace('/', '_') + "_i" + traza.get("intento") + ".txt"),
                    prompt.text());
        } catch (Exception e) {
            traza.put("prompt_guardado", "no: " + e.getClass().getSimpleName());
        }
    }

    // ================================================================== IA SIMULADA (sin red)

    /**
     * Intento 1: una propuesta que el backend DEBE rechazar (rota por usuario: subir una serie, tocar una sesion no
     * senalada, un sustituto mas dificil). Intento 2: la mejor candidata valida de cada sesion segun la sonda (si una
     * sesion no tiene ninguna, no se propone nada para ella). Intento 3: solo la primera sesion con candidata.
     */
    private Object simulada(org.mockito.invocation.InvocationOnMock inv) throws IOException {
        PlanGenerationContext ctx = inv.getArgument(0);
        WeekDerivation der = inv.getArgument(1);
        int intento = inv.getArgument(4);
        intentoActual.set(intento);
        var traza = new LinkedHashMap<String, Object>();
        traza.put("etiqueta", etiquetaActual);
        traza.put("tipo", "ADAPTACION_SIMULADA");
        traza.put("intento", String.valueOf(intento));
        guardarPrompt(inv, traza);
        var prompt = promptBuilder.build(ctx, der, inv.getArgument(2), inv.getArgument(3));
        if (intento == 1) sondear(ctx, der);
        var validas = validasPorEtiqueta.getOrDefault(etiquetaActual, Map.of());
        var sesionesJson = new ArrayList<String>();
        var acciones = new ArrayList<String>();
        var senaladas = ctx.adjustment().sesionesParaIA();
        if (intento == 1) {
            int k = Math.floorMod(invalidas.getAndIncrement(), 3); // por orden de usuario: las tres clases
            var primera = senaladas.get(0);
            var w = der.from(primera.plannedWorkoutId()).orElseThrow().workout();
            var ej = new ArrayList<>(w.exercises());
            Long sesion = primera.plannedWorkoutId();
            LocalDate fecha = w.scheduledDate();
            String accion;
            if (k == 1) {
                // tocar una sesion NO senalada
                var noSenalada = der.workouts().stream().filter(d -> senaladas.stream()
                        .noneMatch(s -> s.plannedWorkoutId().equals(d.sourcePlannedWorkoutId()))).findFirst();
                if (noSenalada.isPresent()) {
                    sesion = noSenalada.get().sourcePlannedWorkoutId();
                    fecha = noSenalada.get().workout().scheduledDate();
                    ej = new ArrayList<>(noSenalada.get().workout().exercises());
                    ej.set(0, conSeries(ej.get(0), ej.get(0).plannedSets() == null ? 1 : ej.get(0).plannedSets() - 1));
                    accion = "INVALIDA_SESION_NO_SENALADA " + sesion;
                } else {
                    ej.set(0, conSeries(ej.get(0), (ej.get(0).plannedSets() == null ? 1 : ej.get(0).plannedSets()) + 1));
                    accion = "INVALIDA_SUBE_SERIE " + ej.get(0).exerciseId();
                }
            } else if (k == 2) {
                // un sustituto MAS DIFICIL (mismo patron y zona) para el primer ejercicio que lo tenga en el catalogo
                var elegibles = porId(ctx.availableExercises());
                String hecho = null;
                for (int x = 0; x < ej.size() && hecho == null; x++) {
                    var original = ej.get(x);
                    var orig = elegibles.get(original.exerciseId());
                    if (orig == null) continue;
                    var mas = ctx.availableExercises().stream().filter(c -> c.difficulty() > orig.difficulty()
                            && Objects.equals(c.movementPattern(), orig.movementPattern())
                            && Objects.equals(c.bodyPartCode(), orig.bodyPartCode())
                            && c.defaultPrescription() == original.prescriptionType()
                            && w.exercises().stream().noneMatch(y -> y.exerciseId().equals(c.exerciseId()))).findFirst();
                    if (mas.isPresent()) {
                        ej.set(x, conId(original, mas.get().exerciseId()));
                        hecho = "INVALIDA_SUSTITUTO_MAS_DIFICIL " + original.exerciseId() + "(d" + orig.difficulty() + ")->"
                                + mas.get().exerciseId() + "(d" + mas.get().difficulty() + ")";
                    }
                }
                if (hecho == null) {
                    ej.set(0, conSeries(ej.get(0), (ej.get(0).plannedSets() == null ? 1 : ej.get(0).plannedSets()) + 1));
                    hecho = "INVALIDA_SUBE_SERIE (sin sustituto mas dificil en el catalogo elegible) " + ej.get(0).exerciseId();
                }
                accion = hecho;
            } else {
                ej.set(0, conSeries(ej.get(0), (ej.get(0).plannedSets() == null ? 1 : ej.get(0).plannedSets()) + 1));
                accion = "INVALIDA_SUBE_SERIE " + ej.get(0).exerciseId();
            }
            acciones.add(accion);
            sesionesJson.add(sesionJson(sesion, fecha, ej));
        } else {
            for (var s : senaladas) {
                var lista = validas.getOrDefault(s.plannedWorkoutId(), List.of());
                if (lista.isEmpty()) {
                    acciones.add(s.plannedWorkoutId() + ": SIN_CANDIDATA_VALIDA (no se propone nada)");
                    continue;
                }
                var c = mejor(lista, s.intervencion());
                acciones.add(s.plannedWorkoutId() + ": " + c.clase() + " " + c.descripcion());
                sesionesJson.add(sesionJson(c.sesion(), c.fecha(), c.ejercicios()));
                if (intento == 3) break;
            }
        }
        String salida = "{\"schema_version\":\"FIX-1.0\",\"rationale\":\"IA simulada del laboratorio\",\"sessions\":["
                + String.join(",", sesionesJson) + "]}";
        traza.put("acciones", String.join(" | ", acciones));
        traza.put("resultado", "RESPUESTA_SIMULADA");
        traza.put("salida", salida);
        trazasIa.add(json(traza));
        return new ReplicateSessionFixGenerator.RawFix(prompt.snapshotJson(), salida);
    }

    /** Preferencia por causa: lo mas pertinente primero (independiente de lo que publique el backend). */
    private static Candidata mejor(List<Candidata> lista, TipoIntervencion intervencion) {
        var orden = switch (intervencion) {
            case VARIANTE_MAS_SENCILLA_O_CARGA -> List.of("SUSTITUTO_MAS_FACIL", "MENOS_CARGA", "MENOS_REPS", "MENOS_SEGUNDOS",
                    "MENOS_SERIES", "SUSTITUTO_LATERAL");
            case REVISAR_CARGA_Y_RECUPERACION -> List.of("MENOS_SERIES", "MENOS_CARGA", "MENOS_REPS", "MENOS_SEGUNDOS");
            default -> List.of("MENOS_REPS", "MENOS_SEGUNDOS", "MENOS_CARGA", "MENOS_SERIES", "SUSTITUTO_LATERAL",
                    "SUSTITUTO_MAS_FACIL");
        };
        for (var clase : orden)
            for (var c : lista) if (c.clase().equals(clase)) return c;
        return lista.get(0);
    }

    // ================================================================== SONDA DE FACTIBILIDAD

    /**
     * Enumera las modificaciones simples que permite la intervencion de cada sesion senalada para la IA y las juzga
     * con el productor real (V8C, validacion estricta, limite de dificultad). Solo mide.
     */
    private void sondear(PlanGenerationContext ctx, WeekDerivation der) {
        if (validasPorEtiqueta.containsKey(etiquetaActual)) return;
        var porSesion = new LinkedHashMap<Long, List<Candidata>>();
        validasPorEtiqueta.put(etiquetaActual, porSesion);
        var elegibles = porId(ctx.availableExercises());
        var exclusiones = exclusionesResolver.resolver(ctx);
        sondeando = true;
        try {
            for (var s : ctx.adjustment().sesionesParaIA()) {
                var d = der.from(s.plannedWorkoutId());
                if (d.isEmpty()) continue;
                var w = d.get().workout();
                var candidatas = candidatas(s, w, ctx, elegibles);
                var validas = new ArrayList<Candidata>();
                var porClase = new TreeMap<String, int[]>();
                var motivos = new TreeMap<String, Integer>();
                for (var c : candidatas) {
                    var cuenta = porClase.computeIfAbsent(c.clase(), k -> new int[2]);
                    cuenta[0]++;
                    String veredicto;
                    try {
                        var corr = correcciones.interpretar("{\"schema_version\":\"FIX-1.0\",\"rationale\":\"sonda\","
                                + "\"sessions\":[" + sesionJson(c.sesion(), c.fecha(), c.ejercicios()) + "]}", ctx);
                        var r = productor.produce(ctx, der, corr, elegibles, exclusiones);
                        if (r.publicable() && r.conservacion() != null && r.conservacion().huboAdaptacionEfectiva()) {
                            veredicto = "VALIDA";
                            cuenta[1]++;
                            validas.add(c);
                        } else veredicto = codigo(r.problemas());
                    } catch (RuntimeException e) {
                        veredicto = "CONTRATO:" + e.getClass().getSimpleName();
                    }
                    if (!"VALIDA".equals(veredicto)) motivos.merge(veredicto, 1, Integer::sum);
                }
                porSesion.put(s.plannedWorkoutId(), validas);
                coherenciaV2(ctx, der, s.plannedWorkoutId(), candidatas, validas);
                var f = new LinkedHashMap<String, Object>();
                f.put("etiqueta", etiquetaActual);
                f.put("sesion", s.plannedWorkoutId());
                f.put("intervencion", s.intervencion());
                f.put("permite_sustituir", s.intervencion().permiteSustituirEjercicio());
                f.put("permite_dosis", s.intervencion().permiteCambiarDosis());
                f.put("ejercicios", w.exercises().size());
                f.put("candidatas", candidatas.size());
                f.put("validas", validas.size());
                f.put("por_clase_probadas_validas", porClase.entrySet().stream()
                        .map(e -> e.getKey() + "=" + e.getValue()[0] + "/" + e.getValue()[1]).collect(Collectors.joining(" ")));
                f.put("motivos_rechazo", motivos.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue())
                        .collect(Collectors.joining(" ")));
                f.put("validas_detalle", validas.stream().limit(12).map(c -> c.clase() + ":" + c.descripcion())
                        .collect(Collectors.joining(" | ")));
                factibilidad.add(f);
            }
        } finally {
            sondeando = false;
        }
    }

    private List<Candidata> candidatas(PlanAdjustment.TargetedSession s, PlanDraft.DraftWorkout w,
                                       PlanGenerationContext ctx, Map<Long, CandidateExercise> elegibles) {
        var out = new ArrayList<Candidata>();
        var ej = w.exercises();
        Long id = s.plannedWorkoutId();
        for (int i = 0; i < ej.size(); i++) {
            var e = ej.get(i);
            if (s.intervencion().permiteCambiarDosis()) {
                int series = e.plannedSets() == null ? 1 : e.plannedSets();
                if (series > 1) out.add(cambio(id, w, i, conSeries(e, series - 1), "MENOS_SERIES",
                        e.exerciseId() + " series " + series + "->" + (series - 1)));
                if (e.plannedReps() != null)
                    for (int d = 1; d <= 2; d++)
                        if (e.plannedReps() - d >= 1) out.add(cambio(id, w, i, conReps(e, e.plannedReps() - d), "MENOS_REPS",
                                e.exerciseId() + " reps " + e.plannedReps() + "->" + (e.plannedReps() - d)));
                if (e.plannedDurationSeconds() != null)
                    for (int d : new int[]{5, 10})
                        if (e.plannedDurationSeconds() - d >= 10) out.add(cambio(id, w, i,
                                conSegundos(e, e.plannedDurationSeconds() - d), "MENOS_SEGUNDOS",
                                e.exerciseId() + " s " + e.plannedDurationSeconds() + "->" + (e.plannedDurationSeconds() - d)));
                if (e.targetLoadKg() != null && e.targetLoadKg().signum() > 0) {
                    var menos = e.targetLoadKg().multiply(new BigDecimal("0.9")).setScale(1, java.math.RoundingMode.HALF_UP);
                    out.add(cambio(id, w, i, conCarga(e, menos), "MENOS_CARGA",
                            e.exerciseId() + " kg " + e.targetLoadKg() + "->" + menos));
                }
            }
            if (s.intervencion().permiteSustituirEjercicio()) {
                var orig = elegibles.get(e.exerciseId());
                if (orig == null) continue;
                for (var c : ctx.availableExercises()) {
                    if (c.exerciseId().equals(e.exerciseId()) || c.defaultPrescription() != e.prescriptionType()) continue;
                    if (!Objects.equals(c.movementPattern(), orig.movementPattern())
                            || !Objects.equals(c.bodyPartCode(), orig.bodyPartCode())) continue;
                    if (ej.stream().anyMatch(x -> x.exerciseId().equals(c.exerciseId()))) continue;
                    String clase = c.difficulty() < orig.difficulty() ? "SUSTITUTO_MAS_FACIL"
                            : c.difficulty() == orig.difficulty() ? "SUSTITUTO_LATERAL" : "SUSTITUTO_MAS_DIFICIL";
                    out.add(cambio(id, w, i, conId(e, c.exerciseId()), clase,
                            e.exerciseId() + "(d" + orig.difficulty() + ")->" + c.exerciseId() + "(d" + c.difficulty() + ")"));
                }
            }
        }
        return out;
    }

    private static Candidata cambio(Long id, PlanDraft.DraftWorkout w, int i, PlanDraft.DraftExercise nuevo,
                                    String clase, String desc) {
        var ej = new ArrayList<>(w.exercises());
        ej.set(i, nuevo);
        return new Candidata(id, clase, desc, ej, w.scheduledDate());
    }

    private static String codigo(List<String> problemas) {
        if (problemas == null || problemas.isEmpty()) return "SIN_EFECTO";
        var p = problemas.get(0);
        var m = java.util.regex.Pattern.compile("(V\\d+[A-Z]?|4[A-D]|SUST-SEG-1|LIMITE[A-Z_]*)").matcher(p);
        return m.find() ? m.group(1) : p.length() > 40 ? p.substring(0, 40) : p;
    }

    private static Map<Long, CandidateExercise> porId(List<CandidateExercise> lista) {
        var m = new LinkedHashMap<Long, CandidateExercise>();
        if (lista != null) lista.forEach(c -> m.putIfAbsent(c.exerciseId(), c));
        return m;
    }

    private static PlanDraft.DraftExercise conSeries(PlanDraft.DraftExercise x, int series) {
        return new PlanDraft.DraftExercise(x.exerciseId(), x.prescriptionType(), (short) series, x.plannedReps(),
                x.plannedDurationSeconds(), x.targetLoadKg(), x.restSeconds(), null);
    }

    private static PlanDraft.DraftExercise conReps(PlanDraft.DraftExercise x, int reps) {
        return new PlanDraft.DraftExercise(x.exerciseId(), x.prescriptionType(), x.plannedSets(), (short) reps,
                x.plannedDurationSeconds(), x.targetLoadKg(), x.restSeconds(), null);
    }

    private static PlanDraft.DraftExercise conSegundos(PlanDraft.DraftExercise x, int s) {
        return new PlanDraft.DraftExercise(x.exerciseId(), x.prescriptionType(), x.plannedSets(), x.plannedReps(), s,
                x.targetLoadKg(), x.restSeconds(), null);
    }

    private static PlanDraft.DraftExercise conCarga(PlanDraft.DraftExercise x, BigDecimal kg) {
        return new PlanDraft.DraftExercise(x.exerciseId(), x.prescriptionType(), x.plannedSets(), x.plannedReps(),
                x.plannedDurationSeconds(), kg, x.restSeconds(), null);
    }

    private static PlanDraft.DraftExercise conId(PlanDraft.DraftExercise x, Long id) {
        return new PlanDraft.DraftExercise(id, x.prescriptionType(), x.plannedSets(), x.plannedReps(),
                x.plannedDurationSeconds(), null, x.restSeconds(), null);
    }

    private static String sesionJson(Long id, LocalDate fecha, List<PlanDraft.DraftExercise> ej) {
        return "{\"source_planned_workout_id\":" + id + ",\"scheduled_date\":\"" + fecha
                + "\",\"expected_duration_minutes\":null,\"change_reason\":\"laboratorio\",\"exercises\":["
                + ej.stream().map(x -> "{\"exercise_id\":" + x.exerciseId() + ",\"prescription_type\":\""
                        + x.prescriptionType() + "\",\"planned_sets\":" + x.plannedSets() + ",\"planned_reps\":" + x.plannedReps()
                        + ",\"planned_duration_seconds\":" + x.plannedDurationSeconds() + ",\"target_load_kg\":"
                        + x.targetLoadKg() + ",\"rest_seconds\":" + x.restSeconds() + ",\"notes\":null}")
                .collect(Collectors.joining(",")) + "]}";
    }

    static final List<Map<String, Object>> coherencia = Collections.synchronizedList(new ArrayList<>());

    /**
     * Lo que la factibilidad V2 (texto que recibe el modelo) dice de cada cambio simple, frente al veredicto REAL del
     * productor y los validadores sobre el mismo cambio (sonda). Una discrepancia es informacion falsa en el prompt.
     */
    private void coherenciaV2(PlanGenerationContext ctx, WeekDerivation der, Long sesion, List<Candidata> candidatas,
                              List<Candidata> validas) {
        var f = main.web.services.fitsense.planning.infrastructure.generation.ai.FactibilidadAdaptacion.calcular(ctx, der)
                .stream().filter(x -> x.plannedWorkoutId().equals(sesion)).findFirst().orElse(null);
        var resumen = new LinkedHashMap<String, Object>();
        resumen.put("etiqueta", etiquetaActual);
        resumen.put("sesion", sesion);
        resumen.put("elemento", "SESION");
        resumen.put("v2", f == null ? "sin bloque" : "margen=" + f.margen() + " hayCambioFactible=" + f.hayCambioFactible());
        resumen.put("sonda", "validas=" + validas.size());
        resumen.put("coincide", f == null ? "" : String.valueOf(f.hayCambioFactible() == !validas.isEmpty()));
        coherencia.add(resumen);
        if (f == null) return;
        for (var o : f.opciones()) {
            var igual = candidatas.stream().filter(c -> c.descripcion().startsWith(o.exerciseId() + " ")
                    && dosisDe(c, o.exerciseId()).equals(o.despues())).findFirst();
            var m = new LinkedHashMap<String, Object>();
            m.put("etiqueta", etiquetaActual);
            m.put("sesion", sesion);
            m.put("elemento", "OPCION " + o.exerciseId() + " " + o.antes() + "->" + o.despues() + " retira " + o.segundosRetirados());
            m.put("v2", o.cabe() ? "cabe" : "NO cabe");
            m.put("sonda", igual.isEmpty() ? "no probada" : validas.contains(igual.get()) ? "VALIDA" : "RECHAZADA");
            m.put("coincide", igual.isEmpty() ? "" : String.valueOf(o.cabe() == validas.contains(igual.get())));
            coherencia.add(m);
        }
        for (var x : f.sustitutos()) {
            var igual = candidatas.stream().filter(c -> c.descripcion().startsWith(x.reemplaza() + "(")
                    && c.descripcion().contains(")->" + x.exerciseId() + "(")).findFirst();
            var m = new LinkedHashMap<String, Object>();
            m.put("etiqueta", etiquetaActual);
            m.put("sesion", sesion);
            m.put("elemento", "SUSTITUTO " + x.reemplaza() + "->" + x.exerciseId());
            m.put("v2", x.mismaDosis() ? "misma dosis" : "otra dosis");
            m.put("sonda", igual.isEmpty() ? "no probada (otra zona o tipo)" : validas.contains(igual.get()) ? "VALIDA" : "RECHAZADA");
            m.put("coincide", igual.isEmpty() || !x.mismaDosis() ? "" : String.valueOf(validas.contains(igual.get())));
            coherencia.add(m);
        }
    }

    private static String dosisDe(Candidata c, Long exerciseId) {
        var e = c.ejercicios().stream().filter(x -> x.exerciseId().equals(exerciseId)).findFirst().orElseThrow();
        if (e.prescriptionType() == PrescriptionType.DURATION)
            return (e.plannedSets() == null ? "" : e.plannedSets() + (e.plannedSets() == 1 ? " serie de " : " series de "))
                    + e.plannedDurationSeconds() + " s";
        return e.plannedSets() + "x" + e.plannedReps();
    }

    // ================================================================== exportacion para el oraculo

    static final Set<String> OMITIDAS = Set.of("users.password_hash", "exercises.instructions_es",
            "exercises.instruction_steps_es", "exercises.instructions_en", "exercises.image_path", "exercises.gif_path",
            "exercises.media_attribution");
    static final Set<String> HUELLA = Set.of("weekly_training_plans.input_snapshot", "weekly_training_plans.output_snapshot");

    /** Todas las tablas de la base efimera (solo contiene los usuarios de esta ejecucion y el catalogo). */
    private void exportarTablas() throws IOException {
        var dir = SALIDA.resolve("tablas");
        Files.createDirectories(dir);
        var tablas = jdbc.queryForList("SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'"
                + " AND table_type = 'BASE TABLE' AND table_name <> 'flyway_schema_history' ORDER BY table_name", String.class);
        for (var t : tablas) {
            var cols = jdbc.queryForList("SELECT column_name FROM information_schema.columns WHERE table_schema = 'public'"
                    + " AND table_name = ? ORDER BY ordinal_position", String.class, t);
            // Sin textos del catalogo ni la contrasena cifrada; las instantaneas JSON de los planes (MB) van como huella.
            cols = cols.stream().filter(c -> !OMITIDAS.contains(t + "." + c)).toList();
            String sel = cols.stream().map(c -> HUELLA.contains(t + "." + c)
                    ? "md5(\"" + c + "\"::text) || ':' || length(\"" + c + "\"::text) AS \"" + c + "\""
                    : "\"" + c + "\"::text AS \"" + c + "\"").collect(Collectors.joining(", "));
            var filas = jdbc.queryForList("SELECT " + sel + " FROM " + t + " ORDER BY 1");
            var out = new ArrayList<String>();
            out.add(String.join(",", cols));
            for (var f : filas) out.add(cols.stream().map(c -> celda(f.get(c))).collect(Collectors.joining(",")));
            Files.write(dir.resolve(t + ".csv"), out, StandardCharsets.UTF_8);
        }
    }

    // ================================================================== ficheros

    private static String sha256(byte[] b) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));
        } catch (Exception e) {
            return "?";
        }
    }

    private static String json(Map<String, ?> m) {
        return m.entrySet().stream().map(e -> "\"" + e.getKey() + "\":" + (e.getValue() instanceof Number
                ? e.getValue() : "\"" + String.valueOf(e.getValue()).replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n") + "\"")).collect(Collectors.joining(",", "{", "}"));
    }

    private List<Perfil> leerPerfiles() throws IOException {
        var lineas = Files.readAllLines(PERFILES);
        var cab = parse(lineas.get(0));
        var out = new ArrayList<Perfil>();
        for (var l : lineas.subList(1, lineas.size())) {
            if (l.isBlank()) continue;
            var v = parse(l);
            var m = new LinkedHashMap<String, String>();
            for (int i = 0; i < cab.size(); i++) m.put(cab.get(i), i < v.size() ? v.get(i) : "");
            out.add(new Perfil(m));
        }
        if (PERFILES_OTROS.isBlank()) assertEquals(20, out.size(), "20 perfiles congelados");
        return out;
    }

    private static List<String> parse(String l) {
        var out = new ArrayList<String>();
        var sb = new StringBuilder();
        boolean q = false;
        for (int i = 0; i < l.length(); i++) {
            char c = l.charAt(i);
            if (c == '"') {
                if (q && i + 1 < l.length() && l.charAt(i + 1) == '"') { sb.append('"'); i++; } else q = !q;
            } else if (c == ',' && !q) { out.add(sb.toString()); sb.setLength(0); } else sb.append(c);
        }
        out.add(sb.toString());
        return out;
    }

    private void csv(String nombre, List<Map<String, Object>> filas) throws IOException {
        var cols = new LinkedHashSet<String>();
        filas.forEach(f -> cols.addAll(f.keySet()));
        var out = new ArrayList<String>();
        out.add(String.join(",", cols));
        for (var f : filas)
            out.add(cols.stream().map(c -> celda(f.get(c))).collect(Collectors.joining(",")));
        escribir(nombre, out);
    }

    private static String celda(Object o) {
        if (o == null) return "";
        String s = String.valueOf(o);
        return s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")
                ? "\"" + s.replace("\"", "\"\"") + "\"" : s;
    }

    private void escribir(String nombre, List<String> lineas) throws IOException {
        Files.write(SALIDA.resolve(nombre), lineas, StandardCharsets.UTF_8);
    }
}
