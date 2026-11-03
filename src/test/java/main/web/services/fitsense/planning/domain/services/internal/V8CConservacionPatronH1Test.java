package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.planning.domain.model.valueobjects.AdjustmentType;
import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.ConservationReport;
import main.web.services.fitsense.planning.domain.model.valueobjects.DerivedWorkout;
import main.web.services.fitsense.planning.domain.model.valueobjects.ExclusionesDeSeguridad;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.model.valueobjects.PrescriptionType;
import main.web.services.fitsense.planning.domain.model.valueobjects.PreviousWeekSummary;
import main.web.services.fitsense.planning.domain.model.valueobjects.WeekDerivation;
import main.web.services.fitsense.planning.domain.model.valueobjects.WorkoutFocus;
import main.web.services.fitsense.planning.domain.services.ConservationValidator.ReglaPatron;
import main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * H1 · CONSERVACION DEL PATRON EN LAS SUSTITUCIONES DE V8C: regla historica (CONJUNTO) frente a la correccion
 * candidata (MULTICONJUNTO, apagada por defecto). Datos reales del catalogo V44_2.
 * <p>
 * El caso que motiva H1 es U04 S2 del laboratorio GUION_COMBINADOS (#12b, PR #30): en una sesion senalada como
 * VARIANTE se sustituye 380 (HORIZONTAL_PUSH) por 56 (HORIZONTAL_PUSH) y 389 (VERTICAL_PULL) por 72
 * (HORIZONTAL_PUSH). Con CONJUNTO se publica y la sesion pasa de 2 tirones verticales a 1.
 */
@DisplayName("H1 · V8C: conservacion del patron (conjunto historico frente a multiconjunto candidato)")
class V8CConservacionPatronH1Test {

    static final LocalDate L1 = LocalDate.of(2026, 11, 2);
    static final LocalDate L2 = L1.plusWeeks(1);
    static final long SENALADA = 2L, CONSERVADA = 1L;

    /** id, zona, equipamiento, musculo, patron, dificultad (evidencia/catalogo_exercises_v44_2.csv). */
    static final Map<Long, CandidateExercise> CAT = new LinkedHashMap<>();

    static {
        c(380, "chest", "cable", "pectorals", "HORIZONTAL_PUSH", 2);         // cable one arm incline press
        c(389, "back", "cable", "lats", "VERTICAL_PULL", 2);                 // cable pulldown
        c(360, "upper legs", "cable", "glutes", "HIP_HINGE", 2);             // cable deadlift
        c(779, "shoulders", "dumbbell", "delts", "VERTICAL_PUSH", 2);        // dumbbell standing palms in press
        c(1236, "upper legs", "smith machine", "glutes", "KNEE_DOMINANT", 2);// smith hack squat
        c(580, "chest", "dumbbell", "pectorals", "HORIZONTAL_PUSH", 1);      // dumbbell incline breeding
        c(392, "back", "cable", "lats", "VERTICAL_PULL", 2);                 // cable rear pulldown
        c(56, "chest", "body weight", "pectorals", "HORIZONTAL_PUSH", 2);    // archer push up
        c(72, "chest", "leverage machine", "pectorals", "HORIZONTAL_PUSH", 1);// assisted chest dip (kneeling)
        c(313, "upper arms", "body weight", "triceps", "HORIZONTAL_PUSH", 1);
        c(129, "upper arms", "band", "triceps", "HORIZONTAL_PUSH", 1);
        c(775, "back", "body weight", "upper back", "HORIZONTAL_PUSH", 1);
        c(780, "shoulders", "dumbbell", "delts", "HORIZONTAL_PULL", 2);
        c(119, "back", "band", "upper back", "HORIZONTAL_PULL", 1);
        c(933, "chest", "body weight", "pectorals", "HORIZONTAL_PUSH", 1);
        c(318, "upper arms", "body weight", "triceps", "HORIZONTAL_PUSH", 1);
        c(73, "chest", "body weight", "pectorals", "HORIZONTAL_PUSH", 1);
        c(300, "back", "band", "lats", "HORIZONTAL_PULL", 1);
        c(862, "waist", "body weight", "abs", "VERTICAL_PULL", 1);           // gorilla chin (H13)
        c(74, "back", "leverage machine", "lats", "VERTICAL_PULL", 1);
        c(9001, "upper legs", "body weight", "quads", "KNEE_DOMINANT", 1);   // sesion conservada
    }

    static void c(long id, String zona, String eq, String musculo, String patron, int dif) {
        CAT.put(id, new CandidateExercise(id, "e" + id, zona, eq, dif, PrescriptionType.SETS_REPS, musculo, patron));
    }

    static final List<Long> U04_S2_ANTES = List.of(380L, 389L, 360L, 779L, 1236L, 580L, 392L);
    static final List<Long> U04_S2_DESPUES = List.of(56L, 72L, 360L, 779L, 1236L, 580L, 392L);

    // ------------------------------------------------------------------------------------------------------------

    @Test
    @DisplayName("0. Por defecto (sin configurar) la regla es la historica: CONJUNTO")
    void porDefecto() {
        assertEquals(ReglaPatron.CONJUNTO, new ConservationValidator().reglaPatron());
        assertEquals(ReglaPatron.CONJUNTO, new ConservationValidator("conjunto").reglaPatron());
        assertEquals(ReglaPatron.CONJUNTO, new ConservationValidator("").reglaPatron());
        assertEquals(ReglaPatron.MULTICONJUNTO, new ConservationValidator(" Multiconjunto ").reglaPatron());
        assertThrows(IllegalArgumentException.class, () -> new ConservationValidator("pareja"));
    }

    @Test
    @DisplayName("1a. ANTES (CONJUNTO): U04 S2 pierde un tiron vertical y se ACEPTA (comportamiento historico)")
    void u04AntesSeAcepta() {
        var r = variante(ReglaPatron.CONJUNTO, U04_S2_ANTES, U04_S2_DESPUES);
        assertTrue(r.aceptable(), r.problems()::toString);
        assertEquals(List.of(SENALADA), r.aplicadas());
    }

    @Test
    @DisplayName("1b. DESPUES (MULTICONJUNTO): U04 S2 se RECHAZA por anadir 2 HORIZONTAL_PUSH retirando 1")
    void u04DespuesSeRechaza() {
        var r = variante(ReglaPatron.MULTICONJUNTO, U04_S2_ANTES, U04_S2_DESPUES);
        assertFalse(r.aceptable());
        assertTrue(r.aplicadas().isEmpty());
        assertEquals(1, r.problems().size(), r.problems()::toString);
        assertTrue(r.problems().get(0).contains("anade 2 ejercicio(s) de patron HORIZONTAL_PUSH [56, 72] y solo "
                + "retira 1 de ese patron [380]"), r.problems()::toString);
    }

    @Test
    @DisplayName("1c. MULTICONJUNTO: la correccion legitima de U04 S2 (389 por un VERTICAL_PULL) se acepta")
    void u04CorregidoSeAcepta() {
        var r = variante(ReglaPatron.MULTICONJUNTO, U04_S2_ANTES, List.of(56L, 74L, 360L, 779L, 1236L, 580L, 392L));
        assertTrue(r.aceptable(), r.problems()::toString);
    }

    @ParameterizedTest(name = "{0}: {1} -> {2} se acepta con las dos reglas")
    @DisplayName("2. Sustituciones reales del mismo patron (incluidas las de cambio de musculo) siguen aceptadas")
    @CsvSource({"C1 r1 U04, 580, 313", "C2 r2 U04, 580, 129", "C2 r1 U09, 775, 313", "C2 U09, 780, 119",
            "C1 r2 U14, 933, 318", "PR28 U09, 775, 73", "PR28 U09, 780, 300", "RC R05/R08, 862, 74"})
    void mismasPatron(String caso, long antes, long despues) {
        for (var regla : ReglaPatron.values()) {
            var r = variante(regla, List.of(antes, 9001L), List.of(despues, 9001L));
            assertTrue(r.aceptable(), () -> regla + " " + r.problems());
            assertEquals(List.of(SENALADA), r.aplicadas());
        }
    }

    @Test
    @DisplayName("3. Dos sustituciones cruzadas del mismo par de patrones (PUSH->PUSH y PULL->PULL) se aceptan")
    void dosParejas() {
        for (var regla : ReglaPatron.values())
            assertTrue(variante(regla, List.of(380L, 389L), List.of(56L, 74L)).aceptable(), regla::name);
    }

    @Test
    @DisplayName("4. Anadir 2 de un patron retirando 1 de ese patron: CONJUNTO acepta, MULTICONJUNTO rechaza")
    void unoPorDos() {
        assertTrue(variante(ReglaPatron.CONJUNTO, List.of(580L, 9001L), List.of(73L, 313L, 9001L)).aceptable());
        var r = variante(ReglaPatron.MULTICONJUNTO, List.of(580L, 9001L), List.of(73L, 313L, 9001L));
        assertFalse(r.aceptable());
        assertTrue(r.problems().get(0).contains("anade 2 ejercicio(s) de patron HORIZONTAL_PUSH"), r.problems()::toString);
    }

    @ParameterizedTest
    @EnumSource(ReglaPatron.class)
    @DisplayName("5. Otro patron se rechaza con las dos reglas, con el mensaje historico y sin duplicarlo")
    void otroPatron(ReglaPatron regla) {
        var r = variante(regla, List.of(780L, 9001L), List.of(313L, 9001L));
        assertFalse(r.aceptable());
        assertEquals(1, r.problems().size(), r.problems()::toString);
        assertTrue(r.problems().get(0).contains("mismo patron de movimiento"));
    }

    @ParameterizedTest
    @EnumSource(ReglaPatron.class)
    @DisplayName("6. VARIANTE solo con cambio de dosis (sin sustituir) se acepta igual con las dos reglas")
    void soloDosis(ReglaPatron regla) {
        var nuevos = List.of(ej(580L, 3, 8), ej(9001L, 3, 10));
        var r = validar(regla, TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA, "TOO_DIFFICULT", List.of(580L, 9001L),
                nuevos);
        assertTrue(r.aceptable(), r.problems()::toString);
        assertEquals(List.of(SENALADA), r.aplicadas());
    }

    @ParameterizedTest
    @EnumSource(ReglaPatron.class)
    @DisplayName("7. VARIANTE que retira sin anadir sigue aceptada (la regla no prohibe retiradas)")
    void retirarSinAnadir(ReglaPatron regla) {
        var r = variante(regla, List.of(580L, 389L, 9001L), List.of(580L, 9001L));
        assertTrue(r.aceptable(), r.problems()::toString);
    }

    @ParameterizedTest
    @EnumSource(ReglaPatron.class)
    @DisplayName("8. VARIANTE que retira 2 de un patron y anade 1 de ese patron se acepta")
    void dosPorUno(ReglaPatron regla) {
        var r = variante(regla, List.of(380L, 580L, 9001L), List.of(56L, 9001L));
        assertTrue(r.aceptable(), r.problems()::toString);
    }

    @ParameterizedTest
    @EnumSource(ReglaPatron.class)
    @DisplayName("9. FATIGA e INVESTIGAR con reduccion de dosis: sin cambios con las dos reglas")
    void fatigaEInvestigar(ReglaPatron regla) {
        for (var t : List.of(TipoIntervencion.REVISAR_CARGA_Y_RECUPERACION, TipoIntervencion.INVESTIGAR_CAMBIO_MINIMO)) {
            var r = validar(regla, t, t == TipoIntervencion.REVISAR_CARGA_Y_RECUPERACION ? "FATIGUE"
                    : "LACK_OF_MOTIVATION", List.of(580L, 9001L), List.of(ej(580L, 2, 10), ej(9001L, 3, 10)));
            assertTrue(r.aceptable(), () -> t + " " + r.problems());
            assertEquals(List.of(SENALADA), r.aplicadas());
        }
    }

    @ParameterizedTest
    @EnumSource(ReglaPatron.class)
    @DisplayName("10. FATIGA no autoriza sustituir: se rechaza igual con las dos reglas (la regla nueva no lo toca)")
    void fatigaNoSustituye(ReglaPatron regla) {
        var r = validar(regla, TipoIntervencion.REVISAR_CARGA_Y_RECUPERACION, "FATIGUE", List.of(580L, 9001L),
                List.of(ej(73L, 3, 10), ej(9001L, 3, 10)));
        assertFalse(r.aceptable());
        assertTrue(r.problems().get(0).contains("no autoriza sustituir"), r.problems()::toString);
    }

    @ParameterizedTest
    @EnumSource(ReglaPatron.class)
    @DisplayName("11. DOLOR: sustituir por otro patron sigue permitido (la seguridad no exige el mismo patron)")
    void dolorOtroPatron(ReglaPatron regla) {
        // Sesion con dolor (380 HORIZONTAL_PUSH y 389 VERTICAL_PULL): se sustituyen por un HIP_HINGE y un KNEE_DOMINANT.
        var r = validar(regla, TipoIntervencion.SEGURIDAD_ANTE_DOLOR, "PAIN_OR_DISCOMFORT", List.of(380L, 389L),
                List.of(ej(360L, 3, 10), ej(1236L, 3, 10)));
        assertTrue(r.aceptable(), r.problems()::toString);
        assertEquals(List.of(SENALADA), r.aplicadas());
    }

    @ParameterizedTest
    @EnumSource(ReglaPatron.class)
    @DisplayName("12. DOLOR: anadir mas de lo retirado sigue rechazado con su regla propia")
    void dolorNoAnade(ReglaPatron regla) {
        var r = validar(regla, TipoIntervencion.SEGURIDAD_ANTE_DOLOR, "PAIN_OR_DISCOMFORT", List.of(380L),
                List.of(ej(360L, 3, 10), ej(1236L, 3, 10)));
        assertFalse(r.aceptable());
        assertTrue(r.problems().stream().anyMatch(p -> p.contains("no se anade trabajo")), r.problems()::toString);
    }

    @ParameterizedTest
    @EnumSource(ReglaPatron.class)
    @DisplayName("13. La sesion NO senalada no puede cambiar, con las dos reglas")
    void noSenalada(ReglaPatron regla) {
        var v8c = new ConservationValidator(regla);
        var r = v8c.validate(derivacion(List.of(ej(580L, 3, 10)), List.of(ej(1236L, 3, 10))),
                semana("TOO_DIFFICULT", List.of(580L)), orden(TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA), CAT);
        assertFalse(r.aceptable());
        assertTrue(r.problems().stream().anyMatch(p -> p.contains("no estaba senalada")), r.problems()::toString);
    }

    // ------------------------------------------------------------------------------------------------------------

    ConservationReport variante(ReglaPatron regla, List<Long> antes, List<Long> despues) {
        return validar(regla, TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA, "TOO_DIFFICULT", antes,
                despues.stream().map(id -> ej(id, 3, 10)).toList());
    }

    ConservationReport validar(ReglaPatron regla, TipoIntervencion t, String motivo, List<Long> antes,
                               List<PlanDraft.DraftExercise> despues) {
        return new ConservationValidator(regla).validate(derivacion(despues, List.of(ej(9001L, 3, 10))),
                semana(motivo, antes), orden(t), CAT, ExclusionesDeSeguridad.ninguna(), List.of(), false, 30);
    }

    static WeekDerivation derivacion(List<PlanDraft.DraftExercise> senalada, List<PlanDraft.DraftExercise> conservada) {
        return new WeekDerivation(List.of(
                new DerivedWorkout(CONSERVADA, new PlanDraft.DraftWorkout(L2, WorkoutFocus.FULL_BODY, "A", 45, conservada)),
                new DerivedWorkout(SENALADA, new PlanDraft.DraftWorkout(L2.plusDays(2), WorkoutFocus.FULL_BODY, "B", 45,
                        senalada))), List.of());
    }

    static PreviousWeekSummary semana(String motivo, List<Long> antes) {
        var previas = new ArrayList<PreviousWeekSummary.PrescriptionOutcome>();
        previas.add(presc(CONSERVADA, L1, 9001L, null));
        for (var id : antes) previas.add(presc(SENALADA, L1.plusDays(2), id, motivo));
        return new PreviousWeekSummary(new BigDecimal("50"), null, 0, Map.of(), List.of(
                new PreviousWeekSummary.WorkoutOutcome(CONSERVADA, L1, "FULL_BODY", "COMPLETED", null, true, "COMPLETED",
                        (short) 6, new BigDecimal("100"), "A", (short) 45),
                new PreviousWeekSummary.WorkoutOutcome(SENALADA, L1.plusDays(2), "FULL_BODY", "SKIPPED", motivo, true,
                        "SKIPPED", null, BigDecimal.ZERO, "B", (short) 45)), previas, List.of());
    }

    static PreviousWeekSummary.PrescriptionOutcome presc(long w, LocalDate f, long id, String motivo) {
        boolean senalada = w == SENALADA;
        return new PreviousWeekSummary.PrescriptionOutcome(w, f, id, "e" + id, "SETS_REPS", (short) 3, (short) 10, null,
                null, null, null, null, null, senalada ? BigDecimal.ZERO : new BigDecimal("100"),
                senalada ? "SKIPPED" : "COMPLETED", senalada ? motivo : null, senalada ? "SKIPPED" : "COMPLETED",
                (short) 90);
    }

    static PlanAdjustment orden(TipoIntervencion t) {
        return new PlanAdjustment(List.of(AdjustmentType.NONE), null, null, null, 0.0, 0.0, null, null, null,
                "causa", null, null, List.of(new PlanAdjustment.TargetedSession(SENALADA, t)));
    }

    static PlanDraft.DraftExercise ej(long id, int series, int reps) {
        return new PlanDraft.DraftExercise(id, PrescriptionType.SETS_REPS, (short) series, (short) reps, null, null,
                (short) 90, null);
    }
}
