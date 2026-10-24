package main.web.services.fitsense.analytics.application.internal.outboundservices.acl;

import main.web.services.fitsense.analytics.domain.model.valueobjects.WeekPlanInput;
import main.web.services.fitsense.analytics.domain.model.valueobjects.WeekSessionInput;
import main.web.services.fitsense.execution.interfaces.acl.ExecutionContextFacade;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
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
     * Dias sin ninguna sesion VALIDA al cierre de la semana (17.5): la ultima
     * sesion que cuenta y llega al umbral de 17.3 (30 %), hasta el domingo
     * incluido. Una sesion del 10 % aporta su 10 % a la adherencia pero no
     * reinicia este contador.
     * <p>
     * Null si nunca tuvo una sesion valida: no es cero, y tampoco un numero
     * grande arbitrario que marcaria como abandono a alguien que acaba de
     * inscribirse.
     */
    public Short daysSinceLastWorkout(Long userId, LocalDate weekEnd, double minValidPct, ZoneId zone) {
        var finDeSemana = weekEnd.plusDays(1).atStartOfDay(zone).toOffsetDateTime();
        return executionContextFacade.fetchLastValidSessionAt(userId, minValidPct, finDeSemana)
                .map(at -> diasHasta(at, weekEnd, zone))
                .orElse(null);
    }

    /**
     * Dias entre el DIA LOCAL de la sesion y el cierre. Con la fecha UTC, una
     * sesion a las 20:00 en Lima (01:00 UTC del dia siguiente) contaba un dia
     * de menos.
     */
    public static short diasHasta(OffsetDateTime at, LocalDate weekEnd, ZoneId zone) {
        long days = ChronoUnit.DAYS.between(at.atZoneSameInstant(zone).toLocalDate(), weekEnd);
        return (short) Math.max(0, Math.min(days, Short.MAX_VALUE));
    }
}
