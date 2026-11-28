package main.web.services.fitsense.planning.infrastructure.generation.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

import static main.web.services.fitsense.planning.infrastructure.generation.ai.SessionFixPromptV2Test.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Rediseno R/R' (P-A1, P-A2): CON LA POLITICA HISTORICA EL PROMPT DE ADAPTACION NO CAMBIA NI UN BYTE. Las huellas
 * se tomaron del codigo de {@code be825f9} (antes de tocar el prompt) para los seis escenarios de referencia (U03,
 * U04, U08, U09, U18, U19) y las tres versiones del prompt (v1, v2, v2.1). Cualquier cambio de texto con la politica
 * historica rompe esta prueba.
 */
@DisplayName("Rediseno: con la politica historica el prompt de adaptacion es identico byte a byte")
class PromptHistoricoHuellaTest {

    static final SessionFixPromptBuilder V21 = new SessionFixPromptBuilder(JSON, "v2.1");

    static Map<String, String> huellas() {
        var m = new LinkedHashMap<String, String>();
        var escenarios = new LinkedHashMap<String, Escenario>();
        escenarios.put("U03", u03());
        escenarios.put("U04", u04());
        escenarios.put("U08", u08());
        escenarios.put("U09", u09());
        escenarios.put("U18", u18());
        escenarios.put("U19", u19());
        for (var e : escenarios.entrySet()) {
            m.put(e.getKey() + "/v1", sha(texto(V1, e.getValue(), AiRetryFeedback.none())));
            m.put(e.getKey() + "/v2", sha(texto(V2, e.getValue(), AiRetryFeedback.none())));
            m.put(e.getKey() + "/v2.1", sha(texto(V21, e.getValue(), AiRetryFeedback.none())));
        }
        return m;
    }

    static String sha(String s) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(s.getBytes(StandardCharsets.UTF_8))).substring(0, 16);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** Huellas de be825f9 (base del rediseno). */
    static final Map<String, String> ESPERADAS = Map.ofEntries(
            Map.entry("U03/v1", "882c439b22014199"), Map.entry("U03/v2", "c8eed09f1e45ab5f"),
            Map.entry("U03/v2.1", "4ca9025ce3ade719"), Map.entry("U04/v1", "ef6d44186dec7304"),
            Map.entry("U04/v2", "ce7f84c6e14013d7"), Map.entry("U04/v2.1", "1a907998d578aedc"),
            Map.entry("U08/v1", "05b17eb1272e0ce1"), Map.entry("U08/v2", "b2e481f7e496c3ac"),
            Map.entry("U08/v2.1", "cf111823fd45a044"), Map.entry("U09/v1", "f0d4e37ea9eb18dd"),
            Map.entry("U09/v2", "fc8856188d90ef57"), Map.entry("U09/v2.1", "a461a3bd475e4a6a"),
            Map.entry("U18/v1", "be3e3c1f466c9669"), Map.entry("U18/v2", "fab647261afceb04"),
            Map.entry("U18/v2.1", "397970274bfd8809"), Map.entry("U19/v1", "56db2c020617ceea"),
            Map.entry("U19/v2", "2f231663c39eec21"), Map.entry("U19/v2.1", "27b6597fcd5f1c9c"));

    @Test
    @DisplayName("las 18 huellas (6 escenarios x 3 versiones) coinciden con las de la base")
    void identico() {
        var h = huellas();
        h.forEach((k, v) -> System.out.println("HUELLA " + k + " " + v));
        assertEquals(ESPERADAS, h);
    }
}
