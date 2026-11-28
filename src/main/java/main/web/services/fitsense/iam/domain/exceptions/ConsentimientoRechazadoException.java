package main.web.services.fitsense.iam.domain.exceptions;

import main.web.services.fitsense.shared.domain.exceptions.DomainRuleViolationException;

/**
 * Registro o aceptacion rechazados por el contrato de consentimientos (C1, opcion A). Responde 422, como cualquier
 * {@link DomainRuleViolationException}, pero con un codigo estable en {@code details[0]} para que la app reaccione
 * sin interpretar el texto. El mensaje solo habla de documentos y versiones publicas, nunca de la cuenta.
 */
public class ConsentimientoRechazadoException extends DomainRuleViolationException {

    /** Falta una aceptacion obligatoria (condiciones, privacidad o consentimiento informado). */
    public static final String CONSENTIMIENTO_REQUERIDO = "CONSENTIMIENTO_REQUERIDO";
    /** Opcion A con el registro definitivo: sin aceptar participar no se crea la cuenta. */
    public static final String PARTICIPACION_REQUERIDA = "PARTICIPACION_REQUERIDA";
    /** Se envio una version de un tipo que todavia no tiene documento aprobado y publicado. */
    public static final String DOCUMENTO_NO_PUBLICADO = "DOCUMENTO_NO_PUBLICADO";
    /** La version enviada no es la vigente: inexistente, vencida o cambiada mientras la persona se registraba. */
    public static final String VERSION_NO_VIGENTE = "VERSION_NO_VIGENTE";
    /** Version del consentimiento informado sin aceptar participar. */
    public static final String VERSION_SIN_PARTICIPACION = "VERSION_SIN_PARTICIPACION";

    private final String codigo;

    public ConsentimientoRechazadoException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }
}
