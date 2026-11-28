package main.web.services.fitsense.planning.domain.exceptions;

import main.web.services.fitsense.shared.domain.exceptions.DomainRuleViolationException;

import java.time.LocalDate;

/**
 * P1-02 (cierre de preproduccion): el plan activo de la semana cambio entre la preparacion y la publicacion (otra
 * generacion, una reparacion de seguridad u otra adaptacion lo publico mientras la IA respondia). Publicar encima
 * reemplazaria en silencio una version que nadie vio: no se publica, el plan vigente se conserva y el historial queda
 * intacto. Es un conflicto (409), no un error del servidor.
 */
public class PlanCambioDuranteLaGeneracionException extends DomainRuleViolationException {

    private final Long esperado;
    private final Long encontrado;

    public PlanCambioDuranteLaGeneracionException(Long userId, LocalDate semana, Long esperado, Long encontrado) {
        super(("El plan activo del usuario %d para la semana del %s cambio durante la generacion (se preparo sobre %s"
                + " y ahora es %s): no se publica encima.").formatted(userId, semana,
                esperado == null ? "ninguno" : esperado, encontrado == null ? "ninguno" : encontrado));
        this.esperado = esperado;
        this.encontrado = encontrado;
    }

    public Long esperado() {
        return esperado;
    }

    public Long encontrado() {
        return encontrado;
    }
}
