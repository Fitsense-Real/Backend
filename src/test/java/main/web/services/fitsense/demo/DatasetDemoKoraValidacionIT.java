package main.web.services.fitsense.demo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import main.web.services.fitsense.iam.interfaces.acl.IamContextFacade;
import main.web.services.fitsense.planning.infrastructure.scheduling.OverdueWorkoutScheduler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * DATASET DEMOSTRATIVO DE KORA · VALIDACION de los scripts entregados en {@code docs/FitSense/dataset_demo_kora/}.
 * <p>
 * Base PostgreSQL NUEVA (Testcontainers) con el esquema real (Flyway hasta V65) y la aplicacion completa, sin IA ni
 * Replicate, haciendo lo que dice README_EJECUCION.md:
 * <ol>
 *   <li>otra cuenta normal con su plan y una sesion (para comprobar que la demo no la toca);</li>
 *   <li>los scripts fallan sin la cuenta demo y no dejan nada;</li>
 *   <li>registro de la cuenta demo por la API, sin participacion en el estudio;</li>
 *   <li>01–05; una segunda pasada no cambia nada (idempotencia);</li>
 *   <li>las consultas de 06 (integridad y recalculo de la adherencia);</li>
 *   <li>los endpoints que usa la app, como el usuario demo (las respuestas quedan en {@code evidencia/api});</li>
 *   <li>el backend no genera otro plan: ni por la API ni en el cierre de los lunes;</li>
 *   <li>la tarea de vencimiento real sobre la semana en curso;</li>
 *   <li>07 borra solo la demo; repetirlo no hace nada; se puede volver a cargar.</li>
 * </ol>
 * Las salidas se escriben en {@code docs/FitSense/dataset_demo_kora/evidencia/} con
 * {@code -Dfitsense.demo.evidencia=SI}; sin la propiedad (CI), la prueba solo comprueba.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.plan-generation.worker.enabled=false",
        "fitsense.push.enabled=false"
})
@DisplayName("Dataset demostrativo de Kora · validacion de los scripts SQL")
class DatasetDemoKoraValidacionIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired WebApplicationContext contexto;
    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource datos;
    @Autowired IamContextFacade iam;
    @Autowired OverdueWorkoutScheduler vencidas;

    static final ObjectMapper JSON = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    static final ZoneId LIMA = ZoneId.of("America/Lima");
    static final String EMAIL = DatasetDemoKoraGeneradorIT.EMAIL;
    static final Path DIR = Path.of(System.getProperty("fitsense.demo.salida", "docs/FitSense/dataset_demo_kora"));
    static final boolean GUARDAR = "SI".equals(System.getProperty("fitsense.demo.evidencia"));
    static final Path EVIDENCIA = DIR.resolve("evidencia");
    static final List<String> CARGA = List.of("01_USUARIO_DEMO.sql", "02_RUTINA_INICIAL.sql",
            "03_HISTORIAL_ADHERENCIA.sql", "04_ADAPTACIONES.sql", "05_RUTINA_VIGENTE.sql");
    /** Tablas con filas de una cuenta (directas por user_id o a traves de su plan o sesion). */
    static final Map<String, String> FILAS = new LinkedHashMap<>();
    static {
        FILAS.put("user_profiles", "SELECT count(*) FROM user_profiles WHERE user_id = ?");
        FILAS.put("weekly_training_plans", "SELECT count(*) FROM weekly_training_plans WHERE user_id = ?");
        FILAS.put("planned_workouts", "SELECT count(*) FROM planned_workouts pw JOIN weekly_training_plans p"
                + " ON p.plan_id = pw.plan_id WHERE p.user_id = ?");
        FILAS.put("planned_workout_exercises", "SELECT count(*) FROM planned_workout_exercises pwe JOIN planned_workouts pw"
                + " ON pw.planned_workout_id = pwe.planned_workout_id JOIN weekly_training_plans p ON p.plan_id = pw.plan_id"
                + " WHERE p.user_id = ?");
        FILAS.put("workout_sessions", "SELECT count(*) FROM workout_sessions WHERE user_id = ?");
        FILAS.put("workout_session_exercises", "SELECT count(*) FROM workout_session_exercises wse JOIN workout_sessions s"
                + " ON s.session_id = wse.session_id WHERE s.user_id = ?");
        FILAS.put("weekly_user_metrics", "SELECT count(*) FROM weekly_user_metrics WHERE user_id = ?");
        FILAS.put("plan_adaptation_attempts", "SELECT count(*) FROM plan_adaptation_attempts WHERE user_id = ?");
        FILAS.put("safe_regeneration_attempts", "SELECT count(*) FROM safe_regeneration_attempts WHERE user_id = ?");
        FILAS.put("user_interventions", "SELECT count(*) FROM user_interventions WHERE user_id = ?");
        FILAS.put("user_safety_exclusions", "SELECT count(*) FROM user_safety_exclusions WHERE user_id = ?");
        FILAS.put("plan_generation_operations", "SELECT count(*) FROM plan_generation_operations WHERE user_id = ?");
        FILAS.put("notification_events", "SELECT count(*) FROM notification_events WHERE user_id = ?");
        FILAS.put("users", "SELECT count(*) FROM users WHERE user_id = ?");
        FILAS.put("user_consents", "SELECT count(*) FROM user_consents WHERE user_id = ?");
    }

    MockMvc http;
    final StringBuilder registro = new StringBuilder();

    record Resp(int status, JsonNode cuerpo, String texto) {}

    Resp llamar(String token, MockHttpServletRequestBuilder p) throws Exception {
        MvcResult r = http.perform(token == null ? p : p.header("Authorization", "Bearer " + token)).andReturn();
        String t = r.getResponse().getContentAsString();
        JsonNode j = null;
        try { j = t.isBlank() ? null : JSON.readTree(t); } catch (Exception ignorada) { /* no es JSON */ }
        return new Resp(r.getResponse().getStatus(), j, t);
    }

    JsonNode ok(String token, MockHttpServletRequestBuilder p) throws Exception {
        var r = llamar(token, p);
        assertTrue(r.status() / 100 == 2, p + " -> " + r.status() + " " + r.texto());
        return r.cuerpo();
    }

    void anotar(String s) {
        System.out.println("DEMO_VAL " + s);
        registro.append(s).append('\n');
    }

    String registrar(String email, String nombre, boolean participa) throws Exception {
        String clave = "Prueba-" + UUID.randomUUID();
        ok(null, post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"%s","password":"%s","firstName":"%s","lastName":"Demo Kora",
                 "timezone":"America/Lima","acceptsStudyParticipation":%s}""".formatted(email, clave, nombre, participa)));
        return ok(null, post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, clave))).get("token").asText();
    }

    /** Ejecuta un script entero (un bloque DO) y devuelve sus avisos (RAISE NOTICE). */
    List<String> ejecutar(String archivo) throws Exception {
        String sql = Files.readString(DIR.resolve(archivo), StandardCharsets.UTF_8);
        var avisos = new ArrayList<String>();
        try (var c = datos.getConnection(); var st = c.createStatement()) {
            c.setAutoCommit(true);
            st.execute(sql);
            for (SQLWarning w = st.getWarnings(); w != null; w = w.getNextWarning()) avisos.add(w.getMessage());
        }
        avisos.forEach(a -> anotar("  " + archivo + ": " + a));
        return avisos;
    }

    Map<String, Long> filas(long usuario) {
        var m = new LinkedHashMap<String, Long>();
        FILAS.forEach((t, q) -> m.put(t, jdbc.queryForObject(q, Long.class, usuario)));
        return m;
    }

    /** Huella de todas las filas de una cuenta: cambia si se borra o modifica cualquiera. */
    String huella(long usuario) {
        return jdbc.queryForObject("SELECT md5(concat_ws('|',"
                + " (SELECT string_agg(t::text, ',' ORDER BY plan_id) FROM weekly_training_plans t WHERE user_id = ?),"
                + " (SELECT string_agg(pw::text, ',' ORDER BY planned_workout_id) FROM planned_workouts pw JOIN weekly_training_plans p"
                + "   USING (plan_id) WHERE p.user_id = ?),"
                + " (SELECT string_agg(s::text, ',' ORDER BY session_id) FROM workout_sessions s WHERE user_id = ?),"
                + " (SELECT string_agg(x::text, ',') FROM user_profiles x WHERE user_id = ?),"
                + " (SELECT string_agg(u::text, ',') FROM users u WHERE user_id = ?)))",
                String.class, usuario, usuario, usuario, usuario, usuario);
    }

    void guardar(String nombre, String contenido) throws Exception {
        if (!GUARDAR) return;
        Path p = EVIDENCIA.resolve(nombre);
        Files.createDirectories(p.getParent());
        Files.writeString(p, contenido, StandardCharsets.UTF_8);
    }

    @Test
    void scriptsDeLaDemo() throws Exception {
        http = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        LocalDate hoy = LocalDate.now(LIMA);
        LocalDate w0 = hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        anotar("Base nueva con Flyway: " + jdbc.queryForObject("SELECT max(version::int) FROM flyway_schema_history"
                + " WHERE success AND version ~ '^[0-9]+$'", Integer.class) + " · hoy en Lima " + hoy + " · W0 " + w0);
        long catalogo = jdbc.queryForObject("SELECT count(*) FROM exercises", Long.class);

        // 1. Otra cuenta, normal, con su plan y (si queda dia) una sesion.
        String tokenOtra = registrar("otra.cuenta@example.com", "Otra", true);
        ok(tokenOtra, post("/api/v1/users/me/profile").contentType(MediaType.APPLICATION_JSON).content("""
                {"birthDate":"1990-01-01","biologicalSex":"FEMALE","heightCm":165,"currentWeightKg":60,
                 "fitnessLevel":"BEGINNER","goalType":"GENERAL_FITNESS","goalText":"Estar en forma","trainingLocation":"HOME","daysPerWeek":3,
                 "availableDays":[1,3,5],"sessionMinutes":30,"equipmentCodes":["body weight"],"blockedExerciseIds":[],
                 "allowHighImpact":true,"allowFloorExercises":true,"allowKneelingExercises":true,
                 "allowHangingExercises":true,"allowOverheadExercises":true}"""));
        var planOtra = ok(tokenOtra, post("/api/v1/users/me/plan/generate"));
        long otra = jdbc.queryForObject("SELECT user_id FROM users WHERE email = 'otra.cuenta@example.com'", Long.class);
        for (var w : planOtra.get("workouts")) {
            if (LocalDate.parse(w.get("scheduledDate").asText()).isBefore(hoy)) continue;
            var s = ok(tokenOtra, post("/api/v1/workouts/{id}/start", w.get("plannedWorkoutId").asLong()));
            long sid = s.has("sessionId") ? s.get("sessionId").asLong() : s.get("id").asLong();
            ok(tokenOtra, post("/api/v1/sessions/{s}/finish", sid).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"sessionRpe\":5,\"satisfaction\":4,\"activeMinutes\":20}"));
            break;
        }
        var filasOtra = filas(otra);
        String huellaOtra = huella(otra);
        anotar("Otra cuenta " + otra + ": " + filasOtra);

        // 2. Sin la cuenta demo, 01 falla y no deja nada.
        var sinCuenta = assertThrows(SQLException.class, () -> ejecutar("01_USUARIO_DEMO.sql"));
        anotar("01 sin cuenta registrada -> ERROR: " + sinCuenta.getMessage().lines().findFirst().orElse(""));
        assertTrue(sinCuenta.getMessage().contains("no existe la cuenta"));
        var sinPerfil = assertThrows(SQLException.class, () -> ejecutar("02_RUTINA_INICIAL.sql"));
        anotar("02 sin cuenta registrada -> ERROR: " + sinPerfil.getMessage().lines().findFirst().orElse(""));

        // 3. Cuenta demo por la API, SIN participacion.
        String token = registrar(EMAIL, "Usuario", false);
        long demo = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, EMAIL);
        anotar("Cuenta demo registrada por la API: user_id " + demo);
        var antes = filas(demo);

        // 4. 01–05 y una segunda pasada.
        anotar("Primera pasada de 01-05:");
        for (var f : CARGA) {
            var avisos = ejecutar(f);
            assertTrue(avisos.stream().anyMatch(a -> a.contains("aplicado")), f + " " + avisos);
        }
        var cargadas = filas(demo);
        String huellaDemo = huella(demo);
        anotar("Filas de la demo tras la carga: " + cargadas);
        assertEquals(1L, cargadas.get("user_profiles"));
        assertEquals(4L, cargadas.get("weekly_training_plans"));
        assertEquals(20L, cargadas.get("planned_workouts"));
        assertEquals(14L, cargadas.get("workout_sessions"));
        assertEquals(3L, cargadas.get("weekly_user_metrics"));
        assertEquals(3L, cargadas.get("plan_adaptation_attempts"));
        assertEquals(antes.get("users"), cargadas.get("users"));
        assertEquals(antes.get("user_consents"), cargadas.get("user_consents"));
        anotar("Segunda pasada de 01-05 (idempotencia):");
        for (var f : CARGA) {
            var avisos = ejecutar(f);
            assertTrue(avisos.stream().anyMatch(a -> a.contains("No se cambia nada")), f + " " + avisos);
        }
        assertEquals(cargadas, filas(demo), "la segunda pasada no cambia nada");
        assertEquals(huellaDemo, huella(demo));
        assertEquals(filasOtra, filas(otra));
        assertEquals(huellaOtra, huella(otra));

        // 5. Consultas de 06.
        var salida06 = new StringBuilder();
        var resultados = consultas06(salida06);
        guardar("06_resultados.txt", salida06.toString());
        resultados.get(14).forEach(f -> assertEquals(Boolean.TRUE, f.get("ok"), "06 #14: " + f));
        resultados.get(9).forEach(f -> assertEquals(Boolean.TRUE, f.get("coincide"), "06 #9: " + f));
        assertEquals(1, resultados.get(4).size(), "06 #4: un plan vigente hoy");
        assertEquals(2, resultados.get(11).size(), "06 #11: sustitucion y progresion " + resultados.get(11));
        anotar("06: integridad " + resultados.get(14).size() + "/" + resultados.get(14).size()
                + " ok; adherencia recalculada coincide en " + resultados.get(9).size() + " semanas");

        // 6. La API, como la app.
        var api = new LinkedHashMap<String, String>();
        api.put("01_users_me", "/api/v1/users/me");
        api.put("02_profile", "/api/v1/users/me/profile");
        api.put("03_plan_current", "/api/v1/users/me/plan/current");
        api.put("04_plans", "/api/v1/users/me/plans");
        for (long p = 910_000_000L; p <= 910_000_003L; p++) api.put("05_plan_" + p, "/api/v1/users/me/plan/" + p);
        api.put("06_plan_adaptations", "/api/v1/users/me/plan/adaptations");
        api.put("07_plan_limitations", "/api/v1/users/me/plan/limitations");
        api.put("08_metrics_weekly", "/api/v1/users/me/metrics/weekly");
        for (int k = 3; k >= 1; k--) api.put("09_metrics_weekly_W-" + k, "/api/v1/users/me/metrics/weekly/" + w0.minusWeeks(k));
        api.put("10_interventions", "/api/v1/users/me/interventions");
        api.put("11_pain_exclusions", "/api/v1/users/me/pain-exclusions");
        api.put("12_pain_exclusions_effective", "/api/v1/users/me/pain-exclusions/effective");
        for (long s : jdbc.queryForList("SELECT session_id FROM workout_sessions WHERE user_id = ? ORDER BY session_id LIMIT 1",
                Long.class, demo)) api.put("13_session_" + s, "/api/v1/sessions/" + s);
        var respuestas = new LinkedHashMap<String, Resp>();
        for (var e : api.entrySet()) {
            var r = llamar(token, get(e.getValue()));
            respuestas.put(e.getKey(), r);
            anotar("GET " + e.getValue() + " -> " + r.status() + " (" + r.texto().length() + " bytes)");
            guardar("api/" + e.getKey() + ".json", r.cuerpo() == null ? r.texto() : JSON.writeValueAsString(r.cuerpo()));
            // /plan/limitations mide la PROXIMA generacion y exige participar en el estudio: 422 para la demo (limitacion
            // conocida; la app no lo consume, P19 del cierre de politicas).
            int esperado = e.getKey().equals("07_plan_limitations") ? 422 : 200;
            assertEquals(esperado, r.status(), e.getValue() + " " + r.texto());
        }
        var actual = respuestas.get("03_plan_current").cuerpo();
        assertEquals(910_000_003L, actual.get("planId").asLong());
        assertEquals(w0.toString(), actual.get("weekStartDate").asText());
        assertEquals(5, actual.get("workouts").size());
        anotar("plan/current: planId " + actual.get("planId") + ", semana " + actual.get("weekStartDate")
                + ", " + actual.get("workouts").size() + " sesiones");

        // 7. El backend no genera otro plan.
        long planes = jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ?", Long.class, demo);
        var gen = llamar(token, post("/api/v1/users/me/plan/generate"));
        anotar("POST /users/me/plan/generate -> " + gen.status() + " " + gen.texto());
        assertTrue(gen.status() / 100 == 4, "la generacion se rechaza");
        var op = llamar(token, post("/api/v1/users/me/plan/generation"));
        anotar("POST /users/me/plan/generation -> " + op.status() + " " + op.texto());
        // Idempotente: con el plan de la semana ya publicado devuelve una operacion COMPLETED que apunta a el, sin
        // generar nada ni llamar a la IA (sin predicciones).
        assertEquals(200, op.status());
        assertEquals("COMPLETED", op.cuerpo().get("status").asText());
        assertEquals(910_000_003L, op.cuerpo().get("planId").asLong());
        assertNull(op.cuerpo().get("startedAt").textValue());
        var ciclo = llamar(token, post("/api/v1/cycle/run-for-me"));
        anotar("POST /cycle/run-for-me -> " + ciclo.status());
        assertTrue(ciclo.status() / 100 == 4, "el disparo manual del ciclo esta cerrado");
        var participantes = iam.fetchActiveParticipantIdsInTimezone("America/Lima");
        anotar("Participantes del cierre de los lunes en America/Lima: " + participantes + " (demo " + demo
                + (participantes.contains(demo) ? " INCLUIDA" : " no incluida") + ")");
        assertFalse(participantes.contains(demo));
        assertEquals(planes, jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ?", Long.class, demo));
        anotar("plan_generation_operations de la demo: " + jdbc.queryForList("SELECT status, plan_id, executions"
                + " FROM plan_generation_operations WHERE user_id = ?", demo) + "; predicciones: " + jdbc.queryForObject(
                "SELECT count(*) FROM plan_generation_predictions WHERE user_id = ?", Long.class, demo));
        assertEquals(0L, jdbc.queryForObject("SELECT count(*) FROM plan_generation_predictions WHERE user_id = ?", Long.class, demo));

        // 8. La tarea real de vencimiento sobre la semana en curso (lo que hara el backend de destino cada hora).
        var antesVenc = jdbc.queryForList("SELECT scheduled_date, status FROM planned_workouts WHERE plan_id = 910000003"
                + " ORDER BY scheduled_date");
        vencidas.expireOverdue();
        var despuesVenc = jdbc.queryForList("SELECT scheduled_date, status, skip_reason FROM planned_workouts"
                + " WHERE plan_id = 910000003 ORDER BY scheduled_date");
        anotar("Semana en curso antes del vencimiento: " + antesVenc);
        anotar("Semana en curso despues del vencimiento: " + despuesVenc);
        for (var f : despuesVenc) {
            LocalDate d = ((java.sql.Date) f.get("scheduled_date")).toLocalDate();
            assertEquals(d.isBefore(hoy) ? "SKIPPED" : "SCHEDULED", f.get("status"), "vencimiento de " + d);
        }
        assertEquals(1L, jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ? AND status = 'ACTIVE'",
                Long.class, demo));

        // 9. 07: solo la demo.
        anotar("07 (limpieza):");
        ejecutar("07_ELIMINAR_DEMO.sql");
        var limpias = filas(demo);
        anotar("Filas de la demo tras 07: " + limpias);
        limpias.forEach((t, n) -> {
            if (!t.equals("users") && !t.equals("user_consents")) assertEquals(0L, n, t);
        });
        assertEquals(1L, limpias.get("users"), "la cuenta se conserva (v_eliminar_cuenta = false)");
        assertEquals(filasOtra, filas(otra), "la otra cuenta no cambia");
        assertEquals(huellaOtra, huella(otra), "la otra cuenta no cambia");
        assertEquals(catalogo, jdbc.queryForObject("SELECT count(*) FROM exercises", Long.class));
        assertEquals(0L, jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE plan_id BETWEEN 910000000"
                + " AND 910999999", Long.class));
        anotar("Otra cuenta tras 07: " + filas(otra) + " (identica)");
        anotar("07 otra vez:");
        ejecutar("07_ELIMINAR_DEMO.sql");
        assertEquals(limpias, filas(demo));

        // 10. Se puede volver a cargar y limpiar.
        anotar("Recarga de 01-05 despues de 07:");
        for (var f : CARGA) ejecutar(f);
        assertEquals(4L, filas(demo).get("weekly_training_plans"));
        assertEquals(200, llamar(token, get("/api/v1/users/me/plan/current")).status());
        ejecutar("07_ELIMINAR_DEMO.sql");
        assertEquals(huellaOtra, huella(otra));
        guardar("validacion.txt", registro.toString());
    }

    /** Ejecuta las consultas de 06 (separadas por «-- ###») y devuelve sus filas por numero. */
    Map<Integer, List<Map<String, Object>>> consultas06(StringBuilder salida) throws Exception {
        String sql = Files.readString(DIR.resolve("06_VERIFICACION.sql"), StandardCharsets.UTF_8);
        var res = new TreeMap<Integer, List<Map<String, Object>>>();
        for (String trozo : sql.split("(?m)^-- ### ")) {
            if (!Character.isDigit(trozo.charAt(0))) continue;
            int n = Integer.parseInt(trozo.substring(0, trozo.indexOf('.')));
            var filas = jdbc.queryForList(trozo.substring(trozo.indexOf('\n') + 1));
            res.put(n, filas);
            salida.append("### ").append(trozo, 0, trozo.indexOf('\n')).append("\n");
            salida.append("(").append(filas.size()).append(" filas)\n");
            filas.forEach(f -> salida.append("  ").append(f).append('\n'));
            salida.append('\n');
        }
        return res;
    }
}
