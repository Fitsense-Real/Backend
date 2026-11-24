package main.web.services.fitsense.planning;

import com.fasterxml.jackson.databind.ObjectMapper;
import main.web.services.fitsense.planning.application.internal.commandservices.GeneracionInicialService;
import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.GenerationSource;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.model.valueobjects.PrescriptionType;
import main.web.services.fitsense.planning.domain.model.valueobjects.WorkoutFocus;
import main.web.services.fitsense.planning.domain.services.DuracionPorObjetivoPolicy;
import main.web.services.fitsense.planning.domain.services.PlanDraftValidator;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.*;
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
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * O06 · LIMITE DE DURACION: tiempo SOLICITADO frente a tiempo AUTORIZADO (orden del 23/11/2026, cierre de pendientes).
 * <p>
 * O06 (cohorte O): LOSE_WEIGHT, intermedio, gimnasio, 3 dias, <b>35 minutos solicitados</b>. Tres magnitudes distintas:
 * <ol>
 *   <li><b>solicitado</b>: {@code session_minutes} del perfil (35). P-01 solo decide si el objetivo lo admite (30-75);</li>
 *   <li><b>autorizado</b>: el techo de V4-C en segundos estimados, {@code limiteV4SegundosDe(35)}: con PLAN-REV-2.0,
 *   35 x 60 = 2.100 s, SIN tolerancia (la historica PLAN-HIST-1 admitia +15 %, 2.415 s). Es lo que viaja a la IA como
 *   {@code max_session_seconds} y lo que V4 exige;</li>
 *   <li><b>publicado</b>: {@code expected_duration_minutes} de cada sesion, que calcula el backend y nunca supera lo
 *   solicitado; tras una adaptacion (D8) puede ser menor (O06 con gpt-5-mini: 29 min y, tras el dolor, 16-19 min).</li>
 * </ol>
 * Sin IA (motor de reglas): fija el contrato, no la calidad de la IA. No cambia ninguna regla.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.planning.policy=PLAN-REV-2.0"
})
@DisplayName("O06 · 35 min solicitados: autorizado 2.100 s (REV2, sin tolerancia), publicado <= 35 y valido en V4")
class O06LimiteDuracionIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    static final LocalDate SEMANA = LocalDate.of(2026, 11, 2);

    @Autowired GeneracionInicialService generacion;
    @Autowired WeeklyTrainingPlanCommandService planComandos;
    @Autowired PlanDraftValidator validador;
    @Autowired UserProfileCommandService perfiles;
    @Autowired JdbcTemplate jdbc;

    @Test
    void solicitadoAutorizadoPublicado() throws Exception {
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES ('o06@pruebas.invalid', 'x', 'O06', now(), now())");
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = 'o06@pruebas.invalid'", Long.class);
        // Perfil de O06 (O_PERFILES.csv): LOSE_WEIGHT, INTERMEDIATE, GYM, 3 dias (L-X-V), 35 min, sin restricciones.
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1974, 4, 18), BiologicalSex.FEMALE,
                new BigDecimal("164"), new BigDecimal("63"), new BigDecimal("58"), FitnessLevel.INTERMEDIATE,
                GoalType.LOSE_WEIGHT, "O06", TrainingLocation.GYM, (short) 3, List.of((short) 1, (short) 3, (short) 5),
                (short) 35, List.of("body weight", "dumbbell", "barbell", "cable", "leverage machine", "bench", "band",
                        "kettlebell", "ez barbell", "smith machine"), List.of(), "Ninguna", true, true, true, true, true));

        // 1. Solicitado: 35, admitido por P-01 para LOSE_WEIGHT.
        assertTrue(DuracionPorObjetivoPolicy.admite("LOSE_WEIGHT", 35));

        var plan = generacion.generar(GenerateWeeklyPlanCommand.firstPlan(u, SEMANA)).orElseThrow();
        long id = plan.getId();
        var snapshot = new ObjectMapper().readTree(jdbc.queryForObject(
                "SELECT input_snapshot::text FROM weekly_training_plans WHERE plan_id = ?", String.class, id));
        var restricciones = snapshot.path("constraints");
        assertEquals(35, restricciones.path("session_minutes").asInt(), snapshot.toString());

        // 2. Autorizado: el techo de V4-C de la politica vigente del plan, sin tolerancia en REV2.
        var ctx = planComandos.prepare(new GenerateWeeklyPlanCommand(u, SEMANA, null, true, null, null)).context();
        assertEquals("PLAN-REV-2.0", ctx.policy().id());
        assertEquals(0, ctx.policy().toleranciaV4Pct());
        assertEquals(2100L, ctx.policy().limiteV4SegundosDe(35));
        assertEquals(2100L, restricciones.path("max_session_seconds").asLong(),
                "la IA recibe el techo AUTORIZADO, no los minutos con tolerancia");

        // 3. Publicado: cada sesion <= 35 min declarados y el contenido cabe en el techo (V4 sobre lo PUBLICADO).
        var borrador = new ArrayList<PlanDraft.DraftWorkout>();
        for (var f : jdbc.queryForList("SELECT planned_workout_id, scheduled_date, focus_code, workout_name,"
                + " expected_duration_minutes FROM planned_workouts WHERE plan_id = ? ORDER BY scheduled_date", id)) {
            int minutos = ((Number) f.get("expected_duration_minutes")).intValue();
            assertTrue(minutos <= 35, "publicado " + minutos + " > solicitado 35");
            long w = ((Number) f.get("planned_workout_id")).longValue();
            var ej = jdbc.query("SELECT exercise_id, prescription_type, planned_sets, planned_reps,"
                    + " planned_duration_seconds, target_load_kg, rest_seconds FROM planned_workout_exercises"
                    + " WHERE planned_workout_id = ? ORDER BY exercise_order", (rs, i) -> new PlanDraft.DraftExercise(
                    rs.getLong(1), PrescriptionType.valueOf(rs.getString(2)), (Short) rs.getObject(3, Short.class),
                    (Short) rs.getObject(4, Short.class), (Integer) rs.getObject(5, Integer.class),
                    rs.getBigDecimal(6), (Short) rs.getObject(7, Short.class), null), w);
            borrador.add(new PlanDraft.DraftWorkout(((java.sql.Date) f.get("scheduled_date")).toLocalDate(),
                    WorkoutFocus.valueOf((String) f.get("focus_code")), (String) f.get("workout_name"), minutos, ej));
        }
        assertFalse(borrador.isEmpty());
        try {
            validador.validate(new PlanDraft(GenerationSource.RULE_ENGINE, null, "o06", null, "r", borrador), ctx, 30);
        } catch (InvalidPlanDraftException e) {
            fail("lo publicado no cabe en lo autorizado: " + e.problems());
        }
    }
}
