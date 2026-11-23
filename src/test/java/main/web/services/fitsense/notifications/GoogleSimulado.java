package main.web.services.fitsense.notifications;

import com.google.api.client.http.LowLevelHttpRequest;
import com.google.api.client.http.LowLevelHttpResponse;
import com.google.api.client.testing.http.MockHttpTransport;
import com.google.api.client.testing.http.MockLowLevelHttpRequest;
import com.google.api.client.testing.http.MockLowLevelHttpResponse;

import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Google simulado para las pruebas de arranque: un {@code HttpTransport} que intercepta TODAS las llamadas del SDK
 * (token OAuth y API de FCM). Ninguna sale de la JVM. Tambien genera una credencial de cuenta de servicio SINTETICA
 * (clave RSA creada al vuelo) con la misma estructura que la real, sin ningun secreto real.
 */
public class GoogleSimulado extends MockHttpTransport {

    public static final String URL_TOKEN = "https://oauth2.googleapis.com/token";
    public static final String URL_FCM = "https://fcm.googleapis.com/v1/projects/kora-fitpe/messages:send";

    public record Llamada(String metodo, String url, String cuerpo) {}

    public final List<Llamada> llamadas = new CopyOnWriteArrayList<>();
    /** Respuesta de FCM: 200 con nombre, o un error con su codigo de FCM. */
    public volatile String errorFcm;

    @Override
    public LowLevelHttpRequest buildRequest(String metodo, String url) {
        return new MockLowLevelHttpRequest(url) {
            @Override
            public LowLevelHttpResponse execute() throws java.io.IOException {
                llamadas.add(new Llamada(metodo, url, getContentAsString()));
                var r = new MockLowLevelHttpResponse().setContentType("application/json");
                if (url.startsWith(URL_TOKEN))
                    return r.setStatusCode(200).setContent(
                            "{\"access_token\":\"token-sintetico\",\"expires_in\":3600,\"token_type\":\"Bearer\"}");
                if (url.startsWith(URL_FCM)) {
                    if (errorFcm == null)
                        return r.setStatusCode(200).setContent("{\"name\":\"projects/kora-fitpe/messages/0:1\"}");
                    return r.setStatusCode(404).setContent("""
                            {"error":{"code":404,"message":"Requested entity was not found.","status":"NOT_FOUND",
                             "details":[{"@type":"type.googleapis.com/google.firebase.fcm.v1.FcmError","errorCode":"%s"}]}}"""
                            .formatted(errorFcm));
                }
                return r.setStatusCode(599).setContent("{\"error\":\"destino no previsto en la prueba\"}");
            }
        };
    }

    /** JSON de cuenta de servicio sintetica en Base64, como se entregaria en FITSENSE_FIREBASE_CREDENTIALS_BASE64. */
    public static String credencialSinteticaBase64() {
        try {
            var g = KeyPairGenerator.getInstance("RSA");
            g.initialize(2048);
            var clave = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(g.generateKeyPair().getPrivate().getEncoded());
            var pem = "-----BEGIN PRIVATE KEY-----\n" + clave + "\n-----END PRIVATE KEY-----\n";
            var json = """
                    {"type":"service_account","project_id":"kora-fitpe","private_key_id":"0000sintetica",
                     "private_key":"%s",
                     "client_email":"push-sintetico@kora-fitpe.iam.gserviceaccount.com","client_id":"000000000000000000000",
                     "auth_uri":"https://accounts.google.com/o/oauth2/auth","token_uri":"%s"}"""
                    .formatted(pem.replace("\n", "\\n"), URL_TOKEN);
            return Base64.getEncoder().encodeToString(json.getBytes());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
