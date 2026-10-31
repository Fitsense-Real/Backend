package main.web.services.fitsense.planning.application.internal.commandservices;

import main.web.services.fitsense.planning.domain.model.entities.PlannedWorkout;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Orden de cierre del MVP (31/10/2026), §7: progresion y reincorporacion con las dosis excepcionales de
 * 4D v3.2 (relleno de zona pequena 2x13 -> 3x13) y 4D v3.3 (biceps de relleno en PULL 2x10 -> 3x10).
 * Una serie adicional autorizada no es una dosis base nueva: el ejercicio que vuelve lo hace con la base.
 */
class Reincorporacion4DTest {

    static final CandidateExercise GEMELO = new CandidateExercise(10L, "Elevacion de talones", "lower legs",
            "body weight", 1, PrescriptionType.SETS_REPS, "calves", null);
    static final CandidateExercise CURL = new CandidateExercise(20L, "Curl con banda", "upper arms",
            "band", 1, PrescriptionType.SETS_REPS, "biceps", "ELBOW_FLEXION");

    @Test
    @DisplayName("4D v3.2: un relleno de zona pequena retirado con 3x13 vuelve con 2x13, no con la ampliada")
    void zonaPequenaVuelveConLaBase() {
        var x = retirado(10L, 3, 13);
        var vuelta = ProgresionResolver.exacta(x, GEMELO, contexto());
        assertEquals(dosis(2, 13, 90), dosis(vuelta), "la reincorporacion no copia la dosis ampliada");
    }

    @Test
    @DisplayName("4D v3.3: el biceps de relleno retirado con 3x10 vuelve con 2x10")
    void bicepsVuelveConLaBase() {
        var x = retirado(20L, 3, 10);
        var vuelta = ProgresionResolver.exacta(x, CURL, contexto());
        assertEquals(dosis(2, 10, 90), dosis(vuelta));
    }

    @Test
    @DisplayName("tampoco se restaura una dosis historica progresada (2x15): la base es la de la politica")
    void noRestauraLaHistorica() {
        var vuelta = ProgresionResolver.exacta(retirado(10L, 2, 15), GEMELO, contexto());
        assertEquals(dosis(2, 13, 90), dosis(vuelta));
    }

    @Test
    @DisplayName("progresar desde la ampliada 3x13 respeta los limites: +2 repeticiones y 3 series como mucho")
    void progresionDesdeLaAmpliada() {
        var rango = ProgresionResolver.rango(ej(10L, 3, 13), GEMELO, contexto());
        assertNotNull(rango, "la ampliada autorizada se reconoce como dosis de partida");
        assertEquals(3, rango.seriesMax());
        assertEquals(15, rango.repsMax());
        assertTrue(rango.admite(ej(10L, 3, 14)));
        assertTrue(rango.admite(ej(10L, 3, 15)));
        assertFalse(rango.admite(ej(10L, 3, 16)), "mas de +2 repeticiones");
        assertFalse(rango.admite(ej(10L, 4, 13)), "mas de 3 series");
    }

    @Test
    @DisplayName("el biceps ampliado a 3x10 progresa como relleno: hasta 3x12 y nunca 4 series")
    void progresionDelBicepsAmpliado() {
        var rango = ProgresionResolver.rango(ej(20L, 3, 10), CURL, contexto());
        assertNotNull(rango);
        assertTrue(rango.admite(ej(20L, 3, 11)));
        assertTrue(rango.admite(ej(20L, 3, 12)));
        assertFalse(rango.admite(ej(20L, 3, 13)));
        assertFalse(rango.admite(ej(20L, 4, 10)));
    }

    // =======================================================================

    private static main.web.services.fitsense.planning.domain.model.entities.PlannedWorkoutExercise retirado(
            long id, int series, int reps) {
        var w = new PlannedWorkout(null, LocalDate.of(2026, 11, 6), WorkoutFocus.PULL, "Traccion", 30, (short) 1,
                OffsetDateTime.now().plusDays(1));
        return w.addExercise(id, PrescriptionType.SETS_REPS, (short) series, (short) reps, null, null, (short) 90, null);
    }

    private static PlanDraft.DraftExercise ej(long id, int series, int reps) {
        return new PlanDraft.DraftExercise(id, PrescriptionType.SETS_REPS, (short) series, (short) reps, null, null,
                (short) 90, null);
    }

    private static String dosis(int s, int r, int d) {
        return s + "x" + r + "/" + d;
    }

    private static String dosis(PlanDraft.DraftExercise e) {
        return e.plannedSets() + "x" + e.plannedReps() + "/" + e.restSeconds();
    }

    private static PlanGenerationContext contexto() {
        var perfil = new PlanningProfile(1L, 30, "M", new BigDecimal("175"), new BigDecimal("75"),
                new BigDecimal("72"), "INTERMEDIATE", "GAIN_MUSCLE", "ganar musculo", null,
                "HOME", 2, List.of((short) 1, (short) 4), 30, List.of("body weight", "band"), List.of(),
                SafetyProfile.Declaradas.sinLimitaciones());
        return new PlanGenerationContext(1L, (short) 2, LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 8), perfil,
                PlanAdjustment.none(), null, List.of(GEMELO, CURL), null, null);
    }
}
