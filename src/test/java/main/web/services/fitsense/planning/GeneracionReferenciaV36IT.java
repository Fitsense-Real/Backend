package main.web.services.fitsense.planning;

import main.web.services.fitsense.EsquemaVigenteSobreCatalogoAnterior;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.nio.file.Path;

/**
 * El catalogo V36 (Flyway solo hasta la 36) con el motor VIGENTE, que incluye V20
 * exacto en segundos (IMPLEMENTACION_V20.md). Compara contra
 * {@code docs/FitSense/evidencia/progresion/generacion_referencia_v36_motor_v20.txt}.
 * <p>
 * NO es la salida historica. La original, {@code generacion_referencia_v36.txt},
 * se conserva intacta (la reproducia el motor anterior a V20 exacto, commit e14da35) y
 * su huella la protege {@code ReferenciasHistoricasInmutablesTest}. Que cambio por
 * el motor y que por el catalogo: {@code docs/FitSense/evidencia/progresion/REFERENCIAS.md}.
 */
@Testcontainers
@Import(EsquemaVigenteSobreCatalogoAnterior.class)
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        // Valores historicos fijados explicitamente al activar la configuracion evaluada (11/11/2026): esta
        // prueba se escribio sobre los valores por defecto anteriores y comprueba ese comportamiento.
        "fitsense.planning.policy=PLAN-HIST-1",
        "fitsense.ai.adaptation.prompt-version=v1",
        "fitsense.adaptation.progression.enabled=false",
        "fitsense.adaptation.difficulty-guard=off",
        "fitsense.adaptation.pain-localized-removal=off",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "spring.flyway.target=36"
})
class GeneracionReferenciaV36IT extends GeneracionReferenciaBase {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Override
    Path referencia() {
        return Path.of("docs/FitSense/evidencia/progresion/generacion_referencia_v36_motor_v20.txt");
    }

    @Override
    String propiedadCaptura() {
        return "fitsense.generacion.v36.capturar";
    }
}
