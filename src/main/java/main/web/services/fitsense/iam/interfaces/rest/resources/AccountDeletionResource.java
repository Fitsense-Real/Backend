package main.web.services.fitsense.iam.interfaces.rest.resources;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.UUID;

/**
 * Estado de una eliminacion de cuenta. {@code DELETED}: ya no queda ninguna fila de la persona. {@code PENDING}: la
 * cuenta ya esta desactivada (no admite inicio de sesion ni tokens) y el borrado se completa automaticamente.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AccountDeletionResource(String status, UUID deletionId, Instant requestedAt, Instant completedAt,
                                      Boolean alreadyRequested) {
}
