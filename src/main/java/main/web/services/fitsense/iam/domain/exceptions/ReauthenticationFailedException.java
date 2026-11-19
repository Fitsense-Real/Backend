package main.web.services.fitsense.iam.domain.exceptions;

/** La contrasena con la que se confirma una accion sensible no es la de la cuenta. */
public class ReauthenticationFailedException extends RuntimeException {
    public ReauthenticationFailedException() {
        super("La contrasena no es correcta. La cuenta no se ha eliminado.");
    }
}
