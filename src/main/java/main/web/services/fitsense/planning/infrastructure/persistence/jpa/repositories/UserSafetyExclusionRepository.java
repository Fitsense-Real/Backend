package main.web.services.fitsense.planning.infrastructure.persistence.jpa.repositories;

import main.web.services.fitsense.planning.domain.model.aggregates.UserSafetyExclusion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserSafetyExclusionRepository extends JpaRepository<UserSafetyExclusion, Long> {

    List<UserSafetyExclusion> findByUserIdOrderByIdAsc(Long userId);

    List<UserSafetyExclusion> findByUserIdAndStatusOrderByIdAsc(Long userId, UserSafetyExclusion.Status status);

    List<UserSafetyExclusion> findByUserIdAndExerciseIdAndStatus(Long userId, Long exerciseId,
                                                                 UserSafetyExclusion.Status status);

    boolean existsByUserIdAndExerciseIdAndSourcePlannedWorkoutId(Long userId, Long exerciseId,
                                                                  Long sourcePlannedWorkoutId);
}
