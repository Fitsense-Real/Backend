package main.web.services.fitsense.planning.domain.exceptions;

/**
 * 4A v6 · NINGUNA capacidad principal del objetivo es factible con las
 * condiciones actuales del participante. No se publica una semana hecha solo de
 * secundarias como si cumpliera el objetivo: como mucho existe una alternativa
 * LIMITADA, que requiere la aceptacion del participante y no se activa sola.
 * <p>
 * Extiende {@link PlanGenerationFailedException} a proposito: para la tarea
 * semanal es una semana sin plan, igual que cuando el motor de reglas no
 * consigue un plan valido.
 */
public class NoPrimaryCapabilityFeasibleException extends PlanGenerationFailedException {
    public NoPrimaryCapabilityFeasibleException(Long userId, String detail) {
        super(userId, "NINGUNA_PRINCIPAL_FACTIBLE: " + detail);
    }
}
