package main.web.services.fitsense.planning.domain.model.valueobjects;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Una dosis distinta de la exacta que V24 puede aceptar, y SOLO en esa
 * colocacion: la sesion del {@code fecha} y el ejercicio {@code exerciseId},
 * con exactamente esas series, repeticiones, segundos y descanso.
 * <p>
 * La construye V8C en la adaptacion P2-D, nunca la generacion: la generacion
 * valida sin informe de conservacion y V24 aplica su regla original. Tres
 * origenes:
 * <ul>
 *   <li>{@code HEREDADA}: la dosis ya publicada de una copia identica, con su
 *       linaje V31 + V32 integro;</li>
 *   <li>{@code PASO}: la dosis nueva del paso de progresion que propuso el
 *       backend en este ciclo;</li>
 *   <li>{@code REDUCCION}: la dosis que una correccion de la IA BAJO en una
 *       sesion senalada cuya intervencion permite cambiar la dosis, aprobada
 *       por V8C. Nunca una subida.</li>
 * </ul>
 */
public record DosisAutorizada(LocalDate fecha, Long exerciseId, Short series, Short reps, Integer segundos,
                              Short descanso, Origen origen) {

    public enum Origen { HEREDADA, PASO, REDUCCION }

    public static DosisAutorizada de(LocalDate fecha, PlanDraft.DraftExercise e, Origen origen) {
        return new DosisAutorizada(fecha, e.exerciseId(), e.plannedSets(), e.plannedReps(),
                e.plannedDurationSeconds(), e.restSeconds(), origen);
    }

    /** Si autoriza exactamente esa colocacion con esa dosis. */
    public boolean autoriza(LocalDate fecha, PlanDraft.DraftExercise e) {
        return Objects.equals(this.fecha, fecha) && Objects.equals(exerciseId, e.exerciseId())
                && Objects.equals(series, e.plannedSets()) && Objects.equals(reps, e.plannedReps())
                && Objects.equals(segundos, e.plannedDurationSeconds()) && Objects.equals(descanso, e.restSeconds());
    }
}
