package main.web.services.fitsense.planning;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import main.web.services.fitsense.planning.application.internal.commandservices.PainMemoryService;
import main.web.services.fitsense.planning.application.internal.generation.PlanGenerationOperationService;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.services.ContratoReparto;
import main.web.services.fitsense.planning.domain.services.DosisSemanalPolicy;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.*;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * H2-v2 (PR-2.0) EN EL PIPELINE REAL con Replicate SIMULADO en el endpoint de gpt-5-mini (sin llamadas pagadas), con la
 * variante encendida ({@code fitsense.ai.generacion.prompt-version=PR-2.0}). La salida simulada parte de la rutina que
 * el motor de reglas publica para un perfil identico (valida por construccion) y se manipula para cada caso:
 * <ul>
 *   <li>valida: se publica como {@code AI} con {@code model_name = gpt-5-mini}, en 1 prediccion;</li>
 *   <li>con una sesion por encima del techo de V26: se rechaza, el reintento lleva el motivo, la pista de V26 y las
 *   reglas 1 a 21; tras 3 intentos el plan sale del motor de reglas y se registra como {@code RULE_ENGINE} con
 *   {@code model_name} nulo (una generacion fallida de la IA nunca consta como IA);</li>
 *   <li>con dolor (O20) en un ejercicio que la salida trae: available_exercises no lo ofrece y lo publicado no lo trae;</li>
 *   <li>5 y 6 dias (los escenarios que dieron problemas en la evaluacion anterior).</li>
 * </ul>
 * Cada peticion lleva la version PR-2.0 (instantanea y texto) y el plan guarda la instantanea de esa version.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=true",
        "fitsense.ai.api-token=token-ficticio-de-prueba",
        "fitsense.ai.poll-interval-millis=200",
        "fitsense.ai.generate.max-per-hour=100",
        "fitsense.plan-generation.worker.enabled=false",
        "fitsense.ai.generacion.prompt-version=PR-2.0"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("H2-v2 (PR-2.0) en el pipeline con Replicate simulado: publica lo valido, rechaza V26 y O20, respaldo bien registrado")
class H2v2PipelineSimuladoIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    static final HttpServer REPLICATE;
    static final AtomicInteger POSTS = new AtomicInteger();
    static final List<String> CUERPOS = new CopyOnWriteArrayList<>();
    static volatile String SALIDA = "\"{}\"";

    static {
        try {
            REPLICATE = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        REPLICATE.createContext("/v1/models/", x -> {
            CUERPOS.add(new String(x.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            responder(x, 201, prediccion("h" + POSTS.incrementAndGet(), "starting", null));
        });
        REPLICATE.createContext("/v1/predictions/", x -> responder(x, 200,
                prediccion(x.getRequestURI().getPath().substring("/v1/predictions/".length()), "succeeded", SALIDA)));
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

    static final ObjectMapper JSON = new ObjectMapper();
    static final AtomicInteger SECUENCIA = new AtomicInteger();
    static final LocalDate SEMANA = LocalDate.of(2026, 11, 23);
    static final List<String> GIMNASIO = List.of("body weight", "dumbbell", "barbell", "cable", "leverage machine",
            "bench", "band", "stationary bike", "treadmill", "kettlebell", "ez barbell", "smith machine", "pull-up bar");

    @Autowired PlanGenerationOperationService service;
    @Autowired WeeklyTrainingPlanCommandService planComandos;
    @Autowired UserProfileCommandService perfiles;
    @Autowired PainMemoryService dolor;
    @Autowired JdbcTemplate jdbc;

    /** Rutina modelo (motor de reglas) del perfil de 5 dias y la de 6 dias, en el formato de salida de la IA. */
    static String salida5, salida6;
    static long plan5, sesion5;

    @BeforeEach
    void limpiar() {
        POSTS.set(0);
        CUERPOS.clear();
    }

    /** GAIN_MUSCLE intermedio en gimnasio, 60 min: 5 dias (G05 sin restricciones) o 6 dias (G25). */
    long participante(int dias) {
        String email = "h2v2-sim-" + SECUENCIA.incrementAndGet() + "-" + UUID.randomUUID() + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'H2v2', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        var d = dias == 6 ? List.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6)
                : List.of((short) 1, (short) 2, (short) 3, (short) 5, (short) 6);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1990, 3, 10), BiologicalSex.FEMALE,
                new BigDecimal("165"), new BigDecimal("72"), null, FitnessLevel.INTERMEDIATE, GoalType.GAIN_MUSCLE,
                "H2-v2 simulado", TrainingLocation.GYM, (short) dias, d, (short) 60, GIMNASIO, List.of(), "Ninguna",
                true, true, true, true, true));
        return u;
    }

    void generar(long u) {
        var id = service.request(u).operation().getId();
        assertThat(service.claimNext()).contains(id);
        service.execute(id);
        assertThat(jdbc.queryForObject("SELECT status FROM plan_generation_operations WHERE operation_id = ?",
                String.class, id)).isEqualTo("COMPLETED");
    }

    Map<String, Object> plan(long u) {
        return jdbc.queryForMap("SELECT plan_id, generation_source, model_name, generation_attempts,"
                + " input_snapshot::text AS snapshot FROM weekly_training_plans WHERE user_id = ?", u);
    }

    /** Cada peticion: endpoint de gpt-5-mini, formato directo y version PR-2.0 en la instantanea y en el texto. */
    void comprobarPeticionesPR20(Set<Long> excluidos) throws Exception {
        assertThat(CUERPOS).isNotEmpty();
        for (var cuerpo : CUERPOS) {
            JsonNode input = JSON.readTree(cuerpo).get("input");
            assertThat(input.has("model") || input.has("json_schema")).isFalse();
            assertThat(input.get("reasoning_effort").asText()).isEqualTo("medium");
            String prompt = input.get("prompt").asText();
            assertThat(prompt).contains("\"prompt_version\":\"PR-2.0\"").contains("21. VOLUMEN POR GRUPO MUSCULAR")
                    .contains("COMPROBACION DEL REPARTO").contains("max_per_body_part_por_n_trabajo")
                    .doesNotContain("mas 15 %");
            if (!excluidos.isEmpty()) {
                // Solo dentro del arreglo available_exercises: el reintento cita tambien la propuesta rechazada.
                int i = prompt.indexOf("\"available_exercises\"");
                int ini = prompt.indexOf('[', i), prof = 0, fin = ini;
                for (; fin < prompt.length(); fin++) {
                    char ch = prompt.charAt(fin);
                    if (ch == '[') prof++;
                    else if (ch == ']' && --prof == 0) break;
                }
                var m = java.util.regex.Pattern.compile("\"exercise_id\"\\s*:\\s*(\\d+)")
                        .matcher(prompt.substring(ini, fin + 1));
                var ofrecidos = new TreeSet<Long>();
                while (m.find()) ofrecidos.add(Long.parseLong(m.group(1)));
                assertThat(ofrecidos).doesNotContainAnyElementsOf(excluidos);
            }
        }
    }

    @Test
    @Order(1)
    @DisplayName("Modelo (5 y 6 dias): sin salida valida, 3 intentos y respaldo RULE_ENGINE con model_name nulo; el plan"
            + " guarda la instantanea PR-2.0")
    void modelo() throws Exception {
        for (int dias : List.of(5, 6)) {
            limpiar();
            long u = participante(dias);
            SALIDA = "\"{}\"";
            generar(u);
            var p = plan(u);
            assertThat(p.get("generation_source")).isEqualTo("RULE_ENGINE");
            assertThat(p.get("model_name")).as("un respaldo no consta como IA").isNull();
            assertThat(POSTS.get()).isEqualTo(3);
            assertThat(JSON.readTree((String) p.get("snapshot")).get("prompt_version").asText()).isEqualTo("PR-2.0");
            comprobarPeticionesPR20(Set.of());
            long planId = ((Number) p.get("plan_id")).longValue();
            if (dias == 5) {
                plan5 = planId;
                sesion5 = jdbc.queryForObject("SELECT min(planned_workout_id) FROM planned_workouts WHERE plan_id = ?",
                        Long.class, planId);
                salida5 = salidaDesdePlan(planId);
            } else salida6 = salidaDesdePlan(planId);
        }
    }

    @Test
    @Order(2)
    @DisplayName("Propuesta valida de 5 y de 6 dias: publicada como AI (gpt-5-mini) en 1 prediccion")
    void validaSePublica() throws Exception {
        for (var caso : List.of(Map.entry(5, salida5), Map.entry(6, salida6))) {
            limpiar();
            long u = participante(caso.getKey());
            SALIDA = JSON.writeValueAsString(caso.getValue());
            generar(u);
            var p = plan(u);
            assertThat(p.get("generation_source")).as(caso.getKey() + " dias").isEqualTo("AI");
            assertThat(p.get("model_name")).isEqualTo("gpt-5-mini");
            assertThat(POSTS.get()).isEqualTo(1);
            comprobarPeticionesPR20(Set.of());
        }
    }

    @Test
    @Order(3)
    @DisplayName("Una sesion por encima del techo de V26: rechazo, reintento con la pista de V26 y respaldo bien registrado")
    void v26RechazoReintentoYRespaldo() throws Exception {
        long u = participante(5);
        var ctx = planComandos.prepare(GenerateWeeklyPlanCommand.firstPlan(u, SEMANA)).context();
        var complementos = ContratoReparto.complementos(ctx);
        var porId = ctx.availableExercises().stream()
                .collect(Collectors.toMap(CandidateExercise::exerciseId, c -> c, (a, b) -> a));
        int techo = DosisSemanalPolicy.techoSeriesSesion(ctx.profile().fitnessLevel());
        // La salida modelo con un ejercicio que cuenta para V26 subido por encima del techo de su sesion.
        var raiz = (ObjectNode) JSON.readTree(salida5);
        ObjectNode elegido = null;
        for (var w : raiz.get("workouts"))
            for (var e : w.get("exercises"))
                if (elegido == null && ContratoReparto.cuentaParaVolumen(porId.get(e.get("exercise_id").asLong()),
                        complementos)) elegido = (ObjectNode) e;
        assertThat(elegido).as("premisa: hay un ejercicio que cuenta").isNotNull();
        elegido.put("planned_sets", techo + 1);
        SALIDA = JSON.writeValueAsString(JSON.writeValueAsString(raiz));
        generar(u);
        var p = plan(u);
        assertThat(POSTS.get()).as("3 intentos").isEqualTo(3);
        assertThat(p.get("generation_source")).isEqualTo("RULE_ENGINE");
        assertThat(p.get("model_name")).as("la IA fallo: no se registra como IA").isNull();
        comprobarPeticionesPR20(Set.of());
        var reintento = JSON.readTree(CUERPOS.get(1)).get("input").get("prompt").asText();
        assertThat(reintento).contains("INTENTO ANTERIOR RECHAZADO").contains("V26: el entrenamiento del")
                .contains("V26: en esa sesion (o en la semana) baja series").contains("las reglas 1 a 21 completas");
        var intentos = jdbc.queryForObject("SELECT count(*) FROM plan_generation_predictions WHERE user_id = ?",
                Integer.class, u);
        assertThat(intentos).as("el libro registra las 3 predicciones").isEqualTo(3);
    }

    @Test
    @Order(4)
    @DisplayName("O20 con PR-2.0: dolor en un ejercicio de la rutina; no se ofrece, la propuesta que lo trae se rechaza")
    void dolorO20() throws Exception {
        long u = participante(5);
        var enRutina = jdbc.queryForList("SELECT pwe.exercise_id FROM planned_workout_exercises pwe JOIN planned_workouts pw"
                + " ON pw.planned_workout_id = pwe.planned_workout_id WHERE pw.plan_id = ? ORDER BY pwe.planned_exercise_id",
                Long.class, plan5);
        long z = enRutina.get(0);
        jdbc.update("INSERT INTO user_safety_exclusions (user_id, exercise_id, reason, source_plan_id,"
                + " source_planned_workout_id, event_date, recorded_by) VALUES (?, ?, 'DOLOR_IDENTIFICADO', ?, ?,"
                + " current_date, 'CICLO_SEMANAL')", u, z, plan5, sesion5);
        var excluidos = dolor.excluidosActivos(u);
        SALIDA = JSON.writeValueAsString(salida5);
        generar(u);
        var p = plan(u);
        var publicados = jdbc.queryForList("SELECT pwe.exercise_id FROM planned_workout_exercises pwe JOIN"
                + " planned_workouts pw ON pw.planned_workout_id = pwe.planned_workout_id WHERE pw.plan_id = ?",
                Long.class, ((Number) p.get("plan_id")).longValue());
        assertThat(publicados).doesNotContainAnyElementsOf(excluidos);
        assertThat(p.get("generation_source")).isEqualTo("RULE_ENGINE");
        comprobarPeticionesPR20(excluidos);
    }

    String salidaDesdePlan(long plan) throws Exception {
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
        p.put("plan_name", "Plan simulado");
        p.put("total_volume", 0);
        p.put("rationale", "Salida simulada (no es una respuesta real de la IA).");
        p.put("workouts", sesiones);
        return JSON.writeValueAsString(p);
    }
}
