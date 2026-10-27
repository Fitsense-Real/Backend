package main.web.services.fitsense.planning.application.internal.commandservices;

import main.web.services.fitsense.planning.domain.model.entities.PlannedWorkout;
import main.web.services.fitsense.planning.domain.model.entities.PlannedWorkoutExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.AdaptationAttempt;
import main.web.services.fitsense.planning.infrastructure.persistence.jpa.repositories.PlanAdaptationAttemptRepository;
import main.web.services.fitsense.planning.infrastructure.persistence.jpa.repositories.PlannedWorkoutRepository;
import main.web.services.fitsense.shared.infrastructure.json.JsonSupport;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * V35 · QUE DOSIS DE LA SEMANA MEDIDA TIENEN UN ORIGEN AUTORIZADO VERIFICABLE.
 * <p>
 * Para cada ejercicio de cada sesion se busca, hacia atras por V31, el paso en
 * que su dosis actual aparecio. Las copias de esa dosis se atraviesan (la
 * sesion puede haber cambiado en otra cosa). El paso que la introdujo decide:
 * <ul>
 *   <li>un paso de progresion (V35, {@code progressed_sessions}) cuyo detalle
 *       en V32 dice ese ejercicio y exactamente esa dosis: autorizada;</li>
 *   <li>una adaptacion APLICADA de la IA que BAJO esa dosis (ninguna dimension
 *       sube, el descanso igual): autorizada;</li>
 *   <li>cualquier otra cosa -la generacion inicial, una subida de la IA, un
 *       paso sin intento o ambiguo, un informe ilegible-: no. Entonces V24
 *       aplica su regla original a esa dosis.</li>
 * </ul>
 * No concede nada: solo reconoce lo que ya se autorizo y publico. V24 vuelve a
 * comprobar el rango ({@code ProgresionDosisPolicy}).
 */
@Component
public class DosisLinajeResolver {

    private final PlannedWorkoutRepository plannedWorkoutRepository;
    private final PlanAdaptationAttemptRepository attemptRepository;
    private final JsonSupport jsonSupport;

    public DosisLinajeResolver(PlannedWorkoutRepository plannedWorkoutRepository,
                               PlanAdaptationAttemptRepository attemptRepository,
                               JsonSupport jsonSupport) {
        this.plannedWorkoutRepository = plannedWorkoutRepository;
        this.attemptRepository = attemptRepository;
        this.jsonSupport = jsonSupport;
    }

    /** Por sesion, los ejercicios cuya dosis tiene origen autorizado. */
    public Map<Long, Set<Long>> resolver(Collection<PlannedWorkout> sesiones) {
        var porSesion = new LinkedHashMap<Long, Set<Long>>();
        for (var sesion : sesiones) {
            var conLinaje = new LinkedHashSet<Long>();
            for (var e : sesion.exercisesView())
                if (autorizada(sesion, e)) conLinaje.add(e.getExerciseId());
            if (!conLinaje.isEmpty()) porSesion.put(sesion.getId(), conLinaje);
        }
        return porSesion;
    }

    private boolean autorizada(PlannedWorkout sesion, PlannedWorkoutExercise ejercicio) {
        var vistas = new HashSet<Long>();
        var actual = sesion;
        while (actual != null && vistas.add(actual.getId())) {
            Long origen = actual.getSourcePlannedWorkoutId();
            if (origen == null) return false;   // generacion inicial: rige la dosis exacta

            var intentos = attemptRepository.findByResultingPlanId(actual.getPlan().getId());
            if (intentos.size() != 1) return false;
            var intento = intentos.get(0);
            var clase = EvidenciaV32.clase(intento, origen);
            var informe = EvidenciaV32.informe(intento, jsonSupport);
            if (clase.isEmpty() || informe.isEmpty()) return false;

            var anterior = plannedWorkoutRepository.findById(origen).orElse(null);
            if (anterior == null) return false;
            var previo = anterior.exercisesView().stream()
                    .filter(e -> Objects.equals(e.getExerciseId(), ejercicio.getExerciseId())).toList();
            if (previo.size() > 1) return false;

            if (previo.size() == 1 && misma(previo.get(0), ejercicio)) {   // copia de esta dosis
                actual = anterior;
                continue;
            }
            // Aqui aparecio la dosis: tiene que haberla puesto un paso o una reduccion.
            return switch (clase.get()) {
                case PROGRESADA -> EvidenciaV32.paso(informe.get(), origen)
                        .filter(p -> Objects.equals(p.exerciseId(), ejercicio.getExerciseId()))
                        .filter(p -> !"REINCORPORACION".equals(p.tipo()))
                        .map(EvidenciaV32.Informe.Paso::despues)
                        .filter(d -> Objects.equals(d.series(), ejercicio.getPlannedSets())
                                && Objects.equals(d.reps(), ejercicio.getPlannedReps())
                                && Objects.equals(d.segundos(), ejercicio.getPlannedDurationSeconds())
                                && Objects.equals(d.descanso(), ejercicio.getRestSeconds()))
                        .isPresent();
                case APLICADA -> intento.getStatus() == AdaptationAttempt.Status.APLICADA
                        && previo.size() == 1 && baja(previo.get(0), ejercicio);
                default -> false;
            };
        }
        return false;
    }

    private static boolean misma(PlannedWorkoutExercise a, PlannedWorkoutExercise b) {
        return a.getPrescriptionType() == b.getPrescriptionType()
                && Objects.equals(a.getPlannedSets(), b.getPlannedSets())
                && Objects.equals(a.getPlannedReps(), b.getPlannedReps())
                && Objects.equals(a.getPlannedDurationSeconds(), b.getPlannedDurationSeconds())
                && Objects.equals(a.getRestSeconds(), b.getRestSeconds());
    }

    /** Ninguna dimension sube, el tipo y el descanso no cambian, y algo baja. */
    private static boolean baja(PlannedWorkoutExercise antes, PlannedWorkoutExercise despues) {
        return antes.getPrescriptionType() == despues.getPrescriptionType()
                && Objects.equals(antes.getRestSeconds(), despues.getRestSeconds())
                && noSube(antes.getPlannedSets(), despues.getPlannedSets())
                && noSube(antes.getPlannedReps(), despues.getPlannedReps())
                && noSube(antes.getPlannedDurationSeconds(), despues.getPlannedDurationSeconds())
                && !misma(antes, despues);
    }

    private static boolean noSube(Number antes, Number despues) {
        if (antes == null || despues == null) return Objects.equals(antes, despues);
        return despues.intValue() <= antes.intValue();
    }
}
