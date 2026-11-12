package main.web.services.fitsense.analytics.domain.services;

import main.web.services.fitsense.analytics.domain.model.valueobjects.WeekPlanInput;
import main.web.services.fitsense.analytics.domain.model.valueobjects.WeekSessionInput;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Un SKIPPED cuenta como registro SOLO si la persona dio la causa. La tarea de
 * vencimiento cierra como SKIPPED, sin causa, todo lo que no se registro.
 */
class OmisionDeclaradaTest {

    static final LocalDate LUNES = LocalDate.of(2026, 10, 12);
    final WeeklyMetricsCalculator calculator = new WeeklyMetricsCalculator();

    private static WeekPlanInput.ScheduledWorkout previsto(long id, int dia, String estado, String causa) {
        return new WeekPlanInput.ScheduledWorkout(id, LUNES.plusDays(dia), 45, 4, estado, causa);
    }

    @Test
    @DisplayName("1 hecho y 2 vencidos sin causa: cobertura 33,33, observada 100; la oficial sigue en 33,33")
    void vencidosNoSonRegistro() {
        var m = calculator.calculate(new WeekPlanInput(7L, (short) 2, List.of(
                        previsto(1, 0, "COMPLETED", null), previsto(2, 2, "SKIPPED", null),
                        previsto(3, 4, "SKIPPED", null))),
                List.of(new WeekSessionInput(1L, BigDecimal.valueOf(100), (short) 40, (short) 6, null,
                        4, 0, 0, 0, null)),
                null, (short) 1, 0, 0, 0, RiesgoEscenariosTest.params());
        assertEquals(1, m.recordedWorkouts());
        assertEquals(new BigDecimal("33.33"), m.registrationCoveragePct());
        assertEquals(new BigDecimal("100.00"), m.observedAdherencePct());
        assertEquals(new BigDecimal("33.33"), m.weightedAdherencePct(), "la oficial no cambia: cuentan 0");
        assertFalse(m.hasEnoughEvidence(50.0), "sin evidencia: INSUFFICIENT_DATA");
    }

    @Test
    @DisplayName("los mismos con causa declarada: cobertura 100, observada 33,33")
    void conCausaSiSonRegistro() {
        var m = calculator.calculate(new WeekPlanInput(7L, (short) 2, List.of(
                        previsto(1, 0, "COMPLETED", null), previsto(2, 2, "SKIPPED", "LACK_OF_TIME"),
                        previsto(3, 4, "SKIPPED", "FATIGUE"))),
                List.of(new WeekSessionInput(1L, BigDecimal.valueOf(100), (short) 40, (short) 6, null,
                        4, 0, 0, 0, null)),
                null, (short) 1, 0, 0, 0, RiesgoEscenariosTest.params());
        assertEquals(3, m.recordedWorkouts());
        assertEquals(new BigDecimal("100.00"), m.registrationCoveragePct());
        assertEquals(new BigDecimal("33.33"), m.observedAdherencePct());
    }

    @Test
    @DisplayName("el identificador de sesiones tampoco senala un vencido sin causa")
    void identificador() {
        var plan = new WeekPlanInput(7L, (short) 2, List.of(previsto(1, 0, "SKIPPED", null),
                previsto(2, 2, "SKIPPED", "PAIN_OR_DISCOMFORT")));
        var resultado = new SessionOutcomeIdentifier().identify(plan, List.of(), RiesgoEscenariosTest.params());
        assertFalse(resultado.get(0).recorded());
        assertNull(resultado.get(0).completionPct());
        assertFalse(resultado.get(0).problematic());
        assertTrue(resultado.get(1).recorded());
        assertTrue(resultado.get(1).problematic());
    }
}
