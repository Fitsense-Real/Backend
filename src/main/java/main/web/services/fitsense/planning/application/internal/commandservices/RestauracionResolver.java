package main.web.services.fitsense.planning.application.internal.commandservices;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import main.web.services.fitsense.planning.application.internal.outboundservices.acl.ExternalExecutionService;
import main.web.services.fitsense.planning.domain.model.aggregates.PlanAdaptationAttempt;
import main.web.services.fitsense.planning.domain.model.entities.PlannedWorkout;
import main.web.services.fitsense.planning.domain.model.entities.PlannedWorkoutExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.AdaptationAttempt;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanGenerationContext;
import main.web.services.fitsense.planning.domain.model.valueobjects.PreviousWeekSummary;
import main.web.services.fitsense.planning.infrastructure.persistence.jpa.repositories.PlanAdaptationAttemptRepository;
import main.web.services.fitsense.planning.infrastructure.persistence.jpa.repositories.PlannedWorkoutRepository;
import main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion;
import main.web.services.fitsense.shared.infrastructure.json.JsonSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

/**
 * V35 · LA RESTAURACION ELEGIBLE DE ESTE CICLO, SI HAY UNA (diseno v0.3, §3).
 * <p>
 * Para cada sesion de la semana medida se recorre su linaje hacia atras (V31)
 * clasificando cada paso con la evidencia V32 del intento que publico la
 * derivada. Las copias se atraviesan; el primer paso que no es copia decide:
 * <ul>
 *   <li>una adaptacion APLICADA con una intervencion RESTAURABLE
 *       (fatiga, motivacion, horario): la referencia es el origen de ese
 *       paso, la prescripcion anterior a la adaptacion;</li>
 *   <li>cualquier otra cosa -otra intervencion, sin efecto, una sustitucion
 *       por seguridad, una restauracion ya hecha, un paso sin intento o
 *       ambiguo, la generacion inicial-: nada que restaurar.</li>
 * </ul>
 * <b>Evidencia positiva, no ausencia de causa.</b> Solo se restaura si la
 * sesion adaptada se COMPLETO en la semana medida (estado de sesion
 * COMPLETED, el umbral ya aprobado) y sin las senales incompatibles
 * declaradas en ella: fatiga o dolor para la fatiga, falta de motivacion o
 * dolor para la motivacion, dolor para el horario. Tampoco si consta dolor en
 * alguna copia intermedia. Sin informacion, el ajuste se mantiene y la
 * restauracion queda pendiente: la copia lo arrastra y el recorrido la vuelve
 * a encontrar en el ciclo siguiente.
 * <p>
 * Devuelve a lo sumo UNA: la de adaptacion mas antigua y, a igualdad, la de
 * fecha mas temprana. La revalidacion completa (V8C, V1-V25, seguridad) la
 * hace despues el productor; si no pasa, no se restaura.
 */
@Component
public class RestauracionResolver {

    private static final Logger log = LoggerFactory.getLogger(RestauracionResolver.class);

    /** Las intervenciones cuyo cambio se restaura, con las causas que lo impiden. */
    private static final Map<TipoIntervencion, Set<String>> INCOMPATIBLES = Map.of(
            TipoIntervencion.REVISAR_CARGA_Y_RECUPERACION, Set.of("FATIGUE", "PAIN_OR_DISCOMFORT"),
            TipoIntervencion.INVESTIGAR_CAMBIO_MINIMO, Set.of("LACK_OF_MOTIVATION", "PAIN_OR_DISCOMFORT"),
            TipoIntervencion.REPROGRAMAR_DIAS, Set.of("PAIN_OR_DISCOMFORT"));

    private static final String DOLOR = "PAIN_OR_DISCOMFORT";
    private static final String COMPLETADA = "COMPLETED";

    /**
     * @param origen      la sesion de la semana medida
     * @param referencia  la sesion anterior a la adaptacion
     * @param contenido   la referencia puesta en la semana nueva (su dia de la semana)
     */
    public record Restauracion(Long origen, Long referencia, TipoIntervencion intervencion,
                               LocalDate semanaAdaptada, PlanDraft.DraftWorkout contenido) {}

    private final PlannedWorkoutRepository plannedWorkoutRepository;
    private final PlanAdaptationAttemptRepository attemptRepository;
    private final ExternalExecutionService externalExecutionService;
    private final JsonSupport jsonSupport;

    public RestauracionResolver(PlannedWorkoutRepository plannedWorkoutRepository,
                                PlanAdaptationAttemptRepository attemptRepository,
                                ExternalExecutionService externalExecutionService,
                                JsonSupport jsonSupport) {
        this.plannedWorkoutRepository = plannedWorkoutRepository;
        this.attemptRepository = attemptRepository;
        this.externalExecutionService = externalExecutionService;
        this.jsonSupport = jsonSupport;
    }

    @Transactional(readOnly = true)
    public Optional<Restauracion> candidata(PlanGenerationContext context) {
        var previa = context.previousWeek();
        if (previa == null || previa.workouts() == null) return Optional.empty();

        var candidatas = new ArrayList<Restauracion>();
        for (var sesion : previa.workouts()) {
            if (sesion.plannedWorkoutId() == null) continue;
            elegible(sesion, previa, context.weekStartDate()).ifPresent(candidatas::add);
        }
        var elegida = candidatas.stream()
                .min(Comparator.comparing(Restauracion::semanaAdaptada)
                        .thenComparing(r -> r.contenido().scheduledDate()));
        elegida.ifPresent(r -> log.info("RESTAURACION_ELEGIBLE usuario {} · sesion {} -> referencia {} ({}) · {}"
                        + " candidata(s)", context.userId(), r.origen(), r.referencia(), r.intervencion(),
                candidatas.size()));
        return elegida;
    }

    private Optional<Restauracion> elegible(PreviousWeekSummary.WorkoutOutcome medida, PreviousWeekSummary previa,
                                            LocalDate semanaNueva) {
        // Evidencia positiva: la sesion se completo. Sin registro, parcial u omitida: no.
        if (!medida.recorded() || !COMPLETADA.equals(medida.sessionStatus())) return Optional.empty();

        var causasDeclaradas = new HashSet<String>();
        for (var p : previa.prescriptions())
            if (medida.plannedWorkoutId().equals(p.plannedWorkoutId()) && p.skipReason() != null)
                causasDeclaradas.add(p.skipReason());

        var sesion = plannedWorkoutRepository.findById(medida.plannedWorkoutId()).orElse(null);
        var copias = new ArrayList<Long>();
        var vistas = new HashSet<Long>();
        var actual = sesion;
        while (actual != null && vistas.add(actual.getId())) {
            Long origen = actual.getSourcePlannedWorkoutId();
            if (origen == null) return Optional.empty();

            var intentos = attemptRepository.findByResultingPlanId(actual.getPlan().getId());
            if (intentos.size() != 1) return Optional.empty();
            var intento = intentos.get(0);

            int clases = (contiene(intento.getAppliedSessions(), origen) ? 1 : 0)
                    + (contiene(intento.getNoEffectSessions(), origen) ? 1 : 0)
                    + (contiene(intento.getConservedSessions(), origen) ? 1 : 0)
                    + (contiene(intento.getRestoredSessions(), origen) ? 1 : 0);
            if (clases != 1 || sustituida(intento, origen)) return Optional.empty();

            if (contiene(intento.getConservedSessions(), origen)) {
                copias.add(actual.getId());
                actual = plannedWorkoutRepository.findById(origen).orElse(null);
                continue;
            }
            if (!contiene(intento.getAppliedSessions(), origen)
                    || intento.getStatus() != AdaptationAttempt.Status.APLICADA) return Optional.empty();

            var intervencion = intervencion(intento, origen);
            if (intervencion == null || !INCOMPATIBLES.containsKey(intervencion)) return Optional.empty();
            if (causasDeclaradas.stream().anyMatch(INCOMPATIBLES.get(intervencion)::contains))
                return Optional.empty();
            copias.add(actual.getId());
            if (huboDolor(copias)) return Optional.empty();

            var referencia = plannedWorkoutRepository.findById(origen).orElse(null);
            if (referencia == null) return Optional.empty();
            var contenido = enLaSemanaNueva(referencia, semanaNueva);
            return Optional.of(new Restauracion(medida.plannedWorkoutId(), referencia.getId(), intervencion,
                    actual.getPlan().getWeekStartDate(), contenido));
        }
        return Optional.empty();
    }

    /** La referencia tal cual, en su dia de la semana dentro de la semana nueva. */
    private static PlanDraft.DraftWorkout enLaSemanaNueva(PlannedWorkout referencia, LocalDate semanaNueva) {
        var ejercicios = referencia.exercisesView().stream()
                .sorted(Comparator.comparing(PlannedWorkoutExercise::getExerciseOrder))
                .map(e -> new PlanDraft.DraftExercise(e.getExerciseId(), e.getPrescriptionType(),
                        e.getPlannedSets(), e.getPlannedReps(), e.getPlannedDurationSeconds(),
                        e.getTargetLoadKg(), e.getRestSeconds(), null))
                .toList();
        var dia = semanaNueva.plusDays(referencia.getScheduledDate().getDayOfWeek().getValue() - 1L);
        return new PlanDraft.DraftWorkout(dia, referencia.getFocusCode(), referencia.getWorkoutName(),
                referencia.getExpectedDurationMinutes(), ejercicios);
    }

    /** Dolor registrado en cualquier sesion entre la adaptacion y la semana medida. */
    private boolean huboDolor(List<Long> sesiones) {
        return externalExecutionService.fetchResultsByWorkout(sesiones).values().stream()
                .flatMap(r -> r.exercises() == null ? java.util.stream.Stream.empty() : r.exercises().stream())
                .anyMatch(e -> DOLOR.equals(e.skipReason()));
    }

    private static boolean contiene(Long[] ids, Long id) {
        return ids != null && Arrays.asList(ids).contains(id);
    }

    private static TipoIntervencion intervencion(PlanAdaptationAttempt intento, Long origen) {
        var sesiones = intento.getTargetedSessions();
        var intervenciones = intento.getTargetedInterventions();
        if (sesiones == null || intervenciones == null || sesiones.length != intervenciones.length) return null;
        for (int i = 0; i < sesiones.length; i++)
            if (origen.equals(sesiones[i])) {
                try {
                    return TipoIntervencion.valueOf(intervenciones[i]);
                } catch (IllegalArgumentException e) {
                    return null;
                }
            }
        return null;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Sustituciones(List<Ref> sustituciones) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        record Ref(Long sesionOrigen) {}
    }

    private boolean sustituida(PlanAdaptationAttempt intento, Long origen) {
        var json = intento.getConservationReport();
        if (json == null || json.isBlank()) return false;
        try {
            var informe = jsonSupport.read(json, Sustituciones.class);
            return informe.sustituciones() != null && informe.sustituciones().stream()
                    .anyMatch(r -> r != null && origen.equals(r.sesionOrigen()));
        } catch (IllegalStateException e) {
            return true;
        }
    }
}
