package main.web.services.fitsense.planning.infrastructure.generation.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.valueobjects.CorrectedSession;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.model.valueobjects.PrescriptionType;
import main.web.services.fitsense.shared.infrastructure.json.JsonSupport;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Convierte la respuesta del contrato A en {@code List<CorrectedSession>}.
 * <p>
 * UNA RESPUESTA INVALIDA, AUSENTE O INCOMPLETA NUNCA SE CONVIERTE EN UNA LISTA
 * VACIA. Es la regla que decidimos en el empalme y aqui es donde de verdad se
 * sostiene: si el adaptador devolviera una lista vacia ante un JSON roto, el
 * productor conservaria la semana y el intento quedaria registrado como
 * adaptacion SIN EFECTO. Un fallo del modelo contado como un intento legitimo
 * que no cambio nada es la peor lectura posible de la evidencia.
 * <p>
 * La distincion, que es la misma de {@link CorrectedSession}:
 * <ul>
 *   <li>{@code sessions: []} — la IA contesto que no propone correcciones.
 *       LEGITIMO: devuelve lista vacia y el productor conserva la semana.</li>
 *   <li>{@code sessions} ausente o nulo, o JSON que no parsea — la respuesta no
 *       vale. LANZA.</li>
 *   <li>{@code exercises} ausente o nulo en una sesion — prescripcion
 *       incompleta. LANZA.</li>
 * </ul>
 * LO QUE LANZA ES {@link InvalidPlanDraftException}, no una excepcion tecnica,
 * siguiendo lo que ya hace {@code PlanDraftFromJsonAssembler}: un enum
 * desconocido o una fecha mal formada son fallos del MODELO, y como tales tienen
 * que viajar en la lista de problemas que se le devuelve en el reintento. Si
 * fueran excepciones tecnicas, el reintento no sabria que corregir.
 * <p>
 * COMPRUEBA LOS IDENTIFICADORES CONTRA LO SOLICITADO. Un
 * {@code source_planned_workout_id} que no estaba entre las sesiones senaladas es
 * un fallo del modelo y se reporta como tal, para que el reintento lo lea. El
 * empalme tambien lo rechazaria, pero ahi seria una excepcion de programacion y
 * el modelo no se enteraria.
 */
@Component
public class SessionFixFromJsonAssembler {

    private final JsonSupport jsonSupport;

    public SessionFixFromJsonAssembler(JsonSupport jsonSupport) {
        this.jsonSupport = jsonSupport;
    }

    /**
     * @param json        la salida cruda del proveedor.
     * @param solicitadas los {@code plannedWorkoutId} de las sesiones senaladas.
     *                    Se usan para rechazar identificadores inventados.
     * @throws InvalidPlanDraftException si la respuesta no vale. Nunca devuelve
     *                                   lista vacia por un fallo.
     */
    @SuppressWarnings("unchecked")   // la lectura a Map crudo es deliberada
    public List<CorrectedSession> toCorrections(String json, Set<Long> solicitadas) {
        String limpio = stripCodeFences(json);

        // PRIMERA PASADA, EN CRUDO. Hay cosas que la lectura tipada ya no deja
        // ver, porque Jackson las normaliza antes de que lleguen:
        //
        //   - ACCEPT_SINGLE_VALUE_AS_ARRAY esta activado en JsonSupport -hace
        //     falta porque Replicate devuelve 'output' como cadena o como
        //     arreglo segun el modelo-, asi que un OBJETO suelto en 'sessions'
        //     se convierte en una lista de uno y el contrato queda incumplido
        //     sin que nadie se entere.
        //   - un elemento nulo dentro de un arreglo llega como null y luego
        //     revienta lejos de aqui.
        //
        // Son dos parseos de una carga pequena, a cambio de no aceptar formas
        // que el contrato prohibe.
        Map<String, Object> crudo;
        try {
            crudo = jsonSupport.read(limpio, Map.class);
        } catch (RuntimeException e) {
            throw new InvalidPlanDraftException(List.of(
                    "La respuesta no es un JSON valido con la forma esperada: " + e.getMessage()));
        }

        if (crudo == null)
            throw new InvalidPlanDraftException(List.of("La respuesta esta vacia."));

        var estructura = revisarEstructura(crudo);
        if (!estructura.isEmpty()) throw new InvalidPlanDraftException(estructura);

        AiFix parsed;
        try {
            parsed = jsonSupport.read(limpio, AiFix.class);
        } catch (RuntimeException e) {
            throw new InvalidPlanDraftException(List.of(
                    "La respuesta no es un JSON valido con la forma esperada: " + e.getMessage()));
        }

        // La version del contrato: si manana cambia, una respuesta de la version
        // vieja se rechaza en vez de interpretarse mal.
        if (!SessionFixJsonSchema.VERSION.equals(parsed.schemaVersion()))
            throw new InvalidPlanDraftException(List.of(
                    "schema_version es '%s' y este contrato es '%s'."
                            .formatted(parsed.schemaVersion(), SessionFixJsonSchema.VERSION)));

        // Y la lista vacia SI es una respuesta: no se corrige nada. Pero solo se
        // acepta DESPUES de comprobar que la respuesta esta completa: una
        // respuesta a medias no puede pasar por «no propongo correcciones».
        if (parsed.sessions().isEmpty()) return List.of();

        var problems = new ArrayList<String>();
        var correcciones = new ArrayList<CorrectedSession>();
        var vistas = new LinkedHashSet<Long>();
        var pedidas = solicitadas == null ? Set.<Long>of() : solicitadas;

        for (var session : parsed.sessions()) {
            Long origen = session.sourcePlannedWorkoutId();

            if (origen == null) {
                problems.add("Una sesion corregida no trae source_planned_workout_id. "
                        + "Es obligatorio: sin el no se sabe que sesion corrige.");
                continue;
            }
            if (!pedidas.contains(origen)) {
                problems.add(("La sesion %d no estaba senalada. Las senaladas son %s: corrige "
                        + "solo esas.").formatted(origen, ordenadas(pedidas)));
                continue;
            }
            if (!vistas.add(origen)) {
                problems.add("La sesion %d viene dos veces. Corrige cada una una sola vez."
                        .formatted(origen));
                continue;
            }

            // exercises ES la prescripcion completa, asi que su ausencia no es
            // «sin ejercicios», es una respuesta incompleta.
            if (session.exercises() == null) {
                problems.add(("La sesion %d no trae 'exercises'. Es la prescripcion completa "
                        + "de la sesion, no un parche: si de verdad quieres dejarla sin "
                        + "ejercicios, devuelve un arreglo vacio.").formatted(origen));
                continue;
            }

            LocalDate fecha = session.scheduledDate() == null ? null
                    : parseDate(session.scheduledDate(), origen, problems);
            if (session.scheduledDate() != null && fecha == null) continue;

            var ejercicios = new ArrayList<PlanDraft.DraftExercise>();
            boolean ejerciciosValidos = true;
            for (var exercise : session.exercises()) {
                if (exercise.exerciseId() == null) {
                    problems.add("Un ejercicio de la sesion %d no trae exercise_id."
                            .formatted(origen));
                    ejerciciosValidos = false;
                    continue;
                }
                var tipo = parseEnum(exercise.prescriptionType(), origen,
                        exercise.exerciseId(), problems);
                if (tipo == null) {
                    ejerciciosValidos = false;
                    continue;
                }

                // Las tres conversiones se hacen ANTES de construir, y si alguna
                // se sale de rango el ejercicio entero se descarta: construirlo
                // con el campo en null dejaria pasar una prescripcion a medias.
                int antes = problems.size();
                var series = toShort(exercise.plannedSets(), "planned_sets",
                        origen, exercise.exerciseId(), problems);
                var reps = toShort(exercise.plannedReps(), "planned_reps",
                        origen, exercise.exerciseId(), problems);
                var descanso = toShort(exercise.restSeconds(), "rest_seconds",
                        origen, exercise.exerciseId(), problems);
                if (problems.size() > antes) {
                    ejerciciosValidos = false;
                    continue;
                }

                ejercicios.add(new PlanDraft.DraftExercise(
                        exercise.exerciseId(), tipo, series, reps,
                        exercise.plannedDurationSeconds(),
                        exercise.targetLoadKg(),
                        descanso,
                        exercise.notes()));
            }
            if (!ejerciciosValidos) continue;

            correcciones.add(new CorrectedSession(origen, fecha,
                    session.expectedDurationMinutes(), List.copyOf(ejercicios),
                    session.changeReason()));
        }

        if (!problems.isEmpty()) throw new InvalidPlanDraftException(problems);

        return List.copyOf(correcciones);
    }

    /**
     * Lo que el contrato exige de la FORMA, antes de mirar el contenido.
     * <p>
     * Se hace sobre el JSON crudo porque la lectura tipada ya no deja ver si
     * 'sessions' llego como arreglo o como objeto suelto, ni si un elemento
     * venia nulo.
     */
    private List<String> revisarEstructura(Map<String, Object> crudo) {
        var problems = new ArrayList<String>();

        if (!crudo.containsKey("schema_version") || crudo.get("schema_version") == null)
            problems.add("La respuesta no trae 'schema_version'. El contrato lo exige: '%s'."
                    .formatted(SessionFixJsonSchema.VERSION));

        if (!crudo.containsKey("rationale") || crudo.get("rationale") == null)
            problems.add("La respuesta no trae 'rationale'. El contrato lo exige: explica en "
                    + "una linea por que corregiste asi.");

        // AUSENTE NO ES VACIO. Que falte el arreglo no significa que no haya
        // correcciones; significa que la respuesta esta incompleta.
        if (!crudo.containsKey("sessions") || crudo.get("sessions") == null) {
            problems.add("La respuesta no trae el arreglo 'sessions'. Si no propones ninguna "
                    + "correccion, devuelvelo vacio: no lo omitas.");
            return problems;
        }

        if (!(crudo.get("sessions") instanceof List<?> sessions)) {
            problems.add("'sessions' tiene que ser un arreglo, no un objeto suelto, aunque "
                    + "corrijas una sola sesion.");
            return problems;
        }

        for (int i = 0; i < sessions.size(); i++) {
            var sesion = sessions.get(i);
            if (sesion == null) {
                problems.add("El elemento %d de 'sessions' es nulo.".formatted(i));
                continue;
            }
            if (!(sesion instanceof Map<?, ?> mapa)) {
                problems.add("El elemento %d de 'sessions' no es un objeto.".formatted(i));
                continue;
            }
            var ejercicios = mapa.get("exercises");
            if (ejercicios == null) continue;   // lo reporta el recorrido tipado, con el id
            if (!(ejercicios instanceof List<?> lista)) {
                problems.add(("'exercises' del elemento %d tiene que ser un arreglo, no un "
                        + "objeto suelto.").formatted(i));
                continue;
            }
            for (int j = 0; j < lista.size(); j++)
                if (lista.get(j) == null)
                    problems.add("El ejercicio %d del elemento %d de 'sessions' es nulo."
                            .formatted(j, i));
        }

        return problems;
    }

    private static List<Long> ordenadas(Set<Long> ids) {
        return ids.stream().sorted().toList();
    }

    /**
     * Con json_schema no deberian aparecer, pero un modelo pequeno a veces
     * envuelve la respuesta igual. Mismo tratamiento que en
     * {@code PlanDraftFromJsonAssembler}: cuesta tres lineas y evita contar como
     * fallo del modelo algo que no lo es.
     */
    private String stripCodeFences(String raw) {
        if (raw == null) return "";
        var trimmed = raw.trim();
        if (!trimmed.startsWith("```")) return trimmed;
        int firstBreak = trimmed.indexOf('\n');
        int lastFence = trimmed.lastIndexOf("```");
        if (firstBreak < 0 || lastFence <= firstBreak) return trimmed;
        return trimmed.substring(firstBreak + 1, lastFence).trim();
    }

    private LocalDate parseDate(String raw, Long origen, List<String> problems) {
        try {
            return LocalDate.parse(raw);
        } catch (RuntimeException e) {
            problems.add("scheduled_date invalido en la sesion %d: '%s'. Usa AAAA-MM-DD."
                    .formatted(origen, raw));
            return null;
        }
    }

    private PrescriptionType parseEnum(String raw, Long origen, Long exerciseId,
                                       List<String> problems) {
        try {
            return PrescriptionType.valueOf(String.valueOf(raw).trim().toUpperCase());
        } catch (RuntimeException e) {
            problems.add("prescription_type invalido en el ejercicio %d de la sesion %d: '%s'."
                    .formatted(exerciseId, origen, raw));
            return null;
        }
    }

    /**
     * Convierte a Short COMPROBANDO EL RANGO.
     * <p>
     * {@code Integer.shortValue()} trunca en silencio, y para una dosis eso es
     * peor que fallar: 65626 segundos de descanso se convierten en <b>90</b>,
     * que es exactamente el descanso estructural de 4D, y 65539 series se
     * convierten en <b>3</b>. Un valor absurdo se transforma en uno que PASA la
     * validacion, y despues no hay forma de saber que la prescripcion no era la
     * que el modelo dijo.
     * <p>
     * Se reporta como problema del modelo, con el valor original, para que el
     * reintento sepa que corregir.
     */
    private Short toShort(Integer value, String campo, Long origen, Long exerciseId,
                          List<String> problems) {
        if (value == null) return null;
        if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) {
            problems.add(("%s del ejercicio %d de la sesion %d vale %d, que se sale del rango "
                    + "permitido (%d a %d).")
                    .formatted(campo, exerciseId, origen, value,
                            (int) Short.MIN_VALUE, (int) Short.MAX_VALUE));
            return null;
        }
        return value.shortValue();
    }

    // Estructura literal del contrato A. Se mapea aparte del dominio para que un
    // cambio del contrato con el proveedor no toque los value objects.
    private record AiFix(
            @JsonProperty("schema_version") String schemaVersion,
            String rationale,
            List<AiSession> sessions) {}

    private record AiSession(
            @JsonProperty("source_planned_workout_id") Long sourcePlannedWorkoutId,
            @JsonProperty("scheduled_date") String scheduledDate,
            @JsonProperty("expected_duration_minutes") Integer expectedDurationMinutes,
            @JsonProperty("change_reason") String changeReason,
            List<AiExercise> exercises) {}

    private record AiExercise(
            @JsonProperty("exercise_id") Long exerciseId,
            // exercise_order NO esta: el orden efectivo es la posicion en el
            // arreglo. Recibirlo y no usarlo era tener dos fuentes de verdad
            // para el mismo dato.
            @JsonProperty("prescription_type") String prescriptionType,
            @JsonProperty("planned_sets") Integer plannedSets,
            @JsonProperty("planned_reps") Integer plannedReps,
            @JsonProperty("planned_duration_seconds") Integer plannedDurationSeconds,
            @JsonProperty("target_load_kg") BigDecimal targetLoadKg,
            @JsonProperty("rest_seconds") Integer restSeconds,
            String notes) {}
}
