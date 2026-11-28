package main.web.services.fitsense.auditoria;

import main.web.services.fitsense.planning.infrastructure.generation.ai.ReplicateProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AUDITORIA DE ACTIVACION (11/11/2026) · con la IA encendida por defecto (PR #45), un entorno sin
 * REPLICATE_API_TOKEN no puede intentar llamar al proveedor: {@code isUsable()} es falso, la generacion usa el
 * motor de reglas y la adaptacion sigue el camino sin IA (conservar o respaldo seguro). Sin red.
 */
@DisplayName("Auditoria · IA encendida sin token: no utilizable, sin llamadas")
class SinTokenReplicateTest {

    private static ReplicateProperties con(boolean enabled, String token) {
        return new ReplicateProperties(enabled, token, null, "gpt-5-mini", "medium", 150, 60);
    }

    @Test
    void encendidaSinTokenNoEsUtilizable() {
        assertFalse(con(true, null).isUsable());
        assertFalse(con(true, "").isUsable());
        assertFalse(con(true, "   ").isUsable());
    }

    @Test
    void apagadaNoEsUtilizableAunqueHayaToken() {
        assertFalse(con(false, "marcador-no-real").isUsable(), "FITSENSE_AI_ENABLED=false es el interruptor de emergencia");
    }

    @Test
    void encendidaConTokenEsUtilizableYConservaLosLimites() {
        var p = con(true, "marcador-no-real");
        assertTrue(p.isUsable());
        assertEquals(150, p.timeoutSeconds());
        assertEquals(60, p.preferWaitSeconds());
        assertEquals(150, new ReplicateProperties(true, "x", null, null, null, 0, 0).timeoutSeconds(),
                "sin valor, el limite vuelve a 150 s");
    }
}
