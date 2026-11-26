package main.web.services.fitsense.planning.infrastructure.generation.ai;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import main.web.services.fitsense.configuration.domain.model.valueobjects.PrescriptionParams;
import main.web.services.fitsense.planning.application.internal.commandservices.PlanGenerationPipeline;
import main.web.services.fitsense.planning.domain.exceptions.AiProviderUnavailableException;
import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.domain.services.PlanDraftNormalizer;
import main.web.services.fitsense.planning.domain.services.PlanDraftValidator;
import main.web.services.fitsense.planning.domain.services.TrainingPlanGenerator;
import main.web.services.fitsense.shared.infrastructure.json.JsonSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RT · TRANSPORTE DE REPLICATE: una prediccion que existe no se da por fallida ni se paga dos veces (27/11/2026).
 * <p>
 * Origen: en la evaluacion H2-v4 (G08, replica 2, brazo B) el POST recibio cabeceras 2xx sin {@code Content-Type} y un
 * cuerpo cortado. Spring lanzo {@code "Error while extracting response for type [java.lang.String] and content type
 * [application/octet-stream]"}, el backend lo trato como fallo del proveedor y la prediccion
 * {@code g40bmfgq8nrzr0d236ktnedbfm} termino (y se facturo) en Replicate sin que nadie la leyera.
 * <p>
 * Escenarios T01-T14 de la orden. Todo contra {@link ReplicateFalso} (un socket local con el cliente HTTP de
 * produccion) o con libros en memoria: <b>ninguna llamada a Replicate</b>, ningun token real.
 */
@DisplayName("RT · transporte de Replicate: recuperar la prediccion existente, sin duplicar ni inventar")
class ReplicateRecuperacionTransporteTest {

    private static final String ID = "g40bmfgq8nrzr0d236ktnedbfm";
    private static final String PROMPT = "prompt de prueba";
    private static final long USUARIO = 7L;
    private static final String SALIDA_OK = "\"{\\\"ok\\\":1}\"";
    private static final ObjectMapper JSON = new ObjectMapper();

    // ------------------------------------------------------------------ el guion del Replicate falso

    /** Que hace "Replicate": un modo por POST (en orden), y como responde a listados y consultas. */
    static final class Escenario implements ReplicateFalso.Guion {
        final Deque<String> modosPost = new ArrayDeque<>();
        final Deque<String> salidas = new ArrayDeque<>();
        volatile boolean creada;
        volatile boolean enListado = true;
        volatile boolean entradaDistinta;
        volatile boolean duplicada;
        volatile String estadoFinal = "succeeded";
        volatile int consultasEnCurso;
        volatile int fallosDeConsulta;
        volatile JsonNode entradaFija;
        final AtomicInteger consultasDeLaNuestra = new AtomicInteger();
        String salidaActual = SALIDA_OK;
        ReplicateFalso replicate;

        Escenario post(String... modos) {
            modosPost.addAll(List.of(modos));
            return this;
        }

        @Override
        public void responder(ReplicateFalso.Peticion p, java.net.Socket s, java.io.OutputStream out) throws Exception {
            var base = replicate.base();
            if (p.metodo().equals("POST") && p.ruta().startsWith("/v1/models/")) {
                var modo = modosPost.isEmpty() ? "normal" : modosPost.poll();
                if (!salidas.isEmpty()) salidaActual = salidas.poll();
                consultasDeLaNuestra.set(0);
                switch (modo) {
                    case "normal" -> {
                        creada = true;
                        ReplicateFalso.json(out, 201, ReplicateFalso.prediccion(ID, "starting", null,
                                replicate.ultimaEntrada(), base));
                    }
                    case "g08" -> {
                        creada = true;
                        ReplicateFalso.cortadaSinTipo(s, out, "{\"id\":\"g40bm");
                    }
                    case "truncada" -> {
                        creada = true;
                        ReplicateFalso.truncada(out, "{\"id\":\"g40bm");
                    }
                    case "a-medias" -> {
                        creada = true;
                        ReplicateFalso.colgadaAMedias(out, "{\"id\":\"g40bm");
                    }
                    case "cuelga" -> {
                        creada = true;
                        Thread.sleep(10_000);
                    }
                    case "lento" -> {
                        creada = true;
                        Thread.sleep(400);
                        ReplicateFalso.json(out, 201, ReplicateFalso.prediccion(ID, "starting", null,
                                replicate.ultimaEntrada(), base));
                    }
                    case "cierra" -> { /* cierra sin responder ni crear */ }
                    case "502-creada" -> {
                        creada = true;
                        ReplicateFalso.json(out, 502, "{\"detail\":\"bad gateway\"}");
                    }
                    case "502" -> ReplicateFalso.json(out, 502, "{\"detail\":\"bad gateway\"}");
                    case "422" -> ReplicateFalso.json(out, 422, "{\"detail\":\"input invalido\"}");
                    case "sin-id" -> {
                        creada = true;
                        ReplicateFalso.json(out, 201, "{\"status\":\"starting\"}");
                    }
                    default -> throw new IllegalStateException(modo);
                }
                return;
            }
            if (p.metodo().equals("GET") && (p.ruta().equals("/v1/predictions") || p.ruta().startsWith("/v1/predictions?"))) {
                var ids = new ArrayList<String>();
                ids.add("otra1");
                if (creada && enListado) ids.add(0, ID);
                if (creada && enListado && duplicada) ids.add("dup2");
                ReplicateFalso.json(out, 200, ReplicateFalso.listado(ids.toArray(String[]::new)));
                return;
            }
            if (p.metodo().equals("POST") && p.ruta().endsWith("/cancel")) {
                ReplicateFalso.json(out, 200, ReplicateFalso.prediccion(ID, "canceled", null, null, base));
                return;
            }
            if (p.metodo().equals("GET") && p.ruta().startsWith("/v1/predictions/")) {
                var id = p.ruta().substring("/v1/predictions/".length());
                var nuestra = entradaFija != null ? entradaFija : replicate.ultimaEntrada();
                if (id.equals("otra1")) {
                    var otra = JSON.createObjectNode().put("prompt", "la peticion de otro usuario");
                    ReplicateFalso.json(out, 200, ReplicateFalso.prediccion("otra1", "succeeded", SALIDA_OK, otra, base));
                    return;
                }
                if (id.equals("dup2")) {
                    ReplicateFalso.json(out, 200, ReplicateFalso.prediccion("dup2", "succeeded", SALIDA_OK, nuestra, base));
                    return;
                }
                int n = consultasDeLaNuestra.incrementAndGet();
                if (n <= fallosDeConsulta) return; // corta la conexion sin responder
                var entrada = entradaDistinta ? JSON.createObjectNode().put("prompt", "otra entrada") : nuestra;
                var estado = n <= fallosDeConsulta + consultasEnCurso ? "processing" : estadoFinal;
                ReplicateFalso.json(out, 200, ReplicateFalso.prediccion(ID, estado,
                        "succeeded".equals(estado) ? salidaActual : null, entrada, base));
                return;
            }
            ReplicateFalso.json(out, 404, "{\"detail\":\"no existe\"}");
        }
    }

    private final List<AutoCloseable> abiertos = new ArrayList<>();
    private ListAppender<ILoggingEvent> logs;
    private ch.qos.logback.classic.Logger logger;

    @BeforeEach
    void capturar() {
        logger = ((LoggerContext) LoggerFactory.getILoggerFactory()).getLogger(ReplicateTrainingPlanGenerator.class);
        logs = new ListAppender<>();
        logs.start();
        logger.addAppender(logs);
        logger.setLevel(Level.INFO);
    }

    @AfterEach
    void cerrar() throws Exception {
        logger.detachAppender(logs);
        for (var a : abiertos) a.close();
    }

    private ReplicateFalso replicate(Escenario e) throws IOException {
        var r = new ReplicateFalso(e);
        e.replicate = r;
        abiertos.add(r);
        return r;
    }

    /** El cliente de produccion (HttpURLConnection con limites): POST 3 s, sondeo 2 s, sondeo total 6 s, pausa 50 ms. */
    private static ReplicateTrainingPlanGenerator generador(ReplicateFalso r) {
        var p = new ReplicateProperties(true, "token-de-prueba", r.urlDelModelo(), "gpt-5-mini", "medium", 6, 1, 2, 3,
                2, 50);
        return new ReplicateTrainingPlanGenerator(p, null, null, new JsonSupport(), ReplicateHttp.post(p),
                ReplicateHttp.sondeo(p));
    }

    private String llamar(ReplicateTrainingPlanGenerator g) {
        return g.call(PROMPT, 1, USUARIO, (short) 1, 1);
    }

    private List<String> lineas(String prefijo) {
        return logs.list.stream().map(ILoggingEvent::getFormattedMessage).filter(m -> m.startsWith(prefijo)).toList();
    }

    private boolean hayLog(String fragmento) {
        return logs.list.stream().map(ILoggingEvent::getFormattedMessage).anyMatch(m -> m.contains(fragmento));
    }

    // =====================================================================================================
    // Diagnostico: el error de G08 reproducido sin gasto
    // =====================================================================================================

    @Test
    @DisplayName("G08 · cabeceras 2xx sin Content-Type y cuerpo cortado: el mismo error de Spring que en la evaluacion")
    void g08ReproducidoConElClienteDeProduccion() throws Exception {
        var r = replicate(new Escenario().post("g08"));
        var p = new ReplicateProperties(true, "t", r.urlDelModelo(), "gpt-5-mini", "medium", 6, 1, 2, 3, 2, 50);
        var cliente = ReplicateHttp.post(p).build();

        var e = assertThrows(RestClientException.class, () -> cliente.post().uri(r.urlDelModelo())
                .body(Map.of("input", Map.of("prompt", "x"))).retrieve().body(String.class));

        assertEquals("Error while extracting response for type [java.lang.String] and content type "
                + "[application/octet-stream]", e.getMessage(), "el mensaje exacto de la evaluacion H2-v4");
        assertFalse(e instanceof org.springframework.web.client.ResourceAccessException,
                "no es un fallo de E/S para el clasificador antiguo");
        assertFalse(e instanceof org.springframework.web.client.RestClientResponseException, "ni un codigo HTTP");
        assertInstanceOf(IOException.class, e.getCause(), "la causa es leer un cuerpo que se corto");
        assertEquals(1, r.posts());
    }

    @Test
    @DisplayName("G08 · el tipo application/octet-stream es el de omision de Spring: con Content-Type el error dice JSON")
    void elTipoOctetStreamSaleDeLaFaltaDeCabecera() throws Exception {
        try (var r = new ReplicateFalso((p, s, out) -> {
            out.write(("HTTP/1.1 201 Created\r\nContent-Type: application/json\r\nContent-Length: 5000\r\n\r\n"
                    + "{\"id\":").getBytes());
            out.flush();
            s.setSoLinger(true, 0);
            s.close();
        })) {
            var p = new ReplicateProperties(true, "t", r.urlDelModelo(), "gpt-5-mini", "medium", 6, 1, 2, 3, 2, 50);
            var cliente = ReplicateHttp.post(p).build();
            var e = assertThrows(RestClientException.class, () -> cliente.post().uri(r.urlDelModelo())
                    .body(Map.of("a", 1)).retrieve().body(String.class));
            assertEquals("Error while extracting response for type [java.lang.String] and content type "
                    + "[application/json]", e.getMessage(), "con la cabecera, Spring nombra el tipo real");
        }
    }

    // =====================================================================================================
    // T01-T14
    // =====================================================================================================

    @Test
    @DisplayName("T01 · respuesta JSON correcta: se sigue por su id y se usa la salida; 1 POST, sin buscar nada")
    void t01RespuestaCorrecta() throws Exception {
        var r = replicate(new Escenario().post("normal"));
        assertEquals("{\"ok\":1}", llamar(generador(r)));
        assertEquals(1, r.posts());
        assertEquals(0, r.cuantos("GET", "/v1/predictions") - r.cuantos("GET", "/v1/predictions/"),
                "no hace falta el listado");
        assertTrue(lineas("REPLICATE_RECUPERACION").isEmpty());
    }

    @Test
    @DisplayName("T02 · FALLABA ANTES (G08) · application/octet-stream con la prediccion creada: se recupera, 1 POST")
    void t02G08SeRecupera() throws Exception {
        var r = replicate(new Escenario().post("g08"));
        assertEquals("{\"ok\":1}", llamar(generador(r)), "la salida de la prediccion que ya existia");
        assertEquals(1, r.posts(), "no se crea una segunda prediccion");
        assertTrue(hayLog("content type [application/octet-stream]"), "el fallo de lectura queda registrado");
        assertTrue(lineas("REPLICATE_RECUPERACION").get(0).contains("resultado=RECUPERADA prediction_id=" + ID),
                lineas("REPLICATE_RECUPERACION").toString());
        var timing = lineas("REPLICATE_TIMING").get(0);
        assertTrue(timing.contains("prediction_id=" + ID) && timing.contains("resultado=SUCCEEDED"), timing);
    }

    @Test
    @DisplayName("T02 · inverso: si la unica candidata tiene OTRA entrada, no se adopta (nunca una ajena)")
    void t02OtraEntradaNoSeAdopta() throws Exception {
        var e = new Escenario().post("g08");
        e.entradaDistinta = true;
        var r = replicate(e);
        var ex = assertThrows(AiProviderUnavailableException.class, () -> llamar(generador(r)));
        assertTrue(ex.getMessage().contains("PREDICCION_NO_CONFIRMADA"), ex.getMessage());
        assertTrue(ex.getMessage().contains("octet-stream"), "conserva el motivo original: " + ex.getMessage());
        assertEquals(1, r.posts(), "ni se reenvia");
    }

    @Test
    @DisplayName("T02 · dos predicciones con la misma entrada: AMBIGUA, no se elige ninguna")
    void t02AmbiguaNoSeAdopta() throws Exception {
        var e = new Escenario().post("g08");
        e.duplicada = true;
        var r = replicate(e);
        assertThrows(AiProviderUnavailableException.class, () -> llamar(generador(r)));
        assertTrue(lineas("REPLICATE_RECUPERACION").get(0).contains("resultado=AMBIGUA"));
        assertEquals(1, r.posts());
    }

    @Test
    @DisplayName("T02 · cuerpo cortado con cierre normal (JSON incompleto): tambien se busca y se recupera")
    void t02TruncadaSeRecupera() throws Exception {
        var r = replicate(new Escenario().post("truncada"));
        assertEquals("{\"ok\":1}", llamar(generador(r)));
        assertEquals(1, r.posts());
    }

    @Test
    @DisplayName("T04 · cuerpo a medias que se queda colgado hasta el limite de lectura: 1 POST y se recupera")
    void t04AMediasSeRecupera() throws Exception {
        var r = replicate(new Escenario().post("a-medias"));
        assertEquals("{\"ok\":1}", llamar(generador(r)));
        assertEquals(1, r.posts(), "el limite de lectura no provoca un reenvio del POST");
    }

    @Test
    @DisplayName("T03 · 2xx sin identificador: no se inventa el id; se busca y, si no aparece, incierta sin reenviar")
    void t03SinIdentificador() throws Exception {
        var e = new Escenario().post("sin-id");
        e.enListado = false;
        var r = replicate(e);
        var ex = assertThrows(AiProviderUnavailableException.class, () -> llamar(generador(r)));
        assertTrue(ex.getMessage().contains("PREDICCION_NO_CONFIRMADA"), ex.getMessage());
        assertTrue(lineas("REPLICATE_TIMING").get(0).contains("prediction_id=desconocido"), "nunca inventado");
        assertEquals(1, r.posts());
        assertEquals(ReplicateTrainingPlanGenerator.RECUPERACION_RONDAS,
                r.cuantos("GET", "/v1/predictions") - r.cuantos("GET", "/v1/predictions/"));
    }

    @Test
    @DisplayName("T03 · 2xx sin identificador pero la prediccion aparece: se sigue esa")
    void t03SinIdentificadorRecuperada() throws Exception {
        var r = replicate(new Escenario().post("sin-id"));
        assertEquals("{\"ok\":1}", llamar(generador(r)));
        assertEquals(1, r.posts());
    }

    @Test
    @DisplayName("T04 · FALLABA ANTES · un GET de sondeo que falla una vez ya no cancela la prediccion: se reintenta")
    void t04SondeoFallaUnaVez() throws Exception {
        var e = new Escenario().post("normal");
        e.fallosDeConsulta = 2;
        var r = replicate(e);
        assertEquals("{\"ok\":1}", llamar(generador(r)));
        assertEquals(0, r.cuantos("POST", "/v1/predictions/"), "no se cancela nada");
        assertEquals(1, r.posts());
    }

    @Test
    @DisplayName("T04 · limite de lectura del POST con la prediccion ya creada: se recupera por el listado")
    void t04TimeoutDelPostConPrediccionCreada() throws Exception {
        var r = replicate(new Escenario().post("cuelga"));
        assertEquals("{\"ok\":1}", llamar(generador(r)));
        assertEquals(1, r.posts());
        assertTrue(lineas("REPLICATE_RECUPERACION").get(0).contains("RECUPERADA"));
    }

    @Test
    @DisplayName("T05 · conexion cerrada sin respuesta: el cliente NO reenvia el POST y queda incierta (sin duplicar)")
    void t05CerradaSinRespuesta() throws Exception {
        var r = replicate(new Escenario().post("cierra", "normal"));
        var ex = assertThrows(AiProviderUnavailableException.class, () -> llamar(generador(r)));
        assertTrue(ex.getMessage().contains("PREDICCION_NO_CONFIRMADA"), ex.getMessage());
        assertEquals(1, r.posts(), "HttpURLConnection reenvia un POST sin streaming; el de Spring no debe hacerlo");
    }

    @Test
    @DisplayName("T05 · incierta en el pipeline: un solo POST y el respaldo RULE_ENGINE, sin segundo intento de IA")
    void t05InciertaEnElPipeline() throws Exception {
        var e = new Escenario().post("cierra", "normal", "normal");
        var r = replicate(e);
        var ia = new SoloHttp(generador(r));
        var reglas = mock(TrainingPlanGenerator.class);
        when(reglas.generate(any(), anyList())).thenReturn(borrador(GenerationSource.RULE_ENGINE));
        var pipeline = new PlanGenerationPipeline(ia, reglas, mock(PlanDraftValidator.class), normalizador());

        var res = pipeline.run(contexto(), 30);

        assertEquals(GenerationSource.RULE_ENGINE, res.draft().source());
        assertEquals(1, r.posts(), "no se paga otra prediccion por un envio incierto");
        verify(reglas).generate(any(), argThat(l -> l.stream().anyMatch(x -> x.contains("PREDICCION_NO_CONFIRMADA"))));
    }

    @Test
    @DisplayName("T06 · 5xx con la prediccion creada: se recupera; 5xx sin prediccion: incierta; 4xx: rechazo definitivo")
    void t06Errores5xxY4xx() throws Exception {
        var creada = replicate(new Escenario().post("502-creada"));
        assertEquals("{\"ok\":1}", llamar(generador(creada)));
        assertEquals(1, creada.posts());

        var sinCrear = replicate(new Escenario().post("502"));
        var e1 = assertThrows(AiProviderUnavailableException.class, () -> llamar(generador(sinCrear)));
        assertTrue(e1.getMessage().contains("PREDICCION_NO_CONFIRMADA") && e1.getMessage().contains("http 502"),
                e1.getMessage());
        assertEquals(1, sinCrear.posts());

        var rechazada = replicate(new Escenario().post("422"));
        var e2 = assertThrows(AiProviderUnavailableException.class, () -> llamar(generador(rechazada)));
        assertFalse(e2.getMessage().contains("PREDICCION_NO_CONFIRMADA"), "un 4xx no es incierto: " + e2.getMessage());
        assertEquals(0, rechazada.cuantos("GET", "/v1/predictions"), "ni se busca: Replicate la rechazo");
    }

    @Test
    @DisplayName("T06 · sin conexion: no llego a enviarse, ni se busca ni se reintenta")
    void t06SinConexion() throws Exception {
        int puerto;
        try (var ss = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            puerto = ss.getLocalPort();
        }
        var p = new ReplicateProperties(true, "t", "http://127.0.0.1:" + puerto
                + "/v1/models/openai/gpt-5-mini/predictions", "gpt-5-mini", "medium", 6, 1, 2, 3, 2, 50);
        var g = new ReplicateTrainingPlanGenerator(p, null, null, new JsonSupport(), ReplicateHttp.post(p),
                ReplicateHttp.sondeo(p));
        var e = assertThrows(AiProviderUnavailableException.class, () -> llamar(g));
        assertFalse(e.getMessage().contains("PREDICCION_NO_CONFIRMADA"), e.getMessage());
        assertTrue(lineas("REPLICATE_RECUPERACION").isEmpty());
    }

    @Test
    @DisplayName("T07 · prediccion recuperada aun en curso: se sigue hasta el final, sin crear otra")
    void t07RecuperadaEnCurso() throws Exception {
        var e = new Escenario().post("g08");
        e.consultasEnCurso = 3;
        var r = replicate(e);
        assertEquals("{\"ok\":1}", llamar(generador(r)));
        assertEquals(1, r.posts());
        assertTrue(e.consultasDeLaNuestra.get() >= 4, "la de la recuperacion y las del sondeo");
    }

    @Test
    @DisplayName("T08 · prediccion recuperada ya completada: su salida se lee UNA vez (sin sondeo)")
    void t08RecuperadaCompletada() throws Exception {
        var e = new Escenario().post("g08");
        var r = replicate(e);
        assertEquals("{\"ok\":1}", llamar(generador(r)));
        assertEquals(1, e.consultasDeLaNuestra.get(), "un solo GET de la nuestra");
        assertEquals(1, r.posts());
    }

    @Test
    @DisplayName("T09 · prediccion recuperada cancelada: no es un exito")
    void t09Cancelada() throws Exception {
        var e = new Escenario().post("g08");
        e.estadoFinal = "canceled";
        var r = replicate(e);
        var ex = assertThrows(AiProviderUnavailableException.class, () -> llamar(generador(r)));
        assertTrue(ex.getMessage().contains("canceled"), ex.getMessage());
        assertEquals(1, r.posts());
    }

    @Test
    @DisplayName("T10 · prediccion recuperada fallida: el mismo fallo del proveedor de siempre")
    void t10Fallida() throws Exception {
        var e = new Escenario().post("g08");
        e.estadoFinal = "failed";
        var r = replicate(e);
        var ex = assertThrows(AiProviderUnavailableException.class, () -> llamar(generador(r)));
        assertTrue(ex.getMessage().contains("failed"), ex.getMessage());
        assertEquals(1, r.posts());
    }

    @Test
    @DisplayName("T11 · salida recuperada que no es un plan: la rechaza el contrato y se reintenta como un plan invalido")
    void t11SalidaInvalidaRecuperada() throws Exception {
        var e = new Escenario().post("g08", "normal");
        e.salidas.addAll(List.of("\"{\\\"mal\\\":1}\"", SALIDA_OK));
        var r = replicate(e);
        var ia = new SoloHttp(generador(r));
        var reglas = mock(TrainingPlanGenerator.class);
        var pipeline = new PlanGenerationPipeline(ia, reglas, mock(PlanDraftValidator.class), normalizador());

        var res = pipeline.run(contexto(), 30);

        assertEquals(GenerationSource.AI, res.draft().source());
        assertEquals(List.of("{\"mal\":1}", "{\"ok\":1}"), ia.salidasValidadas,
                "la salida recuperada pasa por el mismo contrato que una recibida normalmente");
        assertEquals(2, r.posts(), "el segundo POST es el reintento por PLAN INVALIDO, no un duplicado del transporte");
        verifyNoInteractions(reglas);
    }

    @Test
    @DisplayName("T12 · dos ejecuciones simultaneas de la misma operacion: un solo POST")
    void t12Concurrencia() throws Exception {
        var e = new Escenario().post("lento");
        var r = replicate(e);
        var g = generador(r);
        var libro = new LibroSincronizado(new DiarioDePrediccionesReplicateTest.LibroEnMemoria());
        g.setDiario(libro);
        var op = UUID.randomUUID();
        var salida = Executors.newFixedThreadPool(2);
        try {
            var listos = new CyclicBarrier(2);
            Callable<String> tarea = () -> {
                listos.await();
                try {
                    return DiarioDePredicciones.durante(op, USUARIO, () -> g.call(PROMPT, 1, USUARIO, (short) 1, 1));
                } catch (AiProviderUnavailableException ex) {
                    return "INCIERTA";
                }
            };
            var a = salida.submit(tarea);
            var b = salida.submit(tarea);
            var resultados = List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS));
            assertEquals(1, r.posts(), "una sola prediccion para la misma operacion e intento: " + resultados);
            assertTrue(resultados.contains("{\"ok\":1}"), resultados.toString());
        } finally {
            salida.shutdownNow();
        }
    }

    @Test
    @DisplayName("T13 · FALLABA ANTES · reanudacion tras morir el proceso con el envio sin confirmar: se recupera, 0 POST")
    void t13ReanudacionSolicitando() throws Exception {
        var entrada = entradaEnviada();
        var e = new Escenario();
        e.creada = true;
        e.entradaFija = entrada;
        var r = replicate(e);
        var g = generador(r);
        var libro = new DiarioDePrediccionesReplicateTest.LibroEnMemoria();
        g.setDiario(libro);
        var op = UUID.randomUUID();
        libro.anadir(op, 1, g.huellaDe(PROMPT, 1), DiarioDePredicciones.Estado.SOLICITANDO, null, null);

        var out = DiarioDePredicciones.durante(op, USUARIO, () -> g.call(PROMPT, 1, USUARIO, (short) 1, 1));

        assertEquals("{\"ok\":1}", out);
        assertEquals(0, r.posts(), "no se paga otra");
        var fila = libro.deIntento(op, 1).orElseThrow();
        assertEquals(DiarioDePredicciones.Estado.SUCCEEDED, fila.estado());
        assertEquals(ID, fila.predictionId());
    }

    @Test
    @DisplayName("T13 · con libro: el caso G08 queda ENVIADA->SUCCEEDED con su id, no NO_CONFIRMADA")
    void t13ConLibroG08() throws Exception {
        var r = replicate(new Escenario().post("g08"));
        var g = generador(r);
        var libro = new DiarioDePrediccionesReplicateTest.LibroEnMemoria();
        g.setDiario(libro);
        var op = UUID.randomUUID();
        assertEquals("{\"ok\":1}", DiarioDePredicciones.durante(op, USUARIO, () -> llamar(g)));
        var fila = libro.deIntento(op, 1).orElseThrow();
        assertEquals(DiarioDePredicciones.Estado.SUCCEEDED, fila.estado());
        assertEquals(ID, fila.predictionId());
        // Y una nueva ejecucion de la operacion reutiliza la salida sin llamar.
        assertEquals("{\"ok\":1}", DiarioDePredicciones.durante(op, USUARIO, () -> llamar(g)));
        assertEquals(1, r.posts());
    }

    @Test
    @DisplayName("T14 · adaptacion (correcciones de sesion): el mismo transporte recupera G08 con su propio esquema")
    void t14Adaptacion() throws Exception {
        var r = replicate(new Escenario().post("g08"));
        var g = generador(r);
        var out = g.call(PROMPT, ReplicateTrainingPlanGenerator.Peticion.deCorrecciones(1), USUARIO, (short) 1, 1);
        assertEquals("{\"ok\":1}", out);
        assertEquals(1, r.posts());
        assertEquals(ReplicateTrainingPlanGenerator.instruccionDeSistema(
                        ReplicateTrainingPlanGenerator.Peticion.deCorrecciones(1)),
                r.ultimaEntrada().get("system_prompt").asText(), "la peticion de adaptacion, con su propio esquema");
    }

    // =====================================================================================================
    // ayudantes
    // =====================================================================================================

    /** La entrada exacta que el generador envia para PROMPT (capturada de un POST a un Replicate falso). */
    private JsonNode entradaEnviada() throws Exception {
        try (var r = new ReplicateFalso((p, s, out) -> ReplicateFalso.json(out, 201,
                "{\"id\":\"x1\",\"status\":\"succeeded\",\"output\":" + SALIDA_OK + "}"))) {
            llamar(generador(r));
            return r.ultimaEntrada();
        }
    }

    /** El generador real con HTTP real, sin plantilla de prompt: la salida que contenga "ok" es un plan valido. */
    private static final class SoloHttp extends ReplicateTrainingPlanGenerator {
        final List<String> salidasValidadas = Collections.synchronizedList(new ArrayList<>());
        private final ReplicateTrainingPlanGenerator transporte;

        SoloHttp(ReplicateTrainingPlanGenerator transporte) {
            super(new ReplicateProperties(true, "t", "http://127.0.0.1:1/x", "gpt-5-mini", "medium", 6, 1),
                    null, null, new JsonSupport(), RestClient.builder());
            this.transporte = transporte;
        }

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public String requestOutput(PlanGenerationContext context, AiRetryFeedback feedback, int attempt) {
            return transporte.call(PROMPT, 1, context.userId(), context.weekNumber(), attempt);
        }

        @Override
        public PlanDraft toDraft(String output) {
            salidasValidadas.add(output);
            if (output.contains("\"ok\"")) return borrador(GenerationSource.AI);
            throw new InvalidPlanDraftException(List.of("la salida del modelo no cumple el contrato"));
        }
    }

    /** El libro en memoria con cada operacion bajo un cerrojo, como las transacciones de la base. */
    private record LibroSincronizado(DiarioDePredicciones d) implements DiarioDePredicciones {
        @Override public synchronized Optional<Entrada> deIntento(UUID o, int a) { return d.deIntento(o, a); }
        @Override public synchronized Optional<Entrada> reutilizable(long u, String h, UUID e, Instant desde) {
            return d.reutilizable(u, h, e, desde);
        }
        @Override public synchronized Entrada adoptar(Entrada o, UUID op, int a, Instant ahora) {
            return d.adoptar(o, op, a, ahora);
        }
        @Override public synchronized long solicitar(UUID o, long u, int a, String h, Instant ahora) {
            return d.solicitar(o, u, a, h, ahora);
        }
        @Override public synchronized void enviada(long f, String p, Instant ahora) { d.enviada(f, p, ahora); }
        @Override public synchronized void consultada(long f, int c, Instant ahora) { d.consultada(f, c, ahora); }
        @Override public synchronized void terminada(long f, Estado e, String o, String err, Instant ahora) {
            d.terminada(f, e, o, err, ahora);
        }
        @Override public synchronized Recuento recuento(UUID o) { return d.recuento(o); }
    }

    private static final LocalDate LUNES = LocalDate.of(2026, 10, 12);

    private static PlanDraftNormalizer normalizador() {
        var n = mock(PlanDraftNormalizer.class);
        when(n.normalize(any(), any())).thenAnswer(inv -> new PlanDraftNormalizer.Result(inv.getArgument(0), List.of()));
        return n;
    }

    private static PlanGenerationContext contexto() {
        return new PlanGenerationContext(USUARIO, (short) 1, LUNES, LUNES.plusDays(6),
                perfil(), PlanAdjustment.none(), null, List.of(ej(1L), ej(2L)),
                SafetyProfile.none(), (PrescriptionParams) null);
    }

    private static PlanDraft borrador(GenerationSource origen) {
        var ejercicios = new ArrayList<PlanDraft.DraftExercise>();
        ejercicios.add(new PlanDraft.DraftExercise(1L, PrescriptionType.SETS_REPS,
                (short) 2, (short) 10, null, null, (short) 75, null));
        return new PlanDraft(origen, origen == GenerationSource.AI ? "gpt-5-mini" : null,
                "plan de prueba", null, "prueba",
                List.of(new PlanDraft.DraftWorkout(LUNES, WorkoutFocus.FULL_BODY,
                        "FULL_BODY - Sesion 1", 45, ejercicios)));
    }

    private static CandidateExercise ej(long id) {
        return new CandidateExercise(id, "ejercicio " + id, "chest", "body weight", 1,
                PrescriptionType.SETS_REPS, null, "HORIZONTAL_PUSH");
    }

    private static PlanningProfile perfil() {
        return new PlanningProfile(USUARIO, 30, "MALE", new BigDecimal("175.0"),
                new BigDecimal("72.0"), new BigDecimal("75.0"), "BEGINNER",
                "GAIN_MUSCLE", "Ganar masa muscular entrenando en casa", "Ninguna",
                "HOME", 2, List.of((short) 1, (short) 3), 45,
                List.of("body weight", "dumbbell", "band"), List.of(), SafetyProfile.Declaradas.sinLimitaciones());
    }
}
