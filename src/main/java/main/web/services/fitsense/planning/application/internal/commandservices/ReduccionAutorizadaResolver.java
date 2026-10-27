package main.web.services.fitsense.planning.application.internal.commandservices;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import main.web.services.fitsense.planning.domain.model.aggregates.PlanAdaptationAttempt;
import main.web.services.fitsense.planning.domain.model.entities.PlannedWorkout;
import main.web.services.fitsense.planning.domain.model.valueobjects.AdaptationAttempt;
import main.web.services.fitsense.planning.infrastructure.persistence.jpa.repositories.PlanAdaptationAttemptRepository;
import main.web.services.fitsense.planning.infrastructure.persistence.jpa.repositories.PlannedWorkoutRepository;
import main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion;
import main.web.services.fitsense.shared.infrastructure.json.JsonSupport;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * D8 · QUE SESIONES DE LA SEMANA MEDIDA LLEVAN UNA REDUCCION AUTORIZADA.
 * <p>
 * Un ACORTAR_O_REORGANIZAR aplicado puede dejar la sesion bajo el piso de V20:
 * V8C la exime esa semana. Sin heredar la exencion, la COPIA de la semana
 * siguiente incumplia V20 y la semana quedaba NO_FACTIBLE aunque la persona
 * hubiera cumplido todo (VERIFICACION_DISENO_V03.md).
 * <p>
 * Se recorre el linaje hacia atras (V31) y cada paso se clasifica con la
 * evidencia V32 del intento que publico la derivada:
 * <ul>
 *   <li>copia (en {@code conserved_sessions}, sin sustitucion por seguridad):
 *       se sigue retrocediendo;</li>
 *   <li>sin efecto con ACORTAR_O_REORGANIZAR: la sesion no cambio y esa semana
 *       volvio a estar autorizada a acortarse; se sigue retrocediendo;</li>
 *   <li>aplicada con ACORTAR_O_REORGANIZAR en un intento APLICADA: es el
 *       origen de la reduccion. Autorizada;</li>
 *   <li>cualquier otra cosa -otra intervencion, una sustitucion por
 *       seguridad, un paso sin intento o con mas de uno, un origen en mas de
 *       una clase, la generacion inicial-: NO autorizada.</li>
 * </ul>
 * Solo conserva una reduccion ya autorizada; no concede ninguna nueva. Ante un
 * linaje incompleto o ambiguo, no hay exencion: V20 rige como siempre.
 */
@Component
public class ReduccionAutorizadaResolver {

    private final PlannedWorkoutRepository plannedWorkoutRepository;
    private final PlanAdaptationAttemptRepository attemptRepository;
    private final JsonSupport jsonSupport;

    public ReduccionAutorizadaResolver(PlannedWorkoutRepository plannedWorkoutRepository,
                                       PlanAdaptationAttemptRepository attemptRepository,
                                       JsonSupport jsonSupport) {
        this.plannedWorkoutRepository = plannedWorkoutRepository;
        this.attemptRepository = attemptRepository;
        this.jsonSupport = jsonSupport;
    }

    /** Los identificadores de las sesiones con una reduccion autorizada heredada. */
    public Set<Long> resolver(Collection<PlannedWorkout> sesiones) {
        var autorizadas = new LinkedHashSet<Long>();
        for (var sesion : sesiones)
            if (autorizada(sesion)) autorizadas.add(sesion.getId());
        return autorizadas;
    }

    private boolean autorizada(PlannedWorkout sesion) {
        var vistas = new HashSet<Long>();
        var actual = sesion;
        while (actual != null && vistas.add(actual.getId())) {
            Long origen = actual.getSourcePlannedWorkoutId();
            if (origen == null) return false;   // generacion inicial: nada que heredar

            var intentos = attemptRepository.findByResultingPlanId(actual.getPlan().getId());
            if (intentos.size() != 1) return false;   // sin evidencia o ambigua
            var intento = intentos.get(0);

            boolean aplicada = contiene(intento.getAppliedSessions(), origen);
            boolean sinEfecto = contiene(intento.getNoEffectSessions(), origen);
            boolean conservada = contiene(intento.getConservedSessions(), origen);
            if ((aplicada ? 1 : 0) + (sinEfecto ? 1 : 0) + (conservada ? 1 : 0) != 1) return false;
            if (sustituida(intento, origen)) return false;

            if (aplicada)
                return intento.getStatus() == AdaptationAttempt.Status.APLICADA
                        && intervencion(intento, origen) == TipoIntervencion.ACORTAR_O_REORGANIZAR;
            if (sinEfecto && intervencion(intento, origen) != TipoIntervencion.ACORTAR_O_REORGANIZAR)
                return false;

            actual = plannedWorkoutRepository.findById(origen).orElse(null);
        }
        return false;
    }

    private static boolean contiene(Long[] ids, Long id) {
        return ids != null && Arrays.asList(ids).contains(id);
    }

    private static TipoIntervencion intervencion(PlanAdaptationAttempt intento, Long origen) {
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Sustituciones(List<Ref> sustituciones) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        record Ref(Long sesionOrigen) {}
    }

    /** Una sustitucion por seguridad en el paso corta la herencia. Si el informe no se puede leer, tambien. */
    private boolean sustituida(PlanAdaptationAttempt intento, Long origen) {
        var json = intento.getConservationReport();
        if (json == null || json.isBlank()) return false;
        try {
            var informe = jsonSupport.read(json, Sustituciones.class);
            return informe.sustituciones() != null && informe.sustituciones().stream()
                    .anyMatch(r -> r != null && origen.equals(r.sesionOrigen()));
        } catch (IllegalStateException e) {
            return true;
        }
    }
}
