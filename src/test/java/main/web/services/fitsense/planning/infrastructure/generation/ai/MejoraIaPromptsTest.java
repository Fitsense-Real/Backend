package main.web.services.fitsense.planning.infrastructure.generation.ai;

import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.model.valueobjects.PrescriptionType;
import main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion;
import main.web.services.fitsense.shared.infrastructure.json.JsonSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static main.web.services.fitsense.planning.infrastructure.generation.ai.SessionFixPromptV2Test.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Experimento de mejora (MEJ, 07/11/2026): variantes PR-1.8 (generacion) y FIX-PR-2.2 (adaptacion). Las dos estan
 * apagadas por defecto y solo corrigen la COHERENCIA del prompt con lo que el backend ya valida; ninguna regla nueva.
 */
class MejoraIaPromptsTest {

    static final JsonSupport JSON = new JsonSupport();
    static final SessionFixPromptBuilder V21 = new SessionFixPromptBuilder(JSON, "v2.1");
    static final SessionFixPromptBuilder V22 = new SessionFixPromptBuilder(JSON, "v2.2");

    /** O18 (GRA-R2): GAIN_MUSCLE, ACORTAR, cuatro estructurales y un relleno (1141, giros rusos, sin slot 4C). */
    static Escenario o18() {
        var catalogo = new ArrayList<CandidateExercise>(catalogoGm());
        catalogo.add(cand(1141L, "waist", PrescriptionType.SETS_REPS, "abs", "TRUNK_ROTATION_LATERAL_FLEXION", 1));
        List<PlanDraft.DraftExercise> sesion = List.of(sr(380L, 3, 10, 90), sr(351L, 3, 10, 90), sr(637L, 3, 10, 90),
                sr(1073L, 3, 10, 90), sr(1141L, 2, 13, 90));
        return escenario("GAIN_MUSCLE", "INTERMEDIATE", 45, catalogo, null,
                53L, TipoIntervencion.ACORTAR_O_REORGANIZAR, sesion);
    }

    @Test
    @DisplayName("V2.1 reproduce el defecto de O18: ACORTAR en GAIN_MUSCLE sin opciones y 'OMITELA'")
    void v21SinOpcionDeRetirar() {
        var s = FactibilidadAdaptacion.calcular(o18().ctx(), o18().der()).get(0);
        assertTrue(s.retiradas().isEmpty());
        assertFalse(s.hayCambioFactible());
        var t = plano(texto(V21, o18(), null));
        assertTrue(t.contains("D. OPCIONES: ninguna."), t);
        assertTrue(t.contains("OMITELA del arreglo: es la respuesta correcta"), t);
    }

    @Test
    @DisplayName("V2.2: ofrece retirar UNA colocacion de relleno (solo 1141, ninguna estructural) y ya no manda omitir")
    void v22OfreceRetirarElRelleno() {
        var s = FactibilidadAdaptacion.calcular(o18().ctx(), o18().der(), true).get(0);
        assertEquals(List.of(1141L), s.retiradas().stream().map(FactibilidadAdaptacion.Opcion::exerciseId).toList());
        assertTrue(s.retiradas().get(0).cabe(), "sin minimo de duracion: cabe");
        assertTrue(s.retiradas().get(0).segundosRetirados() > 0);
        assertTrue(s.hayCambioFactible());
        var t = plano(texto(V22, o18(), null));
        assertTrue(t.contains("retirar UNA colocacion de relleno"), t);
        assertTrue(t.contains("1141 (2x13) retira"), t);
        assertFalse(t.contains("OMITELA del arreglo: es la respuesta correcta"), t);
        assertTrue(t.contains("DOLOR (prompt V2.2)"), t);
        assertEquals(SessionFixPromptBuilder.VERSION_V22, V22.version());
    }

    @Test
    @DisplayName("V2.2 no ofrece retirar donde la intervencion no lo permite (REVISAR_CARGA) y deja V2.1 igual")
    void v22SoloEnAcortar() {
        var s = FactibilidadAdaptacion.calcular(u03().ctx(), u03().der(), true).get(0);
        assertTrue(s.retiradas().isEmpty());
        assertEquals(plano(texto(V21, u03(), null)).replace("FIX-PR-2.1-EXP", "X"),
                plano(texto(V22, u03(), null)).replace("FIX-PR-2.2-EXP", "X").replaceAll(
                        " DOLOR \\(prompt V2\\.2\\).*?aunque la dosis no se pueda bajar\\.", ""));
    }

    @Test
    @DisplayName("PR-1.8: apagado por defecto (PR-1.7 identico) y, encendido, el bloque de coherencia V15/V26/dosis")
    void generacionPr18() {
        var ctx = PromptCapacidadesTest.contexto("INCREASE_STRENGTH");
        var defecto = new PlanPromptBuilder(JSON).build(ctx, "{}", null);
        var explicito = new PlanPromptBuilder(JSON, "pr-1.7").build(ctx, "{}", null);
        assertEquals(defecto, explicito);
        assertFalse(defecto.contains("COHERENCIA CON LOS VALIDADORES"));
        var b18 = new PlanPromptBuilder(JSON, "pr-1.8");
        assertEquals(PlanPromptBuilder.VERSION_V18, b18.version());
        var v18 = b18.build(ctx, "{}", null);
        assertTrue(v18.contains("COHERENCIA CON LOS VALIDADORES (PR-1.8)"));
        assertTrue(v18.contains("REPARTO (V15)"));
        assertTrue(v18.startsWith(defecto.substring(0, defecto.indexOf("\nDatos de entrada:"))),
                "PR-1.8 = PR-1.7 + un bloque antes de los datos");
        assertThrows(IllegalArgumentException.class, () -> new PlanPromptBuilder(JSON, "pr-9"));
    }
}
