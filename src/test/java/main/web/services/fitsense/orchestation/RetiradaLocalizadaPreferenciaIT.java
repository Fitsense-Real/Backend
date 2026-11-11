package main.web.services.fitsense.orchestation;

import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static main.web.services.fitsense.profiling.domain.model.valueobjects.FitnessLevel.*;
import static main.web.services.fitsense.profiling.domain.model.valueobjects.GoalType.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * RETIRADA LOCALIZADA ANTE DOLOR con el limite C ({@code preferencia}) y la retirada encendida (configuracion D), solo
 * en esta prueba. Cuatro perfiles congelados, en orden de id: la retirada valida (R0521), la que V20 rechaza y P-03
 * resuelve (R0487), la que V20 rechaza y P-03 tampoco (R0524) y la del 135 tambien en una sesion no senalada (R0545).
 */
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        // Valores historicos fijados explicitamente al activar la configuracion evaluada (11/11/2026): esta
        // prueba se escribio sobre los valores por defecto anteriores y comprueba ese comportamiento.
        "fitsense.planning.policy=PLAN-HIST-1",
        "fitsense.ai.adaptation.prompt-version=v1",
        "fitsense.adaptation.progression.enabled=false",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.adaptation.difficulty-guard=preferencia",
        "fitsense.adaptation.pain-localized-removal=on"
})
@DisplayName("Retirada localizada ante dolor (limite + preferencia C + retirada on): valida, rechazada por V20, por P-03 y por sesion no senalada")
class RetiradaLocalizadaPreferenciaIT extends RetiradaLocalizadaBase {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Test
    @Order(1)
    @DisplayName("R0487: retirar el 135 deja la sesion por debajo de V20 -> se rechaza y P-03 publica la regeneracion")
    void v20RechazaYP03Publica() {
        assertTrue(producer.retiradaLocalizada());
        var e = escenario("R0487", LOSE_WEIGHT, INTERMEDIATE, 6, 60);
        assertRetiradaRechazada(e, "V20");
        assertEquals("PUBLICADO", e.p03().get(0).get("outcome"));
        assertTrue(e.plan().isPresent());
        assertEquals("REGENERACION_SEGURA_REGLAS", resultadoApi(e.usuario()));
    }

    @Test
    @Order(2)
    @DisplayName("R0521: la retirada pasa V8C y la validacion estricta -> se publica, solo sin el 135; idempotente; semana siguiente")
    void retiradaValida() {
        var e = escenario("R0521", GENERAL_FITNESS, INTERMEDIATE, 6, 30);
        assertRetiradaPublicada(e);
        long plan = e.plan().get();
        // Idempotencia: volver a correr el ciclo de esa semana no publica otro plan.
        ciclo.runFor(e.usuario(), MED, NUE);
        assertEquals(1, activos(e.usuario(), NUE));
        assertEquals(plan, planActivo(e.usuario(), NUE).orElseThrow());
        assertSemanaSiguiente(e);
    }

    @Test
    @Order(3)
    @DisplayName("R0524: la retirada no pasa V20 y P-03 tampoco puede publicar -> NO_FACTIBLE, sin plan invalido")
    void v20RechazaYP03NoFactible() {
        var e = escenario("R0524", GENERAL_FITNESS, INTERMEDIATE, 6, 75);
        assertRetiradaRechazada(e, "V20");
        assertEquals("NO_FACTIBLE", e.p03().get(0).get("outcome"));
        assertTrue(e.plan().isEmpty());
        assertEquals("NO_FACTIBLE_POR_SEGURIDAD", resultadoApi(e.usuario()));
    }

    @Test
    @Order(4)
    @DisplayName("R0545: el 135 esta tambien en una sesion no senalada -> no se retira (no se toca lo no senalado)")
    void tambienEnNoSenalada() {
        var e = escenario("R0545", GAIN_MUSCLE, INTERMEDIATE, 7, 30);
        assertEquals(2, e.sesiones135().size());
        assertRetiradaRechazada(e, "no se senalo por dolor");
    }
}
