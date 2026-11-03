package main.web.services.fitsense.orchestation;

import main.web.services.fitsense.planning.application.internal.commandservices.PlanningPolicySelector;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.domain.services.SessionDurationEstimator;
import main.web.services.fitsense.planning.infrastructure.generation.ai.AiRetryFeedback;
import main.web.services.fitsense.planning.infrastructure.generation.ai.ReplicateSessionFixGenerator;
import main.web.services.fitsense.planning.infrastructure.generation.ai.ReplicateTrainingPlanGenerator;
import main.web.services.fitsense.planning.infrastructure.generation.rules.RuleBasedTrainingPlanGenerator;
import main.web.services.fitsense.planning.interfaces.acl.PlanningContextFacade;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.*;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * IA de GENERACION simulada (sin Replicate): propone la semana del motor de reglas con la primera sesion inflada
 * (mas series) hasta superar los 2.070 s estimados de un perfil de 30 min, y DECLARA 35 min, dentro de
 * {@code round(30 x 1,15)}. Es el caso del 142 % llevado al pipeline real (validador, reintentos y respaldo).
 */
abstract class GeneracionIAV4CBase {

    @MockitoBean ReplicateTrainingPlanGenerator iaPlan;
    @MockitoBean ReplicateSessionFixGenerator iaFix;

    @Autowired PlanningContextFacade planning;
    @Autowired UserProfileCommandService perfiles;
    @Autowired RuleBasedTrainingPlanGenerator reglas;
    @Autowired SessionDurationEstimator estimador;
    @Autowired PlanningPolicySelector selector;
    @Autowired JdbcTemplate jdbc;

    static final LocalDate SEMANA = LocalDate.of(2026, 11, 2);
    static final List<String> GIMNASIO = List.of("body weight", "dumbbell", "barbell", "cable", "leverage machine",
            "bench", "band", "stationary bike", "treadmill", "kettlebell", "ez barbell", "smith machine");

    final List<String> rechazos = new CopyOnWriteArrayList<>();
    final AtomicReference<Long> segundosPropuestos = new AtomicReference<>();
    final AtomicReference<PlanGenerationContext> contexto = new AtomicReference<>();

    void iaSimulada() {
        reset(iaPlan, iaFix);
        rechazos.clear();
        when(iaFix.isEnabled()).thenReturn(false);
        when(iaPlan.isEnabled()).thenReturn(true);
        when(iaPlan.modelName()).thenReturn("simulado");
        when(iaPlan.source()).thenReturn(GenerationSource.AI);
        when(iaPlan.requestOutput(any(), any(), anyInt())).thenAnswer(inv -> {
            contexto.set(inv.getArgument(0));
            if (inv.getArgument(1) instanceof AiRetryFeedback f && f.rejectionProblems() != null)
                rechazos.addAll(f.rejectionProblems());
            return "salida-simulada";
        });
        when(iaPlan.toDraft(anyString())).thenAnswer(inv -> propuesta(contexto.get()));
    }

    /** La semana de reglas, como si la propusiera la IA, con la primera sesion inflada y declarando 35 min. */
    PlanDraft propuesta(PlanGenerationContext ctx) {
        var base = reglas.generate(ctx, List.of());
        var sesiones = new ArrayList<>(base.workouts());
        var primera = sesiones.get(0);
        var ejercicios = new ArrayList<>(primera.exercises());
        var inflada = primera;
        for (int vuelta = 0; vuelta < 6 && estimador.estimateSeconds(inflada, ctx.prescription()) <= 2550; vuelta++) {
            for (int i = 0; i < ejercicios.size(); i++) {
                var e = ejercicios.get(i);
                short s = (short) ((e.plannedSets() == null ? 1 : e.plannedSets()) + 1);
                ejercicios.set(i, new PlanDraft.DraftExercise(e.exerciseId(), e.prescriptionType(), s, e.plannedReps(),
                        e.plannedDurationSeconds(), e.targetLoadKg(), e.restSeconds(), e.notes()));
            }
            inflada = new PlanDraft.DraftWorkout(primera.scheduledDate(), primera.focus(), primera.name(), 35,
                    List.copyOf(ejercicios));
        }
        segundosPropuestos.set(estimador.estimateSeconds(inflada, ctx.prescription()));
        sesiones.set(0, inflada);
        return new PlanDraft(GenerationSource.AI, "simulado", base.planName(), base.declaredTotalVolume(),
                base.rationale(), sesiones);
    }

    long participante30(String email) {
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'IA', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1990, 6, 15), BiologicalSex.FEMALE,
                new BigDecimal("168"), new BigDecimal("68"), null, FitnessLevel.INTERMEDIATE, GoalType.GENERAL_FITNESS,
                "ia simulada", TrainingLocation.GYM, (short) 3, List.of((short) 1, (short) 3, (short) 5), (short) 30,
                GIMNASIO, List.of(), "Ninguna", true, true, true, true, true));
        return u;
    }
}
