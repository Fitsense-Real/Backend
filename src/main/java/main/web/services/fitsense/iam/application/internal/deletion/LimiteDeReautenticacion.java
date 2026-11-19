package main.web.services.fitsense.iam.application.internal.deletion;

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
 * Limite de contrasenas incorrectas al confirmar una accion sensible (eliminar la cuenta): {@code maximo} fallos por
 * usuario en una ventana deslizante. En memoria, como el resto de limites del backend: el despliegue es de una sola
 * instancia (PREPARACION_HTTPS_RAILWAY.md). Un reinicio vacia el contador, lo que solo devuelve los intentos.
 */
@Component
public class LimiteDeReautenticacion {

    private final int maximo;
    private final Duration ventana;
    private final Clock clock;
    private final ConcurrentHashMap<Long, Deque<Instant>> fallos = new ConcurrentHashMap<>();

    public LimiteDeReautenticacion(@Value("${fitsense.account-deletion.max-failed-reauth:5}") int maximo,
                                   @Value("${fitsense.account-deletion.reauth-window-minutes:15}") long minutos,
                                   Optional<Clock> clock) {
        this.maximo = maximo;
        this.ventana = Duration.ofMinutes(minutos);
        this.clock = clock.orElse(Clock.systemUTC());
    }

    /** Segundos hasta que se pueda volver a intentar; 0 si se puede ya. */
    public long bloqueadoDurante(long userId) {
        var ahora = clock.instant();
        var l = fallos.get(userId);
        if (l == null) return 0;
        synchronized (l) {
            purgar(l, ahora);
            if (l.size() < maximo) return 0;
            return Math.max(1, Duration.between(ahora, l.peekFirst().plus(ventana)).toSeconds());
        }
    }

    /**
     * Reserva un intento ANTES de comprobar la contrasena, de forma atomica: con N peticiones simultaneas solo
     * {@code maximo} llegan a comprobarla en la ventana. Devuelve los segundos de espera si no quedan intentos (y no
     * reserva), o 0 si se reservo. Si la contrasena resulta correcta, {@link #limpiar} libera las reservas.
     */
    public long reservar(long userId) {
        var ahora = clock.instant();
        var l = fallos.computeIfAbsent(userId, k -> new ArrayDeque<>());
        synchronized (l) {
            purgar(l, ahora);
            if (l.size() >= maximo)
                return Math.max(1, Duration.between(ahora, l.peekFirst().plus(ventana)).toSeconds());
            l.addLast(ahora);
            return 0;
        }
    }

    public void registrarFallo(long userId) {
        var ahora = clock.instant();
        var l = fallos.computeIfAbsent(userId, k -> new ArrayDeque<>());
        synchronized (l) {
            purgar(l, ahora);
            l.addLast(ahora);
        }
    }

    public void limpiar(long userId) {
        fallos.remove(userId);
    }

    private void purgar(Deque<Instant> l, Instant ahora) {
        while (!l.isEmpty() && !l.peekFirst().plus(ventana).isAfter(ahora)) l.removeFirst();
    }
}
