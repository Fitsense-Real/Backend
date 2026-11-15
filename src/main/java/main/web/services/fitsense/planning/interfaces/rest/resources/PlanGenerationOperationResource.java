package main.web.services.fitsense.planning.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import main.web.services.fitsense.planning.domain.model.aggregates.PlanGenerationOperation;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Estado de la generacion del primer plan (V58). La app guarda
 * {@code operationId} y consulta hasta que {@code terminal} sea true.
 */
public record PlanGenerationOperationResource(
        UUID operationId,
        @Schema(example = "PROCESSING", allowableValues = {"PENDING", "PROCESSING", "COMPLETED", "FAILED", "NO_FACTIBLE"})
        String status,
        boolean terminal,
        LocalDate weekStartDate,
        @Schema(description = "Plan publicado; solo con COMPLETED") Long planId,
        @Schema(description = "Solo con FAILED o NO_FACTIBLE. NO_FACTIBLE usa los codigos de NoFactibleReason")
        String reasonCode,
        @Schema(description = "Texto para el participante; solo con FAILED o NO_FACTIBLE") String message,
        @Schema(description = "true si un reintento explicito (POST de nuevo) puede salir bien") boolean retryable,
        OffsetDateTime createdAt,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        @Schema(description = "Cuando volver a consultar; null si ya termino") Integer pollAfterSeconds
) {
    public static final int POLL_AFTER_SECONDS = 3;

    public static PlanGenerationOperationResource of(PlanGenerationOperation op) {
        boolean terminal = op.getStatus().isTerminal();
        return new PlanGenerationOperationResource(op.getId(), op.getStatus().name(), terminal,
                op.getWeekStartDate(), op.getPlanId(), op.getReasonCode(), op.getMessage(), op.isRetryable(),
                op.getCreatedAt(), op.getStartedAt(), op.getFinishedAt(), terminal ? null : POLL_AFTER_SECONDS);
    }
}
