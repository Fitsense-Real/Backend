package main.web.services.fitsense.planning.infrastructure.persistence.jpa.repositories;

import main.web.services.fitsense.planning.domain.model.aggregates.UserSafetyExclusion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserSafetyExclusionRepository extends JpaRepository<UserSafetyExclusion, Long> {

    List<UserSafetyExclusion> findByUserIdOrderByIdAsc(Long userId);

    List<UserSafetyExclusion> findByUserIdAndStatusOrderByIdAsc(Long userId, UserSafetyExclusion.Status status);

    List<UserSafetyExclusion> findByUserIdAndExerciseIdAndStatus(Long userId, Long exerciseId,
                                                                 UserSafetyExclusion.Status status);

    /**
     * Las activas de un ejercicio con BLOQUEO DE FILA: dos reactivaciones simultaneas se serializan y la segunda ya
     * no encuentra nada activo. Un dolor registrado despues de este bloqueo es una fila nueva y sigue activa.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT x FROM UserSafetyExclusion x WHERE x.userId = :userId AND x.exerciseId = :exerciseId"
            + " AND x.status = main.web.services.fitsense.planning.domain.model.aggregates.UserSafetyExclusion.Status.ACTIVA"
            + " ORDER BY x.id")
    List<UserSafetyExclusion> bloquearActivas(@Param("userId") Long userId, @Param("exerciseId") Long exerciseId);

    /**
     * Registro IDEMPOTENTE y seguro ante carreras: el evento original (usuario, ejercicio, sesion de origen) es la
     * identidad; si ya existe -activo o reactivado- no se inserta nada. Devuelve 1 si inserto, 0 si ya estaba.
     */
    @Modifying
    @Query(value = "INSERT INTO user_safety_exclusions (user_id, exercise_id, reason, source_plan_id,"
            + " source_planned_workout_id, source_planned_exercise_id, event_date, recorded_by)"
            + " VALUES (:userId, :exerciseId, :reason, :planId, :workoutId, :plannedExerciseId, :eventDate, :recordedBy)"
            + " ON CONFLICT (user_id, exercise_id, source_planned_workout_id) DO NOTHING", nativeQuery = true)
    int registrarSiNoExiste(@Param("userId") Long userId, @Param("exerciseId") Long exerciseId,
                            @Param("reason") String reason, @Param("planId") Long planId,
                            @Param("workoutId") Long workoutId, @Param("plannedExerciseId") Long plannedExerciseId,
                            @Param("eventDate") java.time.LocalDate eventDate, @Param("recordedBy") String recordedBy);

    boolean existsByUserIdAndExerciseIdAndSourcePlannedWorkoutId(Long userId, Long exerciseId,
                                                                  Long sourcePlannedWorkoutId);
}
