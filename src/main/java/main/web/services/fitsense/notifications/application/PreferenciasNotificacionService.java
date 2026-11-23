package main.web.services.fitsense.notifications.application;

import main.web.services.fitsense.notifications.domain.TipoEventoPush;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Clock;

/**
 * Preferencias por categoria. Sin fila, las dos categorias estan activadas (el permiso del sistema operativo sigue
 * siendo la puerta de entrada: sin permiso de notificaciones la app no se registra). Se consultan en el momento del
 * envio, no al crear el evento: desactivar una categoria detiene tambien lo que estaba pendiente.
 */
@Service
public class PreferenciasNotificacionService {

    private final JdbcTemplate jdbc;
    private final Clock reloj;

    public PreferenciasNotificacionService(JdbcTemplate jdbc, ObjectProvider<Clock> reloj) {
        this.jdbc = jdbc;
        this.reloj = reloj.getIfAvailable(Clock::systemUTC);
    }

    public record Preferencias(boolean planUpdates, boolean accountNotices) {
        public static final Preferencias POR_OMISION = new Preferencias(true, true);

        public boolean permite(TipoEventoPush.Categoria categoria) {
            return categoria == TipoEventoPush.Categoria.PLANES ? planUpdates : accountNotices;
        }
    }

    public Preferencias leer(long userId) {
        return jdbc.query("SELECT plan_updates, account_notices FROM notification_preferences WHERE user_id = ?",
                        (rs, n) -> new Preferencias(rs.getBoolean(1), rs.getBoolean(2)), userId)
                .stream().findFirst().orElse(Preferencias.POR_OMISION);
    }

    @Transactional
    public Preferencias guardar(long userId, Preferencias p) {
        jdbc.update("INSERT INTO notification_preferences (user_id, plan_updates, account_notices, updated_at)"
                        + " VALUES (?, ?, ?, ?) ON CONFLICT (user_id) DO UPDATE SET plan_updates = EXCLUDED.plan_updates,"
                        + " account_notices = EXCLUDED.account_notices, updated_at = EXCLUDED.updated_at",
                userId, p.planUpdates(), p.accountNotices(), Timestamp.from(reloj.instant()));
        return leer(userId);
    }
}
