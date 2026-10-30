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
 * Una orden de adaptacion que fuerza sesiones mas cortas que las del perfil
 * (perfil de 60 minutos, {@code forcedSessionMinutes} = 45). El prompt de la
 * generacion y el de la correccion P2-D se construyen con la duracion
 * efectiva; V24 tiene que medir con la misma.
 * <p>
 * Fijada en el catalogo V39: las sesiones son las del plan 60, con 1225 a 3x13 como fuerza de
 * zona pequena. Desde V40 (C4/C6) 1225 es TRUNK_FLEXION y V24 le exige 2x13, igual que paso con
 * 872 en V36 (ver {@link DuracionForzadaPlan60IT}, catalogo V35). Lo que se prueba aqui es la
 * duracion forzada, no la clasificacion de 1225.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "spring.flyway.target=39"
})
class DuracionForzadaIT extends DuracionForzadaBase {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Test
    @DisplayName("perfil de 60 y orden de 45: los dos prompts dicen 45 minutos y maximo 6")
    void promptConLaDuracionEfectiva() {
        var ctx = contexto(60, forzar(45));
        assertEquals(45, ctx.effectiveSessionMinutes());

        var c = PlanInputSnapshot.of(ctx).constraints();
        assertEquals(45, c.sessionMinutes());
        assertEquals(6, c.maxColocacionesDia());
        assertEquals(45 * MvpDosePolicy.FRACCION_UTIL, c.minutosUtilesDia(), 1e-9);

        var fix = SessionFixInputSnapshot.of(PlanInputSnapshot.of(ctx)).constraints();
        assertEquals(45, fix.sessionMinutes());
        assertEquals(6, fix.maxColocacionesDia());
    }

    @Test
    @DisplayName("perfil de 60 y orden de 45: siete por sesion se rechaza por el tope y por el presupuesto de 45")
    void sieteSeRechazaConLaOrdenDe45() {
        var problemas = v24(borrador(sesiones(), 43), contexto(60, forzar(45)));
        assertEquals(3, problemas.stream()
                .filter(p -> p.contains("tiene 7 colocaciones y el maximo de 4D v3.1 para sesiones de 45 minutos es 6"))
                .count(), problemas.toString());
        assertEquals(3, problemas.stream()
                .filter(p -> p.contains("el presupuesto del dia es 38.3 (85 % de 45 minutos)")).count(),
                problemas.toString());
    }

    @Test
    @DisplayName("perfil de 60 y orden de 45: seis por sesion pasa V24")
    void seisPasaConLaOrdenDe45() {
        var seis = sesiones();
        seis.forEach(s -> s.remove(s.size() - 1));
        assertEquals(List.of(), v24(borrador(seis, 37), contexto(60, forzar(45))));
    }

    @Test
    @DisplayName("motor de reglas con una orden de 45 sobre un perfil de 60: publica como mucho seis por sesion")
    void motorDeReglasConLaOrdenDe45() {
        long u = participante(60);
        long plan = planning.generateWeeklyPlan(u, LUNES, forzar(45), null, null).orElseThrow();
        var porSesion = jdbc.queryForList("SELECT count(*) FROM planned_workout_exercises pwe JOIN planned_workouts pw"
                + " ON pw.planned_workout_id = pwe.planned_workout_id WHERE pw.plan_id = ?"
                + " GROUP BY pw.planned_workout_id", Long.class, plan);
        assertEquals(3, porSesion.size());
        assertTrue(porSesion.stream().allMatch(n -> n <= 6), porSesion.toString());
    }
}
