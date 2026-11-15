package main.web.services.fitsense.planning.application.internal.generation;

import main.web.services.fitsense.catalog.domain.exceptions.EmptyEligibleSetException;
import main.web.services.fitsense.planning.domain.exceptions.InsufficientSlotCoverageException;
import main.web.services.fitsense.planning.domain.exceptions.PlanGenerationFailedException;
import main.web.services.fitsense.planning.domain.exceptions.ProfileOutsideGoalDurationException;
import main.web.services.fitsense.planning.domain.model.valueobjects.GenerationOperationStatus;
import main.web.services.fitsense.planning.domain.model.valueobjects.NoFactibleReason;
import main.web.services.fitsense.shared.domain.exceptions.DomainRuleViolationException;
import main.web.services.fitsense.shared.domain.exceptions.ResourceNotFoundException;

/**
 * Traduce lo que lanza la generacion del primer plan al resultado de la
 * operacion. Dominio puro: se prueba sin Spring ni base.
 * <p>
 * NO_FACTIBLE es "no existe una semana valida con tus condiciones" y nunca se
 * reintenta solo: reintentar no cambia el catalogo ni las restricciones, y no
 * se relajan para conseguir un plan. FAILED con retryable=true es un problema
 * tecnico: un reintento explicito puede salir bien.
 * <p>
 * Los mensajes son para el participante; el detalle tecnico queda en el log.
 */
public final class GenerationFailureClassifier {

    public static final String ERROR_TECNICO = "ERROR_TECNICO";
    public static final String EJECUCION_INTERRUMPIDA = "EJECUCION_INTERRUMPIDA";
    public static final String PERFIL_INCOMPLETO = "PERFIL_INCOMPLETO";
    public static final String NO_GENERABLE = "NO_GENERABLE";
    public static final String TIEMPO_EXCEDIDO = "TIEMPO_EXCEDIDO";

    public static final String TEXTO_ERROR_TECNICO =
            "No pudimos preparar tu rutina por un problema técnico. Puedes intentarlo de nuevo.";
    public static final String TEXTO_INTERRUMPIDA =
            "La preparación de tu rutina se interrumpió. Puedes intentarlo de nuevo.";
    public static final String TEXTO_TIEMPO_EXCEDIDO =
            "La preparación de tu rutina tardó más de lo permitido. Puedes intentarlo de nuevo.";
    public static final String TEXTO_PERFIL_INCOMPLETO =
            "Completa tu perfil antes de preparar tu primera rutina.";
    public static final String TEXTO_NO_GENERABLE =
            "En este momento no es posible preparar tu rutina. Si persiste, contacta al equipo del estudio.";

    public record Outcome(GenerationOperationStatus status, String reasonCode, String message, boolean retryable) {}

    private GenerationFailureClassifier() {
    }

    public static Outcome classify(Throwable error) {
        if (error instanceof EmptyEligibleSetException || error instanceof InsufficientSlotCoverageException)
            return noFactible(NoFactibleReason.SIN_SEMANA_GENERABLE);
        if (error instanceof PlanGenerationFailedException)
            return noFactible(NoFactibleReason.SEMANA_NO_VALIDA);
        if (error instanceof ProfileOutsideGoalDurationException)
            return noFactible(NoFactibleReason.DISPONIBILIDAD_NO_CUMPLIBLE);
        if (error instanceof ResourceNotFoundException)
            return new Outcome(GenerationOperationStatus.FAILED, PERFIL_INCOMPLETO, TEXTO_PERFIL_INCOMPLETO, false);
        if (error instanceof DomainRuleViolationException)
            // Usuario no activo en el estudio u otra regla: reintentar no cambia nada.
            return new Outcome(GenerationOperationStatus.FAILED, NO_GENERABLE, TEXTO_NO_GENERABLE, false);
        return new Outcome(GenerationOperationStatus.FAILED, ERROR_TECNICO, TEXTO_ERROR_TECNICO, true);
    }

    public static Outcome interrupted() {
        return new Outcome(GenerationOperationStatus.FAILED, EJECUCION_INTERRUMPIDA, TEXTO_INTERRUMPIDA, true);
    }

    /** Supero el tiempo maximo (en cola o en ejecucion): nunca se publica tarde. */
    public static Outcome timedOut() {
        return new Outcome(GenerationOperationStatus.FAILED, TIEMPO_EXCEDIDO, TEXTO_TIEMPO_EXCEDIDO, true);
    }

    private static Outcome noFactible(NoFactibleReason reason) {
        return new Outcome(GenerationOperationStatus.NO_FACTIBLE, reason.name(), reason.texto(), false);
    }
}
