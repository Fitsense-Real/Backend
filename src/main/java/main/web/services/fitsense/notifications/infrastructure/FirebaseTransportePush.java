package main.web.services.fitsense.notifications.infrastructure;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import main.web.services.fitsense.notifications.domain.MensajePush;
import main.web.services.fitsense.notifications.domain.TransportePush;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Envio por Firebase Admin SDK, dirigido por <b>Firebase Installation ID</b> ({@code Message.Builder.setFid}, desde
 * firebase-admin 9.10.0). No se usan tokens de registro FCM: el contrato admite un solo tipo de identificador.
 * <p>
 * El mensaje se restringe al paquete Android del contrato ({@code setRestrictedPackageName}) y lleva TTL: un aviso de
 * plan que no se entrega en ese tiempo ya no tiene sentido.
 * <p>
 * Los registros nunca incluyen el FID completo ni el contenido del mensaje.
 */
public class FirebaseTransportePush implements TransportePush {

    private static final Logger log = LoggerFactory.getLogger(FirebaseTransportePush.class);

    private final FirebaseMessaging messaging;
    private final String paqueteAndroid;
    private final Duration ttl;

    public FirebaseTransportePush(FirebaseMessaging messaging, String paqueteAndroid, Duration ttl) {
        this.messaging = messaging;
        this.paqueteAndroid = paqueteAndroid;
        this.ttl = ttl;
    }

    @Override
    public boolean disponible() {
        return true;
    }

    @Override
    public Resultado enviar(String installationId, MensajePush mensaje) {
        try {
            return Resultado.enviado(messaging.send(construir(installationId, mensaje, paqueteAndroid, ttl)));
        } catch (FirebaseMessagingException e) {
            var codigo = e.getMessagingErrorCode();
            var tipo = clasificar(codigo);
            log.warn("FCM rechazo un envio a {}: {} ({})", ofuscar(installationId),
                    codigo == null ? e.getErrorCode() : codigo, tipo);
            return Resultado.fallo(tipo, codigo == null ? String.valueOf(e.getErrorCode()) : codigo.name());
        } catch (RuntimeException e) {
            log.warn("Fallo transitorio al enviar a {}: {}", ofuscar(installationId), e.getClass().getSimpleName());
            return Resultado.fallo(Tipo.ERROR_TEMPORAL, e.getClass().getSimpleName());
        }
    }

    /** El mensaje de FCM. Publico y estatico para probarlo sin red. */
    public static Message construir(String installationId, MensajePush m, String paqueteAndroid, Duration ttl) {
        return Message.builder()
                .setFid(installationId)
                .putAllData(m.datos())
                .setNotification(Notification.builder().setTitle(m.titulo()).setBody(m.cuerpo()).build())
                .setAndroidConfig(AndroidConfig.builder()
                        .setRestrictedPackageName(paqueteAndroid)
                        .setPriority(AndroidConfig.Priority.NORMAL)
                        .setTtl(ttl.toMillis())
                        .setCollapseKey(m.agrupacion())
                        .setNotification(AndroidNotification.builder()
                                .setChannelId(m.canal())
                                .setTag(m.agrupacion())
                                .build())
                        .build())
                .build();
    }

    /**
     * Que hacer con cada codigo de error de FCM HTTP v1.
     * <ul>
     *   <li>UNREGISTERED: la instalacion ya no esta registrada (app desinstalada, {@code unregister()}, datos
     *       borrados). SENDER_ID_MISMATCH: el FID pertenece a otro proyecto. En los dos casos el dispositivo se
     *       desvincula y no se vuelve a intentar.</li>
     *   <li>INVALID_ARGUMENT: el mensaje o el identificador son invalidos; se marca el envio como fallido sin
     *       desvincular (podria ser un defecto del mensaje y no del dispositivo).</li>
     *   <li>El resto (UNAVAILABLE, INTERNAL, QUOTA_EXCEEDED, THIRD_PARTY_AUTH_ERROR, desconocido): transitorio.</li>
     * </ul>
     */
    public static Tipo clasificar(MessagingErrorCode codigo) {
        if (codigo == null) return Tipo.ERROR_TEMPORAL;
        return switch (codigo) {
            case UNREGISTERED, SENDER_ID_MISMATCH -> Tipo.NO_REGISTRADO;
            case INVALID_ARGUMENT -> Tipo.ERROR_PERMANENTE;
            default -> Tipo.ERROR_TEMPORAL;
        };
    }

    /** Solo los 4 primeros caracteres: suficiente para correlacionar, insuficiente para reutilizar. */
    public static String ofuscar(String installationId) {
        if (installationId == null || installationId.length() < 8) return "****";
        return installationId.substring(0, 4) + "…";
    }
}
