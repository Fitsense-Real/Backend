package main.web.services.fitsense.iam.application.internal.commandservices;

import main.web.services.fitsense.iam.application.internal.outboundservices.hashing.HashingService;
import main.web.services.fitsense.iam.application.internal.outboundservices.notification.PasswordResetNotifier;
import main.web.services.fitsense.iam.domain.model.aggregates.PasswordResetToken;
import main.web.services.fitsense.iam.domain.model.valueobjects.EmailAddress;
import main.web.services.fitsense.iam.infrastructure.persistence.jpa.repositories.PasswordResetTokenRepository;
import main.web.services.fitsense.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Recuperacion de contrasena (cierre definitivo del backend, fase 2.2).
 * <ul>
 *   <li><b>Solicitud:</b> siempre la misma respuesta, exista o no el correo (no permite descubrir cuentas). Si la
 *       cuenta existe y puede iniciar sesion, y no supero el limite de solicitudes por hora, se crea un token
 *       aleatorio de 256 bits, de un solo uso y con caducidad corta; se guarda SOLO su SHA-256.</li>
 *   <li><b>Confirmacion:</b> el token (bloqueado en la base para que dos confirmaciones simultaneas no lo usen dos
 *       veces) tiene que existir, no estar usado ni caducado y ser de una cuenta activa. Se cambia la contrasena, se
 *       marca usado y se invalidan los demas tokens pendientes de esa cuenta. Cualquier fallo da el mismo error.</li>
 * </ul>
 * La entrega la hace {@link PasswordResetNotifier}; sin proveedor autorizado no se envia nada.
 */
@Service
public class PasswordResetService {

    public static final String TOKEN_NO_VALIDO = "El codigo de recuperacion no es valido o ha caducado.";

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final UserRepository usuarios;
    private final PasswordResetTokenRepository tokens;
    private final HashingService hashing;
    private final PasswordResetNotifier notificador;
    private final Duration vigencia;
    private final int maximoPorHora;
    private final Clock reloj;

    public PasswordResetService(UserRepository usuarios, PasswordResetTokenRepository tokens, HashingService hashing,
                                PasswordResetNotifier notificador,
                                @Value("${fitsense.password-reset.ttl-minutes:30}") long minutos,
                                @Value("${fitsense.password-reset.max-requests-per-hour:3}") int maximoPorHora,
                                ObjectProvider<Clock> reloj) {
        if (minutos < 5 || minutos > 120)
            throw new IllegalArgumentException("fitsense.password-reset.ttl-minutes debe estar entre 5 y 120.");
        if (maximoPorHora < 1)
            throw new IllegalArgumentException("fitsense.password-reset.max-requests-per-hour debe ser al menos 1.");
        this.usuarios = usuarios;
        this.tokens = tokens;
        this.hashing = hashing;
        this.notificador = notificador;
        this.vigencia = Duration.ofMinutes(minutos);
        this.maximoPorHora = maximoPorHora;
        this.reloj = reloj.getIfAvailable(Clock::systemUTC);
    }

    /** No devuelve nada a proposito: el resultado no depende de si el correo existe. */
    @Transactional
    public void solicitar(String email) {
        EmailAddress direccion;
        try {
            direccion = new EmailAddress(email);
        } catch (IllegalArgumentException e) {
            return;
        }
        var usuario = usuarios.findByEmail(direccion).filter(u -> u.canSignIn()).orElse(null);
        if (usuario == null) return;
        var ahora = OffsetDateTime.now(reloj);
        if (tokens.countByUserIdAndCreatedAtAfter(usuario.getId(), ahora.minusHours(1)) >= maximoPorHora) return;

        var bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        var token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        var caduca = ahora.plus(vigencia);
        tokens.save(new PasswordResetToken(usuario.getId(), sha256(token), ahora, caduca));
        notificador.enviar(direccion.address(), token, caduca);
    }

    @Transactional
    public void confirmar(String token, String nuevaContrasena) {
        if (token == null || token.isBlank() || token.length() > 200)
            throw new IllegalArgumentException(TOKEN_NO_VALIDO);
        var ahora = OffsetDateTime.now(reloj);
        var fila = tokens.bloquearPorHash(sha256(token.trim())).filter(t -> t.usableAt(ahora))
                .orElseThrow(() -> new IllegalArgumentException(TOKEN_NO_VALIDO));
        var usuario = usuarios.findById(fila.getUserId()).filter(u -> u.canSignIn())
                .orElseThrow(() -> new IllegalArgumentException(TOKEN_NO_VALIDO));
        usuario.changePassword(hashing.encode(nuevaContrasena));
        usuarios.save(usuario);
        fila.markUsed(ahora);
        tokens.save(fila);
        tokens.invalidarPendientes(usuario.getId(), ahora);
    }

    /** Para informar en el arranque y en la documentacion: sin entrega real el flujo no es operativo. */
    public boolean entregaReal() {
        return notificador.entregaReal();
    }

    static String sha256(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
