package main.web.services.fitsense.p2d;

import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.domain.services.CatalogEligibilityPolicy;
import main.web.services.fitsense.planning.domain.services.GoalFeasibilityPolicy;
import main.web.services.fitsense.planning.domain.model.valueobjects.MvpDosePolicy;
import main.web.services.fitsense.planning.domain.services.SessionDurationEstimator;
import main.web.services.fitsense.planning.infrastructure.generation.ai.AiRetryFeedback;
import main.web.services.fitsense.planning.infrastructure.generation.ai.ReplicateSessionFixGenerator;
import main.web.services.fitsense.planning.interfaces.acl.MeasuredWeekView;
import main.web.services.fitsense.planning.interfaces.acl.PlanningContextFacade;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.BiologicalSex;
import main.web.services.fitsense.profiling.domain.model.valueobjects.FitnessLevel;
import main.web.services.fitsense.profiling.domain.model.valueobjects.GoalType;
import main.web.services.fitsense.profiling.domain.model.valueobjects.TrainingLocation;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Regresion de P2-D (adaptacion) ante los cambios que vienen: reclasificacion
 * de ejercicios, complementos de cardio y movilidad, y bloqueos individuales.
 * <p>
 * FLUJO REAL, IA SIMULADA. PostgreSQL efimero con V1..V33 (Flyway),
 * {@link PlanningContextFacade#adaptWeeklyPlan} de produccion, validadores
 * reales (V8C, estricta con 4D en ENFORCE, V1..V24) y persistencia real. Lo
 * unico simulado es {@link ReplicateSessionFixGenerator}: devuelve las
 * correcciones que cada caso necesita. Nada llega a Replicate y no hay ninguna
 * autorizacion de pago que pedir.
 * <p>
 * Las clases de P2-D de produccion NO se tocan: esta prueba solo las ejerce.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
class AdaptacionRegresionIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    /** La IA de correccion, simulada. Ningun otro bean se sustituye. */
    @MockitoBean ReplicateSessionFixGenerator ia;

    @Autowired PlanningContextFacade planning;
    @Autowired UserProfileCommandService perfiles;
    @Autowired SessionDurationEstimator estimador;
    @Autowired JdbcTemplate jdbc;
    @Autowired main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService planComandos;

    static final LocalDate SEMANA_MEDIDA = LocalDate.of(2026, 11, 2);
    static final LocalDate SEMANA_NUEVA = SEMANA_MEDIDA.plusWeeks(1);
    static final AtomicInteger SECUENCIA = new AtomicInteger();

    @BeforeEach
    void iaSimulada() {
        reset(ia);
        when(ia.isEnabled()).thenReturn(true);
        when(ia.modelName()).thenReturn("simulado");
        when(ia.pedirRespuesta(any(), any(), any(), any(), anyInt()))
                .thenReturn(new ReplicateSessionFixGenerator.RawFix("{}", "salida-simulada"));
    }

    // =====================================================================
    // Escenario
    // =====================================================================

    record Ejercicio(long id, String tipo, Short series, Short reps, Integer segundos, Short descanso) {
        PlanDraft.DraftExercise borrador() {
            return new PlanDraft.DraftExercise(id, PrescriptionType.valueOf(tipo), series, reps, segundos,
                    null, descanso, null);
        }
    }

    record Sesion(long id, LocalDate fecha, String enfoque, List<Ejercicio> ejercicios) {}

    record Participante(long userId, long planMedido, long metrica, List<Sesion> semana) {
        Set<Long> ids() {
            return semana.stream().flatMap(s -> s.ejercicios().stream()).map(Ejercicio::id)
                    .collect(Collectors.toSet());
        }
    }

    static final List<String> EQUIPAMIENTO = List.of("dumbbell", "barbell", "cable", "leverage machine", "bench", "band");

    private Participante participante(GoalType objetivo, Boolean permiteSuelo) {
        return participante(objetivo, permiteSuelo, EQUIPAMIENTO, true);
    }

    private Participante participante(GoalType objetivo, Boolean permiteSuelo, List<String> equipamiento,
                                      Boolean permiteBrazosArriba) {
        int n = SECUENCIA.incrementAndGet();
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Prueba', now(), now())", "regresion" + n + "@pruebas.invalid");
        long userId = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class,
                "regresion" + n + "@pruebas.invalid");
        perfiles.handle(new CreateUserProfileCommand(userId, LocalDate.of(1992, 5, 10), BiologicalSex.FEMALE,
                new BigDecimal("165"), new BigDecimal("68"), null, FitnessLevel.INTERMEDIATE, objetivo,
                "prueba de regresion", TrainingLocation.GYM, (short) 3, List.of((short) 1, (short) 3, (short) 5),
                (short) 45, equipamiento,
                List.of(), "Ninguna", true, permiteSuelo, true, true, permiteBrazosArriba));

        long plan = planning.generateWeeklyPlan(userId, SEMANA_MEDIDA, PlanAdjustment.none(), null, null)
                .orElseThrow(() -> new AssertionError("no se genero la semana medida"));
        jdbc.update("INSERT INTO weekly_user_metrics (user_id, plan_id, week_number, week_start_date,"
                        + " week_end_date, has_active_plan, calculation_version, weighted_adherence_pct)"
                        + " VALUES (?, ?, 1, ?, ?, TRUE, 'MVP-1.8', 42.00)",
                userId, plan, SEMANA_MEDIDA, SEMANA_MEDIDA.plusDays(6));
        long metrica = jdbc.queryForObject("SELECT weekly_metric_id FROM weekly_user_metrics"
                + " WHERE user_id = ? AND week_start_date = ?", Long.class, userId, SEMANA_MEDIDA);
        return new Participante(userId, plan, metrica, semana(plan));
    }

    private List<Sesion> semana(long planId) {
        var sesiones = jdbc.query("SELECT planned_workout_id, scheduled_date, focus_code FROM planned_workouts"
                        + " WHERE plan_id = ? ORDER BY scheduled_date",
                (rs, i) -> new long[]{rs.getLong(1), rs.getDate(2).toLocalDate().toEpochDay()},
                planId);
        var enfoques = jdbc.queryForList("SELECT focus_code FROM planned_workouts WHERE plan_id = ?"
                + " ORDER BY scheduled_date", String.class, planId);
        var lista = new ArrayList<Sesion>();
        for (int i = 0; i < sesiones.size(); i++) {
            long id = sesiones.get(i)[0];
            var ejercicios = jdbc.query("SELECT exercise_id, prescription_type, planned_sets, planned_reps,"
                            + " planned_duration_seconds, rest_seconds FROM planned_workout_exercises"
                            + " WHERE planned_workout_id = ? ORDER BY exercise_order",
                    (rs, k) -> new Ejercicio(rs.getLong(1), rs.getString(2), (Short) rs.getObject(3, Short.class),
                            (Short) rs.getObject(4, Short.class), (Integer) rs.getObject(5, Integer.class),
                            (Short) rs.getObject(6, Short.class)), id);
            lista.add(new Sesion(id, LocalDate.ofEpochDay(sesiones.get(i)[1]), enfoques.get(i), ejercicios));
        }
        return lista;
    }

    private Optional<Long> adaptar(Participante p, AdaptationDecision decision,
                                   List<PlanAdjustment.TargetedSession> senaladas) {
        var orden = new PlanAdjustment(List.of(AdjustmentType.NONE), null, null, null, 0.0, 0.0, null, null,
                null, "regresion", null, null, senaladas);
        return planning.adaptWeeklyPlan(p.userId(), SEMANA_NUEVA, orden, new BigDecimal("42.00"), null,
                new MeasuredWeekView(p.metrica(), p.planMedido(), new BigDecimal("42.00"),
                        new BigDecimal("100.00"), decision.name()));
    }

    /** La fila de evidencia V32 del intento sobre la semana nueva. */
    private Map<String, Object> intento(Participante p) {
        return jdbc.queryForMap("SELECT status, generation_source, ai_calls, resulting_plan_id,"
                + " rejection_reason FROM plan_adaptation_attempts WHERE user_id = ? AND new_week_start_date = ?",
                p.userId(), SEMANA_NUEVA);
    }

    /** generation_attempts del plan publicado. */
    private int intentosDeGeneracion(long planId) {
        return jdbc.queryForObject("SELECT generation_attempts FROM weekly_training_plans WHERE plan_id = ?",
                Integer.class, planId);
    }

    private int llamadasIa(Participante p) {
        return ((Number) intento(p).get("ai_calls")).intValue();
    }

    private String patron(long ejercicio) {
        return jdbc.queryForObject("SELECT movement_pattern FROM exercises WHERE exercise_id = ?",
                String.class, ejercicio);
    }

    private static boolean esSlot(String patron) {
        return MandatorySlot.ofMovementPattern(patron).isPresent();
    }

    /** Lo publicado conserva, sesion a sesion, ejercicios y dosis de la semana medida. */
    private void assertCopiaExacta(Participante p, long planNuevo) {
        var nueva = semana(planNuevo);
        assertEquals(p.semana().size(), nueva.size());
        for (int i = 0; i < nueva.size(); i++) {
            assertEquals(p.semana().get(i).fecha().plusWeeks(1), nueva.get(i).fecha());
            assertEquals(p.semana().get(i).ejercicios(), nueva.get(i).ejercicios(),
                    "la sesion " + p.semana().get(i).id() + " cambio al conservarse");
        }
    }

    /**
     * Sustitucion por seguridad publicada: solo cambian las posiciones del
     * quitado, cada una por un sustituto; dia, enfoque, duracion y el resto de
     * ejercicios y dosis quedan identicos. La evidencia V32 registra cada una.
     * Devuelve los sustitutos.
     */
    private List<Long> assertSustitucionSegura(Participante p, long planNuevo, long quitado, String motivo) {
        return assertSustitucionSegura(p, planNuevo, quitado, motivo, 0);
    }

    private List<Long> assertSustitucionSegura(Participante p, long planNuevo, long quitado, String motivo,
                                               int llamadas) {
        var nueva = semana(planNuevo);
        assertEquals(p.semana().size(), nueva.size());
        var puestos = new ArrayList<Long>();
        for (int i = 0; i < nueva.size(); i++) {
            var antes = p.semana().get(i);
            var despues = nueva.get(i);
            assertEquals(antes.fecha().plusWeeks(1), despues.fecha());
            assertEquals(antes.enfoque(), despues.enfoque());
            assertEquals(duracion(antes.id()), duracion(despues.id()), "la duracion declarada no cambia");
            assertEquals(antes.ejercicios().size(), despues.ejercicios().size());
            for (int j = 0; j < antes.ejercicios().size(); j++) {
                var e = antes.ejercicios().get(j);
                var n = despues.ejercicios().get(j);
                if (e.id() == quitado) {
                    assertNotEquals(quitado, n.id());
                    puestos.add(n.id());
                } else {
                    assertEquals(e, n, "la sesion " + antes.id() + " cambio fuera de la sustitucion");
                }
            }
        }
        assertFalse(puestos.isEmpty(), "premisa: el quitado estaba en la semana medida");

        var evidencia = intento(p);
        assertEquals(planNuevo, ((Number) evidencia.get("resulting_plan_id")).longValue());
        assertEquals("CONSERVED", evidencia.get("generation_source"));
        assertEquals(llamadas, llamadasIa(p));
        assertEquals(Math.max(1, llamadas), intentosDeGeneracion(planNuevo));
        var registradas = jdbc.queryForList("SELECT s->>'quitado' AS quitado, s->>'puesto' AS puesto,"
                + " s->>'motivo' AS motivo, s->>'regla' AS regla, s->>'sesion_origen' AS sesion,"
                + " s->>'dosis_nueva' AS dosis FROM plan_adaptation_attempts a,"
                + " jsonb_array_elements(a.conservation_report->'sustituciones') s"
                + " WHERE a.user_id = ? AND a.new_week_start_date = ?", p.userId(), SEMANA_NUEVA);
        assertEquals(puestos.size(), registradas.size(), String.valueOf(registradas));
        for (int k = 0; k < registradas.size(); k++) {
            var r = registradas.get(k);
            assertEquals(String.valueOf(quitado), r.get("quitado"));
            assertTrue(puestos.contains(Long.valueOf((String) r.get("puesto"))), String.valueOf(r));
            assertTrue(String.valueOf(r.get("motivo")).contains(motivo), String.valueOf(r));
            assertEquals("SUST-SEG-1", r.get("regla"));
            assertNotNull(r.get("sesion"));
            assertNotNull(r.get("dosis"));
        }
        return puestos;
    }

    private int duracion(long plannedWorkoutId) {
        return jdbc.queryForObject("SELECT expected_duration_minutes FROM planned_workouts"
                + " WHERE planned_workout_id = ?", Integer.class, plannedWorkoutId);
    }

    /** GAIN_MUSCLE: la dosis del sustituto es la de 4D para su clase y su zona. */
    private void assertDosis4D(long planNuevo, long ejercicio) {
        var zona = jdbc.queryForObject("SELECT bp.code FROM exercises x JOIN body_parts bp"
                + " ON bp.body_part_id = x.body_part_id WHERE x.exercise_id = ?", String.class, ejercicio);
        var esperada = MvpDosePolicy.dosisPara(MandatorySlot.ofMovementPattern(patron(ejercicio)), zona)
                .orElseThrow();
        var publicada = semana(planNuevo).stream().flatMap(s -> s.ejercicios().stream())
                .filter(e -> e.id() == ejercicio).findFirst().orElseThrow();
        assertEquals(esperada.series(), publicada.series());
        assertEquals(esperada.reps(), publicada.reps());
        assertEquals(esperada.descansoSegundos(), publicada.descanso());
    }

    /** La IA simulada: devuelve, para cada contexto, la correccion que construye el caso. */
    private void iaPropone(Function<PlanGenerationContext, List<CorrectedSession>> propuesta) {
        when(ia.interpretar(anyString(), any())).thenAnswer(inv -> propuesta.apply(inv.getArgument(1)));
    }

    /** Una sesion corregida: la original con UN ejercicio sustituido, duracion recalculada. */
    private CorrectedSession sustituir(PlanGenerationContext ctx, Sesion s, long quitado,
                                       PlanDraft.DraftExercise nuevo) {
        var ejercicios = s.ejercicios().stream()
                .map(e -> e.id() == quitado ? nuevo : e.borrador()).toList();
        var fecha = s.fecha().plusWeeks(1);
        int minutos = estimador.estimateMinutes(new PlanDraft.DraftWorkout(fecha,
                WorkoutFocus.valueOf(s.enfoque()), "x", 0, ejercicios), ctx.prescription());
        return new CorrectedSession(s.id(), fecha, minutos, ejercicios, "correccion simulada");
    }

    // =====================================================================
    // 0. Publicacion sin IA (defecto ck_plan_attempts, corregido)
    // =====================================================================

    /**
     * Una continuidad que no llama a la IA es un intento DETERMINISTA de
     * producir la semana: generation_attempts = 1, ai_calls = 0. Antes se
     * publicaba con generation_attempts = 0, ck_plan_attempts (V4) lo rechazaba
     * y la semana quedaba sin plan y sin evidencia.
     */
    @Nested
    @DisplayName("0 · publicar sin IA: la semana conservada se publica y la evidencia dice la verdad")
    class PublicacionSinIa {

        private void assertContinuidadSinIa(Participante p, long nuevo, String estado) {
            assertCopiaExacta(p, nuevo);
            assertEquals(1, intentosDeGeneracion(nuevo));
            var evidencia = intento(p);
            assertEquals(0, llamadasIa(p), "no se llamo a la IA y la evidencia no puede decir otra cosa");
            assertEquals(estado, evidencia.get("status"));
            assertEquals("CONSERVED", evidencia.get("generation_source"));
            assertEquals(nuevo, ((Number) evidencia.get("resulting_plan_id")).longValue());
            verify(ia, never()).pedirRespuesta(any(), any(), any(), any(), anyInt());
        }

        @Test
        @DisplayName("NO_CHANGE: copia exacta, generation_attempts = 1, ai_calls = 0")
        void noChangePublicaLaCopiaExacta() {
            var p = participante(GoalType.GAIN_MUSCLE, true);
            long nuevo = adaptar(p, AdaptationDecision.NO_CHANGE, List.of())
                    .orElseThrow(() -> new AssertionError("NO_CHANGE no publico: " + intento(p)));
            assertContinuidadSinIa(p, nuevo, "SIN_ORDEN");
        }

        @Test
        @DisplayName("INSUFFICIENT_DATA: conserva y publica sin IA")
        void insufficientDataPublicaSinIa() {
            var p = participante(GoalType.GAIN_MUSCLE, true);
            long nuevo = adaptar(p, AdaptationDecision.INSUFFICIENT_DATA, List.of())
                    .orElseThrow(() -> new AssertionError("INSUFFICIENT_DATA no publico: " + intento(p)));
            assertContinuidadSinIa(p, nuevo, "SIN_ORDEN");
        }

        @Test
        @DisplayName("IA desactivada con orden de adaptar: publica el respaldo valido sin pedir nada")
        void iaDesactivadaPublicaElRespaldo() {
            var p = participante(GoalType.GAIN_MUSCLE, true);
            when(ia.isEnabled()).thenReturn(false);
            long nuevo = adaptar(p, AdaptationDecision.ADAPT_LOCALIZED, List.of(new PlanAdjustment.TargetedSession(
                    p.semana().get(0).id(), TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA)))
                    .orElseThrow(() -> new AssertionError("sin respaldo: " + intento(p)));
            assertContinuidadSinIa(p, nuevo, "RESPALDO");
        }

        @Test
        @DisplayName("fallo real de publicacion: rollback, ni plan ni evidencia a medias")
        void falloDePublicacionHaceRollback() {
            var p = participante(GoalType.GAIN_MUSCLE, true);
            var medidaAntes = semana(p.planMedido());
            var inexistente = new Participante(p.userId(), p.planMedido(), 999_999_999L, p.semana());

            // fk_paa_metric no es diferida: la evidencia no se puede insertar y
            // la transaccion de publicacion entera tiene que deshacerse.
            assertThrows(RuntimeException.class,
                    () -> adaptar(inexistente, AdaptationDecision.NO_CHANGE, List.of()));

            assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans"
                    + " WHERE user_id = ? AND week_start_date = ?", Integer.class, p.userId(), SEMANA_NUEVA),
                    "quedo un plan sin su evidencia");
            assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM plan_adaptation_attempts WHERE user_id = ?",
                    Integer.class, p.userId()), "quedo evidencia sin su plan");
            assertEquals(medidaAntes, semana(p.planMedido()), "la semana medida no se toca");
        }
    }

    /**
     * Cortacircuitos abierto. Va en su propia clase y con contexto descartado al
     * terminar: el cortacircuitos vive en el bean AdaptedWeekCommandService y,
     * abierto, cambiaria el comportamiento de las demas pruebas durante diez
     * minutos.
     */
    @Nested
    @org.springframework.test.annotation.DirtiesContext(
            classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
    @DisplayName("0b · cortacircuitos abierto: publica el respaldo sin invocar Replicate")
    class CortacircuitosAbierto {

        @Test
        void conElCortacircuitosAbiertoSePublicaElRespaldoSinLlamar() {
            when(ia.pedirRespuesta(any(), any(), any(), any(), anyInt()))
                    .thenThrow(new main.web.services.fitsense.planning.domain.exceptions
                            .AiProviderUnavailableException("proveedor caido (simulado)"));

            // Tres caidas seguidas abren el cortacircuitos. Cada una ya publica
            // su respaldo, con la llamada fallida contada.
            for (int i = 0; i < main.web.services.fitsense.planning.infrastructure.generation.ai
                    .AirCircuitBreaker.UMBRAL; i++) {
                var q = participante(GoalType.GAIN_MUSCLE, true);
                long plan = adaptar(q, AdaptationDecision.ADAPT_LOCALIZED, List.of(new PlanAdjustment.TargetedSession(
                        q.semana().get(0).id(), TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA)))
                        .orElseThrow(() -> new AssertionError("sin respaldo con el proveedor caido"));
                assertEquals(1, llamadasIa(q));
                assertEquals(1, intentosDeGeneracion(plan));
            }
            verify(ia, times(3)).pedirRespuesta(any(), any(), any(), any(), anyInt());

            // Abierto: la siguiente adaptacion no pide nada y publica el respaldo.
            var p = participante(GoalType.GAIN_MUSCLE, true);
            long nuevo = adaptar(p, AdaptationDecision.ADAPT_LOCALIZED, List.of(new PlanAdjustment.TargetedSession(
                    p.semana().get(0).id(), TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA)))
                    .orElseThrow(() -> new AssertionError("sin respaldo con el cortacircuitos abierto"));
            verify(ia, times(3)).pedirRespuesta(any(), any(), any(), any(), anyInt());
            assertCopiaExacta(p, nuevo);
            assertEquals(0, llamadasIa(p));
            assertEquals(1, intentosDeGeneracion(nuevo));
            assertEquals("RESPALDO", intento(p).get("status"));
        }
    }

    // =====================================================================
    // 0c. INC-CAT-1374: una semana existente que ya lo contiene
    // =====================================================================

    /**
     * Diagnostico de P2-D con la contencion de INC-CAT-1374 activa. El 1374 se
     * inyecta en la semana medida como si se hubiera generado ANTES de la
     * contencion, en lugar de un relleno de cintura de la misma clase de dosis
     * -asi V15, V16 y V24 no cambian-, y se pide la continuidad.
     */
    @Nested
    @DisplayName("0c · INC-CAT-1374: semanas que ya contienen el 1374")
    class Contencion1374 {

        /** Sustituye en la semana medida un relleno de cintura por el 1374. */
        private Participante con1374(Participante p) {
            var relleno = p.semana().stream()
                    .filter(s -> WorkoutFocus.valueOf(s.enfoque()).accepts("waist")
                            || WorkoutFocus.valueOf(s.enfoque()) == WorkoutFocus.FULL_BODY)
                    .flatMap(s -> s.ejercicios().stream())
                    .filter(e -> "SETS_REPS".equals(e.tipo()) && !esSlot(patron(e.id())))
                    // De cintura si lo hay (la zona del 1374); si no, cualquier
                    // relleno: con 4D v3.1 la semana medida puede no traer uno de
                    // cintura, y en FULL_BODY V15 no rechaza otra zona.
                    .sorted(Comparator.comparing((Ejercicio e) -> !"waist".equals(jdbc.queryForObject(
                            "SELECT bp.code FROM exercises x JOIN body_parts bp"
                            + " ON bp.body_part_id = x.body_part_id WHERE x.exercise_id = ?", String.class, e.id()))))
                    .findFirst().orElseThrow(() -> new AssertionError("premisa: sin relleno"));
            assertFalse(p.ids().contains(1374L));
            jdbc.update("UPDATE planned_workout_exercises SET exercise_id = 1374 WHERE exercise_id = ?"
                    + " AND planned_workout_id IN (SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ?)",
                    relleno.id(), p.planMedido());
            return new Participante(p.userId(), p.planMedido(), p.metrica(), semana(p.planMedido()));
        }

        @Test
        @DisplayName("sin barra: el 1374 se sustituye por un relleno elegible; el resto de la semana, identico")
        void sinBarraSeSustituye() {
            var p = con1374(participante(GoalType.GAIN_MUSCLE, true));
            var medida = semana(p.planMedido());

            long nuevo = adaptar(p, AdaptationDecision.NO_CHANGE, List.of())
                    .orElseThrow(() -> new AssertionError("no se publico: " + intento(p)));
            var puestos = assertSustitucionSegura(p, nuevo, 1374L, "INC-CAT-1374");
            for (long puesto : puestos) {
                assertFalse(esSlot(patron(puesto)), "un relleno se sustituye por un relleno");
                assertDosis4D(nuevo, puesto);
            }
            assertEquals(medida, semana(p.planMedido()), "la semana medida no se toca");
            verifyNoInteractions(ia);
        }

        @Test
        @DisplayName("brazos arriba prohibidos, aun con barra: tambien se sustituye")
        void sinBrazosArribaSeSustituye() {
            var con = new ArrayList<>(EQUIPAMIENTO);
            con.add("pull-up bar");
            var p = con1374(participante(GoalType.GAIN_MUSCLE, true, con, false));
            long nuevo = adaptar(p, AdaptationDecision.NO_CHANGE, List.of())
                    .orElseThrow(() -> new AssertionError("no se publico: " + intento(p)));
            for (long puesto : assertSustitucionSegura(p, nuevo, 1374L, "INC-CAT-1374"))
                assertNotEquals(Boolean.TRUE, jdbc.queryForObject(
                        "SELECT requires_overhead FROM exercises WHERE exercise_id = ?", Boolean.class, puesto));
        }

        @Test
        @DisplayName("con barra y sin restricciones: se conserva la copia exacta, con el 1374")
        void conBarraSeConserva() {
            var con = new ArrayList<>(EQUIPAMIENTO);
            con.add("pull-up bar");
            var p = con1374(participante(GoalType.GAIN_MUSCLE, true, con, true));
            long nuevo = adaptar(p, AdaptationDecision.NO_CHANGE, List.of())
                    .orElseThrow(() -> new AssertionError("no se conservo: " + intento(p)));
            assertCopiaExacta(p, nuevo);
            assertTrue(semana(nuevo).stream().flatMap(s -> s.ejercicios().stream()).anyMatch(e -> e.id() == 1374L));
            assertEquals(1, intentosDeGeneracion(nuevo));
            assertEquals(0, llamadasIa(p));
        }
    }

    // =====================================================================
    // 1. Reclasificacion
    // =====================================================================

    @Nested
    @DisplayName("1 · una reclasificacion no altera en silencio una sesion conservada")
    class Reclasificacion {

        @Test
        @DisplayName("control: sin cambios de catalogo y sin propuesta de la IA, se publica una copia exacta")
        void sinCambiosSePublicaLaCopiaExacta() {
            var p = participante(GoalType.GAIN_MUSCLE, true);
            iaPropone(ctx -> List.of());
            long nuevo = adaptar(p, AdaptationDecision.ADAPT_LOCALIZED, List.of(new PlanAdjustment.TargetedSession(
                    p.semana().get(0).id(), TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA)))
                    .orElseThrow(() -> new AssertionError("el control no paso: " + intento(p)));
            assertCopiaExacta(p, nuevo);
            assertEquals("CONSERVED", intento(p).get("generation_source"));
            // IA llamada una vez: el numero real, en el plan y en la evidencia.
            assertEquals(1, llamadasIa(p));
            assertEquals(1, intentosDeGeneracion(nuevo));
        }

        @Test
        @DisplayName("relleno reclasificado como estructural: la dosis conservada ya no cumple 4D y se rechaza con su id")
        void rellenoReclasificadoComoEstructural() {
            var p = participante(GoalType.GAIN_MUSCLE, true);
            long relleno = p.semana().stream().flatMap(s -> s.ejercicios().stream())
                    .filter(e -> "SETS_REPS".equals(e.tipo()) && !esSlot(patron(e.id())))
                    .map(Ejercicio::id).findFirst()
                    .orElseThrow(() -> new AssertionError("la semana medida no tiene relleno"));
            var dosisAntes = semana(p.planMedido());

            // Simula una correccion futura de datos, solo en esta base efimera.
            jdbc.update("UPDATE exercises SET movement_pattern = 'HIP_HINGE' WHERE exercise_id = ?", relleno);

            var publicado = adaptar(p, AdaptationDecision.NO_CHANGE, List.of());
            assertTrue(publicado.isEmpty(), "se publico una sesion conservada con dosis que ya no corresponde");
            var evidencia = intento(p);
            assertEquals("NO_FACTIBLE", evidencia.get("status"));
            assertNull(evidencia.get("resulting_plan_id"));
            assertTrue(String.valueOf(evidencia.get("rejection_reason")).contains("V24: el ejercicio " + relleno),
                    String.valueOf(evidencia.get("rejection_reason")));
            assertEquals(dosisAntes, semana(p.planMedido()), "la semana medida no se toca");
        }

        @Test
        @DisplayName("reclasificado con una restriccion de seguridad vigente: se sustituye SOLO ese ejercicio, por uno de su slot")
        void reclasificadoConRestriccionDeSeguridad() {
            var p = participante(GoalType.GAIN_MUSCLE, false);
            var afectado = p.semana().get(0).ejercicios().get(0);
            jdbc.update("UPDATE exercises SET requires_floor = TRUE WHERE exercise_id = ?", afectado.id());
            try {
                long nuevo = adaptar(p, AdaptationDecision.NO_CHANGE, List.of())
                        .orElseThrow(() -> new AssertionError("no se publico: " + intento(p)));
                for (long puesto : assertSustitucionSegura(p, nuevo, afectado.id(), "suelo")) {
                    assertEquals(MandatorySlot.ofMovementPattern(patron(afectado.id())),
                            MandatorySlot.ofMovementPattern(patron(puesto)), "mismo slot");
                    assertNotEquals(Boolean.TRUE, jdbc.queryForObject(
                            "SELECT requires_floor FROM exercises WHERE exercise_id = ?", Boolean.class, puesto));
                    assertDosis4D(nuevo, puesto);
                }
            } finally {
                jdbc.update("UPDATE exercises SET requires_floor = FALSE WHERE exercise_id = ?", afectado.id());
            }
        }

        @Test
        @DisplayName("con la IA corrigiendo otra sesion: la sustitucion por seguridad tambien se aplica, y nada mas")
        void sustitucionConIaSinPropuesta() {
            var p = participante(GoalType.GAIN_MUSCLE, false);
            var afectado = p.semana().get(1).ejercicios().get(0);
            iaPropone(ctx -> List.of());
            jdbc.update("UPDATE exercises SET requires_floor = TRUE WHERE exercise_id = ?", afectado.id());
            try {
                long nuevo = adaptar(p, AdaptationDecision.ADAPT_LOCALIZED, List.of(new PlanAdjustment.TargetedSession(
                        p.semana().get(0).id(), TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA)))
                        .orElseThrow(() -> new AssertionError("no se publico: " + intento(p)));
                assertSustitucionSegura(p, nuevo, afectado.id(), "suelo", 1);
            } finally {
                jdbc.update("UPDATE exercises SET requires_floor = FALSE WHERE exercise_id = ?", afectado.id());
            }
        }

        @Test
        @DisplayName("sin sustituto seguro en todo el slot: NO_FACTIBLE con el motivo de seguridad; nada se publica")
        void sinSustitutoSeguroNoFactible() {
            var p = participante(GoalType.GAIN_MUSCLE, false);
            var afectado = p.semana().get(0).ejercicios().stream().filter(e -> esSlot(patron(e.id())))
                    .findFirst().orElseThrow();
            var slot = MandatorySlot.ofMovementPattern(patron(afectado.id())).orElseThrow();
            var patrones = jdbc.queryForList("SELECT DISTINCT movement_pattern FROM exercises"
                    + " WHERE movement_pattern IS NOT NULL", String.class).stream()
                    .filter(x -> MandatorySlot.ofMovementPattern(x).equals(Optional.of(slot))).toList();
            var tocados = jdbc.queryForList("SELECT exercise_id FROM exercises WHERE movement_pattern = ANY(?)"
                    + " AND requires_floor IS DISTINCT FROM TRUE", Long.class,
                    (Object) patrones.toArray(String[]::new));
            var medida = semana(p.planMedido());
            jdbc.update("UPDATE exercises SET requires_floor = TRUE WHERE exercise_id = ANY(?)",
                    (Object) tocados.toArray(Long[]::new));
            try {
                assertTrue(adaptar(p, AdaptationDecision.NO_CHANGE, List.of()).isEmpty(),
                        "se publico una semana sin sustituto seguro");
                var evidencia = intento(p);
                assertEquals("NO_FACTIBLE", evidencia.get("status"));
                assertNull(evidencia.get("resulting_plan_id"));
                var motivo = String.valueOf(evidencia.get("rejection_reason"));
                assertTrue(motivo.startsWith("SEGURIDAD: el ejercicio "), motivo);
                assertTrue(motivo.contains("SUST-SEG-1") && motivo.contains("suelo"), motivo);
                assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans"
                        + " WHERE user_id = ? AND week_start_date = ?", Integer.class, p.userId(), SEMANA_NUEVA));
                assertEquals(medida, semana(p.planMedido()), "la semana medida no se toca");
            } finally {
                jdbc.update("UPDATE exercises SET requires_floor = FALSE WHERE exercise_id = ANY(?)",
                        (Object) tocados.toArray(Long[]::new));
            }
        }
    }

    // =====================================================================
    // 2. Complementos
    // =====================================================================

    @Nested
    @DisplayName("2 · complementos de cardio y movilidad en la adaptacion")
    class Complementos {

        /**
         * Sesiones senaladas por SEGURIDAD_ANTE_DOLOR sin ejercicio identificado:
         * V8C exige que no reaparezca NINGUNO de sus ejercicios, asi que cada una
         * se reemplaza entera. Cada posicion no forzada recibe una alternativa de
         * la misma zona, el mismo tipo y la misma clase de slot -prefiriendo el
         * mismo patron y musculo- para que V15, V16, V17, V22 y V23 sigan como
         * estaban.
         */
        private List<CorrectedSession> reemplazar(PlanGenerationContext ctx, Participante p,
                                                  Map<Long, Map<Long, CandidateExercise>> forzadosPorSesion) {
            var usados = new HashSet<>(p.ids());
            forzadosPorSesion.values().forEach(m -> m.values().forEach(c -> usados.add(c.exerciseId())));
            var porId = ctx.availableExercises().stream()
                    .collect(Collectors.toMap(CandidateExercise::exerciseId, c -> c, (a, b) -> a));
            var correcciones = new ArrayList<CorrectedSession>();
            for (var s : p.semana()) {
                var forzados = forzadosPorSesion.get(s.id());
                if (forzados == null) continue;
                var nuevos = new ArrayList<PlanDraft.DraftExercise>();
                for (var e : s.ejercicios()) {
                    var c = forzados.get(e.id());
                    if (c == null) {
                        var o = porId.get(e.id());
                        var slot = MandatorySlot.of(o);
                        var enfoque = WorkoutFocus.valueOf(s.enfoque());
                        // Primero la misma zona; si no hay, otra zona del enfoque.
                        c = ctx.availableExercises().stream()
                                .filter(x -> enfoque.accepts(x.bodyPartCode())
                                        || Objects.equals(x.bodyPartCode(), o.bodyPartCode()))
                                .filter(x -> x.defaultPrescription() == o.defaultPrescription())
                                .filter(x -> MandatorySlot.of(x).equals(slot))
                                .filter(x -> !usados.contains(x.exerciseId()))
                                .filter(x -> !GoalFeasibilityPolicy.enRevision(x))
                                .filter(x -> !CatalogEligibilityPolicy.esComplemento(x, ctx.profile().goalType()))
                                .filter(x -> !"upper arms".equals(o.bodyPartCode())
                                        || Objects.equals(x.targetMuscle(), o.targetMuscle()))
                                .sorted(Comparator.comparing((CandidateExercise x) ->
                                                !Objects.equals(x.bodyPartCode(), o.bodyPartCode()))
                                        // Si hay que cambiar de zona, la menos representada en la sesion
                                        // nueva, como haria la IA para respetar V15 (desde V37, 142 ya no
                                        // tiene alternativa de su zona: 776 esta inactivo).
                                        .thenComparing(x -> nuevos.stream().filter(n -> Objects.equals(
                                                porId.get(n.exerciseId()) == null ? null
                                                        : porId.get(n.exerciseId()).bodyPartCode(),
                                                x.bodyPartCode())).count())
                                        .thenComparing(x -> !Objects.equals(x.movementPattern(), o.movementPattern()))
                                        .thenComparing(x -> !Objects.equals(x.targetMuscle(), o.targetMuscle())))
                                .findFirst().orElseThrow(() -> new AssertionError(
                                        "premisa: sin alternativa para " + e.id() + " " + o));
                        usados.add(c.exerciseId());
                    }
                    nuevos.add(dosis(c, e));
                }
                var fecha = s.fecha().plusWeeks(1);
                int minutos = estimador.estimateMinutes(new PlanDraft.DraftWorkout(fecha,
                        WorkoutFocus.valueOf(s.enfoque()), "x", 0, nuevos), ctx.prescription());
                correcciones.add(new CorrectedSession(s.id(), fecha, minutos, nuevos, "correccion simulada"));
            }
            return correcciones;
        }

        /** La dosis de la posicion sustituida, en el tipo que exige el catalogo (V7). */
        private PlanDraft.DraftExercise dosis(CandidateExercise c, Ejercicio e) {
            if (c.defaultPrescription() == PrescriptionType.DURATION) {
                int segundos = e.segundos() != null ? e.segundos()
                        : Math.max(20, (e.reps() == null ? 10 : e.reps()) * 3);
                return new PlanDraft.DraftExercise(c.exerciseId(), PrescriptionType.DURATION, e.series(), null,
                        segundos, null, e.descanso(), null);
            }
            return new PlanDraft.DraftExercise(c.exerciseId(), PrescriptionType.SETS_REPS, e.series(),
                    e.reps() == null ? (short) 12 : e.reps(), null, null, e.descanso(), null);
        }

        private CandidateExercise cardio(Participante p, PlanGenerationContext ctx) {
            return cardios(p, ctx, 1).get(0);
        }

        private List<CandidateExercise> cardios(Participante p, PlanGenerationContext ctx, int n) {
            var lista = ctx.availableExercises().stream()
                    .filter(x -> CatalogEligibilityPolicy.esComplemento(x, ctx.profile().goalType()))
                    .filter(x -> "LOCOMOTION_CARDIO".equals(x.movementPattern()))
                    .filter(x -> !p.ids().contains(x.exerciseId()))
                    .limit(n).toList();
            if (lista.size() < n) throw new AssertionError("premisa: solo " + lista.size() + " cardios complementarios");
            return lista;
        }

        /** Colocaciones por slot de la semana medida: slot -> [(sesion, ejercicio)]. */
        private Map<MandatorySlot, List<long[]>> porSlot(Participante p) {
            var mapa = new EnumMap<MandatorySlot, List<long[]>>(MandatorySlot.class);
            for (var s : p.semana())
                for (var e : s.ejercicios())
                    MandatorySlot.ofMovementPattern(patron(e.id()))
                            .ifPresent(sl -> mapa.computeIfAbsent(sl, k -> new ArrayList<>()).add(new long[]{s.id(), e.id()}));
            return mapa;
        }

        private boolean esRelleno(Ejercicio e) {
            return "SETS_REPS".equals(e.tipo()) && !esSlot(patron(e.id()));
        }

        /** SETS_REPS estructural cuyo slot aparece al menos dos veces en la semana medida. */
        private boolean slotRepetido(Participante p, Ejercicio e) {
            if (!"SETS_REPS".equals(e.tipo())) return false;
            var slot = MandatorySlot.ofMovementPattern(patron(e.id()));
            if (slot.isEmpty()) return false;
            long n = p.semana().stream().flatMap(s -> s.ejercicios().stream())
                    .filter(x -> MandatorySlot.ofMovementPattern(patron(x.id())).equals(slot)).count();
            return n >= 2;
        }

        /**
         * Para cada slot que falte en la semana medida, una posicion de relleno
         * libre en una sesion cuyo enfoque admita ese slot, y el ejercicio que la
         * ocupa. Asi V23 solo puede quejarse de lo que el caso provoca.
         */
        private void cubrirSlotsQueFaltan(PlanGenerationContext ctx, Participante p,
                                          Map<Long, Map<Long, CandidateExercise>> forzados) {
            var presentes = porSlot(p).keySet();
            for (var slot : MandatorySlot.ALL) {
                if (presentes.contains(slot)) continue;
                boolean cubierto = false;
                for (var s : p.semana()) {
                    var enfoque = WorkoutFocus.valueOf(s.enfoque());
                    var yaForzados = forzados.getOrDefault(s.id(), Map.of());
                    var hueco = s.ejercicios().stream().filter(this::esRelleno)
                            .filter(e -> !yaForzados.containsKey(e.id())).findFirst();
                    var ejercicio = ctx.availableExercises().stream()
                            .filter(x -> MandatorySlot.of(x).filter(slot::equals).isPresent())
                            .filter(x -> enfoque.accepts(x.bodyPartCode()))
                            .filter(x -> x.defaultPrescription() == PrescriptionType.SETS_REPS)
                            .filter(x -> !p.ids().contains(x.exerciseId()))
                            .filter(x -> !GoalFeasibilityPolicy.enRevision(x))
                            .findFirst();
                    if (hueco.isPresent() && ejercicio.isPresent()) {
                        forzados.computeIfAbsent(s.id(), k -> new HashMap<>()).put(hueco.get().id(), ejercicio.get());
                        cubierto = true;
                        break;
                    }
                }
                if (!cubierto) throw new AssertionError("premisa: no hay donde cubrir " + slot);
            }
        }

        private List<PlanAdjustment.TargetedSession> senaladas(Map<Long, Map<Long, CandidateExercise>> forzados) {
            return forzados.keySet().stream().sorted()
                    .map(id -> new PlanAdjustment.TargetedSession(id, TipoIntervencion.SEGURIDAD_ANTE_DOLOR)).toList();
        }

        @Test
        @DisplayName("GENERAL_FITNESS: un cardio en una sesion corregida se acepta y se publica; las no senaladas se conservan")
        void cardioEnLaSesionCorregidaSePublica() {
            var p = participante(GoalType.GENERAL_FITNESS, true);
            // Desde 4D v4 la semana de GENERAL_FITNESS puede no tener relleno
            // (todo son colocaciones de capacidad): vale cualquier colocacion de
            // trabajo SETS_REPS cuyo slot este cubierto por otra en la semana, asi
            // que V23 sigue sin poder quejarse por lo que el caso provoca.
            var reemplazable = (java.util.function.Predicate<Ejercicio>) e -> esRelleno(e) || slotRepetido(p, e);
            var sesion = p.semana().stream().filter(s -> s.ejercicios().stream().anyMatch(reemplazable))
                    .findFirst().orElseThrow(() -> new AssertionError("premisa: sin sesion con relleno"));
            var relleno = sesion.ejercicios().stream().filter(reemplazable).findFirst().orElseThrow();
            var elegido = new long[1];
            iaPropone(ctx -> {
                var forzados = new HashMap<Long, Map<Long, CandidateExercise>>();
                var c = cardio(p, ctx);
                elegido[0] = c.exerciseId();
                forzados.computeIfAbsent(sesion.id(), k -> new HashMap<>()).put(relleno.id(), c);
                cubrirSlotsQueFaltan(ctx, p, forzados);
                return reemplazar(ctx, p, forzados);
            });
            // La orden se calcula sin contexto con la misma regla que usa la IA simulada.
            var orden = ordenPara(p, sesion.id(), relleno.id());

            long nuevo = adaptar(p, AdaptationDecision.ADAPT_LOCALIZED, orden)
                    .orElseThrow(() -> new AssertionError("rechazado: " + intento(p)));

            assertEquals("AI", intento(p).get("generation_source"));
            var publicada = semana(nuevo);
            var senaladasIds = orden.stream().map(PlanAdjustment.TargetedSession::plannedWorkoutId).toList();
            for (int i = 0; i < publicada.size(); i++) {
                var anterior = p.semana().get(i);
                var ids = publicada.get(i).ejercicios().stream().map(Ejercicio::id).toList();
                if (anterior.id() == sesion.id())
                    assertTrue(ids.contains(elegido[0]), "el cardio no esta en la sesion corregida");
                if (senaladasIds.contains(anterior.id()))
                    assertTrue(Collections.disjoint(ids, anterior.ejercicios().stream().map(Ejercicio::id).toList()),
                            "reaparece un ejercicio de una sesion con dolor");
                else
                    assertEquals(anterior.ejercicios(), publicada.get(i).ejercicios(), "una sesion no senalada cambio");
            }
        }

        /**
         * Las sesiones que la correccion va a tocar, calculadas sin contexto: la
         * del caso y, por cada slot que falte, la primera con relleno libre cuyo
         * enfoque admita una zona de ese slot.
         */
        private List<PlanAdjustment.TargetedSession> ordenPara(Participante p, long sesionDelCaso, long posicionDelCaso) {
            return ordenPara(p, Set.of(sesionDelCaso), Set.of(posicionDelCaso));
        }

        private List<PlanAdjustment.TargetedSession> ordenPara(Participante p, Set<Long> sesionesDelCaso,
                                                               Set<Long> posicionesDelCaso) {
            var ids = new TreeSet<Long>(sesionesDelCaso);
            var presentes = porSlot(p).keySet();
            var zonasDeSlot = Map.of(MandatorySlot.LOWER_KNEE, List.of("upper legs"),
                    MandatorySlot.LOWER_HIP, List.of("upper legs", "back"),
                    MandatorySlot.UPPER_PUSH, List.of("chest", "shoulders", "upper arms"),
                    MandatorySlot.UPPER_PULL, List.of("back", "upper arms"));
            var ocupadas = new HashSet<>(posicionesDelCaso);
            for (var slot : MandatorySlot.ALL) {
                if (presentes.contains(slot)) continue;
                for (var s : p.semana()) {
                    var enfoque = WorkoutFocus.valueOf(s.enfoque());
                    var hueco = s.ejercicios().stream().filter(this::esRelleno)
                            .filter(e -> !ocupadas.contains(e.id())).findFirst();
                    if (hueco.isPresent() && zonasDeSlot.get(slot).stream().anyMatch(enfoque::accepts)) {
                        ids.add(s.id());
                        ocupadas.add(hueco.get().id());
                        break;
                    }
                }
            }
            return ids.stream().map(id -> new PlanAdjustment.TargetedSession(id, TipoIntervencion.SEGURIDAD_ANTE_DOLOR)).toList();
        }

        @Test
        @DisplayName("un cardio no pasa por estructural: si ocupa todas las colocaciones de un slot, V23 lo rechaza por ese slot")
        void cardioNoSustituyeAlTrabajoEstructural() {
            var p = participante(GoalType.GENERAL_FITNESS, true);
            var menor = porSlot(p).entrySet().stream().min(Comparator.comparingInt(e -> e.getValue().size()))
                    .orElseThrow(() -> new AssertionError("premisa: la semana no tiene slots"));
            var desplazado = menor.getKey();
            var colocaciones = menor.getValue();
            var sesiones = colocaciones.stream().map(c -> c[0]).collect(Collectors.toSet());
            var posiciones = colocaciones.stream().map(c -> c[1]).collect(Collectors.toSet());
            iaPropone(ctx -> {
                var forzados = new HashMap<Long, Map<Long, CandidateExercise>>();
                var cs = cardios(p, ctx, colocaciones.size());
                for (int i = 0; i < colocaciones.size(); i++)
                    forzados.computeIfAbsent(colocaciones.get(i)[0], k -> new HashMap<>())
                            .put(colocaciones.get(i)[1], cs.get(i));
                cubrirSlotsQueFaltan(ctx, p, forzados);
                return reemplazar(ctx, p, forzados);
            });

            var publicado = adaptar(p, AdaptationDecision.ADAPT_LOCALIZED, ordenPara(p, sesiones, posiciones));

            var feedback = ArgumentCaptor.forClass(AiRetryFeedback.class);
            verify(ia, times(3)).pedirRespuesta(any(), any(), any(), feedback.capture(), anyInt());
            var motivos = feedback.getAllValues().get(2).toString();
            assertTrue(motivos.contains("V23: la semana no cubre " + desplazado + "."), motivos);
            assertEquals("NO_FACTIBLE", intento(p).get("status"));
            // P-03 (31/10/2026): la propuesta de la IA nunca se publica. Si se publica algo es la regeneracion con el
            // motor de reglas en el mismo ciclo, registrada en safe_regeneration_attempts y validada entera.
            publicado.ifPresent(plan -> {
                assertEquals("RULE_ENGINE", jdbc.queryForObject("SELECT generation_source FROM weekly_training_plans"
                        + " WHERE plan_id = ?", String.class, plan));
                assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM safe_regeneration_attempts WHERE"
                        + " resulting_plan_id = ? AND outcome = 'PUBLICADO'", Integer.class, plan));
            });
            assertEquals(3, ((Number) intento(p).get("ai_calls")).intValue());
        }
    }

    // =====================================================================
    // 3. Bloqueos individuales
    // =====================================================================

    @Nested
    @DisplayName("3 · un ejercicio bloqueado por el participante")
    class Bloqueos {

        /** Sustituto del mismo patron, zona y dificultad permitida, fuera de la semana. */
        private long sustitutoDe(Participante p, long ejercicio) {
            var fila = jdbc.queryForMap("SELECT movement_pattern, body_part_id FROM exercises WHERE exercise_id = ?",
                    ejercicio);
            var candidatos = jdbc.queryForList("SELECT exercise_id FROM exercises WHERE is_active"
                            + " AND movement_pattern = ? AND body_part_id = ? AND difficulty_level <= 2"
                            + " AND exercise_id <> ? ORDER BY exercise_id", Long.class,
                    fila.get("movement_pattern"), fila.get("body_part_id"), ejercicio);
            // Solo los que el participante puede recibir de verdad (equipamiento, dificultad y
            // restricciones), con el mismo conjunto que vera la IA: desde V39, por ejemplo, 301
            // exige espaldera y ya no es un sustituto valido para este perfil.
            var elegibles = planComandos.prepare(new main.web.services.fitsense.planning.domain.model.commands
                            .GenerateWeeklyPlanCommand(p.userId(), SEMANA_MEDIDA, null, true, null, null))
                    .context().availableExercises().stream()
                    .map(main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise::exerciseId)
                    .collect(Collectors.toSet());
            return candidatos.stream().filter(id -> !p.ids().contains(id)).filter(elegibles::contains).findFirst()
                    .orElseThrow(() -> new AssertionError("sin sustituto del mismo patron"));
        }

        private boolean tieneSustituto(Participante p, long ejercicio) {
            try {
                sustitutoDe(p, ejercicio);
                return true;
            } catch (AssertionError e) {
                return false;
            }
        }

        private Optional<Long> sustituirConVariante(Participante p, Sesion sesion, Ejercicio x, long z) {
            iaPropone(ctx -> {
                var nuevo = new PlanDraft.DraftExercise(z, PrescriptionType.SETS_REPS, x.series(), x.reps(),
                        null, null, x.descanso(), null);
                return List.of(sustituir(ctx, sesion, x.id(), nuevo));
            });
            return adaptar(p, AdaptationDecision.ADAPT_LOCALIZED, List.of(new PlanAdjustment.TargetedSession(
                    sesion.id(), TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA)));
        }

        /** El primer estructural de la sesion que tiene un sustituto elegible del mismo patron y zona. */
        private Ejercicio estructuralConSustituto(Participante p, Sesion sesion) {
            return sesion.ejercicios().stream().filter(e -> "SETS_REPS".equals(e.tipo()) && esSlot(patron(e.id())))
                    .filter(e -> tieneSustituto(p, e.id())).findFirst()
                    .orElseThrow(() -> new AssertionError("premisa: ningun estructural con sustituto"));
        }


        /** El elegible para la sustitucion, comprobado con el mismo contexto que vera la IA. */
        private boolean elegible(Participante p, long id) {
            var bloqueados = jdbc.queryForObject("SELECT blocked_exercise_ids::text FROM user_profiles WHERE user_id = ?",
                    String.class, p.userId());
            return !bloqueados.contains(String.valueOf(id));
        }

        @Test
        @DisplayName("control: la misma variante, sin bloqueo, se publica")
        void sinBloqueoLaVarianteSePublica() {
            var p = participante(GoalType.GAIN_MUSCLE, true);
            var sesion = p.semana().get(0);
            // Premisa (V37): el primer estructural de la sesion que tiene sustituto del mismo patron.
            // Con 776 inactivo, 142 (back/HIP_HINGE) ya no tiene alternativa de su misma zona.
            var x = estructuralConSustituto(p, sesion);
            long z = sustitutoDe(p, x.id());

            long nuevo = sustituirConVariante(p, sesion, x, z)
                    .orElseThrow(() -> new AssertionError("el control no paso: " + intento(p)));
            assertTrue(semana(nuevo).get(0).ejercicios().stream().anyMatch(e -> e.id() == z));
            assertEquals("AI", intento(p).get("generation_source"));
        }

        @Test
        @DisplayName("bloqueado: la IA lo propone, no entra en la adaptacion y se conserva la semana")
        void unBloqueadoNoEntraEnUnaAdaptacionNueva() {
            var p = participante(GoalType.GAIN_MUSCLE, true);
            var sesion = p.semana().get(0);
            var x = estructuralConSustituto(p, sesion);
            long z = sustitutoDe(p, x.id());
            jdbc.update("UPDATE user_profiles SET blocked_exercise_ids = ARRAY[?]::bigint[] WHERE user_id = ?",
                    z, p.userId());
            assertFalse(elegible(p, z));

            var publicado = sustituirConVariante(p, sesion, x, z);

            var feedback = ArgumentCaptor.forClass(AiRetryFeedback.class);
            verify(ia, times(3)).pedirRespuesta(any(), any(), any(), feedback.capture(), anyInt());
            assertTrue(feedback.getAllValues().get(2).toString().contains("V1: el ejercicio " + z),
                    feedback.getAllValues().get(2).toString());
            long nuevo = publicado.orElseThrow(() -> new AssertionError("sin respaldo: " + intento(p)));
            assertFalse(semana(nuevo).stream().flatMap(s -> s.ejercicios().stream()).anyMatch(e -> e.id() == z),
                    "el ejercicio bloqueado se publico");
            assertCopiaExacta(p, nuevo);
            assertEquals("CONSERVED", intento(p).get("generation_source"));
            // IA llamada tres veces: el numero real, en el plan y en la evidencia.
            assertEquals(3, llamadasIa(p));
            assertEquals(3, intentosDeGeneracion(nuevo));
        }

        @Test
        @DisplayName("bloqueado despues de la semana medida: el conflicto con la sesion anterior se detecta")
        void bloqueadoQueEstabaEnLaSemanaAnterior() {
            var p = participante(GoalType.GAIN_MUSCLE, true);
            long y = p.semana().get(1).ejercicios().get(0).id();
            jdbc.update("UPDATE user_profiles SET blocked_exercise_ids = ARRAY[?]::bigint[] WHERE user_id = ?",
                    y, p.userId());

            assertTrue(adaptar(p, AdaptationDecision.NO_CHANGE, List.of()).isEmpty(),
                    "se conservo un ejercicio que el participante bloqueo");
            var evidencia = intento(p);
            assertEquals("NO_FACTIBLE", evidencia.get("status"));
            assertTrue(String.valueOf(evidencia.get("rejection_reason")).contains("V1: el ejercicio " + y),
                    String.valueOf(evidencia.get("rejection_reason")));
        }
    }
}
