package main.web.services.fitsense.planning.domain.model.valueobjects;

import java.util.List;

/**
 * Lo que produce el productor de la semana adaptada: una PROPUESTA, no una
 * publicacion.
 * <p>
 * SEPARAR PROPUESTA DE PUBLICACION es el motivo de que esto exista. El productor
 * decide si algo es publicable; no publica. Quien publica es P2-B-2, y solo si
 * {@link #publicable()}.
 * <p>
 * LA CORRESPONDENCIA VIAJA EN {@code semana}, que es {@code List<DerivedWorkout>}
 * y no {@code PlanDraft}. Cada sesion sigue diciendo de cual proviene, sin clave
 * por fecha ni por posicion, hasta que {@code linkSource} la persista. Se
 * construye un {@code PlanDraft} para ejecutar los validadores existentes, pero
 * ese borrador NO sale de aqui: si saliera, la correspondencia habria que
 * reconstruirla, que es justo lo que V31 existe para evitar.
 *
 * @param estado       si se puede publicar.
 * @param semana       las sesiones con su origen. Vacia si NO_FACTIBLE.
 * @param source       {@link GenerationSource#AI} solo si la IA cambio algo de
 *                     verdad; {@code CONSERVED} si la semana quedo igual, aunque
 *                     la IA haya contestado.
 * @param conservacion el informe de V8C. Nunca null.
 * @param problemas    todo lo que impide publicar: V8C, seguridad, 4C y dosis.
 *                     Vacia si PUBLICABLE.
 */
public record AdaptedWeekResult(Estado estado,
                                Motivo motivo,
                                List<DerivedWorkout> semana,
                                GenerationSource source,
                                ConservationReport conservacion,
                                List<String> problemas) {

    /**
     * POR QUE la semana quedo como quedo. Es un dato, no una deduccion.
     * <p>
     * Sin esto, tres caminos distintos -no habia orden, la IA contesto sin
     * cambiar nada, la IA fallo- producen el mismo par
     * {@code PUBLICABLE + CONSERVED}, y P2-B-2 tendria que reconstruir cual fue
     * mirando el contexto y el informe. Reconstruir el desenlace es justo lo que
     * no puede pasar en una tabla de evidencia: V32 distingue SIN_ORDEN,
     * SIN_EFECTO y RESPALDO, y quien los registra tiene que SABERLO.
     * <p>
     * El motivo dice el CAMINO; {@link Estado} dice si se publica. Son
     * independientes: un RESPALDO_IA puede acabar NO_FACTIBLE.
     */
    public enum Motivo {
        /** No hubo baja adherencia, o no habia datos. Se copio la semana. */
        SIN_ORDEN,
        /** Se ordeno adaptar y la IA cambio algo de verdad. */
        CORRECCION_IA,
        /**
         * Se ordeno adaptar, la IA contesto y no cambio nada. La adaptacion
         * ocurrio y no tuvo efecto: las dos cosas son ciertas.
         */
        SIN_EFECTO,
        /**
         * No hubo respuesta usable de la IA. OJO: esto NO es el registro del
         * intento rechazado, que es un hecho aparte y lo registra quien llamo a
         * la IA. Son dos evidencias, no un intento con exito.
         */
        RESPALDO_IA
    }

    public enum Estado {
        /** Hay semana y pasa todo. */
        PUBLICABLE,
        /**
         * No hay semana que publicar. No es un error tecnico: es la respuesta
         * correcta cuando conservar seria inseguro o la propuesta no pasa.
         * P2-B-2 registra la evidencia igualmente.
         */
        NO_FACTIBLE
    }

    public AdaptedWeekResult {
        if (estado == null) throw new IllegalArgumentException("Un resultado sin estado no dice nada.");
        if (motivo == null)
            throw new IllegalArgumentException(
                    "Sin motivo, P2-B-2 tendria que adivinar que paso para registrar V32.");
        if (conservacion == null)
            throw new IllegalArgumentException("Sin informe de conservacion no hay resultado.");
        semana = semana == null ? List.of() : List.copyOf(semana);
        problemas = problemas == null ? List.of() : List.copyOf(problemas);

        if (estado == Estado.PUBLICABLE && !problemas.isEmpty())
            throw new IllegalArgumentException(
                    "Publicable con problemas es una contradiccion: " + problemas);
        if (estado == Estado.PUBLICABLE && semana.isEmpty())
            throw new IllegalArgumentException("Publicable sin sesiones no es publicable.");
        if (estado == Estado.NO_FACTIBLE && problemas.isEmpty())
            throw new IllegalArgumentException(
                    "No factible sin motivo no se puede registrar como evidencia.");
        if (estado == Estado.NO_FACTIBLE && !semana.isEmpty())
            throw new IllegalArgumentException(
                    "No factible con semana invita a publicarla igual.");

        // El origen se deduce del camino, y al reves no: si alguien pone AI en
        // un respaldo, la cifra de IA frente a reglas de la tesis queda inflada
        // con semanas en las que la IA no hizo nada.
        if (estado == Estado.PUBLICABLE) {
            var esperado = motivo == Motivo.CORRECCION_IA
                    ? GenerationSource.AI : GenerationSource.CONSERVED;
            if (source != esperado)
                throw new IllegalArgumentException(
                        "Un resultado " + motivo + " no puede declarar origen " + source
                                + ": corresponde " + esperado + ".");
        }
    }

    public boolean publicable() {
        return estado == Estado.PUBLICABLE;
    }

    /** Las sesiones que declaran origen. Bajo adaptacion son todas. */
    public List<Long> origenes() {
        return semana.stream().map(DerivedWorkout::sourcePlannedWorkoutId).toList();
    }

    public static AdaptedWeekResult noFactible(Motivo motivo, ConservationReport conservacion,
                                               List<String> problemas) {
        return new AdaptedWeekResult(Estado.NO_FACTIBLE, motivo, List.of(), null,
                conservacion, problemas);
    }
}
