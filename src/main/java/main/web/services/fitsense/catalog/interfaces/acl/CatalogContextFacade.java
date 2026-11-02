package main.web.services.fitsense.catalog.interfaces.acl;

import main.web.services.fitsense.catalog.domain.model.aggregates.Exercise;
import main.web.services.fitsense.catalog.domain.model.queries.GetEligibleExercisesQuery;
import main.web.services.fitsense.catalog.domain.model.queries.GetExerciseByIdQuery;
import main.web.services.fitsense.catalog.domain.services.ExerciseQueryService;
import main.web.services.fitsense.catalog.infrastructure.persistence.jpa.repositories.BodyPartRepository;
import main.web.services.fitsense.catalog.infrastructure.persistence.jpa.repositories.EquipmentTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Unico punto de entrada al catalogo desde otros contextos.
 */
@Service
public class CatalogContextFacade {

    private final ExerciseQueryService exerciseQueryService;
    private final BodyPartRepository bodyPartRepository;
    private final EquipmentTypeRepository equipmentTypeRepository;

    public CatalogContextFacade(ExerciseQueryService exerciseQueryService,
                                BodyPartRepository bodyPartRepository,
                                EquipmentTypeRepository equipmentTypeRepository) {
        this.exerciseQueryService = exerciseQueryService;
        this.bodyPartRepository = bodyPartRepository;
        this.equipmentTypeRepository = equipmentTypeRepository;
    }

    /**
     * @param excludeHighImpact fuera saltos y pliometria
     * @param excludeFloorWork  fuera lo que exige bajar al suelo
     * @param excludeAxialLoad  fuera carga sobre columna o cuello
     * @param excludeKneeling   fuera lo que exige apoyar las rodillas
     * @param excludeHanging    fuera lo que exige suspenderse
     * @param excludeOverhead   fuera lo que exige los brazos sobre la cabeza
     * @param excludeInversion  fuera las posiciones con la cabeza bajo el corazon
     */
    @Transactional(readOnly = true)
    public List<EligibleExerciseView> fetchEligibleExercises(List<String> equipmentCodes,
                                                             boolean excludeGymOnly,
                                                             int maxDifficultyLevel,
                                                             List<Long> blockedExerciseIds,
                                                             boolean excludeHighImpact,
                                                             boolean excludeFloorWork,
                                                             boolean excludeAxialLoad,
                                                             boolean excludeKneeling,
                                                             boolean excludeHanging,
                                                             boolean excludeOverhead,
                                                             boolean excludeInversion) {
        var query = new GetEligibleExercisesQuery(equipmentCodes, excludeGymOnly,
                maxDifficultyLevel, blockedExerciseIds,
                excludeHighImpact, excludeFloorWork, excludeAxialLoad,
                excludeKneeling, excludeHanging, excludeOverhead, excludeInversion);

        var bodyParts = indexBodyParts();
        var equipment = indexEquipment();

        return exerciseQueryService.handle(query).stream()
                .map(exercise -> toView(exercise, bodyParts, equipment))
                .toList();
    }

    /** Detalle de un conjunto de ids. Lo usa planning para pintar el plan con media. */
    @Transactional(readOnly = true)
    public Map<Long, EligibleExerciseView> fetchDetails(Set<Long> exerciseIds) {
        if (exerciseIds == null || exerciseIds.isEmpty()) return Map.of();

        var bodyParts = indexBodyParts();
        var equipment = indexEquipment();

        return exerciseIds.stream()
                .map(id -> exerciseQueryService.handle(new GetExerciseByIdQuery(id)))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(Exercise::getId,
                        exercise -> toView(exercise, bodyParts, equipment), (a, b) -> a));
    }

    /** Nombre para mostrar de un conjunto de ids. */
    /** Las banderas de seguridad de cada ejercicio pedido, exista o no en el conjunto elegible. */
    @Transactional(readOnly = true)
    public Map<Long, ExerciseSafetyFlagsView> fetchSafetyFlags(Set<Long> exerciseIds) {
        if (exerciseIds == null || exerciseIds.isEmpty()) return Map.of();
        return exerciseIds.stream()
                .map(id -> exerciseQueryService.handle(new GetExerciseByIdQuery(id)))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(Exercise::getId, e -> new ExerciseSafetyFlagsView(e.getId(),
                        e.isActive(), e.isHighImpact(), e.isRequiresFloor(), e.isAxialLoad(),
                        e.isRequiresKneeling(), e.isRequiresHanging(), e.isRequiresOverhead(),
                        e.isRequiresInversion()), (a, b) -> a));
    }

    @Transactional(readOnly = true)
    public Map<Long, String> fetchNames(Set<Long> exerciseIds) {
        return fetchDetails(exerciseIds).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().nameEs(), (a, b) -> a));
    }

    private Map<Short, String> indexBodyParts() {
        var index = new HashMap<Short, String>();
        bodyPartRepository.findAll().forEach(bp -> index.put(bp.getId(), bp.getCode()));
        return index;
    }

    private Map<Short, String> indexEquipment() {
        var index = new HashMap<Short, String>();
        equipmentTypeRepository.findAll().forEach(eq -> index.put(eq.getId(), eq.getCode()));
        return index;
    }

    private static EligibleExerciseView toView(Exercise exercise,
                                               Map<Short, String> bodyParts,
                                               Map<Short, String> equipment) {
        return new EligibleExerciseView(
                exercise.getId(),
                exercise.displayName(),
                bodyParts.get(exercise.getBodyPartId()),
                equipment.get(exercise.getEquipmentId()),
                exercise.getTargetMuscle(),
                exercise.getDifficultyLevel(),
                exercise.getDefaultPrescription().name(),
                exercise.getGifPath(),
                exercise.getImagePath(),
                exercise.getMediaAttribution(),
                // V26. Hasta ahora ninguna clase Java leia esta columna.
                exercise.getMovementPattern());
    }
}
