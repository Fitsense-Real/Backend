package main.web.services.fitsense.planning;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import main.web.services.fitsense.RelojPruebas;
import main.web.services.fitsense.execution.domain.model.commands.FinishWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.model.commands.RecordSessionExerciseCommand;
import main.web.services.fitsense.execution.domain.model.commands.StartWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.services.WorkoutSessionCommandService;
import main.web.services.fitsense.orchestration.application.internal.WeeklyCycleService;
import main.web.services.fitsense.planning.application.internal.generation.PlanGenerationOperationService;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.interfaces.acl.PlanningContextFacade;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.*;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import main.web.services.fitsense.shared.domain.model.valueobjects.SkipReason;
import main.web.services.fitsense.shared.infrastructure.logging.DetalleSensible;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * B1.1 · P0-01 (S-1): los logs no llevan la salida de la IA, las notas de salud, el objetivo en texto libre ni QUE
 * ejercicio dolio. Replicate es un servidor LOCAL (ninguna llamada pagada) que devuelve salidas con marcas unicas; la
 * prueba captura TODA la salida de la aplicacion y comprueba que ninguna marca aparece.
 * <p>
 * Cubre lo que pide la orden: generacion con notas de salud (sincrona y asincrona), reintento tras una propuesta
 * invalida, error del proveedor (HTTP 500 con datos en el cuerpo), publicacion y adaptacion con dolor. Y, para que la
 * prueba valga algo, el modo detallado (solo fuera de prod) SI escribe la salida: la marca aparece.
 */
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=true",
        "fitsense.ai.api-token=token-ficticio-de-prueba",
        "fitsense.ai.poll-interval-millis=100",
        "fitsense.ai.generate.max-per-hour=100",
        "fitsense.ai.generate.feasibility-precheck=false",
        "fitsense.plan-generation.worker.enabled=false",
        "fitsense.adaptation.progression.enabled=false"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("S-1 · logs sin datos sensibles: salida de la IA, salud, objetivo y dolor (Replicate simulado)")
class LogsSinDatosSensiblesIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    /** El reloj lo controla la prueba: sin barrido de entrenamientos vencidos. */
    @MockitoBean main.web.services.fitsense.planning.infrastructure.scheduling.OverdueWorkoutScheduler vencidas;

    static final String NOTA_SALUD = "NOTA-SALUD-SECRETA-7f3a";
    static final String OBJETIVO = "OBJETIVO-PRIVADO-91bc";
    static final String MARCA_SALIDA = "SALIDA-IA-PRIVADA-";
    static final String MARCA_ERROR = "CUERPO-ERROR-PRIVADO-55d1";

    // ------------------------------------------------------------- Replicate local

    static final HttpServer REPLICATE;
    /** Lo que responde cada POST, en orden: "500" o la salida (un texto JSON con comillas escapadas). */
    static final Deque<String> GUION = new ConcurrentLinkedDeque<>();
    static final Map<String, String> SALIDAS = new ConcurrentHashMap<>();
    static final AtomicInteger POSTS = new AtomicInteger();
    static volatile String SALIDA_POR_OMISION = "\"{}\"";

    static {
        try {
            REPLICATE = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        REPLICATE.createContext("/v1/models/", x -> {
            x.getRequestBody().readAllBytes();
            int n = POSTS.incrementAndGet();
            String paso = GUION.pollFirst();
            if ("500".equals(paso)) {
                responder(x, 500, "{\"detail\":\"" + MARCA_ERROR + "\"}");
                return;
            }
            SALIDAS.put("p" + n, paso == null ? SALIDA_POR_OMISION : paso);
            responder(x, 201, prediccion("p" + n, "starting", null));
        });
        REPLICATE.createContext("/v1/predictions/", x -> {
            String id = x.getRequestURI().getPath().substring("/v1/predictions/".length());
            responder(x, 200, prediccion(id, "succeeded", SALIDAS.getOrDefault(id, "\"{}\"")));
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
        r.add("fitsense.ai.base-url", () -> base() + "/v1/models/openai/gpt-5-mini/predictions");
    }

    @AfterAll
    static void parar() {
        REPLICATE.stop(0);
        DetalleSensible.fijar(false);
    }

    // ------------------------------------------------------------- dependencias

    static final ObjectMapper JSON = new ObjectMapper();
    static final AtomicInteger SECUENCIA = new AtomicInteger();
    static final LocalDate S1 = RelojPruebas.SEMANA_REFERENCIA;
    /** Una lista de ids en un log de dolor: "[123" o "{123" (el detalle). En forma tecnica solo hay "n=3". */
    static final Pattern LISTA_DE_IDS = Pattern.compile("[\\[{]\\d");

    @Autowired PlanGenerationOperationService operaciones;
    @Autowired PlanningContextFacade planning;
    @Autowired UserProfileCommandService perfiles;
    @Autowired WorkoutSessionCommandService sesiones;
    @Autowired WeeklyCycleService ciclo;
    @Autowired JdbcTemplate jdbc;

    static String salidaValida;

    @BeforeEach
    void limpiar() {
        GUION.clear();
        POSTS.set(0);
        SALIDA_POR_OMISION = "\"{}\"";
        DetalleSensible.fijar(false);
    }

    long participante() {
        String email = "logs-s1-" + SECUENCIA.incrementAndGet() + "-" + UUID.randomUUID() + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Sintetico', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1990, 3, 10), BiologicalSex.FEMALE,
                new BigDecimal("164"), new BigDecimal("63"), null, FitnessLevel.INTERMEDIATE, GoalType.GENERAL_FITNESS,
                OBJETIVO, TrainingLocation.GYM, (short) 2, List.of((short) 2, (short) 4), (short) 55,
                List.of("body weight", "dumbbell", "barbell", "bench", "cable", "leverage machine", "band",
                        "pull-up bar"), List.of(), NOTA_SALUD, true, true, true, false, false));
        return u;
    }

    /** Generacion inicial asincrona (operacion V58/V59 y libro de predicciones). */
    long generarAsincrona(long u) {
        var id = operaciones.request(u).operation().getId();
        assertThat(operaciones.claimNext()).contains(id);
        operaciones.execute(id);
        return jdbc.queryForObject("SELECT plan_id FROM plan_generation_operations WHERE operation_id = ?",
                Long.class, id);
    }

    /** Las marcas privadas no aparecen en ningun sitio de la salida de la aplicacion. */
    static void sinDatosSensibles(CapturedOutput salida) {
        var todo = salida.getAll();
        assertThat(todo).as("nota de salud").doesNotContain(NOTA_SALUD);
        assertThat(todo).as("objetivo en texto libre").doesNotContain(OBJETIVO);
        assertThat(todo).as("salida de la IA").doesNotContain(MARCA_SALIDA);
        assertThat(todo).as("cuerpo de error del proveedor").doesNotContain(MARCA_ERROR);
        for (var linea : todo.lines().toList()) {
            if (linea.contains("DOLOR_") || linea.contains("EXCLUSIONES_SEGURIDAD") || linea.contains("REGENERACION_SEGURA")
                    || linea.contains("REPARACION_SEGURIDAD"))
                assertThat(LISTA_DE_IDS.matcher(linea).find()).as("ids de ejercicios con dolor en: %s", linea)
                        .isFalse();
        }
    }

    // ------------------------------------------------------------- casos

    @Test
    @Order(1)
    @DisplayName("Generacion sincrona con notas de salud y propuestas invalidas (3 intentos): respaldo, sin datos en el log")
    void sincronaConNotasDeSalud(CapturedOutput salida) throws Exception {
        long u = participante();
        SALIDA_POR_OMISION = JSON.writeValueAsString("{\"marca\":\"" + MARCA_SALIDA + "0\"}");
        long plan = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        assertThat(POSTS.get()).as("se pidio a la IA").isGreaterThanOrEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT generation_source FROM weekly_training_plans WHERE plan_id = ?",
                String.class, plan)).isEqualTo("RULE_ENGINE");
        assertThat(salida.getAll()).as("la linea tecnica sigue existiendo").contains("Salida de la IA (prediccion")
                .contains("huella=");
        sinDatosSensibles(salida);
    }

    @Test
    @Order(2)
    @DisplayName("Asincrona: una propuesta invalida y luego una valida (reintento); se publica la de la IA sin registrarla")
    void reintentoYPublicacion(CapturedOutput salida) throws Exception {
        // El modelo: la rutina que el respaldo publica para el mismo perfil y la misma semana, en formato de la IA.
        SALIDA_POR_OMISION = JSON.writeValueAsString("{\"marca\":\"" + MARCA_SALIDA + "0\"}");
        salidaValida = salidaDesdePlan(generarAsincrona(participante()));
        SALIDA_POR_OMISION = "\"{}\"";
        POSTS.set(0);

        long u = participante();
        GUION.add(JSON.writeValueAsString("{\"marca\":\"" + MARCA_SALIDA + "1\"}"));
        GUION.add(JSON.writeValueAsString(salidaValida.replace("Salida simulada", MARCA_SALIDA + "2")));
        long plan = generarAsincrona(u);
        assertThat(POSTS.get()).isEqualTo(2);
        assertThat(jdbc.queryForMap("SELECT generation_source, generation_attempts FROM weekly_training_plans"
                + " WHERE plan_id = ?", plan)).containsEntry("generation_source", "AI")
                .containsEntry("generation_attempts", 2);
        // La salida SI queda en el libro de predicciones (se borra con la cuenta), que es donde debe estar.
        assertThat(jdbc.queryForObject("SELECT count(*) FROM plan_generation_predictions WHERE user_id = ?"
                + " AND output LIKE ?", Integer.class, u, "%" + MARCA_SALIDA + "2%")).isEqualTo(1);
        sinDatosSensibles(salida);
    }

    @Test
    @Order(3)
    @DisplayName("Error del proveedor (HTTP 500 con datos en el cuerpo): respaldo; el cuerpo del error no se registra")
    void errorDeReplicate(CapturedOutput salida) {
        long u = participante();
        GUION.add("500");
        long plan = generarAsincrona(u);
        assertThat(POSTS.get()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT generation_source FROM weekly_training_plans WHERE plan_id = ?",
                String.class, plan)).isEqualTo("RULE_ENGINE");
        assertThat(salida.getAll()).contains("http 500");
        sinDatosSensibles(salida);
    }

    @Test
    @Order(4)
    @DisplayName("Adaptacion con dolor (ejercicio dejado por dolor y respuestas invalidas de la IA): sin ids de dolor")
    void adaptacionConDolor(CapturedOutput salida) throws Exception {
        long u = participante();
        long plan = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        var ids = jdbc.queryForList("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ?"
                + " ORDER BY scheduled_date", Long.class, plan);
        var ejercicios = jdbc.queryForList("SELECT exercise_id FROM planned_workout_exercises WHERE"
                + " planned_workout_id = ? ORDER BY exercise_order", Long.class, ids.get(0));
        long dolio = ejercicios.get(ejercicios.size() - 1);
        hacer(u, ids.get(0), dolio);
        for (int i = 1; i < ids.size(); i++) hacer(u, ids.get(i), -1);

        SALIDA_POR_OMISION = JSON.writeValueAsString("{\"marca\":\"" + MARCA_SALIDA + "3\"}");
        ciclo.runFor(u, S1, S1.plusWeeks(1));
        // El cierre registra el dolor (PainMemoryService) y adapta la semana siguiente sin ese ejercicio.
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_safety_exclusions WHERE user_id = ?",
                Integer.class, u)).as("premisa: el dolor quedo registrado").isPositive();
        assertThat(salida.getAll()).as("premisa: hubo registros de dolor").contains("DOLOR_REGISTRADO");
        sinDatosSensibles(salida);
    }

    @Test
    @Order(5)
    @DisplayName("Control: con el detalle sensible permitido (solo fuera de prod), la salida de la IA SI se escribe")
    void elModoDetalladoEscribeLaSalida(CapturedOutput salida) throws Exception {
        DetalleSensible.fijar(true);
        try {
            long u = participante();
            GUION.add(JSON.writeValueAsString("{\"marca\":\"" + MARCA_SALIDA + "4\"}"));
            generarAsincrona(u);
            assertThat(salida.getAll()).contains(MARCA_SALIDA + "4");
        } finally {
            DetalleSensible.fijar(false);
        }
    }

    // ------------------------------------------------------------- apoyo

    private void hacer(long u, long w, long conDolor) {
        RelojPruebas.abrirVentanaDeEjecucion(jdbc, u);
        long s = sesiones.handle(new StartWorkoutSessionCommand(u, w)).orElseThrow().getId();
        for (var f : jdbc.queryForList("SELECT pwe.planned_exercise_id, pwe.exercise_id, pwe.prescription_type,"
                + " pwe.planned_sets, pwe.planned_reps, pwe.planned_duration_seconds FROM planned_workout_exercises pwe"
                + " WHERE pwe.planned_workout_id = ? ORDER BY pwe.exercise_order", w)) {
            long pe = ((Number) f.get("planned_exercise_id")).longValue();
            if (((Number) f.get("exercise_id")).longValue() == conDolor) {
                sesiones.handle(new RecordSessionExerciseCommand(u, s, pe, (short) 0, null, null, null,
                        SkipReason.PAIN_OR_DISCOMFORT));
                continue;
            }
            boolean duracion = "DURATION".equals(f.get("prescription_type"));
            int series = f.get("planned_sets") == null ? 1 : ((Number) f.get("planned_sets")).intValue();
            int total = series * ((Number) f.get(duracion ? "planned_duration_seconds" : "planned_reps")).intValue();
            sesiones.handle(new RecordSessionExerciseCommand(u, s, pe, (short) series, duracion ? null : total,
                    duracion ? total : null, null, null));
        }
        sesiones.handle(new FinishWorkoutSessionCommand(u, s, (short) 6, (short) 4, (short) 45));
    }

    /** La rutina publicada, en el formato de salida de la IA (igual que Gpt5MiniConO20IT). */
    String salidaDesdePlan(long plan) throws Exception {
        var sesionesPlan = new ArrayList<Map<String, Object>>();
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
            sesionesPlan.add(s);
        }
        var p = new LinkedHashMap<String, Object>();
        p.put("schema_version", PlanDraft.SCHEMA_VERSION);
        p.put("plan_name", "Plan simulado");
        p.put("total_volume", 0);
        p.put("rationale", "Salida simulada (no es una respuesta real de la IA).");
        p.put("workouts", sesionesPlan);
        return JSON.writeValueAsString(p);
    }
}
