package main.web.services.fitsense.planning.application.internal.commandservices;

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
 *   <li>sin efecto con OTRA intervencion -tambien el RESPALDO, que deja la
 *       senalada como sin efecto- y la sesion publicada IDENTICA a la anterior
 *       (ejercicios en el mismo orden, tipo, series, repeticiones, segundos,
 *       descanso, carga, enfoque y duracion): sigue siendo la copia de la
 *       reduccion autorizada; se sigue retrocediendo (27/10/2026). Si cambio
 *       algo, se corta. Una intervencion por dolor nunca hereda;</li>
 *   <li>aplicada con ACORTAR_O_REORGANIZAR en un intento APLICADA: es el
 *       origen de la reduccion. Autorizada;</li>
 *   <li>un paso de progresion (V34) que no reduce volumen ni duracion
 *       (D-P7): la sesion sigue acortada por la misma reduccion; se sigue
 *       retrocediendo;</li>
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

            var clase = EvidenciaV32.clase(intento, origen);
            var informe = EvidenciaV32.informe(intento, jsonSupport);
            if (clase.isEmpty() || informe.isEmpty()) return false;
            if (EvidenciaV32.sustituida(informe.get(), origen)) return false;

            var anterior = plannedWorkoutRepository.findById(origen).orElse(null);
            if (anterior == null) return false;   // linaje roto

            switch (clase.get()) {
                case APLICADA -> {
                    return intento.getStatus() == AdaptationAttempt.Status.APLICADA
                            && EvidenciaV32.intervencion(intento, origen) == TipoIntervencion.ACORTAR_O_REORGANIZAR;
                }
                case SIN_EFECTO -> {
                    var intervencion = EvidenciaV32.intervencion(intento, origen);
                    // Antes, cualquier intervencion que no fuera ACORTAR cortaba la herencia aunque
                    // la sesion saliera identica: tras una senalada por fatiga que acababa en
                    // respaldo, la copia siguiente incumplia V20 y la semana salia NO_FACTIBLE
                    // (5 casos con Replicate real). La seguridad no hereda nunca.
                    if (intervencion == null || intervencion.exigeRevisionDeSeguridad()) return false;
                    if (intervencion != TipoIntervencion.ACORTAR_O_REORGANIZAR && !identica(actual, anterior))
                        return false;
                }
                case PROGRESADA -> {
                    // D-P7: un paso que NO reduce la duracion no interrumpe la
                    // herencia; una vuelta atras, o un paso sin detalle, si.
                    var paso = EvidenciaV32.paso(informe.get(), origen);
                    if (paso.isEmpty() || !Set.of("PROGRESION", "RECUPERACION").contains(paso.get().tipo())
                            || paso.get().volumenAntes() == null || paso.get().volumenDespues() == null
                            || paso.get().volumenDespues() < paso.get().volumenAntes()
                            || actual.getExpectedDurationMinutes() < duracion(origen))
                        return false;
                }
                case CONSERVADA -> { }
            }
            actual = anterior;
        }
        return false;
    }

    /**
     * La misma prescripcion: enfoque, duracion y ejercicios en el mismo orden con su tipo,
     * series, repeticiones, segundos, descanso y carga. Se ignora lo administrativo
     * (identificadores, fechas, estado de ejecucion).
     */
    static boolean identica(PlannedWorkout a, PlannedWorkout b) {
        if (a.getFocusCode() != b.getFocusCode()
                || !java.util.Objects.equals(a.getExpectedDurationMinutes(), b.getExpectedDurationMinutes()))
            return false;
        var x = ordenados(a);
        var y = ordenados(b);
        if (x.size() != y.size()) return false;
        for (int i = 0; i < x.size(); i++) {
            var e = x.get(i);
            var f = y.get(i);
            if (!java.util.Objects.equals(e.getExerciseId(), f.getExerciseId())
                    || e.getPrescriptionType() != f.getPrescriptionType()
                    || !java.util.Objects.equals(e.getPlannedSets(), f.getPlannedSets())
                    || !java.util.Objects.equals(e.getPlannedReps(), f.getPlannedReps())
                    || !java.util.Objects.equals(e.getPlannedDurationSeconds(), f.getPlannedDurationSeconds())
                    || !java.util.Objects.equals(e.getRestSeconds(), f.getRestSeconds())
                    || !mismaCarga(e.getTargetLoadKg(), f.getTargetLoadKg()))
                return false;
        }
        return true;
    }

    private static java.util.List<main.web.services.fitsense.planning.domain.model.entities.PlannedWorkoutExercise>
    ordenados(PlannedWorkout w) {
        return w.exercisesView().stream()
                .sorted(java.util.Comparator.comparing(
                        main.web.services.fitsense.planning.domain.model.entities.PlannedWorkoutExercise::getExerciseOrder,
                        java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                .toList();
    }

    private static boolean mismaCarga(java.math.BigDecimal a, java.math.BigDecimal b) {
        return a == null ? b == null : b != null && a.compareTo(b) == 0;
    }

    private int duracion(Long sesion) {
        return plannedWorkoutRepository.findById(sesion).map(w -> (int) w.getExpectedDurationMinutes())
                .orElse(Integer.MAX_VALUE);
    }
}
