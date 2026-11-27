package main.web.services.fitsense.planning.infrastructure.generation.ai;

import main.web.services.fitsense.planning.domain.exceptions.AiProviderUnavailableException;
import main.web.services.fitsense.shared.infrastructure.json.JsonSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.ConnectException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/**
 * V59 · LIBRO DE PREDICCIONES en el cliente de Replicate. HTTP simulado (MockRestServiceServer) y reloj controlado:
 * ninguna prueba llama a Replicate ni espera de verdad. Cada prueba fija CUANTAS peticiones HTTP se hacen: el
 * objetivo es demostrar que una reanudacion no paga otra prediccion cuando hay una recuperable.
 */
@DisplayName("V59 · libro de predicciones: reanudar sin pagar otra prediccion")
class DiarioDePrediccionesReplicateTest {

    private static final String URL = "https://api.replicate.test/v1/models/openai/gpt-5-structured/predictions";
    private static final String GET = "https://api.replicate.test/v1/predictions/p1";
    private static final long USUARIO = 7L;
    private static final String PROMPT = "prompt";

    private static final class Reloj extends Clock {
        Instant ahora = Instant.parse("2026-11-16T12:00:00Z");
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return ahora; }
    }

    /** Libro en memoria con la misma semantica que JdbcDiarioDePredicciones (probado aparte contra PostgreSQL). */
    static final class LibroEnMemoria implements DiarioDePredicciones {
        final List<Entrada> filas = new ArrayList<>();
        final List<String> eventos = new ArrayList<>();
        long secuencia = 0;

        Entrada fila(long id) {
            return filas.stream().filter(f -> f.id() == id).findFirst().orElseThrow();
        }

        void reemplazar(Entrada nueva) {
            filas.replaceAll(f -> f.id() == nueva.id() ? nueva : f);
        }

        Entrada anadir(UUID op, int intento, String hash, Estado estado, String pid, String salida) {
            var e = new Entrada(++secuencia, op, USUARIO, intento, hash, estado, pid, salida, null, null);
            filas.add(e);
            return e;
        }

        @Override public Optional<Entrada> deIntento(UUID op, int intento) {
            return filas.stream().filter(f -> f.operationId().equals(op) && f.attempt() == intento
                    && f.estado() != Estado.DESCARTADA).findFirst();
        }

        /** Para simular otra ejecucion que anota el mismo intento justo antes que esta (carrera). */
        Runnable antesDeSolicitar = () -> { };

        @Override public long solicitar(UUID op, long u, int intento, String hash, Instant ahora) {
            antesDeSolicitar.run();
            antesDeSolicitar = () -> { };
            // Mismas reglas que JdbcDiarioDePredicciones: solo se descarta la fila de OTRA peticion; la misma peticion
            // ya anotada viola ux_pgp_intento_vigente.
            var vigente = deIntento(op, intento);
            if (vigente.isPresent() && vigente.get().requestHash().equals(hash))
                throw new org.springframework.dao.DuplicateKeyException("ux_pgp_intento_vigente");
            vigente.ifPresent(f -> reemplazar(new Entrada(f.id(), f.operationId(), f.userId(),
                    f.attempt(), f.requestHash(), Estado.DESCARTADA, f.predictionId(), f.output(), f.error(),
                    f.reusedFrom())));
            eventos.add("solicitar " + intento);
            return anadir(op, intento, hash, Estado.SOLICITANDO, null, null).id();
        }

        @Override public void enviada(long id, String pid, Instant ahora) {
            var f = fila(id);
            if (f.predictionId() != null || (f.estado() != Estado.SOLICITANDO && f.estado() != Estado.NO_CONFIRMADA))
                return;
            eventos.add("enviada " + pid);
            reemplazar(new Entrada(id, f.operationId(), f.userId(), f.attempt(), f.requestHash(),
                    f.estado() == Estado.SOLICITANDO ? Estado.ENVIADA : f.estado(), pid, null, null, f.reusedFrom()));
        }

        @Override public void consultada(long id, int consultas, Instant ahora) {
            eventos.add("consultada " + consultas);
        }

        @Override public void terminada(long id, Estado estado, String salida, String error, Instant ahora) {
            var f = fila(id);
            eventos.add("terminada " + estado);
            boolean abierta = f.estado() == Estado.SOLICITANDO || f.estado() == Estado.ENVIADA;
            reemplazar(new Entrada(id, f.operationId(), f.userId(), f.attempt(), f.requestHash(),
                    abierta ? estado : f.estado(), f.predictionId(), salida != null ? salida : f.output(),
                    abierta ? error : f.error(), f.reusedFrom()));
        }

        @Override public Recuento recuento(UUID op) {
            throw new UnsupportedOperationException();
        }
    }

    private MockRestServiceServer server;
    private Reloj reloj;

    private ReplicateTrainingPlanGenerator generador(LibroEnMemoria libro) {
        var builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        reloj = new Reloj();
        var p = new ReplicateProperties(true, "token-de-prueba", URL, "gpt-5-nano", "medium", 150, 60);
        var g = new ReplicateTrainingPlanGenerator(p, null, null, new JsonSupport(), builder)
                .conTiempoDePrueba(reloj, d -> reloj.ahora = reloj.ahora.plusSeconds(60));
        g.setDiario(libro);
        return g;
    }

    private static String prediccion(String status) {
        return "{\"id\":\"p1\",\"status\":\"" + status + "\",\"output\":"
                + ("succeeded".equals(status) ? "{\"json_output\":{\"ok\":1}}" : "null")
                + ",\"urls\":{\"get\":\"" + GET + "\",\"cancel\":null}}";
    }

    private String llamar(ReplicateTrainingPlanGenerator g, UUID op, int intento) {
        return DiarioDePredicciones.durante(op, USUARIO, () -> g.call(PROMPT, 1, USUARIO, (short) 1, intento));
    }

    // ------------------------------------------------------------------------------------------------------
    @Test
    @DisplayName("nueva: se anota ANTES del POST, el POST va SIN Prefer: wait y el id se guarda antes de esperar")
    void nuevaPrediccionAnotadaAntesDeEnviar() {
        var libro = new LibroEnMemoria();
        var g = generador(libro);
        var op = UUID.randomUUID();
        var anotadaAlEnviar = new AtomicReference<DiarioDePredicciones.Estado>();
        server.expect(ExpectedCount.once(), requestTo(URL)).andExpect(method(HttpMethod.POST))
                .andExpect(headerDoesNotExist("Prefer"))
                .andExpect(r -> anotadaAlEnviar.set(libro.deIntento(op, 1).orElseThrow().estado()))
                .andRespond(withSuccess(prediccion("starting"), MediaType.APPLICATION_JSON));
        server.expect(ExpectedCount.once(), requestTo(GET))
                .andRespond(withSuccess(prediccion("succeeded"), MediaType.APPLICATION_JSON));

        assertEquals("{\"ok\":1}", llamar(g, op, 1));
        server.verify();
        assertEquals(DiarioDePredicciones.Estado.SOLICITANDO, anotadaAlEnviar.get(), "anotada antes de enviar");
        var f = libro.deIntento(op, 1).orElseThrow();
        assertEquals(DiarioDePredicciones.Estado.SUCCEEDED, f.estado());
        assertEquals("p1", f.predictionId());
        assertEquals("{\"ok\":1}", f.output());
        assertEquals(List.of("solicitar 1", "enviada p1", "consultada 1", "terminada SUCCEEDED"), libro.eventos);
    }

    @Test
    @DisplayName("E4 · reanudar una prediccion ya identificada: se consulta por su id (GET), CERO POST")
    void reanudaPorIdentificador() {
        var libro = new LibroEnMemoria();
        var g = generador(libro);
        var op = UUID.randomUUID();
        libro.anadir(op, 1, g.huellaDe(PROMPT, 1), DiarioDePredicciones.Estado.ENVIADA, "p1", null);
        server.expect(ExpectedCount.times(2), requestTo(GET)).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(prediccion("processing"), MediaType.APPLICATION_JSON));
        server.expect(ExpectedCount.once(), requestTo(GET))
                .andRespond(withSuccess(prediccion("succeeded"), MediaType.APPLICATION_JSON));

        assertEquals("{\"ok\":1}", llamar(g, op, 1));
        server.verify(); // cualquier POST habria fallado: no estaba esperado
        assertEquals(DiarioDePredicciones.Estado.SUCCEEDED, libro.deIntento(op, 1).orElseThrow().estado());
    }

    @Test
    @DisplayName("E7 · la prediccion termino mientras el backend reiniciaba: un GET la recoge, CERO POST")
    void terminoDuranteElReinicio() {
        var libro = new LibroEnMemoria();
        var g = generador(libro);
        var op = UUID.randomUUID();
        libro.anadir(op, 1, g.huellaDe(PROMPT, 1), DiarioDePredicciones.Estado.ENVIADA, "p1", null);
        server.expect(ExpectedCount.once(), requestTo(GET))
                .andRespond(withSuccess(prediccion("succeeded"), MediaType.APPLICATION_JSON));
        assertEquals("{\"ok\":1}", llamar(g, op, 1));
        server.verify();
    }

    @Test
    @DisplayName("E10 · un resultado ya recibido se reutiliza tantas veces como se reanude: CERO peticiones HTTP")
    void resultadoRecibidoDosVeces() {
        var libro = new LibroEnMemoria();
        var g = generador(libro);
        var op = UUID.randomUUID();
        libro.anadir(op, 1, g.huellaDe(PROMPT, 1), DiarioDePredicciones.Estado.SUCCEEDED, "p1", "{\"ok\":1}");
        assertEquals("{\"ok\":1}", llamar(g, op, 1));
        assertEquals("{\"ok\":1}", llamar(g, op, 1));
        server.verify();
    }

    @Test
    @DisplayName("E17 · aceptada externamente sin id confirmado (SOLICITANDO): NO se reenvia; NO_CONFIRMADA y fallo de proveedor")
    void aceptadaSinIdNoSeReenvia() {
        var libro = new LibroEnMemoria();
        var g = generador(libro);
        var op = UUID.randomUUID();
        libro.anadir(op, 1, g.huellaDe(PROMPT, 1), DiarioDePredicciones.Estado.SOLICITANDO, null, null);
        var e = assertThrows(AiProviderUnavailableException.class, () -> llamar(g, op, 1));
        assertTrue(e.getMessage().contains("PREDICCION_NO_CONFIRMADA"), e.getMessage());
        server.verify();
        assertEquals(DiarioDePredicciones.Estado.NO_CONFIRMADA, libro.deIntento(op, 1).orElseThrow().estado());
        // y una segunda reanudacion repite el desenlace sin pedir nada
        assertThrows(AiProviderUnavailableException.class, () -> llamar(g, op, 1));
        server.verify();
    }

    @Test
    @DisplayName("E13 · error del proveedor ya registrado (FAILED): se repite el desenlace, CERO POST")
    void errorRegistradoSeRepite() {
        var libro = new LibroEnMemoria();
        var g = generador(libro);
        var op = UUID.randomUUID();
        libro.anadir(op, 1, g.huellaDe(PROMPT, 1), DiarioDePredicciones.Estado.FAILED, "p1", null);
        assertThrows(AiProviderUnavailableException.class, () -> llamar(g, op, 1));
        server.verify();
    }

    @Test
    @DisplayName("E13 · 5xx del POST: el envio pudo aceptarse -> NO_CONFIRMADA (incierto), no ERROR_ENVIO")
    void cincoXXEsIncierto() {
        var libro = new LibroEnMemoria();
        var g = generador(libro);
        var op = UUID.randomUUID();
        server.expect(ExpectedCount.once(), requestTo(URL)).andRespond(withStatus(HttpStatus.BAD_GATEWAY));
        assertThrows(AiProviderUnavailableException.class, () -> llamar(g, op, 1));
        server.verify();
        assertEquals(DiarioDePredicciones.Estado.NO_CONFIRMADA, libro.deIntento(op, 1).orElseThrow().estado());
    }

    @Test
    @DisplayName("4xx del POST: Replicate la rechazo, no existe -> ERROR_ENVIO")
    void cuatroXXNoExiste() {
        var libro = new LibroEnMemoria();
        var g = generador(libro);
        var op = UUID.randomUUID();
        server.expect(ExpectedCount.once(), requestTo(URL)).andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY));
        assertThrows(AiProviderUnavailableException.class, () -> llamar(g, op, 1));
        server.verify();
        assertEquals(DiarioDePredicciones.Estado.ERROR_ENVIO, libro.deIntento(op, 1).orElseThrow().estado());
    }

    @Test
    @DisplayName("E14 · proveedor inaccesible (conexion rechazada): no llego a enviarse -> ERROR_ENVIO")
    void conexionRechazada() {
        var libro = new LibroEnMemoria();
        var g = generador(libro);
        var op = UUID.randomUUID();
        server.expect(ExpectedCount.once(), requestTo(URL))
                .andRespond(withException(new ConnectException("Connection refused")));
        assertThrows(AiProviderUnavailableException.class, () -> llamar(g, op, 1));
        server.verify();
        assertEquals(DiarioDePredicciones.Estado.ERROR_ENVIO, libro.deIntento(op, 1).orElseThrow().estado());
    }

    @Test
    @DisplayName("corte de lectura del POST (pudo crearse): NO_CONFIRMADA")
    void corteDeLecturaEsIncierto() {
        var libro = new LibroEnMemoria();
        var g = generador(libro);
        var op = UUID.randomUUID();
        server.expect(ExpectedCount.once(), requestTo(URL)).andRespond(withException(new IOException("reset")));
        assertThrows(AiProviderUnavailableException.class, () -> llamar(g, op, 1));
        server.verify();
        assertEquals(DiarioDePredicciones.Estado.NO_CONFIRMADA, libro.deIntento(op, 1).orElseThrow().estado());
    }

    // REVISION DE IDENTIDAD (integracion, 27/11/2026). Las dos pruebas E8 afirmaban que una operacion NUEVA del mismo
    // usuario ADOPTABA la prediccion de otra con la misma huella (CERO POST). El contenido no prueba identidad: ahora
    // cada operacion pide la suya. Las pruebas se invierten a proposito; ver 11_INTEGRACION_FINAL_Y_MERGES.md.

    @Test
    @DisplayName("E8 (revisada) · la prediccion TERMINADA de otra operacion del MISMO usuario y la misma entrada NO se"
            + " adopta: la nueva pide la suya (1 POST)")
    void noAdoptaLaTerminadaDeOtraOperacion() {
        var libro = new LibroEnMemoria();
        var g = generador(libro);
        var vencida = UUID.randomUUID();
        var nueva = UUID.randomUUID();
        libro.anadir(vencida, 1, g.huellaDe(PROMPT, 1), DiarioDePredicciones.Estado.SUCCEEDED, "p1", "{\"ajeno\":1}");
        server.expect(ExpectedCount.once(), requestTo(URL))
                .andRespond(withSuccess(prediccion("succeeded"), MediaType.APPLICATION_JSON));
        assertEquals("{\"ok\":1}", llamar(g, nueva, 1), "su propia salida, no la de la otra operacion");
        server.verify();
        var f = libro.deIntento(nueva, 1).orElseThrow();
        assertNull(f.reusedFrom(), "nada heredado");
        assertTrue(libro.eventos.stream().noneMatch(e -> e.startsWith("adoptar")));
    }

    @Test
    @DisplayName("E8 (revisada) · la de otra operacion AUN EN CURSO tampoco se adopta ni se consulta: 1 POST propio")
    void noAdoptaLaEnCursoDeOtraOperacion() {
        var libro = new LibroEnMemoria();
        var g = generador(libro);
        libro.anadir(UUID.randomUUID(), 1, g.huellaDe(PROMPT, 1), DiarioDePredicciones.Estado.ENVIADA, "pOtra", null);
        server.expect(ExpectedCount.once(), requestTo(URL))
                .andRespond(withSuccess(prediccion("succeeded"), MediaType.APPLICATION_JSON));
        assertEquals("{\"ok\":1}", llamar(g, UUID.randomUUID(), 1));
        server.verify();   // ningun GET a la prediccion de la otra operacion
    }

    @Test
    @DisplayName("identidad · una generacion y una adaptacion con la MISMA entrada no comparten prediccion")
    void generacionYAdaptacionNoSeConfunden() {
        var libro = new LibroEnMemoria();
        var g = generador(libro);
        var generacion = UUID.randomUUID();
        // La adaptacion anotada con la huella que tendria su propia peticion: aun asi, otra operacion no la ve.
        libro.anadir(generacion, 1, g.huellaDe(PROMPT, 1), DiarioDePredicciones.Estado.SUCCEEDED, "pGen",
                "{\"generacion\":1}");
        server.expect(ExpectedCount.once(), requestTo(URL))
                .andRespond(withSuccess(prediccion("succeeded"), MediaType.APPLICATION_JSON));
        var adaptacion = UUID.randomUUID();
        var salida = DiarioDePredicciones.durante(adaptacion, USUARIO, () -> g.call(PROMPT,
                ReplicateTrainingPlanGenerator.Peticion.deCorrecciones(1), USUARIO, (short) 1, 1));
        assertEquals("{\"ok\":1}", salida);
        server.verify();
        assertNull(libro.deIntento(adaptacion, 1).orElseThrow().reusedFrom());
    }

    @Test
    @DisplayName("nunca se reutiliza la prediccion de OTRO usuario ni la de otra peticion")
    void noSeReutilizaDeOtroUsuarioNiOtraPeticion() {
        var libro = new LibroEnMemoria();
        var g = generador(libro);
        var otra = UUID.randomUUID();
        libro.filas.add(new DiarioDePredicciones.Entrada(++libro.secuencia, otra, 99L, 1, g.huellaDe(PROMPT, 1),
                DiarioDePredicciones.Estado.SUCCEEDED, "pX", "{\"ajeno\":1}", null, null));
        libro.anadir(otra, 1, g.huellaDe("otro prompt", 1), DiarioDePredicciones.Estado.SUCCEEDED, "pY", "{\"y\":1}");
        server.expect(ExpectedCount.once(), requestTo(URL))
                .andRespond(withSuccess(prediccion("succeeded"), MediaType.APPLICATION_JSON));
        assertEquals("{\"ok\":1}", llamar(g, UUID.randomUUID(), 1));
        server.verify();
    }

    @Test
    @DisplayName("la peticion cambio (otra huella): la fila vieja queda DESCARTADA y se pide una nueva")
    void peticionDistintaSeDescarta() {
        var libro = new LibroEnMemoria();
        var g = generador(libro);
        var op = UUID.randomUUID();
        libro.anadir(op, 1, "f".repeat(64), DiarioDePredicciones.Estado.SUCCEEDED, "p0", "{\"viejo\":1}");
        server.expect(ExpectedCount.once(), requestTo(URL))
                .andRespond(withSuccess(prediccion("succeeded"), MediaType.APPLICATION_JSON));
        assertEquals("{\"ok\":1}", llamar(g, op, 1));
        server.verify();
        assertEquals(DiarioDePredicciones.Estado.DESCARTADA, libro.fila(1).estado());
    }

    @Test
    @DisplayName("R17 · carrera: otra ejecucion anota el intento (ya ENVIADA) justo antes: se sigue la suya, CERO POST")
    void carreraConOtraEjecucionEnviada() {
        var libro = new LibroEnMemoria();
        var g = generador(libro);
        var op = UUID.randomUUID();
        libro.antesDeSolicitar = () -> libro.anadir(op, 1, g.huellaDe(PROMPT, 1),
                DiarioDePredicciones.Estado.ENVIADA, "p1", null);
        server.expect(ExpectedCount.once(), requestTo(GET)).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(prediccion("succeeded"), MediaType.APPLICATION_JSON));
        assertEquals("{\"ok\":1}", llamar(g, op, 1));
        server.verify();
        assertEquals(1, libro.filas.size(), "no se anoto una segunda prediccion");
        assertEquals(DiarioDePredicciones.Estado.SUCCEEDED, libro.fila(1).estado());
    }

    @Test
    @DisplayName("R07/R17 · carrera: la otra ejecucion esta a mitad de su POST (SOLICITANDO): NO se envia otra; NO_CONFIRMADA")
    void carreraConOtraEjecucionSinConfirmar() {
        var libro = new LibroEnMemoria();
        var g = generador(libro);
        var op = UUID.randomUUID();
        libro.antesDeSolicitar = () -> libro.anadir(op, 1, g.huellaDe(PROMPT, 1),
                DiarioDePredicciones.Estado.SOLICITANDO, null, null);
        var e = assertThrows(AiProviderUnavailableException.class, () -> llamar(g, op, 1));
        assertTrue(e.getMessage().contains("PREDICCION_NO_CONFIRMADA"), e.getMessage());
        server.verify();
        assertEquals(1, libro.filas.size());
        assertEquals(DiarioDePredicciones.Estado.NO_CONFIRMADA, libro.fila(1).estado());
    }

    @Test
    @DisplayName("fencing: si el worker ya no es propietario, no se envia nada")
    void sinPropiedadNoSeEnvia() {
        var libro = new LibroEnMemoria();
        var g = generador(libro);
        var op = UUID.randomUUID();
        assertThrows(IllegalStateException.class, () -> DiarioDePredicciones.durante(op, USUARIO,
                () -> { throw new IllegalStateException("ya no es tuya"); },
                () -> g.call(PROMPT, 1, USUARIO, (short) 1, 1)));
        server.verify();
        assertTrue(libro.filas.isEmpty());
    }

    @Test
    @DisplayName("R04 · sin propiedad tampoco se reanuda: la fila SOLICITANDO del propietario NO se marca NO_CONFIRMADA")
    void sinPropiedadNoSeTocaElLibro() {
        var libro = new LibroEnMemoria();
        var g = generador(libro);
        var op = UUID.randomUUID();
        libro.anadir(op, 1, g.huellaDe(PROMPT, 1), DiarioDePredicciones.Estado.SOLICITANDO, null, null);
        assertThrows(IllegalStateException.class, () -> DiarioDePredicciones.durante(op, USUARIO,
                () -> { throw new IllegalStateException("ya no es tuya"); },
                () -> g.call(PROMPT, 1, USUARIO, (short) 1, 1)));
        server.verify();
        assertEquals(DiarioDePredicciones.Estado.SOLICITANDO, libro.fila(1).estado());
    }

    @Test
    @DisplayName("fuera de una operacion asincrona (endpoint sincrono, ciclo, adaptacion): igual que antes, con Prefer: wait")
    void sinAmbitoNadaCambia() {
        var libro = new LibroEnMemoria();
        var g = generador(libro);
        server.expect(ExpectedCount.once(), requestTo(URL)).andExpect(header("Prefer", "wait=60"))
                .andRespond(withSuccess(prediccion("succeeded"), MediaType.APPLICATION_JSON));
        assertEquals("{\"ok\":1}", g.call(PROMPT, 1, USUARIO, (short) 1, 1));
        server.verify();
        assertTrue(libro.filas.isEmpty(), "sin ambito el libro no se usa");
    }

    @Test
    @DisplayName("la huella es estable entre arranques: no depende del orden de un Map.of")
    void huellaEstable() {
        var a = new java.util.LinkedHashMap<String, Object>();
        a.put("model", "m");
        a.put("prompt", "p");
        a.put("json_schema", java.util.Map.of("b", 1, "a", java.util.Map.of("y", 2, "x", 3)));
        var b = new java.util.LinkedHashMap<String, Object>();
        b.put("json_schema", new java.util.TreeMap<>(java.util.Map.of("a", new java.util.TreeMap<>(java.util.Map.of("x", 3, "y", 2)), "b", 1)));
        b.put("prompt", "p");
        b.put("model", "m");
        assertEquals(ReplicateTrainingPlanGenerator.huella(a), ReplicateTrainingPlanGenerator.huella(b));
        assertEquals(64, ReplicateTrainingPlanGenerator.huella(a).length());
    }
}
