package main.web.services.fitsense.planning;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.nio.file.Path;

/**
 * REFERENCIA DE LA VERSION TECNICA CONSERVADORA ({@code FITSENSE-CATALOGO-TECNICO-CONSERVADOR-v1}). Los mismos 72
 * perfiles, la misma semana fija (semilla del motor de reglas, {@code RelojPruebas.SEMANA_REFERENCIA}) y la misma
 * politica historica que {@link GeneracionReferenciaIT}, con la contencion INC-CAT-CONS-v1 ENCENDIDA (el valor por
 * defecto) y el catalogo hasta V56. Compara contra {@code generacion_referencia_conservador_v1.txt}, guardada APARTE:
 * la referencia historica ({@code generacion_referencia.txt}) no se sobrescribe. Cada diferencia entre las dos esta
 * explicada plan a plan en {@code docs/FitSense/catalogo_integral/cierre_conservador/COMPARACION_72_PLANES.xlsx} y en
 * {@code REFERENCIA_CATALOGO_CONSERVADOR.md}.
 * <p>
 * Para regenerarla (solo con un cambio aprobado que cree una version nueva):
 * {@code -Dfitsense.generacion.conservador.capturar=true}.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.planning.policy=PLAN-HIST-1",
        "fitsense.ai.adaptation.prompt-version=v1",
        "fitsense.adaptation.progression.enabled=false",
        "fitsense.adaptation.difficulty-guard=off",
        "fitsense.adaptation.pain-localized-removal=off",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.catalogo.contencion-conservadora=true"
})
class GeneracionReferenciaConservadoraIT extends GeneracionReferenciaBase {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Override
    Path referencia() {
        return Path.of("docs/FitSense/evidencia/progresion/generacion_referencia_conservador_v1.txt");
    }

    @Override
    String propiedadCaptura() {
        return "fitsense.generacion.conservador.capturar";
    }
}
