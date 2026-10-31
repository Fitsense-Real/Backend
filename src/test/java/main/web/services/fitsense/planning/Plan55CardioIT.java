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
 * El plan 55 (LOSE_WEIGHT, GPT-5-mini, 24/10/2026) tal como se publico, pasado
 * por el validador de produccion con el catalogo real: con la V25 por sesion
 * se rechaza por el miercoles y el viernes sin CARDIO. Y la misma semana con el
 * gateo de oso repetido los tres dias pasa la V25.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
class Plan55CardioIT {

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
                + " VALUES ('plan55@pruebas.invalid', 'x', 'Prueba', now(), now())");
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = 'plan55@pruebas.invalid'", Long.class);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1985, 3, 2), BiologicalSex.FEMALE,
                new BigDecimal("165"), new BigDecimal("80"), new BigDecimal("70"), FitnessLevel.BEGINNER,
                GoalType.LOSE_WEIGHT, "bajar de peso", TrainingLocation.GYM, (short) 3,
                List.of((short) 1, (short) 3, (short) 5), (short) 45,
                List.of("dumbbell", "cable", "band", "leverage machine"),
                List.of(), "Ninguna", false, true, true, true, true));
        return planes.prepare(GenerateWeeklyPlanCommand.firstPlan(u, LUNES)).context();
    }

    static PlanDraft.DraftExercise sr(long id, int series, int reps) {
        return new PlanDraft.DraftExercise(id, PrescriptionType.SETS_REPS, (short) series, (short) reps, null, null,
                (short) 120, null);
    }

    static final PlanDraft.DraftExercise GATEO = new PlanDraft.DraftExercise(253L, PrescriptionType.DURATION,
            (short) 3, null, 90, null, (short) 60, null);

    private static PlanDraft plan55(boolean cardioLosTresDias) {
        var lunes = new ArrayList<>(List.of(GATEO, sr(1096, 2, 8), sr(548, 2, 8), sr(349, 2, 8), sr(705, 2, 8),
                sr(121, 2, 8), sr(144, 2, 8), sr(872, 2, 10)));
        var miercoles = new ArrayList<>(List.of(sr(349, 2, 8), sr(592, 2, 8), sr(436, 2, 8), sr(705, 2, 8),
                sr(740, 2, 8), sr(574, 2, 8), sr(1102, 2, 10), sr(1059, 2, 10)));
        var viernes = new ArrayList<>(List.of(sr(1099, 2, 8), sr(547, 2, 8), sr(990, 2, 8), sr(1263, 2, 8),
                sr(144, 2, 8), sr(1004, 2, 8), sr(791, 2, 8), sr(872, 2, 10)));
        if (cardioLosTresDias) {
            miercoles.set(miercoles.size() - 1, GATEO);
            viernes.set(viernes.size() - 1, GATEO);
        }
        return new PlanDraft(GenerationSource.AI, "gpt-5-mini", "plan 55", 393, "r", List.of(
                new PlanDraft.DraftWorkout(LUNES, WorkoutFocus.FULL_BODY, "lunes", 39, lunes),
                new PlanDraft.DraftWorkout(LUNES.plusDays(2), WorkoutFocus.FULL_BODY, "miercoles", 36, miercoles),
                new PlanDraft.DraftWorkout(LUNES.plusDays(4), WorkoutFocus.FULL_BODY, "viernes", 36, viernes)));
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
    @DisplayName("el plan 55 se rechaza por el miercoles y el viernes sin CARDIO; con cardio los tres dias, no")
    void plan55() {
        var ctx = contexto();
        assertTrue(ctx.availableExercises().stream().anyMatch(c -> c.exerciseId() == 253L),
                "premisa: el gateo de oso es elegible");

        var rechazos = v25(plan55(false), ctx);
        assertEquals(2, rechazos.size(), rechazos.toString());
        assertTrue(rechazos.get(0).contains(LUNES.plusDays(2).toString()), rechazos.toString());
        assertTrue(rechazos.get(1).contains(LUNES.plusDays(4).toString()), rechazos.toString());

        assertEquals(List.of(), v25(plan55(true), ctx), "el mismo cardio repetido en dias no consecutivos vale");
    }
}
