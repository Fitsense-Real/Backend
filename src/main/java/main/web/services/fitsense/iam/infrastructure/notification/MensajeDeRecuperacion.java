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
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' HH:mm 'UTC'");

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

    /**
     * C2 · texto claro para quien lo recibe: en castellano con tildes (los tres transportes envian UTF-8), cuanto dura
     * el codigo ademas de la hora en UTC (las personas del estudio no viven en UTC) y que hacer con un codigo de 43
     * caracteres (copiar y pegar, no teclear). Sigue sin llevar la contrasena ni ningun dato de la cuenta.
     */
    static CorreoSaliente.Mensaje crear(String para, String token, OffsetDateTime caduca, Optional<String> enlace,
                                        String app, long minutos) {
        String accion = enlace
                .map(e -> "Para elegir una contraseña nueva, abre este enlace desde el teléfono donde usas la app:\n\n"
                        + e.replace(MARCADOR, token))
                .orElse("Para elegir una contraseña nueva, copia este código y pégalo en la app,"
                        + " en «Restablecer contraseña»:\n\n" + token);
        String texto = """
                Hola:

                Recibimos una solicitud para restablecer la contraseña de tu cuenta de %s.

                %s

                Caduca en %d minutos (el %s) y solo sirve una vez. Si pediste más de un código, solo vale el del correo
                más reciente. Al cambiar la contraseña se cierran las sesiones abiertas en otros dispositivos.

                Si no lo pediste tú, ignora este mensaje: tu contraseña no cambia.
                Nunca te pediremos tu contraseña ni este código por correo, teléfono o mensaje.
                """.formatted(app, accion, minutos, HORA.format(caduca.withOffsetSameInstant(ZoneOffset.UTC)));
        return new CorreoSaliente.Mensaje("RECUPERACION_CONTRASENA", para,
                "%s: código para restablecer tu contraseña".formatted(app), texto);
    }
}
