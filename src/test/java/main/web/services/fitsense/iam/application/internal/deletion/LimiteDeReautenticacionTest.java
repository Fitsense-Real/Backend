package main.web.services.fitsense.iam.application.internal.deletion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Limite de reautenticacion: reserva atomica antes de comprobar la contrasena")
class LimiteDeReautenticacionTest {

    @Test
    @DisplayName("50 intentos simultaneos: solo 5 llegan a comprobar la contrasena")
    void concurrencia() throws Exception {
        var limite = new LimiteDeReautenticacion(5, 15, Optional.empty());
        int n = 50;
        var salida = new CountDownLatch(1);
        var reservados = new AtomicInteger();
        var pool = Executors.newFixedThreadPool(16);
        var hechos = new CountDownLatch(n);
        for (int i = 0; i < n; i++)
            pool.submit(() -> {
                try {
                    salida.await();
                    if (limite.reservar(7L) == 0) reservados.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    hechos.countDown();
                }
            });
        salida.countDown();
        hechos.await();
        pool.shutdown();
        assertEquals(5, reservados.get());
        assertTrue(limite.reservar(7L) > 0, "agotado: devuelve la espera");
        assertEquals(0, limite.reservar(8L), "otra cuenta no se ve afectada");
    }

    @Test
    @DisplayName("contrasena correcta: limpiar libera las reservas")
    void limpiar() {
        var limite = new LimiteDeReautenticacion(2, 15, Optional.empty());
        assertEquals(0, limite.reservar(1L));
        assertEquals(0, limite.reservar(1L));
        assertTrue(limite.reservar(1L) > 0);
        limite.limpiar(1L);
        assertEquals(0, limite.reservar(1L));
    }
}
