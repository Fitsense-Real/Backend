package main.web.services.fitsense.planning.domain.exceptions;

import main.web.services.fitsense.shared.domain.exceptions.DomainRuleViolationException;

/**
 * Opcion A: la cuenta no es un participante activo y elegible del estudio (no participa, se retiro, la cuenta no esta
 * activa o, con el registro definitivo, no tiene un consentimiento informado valido). No se le genera ni se le publica
 * ningun plan. 422 con {@code details[0]="PARTICIPACION_NO_VIGENTE"}; el mensaje no lleva identificadores internos.
 */
public class ParticipacionNoVigenteException extends DomainRuleViolationException {

    public static final String CODIGO = "PARTICIPACION_NO_VIGENTE";
    /** Para la persona: sin identificadores internos ni el motivo concreto (retirada, cuenta inactiva...). */
    public static final String TEXTO = "Tu cuenta no tiene una participacion activa en el estudio: Kora no puede"
            + " preparar rutinas nuevas. Si crees que es un error, contacta al equipo del estudio.";

    public ParticipacionNoVigenteException(String mensaje) {
        super(mensaje);
    }
}
