package main.web.services.fitsense.analytics.domain.model.valueobjects;

import java.time.LocalDate;
import java.util.*;

/** El denominador de 17.4, traducido al lenguaje de analytics. */
public record WeekPlanInput(
        Long planId,
        short weekNumber,
        List<ScheduledWorkout> workouts
) {
    public record ScheduledWorkout(
            Long plannedWorkoutId,
            LocalDate scheduledDate,
            int expectedDurationMinutes,
            int assignedExercises,
            String status,
            String skipReason,
            /** Version del plan; null si no se conoce (una sola version). */
            Long planId
    ) {
        public ScheduledWorkout(Long plannedWorkoutId, LocalDate scheduledDate, int expectedDurationMinutes,
                                int assignedExercises, String status, String skipReason) {
            this(plannedWorkoutId, scheduledDate, expectedDurationMinutes, assignedExercises, status,
                    skipReason, null);
        }

        /**
         * La persona DIJO que no lo haria: SKIPPED con causa. La causa es
         * obligatoria al omitir, y la tarea de vencimiento la deja en NULL; sin
         * esta distincion todo vencido sin registro contaba como declarado, y la
         * cobertura de registro llegaba siempre al 100 % al cerrar la semana.
         */
        public boolean omisionDeclarada() {
            return "SKIPPED".equals(status) && skipReason != null;
        }

        boolean finished() {
            return "COMPLETED".equals(status) || "PARTIAL".equals(status) || "SKIPPED".equals(status);
        }
    }

    /**
     * UN ENTRENAMIENTO POR DIA ENTRE VERSIONES. La semana suma los previstos no
     * REPLACED de todas sus versiones, y un previsto ya cerrado sobrevive al
     * reemplazo: si la version nueva trae el mismo dia, ese dia contaria dos
     * veces. Entre los de un mismo dia de versiones DISTINTAS queda uno, por
     * este orden: el que tiene sesion que cuenta, el que ya esta cerrado, el de
     * la version mas reciente. Dentro de una misma version no se toca nada.
     *
     * @param conSesion entrenamientos con una sesion que cuenta
     */
    public WeekPlanInput unoPorDia(Set<Long> conSesion) {
        if (workouts == null || workouts.size() < 2) return this;
        // La lista llega por version ascendente: el indice mas alto es el mas reciente.
        var porDia = new LinkedHashMap<LocalDate, List<Integer>>();
        for (int i = 0; i < workouts.size(); i++)
            porDia.computeIfAbsent(workouts.get(i).scheduledDate(), d -> new ArrayList<>()).add(i);

        var fuera = new HashSet<Integer>();
        for (var indices : porDia.values()) {
            long versiones = indices.stream().map(i -> workouts.get(i).planId()).filter(Objects::nonNull)
                    .distinct().count();
            if (versiones < 2) continue;
            int ganador = indices.stream().max(Comparator
                    .comparingInt((Integer i) -> conSesion.contains(workouts.get(i).plannedWorkoutId()) ? 1 : 0)
                    .thenComparingInt(i -> workouts.get(i).finished() ? 1 : 0)
                    .thenComparingInt(i -> i)).orElseThrow();
            var version = workouts.get(ganador).planId();
            indices.stream().filter(i -> !Objects.equals(workouts.get(i).planId(), version)).forEach(fuera::add);
        }
        if (fuera.isEmpty()) return this;
        var quedan = new ArrayList<ScheduledWorkout>();
        for (int i = 0; i < workouts.size(); i++) if (!fuera.contains(i)) quedan.add(workouts.get(i));
        return new WeekPlanInput(planId, weekNumber, List.copyOf(quedan));
    }

    public boolean hasActivePlan() {
        return planId != null && !workouts.isEmpty();
    }

    public int scheduledWorkouts() {
        return workouts.size();
    }

    public int assignedExercises() {
        return workouts.stream().mapToInt(ScheduledWorkout::assignedExercises).sum();
    }
}
