package main.web.services.fitsense.planning.domain.exceptions;

import main.web.services.fitsense.shared.domain.exceptions.DomainRuleViolationException;

/**
 * Opcion A: la cuenta no es un participante activo y elegible del estudio (no participa, se retiro, la cuenta no esta
 * activa o, con el registro definitivo, no tiene un consentimiento informado valido). No se le genera ni se le publica
 * ningun plan. 422 con {@code details[0]="PARTICIPACION_NO_VIGENTE"}; el mensaje no cambia respecto al anterior.
 */
public class ParticipacionNoVigenteException extends DomainRuleViolationException {

    public static final String CODIGO = "PARTICIPACION_NO_VIGENTE";

    public ParticipacionNoVigenteException(String mensaje) {
        super(mensaje);
    }
}
