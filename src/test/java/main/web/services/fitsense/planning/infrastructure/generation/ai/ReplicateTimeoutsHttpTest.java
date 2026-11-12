package main.web.services.fitsense.planning.infrastructure.generation.ai;

import main.web.services.fitsense.planning.domain.exceptions.AiProviderUnavailableException;
import main.web.services.fitsense.shared.infrastructure.json.JsonSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * R-3 · LIMITES HTTP EXPLICITOS, PROBADOS CONTRA SERVIDORES LOCALES DE VERDAD (resiliencia de Replicate, 11/11/2026).
 * <p>
 * Nada de Replicate: cada prueba levanta un {@link ServerSocket} en 127.0.0.1 que se comporta mal a proposito (no
 * acepta, acepta y calla, corta a medias) y mide que la llamada termine dentro del limite configurado, con un fallo
 * del PROVEEDOR -el que lleva al respaldo-, y no colgada.
 * <p>
 * Los limites se bajan a 2-3 s para que la suite no tarde; la logica es la misma que con los de produccion.
 */
@DisplayName("R-3 · timeouts HTTP de Replicate: conexion, lectura, respuesta a medias y sondeo, todos acotados")
class ReplicateTimeoutsHttpTest {

    private final List<AutoCloseable> abiertos = new ArrayList<>();

    @AfterEach
    void cerrar() throws Exception {
        for (var a : abiertos) a.close();
    }

    /** Lectura del POST 3 s (Prefer: wait 1 s), sondeo 2 s, conexion 2 s, sondeo total 6 s. */
    private ReplicateTrainingPlanGenerator generador(String url) {
        var p = new ReplicateProperties(true, "token-de-prueba", url, "gpt-5-nano", "medium", 6, 1, 2, 3, 2);
        return new ReplicateTrainingPlanGenerator(p, null, null, new JsonSupport(),
                ReplicateHttp.post(p), ReplicateHttp.sondeo(p));
    }

    /** Un servidor que a cada conexion le hace lo que diga {@code conducta}, en su propio hilo. */
    private int servidor(Consumer<Socket> conducta) throws IOException {
        var ss = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
        abiertos.add(ss);
        var hilo = new Thread(() -> {
            while (!ss.isClosed()) {
                try {
                    var s = ss.accept();
                    abiertos.add(s);
                    new Thread(() -> conducta.accept(s)).start();
                } catch (IOException e) {
                    return;
                }
            }
        });
        hilo.setDaemon(true);
        hilo.start();
        return ss.getLocalPort();
    }

    private static void leerCabeceras(Socket s) throws IOException {
        InputStream in = s.getInputStream();
        int a = 0, b = 0, c = 0, d;
        while ((d = in.read()) != -1) {
            if (a == '\r' && b == '\n' && c == '\r' && d == '\n') return;
            a = b; b = c; c = d;
        }
    }

    private static void responder(Socket s, String cuerpo) throws IOException {
        leerCabeceras(s);
        drenar(s);
        var bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
        OutputStream out = s.getOutputStream();
        out.write(("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: " + bytes.length
                + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
        out.write(bytes);
        out.flush();
    }

    /** El cuerpo del POST: lo que haya disponible, sin bloquear. */
    private static void drenar(Socket s) throws IOException {
        try {
            s.setSoTimeout(200);
            var buf = new byte[8192];
            while (s.getInputStream().read(buf) > 0) {
                // se descarta
            }
        } catch (java.net.SocketTimeoutException e) {
            // ya no hay mas
        } finally {
            s.setSoTimeout(0);
        }
    }

    private static long medir(Runnable r) {
        long t = System.nanoTime();
        r.run();
        return Duration.ofNanos(System.nanoTime() - t).toMillis();
    }

    @Test
    @DisplayName("el servidor no acepta conexiones: fallo del proveedor enseguida, sin colgarse")
    void sinConexion() throws IOException {
        int puerto;
        try (var ss = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            puerto = ss.getLocalPort();
        }
        var g = generador("http://127.0.0.1:" + puerto + "/predictions");
        long ms = medir(() -> assertThrows(AiProviderUnavailableException.class,
                () -> g.call("prompt", 1, 7L, (short) 1, 1)));
        assertTrue(ms < 3_000, "tardo " + ms + " ms");
    }

    @Test
    @DisplayName("acepta la conexion y no responde: corta al limite de lectura del POST (3 s), no espera sin fin")
    void aceptaYNoResponde() throws IOException {
        var cerrada = new CountDownLatch(1);
        int puerto = servidor(s -> {
            try {
                leerCabeceras(s);
                drenar(s);
                // No responde. Espera a que el cliente se rinda y cierre: entonces read() devuelve -1.
                if (s.getInputStream().read() == -1) cerrada.countDown();
            } catch (IOException e) {
                cerrada.countDown();
            }
        });
        var g = generador("http://127.0.0.1:" + puerto + "/predictions");

        long ms = medir(() -> assertThrows(AiProviderUnavailableException.class,
                () -> g.call("prompt", 1, 7L, (short) 1, 1)));

        assertTrue(ms >= 2_500 && ms < 8_000, "tardo " + ms + " ms; el limite de lectura es 3 s");
        assertDoesNotThrow(() -> assertTrue(cerrada.await(3, TimeUnit.SECONDS),
                "el cliente debe soltar la conexion al rendirse"));
    }

    @Test
    @DisplayName("respuesta a medias que se interrumpe: corta al limite de lectura, no se queda esperando el resto")
    void respuestaParcial() throws IOException {
        int puerto = servidor(s -> {
            try {
                leerCabeceras(s);
                drenar(s);
                var out = s.getOutputStream();
                out.write(("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: 500\r\n\r\n"
                        + "{\"id\":\"p1\",\"status\":\"succ").getBytes(StandardCharsets.US_ASCII));
                out.flush();
                Thread.sleep(20_000);
            } catch (Exception e) {
                // el cliente cerro
            }
        });
        var g = generador("http://127.0.0.1:" + puerto + "/predictions");

        long ms = medir(() -> assertThrows(AiProviderUnavailableException.class,
                () -> g.call("prompt", 1, 7L, (short) 1, 1)));

        assertTrue(ms < 8_000, "tardo " + ms + " ms");
    }

    @Test
    @DisplayName("un GET de sondeo que no responde: corta al limite del sondeo (2 s) y termina como fallo del proveedor")
    void sondeoColgado() throws IOException {
        var puertoHolder = new int[1];
        puertoHolder[0] = servidor(s -> {
            try {
                leerCabeceras(s);
                // La primera conexion es el POST: prediccion en curso con urls.get hacia este mismo servidor.
                // Las siguientes (el GET) no reciben respuesta.
                if (!marcarPost()) {
                    if (s.getInputStream().read() == -1) return;
                    return;
                }
                drenar(s);
                var cuerpo = "{\"id\":\"p1\",\"status\":\"starting\",\"urls\":{\"get\":\"http://127.0.0.1:"
                        + puertoHolder[0] + "/predictions/p1\"}}";
                var bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
                var out = s.getOutputStream();
                out.write(("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: " + bytes.length
                        + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
                out.write(bytes);
                out.flush();
            } catch (IOException e) {
                // el cliente cerro
            }
        });
        var g = generador("http://127.0.0.1:" + puertoHolder[0] + "/predictions");

        // 3 s de pausa antes del primer GET + 2 s de limite de lectura.
        long ms = medir(() -> assertThrows(AiProviderUnavailableException.class,
                () -> g.call("prompt", 1, 7L, (short) 1, 1)));

        assertTrue(ms >= 4_500 && ms < 10_000, "tardo " + ms + " ms");
    }

    private boolean postAtendido;

    private synchronized boolean marcarPost() {
        if (postAtendido) return false;
        postAtendido = true;
        return true;
    }

    @Test
    @DisplayName("respuesta normal dentro del limite: la salida del modelo llega igual que antes (control)")
    void respuestaNormal() throws IOException {
        int puerto = servidor(s -> {
            try {
                responder(s, "{\"id\":\"p1\",\"status\":\"succeeded\",\"output\":{\"json_output\":{\"ok\":1}}}");
            } catch (IOException e) {
                // nada
            }
        });
        var g = generador("http://127.0.0.1:" + puerto + "/predictions");
        assertEquals("{\"ok\":1}", g.call("prompt", 1, 7L, (short) 1, 1));
    }

    @Test
    @DisplayName("configuracion: valores por omision y acotados, coordinados con Prefer: wait y con el sondeo de 150 s")
    void configuracionAcotada() {
        var omision = new ReplicateProperties(true, "t", null, null, null, 0, 0);
        assertEquals(150, omision.timeoutSeconds(), "los 150 s del sondeo no cambian");
        assertEquals(10, omision.connectTimeoutSeconds());
        assertEquals(90, omision.readTimeoutSeconds(), "Prefer: wait (60) + 30");
        assertEquals(30, omision.pollReadTimeoutSeconds());

        var corto = new ReplicateProperties(true, "t", null, null, null, 150, 60, 0, 10, 1);
        assertEquals(62, corto.readTimeoutSeconds(), "nunca por debajo de lo que Replicate retiene el POST");
        assertEquals(2, corto.pollReadTimeoutSeconds());

        var largo = new ReplicateProperties(true, "t", null, null, null, 150, 60, 999, 9999, 999);
        assertEquals(60, largo.connectTimeoutSeconds());
        assertEquals(300, largo.readTimeoutSeconds());
        assertEquals(120, largo.pollReadTimeoutSeconds());

        // 10 + 90 + 150 + 3 + 10 + 30
        assertEquals(293, omision.esperaMaximaSegundos(), "la espera de una llamada tiene un maximo conocido");
    }
}
