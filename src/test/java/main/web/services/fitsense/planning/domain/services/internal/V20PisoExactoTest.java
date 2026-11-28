package main.web.services.fitsense.planning.domain.services.internal;

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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * V20 AL 70 % EXACTO, EN SEGUNDOS (decision del 30/10/2026, PROTOTIPOS_V20.md).
 * <p>
 * Antes V20 comparaba dos enteros redondeados: el piso se truncaba
 * ({@code 45 x 70 / 100 = 31}) y la duracion se redondeaba al minuto, asi que
 * con 45 minutos aceptaba desde 1.830 s (67,8 %). Ahora compara los segundos
 * del estimador, sin redondear, con {@code ceil(minutos x 60 x 70 / 100)}:
 * 1.890 s con 45 minutos y 3.150 s con 75.
 */
@DisplayName("V20 · el piso es el 70 % exacto en segundos, sin redondeo")
class V20PisoExactoTest {

    private static final LocalDate LUNES = LocalDate.of(2026, 11, 2);
    private static final SessionDurationEstimator ESTIMADOR = new SessionDurationEstimator();
    /** Calentamiento 5 min, transicion 60 s, 3 s por repeticion: los valores por omision (V14). */
    private static final PrescriptionParams PARAMETROS =
            new PrescriptionParams(null, null, null, null, null, null, null, null, null, null);
    private static final PlanDraftValidator VALIDADOR = new PlanDraftValidator(ESTIMADOR,
            SlotCoverageEnforcement.ENFORCE_AI_ONLY, DoseEnforcement.ENFORCE);

    @ParameterizedTest(name = "{0} min: el piso es {1} s")
    @CsvSource({"30, 1260", "45, 1890", "60, 2520", "75, 3150"})
    void pisoEnSegundos(int minutos, long piso) {
        assertEquals(piso, PlanDraftValidator.minimumSessionSeconds(contexto(minutos)));
    }

    @ParameterizedTest(name = "{0} min y {1} s: V20 {2}")
    @CsvSource({
            // Los que antes pasaban solo por redondeo (31 min estimados frente a 31).
            "45, 1830, rechaza",
            "45, 1836, rechaza",
            "45, 1889, rechaza",
            "45, 1890, acepta",
            "45, 1891, acepta",
            "30, 1259, rechaza",
            "30, 1260, acepta",
            "60, 2519, rechaza",
            "60, 2520, acepta",
            "75, 3149, rechaza",
            "75, 3150, acepta",
    })
    void comparaSegundosSinRedondear(int minutos, int segundos, String veredicto) {
        var problemas = problemasV20(minutos, segundos);
        if ("acepta".equals(veredicto)) assertEquals(List.of(), problemas);
        else {
            assertEquals(1, problemas.size(), problemas.toString());
            long piso = PlanDraftValidator.minimumSessionSeconds(contexto(minutos));
            assertTrue(problemas.get(0).contains("dura unos %d s y el minimo es %d s".formatted(segundos, piso)),
                    problemas.get(0));
            assertTrue(problemas.get(0).contains("le faltan %d s".formatted(piso - segundos)), problemas.get(0));
        }
    }

    @Test
    @DisplayName("1.830 s se redondean a 31 minutos, el piso entero de 45: ya no basta")
    void elRedondeoYaNoDecide() {
        var sesion = sesionDe(1830);
        assertEquals(1830, ESTIMADOR.estimateSeconds(sesion, PARAMETROS));
        assertEquals(31, ESTIMADOR.estimateMinutes(sesion, PARAMETROS));
        assertEquals(31, PlanDraftValidator.minimumSessionMinutes(contexto(45)));
        assertFalse(problemasV20(45, 1830).isEmpty());
    }

    /** Solo los problemas de V20 que da el validador real para una sesion de esos segundos. */
    private static List<String> problemasV20(int minutos, int segundos) {
        var borrador = new PlanDraft(GenerationSource.RULE_ENGINE, null, "medicion", null, null,
                List.of(sesionDe(segundos)));
        try {
            VALIDADOR.validate(borrador, contexto(minutos), 30);
            return List.of();
        } catch (InvalidPlanDraftException e) {
            return e.problems().stream().filter(p -> p.startsWith("V20")).toList();
        }
    }

    /** Calentamiento de 5 min y un unico ejercicio por tiempo: total exacto, sin transiciones. */
    private static PlanDraft.DraftWorkout sesionDe(int segundosTotales) {
        int trabajo = segundosTotales - 5 * 60;
        var ejercicio = new PlanDraft.DraftExercise(1L, PrescriptionType.DURATION, (short) 1, null,
                trabajo, null, (short) 60, null);
        return new PlanDraft.DraftWorkout(LUNES, WorkoutFocus.FULL_BODY, "medicion", 1, List.of(ejercicio));
    }

    private static PlanGenerationContext contexto(int minutos) {
        var perfil = new PlanningProfile(1L, 30, "MALE", new BigDecimal("175.0"), new BigDecimal("72.0"),
                null, "BEGINNER", "GENERAL_FITNESS", "evaluacion", "Ninguna", "HOME", 3,
                List.of((short) 1, (short) 3, (short) 5), minutos, List.of("body weight"), List.of(),
                SafetyProfile.Declaradas.sinLimitaciones());
        return new PlanGenerationContext(1L, (short) 1, LUNES, LUNES.plusDays(6), perfil, PlanAdjustment.none(),
                null, List.of(), SafetyProfile.none(), PARAMETROS);
    }
}
