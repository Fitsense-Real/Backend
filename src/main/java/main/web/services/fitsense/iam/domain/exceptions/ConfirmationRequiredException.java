package main.web.services.fitsense.iam.domain.exceptions;

/** Una accion irreversible llego sin la confirmacion explicita. */
public class ConfirmationRequiredException extends RuntimeException {
    public ConfirmationRequiredException(String message) {
        super(message);
    }
}
