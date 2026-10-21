package main.web.services.fitsense.planning.domain.model.valueobjects;

import main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion;

import java.util.List;
import java.util.Optional;

/**
 * El bloque "adjustment" de 19.1: lo que la regla ORDENA para esta semana.
 * <p>
 * ES EL ESPEJO DE LA DECISION DE ADAPTACION DENTRO DE {@code planning}, y por
 * eso vive aqui y no en {@code adaptation}: {@code planning} no importa nada de
 * {@code adaptation} -la dependencia va en el otro sentido-, asi que la orden le
 * llega traducida. {@code WeeklyCycleService}, en {@code orchestration}, es quien
 * traduce.
 * <p>
 * CONVIVEN DOS MECANISMOS, y no pueden dispararse a la vez:
 * <ul>
 *   <li><b>El antiguo, por objetivo porcentual.</b> {@code targetVolume} con su
 *       rango. La validacion 8 comprueba que el volumen entregado cae dentro.
 *       Activo cuando {@link #isActive()}.</li>
 *   <li><b>El nuevo, por adaptacion localizada.</b> {@link #targetedSessions()}:
 *       que sesiones fallaron y que se permite cambiar en cada una. La validacion
 *       de conservacion comprueba que se cambio esa parte y SOLO esa. Activo
 *       cuando {@link #adapta()}.</li>
 * </ul>
 * El segundo existe porque el primero es irreconciliable con la dosis fija de
 * 4D: con series y repeticiones fijadas por capacidad, nada puede mover el
 * volumen hasta un objetivo porcentual sin romper la dosis.
 */
public record PlanAdjustment(
        List<AdjustmentType> types,
        Integer targetVolume,
        Integer targetVolumeMin,
        Integer targetVolumeMax,
        double targetVolumeChangePct,
        double loadChangePct,
        Integer maxDifficultyLevel,
        Integer forcedDaysPerWeek,
        Integer forcedSessionMinutes,
        String reason,
        String dominantSkipReason,
        String distributionHint,

        /**
         * Las sesiones que la adaptacion senalo, con lo que se permite cambiar en
         * cada una. Vacia cuando no hay adaptacion localizada, que es el caso de
         * la primera semana y el de una semana con buena adherencia: si no hubo
         * baja adherencia NO se adapta nada.
         * <p>
         * Se identifican por {@code plannedWorkoutId} y no por fecha: con
         * SCHEDULE_CHANGE la sesion cambia de dia legitimamente, asi que validar
         * por fecha rechazaria el cambio correcto.
         */
        List<TargetedSession> targetedSessions
) {

    /**
     * Una sesion senalada.
     *
     * @param plannedWorkoutId la sesion de la semana MEDIDA que fallo. Es el
     *                         origen del que debe derivar su sesion nueva.
     * @param intervencion     que se permite cambiar dentro de ella.
     */
    public record TargetedSession(Long plannedWorkoutId, TipoIntervencion intervencion) {
        public TargetedSession {
            if (plannedWorkoutId == null)
                throw new IllegalArgumentException(
                        "Una sesion senalada tiene que decir cual es.");
            if (intervencion == null)
                throw new IllegalArgumentException(
                        "Una sesion senalada tiene que decir que se permite cambiar en ella.");
        }
    }

    public PlanAdjustment {
        targetedSessions = targetedSessions == null ? List.of() : List.copyOf(targetedSessions);

        // LOS DOS MECANISMOS NO PUEDEN ACTUAR JUNTOS, y no basta con que el flujo
        // previsto no los combine: si una orden llegara con las dos cosas, se
        // dispararian a la vez la validacion 8 -el rango porcentual- y la
        // validacion de conservacion, que son irreconciliables. Con la dosis de
        // 4D fijada por capacidad, nada puede mover el volumen hasta un objetivo
        // porcentual sin romper la dosis; eso es lo que hizo nacer la adaptacion
        // localizada. Mejor no construirla que descubrirlo en las validaciones.
        if (targetVolume != null && !targetedSessions.isEmpty())
            throw new IllegalArgumentException(
                    "No se pueden combinar ajustes porcentuales y adaptacion localizada"
                            + " (target_volume=" + targetVolume + ", sesiones senaladas="
                            + targetedSessions.size() + ")");
    }

    /** Primera semana: sin ajuste previo. No hay rango de volumen que verificar. */
    public static PlanAdjustment none() {
        return new PlanAdjustment(List.of(AdjustmentType.NONE), null, null, null,
                0.0, 0.0, null, null, null, null, null, null, List.of());
    }

    /**
     * Compatibilidad: el constructor de doce componentes que ya usaba
     * {@code WeeklyCycleService}. Sin sesiones senaladas, que es lo que
     * corresponde al mecanismo antiguo.
     */
    public PlanAdjustment(List<AdjustmentType> types, Integer targetVolume,
                          Integer targetVolumeMin, Integer targetVolumeMax,
                          double targetVolumeChangePct, double loadChangePct,
                          Integer maxDifficultyLevel, Integer forcedDaysPerWeek,
                          Integer forcedSessionMinutes, String reason,
                          String dominantSkipReason, String distributionHint) {
        this(types, targetVolume, targetVolumeMin, targetVolumeMax, targetVolumeChangePct,
                loadChangePct, maxDifficultyLevel, forcedDaysPerWeek, forcedSessionMinutes,
                reason, dominantSkipReason, distributionHint, List.of());
    }

    /** Hay orden de volumen: el mecanismo antiguo, con su rango porcentual. */
    public boolean isActive() {
        return targetVolume != null;
    }

    /**
     * Hay adaptacion localizada: al menos una sesion senalada.
     * <p>
     * NO se confunde con {@link #isActive()} a proposito. Si {@code isActive()}
     * devolviera true por esto, la validacion 8 leeria {@code targetVolumeMin()}
     * en null y reventaria.
     */
    public boolean adapta() {
        return !targetedSessions.isEmpty();
    }

    public boolean esSenalada(Long plannedWorkoutId) {
        return intervencionDe(plannedWorkoutId).isPresent();
    }

    /** Que se permite cambiar en esa sesion, si fue senalada. */
    public Optional<TipoIntervencion> intervencionDe(Long plannedWorkoutId) {
        if (plannedWorkoutId == null) return Optional.empty();
        return targetedSessions.stream()
                .filter(s -> plannedWorkoutId.equals(s.plannedWorkoutId()))
                .map(TargetedSession::intervencion)
                .findFirst();
    }

    public boolean has(AdjustmentType type) {
        return types != null && types.contains(type);
    }

    /** Cuando la orden incluye LOWER_LOAD, target_load_kg se deja en NULL (20.5). */
    public boolean clearsLoad() {
        return has(AdjustmentType.LOWER_LOAD);
    }
}
