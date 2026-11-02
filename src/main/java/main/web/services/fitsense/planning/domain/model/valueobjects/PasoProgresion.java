package main.web.services.fitsense.planning.domain.model.valueobjects;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * UN paso de progresion que propone el backend para UNA sesion no senalada en
 * un ciclo sin orden de adaptar (DISENO_PROGRESION_Y_REINCORPORACION).
 * <p>
 * {@code contenido} es la sesion completa ya con el paso, en su fecha de la
 * semana nueva: es lo que V8C compara con la derivada (identica) y lo que se
 * publica. No viaja a la evidencia; el resto si, en
 * {@code conservation_report.pasos}.
 *
 * @param origen        la sesion de la semana medida
 * @param exerciseId    el unico ejercicio que cambia
 * @param posicion      su posicion en la sesion nueva
 * @param antes         su dosis antes del paso; null si se reincorpora
 * @param despues       su dosis despues; null si la vuelta atras lo retira
 * @param estadoReserva estado del ejercicio retirado (R2), si lo hay
 */
public record PasoProgresion(Long origen, Tipo tipo, Long exerciseId, int posicion, Dosis antes, Dosis despues,
                             int volumenAntes, int volumenDespues, int volumenBase, int techo,
                             String estadoReserva, @JsonIgnore PlanDraft.DraftWorkout contenido) {

    public enum Tipo { RECUPERACION, PROGRESION, REINCORPORACION, VUELTA_ATRAS }

    public record Dosis(Short series, Short reps, Integer segundos, Short descanso) {
        public static Dosis de(PlanDraft.DraftExercise e) {
            return e == null ? null : new Dosis(e.plannedSets(), e.plannedReps(), e.plannedDurationSeconds(),
                    e.restSeconds());
        }
    }

    /**
     * D-P7: solo una recuperacion o una progresion que no baja el volumen
     * mantiene la herencia de D8. Una reincorporacion o una vuelta atras, no.
     */
    public boolean noReduce() {
        return (tipo == Tipo.PROGRESION || tipo == Tipo.RECUPERACION) && volumenDespues >= volumenAntes;
    }
}
