package main.web.services.fitsense.orchestation;

import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link LinajeIntegridadIT} con {@code PLAN-REV-2.0} (cierre tecnico, fase 3): los puntos A-E y 1-3 se ejecutan tal
 * cual; solo cambia el punto F, cuya premisa es exclusivamente historica.
 * <p>
 * F historico: sin el intento V32 que publico el acortado, la copia no hereda la exencion de V20 y V20 (piso del 70 %)
 * la rechaza. Con REV2 no hay piso del 70 % (P-G1 aprobado): la exencion no hace falta y la copia se publica. Lo que el
 * punto protege -que una derivacion sin evidencia NO se trate como autorizada- se comprueba de otra forma: la copia
 * conserva EXACTAMENTE el acortado (nada se restaura sin evidencia), y la consulta de integridad sigue detectando la
 * derivacion huerfana del plan cuyo intento se borro.
 */
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.planning.policy=PLAN-REV-2.0",
        "fitsense.planning.policy.allow-candidates=true"
})
@DisplayName("REV2 · el linaje V31 + V32 se clasifica sin ambiguedad; sin evidencia, se detecta y no se restaura")
class LinajeIntegridadRev2IT extends LinajeIntegridadIT {

    @Override
    void puntoF(long e, long previo, long e2) throws Exception {
        configurarIa(true);
        int acortado = duracionViernes(previo);
        assertEquals(1, jdbc.update("DELETE FROM plan_adaptation_attempts WHERE resulting_plan_id = ?", e2));
        ejecutar(e, previo, Dia.HECHO, Dia.HECHO, Dia.HECHO);
        var siguiente = ciclo.runFor(e, S1.plusWeeks(4), S1.plusWeeks(5));
        var ultimo = jdbc.queryForMap("SELECT status, rejection_reason FROM plan_adaptation_attempts WHERE user_id = ?"
                + " ORDER BY attempt_id DESC LIMIT 1", e);
        System.out.println("LINAJE_REV2 d8_sin_evidencia " + ultimo + " plan=" + siguiente);
        assertTrue(siguiente.isPresent(), "REV2 no tiene piso del 70 %: la copia se publica " + ultimo);
        assertEquals(acortado, duracionViernes(siguiente.get()), "sin evidencia nada se restaura: el acortado se conserva");
        var violaciones = jdbc.queryForList(consultas()[0]);
        assertTrue(violaciones.stream().anyMatch(r -> "PLAN_DERIVADO_SIN_INTENTO_UNICO".equals(r.get("violacion"))
                && ((Number) r.get("id")).longValue() == e2), "la derivacion sin evidencia se sigue detectando: " + violaciones);
    }
}
