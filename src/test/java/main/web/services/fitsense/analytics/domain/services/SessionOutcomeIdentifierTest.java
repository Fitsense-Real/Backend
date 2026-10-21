package main.web.services.fitsense.analytics.domain.services;

import main.web.services.fitsense.analytics.domain.model.valueobjects.SessionOutcome;
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
 * Cual de las sesiones fue un problema, y cual no se puede juzgar.
 * <p>
 * El criterio no lo inventa el identificador: reutiliza
 * {@code session_completed_threshold_pct} = 80, el mismo umbral con el que el
 * calculo semanal cuenta las sesiones completadas.
 */
class SessionOutcomeIdentifierTest {

    private static final LocalDate LUNES = LocalDate.of(2026, 10, 12);

    private final SessionOutcomeIdentifier identifier = new SessionOutcomeIdentifier();

    @Test
    @DisplayName("Sin registro no se senala: no se corrige lo que no consta que fallo")
    void sinRegistroNoSeSenala() {
        var r = identificar(List.of(prev(1, 0, "SCHEDULED", null)), List.of());

        assertEquals(1, r.size());
        var s = r.get(0);
        assertFalse(s.recorded());
        assertTrue(s.unknown());
        assertNull(s.completionPct(), "sin registro el cumplimiento es un hueco, no un cero");
        assertFalse(s.problematic(), "no se puede senalar una sesion que no se sabe si fallo");
    }

    @Test
    @DisplayName("Omision DECLARADA: hay registro, cumplimiento 0 y queda senalada")
    void omisionDeclarada() {
        var r = identificar(List.of(prev(1, 0, "SKIPPED", "LACK_OF_TIME")), List.of());

        var s = r.get(0);
        assertTrue(s.recorded(), "la persona nos dijo que no entreno: eso es informacion");
        assertEquals(new BigDecimal("0"), s.completionPct());
        assertTrue(s.problematic());
        assertEquals("LACK_OF_TIME", s.skipReason());
    }

    @Test
    @DisplayName("Sesion completada al 100 %: registrada y no senalada")
    void completada() {
        var r = identificar(List.of(prev(1, 0, "COMPLETED", null)), List.of(ses(1, 100, null)));

        assertTrue(r.get(0).recorded());
        assertFalse(r.get(0).problematic());
    }

    @Test
    @DisplayName("Por debajo del 80 % se senala; en el 80 % o por encima, no")
    void elUmbralEsElDelInstrumento() {
        assertTrue(identificar(List.of(prev(1, 0, "PARTIAL", null)),
                List.of(ses(1, 79, null))).get(0).problematic());
        assertFalse(identificar(List.of(prev(1, 0, "PARTIAL", null)),
                List.of(ses(1, 80, null))).get(0).problematic());
        assertFalse(identificar(List.of(prev(1, 0, "COMPLETED", null)),
                List.of(ses(1, 85, null))).get(0).problematic());
    }

    @Test
    @DisplayName("Un RPE alto NO convierte en problema una sesion completada")
    void elRpeNoSenala() {
        var r = identificar(List.of(prev(1, 0, "COMPLETED", null)),
                List.of(ses(1, 100, (short) 9)));

        assertFalse(r.get(0).problematic(), "completada con esfuerzo alto no es un fallo");
        assertEquals(Short.valueOf((short) 9), r.get(0).sessionRpe(),
                "pero el RPE viaja como contexto");
    }

    @Test
    @DisplayName("El motivo de la sesion manda sobre el del previsto")
    void elMotivoDeLaSesionManda() {
        var sesion = new WeekSessionInput(1L, BigDecimal.valueOf(40), (short) 30, (short) 7,
                (short) 3, 2, 120, 40, 0, "TOO_DIFFICULT");
        var r = identificar(List.of(prev(1, 0, "PARTIAL", "LACK_OF_TIME")), List.of(sesion));

        assertEquals("TOO_DIFFICULT", r.get(0).skipReason(),
                "lo que registro la persona al cerrar la sesion es mas fiable");
    }

    @Test
    @DisplayName("La peor es la de menor cumplimiento, y nunca una sin registro")
    void laPeorIgnoraLasQueNoConstan() {
        var r = identificar(
                List.of(prev(1, 0, "COMPLETED", null), prev(2, 2, "PARTIAL", null),
                        prev(3, 4, "SCHEDULED", null)),
                List.of(ses(1, 100, null), ses(2, 45, null)));

        var peor = identifier.worst(r);
        assertTrue(peor.isPresent());
        assertEquals(2L, peor.get().plannedWorkoutId(),
                "la del 45 %, no la sin registro aunque parezca la peor");
    }

    @Test
    @DisplayName("A igual cumplimiento, la mas antigua: se corrige donde empezo")
    void empatePorFecha() {
        var r = identificar(
                List.of(prev(1, 0, "SKIPPED", "LACK_OF_TIME"), prev(2, 2, "SKIPPED", "LACK_OF_TIME")),
                List.of());

        assertEquals(1L, identifier.worst(r).orElseThrow().plannedWorkoutId());
    }

    @Test
    @DisplayName("Semana entera sin registro: ninguna senalada y ninguna peor")
    void semanaSinRegistro() {
        var r = identificar(
                List.of(prev(1, 0, "SCHEDULED", null), prev(2, 2, "SCHEDULED", null),
                        prev(3, 4, "SCHEDULED", null)),
                List.of());

        assertEquals(3, r.size());
        assertTrue(r.stream().noneMatch(SessionOutcome::problematic));
        assertTrue(identifier.worst(r).isEmpty());
    }

    @Test
    @DisplayName("Las sesiones salen ordenadas por fecha, para que el reparto se lea")
    void ordenadasPorFecha() {
        var r = identificar(
                List.of(prev(3, 4, "COMPLETED", null), prev(1, 0, "COMPLETED", null),
                        prev(2, 2, "COMPLETED", null)),
                List.of(ses(1, 100, null), ses(2, 100, null), ses(3, 100, null)));

        assertEquals(List.of(1L, 2L, 3L),
                r.stream().map(SessionOutcome::plannedWorkoutId).toList());
    }

    @Test
    @DisplayName("Semana sin previstos: lista vacia, no excepcion")
    void semanaSinPlan() {
        assertTrue(identifier.identify(new WeekPlanInput(null, (short) 1, List.of()),
                List.of(), params()).isEmpty());
    }

    // -----------------------------------------------------------------------

    private List<SessionOutcome> identificar(List<WeekPlanInput.ScheduledWorkout> previstos,
                                             List<WeekSessionInput> sesiones) {
        return identifier.identify(new WeekPlanInput(7L, (short) 2, previstos), sesiones, params());
    }

    private static WeekPlanInput.ScheduledWorkout prev(long id, int dia, String estado, String motivo) {
        return new WeekPlanInput.ScheduledWorkout(id, LUNES.plusDays(dia), 45, 5, estado, motivo);
    }

    private static WeekSessionInput ses(long id, double pct, Short rpe) {
        return new WeekSessionInput(id, BigDecimal.valueOf(pct), (short) 40, rpe, (short) 4,
                (int) Math.round(5 * pct / 100.0), 300, 100, 0, null);
    }

    private static CalculationParams params() {
        var adherence = new CalculationParams.Adherence(80.0, 30.0, 80.0, 100.0,
                "weighted_adherence_pct", "LAST_FINISHED", 7);
        var adjustment = new CalculationParams.Adjustment(80.0, 50.0, 20.0, 30.0, 10.0,
                5.0, 5.0, 15.0, 20.0, 40.0, 5.0, 1, 2, 20, 2, 2, 6, 20, 1, 1, 20.0, 8.0);
        return new CalculationParams(adherence, adjustment, null,
                new CalculationParams.Dropout(14), null);
    }
}
