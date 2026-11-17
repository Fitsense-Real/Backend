package main.web.services.fitsense.planning.infrastructure.persistence.jpa.repositories;

import main.web.services.fitsense.planning.domain.model.aggregates.PlanGenerationOperation;
import main.web.services.fitsense.planning.domain.model.valueobjects.GenerationOperationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlanGenerationOperationRepository extends JpaRepository<PlanGenerationOperation, UUID> {

    /** Propiedad: una operacion solo se ve si es del usuario que pregunta. */
    Optional<PlanGenerationOperation> findByIdAndUserId(UUID id, Long userId);

    Optional<PlanGenerationOperation> findFirstByUserIdAndStatusIn(Long userId, Collection<GenerationOperationStatus> statuses);

    Optional<PlanGenerationOperation> findFirstByUserIdOrderByCreatedAtDesc(Long userId);
}
