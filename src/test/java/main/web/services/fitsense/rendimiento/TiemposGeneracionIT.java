package main.web.services.fitsense.rendimiento;

import main.web.services.fitsense.RelojPruebas;
import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.GenerationSource;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.services.PlanDraftValidator;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
import main.web.services.fitsense.planning.infrastructure.generation.ai.AiRetryFeedback;
import main.web.services.fitsense.planning.infrastructure.generation.ai.PlanInputSnapshot;
import main.web.services.fitsense.planning.infrastructure.generation.ai.PlanPromptBuilder;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.*;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import main.web.services.fitsense.shared.infrastructure.json.JsonSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RENDIMIENTO (12/11/2026) · CUANTO TARDA FITSENSE (no Replicate) EN CADA ETAPA DE UNA GENERACION. Laboratorio
 * opt-in ({@code -Dfitsense.medicion.tiempos=true}): imprime DIAG_TIEMPOS con p50/p95 por etapa y no afirma nada
 * sobre tiempos (dependen de la maquina). PostgreSQL de Testcontainers; IA apagada: ninguna llamada a Replicate.
 * <p>
 * Etapas: prepare (perfil, contexto y conjunto elegible), construir el prompt (lo que se enviaria), validar UNA
 * propuesta (el plan determinista presentado como de la IA), el motor de reglas con su validacion (el respaldo), y
 * persistir el plan.
 */
@Testcontainers
@EnabledIfSystemProperty(named = "fitsense.medicion.tiempos", matches = "true")
@SpringBootTest(properties = "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef")
@DisplayName("Rendimiento · tiempos propios de FitSense por etapa (laboratorio opt-in)")
class TiemposGeneracionIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired WeeklyTrainingPlanCommandService planes;
    @Autowired UserProfileCommandService perfiles;
    @Autowired PlanPromptBuilder prompts;
    @Autowired PlanDraftValidator validador;
    @Autowired JdbcTemplate jdbc;

    static final AtomicInteger SECUENCIA = new AtomicInteger();
    static final LocalDate S1 = RelojPruebas.SEMANA_REFERENCIA;
    static final List<String> GIMNASIO = List.of("body weight", "dumbbell", "barbell", "cable", "leverage machine",
            "bench", "band", "stationary bike", "treadmill", "kettlebell", "ez barbell", "smith machine");
    static final int REPETICIONES = 15, CALENTAMIENTO = 3;

    record Caso(String nombre, GoalType objetivo, FitnessLevel nivel, int dias, List<Short> diasSemana, int minutos) {}

    @Test
    void medir() {
        var casos = List.of(
                new Caso("LOSE_WEIGHT 3x45", GoalType.LOSE_WEIGHT, FitnessLevel.BEGINNER, 3, List.of((short) 1, (short) 3, (short) 5), 45),
                new Caso("GAIN_MUSCLE 4x60", GoalType.GAIN_MUSCLE, FitnessLevel.INTERMEDIATE, 4, List.of((short) 1, (short) 2, (short) 4, (short) 5), 60),
                new Caso("INCREASE_STRENGTH 3x60", GoalType.INCREASE_STRENGTH, FitnessLevel.INTERMEDIATE, 3, List.of((short) 1, (short) 3, (short) 5), 60),
                new Caso("GENERAL_FITNESS 3x45", GoalType.GENERAL_FITNESS, FitnessLevel.BEGINNER, 3, List.of((short) 1, (short) 3, (short) 5), 45),
                new Caso("H-G9 GENERAL_FITNESS INT 1x30", GoalType.GENERAL_FITNESS, FitnessLevel.INTERMEDIATE, 1, List.of((short) 2), 30));
        var json = new JsonSupport();
        for (var c : casos) {
            var etapas = new LinkedHashMap<String, List<Double>>();
            int caracteres = 0;
            for (int i = 0; i < REPETICIONES + CALENTAMIENTO; i++) {
                long u = participante(c);
                var cmd = GenerateWeeklyPlanCommand.firstPlan(u, S1);
                long t = System.nanoTime();
                var prepared = planes.prepare(cmd);
                double prepare = ms(t);
                var ctx = prepared.context();

                t = System.nanoTime();
                var prompt = prompts.build(ctx, json.write(PlanInputSnapshot.of(ctx)), AiRetryFeedback.none());
                double construir = ms(t);
                caracteres = prompt.length();

                t = System.nanoTime();
                var generated = planes.generate(prepared);
                double respaldo = ms(t);

                var d = generated.draft();
                var comoIa = new PlanDraft(GenerationSource.AI, d.modelName(), d.planName(), d.declaredTotalVolume(),
                        d.rationale(), d.workouts());
                t = System.nanoTime();
                try {
                    validador.validate(comoIa, ctx, 30);
                } catch (InvalidPlanDraftException ignorado) {
                    // se mide igual
                }
                double validar = ms(t);

                t = System.nanoTime();
                planes.persist(prepared, generated);
                double persistir = ms(t);

                if (i < CALENTAMIENTO) continue;
                etapas.computeIfAbsent("prepare_perfil_catalogo", k -> new ArrayList<>()).add(prepare);
                etapas.computeIfAbsent("construir_prompt", k -> new ArrayList<>()).add(construir);
                etapas.computeIfAbsent("validar_una_propuesta", k -> new ArrayList<>()).add(validar);
                etapas.computeIfAbsent("motor_de_reglas_y_validacion", k -> new ArrayList<>()).add(respaldo);
                etapas.computeIfAbsent("persistir", k -> new ArrayList<>()).add(persistir);
            }
            for (var e : etapas.entrySet())
                System.out.printf(Locale.ROOT, "DIAG_TIEMPOS caso=\"%s\" etapa=%s n=%d p50_ms=%.1f p95_ms=%.1f max_ms=%.1f%n",
                        c.nombre(), e.getKey(), e.getValue().size(), pct(e.getValue(), 0.5), pct(e.getValue(), 0.95),
                        Collections.max(e.getValue()));
            System.out.printf(Locale.ROOT, "DIAG_TIEMPOS caso=\"%s\" prompt_caracteres=%d%n", c.nombre(), caracteres);
        }
    }

    private static double ms(long desde) {
        return (System.nanoTime() - desde) / 1_000_000.0;
    }

    private static double pct(List<Double> v, double p) {
        var s = v.stream().sorted().toList();
        return s.get((int) Math.ceil(p * s.size()) - 1);
    }

    private long participante(Caso c) {
        String email = "rendimiento-tiempos-" + SECUENCIA.incrementAndGet() + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Tiempos', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1990, 6, 15), BiologicalSex.FEMALE,
                new BigDecimal("165"), new BigDecimal("62"), null, c.nivel(), c.objetivo(), "Tiempos",
                TrainingLocation.GYM, (short) c.dias(), c.diasSemana(), (short) c.minutos(), GIMNASIO, List.of(),
                "Ninguna", true, true, true, true, true));
        return u;
    }
}
