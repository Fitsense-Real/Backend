package main.web.services.fitsense.adaptation.application.internal.commandservices;

import main.web.services.fitsense.adaptation.domain.model.aggregates.UserIntervention;
import main.web.services.fitsense.shared.domain.model.valueobjects.SkipReason;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * P-T2: el texto para la persona va a message_shown y el motivo TECNICO de la orden sigue siendo el reason de la
 * decision, que es lo que cruza a planning (WeeklyCycleService.toAdjustment usa order.reason(), no message) y al
 * snapshot de la IA. Asi el cambio de redaccion no toca la entrada de Replicate ni ninguna decision.
 */
@DisplayName("P-T2: message_shown lleva la intencion; el reason tecnico no cambia")
class MensajeIntencionDecisionTest {

    static final String TECNICO = "Adherencia observada 66.67 % (cobertura 100.00 %). Entre 50 % y 80 %: se corrige solo"
            + " la peor sesion.";

    @Test
    @DisplayName("reason = motivo tecnico; message = intencion; la intervencion guarda la intencion")
    void separados() {
        var decision = UserInterventionCommandServiceImpl.sinAjustePorcentual(TECNICO, "Revisaremos esa sesión.");
        assertEquals(TECNICO, decision.reason());
        assertEquals("Revisaremos esa sesión.", decision.message());
        var i = new UserIntervention(7L, 300L, 400L, new BigDecimal("66.67"), SkipReason.TOO_DIFFICULT, decision,
                120, 3, 45, 2, "v6");
        assertEquals("Revisaremos esa sesión.", i.getMessageShown());
    }

    @Test
    @DisplayName("Sin texto para la persona, se comporta como antes (message = motivo)")
    void comoAntes() {
        assertEquals(TECNICO, UserInterventionCommandServiceImpl.sinAjustePorcentual(TECNICO, null).message());
        assertEquals(TECNICO, UserInterventionCommandServiceImpl.sinAjustePorcentual(TECNICO).message());
        assertEquals(UserInterventionCommandServiceImpl.sinAjustePorcentual(TECNICO).reason(),
                UserInterventionCommandServiceImpl.sinAjustePorcentual(TECNICO, "x").reason());
    }
}
