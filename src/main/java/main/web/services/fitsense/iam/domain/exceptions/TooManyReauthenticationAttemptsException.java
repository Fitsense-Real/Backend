package main.web.services.fitsense.iam.domain.exceptions;

/** Demasiadas contrasenas incorrectas seguidas al confirmar una accion sensible. */
public class TooManyReauthenticationAttemptsException extends RuntimeException {
    private final long retryAfterSeconds;

    public TooManyReauthenticationAttemptsException(long retryAfterSeconds) {
        super("Demasiados intentos con una contrasena incorrecta. Espera antes de volver a intentarlo.");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
