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
 * Referencia oficial desde V37 (aprobada por el responsable del proyecto,
 * 29/10/2026), regenerada con V20 exacto en segundos (aprobado el 30/10/2026: 13
 * perfiles cambian, IMPLEMENTACION_V20.md §2.4). La anterior, la del motor de e14da35,
 * se conserva como {@code generacion_referencia_v37_motor_e14da35.txt}. Las anteriores se conservan como evidencia en
 * {@link GeneracionReferenciaV36IT} y {@link GeneracionReferenciaV35IT}; las
 * sesiones que cambiaron estan explicadas en {@code docs/FitSense/CATALOGO_V37.md}
 * y {@code docs/FitSense/CATALOGO_V36.md}.
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
