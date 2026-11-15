package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * V8C · LA IA NUNCA SUBE DOSIS, TAMPOCO SOBRE UN EJERCICIO QUE LUEGO SE SUSTITUYE (HALL-PROG-01).
 * <p>
 * Desde la correccion de HALL-PROG-01, el sustituto SUST-SEG-1 ya no hereda la dosis del ejercicio quitado: recibe una
 * dosis de capacidad sin mas trabajo. Por eso la subida que la IA hizo sobre el quitado ya no llega al sustituto y la
 * comprobacion por sustituto ({@link AdaptedWeekProducer#subidasPorSustitucion}) no la ve. Esta comprobacion compara la
 * dosis que la IA dio al quitado con la que tenia antes de la IA, para que la propuesta se siga rechazando.
 */
@DisplayName("V8C · una subida de la IA sobre un ejercicio sustituido se rechaza aunque el sustituto no la herede")
class SubidaIaEnQuitadoTest {

    private static final LocalDate DIA = LocalDate.of(2026, 11, 12);
    private static final long S = 2L, QUITADO = 1222L, PUESTO = 56L;

    @Test
    @DisplayName("O12: la IA sube 1222 de 3x18 a 4x18 y SUST-SEG-1 lo sustituye: se rechaza")
    void subeSeries() {
        var p = revisar(ej(QUITADO, 3, 18), ej(QUITADO, 4, 18));
        assertEquals(1, p.size(), p.toString());
        assertTrue(p.get(0).contains("sube la dosis de 1222 antes de que SUST-SEG-1 lo sustituya por 56"), p.get(0));
        assertTrue(p.get(0).contains("4x18") && p.get(0).contains("3x18"), p.get(0));
    }

    @Test
    @DisplayName("sube repeticiones o segundos: se rechaza")
    void subeRepeticionesOSegundos() {
        assertEquals(1, revisar(ej(QUITADO, 3, 18), ej(QUITADO, 3, 20)).size());
        assertEquals(1, revisar(dur(QUITADO, 3, 60), dur(QUITADO, 3, 90)).size());
    }

    @Test
    @DisplayName("control: la IA baja o mantiene la dosis del quitado: no hay rechazo")
    void bajarOMantener() {
        assertEquals(List.of(), revisar(ej(QUITADO, 3, 18), ej(QUITADO, 2, 18)));
        assertEquals(List.of(), revisar(ej(QUITADO, 3, 18), ej(QUITADO, 3, 18)));
        assertEquals(List.of(), revisar(dur(QUITADO, 3, 60), dur(QUITADO, 3, 45)));
    }

    @Test
    @DisplayName("control: la IA quita el ejercicio (no esta en su semana): nada que comparar")
    void quitadoPorLaIa() {
        var antes = List.of(new DerivedWorkout(S, sesion(ej(QUITADO, 3, 18))));
        var ia = List.of(new DerivedWorkout(S, sesion(ej(999L, 3, 18))));
        assertEquals(List.of(), AdaptedWeekProducer.subidasDeLaIaEnQuitados(antes, ia, List.of(sustitucion())));
    }

    @Test
    @DisplayName("control: una subida en un ejercicio NO sustituido no la juzga esta regla (la juzga V8C general)")
    void noSustituido() {
        var antes = List.of(new DerivedWorkout(S, sesion(ej(500L, 3, 18))));
        var ia = List.of(new DerivedWorkout(S, sesion(ej(500L, 4, 18))));
        assertEquals(List.of(), AdaptedWeekProducer.subidasDeLaIaEnQuitados(antes, ia, List.of(sustitucion())));
    }

    // =======================================================================

    private static List<String> revisar(PlanDraft.DraftExercise antes, PlanDraft.DraftExercise conIa) {
        return AdaptedWeekProducer.subidasDeLaIaEnQuitados(List.of(new DerivedWorkout(S, sesion(antes))),
                List.of(new DerivedWorkout(S, sesion(conIa))), List.of(sustitucion()));
    }

    private static SustitucionSeguridad sustitucion() {
        return new SustitucionSeguridad(S, DIA, QUITADO, "HORIZONTAL_PUSH", PUESTO, "dolor_registrado", "SUST-SEG-1",
                null, null);
    }

    private static PlanDraft.DraftExercise ej(long id, int series, int reps) {
        return new PlanDraft.DraftExercise(id, PrescriptionType.SETS_REPS, (short) series, (short) reps, null, null,
                (short) 45, null);
    }

    private static PlanDraft.DraftExercise dur(long id, int series, int segundos) {
        return new PlanDraft.DraftExercise(id, PrescriptionType.DURATION, (short) series, null, segundos, null,
                (short) 60, null);
    }

    private static PlanDraft.DraftWorkout sesion(PlanDraft.DraftExercise e) {
        return new PlanDraft.DraftWorkout(DIA, WorkoutFocus.FULL_BODY, "Sesion " + S, 40, List.of(e));
    }
}
