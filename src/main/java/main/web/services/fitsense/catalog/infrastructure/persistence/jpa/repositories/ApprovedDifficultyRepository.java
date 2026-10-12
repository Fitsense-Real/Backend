package main.web.services.fitsense.catalog.infrastructure.persistence.jpa.repositories;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Lectura de {@code v_exercises_manually_approved} (vista de la V27).
 * <p>
 * IMPLEMENTADO Y APAGADO. Hoy la vista tiene 4 filas -las cuatro aprobaciones
 * de LOWER_HIP de la V28- asi que aplicarlo como filtro global dejaria el
 * conjunto elegible en cuatro ejercicios y ningun plan seria posible. El
 * interruptor es {@code fitsense.approvals.filter-enabled}, por omision
 * {@code false}, y no se enciende hasta que haya aprobaciones suficientes para
 * los cuatro slots.
 * <p>
 * No hay entidad JPA para la vista a proposito: una vista de solo lectura
 * mapeada como {@code @Entity} invita a que alguien intente escribirla, y la
 * V27 pone un disparador que lo impide con un error en tiempo de ejecucion. Una
 * consulta nativa dice lo que es.
 * <p>
 * Tampoco se toca {@code ck_exercises_difficulty} ni la columna
 * {@code difficulty_level}: la vista NO sustituye al catalogo, lo autoriza. La
 * columna sigue siendo la que filtra por nivel (con su kappa 0,298 declarado) y
 * esto es una capa de admision por encima.
 */
@Repository
public class ApprovedDifficultyRepository {

    private final EntityManager entityManager;

    public ApprovedDifficultyRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Ids con ultima decision APPROVED, y el nivel que el revisor humano fijo.
     * <p>
     * La vista ya resuelve "ultima decision por ejercicio" y excluye REVOKED, de
     * modo que aqui no hay logica de vigencia que duplicar ni que desincronizar.
     */
    @SuppressWarnings("unchecked")
    public Map<Long, Short> findApprovedLevels() {
        var filas = (List<Object[]>) entityManager.createNativeQuery("""
                SELECT exercise_id, approved_difficulty_level
                FROM public.v_exercises_manually_approved
                """).getResultList();

        return filas.stream().collect(Collectors.toMap(
                fila -> ((Number) fila[0]).longValue(),
                fila -> ((Number) fila[1]).shortValue(),
                (a, b) -> a));
    }

    /** Cuantos ejercicios hay aprobados. Para el log de arranque y el diagnostico. */
    public long countApproved() {
        var total = entityManager.createNativeQuery("""
                SELECT count(*) FROM public.v_exercises_manually_approved
                """).getSingleResult();
        return ((Number) total).longValue();
    }

    /**
     * Cuantos aprobados hay por patron de movimiento. Es la cifra que decide si
     * el filtro se puede encender: hace falta cobertura de los cuatro slots de
     * 4C, no un total alto.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Long> countApprovedByMovementPattern() {
        var filas = (List<Object[]>) entityManager.createNativeQuery("""
                SELECT coalesce(movement_pattern, '(sin patron)'), count(*)
                FROM public.v_exercises_manually_approved
                GROUP BY 1
                """).getResultList();

        return filas.stream().collect(Collectors.toMap(
                fila -> (String) fila[0],
                fila -> ((Number) fila[1]).longValue(),
                (a, b) -> a));
    }
}
