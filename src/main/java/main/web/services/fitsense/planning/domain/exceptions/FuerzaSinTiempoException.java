package main.web.services.fitsense.planning.domain.exceptions;

import main.web.services.fitsense.planning.domain.services.CoberturaFuerzaTiempo;
import main.web.services.fitsense.shared.domain.exceptions.DomainRuleViolationException;

/**
 * Cierre PLAN-REV-2.0, fase 2 (alternativa C): INCREASE_STRENGTH sin tiempo para la prescripcion completa (los slots
 * de fuerza que exige V25 con la dosis de fuerza no caben en el limite de V4 de la politica activa). Se informa ANTES
 * de generar, con las alternativas de disponibilidad, sin bajar la dosis, sin quitar patrones y sin exceder el
 * tiempo declarado. HTTP 422, como cualquier regla de dominio.
 */
public class FuerzaSinTiempoException extends DomainRuleViolationException {

    public static final String CODIGO = "FUERZA_SIN_TIEMPO";

    public FuerzaSinTiempoException(Long userId, CoberturaFuerzaTiempo.Deficit d, int minutosPerfil) {
        super(mensaje(d, minutosPerfil));
    }

    public static String mensaje(CoberturaFuerzaTiempo.Deficit d, int minutosPerfil) {
        return ("%s: con %d %s de %d minutos no caben los %d patrones de fuerza de tu objetivo con la dosis de fuerza"
                + " (se necesitan unos %d minutos por sesion). No acortamos la dosis ni quitamos patrones para que"
                + " quepa. Puedes indicar %d minutos o mas por sesion, o mas dias por semana.")
                .formatted(CODIGO, d.sesiones(), d.sesiones() == 1 ? "sesion" : "sesiones", minutosPerfil,
                        d.slotsExigidos(), d.minutosNecesarios(), d.minutosNecesarios());
    }
}
