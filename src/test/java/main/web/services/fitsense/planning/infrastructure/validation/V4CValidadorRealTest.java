package main.web.services.fitsense.planning.infrastructure.validation;

import main.web.services.fitsense.configuration.domain.model.valueobjects.PrescriptionParams;
import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.domain.services.DoseEnforcement;
import main.web.services.fitsense.planning.domain.services.PlanDraftValidator;
import main.web.services.fitsense.planning.domain.services.SessionDurationEstimator;
import main.web.services.fitsense.planning.domain.services.SlotCoverageEnforcement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * V4-C con el VALIDADOR REAL: segundos estimados frente a M x 60 x 1,15, sin depender de lo declarado. La politica
 * historica conserva su comportamiento (minutos declarados frente a round(M x 1,15)).
 */
@DisplayName("V4-C en el validador real (segundos estimados) frente a V4 historico")
class V4CValidadorRealTest {

    static final LocalDate LUNES = LocalDate.of(2026, 10, 12);
    static final String[] ZONAS = {"chest", "back", "upper legs", "shoulders", "upper arms", "waist", "lower legs"};
    static final PrescriptionParams P = new PrescriptionParams(0, 3, 60, 5, 60, 20, null, null, 10, 180);
    static final PlanDraftValidator VALIDADOR = new PlanDraftValidator(new SessionDurationEstimator(),
            SlotCoverageEnforcement.OBSERVE, DoseEnforcement.OBSERVE);

    static List<String> v4(PlanDraft d, PlanGenerationContext ctx) {
        try {
            VALIDADOR.validate(d, ctx, 30, ConservationReport.vacio());
            return List.of();
        } catch (InvalidPlanDraftException e) {
            return e.problems().stream().filter(p -> p.startsWith("V4:")).toList();
        }
    }

    /** Una sesion con un ejercicio de DURACION de una serie: segundos = 300 + duracion. Control al segundo. */
    static PlanDraft duracion(int segundosTrabajo, int declarados) {
        var e = new PlanDraft.DraftExercise(1L, PrescriptionType.DURATION, (short) 1, null, segundosTrabajo, null,
                (short) 60, null);
        var e2 = new PlanDraft.DraftExercise(2L, PrescriptionType.DURATION, (short) 1, null, 30, null, (short) 60, null);
        return new PlanDraft(GenerationSource.AI, "m", "p", null, "r", List.of(new PlanDraft.DraftWorkout(LUNES,
                WorkoutFocus.FULL_BODY, "s", declarados, List.of(e, e2))));
    }

    static PlanGenerationContext ctx(int minutos, PlanningPolicy politica) {
        var perfil = new PlanningProfile(1L, 30, "FEMALE", new BigDecimal("168"), new BigDecimal("68"), null,
                "INTERMEDIATE", "GENERAL_FITNESS", "x", "Ninguna", "HOME", 3, List.of((short) 1, (short) 3, (short) 5),
                minutos, List.of("body weight"), List.of(), SafetyProfile.Declaradas.sinLimitaciones());
        var c = List.of(new CandidateExercise(1L, "plancha", "waist", "body weight", 1, PrescriptionType.DURATION, null,
                        null),
                new CandidateExercise(2L, "plancha lateral", "waist", "body weight", 1, PrescriptionType.DURATION, null,
                        null));
        return new PlanGenerationContext(1L, (short) 1, LUNES, LUNES.plusDays(6), perfil, PlanAdjustment.none(), null,
                c, SafetyProfile.none(), P, politica);
    }

    /**
     * Segundos estimados = 300 (calentamiento) + trabajo + 30 + 60 (transicion). Se elige el trabajo para quedar en
     * el limite exacto, un segundo por debajo y uno por encima, y se declara lo correcto, menos y mas.
     */
    @ParameterizedTest(name = "{0} min: {1} s estimados declarando {2} min -> V4-C rechaza={3}")
    @CsvSource({
            "30,2069,35,false", "30,2070,35,false", "30,2071,35,true", "30,2071,30,true", "30,2069,60,false",
            "45,3104,52,false", "45,3105,52,false", "45,3106,40,true", "45,3105,90,false",
            "60,4139,69,false", "60,4140,69,false", "60,4141,69,true", "60,4141,50,true",
            "75,5174,86,false", "75,5175,86,false", "75,5176,86,true", "75,5176,60,true"})
    void bordes(int minutos, int segundosEstimados, int declarados, boolean rechaza) {
        var d = duracion(segundosEstimados - 300 - 30 - 60, declarados);
        var c = ctx(minutos, PlanningPolicy.V4C);
        assertEquals(segundosEstimados, new SessionDurationEstimator().estimateSeconds(d.workouts().get(0), P));
        assertEquals(rechaza, !v4(d, c).isEmpty(), v4(d, c).toString());
    }

    @Test
    @DisplayName("historica intacta: compara lo declarado (60 > 35 rechaza; 35 con 2.071 s pasa)")
    void historicaIntacta() {
        assertFalse(v4(duracion(60, 60), ctx(30, PlanningPolicy.HISTORICA)).isEmpty(), "declara 60 en un perfil de 30");
        assertTrue(v4(duracion(2071 - 390, 35), ctx(30, PlanningPolicy.HISTORICA)).isEmpty(),
                "con la historica cuenta lo declarado");
    }

    @Test
    @DisplayName("el caso del 142 %: 2.550 s declarando 35 min en un perfil de 30. Historica lo admite; V4-C lo rechaza")
    void caso142() {
        var ejercicios = new ArrayList<PlanDraft.DraftExercise>();
        var candidatos = new ArrayList<CandidateExercise>();
        for (long id = 1; id <= 7; id++) {
            ejercicios.add(new PlanDraft.DraftExercise(id, PrescriptionType.SETS_REPS, (short) 3, (short) 10, null, null,
                    (short) 90, null));
            candidatos.add(new CandidateExercise(id, "e" + id, ZONAS[(int) id - 1], "body weight", 1,
                    PrescriptionType.SETS_REPS, null, null));
        }
        var d = new PlanDraft(GenerationSource.AI, "m", "p", null, "r", List.of(new PlanDraft.DraftWorkout(LUNES,
                WorkoutFocus.FULL_BODY, "s", 35, ejercicios)));
        var base = ctx(30, PlanningPolicy.HISTORICA);
        var hist = new PlanGenerationContext(base.userId(), base.weekNumber(), base.weekStartDate(), base.weekEndDate(),
                base.profile(), base.adjustment(), null, candidatos, base.safety(), P);
        assertEquals(2550, new SessionDurationEstimator().estimateSeconds(d.workouts().get(0), P));
        assertTrue(v4(d, hist).isEmpty(), "historica: declara 35 <= 35");
        var r = v4(d, hist.conPolitica(PlanningPolicy.V4C));
        assertEquals(1, r.size(), r.toString());
        assertTrue(r.get(0).contains("2550 s") && r.get(0).contains("2070 s"), r.get(0));
    }
}
