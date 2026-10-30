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
 * Auditoria integral (30/10/2026), fase 6: secuencias de varias semanas que reproducen
 * comportamientos humanos, con los servicios reales (generacion, registro, adherencia, decision,
 * V8C, V1-V25, V31-V33 y publicacion) y la IA simulada respetando los permisos de cada
 * intervencion. Progresion APAGADA, como en produccion. Cada semana se comprueba: plan publicado,
 * linaje sin violaciones, ninguna sesion no senalada cambia, el ejercicio que dolio no vuelve y
 * ninguna sesion se recorta sin una causa que lo autorice.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.adaptation.progression.enabled=false"
})
@DisplayName("Validacion multisemana: escenarios humanos con el ciclo real, en los cuatro objetivos")
class ValidacionMultisemanaIT {

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

    enum Dia { HECHO, FATIGA, TIEMPO, PUNTUAL, SIN_TIEMPO, HABITUAL_ALCANZA, HABITUAL_NO_ALCANZA, HABITUAL_INVIABLE, OTRA, PARCIAL_DOLOR, DIFICIL, DESMOTIVADO, HORARIO, NADA }

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

    /** Una secuencia: las semanas (lunes, miercoles, viernes) y lo que se espera de cada ciclo. */
    record Secuencia(String nombre, Dia[][] semanas, String[] intervenciones) {}

    static Dia[] s(Dia... d) { return d; }

    static final Dia H = Dia.HECHO;

    static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments> casos() {
        var secuencias = List.of(
                new Secuencia("1 cumple todo", new Dia[][]{s(H, H, H), s(H, H, H), s(H, H, H)}, new String[]{"{}", "{}", "{}"}),
                new Secuencia("2 imprevisto una vez", new Dia[][]{s(H, H, Dia.PUNTUAL), s(H, H, H), s(H, H, H)},
                        new String[]{"{CONSERVAR}", "{}", "{}"}),
                new Secuencia("3 falta de tiempo recurrente con minutos", new Dia[][]{s(H, H, Dia.HABITUAL_NO_ALCANZA),
                        s(H, H, H), s(H, H, H)}, new String[]{"{ACORTAR_O_REORGANIZAR}", "{}", "{}"}),
                new Secuencia("4 sin ningun minuto ese dia", new Dia[][]{s(H, H, Dia.SIN_TIEMPO), s(H, H, H)},
                        new String[]{"{CONSERVAR}", "{}"}),
                new Secuencia("6 fatiga", new Dia[][]{s(H, H, Dia.FATIGA), s(H, H, H)},
                        new String[]{"{REVISAR_CARGA_Y_RECUPERACION}", "{}"}),
                new Secuencia("7 dolor", new Dia[][]{s(H, Dia.PARCIAL_DOLOR, H), s(H, H, H), s(H, H, H)},
                        new String[]{"{SEGURIDAD_ANTE_DOLOR}", "{}", "{}"}),
                new Secuencia("8 demasiado dificil", new Dia[][]{s(H, H, Dia.DIFICIL), s(H, H, H)},
                        new String[]{"{VARIANTE_MAS_SENCILLA_O_CARGA}", "{}"}),
                new Secuencia("9 desmotivado", new Dia[][]{s(H, H, Dia.DESMOTIVADO), s(H, H, H)},
                        new String[]{"{INVESTIGAR_CAMBIO_MINIMO}", "{}"}),
                new Secuencia("10 varios motivos", new Dia[][]{s(H, Dia.PARCIAL_DOLOR, Dia.TIEMPO), s(H, H, H)},
                        new String[]{"{SEGURIDAD_ANTE_DOLOR}", "{}"}),
                new Secuencia("11 alterna y 13 vuelve a omitir la adaptada", new Dia[][]{s(H, H, Dia.FATIGA), s(H, H, H),
                        s(H, H, Dia.FATIGA), s(H, H, H)},
                        new String[]{"{REVISAR_CARGA_Y_RECUPERACION}", "{}", "{REVISAR_CARGA_Y_RECUPERACION}", "{}"}),
                new Secuencia("horario", new Dia[][]{s(H, H, Dia.HORARIO), s(H, H, H)},
                        new String[]{"{REPROGRAMAR_DIAS}", "{}"}),
                new Secuencia("sin registro", new Dia[][]{s(H, Dia.NADA, Dia.NADA), s(H, H, H)},
                        new String[]{"{}", "{}"}));
        var casos = new ArrayList<org.junit.jupiter.params.provider.Arguments>();
        for (var objetivo : GoalType.values())
            for (var sec : secuencias) casos.add(org.junit.jupiter.params.provider.Arguments.of(objetivo, sec));
        return casos.stream();
    }

    @org.junit.jupiter.params.ParameterizedTest(name = "{0} · {1}")
    @org.junit.jupiter.params.provider.MethodSource("casos")
    void secuencia(GoalType objetivo, Secuencia sec) throws Exception {
        long u = participante(objetivo, FitnessLevel.INTERMEDIATE, 45);
        long plan = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        var traza = new ArrayList<String>();
        var dolieron = new HashSet<Long>();
        for (int k = 0; k < sec.semanas().length; k++) {
            var antes = semana(plan);
            var minutosAntes = estructura(plan, 7);
            ejecutar(u, plan, sec.semanas()[k]);
            dolieron.addAll(jdbc.queryForList("SELECT pwe.exercise_id FROM workout_session_exercises wse JOIN"
                    + " planned_workout_exercises pwe ON pwe.planned_exercise_id = wse.planned_exercise_id JOIN"
                    + " planned_workouts pw ON pw.planned_workout_id = pwe.planned_workout_id WHERE pw.plan_id = ?"
                    + " AND wse.skip_reason = 'PAIN_OR_DISCOMFORT'", Long.class, plan));
            var publicado = ciclo.runFor(u, S1.plusWeeks(k), S1.plusWeeks(k + 1));
            var ev = ultimo(u);
            traza.add("s" + (k + 2) + " " + ev.get("decision") + "/" + ev.get("status") + " " + ev.get("targeted_interventions"));
            assertTrue(publicado.isPresent(), sec.nombre() + ": semana " + (k + 2) + " sin publicar: " + traza + " " + ev);
            plan = publicado.get();
            assertEquals(sec.intervenciones()[k], ev.get("targeted_interventions"), sec.nombre() + " " + traza);
            var despues = semana(plan);
            var senaladas = String.valueOf(ev.get("targeted_interventions"));
            // Sin orden, o con una orden que no cambia nada: la semana es identica.
            if (senaladas.equals("{}") || senaladas.equals("{CONSERVAR}") || "SIN_EFECTO".equals(ev.get("status"))
                    || "RESPALDO".equals(ev.get("status"))) {
                for (int i = 0; i < antes.size(); i++)
                    assertEquals(antes.get(i).getValue(), despues.get(i).getValue(), sec.nombre() + " sesion " + i + " " + traza);
                assertEquals(minutosAntes, estructura(plan, 0), "mismas fechas y minutos " + traza);
            }
            // Nada se recorta sin una causa que lo autorice.
            var mins0 = minutosAntes.stream().map(x -> Integer.parseInt(x.split("\\|")[2])).toList();
            var mins1 = estructura(plan, 0).stream().map(x -> Integer.parseInt(x.split("\\|")[2])).toList();
            for (int i = 0; i < mins0.size(); i++)
                if (mins1.get(i) < mins0.get(i))
                    assertTrue(senaladas.contains("ACORTAR") || senaladas.contains("SEGURIDAD")
                                    || senaladas.contains("REVISAR") || senaladas.contains("INVESTIGAR"),
                            sec.nombre() + ": la sesion " + i + " baja de " + mins0.get(i) + " a " + mins1.get(i)
                                    + " sin causa que lo autorice " + traza);
            // Lo que dolio no vuelve.
            for (var ses : despues)
                for (var e : ses.getValue())
                    assertFalse(dolieron.contains(e.id()), sec.nombre() + ": vuelve el ejercicio " + e.id() + " " + traza);
            assertSinViolaciones();
        }
        System.out.println("MULTISEMANA " + objetivo + " · " + sec.nombre() + " · " + traza);
    }

    /**
     * PENDIENTE DE DECISION (AUDITORIA_INTEGRAL_LOGICA.md): un cambio del perfil (dias, equipamiento)
     * no llega a la semana copiada y la semana siguiente no se publica. Reproduccion opt-in
     * ({@code -Dfitsense.pendientes=true}) para no fijar como correcto lo que no lo es ni romper el CI
     * mientras no se decida la politica.
     */
    @org.junit.jupiter.params.ParameterizedTest(name = "5 · cambia su perfil ({0}): la semana siguiente debe publicarse")
    @org.junit.jupiter.params.provider.ValueSource(strings = {"dias", "equipamiento"})
    void cambiaSuPerfil(String cambio) throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(Boolean.getBoolean("fitsense.pendientes"),
                "reproduccion de un pendiente de decision");
        long u = participante(GoalType.GENERAL_FITNESS, FitnessLevel.INTERMEDIATE, 45);
        long w1 = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        ejecutar(u, w1, H, H, H);
        if (cambio.equals("dias"))
            jdbc.update("UPDATE user_profiles SET available_days = '{2,4,6}' WHERE user_id = ?", u);
        else
            jdbc.update("UPDATE user_profiles SET equipment_codes = '{\"body weight\",\"dumbbell\",\"band\"}'"
                    + " WHERE user_id = ?", u);
        var publicado = ciclo.runFor(u, S1, S1.plusWeeks(1));
        var ev = ultimo(u);
        System.out.println("CAMBIA_PERFIL " + cambio + " " + ev.get("decision") + "/" + ev.get("status") + " "
                + ev.get("conservation_report"));
        assertTrue(publicado.isPresent(), "la semana se publica: " + ev);
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
            Long sustituto = null;
            if (senalada.intervencion() == TipoIntervencion.SEGURIDAD_ANTE_DOLOR) {
                // Sustituir el ejercicio que dolio por un elegible del mismo patron y grupo (lo permite la
                // intervencion); si no hay, retirarlo.
                var dolio = jdbc.queryForList("SELECT pwe.exercise_id FROM workout_session_exercises wse"
                        + " JOIN planned_workout_exercises pwe ON pwe.planned_exercise_id = wse.planned_exercise_id"
                        + " WHERE pwe.planned_workout_id = ? AND wse.skip_reason = 'PAIN_OR_DISCOMFORT'", Long.class, id);
                for (int k = 0; k < actuales.size() && quitar < 0; k++)
                    if (dolio.contains(actuales.get(k).getKey().exerciseId())) quitar = k;
                if (quitar >= 0) {
                    long idDolio = actuales.get(quitar).getKey().exerciseId();
                    var original = ctx.availableExercises().stream()
                            .filter(c -> c.exerciseId().equals(idDolio)).findFirst();
                    var enSesion = actuales.stream().map(x -> x.getKey().exerciseId()).collect(Collectors.toSet());
                    sustituto = original.flatMap(o -> ctx.availableExercises().stream()
                            .filter(c -> !enSesion.contains(c.exerciseId()) && !dolio.contains(c.exerciseId())
                                    && java.util.Objects.equals(c.movementPattern(), o.movementPattern())
                                    && java.util.Objects.equals(c.bodyPartCode(), o.bodyPartCode()))
                            .map(c -> c.exerciseId()).findFirst()).orElse(null);
                }
            }
            if (senalada.intervencion() == TipoIntervencion.ACORTAR_O_REORGANIZAR)
                for (int k = actuales.size() - 1; k >= 0 && quitar < 0; k--)
                    if (MandatorySlot.ofMovementPattern(actuales.get(k).getValue()).isEmpty()) quitar = k;
            var lista = new ArrayList<PlanDraft.DraftExercise>();
            boolean hecho = false;
            boolean ganarMusculo = "GAIN_MUSCLE".equals(ctx.profile().goalType());
            for (int k = 0; k < actuales.size(); k++) {
                var e = actuales.get(k).getKey();
                if (k == quitar) {
                    if (sustituto != null)
                        lista.add(new PlanDraft.DraftExercise(sustituto, e.prescriptionType(), e.plannedSets(),
                                e.plannedReps(), e.plannedDurationSeconds(), null, e.restSeconds(), null));
                    continue;
                }
                Short series = e.plannedSets(), reps = e.plannedReps();
                if ((senalada.intervencion() == TipoIntervencion.REVISAR_CARGA_Y_RECUPERACION
                        || senalada.intervencion() == TipoIntervencion.INVESTIGAR_CAMBIO_MINIMO) && !hecho
                        && !ganarMusculo   // 4D: en GAIN_MUSCLE no hay reducciones autorizadas
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
        String email = "multisemana" + SECUENCIA.incrementAndGet() + "@pruebas.invalid";
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
                case PARCIAL_DOLOR -> {
                    // Todo hecho salvo el ultimo ejercicio, dejado por dolor.
                    long s = sesiones.handle(new StartWorkoutSessionCommand(u, w)).orElseThrow().getId();
                    var filas = jdbc.queryForList("SELECT planned_exercise_id, prescription_type, planned_sets,"
                            + " planned_reps, planned_duration_seconds FROM planned_workout_exercises"
                            + " WHERE planned_workout_id = ? ORDER BY exercise_order", w);
                    for (int k = 0; k < filas.size(); k++) {
                        var f = filas.get(k);
                        long pe = ((Number) f.get("planned_exercise_id")).longValue();
                        if (k == filas.size() - 1) {
                            sesiones.handle(new RecordSessionExerciseCommand(u, s, pe, (short) 0, null, null, null,
                                    SkipReason.PAIN_OR_DISCOMFORT));
                            continue;
                        }
                        boolean duracion = "DURATION".equals(f.get("prescription_type"));
                        int series = f.get("planned_sets") == null ? 1 : ((Number) f.get("planned_sets")).intValue();
                        int total = series * ((Number) f.get(duracion ? "planned_duration_seconds" : "planned_reps")).intValue();
                        sesiones.handle(new RecordSessionExerciseCommand(u, s, pe, (short) series,
                                duracion ? null : total, duracion ? total : null, null, null));
                    }
                    sesiones.handle(new FinishWorkoutSessionCommand(u, s, (short) 6, (short) 4, (short) 45));
                }
                case DIFICIL -> planes.skip(UserDetailsImpl.build(usuarios.findById(u).orElseThrow()), w,
                        new SkipWorkoutResource(SkipReason.TOO_DIFFICULT));
                case DESMOTIVADO -> planes.skip(UserDetailsImpl.build(usuarios.findById(u).orElseThrow()), w,
                        new SkipWorkoutResource(SkipReason.LACK_OF_MOTIVATION));
                case HORARIO -> planes.skip(UserDetailsImpl.build(usuarios.findById(u).orElseThrow()), w,
                        new SkipWorkoutResource(SkipReason.SCHEDULE_CHANGE));
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
