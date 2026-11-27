package main.web.services.fitsense.iam;

import main.web.services.fitsense.shared.domain.exceptions.DomainRuleViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * B2.4 · OPCION A, la configuracion POR OMISION (comportamiento de siempre): solo los participantes activos reciben
 * planes. Esta prueba documenta el comportamiento que la auditoria encontro (P0-04); que se mantenga es una decision
 * del equipo del estudio y del comite, y exige que el registro lo diga claramente.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
@DisplayName("P0-04 · opcion A (por omision): sin participar, ni plan inicial ni cierre semanal")
class ParticipacionOpcionAIT extends ParticipacionBase {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Test
    @DisplayName("no participante y participante retirado: 422 al pedir el plan; fuera del cierre semanal")
    void soloParticipantes() {
        long participante = cuenta(true, false);
        long noParticipa = cuenta(false, false);
        long retirado = cuenta(true, true);

        assertEquals("COMPLETED", generar(participante));
        assertThrows(DomainRuleViolationException.class, () -> operaciones.request(noParticipa));
        assertThrows(DomainRuleViolationException.class, () -> operaciones.request(retirado));

        var cierre = iam.fetchActiveParticipantIdsInTimezone(ZONA);
        assertTrue(cierre.contains(participante));
        assertFalse(cierre.contains(noParticipa));
        assertFalse(cierre.contains(retirado));
    }
}
