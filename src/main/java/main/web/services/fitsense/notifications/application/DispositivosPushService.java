package main.web.services.fitsense.notifications.application;

import main.web.services.fitsense.notifications.infrastructure.FirebaseTransportePush;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Registro de instalaciones de la app (Firebase Installation ID) para la cuenta autenticada.
 * <ul>
 *   <li><b>Registro y renovacion</b>: idempotente. El mismo FID para la misma cuenta solo actualiza la ultima
 *       actividad, la version de la app y la version de credenciales. Un FID nuevo (renovado por Firebase) es otro
 *       dispositivo; el anterior se desvincula cuando la app avisa ({@code onUnregistered}) o cuando FCM responde
 *       UNREGISTERED.</li>
 *   <li><b>Cambio de cuenta</b>: un FID activo pertenece a una sola cuenta. Registrarlo con otra cuenta revoca el
 *       vinculo anterior (CAMBIO_DE_CUENTA): la persona anterior deja de recibir en ese telefono.</li>
 *   <li><b>Desvinculacion</b>: idempotente y solo sobre los dispositivos propios.</li>
 *   <li><b>Cambio de contrasena</b>: el dispositivo guarda la version de credenciales con la que se registro; si cambia,
 *       deja de recibir hasta que la app vuelva a registrarse con una sesion nueva.</li>
 * </ul>
 * Solo se admiten FID: un token de registro FCM (lleva ':' y mide mas de 64) se rechaza.
 */
@Service
public class DispositivosPushService {

    /** Alfabeto base64url, sin ':' (los tokens de registro FCM lo llevan). Un FID mide 22 caracteres. */
    static final Pattern FID = Pattern.compile("^[A-Za-z0-9_-]{16,64}$");
    public static final String PLATAFORMA_ANDROID = "ANDROID";

    private final JdbcTemplate jdbc;
    private final String paqueteAndroid;
    private final int maximoPorUsuario;
    private final Clock reloj;

    public DispositivosPushService(JdbcTemplate jdbc,
                                   @Value("${fitsense.push.android.package:pe.fit.kora}") String paqueteAndroid,
                                   @Value("${fitsense.push.max-devices-per-user:5}") int maximoPorUsuario,
                                   ObjectProvider<Clock> reloj) {
        if (maximoPorUsuario < 1) throw new IllegalArgumentException("fitsense.push.max-devices-per-user >= 1");
        this.jdbc = jdbc;
        this.paqueteAndroid = paqueteAndroid;
        this.maximoPorUsuario = maximoPorUsuario;
        this.reloj = reloj.getIfAvailable(Clock::systemUTC);
    }

    public record Dispositivo(long deviceId, String installationIdHint, String platform, String appVersion,
                              Instant registeredAt, Instant lastSeenAt, boolean receivesNotifications) {}

    public record Registro(Dispositivo dispositivo, boolean nuevo) {}

    @Transactional
    public Registro registrar(long userId, String installationId, String plataforma, String appId, String appVersion) {
        if (installationId == null || !FID.matcher(installationId.trim()).matches())
            throw new SolicitudPushInvalidaException("IDENTIFICADOR_PUSH_INVALIDO",
                    "El identificador debe ser un Firebase Installation ID (no un token de registro FCM).");
        var fid = installationId.trim();
        if (!PLATAFORMA_ANDROID.equals(plataforma))
            throw new SolicitudPushInvalidaException("PLATAFORMA_NO_ADMITIDA", "Solo se admite la plataforma ANDROID.");
        if (!paqueteAndroid.equals(appId))
            throw new SolicitudPushInvalidaException("APLICACION_NO_ADMITIDA",
                    "La aplicacion no corresponde a esta configuracion de Firebase.");
        if (appVersion != null && appVersion.length() > 40)
            throw new SolicitudPushInvalidaException("VERSION_APP_INVALIDA", "appVersion admite como maximo 40 caracteres.");

        var ahora = Timestamp.from(reloj.instant());
        // La cuenta se bloquea para que dos registros simultaneos no superen el maximo ni lean una version vieja.
        var cuenta = jdbc.queryForMap("SELECT status, token_version FROM users WHERE user_id = ? FOR UPDATE", userId);
        if (!"ACTIVE".equals(cuenta.get("status")))
            throw new SolicitudPushInvalidaException("CUENTA_NO_ACTIVA", "La cuenta no esta activa.");
        int version = ((Number) cuenta.get("token_version")).intValue();

        var actual = jdbc.queryForList("SELECT device_id, user_id FROM push_devices"
                + " WHERE installation_id = ? AND revoked_at IS NULL FOR UPDATE", fid);
        if (!actual.isEmpty()) {
            long dueno = ((Number) actual.get(0).get("user_id")).longValue();
            long id = ((Number) actual.get(0).get("device_id")).longValue();
            if (dueno == userId) {
                jdbc.update("UPDATE push_devices SET last_seen_at = ?, token_version = ?, app_version = ?"
                        + " WHERE device_id = ?", ahora, version, appVersion, id);
                return new Registro(leer(userId, id), false);
            }
            revocar(id, "CAMBIO_DE_CUENTA", ahora);
        }
        // Como mucho N dispositivos activos: el menos reciente deja su lugar.
        var activos = jdbc.queryForList("SELECT device_id FROM push_devices WHERE user_id = ? AND revoked_at IS NULL"
                + " ORDER BY last_seen_at DESC, device_id DESC", Long.class, userId);
        for (int i = maximoPorUsuario - 1; i < activos.size(); i++) revocar(activos.get(i), "REEMPLAZADO", ahora);

        Long id = jdbc.queryForObject("INSERT INTO push_devices (user_id, installation_id, identifier_type, platform,"
                + " app_id, app_version, token_version, registered_at, last_seen_at)"
                + " VALUES (?, ?, 'FID', ?, ?, ?, ?, ?, ?) RETURNING device_id", Long.class,
                userId, fid, plataforma, appId, appVersion, version, ahora, ahora);
        return new Registro(leer(userId, id), true);
    }

    /** Idempotente: desvincular un FID ajeno o inexistente no hace nada y no lo revela. */
    @Transactional
    public void desvincular(long userId, String installationId) {
        if (installationId == null) return;
        jdbc.update("UPDATE push_devices SET revoked_at = ?, revoke_reason = 'DESVINCULADO'"
                        + " WHERE user_id = ? AND installation_id = ? AND revoked_at IS NULL",
                Timestamp.from(reloj.instant()), userId, installationId.trim());
    }

    public List<Dispositivo> listar(long userId) {
        return jdbc.query("SELECT d.device_id, d.installation_id, d.platform, d.app_version, d.registered_at,"
                + " d.last_seen_at, (d.token_version = u.token_version) AS recibe"
                + " FROM push_devices d JOIN users u ON u.user_id = d.user_id"
                + " WHERE d.user_id = ? AND d.revoked_at IS NULL ORDER BY d.last_seen_at DESC", (rs, n) ->
                new Dispositivo(rs.getLong(1), FirebaseTransportePush.ofuscar(rs.getString(2)), rs.getString(3),
                        rs.getString(4), rs.getTimestamp(5).toInstant(), rs.getTimestamp(6).toInstant(),
                        rs.getBoolean(7)), userId);
    }

    /** FCM respondio que el FID ya no esta registrado: no se le vuelve a enviar. */
    public void marcarNoRegistrado(long deviceId) {
        jdbc.update("UPDATE push_devices SET revoked_at = ?, revoke_reason = 'NO_REGISTRADO_EN_FCM'"
                + " WHERE device_id = ? AND revoked_at IS NULL", Timestamp.from(reloj.instant()), deviceId);
    }

    private void revocar(long deviceId, String motivo, Timestamp ahora) {
        jdbc.update("UPDATE push_devices SET revoked_at = ?, revoke_reason = ? WHERE device_id = ?"
                + " AND revoked_at IS NULL", ahora, motivo, deviceId);
    }

    private Dispositivo leer(long userId, long deviceId) {
        return listar(userId).stream().filter(d -> d.deviceId() == deviceId).findFirst().orElseThrow();
    }

    /** 400 con el codigo en {@code details[0]}. */
    public static class SolicitudPushInvalidaException extends RuntimeException {
        private final String codigo;

        public SolicitudPushInvalidaException(String codigo, String mensaje) {
            super(mensaje);
            this.codigo = codigo;
        }

        public String codigo() {
            return codigo;
        }
    }
}
