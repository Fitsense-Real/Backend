package main.web.services.fitsense.planning;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import main.web.services.fitsense.planning.application.internal.commandservices.PainMemoryService;
import main.web.services.fitsense.planning.application.internal.generation.PlanGenerationOperationService;
import main.web.services.fitsense.planning.application.internal.outboundservices.acl.ExternalCatalogService;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
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
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GPT-5 MINI DIRECTO + O20: las exclusiones por dolor de la #84 (ejercicio que dolio, sus variantes equivalentes y la
 * precaucion por angulo) se aplican tambien a la generacion con {@code openai/gpt-5-mini}. Replicate es un servidor
 * LOCAL que imita el endpoint {@code /v1/models/openai/gpt-5-mini/predictions}: ninguna llamada pagada.
 * <p>
 * La salida simulada es la rutina que el motor de reglas publica para un perfil identico, en el formato de salida de
 * la IA. Asi, con dolor en un ejercicio sin relacion con esa rutina la propuesta es valida y se publica como AI; con
 * dolor en algo que la rutina trae (o su equivalente, o el mismo gesto con otro angulo), la propuesta lo contiene y el
 * backend la rechaza (V1, sin tocar el validador): lo publicado nunca lo trae.
 * <p>
 * En cada peticion se comprueba el formato directo (system_prompt, prompt, reasoning_effort=medium, verbosity,
 * max_completion_tokens; sin model ni json_schema) y que available_exercises del prompt no ofrece nada excluido.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=true",
        "fitsense.ai.api-token=token-ficticio-de-prueba",
        "fitsense.ai.poll-interval-millis=200",
        "fitsense.ai.generate.max-per-hour=100",
        "fitsense.plan-generation.worker.enabled=false"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("GPT-5 Mini directo con O20: la IA nunca recibe ni publica lo excluido por dolor (Replicate simulado)")
class Gpt5MiniConO20IT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    static final HttpServer REPLICATE;
    static final AtomicInteger POSTS = new AtomicInteger();
    static final List<String> CUERPOS = new CopyOnWriteArrayList<>();
    static final List<String> RUTAS = new CopyOnWriteArrayList<>();
    static volatile String SALIDA = "\"{}\"";

    static {
        try {
            REPLICATE = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        REPLICATE.createContext("/v1/models/", x -> {
            CUERPOS.add(new String(x.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            RUTAS.add(x.getRequestURI().getPath());
            int n = POSTS.incrementAndGet();
            responder(x, 201, prediccion("g" + n, "starting", null));
        });
        REPLICATE.createContext("/v1/predictions/", x -> {
            String id = x.getRequestURI().getPath().substring("/v1/predictions/".length());
            responder(x, 200, prediccion(id, "succeeded", SALIDA));
        });
        REPLICATE.start();
    }

    static String base() {
        return "http://127.0.0.1:" + REPLICATE.getAddress().getPort();
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
        // La configuracion de produccion de la #83, con el host local: formato deducido (direct) y valores por omision.
        r.add("fitsense.ai.base-url", () -> base() + "/v1/models/openai/gpt-5-mini/predictions");
    }

    @AfterAll
    static void parar() {
        REPLICATE.stop(0);
    }

    static final AtomicInteger SECUENCIA = new AtomicInteger();
    static final ObjectMapper JSON = new ObjectMapper();
    static final Pattern ID_EN_PROMPT = Pattern.compile("\"exercise_id\"\\s*:\\s*(\\d+)");

    @Autowired PlanGenerationOperationService service;
    @Autowired UserProfileCommandService perfiles;
    @Autowired JdbcTemplate jdbc;
    @Autowired PainMemoryService dolor;
    @Autowired ExternalCatalogService catalogo;

    /** El modelo: la rutina del motor de reglas para el perfil comun, y su plan (para citarlo como origen del dolor). */
    static String salida;
    static long planModelo, sesionModelo;
    static Set<Long> enLaRutina;

    @BeforeEach
    void limpiar() {
        POSTS.set(0);
        CUERPOS.clear();
        RUTAS.clear();
    }

    long participante() {
        String email = "g5m-o20-" + SECUENCIA.incrementAndGet() + "-" + UUID.randomUUID() + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'G5M', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1990, 3, 10), BiologicalSex.FEMALE,
                new BigDecimal("164"), new BigDecimal("63"), null, FitnessLevel.INTERMEDIATE, GoalType.GENERAL_FITNESS,
                "GPT-5 Mini con O20", TrainingLocation.GYM, (short) 2, List.of((short) 2, (short) 4), (short) 55,
                List.of("body weight", "dumbbell", "barbell", "bench", "cable", "leverage machine", "band",
                        "pull-up bar"), List.of(), "Ninguna", true, true, true, false, false));
        return u;
    }

    /** Generacion inicial por el pipeline real (operacion asincrona, sin el trabajador de fondo). */
    void generar(long u) {
        var id = service.request(u).operation().getId();
        assertThat(service.claimNext()).contains(id);
        service.execute(id);
        assertThat(jdbc.queryForObject("SELECT status FROM plan_generation_operations WHERE operation_id = ?",
                String.class, id)).isEqualTo("COMPLETED");
    }

    @Test
    @Order(1)
    @DisplayName("Modelo: sin salida valida de la IA se publica el respaldo por reglas; peticiones en formato directo")
    void modelo() throws Exception {
        long a = participante();
        SALIDA = "\"{}\"";
        generar(a);
        assertThat(jdbc.queryForObject("SELECT generation_source FROM weekly_training_plans WHERE user_id = ?",
                String.class, a)).as("respaldo por reglas").isEqualTo("RULE_ENGINE");
        assertThat(POSTS.get()).as("se intento con la IA").isGreaterThanOrEqualTo(1);
        comprobarFormatoDirecto(Set.of());
        planModelo = jdbc.queryForObject("SELECT plan_id FROM weekly_training_plans WHERE user_id = ?", Long.class, a);
        sesionModelo = jdbc.queryForObject("SELECT min(planned_workout_id) FROM planned_workouts WHERE plan_id = ?",
                Long.class, planModelo);
        enLaRutina = new TreeSet<>(ejerciciosDe(planModelo));
        salida = salidaDesdePlan(planModelo);
    }

    @Test
    @Order(2)
    @DisplayName("Dolor en algo sin relacion con la rutina: la propuesta de GPT-5 Mini es valida y se publica como AI")
    void sinRelacionSePublicaLaIa() throws Exception {
        long z = ejercicioSinRelacion();
        long u = participante();
        registrarDolor(u, z);
        SALIDA = JSON.writeValueAsString(salida);
        generar(u);
        var efectivos = dolor.excluidosActivos(u);
        assertThat(efectivos).contains(z);
        var plan = planDe(u);
        assertThat(jdbc.queryForMap("SELECT generation_source, model_name FROM weekly_training_plans WHERE plan_id = ?",
                plan)).containsEntry("generation_source", "AI").containsEntry("model_name", "gpt-5-mini");
        assertThat(POSTS.get()).isEqualTo(1);
        assertThat(ejerciciosDe(plan)).doesNotContainAnyElementsOf(efectivos);
        comprobarFormatoDirecto(efectivos);
    }

    @Test
    @Order(3)
    @DisplayName("Dolor en el ejercicio que la rutina trae: la propuesta lo contiene, se rechaza y lo publicado no lo trae")
    void directoSeRechaza() throws Exception {
        caso(enLaRutina.iterator().next(), "directo");
    }

    @Test
    @Order(4)
    @DisplayName("Dolor en una variante EQUIVALENTE de un ejercicio de la rutina: tampoco se publica la de la rutina")
    void equivalenteSeRechaza() throws Exception {
        caso(relacionado(java.util.Set.of("VARIANTE_MISMO_EJERCICIO", "MISMO_GESTO")), "equivalente");
    }

    @Test
    @Order(5)
    @DisplayName("Dolor en el mismo gesto con OTRO ANGULO que uno de la rutina: precaucion, tampoco se ofrece ni publica")
    void precaucionPorAnguloSeRechaza() throws Exception {
        // La rutina del perfil comun puede no tener ningun ejercicio con variante de otro angulo: entonces el dolor va
        // en uno de otro angulo de un ejercicio del catalogo, y la salida simulada se construye con ese ejercicio.
        var relaciones = catalogo.fetchEquivalentVariants(enLaRutina);
        boolean enRutina = enLaRutina.stream().anyMatch(w -> relaciones.getOrDefault(w, List.of()).stream()
                .anyMatch(e -> "PRECAUCION_OTRO_ANGULO".equals(e.level())));
        if (enRutina) {
            caso(relacionado(Set.of("PRECAUCION_OTRO_ANGULO")), "precaucion por angulo");
            return;
        }
        // Inclinado frente a plano: dolor en el inclinado; la propuesta trae el plano en lugar de un ejercicio de la
        // rutina del mismo enfoque.
        long inclinado = jdbc.queryForObject("SELECT min(exercise_id) FROM exercises WHERE LOWER(name_en) ="
                + " 'lever incline chest press v. 2' AND is_active", Long.class);
        long plano = jdbc.queryForObject("SELECT min(exercise_id) FROM exercises WHERE LOWER(name_en) ="
                + " 'lever chest press' AND is_active", Long.class);
        long u = participante();
        registrarDolor(u, inclinado);
        var efectivos = dolor.excluidosActivos(u);
        assertThat(efectivos).as("premisa: el plano es precaucion del inclinado").contains(plano);
        var reemplazado = enLaRutina.iterator().next();
        SALIDA = JSON.writeValueAsString(salida.replace("\"exercise_id\":" + reemplazado + ",",
                "\"exercise_id\":" + plano + ","));
        generar(u);
        var plan = planDe(u);
        assertThat(ejerciciosDe(plan)).as("lo publicado no trae el plano").doesNotContainAnyElementsOf(efectivos);
        comprobarFormatoDirecto(efectivos);
        System.out.println("G5M_O20 precaucion por angulo: dolor en " + inclinado + " excluye " + efectivos
                + "; la salida traia " + plano + "; origen " + jdbc.queryForObject("SELECT generation_source FROM"
                + " weekly_training_plans WHERE plan_id = ?", String.class, plan) + "; peticiones " + POSTS.get());
    }

    /** Un caso en que la salida de la IA trae algo excluido: nunca se ofrece en el prompt ni se publica. */
    void caso(long z, String que) throws Exception {
        long u = participante();
        registrarDolor(u, z);
        var efectivos = dolor.excluidosActivos(u);
        var enConflicto = new TreeSet<>(enLaRutina);
        enConflicto.retainAll(efectivos);
        assertThat(enConflicto).as("premisa (%s): la salida simulada trae algo excluido", que).isNotEmpty();
        SALIDA = JSON.writeValueAsString(salida);
        generar(u);
        var plan = planDe(u);
        assertThat(ejerciciosDe(plan)).as("%s: lo publicado nunca trae lo excluido", que)
                .doesNotContainAnyElementsOf(efectivos);
        assertThat(POSTS.get()).isGreaterThanOrEqualTo(1);
        comprobarFormatoDirecto(efectivos);
        System.out.println("G5M_O20 " + que + ": dolor en " + z + " excluye " + efectivos + "; la salida traia "
                + enConflicto + "; origen " + jdbc.queryForObject("SELECT generation_source FROM weekly_training_plans"
                + " WHERE plan_id = ?", String.class, plan) + "; peticiones " + POSTS.get());
    }

    /**
     * Cada peticion: al endpoint de gpt-5-mini y en formato directo, reasoning_effort=medium por omision, sin los
     * parametros de gpt-5-structured, y sin ofrecer nada excluido en available_exercises.
     */
    void comprobarFormatoDirecto(Set<Long> excluidos) throws Exception {
        assertThat(CUERPOS).isNotEmpty();
        assertThat(RUTAS).allMatch(r -> r.equals("/v1/models/openai/gpt-5-mini/predictions"));
        for (var cuerpo : CUERPOS) {
            JsonNode input = JSON.readTree(cuerpo).get("input");
            assertThat(input.has("model")).as("gpt-5-mini no admite model").isFalse();
            assertThat(input.has("json_schema")).as("gpt-5-mini no admite json_schema").isFalse();
            assertThat(input.has("messages")).isFalse();
            assertThat(input.get("system_prompt").asText()).isNotBlank();
            assertThat(input.get("reasoning_effort").asText()).isEqualTo("medium");
            assertThat(input.get("verbosity").asText()).isEqualTo("low");
            assertThat(input.get("max_completion_tokens").asInt()).isEqualTo(16000);
            // Solo dentro de available_exercises: el prompt de correccion cita tambien la propuesta rechazada.
            var prompt = input.get("prompt").asText();
            var ofrecidos = new TreeSet<Long>();
            int i = prompt.indexOf("\"available_exercises\"");
            if (i >= 0) {
                int ini = prompt.indexOf('[', i), prof = 0, fin = ini;
                for (; fin < prompt.length(); fin++) {
                    char ch = prompt.charAt(fin);
                    if (ch == '[') prof++;
                    else if (ch == ']' && --prof == 0) break;
                }
                var m = ID_EN_PROMPT.matcher(prompt.substring(ini, fin + 1));
                while (m.find()) ofrecidos.add(Long.parseLong(m.group(1)));
            }
            if (i >= 0) {
                assertThat(ofrecidos).as("available_exercises").isNotEmpty();
                if (!excluidos.isEmpty()) assertThat(ofrecidos).doesNotContainAnyElementsOf(excluidos);
            }
        }
    }

    /** Un ejercicio del catalogo activo fuera de la rutina y sin ninguna relacion (equivalencia o angulo) con ella. */
    long ejercicioSinRelacion() {
        for (long id : jdbc.queryForList("SELECT exercise_id FROM exercises WHERE is_active ORDER BY exercise_id",
                Long.class)) {
            if (enLaRutina.contains(id)) continue;
            var rel = catalogo.fetchEquivalentVariants(Set.of(id)).getOrDefault(id, List.of());
            if (rel.stream().noneMatch(e -> enLaRutina.contains(e.exerciseId()))) return id;
        }
        throw new IllegalStateException("sin ejercicio libre");
    }

    /** Un ejercicio FUERA de la rutina relacionado, con uno de esos niveles, con un ejercicio de la rutina. */
    long relacionado(Set<String> niveles) {
        var relaciones = catalogo.fetchEquivalentVariants(enLaRutina);
        for (var w : enLaRutina)
            for (var e : relaciones.getOrDefault(w, List.of()))
                if (niveles.contains(e.level()) && !enLaRutina.contains(e.exerciseId())
                        && Boolean.TRUE.equals(jdbc.queryForObject("SELECT is_active FROM exercises WHERE exercise_id = ?",
                        Boolean.class, e.exerciseId())))
                    return e.exerciseId();
        throw new IllegalStateException("La rutina del modelo no tiene ningun ejercicio con relacion " + niveles
                + ": " + enLaRutina);
    }

    /**
     * Una exclusion por dolor CONFIRMADA. Cita como evento de origen la sesion del plan modelo (las columnas son NOT
     * NULL en V43): solo importa a quien pertenece la exclusion, que es {@code u}.
     */
    void registrarDolor(long u, long ejercicio) {
        jdbc.update("INSERT INTO user_safety_exclusions (user_id, exercise_id, reason, source_plan_id,"
                + " source_planned_workout_id, event_date, recorded_by) VALUES (?, ?, 'DOLOR_IDENTIFICADO', ?, ?,"
                + " current_date, 'CICLO_SEMANAL')", u, ejercicio, planModelo, sesionModelo);
    }

    long planDe(long u) {
        return jdbc.queryForObject("SELECT plan_id FROM weekly_training_plans WHERE user_id = ?", Long.class, u);
    }

    List<Long> ejerciciosDe(long plan) {
        return jdbc.queryForList("SELECT pwe.exercise_id FROM planned_workout_exercises pwe JOIN planned_workouts pw"
                + " ON pw.planned_workout_id = pwe.planned_workout_id WHERE pw.plan_id = ?", Long.class, plan);
    }

    /** La rutina publicada, en el formato de salida de la IA (19.2). */
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
