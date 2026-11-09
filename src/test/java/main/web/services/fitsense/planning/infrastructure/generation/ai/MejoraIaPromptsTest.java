package main.web.services.fitsense.planning.infrastructure.generation.ai;

import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanGenerationContext;
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

    // ------------------------------------------------------------------ ronda 2 (09/11/2026)

    static PlanGenerationContext conPrescripcion(String objetivo) {
        var c = PromptCapacidadesTest.contexto(objetivo);
        return new PlanGenerationContext(c.userId(), c.weekNumber(), c.weekStartDate(), c.weekEndDate(), c.profile(),
                c.adjustment(), c.previousWeek(), c.availableExercises(), c.safety(), SessionFixPromptV2Test.P)
                .conPolitica(main.web.services.fitsense.planning.domain.model.valueobjects.PlanningPolicy.REV2);
    }

    @Test
    @DisplayName("pr-1.8a: PR-1.7 + solo las lineas V15/V26 (sin la dosis orientativa); PR-1.7 sigue identico")
    void r2Pr18a() {
        for (var objetivo : List.of("GAIN_MUSCLE", "LOSE_WEIGHT", "INCREASE_STRENGTH", "GENERAL_FITNESS")) {
            var ctx = conPrescripcion(objetivo);
            var base = new PlanPromptBuilder(JSON).build(ctx, "{}", null);
            var a = new PlanPromptBuilder(JSON, "pr-1.8a").build(ctx, "{}", null);
            assertTrue(a.startsWith(base.substring(0, base.indexOf("\nDatos de entrada:"))), objetivo);
            assertTrue(a.contains("COHERENCIA CON LOS VALIDADORES (PR-1.8a-EXP)"), objetivo);
            assertTrue(a.contains("- REPARTO (V15)"), objetivo);
            assertTrue(a.contains("- VOLUMEN POR GRUPO (V26)"), objetivo);
            assertFalse(a.contains("DOSIS ORIENTATIVA"), objetivo);
            assertFalse(a.contains("ESTRUCTURA DE TU OBJETIVO"), objetivo);
            // Las lineas V15/V26 son las mismas de PR-1.8.
            var full = new PlanPromptBuilder(JSON, "pr-1.8").build(ctx, "{}", null);
            for (var l : a.split("\n"))
                if (l.startsWith("- REPARTO") || l.startsWith("- VOLUMEN POR GRUPO")) assertTrue(full.contains(l), l);
        }
        assertEquals("PR-1.8a-EXP", new PlanPromptBuilder(JSON, "pr-1.8a").version());
    }

    @Test
    @DisplayName("pr-1.8b: la dosis orientativa nunca pide subir la dosis; en GAIN_MUSCLE remite a dosis_por_clase (V24)")
    void r2Pr18b() {
        var gm = new PlanPromptBuilder(JSON, "pr-1.8b").build(conPrescripcion("GAIN_MUSCLE"), "{}", null);
        assertTrue(gm.contains("la dosis de cada ejercicio NO se cambia para alcanzarla"), gm);
        assertTrue(gm.contains("constraints.dosis_por_clase): copiala tal cual"), gm);
        assertFalse(gm.contains("acercate a ella"), "la frase de PR-1.8 que empujaba a subir series no viaja");
        var lw = new PlanPromptBuilder(JSON, "pr-1.8b").build(conPrescripcion("LOSE_WEIGHT"), "{}", null);
        if (lw.contains("DOSIS ORIENTATIVA")) assertTrue(lw.contains("constraints.capacidades.dosis"), lw);
        assertFalse(lw.contains("dosis_por_clase): copiala"), lw);
    }

    @Test
    @DisplayName("pr-1.8c: estructura del objetivo con el coste de cada dosis (mismo estimador) y el tiempo util")
    void r2Pr18c() {
        var ctx = conPrescripcion("LOSE_WEIGHT");
        var c = new PlanPromptBuilder(JSON, "pr-1.8c").build(ctx, "{}", null);
        assertTrue(c.contains("ESTRUCTURA DE TU OBJETIVO (LOSE_WEIGHT"), c);
        assertTrue(c.contains("Principales factibles: [CARDIO]"), c);
        var cardio1 = PlanInputSnapshot.of(ctx).constraints().capacidades().dosis().stream()
                .filter(d -> d.codigo().equals("CARDIO_1")).findFirst().orElseThrow();
        long esperado = (long) cardio1.series() * cardio1.segundos()
                + (long) (cardio1.series() - 1) * cardio1.descansoSegundos();
        assertTrue(c.contains("CARDIO_1=" + esperado + " s;"), c);
        assertFalse(c.contains("DOSIS ORIENTATIVA"), c);
        assertTrue(c.contains("Tiempo util por sesion: "), c);
        for (var objetivo : List.of("GAIN_MUSCLE", "INCREASE_STRENGTH", "GENERAL_FITNESS"))
            assertTrue(new PlanPromptBuilder(JSON, "pr-1.8c").build(conPrescripcion(objetivo), "{}", null)
                    .contains("ESTRUCTURA DE TU OBJETIVO (" + objetivo), objetivo);
        assertThrows(IllegalArgumentException.class, () -> new PlanPromptBuilder(JSON, "pr-1.8d"));
    }

    @Test
    @DisplayName("v2.2a: la retirada de un relleno en ACORTAR (O18) sin la linea de dolor; fuera de ACORTAR = V2.1")
    void r2V22a() {
        var b = new SessionFixPromptBuilder(JSON, "v2.2a");
        assertEquals("FIX-PR-2.2a-EXP", b.version());
        var t = plano(texto(b, o18(), null));
        assertTrue(t.contains("1141 (2x13) retira"), t);
        assertFalse(t.contains("OMITELA del arreglo: es la respuesta correcta"), t);
        assertFalse(t.contains("DOLOR (prompt"), t);
        assertTrue(t.contains("ACORTAR (prompt FIX-PR-2.2a-EXP)"), t);
        assertEquals(plano(texto(V21, u03(), null)).replace("FIX-PR-2.1-EXP", "X"),
                plano(texto(b, u03(), null)).replace("FIX-PR-2.2a-EXP", "X").replaceAll(
                        " ACORTAR \\(prompt X\\).*?aunque la dosis no se pueda bajar\\.", ""));
    }

    @Test
    @DisplayName("v2.4: resumen del caso en cinco puntos antes del detalle; el detalle de v2.2a no cambia")
    void r2V24() {
        var b = new SessionFixPromptBuilder(JSON, "v2.4");
        assertEquals("FIX-PR-2.4-EXP", b.version());
        var t = plano(texto(b, o18(), null));
        for (var punto : List.of("1 PROBLEMA:", "2 SESION:", "3 OPCIONES QUE EXISTEN:", "4 LO MODIFICABLE:",
                "5 RESULTADO BUSCADO:"))
            assertTrue(t.contains(punto), punto + " en " + t);
        assertTrue(t.contains("1 rellenos que se pueden retirar (uno)"), t);
        assertTrue(t.indexOf("RESUMEN DEL CASO (V2.4):") < t.indexOf("FACTIBILIDAD (V2;"), t);
        var a = plano(texto(new SessionFixPromptBuilder(JSON, "v2.2a"), o18(), null));
        String fact = a.substring(a.indexOf("FACTIBILIDAD (V2;"), a.indexOf("E. PROHIBIDO"));
        assertTrue(t.contains(fact), "el bloque de factibilidad es el mismo de v2.2a");
        assertThrows(IllegalArgumentException.class, () -> new SessionFixPromptBuilder(JSON, "v2.5"));
    }
}
