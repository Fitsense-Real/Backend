package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.RelojPruebas;
import main.web.services.fitsense.catalog.domain.services.FamiliaDeMovimientoEquivalente;
import main.web.services.fitsense.execution.domain.model.commands.FinishWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.model.commands.RecordSessionExerciseCommand;
import main.web.services.fitsense.execution.domain.model.commands.StartWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.services.WorkoutSessionCommandService;
import main.web.services.fitsense.orchestration.application.internal.WeeklyCycleService;
import main.web.services.fitsense.planning.application.internal.outboundservices.acl.ExternalCatalogService;
import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.domain.model.valueobjects.WorkoutFocus;
import main.web.services.fitsense.planning.infrastructure.generation.ai.ReplicateSessionFixGenerator;
import main.web.services.fitsense.planning.interfaces.acl.PlanningContextFacade;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.BiologicalSex;
import main.web.services.fitsense.profiling.domain.model.valueobjects.FitnessLevel;
import main.web.services.fitsense.profiling.domain.model.valueobjects.GoalType;
import main.web.services.fitsense.profiling.domain.model.valueobjects.TrainingLocation;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import main.web.services.fitsense.shared.domain.model.valueobjects.SkipReason;
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
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * O20 · OPCION A · SIN ALTERNATIVA (orden de seguridad del 24/11/2026): si, excluida la familia de lo que dolio, no
 * queda ningun sustituto que cumpla SUST-SEG-1, NO se fuerza una sustitucion insegura ni se usa una variante
 * equivalente: el caso lo resuelve el mecanismo seguro existente (retirada localizada en la sesion senalada,
 * NO_FACTIBLE con su causa y P-03) sin publicar nada invalido.
 * <p>
 * CATALOGO REDUCIDO A PROPOSITO, solo en la base efimera de esta prueba: se desactivan todos los ejercicios que
 * {@link SafetySubstitutionPolicy#descarte} admitiria como sustitutos de 537 en las sesiones donde esta (la misma
 * funcion que usa la politica, con el limite de dificultad apagado: el conjunto mas amplio). Solo quedan activas las
 * variantes de su familia, que son justo lo que la opcion A prohibe. La IA de adaptacion se simula rechazada; sin
 * Replicate. No cambia ningun validador.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
@DisplayName("O20 opcion A: sin alternativa fuera de la familia, se retira o queda NO_FACTIBLE; nunca la variante")
class DolorSinAlternativaIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @MockitoBean ReplicateSessionFixGenerator ia;
    @MockitoBean main.web.services.fitsense.planning.infrastructure.scheduling.OverdueWorkoutScheduler vencidas;

    @Autowired WeeklyCycleService ciclo;
    @Autowired PlanningContextFacade planning;
    @Autowired WeeklyTrainingPlanCommandService planComandos;
    @Autowired ExternalCatalogService catalogo;
    @Autowired main.web.services.fitsense.planning.application.internal.commandservices.PainMemoryService dolor;
    @Autowired UserProfileCommandService perfiles;
    @Autowired WorkoutSessionCommandService sesiones;
    @Autowired JdbcTemplate jdbc;

    static final LocalDate S1 = RelojPruebas.SEMANA_REFERENCIA;
    static final long FRONT_RAISE = 537L;

    @BeforeEach
    void iaSiempreRechazada() {
        reset(ia);
        when(ia.isEnabled()).thenReturn(true);
        when(ia.modelName()).thenReturn("simulado");
        when(ia.pedirRespuesta(any(), any(), any(), any(), anyInt()))
                .thenReturn(new ReplicateSessionFixGenerator.RawFix("{}", "salida-simulada"));
        when(ia.interpretar(anyString(), any())).thenThrow(new InvalidPlanDraftException(
                List.of("PRUEBA: propuesta rechazada para que decida el camino determinista de seguridad")));
    }

    @Test
    void sinAlternativa() {
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES ('sin-alternativa@pruebas.invalid', 'x', 'Prueba', now(), now())");
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = 'sin-alternativa@pruebas.invalid'",
                Long.class);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1990, 12, 1), BiologicalSex.MALE,
                new BigDecimal("178"), new BigDecimal("78"), null, FitnessLevel.INTERMEDIATE, GoalType.INCREASE_STRENGTH,
                "sin-alternativa", TrainingLocation.HOME, (short) 3, List.of((short) 1, (short) 3, (short) 5), (short) 45,
                List.of("body weight", "dumbbell", "band"), List.of(), "Ninguna", true, true, true, true, true));
        long plan = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        var ids = jdbc.queryForList("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ? ORDER BY"
                + " scheduled_date", Long.class, plan);
        colocar(ids.get(0), FRONT_RAISE);
        colocar(ids.get(ids.size() - 1), FRONT_RAISE);
        for (int i = 0; i < ids.size(); i++) hacer(u, ids.get(i), i == ids.size() - 1 ? FRONT_RAISE : -1);

        // El dolor de la semana se registra como lo hara el ciclo (idempotente: el ciclo no duplica la fila).
        dolor.registrarSemana(u, S1);
        assertEquals(Set.of(FRONT_RAISE), dolor.excluidosActivosDirectos(u), "premisa: 537 excluido por dolor");
        // Familia de 537 y sustitutos posibles fuera de ella, con la funcion de la politica.
        var familia = new TreeSet<Long>();
        catalogo.fetchEquivalentVariants(Set.of(FRONT_RAISE)).get(FRONT_RAISE).forEach(e -> familia.add(e.exerciseId()));
        familia.add(FRONT_RAISE);
        var original = catalogo.fetchAsCandidates(Set.of(FRONT_RAISE)).get(FRONT_RAISE);
        var ctx = planComandos.prepare(new GenerateWeeklyPlanCommand(u, S1.plusWeeks(1), null, true, null, null))
                .context();
        var enfoques = new HashSet<WorkoutFocus>();
        for (var w : List.of(ids.get(0), ids.get(ids.size() - 1)))
            enfoques.add(WorkoutFocus.valueOf(jdbc.queryForObject("SELECT focus_code FROM planned_workouts WHERE"
                    + " planned_workout_id = ?", String.class, w)));
        var sustitutos = new TreeSet<Long>();
        for (var c : ctx.availableExercises())
            for (var enfoque : enfoques)
                if (SafetySubstitutionPolicy.descarte(c, original, enfoque, Set.of(), Set.of(),
                        GoalType.INCREASE_STRENGTH.name(), false) == null)
                    sustitutos.add(c.exerciseId());
        assertFalse(sustitutos.isEmpty(), "premisa: con el catalogo completo SI hay sustitutos fuera de la familia");
        sustitutos.forEach(id -> assertFalse(familia.contains(id), "la familia ya no es elegible: " + id));
        // Catalogo reducido: ningun sustituto posible fuera de la familia.
        jdbc.update("UPDATE exercises SET is_active = false WHERE exercise_id IN (" + String.join(",",
                sustitutos.stream().map(String::valueOf).toList()) + ")");
        // La variante sigue ACTIVA en el catalogo: si algo la eligiera, se veria.
        assertTrue(jdbc.queryForObject("SELECT is_active FROM exercises WHERE exercise_id = 538", Boolean.class));

        var semana2 = ciclo.runFor(u, S1, S1.plusWeeks(1));
        var intento = jdbc.queryForMap("SELECT status, decision, rejection_reason, conservation_report::text AS"
                + " informe FROM plan_adaptation_attempts WHERE user_id = ? ORDER BY attempt_id DESC LIMIT 1", u);
        System.out.println("SIN_ALTERNATIVA publicado=" + semana2 + " intento=" + intento);

        // Nada de la familia ni nada desactivado en ningun plan activo.
        for (var p : jdbc.queryForList("SELECT plan_id FROM weekly_training_plans WHERE user_id = ? AND status ="
                + " 'ACTIVE'", Long.class, u)) {
            var ejercicios = jdbc.queryForList("SELECT pwe.exercise_id FROM planned_workout_exercises pwe JOIN"
                    + " planned_workouts pw ON pw.planned_workout_id = pwe.planned_workout_id WHERE pw.plan_id = ?",
                    Long.class, p);
            for (var e : ejercicios) assertFalse(familia.contains(e), "plan " + p + " con la familia: " + e);
            if (semana2.isPresent() && p.equals(semana2.get()))
                for (var e : ejercicios)
                    assertTrue(jdbc.queryForObject("SELECT is_active FROM exercises WHERE exercise_id = ?",
                            Boolean.class, e), "plan publicado con un ejercicio desactivado: " + e);
        }
        String informe = String.valueOf(intento.get("informe")) + " " + intento.get("rejection_reason");
        assertFalse(informe.matches("(?s).*\"puesto\":\\s*(" + String.join("|", familia.stream().map(String::valueOf)
                .toList()) + ")\\b.*"), "SUST-SEG-1 no pone ninguna variante: " + informe);
        // El caso quedo resuelto por el mecanismo seguro existente y con su causa.
        if (semana2.isEmpty())
            assertEquals("NO_FACTIBLE", intento.get("status"), "sin plan, NO_FACTIBLE con su causa: " + intento);
        else
            assertTrue(informe.contains("RETIRADA") || informe.contains("REGENERACION_SEGURA")
                    || informe.contains("\"sustituciones\": []"), "plan publicado por la via segura: " + informe);
        assertNotNull(intento.get("status"));
    }

    private void colocar(long plannedWorkoutId, long ejercicio) {
        long ultimo = jdbc.queryForObject("SELECT planned_exercise_id FROM planned_workout_exercises WHERE"
                + " planned_workout_id = ? ORDER BY exercise_order DESC LIMIT 1", Long.class, plannedWorkoutId);
        jdbc.update("UPDATE planned_workout_exercises SET exercise_id = ?, prescription_type = 'SETS_REPS',"
                + " planned_sets = 2, planned_reps = 10, planned_duration_seconds = NULL, target_load_kg = NULL,"
                + " rest_seconds = 90 WHERE planned_exercise_id = ?", ejercicio, ultimo);
    }

    private void hacer(long u, long w, long conDolor) {
        RelojPruebas.abrirVentanaDeEjecucion(jdbc, u);
        long s = sesiones.handle(new StartWorkoutSessionCommand(u, w)).orElseThrow().getId();
        for (var f : jdbc.queryForList("SELECT pwe.planned_exercise_id, pwe.exercise_id, pwe.prescription_type,"
                + " pwe.planned_sets, pwe.planned_reps, pwe.planned_duration_seconds FROM planned_workout_exercises pwe"
                + " WHERE pwe.planned_workout_id = ? ORDER BY pwe.exercise_order", w)) {
            long pe = ((Number) f.get("planned_exercise_id")).longValue();
            if (((Number) f.get("exercise_id")).longValue() == conDolor) {
                sesiones.handle(new RecordSessionExerciseCommand(u, s, pe, (short) 0, null, null, null,
                        SkipReason.PAIN_OR_DISCOMFORT));
                continue;
            }
            boolean duracion = "DURATION".equals(f.get("prescription_type"));
            int series = f.get("planned_sets") == null ? 1 : ((Number) f.get("planned_sets")).intValue();
            int total = series * ((Number) f.get(duracion ? "planned_duration_seconds" : "planned_reps")).intValue();
            sesiones.handle(new RecordSessionExerciseCommand(u, s, pe, (short) series, duracion ? null : total,
                    duracion ? total : null, null, null));
        }
        sesiones.handle(new FinishWorkoutSessionCommand(u, s, (short) 6, (short) 4, (short) 45));
    }
}
