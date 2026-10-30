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
import main.web.services.fitsense.shared.domain.model.valueobjects.ContextoFaltaDeTiempo;
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
 * DISENO_ADAPTACION_CONTEXTUAL v0.3 (D1, D2, D6, D7) y caso A01: una omision por falta de tiempo no
 * justifica por si sola recortar. Con el ciclo semanal real y la IA simulada:
 * <ul>
 *   <li>sin respuesta, imprevisto o sin tiempo ese dia: se conserva la sesion, 0 llamadas,
 *       {@code CONSERVAR} en V32, sin efecto, origen CONSERVED, regla ADAPT-2;</li>
 *   <li>menos tiempo habitual con M que ya alcanza: igual;</li>
 *   <li>menos tiempo habitual con M que no alcanza: se acorta con la IA y la sesion publicada dura
 *       como mucho M (V8C);</li>
 *   <li>una omision sin causa ({@code CAUSA_NO_REGISTRADA}) tampoco llama a la IA (D6);</li>
 *   <li>la falta de tiempo no cambia el perfil ni las demas sesiones.</li>
 * </ul>
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.adaptation.progression.enabled=true"
})
@DisplayName("Adaptacion por causas: falta de tiempo con contexto (A01), sin IA cuando no hay nada que pedir")
class AdaptacionPorCausasIT {

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

    enum Dia { HECHO, FATIGA, TIEMPO, PUNTUAL, SIN_TIEMPO, HABITUAL_ALCANZA, HABITUAL_NO_ALCANZA, HABITUAL_INVIABLE, OTRA, NADA }

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

    @org.junit.jupiter.params.ParameterizedTest(name = "A01 · viernes omitido por falta de tiempo, {0}: se conserva sin IA")
    @org.junit.jupiter.params.provider.EnumSource(value = Dia.class, names = {"TIEMPO", "PUNTUAL", "SIN_TIEMPO", "HABITUAL_ALCANZA"})
    void faltaDeTiempoSinEvidenciaDeRecorte(Dia viernes) throws Exception {
        long u = participante(GoalType.GAIN_MUSCLE, FitnessLevel.INTERMEDIATE, 45);
        long w1 = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        var perfilAntes = perfil(u);
        ejecutar(u, w1, Dia.HECHO, Dia.HECHO, viernes);
        long w2 = ciclo(u, 0, 0);
        var ev = ultimo(u);
        assertEquals("ADAPT_LOCALIZED", ev.get("decision"), String.valueOf(ev));
        assertEquals("{CONSERVAR}", ev.get("targeted_interventions"));
        assertEquals("SIN_EFECTO", ev.get("status"));
        assertEquals("CONSERVED", ev.get("generation_source"));
        assertEquals(0, ((Number) ev.get("ai_calls")).intValue());
        assertEquals("ADAPT-2", ev.get("adaptation_rule_version"));
        // Las tres sesiones, viernes incluido, identicas: nada se recorta sin evidencia.
        var a = semana(w1);
        var b = semana(w2);
        for (int i = 0; i < a.size(); i++) assertEquals(a.get(i).getValue(), b.get(i).getValue(), "sesion " + i);
        assertEquals(estructura(w1, 7), estructura(w2, 0), "mismas fechas +7 y mismos minutos");
        assertEquals(perfilAntes, perfil(u), "una omision no cambia la disponibilidad del perfil");
        assertSinViolaciones();
    }

    @Test
    @DisplayName("A01 · menos tiempo habitual que no alcanza: se acorta con la IA y la sesion cabe en los minutos declarados")
    void menosTiempoHabitualAcorta() throws Exception {
        long u = participante(GoalType.GAIN_MUSCLE, FitnessLevel.INTERMEDIATE, 45);
        long w1 = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        ejecutar(u, w1, Dia.HECHO, Dia.HECHO, Dia.HABITUAL_NO_ALCANZA);
        int m = minutosDeclarados(w1);
        long w2 = ciclo(u, 0, 1);
        var ev = ultimo(u);
        assertEquals("{ACORTAR_O_REORGANIZAR}", ev.get("targeted_interventions"), String.valueOf(ev));
        assertEquals("APLICADA", ev.get("status"), String.valueOf(ev));
        var viernes = estructura(w2, 0).get(2).split("\\|");
        assertTrue(Integer.parseInt(viernes[2]) <= m, "el viernes dura " + viernes[2] + " y se declararon " + m);
        var a = semana(w1);
        var b = semana(w2);
        for (int i = 0; i < 2; i++) assertEquals(a.get(i).getValue(), b.get(i).getValue(), "lunes y miercoles iguales");
        assertSinViolaciones();
    }

    @Test
    @DisplayName("A01 · M inalcanzable sin tocar estructurales: no se fuerza; se conserva y consta (3 intentos, RESPALDO)")
    void menosTiempoHabitualInviable() throws Exception {
        long u = participante(GoalType.GAIN_MUSCLE, FitnessLevel.INTERMEDIATE, 45);
        long w1 = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        ejecutar(u, w1, Dia.HECHO, Dia.HECHO, Dia.HABITUAL_INVIABLE);
        long w2 = ciclo(u, 0, 3);
        var ev = ultimo(u);
        assertEquals("{ACORTAR_O_REORGANIZAR}", ev.get("targeted_interventions"), String.valueOf(ev));
        assertEquals("RESPALDO", ev.get("status"), String.valueOf(ev));
        var a = semana(w1);
        var b = semana(w2);
        for (int i = 0; i < a.size(); i++) assertEquals(a.get(i).getValue(), b.get(i).getValue(), "se conserva todo");
        assertSinViolaciones();
    }

    @Test
    @DisplayName("D6 · omision por otra causa: CAUSA_NO_REGISTRADA, se conserva y no se llama a la IA")
    void sinCausaNoLlamaALaIA() throws Exception {
        long u = participante(GoalType.LOSE_WEIGHT, FitnessLevel.INTERMEDIATE, 45);
        long w1 = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        ejecutar(u, w1, Dia.HECHO, Dia.HECHO, Dia.OTRA);
        long w2 = ciclo(u, 0, 0);
        var ev = ultimo(u);
        assertEquals("{CAUSA_NO_REGISTRADA}", ev.get("targeted_interventions"), String.valueOf(ev));
        assertEquals("SIN_EFECTO", ev.get("status"));
        assertEquals(0, ((Number) ev.get("ai_calls")).intValue());
        var a = semana(w1);
        var b = semana(w2);
        for (int i = 0; i < a.size(); i++) assertEquals(a.get(i).getValue(), b.get(i).getValue());
        assertSinViolaciones();
    }

    @Test
    @DisplayName("Contrato: contexto sin LACK_OF_TIME, minutos sin MENOS_TIEMPO_HABITUAL o fuera de rango se rechazan")
    void contratoDelSalto() {
        long u = participante(GoalType.GAIN_MUSCLE, FitnessLevel.INTERMEDIATE, 45);
        long w1 = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        long w = jdbc.queryForList("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ? ORDER BY scheduled_date",
                Long.class, w1).get(0);
        var p = UserDetailsImpl.build(usuarios.findById(u).orElseThrow());
        assertThrows(RuntimeException.class, () -> planes.skip(p, w, new SkipWorkoutResource(SkipReason.FATIGUE,
                ContextoFaltaDeTiempo.IMPREVISTO, null)));
        assertThrows(RuntimeException.class, () -> planes.skip(p, w, new SkipWorkoutResource(SkipReason.LACK_OF_TIME,
                ContextoFaltaDeTiempo.IMPREVISTO, 30)));
        assertThrows(RuntimeException.class, () -> planes.skip(p, w, new SkipWorkoutResource(SkipReason.LACK_OF_TIME,
                ContextoFaltaDeTiempo.MENOS_TIEMPO_HABITUAL, null)));
        // Valido: queda guardado tal cual.
        planes.skip(p, w, new SkipWorkoutResource(SkipReason.LACK_OF_TIME, ContextoFaltaDeTiempo.MENOS_TIEMPO_HABITUAL, 30));
        assertEquals("MENOS_TIEMPO_HABITUAL|30", jdbc.queryForObject("SELECT lack_of_time_context || '|' ||"
                + " declared_available_minutes FROM planned_workouts WHERE planned_workout_id = ?", String.class, w));
        // Un cliente antiguo (solo la causa) sigue funcionando: sin contexto.
        long w2 = jdbc.queryForList("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ? ORDER BY scheduled_date",
                Long.class, w1).get(1);
        planes.skip(p, w2, new SkipWorkoutResource(SkipReason.LACK_OF_TIME));
        assertNull(jdbc.queryForObject("SELECT lack_of_time_context FROM planned_workouts WHERE planned_workout_id = ?",
                String.class, w2));
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
                Short series = e.plannedSets(), reps = e.plannedReps();
                if (senalada.intervencion() == TipoIntervencion.REVISAR_CARGA_Y_RECUPERACION && !hecho
                        && e.prescriptionType() == PrescriptionType.SETS_REPS && reps != null
                        && reps - 2 >= minimo(ctx, e.exerciseId())) {
                    reps = (short) (reps - 2);
                    hecho = true;
                }
                lista.add(new PlanDraft.DraftExercise(e.exerciseId(), e.prescriptionType(), series, reps,
                        e.plannedDurationSeconds(), null, e.restSeconds(), null));
            }
            var fecha = ((java.sql.Date) f.get("scheduled_date")).toLocalDate().plusWeeks(1);
            int min = estimador.estimateMinutes(new PlanDraft.DraftWorkout(fecha,
                    WorkoutFocus.valueOf((String) f.get("focus_code")), "x", 0, lista), ctx.prescription());
            salida.add(new CorrectedSession(id, fecha, min, lista, "ajuste simulado"));
        }
        return salida;
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
        String email = "causas" + SECUENCIA.incrementAndGet() + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Prueba', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1992, 1, 15), BiologicalSex.FEMALE,
                new BigDecimal("165"), new BigDecimal("70"), null, nivel, objetivo, "sin ia", TrainingLocation.GYM,
                (short) 3, List.of((short) 1, (short) 3, (short) 5), (short) minutos, GIMNASIO,
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
                + " progressed_sessions::text, conservation_report::text, adaptation_rule_version FROM plan_adaptation_attempts WHERE user_id = ?"
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
                case PUNTUAL -> planes.skip(UserDetailsImpl.build(usuarios.findById(u).orElseThrow()), w,
                        new SkipWorkoutResource(SkipReason.LACK_OF_TIME, ContextoFaltaDeTiempo.IMPREVISTO, null));
                case SIN_TIEMPO -> planes.skip(UserDetailsImpl.build(usuarios.findById(u).orElseThrow()), w,
                        new SkipWorkoutResource(SkipReason.LACK_OF_TIME, ContextoFaltaDeTiempo.SIN_TIEMPO_ESE_DIA, null));
                case HABITUAL_ALCANZA -> planes.skip(UserDetailsImpl.build(usuarios.findById(u).orElseThrow()), w,
                        new SkipWorkoutResource(SkipReason.LACK_OF_TIME, ContextoFaltaDeTiempo.MENOS_TIEMPO_HABITUAL,
                                Math.max(15, minutos(w))));
                case HABITUAL_NO_ALCANZA -> planes.skip(UserDetailsImpl.build(usuarios.findById(u).orElseThrow()), w,
                        new SkipWorkoutResource(SkipReason.LACK_OF_TIME, ContextoFaltaDeTiempo.MENOS_TIEMPO_HABITUAL,
                                minutos(w) - 3));
                case HABITUAL_INVIABLE -> planes.skip(UserDetailsImpl.build(usuarios.findById(u).orElseThrow()), w,
                        new SkipWorkoutResource(SkipReason.LACK_OF_TIME, ContextoFaltaDeTiempo.MENOS_TIEMPO_HABITUAL, 15));
                case OTRA -> planes.skip(UserDetailsImpl.build(usuarios.findById(u).orElseThrow()), w,
                        new SkipWorkoutResource(SkipReason.OTHER));
                case NADA -> { }
            }
        }
    }

    private int minutos(long w) {
        return jdbc.queryForObject("SELECT expected_duration_minutes FROM planned_workouts WHERE planned_workout_id = ?",
                Integer.class, w);
    }

    /** Los minutos declarados del viernes de ese plan (HABITUAL_NO_ALCANZA). */
    private int minutosDeclarados(long planId) {
        return jdbc.queryForObject("SELECT declared_available_minutes FROM planned_workouts WHERE plan_id = ?"
                + " AND declared_available_minutes IS NOT NULL", Integer.class, planId);
    }

    private Map<String, Object> perfil(long u) {
        return jdbc.queryForMap("SELECT session_minutes, days_per_week, available_days::text FROM user_profiles"
                + " WHERE user_id = ?", u);
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
