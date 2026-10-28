package main.web.services.fitsense.planning.domain.services.internal;

import main.web.services.fitsense.configuration.domain.model.valueobjects.PrescriptionParams;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.domain.services.PlanDraftValidator;
import main.web.services.fitsense.planning.domain.services.SessionDurationEstimator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DEFECTO CONOCIDO DE V20 (evaluacion del 28/10/2026, sin corregir todavia).
 * <p>
 * V20 debe exigir el 70 % de los minutos configurados. Hoy compara dos enteros
 * redondeados: el piso se TRUNCA ({@code 45 x 70 / 100 = 31}, no 31,5) y la
 * duracion estimada se REDONDEA al minuto ({@code Math.round(1830 s / 60) = 31}).
 * Con 45 minutos se acepta desde 30:30 (67,8 %); el 70 % exacto es 31:30.
 * <p>
 * Estas pruebas FIJAN EL COMPORTAMIENTO ACTUAL para documentarlo: cuando se
 * apruebe la correccion (comparar segundos con el 70 % exacto), deben invertirse
 * y la referencia de 72 perfiles se revisara aparte (EVALUACION_GENERACION_4_6_DIAS.md).
 */
@DisplayName("V20 · el piso actual admite menos del 70 % por redondeo (defecto documentado)")
class V20UmbralRedondeoTest {

    private static final LocalDate LUNES = LocalDate.of(2026, 11, 2);
    private static final SessionDurationEstimator ESTIMADOR = new SessionDurationEstimator();
    /** Calentamiento 5 min, transicion 60 s, 3 s por repeticion: los valores por omision (V14). */
    private static final PrescriptionParams PARAMETROS =
            new PrescriptionParams(null, null, null, null, null, null, null, null, null, null);

    @ParameterizedTest(name = "{0} min configurados: {1} s reales pasan V20 y son el {3} %")
    @CsvSource({
            // minutos, segundos reales, piso exacto en segundos (70 %), porcentaje real
            "30, 1230, 1260, 68.3",
            "45, 1830, 1890, 67.8",
            "45, 1889, 1890, 69.96",
            "60, 2490, 2520, 69.2",
    })
    void pisoActualAdmiteMenosDelSetentaPorCiento(int minutos, int segundos, int pisoExacto, double pct) {
        var sesion = sesionDe(segundos);
        int estimada = ESTIMADOR.estimateMinutes(sesion, PARAMETROS);
        int piso = PlanDraftValidator.minimumSessionMinutes(contexto(minutos));

        assertTrue(estimada >= piso,
                "hoy V20 la acepta: %d min estimados frente a un piso de %d".formatted(estimada, piso));
        assertTrue(segundos < pisoExacto,
                "y sin embargo dura %d s, menos que el 70 %% exacto (%d s)".formatted(segundos, pisoExacto));
    }

    @ParameterizedTest(name = "{0} min: el piso entero es {1} y el 70 % exacto {2} s")
    @CsvSource({"30, 21, 1260", "45, 31, 1890", "60, 42, 2520"})
    void pisoTruncado(int minutos, int pisoActual, int pisoExactoSegundos) {
        assertEquals(pisoActual, PlanDraftValidator.minimumSessionMinutes(contexto(minutos)));
        assertEquals(pisoExactoSegundos, minutos * 60 * 70 / 100);
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
