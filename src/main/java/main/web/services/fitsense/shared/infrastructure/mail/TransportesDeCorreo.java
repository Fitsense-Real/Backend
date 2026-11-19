package main.web.services.fitsense.shared.infrastructure.mail;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.internet.InternetAddress;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/** Implementaciones de {@link CorreoSaliente}. Se eligen y validan en {@link CorreoConfiguracion}. */
public final class TransportesDeCorreo {

    private TransportesDeCorreo() {
    }

    private static final ObjectMapper JSON = new ObjectMapper();

    /** Remitente: direccion y nombre visible. */
    public record Remitente(String direccion, String nombre) {
        String formateado() {
            return nombre == null || nombre.isBlank() ? direccion : "%s <%s>".formatted(nombre, direccion);
        }
    }

    /** Sin proveedor: no se envia nada. */
    public static CorreoSaliente ninguno() {
        return new CorreoSaliente() {
            @Override
            public void enviar(Mensaje mensaje) {
                throw new CorreoNoEnviadoException("sin proveedor de correo (fitsense.mail.transport=none)");
            }

            @Override
            public String transporte() {
                return "none";
            }

            @Override
            public boolean entregaReal() {
                return false;
            }
        };
    }

    /** SMTP con el JavaMailSender de Spring (spring.mail.*). En Railway Hobby el SMTP saliente esta bloqueado. */
    public static CorreoSaliente smtp(JavaMailSender sender, Remitente remitente) {
        return new CorreoSaliente() {
            @Override
            public void enviar(Mensaje m) {
                var correo = new SimpleMailMessage();
                correo.setFrom(remitente.formateado());
                correo.setTo(m.para());
                correo.setSubject(m.asunto());
                correo.setText(m.texto());
                try {
                    sender.send(correo);
                } catch (RuntimeException e) {
                    throw new CorreoNoEnviadoException("SMTP: " + e.getClass().getSimpleName());
                }
            }

            @Override
            public String transporte() {
                return "smtp";
            }

            @Override
            public boolean entregaReal() {
                return true;
            }
        };
    }

    /** API HTTPS de Resend: POST {base}/emails con Authorization: Bearer. */
    public static CorreoSaliente resend(HttpClient http, URI base, String apiKey, Remitente remitente,
                                        Duration timeout) {
        return new ApiHttps("resend", http, timeout) {
            @Override
            HttpRequest peticion(Mensaje m) {
                return HttpRequest.newBuilder(base.resolve("/emails")).timeout(timeout)
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json(Map.of(
                                "from", remitente.formateado(), "to", List.of(m.para()),
                                "subject", m.asunto(), "text", m.texto()))))
                        .build();
            }
        };
    }

    /** API HTTPS de Brevo: POST {base}/v3/smtp/email con la cabecera api-key. */
    public static CorreoSaliente brevo(HttpClient http, URI base, String apiKey, Remitente remitente,
                                       Duration timeout) {
        return new ApiHttps("brevo", http, timeout) {
            @Override
            HttpRequest peticion(Mensaje m) {
                var sender = remitente.nombre() == null || remitente.nombre().isBlank()
                        ? Map.of("email", remitente.direccion())
                        : Map.of("email", remitente.direccion(), "name", remitente.nombre());
                return HttpRequest.newBuilder(base.resolve("/v3/smtp/email")).timeout(timeout)
                        .header("api-key", apiKey)
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json(Map.of(
                                "sender", sender, "to", List.of(Map.of("email", m.para())),
                                "subject", m.asunto(), "textContent", m.texto()))))
                        .build();
            }
        };
    }

    private abstract static class ApiHttps implements CorreoSaliente {
        private final String nombre;
        private final HttpClient http;

        ApiHttps(String nombre, HttpClient http, Duration timeout) {
            this.nombre = nombre;
            this.http = http;
        }

        abstract HttpRequest peticion(Mensaje m);

        @Override
        public void enviar(Mensaje m) {
            HttpResponse<String> r;
            try {
                r = http.send(peticion(m), HttpResponse.BodyHandlers.ofString());
            } catch (IOException e) {
                throw new CorreoNoEnviadoException(nombre + ": " + e.getClass().getSimpleName());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new CorreoNoEnviadoException(nombre + ": interrumpido");
            }
            // El cuerpo de error del proveedor puede repetir el destinatario: no se propaga, solo el codigo.
            if (r.statusCode() / 100 != 2) throw new CorreoNoEnviadoException(nombre + ": HTTP " + r.statusCode());
        }

        @Override
        public String transporte() {
            return nombre;
        }

        @Override
        public boolean entregaReal() {
            return true;
        }
    }

    static String json(Object o) {
        try {
            return JSON.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Valida una direccion de correo (remitente de la configuracion). */
    static boolean direccionValida(String d) {
        if (d == null || d.isBlank()) return false;
        try {
            new InternetAddress(d, true).validate();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
