package main.web.services.fitsense.planning.interfaces.rest.transform;

import main.web.services.fitsense.catalog.interfaces.acl.EligibleExerciseView;
import main.web.services.fitsense.planning.domain.model.aggregates.WeeklyTrainingPlan;
import main.web.services.fitsense.planning.domain.model.entities.PlannedWorkout;
import main.web.services.fitsense.planning.domain.model.valueobjects.EffortGuidance;
import main.web.services.fitsense.planning.interfaces.rest.resources.CapabilityCoverageResource;
import main.web.services.fitsense.planning.interfaces.rest.resources.EffortGuidanceResource;
import main.web.services.fitsense.planning.interfaces.rest.resources.PlanSummaryResource;
import main.web.services.fitsense.planning.interfaces.rest.resources.PlannedExerciseResource;
import main.web.services.fitsense.planning.interfaces.rest.resources.PlannedWorkoutResource;
import main.web.services.fitsense.planning.interfaces.rest.resources.WeeklyTrainingPlanResource;

import java.util.Map;

/**
 * El detalle de los ejercicios llega por parametro y no se lee del agregado:
 * planning guarda ids, no nombres ni rutas de media. Resolverlo aqui mantiene
 * el catalogo como unica fuente y evita duplicar texto que puede quedar
 * desfasado.
 */
public class WeeklyTrainingPlanResourceFromEntityAssembler {

    private WeeklyTrainingPlanResourceFromEntityAssembler() {}

    public static final String AVISO_BLOQUEO = "Esta sesion tiene un ejercicio que ya no es seguro para ti. "
            + "No la inicies: necesita una correccion.";

    public static WeeklyTrainingPlanResource toResourceFromEntity(
            WeeklyTrainingPlan entity, Map<Long, EligibleExerciseView> catalogo,
            EffortGuidance guidance) {
        return toResourceFromEntity(entity, catalogo, guidance, Map.of());
    }

    /**
     * @param conflictos por planned_exercise_id, la causa de seguridad de HOY
     *                   (solo sesiones abiertas de un plan activo)
     */
    public static WeeklyTrainingPlanResource toResourceFromEntity(
            WeeklyTrainingPlan entity, Map<Long, EligibleExerciseView> catalogo,
            EffortGuidance guidance, Map<Long, String> conflictos) {
        return toResourceFromEntity(entity, catalogo, guidance, conflictos, null);
    }

    public static WeeklyTrainingPlanResource toResourceFromEntity(
            WeeklyTrainingPlan entity, Map<Long, EligibleExerciseView> catalogo,
            EffortGuidance guidance, Map<Long, String> conflictos, CapabilityCoverageResource cobertura) {

        var workouts = entity.workoutsView().stream()
                .map(workout -> toWorkoutResource(workout, catalogo, conflictos))
                .toList();

        return new WeeklyTrainingPlanResource(
                entity.getId(),
                entity.getWeekNumber(),
                entity.getWeekStartDate(),
                entity.getWeekEndDate(),
                entity.getPlanVersion(),
                entity.getParentPlanId(),
                entity.getPlannedDaysCount(),
                entity.getStatus().name(),
                entity.getGenerationSource().name(),
                entity.getModelName(),
                entity.getAdjustmentApplied(),
                entity.getAdjustmentReason(),
                entity.getActivatedAt(),
                workouts,
                guidance == null ? null : new EffortGuidanceResource(
                        guidance.repsInReserve(), guidance.setsRepsText(),
                        guidance.durationText(), guidance.loadText()),
                cobertura);
    }

    public static PlanSummaryResource toSummaryFromEntity(WeeklyTrainingPlan entity) {
        return new PlanSummaryResource(
                entity.getId(),
                entity.getWeekNumber(),
                entity.getWeekStartDate(),
                entity.getWeekEndDate(),
                entity.getPlanVersion(),
                entity.getPlannedDaysCount(),
                entity.getStatus().name(),
                entity.getGenerationSource().name(),
                entity.getAdjustmentApplied());
    }

    public static PlannedWorkoutResource toWorkoutResource(
            PlannedWorkout workout, Map<Long, EligibleExerciseView> catalogo) {
        return toWorkoutResource(workout, catalogo, Map.of());
    }

    public static PlannedWorkoutResource toWorkoutResource(
            PlannedWorkout workout, Map<Long, EligibleExerciseView> catalogo, Map<Long, String> conflictos) {

        var exercises = workout.exercisesView().stream()
                .map(exercise -> {
                    var detalle = catalogo.get(exercise.getExerciseId());
                    return new PlannedExerciseResource(
                            exercise.getId(),
                            exercise.getExerciseId(),
                            detalle == null ? null : detalle.nameEs(),
                            detalle == null ? null : detalle.targetMuscle(),
                            detalle == null ? null : detalle.bodyPartCode(),
                            detalle == null ? null : detalle.equipmentCode(),
                            detalle == null ? 0 : detalle.difficultyLevel(),
                            exercise.getExerciseOrder(),
                            exercise.getPrescriptionType().name(),
                            exercise.getPlannedSets(),
                            exercise.getPlannedReps(),
                            exercise.getPlannedDurationSeconds(),
                            exercise.getTargetLoadKg(),
                            exercise.getRestSeconds(),
                            exercise.getNotes(),
                            detalle == null ? null : detalle.gifPath(),
                            detalle == null ? null : detalle.imagePath(),
                            detalle == null ? null : detalle.mediaAttribution(),
                            conflictos.get(exercise.getId()));
                })
                .toList();

        boolean bloqueada = workout.exercisesView().stream().anyMatch(e -> conflictos.containsKey(e.getId()));
        return new PlannedWorkoutResource(
                workout.getId(),
                workout.getScheduledDate(),
                workout.getFocusCode().name(),
                workout.getWorkoutName(),
                workout.getExpectedDurationMinutes(),
                workout.getDisplayOrder(),
                workout.getStatus().name(),
                workout.getSkipReason() == null ? null : workout.getSkipReason().name(),
                workout.getExpiresAt(),
                exercises,
                bloqueada,
                bloqueada ? AVISO_BLOQUEO : null);
    }
}