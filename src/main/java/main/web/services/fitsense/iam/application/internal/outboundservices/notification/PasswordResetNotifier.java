package main.web.services.fitsense.iam.application.internal.outboundservices.notification;

import java.time.OffsetDateTime;

/**
 * Entrega del codigo de recuperacion al usuario. NO hay proveedor de correo autorizado (cierre definitivo, 2.2): la
 * implementacion por defecto no envia nada. Un proveedor real se anade como otra implementacion, con sus
 * credenciales en variables de entorno, nunca en el repositorio.
 */
public interface PasswordResetNotifier {

    void enviar(String email, String token, OffsetDateTime caduca);

    /** Si este canal entrega de verdad. Con false, el flujo NO esta operativo para usuarios reales. */
    boolean entregaReal();
}
