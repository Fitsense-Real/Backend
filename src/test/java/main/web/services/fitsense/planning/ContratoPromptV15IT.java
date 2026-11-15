package main.web.services.fitsense.planning;

import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanGenerationContext;
import main.web.services.fitsense.planning.domain.model.valueobjects.PrescriptionType;
import main.web.services.fitsense.planning.domain.model.valueobjects.WorkoutFocus;
import main.web.services.fitsense.planning.domain.services.CatalogEligibilityPolicy;
import main.web.services.fitsense.planning.domain.services.PlanDraftValidator;
import main.web.services.fitsense.planning.domain.services.RepartoV15;
import main.web.services.fitsense.planning.domain.services.TrainingPlanGenerator;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.*;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * H2 · CONTRATO PROMPT–V15 (prueba permanente, sin Replicate). Portada de la sonda de la auditoria del caso 39
 * (rama audit/caso-usuario-39, Caso39ContratoV15IT#sondaV15): sobre un contexto real, sustituye una sesion por
 * composiciones con n = 2..6 ejercicios de trabajo, k = 1..n del mismo grupo y 2 complementos, valida la semana
 * completa con el VALIDADOR REAL y exige que {@link RepartoV15#topeMismoGrupo} prediga exactamente cuando V15 rechaza
 * por la regla de la mitad. Si alguien cambia V15 o la funcion, esta prueba falla: lo que se comunica a la IA
 * (PR-1.9) y lo que se verifica no pueden separarse en silencio.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false"
})
@DisplayName("H2 · contrato prompt–V15: la funcion compartida predice al validador real")
class ContratoPromptV15IT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired WeeklyTrainingPlanCommandService planComandos;
    @Autowired UserProfileCommandService perfiles;
    @Autowired PlanDraftValidator validador;
    @Autowired @Qualifier("ruleBasedTrainingPlanGenerator") TrainingPlanGenerator reglas;
    @Autowired JdbcTemplate jdbc;

    static final LocalDate SEMANA = LocalDate.of(2026, 11, 16);
    static final AtomicInteger SEQ = new AtomicInteger(1000);
    static final String POLITICA = "por-defecto";

    static final List<String> EQUIPO_39 = List.of("assisted", "band", "barbell", "bench", "body weight", "cable",
            "captains chair", "dumbbell", "ez barbell", "ghd bench", "gymnastic rings", "hyperextension bench",
            "landmine", "leverage machine", "olympic barbell", "other", "parallel bars", "preacher bench",
            "pull-up bar", "rope", "sled machine", "smith machine", "squat rack", "stationary bike", "step platform",
            "suspension straps", "treadmill", "vertical pole", "weighted");

    // ---------------------------------------------------------------------------------------------------------
    @Test
    @DisplayName("H2: el validador real (V15) admite exactamente RepartoV15.topeMismoGrupo ejercicios del mismo grupo")
    void sondaV15() throws Exception {
        long u = usuario(null, GoalType.GENERAL_FITNESS, FitnessLevel.BEGINNER, TrainingLocation.GYM, EQUIPO_39,
                (short) 4, List.of((short) 1, (short) 4, (short) 5, (short) 7), (short) 60);
        var ctx = planComandos.prepare(GenerateWeeklyPlanCommand.firstPlan(u, SEMANA)).context();
        var base = reglas.generate(ctx, List.of());
        var compl = CatalogEligibilityPolicy.complementos(ctx.availableExercises(), ctx.profile().goalType());
        var porGrupo = ctx.availableExercises().stream().filter(c -> !compl.contains(c.exerciseId()))
                .filter(c -> c.defaultPrescription() == PrescriptionType.SETS_REPS)
                .collect(Collectors.groupingBy(CandidateExercise::bodyPartCode, TreeMap::new, Collectors.toList()));
        var cardio = ctx.availableExercises().stream().filter(c -> compl.contains(c.exerciseId())
                && "cardio".equals(c.bodyPartCode())).findFirst().orElseThrow();
        var movilidad = ctx.availableExercises().stream().filter(c -> compl.contains(c.exerciseId())
                && !"cardio".equals(c.bodyPartCode())).findFirst().orElseThrow();

        var sb = new StringBuilder("politica;fecha;enfoque;n_trabajo;complementos;mismo_grupo;grupo;v15_mitad_rechaza;tope_funcion\n");
        for (int idx = 0; idx < base.workouts().size(); idx++) {
            var w = base.workouts().get(idx);
            var grupos = w.focus().bodyPartCodes();
            String dominante = grupos.get(0);
            for (int n = 2; n <= 6; n++) {
                for (int k = 1; k <= n; k++) {
                    var ej = new ArrayList<PlanDraft.DraftExercise>();
                    var usados = new HashSet<Long>();
                    for (int i = 0; i < k; i++) ej.add(serie(siguiente(porGrupo.get(dominante), usados)));
                    int g = 1;
                    while (ej.size() < n) {
                        var otro = grupos.get(g % grupos.size()); g++;
                        if (otro.equals(dominante)) continue;
                        var c = siguienteOpcional(porGrupo.getOrDefault(otro, List.of()), usados);
                        if (c != null) ej.add(serie(c));
                    }
                    ej.add(duracion(cardio, 90));
                    ej.add(duracion(movilidad, 30));
                    var semana = new ArrayList<>(base.workouts());
                    semana.set(idx, new PlanDraft.DraftWorkout(w.scheduledDate(), w.focus(), w.name(),
                            w.expectedDurationMinutes(), ej));
                    var problemas = problemas(new PlanDraft(base.source(), null, base.planName(), null,
                            base.rationale(), semana), ctx);
                    boolean rechaza = problemas.stream().anyMatch(p -> p.startsWith("V15: el entrenamiento del "
                            + w.scheduledDate()) && p.contains(" dedica "));
                    sb.append(POLITICA).append(';').append(w.scheduledDate()).append(';').append(w.focus()).append(';')
                            .append(n).append(";2;").append(k).append(';').append(dominante).append(';')
                            .append(rechaza).append(';').append(RepartoV15.topeMismoGrupo(w.focus(), n)).append('\n');
                    Integer tope = RepartoV15.topeMismoGrupo(w.focus(), n);
                    assertEquals(tope != null && k > tope, rechaza,
                            "la funcion debe predecir exactamente a V15: " + w.focus() + " n=" + n + " k=" + k);
                }
            }
        }
        assertTrue(sb.toString().lines().count() > 40, "la sonda recorrio las combinaciones");
    }

    // ---------------------------------------------------------------------------------------------------------

    private long usuario(Long id, GoalType objetivo, FitnessLevel nivel, TrainingLocation lugar, List<String> equipo,
                         short dias, List<Short> disponibles, short minutos) {
        long u = id != null ? id : SEQ.incrementAndGet();
        jdbc.update("INSERT INTO users (user_id, email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " OVERRIDING SYSTEM VALUE VALUES (?, ?, 'x', 'Contrato', now(), now())", u, "c" + u + "@pruebas.invalid");
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1985, 6, 1), BiologicalSex.MALE,
                new BigDecimal("180"), new BigDecimal("100"), new BigDecimal("80"), nivel, objetivo, "Contrato",
                lugar, dias, disponibles, minutos, equipo, List.of(), null, true, true, true, false, false));
        return u;
    }

    private List<String> problemas(PlanDraft d, PlanGenerationContext ctx) {
        try {
            validador.validate(d, ctx, 30);
            return List.of();
        } catch (InvalidPlanDraftException e) {
            return e.problems();
        }
    }

    private static CandidateExercise siguiente(List<CandidateExercise> pool, Set<Long> usados) {
        var c = siguienteOpcional(pool, usados);
        if (c == null) throw new IllegalStateException("sin candidatos suficientes");
        return c;
    }

    private static CandidateExercise siguienteOpcional(List<CandidateExercise> pool, Set<Long> usados) {
        for (var c : pool) if (usados.add(c.exerciseId())) return c;
        return null;
    }

    private static PlanDraft.DraftExercise serie(CandidateExercise c) {
        return new PlanDraft.DraftExercise(c.exerciseId(), PrescriptionType.SETS_REPS, (short) 3, (short) 10, null,
                null, (short) 90, null);
    }

    private static PlanDraft.DraftExercise duracion(CandidateExercise c, int segundos) {
        return new PlanDraft.DraftExercise(c.exerciseId(), PrescriptionType.DURATION, (short) 2, null, segundos,
                null, (short) 30, null);
    }
}
