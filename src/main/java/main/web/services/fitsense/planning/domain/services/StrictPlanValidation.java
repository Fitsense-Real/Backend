package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.planning.domain.model.valueobjects.ConservationReport;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanGenerationContext;

import java.util.List;

/**
 * La validacion completa de un borrador, vista por el productor de adaptacion.
 * <p>
 * POR QUE UN PUERTO Y NO {@code PlanDraftValidator} DIRECTO. Dos razones, y la
 * segunda es la importante.
 * <p>
 * La primera es que deja al productor probable sin montar el validador entero.
 * <p>
 * La segunda: <b>en OBSERVE, el incumplimiento de dosis de 4D se registra en el
 * log y se descarta</b>. {@code validateMvpDose} hace {@code log.warn(...)} y
 * {@code return} sin anadir nada a {@code problems}, asi que un validador en
 * OBSERVE NO puede decirle al productor que la dosis no cumple. Y la condicion
 * es que el camino de adaptacion no publique dosis incompatibles con 4D
 * <i>aunque V24 siga en OBSERVE</i>.
 * <p>
 * Por eso el puerto declara {@link #bloqueaDosis4D()} y el productor SE NIEGA a
 * trabajar con una implementacion que devuelva false. Asi el requisito no queda
 * en el cableado -donde fallaria en silencio publicando una dosis mala- sino en
 * una excepcion ruidosa la primera vez.
 */
public interface StrictPlanValidation {

    /**
     * Revisa el borrador y devuelve TODOS los problemas, sin parar en el
     * primero. Lista vacia significa que pasa.
     * <p>
     * No lanza: el productor necesita juntar estos problemas con los de V8C
     * para registrarlos como evidencia, y una excepcion perderia los otros.
     *
     * @param conservacion el informe de V8C, que V20 necesita para saber que
     *                     sesiones pueden quedar por debajo del minimo.
     */
    List<String> revisar(PlanDraft draft, PlanGenerationContext context,
                         ConservationReport conservacion);

    /**
     * Si esta implementacion rechaza -no solo registra- una dosis que no cumple
     * 4D. El camino de adaptacion exige true.
     */
    boolean bloqueaDosis4D();

    /**
     * Si exige la V24 por capacidades (D-P11). Entonces V8C tiene que emitir
     * las reducciones autorizadas, o rechazaria las de fatiga.
     */
    default boolean validaCapacidades() {
        return false;
    }
}
