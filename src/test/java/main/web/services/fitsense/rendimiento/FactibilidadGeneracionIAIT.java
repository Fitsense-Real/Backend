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
import main.web.services.fitsense.planning.infrastructure.generation.ai.ReplicateTrainingPlanGenerator;
import main.web.services.fitsense.planning.infrastructure.generation.rules.RuleBasedTrainingPlanGenerator;
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
 * RENDIMIENTO (12/11/2026) · LA COMPROBACION PREVIA EVITA LAS LLAMADAS DE H-G9 Y NINGUNA MAS.
 * <p>
 * Con catalogo, validador y motor de reglas reales (PostgreSQL de Testcontainers). La IA es un doble que cuenta las
 * llamadas y rechaza cada propuesta (como Replicate en H-G9): ninguna llamada a Replicate.
 * <ol>
 *   <li>La rejilla de H-G9 ({@code 03_DIAGNOSTICO_HG9.md}): GENERAL_FITNESS de 1 dia en gimnasio, tres niveles y
 *       30-45 min. Imposible exactamente en intermedio y avanzado con 30 y 35 min: 0 llamadas (antes 3) y el mismo
 *       motor de reglas. Los perfiles parecidos factibles (40-45 min, principiante) siguen llamando a la IA.</li>
 *   <li>Contraejemplos: para cada perfil de 1 dia (cuatro objetivos, tres niveles, 30-75 min, gimnasio y casa), si
 *       el plan del motor de reglas -o una de sus variantes deterministas- pasa el validador COMO SI LO HUBIERA
 *       PROPUESTO LA IA, existe una propuesta valida y la comprobacion no puede marcarlo imposible.</li>
 * </ol>
 * Imprime DIAG_FACTIBILIDAD por perfil.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=true",
        "fitsense.ai.api-token=token-de-prueba-sin-uso"
})
@DisplayName("Rendimiento · comprobacion previa de factibilidad: H-G9 sin llamadas y ningun falso positivo")
class FactibilidadGeneracionIAIT {

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
    static final List<String> CASA = List.of("body weight");

    private final AtomicInteger llamadas = new AtomicInteger();

    @BeforeEach
    void iaQueRechaza() {
        when(ia.isEnabled()).thenReturn(true);
        when(ia.requestOutput(any(), any(), anyInt())).thenAnswer(inv -> {
            llamadas.incrementAndGet();
            throw new InvalidPlanDraftException(List.of("V4: propuesta simulada rechazada"));
        });
    }

    @Test
    @DisplayName("H-G9: intermedio/avanzado 30-35 min sin llamadas (antes 3); los parecidos factibles siguen llamando")
    void rejillaHG9() {
        for (var nivel : List.of(FitnessLevel.BEGINNER, FitnessLevel.INTERMEDIATE, FitnessLevel.ADVANCED))
            for (int minutos : List.of(30, 35, 40, 45)) {
                long u = participante(GoalType.GENERAL_FITNESS, nivel, TrainingLocation.GYM, GIMNASIO, minutos);
                var ctx = planComandos.prepare(new GenerateWeeklyPlanCommand(u, S1, null, true, null, null)).context();
                var inviable = FactibilidadGeneracionIA.evaluar(ctx, validador.coverageEnforcement(),
                        validador.capabilityEnforcement());
                boolean esperado = nivel != FitnessLevel.BEGINNER && minutos <= 35;
                System.out.println("DIAG_FACTIBILIDAD HG9 " + nivel + " " + minutos + " min -> "
                        + inviable.map(FactibilidadGeneracionIA.Inviabilidad::describe).orElse("factible"));
                assertEquals(esperado, inviable.isPresent(), nivel + " " + minutos + " min");

                int antes = llamadas.get();
                long plan = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
                var fila = jdbc.queryForMap("SELECT generation_source, generation_attempts FROM weekly_training_plans"
                        + " WHERE plan_id = ?", plan);
                assertEquals("RULE_ENGINE", fila.get("generation_source"), "la IA simulada rechaza todo");
                assertEquals(esperado ? 0 : 3, llamadas.get() - antes, nivel + " " + minutos + " min: llamadas");
                assertEquals(esperado ? 1 : 4, ((Number) fila.get("generation_attempts")).intValue());
            }
    }

    @Test
    @DisplayName("sin falsos positivos: si el plan determinista vale como propuesta de la IA, nunca se marca imposible")
    void sinContraejemplos() {
        int perfilesEvaluados = 0, inviables = 0, conContraejemplo = 0;
        var resumen = new TreeMap<String, Integer>();
        for (var objetivo : GoalType.values())
            for (var nivel : FitnessLevel.values())
                for (var lugar : List.of(TrainingLocation.GYM, TrainingLocation.HOME))
                    for (int minutos = 30; minutos <= 75; minutos += 5) {
                        Long u;
                        try {
                            u = participante(objetivo, nivel, lugar, lugar == TrainingLocation.GYM ? GIMNASIO : CASA,
                                    minutos);
                        } catch (RuntimeException e) {
                            continue;   // combinacion que el perfil no admite (P-01)
                        }
                        main.web.services.fitsense.planning.domain.model.valueobjects.PlanGenerationContext ctx;
                        try {
                            ctx = planComandos.prepare(new GenerateWeeklyPlanCommand(u, S1, null, true, null, null))
                                    .context();
                        } catch (RuntimeException e) {
                            continue;   // p. ej. FUERZA_SIN_TIEMPO: ya no se genera
                        }
                        perfilesEvaluados++;
                        var inviable = FactibilidadGeneracionIA.evaluar(ctx, validador.coverageEnforcement(),
                                validador.capabilityEnforcement());
                        boolean contraejemplo = existePropuestaValidaDeIA(ctx);
                        if (contraejemplo) conContraejemplo++;
                        if (inviable.isPresent()) {
                            inviables++;
                            resumen.merge(objetivo + "/" + nivel + "/" + lugar, 1, Integer::sum);
                        }
                        System.out.println("DIAG_FACTIBILIDAD " + objetivo + " " + nivel + " " + lugar + " " + minutos
                                + " min -> " + (inviable.isPresent() ? "INVIABLE " + inviable.get().describe()
                                : "factible") + (contraejemplo ? " [propuesta valida conocida]" : ""));
                        assertFalse(inviable.isPresent() && contraejemplo,
                                "FALSO POSITIVO: hay una propuesta valida y se marco imposible: " + objetivo + " "
                                        + nivel + " " + lugar + " " + minutos);
                    }
        System.out.println("DIAG_FACTIBILIDAD_RESUMEN perfiles=" + perfilesEvaluados + " inviables=" + inviables
                + " con_propuesta_valida_conocida=" + conContraejemplo + " inviables_por_grupo=" + resumen);
        assertTrue(perfilesEvaluados > 100, "la rejilla debe cubrir los cuatro objetivos: " + perfilesEvaluados);
        assertTrue(conContraejemplo > 0, "la busqueda de contraejemplos debe encontrar propuestas validas");
    }

    /** El plan del motor de reglas, o una de sus variantes, validado como si fuera de la IA (V23 incluida). */
    private boolean existePropuestaValidaDeIA(
            main.web.services.fitsense.planning.domain.model.valueobjects.PlanGenerationContext ctx) {
        var borradores = new ArrayList<PlanDraft>();
        try {
            borradores.add(reglas.generate(ctx, List.of()));
        } catch (RuntimeException ignorado) {
            // sin borrador
        }
        if (reglas instanceof RuleBasedTrainingPlanGenerator r)
            for (int k = 1; k <= 4; k++)
                try {
                    borradores.add(r.generarVariante(ctx, List.of(), k));
                } catch (RuntimeException ignorado) {
                    // siguiente
                }
        for (var d : borradores) {
            var comoIa = new PlanDraft(GenerationSource.AI, d.modelName(), d.planName(), d.declaredTotalVolume(),
                    d.rationale(), d.workouts());
            try {
                validador.validate(comoIa, ctx, 30);
                return true;
            } catch (InvalidPlanDraftException ignorado) {
                // no vale como propuesta de la IA
            }
        }
        return false;
    }

    private long participante(GoalType objetivo, FitnessLevel nivel, TrainingLocation lugar, List<String> equipo,
                              int minutos) {
        String email = "rendimiento-fact-" + SECUENCIA.incrementAndGet() + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Rendimiento', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1990, 6, 15), BiologicalSex.FEMALE,
                new BigDecimal("165"), new BigDecimal("62"), null, nivel, objetivo, "Rendimiento", lugar, (short) 1,
                List.of((short) 2), (short) minutos, equipo, List.of(), "Ninguna", true, true, true, true, true));
        return u;
    }
}
