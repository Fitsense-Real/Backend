package main.web.services.fitsense.planning.application.internal.commandservices;

import main.web.services.fitsense.planning.domain.exceptions.DemasiadasGeneracionesException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * R-5 · LIMITE DE GENERACIONES INICIALES POR PERSONA (resiliencia de Replicate, 11/11/2026).
 * <p>
 * Ventana deslizante de una hora: como mucho {@code fitsense.ai.generate.max-per-hour} peticiones que LLEGAN A
 * GENERAR (las que pueden llamar a la IA, hasta 3 predicciones cada una). No cuentan las que se rechazan antes:
 * plan ya existente, perfil fuera de rango, generacion en curso.
 * <p>
 * <b>Por que 5 por defecto.</b> El flujo normal necesita 1. Si falla (proveedor caido, perfil sin semana posible),
 * la persona puede corregir su perfil y reintentar algunas veces. 5 por hora acota el peor caso a 15 predicciones
 * por persona y hora (unos USD 0,35 con el coste de referencia de GRA) sin bloquear un uso razonable.
 * <p>
 * <b>Limite declarado:</b> la ventana vive en memoria y es por instancia. Con N instancias el limite efectivo es
 * N x el configurado, y un reinicio la vacia. La exclusion de generaciones simultaneas, en cambio, si es comun a
 * todas las instancias (bloqueo consultivo de PostgreSQL). Este limite no es un tope de gasto de la cuenta de
 * Replicate: ese tope solo puede ponerse en la propia cuenta.
 */
@Component
public class LimiteDeGeneraciones {

    static final Duration VENTANA = Duration.ofHours(1);

    private final int maximoPorHora;
    private final Clock reloj;
    private final Map<Long, Deque<Instant>> usos = new ConcurrentHashMap<>();

    /** Con dos constructores, Spring necesita saber cual usar (INC-4C-01). */
    @org.springframework.beans.factory.annotation.Autowired
    public LimiteDeGeneraciones(@Value("${fitsense.ai.generate.max-per-hour:5}") int maximoPorHora,
                                ObjectProvider<Clock> reloj) {
        this(maximoPorHora, reloj.getIfAvailable(Clock::systemUTC));
    }

    LimiteDeGeneraciones(int maximoPorHora, Clock reloj) {
        if (maximoPorHora < 1 || maximoPorHora > 100)
            throw new IllegalArgumentException("fitsense.ai.generate.max-per-hour debe estar entre 1 y 100.");
        this.maximoPorHora = maximoPorHora;
        this.reloj = reloj;
    }

    /** Cuenta una generacion para {@code userId}, o lanza {@link DemasiadasGeneracionesException} sin contarla. */
    public void consumir(long userId) {
        var ahora = reloj.instant();
        var cola = usos.computeIfAbsent(userId, k -> new ArrayDeque<>());
        synchronized (cola) {
            while (!cola.isEmpty() && !cola.peekFirst().isAfter(ahora.minus(VENTANA))) cola.pollFirst();
            if (cola.size() >= maximoPorHora) {
                long espera = Math.max(1, Duration.between(ahora, cola.peekFirst().plus(VENTANA)).toSeconds());
                throw new DemasiadasGeneracionesException(espera);
            }
            cola.addLast(ahora);
        }
    }

    int maximoPorHora() {
        return maximoPorHora;
    }
}
