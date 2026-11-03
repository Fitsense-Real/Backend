package main.web.services.fitsense.planning;

import main.web.services.fitsense.EsquemaVigenteSobreCatalogoAnterior;
import org.springframework.context.annotation.Import;
import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.domain.services.PlanDraftValidator;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
import main.web.services.fitsense.planning.infrastructure.generation.ai.PlanInputSnapshot;
import main.web.services.fitsense.planning.infrastructure.generation.ai.PlanPromptBuilder;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.BiologicalSex;
import main.web.services.fitsense.profiling.domain.model.valueobjects.FitnessLevel;
import main.web.services.fitsense.profiling.domain.model.valueobjects.GoalType;
import main.web.services.fitsense.profiling.domain.model.valueobjects.TrainingLocation;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import main.web.services.fitsense.shared.domain.model.valueobjects.TrainingWeek;
import main.web.services.fitsense.shared.infrastructure.json.JsonSupport;
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
 * El plan 60 (GAIN_MUSCLE, GPT-5-mini, 26/10/2026) tal como se publico, con el
 * perfil del usuario 27 y el catalogo real, por el validador de produccion
 * (4D en ENFORCE, el valor de application.properties).
 * <p>
 * Siete colocaciones por sesion a 60 minutos es el tope de 4D v3.1
 * (ADENDA_4D_v3_1.md), no un incumplimiento. El «maximo 6» del log de
 * DOSIS_4D era el texto: imprimia la referencia congelada de 4D v3. Por debajo
 * de 60 minutos el tope es 6 y el mismo plan se rechaza.
 */
@Testcontainers
@Import(EsquemaVigenteSobreCatalogoAnterior.class)
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        // V36: el plan 60 se publico con el catalogo ANTERIOR (872 como estructural a
        // 3x13). Con V36, 872 es TRUNK_FLEXION y V24 le exige 2x13: ver
        // CATALOGO_V36.md, seccion 8.
        "spring.flyway.target=35"
})
class Plan60HipertrofiaIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired WeeklyTrainingPlanCommandService planes;
    @Autowired UserProfileCommandService perfiles;
    @Autowired PlanDraftValidator validador;
    @Autowired PlanPromptBuilder prompts;
    @Autowired JsonSupport json;
    @Autowired JdbcTemplate jdbc;

    static final java.util.concurrent.atomic.AtomicInteger SECUENCIA = new java.util.concurrent.atomic.AtomicInteger();
    static final LocalDate LUNES = TrainingWeek.containing(LocalDate.now()).startDate().plusWeeks(1);

    private PlanGenerationContext contexto(int minutos) {
        String email = "plan60-" + minutos + "-" + SECUENCIA.incrementAndGet() + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Prueba', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.now().minusYears(27).minusDays(10),
                BiologicalSex.FEMALE, new BigDecimal("163"), new BigDecimal("58"), null, FitnessLevel.INTERMEDIATE,
                GoalType.GAIN_MUSCLE, "Ganar masa muscular", TrainingLocation.GYM, (short) 3,
                List.of((short) 1, (short) 3, (short) 5), (short) minutos,
                List.of("body weight", "dumbbell", "barbell", "cable", "leverage machine", "bench", "smith machine",
                        "kettlebell", "band"),
                List.of(), null, false, true, true, true, true));
        return planes.prepare(GenerateWeeklyPlanCommand.firstPlan(u, LUNES)).context();
    }

    static PlanDraft.DraftExercise sr(long id, int reps) {
        return new PlanDraft.DraftExercise(id, PrescriptionType.SETS_REPS, (short) 3, (short) reps, null, null,
                (short) 90, null);
    }

    /** Las 21 colocaciones publicadas, todas 3x10 a 90 s salvo 872 y 1225 (3x13, zona pequena). */
    static List<List<PlanDraft.DraftExercise>> sesiones() {
        return List.of(
                new ArrayList<>(List.of(sr(177, 10), sr(357, 10), sr(755, 10), sr(740, 10), sr(715, 10),
                        sr(517, 10), sr(432, 10))),
                new ArrayList<>(List.of(sr(341, 10), sr(392, 10), sr(1237, 10), sr(512, 10), sr(714, 10),
                        sr(421, 10), sr(872, 13))),
                new ArrayList<>(List.of(sr(1219, 10), sr(389, 10), sr(1235, 10), sr(1023, 10), sr(1225, 13),
                        sr(1195, 10), sr(323, 10))));
    }

    static PlanDraft borrador(List<List<PlanDraft.DraftExercise>> s, int minutos) {
        return new PlanDraft(GenerationSource.AI, "gpt-5-mini", "plan 60", 600, "r", List.of(
                new PlanDraft.DraftWorkout(LUNES, WorkoutFocus.FULL_BODY, "lunes", minutos, s.get(0)),
                new PlanDraft.DraftWorkout(LUNES.plusDays(2), WorkoutFocus.FULL_BODY, "miercoles", minutos, s.get(1)),
                new PlanDraft.DraftWorkout(LUNES.plusDays(4), WorkoutFocus.FULL_BODY, "viernes", minutos, s.get(2))));
    }

    private List<String> problemas(PlanDraft draft, PlanGenerationContext ctx) {
        try {
            validador.validate(draft, ctx, 30);
            return List.of();
        } catch (InvalidPlanDraftException e) {
            return e.problems();
        }
    }

    private static List<String> topes(List<String> problemas) {
        return problemas.stream().filter(p -> p.contains("colocaciones y el maximo")).toList();
    }

    @Test
    @DisplayName("a 60 minutos el plan 60 (siete por sesion) es valido; con ocho en una sesion, V24 lo rechaza")
    void sesionDeSesentaMinutos() {
        var ctx = contexto(60);
        assertEquals(List.of(), problemas(borrador(sesiones(), 43), ctx));

        var ocho = sesiones();
        ocho.get(0).add(sr(548, 10));
        var rechazo = topes(problemas(borrador(ocho, 43), ctx));
        assertEquals(1, rechazo.size(), rechazo.toString());
        assertTrue(rechazo.get(0).contains("tiene 8 colocaciones y el maximo de 4D v3.1 para sesiones de 60 minutos es 7"),
                rechazo.toString());
    }

    @Test
    @DisplayName("a 45 minutos el mismo plan (siete por sesion) se rechaza en cada sesion; con seis, V24 lo acepta")
    void sesionDeMenosDeSesentaMinutos() {
        var ctx = contexto(45);
        var siete = problemas(borrador(sesiones(), 43), ctx);
        var rechazo = topes(siete);
        assertEquals(3, rechazo.size(), siete.toString());
        rechazo.forEach(p -> assertTrue(p.contains("tiene 7 colocaciones y el maximo de 4D v3.1 para sesiones de 45 minutos es 6"), p));

        var seis = sesiones();
        seis.forEach(s -> s.remove(s.size() - 1));
        var problemas = problemas(borrador(seis, 37), ctx);
        assertEquals(List.of(), problemas.stream().filter(p -> p.startsWith("V24")).toList(), problemas.toString());
        assertEquals(List.of(), problemas);
    }

    @Test
    @DisplayName("el prompt de GAIN_MUSCLE dice el tope efectivo: 7 a 60 minutos, 6 a 45")
    void promptConElTope() {
        for (int minutos : new int[]{60, 45}) {
            var ctx = contexto(minutos);
            int tope = minutos >= 60 ? 7 : 6;
            assertEquals(tope, PlanInputSnapshot.of(ctx).constraints().maxColocacionesDia());
            var snapshot = json.write(PlanInputSnapshot.of(ctx));
            assertTrue(snapshot.contains("\"max_colocaciones_dia\":" + tope), snapshot);
            var prompt = prompts.build(ctx, snapshot, null);
            assertTrue(prompt.contains("No pongas mas de constraints.max_colocaciones_dia ejercicios"), prompt);
        }
    }

    @Test
    @DisplayName("el log de DOSIS_4D imprime el tope que exige V24, no el 6 congelado")
    void logConElTope() {
        assertTrue(MvpDosePolicy.describeTabla(60).contains("maximo 7 colocaciones/dia"));
        assertTrue(MvpDosePolicy.describeTabla(59).contains("maximo 6 colocaciones/dia"));
        assertTrue(MvpDosePolicy.describeTabla(45).contains("maximo 6 colocaciones/dia"));
    }
}
