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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A_w = sum(A_s / 100) / m x 100: cada previsto pesa uno. Los esperados se
 * calculan a mano en cada prueba.
 */
class AdherenciaSinPesosTest {

    private static final LocalDate LUNES = LocalDate.of(2026, 10, 12);
    private final WeeklyMetricsCalculator calculator = new WeeklyMetricsCalculator();

    @Test
    @DisplayName("100, 50 y 0 en sesiones de 60, 45 y 30 minutos: 50, no 61,11")
    void sinPesoPorDuracion() {
        // Antes: (60x100 + 45x50 + 30x0) / 135 = 61,11. Ahora: (1 + 0,5 + 0) / 3 = 50.
        var m = calcular(List.of(previsto(1, 0, 60, "COMPLETED"), previsto(2, 2, 45, "PARTIAL"),
                        previsto(3, 4, 30, "SKIPPED")),
                List.of(sesion(1, 100), sesion(2, 50)));
        assertEquals(cifra("50.00"), m.weightedAdherencePct());
        // Observada: los tres tienen registro (el tercero es omision declarada).
        assertEquals(cifra("50.00"), m.observedAdherencePct());
        // Riesgo con la tabla de prueba (>= 50 -> 0; por debajo -> 30): 50 no puntua.
        // Con 61,11 tampoco puntuaba; la diferencia se ve en observadaSinPeso.
        assertFalse(m.riskFactors().containsKey("low_adherence"), m.riskFactors().toString());
    }

    @Test
    @DisplayName("la observada tampoco pondera: 100 y 25 con 60 y 20 minutos, sin registro el tercero -> 62,5")
    void observadaSinPeso() {
        // Primaria: (1 + 0,25 + 0) / 3 = 41,67. Observada: (1 + 0,25) / 2 = 62,5.
        var m = calcular(List.of(previsto(1, 0, 60, "COMPLETED"), previsto(2, 2, 20, "SKIPPED"),
                        previsto(3, 4, 45, "SCHEDULED")),
                List.of(sesion(1, 100), sesion(2, 25)));
        assertEquals(cifra("41.67"), m.weightedAdherencePct());
        assertEquals(cifra("62.50"), m.observedAdherencePct());
        // Puntos por baja adherencia: 41,67 < 50 -> 30.
        assertEquals(30, m.riskFactors().get("low_adherence"));
    }

    @Test
    @DisplayName("un cumplimiento por encima de 100 se topa: 150 y 0 -> 50")
    void tope() {
        var m = calcular(List.of(previsto(1, 0, 45, "COMPLETED"), previsto(2, 2, 45, "SKIPPED")),
                List.of(sesion(1, 150), sesion(2, 0)));
        assertEquals(cifra("50.00"), m.weightedAdherencePct());
    }

    @Test
    @DisplayName("versiones: el dia hecho en la 1 y repetido en la 2 cuenta una vez")
    void unoPorDia() {
        // v1 (plan 10): lunes COMPLETED con sesion, miercoles SKIPPED declarado.
        // v2 (plan 20): lunes y miercoles SCHEDULED repetidos, viernes con sesion al 50.
        // Quedan: lunes v1 (1), miercoles v1 (0), viernes v2 (0,5). (1 + 0 + 0,5) / 3 = 50.
        var semana = List.of(
                previsto(1, 0, 45, "COMPLETED", 10L), previsto(2, 2, 45, "SKIPPED", 10L),
                previsto(11, 0, 45, "SCHEDULED", 20L), previsto(12, 2, 45, "SCHEDULED", 20L),
                previsto(13, 4, 45, "PARTIAL", 20L));
        var m = calcular(semana, List.of(sesion(1, 100), sesion(13, 50)));
        assertEquals(3, m.scheduledWorkouts());
        assertEquals(cifra("50.00"), m.weightedAdherencePct());

        var quedan = new WeekPlanInput(20L, (short) 1, semana).unoPorDia(Set.of(1L, 13L)).workouts().stream()
                .map(WeekPlanInput.ScheduledWorkout::plannedWorkoutId).toList();
        assertEquals(List.of(1L, 2L, 13L), quedan);
    }

    @Test
    @DisplayName("versiones: si el dia repetido se hace en la version nueva, cuenta la nueva")
    void unoPorDiaGanaLaSesion() {
        var semana = List.of(previsto(1, 0, 45, "SKIPPED", 10L), previsto(11, 0, 45, "COMPLETED", 20L));
        var quedan = new WeekPlanInput(20L, (short) 1, semana).unoPorDia(Set.of(11L)).workouts();
        assertEquals(1, quedan.size());
        assertEquals(11L, quedan.get(0).plannedWorkoutId());
    }

    @Test
    @DisplayName("dentro de una misma version no se quita nada, aunque coincida el dia")
    void mismaVersionIntacta() {
        var semana = List.of(previsto(1, 0, 45, "SCHEDULED", 10L), previsto(2, 0, 45, "SCHEDULED", 10L));
        assertEquals(2, new WeekPlanInput(10L, (short) 1, semana).unoPorDia(Set.of()).workouts().size());
    }

    // ------------------------------------------------------------------ utilidades

    private MetricsCalculation calcular(List<WeekPlanInput.ScheduledWorkout> previstos,
                                        List<WeekSessionInput> sesiones) {
        return calculator.calculate(new WeekPlanInput(7L, (short) 2, previstos), sesiones,
                null, (short) 1, 450, 150, 0, params());
    }

    private static WeekPlanInput.ScheduledWorkout previsto(long id, int dia, int minutos, String estado) {
        return previsto(id, dia, minutos, estado, null);
    }

    private static WeekPlanInput.ScheduledWorkout previsto(long id, int dia, int minutos, String estado, Long plan) {
        return new WeekPlanInput.ScheduledWorkout(id, LUNES.plusDays(dia), minutos, 4, estado,
                "SKIPPED".equals(estado) ? "LACK_OF_TIME" : null, plan);
    }

    private static WeekSessionInput sesion(long id, double cumplimiento) {
        return new WeekSessionInput(id, BigDecimal.valueOf(cumplimiento), (short) 40, (short) 6, null,
                0, 0, 0, 0, null);
    }

    private static BigDecimal cifra(String valor) {
        return new BigDecimal(valor);
    }

    private static CalculationParams params() {
        var adherence = new CalculationParams.Adherence(80.0, 30.0, 80.0, 100.0,
                "weighted_adherence_pct", "LAST_FINISHED", 7);
        var adjustment = new CalculationParams.Adjustment(80.0, 50.0, 20.0, 30.0, 10.0,
                5.0, 5.0, 15.0, 20.0, 40.0, 5.0, 1, 2, 20, 2, 2, 6, 20, 1, 1, 20.0, 8.0);
        var risk = new CalculationParams.Risk(
                List.of(new CalculationParams.Risk.AdherencePoints(50.0, 0),
                        new CalculationParams.Risk.AdherencePoints(0.0, 30)),
                List.of(new CalculationParams.Risk.SkipPoints(2, 20)),
                List.of(new CalculationParams.Risk.DropPoints(15.0, 15)),
                10, 8.0, new CalculationParams.Risk.Levels(0.0, 25.0, 50.0, 75.0));
        return new CalculationParams(adherence, adjustment, risk, new CalculationParams.Dropout(14), null);
    }
}
