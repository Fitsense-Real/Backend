package main.web.services.fitsense.orchestation;

import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanningPolicy;
import main.web.services.fitsense.shared.infrastructure.json.JsonSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** PLAN-V4-0.1: la propuesta de la IA que declara 35 min con mas de 2.070 s estimados se rechaza por V4. */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.planning.policy=PLAN-V4-0.1",
        "fitsense.planning.policy.allow-candidates=true"
})
@DisplayName("IA de generacion simulada con PLAN-V4-0.1: 35 min declarados y > 2.070 s estimados -> V4 rechaza")
class GeneracionIAV4CIT extends GeneracionIAV4CBase {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Test
    @SuppressWarnings("unchecked")
    void rechazaYPublicaElRespaldo() {
        assertSame(PlanningPolicy.V4C, selector.activa());
        iaSimulada();
        long u = participante30("ia-v4c@pruebas.invalid");
        long plan = planning.generateWeeklyPlan(u, SEMANA, PlanAdjustment.none(), null, null).orElseThrow();
        assertTrue(segundosPropuestos.get() > 2070, "la propuesta supera el techo: " + segundosPropuestos.get());
        assertTrue(rechazos.stream().anyMatch(p -> p.startsWith("V4:") && p.contains("2070 s")),
                "V4-C rechaza la propuesta de la IA declare lo que declare: " + rechazos);
        var p = jdbc.queryForMap("SELECT generation_source, planning_policy, planning_policy_trace::text t FROM"
                + " weekly_training_plans WHERE plan_id = ?", plan);
        assertEquals("RULE_ENGINE", p.get("generation_source"), "se publica el respaldo, no la propuesta");
        assertEquals("PLAN-V4-0.1", p.get("planning_policy"));
        var t = new JsonSupport().read(String.valueOf(p.get("t")), Map.class);
        for (var s : (List<Map<String, Object>>) t.get("sesiones"))
            assertTrue(((Number) s.get("segundos_estimados")).longValue() <= 2070, s.toString());
    }
}
