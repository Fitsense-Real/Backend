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
 * Referencia regenerada con V47 (banderas de seguridad que el GIF exige) y V48 (dificultad conservadora) de la
 * auditoria integral del catalogo: 47 de 72 semanas cambian y todas siguen VALIDA, explicadas una a una en
 * {@code docs/FitSense/catalogo_integral/evidencia/DIFERENCIAS_REFERENCIA_V47_V48.md}. Regenerada de nuevo con V50
 * (clasificacion y equipamiento): 68 de 72 cambian y siguen VALIDA, en {@code .../DIFERENCIAS_REFERENCIA_V50.md}.
 * Antes: referencia oficial del catalogo V39 (V38 + V39, decisiones F y G del 30/10/2026) con el
 * motor de E1/E2. Las 72 diferencias frente a V37 estan revisadas en
 * {@code docs/FitSense/evidencia/v39/DIFERENCIAS_REFERENCIAS_V39.md}. Las anteriores se
 * conservan intactas: {@code generacion_referencia_v37_motor_v20.txt} (V37 con V20 exacto,
 * protegida tambien por {@link GeneracionReferenciaV37IT}) y
 * {@code generacion_referencia_v37_motor_e14da35.txt}. Las anteriores se conservan como evidencia en
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
        // Valores historicos fijados explicitamente al activar la configuracion evaluada (11/11/2026): esta
        // prueba se escribio sobre los valores por defecto anteriores y comprueba ese comportamiento.
        "fitsense.planning.policy=PLAN-HIST-1",
        "fitsense.ai.adaptation.prompt-version=v1",
        "fitsense.adaptation.progression.enabled=false",
        "fitsense.adaptation.difficulty-guard=off",
        "fitsense.adaptation.pain-localized-removal=off",
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
