package main.web.services.fitsense.planning.infrastructure.scheduling;

import jakarta.annotation.PreDestroy;
import main.web.services.fitsense.planning.application.internal.generation.PlanGenerationOperationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Ejecuta las operaciones de generacion inicial (V58).
 * <p>
 * Cada pocos segundos recupera las huerfanas y reclama PENDING mientras haya
 * hueco. La generacion corre en un pool PROPIO: el planificador de Spring tiene
 * un solo hilo por defecto, y una generacion de minutos lo bloquearia junto con
 * el cierre semanal y el vencimiento de entrenamientos.
 * <p>
 * Mientras ejecuta, late cada {@code heartbeat-ms}: una generacion lenta pero
 * viva nunca se toma por huerfana. Si el proceso se detiene con una operacion
 * en curso, esta deja de latir y el barrido (de esta u otra instancia) la
 * vuelve a poner en cola pasados {@code stale-seconds}.
 * <p>
 * Las pruebas lo apagan ({@code fitsense.plan-generation.worker.enabled=false}
 * en surefire y failsafe): cada contexto en cache tendria su propio worker
 * sondeando una base que ya no existe.
 */
@Component
@ConditionalOnProperty(name = "fitsense.plan-generation.worker.enabled", havingValue = "true", matchIfMissing = true)
public class PlanGenerationWorker {

    private static final Logger log = LoggerFactory.getLogger(PlanGenerationWorker.class);

    private final PlanGenerationOperationService service;
    private final int maxConcurrent;
    private final ExecutorService pool;
    private final AtomicInteger inFlight = new AtomicInteger();
    private final java.util.concurrent.atomic.AtomicBoolean stopping = new java.util.concurrent.atomic.AtomicBoolean();
    private final java.util.Set<java.util.UUID> running = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public PlanGenerationWorker(PlanGenerationOperationService service,
                                @Value("${fitsense.plan-generation.max-concurrent:2}") int maxConcurrent) {
        this.service = service;
        this.maxConcurrent = Math.max(1, maxConcurrent);
        var counter = new AtomicInteger();
        this.pool = Executors.newFixedThreadPool(this.maxConcurrent, r -> {
            var thread = new Thread(r, "generacion-inicial-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
    }

    @Scheduled(initialDelayString = "${fitsense.plan-generation.initial-delay-ms:5000}",
            fixedDelayString = "${fitsense.plan-generation.poll-ms:2000}")
    public void tick() {
        try {
            if (stopping.get()) return;
            service.recoverStale();
            while (inFlight.get() < maxConcurrent && !stopping.get()) {
                var next = service.claimNext();
                if (next.isEmpty()) return;
                inFlight.incrementAndGet();
                var id = next.get();
                running.add(id);
                pool.execute(() -> {
                    try {
                        service.execute(id, stopping::get);
                    } finally {
                        running.remove(id);
                        inFlight.decrementAndGet();
                    }
                });
            }
        } catch (RuntimeException e) {
            // Una tarea programada que revienta deja de ejecutarse en silencio.
            log.error("Fallo el ciclo del worker de generacion inicial: {}", e.getMessage(), e);
        }
    }

    @Scheduled(initialDelayString = "${fitsense.plan-generation.heartbeat-ms:20000}",
            fixedDelayString = "${fitsense.plan-generation.heartbeat-ms:20000}")
    public void heartbeat() {
        try {
            service.heartbeat(java.util.List.copyOf(running));
        } catch (RuntimeException e) {
            log.error("Fallo el latido de la generacion inicial: {}", e.getMessage(), e);
        }
    }

    /**
     * Apagado ordenado (despliegue, SIGTERM): no reclama mas, interrumpe las
     * generaciones en curso sin registrarlas como fallidas y las devuelve a la
     * cola para que la siguiente instancia las retome. Si el proceso muere sin
     * llegar aqui, el barrido las recupera por falta de latido.
     */
    @PreDestroy
    void stop() {
        stopping.set(true);
        var interrupted = java.util.List.copyOf(running);
        pool.shutdownNow();
        try {
            pool.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        try {
            service.requeueOnShutdown(interrupted);
        } catch (RuntimeException e) {
            log.error("No se pudieron devolver a la cola las generaciones interrumpidas: {}", e.getMessage());
        }
    }
}
