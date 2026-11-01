package main.web.services.fitsense.planning.infrastructure.validation;

import main.web.services.fitsense.configuration.domain.model.valueobjects.PrescriptionParams;
import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.domain.services.DoseEnforcement;
import main.web.services.fitsense.planning.domain.services.PlanDraftValidator;
import main.web.services.fitsense.planning.domain.services.SessionDurationEstimator;
import main.web.services.fitsense.planning.domain.services.SlotCoverageEnforcement;
import main.web.services.fitsense.planning.domain.services.experimental.TechoV4Versionado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AUDITORIA V4 (documenta el comportamiento VIGENTE; no lo cambia). V4 compara los minutos DECLARADOS con
 * {@code Math.round(M x 1,15)} y V17 admite un desvio del 20 % entre lo declarado y lo estimado. Un borrador puede
 * declarar 35 minutos para un contenido de 2.550 s (42,5 min, 142 % de 30) y pasar V4 y V17. La version candidata
 * {@link TechoV4Versionado#SEGUNDOS} lo rechazaria. El motor de reglas declara siempre lo estimado; la brecha solo
 * puede venir de un borrador de IA.
 */
class V4DuracionDeclaradaTest {

    static final LocalDate LUNES = LocalDate.of(2026, 10, 12);
    static final String[] ZONAS = {"chest", "back", "upper legs", "shoulders", "upper arms", "waist", "lower legs"};

    @Test
    @DisplayName("vigente: una sesion de 142 % de M que declara 35 min pasa V4 y V17; en segundos estrictos no pasaria")
    void declaradoFrenteAEstimado() {
        var p = new PrescriptionParams(0, 3, 60, 5, 60, 20, null, null, 10, 180);
        var ejercicios = new ArrayList<PlanDraft.DraftExercise>();
        var candidatos = new ArrayList<CandidateExercise>();
        for (long id = 1; id <= 7; id++) {
            ejercicios.add(new PlanDraft.DraftExercise(id, PrescriptionType.SETS_REPS, (short) 3, (short) 10, null, null,
                    (short) 90, null));
            candidatos.add(new CandidateExercise(id, "ejercicio " + id, ZONAS[(int) id - 1], "body weight", 1,
                    PrescriptionType.SETS_REPS, null, null));
        }
        var w = new PlanDraft.DraftWorkout(LUNES, WorkoutFocus.FULL_BODY, "s1", 35, List.copyOf(ejercicios));
        var est = new SessionDurationEstimator();
        long seg = est.estimateSeconds(w, p);
        assertEquals(2550, seg, "300 + 7 x 270 + 6 x 60");
        assertEquals(43, est.estimateMinutes(w, p));

        var ctx = new PlanGenerationContext(1L, (short) 1, LUNES, LUNES.plusDays(6), perfil(30), PlanAdjustment.none(),
                null, List.copyOf(candidatos), SafetyProfile.none(), p);
        var validador = new PlanDraftValidator(est, SlotCoverageEnforcement.OBSERVE, DoseEnforcement.OBSERVE);
        List<String> problemas;
        try {
            validador.validate(new PlanDraft(GenerationSource.AI, "modelo", "p", null, "r", List.of(w)), ctx, 30,
                    ConservationReport.vacio());
            problemas = List.of();
        } catch (InvalidPlanDraftException e) {
            problemas = e.problems();
        }
        assertTrue(problemas.stream().noneMatch(x -> x.startsWith("V4:")), "V4 vigente no lo detecta: " + problemas);
        assertTrue(problemas.stream().noneMatch(x -> x.startsWith("V17:")), "V17 lo tolera (18,6 % <= 20 %): " + problemas);

        assertTrue(TechoV4Versionado.HISTORICO.admite(TechoV4Versionado.minutosDeclarados(seg) - 8, 30),
                "declarar 35 en vez de 43 basta");
        assertFalse(TechoV4Versionado.SEGUNDOS.admite(seg, 30), "en segundos estrictos: 2.550 > 2.070");
        assertFalse(TechoV4Versionado.HISTORICO.admite(seg, 30), "con los minutos estimados tambien se rechazaria");
    }

    static PlanningProfile perfil(int minutos) {
        return new PlanningProfile(1L, 30, "FEMALE", new BigDecimal("168"), new BigDecimal("68"), null, "INTERMEDIATE",
                "GENERAL_FITNESS", "condicion", "Ninguna", "HOME", 3, List.of((short) 1, (short) 3, (short) 5), minutos,
                List.of("body weight"), List.of(), SafetyProfile.Declaradas.sinLimitaciones());
    }
}
