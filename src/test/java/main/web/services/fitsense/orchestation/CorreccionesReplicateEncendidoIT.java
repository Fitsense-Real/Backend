package main.web.services.fitsense.orchestation;

import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Las cinco correcciones con la progresion ENCENDIDA (y con ella la V24 por capacidades), solo en esta base aislada. */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.adaptation.progression.enabled=true"
})
@DisplayName("Correcciones tras Replicate real · progresion encendida")
class CorreccionesReplicateEncendidoIT extends CorreccionesReplicateBase {
}
