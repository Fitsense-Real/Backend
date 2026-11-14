package main.web.services.fitsense.catalogo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.Arrays;
import java.util.List;

/** V53-B · medicion opt-in sobre la rejilla de 168 ({@link MedicionPermisoApoyoSuspendidoBase}), modo OPCION_B. */
@Testcontainers
@EnabledIfSystemProperty(named = "fitsense.v53b.medicion", matches = "true")
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.seguridad.permiso-apoyo-suspendido=true"
})
@DisplayName("V53-B · medicion de la rejilla de 168, modo OPCION_B (opt-in)")
class MedicionPermisoApoyoSuspendidoOpcionBIT extends MedicionPermisoApoyoSuspendidoBase {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Override
    String modo() {
        return "OPCION_B";
    }

    @Override
    List<Boolean> permisos() {
        return Arrays.asList(null, Boolean.TRUE, Boolean.FALSE);
    }
}
