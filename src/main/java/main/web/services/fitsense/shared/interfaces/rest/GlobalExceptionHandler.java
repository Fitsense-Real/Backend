package main.web.services.fitsense.shared.interfaces.rest;

import main.web.services.fitsense.shared.domain.exceptions.DomainRuleViolationException;
import main.web.services.fitsense.shared.domain.exceptions.ResourceNotFoundException;
import main.web.services.fitsense.shared.interfaces.rest.resources.ApiErrorResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResource> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), List.of());
    }

    @ExceptionHandler(DomainRuleViolationException.class)
    public ResponseEntity<ApiErrorResource> handleDomainRule(DomainRuleViolationException ex) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), List.of());
    }

    /**
     * Una restriccion de la base rechazo la escritura: casi siempre dos
     * peticiones simultaneas (dos inicios de sesion, dos reportes del mismo
     * entrenamiento) donde la segunda llego tarde. Es un conflicto, no un error
     * del servidor, y la base ya dejo los datos intactos.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResource> handleConflict(DataIntegrityViolationException ex) {
        return build(HttpStatus.CONFLICT,
                "La operacion choco con otra simultanea y no se aplico. Consulta el estado actual.", List.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResource> handleIllegalArgument(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResource> handleAccessDenied(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, "No tienes acceso a este recurso.", List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResource> handleValidation(MethodArgumentNotValidException ex) {
        var details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> "%s: %s".formatted(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return build(HttpStatus.BAD_REQUEST, "La solicitud tiene campos invalidos.", details);
    }

    private ResponseEntity<ApiErrorResource> build(HttpStatus status, String message, List<String> details) {
        return ResponseEntity.status(status)
                .body(ApiErrorResource.of(status.value(), status.getReasonPhrase(), message, details));
    }
}
