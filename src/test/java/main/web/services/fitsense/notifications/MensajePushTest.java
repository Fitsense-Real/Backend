package main.web.services.fitsense.notifications;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import main.web.services.fitsense.notifications.domain.MensajePush;
import main.web.services.fitsense.notifications.domain.TipoEventoPush;
import main.web.services.fitsense.notifications.domain.TransportePush;
import main.web.services.fitsense.notifications.infrastructure.FirebaseTransportePush;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Contrato del mensaje y del transporte FCM, sin red. */
@DisplayName("Notificaciones push: contrato del mensaje y del transporte FCM")
class MensajePushTest {

    static final UUID ID = UUID.fromString("0b6f3c2e-1d4a-4e7b-9c1f-5a8d2e3b4c6d");
    static final Instant CREADO = Instant.parse("2026-11-22T14:05:00Z");

    @Test
    @DisplayName("datos minimos del contrato para un evento de plan")
    void datosDePlan() {
        var m = MensajePush.para(TipoEventoPush.PLAN_ADAPTED, ID, null, "PLAN", 42L, CREADO);
        assertEquals(Map.of("schemaVersion", "1", "eventType", "PLAN_ADAPTED", "eventId", ID.toString(),
                "resourceType", "PLAN", "resourceId", "42", "targetScreen", "WEEKLY_PLAN",
                "createdAt", "2026-11-22T14:05:00Z"), m.datos());
        assertEquals(MensajePush.CANAL_PLANES, m.canal());
        assertEquals("plan_semana", m.agrupacion(), "una notificacion de plan reemplaza a la anterior en el telefono");
    }

    @Test
    @DisplayName("aviso de cuenta: sin recurso, con noticeCode y pantalla de seguridad")
    void avisoDeCuenta() {
        var m = MensajePush.para(TipoEventoPush.ACCOUNT_NOTICE, ID, "PASSWORD_CHANGED", null, null, CREADO);
        assertEquals("PASSWORD_CHANGED", m.datos().get("noticeCode"));
        assertEquals("ACCOUNT_SECURITY", m.datos().get("targetScreen"));
        assertFalse(m.datos().containsKey("resourceId"));
        assertEquals(MensajePush.CANAL_CUENTA, m.canal());
    }

    @Test
    @DisplayName("ningun texto lleva datos de salud ni de entrenamiento")
    void sinDatosSensibles() {
        var prohibidas = List.of("dolor", "lesion", "peso", "kg", "adherencia", "fatiga", "salud", "enfermedad",
                "molestia", "sentadilla", "%");
        for (var t : TipoEventoPush.values()) {
            var m = MensajePush.para(t, ID, t == TipoEventoPush.ACCOUNT_NOTICE ? "PASSWORD_CHANGED" : null,
                    t == TipoEventoPush.ACCOUNT_NOTICE ? null : "PLAN", t == TipoEventoPush.ACCOUNT_NOTICE ? null : 7L,
                    CREADO);
            var texto = (m.titulo() + " " + m.cuerpo() + " " + m.datos().values()).toLowerCase(Locale.ROOT);
            for (var p : prohibidas) assertFalse(texto.contains(p), t + " contiene «" + p + "»");
        }
    }

    @Test
    @DisplayName("el mensaje FCM va dirigido por FID (no por token), restringido al paquete y con TTL")
    void mensajeFcm() throws Exception {
        var m = MensajePush.para(TipoEventoPush.WEEKLY_PLAN_READY, ID, null, "PLAN", 9L, CREADO);
        Message fcm = FirebaseTransportePush.construir("cR_9l0aBcD3fGh1jK2lM4n", m, "pe.fit.kora", Duration.ofHours(24));
        assertEquals("cR_9l0aBcD3fGh1jK2lM4n", campo(fcm, "fid"));
        assertNull(campo(fcm, "token"), "nunca un token de registro");
        AndroidConfig android = (AndroidConfig) campo(fcm, "androidConfig");
        assertEquals("pe.fit.kora", campo(android, "restrictedPackageName"));
        assertEquals("86400s", campo(android, "ttl"));
        @SuppressWarnings("unchecked")
        var datos = (Map<String, String>) campo(fcm, "data");
        assertEquals("WEEKLY_PLAN_READY", datos.get("eventType"));
    }

    @Test
    @DisplayName("clasificacion de errores de FCM: UNREGISTERED desvincula, INVALID_ARGUMENT no reintenta, el resto reintenta")
    void clasificacion() {
        assertEquals(TransportePush.Tipo.NO_REGISTRADO, FirebaseTransportePush.clasificar(MessagingErrorCode.UNREGISTERED));
        assertEquals(TransportePush.Tipo.NO_REGISTRADO,
                FirebaseTransportePush.clasificar(MessagingErrorCode.SENDER_ID_MISMATCH));
        assertEquals(TransportePush.Tipo.ERROR_PERMANENTE,
                FirebaseTransportePush.clasificar(MessagingErrorCode.INVALID_ARGUMENT));
        for (var c : List.of(MessagingErrorCode.UNAVAILABLE, MessagingErrorCode.INTERNAL,
                MessagingErrorCode.QUOTA_EXCEEDED, MessagingErrorCode.THIRD_PARTY_AUTH_ERROR))
            assertEquals(TransportePush.Tipo.ERROR_TEMPORAL, FirebaseTransportePush.clasificar(c), c.name());
        assertEquals(TransportePush.Tipo.ERROR_TEMPORAL, FirebaseTransportePush.clasificar(null));
    }

    @Test
    @DisplayName("los registros solo muestran 4 caracteres del FID")
    void ofuscado() {
        assertEquals("cR_9…", FirebaseTransportePush.ofuscar("cR_9l0aBcD3fGh1jK2lM4n"));
        assertEquals("****", FirebaseTransportePush.ofuscar("corto"));
    }

    static Object campo(Object o, String nombre) throws Exception {
        Field f = o.getClass().getDeclaredField(nombre);
        f.setAccessible(true);
        return f.get(o);
    }
}
