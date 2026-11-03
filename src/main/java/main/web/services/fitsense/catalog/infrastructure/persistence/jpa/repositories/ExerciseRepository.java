package main.web.services.fitsense.catalog.infrastructure.persistence.jpa.repositories;

import main.web.services.fitsense.catalog.domain.model.aggregates.Exercise;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExerciseRepository extends JpaRepository<Exercise, Long> {

    /**
     * Filtro del catalogo para la app.
     * <p>
     * Se usa COALESCE en vez de "(:param IS NULL OR columna = :param)": ese
     * patron obliga a PostgreSQL a deducir el tipo de un parametro que solo
     * aparece comparado contra NULL, y falla con "could not determine data type
     * of parameter". Dentro de COALESCE el tipo lo aporta la columna.
     */
    @Query("""
           SELECT e FROM Exercise e
           WHERE e.active = true
             AND e.bodyPartId = COALESCE(:bodyPartId, e.bodyPartId)
             AND e.equipmentId = COALESCE(:equipmentId, e.equipmentId)
             AND e.difficultyLevel <= :maxDifficulty
             AND LOWER(e.nameEs) LIKE :search
           ORDER BY e.nameEs ASC
           """)
    List<Exercise> search(@Param("bodyPartId") Short bodyPartId,
                          @Param("equipmentId") Short equipmentId,
                          @Param("maxDifficulty") short maxDifficulty,
                          @Param("search") String search,
                          Limit limit);

    /**
     * Conjunto elegible. Las siete ultimas banderas son las restricciones: cuando
     * llegan en true, se descarta lo marcado.
     * <p>
     * La forma "(:excluir = false OR e.campo = false)" permite pasar la
     * restriccion como bandera en vez de tener una consulta por combinacion.
     * <p>
     * equipmentIds nunca llega vacio (el query service garantiza al menos
     * 'body weight') y blockedIds tampoco (se rellena con un centinela): un IN
     * vacio en JPQL es un error de sintaxis en tiempo de ejecucion, no una
     * lista sin resultados.
     *
     * EL FILTRO DE EQUIPAMIENTO ES LA RELACION N:M DE V23, NO LA COLUMNA.
     * Antes era "e.equipmentId IN :equipmentIds", el implemento PRINCIPAL, del
     * que la propia V23 dice: "ya no filtra nada". Semantica real: grupos
     * distintos se combinan con AND -hacen falta todos- y los implementos del
     * mismo grupo con OR -basta uno-; un ejercicio sin filas no exige nada y
     * siempre pasa.
     *
     * El NOT EXISTS de abajo dice exactamente eso: no debe existir ningun grupo
     * de requisitos del ejercicio en el que NINGUNA de sus opciones este entre
     * el equipamiento disponible.
     *
     * Medido sobre el catalogo activo y el perfil de casa {peso corporal,
     * mancuerna, banda}: la columna admitia 592 ejercicios y la relacion real
     * admite 530. Los 62 de diferencia exigen balon de estabilidad (30), banco
     * (26), cajon pliometrico (6), step (6), barra fija, BOSU y rueda abdominal.
     * En la otra direccion, cero: la columna nunca era mas estricta.
     */
    @Query("""
           SELECT e FROM Exercise e
           WHERE e.active = true
             AND NOT EXISTS (
                   SELECT 1 FROM ExerciseEquipmentRequirement r
                   WHERE r.exerciseId = e.id
                     AND NOT EXISTS (
                           SELECT 1 FROM ExerciseEquipmentRequirement alternativa
                           WHERE alternativa.exerciseId = r.exerciseId
                             AND alternativa.groupNo = r.groupNo
                             AND alternativa.equipmentId IN :equipmentIds))
             AND e.difficultyLevel <= :maxDifficulty
             AND e.id NOT IN :blockedIds
             AND (:excludeHighImpact = false OR e.highImpact = false)
             AND (:excludeFloorWork  = false OR e.requiresFloor = false)
             AND (:excludeAxialLoad  = false OR e.axialLoad = false)
             AND (:excludeKneeling   = false OR e.requiresKneeling = false)
             AND (:excludeHanging    = false OR e.requiresHanging = false)
             AND (:excludeOverhead   = false OR e.requiresOverhead = false)
             AND (:excludeInversion  = false OR e.requiresInversion = false)
           ORDER BY e.bodyPartId ASC, e.difficultyLevel ASC, e.id ASC
           """)
    List<Exercise> findEligible(@Param("equipmentIds") List<Short> equipmentIds,
                                @Param("maxDifficulty") short maxDifficulty,
                                @Param("blockedIds") List<Long> blockedIds,
                                @Param("excludeHighImpact") boolean excludeHighImpact,
                                @Param("excludeFloorWork") boolean excludeFloorWork,
                                @Param("excludeAxialLoad") boolean excludeAxialLoad,
                                @Param("excludeKneeling") boolean excludeKneeling,
                                @Param("excludeHanging") boolean excludeHanging,
                                @Param("excludeOverhead") boolean excludeOverhead,
                                @Param("excludeInversion") boolean excludeInversion);

    long countByActiveTrue();
}
