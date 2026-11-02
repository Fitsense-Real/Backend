package main.web.services.fitsense.planning.interfaces.rest;

import main.web.services.fitsense.planning.domain.exceptions.PlanGenerationFailedException;
import main.web.services.fitsense.planning.domain.services.DiagnosticoNoFactible;
import main.web.services.fitsense.shared.interfaces.rest.resources.ApiErrorResource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Respuesta ESTRUCTURADA de una semana NO_FACTIBLE por falta de un patron principal (NOFACT-0.1). Solo existe con
 * {@code fitsense.api.no-factible-estructurado.enabled=true}: apagado, este bean no se crea y la respuesta es la de
 * siempre ({@code GlobalExceptionHandler}, 422 con el mensaje). Encendido, si el fallo no es de ese tipo devuelve la
 * misma respuesta que el manejador global.
 */
@RestControllerAdvice(assignableTypes = TrainingPlanController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name = "fitsense.api.no-factible-estructurado.enabled", havingValue = "true")
public class NoFactibleExceptionHandler {

    public record NoFactibleResource(OffsetDateTime timestamp, int status, String error,
                                     DiagnosticoNoFactible.Diagnostico diagnosis) {}

    @ExceptionHandler(PlanGenerationFailedException.class)
    public ResponseEntity<?> handle(PlanGenerationFailedException ex) {
        var d = DiagnosticoNoFactible.de(ex.problems());
        var status = HttpStatus.UNPROCESSABLE_ENTITY;
        if (d.isEmpty())
            return ResponseEntity.status(status).body(ApiErrorResource.of(status.value(), status.getReasonPhrase(),
                    ex.getMessage(), List.of()));
        return ResponseEntity.status(status).body(new NoFactibleResource(OffsetDateTime.now(), status.value(),
                status.getReasonPhrase(), d.get()));
    }
}
