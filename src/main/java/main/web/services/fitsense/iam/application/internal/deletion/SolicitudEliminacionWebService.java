package main.web.services.fitsense.iam.application.internal.deletion;

import main.web.services.fitsense.iam.domain.model.valueobjects.EmailAddress;
import main.web.services.fitsense.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import main.web.services.fitsense.shared.infrastructure.mail.CorreoSaliente;
import main.web.services.fitsense.shared.infrastructure.mail.EnvioDeCorreo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Eliminacion de cuenta pedida desde una pagina web publica (requisito de Google Play: poder pedirla sin la app).
 * <ol>
 *   <li><b>Solicitud</b> ({@link #solicitar}): siempre la misma respuesta. Si el correo es de una cuenta activa y no
 *       supero el limite por hora, se crea un token de 256 bits de un solo uso (solo su SHA-256 en la base) y se envia
 *       por el servicio de correo comun tras el commit.</li>
 *   <li><b>Confirmacion</b> ({@link #confirmar}): el token se bloquea, se comprueba y se gasta (con los demas
 *       pendientes de la cuenta); despues se ejecuta exactamente el mismo procedimiento que la app
 *       ({@link EliminacionDeCuentaService}, origen WEB).</li>
 * </ol>
 * Recibir el correo demuestra el control del buzon, que es la misma prueba que permite recuperar la contrasena: no
 * se pide la contrasena. Solo POST borra: un GET del enlace (un antivirus que lo abre, una vista previa) no hace nada.
 */
@Service
public class SolicitudEliminacionWebService {

    public static final String TOKEN_NO_VALIDO = "El enlace de eliminacion no es valido o ha caducado.";

    private static final Logger log = LoggerFactory.getLogger(SolicitudEliminacionWebService.class);
    private static final SecureRandom ALEATORIO = new SecureRandom();
    private static final String MARCADOR = "{token}";
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm 'UTC'");

    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final UserRepository usuarios;
    private final EnvioDeCorreo envio;
    private final EliminacionDeCuentaService eliminacion;
    private final Duration vigencia;
    private final int maximoPorHora;
    private final Optional<String> enlace;
    private final String app;
    private final Clock clock;

    public SolicitudEliminacionWebService(JdbcTemplate jdbc, PlatformTransactionManager transacciones,
                                          UserRepository usuarios, EnvioDeCorreo envio,
                                          EliminacionDeCuentaService eliminacion,
                                          @Value("${fitsense.account-deletion.web.ttl-minutes:30}") long minutos,
                                          @Value("${fitsense.account-deletion.web.max-requests-per-hour:3}") int maximo,
                                          @Value("${fitsense.account-deletion.web.link-template:}") String plantilla,
                                          @Value("${fitsense.mail.from-name:Kora}") String app,
                                          Optional<Clock> clock) {
        if (minutos < 5 || minutos > 120)
            throw new IllegalArgumentException("fitsense.account-deletion.web.ttl-minutes debe estar entre 5 y 120.");
        this.jdbc = jdbc;
        this.tx = new TransactionTemplate(transacciones);
        this.usuarios = usuarios;
        this.envio = envio;
        this.eliminacion = eliminacion;
        this.vigencia = Duration.ofMinutes(minutos);
        this.maximoPorHora = Math.max(1, maximo);
        this.enlace = validarPlantilla(plantilla);
        this.app = app;
        this.clock = clock.orElse(Clock.systemUTC());
    }

    /** No devuelve nada a proposito: el resultado no depende de si el correo existe. */
    public void solicitar(String email) {
        EmailAddress direccion;
        try {
            direccion = new EmailAddress(email);
        } catch (IllegalArgumentException e) {
            return;
        }
        if (!envio.correo().entregaReal()) {
            log.info("ELIMINACION_WEB solicitud recibida, pero no hay transporte de correo: no se envia nada");
            return;
        }
        tx.executeWithoutResult(s -> {
            var usuario = usuarios.findByEmail(direccion).filter(u -> u.canSignIn()).orElse(null);
            if (usuario == null) return;
            var ahora = clock.instant();
            Integer recientes = jdbc.queryForObject("SELECT count(*) FROM account_deletion_tokens WHERE user_id = ?"
                    + " AND created_at > ?", Integer.class, usuario.getId(), Timestamp.from(ahora.minusSeconds(3600)));
            if (recientes != null && recientes >= maximoPorHora) return;
            var bytes = new byte[32];
            ALEATORIO.nextBytes(bytes);
            var token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            var caduca = ahora.plus(vigencia);
            jdbc.update("INSERT INTO account_deletion_tokens (user_id, token_hash, created_at, expires_at)"
                    + " VALUES (?, ?, ?, ?)", usuario.getId(), sha256(token), Timestamp.from(ahora),
                    Timestamp.from(caduca));
            envio.trasConfirmar(mensaje(direccion.address(), token, caduca));
        });
    }

    /**
     * Gasta el token y elimina la cuenta con el procedimiento comun.
     *
     * @throws IllegalArgumentException con {@link #TOKEN_NO_VALIDO} para cualquier token no utilizable
     */
    public EliminacionDeCuentaService.Eliminacion confirmar(String token) {
        if (token == null || token.isBlank() || token.length() > 200) throw new IllegalArgumentException(TOKEN_NO_VALIDO);
        var ahora = Timestamp.from(clock.instant());
        Long userId = tx.execute(s -> {
            var filas = jdbc.queryForList("SELECT t.user_id, t.expires_at, t.used_at, u.status FROM"
                    + " account_deletion_tokens t JOIN users u ON u.user_id = t.user_id WHERE t.token_hash = ?"
                    + " FOR UPDATE OF t", sha256(token.trim()));
            if (filas.isEmpty()) return null;
            var f = filas.get(0);
            if (f.get("used_at") != null || !((Timestamp) f.get("expires_at")).after(ahora)) return null;
            if (!"ACTIVE".equals(f.get("status"))) return null;
            long id = ((Number) f.get("user_id")).longValue();
            // Un solo uso: este y cualquier otro pendiente de la cuenta.
            jdbc.update("UPDATE account_deletion_tokens SET used_at = ? WHERE user_id = ? AND used_at IS NULL",
                    ahora, id);
            return id;
        });
        if (userId == null) throw new IllegalArgumentException(TOKEN_NO_VALIDO);
        try {
            return eliminacion.eliminar(userId, EliminacionDeCuentaService.Origen.WEB);
        } catch (EliminacionDeCuentaService.EliminacionNoRegistradaException e) {
            // La eliminacion no llego a registrarse (nada cambio): se devuelven los tokens gastados en este intento
            // para que el mismo enlace sirva al reintentar, como promete la respuesta 503.
            jdbc.update("UPDATE account_deletion_tokens SET used_at = NULL WHERE user_id = ? AND used_at = ?"
                    + " AND expires_at > ?", userId, ahora, ahora);
            throw e;
        }
    }

    private CorreoSaliente.Mensaje mensaje(String para, String token, Instant caduca) {
        String accion = enlace
                .map(e -> "Para confirmar la eliminacion, abre este enlace y pulsa el boton de confirmar:\n\n"
                        + e.replace(MARCADOR, token))
                .orElse("Para confirmar la eliminacion, introduce este codigo en la pagina de eliminacion:\n\n"
                        + token);
        String texto = """
                Hola:

                Hemos recibido una solicitud para eliminar tu cuenta de %s y todos sus datos.

                %s

                Caduca el %s y solo sirve una vez. La eliminacion es irreversible.

                Si no lo has pedido tu, ignora este mensaje: tu cuenta no se elimina.
                """.formatted(app, accion, HORA.format(caduca.atOffset(ZoneOffset.UTC)));
        return new CorreoSaliente.Mensaje("ELIMINACION_CUENTA_WEB", para,
                "%s: confirma la eliminacion de tu cuenta".formatted(app), texto);
    }

    static Optional<String> validarPlantilla(String plantilla) {
        if (plantilla == null || plantilla.isBlank()) return Optional.empty();
        String p = plantilla.trim();
        if (!p.contains(MARCADOR))
            throw new IllegalStateException("fitsense.account-deletion.web.link-template debe contener " + MARCADOR);
        var uri = URI.create(p.replace(MARCADOR, "x"));
        boolean local = "http".equals(uri.getScheme()) && "localhost".equals(uri.getHost());
        if (!"https".equals(uri.getScheme()) && !local)
            throw new IllegalStateException("fitsense.account-deletion.web.link-template debe usar https");
        return Optional.of(p);
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
