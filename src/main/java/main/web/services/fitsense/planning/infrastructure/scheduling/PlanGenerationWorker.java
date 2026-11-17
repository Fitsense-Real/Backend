package main.web.services.fitsense.planning.infrastructure.scheduling;

import jakarta.annotation.PreDestroy;
import main.web.services.fitsense.planning.application.internal.generation.PlanGenerationOperationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
 * en curso, esta deja de latir, su lease vence ({@code lease-seconds}) y el barrido
 * (de esta u otra instancia) la vuelve a poner en cola.
 * <p>
 * V59: el latido y el ciclo corren en hilos PROPIOS ({@code generacion-inicial-latido} y
 * {@code generacion-inicial-ciclo}), no en el planificador de Spring. Ese planificador tiene
 * un solo hilo y lo comparte con el cierre semanal, que llama a la IA usuario por usuario
 * durante minutos: con el latido alli, una generacion viva dejaba de latir, se tomaba por
 * huerfana, se reencolaba y se ejecutaba dos veces (predicciones duplicadas).
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
    /** V59: un hilo para el ciclo y otro para el latido; el latido nunca espera al ciclo ni al cierre semanal. */
    private final java.util.concurrent.ScheduledExecutorService ciclo;
    private final java.util.concurrent.ScheduledExecutorService latido;
    private final long initialDelayMs;
    private final long pollMs;
    private final long heartbeatMs;
    /** V59: cada cuanto se barre (recoverStale). El reclamo sigue cada poll-ms; el barrido no necesita tanto. */
    private final long recoverMs;
    private volatile long ultimoBarrido = Long.MIN_VALUE;

    /** Para pruebas: sin programar nada; las pruebas llaman a tick() y heartbeat() a mano. */
    public PlanGenerationWorker(PlanGenerationOperationService service, int maxConcurrent) {
        this(service, maxConcurrent, -1, -1, -1, 0);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public PlanGenerationWorker(PlanGenerationOperationService service,
                                @Value("${fitsense.plan-generation.max-concurrent:2}") int maxConcurrent,
                                @Value("${fitsense.plan-generation.initial-delay-ms:5000}") long initialDelayMs,
                                @Value("${fitsense.plan-generation.poll-ms:2000}") long pollMs,
                                @Value("${fitsense.plan-generation.heartbeat-ms:20000}") long heartbeatMs,
                                @Value("${fitsense.plan-generation.recover-ms:30000}") long recoverMs) {
        this.service = service;
        this.recoverMs = Math.max(0, recoverMs);
        this.initialDelayMs = initialDelayMs;
        this.pollMs = pollMs;
        this.heartbeatMs = heartbeatMs;
        this.ciclo = hilo("generacion-inicial-ciclo");
        this.latido = hilo("generacion-inicial-latido");
        this.maxConcurrent = Math.max(1, maxConcurrent);
        var counter = new AtomicInteger();
        this.pool = Executors.newFixedThreadPool(this.maxConcurrent, r -> {
            var thread = new Thread(r, "generacion-inicial-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
    }

    private static java.util.concurrent.ScheduledExecutorService hilo(String nombre) {
        return Executors.newSingleThreadScheduledExecutor(r -> {
            var thread = new Thread(r, nombre);
            thread.setDaemon(true);
            return thread;
        });
    }

    /** V59: programa el ciclo y el latido en sus hilos propios (no en el planificador compartido de Spring). */
    @jakarta.annotation.PostConstruct
    public void start() {
        if (pollMs <= 0 || heartbeatMs <= 0) return;
        ciclo.scheduleWithFixedDelay(this::tick, Math.max(0, initialDelayMs), pollMs,
                java.util.concurrent.TimeUnit.MILLISECONDS);
        latido.scheduleWithFixedDelay(this::heartbeat, heartbeatMs, heartbeatMs,
                java.util.concurrent.TimeUnit.MILLISECONDS);
        log.info("Generacion inicial: worker {} con ciclo cada {} ms, barrido cada {} ms y latido cada {} ms en hilos"
                + " propios", service.owner(), pollMs, recoverMs, heartbeatMs);
    }

    public void tick() {
        try {
            if (stopping.get()) return;
            // V59: el barrido son tres UPDATE indexados; cada recover-ms basta (el lease dura 90 s). Con #71 corria en
            // cada ciclo (cada 2 s) en cada instancia.
            long ahora = System.nanoTime() / 1_000_000;
            if (ultimoBarrido == Long.MIN_VALUE || ahora - ultimoBarrido >= recoverMs) {
                ultimoBarrido = ahora;
                service.recoverStale();
            }
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
    public void stop() {
        stopping.set(true);
        ciclo.shutdownNow();
        latido.shutdownNow();
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
