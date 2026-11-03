package main.web.services.fitsense.administration.interfaces.rest.resources;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Fila del listado de participantes, seudonimizada: participantCode y el
 * identificador interno, sin nombre ni correo (estan en el detalle).
 *
 * @param studyStatus ENROLLED, WITHDRAWN o NOT_PARTICIPANT (cuenta sin consentimiento de estudio)
 */
public record ParticipantSummaryResource(
        Long participantId,
        String participantCode,
        String status,
        String studyStatus,
        OffsetDateTime enrolledAt,
        OffsetDateTime withdrawnAt,
        String goalType,
        String fitnessLevel,
        String trainingLocation,
        LocalDate lastWeekStartDate,
        BigDecimal lastOfficialAdherencePct,
        String lastRiskLevel,
        Boolean lastIsDropout
) {}
