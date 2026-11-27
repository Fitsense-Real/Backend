package main.web.services.fitsense.shared.infrastructure.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Fija {@link DetalleSensible} al arrancar. {@code fitsense.logging.sensitive-detail=true} con el perfil {@code prod}
 * impide arrancar: fallar visible es mejor que escribir datos de salud en un log remoto (el mismo criterio que
 * {@code fitsense.password-reset.delivery=dev-log}).
 */
@Configuration
public class DetalleSensibleConfiguracion {

    private static final Logger log = LoggerFactory.getLogger(DetalleSensibleConfiguracion.class);

    public DetalleSensibleConfiguracion(@Value("${fitsense.logging.sensitive-detail:false}") boolean detalle,
                                        Environment entorno) {
        if (detalle && entorno.matchesProfiles("prod"))
            throw new IllegalStateException("fitsense.logging.sensitive-detail=true no se admite con el perfil prod:"
                    + " escribiria la salida de la IA y el dolor de las personas en los logs (S-1).");
        DetalleSensible.fijar(detalle);
        if (detalle)
            log.warn("LOGS_DETALLE_SENSIBLE activado (fitsense.logging.sensitive-detail=true): solo para datos"
                    + " sinteticos. Nunca con participantes reales.");
        else
            log.info("LOGS_DETALLE_SENSIBLE desactivado: la salida de la IA y el dolor se registran en forma tecnica");
    }
}
