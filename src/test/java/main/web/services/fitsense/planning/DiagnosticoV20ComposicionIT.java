package main.web.services.fitsense.planning;

import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.model.valueobjects.WorkoutFocus;
import main.web.services.fitsense.planning.domain.services.CatalogEligibilityPolicy;
import main.web.services.fitsense.planning.domain.services.PlanDraftValidator;
import main.web.services.fitsense.planning.domain.services.SessionDurationEstimator;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
import main.web.services.fitsense.planning.infrastructure.generation.rules.RuleBasedTrainingPlanGenerator;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.BiologicalSex;
import main.web.services.fitsense.profiling.domain.model.valueobjects.FitnessLevel;
import main.web.services.fitsense.profiling.domain.model.valueobjects.GoalType;
import main.web.services.fitsense.profiling.domain.model.valueobjects.TrainingLocation;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import main.web.services.fitsense.shared.domain.model.valueobjects.TrainingWeek;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * DIAGNOSTICO de V20 (sin cambiar nada): por que LOSE_WEIGHT principiante en casa
 * con 4 y 5 dias se queda sin plan cuando V37.1 exige el implemento real.
 * Opt-in: {@code -Dfitsense.v20.diagnostico.dir=<carpeta>}; el catalogo se elige con
 * {@code -Dspring.flyway.locations} (V37 vigente o V37 + V37.1).
 * <p>
 * Para cada sesion del borrador del motor de reglas escribe: enfoque, colocaciones
 * (tope 8), segundos del estimador, minutos redondeados frente al piso entero de V20
 * y frente al 70 % exacto, y el repertorio del enfoque que el perfil aun podria usar
 * (elegibles del enfoque, no complementos, que no estan en la sesion). Asi se separa
 * un defecto de REDONDEO (la sesion llega al 70 % y el entero la rechaza) de uno de
 * COMPOSICION (no llega al 70 % y quedan o no quedan candidatos con que completarla).
 */
@Testcontainers
@EnabledIfSystemProperty(named = "fitsense.v20.diagnostico.dir", matches = ".+")
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
class DiagnosticoV20ComposicionIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired WeeklyTrainingPlanCommandService planComandos;
    @Autowired UserProfileCommandService perfiles;
    @Autowired RuleBasedTrainingPlanGenerator motor;
    @Autowired JdbcTemplate jdbc;

    @Autowired SessionDurationEstimator estimador;

    @Test
    @DisplayName("LOSE_WEIGHT principiante: sesiones del motor de reglas frente al piso de V20")
    void diagnostico() throws Exception {
        var semana = TrainingWeek.containing(LocalDate.now()).startDate().plusWeeks(1);
        var sb = new StringBuilder("perfil\tfecha\tenfoque\tcolocaciones\tsegundos\tmin_estimador\tpiso_entero"
                + "\tpiso_exacto_s\tpct_real\tveredicto\trepertorio_libre_enfoque\tlibres_de_trabajo\tejercicios\n");
        var candidatos = new StringBuilder("perfil\tfecha\tveredicto\tid\tgrupo\tpatron\tclase\tprescripcion\tnivel\n");
        int n = 0;
        for (var nivel : List.of(FitnessLevel.BEGINNER))
            for (int dias : new int[]{3, 4, 5, 6})
                for (var lugar : List.of("CASA", "GYM")) {
                    n++;
                    String perfil = "LOSE_WEIGHT/" + nivel + "/" + dias + "d/45/" + lugar;
                    String email = "diagv20_" + n + "@pruebas.invalid";
                    jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                            + " VALUES (?, 'x', 'Prueba', now(), now())", email);
                    long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
                    var disponibles = new ArrayList<Short>();
                    for (short d = 1; d <= dias; d++) disponibles.add(d);
                    perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1992, 1, 15), BiologicalSex.FEMALE,
                            new BigDecimal("165"), new BigDecimal("70"), null, nivel, GoalType.LOSE_WEIGHT,
                            "evaluacion", "GYM".equals(lugar) ? TrainingLocation.GYM : TrainingLocation.HOME,
                            (short) dias, disponibles, (short) 45,
                            "GYM".equals(lugar) ? GeneracionReferenciaBase.GIMNASIO : GeneracionReferenciaBase.CASA,
                            List.of(), "Ninguna", true, true, true, true, true));
                    var ctx = planComandos.prepare(new GenerateWeeklyPlanCommand(u, semana, null, true, null, null))
                            .context();
                    var borrador = motor.generate(ctx, List.of());
                    int piso = PlanDraftValidator.minimumSessionMinutes(ctx);
                    int pisoExacto = (int) Math.ceil(ctx.effectiveSessionMinutes() * 60.0
                            * ctx.prescription().floorPct() / 100.0);
                    var complementos = CatalogEligibilityPolicy.complementos(ctx.availableExercises(), "LOSE_WEIGHT");
                    for (var w : borrador.workouts()) {
                        int seg = segundos(w, ctx.prescription());
                        int min = estimador.estimateMinutes(w, ctx.prescription());
                        var enSesion = w.exercises().stream().map(PlanDraft.DraftExercise::exerciseId)
                                .collect(Collectors.toSet());
                        var libres = ctx.availableExercises().stream()
                                .filter(c -> !enSesion.contains(c.exerciseId()))
                                .filter(c -> w.focus().accepts(c.bodyPartCode()))
                                .toList();
                        long libresTrabajo = libres.stream().filter(c -> !complementos.contains(c.exerciseId())).count();
                        String veredicto = min >= piso ? (seg >= pisoExacto ? "PASA" : "PASA_SOLO_POR_REDONDEO")
                                : (seg >= pisoExacto ? "RECHAZA_SOLO_POR_REDONDEO" : "COMPOSICION_CORTA");
                        sb.append(perfil).append('\t').append(w.scheduledDate().getDayOfWeek()).append('\t')
                                .append(w.focus()).append('\t').append(w.exercises().size()).append('\t').append(seg)
                                .append('\t').append(min).append('\t').append(piso).append('\t').append(pisoExacto)
                                .append('\t').append(String.format(Locale.ROOT, "%.1f", 100.0 * seg / (45 * 60)))
                                .append('\t').append(veredicto).append('\t').append(libres.size()).append('\t')
                                .append(libresTrabajo).append('\t')
                                .append(w.exercises().stream().map(e -> e.exerciseId() + ":" + e.plannedSets() + "x"
                                        + (e.plannedReps() != null ? e.plannedReps() + "r" : e.plannedDurationSeconds() + "s")
                                        + "/" + e.restSeconds()).toList())
                                .append('\n');
                        if ("COMPOSICION_CORTA".equals(veredicto) || veredicto.startsWith("PASA_SOLO"))
                            for (var c : libres) {
                                if (complementos.contains(c.exerciseId())) continue;
                                candidatos.append(perfil).append('\t').append(w.scheduledDate().getDayOfWeek())
                                        .append('\t').append(veredicto).append('\t').append(c.exerciseId())
                                        .append('\t').append(c.bodyPartCode()).append('\t').append(c.movementPattern())
                                        .append('\t').append(main.web.services.fitsense.planning.domain.services
                                                .CapabilityPrescriptionPolicy.claseDe(c))
                                        .append('\t').append(c.defaultPrescription()).append('\t').append(c.difficulty())
                                        .append('\n');
                            }
                    }
                }
        var dir = Path.of(System.getProperty("fitsense.v20.diagnostico.dir"));
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("diagnostico_v20.tsv"), sb.toString());
        Files.writeString(dir.resolve("diagnostico_v20_candidatos.tsv"), candidatos.toString());
    }

    /** Los mismos segundos que suma SessionDurationEstimator antes de redondear al minuto. */
    static int segundos(PlanDraft.DraftWorkout w,
                        main.web.services.fitsense.configuration.domain.model.valueobjects.PrescriptionParams p) {
        if (w.exercises().isEmpty()) return 0;
        long total = (long) p.warmupMinutesOrDefault() * 60
                + (long) (w.exercises().size() - 1) * p.transitionSecondsOrDefault();
        for (var e : w.exercises()) {
            int series = e.plannedSets() == null ? 1 : e.plannedSets();
            total += e.prescriptionType() == main.web.services.fitsense.planning.domain.model.valueobjects.PrescriptionType.DURATION
                    ? (long) series * (e.plannedDurationSeconds() == null ? 0 : e.plannedDurationSeconds())
                    : (long) series * (e.plannedReps() == null ? 0 : e.plannedReps()) * p.secondsPerRepOrDefault();
            int descanso = e.restSeconds() == null ? p.defaultRestSecondsOrDefault() : e.restSeconds();
            total += (long) Math.max(0, series - 1) * descanso;
        }
        return (int) total;
    }
}
