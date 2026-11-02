package main.web.services.fitsense.iam.infrastructure.notification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

/** dev-log escribe el codigo en el log: solo se admite en local; fuera de ahi la aplicacion no arranca. */
class PasswordResetNotifiersTest {

    private final PasswordResetNotifiers config = new PasswordResetNotifiers();

    private static MockEnvironment con(String... perfiles) {
        var e = new MockEnvironment();
        e.setActiveProfiles(perfiles);
        return e;
    }

    @Test
    @DisplayName("dev-log sin perfil, o con un perfil remoto, impide arrancar")
    void devLogFueraDeLocalNoArranca() {
        for (var perfiles : new String[][]{{}, {"prod"}, {"railway"}, {"default"}}) {
            var e = assertThrows(IllegalStateException.class, () -> config.registroDeDesarrollo(con(perfiles)));
            assertTrue(e.getMessage().contains("local-dev"), e.getMessage());
        }
    }

    @Test
    @DisplayName("dev-log con local o local-dev funciona y no se presenta como entrega real")
    void devLogEnLocal() {
        for (var perfil : new String[]{"local", "local-dev"}) {
            var n = config.registroDeDesarrollo(con(perfil));
            assertFalse(n.entregaReal());
            assertDoesNotThrow(() -> n.enviar("x@pruebas.invalid", "codigo", OffsetDateTime.now()));
        }
    }
}
