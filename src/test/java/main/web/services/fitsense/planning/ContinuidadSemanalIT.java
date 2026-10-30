package main.web.services.fitsense.planning;

import main.web.services.fitsense.execution.domain.model.commands.FinishWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.model.commands.RecordSessionExerciseCommand;
import main.web.services.fitsense.execution.domain.model.commands.StartWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.services.WorkoutSessionCommandService;
import main.web.services.fitsense.orchestration.application.internal.WeeklyCycleService;
import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.domain.services.CapabilityCoveragePolicy;
import main.web.services.fitsense.planning.domain.services.PlanDraftValidator;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
import main.web.services.fitsense.planning.infrastructure.generation.ai.ReplicateSessionFixGenerator;
import main.web.services.fitsense.planning.interfaces.acl.PlanningContextFacade;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.BiologicalSex;
import main.web.services.fitsense.profiling.domain.model.valueobjects.FitnessLevel;
import main.web.services.fitsense.profiling.domain.model.valueobjects.GoalType;
import main.web.services.fitsense.profiling.domain.model.valueobjects.TrainingLocation;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import main.web.services.fitsense.shared.domain.model.valueobjects.TrainingWeek;
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
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * CONTINUIDAD SEMANAL: un plan nuevo continua a la semana siguiente con el ciclo
 * real ({@link WeeklyCycleService#runFor}): copia V31 validada por la adaptacion
 * P2-D. V15 cuenta como disponibles los mismos grupos en la generacion y en la
 * copia (ver {@code PlanDraftValidator.prescribibleParaV15}); antes, los 8 planes
 * de LOSE_WEIGHT de 5-6 dias que recupero la correccion del respaldo terminaban
 * en NO_FACTIBLE en la semana 2 porque la copia exigia gemelos.
 * <p>
 * Progresion apagada (valor por defecto) y la validacion de capacidades de la
 * adaptacion sin tocar. IA simulada: nunca se llama a Replicate.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
@DisplayName("Continuidad semanal: generacion y copia con el mismo V15")
class ContinuidadSemanalIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @MockitoBean ReplicateSessionFixGenerator ia;

    @Autowired WeeklyCycleService ciclo;
    @Autowired PlanningContextFacade planning;
    @Autowired WeeklyTrainingPlanCommandService planComandos;
    @Autowired PlanDraftValidator validadorGeneracion;
    @Autowired UserProfileCommandService perfiles;
    @Autowired WorkoutSessionCommandService sesiones;
    @Autowired JdbcTemplate jdbc;

    static final LocalDate S1 = TrainingWeek.containing(LocalDate.now()).startDate().plusWeeks(1);
    static final AtomicInteger SECUENCIA = new AtomicInteger();

    @Test
    @DisplayName("los 8 LOSE_WEIGHT de 5-6 dias: semana 1 valida y completa, semana 2 publicada y valida")
    void loseWeightCincoSeisDias() {
        assertEquals("ENFORCE", validadorGeneracion.capabilityEnforcement().name(), "la generacion de produccion");
        var fallos = new ArrayList<String>();
        int casos = 0;
        for (var nivel : List.of(FitnessLevel.INTERMEDIATE, FitnessLevel.ADVANCED))
            for (int dias : new int[]{5, 6})
                for (var lugar : List.of("GYM", "CASA")) {
                    casos++;
                    String perfil = "LOSE_WEIGHT/" + nivel + "/" + dias + "d/" + lugar;
                    long u = participante(GoalType.LOSE_WEIGHT, nivel, dias, lugar);
                    long w1 = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null)
                            .orElseThrow(() -> new AssertionError("sin plan inicial: " + perfil));
                    // 1-2: semana 1 valida con capacidades principales cubiertas.
                    revisar(perfil + " semana 1", u, S1, w1, fallos);
                    cumplirTodo(u, w1);
                    var r = ciclo.runFor(u, S1, S1.plusWeeks(1));
                    // 3-4: publica la semana 2, sin NO_FACTIBLE.
                    var intento = ultimo(u);
                    if (r.isEmpty()) {
                        fallos.add(perfil + " -> " + intento);
                        continue;
                    }
                    assertEquals("SIN_ORDEN", intento.get("status"), perfil);
                    assertEquals("CONSERVED", intento.get("generation_source"), perfil);
                    // 5: la semana 2 cumple las mismas reglas que la generacion (con capacidades en ENFORCE).
                    revisar(perfil + " semana 2", u, S1.plusWeeks(1), r.get(), fallos);
                }
        assertEquals(8, casos);
        assertEquals(List.of(), fallos);
        verify(ia, never()).pedirRespuesta(any(), any(), any(), any(), anyInt());
    }

    @Test
    @DisplayName("96 perfiles (4 objetivos x 3 niveles x 3-6 dias x gimnasio/casa): todos publican la semana 2")
    void noventaYSeisPerfiles() {
        var fallos = new ArrayList<String>();
        int ciclos = 0;
        for (var objetivo : GoalType.values())
            for (var nivel : FitnessLevel.values())
                for (int dias : new int[]{3, 4, 5, 6})
                    for (var lugar : List.of("GYM", "CASA")) {
                        boolean cumple = ciclos++ % 2 == 0;
                        String perfil = objetivo + "/" + nivel + "/" + dias + "d/" + lugar
                                + (cumple ? " cumple" : " sin registros");
                        long u = participante(objetivo, nivel, dias, lugar);
                        long w1;
                        try {
                            w1 = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null)
                                    .orElseThrow(() -> new AssertionError("sin plan inicial: " + perfil));
                        } catch (RuntimeException e) {
                            // Se anota y se sigue: el informe lista todos los perfiles sin plan, no solo el primero.
                            fallos.add(perfil + " -> sin plan inicial: " + e.getMessage());
                            continue;
                        }
                        if (cumple) cumplirTodo(u, w1);
                        var r = ciclo.runFor(u, S1, S1.plusWeeks(1));
                        var intento = ultimo(u);
                        if (r.isEmpty() || !"SIN_ORDEN".equals(intento.get("status"))
                                || !"CONSERVED".equals(intento.get("generation_source")))
                            fallos.add(perfil + " -> " + intento);
                    }
        assertEquals(96, ciclos);
        assertEquals(List.of(), fallos, "semanas siguientes no publicadas");
        verify(ia, never()).pedirRespuesta(any(), any(), any(), any(), anyInt());
    }

    // =======================================================================
    // Material
    // =======================================================================

    /** La semana con el validador de la GENERACION y la cobertura de capacidades del objetivo. */
    private void revisar(String etiqueta, long u, LocalDate semana, long plan, List<String> fallos) {
        var ctx = planComandos.prepare(new GenerateWeeklyPlanCommand(u, semana, null, true, null, null)).context();
        var borrador = borrador(plan);
        try {
            validadorGeneracion.validate(borrador, ctx, 30);
        } catch (InvalidPlanDraftException e) {
            fallos.add(etiqueta + " no valida: " + e.problems());
        }
        var cobertura = CapabilityCoveragePolicy.evaluar(ctx.profile().goalType(), ctx.profile().fitnessLevel(),
                borrador, ctx.availableExercises(), ctx.prescription().repLimits());
        if (cobertura.resultado() != CapabilityCoveragePolicy.Resultado.COMPLETO)
            fallos.add(etiqueta + " cobertura " + cobertura.resultado());
    }

    private PlanDraft borrador(long plan) {
        var sesionesPlan = new ArrayList<PlanDraft.DraftWorkout>();
        for (var f : jdbc.queryForList("SELECT planned_workout_id, scheduled_date, focus_code, workout_name,"
                + " expected_duration_minutes FROM planned_workouts WHERE plan_id = ? ORDER BY scheduled_date", plan)) {
            long id = ((Number) f.get("planned_workout_id")).longValue();
            var ejercicios = jdbc.query("SELECT exercise_id, prescription_type, planned_sets, planned_reps,"
                            + " planned_duration_seconds, target_load_kg, rest_seconds FROM planned_workout_exercises"
                            + " WHERE planned_workout_id = ? ORDER BY exercise_order",
                    (rs, k) -> new PlanDraft.DraftExercise(rs.getLong(1), PrescriptionType.valueOf(rs.getString(2)),
                            rs.getObject(3, Short.class), rs.getObject(4, Short.class), rs.getObject(5, Integer.class),
                            rs.getBigDecimal(6), rs.getObject(7, Short.class), null), id);
            sesionesPlan.add(new PlanDraft.DraftWorkout(((java.sql.Date) f.get("scheduled_date")).toLocalDate(),
                    WorkoutFocus.valueOf((String) f.get("focus_code")), (String) f.get("workout_name"),
                    ((Number) f.get("expected_duration_minutes")).intValue(), ejercicios));
        }
        return new PlanDraft(GenerationSource.RULE_ENGINE, null, "continuidad", null, "r", sesionesPlan);
    }

    private long participante(GoalType objetivo, FitnessLevel nivel, int dias, String lugar) {
        String email = "continuidad-" + SECUENCIA.incrementAndGet() + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Prueba', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        var disponibles = new ArrayList<Short>();
        if (dias == 3) disponibles.addAll(List.of((short) 1, (short) 3, (short) 5));
        else for (short d = 1; d <= dias; d++) disponibles.add(d);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1992, 1, 15), BiologicalSex.FEMALE,
                new BigDecimal("165"), new BigDecimal("70"), null, nivel, objetivo, "continuidad",
                "GYM".equals(lugar) ? TrainingLocation.GYM : TrainingLocation.HOME, (short) dias, disponibles,
                (short) 45, "GYM".equals(lugar) ? GeneracionReferenciaIT.GIMNASIO : GeneracionReferenciaIT.CASA,
                List.of(), "Ninguna", true, true, true, true, true));
        return u;
    }

    /** Cumple todas las sesiones del plan con lo prescrito. */
    private void cumplirTodo(long u, long plan) {
        for (long w : jdbc.queryForList("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ?"
                + " ORDER BY scheduled_date", Long.class, plan)) {
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
    }

    private Map<String, Object> ultimo(long u) {
        return jdbc.queryForMap("SELECT decision, status, generation_source, coalesce(rejection_reason, '') AS motivo"
                + " FROM plan_adaptation_attempts WHERE user_id = ? ORDER BY attempt_id DESC LIMIT 1", u);
    }
}
