package main.web.services.fitsense.notifications.application;

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

/**
 * Convierte en eventos de la bandeja de salida los planes que la generacion y el cierre semanal YA publicaron.
 * <p>
 * <b>Por que se detecta y no se publica desde la generacion o la adaptacion:</b> la orden prohibe modificar esos
 * procesos, y leer las filas confirmadas garantiza por construccion que solo se notifica lo que esta persistido. La
 * generacion y la adaptacion no saben que existe Firebase: un fallo de FCM no las puede revertir ni interrumpir.
 * <ul>
 *   <li>PLAN_GENERATED: {@code plan_generation_operations} en COMPLETED (siempre con plan, CHECK de V58).</li>
 *   <li>PLAN_ADAPTED: {@code plan_adaptation_attempts} con plan resultante y estado APLICADA.</li>
 *   <li>WEEKLY_PLAN_READY: el mismo registro con SIN_ORDEN, SIN_EFECTO o RESPALDO (continuidad publicada).
 *       RECHAZADA y NO_FACTIBLE no publican plan y no se notifican.</li>
 * </ul>
 * <b>Antiduplicado:</b> la clave es {@code PLAN:<plan_id>} (UNIQUE): un plan se notifica una sola vez aunque varios
 * registros lo citen, y si uno de ellos es APLICADA gana PLAN_ADAPTED. Un plan ya REEMPLAZADO no se notifica.
 * <p>
 * Solo mira hacia atras una ventana acotada (por omision 6 h): al activar la funcion no se notifica el historial.
 */
@Component
public class DetectorEventosPush {

    private static final Logger log = LoggerFactory.getLogger(DetectorEventosPush.class);

    static final String GENERADOS = """
            INSERT INTO notification_events (event_id, user_id, event_type, resource_type, resource_id, target_screen,
                                             source, dedupe_key, status, attempts, next_attempt_at, created_at, recorded_at)
            SELECT gen_random_uuid(), o.user_id, 'PLAN_GENERATED', 'PLAN', o.plan_id, 'WEEKLY_PLAN',
                   'plan_generation_operations', 'PLAN:' || o.plan_id, 'PENDIENTE', 0, ?, o.finished_at, ?
              FROM plan_generation_operations o
              JOIN weekly_training_plans p ON p.plan_id = o.plan_id AND p.status <> 'REPLACED'
              JOIN users u ON u.user_id = o.user_id AND u.status = 'ACTIVE'
             WHERE o.status = 'COMPLETED' AND o.finished_at >= ?
            ON CONFLICT (dedupe_key) DO NOTHING""";

    static final String SEMANALES = """
            INSERT INTO notification_events (event_id, user_id, event_type, resource_type, resource_id, target_screen,
                                             source, dedupe_key, status, attempts, next_attempt_at, created_at, recorded_at)
            SELECT DISTINCT ON (a.resulting_plan_id)
                   gen_random_uuid(), a.user_id,
                   CASE WHEN a.status = 'APLICADA' THEN 'PLAN_ADAPTED' ELSE 'WEEKLY_PLAN_READY' END,
                   'PLAN', a.resulting_plan_id, 'WEEKLY_PLAN', 'plan_adaptation_attempts',
                   'PLAN:' || a.resulting_plan_id, 'PENDIENTE', 0, ?, a.created_at, ?
              FROM plan_adaptation_attempts a
              JOIN weekly_training_plans p ON p.plan_id = a.resulting_plan_id AND p.status <> 'REPLACED'
                                          AND p.user_id = a.user_id
              JOIN users u ON u.user_id = a.user_id AND u.status = 'ACTIVE'
             WHERE a.resulting_plan_id IS NOT NULL
               AND a.status IN ('APLICADA', 'SIN_EFECTO', 'RESPALDO', 'SIN_ORDEN')
               AND a.created_at >= ?
             ORDER BY a.resulting_plan_id, (a.status = 'APLICADA') DESC, a.attempt_id DESC
            ON CONFLICT (dedupe_key) DO NOTHING""";

    private final JdbcTemplate jdbc;
    private final boolean activado;
    private final Duration ventana;
    private final Clock reloj;

    public DetectorEventosPush(JdbcTemplate jdbc,
                               @Value("${fitsense.push.enabled:false}") boolean activado,
                               @Value("${fitsense.push.detection-window-hours:6}") long horas,
                               ObjectProvider<Clock> reloj) {
        if (horas < 1 || horas > 48)
            throw new IllegalArgumentException("fitsense.push.detection-window-hours debe estar entre 1 y 48.");
        this.jdbc = jdbc;
        this.activado = activado;
        this.ventana = Duration.ofHours(horas);
        this.reloj = reloj.getIfAvailable(Clock::systemUTC);
    }

    @Scheduled(initialDelayString = "${fitsense.push.detector-initial-delay-ms:30000}",
            fixedDelayString = "${fitsense.push.detector-delay-ms:30000}")
    public void programado() {
        if (!activado) return;
        try {
            detectar();
        } catch (RuntimeException e) {
            log.warn("Deteccion de eventos push fallida; se reintenta en la siguiente pasada: {}",
                    e.getClass().getSimpleName());
        }
    }

    /** Cuantos eventos nuevos se registraron. Idempotente. */
    public int detectar() {
        var ahora = Timestamp.from(reloj.instant());
        var desde = Timestamp.from(reloj.instant().minus(ventana));
        // Primero el cierre semanal: si un mismo plan apareciera en las dos fuentes, el tipo lo decide la adaptacion.
        int n = jdbc.update(SEMANALES, ahora, ahora, desde);
        n += jdbc.update(GENERADOS, ahora, ahora, desde);
        return n;
    }
}
