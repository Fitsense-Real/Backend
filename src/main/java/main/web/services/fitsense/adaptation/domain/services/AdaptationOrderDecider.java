package main.web.services.fitsense.adaptation.domain.services;

import main.web.services.fitsense.adaptation.domain.model.valueobjects.AdaptationOrder;
import main.web.services.fitsense.adaptation.domain.model.valueobjects.AdaptationOutcome;
import main.web.services.fitsense.adaptation.domain.model.valueobjects.AdjustmentContext;
import main.web.services.fitsense.analytics.interfaces.acl.SessionOutcomeView;
import main.web.services.fitsense.configuration.domain.model.valueobjects.CalculationParams;
import main.web.services.fitsense.shared.domain.model.valueobjects.EvidenceRule;
import main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/**
 * Decide si se adapta, con que alcance y que se permite cambiar.
 * <p>
 * Sustituye la etapa 18.2 de {@code AdjustmentDecisionTable} -el porcentaje de
 * reduccion por tramo de adherencia- y conserva la idea de 18.3: la causa
 * dominante modifica la forma. La diferencia es que ahora la causa se lee POR
 * SESION y no agregada para toda la semana, que es lo que permite corregir solo
 * la parte que fallo.
 * <p>
 * LA SECUENCIA, en este orden y no en otro:
 * <ol>
 *   <li>¿Hay suficientes sesiones CON REGISTRO? Si no, INSUFFICIENT_DATA y se
 *       conserva. No se interpreta la falta de registro como adherencia baja.</li>
 *   <li>¿La adherencia OBSERVADA es adecuada? Si lo es, NO_CHANGE y se conserva.
 *       Sin progresion automatica.</li>
 *   <li>¿Que sesiones fallaron y por que? Las senala el identificador de
 *       analytics con los umbrales del instrumento.</li>
 *   <li>Localizada o ampliada, segun el tramo.</li>
 * </ol>
 * Usa la adherencia OBSERVADA, no la ponderada. La ponderada mete las sesiones
 * sin registro como ceros, asi que un participante que cumplio el 100 % de lo
 * que registro puede aparecer con un 66 % y recibir una adaptacion que no
 * necesita. La ponderada sigue siendo la metrica primaria del estudio y no se
 * toca; para DECIDIR se usa lo observado.
 */
@Service
public class AdaptationOrderDecider {

    /**
     * @param coberturaMinimaPct cobertura de registro exigida. Entra como
     *                           parametro porque es una decision metodologica
     *                           PROVISIONAL, pendiente de aprobacion, y no debe
     *                           quedar escrita en el dominio. Su sitio
     *                           definitivo es la configuracion de calculo.
     */
    public AdaptationOrder decide(AdjustmentContext context, CalculationParams params,
                                  double coberturaMinimaPct) {

        var limits = params.adjustment();
        var observada = context.observedAdherencePct();
        var cobertura = context.registrationCoveragePct();
        int registrados = context.recordedWorkouts();
        int previstos = context.scheduledWorkouts();

        // 1. Evidencia. UNA SOLA REGLA, la de shared: la condicion estaba
        //    escrita aqui otra vez, que es justo lo que EvidenceRule existe
        //    para evitar. Sin un solo registro, observada es NULL y su
        //    ausencia niega la evidencia por bajo que se configure el umbral.
        if (!EvidenceRule.enoughEvidence(observada, cobertura, coberturaMinimaPct)) {
            return AdaptationOrder.sinCambios(AdaptationOutcome.INSUFFICIENT_DATA,
                    observada, cobertura, registrados, previstos,
                    "Cobertura de registro insuficiente: %s de %d sesiones con registro, "
                            .formatted(registrados, previstos)
                            + "minimo exigido %.0f %%.".formatted(coberturaMinimaPct));
        }

        // 2. Adherencia adecuada: se conserva. Y NO se progresa.
        if (observada.doubleValue() >= limits.goodThresholdPct()) {
            return AdaptationOrder.sinCambios(AdaptationOutcome.NO_CHANGE,
                    observada, cobertura, registrados, previstos,
                    "Adherencia observada %s %% sobre un umbral de %.0f %%: se conserva."
                            .formatted(observada, limits.goodThresholdPct()));
        }

        // 3. Las sesiones senaladas, de peor a mejor cumplimiento.
        var senaladas = context.sessionOutcomes() == null ? List.<SessionOutcomeView>of()
                : context.sessionOutcomes().stream()
                .filter(SessionOutcomeView::problematic)
                .sorted(Comparator
                        .comparing((SessionOutcomeView s) -> s.completionPct() == null
                                ? BigDecimal.ZERO : s.completionPct())
                        .thenComparing(SessionOutcomeView::scheduledDate,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        // Adherencia baja y ninguna sesion senalada no deberia ocurrir: si el
        // cumplimiento observado no llega al umbral, al menos una sesion no se
        // completo. Si ocurre, es un desacuerdo entre el calculo semanal y el
        // identificador, y conservar es mas seguro que adaptar a ciegas.
        if (senaladas.isEmpty()) {
            return AdaptationOrder.sinCambios(AdaptationOutcome.INSUFFICIENT_DATA,
                    observada, cobertura, registrados, previstos,
                    "Adherencia observada %s %% por debajo del umbral pero ninguna sesion "
                            .formatted(observada)
                            + "quedo senalada. No se adapta a ciegas.");
        }

        // 4. El alcance. Localizada toca UNA sesion; ampliada, todas.
        boolean ampliada = observada.doubleValue() < limits.moderateThresholdPct();
        var objetivo = ampliada ? senaladas : List.of(senaladas.get(0));

        var sesiones = objetivo.stream()
                .map(s -> new AdaptationOrder.TargetedSession(
                        s.plannedWorkoutId(), s.scheduledDate(),
                        s.completionPct(), s.skipReason(), s.sessionRpe(),
                        TipoIntervencion.deMotivo(s.skipReason())))
                .toList();

        return new AdaptationOrder(
                ampliada ? AdaptationOutcome.ADAPT_EXPANDED : AdaptationOutcome.ADAPT_LOCALIZED,
                sesiones, observada, cobertura, registrados, previstos,
                "Adherencia observada %s %% (cobertura %s %%). %s"
                        .formatted(observada, cobertura,
                                ampliada
                                        ? "Por debajo de %.0f %%: se abarcan las %d sesiones senaladas."
                                        .formatted(limits.moderateThresholdPct(), sesiones.size())
                                        : "Entre %.0f %% y %.0f %%: se corrige solo la peor sesion."
                                        .formatted(limits.moderateThresholdPct(),
                                                limits.goodThresholdPct())));
    }
}
