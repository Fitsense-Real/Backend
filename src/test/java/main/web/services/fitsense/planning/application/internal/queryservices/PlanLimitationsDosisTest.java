package main.web.services.fitsense.planning.application.internal.queryservices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Fase 5: la dosis limitada de la traza llega a /plan/limitations")
class PlanLimitationsDosisTest {

    @Test
    void limitada() {
        var l = PlanLimitationsService.dosis("{\"politica\":\"PLAN-REV-2.0\",\"dosis\":{\"estado\":\"DOSIS_LIMITADA\","
                + "\"grupos_bajo_objetivo\":[\"shoulders\"],\"aerobico_trabajo_minutos\":72,\"aerobico_objetivo_minutos\":150}}");
        assertTrue(l.isPresent());
        assertEquals("DOSE", l.get().type());
        assertTrue(l.get().message().contains("hombros") && l.get().message().contains("72 de 150"), l.get().message());
        assertFalse(l.get().message().toLowerCase().contains("perder"), "no promete resultados");
    }

    @Test
    void sinAviso() {
        assertTrue(PlanLimitationsService.dosis("{\"dosis\":{\"estado\":\"DOSIS_OBJETIVO\"}}").isEmpty());
        assertTrue(PlanLimitationsService.dosis("{\"politica\":\"PLAN-HIST-1\",\"sesiones\":[]}").isEmpty(),
                "la traza historica no tiene dosis");
        assertTrue(PlanLimitationsService.dosis(null).isEmpty());
        assertTrue(PlanLimitationsService.dosis("no json").isEmpty());
    }
}
