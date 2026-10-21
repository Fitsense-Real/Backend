package main.web.services.fitsense.planning.infrastructure.generation.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.List;

/**
 * Los datos de entrada de la ADAPTACION LOCALIZADA: una proyeccion del
 * {@link PlanInputSnapshot} de generacion inicial.
 * <p>
 * POR QUE EXISTE. El prompt de adaptacion solo debe describirle al modelo las
 * sesiones senaladas. Reutilizar el snapshot completo lo contradecia por otra
 * puerta: {@code previous_week.workouts} lleva TODAS las sesiones de la semana
 * anterior con su prescripcion y su ejecucion, de modo que las conservadas
 * volvian a viajar al final del prompt aunque el bloque de senaladas nombrara
 * solo una.
 * <p>
 * LISTA BLANCA, NO LISTA NEGRA. Esto no filtra campos del JSON general: es un
 * record propio que NOMBRA los campos admitidos. La diferencia importa. Si
 * manana se anade un dato a {@link PlanInputSnapshot}, o a su
 * {@code Constraints}, no puede empezar a viajar al prompt de adaptacion por
 * descuido: no hay donde ponerlo. Para que entre hay que anadirlo aqui a mano,
 * que es justo la decision que queremos que alguien tome a proposito.
 * <p>
 * NO SE DUPLICA LA MAQUINARIA DEL CATALOGO. {@code user} y
 * {@code available_exercises} se toman tal cual del snapshot general, con sus
 * mismos tipos: el barajado por grupo muscular, {@code min_reps},
 * {@code movement_pattern}, {@code slot} y {@code clase_dosis} se calculan una
 * sola vez y en un solo sitio. Ninguno de esos dos bloques puede llevar
 * historial de sesiones, asi que reutilizar el tipo no abre la puerta que esta
 * clase cierra. El unico bloque que mezcla datos de sesion con datos de semana
 * es {@code Constraints}, y de ese se declaran los campos uno por uno.
 *
 * @param schemaVersion     version de ESTA proyeccion, no la del snapshot
 *                          general.
 * @param promptVersion     {@link SessionFixPromptBuilder#VERSION}, no la de
 *                          {@code PlanPromptBuilder}. Las reglas que recibio el
 *                          modelo en una adaptacion son las de adaptacion, y es
 *                          lo que tiene que quedar registrado.
 * @param constraints       solo los limites que afectan a corregir UNA sesion.
 * @param availableExercises el catalogo elegible, igual que en generacion.
 * @param safetyNotes       se conserva: V8C puede exigir revision de seguridad.
 */
public record SessionFixInputSnapshot(
        @JsonProperty("schema_version") String schemaVersion,
        @JsonProperty("principles_version") String principlesVersion,
        @JsonProperty("prompt_version") String promptVersion,
        PlanInputSnapshot.User user,
        Constraints constraints,
        @JsonProperty("available_exercises")
        List<PlanInputSnapshot.AvailableExercise> availableExercises,
        @JsonProperty("safety_notes") String safetyNotes
) {

    /** Subir esta version es obligatorio si cambia que campos viajan. */
    public static final String VERSION = "FIX-IN-1.0";

    /**
     * Los limites de una CORRECCION, no los de disenar una semana.
     * <p>
     * Lo que se queda fuera, y por que:
     * <ul>
     *   <li>{@code suggested_split}: la division de la semana. Bajo adaptacion
     *       las fechas salen de la derivacion de V31, no del
     *       {@code WeeklySplitPlanner}, asi que mandarla no es redundante: es
     *       informacion que CONTRADICE la regla de que la fecha solo cambia si
     *       el motivo de esa sesion lo autoriza.
     *   <li>{@code days_per_week}: cuantos dias entrena es una decision de
     *       semana. El modelo no puede anadir ni quitar sesiones aqui.
     *   <li>{@code reduce_volume_caps}: pertenece al mecanismo porcentual, que
     *       {@code PlanAdjustment} ya prohibe combinar con sesiones senaladas.
     *   <li>{@code slots_exigibles}: cobertura de 4C, que se verifica sobre la
     *       semana completa. Mandarlo invita a meter los slots que falten en la
     *       unica sesion que puede tocar. Si una correccion rompe la cobertura,
     *       el rechazo se lo dice con el motivo concreto en el reintento.
     * </ul>
     */
    public record Constraints(
            @JsonProperty("week_number") short weekNumber,
            @JsonProperty("week_start_date") LocalDate weekStartDate,
            @JsonProperty("week_end_date") LocalDate weekEndDate,
            @JsonProperty("available_days") List<Short> availableDays,
            @JsonProperty("session_minutes") int sessionMinutes,
            @JsonProperty("training_location") String trainingLocation,
            @JsonProperty("max_difficulty_level") int maxDifficultyLevel,
            @JsonProperty("max_reps") Integer maxReps,
            @JsonProperty("target_reps") Integer targetReps,
            @JsonProperty("target_sets") Integer targetSets,
            @JsonProperty("min_session_minutes") Integer minSessionMinutes,
            @JsonProperty("min_exercises_per_session") Integer minExercisesPerSession,
            @JsonProperty("max_rest_seconds") Integer maxRestSeconds,
            @JsonProperty("dosis_por_clase") List<PlanInputSnapshot.DosisPorClase> dosisPorClase,
            @JsonProperty("max_colocaciones_dia") Integer maxColocacionesDia,
            @JsonProperty("minutos_utiles_dia") Double minutosUtilesDia
    ) {}

    /**
     * Proyecta el snapshot general. Campo por campo y por su nombre: esta lista
     * ES el contrato de lo que el modelo puede ver en una adaptacion.
     */
    public static SessionFixInputSnapshot of(PlanInputSnapshot full) {
        if (full == null)
            throw new IllegalArgumentException(
                    "No hay datos de entrada que proyectar para la adaptacion.");

        var c = full.constraints();
        if (c == null)
            throw new IllegalArgumentException(
                    "Los datos de entrada no traen restricciones: no se puede pedir"
                            + " una correccion sin los limites que la validan.");

        return new SessionFixInputSnapshot(
                VERSION,
                full.principlesVersion(),
                SessionFixPromptBuilder.VERSION,
                full.user(),
                new Constraints(
                        c.weekNumber(),
                        c.weekStartDate(),
                        c.weekEndDate(),
                        c.availableDays(),
                        c.sessionMinutes(),
                        c.trainingLocation(),
                        c.maxDifficultyLevel(),
                        c.maxReps(),
                        c.targetReps(),
                        c.targetSets(),
                        c.minSessionMinutes(),
                        c.minExercisesPerSession(),
                        c.maxRestSeconds(),
                        c.dosisPorClase(),
                        c.maxColocacionesDia(),
                        c.minutosUtilesDia()),
                full.availableExercises(),
                full.safetyNotes());
    }
}
