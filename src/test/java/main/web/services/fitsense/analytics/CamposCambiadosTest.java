package main.web.services.fitsense.analytics;

import main.web.services.fitsense.analytics.domain.model.aggregates.WeeklyUserMetrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("C4 · campos cambiados entre dos instantaneas de una medicion")
class CamposCambiadosTest {

    static LinkedHashMap<String, Object> fila(Object adherencia, Object omitidas, Object riesgo) {
        var m = new LinkedHashMap<String, Object>();
        m.put("weighted_adherence_pct", adherencia);
        m.put("skipped_workouts", omitidas);
        m.put("risk_level", riesgo);
        return m;
    }

    @Test
    @DisplayName("igual valor con otra escala no es un cambio; un valor distinto o un null, si")
    void diferencias() {
        assertEquals(List.of(), WeeklyUserMetrics.camposCambiados(
                fila(new BigDecimal("12.50"), (short) 1, "LOW"), fila(new BigDecimal("12.5"), (short) 1, "LOW")));
        assertEquals(List.of("weighted_adherence_pct", "risk_level"), WeeklyUserMetrics.camposCambiados(
                fila(new BigDecimal("12.50"), (short) 1, "LOW"), fila(new BigDecimal("40.00"), (short) 1, null)));
        assertEquals(List.of("skipped_workouts"), WeeklyUserMetrics.camposCambiados(
                fila(null, (short) 0, null), fila(null, (short) 2, null)));
    }
}
