package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanningPolicy;
import main.web.services.fitsense.planning.domain.model.valueobjects.PrescriptionType;
import main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Fase 6: el prompt y la factibilidad usan el mismo criterio de sustituto que V8C (P-A2 y P-C2). */
@DisplayName("admiteComoSustituto: prompt, factibilidad y V8C dicen lo mismo")
class AdmiteComoSustitutoTest {

    static CandidateExercise c(long id, String nombre, String musculo, int dificultad, boolean impacto) {
        return new CandidateExercise(id, nombre, "cardio", "body weight", dificultad, PrescriptionType.DURATION,
                musculo, "LOCOMOTION_CARDIO", impacto);
    }

    static final CandidateExercise BICI = c(1, "bicicleta estatica", "cardiovascular system", 1, false);
    static final CandidateExercise SALTOS = c(2, "saltos con las piernas separadas", "cardiovascular system", 1, false);
    static final CandidateExercise BURPEE = c(3, "burpee", "cardiovascular system", 1, true);

    @Test
    void varianteNoSubeImpactoConRev2() {
        var v = TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA;
        assertFalse(PermisosIntervencion.admiteComoSustituto(v, PlanningPolicy.REV2, BICI, BURPEE), "BAJO -> ALTO");
        assertFalse(PermisosIntervencion.admiteComoSustituto(v, PlanningPolicy.REV2, BICI, SALTOS), "BAJO -> MODERADO");
        assertTrue(PermisosIntervencion.admiteComoSustituto(v, PlanningPolicy.REV2, BURPEE, BICI), "ALTO -> BAJO");
        assertTrue(PermisosIntervencion.admiteComoSustituto(v, PlanningPolicy.HISTORICA, BICI, BURPEE),
                "con la historica no cambia nada");
    }

    @Test
    void desmotivacionSoloEquivalentesEstrictos() {
        var m = TipoIntervencion.INVESTIGAR_CAMBIO_MINIMO;
        assertFalse(PermisosIntervencion.admiteComoSustituto(m, PlanningPolicy.REV2, BICI, BURPEE));
        assertTrue(PermisosIntervencion.admiteComoSustituto(m, PlanningPolicy.HISTORICA, BICI, BURPEE));
    }
}
