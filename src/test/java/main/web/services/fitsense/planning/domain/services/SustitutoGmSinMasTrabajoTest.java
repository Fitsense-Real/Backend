package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.configuration.domain.model.valueobjects.PrescriptionParams;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HALL-GM-01 · en GAIN_MUSCLE (4D) el sustituto de seguridad recibia la dosis BASE de su clase aunque la colocacion
 * quitada tuviera una dosis autorizada menor (mantenimiento P-A1 ante fatiga, 3x10 -> 2x10). Ante dolor se anadia
 * trabajo y se deshacia la reduccion. Ahora nunca supera el trabajo del quitado; si la base lo supera, el sustituto
 * conserva la dosis del quitado, que V24 rechaza (el sustituto no tiene ninguna autorizacion): no se publica.
 */
@DisplayName("HALL-GM-01 · en GAIN_MUSCLE el sustituto de seguridad nunca tiene mas trabajo que el quitado")
class SustitutoGmSinMasTrabajoTest {

    private static final LocalDate LUNES = LocalDate.of(2026, 11, 30);
    /** Estructural de empuje horizontal (pecho): base 4D de su clase. */
    private static final CandidateExercise SUSTITUTO = new CandidateExercise(72L, "ej72", "chest", "body weight", 1,
            PrescriptionType.SETS_REPS, "pectorals", "HORIZONTAL_PUSH");
    private static final MvpDosePolicy.Dosis BASE =
            MvpDosePolicy.dosisPara(MandatorySlot.of(SUSTITUTO), SUSTITUTO.bodyPartCode()).orElseThrow();

    @Test
    @DisplayName("el defecto: el quitado a mantenimiento (base - 1 serie) ya no da al sustituto la base completa")
    void mantenimientoNoSeDeshace() {
        assertTrue(BASE.series() >= 3, "premisa: estructural con mantenimiento posible " + BASE.series());
        var quitado = reps(118L, BASE.series() - 1, BASE.reps(), BASE.descansoSegundos());
        var d = SafetySubstitutionPolicy.dosis(SUSTITUTO, quitado, contexto());
        assertTrue(SafetySubstitutionPolicy.trabajo(d) <= SafetySubstitutionPolicy.trabajo(quitado), d.toString());
        assertEquals(BASE.series() - 1, (int) d.plannedSets());
        assertEquals(72L, (long) d.exerciseId());
    }

    @Test
    @DisplayName("y V24 no la acredita sin autorizacion: no es la base ni se transfiere el mantenimiento del quitado")
    void v24RechazaSinAutorizacion() {
        var d = SafetySubstitutionPolicy.dosis(SUSTITUTO,
                reps(118L, BASE.series() - 1, BASE.reps(), BASE.descansoSegundos()), contexto());
        boolean esBase = d.plannedSets() == BASE.series() && d.plannedReps() == BASE.reps()
                && d.restSeconds() == BASE.descansoSegundos();
        assertFalse(esBase, "la dosis no es la base: V24 la rechaza si no hay autorizacion");
        // Con una autorizacion HEREDADA del propio sustituto si seria admisible: la regla de V24 no cambia.
        assertTrue(ProgresionDosisPolicy.admite4D(BASE, d, DosisAutorizada.Origen.HEREDADA, true));
    }

    @Test
    @DisplayName("control: si la base no supera el trabajo del quitado, se mantiene la base (sin cambios)")
    void baseQueCabe() {
        for (int[] q : new int[][]{{3, 10, 90}, {3, 11, 90}, {3, 12, 120}, {4, 10, 90}}) {
            var d = SafetySubstitutionPolicy.dosis(SUSTITUTO, reps(1L, q[0], q[1], q[2]), contexto());
            if ((long) BASE.series() * BASE.reps() > (long) q[0] * q[1]) continue;
            assertEquals((int) BASE.series(), (int) d.plannedSets(), java.util.Arrays.toString(q));
            assertEquals((int) BASE.reps(), (int) d.plannedReps(), java.util.Arrays.toString(q));
            assertEquals((int) BASE.descansoSegundos(), (int) d.restSeconds(), java.util.Arrays.toString(q));
        }
    }

    @Test
    @DisplayName("nunca mas trabajo, para cualquier dosis del quitado")
    void nuncaMasTrabajo() {
        for (int s = 1; s <= 4; s++)
            for (int r = 6; r <= 15; r++) {
                var quitado = reps(1L, s, r, 90);
                var d = SafetySubstitutionPolicy.dosis(SUSTITUTO, quitado, contexto());
                assertTrue(SafetySubstitutionPolicy.trabajo(d) <= SafetySubstitutionPolicy.trabajo(quitado),
                        s + "x" + r + " -> " + d);
            }
    }

    private static PlanDraft.DraftExercise reps(long id, int series, int reps, int descanso) {
        return new PlanDraft.DraftExercise(id, PrescriptionType.SETS_REPS, (short) series, (short) reps, null, null,
                (short) descanso, null);
    }

    private static PlanGenerationContext contexto() {
        var perfil = new PlanningProfile(7L, 35, "FEMALE", new BigDecimal("165"), new BigDecimal("70"), null,
                "INTERMEDIATE", "GAIN_MUSCLE", "x", "Ninguna", "GYM", 3, List.of((short) 1, (short) 3, (short) 5), 45,
                List.of("body weight"), List.of(), SafetyProfile.Declaradas.sinLimitaciones());
        return new PlanGenerationContext(7L, (short) 4, LUNES, LUNES.plusDays(6), perfil, PlanAdjustment.none(),
                null, List.of(SUSTITUTO), SafetyProfile.none(), (PrescriptionParams) null);
    }
}
