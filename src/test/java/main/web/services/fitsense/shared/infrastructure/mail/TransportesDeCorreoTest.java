package main.web.services.fitsense.shared.infrastructure.mail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

/** Transportes HTTPS (Resend, Brevo) contra un servidor HTTP local simulado: nunca contra el proveedor real. */
@DisplayName("Correo · transportes resend y brevo contra un servidor simulado")
class TransportesDeCorreoTest {

    record Recibida(String metodo, String ruta, Map<String, List<String>> cabeceras, String cuerpo) {}

    HttpServer servidor;
    List<Recibida> recibidas = new CopyOnWriteArrayList<>();
    volatile int estado = 200;
    URI base;
    final HttpClient http = HttpClient.newHttpClient();
    final TransportesDeCorreo.Remitente remitente = new TransportesDeCorreo.Remitente("no-reply@pruebas.invalid", "Kora");
    final CorreoSaliente.Mensaje mensaje = new CorreoSaliente.Mensaje("PRUEBA", "persona@pruebas.invalid",
            "Asunto \"con comillas\"", "Linea 1\nCodigo: abc_DEF-123");

    @BeforeEach
    void arrancar() throws Exception {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", ex -> {
            var cuerpo = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            recibidas.add(new Recibida(ex.getRequestMethod(), ex.getRequestURI().getPath(), ex.getRequestHeaders(),
                    cuerpo));
            byte[] r = "{\"message\":\"persona@pruebas.invalid rechazada\"}".getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(estado, r.length);
            ex.getResponseBody().write(r);
            ex.close();
        });
        servidor.start();
        base = URI.create("http://127.0.0.1:" + servidor.getAddress().getPort());
    }

    @AfterEach
    void parar() {
        servidor.stop(0);
    }

    JsonNode json(String s) throws Exception {
        return new ObjectMapper().readTree(s);
    }

    @Test
    @DisplayName("resend: POST /emails con Bearer y JSON escapado")
    void resend() throws Exception {
        var c = TransportesDeCorreo.resend(http, base, "clave-sintetica", remitente, Duration.ofSeconds(5));
        c.enviar(mensaje);
        var r = recibidas.get(0);
        assertEquals("POST", r.metodo());
        assertEquals("/emails", r.ruta());
        assertEquals("Bearer clave-sintetica", r.cabeceras().get("Authorization").get(0));
        var j = json(r.cuerpo());
        assertEquals("Kora <no-reply@pruebas.invalid>", j.get("from").asText());
        assertEquals("persona@pruebas.invalid", j.get("to").get(0).asText());
        assertEquals("Asunto \"con comillas\"", j.get("subject").asText());
        assertEquals("Linea 1\nCodigo: abc_DEF-123", j.get("text").asText());
        assertEquals("resend", c.transporte());
        assertTrue(c.entregaReal());
    }

    @Test
    @DisplayName("brevo: POST /v3/smtp/email con api-key y el formato de Brevo")
    void brevo() throws Exception {
        estado = 201;
        var c = TransportesDeCorreo.brevo(http, base, "clave-sintetica", remitente, Duration.ofSeconds(5));
        c.enviar(mensaje);
        var r = recibidas.get(0);
        assertEquals("/v3/smtp/email", r.ruta());
        assertEquals("clave-sintetica", r.cabeceras().get("Api-key").get(0));
        var j = json(r.cuerpo());
        assertEquals("no-reply@pruebas.invalid", j.get("sender").get("email").asText());
        assertEquals("Kora", j.get("sender").get("name").asText());
        assertEquals("persona@pruebas.invalid", j.get("to").get(0).get("email").asText());
        assertEquals("Linea 1\nCodigo: abc_DEF-123", j.get("textContent").asText());
    }

    @Test
    @DisplayName("rechazo del proveedor: excepcion con el codigo HTTP y sin el cuerpo (que repite el destinatario)")
    void rechazo() {
        estado = 422;
        var c = TransportesDeCorreo.resend(http, base, "k", remitente, Duration.ofSeconds(5));
        var e = assertThrows(CorreoSaliente.CorreoNoEnviadoException.class, () -> c.enviar(mensaje));
        assertEquals("resend: HTTP 422", e.getMessage());
        assertFalse(e.getMessage().contains("@"));
    }

    @Test
    @DisplayName("proveedor inalcanzable: excepcion sin datos")
    void inalcanzable() {
        servidor.stop(0);
        var c = TransportesDeCorreo.brevo(http, base, "k", remitente, Duration.ofSeconds(2));
        var e = assertThrows(CorreoSaliente.CorreoNoEnviadoException.class, () -> c.enviar(mensaje));
        assertTrue(e.getMessage().startsWith("brevo: "), e.getMessage());
    }

    @Test
    @DisplayName("el mensaje no muestra destinatario ni cuerpo en toString (logs)")
    void toStringSinDatos() {
        assertFalse(mensaje.toString().contains("persona@"));
        assertFalse(mensaje.toString().contains("abc_DEF"));
    }

    @Test
    @DisplayName("none no entrega; la configuracion incompleta impide arrancar")
    void configuracion() {
        var conf = new CorreoConfiguracion();
        var none = conf.correoSaliente("none", "", "Kora", "", "", 10, null);
        assertFalse(none.entregaReal());
        assertThrows(IllegalStateException.class,
                () -> conf.correoSaliente("resend", "no-reply@pruebas.invalid", "Kora", "", "", 10, null));
        assertThrows(IllegalStateException.class,
                () -> conf.correoSaliente("resend", "no-es-correo", "Kora", "k", "", 10, null));
        assertThrows(IllegalStateException.class,
                () -> conf.correoSaliente("brevo", "no-reply@pruebas.invalid", "Kora", "k", "http://otro.invalid", 10,
                        null));
        assertThrows(IllegalStateException.class,
                () -> conf.correoSaliente("sendgrid", "no-reply@pruebas.invalid", "Kora", "k", "", 10, null));
        assertEquals("resend",
                conf.correoSaliente("resend", "no-reply@pruebas.invalid", "Kora", "k", "", 10, null).transporte());
    }
}
