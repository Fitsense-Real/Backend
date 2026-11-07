package main.web.services.fitsense.analytics.domain.services;

import main.web.services.fitsense.analytics.domain.model.valueobjects.MetricsCalculation;
import main.web.services.fitsense.analytics.domain.model.valueobjects.WeekPlanInput;
import main.web.services.fitsense.analytics.domain.model.valueobjects.WeekSessionInput;
import main.web.services.fitsense.configuration.domain.model.valueobjects.CalculationParams;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * S-MAX y adherencia: la metrica primaria se mide contra las sesiones PROGRAMADAS, no contra los dias disponibles. Un
 * principiante con 5 dias disponibles recibe 4 sesiones: completar las 4 es el 100 %; hacer 3 de 4 es el 75 % (no el
 * 60 % que daria dividir por los 5 dias disponibles).
 */
class AdherenciaSesionesProgramadasTest {

    private static final LocalDate LUNES = LocalDate.of(2026, 11, 2);
    private final WeeklyMetricsCalculator calculator = new WeeklyMetricsCalculator();

    private MetricsCalculation calcular(List<WeekPlanInput.ScheduledWorkout> previstos, List<WeekSessionInput> sesiones) {
        return calculator.calculate(new WeekPlanInput(7L, (short) 1, previstos), sesiones,
                BigDecimal.valueOf(75), (short) 4, 450, 150, 0, params());
    }

    private static List<WeekPlanInput.ScheduledWorkout> cuatro() {
        return List.of(previsto(1, 0, "COMPLETED"), previsto(2, 1, "COMPLETED"), previsto(3, 3, "COMPLETED"),
                previsto(4, 4, "COMPLETED"));
    }

    @Test
    @DisplayName("4 programadas (de 5 disponibles) y las 4 completas: 100 %")
    void cuatroDeCuatro() {
        var m = calcular(cuatro(), List.of(sesion(1, 100), sesion(2, 100), sesion(3, 100), sesion(4, 100)));
        assertEquals(4, m.scheduledWorkouts());
        assertEquals(new BigDecimal("100.00"), m.weightedAdherencePct());
        assertEquals(new BigDecimal("100.00"), m.registrationCoveragePct());
    }

    @Test
    @DisplayName("3 de las 4 programadas: 75 % (denominador = programadas, no disponibles)")
    void tresDeCuatro() {
        var m = calcular(cuatro(), List.of(sesion(1, 100), sesion(2, 100), sesion(3, 100)));
        assertEquals(new BigDecimal("75.00"), m.weightedAdherencePct());
    }

    private static WeekPlanInput.ScheduledWorkout previsto(long id, int diaOffset, String estado) {
        return new WeekPlanInput.ScheduledWorkout(id, LUNES.plusDays(diaOffset), 45, 5,
                estado, "SKIPPED".equals(estado) ? "LACK_OF_TIME" : null);
    }

    private static WeekSessionInput sesion(long id, double cumplimientoPct) {
        return new WeekSessionInput(id, BigDecimal.valueOf(cumplimientoPct), (short) 40,
                (short) 6, (short) 4, (int) Math.round(5 * cumplimientoPct / 100.0),
                300, 100, 0, null);
    }

    private static CalculationParams params() {
        var adherence = new CalculationParams.Adherence(80.0, 30.0, 80.0, 100.0,
                "weighted_adherence_pct", "LAST_FINISHED", 7);
        var adjustment = new CalculationParams.Adjustment(80.0, 50.0, 20.0, 30.0, 10.0,
                5.0, 5.0, 15.0, 20.0, 40.0, 5.0, 1, 2, 20, 2, 2, 6, 20, 1, 1, 20.0, 8.0);
        var risk = new CalculationParams.Risk(
                List.of(new CalculationParams.Risk.AdherencePoints(50.0, 30)),
                List.of(new CalculationParams.Risk.SkipPoints(2, 20)),
                List.of(new CalculationParams.Risk.DropPoints(15.0, 15)),
                10, 8.0, new CalculationParams.Risk.Levels(0.0, 25.0, 50.0, 75.0));
        return new CalculationParams(adherence, adjustment, risk,
                new CalculationParams.Dropout(14), null);
    }
}
