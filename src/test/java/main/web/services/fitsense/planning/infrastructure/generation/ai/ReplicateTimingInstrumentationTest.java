package main.web.services.fitsense.planning.infrastructure.generation.ai;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import main.web.services.fitsense.planning.domain.exceptions.AiProviderUnavailableException;
import main.web.services.fitsense.shared.infrastructure.json.JsonSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Los cuatro caminos de una llamada a Replicate, con el HTTP simulado y
 * LEYENDO LA LINEA DEL LOG, que es el entregable de este parche: si la linea no
 * sale, o sale con un campo inventado, la medicion de la serie no vale.
 * <p>
 * NO SE EJECUTO EN EL ENTORNO DE PREPARACION: repo1.maven.org responde 403 y
 * sin {@code spring-test} no hay {@code MockRestServiceServer}. Las dos pruebas
 * puras -{@code AiCircuitBreakerTest} y {@code ReplicateTimingTest}, 8 + 9- si
 * se ejecutaron. Esta hay que correrla en el IDE.
 * <p>
 * TARDA UNOS 13 SEGUNDOS. {@code POLL_INTERVAL} son 3 s reales y no se toca:
 * cambiarlo para que la prueba corra antes seria probar otra cosa.
 */
class ReplicateTimingInstrumentationTest {

    /** Lo que se hace esperar al POST para probar que su tramo se mide de verdad. */
    private static final long ESPERA_SIMULADA_MS = 300;

    private static final String URL = "https://api.replicate.test/predictions";
    private static final String POLL = "https://api.replicate.test/predictions/mc7xdrmr";

    private ReplicateTrainingPlanGenerator generator;
    private MockRestServiceServer server;
    private ListAppender<ILoggingEvent> logs;
    private ch.qos.logback.classic.Logger logger;

    private void montar(int timeoutSeconds) {
        var builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        var properties = new ReplicateProperties(
                true, "token-de-prueba", URL, "gpt-5-nano", "medium", timeoutSeconds, 0);
        // promptBuilder y assembler no se usan en call(): se entra por debajo de
        // requestOutput a proposito, para no tener que montar un
        // PlanGenerationContext entero y que la prueba mida solo el HTTP.
        generator = new ReplicateTrainingPlanGenerator(
                properties, null, null, new JsonSupport(), builder);
    }

    @BeforeEach
    void capturarLog() {
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

    /** La linea REPLICATE_TIMING partida en pares clave=valor. Falla si no hay ninguna. */
    private Map<String, String> timing() {
        var linea = logs.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .filter(m -> m.startsWith("REPLICATE_TIMING"))
                .reduce((primera, ultima) -> ultima);
        assertTrue(linea.isPresent(), "no se registro ninguna linea REPLICATE_TIMING");

        var campos = new LinkedHashMap<String, String>();
        for (var trozo : linea.get().substring("REPLICATE_TIMING ".length()).split(" ")) {
            var par = trozo.split("=", 2);
            campos.put(par[0], par.length > 1 ? par[1] : "");
        }
        return campos;
    }

    private static void dormir(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String prediccion(String status, boolean conUrls) {
        return """
               {"id":"mc7xdrmr","status":"%s","output":%s,"error":null,"logs":null,
                "urls":%s}
               """.formatted(status,
                "succeeded".equals(status) ? "{\"json_output\":{\"workouts\":[]}}" : "null",
                conUrls ? "{\"get\":\"" + POLL + "\",\"cancel\":null,\"web\":null}" : "null");
    }

    @Test
    @DisplayName("1 · el POST vuelve resuelto: no hay sondeo y el tramo sale en cero")
    void postDirectoSinSondeo() {
        montar(150);
        server.expect(requestTo(URL))
                .andRespond(withSuccess(prediccion("succeeded", false), MediaType.APPLICATION_JSON));

        var salida = generator.call("prompt", 3, 7L, (short) 1, 1);

        assertFalse(salida.isBlank());
        server.verify();

        var t = timing();
        assertEquals("SUCCEEDED", t.get("resultado"));
        assertEquals("0", t.get("polling_ms"), "sin sondeo el tramo es cero, no el tiempo del POST");
        assertEquals("0", t.get("polling_requests"));
        assertEquals("mc7xdrmr", t.get("prediction_id"));
        assertEquals("7", t.get("user"));
        assertEquals("1", t.get("attempt"));
        assertEquals(t.get("post_ms"), t.get("total_ms"), "el total es solo el POST");
    }

    @Test
    @DisplayName("2 · la prediccion necesita varias consultas: se cuentan y se mide el tramo")
    void variasConsultas() {
        montar(30);
        server.expect(requestTo(URL))
                .andRespond(withSuccess(prediccion("starting", true), MediaType.APPLICATION_JSON));
        server.expect(requestTo(POLL))
                .andRespond(withSuccess(prediccion("processing", true), MediaType.APPLICATION_JSON));
        server.expect(requestTo(POLL))
                .andRespond(withSuccess(prediccion("succeeded", true), MediaType.APPLICATION_JSON));

        generator.call("prompt", 3, 7L, (short) 2, 1);
        server.verify();

        var t = timing();
        assertEquals("SUCCEEDED", t.get("resultado"));
        assertEquals("2", t.get("polling_requests"), "dos consultas, no una ni tres");
        assertTrue(Long.parseLong(t.get("polling_ms")) >= 6_000,
                "dos esperas de 3 s: el tramo de sondeo tiene que reflejarlas");
        assertEquals(Long.parseLong(t.get("post_ms")) + Long.parseLong(t.get("polling_ms")),
                Long.parseLong(t.get("total_ms")));
    }

    @Test
    @DisplayName("3 · se agota el sondeo: resultado=TIMEOUT y la linea sale aunque se lance")
    void timeoutEnSondeo() {
        montar(4);
        server.expect(requestTo(URL))
                .andRespond(withSuccess(prediccion("starting", true), MediaType.APPLICATION_JSON));
        server.expect(ExpectedCount.manyTimes(), requestTo(POLL))
                .andRespond(withSuccess(prediccion("processing", true), MediaType.APPLICATION_JSON));

        // Es el caso de los planes 6 y 7 del 13/10/2026: la prediccion seguia
        // viva y el pipeline la dio por perdida.
        assertThrows(AiProviderUnavailableException.class,
                () -> generator.call("prompt", 3, 7L, (short) 3, 2));

        var t = timing();
        assertEquals("TIMEOUT", t.get("resultado"),
                "aqui las consultas SI respondieron: lo que se agoto fue el limite. "
                        + "Es el unico caso que dice algo sobre timeout-seconds");
        assertEquals("mc7xdrmr", t.get("prediction_id"), "el id se conocia: no se pierde al lanzar");
        assertEquals("2", t.get("attempt"), "el intento ata el tiempo a esta generacion");
        assertTrue(Integer.parseInt(t.get("polling_requests")) >= 1);
    }

    @Test
    @DisplayName("4 · el POST falla antes de dar un id: prediction_id=desconocido, nunca inventado")
    void errorEnPost() {
        montar(150);
        server.expect(requestTo(URL)).andRespond(withServerError());

        assertThrows(AiProviderUnavailableException.class,
                () -> generator.call("prompt", 3, 7L, (short) 4, 1));

        var t = timing();
        assertEquals("POST_ERROR", t.get("resultado"));
        assertEquals("desconocido", t.get("prediction_id"));
        assertEquals("0", t.get("polling_ms"), "no se llego a sondear");
        assertEquals("0", t.get("polling_requests"));
    }

    @Test
    @DisplayName("4b · un POST que tarda en fallar NO se registra como 0 ms")
    void elPostQueTardaEnFallarSeMide() {
        montar(150);
        server.expect(requestTo(URL)).andRespond(peticion -> {
            dormir(ESPERA_SIMULADA_MS);
            return withServerError().createResponse(peticion);
        });

        assertThrows(AiProviderUnavailableException.class,
                () -> generator.call("prompt", 3, 7L, (short) 4, 1));

        var t = timing();
        assertEquals("POST_ERROR", t.get("resultado"));
        // Antes del 13/10/2026 esto salia 0 pasara lo que pasara, y por tanto un
        // POST que revienta a los 50 s se escribia igual que uno instantaneo.
        assertTrue(Long.parseLong(t.get("post_ms")) >= ESPERA_SIMULADA_MS,
                "cuanto tardo en fallar tambien es dato: salio " + t.get("post_ms") + " ms");
    }

    @Test
    @DisplayName("5 · un GET de sondeo que falla es POLL_ERROR, NO un timeout")
    void falloDeConsultaNoEsTimeout() {
        // El limite son 150 s y esto ocurre a los ~3: llamarlo TIMEOUT seria
        // decir que el tiempo de espera se quedo corto, que es falso, y sobre esa
        // confusion se iba a decidir si ampliarlo.
        montar(150);
        server.expect(requestTo(URL))
                .andRespond(withSuccess(prediccion("starting", true), MediaType.APPLICATION_JSON));
        server.expect(requestTo(POLL)).andRespond(withServerError());

        assertThrows(AiProviderUnavailableException.class,
                () -> generator.call("prompt", 3, 7L, (short) 5, 1));

        var t = timing();
        assertEquals("POLL_ERROR", t.get("resultado"));
        assertEquals("1", t.get("polling_requests"), "fallo en la primera consulta");
        assertTrue(Long.parseLong(t.get("polling_ms")) < 150_000,
                "ocurrio muchisimo antes del limite, y el registro tiene que decirlo");
        assertEquals("mc7xdrmr", t.get("prediction_id"), "el id ya se conocia");
    }

    @Test
    @DisplayName("5b · una prediccion en curso sin urls.get tambien es POLL_ERROR")
    void sinUrlDeConsultaEsPollError() {
        montar(150);
        server.expect(requestTo(URL))
                .andRespond(withSuccess(prediccion("starting", false), MediaType.APPLICATION_JSON));

        assertThrows(AiProviderUnavailableException.class,
                () -> generator.call("prompt", 3, 7L, (short) 5, 1));

        var t = timing();
        assertEquals("POLL_ERROR", t.get("resultado"),
                "no se pudo preguntar; no es que dejaramos de esperar");
        assertEquals("0", t.get("polling_requests"));
    }

    @Test
    @DisplayName("la linea no lleva el prompt, ni el token, ni nada del participante")
    void laLineaNoFiltraNada() {
        montar(150);
        server.expect(requestTo(URL))
                .andRespond(withSuccess(prediccion("succeeded", false), MediaType.APPLICATION_JSON));

        generator.call("PROMPT-SECRETO-CON-DATOS", 3, 7L, (short) 1, 1);

        var linea = logs.list.stream().map(ILoggingEvent::getFormattedMessage)
                .filter(m -> m.startsWith("REPLICATE_TIMING")).findFirst().orElseThrow();
        assertFalse(linea.contains("PROMPT-SECRETO-CON-DATOS"));
        assertFalse(linea.contains("token-de-prueba"));
    }

    @Test
    @DisplayName("el estado no exitoso que no es timeout se distingue del que si lo es")
    void falloQueNoEsTimeout() {
        montar(150);
        server.expect(requestTo(URL))
                .andRespond(withSuccess(prediccion("failed", false), MediaType.APPLICATION_JSON));

        assertThrows(AiProviderUnavailableException.class,
                () -> generator.call("prompt", 3, 7L, (short) 1, 1));

        assertEquals("NOT_SUCCEEDED", timing().get("resultado"),
                "'failed' es el proveedor diciendo que no; TIMEOUT es nosotros dejando de esperar");
    }

    @Test
    @DisplayName("no cambia lo que ya hacia: sigue lanzando AiProviderUnavailableException")
    void laDecisionNoSeMueve() {
        montar(150);
        server.expect(requestTo(URL)).andRespond(withServerError());

        // La instrumentacion no toca la politica 19.4. El pipeline tiene que
        // seguir viendo exactamente la misma excepcion que antes, porque es la
        // unica que cuenta para el cortacircuitos.
        var e = assertThrows(AiProviderUnavailableException.class,
                () -> generator.call("prompt", 3, null, (short) 1, 0));
        assertNotNull(e.problems());

        var t = timing();
        assertEquals("desconocido", t.get("user"), "sin usuario no se inventa uno");
        assertEquals("desconocido", t.get("attempt"));
    }
}