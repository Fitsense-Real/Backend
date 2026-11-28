package main.web.services.fitsense.planning.application.internal.queryservices;

import main.web.services.fitsense.planning.application.internal.commandservices.SafetyConflictChecker;
import main.web.services.fitsense.planning.application.internal.outboundservices.acl.ExternalProfilingService;
import main.web.services.fitsense.planning.domain.model.aggregates.WeeklyTrainingPlan;
import main.web.services.fitsense.planning.domain.model.entities.PlannedWorkout;
import main.web.services.fitsense.planning.domain.model.valueobjects.SafetyProfile;
import main.web.services.fitsense.planning.domain.model.valueobjects.WorkoutStatus;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * PLANES YA PUBLICADOS · que ejercicios de una sesion ABIERTA chocan HOY con la
 * seguridad del participante.
 * <p>
 * Un plan guardado no se revalida solo: si un ejercicio deja de ser compatible
 * despues de publicarse (INC-CAT-1374, una restriccion declarada despues, una
 * bandera corregida), la sesion sigue mostrandolo. Este servicio lo dice con la
 * causa, para que la lectura del plan lo marque y para que execution se niegue
 * a ejecutarlo. No modifica nada.
 * <p>
 * Solo se revisan las sesiones ABIERTAS (SCHEDULED, IN_PROGRESS). Las
 * terminadas y las REPLACED son historia: se consultan tal como fueron.
 */
@Service
public class PublishedPlanSafetyService {

    private final ExternalProfilingService profiling;
    private final SafetyConflictChecker checker;
    /**
     * P-DOLOR-PUB (cierre tecnico conservador, 15/11/2026). Un ejercicio que el participante reporto con dolor (P-02,
     * exclusion ACTIVA) se excluye de las semanas NUEVAS, pero seguia visible y ejecutable en las sesiones YA
     * publicadas y abiertas que lo repiten (otro dia de la misma semana, o una semana ya publicada cuando la
     * adaptacion queda NO_FACTIBLE). Con esta proteccion se marca con la causa {@code dolor_registrado}: la sesion
     * no se bloquea entera por el (PlannedWorkoutView.blockedForSafety), pero ese ejercicio solo puede registrarse
     * como omitido. No anade sustitutos, dosis ni sesiones. Encendida por defecto en la version candidata;
     * fitsense.seguridad.dolor-en-planes-publicados=false la apaga (comportamiento anterior).
     */
    private main.web.services.fitsense.planning.application.internal.commandservices.PainMemoryService painMemory;
    @org.springframework.beans.factory.annotation.Value("${fitsense.seguridad.dolor-en-planes-publicados:true}")
    private boolean dolorEnPlanesPublicados = true;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setPainMemory(main.web.services.fitsense.planning.application.internal.commandservices.PainMemoryService painMemory) {
        this.painMemory = painMemory;
    }

    public PublishedPlanSafetyService(ExternalProfilingService profiling, SafetyConflictChecker checker) {
        this.profiling = profiling;
        this.checker = checker;
    }

    public static boolean abierta(PlannedWorkout workout) {
        return workout.getStatus() == WorkoutStatus.SCHEDULED || workout.getStatus() == WorkoutStatus.IN_PROGRESS;
    }

    /** Por planned_exercise_id, el motivo. Solo sesiones abiertas de un plan activo. */
    public Map<Long, String> conflictos(WeeklyTrainingPlan plan) {
        if (plan == null || !plan.isActive()) return Map.of();
        return conflictos(plan.getUserId(), plan.workoutsView().stream().filter(PublishedPlanSafetyService::abierta)
                .toList());
    }

    /** Por planned_exercise_id, el motivo, de una sesion si esta abierta. */
    public Map<Long, String> conflictos(Long userId, PlannedWorkout workout) {
        if (workout == null || !abierta(workout)) return Map.of();
        return conflictos(userId, List.of(workout));
    }

    /**
     * Para execution: tambien un SKIPPED, porque todavia admite el reporte
     * retroactivo y reportarlo es registrar ahora lo que se hizo. Los
     * COMPLETED, PARTIAL y REPLACED no admiten registros nuevos.
     */
    public Map<Long, String> conflictosParaRegistrar(Long userId, PlannedWorkout workout) {
        if (workout == null || !(abierta(workout) || workout.getStatus() == WorkoutStatus.SKIPPED)) return Map.of();
        return conflictos(userId, List.of(workout));
    }

    private Map<Long, String> conflictos(Long userId, List<PlannedWorkout> workouts) {
        if (workouts.isEmpty()) return Map.of();
        // Sin perfil no hay restricciones contra las que comprobar; tampoco
        // puede haber plan, porque generarlo exige el perfil.
        var perfil = profiling.fetchProfile(userId).orElse(null);
        if (perfil == null) return Map.of();
        // Las mismas restricciones que aplica hoy el filtro del conjunto elegible.
        var seguridad = SafetyProfile.forAgeLevelAndDeclared(perfil.age(), perfil.fitnessLevel(), perfil.declaradas());
        var ejercicios = workouts.stream().flatMap(w -> w.exercisesView().stream()).toList();
        var motivos = new LinkedHashMap<>(checker.motivos(ejercicios.stream().map(e -> e.getExerciseId()).toList(),
                perfil, seguridad));
        if (dolorEnPlanesPublicados && painMemory != null) {
            var dolor = painMemory.excluidosActivos(userId);
            for (var e : ejercicios)
                if (dolor.contains(e.getExerciseId()))
                    motivos.merge(e.getExerciseId(), main.web.services.fitsense.planning.domain.services
                            .SafetyConflictPolicy.DOLOR_REGISTRADO, (a, b) -> a.contains(b) ? a : a + "+" + b);
        }
        var porPrescripcion = new LinkedHashMap<Long, String>();
        for (var e : ejercicios)
            if (motivos.containsKey(e.getExerciseId())) porPrescripcion.put(e.getId(), motivos.get(e.getExerciseId()));
        return porPrescripcion;
    }
}
