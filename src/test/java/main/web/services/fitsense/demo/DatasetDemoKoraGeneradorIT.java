package main.web.services.fitsense.demo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import main.web.services.fitsense.RelojPruebas;
import main.web.services.fitsense.orchestration.application.internal.WeeklyCycleService;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.interfaces.acl.PlanningContextFacade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * DATASET DEMOSTRATIVO DE KORA · GENERADOR (opt-in: {@code -Dfitsense.demo.generar=SI}; CI lo omite).
 * <p>
 * Construye en una PostgreSQL de PRUEBA (Testcontainers, esquema real con Flyway hasta V65) un usuario sintetico con
 * los flujos REALES del backend, sin IA (motor de reglas) y sin Replicate:
 * <ol>
 *   <li>registro e inicio de sesion por la API ({@code /api/v1/auth/register}, {@code /login}) y perfil por la API;</li>
 *   <li>rutina inicial de la semana W-3 con el motor de reglas (origen {@code RULE_ENGINE}, sin modelo);</li>
 *   <li>sesiones por los mismos endpoints que la app (start, PUT de cada ejercicio, finish, skip);</li>
 *   <li>tres cierres semanales reales ({@link WeeklyCycleService#runFor}): W-3 con baja adherencia y dolor en un
 *   ejercicio (decision ADAPT_LOCALIZED; sin IA, respaldo con la sustitucion SUST-SEG-1), W-2 y W-1 completas (la
 *   segunda semana seguida al 100 % activa la progresion del backend); el ultimo publica la semana vigente W0.</li>
 * </ol>
 * Despues coloca las horas de lo ejecutado en el calendario de cada semana (la ejecucion real fue «ahora») y exporta
 * las filas del usuario a los SQL de {@code docs/FitSense/dataset_demo_kora/} ({@link ExportadorDatasetDemo}).
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.plan-generation.worker.enabled=false",
        "fitsense.push.enabled=false"
})
@DisplayName("Dataset demostrativo de Kora · generador (opt-in)")
class DatasetDemoKoraGeneradorIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @MockitoBean main.web.services.fitsense.planning.infrastructure.scheduling.OverdueWorkoutScheduler vencidas;
    @MockitoBean main.web.services.fitsense.orchestration.infrastructure.scheduling.WeeklyCycleScheduler cicloProgramado;

    @Autowired WebApplicationContext contexto;
    @Autowired WeeklyCycleService ciclo;
    @Autowired PlanningContextFacade planning;
    @Autowired JdbcTemplate jdbc;

    static final ObjectMapper JSON = new ObjectMapper();
    static final ZoneId LIMA = ZoneId.of("America/Lima");
    public static final String EMAIL = "usuario.demo.kora@example.com";
    /** Ids reservados: los del dataset empiezan aqui (no colisionan con los de una base en uso). */
    public static final long BASE_IDS = 910_000_000L;
    /** El user_id del generador: unico, para sustituirlo por el de la cuenta real en los textos. */
    public static final long USUARIO_GENERADOR = 987_654_321L;
    static final Path SALIDA = Path.of(System.getProperty("fitsense.demo.salida", "docs/FitSense/dataset_demo_kora"));

    MockMvc http;
    String token;

    record Resp(int status, JsonNode cuerpo, String texto) {}

    Resp llamar(MockHttpServletRequestBuilder p) throws Exception {
        MvcResult r = http.perform(token == null ? p : p.header("Authorization", "Bearer " + token)).andReturn();
        String t = r.getResponse().getContentAsString();
        return new Resp(r.getResponse().getStatus(), t.isBlank() ? null : JSON.readTree(t), t);
    }

    JsonNode ok(MockHttpServletRequestBuilder p) throws Exception {
        var r = llamar(p);
        assertTrue(r.status() / 100 == 2, p + " -> " + r.status() + " " + r.texto());
        return r.cuerpo();
    }

    @Test
    void generar() throws Exception {
        assumeTrue("SI".equals(System.getProperty("fitsense.demo.generar")), "opt-in: -Dfitsense.demo.generar=SI");
        http = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        reservarIds();

        // 1. Cuenta y perfil por la API, como la app. Aqui con participacion en el estudio: el backend solo GENERA planes
        // para participantes activos. (En la base de destino se recomienda registrarla SIN participacion: ver/ejecutar
        // no lo exige y asi ni el ciclo de los lunes ni una generacion pedida desde la app llaman a la IA.)
        String clave = "Demo-" + UUID.randomUUID();
        ok(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"%s","password":"%s","firstName":"Usuario","lastName":"Demo Kora",
                 "timezone":"America/Lima","acceptsStudyParticipation":true}""".formatted(EMAIL, clave)));
        var login = ok(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(EMAIL, clave)));
        token = login.get("token").asText();
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, EMAIL);
        assertEquals(USUARIO_GENERADOR, u, "user_id reservado del generador");
        ok(post("/api/v1/users/me/profile").contentType(MediaType.APPLICATION_JSON).content("""
                {"birthDate":"1995-04-12","biologicalSex":"MALE","heightCm":176,"currentWeightKg":74,
                 "fitnessLevel":"INTERMEDIATE","goalType":"GAIN_MUSCLE",
                 "goalText":"Ganar masa muscular (usuario de demostracion de Kora)",
                 "trainingLocation":"GYM","daysPerWeek":5,"availableDays":[1,2,3,4,5],"sessionMinutes":60,
                 "equipmentCodes":["body weight","dumbbell","barbell","cable","leverage machine","bench","band",
                                   "stationary bike","treadmill","kettlebell","ez barbell","smith machine","pull-up bar"],
                 "blockedExerciseIds":[],"healthNotes":"Ninguna",
                 "allowHighImpact":true,"allowFloorExercises":true,"allowKneelingExercises":true,
                 "allowHangingExercises":true,"allowOverheadExercises":true}"""));

        // 2. Semanas: W0 es la semana en curso (lunes en Lima); el historial son las tres anteriores.
        LocalDate w0 = LocalDate.now(LIMA).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate w3 = w0.minusWeeks(3), w2 = w0.minusWeeks(2), w1 = w0.minusWeeks(1);
        long inicial = planning.generateWeeklyPlan(u, w3, PlanAdjustment.none(), null, null).orElseThrow();
        assertEquals("RULE_ENGINE", origen(inicial));
        var expira = guardarVencimientos(u);

        // 3. W-3 (baja adherencia): dos completas, una parcial por fatiga, una con molestias en un ejercicio concreto
        // (el resto hecho) y una omitida por falta de tiempo.
        var s3 = sesiones(inicial);
        RelojPruebas.abrirVentanaDeEjecucion(jdbc, u);
        entrenar(s3.get(0), 1.0, 6, 4, -1);
        entrenar(s3.get(1), 1.0, 7, 4, -1);
        entrenar(s3.get(2), 0.5, 7, 3, -1);
        entrenar(s3.get(3), 1.0, 7, 3, 0);
        omitir(s3.get(4), "LACK_OF_TIME");
        long adaptada1 = ciclo.runFor(u, w3, w2).orElseThrow(() -> new AssertionError("ciclo W-3: " + ultimoIntento(u)));
        System.out.println("DEMO_CICLO_1 " + ultimoIntento(u));

        // 4. W-2 (plan de la adaptacion 1): semana completa con esfuerzo moderado.
        guardarVencimientos(u).forEach(expira::putIfAbsent);
        RelojPruebas.abrirVentanaDeEjecucion(jdbc, u);
        for (var w : sesiones(adaptada1)) entrenar(w, 1.0, 7, 4, -1);
        long adaptada2 = ciclo.runFor(u, w2, w1).orElseThrow(() -> new AssertionError("ciclo W-2: " + ultimoIntento(u)));
        System.out.println("DEMO_CICLO_2 " + ultimoIntento(u));

        // 5. W-1: semana completa otra vez; con dos semanas al 100 % el cierre aplica la progresion (adaptacion 2).
        guardarVencimientos(u).forEach(expira::putIfAbsent);
        RelojPruebas.abrirVentanaDeEjecucion(jdbc, u);
        for (var w : sesiones(adaptada2)) entrenar(w, 1.0, 6, 4, -1);
        long vigente = ciclo.runFor(u, w1, w0).orElseThrow(() -> new AssertionError("ciclo W-1: " + ultimoIntento(u)));
        System.out.println("DEMO_CICLO_3 " + ultimoIntento(u));
        guardarVencimientos(u).forEach(expira::putIfAbsent);

        // 6. Las horas en el calendario de cada semana y los vencimientos originales.
        new CalendarioDemo(jdbc, u, LIMA).colocar(expira, w3);

        for (var par : List.of(new long[]{inicial, adaptada1}, new long[]{adaptada1, adaptada2}, new long[]{adaptada2, vigente}))
            System.out.println("DEMO_DIFF " + par[0] + "->" + par[1] + " " + diferencias(par[0], par[1]));

        // 7. La API muestra lo que veria la app (antes de exportar, en la base del generador).
        System.out.println("DEMO_PLANES inicial=" + inicial + " adaptada1=" + adaptada1 + " adaptada2=" + adaptada2
                + " vigente=" + vigente);
        jdbc.queryForList("SELECT plan_id, week_start_date, status, generation_source, model_name, parent_plan_id,"
                + " plan_version, week_number FROM weekly_training_plans WHERE user_id = ? ORDER BY plan_id", u)
                .forEach(f -> System.out.println("DEMO_PLAN " + f));
        jdbc.queryForList("SELECT pw.plan_id, pw.scheduled_date, pw.status, pw.skip_reason, pw.expires_at FROM planned_workouts pw"
                + " JOIN weekly_training_plans p ON p.plan_id = pw.plan_id WHERE p.user_id = ? ORDER BY 1, 2", u)
                .forEach(f -> System.out.println("DEMO_PW " + f));
        var actual = ok(get("/api/v1/users/me/plan/current"));
        assertEquals(vigente, actual.get("planId").asLong(), "la vigente es la de W0");

        // 8. Exportar.
        var exportador = new ExportadorDatasetDemo(jdbc, u, USUARIO_GENERADOR, w0, EMAIL);
        exportador.describir(System.out::println);
        exportador.escribir(SALIDA, Map.of(inicial, 2, adaptada1, 4, adaptada2, 4, vigente, 5));
    }

    /** Prescripciones por posicion (dia, orden) de dos planes y lo que cambia. */
    List<String> diferencias(long a, long b) {
        var sql = "SELECT pw.display_order AS d, pwe.exercise_order AS o, pwe.exercise_id AS e, pwe.planned_sets AS s,"
                + " pwe.planned_reps AS r, pwe.planned_duration_seconds AS t, pwe.rest_seconds AS rest"
                + " FROM planned_workouts pw JOIN planned_workout_exercises pwe ON pwe.planned_workout_id = pw.planned_workout_id"
                + " WHERE pw.plan_id = ? ORDER BY 1, 2";
        var ma = new TreeMap<String, String>();
        var mb = new TreeMap<String, String>();
        jdbc.queryForList(sql, a).forEach(f -> ma.put(f.get("d") + "/" + f.get("o"), f.get("e") + " " + f.get("s") + "x"
                + f.get("r") + "/" + f.get("t") + "s r" + f.get("rest")));
        jdbc.queryForList(sql, b).forEach(f -> mb.put(f.get("d") + "/" + f.get("o"), f.get("e") + " " + f.get("s") + "x"
                + f.get("r") + "/" + f.get("t") + "s r" + f.get("rest")));
        var claves = new TreeSet<String>();
        claves.addAll(ma.keySet());
        claves.addAll(mb.keySet());
        var r = new ArrayList<String>();
        for (var k : claves)
            if (!Objects.equals(ma.get(k), mb.get(k))) r.add(k + ": " + ma.get(k) + " -> " + mb.get(k));
        return r;
    }

    String origen(long plan) {
        return jdbc.queryForObject("SELECT generation_source FROM weekly_training_plans WHERE plan_id = ?",
                String.class, plan);
    }

    String ultimoIntento(long u) {
        return jdbc.queryForList("SELECT attempt_id, decision, status, generation_source, rejection_reason,"
                + " conservation_report::text AS informe"
                + " FROM plan_adaptation_attempts WHERE user_id = ? ORDER BY attempt_id DESC LIMIT 1", u).toString();
    }

    Map<Long, java.sql.Timestamp> guardarVencimientos(long u) {
        var m = new HashMap<Long, java.sql.Timestamp>();
        for (var f : jdbc.queryForList("SELECT pw.planned_workout_id, pw.expires_at FROM planned_workouts pw JOIN"
                + " weekly_training_plans p ON p.plan_id = pw.plan_id WHERE p.user_id = ?", u))
            m.putIfAbsent(((Number) f.get("planned_workout_id")).longValue(), (java.sql.Timestamp) f.get("expires_at"));
        return m;
    }

    List<JsonNode> sesiones(long plan) throws Exception {
        var l = new ArrayList<JsonNode>();
        ok(get("/api/v1/users/me/plan/{id}", plan)).get("workouts").forEach(l::add);
        return l;
    }

    /** Lo que hace la app: inicia, registra los primeros {@code fraccion} ejercicios completos (y uno a medias si
     * es parcial; el resto como no hecho por falta de tiempo) y cierra con RPE, satisfaccion y minutos. */
    void entrenar(JsonNode w, double fraccion, int rpe, int satisfaccion, int dolorEn) throws Exception {
        var s = ok(post("/api/v1/workouts/{id}/start", w.get("plannedWorkoutId").asLong()));
        long sid = s.has("sessionId") ? s.get("sessionId").asLong() : s.get("id").asLong();
        var ejercicios = new ArrayList<JsonNode>();
        w.get("exercises").forEach(ejercicios::add);
        int completos = (int) Math.floor(ejercicios.size() * fraccion);
        for (int i = 0; i < ejercicios.size(); i++) {
            var e = ejercicios.get(i);
            boolean duracion = "DURATION".equals(e.get("prescriptionType").asText());
            int series = e.get("plannedSets").isNull() ? 1 : e.get("plannedSets").asInt();
            String cuerpo;
            if (i == dolorEn)
                cuerpo = "{\"actualSets\":0,\"skipReason\":\"PAIN_OR_DISCOMFORT\"}";
            else if (i < completos)
                cuerpo = duracion ? "{\"actualSets\":%d,\"actualDurationSeconds\":%d}".formatted(series,
                        series * e.get("plannedDurationSeconds").asInt())
                        : "{\"actualSets\":%d,\"actualRepsTotal\":%d}".formatted(series, series * e.get("plannedReps").asInt());
            else if (i == completos && fraccion < 1.0)
                cuerpo = duracion ? "{\"actualSets\":%d,\"actualDurationSeconds\":%d,\"skipReason\":\"FATIGUE\"}"
                        .formatted(Math.max(1, series - 1), Math.max(1, series - 1) * e.get("plannedDurationSeconds").asInt())
                        : "{\"actualSets\":%d,\"actualRepsTotal\":%d,\"skipReason\":\"FATIGUE\"}".formatted(
                        Math.max(1, series - 1), Math.max(1, series - 1) * e.get("plannedReps").asInt());
            else
                cuerpo = "{\"actualSets\":0,\"skipReason\":\"LACK_OF_TIME\"}";
            ok(put("/api/v1/sessions/{s}/exercises/{pe}", sid, e.get("plannedExerciseId").asLong())
                    .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
        }
        int minutos = fraccion < 1.0 ? 30 : 55;
        ok(post("/api/v1/sessions/{s}/finish", sid).contentType(MediaType.APPLICATION_JSON)
                .content("{\"sessionRpe\":%d,\"satisfaction\":%d,\"activeMinutes\":%d}".formatted(rpe, satisfaccion, minutos)));
    }

    void omitir(JsonNode w, String causa) throws Exception {
        ok(post("/api/v1/workouts/{id}/skip", w.get("plannedWorkoutId").asLong())
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"%s\"}".formatted(causa)));
    }

    /** Todas las secuencias de identidad de la base de PRUEBA empiezan en el rango reservado (solo aqui). */
    void reservarIds() {
        for (var f : jdbc.queryForList("SELECT table_name, column_name FROM information_schema.columns WHERE"
                + " table_schema = 'public' AND (is_identity = 'YES' OR column_default LIKE 'nextval%')")) {
            String tabla = (String) f.get("table_name"), col = (String) f.get("column_name");
            String seq = jdbc.queryForObject("SELECT pg_get_serial_sequence(?, ?)", String.class, "public." + tabla, col);
            if (seq == null) continue;
            long inicio = "users".equals(tabla) ? USUARIO_GENERADOR : BASE_IDS;
            // Las secuencias pequenas (smallint: catalogo, configuracion) no admiten el rango y no son del dataset.
            Long maximo = jdbc.queryForObject("SELECT max_value FROM pg_sequences WHERE schemaname || '.' || sequencename = ?",
                    Long.class, seq);
            if (maximo == null || maximo < inicio) continue;
            jdbc.execute("ALTER SEQUENCE " + seq + " RESTART WITH " + inicio);
        }
    }
}
