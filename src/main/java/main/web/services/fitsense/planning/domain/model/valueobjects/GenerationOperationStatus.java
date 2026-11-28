package main.web.services.fitsense.planning.domain.model.valueobjects;

/**
 * Estado de una operacion de generacion asincrona (V58).
 * <p>
 * PENDING: registrada, esperando a un worker. PROCESSING: un worker la esta
 * ejecutando. COMPLETED: el plan se publico (siempre con plan_id). FAILED:
 * error tecnico o regla que impide generar. NO_FACTIBLE: no existe una semana
 * valida con las condiciones del participante; no se relajan restricciones
 * para conseguirla.
 */
public enum GenerationOperationStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    NO_FACTIBLE;

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == NO_FACTIBLE;
    }
}
