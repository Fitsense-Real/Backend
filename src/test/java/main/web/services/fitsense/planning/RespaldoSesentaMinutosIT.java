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
 * RESPALDO DE 60 MINUTOS CON 5 Y 6 DIAS (campana MVP del 30/10/2026, usuario U06).
 * <p>
 * U06 (GAIN_MUSCLE avanzado, gimnasio, lunes, miercoles, jueves, sabado y domingo, 60 min) se
 * quedo SIN PLAN INICIAL: la IA agoto sus intentos (un timeout del proveedor) y el motor de reglas,
 * que es el respaldo, no supero V15 (dia LEGS solo con upper legs) ni V20 (domingo corto). Los
 * barridos previos solo cubrian 45 min y dias seguidos. Motor de reglas, sin IA, semana de la
 * campana (2/11/2026).
 * <p>
 * DIAGNOSTICO: no es un fallo del motor, es un CONFLICTO DE POLITICAS aprobadas en GAIN_MUSCLE a
 * 60 min con 5-6 dias. 4D v3.1 fija la dosis (estructural 3x10/90 ~364 s; resistido de zona
 * pequena 2x13/90 ~262 s) y un tope de 7 colocaciones; V20 pide 2520 s; V15 pide dos grupos y, en
 * LOWER_BODY, ninguno por encima de la mitad. LEGS/PULL con dos grupos: 6 x 364 + 262 = 2448 s.
 * LOWER_BODY con la mitad: 3 x 364 + 4 x 262 = 2142 s. Ninguna composicion valida llega al piso.
 * Pendiente de decision (HALLAZGOS_Y_CORRECCIONES_MVP.md): se reproduce con
 * {@code -Dfitsense.pendientes=true}. Los otros tres objetivos son regresion normal.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
@DisplayName("Respaldo de reglas: 5 y 6 dias de 60 minutos publican plan")
class RespaldoSesentaMinutosIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired PlanningContextFacade planning;
    @Autowired WeeklyTrainingPlanCommandService planComandos;
    @Autowired RuleBasedTrainingPlanGenerator motor;
    @Autowired SessionDurationEstimator estimador;
    @Autowired UserProfileCommandService perfiles;
    @Autowired JdbcTemplate jdbc;

    static final LocalDate S1 = LocalDate.of(2026, 11, 2);
    private int secuencia;

    record Caso(GoalType objetivo, FitnessLevel nivel, List<Short> dias, int minutos, String lugar) {
        @Override public String toString() { return objetivo + "/" + nivel + "/" + dias + "/" + minutos + "/" + lugar; }
    }

    @Test
    @org.junit.jupiter.api.condition.EnabledIfSystemProperty(named = "fitsense.pendientes", matches = "true")
    @DisplayName("U06: GAIN_MUSCLE avanzado, gimnasio, 5 dias no seguidos, 60 min")
    void usuarioU06() {
        var fallos = new ArrayList<String>();
        generar(new Caso(GoalType.GAIN_MUSCLE, FitnessLevel.ADVANCED, dias(1, 3, 4, 6, 7), 60, "GYM"), fallos);
        assertEquals(List.of(), fallos);
    }

    @Test
    @DisplayName("Barrido de 60 min sin GAIN_MUSCLE: 3 objetivos x 3 niveles x 5 y 6 dias (seguidos y como U06/U07) x gimnasio y casa")
    void barridoSesentaMinutos() {
        barrer(List.of(GoalType.LOSE_WEIGHT, GoalType.INCREASE_STRENGTH, GoalType.GENERAL_FITNESS));
    }

    @Test
    @org.junit.jupiter.api.condition.EnabledIfSystemProperty(named = "fitsense.pendientes", matches = "true")
    @DisplayName("PENDIENTE: GAIN_MUSCLE a 60 min con 5 y 6 dias (conflicto 4D v3.1 / V15 / V20)")
    void gainMuscleSesentaMinutos() {
        barrer(List.of(GoalType.GAIN_MUSCLE));
    }

    private void barrer(List<GoalType> objetivos) {
        var fallos = new ArrayList<String>();
        int n = 0;
        for (var objetivo : objetivos)
            for (var nivel : FitnessLevel.values())
                for (var d : List.of(dias(1, 2, 3, 4, 5), dias(1, 3, 4, 6, 7), dias(1, 2, 3, 4, 5, 6), dias(1, 2, 3, 5, 6, 7)))
                    for (var lugar : List.of("GYM", "CASA")) {
                        generar(new Caso(objetivo, nivel, d, 60, lugar), fallos);
                        n++;
                    }
        System.out.println("BARRIDO_60 " + (n - fallos.size()) + "/" + n + " con plan");
        fallos.forEach(f -> System.out.println("BARRIDO_60_FALLO " + f));
        assertEquals(List.of(), fallos);
    }

    private static List<Short> dias(int... d) {
        var l = new ArrayList<Short>();
        for (int x : d) l.add((short) x);
        return List.copyOf(l);
    }

    private void generar(Caso caso, List<String> fallos) {
        long u = participante(caso);
        try {
            planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null)
                    .orElseThrow(() -> new AssertionError("sin plan: " + caso));
        } catch (PlanGenerationFailedException e) {
            fallos.add(caso + " -> " + e.getMessage() + borrador(u));
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
                        + c.movementPattern() + "/" + CapabilityPrescriptionPolicy.claseDe(c)).append("] ")
                        .append(e.plannedSets()).append('x')
                        .append(e.plannedReps() != null ? e.plannedReps() + "r" : e.plannedDurationSeconds() + "s")
                        .append('/').append(e.restSeconds());
            }
        }
        return sb.toString();
    }

    private long participante(Caso caso) {
        String email = "r60-" + (++secuencia) + "-" + System.nanoTime() + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Prueba', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        boolean gym = "GYM".equals(caso.lugar());
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1992, 1, 15), BiologicalSex.FEMALE,
                new BigDecimal("165"), new BigDecimal("70"), null, caso.nivel(), caso.objetivo(), "r60",
                gym ? TrainingLocation.GYM : TrainingLocation.HOME, (short) caso.dias().size(), caso.dias(),
                (short) caso.minutos(), gym ? GeneracionReferenciaBase.GIMNASIO : GeneracionReferenciaBase.CASA,
                List.of(), "Ninguna", true, true, true, true, true));
        return u;
    }
}
