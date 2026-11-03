package main.web.services.fitsense.planning.infrastructure.generation.rules;

import main.web.services.fitsense.planning.domain.exceptions.PlanGenerationFailedException;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.GoalCapabilityPolicy.Capability;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.domain.model.valueobjects.WorkoutFocus;
import main.web.services.fitsense.planning.domain.services.CapabilityPrescriptionPolicy;
import main.web.services.fitsense.planning.domain.services.CapabilityPrescriptionPolicy.Clase;
import main.web.services.fitsense.planning.domain.services.SessionDurationEstimator;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
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
 * E1/E2 (decision E del 30/10/2026, INFORME_FINAL_V39.md): los dos casos que dejaban sin plan
 * a V38 + V39, con el generador y el validador de produccion.
 * <ul>
 *   <li>GENERAL_FITNESS principiante, 4-6 dias: el dia de pierna con tres {@code upper legs}
 *       se corrige por la mitad de V15 (E2, motivo MITAD).</li>
 *   <li>LOSE_WEIGHT intermedio y avanzado, 3 dias en casa: el FULL_BODY con un solo grupo se
 *       corrige (E1, motivo GRUPOS).</li>
 * </ul>
 * Cada cambio registrado ({@link CapabilityWeekBuilder#cambiosGrupo()}) tiene que conservar la
 * capacidad, entrar como trabajo resistido o estructural (nunca movilidad ni cardio; una fuerza
 * entra como estructural, no como relleno abdominal), cambiar de grupo, y la semana tiene que
 * publicarse con V15, V18, V20, V23, V24 y V25 en vigor.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
@DisplayName("E1/E2: la correccion de grupos conserva capacidad y publica")
class ComposicionE1E2IT {

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
    static final List<String> GYM = List.of("body weight", "dumbbell", "barbell", "cable", "leverage machine",
            "bench", "band", "stationary bike", "treadmill", "kettlebell", "ez barbell", "smith machine");
    static final List<String> CASA = List.of("body weight", "dumbbell", "band");
    static final int SEMILLAS = 4;
    private static int secuencia;

    record Caso(GoalType objetivo, FitnessLevel nivel, int dias, boolean gym) {
        @Override public String toString() { return objetivo + "/" + nivel + "/" + dias + "d/" + (gym ? "GYM" : "CASA"); }
    }

    @Test
    @DisplayName("GENERAL_FITNESS principiante (pierna, E2) y LOSE_WEIGHT int./av. 3d casa (FULL_BODY, E1)")
    void casosQueOriginaronElProblema() {
        var casos = new ArrayList<Caso>();
        for (int dias : new int[]{4, 5, 6})
            for (boolean gym : new boolean[]{true, false})
                casos.add(new Caso(GoalType.GENERAL_FITNESS, FitnessLevel.BEGINNER, dias, gym));
        casos.add(new Caso(GoalType.LOSE_WEIGHT, FitnessLevel.INTERMEDIATE, 3, false));
        casos.add(new Caso(GoalType.LOSE_WEIGHT, FitnessLevel.ADVANCED, 3, false));

        var fallos = new ArrayList<String>();
        var motivos = new EnumMap<GoalType, Set<String>>(GoalType.class);
        for (var caso : casos)
            for (int semilla = 1; semilla <= SEMILLAS; semilla++) {
                long u = participante(caso);
                var ctx = planComandos.prepare(new GenerateWeeklyPlanCommand(u, S1, null, true, null, null)).context();
                var constructor = new CapabilityWeekBuilder(ctx, estimador, motor.seedFor(ctx));
                var borrador = constructor.construir(motor::finalizar);
                assertEquals(motor.generate(ctx, List.of()), borrador, "el constructor es el del motor");
                Map<Long, CandidateExercise> el = ctx.availableExercises().stream()
                        .collect(Collectors.toMap(CandidateExercise::exerciseId, Function.identity(), (a, b) -> a));
                for (var c : constructor.cambiosGrupo()) {
                    String id = caso + " s" + semilla + " " + c.fecha() + " " + c.motivo() + " " + c.quitado() + "->" + c.elegido();
                    motivos.computeIfAbsent(caso.objetivo(), k -> new TreeSet<>()).add(c.motivo() + "/" + c.enfoque());
                    if (c.capacidadElegida() != c.capacidadQuitada())
                        fallos.add(id + ": capacidad " + c.capacidadQuitada() + " -> " + c.capacidadElegida());
                    var clase = CapabilityPrescriptionPolicy.claseDe(el.get(c.elegido()));
                    if (clase != Clase.ESTRUCTURAL && clase != Clase.RESISTIDO)
                        fallos.add(id + ": entra como " + clase);
                    if (c.capacidadQuitada() == Capability.STRENGTH && clase != Clase.ESTRUCTURAL)
                        fallos.add(id + ": una fuerza no puede pasar a " + clase);
                    if (c.grupoElegido().equals(c.grupoQuitado())) fallos.add(id + ": mismo grupo");
                }
                try {
                    planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null)
                            .orElseThrow(() -> new AssertionError("sin plan"));
                } catch (PlanGenerationFailedException e) {
                    fallos.add(caso + " s" + semilla + " no publica: " + e.getMessage());
                }
            }
        assertEquals(List.of(), fallos);
        // Los dos mecanismos tienen que haber actuado en los casos que los originaron.
        assertTrue(motivos.getOrDefault(GoalType.GENERAL_FITNESS, Set.of()).stream().anyMatch(m -> m.startsWith("MITAD")),
                "E2 en GENERAL_FITNESS: " + motivos);
        assertTrue(motivos.getOrDefault(GoalType.LOSE_WEIGHT, Set.of())
                        .contains("GRUPOS/" + WorkoutFocus.FULL_BODY), "E1 en LOSE_WEIGHT FULL_BODY: " + motivos);
    }

    private long participante(Caso caso) {
        String email = "e1e2-" + (++secuencia) + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Prueba', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        var disponibles = new ArrayList<Short>();
        if (caso.dias() == 3) disponibles.addAll(List.of((short) 1, (short) 3, (short) 5));
        else for (short d = 1; d <= caso.dias(); d++) disponibles.add(d);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1992, 1, 15), BiologicalSex.FEMALE,
                new BigDecimal("165"), new BigDecimal("70"), null, caso.nivel(), caso.objetivo(), "e1e2",
                caso.gym() ? TrainingLocation.GYM : TrainingLocation.HOME, (short) caso.dias(), disponibles,
                (short) 45, caso.gym() ? GYM : CASA, List.of(), "Ninguna", true, true, true, true, true));
        return u;
    }
}
