package main.web.services.fitsense.rendimiento;

import main.web.services.fitsense.RelojPruebas;
import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.GenerationSource;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.services.FactibilidadGeneracionIA;
import main.web.services.fitsense.planning.domain.services.PlanDraftValidator;
import main.web.services.fitsense.planning.domain.services.TrainingPlanGenerator;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
import main.web.services.fitsense.planning.infrastructure.generation.ai.AiRetryFeedback;
import main.web.services.fitsense.planning.infrastructure.generation.ai.ReplicateTrainingPlanGenerator;
import main.web.services.fitsense.planning.interfaces.acl.PlanningContextFacade;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.*;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AUDITORIA DE LA PR #54 · LOS REINTENTOS SIGUEN INTACTOS CON LA COMPROBACION PREVIA ACTIVA (casos 1-7 de la orden).
 * <p>
 * Pipeline, catalogo, validador (V1-V27) y motor de reglas reales; PostgreSQL de Testcontainers. La IA es un doble
 * que devuelve, por intento, una propuesta invalida (dos sesiones en una semana de una: V2) o la propuesta valida
 * conocida (el plan determinista presentado como de la IA, que el validador acepta). Ninguna llamada a Replicate.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=true",
        "fitsense.ai.api-token=token-de-prueba-sin-uso"
})
@DisplayName("Auditoria PR #54 · reintentos 1-7 con la comprobacion previa activa")
class ReintentosYFactibilidadIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @MockitoBean ReplicateTrainingPlanGenerator ia;
    @Autowired PlanningContextFacade planning;
    @Autowired WeeklyTrainingPlanCommandService planComandos;
    @Autowired UserProfileCommandService perfiles;
    @Autowired PlanDraftValidator validador;
    @Autowired @Qualifier("ruleBasedTrainingPlanGenerator") TrainingPlanGenerator reglas;
    @Autowired JdbcTemplate jdbc;

    static final AtomicInteger SECUENCIA = new AtomicInteger();
    static final LocalDate S1 = RelojPruebas.SEMANA_REFERENCIA;
    static final List<String> GIMNASIO = List.of("body weight", "dumbbell", "barbell", "cable", "leverage machine",
            "bench", "band", "stationary bike", "treadmill", "kettlebell", "ez barbell", "smith machine");

    private final List<AiRetryFeedback> feedbacks = new ArrayList<>();
    private PlanDraft valida;
    private PlanDraft invalida;

    @BeforeEach
    void limpiar() {
        feedbacks.clear();
        when(ia.isEnabled()).thenReturn(true);
        when(ia.toDraft(anyString())).thenAnswer(inv -> "VALIDA".equals(inv.getArgument(0)) ? valida : invalida);
    }

    /** O10 de la cohorte O: LOSE_WEIGHT avanzado, una sesion de 30 min en casa. Valido a la primera en GRA y CONF-IA. */
    private long perfilO10() {
        return participante(GoalType.LOSE_WEIGHT, FitnessLevel.ADVANCED, TrainingLocation.HOME, List.of("body weight"), 30);
    }

    /** Prepara la propuesta valida conocida (y una invalida) para ese participante y su semana. */
    private void propuestas(long u) {
        var ctx = planComandos.prepare(GenerateWeeklyPlanCommand.firstPlan(u, S1)).context();
        var d = reglas.generate(ctx, List.of());
        valida = new PlanDraft(GenerationSource.AI, "doble", d.planName(), d.declaredTotalVolume(), d.rationale(),
                d.workouts());
        assertDoesNotThrow(() -> validador.validate(valida, ctx, 30), "la propuesta valida conocida debe pasar");
        var w = d.workouts().get(0);
        invalida = new PlanDraft(GenerationSource.AI, "doble", d.planName(), d.declaredTotalVolume(), d.rationale(),
                List.of(w, w));
        assertThrows(InvalidPlanDraftException.class, () -> validador.validate(invalida, ctx, 30));
        assertTrue(FactibilidadGeneracionIA.evaluar(ctx, validador.coverageEnforcement(),
                validador.capabilityEnforcement()).isEmpty(), "con solucion conocida, nunca inviable");
    }

    /** La IA devuelve, intento a intento, lo indicado (VALIDA o INVALIDA). */
    private void secuencia(String... salidas) {
        var i = new int[]{0};
        when(ia.requestOutput(any(), any(), anyInt())).thenAnswer(inv -> {
            feedbacks.add(inv.getArgument(1));
            return salidas[Math.min(i[0]++, salidas.length - 1)];
        });
    }

    private Map<String, Object> generar(long u) {
        long plan = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        return jdbc.queryForMap("SELECT generation_source, generation_attempts FROM weekly_training_plans WHERE plan_id = ?",
                plan);
    }

    @Test
    @DisplayName("caso 1 · primer intento valido: se publica la IA (1 llamada)")
    void caso1() {
        long u = perfilO10();
        propuestas(u);
        secuencia("VALIDA");
        var r = generar(u);
        assertEquals("AI", r.get("generation_source"));
        assertEquals(1, ((Number) r.get("generation_attempts")).intValue());
        assertEquals(1, feedbacks.size());
    }

    @Test
    @DisplayName("caso 2 · primero invalido y segundo valido: se publica el segundo, con el feedback del primero")
    void caso2() {
        long u = perfilO10();
        propuestas(u);
        secuencia("INVALIDA", "VALIDA");
        var r = generar(u);
        assertEquals("AI", r.get("generation_source"));
        assertEquals(2, ((Number) r.get("generation_attempts")).intValue());
        assertEquals(2, feedbacks.size());
        assertTrue(feedbacks.get(0).isEmpty());
        assertEquals("INVALIDA", feedbacks.get(1).rejectedOutput(), "el reintento recibe su propuesta rechazada");
        assertTrue(feedbacks.get(1).rejectionProblems().stream().anyMatch(m -> m.startsWith("V2")),
                feedbacks.get(1).rejectionProblems().toString());
    }

    @Test
    @DisplayName("caso 3 · dos invalidos y el tercero valido: se publica el tercero")
    void caso3() {
        long u = perfilO10();
        propuestas(u);
        secuencia("INVALIDA", "INVALIDA", "VALIDA");
        var r = generar(u);
        assertEquals("AI", r.get("generation_source"));
        assertEquals(3, ((Number) r.get("generation_attempts")).intValue());
        assertEquals(3, feedbacks.size());
        assertFalse(feedbacks.get(2).isEmpty());
    }

    @Test
    @DisplayName("caso 4 · tres invalidos: respaldo determinista (3 llamadas + respaldo)")
    void caso4() {
        long u = perfilO10();
        propuestas(u);
        secuencia("INVALIDA", "INVALIDA", "INVALIDA");
        var r = generar(u);
        assertEquals("RULE_ENGINE", r.get("generation_source"));
        assertEquals(4, ((Number) r.get("generation_attempts")).intValue());
        assertEquals(3, feedbacks.size());
    }

    @Test
    @DisplayName("caso 5 · inviabilidad demostrada (H-G9, 30 min): no se llama a la IA y decide el flujo determinista")
    void caso5() {
        long u = participante(GoalType.GENERAL_FITNESS, FitnessLevel.INTERMEDIATE, TrainingLocation.GYM, GIMNASIO, 30);
        secuencia("INVALIDA");
        var r = generar(u);
        assertEquals("RULE_ENGINE", r.get("generation_source"));
        assertEquals(1, ((Number) r.get("generation_attempts")).intValue());
        assertEquals(0, feedbacks.size(), "0 llamadas");
    }

    @Test
    @DisplayName("caso 6 · aparentemente inviable (una sesion de 30 min, avanzado) pero con solucion conocida: se llama")
    void caso6() {
        long u = perfilO10();
        propuestas(u);
        secuencia("VALIDA");
        var r = generar(u);
        assertEquals("AI", r.get("generation_source"));
        assertEquals(1, feedbacks.size());
    }

    @Test
    @DisplayName("caso 7 · factibilidad no demostrada (GENERAL_FITNESS intermedio 38 min, junto al umbral): se llama"
            + " a la IA con sus tres intentos")
    void caso7() {
        long u = participante(GoalType.GENERAL_FITNESS, FitnessLevel.INTERMEDIATE, TrainingLocation.GYM, GIMNASIO, 38);
        var ctx = planComandos.prepare(GenerateWeeklyPlanCommand.firstPlan(u, S1)).context();
        var calculo = FactibilidadGeneracionIA.calcular(ctx, validador.coverageEnforcement(),
                validador.capabilityEnforcement());
        assertTrue(calculo.isPresent());
        assertTrue(calculo.get().segundosMinimos() <= calculo.get().limiteSegundos(), "2265 s <= 2280 s");
        assertTrue(FactibilidadGeneracionIA.evaluar(ctx, validador.coverageEnforcement(),
                validador.capabilityEnforcement()).isEmpty());
        var d = reglas.generate(ctx, List.of());
        invalida = new PlanDraft(GenerationSource.AI, "doble", d.planName(), d.declaredTotalVolume(), d.rationale(),
                List.of(d.workouts().get(0), d.workouts().get(0)));
        secuencia("INVALIDA", "INVALIDA", "INVALIDA");
        var r = generar(u);
        assertEquals(3, feedbacks.size(), "los tres intentos");
        assertEquals("RULE_ENGINE", r.get("generation_source"));
        assertEquals(4, ((Number) r.get("generation_attempts")).intValue());
    }

    private long participante(GoalType objetivo, FitnessLevel nivel, TrainingLocation lugar, List<String> equipo,
                              int minutos) {
        String email = "reintentos-54-" + SECUENCIA.incrementAndGet() + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Reintentos', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1988, 5, 16), BiologicalSex.FEMALE,
                new BigDecimal("164"), new BigDecimal("63"), null, nivel, objetivo, "Reintentos", lugar, (short) 1,
                List.of((short) 6), (short) minutos, equipo, List.of(), "Ninguna", true, true, true, true, true));
        return u;
    }
}
