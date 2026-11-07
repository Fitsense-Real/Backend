package main.web.services.fitsense.orchestation;

import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link CatalogoInactivoIT} con la politica HISTORICA (por defecto): su comportamiento no cambia. Sin
 * {@code reparaCatalogoInactivo}, un heredado desactivado en el catalogo no tiene causa de sustitucion y la copia
 * siguiente falla V1: NO_FACTIBLE, con el motivo V1 registrado, y nada inactivo publicado. Es la limitacion L7, que la
 * fase 4 solo corrige en PLAN-REV-2.0; extenderla a la historica requiere decision (cambiaria la politica congelada).
 */
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
@DisplayName("Fase 4 · ejercicio desactivado en el catalogo con un plan publicado (HISTORICA: sin cambios)")
class CatalogoInactivoHistIT extends CatalogoInactivoIT {

    @Override
    void esperadoConSustituto(Optional<Long> w2, Map<String, Object> ultimo, Fila x, List<List<Long>> antes, long w1) {
        historica(w2, ultimo);
    }

    @Override
    void esperadoSinSustituto(Optional<Long> w2, Map<String, Object> ultimo) {
        historica(w2, ultimo);
    }

    @Override
    void esperadoConIa(Optional<Long> w2, Map<String, Object> ultimo, Fila x) {
        // Con IA la ultima palabra la tiene la continuidad segura: tampoco publica el inactivo.
        assertTrue(w2.isEmpty() || !String.valueOf(ultimo.get("informe")).contains("SUST-CAT-1"), String.valueOf(ultimo));
        sinInactivosPublicados();
    }

    void historica(Optional<Long> w2, Map<String, Object> ultimo) {
        assertTrue(w2.isEmpty(), "historica: la copia con un inactivo no sale " + ultimo);
        assertEquals("NO_FACTIBLE", ultimo.get("status"), String.valueOf(ultimo));
        assertTrue(String.valueOf(ultimo.get("rejection_reason")).startsWith("V1"), String.valueOf(ultimo));
        sinInactivosPublicados();
    }
}
