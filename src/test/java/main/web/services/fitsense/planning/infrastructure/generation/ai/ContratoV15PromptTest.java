package main.web.services.fitsense.planning.infrastructure.generation.ai;

import main.web.services.fitsense.planning.domain.model.valueobjects.WorkoutFocus;
import main.web.services.fitsense.planning.domain.services.CatalogEligibilityPolicy;
import main.web.services.fitsense.planning.domain.services.RepartoV15;
import main.web.services.fitsense.shared.infrastructure.json.JsonSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * H2 · contrato prompt–V15 (sin Replicate). Con el interruptor apagado (configuracion evaluada PR-1.7) el prompt y la
 * instantanea no cambian ni un byte; encendido, la IA recibe la regla de la mitad tal como la verifica V15.
 */
@DisplayName("H2 · contrato prompt–V15: apagado identico a PR-1.7; encendido, la regla de V15 tal cual")
class ContratoV15PromptTest {

    static final JsonSupport JSON = new JsonSupport();

    static PlanPromptBuilder builder(boolean contrato) {
        var b = new PlanPromptBuilder(JSON);
        b.setContratoV15(contrato);
        return b;
    }

    @Test
    @DisplayName("apagado: la instantanea es exactamente la de PR-1.7 (sin campos nuevos) y el prompt no cambia")
    void apagadoIdentico() {
        for (var objetivo : new String[]{"LOSE_WEIGHT", "GENERAL_FITNESS", "INCREASE_STRENGTH", "GAIN_MUSCLE"}) {
            var ctx = PromptCapacidadesTest.contexto(objetivo);
            var historica = JSON.write(PlanInputSnapshot.of(ctx));
            assertEquals(historica, JSON.write(PlanInputSnapshot.of(ctx, false)), objetivo);
            assertFalse(historica.contains("max_per_body_part_por_n_trabajo"), objetivo);
            assertFalse(historica.contains("complementos_no_cuentan"), objetivo);
            assertTrue(historica.contains("\"prompt_version\":\"PR-1.7\""), objetivo);
            var defecto = new PlanPromptBuilder(JSON).build(ctx, historica, null);
            assertEquals(defecto, builder(false).build(ctx, historica, null), objetivo);
            assertEquals("PR-1.7", builder(false).version());
        }
    }

    @Test
    @DisplayName("encendido: tabla por n identica a RepartoV15, complementos de V15, min <= max_colocaciones (C1-C3 = 0)")
    void encendidoSinContradicciones() {
        for (var objetivo : new String[]{"LOSE_WEIGHT", "GENERAL_FITNESS", "INCREASE_STRENGTH", "GAIN_MUSCLE"}) {
            var ctx = PromptCapacidadesTest.contexto(objetivo);
            var s = PlanInputSnapshot.of(ctx, true);
            var c = s.constraints();
            assertEquals("PR-1.9", s.promptVersion(), objetivo);
            // C1: el minimo nunca pide mas ejercicios de los que el tope admite.
            if (c.minExercisesPerSession() != null && c.maxColocacionesDia() != null)
                assertTrue(c.minExercisesPerSession() <= c.maxColocacionesDia(), objetivo + ": " + c);
            assertEquals(CatalogEligibilityPolicy.complementos(ctx.availableExercises(), ctx.profile().goalType())
                    .stream().sorted().toList(), c.complementosNoCuentan(), objetivo);
            for (var sesion : c.suggestedSplit()) {
                assertNull(sesion.maxPerBodyPart(), "un solo numero no puede decir la regla de la mitad");
                int grupos = WorkoutFocus.valueOf(sesion.focusCode()).bodyPartCodes().size();
                int maxTrabajo = c.maxColocacionesDia() == null ? 10 : c.maxColocacionesDia();
                var tabla = sesion.maxPerBodyPartPorNTrabajo();
                if (grupos < RepartoV15.MIN_GRUPOS || maxTrabajo < RepartoV15.MIN_TRABAJO) {
                    assertNull(tabla, objetivo + " " + sesion.focusCode() + ": sin regla, sin tabla");
                    continue;
                }
                // C3: cuando la regla aplica, se comunica; C2: con el mismo valor que verifica V15, para cada n.
                assertNotNull(tabla, objetivo + " " + sesion.focusCode());
                for (int n = RepartoV15.MIN_TRABAJO; n <= maxTrabajo; n++)
                    assertEquals(RepartoV15.topeMismoGrupo(grupos, n), tabla.get(Integer.toString(n)),
                            objetivo + " " + sesion.focusCode() + " n=" + n);
            }
        }
    }

    @Test
    @DisplayName("encendido: las capacidades gobiernan, asi que no se comunica el minimo calculado con target_sets")
    void sinMinimoConCapacidades() {
        for (var objetivo : new String[]{"LOSE_WEIGHT", "GENERAL_FITNESS", "INCREASE_STRENGTH"})
            assertNull(PlanInputSnapshot.of(PromptCapacidadesTest.contexto(objetivo), true).constraints()
                    .minExercisesPerSession(), objetivo);
    }

    @Test
    @DisplayName("encendido: el prompt dice la regla de V15 y ya no la frase de PR-1.7 (regla 15 y comprobacion final)")
    void textoEncendido() {
        for (var objetivo : new String[]{"LOSE_WEIGHT", "GAIN_MUSCLE"}) {
            var ctx = PromptCapacidadesTest.contexto(objetivo);
            var prompt = builder(true).build(ctx, JSON.write(PlanInputSnapshot.of(ctx, true)), null);
            assertFalse(PlanPromptBuilder.REGLA15_HISTORICA.matcher(prompt).find(), objetivo);
            assertTrue(prompt.contains(PlanPromptBuilder.REGLA15_V15), objetivo);
            assertFalse(PlanPromptBuilder.COMPROBACION_HISTORICA.matcher(prompt).find(), objetivo);
            if (objetivo.equals("LOSE_WEIGHT")) assertTrue(prompt.contains(PlanPromptBuilder.COMPROBACION_V15));
            assertEquals("PR-1.9", builder(true).version());
        }
    }

    @Test
    @DisplayName("la adaptacion no depende del interruptor: AdaptedWeekCommandService no lo lee (V2.1 intacta)")
    void adaptacionNoLeeElInterruptor() {
        // H2 solo corrige la generacion (PR-1.7 -> PR-1.9). La adaptacion proyecta la instantanea historica
        // (min_exercises_per_session incluido) en SessionFixInputSnapshot; si leyera la bandera, encenderla cambiaria
        // en silencio lo que recibe la IA en V2.1, que no se ha evaluado.
        for (var campo : main.web.services.fitsense.planning.application.internal.commandservices
                .AdaptedWeekCommandService.class.getDeclaredFields()) {
            var valor = campo.getAnnotation(org.springframework.beans.factory.annotation.Value.class);
            assertFalse(valor != null && valor.value().contains("contrato-v15"), campo.getName());
        }
        for (var objetivo : new String[]{"LOSE_WEIGHT", "GENERAL_FITNESS", "INCREASE_STRENGTH", "GAIN_MUSCLE"}) {
            var ctx = PromptCapacidadesTest.contexto(objetivo);
            assertEquals(JSON.write(SessionFixInputSnapshot.of(PlanInputSnapshot.of(ctx), "v2.1")),
                    JSON.write(SessionFixInputSnapshot.of(PlanInputSnapshot.of(ctx, false), "v2.1")), objetivo);
        }
    }

    @Test
    @DisplayName("si el texto historico cambia, la variante falla en vez de quedarse a medias")
    void falloRapido() {
        assertThrows(IllegalStateException.class, () -> PlanPromptBuilder.aplicarContratoV15("sin la regla", false));
    }
}
