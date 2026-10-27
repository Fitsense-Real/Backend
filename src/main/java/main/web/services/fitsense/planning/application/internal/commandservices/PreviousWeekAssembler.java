package main.web.services.fitsense.planning.application.internal.commandservices;

import main.web.services.fitsense.execution.interfaces.acl.WorkoutResultView;
import main.web.services.fitsense.planning.application.internal.outboundservices.acl.ExternalCatalogService;
import main.web.services.fitsense.planning.application.internal.outboundservices.acl.ExternalExecutionService;
import main.web.services.fitsense.planning.domain.model.aggregates.WeeklyTrainingPlan;
import main.web.services.fitsense.planning.domain.model.entities.PlannedWorkout;
import main.web.services.fitsense.planning.domain.model.valueobjects.PreviousWeekSummary;
import main.web.services.fitsense.planning.domain.model.valueobjects.WorkoutStatus;
import main.web.services.fitsense.planning.infrastructure.persistence.jpa.repositories.PlannedWorkoutRepository;
import main.web.services.fitsense.planning.infrastructure.persistence.jpa.repositories.WeeklyTrainingPlanRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Arma el bloque previous_week: lo que se prescribio la semana anterior y lo
 * que la persona hizo de verdad, ejercicio por ejercicio.
 * <p>
 * Lo hecho sale del intento que cuenta para la adherencia. Si se leyera otro
 * intento, la IA y la metrica verian datos distintos de la misma semana.
 */
@Component
public class PreviousWeekAssembler {

    private final WeeklyTrainingPlanRepository planRepository;
    private final PlannedWorkoutRepository plannedWorkoutRepository;
    private final ExternalCatalogService externalCatalogService;
    private final ExternalExecutionService externalExecutionService;
    private final ReduccionAutorizadaResolver reduccionAutorizadaResolver;

    public PreviousWeekAssembler(WeeklyTrainingPlanRepository planRepository,
                                 PlannedWorkoutRepository plannedWorkoutRepository,
                                 ExternalCatalogService externalCatalogService,
                                 ExternalExecutionService externalExecutionService,
                                 ReduccionAutorizadaResolver reduccionAutorizadaResolver) {
        this.planRepository = planRepository;
        this.plannedWorkoutRepository = plannedWorkoutRepository;
        this.externalCatalogService = externalCatalogService;
        this.externalExecutionService = externalExecutionService;
        this.reduccionAutorizadaResolver = reduccionAutorizadaResolver;
    }

    public PreviousWeekSummary assemble(Long userId, LocalDate previousWeekStart,
                                        int durationToRepsDivisor,
                                        BigDecimal adherencePct, BigDecimal averageRpe) {
        var plans = planRepository.findByUserIdAndWeekStartDateOrderByPlanVersionAsc(
                userId, previousWeekStart);
        if (plans.isEmpty()) return PreviousWeekSummary.empty();

        // Ultima version del plan. LIMITACION: si el plan se reemplazo a mitad
        // de semana, lo registrado sobre entrenamientos de la version anterior
        // (REPLACED) no entra aqui.
        var plan = plans.get(plans.size() - 1);

        int totalVolume = plan.equivalentVolume(durationToRepsDivisor);

        var activeWorkouts = plan.workoutsView().stream()
                .filter(workout -> workout.getStatus() != WorkoutStatus.REPLACED)
                .toList();

        var names = externalCatalogService.fetchNames(activeWorkouts.stream()
                .flatMap(workout -> workout.exercisesView().stream())
                .map(exercise -> exercise.getExerciseId())
                .collect(Collectors.toSet()));

        var results = externalExecutionService.fetchResultsByWorkout(activeWorkouts.stream()
                .map(PlannedWorkout::getId)
                .toList());

        var bodyPartDistribution = new HashMap<String, Integer>();
        var workouts = new ArrayList<PreviousWeekSummary.WorkoutOutcome>();
        var prescriptions = new ArrayList<PreviousWeekSummary.PrescriptionOutcome>();

        for (var workout : activeWorkouts) {
            bodyPartDistribution.merge(workout.getFocusCode().name(), 1, Integer::sum);

            var result = results.get(workout.getId());
            workouts.add(toWorkoutOutcome(workout, result));

            Map<Long, WorkoutResultView.ExerciseResult> byPlannedExercise = result == null
                    ? Map.of()
                    : result.exercises().stream().collect(Collectors.toMap(
                    WorkoutResultView.ExerciseResult::plannedExerciseId,
                    Function.identity(), (first, second) -> first));

            for (var exercise : workout.exercisesView()) {
                var done = byPlannedExercise.get(exercise.getId());
                prescriptions.add(new PreviousWeekSummary.PrescriptionOutcome(
                        workout.getId(),
                        workout.getScheduledDate(),
                        exercise.getExerciseId(),
                        names.getOrDefault(exercise.getExerciseId(), null),
                        exercise.getPrescriptionType() == null ? null : exercise.getPrescriptionType().name(),
                        exercise.getPlannedSets(),
                        exercise.getPlannedReps(),
                        exercise.getPlannedDurationSeconds(),
                        exercise.getTargetLoadKg(),
                        done == null ? null : done.actualSets(),
                        done == null ? null : done.actualRepsTotal(),
                        done == null ? null : done.actualDurationSeconds(),
                        done == null ? null : done.actualLoadKg(),
                        done == null ? null : done.completionPercentage(),
                        done == null || done.status() == null
                                ? PreviousWeekSummary.NOT_RECORDED : done.status(),
                        done == null ? null : done.skipReason(),
                        workout.getStatus().name(),
                        exercise.getRestSeconds()));
            }
        }

        var usedLast7Days = plannedWorkoutRepository.findExerciseIdsUsedBetween(
                userId, previousWeekStart, previousWeekStart.plusDays(7));

        return new PreviousWeekSummary(adherencePct, averageRpe, totalVolume,
                bodyPartDistribution, List.copyOf(workouts), List.copyOf(prescriptions),
                List.copyOf(usedLast7Days), reduccionAutorizadaResolver.resolver(activeWorkouts));
    }

    private static PreviousWeekSummary.WorkoutOutcome toWorkoutOutcome(PlannedWorkout workout,
                                                                       WorkoutResultView result) {
        return new PreviousWeekSummary.WorkoutOutcome(
                workout.getId(),
                workout.getScheduledDate(),
                workout.getFocusCode().name(),
                workout.getStatus().name(),
                workout.getSkipReason() == null ? null : workout.getSkipReason().name(),
                hayRegistro(result != null, workout.getStatus()),
                result == null ? null : result.sessionStatus(),
                result == null ? null : result.sessionRpe(),
                result == null ? null : result.completionPercentage(),
                workout.getWorkoutName(),
                workout.getExpectedDurationMinutes());
    }

    /**
     * Si de esa sesion HAY INFORMACION. Alineado con V30.
     * <p>
     * Antes era {@code result != null}: solo contaba como registrada si existia
     * un resultado de ejecucion. Pero una OMISION DECLARADA es informacion: la
     * persona nos dijo que no entreno. {@code WeeklyMetricsCalculator} ya lo
     * contaba asi desde V30, y {@code planning} no, de modo que el mismo
     * concepto significaba dos cosas en dos modulos.
     * <p>
     * Importa porque este valor viaja al prompt por dos caminos -el bloque de
     * sesiones senaladas de la adaptacion y {@code previous_week.workouts} de la
     * generacion inicial-, y la distincion que se perdia es justo la que decide
     * si adaptar: una omision declarada no es una sesion desconocida.
     * <p>
     * Se compara con la CONSTANTE del enum, no con la cadena "SKIPPED": si
     * manana se renombra, esto deja de compilar en vez de devolver false en
     * silencio.
     * <p>
     * No es privado para que la prueba pueda recorrer la tabla de verdad sin
     * montar una entidad JPA.
     *
     * @param conResultado si existe un {@link WorkoutResultView} de esa sesion
     * @param status       el estado del previsto; nunca null en esta ruta
     */
    static boolean hayRegistro(boolean conResultado, WorkoutStatus status) {
        return conResultado || status == WorkoutStatus.SKIPPED;
    }
}