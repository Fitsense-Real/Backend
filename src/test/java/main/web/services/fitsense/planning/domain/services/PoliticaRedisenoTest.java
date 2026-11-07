package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.configuration.domain.model.valueobjects.PrescriptionParams;
import main.web.services.fitsense.planning.application.internal.commandservices.PlanningPolicySelector;
import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Rediseno tras la auditoria R/R' (orden del 05/11/2026): versiones de politica, V20 y V4 con M como maximo, V24 sin
 * relajar, V26 de volumen semanal y division sin PULL falsas. La politica historica no cambia.
 */
@DisplayName("Rediseno de politicas: catalogo, V20/V4/V24/V26 y division sin PULL falsas")
class PoliticaRedisenoTest {

    static final LocalDate LUNES = LocalDate.of(2026, 11, 2);
    static final PrescriptionParams P = new PrescriptionParams(70, 3, 60, 5, 60, 20, null, null, 10, 180);
    static final PlanDraftValidator VALIDADOR = new PlanDraftValidator(new SessionDurationEstimator(),
            SlotCoverageEnforcement.OBSERVE, DoseEnforcement.OBSERVE);

    static PlanGenerationContext ctx(String objetivo, String nivel, int minutos, PlanningPolicy politica,
                                     List<CandidateExercise> elegibles) {
        var perfil = new PlanningProfile(1L, 30, "FEMALE", new BigDecimal("168"), new BigDecimal("68"), null, nivel,
                objetivo, "x", "Ninguna", "HOME", 3, List.of((short) 1, (short) 3, (short) 5), minutos,
                List.of("body weight"), List.of(), SafetyProfile.Declaradas.sinLimitaciones());
        return new PlanGenerationContext(1L, (short) 1, LUNES, LUNES.plusDays(6), perfil, PlanAdjustment.none(), null,
                elegibles, SafetyProfile.none(), P, politica);
    }

    static List<String> problemas(PlanDraft d, PlanGenerationContext c, String prefijo) {
        try {
            VALIDADOR.validate(d, c, 30, ConservationReport.vacio());
            return List.of();
        } catch (InvalidPlanDraftException e) {
            return e.problems().stream().filter(p -> p.startsWith(prefijo)).toList();
        }
    }

    static PlanDraft sesion(List<PlanDraft.DraftExercise> ejercicios) {
        return new PlanDraft(GenerationSource.RULE_ENGINE, null, "p", null, "r", List.of(
                new PlanDraft.DraftWorkout(LUNES, WorkoutFocus.FULL_BODY, "s", 30, ejercicios)));
    }

    @Test
    @DisplayName("catalogo: la historica sigue por omision; las nuevas son candidatas y exigen permiso explicito")
    void catalogo() {
        assertSame(PlanningPolicy.HISTORICA, PlanningPolicy.deRegistro(null));
        assertTrue(PlanningPolicy.HISTORICA.pisoV20());
        assertEquals(15, PlanningPolicy.HISTORICA.toleranciaV4Pct());
        assertFalse(PlanningPolicy.HISTORICA.volumenSemanal() || PlanningPolicy.HISTORICA.fatigaGM()
                || PlanningPolicy.HISTORICA.pullReal() || PlanningPolicy.HISTORICA.sustitucionDesmotivacion());
        for (var p : List.of(PlanningPolicy.DUR_A, PlanningPolicy.DUR_B, PlanningPolicy.REV2)) {
            assertTrue(p.candidata(), p.id());
            assertFalse(p.pisoV20(), p.id());
            assertEquals(0, p.toleranciaV4Pct(), p.id());
            assertSame(p, PlanningPolicy.deRegistro(p.id()));
            assertThrows(IllegalStateException.class, () -> new PlanningPolicySelector(p.id(), false),
                    "sin allow-candidates no arranca");
            assertSame(p, new PlanningPolicySelector(p.id(), true).activa());
        }
        assertThrows(PlanningPolicy.PoliticaDesconocidaException.class, () -> PlanningPolicy.deRegistro("PLAN-X"));
    }

    @Test
    @DisplayName("V20: la historica rechaza por debajo del 70 %; las nuevas no; el objetivo de llenado: 70 % en historica y B, 0 en A")
    void v20() {
        var c = List.of(new CandidateExercise(1L, "plancha", "waist", "body weight", 1, PrescriptionType.DURATION,
                null, null), new CandidateExercise(2L, "plancha lateral", "waist", "body weight", 1,
                PrescriptionType.DURATION, null, null));
        var d = sesion(List.of(new PlanDraft.DraftExercise(1L, PrescriptionType.DURATION, (short) 2, null, 30, null,
                (short) 30, null), new PlanDraft.DraftExercise(2L, PrescriptionType.DURATION, (short) 2, null, 30, null,
                (short) 30, null)));
        assertFalse(problemas(d, ctx("GENERAL_FITNESS", "INTERMEDIATE", 60, PlanningPolicy.HISTORICA, c), "V20:").isEmpty());
        for (var p : List.of(PlanningPolicy.DUR_A, PlanningPolicy.DUR_B, PlanningPolicy.REV2))
            assertTrue(problemas(d, ctx("GENERAL_FITNESS", "INTERMEDIATE", 60, p, c), "V20:").isEmpty(), p.id());
        assertEquals(2520, PlanDraftValidator.objetivoLlenadoSegundos(ctx("GENERAL_FITNESS", "INTERMEDIATE", 60,
                PlanningPolicy.HISTORICA, c)));
        assertEquals(2520, PlanDraftValidator.objetivoLlenadoSegundos(ctx("GENERAL_FITNESS", "INTERMEDIATE", 60,
                PlanningPolicy.DUR_B, c)));
        assertEquals(0, PlanDraftValidator.objetivoLlenadoSegundos(ctx("GENERAL_FITNESS", "INTERMEDIATE", 60,
                PlanningPolicy.DUR_A, c)));
        assertEquals(0, PlanDraftValidator.objetivoLlenadoSegundos(ctx("GENERAL_FITNESS", "INTERMEDIATE", 60,
                PlanningPolicy.REV2, c)));
    }

    @Test
    @DisplayName("V4: con M como maximo no hay tolerancia; con V4-C historica, 15 %")
    void v4() {
        assertEquals(3600, PlanningPolicy.REV2.limiteV4SegundosDe(60));
        assertEquals(4140, PlanningPolicy.V4C.limiteV4SegundosDe(60));
        assertTrue(PlanningPolicy.REV2.excedeTecho(3601, 0, 60));
        assertFalse(PlanningPolicy.REV2.excedeTecho(3600, 0, 60));
        assertFalse(PlanningPolicy.V4C.excedeTecho(4140, 0, 60));
    }

    @Test
    @DisplayName("V26: 8 ejercicios de pecho de 3 series en una sesion pasan del techo por sesion con REV2; la historica no lo mira")
    void v26() {
        var elegibles = new ArrayList<CandidateExercise>();
        var ejercicios = new ArrayList<PlanDraft.DraftExercise>();
        for (long i = 1; i <= 8; i++) {
            elegibles.add(new CandidateExercise(i, "press " + i, "chest", "body weight", 1, PrescriptionType.SETS_REPS,
                    "pectorals", "HORIZONTAL_PUSH", false));
            ejercicios.add(new PlanDraft.DraftExercise(i, PrescriptionType.SETS_REPS, (short) 3, (short) 10, null,
                    null, (short) 90, null));
        }
        var d = sesion(ejercicios);
        assertTrue(problemas(d, ctx("INCREASE_STRENGTH", "INTERMEDIATE", 75, PlanningPolicy.HISTORICA, elegibles),
                "V26:").isEmpty());
        var v26 = problemas(d, ctx("INCREASE_STRENGTH", "INTERMEDIATE", 75, PlanningPolicy.REV2, elegibles), "V26:");
        assertFalse(v26.isEmpty(), "24 series de pecho en una sesion > 10");
        assertTrue(v26.stream().anyMatch(p -> p.contains("techo por sesion")), v26.toString());
        assertTrue(v26.stream().anyMatch(p -> p.contains("techo semanal")), "24 > 20 en la semana: " + v26);
    }

    /** Una semana previa publicada con {@code series[i]} series del ejercicio i+1 (todas en la misma sesion). */
    static PreviousWeekSummary previa(int... series) {
        var pr = new ArrayList<PreviousWeekSummary.PrescriptionOutcome>();
        for (int i = 0; i < series.length; i++)
            pr.add(new PreviousWeekSummary.PrescriptionOutcome(77L, LUNES.minusDays(7), i + 1L, "press", "SETS_REPS",
                    (short) series[i], (short) 10, null, null, null, null, null, null, null, null, null, "COMPLETED",
                    (short) 90));
        return new PreviousWeekSummary(new BigDecimal("100"), null, 0, java.util.Map.of(), List.of(), pr, List.of());
    }

    @Test
    @DisplayName("V26 en una semana derivada: no obliga a recortar el volumen heredado, pero sigue impidiendo subirlo")
    void v26SemanaDerivada() {
        var elegibles = new ArrayList<CandidateExercise>();
        for (long i = 1; i <= 9; i++)
            elegibles.add(new CandidateExercise(i, "press " + i, "chest", "body weight", 1, PrescriptionType.SETS_REPS,
                    "pectorals", "HORIZONTAL_PUSH", false));
        java.util.function.IntFunction<PlanDraft> conSeries = n -> {
            var ej = new ArrayList<PlanDraft.DraftExercise>();
            for (long i = 1; i <= 8; i++)
                ej.add(new PlanDraft.DraftExercise(i, PrescriptionType.SETS_REPS, (short) (i == 1 ? n : 3), (short) 10,
                        null, null, (short) 90, null));
            return sesion(ej);
        };
        java.util.function.Function<PreviousWeekSummary, PlanGenerationContext> ctx = pw -> {
            var c = ctx("INCREASE_STRENGTH", "INTERMEDIATE", 75, PlanningPolicy.REV2, elegibles);
            return new PlanGenerationContext(c.userId(), (short) 2, c.weekStartDate(), c.weekEndDate(), c.profile(),
                    c.adjustment(), pw, c.availableExercises(), c.safety(), c.prescription(), c.policy());
        };
        // Heredada de una semana con 24 series (p. ej. un plan generado con la historica): la copia exacta pasa.
        var heredada = previa(3, 3, 3, 3, 3, 3, 3, 3);
        assertTrue(problemas(conSeries.apply(3), ctx.apply(heredada), "V26:").isEmpty());
        // Subir una serie por encima de lo heredado (y del techo) se rechaza.
        var sube = problemas(conSeries.apply(4), ctx.apply(heredada), "V26:");
        assertTrue(sube.stream().anyMatch(p -> p.contains("techo por sesion")), sube.toString());
        assertTrue(sube.stream().anyMatch(p -> p.contains("techo semanal")), sube.toString());
        // Semana previa por debajo del techo: el techo rige tal cual (no se gana margen por heredar).
        var baja = problemas(conSeries.apply(3), ctx.apply(previa(3, 3, 3)), "V26:");
        assertFalse(baja.isEmpty(), "24 series > 10 por sesion y > 20 por semana aunque la previa tuviera 9");
        // Sin semana previa (generacion inicial): el techo de siempre.
        assertFalse(problemas(conSeries.apply(3), ctx.apply(null), "V26:").isEmpty());
    }

    @Test
    @DisplayName("P-G3: sin traccion elegible, 5 y 6 dias alternan superior/inferior; con traccion o con la historica, la division de siempre")
    void division() {
        List<Short> dias = List.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6);
        var siempre5 = WeeklySplitPlanner.plan(LUNES, dias, 5, 1).stream().map(WeeklySplitPlanner.PlannedSession::focus).toList();
        assertTrue(siempre5.contains(WorkoutFocus.PULL));
        var sin5 = WeeklySplitPlanner.plan(LUNES, dias, 5, 1, true).stream().map(WeeklySplitPlanner.PlannedSession::focus).toList();
        assertEquals(List.of(WorkoutFocus.UPPER_BODY, WorkoutFocus.LOWER_BODY, WorkoutFocus.UPPER_BODY,
                WorkoutFocus.LOWER_BODY, WorkoutFocus.UPPER_BODY), sin5);
        var sin6 = WeeklySplitPlanner.plan(LUNES, dias, 6, 1, true).stream().map(WeeklySplitPlanner.PlannedSession::focus).toList();
        assertFalse(sin6.contains(WorkoutFocus.PULL));
        assertEquals(WeeklySplitPlanner.plan(LUNES, dias, 4, 1), WeeklySplitPlanner.plan(LUNES, dias, 4, 1, true),
                "con 4 dias no hay PULL: nada cambia");
        var sinPull = List.of(new CandidateExercise(1L, "flexion", "chest", "body weight", 1, PrescriptionType.SETS_REPS,
                null, "HORIZONTAL_PUSH", false));
        var conPull = List.of(new CandidateExercise(2L, "remo con banda", "back", "band", 1, PrescriptionType.SETS_REPS,
                null, "HORIZONTAL_PULL", false));
        assertTrue(ctx("GAIN_MUSCLE", "INTERMEDIATE", 60, PlanningPolicy.REV2, sinPull).sinTraccionReal());
        assertFalse(ctx("GAIN_MUSCLE", "INTERMEDIATE", 60, PlanningPolicy.REV2, conPull).sinTraccionReal());
        assertFalse(ctx("GAIN_MUSCLE", "INTERMEDIATE", 60, PlanningPolicy.HISTORICA, sinPull).sinTraccionReal(),
                "la historica no cambia la division");
    }

    @Test
    @DisplayName("V27: con REV2 una sesion PULL sin traccion se rechaza si hay traccion elegible; la historica no lo mira")
    void v27() {
        var elegibles = List.of(
                new CandidateExercise(1L, "fondos de escapula", "back", "body weight", 1, PrescriptionType.SETS_REPS,
                        null, "HORIZONTAL_PUSH", false),
                new CandidateExercise(2L, "remo con banda", "back", "band", 1, PrescriptionType.SETS_REPS, null,
                        "HORIZONTAL_PULL", false));
        var pullSin = new PlanDraft(GenerationSource.RULE_ENGINE, null, "p", null, "r", List.of(new PlanDraft.DraftWorkout(
                LUNES, WorkoutFocus.PULL, "s", 30, List.of(new PlanDraft.DraftExercise(1L, PrescriptionType.SETS_REPS,
                (short) 3, (short) 10, null, null, (short) 90, null)))));
        var pullCon = new PlanDraft(GenerationSource.RULE_ENGINE, null, "p", null, "r", List.of(new PlanDraft.DraftWorkout(
                LUNES, WorkoutFocus.PULL, "s", 30, List.of(new PlanDraft.DraftExercise(2L, PrescriptionType.SETS_REPS,
                (short) 3, (short) 10, null, null, (short) 90, null)))));
        assertFalse(problemas(pullSin, ctx("LOSE_WEIGHT", "INTERMEDIATE", 45, PlanningPolicy.REV2, elegibles), "V27:").isEmpty());
        assertTrue(problemas(pullCon, ctx("LOSE_WEIGHT", "INTERMEDIATE", 45, PlanningPolicy.REV2, elegibles), "V27:").isEmpty());
        assertTrue(problemas(pullSin, ctx("LOSE_WEIGHT", "INTERMEDIATE", 45, PlanningPolicy.HISTORICA, elegibles), "V27:").isEmpty());
    }

    @Test
    @DisplayName("dosis semanal: techos y objetivos explicitos, el objetivo nunca supera el techo")
    void dosis() {
        for (var nivel : List.of("BEGINNER", "INTERMEDIATE", "ADVANCED"))
            for (var obj : List.of("GAIN_MUSCLE", "INCREASE_STRENGTH", "LOSE_WEIGHT", "GENERAL_FITNESS"))
                for (var g : List.of("chest", "back", "upper legs", "waist"))
                    assertTrue(DosisSemanalPolicy.objetivoSeriesSemana(obj, nivel, g)
                            <= DosisSemanalPolicy.techoSeriesSemana(nivel), obj + " " + nivel + " " + g);
        assertEquals(12, DosisSemanalPolicy.techoSeriesSemana("BEGINNER"));
        assertEquals(20, DosisSemanalPolicy.techoSeriesSemana("INTERMEDIATE"));
        assertEquals(24, DosisSemanalPolicy.techoSeriesSemana("ADVANCED"));
        assertEquals(0, DosisSemanalPolicy.objetivoAerobicoMinutosSemana("GAIN_MUSCLE", "INTERMEDIATE"));
    }
}
