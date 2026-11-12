package main.web.services.fitsense.configuracion;

import main.web.services.fitsense.planning.application.internal.commandservices.PlanningPolicySelector;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanningPolicy;
import main.web.services.fitsense.planning.domain.services.AdaptedWeekProducer;
import main.web.services.fitsense.planning.domain.services.ConservationValidator;
import main.web.services.fitsense.planning.domain.services.SafetySubstitutionPolicy;
import main.web.services.fitsense.planning.infrastructure.generation.ai.PlanPromptBuilder;
import main.web.services.fitsense.planning.infrastructure.generation.ai.ReplicateProperties;
import main.web.services.fitsense.planning.infrastructure.generation.ai.SessionFixPromptBuilder;
import main.web.services.fitsense.planning.infrastructure.validation.StrictValidationAdapter;
import main.web.services.fitsense.planning.interfaces.acl.PlanningContextFacade;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.*;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
 * La configuracion evaluada, tal como la cablea Spring con application.properties (sin ningun override salvo la base
 * efimera y el secreto JWT de prueba): PLAN-REV-2.0, V2.1, PR-1.7, V8C multiconjunto con limite de dificultad,
 * retirada localizada y progresion (que enciende la V24 por capacidades en la adaptacion).
 * <p>
 * Aislamiento de Replicate: failsafe fuerza {@code fitsense.ai.enabled=false} (pom.xml) y no hay token, asi que esta
 * prueba no puede llamar al proveedor; los planes los publica el motor de reglas con la politica evaluada.
 */
@Testcontainers
@SpringBootTest(properties = "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef")
@DisplayName("Configuracion evaluada cableada por Spring: politica, prompts, V8C, guardas y progresion")
class ConfiguracionEvaluadaIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    static final LocalDate SEMANA = LocalDate.of(2026, 11, 2);
    static final List<String> GIMNASIO = List.of("body weight", "dumbbell", "barbell", "cable", "leverage machine",
            "bench", "band", "stationary bike", "treadmill", "kettlebell", "ez barbell", "smith machine");

    @Autowired PlanningPolicySelector selector;
    @Autowired SessionFixPromptBuilder correccion;
    @Autowired ConservationValidator v8c;
    @Autowired AdaptedWeekProducer productor;
    @Autowired StrictValidationAdapter estricta;
    @Autowired ReplicateProperties replicate;
    @Autowired PlanningContextFacade planning;
    @Autowired UserProfileCommandService perfiles;
    @Autowired JdbcTemplate jdbc;
    @Value("${fitsense.adaptation.progression.enabled}") boolean progresion;

    @Test
    void cableaLaConfiguracionEvaluada() {
        assertSame(PlanningPolicy.REV2, selector.activa(), "politica de los planes nuevos");
        assertEquals("FIX-PR-2.1-EXP", correccion.version(), "prompt de adaptacion V2.1");
        assertEquals("PR-1.7", PlanPromptBuilder.VERSION, "prompt de generacion PR-1.7");
        assertEquals(ConservationValidator.ReglaPatron.MULTICONJUNTO, v8c.reglaPatron(), "V8C multiconjunto");
        assertEquals(SafetySubstitutionPolicy.Estrategia.LIMITE_Y_PREFERENCIA, v8c.estrategiaSustitucion());
        assertTrue(v8c.limitaDificultad(), "limite de dificultad activo");
        assertTrue(productor.retiradaLocalizada(), "retirada localizada ante dolor activa");
        assertTrue(progresion, "progresion activa");
        assertTrue(estricta.validaCapacidades(), "con progresion, V24 por capacidades tambien en la adaptacion");
        assertTrue(estricta.bloqueaDosis4D(), "4D sigue en ENFORCE");
    }

    @Test
    void lasPruebasNoPuedenLlamarAReplicate() {
        assertFalse(replicate.enabled(), "failsafe apaga la IA en las pruebas");
        assertFalse(replicate.isUsable(), "sin IA utilizable no hay llamadas al proveedor");
    }

    @Test
    void losCuatroObjetivosGeneranConPlanRev2() {
        int i = 0;
        for (var objetivo : GoalType.values()) {
            long u = participante("config-" + (i++) + "@pruebas.invalid", objetivo);
            long plan = planning.generateWeeklyPlan(u, SEMANA, PlanAdjustment.none(), null, null).orElseThrow();
            var fila = jdbc.queryForMap("SELECT generation_source, planning_policy FROM weekly_training_plans"
                    + " WHERE plan_id = ?", plan);
            assertEquals("PLAN-REV-2.0", fila.get("planning_policy"), objetivo + ": politica guardada");
            assertEquals("RULE_ENGINE", fila.get("generation_source"), objetivo + ": sin IA en pruebas, motor de reglas");
            Integer sesiones = jdbc.queryForObject("SELECT count(*) FROM planned_workouts WHERE plan_id = ?",
                    Integer.class, plan);
            assertTrue(sesiones != null && sesiones > 0, objetivo + ": el plan tiene sesiones");
        }
    }

    long participante(String email, GoalType objetivo) {
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Config', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1990, 6, 15), BiologicalSex.FEMALE,
                new BigDecimal("168"), new BigDecimal("68"), null, FitnessLevel.INTERMEDIATE, objetivo,
                "configuracion evaluada", TrainingLocation.GYM, (short) 3, List.of((short) 1, (short) 3, (short) 5),
                (short) 45, GIMNASIO, List.of(), "Ninguna", true, true, true, true, true));
        return u;
    }
}
