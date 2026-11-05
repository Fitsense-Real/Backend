package main.web.services.fitsense.planning.infrastructure.generation.ai;

import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanGenerationContext;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanningPolicy;
import main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static main.web.services.fitsense.planning.infrastructure.generation.ai.SessionFixPromptV2Test.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Rediseno R/R' (P-A1, P-A2): lo que el prompt y la factibilidad previa dicen CON {@code PLAN-REV-2.0}. Con la
 * politica historica el texto es identico byte a byte ({@link PromptHistoricoHuellaTest}); aqui se comprueba que el
 * cambio de REV2 es explicito y coincide con lo que V8C admite.
 */
@DisplayName("Rediseno: el prompt y la factibilidad con PLAN-REV-2.0 dicen lo mismo que V8C")
class PromptRev2Test {

    static final SessionFixPromptBuilder V21 = new SessionFixPromptBuilder(JSON, "v2.1");

    static Escenario rev2(Escenario e) {
        return new Escenario(e.ctx().conPolitica(PlanningPolicy.REV2), e.der());
    }

    @Test
    @DisplayName("P-A2: INVESTIGAR con REV2 ofrece UNA sustitucion equivalente y prohibe combinarla con la dosis")
    void investigarSustituye() {
        for (var b : java.util.List.of(V1, V2, V21)) {
            var t = plano(texto(b, rev2(u04()), AiRetryFeedback.none()));
            assertTrue(t.contains("SUSTITUIR COMO MAXIMO UN ejercicio"), t);
            assertTrue(t.contains("Nunca las dos cosas en la misma sesion"), t);
            var h = plano(texto(b, u04(), AiRetryFeedback.none()));
            assertFalse(h.contains("SUSTITUIR COMO MAXIMO UN"), "la historica no cambia");
        }
    }

    /** El mismo escenario con el impacto del catalogo conocido (high_impact = false en todos). */
    static Escenario conImpactoBajo(Escenario e) {
        var c = e.ctx();
        var catalogo = c.availableExercises().stream().map(x -> new CandidateExercise(x.exerciseId(), x.name(),
                x.bodyPartCode(), x.equipmentCode(), x.difficulty(), x.defaultPrescription(), x.targetMuscle(),
                x.movementPattern(), Boolean.FALSE)).toList();
        return new Escenario(new PlanGenerationContext(c.userId(), c.weekNumber(), c.weekStartDate(), c.weekEndDate(),
                c.profile(), c.adjustment(), c.previousWeek(), catalogo, c.safety(), c.prescription(), c.policy()),
                e.der());
    }

    @Test
    @DisplayName("P-A2: con impacto DESCONOCIDO no se ofrece ningun sustituto (prudencia de ImpactoPolicy)")
    void impactoDesconocido() {
        var s = FactibilidadAdaptacion.calcular(rev2(u04()).ctx(), u04().der()).stream()
                .filter(x -> x.intervencion() == TipoIntervencion.INVESTIGAR_CAMBIO_MINIMO).findFirst().orElseThrow();
        assertTrue(s.sustitutos().isEmpty(), s.sustitutos().toString());
    }

    @Test
    @DisplayName("P-A2: la lista de sustitutos de INVESTIGAR solo trae equivalentes estrictos (72 por 380/496)")
    void listaEstricta() {
        var s = FactibilidadAdaptacion.calcular(rev2(conImpactoBajo(u04())).ctx(), u04().der()).stream()
                .filter(x -> x.intervencion() == TipoIntervencion.INVESTIGAR_CAMBIO_MINIMO).findFirst().orElseThrow();
        assertFalse(s.sustitutos().isEmpty(), "con REV2 hay sustitutos");
        // 380 (chest/pectorals, dif 2): 72 y 9002 (chest/pectorals, dif 1, bajo impacto). Ninguno de otro grupo,
        // patron, mas dificil o de impacto desconocido.
        var porId = new java.util.HashMap<Long, CandidateExercise>();
        conImpactoBajo(u04()).ctx().availableExercises().forEach(c -> porId.put(c.exerciseId(), c));
        s.sustitutos().forEach(x -> assertTrue(main.web.services.fitsense.planning.domain.services.PermisosIntervencion
                .equivalenteEstricto(porId.get(x.reemplaza()), porId.get(x.exerciseId())), x.toString()));
        assertTrue(s.sustitutos().stream().anyMatch(x -> x.reemplaza() == 380L && x.exerciseId() == 72L),
                s.sustitutos().toString());
        var h = FactibilidadAdaptacion.calcular(u04().ctx(), u04().der()).stream()
                .filter(x -> x.intervencion() == TipoIntervencion.INVESTIGAR_CAMBIO_MINIMO).findFirst().orElseThrow();
        assertTrue(h.sustitutos().isEmpty(), "con la historica INVESTIGAR no sustituye");
    }

    @Test
    @DisplayName("P-A1: REVISAR en GAIN_MUSCLE con REV2 ofrece la dosis de mantenimiento (3x10 -> 2x10) y nada mas")
    void fatigaGm() {
        var t = plano(texto(V21, rev2(u03()), AiRetryFeedback.none()));
        assertTrue(t.contains("DOSIS (fatiga, GAIN_MUSCLE)"), t);
        var s = FactibilidadAdaptacion.calcular(rev2(u03()).ctx(), u03().der()).stream()
                .filter(x -> x.intervencion() == TipoIntervencion.REVISAR_CARGA_Y_RECUPERACION).findFirst()
                .orElseThrow();
        assertTrue(s.dosisReducible(), s.toString());
        assertFalse(s.opciones().isEmpty());
        s.opciones().forEach(o -> assertTrue(o.antes().startsWith("3x10") && o.despues().startsWith("2x10"),
                o.toString()));
        var h = FactibilidadAdaptacion.calcular(u03().ctx(), u03().der()).stream()
                .filter(x -> x.intervencion() == TipoIntervencion.REVISAR_CARGA_Y_RECUPERACION).findFirst()
                .orElseThrow();
        assertFalse(h.dosisReducible(), "con la historica 4D no se reduce");
    }

    @Test
    @DisplayName("P-A1 no toca otros objetivos: REVISAR en GENERAL_FITNESS con REV2 no menciona la dosis de mantenimiento")
    void otrosObjetivos() {
        var t = plano(texto(V21, rev2(u18()), AiRetryFeedback.none()));
        assertFalse(t.contains("DOSIS (fatiga, GAIN_MUSCLE)"), t);
    }
}
