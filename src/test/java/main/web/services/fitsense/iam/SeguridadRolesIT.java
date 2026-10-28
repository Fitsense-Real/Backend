package main.web.services.fitsense.iam;

import main.web.services.fitsense.execution.infrastructure.scheduling.StaleSessionScheduler;
import main.web.services.fitsense.orchestration.infrastructure.scheduling.WeeklyCycleScheduler;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.infrastructure.scheduling.OverdueWorkoutScheduler;
import main.web.services.fitsense.planning.interfaces.acl.PlanningContextFacade;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.BiologicalSex;
import main.web.services.fitsense.profiling.domain.model.valueobjects.FitnessLevel;
import main.web.services.fitsense.profiling.domain.model.valueobjects.GoalType;
import main.web.services.fitsense.profiling.domain.model.valueobjects.TrainingLocation;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import main.web.services.fitsense.shared.domain.model.valueobjects.TrainingWeek;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

/**
 * ROLES Y AUTORIZACION, de punta a punta: registro y login reales, JWT reales por
 * la cabecera Authorization y el filtro de produccion. Nada se simula con
 * {@code with(user(...))}: lo que se prueba es justamente quien llega a que.
 * <p>
 * PostgreSQL 16 efimero con V1-V35, IA desactivada. Las tareas programadas se
 * invocan directamente, sin contexto de seguridad, como las invoca el planificador.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.cors.allowed-origins=http://localhost:4200, https://panel.pruebas.invalid"
})
@DisplayName("Seguridad · roles USER/ADMIN, aislamiento de datos y endurecimiento")
class SeguridadRolesIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired WebApplicationContext contexto;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserProfileCommandService perfiles;
    @Autowired PlanningContextFacade planning;
    @Autowired StaleSessionScheduler colgadas;
    @Autowired OverdueWorkoutScheduler vencidos;
    @Autowired WeeklyCycleScheduler ciclo;

    MockMvc http;

    static final AtomicInteger SECUENCIA = new AtomicInteger();
    static final String CLAVE = "clave-de-prueba-123";
    static final LocalDate SEMANA = TrainingWeek.containing(LocalDate.now()).startDate().plusWeeks(1);
    static final Path SCRIPT_ROL = Path.of("docs/FitSense/operaciones/cambiar_rol.sql");

    static final List<String> RUTAS_ADMIN = List.of(
            "/api/v1/admin/analytics/overview",
            "/api/v1/admin/analytics/adherence",
            "/api/v1/admin/analytics/adherence/trends",
            "/api/v1/admin/analytics/segments?by=GOAL_TYPE",
            "/api/v1/admin/participants",
            "/api/v1/admin/participants/1",
            "/api/v1/admin/participants/1/adherence",
            "/api/v1/admin/config/active");

    record Cuenta(long userId, String email, String token) {}

    @BeforeEach
    void http() {
        http = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    // ------------------------------------------------------------------ apoyo

    private long registrar(String email, boolean participa, String extra) throws Exception {
        String cuerpo = """
                {"email":"%s","password":"%s","firstName":"Prueba","lastName":"Roles",
                 "timezone":"America/Lima","acceptsStudyParticipation":%s%s}
                """.formatted(email, CLAVE, participa, extra);
        http.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"));
        return jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
    }

    private String login(String email) throws Exception {
        var res = http.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, CLAVE)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Matcher m = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"").matcher(res);
        assertTrue(m.find(), res);
        return m.group(1);
    }

    private Cuenta usuario(boolean participa) throws Exception {
        String email = "roles" + SECUENCIA.incrementAndGet() + "@pruebas.invalid";
        long id = registrar(email, participa, "");
        return new Cuenta(id, email, login(email));
    }

    private Cuenta usuario() throws Exception {
        return usuario(true);
    }

    private Cuenta admin() throws Exception {
        var c = usuario(false);
        cambiarRol(c, "ADMIN", "CAMBIAR %d A ADMIN".formatted(c.userId()));
        return c;
    }

    /** Ejecuta el procedimiento real de docs/FitSense/operaciones con estas constantes. */
    private void cambiarRol(Cuenta c, String rol, String confirma) throws Exception {
        cambiarRol(c.email(), c.userId(), rol, "Responsable de pruebas", confirma);
    }

    private void cambiarRol(String email, long userId, String rol, String operador, String confirma)
            throws Exception {
        String sql = Files.readString(SCRIPT_ROL);
        sql = constante(sql, "v_email", "'" + email + "'");
        sql = constante(sql, "v_user_id", String.valueOf(userId));
        sql = constante(sql, "v_rol", "'" + rol + "'");
        sql = constante(sql, "v_operador", "'" + operador + "'");
        sql = constante(sql, "v_confirma", "'" + confirma + "'");
        jdbc.execute(sql);
    }

    private static String constante(String sql, String nombre, String valor) {
        var m = Pattern.compile("(" + nombre + "\\s+\\w+\\s+:= )[^;]+;").matcher(sql);
        assertTrue(m.find(), "el script ya no declara " + nombre);
        return m.replaceFirst(Matcher.quoteReplacement(m.group(1) + valor + ";"));
    }

    private String rol(long userId) {
        return jdbc.queryForObject("SELECT role FROM users WHERE user_id = ?", String.class, userId);
    }

    private ResultActions como(Cuenta c, MockHttpServletRequestBuilder peticion) throws Exception {
        return http.perform(peticion.header("Authorization", "Bearer " + c.token()));
    }

    private void perfil(long userId, GoalType objetivo) {
        perfiles.handle(new CreateUserProfileCommand(userId, LocalDate.of(1992, 5, 10), BiologicalSex.FEMALE,
                new BigDecimal("165"), new BigDecimal("68"), null, FitnessLevel.INTERMEDIATE, objetivo,
                "prueba de roles", TrainingLocation.GYM, (short) 3,
                List.of((short) 1, (short) 3, (short) 5), (short) 45,
                List.of("dumbbell", "barbell", "cable", "leverage machine", "bench", "band"),
                List.of(), "Ninguna", true, true, true, true, true));
    }

    private long plan(Cuenta c) {
        perfil(c.userId(), GoalType.GAIN_MUSCLE);
        return planning.generateWeeklyPlan(c.userId(), SEMANA, PlanAdjustment.none(), null, null)
                .orElseThrow(() -> new AssertionError("no se genero la semana"));
    }

    private List<Long> entrenamientos(long planId) {
        return jdbc.queryForList("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ?"
                + " ORDER BY scheduled_date", Long.class, planId);
    }

    private long primerEjercicio(long plannedWorkoutId) {
        return jdbc.queryForObject("SELECT planned_exercise_id FROM planned_workout_exercises"
                + " WHERE planned_workout_id = ? ORDER BY exercise_order LIMIT 1", Long.class, plannedWorkoutId);
    }

    private long iniciar(Cuenta c, long plannedWorkoutId) throws Exception {
        var res = como(c, post("/api/v1/workouts/" + plannedWorkoutId + "/start"))
                .andExpect(status().is2xxSuccessful()).andReturn().getResponse().getContentAsString();
        Matcher m = Pattern.compile("\"sessionId\"\\s*:\\s*(\\d+)").matcher(res);
        assertTrue(m.find(), res);
        return Long.parseLong(m.group(1));
    }

    private String registroDe(long plannedExerciseId) {
        boolean duracion = "DURATION".equals(jdbc.queryForObject("SELECT prescription_type FROM"
                + " planned_workout_exercises WHERE planned_exercise_id = ?", String.class, plannedExerciseId));
        return duracion ? "{\"actualSets\":1,\"actualDurationSeconds\":20}" : "{\"actualSets\":1,\"actualRepsTotal\":5}";
    }

    // ---------------------------------------------------------------- pruebas

    @Nested
    @DisplayName("USER: sus funciones, nada de administracion")
    class Usuario {

        @Test
        @DisplayName("accede a sus datos y al catalogo")
        void susFunciones() throws Exception {
            var u = usuario();
            como(u, get("/api/v1/users/me")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(u.userId()))
                    .andExpect(jsonPath("$.role").value("USER"));
            como(u, get("/api/v1/users/me/plans")).andExpect(status().isOk());
            como(u, get("/api/v1/users/me/interventions")).andExpect(status().isOk());
            como(u, get("/api/v1/users/me/metrics/weekly")).andExpect(status().isOk());
            como(u, get("/api/v1/catalog/body-parts")).andExpect(status().isOk());
        }

        @Test
        @DisplayName("recibe 403 en cada endpoint de /api/v1/admin")
        void prohibidoEnAdmin() throws Exception {
            var u = usuario();
            for (String ruta : RUTAS_ADMIN)
                como(u, get(ruta)).andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.status").value(403));
        }

        @Test
        @DisplayName("sin token: 401 en rutas de usuario y de administracion")
        void anonimo() throws Exception {
            http.perform(get("/api/v1/users/me")).andExpect(status().isUnauthorized());
            for (String ruta : RUTAS_ADMIN)
                http.perform(get(ruta)).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("una ruta fuera del contrato se deniega por defecto")
        void denegadoPorDefecto() throws Exception {
            var u = usuario();
            como(u, get("/api/v2/users/me")).andExpect(status().isForbidden());
            como(u, get("/interno/cualquier-cosa")).andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Aislamiento: un USER nunca ve recursos de otro")
    class Aislamiento {

        @Test
        @DisplayName("plan, entrenamiento y sesion ajenos responden 404")
        void recursosAjenos() throws Exception {
            var a = usuario();
            var b = usuario();
            long planA = plan(a);
            long w = entrenamientos(planA).get(0);

            como(b, get("/api/v1/users/me/plan/" + planA)).andExpect(status().isNotFound());
            como(b, post("/api/v1/workouts/" + w + "/start")).andExpect(status().isNotFound());

            long sesion = iniciar(a, w);
            como(b, get("/api/v1/sessions/" + sesion)).andExpect(status().isNotFound());
            como(b, post("/api/v1/sessions/" + sesion + "/abandon")).andExpect(status().isNotFound());
            como(a, get("/api/v1/sessions/" + sesion)).andExpect(status().isOk());
        }

        @Test
        @DisplayName("PUT /sessions/{id}/exercises/{peId}: el ejercicio tiene que ser de esa sesion")
        void propiedadDelEjercicio() throws Exception {
            var a = usuario();
            var b = usuario();
            long planA = plan(a);
            long planB = plan(b);
            var wa = entrenamientos(planA);
            long sesion = iniciar(a, wa.get(0));

            long propio = primerEjercicio(wa.get(0));
            long deOtroEntrenamiento = primerEjercicio(wa.get(1));
            long deOtraPersona = primerEjercicio(entrenamientos(planB).get(0));

            for (long ajeno : List.of(deOtroEntrenamiento, deOtraPersona)) {
                como(a, put("/api/v1/sessions/" + sesion + "/exercises/" + ajeno)
                        .contentType(MediaType.APPLICATION_JSON).content(registroDe(ajeno)))
                        .andExpect(status().isUnprocessableEntity());
            }
            // La sesion de A, con el ejercicio de A, pero pedida por B: 404.
            como(b, put("/api/v1/sessions/" + sesion + "/exercises/" + propio)
                    .contentType(MediaType.APPLICATION_JSON).content(registroDe(propio)))
                    .andExpect(status().isNotFound());
            assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM workout_session_exercises"
                    + " WHERE session_id = ?", Integer.class, sesion), "nada ajeno quedo registrado");

            como(a, put("/api/v1/sessions/" + sesion + "/exercises/" + propio)
                    .contentType(MediaType.APPLICATION_JSON).content(registroDe(propio)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("ADMIN: hereda USER y ve la administracion")
    class Administrador {

        @Test
        @DisplayName("usa las funciones de USER")
        void heredaUsuario() throws Exception {
            var adm = admin();
            como(adm, get("/api/v1/users/me")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.role").value("ADMIN"));
            como(adm, get("/api/v1/users/me/metrics/weekly")).andExpect(status().isOk());
            como(adm, get("/api/v1/catalog/body-parts")).andExpect(status().isOk());
            como(adm, get("/api/v1/config/active")).andExpect(status().isOk());
        }

        @Test
        @DisplayName("el login informa el rol a la interfaz")
        void loginConRol() throws Exception {
            var adm = admin();
            http.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(adm.email(), CLAVE)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.role").value("ADMIN"));
        }

        @Test
        @DisplayName("todas las rutas de /api/v1/admin responden a ADMIN")
        void rutasAdmin() throws Exception {
            var adm = admin();
            var u = usuario();
            como(adm, get("/api/v1/admin/analytics/overview")).andExpect(status().isOk());
            como(adm, get("/api/v1/admin/analytics/adherence")).andExpect(status().isOk());
            como(adm, get("/api/v1/admin/analytics/adherence/trends")).andExpect(status().isOk());
            for (String by : List.of("GOAL_TYPE", "FITNESS_LEVEL", "TRAINING_LOCATION", "RISK_LEVEL"))
                como(adm, get("/api/v1/admin/analytics/segments?by=" + by)).andExpect(status().isOk());
            como(adm, get("/api/v1/admin/participants")).andExpect(status().isOk());
            como(adm, get("/api/v1/admin/participants/" + u.userId())).andExpect(status().isOk());
            como(adm, get("/api/v1/admin/participants/" + u.userId() + "/adherence")).andExpect(status().isOk());
            como(adm, get("/api/v1/admin/config/active")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.params.adherence").exists());
        }

        @Test
        @DisplayName("filtros invalidos: 400; participante inexistente o ADMIN: 404")
        void entradaInvalida() throws Exception {
            var adm = admin();
            como(adm, get("/api/v1/admin/analytics/overview?from=2020-02-01&to=2020-01-01"))
                    .andExpect(status().isBadRequest());
            como(adm, get("/api/v1/admin/analytics/segments?by=EMAIL")).andExpect(status().isBadRequest());
            como(adm, get("/api/v1/admin/participants?size=1000")).andExpect(status().isBadRequest());
            como(adm, get("/api/v1/admin/participants?studyStatus=X")).andExpect(status().isBadRequest());
            como(adm, get("/api/v1/admin/participants/999999999")).andExpect(status().isNotFound());
            como(adm, get("/api/v1/admin/participants/" + adm.userId())).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Analitica: cifras de las columnas existentes, poblacion del estudio")
    class Analitica {

        /** Semanas de 2020: ningun otro caso escribe ahi. */
        static final LocalDate S1 = LocalDate.of(2020, 1, 6);
        static final LocalDate S2 = LocalDate.of(2020, 1, 13);

        private void semana(long userId, LocalDate lunes, int numero, boolean conPlan, Double adherencia,
                            Double cobertura, String riesgo, boolean abandono) {
            jdbc.update("""
                    INSERT INTO weekly_user_metrics (user_id, week_number, week_start_date, week_end_date,
                        has_active_plan, scheduled_workouts, valid_workouts, completed_workouts,
                        recorded_workouts, weighted_adherence_pct, registration_coverage_pct, risk_level,
                        is_dropout, calculation_version)
                    VALUES (?, ?, ?, ?, ?, ?, ?, 0, ?, ?, ?, ?, ?, 'MVP-PRUEBA')
                    """, userId, numero, lunes, lunes.plusDays(6), conPlan, conPlan ? 3 : 0,
                    conPlan ? 3 : 0, conPlan ? 3 : 0, adherencia, cobertura, riesgo, abandono);
        }

        @Test
        @DisplayName("overview, tendencia, distribucion, segmentos y serie de un participante")
        void cifras() throws Exception {
            var adm = admin();
            var p1 = usuario();
            var p2 = usuario();
            var ajeno = usuario(false);   // sin consentimiento de estudio: no entra
            perfil(p1.userId(), GoalType.GAIN_MUSCLE);

            semana(p1.userId(), S1, 1, true, 100.0, 100.0, "LOW", false);
            semana(p1.userId(), S2, 2, true, 50.0, 100.0, "MODERATE", false);
            semana(p2.userId(), S1, 1, true, 0.0, 0.0, "HIGH", false);
            semana(p2.userId(), S2, 2, false, null, null, null, true);
            semana(ajeno.userId(), S1, 1, true, 10.0, 100.0, "CRITICAL", false);
            semana(adm.userId(), S1, 1, true, 90.0, 100.0, "LOW", false);

            String periodo = "?from=2020-01-06&to=2020-01-13";
            como(adm, get("/api/v1/admin/analytics/overview" + periodo)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.measurement.participantsMeasured").value(2))
                    .andExpect(jsonPath("$.measurement.weeksMeasured").value(4))
                    .andExpect(jsonPath("$.measurement.weeksWithPlan").value(3))
                    .andExpect(jsonPath("$.measurement.weeksWithAdherence").value(3))
                    .andExpect(jsonPath("$.officialAdherence.meanPct").value(50.0))
                    .andExpect(jsonPath("$.officialAdherence.medianPct").value(50.0))
                    .andExpect(jsonPath("$.latestWeek.participants").value(2))
                    .andExpect(jsonPath("$.latestWeek.riskLevels.MODERATE").value(1))
                    .andExpect(jsonPath("$.latestWeek.riskLevels.NONE").value(1))
                    .andExpect(jsonPath("$.latestWeek.dropouts").value(1))
                    .andExpect(jsonPath("$.enrollment.enrolled").value(greaterThanOrEqualTo(2)));

            como(adm, get("/api/v1/admin/analytics/adherence/trends" + periodo)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.weeks.length()").value(2))
                    .andExpect(jsonPath("$.weeks[0].weekStartDate").value("2020-01-06"))
                    .andExpect(jsonPath("$.weeks[0].participantsMeasured").value(2))
                    .andExpect(jsonPath("$.weeks[0].meanOfficialAdherencePct").value(50.0))
                    .andExpect(jsonPath("$.weeks[0].riskLevels.HIGH").value(1))
                    .andExpect(jsonPath("$.weeks[1].weeksWithPlan").value(1))
                    .andExpect(jsonPath("$.weeks[1].dropouts").value(1));

            como(adm, get("/api/v1/admin/analytics/adherence" + periodo)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.indicators[0].indicator").value("OFFICIAL_ADHERENCE"))
                    .andExpect(jsonPath("$.indicators[0].column").value("weighted_adherence_pct"))
                    .andExpect(jsonPath("$.indicators[0].weeks").value(3))
                    .andExpect(jsonPath("$.indicators[0].minPct").value(0.0))
                    .andExpect(jsonPath("$.indicators[0].maxPct").value(100.0))
                    .andExpect(jsonPath("$.indicators[*].indicator", not(hasItem("OBSERVED_ADHERENCE"))));

            como(adm, get("/api/v1/admin/analytics/segments" + periodo + "&by=GOAL_TYPE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.segments[?(@.value=='GAIN_MUSCLE')].meanOfficialAdherencePct").value(75.0))
                    .andExpect(jsonPath("$.segments[?(@.value=='NONE')].meanOfficialAdherencePct").value(0.0));

            como(adm, get("/api/v1/admin/participants/" + p1.userId() + "/adherence" + periodo))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.participantCode").isNotEmpty())
                    .andExpect(jsonPath("$.weeks.length()").value(2))
                    .andExpect(jsonPath("$.weeks[0].officialAdherencePct").value(100.0))
                    .andExpect(jsonPath("$.weeks[1].officialAdherencePct").value(50.0))
                    .andExpect(jsonPath("$.email").doesNotExist());
        }

        @Test
        @DisplayName("listado seudonimizado; nombre y correo solo en el detalle")
        void datosPersonales() throws Exception {
            var adm = admin();
            var p = usuario();
            var lista = como(adm, get("/api/v1/admin/participants?size=200")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.items[*].participantId", hasItem((int) p.userId())))
                    .andExpect(jsonPath("$.items[*].participantId", not(hasItem((int) adm.userId()))))
                    .andReturn().getResponse().getContentAsString();
            assertFalse(lista.contains("@pruebas.invalid"), "el listado no lleva correos");
            assertFalse(lista.contains("\"firstName\""), "el listado no lleva nombres");

            var resumen = como(adm, get("/api/v1/admin/analytics/overview")).andReturn()
                    .getResponse().getContentAsString();
            assertFalse(resumen.contains("@"), "la analitica no lleva datos personales");

            como(adm, get("/api/v1/admin/participants/" + p.userId())).andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(p.email()))
                    .andExpect(jsonPath("$.participantCode").isNotEmpty())
                    .andExpect(jsonPath("$.healthNotes").doesNotExist());
        }
    }

    @Nested
    @DisplayName("Cuentas y roles")
    class CuentasYRoles {

        @Test
        @DisplayName("un JWT vigente de una cuenta INACTIVE ya no autentica")
        void cuentaInactiva() throws Exception {
            var u = usuario();
            como(u, get("/api/v1/users/me")).andExpect(status().isOk());
            jdbc.update("UPDATE users SET status = 'INACTIVE' WHERE user_id = ?", u.userId());
            como(u, get("/api/v1/users/me")).andExpect(status().isUnauthorized());
            como(u, get("/api/v1/catalog/body-parts")).andExpect(status().isUnauthorized());
            jdbc.update("UPDATE users SET status = 'DELETED' WHERE user_id = ?", u.userId());
            como(u, get("/api/v1/users/me")).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("el registro publico nunca crea ADMIN, aunque el cuerpo lo pida")
        void registroSinAdmin() throws Exception {
            String email = "intruso" + SECUENCIA.incrementAndGet() + "@pruebas.invalid";
            long id = registrar(email, true,
                    ",\"role\":\"ADMIN\",\"roles\":[\"ADMIN\"],\"authorities\":[\"ROLE_ADMIN\"]");
            assertEquals("USER", rol(id));
            var c = new Cuenta(id, email, login(email));
            como(c, get("/api/v1/admin/analytics/overview")).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("el cambio de rol vale desde la peticion siguiente, con el mismo token")
        void cambioDeRol() throws Exception {
            var u = usuario();
            como(u, get("/api/v1/admin/analytics/overview")).andExpect(status().isForbidden());
            cambiarRol(u, "ADMIN", "CAMBIAR %d A ADMIN".formatted(u.userId()));
            como(u, get("/api/v1/admin/analytics/overview")).andExpect(status().isOk());
            cambiarRol(u, "USER", "CAMBIAR %d A USER".formatted(u.userId()));
            como(u, get("/api/v1/admin/analytics/overview")).andExpect(status().isForbidden());
            como(u, get("/api/v1/users/me")).andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Promocion a ADMIN: solo con autorizacion explicita")
    class Promocion {

        @Test
        @DisplayName("el script tal como esta en el repositorio no hace nada")
        void scriptSinRellenar() throws Exception {
            String antes = jdbc.queryForObject("SELECT count(*) FROM users WHERE role = 'ADMIN'", String.class);
            var e = assertThrows(DataAccessException.class, () -> jdbc.execute(Files.readString(SCRIPT_ROL)));
            assertThat(e.getMessage(), containsString("v_rol debe ser ADMIN o USER"));
            assertEquals(antes, jdbc.queryForObject("SELECT count(*) FROM users WHERE role = 'ADMIN'", String.class));
        }

        @Test
        @DisplayName("se rechaza sin confirmacion, sin operador, con datos que no cuadran o sin titularidad")
        void rechazos() throws Exception {
            var u = usuario();
            String ok = "CAMBIAR %d A ADMIN".formatted(u.userId());

            var sinConfirmar = assertThrows(DataAccessException.class, () -> cambiarRol(u, "ADMIN", ""));
            assertThat(sinConfirmar.getMessage(), containsString("Confirmacion ausente o incorrecta"));
            assertThrows(DataAccessException.class,
                    () -> cambiarRol(u, "ADMIN", "CAMBIAR %d A ADMIN".formatted(u.userId() + 1)));
            assertThat(assertThrows(DataAccessException.class,
                    () -> cambiarRol(u.email(), u.userId(), "ADMIN", " ", ok)).getMessage(),
                    containsString("v_operador es obligatorio"));
            assertThat(assertThrows(DataAccessException.class,
                    () -> cambiarRol("otro@pruebas.invalid", u.userId(), "ADMIN", "Resp", ok)).getMessage(),
                    containsString("No hay una cuenta"));

            // Registrada pero nunca inicio sesion: no hay prueba de titularidad.
            String email = "sinlogin" + SECUENCIA.incrementAndGet() + "@pruebas.invalid";
            long sinLogin = registrar(email, true, "");
            assertThat(assertThrows(DataAccessException.class, () -> cambiarRol(email, sinLogin, "ADMIN", "Resp",
                    "CAMBIAR %d A ADMIN".formatted(sinLogin))).getMessage(), containsString("nunca inicio sesion"));

            jdbc.update("UPDATE users SET status = 'INACTIVE' WHERE user_id = ?", u.userId());
            assertThat(assertThrows(DataAccessException.class, () -> cambiarRol(u, "ADMIN", ok)).getMessage(),
                    containsString("no esta activa"));

            assertEquals("USER", rol(u.userId()));
            assertEquals("USER", rol(sinLogin));
        }

        @Test
        @DisplayName("la base rechaza un ADMIN sin rastro de quien lo promovio")
        void sinAuditoria() throws Exception {
            var u = usuario();
            assertThrows(DataIntegrityViolationException.class,
                    () -> jdbc.update("UPDATE users SET role = 'ADMIN' WHERE user_id = ?", u.userId()));
            assertThrows(DataIntegrityViolationException.class,
                    () -> jdbc.update("UPDATE users SET role = 'ROOT' WHERE user_id = ?", u.userId()));
            assertEquals("USER", rol(u.userId()));
        }

        @Test
        @DisplayName("con todo en regla promueve y deja quien y cuando")
        void promocion() throws Exception {
            var u = usuario();
            cambiarRol(u.email(), u.userId(), "ADMIN", "Investigadora responsable",
                    "CAMBIAR %d A ADMIN".formatted(u.userId()));
            var fila = jdbc.queryForMap("SELECT role, role_updated_at, role_updated_by FROM users"
                    + " WHERE user_id = ?", u.userId());
            assertEquals("ADMIN", fila.get("role"));
            assertNotNull(fila.get("role_updated_at"));
            assertEquals("Investigadora responsable", fila.get("role_updated_by"));
        }
    }

    @Nested
    @DisplayName("Endurecimiento")
    class Endurecimiento {

        @Test
        @DisplayName("run-for-me queda bloqueado para USER y para ADMIN")
        void cicloManual() throws Exception {
            como(usuario(), post("/api/v1/cycle/run-for-me")).andExpect(status().isForbidden());
            como(admin(), post("/api/v1/cycle/run-for-me")).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Swagger y /v3/api-docs apagados")
        void documentacion() throws Exception {
            http.perform(get("/v3/api-docs")).andExpect(status().isUnauthorized());
            http.perform(get("/swagger-ui/index.html")).andExpect(status().isUnauthorized());
            como(admin(), get("/v3/api-docs")).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("config/active: USER ve solo lo que usa la app")
        void configuracion() throws Exception {
            como(usuario(), get("/api/v1/config/active")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.version").isNotEmpty())
                    .andExpect(jsonPath("$.retroactiveReportDays").isNumber())
                    .andExpect(jsonPath("$.params").doesNotExist())
                    .andExpect(jsonPath("$.description").doesNotExist());
        }

        @Test
        @DisplayName("CORS: solo los origenes configurados, sin credenciales de navegador")
        void cors() throws Exception {
            http.perform(options("/api/v1/admin/analytics/overview")
                            .header("Origin", "http://localhost:4200")
                            .header("Access-Control-Request-Method", "GET")
                            .header("Access-Control-Request-Headers", "Authorization"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"))
                    .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
            http.perform(options("/api/v1/users/me")
                            .header("Origin", "https://panel.pruebas.invalid")
                            .header("Access-Control-Request-Method", "GET"))
                    .andExpect(status().isOk());
            http.perform(options("/api/v1/users/me")
                            .header("Origin", "https://atacante.invalid")
                            .header("Access-Control-Request-Method", "GET"))
                    .andExpect(status().isForbidden())
                    .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
            como(usuario(), get("/api/v1/users/me").header("Origin", "https://atacante.invalid"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("las tareas programadas corren sin contexto de seguridad")
        void tareasProgramadas() {
            SecurityContextHolder.clearContext();
            assertDoesNotThrow(() -> colgadas.abandonStaleSessions());
            assertDoesNotThrow(() -> vencidos.expireOverdue());
            assertDoesNotThrow(() -> ciclo.runHourly());
        }
    }

    private static <T> void assertThat(T actual, org.hamcrest.Matcher<? super T> matcher) {
        org.hamcrest.MatcherAssert.assertThat(actual, matcher);
    }
}
