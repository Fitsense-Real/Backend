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
    @DisplayName("sin plantilla: el codigo; con plantilla: el enlace; caducidad en UTC")
    void contenido() {
        var codigo = MensajeDeRecuperacion.crear("p@pruebas.invalid", "TOKEN_abc-1", CADUCA, Optional.empty(), "Kora");
        assertTrue(codigo.texto().contains("introduce este codigo en la app:\n\nTOKEN_abc-1"));
        assertTrue(codigo.texto().contains("20/11/2026 02:30 UTC"));
        assertEquals("Kora: restablece tu contrasena", codigo.asunto());
        assertEquals("RECUPERACION_CONTRASENA", codigo.tipo());
        var enlace = MensajeDeRecuperacion.crear("p@pruebas.invalid", "TOKEN_abc-1", CADUCA,
                Optional.of("https://web.pruebas.invalid/restablecer?token={token}"), "Kora");
        assertTrue(enlace.texto().contains("https://web.pruebas.invalid/restablecer?token=TOKEN_abc-1"));
        assertTrue(enlace.texto().contains("Nunca te pediremos tu contrasena por correo."));
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
