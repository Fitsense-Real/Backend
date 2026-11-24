package main.web.services.fitsense.iam;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import main.web.services.fitsense.RelojPruebas;
import main.web.services.fitsense.iam.application.internal.deletion.EliminacionDeCuentaService;
import main.web.services.fitsense.orchestration.application.internal.WeeklyCycleService;
import main.web.services.fitsense.planning.application.internal.generation.PlanGenerationOperationService;
import org.junit.jupiter.api.BeforeEach;
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

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code DELETE /api/v1/users/me} de punta a punta, con registro, login, JWT y filtro reales sobre PostgreSQL 16
 * efimero (todas las migraciones) y datos sinteticos. IA desactivada: ninguna prueba llama a Replicate; las
 * operaciones y predicciones "en curso" se simulan con filas en el libro.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.account-deletion.retry-initial-delay-ms=3600000"
})
@DisplayName("Cuentas · eliminacion completa (DELETE /api/v1/users/me)")
class EliminacionDeCuentaIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    static final String CLAVE = "Clave-de-prueba-123";
    static final AtomicInteger SECUENCIA = new AtomicInteger();
    static final ObjectMapper JSON = new ObjectMapper();
    static final List<String> GYM = List.of("dumbbell", "barbell", "cable", "leverage machine", "bench", "band");

    @Autowired WebApplicationContext contexto;
    @Autowired JdbcTemplate jdbc;
    @Autowired WeeklyCycleService ciclo;
    @Autowired EliminacionDeCuentaService eliminacion;
    @Autowired PlanGenerationOperationService operaciones;
    MockMvc http;

    record Cuenta(long userId, String email, String token) {}

    @BeforeEach
    void http() {
        http = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    // ================================================================ apoyo

    MvcResult como(Cuenta c, MockHttpServletRequestBuilder p) throws Exception {
        return http.perform(p.header("Authorization", "Bearer " + c.token())).andReturn();
    }

    JsonNode ok(Cuenta c, MockHttpServletRequestBuilder p) throws Exception {
        var r = como(c, p);
        assertEquals(2, r.getResponse().getStatus() / 100, p + " -> " + r.getResponse().getContentAsString());
        String cuerpo = r.getResponse().getContentAsString();
        return cuerpo.isBlank() ? null : JSON.readTree(cuerpo);
    }

    int login(String email, String clave) throws Exception {
        return http.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, clave))).andReturn()
                .getResponse().getStatus();
    }

    Cuenta cuenta() throws Exception {
        String email = "eliminar" + SECUENCIA.incrementAndGet() + "@pruebas.invalid";
        http.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email":"%s","password":"%s","firstName":"Sintetico","lastName":"Eliminable",
                         "timezone":"America/Lima","acceptsStudyParticipation":true}""".formatted(email, CLAVE)))
                .andExpect(status().isCreated());
        String res = http.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, CLAVE)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Matcher m = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"").matcher(res);
        assertTrue(m.find(), res);
        return new Cuenta(jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email),
                email, m.group(1));
    }

    void perfil(Cuenta c) throws Exception {
        ok(c, post("/api/v1/users/me/profile").contentType(MediaType.APPLICATION_JSON).content("""
                {"birthDate":"1990-01-01","biologicalSex":"FEMALE","heightCm":165,"currentWeightKg":68,
                 "fitnessLevel":"INTERMEDIATE","goalType":"GAIN_MUSCLE","goalText":"texto sintetico",
                 "trainingLocation":"GYM","daysPerWeek":3,"availableDays":[1,3,5],"sessionMinutes":45,
                 "equipmentCodes":%s,"blockedExerciseIds":[],"healthNotes":"nota sintetica",
                 "allowHighImpact":true,"allowFloorExercises":true,"allowKneelingExercises":true,
                 "allowHangingExercises":true,"allowOverheadExercises":true}""".formatted(JSON.writeValueAsString(GYM))));
    }

    /** Perfil, plan, una sesion omitida por dolor (P-02), dos entrenadas, cierre semanal y un token de recuperacion. */
    Cuenta conHistorial() throws Exception {
        var c = cuenta();
        perfil(c);
        var r = como(c, post("/api/v1/users/me/plan/generate"));
        assertEquals(201, r.getResponse().getStatus(), r.getResponse().getContentAsString());
        var plan = JSON.readTree(r.getResponse().getContentAsString());
        var sesiones = new ArrayList<JsonNode>();
        plan.get("workouts").forEach(sesiones::add);
        RelojPruebas.abrirVentanaDeEjecucion(jdbc, c.userId());
        ok(c, post("/api/v1/workouts/{id}/skip", sesiones.get(0).get("plannedWorkoutId").asLong())
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"PAIN_OR_DISCOMFORT\"}"));
        // Releido tras la omision: lo que la exclusion por dolor (y, provisionalmente, sus variantes equivalentes) marca
        // en las sesiones abiertas solo puede registrarse como omitido (P-DOLOR-PUB).
        var vigente = ok(c, get("/api/v1/users/me/plan/{id}", plan.get("planId").asLong()));
        sesiones.clear();
        vigente.get("workouts").forEach(sesiones::add);
        for (int i = 1; i < sesiones.size(); i++) entrenar(c, sesiones.get(i));
        var semana = LocalDate.parse(plan.get("weekStartDate").asText());
        assertTrue(ciclo.runFor(c.userId(), semana, semana.plusWeeks(1)).isPresent(), "el ciclo publica la semana 2");
        http.perform(post("/api/v1/auth/password-reset/request").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\"}".formatted(c.email()))).andExpect(status().isAccepted());
        operacionYPrediccion(c.userId(), "COMPLETED", planActivo(c.userId()));
        return c;
    }

    void entrenar(Cuenta c, JsonNode workout) throws Exception {
        var sesion = ok(c, post("/api/v1/workouts/{id}/start", workout.get("plannedWorkoutId").asLong()));
        long sid = sesion.get("sessionId") != null ? sesion.get("sessionId").asLong() : sesion.get("id").asLong();
        for (var e : workout.get("exercises")) {
            if (e.hasNonNull("safetyBlockReason")) {
                assertTrue(e.get("safetyBlockReason").asText().contains("dolor_registrado"), e.toString());
                ok(c, put("/api/v1/sessions/{s}/exercises/{pe}", sid, e.get("plannedExerciseId").asLong())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actualSets\":0,\"skipReason\":\"PAIN_OR_DISCOMFORT\"}"));
                continue;
            }
            boolean duracion = "DURATION".equals(e.get("prescriptionType").asText());
            int series = e.get("plannedSets").isNull() ? 1 : e.get("plannedSets").asInt();
            String cuerpo = duracion
                    ? "{\"actualSets\":%d,\"actualDurationSeconds\":%d}".formatted(series,
                    series * e.get("plannedDurationSeconds").asInt())
                    : "{\"actualSets\":%d,\"actualRepsTotal\":%d}".formatted(series, series * e.get("plannedReps").asInt());
            ok(c, put("/api/v1/sessions/{s}/exercises/{pe}", sid, e.get("plannedExerciseId").asLong())
                    .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
        }
        ok(c, post("/api/v1/sessions/{s}/finish", sid).contentType(MediaType.APPLICATION_JSON)
                .content("{\"sessionRpe\":6,\"satisfaction\":4,\"activeMinutes\":40}"));
    }

    Long planActivo(long userId) {
        return jdbc.queryForObject("SELECT max(plan_id) FROM weekly_training_plans WHERE user_id = ?", Long.class, userId);
    }

    /** Una fila de generacion asincrona y su prediccion en el libro (sin Replicate). */
    UUID operacionYPrediccion(long userId, String estado, Long planId) {
        var op = UUID.randomUUID();
        boolean enCurso = "PROCESSING".equals(estado);
        jdbc.update("""
                INSERT INTO plan_generation_operations (operation_id, user_id, kind, status, plan_id, retryable,
                       executions, created_at, started_at, finished_at, updated_at, lease_owner, lease_expires_at,
                       first_started_at)
                VALUES (?, ?, 'FIRST_PLAN', ?, ?, FALSE, 1, now(), now(), ?, now(), ?, ?, now())
                """, op, userId, estado, enCurso ? null : planId,
                enCurso ? null : java.sql.Timestamp.from(java.time.Instant.now()),
                enCurso ? "otro-worker" : null,
                enCurso ? java.sql.Timestamp.from(java.time.Instant.now().plusSeconds(600)) : null);
        jdbc.update("""
                INSERT INTO plan_generation_predictions (operation_id, user_id, attempt, request_hash, status,
                       replicate_prediction_id, requested_at, sent_at, polls)
                VALUES (?, ?, 1, ?, 'ENVIADA', ?, now(), now(), 0)
                """, op, userId, "0".repeat(64), "sintetica-" + op.toString().substring(0, 8));
        return op;
    }

    String borrar(Cuenta c, String cuerpo) throws Exception {
        return como(c, delete("/api/v1/users/me").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .getResponse().getContentAsString();
    }

    int borrarEstado(Cuenta c, String cuerpo) throws Exception {
        return como(c, delete("/api/v1/users/me").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .getResponse().getStatus();
    }

    static String confirmado(String clave) {
        return "{\"password\":\"%s\",\"confirm\":true}".formatted(clave);
    }

    /** Todas las tablas con columna user_id (salvo el registro de eliminaciones), leidas del esquema real. */
    List<String> tablasConUserId() {
        return jdbc.queryForList("SELECT table_name FROM information_schema.columns WHERE table_schema = 'public'"
                + " AND column_name = 'user_id' AND table_name <> 'account_deletions' ORDER BY 1", String.class);
    }

    Map<String, Integer> filasDe(long userId) {
        var m = new LinkedHashMap<String, Integer>();
        for (var t : tablasConUserId())
            m.put(t, jdbc.queryForObject("SELECT count(*) FROM " + t + " WHERE user_id = ?", Integer.class, userId));
        // Las dependientes sin user_id, por su padre.
        m.put("planned_workouts", jdbc.queryForObject("SELECT count(*) FROM planned_workouts pw JOIN"
                + " weekly_training_plans p ON p.plan_id = pw.plan_id WHERE p.user_id = ?", Integer.class, userId));
        m.put("workout_session_exercises", jdbc.queryForObject("SELECT count(*) FROM workout_session_exercises e"
                + " JOIN workout_sessions s ON s.session_id = e.session_id WHERE s.user_id = ?", Integer.class, userId));
        return m;
    }

    int registros() {
        return jdbc.queryForObject("SELECT count(*) FROM account_deletions", Integer.class);
    }

    int totalFilas(long userId) {
        return filasDe(userId).values().stream().mapToInt(Integer::intValue).sum();
    }

    // ================================================================ escenarios

    @Test
    @DisplayName("cuenta con rutina, sesiones, dolor, metricas, adaptacion, recuperacion y generacion: no queda ninguna"
            + " fila suya, el token y el login dejan de servir y la otra cuenta queda intacta")
    void eliminacionCompleta() throws Exception {
        var c = conHistorial();
        var otra = conHistorial();
        var antes = filasDe(c.userId());
        for (var t : List.of("users", "user_profiles", "weekly_training_plans", "planned_workouts", "workout_sessions",
                "workout_session_exercises", "weekly_user_metrics", "plan_adaptation_attempts", "user_safety_exclusions",
                "password_reset_tokens", "plan_generation_operations", "plan_generation_predictions"))
            assertTrue(antes.get(t) > 0, "premisa: la cuenta tiene filas en " + t + ": " + antes);
        var otraAntes = filasDe(otra.userId());

        var r = como(c, delete("/api/v1/users/me").contentType(MediaType.APPLICATION_JSON).content(confirmado(CLAVE)));
        assertEquals(200, r.getResponse().getStatus(), r.getResponse().getContentAsString());
        assertEquals("no-store", r.getResponse().getHeader("Cache-Control"));
        var cuerpo = JSON.readTree(r.getResponse().getContentAsString());
        assertEquals("DELETED", cuerpo.get("status").asText());
        assertNotNull(cuerpo.get("completedAt"));
        assertFalse(r.getResponse().getContentAsString().contains(c.email()), "la respuesta no repite el correo");

        filasDe(c.userId()).forEach((t, n) -> assertEquals(0, n, "quedan filas en " + t));
        assertEquals(otraAntes, filasDe(otra.userId()), "la otra cuenta no pierde nada");

        var registro = jdbc.queryForMap("SELECT * FROM account_deletions WHERE deletion_id = ?",
                UUID.fromString(cuerpo.get("deletionId").asText()));
        assertEquals("COMPLETADA", registro.get("status"));
        assertNull(registro.get("user_id"), "al completarse, el registro ya no apunta a la cuenta");
        assertEquals("APP", registro.get("origin"));
        String filas = String.valueOf(registro.get("rows_deleted"));
        assertFalse(filas.contains("@") || filas.contains(c.email()), "el registro no guarda datos personales");

        assertEquals(401, como(c, get("/api/v1/users/me")).getResponse().getStatus(), "el JWT deja de servir");
        assertEquals(401, como(c, get("/api/v1/users/me/plan/current")).getResponse().getStatus());
        assertEquals(401, login(c.email(), CLAVE), "no se puede iniciar sesion");
        assertEquals(401, borrarEstado(c, confirmado(CLAVE)), "una segunda solicitud con el mismo token: 401");
        assertEquals(200, como(otra, get("/api/v1/users/me")).getResponse().getStatus(), "la otra sigue entrando");
    }

    @Test
    @DisplayName("sin confirm=true: 400 y no cambia nada; contrasena incorrecta: 403; cinco fallos: 429")
    void confirmacionYReautenticacion() throws Exception {
        var c = cuenta();
        int filas = totalFilas(c.userId());
        for (var cuerpo : List.of("{\"password\":\"%s\"}".formatted(CLAVE),
                "{\"password\":\"%s\",\"confirm\":false}".formatted(CLAVE)))
            assertEquals(400, borrarEstado(c, cuerpo), cuerpo);
        assertTrue(borrar(c, "{\"password\":\"%s\",\"confirm\":false}".formatted(CLAVE))
                .contains("CONFIRMACION_REQUERIDA"));
        assertEquals(400, como(c, delete("/api/v1/users/me")).getResponse().getStatus(), "sin cuerpo");

        String fallo = borrar(c, confirmado("otra-clave-cualquiera"));
        assertTrue(fallo.contains("REAUTENTICACION_FALLIDA"), fallo);
        assertFalse(fallo.contains("otra-clave-cualquiera"), "la respuesta no repite la contrasena");
        for (int i = 0; i < 3; i++) assertEquals(403, borrarEstado(c, confirmado("mal" + i)));
        assertEquals(403, borrarEstado(c, "{\"confirm\":true}"), "sin contrasena (quinto fallo)");
        var bloqueo = como(c, delete("/api/v1/users/me").contentType(MediaType.APPLICATION_JSON)
                .content(confirmado(CLAVE)));
        assertEquals(429, bloqueo.getResponse().getStatus(), "tras cinco fallos, ni la buena pasa");
        assertNotNull(bloqueo.getResponse().getHeader("Retry-After"));

        assertEquals(filas, totalFilas(c.userId()), "nada se borro");
        assertEquals("ACTIVE", jdbc.queryForObject("SELECT status FROM users WHERE user_id = ?", String.class,
                c.userId()));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM account_deletions WHERE user_id = ?", Integer.class,
                c.userId()));
        assertEquals(200, como(c, get("/api/v1/users/me")).getResponse().getStatus(), "la cuenta sigue operativa");
    }

    @Test
    @DisplayName("sin token o con un token invalido: 401; el JWT de otra cuenta no puede borrar esta")
    void soloLaPropia() throws Exception {
        var c = cuenta();
        var otra = cuenta();
        assertEquals(401, http.perform(delete("/api/v1/users/me").contentType(MediaType.APPLICATION_JSON)
                .content(confirmado(CLAVE))).andReturn().getResponse().getStatus());
        assertEquals(401, http.perform(delete("/api/v1/users/me").header("Authorization", "Bearer x.y.z")
                .contentType(MediaType.APPLICATION_JSON).content(confirmado(CLAVE))).andReturn().getResponse()
                .getStatus());
        // La ruta no admite identificador: el token de "otra" solo puede borrar a "otra".
        assertEquals(200, borrarEstado(otra, confirmado(CLAVE)));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM users WHERE user_id = ?", Integer.class, c.userId()));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM users WHERE user_id = ?", Integer.class,
                otra.userId()));
    }

    @Test
    @DisplayName("dos solicitudes a la vez: una sola eliminacion, una sola respuesta que la completa, nada duplicado")
    void concurrentes() throws Exception {
        var c = conHistorial();
        int registrosAntes = registros();
        int n = 4;
        var salida = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(n);
        var tareas = new ArrayList<java.util.concurrent.Future<Integer>>();
        for (int i = 0; i < n; i++)
            tareas.add(pool.submit((Callable<Integer>) () -> {
                salida.await();
                return borrarEstado(c, confirmado(CLAVE));
            }));
        salida.countDown();
        var estados = new ArrayList<Integer>();
        for (var t : tareas) estados.add(t.get());
        pool.shutdown();

        assertTrue(estados.contains(200), "una la completa: " + estados);
        assertTrue(Set.of(200, 202, 401, 404).containsAll(new HashSet<>(estados)), "ninguna falla de otro modo: "
                + estados);
        assertEquals(registrosAntes + 1, registros(), "una sola eliminacion registrada: " + estados);
        assertEquals(0, totalFilas(c.userId()));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM account_deletions WHERE user_id = ?", Integer.class,
                c.userId()), "no queda ninguna eliminacion pendiente ni duplicada");
    }

    @Test
    @DisplayName("borrado interrumpido: 202 PENDING, la cuenta ya no entra, la generacion en curso queda cancelada"
            + " sin publicar, y la reanudacion lo completa")
    void interrumpidaYReanudada() throws Exception {
        var c = conHistorial();
        var enCurso = operacionYPrediccion(c.userId(), "PROCESSING", null);
        int planesAntes = jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ?",
                Integer.class, c.userId());
        // Un fallo a mitad del borrado (como un bloqueo o una caida): el DELETE de users se rechaza.
        jdbc.execute("""
                CREATE OR REPLACE FUNCTION pruebas_impide_borrar() RETURNS trigger AS $$
                BEGIN RAISE EXCEPTION 'borrado interrumpido (prueba)'; END $$ LANGUAGE plpgsql""");
        jdbc.execute("CREATE TRIGGER pruebas_impide_borrar BEFORE DELETE ON users FOR EACH ROW"
                + " WHEN (OLD.user_id = " + c.userId() + ") EXECUTE FUNCTION pruebas_impide_borrar()");
        String cuerpo;
        try {
            var r = como(c, delete("/api/v1/users/me").contentType(MediaType.APPLICATION_JSON)
                    .content(confirmado(CLAVE)));
            cuerpo = r.getResponse().getContentAsString();
            assertEquals(202, r.getResponse().getStatus(), cuerpo);
            assertEquals("PENDING", JSON.readTree(cuerpo).get("status").asText());
            assertNull(JSON.readTree(cuerpo).get("completedAt"), "no se afirma lo que no ha terminado");

            // Todo el borrado se deshizo, pero la cuenta ya esta cerrada.
            assertEquals("DELETED", jdbc.queryForObject("SELECT status FROM users WHERE user_id = ?", String.class,
                    c.userId()));
            assertEquals(planesAntes, jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE"
                    + " user_id = ?", Integer.class, c.userId()));
            assertEquals(401, como(c, get("/api/v1/users/me")).getResponse().getStatus());
            assertEquals(401, login(c.email(), CLAVE));
            var op = jdbc.queryForMap("SELECT status, reason_code, retryable, lease_owner FROM"
                    + " plan_generation_operations WHERE operation_id = ?", enCurso);
            assertEquals("FAILED", op.get("status"));
            assertEquals("CUENTA_ELIMINADA", op.get("reason_code"));
            assertEquals(false, op.get("retryable"));
            assertNull(op.get("lease_owner"));
            // El worker que tuviera la operacion ya no la ejecuta ni publica.
            operaciones.execute(enCurso);
            assertEquals(planesAntes, jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE"
                    + " user_id = ?", Integer.class, c.userId()), "nada publicado despues de eliminar");

            var fila = jdbc.queryForMap("SELECT status, attempts, last_error FROM account_deletions WHERE user_id = ?",
                    c.userId());
            assertEquals("PENDIENTE", fila.get("status"));
            assertEquals(1, fila.get("attempts"));
            assertFalse(String.valueOf(fila.get("last_error")).contains("@"), "el error no guarda datos");
            assertEquals(0, eliminacion.reanudarPendientes(Duration.ZERO), "mientras falle, sigue pendiente");
        } finally {
            jdbc.execute("DROP TRIGGER IF EXISTS pruebas_impide_borrar ON users");
        }
        assertTrue(eliminacion.reanudarPendientes(Duration.ZERO) >= 1);
        assertEquals(0, totalFilas(c.userId()));
        var id = UUID.fromString(JSON.readTree(cuerpo).get("deletionId").asText());
        var fin = eliminacion.consultar(id).orElseThrow();
        assertTrue(fin.completada());
        assertNotNull(fin.completadaEn());
    }

    @Test
    @DisplayName("V60: los codigos P-xxx no se reutilizan ni chocan despues de eliminar cuentas")
    void codigoDeParticipanteNoSeReutiliza() throws Exception {
        var a = cuenta();
        var b = cuenta();
        String codigoB = jdbc.queryForObject("SELECT participant_code FROM users WHERE user_id = ?", String.class,
                b.userId());
        assertEquals(200, borrarEstado(a, confirmado(CLAVE)));
        assertEquals(200, borrarEstado(b, confirmado(CLAVE)));
        var c = cuenta();
        var d = cuenta();
        String codigoC = jdbc.queryForObject("SELECT participant_code FROM users WHERE user_id = ?", String.class,
                c.userId());
        String codigoD = jdbc.queryForObject("SELECT participant_code FROM users WHERE user_id = ?", String.class,
                d.userId());
        assertNotEquals(codigoB, codigoC, "no hereda el codigo de una persona eliminada");
        assertTrue(Integer.parseInt(codigoC.substring(2)) > Integer.parseInt(codigoB.substring(2)));
        assertNotEquals(codigoC, codigoD);
    }

    @Test
    @DisplayName("guarda del esquema: toda tabla con user_id o con FK hacia los datos de la persona esta cubierta")
    void ningunaTablaSinCubrir() {
        var cubiertas = new HashSet<>(EliminacionDeCuentaService.TABLAS_EN_ORDEN);
        // Las que caen por ON DELETE CASCADE desde una cubierta, hasta el cierre.
        var fks = jdbc.queryForList("""
                SELECT tc.relname AS origen, tr.relname AS destino, c.confdeltype AS accion
                  FROM pg_constraint c JOIN pg_class tc ON tc.oid = c.conrelid JOIN pg_class tr ON tr.oid = c.confrelid
                 WHERE c.contype = 'f' AND tc.relnamespace = 'public'::regnamespace""");
        boolean cambio = true;
        while (cambio) {
            cambio = false;
            for (var fk : fks)
                if ("c".equals(String.valueOf(fk.get("accion"))) && cubiertas.contains((String) fk.get("destino"))
                        && cubiertas.add((String) fk.get("origen"))) cambio = true;
        }
        var sinCubrir = new ArrayList<String>();
        for (var t : tablasConUserId())
            if (!cubiertas.contains(t)) sinCubrir.add(t + " (tiene user_id)");
        for (var fk : fks)
            if (cubiertas.contains((String) fk.get("destino")) && !cubiertas.contains((String) fk.get("origen")))
                sinCubrir.add(fk.get("origen") + " -> " + fk.get("destino"));
        assertEquals(List.of(), sinCubrir,
                "anade estas tablas a EliminacionDeCuentaService.TABLAS_EN_ORDEN (y al inventario de datos)");
        assertTrue(EliminacionDeCuentaService.TABLAS_EN_ORDEN.get(EliminacionDeCuentaService.TABLAS_EN_ORDEN.size() - 1)
                .equals("users"), "users se borra la ultima");
    }
}
