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

/**
 * EVIDENCIA HISTORICA. El plan 60 tal como se publico (26/10/2026) con el
 * catalogo ANTERIOR a V36, sin orden de duracion: a 60 minutos, siete por
 * sesion pasan V24. Aplica Flyway solo hasta V35 porque el plan lleva 872 como
 * estructural (3x13); con V36, 872 es TRUNK_FLEXION y V24 le exige la dosis de
 * relleno (2x13). Ver CATALOGO_V36.md.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "spring.flyway.target=35"
})
class DuracionForzadaPlan60IT extends DuracionForzadaBase {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Test
    @DisplayName("sin orden, o con una orden sin minutos, rige la duracion del perfil: 7 a 60 minutos")
    void sinMinutosForzadosRigeElPerfil() {
        assertEquals(List.of(), v24(borrador(sesiones(), 43), contexto(60, null)));
        assertEquals(List.of(), v24(borrador(sesiones(), 43), contexto(60, forzar(null))));
    }

}
