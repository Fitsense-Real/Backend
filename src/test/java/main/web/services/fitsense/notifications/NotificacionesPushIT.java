package main.web.services.fitsense.notifications;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import main.web.services.fitsense.iam.application.internal.deletion.EliminacionDeCuentaService;
import main.web.services.fitsense.iam.application.internal.outboundservices.notification.PasswordResetNotifier;
import main.web.services.fitsense.notifications.application.DetectorEventosPush;
import main.web.services.fitsense.notifications.application.EmisorPush;
import main.web.services.fitsense.notifications.domain.MensajePush;
import main.web.services.fitsense.notifications.domain.TransportePush;
import main.web.services.fitsense.orchestration.application.internal.WeeklyCycleService;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.interfaces.acl.PlanningContextFacade;
import main.web.services.fitsense.shared.domain.model.valueobjects.TrainingWeek;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Notificaciones push (V64) de punta a punta sobre PostgreSQL 16, con registro, login y JWT reales y un transporte
 * SIMULADO en lugar de Firebase: ninguna prueba llama a Google. Los procesos programados no corren solos (retardo de
 * un dia); la prueba invoca el detector y el emisor cuando le toca.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        // Encendido para que el aviso de cuenta se registre; sin credenciales, el transporte real queda no disponible
        // y el que se usa es el simulado (@Primary).
        "fitsense.push.enabled=true",
        "fitsense.push.detector-initial-delay-ms=86400000",
        "fitsense.push.sender-initial-delay-ms=86400000",
        "fitsense.push.max-attempts=3"
})
@Import(NotificacionesPushIT.Simulados.class)
@DisplayName("Notificaciones push: FID, eventos sin duplicados, preferencias, seguridad y fallos de FCM")
class NotificacionesPushIT {

    /** Lo que el transporte simulado "envio", y los resultados que debe devolver (por omision, ENVIADO). */
    static final List<String[]> ENVIOS = new CopyOnWriteArrayList<>();
    static final List<MensajePush> MENSAJES = new CopyOnWriteArrayList<>();
    static final Queue<TransportePush.Resultado> RESPUESTAS = new ConcurrentLinkedQueue<>();
    static final Map<String, List<String>> CORREOS = new ConcurrentHashMap<>();

    @TestConfiguration
    static class Simulados {
        @Bean
        @Primary
        TransportePush transporteSimulado() {
            return new TransportePush() {
                @Override
                public boolean disponible() {
                    return true;
                }

                @Override
                public Resultado enviar(String fid, MensajePush m) {
                    ENVIOS.add(new String[]{fid, m.datos().get("eventType"), m.datos().get("eventId")});
                    MENSAJES.add(m);
                    var r = RESPUESTAS.poll();
                    return r != null ? r : Resultado.enviado("projects/kora-fitpe/messages/" + UUID.randomUUID());
                }
            };
        }

        @Bean
        @Primary
        PasswordResetNotifier buzon() {
            return new PasswordResetNotifier() {
                @Override
                public void enviar(String email, String token, OffsetDateTime caduca) {
                    CORREOS.computeIfAbsent(email, k -> new CopyOnWriteArrayList<>()).add(token);
                }

                @Override
                public boolean entregaReal() {
                    return false;
                }
            };
        }
    }

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    static final AtomicInteger SECUENCIA = new AtomicInteger();
    static final ObjectMapper JSON = new ObjectMapper();
    static final String CLAVE = "clave-sintetica-123";
    static final List<String> GYM = List.of("body weight", "dumbbell", "barbell", "cable", "leverage machine",
            "bench", "band", "stationary bike", "treadmill", "kettlebell", "ez barbell", "smith machine");
    static final LocalDate S1 = TrainingWeek.containing(LocalDate.now()).startDate().plusWeeks(1);

    @Autowired WebApplicationContext contexto;
    @Autowired JdbcTemplate jdbc;
    @Autowired TransactionTemplate tx;
    @Autowired DetectorEventosPush detector;
    @Autowired EmisorPush emisor;
    @Autowired PlanningContextFacade planning;
    @Autowired WeeklyCycleService ciclo;
    @Autowired EliminacionDeCuentaService eliminacion;
    MockMvc http;

    record Cuenta(long userId, String email, String token) {}

    @BeforeEach
    void preparar() {
        http = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        // Cada prueba empieza sin pendientes: lo que dejaron las anteriores se detecta y se cierra aqui (el
        // antiduplicado impide que vuelva a aparecer), asi no se mezcla con lo que mide esta prueba.
        detector.detectar();
        jdbc.update("UPDATE notification_events SET status = 'DESCARTADO', status_reason = 'PRUEBA_ANTERIOR',"
                + " lease_until = NULL, finished_at = now() WHERE status IN ('PENDIENTE', 'EN_ENVIO')");
        ENVIOS.clear();
        MENSAJES.clear();
        RESPUESTAS.clear();
    }

    // ================================================================ endpoints y contrato de identificadores

    @Test
    @DisplayName("registro idempotente por FID; tokens FCM, otra app u otra plataforma se rechazan con codigo")
    void registroDeDispositivos() throws Exception {
        var c = cuenta();
        String fid = fid();
        var r1 = registrar(c, fid);
        assertEquals(201, r1.getResponse().getStatus(), r1.getResponse().getContentAsString());
        var r2 = registrar(c, fid);
        assertEquals(200, r2.getResponse().getStatus(), "el mismo FID no crea otro dispositivo");
        assertEquals(json(r1).get("deviceId"), json(r2).get("deviceId"));
        assertFalse(r1.getResponse().getContentAsString().contains(fid), "el FID completo nunca vuelve en la API");

        assertCodigo(como(c, put("/api/v1/users/me/push-devices").contentType(MediaType.APPLICATION_JSON).content(
                cuerpo("dXy7:APA91bHPRgkF3JUikC4ENAHEeMrd41Zxv3hVZjC9KtT8OvPVGJ-hQMRKRrZuJAEcl7B338qju59zJMjw2DELjzEvxwYv7hH5Ynpc1ODQ0aT4U4OFEeco8ohsN5PjL1iC2dNtk2BAokeMCg2ZXKqpc8FXKmhX94kIxQ",
                        "ANDROID", "pe.fit.kora"))), "IDENTIFICADOR_PUSH_INVALIDO");
        assertCodigo(como(c, put("/api/v1/users/me/push-devices").contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo(fid(), "ANDROID", "com.example.otra"))), "APLICACION_NO_ADMITIDA");
        assertCodigo(como(c, put("/api/v1/users/me/push-devices").contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo(fid(), "IOS", "pe.fit.kora"))), "PLATAFORMA_NO_ADMITIDA");
        assertEquals(1, activos(c.userId()), "ningun rechazo dejo filas");

        // Sin sesion: nada.
        int anonimo = http.perform(put("/api/v1/users/me/push-devices").contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo(fid(), "ANDROID", "pe.fit.kora"))).andReturn().getResponse().getStatus();
        assertTrue(anonimo == 401 || anonimo == 403, "sin JWT: " + anonimo);

        var lista = json(como(c, get("/api/v1/users/me/push-devices")));
        assertEquals(1, lista.size());
        assertTrue(lista.get(0).get("receivesNotifications").asBoolean());

        assertEquals(204, como(c, delete("/api/v1/users/me/push-devices/" + fid)).getResponse().getStatus());
        assertEquals(204, como(c, delete("/api/v1/users/me/push-devices/" + fid)).getResponse().getStatus(),
                "desvincular es idempotente");
        assertEquals(0, activos(c.userId()));
        assertEquals("DESVINCULADO", jdbc.queryForObject("SELECT revoke_reason FROM push_devices WHERE installation_id = ?",
                String.class, fid));
    }

    @Test
    @DisplayName("cambio de cuenta: el mismo FID en otra cuenta revoca el vinculo anterior; nadie desvincula lo ajeno")
    void cambioDeCuenta() throws Exception {
        var a = cuenta();
        var b = cuenta();
        String fid = fid();
        registrar(a, fid);
        assertEquals(201, registrar(b, fid).getResponse().getStatus());
        assertEquals(0, activos(a.userId()), "A deja de recibir en ese telefono");
        assertEquals(1, activos(b.userId()));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM push_devices WHERE user_id = ?"
                + " AND revoke_reason = 'CAMBIO_DE_CUENTA'", Integer.class, a.userId()));
        // A intenta desvincular el FID que ahora es de B: no cambia nada.
        assertEquals(204, como(a, delete("/api/v1/users/me/push-devices/" + fid)).getResponse().getStatus());
        assertEquals(1, activos(b.userId()));
    }

    @Test
    @DisplayName("preferencias: por omision activadas; desactivar PLANES descarta tambien lo pendiente")
    void preferencias() throws Exception {
        var c = conPlan();
        String fid = fid();
        registrar(c.cuenta(), fid);
        var pref = json(como(c.cuenta(), get("/api/v1/users/me/notification-preferences")));
        assertTrue(pref.get("planUpdates").asBoolean());
        assertTrue(pref.get("accountNotices").asBoolean());

        operacionCompletada(c.cuenta().userId(), c.planId());
        assertEquals(1, detector.detectar());
        assertEquals(200, como(c.cuenta(), put("/api/v1/users/me/notification-preferences")
                .contentType(MediaType.APPLICATION_JSON).content("{\"planUpdates\":false,\"accountNotices\":true}"))
                .getResponse().getStatus());
        emisor.procesar();
        assertEquals(List.of(), ENVIOS, "la preferencia se consulta al enviar");
        assertEquals("DESCARTADO/PREFERENCIA_DESACTIVADA", estado(c.planId()));
    }

    // ================================================================ eventos reales del backend

    @Test
    @DisplayName("PLAN_GENERATED: una operacion asincrona COMPLETED se notifica UNA vez, por FID y sin datos de salud")
    void planGenerado() throws Exception {
        var c = conPlan();
        String fid = fid();
        registrar(c.cuenta(), fid);
        assertEquals(0, detector.detectar(), "sin operacion asincrona no hay evento (la generacion sincrona responde en HTTP)");
        operacionCompletada(c.cuenta().userId(), c.planId());

        assertEquals(1, detector.detectar());
        assertEquals(0, detector.detectar(), "idempotente");
        assertEquals(1, emisor.procesar());
        assertEquals(0, emisor.procesar(), "un evento enviado no se vuelve a reclamar");

        assertEquals(1, ENVIOS.size());
        assertEquals(fid, ENVIOS.get(0)[0], "destino: el FID registrado");
        assertEquals("PLAN_GENERATED", ENVIOS.get(0)[1]);
        var datos = MENSAJES.get(0).datos();
        assertEquals(Set.of("schemaVersion", "eventType", "eventId", "resourceType", "resourceId", "targetScreen",
                "createdAt"), datos.keySet());
        assertEquals("PLAN", datos.get("resourceType"));
        assertEquals(String.valueOf(c.planId()), datos.get("resourceId"));
        assertEquals("WEEKLY_PLAN", datos.get("targetScreen"));
        assertEquals("ENVIADO/null", estado(c.planId()));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM notification_deliveries WHERE status = 'ENVIADO'"
                + " AND user_id = ?", Integer.class, c.cuenta().userId()));
    }

    @Test
    @DisplayName("WEEKLY_PLAN_READY con el cierre semanal real; si el mismo plan tiene una adaptacion APLICADA, solo PLAN_ADAPTED")
    void cierreSemanal() throws Exception {
        var c = conPlan();
        registrar(c.cuenta(), fid());
        long u = c.cuenta().userId();
        var semana2 = ciclo.runFor(u, S1, S1.plusWeeks(1)).orElseThrow(() -> new AssertionError("sin semana 2"));
        assertEquals("SIN_ORDEN", jdbc.queryForObject("SELECT status FROM plan_adaptation_attempts WHERE user_id = ?"
                + " ORDER BY attempt_id DESC LIMIT 1", String.class, u));

        // El mismo plan citado por un segundo registro APLICADA (insercion directa, sin los triggers de V32).
        tx.executeWithoutResult(s -> {
            jdbc.execute("SET LOCAL session_replication_role = replica");
            jdbc.update("""
                    INSERT INTO plan_adaptation_attempts (user_id, weekly_metric_id, source_plan_id, new_week_start_date,
                           resulting_plan_id, decision, status, generation_source, ai_calls, targeted_sessions,
                           targeted_interventions, applied_sessions, no_effect_sessions, conserved_sessions,
                           observed_adherence_pct, registration_coverage_pct)
                    SELECT user_id, weekly_metric_id, source_plan_id, new_week_start_date, resulting_plan_id,
                           'ADAPT_LOCALIZED', 'APLICADA', 'AI', 1,
                           ARRAY[(SELECT min(planned_workout_id) FROM planned_workouts WHERE plan_id = resulting_plan_id)],
                           ARRAY['ACORTAR_O_REORGANIZAR']::varchar(40)[],
                           ARRAY[(SELECT min(planned_workout_id) FROM planned_workouts WHERE plan_id = resulting_plan_id)],
                           '{}'::bigint[], '{}'::bigint[],
                           observed_adherence_pct, registration_coverage_pct
                      FROM plan_adaptation_attempts WHERE user_id = ? ORDER BY attempt_id DESC LIMIT 1""", u);
        });

        assertEquals(1, detector.detectar(), "dos registros, un plan: un solo evento");
        assertEquals("PLAN_ADAPTED", jdbc.queryForObject("SELECT event_type FROM notification_events WHERE resource_id = ?",
                String.class, semana2));
        emisor.procesar();
        assertEquals(1, ENVIOS.size());
        assertEquals("PLAN_ADAPTED", ENVIOS.get(0)[1]);
    }

    @Test
    @DisplayName("solo eventos reales: FAILED/NO_FACTIBLE y la generacion sincrona no notifican; RECHAZADA no puede citar un plan")
    void soloEventosReales() throws Exception {
        var c = conPlan();
        registrar(c.cuenta(), fid());
        long u = c.cuenta().userId();
        jdbc.update("INSERT INTO plan_generation_operations (operation_id, user_id, kind, status, week_start_date,"
                + " reason_code, executions, created_at, started_at, finished_at, updated_at) VALUES"
                + " (?, ?, 'FIRST_PLAN', 'FAILED', ?, 'PRUEBA', 1, now(), now(), now(), now()),"
                + " (?, ?, 'FIRST_PLAN', 'NO_FACTIBLE', ?, 'PRUEBA', 1, now(), now(), now(), now())",
                UUID.randomUUID(), u, S1, UUID.randomUUID(), u, S1);
        assertEquals(0, detector.detectar(), "generacion sincrona, FAILED y NO_FACTIBLE: ningun evento");

        var semana2 = ciclo.runFor(u, S1, S1.plusWeeks(1)).orElseThrow();
        long intento = jdbc.queryForObject("SELECT attempt_id FROM plan_adaptation_attempts WHERE user_id = ?"
                + " ORDER BY attempt_id DESC LIMIT 1", Long.class, u);
        // Garantia del esquema (V32, ck_paa_plan_segun_estado): un intento RECHAZADA o NO_FACTIBLE nunca cita un plan,
        // asi que el detector no puede notificar un plan que no se publico.
        for (var estado : List.of("RECHAZADA", "NO_FACTIBLE"))
            assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> jdbc.update(
                    "UPDATE plan_adaptation_attempts SET status = ?, rejection_reason = 'x' WHERE attempt_id = ?",
                    estado, intento), estado);
        assertEquals(1, detector.detectar(), "solo la semana publicada");
        assertEquals(0, detector.detectar());
        assertEquals("WEEKLY_PLAN_READY", jdbc.queryForObject("SELECT event_type FROM notification_events"
                + " WHERE user_id = ? AND resource_id = ?", String.class, u, semana2));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM notification_events WHERE user_id = ?",
                Integer.class, u), "un evento por plan publicado y ninguno mas");
    }

    @Test
    @DisplayName("un plan reemplazado antes del envio no se notifica; un evento mas viejo que el TTL caduca")
    void planNoVigenteYCaducado() throws Exception {
        var c = conPlan();
        registrar(c.cuenta(), fid());
        operacionCompletada(c.cuenta().userId(), c.planId());
        detector.detectar();
        jdbc.update("UPDATE weekly_training_plans SET status = 'REPLACED', replaced_at = now() WHERE plan_id = ?", c.planId());
        emisor.procesar();
        assertEquals("DESCARTADO/PLAN_NO_VIGENTE", estado(c.planId()));

        var d = conPlan();
        registrar(d.cuenta(), fid());
        operacionCompletada(d.cuenta().userId(), d.planId());
        detector.detectar();
        jdbc.update("UPDATE notification_events SET created_at = now() - interval '25 hours' WHERE resource_id = ?", d.planId());
        emisor.procesar();
        assertEquals("CADUCADO/MAS_ANTIGUO_QUE_TTL", estado(d.planId()));
        assertEquals(List.of(), ENVIOS);
    }

    // ================================================================ seguridad y fallos de FCM

    @Test
    @DisplayName("cuenta en eliminacion: no se envia; eliminada: no quedan dispositivos, eventos, entregas ni preferencias")
    void cuentaEliminada() throws Exception {
        var c = conPlan();
        registrar(c.cuenta(), fid());
        como(c.cuenta(), put("/api/v1/users/me/notification-preferences").contentType(MediaType.APPLICATION_JSON)
                .content("{\"planUpdates\":true,\"accountNotices\":false}"));
        operacionCompletada(c.cuenta().userId(), c.planId());
        detector.detectar();
        jdbc.update("UPDATE users SET status = 'DELETED' WHERE user_id = ?", c.cuenta().userId());
        emisor.procesar();
        assertEquals("DESCARTADO/CUENTA_NO_ACTIVA", estado(c.planId()));
        assertEquals(List.of(), ENVIOS);

        jdbc.update("UPDATE users SET status = 'ACTIVE' WHERE user_id = ?", c.cuenta().userId());
        eliminacion.eliminar(c.cuenta().userId(), EliminacionDeCuentaService.Origen.APP);
        for (var t : List.of("push_devices", "notification_events", "notification_deliveries", "notification_preferences"))
            assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM " + t + " WHERE user_id = ?", Integer.class,
                    c.cuenta().userId()), t);
    }

    @Test
    @DisplayName("UNREGISTERED desvincula el FID; un error temporal se reintenta sin duplicar lo ya aceptado")
    void fallosDeFcm() throws Exception {
        var c = conPlan();
        String muerto = fid();
        String vivo = fid();
        registrar(c.cuenta(), muerto);
        registrar(c.cuenta(), vivo);
        operacionCompletada(c.cuenta().userId(), c.planId());
        detector.detectar();

        // Orden de envio: por device_id (muerto primero). Muerto: UNREGISTERED. Vivo: UNAVAILABLE.
        RESPUESTAS.add(TransportePush.Resultado.fallo(TransportePush.Tipo.NO_REGISTRADO, "UNREGISTERED"));
        RESPUESTAS.add(TransportePush.Resultado.fallo(TransportePush.Tipo.ERROR_TEMPORAL, "UNAVAILABLE"));
        emisor.procesar();
        assertEquals("NO_REGISTRADO_EN_FCM", jdbc.queryForObject(
                "SELECT revoke_reason FROM push_devices WHERE installation_id = ?", String.class, muerto));
        assertEquals("PENDIENTE/REINTENTO", estado(c.planId()));
        assertEquals(0, emisor.procesar(), "espera su turno: el reintento no es inmediato");

        jdbc.update("UPDATE notification_events SET next_attempt_at = now() - interval '1 second' WHERE resource_id = ?",
                c.planId());
        emisor.procesar();   // ahora acepta
        assertEquals("ENVIADO/null", estado(c.planId()));
        assertEquals(List.of(muerto, vivo, vivo), ENVIOS.stream().map(e -> e[0]).toList(),
                "el FID desvinculado no se vuelve a intentar");
    }

    @Test
    @DisplayName("FCM caido: los reintentos se agotan sin tocar el plan ni el ciclo; el evento queda FALLIDO")
    void fcmCaido() throws Exception {
        var c = conPlan();
        registrar(c.cuenta(), fid());
        operacionCompletada(c.cuenta().userId(), c.planId());
        detector.detectar();
        for (int i = 0; i < 3; i++) {
            RESPUESTAS.add(TransportePush.Resultado.fallo(TransportePush.Tipo.ERROR_TEMPORAL, "UNAVAILABLE"));
            jdbc.update("UPDATE notification_events SET next_attempt_at = now() - interval '1 second'"
                    + " WHERE resource_id = ? AND status = 'PENDIENTE'", c.planId());
            emisor.procesar();
        }
        assertEquals("FALLIDO/REINTENTOS_AGOTADOS", estado(c.planId()));
        assertEquals("ACTIVE", jdbc.queryForObject("SELECT status FROM weekly_training_plans WHERE plan_id = ?",
                String.class, c.planId()), "el plan sigue publicado: Firebase no participa de la generacion");
        assertTrue(ciclo.runFor(c.cuenta().userId(), S1, S1.plusWeeks(1)).isPresent(),
                "el ciclo semanal funciona con FCM caido");
    }

    @Test
    @DisplayName("ACCOUNT_NOTICE: el cambio de contrasena avisa a los dispositivos de antes y los deja sin recibir hasta registrarse de nuevo")
    void cambioDeContrasena() throws Exception {
        var c = cuenta();
        String fid = fid();
        registrar(c, fid);
        http.perform(post("/api/v1/auth/password-reset/request").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\"}".formatted(c.email()))).andExpect(status().isAccepted());
        var token = CORREOS.get(c.email()).get(CORREOS.get(c.email()).size() - 1);
        http.perform(post("/api/v1/auth/password-reset/confirm").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"%s\",\"newPassword\":\"otra-clave-456\"}".formatted(token)))
                .andExpect(status().is2xxSuccessful());

        var aviso = jdbc.queryForMap("SELECT event_type, notice_code, target_token_version, target_screen"
                + " FROM notification_events WHERE user_id = ?", c.userId());
        assertEquals("ACCOUNT_NOTICE", aviso.get("event_type"));
        assertEquals("PASSWORD_CHANGED", aviso.get("notice_code"));
        assertEquals(0, ((Number) aviso.get("target_token_version")).intValue());
        assertEquals("ACCOUNT_SECURITY", aviso.get("target_screen"));
        emisor.procesar();
        assertEquals(1, ENVIOS.size(), "el telefono registrado antes del cambio recibe el aviso");
        assertEquals("PASSWORD_CHANGED", MENSAJES.get(0).datos().get("noticeCode"));
        assertFalse(MENSAJES.get(0).datos().containsKey("resourceId"));

        // Con la sesion nueva: el dispositivo viejo ya no recibe hasta registrarse otra vez.
        var nueva = login(c.email(), "otra-clave-456");
        var lista = json(como(nueva, get("/api/v1/users/me/push-devices")));
        assertFalse(lista.get(0).get("receivesNotifications").asBoolean());
        assertEquals(200, registrar(nueva, fid).getResponse().getStatus());
        assertTrue(json(como(nueva, get("/api/v1/users/me/push-devices"))).get(0).get("receivesNotifications").asBoolean());
    }

    // ================================================================ apoyo

    record ConPlan(Cuenta cuenta, long planId) {}

    ConPlan conPlan() throws Exception {
        var c = cuenta();
        ok(c, post("/api/v1/users/me/profile").contentType(MediaType.APPLICATION_JSON).content("""
                {"birthDate":"1990-01-01","biologicalSex":"FEMALE","heightCm":165,"currentWeightKg":68,
                 "fitnessLevel":"INTERMEDIATE","goalType":"GAIN_MUSCLE","goalText":"texto sintetico",
                 "trainingLocation":"GYM","daysPerWeek":3,"availableDays":[1,3,5],"sessionMinutes":45,
                 "equipmentCodes":%s,"blockedExerciseIds":[],"healthNotes":"nota sintetica",
                 "allowHighImpact":true,"allowFloorExercises":true,"allowKneelingExercises":true,
                 "allowHangingExercises":true,"allowOverheadExercises":true}""".formatted(JSON.writeValueAsString(GYM))));
        long plan = planning.generateWeeklyPlan(c.userId(), S1, PlanAdjustment.none(), null, null).orElseThrow();
        return new ConPlan(c, plan);
    }

    /** La fila que deja la generacion asincrona al publicar (V58): COMPLETED con su plan. */
    void operacionCompletada(long userId, long planId) {
        jdbc.update("INSERT INTO plan_generation_operations (operation_id, user_id, kind, status, week_start_date, plan_id,"
                        + " executions, created_at, started_at, finished_at, updated_at)"
                        + " VALUES (?, ?, 'FIRST_PLAN', 'COMPLETED', ?, ?, 1, now(), now(), now(), now())",
                UUID.randomUUID(), userId, S1, planId);
    }

    String estado(long planId) {
        var f = jdbc.queryForMap("SELECT status, status_reason FROM notification_events WHERE resource_id = ?", planId);
        return f.get("status") + "/" + f.get("status_reason");
    }

    int activos(long userId) {
        return jdbc.queryForObject("SELECT count(*) FROM push_devices WHERE user_id = ? AND revoked_at IS NULL",
                Integer.class, userId);
    }

    static String fid() {
        // Forma de un FID: 22 caracteres base64url.
        var s = UUID.randomUUID().toString().replace("-", "");
        return ("c" + s).substring(0, 22);
    }

    static String cuerpo(String fid, String plataforma, String app) {
        return "{\"installationId\":\"%s\",\"platform\":\"%s\",\"appId\":\"%s\",\"appVersion\":\"1.0.0+1\"}"
                .formatted(fid, plataforma, app);
    }

    MvcResult registrar(Cuenta c, String fid) throws Exception {
        return como(c, put("/api/v1/users/me/push-devices").contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo(fid, "ANDROID", "pe.fit.kora")));
    }

    MvcResult como(Cuenta c, MockHttpServletRequestBuilder p) throws Exception {
        return http.perform(p.header("Authorization", "Bearer " + c.token())).andReturn();
    }

    JsonNode ok(Cuenta c, MockHttpServletRequestBuilder p) throws Exception {
        var r = como(c, p);
        assertEquals(2, r.getResponse().getStatus() / 100, p + " -> " + r.getResponse().getContentAsString());
        return json(r);
    }

    static JsonNode json(MvcResult r) throws Exception {
        var s = r.getResponse().getContentAsString();
        return s.isBlank() ? null : JSON.readTree(s);
    }

    static void assertCodigo(MvcResult r, String codigo) throws Exception {
        assertEquals(400, r.getResponse().getStatus(), r.getResponse().getContentAsString());
        assertEquals(codigo, json(r).get("details").get(0).asText());
    }

    Cuenta cuenta() throws Exception {
        String email = "push" + SECUENCIA.incrementAndGet() + "@pruebas.invalid";
        http.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email":"%s","password":"%s","firstName":"Sintetico","lastName":"Push",
                         "timezone":"America/Lima","acceptsStudyParticipation":true}""".formatted(email, CLAVE)))
                .andExpect(status().isCreated());
        return login(email, CLAVE);
    }

    Cuenta login(String email, String clave) throws Exception {
        String res = http.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, clave)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Matcher m = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"").matcher(res);
        assertTrue(m.find(), res);
        return new Cuenta(jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email),
                email, m.group(1));
    }
}
