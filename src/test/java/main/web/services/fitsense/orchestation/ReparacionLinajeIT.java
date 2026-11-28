package main.web.services.fitsense.orchestation;

import main.web.services.fitsense.execution.domain.model.commands.FinishWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.model.commands.RecordSessionExerciseCommand;
import main.web.services.fitsense.execution.domain.model.commands.StartWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.services.WorkoutSessionCommandService;
import main.web.services.fitsense.orchestration.application.internal.WeeklyCycleService;
import main.web.services.fitsense.planning.application.internal.commandservices.PublishedPlanSafetyRepairService;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.infrastructure.generation.ai.ReplicateSessionFixGenerator;
import main.web.services.fitsense.planning.interfaces.acl.PlanningContextFacade;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.BiologicalSex;
import main.web.services.fitsense.profiling.domain.model.valueobjects.FitnessLevel;
import main.web.services.fitsense.profiling.domain.model.valueobjects.GoalType;
import main.web.services.fitsense.profiling.domain.model.valueobjects.TrainingLocation;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AUDITORIA POST R/R' · P-P2 y H6 CON EL CICLO REAL. Son defectos de implementacion y se corrigen en todas las
 * politicas; aqui se usa la historica, con la progresion encendida para tener una dosis progresada publicada.
 * <ul>
 *   <li><b>P-P2:</b> una semana con una dosis PROGRESADA (fuera de la exacta de 4D) y un ejercicio que hoy es
 *       inseguro (1374 sin barra) se repara, porque la reparacion emite las dosis HEREDADAS con linaje. Antes V24
 *       rechazaba la dosis progresada y la reparacion quedaba NO_VALIDA.</li>
 *   <li><b>H6:</b> la version reparada ({@code SAFETY_REPAIR}) no tiene intento propio. El ciclo siguiente sigue el
 *       linaje por el plan padre (la sesion gemela es identica) y la copia hereda la dosis progresada. Antes el
 *       linaje se cortaba y V24 rechazaba la copia (NO_FACTIBLE).</li>
 * </ul>
 * <b>Determinista (16/11/2026).</b> La semana salia de {@code LocalDate.now()} y el motor de reglas siembra con
 * {@code userId x 1.000.003 + inicio de semana}: cada lunes cambiaba que relleno se convertia en el 1374. Si era un
 * relleno de zona pequena (2x13/90, la misma capacidad 4D que su sustituto seguro 104) la reparacion se publicaba; si
 * era uno de 2x10/90, el 104 no puede recibir menos de 2x13 (V24) ni mas que los 2x10 del quitado (V8C) y la
 * reparacion queda NO_VALIDA sin publicar nada, la perdida de factibilidad conservadora ya documentada
 * (IMPACTO_FACTIBILIDAD.md). Barrido de 16 semanas: 7 publicadas y 9 NO_VALIDA, siempre por esa dosis; nunca por la
 * fecha en si (cambio de mes y de ano incluidos). Ahora la semana es fija ({@link RelojPruebas}), la premisa de P-P2
 * se elige explicita (un relleno con la capacidad de su sustituto) y el caso NO_VALIDA tiene su propia prueba.
 * Docs: docs/FitSense/correccion_ci_linaje/DIAGNOSTICO_REPARACION_LINAJE.md.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.adaptation.progression.enabled=true"
})
@DisplayName("P-P2 y H6: reparar una semana progresada y seguir el linaje por la version reparada")
class ReparacionLinajeIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @MockitoBean ReplicateSessionFixGenerator ia;

    @Autowired WeeklyCycleService ciclo;
    @Autowired PlanningContextFacade planning;
    @Autowired UserProfileCommandService perfiles;
    @Autowired WorkoutSessionCommandService sesiones;
    @Autowired JdbcTemplate jdbc;

    static final AtomicInteger SECUENCIA = new AtomicInteger();
    /** Semana fija de las referencias (2026-11-02). Las sesiones se abren con RelojPruebas.abrirVentanaDeEjecucion. */
    static final LocalDate S1 = main.web.services.fitsense.RelojPruebas.SEMANA_REFERENCIA;
    static final List<String> EQUIPAMIENTO = List.of("dumbbell", "barbell", "cable", "leverage machine", "bench", "band");
    /** Solo rellenos: ningun patron que cuente como slot 4C. */
    static final String SIN_SLOT = " AND x.movement_pattern NOT IN ('KNEE_DOMINANT','HIP_HINGE','HIP_EXTENSION',"
            + "'HORIZONTAL_PUSH','VERTICAL_PUSH','HORIZONTAL_PULL','VERTICAL_PULL')";

    @BeforeEach
    void iaSimulada() {
        reset(ia);
        when(ia.isEnabled()).thenReturn(true);
        when(ia.modelName()).thenReturn("simulado");
        when(ia.pedirRespuesta(any(), any(), any(), any(), anyInt()))
                .thenReturn(new ReplicateSessionFixGenerator.RawFix("{}", "salida-simulada"));
        when(ia.interpretar(anyString(), any())).thenAnswer(inv -> List.of());
    }

    /** Relleno de zona pequena: la dosis 4D (HYPERTROPHY) de su capacidad es 2x13/90, la misma que la del sustituto. */
    static final String MISMA_CAPACIDAD_QUE_EL_SUSTITUTO = " AND pwe.planned_sets = 2 AND pwe.planned_reps = 13"
            + " AND pwe.rest_seconds = 90";
    /** Relleno de 2x10/90: su sustituto seguro (zona pequena, 2x13) no puede recibir su dosis. */
    static final String OTRA_CAPACIDAD = " AND pwe.planned_sets = 2 AND pwe.planned_reps = 10 AND pwe.rest_seconds = 90";

    @Test
    @DisplayName("semana progresada + 1374 inseguro: se repara (P-P2) y la siguiente hereda el paso por la version reparada (H6)")
    void repararYHeredar() {
        repararYHeredar(S1);
    }

    @Test
    @DisplayName("semanas con cambio de mes (30/11-06/12) y de ano (21/12-03/01): la misma reparacion y la misma herencia")
    void repararYHeredarEnCambiosDeMesYAno() {
        // S1, S1+1 y S1+2: la semana reparada (S1+2) y la copia (S1+3) cruzan el cambio de mes o de ano.
        repararYHeredar(LocalDate.of(2026, 11, 16));   // S1+2 = 30/11-06/12 (noviembre -> diciembre)
        repararYHeredar(LocalDate.of(2026, 12, 14));   // S1+2 = 28/12-03/01 (2026 -> 2027)
    }

    @Test
    @DisplayName("sin sustituto con la misma dosis: la reparacion queda NO_VALIDA y no publica nada; la semana y su paso de"
            + " progresion siguen intactos (perdida de factibilidad conservadora)")
    void sinSustitutoConLaMismaDosisNoPublicaNada() {
        var s = semanaProgresada(S1);
        long pe = relleno(s, OTRA_CAPACIDAD);
        int planes = planesDe(s.usuario());
        assertEquals(1, jdbc.update("UPDATE planned_workout_exercises SET exercise_id = 1374 WHERE planned_exercise_id = ?",
                pe));

        var r = planning.repairPublishedPlanForSafety(s.usuario(), s.s1().plusWeeks(2), hoy(s.s1()));
        System.out.println("P-P2 sin sustituto " + r);
        assertEquals(PublishedPlanSafetyRepairService.Estado.NO_VALIDA, r.estado(), r.toString());
        assertTrue(r.problemas().stream().anyMatch(p -> p.startsWith("V24")), "el motivo es la dosis de V24: " + r);
        assertNull(r.planNuevo());
        assertEquals(planes, planesDe(s.usuario()), "no se publica nada");
        assertEquals(s.w3(), jdbc.queryForObject("SELECT plan_id FROM weekly_training_plans WHERE user_id = ?"
                + " AND week_start_date = ? AND status = 'ACTIVE'", Long.class, s.usuario(), s.s1().plusWeeks(2)),
                "la semana publicada sigue siendo la misma");
        assertEquals(s.dosisProgresada(), dosis(s.w3()).get(s.i()), "el paso de progresion no se toca");
    }

    @Autowired main.web.services.fitsense.planning.application.internal.outboundservices.acl.ExternalCatalogService catalogo;

    /** Tres semanas con el ciclo real hasta un paso de progresion publicado en la tercera (S1+2). */
    record SemanaProgresada(LocalDate s1, long usuario, long w3, int i, long progresada, List<Map<String, Object>> dosisProgresada) {}

    SemanaProgresada semanaProgresada(LocalDate s1) {
        long u = participante();
        long w1 = planning.generateWeeklyPlan(u, s1, PlanAdjustment.none(), null, null).orElseThrow();
        ejecutar(u, w1);
        long w2 = ciclo(u, s1, 0);
        ejecutar(u, w2);
        long w3 = ciclo(u, s1, 1);
        var medida = dosis(w2);
        var conPaso = dosis(w3);
        int i = -1;
        for (int k = 0; k < conPaso.size(); k++)
            if (!medida.get(k).equals(conPaso.get(k))) i = k;
        assertTrue(i >= 0, "premisa: un paso de progresion en la semana 3: " + ultimoIntento(u));
        return new SemanaProgresada(s1, u, w3, i, sesiones(w3).get(i), conPaso.get(i));
    }

    /** Un relleno (ningun patron de slot 4C) de OTRA sesion que la progresada, con la capacidad indicada. */
    long relleno(SemanaProgresada s, String capacidad) {
        var ids = jdbc.queryForList("SELECT pwe.planned_exercise_id FROM planned_workout_exercises pwe"
                + " JOIN planned_workouts pw ON pw.planned_workout_id = pwe.planned_workout_id"
                + " JOIN exercises x ON x.exercise_id = pwe.exercise_id"
                + " WHERE pw.plan_id = ? AND pw.planned_workout_id <> ? AND pwe.prescription_type = 'SETS_REPS'"
                + SIN_SLOT + capacidad + " ORDER BY pw.scheduled_date, pwe.exercise_order LIMIT 1", Long.class,
                s.w3(), s.progresada());
        assertFalse(ids.isEmpty(), "premisa: la semana " + s.s1().plusWeeks(2) + " tiene un relleno" + capacidad);
        return ids.get(0);
    }

    /** El dia anterior a la semana que se repara: toda ella es futura, como cuando la prueba usaba la semana siguiente. */
    static LocalDate hoy(LocalDate s1) {
        return s1.plusWeeks(2).minusDays(1);
    }

    private void repararYHeredar(LocalDate s1) {
        var s = semanaProgresada(s1);
        long u = s.usuario();
        int i = s.i();
        var dosisProgresada = s.dosisProgresada();

        // Un relleno de OTRA sesion pasa a ser el 1374 (exige barra, que no tiene): la semana queda insegura. Se elige
        // uno con la capacidad de su sustituto seguro, la premisa de P-P2: reparar sin cambiar ninguna dosis.
        long pe = relleno(s, MISMA_CAPACIDAD_QUE_EL_SUSTITUTO);
        assertEquals(1, jdbc.update("UPDATE planned_workout_exercises SET exercise_id = 1374 WHERE planned_exercise_id = ?",
                pe));

        var r = planning.repairPublishedPlanForSafety(u, s1.plusWeeks(2), hoy(s1));
        System.out.println("P-P2 " + r);
        assertEquals(PublishedPlanSafetyRepairService.Estado.REPARADA, r.estado(),
                "P-P2: la semana con la dosis progresada se puede reparar: " + r);
        long reparada = r.planNuevo();
        assertEquals(dosisProgresada, dosis(reparada).get(i), "la sesion progresada no cambia al reparar");
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM plan_adaptation_attempts WHERE resulting_plan_id = ?",
                Integer.class, reparada), "premisa de H6: la version reparada no tiene intento propio");

        // H6: la semana siguiente (copia) sigue el linaje por el plan padre y hereda la dosis progresada.
        ejecutar(u, reparada);
        var w4 = ciclo.runFor(u, s1.plusWeeks(2), s1.plusWeeks(3));
        var ev4 = ultimoIntento(u);
        System.out.println("H6 " + ev4.get("status") + " " + ev4.get("rejection_reason"));
        assertTrue(w4.isPresent(), "H6: la copia tras la reparacion se publica: " + ev4);
        assertEquals(dosisProgresada, dosis(w4.get()).get(i), "la dosis progresada se hereda por la version reparada");
    }

    int planesDe(long u) {
        return jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ?", Integer.class, u);
    }

    // =======================================================================

    long ciclo(long u, LocalDate s1, int k) {
        var r = ciclo.runFor(u, s1.plusWeeks(k), s1.plusWeeks(k + 1));
        assertTrue(r.isPresent(), "sin semana publicada en la " + (k + 2) + ": " + ultimoIntento(u));
        return r.get();
    }

    long participante() {
        String email = "reparacion-linaje" + SECUENCIA.incrementAndGet() + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Prueba', now(), now())", email);
        long userId = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(userId, LocalDate.of(1992, 5, 10), BiologicalSex.FEMALE,
                new BigDecimal("165"), new BigDecimal("68"), null, FitnessLevel.INTERMEDIATE,
                GoalType.GAIN_MUSCLE, "reparacion y linaje", TrainingLocation.GYM, (short) 3,
                List.of((short) 1, (short) 3, (short) 5), (short) 45, EQUIPAMIENTO, List.of(), "Ninguna",
                true, true, true, true, true));
        return userId;
    }

    List<Long> sesiones(long planId) {
        return jdbc.queryForList("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ?"
                + " ORDER BY scheduled_date", Long.class, planId);
    }

    /** Por sesion, la dosis de cada ejercicio (id, tipo, series, reps, segundos, descanso). */
    List<List<Map<String, Object>>> dosis(long planId) {
        return sesiones(planId).stream().map(w -> jdbc.queryForList("SELECT exercise_id, prescription_type,"
                + " planned_sets, planned_reps, planned_duration_seconds, rest_seconds FROM planned_workout_exercises"
                + " WHERE planned_workout_id = ? ORDER BY exercise_order", w)).toList();
    }

    void ejecutar(long u, long planId) {
        main.web.services.fitsense.RelojPruebas.abrirVentanaDeEjecucion(jdbc, u);
        for (long w : sesiones(planId)) {
            long sesion = sesiones.handle(new StartWorkoutSessionCommand(u, w)).orElseThrow().getId();
            for (var f : jdbc.queryForList("SELECT planned_exercise_id, prescription_type, planned_sets,"
                    + " planned_reps, planned_duration_seconds FROM planned_workout_exercises"
                    + " WHERE planned_workout_id = ? ORDER BY exercise_order", w)) {
                boolean duracion = "DURATION".equals(f.get("prescription_type"));
                int series = f.get("planned_sets") == null ? 1 : ((Number) f.get("planned_sets")).intValue();
                int total = series * ((Number) f.get(duracion ? "planned_duration_seconds" : "planned_reps")).intValue();
                sesiones.handle(new RecordSessionExerciseCommand(u, sesion,
                        ((Number) f.get("planned_exercise_id")).longValue(), (short) series,
                        duracion ? null : total, duracion ? total : null, null, null));
            }
            sesiones.handle(new FinishWorkoutSessionCommand(u, sesion, (short) 6, (short) 4, (short) 45));
        }
    }

    Map<String, Object> ultimoIntento(long u) {
        return jdbc.queryForMap("SELECT decision, status, ai_calls, resulting_plan_id, progressed_sessions::text,"
                + " rejection_reason FROM plan_adaptation_attempts WHERE user_id = ? ORDER BY attempt_id DESC LIMIT 1", u);
    }
}
