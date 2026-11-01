package main.web.services.fitsense.planning.infrastructure.validation;

import main.web.services.fitsense.configuration.domain.model.valueobjects.PrescriptionParams;
import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.domain.services.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * La traza de la politica registra el limite que V4 APLICA (minutos del perfil, la regla del validador) y, aparte,
 * los minutos efectivos de la semana. Cada caso se contrasta con el validador real: la traza dice "dentro" exactamente
 * cuando el validador no devuelve ningun problema V4.
 */
@DisplayName("Traza V4-C: limite aplicado frente a minutos efectivos, contrastado con el validador real")
class TrazaV4CTest {

    static final LocalDate LUNES = LocalDate.of(2026, 10, 12);
    static final PrescriptionParams P = new PrescriptionParams(0, 3, 60, 5, 60, 20, null, null, 10, 180);
    static final PlanDraftValidator VALIDADOR = new PlanDraftValidator(new SessionDurationEstimator(),
            SlotCoverageEnforcement.OBSERVE, DoseEnforcement.OBSERVE);
    static final SessionDurationEstimator EST = new SessionDurationEstimator();

    static PlanAdjustment acortar(int minutos) {
        return new PlanAdjustment(List.of(AdjustmentType.REDUCE_DURATION), null, null, null, 0.0, 0.0, null, null,
                minutos, "ACORTAR", null, null);
    }

    static PlanGenerationContext ctx(int minutos, PlanningPolicy politica, PlanAdjustment ajuste, PrescriptionParams p) {
        var perfil = new PlanningProfile(1L, 30, "FEMALE", new BigDecimal("168"), new BigDecimal("68"), null,
                "INTERMEDIATE", "GENERAL_FITNESS", "x", "Ninguna", "HOME", 3, List.of((short) 1, (short) 3, (short) 5),
                minutos, List.of("body weight"), List.of(), SafetyProfile.Declaradas.sinLimitaciones());
        var c = List.of(new CandidateExercise(1L, "plancha", "waist", "body weight", 1, PrescriptionType.DURATION, null,
                        null),
                new CandidateExercise(2L, "plancha lateral", "waist", "body weight", 1, PrescriptionType.DURATION, null,
                        null));
        return new PlanGenerationContext(1L, (short) 1, LUNES, LUNES.plusDays(6), perfil, ajuste, null, c,
                SafetyProfile.none(), p, politica);
    }

    /** Segundos estimados exactos: 300 de calentamiento + trabajo + 30 + 60 de transicion. */
    static PlanDraft.DraftWorkout sesion(long segundos, int declarados) {
        var e = new PlanDraft.DraftExercise(1L, PrescriptionType.DURATION, (short) 1, null, (int) segundos - 390, null,
                (short) 60, null);
        var e2 = new PlanDraft.DraftExercise(2L, PrescriptionType.DURATION, (short) 1, null, 30, null, (short) 60, null);
        return new PlanDraft.DraftWorkout(LUNES, WorkoutFocus.FULL_BODY, "s", declarados, List.of(e, e2));
    }

    static List<String> v4(PlanDraft.DraftWorkout w, PlanGenerationContext c) {
        var d = new PlanDraft(GenerationSource.AI, "m", "p", null, "r", List.of(w));
        try {
            VALIDADOR.validate(d, c, 30, ConservationReport.vacio());
            return List.of();
        } catch (InvalidPlanDraftException ex) {
            return ex.problems().stream().filter(p -> p.startsWith("V4:")).toList();
        }
    }

    static Map<String, Object> traza(PlanGenerationContext c, PlanDraft.DraftWorkout w) {
        return PlanningPolicyTrace.de(c, List.of(w), GenerationSource.AI, "ADAPTACION", "MVP-1.4", Set.of(), EST);
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> aplicado(Map<String, Object> t) {
        return (Map<String, Object>) ((Map<String, Object>) t.get("parametros")).get("limite_v4_aplicado");
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> sesion0(Map<String, Object> t) {
        return ((List<Map<String, Object>>) t.get("sesiones")).get(0);
    }

    @ParameterizedTest(name = "{0} min: limite aplicado {1} s; {1}-1, {1} y {1}+1 contra el validador")
    @CsvSource({"30,2070", "45,3105", "60,4140", "75,5175"})
    void limiteAplicadoPorPerfil(int m, long limite) {
        var c = ctx(m, PlanningPolicy.V4C, PlanAdjustment.none(), P);
        for (long s : new long[]{limite - 1, limite, limite + 1}) {
            var w = sesion(s, (int) Math.round(s / 60.0));
            assertEquals(s, EST.estimateSeconds(w, P));
            var t = traza(c, w);
            var a = aplicado(t);
            assertEquals("V4C_SEGUNDOS", a.get("regla"));
            assertEquals(m, a.get("minutos_base"));
            assertEquals(limite, ((Number) a.get("segundos")).longValue());
            assertEquals(m, t.get("minutos_perfil"));
            assertEquals(m, t.get("minutos_efectivos"));
            boolean dentro = (Boolean) sesion0(t).get("dentro_limite_v4_aplicado");
            assertEquals(s <= limite, dentro, s + " s");
            assertEquals(dentro, v4(w, c).isEmpty(), "la traza dice lo mismo que el validador: " + v4(w, c));
        }
    }

    @Test
    @DisplayName("60 min con ACORTAR a 30: 60 configurados, 30 efectivos, 4.140 s aplicados; 2.070 s solo informativo")
    void acortarDistingueLosMinutos() {
        var c = ctx(60, PlanningPolicy.V4C, acortar(30), P);
        assertEquals(30, c.effectiveSessionMinutes());
        for (long s : new long[]{3000, 4140, 4141}) {
            var w = sesion(s, (int) Math.round(s / 60.0));
            var t = traza(c, w);
            assertEquals(60, t.get("minutos_perfil"));
            assertEquals(30, t.get("minutos_efectivos"));
            assertEquals(30, t.get("minutos_disponibles"), "campo historico: sigue siendo el efectivo");
            var a = aplicado(t);
            assertEquals(4140L, ((Number) a.get("segundos")).longValue(), "el que usa el validador");
            assertEquals(60, a.get("minutos_base"));
            @SuppressWarnings("unchecked") var params = (Map<String, Object>) t.get("parametros");
            assertEquals(Map.of("segundos", 2070L), params.get("limite_v4"), "campo historico: informativo, efectivo");
            assertEquals(30, params.get("limite_v4_minutos_base"));
            boolean dentro = (Boolean) sesion0(t).get("dentro_limite_v4_aplicado");
            assertEquals(s <= 4140, dentro, s + " s");
            assertEquals(dentro, v4(w, c).isEmpty(), "validador: " + v4(w, c));
        }
    }

    @ParameterizedTest(name = "historica {0} min, declara {1}: dentro={2}")
    @CsvSource({"30,35,true", "30,36,false", "45,52,true", "45,53,false", "60,69,true", "60,70,false",
            "75,86,true", "75,87,false"})
    void historicaSinCambios(int m, int declarados, boolean dentro) {
        var c = ctx(m, PlanningPolicy.HISTORICA, PlanAdjustment.none(), P);
        var w = sesion(declarados * 60L, declarados);
        var t = traza(c, w);
        var a = aplicado(t);
        assertEquals("V4_MINUTOS", a.get("regla"));
        assertEquals((int) Math.round(m * 1.15), a.get("minutos_declarados"));
        assertNull(a.get("segundos"));
        @SuppressWarnings("unchecked") var params = (Map<String, Object>) t.get("parametros");
        assertEquals("HISTORICO", params.get("v4"));
        assertEquals(Map.of("minutos_declarados", (int) Math.round(m * 1.15)), params.get("limite_v4"));
        assertEquals(dentro, sesion0(t).get("dentro_limite_v4_aplicado"));
        assertEquals(dentro, v4(w, c).isEmpty(), "aceptacion historica: " + v4(w, c));
    }

    @Test
    @DisplayName("historica con ACORTAR: el limite aplicado sigue siendo round(perfil x 1,15), como el validador")
    void historicaConAcortar() {
        var c = ctx(60, PlanningPolicy.HISTORICA, acortar(30), P);
        var w = sesion(60L * 60, 60);
        var t = traza(c, w);
        assertEquals(69, aplicado(t).get("minutos_declarados"));
        assertEquals(true, sesion0(t).get("dentro_limite_v4_aplicado"));
        assertTrue(v4(w, c).isEmpty());
    }

    @Test
    @DisplayName("sin bloque prescription: no se inventan segundos y V4-C cae en la regla de minutos, igual que el validador")
    void sinPrescripcion() {
        var c = ctx(30, PlanningPolicy.V4C, PlanAdjustment.none(), null);
        var w = sesion(5000, 35);
        var t = traza(c, w);
        var s = sesion0(t);
        assertNull(s.get("segundos_estimados"), "sin estimacion no hay segundos (antes 0)");
        assertNull(s.get("pct_de_m"));
        assertEquals(false, s.get("bajo_piso_v20"));
        var a = aplicado(t);
        assertEquals("V4_MINUTOS", a.get("regla"));
        assertEquals(35, a.get("minutos_declarados"));
        assertFalse(a.containsKey("segundos"));
        assertEquals(true, s.get("dentro_limite_v4_aplicado"));
        assertTrue(v4(w, c).isEmpty(), "el validador tampoco aplica V4-C sin prescription: " + v4(w, c));
        var w36 = sesion(5000, 36);
        assertEquals(false, sesion0(traza(c, w36)).get("dentro_limite_v4_aplicado"));
        assertFalse(v4(w36, c).isEmpty());
    }
}
