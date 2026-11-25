package main.web.services.fitsense.planning.infrastructure.generation.ai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LibroPresupuestoH2v2Test {

    @TempDir Path dir;

    @Test
    void lasReservasSeAcumulanEntreEjecucionesYNoSeDevuelven() {
        var f = dir.resolve("libro.csv");
        var libro = new LibroPresupuestoH2v2(f);
        assertTrue(libro.reservar("E1", 3.20, "G01", "A", 1, 3.00, ""));
        libro.resultado("E1", "G01", "A", 1, "p1", "ERROR_PROVEEDOR");
        // Una ejecucion nueva lee lo ya reservado: el limite no se reinicia.
        var otra = new LibroPresupuestoH2v2(f);
        assertEquals(3.00, otra.reservado(null), 1e-9);
        assertEquals(3.00, otra.reservado("E1"), 1e-9);
        assertFalse(otra.reservar("E1", 3.20, "G02", "A", 1, 0.30, ""), "no cabe en el tope de la etapa");
        assertTrue(otra.reservar("E2", 7.50, "G02", "A", 1, 4.50, ""));
        assertFalse(otra.reservar("E2", 7.50, "G02", "A", 2, 0.01, ""), "no cabe en el tope total de 7,50");
        assertEquals(7.50, otra.reservado(null), 1e-9);
        assertEquals(0.0, otra.saldo(), 1e-9);
    }

    @Test
    void laCotaEsLaDeReplicateConsumo() {
        // 1 token por byte + 256 de formato a 0,25 USD/M y 16.000 de salida a 2,00 USD/M.
        assertEquals((197_299 + 256) / 1e6 * 0.25 + 0.032, LibroPresupuestoH2v2.cota(197_299, 16_000), 1e-12);
    }
}
