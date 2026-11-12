package main.web.services.fitsense.orchestation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static main.web.services.fitsense.orchestation.ControlPresupuestoK20.EstadoProveedor.*;
import static main.web.services.fitsense.orchestation.ControlPresupuestoK20.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("P-H1: control de presupuesto del arnes K20 (sin red, sin Replicate)")
class ControlPresupuestoK20Test {

    static final double R = 0.05, TE = 0.25, TS = 2.00;

    @Test
    @DisplayName("failed sin metricas: protocolo congelado bloquea; P-H1 carga la reserva y sigue")
    void failed() {
        var c = contabilizar(BLOQUEAR, true, FAILED, null, null, R, TE, TS, "p1");
        assertEquals(R, c.usd(), 1e-12);
        assertNotNull(c.bloqueo());
        var h = contabilizar(RESERVA_SIN_BLOQUEO, true, FAILED, null, null, R, TE, TS, "p1");
        assertEquals(R, h.usd(), 1e-12, "la reserva se contabiliza igual");
        assertNull(h.bloqueo());
    }

    @Test
    @DisplayName("canceled sin metricas (caso J08): P-H1 sigue; con BLOQUEAR, bloquea")
    void canceled() {
        assertNull(contabilizar(RESERVA_SIN_BLOQUEO, true, CANCELED, null, null, R, TE, TS, "p2").bloqueo());
        assertNotNull(contabilizar(BLOQUEAR, true, CANCELED, null, null, R, TE, TS, "p2").bloqueo());
    }

    @Test
    @DisplayName("timeout con la prediccion aun en curso o en estado desconocido: bloquea siempre (coste no acotado)")
    void timeout() {
        assertNotNull(contabilizar(RESERVA_SIN_BLOQUEO, true, EN_CURSO, null, null, R, TE, TS, "p3").bloqueo());
        assertNotNull(contabilizar(RESERVA_SIN_BLOQUEO, true, DESCONOCIDO, null, null, R, TE, TS, "p3").bloqueo());
        // Caso U13 (autorizacion REV2 del 05/11/2026): el backend agoto su espera y el arnes pidio la cancelacion. Aunque
        // el proveedor la de por cancelada, un timeout NO es un fallo confirmado: bloquea y carga la reserva.
        var u13 = contabilizar(RESERVA_SIN_BLOQUEO, true, CANCELED, null, null, R, TE, TS, "p3", true);
        assertNotNull(u13.bloqueo());
        assertEquals(R, u13.usd(), 1e-12);
        assertNotNull(contabilizar(RESERVA_SIN_BLOQUEO, true, FAILED, null, null, R, TE, TS, "p3", true).bloqueo());
        // Cancelada por el proveedor, sin que el arnes lo pidiera (fallo confirmado sin respuesta): sigue.
        assertNull(contabilizar(RESERVA_SIN_BLOQUEO, true, CANCELED, null, null, R, TE, TS, "p3", false).bloqueo());
    }

    @Test
    @DisplayName("respuesta recibida pero sin metricas: bloquea aun con P-H1 (su coste no lo acota la reserva)")
    void respuestaSinMetricas() {
        assertNotNull(contabilizar(RESERVA_SIN_BLOQUEO, false, SUCCEEDED, null, null, R, TE, TS, "p4").bloqueo());
        assertNotNull(contabilizar(RESERVA_SIN_BLOQUEO, false, SUCCEEDED, 1000L, null, R, TE, TS, "p4").bloqueo(),
                "metricas incompletas = sin metricas");
    }

    @Test
    @DisplayName("con metricas: coste por tokens; por encima de la reserva bloquea")
    void conMetricas() {
        var c = contabilizar(RESERVA_SIN_BLOQUEO, false, SUCCEEDED, 68_186L, 3_672L, R, TE, TS, "p5");
        assertEquals(68_186 * 0.25e-6 + 3_672 * 2e-6, c.usd(), 1e-12);
        assertNull(c.bloqueo());
        assertNotNull(contabilizar(RESERVA_SIN_BLOQUEO, false, SUCCEEDED, 10_000L, 30_000L, R, TE, TS, "p6").bloqueo());
    }

    @Test
    @DisplayName("gasto proximo al limite: no se llama si gasto medido + reserva supera el tope, ni por encima del tope de predicciones")
    void limite() {
        assertTrue(puedeLlamar(1, 24, 0.0, R, 1.20, null));
        assertTrue(puedeLlamar(24, 24, 1.15, R, 1.20, null), "1,15 + 0,05 = 1,20 cabe justo");
        assertFalse(puedeLlamar(24, 24, 1.151, R, 1.20, null), "no cabe la reserva");
        assertFalse(puedeLlamar(25, 24, 0.0, R, 1.20, null), "tope de predicciones");
        assertFalse(puedeLlamar(25, 100, 0.0, R, 1.20, null), "25 x 0,05 > 1,20: cota estatica");
        assertFalse(puedeLlamar(1, 24, 0.0, R, 1.20, "bloqueo previo"));
    }

    @Test
    @DisplayName("secuencia: dos fallidas sin metricas seguidas con P-H1 cargan 0,10 y la tercera llamada solo si cabe")
    void secuencia() {
        double gasto = 0;
        String bloqueo = null;
        for (int n = 1; n <= 2; n++) {
            assertTrue(puedeLlamar(n, 24, gasto, R, 0.15, bloqueo));
            var c = contabilizar(RESERVA_SIN_BLOQUEO, true, FAILED, null, null, R, TE, TS, "f" + n);
            gasto += c.usd();
            bloqueo = c.bloqueo();
        }
        assertEquals(0.10, gasto, 1e-12);
        assertTrue(puedeLlamar(3, 24, gasto, R, 0.15, bloqueo), "0,10 + 0,05 = 0,15 cabe");
        assertFalse(puedeLlamar(4, 24, gasto + R, R, 0.15, bloqueo), "ya no cabe otra reserva");
    }
}
