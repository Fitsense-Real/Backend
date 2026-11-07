package main.web.services.fitsense.planning.infrastructure.generation.ai;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static main.web.services.fitsense.planning.infrastructure.generation.ai.SessionFixPromptV2Test.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Prompt V2.1 (candidata posterior K20, experimental): V2 mas indicaciones de PERTINENCIA en el texto. No cambia
 * ninguna regla, validador ni la factibilidad calculada; V1 y V2 quedan identicos.
 */
@DisplayName("Prompt de adaptacion V2.1 (experimental): pertinencia por causa sin cambiar reglas")
class SessionFixPromptV21Test {

    static final SessionFixPromptBuilder V21 = new SessionFixPromptBuilder(JSON, "v2.1");

    @Test
    @DisplayName("1. Version e indicador: v2.1 -> FIX-PR-2.1-EXP; v1 y v2 sin cambios; valor desconocido no arranca")
    void version() {
        assertEquals(SessionFixPromptBuilder.VERSION_V21, V21.version());
        assertEquals(SessionFixPromptBuilder.VERSION, V1.version());
        assertEquals(SessionFixPromptBuilder.VERSION_V2, V2.version());
        assertThrows(IllegalArgumentException.class, () -> new SessionFixPromptBuilder(JSON, "v9"));
        assertFalse(texto(V2, u09(), AiRetryFeedback.none()).contains("V2.1"), "V2 no recibe nada de V2.1");
        assertFalse(texto(V1, u09(), AiRetryFeedback.none()).contains("FACTIBILIDAD"), "V1 intacto");
    }

    @Test
    @DisplayName("2. V2.1 contiene todo V2: quitando sus anadidos queda el texto de V2 (salvo la version)")
    void v21SoloAnade() {
        for (var e : new Escenario[]{u03(), u04(), u08(), u09(), u18(), u19()}) {
            String v2 = texto(V2, e, AiRetryFeedback.none()).replace(SessionFixPromptBuilder.VERSION_V2, "X");
            String v21 = texto(V21, e, AiRetryFeedback.none()).replace(SessionFixPromptBuilder.VERSION_V21, "X")
                    .replaceAll("(?s)PERTINENCIA \\(prompt V2\\.1\\).*?no reescribas la sesion entera\\.\\n", "")
                    .replaceAll("(?m)^    G\\. [^\\n]*\\n(      [^\\n]*\\n)*", "");
            // V2.1 retira la invitacion a combinar cambios SOLO en la sesion de cambio minimo
            String v2SinCombinarEnMinimo = v2;
            if (v21.length() != v2.length())
                v2SinCombinarEnMinimo = quitarCombinarEnCambioMinimo(v2);
            assertEquals(v2SinCombinarEnMinimo, v21);
        }
    }

    private static String quitarCombinarEnCambioMinimo(String v2) {
        var partes = v2.split("(?=\\n- source_planned_workout_id: )");
        var sb = new StringBuilder();
        for (var p : partes)
            sb.append(p.contains("Investigar que dificulta") ? p.replaceAll("(?m)^      Puedes combinar cambios.*\\n", "") : p);
        return sb.toString();
    }

    @Test
    @DisplayName("3. Cambio minimo (INVESTIGAR): una sola opcion sobre un solo ejercicio y sin invitacion a combinar")
    void cambioMinimo() {
        var f = FactibilidadAdaptacion.calcular(u19().ctx(), u19().der()).stream()
                .filter(x -> x.plannedWorkoutId() == 111L).findFirst().orElseThrow();
        String t = FactibilidadAdaptacion.textoV21(f, u19().ctx().availableExercises());
        assertTrue(t.contains("G. CAMBIO MINIMO (V2.1): aplica UNA sola opcion [cabe] de D, sobre UN solo ejercicio."), t);
        assertFalse(t.contains("Puedes combinar cambios"), t);
        // la factibilidad (opciones, margen) es la misma que en V2
        assertTrue(t.contains(FactibilidadAdaptacion.texto(f).lines().filter(l -> l.contains("C. LIMITES")).findFirst()
                .orElseThrow()));
    }

    @Test
    @DisplayName("4. Demasiado dificil con variantes mas faciles del mismo musculo: se listan con su dificultad")
    void masFacilConVariantes() {
        var f = FactibilidadAdaptacion.calcular(u09().ctx(), u09().der()).stream()
                .filter(x -> x.plannedWorkoutId() == 50L).findFirst().orElseThrow();
        String t = FactibilidadAdaptacion.textoV21(f, u09().ctx().availableExercises());
        // 1106 (dif 2, pectorals): 72 (dif 1, pectorals) admite la misma dosis
        assertTrue(t.contains("- 1106 (dif 2, pectorals): 72 (dif 1)"), t);
        assertTrue(t.contains("Usa uno o dos de estos"), t);
        // 994 (dif 1): no hay nada mas facil
        assertTrue(t.contains("- 994 (dif 1, upper back): ninguno"), t);
    }

    @Test
    @DisplayName("5. Demasiado dificil en principiante (todo dificultad 1): la indicacion es bajar la dosis")
    void masFacilSinVariantes() {
        var f = FactibilidadAdaptacion.calcular(u19().ctx(), u19().der()).stream()
                .filter(x -> x.plannedWorkoutId() == 110L).findFirst().orElseThrow();
        String t = FactibilidadAdaptacion.textoV21(f, u19().ctx().availableExercises());
        assertTrue(t.contains("No hay ninguna variante de menor dificultad registrada del mismo musculo"), t);
        assertTrue(t.contains("BAJA LA DOSIS"), t);
    }

    @Test
    @DisplayName("6. Fatiga con opciones: bajar trabajo sin sustituir; GM sin solucion: sigue diciendo que se omita")
    void cansancio() {
        var f18 = FactibilidadAdaptacion.calcular(u18().ctx(), u18().der()).get(0);
        assertTrue(FactibilidadAdaptacion.textoV21(f18, u18().ctx().availableExercises()).contains("G. CANSANCIO (V2.1)"));
        var f03 = FactibilidadAdaptacion.calcular(u03().ctx(), u03().der()).get(0);
        String t03 = FactibilidadAdaptacion.textoV21(f03, u03().ctx().availableExercises());
        assertFalse(t03.contains("G. CANSANCIO"), "sin opciones no se invita a bajar trabajo");
        assertTrue(t03.contains("OMITELA del arreglo"), t03);
    }

    @Test
    @DisplayName("7. El bloque general de pertinencia solo aparece con v2.1")
    void bloqueGeneral() {
        assertTrue(plano(texto(V21, u04(), AiRetryFeedback.none())).contains("PERTINENCIA (prompt V2.1)"));
        assertFalse(plano(texto(V2, u04(), AiRetryFeedback.none())).contains("PERTINENCIA"));
    }
}
