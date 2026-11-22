package main.web.services.fitsense.notifications.domain;

/**
 * Envio de UN mensaje a UN dispositivo identificado por su Firebase Installation ID. La implementacion de produccion
 * usa Firebase Admin SDK; las pruebas usan un transporte simulado. Nunca lanza: todo fallo vuelve como resultado.
 */
public interface TransportePush {

    /** Falso sin credenciales o con el envio apagado: el emisor no reclama eventos y estos esperan (o caducan). */
    boolean disponible();

    Resultado enviar(String installationId, MensajePush mensaje);

    enum Tipo {
        /** FCM acepto el mensaje. */
        ENVIADO,
        /** El FID ya no es valido para esta app (UNREGISTERED, SENDER_ID_MISMATCH): se desvincula el dispositivo. */
        NO_REGISTRADO,
        /** Rechazo definitivo de ESTE mensaje (p. ej. INVALID_ARGUMENT): no se reintenta, el dispositivo sigue. */
        ERROR_PERMANENTE,
        /** Fallo transitorio (red, UNAVAILABLE, INTERNAL, cuota): se reintenta con espera creciente. */
        ERROR_TEMPORAL
    }

    record Resultado(Tipo tipo, String messageId, String codigoError) {
        public static Resultado enviado(String messageId) {
            return new Resultado(Tipo.ENVIADO, messageId, null);
        }

        public static Resultado fallo(Tipo tipo, String codigo) {
            if (tipo == Tipo.ENVIADO) throw new IllegalArgumentException("Un fallo no puede ser ENVIADO");
            return new Resultado(tipo, null, codigo == null ? "DESCONOCIDO" : codigo);
        }
    }
}
