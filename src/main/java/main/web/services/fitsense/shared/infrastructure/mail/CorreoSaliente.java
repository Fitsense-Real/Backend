package main.web.services.fitsense.shared.infrastructure.mail;

/**
 * Envio de correo transaccional, independiente del proveedor ({@code fitsense.mail.transport}). Lo usan la
 * recuperacion de contrasena y la solicitud web de eliminacion de cuenta: un unico servicio de correo.
 * <p>
 * Las implementaciones nunca registran el destinatario ni el cuerpo (puede llevar un codigo de un solo uso).
 */
public interface CorreoSaliente {

    /** Envia el mensaje en el hilo que llama. Lanza {@link CorreoNoEnviadoException} si el proveedor lo rechaza. */
    void enviar(Mensaje mensaje);

    /** Nombre del transporte para logs y diagnostico: none, smtp, resend, brevo. */
    String transporte();

    /** Falso para {@code none}: el flujo que depende del correo no es operativo. */
    boolean entregaReal();

    /**
     * @param tipo etiqueta sin datos personales para los logs (p. ej. RECUPERACION_CONTRASENA)
     */
    record Mensaje(String tipo, String para, String asunto, String texto) {
        @Override
        public String toString() {
            return "Mensaje[tipo=" + tipo + ", para=***, asunto=" + asunto + ", texto=***]";
        }
    }

    class CorreoNoEnviadoException extends RuntimeException {
        public CorreoNoEnviadoException(String causa) {
            super(causa);
        }
    }
}
