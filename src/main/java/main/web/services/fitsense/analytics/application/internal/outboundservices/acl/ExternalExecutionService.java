package main.web.services.fitsense.analytics.application.internal.outboundservices.acl;

import main.web.services.fitsense.analytics.domain.model.valueobjects.WeekPlanInput;
import main.web.services.fitsense.analytics.domain.model.valueobjects.WeekSessionInput;
import main.web.services.fitsense.execution.interfaces.acl.ExecutionContextFacade;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Capa anticorrupcion hacia execution: el numerador de la semana. */
@Service("analyticsExternalPlanningService")
public class ExternalExecutionService {

    private final ExecutionContextFacade executionContextFacade;

    public ExternalExecutionService(ExecutionContextFacade executionContextFacade) {
        this.executionContextFacade = executionContextFacade;
    }

    /**
     * Las sesiones que cuentan de los entrenamientos previstos de la semana,
     * por entrenamiento y no por fecha de inicio: el reporte retroactivo y la
     * zona horaria no cambian a que semana pertenece una sesion.
     */
    public List<WeekSessionInput> fetchWeekSessions(WeekPlanInput plan, int durationToRepsDivisor) {
        if (plan == null || plan.workouts() == null || plan.workouts().isEmpty()) return List.of();
        var ids = plan.workouts().stream().map(WeekPlanInput.ScheduledWorkout::plannedWorkoutId).toList();
        return executionContextFacade
                .fetchCountedSessionsOf(ids, durationToRepsDivisor).stream()
                .map(session -> new WeekSessionInput(
                        session.plannedWorkoutId(),
                        session.completionPercentage(),
                        session.activeMinutes(),
                        session.sessionRpe(),
                        session.satisfaction(),
                        session.completedExercises(),
                        session.executedVolume(),
                        session.executedReps(),
                        session.executedSeconds(),
                        session.dominantSkipReason()))
                .toList();
    }

    /**
     * Dias sin ninguna sesion valida al cierre (17.5). Null si nunca entreno: no
     * es cero, y tampoco un numero grande arbitrario que marcaria como abandono
     * a alguien que acaba de inscribirse.
     */
    public Short daysSinceLastWorkout(Long userId, LocalDate weekEnd) {
        return executionContextFacade.fetchLastCountedSessionAt(userId)
                .map(at -> {
                    long days = ChronoUnit.DAYS.between(at.toLocalDate(), weekEnd);
                    return (short) Math.max(0, Math.min(days, Short.MAX_VALUE));
                })
                .orElse(null);
    }
}