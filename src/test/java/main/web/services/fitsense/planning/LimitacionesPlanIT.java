package main.web.services.fitsense.planning;

import main.web.services.fitsense.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import main.web.services.fitsense.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.interfaces.acl.PlanningContextFacade;
import main.web.services.fitsense.planning.interfaces.rest.TrainingPlanController;
import main.web.services.fitsense.planning.interfaces.rest.resources.PlanLimitationsResource.Limitation;
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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Decision I (30/10/2026): la traccion sin implementos es no factible y se acepta (V23 no se
 * toca), pero el endpoint de limitaciones lo comunica con un texto para la app que no obliga a
 * comprar nada. Ademas el plan se sigue publicando y no contiene traccion inventada.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
@DisplayName("I: limitaciones del plan por equipamiento")
class LimitacionesPlanIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired TrainingPlanController planes;
    @Autowired PlanningContextFacade planning;
    @Autowired UserProfileCommandService perfiles;
    @Autowired UserRepository usuarios;
    @Autowired JdbcTemplate jdbc;

    private static int secuencia;

    @Test
    @DisplayName("Solo peso corporal: UPPER_PULL no factible, mensaje sin compra obligatoria, y el plan publica sin traccion")
    void soloPesoCorporal() {
        long u = participante(List.of("body weight"));
        var res = planes.limitations(principal(u)).getBody();
        assertNotNull(res);
        var traccion = res.limitations().stream().filter(l -> "UPPER_PULL".equals(l.code())).findFirst()
                .orElseThrow(() -> new AssertionError("falta UPPER_PULL: " + res));
        assertEquals("SLOT", traccion.type());
        assertTrue(traccion.message().contains("No necesitas comprar nada"), traccion.message());
        assertEquals(List.of("band", "wall bars", "anchor point"), traccion.optionalEquipmentCodes());
        // Los codigos sugeridos existen en el catalogo de equipamiento que lee la app.
        for (var codigo : traccion.optionalEquipmentCodes())
            assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM equipment_types WHERE code = ?",
                    Integer.class, codigo), codigo);
        // Solo lectura: no se escribio ningun plan.
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ?",
                Integer.class, u));

        // El plan se publica igualmente, y sin ejercicios de traccion.
        var semana = res.weekStartDate();
        planning.generateWeeklyPlan(u, semana, PlanAdjustment.none(), null, null)
                .orElseThrow(() -> new AssertionError("sin plan"));
        int tracciones = jdbc.queryForObject("""
                SELECT count(*) FROM weekly_training_plans p
                JOIN planned_workouts w ON w.plan_id = p.plan_id
                JOIN planned_workout_exercises e ON e.planned_workout_id = w.planned_workout_id
                JOIN exercises x ON x.exercise_id = e.exercise_id
                WHERE p.user_id = ? AND x.movement_pattern IN ('HORIZONTAL_PULL', 'VERTICAL_PULL')
                """, Integer.class, u);
        assertEquals(0, tracciones, "no se fabrica traccion sin implementos");
    }

    @Test
    @DisplayName("Con banda: sin limitacion de traccion")
    void conBanda() {
        long u = participante(List.of("body weight", "band"));
        List<Limitation> lim = planes.limitations(principal(u)).getBody().limitations();
        assertTrue(lim.stream().noneMatch(l -> "UPPER_PULL".equals(l.code())), lim.toString());
    }

    private long participante(List<String> equipamiento) {
        String email = "limit-" + (++secuencia) + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Prueba', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1992, 1, 15), BiologicalSex.FEMALE,
                new BigDecimal("165"), new BigDecimal("70"), null, FitnessLevel.BEGINNER,
                GoalType.GENERAL_FITNESS, "limitaciones", TrainingLocation.HOME, (short) 3,
                List.of((short) 1, (short) 3, (short) 5), (short) 45, equipamiento, List.of(), "Ninguna",
                true, true, true, true, true));
        return u;
    }

    private UserDetailsImpl principal(long u) {
        return UserDetailsImpl.build(usuarios.findById(u).orElseThrow());
    }
}
