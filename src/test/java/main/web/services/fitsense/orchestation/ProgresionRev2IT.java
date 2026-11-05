package main.web.services.fitsense.orchestation;

import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * {@link ProgresionIT} (progresion ENCENDIDA) con {@code PLAN-REV-2.0} (cierre tecnico, fase 3). Las 24 pruebas se
 * ejecutan sin cambiar ninguna asercion: con REV2 ninguna depende de una premisa exclusivamente historica (medido el
 * 05/11/2026: 24/24 con REV2). Fija en la suite permanente lo que antes solo se comprobaba con una propiedad de sistema:
 * dos semanas cumplidas al 100 % -> un paso; copia del paso; techo 1,10; fatiga -> reduccion o vuelta atras, nunca
 * subida; dolor manda; P-02/P-03 con progresion; reincorporacion solo tras una PROGRESION posterior al acortado.
 */
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.adaptation.progression.enabled=true",
        "fitsense.planning.policy=PLAN-REV-2.0",
        "fitsense.planning.policy.allow-candidates=true"
})
@DisplayName("REV2 · progresion encendida (bateria de ProgresionIT)")
class ProgresionRev2IT extends ProgresionIT {
}
