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

    /** B5.1: cuenta inicios de sesion fallidos y limites superados (solo el numero). */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private main.web.services.fitsense.shared.infrastructure.observability.ContadoresOperativos contadores;

    private void contar(String suceso) {
        if (contadores != null) contadores.sumar(suceso);
    }


    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResource> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), List.of());
    }

    /** R-5: ya hay una generacion en curso para esta persona; no se ejecuto otra. */
    @ExceptionHandler(main.web.services.fitsense.planning.domain.exceptions.GeneracionEnCursoException.class)
    public ResponseEntity<ApiErrorResource> handleGeneracionEnCurso(RuntimeException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), List.of());
    }

    /** R-5: sin el bloqueo de exclusion no se genera; transitorio. */
    @ExceptionHandler(main.web.services.fitsense.planning.domain.exceptions.GeneracionSinExclusionException.class)
    public ResponseEntity<ApiErrorResource> handleGeneracionSinExclusion(RuntimeException ex) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), List.of());
    }

    /** R-5: limite de generaciones por hora alcanzado. */
    @ExceptionHandler(main.web.services.fitsense.planning.domain.exceptions.DemasiadasGeneracionesException.class)
    public ResponseEntity<ApiErrorResource> handleDemasiadasGeneraciones(
            main.web.services.fitsense.planning.domain.exceptions.DemasiadasGeneracionesException ex) {
        var status = HttpStatus.TOO_MANY_REQUESTS;
        return ResponseEntity.status(status)
                .header(org.springframework.http.HttpHeaders.RETRY_AFTER, String.valueOf(ex.reintentarEnSegundos()))
                .body(ApiErrorResource.of(status.value(), status.getReasonPhrase(), ex.getMessage(), List.of()));
    }

    /** Eliminacion de cuenta: falta confirm=true. */
    @ExceptionHandler(main.web.services.fitsense.iam.domain.exceptions.ConfirmationRequiredException.class)
    public ResponseEntity<ApiErrorResource> handleConfirmationRequired(RuntimeException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), List.of("CONFIRMACION_REQUERIDA"));
    }

    /** Eliminacion de cuenta: la contrasena de reautenticacion no es la de la cuenta. */
    @ExceptionHandler(main.web.services.fitsense.iam.domain.exceptions.ReauthenticationFailedException.class)
    public ResponseEntity<ApiErrorResource> handleReauthenticationFailed(RuntimeException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), List.of("REAUTENTICACION_FALLIDA"));
    }

    @ExceptionHandler(main.web.services.fitsense.iam.domain.exceptions.TooManyReauthenticationAttemptsException.class)
    public ResponseEntity<ApiErrorResource> handleTooManyReauthentication(
            main.web.services.fitsense.iam.domain.exceptions.TooManyReauthenticationAttemptsException ex) {
        var status = HttpStatus.TOO_MANY_REQUESTS;
        return ResponseEntity.status(status)
                .header(org.springframework.http.HttpHeaders.RETRY_AFTER, String.valueOf(ex.retryAfterSeconds()))
                .body(ApiErrorResource.of(status.value(), status.getReasonPhrase(), ex.getMessage(),
                        List.of("DEMASIADOS_INTENTOS")));
    }

    /** Solo en una carrera (dos solicitudes a la vez): la cuenta ya no existe. Lo normal es un 401 del filtro. */
    @ExceptionHandler(main.web.services.fitsense.iam.application.internal.deletion.EliminacionDeCuentaService.CuentaInexistenteException.class)
    public ResponseEntity<ApiErrorResource> handleCuentaInexistente(RuntimeException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), List.of("CUENTA_INEXISTENTE"));
    }

    /** La fase de registro de la eliminacion fallo: no se cambio nada. */
    @ExceptionHandler(main.web.services.fitsense.iam.application.internal.deletion.EliminacionDeCuentaService.EliminacionNoRegistradaException.class)
    public ResponseEntity<ApiErrorResource> handleEliminacionNoRegistrada(RuntimeException ex) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), List.of("ELIMINACION_NO_REGISTRADA"));
    }

    /** Endpoints publicos: limite por IP superado (no depende de si el correo existe). */
    @ExceptionHandler(main.web.services.fitsense.shared.infrastructure.ratelimit.LimitePorIp.DemasiadasSolicitudesException.class)
    public ResponseEntity<ApiErrorResource> handleDemasiadasSolicitudes(
            main.web.services.fitsense.shared.infrastructure.ratelimit.LimitePorIp.DemasiadasSolicitudesException ex) {
        contar(main.web.services.fitsense.shared.infrastructure.observability.ContadoresOperativos.LIMITE_SUPERADO);
        var status = HttpStatus.TOO_MANY_REQUESTS;
        return ResponseEntity.status(status)
                .header(org.springframework.http.HttpHeaders.RETRY_AFTER, String.valueOf(ex.reintentarEnSegundos()))
                .body(ApiErrorResource.of(status.value(), status.getReasonPhrase(), ex.getMessage(),
                        List.of("DEMASIADAS_SOLICITUDES")));
    }

    /** Registro push invalido (identificador que no es FID, plataforma o app no admitidas): codigo en details[0]. */
    @ExceptionHandler(main.web.services.fitsense.notifications.application.DispositivosPushService.SolicitudPushInvalidaException.class)
    public ResponseEntity<ApiErrorResource> handleSolicitudPushInvalida(
            main.web.services.fitsense.notifications.application.DispositivosPushService.SolicitudPushInvalidaException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), List.of(ex.codigo()));
    }

    /** P1-02: el plan activo cambio durante la generacion; no se publico encima. */
    @ExceptionHandler(main.web.services.fitsense.planning.domain.exceptions.PlanCambioDuranteLaGeneracionException.class)
    public ResponseEntity<ApiErrorResource> handlePlanCambiado(
            main.web.services.fitsense.planning.domain.exceptions.PlanCambioDuranteLaGeneracionException ex) {
        return build(HttpStatus.CONFLICT, "Tu plan cambio mientras se preparaba uno nuevo. Consulta el plan actual.",
                List.of("PLAN_CAMBIADO_DURANTE_LA_GENERACION"));
    }

    /** C1 · contrato de consentimientos: 422 con el codigo estable en details[0] (mismo mensaje que antes). */
    @ExceptionHandler(main.web.services.fitsense.iam.domain.exceptions.ConsentimientoRechazadoException.class)
    public ResponseEntity<ApiErrorResource> handleConsentimientoRechazado(
            main.web.services.fitsense.iam.domain.exceptions.ConsentimientoRechazadoException ex) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), List.of(ex.codigo()));
    }

    /** C1 · opcion A: la cuenta no es un participante activo y elegible; no se generan ni publican planes. */
    @ExceptionHandler(main.web.services.fitsense.planning.domain.exceptions.ParticipacionNoVigenteException.class)
    public ResponseEntity<ApiErrorResource> handleParticipacionNoVigente(
            main.web.services.fitsense.planning.domain.exceptions.ParticipacionNoVigenteException ex) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(),
                List.of(main.web.services.fitsense.planning.domain.exceptions.ParticipacionNoVigenteException.CODIGO));
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
        contar(main.web.services.fitsense.shared.infrastructure.observability.ContadoresOperativos.LOGIN_FALLIDO);
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
