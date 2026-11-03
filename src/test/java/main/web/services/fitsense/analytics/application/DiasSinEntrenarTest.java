package main.web.services.fitsense.analytics.application;

import main.web.services.fitsense.analytics.application.internal.outboundservices.acl.ExternalExecutionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Dias sin entrenar contados en el dia LOCAL del participante, no en UTC. */
class DiasSinEntrenarTest {

    static final ZoneId LIMA = ZoneId.of("America/Lima");
    static final LocalDate CIERRE = LocalDate.of(2026, 11, 22); // domingo

    @Test
    @DisplayName("domingo 23:50 en Lima (lunes 04:50 UTC): 14 dias, no 13")
    void nocheDelDomingo() {
        var at = OffsetDateTime.parse("2026-11-09T04:50:00Z");
        assertEquals(14, ExternalExecutionService.diasHasta(at, CIERRE, LIMA));
    }

    @Test
    @DisplayName("lunes 00:10 en Lima: 13 dias")
    void madrugadaDelLunes() {
        var at = OffsetDateTime.parse("2026-11-09T05:10:00Z");
        assertEquals(13, ExternalExecutionService.diasHasta(at, CIERRE, LIMA));
    }

    @Test
    @DisplayName("el mismo instante en Tokio ya es lunes por la tarde: 13 dias")
    void otraZona() {
        var at = OffsetDateTime.parse("2026-11-09T04:50:00Z");
        assertEquals(13, ExternalExecutionService.diasHasta(at, CIERRE, ZoneId.of("Asia/Tokyo")));
    }

    @Test
    @DisplayName("una sesion el mismo domingo del cierre: 0; nunca negativo")
    void mismoDia() {
        assertEquals(0, ExternalExecutionService.diasHasta(OffsetDateTime.parse("2026-11-22T23:00:00-05:00"),
                CIERRE, LIMA));
        assertEquals(0, ExternalExecutionService.diasHasta(OffsetDateTime.parse("2026-11-25T10:00:00-05:00"),
                CIERRE, LIMA));
    }
}
