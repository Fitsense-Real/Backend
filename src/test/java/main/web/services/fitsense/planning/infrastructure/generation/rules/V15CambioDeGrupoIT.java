package main.web.services.fitsense.planning.infrastructure.generation.rules;

import main.web.services.fitsense.planning.domain.exceptions.PlanGenerationFailedException;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.services.CapabilityPrescriptionPolicy;
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
 * V15 · EL PASO DE OTRO GRUPO (IMPLEMENTACION_V20.md §1.2, aprobado el 30/10/2026).
 * <p>
 * Cuando el relleno romperia V15 y no hay series que subir, el constructor cambia el
 * ejercicio por otro del enfoque. Aqui se comprueba, sobre cada cambio que registra
 * el constructor ({@link CapabilityWeekBuilder#cambiosV15()}), que:
 * <ul>
 *   <li>antes se intento subir series: el dia no llegaba al piso con ellas (si no,
 *       no habria cambio);</li>
 *   <li>la capacidad es la misma que la del ejercicio rechazado;</li>
 *   <li>la dosis es exactamente la de esa capacidad para el ejercicio, el objetivo y el
 *       nivel (no se supera ninguna dosis);</li>
 *   <li>el grupo es otro y la sesion respeta la mitad de V15;</li>
 *   <li>el plan se publica con el validador de produccion (V15, V18, V23, V24, V25).</li>
 * </ul>
 * Con V37 el paso no se activa en estos perfiles (el relleno no rompe V15); con V38 (la antigua V37.1)
 * si (GENERAL_FITNESS 4 dias en gimnasio). La prueba exige al menos un cambio solo cuando
 * V38 (la V37.1) esta aplicada y comprueba las propiedades de todos los que haya.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
@DisplayName("V15 · el relleno cambiado de grupo conserva capacidad y dosis y publica")
class V15CambioDeGrupoIT {

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
    /** El equipamiento de los perfiles de referencia (GeneracionReferenciaBase). */
    static final List<String> GIMNASIO = List.of("body weight", "dumbbell", "barbell", "cable",
            "leverage machine", "bench", "band", "stationary bike", "treadmill", "kettlebell", "ez barbell", "smith machine");
    static final List<String> CASA = List.of("body weight", "dumbbell", "band");
    private int secuencia;

    @Test
    @DisplayName("GENERAL_FITNESS 4d gimnasio y 6d casa: cada cambio conserva la capacidad, la dosis y V15")
    void cambiosDeGrupo() {
        boolean v371 = jdbc.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version IN ('37.1', '38') AND success", Integer.class) > 0;
        var fallos = new ArrayList<String>();
        int cambios = 0;
        for (var nivel : List.of(FitnessLevel.BEGINNER, FitnessLevel.INTERMEDIATE, FitnessLevel.ADVANCED))
            for (var lugar : List.of("GYM", "CASA"))
                for (int dias : new int[]{4, 6})
                    for (int semilla = 1; semilla <= SEMILLAS; semilla++) {
                        String perfil = "GENERAL_FITNESS/" + nivel + "/" + dias + "d/45/" + lugar + " semilla " + semilla;
                        long u = participante(nivel, dias, lugar);
                        var ctx = planComandos.prepare(new GenerateWeeklyPlanCommand(u, S1, null, true, null, null))
                                .context();
                        var constructor = new CapabilityWeekBuilder(ctx, estimador, motor.seedFor(ctx));
                        var borrador = constructor.construir(motor::finalizar);
                        assertEquals(motor.generate(ctx, List.of()), borrador, "el constructor es el del motor: " + perfil);
                        if (constructor.cambiosV15().isEmpty()) continue;

                        Map<Long, CandidateExercise> elegibles = ctx.availableExercises().stream()
                                .collect(Collectors.toMap(CandidateExercise::exerciseId, Function.identity(), (a, b) -> a));
                        var limites = ctx.prescription().repLimits();
                        for (var c : constructor.cambiosV15()) {
                            cambios++;
                            String id = perfil + " " + c.fecha() + " " + c.rechazado() + "->" + c.elegido();
                            if (c.capacidadElegido() != c.capacidadRechazado())
                                fallos.add(id + ": capacidad " + c.capacidadRechazado() + " -> " + c.capacidadElegido());
                            if (c.grupoElegido().equals(c.grupoRechazado()))
                                fallos.add(id + ": mismo grupo " + c.grupoElegido());
                            var candidato = elegibles.get(c.elegido());
                            var dosis = CapabilityPrescriptionPolicy.dosis(c.capacidadRechazado(), candidato,
                                    "GENERAL_FITNESS", nivel.name(), limites);
                            // coincideCon: tipo, repeticiones o segundos y descanso de la dosis, y series
                            // entre las de la dosis y su maximo autorizado.
                            if (dosis.isEmpty() || !dosis.get().coincideCon(c.ejercicio()))
                                fallos.add(id + ": la dosis no es la de " + c.capacidadRechazado() + " " + c.ejercicio());
                            var sesion = borrador.workouts().stream()
                                    .filter(w -> w.scheduledDate().equals(c.fecha())).findFirst().orElseThrow();
                            if (sesion.exercises().stream().noneMatch(e -> e.exerciseId().equals(c.elegido())))
                                fallos.add(id + ": el elegido no esta en la sesion");
                            // El rechazado puede volver despues por otro paso (otro relleno, o E2) cuando
                            // el reparto ya lo admite: no se comprueba su ausencia.
                        }
                        // El validador de produccion decide: V15, V18, V23, V24, V25 y V20.
                        try {
                            planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null)
                                    .orElseThrow(() -> new AssertionError("sin plan"));
                        } catch (PlanGenerationFailedException e) {
                            fallos.add(perfil + " no publica: " + e.getMessage());
                        }
                    }
        if (v371) assertTrue(cambios > 0, "con V37.1 el paso tiene que activarse al menos una vez");
        assertEquals(List.of(), fallos);
    }

    private long participante(FitnessLevel nivel, int dias, String lugar) {
        String email = "v15cambio-" + (++secuencia) + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Prueba', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        var disponibles = new ArrayList<Short>();
        for (short d = 1; d <= dias; d++) disponibles.add(d);
        boolean gym = "GYM".equals(lugar);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1992, 1, 15), BiologicalSex.FEMALE,
                new BigDecimal("165"), new BigDecimal("70"), null, nivel, GoalType.GENERAL_FITNESS, "v15",
                gym ? TrainingLocation.GYM : TrainingLocation.HOME, (short) dias, disponibles, (short) 45,
                gym ? GIMNASIO : CASA,
                List.of(), "Ninguna", true, true, true, true, true));
        return u;
    }
}
