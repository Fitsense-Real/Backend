package main.web.services.fitsense.notifications.application;

import main.web.services.fitsense.notifications.domain.MensajePush;
import main.web.services.fitsense.notifications.domain.TipoEventoPush;
import main.web.services.fitsense.notifications.domain.TransportePush;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Envia los eventos de la bandeja de salida. Corre en su propio hilo programado y FUERA de cualquier transaccion de
 * negocio: lo que pase con Firebase nunca afecta a la generacion, la adaptacion ni a ningun otro proceso.
 * <p>
 * Por cada evento, en el momento del envio (no al crearlo):
 * <ol>
 *   <li>caduca si es mas antiguo que el TTL (por omision 24 h);</li>
 *   <li>se descarta si la cuenta no esta ACTIVE (eliminacion en curso, inactiva), si la persona desactivo esa categoria
 *       o si el plan ya no esta vigente (reemplazado, ajeno o inexistente);</li>
 *   <li>va solo a dispositivos activos de esa cuenta, de la app del contrato y con la version de credenciales vigente
 *       (o, para un aviso de cambio de contrasena, la anterior);</li>
 *   <li>no repite un envio que FCM ya acepto para ese dispositivo.</li>
 * </ol>
 * UNREGISTERED desvincula el dispositivo. Los fallos transitorios se reintentan con espera creciente, como mucho
 * {@code max-attempts} veces.
 */
@Component
public class EmisorPush {

    private static final Logger log = LoggerFactory.getLogger(EmisorPush.class);

    private final JdbcTemplate jdbc;
    private final TransportePush transporte;
    private final PreferenciasNotificacionService preferencias;
    private final DispositivosPushService dispositivos;
    private final String paqueteAndroid;
    private final int lote;
    private final int maximoIntentos;
    private final Duration ttl;
    private final Duration lease;
    private final Clock reloj;

    public EmisorPush(JdbcTemplate jdbc, TransportePush transporte, PreferenciasNotificacionService preferencias,
                      DispositivosPushService dispositivos,
                      @Value("${fitsense.push.android.package:pe.fit.kora}") String paqueteAndroid,
                      @Value("${fitsense.push.batch-size:50}") int lote,
                      @Value("${fitsense.push.max-attempts:5}") int maximoIntentos,
                      @Value("${fitsense.push.ttl-hours:24}") long ttlHoras,
                      @Value("${fitsense.push.lease-seconds:120}") long leaseSegundos,
                      ObjectProvider<Clock> reloj) {
        if (lote < 1 || maximoIntentos < 1 || ttlHoras < 1 || leaseSegundos < 30)
            throw new IllegalArgumentException("Configuracion de fitsense.push.* fuera de rango.");
        this.jdbc = jdbc;
        this.transporte = transporte;
        this.preferencias = preferencias;
        this.dispositivos = dispositivos;
        this.paqueteAndroid = paqueteAndroid;
        this.lote = lote;
        this.maximoIntentos = maximoIntentos;
        this.ttl = Duration.ofHours(ttlHoras);
        this.lease = Duration.ofSeconds(leaseSegundos);
        this.reloj = reloj.getIfAvailable(Clock::systemUTC);
    }

    record Evento(UUID id, long userId, TipoEventoPush tipo, String noticeCode, String resourceType, Long resourceId,
                  Integer versionDestino, int intentos, Instant creadoEn) {}

    record Destino(long deviceId, String installationId) {}

    @Scheduled(initialDelayString = "${fitsense.push.sender-initial-delay-ms:40000}",
            fixedDelayString = "${fitsense.push.sender-delay-ms:15000}")
    public void programado() {
        if (!transporte.disponible()) return;
        try {
            procesar();
        } catch (RuntimeException e) {
            log.warn("Pasada del emisor push fallida; se reintenta: {}", e.getClass().getSimpleName());
        }
    }

    /** Procesa un lote. Devuelve cuantos eventos reclamo. */
    public int procesar() {
        if (!transporte.disponible()) return 0;
        var eventos = reclamar();
        for (var e : eventos) {
            try {
                procesar(e);
            } catch (RuntimeException ex) {
                // El evento queda EN_ENVIO y se reclama de nuevo al vencer el lease.
                log.warn("Evento push {} no procesado: {}", e.id(), ex.getClass().getSimpleName());
            }
        }
        return eventos.size();
    }

    private List<Evento> reclamar() {
        var ahora = reloj.instant();
        return jdbc.query("""
                UPDATE notification_events SET status = 'EN_ENVIO', lease_until = ?, attempts = attempts + 1
                 WHERE event_id IN (SELECT event_id FROM notification_events
                                     WHERE (status = 'PENDIENTE' AND next_attempt_at <= ?)
                                        OR (status = 'EN_ENVIO' AND lease_until < ?)
                                     ORDER BY next_attempt_at LIMIT ? FOR UPDATE SKIP LOCKED)
                RETURNING event_id, user_id, event_type, notice_code, resource_type, resource_id,
                          target_token_version, attempts, created_at""", (rs, n) -> new Evento(
                        rs.getObject(1, UUID.class), rs.getLong(2), TipoEventoPush.valueOf(rs.getString(3)),
                        rs.getString(4), rs.getString(5), (Long) rs.getObject(6, Long.class),
                        (Integer) rs.getObject(7, Integer.class), rs.getInt(8), rs.getTimestamp(9).toInstant()),
                Timestamp.from(ahora.plus(lease)), Timestamp.from(ahora), Timestamp.from(ahora), lote);
    }

    private void procesar(Evento e) {
        var ahora = reloj.instant();
        if (e.creadoEn().plus(ttl).isBefore(ahora)) {
            terminar(e, "CADUCADO", "MAS_ANTIGUO_QUE_TTL");
            return;
        }
        var cuenta = jdbc.queryForList("SELECT status, token_version FROM users WHERE user_id = ?", e.userId());
        if (cuenta.isEmpty() || !"ACTIVE".equals(cuenta.get(0).get("status"))) {
            terminar(e, "DESCARTADO", "CUENTA_NO_ACTIVA");
            return;
        }
        if (!preferencias.leer(e.userId()).permite(e.tipo().categoria())) {
            terminar(e, "DESCARTADO", "PREFERENCIA_DESACTIVADA");
            return;
        }
        if ("PLAN".equals(e.resourceType()) && !planVigente(e.resourceId(), e.userId())) {
            terminar(e, "DESCARTADO", "PLAN_NO_VIGENTE");
            return;
        }
        int version = e.versionDestino() != null ? e.versionDestino()
                : ((Number) cuenta.get(0).get("token_version")).intValue();
        var destinos = jdbc.query("""
                SELECT d.device_id, d.installation_id FROM push_devices d
                 WHERE d.user_id = ? AND d.revoked_at IS NULL AND d.token_version = ? AND d.app_id = ?
                   AND NOT EXISTS (SELECT 1 FROM notification_deliveries x WHERE x.event_id = ?
                                      AND x.device_id = d.device_id AND x.status IN ('ENVIADO', 'ERROR_PERMANENTE'))
                 ORDER BY d.device_id""", (rs, n) -> new Destino(rs.getLong(1), rs.getString(2)),
                e.userId(), version, paqueteAndroid, e.id());
        int enviados = enviadosPrevios(e.id());
        int temporales = 0;
        var mensaje = MensajePush.para(e.tipo(), e.id(), e.noticeCode(), e.resourceType(), e.resourceId(), e.creadoEn());
        for (var d : destinos) {
            var r = transporte.enviar(d.installationId(), mensaje);
            switch (r.tipo()) {
                case ENVIADO -> {
                    registrar(e, d, "ENVIADO", r.messageId(), null);
                    enviados++;
                }
                case NO_REGISTRADO -> {
                    registrar(e, d, "ERROR_PERMANENTE", null, r.codigoError());
                    dispositivos.marcarNoRegistrado(d.deviceId());
                }
                case ERROR_PERMANENTE -> registrar(e, d, "ERROR_PERMANENTE", null, r.codigoError());
                case ERROR_TEMPORAL -> {
                    registrar(e, d, "ERROR_TEMPORAL", null, r.codigoError());
                    temporales++;
                }
            }
        }
        if (temporales > 0 && e.intentos() < maximoIntentos) {
            reprogramar(e, ahora.plus(espera(e.intentos())));
        } else if (enviados > 0) {
            terminar(e, "ENVIADO", null);
        } else if (destinos.isEmpty()) {
            terminar(e, "SIN_DESTINO", "SIN_DISPOSITIVOS_ACTIVOS");
        } else {
            terminar(e, "FALLIDO", temporales > 0 ? "REINTENTOS_AGOTADOS" : "DESTINOS_RECHAZADOS");
        }
    }

    /** 30 s, 60 s, 2 min, 4 min… con tope de 30 min. */
    static Duration espera(int intentos) {
        long segundos = 30L << Math.min(Math.max(intentos - 1, 0), 6);
        return Duration.ofSeconds(Math.min(segundos, 1800));
    }

    private boolean planVigente(Long planId, long userId) {
        var filas = jdbc.queryForList("SELECT 1 FROM weekly_training_plans WHERE plan_id = ? AND user_id = ?"
                + " AND status <> 'REPLACED'", planId, userId);
        return !filas.isEmpty();
    }

    private int enviadosPrevios(UUID evento) {
        Integer n = jdbc.queryForObject("SELECT count(*) FROM notification_deliveries WHERE event_id = ?"
                + " AND status = 'ENVIADO'", Integer.class, evento);
        return n == null ? 0 : n;
    }

    private void registrar(Evento e, Destino d, String estado, String messageId, String codigo) {
        jdbc.update("INSERT INTO notification_deliveries (event_id, device_id, user_id, status, fcm_message_id,"
                        + " error_code, attempted_at) VALUES (?, ?, ?, ?, ?, ?, ?)", e.id(), d.deviceId(), e.userId(),
                estado, messageId, codigo == null ? null : truncar(codigo, 40), Timestamp.from(reloj.instant()));
    }

    private void reprogramar(Evento e, Instant cuando) {
        jdbc.update("UPDATE notification_events SET status = 'PENDIENTE', status_reason = 'REINTENTO',"
                        + " lease_until = NULL, next_attempt_at = ? WHERE event_id = ? AND status = 'EN_ENVIO'",
                Timestamp.from(cuando), e.id());
    }

    private void terminar(Evento e, String estado, String motivo) {
        jdbc.update("UPDATE notification_events SET status = ?, status_reason = ?, lease_until = NULL,"
                        + " finished_at = ? WHERE event_id = ? AND status = 'EN_ENVIO'",
                estado, motivo, Timestamp.from(reloj.instant()), e.id());
    }

    private static String truncar(String s, int n) {
        return s.length() <= n ? s : s.substring(0, n);
    }
}
