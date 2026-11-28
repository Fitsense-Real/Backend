package main.web.services.fitsense.analytics.application.internal.outboundservices.acl;

import main.web.services.fitsense.iam.interfaces.acl.IamContextFacade;
import org.springframework.stereotype.Service;

import java.time.ZoneId;

/**
 * Capa anticorrupcion hacia IAM: la zona horaria del participante, que define
 * en que dia local cayo cada sesion.
 * <p>
 * Nombre de bean explicito: planning ya tiene un ExternalIamService.
 */
@Service("analyticsExternalIamService")
public class ExternalIamService {

    /**
     * Zona de referencia del MVP, la misma que usa planning. users.timezone es
     * NOT NULL con este valor por defecto (V1), asi que solo aplica si el
     * usuario no se encuentra.
     */
    public static final ZoneId ZONA_DE_REFERENCIA = ZoneId.of("America/Lima");

    private final IamContextFacade iamContextFacade;

    public ExternalIamService(IamContextFacade iamContextFacade) {
        this.iamContextFacade = iamContextFacade;
    }

    public ZoneId timezoneOf(Long userId) {
        return iamContextFacade.fetchTimezone(userId).orElse(ZONA_DE_REFERENCIA);
    }
}
