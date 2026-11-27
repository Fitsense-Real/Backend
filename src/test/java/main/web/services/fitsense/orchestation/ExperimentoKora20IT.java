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
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.domain.services.PlanDraftValidator;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
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

/**
 * Experimento del short paper IEEE (Kora): 20 usuarios sinteticos x 3 sesiones, con los servicios reales.
 * <p>
 * Protocolo congelado en {@code docs/FitSense/short_paper_ieee/experimento_final_20_usuarios/02_PROTOCOLO_...md}; los
 * perfiles se leen de {@code 03_PERFILES_U01_U20.csv} (fuente unica, no se repiten aqui). Opt-in:
 * <pre>
 *   -Dfitsense.experimento.kora20=true
 *   -Dfitsense.experimento.kora20.salida=docs/FitSense/short_paper_ieee/experimento_final_20_usuarios/evidencia/A0
 *   -Dfitsense.experimento.kora20.modo=DETERMINISTA | REPLICATE
 *   -Dfitsense.experimento.kora20.commit=$(git rev-parse HEAD)
 * </pre>
 * La carpeta de salida debe no existir o estar vacia: una ejecucion nunca sobrescribe otra.
 * <p>
 * REPLICATE (etapa C): exige {@code REPLICATE_API_TOKEN} en el entorno y una base local. Limite: 90 solicitudes al
 * proveedor (cada intento es una prediccion) y USD 10; lo que llegue antes detiene las llamadas. Los usuarios
 * U01-U20 se crean primero con la generacion por IA DESACTIVADA (mismos userId y semillas que en la etapa A, luego el
 * mismo plan inicial) y la IA real interviene en su adaptacion; despues 20 gemelos miden la generacion inicial con IA.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.adaptation.progression.enabled=false"
})
@EnabledIfSystemProperty(named = "fitsense.experimento.kora20", matches = "true")
@DisplayName("Experimento Kora: 20 usuarios x 3 sesiones con los servicios reales")
class ExperimentoKora20IT {

    static final String MODO = System.getProperty("fitsense.experimento.kora20.modo", "DETERMINISTA");
    static final boolean IA = "REPLICATE".equals(MODO);
    /**
     * {@code proxy}: la credencial de Replicate la inyecta el proxy del entorno (secreto de red) en las peticiones a
     * api.replicate.com; el laboratorio nunca la ve. El backend exige un token no vacio, asi que recibe un marcador.
     */
    static final boolean CREDENCIAL_PROXY = "proxy".equals(System.getProperty("fitsense.experimento.kora20.credencial", ""));
    static final Path DOCS = Path.of("docs/FitSense/short_paper_ieee/experimento_final_20_usuarios");
    static final Path SALIDA = Path.of(System.getProperty("fitsense.experimento.kora20.salida", "target/kora20"));
    static final LocalDate S1 = LocalDate.of(2026, 11, 2);
    static final LocalDate S2 = S1.plusWeeks(1);
    /** Predicciones de ESTA ejecucion: el total autorizado (90) menos las ya consumidas en intentos anteriores. */
    static final int MAX_LLAMADAS = Integer.getInteger("fitsense.experimento.kora20.max-llamadas", 90);
    /** Variacion de transporte registrada (anexo 2): espera sincrona del POST antes de pasar al sondeo. */
    static final String PREFER_WAIT = System.getProperty("fitsense.experimento.kora20.prefer-wait", "");
    /** Coste maximo supuesto por prediccion (conservador; la campana MVP observo ~US$ 0,02): 90 x 0,10 = 9 < 10. */
    static final double COSTE_MAX_POR_LLAMADA_USD = 0.10;

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @DynamicPropertySource
    static void ia(DynamicPropertyRegistry r) {
        r.add("fitsense.ai.enabled", () -> String.valueOf(IA));
        if (IA && !PREFER_WAIT.isBlank()) r.add("fitsense.ai.prefer-wait-seconds", () -> PREFER_WAIT);
        r.add("fitsense.ai.api-token", () -> !IA ? "" : CREDENCIAL_PROXY ? "marcador-credencial-inyectada-por-el-proxy"
                : Objects.requireNonNullElse(System.getenv("REPLICATE_API_TOKEN"), ""));
    }

    /** El reloj lo controla el laboratorio: sin barrido horario (ver protocolo §5). */
    @MockitoBean main.web.services.fitsense.planning.infrastructure.scheduling.OverdueWorkoutScheduler vencidas;
    @MockitoSpyBean PlanDraftValidator validador;
    @MockitoSpyBean ReplicateTrainingPlanGenerator generador;
    @MockitoSpyBean ReplicateSessionFixGenerator correcciones;

    @Autowired UserCommandService cuentas;
    @Autowired UserProfileCommandService perfiles;
    @Autowired PlanningContextFacade planning;
    @Autowired WorkoutSessionCommandService sesiones;
    @Autowired TrainingPlanController planes;
    @Autowired UserRepository usuarios;
    @Autowired WeeklyCycleService ciclo;
    @Autowired JdbcTemplate jdbc;
    /** Lo que ve el cliente en GET /users/me/plan/adaptations (laboratorio v2). */
    @Autowired main.web.services.fitsense.planning.application.internal.queryservices.AdaptationOutcomeQueryService resultadosApi;

    // ---- trazas por usuario (las llena el espia del validador y del proveedor)
    static final AtomicInteger validaciones = new AtomicInteger();
    static final List<String> rechazosValidacion = Collections.synchronizedList(new ArrayList<>());
    static final AtomicInteger llamadas = new AtomicInteger();
    static volatile String etiquetaActual = "";
    static final List<String> trazasIa = Collections.synchronizedList(new ArrayList<>());

    record Perfil(Map<String, String> c) {
        String get(String k) { return c.get(k); }
    }

    @Test
    void experimento() throws Exception {
        // ------------------------------------------------------------ guardas previas
        String url = postgres.getJdbcUrl();
        assertTrue(url.startsWith("jdbc:postgresql://localhost:") || url.startsWith("jdbc:postgresql://127.0.0.1:"),
                "solo base efimera local: " + url);
        assertEquals(url.replaceAll("\\?.*", ""), Objects.requireNonNull(jdbc.getDataSource()).getConnection()
                .getMetaData().getURL().replaceAll("\\?.*", ""), "el contexto usa la base efimera");
        int cuenta = -1;
        if (IA && CREDENCIAL_PROXY) {
            // Comprobacion GRATUITA (no es una prediccion): la cuenta responde 200 solo si el proxy inyecta credencial.
            var resp = java.net.http.HttpClient.newHttpClient().send(java.net.http.HttpRequest.newBuilder(
                    java.net.URI.create("https://api.replicate.com/v1/account")).GET().build(),
                    java.net.http.HttpResponse.BodyHandlers.discarding());
            cuenta = resp.statusCode();
            assertEquals(200, cuenta, "BLOQUEADO: el proxy no autentica en Replicate (ninguna llamada realizada)");
        } else if (IA) assertFalse(Objects.requireNonNullElse(System.getenv("REPLICATE_API_TOKEN"), "").isBlank(),
                "BLOQUEADO: sin REPLICATE_API_TOKEN no se ejecuta la etapa C (ninguna llamada realizada)");
        if (Files.exists(SALIDA)) try (var s = Files.list(SALIDA)) {
            assertTrue(s.findAny().isEmpty(), "la carpeta " + SALIDA + " ya tiene evidencia: no se sobrescribe");
        }
        Files.createDirectories(SALIDA);
        espiar();

        var lista = leerPerfiles();
        var cfg = new LinkedHashMap<String, Object>();
        cfg.put("modo", MODO);
        cfg.put("commit", System.getProperty("fitsense.experimento.kora20.commit", "no indicado"));
        cfg.put("fecha_ejecucion_utc", java.time.Instant.now().toString());
        cfg.put("base", "PostgreSQL efimero " + jdbc.queryForObject("SHOW server_version", String.class));
        cfg.put("flyway_version", jdbc.queryForObject("SELECT version FROM flyway_schema_history WHERE success"
                + " ORDER BY installed_rank DESC LIMIT 1", String.class));
        cfg.put("migraciones", jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE success", Integer.class));
        cfg.put("ejercicios_activos", jdbc.queryForObject("SELECT count(*) FROM exercises WHERE is_active", Integer.class));
        cfg.put("ia_habilitada", generador.isEnabled());
        cfg.put("modelo", generador.modelName());
        cfg.put("credencial", !IA ? "ninguna" : CREDENCIAL_PROXY ? "secreto de red inyectado por el proxy (no visible)" : "variable REPLICATE_API_TOKEN");
        cfg.put("comprobacion_cuenta_http", cuenta);
        cfg.put("prefer_wait_seconds", PREFER_WAIT.isBlank() ? "60 (valor por defecto)" : PREFER_WAIT + " (variacion registrada, anexo 2)");
        cfg.put("limite", MAX_LLAMADAS + " predicciones / USD 10 (supuesto max " + COSTE_MAX_POR_LLAMADA_USD + " por llamada)");
        cfg.put("politica_por_defecto", "PLAN-HIST-1 (allow-candidates=false)");
        cfg.put("progresion", "false");
        escribir("00_configuracion.txt", cfg.entrySet().stream().map(e -> e.getKey() + ": " + e.getValue()).toList());

        var gen = new ArrayList<Map<String, Object>>();
        var planIni = new ArrayList<Map<String, Object>>();
        var ejec = new ArrayList<Map<String, Object>>();
        var adh = new ArrayList<Map<String, Object>>();
        var dec = new ArrayList<Map<String, Object>>();
        var sig = new ArrayList<Map<String, Object>>();
        var res = new ArrayList<Map<String, Object>>();
        var incid = new ArrayList<String>();

        for (var p : lista) ejecutarUsuario(p, false, gen, planIni, ejec, adh, dec, sig, res, incid);
        if (IA) for (var p : lista) ejecutarGemelo(p, gen, planIni, incid);

        csv("generacion.csv", gen);
        csv("plan_inicial.csv", planIni);
        csv("ejecucion.csv", ejec);
        csv("adherencia.csv", adh);
        csv("decisiones.csv", dec);
        csv("plan_siguiente.csv", sig);
        csv("resultados_20.csv", res);
        escribir("incidencias.log", incid);
        if (IA) escribir("trazas_ia.jsonl", trazasIa);
        escribir("99_resumen.txt", List.of("usuarios=" + res.size(), "llamadas_proveedor=" + llamadas.get(),
                "incidencias=" + incid.size()));
        System.out.println("KORA20 " + MODO + " -> " + SALIDA + " usuarios=" + res.size() + " llamadas=" + llamadas.get());
    }

    // ================================================================== un usuario

    private void ejecutarUsuario(Perfil p, boolean gemelo, List<Map<String, Object>> gen, List<Map<String, Object>> planIni,
                                 List<Map<String, Object>> ejec, List<Map<String, Object>> adh,
                                 List<Map<String, Object>> dec, List<Map<String, Object>> sig,
                                 List<Map<String, Object>> res, List<String> incid) {
        String id = p.get("id");
        etiquetaActual = id + "/adaptacion";
        var r = new LinkedHashMap<String, Object>();
        r.put("id", id);
        r.put("objetivo", p.get("objetivo"));
        r.put("patron", p.get("patron") + " " + p.get("secuencia"));
        r.put("perfil", p.get("nivel") + ", " + p.get("minutos_sesion") + " min, " + p.get("ubicacion") + " ("
                + p.get("equipamiento").replace("|", ", ") + ")");
        long u;
        try {
            u = alta(p, id.toLowerCase());
        } catch (RuntimeException e) {
            incid.add(id + " ALTA ERROR " + e);
            r.put("generacion_inicial", "ERROR alta/perfil: " + e.getMessage());
            res.add(r);
            return;
        }
        r.put("user_id", u);
        r.put("semilla", u * 1_000_003L + S1.toEpochDay());

        // ---------------------------------------------------------- generacion inicial
        validaciones.set(0);
        rechazosValidacion.clear();
        Optional<Long> plan;
        String errorGen = null;
        if (IA) doReturn(false).when(generador).isEnabled(); // copia controlada: mismo plan inicial que la etapa A
        try {
            plan = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null);
        } catch (RuntimeException e) {
            plan = Optional.empty();
            errorGen = e.getClass().getSimpleName() + ": " + e.getMessage();
        } finally {
            if (IA) doCallRealMethod().when(generador).isEnabled();
        }
        gen.add(filaGeneracion(id, "U", u, plan, errorGen));
        if (plan.isEmpty()) {
            incid.add(id + " GENERACION sin plan: " + errorGen);
            r.put("generacion_inicial", errorGen == null ? "SIN PLAN" : "ERROR: " + errorGen);
            res.add(r);
            return;
        }
        long planId = plan.get();
        r.put("generacion_inicial", "PUBLICADO (plan " + planId + ")");
        var medidas = sesionesDe(planId);
        planIni.addAll(detallePlan(id, "inicial", planId));
        if (medidas.size() != 3) incid.add(id + " el plan inicial tiene " + medidas.size() + " sesiones, no 3");

        // ---------------------------------------------------------- ejecucion
        String[] patron = p.get("secuencia").split("/");
        String[] motivos = {"", p.get("motivo_omision_s2"), p.get("motivo_omision_s3")};
        RelojPruebas.abrirVentanaDeEjecucion(jdbc, u);
        for (int i = 0; i < medidas.size() && i < 3; i++) {
            long w = medidas.get(i);
            try {
                switch (patron[i]) {
                    case "H" -> hacer(u, w, 1.0, (short) 6, (short) 4);
                    case "P40" -> hacer(u, w, 0.4, (short) 5, (short) 3);
                    case "O" -> planes.skip(UserDetailsImpl.build(usuarios.findById(u).orElseThrow()), w,
                            new SkipWorkoutResource(SkipReason.valueOf(motivos[i])));
                    case "N" -> { }
                    default -> throw new IllegalStateException("patron desconocido " + patron[i]);
                }
            } catch (RuntimeException e) {
                incid.add(id + " EJECUCION S" + (i + 1) + " " + patron[i] + " ERROR " + e);
            }
            ejec.addAll(filasEjecucion(id, i + 1, patron[i], i < 3 ? motivos[i] : "", w));
        }

        // ---------------------------------------------------------- cierre semanal
        Optional<Long> siguiente = Optional.empty();
        String errorCiclo = null;
        int llamadasAntes = llamadas.get();
        try {
            siguiente = ciclo.runFor(u, S1, S2);
        } catch (RuntimeException e) {
            errorCiclo = e.getClass().getSimpleName() + ": " + e.getMessage();
            incid.add(id + " CICLO ERROR " + errorCiclo);
        }
        var m = jdbc.queryForList("SELECT weighted_adherence_pct, observed_adherence_pct, registration_coverage_pct,"
                + " scheduled_workouts, valid_workouts, skipped_workouts, completed_workouts, recorded_workouts,"
                + " frequency_adherence_pct, workout_adherence_pct, exercise_adherence_pct, average_session_rpe"
                + " FROM weekly_user_metrics WHERE user_id = ? AND week_start_date = ?", u, S1);
        var metr = m.isEmpty() ? Map.<String, Object>of() : m.get(0);
        var indep = recalculo(medidas);
        var a = new LinkedHashMap<String, Object>();
        a.put("id", id);
        a.put("Aw_backend", metr.get("weighted_adherence_pct"));
        a.put("Aobs_backend", metr.get("observed_adherence_pct"));
        a.put("C_backend", metr.get("registration_coverage_pct"));
        a.put("Aw_recalculo_independiente", indep[0]);
        a.put("Aobs_recalculo_independiente", indep[1]);
        a.put("C_recalculo_independiente", indep[2]);
        a.put("cumplimiento_por_sesion_recalculado", indep[3]);
        a.putAll(metr);
        adh.add(a);

        var v32 = jdbc.queryForList("SELECT attempt_id, decision, status, generation_source, ai_calls,"
                + " targeted_sessions::text, targeted_interventions::text, applied_sessions::text,"
                + " no_effect_sessions::text, conserved_sessions::text, resulting_plan_id, rejection_reason,"
                + " reason_codes::text, conservation_report::text FROM plan_adaptation_attempts WHERE user_id = ?"
                + " ORDER BY attempt_id", u);
        var d = new LinkedHashMap<String, Object>();
        d.put("id", id);
        d.put("filas_v32", v32.size());
        if (!v32.isEmpty()) d.putAll(v32.get(v32.size() - 1));
        d.put("indices_senalados", indices(medidas, (String) d.get("targeted_sessions")));
        d.put("llamadas_ia_ciclo", llamadas.get() - llamadasAntes);
        d.put("error_ciclo", errorCiclo);
        var api = resultadosApi.history(u);
        if (!api.isEmpty()) {
            var o = api.get(0);
            d.put("api_resultado", o.resultado());
            d.put("api_estado", o.estado());
            d.put("api_estado_publicacion", o.estadoPublicacion());
            d.put("api_motivo_codigo", o.motivoCodigo());
            d.put("api_motivos", String.join(" || ", o.motivos()));
        }
        d.put("api_filas", api.size());
        dec.add(d);

        r.put("Aw", metr.get("weighted_adherence_pct"));
        r.put("Aobs", metr.get("observed_adherence_pct"));
        r.put("C", metr.get("registration_coverage_pct"));
        r.put("decision", d.get("decision"));
        r.put("estado_v32", d.get("status"));
        r.put("sesiones_senaladas", d.get("indices_senalados"));
        r.put("llamadas_ia", d.get("ai_calls"));
        r.put("resultado_api", d.get("api_resultado"));
        if (siguiente.isPresent()) {
            long nuevo = siguiente.get();
            var nuevas = sesionesDe(nuevo);
            planIni.addAll(detallePlan(id, "siguiente", nuevo));
            var cambiadas = new ArrayList<String>();
            for (int i = 0; i < Math.max(medidas.size(), nuevas.size()); i++) {
                String antes = i < medidas.size() ? firma(medidas.get(i), 0) : "-";
                String despues = i < nuevas.size() ? firma(nuevas.get(i), -7) : "-";
                var f = new LinkedHashMap<String, Object>();
                f.put("id", id);
                f.put("sesion", "S" + (i + 1));
                f.put("plan_medido", planId);
                f.put("plan_siguiente", nuevo);
                f.put("igual_a_la_medida", antes.equals(despues));
                f.put("medida", antes);
                f.put("siguiente", despues);
                sig.add(f);
                if (!antes.equals(despues)) cambiadas.add("S" + (i + 1));
            }
            r.put("plan_siguiente", "PUBLICADO (plan " + nuevo + ", " + jdbc.queryForObject("SELECT generation_source"
                    + " FROM weekly_training_plans WHERE plan_id = ?", String.class, nuevo) + ")");
            r.put("sesiones_cambiadas", cambiadas.isEmpty() ? "ninguna" : String.join("|", cambiadas));
        } else {
            r.put("plan_siguiente", "NO PUBLICADO" + (errorCiclo == null ? "" : " (" + errorCiclo + ")"));
            r.put("sesiones_cambiadas", "-");
        }
        r.put("violaciones_linaje", violaciones());
        res.add(r);
    }

    /** Etapa C: la generacion inicial con IA de un gemelo del perfil (solo se mide la generacion). */
    private void ejecutarGemelo(Perfil p, List<Map<String, Object>> gen, List<Map<String, Object>> planIni,
                                List<String> incid) {
        String id = p.get("id");
        etiquetaActual = id + "G/generacion";
        long u;
        try {
            u = alta(p, id.toLowerCase() + "-gemelo");
        } catch (RuntimeException e) {
            incid.add(id + "G ALTA ERROR " + e);
            return;
        }
        validaciones.set(0);
        rechazosValidacion.clear();
        Optional<Long> plan;
        String error = null;
        try {
            plan = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null);
        } catch (RuntimeException e) {
            plan = Optional.empty();
            error = e.getClass().getSimpleName() + ": " + e.getMessage();
        }
        gen.add(filaGeneracion(id + "G", "GEMELO_IA", u, plan, error));
        plan.ifPresent(pl -> planIni.addAll(detallePlan(id + "G", "inicial_ia", pl)));
    }

    // ================================================================== acciones

    private long alta(Perfil p, String sufijo) {
        var user = cuentas.handle(new SignUpCommand("kora20-" + sufijo + "@pruebas.invalid", "clave-de-laboratorio-123",
                "Sintetico", p.get("id"), p.get("zona_horaria"), true)).orElseThrow();
        long u = user.getId();
        var dias = Arrays.stream(p.get("dias_disponibles").split(" ")[0].split("\\|")).map(Short::valueOf).toList();
        String objetivoPeso = p.get("peso_objetivo_kg");
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.parse(p.get("nacimiento")),
                BiologicalSex.valueOf(p.get("sexo")), new BigDecimal(p.get("altura_cm")), new BigDecimal(p.get("peso_kg")),
                objetivoPeso == null || objetivoPeso.isBlank() ? null : new BigDecimal(objetivoPeso),
                FitnessLevel.valueOf(p.get("nivel")), GoalType.valueOf(p.get("goal_type")), "Experimento Kora " + p.get("id"),
                TrainingLocation.valueOf(p.get("ubicacion")), Short.valueOf(p.get("dias_por_semana")), dias,
                Short.valueOf(p.get("minutos_sesion")), List.of(p.get("equipamiento").split("\\|")), List.of(),
                p.get("notas_salud"), true, true, true, true, true));
        return u;
    }

    /** Registra cada ejercicio prescrito con la fraccion indicada de su dosis total (redondeo half-up). */
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

    // ================================================================== captura

    private Map<String, Object> filaGeneracion(String id, String tipo, long u, Optional<Long> plan, String error) {
        var g = new LinkedHashMap<String, Object>();
        g.put("id", id);
        g.put("tipo", tipo);
        g.put("user_id", u);
        g.put("semilla", u * 1_000_003L + S1.toEpochDay());
        g.put("resultado", plan.isPresent() ? "PUBLICADO" : (error != null && error.contains("PlanGeneration")
                ? "NO_FACTIBLE" : error != null ? "ERROR" : "SIN_PLAN"));
        g.put("error", error);
        g.put("validaciones_ejecutadas", validaciones.get());
        g.put("validaciones_rechazadas", rechazosValidacion.size());
        g.put("motivos_rechazo", String.join(" || ", rechazosValidacion));
        if (plan.isPresent()) {
            var w = jdbc.queryForMap("SELECT plan_id, status, generation_source, model_name, generation_attempts,"
                    + " planning_policy, planned_days_count FROM weekly_training_plans WHERE plan_id = ?", plan.get());
            g.putAll(w);
            g.put("sesiones", jdbc.queryForObject("SELECT count(*) FROM planned_workouts WHERE plan_id = ?", Integer.class,
                    plan.get()));
            g.put("estructura", String.join(" | ", jdbc.queryForList("SELECT scheduled_date || ' ' || focus_code || ' '"
                    + " || expected_duration_minutes || 'min ' || (SELECT count(*) FROM planned_workout_exercises e"
                    + " WHERE e.planned_workout_id = pw.planned_workout_id) || 'ej' FROM planned_workouts pw"
                    + " WHERE plan_id = ? ORDER BY scheduled_date", String.class, plan.get())));
        }
        return g;
    }

    private List<Long> sesionesDe(long planId) {
        return jdbc.queryForList("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ? ORDER BY scheduled_date",
                Long.class, planId);
    }

    private List<Map<String, Object>> detallePlan(String id, String cual, long planId) {
        var filas = jdbc.queryForList("SELECT pw.planned_workout_id, pw.scheduled_date, pw.focus_code,"
                + " pw.expected_duration_minutes, pwe.exercise_order, pwe.exercise_id, coalesce(ex.name_es, ex.name_en) AS ejercicio,"
                + " pwe.prescription_type, pwe.planned_sets, pwe.planned_reps, pwe.planned_duration_seconds,"
                + " pwe.rest_seconds FROM planned_workouts pw JOIN planned_workout_exercises pwe"
                + " ON pwe.planned_workout_id = pw.planned_workout_id JOIN exercises ex ON ex.exercise_id = pwe.exercise_id"
                + " WHERE pw.plan_id = ? ORDER BY pw.scheduled_date, pwe.exercise_order", planId);
        var out = new ArrayList<Map<String, Object>>();
        for (var f : filas) {
            var m = new LinkedHashMap<String, Object>();
            m.put("id", id);
            m.put("plan", cual);
            m.put("plan_id", planId);
            m.putAll(f);
            out.add(m);
        }
        return out;
    }

    private List<Map<String, Object>> filasEjecucion(String id, int n, String patron, String motivo, long w) {
        var out = new ArrayList<Map<String, Object>>();
        var pw = jdbc.queryForMap("SELECT status, skip_reason FROM planned_workouts WHERE planned_workout_id = ?", w);
        var ses = jdbc.queryForList("SELECT session_id, status, completion_percentage, counts_toward_adherence,"
                + " session_rpe FROM workout_sessions WHERE planned_workout_id = ? ORDER BY session_id DESC LIMIT 1", w);
        var ejercicios = jdbc.queryForList("SELECT pwe.planned_exercise_id, pwe.exercise_id, pwe.prescription_type,"
                + " pwe.planned_sets, pwe.planned_reps, pwe.planned_duration_seconds, wse.actual_sets,"
                + " wse.actual_reps_total, wse.actual_duration_seconds, wse.completion_percentage AS pct_ejercicio,"
                + " wse.status AS estado_ejercicio FROM planned_workout_exercises pwe LEFT JOIN workout_session_exercises wse"
                + " ON wse.planned_exercise_id = pwe.planned_exercise_id"
                + (ses.isEmpty() ? " AND false" : " AND wse.session_id = " + ses.get(0).get("session_id"))
                + " WHERE pwe.planned_workout_id = ? ORDER BY pwe.exercise_order", w);
        for (var e : ejercicios) {
            var m = new LinkedHashMap<String, Object>();
            m.put("id", id);
            m.put("sesion", "S" + n);
            m.put("patron", patron);
            m.put("motivo_omision", motivo);
            m.put("planned_workout_id", w);
            m.put("estado_planificada", pw.get("status"));
            m.put("skip_reason", pw.get("skip_reason"));
            m.put("session_id", ses.isEmpty() ? null : ses.get(0).get("session_id"));
            m.put("estado_sesion", ses.isEmpty() ? null : ses.get(0).get("status"));
            m.put("pct_sesion", ses.isEmpty() ? null : ses.get(0).get("completion_percentage"));
            m.put("cuenta_adherencia", ses.isEmpty() ? null : ses.get(0).get("counts_toward_adherence"));
            boolean duracion = "DURATION".equals(e.get("prescription_type"));
            int series = e.get("planned_sets") == null ? 1 : ((Number) e.get("planned_sets")).intValue();
            Object base = e.get(duracion ? "planned_duration_seconds" : "planned_reps");
            m.put("dosis_prescrita", base == null ? null : series * ((Number) base).intValue());
            m.put("dosis_ejecutada", e.get(duracion ? "actual_duration_seconds" : "actual_reps_total"));
            m.putAll(e);
            out.add(m);
        }
        return out;
    }

    /**
     * Recalculo independiente (sin funciones del backend), con las definiciones del articulo: ejercicio = min(100,
     * 100 x ejecutada / prescrita); sesion = media de sus ejercicios prescritos (sin registro = 0); Aw = media de las 3;
     * Aobs = media de las que tienen registro (sesion terminada u omision declarada con motivo); C = con registro / 3.
     */
    private Object[] recalculo(List<Long> medidas) {
        double suma = 0, sumaObs = 0;
        int conRegistro = 0;
        var porSesion = new ArrayList<String>();
        for (long w : medidas) {
            var pw = jdbc.queryForMap("SELECT status, skip_reason FROM planned_workouts WHERE planned_workout_id = ?", w);
            var ses = jdbc.queryForList("SELECT session_id FROM workout_sessions WHERE planned_workout_id = ?"
                    + " AND counts_toward_adherence ORDER BY session_id DESC LIMIT 1", Long.class, w);
            double c;
            boolean registro;
            if (!ses.isEmpty()) {
                registro = true;
                var ej = jdbc.queryForList("SELECT pwe.prescription_type, pwe.planned_sets, pwe.planned_reps,"
                        + " pwe.planned_duration_seconds, wse.actual_reps_total, wse.actual_duration_seconds"
                        + " FROM planned_workout_exercises pwe LEFT JOIN workout_session_exercises wse ON"
                        + " wse.planned_exercise_id = pwe.planned_exercise_id AND wse.session_id = ?"
                        + " WHERE pwe.planned_workout_id = ?", ses.get(0), w);
                double t = 0;
                for (var e : ej) {
                    boolean dur = "DURATION".equals(e.get("prescription_type"));
                    int series = e.get("planned_sets") == null ? 1 : ((Number) e.get("planned_sets")).intValue();
                    double presc = series * ((Number) e.get(dur ? "planned_duration_seconds" : "planned_reps")).doubleValue();
                    Object hecho = e.get(dur ? "actual_duration_seconds" : "actual_reps_total");
                    t += presc <= 0 ? 0 : Math.min(100.0, 100.0 * (hecho == null ? 0 : ((Number) hecho).doubleValue()) / presc);
                }
                c = ej.isEmpty() ? 0 : t / ej.size();
            } else {
                registro = "SKIPPED".equals(pw.get("status")) && pw.get("skip_reason") != null;
                c = 0;
            }
            suma += c;
            if (registro) {
                conRegistro++;
                sumaObs += c;
            }
            porSesion.add(registro ? String.format(Locale.ROOT, "%.4f", c) : "sin_registro");
        }
        int n = medidas.size();
        return new Object[]{r2(n == 0 ? null : suma / n), r2(conRegistro == 0 ? null : sumaObs / conRegistro),
                r2(n == 0 ? null : 100.0 * conRegistro / n), String.join("|", porSesion)};
    }

    private static BigDecimal r2(Double x) {
        return x == null ? null : BigDecimal.valueOf(x).setScale(2, RoundingMode.HALF_UP);
    }

    /** Fecha (desplazada), enfoque, minutos y ejercicios con su dosis y descanso: lo que define una sesion. */
    private String firma(long w, int desplazamiento) {
        var cab = jdbc.queryForMap("SELECT scheduled_date + ? AS d, focus_code, expected_duration_minutes FROM"
                + " planned_workouts WHERE planned_workout_id = ?", desplazamiento, w);
        var ej = jdbc.queryForList("SELECT exercise_id || ':' || prescription_type || ':' || coalesce(planned_sets::text,'-')"
                + " || 'x' || coalesce(planned_reps::text, planned_duration_seconds::text || 's') || ':r'"
                + " || coalesce(rest_seconds::text,'-') FROM planned_workout_exercises WHERE planned_workout_id = ?"
                + " ORDER BY exercise_order", String.class, w);
        return cab.get("d") + " " + cab.get("focus_code") + " " + cab.get("expected_duration_minutes") + "min ["
                + String.join(", ", ej) + "]";
    }

    private static String indices(List<Long> medidas, String senaladas) {
        if (senaladas == null || senaladas.equals("{}")) return "ninguna";
        var ids = Arrays.stream(senaladas.replaceAll("[{}]", "").split(",")).map(String::trim).map(Long::valueOf).toList();
        return ids.stream().map(x -> medidas.contains(x) ? "S" + (medidas.indexOf(x) + 1) : "?" + x)
                .collect(Collectors.joining("|"));
    }

    /** Violaciones de linaje V31/V32 (consulta oficial del repositorio). */
    private int violaciones() {
        try {
            var texto = Files.readString(Path.of("docs/FitSense/consultas/linaje_v31_v32.sql")).lines()
                    .filter(l -> !l.stripLeading().startsWith("--")).collect(Collectors.joining("\n"));
            return jdbc.queryForList(texto.split(";")[0].strip()).size();
        } catch (IOException e) {
            return -1;
        }
    }

    // ================================================================== espias (sin cambiar el comportamiento)

    private void espiar() {
        doAnswer(inv -> {
            validaciones.incrementAndGet();
            try {
                return inv.callRealMethod();
            } catch (RuntimeException e) {
                rechazosValidacion.add(e.getMessage() == null ? e.getClass().getSimpleName()
                        : e.getMessage().replaceAll("\\s+", " "));
                throw e;
            }
        }).when(validador).validate(any(), any(), anyInt(), any());
        if (!IA) return;
        doAnswer(inv -> proveedor(inv, "GENERACION")).when(generador).requestOutput(any(), any(), anyInt());
        doAnswer(inv -> proveedor(inv, "ADAPTACION")).when(correcciones).pedirRespuesta(any(), any(), any(), any(), anyInt());
    }

    /** Una prediccion real, contada antes de hacerse: al llegar al limite no se llama (se registra). */
    private Object proveedor(org.mockito.invocation.InvocationOnMock inv, String tipo) throws Throwable {
        // Todo lo del laboratorio ANTES de contar y de llamar, y sin poder romper la llamada (intento 1 de la etapa C:
        // String.valueOf(inv.getArgument(0)) se resolvia como valueOf(char[]) y lanzaba ClassCastException).
        var traza = new LinkedHashMap<String, Object>();
        traza.put("etiqueta", etiquetaActual);
        traza.put("tipo", tipo);
        traza.put("intento", String.valueOf((Object) inv.getArgument(inv.getArguments().length - 1)));
        String entrada;
        try {
            entrada = huella(String.valueOf((Object) inv.getArgument(0)) + "|" + String.valueOf((Object) inv.getArgument(1)));
        } catch (RuntimeException e) {
            entrada = "no_calculable:" + e.getClass().getSimpleName();
        }
        traza.put("huella_entrada", entrada);
        int n = llamadas.incrementAndGet();
        if (n > MAX_LLAMADAS || n * COSTE_MAX_POR_LLAMADA_USD > 10.0) {
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

    private static String huella(String s) {
        try {
            var d = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d).substring(0, 16);
        } catch (Exception e) {
            return "?";
        }
    }

    private static String json(Map<String, ?> m) {
        return m.entrySet().stream().map(e -> "\"" + e.getKey() + "\":" + (e.getValue() instanceof Number
                ? e.getValue() : "\"" + String.valueOf(e.getValue()).replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n") + "\"")).collect(Collectors.joining(",", "{", "}"));
    }

    // ================================================================== ficheros

    private List<Perfil> leerPerfiles() throws IOException {
        var lineas = Files.readAllLines(DOCS.resolve("03_PERFILES_U01_U20.csv"));
        var cab = parse(lineas.get(0));
        var out = new ArrayList<Perfil>();
        for (var l : lineas.subList(1, lineas.size())) {
            if (l.isBlank()) continue;
            var v = parse(l);
            var m = new LinkedHashMap<String, String>();
            for (int i = 0; i < cab.size(); i++) m.put(cab.get(i), i < v.size() ? v.get(i) : "");
            out.add(new Perfil(m));
        }
        assertEquals(20, out.size(), "20 perfiles congelados");
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
        return s.contains(",") || s.contains("\"") || s.contains("\n") ? "\"" + s.replace("\"", "\"\"") + "\"" : s;
    }

    private void escribir(String nombre, List<String> lineas) throws IOException {
        Files.write(SALIDA.resolve(nombre), lineas, StandardCharsets.UTF_8);
    }
}
