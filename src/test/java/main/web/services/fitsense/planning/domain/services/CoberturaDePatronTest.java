package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.MandatorySlot;
import main.web.services.fitsense.planning.domain.model.valueobjects.MvpDosePolicy;
import main.web.services.fitsense.planning.domain.model.valueobjects.PrescriptionType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("P15-C experimental: un aislado no cubre un patron principal, pero conserva su dosis")
class CoberturaDePatronTest {

    static CandidateExercise c(long id, String patron, Boolean aislado) {
        return new CandidateExercise(id, "x", "upper legs", "body weight", 1, PrescriptionType.SETS_REPS,
                "hamstrings", patron, false, aislado);
    }

    @AfterEach
    void apagar() {
        CoberturaDePatron.configurar(false);
    }

    @Test
    @DisplayName("desactivado (por defecto): identico a MandatorySlot.of para aislados, multiarticulares y desconocidos")
    void desactivado() {
        assertFalse(CoberturaDePatron.activa());
        for (var x : new CandidateExercise[]{c(1, "KNEE_DOMINANT", true), c(2, "KNEE_DOMINANT", false),
                c(3, "KNEE_DOMINANT", null), c(4, "TRUNK_FLEXION", true)})
            assertEquals(MandatorySlot.of(x), CoberturaDePatron.slot(x));
    }

    @Test
    @DisplayName("activado: el aislado no cubre; el multiarticular y el desconocido si; la dosis por slot no cambia")
    void activado() {
        CoberturaDePatron.configurar(true);
        var curl = c(1009, "KNEE_DOMINANT", true);
        assertEquals(Optional.empty(), CoberturaDePatron.slot(curl));
        assertEquals(Optional.of(MandatorySlot.LOWER_KNEE), CoberturaDePatron.slot(c(1205, "KNEE_DOMINANT", false)));
        assertEquals(Optional.of(MandatorySlot.LOWER_KNEE), CoberturaDePatron.slot(c(9, "KNEE_DOMINANT", null)));
        // La dosis sigue saliendo del slot del patron (MandatorySlot.of), no de la cobertura.
        assertEquals(Optional.of(MandatorySlot.LOWER_KNEE), MandatorySlot.of(curl));
        assertEquals(MvpDosePolicy.dosisPara(MandatorySlot.of(curl), "upper legs"),
                MvpDosePolicy.dosisPara(MandatorySlot.of(c(2, "KNEE_DOMINANT", false)), "upper legs"));
    }
}
