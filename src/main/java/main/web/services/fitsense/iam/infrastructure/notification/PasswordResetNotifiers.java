package main.web.services.fitsense.iam.infrastructure.notification;

import main.web.services.fitsense.iam.application.internal.outboundservices.notification.PasswordResetNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.time.OffsetDateTime;

/**
 * Canales de entrega del codigo de recuperacion, elegidos con {@code fitsense.password-reset.delivery}:
 * <ul>
 *   <li>{@code none} (por defecto): no se envia nada. El flujo queda NO operativo para usuarios reales.</li>
 *   <li>{@code dev-log}: SOLO DESARROLLO LOCAL. Escribe el codigo en el log para poder probar el flujo de punta a
 *       punta sin proveedor de correo. Nunca en un entorno con usuarios: el log no es un canal seguro. Por eso
 *       solo se acepta con el perfil {@code local} o {@code local-dev}: fuera de ellos la aplicacion NO ARRANCA
 *       (fallar visible es mejor que caer en silencio a {@code none} o, peor, escribir codigos en un log remoto).</li>
 * </ul>
 * Ninguno registra el correo ni, en {@code none}, el codigo.
 */
@Configuration
public class PasswordResetNotifiers {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetNotifiers.class);

    /** Unicos perfiles en los que el codigo puede ir al log. */
    static final String PERFILES_LOCALES = "local | local-dev";

    @Bean
    @ConditionalOnProperty(name = "fitsense.password-reset.delivery", havingValue = "dev-log")
    PasswordResetNotifier registroDeDesarrollo(Environment entorno) {
        if (!entorno.matchesProfiles(PERFILES_LOCALES))
            throw new IllegalStateException("fitsense.password-reset.delivery=dev-log solo se admite con el perfil"
                    + " local o local-dev (escribe el codigo de recuperacion en el log). Perfiles activos: "
                    + String.join(",", entorno.getActiveProfiles()) + ". Usa none o un proveedor de correo.");
        return new PasswordResetNotifier() {
            @Override
            public void enviar(String email, String token, OffsetDateTime caduca) {
                log.warn("RECUPERACION (SOLO DESARROLLO, fitsense.password-reset.delivery=dev-log): codigo {} valido"
                        + " hasta {}. No usar este modo con usuarios reales.", token, caduca);
            }

            @Override
            public boolean entregaReal() {
                return false;
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean(PasswordResetNotifier.class)
    PasswordResetNotifier sinEntrega() {
        return new PasswordResetNotifier() {
            @Override
            public void enviar(String email, String token, OffsetDateTime caduca) {
                log.info("RECUPERACION: solicitud registrada, pero no hay proveedor de correo configurado: no se"
                        + " envia nada (fitsense.password-reset.delivery=none).");
            }

            @Override
            public boolean entregaReal() {
                return false;
            }
        };
    }
}
