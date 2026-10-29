package main.web.services.fitsense.planning;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.nio.file.Path;

/**
 * PROTECCION DE LA GENERACION INICIAL. Los planes iniciales de los 72 perfiles de
 * referencia (4 objetivos x 3 niveles x 30/45/60 min x gimnasio/casa), con el
 * catalogo vigente, deben ser identicos -ejercicios, dosis, estructura y resultado
 * de la validacion- a {@code docs/FitSense/evidencia/progresion/generacion_referencia.txt}.
 * <p>
 * Referencia oficial desde V36 (promovida por decision del responsable del
 * proyecto, 28/10/2026). La anterior se conserva como evidencia en
 * {@link GeneracionReferenciaV35IT}; las 213 sesiones que cambiaron estan
 * explicadas en {@code docs/FitSense/CATALOGO_V36.md}.
 * <p>
 * Para regenerarla (solo si se aprueba un cambio de la generacion):
 * {@code -Dfitsense.generacion.capturar=true}.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
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
