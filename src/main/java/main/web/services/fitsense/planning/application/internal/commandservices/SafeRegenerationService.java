package main.web.services.fitsense.planning.application.internal.commandservices;

import main.web.services.fitsense.planning.domain.model.aggregates.PlanAdaptationAttempt;
import main.web.services.fitsense.planning.domain.model.aggregates.SafeRegenerationAttempt;
import main.web.services.fitsense.planning.domain.model.aggregates.SafeRegenerationAttempt.Outcome;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
import main.web.services.fitsense.planning.infrastructure.persistence.jpa.repositories.PlanAdaptationAttemptRepository;
import main.web.services.fitsense.planning.infrastructure.persistence.jpa.repositories.SafeRegenerationAttemptRepository;
import main.web.services.fitsense.planning.infrastructure.persistence.jpa.repositories.WeeklyTrainingPlanRepository;
import main.web.services.fitsense.planning.interfaces.acl.MeasuredWeekView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * P-03 · REGENERACION SEGURA EN EL MISMO CICLO, nivel 2.
 * <p>
 * El nivel 1 vive en el productor: la sustitucion determinista SUST-SEG-1 de lo que dolio, que conserva
 * el resto de la semana. Si ni la IA ni esa sustitucion dan una semana valida, la adaptacion queda
 * NO_FACTIBLE (V32). Antes, la persona se quedaba sin plan esa semana y el ciclo siguiente generaba desde
 * cero. Ahora esa misma generacion se hace AQUI, en el mismo ciclo:
 * <ol>
 *   <li>solo si el NO_FACTIBLE es por dolor (SEGURIDAD_ANTE_DOLOR), que es lo que aprobo P-03;</li>
 *   <li>solo con el motor de reglas (P-05: sin llamadas pagadas), con el conjunto elegible que ya excluye
 *       lo que dolio (P-02) y con la validacion completa V1-V25: mismo perfil, mismos dias, mismas
 *       sesiones (V2), sin M ni progresiones, dosis base de 4D;</li>
 *   <li>con una comprobacion explicita final: ningun ejercicio excluido en lo publicado;</li>
 *   <li>idempotente: una fila por medicion (UNIQUE weekly_metric_id). Repetir el ciclo devuelve el mismo
 *       plan; el plan y su traza se escriben en UNA transaccion, asi que una interrupcion no deja medio
 *       hecho.</li>
 * </ol>
 * Si tampoco hay semana valida, se registra NO_FACTIBLE con el motivo y no se publica nada.
 * <p>
 * Consecuencia que hay que conocer: la semana regenerada parte de las dosis base (sin linaje V31 hacia la
 * semana medida), igual que la generacion desde cero que antes ocurria una semana despues.
 */
@Service
public class SafeRegenerationService {

    private static final Logger log = LoggerFactory.getLogger(SafeRegenerationService.class);
    private static final String DOLOR = "SEGURIDAD_ANTE_DOLOR";

    private final SafeRegenerationAttemptRepository regeneraciones;
    private final PlanAdaptationAttemptRepository intentos;
    private final WeeklyTrainingPlanRepository planes;
    private final WeeklyTrainingPlanCommandService comandos;
    private final PlanGenerationPipeline pipeline;
    private final PainMemoryService dolor;
    private final TransactionTemplate tx;

    public SafeRegenerationService(SafeRegenerationAttemptRepository regeneraciones,
                                   PlanAdaptationAttemptRepository intentos, WeeklyTrainingPlanRepository planes,
                                   WeeklyTrainingPlanCommandService comandos, PlanGenerationPipeline pipeline,
                                   PainMemoryService dolor, TransactionTemplate tx) {
        this.regeneraciones = regeneraciones;
        this.intentos = intentos;
        this.planes = planes;
        this.comandos = comandos;
        this.pipeline = pipeline;
        this.dolor = dolor;
        this.tx = tx;
    }

    /** Idempotencia: si esta medicion ya se resolvio por P-03, su desenlace. */
    public Optional<SafeRegenerationAttempt> yaResuelta(Long weeklyMetricId) {
        return regeneraciones.findByWeeklyMetricId(weeklyMetricId);
    }

    /**
     * @return el plan regenerado, o vacio (no hubo NO_FACTIBLE por dolor, o la regeneracion tampoco es
     *         valida y queda NO_FACTIBLE registrado).
     */
    public Optional<Long> regenerar(Long userId, LocalDate newWeekStart, MeasuredWeekView medida,
                                    BigDecimal adherencia, BigDecimal rpe) {
        var previa = yaResuelta(medida.weeklyMetricId());
        if (previa.isPresent()) return Optional.ofNullable(previa.get().getResultingPlanId());

        var noFactible = intentos.findByUserIdAndNewWeekStartDateOrderByIdAsc(userId, newWeekStart).stream()
                .filter(a -> medida.weeklyMetricId().equals(a.getWeeklyMetricId()))
                .reduce((a, b) -> b)
                .filter(a -> a.getStatus() == AdaptationAttempt.Status.NO_FACTIBLE && porDolor(a));
        if (noFactible.isEmpty()) return Optional.empty();
        var intento = noFactible.get();

        var excluidos = List.copyOf(dolor.excluidosActivos(userId));
        // Sin duplicar la semana: si ya hay un plan activo, no se regenera encima.
        if (planes.findByUserIdAndWeekStartDateAndStatus(userId, newWeekStart, PlanStatus.ACTIVE).isPresent())
            return registrarNoFactible(userId, medida, newWeekStart, intento, excluidos,
                    "Ya hay un plan activo para la semana del %s: P-03 no duplica la semana.".formatted(newWeekStart));

        PreparedPlanGeneration preparada;
        PlanGenerationPipeline.Result generado;
        try {
            preparada = comandos.prepare(new GenerateWeeklyPlanCommand(userId, newWeekStart, PlanAdjustment.none(),
                    false, adherencia, rpe));
            generado = pipeline.runDeterministic(preparada.context(), preparada.divisor());
        } catch (main.web.services.fitsense.shared.domain.exceptions.DomainRuleViolationException e) {
            return registrarNoFactible(userId, medida, newWeekStart, intento, excluidos, e.getMessage());
        }

        // Comprobacion explicita, independiente de V1: nada de lo excluido en lo que se va a publicar.
        var reaparecen = generado.draft().workouts().stream().flatMap(w -> w.exercises().stream())
                .map(PlanDraft.DraftExercise::exerciseId).filter(excluidos::contains).distinct().toList();
        if (!reaparecen.isEmpty())
            return registrarNoFactible(userId, medida, newWeekStart, intento, excluidos,
                    "El borrador regenerado trae ejercicios excluidos por dolor " + reaparecen + ": no se publica.");

        // NO SE ANADE TRABAJO (auditoria del nivel 2, orden de cierre del PR #16). La regeneracion vuelve a las dosis
        // base y no conserva el linaje de la semana medida: tras una reduccion autorizada (fatiga) eso DESHACIA la
        // reduccion en plena semana de dolor (LOSE_WEIGHT: 102 -> 108 repeticiones). Ante dolor no se anade trabajo
        // y P-03 no puede superar el volumen: si la semana regenerada sube las repeticiones o los segundos totales
        // de la semana medida, no hay alternativa segura con las reglas vigentes y queda NO_FACTIBLE.
        var masTrabajo = masTrabajoQueLaMedida(medida.measuredPlanId(), generado.draft());
        if (masTrabajo != null)
            return registrarNoFactible(userId, medida, newWeekStart, intento, excluidos, masTrabajo);

        var plan = tx.execute(status -> {
            var guardado = comandos.persist(preparada, new GeneratedPlan(generado.draft(), generado.attempts()))
                    .orElseThrow();
            regeneraciones.save(new SafeRegenerationAttempt(userId, medida.weeklyMetricId(), medida.measuredPlanId(),
                    newWeekStart, intento.getId(), intento.getRejectionReason(), excluidos, Outcome.PUBLICADO,
                    guardado.getId(), null));
            return guardado.getId();
        });
        log.warn("REGENERACION_SEGURA usuario {} semana {}: NO_FACTIBLE por dolor (intento V32 {}) resuelto en el"
                        + " mismo ciclo con el motor de reglas · plan {} · excluidos {}", userId, newWeekStart,
                intento.getId(), plan, excluidos);
        return Optional.ofNullable(plan);
    }

    /**
     * El motivo, o null si la semana regenerada no sube ni las repeticiones ni los segundos totales planificados de
     * la semana medida (series x repeticiones y series x segundos, por separado, sin convertir uno en otro).
     */
    String masTrabajoQueLaMedida(Long planMedido, PlanDraft regenerada) {
        var medidaPlan = tx.execute(status -> planes.findById(planMedido).map(p -> {
            long r = 0, sg = 0;
            for (var w : p.workoutsView())
                for (var e : w.exercisesView()) {
                    int series = e.getPlannedSets() == null ? 1 : e.getPlannedSets();
                    r += (long) series * (e.getPlannedReps() == null ? 0 : e.getPlannedReps());
                    sg += (long) series * (e.getPlannedDurationSeconds() == null ? 0 : e.getPlannedDurationSeconds());
                }
            return new long[]{r, sg};
        }).orElse(null));
        if (medidaPlan == null) return "P-03 nivel 2: no se encuentra la semana medida para comparar el trabajo.";
        long r = 0, sg = 0;
        for (var w : regenerada.workouts())
            for (var e : w.exercises()) {
                int series = e.plannedSets() == null ? 1 : e.plannedSets();
                r += (long) series * (e.plannedReps() == null ? 0 : e.plannedReps());
                sg += (long) series * (e.plannedDurationSeconds() == null ? 0 : e.plannedDurationSeconds());
            }
        if (r <= medidaPlan[0] && sg <= medidaPlan[1]) return null;
        return ("P-03 nivel 2: la semana regenerada con dosis base sube el trabajo de la semana medida (repeticiones %d"
                + " -> %d, segundos %d -> %d); ante dolor no se anade trabajo y no hay otra alternativa determinista.")
                .formatted(medidaPlan[0], r, medidaPlan[1], sg);
    }

    private static boolean porDolor(PlanAdaptationAttempt a) {
        return (a.getTargetedInterventions() != null && Arrays.asList(a.getTargetedInterventions()).contains(DOLOR))
                || (a.getRejectionReason() != null && a.getRejectionReason().toLowerCase(Locale.ROOT).contains("dolor"));
    }

    private Optional<Long> registrarNoFactible(Long userId, MeasuredWeekView medida, LocalDate newWeekStart,
                                               PlanAdaptationAttempt intento, List<Long> excluidos, String motivo) {
        tx.executeWithoutResult(status -> regeneraciones.save(new SafeRegenerationAttempt(userId,
                medida.weeklyMetricId(), medida.measuredPlanId(), newWeekStart, intento.getId(),
                intento.getRejectionReason(), excluidos, Outcome.NO_FACTIBLE, null, motivo)));
        log.error("REGENERACION_SEGURA_NO_FACTIBLE usuario {} semana {}: {}", userId, newWeekStart, motivo);
        return Optional.empty();
    }
}
