package main.web.services.fitsense.planning;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.nio.file.Path;

/**
 * Referencia CANDIDATA de los 72 perfiles con un catalogo propuesto (no la
 * oficial). Opt-in: {@code -Dfitsense.candidata.referencia=<archivo>}; con
 * {@code -Dfitsense.candidata.capturar=true} la escribe en vez de compararla.
 * <p>
 * El catalogo se elige con Flyway desde la linea de ordenes, sin tocar
 * db/migration; por ejemplo, la propuesta V37 de la auditoria:
 * {@code -Dspring.flyway.locations=classpath:db/migration,filesystem:docs/FitSense/auditoria_catalogo/v37/migracion}.
 * La referencia oficial ({@link GeneracionReferenciaIT}) no se toca.
 */
@Testcontainers
@EnabledIfSystemProperty(named = "fitsense.candidata.referencia", matches = ".+")
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
class GeneracionReferenciaCandidataIT extends GeneracionReferenciaBase {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Override
    Path referencia() {
        return Path.of(System.getProperty("fitsense.candidata.referencia"));
    }

    @Override
    String propiedadCaptura() {
        return "fitsense.candidata.capturar";
    }
}
