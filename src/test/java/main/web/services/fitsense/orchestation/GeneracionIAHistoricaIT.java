package main.web.services.fitsense.orchestation;

import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanningPolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Control con la politica por defecto (PLAN-HIST-1). En el pipeline real de generacion, el normalizador (B2) sustituye
 * los minutos declarados por los calculados, asi que la V4 historica TAMBIEN rechaza la propuesta, con su regla de
 * siempre (minutos redondeados frente a round(M x 1,15)) y su mensaje de siempre. La historica conserva su
 * comportamiento; lo que V4-C cambia en este camino es solo la brecha de redondeo (BrechaRedondeoV4Test).
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
@DisplayName("IA de generacion simulada con PLAN-HIST-1: la misma propuesta se rechaza por la V4 de siempre (minutos)")
class GeneracionIAHistoricaIT extends GeneracionIAV4CBase {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Test
    void historicaRechazaPorMinutosNormalizados() {
        assertSame(PlanningPolicy.HISTORICA, selector.activa());
        iaSimulada();
        long u = participante30("ia-hist@pruebas.invalid");
        long plan = planning.generateWeeklyPlan(u, SEMANA, PlanAdjustment.none(), null, null).orElseThrow();
        assertTrue(segundosPropuestos.get() > 2070, "la propuesta supera el techo de V4-C: " + segundosPropuestos.get());
        assertTrue(rechazos.stream().anyMatch(p -> p.startsWith("V4:") && p.contains("minutos y el techo es 35")),
                "V4 historica sobre los minutos normalizados: " + rechazos);
        assertTrue(rechazos.stream().noneMatch(p -> p.contains("segun su contenido")), "sin el texto de V4-C");
        var p = jdbc.queryForMap("SELECT generation_source, coalesce(planning_policy, 'NULL') pol FROM"
                + " weekly_training_plans WHERE plan_id = ?", plan);
        assertEquals("RULE_ENGINE", p.get("generation_source"));
        assertEquals("PLAN-HIST-1", p.get("pol"));
    }
}
