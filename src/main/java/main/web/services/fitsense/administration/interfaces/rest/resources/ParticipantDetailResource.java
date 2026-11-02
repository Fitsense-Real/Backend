package main.web.services.fitsense.administration.interfaces.rest.resources;

import java.time.OffsetDateTime;

/**
 * Detalle administrativo de un participante. Es la UNICA respuesta con nombre y
 * correo, y cada consulta queda en el registro de auditoria. No incluye notas de
 * salud, fecha de nacimiento ni medidas: ninguna funcion administrativa actual
 * las necesita.
 */
public record ParticipantDetailResource(
        Long participantId,
        String participantCode,
        String firstName,
        String lastName,
        String email,
        String timezone,
        String status,
        String studyStatus,
        String consentVersion,
        OffsetDateTime enrolledAt,
        OffsetDateTime withdrawnAt,
        OffsetDateTime lastLoginAt,
        OffsetDateTime createdAt,
        Profile profile
) {
    /** null si la cuenta aun no completo su perfil. */
    public record Profile(String goalType, String fitnessLevel, String trainingLocation,
                          Short daysPerWeek, Short sessionMinutes) {}
}
