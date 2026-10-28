package main.web.services.fitsense.planning;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.nio.file.Path;

/**
 * PROTECCION DE LA GENERACION INICIAL. Los planes iniciales de los 72 perfiles
 * de referencia deben ser identicos a los guardados en
 * {@code docs/FitSense/evidencia/progresion/generacion_referencia.txt}, que se
 * capturaron ANTES de implementar la progresion.
 * <p>
 * La referencia oficial es la del catalogo ANTERIOR a V36, y por eso esta
 * prueba aplica Flyway solo hasta V35: demuestra que el codigo de generacion
 * no cambio. El efecto de V36 lo mide {@link GeneracionReferenciaCandidataV36IT}
 * contra una referencia candidata aparte, sin sustituir esta.
 * <p>
 * Para regenerar la referencia (solo si se aprueba un cambio de la generacion):
 * {@code -Dfitsense.generacion.capturar=true}.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "spring.flyway.target=35"
})
class GeneracionReferenciaIT extends GeneracionReferenciaBase {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Override
    Path referencia() {
        return Path.of("docs/FitSense/evidencia/progresion/generacion_referencia.txt");
    }

    @Override
    String propiedadCaptura() {
        return "fitsense.generacion.capturar";
    }
}
