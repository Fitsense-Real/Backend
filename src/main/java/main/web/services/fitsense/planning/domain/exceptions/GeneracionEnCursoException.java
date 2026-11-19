package main.web.services.fitsense.planning.domain.exceptions;

/**
 * R-5 (resiliencia de Replicate, 11/11/2026): ya hay una generacion en curso para esta persona -otra peticion o el
 * ciclo semanal- y esta NO se ejecuta, para no pagar dos veces la misma semana ni producir dos resultados
 * contradictorios. Es seguro reintentar despues. HTTP 409.
 */
public class GeneracionEnCursoException extends RuntimeException {
    public GeneracionEnCursoException(Long userId) {
        super("Ya se esta preparando tu plan. Espera un momento y vuelve a consultarlo.");
    }
}
