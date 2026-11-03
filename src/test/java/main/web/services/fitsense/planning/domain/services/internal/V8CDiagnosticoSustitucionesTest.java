package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.planning.domain.model.valueobjects.AdjustmentType;
import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.DerivedWorkout;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.model.valueobjects.PrescriptionType;
import main.web.services.fitsense.planning.domain.model.valueobjects.PreviousWeekSummary;
import main.web.services.fitsense.planning.domain.model.valueobjects.WeekDerivation;
import main.web.services.fitsense.planning.domain.model.valueobjects.WorkoutFocus;
import main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DIAGNOSTICO (evaluacion de calidad del prompt V2): QUE COMPRUEBA V8C EN UNA SUSTITUCION Y QUE NO.
 * <p>
 * No cambia V8C: DOCUMENTA su comportamiento vigente con los datos reales del catalogo (V44_2) de las sustituciones
 * observadas en las corridas C1/C2 y en la etapa C. Si una prueba de aqui empieza a fallar es porque V8C cambio; la
 * prueba debe actualizarse a sabiendas, no en silencio.
 * <p>
 * Hallazgos que fija: (1) el patron se comprueba POR CONJUNTO (cada anadido tiene un patron de entre los retirados), no
 * por pareja; (2) el musculo objetivo no se compara; (3) la distribucion de patrones de la sesion no se conserva; (4) el
 * numero de anadidos no se limita fuera del dolor; (5) la dificultad relativa no se compara (solo el maximo del perfil,
 * V6, en el validador estricto).
 */
@DisplayName("Diagnostico V8C: sustituciones (patron por conjunto, musculo, distribucion, numero, dificultad)")
class V8CDiagnosticoSustitucionesTest {

    static final LocalDate L1 = LocalDate.of(2026, 11, 2);
    static final LocalDate L2 = LocalDate.of(2026, 11, 9);
    final ConservationValidator v8c = new ConservationValidator();

    /** Datos reales del catalogo (evidencia/catalogo_exercises_v44_2.csv): id, zona, musculo, patron, dificultad. */
    static final Map<Long, CandidateExercise> CAT = new LinkedHashMap<>();

    static {
        c(580, "chest", "pectorals", "HORIZONTAL_PUSH", 1);   // dumbbell incline breeding («Curl inclinado…»)
        c(313, "upper arms", "triceps", "HORIZONTAL_PUSH", 1); // bench dip (knees bent)
        c(129, "upper arms", "triceps", "HORIZONTAL_PUSH", 1); // band close-grip push-up
        c(775, "back", "upper back", "HORIZONTAL_PUSH", 1);    // elbow lift - reverse push-up
        c(780, "shoulders", "delts", "HORIZONTAL_PULL", 2);    // dumbbell upright row
        c(119, "back", "upper back", "HORIZONTAL_PULL", 1);    // band one arm twisting seated row
        c(933, "chest", "pectorals", "HORIZONTAL_PUSH", 1);    // kneeling push-up
        c(318, "upper arms", "triceps", "HORIZONTAL_PUSH", 1); // body-up
        c(73, "chest", "pectorals", "HORIZONTAL_PUSH", 1);
        c(300, "back", "lats", "HORIZONTAL_PULL", 1);
        c(1039, "back", "upper back", "HORIZONTAL_PULL", 1);
        c(540, "chest", "pectorals", "HORIZONTAL_PUSH", 2);
        c(9001, "upper legs", "quads", "KNEE_DOMINANT", 1);    // sesion conservada
    }

    static void c(long id, String zona, String musculo, String patron, int dif) {
        CAT.put(id, new CandidateExercise(id, "e" + id, zona, "body weight", dif, PrescriptionType.SETS_REPS, musculo,
                patron));
    }

    static PlanDraft.DraftExercise ej(long id) {
        return new PlanDraft.DraftExercise(id, PrescriptionType.SETS_REPS, (short) 3, (short) 10, null, null, (short) 90,
                null);
    }

    /** Sesion 2 senalada como VARIANTE: antes {antes}, despues {despues}. La sesion 1 se conserva igual. */
    ConservationReportView validar(List<Long> antes, List<Long> despues) {
        var previas = new ArrayList<PreviousWeekSummary.PrescriptionOutcome>();
        previas.add(presc(1L, L1, 9001L));
        for (var id : antes) previas.add(presc(2L, L1.plusDays(2), id));
        var semana = new PreviousWeekSummary(new BigDecimal("40"), null, 0, Map.of(), List.of(
                new PreviousWeekSummary.WorkoutOutcome(1L, L1, "FULL_BODY", "COMPLETED", null, true, "COMPLETED",
                        (short) 6, new BigDecimal("100"), "A", (short) 45),
                new PreviousWeekSummary.WorkoutOutcome(2L, L1.plusDays(2), "FULL_BODY", "SKIPPED", "TOO_DIFFICULT",
                        true, "SKIPPED", null, BigDecimal.ZERO, "B", (short) 45)), previas, List.of());
        var der = new WeekDerivation(List.of(
                new DerivedWorkout(1L, new PlanDraft.DraftWorkout(L2, WorkoutFocus.FULL_BODY, "A", 45, List.of(ej(9001L)))),
                new DerivedWorkout(2L, new PlanDraft.DraftWorkout(L2.plusDays(2), WorkoutFocus.FULL_BODY, "B", 45,
                        despues.stream().map(V8CDiagnosticoSustitucionesTest::ej).toList()))), List.of());
        var orden = new PlanAdjustment(List.of(AdjustmentType.NONE), null, null, null, 0.0, 0.0, null, null, null,
                "baja adherencia", null, null,
                List.of(new PlanAdjustment.TargetedSession(2L, TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA)));
        var r = v8c.validate(der, semana, orden, CAT);
        return new ConservationReportView(r.aceptable(), r.problems(), r.aplicadas());
    }

    record ConservationReportView(boolean aceptable, List<String> problemas, List<Long> aplicadas) {}

    static PreviousWeekSummary.PrescriptionOutcome presc(long w, LocalDate f, long id) {
        return new PreviousWeekSummary.PrescriptionOutcome(w, f, id, "e" + id, "SETS_REPS", (short) 3, (short) 10, null,
                null, null, null, null, null, w == 2L ? BigDecimal.ZERO : new BigDecimal("100"), "SKIPPED",
                w == 2L ? "TOO_DIFFICULT" : null, "SKIPPED", (short) 90);
    }

    @ParameterizedTest(name = "{0}: {1} -> {2} ({3} -> {4}) se ACEPTA: V8C no compara el musculo")
    @DisplayName("1. Las sustituciones observadas con cambio de musculo, con sus datos reales, pasan V8C")
    @CsvSource({
            "C1 r1 U04 S2, 580, 313, pectorals, triceps",
            "C2 r2 U04 S2, 580, 129, pectorals, triceps",
            "C2 r1 U09 S2, 775, 313, upper back, triceps",
            "C2 r1/r2 U09 S2, 780, 119, delts, upper back",
            "C1 r2 U14 S2, 933, 318, pectorals, triceps",
            "C (PR28) U09 S2, 775, 73, upper back, pectorals",
            "C (PR28) U09 S2, 780, 300, delts, lats"})
    void observadas(String caso, long antes, long despues, String mAntes, String mDespues) {
        assertEquals(mAntes, CAT.get(antes).targetMuscle());
        assertEquals(mDespues, CAT.get(despues).targetMuscle());
        var r = validar(List.of(antes), List.of(despues));
        assertTrue(r.aceptable(), r.problemas()::toString);
        assertEquals(List.of(2L), r.aplicadas(), "cuenta como adaptacion aplicada");
    }

    @Test
    @DisplayName("2. Patron POR CONJUNTO: retirar PUSH+PULL y anadir dos PUSH pasa V8C (la sesion pierde su PULL)")
    void distribucionNoSeConserva() {
        var r = validar(List.of(540L, 1039L), List.of(73L, 313L));
        assertTrue(r.aceptable(), r.problemas()::toString);
    }

    @Test
    @DisplayName("3. Fuera del dolor, el numero de anadidos no se limita: 1 retirado por 2 del mismo patron pasa V8C")
    void numeroNoSeLimita() {
        var r = validar(List.of(540L), List.of(73L, 313L));
        assertTrue(r.aceptable(), r.problemas()::toString);
    }

    @Test
    @DisplayName("4. La dificultad relativa no se compara: 300 (dif 1) por 780 (dif 2) pasa V8C")
    void dificultadRelativaNoSeCompara() {
        var r = validar(List.of(300L), List.of(780L));
        assertTrue(r.aceptable(), r.problemas()::toString);
    }

    @Test
    @DisplayName("5. Control: otro patron SI se rechaza (la regla que V8C aplica)")
    void otroPatronSeRechaza() {
        var r = validar(List.of(780L), List.of(313L));
        assertFalse(r.aceptable());
        assertTrue(r.problemas().stream().anyMatch(p -> p.contains("mismo patron de movimiento")), r.problemas()::toString);
    }
}
