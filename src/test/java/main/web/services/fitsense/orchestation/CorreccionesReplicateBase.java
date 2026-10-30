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
import main.web.services.fitsense.planning.infrastructure.generation.ai.AiRetryFeedback;
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
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * LAS CINCO CORRECCIONES TRAS LAS CAMPANAS CON REPLICATE REAL (27/10/2026), por el
 * ciclo semanal completo: base PostgreSQL efimera, publicacion real y evidencia
 * V31-V34. La IA simulada reproduce la propuesta defectuosa que hizo la real
 * (evidencia/replicate_real/adaptacion) en los TRES intentos.
 * <p>
 * Dos subclases: interruptores apagados (la configuracion de produccion) y
 * progresion encendida. Las cinco pruebas corren en las dos.
 */
abstract class CorreccionesReplicateBase {

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

    enum Dia { HECHO, FATIGA, TIEMPO, MOTIVACION, DOLOR }

    /** Lo que propone la IA simulada en las senaladas. */
    enum Modo { SUBE_SERIES, DURACION_UNA_SERIE, IGUAL, SOLO_MINUTOS, DOLOR_ANADE_MAS }

    record Ej(long id, String tipo, Short series, Short reps, Integer segundos, Short descanso, BigDecimal carga) {}

    private Modo modo;
    private final List<AiRetryFeedback> feedbacks = new CopyOnWriteArrayList<>();

    @BeforeEach
    void iaSimulada() {
        reset(ia);
        feedbacks.clear();
        when(ia.isEnabled()).thenReturn(true);
        when(ia.modelName()).thenReturn("simulado");
        when(ia.pedirRespuesta(any(), any(), any(), any(), anyInt())).thenAnswer(inv -> {
            if (inv.getArgument(3) instanceof AiRetryFeedback f) feedbacks.add(f);
            return new ReplicateSessionFixGenerator.RawFix("{}", "salida-simulada");
        });
        when(ia.interpretar(anyString(), any())).thenAnswer(inv -> corregir(inv.getArgument(1)));
    }

    // =======================================================================
    // 1 · La IA nunca sube dosis
    // =======================================================================

    @Test
    @DisplayName("C1 · la IA sube series tras una omision por fatiga en los 3 intentos -> RESPALDO, ninguna subida publicada")
    void c1SubidaDeDosis() throws Exception {
        modo = Modo.SUBE_SERIES;
        long u = participante(GoalType.INCREASE_STRENGTH, FitnessLevel.ADVANCED, 60);
        long plan = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        ejecutar(u, plan, Dia.HECHO, Dia.HECHO, Dia.FATIGA);
        var antes = semana(plan);

        long nuevo = ciclo(u, 0, 3);
        var ev = ultimo(u);
        assertEquals("RESPALDO", ev.get("status"), String.valueOf(ev));
        assertEquals(3, ((Number) ev.get("ai_calls")).intValue());
        assertTrue(feedbacks.stream().flatMap(f -> f.rejectionProblems().stream())
                .anyMatch(p -> p.contains("sube la dosis")), feedbacks.toString());
        var despues = semana(nuevo);
        for (int i = 0; i < antes.size(); i++)
            assertEquals(dosis(antes.get(i).getValue()), dosis(despues.get(i).getValue()),
                    "la semana publicada es la copia: ninguna dosis subida");
        assertSinViolaciones();
    }

    // =======================================================================
    // 2 · V9: series minimas en ejercicios por duracion
    // =======================================================================

    @Test
    @DisplayName("C2 · DURATION con 1 serie -> rechazo de V9 y reintento; sin excepcion SQL, semana publicada con evidencia")
    void c2DuracionUnaSerie() throws Exception {
        modo = Modo.DURACION_UNA_SERIE;
        long u = participante(GoalType.LOSE_WEIGHT, FitnessLevel.INTERMEDIATE, 45);
        long plan = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        var dias = semana(plan);
        int conDuracion = -1;
        for (int i = 0; i < dias.size() && conDuracion < 0; i++)
            if (dias.get(i).getValue().stream().anyMatch(e -> "DURATION".equals(e.tipo()) && e.series() != null))
                conDuracion = i;
        assertTrue(conDuracion >= 0, "premisa: una sesion con un ejercicio por duracion con series");
        var guion = new Dia[]{Dia.HECHO, Dia.HECHO, Dia.HECHO};
        guion[conDuracion] = Dia.TIEMPO;
        ejecutar(u, plan, guion);

        int filas = intentos(u);
        long nuevo = ciclo(u, 0, 3);
        assertEquals(filas + 1, intentos(u), "el ciclo deja su evidencia V32");
        var ev = ultimo(u);
        assertEquals("RESPALDO", ev.get("status"), String.valueOf(ev));
        assertTrue(feedbacks.stream().flatMap(f -> f.rejectionProblems().stream())
                .anyMatch(p -> p.startsWith("V9:")), "V9 lo rechaza antes de guardar: " + feedbacks);
        assertTrue(semana(nuevo).stream().flatMap(d -> d.getValue().stream())
                .noneMatch(e -> e.series() != null && e.series() < 2), "nada con menos de 2 series publicado");
        assertSinViolaciones();
    }

    // =======================================================================
    // 3 · V11: el respaldo retira la carga que ya no vale
    // =======================================================================

    @Test
    @DisplayName("C3 · carga sugerida con la sesion omitida: el respaldo la retira a null y publica; con peso anotado se conserva")
    void c3CargaSinReferencia() throws Exception {
        modo = Modo.IGUAL;
        long u = participante(GoalType.GENERAL_FITNESS, FitnessLevel.INTERMEDIATE, 45);
        long plan = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        long viernes = semana(plan).get(2).getKey();
        long ejercicio = jdbc.queryForObject("SELECT exercise_id FROM planned_workout_exercises WHERE planned_workout_id = ?"
                + " AND prescription_type = 'SETS_REPS' ORDER BY exercise_order LIMIT 1", Long.class, viernes);
        jdbc.update("UPDATE planned_workout_exercises SET target_load_kg = 20.00 WHERE planned_workout_id = ?"
                + " AND exercise_id = ?", viernes, ejercicio);

        // s2: todo hecho, con los 20 kg anotados -> la copia CONSERVA la carga (tiene referencia valida).
        ejecutar(u, plan, Dia.HECHO, Dia.HECHO, Dia.HECHO);
        plan = ciclo(u, 0, 0);
        assertEquals(new BigDecimal("20.00"), carga(semana(plan).get(2).getKey(), ejercicio),
                "con peso anotado la sugerencia no se pierde");

        // s3: miercoles y viernes omitidos -> sin peso anotado: V11 rechaza los 20 kg.
        ejecutar(u, plan, Dia.HECHO, Dia.MOTIVACION, Dia.MOTIVACION);
        long nuevo = ciclo(u, 1, 3);
        var ev = ultimo(u);
        assertEquals("RESPALDO", ev.get("status"), String.valueOf(ev));
        assertTrue(feedbacks.stream().flatMap(f -> f.rejectionProblems().stream())
                .anyMatch(p -> p.startsWith("V11:")), "la IA la mantuvo y V11 la rechazo: " + feedbacks);
        assertNull(carga(semana(nuevo).get(2).getKey(), ejercicio), "la sugerencia se retira a null, no a cero");
        assertTrue(informe(ev).contains("\"correcciones_del_backend\":[\"V11:"), informe(ev));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM planned_workout_exercises pwe JOIN planned_workouts pw"
                + " ON pw.planned_workout_id = pwe.planned_workout_id WHERE pw.plan_id = ? AND pwe.target_load_kg = 0",
                Integer.class, nuevo));
        assertSinViolaciones();
    }

    // =======================================================================
    // 4 · Los minutos solos no son una adaptacion
    // =======================================================================

    @Test
    @DisplayName("C4 · mismo contenido y un 10 % menos de minutos -> SIN_EFECTO y se publica la duracion validada")
    void c4SoloMinutos() throws Exception {
        modo = Modo.SOLO_MINUTOS;
        long u = participante(GoalType.LOSE_WEIGHT, FitnessLevel.INTERMEDIATE, 45);
        long plan = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        ejecutar(u, plan, Dia.HECHO, Dia.HECHO, Dia.FATIGA);
        int minutos = minutos(semana(plan).get(2).getKey());

        long nuevo = ciclo(u, 0, 1);
        var ev = ultimo(u);
        assertEquals("SIN_EFECTO", ev.get("status"), String.valueOf(ev));
        assertEquals("{}", ev.get("applied_sessions"), "V33: sin aplicadas");
        assertEquals(minutos, minutos(semana(nuevo).get(2).getKey()), "no se publica un tiempo inventado");
        assertTrue(informe(ev).contains("DURACION"), informe(ev));
        assertSinViolaciones();
    }

    // =======================================================================
    // 5 · Ante dolor no se anade trabajo
    // =======================================================================

    @Test
    @DisplayName("C5 · dolor: la IA retira todo y anade uno mas en los 3 intentos -> NO_FACTIBLE trazable, nada publicado")
    void c5DolorAnadeMas() throws Exception {
        modo = Modo.DOLOR_ANADE_MAS;
        long u = participante(GoalType.GAIN_MUSCLE, FitnessLevel.BEGINNER, 30);
        long plan = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        ejecutar(u, plan, Dia.HECHO, Dia.HECHO, Dia.DOLOR);

        clearInvocations(ia);
        var r = ciclo.runFor(u, S1, S1.plusWeeks(1));
        assertTrue(r.isEmpty(), "sin alternativa valida no se publica la sesion con mas trabajo");
        var ev = ultimo(u);
        assertEquals("NO_FACTIBLE", ev.get("status"), String.valueOf(ev));
        assertEquals(3, ((Number) ev.get("ai_calls")).intValue());
        assertTrue(feedbacks.stream().flatMap(f -> f.rejectionProblems().stream())
                .anyMatch(p -> p.contains("no se anade trabajo")), feedbacks.toString());
        assertNotNull(ev.get("rejection_reason"), "el motivo queda registrado");
        assertSinViolaciones();
    }

    // =======================================================================
    // IA simulada
    // =======================================================================

    private List<CorrectedSession> corregir(PlanGenerationContext ctx) {
        var salida = new ArrayList<CorrectedSession>();
        for (var senalada : ctx.adjustment().targetedSessions()) {
            long id = senalada.plannedWorkoutId();
            var f = jdbc.queryForMap("SELECT scheduled_date, focus_code, expected_duration_minutes FROM planned_workouts"
                    + " WHERE planned_workout_id = ?", id);
            var actuales = ejercicios(id);
            var lista = new ArrayList<PlanDraft.DraftExercise>();
            if (modo == Modo.DOLOR_ANADE_MAS) {
                var enSesion = actuales.stream().map(Ej::id).collect(Collectors.toSet());
                ctx.availableExercises().stream()
                        .filter(c -> c.defaultPrescription() == PrescriptionType.SETS_REPS && !enSesion.contains(c.exerciseId()))
                        .limit(actuales.size() + 1)
                        .forEach(c -> lista.add(new PlanDraft.DraftExercise(c.exerciseId(), PrescriptionType.SETS_REPS,
                                (short) 2, (short) 10, null, null, (short) 90, null)));
            } else {
                boolean hecho = false;
                for (var e : actuales) {
                    Short series = e.series();
                    if (modo == Modo.SUBE_SERIES && !hecho && "SETS_REPS".equals(e.tipo()) && series != null) {
                        series = (short) (series + 1);
                        hecho = true;
                    }
                    if (modo == Modo.DURACION_UNA_SERIE && "DURATION".equals(e.tipo()) && series != null)
                        series = 1;
                    lista.add(new PlanDraft.DraftExercise(e.id(), PrescriptionType.valueOf(e.tipo()), series, e.reps(),
                            e.segundos(), e.carga(), e.descanso(), null));
                }
            }
            var fecha = ((java.sql.Date) f.get("scheduled_date")).toLocalDate().plusWeeks(1);
            // SOLO_MINUTOS: el 90 % de lo declarado, dentro de la tolerancia de V17, como el caso real
            // (51 -> 41 min con el mismo contenido): asi pasaba los validadores y salia APLICADA.
            int declarados = ((Number) f.get("expected_duration_minutes")).intValue();
            int min = modo == Modo.SOLO_MINUTOS || modo == Modo.IGUAL
                    ? (modo == Modo.SOLO_MINUTOS ? (int) Math.round(declarados * 0.9) : declarados)
                    : estimador.estimateMinutes(new PlanDraft.DraftWorkout(fecha,
                    WorkoutFocus.valueOf((String) f.get("focus_code")), "x", 0, lista), ctx.prescription());
            salida.add(new CorrectedSession(id, fecha, min, lista, "propuesta simulada " + modo));
        }
        return salida;
    }

    // =======================================================================
    // Material
    // =======================================================================

    private long ciclo(long u, int k, int llamadasEsperadas) {
        clearInvocations(ia);
        var plan = ciclo.runFor(u, S1.plusWeeks(k), S1.plusWeeks(k + 1));
        int llamadas = (int) Mockito.mockingDetails(ia).getInvocations().stream()
                .filter(i -> i.getMethod().getName().equals("pedirRespuesta")).count();
        assertEquals(llamadasEsperadas, llamadas, "llamadas a la IA en la semana " + (k + 2) + ": " + ultimo(u));
        assertTrue(plan.isPresent(), "sin semana publicada en la " + (k + 2) + ": " + ultimo(u));
        return plan.get();
    }

    private long participante(GoalType objetivo, FitnessLevel nivel, int minutos) {
        String email = "correccion" + SECUENCIA.incrementAndGet() + "-" + getClass().getSimpleName().toLowerCase() + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Prueba', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1992, 1, 15), BiologicalSex.FEMALE,
                new BigDecimal("165"), new BigDecimal("70"), null, nivel, objetivo, "correcciones",
                TrainingLocation.GYM, (short) 3, List.of((short) 1, (short) 3, (short) 5), (short) minutos, GIMNASIO,
                List.of(), "Ninguna", true, true, true, true, true));
        return u;
    }

    private List<Map.Entry<Long, List<Ej>>> semana(long planId) {
        return jdbc.queryForList("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ? ORDER BY scheduled_date",
                Long.class, planId).stream().map(id -> Map.entry(id, ejercicios(id))).toList();
    }

    private List<Ej> ejercicios(long sesion) {
        return jdbc.query("SELECT exercise_id, prescription_type, planned_sets, planned_reps, planned_duration_seconds,"
                        + " rest_seconds, target_load_kg FROM planned_workout_exercises WHERE planned_workout_id = ?"
                        + " ORDER BY exercise_order",
                (rs, k) -> new Ej(rs.getLong(1), rs.getString(2), rs.getObject(3, Short.class),
                        rs.getObject(4, Short.class), rs.getObject(5, Integer.class), rs.getObject(6, Short.class),
                        rs.getBigDecimal(7)), sesion);
    }

    private static List<String> dosis(List<Ej> l) {
        return l.stream().map(e -> e.id() + ":" + e.series() + "x" + e.reps() + "/" + e.segundos() + "/" + e.descanso())
                .toList();
    }

    private BigDecimal carga(long sesion, long exerciseId) {
        return jdbc.queryForList("SELECT target_load_kg FROM planned_workout_exercises WHERE planned_workout_id = ?"
                + " AND exercise_id = ?", BigDecimal.class, sesion, exerciseId).get(0);
    }

    private int minutos(long sesion) {
        return jdbc.queryForObject("SELECT expected_duration_minutes FROM planned_workouts WHERE planned_workout_id = ?",
                Integer.class, sesion);
    }

    private int intentos(long u) {
        return jdbc.queryForObject("SELECT count(*) FROM plan_adaptation_attempts WHERE user_id = ?", Integer.class, u);
    }

    private Map<String, Object> ultimo(long u) {
        return jdbc.queryForMap("SELECT decision, status, generation_source, ai_calls, targeted_interventions::text,"
                + " applied_sessions::text, rejection_reason, conservation_report::text FROM plan_adaptation_attempts"
                + " WHERE user_id = ? ORDER BY attempt_id DESC LIMIT 1", u);
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
                            + " planned_reps, planned_duration_seconds, target_load_kg FROM planned_workout_exercises"
                            + " WHERE planned_workout_id = ?", w)) {
                        boolean duracion = "DURATION".equals(f.get("prescription_type"));
                        int series = f.get("planned_sets") == null ? 1 : ((Number) f.get("planned_sets")).intValue();
                        int total = series * ((Number) f.get(duracion ? "planned_duration_seconds" : "planned_reps")).intValue();
                        sesiones.handle(new RecordSessionExerciseCommand(u, s, ((Number) f.get("planned_exercise_id")).longValue(),
                                (short) series, duracion ? null : total, duracion ? total : null,
                                (BigDecimal) f.get("target_load_kg"), null));
                    }
                    sesiones.handle(new FinishWorkoutSessionCommand(u, s, (short) 6, (short) 4, (short) 45));
                }
                case FATIGA -> omitir(u, w, SkipReason.FATIGUE);
                case TIEMPO -> omitir(u, w, SkipReason.LACK_OF_TIME);
                case MOTIVACION -> omitir(u, w, SkipReason.LACK_OF_MOTIVATION);
                case DOLOR -> omitir(u, w, SkipReason.PAIN_OR_DISCOMFORT);
            }
        }
    }

    private void omitir(long u, long w, SkipReason motivo) {
        if (motivo == SkipReason.LACK_OF_TIME) saltoPorFaltaDeTiempo(UserDetailsImpl.build(usuarios.findById(u).orElseThrow()), w);
        else planes.skip(UserDetailsImpl.build(usuarios.findById(u).orElseThrow()), w, new SkipWorkoutResource(motivo));
    }

    private void assertSinViolaciones() throws Exception {
        var texto = Files.readString(Path.of("docs/FitSense/consultas/linaje_v31_v32.sql")).lines()
                .filter(l -> !l.stripLeading().startsWith("--")).collect(Collectors.joining("\n"));
        assertEquals(List.of(), jdbc.queryForList(texto.split(";")[0].strip()));
    }

    /**
     * ADAPT-2 (DISENO_ADAPTACION_CONTEXTUAL v0.3): una omision por falta de tiempo SIN contexto ya
     * no se acorta (D1). Este escenario prueba lo que pasa tras un recorte, asi que la persona
     * declara que suele tener menos tiempo del que dura la sesion (un minuto menos).
     */
    private org.springframework.http.ResponseEntity<Void> saltoPorFaltaDeTiempo(
            main.web.services.fitsense.iam.infrastructure.authorization.sfs.model.UserDetailsImpl principal, long w) {
        int minutos = jdbc.queryForObject("SELECT expected_duration_minutes FROM planned_workouts"
                + " WHERE planned_workout_id = ?", Integer.class, w);
        return planes.skip(principal, w, new SkipWorkoutResource(SkipReason.LACK_OF_TIME,
                main.web.services.fitsense.shared.domain.model.valueobjects.ContextoFaltaDeTiempo.MENOS_TIEMPO_HABITUAL,
                Math.max(15, minutos - 1)));
    }
}
