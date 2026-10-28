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
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        // V36: el borrador es el plan 60 tal como se publico con el catalogo ANTERIOR
        // (872 como estructural a 3x13). Con V36, 872 es TRUNK_FLEXION y V24 le exige
        // 2x13: ver CATALOGO_V36.md, seccion 8.
        "spring.flyway.target=35"
})
class DuracionForzadaIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

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

    private long participante(int minutosPerfil) {
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

    private PlanGenerationContext contexto(int minutosPerfil, PlanAdjustment ajuste) {
        return planes.prepare(new GenerateWeeklyPlanCommand(participante(minutosPerfil), LUNES, ajuste, false,
                null, null)).context();
    }

    private List<String> v24(PlanDraft draft, PlanGenerationContext ctx) {
        try {
            validador.validate(draft, ctx, 30);
            return List.of();
        } catch (InvalidPlanDraftException e) {
            return e.problems().stream().filter(p -> p.startsWith("V24")).toList();
        }
    }

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
    @DisplayName("sin orden, o con una orden sin minutos, rige la duracion del perfil: 7 a 60 minutos")
    void sinMinutosForzadosRigeElPerfil() {
        assertEquals(List.of(), v24(borrador(sesiones(), 43), contexto(60, null)));
        assertEquals(List.of(), v24(borrador(sesiones(), 43), contexto(60, forzar(null))));
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
