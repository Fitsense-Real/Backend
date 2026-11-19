package main.web.services.fitsense.iam.infrastructure.notification;

import main.web.services.fitsense.shared.infrastructure.mail.CorreoSaliente;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Texto del correo de recuperacion. Lleva el enlace (si hay plantilla) o el codigo, la caducidad y el aviso de que
 * nunca se pide la contrasena por correo. Nunca lleva la contrasena: el backend no la conoce en claro al enviarlo.
 */
final class MensajeDeRecuperacion {

    static final String MARCADOR = "{token}";
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm 'UTC'");

    private MensajeDeRecuperacion() {
    }

    /** Vacia: el correo lleva solo el codigo. Si no, https (o http://localhost para pruebas) y con {token}. */
    static Optional<String> validarPlantilla(String plantilla) {
        if (plantilla == null || plantilla.isBlank()) return Optional.empty();
        String p = plantilla.trim();
        if (!p.contains(MARCADOR))
            throw new IllegalStateException("fitsense.password-reset.link-template debe contener " + MARCADOR);
        var uri = URI.create(p.replace(MARCADOR, "x"));
        boolean local = "http".equals(uri.getScheme()) && "localhost".equals(uri.getHost());
        if (!"https".equals(uri.getScheme()) && !local)
            throw new IllegalStateException("fitsense.password-reset.link-template debe usar https");
        return Optional.of(p);
    }

    static CorreoSaliente.Mensaje crear(String para, String token, OffsetDateTime caduca, Optional<String> enlace,
                                        String app) {
        String accion = enlace
                .map(e -> "Para elegir una contrasena nueva, abre este enlace:\n\n" + e.replace(MARCADOR, token))
                .orElse("Para elegir una contrasena nueva, introduce este codigo en la app:\n\n" + token);
        String texto = """
                Hola:

                Hemos recibido una solicitud para restablecer la contrasena de tu cuenta de %s.

                %s

                Caduca el %s y solo sirve una vez.

                Si no lo has pedido tu, ignora este mensaje: tu contrasena no cambia.
                Nunca te pediremos tu contrasena por correo.
                """.formatted(app, accion, HORA.format(caduca.withOffsetSameInstant(ZoneOffset.UTC)));
        return new CorreoSaliente.Mensaje("RECUPERACION_CONTRASENA", para,
                "%s: restablece tu contrasena".formatted(app), texto);
    }
}
