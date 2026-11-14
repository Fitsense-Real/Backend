package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.configuration.domain.model.valueobjects.PrescriptionParams;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HALL-PROG-01 · la dosis de un sustituto de seguridad (SUST-SEG-1) en los objetivos que se prescriben por capacidades
 * (4D v4).
 * <p>
 * Antes, el sustituto copiaba series, repeticiones y descanso del ejercicio quitado. Si el quitado llevaba una dosis
 * PROGRESADA (3x19 tras subir desde 3x18 de resistencia muscular), el sustituto la heredaba sin la autorizacion de
 * progresion, que es de la colocacion original, y V24 rechazaba la semana: "no lleva la dosis de ninguna capacidad".
 * Ahora la dosis del sustituto es siempre la de una capacidad admisible para el (la misma fuente que V24), nunca con
 * mas trabajo que el quitado, y preferentemente la de la misma capacidad (el mismo descanso).
 */
@DisplayName("HALL-PROG-01 · el sustituto de seguridad recibe una dosis de capacidad valida y nunca mas trabajo")
class SustitutoDosisCapacidadTest {

    private static final LocalDate LUNES = LocalDate.of(2026, 11, 30);
    /** Estructural de empuje horizontal: admite fuerza, hipertrofia y resistencia muscular. */
    private static final CandidateExercise SUSTITUTO = new CandidateExercise(72L, "ej72", "chest", "body weight", 1,
            PrescriptionType.SETS_REPS, "pectorals", "HORIZONTAL_PUSH");
    /** Cardio de duracion. */
    private static final CandidateExercise CARDIO = new CandidateExercise(1054L, "escalador", "cardio", "body weight",
            1, PrescriptionType.DURATION, "cardiovascular system", "LOCOMOTION_CARDIO");

    @Test
    @DisplayName("positivo: la dosis progresada 3x19/45 del quitado no pasa al sustituto; recibe 3x18/45 (resistencia)")
    void progresadaNoSeHereda() {
        var d = SafetySubstitutionPolicy.dosis(SUSTITUTO, reps(1179L, 3, 19, 45), contexto("GENERAL_FITNESS"));
        assertEquals(3, (int) d.plannedSets());
        assertEquals(18, (int) d.plannedReps());
        assertEquals(45, (int) d.restSeconds());
        assertEquals(72L, (long) d.exerciseId());
        assertTrue(acreditada(d, "GENERAL_FITNESS"), "V24 debe acreditar la dosis del sustituto: " + d);
    }

    @Test
    @DisplayName("regresion: una dosis que ya es exactamente de capacidad se conserva (3x10/90 hipertrofia)")
    void dosisDeCapacidadSeConserva() {
        var original = reps(500L, 3, 10, 90);
        var d = SafetySubstitutionPolicy.dosis(SUSTITUTO, original, contexto("GENERAL_FITNESS"));
        assertEquals(3, (int) d.plannedSets());
        assertEquals(10, (int) d.plannedReps());
        assertEquals(90, (int) d.restSeconds());
        assertTrue(acreditada(d, "GENERAL_FITNESS"));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"GENERAL_FITNESS", "LOSE_WEIGHT", "INCREASE_STRENGTH"})
    @DisplayName("ante dolor nunca se anade trabajo: el sustituto no supera series x repeticiones del quitado")
    void nuncaMasTrabajo(String objetivo) {
        for (int[] q : new int[][]{{3, 19, 45}, {3, 7, 150}, {3, 11, 90}, {2, 19, 45}, {3, 18, 45}, {3, 6, 150}}) {
            var original = reps(1L, q[0], q[1], q[2]);
            var d = SafetySubstitutionPolicy.dosis(SUSTITUTO, original, contexto(objetivo));
            assertTrue(SafetySubstitutionPolicy.trabajo(d) <= SafetySubstitutionPolicy.trabajo(original),
                    objetivo + " " + java.util.Arrays.toString(q) + " -> " + d);
        }
    }

    @Test
    @DisplayName("negativo: si ninguna dosis de capacidad cabe sin anadir trabajo, no se inventa una; queda la heredada")
    void sinDosisQueQuepa() {
        // 1x5/150: la menor dosis de capacidad del sustituto (fuerza 3x5) tiene mas trabajo. No se sube.
        var original = reps(1L, 1, 5, 150);
        var d = SafetySubstitutionPolicy.dosis(SUSTITUTO, original, contexto("GENERAL_FITNESS"));
        assertTrue(SafetySubstitutionPolicy.trabajo(d) <= SafetySubstitutionPolicy.trabajo(original), d.toString());
        assertFalse(acreditada(d, "GENERAL_FITNESS"),
                "la dosis heredada no es de capacidad: los validadores rechazaran la semana en vez de publicarla");
    }

    @Test
    @DisplayName("control: GAIN_MUSCLE sigue con su dosis 4D v3 (sin cambios)")
    void gainMuscleSinCambios() {
        var d = SafetySubstitutionPolicy.dosis(SUSTITUTO, reps(1L, 3, 11, 90), contexto("GAIN_MUSCLE"));
        var esperado = MvpDosePolicy.dosisPara(MandatorySlot.of(SUSTITUTO), SUSTITUTO.bodyPartCode()).orElseThrow();
        assertEquals((int) esperado.series(), (int) d.plannedSets());
        assertEquals((int) esperado.reps(), (int) d.plannedReps());
    }

    @Test
    @DisplayName("control: sin perfil (contexto incompleto) el comportamiento anterior no cambia")
    void sinPerfil() {
        var original = reps(1L, 3, 19, 45);
        var d = SafetySubstitutionPolicy.dosis(SUSTITUTO, original, null);
        assertEquals(3, (int) d.plannedSets());
        assertEquals(19, (int) d.plannedReps());
        assertEquals(45, (int) d.restSeconds());
    }

    @Test
    @DisplayName("control: el cardio de duracion con su dosis de capacidad se conserva")
    void cardioDuracion() {
        var original = new PlanDraft.DraftExercise(1053L, PrescriptionType.DURATION, (short) 3, null, 120, null,
                (short) 60, null);
        var d = SafetySubstitutionPolicy.dosis(CARDIO, original, contexto("GENERAL_FITNESS"));
        assertEquals(PrescriptionType.DURATION, d.prescriptionType());
        assertEquals(3, (int) d.plannedSets());
        assertEquals(120, (int) d.plannedDurationSeconds());
        assertEquals(60, (int) d.restSeconds());
    }

    // =======================================================================

    private static boolean acreditada(PlanDraft.DraftExercise d, String objetivo) {
        return CapabilityPrescriptionPolicy.acreditada(d, SUSTITUTO, objetivo, "INTERMEDIATE", null).isPresent();
    }

    private static PlanDraft.DraftExercise reps(long id, int series, int reps, int descanso) {
        return new PlanDraft.DraftExercise(id, PrescriptionType.SETS_REPS, (short) series, (short) reps, null, null,
                (short) descanso, null);
    }

    private static PlanGenerationContext contexto(String objetivo) {
        var perfil = new PlanningProfile(7L, 35, "FEMALE", new BigDecimal("165"), new BigDecimal("70"), null,
                "INTERMEDIATE", objetivo, "x", "Ninguna", "GYM", 3, List.of((short) 1, (short) 3, (short) 5), 45,
                List.of("body weight"), List.of(), SafetyProfile.Declaradas.sinLimitaciones());
        return new PlanGenerationContext(7L, (short) 4, LUNES, LUNES.plusDays(6), perfil, PlanAdjustment.none(),
                null, List.of(SUSTITUTO, CARDIO), SafetyProfile.none(), (PrescriptionParams) null);
    }
}
