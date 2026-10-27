package main.web.services.fitsense.orchestation;

import main.web.services.fitsense.execution.domain.model.commands.FinishWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.model.commands.RecordSessionExerciseCommand;
import main.web.services.fitsense.execution.domain.model.commands.StartWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.services.WorkoutSessionCommandService;
import main.web.services.fitsense.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import main.web.services.fitsense.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import main.web.services.fitsense.orchestration.application.internal.WeeklyCycleService;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.domain.services.SessionDurationEstimator;
import main.web.services.fitsense.planning.infrastructure.generation.ai.ReplicateSessionFixGenerator;
import main.web.services.fitsense.planning.interfaces.acl.PlanningContextFacade;
import main.web.services.fitsense.planning.interfaces.rest.TrainingPlanController;
import main.web.services.fitsense.planning.interfaces.rest.resources.SkipWorkoutResource;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.BiologicalSex;
import main.web.services.fitsense.profiling.domain.model.valueobjects.FitnessLevel;
import main.web.services.fitsense.profiling.domain.model.valueobjects.GoalType;
import main.web.services.fitsense.profiling.domain.model.valueobjects.TrainingLocation;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import main.web.services.fitsense.shared.domain.model.valueobjects.SkipReason;
import main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion;
import main.web.services.fitsense.shared.domain.model.valueobjects.TrainingWeek;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * COPIA SEMANAL Y MANTENIMIENTO SIN IA: cuenta las llamadas a la IA de
 * correccion ({@code ReplicateSessionFixGenerator.pedirRespuesta}, la unica que
 * hace el ciclo semanal) y falla si hay alguna donde esta prohibida.
 * <p>
 * Ciclo real ({@link WeeklyCycleService#runFor}) sobre PostgreSQL efimero, con
 * la progresion encendida SOLO aqui. La IA esta simulada: nunca se llama a
 * Replicate. La generacion inicial de cada participante no cuenta (es la
 * generacion inicial, con su propio camino).
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.adaptation.progression.enabled=true"
})
@DisplayName("Pendientes de la progresion: recuperacion de series, carga y ciclos simultaneos")
class PendientesProgresionIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @MockitoBean ReplicateSessionFixGenerator ia;

    @Autowired WeeklyCycleService ciclo;
    @Autowired PlanningContextFacade planning;
    @Autowired UserProfileCommandService perfiles;
    @Autowired WorkoutSessionCommandService sesiones;
    @Autowired TrainingPlanController planes;
    @Autowired UserRepository usuarios;
    @Autowired SessionDurationEstimator estimador;
    @Autowired JdbcTemplate jdbc;

    static final AtomicInteger SECUENCIA = new AtomicInteger();
    static final LocalDate S1 = TrainingWeek.containing(LocalDate.now()).startDate().plusWeeks(1);
    static final List<String> GIMNASIO = List.of("body weight", "dumbbell", "barbell", "cable", "leverage machine",
            "bench", "band", "stationary bike", "treadmill", "kettlebell", "ez barbell", "smith machine");

    enum Dia { HECHO, FATIGA, TIEMPO, NADA, PARCIAL_FATIGA }

    record Ej(long id, String tipo, Short series, Short reps, Integer segundos, Short descanso) {}

    @BeforeEach
    void iaSimulada() {
        reset(ia);
        when(ia.isEnabled()).thenReturn(true);
        when(ia.modelName()).thenReturn("simulado");
        when(ia.pedirRespuesta(any(), any(), any(), any(), anyInt()))
                .thenReturn(new ReplicateSessionFixGenerator.RawFix("{}", "salida-simulada"));
        when(ia.interpretar(anyString(), any())).thenAnswer(inv -> corregir(inv.getArgument(1)));
    }

    // =======================================================================
    // Escenarios
    // =======================================================================

    /** Que hace la IA simulada en una sesion senalada por fatiga. */
    enum Modo { SERIE, SOLO_CARGA, CARGA_SUBE }

    private Modo modo = Modo.SERIE;

    @Test
    @DisplayName("opcion 2: devolver una serie supera el 10 %; la dosis reducida se conserva y progresan otros ejercicios")
    void serieNoRecuperableYProgresionDeOtros() throws Exception {
        modo = Modo.SERIE;
        long u = participante(GoalType.LOSE_WEIGHT, FitnessLevel.INTERMEDIATE, 45);
        long plan = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        ejecutar(u, plan, Dia.HECHO, Dia.HECHO, Dia.FATIGA);
        var antes = semana(plan).get(2).getValue();
        plan = ciclo(u, 0, 1);
        assertEquals("APLICADA", ultimo(u).get("status"), String.valueOf(ultimo(u)));
        var reducido = semana(plan).get(2).getValue();
        var bajado = distinto(antes, reducido);
        var original = antes.stream().filter(e -> e.id() == bajado.id()).findFirst().orElseThrow();
        assertEquals(original.series() - 1, (int) bajado.series(), "premisa: la IA quito una serie");
        assertEquals(original.reps(), bajado.reps());

        boolean otroProgreso = false;
        var traza = new ArrayList<String>();
        for (int k = 1; k <= 8 && !otroProgreso; k++) {
            ejecutar(u, plan, Dia.HECHO, Dia.HECHO, Dia.HECHO);
            var viernesAntes = semana(plan).get(2);
            plan = ciclo(u, k, 0);
            var ev = ultimo(u);
            assertFalse(informe(ev).contains("\"tipo\":\"RECUPERACION\""),
                    "devolver la serie supera el 10 %: no hay recuperacion. " + informe(ev));
            var viernes = semana(plan).get(2);
            var ejReducido = viernes.getValue().stream().filter(e -> e.id() == bajado.id()).findFirst().orElseThrow();
            assertEquals(bajado.series(), ejReducido.series(), "la serie reducida se conserva (opcion 2)");
            if (String.valueOf(ev.get("progressed_sessions")).equals("{" + viernesAntes.getKey() + "}")) {
                var cambiado = distinto(viernesAntes.getValue(), viernes.getValue());
                assertNotEquals(bajado.id(), cambiado.id(), "progresa OTRO ejercicio");
                otroProgreso = true;
            }
            traza.add("s" + (k + 2) + " " + ev.get("progressed_sessions"));
        }
        System.out.println("PENDIENTE opcion2 reducido=" + bajado + " " + traza);
        assertTrue(otroProgreso, "el viernes tenia que progresar en otro ejercicio: " + traza);
        assertSinViolaciones();
    }

    @Test
    @DisplayName("carga: una adaptacion que SOLO baja la carga sugerida -> que se publica y como se registra")
    void adaptacionSoloDeCarga() throws Exception {
        modo = Modo.SOLO_CARGA;
        long u = participante(GoalType.GENERAL_FITNESS, FitnessLevel.INTERMEDIATE, 45);
        long plan = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        // V11: solo hay carga sugerida si se anoto peso. El viernes: el primer ejercicio por
        // repeticiones prescrito con 20 kg y hecho con 20 kg; el resto, dejado por fatiga.
        long viernes = semana(plan).get(2).getKey();
        assertEquals(1, jdbc.update("UPDATE planned_workout_exercises SET target_load_kg = 20.00 WHERE planned_exercise_id ="
                + " (SELECT planned_exercise_id FROM planned_workout_exercises WHERE planned_workout_id = ?"
                + " AND prescription_type = 'SETS_REPS' ORDER BY exercise_order LIMIT 1)", viernes));
        ejecutar(u, plan, Dia.HECHO, Dia.HECHO, Dia.PARCIAL_FATIGA);
        System.out.println("PENDIENTE solo_carga premisa: " + ultimoEstadoSesion(viernes));
        long w2 = ciclo(u, 0, -1);
        var ev = ultimo(u);
        var cargas = jdbc.queryForList("SELECT DISTINCT target_load_kg::text FROM planned_workout_exercises"
                + " WHERE planned_workout_id = ?", String.class, semana(w2).get(2).getKey());
        System.out.println("PENDIENTE solo_carga status=" + ev.get("status") + " source=" + ev.get("generation_source")
                + " ai_calls=" + ev.get("ai_calls") + " cargas publicadas=" + cargas + " informe=" + informe(ev));
        // 2(a): la reduccion autorizada de carga es una adaptacion aplicada, con evidencia coherente (V33).
        assertEquals("APLICADA", ev.get("status"), String.valueOf(ev));
        assertEquals("AI", ev.get("generation_source"));
        assertEquals(1, ((Number) ev.get("ai_calls")).intValue());
        assertEquals("{" + viernes + "}", jdbc.queryForObject("SELECT applied_sessions::text FROM plan_adaptation_attempts"
                + " WHERE user_id = ? ORDER BY attempt_id DESC LIMIT 1", String.class, u));
        assertTrue(cargas.contains("16.00"), "se publica la carga reducida: " + cargas);
        assertSinViolaciones();
    }

    private String ultimoEstadoSesion(long plannedWorkoutId) {
        return String.valueOf(jdbc.queryForList("SELECT status, completion_percentage FROM workout_sessions"
                + " WHERE planned_workout_id = ?", plannedWorkoutId));
    }

    @Test
    @DisplayName("carga: si la IA la SUBE (20 -> 24 kg) se rechaza y no se publica la subida")
    void subidaDeCargaRechazada() throws Exception {
        modo = Modo.CARGA_SUBE;
        long u = participante(GoalType.GENERAL_FITNESS, FitnessLevel.INTERMEDIATE, 45);
        long plan = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        long viernes = semana(plan).get(2).getKey();
        jdbc.update("UPDATE planned_workout_exercises SET target_load_kg = 20.00 WHERE planned_exercise_id ="
                + " (SELECT planned_exercise_id FROM planned_workout_exercises WHERE planned_workout_id = ?"
                + " AND prescription_type = 'SETS_REPS' ORDER BY exercise_order LIMIT 1)", viernes);
        ejecutar(u, plan, Dia.HECHO, Dia.HECHO, Dia.PARCIAL_FATIGA);
        var w2 = ciclo.runFor(u, S1, S1.plusWeeks(1));
        var ev = ultimo(u);
        System.out.println("PENDIENTE carga_sube status=" + ev.get("status") + " ai_calls=" + ev.get("ai_calls"));
        assertNotEquals("APLICADA", ev.get("status"), String.valueOf(ev));
        w2.ifPresent(pl -> assertFalse(jdbc.queryForList("SELECT target_load_kg::text FROM planned_workout_exercises"
                + " WHERE planned_workout_id = ?", String.class, semana(pl).get(2).getKey()).contains("24.00"),
                "se publico la subida de carga"));
        assertSinViolaciones();
    }

    @org.junit.jupiter.params.ParameterizedTest(name = "{0}")
    @org.junit.jupiter.params.provider.ValueSource(strings = {"GYM", "CASA"})
    @DisplayName("1(a): LOSE_WEIGHT / BEGINNER / 30 no puede dar ningun paso tras acortar -> se mantiene, no reincorpora")
    void sinProgresionNoReincorpora(String lugar) throws Exception {
        long u = participante(GoalType.LOSE_WEIGHT, FitnessLevel.BEGINNER, 30, lugar);
        long plan = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        ejecutar(u, plan, Dia.HECHO, Dia.HECHO, Dia.TIEMPO);
        plan = ciclo(u, 0, -1);
        assertEquals("APLICADA", ultimo(u).get("status"), String.valueOf(ultimo(u)));
        var acortado = semana(plan).get(2).getValue();
        var traza = new ArrayList<String>();
        for (int k = 1; k <= 8; k++) {
            ejecutar(u, plan, Dia.HECHO, Dia.HECHO, Dia.HECHO);
            plan = ciclo(u, k, 0);
            var ev = ultimo(u);
            assertEquals("SIN_ORDEN", ev.get("status"));
            assertFalse(informe(ev).contains("\"tipo\":\"REINCORPORACION\""),
                    "sin progresion previa no se reincorpora: " + traza);
            traza.add("s" + (k + 2) + " " + ev.get("progressed_sessions"));
        }
        System.out.println("PENDIENTE sin_progresion " + lugar + " " + traza);
        assertEquals(acortado, semana(plan).get(2).getValue(), "el viernes acortado se mantiene igual");
        assertSinViolaciones();
    }

    @Test
    @DisplayName("dos ciclos SIMULTANEOS de la misma semana: una sola semana vigente (la evidencia puede duplicarse: hallazgo abierto)")
    void ciclosSimultaneos() throws Exception {
        long u = participante(GoalType.GENERAL_FITNESS, FitnessLevel.INTERMEDIATE, 45);
        long plan = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        ejecutar(u, plan, Dia.HECHO, Dia.HECHO, Dia.HECHO);
        var listos = new java.util.concurrent.CountDownLatch(1);
        var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        var resultados = new ArrayList<java.util.concurrent.Future<Optional<Long>>>();
        for (int i = 0; i < 2; i++)
            resultados.add(pool.submit(() -> {
                listos.await();
                return ciclo.runFor(u, S1, S1.plusWeeks(1));
            }));
        listos.countDown();
        var planes = new ArrayList<String>();
        for (var r : resultados) {
            try {
                planes.add(String.valueOf(r.get(120, java.util.concurrent.TimeUnit.SECONDS)));
            } catch (java.util.concurrent.ExecutionException e) {
                planes.add("EXCEPCION " + e.getCause());
            }
        }
        pool.shutdown();
        int vigentes = jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ?"
                + " AND week_start_date = ? AND status = 'ACTIVE'", Integer.class, u, S1.plusWeeks(1));
        var intentos = jdbc.queryForList("SELECT status FROM plan_adaptation_attempts WHERE user_id = ? ORDER BY attempt_id",
                String.class, u);
        int intervenciones = jdbc.queryForObject("SELECT count(*) FROM user_interventions WHERE user_id = ?",
                Integer.class, u);
        System.out.println("PENDIENTE simultaneos resultados=" + planes + " planes vigentes=" + vigentes
                + " intentos V32=" + intentos + " intervenciones=" + intervenciones);
        assertEquals(1, vigentes, "una sola semana vigente: " + planes);
        // HALLAZGO ABIERTO (27/10/2026), no corregido: segun quien gane la carrera, las dos
        // ejecuciones pueden publicar (una version queda REPLACED) y dejar DOS intentos V32 para
        // la misma decision. Lo que se garantiza hoy es una sola semana vigente; la evidencia
        // duplicada se registra aqui para el informe (COPIA_SEMANAL_SIN_IA.md §6.3).
        long publicadas = intentos.stream().filter(x -> !x.equals("NO_FACTIBLE") && !x.equals("RECHAZADA")).count();
        System.out.println("PENDIENTE simultaneos evidencias_publicadas=" + publicadas);
        assertTrue(publicadas >= 1 && publicadas <= 2, String.valueOf(intentos));
        assertSinViolaciones();
    }

    /** El unico ejercicio que cambio entre dos versiones de una sesion. */
    private static Ej distinto(List<Ej> a, List<Ej> b) {
        var d = new ArrayList<Ej>();
        for (int i = 0; i < Math.min(a.size(), b.size()); i++) if (!a.get(i).equals(b.get(i))) d.add(b.get(i));
        assertEquals(1, d.size(), "un solo ejercicio cambiado: " + d);
        return d.get(0);
    }

    // =======================================================================
    // Contador
    // =======================================================================

    /** Un ciclo de la semana k (S1 + k), comprobando el numero exacto de llamadas a la IA. */
    private long ciclo(long u, int k, int llamadasEsperadas) {
        clearInvocations(ia);
        var plan = ciclo.runFor(u, S1.plusWeeks(k), S1.plusWeeks(k + 1));
        int llamadas = (int) Mockito.mockingDetails(ia).getInvocations().stream()
                .filter(i -> i.getMethod().getName().equals("pedirRespuesta")).count();
        if (llamadasEsperadas >= 0)
            assertEquals(llamadasEsperadas, llamadas, "llamadas a la IA en la semana " + (k + 2) + ": " + ultimo(u));
        assertTrue(plan.isPresent(), "sin semana publicada en la " + (k + 2) + ": " + ultimo(u));
        return plan.get();
    }

    // =======================================================================
    // IA simulada: solo lo que permite su intervencion
    // =======================================================================

    private List<CorrectedSession> corregir(PlanGenerationContext ctx) {
        var salida = new ArrayList<CorrectedSession>();
        for (var senalada : ctx.adjustment().targetedSessions()) {
            long id = senalada.plannedWorkoutId();
            var f = jdbc.queryForMap("SELECT scheduled_date, focus_code FROM planned_workouts WHERE planned_workout_id = ?", id);
            var actuales = jdbc.query("SELECT pwe.exercise_id, pwe.prescription_type, pwe.planned_sets, pwe.planned_reps,"
                            + " pwe.planned_duration_seconds, pwe.rest_seconds, e.movement_pattern FROM planned_workout_exercises pwe"
                            + " JOIN exercises e ON e.exercise_id = pwe.exercise_id WHERE pwe.planned_workout_id = ?"
                            + " ORDER BY pwe.exercise_order",
                    (rs, k) -> Map.entry(new PlanDraft.DraftExercise(rs.getLong(1), PrescriptionType.valueOf(rs.getString(2)),
                            rs.getObject(3, Short.class), rs.getObject(4, Short.class), rs.getObject(5, Integer.class), null,
                            rs.getObject(6, Short.class), null), String.valueOf(rs.getString(7))), id);
            int quitar = -1;
            if (senalada.intervencion() == TipoIntervencion.ACORTAR_O_REORGANIZAR)
                for (int k = actuales.size() - 1; k >= 0 && quitar < 0; k--)
                    if (MandatorySlot.ofMovementPattern(actuales.get(k).getValue()).isEmpty()) quitar = k;
            var lista = new ArrayList<PlanDraft.DraftExercise>();
            boolean hecho = false;
            for (int k = 0; k < actuales.size(); k++) {
                if (k == quitar) continue;
                var e = actuales.get(k).getKey();
                Short series = e.plannedSets();
                java.math.BigDecimal carga = cargaDe(id, e.exerciseId());
                if (senalada.intervencion() == TipoIntervencion.REVISAR_CARGA_Y_RECUPERACION) {
                    if (modo == Modo.SOLO_CARGA && carga != null)
                        carga = carga.multiply(new java.math.BigDecimal("0.80")).setScale(2, java.math.RoundingMode.HALF_UP);
                    else if (modo == Modo.CARGA_SUBE && carga != null)
                        carga = carga.multiply(new java.math.BigDecimal("1.20")).setScale(2, java.math.RoundingMode.HALF_UP);
                    else if (modo == Modo.SERIE && !hecho && e.prescriptionType() == PrescriptionType.SETS_REPS
                            && series != null && series > 1) {
                        series = (short) (series - 1);
                        hecho = true;
                    }
                }
                lista.add(new PlanDraft.DraftExercise(e.exerciseId(), e.prescriptionType(), series, e.plannedReps(),
                        e.plannedDurationSeconds(), carga, e.restSeconds(), null));
            }
            var fecha = ((java.sql.Date) f.get("scheduled_date")).toLocalDate().plusWeeks(1);
            int min = estimador.estimateMinutes(new PlanDraft.DraftWorkout(fecha,
                    WorkoutFocus.valueOf((String) f.get("focus_code")), "x", 0, lista), ctx.prescription());
            salida.add(new CorrectedSession(id, fecha, min, lista, "ajuste simulado"));
        }
        return salida;
    }

    private java.math.BigDecimal cargaDe(long sesion, long exerciseId) {
        return jdbc.queryForList("SELECT target_load_kg FROM planned_workout_exercises WHERE planned_workout_id = ?"
                + " AND exercise_id = ?", java.math.BigDecimal.class, sesion, exerciseId).stream().filter(Objects::nonNull).findFirst().orElse(null);
    }

    private static int minimo(PlanGenerationContext ctx, long exerciseId) {
        var limites = ctx.prescription() == null ? null : ctx.prescription().repLimits();
        var c = ctx.availableExercises().stream().filter(x -> x.exerciseId() == exerciseId).findFirst();
        return limites == null || c.isEmpty() ? 0 : limites.minRepsFor(c.get().bodyPartCode(), ctx.profile().fitnessLevel());
    }

    // =======================================================================
    // Material
    // =======================================================================

    private long participante(GoalType objetivo, FitnessLevel nivel, int minutos) {
        return participante(objetivo, nivel, minutos, "GYM");
    }

    private long participante(GoalType objetivo, FitnessLevel nivel, int minutos, String lugar) {
        String email = "pendiente" + SECUENCIA.incrementAndGet() + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Prueba', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1992, 1, 15), BiologicalSex.FEMALE,
                new BigDecimal("165"), new BigDecimal("70"), null, nivel, objetivo, "sin ia",
                "GYM".equals(lugar) ? TrainingLocation.GYM : TrainingLocation.HOME,
                (short) 3, List.of((short) 1, (short) 3, (short) 5), (short) minutos,
                "GYM".equals(lugar) ? GIMNASIO : List.of("body weight", "dumbbell", "band"),
                List.of(), "Ninguna", true, true, true, true, true));
        return u;
    }

    /** (planned_workout_id, ejercicios con su dosis) por fecha. */
    private List<Map.Entry<Long, List<Ej>>> semana(long planId) {
        return jdbc.queryForList("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ? ORDER BY scheduled_date",
                Long.class, planId).stream().map(id -> Map.entry(id, jdbc.query("SELECT exercise_id, prescription_type,"
                        + " planned_sets, planned_reps, planned_duration_seconds, rest_seconds FROM planned_workout_exercises"
                        + " WHERE planned_workout_id = ? ORDER BY exercise_order",
                (rs, k) -> new Ej(rs.getLong(1), rs.getString(2), rs.getObject(3, Short.class), rs.getObject(4, Short.class),
                        rs.getObject(5, Integer.class), rs.getObject(6, Short.class)), id))).toList();
    }

    private List<String> estructura(long planId, int dias) {
        return jdbc.queryForList("SELECT (scheduled_date + ?) || '|' || focus_code || '|' || expected_duration_minutes"
                + " FROM planned_workouts WHERE plan_id = ? ORDER BY scheduled_date", String.class, dias, planId);
    }

    private static int volumen(List<Ej> l) {
        return l.stream().mapToInt(e -> {
            int s = e.series() == null ? 1 : e.series();
            return "DURATION".equals(e.tipo()) ? Math.round((float) (s * (e.segundos() == null ? 0 : e.segundos())) / 30)
                    : s * (e.reps() == null ? 0 : e.reps());
        }).sum();
    }

    private Long origen(long id) {
        return jdbc.queryForObject("SELECT source_planned_workout_id FROM planned_workouts WHERE planned_workout_id = ?",
                Long.class, id);
    }

    private List<Map<String, Object>> historial(long planId) {
        return jdbc.queryForList("SELECT ws.session_id, ws.status, ws.completion_percentage, ws.counts_toward_adherence,"
                + " pw.status AS pw_status FROM planned_workouts pw LEFT JOIN workout_sessions ws"
                + " ON ws.planned_workout_id = pw.planned_workout_id WHERE pw.plan_id = ? ORDER BY pw.scheduled_date", planId);
    }

    private int intentos(long u) {
        return jdbc.queryForObject("SELECT count(*) FROM plan_adaptation_attempts WHERE user_id = ?", Integer.class, u);
    }

    private Map<String, Object> ultimo(long u) {
        return jdbc.queryForMap("SELECT decision, status, generation_source, ai_calls, targeted_interventions::text,"
                + " progressed_sessions::text, conservation_report::text FROM plan_adaptation_attempts WHERE user_id = ?"
                + " ORDER BY attempt_id DESC LIMIT 1", u);
    }

    private static String informe(Map<String, Object> ev) {
        return String.valueOf(ev.get("conservation_report")).replace(" ", "");
    }

    private void ejecutar(long u, long planId, Dia... dias) {
        var ids = jdbc.queryForList("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ? ORDER BY scheduled_date",
                Long.class, planId);
        for (int i = 0; i < ids.size(); i++) {
            long w = ids.get(i);
            switch (dias[i]) {
                case HECHO -> {
                    long s = sesiones.handle(new StartWorkoutSessionCommand(u, w)).orElseThrow().getId();
                    for (var f : jdbc.queryForList("SELECT planned_exercise_id, prescription_type, planned_sets,"
                            + " planned_reps, planned_duration_seconds FROM planned_workout_exercises"
                            + " WHERE planned_workout_id = ?", w)) {
                        boolean duracion = "DURATION".equals(f.get("prescription_type"));
                        int series = f.get("planned_sets") == null ? 1 : ((Number) f.get("planned_sets")).intValue();
                        int total = series * ((Number) f.get(duracion ? "planned_duration_seconds" : "planned_reps")).intValue();
                        sesiones.handle(new RecordSessionExerciseCommand(u, s, ((Number) f.get("planned_exercise_id")).longValue(),
                                (short) series, duracion ? null : total, duracion ? total : null, null, null));
                    }
                    sesiones.handle(new FinishWorkoutSessionCommand(u, s, (short) 6, (short) 4, (short) 45));
                }
                case FATIGA -> planes.skip(UserDetailsImpl.build(usuarios.findById(u).orElseThrow()), w,
                        new SkipWorkoutResource(SkipReason.FATIGUE));
                case TIEMPO -> planes.skip(UserDetailsImpl.build(usuarios.findById(u).orElseThrow()), w,
                        new SkipWorkoutResource(SkipReason.LACK_OF_TIME));
                case NADA -> { }
                case PARCIAL_FATIGA -> {
                    long s = sesiones.handle(new StartWorkoutSessionCommand(u, w)).orElseThrow().getId();
                    boolean primero = true;
                    for (var f : jdbc.queryForList("SELECT planned_exercise_id, prescription_type, planned_sets,"
                            + " planned_reps, planned_duration_seconds, target_load_kg FROM planned_workout_exercises"
                            + " WHERE planned_workout_id = ? ORDER BY exercise_order", w)) {
                        long pe = ((Number) f.get("planned_exercise_id")).longValue();
                        if (primero && f.get("target_load_kg") != null) {
                            int series = ((Number) f.get("planned_sets")).intValue();
                            sesiones.handle(new RecordSessionExerciseCommand(u, s, pe, (short) series,
                                    series * ((Number) f.get("planned_reps")).intValue(), null,
                                    new java.math.BigDecimal("20.00"), null));
                            primero = false;
                        } else {
                            sesiones.handle(new RecordSessionExerciseCommand(u, s, pe, (short) 0, null, null, null,
                                    SkipReason.FATIGUE));
                        }
                    }
                    sesiones.handle(new FinishWorkoutSessionCommand(u, s, (short) 8, (short) 4, (short) 20));
                }
            }
        }
    }

    private void assertSinViolaciones() throws Exception {
        var texto = Files.readString(Path.of("docs/FitSense/consultas/linaje_v31_v32.sql")).lines()
                .filter(l -> !l.stripLeading().startsWith("--")).collect(Collectors.joining("\n"));
        assertEquals(List.of(), jdbc.queryForList(texto.split(";")[0].strip()));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM plan_adaptation_attempts WHERE"
                + " cardinality(progressed_sessions) > 1 OR (cardinality(progressed_sessions) = 1 AND"
                + " (status <> 'SIN_ORDEN' OR resulting_plan_id IS NULL))", Integer.class));
    }
}
