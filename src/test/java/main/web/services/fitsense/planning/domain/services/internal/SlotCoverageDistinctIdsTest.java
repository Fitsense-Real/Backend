package main.web.services.fitsense.planning.domain.services.internal;

import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.MandatorySlot;
import main.web.services.fitsense.planning.domain.model.valueobjects.PrescriptionType;
import main.web.services.fitsense.planning.domain.model.valueobjects.SlotCoverage;
import main.web.services.fitsense.planning.domain.services.SlotCoveragePolicy;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SlotCoverageDistinctIdsTest {

    private CandidateExercise exercise(long id, String pattern) {
        return new CandidateExercise(id, "ID " + id, "back", "body weight", 1,
                PrescriptionType.SETS_REPS, "lats", pattern);
    }

    @Test
    void filasRepetidasDeUnMismoIdNoConviertenUnPoolFragilEnRobusto() {
        var candidates = List.of(
                exercise(1, "KNEE_DOMINANT"), exercise(2, "HIP_HINGE"),
                exercise(3, "HORIZONTAL_PUSH"), exercise(4, "HORIZONTAL_PULL"));
        var duplicated = new ArrayList<CandidateExercise>();
        for (int repetition = 0; repetition < 3; repetition++) duplicated.addAll(candidates);

        var result = SlotCoveragePolicy.ofEligible(duplicated);
        assertEquals(SlotCoverage.Verdict.FRAGIL, result.verdict());
        for (var slot : MandatorySlot.ALL) assertEquals(1, result.count(slot));
    }

    @Test
    void tresIdsDistintosPorSlotSiguenSiendoRobustos() {
        var list = new ArrayList<CandidateExercise>();
        for (int i = 0; i < 3; i++) {
            list.add(exercise(10 + i, "KNEE_DOMINANT"));
            list.add(exercise(20 + i, "HIP_HINGE"));
            list.add(exercise(30 + i, "HORIZONTAL_PUSH"));
            list.add(exercise(40 + i, "HORIZONTAL_PULL"));
        }
        assertEquals(SlotCoverage.Verdict.ROBUSTO,
                SlotCoveragePolicy.ofEligible(list).verdict());
    }

    @Test
    void faltanteEnUnSlotSigueSiendoNoFactible() {
        var result = SlotCoveragePolicy.ofEligible(List.of(
                exercise(1,"KNEE_DOMINANT"), exercise(3,"HORIZONTAL_PUSH"),
                exercise(4,"HORIZONTAL_PULL")));
        assertEquals(SlotCoverage.Verdict.NO_FACTIBLE, result.verdict());
        assertEquals(0, result.count(MandatorySlot.LOWER_HIP));
    }

    @Test
    void idNuloNoCuentaComoCandidato() {
        var result = SlotCoveragePolicy.ofEligible(List.of(
                new CandidateExercise(null, "sin id", "upper legs", "body weight", 1,
                        PrescriptionType.SETS_REPS, "glutes", "HIP_HINGE"),
                exercise(1, "KNEE_DOMINANT"), exercise(3, "HORIZONTAL_PUSH"),
                exercise(4, "HORIZONTAL_PULL")));
        assertEquals(SlotCoverage.Verdict.NO_FACTIBLE, result.verdict());
        assertEquals(0, result.count(MandatorySlot.LOWER_HIP));
    }
}
