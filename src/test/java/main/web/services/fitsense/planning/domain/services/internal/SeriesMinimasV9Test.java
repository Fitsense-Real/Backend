package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.model.valueobjects.PrescriptionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * V9 · EL MINIMO DE SERIES TAMBIEN EN LOS EJERCICIOS POR DURACION (correccion 2, 27/10/2026).
 * <p>
 * La base lo exige ({@code ck_pwe_min_sets}: series nulas o >= 2) para cualquier
 * tipo. V9 solo lo comprobaba en SETS_REPS, asi que dos propuestas reales de
 * Replicate ({@code 1150: 1x30 s}, {@code 1325: 1x120 s}) pasaron la validacion y
 * reventaron al guardar: semana sin plan y sin evidencia. Ahora se rechazan en
 * la validacion y la IA puede corregirlas en el reintento.
 */
@DisplayName("V9 · series minimas tambien en DURATION, sin exigir series donde pueden faltar")
class SeriesMinimasV9Test {

    @Test
    @DisplayName("los dos casos reales (1x30 s y 1x120 s) se rechazan por V9, no por la base")
    void casosReales() {
        for (var e : List.of(duracion(1150, 1, 30), duracion(1325, 1, 120))) {
            var p = problemas(e);
            assertTrue(p.stream().anyMatch(x -> x.startsWith("V9: el ejercicio " + e.exerciseId() + " baja de 2 series")),
                    p.toString());
        }
    }

    @Test
    @DisplayName("DURATION con 2 series o mas: valido")
    void duracionConDosSeries() {
        assertEquals(List.of(), problemas(duracion(1150, 2, 30)));
        assertEquals(List.of(), problemas(duracion(1150, 3, 120)));
    }

    @Test
    @DisplayName("DURATION sin series (un bloque continuo, p. ej. cardio de 600 s): sigue siendo valido")
    void duracionSinSeries() {
        assertEquals(List.of(), problemas(duracion(1133, null, 600)));
    }

    @Test
    @DisplayName("los segundos se siguen validando aparte (V10)")
    void segundosAparte() {
        var p = problemas(duracion(1150, 1, 10));
        assertTrue(p.stream().anyMatch(x -> x.startsWith("V9:")), p.toString());
        assertTrue(p.stream().anyMatch(x -> x.startsWith("V10:")), p.toString());
    }

    @Test
    @DisplayName("SETS_REPS: el minimo de siempre")
    void repeticionesComoSiempre() {
        assertTrue(problemas(new PlanDraft.DraftExercise(7L, PrescriptionType.SETS_REPS, (short) 1, (short) 10, null,
                null, (short) 90, null)).stream().anyMatch(x -> x.startsWith("V9:")));
        assertEquals(List.of(), problemas(new PlanDraft.DraftExercise(7L, PrescriptionType.SETS_REPS, (short) 2,
                (short) 10, null, null, (short) 90, null)));
    }

    private static PlanDraft.DraftExercise duracion(long id, Integer series, int segundos) {
        return new PlanDraft.DraftExercise(id, PrescriptionType.DURATION, series == null ? null : series.shortValue(),
                null, segundos, null, (short) 60, null);
    }

    private static List<String> problemas(PlanDraft.DraftExercise e) {
        var p = new ArrayList<String>();
        PlanDraftValidator.validatePrescription(e, p);
        return p;
    }
}
