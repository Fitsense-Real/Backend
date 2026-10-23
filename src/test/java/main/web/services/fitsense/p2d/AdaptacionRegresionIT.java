package main.web.services.fitsense.p2d;

import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.domain.services.CatalogEligibilityPolicy;
import main.web.services.fitsense.planning.domain.services.GoalFeasibilityPolicy;
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

    private Participante participante(GoalType objetivo, Boolean permiteSuelo) {
        int n = SECUENCIA.incrementAndGet();
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Prueba', now(), now())", "regresion" + n + "@pruebas.invalid");
        long userId = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class,
                "regresion" + n + "@pruebas.invalid");
        perfiles.handle(new CreateUserProfileCommand(userId, LocalDate.of(1992, 5, 10), BiologicalSex.FEMALE,
                new BigDecimal("165"), new BigDecimal("68"), null, FitnessLevel.INTERMEDIATE, objetivo,
                "prueba de regresion", TrainingLocation.GYM, (short) 3, List.of((short) 1, (short) 3, (short) 5),
                (short) 45, List.of("dumbbell", "barbell", "cable", "leverage machine", "bench", "band"),
                List.of(), "Ninguna", true, permiteSuelo, true, true, true));

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
        }

        /**
         * DEFECTO ABIERTO, CARACTERIZADO, NO CORREGIDO (P2-D intocable en este
         * paso). Una continuidad sin llamadas a la IA -NO_CHANGE,
         * INSUFFICIENT_DATA, o respaldo con la IA apagada o el cortacircuitos
         * abierto- se publica con generation_attempts = ai_calls = 0, y
         * ck_plan_attempts (V4) exige >= 1. La semana queda sin plan.
         * <p>
         * Cuando se corrija, ESTA PRUEBA FALLARA a proposito: hay que sustituirla
         * por la que comprueba la copia exacta con NO_CHANGE.
         */
        @Test
        @DisplayName("DEFECTO ABIERTO: NO_CHANGE sin llamadas a la IA viola ck_plan_attempts y no publica")
        void defectoAbiertoNoChangeSinLlamadasViolaCkPlanAttempts() {
            var p = participante(GoalType.GAIN_MUSCLE, true);
            var error = assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                    () -> adaptar(p, AdaptationDecision.NO_CHANGE, List.of()));
            assertTrue(error.getMostSpecificCause().getMessage().contains("ck_plan_attempts"),
                    error.getMostSpecificCause().getMessage());
            assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans"
                    + " WHERE user_id = ? AND week_start_date = ?", Integer.class, p.userId(), SEMANA_NUEVA));
            System.out.println("DEFECTO_ABIERTO ck_plan_attempts: intentos registrados = " + jdbc.queryForObject(
                    "SELECT count(*) FROM plan_adaptation_attempts WHERE user_id = ?", Integer.class, p.userId()));
            verifyNoInteractions(ia);
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
        @DisplayName("reclasificado con una restriccion de seguridad vigente: se trata como problema real, no se conserva")
        void reclasificadoConRestriccionDeSeguridad() {
            var p = participante(GoalType.GAIN_MUSCLE, false);
            long afectado = p.semana().get(0).ejercicios().get(0).id();

            jdbc.update("UPDATE exercises SET requires_floor = TRUE WHERE exercise_id = ?", afectado);

            assertTrue(adaptar(p, AdaptationDecision.NO_CHANGE, List.of()).isEmpty());
            var evidencia = intento(p);
            assertEquals("NO_FACTIBLE", evidencia.get("status"));
            assertTrue(String.valueOf(evidencia.get("rejection_reason"))
                            .contains("V1: el ejercicio " + afectado + " no esta en available_exercises"),
                    String.valueOf(evidencia.get("rejection_reason")));
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
                                .filter(x -> !GoalFeasibilityPolicy.CLASIFICACION_EN_REVISION.contains(x.exerciseId()))
                                .filter(x -> !CatalogEligibilityPolicy.esComplemento(x, ctx.profile().goalType()))
                                .filter(x -> !"upper arms".equals(o.bodyPartCode())
                                        || Objects.equals(x.targetMuscle(), o.targetMuscle()))
                                .sorted(Comparator.comparing((CandidateExercise x) ->
                                                !Objects.equals(x.bodyPartCode(), o.bodyPartCode()))
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
                            .filter(x -> !GoalFeasibilityPolicy.CLASIFICACION_EN_REVISION.contains(x.exerciseId()))
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
            var sesion = p.semana().stream().filter(s -> s.ejercicios().stream().anyMatch(this::esRelleno))
                    .findFirst().orElseThrow(() -> new AssertionError("premisa: sin sesion con relleno"));
            var relleno = sesion.ejercicios().stream().filter(this::esRelleno).findFirst().orElseThrow();
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
            assertTrue(publicado.isEmpty());
            assertEquals("NO_FACTIBLE", intento(p).get("status"));
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
            return candidatos.stream().filter(id -> !p.ids().contains(id)).findFirst()
                    .orElseThrow(() -> new AssertionError("sin sustituto del mismo patron"));
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

        private Ejercicio estructural(Sesion s) {
            return s.ejercicios().stream().filter(e -> "SETS_REPS".equals(e.tipo()) && esSlot(patron(e.id())))
                    .findFirst().orElseThrow();
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
            var x = estructural(sesion);
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
            var x = estructural(sesion);
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
            assertEquals(3, ((Number) intento(p).get("ai_calls")).intValue());
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
