package main.web.services.fitsense.planning.infrastructure.generation.ai;

import main.web.services.fitsense.shared.infrastructure.json.JsonSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * El esquema del contrato A, comprobado estructuralmente.
 * <p>
 * No se puede probar contra Replicate desde aqui, pero si se puede comprobar
 * que cumple las dos reglas del modo estricto de salida estructurada, que son
 * las que ya rompieron la peticion una vez en este proyecto:
 * <ul>
 *   <li>ni {@code minItems} ni {@code maxItems} en ningun sitio;</li>
 *   <li>{@code required} lista TODAS las propiedades de cada objeto.</li>
 * </ul>
 * Y lo que decide el diseno: que {@code source_planned_workout_id} sea
 * obligatorio y NO nullable, y que el enfoque y el nombre NO esten.
 */
class SessionFixJsonSchemaTest {

    private final JsonSupport json = new JsonSupport();

    @Test
    @DisplayName("Es JSON valido y tiene la forma que Replicate espera")
    void esJsonValido() {
        var serializado = json.write(SessionFixJsonSchema.format(2));

        assertTrue(serializado.contains("\"type\":\"json_schema\""));
        assertTrue(serializado.contains("\"name\":\"corrected_sessions\""));
        assertTrue(serializado.contains("source_planned_workout_id"));
    }

    @Test
    @DisplayName("Ni minItems ni maxItems: el modo estricto los rechaza y tumba la peticion")
    void sinMinItemsNiMaxItems() {
        var serializado = json.write(SessionFixJsonSchema.format(3));

        assertFalse(serializado.contains("minItems"),
                "el modo estricto de OpenAI rechaza minItems y la peticion entera falla");
        assertFalse(serializado.contains("maxItems"),
                "lo mismo con maxItems");
        assertTrue(serializado.contains("Hasta 3 sesiones"),
                "el numero va en la descripcion, que es lo unico que se admite");
    }

    @Test
    @DisplayName("Todo objeto declara additionalProperties false y required completo")
    void requiredCompletoEnTodoObjeto() {
        var faltantes = new ArrayList<String>();
        recorrer(SessionFixJsonSchema.format(2), "raiz", faltantes);

        assertTrue(faltantes.isEmpty(),
                () -> "el modo estricto exige que required liste todas las propiedades: "
                        + faltantes);
    }

    @Test
    @DisplayName("source_planned_workout_id es obligatorio y NO nullable")
    void elOrigenEsObligatorio() {
        var sesion = sesion(SessionFixJsonSchema.format(2));

        @SuppressWarnings("unchecked")
        var required = (List<String>) sesion.get("required");
        assertTrue(required.contains("source_planned_workout_id"));

        @SuppressWarnings("unchecked")
        var propiedades = (Map<String, Object>) sesion.get("properties");
        @SuppressWarnings("unchecked")
        var origen = (Map<String, Object>) propiedades.get("source_planned_workout_id");

        assertEquals("integer", origen.get("type"),
                "si fuera nullable, la IA podria devolver una correccion sin decir que corrige, "
                        + "y emparejar por fecha es lo que V31 existe para evitar");
    }

    @Test
    @DisplayName("El enfoque y el nombre NO estan en el contrato")
    void elEnfoqueNoViajaEnElContrato() {
        var serializado = json.write(SessionFixJsonSchema.format(2));

        assertFalse(serializado.contains("focus_code"),
                "lo pone el empalme desde la copia: la IA no tiene por donde cambiarlo");
        assertFalse(serializado.contains("workout_name"),
                "igual el nombre: la sesion sigue siendo la misma sesion");
    }

    @Test
    @DisplayName("La descripcion le dice que omita una sesion en vez de devolverla igual")
    void instruyeOmitirEnVezDeRepetir() {
        var serializado = json.write(SessionFixJsonSchema.format(2));

        assertTrue(serializado.contains("omitela del arreglo"),
                "devolver una sesion identica gastaria tokens y V8C la contaria igual "
                        + "como sin efecto");
        assertTrue(serializado.contains("las demas se conservan"),
                "tiene que saber que no debe tocar el resto del plan");
    }

    // -----------------------------------------------------------------------

    /** El objeto que describe una sesion corregida, dentro del esquema. */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> sesion(Map<String, Object> format) {
        var formato = (Map<String, Object>) format.get("format");
        var esquema = (Map<String, Object>) formato.get("schema");
        var propiedades = (Map<String, Object>) esquema.get("properties");
        var sessions = (Map<String, Object>) propiedades.get("sessions");
        return (Map<String, Object>) sessions.get("items");
    }

    /**
     * Recorre el esquema y anota todo objeto cuyo required no cubra sus
     * propiedades, o que no declare additionalProperties false.
     */
    @SuppressWarnings("unchecked")
    private static void recorrer(Object nodo, String ruta, List<String> faltantes) {
        if (!(nodo instanceof Map<?, ?> mapa)) return;
        var m = (Map<String, Object>) mapa;

        if ("object".equals(m.get("type"))) {
            var propiedades = (Map<String, Object>) m.get("properties");
            var required = (List<String>) m.get("required");

            if (!Boolean.FALSE.equals(m.get("additionalProperties")))
                faltantes.add(ruta + ": sin additionalProperties false");

            if (propiedades != null) {
                var declaradas = required == null ? List.<String>of() : required;
                for (var clave : propiedades.keySet())
                    if (!declaradas.contains(clave))
                        faltantes.add(ruta + "." + clave + ": no esta en required");
            }
        }

        for (var entrada : m.entrySet()) {
            var valor = entrada.getValue();
            if (valor instanceof Map<?, ?>)
                recorrer(valor, ruta + "." + entrada.getKey(), faltantes);
            else if (valor instanceof List<?> lista)
                for (var elemento : lista) recorrer(elemento, ruta + "." + entrada.getKey(), faltantes);
        }
    }
}
