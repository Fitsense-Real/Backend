package main.web.services.fitsense.planning;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.nio.file.Path;

/**
 * EVIDENCIA HISTORICA. Los planes iniciales de los 72 perfiles de referencia con el
 * catalogo ANTERIOR a V36 (Flyway solo hasta V35) siguen siendo identicos a
 * {@code docs/FitSense/evidencia/progresion/generacion_referencia_v35.txt}, la
 * referencia oficial hasta V36, capturada antes de implementar la progresion.
 * Demuestra que el codigo de generacion no cambio con V36: lo que cambia es el
 * catalogo. La referencia vigente la protege {@link GeneracionReferenciaIT}.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "spring.flyway.target=35"
})
class GeneracionReferenciaV35IT extends GeneracionReferenciaBase {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Override
    Path referencia() {
        return Path.of("docs/FitSense/evidencia/progresion/generacion_referencia_v35.txt");
    }

    @Override
    String propiedadCaptura() {
        return "fitsense.generacion.capturar.v35";
    }
}
