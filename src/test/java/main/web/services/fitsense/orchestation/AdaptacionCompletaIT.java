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
import main.web.services.fitsense.shared.domain.model.valueobjects.TrainingWeek;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ADAPTACION COMPLETA, del registro de una semana a la publicacion de la
 * siguiente, por el ciclo semanal de produccion ({@link WeeklyCycleService}):
 * cerrar, medir, puntuar la intervencion anterior, decidir y adaptar.
 * <p>
 * Cuatro objetivos x cuatro decisiones. La semana medida la genera el motor
 * de reglas y se ejecuta de verdad (sesiones, omisiones); la adherencia y la
 * decision salen del calculo real. Lo UNICO simulado es la IA de correccion
 * ({@link ReplicateSessionFixGenerator}): ante las sesiones senaladas por
 * LACK_OF_TIME (ACORTAR_O_REORGANIZAR) retira una colocacion de relleno, lo que
 * esa intervencion permite. Nada llega a Replicate.
 * <p>
 * Los esperados se fijan por escenario a mano: que decision, cuantas sesiones
 * cambian y cuales, y que no se pierde nada del historial.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
class AdaptacionCompletaIT {

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
    @Autowired main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService planComandos;
    @Autowired org.springframework.transaction.PlatformTransactionManager transacciones;
    @Autowired main.web.services.fitsense.analytics.interfaces.acl.AnalyticsContextFacade analitica;

    static final AtomicInteger SECUENCIA = new AtomicInteger();
    static final LocalDate MEDIDA = TrainingWeek.containing(LocalDate.now()).startDate().plusWeeks(1);
    static final LocalDate NUEVA = MEDIDA.plusWeeks(1);
    static final List<String> EQUIPAMIENTO = List.of("dumbbell", "barbell", "cable", "leverage machine", "bench", "band");

    /** Lo que se hace en cada uno de los tres dias de la semana medida. */
    enum Dia { HECHO, OMITIDO, NADA }

    record Ejercicio(long id, String tipo, Short series, Short reps, Integer segundos, Short descanso) {
        PlanDraft.DraftExercise borrador() {
            return new PlanDraft.DraftExercise(id, PrescriptionType.valueOf(tipo), series, reps, segundos,
                    null, descanso, null);
        }
    }

    record Sesion(long id, LocalDate fecha, String enfoque, int minutos, List<Ejercicio> ejercicios) {}

    /**
     * Escenario: dias ejecutados, decision esperada y cuantas sesiones debe
     * cambiar la adaptacion.
     * <ul>
     *   <li>H H H: observada 100, cobertura 100 -> NO_CHANGE, 0.</li>
     *   <li>H H O: observada (1 + 1 + 0) / 3 = 66,67 -> ADAPT_LOCALIZED, la omitida.</li>
     *   <li>H O O: observada 1 / 3 = 33,33 -> ADAPT_EXPANDED, las dos omitidas.</li>
     *   <li>H N N: cobertura 1 / 3 = 33,33 &lt; 50 -> INSUFFICIENT_DATA, 0.</li>
     * </ul>
     */
    enum Escenario {
        NO_CHANGE(0, Dia.HECHO, Dia.HECHO, Dia.HECHO),
        ADAPT_LOCALIZED(1, Dia.HECHO, Dia.HECHO, Dia.OMITIDO),
        ADAPT_EXPANDED(2, Dia.HECHO, Dia.OMITIDO, Dia.OMITIDO),
        INSUFFICIENT_DATA(0, Dia.HECHO, Dia.NADA, Dia.NADA);

        final int cambiadas;
        final Dia[] dias;

        Escenario(int cambiadas, Dia... dias) {
            this.cambiadas = cambiadas;
            this.dias = dias;
        }
    }

    static Stream<Arguments> casos() {
        var casos = new ArrayList<Arguments>();
        for (var objetivo : List.of(GoalType.GAIN_MUSCLE, GoalType.INCREASE_STRENGTH, GoalType.LOSE_WEIGHT,
                GoalType.GENERAL_FITNESS))
            for (var escenario : Escenario.values())
                casos.add(Arguments.of(objetivo, escenario));
        return casos.stream();
    }

    @BeforeEach
    void iaSimulada() {
        reset(ia);
        when(ia.isEnabled()).thenReturn(true);
        when(ia.modelName()).thenReturn("simulado");
        when(ia.pedirRespuesta(any(), any(), any(), any(), anyInt()))
                .thenReturn(new ReplicateSessionFixGenerator.RawFix("{}", "salida-simulada"));
    }

    // =====================================================================

    @ParameterizedTest(name = "{0} · {1}")
    @MethodSource("casos")
    @DisplayName("ciclo completo: ejecutar, medir, decidir, adaptar y publicar")
    void cicloCompleto(GoalType objetivo, Escenario escenario) {
        long u = participante(objetivo);
        long planMedido = planning.generateWeeklyPlan(u, MEDIDA, PlanAdjustment.none(), null, null).orElseThrow();
        var medida = semana(planMedido);
        assertEquals(3, medida.size(), "premisa: tres sesiones");
        ejecutar(u, medida, escenario.dias);
        var historico = historial(u, planMedido);

        // La IA simulada retira un relleno de cada sesion senalada.
        when(ia.interpretar(anyString(), any())).thenAnswer(inv -> corregir(inv.getArgument(1), medida));

        long planNuevo = ciclo.runFor(u, MEDIDA, NUEVA).orElseThrow(
                () -> new AssertionError("no se publico la semana nueva: " + evidencia(u)));

        // 1. Decision y trazabilidad.
        var ev = evidencia(u);
        assertEquals(escenario.name(), ev.get("decision"), String.valueOf(ev));
        var senaladas = senaladas(escenario, medida);
        assertEquals(escenario.cambiadas, senaladas.size());
        // Una senalada sin relleno no admite ACORTAR: la IA simulada la devuelve igual.
        var cambian = senaladas.stream().filter(id -> relleno(sesion(medida, id)).isPresent()).toList();
        String estado = escenario.cambiadas == 0 ? "SIN_ORDEN" : cambian.isEmpty() ? "SIN_EFECTO" : "APLICADA";
        assertEquals(estado, ev.get("status"), String.valueOf(ev));
        assertEquals(planNuevo, ((Number) ev.get("resulting_plan_id")).longValue());
        assertEquals(planMedido, ((Number) ev.get("source_plan_id")).longValue());
        assertEquals(cambian, lista(ev.get("applied_sessions")), "aplicadas = senaladas con algo que corregir");
        // La intervencion queda enlazada al plan que produjo.
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM user_interventions WHERE user_id = ?"
                + " AND resulting_plan_id = ?", Integer.class, u, planNuevo));
        assertEquals(escenario.cambiadas > 0 ? 1 : 0, ((Number) ev.get("ai_calls")).intValue(),
                "una sola peticion a la IA");
        assertEquals(cambian.isEmpty() ? "CONSERVED" : "AI", ev.get("generation_source"));

        // 2. Una sola version vigente, con linaje.
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ?"
                + " AND week_start_date = ? AND status = 'ACTIVE'", Integer.class, u, NUEVA));
        assertEquals(planNuevo, jdbc.queryForObject("SELECT plan_id FROM weekly_training_plans WHERE user_id = ?"
                + " AND week_start_date = ? AND status = 'ACTIVE'", Long.class, u, NUEVA));

        // 3. La semana nueva: misma estructura, +7 dias; solo cambian las senaladas, y solo un relleno menos.
        var nueva = semana(planNuevo);
        assertEquals(medida.size(), nueva.size());
        for (int i = 0; i < medida.size(); i++) {
            var antes = medida.get(i);
            var despues = nueva.get(i);
            assertEquals(antes.fecha().plusWeeks(1), despues.fecha());
            assertEquals(antes.enfoque(), despues.enfoque(), "el enfoque se conserva");
            assertEquals(antes.id(), jdbc.queryForObject("SELECT source_planned_workout_id FROM planned_workouts"
                    + " WHERE planned_workout_id = ?", Long.class, despues.id()), "linaje V31");
            if (cambian.contains(antes.id())) {
                var esperado = new ArrayList<>(antes.ejercicios());
                esperado.remove(relleno(antes).orElseThrow());
                assertEquals(esperado, despues.ejercicios(), "senalada " + antes.id() + ": un relleno menos");
            } else {
                assertEquals(antes.ejercicios(), despues.ejercicios(), "no senalada " + antes.id() + " cambio");
                assertEquals(antes.minutos(), despues.minutos());
            }
        }

        // 4. Seguridad y objetivo: nada fuera de lo que ya era elegible y seguro para la persona.
        var idsMedida = medida.stream().flatMap(s -> s.ejercicios().stream()).map(Ejercicio::id).toList();
        assertTrue(nueva.stream().flatMap(s -> s.ejercicios().stream()).allMatch(e -> idsMedida.contains(e.id())),
                "la adaptacion no incorpora ejercicios nuevos");
        assertEquals(objetivo.name(), jdbc.queryForObject("SELECT goal_type FROM user_profiles WHERE user_id = ?",
                String.class, u), "el objetivo no cambia");
        assertFalse(planning.fetchPlannedWorkout(nueva.get(0).id()).orElseThrow().blockedForSafety());

        // 5. Historial intacto.
        assertEquals(historico, historial(u, planMedido), "la adaptacion modifico la semana medida");

        // 6. Reintento del ciclo: no publica otra version ni otra evidencia.
        ciclo.runFor(u, MEDIDA, NUEVA);
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ?"
                + " AND week_start_date = ?", Integer.class, u, NUEVA), "el reintento publico otra version");
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM plan_adaptation_attempts WHERE user_id = ?"
                + " AND new_week_start_date = ?", Integer.class, u, NUEVA));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM user_interventions WHERE user_id = ?",
                Integer.class, u));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("sin IA (solo motor de reglas): la orden de adaptar se registra y la semana se conserva (RESPALDO)")
    void sinIaSeConserva() {
        when(ia.isEnabled()).thenReturn(false);
        long u = participante(GoalType.GAIN_MUSCLE);
        long planMedido = planning.generateWeeklyPlan(u, MEDIDA, PlanAdjustment.none(), null, null).orElseThrow();
        var medida = semana(planMedido);
        ejecutar(u, medida, Escenario.ADAPT_LOCALIZED.dias);

        long planNuevo = ciclo.runFor(u, MEDIDA, NUEVA).orElseThrow();
        var ev = evidencia(u);
        assertEquals("ADAPT_LOCALIZED", ev.get("decision"));
        assertEquals("RESPALDO", ev.get("status"));
        assertEquals("CONSERVED", ev.get("generation_source"));
        assertEquals(0, ((Number) ev.get("ai_calls")).intValue());
        var nueva = semana(planNuevo);
        for (int i = 0; i < medida.size(); i++)
            assertEquals(medida.get(i).ejercicios(), nueva.get(i).ejercicios(), "sin IA no se corrige nada");
        verify(ia, never()).pedirRespuesta(any(), any(), any(), any(), anyInt());
    }

    @org.junit.jupiter.api.Test
    @DisplayName("omitida por dolor y sin IA: no se republica el ejercicio que dolio; la semana queda sin plan y consta")
    void dolorSinIa() {
        when(ia.isEnabled()).thenReturn(false);
        long u = participante(GoalType.GAIN_MUSCLE);
        long planMedido = planning.generateWeeklyPlan(u, MEDIDA, PlanAdjustment.none(), null, null).orElseThrow();
        var medida = semana(planMedido);
        ejecutar(u, medida, Escenario.ADAPT_LOCALIZED.dias, SkipReason.PAIN_OR_DISCOMFORT);

        assertTrue(ciclo.runFor(u, MEDIDA, NUEVA).isEmpty());
        var ev = evidencia(u);
        assertEquals("ADAPT_LOCALIZED", ev.get("decision"));
        assertEquals("NO_FACTIBLE", ev.get("status"));
        assertNull(ev.get("resulting_plan_id"));
        assertTrue(String.valueOf(ev.get("rejection_reason")).contains("dolor"), String.valueOf(ev));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ?"
                + " AND week_start_date = ?", Integer.class, u, NUEVA));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("lo no registrado vence antes del ciclo: SKIPPED sin causa NO es un registro -> INSUFFICIENT_DATA")
    void vencidosAntesDelCiclo() {
        long u = participante(GoalType.GAIN_MUSCLE);
        long planMedido = planning.generateWeeklyPlan(u, MEDIDA, PlanAdjustment.none(), null, null).orElseThrow();
        var medida = semana(planMedido);
        ejecutar(u, medida, Escenario.INSUFFICIENT_DATA.dias);
        // La tarea horaria, ya pasada la semana: cierra como SKIPPED, sin causa, lo no registrado.
        planComandos.handle(new main.web.services.fitsense.planning.domain.model.commands
                .ExpireOverdueWorkoutsCommand(NUEVA.atStartOfDay().atOffset(java.time.ZoneOffset.UTC).plusHours(6)));
        assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM planned_workouts WHERE plan_id = ?"
                + " AND status = 'SKIPPED' AND skip_reason IS NULL", Integer.class, planMedido));

        ciclo.runFor(u, MEDIDA, NUEVA).orElseThrow();
        // Cobertura 1 / 3 = 33,33: no hay evidencia para adaptar. Antes salia 100 % y ADAPT_EXPANDED.
        assertEquals("INSUFFICIENT_DATA", evidencia(u).get("decision"));
        var m = jdbc.queryForMap("SELECT weighted_adherence_pct, registration_coverage_pct, recorded_workouts"
                + " FROM weekly_user_metrics WHERE user_id = ? AND week_start_date = ?", u, MEDIDA);
        assertEquals(0, new BigDecimal("33.33").compareTo((BigDecimal) m.get("weighted_adherence_pct")));
        assertEquals(0, new BigDecimal("33.33").compareTo((BigDecimal) m.get("registration_coverage_pct")));
        assertEquals(1, ((Number) m.get("recorded_workouts")).intValue());
    }

    @org.junit.jupiter.api.Test
    @DisplayName("primer plan ya generado para la semana nueva y ninguna semana medida: el ciclo lo respeta")
    void primerPlanAnticipado() {
        long u = participante(GoalType.GAIN_MUSCLE);
        long primero = planning.generateWeeklyPlan(u, NUEVA, PlanAdjustment.none(), null, null).orElseThrow();
        assertEquals(primero, ciclo.runFor(u, MEDIDA, NUEVA).orElseThrow());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ?",
                Integer.class, u));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("POST generate: ninguna sesion nace vencida; GET current la devuelve aunque empiece el lunes que viene")
    void generarSinSesionesPasadas() {
        long u = participante(GoalType.GAIN_MUSCLE);
        var principal = UserDetailsImpl.build(usuarios.findById(u).orElseThrow());
        var hoy = LocalDate.now(java.time.ZoneId.of("America/Lima"));
        var lunes = TrainingWeek.containing(hoy).startDate();
        // Lunes, miercoles y viernes: la actual solo si hoy es lunes.
        var esperado = hoy.equals(lunes) ? lunes : lunes.plusWeeks(1);

        var creado = planes.generate(principal).getBody();
        assertEquals(esperado, creado.weekStartDate());
        assertTrue(creado.workouts().stream().noneMatch(w -> w.scheduledDate().isBefore(hoy)), "ninguna en el pasado");
        assertEquals(creado.planId(), new org.springframework.transaction.support.TransactionTemplate(transacciones)
                .execute(tx -> planes.current(principal).getBody().planId()));
    }

    // ------------------------------------------------------------------ utilidades

    /**
     * Orden con {@code forcedSessionMinutes} = 45 sobre un perfil de 60. El
     * motor de reglas genero la semana medida con siete colocaciones
     * estructurales por sesion (el tope de 60 minutos) y ningun relleno. La
     * orden se pasa directamente a {@code adaptWeeklyPlan}: el decisor actual
     * nunca fija minutos.
     * <p>
     * La IA devuelve la senalada igual. V24 mide con 45 minutos (tope 6) y
     * rechaza las tres sesiones; no se publica nada. Antes medía con el perfil
     * (tope 7) y la semana de siete se publicaba con una orden de 45. Sin
     * relleno, ACORTAR_O_REORGANIZAR tampoco puede bajar a seis (V8C), y las
     * no senaladas no se tocan: con esta orden P2-D no tiene semana valida.
     */
    @org.junit.jupiter.api.Test
    @DisplayName("P2-D con una orden de 45 minutos sobre un perfil de 60: V24 mide con 45 y no publica siete")
    void ordenDeSesionesMasCortas() {
        long u = participante(GoalType.GAIN_MUSCLE, 60);
        long planMedido = planning.generateWeeklyPlan(u, MEDIDA, PlanAdjustment.none(), null, null).orElseThrow();
        var medida = semana(planMedido);
        assertEquals(List.of(7, 7, 7), medida.stream().map(x -> x.ejercicios().size()).toList(),
                "premisa: siete por sesion a 60 minutos");
        ejecutar(u, medida, Escenario.ADAPT_LOCALIZED.dias);
        planning.closeWeek(u, MEDIDA);
        var metricas = analitica.calculateWeek(u, MEDIDA).orElseThrow();

        var senalada = medida.get(2);
        var ajuste = new PlanAdjustment(List.of(AdjustmentType.NONE), null, null, null, 0.0, 0.0, null, null, 45,
                "sesiones de 45 minutos", "LACK_OF_TIME", null, List.of(new PlanAdjustment.TargetedSession(
                senalada.id(), main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion
                .ACORTAR_O_REORGANIZAR)));
        when(ia.interpretar(anyString(), any())).thenAnswer(inv -> List.of(new CorrectedSession(senalada.id(),
                senalada.fecha().plusWeeks(1), 40,
                senalada.ejercicios().stream().map(Ejercicio::borrador).toList(), "x")));

        var publicado = planning.adaptWeeklyPlan(u, NUEVA, ajuste, null, null,
                new main.web.services.fitsense.planning.interfaces.acl.MeasuredWeekView(metricas.weeklyMetricId(),
                        planMedido, metricas.observedAdherencePct(), metricas.registrationCoveragePct(),
                        "ADAPT_LOCALIZED"));

        assertTrue(publicado.isEmpty(), "se publico una semana de siete colocaciones con una orden de 45 minutos: "
                + publicado.map(id -> semana(id).stream().map(x -> x.ejercicios().size()).toList()));
        var ev = evidencia(u);
        assertEquals("NO_FACTIBLE", ev.get("status"), String.valueOf(ev));
        String motivo = String.valueOf(ev.get("rejection_reason"));
        for (var s : medida)
            assertTrue(motivo.contains("del " + s.fecha().plusWeeks(1)
                    + " tiene 7 colocaciones y el maximo de 4D v3.1 para sesiones de 45 minutos es 6"), motivo);
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ?"
                + " AND week_start_date = ?", Integer.class, u, NUEVA));
    }

    private long participante(GoalType objetivo) {
        return participante(objetivo, 45);
    }

    private long participante(GoalType objetivo, int minutos) {
        int n = SECUENCIA.incrementAndGet();
        String email = "adaptacion" + n + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Prueba', now(), now())", email);
        long userId = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(userId, LocalDate.of(1992, 5, 10), BiologicalSex.FEMALE,
                new BigDecimal("165"), new BigDecimal("68"), null, FitnessLevel.INTERMEDIATE, objetivo,
                "prueba de adaptacion", TrainingLocation.GYM, (short) 3,
                List.of((short) 1, (short) 3, (short) 5), (short) minutos, EQUIPAMIENTO,
                List.of(), "Ninguna", true, true, true, true, true));
        return userId;
    }

    private List<Sesion> semana(long planId) {
        var filas = jdbc.queryForList("SELECT planned_workout_id, scheduled_date, focus_code,"
                + " expected_duration_minutes FROM planned_workouts WHERE plan_id = ? ORDER BY scheduled_date", planId);
        var lista = new ArrayList<Sesion>();
        for (var f : filas) {
            long id = ((Number) f.get("planned_workout_id")).longValue();
            var ejercicios = jdbc.query("SELECT exercise_id, prescription_type, planned_sets, planned_reps,"
                            + " planned_duration_seconds, rest_seconds FROM planned_workout_exercises"
                            + " WHERE planned_workout_id = ? ORDER BY exercise_order",
                    (rs, k) -> new Ejercicio(rs.getLong(1), rs.getString(2), rs.getObject(3, Short.class),
                            rs.getObject(4, Short.class), rs.getObject(5, Integer.class),
                            rs.getObject(6, Short.class)), id);
            lista.add(new Sesion(id, ((java.sql.Date) f.get("scheduled_date")).toLocalDate(),
                    (String) f.get("focus_code"), ((Number) f.get("expected_duration_minutes")).intValue(),
                    ejercicios));
        }
        return lista;
    }

    private void ejecutar(long u, List<Sesion> semana, Dia[] dias) {
        ejecutar(u, semana, dias, SkipReason.LACK_OF_TIME);
    }

    private void ejecutar(long u, List<Sesion> semana, Dia[] dias, SkipReason causa) {
        var principal = UserDetailsImpl.build(usuarios.findById(u).orElseThrow());
        for (int i = 0; i < semana.size(); i++) {
            var s = semana.get(i);
            switch (dias[i]) {
                case HECHO -> {
                    long sesion = sesiones.handle(new StartWorkoutSessionCommand(u, s.id())).orElseThrow().getId();
                    var pes = jdbc.queryForList("SELECT planned_exercise_id FROM planned_workout_exercises"
                            + " WHERE planned_workout_id = ? ORDER BY exercise_order", Long.class, s.id());
                    for (int k = 0; k < pes.size(); k++) {
                        var e = s.ejercicios().get(k);
                        boolean duracion = "DURATION".equals(e.tipo());
                        int series = e.series() == null ? 1 : e.series();
                        sesiones.handle(new RecordSessionExerciseCommand(u, sesion, pes.get(k), (short) series,
                                duracion ? null : series * e.reps(), duracion ? series * e.segundos() : null,
                                null, null));
                    }
                    sesiones.handle(new FinishWorkoutSessionCommand(u, sesion, (short) 6, (short) 4, (short) 45));
                }
                case OMITIDO -> {
                    if (causa == SkipReason.LACK_OF_TIME) saltoPorFaltaDeTiempo(principal, s.id());
                    else planes.skip(principal, s.id(), new SkipWorkoutResource(causa));
                }
                case NADA -> { }
            }
        }
    }

    private static Set<Long> senaladas(Escenario escenario, List<Sesion> medida) {
        if (escenario.cambiadas == 0) return Set.of();
        var ids = new LinkedHashSet<Long>();
        for (int i = 0; i < medida.size(); i++)
            if (escenario.dias[i] == Dia.OMITIDO) ids.add(medida.get(i).id());
        return ids;
    }

    /** El relleno que retira la IA simulada: la ultima colocacion cuyo patron no es un slot de 4C. */
    private Optional<Ejercicio> relleno(Sesion s) {
        for (int k = s.ejercicios().size() - 1; k >= 0; k--) {
            var e = s.ejercicios().get(k);
            var patron = jdbc.queryForObject("SELECT movement_pattern FROM exercises WHERE exercise_id = ?",
                    String.class, e.id());
            if (MandatorySlot.ofMovementPattern(patron).isEmpty()) return Optional.of(e);
        }
        return Optional.empty();
    }

    private static Sesion sesion(List<Sesion> semana, long id) {
        return semana.stream().filter(s -> s.id() == id).findFirst().orElseThrow();
    }

    private List<CorrectedSession> corregir(PlanGenerationContext ctx, List<Sesion> medida) {
        var senaladas = ctx.adjustment().targetedSessions().stream()
                .map(PlanAdjustment.TargetedSession::plannedWorkoutId).toList();
        var correcciones = new ArrayList<CorrectedSession>();
        for (var s : medida) {
            if (!senaladas.contains(s.id())) continue;
            var quitar = relleno(s).orElse(null);
            var ejercicios = s.ejercicios().stream().filter(e -> e != quitar).map(Ejercicio::borrador).toList();
            var fecha = s.fecha().plusWeeks(1);
            int minutos = estimador.estimateMinutes(new PlanDraft.DraftWorkout(fecha,
                    WorkoutFocus.valueOf(s.enfoque()), "x", 0, ejercicios), ctx.prescription());
            correcciones.add(new CorrectedSession(s.id(), fecha, minutos, ejercicios, "retira un relleno"));
        }
        return correcciones;
    }

    private Map<String, Object> evidencia(long u) {
        var filas = jdbc.queryForList("SELECT decision, status, generation_source, ai_calls, resulting_plan_id,"
                + " source_plan_id, applied_sessions, rejection_reason FROM plan_adaptation_attempts WHERE user_id = ?"
                + " AND new_week_start_date = ?", u, NUEVA);
        return filas.isEmpty() ? Map.of() : filas.get(0);
    }

    private static List<Long> lista(Object arreglo) {
        try {
            var valores = (Object[]) ((java.sql.Array) arreglo).getArray();
            return Arrays.stream(valores).map(v -> ((Number) v).longValue()).toList();
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Todo lo ejecutado y lo indicado de la semana medida: no debe cambiar al adaptar. */
    private List<Map<String, Object>> historial(long u, long planMedido) {
        var filas = new ArrayList<Map<String, Object>>();
        filas.addAll(jdbc.queryForList("SELECT session_id, planned_workout_id, attempt_number, status,"
                + " completion_percentage, counts_toward_adherence, session_rpe FROM workout_sessions"
                + " WHERE user_id = ? ORDER BY session_id", u));
        filas.addAll(jdbc.queryForList("SELECT wse.* FROM workout_session_exercises wse JOIN workout_sessions ws"
                + " ON ws.session_id = wse.session_id WHERE ws.user_id = ? ORDER BY wse.session_exercise_id", u));
        filas.addAll(jdbc.queryForList("SELECT pw.planned_workout_id, pw.scheduled_date, pwe.exercise_id,"
                + " pwe.planned_sets, pwe.planned_reps, pwe.planned_duration_seconds FROM planned_workouts pw"
                + " JOIN planned_workout_exercises pwe ON pwe.planned_workout_id = pw.planned_workout_id"
                + " WHERE pw.plan_id = ? ORDER BY pw.scheduled_date, pwe.exercise_order", planMedido));
        return filas;
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
