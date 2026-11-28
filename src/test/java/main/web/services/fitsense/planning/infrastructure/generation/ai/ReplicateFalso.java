package main.web.services.fitsense.planning.infrastructure.generation.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * RT · UN REPLICATE FALSO SOBRE UN SOCKET REAL (sin red, sin credenciales, sin coste).
 * <p>
 * Sirve para reproducir lo que {@code MockRestServiceServer} no puede: respuestas que se cortan a mitad del cuerpo,
 * cabeceras sin {@code Content-Type}, conexiones que se cierran sin responder. El cliente es el de produccion
 * ({@link ReplicateHttp}: {@code HttpURLConnection} con sus limites), asi que se prueba el transporte de verdad.
 * <p>
 * Cada peticion se registra (metodo, ruta y cuerpo) para poder contar cuantos POST recibio "Replicate".
 */
final class ReplicateFalso implements AutoCloseable {

    record Peticion(String metodo, String ruta, String cuerpo) {}

    /** Lo que hace el servidor con cada peticion. */
    @FunctionalInterface
    interface Guion {
        void responder(Peticion p, Socket s, OutputStream out) throws Exception;
    }

    private static final ObjectMapper JSON = new ObjectMapper();
    private final ServerSocket ss;
    private final List<Socket> abiertos = Collections.synchronizedList(new ArrayList<>());
    final List<Peticion> recibidas = Collections.synchronizedList(new ArrayList<>());

    ReplicateFalso(Guion guion) throws IOException {
        ss = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
        var hilo = new Thread(() -> {
            while (!ss.isClosed()) {
                try {
                    var s = ss.accept();
                    abiertos.add(s);
                    var t = new Thread(() -> atender(s, guion));
                    t.setDaemon(true);
                    t.start();
                } catch (IOException e) {
                    return;
                }
            }
        });
        hilo.setDaemon(true);
        hilo.start();
    }

    /** La URL de creacion de predicciones del modelo, como la de produccion. */
    String urlDelModelo() {
        return "http://127.0.0.1:" + ss.getLocalPort() + "/v1/models/openai/gpt-5-mini/predictions";
    }

    long cuantos(String metodo, String prefijo) {
        synchronized (recibidas) {
            return recibidas.stream().filter(p -> p.metodo().equals(metodo) && p.ruta().startsWith(prefijo)).count();
        }
    }

    long posts() {
        return cuantos("POST", "/v1/models/");
    }

    /** La {@code input} del ultimo POST de creacion, tal como la recibio el servidor. */
    JsonNode ultimaEntrada() throws IOException {
        Peticion ultimo = null;
        synchronized (recibidas) {
            for (var p : recibidas) if (p.metodo().equals("POST") && p.ruta().startsWith("/v1/models/")) ultimo = p;
        }
        if (ultimo == null) throw new IllegalStateException("no hubo POST");
        return JSON.readTree(ultimo.cuerpo()).get("input");
    }

    private void atender(Socket s, Guion guion) {
        try {
            var in = s.getInputStream();
            var linea = leerLinea(in);
            if (linea == null || linea.isBlank()) return;
            var partes = linea.split(" ");
            int largo = -1;
            boolean troceado = false;
            String h;
            while ((h = leerLinea(in)) != null && !h.isEmpty()) {
                var minus = h.toLowerCase(Locale.ROOT);
                if (minus.startsWith("content-length:")) largo = Integer.parseInt(h.substring(15).trim());
                if (minus.startsWith("transfer-encoding:") && minus.contains("chunked")) troceado = true;
            }
            String cuerpo = troceado ? leerTrozos(in) : largo > 0 ? new String(in.readNBytes(largo),
                    StandardCharsets.UTF_8) : "";
            var p = new Peticion(partes[0], partes.length > 1 ? partes[1] : "", cuerpo);
            recibidas.add(p);
            guion.responder(p, s, s.getOutputStream());
        } catch (Exception e) {
            // el cliente cerro o el guion corto la conexion a proposito
        } finally {
            try {
                s.close();
            } catch (IOException ignorada) {
                // nada
            }
        }
    }

    private static String leerLinea(InputStream in) throws IOException {
        var b = new ByteArrayOutputStream();
        int c;
        while ((c = in.read()) != -1) {
            if (c == '\n') break;
            if (c != '\r') b.write(c);
        }
        if (c == -1 && b.size() == 0) return null;
        return b.toString(StandardCharsets.US_ASCII);
    }

    private static String leerTrozos(InputStream in) throws IOException {
        var b = new ByteArrayOutputStream();
        while (true) {
            var tam = leerLinea(in);
            if (tam == null) break;
            int n = Integer.parseInt(tam.split(";")[0].trim(), 16);
            if (n == 0) {
                leerLinea(in);
                break;
            }
            b.write(in.readNBytes(n));
            leerLinea(in);
        }
        return b.toString(StandardCharsets.UTF_8);
    }

    // ------------------------------------------------------------------ respuestas

    static void json(OutputStream out, int estado, String cuerpo) throws IOException {
        json(out, estado, cuerpo, "");
    }

    /** Con cabeceras extra ({@code "Nombre: valor\r\n"}), p. ej. un {@code Location} que la API no documenta. */
    static void json(OutputStream out, int estado, String cuerpo, String cabeceras) throws IOException {
        var bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
        out.write(("HTTP/1.1 " + estado + " X\r\nContent-Type: application/json\r\nContent-Length: " + bytes.length
                + "\r\n" + cabeceras + "Connection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
        out.write(bytes);
        out.flush();
    }

    /**
     * EL CASO G08 (H2-v4, 26/11/2026): cabeceras 2xx SIN {@code Content-Type} y un cuerpo que se corta antes de su
     * {@code Content-Length}. Spring da el tipo por {@code application/octet-stream} y falla al leer el cuerpo.
     */
    static void cortadaSinTipo(Socket s, OutputStream out, String principio) throws IOException {
        out.write(("HTTP/1.1 201 Created\r\nContent-Length: 5000\r\nConnection: close\r\n\r\n" + principio)
                .getBytes(StandardCharsets.UTF_8));
        out.flush();
        // RST, no FIN: la conexion se REINICIA a mitad del cuerpo (lo que hace un proxy o una red que corta). Con un
        // cierre normal, HttpURLConnection entregaria el cuerpo incompleto sin error.
        s.setSoLinger(true, 0);
        s.close();
    }

    /** Cabeceras 2xx y un cuerpo cortado con un cierre NORMAL (FIN): el cliente recibe un JSON incompleto. */
    static void truncada(OutputStream out, String principio) throws IOException {
        out.write(("HTTP/1.1 201 Created\r\nContent-Type: application/json\r\nContent-Length: 5000\r\n"
                + "Connection: close\r\n\r\n" + principio).getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    /** Cabeceras 2xx y el principio del cuerpo; luego NADA hasta que el cliente se rinde (limite de lectura). */
    static void colgadaAMedias(OutputStream out, String principio) throws Exception {
        out.write(("HTTP/1.1 201 Created\r\nContent-Type: application/json\r\nContent-Length: 5000\r\n\r\n"
                + principio).getBytes(StandardCharsets.UTF_8));
        out.flush();
        Thread.sleep(20_000);
    }

    /** Una prediccion como la devuelve Replicate (sin metricas). {@code output} ya en JSON o null. */
    static String prediccion(String id, String estado, String outputJson, JsonNode input, String base) {
        return prediccion(id, estado, outputJson, input, base, Instant.now(), false);
    }

    /** Con la fecha de creacion y el indicador {@code data_removed} de Replicate. */
    static String prediccion(String id, String estado, String outputJson, JsonNode input, String base,
                             Instant creada, boolean datosBorrados) {
        var urls = "{\"get\":\"" + base + "/v1/predictions/" + id + "\",\"cancel\":\"" + base + "/v1/predictions/" + id
                + "/cancel\"}";
        return "{\"id\":\"" + id + "\",\"model\":\"openai/gpt-5-mini\",\"source\":\"api\",\"status\":\"" + estado
                + "\",\"created_at\":\"" + creada + "\",\"data_removed\":" + datosBorrados + ",\"input\":"
                + (input == null || datosBorrados ? "{}" : input.toString())
                + ",\"output\":" + (outputJson == null || datosBorrados ? "null" : outputJson) + ",\"error\":null,\"urls\":"
                + urls + "}";
    }

    /** El listado de {@code GET /v1/predictions}: como el real, con la entrada vacia. */
    static String listado(String... ids) {
        var sb = new StringBuilder("{\"next\":null,\"results\":[");
        for (int i = 0; i < ids.length; i++) {
            if (i > 0) sb.append(',');
            sb.append("{\"id\":\"").append(ids[i]).append("\",\"model\":\"openai/gpt-5-mini\",\"source\":\"api\"")
                    .append(",\"status\":\"processing\",\"created_at\":\"").append(Instant.now())
                    .append("\",\"input\":{},\"output\":null}");
        }
        return sb.append("]}").toString();
    }

    String base() {
        return "http://127.0.0.1:" + ss.getLocalPort();
    }

    @Override
    public void close() throws IOException {
        ss.close();
        synchronized (abiertos) {
            for (var s : abiertos) s.close();
        }
    }
}
