package main.web.services.fitsense.integral;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O04 (evaluacion gpt-5-mini, 23/11/2026): un ejercicio DURATION SIN SERIES ({@code planned_sets = NULL}) en un plan
 * publicado, de punta a punta por la API que usa la app.
 * <p>
 * Esa forma la produce la IA en adaptaciones (O04 con gpt-5-mini y GRA-R2 con gpt-5-structured) y la admiten el
 * contrato de datos (V4, {@code ck_pwe_min_sets}: series nulas o >= 2), V9 ("sin series, un bloque continuo, sigue
 * siendo valido") y V8C. El motor de reglas nunca la genera, asi que aqui se reproduce poniendo a NULL las series de un
 * DURATION de un plan real: es exactamente la fila que publico la adaptacion de O04.
 * <p>
 * Fija la interpretacion UNICA del backend: <b>sin series = una sola tanda de planned_duration_seconds</b>.
 * <ul>
 *   <li>el plan la expone tal cual: {@code plannedSets = null} con {@code plannedDurationSeconds};</li>
 *   <li>registrar la duracion hecha da el cumplimiento contra 1 x duracion (ni 0 ni dividido por null);</li>
 *   <li>el volumen indicado del plan cuenta 1 x duracion.</li>
 * </ul>
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
@DisplayName("O04 · DURATION sin series por la API: se muestra, se registra y cuenta como una tanda")
class DuracionSinSeriesHttpIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    static final ObjectMapper JSON = new ObjectMapper();

    @Autowired WebApplicationContext contexto;
    @Autowired JdbcTemplate jdbc;
    @Autowired main.web.services.fitsense.planning.interfaces.acl.PlanningContextFacade planificacion;
    MockMvc http;
    long usuario;
    java.time.LocalDate semana;
    String token;

    @BeforeEach
    void http() {
        http = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    MvcResult como(MockHttpServletRequestBuilder p) throws Exception {
        return http.perform(p.header("Authorization", "Bearer " + token)).andReturn();
    }

    JsonNode ok(MockHttpServletRequestBuilder p) throws Exception {
        var r = como(p);
        assertEquals(2, r.getResponse().getStatus() / 100, p + " -> " + r.getResponse().getContentAsString());
        var cuerpo = r.getResponse().getContentAsString();
        return cuerpo.isBlank() ? null : JSON.readTree(cuerpo);
    }

    @Test
    void duracionSinSeries() throws Exception {
        String email = "o04-duracion@pruebas.invalid", clave = "Clave-de-prueba-123";
        http.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email":"%s","password":"%s","firstName":"O04","lastName":"Sintetico",
                         "timezone":"America/Lima","acceptsStudyParticipation":true}""".formatted(email, clave)))
                .andExpect(status().isCreated());
        var login = http.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, clave))).andReturn()
                .getResponse().getContentAsString();
        var m = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"").matcher(login);
        assertTrue(m.find(), login);
        token = m.group(1);
        // LOSE_WEIGHT: CARDIO en cada sesion, asi que hay ejercicios DURATION en el plan.
        ok(post("/api/v1/users/me/profile").contentType(MediaType.APPLICATION_JSON).content("""
                {"birthDate":"1992-05-10","biologicalSex":"FEMALE","heightCm":165,"currentWeightKg":68,
                 "fitnessLevel":"BEGINNER","goalType":"LOSE_WEIGHT","goalText":"O04",
                 "trainingLocation":"GYM","daysPerWeek":3,"availableDays":[1,3,5],"sessionMinutes":45,
                 "equipmentCodes":["dumbbell","barbell","cable","leverage machine","bench","band","stationary bike","treadmill"],
                 "blockedExerciseIds":[],"healthNotes":"Ninguna",
                 "allowHighImpact":true,"allowFloorExercises":true,"allowKneelingExercises":true,
                 "allowHangingExercises":true,"allowOverheadExercises":true}"""));
        var generado = como(post("/api/v1/users/me/plan/generate"));
        assertEquals(201, generado.getResponse().getStatus(), generado.getResponse().getContentAsString());

        // La fila de O04: un DURATION con series, que pasa a NULL (lo que publico la adaptacion de la IA).
        var fila = jdbc.queryForMap("SELECT pwe.planned_exercise_id, pwe.planned_workout_id, pwe.planned_sets,"
                + " pwe.planned_duration_seconds FROM planned_workout_exercises pwe JOIN planned_workouts pw"
                + " ON pw.planned_workout_id = pwe.planned_workout_id WHERE pwe.prescription_type = 'DURATION'"
                + " AND pwe.planned_sets IS NOT NULL AND (SELECT count(*) FROM planned_workout_exercises o"
                + " WHERE o.planned_workout_id = pwe.planned_workout_id AND o.prescription_type = 'DURATION'"
                + " AND o.planned_sets IS NOT NULL) >= 2 ORDER BY pw.scheduled_date, pwe.exercise_order LIMIT 1");
        long pe = ((Number) fila.get("planned_exercise_id")).longValue();
        long workout = ((Number) fila.get("planned_workout_id")).longValue();
        int segundos = ((Number) fila.get("planned_duration_seconds")).intValue();
        usuario = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        semana = jdbc.queryForObject("SELECT week_start_date FROM weekly_training_plans WHERE user_id = ?",
                java.time.LocalDate.class, usuario);
        long antesTotal = segundosIndicados();
        int seriesAntes = ((Number) fila.get("planned_sets")).intValue();
        assertEquals(1, jdbc.update("UPDATE planned_workout_exercises SET planned_sets = NULL WHERE planned_exercise_id = ?",
                pe), "el contrato de datos (ck_pwe_min_sets) admite DURATION sin series");

        // 1. Se expone tal cual: plannedSets null, la duracion presente. La app tiene lo que necesita para pintarlo.
        var plan = ok(get("/api/v1/users/me/plan/current"));
        JsonNode ejercicio = null;
        for (var w : plan.get("workouts"))
            for (var e : w.get("exercises"))
                if (e.get("plannedExerciseId").asLong() == pe) ejercicio = e;
        assertNotNull(ejercicio);
        assertEquals("DURATION", ejercicio.get("prescriptionType").asText());
        assertTrue(ejercicio.get("plannedSets").isNull(), ejercicio.toString());
        assertEquals(segundos, ejercicio.get("plannedDurationSeconds").asInt());
        assertTrue(ejercicio.get("plannedReps").isNull());

        // 2. El volumen indicado cuenta una tanda: baja exactamente (series - 1) x duracion.
        assertEquals(antesTotal - (long) (seriesAntes - 1) * segundos, segundosIndicados());

        // 3. Se registra sin perder informacion. La app envia el TOTAL de segundos (y, si quiere, las series).
        var sesion = ok(post("/api/v1/workouts/{id}/start", workout));
        long sid = sesion.get("sessionId").asLong();
        var hecho = registrar(sid, pe, "{\"actualDurationSeconds\":%d}".formatted(segundos));
        assertEquals(0, hecho.get("completionPercentage").decimalValue().compareTo(new java.math.BigDecimal("100")),
                hecho.toString());
        assertEquals("COMPLETED", hecho.get("status").asText());

        var mitad = registrar(sid, pe, "{\"actualDurationSeconds\":%d}".formatted(segundos / 2));
        assertEquals(0, mitad.get("completionPercentage").decimalValue()
                .compareTo(java.math.BigDecimal.valueOf(segundos / 2 * 100.0 / segundos).setScale(2,
                        java.math.RoundingMode.HALF_UP)), mitad.toString());
        assertEquals("PARTIAL", mitad.get("status").asText());

        var conSeries = registrar(sid, pe, "{\"actualSets\":1,\"actualDurationSeconds\":%d}".formatted(segundos));
        assertEquals("COMPLETED", conSeries.get("status").asText(), conSeries.toString());
        assertEquals(1, conSeries.get("actualSets").asInt(), "las series registradas se conservan");
        assertEquals(segundos, conSeries.get("actualDurationSeconds").asInt());

        // 4. CONTRATO (dependencia de Mobile, ver docs/FitSense/cierre_gpt5_mini_h2/02_O04_O20.md): actualDurationSeconds
        //    es el TOTAL de todas las series. Un DURATION CON series (N x d) registrado con la duracion de UNA serie (lo
        //    que envia hoy la app con «Lo completé») cuenta 1/N: no es un fallo del backend, es el denominador de 17.1.
        var otro = jdbc.queryForMap("SELECT pwe.planned_exercise_id, pwe.planned_sets, pwe.planned_duration_seconds"
                + " FROM planned_workout_exercises pwe WHERE pwe.planned_workout_id = ? AND pwe.prescription_type ="
                + " 'DURATION' AND pwe.planned_sets IS NOT NULL ORDER BY pwe.exercise_order LIMIT 1", workout);
        long pe2 = ((Number) otro.get("planned_exercise_id")).longValue();
        int n = ((Number) otro.get("planned_sets")).intValue(), d = ((Number) otro.get("planned_duration_seconds")).intValue();
        var unaSerie = registrar(sid, pe2, "{\"actualSets\":%d,\"actualDurationSeconds\":%d}".formatted(n, d));
        assertEquals(0, unaSerie.get("completionPercentage").decimalValue().compareTo(java.math.BigDecimal
                .valueOf(100.0 / n).setScale(2, java.math.RoundingMode.HALF_UP)), unaSerie.toString());
        assertEquals("PARTIAL", unaSerie.get("status").asText(), "N x d hecho pero enviado como d: cuenta 1/N");
        var total = registrar(sid, pe2, "{\"actualSets\":%d,\"actualDurationSeconds\":%d}".formatted(n, n * d));
        assertEquals("COMPLETED", total.get("status").asText(), "con el total, completado");
    }

    private JsonNode registrar(long sesion, long pe, String cuerpo) throws Exception {
        var r = ok(put("/api/v1/sessions/{s}/exercises/{pe}", sesion, pe).contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo));
        for (var e : r.get("exercises")) if (e.get("plannedExerciseId").asLong() == pe) return e;
        throw new AssertionError("el ejercicio no esta en la sesion: " + r);
    }

    /** Segundos indicados de la semana, leidos de planning por la misma fachada que usa la adherencia. */
    private long segundosIndicados() {
        return planificacion.fetchWeekBreakdown(usuario, semana).seconds();
    }
}
