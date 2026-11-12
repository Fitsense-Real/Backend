package main.web.services.fitsense.planning;

import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.domain.services.PlanDraftValidator;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
import main.web.services.fitsense.planning.infrastructure.generation.ai.PlanInputSnapshot;
import main.web.services.fitsense.planning.infrastructure.generation.ai.SessionFixInputSnapshot;
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
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static main.web.services.fitsense.planning.Plan60HipertrofiaIT.LUNES;
import static main.web.services.fitsense.planning.Plan60HipertrofiaIT.borrador;
import static main.web.services.fitsense.planning.Plan60HipertrofiaIT.sesiones;
import static org.junit.jupiter.api.Assertions.*;

/** Material comun de {@link DuracionForzadaIT} (catalogo vigente) y {@link DuracionForzadaPlan60IT} (V35). */
abstract class DuracionForzadaBase {

    @Autowired WeeklyTrainingPlanCommandService planes;
    @Autowired UserProfileCommandService perfiles;
    @Autowired PlanDraftValidator validador;
    @Autowired JdbcTemplate jdbc;
    @Autowired main.web.services.fitsense.planning.interfaces.acl.PlanningContextFacade planning;

    static final AtomicInteger SECUENCIA = new AtomicInteger();

    static PlanAdjustment forzar(Integer minutos) {
        return new PlanAdjustment(List.of(AdjustmentType.REDUCE_DURATION), null, null, null, 0.0, 0.0,
                null, null, minutos, "sesiones mas cortas", null, null);
    }

    long participante(int minutosPerfil) {
        String email = "forzada-" + SECUENCIA.incrementAndGet() + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Prueba', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.now().minusYears(27).minusDays(10),
                BiologicalSex.FEMALE, new BigDecimal("163"), new BigDecimal("58"), null, FitnessLevel.INTERMEDIATE,
                GoalType.GAIN_MUSCLE, "Ganar masa muscular", TrainingLocation.GYM, (short) 3,
                List.of((short) 1, (short) 3, (short) 5), (short) minutosPerfil,
                List.of("body weight", "dumbbell", "barbell", "cable", "leverage machine", "bench", "smith machine",
                        "kettlebell", "band"),
                List.of(), null, false, true, true, true, true));
        return u;
    }

    PlanGenerationContext contexto(int minutosPerfil, PlanAdjustment ajuste) {
        return planes.prepare(new GenerateWeeklyPlanCommand(participante(minutosPerfil), LUNES, ajuste, false,
                null, null)).context();
    }

    List<String> v24(PlanDraft draft, PlanGenerationContext ctx) {
        try {
            validador.validate(draft, ctx, 30);
            return List.of();
        } catch (InvalidPlanDraftException e) {
            return e.problems().stream().filter(p -> p.startsWith("V24")).toList();
        }
    }
}
