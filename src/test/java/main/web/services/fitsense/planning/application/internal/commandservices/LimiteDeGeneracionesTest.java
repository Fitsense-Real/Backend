package main.web.services.fitsense.planning.application.internal.commandservices;

import main.web.services.fitsense.planning.domain.exceptions.DemasiadasGeneracionesException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("R-5 · limite de generaciones iniciales por persona y hora (ventana deslizante)")
class LimiteDeGeneracionesTest {

    private static final class Reloj extends Clock {
        Instant ahora = Instant.parse("2026-11-11T12:00:00Z");
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return ahora; }
    }

    @Test
    @DisplayName("diez peticiones seguidas: pasan 5 y las otras 5 reciben 429 con Retry-After")
    void diezSeguidas() {
        var reloj = new Reloj();
        var limite = new LimiteDeGeneraciones(5, reloj);
        int pasan = 0, rechazadas = 0;
        for (int i = 0; i < 10; i++) {
            try {
                limite.consumir(1L);
                pasan++;
            } catch (DemasiadasGeneracionesException e) {
                rechazadas++;
                assertTrue(e.reintentarEnSegundos() > 0 && e.reintentarEnSegundos() <= 3600);
            }
        }
        assertEquals(5, pasan);
        assertEquals(5, rechazadas);
    }

    @Test
    @DisplayName("una rechazada no cuenta: al pasar la hora de la primera vuelve a haber hueco")
    void laVentanaSeDesliza() {
        var reloj = new Reloj();
        var limite = new LimiteDeGeneraciones(2, reloj);
        limite.consumir(1L);
        reloj.ahora = reloj.ahora.plusSeconds(600);
        limite.consumir(1L);
        assertThrows(DemasiadasGeneracionesException.class, () -> limite.consumir(1L));
        reloj.ahora = reloj.ahora.plusSeconds(3000);   // 3600 desde la primera
        assertDoesNotThrow(() -> limite.consumir(1L));
        assertThrows(DemasiadasGeneracionesException.class, () -> limite.consumir(1L));
    }

    @Test
    @DisplayName("cada persona tiene su propio limite: una no bloquea a otra")
    void porPersona() {
        var limite = new LimiteDeGeneraciones(1, new Reloj());
        limite.consumir(1L);
        assertDoesNotThrow(() -> limite.consumir(2L));
        assertThrows(DemasiadasGeneracionesException.class, () -> limite.consumir(1L));
    }

    @Test
    @DisplayName("configuracion fuera de rango: el arranque falla con un mensaje claro")
    void configuracionValidada() {
        assertThrows(IllegalArgumentException.class, () -> new LimiteDeGeneraciones(0, new Reloj()));
        assertThrows(IllegalArgumentException.class, () -> new LimiteDeGeneraciones(101, new Reloj()));
        assertEquals(5, new LimiteDeGeneraciones(5, new Reloj()).maximoPorHora());
    }
}
