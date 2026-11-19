package main.web.services.fitsense.adaptation.domain.model.valueobjects;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion;

/**
 * La orden de adaptacion. Sustituye a la orden porcentual.
 * <p>
 * NO LLEVA {@code targetVolume} NI BANDA, y eso es lo esencial. Un reemplazo del
 * mismo patron de movimiento conserva la dosis, asi que el volumen semanal queda
 * practicamente igual; la orden antigua exigia que el volumen SE MOVIERA -la
 * regla de direccionalidad de {@code AdjustmentDecisionTable} garantiza que la
 * banda nunca solapa el volumen previo-, de modo que una adaptacion localizada
 * correcta habria sido rechazada por V8 por no haber movido nada. Quitando la
 * banda, esa contradiccion desaparece sin inventar ninguna excepcion.
 * <p>
 * Lo que la orden dice es QUE SESIONES se pueden tocar y QUE se permite cambiar
 * en cada una. Lo que no esta senalado se conserva.
 */
public record AdaptationOrder(
        AdaptationOutcome outcome,
        List<TargetedSession> sessions,
        BigDecimal observedAdherencePct,
        BigDecimal registrationCoveragePct,
        int recordedWorkouts,
        int scheduledWorkouts,
        String reason
) {

    /**
     * Una sesion senalada y lo que se permite hacer en ella.
     *
     * @param plannedWorkoutId el identificador PERSISTENTE, que es por el que
     *                         hay que reconocer la sesion. Con
     *                         {@code SCHEDULE_CHANGE} la IA puede mover el
     *                         entrenamiento del miercoles al jueves, y entonces
     *                         la fecha cambia legitimamente: validar por fecha
     *                         rechazaria un cambio correcto.
     */
    public record TargetedSession(
            Long plannedWorkoutId,
            LocalDate scheduledDate,
            BigDecimal completionPct,
            String skipReason,
            Short sessionRpe,
            TipoIntervencion intervencion
    ) {}

    public static AdaptationOrder sinCambios(AdaptationOutcome outcome,
                                             BigDecimal observada, BigDecimal cobertura,
                                             int registrados, int previstos, String razon) {
        return new AdaptationOrder(outcome, List.of(), observada, cobertura,
                registrados, previstos, razon);
    }

    /** Si corresponde pedirle a la IA una propuesta. */
    public boolean adapta() {
        return outcome == AdaptationOutcome.ADAPT_LOCALIZED
                || outcome == AdaptationOutcome.ADAPT_EXPANDED;
    }

    /**
     * Si corresponde conservar la prescripcion anterior tal cual.
     * <p>
     * Vale igual para adherencia adecuada que para falta de evidencia: en los
     * dos casos no se adapta. Se distinguen en el registro, no en el efecto, y
     * por eso {@link #outcome()} se guarda aparte.
     */
    public boolean conserva() {
        return outcome == AdaptationOutcome.NO_CHANGE
                || outcome == AdaptationOutcome.INSUFFICIENT_DATA;
    }

    /** Lo que usara la validacion de conservacion para saber que puede cambiar. */
    public boolean esSesionSenalada(Long plannedWorkoutId) {
        return plannedWorkoutId != null && sessions.stream()
                .anyMatch(s -> plannedWorkoutId.equals(s.plannedWorkoutId()));
    }

    /**
     * Si en esa sesion se permite que un ejercicio cambie. Una sesion no
     * senalada nunca lo permite, y una senalada solo si su tipo de intervencion
     * lo admite: el motivo orienta, y si el problema era el horario, cambiar
     * ejercicios no esta permitido.
     */
    public boolean permiteSustituirEjercicioEn(Long plannedWorkoutId) {
        return sessions.stream()
                .filter(s -> plannedWorkoutId != null
                        && plannedWorkoutId.equals(s.plannedWorkoutId()))
                .anyMatch(s -> s.intervencion().permiteSustituirEjercicio());
    }

    public Optional<TargetedSession> sesion(Long plannedWorkoutId) {
        return sessions.stream()
                .filter(s -> plannedWorkoutId != null
                        && plannedWorkoutId.equals(s.plannedWorkoutId()))
                .findFirst();
    }

    /** Una linea para el log y para el prompt. */
    public String describe() {
        if (!adapta()) return outcome + " · " + reason;
        var detalle = sessions.stream()
                .map(s -> s.scheduledDate()
                        + " (" + (s.skipReason() == null ? "sin motivo" : s.skipReason())
                        + " -> " + s.intervencion() + ")")
                .toList();
        return outcome + " · " + sessions.size() + " sesion(es) senalada(s): " + detalle;
    }
}
