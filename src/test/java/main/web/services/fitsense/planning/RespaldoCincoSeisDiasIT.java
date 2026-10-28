package main.web.services.fitsense.planning;

import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.domain.services.CapabilityPrescriptionPolicy;
import main.web.services.fitsense.planning.domain.services.CatalogEligibilityPolicy;
import main.web.services.fitsense.planning.domain.services.PlanDraftValidator;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
import main.web.services.fitsense.planning.infrastructure.generation.rules.RuleBasedTrainingPlanGenerator;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.BiologicalSex;
import main.web.services.fitsense.profiling.domain.model.valueobjects.FitnessLevel;
import main.web.services.fitsense.profiling.domain.model.valueobjects.GoalType;
import main.web.services.fitsense.profiling.domain.model.valueobjects.TrainingLocation;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RESPALDO DE 5 Y 6 DIAS. Los 12 perfiles que en la evaluacion del 28/10/2026
 * (EVALUACION_GENERACION_4_6_DIAS.md) se quedaban SIN PLAN: el motor de reglas
 * armaba sesiones de traccion o de piernas con un solo grupo y V15 las
 * rechazaba. El motor de reglas es el respaldo cuando la IA falla: tiene que
 * producir una semana valida con el conjunto elegible que existe, y cuando no
 * puede, la prueba demuestra por que (CORRECCION_RESPALDO_5_6_DIAS.md).
 * <p>
 * Se prueba el generador directamente (sin IA, sin Replicate) y su borrador se
 * pasa por el MISMO validador de produccion, sin tocar V15 ni V20. La semana es
 * fija para que la semilla -usuario y semana- sea reproducible.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
@DisplayName("Respaldo · el motor de reglas genera semanas validas de 5 y 6 dias")
class RespaldoCincoSeisDiasIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired WeeklyTrainingPlanCommandService planComandos;
    @Autowired RuleBasedTrainingPlanGenerator reglas;
    @Autowired UserProfileCommandService perfiles;
    @Autowired PlanDraftValidator validador;
    @Autowired JdbcTemplate jdbc;

    /** La semana de la evaluacion: la semilla del motor de reglas depende de ella. */
    static final LocalDate SEMANA = LocalDate.of(2026, 11, 2);
    static final AtomicInteger SECUENCIA = new AtomicInteger();
    static final int SEMILLAS = 4;

    /**
     * Los 4 de GENERAL_FITNESS (y los mismos en gimnasio, que fallaban con otras
     * semillas): la traccion se quedaba en {@code back} y un estiramiento de
     * triceps podia entrar en PULL (V22). Ahora llevan biceps y nada de triceps.
     */
    @ParameterizedTest(name = "{0}/{1}/{2}d/{3}")
    @CsvSource({
            "GENERAL_FITNESS, INTERMEDIATE, 5, CASA",
            "GENERAL_FITNESS, INTERMEDIATE, 6, CASA",
            "GENERAL_FITNESS, ADVANCED, 5, CASA",
            "GENERAL_FITNESS, ADVANCED, 6, CASA",
            "GENERAL_FITNESS, INTERMEDIATE, 6, GYM",
            "GENERAL_FITNESS, ADVANCED, 6, GYM",
    })
    @DisplayName("GENERAL_FITNESS 5-6 dias, 4 semillas: semana valida, cada sesion en dos grupos de su enfoque")
    void semanaValida(GoalType objetivo, FitnessLevel nivel, int dias, String lugar) {
        // La semilla del motor depende del usuario: cuatro usuarios, cuatro semillas.
        for (int semilla = 0; semilla < SEMILLAS; semilla++) semanaValidaCon(objetivo, nivel, dias, lugar);
    }

    private void semanaValidaCon(GoalType objetivo, FitnessLevel nivel, int dias, String lugar) {
        var ctx = contexto(objetivo, nivel, dias, lugar);
        var borrador = reglas.generate(ctx, List.of());
        var descripcion = describir(borrador, ctx, objetivo);

        assertEquals(dias, borrador.workouts().size(), "una sesion por dia pedido\n" + descripcion);
        try {
            validador.validate(borrador, ctx, 30);
        } catch (InvalidPlanDraftException e) {
            fail("el respaldo no pasa la validacion: " + e.problems() + "\n" + descripcion);
        }
        var porId = porId(ctx);
        var complementos = CatalogEligibilityPolicy.complementos(ctx.availableExercises(), objetivo.name());
        for (var w : borrador.workouts()) {
            var grupos = w.exercises().stream().filter(e -> !complementos.contains(e.exerciseId()))
                    .map(e -> porId.get(e.exerciseId()).bodyPartCode()).filter(w.focus()::accepts)
                    .collect(Collectors.toSet());
            assertTrue(grupos.size() >= 2, w.focus() + " con un solo grupo\n" + descripcion);
            // El cambio pone trabajo con dosis del objetivo, no relleno: ninguna
            // colocacion sin capacidad acreditable (V24/V25 ya lo exigen, se repite aqui).
            for (var e : w.exercises())
                assertFalse(CapabilityPrescriptionPolicy.admisibles(porId.get(e.exerciseId()), objetivo.name(),
                        nivel.name()).isEmpty(), "ejercicio sin capacidad del objetivo: " + e.exerciseId());
        }
    }

    /**
     * Los 8 de LOSE_WEIGHT: la traccion ya sale bien, pero las sesiones LEGS no
     * tienen solucion con las reglas vigentes y NO se fuerzan:
     * <ul>
     *   <li>V15 cuenta {@code lower legs} como grupo disponible (hay gemelos
     *       elegibles) y exige dos grupos en LEGS;</li>
     *   <li>en LOSE_WEIGHT la fuerza es la unica capacidad resistida (4A v6), y
     *       su dosis solo existe para estructurales (4D); todo {@code lower legs}
     *       es RESISTIDO (flexion plantar) y no tiene ninguna capacidad
     *       admisible.</li>
     * </ul>
     * La prueba fija las dos mitades de la contradiccion para que una decision
     * posterior (V15, la politica de capacidades o la division) la cambie a
     * sabiendas.
     */
    @ParameterizedTest(name = "{0}/{1}/{2}d/{3}")
    @CsvSource({
            "LOSE_WEIGHT, INTERMEDIATE, 5, GYM",
            "LOSE_WEIGHT, INTERMEDIATE, 5, CASA",
            "LOSE_WEIGHT, INTERMEDIATE, 6, GYM",
            "LOSE_WEIGHT, INTERMEDIATE, 6, CASA",
            "LOSE_WEIGHT, ADVANCED, 5, GYM",
            "LOSE_WEIGHT, ADVANCED, 5, CASA",
            "LOSE_WEIGHT, ADVANCED, 6, GYM",
            "LOSE_WEIGHT, ADVANCED, 6, CASA",
    })
    @DisplayName("LOSE_WEIGHT 5-6 dias: solo queda LEGS, imposible sin cambiar una regla")
    void loseWeightPiernasSinSolucion(GoalType objetivo, FitnessLevel nivel, int dias, String lugar) {
        var ctx = contexto(objetivo, nivel, dias, lugar);
        var borrador = reglas.generate(ctx, List.of());
        var descripcion = describir(borrador, ctx, objetivo);
        var e = assertThrows(InvalidPlanDraftException.class, () -> validador.validate(borrador, ctx, 30));
        for (var p : e.problems())
            assertTrue(p.startsWith("V15:") && p.contains("enfoque LEGS") && p.contains("solo trabaja [upper legs]"),
                    "queda un problema que no es el de LEGS: " + p + "\n" + descripcion);

        var complementos = CatalogEligibilityPolicy.complementos(ctx.availableExercises(), objetivo.name());
        var gemelos = ctx.availableExercises().stream().filter(c -> "lower legs".equals(c.bodyPartCode()))
                .filter(c -> !complementos.contains(c.exerciseId())).toList();
        assertFalse(gemelos.isEmpty(), "V15 los cuenta como grupo disponible");
        for (var c : gemelos)
            assertTrue(CapabilityPrescriptionPolicy.admisibles(c, objetivo.name(), nivel.name()).isEmpty(),
                    "hay un gemelo prescribible y el generador no lo usa: " + c.exerciseId() + " " + c.name());
    }

    private PlanGenerationContext contexto(GoalType objetivo, FitnessLevel nivel, int dias, String lugar) {
        String email = "respaldo" + SECUENCIA.incrementAndGet() + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Prueba', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        var disponibles = new ArrayList<Short>();
        for (short d = 1; d <= dias; d++) disponibles.add(d);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1992, 1, 15), BiologicalSex.FEMALE,
                new BigDecimal("165"), new BigDecimal("70"), null, nivel, objetivo, "respaldo",
                "GYM".equals(lugar) ? TrainingLocation.GYM : TrainingLocation.HOME, (short) dias, disponibles,
                (short) 45, "GYM".equals(lugar) ? GeneracionReferenciaIT.GIMNASIO : GeneracionReferenciaIT.CASA,
                List.of(), "Ninguna", true, true, true, true, true));
        return planComandos.prepare(new GenerateWeeklyPlanCommand(u, SEMANA, null, true, null, null)).context();
    }

    private static Map<Long, CandidateExercise> porId(PlanGenerationContext ctx) {
        return ctx.availableExercises().stream()
                .collect(Collectors.toMap(CandidateExercise::exerciseId, c -> c, (a, b) -> a));
    }

    /** Por sesion: enfoque y cada ejercicio con su grupo y su patron. */
    static String describir(PlanDraft borrador, PlanGenerationContext ctx, GoalType objetivo) {
        var porId = porId(ctx);
        var complementos = CatalogEligibilityPolicy.complementos(ctx.availableExercises(), objetivo.name());
        var sb = new StringBuilder();
        for (var w : borrador.workouts()) {
            sb.append("  ").append(w.scheduledDate().getDayOfWeek()).append(' ').append(w.focus()).append(':');
            for (var e : w.exercises()) {
                var c = porId.get(e.exerciseId());
                sb.append(' ').append(e.exerciseId()).append('[')
                        .append(c == null ? "?" : c.bodyPartCode() + "/" + c.movementPattern())
                        .append(complementos.contains(e.exerciseId()) ? "/comp" : "").append(']');
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}
