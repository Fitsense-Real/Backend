package main.web.services.fitsense.planning;

import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.domain.services.PlanDraftValidator;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
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
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * El plan 58 (INCREASE_STRENGTH, GPT-5-mini, 26/10/2026) tal como se publico,
 * con el perfil del usuario 25 y el catalogo real, por el validador de
 * produccion: con la V25 de fuerza se rechaza por LOWER_HIP. Con la bisagra
 * del miercoles en dosis de fuerza, la V25 lo acepta.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
class Plan58FuerzaIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired WeeklyTrainingPlanCommandService planes;
    @Autowired UserProfileCommandService perfiles;
    @Autowired PlanDraftValidator validador;
    @Autowired JdbcTemplate jdbc;

    static final LocalDate LUNES = TrainingWeek.containing(LocalDate.now()).startDate().plusWeeks(1);

    private PlanGenerationContext contexto() {
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES ('plan58@pruebas.invalid', 'x', 'Prueba', now(), now())");
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = 'plan58@pruebas.invalid'", Long.class);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1998, 4, 12), BiologicalSex.MALE,
                new BigDecimal("177"), new BigDecimal("76"), null, FitnessLevel.INTERMEDIATE,
                GoalType.INCREASE_STRENGTH, "Aumentar progresivamente mi fuerza", TrainingLocation.GYM, (short) 3,
                List.of((short) 1, (short) 3, (short) 5), (short) 60,
                List.of("body weight", "dumbbell", "barbell", "cable", "leverage machine", "bench", "smith machine",
                        "kettlebell", "band"),
                List.of(), null, true, true, true, true, true));
        return planes.prepare(GenerateWeeklyPlanCommand.firstPlan(u, LUNES)).context();
    }

    static PlanDraft.DraftExercise sr(long id, int series, int reps, int descanso) {
        return new PlanDraft.DraftExercise(id, PrescriptionType.SETS_REPS, (short) series, (short) reps, null, null,
                (short) descanso, null);
    }

    private static PlanDraft plan58(boolean bisagraConFuerza) {
        var lunes = List.of(sr(755, 3, 6, 150), sr(548, 3, 10, 90), sr(436, 3, 10, 90), sr(739, 3, 10, 90),
                sr(503, 2, 13, 90), sr(519, 3, 10, 90), sr(1103, 3, 13, 90));
        var miercoles = new ArrayList<>(List.of(sr(517, 3, 6, 150), sr(711, 3, 10, 90), sr(553, 3, 10, 90),
                sr(778, 3, 10, 90), sr(872, 3, 13, 90), sr(785, 3, 10, 90), sr(1063, 2, 13, 90)));
        if (bisagraConFuerza) miercoles.set(2, sr(553, 3, 6, 150));
        var viernes = List.of(sr(485, 3, 6, 150), sr(323, 3, 10, 90), sr(1073, 3, 10, 90), sr(704, 3, 10, 90),
                sr(1062, 2, 13, 90), sr(436, 3, 10, 90), sr(755, 3, 10, 90));
        return new PlanDraft(GenerationSource.AI, "gpt-5-mini", "plan 58", 600, "r", List.of(
                new PlanDraft.DraftWorkout(LUNES, WorkoutFocus.FULL_BODY, "lunes", 44, lunes),
                new PlanDraft.DraftWorkout(LUNES.plusDays(2), WorkoutFocus.FULL_BODY, "miercoles", 44, miercoles),
                new PlanDraft.DraftWorkout(LUNES.plusDays(4), WorkoutFocus.FULL_BODY, "viernes", 43, viernes)));
    }

    private List<String> v25(PlanDraft draft, PlanGenerationContext ctx) {
        try {
            validador.validate(draft, ctx, 30);
            return List.of();
        } catch (InvalidPlanDraftException e) {
            return e.problems().stream().filter(p -> p.startsWith("V25")).toList();
        }
    }

    @Test
    @DisplayName("el plan 58 se rechaza por LOWER_HIP sin dosis de fuerza; con la bisagra a 3x6/150, la V25 lo acepta")
    void plan58() {
        var ctx = contexto();
        var rechazos = v25(plan58(false), ctx);
        assertEquals(1, rechazos.size(), rechazos.toString());
        assertTrue(rechazos.get(0).contains("LOWER_HIP"), rechazos.toString());
        assertEquals(List.of(), v25(plan58(true), ctx));
    }
}
