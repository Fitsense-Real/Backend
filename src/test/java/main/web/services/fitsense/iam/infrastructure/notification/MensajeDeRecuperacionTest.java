package main.web.services.fitsense.iam.infrastructure.notification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Correo de recuperacion: enlace o codigo, caducidad, sin contrasena")
class MensajeDeRecuperacionTest {

    static final OffsetDateTime CADUCA = OffsetDateTime.parse("2026-11-19T21:30:00-05:00");

    @Test
    @DisplayName("sin plantilla: el codigo; con plantilla: el enlace; vigencia en minutos y hora en UTC; con tildes")
    void contenido() {
        var codigo = MensajeDeRecuperacion.crear("p@pruebas.invalid", "TOKEN_abc-1", CADUCA, Optional.empty(), "Kora", 30);
        assertTrue(codigo.texto().contains("pégalo en la app, en «Restablecer contraseña»:\n\nTOKEN_abc-1"));
        assertTrue(codigo.texto().contains("Caduca en 30 minutos (el 20/11/2026 a las 02:30 UTC)"));
        assertTrue(codigo.texto().contains("se cierran las sesiones"));
        assertTrue(codigo.texto().contains("solo vale el del correo"), "C6: solo el codigo mas reciente");
        assertEquals("Kora: código para restablecer tu contraseña", codigo.asunto());
        assertEquals("RECUPERACION_CONTRASENA", codigo.tipo());
        var enlace = MensajeDeRecuperacion.crear("p@pruebas.invalid", "TOKEN_abc-1", CADUCA,
                Optional.of("https://web.pruebas.invalid/restablecer?token={token}"), "Kora", 30);
        assertTrue(enlace.texto().contains("https://web.pruebas.invalid/restablecer?token=TOKEN_abc-1"));
        assertTrue(enlace.texto().contains("Nunca te pediremos tu contraseña ni este código"));
    }

    @Test
    @DisplayName("nunca lleva la contrasena ni el destinatario en el cuerpo")
    void sinDatosDeLaCuenta() {
        var m = MensajeDeRecuperacion.crear("persona@pruebas.invalid", "TOKEN_abc-1", CADUCA, Optional.empty(), "Kora", 30);
        assertFalse(m.texto().contains("persona@pruebas.invalid"));
        assertFalse(m.texto().toLowerCase().contains("contraseña actual"));
    }

    @Test
    @DisplayName("plantilla: vacia vale; exige {token} y https (http solo en localhost)")
    void plantilla() {
        assertEquals(Optional.empty(), MensajeDeRecuperacion.validarPlantilla(" "));
        assertThrows(IllegalStateException.class,
                () -> MensajeDeRecuperacion.validarPlantilla("https://web.pruebas.invalid/restablecer"));
        assertThrows(IllegalStateException.class,
                () -> MensajeDeRecuperacion.validarPlantilla("http://web.pruebas.invalid/r?t={token}"));
        assertTrue(MensajeDeRecuperacion.validarPlantilla("http://localhost:8080/r?t={token}").isPresent());
    }
}
