package main.web.services.fitsense.iam;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * B2.4 · OPCION B, PREPARADA Y NO ACTIVADA ({@code fitsense.study.plans-require-participation=false}): cualquier
 * cuenta activa recibe planes; la retirada del estudio no quita el servicio; la participacion no cambia sola y los
 * analisis siguen filtrando por {@code is_study_participant}.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.study.plans-require-participation=false"
})
@DisplayName("P0-04 · opcion B (preparada): planes para no participantes y retirados, fuera del analisis")
class ParticipacionOpcionBIT extends ParticipacionBase {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Test
    @DisplayName("no participante y retirado reciben plan inicial y entran en el cierre semanal")
    void todasLasCuentasActivas() {
        long participante = cuenta(true, false);
        long noParticipa = cuenta(false, false);
        long retirado = cuenta(true, true);

        assertEquals("COMPLETED", generar(participante));
        assertEquals("COMPLETED", generar(noParticipa));
        assertEquals("COMPLETED", generar(retirado));

        var cierre = iam.fetchActiveParticipantIdsInTimezone(ZONA);
        assertTrue(cierre.containsAll(java.util.List.of(participante, noParticipa, retirado)));
    }

    @Test
    @DisplayName("la participacion no se marca sola y la poblacion de analisis solo cuenta participantes")
    void analisisSoloParticipantes() {
        long noParticipa = cuenta(false, false);
        generar(noParticipa);
        assertFalse(jdbc.queryForObject("SELECT is_study_participant FROM users WHERE user_id = ?", Boolean.class,
                noParticipa));
        // La poblacion de AdminAnalyticsQueryService: is_study_participant AND role = 'USER'.
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM users WHERE user_id = ? AND is_study_participant"
                + " AND role = 'USER'", Integer.class, noParticipa));
    }
}
