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
 * El catalogo V40 (Flyway solo hasta la 40) con el motor VIGENTE. Compara contra
 * {@code generacion_referencia_v40.txt}: la referencia oficial hasta V41, congelada el
 * 30/10/2026 (auditoria integral). Demuestra que lo que cambia con V41 es por el catalogo. El archivo
 * esta ademas protegido por su huella en {@code ReferenciasHistoricasInmutablesTest}.
 */
@Testcontainers
@Import(EsquemaVigenteSobreCatalogoAnterior.class)
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "spring.flyway.target=40"
})
class GeneracionReferenciaV40IT extends GeneracionReferenciaBase {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Override
    Path referencia() {
        return Path.of("docs/FitSense/evidencia/progresion/generacion_referencia_v40.txt");
    }

    @Override
    String propiedadCaptura() {
        return "fitsense.generacion.v40.capturar";
    }
}
