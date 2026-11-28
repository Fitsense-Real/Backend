package main.web.services.fitsense.shared.infrastructure.logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("S-1 · DetalleSensible: forma tecnica por omision; detalle solo fuera de prod")
class DetalleSensibleTest {

    @AfterEach
    void restaurar() {
        DetalleSensible.fijar(false);
    }

    @Test
    @DisplayName("por omision: longitud y huella, nunca el texto; recuentos en vez de ids")
    void formaTecnica() {
        DetalleSensible.fijar(false);
        String salida = "{\"plan\":\"dato personal\"}";
        String r = DetalleSensible.texto(salida);
        assertFalse(r.contains("dato personal"), r);
        assertTrue(r.startsWith("caracteres=" + salida.length() + " huella="), r);
        assertEquals(12, r.substring(r.indexOf("huella=") + 7).length());
        assertEquals("n=3", DetalleSensible.ids(List.of(1, 2, 3)));
        assertEquals("n=1", DetalleSensible.mapa(Map.of(1L, 2L)));
        assertEquals("[omitido]", DetalleSensible.valor(1054L));
        assertEquals("caracteres=0", DetalleSensible.texto(null));
    }

    @Test
    @DisplayName("la huella es estable (permite cotejar con el libro de predicciones) y distinta para otro texto")
    void huella() {
        assertEquals(DetalleSensible.huella("abc"), DetalleSensible.huella("abc"));
        assertNotEquals(DetalleSensible.huella("abc"), DetalleSensible.huella("abd"));
    }

    @Test
    @DisplayName("con detalle permitido: el texto y los ids completos")
    void detalle() {
        DetalleSensible.fijar(true);
        assertEquals("x", DetalleSensible.texto("x"));
        assertEquals(List.of(1, 2), DetalleSensible.ids(List.of(1, 2)));
        assertEquals(7L, DetalleSensible.valor(7L));
    }

    @Test
    @DisplayName("prod + fitsense.logging.sensitive-detail=true: la aplicacion no arranca")
    void prodNoAdmiteDetalle() {
        var prod = new MockEnvironment();
        prod.setActiveProfiles("prod");
        var e = assertThrows(IllegalStateException.class, () -> new DetalleSensibleConfiguracion(true, prod));
        assertTrue(e.getMessage().contains("prod"));
        assertFalse(DetalleSensible.permitido());
        new DetalleSensibleConfiguracion(false, prod);
        assertFalse(DetalleSensible.permitido());
    }

    @Test
    @DisplayName("fuera de prod (local con datos sinteticos) se puede activar")
    void localAdmiteDetalle() {
        var local = new MockEnvironment();
        local.setActiveProfiles("local-dev");
        new DetalleSensibleConfiguracion(true, local);
        assertTrue(DetalleSensible.permitido());
    }
}
