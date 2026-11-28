package main.web.services.fitsense.shared.interfaces.rest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("C8 · los mensajes de error no llevan el id interno de la cuenta")
class MensajesSinIdInternoTest {

    @Test
    @DisplayName("los mensajes de dominio anteriores quedan sin el id; los demas no cambian")
    void sinId() {
        assertEquals("El usuario ya tiene un plan activo para la semana del 2026-11-30.",
                GlobalExceptionHandler.sinIdInterno("El usuario 5 ya tiene un plan activo para la semana del 2026-11-30."));
        assertEquals("Perfil del usuario no encontrado", GlobalExceptionHandler.sinIdInterno("Perfil del usuario no encontrado: 5"));
        assertEquals("Usuario no encontrado", GlobalExceptionHandler.sinIdInterno("Usuario no encontrado: 42"));
        assertEquals("No hay ejercicios elegibles para el usuario. Revisa el equipamiento declarado.",
                GlobalExceptionHandler.sinIdInterno("No hay ejercicios elegibles para el usuario 7. Revisa el equipamiento declarado."));
        assertEquals("El plan activo del usuario para la semana del 2026-11-30 cambio",
                GlobalExceptionHandler.sinIdInterno("El plan activo del usuario 12 para la semana del 2026-11-30 cambio"));
        // Otros recursos que nombra la propia peticion: su id se queda.
        assertEquals("Plan no encontrado: 123", GlobalExceptionHandler.sinIdInterno("Plan no encontrado: 123"));
        assertEquals("Faltan 3 sesiones", GlobalExceptionHandler.sinIdInterno("Faltan 3 sesiones"));
        assertNull(GlobalExceptionHandler.sinIdInterno(null));
    }
}
