package main.web.services.fitsense.planning.interfaces.acl;

import main.web.services.fitsense.planning.application.internal.commandservices.AdaptedWeekCommandService;
import main.web.services.fitsense.planning.domain.model.aggregates.WeeklyTrainingPlan;
import main.web.services.fitsense.planning.domain.model.commands.ClosePlanWeekCommand;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.commands.RecordWorkoutOutcomeCommand;
import main.web.services.fitsense.planning.domain.model.commands.StartPlannedWorkoutCommand;
import main.web.services.fitsense.planning.domain.model.entities.PlannedWorkout;
import main.web.services.fitsense.planning.domain.model.queries.GetPlannedWorkoutByIdQuery;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.domain.model.valueobjects.WorkoutStatus;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanQueryService;
import main.web.services.fitsense.planning.infrastructure.persistence.jpa.repositories.WeeklyTrainingPlanRepository;
import main.web.services.fitsense.shared.domain.model.valueobjects.SkipReason;
import main.web.services.fitsense.planning.application.internal.queryservices.PublishedPlanSafetyService;
import main.web.services.fitsense.planning.application.internal.commandservices.PublishedPlanSafetyRepairService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Unico punto de entrada a planning desde otros contextos: lo consumen
 * execution, analytics, adaptation y la tarea semanal.
 */
@Service
public class PlanningContextFacade {

    private final WeeklyTrainingPlanQueryService queryService;
    private final WeeklyTrainingPlanCommandService commandService;
    private final WeeklyTrainingPlanRepository planRepository;
    private final AdaptedWeekCommandService adaptedWeekCommandService;
    private final PublishedPlanSafetyService publishedPlanSafety;
    private final PublishedPlanSafetyRepairService publishedPlanRepair;

    public PlanningContextFacade(WeeklyTrainingPlanQueryService queryService,
                                 WeeklyTrainingPlanCommandService commandService,
                                 WeeklyTrainingPlanRepository planRepository,
                                 AdaptedWeekCommandService adaptedWeekCommandService,
                                 PublishedPlanSafetyService publishedPlanSafety,
                                 PublishedPlanSafetyRepairService publishedPlanRepair) {
        this.queryService = queryService;
        this.commandService = commandService;
        this.planRepository = planRepository;
        this.adaptedWeekCommandService = adaptedWeekCommandService;
        this.publishedPlanSafety = publishedPlanSafety;
        this.publishedPlanRepair = publishedPlanRepair;
    }

    /**
     * Denominador de la semana segun 17.4.
     * <p>
     * Recorre TODAS las versiones de la semana, no solo la activa, y descarta
     * los entrenamientos REPLACED uno a uno. Asi los ya ejecutados antes de una
     * intervencion siguen contando aunque su version haya sido reemplazada: si
     * solo se mirara el plan activo, una intervencion a mitad de semana borraria
     * sesiones reales del denominador.
     */
    @Transactional(readOnly = true)
    public WeekDenominatorView fetchWeekDenominator(Long userId, LocalDate weekStartDate) {
        var plans = planRepository.findByUserIdAndWeekStartDateOrderByPlanVersionAsc(
                userId, weekStartDate);
        if (plans.isEmpty()) return WeekDenominatorView.none();

        var workouts = new ArrayList<WeekDenominatorView.ScheduledWorkout>();
        for (var plan : plans) {
            for (var workout : plan.workoutsView()) {
                if (workout.getStatus() == WorkoutStatus.REPLACED) continue;
                workouts.add(new WeekDenominatorView.ScheduledWorkout(
                        workout.getId(),
                        workout.getScheduledDate(),
                        workout.getExpectedDurationMinutes(),
                        workout.assignedExerciseCount(),
                        workout.getStatus().name(),
                        workout.getSkipReason() == null ? null : workout.getSkipReason().name(),
                        plan.getId()));
            }
        }

        var last = plans.get(plans.size() - 1);
        return new WeekDenominatorView(last.getId(), last.getWeekNumber(), List.copyOf(workouts));
    }

    /**
     * Componentes crudos de la semana (V17). Se resuelve sobre la misma version
     * del plan que fetchWeekVolume —la ultima— para que el equivalente y sus
     * ingredientes describan siempre el mismo plan.
     */
    @Transactional(readOnly = true)
    public WeekVolumeBreakdown fetchWeekBreakdown(Long userId, LocalDate weekStartDate) {
        var plans = planRepository.findByUserIdAndWeekStartDateOrderByPlanVersionAsc(
                userId, weekStartDate);
        if (plans.isEmpty()) return WeekVolumeBreakdown.empty();

        var plan = plans.get(plans.size() - 1);
        return new WeekVolumeBreakdown(plan.plannedRepsTotal(), plan.plannedSecondsTotal());
    }

    /** Volumen en repeticiones equivalentes de la ultima version de una semana (18.1). */
    @Transactional(readOnly = true)
    public int fetchWeekVolume(Long userId, LocalDate weekStartDate, int divisor) {
        var plans = planRepository.findByUserIdAndWeekStartDateOrderByPlanVersionAsc(
                userId, weekStartDate);
        if (plans.isEmpty()) return 0;
        return plans.get(plans.size() - 1).equivalentVolume(divisor);
    }

    /**
     * Volumen de la semana 1 del participante. Es la referencia del tope
     * acumulado de 18.4: ninguna semana puede bajar del 60 % de este numero,
     * por muchas reducciones consecutivas que se acumulen.
     */
    @Transactional(readOnly = true)
    public int fetchBaselineVolume(Long userId, int divisor) {
        return planRepository.findFirstByUserIdAndWeekNumberOrderByPlanVersionAsc(userId, (short) 1)
                .map(plan -> plan.equivalentVolume(divisor))
                .orElse(0);
    }

    /** Cierra la semana: el plan activo pasa a COMPLETED si nadie lo reemplazo. */
    @Transactional
    public void closeWeek(Long userId, LocalDate weekStartDate) {
        commandService.handle(new ClosePlanWeekCommand(userId, weekStartDate));
    }

    /** Genera el plan de la semana nueva con el ajuste ya decidido. */
    /** Genera el plan de la semana nueva con el ajuste ya decidido. */
    /**
     * Genera el plan de la semana nueva con el ajuste ya decidido.
     * <p>
     * SIN @Transactional, deliberadamente. Coordina tres fases y solo la primera
     * y la tercera abren transaccion; la del medio hace hasta dos llamadas HTTP
     * de ~90 s. Poner una transaccion aqui volveria a meter esa espera dentro de
     * una conexion de base de datos, que es lo que provocaba que Postgres la
     * terminara por idle-in-transaction y se perdieran las metricas del ciclo.
     * <p>
     * Tampoco hace falta ya REQUIRES_NEW: al no existir transaccion abarcando la
     * generacion, un fallo suyo no puede marcar como rollback-only la del ciclo
     * semanal.
     */
    public Optional<Long> generateWeeklyPlan(Long userId, LocalDate weekStartDate,
                                             PlanAdjustment adjustment,
                                             BigDecimal previousAdherencePct,
                                             BigDecimal previousAverageRpe) {
        var command = new GenerateWeeklyPlanCommand(userId, weekStartDate, adjustment, false,
                previousAdherencePct, previousAverageRpe);

        var prepared = commandService.prepare(command);
        var generated = commandService.generate(prepared);
        return commandService.persist(prepared, generated)
                .map(WeeklyTrainingPlan::getId);
    }

    /**
     * LA SEMANA DERIVADA DE LA ANTERIOR, corregida o conservada.
     * <p>
     * Separado de {@link #generateWeeklyPlan} a proposito, y no por simetria:
     * la adaptacion necesita cuatro datos de la semana MEDIDA que la generacion
     * inicial no usa -la medicion, el plan del que proviene, la adherencia
     * observada y la cobertura de registro-, y produce una evidencia distinta.
     * Meter los dos caminos en una sola firma la dejaria con cuatro parametros
     * nulos la mitad de las veces. Es la misma division que ya existe dentro,
     * entre {@code persist} y {@code publishAdapted}.
     * <p>
     * ESTOS DATOS ENTRAN POR AQUI porque viven en analytics y planning no tiene
     * ACL hacia analytics; la tarea semanal si los tiene.
     * <p>
     * SIN {@code @Transactional}, igual que la fase 2 de la generacion inicial:
     * hace hasta tres llamadas HTTP de ~90 s y mantenerlas dentro de una
     * transaccion la convierte en idle-in-transaction. Las escrituras las abren
     * {@code publishAdapted} y el registrador de fallos, cada una la suya.
     * <p>
     * {@code replaceExisting} va en {@code true}: una semana adaptada SUSTITUYE
     * a la version anterior de esa misma semana si ya existe, que es justo lo
     * que {@code nextVersionOf} y {@code ux_plan_active} esperan.
     *
     * @return el plan publicado, o vacio si no se pudo publicar ninguno. Vacio
     *         no es un fallo tecnico: es la respuesta correcta cuando conservar
     *         seria inseguro o la propuesta no pasa la validacion.
     */
    public Optional<Long> adaptWeeklyPlan(Long userId, LocalDate weekStartDate,
                                          PlanAdjustment adjustment,
                                          BigDecimal previousAdherencePct,
                                          BigDecimal previousAverageRpe,
                                          MeasuredWeekView medida) {
        var command = new GenerateWeeklyPlanCommand(userId, weekStartDate, adjustment, true,
                previousAdherencePct, previousAverageRpe);

        var prepared = commandService.prepare(command);
        return adaptedWeekCommandService.adaptar(prepared, medida)
                .map(WeeklyTrainingPlan::getId);
    }

    /**
     * Repara por seguridad la semana activa que contiene ese dia, con una
     * version nueva (ver {@link PublishedPlanSafetyRepairService}).
     * <p>
     * NADIE LA LLAMA TODAVIA: no hay endpoint ni tarea. Ejecutarla sobre planes
     * reales es una decision de despliegue que no esta autorizada.
     * Sin {@code @Transactional}: el servicio abre las suyas por fases.
     */
    public PublishedPlanSafetyRepairService.Resultado repairPublishedPlanForSafety(Long userId, LocalDate day,
                                                                                   LocalDate today) {
        return publishedPlanRepair.reparar(userId, day, today);
    }

    @Transactional(readOnly = true)
    public Optional<PlannedWorkoutView> fetchPlannedWorkout(Long plannedWorkoutId) {
        return queryService.handle(new GetPlannedWorkoutByIdQuery(plannedWorkoutId))
                .map(this::toView);
    }

    @Transactional
    public void markWorkoutInProgress(Long plannedWorkoutId) {
        commandService.handle(new StartPlannedWorkoutCommand(plannedWorkoutId));
    }

    @Transactional
    public void recordWorkoutOutcome(Long plannedWorkoutId, String outcome, SkipReason skipReason) {
        commandService.handle(new RecordWorkoutOutcomeCommand(
                plannedWorkoutId, WorkoutStatus.valueOf(outcome), skipReason));
    }

    /**
     * La vista lleva los conflictos de seguridad de HOY: execution no tiene que
     * saber de restricciones para negarse a ejecutar una sesion insegura.
     */
    private PlannedWorkoutView toView(PlannedWorkout workout) {
        var targets = workout.exercisesView().stream()
                .map(exercise -> new PlannedExerciseTarget(
                        exercise.getId(),
                        exercise.getExerciseId(),
                        exercise.getPrescriptionType().name(),
                        exercise.getPlannedSets(),
                        exercise.getPlannedReps(),
                        exercise.getPlannedDurationSeconds()))
                .toList();

        return new PlannedWorkoutView(
                workout.getId(),
                workout.getPlan().getId(),
                workout.getPlan().getUserId(),
                workout.getScheduledDate(),
                workout.getFocusCode().name(),
                workout.getWorkoutName(),
                workout.getExpectedDurationMinutes(),
                workout.getStatus().name(),
                workout.getExpiresAt(),
                targets,
                publishedPlanSafety.conflictosParaRegistrar(workout.getPlan().getUserId(), workout));
    }
}