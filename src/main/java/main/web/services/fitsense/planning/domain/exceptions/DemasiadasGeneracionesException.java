package main.web.services.fitsense.planning.domain.exceptions;

/**
 * R-5: la persona alcanzo el limite de generaciones iniciales por hora ({@code fitsense.ai.generate.max-per-hour}).
 * Solo cuentan las peticiones que llegan a generar (las que podrian llamar a la IA). HTTP 429 con Retry-After.
 */
public class DemasiadasGeneracionesException extends RuntimeException {

    private final long reintentarEnSegundos;

    public DemasiadasGeneracionesException(long reintentarEnSegundos) {
        super("Has solicitado tu plan demasiadas veces en poco tiempo. Vuelve a intentarlo mas tarde.");
        this.reintentarEnSegundos = reintentarEnSegundos;
    }

    public long reintentarEnSegundos() {
        return reintentarEnSegundos;
    }
}
