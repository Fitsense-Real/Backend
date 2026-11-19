package main.web.services.fitsense.orchestation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.adaptation.difficulty-guard=off"
})
@DisplayName("Dolor en el 135 sin sustituto de igual o menor dificultad, de extremo a extremo (limite off)")
class SinSustitutoDolorLimiteApagadoIT extends SinSustitutoDolorBase {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Test
    @DisplayName("el 135 no vuelve a publicarse en ninguna semana y el ciclo termina")
    void flujo() throws Exception {
        var r = recorrer();
        System.out.println("FLUJO_135 " + r);
        assertFalse(v8c.limitaDificultad());
        // HOY: SUST-SEG-1 sustituye el 135 por uno MAS DIFICIL y la semana se publica (respaldo P-03 nivel 1).
        assertEquals(true, r.get("plan_semana_nueva"), String.valueOf(r));
        assertNotNull(r.get("dificultad_sustituto"), String.valueOf(r));
        assertTrue((Integer) r.get("dificultad_sustituto") > 1, "comportamiento actual documentado: " + r);
    }
}
