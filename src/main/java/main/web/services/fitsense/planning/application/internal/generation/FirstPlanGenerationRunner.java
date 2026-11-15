package main.web.services.fitsense.planning.application.internal.generation;

/**
 * Genera y publica el primer plan de un participante. Lanza las mismas
 * excepciones de dominio que el endpoint sincrono.
 */
public interface FirstPlanGenerationRunner {

    /**
     * @param beforePersist se invoca justo antes de publicar; lanza
     *        {@link OperationAbandonedException} si la operacion ya se cerro
     *        (tiempo maximo superado), y entonces no se publica nada.
     * @return el id del plan publicado.
     */
    Long run(Long userId, Runnable beforePersist);
}
