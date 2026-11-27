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
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RT · TRANSPORTE DE REPLICATE: una prediccion que existe no se da por fallida ni se paga dos veces, y NUNCA se le
 * atribuye a una peticion el resultado de otra (27/11/2026).
 * <p>
 * Origen: en la evaluacion H2-v4 (G08, replica 2, brazo B) el POST recibio cabeceras 2xx sin {@code Content-Type} y un
 * cuerpo cortado (RST). Spring lanzo {@code "Error while extracting response for type [java.lang.String] and content
 * type [application/octet-stream]"}, el backend lo trato como fallo del proveedor y la prediccion
 * {@code g40bmfgq8nrzr0d236ktnedbfm} termino (y se facturo) sin que nadie la leyera.
 * <p>
 * IDENTIDAD (revision de seguridad): la API de Replicate (OpenAPI 1.0.0-a1) no documenta cabecera con el id, ni clave
 * de idempotencia, ni metadatos de prediccion. La unica prueba de pertenencia es el {@code id} de la respuesta de ESE
 * POST. Por eso solo se recupera por ese id (aunque llegue al principio de un cuerpo cortado), confirmado por GET; nunca
 * por contenido.
 * <p>
 * Escenarios T01-T14 (orden de transporte) y S01-S15 (orden de validacion final). Todo contra {@link ReplicateFalso}
 * (un socket local con el cliente HTTP de produccion) o con libros en memoria: <b>ninguna llamada a Replicate</b>.
 */
@DisplayName("RT · transporte de Replicate: recuperar solo la prediccion propia, sin duplicar ni atribuir ajenas")
class ReplicateRecuperacionTransporteTest {

    private static final String ID = "g40bmfgq8nrzr0d236ktnedbfm";
    private static final String PROMPT = "prompt de prueba";
    private static final long USUARIO = 7L;
    private static final String SALIDA_OK = "\"{\\\"ok\\\":1}\"";
    private static final ObjectMapper JSON = new ObjectMapper();

    // ------------------------------------------------------------------ el guion del Replicate falso

    /**
     * Que hace "Replicate". Cada POST crea (o no) una prediccion con el id que toque y guarda SU entrada; los GET por id
     * devuelven la entrada de esa prediccion; el listado devuelve todas las creadas (mas una ajena). Asi se pueden
     * simular dos usuarios con peticiones identicas contra el mismo servidor.
     */
    static final class Escenario implements ReplicateFalso.Guion {
        final Deque<String> modosPost = new ArrayDeque<>();
        final Deque<String> idsPost = new ArrayDeque<>();
        final Deque<String> salidas = new ArrayDeque<>();
        final Map<String, JsonNode> creadas = Collections.synchronizedMap(new LinkedHashMap<>());
        final Map<String, AtomicInteger> consultas = new ConcurrentHashMap<>();
        volatile boolean entradaDistinta;
        volatile boolean datosBorrados;
        volatile Instant creadaEn;
        volatile String estadoFinal = "succeeded";
        volatile int consultasEnCurso;
        volatile int fallosDeConsulta;
        volatile String salidaActual = SALIDA_OK;
        ReplicateFalso replicate;

        Escenario post(String... modos) {
            modosPost.addAll(List.of(modos));
            return this;
        }

        Escenario ids(String... ids) {
            idsPost.addAll(List.of(ids));
            return this;
        }

        int consultasDe(String id) {
            var n = consultas.get(id);
            return n == null ? 0 : n.get();
        }

        private static String principio(String id) {
            return "{\"id\":\"" + id + "\",\"model\":\"openai/gpt-5-mini\",\"version\":\"hidden\",\"input\":{\"prompt\":\"";
        }

        @Override
        public void responder(ReplicateFalso.Peticion p, java.net.Socket s, java.io.OutputStream out) throws Exception {
            var base = replicate.base();
            if (p.metodo().equals("POST") && p.ruta().startsWith("/v1/models/")) {
                var modo = modosPost.isEmpty() ? "normal" : modosPost.poll();
                var id = idsPost.isEmpty() ? ID : idsPost.poll();
                if (!salidas.isEmpty()) salidaActual = salidas.poll();
                var entrada = JSON.readTree(p.cuerpo()).get("input");
                Runnable crear = () -> creadas.put(id, entrada);
                switch (modo) {
                    case "normal" -> {
                        crear.run();
                        ReplicateFalso.json(out, 201, ReplicateFalso.prediccion(id, "starting", null, entrada, base));
                    }
                    case "202" -> {
                        crear.run();
                        ReplicateFalso.json(out, 202, ReplicateFalso.prediccion(id, "processing", null, entrada, base));
                    }
                    case "location-ajena" -> {
                        crear.run();
                        ReplicateFalso.json(out, 201, ReplicateFalso.prediccion(id, "starting", null, entrada, base),
                                "Location: " + base + "/v1/predictions/ajenaloc1\r\n");
                    }
                    case "g08" -> {
                        crear.run();
                        ReplicateFalso.cortadaSinTipo(s, out, principio(id));
                    }
                    case "g08-lento" -> {
                        crear.run();
                        Thread.sleep(300);
                        ReplicateFalso.cortadaSinTipo(s, out, principio(id));
                    }
                    case "g08-id-cortado" -> {
                        crear.run();
                        ReplicateFalso.cortadaSinTipo(s, out, "{\"id\":\"" + id.substring(0, 9));
                    }
                    case "g08-id-anidado" -> {
                        crear.run();
                        ReplicateFalso.cortadaSinTipo(s, out, "{\"status\":\"starting\",\"input\":{\"id\":\"" + id
                                + "\",\"prompt\":\"");
                    }
                    case "truncada" -> {
                        crear.run();
                        ReplicateFalso.truncada(out, principio(id));
                    }
                    case "a-medias" -> {
                        crear.run();
                        ReplicateFalso.colgadaAMedias(out, principio(id));
                    }
                    case "cuelga" -> {
                        crear.run();
                        Thread.sleep(10_000);
                    }
                    case "cierra" -> { /* cierra sin responder y SIN crear */ }
                    case "cierra-creada" -> crear.run(); // la crea y cierra sin responder
                    case "502" -> ReplicateFalso.json(out, 502, "{\"detail\":\"bad gateway\"}");
                    case "502-creada" -> {
                        crear.run();
                        ReplicateFalso.json(out, 502, "{\"detail\":\"bad gateway\"}");
                    }
                    case "422" -> ReplicateFalso.json(out, 422, "{\"detail\":\"input invalido\"}");
                    case "sin-id" -> {
                        crear.run();
                        ReplicateFalso.json(out, 201, "{\"status\":\"starting\"}");
                    }
                    default -> throw new IllegalStateException(modo);
                }
                return;
            }
            if (p.metodo().equals("GET") && (p.ruta().equals("/v1/predictions") || p.ruta().startsWith("/v1/predictions?"))) {
                var ids = new ArrayList<>(creadas.keySet());
                ids.add("otra1");
                ReplicateFalso.json(out, 200, ReplicateFalso.listado(ids.toArray(String[]::new)));
                return;
            }
            if (p.metodo().equals("POST") && p.ruta().endsWith("/cancel")) {
                var id = p.ruta().split("/")[3];
                ReplicateFalso.json(out, 200, ReplicateFalso.prediccion(id, "canceled", null, creadas.get(id), base));
                return;
            }
            if (p.metodo().equals("GET") && p.ruta().startsWith("/v1/predictions/")) {
                var id = p.ruta().substring("/v1/predictions/".length());
                if (id.equals("otra1")) { // la de otro usuario, con otra entrada, como en el Replicate real
                    var otra = JSON.createObjectNode().put("prompt", "la peticion de otro usuario");
                    ReplicateFalso.json(out, 200, ReplicateFalso.prediccion("otra1", "succeeded", SALIDA_OK, otra, base));
                    return;
                }
                var suya = creadas.get(id);
                if (suya == null) {
                    ReplicateFalso.json(out, 404, "{\"detail\":\"Not found.\"}");
                    return;
                }
                int n = consultas.computeIfAbsent(id, k -> new AtomicInteger()).incrementAndGet();
                if (n <= fallosDeConsulta) return; // corta la conexion sin responder
                var entrada = entradaDistinta ? JSON.createObjectNode().put("prompt", "otra entrada") : suya;
                var estado = n <= fallosDeConsulta + consultasEnCurso ? "processing" : estadoFinal;
                ReplicateFalso.json(out, 200, ReplicateFalso.prediccion(id, estado,
                        "succeeded".equals(estado) ? salidaActual : null, entrada, base,
                        creadaEn == null ? Instant.now() : creadaEn, datosBorrados));
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

    private static long listados(ReplicateFalso r) {
        return r.cuantos("GET", "/v1/predictions") - r.cuantos("GET", "/v1/predictions/");
    }

    private List<String> lineas(String prefijo) {
        return logs.list.stream().map(ILoggingEvent::getFormattedMessage).filter(m -> m.startsWith(prefijo)).toList();
    }

    private boolean hayLog(String fragmento) {
        return logs.list.stream().map(ILoggingEvent::getFormattedMessage).anyMatch(m -> m.contains(fragmento));
    }

    private static void noConfirmada(Executable e) {
        var ex = assertThrows(AiProviderUnavailableException.class, e::run);
        assertTrue(ex.getMessage().contains("PREDICCION_NO_CONFIRMADA"), ex.getMessage());
        assertFalse(ex.getMessage().toLowerCase(Locale.ROOT).contains("no existe"),
                "la incertidumbre nunca se presenta como prueba de que no existe: " + ex.getMessage());
    }

    @FunctionalInterface
    private interface Executable {
        void run() throws Throwable;
    }

    // =====================================================================================================
    // Diagnostico: el error de G08 reproducido sin gasto (Spring, sin la correccion)
    // =====================================================================================================

    @Test
    @DisplayName("G08 · cabeceras 2xx sin Content-Type y cuerpo cortado (RST): el mismo error de Spring que en H2-v4")
    void g08ReproducidoConElClienteDeProduccion() throws Exception {
        var r = replicate(new Escenario().post("g08"));
        var p = new ReplicateProperties(true, "t", r.urlDelModelo(), "gpt-5-mini", "medium", 6, 1, 2, 3, 2, 50);
        var cliente = ReplicateHttp.post(p).build();

        var e = assertThrows(RestClientException.class, () -> cliente.post().uri(r.urlDelModelo())
                .body(Map.of("input", Map.of("prompt", "x"))).retrieve().body(String.class));

        assertEquals("Error while extracting response for type [java.lang.String] and content type "
                + "[application/octet-stream]", e.getMessage(), "el mensaje exacto de la evaluacion H2-v4");
        assertFalse(e instanceof org.springframework.web.client.ResourceAccessException);
        assertFalse(e instanceof org.springframework.web.client.RestClientResponseException);
        assertInstanceOf(IOException.class, e.getCause(), "la causa es leer un cuerpo que se corto");
        assertEquals(1, r.posts());
    }

    @Test
    @DisplayName("G08 · application/octet-stream es el tipo de omision de Spring: con Content-Type el error dice JSON")
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
                    + "[application/json]", e.getMessage());
        }
    }

    @Test
    @DisplayName("id de primer nivel de un sobre cortado: solo completo; nunca uno anidado ni uno a medias")
    void idDeSuRespuesta() {
        assertEquals(ID, ReplicateTrainingPlanGenerator.idDeSuRespuesta(Escenario.principio(ID)));
        assertEquals(ID, ReplicateTrainingPlanGenerator.idDeSuRespuesta(
                "{\"model\":\"m\",\"input\":{\"id\":\"anidado1\"},\"id\":\"" + ID + "\",\"x\":"));
        assertNull(ReplicateTrainingPlanGenerator.idDeSuRespuesta("{\"id\":\"g40bmfg"), "cortado dentro del id");
        assertNull(ReplicateTrainingPlanGenerator.idDeSuRespuesta("{\"input\":{\"id\":\"" + ID + "\"},\"st"),
                "solo anidado");
        assertNull(ReplicateTrainingPlanGenerator.idDeSuRespuesta("{\"id\":\"../../otra\"}"), "no alfanumerico");
        assertNull(ReplicateTrainingPlanGenerator.idDeSuRespuesta("<html>"));
        assertNull(ReplicateTrainingPlanGenerator.idDeSuRespuesta(""));
        assertNull(ReplicateTrainingPlanGenerator.idDeSuRespuesta("{\"id\":123}"));
    }

    // =====================================================================================================
    // T01-T14 (orden de transporte)
    // =====================================================================================================

    @Test
    @DisplayName("T01 · respuesta JSON correcta: se sigue por su id; 1 POST, sin listado")
    void t01RespuestaCorrecta() throws Exception {
        var r = replicate(new Escenario().post("normal"));
        assertEquals("{\"ok\":1}", llamar(generador(r)));
        assertEquals(1, r.posts());
        assertEquals(0, listados(r));
        assertTrue(lineas("REPLICATE_RECUPERACION").isEmpty());
    }

    @Test
    @DisplayName("T02 · FALLABA ANTES (G08) · octet-stream con el id ya recibido: se confirma por su id y se usa; 1 POST")
    void t02G08SeRecuperaPorSuId() throws Exception {
        var r = replicate(new Escenario().post("g08"));
        assertEquals("{\"ok\":1}", llamar(generador(r)), "la salida de la prediccion que ya existia");
        assertEquals(1, r.posts(), "no se crea una segunda prediccion");
        assertEquals(0, listados(r), "nunca se busca por contenido");
        assertTrue(hayLog("Content-Type ausente"), "el corte queda registrado con su causa");
        assertTrue(lineas("REPLICATE_RECUPERACION").get(0).contains("resultado=RECUPERADA_POR_ID prediction_id=" + ID));
        var timing = lineas("REPLICATE_TIMING").get(0);
        assertTrue(timing.contains("prediction_id=" + ID) && timing.contains("resultado=SUCCEEDED"), timing);
    }

    @Test
    @DisplayName("T02 · cuerpo cortado con cierre normal (JSON incompleto) que trae el id: tambien se confirma y se usa")
    void t02TruncadaSeRecupera() throws Exception {
        var r = replicate(new Escenario().post("truncada"));
        assertEquals("{\"ok\":1}", llamar(generador(r)));
        assertEquals(1, r.posts());
    }

    @Test
    @DisplayName("T03 · 2xx sin identificador: no se inventa; incierta sin reenviar y sin buscar")
    void t03SinIdentificador() throws Exception {
        var r = replicate(new Escenario().post("sin-id"));
        noConfirmada(() -> llamar(generador(r)));
        assertTrue(lineas("REPLICATE_TIMING").get(0).contains("prediction_id=desconocido"), "nunca inventado");
        assertEquals(1, r.posts());
        assertEquals(0, listados(r));
    }

    @Test
    @DisplayName("T04 · cuerpo a medias con el id hasta el limite de lectura: se confirma y se usa; 1 POST")
    void t04AMediasConId() throws Exception {
        var r = replicate(new Escenario().post("a-medias"));
        assertEquals("{\"ok\":1}", llamar(generador(r)));
        assertEquals(1, r.posts(), "el limite de lectura no provoca un reenvio del POST");
    }

    @Test
    @DisplayName("T04 · limite de lectura del POST SIN recibir nada: incierta, aunque exista; ni se busca ni se reenvia")
    void t04TimeoutSinNada() throws Exception {
        var r = replicate(new Escenario().post("cuelga"));
        noConfirmada(() -> llamar(generador(r)));
        assertEquals(1, r.posts());
        assertEquals(0, listados(r));
    }

    @Test
    @DisplayName("T05 · conexion cerrada sin respuesta: el cliente NO reenvia el POST; incierta")
    void t05CerradaSinRespuesta() throws Exception {
        var r = replicate(new Escenario().post("cierra", "normal"));
        noConfirmada(() -> llamar(generador(r)));
        assertEquals(1, r.posts(), "HttpURLConnection reenvia un POST sin streaming; el de Spring no debe hacerlo");
    }

    @Test
    @DisplayName("T06 · 5xx: incierta (sin buscar); 4xx: rechazo definitivo; sin conexion: no llego a enviarse")
    void t06Errores() throws Exception {
        var creada = replicate(new Escenario().post("502-creada"));
        noConfirmada(() -> llamar(generador(creada)));
        assertEquals(1, creada.posts());
        assertEquals(0, listados(creada));

        var rechazada = replicate(new Escenario().post("422"));
        var e = assertThrows(AiProviderUnavailableException.class, () -> llamar(generador(rechazada)));
        assertFalse(e.getMessage().contains("PREDICCION_NO_CONFIRMADA"), "un 4xx no es incierto: " + e.getMessage());

        int puerto;
        try (var ss = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            puerto = ss.getLocalPort();
        }
        var p = new ReplicateProperties(true, "t", "http://127.0.0.1:" + puerto
                + "/v1/models/openai/gpt-5-mini/predictions", "gpt-5-mini", "medium", 6, 1, 2, 3, 2, 50);
        var g = new ReplicateTrainingPlanGenerator(p, null, null, new JsonSupport(), ReplicateHttp.post(p),
                ReplicateHttp.sondeo(p));
        var e2 = assertThrows(AiProviderUnavailableException.class, () -> llamar(g));
        assertFalse(e2.getMessage().contains("PREDICCION_NO_CONFIRMADA"), e2.getMessage());
    }

    @Test
    @DisplayName("T07 · confirmada por su id y aun en curso: se sigue hasta el final, sin crear otra")
    void t07EnCurso() throws Exception {
        var e = new Escenario().post("g08");
        e.consultasEnCurso = 3;
        var r = replicate(e);
        assertEquals("{\"ok\":1}", llamar(generador(r)));
        assertEquals(1, r.posts());
        assertTrue(e.consultasDe(ID) >= 4, "la de la confirmacion y las del sondeo");
    }

    @Test
    @DisplayName("T08 · confirmada por su id y ya completada: su salida se lee UNA vez")
    void t08Completada() throws Exception {
        var e = new Escenario().post("g08");
        var r = replicate(e);
        assertEquals("{\"ok\":1}", llamar(generador(r)));
        assertEquals(1, e.consultasDe(ID));
    }

    @Test
    @DisplayName("T09 · confirmada y cancelada: no es un exito")
    void t09Cancelada() throws Exception {
        var e = new Escenario().post("g08");
        e.estadoFinal = "canceled";
        var r = replicate(e);
        var ex = assertThrows(AiProviderUnavailableException.class, () -> llamar(generador(r)));
        assertTrue(ex.getMessage().contains("canceled"), ex.getMessage());
    }

    @Test
    @DisplayName("T10 · confirmada y fallida: el mismo fallo del proveedor de siempre")
    void t10Fallida() throws Exception {
        var e = new Escenario().post("g08");
        e.estadoFinal = "failed";
        var r = replicate(e);
        var ex = assertThrows(AiProviderUnavailableException.class, () -> llamar(generador(r)));
        assertTrue(ex.getMessage().contains("failed"), ex.getMessage());
    }

    @Test
    @DisplayName("T11 · salida recuperada que no es un plan: la rechaza el contrato y se reintenta como plan invalido")
    void t11SalidaInvalidaRecuperada() throws Exception {
        var e = new Escenario().post("g08", "normal").ids(ID, "segunda1");
        e.salidas.addAll(List.of("\"{\\\"mal\\\":1}\"", SALIDA_OK));
        var r = replicate(e);
        var ia = new SoloHttp(generador(r));
        var reglas = mock(TrainingPlanGenerator.class);
        var pipeline = new PlanGenerationPipeline(ia, reglas, mock(PlanDraftValidator.class), normalizador());

        var res = pipeline.run(contexto(), 30);

        assertEquals(GenerationSource.AI, res.draft().source());
        assertEquals(List.of("{\"mal\":1}", "{\"ok\":1}"), ia.salidasValidadas);
        assertEquals(2, r.posts(), "el segundo POST es el reintento por PLAN INVALIDO, no un duplicado del transporte");
        verifyNoInteractions(reglas);
    }

    @Test
    @DisplayName("T12 · dos ejecuciones simultaneas de la misma operacion: un solo POST")
    void t12Concurrencia() throws Exception {
        var r = replicate(new Escenario().post("normal"));
        concurrentes(r);
    }

    @Test
    @DisplayName("T13 · con libro, G08: ENVIADA->SUCCEEDED con su id; reanudar reutiliza sin llamar")
    void t13ConLibroG08() throws Exception {
        var r = replicate(new Escenario().post("g08"));
        var g = generador(r);
        var libro = new DiarioDePrediccionesReplicateTest.LibroEnMemoria();
        g.setDiario(libro);
        var op = UUID.randomUUID();
        assertEquals("{\"ok\":1}", DiarioDePredicciones.durante(op, USUARIO, () -> llamar(g)));
        var fila = libro.deIntento(op, 1).orElseThrow();
        assertEquals(DiarioDePredicciones.Estado.SUCCEEDED, fila.estado());
        assertEquals(ID, fila.predictionId(), "el id de su respuesta queda en el libro");
        assertEquals("{\"ok\":1}", DiarioDePredicciones.durante(op, USUARIO, () -> llamar(g)));
        assertEquals(1, r.posts());
    }

    @Test
    @DisplayName("T14 · adaptacion (correcciones de sesion): el mismo transporte confirma G08 por su id")
    void t14Adaptacion() throws Exception {
        var r = replicate(new Escenario().post("g08"));
        var out = generador(r).call(PROMPT, ReplicateTrainingPlanGenerator.Peticion.deCorrecciones(1), USUARIO,
                (short) 1, 1);
        assertEquals("{\"ok\":1}", out);
        assertEquals(ReplicateTrainingPlanGenerator.instruccionDeSistema(
                        ReplicateTrainingPlanGenerator.Peticion.deCorrecciones(1)),
                r.ultimaEntrada().get("system_prompt").asText(), "la peticion de adaptacion, con su propio esquema");
    }

    // =====================================================================================================
    // S01-S15 (orden de validacion final)
    // =====================================================================================================

    @Test
    @DisplayName("S01 · FALLABA EN #88 · dos usuarios con entradas identicas: A no se apropia de la prediccion de B")
    void s01DosUsuariosIdenticos() throws Exception {
        var e = new Escenario().post("normal", "cierra").ids("prediccionb1", "prediccionA1");
        var r = replicate(e);
        // Usuario B: su prediccion se crea y se lee bien.
        assertEquals("{\"ok\":1}", generador(r).call(PROMPT, 1, 8L, (short) 1, 1));
        int deBAntes = e.consultasDe("prediccionb1");
        // Usuario A: misma entrada; su POST se corta antes de crear nada.
        noConfirmada(() -> generador(r).call(PROMPT, 1, USUARIO, (short) 1, 1));
        assertEquals(deBAntes, e.consultasDe("prediccionb1"), "A ni siquiera consulta la prediccion de B");
        assertEquals(0, listados(r));
        assertEquals(2, r.posts(), "uno de cada usuario; A no reenvia");
    }

    @Test
    @DisplayName("S01 · dos usuarios: si la de A tambien existe pero sin id, tampoco se elige (indistinguibles)")
    void s01DosUsuariosAmbasExisten() throws Exception {
        var e = new Escenario().post("normal", "cierra-creada").ids("prediccionb1", "prediccionA1");
        var r = replicate(e);
        generador(r).call(PROMPT, 1, 8L, (short) 1, 1);
        noConfirmada(() -> generador(r).call(PROMPT, 1, USUARIO, (short) 1, 1));
        assertEquals(0, e.consultasDe("prediccionA1"), "sin su id, la de A no se puede distinguir de la de B");
    }

    @Test
    @DisplayName("S02 · una unica coincidencia por contenido NO prueba identidad: no se adopta")
    void s02UnaCoincidenciaSinIdentidad() throws Exception {
        var e = new Escenario().post("cuelga");
        var r = replicate(e);
        noConfirmada(() -> llamar(generador(r)));
        assertEquals(1, e.creadas.size(), "existe exactamente una con la misma entrada");
        assertEquals(0, e.consultasDe(ID), "y aun asi no se adopta");
    }

    @Test
    @DisplayName("S03 · id en la respuesta (201 y 202): se guarda en el libro y se sigue por ese id")
    void s03IdEnLaRespuesta() throws Exception {
        for (var modo : List.of("normal", "202")) {
            var e = new Escenario().post(modo);
            var r = replicate(e);
            var g = generador(r);
            var libro = new DiarioDePrediccionesReplicateTest.LibroEnMemoria();
            g.setDiario(libro);
            var op = UUID.randomUUID();
            assertEquals("{\"ok\":1}", DiarioDePredicciones.durante(op, USUARIO, () -> llamar(g)), modo);
            assertTrue(libro.eventos.contains("enviada " + ID), modo + ": " + libro.eventos);
            assertEquals(ID, libro.deIntento(op, 1).orElseThrow().predictionId());
        }
    }

    @Test
    @DisplayName("S03 · Location no documentado: se ignora; manda el id del cuerpo")
    void s03LocationNoDocumentadoSeIgnora() throws Exception {
        var e = new Escenario().post("location-ajena");
        var r = replicate(e);
        assertEquals("{\"ok\":1}", llamar(generador(r)));
        assertEquals(0, r.cuantos("GET", "/v1/predictions/ajenaloc1"), "no se sigue una URL de cabecera");
        assertTrue(e.consultasDe(ID) >= 1);
    }

    @Test
    @DisplayName("S04 · parcial con id: solo el id completo y de primer nivel; su GET debe confirmar la misma entrada")
    void s04ParcialConId() throws Exception {
        var cortado = replicate(new Escenario().post("g08-id-cortado"));
        noConfirmada(() -> llamar(generador(cortado)));
        assertEquals(0, cortado.cuantos("GET", "/v1/predictions/"), "un id a medias no se completa ni se busca");

        var anidado = replicate(new Escenario().post("g08-id-anidado"));
        noConfirmada(() -> llamar(generador(anidado)));
        assertEquals(0, anidado.cuantos("GET", "/v1/predictions/"), "un id anidado no es el de la prediccion");

        var e = new Escenario().post("g08");
        e.entradaDistinta = true;
        var distinta = replicate(e);
        noConfirmada(() -> llamar(generador(distinta)));
        assertTrue(hayLog("resultado=IDENTIDAD_DISTINTA prediction_id=" + ID));
    }

    @Test
    @DisplayName("S05 · varias predicciones con la misma entrada: no se elige ninguna")
    void s05VariasCandidatas() throws Exception {
        var e = new Escenario().post("normal", "normal", "cierra-creada").ids("igual1", "igual2", "igual3");
        var r = replicate(e);
        generador(r).call(PROMPT, 1, 8L, (short) 1, 1);
        generador(r).call(PROMPT, 1, 9L, (short) 1, 1);
        noConfirmada(() -> llamar(generador(r)));
        assertEquals(0, e.consultasDe("igual3"));
        assertEquals(0, listados(r));
    }

    @Test
    @DisplayName("S06 · mas candidatas que cualquier limite: la falta de id se informa como NO confirmada, no inexistente")
    void s06BusquedaIncompletaNoEsInexistencia() throws Exception {
        var e = new Escenario().post("cierra-creada");
        for (int i = 0; i < 30; i++) e.creadas.put("ruido" + i, JSON.createObjectNode().put("prompt", "x" + i));
        var r = replicate(e);
        var g = generador(r);
        var libro = new DiarioDePrediccionesReplicateTest.LibroEnMemoria();
        g.setDiario(libro);
        var op = UUID.randomUUID();
        noConfirmada(() -> DiarioDePredicciones.durante(op, USUARIO, () -> llamar(g)));
        assertEquals(DiarioDePredicciones.Estado.NO_CONFIRMADA, libro.deIntento(op, 1).orElseThrow().estado(),
                "NO_CONFIRMADA (pudo crearse), nunca ERROR_ENVIO (no existe)");
    }

    @Test
    @DisplayName("S07 · desfase de reloj de horas: la identidad por id no depende de ventanas de tiempo")
    void s07DesfaseDeReloj() throws Exception {
        var e = new Escenario().post("g08");
        e.creadaEn = Instant.now().minus(Duration.ofHours(5));
        var r = replicate(e);
        var g = generador(r).conTiempoDePrueba(Clock.fixed(Instant.now().plus(Duration.ofHours(5)), ZoneOffset.UTC),
                d -> Thread.sleep(d.toMillis()));
        // Con el reloj fijo, el sondeo no avanzaria: la prediccion ya esta terminada y basta su confirmacion.
        assertEquals("{\"ok\":1}", llamar(g));
    }

    @Test
    @DisplayName("S08 · datos de la prediccion ya borrados (data_removed): no se puede verificar; incierta")
    void s08DatosBorrados() throws Exception {
        var e = new Escenario().post("g08");
        e.datosBorrados = true;
        var r = replicate(e);
        noConfirmada(() -> llamar(generador(r)));
        assertTrue(hayLog("resultado=NO_VERIFICABLE_DATOS_BORRADOS prediction_id=" + ID));
        assertEquals(1, r.posts());
    }

    @Test
    @DisplayName("S09 · POST recibido y conexion cortada sin respuesta: ningun segundo POST")
    void s09PostSinRespuesta() throws Exception {
        var e = new Escenario().post("cierra-creada", "normal");
        var r = replicate(e);
        noConfirmada(() -> llamar(generador(r)));
        assertEquals(1, r.posts());
        assertEquals(1, e.creadas.size());
    }

    @Test
    @DisplayName("S10 · GET intermitente: 2 fallos se toleran sin cancelar; 3 seguidos terminan y se cancela una vez")
    void s10GetIntermitente() throws Exception {
        var e = new Escenario().post("normal");
        e.fallosDeConsulta = 2;
        var r = replicate(e);
        assertEquals("{\"ok\":1}", llamar(generador(r)));
        assertEquals(0, r.cuantos("POST", "/v1/predictions/"), "no se cancela nada");

        var e3 = new Escenario().post("normal");
        e3.fallosDeConsulta = 100; // el JDK reintenta un GET una vez si la conexion se cierra sin respuesta
        var r3 = replicate(e3);
        var ex = assertThrows(AiProviderUnavailableException.class, () -> llamar(generador(r3)));
        assertNotNull(ex);
        assertEquals(1, r3.cuantos("POST", "/v1/predictions/"), "una sola cancelacion al rendirse");
        assertTrue(lineas("REPLICATE_TIMING").get(1).contains("resultado=POLL_ERROR"), lineas("REPLICATE_TIMING").toString());
        assertEquals(1, r3.posts());
    }

    @Test
    @DisplayName("S11 · dos solicitudes simultaneas de la misma operacion con la respuesta cortada: un solo POST")
    void s11ConcurrentesConCorte() throws Exception {
        var r = replicate(new Escenario().post("g08-lento"));
        concurrentes(r);
    }

    @Test
    @DisplayName("S12 · adaptacion con dos usuarios identicos: tampoco reutiliza la prediccion ajena")
    void s12AdaptacionNoReutilizaAjena() throws Exception {
        var e = new Escenario().post("normal", "cierra").ids("adaptb1", "adapta1");
        var r = replicate(e);
        var peticion = ReplicateTrainingPlanGenerator.Peticion.deCorrecciones(1);
        assertEquals("{\"ok\":1}", generador(r).call(PROMPT, peticion, 8L, (short) 1, 1));
        int antes = e.consultasDe("adaptb1");
        noConfirmada(() -> generador(r).call(PROMPT, peticion, USUARIO, (short) 1, 1));
        assertEquals(antes, e.consultasDe("adaptb1"));
    }

    @Test
    @DisplayName("S13 · reanudacion V59: ENVIADA se sigue por su id, SUCCEEDED se reutiliza, SOLICITANDO queda incierta")
    void s13ReanudacionV59() throws Exception {
        var e = new Escenario();
        var r = replicate(e);
        var g = generador(r);
        var libro = new DiarioDePrediccionesReplicateTest.LibroEnMemoria();
        g.setDiario(libro);
        var huella = g.huellaDe(PROMPT, 1);

        // ENVIADA: el proceso murio con la prediccion en curso y su id en el libro.
        e.creadas.put(ID, JSON.createObjectNode().put("prompt", "la que sea"));
        var enviada = UUID.randomUUID();
        libro.anadir(enviada, 1, huella, DiarioDePredicciones.Estado.ENVIADA, ID, null);
        assertEquals("{\"ok\":1}", DiarioDePredicciones.durante(enviada, USUARIO, () -> llamar(g)));

        // SUCCEEDED: se reutiliza sin llamar.
        var hecha = UUID.randomUUID();
        libro.anadir(hecha, 1, huella, DiarioDePredicciones.Estado.SUCCEEDED, "otra2", "{\"ok\":2}");
        int peticiones = r.recibidas.size();
        assertEquals("{\"ok\":2}", DiarioDePredicciones.durante(hecha, USUARIO, () -> llamar(g)));
        assertEquals(peticiones, r.recibidas.size());

        // SOLICITANDO: sin id de ese envio; aunque haya predicciones con la misma entrada, no se adopta ninguna.
        var sinConfirmar = UUID.randomUUID();
        libro.anadir(sinConfirmar, 1, huella, DiarioDePredicciones.Estado.SOLICITANDO, null, null);
        noConfirmada(() -> DiarioDePredicciones.durante(sinConfirmar, USUARIO, () -> llamar(g)));
        assertEquals(peticiones, r.recibidas.size(), "ni POST ni busqueda");
        assertEquals(0, r.posts());
    }

    @Test
    @DisplayName("S14 · la rutina de una prediccion recuperada que el VALIDADOR rechaza se trata igual que una ordinaria")
    void s14ValidacionTrasRecuperar() throws Exception {
        var e = new Escenario().post("g08", "normal").ids(ID, "segunda1");
        var r = replicate(e);
        var ia = new SoloHttp(generador(r));
        var validador = mock(PlanDraftValidator.class);
        doThrow(new InvalidPlanDraftException(List.of("V15: reparto")))
                .doNothing().when(validador).validate(any(), any(), anyInt());
        var pipeline = new PlanGenerationPipeline(ia, mock(TrainingPlanGenerator.class), validador, normalizador());

        var res = pipeline.run(contexto(), 30);

        assertEquals(GenerationSource.AI, res.draft().source());
        verify(validador, times(2)).validate(any(), any(), anyInt());
        assertEquals(2, ia.salidasValidadas.size(), "la recuperada paso por el validador y fue rechazada");
        assertEquals(2, r.posts());
    }

    @Test
    @DisplayName("S15 · sin confirmar: respaldo RULE_ENGINE, nunca una generacion de IA, y un solo POST")
    void s15RespaldoSeguro() throws Exception {
        var e = new Escenario().post("normal", "cierra", "normal").ids("prediccionb1", "prediccionA1", "nunca1");
        var r = replicate(e);
        generador(r).call(PROMPT, 1, 8L, (short) 1, 1); // la de B, identica
        var ia = new SoloHttp(generador(r));
        var reglas = mock(TrainingPlanGenerator.class);
        when(reglas.generate(any(), anyList())).thenReturn(borrador(GenerationSource.RULE_ENGINE));
        var pipeline = new PlanGenerationPipeline(ia, reglas, mock(PlanDraftValidator.class), normalizador());

        var res = pipeline.run(contexto(), 30);

        assertEquals(GenerationSource.RULE_ENGINE, res.draft().source());
        assertTrue(ia.salidasValidadas.isEmpty(), "ninguna salida incierta llega al contrato de la IA");
        assertEquals(2, r.posts(), "el de B y el de A; A no reenvia");
        verify(reglas).generate(any(), argThat(l -> l.stream().anyMatch(x -> x.contains("PREDICCION_NO_CONFIRMADA"))));
    }

    // =====================================================================================================
    // ayudantes
    // =====================================================================================================

    private void concurrentes(ReplicateFalso r) throws Exception {
        var g = generador(r);
        var libro = new LibroSincronizado(new DiarioDePrediccionesReplicateTest.LibroEnMemoria());
        g.setDiario(libro);
        var op = UUID.randomUUID();
        var hilos = Executors.newFixedThreadPool(2);
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
            var a = hilos.submit(tarea);
            var b = hilos.submit(tarea);
            var resultados = List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS));
            assertEquals(1, r.posts(), "una sola prediccion para la misma operacion e intento: " + resultados);
            assertTrue(resultados.contains("{\"ok\":1}"), resultados.toString());
        } finally {
            hilos.shutdownNow();
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
