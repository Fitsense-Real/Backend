package main.web.services.fitsense.notifications.infrastructure;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import main.web.services.fitsense.notifications.domain.MensajePush;
import main.web.services.fitsense.notifications.domain.TransportePush;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Base64;

/**
 * Transporte de notificaciones push.
 * <p>
 * <b>Credenciales, solo en el servidor</b> (nunca en el repositorio), en este orden:
 * <ol>
 *   <li>{@code FITSENSE_FIREBASE_CREDENTIALS_BASE64}: el JSON de la cuenta de servicio en Base64, como secreto del
 *       entorno (p. ej. variable secreta de Railway);</li>
 *   <li>{@code FITSENSE_FIREBASE_CREDENTIALS_FILE}: ruta a un archivo montado fuera del repositorio;</li>
 *   <li>{@code GOOGLE_APPLICATION_CREDENTIALS} (Application Default Credentials), SOLO si la variable esta definida: sin
 *       ella no se sondea el servidor de metadatos de Google Cloud, que retrasaria o colgaria el arranque.</li>
 * </ol>
 * Si el envio esta apagado ({@code fitsense.push.enabled=false}, el valor por omision) o no hay credenciales, el
 * transporte queda NO disponible: la aplicacion arranca igual, los dispositivos se pueden registrar y ningun proceso
 * de entrenamiento depende de Firebase.
 */
@Configuration
public class PushConfiguracion {

    private static final Logger log = LoggerFactory.getLogger(PushConfiguracion.class);
    static final String NOMBRE_APP = "fitsense-push";

    @Bean
    TransportePush transportePush(@Value("${fitsense.push.enabled:false}") boolean activado,
                                  @Value("${fitsense.push.firebase.project-id:kora-fitpe}") String proyecto,
                                  @Value("${fitsense.push.android.package:pe.fit.kora}") String paquete,
                                  @Value("${fitsense.push.credentials-base64:}") String base64,
                                  @Value("${fitsense.push.credentials-file:}") String archivo,
                                  @Value("${fitsense.push.ttl-hours:24}") long ttlHoras) {
        if (!activado) {
            log.info("Notificaciones push apagadas (fitsense.push.enabled=false): no se envia nada.");
            return new TransporteNoDisponible("apagado");
        }
        try {
            var credenciales = credenciales(base64, archivo);
            var opciones = FirebaseOptions.builder()
                    .setCredentials(credenciales)
                    .setProjectId(proyecto)
                    .setConnectTimeout(10_000)
                    .setReadTimeout(10_000)
                    .build();
            var app = FirebaseApp.getApps().stream().filter(a -> NOMBRE_APP.equals(a.getName())).findFirst()
                    .orElseGet(() -> FirebaseApp.initializeApp(opciones, NOMBRE_APP));
            log.info("Notificaciones push activas: proyecto {}, paquete {}, destino por FID.", proyecto, paquete);
            return new FirebaseTransportePush(FirebaseMessaging.getInstance(app), paquete, Duration.ofHours(ttlHoras));
        } catch (IOException | RuntimeException e) {
            // Sin el detalle de la excepcion: podria citar parte de la credencial.
            log.error("Notificaciones push activadas pero sin credenciales validas ({}): no se envia nada.",
                    e.getClass().getSimpleName());
            return new TransporteNoDisponible("sin credenciales");
        }
    }

    static GoogleCredentials credenciales(String base64, String archivo) throws IOException {
        if (base64 != null && !base64.isBlank())
            try (InputStream in = new ByteArrayInputStream(Base64.getDecoder().decode(base64.trim()))) {
                return GoogleCredentials.fromStream(in);
            }
        if (archivo != null && !archivo.isBlank())
            try (InputStream in = new FileInputStream(archivo)) {
                return GoogleCredentials.fromStream(in);
            }
        var adc = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
        if (adc != null && !adc.isBlank()) return GoogleCredentials.getApplicationDefault();
        throw new IOException("Sin credenciales de Firebase configuradas");
    }

    /** Transporte apagado: nunca envia y el emisor no reclama eventos. */
    public record TransporteNoDisponible(String motivo) implements TransportePush {
        @Override
        public boolean disponible() {
            return false;
        }

        @Override
        public Resultado enviar(String installationId, MensajePush mensaje) {
            return Resultado.fallo(Tipo.ERROR_TEMPORAL, "TRANSPORTE_NO_DISPONIBLE");
        }
    }
}
