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
 * La retirada encendida con el limite APAGADO (A): como SUST-SEG-1 encuentra sustituto (780), la retirada no se usa.
 * Solo actua cuando no hay sustituto compatible.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.adaptation.difficulty-guard=off",
        "fitsense.adaptation.pain-localized-removal=on"
})
@DisplayName("Retirada localizada encendida sin limite: si hay sustituto, se sustituye y no se retira nada")
class RetiradaLocalizadaSinLimiteIT extends RetiradaLocalizadaBase {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Test
    @DisplayName("R0521 con A: SUST-SEG-1 pone el 780; ninguna nota de retirada")
    void conSustitutoNoSeRetira() {
        var e = escenario("R0521", GENERAL_FITNESS, INTERMEDIATE, 6, 30);
        assertEquals("RESPALDO", e.v32().get("status"));
        assertFalse(String.valueOf(e.v32().get("informe")).contains("RETIRADA_SEGURA"));
        assertTrue(ejerciciosDe(e.plan().orElseThrow()).contains(780L));
        assertTrue(e.p03().isEmpty());
    }
}
