package main.web.services.fitsense.shared.interfaces.rest;

import main.web.services.fitsense.shared.domain.exceptions.DomainRuleViolationException;
import main.web.services.fitsense.shared.domain.exceptions.ResourceNotFoundException;
import main.web.services.fitsense.shared.interfaces.rest.resources.ApiErrorResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import main.web.services.fitsense.iam.domain.exceptions.InvalidCredentialsException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
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

    /**
     * Login con correo desconocido, contrasena incorrecta o cuenta no activa: 401 con el mismo mensaje en los tres
     * casos (no revela si el correo existe). Antes no tenia manejador y terminaba en 500 (cierre definitivo, fase 3.2).
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiErrorResource> handleInvalidCredentials(InvalidCredentialsException ex) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), List.of());
    }

    /** Un identificador o parametro con formato incorrecto (p. ej. /sessions/abc): 400 con el cuerpo de siempre. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResource> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return build(HttpStatus.BAD_REQUEST, "El parametro %s no tiene un formato valido.".formatted(ex.getName()),
                List.of());
    }

    /** JSON mal formado o con un valor de tipo incorrecto: 400, sin repetir el contenido recibido. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResource> handleUnreadable(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud no es valido.", List.of());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResource> handleMissingParameter(MissingServletRequestParameterException ex) {
        return build(HttpStatus.BAD_REQUEST, "Falta el parametro %s.".formatted(ex.getParameterName()), List.of());
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
