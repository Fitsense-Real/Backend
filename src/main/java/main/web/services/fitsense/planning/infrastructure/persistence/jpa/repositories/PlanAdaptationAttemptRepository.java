package main.web.services.fitsense.planning.infrastructure.persistence.jpa.repositories;

import main.web.services.fitsense.planning.domain.model.aggregates.PlanAdaptationAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * La evidencia de adaptacion. SOLO SE INSERTA Y SE LEE.
 * <p>
 * No hay nada que actualice ni borre a proposito: una fila mal registrada se
 * corrige anadiendo otra, no reescribiendo la anterior. {@code JpaRepository}
 * trae {@code delete} y {@code save} heredados; que existan no significa que se
 * usen, y una fila de evidencia que cambia despues no es evidencia.
 */
@Repository
public interface PlanAdaptationAttemptRepository
        extends JpaRepository<PlanAdaptationAttempt, Long> {

    /**
     * Los intentos de una semana, en orden. Devuelve VARIOS a proposito: el
     * intento de IA rechazado y el respaldo que se publico despues son dos
     * hechos distintos de la misma semana, y la tabla no tiene clave unica
     * justamente para que quepan los dos.
     */
    List<PlanAdaptationAttempt> findByUserIdAndNewWeekStartDateOrderByIdAsc(
            Long userId, LocalDate newWeekStartDate);

    /** Todo lo registrado de un participante, para la lectura de la tesis. */
    List<PlanAdaptationAttempt> findByUserIdOrderByIdAsc(Long userId);

    /** Los intentos que publicaron ese plan. En un linaje integro, exactamente uno. */
    List<PlanAdaptationAttempt> findByResultingPlanId(Long resultingPlanId);

    /**
     * BLOQUEA LA FILA DE LA MEDICION hasta el final de la transaccion que llama.
     * <p>
     * Es la coordinacion persistente de la publicacion: dos ejecuciones del
     * ciclo de la misma semana (p. ej. una que perdio el bloqueo de sesion
     * mientras esperaba a la IA) se ponen en fila aqui, y la segunda, al
     * entrar, ya VE lo que confirmo la primera. Sin migracion: usa la fila de
     * weekly_user_metrics que ya existe. Devuelve vacio si la medicion no
     * existe, y entonces no hay nada que coordinar.
     */
    @Query(value = "SELECT weekly_metric_id FROM weekly_user_metrics WHERE weekly_metric_id = :id FOR UPDATE",
            nativeQuery = true)
    Optional<Long> bloquearMedicion(@Param("id") Long weeklyMetricId);

    /** El plan que ya publico un intento de esa medicion, si alguno lo hizo. */
    @Query(value = "SELECT resulting_plan_id FROM plan_adaptation_attempts WHERE weekly_metric_id = :id"
            + " AND resulting_plan_id IS NOT NULL ORDER BY attempt_id LIMIT 1", nativeQuery = true)
    Optional<Long> planPublicadoDeLaMedicion(@Param("id") Long weeklyMetricId);
}
