package main.web.services.fitsense.shared.infrastructure.mail;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Envia correos fuera del hilo de la peticion y solo despues del commit:
 * <ul>
 *   <li>la respuesta de un endpoint publico no tarda mas cuando el correo existe (el envio no la retrasa), lo que
 *       evitaria descubrir cuentas por el tiempo de respuesta;</li>
 *   <li>si la transaccion se deshace, no sale ningun correo con un codigo que no existe.</li>
 * </ul>
 * Un hilo y una cola acotada: si el proveedor se cuelga, los correos que no caben se descartan con un aviso (la
 * persona puede volver a pedirlo) en lugar de agotar la memoria. Un fallo de envio se registra sin el destinatario.
 */
public class EnvioDeCorreo {

    private static final Logger log = LoggerFactory.getLogger(EnvioDeCorreo.class);

    private final CorreoSaliente correo;
    private final ThreadPoolExecutor ejecutor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(200), r -> {
        var t = new Thread(r, "envio-correo");
        t.setDaemon(true);
        return t;
    });

    private main.web.services.fitsense.shared.infrastructure.observability.ContadoresOperativos contadores;

    public EnvioDeCorreo(CorreoSaliente correo) {
        this.correo = correo;
    }

    /** B5.1: contadores operativos (opcionales: las pruebas unitarias construyen esto sin Spring). */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void contadores(main.web.services.fitsense.shared.infrastructure.observability.ContadoresOperativos contadores) {
        this.contadores = contadores;
    }

    private void contar(String suceso) {
        if (contadores != null) contadores.sumar(suceso);
    }

    public CorreoSaliente correo() {
        return correo;
    }

    /** Programa el envio para cuando la transaccion en curso confirme (o ya, si no hay transaccion). */
    public void trasConfirmar(CorreoSaliente.Mensaje mensaje) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    encolar(mensaje);
                }
            });
        } else {
            encolar(mensaje);
        }
    }

    private void encolar(CorreoSaliente.Mensaje mensaje) {
        try {
            ejecutor.execute(() -> enviar(mensaje));
        } catch (RejectedExecutionException e) {
            log.warn("CORREO_DESCARTADO tipo={} transporte={}: cola llena", mensaje.tipo(), correo.transporte());
            contar(main.web.services.fitsense.shared.infrastructure.observability.ContadoresOperativos.CORREO_DESCARTADO);
        }
    }

    private void enviar(CorreoSaliente.Mensaje mensaje) {
        for (int intento = 1; intento <= 2; intento++) {
            try {
                correo.enviar(mensaje);
                log.info("CORREO_ENVIADO tipo={} transporte={}", mensaje.tipo(), correo.transporte());
                contar(main.web.services.fitsense.shared.infrastructure.observability.ContadoresOperativos.CORREO_ENVIADO);
                return;
            } catch (CorreoSaliente.CorreoNoEnviadoException e) {
                log.warn("CORREO_FALLIDO tipo={} transporte={} intento={} causa={}", mensaje.tipo(),
                        correo.transporte(), intento, e.getMessage());
            } catch (RuntimeException e) {
                log.warn("CORREO_FALLIDO tipo={} transporte={} intento={} causa={}", mensaje.tipo(),
                        correo.transporte(), intento, e.getClass().getSimpleName());
            }
            contar(main.web.services.fitsense.shared.infrastructure.observability.ContadoresOperativos.CORREO_FALLIDO);
            if (!correo.entregaReal()) return;
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    /** Para pruebas: espera a que se vacie la cola. */
    public boolean esperarVacio(long milisegundos) throws InterruptedException {
        long fin = System.currentTimeMillis() + milisegundos;
        while (System.currentTimeMillis() < fin) {
            if (ejecutor.getQueue().isEmpty() && ejecutor.getActiveCount() == 0) return true;
            Thread.sleep(50);
        }
        return false;
    }

    @PreDestroy
    void cerrar() throws InterruptedException {
        ejecutor.shutdown();
        ejecutor.awaitTermination(10, TimeUnit.SECONDS);
    }
}
