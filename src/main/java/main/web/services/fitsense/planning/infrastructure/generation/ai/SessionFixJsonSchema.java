package main.web.services.fitsense.planning.infrastructure.generation.ai;

import java.util.List;
import java.util.Map;

/**
 * Esquema JSON del CONTRATO A: la IA devuelve las sesiones corregidas, no un
 * plan.
 * <p>
 * SEPARADO DE {@code PlanJsonSchema} A PROPOSITO. El contrato de generacion
 * inicial no se toca: sigue pidiendo un plan entero para la primera semana. Este
 * es otro contrato, para otra pregunta -«corrige estas sesiones»-, y mezclarlos
 * obligaria a que un mismo esquema describiera dos cosas distintas.
 * <p>
 * LO QUE NO ESTA AQUI ES LA MITAD DEL DISENO. No hay {@code focus_code} ni
 * {@code workout_name}: el enfoque y el nombre los pone el empalme desde la copia
 * de la semana anterior, asi que la IA no tiene por donde cambiarlos. La regla de
 * V8C «una sesion senalada se corrige, no se reemplaza por otra» queda
 * garantizada por el contrato y no por la validacion.
 * <p>
 * Y {@code source_planned_workout_id} es OBLIGATORIO y NO nullable: es la
 * identidad de la sesion que se corrige. Sin el no se sabe que se corrigio, y
 * emparejar por fecha es justo lo que V31 existe para evitar -con
 * {@code SCHEDULE_CHANGE} la fecha cambia a proposito-.
 * <p>
 * DOS RESTRICCIONES DEL MODO ESTRICTO, heredadas de {@code PlanJsonSchema} y
 * aprendidas a golpes alli:
 * <ul>
 *   <li>Nada de {@code minItems} ni {@code maxItems}: el modo estricto de salida
 *       estructurada de OpenAI los rechaza y la peticion entera falla. El numero
 *       de sesiones va en la {@code description}, que es lo unico que se puede
 *       poner donde el modelo genera.</li>
 *   <li>{@code required} tiene que listar TODAS las propiedades, asi que lo
 *       opcional se expresa con tipo nullable y no omitiendolo.</li>
 * </ul>
 */
final class SessionFixJsonSchema {

    /**
     * La version del contrato. Va como enum de UN valor para que el modo
     * estricto obligue al modelo a emitirla exactamente, y el ensamblador la
     * vuelve a comprobar: si manana cambia el contrato, una respuesta de la
     * version vieja se rechaza en vez de interpretarse mal.
     */
    static final String VERSION = "FIX-1.0";

    private SessionFixJsonSchema() {}

    /**
     * @param sessionCount cuantas sesiones senalo la orden. Viaja como
     *                     descripcion, no como minItems/maxItems. Que vengan
     *                     todas no se exige aqui: conservar una senalada esta
     *                     permitido y el empalme lo admite.
     */
    static Map<String, Object> format(int sessionCount) {
        return Map.of("format", Map.of(
                "type", "json_schema",
                "name", "corrected_sessions",
                "schema", schema(sessionCount)));
    }

    private static Map<String, Object> schema(int sessionCount) {
        return object(
                Map.of(
                        "schema_version", enumOf(VERSION),
                        "rationale", type("string"),
                        "sessions", Map.of("type", "array", "items", session(),
                                "description", ("Hasta %d sesiones corregidas, una por cada "
                                        + "source_planned_workout_id de las sesiones senaladas. "
                                        + "No incluyas ninguna otra sesion del plan: las demas se "
                                        + "conservan tal cual. Si no encuentras una correccion "
                                        + "mejor para una sesion senalada, omitela del arreglo en "
                                        + "vez de devolverla igual.").formatted(sessionCount))),
                List.of("schema_version", "rationale", "sessions"));
    }

    private static Map<String, Object> session() {
        return object(
                Map.of(
                        "source_planned_workout_id", Map.of("type", "integer",
                                "description", "El identificador EXACTO de la sesion senalada "
                                        + "que estas corrigiendo, tal como se te dio. No lo "
                                        + "inventes ni lo cambies."),
                        "scheduled_date", nullable("string"),
                        "expected_duration_minutes", nullable("integer"),
                        "exercises", Map.of("type", "array", "items", exercise(),
                                "description", "La prescripcion COMPLETA de la sesion corregida, "
                                        + "no un parche: lo que no pongas aqui no estara en la "
                                        + "sesion. Sin repetir exercise_id dentro de la misma "
                                        + "sesion. El ORDEN es la posicion en este arreglo."),
                        "change_reason", type("string")),
                List.of("source_planned_workout_id", "scheduled_date",
                        "expected_duration_minutes", "exercises", "change_reason"));
    }

    private static Map<String, Object> exercise() {
        return object(
                Map.of(
                        // exercise_order NO esta: el orden efectivo es la
                        // posicion en el arreglo, y dos fuentes de verdad para
                        // el mismo dato es un defecto esperando a ocurrir.
                        "exercise_id", type("integer"),
                        "prescription_type", enumOf("SETS_REPS", "DURATION"),
                        "planned_sets", nullable("integer"),
                        "planned_reps", nullable("integer"),
                        "planned_duration_seconds", nullable("integer"),
                        "target_load_kg", nullable("number"),
                        "rest_seconds", nullable("integer"),
                        "notes", nullable("string")),
                List.of("exercise_id", "prescription_type", "planned_sets",
                        "planned_reps", "planned_duration_seconds", "target_load_kg",
                        "rest_seconds", "notes"));
    }

    private static Map<String, Object> object(Map<String, Object> properties, List<String> required) {
        return Map.of(
                "type", "object",
                "properties", properties,
                "required", required,
                "additionalProperties", false);
    }

    private static Map<String, Object> type(String type) {
        return Map.of("type", type);
    }

    private static Map<String, Object> nullable(String type) {
        return Map.of("type", List.of(type, "null"));
    }

    private static Map<String, Object> enumOf(String... values) {
        return Map.of("type", "string", "enum", List.of(values));
    }
}
