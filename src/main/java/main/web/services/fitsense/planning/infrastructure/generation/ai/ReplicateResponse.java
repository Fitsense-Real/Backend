package main.web.services.fitsense.planning.infrastructure.generation.ai;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Respuesta de Replicate.
 * <p>
 * output se declara como JsonNode y no como tipo concreto porque su forma
 * depende del modelo: unos devuelven texto, otros un arreglo de fragmentos, y
 * gpt-5-structured un objeto envuelto en json_output.
 */
record ReplicateResponse(
        String id,
        String status,
        JsonNode output,
        String error,
        String logs,
        Urls urls,
        Metrics metrics
) {
    /** urls.get es la direccion para consultar el estado de una prediccion en curso. */
    record Urls(String get, String cancel, String web) {}

    /**
     * G5M (23/11/2026): {@code metrics} de Replicate. gpt-5-structured informaba token_input_count y
     * token_output_count; openai/gpt-5-mini directo (comprobado el 23/11/2026) solo informa predict_time y total_time,
     * asi que los tokens pueden llegar nulos y el coste se registra entonces como "no_reportado".
     */
    record Metrics(Double predictTime, Long tokenInputCount, Long tokenOutputCount) {}

    /** Sin metricas (respuestas armadas a mano: reanudacion, cancelacion). */
    ReplicateResponse(String id, String status, JsonNode output, String error, String logs, Urls urls) {
        this(id, status, output, error, logs, urls, null);
    }

    boolean succeeded() {
        return "succeeded".equals(status);
    }

    /** Sigue trabajando: hay que volver a consultar, no es un fallo. */
    boolean pending() {
        return "starting".equals(status) || "processing".equals(status);
    }

    String pollUrl() {
        return urls == null ? null : urls.get();
    }

    String joinedOutput() {
        if (output == null || output.isNull()) return "";
        if (output.isTextual()) return output.asText();
        if (output.has("json_output")) return output.get("json_output").toString();
        if (output.has("text")) return output.get("text").asText();
        if (output.isArray()) {
            var sb = new StringBuilder();
            output.forEach(n -> sb.append(n.isTextual() ? n.asText() : n.toString()));
            return sb.toString();
        }
        return output.toString();
    }
}