package main.web.services.fitsense.planning.infrastructure.generation.ai;

import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * R-3 · EL CLIENTE HTTP DE REPLICATE, CON LIMITES EXPLICITOS (resiliencia de Replicate, 11/11/2026).
 * <p>
 * Antes, {@code RestClient.builder()} sin fabrica usaba el cliente del JDK sin limite de conexion ni de lectura.
 * Ahora cada cliente tiene:
 * <ul>
 *   <li><b>conexion</b>: {@code fitsense.ai.connect-timeout-seconds} (10 s);</li>
 *   <li><b>lectura</b>: el tiempo maximo SIN RECIBIR DATOS en el socket. Para el POST,
 *       {@code fitsense.ai.read-timeout-seconds} (Prefer: wait + 30 s = 90 s); para el sondeo y la cancelacion,
 *       {@code fitsense.ai.poll-read-timeout-seconds} (30 s).</li>
 * </ul>
 * Se usa {@link SimpleClientHttpRequestFactory} ({@code HttpURLConnection}, sin dependencias nuevas) y no el cliente
 * del JDK con limite de peticion, porque ese limite solo cubre hasta recibir las cabeceras: un cuerpo que se queda a
 * medias seguiria esperando sin fin. Con un limite de lectura del socket, una respuesta parcial que se interrumpe
 * tambien termina. Lo que no acota ninguno de los dos es un servidor que manda un byte cada pocos segundos para
 * siempre; el sondeo, que es la parte larga, si tiene limite total ({@code fitsense.ai.timeout-seconds}).
 * <p>
 * Respeta el proxy y el almacen de certificados de la JVM ({@code https.proxyHost}, {@code javax.net.ssl.*}).
 */
final class ReplicateHttp {

    private ReplicateHttp() {
    }

    static ClientHttpRequestFactory fabrica(int conexionSegundos, int lecturaSegundos) {
        var f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(Duration.ofSeconds(conexionSegundos));
        f.setReadTimeout(Duration.ofSeconds(lecturaSegundos));
        return f;
    }

    /** El cliente del POST: la lectura espera lo que Replicate retiene la peticion, y un margen. */
    static RestClient.Builder post(ReplicateProperties p) {
        return RestClient.builder().requestFactory(fabrica(p.connectTimeoutSeconds(), p.readTimeoutSeconds()));
    }

    /** El cliente del sondeo y de la cancelacion: peticiones cortas. */
    static RestClient.Builder sondeo(ReplicateProperties p) {
        return RestClient.builder().requestFactory(fabrica(p.connectTimeoutSeconds(), p.pollReadTimeoutSeconds()));
    }
}
