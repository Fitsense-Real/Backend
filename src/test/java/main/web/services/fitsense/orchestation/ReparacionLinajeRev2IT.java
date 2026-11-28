package main.web.services.fitsense.orchestation;

import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * {@link ReparacionLinajeIT} con {@code PLAN-REV-2.0} (cierre tecnico, fases 3 y 7: adaptacion publicada -> reparacion
 * de seguridad). Sin cambiar ninguna asercion: la semana progresada se repara por version nueva (P-P2), la version
 * reparada conserva la politica de la que reemplaza y la siguiente hereda el paso por ella (H6).
 */
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.adaptation.progression.enabled=true",
        "fitsense.planning.policy=PLAN-REV-2.0",
        "fitsense.planning.policy.allow-candidates=true"
})
@DisplayName("REV2 · P-P2 y H6: reparar una semana progresada y seguir el linaje por la version reparada")
class ReparacionLinajeRev2IT extends ReparacionLinajeIT {
}
