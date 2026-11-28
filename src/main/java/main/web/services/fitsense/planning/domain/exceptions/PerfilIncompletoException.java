package main.web.services.fitsense.planning.domain.exceptions;

import main.web.services.fitsense.shared.domain.exceptions.DomainRuleViolationException;

/**
 * C1 · sin perfil no hay plan. 422 con {@code details[0]="PERFIL_INCOMPLETO"} (el mismo codigo que ya lleva una operacion
 * asincrona fallida por perfil, {@code reasonCode=PERFIL_INCOMPLETO}), para que la app lo distinga sin leer el texto.
 */
public class PerfilIncompletoException extends DomainRuleViolationException {

    public static final String CODIGO = "PERFIL_INCOMPLETO";

    public PerfilIncompletoException(String mensaje) {
        super(mensaje);
    }
}
