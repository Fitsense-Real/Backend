package main.web.services.fitsense.planning.application.internal.commandservices;

import main.web.services.fitsense.planning.domain.model.aggregates.PlanAdaptationAttempt;
import main.web.services.fitsense.planning.domain.model.aggregates.WeeklyTrainingPlan;
import main.web.services.fitsense.planning.domain.model.entities.PlannedWorkout;
import main.web.services.fitsense.planning.domain.model.entities.PlannedWorkoutExercise;
import main.web.services.fitsense.planning.infrastructure.persistence.jpa.repositories.PlanAdaptationAttemptRepository;
import main.web.services.fitsense.planning.infrastructure.persistence.jpa.repositories.WeeklyTrainingPlanRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * H6 (auditoria post R/R', 05/11/2026): EL INTENTO V32 QUE PUBLICO UNA SESION, AUNQUE SU PLAN SEA UNA REPARACION.
 * <p>
 * Los tres recorridos de linaje (D8, dosis heredadas y progresion) buscaban el intento por el plan de la sesion. Una
 * reparacion por seguridad de un plan publicado ({@link PublishedPlanSafetyRepairService}, {@code SAFETY_REPAIR})
 * publica una VERSION NUEVA de la misma semana sin intento propio: el intento sigue apuntando a la version
 * reemplazada. Desde ese momento el linaje del participante se rompia para siempre (sin progresion, sin D8 y sin
 * dosis heredadas). Ahora, si el plan es una reparacion, se usa el intento de la version que reparo, PERO solo para
 * las sesiones que la reparacion dejo IDENTICAS: una sesion en la que la reparacion sustituyo un ejercicio por
 * seguridad no hereda nada (igual que una sustitucion por seguridad corta la herencia en un ciclo normal).
 * <p>
 * Defecto de implementacion, no cambio de politica: aplica a todas las versiones de politica.
 */
final class IntentoDeLinaje {

    static final String REPARACION = PublishedPlanSafetyRepairService.AJUSTE;

    private IntentoDeLinaje() {
    }

    /**
     * Los intentos que publicaron la sesion: los de su plan; si su plan es una reparacion sin intento propio y la
     * reparacion dejo la sesion identica, los de la version reparada (recursivamente). Lista vacia si no hay, o si
     * la reparacion cambio la sesion.
     */
    static List<PlanAdaptationAttempt> de(PlannedWorkout sesion, PlanAdaptationAttemptRepository intentos,
                                          WeeklyTrainingPlanRepository planes) {
        var plan = sesion.getPlan();
        var lista = intentos.findByResultingPlanId(plan.getId());
        var vistos = new HashSet<Long>();
        var actual = sesion;
        while (lista.isEmpty() && planes != null && plan != null && vistos.add(plan.getId())
                && REPARACION.equals(plan.getAdjustmentApplied()) && plan.getParentPlanId() != null) {
            var padre = planes.findById(plan.getParentPlanId()).orElse(null);
            if (padre == null) return List.of();
            var gemela = gemela(padre, actual);
            if (gemela == null || !identicas(gemela, actual)) return List.of();
            plan = padre;
            actual = gemela;
            lista = intentos.findByResultingPlanId(padre.getId());
        }
        return lista;
    }

    /** La sesion de la version reparada que la reparacion copio: mismo origen V31, misma fecha y enfoque. */
    private static PlannedWorkout gemela(WeeklyTrainingPlan padre, PlannedWorkout sesion) {
        var candidatas = padre.workoutsView().stream()
                .filter(w -> Objects.equals(w.getSourcePlannedWorkoutId(), sesion.getSourcePlannedWorkoutId()))
                .filter(w -> Objects.equals(w.getScheduledDate(), sesion.getScheduledDate()))
                .filter(w -> w.getFocusCode() == sesion.getFocusCode())
                .toList();
        return candidatas.size() == 1 ? candidatas.get(0) : null;
    }

    private static boolean identicas(PlannedWorkout a, PlannedWorkout b) {
        var ea = a.exercisesView();
        var eb = b.exercisesView();
        if (ea.size() != eb.size()) return false;
        for (int i = 0; i < ea.size(); i++)
            if (!misma(ea.get(i), eb.get(i))) return false;
        return a.getExpectedDurationMinutes() == b.getExpectedDurationMinutes();
    }

    private static boolean misma(PlannedWorkoutExercise a, PlannedWorkoutExercise b) {
        return Objects.equals(a.getExerciseId(), b.getExerciseId())
                && a.getPrescriptionType() == b.getPrescriptionType()
                && Objects.equals(a.getPlannedSets(), b.getPlannedSets())
                && Objects.equals(a.getPlannedReps(), b.getPlannedReps())
                && Objects.equals(a.getPlannedDurationSeconds(), b.getPlannedDurationSeconds())
                && Objects.equals(a.getRestSeconds(), b.getRestSeconds());
    }
}
