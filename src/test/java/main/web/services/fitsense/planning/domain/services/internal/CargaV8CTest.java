package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * V8C · LA CARGA SUGERIDA EN UNA SESION SENALADA (decision 2(a), 27/10/2026).
 * <p>
 * Bajarla, desde una carga prescrita, en una intervencion que lo permite, es un
 * cambio efectivo (APLICADA). Subirla, ponerla donde no habia o bajarla sin
 * permiso se rechaza. En las sesiones no senaladas la carga sigue sin compararse.
 */
@DisplayName("V8C · carga: solo la reduccion autorizada cuenta como adaptacion; lo demas se rechaza")
class CargaV8CTest {

    private static final LocalDate LUNES = LocalDate.of(2026, 11, 2);
    private static final long S = 801L;       // la senalada
    private static final long OTRA = 802L;    // no senalada
    private static final long A = 10L;        // con carga prescrita (20 kg)
    private static final long B = 11L;        // sin carga prescrita

    private final ConservationValidator v8c = new ConservationValidator();

    @Test
    @DisplayName("REVISAR_CARGA: 20 -> 16 kg, sin mas cambios -> aplicada")
    void reduccionAutorizada() {
        var r = validar(TipoIntervencion.REVISAR_CARGA_Y_RECUPERACION, carga("16.00"), null, carga("20.00"));
        assertTrue(r.aceptable(), r.problems().toString());
        assertEquals(List.of(S), r.aplicadas());
        assertTrue(r.huboAdaptacionEfectiva());
    }

    @Test
    @DisplayName("VARIANTE_MAS_SENCILLA_O_CARGA tambien la permite")
    void reduccionEnVariante() {
        var r = validar(TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA, carga("18.00"), null, carga("20.00"));
        assertTrue(r.aceptable(), r.problems().toString());
        assertEquals(List.of(S), r.aplicadas());
    }

    @Test
    @DisplayName("la misma carga -> sin efecto (como antes)")
    void sinCambio() {
        var r = validar(TipoIntervencion.REVISAR_CARGA_Y_RECUPERACION, carga("20.00"), null, carga("20.00"));
        assertTrue(r.aceptable());
        assertEquals(List.of(S), r.sinEfecto());
    }

    @Test
    @DisplayName("subir la carga (20 -> 24 kg) se rechaza")
    void subidaRechazada() {
        var r = validar(TipoIntervencion.REVISAR_CARGA_Y_RECUPERACION, carga("24.00"), null, carga("20.00"));
        assertFalse(r.aceptable());
        assertTrue(r.problems().stream().anyMatch(p -> p.contains("sube la carga")), r.problems().toString());
        assertTrue(r.aplicadas().isEmpty());
    }

    @Test
    @DisplayName("poner carga donde no habia ninguna prescrita se rechaza")
    void cargaNuevaRechazada() {
        var r = validar(TipoIntervencion.REVISAR_CARGA_Y_RECUPERACION, carga("20.00"), carga("10.00"), carga("20.00"));
        assertFalse(r.aceptable());
        assertTrue(r.problems().stream().anyMatch(p -> p.contains("no tenia carga prescrita")), r.problems().toString());
    }

    @Test
    @DisplayName("bajar la carga en una intervencion que no lo permite (ACORTAR, INVESTIGAR) se rechaza")
    void reduccionSinPermiso() {
        for (var t : List.of(TipoIntervencion.ACORTAR_O_REORGANIZAR, TipoIntervencion.INVESTIGAR_CAMBIO_MINIMO,
                TipoIntervencion.REPROGRAMAR_DIAS, TipoIntervencion.CAUSA_NO_REGISTRADA)) {
            var r = validar(t, carga("16.00"), null, carga("20.00"));
            assertFalse(r.aceptable(), t.name());
            assertTrue(r.problems().stream().anyMatch(p -> p.contains("no autoriza cambiar la carga")),
                    t + ": " + r.problems());
        }
    }

    @Test
    @DisplayName("una carga a cero se rechaza")
    void cargaCero() {
        var r = validar(TipoIntervencion.REVISAR_CARGA_Y_RECUPERACION, carga("0.00"), null, carga("20.00"));
        assertFalse(r.aceptable());
    }

    @Test
    @DisplayName("quitar la sugerencia (20 -> null) no es efecto ni se rechaza")
    void quitarSugerencia() {
        var r = validar(TipoIntervencion.REVISAR_CARGA_Y_RECUPERACION, null, null, carga("20.00"));
        assertTrue(r.aceptable(), r.problems().toString());
        assertEquals(List.of(S), r.sinEfecto());
    }

    @Test
    @DisplayName("en la sesion NO senalada la carga no se compara (como antes)")
    void noSenaladaIgual() {
        var r = validar(TipoIntervencion.REVISAR_CARGA_Y_RECUPERACION, carga("20.00"), null, carga("12.00"));
        assertTrue(r.aceptable(), r.problems().toString());
        assertTrue(r.conservadas().contains(OTRA));
    }

    @Test
    @DisplayName("bajar la carga y ademas tocar otra cosa sin permiso sigue rechazado por V8C")
    void cargaYOtroCambio() {
        var previa = previa();
        var derivada = new WeekDerivation(List.of(
                new DerivedWorkout(S, sesion(S, 0, List.of(ej(A, carga("16.00")), ej(B, null)), 1)),   // cambia de dia
                new DerivedWorkout(OTRA, sesion(OTRA, 2, List.of(ej(A, carga("20.00"))), 0))), List.of());
        var r = v8c.validate(derivada, previa, orden(TipoIntervencion.REVISAR_CARGA_Y_RECUPERACION), Map.of());
        assertFalse(r.aceptable());
        assertTrue(r.aplicadas().isEmpty());
    }

    // =======================================================================

    private ConservationReport validar(TipoIntervencion t, BigDecimal cargaA, BigDecimal cargaB, BigDecimal cargaOtra) {
        var derivada = new WeekDerivation(List.of(
                new DerivedWorkout(S, sesion(S, 0, List.of(ej(A, cargaA), ej(B, cargaB)), 0)),
                new DerivedWorkout(OTRA, sesion(OTRA, 2, List.of(ej(A, cargaOtra)), 0))), List.of());
        return v8c.validate(derivada, previa(), orden(t), Map.of());
    }

    private static BigDecimal carga(String kg) {
        return new BigDecimal(kg);
    }

    private static PlanDraft.DraftExercise ej(long id, BigDecimal carga) {
        return new PlanDraft.DraftExercise(id, PrescriptionType.SETS_REPS, (short) 3, (short) 10, null, carga,
                (short) 90, null);
    }

    private static PlanDraft.DraftWorkout sesion(long id, int dia, List<PlanDraft.DraftExercise> l, int desplazar) {
        return new PlanDraft.DraftWorkout(LUNES.plusDays(dia + desplazar).plusWeeks(1), WorkoutFocus.FULL_BODY,
                "Sesion " + id, 40, l);
    }

    /** La semana medida: S con A (20 kg) y B (sin carga); OTRA con A (20 kg). */
    private static PreviousWeekSummary previa() {
        var sesiones = List.of(
                new PreviousWeekSummary.WorkoutOutcome(S, LUNES, "FULL_BODY", "SKIPPED", "FATIGUE", true, "SKIPPED",
                        null, BigDecimal.ZERO, "Sesion " + S, (short) 40),
                new PreviousWeekSummary.WorkoutOutcome(OTRA, LUNES.plusDays(2), "FULL_BODY", "COMPLETED", null, true,
                        "COMPLETED", (short) 6, new BigDecimal("100.00"), "Sesion " + OTRA, (short) 40));
        var prescritas = List.of(
                presc(S, LUNES, A, new BigDecimal("20.00")),
                presc(S, LUNES, B, null),
                presc(OTRA, LUNES.plusDays(2), A, new BigDecimal("20.00")));
        return new PreviousWeekSummary(new BigDecimal("66.67"), new BigDecimal("6.0"), null, Map.of(), sesiones,
                prescritas, List.of());
    }

    private static PreviousWeekSummary.PrescriptionOutcome presc(long sesion, LocalDate dia, long id, BigDecimal carga) {
        return new PreviousWeekSummary.PrescriptionOutcome(sesion, dia, id, "ejercicio " + id, "SETS_REPS",
                (short) 3, (short) 10, null, carga, (short) 3, 30, null, carga, new BigDecimal("100.00"),
                "COMPLETED", null, "COMPLETED", (short) 90);
    }

    private static PlanAdjustment orden(TipoIntervencion t) {
        return new PlanAdjustment(List.of(AdjustmentType.NONE), null, null, null, 0.0, 0.0, null, null, null,
                "causa", null, null, List.of(new PlanAdjustment.TargetedSession(S, t)));
    }
}
