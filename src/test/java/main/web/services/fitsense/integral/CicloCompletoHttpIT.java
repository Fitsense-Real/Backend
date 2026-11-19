package main.web.services.fitsense.integral;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import main.web.services.fitsense.RelojPruebas;
import main.web.services.fitsense.orchestration.application.internal.WeeklyCycleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CICLO COMPLETO POR LA API (validacion funcional integral, 16/11/2026). Un participante sintetico hace, por HTTP y
 * con su JWT, lo mismo que la app: registrarse, iniciar sesion, crear su perfil, generar su primera rutina,
 * consultarla, entrenar (completo, parcial u omitido, con causa o con dolor), ver su adherencia y su historial, y
 * reincorporar un ejercicio retirado por dolor. Lo unico que no pasa por HTTP es el cierre semanal, que en produccion
 * lo lanza el planificador: aqui se ejecuta {@link WeeklyCycleService#runFor} para avanzar semana a semana.
 * <p>
 * PostgreSQL 16 efimero (V1..V57), IA apagada (motor de reglas), sin Replicate. Lo comprobado es invariante de la
 * semilla: estados, conteos, adherencia oficial, decisiones registradas, aislamiento y conservacion del historial,
 * nunca una dosis concreta.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
@DisplayName("Integral · ciclo completo por la API: perfil, rutina, entrenamiento, adherencia, ciclo, historial")
class CicloCompletoHttpIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    static final String CLAVE = "Clave-de-prueba-123";
    static final Path SCRIPT_ROL = Path.of("docs/FitSense/operaciones/cambiar_rol.sql");
    static final AtomicInteger SECUENCIA = new AtomicInteger();
    static final ObjectMapper JSON = new ObjectMapper();

    @Autowired WebApplicationContext contexto;
    @Autowired WeeklyCycleService ciclo;
    @Autowired JdbcTemplate jdbc;
    MockMvc http;

    @BeforeEach
    void http() {
        http = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    record Cuenta(long userId, String email, String token) {}

    // ================================================================ HTTP

    MvcResult como(Cuenta c, MockHttpServletRequestBuilder p) throws Exception {
        return http.perform(p.header("Authorization", "Bearer " + c.token())).andReturn();
    }

    JsonNode json(MvcResult r) throws Exception {
        return JSON.readTree(r.getResponse().getContentAsString());
    }

    JsonNode ok(Cuenta c, MockHttpServletRequestBuilder p) throws Exception {
        var r = como(c, p);
        assertTrue(r.getResponse().getStatus() / 100 == 2,
                p.toString() + " -> " + r.getResponse().getStatus() + " " + r.getResponse().getContentAsString());
        String cuerpo = r.getResponse().getContentAsString();
        return cuerpo.isBlank() ? null : JSON.readTree(cuerpo);
    }

    Cuenta cuenta() throws Exception {
        String email = "integral" + SECUENCIA.incrementAndGet() + "@pruebas.invalid";
        http.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email":"%s","password":"%s","firstName":"Integral","lastName":"Sintetico",
                         "timezone":"America/Lima","acceptsStudyParticipation":true}""".formatted(email, CLAVE)))
                .andExpect(status().isCreated());
        String res = http.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, CLAVE)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Matcher m = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"").matcher(res);
        assertTrue(m.find(), res);
        long id = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        return new Cuenta(id, email, m.group(1));
    }

    Cuenta admin() throws Exception {
        var c = cuenta();
        String sql = Files.readString(SCRIPT_ROL);
        sql = constante(sql, "v_email", "'" + c.email() + "'");
        sql = constante(sql, "v_user_id", String.valueOf(c.userId()));
        sql = constante(sql, "v_rol", "'ADMIN'");
        sql = constante(sql, "v_confirma", "'CAMBIAR %d A ADMIN'".formatted(c.userId()));
        jdbc.execute(sql);
        return c;
    }

    static String constante(String sql, String nombre, String valor) {
        var m = Pattern.compile("(" + nombre + "\\s+\\w+\\s+:= )[^;]+;").matcher(sql);
        assertTrue(m.find(), "el script ya no declara " + nombre);
        return m.replaceFirst(Matcher.quoteReplacement(m.group(1) + valor + ";"));
    }

    void perfil(Cuenta c, String objetivo, String lugar, List<String> equipo, boolean colgarse) throws Exception {
        perfil(c, objetivo, lugar, equipo, colgarse, List.of());
    }

    void perfil(Cuenta c, String objetivo, String lugar, List<String> equipo, boolean colgarse, List<Long> bloqueados)
            throws Exception {
        String equipoJson = JSON.writeValueAsString(equipo);
        ok(c, post("/api/v1/users/me/profile").contentType(MediaType.APPLICATION_JSON).content("""
                {"birthDate":"1992-05-10","biologicalSex":"FEMALE","heightCm":165,"currentWeightKg":68,
                 "fitnessLevel":"INTERMEDIATE","goalType":"%s","goalText":"validacion integral",
                 "trainingLocation":"%s","daysPerWeek":3,"availableDays":[1,3,5],"sessionMinutes":45,
                 "equipmentCodes":%s,"blockedExerciseIds":%s,"healthNotes":"Ninguna",
                 "allowHighImpact":true,"allowFloorExercises":true,"allowKneelingExercises":true,
                 "allowHangingExercises":%s,"allowOverheadExercises":true}""".formatted(objetivo, lugar, equipoJson,
                JSON.writeValueAsString(bloqueados), colgarse)));
    }

    static final List<String> GYM = List.of("dumbbell", "barbell", "cable", "leverage machine", "bench", "band");

    JsonNode generar(Cuenta c) throws Exception {
        var r = como(c, post("/api/v1/users/me/plan/generate"));
        assertEquals(201, r.getResponse().getStatus(), r.getResponse().getContentAsString());
        return json(r);
    }

    JsonNode actual(Cuenta c) throws Exception {
        return ok(c, get("/api/v1/users/me/plan/current"));
    }

    List<JsonNode> sesiones(JsonNode plan) {
        var l = new ArrayList<JsonNode>();
        plan.get("workouts").forEach(l::add);
        return l;
    }

    /** Inicia la sesion, registra los {@code fraccion} primeros ejercicios completos y la cierra. */
    void entrenar(Cuenta c, JsonNode workout, double fraccion) throws Exception {
        long w = workout.get("plannedWorkoutId").asLong();
        var sesion = ok(c, post("/api/v1/workouts/{id}/start", w));
        long sid = sesion.get("sessionId") != null ? sesion.get("sessionId").asLong() : sesion.get("id").asLong();
        var ejercicios = new ArrayList<JsonNode>();
        workout.get("exercises").forEach(ejercicios::add);
        int hechos = (int) Math.ceil(ejercicios.size() * fraccion);
        for (int i = 0; i < hechos; i++) {
            var e = ejercicios.get(i);
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

    void omitir(Cuenta c, JsonNode workout, String causa) throws Exception {
        ok(c, post("/api/v1/workouts/{id}/skip", workout.get("plannedWorkoutId").asLong())
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"%s\"}".formatted(causa)));
    }

    LocalDate semana(JsonNode plan) {
        return LocalDate.parse(plan.get("weekStartDate").asText());
    }

    /** El cierre semanal de la semana medida: publica (o no) la siguiente. */
    long cerrar(Cuenta c, LocalDate medida) {
        var r = ciclo.runFor(c.userId(), medida, medida.plusWeeks(1));
        assertTrue(r.isPresent(), "el ciclo publica la semana " + medida.plusWeeks(1) + ": " + ultimoIntento(c));
        return r.get();
    }

    String ultimoIntento(Cuenta c) {
        var l = jdbc.queryForList("SELECT decision, status, rejection_reason FROM plan_adaptation_attempts"
                + " WHERE user_id = ? ORDER BY attempt_id DESC LIMIT 1", c.userId());
        return l.isEmpty() ? "sin intento" : l.get(0).toString();
    }

    JsonNode metricas(Cuenta c, LocalDate semana) throws Exception {
        return ok(c, get("/api/v1/users/me/metrics/weekly/{w}", semana));
    }

    double adherencia(JsonNode m) {
        return m.get("weightedAdherencePct").asDouble();
    }

    /** Volumen planificado de un plan: series x repeticiones + series x segundos / 3 (comparacion interna). */
    int volumen(long planId) {
        return jdbc.queryForObject("SELECT coalesce(sum(coalesce(planned_sets, 1) * coalesce(planned_reps,"
                + " planned_duration_seconds / 3)), 0) FROM planned_workout_exercises pwe JOIN planned_workouts pw"
                + " ON pw.planned_workout_id = pwe.planned_workout_id WHERE pw.plan_id = ?", Integer.class, planId);
    }

    void verificarPublicable(JsonNode plan) {
        assertEquals("ACTIVE", plan.get("status").asText(), plan.toString());
        assertFalse(sesiones(plan).isEmpty());
        for (var w : sesiones(plan)) {
            assertFalse(w.get("safetyBlocked").asBoolean(), "ninguna sesion publicada esta bloqueada: " + w);
            assertFalse(w.get("exercises").isEmpty());
        }
    }

    // ================================================================ escenarios

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"GAIN_MUSCLE", "LOSE_WEIGHT", "INCREASE_STRENGTH", "GENERAL_FITNESS"})
    @DisplayName("cumple todas sus sesiones tres semanas: 100 %, ciclo, nueva semana sin reducir volumen, historial")
    void cumpleTodo(String objetivo) throws Exception {
        var c = cuenta();
        perfil(c, objetivo, "GYM", GYM, true);
        var p1 = generar(c);
        verificarPublicable(p1);
        assertEquals(3, p1.get("workouts").size());
        assertEquals(p1.get("planId").asLong(), actual(c).get("planId").asLong(), "current = la generada");

        var planes = new ArrayList<Long>(List.of(p1.get("planId").asLong()));
        var plan = p1;
        for (int k = 0; k < 2; k++) {
            RelojPruebas.abrirVentanaDeEjecucion(jdbc, c.userId());
            for (var w : sesiones(plan)) entrenar(c, w, 1.0);
            long nuevo = cerrar(c, semana(plan));
            var m = metricas(c, semana(plan));
            assertEquals(100.0, adherencia(m), 0.001, "adherencia oficial de quien lo hace todo: " + m);
            assertEquals(3, m.get("completedWorkouts").asInt(), m.toString());
            var siguiente = ok(c, get("/api/v1/users/me/plan/{id}", nuevo));
            verificarPublicable(siguiente);
            assertEquals(semana(plan).plusWeeks(1), semana(siguiente));
            assertTrue(volumen(nuevo) >= volumen(planes.get(planes.size() - 1)),
                    objetivo + ": quien cumple todo no recibe menos volumen: " + ultimoIntento(c));
            planes.add(nuevo);
            plan = siguiente;
        }
        System.out.printf("INTEGRAL %s volumen=%s ultimo=%s%n", objetivo,
                planes.stream().map(this::volumen).toList(), ultimoIntento(c));

        // Historial: las tres semanas siguen, cada una con su id; las anteriores no se pierden.
        var historial = ok(c, get("/api/v1/users/me/plans"));
        var ids = new ArrayList<Long>();
        historial.forEach(h -> ids.add(h.get("planId").asLong()));
        assertTrue(ids.containsAll(planes), "historial " + ids + " contiene " + planes);
        for (long id : planes) assertNotNull(ok(c, get("/api/v1/users/me/plan/{id}", id)));
        assertTrue(ok(c, get("/api/v1/users/me/plan/adaptations")).size() >= 2, "las dos decisiones quedan visibles");
        assertTrue(ok(c, get("/api/v1/users/me/metrics/weekly")).size() >= 2);

        // Los entrenamientos quedan vinculados a su plan: tres sesiones COMPLETED por cada semana medida.
        for (int k = 0; k < 2; k++)
            assertEquals(3, jdbc.queryForObject("SELECT count(*) FROM workout_sessions s JOIN planned_workouts pw"
                    + " ON pw.planned_workout_id = s.planned_workout_id WHERE pw.plan_id = ? AND s.status = 'COMPLETED'",
                    Integer.class, planes.get(k)));

        // Aislamiento: otra persona no ve nada de esto.
        var otra = cuenta();
        assertEquals(404, como(otra, get("/api/v1/users/me/plan/{id}", planes.get(0))).getResponse().getStatus());
        long wAjeno = sesiones(p1).get(0).get("plannedWorkoutId").asLong();
        assertEquals(404, como(otra, post("/api/v1/workouts/{id}/skip", wAjeno).contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"FATIGUE\"}")).getResponse().getStatus());

        // Administracion: la adherencia del participante coincide con la suya.
        var adm = admin();
        var suya = ok(adm, get("/api/v1/admin/participants/{id}/adherence", c.userId()));
        assertNotNull(suya);
        assertTrue(suya.toString().contains("100"), "el panel ve la misma adherencia: " + suya);
        assertNotNull(ok(adm, get("/api/v1/admin/analytics/overview")));
        assertEquals(403, como(c, get("/api/v1/admin/analytics/overview")).getResponse().getStatus());
    }

    @Test
    @DisplayName("cumplimiento parcial: la mitad de cada sesion -> adherencia entre 0 y 100, decision registrada, semana valida")
    void cumplimientoParcial() throws Exception {
        var c = cuenta();
        perfil(c, "LOSE_WEIGHT", "GYM", GYM, true);
        var p1 = generar(c);
        RelojPruebas.abrirVentanaDeEjecucion(jdbc, c.userId());
        for (var w : sesiones(p1)) entrenar(c, w, 0.5);
        long nuevo = cerrar(c, semana(p1));
        double a = adherencia(metricas(c, semana(p1)));
        assertTrue(a > 0 && a < 100, "parcial: " + a);
        // Formula oficial (ADHERENCIA.md): A_s = ejercicios hechos / indicados; A_w = media de A_s sobre los previstos.
        double esperado = sesiones(p1).stream().mapToDouble(w -> {
            int n = w.get("exercises").size();
            return Math.ceil(n * 0.5) / n;
        }).average().orElseThrow() * 100;
        assertEquals(esperado, a, 0.01, "A_w oficial");
        assertFalse(ultimoIntento(c).equals("sin intento"));
        verificarPublicable(ok(c, get("/api/v1/users/me/plan/{id}", nuevo)));
    }

    @Test
    @DisplayName("baja adherencia y omisiones con causa: se conservan las causas, la adaptacion decide y publica una semana valida")
    void omiteYBajaAdherencia() throws Exception {
        var c = cuenta();
        perfil(c, "GENERAL_FITNESS", "GYM", GYM, true);
        var p1 = generar(c);
        var s = sesiones(p1);
        RelojPruebas.abrirVentanaDeEjecucion(jdbc, c.userId());
        omitir(c, s.get(0), "LACK_OF_TIME");
        omitir(c, s.get(1), "FATIGUE");
        entrenar(c, s.get(2), 1.0);
        long nuevo = cerrar(c, semana(p1));
        var m = metricas(c, semana(p1));
        assertEquals(100.0 / 3, adherencia(m), 0.01, "A_w oficial: (0 + 0 + 1) / 3: " + m);
        assertEquals(2, m.get("skippedWorkouts").asInt(), m.toString());
        // Las causas quedan en el historico de la semana medida.
        var medida = ok(c, get("/api/v1/users/me/plan/{id}", p1.get("planId").asLong()));
        var causas = new ArrayList<String>();
        medida.get("workouts").forEach(w -> causas.add(w.get("skipReason").asText()));
        assertTrue(causas.containsAll(List.of("LACK_OF_TIME", "FATIGUE")), causas.toString());
        String intento = ultimoIntento(c);
        assertTrue(intento.contains("ADAPT_") || intento.contains("NO_CHANGE"), intento);
        verificarPublicable(ok(c, get("/api/v1/users/me/plan/{id}", nuevo)));
    }

    @Test
    @DisplayName("dolor: la sesion omitida por dolor retira sus ejercicios (P-02), la semana siguiente no los trae y se"
            + " reincorporan solo con confirmacion")
    void dolorYReincorporacion() throws Exception {
        var c = cuenta();
        perfil(c, "GAIN_MUSCLE", "GYM", GYM, true);
        var p1 = generar(c);
        var s = sesiones(p1);
        RelojPruebas.abrirVentanaDeEjecucion(jdbc, c.userId());
        omitir(c, s.get(0), "PAIN_OR_DISCOMFORT");
        entrenar(c, s.get(1), 1.0);
        entrenar(c, s.get(2), 1.0);

        var exclusiones = ok(c, get("/api/v1/users/me/pain-exclusions"));
        assertTrue(exclusiones.size() > 0, "P-02: lo que dolio queda retirado");
        var retirados = new ArrayList<Long>();
        exclusiones.forEach(e -> retirados.add(e.get("exerciseId").asLong()));

        long nuevo = cerrar(c, semana(p1));
        var siguiente = ok(c, get("/api/v1/users/me/plan/{id}", nuevo));
        verificarPublicable(siguiente);
        siguiente.get("workouts").forEach(w -> w.get("exercises").forEach(e ->
                assertFalse(retirados.contains(e.get("exerciseId").asLong()), "lo que dolio no vuelve: " + e)));

        // Reincorporacion: sin confirmar (confirmNoPain=false), 422 y sigue retirado; con confirmacion, se reactiva.
        long ex = retirados.get(0);
        assertEquals(422, como(c, post("/api/v1/users/me/pain-exclusions/{e}/reactivation", ex)
                .contentType(MediaType.APPLICATION_JSON).content("{\"confirmNoPain\":false}")).getResponse().getStatus());
        ok(c, post("/api/v1/users/me/pain-exclusions/{e}/reactivation", ex).contentType(MediaType.APPLICATION_JSON)
                .content("{\"confirmNoPain\":true,\"comment\":\"ya no duele\"}"));
        var despues = ok(c, get("/api/v1/users/me/pain-exclusions"));
        var estado = new ArrayList<String>();
        despues.forEach(e -> { if (e.get("exerciseId").asLong() == ex) estado.add(e.get("status").asText()); });
        assertFalse(estado.contains("ACTIVA"), "reactivado: " + despues);
        // El historial de la semana medida conserva la causa.
        assertEquals("PAIN_OR_DISCOMFORT", ok(c, get("/api/v1/users/me/plan/{id}", p1.get("planId").asLong()))
                .get("workouts").get(0).get("skipReason").asText());
    }

    @Test
    @DisplayName("restriccion que vacia un slot (todas las caderas bloqueadas): cobertura PARCIAL explicada en"
            + " /plan/limitations, plan publicado sin ningun bloqueado, sin relajar nada")
    void coberturaParcialExplicada() throws Exception {
        var c = cuenta();
        // El bloqueo individual es un dato del perfil (blockedExerciseIds). Sin ninguna cadera, el slot LOWER_HIP queda
        // vacio (4C v4 §6 NO FACTIBLE). La comprobacion previa esta en fitsense.coverage.pregeneration=OBSERVE por
        // decision del estudio (application.properties): registra y sigue, sin 422. Se publica el resto del cuerpo y
        // /plan/limitations explica lo que falta. Nunca se cuela un ejercicio bloqueado.
        var cadera = jdbc.queryForList("SELECT exercise_id FROM exercises WHERE movement_pattern IN ('HIP_HINGE',"
                + " 'HIP_EXTENSION')", Long.class);
        assertFalse(cadera.isEmpty());
        perfil(c, "GAIN_MUSCLE", "GYM", GYM, true, cadera);

        var limitaciones = ok(c, get("/api/v1/users/me/plan/limitations"));
        var codigos = new ArrayList<String>();
        limitaciones.get("limitations").forEach(l -> codigos.add(l.get("code").asText()));
        assertTrue(codigos.contains("LOWER_HIP"), "la limitacion se explica antes de generar: " + limitaciones);

        var plan = generar(c);
        verificarPublicable(plan);
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM planned_workout_exercises pwe JOIN planned_workouts pw"
                + " ON pw.planned_workout_id = pwe.planned_workout_id WHERE pw.plan_id = ? AND pwe.exercise_id = ANY(?)",
                Integer.class, plan.get("planId").asLong(), cadera.toArray(new Long[0])), "ningun bloqueado en el plan");
        assertEquals(cadera.size(), jdbc.queryForObject("SELECT cardinality(blocked_exercise_ids) FROM user_profiles"
                + " WHERE user_id = ?", Integer.class, c.userId()), "el perfil conserva el bloqueo");
    }

    @Test
    @DisplayName("resto de la API que usa Flutter: catalogo, abandono, reporte, intervenciones y retiro del estudio")
    void restoDeLaApi() throws Exception {
        var c = cuenta();
        perfil(c, "GENERAL_FITNESS", "GYM", GYM, true);

        // Catalogo (lectura): grupos, equipamiento, busqueda con filtro y ficha.
        assertTrue(ok(c, get("/api/v1/catalog/body-parts")).size() > 0);
        assertTrue(ok(c, get("/api/v1/catalog/equipment-types")).size() > 0);
        var faciles = ok(c, get("/api/v1/catalog/exercises").param("maxDifficulty", "1"));
        assertTrue(faciles.size() > 0);
        faciles.forEach(e -> assertTrue(e.get("difficultyLevel").asInt() <= 1, "el filtro se respeta: " + e));
        long ejercicio = faciles.get(0).get("exerciseId").asLong();
        assertEquals(ejercicio, ok(c, get("/api/v1/catalog/exercises/{id}", ejercicio)).get("exerciseId").asLong());

        var p1 = generar(c);
        var s = sesiones(p1);
        RelojPruebas.abrirVentanaDeEjecucion(jdbc, c.userId());

        // Abandono: no cuenta como intento y el entrenamiento sigue exigible.
        long w0 = s.get(0).get("plannedWorkoutId").asLong();
        long sid = ok(c, post("/api/v1/workouts/{id}/start", w0)).get("sessionId").asLong();
        var abandonada = ok(c, post("/api/v1/sessions/{s}/abandon", sid));
        assertEquals("ABANDONED", abandonada.get("status").asText(), abandonada.toString());
        assertFalse(abandonada.get("countsTowardAdherence").asBoolean());
        entrenar(c, s.get(0), 1.0);

        // Reporte retroactivo («ya lo hice») de la segunda sesion, con todos sus ejercicios.
        var w1 = s.get(1);
        var ejercicios = new ArrayList<String>();
        w1.get("exercises").forEach(e -> {
            int series = e.get("plannedSets").isNull() ? 1 : e.get("plannedSets").asInt();
            ejercicios.add("DURATION".equals(e.get("prescriptionType").asText())
                    ? "{\"plannedExerciseId\":%d,\"actualSets\":%d,\"actualDurationSeconds\":%d}".formatted(
                    e.get("plannedExerciseId").asLong(), series, series * e.get("plannedDurationSeconds").asInt())
                    : "{\"plannedExerciseId\":%d,\"actualSets\":%d,\"actualRepsTotal\":%d}".formatted(
                    e.get("plannedExerciseId").asLong(), series, series * e.get("plannedReps").asInt()));
        });
        var reportada = ok(c, post("/api/v1/workouts/{id}/report", w1.get("plannedWorkoutId").asLong())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"performedAt\":\"%s\",\"sessionRpe\":6,\"satisfaction\":4,\"activeMinutes\":40,\"exercises\":[%s]}"
                        .formatted(java.time.OffsetDateTime.now(), String.join(",", ejercicios))));
        assertTrue(reportada.get("countsTowardAdherence").asBoolean(), reportada.toString());
        omitir(c, s.get(2), "LACK_OF_TIME");

        cerrar(c, semana(p1));
        var m = metricas(c, semana(p1));
        assertEquals(2, m.get("completedWorkouts").asInt(), "entrenada tras abandonar + reportada: " + m);
        assertEquals(1, m.get("skippedWorkouts").asInt(), m.toString());
        assertEquals(200.0 / 3, adherencia(m), 0.01, "A_w oficial: el abandono no cuenta, el reporte si: " + m);

        // Intervenciones: la API devuelve exactamente las registradas para el usuario.
        assertEquals(jdbc.queryForObject("SELECT count(*) FROM user_interventions WHERE user_id = ?", Integer.class,
                c.userId()), ok(c, get("/api/v1/users/me/interventions")).size());

        // Retiro voluntario: queda registrado y no borra nada de lo recolectado.
        int planes = ok(c, get("/api/v1/users/me/plans")).size();
        var retirado = ok(c, post("/api/v1/users/me/withdraw"));
        assertFalse(retirado.get("withdrawnAt").isNull(), retirado.toString());
        assertFalse(ok(c, get("/api/v1/users/me")).get("withdrawnAt").isNull());
        assertEquals(planes, jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ?",
                Integer.class, c.userId()), "el retiro no borra los planes");
        System.out.println("INTEGRAL retiro " + retirado + " metricas " + m);
    }
}
