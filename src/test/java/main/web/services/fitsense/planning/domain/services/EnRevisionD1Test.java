package main.web.services.fitsense.planning.domain.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static main.web.services.fitsense.planning.domain.model.valueobjects.PrescriptionType.valueOf;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * D1 (30/10/2026): 853 y 1117 salen de revision solo con la clasificacion revisada en el
 * catalogo (cardio continuo por tiempo). Con el catalogo anterior siguen en revision, asi que
 * las referencias de V35, V36 y V37 no cambian; el resto de la lista sigue igual.
 */
@DisplayName("D1 · 853 y 1117 salen de revision solo con su clasificacion revisada")
class EnRevisionD1Test {

    @ParameterizedTest(name = "{0} {1} {2} -> en revision: {3}")
    @CsvSource({
            // 853: V37.1 lo deja LOCOMOTION_CARDIO por tiempo; antes era KNEE_DOMINANT.
            "853, LOCOMOTION_CARDIO, DURATION, false",
            "853, KNEE_DOMINANT, DURATION, true",
            // 1117: V38 lo pasa a DURATION; con SETS_REPS no tiene dosis de cardio y sigue en revision.
            "1117, LOCOMOTION_CARDIO, DURATION, false",
            "1117, LOCOMOTION_CARDIO, SETS_REPS, true",
            // D2: 298 sigue en revision aunque sea cardio por tiempo.
            "298, LOCOMOTION_CARDIO, DURATION, true",
            // Fuera de la lista: nunca en revision.
            "1054, LOCOMOTION_CARDIO, DURATION, false",
    })
    void condicion(long id, String patron, String prescripcion, boolean esperado) {
        assertEquals(esperado, GoalFeasibilityPolicy.enRevision(id, patron, valueOf(prescripcion)));
    }
}
