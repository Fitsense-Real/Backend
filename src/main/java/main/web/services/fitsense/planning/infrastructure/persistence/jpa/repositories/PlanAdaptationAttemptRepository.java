package main.web.services.fitsense.planning.infrastructure.persistence.jpa.repositories;

import main.web.services.fitsense.planning.domain.model.aggregates.PlanAdaptationAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

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
}
