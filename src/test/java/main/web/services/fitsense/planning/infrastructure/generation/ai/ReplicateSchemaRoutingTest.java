package main.web.services.fitsense.planning.infrastructure.generation.ai;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import main.web.services.fitsense.shared.infrastructure.json.JsonSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * QUE VIAJA EN EL POST Y COMO QUEDA REGISTRADO, segun quien pregunte.
 * <p>
 * El transporte es compartido -{@code call} hace el POST, el sondeo y la linea
 * REPLICATE_TIMING para las dos- y eso es deliberado. Lo que NO puede
 * compartirse son dos cosas, y esta prueba existe porque las dos se
 * confundieron una vez cada una:
 * <ol>
 *   <li><b>El esquema.</b> {@code callMedido} llamaba a
 *       {@code PlanJsonSchema.format(workoutCount)} FIJO, asi que una
 *       adaptacion habria pedido al modelo una semana entera.</li>
 *   <li><b>La etiqueta del prompt.</b> La linea "Salida de la IA" imprimia
 *       {@code PlanPromptBuilder.VERSION} y {@code PrescriptionPrinciples.VERSION}
 *       FIJOS. Esa linea es la evidencia de la tesis sobre que propuso el modelo
 *       en cada intento; con la etiqueta equivocada no sirve para comparar.</li>
 * </ol>
 * SE ENTRA POR {@code call}, no por {@code ReplicateSessionFixGenerator}. A
 * proposito: lo que se mide aqui es el transporte, y montar un
 * {@code PlanGenerationContext} entero solo para eso probaria otra cosa.
 * <p>
 * NO SE EJECUTO EN EL ENTORNO DE PREPARACION: repo1.maven.org responde 403 y sin
 * {@code spring-test} no hay {@code MockRestServiceServer}. Hay que correrla en
 * el IDE.
 * <p>
 * Es rapida: las dos predicciones vuelven resueltas del POST, asi que no hay
 * sondeo y no se tocan los 3 s de {@code POLL_INTERVAL}.
 */
class ReplicateSchemaRoutingTest {

    private static final String URL = "https://api.replicate.test/predictions";

    /** Lo que se espera en el log para cada tipo de peticion. */
    private static final String ETIQUETA_SEMANA =
            PlanPromptBuilder.VERSION + " " + PrescriptionPrinciples.VERSION;
    private static final String ETIQUETA_CORRECCION =
            SessionFixPromptBuilder.VERSION + " " + SessionFixJsonSchema.VERSION;

    private ReplicateTrainingPlanGenerator generator;
    private MockRestServiceServer server;
    private ListAppender<ILoggingEvent> logs;
    private ch.qos.logback.classic.Logger logger;

    @BeforeEach
    void montar() {
        var builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        var properties = new ReplicateProperties(
                true, "token-de-prueba", URL, "gpt-5-nano", "medium", 150);
        // promptBuilder y assembler van a null: no se usan en call().
        generator = new ReplicateTrainingPlanGenerator(
                properties, null, null, new JsonSupport(), builder);

        logger = ((LoggerContext) LoggerFactory.getILoggerFactory())
                .getLogger(ReplicateTrainingPlanGenerator.class);
        logs = new ListAppender<>();
        logs.start();
        logger.addAppender(logs);
        logger.setLevel(Level.INFO);
    }

    @AfterEach
    void soltarLog() {
        logger.detachAppender(logs);
    }

    // =======================================================================
    // El esquema que viaja
    // =======================================================================

    @Test
    @DisplayName("Generacion inicial: viaja weekly_training_plan, como antes")
    void generacionInicialMandaElEsquemaDeSemana() {
        server.expect(requestTo(URL))
                .andExpect(jsonPath("$.input.json_schema.format.name").value("weekly_training_plan"))
                .andRespond(withSuccess(resuelta(), MediaType.APPLICATION_JSON));

        generator.call("prompt", 3, 7L, (short) 1, 1);

        server.verify();
    }

    @Test
    @DisplayName("Adaptacion: viaja corrected_sessions, NO el esquema de semana")
    void adaptacionMandaElEsquemaDeCorrecciones() {
        server.expect(requestTo(URL))
                .andExpect(jsonPath("$.input.json_schema.format.name").value("corrected_sessions"))
                .andRespond(withSuccess(resuelta(), MediaType.APPLICATION_JSON));

        generator.call("prompt",
                ReplicateTrainingPlanGenerator.Peticion.deCorrecciones(2),
                7L, (short) 1, 1);

        server.verify();
    }

    @Test
    @DisplayName("Y el esquema de correcciones lleva el tope de sesiones que se pidieron")
    void elEsquemaSeDimensionaConLasSenaladas() {
        // Sin HTTP: se mira el mapa que construye Peticion. Es lo que impide que
        // el modelo devuelva sesiones que nadie senalo, y el numero viaja en la
        // descripcion del arreglo, que es lo que el modelo lee.
        var dos = ReplicateTrainingPlanGenerator.Peticion.deCorrecciones(2);
        var cinco = ReplicateTrainingPlanGenerator.Peticion.deCorrecciones(5);

        assertTrue(descripcionDeSesiones(dos).contains("Hasta 2 sesiones"),
                () -> descripcionDeSesiones(dos));
        assertTrue(descripcionDeSesiones(cinco).contains("Hasta 5 sesiones"),
                () -> "el tope no se dimensiona: " + descripcionDeSesiones(cinco));
    }

    @SuppressWarnings("unchecked")
    private static String descripcionDeSesiones(ReplicateTrainingPlanGenerator.Peticion peticion) {
        var format = (java.util.Map<String, Object>) peticion.jsonSchema().get("format");
        var schema = (java.util.Map<String, Object>) format.get("schema");
        var properties = (java.util.Map<String, Object>) schema.get("properties");
        var sessions = (java.util.Map<String, Object>) properties.get("sessions");
        return String.valueOf(sessions.get("description"));
    }

    // =======================================================================
    // Como queda registrado
    // =======================================================================

    @Test
    @DisplayName("La linea de la generacion inicial NO cambia: misma etiqueta que antes")
    void laEtiquetaDeLaSemanaSeConserva() {
        // El parche convirtio dos marcadores en uno. La linea rendida tiene que
        // ser la misma: si no, las corridas anteriores dejan de ser comparables
        // con las nuevas, que es el motivo por el que esa linea existe.
        server.expect(requestTo(URL))
                .andRespond(withSuccess(resuelta(), MediaType.APPLICATION_JSON));

        generator.call("prompt", 3, 7L, (short) 1, 1);

        var linea = salidaDeLaIa();
        assertTrue(linea.contains("prompt " + ETIQUETA_SEMANA + ","),
                () -> "se esperaba la etiqueta " + ETIQUETA_SEMANA + " y salio: " + linea);
    }

    @Test
    @DisplayName("La de una adaptacion se identifica como adaptacion, no como semana nueva")
    void laEtiquetaDeLaAdaptacionEsSuya() {
        server.expect(requestTo(URL))
                .andRespond(withSuccess(resuelta(), MediaType.APPLICATION_JSON));

        generator.call("prompt",
                ReplicateTrainingPlanGenerator.Peticion.deCorrecciones(2),
                7L, (short) 1, 1);

        var linea = salidaDeLaIa();
        assertTrue(linea.contains("prompt " + ETIQUETA_CORRECCION + ","),
                () -> "se esperaba la etiqueta " + ETIQUETA_CORRECCION + " y salio: " + linea);

        // Lo que fallaba antes del parche, y por lo que esta prueba vale algo:
        // la etiqueta del prompt inicial no puede aparecer en una adaptacion.
        assertFalse(linea.contains(ETIQUETA_SEMANA),
                () -> "la adaptacion quedo registrada como generacion inicial: " + linea);
    }

    // =======================================================================
    // Material
    // =======================================================================

    /** Una prediccion que vuelve resuelta del POST: sin sondeo, sin esperas. */
    private static String resuelta() {
        return """
               {"id":"mc7xdrmr","status":"succeeded","output":{"json_output":{"sessions":[]}},
                "error":null,"logs":null,"urls":null}
               """;
    }

    private String salidaDeLaIa() {
        return logs.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .filter(m -> m.startsWith("Salida de la IA"))
                .reduce((primera, ultima) -> ultima)
                .orElseThrow(() -> new AssertionError(
                        "no se registro ninguna linea 'Salida de la IA'"));
    }
}
