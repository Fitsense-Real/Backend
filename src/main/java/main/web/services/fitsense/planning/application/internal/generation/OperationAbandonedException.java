package main.web.services.fitsense.planning.application.internal.generation;

/**
 * La operacion dejo de estar PROCESSING mientras se generaba (la cerro el
 * barrido por tiempo maximo): el resultado se descarta sin publicarlo.
 */
public class OperationAbandonedException extends RuntimeException {
    public OperationAbandonedException(String message) {
        super(message);
    }
}
