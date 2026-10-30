package main.web.services.fitsense.planning;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * LAS REFERENCIAS HISTORICAS NO SE TOCAN. Son la salida de motores anteriores y deben
 * poder consultarse tal cual (decision del 30/10/2026, REFERENCIAS.md). Las pruebas de
 * regresion comparan contra las referencias del motor vigente ({@code *_motor_v20.txt}
 * y la oficial); estas solo se conservan, y su huella SHA-256 lo garantiza.
 */
@DisplayName("Referencias historicas de generacion: intactas")
class ReferenciasHistoricasInmutablesTest {

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            // catalogo V35, motor anterior a V20 exacto (reproducida por ultima vez en e14da35)
            "generacion_referencia_v35.txt, a32cbe7e6c38a1124100b11d5a0c420718698dc2d4851b85245ecd1a6710e9b3",
            // catalogo V36, mismo motor
            "generacion_referencia_v36.txt, 6f204c07e22a6d4322780710d748daa0730e29ca5781a246ea91f14c6a5fa94b",
            // catalogo V37, mismo motor: la oficial hasta V20 exacto
            "generacion_referencia_v37_motor_e14da35.txt, e46769755875be4dc99b439ecd3837b954b67ffc383221fd4168cb7b908d8d23",
            // catalogo V37, motor con V20 exacto (4a45221): la oficial hasta V38 + V39
            "generacion_referencia_v37_motor_v20.txt, 00951b534160a896c61ec6006e35d0e134be3eef215a3599efb409bc18b559e5",
    })
    void intacta(String archivo, String sha256) throws Exception {
        var bytes = Files.readAllBytes(Path.of("docs/FitSense/evidencia/progresion", archivo));
        assertEquals(sha256, HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)), archivo);
    }
}
