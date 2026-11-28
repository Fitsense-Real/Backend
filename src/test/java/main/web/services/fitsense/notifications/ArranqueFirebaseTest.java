package main.web.services.fitsense.notifications;

import main.web.services.fitsense.notifications.domain.MensajePush;
import main.web.services.fitsense.notifications.domain.TipoEventoPush;
import main.web.services.fitsense.notifications.domain.TransportePush;
import main.web.services.fitsense.notifications.infrastructure.FirebaseTransportePush;
import main.web.services.fitsense.notifications.infrastructure.PushConfiguracion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * REGRESION DEL ARRANQUE CON FIREBASE ACTIVO. Con firebase-admin 9.11.0 y Firestore excluido,
 * {@code FirebaseOptions.build()} lanzaba {@code NoClassDefFoundError: com/google/api/client/json/jackson2/JacksonFactory}
 * (la clase solo llegaba con Firestore) y tumbaba Spring Boot. Esta prueba recorre el camino real del SDK con una
 * credencial sintetica y Google simulado: si vuelve a faltar una clase, el transporte no queda disponible y falla.
 */
@DisplayName("Notificaciones push: el SDK de Firebase arranca y envia por FID (Google simulado, credencial sintetica)")
class ArranqueFirebaseTest {

    static final String FID = "cR_9l0aBcD3fGh1jK2lM4n";

    @Test
    @DisplayName("credencial sintetica valida: transporte Firebase disponible, sin NoClassDefFoundError")
    void arrancaConCredencialSintetica() {
        var google = new GoogleSimulado();
        var t = assertDoesNotThrow(() -> PushConfiguracion.crear(true, "kora-fitpe", "pe.fit.kora",
                GoogleSimulado.credencialSinteticaBase64(), "", 24, google, "prueba-arranque-" + UUID.randomUUID()));
        assertInstanceOf(FirebaseTransportePush.class, t,
                "el SDK no se pudo cargar (" + t + "): revisar dependencias de firebase-admin");
        assertTrue(t.disponible());
        assertEquals(0, google.llamadas.size(), "arrancar no llama a Google");
    }

    @Test
    @DisplayName("envio real del SDK con Google simulado: token OAuth y FCM v1 con fid (nunca token), datos del contrato")
    void envioPorFidConElSdkReal() {
        var google = new GoogleSimulado();
        var t = PushConfiguracion.crear(true, "kora-fitpe", "pe.fit.kora", GoogleSimulado.credencialSinteticaBase64(),
                "", 24, google, "prueba-envio-" + UUID.randomUUID());
        var m = MensajePush.para(TipoEventoPush.PLAN_GENERATED, UUID.randomUUID(), null, "PLAN", 42L, Instant.now());

        var r = t.enviar(FID, m);
        assertEquals(TransportePush.Tipo.ENVIADO, r.tipo(), r.toString());
        assertEquals("projects/kora-fitpe/messages/0:1", r.messageId());

        assertTrue(google.llamadas.stream().allMatch(l -> l.url().startsWith(GoogleSimulado.URL_TOKEN)
                || l.url().startsWith(GoogleSimulado.URL_FCM)), "solo token y FCM, todo interceptado");
        var fcm = google.llamadas.stream().filter(l -> l.url().startsWith(GoogleSimulado.URL_FCM)).findFirst().orElseThrow();
        assertEquals("POST", fcm.metodo());
        assertTrue(fcm.cuerpo().contains("\"fid\":\"" + FID + "\""), fcm.cuerpo());
        assertFalse(fcm.cuerpo().contains("\"token\""), "nunca un token de registro: " + fcm.cuerpo());
        assertTrue(fcm.cuerpo().contains("\"restricted_package_name\":\"pe.fit.kora\""), fcm.cuerpo());
        assertTrue(fcm.cuerpo().contains("\"eventType\":\"PLAN_GENERATED\""), fcm.cuerpo());
        assertTrue(fcm.cuerpo().contains("\"channel_id\":\"kora_planes\""), fcm.cuerpo());
    }

    @Test
    @DisplayName("UNREGISTERED devuelto por FCM se clasifica con el SDK real como NO_REGISTRADO")
    void unregisteredConElSdkReal() {
        var google = new GoogleSimulado();
        google.errorFcm = "UNREGISTERED";
        var t = PushConfiguracion.crear(true, "kora-fitpe", "pe.fit.kora", GoogleSimulado.credencialSinteticaBase64(),
                "", 24, google, "prueba-unregistered-" + UUID.randomUUID());
        var r = t.enviar(FID, MensajePush.para(TipoEventoPush.WEEKLY_PLAN_READY, UUID.randomUUID(), null, "PLAN", 7L,
                Instant.now()));
        assertEquals(TransportePush.Tipo.NO_REGISTRADO, r.tipo());
        assertEquals("UNREGISTERED", r.codigoError());
    }
}
