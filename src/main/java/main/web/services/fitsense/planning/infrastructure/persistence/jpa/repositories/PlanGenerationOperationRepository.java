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

    /*
     * V66: la tabla guarda tambien las operaciones de la generacion sincrona y del cierre semanal. La API de la generacion
     * inicial asincrona (y la app que la consulta) solo ve las suyas: estas tres consultas filtran por tipo.
     */
    Optional<PlanGenerationOperation> findByIdAndUserIdAndKind(UUID id, Long userId, String kind);

    Optional<PlanGenerationOperation> findFirstByUserIdAndKindAndStatusIn(Long userId, String kind,
                                                                         Collection<GenerationOperationStatus> statuses);

    Optional<PlanGenerationOperation> findFirstByUserIdAndKindOrderByCreatedAtDesc(Long userId, String kind);
}
