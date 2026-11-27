package main.web.services.fitsense.iam.application.internal.commandservices;

import jakarta.servlet.http.HttpServletRequest;
import main.web.services.fitsense.shared.infrastructure.ratelimit.LimitePorIp;
import main.web.services.fitsense.shared.infrastructure.ratelimit.LimitePorIp.DemasiadasSolicitudesException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * P0-02 (auditoria de preproduccion): limites contra el abuso de {@code POST /auth/login} y {@code POST /auth/register}.
 * <ul>
 *   <li><b>Por IP</b> (reutiliza {@link LimitePorIp}): frena el rociado de contrasenas y el alta masiva. El cupo es
 *       configurable y, por omision, generoso, porque en una sesion de captacion muchas personas comparten la IP de
 *       una misma red.</li>
 *   <li><b>Por cuenta</b>: como mucho {@code fitsense.auth-rate-limit.login-failures-per-account} intentos FALLIDOS
 *       por correo en la ventana. Despues, 429 aunque la contrasena sea la correcta, hasta que la ventana pase. Se
 *       aplica <b>igual exista o no el correo</b> (la clave es el correo normalizado, no la cuenta): ni la respuesta ni
 *       el limite permiten descubrir cuentas. Un inicio de sesion correcto vacia el contador.</li>
 * </ul>
 * En memoria, como {@link LimitePorIp} (S-4): un reinicio los vacia y no se comparten entre replicas. Hoy se despliega
 * una sola instancia, a proposito.
 */
@Component
public class LimiteDeAutenticacion {

    /** Mismo mensaje para cualquier correo: no revela si la cuenta existe. */
    public static final String MENSAJE_CUENTA = "Demasiados intentos de inicio de sesion. Espera antes de volver a"
            + " intentarlo.";

    private static final int MAXIMO_DE_CLAVES = 50_000;

    private final LimitePorIp limitePorIp;
    private final int loginPorIp;
    private final int registroPorIp;
    private final int fallosPorCuenta;
    private final Duration ventana;
    private final Clock clock;
    private final ConcurrentHashMap<String, Deque<Instant>> fallos = new ConcurrentHashMap<>();

    public LimiteDeAutenticacion(LimitePorIp limitePorIp,
                                 @Value("${fitsense.auth-rate-limit.login-per-ip:60}") int loginPorIp,
                                 @Value("${fitsense.auth-rate-limit.register-per-ip:30}") int registroPorIp,
                                 @Value("${fitsense.auth-rate-limit.login-failures-per-account:5}") int fallosPorCuenta,
                                 @Value("${fitsense.auth-rate-limit.window-minutes:15}") long minutos,
                                 Optional<Clock> clock) {
        if (loginPorIp < 1 || registroPorIp < 1 || fallosPorCuenta < 1 || minutos < 1)
            throw new IllegalArgumentException("fitsense.auth-rate-limit.* deben ser al menos 1.");
        this.limitePorIp = limitePorIp;
        this.loginPorIp = loginPorIp;
        this.registroPorIp = registroPorIp;
        this.fallosPorCuenta = fallosPorCuenta;
        this.ventana = Duration.ofMinutes(minutos);
        this.clock = clock.orElse(Clock.systemUTC());
    }

    /** Antes de comprobar la contrasena: cupo por IP y bloqueo por fallos de ese correo. */
    public void antesDelLogin(String email, HttpServletRequest peticion) {
        limitePorIp.comprobar("auth-login", peticion, loginPorIp, ventana);
        var l = fallos.get(clave(email));
        if (l == null) return;
        var ahora = clock.instant();
        synchronized (l) {
            purgar(l, ahora);
            if (l.size() >= fallosPorCuenta)
                throw new DemasiadasSolicitudesException(MENSAJE_CUENTA,
                        Math.max(1, Duration.between(ahora, l.peekFirst().plus(ventana)).toSeconds()));
        }
    }

    /** Contrasena incorrecta, correo inexistente o cuenta no activa: los tres cuentan igual. */
    public void falloDeLogin(String email) {
        var ahora = clock.instant();
        if (fallos.size() > MAXIMO_DE_CLAVES) purgarTodo(ahora);
        var l = fallos.computeIfAbsent(clave(email), k -> new ArrayDeque<>());
        synchronized (l) {
            purgar(l, ahora);
            l.addLast(ahora);
        }
    }

    public void loginCorrecto(String email) {
        fallos.remove(clave(email));
    }

    public void antesDelRegistro(HttpServletRequest peticion) {
        limitePorIp.comprobar("auth-register", peticion, registroPorIp, ventana);
    }

    /** El correo como lo normaliza el registro ({@code EmailAddress}); uno mal formado tambien cuenta. */
    static String clave(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private void purgar(Deque<Instant> l, Instant ahora) {
        while (!l.isEmpty() && !l.peekFirst().plus(ventana).isAfter(ahora)) l.removeFirst();
    }

    private void purgarTodo(Instant ahora) {
        fallos.entrySet().removeIf(e -> {
            synchronized (e.getValue()) {
                purgar(e.getValue(), ahora);
                return e.getValue().isEmpty();
            }
        });
        if (fallos.size() > MAXIMO_DE_CLAVES) fallos.clear();
    }
}
