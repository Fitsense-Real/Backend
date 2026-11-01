package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.configuration.domain.model.valueobjects.PrescriptionParams;
import main.web.services.fitsense.planning.application.internal.commandservices.PlanningPolicySelector;
import main.web.services.fitsense.planning.domain.model.aggregates.WeeklyTrainingPlan;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Politica de planificacion versionada: catalogo, seleccion, V4-C, traza y saturacion de C2-D")
class PoliticaPlanificacionTest {

    static final LocalDate LUNES = LocalDate.of(2026, 11, 2);
    static final PrescriptionParams P = new PrescriptionParams(70, 3, 60, 5, 60, 20, null, null, 10, 180);

    // ---------------------------------------------------------------------------------------------- catalogo

    @Test
    @DisplayName("catalogo cerrado: null es la historica, desconocida es error, cada componente por separado")
    void catalogo() {
        assertSame(PlanningPolicy.HISTORICA, PlanningPolicy.deRegistro(null));
        assertSame(PlanningPolicy.HISTORICA, PlanningPolicy.deRegistro("  "));
        assertSame(PlanningPolicy.C2D, PlanningPolicy.deRegistro("PLAN-C2D-0.1"));
        assertThrows(PlanningPolicy.PoliticaDesconocidaException.class, () -> PlanningPolicy.deRegistro("PLAN-C2-0.1"),
                "PLAN-C2-0.1 designaba C2-C: no se reutiliza");
        assertThrows(PlanningPolicy.PoliticaDesconocidaException.class, () -> PlanningPolicy.deRegistro("PLAN-E-0.1"),
                "el modelo E no tiene version");
        assertTrue(PlanningPolicy.C2D.c2d() && !PlanningPolicy.C2D.v4Segundos(), "C2-D no activa V4-C");
        assertTrue(!PlanningPolicy.V4C.c2d() && PlanningPolicy.V4C.v4Segundos(), "V4-C no activa C2-D");
        assertTrue(PlanningPolicy.C2D_V4C.c2d() && PlanningPolicy.C2D_V4C.v4Segundos());
        assertFalse(PlanningPolicy.HISTORICA.c2d() || PlanningPolicy.HISTORICA.v4Segundos());
        assertFalse(PlanningPolicy.HISTORICA.candidata());
        assertEquals(4, PlanningPolicy.CATALOGO.size());
    }

    @Test
    @DisplayName("seleccion: historica por defecto; una candidata exige permiso explicito; desconocida no arranca")
    void seleccion() {
        assertSame(PlanningPolicy.HISTORICA, PlanningPolicySelector.historica().activa());
        assertThrows(IllegalStateException.class, () -> new PlanningPolicySelector("PLAN-C2D-0.1", false));
        assertThrows(IllegalStateException.class, () -> new PlanningPolicySelector("PLAN-V4-0.1", false));
        assertThrows(IllegalStateException.class, () -> new PlanningPolicySelector("PLAN-X", true));
        assertSame(PlanningPolicy.C2D, new PlanningPolicySelector("PLAN-C2D-0.1", true).activa());
    }

    @Test
    @DisplayName("contexto: el constructor de siempre es historico; conPolitica solo cambia la politica")
    void contexto() {
        var c = ctx("GENERAL_FITNESS", 30, List.of());
        assertSame(PlanningPolicy.HISTORICA, c.policy());
        var v = c.conPolitica(PlanningPolicy.V4C);
        assertSame(PlanningPolicy.V4C, v.policy());
        assertEquals(c.profile(), v.profile());
        assertEquals(c.prescription(), v.prescription());
    }

    // ---------------------------------------------------------------------------------------------- V4

    @ParameterizedTest(name = "{0} min · {1} s · V4-C excede={2}")
    @CsvSource({"30,2069,false", "30,2070,false", "30,2071,true", "45,3104,false", "45,3105,false", "45,3106,true",
            "60,4139,false", "60,4140,false", "60,4141,true", "75,5174,false", "75,5175,false", "75,5176,true"})
    void v4cBordes(int m, long s, boolean excede) {
        assertEquals(excede, PlanningPolicy.V4C.excedeTecho(s, 0, m));
        assertEquals(m * 69L, PlanningPolicy.limiteV4Segundos(m));
    }

    @Test
    @DisplayName("V4 historico: minutos frente a round(M x 1,15), identico a PlanningProfile.maxSessionMinutes")
    void v4Historico() {
        for (int m = 30; m <= 75; m++) {
            int techo = (int) Math.round(m * 1.15);
            assertFalse(PlanningPolicy.HISTORICA.excedeTecho(999_999, techo, m), "los segundos no cuentan");
            assertTrue(PlanningPolicy.HISTORICA.excedeTecho(0, techo + 1, m));
        }
    }

    // ---------------------------------------------------------------------------------------------- entidad

    @Test
    @DisplayName("entidad: sin politica es historica; registrar una desconocida falla; la version siguiente la hereda")
    void entidad() {
        var plan = WeeklyTrainingPlan.firstVersion(1L, (short) 1, LUNES, LUNES.plusDays(6), GenerationSource.RULE_ENGINE,
                null, "{}", (short) 1);
        assertSame(PlanningPolicy.HISTORICA, plan.politica());
        assertThrows(PlanningPolicy.PoliticaDesconocidaException.class, () -> plan.registrarPolitica("PLAN-??", "{}"));
        plan.registrarPolitica("PLAN-C2D-0.1", "{\"a\":1}");
        var siguiente = WeeklyTrainingPlan.nextVersionOf(plan, GenerationSource.RULE_ENGINE, null, "{}", (short) 1,
                "REPARACION", "motivo");
        assertEquals("PLAN-C2D-0.1", siguiente.getPlanningPolicy(), "una reparacion de la semana no la reinterpreta");
        assertEquals("{\"a\":1}", siguiente.getPlanningPolicyTrace());
    }

    // ---------------------------------------------------------------------------------------------- traza

    @Test
    @DisplayName("traza: segundos por sesion, bajo piso, exencion V8C y excepcion C2-D solo con politica C2-D")
    void traza() {
        var corta = new PlanDraft.DraftWorkout(LUNES, WorkoutFocus.FULL_BODY, "a", 20, List.of(sr(1, 3, 10)));
        var exenta = new PlanDraft.DraftWorkout(LUNES.plusDays(2), WorkoutFocus.FULL_BODY, "b", 20, List.of(sr(1, 3, 10)));
        var est = new SessionDurationEstimator();
        var hist = PlanningPolicyTrace.de(ctx("GENERAL_FITNESS", 45, List.of()), List.of(corta, exenta),
                GenerationSource.RULE_ENGINE, "GENERACION", "MVP-1.4", Set.of(LUNES.plusDays(2)), est);
        assertEquals("PLAN-HIST-1", hist.get("politica"));
        assertEquals(0, hist.get("excepciones_c2d"));
        var c2d = PlanningPolicyTrace.de(ctx("GENERAL_FITNESS", 45, List.of()).conPolitica(PlanningPolicy.C2D),
                List.of(corta, exenta), GenerationSource.RULE_ENGINE, "REGENERACION_P03", "MVP-1.4",
                Set.of(LUNES.plusDays(2)), est);
        assertEquals(1, c2d.get("excepciones_c2d"), "la exenta por V8C no es una excepcion C2-D");
        @SuppressWarnings("unchecked") var ses = (List<Map<String, Object>>) c2d.get("sesiones");
        assertEquals(true, ses.get(0).get("bajo_piso_v20"));
        assertEquals(true, ses.get(0).get("excepcion_c2d"));
        assertEquals(true, ses.get(1).get("exenta_v8c"));
        assertEquals(false, ses.get(1).get("excepcion_c2d"));
        assertEquals(est.estimateSeconds(corta, P), ses.get(0).get("segundos_estimados"));
        assertEquals("REGENERACION_P03", c2d.get("contexto"));
    }

    // ---------------------------------------------------------------------------------------------- saturacion

    static CandidateExercise cand(long id, String grupo, String patron) {
        return new CandidateExercise(id, "e" + id, grupo, "body weight", 1, PrescriptionType.SETS_REPS, null, patron);
    }

    static PlanDraft.DraftExercise sr(long id, int series, int reps) {
        return new PlanDraft.DraftExercise(id, PrescriptionType.SETS_REPS, (short) series, (short) reps, null, null,
                (short) 90, null);
    }

    /** Problemas falsos: V20 si alguna sesion no llega al piso; nada mas. */
    static java.util.function.Function<PlanDraft, List<String>> soloV20(PlanGenerationContext ctx) {
        var est = new SessionDurationEstimator();
        long piso = PlanDraftValidator.minimumSessionSeconds(ctx);
        return d -> d.workouts().stream().filter(w -> est.estimateSeconds(w, ctx.prescription()) < piso)
                .map(w -> "V20: corta " + w.scheduledDate()).toList();
    }

    static PlanDraft borrador(List<PlanDraft.DraftExercise> ejercicios) {
        return new PlanDraft(GenerationSource.RULE_ENGINE, null, "p", null, "r",
                List.of(new PlanDraft.DraftWorkout(LUNES, WorkoutFocus.FULL_BODY, "a", 20, ejercicios)));
    }

    @Test
    @DisplayName("saturacion: sin candidatos ni series que subir -> SATURADA; con un candidato admisible -> NO_SATURADA")
    void saturacion() {
        var en = List.of(cand(1, "chest", "HORIZONTAL_PUSH"), cand(2, "back", "HORIZONTAL_PULL"));
        var ctx = ctx("GENERAL_FITNESS", 75, en);
        var d = borrador(List.of(sr(1, 3, 15), sr(2, 3, 15)));
        var r = SaturacionContenidoV20.evaluar(d, ctx, x -> soloV20(ctx).apply(x).isEmpty() ? List.of()
                : List.of("V15: cualquier cambio rompe la regla de la mitad"), new SessionDurationEstimator(), 100);
        assertEquals(SaturacionContenidoV20.Estado.SATURADA, r.estado(), r.toString());

        var conUno = List.of(cand(1, "chest", "HORIZONTAL_PUSH"), cand(2, "back", "HORIZONTAL_PULL"),
                cand(3, "upper legs", "KNEE_DOMINANT"));
        var ctx2 = ctx("GENERAL_FITNESS", 75, conUno);
        var r2 = SaturacionContenidoV20.evaluar(d, ctx2, soloV20(ctx2), new SessionDurationEstimator(), 100);
        assertEquals(SaturacionContenidoV20.Estado.NO_SATURADA, r2.estado(), r2.toString());
        assertTrue(r2.motivo().startsWith("ANADIR"), r2.motivo());
    }

    @Test
    @DisplayName("saturacion: presupuesto agotado o ejercicio fuera del elegible -> NO_DEMOSTRADA (nunca acepta)")
    void noDemostrada() {
        var en = List.of(cand(1, "chest", "HORIZONTAL_PUSH"), cand(2, "back", "HORIZONTAL_PULL"),
                cand(3, "upper legs", "KNEE_DOMINANT"));
        var ctx = ctx("GENERAL_FITNESS", 75, en);
        var d = borrador(List.of(sr(1, 3, 15), sr(2, 3, 15)));
        assertEquals(SaturacionContenidoV20.Estado.NO_DEMOSTRADA,
                SaturacionContenidoV20.evaluar(d, ctx, x -> List.of("V20: x"), new SessionDurationEstimator(), 0).estado());
        var ajeno = borrador(List.of(sr(99, 3, 15), sr(2, 3, 15)));
        assertEquals(SaturacionContenidoV20.Estado.NO_DEMOSTRADA,
                SaturacionContenidoV20.evaluar(ajeno, ctx, soloV20(ctx), new SessionDurationEstimator(), 100).estado());
    }

    @Test
    @DisplayName("aplicaC2D: solo con politica C2-D, solo si TODO es V20, sin orden de volumen ni autorizaciones")
    void condicionEstricta() {
        var c = ctx("GENERAL_FITNESS", 75, List.of()).conPolitica(PlanningPolicy.C2D);
        var vacio = ConservationReport.vacio();
        assertTrue(PlanDraftValidator.aplicaC2D(c, vacio, List.of("V20: a", "V20: b")));
        assertFalse(PlanDraftValidator.aplicaC2D(c.conPolitica(PlanningPolicy.HISTORICA), vacio, List.of("V20: a")));
        assertFalse(PlanDraftValidator.aplicaC2D(c.conPolitica(PlanningPolicy.V4C), vacio, List.of("V20: a")));
        assertFalse(PlanDraftValidator.aplicaC2D(c, vacio, List.of()));
        for (var otro : List.of("V4: x", "V8C: x", "V15: x", "V18: x", "V22: x", "V23: x", "V24: x", "V25: x", "V1: dolor",
                "sin codigo"))
            assertFalse(PlanDraftValidator.aplicaC2D(c, vacio, List.of("V20: a", otro)), "no se salta " + otro);
    }

    static PlanGenerationContext ctx(String objetivo, int minutos, List<CandidateExercise> elegibles) {
        var perfil = new PlanningProfile(1L, 30, "FEMALE", new BigDecimal("168"), new BigDecimal("68"), null,
                "INTERMEDIATE", objetivo, "x", "Ninguna", "HOME", 3, List.of((short) 1, (short) 3, (short) 5), minutos,
                List.of("body weight"), List.of(), SafetyProfile.Declaradas.sinLimitaciones());
        return new PlanGenerationContext(1L, (short) 1, LUNES, LUNES.plusDays(6), perfil, PlanAdjustment.none(), null,
                elegibles, SafetyProfile.none(), P);
    }
}
