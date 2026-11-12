package main.web.services.fitsense.analytics;

import main.web.services.fitsense.adaptation.interfaces.acl.AdaptationContextFacade;
import main.web.services.fitsense.analytics.interfaces.acl.AnalyticsContextFacade;
import main.web.services.fitsense.analytics.interfaces.acl.WeeklyMetricsView;
import main.web.services.fitsense.execution.domain.model.commands.*;
import main.web.services.fitsense.execution.domain.services.WorkoutSessionCommandService;
import main.web.services.fitsense.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import main.web.services.fitsense.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.interfaces.acl.PlanningContextFacade;
import main.web.services.fitsense.planning.interfaces.rest.TrainingPlanController;
import main.web.services.fitsense.planning.interfaces.rest.resources.SkipWorkoutResource;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.BiologicalSex;
import main.web.services.fitsense.profiling.domain.model.valueobjects.FitnessLevel;
import main.web.services.fitsense.profiling.domain.model.valueobjects.GoalType;
import main.web.services.fitsense.profiling.domain.model.valueobjects.TrainingLocation;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import main.web.services.fitsense.shared.domain.exceptions.DomainRuleViolationException;
import main.web.services.fitsense.shared.domain.model.valueobjects.SkipReason;
import main.web.services.fitsense.shared.domain.model.valueobjects.TrainingWeek;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ADHERENCIA: ejecucion equivalente / planificacion, sin ponderaciones.
 * <pre>
 *   A_e = min(E_e / P_e, 1) x 100          por ejercicio
 *   A_s = sum(A_e / 100) / n x 100         por entrenamiento, n = ejercicios indicados
 *   A_w = sum(A_s / 100) / m x 100         por semana, m = entrenamientos previstos
 * </pre>
 * Cada esperado se calcula A MANO en el comentario de la prueba y se escribe
 * como literal: ninguna prueba reconstruye el resultado con codigo de
 * produccion.
 * <p>
 * PostgreSQL 16 efimero con V1-V33, motor de reglas, sin llamadas a Replicate.
 * Los entrenamientos generados se reducen por SQL a ejercicios con dosis
 * conocidas, para que las cuentas a mano sean exactas.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
class AdherenciaIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired PlanningContextFacade planning;
    @Autowired AnalyticsContextFacade analytics;
    @Autowired AdaptationContextFacade adaptation;
    @Autowired UserProfileCommandService perfiles;
    @Autowired WorkoutSessionCommandService sesiones;
    @Autowired TrainingPlanController planes;
    @Autowired UserRepository usuarios;
    @Autowired JdbcTemplate jdbc;
    @Autowired WebApplicationContext contexto;

    MockMvc http;

    static final AtomicInteger SECUENCIA = new AtomicInteger();
    static final LocalDate SEMANA = TrainingWeek.containing(LocalDate.now()).startDate().plusWeeks(1);
    static final List<String> EQUIPAMIENTO = List.of("dumbbell", "barbell", "cable", "leverage machine", "bench", "band");

    @BeforeEach
    void http() {
        http = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    record Participante(long userId, long planId, List<Long> dias) {}

    /** Dosis de un ejercicio: por repeticiones (series x reps) o por duracion (series x segundos). */
    record Dosis(String tipo, int series, int cantidad) {
        static Dosis reps(int series, int reps) { return new Dosis("SETS_REPS", series, reps); }
        static Dosis segundos(int series, int segundos) { return new Dosis("DURATION", series, segundos); }
    }

    private Participante participante() {
        int n = SECUENCIA.incrementAndGet();
        String email = "adherencia" + n + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Prueba', now(), now())", email);
        long userId = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(userId, LocalDate.of(1992, 5, 10), BiologicalSex.FEMALE,
                new BigDecimal("165"), new BigDecimal("68"), null, FitnessLevel.INTERMEDIATE, GoalType.GAIN_MUSCLE,
                "prueba de adherencia", TrainingLocation.GYM, (short) 3,
                List.of((short) 1, (short) 3, (short) 5), (short) 45, EQUIPAMIENTO,
                List.of(), "Ninguna", true, true, true, true, true));
        long plan = planning.generateWeeklyPlan(userId, SEMANA, PlanAdjustment.none(), null, null)
                .orElseThrow(() -> new AssertionError("no se genero la semana"));
        var dias = jdbc.queryForList("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ?"
                + " ORDER BY scheduled_date", Long.class, plan);
        assertEquals(3, dias.size(), "premisa: tres entrenamientos");
        return new Participante(userId, plan, dias);
    }

    /**
     * Deja el entrenamiento con exactamente esos ejercicios y esas dosis, y
     * devuelve sus planned_exercise_id en orden.
     */
    private List<Long> forma(long plannedWorkoutId, Dosis... dosis) {
        var ids = jdbc.queryForList("SELECT planned_exercise_id FROM planned_workout_exercises"
                + " WHERE planned_workout_id = ? ORDER BY exercise_order", Long.class, plannedWorkoutId);
        assertTrue(ids.size() >= dosis.length, "premisa: el entrenamiento tiene al menos " + dosis.length);
        for (int i = dosis.length; i < ids.size(); i++)
            jdbc.update("DELETE FROM planned_workout_exercises WHERE planned_exercise_id = ?", ids.get(i));
        for (int i = 0; i < dosis.length; i++) {
            var d = dosis[i];
            boolean reps = d.tipo().equals("SETS_REPS");
            jdbc.update("UPDATE planned_workout_exercises SET prescription_type = ?, planned_sets = ?,"
                            + " planned_reps = ?, planned_duration_seconds = ? WHERE planned_exercise_id = ?",
                    d.tipo(), (short) d.series(), reps ? (Short) (short) d.cantidad() : null,
                    reps ? null : d.cantidad(), ids.get(i));
        }
        return ids.subList(0, dosis.length);
    }

    private void duracion(long plannedWorkoutId, int minutos) {
        jdbc.update("UPDATE planned_workouts SET expected_duration_minutes = ? WHERE planned_workout_id = ?",
                (short) minutos, plannedWorkoutId);
    }

    private long iniciar(Participante p, long plannedWorkoutId) {
        return sesiones.handle(new StartWorkoutSessionCommand(p.userId(), plannedWorkoutId)).orElseThrow().getId();
    }

    private void reps(Participante p, long sesion, long pe, int repsTotales) {
        sesiones.handle(new RecordSessionExerciseCommand(p.userId(), sesion, pe, (short) 3, repsTotales, null,
                null, repsTotales == 0 ? SkipReason.FATIGUE : null));
    }

    private void segundos(Participante p, long sesion, long pe, int segundosTotales) {
        sesiones.handle(new RecordSessionExerciseCommand(p.userId(), sesion, pe, (short) 2, null, segundosTotales,
                null, null));
    }

    private void cerrar(Participante p, long sesion) {
        sesiones.handle(new FinishWorkoutSessionCommand(p.userId(), sesion, (short) 6, null, null));
    }

    private void omitir(Participante p, long plannedWorkoutId) {
        planes.skip(UserDetailsImpl.build(usuarios.findById(p.userId()).orElseThrow()), plannedWorkoutId,
                new SkipWorkoutResource(SkipReason.LACK_OF_TIME));
    }

    private BigDecimal cumplimientoSesion(long sesion) {
        return jdbc.queryForObject("SELECT completion_percentage FROM workout_sessions WHERE session_id = ?",
                BigDecimal.class, sesion);
    }

    private WeeklyMetricsView medir(Participante p) {
        return analytics.calculateWeek(p.userId(), SEMANA).orElseThrow();
    }

    private static void igual(String esperado, BigDecimal real) {
        assertNotNull(real, "esperado " + esperado + " y llego null");
        assertEquals(0, new BigDecimal(esperado).compareTo(real), "esperado " + esperado + " y llego " + real);
    }

    /**
     * La semana de referencia: 100 %, 50 % y 0 %.
     * <ul>
     *   <li>Dia 1: un ejercicio 3x10 = 30 reps, hechas 30 -> A_e = 100 -> A_s = 100.</li>
     *   <li>Dia 2: 3x10 hechas 30 (100) y 3x10 hechas 0 (0) -> A_s = (1 + 0) / 2 = 50.</li>
     *   <li>Dia 3: omitido con causa, sin sesion -> 0.</li>
     * </ul>
     * A_w = (1 + 0,5 + 0) / 3 x 100 = 50,00.
     */
    private Participante semanaDeReferencia(int min1, int min2, int min3) {
        var p = participante();
        var d1 = forma(p.dias().get(0), Dosis.reps(3, 10));
        var d2 = forma(p.dias().get(1), Dosis.reps(3, 10), Dosis.reps(3, 10));
        duracion(p.dias().get(0), min1);
        duracion(p.dias().get(1), min2);
        duracion(p.dias().get(2), min3);

        long s1 = iniciar(p, p.dias().get(0));
        reps(p, s1, d1.get(0), 30);
        cerrar(p, s1);
        long s2 = iniciar(p, p.dias().get(1));
        reps(p, s2, d2.get(0), 30);
        reps(p, s2, d2.get(1), 0);
        cerrar(p, s2);
        omitir(p, p.dias().get(2));

        igual("100.00", cumplimientoSesion(s1));
        igual("50.00", cumplimientoSesion(s2));
        return p;
    }

    // =====================================================================

    @Nested
    @DisplayName("1 · formula de la semana")
    class Semana {

        @Test
        @DisplayName("100 %, 50 % y 0 %: 50 %; la API, el riesgo y la adaptacion reciben el mismo 50")
        void referencia() throws Exception {
            var p = semanaDeReferencia(45, 45, 45);
            var m = medir(p);
            igual("50.00", m.weightedAdherencePct());
            // Observada: los tres tienen registro (dos sesiones y una omision declarada).
            igual("50.00", m.observedAdherencePct());
            igual("100.00", m.registrationCoveragePct());

            // API.
            var principal = UserDetailsImpl.build(usuarios.findById(p.userId()).orElseThrow());
            http.perform(get("/api/v1/users/me/metrics/weekly/" + SEMANA).with(user(principal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.weightedAdherencePct").value(50.0))
                    .andExpect(jsonPath("$.observedAdherencePct").value(50.0));

            // Riesgo, tabla activa: >= 80 -> 0, >= 60 -> 10, >= 40 -> 20, >= 0 -> 30.
            // 50 cae en el tramo de 40: 20 puntos por baja adherencia.
            assertTramos();
            var riesgo = jdbc.queryForMap("SELECT weighted_adherence_pct, risk_factors::text AS factores"
                    + " FROM weekly_user_metrics WHERE user_id = ? AND week_start_date = ?", p.userId(), SEMANA);
            igual("50.00", (BigDecimal) riesgo.get("weighted_adherence_pct"));
            assertTrue(String.valueOf(riesgo.get("factores")).contains("\"low_adherence\": 20"),
                    String.valueOf(riesgo.get("factores")));

            // Adaptacion: lee la semana por el mismo facade (primaria y observada)
            // y la intervencion se dispara con la misma cifra.
            var leida = analytics.fetchWeek(p.userId(), SEMANA).orElseThrow();
            igual("50.00", leida.weightedAdherencePct());
            igual("50.00", leida.observedAdherencePct());
            var orden = adaptation.decideForWeek(p.userId(), SEMANA).orElseThrow();
            assertNotNull(orden.outcome());
            igual("50.00", jdbc.queryForObject("SELECT trigger_adherence_pct FROM user_interventions"
                    + " WHERE weekly_metric_id = ?", BigDecimal.class, m.weeklyMetricId()));
        }

        @Test
        @DisplayName("la misma ejecucion con entrenamientos de 60, 45 y 30 minutos: sigue siendo 50 %")
        void sinPesoPorDuracion() {
            // Con el peso por duracion de antes: (60x100 + 45x50 + 30x0) / 135 = 61,11.
            var m = medir(semanaDeReferencia(60, 45, 30));
            igual("50.00", m.weightedAdherencePct());
            igual("50.00", m.observedAdherencePct());
        }

        @Test
        @DisplayName("por debajo del 30 %: el entrenamiento queda SKIPPED pero aporta su 25 %")
        void bajoElMinimo() {
            var p = participante();
            // Dia 1: cuatro ejercicios de 3x10, uno hecho -> A_s = (1 + 0 + 0 + 0) / 4 = 25.
            var d1 = forma(p.dias().get(0), Dosis.reps(3, 10), Dosis.reps(3, 10), Dosis.reps(3, 10),
                    Dosis.reps(3, 10));
            long s = iniciar(p, p.dias().get(0));
            reps(p, s, d1.get(0), 30);
            cerrar(p, s);
            igual("25.00", cumplimientoSesion(s));
            assertEquals("SKIPPED", jdbc.queryForObject("SELECT status FROM planned_workouts"
                    + " WHERE planned_workout_id = ?", String.class, p.dias().get(0)));
            // Dias 2 y 3 omitidos con causa: 0 y 0.
            omitir(p, p.dias().get(1));
            omitir(p, p.dias().get(2));
            // A_w = (0,25 + 0 + 0) / 3 x 100 = 8,33.
            igual("8.33", medir(p).weightedAdherencePct());
        }

        @Test
        @DisplayName("sin registro alguno: 0 % en la primaria; la observada no se puede calcular")
        void sinEjecucion() {
            var p = participante();
            var m = medir(p);
            // Tres previstos sin sesion ni omision: (0 + 0 + 0) / 3 = 0.
            igual("0.00", m.weightedAdherencePct());
            assertNull(m.observedAdherencePct(), "sin registros no hay observada: no es 0 ni 100");
        }

        @Test
        @DisplayName("semana sin plan: no calculable, nunca 100 %")
        void sinEntrenamientosEvaluables() {
            var p = participante();
            var m = analytics.calculateWeek(p.userId(), SEMANA.plusWeeks(4)).orElseThrow();
            assertFalse(m.hasActivePlan());
            assertNull(m.weightedAdherencePct());
            assertNull(m.observedAdherencePct());
        }
    }

    @Nested
    @DisplayName("2 · formula de la sesion y del ejercicio")
    class Sesion {

        @Test
        @DisplayName("ejercicios de 30 y 12 repeticiones pesan lo mismo: 50 %, no 71 %")
        void sinPesoPorRepeticiones() {
            var p = participante();
            // 3x10 = 30 hechas 30 -> 100; 2x6 = 12 hechas 0 -> 0. A_s = (1 + 0) / 2 = 50.
            // Ponderado por repeticiones seria 30 / 42 = 71,43.
            var d = forma(p.dias().get(0), Dosis.reps(3, 10), Dosis.reps(2, 6));
            long s = iniciar(p, p.dias().get(0));
            reps(p, s, d.get(0), 30);
            reps(p, s, d.get(1), 0);
            cerrar(p, s);
            igual("50.00", cumplimientoSesion(s));
        }

        @Test
        @DisplayName("por duracion se comparan segundos: 2x30 s hechos 45 s -> 75 %")
        void porDuracion() {
            var p = participante();
            // 2x30 s = 60 s hechos 45 -> 75; 3x10 = 30 hechas 15 -> 50. A_s = (0,75 + 0,5) / 2 = 62,5.
            var d = forma(p.dias().get(0), Dosis.segundos(2, 30), Dosis.reps(3, 10));
            long s = iniciar(p, p.dias().get(0));
            segundos(p, s, d.get(0), 45);
            reps(p, s, d.get(1), 15);
            cerrar(p, s);
            igual("75.00", jdbc.queryForObject("SELECT completion_percentage FROM workout_session_exercises"
                    + " WHERE session_id = ? AND planned_exercise_id = ?", BigDecimal.class, s, d.get(0)));
            igual("62.50", cumplimientoSesion(s));
        }

        @Test
        @DisplayName("hacer de mas tope 100: 3x10 hechas 45 aporta 100 y no compensa al que falto")
        void topeDe100() {
            var p = participante();
            // 45 / 30 = 150 % -> tope 100; el otro 0. A_s = (1 + 0) / 2 = 50, no 75.
            var d = forma(p.dias().get(0), Dosis.reps(3, 10), Dosis.reps(3, 10));
            long s = iniciar(p, p.dias().get(0));
            reps(p, s, d.get(0), 45);
            reps(p, s, d.get(1), 0);
            cerrar(p, s);
            igual("100.00", jdbc.queryForObject("SELECT completion_percentage FROM workout_session_exercises"
                    + " WHERE session_id = ? AND planned_exercise_id = ?", BigDecimal.class, s, d.get(0)));
            igual("50.00", cumplimientoSesion(s));
            // Dia 1: 50; dias 2 y 3 sin registro: 0. A_w = 0,5 / 3 = 16,67.
            igual("16.67", medir(p).weightedAdherencePct());
        }

        @Test
        @DisplayName("un ejercicio no registrado aporta 0: el denominador son los indicados, no los registrados")
        void noRegistradoCuentaCero() {
            var p = participante();
            // Tres indicados, solo uno registrado al 100 %: A_s = 1 / 3 = 33,33.
            var d = forma(p.dias().get(0), Dosis.reps(3, 10), Dosis.reps(3, 10), Dosis.reps(3, 10));
            long s = iniciar(p, p.dias().get(0));
            reps(p, s, d.get(0), 30);
            cerrar(p, s);
            igual("33.33", cumplimientoSesion(s));
        }
    }

    @Nested
    @DisplayName("3 · una sola vez por entrenamiento")
    class UnaVez {

        @Test
        @DisplayName("abandonado, reintentado y con reenvios: cuenta solo el ultimo intento cerrado")
        void abandonoYReintentos() {
            var p = participante();
            var d = forma(p.dias().get(0), Dosis.reps(3, 10), Dosis.reps(3, 10));
            // Intento 1: todo hecho y abandonado -> no cuenta.
            long a = iniciar(p, p.dias().get(0));
            reps(p, a, d.get(0), 30);
            reps(p, a, d.get(1), 30);
            sesiones.handle(new AbandonWorkoutSessionCommand(p.userId(), a));
            // Intento 2: el primero reenviado tres veces, el segundo a 0 -> 50.
            long s = iniciar(p, p.dias().get(0));
            reps(p, s, d.get(0), 10);
            reps(p, s, d.get(0), 20);
            reps(p, s, d.get(0), 30);
            reps(p, s, d.get(1), 0);
            cerrar(p, s);
            assertThrows(DomainRuleViolationException.class, () -> cerrar(p, s));
            // A_w = (0,5 + 0 + 0) / 3 = 16,67. Si contara el abandonado (100) seria 50; si sumara los dos, 50.
            igual("16.67", medir(p).weightedAdherencePct());
        }

        @Test
        @DisplayName("reporte retroactivo: cuenta en la semana de su entrenamiento y una sola vez")
        void reporteRetroactivo() {
            var p = participante();
            var d = forma(p.dias().get(0), Dosis.reps(3, 10), Dosis.reps(3, 10));
            // Reportado ayer, para un entrenamiento de la semana que viene: la
            // fecha del reporte no lo saca de su semana. (1 + 0,5) / 2 = 75.
            var reporte = new ReportWorkoutCommand(p.userId(), p.dias().get(0), OffsetDateTime.now().minusDays(1),
                    (short) 6, null, null, List.of(
                    new ReportWorkoutCommand.ReportedExercise(d.get(0), (short) 3, 30, null, null, null),
                    new ReportWorkoutCommand.ReportedExercise(d.get(1), (short) 3, 15, null, null, null)));
            sesiones.handle(reporte);
            assertThrows(DomainRuleViolationException.class, () -> sesiones.handle(reporte));
            // A_w = (0,75 + 0 + 0) / 3 = 25.
            igual("25.00", medir(p).weightedAdherencePct());
        }

        @Test
        @DisplayName("versiones reemplazadas: un previsto por dia; el dia hecho en la version 1 no se duplica")
        void versionesReemplazadas() {
            var p = participante();
            var d1 = forma(p.dias().get(0), Dosis.reps(3, 10));
            forma(p.dias().get(1), Dosis.reps(3, 10));
            forma(p.dias().get(2), Dosis.reps(3, 10), Dosis.reps(3, 10));

            // Version 1: dia 1 hecho al 100 %, dia 2 omitido con causa.
            long s1 = iniciar(p, p.dias().get(0));
            reps(p, s1, d1.get(0), 30);
            cerrar(p, s1);
            omitir(p, p.dias().get(1));

            // Version 2 de la misma semana con los tres dias, como la publicaria la
            // adaptacion: lo cerrado de la version 1 sobrevive y lo abierto pasa a REPLACED.
            long v2 = nuevaVersion(p);
            var dias2 = jdbc.queryForList("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ?"
                    + " ORDER BY scheduled_date", Long.class, v2);
            var e3 = jdbc.queryForList("SELECT planned_exercise_id FROM planned_workout_exercises"
                    + " WHERE planned_workout_id = ? ORDER BY exercise_order", Long.class, dias2.get(2));
            long s3 = iniciar(p, dias2.get(2));
            reps(p, s3, e3.get(0), 30);
            reps(p, s3, e3.get(1), 0);
            cerrar(p, s3);

            // Dias: 1 -> el de la version 1 (100, tiene sesion); 2 -> el de la
            // version 1 (omitido, 0); 3 -> el de la version 2 (50). El 3 de la
            // version 1 es REPLACED. A_w = (1 + 0 + 0,5) / 3 = 50. Sumando los
            // cinco previstos no REPLACED seria (1 + 0 + 0 + 0 + 0,5) / 5 = 30.
            var m = medir(p);
            igual("50.00", m.weightedAdherencePct());
            assertEquals(3, jdbc.queryForObject("SELECT scheduled_workouts FROM weekly_user_metrics"
                    + " WHERE weekly_metric_id = ?", Integer.class, m.weeklyMetricId()));
        }
    }

    @Nested
    @DisplayName("4 · ejecucion pendiente")
    class Pendiente {

        @Test
        @DisplayName("con una sesion abierta no se puede omitir; abandonada, si; nunca SKIPPED con sesion en curso")
        void omitirConSesionAbierta() {
            var p = participante();
            long pw = p.dias().get(0);
            long s = iniciar(p, pw);
            var error = assertThrows(DomainRuleViolationException.class, () -> omitir(p, pw));
            assertTrue(error.getMessage().contains("sesion abierta"), error.getMessage());
            assertEquals("IN_PROGRESS", jdbc.queryForObject("SELECT status FROM planned_workouts"
                    + " WHERE planned_workout_id = ?", String.class, pw));

            sesiones.handle(new AbandonWorkoutSessionCommand(p.userId(), s));
            omitir(p, pw);
            assertEquals("SKIPPED", jdbc.queryForObject("SELECT status FROM planned_workouts"
                    + " WHERE planned_workout_id = ?", String.class, pw));

            // Ningun previsto SKIPPED convive con una sesion en curso.
            assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM planned_workouts pw JOIN workout_sessions ws"
                    + " ON ws.planned_workout_id = pw.planned_workout_id WHERE pw.status = 'SKIPPED'"
                    + " AND ws.status = 'IN_PROGRESS'", Integer.class));
        }
    }

    // ------------------------------------------------------------------ utilidades

    /** Los tramos de riesgo por adherencia que las cuentas a mano suponen. */
    private void assertTramos() {
        var tramos = jdbc.queryForObject("SELECT params->'risk'->'adherence_points' FROM calculation_configs"
                + " WHERE is_active", String.class).replace(" ", "");
        assertTrue(tramos.contains("{\"points\":20,\"min_pct\":40.0}") || tramos.contains("{\"min_pct\":40.0,\"points\":20}"),
                tramos);
        assertTrue(tramos.contains("\"min_pct\":60.0"), tramos);
    }

    /**
     * Una version 2 de la semana copiando la 1 fila a fila: la 1 pasa a REPLACED
     * y sus entrenamientos sin cerrar tambien, como hace markReplaced.
     */
    private long nuevaVersion(Participante p) {
        jdbc.update("UPDATE planned_workouts SET status = 'REPLACED' WHERE plan_id = ?"
                + " AND status NOT IN ('COMPLETED','PARTIAL','SKIPPED')", p.planId());
        jdbc.update("UPDATE weekly_training_plans SET status = 'REPLACED' WHERE plan_id = ?", p.planId());

        var columnasPlan = columnas("weekly_training_plans", "plan_id", "plan_version", "parent_plan_id", "status");
        long v2 = jdbc.queryForObject("INSERT INTO weekly_training_plans (" + columnasPlan
                + ", plan_version, parent_plan_id, status) SELECT " + columnasPlan
                + ", plan_version + 1, plan_id, 'ACTIVE' FROM weekly_training_plans WHERE plan_id = ?"
                + " RETURNING plan_id", Long.class, p.planId());

        var columnasW = columnas("planned_workouts", "planned_workout_id", "plan_id", "status", "skip_reason",
                "source_planned_workout_id");
        var columnasE = columnas("planned_workout_exercises", "planned_exercise_id", "planned_workout_id");
        for (long viejo : p.dias()) {
            long nuevo = jdbc.queryForObject("INSERT INTO planned_workouts (" + columnasW + ", plan_id, status)"
                    + " SELECT " + columnasW + ", ?, 'SCHEDULED' FROM planned_workouts WHERE planned_workout_id = ?"
                    + " RETURNING planned_workout_id", Long.class, v2, viejo);
            jdbc.update("INSERT INTO planned_workout_exercises (" + columnasE + ", planned_workout_id) SELECT "
                    + columnasE + ", ? FROM planned_workout_exercises WHERE planned_workout_id = ?", nuevo, viejo);
        }
        return v2;
    }

    /** Columnas de la tabla menos las indicadas, en orden, para copiar filas. */
    private String columnas(String tabla, String... fuera) {
        var excluidas = Set.of(fuera);
        return jdbc.queryForList("SELECT column_name FROM information_schema.columns WHERE table_name = ?"
                        + " AND is_generated = 'NEVER' AND identity_generation IS NULL ORDER BY ordinal_position",
                        String.class, tabla).stream()
                .filter(c -> !excluidas.contains(c))
                .collect(Collectors.joining(", "));
    }
}
