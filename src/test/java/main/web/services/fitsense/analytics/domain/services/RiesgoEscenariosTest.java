package main.web.services.fitsense.analytics.domain.services;

import main.web.services.fitsense.analytics.domain.model.valueobjects.MetricsCalculation;
import main.web.services.fitsense.analytics.domain.model.valueobjects.RiskLevel;
import main.web.services.fitsense.analytics.domain.model.valueobjects.WeekPlanInput;
import main.web.services.fitsense.analytics.domain.model.valueobjects.WeekSessionInput;
import main.web.services.fitsense.configuration.domain.model.valueobjects.CalculationParams;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DETECCION DE RIESGO con la tabla activa (MVP-1.8), escenario por escenario.
 * <pre>
 *   baja adherencia (A_w oficial):  >= 80 -> 0 · >= 60 -> 10 · >= 40 -> 20 · resto -> 30
 *   omisiones al final de semana:   1 -> 5 · 2 -> 15 · 3 o mas -> 25   (sin sesion >= 30 %)
 *   caida frente a la anterior:     >= 15 pp -> 5 · >= 25 pp -> 15
 *   sobreesfuerzo:                  RPE medio >= 8 -> 10
 *   nivel:                          >= 25 MODERATE · >= 50 HIGH · >= 70 CRITICAL
 *   abandono (aparte del puntaje):  >= 14 dias sin sesion que cuente
 * </pre>
 * Semana de tres previstos (lunes, miercoles, viernes). Cada esperado se suma a
 * mano en el nombre o el comentario de la prueba.
 */
class RiesgoEscenariosTest {

    private static final LocalDate LUNES = LocalDate.of(2026, 10, 12);
    private final WeeklyMetricsCalculator calculator = new WeeklyMetricsCalculator();

    @Nested
    @DisplayName("baja adherencia")
    class BajaAdherencia {

        @Test
        @DisplayName("100/100/100, RPE 6: 0 puntos, LOW, sin factores")
        void sinRiesgo() {
            var m = semana(new double[]{100, 100, 100}, 6, BigDecimal.valueOf(100), (short) 1);
            assertRiesgo(m, 0, RiskLevel.LOW, Map.of());
        }

        @Test
        @DisplayName("100/100/40 = 80,00: justo en el umbral, 0 puntos")
        void umbral80() {
            var m = semana(new double[]{100, 100, 40}, 6, null, (short) 1);
            assertEquals(new BigDecimal("80.00"), m.weightedAdherencePct());
            assertRiesgo(m, 0, RiskLevel.LOW, Map.of());
        }

        @Test
        @DisplayName("100/100/39 = 79,67: 10 puntos; el 39 % es sesion valida y no abre racha")
        void bajoEl80() {
            var m = semana(new double[]{100, 100, 39}, 6, null, (short) 1);
            assertRiesgo(m, 10, RiskLevel.LOW, Map.of("low_adherence", 10));
        }

        @Test
        @DisplayName("la duracion no cambia el riesgo: 100/0/0 con 60, 45 y 30 minutos -> 33,33 -> 30 + racha 2 (15) = 45")
        void sinPesoPorDuracion() {
            // Con la formula anterior: 6000 / 135 = 44,44 -> 20 + 15 = 35.
            var previstos = List.of(previsto(1, 0, 60), previsto(2, 2, 45), previsto(3, 4, 30));
            var m = calcular(previstos, List.of(sesion(1, 100, 6)), null, (short) 1);
            assertEquals(new BigDecimal("33.33"), m.weightedAdherencePct());
            assertRiesgo(m, 45, RiskLevel.MODERATE, Map.of("low_adherence", 30, "consecutive_skips", 15));
        }
    }

    @Nested
    @DisplayName("omisiones seguidas")
    class Omisiones {

        @Test
        @DisplayName("100/0/0 al final: 30 + 15 = 45, MODERATE")
        void alFinal() {
            var m = semana(new double[]{100, -1, -1}, 6, null, (short) 1);
            assertEquals(2, m.consecutiveSkips());
            assertRiesgo(m, 45, RiskLevel.MODERATE, Map.of("low_adherence", 30, "consecutive_skips", 15));
        }

        @Test
        @DisplayName("0/0/100 al principio: la racha se cuenta desde el final -> 0; solo 30, MODERATE")
        void alPrincipio() {
            var m = semana(new double[]{-1, -1, 100}, 6, null, (short) 1);
            assertEquals(0, m.consecutiveSkips());
            assertRiesgo(m, 30, RiskLevel.MODERATE, Map.of("low_adherence", 30));
        }

        @Test
        @DisplayName("100/100/25: el 25 % no es valido -> racha 1 (5); 75 -> 10; total 15, LOW")
        void sesionBajoElMinimo() {
            var m = semana(new double[]{100, 100, 25}, 6, null, (short) 1);
            assertEquals(new BigDecimal("75.00"), m.weightedAdherencePct());
            assertEquals(1, m.consecutiveSkips());
            assertRiesgo(m, 15, RiskLevel.LOW, Map.of("low_adherence", 10, "consecutive_skips", 5));
        }
    }

    @Nested
    @DisplayName("caida frente a la semana anterior")
    class Caida {

        @Test
        @DisplayName("de 90 a 80: caida de 10 pp, no puntua")
        void caidaPequena() {
            var m = semana(new double[]{100, 100, 40}, 6, BigDecimal.valueOf(90), (short) 1);
            assertRiesgo(m, 0, RiskLevel.LOW, Map.of());
        }

        @Test
        @DisplayName("de 95 a 80: caida de 15 pp justo en el umbral -> 5")
        void caidaEnUmbral() {
            var m = semana(new double[]{100, 100, 40}, 6, BigDecimal.valueOf(95), (short) 1);
            assertRiesgo(m, 5, RiskLevel.LOW, Map.of("adherence_drop", 5));
        }

        @Test
        @DisplayName("de 100 a 63,33 (100/50/40): 10 + caida 36,67 (15) = 25, MODERATE")
        void caidaFuerte() {
            var m = semana(new double[]{100, 50, 40}, 6, BigDecimal.valueOf(100), (short) 1);
            assertEquals(new BigDecimal("63.33"), m.weightedAdherencePct());
            assertRiesgo(m, 25, RiskLevel.MODERATE, Map.of("low_adherence", 10, "adherence_drop", 15));
        }

        @Test
        @DisplayName("una subida no puntua, y sin semana anterior no hay caida")
        void subidaOSinAnterior() {
            assertRiesgo(semana(new double[]{100, 100, 100}, 6, BigDecimal.valueOf(40), (short) 1),
                    0, RiskLevel.LOW, Map.of());
            assertRiesgo(semana(new double[]{100, 50, 40}, 6, null, (short) 1),
                    10, RiskLevel.LOW, Map.of("low_adherence", 10));
        }
    }

    @Nested
    @DisplayName("sobreesfuerzo, nivel maximo y abandono")
    class Otras {

        @Test
        @DisplayName("RPE medio 8 -> 10; RPE medio 7,67 -> 0")
        void sobreesfuerzo() {
            assertRiesgo(semana(new double[]{100, 100, 100}, 8, null, (short) 1),
                    10, RiskLevel.LOW, Map.of("overexertion", 10));
            var rpes = List.of(sesion(1, 100, 8), sesion(2, 100, 8), sesion(3, 100, 7));
            assertRiesgo(calcular(tres(), rpes, null, (short) 1), 0, RiskLevel.LOW, Map.of());
        }

        @Test
        @DisplayName("nada registrado tras una semana al 100: 30 + 25 + 15 = 70, CRITICAL")
        void critico() {
            var m = semana(new double[]{-1, -1, -1}, 6, BigDecimal.valueOf(100), (short) 9);
            assertEquals(new BigDecimal("0.00"), m.weightedAdherencePct());
            assertRiesgo(m, 70, RiskLevel.CRITICAL,
                    Map.of("low_adherence", 30, "consecutive_skips", 25, "adherence_drop", 15));
        }

        @Test
        @DisplayName("30 + 15 + 5 = 50: HIGH")
        void alto() {
            // 100/0/0 -> 33,33 (30), racha 2 (15); de 50 a 33,33 -> 16,67 pp (5).
            var m = semana(new double[]{100, -1, -1}, 6, BigDecimal.valueOf(50), (short) 1);
            assertRiesgo(m, 50, RiskLevel.HIGH,
                    Map.of("low_adherence", 30, "consecutive_skips", 15, "adherence_drop", 5));
        }

        @Test
        @DisplayName("abandono con 14 dias sin entrenar, no con 13; no suma al puntaje")
        void abandono() {
            var con14 = semana(new double[]{100, 100, 100}, 6, null, (short) 14);
            assertTrue(con14.dropout());
            assertEquals(14, ((Number) con14.riskFactors().get("dropout_days")).intValue());
            assertEquals(0, con14.riskScore().intValue());
            assertFalse(semana(new double[]{100, 100, 100}, 6, null, (short) 13).dropout());
            assertFalse(semana(new double[]{100, 100, 100}, 6, null, null).dropout(),
                    "quien nunca entreno no se marca como abandono");
        }

        @Test
        @DisplayName("semana sin plan: sin puntaje ni nivel; solo la marca de abandono si corresponde")
        void sinPlan() {
            var m = calculator.calculate(new WeekPlanInput(null, (short) 0, List.of()), List.of(),
                    BigDecimal.valueOf(100), (short) 20, 0, 0, 0, params());
            assertNull(m.riskScore());
            assertNull(m.riskLevel());
            assertTrue(m.dropout());
        }
    }

    // ------------------------------------------------------------------ utilidades

    private static void assertRiesgo(MetricsCalculation m, int puntos, RiskLevel nivel, Map<String, Integer> factores) {
        assertEquals(new BigDecimal(puntos).setScale(2), m.riskScore(), "puntaje; factores " + m.riskFactors());
        assertEquals(nivel, m.riskLevel());
        var reales = new java.util.HashMap<>(m.riskFactors());
        reales.remove("dropout_days");
        assertEquals(factores, reales);
    }

    /** Cumplimiento por dia; -1 = sin sesion. Mismo RPE en todas. */
    private MetricsCalculation semana(double[] cumplimientos, int rpe, BigDecimal anterior, Short diasSinEntrenar) {
        var sesiones = new ArrayList<WeekSessionInput>();
        for (int i = 0; i < cumplimientos.length; i++)
            if (cumplimientos[i] >= 0) sesiones.add(sesion(i + 1, cumplimientos[i], rpe));
        return calcular(tres(), sesiones, anterior, diasSinEntrenar);
    }

    private static List<WeekPlanInput.ScheduledWorkout> tres() {
        return List.of(previsto(1, 0, 45), previsto(2, 2, 45), previsto(3, 4, 45));
    }

    private MetricsCalculation calcular(List<WeekPlanInput.ScheduledWorkout> previstos, List<WeekSessionInput> sesiones,
                                        BigDecimal anterior, Short diasSinEntrenar) {
        return calculator.calculate(new WeekPlanInput(7L, (short) 3, previstos), sesiones, anterior,
                diasSinEntrenar, 450, 150, 0, params());
    }

    private static WeekPlanInput.ScheduledWorkout previsto(long id, int dia, int minutos) {
        return new WeekPlanInput.ScheduledWorkout(id, LUNES.plusDays(dia), minutos, 4, "SCHEDULED", null);
    }

    private static WeekSessionInput sesion(long id, double cumplimiento, int rpe) {
        return new WeekSessionInput(id, BigDecimal.valueOf(cumplimiento), (short) 40, (short) rpe, null,
                0, 0, 0, 0, null);
    }

    /** La tabla de riesgo de MVP-1.8, la activa: AdherenciaRiesgoIT comprueba que coincide con la base. */
    static CalculationParams params() {
        var adherence = new CalculationParams.Adherence(80.0, 30.0, 80.0, 100.0,
                "weighted_adherence_pct", "LAST_FINISHED", 7);
        var adjustment = new CalculationParams.Adjustment(80.0, 50.0, 20.0, 30.0, 10.0,
                5.0, 5.0, 15.0, 20.0, 40.0, 5.0, 1, 2, 20, 2, 2, 6, 20, 1, 1, 20.0, 8.0);
        var risk = new CalculationParams.Risk(
                List.of(new CalculationParams.Risk.AdherencePoints(80.0, 0),
                        new CalculationParams.Risk.AdherencePoints(60.0, 10),
                        new CalculationParams.Risk.AdherencePoints(40.0, 20),
                        new CalculationParams.Risk.AdherencePoints(0.0, 30)),
                List.of(new CalculationParams.Risk.SkipPoints(3, 25),
                        new CalculationParams.Risk.SkipPoints(2, 15),
                        new CalculationParams.Risk.SkipPoints(1, 5),
                        new CalculationParams.Risk.SkipPoints(0, 0)),
                List.of(new CalculationParams.Risk.DropPoints(25.0, 15),
                        new CalculationParams.Risk.DropPoints(15.0, 5),
                        new CalculationParams.Risk.DropPoints(0.0, 0)),
                10, 8.0, new CalculationParams.Risk.Levels(0.0, 25.0, 50.0, 70.0));
        return new CalculationParams(adherence, adjustment, risk, new CalculationParams.Dropout(14), null);
    }
}
