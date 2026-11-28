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
 * V8C · ANTE DOLOR NO SE ANADE TRABAJO (correccion 5, 27/10/2026).
 * <p>
 * Con {@code SEGURIDAD_ANTE_DOLOR} se puede sustituir sin exigir el mismo patron
 * (forzarlo podria devolver justo lo que molesto), pero no anadir mas ejercicios
 * de los que se retiran ni subir el volumen de la sesion (18.1, el de V8). Con
 * Replicate real se publico una sesion de 3 ejercicios retirados y 4 anadidos.
 * <p>
 * La sesion medida: S, omitida por dolor, con A, B y C a 3x10 (volumen 90).
 * Sin alternativa valida, el respaldo deja la semana NO_FACTIBLE con su motivo:
 * {@code AdaptedWeekProducerTest} («Fallo de IA con dolor senalado»).
 */
@DisplayName("V8C · dolor: sustituir o retirar, sin mas ejercicios ni mas volumen")
class DolorSinMasTrabajoV8CTest {

    private static final LocalDate LUNES = LocalDate.of(2026, 11, 2);
    private static final long S = 951L;
    private static final long OTRA = 952L;
    private static final long A = 1L, B = 2L, C = 3L;            // los de la sesion con dolor
    private static final long X = 11L, Y = 12L, Z = 13L, W = 14L; // sustitutos elegibles
    private static final long Q = 20L;                           // el de la otra sesion

    private final ConservationValidator v8c = new ConservationValidator();

    @Test
    @DisplayName("retira 3 y anade 4 -> rechazada (el caso observado con Replicate real)")
    void retiraTresAnadeCuatro() {
        var r = validar(ej(X, 3, 10), ej(Y, 3, 10), ej(Z, 3, 10), ej(W, 2, 10));
        assertFalse(r.aceptable());
        assertTrue(r.problems().stream().anyMatch(p -> p.contains("anade 4 ejercicio(s)")), r.problems().toString());
        assertTrue(r.aplicadas().isEmpty());
    }

    @Test
    @DisplayName("retira 3 y anade 3 con mas volumen (90 -> 100) -> rechazada")
    void mismoNumeroConMasVolumen() {
        var r = validar(ej(X, 3, 10), ej(Y, 3, 10), ej(Z, 4, 10));
        assertFalse(r.aceptable());
        assertTrue(r.problems().stream().anyMatch(p -> p.contains("su volumen sube de 90 a 100")),
                r.problems().toString());
    }

    @Test
    @DisplayName("retira 3 y anade 3 con el mismo volumen -> aceptada y aplicada")
    void mismoNumeroMismoVolumen() {
        var r = validar(ej(X, 3, 10), ej(Y, 3, 10), ej(Z, 3, 10));
        assertTrue(r.aceptable(), r.problems().toString());
        assertEquals(List.of(S), r.aplicadas());
    }

    @Test
    @DisplayName("retira 3 y anade 2 elegibles, con menos volumen -> aceptada")
    void retiraTresAnadeDos() {
        var r = validar(ej(X, 3, 10), ej(Y, 3, 10));
        assertTrue(r.aceptable(), r.problems().toString());
        assertEquals(List.of(S), r.aplicadas());
    }

    @Test
    @DisplayName("un sustituto por duracion se mide con el divisor de la configuracion (3x600 s / 30 = 60)")
    void duracionConDivisor() {
        var cardio = new PlanDraft.DraftExercise(W, PrescriptionType.DURATION, (short) 3, null, 600, null,
                (short) 60, null);
        // 3x600 s / 30 = 60: con X y Y (60) suma 120 > 90 -> rechazada.
        var r = validarCon(30, ej(X, 3, 10), ej(Y, 3, 10), cardio);
        assertTrue(r.problems().stream().anyMatch(p -> p.contains("su volumen sube de 90 a 120")),
                r.problems().toString());
        // Con un divisor mayor el mismo bloque pesa menos: 1800 / 120 = 15 -> 75, aceptada.
        assertTrue(validarCon(120, ej(X, 3, 10), ej(Y, 3, 10), cardio).aceptable());
    }

    @Test
    @DisplayName("la dificultad sigue con su regla propia: mismo patron, y anadir sin retirar se rechaza")
    void dificultadSinCambios() {
        var derivada = semana(List.of(ej(A, 3, 10), ej(B, 3, 10), ej(C, 3, 10), ej(X, 3, 10)));
        var r = v8c.validate(derivada, previa(null), orden(TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA),
                catalogo(), ExclusionesDeSeguridad.ninguna(), List.of(), false, 30);
        assertTrue(r.problems().stream().anyMatch(p -> p.contains("sin retirar ninguno")), r.problems().toString());
    }

    // =======================================================================

    private ConservationReport validar(PlanDraft.DraftExercise... nuevos) {
        return validarCon(30, nuevos);
    }

    private ConservationReport validarCon(int divisor, PlanDraft.DraftExercise... nuevos) {
        return v8c.validate(semana(List.of(nuevos)), previa("PAIN_OR_DISCOMFORT"),
                orden(TipoIntervencion.SEGURIDAD_ANTE_DOLOR), catalogo(), ExclusionesDeSeguridad.ninguna(),
                List.of(), false, divisor);
    }

    private static WeekDerivation semana(List<PlanDraft.DraftExercise> enS) {
        return new WeekDerivation(List.of(
                new DerivedWorkout(S, sesion(0, enS)),
                new DerivedWorkout(OTRA, sesion(2, List.of(ej(Q, 3, 10))))), List.of());
    }

    private static PlanDraft.DraftExercise ej(long id, int series, int reps) {
        return new PlanDraft.DraftExercise(id, PrescriptionType.SETS_REPS, (short) series, (short) reps, null, null,
                (short) 90, null);
    }

    private static PlanDraft.DraftWorkout sesion(int dia, List<PlanDraft.DraftExercise> l) {
        return new PlanDraft.DraftWorkout(LUNES.plusDays(dia).plusWeeks(1), WorkoutFocus.FULL_BODY, "Sesion", 40, l);
    }

    private static PreviousWeekSummary previa(String motivo) {
        var sesiones = List.of(
                new PreviousWeekSummary.WorkoutOutcome(S, LUNES, "FULL_BODY", "SKIPPED", motivo, true, "SKIPPED",
                        null, BigDecimal.ZERO, "Sesion", (short) 40),
                new PreviousWeekSummary.WorkoutOutcome(OTRA, LUNES.plusDays(2), "FULL_BODY", "COMPLETED", null, true,
                        "COMPLETED", (short) 6, new BigDecimal("100.00"), "Sesion", (short) 40));
        var prescritas = List.of(presc(S, LUNES, A), presc(S, LUNES, B), presc(S, LUNES, C),
                presc(OTRA, LUNES.plusDays(2), Q));
        return new PreviousWeekSummary(new BigDecimal("66.67"), new BigDecimal("6.0"), null, Map.of(), sesiones,
                prescritas, List.of());
    }

    private static PreviousWeekSummary.PrescriptionOutcome presc(long sesion, LocalDate dia, long id) {
        return new PreviousWeekSummary.PrescriptionOutcome(sesion, dia, id, "ejercicio " + id, "SETS_REPS",
                (short) 3, (short) 10, null, null, (short) 3, 30, null, null, new BigDecimal("100.00"),
                "COMPLETED", null, "COMPLETED", (short) 90);
    }

    private static Map<Long, CandidateExercise> catalogo() {
        var m = new LinkedHashMap<Long, CandidateExercise>();
        for (long id : new long[]{A, B, C, Q}) m.put(id, cand(id, "HORIZONTAL_PUSH"));
        for (long id : new long[]{X, Y, Z}) m.put(id, cand(id, "KNEE_DOMINANT"));
        m.put(W, cand(W, "LOCOMOTION_CARDIO"));
        return m;
    }

    private static CandidateExercise cand(long id, String patron) {
        return new CandidateExercise(id, "ej" + id, "chest", "body weight", 1, PrescriptionType.SETS_REPS,
                "pectorals", patron);
    }

    private static PlanAdjustment orden(TipoIntervencion t) {
        return new PlanAdjustment(List.of(AdjustmentType.NONE), null, null, null, 0.0, 0.0, null, null, null,
                "causa", null, null, List.of(new PlanAdjustment.TargetedSession(S, t)));
    }
}
