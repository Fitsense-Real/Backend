package main.web.services.fitsense.shared.infrastructure.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Locale;

/**
 * Elige el transporte con {@code fitsense.mail.transport}:
 * <ul>
 *   <li>{@code none} (por omision): no se envia nada; recuperacion y eliminacion web quedan NO operativas.</li>
 *   <li>{@code smtp}: {@code spring.mail.host/port/username/password} (JavaMailSender). Railway Hobby bloquea el SMTP
 *       saliente.</li>
 *   <li>{@code resend} / {@code brevo}: API HTTPS (puerto 443), con {@code fitsense.mail.api-key}.</li>
 * </ul>
 * Una configuracion incompleta impide el arranque: es mejor fallar visible que perder correos en silencio. La clave
 * nunca va al log ni al repositorio: se lee de la variable de entorno.
 */
@Configuration
public class CorreoConfiguracion {

    private static final Logger log = LoggerFactory.getLogger(CorreoConfiguracion.class);

    @Bean
    CorreoSaliente correoSaliente(@Value("${fitsense.mail.transport:none}") String transporte,
                                  @Value("${fitsense.mail.from:}") String desde,
                                  @Value("${fitsense.mail.from-name:Kora}") String nombre,
                                  @Value("${fitsense.mail.api-key:}") String apiKey,
                                  @Value("${fitsense.mail.api-base-url:}") String baseUrl,
                                  @Value("${fitsense.mail.timeout-seconds:10}") long timeoutSegundos,
                                  ObjectProvider<JavaMailSender> smtp) {
        String t = transporte.trim().toLowerCase(Locale.ROOT);
        if (t.equals("none")) {
            log.info("CORREO transporte=none: no se envian correos (recuperacion y eliminacion web no operativas)");
            return TransportesDeCorreo.ninguno();
        }
        if (!TransportesDeCorreo.direccionValida(desde))
            throw new IllegalStateException("fitsense.mail.from debe ser una direccion valida con transport=" + t);
        var remitente = new TransportesDeCorreo.Remitente(desde.trim(), nombre);
        var timeout = Duration.ofSeconds(timeoutSegundos);
        CorreoSaliente correo = switch (t) {
            case "smtp" -> {
                var sender = smtp.getIfAvailable();
                if (sender == null)
                    throw new IllegalStateException("fitsense.mail.transport=smtp requiere spring.mail.host");
                yield TransportesDeCorreo.smtp(sender, remitente);
            }
            case "resend", "brevo" -> {
                if (apiKey == null || apiKey.isBlank())
                    throw new IllegalStateException("fitsense.mail.transport=" + t + " requiere fitsense.mail.api-key"
                            + " (variable de entorno FITSENSE_MAIL_API_KEY)");
                var base = URI.create(baseUrl == null || baseUrl.isBlank()
                        ? (t.equals("resend") ? "https://api.resend.com" : "https://api.brevo.com") : baseUrl.trim());
                if (!"https".equals(base.getScheme()) && !esLocal(base))
                    throw new IllegalStateException("fitsense.mail.api-base-url debe ser https");
                var http = HttpClient.newBuilder().connectTimeout(timeout).build();
                yield t.equals("resend")
                        ? TransportesDeCorreo.resend(http, base, apiKey.trim(), remitente, timeout)
                        : TransportesDeCorreo.brevo(http, base, apiKey.trim(), remitente, timeout);
            }
            default -> throw new IllegalStateException(
                    "fitsense.mail.transport debe ser none, smtp, resend o brevo (era " + transporte + ")");
        };
        log.info("CORREO transporte={} remitente configurado", correo.transporte());
        return correo;
    }

    @Bean
    EnvioDeCorreo envioDeCorreo(CorreoSaliente correo) {
        return new EnvioDeCorreo(correo);
    }

    /** Solo para pruebas con un servidor local simulado. */
    static boolean esLocal(URI u) {
        return "http".equals(u.getScheme()) && ("localhost".equals(u.getHost()) || "127.0.0.1".equals(u.getHost()));
    }
}
