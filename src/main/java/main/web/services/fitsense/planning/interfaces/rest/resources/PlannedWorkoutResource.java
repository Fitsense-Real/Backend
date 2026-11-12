package main.web.services.fitsense.planning.interfaces.rest.resources;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record PlannedWorkoutResource(
        Long plannedWorkoutId,
        LocalDate scheduledDate,
        String focusCode,
        String workoutName,
        short expectedDurationMinutes,
        short displayOrder,
        String status,
        String skipReason,
        OffsetDateTime expiresAt,
        List<PlannedExerciseResource> exercises,
        /**
         * La sesion contiene un ejercicio que hoy no es seguro: no se puede
         * iniciar (el backend lo rechaza) y requiere correccion.
         */
        boolean safetyBlocked,
        /** El aviso para mostrar cuando safetyBlocked; null si no. */
        String safetyBlockMessage
) {}
