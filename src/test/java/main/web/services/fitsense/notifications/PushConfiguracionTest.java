package main.web.services.fitsense.notifications;

import main.web.services.fitsense.notifications.application.AvisosDeCuenta;
import main.web.services.fitsense.notifications.application.DetectorEventosPush;
import main.web.services.fitsense.notifications.infrastructure.PushConfiguracion;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Clock;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Sin credenciales validas, el transporte queda no disponible y la aplicacion arranca igual. */
@DisplayName("Notificaciones push: arranque sin credenciales")
class PushConfiguracionTest {

    Object transporte(boolean activado, String base64, String archivo) throws Exception {
        Method m = PushConfiguracion.class.getDeclaredMethod("transportePush", boolean.class, String.class,
                String.class, String.class, String.class, long.class);
        m.setAccessible(true);
        return m.invoke(new PushConfiguracion(), activado, "kora-fitpe", "pe.fit.kora", base64, archivo, 24L);
    }

    @Test
    @DisplayName("apagado por omision: no disponible")
    void apagado() throws Exception {
        var t = (PushConfiguracion.TransporteNoDisponible) transporte(false, "", "");
        assertFalse(t.disponible());
        assertEquals("apagado", t.motivo());
    }

    @Test
    @DisplayName("encendido sin credenciales, con Base64 invalido o con archivo inexistente: no disponible, sin excepcion")
    void sinCredencialesValidas() throws Exception {
        if (System.getenv("GOOGLE_APPLICATION_CREDENTIALS") == null) {
            var t = (PushConfiguracion.TransporteNoDisponible) transporte(true, "", "");
            assertEquals("sin credenciales", t.motivo());
        }
        var basura = Base64.getEncoder().encodeToString("{\"type\":\"service_account\"}".getBytes());
        assertFalse(((PushConfiguracion.TransporteNoDisponible) transporte(true, basura, "")).disponible());
        assertFalse(((PushConfiguracion.TransporteNoDisponible) transporte(true, "%%%no-es-base64", "")).disponible());
        assertFalse(((PushConfiguracion.TransporteNoDisponible) transporte(true, "", "/no/existe/credencial.json"))
                .disponible());
    }

    @Test
    @DisplayName("encendido pero sin credenciales: ni el detector ni los avisos de cuenta escriben nada")
    @SuppressWarnings("unchecked")
    void sinTransporteNoSeRegistraNada() {
        var jdbc = mock(JdbcTemplate.class);
        ObjectProvider<Clock> reloj = mock(ObjectProvider.class);
        when(reloj.getIfAvailable(any())).thenReturn(Clock.systemUTC());
        var apagado = new PushConfiguracion.TransporteNoDisponible("sin credenciales");
        assertEquals(0, new DetectorEventosPush(jdbc, apagado, true, 6, reloj).detectar());
        new AvisosDeCuenta(jdbc, apagado, true, reloj).contrasenaCambiada(1L, 0, 1);
        verifyNoInteractions(jdbc);
    }
}
