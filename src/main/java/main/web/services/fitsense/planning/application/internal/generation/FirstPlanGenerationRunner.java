package main.web.services.fitsense.planning.application.internal.generation;

/**
 * Genera y publica el primer plan de un participante. Lanza las mismas
 * excepciones de dominio que el endpoint sincrono.
 */
public interface FirstPlanGenerationRunner {

    /** @return el id del plan publicado. */
    Long run(Long userId);
}
