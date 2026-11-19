package main.web.services.fitsense.p2d;

import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.domain.services.ConservationValidator;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * H1 · ESCENARIO U04 DE EXTREMO A EXTREMO, para CI (no opt-in). Flujo real de adaptacion
 * ({@link PlanningContextFacade#adaptWeeklyPlan}), validadores reales (V8C, estricta con 4D, V1..V25) y PostgreSQL
 * efimero; solo la IA de correccion esta simulada.
 * <p>
 * Un participante GAIN_MUSCLE intermedio de gimnasio omite una sesion por TOO_DIFFICULT (VARIANTE). La sesion tiene
 * dos ejercicios estructurales de patrones distintos, A y B, con la misma dosis 4D, y el patron de B aparece tambien en
 * otra sesion de la semana (como el VERTICAL_PULL de U04): la cobertura semanal V23 no ve la perdida. La «IA» propone:
 * <ul>
 *   <li>DISTRIBUCION INCORRECTA (el caso U04 / #12b): A -> a1 y B -> a2, ambos del patron A. La sesion pierde B;</li>
 *   <li>DISTRIBUCION CORRECTA: A -> a1 y B -> b1, cada uno de su patron.</li>
 * </ul>
 * La subclase fija la regla de V8C: la de produccion por defecto o la historica {@code conjunto}, explicita.
 */
abstract class ConservacionPatronH1Base {

    @MockitoBean ReplicateSessionFixGenerator ia;

    @Autowired PlanningContextFacade planning;
    @Autowired UserProfileCommandService perfiles;
    @Autowired SessionDurationEstimator estimador;
    @Autowired JdbcTemplate jdbc;
    @Autowired ConservationValidator v8c;
    @Autowired main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService planComandos;

    static final LocalDate SEMANA_MEDIDA = LocalDate.of(2026, 11, 2);
    static final LocalDate SEMANA_NUEVA = SEMANA_MEDIDA.plusWeeks(1);
    static final AtomicInteger SECUENCIA = new AtomicInteger();
    static final List<String> EQUIPAMIENTO = List.of("dumbbell", "barbell", "cable", "leverage machine", "bench", "band");

    record Ejercicio(long id, String tipo, Short series, Short reps, Integer segundos, Short descanso) {
        PlanDraft.DraftExercise borrador() {
            return new PlanDraft.DraftExercise(id, PrescriptionType.valueOf(tipo), series, reps, segundos, null,
                    descanso, null);
        }

        PlanDraft.DraftExercise como(long otro) {
            return new PlanDraft.DraftExercise(otro, PrescriptionType.valueOf(tipo), series, reps, segundos, null,
                    descanso, null);
        }
    }

    record Sesion(long id, LocalDate fecha, String enfoque, List<Ejercicio> ejercicios) {}

    record Participante(long userId, long planMedido, long metrica, List<Sesion> semana) {
        Set<Long> ids() {
            return semana.stream().flatMap(s -> s.ejercicios().stream()).map(Ejercicio::id).collect(Collectors.toSet());
        }
    }

    /** La sesion y los cinco ejercicios del caso: A, B y los sustitutos a1, a2 (patron de A) y b1 (patron de B). */
    record Caso(Participante p, Sesion sesion, Ejercicio a, Ejercicio b, long a1, long a2, long b1) {}

    @BeforeEach
    void iaSimulada() {
        reset(ia);
        when(ia.isEnabled()).thenReturn(true);
        when(ia.modelName()).thenReturn("simulado");
        when(ia.pedirRespuesta(any(), any(), any(), any(), anyInt()))
                .thenReturn(new ReplicateSessionFixGenerator.RawFix("{}", "salida-simulada"));
    }

    // ----------------------------------------------------------------------------------------------- escenario

    Participante participante() {
        int n = SECUENCIA.incrementAndGet();
        String email = ("h1-" + getClass().getSimpleName() + n + "@pruebas.invalid").toLowerCase(java.util.Locale.ROOT);
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Prueba', now(), now())", email);
        long userId = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(userId, LocalDate.of(1992, 5, 10), BiologicalSex.FEMALE,
                new BigDecimal("165"), new BigDecimal("68"), null, FitnessLevel.INTERMEDIATE, GoalType.GAIN_MUSCLE,
                "prueba H1", TrainingLocation.GYM, (short) 3, List.of((short) 1, (short) 3, (short) 5), (short) 45,
                EQUIPAMIENTO, List.of(), "Ninguna", true, true, true, true, true));
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

    List<Sesion> semana(long planId) {
        var filas = jdbc.queryForList("SELECT planned_workout_id, scheduled_date, focus_code FROM planned_workouts"
                + " WHERE plan_id = ? ORDER BY scheduled_date", planId);
        var lista = new ArrayList<Sesion>();
        for (var f : filas) {
            long id = ((Number) f.get("planned_workout_id")).longValue();
            var ejercicios = jdbc.query("SELECT exercise_id, prescription_type, planned_sets, planned_reps,"
                            + " planned_duration_seconds, rest_seconds FROM planned_workout_exercises"
                            + " WHERE planned_workout_id = ? ORDER BY exercise_order",
                    (rs, k) -> new Ejercicio(rs.getLong(1), rs.getString(2), (Short) rs.getObject(3, Short.class),
                            (Short) rs.getObject(4, Short.class), (Integer) rs.getObject(5, Integer.class),
                            (Short) rs.getObject(6, Short.class)), id);
            lista.add(new Sesion(id, ((java.sql.Date) f.get("scheduled_date")).toLocalDate(),
                    (String) f.get("focus_code"), ejercicios));
        }
        return lista;
    }

    String patron(long ejercicio) {
        return jdbc.queryForObject("SELECT movement_pattern FROM exercises WHERE exercise_id = ?", String.class,
                ejercicio);
    }

    /** Sustitutos del mismo patron y zona que el participante puede recibir (el mismo conjunto que vera la IA). */
    List<Long> sustitutos(Participante p, long ejercicio, Set<Long> elegibles) {
        var fila = jdbc.queryForMap("SELECT movement_pattern, body_part_id FROM exercises WHERE exercise_id = ?",
                ejercicio);
        return jdbc.queryForList("SELECT exercise_id FROM exercises WHERE is_active AND movement_pattern = ?"
                                + " AND body_part_id = ? AND difficulty_level <= 2 AND exercise_id <> ? ORDER BY exercise_id",
                        Long.class, fila.get("movement_pattern"), fila.get("body_part_id"), ejercicio).stream()
                .filter(id -> !p.ids().contains(id)).filter(elegibles::contains).toList();
    }

    /**
     * Busca en la semana una sesion con dos estructurales SETS_REPS de patrones distintos (A y B) y la MISMA dosis,
     * de modo que A tenga al menos dos sustitutos y B al menos uno. Asi V24 no distingue las propuestas: solo V8C.
     */
    Caso caso() {
        for (int intento = 0; intento < 6; intento++) {
            var c = casoDe(participante());
            if (c != null) return c;
        }
        throw new AssertionError("premisa: ninguna sesion con dos estructurales de patrones distintos y sustitutos");
    }

    long zona(long ejercicio) {
        return jdbc.queryForObject("SELECT body_part_id FROM exercises WHERE exercise_id = ?", Long.class, ejercicio);
    }

    /**
     * La propuesta incorrecta no puede tropezar con otra regla: ningun grupo muscular puede pasar de la mitad de la
     * sesion (V15) al poner a2 en el lugar de B. Asi la unica diferencia entre las dos reglas de V8C es la de H1.
     */
    boolean v15Admite(Sesion s, Ejercicio b, long a2) {
        var zonas = s.ejercicios().stream().map(e -> e.id() == b.id() ? zona(a2) : zona(e.id()))
                .collect(Collectors.groupingBy(z -> z, Collectors.counting()));
        return zonas.values().stream().allMatch(n -> n <= s.ejercicios().size() / 2);
    }

    Caso casoDe(Participante p) {
        var elegibles = planComandos.prepare(new main.web.services.fitsense.planning.domain.model.commands
                        .GenerateWeeklyPlanCommand(p.userId(), SEMANA_MEDIDA, null, true, null, null))
                .context().availableExercises().stream().map(CandidateExercise::exerciseId).collect(Collectors.toSet());
        for (var s : p.semana()) {
            var estructurales = s.ejercicios().stream().filter(e -> "SETS_REPS".equals(e.tipo()))
                    .filter(e -> MandatorySlot.ofMovementPattern(patron(e.id())).isPresent()).toList();
            for (var a : estructurales)
                for (var b : estructurales) {
                    if (a.id() == b.id() || patron(a.id()).equals(patron(b.id()))) continue;
                    // Como en U04: el patron de B sigue presente en OTRA sesion de la semana, asi que la cobertura
                    // semanal (V23, 4C) se cumple igual y solo V8C puede ver la perdida dentro de la sesion.
                    String pb = patron(b.id());
                    if (p.semana().stream().filter(o -> o.id() != s.id()).flatMap(o -> o.ejercicios().stream())
                            .noneMatch(e -> pb.equals(patron(e.id())))) continue;
                    if (!Objects.equals(a.series(), b.series()) || !Objects.equals(a.reps(), b.reps())
                            || !Objects.equals(a.descanso(), b.descanso())) continue;
                    var sa = sustitutos(p, a.id(), elegibles);
                    var sb = sustitutos(p, b.id(), elegibles);
                    if (sa.size() >= 2 && !sb.isEmpty() && v15Admite(s, b, sa.get(1)))
                        return new Caso(p, s, a, b, sa.get(0), sa.get(1), sb.get(0));
                }
        }
        return null;
    }

    // ----------------------------------------------------------------------------------------------- adaptacion

    /** La «IA» devuelve siempre la misma sesion: A -> nuevoA y B -> nuevoB, con la dosis de cada original. */
    Optional<Long> proponer(Caso c, long nuevoA, long nuevoB) {
        when(ia.interpretar(anyString(), any())).thenAnswer(inv -> {
            PlanGenerationContext ctx = inv.getArgument(1);
            var ejercicios = c.sesion().ejercicios().stream().map(e -> e.id() == c.a().id() ? e.como(nuevoA)
                    : e.id() == c.b().id() ? e.como(nuevoB) : e.borrador()).toList();
            var fecha = c.sesion().fecha().plusWeeks(1);
            int minutos = estimador.estimateMinutes(new PlanDraft.DraftWorkout(fecha,
                    WorkoutFocus.valueOf(c.sesion().enfoque()), "x", 0, ejercicios), ctx.prescription());
            return List.of(new CorrectedSession(c.sesion().id(), fecha, minutos, ejercicios, "correccion simulada"));
        });
        var orden = new PlanAdjustment(List.of(AdjustmentType.NONE), null, null, null, 0.0, 0.0, null, null, null,
                "H1", null, null, List.of(new PlanAdjustment.TargetedSession(c.sesion().id(),
                TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA)));
        return planning.adaptWeeklyPlan(c.p().userId(), SEMANA_NUEVA, orden, new BigDecimal("42.00"), null,
                new MeasuredWeekView(c.p().metrica(), c.p().planMedido(), new BigDecimal("42.00"),
                        new BigDecimal("100.00"), AdaptationDecision.ADAPT_LOCALIZED.name()));
    }

    Map<String, Object> intento(Participante p) {
        return jdbc.queryForMap("SELECT status, generation_source, ai_calls, resulting_plan_id, rejection_reason"
                + " FROM plan_adaptation_attempts WHERE user_id = ? AND new_week_start_date = ?", p.userId(), SEMANA_NUEVA);
    }

    /** Patrones de la sesion, con repeticion (el multiconjunto). */
    Map<String, Long> distribucion(Sesion s) {
        return s.ejercicios().stream().collect(Collectors.groupingBy(e -> patron(e.id()), TreeMap::new,
                Collectors.counting()));
    }

    /** Lo publicado conserva, sesion a sesion, ejercicios y dosis de la semana medida. */
    void assertCopiaExacta(Participante p, long planNuevo) {
        var nueva = semana(planNuevo);
        assertEquals(p.semana().size(), nueva.size());
        for (int i = 0; i < nueva.size(); i++) {
            assertEquals(p.semana().get(i).fecha().plusWeeks(1), nueva.get(i).fecha());
            assertEquals(p.semana().get(i).ejercicios(), nueva.get(i).ejercicios());
        }
    }

    /** Las sesiones no senaladas quedan identicas (ejercicios y dosis). */
    void assertNoSenaladasIntactas(Caso c, long planNuevo) {
        var nueva = semana(planNuevo);
        for (int i = 0; i < nueva.size(); i++)
            if (c.p().semana().get(i).id() != c.sesion().id())
                assertEquals(c.p().semana().get(i).ejercicios(), nueva.get(i).ejercicios(),
                        "la sesion no senalada " + c.p().semana().get(i).id() + " cambio");
    }

    Sesion senaladaPublicada(Caso c, long planNuevo) {
        var nueva = semana(planNuevo);
        for (int i = 0; i < nueva.size(); i++)
            if (c.p().semana().get(i).id() == c.sesion().id()) return nueva.get(i);
        throw new AssertionError("la sesion senalada no esta en la semana nueva");
    }

    List<AiRetryFeedback> feedbacks(int veces) {
        var captor = org.mockito.ArgumentCaptor.forClass(AiRetryFeedback.class);
        verify(ia, times(veces)).pedirRespuesta(any(), any(), any(), captor.capture(), anyInt());
        return captor.getAllValues();
    }
}
