package main.web.services.fitsense.catalog.application.internal.queryservices;

import main.web.services.fitsense.catalog.domain.model.valueobjects.EquivalentExercise;
import main.web.services.fitsense.catalog.domain.services.FamiliaDeMovimientoEquivalente;

import main.web.services.fitsense.catalog.domain.model.aggregates.Exercise;
import main.web.services.fitsense.catalog.domain.model.queries.GetActiveExercisesQuery;
import main.web.services.fitsense.catalog.domain.model.queries.GetEligibleExercisesQuery;
import main.web.services.fitsense.catalog.domain.model.queries.GetExerciseByIdQuery;
import main.web.services.fitsense.catalog.domain.services.ExerciseQueryService;
import main.web.services.fitsense.catalog.infrastructure.persistence.jpa.repositories.BodyPartRepository;
import main.web.services.fitsense.catalog.infrastructure.persistence.jpa.repositories.EquipmentTypeRepository;
import main.web.services.fitsense.catalog.infrastructure.persistence.jpa.repositories.ExerciseRepository;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ExerciseQueryServiceImpl implements ExerciseQueryService {

    /** Siempre disponible: sin equipamiento declarado el plan es de peso corporal. */
    private static final String BODY_WEIGHT_CODE = "body weight";

    /** Centinela para que el NOT IN nunca reciba una lista vacia. */
    private static final long NO_BLOCKED_SENTINEL = -1L;

    /** ck_exercises_difficulty limita a 3. Sin filtro es lo mismo que pedir hasta 3. */
    private static final short MAX_DIFFICULTY_LEVEL = 3;

    private final ExerciseRepository exerciseRepository;
    private final BodyPartRepository bodyPartRepository;
    private final EquipmentTypeRepository equipmentTypeRepository;

    /**
     * Admision por aprobacion humana (V27/V28). APAGADA por omision; mientras lo
     * este, {@code apply} devuelve la lista intacta y no consulta nada.
     */
    private final ApprovedDifficultyGate approvedDifficultyGate;

    public ExerciseQueryServiceImpl(ExerciseRepository exerciseRepository,
                                    BodyPartRepository bodyPartRepository,
                                    EquipmentTypeRepository equipmentTypeRepository,
                                    ApprovedDifficultyGate approvedDifficultyGate) {
        this.exerciseRepository = exerciseRepository;
        this.bodyPartRepository = bodyPartRepository;
        this.equipmentTypeRepository = equipmentTypeRepository;
        this.approvedDifficultyGate = approvedDifficultyGate;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Exercise> handle(GetActiveExercisesQuery query) {
        Short bodyPartId = query.bodyPartCode() == null ? null
                : bodyPartRepository.findByCode(query.bodyPartCode()).map(bp -> bp.getId()).orElse((short) -1);
        Short equipmentId = query.equipmentCode() == null ? null
                : equipmentTypeRepository.findByCode(query.equipmentCode()).map(eq -> eq.getId()).orElse((short) -1);

        short maxDifficulty = query.maxDifficultyLevel() == null
                ? MAX_DIFFICULTY_LEVEL : query.maxDifficultyLevel();
        String search = query.search() == null ? "%" : "%" + query.search() + "%";

        return exerciseRepository.search(bodyPartId, equipmentId, maxDifficulty, search,
                Limit.of(query.limit()));
    }

    /**
     * Traduce las restricciones del perfil a ids de equipamiento antes de
     * consultar, y pasa las banderas de seguridad tal cual.
     */
    @Override
    @Transactional(readOnly = true)
    public List<Exercise> handle(GetEligibleExercisesQuery query) {
        var declared = new ArrayList<String>();
        declared.add(BODY_WEIGHT_CODE);
        if (query.equipmentCodes() != null) {
            query.equipmentCodes().stream()
                    .filter(code -> code != null && !code.isBlank())
                    .map(code -> code.trim().toLowerCase())
                    .forEach(declared::add);
        }

        var equipmentIds = equipmentTypeRepository.findAll().stream()
                .filter(eq -> declared.contains(eq.getCode()))
                .filter(eq -> !(query.excludeGymOnly() && eq.isRequiresGym()))
                .map(eq -> eq.getId())
                .distinct()
                .toList();

        if (equipmentIds.isEmpty()) return List.of();

        var blocked = new ArrayList<Long>();
        blocked.add(NO_BLOCKED_SENTINEL);
        if (query.blockedExerciseIds() != null) blocked.addAll(query.blockedExerciseIds());

        var elegibles = exerciseRepository.findEligible(equipmentIds,
                (short) query.maxDifficultyLevel(),
                blocked, query.excludeHighImpact(), query.excludeFloorWork(), query.excludeAxialLoad(),
                query.excludeKneeling(),
                query.excludeHanging(),
                query.excludeOverhead(),
                query.excludeInversion());

        // Ultimo paso, y APAGADO por omision. Va despues de la consulta y no
        // dentro: la vista de aprobaciones es de la V27 y mezclarla en el JPQL
        // ataria el filtro de equipamiento a ella. Asi encender o apagar el
        // interruptor no cambia ninguna consulta.
        return approvedDifficultyGate.apply(elegibles, query.maxDifficultyLevel());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Exercise> handle(GetExerciseByIdQuery query) {
        return exerciseRepository.findById(query.exerciseId());
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Map<Long, java.util.List<EquivalentExercise>> handle(
            main.web.services.fitsense.catalog.domain.model.queries.GetEquivalentVariantsQuery query) {
        var resultado = new java.util.TreeMap<Long, java.util.List<EquivalentExercise>>();
        if (query == null || query.exerciseIds() == null) return resultado;
        for (var id : query.exerciseIds()) {
            if (id == null) continue;
            var ejercicio = exerciseRepository.findById(id).orElse(null);
            if (ejercicio == null) continue;
            var equivalentes = new java.util.TreeMap<Long, EquivalentExercise>();
            var nombre = ejercicio.getNameEn();
            var clave = FamiliaDeMovimientoEquivalente.clave(nombre);
            if (clave == null) continue;
            // Nivel 1 (la propia clave) y nivel 2 (las demas claves de su grupo confirmado de la lista explicita; si no
            // esta en la lista, no tiene equivalentes de nivel 2). Cada clave arrastra sus variantes v. N.
            var grupo = FamiliaDeMovimientoEquivalente.grupo(nombre);
            var claves = new java.util.TreeSet<String>();
            claves.add(clave);
            grupo.ifPresent(g -> claves.addAll(g.claves()));
            // Cierre final: el mismo gesto con otro angulo (precaucion, no equivalencia).
            claves.addAll(FamiliaDeMovimientoEquivalente.clavesPrecaucionAngulo(nombre));
            for (var k : claves)
                exerciseRepository.candidatosDeFamilia(k, FamiliaDeMovimientoEquivalente.patronVariantes(k)).stream()
                        .filter(e -> !id.equals(e.getId()))
                        .forEach(e -> {
                            var nivel = FamiliaDeMovimientoEquivalente.nivel(nombre, e.getNameEn());
                            if (nivel == null) return; // el LIKE amplia de mas; el filtro es la regla
                            EquivalentExercise eq;
                            if (FamiliaDeMovimientoEquivalente.NIVEL_VARIANTE.equals(nivel))
                                eq = new EquivalentExercise(e.getId(), nivel, null,
                                        "el mismo ejercicio: mismo nombre salvo el sufijo v. N");
                            else if (FamiliaDeMovimientoEquivalente.NIVEL_MISMO_GESTO.equals(nivel))
                                eq = new EquivalentExercise(e.getId(), nivel, grupo.get().id(),
                                        grupo.get().justificacion());
                            else {
                                var fila = FamiliaDeMovimientoEquivalente.precaucionPorAngulo(nombre, e.getNameEn())
                                        .orElseThrow();
                                eq = new EquivalentExercise(e.getId(), nivel, fila.id(), fila.justificacion());
                            }
                            equivalentes.put(e.getId(), eq);
                        });
            if (!equivalentes.isEmpty()) resultado.put(id, java.util.List.copyOf(equivalentes.values()));
        }
        return resultado;
    }
}
