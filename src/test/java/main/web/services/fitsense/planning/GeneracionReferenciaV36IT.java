package main.web.services.fitsense.planning;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.nio.file.Path;

/**
 * EVIDENCIA HISTORICA. Los planes iniciales de los 72 perfiles de referencia con el
 * catalogo V36 (Flyway solo hasta V36) siguen siendo identicos a
 * {@code docs/FitSense/evidencia/progresion/generacion_referencia_v36.txt}, la
 * referencia oficial hasta V37. Demuestra que el codigo de generacion no cambio
 * con V37: lo que cambia es el catalogo. La referencia vigente la protege
 * {@link GeneracionReferenciaIT}.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
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
        return Path.of("docs/FitSense/evidencia/progresion/generacion_referencia_v36.txt");
    }

    @Override
    String propiedadCaptura() {
        return "fitsense.generacion.v36.capturar";
    }
}
