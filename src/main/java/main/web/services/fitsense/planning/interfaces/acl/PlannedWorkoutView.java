package main.web.services.fitsense.planning.interfaces.acl;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Vista de un entrenamiento indicado para execution.
 *
 * @param safetyConflicts por planned_exercise_id, la causa por la que ese
 *                        ejercicio choca HOY con la seguridad del participante.
 *                        Vacio si no hay conflicto o la sesion ya no esta abierta.
 */
public record PlannedWorkoutView(
        Long plannedWorkoutId,
        Long planId,
        Long userId,
        LocalDate scheduledDate,
        String focusCode,
        String workoutName,
        short expectedDurationMinutes,
        String status,
        OffsetDateTime expiresAt,
        List<PlannedExerciseTarget> exercises,
        Map<Long, String> safetyConflicts
) {
    public PlannedWorkoutView {
        safetyConflicts = safetyConflicts == null ? Map.of() : Map.copyOf(safetyConflicts);
    }

    /** Si la sesion contiene algun ejercicio que hoy no se puede ejecutar con seguridad. */
    public boolean blockedForSafety() {
        return !safetyConflicts.isEmpty();
    }

    public boolean isExpired(OffsetDateTime now) {
        return now.isAfter(expiresAt);
    }

    public boolean isOpen() {
        return "SCHEDULED".equals(status) || "IN_PROGRESS".equals(status);
    }
}
