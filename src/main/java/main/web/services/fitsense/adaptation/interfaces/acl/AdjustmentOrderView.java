package main.web.services.fitsense.adaptation.interfaces.acl;

import main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion;

import java.util.List;

/**
 * Lo que la tarea semanal ve de una orden de ajuste.
 * <p>
 * LOS CAMPOS PORCENTUALES SIGUEN AQUI Y YA NO LLEVAN NADA. Desde la activacion
 * de la orden localizada, {@code UserInterventionCommandServiceImpl} construye
 * la decision como {@code NONE / 0}, asi que {@code targetVolume},
 * {@code targetVolumeChangePct} y los forzados llegan vacios. Se conservan
 * porque la vista es tambien lo que se registra de la intervencion, y quitarlos
 * obligaria a tocar {@code user_interventions}; el ajuste de verdad viaja en
 * {@link #targetedSessions()}.
 */
public record AdjustmentOrderView(
        Long interventionId,
        List<String> adjustmentTypes,
        String primaryType,
        int targetVolume,
        int targetVolumeMin,
        int targetVolumeMax,
        double targetVolumeChangePct,
        double loadChangePct,
        Integer forcedDaysPerWeek,
        Integer forcedSessionMinutes,
        Integer forcedMaxDifficulty,
        String reason,
        String dominantSkipReason,
        String distributionHint,
        String message,

        /**
         * EL DESENLACE QUE DECIDIO EL CONTEXTO DE ADAPTACION: NO_CHANGE,
         * INSUFFICIENT_DATA, ADAPT_LOCALIZED o ADAPT_EXPANDED.
         * <p>
         * No se puede deducir contando sesiones senaladas. La diferencia entre
         * localizada y ampliada la decide {@code AdaptationOrderDecider} -una
         * ampliada abarca todas las senaladas, una localizada solo la peor-, y
         * una ampliada con una sola sesion senalada quedaria registrada como
         * localizada. Es un hecho, no una inferencia, y V32 lo guarda como tal.
         * <p>
         * Viaja como texto porque los dos contextos tienen su propio enum con
         * los mismos cuatro nombres, y compartir el tipo los ataria.
         */
        String outcome,

        /**
         * QUE SESIONES FALLARON Y QUE SE PERMITE HACER EN CADA UNA. Vacia cuando
         * no hay adaptacion localizada, que es el caso de la primera semana y el
         * de una semana con buena adherencia.
         */
        List<TargetedSessionView> targetedSessions
) {

    public AdjustmentOrderView {
        targetedSessions = targetedSessions == null ? List.of() : List.copyOf(targetedSessions);
    }

    /**
     * Una sesion senalada, cruzando la frontera con DOS campos y no con seis.
     * <p>
     * {@code AdaptationOrder.TargetedSession} lleva ademas fecha, cumplimiento,
     * motivo y RPE. No viajan a proposito: son del contexto de adaptacion y
     * planning no los lee. {@code SessionFixPromptBuilder} los obtiene de
     * {@code context.previousWeek()} por el mismo {@code plannedWorkoutId},
     * asi que reenviarlos seria ensanchar la frontera con datos que nadie
     * consulta y que podrian quedar desincronizados de su fuente.
     *
     * @param plannedWorkoutId el identificador PERSISTENTE. Es por el que hay
     *                         que reconocer la sesion: con
     *                         {@code SCHEDULE_CHANGE} la fecha cambia
     *                         legitimamente, asi que identificar por fecha
     *                         rechazaria un cambio correcto.
     */
    public record TargetedSessionView(Long plannedWorkoutId, TipoIntervencion intervencion) {

        public TargetedSessionView {
            if (plannedWorkoutId == null)
                throw new IllegalArgumentException(
                        "Una sesion senalada tiene que decir cual es.");
            if (intervencion == null)
                throw new IllegalArgumentException(
                        "Una sesion senalada tiene que decir que se permite hacer en ella.");
        }
    }

    /** Si corresponde pedirle a la IA una correccion localizada. */
    public boolean adapta() {
        return !targetedSessions.isEmpty();
    }
}
