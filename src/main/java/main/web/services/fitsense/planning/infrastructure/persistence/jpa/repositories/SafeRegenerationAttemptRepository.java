package main.web.services.fitsense.planning.infrastructure.persistence.jpa.repositories;

import main.web.services.fitsense.planning.domain.model.aggregates.SafeRegenerationAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SafeRegenerationAttemptRepository extends JpaRepository<SafeRegenerationAttempt, Long> {

    Optional<SafeRegenerationAttempt> findByWeeklyMetricId(Long weeklyMetricId);

    List<SafeRegenerationAttempt> findByUserIdOrderByIdAsc(Long userId);

    Optional<SafeRegenerationAttempt> findByResultingPlanId(Long resultingPlanId);
}
