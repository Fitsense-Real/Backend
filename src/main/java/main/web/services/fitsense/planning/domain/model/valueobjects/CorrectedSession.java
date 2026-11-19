package main.web.services.fitsense.planning.domain.model.valueobjects;

import java.time.LocalDate;
import java.util.List;

/**
 * Lo que la IA devuelve por CADA sesion senalada, y nada mas.
 * <p>
 * ES EL CONTRATO A. A la IA no se le pide un plan: se le pide la correccion de
 * las sesiones que fallaron, identificadas por {@code sourcePlannedWorkoutId},
 * que es el identificador que el backend le dio. Las demas sesiones salen de la
 * derivacion, que ya las tiene copiadas.
 * <p>
 * POR QUE NO SE LE PIDE EL PLAN ENTERO. Si devolviera la semana completa habria
 * que emparejar su respuesta con la semana anterior, y ninguna clave sirve: la
 * fecha cambia entre semanas y con {@code SCHEDULE_CHANGE} cambia a proposito, el
 * orden se desplaza al quitar un dia, el enfoque se repite, y el esquema no
 * impide dos entrenamientos el mismo dia -la unica unicidad es
 * {@code uq_planned_workout_order}-. Deducir la correspondencia al final es
 * exactamente lo que V31 existe para evitar.
 * <p>
 * LO QUE NO PUEDE CAMBIAR, Y NO POR VALIDACION SINO PORQUE NO VIAJA AQUI: el
 * ENFOQUE y el NOMBRE de la sesion. Los pone el empalme desde la copia de la
 * semana anterior. Asi la regla de V8C -una sesion senalada se corrige, no se
 * reemplaza por otra- queda garantizada por construccion y no depende de que la
 * validacion la pille.
 *
 * @param sourcePlannedWorkoutId la sesion de la semana MEDIDA que se corrige.
 *                               Nunca nulo: sin el no se sabe que se corrigio.
 * @param scheduledDate          la fecha nueva. Puede ser la misma; cambiarla
 *                               solo lo autoriza {@code REPROGRAMAR_DIAS}, y eso
 *                               lo comprueba V8C.
 * @param expectedDurationMinutes los minutos declarados. V17 comprueba que
 *                               correspondan al contenido.
 * @param exercises              la prescripcion completa de la sesion corregida,
 *                               no un parche sobre la anterior: lo que no venga
 *                               aqui no esta en la sesion.
 * @param rationale              por que se corrigio asi. Texto libre, para la
 *                               traza; no se valida.
 */
public record CorrectedSession(Long sourcePlannedWorkoutId,
                               LocalDate scheduledDate,
                               Integer expectedDurationMinutes,
                               List<PlanDraft.DraftExercise> exercises,
                               String rationale) {

    public CorrectedSession {
        if (sourcePlannedWorkoutId == null)
            throw new IllegalArgumentException(
                    "Una sesion corregida tiene que decir cual corrige.");

        // exercises ES LA PRESCRIPCION COMPLETA, asi que su ausencia no es una
        // decision:
        //
        //   []    peticion explicita de dejar la sesion sin ejercicios. Se
        //         admite aqui; si eso vale o no lo deciden V5, 4C y 4D.
        //   null  prescripcion incompleta. La respuesta es invalida.
        //
        // Tratar el nulo como lista vacia convertia un campo que la IA olvido
        // en una orden de vaciar la sesion.
        //
        // La fecha y la duracion SI son opcionales y caen a las de la copia:
        // son campos que la correccion puede no tocar.
        if (exercises == null)
            throw new IllegalArgumentException(
                    "La correccion debe incluir exercises explicitamente.");
        exercises = List.copyOf(exercises);
    }
}
