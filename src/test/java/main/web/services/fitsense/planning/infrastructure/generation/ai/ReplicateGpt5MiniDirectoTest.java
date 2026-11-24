package main.web.services.fitsense.planning.infrastructure.generation.ai;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import main.web.services.fitsense.shared.infrastructure.json.JsonSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * G5M (23/11/2026): openai/gpt-5-mini DIRECTO en lugar de openai/gpt-5-structured (E002 desde el 17/11/2026).
 * <p>
 * Lo que se fija aqui, sin red:
 * <ol>
 *   <li>La entrada lleva exactamente los parametros de gpt-5-mini (system_prompt, prompt, reasoning_effort,
 *   verbosity, max_completion_tokens) y NO los de structured (model, json_schema), que no se trasladan.</li>
 *   <li>El esquema que viaja en el system_prompt es el MISMO que structured mandaba en json_schema.</li>
 *   <li>La salida fragmentada (arreglo de cadenas) se une; un texto que no es JSON no se convierte en plan.</li>
 *   <li>La linea REPLICATE_CONSUMO: modelo, formato, tokens y coste, sin prompt ni token.</li>
 *   <li>La configuracion por omision y la reversion a structured son solo configuracion.</li>
 * </ol>
 */
class ReplicateGpt5MiniDirectoTest {

    private static final String URL = "https://api.replicate.test/v1/models/openai/gpt-5-mini/predictions";
    private static final ObjectMapper JSON = new ObjectMapper();

    private ListAppender<ILoggingEvent> logs;
    private ch.qos.logback.classic.Logger logger;

    @BeforeEach
    void montarLog() {
        logger = ((LoggerContext) LoggerFactory.getILoggerFactory()).getLogger(ReplicateTrainingPlanGenerator.class);
        logs = new ListAppender<>();
        logs.start();
        logger.addAppender(logs);
        logger.setLevel(Level.INFO);
    }

    @AfterEach
    void soltarLog() {
        logger.detachAppender(logs);
    }

    private static ReplicateProperties directo() {
        return new ReplicateProperties(true, "token-de-prueba", URL, "gpt-5-mini", "low", 150, 0,
                0, 0, 0, 0, null, null, 0);
    }

    /** Captura el cuerpo del POST y responde con {@code respuesta}. */
    private static final class Captura {
        String cuerpo;
    }

    private String llamar(ReplicateProperties p, ReplicateTrainingPlanGenerator.Peticion peticion, String respuesta,
                          Captura captura) {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(p.baseUrl()))
                .andExpect(req -> captura.cuerpo = ((org.springframework.mock.http.client.MockClientHttpRequest) req)
                        .getBodyAsString())
                .andRespond(withSuccess(respuesta, MediaType.APPLICATION_JSON));
        var ia = new ReplicateTrainingPlanGenerator(p, null, null, new JsonSupport(), builder);
        var salida = ia.call("PROMPT-PRIVADO-NO-DEBE-SALIR-EN-EL-LOG", peticion, 7L, (short) 2, 1);
        server.verify();
        return salida;
    }

    @Test
    @DisplayName("La entrada lleva exactamente los parametros de gpt-5-mini y ninguno de structured")
    void entradaDirecta() throws Exception {
        var captura = new Captura();
        llamar(directo(), ReplicateTrainingPlanGenerator.Peticion.deSemana(3), resuelta("\"{}\""), captura);

        var input = JSON.readTree(captura.cuerpo).path("input");
        var claves = new TreeSet<String>();
        input.fieldNames().forEachRemaining(claves::add);
        assertEquals(new TreeSet<>(List.of("system_prompt", "prompt", "reasoning_effort", "verbosity",
                "max_completion_tokens")), claves, captura.cuerpo);
        assertEquals("low", input.path("reasoning_effort").asText());
        assertEquals("low", input.path("verbosity").asText());
        assertEquals(ReplicateProperties.MAX_COMPLETION_TOKENS_POR_OMISION, input.path("max_completion_tokens").asInt());
        assertEquals("PROMPT-PRIVADO-NO-DEBE-SALIR-EN-EL-LOG", input.path("prompt").asText());
    }

    @Test
    @DisplayName("El esquema del system_prompt es el mismo que structured mandaba en json_schema")
    void elEsquemaEsElMismo() throws Exception {
        for (var peticion : List.of(ReplicateTrainingPlanGenerator.Peticion.deSemana(4),
                ReplicateTrainingPlanGenerator.Peticion.deCorrecciones(2))) {
            var sistema = ReplicateTrainingPlanGenerator.instruccionDeSistema(peticion);
            var enviado = JSON.readTree(sistema.substring(sistema.indexOf("JSON Schema:\n") + "JSON Schema:\n".length()));
            @SuppressWarnings("unchecked")
            var format = (Map<String, Object>) peticion.jsonSchema().get("format");
            assertEquals(JSON.valueToTree(format.get("schema")), enviado);
            assertTrue(sistema.contains("\"" + format.get("name") + "\""));
            assertTrue(sistema.contains("UNICAMENTE con un objeto JSON valido"));
        }
        // Determinista entre arranques (la huella del libro V59 depende de ello): claves ordenadas.
        assertEquals(ReplicateTrainingPlanGenerator.instruccionDeSistema(ReplicateTrainingPlanGenerator.Peticion.deSemana(4)),
                ReplicateTrainingPlanGenerator.instruccionDeSistema(ReplicateTrainingPlanGenerator.Peticion.deSemana(4)));
        assertNotEquals(ReplicateTrainingPlanGenerator.instruccionDeSistema(ReplicateTrainingPlanGenerator.Peticion.deSemana(3)),
                ReplicateTrainingPlanGenerator.instruccionDeSistema(ReplicateTrainingPlanGenerator.Peticion.deSemana(4)),
                "el numero de entrenamientos viaja en el esquema");
    }

    @Test
    @DisplayName("La salida fragmentada se une en orden; el texto no se reinterpreta")
    void salidaFragmentada() {
        var salida = llamar(directo(), ReplicateTrainingPlanGenerator.Peticion.deSemana(3),
                resuelta("[\"{\\\"plan_\", \"name\\\":\", \"\\\"A\\\"}\"]"), new Captura());
        assertEquals("{\"plan_name\":\"A\"}", salida);
    }

    @Test
    @DisplayName("Un texto que no es JSON no se convierte en plan: el ensamblador lo rechaza")
    void textoNoJsonSeRechaza() {
        var assembler = new PlanDraftFromJsonAssembler(new JsonSupport());
        assertThrows(main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException.class,
                () -> assembler.toDraft("Claro, aqui tienes tu plan: ...", "gpt-5-mini"));
        assertThrows(main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException.class,
                () -> assembler.toDraft("", "gpt-5-mini"), "respuesta vacia (tokens agotados en razonamiento)");
        assertThrows(main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException.class,
                () -> assembler.toDraft("{\"schema_version\":\"1\",\"plan_name\":\"A\",\"workouts\":[{", "gpt-5-mini"),
                "JSON cortado por max_completion_tokens");
    }

    @Test
    @DisplayName("REPLICATE_CONSUMO sin tokens informados: no_reportado y cota por el tope de salida")
    void consumoSinTokens() {
        llamar(directo(), ReplicateTrainingPlanGenerator.Peticion.deSemana(3),
                "{\"id\":\"abc123\",\"status\":\"succeeded\",\"output\":\"{}\",\"metrics\":{\"predict_time\":3.93,"
                        + "\"total_time\":3.96},\"urls\":null}", new Captura());
        var linea = linea("REPLICATE_CONSUMO");
        assertTrue(linea.contains("modelo=gpt-5-mini formato=direct prediction_id=abc123 resultado=SUCCEEDED"), linea);
        assertTrue(linea.contains("predict_time_s=3.930 tokens_entrada=no_reportado tokens_salida=no_reportado"
                + " coste_usd=no_reportado"), linea);
        assertTrue(linea.contains("caracteres_salida=2 reasoning_effort=low verbosity=low max_completion_tokens=16000"),
                linea);
        // Cota: 1 token por caracter de entrada + 16.000 de salida a 2,00 USD/M = 0,032 + entrada.
        var cota = Double.parseDouble(linea.replaceAll(".*coste_cota_usd=(\\S+).*", "$1"));
        assertTrue(cota > 0.032 && cota < 0.04, linea);
        assertFalse(linea.contains("PROMPT-PRIVADO"), "el prompt no se registra");
        assertFalse(linea.contains("token-de-prueba"), "el token no se registra");
        assertFalse(logs.list.stream().map(ILoggingEvent::getFormattedMessage)
                .anyMatch(m -> m.contains("PROMPT-PRIVADO") || m.contains("token-de-prueba")), "nada en INFO/WARN");
    }

    @Test
    @DisplayName("REPLICATE_CONSUMO con tokens informados: coste con la tarifa")
    void consumoConTokens() {
        llamar(directo(), ReplicateTrainingPlanGenerator.Peticion.deSemana(3),
                "{\"id\":\"abc124\",\"status\":\"succeeded\",\"output\":[\"{\",\"}\"],\"metrics\":{\"predict_time\":10.0,"
                        + "\"token_input_count\":40000,\"token_output_count\":5000},\"urls\":null}", new Captura());
        var linea = linea("REPLICATE_CONSUMO");
        // 40.000 x 0,25/M + 5.000 x 2,00/M = 0,01 + 0,01
        assertTrue(linea.contains("tokens_entrada=40000 tokens_salida=5000 coste_usd=0.020000"), linea);
    }

    @Test
    @DisplayName("Configuracion: por omision gpt-5-mini directo; structured solo si se configura")
    void configuracion() {
        var omision = new ReplicateProperties(true, "t", null, null, null, 0, 0);
        assertEquals(ReplicateProperties.URL_GPT5_MINI, omision.baseUrl());
        assertEquals(ReplicateProperties.Formato.DIRECT, omision.formato());
        assertEquals("gpt-5-mini", omision.modeloEfectivo(), "el modelo lo fija el endpoint, no la etiqueta");
        assertEquals("low", omision.verbosity());
        assertEquals(16000, omision.maxCompletionTokens());

        var estructurada = new ReplicateProperties(true, "t",
                "https://api.replicate.com/v1/models/openai/gpt-5-structured/predictions", "gpt-5-mini", "medium",
                0, 0);
        assertEquals(ReplicateProperties.Formato.STRUCTURED, estructurada.formato(), "se deduce de la URL");
        assertEquals("gpt-5-mini", estructurada.modeloEfectivo());

        var forzada = new ReplicateProperties(true, "t", URL, "x", "low", 0, 0, 0, 0, 0, 0, "STRUCTURED", null, 0);
        assertEquals(ReplicateProperties.Formato.STRUCTURED, forzada.formato());
        assertThrows(IllegalArgumentException.class,
                () -> new ReplicateProperties(true, "t", URL, "x", "low", 0, 0, 0, 0, 0, 0, "json", null, 0));
        assertEquals(1000, new ReplicateProperties(true, "t", URL, "x", "low", 0, 0, 0, 0, 0, 0, null, null, 5)
                .maxCompletionTokens(), "acotado, no rechazado");
    }

    @Test
    @DisplayName("El plan publicado se etiqueta con el modelo del endpoint")
    void etiquetaDelModelo() {
        var p = new ReplicateProperties(true, "t", URL, "gpt-5-nano", "low", 0, 0);
        var ia = new ReplicateTrainingPlanGenerator(p, null, null, new JsonSupport(), RestClient.builder());
        assertEquals("gpt-5-mini", ia.modelName());
    }

    private static String resuelta(String output) {
        return "{\"id\":\"mc7xdrmr\",\"status\":\"succeeded\",\"output\":" + output + ",\"error\":null,\"logs\":null,"
                + "\"urls\":null}";
    }

    private String linea(String prefijo) {
        return logs.list.stream().map(ILoggingEvent::getFormattedMessage).filter(m -> m.startsWith(prefijo))
                .reduce((a, b) -> b).orElseThrow(() -> new AssertionError("sin linea " + prefijo));
    }
}
