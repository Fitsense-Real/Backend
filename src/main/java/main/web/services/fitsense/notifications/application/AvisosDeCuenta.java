package main.web.services.fitsense.notifications.application;

import main.web.services.fitsense.notifications.domain.TransportePush;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.UUID;

/**
 * Avisos de cuenta (ACCOUNT_NOTICE). Se escriben en la bandeja de salida DENTRO de la transaccion que cambia la cuenta:
 * si el cambio no se confirma, el aviso tampoco existe; el emisor solo ve filas confirmadas.
 * <p>
 * Con {@code fitsense.push.enabled=false} o sin credenciales validas no se registra nada.
 * <p>
 * Hoy el unico aviso es PASSWORD_CHANGED. Va a los dispositivos registrados con la version de credenciales ANTERIOR
 * (los de la persona antes del cambio): si no fue ella, se entera en su telefono. Despues esos dispositivos dejan de
 * recibir hasta registrarse con una sesion nueva.
 */
@Component
public class AvisosDeCuenta {

    private final JdbcTemplate jdbc;
    private final TransportePush transporte;
    private final boolean activado;
    private final Clock reloj;

    public AvisosDeCuenta(JdbcTemplate jdbc, TransportePush transporte, @Value("${fitsense.push.enabled:false}") boolean activado,
                          ObjectProvider<Clock> reloj) {
        this.jdbc = jdbc;
        this.transporte = transporte;
        this.activado = activado;
        this.reloj = reloj.getIfAvailable(Clock::systemUTC);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void contrasenaCambiada(long userId, int versionAnterior, int versionNueva) {
        // Con el envio apagado no se guarda nada: no se acumulan datos que nadie va a enviar.
        if (!activado || !transporte.disponible()) return;
        var ahora = Timestamp.from(reloj.instant());
        jdbc.update("INSERT INTO notification_events (event_id, user_id, event_type, notice_code, target_screen, source,"
                        + " dedupe_key, target_token_version, status, attempts, next_attempt_at, created_at, recorded_at)"
                        + " VALUES (?, ?, 'ACCOUNT_NOTICE', 'PASSWORD_CHANGED', 'ACCOUNT_SECURITY', 'password_reset',"
                        + " ?, ?, 'PENDIENTE', 0, ?, ?, ?) ON CONFLICT (dedupe_key) DO NOTHING",
                UUID.randomUUID(), userId, "ACCOUNT:PASSWORD_CHANGED:" + userId + ":" + versionNueva, versionAnterior,
                ahora, ahora, ahora);
    }
}
