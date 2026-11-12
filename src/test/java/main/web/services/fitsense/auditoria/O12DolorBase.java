package main.web.services.fitsense.auditoria;

import main.web.services.fitsense.RelojPruebas;
import main.web.services.fitsense.execution.domain.model.commands.FinishWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.model.commands.RecordSessionExerciseCommand;
import main.web.services.fitsense.execution.domain.model.commands.StartWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.services.WorkoutSessionCommandService;
import main.web.services.fitsense.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import main.web.services.fitsense.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import main.web.services.fitsense.orchestration.application.internal.WeeklyCycleService;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.domain.services.AdaptedWeekProducer;
import main.web.services.fitsense.planning.infrastructure.generation.ai.ReplicateSessionFixGenerator;
import main.web.services.fitsense.planning.interfaces.acl.PlanningContextFacade;
import main.web.services.fitsense.planning.interfaces.rest.TrainingPlanController;
import main.web.services.fitsense.planning.interfaces.rest.resources.SkipWorkoutResource;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.*;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import main.web.services.fitsense.shared.domain.model.valueobjects.SkipReason;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AUDITORIA DE ACTIVACION (11/11/2026) · O12 de la cohorte O (GENERAL_FITNESS, intermedia, gimnasio, 55 min, martes y
 * jueves, 62 anos, sin colgarse ni por encima de la cabeza). Semana 1: martes hecho, jueves omitido por dolor ->
 * SEGURIDAD_ANTE_DOLOR sobre el jueves. La IA es una respuesta FIJA, identica con la progresion apagada y encendida:
 * <ul>
 *   <li>{@link Respuesta#MENOS_SERIES}: el jueves igual, con una serie menos en el primer ejercicio de 3 series
 *       (lo que propuso la IA simulada del arnes con la progresion apagada: "MENOS_SERIES 591 series 3->2");</li>
 *   <li>{@link Respuesta#VACIA}: ninguna sesion (lo que propuso con la progresion encendida);</li>
 *   <li>{@link Respuesta#SUBE_SERIES}: una serie MAS en el primer ejercicio (la IA nunca puede subir dosis, tampoco con
 *       la progresion encendida: las autorizaciones de progresion no valen en una adaptacion).</li>
 * </ul>
 * Invariante de seguridad comprobado en todas las variantes: ningun ejercicio excluido por el dolor (P-02) aparece en
 * ninguna semana publicada despues, tampoco por progresion ni por reincorporacion. Imprime DIAG_O12 para el informe.
 * Sin red: la IA esta simulada y failsafe apaga Replicate.
 */
abstract class O12DolorBase {

    @MockitoBean ReplicateSessionFixGenerator ia;
    @MockitoSpyBean AdaptedWeekProducer productor;

    @Autowired WeeklyCycleService ciclo;
    @Autowired PlanningContextFacade planning;
    @Autowired UserProfileCommandService perfiles;
    @Autowired WorkoutSessionCommandService sesiones;
    @Autowired TrainingPlanController planes;
    @Autowired UserRepository usuarios;
    @Autowired JdbcTemplate jdbc;
    @Autowired main.web.services.fitsense.planning.application.internal.queryservices.AdaptationOutcomeQueryService adaptaciones;

    static final AtomicInteger SECUENCIA = new AtomicInteger();
    static final LocalDate S1 = RelojPruebas.SEMANA_REFERENCIA;
    static final List<String> EQUIPAMIENTO = List.of("body weight", "dumbbell", "barbell", "cable", "leverage machine",
            "bench", "band", "kettlebell", "ez barbell", "smith machine");

    enum Respuesta { MENOS_SERIES, VACIA, SUBE_SERIES }

    final List<AdaptedWeekResult> producidos = new CopyOnWriteArrayList<>();

    abstract String condicion();

    record Resultado(String estado, String fuente, int llamadas, Set<Long> excluidos, List<Long> jueves,
                     List<Long> juevesPublicado, List<String> problemasPorIntento, Map<Integer, Set<Long>> reaparecen,
                     int pasosProgresion) {}

    Resultado recorrer(Respuesta respuesta) {
        reset(ia);
        producidos.clear();
        when(ia.isEnabled()).thenReturn(true);
        when(ia.modelName()).thenReturn("simulado");
        when(ia.pedirRespuesta(any(), any(), any(), any(), anyInt()))
                .thenReturn(new ReplicateSessionFixGenerator.RawFix("{}", "salida-simulada"));
        when(ia.interpretar(anyString(), any())).thenAnswer(inv -> responder(inv.getArgument(1), respuesta));
        doAnswer(inv -> {
            var r = (AdaptedWeekResult) inv.callRealMethod();
            producidos.add(r);
            return r;
        }).when(productor).produce(any(), any(), any(), any(), any(ExclusionesDeSeguridad.class));

        long u = participante();
        long plan = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        RelojPruebas.abrirVentanaDeEjecucion(jdbc, u);
        var ids = sesionesDe(plan);
        assertEquals(2, ids.size(), "O12 entrena martes y jueves");
        long jueves = ids.get(1);
        var juevesAntes = ejercicios(jueves);
        hecha(u, ids.get(0));
        planes.skip(UserDetailsImpl.build(usuarios.findById(u).orElseThrow()), jueves,
                new SkipWorkoutResource(SkipReason.PAIN_OR_DISCOMFORT));

        var r = ciclo.runFor(u, S1, S1.plusWeeks(1));
        var intento = ultimo(u);
        var excluidos = new TreeSet<>(jdbc.queryForList("SELECT exercise_id FROM user_safety_exclusions WHERE user_id = ?"
                + " AND status = 'ACTIVA'", Long.class, u));
        List<Long> juevesPublicado = List.of();
        String texto = null;
        var api = adaptaciones.history(u).stream().filter(a -> S1.plusWeeks(1).equals(a.weekStartDate())).findFirst().orElseThrow();
        var reaparecen = new TreeMap<Integer, Set<Long>>();
        int pasos = 0;
        if (r.isPresent()) {
            long publicado = r.get();
            var ids2 = sesionesDe(publicado);
            juevesPublicado = ejercicios(ids2.get(ids2.size() - 1));
            texto = jdbc.queryForObject("SELECT adjustment_reason FROM weekly_training_plans WHERE plan_id = ?",
                    String.class, publicado);
            reaparecen.put(2, interseccion(ejerciciosDelPlan(publicado), excluidos));
            // Tres semanas mas, todo hecho: la progresion (si esta encendida) actua en ciclos NO_CHANGE.
            long medido = publicado;
            for (int k = 1; k <= 3; k++) {
                RelojPruebas.abrirVentanaDeEjecucion(jdbc, u);
                for (long w : sesionesDe(medido)) hecha(u, w);
                var siguiente = ciclo.runFor(u, S1.plusWeeks(k), S1.plusWeeks(k + 1));
                if (siguiente.isEmpty()) break;
                medido = siguiente.get();
                reaparecen.put(k + 2, interseccion(ejerciciosDelPlan(medido), excluidos));
            }
            pasos = jdbc.queryForObject("SELECT coalesce(sum(cardinality(progressed_sessions)), 0) FROM"
                    + " plan_adaptation_attempts WHERE user_id = ?", Integer.class, u);
        }
        var problemas = producidos.stream().map(p -> p.estado() + " " + p.motivo() + " " + p.problemas()).toList();
        var res = new Resultado(String.valueOf(intento.get("status")), String.valueOf(intento.get("generation_source")),
                ((Number) intento.get("ai_calls")).intValue(), excluidos, juevesAntes, juevesPublicado, problemas,
                reaparecen, pasos);
        System.out.println("DIAG_O12 condicion=" + condicion() + " respuesta=" + respuesta
                + " estado=" + res.estado() + " fuente=" + res.fuente() + " llamadas=" + res.llamadas()
                + " intervenciones=" + intento.get("targeted_interventions")
                + " rechazo=" + intento.get("rejection_reason")
                + "\nDIAG_O12   jueves_medido=" + dosis(jueves)
                + "\nDIAG_O12   excluidos_P02=" + excluidos
                + "\nDIAG_O12   jueves_publicado=" + juevesPublicado
                + "\nDIAG_O12   intentos=" + problemas
                + "\nDIAG_O12   informe=" + intento.get("conservation_report")
                + "\nDIAG_O12   api=" + api.resultado() + " sesiones=" + api.sesiones() + " motivos=" + api.motivos()
                + "\nDIAG_O12   texto_publicado_jueves=" + texto
                + "\nDIAG_O12   excluidos_que_reaparecen_por_semana=" + reaparecen + " pasos_progresion=" + pasos);

        // Invariante de seguridad (P-02): lo que dolio no vuelve en ninguna semana publicada.
        reaparecen.forEach((semana, ids3) -> assertTrue(ids3.isEmpty(),
                condicion() + "/" + respuesta + ": semana " + semana + " vuelve a prescribir lo que dolio " + ids3));
        assertFalse(excluidos.isEmpty(), "la omision por dolor sin identificar excluye la sesion (P-02)");
        assertTrue(Set.of("APLICADA", "SIN_EFECTO", "NO_FACTIBLE", "RESPALDO").contains(res.estado()), res.toString());
        if (respuesta == Respuesta.SUBE_SERIES) {
            assertNotEquals("APLICADA", res.estado(), "una subida de dosis de la IA no se publica nunca");
            assertTrue(problemas.stream().anyMatch(x -> x.contains("sube la dosis")),
                    "V8C rechaza la subida: " + problemas);
        }
        return res;
    }

    private List<CorrectedSession> responder(PlanGenerationContext ctx, Respuesta respuesta) {
        if (respuesta == Respuesta.VACIA) return List.of();
        var salida = new ArrayList<CorrectedSession>();
        for (var s : ctx.adjustment().sesionesParaIA()) {
            long id = s.plannedWorkoutId();
            var f = jdbc.queryForMap("SELECT scheduled_date, expected_duration_minutes FROM planned_workouts"
                    + " WHERE planned_workout_id = ?", id);
            var lista = new ArrayList<>(jdbc.query("SELECT exercise_id, prescription_type, planned_sets, planned_reps,"
                            + " planned_duration_seconds, rest_seconds, target_load_kg FROM planned_workout_exercises"
                            + " WHERE planned_workout_id = ? ORDER BY exercise_order",
                    (rs, k) -> new PlanDraft.DraftExercise(rs.getLong(1), PrescriptionType.valueOf(rs.getString(2)),
                            rs.getObject(3, Short.class), rs.getObject(4, Short.class), rs.getObject(5, Integer.class),
                            rs.getBigDecimal(7), rs.getObject(6, Short.class), null), id));
            for (int i = 0; i < lista.size(); i++) {
                var e = lista.get(i);
                if (e.plannedSets() != null && e.plannedSets() >= 3) {
                    lista.set(i, new PlanDraft.DraftExercise(e.exerciseId(), e.prescriptionType(),
                            (short) (e.plannedSets() + (respuesta == Respuesta.SUBE_SERIES ? 1 : -1)), e.plannedReps(), e.plannedDurationSeconds(), e.targetLoadKg(),
                            e.restSeconds(), null));
                    break;
                }
            }
            salida.add(new CorrectedSession(id, ((java.sql.Date) f.get("scheduled_date")).toLocalDate().plusWeeks(1),
                    ((Number) f.get("expected_duration_minutes")).intValue(), lista, "respuesta fija de auditoria"));
        }
        return salida;
    }

    long participante() {
        String email = "auditoria-o12-" + condicion() + "-" + SECUENCIA.incrementAndGet() + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'O12', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1964, 3, 10), BiologicalSex.FEMALE,
                new BigDecimal("164"), new BigDecimal("63"), null, FitnessLevel.INTERMEDIATE, GoalType.GENERAL_FITNESS,
                "Auditoria O12", TrainingLocation.GYM, (short) 2, List.of((short) 2, (short) 4), (short) 55,
                EQUIPAMIENTO, List.of(), "Ninguna", true, true, true, false, false));
        return u;
    }

    List<Long> sesionesDe(long plan) {
        return jdbc.queryForList("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ?"
                + " ORDER BY scheduled_date", Long.class, plan);
    }

    List<Long> ejercicios(long w) {
        return jdbc.queryForList("SELECT exercise_id FROM planned_workout_exercises WHERE planned_workout_id = ?"
                + " ORDER BY exercise_order", Long.class, w);
    }

    String dosis(long w) {
        return jdbc.queryForList("SELECT exercise_id, planned_sets, planned_reps, planned_duration_seconds, rest_seconds"
                + " FROM planned_workout_exercises WHERE planned_workout_id = ? ORDER BY exercise_order", w).toString();
    }

    Set<Long> ejerciciosDelPlan(long plan) {
        return new TreeSet<>(jdbc.queryForList("SELECT pwe.exercise_id FROM planned_workout_exercises pwe JOIN"
                + " planned_workouts pw ON pw.planned_workout_id = pwe.planned_workout_id WHERE pw.plan_id = ?",
                Long.class, plan));
    }

    static Set<Long> interseccion(Set<Long> a, Set<Long> b) {
        var r = new TreeSet<>(a);
        r.retainAll(b);
        return r;
    }

    Map<String, Object> ultimo(long u) {
        return jdbc.queryForMap("SELECT status, generation_source, ai_calls, targeted_interventions::text,"
                + " rejection_reason, conservation_report::text FROM plan_adaptation_attempts WHERE user_id = ?"
                + " ORDER BY attempt_id LIMIT 1", u);
    }

    void hecha(long u, long w) {
        long s = sesiones.handle(new StartWorkoutSessionCommand(u, w)).orElseThrow().getId();
        for (var f : jdbc.queryForList("SELECT planned_exercise_id, prescription_type, planned_sets, planned_reps,"
                + " planned_duration_seconds FROM planned_workout_exercises WHERE planned_workout_id = ?", w)) {
            boolean duracion = "DURATION".equals(f.get("prescription_type"));
            int series = f.get("planned_sets") == null ? 1 : ((Number) f.get("planned_sets")).intValue();
            int total = series * ((Number) f.get(duracion ? "planned_duration_seconds" : "planned_reps")).intValue();
            sesiones.handle(new RecordSessionExerciseCommand(u, s, ((Number) f.get("planned_exercise_id")).longValue(),
                    (short) series, duracion ? null : total, duracion ? total : null, null, null));
        }
        sesiones.handle(new FinishWorkoutSessionCommand(u, s, (short) 6, (short) 4, (short) 45));
    }
}
