package main.web.services.fitsense.shared.infrastructure.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limite de peticiones por IP para los endpoints publicos (recuperacion de contrasena, solicitud web de
 * eliminacion): {@code maximo} por accion e IP en una ventana deslizante. Frena el abuso (correos masivos a terceros,
 * fuerza bruta de codigos) sin depender de si el correo existe, asi que no permite descubrir cuentas.
 * <p>
 * La IP es {@link HttpServletRequest#getRemoteAddr()}: detras del proxy de Railway solo es la del cliente con
 * {@code server.forward-headers-strategy=native} (PREPARACION_HTTPS_RAILWAY.md). En memoria: una sola instancia.
 */
@Component
public class LimitePorIp {

    /** Se lanza cuando se supera el limite; el manejador global responde 429 con Retry-After. */
    public static class DemasiadasSolicitudesException extends RuntimeException {
        private final long reintentarEnSegundos;

        public DemasiadasSolicitudesException(long reintentarEnSegundos) {
            super("Demasiadas solicitudes desde esta conexion. Espera antes de volver a intentarlo.");
            this.reintentarEnSegundos = reintentarEnSegundos;
        }

        public long reintentarEnSegundos() {
            return reintentarEnSegundos;
        }
    }

    private static final int MAXIMO_DE_CLAVES = 50_000;

    private final int maximo;
    private final Duration ventana;
    private final Clock clock;
    private final ConcurrentHashMap<String, Deque<Instant>> marcas = new ConcurrentHashMap<>();

    public LimitePorIp(@Value("${fitsense.public-rate-limit.max-per-window:20}") int maximo,
                       @Value("${fitsense.public-rate-limit.window-minutes:15}") long minutos,
                       Optional<Clock> clock) {
        this.maximo = maximo;
        this.ventana = Duration.ofMinutes(minutos);
        this.clock = clock.orElse(Clock.systemUTC());
    }

    /** Cuenta la peticion; lanza si la IP ya agoto el cupo de esta accion. */
    public void comprobar(String accion, HttpServletRequest peticion) {
        var ahora = clock.instant();
        if (marcas.size() > MAXIMO_DE_CLAVES) purgarTodo(ahora);
        var l = marcas.computeIfAbsent(accion + "|" + peticion.getRemoteAddr(), k -> new ArrayDeque<>());
        synchronized (l) {
            purgar(l, ahora);
            if (l.size() >= maximo)
                throw new DemasiadasSolicitudesException(
                        Math.max(1, Duration.between(ahora, l.peekFirst().plus(ventana)).toSeconds()));
            l.addLast(ahora);
        }
    }

    private void purgar(Deque<Instant> l, Instant ahora) {
        while (!l.isEmpty() && !l.peekFirst().plus(ventana).isAfter(ahora)) l.removeFirst();
    }

    private void purgarTodo(Instant ahora) {
        marcas.entrySet().removeIf(e -> {
            synchronized (e.getValue()) {
                purgar(e.getValue(), ahora);
                return e.getValue().isEmpty();
            }
        });
    }
}
