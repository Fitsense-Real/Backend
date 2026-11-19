package main.web.services.fitsense.orchestration.infrastructure.locking;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CicloSemanalBloqueo: conexion directa, clave por participante y fallo seguro")
class CicloSemanalBloqueoTest {

    @Test
    @DisplayName("el endpoint -pooler de Neon se cambia por el directo; otras URL quedan igual")
    void directa() {
        assertEquals("jdbc:postgresql://ep-dark-brook-b4emftmn.c-6.us-east-2.aws.neon.tech/neondb?sslmode=require",
                CicloSemanalBloqueo.directa(
                        "jdbc:postgresql://ep-dark-brook-b4emftmn-pooler.c-6.us-east-2.aws.neon.tech/neondb?sslmode=require"));
        assertEquals("jdbc:postgresql://127.0.0.1:5432/db", CicloSemanalBloqueo.directa("jdbc:postgresql://127.0.0.1:5432/db"));
    }

    @Test
    @DisplayName("la clave es propia del ciclo y distinta por participante")
    void clave() {
        assertNotEquals(CicloSemanalBloqueo.clave(1), CicloSemanalBloqueo.clave(2));
        assertEquals(0x46534331L, CicloSemanalBloqueo.clave(7) >>> 32);
        assertEquals(7, CicloSemanalBloqueo.clave(7) & 0xFFFFFFFFL);
    }

    @Test
    @DisplayName("sin conexion para el bloqueo no se ejecuta el ciclo (SIN_BLOQUEO)")
    void sinConexionNoEjecuta() {
        var bloqueo = new CicloSemanalBloqueo("jdbc:postgresql://127.0.0.1:1/nada", "x", "x", 1);
        var ejecutado = new boolean[1];
        var r = bloqueo.ejecutar(5L, () -> {
            ejecutado[0] = true;
            return "plan";
        });
        assertEquals(CicloSemanalBloqueo.Estado.SIN_BLOQUEO, r.estado());
        assertNull(r.valor());
        assertFalse(ejecutado[0]);
    }
}
