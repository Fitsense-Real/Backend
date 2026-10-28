package main.web.services.fitsense.planning;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.nio.file.Path;

/**
 * REFERENCIA CANDIDATA tras V36. Los mismos 72 perfiles que
 * {@link GeneracionReferenciaIT}, con todas las migraciones (catalogo
 * corregido), contra
 * {@code docs/FitSense/evidencia/catalogo_v36/generacion_referencia_candidata_v36.txt}.
 * <p>
 * NO sustituye la referencia oficial: la convivencia de las dos permite revisar
 * el cambio antes de aprobarlo. Si se aprueba, la candidata pasa a oficial con
 * una decision explicita, no automaticamente.
 * <p>
 * Para regenerar la candidata: {@code -Dfitsense.generacion.capturar.v36=true}.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
class GeneracionReferenciaCandidataV36IT extends GeneracionReferenciaBase {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Override
    Path referencia() {
        return Path.of("docs/FitSense/evidencia/catalogo_v36/generacion_referencia_candidata_v36.txt");
    }

    @Override
    String propiedadCaptura() {
        return "fitsense.generacion.capturar.v36";
    }
}
