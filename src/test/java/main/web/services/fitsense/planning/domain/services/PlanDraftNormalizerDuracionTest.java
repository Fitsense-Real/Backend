package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.configuration.domain.model.valueobjects.PrescriptionParams;
import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.GenerationSource;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanGenerationContext;
import main.web.services.fitsense.planning.domain.model.valueobjects.PrescriptionType;
import main.web.services.fitsense.planning.domain.model.valueobjects.WorkoutFocus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Orden de optimizacion (31/10/2026), B2: la duracion declarada de una propuesta de la IA la fija
 * el backend con el mismo estimador de V17. Once rechazos V17 de las 48 predicciones reales eran
 * minutos mal calculados por la IA sobre un contenido que, por lo demas, podia ser valido.
 */
class PlanDraftNormalizerDuracionTest {

    private static final LocalDate DIA = LocalDate.of(2026, 10, 19);
    private static final PrescriptionParams PARAMS =
            new PrescriptionParams(null, null, null, null, null, null, null, null, null, null);
    private final PlanDraftNormalizer normalizador = new PlanDraftNormalizer();

    @Test
    @DisplayName("Los minutos que declara la IA se sustituyen por los del contenido, y queda registrado")
    void fijaLaDuracion() {
        var sesion = sesion(30);
        int reales = new SessionDurationEstimator().estimateMinutes(sesion, PARAMS);
        assertNotEquals(30, reales, "la prueba necesita una declaracion equivocada");

        var r = normalizador.normalize(borrador(sesion), contexto());

        assertEquals(reales, r.draft().workouts().get(0).expectedDurationMinutes());
        assertTrue(r.corrections().stream().anyMatch(c -> c.startsWith("DURACION:")), r.corrections()::toString);
        assertEquals(sesion.exercises(), r.draft().workouts().get(0).exercises(), "el contenido no cambia");
    }

    @Test
    @DisplayName("Si la IA ya declaro lo correcto, no hay correccion")
    void sinCorreccion() {
        int reales = new SessionDurationEstimator().estimateMinutes(sesion(0), PARAMS);
        var r = normalizador.normalize(borrador(sesion(reales)), contexto());

        assertTrue(r.isClean(), r.corrections()::toString);
    }

    private static PlanDraft borrador(PlanDraft.DraftWorkout sesion) {
        return new PlanDraft(GenerationSource.AI, "modelo", "plan", null, null, List.of(sesion));
    }

    private static PlanDraft.DraftWorkout sesion(int minutos) {
        return new PlanDraft.DraftWorkout(DIA, WorkoutFocus.FULL_BODY, "A", minutos, List.of(
                ej(1), ej(2), ej(3), ej(4)));
    }

    private static PlanDraft.DraftExercise ej(long id) {
        return new PlanDraft.DraftExercise(id, PrescriptionType.SETS_REPS, (short) 3, (short) 10, null, null,
                (short) 90, null);
    }

    private static PlanGenerationContext contexto() {
        var catalogo = List.of(1L, 2L, 3L, 4L).stream().map(id -> new CandidateExercise(id, "e" + id, "upper legs",
                "body weight", 1, PrescriptionType.SETS_REPS, null, null)).toList();
        return new PlanGenerationContext(1L, (short) 1, DIA, DIA.plusDays(6), null, null, null, catalogo, null,
                PARAMS);
    }
}
