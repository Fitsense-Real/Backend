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

@DisplayName("Politica de planificacion versionada: catalogo, seleccion, V4-C, y traza")
class PoliticaPlanificacionTest {

    static final LocalDate LUNES = LocalDate.of(2026, 11, 2);
    static final PrescriptionParams P = new PrescriptionParams(70, 3, 60, 5, 60, 20, null, null, 10, 180);

    // ---------------------------------------------------------------------------------------------- catalogo

    @Test
    @DisplayName("catalogo cerrado: null es la historica, desconocida es error; solo HIST y V4-C en esta rama")
    void catalogo() {
        assertSame(PlanningPolicy.HISTORICA, PlanningPolicy.deRegistro(null));
        assertSame(PlanningPolicy.HISTORICA, PlanningPolicy.deRegistro("  "));
        assertSame(PlanningPolicy.V4C, PlanningPolicy.deRegistro("PLAN-V4-0.1"));
        assertThrows(PlanningPolicy.PoliticaDesconocidaException.class, () -> PlanningPolicy.deRegistro("PLAN-C2-0.1"),
                "PLAN-C2-0.1 designaba C2-C: no se reutiliza");
        assertThrows(PlanningPolicy.PoliticaDesconocidaException.class, () -> PlanningPolicy.deRegistro("PLAN-C2D-0.1"),
                "C2-D no forma parte de esta rama");
        assertThrows(PlanningPolicy.PoliticaDesconocidaException.class, () -> PlanningPolicy.deRegistro("PLAN-E-0.1"),
                "el modelo E no tiene version");
        assertTrue(PlanningPolicy.V4C.v4Segundos() && PlanningPolicy.V4C.candidata());
        assertFalse(PlanningPolicy.HISTORICA.v4Segundos());
        assertFalse(PlanningPolicy.HISTORICA.candidata());
        assertEquals(2, PlanningPolicy.CATALOGO.size());
    }

    @Test
    @DisplayName("seleccion: historica por defecto; una candidata exige permiso explicito; desconocida no arranca")
    void seleccion() {
        assertSame(PlanningPolicy.HISTORICA, PlanningPolicySelector.historica().activa());
        assertSame(PlanningPolicy.HISTORICA, new PlanningPolicySelector("PLAN-HIST-1", false).activa());
        assertThrows(IllegalStateException.class, () -> new PlanningPolicySelector("PLAN-V4-0.1", false));
        assertThrows(IllegalStateException.class, () -> new PlanningPolicySelector("PLAN-X", true));
        assertThrows(IllegalStateException.class, () -> new PlanningPolicySelector("PLAN-C2D-0.1", true));
        assertSame(PlanningPolicy.V4C, new PlanningPolicySelector("PLAN-V4-0.1", true).activa());
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
        plan.registrarPolitica("PLAN-V4-0.1", "{\"a\":1}");
        var siguiente = WeeklyTrainingPlan.nextVersionOf(plan, GenerationSource.RULE_ENGINE, null, "{}", (short) 1,
                "REPARACION", "motivo");
        assertEquals("PLAN-V4-0.1", siguiente.getPlanningPolicy(), "una reparacion de la semana no la reinterpreta");
        assertEquals("{\"a\":1}", siguiente.getPlanningPolicyTrace());
    }

    // ---------------------------------------------------------------------------------------------- traza

    static PlanDraft.DraftExercise sr(long id, int series, int reps) {
        return new PlanDraft.DraftExercise(id, PrescriptionType.SETS_REPS, (short) series, (short) reps, null, null,
                (short) 90, null);
    }

    @Test
    @DisplayName("traza: politica, contexto y segundos estimados por sesion; bajo piso y exencion V8C solo informativos")
    void traza() {
        var corta = new PlanDraft.DraftWorkout(LUNES, WorkoutFocus.FULL_BODY, "a", 20, List.of(sr(1, 3, 10)));
        var exenta = new PlanDraft.DraftWorkout(LUNES.plusDays(2), WorkoutFocus.FULL_BODY, "b", 20, List.of(sr(1, 3, 10)));
        var est = new SessionDurationEstimator();
        var hist = PlanningPolicyTrace.de(ctx("GENERAL_FITNESS", 45, List.of()), List.of(corta, exenta),
                GenerationSource.RULE_ENGINE, "GENERACION", "MVP-1.4", Set.of(LUNES.plusDays(2)), est);
        assertEquals("PLAN-HIST-1", hist.get("politica"));
        assertFalse(hist.containsKey("excepciones_c2d"), "sin C2-D en esta rama");
        var v4 = PlanningPolicyTrace.de(ctx("GENERAL_FITNESS", 45, List.of()).conPolitica(PlanningPolicy.V4C),
                List.of(corta, exenta), GenerationSource.RULE_ENGINE, "REGENERACION_P03", "MVP-1.4",
                Set.of(LUNES.plusDays(2)), est);
        assertEquals("PLAN-V4-0.1", v4.get("politica"));
        @SuppressWarnings("unchecked") var ses = (List<Map<String, Object>>) v4.get("sesiones");
        assertEquals(true, ses.get(0).get("bajo_piso_v20"));
        assertEquals(true, ses.get(1).get("exenta_v8c"));
        assertFalse(ses.get(0).containsKey("excepcion_c2d"));
        assertEquals(est.estimateSeconds(corta, P), ses.get(0).get("segundos_estimados"));
        assertEquals("REGENERACION_P03", v4.get("contexto"));
    }

    static PlanGenerationContext ctx(String objetivo, int minutos, List<CandidateExercise> elegibles) {
        var perfil = new PlanningProfile(1L, 30, "FEMALE", new BigDecimal("168"), new BigDecimal("68"), null,
                "INTERMEDIATE", objetivo, "x", "Ninguna", "HOME", 3, List.of((short) 1, (short) 3, (short) 5), minutos,
                List.of("body weight"), List.of(), SafetyProfile.Declaradas.sinLimitaciones());
        return new PlanGenerationContext(1L, (short) 1, LUNES, LUNES.plusDays(6), perfil, PlanAdjustment.none(), null,
                elegibles, SafetyProfile.none(), P);
    }
}
