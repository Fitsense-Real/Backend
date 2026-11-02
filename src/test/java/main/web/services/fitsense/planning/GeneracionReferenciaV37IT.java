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
 * El catalogo V37 (Flyway solo hasta la 37) con el motor VIGENTE (V20 exacto, D1 y E1/E2).
 * Compara contra {@code generacion_referencia_v37_motor_v20.txt}: la referencia oficial hasta
 * V38 + V39, generada con el motor de V20 exacto. Demuestra que D1 y E1/E2 no cambian nada con
 * el catalogo V37: lo que cambia en V39 es por el catalogo. El archivo esta ademas protegido
 * por su huella en {@code ReferenciasHistoricasInmutablesTest}.
 */
@Testcontainers
@Import(EsquemaVigenteSobreCatalogoAnterior.class)
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "spring.flyway.target=37"
})
class GeneracionReferenciaV37IT extends GeneracionReferenciaBase {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Override
    Path referencia() {
        return Path.of("docs/FitSense/evidencia/progresion/generacion_referencia_v37_motor_v20.txt");
    }

    @Override
    String propiedadCaptura() {
        return "fitsense.generacion.v37.capturar";
    }
}
