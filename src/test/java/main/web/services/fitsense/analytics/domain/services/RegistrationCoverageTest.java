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
 * V30: separar sesion SIN REGISTRO de sesion OMITIDA.
 * <p>
 * El defecto que estas pruebas fijan: hasta V29,
 * {@code completion = session == null ? 0.0 : session.completion()} metia la
 * sesion desconocida como un cero en la adherencia ponderada y la sumaba a
 * {@code skipped}. Un participante que no registro sus entrenamientos quedaba
 * indistinguible de uno que los omitio todos, y son los dos casos que hay que
 * tratar al contrario.
 * <p>
 * LO QUE NO CAMBIA, y hay una prueba por cada cosa: {@code weightedAdherencePct}
 * -la metrica primaria del estudio-, las otras tres adherencias, la racha de
 * omisiones y el riesgo. El unico indicador que cambia de significado es
 * {@code skippedWorkouts}, que pasa a contar solo lo que TIENE registro.
 */
class RegistrationCoverageTest {

    private static final LocalDate LUNES = LocalDate.of(2026, 10, 12);

    /** PROVISIONAL, pendiente de aprobacion metodologica. No es un valor del dominio. */
    private static final double COBERTURA_MINIMA_PCT = 50.0;

    private final WeeklyMetricsCalculator calculator = new WeeklyMetricsCalculator();

    // -----------------------------------------------------------------------
    // Los cuatro casos exigidos
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("3 sesiones sin registro: cobertura 0, sin cumplimiento observado")
    void tresSinRegistro() {
        var m = calcular(
                List.of(previsto(1, 0, "SCHEDULED"), previsto(2, 2, "SCHEDULED"),
                        previsto(3, 4, "SCHEDULED")),
                List.of());

        assertEquals(0, m.recordedWorkouts());
        assertNull(m.observedAdherencePct(), "sin un solo registro no hay denominador");
        assertEquals(cifra("0.00"), m.registrationCoveragePct());
        assertFalse(hayEvidencia(m), "no se puede decidir una adaptacion sobre esto");
    }

    @Test
    @DisplayName("3 omisiones DECLARADAS: cobertura completa y cumplimiento real del 0 %")
    void tresOmitidasDeclaradas() {
        var m = calcular(
                List.of(previsto(1, 0, "SKIPPED"), previsto(2, 2, "SKIPPED"),
                        previsto(3, 4, "SKIPPED")),
                List.of());

        assertEquals(3, m.recordedWorkouts(), "una omision declarada ES un registro");
        assertEquals(cifra("0.00"), m.observedAdherencePct());
        assertEquals(cifra("100.00"), m.registrationCoveragePct());
        assertTrue(hayEvidencia(m), "aqui si sabemos lo que paso: no entreno");
    }

    @Test
    @DisplayName("2 completadas y 1 sin registro: observado 100 %, cobertura 2 de 3")
    void dosCompletadasYUnaSinRegistro() {
        var m = calcular(
                List.of(previsto(1, 0, "COMPLETED"), previsto(2, 2, "COMPLETED"),
                        previsto(3, 4, "SCHEDULED")),
                List.of(sesion(1, 100), sesion(2, 100)));

        assertEquals(2, m.recordedWorkouts());
        assertEquals(cifra("100.00"), m.observedAdherencePct(),
                "cumplio todo lo que consta: la sesion desconocida no lo penaliza");
        assertEquals(cifra("66.67"), m.registrationCoveragePct());
        assertTrue(hayEvidencia(m));

        // ESTE es el caso que justifica V30. La metrica primaria da 66,67 %, que
        // cae en el tramo de baja adherencia y activaria una adaptacion sobre un
        // participante que cumplio el 100 % de lo que registro.
        assertEquals(cifra("66.67"), m.weightedAdherencePct());
    }

    @Test
    @DisplayName("2 omitidas declaradas y 1 sin registro: observado 0 %, cobertura 2 de 3")
    void dosOmitidasYUnaSinRegistro() {
        var m = calcular(
                List.of(previsto(1, 0, "SKIPPED"), previsto(2, 2, "SKIPPED"),
                        previsto(3, 4, "SCHEDULED")),
                List.of());

        assertEquals(2, m.recordedWorkouts());
        assertEquals(cifra("0.00"), m.observedAdherencePct());
        assertEquals(cifra("66.67"), m.registrationCoveragePct());
        assertTrue(hayEvidencia(m));
    }

    @Test
    @DisplayName("La omision tambien puede constar como sesion con cumplimiento 0")
    void omisionComoSesionEnCero() {
        var m = calcular(
                List.of(previsto(1, 0, "PARTIAL"), previsto(2, 2, "PARTIAL"),
                        previsto(3, 4, "SCHEDULED")),
                List.of(sesion(1, 0), sesion(2, 0)));

        assertEquals(2, m.recordedWorkouts());
        assertEquals(cifra("0.00"), m.observedAdherencePct());
        assertEquals(cifra("66.67"), m.registrationCoveragePct());
    }

    // -----------------------------------------------------------------------
    // Lo que NO debe cambiar
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("La metrica primaria conserva su denominador: TODOS los previstos")
    void laPrimariaNoCambiaDeDefinicion() {
        // 100 % y 40 % registrados, uno sin registro. Ponderada sobre los tres:
        // (45*100 + 45*40 + 45*0) / 135 = 46,67.
        var m = calcular(
                List.of(previsto(1, 0, "COMPLETED"), previsto(2, 2, "PARTIAL"),
                        previsto(3, 4, "SCHEDULED")),
                List.of(sesion(1, 100), sesion(2, 40)));

        assertEquals(cifra("46.67"), m.weightedAdherencePct(),
                "si esto cambia, se ha redefinido la metrica primaria del estudio");
        // Y lo observado, sobre los dos con registro: (45*100 + 45*40) / 90 = 70.
        assertEquals(cifra("70.00"), m.observedAdherencePct());
    }

    @Test
    @DisplayName("Las otras tres adherencias, la racha y el riesgo no se mueven")
    void losDemasIndicadoresNoSeMueven() {
        var m = calcular(
                List.of(previsto(1, 0, "COMPLETED"), previsto(2, 2, "PARTIAL"),
                        previsto(3, 4, "SCHEDULED")),
                List.of(sesion(1, 100), sesion(2, 40)));

        assertEquals(2, m.validWorkouts(), "40 % supera el umbral de valida (30 %)");
        assertEquals(1, m.completedWorkouts());
        assertEquals(cifra("66.67"), m.frequencyAdherencePct());
        assertEquals(cifra("33.33"), m.workoutAdherencePct());
        assertEquals(1, m.consecutiveSkips(),
                "la racha sigue contando el previsto sin registro: no entrenar y no "
                        + "registrar son los dos senales de desvinculacion, y eso no se toca aqui");
    }

    @Test
    @DisplayName("skippedWorkouts es el UNICO que cambia: ya no cuenta lo desconocido")
    void elUnicoIndicadorQueCambia() {
        var m = calcular(
                List.of(previsto(1, 0, "COMPLETED"), previsto(2, 2, "COMPLETED"),
                        previsto(3, 4, "SCHEDULED")),
                List.of(sesion(1, 100), sesion(2, 100)));

        // Hasta V29 esto valia 1: el previsto sin registro se contaba omitido.
        assertEquals(0, m.skippedWorkouts(),
                "un entrenamiento sin registro no es un entrenamiento omitido");
    }

    @Test
    @DisplayName("Invariante: validos + omitidos == con registro, en toda forma de semana")
    void invarianteDeParticion() {
        var formas = List.of(
                List.of("SCHEDULED", "SCHEDULED", "SCHEDULED"),
                List.of("SKIPPED", "SKIPPED", "SKIPPED"),
                List.of("COMPLETED", "SCHEDULED", "SKIPPED"),
                List.of("PARTIAL", "PARTIAL", "COMPLETED"));

        for (var forma : formas) {
            var previstos = new java.util.ArrayList<WeekPlanInput.ScheduledWorkout>();
            var sesiones = new java.util.ArrayList<WeekSessionInput>();
            for (int i = 0; i < forma.size(); i++) {
                previstos.add(previsto(i + 1, i * 2, forma.get(i)));
                if (forma.get(i).equals("COMPLETED")) sesiones.add(sesion(i + 1, 100));
                if (forma.get(i).equals("PARTIAL")) sesiones.add(sesion(i + 1, 45));
            }
            var m = calcular(previstos, sesiones);
            assertEquals(m.recordedWorkouts(), m.validWorkouts() + m.skippedWorkouts(),
                    "ck_wum_recorded_partition se rompe con la forma " + forma);
            assertTrue(m.recordedWorkouts() <= m.scheduledWorkouts(),
                    "ck_wum_recorded_range se rompe con la forma " + forma);
        }
    }

    @Test
    @DisplayName("Con cobertura minima del 0 %, una semana sin registros NO tiene evidencia")
    void coberturaMinimaDeCeroNoDeclaraEvidencia() {
        var m = calcular(
                List.of(previsto(1, 0, "SCHEDULED"), previsto(2, 2, "SCHEDULED"),
                        previsto(3, 4, "SCHEDULED")),
                List.of());

        assertEquals(cifra("0.00"), m.registrationCoveragePct());
        // Con el umbral al 0 la cobertura lo cumple: 0 >= 0. Lo que lo impide es
        // que no haya cumplimiento observado, porque no hay ni un registro.
        assertFalse(m.hasEnoughEvidence(0.0),
                "sin una sola sesion registrada no se puede declarar evidencia, "
                        + "por bajo que se configure el umbral");
        assertNull(m.observedAdherencePct());
    }

    @Test
    @DisplayName("Con un solo registro y cobertura minima del 0 %, si hay evidencia")
    void unRegistroBastaCuandoElUmbralEsCero() {
        var m = calcular(
                List.of(previsto(1, 0, "COMPLETED"), previsto(2, 2, "SCHEDULED"),
                        previsto(3, 4, "SCHEDULED")),
                List.of(sesion(1, 100)));

        assertEquals(1, m.recordedWorkouts());
        assertEquals(cifra("33.33"), m.registrationCoveragePct());
        assertTrue(m.hasEnoughEvidence(0.0), "hay un registro y el umbral es 0");
        assertFalse(m.hasEnoughEvidence(COBERTURA_MINIMA_PCT),
                "33,33 % no llega al 50 % provisional");
    }

    @Test
    @DisplayName("Semana sin plan: cero registros y ningun porcentaje, como las demas")
    void semanaSinPlan() {
        var m = calculator.calculate(new WeekPlanInput(null, (short) 1, List.of()),
                List.of(), null, (short) 20, 0, 0, 0, params());

        assertFalse(m.hasActivePlan());
        assertEquals(0, m.recordedWorkouts());
        assertNull(m.observedAdherencePct());
        assertNull(m.registrationCoveragePct());
        assertFalse(m.hasEnoughEvidence(0.0), "sin plan no hay nada sobre lo que decidir");
    }

    // -----------------------------------------------------------------------
    // Utilidades
    // -----------------------------------------------------------------------

    private MetricsCalculation calcular(List<WeekPlanInput.ScheduledWorkout> previstos,
                                        List<WeekSessionInput> sesiones) {
        return calculator.calculate(
                new WeekPlanInput(7L, (short) 2, previstos), sesiones,
                BigDecimal.valueOf(75), (short) 3, 450, 150, 0, params());
    }

    private boolean hayEvidencia(MetricsCalculation m) {
        return m.hasEnoughEvidence(COBERTURA_MINIMA_PCT);
    }

    private static BigDecimal cifra(String valor) {
        return new BigDecimal(valor);
    }

    /** Un previsto de 45 minutos y 5 ejercicios. El motivo solo acompana a SKIPPED. */
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
