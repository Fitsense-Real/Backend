package main.web.services.fitsense.planning;

import main.web.services.fitsense.planning.domain.exceptions.PlanGenerationFailedException;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.services.CapabilityPrescriptionPolicy;
import main.web.services.fitsense.planning.domain.services.PlanDraftValidator;
import main.web.services.fitsense.planning.domain.services.SessionDurationEstimator;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
import main.web.services.fitsense.planning.infrastructure.generation.rules.RuleBasedTrainingPlanGenerator;
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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * V20 EXACTO Y LAS DOS CORRECCIONES DE COMPOSICION (PROTOTIPOS_V20.md, decision del
 * 30/10/2026): los perfiles que el 70 % exacto rompia en la simulacion, reproducidos
 * con el generador y el validador reales, sin IA.
 * <ul>
 *   <li>GENERAL_FITNESS intermedio y avanzado, 4 dias en gimnasio, y principiante,
 *       6 dias en casa: completar el dia inferior con otro {@code upper legs} rompia
 *       V15. Ahora se sube antes una serie dentro de la dosis vigente.</li>
 *   <li>LOSE_WEIGHT principiante, 6 dias en casa: el relleno del miercoles se llevaba
 *       el tercer cardio y el jueves, pegado, se quedaba sin cardio por V18 (V25).
 *       Ahora el relleno no se lleva el ultimo cardio del dia siguiente pegado.</li>
 *   <li>LOSE_WEIGHT principiante, 4 y 5 dias en casa: con V37.1 se quedaban sin plan
 *       (3 cardios de casa y V18). Desde D1 (853 y 1117 como cardio revisado) publican.</li>
 * </ul>
 * Desde el 30/10/2026 (decision F) V37.1 es la V38 oficial y la candidata V38 es V39, ambas
 * en db/migration: el catalogo por omision es V39. Las copias de docs (v37/separables y
 * v38/candidata) son historicas y no se combinan con db/migration (se aplicarian dos veces).
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
@DisplayName("V20 exacto: los perfiles que rompia la simulacion")
class V20ComposicionCasosIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired PlanningContextFacade planning;
    @Autowired WeeklyTrainingPlanCommandService planComandos;
    @Autowired RuleBasedTrainingPlanGenerator motor;
    @Autowired SessionDurationEstimator estimador;
    @Autowired UserProfileCommandService perfiles;
    @Autowired JdbcTemplate jdbc;

    static final LocalDate S1 = TrainingWeek.containing(LocalDate.now()).startDate().plusWeeks(1);
    static final int SEMILLAS = 8;
    private int secuencia;

    @Test
    @DisplayName("GENERAL_FITNESS 4d gimnasio y 6d casa, LOSE_WEIGHT 6d casa: plan valido, todo >= 70 % exacto")
    void perfilesQueRompiaElSetentaExacto() {
        var fallos = new ArrayList<String>();
        for (var caso : List.of(
                new Caso(GoalType.GENERAL_FITNESS, FitnessLevel.INTERMEDIATE, 4, "GYM"),
                new Caso(GoalType.GENERAL_FITNESS, FitnessLevel.ADVANCED, 4, "GYM"),
                new Caso(GoalType.GENERAL_FITNESS, FitnessLevel.BEGINNER, 6, "CASA"),
                new Caso(GoalType.LOSE_WEIGHT, FitnessLevel.BEGINNER, 6, "CASA"))) {
            // La semilla del motor depende del usuario y la semana: varias por perfil.
            for (int semilla = 1; semilla <= SEMILLAS; semilla++) {
                long u = participante(caso);
                try {
                    planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null)
                            .orElseThrow(() -> new AssertionError("sin plan: " + caso));
                } catch (PlanGenerationFailedException e) {
                    fallos.add(caso + " semilla " + semilla + " -> " + e.getMessage() + borrador(u));
                    continue;
                }
                revisarBorrador(caso, u, fallos);
            }
        }
        assertEquals(List.of(), fallos);
    }

    @Test
    @DisplayName("LOSE_WEIGHT principiante 4d y 5d casa: publican con V37 y, desde D1 (853 y 1117 cardio), tambien con V37.1")
    void loseWeightCuatroCincoDiasCasa() {
        // Hasta D1 (30/10/2026) con V37.1 se quedaban sin plan por V20: solo tres cardios de casa
        // y V18. Con 853 fuera de revision (y 1117 con V38) hay cardio para los dias pegados.
        var fallos = new ArrayList<String>();
        for (int dias : new int[]{4, 5}) {
            var caso = new Caso(GoalType.LOSE_WEIGHT, FitnessLevel.BEGINNER, dias, "CASA");
            long u = participante(caso);
            try {
                planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null)
                        .orElseThrow(() -> new AssertionError("sin plan: " + caso));
                revisarBorrador(caso, u, fallos);
            } catch (PlanGenerationFailedException e) {
                fallos.add(caso + " -> " + e.getMessage());
            }
        }
        assertEquals(List.of(), fallos);
    }

    /**
     * Sobre el borrador del motor (determinista): cada sesion llega a los segundos
     * exactos del piso, ningun grupo pasa de la mitad (V15), no se repite el dia
     * anterior pegado (V18) y en LOSE_WEIGHT cada sesion lleva cardio.
     */
    private void revisarBorrador(Caso caso, long u, List<String> fallos) {
        var ctx = planComandos.prepare(new GenerateWeeklyPlanCommand(u, S1, null, true, null, null)).context();
        var borrador = motor.generate(ctx, List.of());
        long piso = PlanDraftValidator.minimumSessionSeconds(ctx);
        assertTrue(piso > 0, caso.toString());
        Map<Long, CandidateExercise> elegibles = ctx.availableExercises().stream()
                .collect(Collectors.toMap(CandidateExercise::exerciseId, Function.identity(), (a, b) -> a));
        PlanDraft.DraftWorkout anterior = null;
        for (var w : borrador.workouts()) {
            long seg = estimador.estimateSeconds(w, ctx.prescription());
            if (seg < piso) fallos.add(caso + " " + w.scheduledDate() + ": " + seg + " s < " + piso + " s");
            if (caso.objetivo() == GoalType.LOSE_WEIGHT && w.exercises().stream()
                    .map(e -> elegibles.get(e.exerciseId()))
                    .noneMatch(c -> c != null && CapabilityPrescriptionPolicy.claseDe(c)
                            == CapabilityPrescriptionPolicy.Clase.CARDIO))
                fallos.add(caso + " " + w.scheduledDate() + ": sin cardio");
            if (anterior != null && anterior.scheduledDate().plusDays(1).equals(w.scheduledDate())) {
                var ayer = anterior.exercises().stream().map(PlanDraft.DraftExercise::exerciseId).collect(Collectors.toSet());
                w.exercises().stream().filter(e -> ayer.contains(e.exerciseId()))
                        .forEach(e -> fallos.add(caso + " " + w.scheduledDate() + ": repite " + e.exerciseId() + " (V18)"));
            }
            anterior = w;
        }
    }

    /** El borrador del motor, sesion a sesion, para leer un fallo. */
    private String borrador(long u) {
        var ctx = planComandos.prepare(new GenerateWeeklyPlanCommand(u, S1, null, true, null, null)).context();
        Map<Long, CandidateExercise> elegibles = ctx.availableExercises().stream()
                .collect(Collectors.toMap(CandidateExercise::exerciseId, Function.identity(), (a, b) -> a));
        var sb = new StringBuilder();
        for (var w : motor.generate(ctx, List.of()).workouts()) {
            sb.append("\n  ").append(w.scheduledDate()).append(' ').append(w.focus()).append(' ')
                    .append(estimador.estimateSeconds(w, ctx.prescription())).append(" s:");
            for (var e : w.exercises()) {
                var c = elegibles.get(e.exerciseId());
                sb.append(' ').append(e.exerciseId()).append('[').append(c == null ? "?" : c.bodyPartCode() + "/"
                        + CapabilityPrescriptionPolicy.claseDe(c)).append("] ").append(e.plannedSets()).append('x')
                        .append(e.plannedReps() != null ? e.plannedReps() + "r" : e.plannedDurationSeconds() + "s")
                        .append('/').append(e.restSeconds());
            }
        }
        return sb.toString();
    }

    record Caso(GoalType objetivo, FitnessLevel nivel, int dias, String lugar) {
        @Override public String toString() { return objetivo + "/" + nivel + "/" + dias + "d/45/" + lugar; }
    }

    private long participante(Caso caso) {
        String email = "v20casos-" + (++secuencia) + "-" + System.nanoTime() + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Prueba', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        var disponibles = new ArrayList<Short>();
        for (short d = 1; d <= caso.dias(); d++) disponibles.add(d);
        boolean gym = "GYM".equals(caso.lugar());
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1992, 1, 15), BiologicalSex.FEMALE,
                new BigDecimal("165"), new BigDecimal("70"), null, caso.nivel(), caso.objetivo(), "v20",
                gym ? TrainingLocation.GYM : TrainingLocation.HOME, (short) caso.dias(), disponibles, (short) 45,
                gym ? GeneracionReferenciaBase.GIMNASIO : GeneracionReferenciaBase.CASA,
                List.of(), "Ninguna", true, true, true, true, true));
        return u;
    }
}
