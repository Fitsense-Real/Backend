package main.web.services.fitsense.orchestration.infrastructure.scheduling;

import main.web.services.fitsense.orchestration.application.internal.WeeklyCycleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * V66 · Relanza los cierres semanales interrumpidos ({@link WeeklyCycleService#recuperarInterrumpidos}).
 * <p>
 * En un hilo PROPIO, no en el planificador de Spring: este tiene un solo hilo, lo comparte con el cierre semanal y el
 * vencimiento de entrenamientos, y un ciclo relanzado puede tardar minutos (la IA).
 * <p>
 * Las pruebas lo apagan ({@code fitsense.ai-operations.recovery.enabled=false} en surefire y failsafe), igual que el
 * worker de la generacion inicial: cada contexto en cache barreria una base que ya no existe. Las pruebas de la
 * recuperacion llaman a {@code recuperarInterrumpidos()} a mano.
 */
@Component
@ConditionalOnProperty(name = "fitsense.ai-operations.recovery.enabled", havingValue = "true", matchIfMissing = true)
public class WeeklyCycleRecoveryScheduler {

    private static final Logger log = LoggerFactory.getLogger(WeeklyCycleRecoveryScheduler.class);

    private final ScheduledExecutorService hilo;

    public WeeklyCycleRecoveryScheduler(WeeklyCycleService ciclo,
                                        @Value("${fitsense.ai-operations.recovery.initial-delay-ms:60000}") long inicialMs,
                                        @Value("${fitsense.ai-operations.recovery.interval-ms:120000}") long intervaloMs) {
        this.hilo = Executors.newSingleThreadScheduledExecutor(r -> {
            var t = new Thread(r, "ciclo-semanal-recuperacion");
            t.setDaemon(true);
            return t;
        });
        hilo.scheduleWithFixedDelay(() -> {
            try {
                int n = ciclo.recuperarInterrumpidos();
                if (n > 0) log.warn("CICLO_SEMANAL_RECUPERACION {} ciclo(s) relanzado(s)", n);
            } catch (RuntimeException e) {
                log.error("Fallo el barrido de ciclos semanales interrumpidos: {}", e.getMessage(), e);
            }
        }, Math.max(0, inicialMs), Math.max(1000, intervaloMs), TimeUnit.MILLISECONDS);
    }

    @jakarta.annotation.PreDestroy
    void parar() {
        hilo.shutdownNow();
    }
}
