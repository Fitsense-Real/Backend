package main.web.services.fitsense.notifications;

import main.web.services.fitsense.notifications.application.DetectorEventosPush;
import main.web.services.fitsense.notifications.application.EmisorPush;
import main.web.services.fitsense.notifications.domain.TransportePush;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.interfaces.acl.PlanningContextFacade;
import main.web.services.fitsense.shared.domain.model.valueobjects.TrainingWeek;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/** Apoyo comun de las tres pruebas de arranque real de Spring Boot con push desactivado, sin credenciales y activo. */
abstract class ArranquePushBase {

    static final String FID = "cR_9l0aBcD3fGh1jK2lM4n";
    static final LocalDate S1 = TrainingWeek.containing(LocalDate.now()).startDate().plusWeeks(1);

    @Autowired WebApplicationContext contexto;
    @Autowired JdbcTemplate jdbc;
    @Autowired TransportePush transporte;
    @Autowired DetectorEventosPush detector;
    @Autowired EmisorPush emisor;
    @Autowired PlanningContextFacade planning;

    record Cuenta(long userId, String token) {}

    MockMvc http() {
        return MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    Cuenta cuenta(String email) throws Exception {
        var http = http();
        http.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"%s","password":"clave-sintetica-123","firstName":"Sintetico","lastName":"Arranque",
                 "timezone":"America/Lima","acceptsStudyParticipation":true}""".formatted(email)));
        var res = http.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"clave-sintetica-123\"}".formatted(email)))
                .andReturn().getResponse().getContentAsString();
        var m = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"").matcher(res);
        assertTrue(m.find(), res);
        return new Cuenta(jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email), m.group(1));
    }

    int registrarDispositivo(Cuenta c) throws Exception {
        return http().perform(put("/api/v1/users/me/push-devices").header("Authorization", "Bearer " + c.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"installationId\":\"%s\",\"platform\":\"ANDROID\",\"appId\":\"pe.fit.kora\"}".formatted(FID)))
                .andReturn().getResponse().getStatus();
    }

    /** Perfil, primer plan y la fila COMPLETED que deja la generacion asincrona. Devuelve el plan. */
    long planGeneradoAsincrono(Cuenta c) throws Exception {
        http().perform(post("/api/v1/users/me/profile").header("Authorization", "Bearer " + c.token())
                .contentType(MediaType.APPLICATION_JSON).content("""
                {"birthDate":"1990-01-01","biologicalSex":"FEMALE","heightCm":165,"currentWeightKg":68,
                 "fitnessLevel":"INTERMEDIATE","goalType":"GAIN_MUSCLE","goalText":"texto sintetico",
                 "trainingLocation":"HOME","daysPerWeek":3,"availableDays":[1,3,5],"sessionMinutes":45,
                 "equipmentCodes":["body weight","dumbbell","bench"],"blockedExerciseIds":[],"healthNotes":"nota",
                 "allowHighImpact":true,"allowFloorExercises":true,"allowKneelingExercises":true,
                 "allowHangingExercises":true,"allowOverheadExercises":true}"""));
        long plan = planning.generateWeeklyPlan(c.userId(), S1, PlanAdjustment.none(), null, null).orElseThrow();
        jdbc.update("INSERT INTO plan_generation_operations (operation_id, user_id, kind, status, week_start_date, plan_id,"
                        + " executions, created_at, started_at, finished_at, updated_at)"
                        + " VALUES (?, ?, 'FIRST_PLAN', 'COMPLETED', ?, ?, 1, now(), now(), now(), now())",
                UUID.randomUUID(), c.userId(), S1, plan);
        return plan;
    }
}
