package main.web.services.fitsense.shared.domain.model.valueobjects;

import main.web.services.fitsense.adaptation.domain.model.valueobjects.AdaptationOutcome;
import main.web.services.fitsense.adaptation.domain.model.valueobjects.AdjustmentContext;
import main.web.services.fitsense.adaptation.domain.services.AdaptationOrderDecider;
import main.web.services.fitsense.analytics.interfaces.acl.SessionOutcomeView;
import main.web.services.fitsense.configuration.domain.model.valueobjects.CalculationParams;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * La regla de evidencia, y que los dos sitios que la usan no se separen.
 * <p>
 * La historia de esta clase es la razon de que exista: la condicion estuvo
 * escrita dos veces y una de las copias olvidaba exigir cumplimiento observado,
 * de modo que con una cobertura minima del 0 % una semana sin registros pasaba
 * el umbral. Se unifico. Despues volvio a escribirse una tercera vez en
 * {@code AdaptationOrderDecider}. La prueba de abajo es lo que detectaria una
 * cuarta.
 */
class EvidenceRuleTest {

    private static final LocalDate LUNES = LocalDate.of(2026, 10, 12);

    @Test
    @DisplayName("Sin cumplimiento observado no hay evidencia, ni con el umbral en 0")
    void sinObservadaNoHayEvidencia() {
        assertFalse(EvidenceRule.enoughEvidence(null, cifra("0.00"), 0.0));
        assertFalse(EvidenceRule.enoughEvidence(null, cifra("100.00"), 0.0));
        assertFalse(EvidenceRule.enoughEvidence(null, null, 0.0));
    }

    @Test
    @DisplayName("Sin cobertura calculada no hay evidencia")
    void sinCoberturaNoHayEvidencia() {
        assertFalse(EvidenceRule.enoughEvidence(cifra("100.00"), null, 0.0));
    }

    @Test
    @DisplayName("La cobertura se compara con el umbral, y el limite entra")
    void elLimiteEntra() {
        assertTrue(EvidenceRule.enoughEvidence(cifra("50.00"), cifra("50.00"), 50.0));
        assertFalse(EvidenceRule.enoughEvidence(cifra("50.00"), cifra("49.99"), 50.0));
        assertTrue(EvidenceRule.enoughEvidence(cifra("0.00"), cifra("66.67"), 50.0));
    }

    @Test
    @DisplayName("Un cumplimiento observado de 0 SI es evidencia: significa que no entreno")
    void ceroObservadoEsEvidencia() {
        assertTrue(EvidenceRule.enoughEvidence(cifra("0.00"), cifra("100.00"), 50.0),
                "tres omisiones declaradas son adherencia real, no falta de datos");
    }

    @Test
    @DisplayName("ACUERDO: el decisor dice INSUFFICIENT_DATA exactamente cuando la regla dice no")
    void elDecisorYLaReglaNoSeSeparan() {
        var decider = new AdaptationOrderDecider();
        var observadas = List.of("0.00", "35.00", "65.00", "100.00");
        var coberturas = List.of("0.00", "33.33", "50.00", "66.67", "100.00");
        var umbrales = List.of(0.0, 50.0, 100.0);

        int comprobadas = 0;
        var desacuerdos = new ArrayList<String>();

        for (var obs : observadas) {
            for (var cob : coberturas) {
                for (var min : umbrales) {
                    // Tambien con observada nula, que es el caso que se escapo.
                    for (boolean nula : List.of(false, true)) {
                        BigDecimal observada = nula ? null : cifra(obs);
                        var contexto = contexto(observada, cifra(cob));

                        boolean reglaDiceSi = EvidenceRule.enoughEvidence(
                                observada, cifra(cob), min);
                        boolean decisorDiceSi = decider.decide(contexto, params(), min)
                                .outcome() != AdaptationOutcome.INSUFFICIENT_DATA;

                        comprobadas++;
                        // El decisor puede decir INSUFFICIENT_DATA con evidencia
                        // suficiente si no hay ninguna sesion senalada, pero en
                        // este contexto siempre hay una, asi que el unico motivo
                        // posible es la evidencia. Si se separan, es un defecto.
                        if (reglaDiceSi != decisorDiceSi)
                            desacuerdos.add("observada=" + observada + " cobertura=" + cob
                                    + " minimo=" + min + ": regla=" + reglaDiceSi
                                    + " decisor=" + decisorDiceSi);
                    }
                }
            }
        }

        assertEquals(120, comprobadas, "la rejilla no es la esperada");
        assertTrue(desacuerdos.isEmpty(),
                "la regla y el decisor se separaron en " + desacuerdos.size()
                        + " casos: " + desacuerdos);
    }

    // -----------------------------------------------------------------------

    /** Una semana con una sesion senalada, para que lo unico en juego sea la evidencia. */
    private static AdjustmentContext contexto(BigDecimal observada, BigDecimal cobertura) {
        var senalada = new SessionOutcomeView(2L, LUNES.plusDays(2), "SKIPPED", true,
                BigDecimal.ZERO, "LACK_OF_TIME", (short) 6, true);
        return new AdjustmentContext(
                observada, null, 480, 480, 3, 45, 2, null, cifra("6.5"),
                List.of(senalada), 2, 3, observada, cobertura);
    }

    private static BigDecimal cifra(String v) {
        return new BigDecimal(v);
    }

    private static CalculationParams params() {
        var adherence = new CalculationParams.Adherence(80.0, 30.0, 80.0, 100.0,
                "weighted_adherence_pct", "LAST_FINISHED", 7);
        var adjustment = new CalculationParams.Adjustment(80.0, 50.0, 20.0, 30.0, 10.0,
                5.0, 5.0, 15.0, 20.0, 40.0, 5.0, 1, 2, 20, 2, 2, 6, 20, 1, 1, 20.0, 8.0);
        return new CalculationParams(adherence, adjustment, null,
                new CalculationParams.Dropout(14), null);
    }
}
