package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment.TargetedSession;
import main.web.services.fitsense.shared.domain.model.valueobjects.ContextoFaltaDeTiempo;
import main.web.services.fitsense.shared.domain.model.valueobjects.SkipReason;
import main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion;
import main.web.services.fitsense.planning.domain.model.valueobjects.WorkoutStatus;

/**
 * Falta de tiempo: la omision es un hecho, la causa es informacion declarada y la intervencion es
 * una decision justificada por ambas (DISENO_ADAPTACION_CONTEXTUAL.md v0.3, §1-2 y D1).
 * <p>
 * El decisor traduce LACK_OF_TIME en {@link TipoIntervencion#ACORTAR_O_REORGANIZAR}. Esa traduccion
 * solo esta justificada cuando hay evidencia de que la sesion no cabe en el tiempo de la persona:
 * <ul>
 *   <li>Sin respuesta, imprevisto o sin ningun minuto ese dia: {@code CONSERVAR}, sin IA. Recortar
 *       cinco minutos no resuelve un imprevisto ni un dia sin tiempo (caso A01).</li>
 *   <li>Menos tiempo habitual con M minutos: si la sesion ya cabe en M, {@code CONSERVAR}; si no,
 *       acortar con M como maximo.</li>
 *   <li>Una sesion EMPEZADA y no terminada por falta de tiempo (parcial) sigue acortandose: es la
 *       evidencia de que no cabia, y en parciales no se pregunta nada (D4).</li>
 * </ul>
 * No cambia el perfil ni las demas sesiones: M es la disponibilidad para ESTE entrenamiento.
 */
public final class FaltaDeTiempoPolicy {

    private FaltaDeTiempoPolicy() {}

    /**
     * @param status         estado del entrenamiento senalado en la semana medida
     * @param skipReason     su causa declarada al omitirlo
     * @param contexto       respuesta opcional a la pregunta de falta de tiempo
     * @param minutos        minutos declarados (solo con MENOS_TIEMPO_HABITUAL)
     * @param minutosSesion  duracion prevista de la sesion
     */
    public static TargetedSession afinar(TargetedSession senalada, WorkoutStatus status, SkipReason skipReason,
                                         ContextoFaltaDeTiempo contexto, Integer minutos, int minutosSesion) {
        if (senalada.intervencion() != TipoIntervencion.ACORTAR_O_REORGANIZAR) return senalada;
        // Solo la omision declarada por falta de tiempo; una parcial conserva la regla de siempre.
        if (status != WorkoutStatus.SKIPPED || skipReason != SkipReason.LACK_OF_TIME) return senalada;

        var id = senalada.plannedWorkoutId();
        if (contexto != ContextoFaltaDeTiempo.MENOS_TIEMPO_HABITUAL || minutos == null)
            return new TargetedSession(id, TipoIntervencion.CONSERVAR);
        if (minutos >= minutosSesion)
            return new TargetedSession(id, TipoIntervencion.CONSERVAR);
        return new TargetedSession(id, TipoIntervencion.ACORTAR_O_REORGANIZAR, minutos);
    }
}
