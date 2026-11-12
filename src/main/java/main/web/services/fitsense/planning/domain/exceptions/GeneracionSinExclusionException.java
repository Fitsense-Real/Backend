package main.web.services.fitsense.planning.domain.exceptions;

/**
 * R-5: no se pudo tomar el bloqueo que impide dos generaciones a la vez (la base no respondio). Sin exclusion no se
 * genera: es justo lo que se quiere evitar. Es transitorio y seguro reintentar. HTTP 503.
 */
public class GeneracionSinExclusionException extends RuntimeException {
    public GeneracionSinExclusionException(Long userId) {
        super("No fue posible preparar tu plan en este momento. Intentalo de nuevo en unos minutos.");
    }
}
