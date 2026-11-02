package main.web.services.fitsense.orchestation;

import main.web.services.fitsense.planning.domain.model.valueobjects.PlanningPolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Ciclo real de 10 semanas con la politica PLAN-HIST-1 (opt-in {@code -Dfitsense.implementacion.c2d=true}). */
@Testcontainers
@EnabledIfSystemProperty(named = "fitsense.implementacion.c2d", matches = "true")
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.adaptation.progression.enabled=true",
        "fitsense.planning.policy=PLAN-HIST-1",
        "fitsense.planning.policy.allow-candidates=false"
})
@DisplayName("Ciclo real 10 semanas · PLAN-HIST-1")
class CicloRealHistoricaIT extends CicloRealPoliticaBase {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Override
    String jdbcUrl() {
        return postgres.getJdbcUrl();
    }

    @Override
    PlanningPolicy esperada() {
        return PlanningPolicy.HISTORICA;
    }

    @Override
    String archivo() {
        return "LONGITUDINAL_PLAN-HIST-1.csv";
    }
}
