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
 * CASO 11 (diseno v0.3) · RESTAURACION REAL DE UNA SESION ADAPTADA POR FATIGA.
 * <p>
 * Ciclo de produccion ({@link WeeklyCycleService#runFor}) sobre PostgreSQL real,
 * con la restauracion habilitada SOLO en esta prueba
 * ({@code fitsense.adaptation.restoration.enabled=true}). Lo unico simulado es
 * la IA de correccion, que en la semana de la fatiga baja dos repeticiones de
 * un ejercicio, lo que REVISAR_CARGA_Y_RECUPERACION permite. Nada llega a
 * Replicate.
 * <p>
 * {@code GENERAL_FITNESS}: en {@code GAIN_MUSCLE} 4D impone la dosis exacta y
 * un ajuste de dosis por fatiga no puede publicarse.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.adaptation.restoration.enabled=true"
})
class RestauracionIT {

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
    static final List<String> EQUIPAMIENTO = List.of("dumbbell", "barbell", "cable", "leverage machine", "bench", "band");

    enum Dia { HECHO, HECHO_CON_FATIGA, FATIGA, TIEMPO, NADA }

    record Ejercicio(long id, String tipo, Short series, Short reps, Integer segundos, Short descanso) {}

    record Sesion(long id, LocalDate fecha, String enfoque, int minutos, List<Ejercicio> ejercicios) {}

    @BeforeEach
    void iaSimulada() {
        reset(ia);
        when(ia.isEnabled()).thenReturn(true);
        when(ia.modelName()).thenReturn("simulado");
        when(ia.pedirRespuesta(any(), any(), any(), any(), anyInt()))
                .thenReturn(new ReplicateSessionFixGenerator.RawFix("{}", "salida-simulada"));
        when(ia.interpretar(anyString(), any())).thenAnswer(inv -> corregir(inv.getArgument(1)));
    }

    /** Semana 1 → fatiga el viernes → semana 2 con el viernes ajustado. Devuelve [w1, w2]. */
    private long[] adaptarPorFatiga(long u) {
        long w1 = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        ejecutar(u, w1, Dia.HECHO, Dia.HECHO, Dia.FATIGA);
        long w2 = ciclo.runFor(u, S1, S1.plusWeeks(1)).orElseThrow();
        var ev = ultimoIntento(u);
        assertEquals("ADAPT_LOCALIZED", ev.get("decision"), String.valueOf(ev));
        assertEquals("APLICADA", ev.get("status"), "premisa: la fatiga se adapto. " + ev);
        assertEquals("{REVISAR_CARGA_Y_RECUPERACION}", ev.get("targeted_interventions"));
        assertNotEquals(semana(w1).get(2).ejercicios(), semana(w2).get(2).ejercicios(),
                "premisa: el viernes adaptado difiere de la referencia");
        return new long[]{w1, w2};
    }

    @Test
    @DisplayName("caso 11: fatiga adaptada, viernes completado sin fatiga -> NO_CHANGE publica la restauracion sin IA")
    void restauracionEnNoChange() throws Exception {
        long u = participante();
        var w = adaptarPorFatiga(u);
        long w1 = w[0], w2 = w[1];
        var referencia = semana(w1);
        var adaptada = semana(w2);

        ejecutar(u, w2, Dia.HECHO, Dia.HECHO, Dia.HECHO);
        var metricasAntes = metricas(u);
        var historialAntes = historial(u, w1, w2);
        clearInvocations(ia);

        long w3 = ciclo.runFor(u, S1.plusWeeks(1), S1.plusWeeks(2)).orElseThrow();
        verify(ia, never()).pedirRespuesta(any(), any(), any(), any(), anyInt());

        // Publicacion y V32.
        var ev = ultimoIntento(u);
        assertEquals("NO_CHANGE", ev.get("decision"));
        assertEquals("SIN_ORDEN", ev.get("status"), String.valueOf(ev));
        assertEquals("CONSERVED", ev.get("generation_source"));
        assertEquals(0, ((Number) ev.get("ai_calls")).intValue());
        assertEquals("{" + adaptada.get(2).id() + "}", ev.get("restored_sessions"));
        assertEquals("{}", ev.get("applied_sessions"));
        assertEquals("{" + adaptada.get(0).id() + "," + adaptada.get(1).id() + "}", ev.get("conserved_sessions"));
        assertEquals(w3, ((Number) ev.get("resulting_plan_id")).longValue());

        // Exactamente la referencia, en su dia; lunes y miercoles, copias.
        var nueva = semana(w3);
        assertEquals(referencia.get(2).ejercicios(), nueva.get(2).ejercicios(), "no se restauro la prescripcion");
        assertEquals(referencia.get(2).minutos(), nueva.get(2).minutos());
        assertEquals(referencia.get(2).enfoque(), nueva.get(2).enfoque());
        assertEquals(referencia.get(2).fecha().plusWeeks(2), nueva.get(2).fecha(), "mismo dia de la semana");
        for (int i = 0; i < 2; i++) {
            assertEquals(adaptada.get(i).ejercicios(), nueva.get(i).ejercicios());
            assertEquals(adaptada.get(i).minutos(), nueva.get(i).minutos());
        }
        // Ningun ejercicio nuevo: todo estaba en la referencia o en la semana medida.
        var conocidos = new HashSet<Long>();
        referencia.forEach(s -> s.ejercicios().forEach(e -> conocidos.add(e.id())));
        adaptada.forEach(s -> s.ejercicios().forEach(e -> conocidos.add(e.id())));
        assertTrue(nueva.stream().flatMap(s -> s.ejercicios().stream()).allMatch(e -> conocidos.contains(e.id())));

        // V31: las tres derivan de la semana medida; una sola version vigente.
        for (int i = 0; i < 3; i++)
            assertEquals(adaptada.get(i).id(), origenDe(nueva.get(i).id()));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ?"
                + " AND week_start_date = ? AND status = 'ACTIVE'", Integer.class, u, S1.plusWeeks(2)));

        // Historial y adherencia ya calculada, intactos.
        assertEquals(historialAntes, historial(u, w1, w2));
        assertEquals(metricasAntes, metricas(u).stream().filter(m -> metricasAntes.contains(m)).toList());

        // Multisemana: la restaurada se copia sin restaurarse otra vez.
        ejecutar(u, w3, Dia.HECHO, Dia.HECHO, Dia.HECHO);
        long w4 = ciclo.runFor(u, S1.plusWeeks(2), S1.plusWeeks(3)).orElseThrow();
        var ev4 = ultimoIntento(u);
        assertEquals("SIN_ORDEN", ev4.get("status"));
        assertEquals("{}", ev4.get("restored_sessions"));
        assertEquals(nueva.get(2).ejercicios(), semana(w4).get(2).ejercicios());

        assertSinViolaciones();
        System.out.println("RESTAURACION caso11 referencia=" + referencia.get(2).ejercicios()
                + " adaptada=" + adaptada.get(2).ejercicios() + " restaurada=" + nueva.get(2).ejercicios());
    }

    @Test
    @DisplayName("sin informacion del viernes adaptado: no se restaura; queda pendiente y se restaura cuando se completa")
    void sinInformacionQuedaPendiente() throws Exception {
        long u = participante();
        var w = adaptarPorFatiga(u);
        var referencia = semana(w[0]);
        var adaptada = semana(w[1]);

        ejecutar(u, w[1], Dia.HECHO, Dia.HECHO, Dia.NADA);
        long w3 = ciclo.runFor(u, S1.plusWeeks(1), S1.plusWeeks(2)).orElseThrow();
        var ev = ultimoIntento(u);
        assertEquals("{}", ev.get("restored_sessions"), String.valueOf(ev));
        assertEquals(adaptada.get(2).ejercicios(), semana(w3).get(2).ejercicios(), "el ajuste se mantiene");

        ejecutar(u, w3, Dia.HECHO, Dia.HECHO, Dia.HECHO);
        long w4 = ciclo.runFor(u, S1.plusWeeks(2), S1.plusWeeks(3)).orElseThrow();
        var ev4 = ultimoIntento(u);
        assertEquals("SIN_ORDEN", ev4.get("status"));
        assertEquals("{" + semana(w3).get(2).id() + "}", ev4.get("restored_sessions"));
        assertEquals(referencia.get(2).ejercicios(), semana(w4).get(2).ejercicios(),
                "tras una copia intermedia se restaura la referencia de la semana 1");
        assertSinViolaciones();
    }

    @Test
    @DisplayName("viernes completado pero con fatiga declarada en un ejercicio: no se restaura")
    void fatigaDeclaradaNoRestaura() {
        long u = participante();
        var w = adaptarPorFatiga(u);
        var adaptada = semana(w[1]);
        ejecutar(u, w[1], Dia.HECHO, Dia.HECHO, Dia.HECHO_CON_FATIGA);
        assertEquals("COMPLETED", jdbc.queryForObject("SELECT status FROM workout_sessions"
                + " WHERE planned_workout_id = ? AND counts_toward_adherence", String.class, adaptada.get(2).id()),
                "premisa: completado, con fatiga declarada en un ejercicio");
        long w3 = ciclo.runFor(u, S1.plusWeeks(1), S1.plusWeeks(2)).orElseThrow();
        var ev = ultimoIntento(u);
        assertEquals("{}", ev.get("restored_sessions"), String.valueOf(ev));
        assertEquals(adaptada.get(2).ejercicios(), semana(w3).get(2).ejercicios());
    }

    @Test
    @DisplayName("restriccion nueva: un ejercicio de la referencia queda bloqueado; no se restaura ni se publica")
    void restriccionIncompatibleNoRestaura() {
        long u = participante();
        var w = adaptarPorFatiga(u);
        var referencia = semana(w[0]);
        var adaptada = semana(w[1]);
        ejecutar(u, w[1], Dia.HECHO, Dia.HECHO, Dia.HECHO);

        // El ejercicio que la fatiga cambio de dosis queda bloqueado por la persona.
        long cambiado = referencia.get(2).ejercicios().stream()
                .filter(e -> !adaptada.get(2).ejercicios().contains(e)).findFirst().orElseThrow().id();
        jdbc.update("UPDATE user_profiles SET blocked_exercise_ids = ARRAY[?]::bigint[] WHERE user_id = ?",
                cambiado, u);

        var w3 = ciclo.runFor(u, S1.plusWeeks(1), S1.plusWeeks(2));
        var ev = ultimoIntento(u);
        System.out.println("RESTAURACION restriccion " + ev.get("status") + " " + ev.get("conservation_report"));
        assertEquals("{}", ev.get("restored_sessions"), String.valueOf(ev));
        assertTrue(String.valueOf(ev.get("conservation_report")).contains("no se restauro")
                        || !"SIN_ORDEN".equals(ev.get("status")),
                "el bloqueo tiene que constar: " + ev);
        w3.ifPresent(plan -> assertTrue(semana(plan).stream().flatMap(s -> s.ejercicios().stream())
                .noneMatch(e -> e.id() == cambiado), "se publico el ejercicio bloqueado"));
    }

    @Test
    @DisplayName("una modificacion por ciclo: si otra sesion se adapta, la restauracion espera al ciclo siguiente")
    void unaModificacionPorCiclo() throws Exception {
        long u = participante();
        var w = adaptarPorFatiga(u);
        var referencia = semana(w[0]);
        var adaptada = semana(w[1]);

        // Viernes completado (restaurable) y lunes omitido por falta de tiempo -> ADAPT_LOCALIZED.
        ejecutar(u, w[1], Dia.TIEMPO, Dia.HECHO, Dia.HECHO);
        long w3 = ciclo.runFor(u, S1.plusWeeks(1), S1.plusWeeks(2)).orElseThrow();
        var ev = ultimoIntento(u);
        assertEquals("ADAPT_LOCALIZED", ev.get("decision"), String.valueOf(ev));
        assertEquals("{}", ev.get("restored_sessions"), "dos modificaciones en un ciclo: " + ev);
        assertEquals(adaptada.get(2).ejercicios(), semana(w3).get(2).ejercicios(), "la restauracion espera");

        ejecutar(u, w3, Dia.HECHO, Dia.HECHO, Dia.HECHO);
        long w4 = ciclo.runFor(u, S1.plusWeeks(2), S1.plusWeeks(3)).orElseThrow();
        var ev4 = ultimoIntento(u);
        assertEquals("SIN_ORDEN", ev4.get("status"));
        assertEquals("{" + semana(w3).get(2).id() + "}", ev4.get("restored_sessions"));
        assertEquals(referencia.get(2).ejercicios(), semana(w4).get(2).ejercicios());
        assertSinViolaciones();
    }

    // =======================================================================

    /**
     * La IA simulada, con lo que permite cada intervencion: REVISAR_CARGA baja dos repeticiones
     * del primer ejercicio por repeticiones; ACORTAR retira el ultimo relleno.
     */
    private List<CorrectedSession> corregir(PlanGenerationContext ctx) {
        var salida = new ArrayList<CorrectedSession>();
        for (var senalada : ctx.adjustment().targetedSessions()) {
            long id = senalada.plannedWorkoutId();
            var s = sesion(id);
            var ejercicios = new ArrayList<PlanDraft.DraftExercise>();
            boolean hecho = false;
            for (var e : s.ejercicios()) {
                if (senalada.intervencion() == TipoIntervencion.ACORTAR_O_REORGANIZAR && !hecho
                        && e == s.ejercicios().get(s.ejercicios().size() - 1)) {
                    hecho = true;
                    continue;
                }
                short reps = e.reps() == null ? 0 : e.reps();
                if (senalada.intervencion() == TipoIntervencion.REVISAR_CARGA_Y_RECUPERACION && !hecho
                        && "SETS_REPS".equals(e.tipo()) && reps >= 8) {
                    reps = (short) (reps - 2);
                    hecho = true;
                }
                ejercicios.add(new PlanDraft.DraftExercise(e.id(), PrescriptionType.valueOf(e.tipo()), e.series(),
                        e.reps() == null ? null : reps, e.segundos(), null, e.descanso(), null));
            }
            var fecha = s.fecha().plusWeeks(1);
            int minutos = estimador.estimateMinutes(new PlanDraft.DraftWorkout(fecha,
                    WorkoutFocus.valueOf(s.enfoque()), "x", 0, ejercicios), ctx.prescription());
            salida.add(new CorrectedSession(id, fecha, minutos, ejercicios, "ajuste simulado"));
        }
        return salida;
    }

    private long participante() {
        String email = "restauracion" + SECUENCIA.incrementAndGet() + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Prueba', now(), now())", email);
        long userId = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(userId, LocalDate.of(1992, 5, 10), BiologicalSex.FEMALE,
                new BigDecimal("165"), new BigDecimal("68"), null, FitnessLevel.INTERMEDIATE,
                GoalType.GENERAL_FITNESS, "prueba de restauracion", TrainingLocation.GYM, (short) 3,
                List.of((short) 1, (short) 3, (short) 5), (short) 45, EQUIPAMIENTO,
                List.of(), "Ninguna", true, true, true, true, true));
        return userId;
    }

    private List<Sesion> semana(long planId) {
        return jdbc.queryForList("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ?"
                + " ORDER BY scheduled_date", Long.class, planId).stream().map(this::sesion).toList();
    }

    private Sesion sesion(long id) {
        var f = jdbc.queryForMap("SELECT scheduled_date, focus_code, expected_duration_minutes FROM planned_workouts"
                + " WHERE planned_workout_id = ?", id);
        var ejercicios = jdbc.query("SELECT exercise_id, prescription_type, planned_sets, planned_reps,"
                        + " planned_duration_seconds, rest_seconds FROM planned_workout_exercises"
                        + " WHERE planned_workout_id = ? ORDER BY exercise_order",
                (rs, k) -> new Ejercicio(rs.getLong(1), rs.getString(2), rs.getObject(3, Short.class),
                        rs.getObject(4, Short.class), rs.getObject(5, Integer.class), rs.getObject(6, Short.class)), id);
        return new Sesion(id, ((java.sql.Date) f.get("scheduled_date")).toLocalDate(), (String) f.get("focus_code"),
                ((Number) f.get("expected_duration_minutes")).intValue(), ejercicios);
    }

    private Long origenDe(long plannedWorkoutId) {
        return jdbc.queryForObject("SELECT source_planned_workout_id FROM planned_workouts"
                + " WHERE planned_workout_id = ?", Long.class, plannedWorkoutId);
    }

    private void ejecutar(long u, long planId, Dia... dias) {
        var principal = UserDetailsImpl.build(usuarios.findById(u).orElseThrow());
        var ids = jdbc.queryForList("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ?"
                + " ORDER BY scheduled_date", Long.class, planId);
        for (int i = 0; i < ids.size(); i++) {
            long w = ids.get(i);
            switch (dias[i]) {
                case HECHO, HECHO_CON_FATIGA -> {
                    long sesion = sesiones.handle(new StartWorkoutSessionCommand(u, w)).orElseThrow().getId();
                    var filas = jdbc.queryForList("SELECT planned_exercise_id, prescription_type, planned_sets,"
                            + " planned_reps, planned_duration_seconds FROM planned_workout_exercises"
                            + " WHERE planned_workout_id = ? ORDER BY exercise_order", w);
                    for (int k = 0; k < filas.size(); k++) {
                        var f = filas.get(k);
                        boolean duracion = "DURATION".equals(f.get("prescription_type"));
                        int series = f.get("planned_sets") == null ? 1 : ((Number) f.get("planned_sets")).intValue();
                        int total = duracion ? series * ((Number) f.get("planned_duration_seconds")).intValue()
                                : series * ((Number) f.get("planned_reps")).intValue();
                        // El ultimo ejercicio, a medias y con fatiga declarada: la sesion sigue completada.
                        boolean fatiga = dias[i] == Dia.HECHO_CON_FATIGA && k == filas.size() - 1;
                        if (fatiga) total = total / 2;
                        sesiones.handle(new RecordSessionExerciseCommand(u, sesion,
                                ((Number) f.get("planned_exercise_id")).longValue(), (short) series,
                                duracion ? null : total, duracion ? total : null, null,
                                fatiga ? SkipReason.FATIGUE : null));
                    }
                    sesiones.handle(new FinishWorkoutSessionCommand(u, sesion, (short) 6, (short) 4, (short) 45));
                }
                case FATIGA -> planes.skip(principal, w, new SkipWorkoutResource(SkipReason.FATIGUE));
                case TIEMPO -> planes.skip(principal, w, new SkipWorkoutResource(SkipReason.LACK_OF_TIME));
                case NADA -> { }
            }
        }
    }

    private Map<String, Object> ultimoIntento(long u) {
        return jdbc.queryForMap("SELECT decision, status, generation_source, ai_calls, resulting_plan_id,"
                + " targeted_interventions::text, applied_sessions::text, no_effect_sessions::text,"
                + " conserved_sessions::text, restored_sessions::text, conservation_report::text, rejection_reason"
                + " FROM plan_adaptation_attempts WHERE user_id = ? ORDER BY attempt_id DESC LIMIT 1", u);
    }

    private List<String> metricas(long u) {
        return jdbc.queryForList("SELECT week_start_date || '|' || weighted_adherence_pct || '|'"
                + " || coalesce(observed_adherence_pct::text, '-') FROM weekly_user_metrics WHERE user_id = ?"
                + " ORDER BY week_start_date", String.class, u);
    }

    private List<Map<String, Object>> historial(long u, long... planes) {
        var filas = new ArrayList<Map<String, Object>>();
        filas.addAll(jdbc.queryForList("SELECT session_id, planned_workout_id, attempt_number, status,"
                + " completion_percentage, counts_toward_adherence, session_rpe FROM workout_sessions"
                + " WHERE user_id = ? ORDER BY session_id", u));
        filas.addAll(jdbc.queryForList("SELECT wse.* FROM workout_session_exercises wse JOIN workout_sessions ws"
                + " ON ws.session_id = wse.session_id WHERE ws.user_id = ? ORDER BY wse.session_exercise_id", u));
        for (long plan : planes)
            filas.addAll(jdbc.queryForList("SELECT pw.planned_workout_id, pw.scheduled_date, pw.status,"
                    + " pwe.exercise_id, pwe.planned_sets, pwe.planned_reps, pwe.planned_duration_seconds"
                    + " FROM planned_workouts pw JOIN planned_workout_exercises pwe"
                    + " ON pwe.planned_workout_id = pw.planned_workout_id WHERE pw.plan_id = ?"
                    + " ORDER BY pw.scheduled_date, pwe.exercise_order", plan));
        return filas;
    }

    private void assertSinViolaciones() throws Exception {
        var texto = Files.readString(Path.of("docs/FitSense/consultas/linaje_v31_v32.sql")).lines()
                .filter(l -> !l.stripLeading().startsWith("--")).collect(Collectors.joining("\n"));
        var violaciones = jdbc.queryForList(texto.split(";")[0].strip());
        assertEquals(List.of(), violaciones);
    }
}
