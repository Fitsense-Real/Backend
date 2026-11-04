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
        "fitsense.adaptation.difficulty-guard=preferencia"
})
@DisplayName("Dolor en el 135 sin sustituto de igual o menor dificultad, de extremo a extremo (limite + preferencia, C)")
class SinSustitutoDolorLimitePreferenciaIT extends SinSustitutoDolorBase {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Test
    @DisplayName("el 135 no vuelve a publicarse en ninguna semana y el ciclo termina")
    void flujo() throws Exception {
        var r = recorrer();
        System.out.println("FLUJO_135 " + r);
        assertTrue(v8c.limitaDificultad());
        assertEquals(main.web.services.fitsense.planning.domain.services.SafetySubstitutionPolicy.Estrategia
                .LIMITE_Y_PREFERENCIA, v8c.estrategiaSustitucion());
        // CON LIMITE: ningun sustituto del 135 puede ser mas dificil; si se publica una copia sustituida, su
        // sustituto es de dificultad <= 1. Si no hay sustituto, la semana no sale por esa via (NO_FACTIBLE) y
        // P-03 nivel 2 regenera o deja NO_FACTIBLE: en ningun caso se publica el 135.
        if (r.get("dificultad_sustituto") != null)
            assertTrue((Integer) r.get("dificultad_sustituto") <= 1, String.valueOf(r));
    }
}
