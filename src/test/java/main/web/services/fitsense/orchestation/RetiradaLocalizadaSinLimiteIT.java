package main.web.services.fitsense.orchestation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static main.web.services.fitsense.profiling.domain.model.valueobjects.FitnessLevel.INTERMEDIATE;
import static main.web.services.fitsense.profiling.domain.model.valueobjects.GoalType.GENERAL_FITNESS;
import static org.junit.jupiter.api.Assertions.*;

/**
 * La retirada pedida con el limite APAGADO (A). DECISION DEL RESPONSABLE (cierre del PR #39): la retirada no es
 * independiente del limite, asi que aqui queda INACTIVA aunque se pida; y SUST-SEG-1, sin limite, encuentra sustituto
 * (780).
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.adaptation.difficulty-guard=off",
        "fitsense.adaptation.pain-localized-removal=on"
})
@DisplayName("Retirada localizada pedida sin limite: queda inactiva y el respaldo es el de siempre")
class RetiradaLocalizadaSinLimiteIT extends RetiradaLocalizadaBase {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Test
    @DisplayName("R0521 con A: SUST-SEG-1 pone el 780; ninguna nota de retirada")
    void conSustitutoNoSeRetira() {
        assertTrue(producer.retiradaSolicitada(), "se pidio por configuracion");
        assertFalse(producer.retiradaLocalizada(), "pero con el limite apagado no actua");
        var e = escenario("R0521", GENERAL_FITNESS, INTERMEDIATE, 6, 30);
        assertEquals("RESPALDO", e.v32().get("status"));
        assertFalse(String.valueOf(e.v32().get("informe")).contains("RETIRADA_SEGURA"));
        assertTrue(ejerciciosDe(e.plan().orElseThrow()).contains(780L));
        assertTrue(e.p03().isEmpty());
    }
}
