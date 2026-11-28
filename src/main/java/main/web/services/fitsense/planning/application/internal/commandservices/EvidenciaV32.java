package main.web.services.fitsense.planning.application.internal.commandservices;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import main.web.services.fitsense.planning.domain.model.aggregates.PlanAdaptationAttempt;
import main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion;
import main.web.services.fitsense.shared.infrastructure.json.JsonSupport;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Lectura de la evidencia V32 de UN paso del linaje (V31): en que clase quedo
 * la sesion de origen y que dice el informe de conservacion. La comparten los
 * recorridos de D8, de las dosis heredadas y de la progresion, para que los
 * tres clasifiquen igual.
 */
final class EvidenciaV32 {

    /** La clase de la sesion de origen en el intento que publico la derivada. */
    enum Clase { APLICADA, SIN_EFECTO, CONSERVADA, PROGRESADA }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Informe(List<Ref> sustituciones, List<Paso> pasos) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        record Ref(Long sesionOrigen) {}

        @JsonIgnoreProperties(ignoreUnknown = true)
        record Paso(Long origen, String tipo, Long exerciseId, Dosis antes, Dosis despues, Integer volumenAntes,
                    Integer volumenDespues, Integer volumenBase, Integer techo, String estadoReserva) {}

        @JsonIgnoreProperties(ignoreUnknown = true)
        record Dosis(Short series, Short reps, Integer segundos, Short descanso) {}
    }

    private EvidenciaV32() {
    }

    /** La unica clase del origen, o vacio si no esta en ninguna o esta en mas de una. */
    static Optional<Clase> clase(PlanAdaptationAttempt intento, Long origen) {
        boolean a = contiene(intento.getAppliedSessions(), origen);
        boolean s = contiene(intento.getNoEffectSessions(), origen);
        boolean c = contiene(intento.getConservedSessions(), origen);
        boolean p = contiene(intento.getProgressedSessions(), origen);
        if ((a ? 1 : 0) + (s ? 1 : 0) + (c ? 1 : 0) + (p ? 1 : 0) != 1) return Optional.empty();
        return Optional.of(a ? Clase.APLICADA : s ? Clase.SIN_EFECTO : c ? Clase.CONSERVADA : Clase.PROGRESADA);
    }

    static boolean contiene(Long[] ids, Long id) {
        return ids != null && Arrays.asList(ids).contains(id);
    }

    static TipoIntervencion intervencion(PlanAdaptationAttempt intento, Long origen) {
        var sesiones = intento.getTargetedSessions();
        var intervenciones = intento.getTargetedInterventions();
        if (sesiones == null || intervenciones == null || sesiones.length != intervenciones.length) return null;
        for (int i = 0; i < sesiones.length; i++)
            if (origen.equals(sesiones[i])) {
                try {
                    return TipoIntervencion.valueOf(intervenciones[i]);
                } catch (IllegalArgumentException e) {
                    return null;
                }
            }
        return null;
    }

    /** El informe, o vacio si no se puede leer: quien llama lo trata como linaje roto. */
    static Optional<Informe> informe(PlanAdaptationAttempt intento, JsonSupport json) {
        var texto = intento.getConservationReport();
        if (texto == null || texto.isBlank()) return Optional.of(new Informe(List.of(), List.of()));
        try {
            var informe = json.read(texto, Informe.class);
            return Optional.of(new Informe(informe.sustituciones() == null ? List.of() : informe.sustituciones(),
                    informe.pasos() == null ? List.of() : informe.pasos()));
        } catch (IllegalStateException e) {
            return Optional.empty();
        }
    }

    /** El paso registrado para ese origen, si es exactamente uno. */
    static Optional<Informe.Paso> paso(Informe informe, Long origen) {
        var deEse = informe.pasos().stream().filter(p -> p != null && origen.equals(p.origen())).toList();
        return deEse.size() == 1 ? Optional.of(deEse.get(0)) : Optional.empty();
    }

    static boolean sustituida(Informe informe, Long origen) {
        return informe.sustituciones().stream().anyMatch(r -> r != null && origen.equals(r.sesionOrigen()));
    }
}
