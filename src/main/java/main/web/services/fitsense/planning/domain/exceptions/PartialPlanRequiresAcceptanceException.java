package main.web.services.fitsense.planning.domain.exceptions;

import main.web.services.fitsense.planning.domain.model.valueobjects.GeneratedPlan;
import main.web.services.fitsense.shared.domain.exceptions.DomainRuleViolationException;

/**
 * 4A v6 · la semana generada es PARCIAL: cubre alguna capacidad principal y no
 * otras. Es una ALTERNATIVA que el participante tiene que aceptar, asi que NO
 * se activa: no se guarda ningun plan y se devuelve la propuesta con lo que
 * falta y por que.
 * <p>
 * No se persiste a proposito: la generacion es determinista (semilla de
 * usuario y semana), asi que aceptarla podra volver a producir exactamente la
 * misma propuesta sin un estado de plan nuevo ni una migracion.
 */
public class PartialPlanRequiresAcceptanceException extends DomainRuleViolationException {

    private final transient GeneratedPlan propuesta;

    public PartialPlanRequiresAcceptanceException(Long userId, GeneratedPlan propuesta) {
        super(("PARCIAL_REQUIERE_ACEPTACION: la semana del usuario %d es una alternativa parcial (%s). "
                + "No se activa sin la aceptacion del participante.").formatted(userId,
                propuesta.cobertura() == null ? "sin cobertura" : propuesta.cobertura().noCubiertas()));
        this.propuesta = propuesta;
    }

    public GeneratedPlan propuesta() {
        return propuesta;
    }
}
