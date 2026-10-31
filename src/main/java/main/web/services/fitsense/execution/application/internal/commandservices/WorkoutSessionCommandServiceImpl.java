package main.web.services.fitsense.execution.application.internal.commandservices;

import main.web.services.fitsense.execution.application.internal.outboundservices.acl.ExternalConfigurationService;
import main.web.services.fitsense.execution.application.internal.outboundservices.acl.ExternalPlanningService;
import main.web.services.fitsense.execution.domain.exceptions.SessionAlreadyInProgressException;
import main.web.services.fitsense.execution.domain.exceptions.SessionNotFoundException;
import main.web.services.fitsense.execution.domain.exceptions.WorkoutNotAvailableException;
import main.web.services.fitsense.execution.domain.model.aggregates.WorkoutSession;
import main.web.services.fitsense.execution.domain.model.commands.*;
import main.web.services.fitsense.execution.domain.model.valueobjects.CompletionThresholds;
import main.web.services.fitsense.execution.domain.model.valueobjects.ExerciseTarget;
import main.web.services.fitsense.execution.domain.model.valueobjects.SessionStatus;
import main.web.services.fitsense.execution.domain.services.WorkoutSessionCommandService;
import main.web.services.fitsense.execution.infrastructure.persistence.jpa.repositories.WorkoutSessionRepository;
import main.web.services.fitsense.planning.interfaces.acl.PlannedWorkoutView;
import main.web.services.fitsense.shared.domain.exceptions.DomainRuleViolationException;
import main.web.services.fitsense.shared.domain.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class WorkoutSessionCommandServiceImpl implements WorkoutSessionCommandService {

    private final WorkoutSessionRepository sessionRepository;
    private final ExternalPlanningService externalPlanningService;
    private final ExternalConfigurationService externalConfigurationService;

    public WorkoutSessionCommandServiceImpl(WorkoutSessionRepository sessionRepository,
                                            ExternalPlanningService externalPlanningService,
                                            ExternalConfigurationService externalConfigurationService) {
        this.sessionRepository = sessionRepository;
        this.externalPlanningService = externalPlanningService;
        this.externalConfigurationService = externalConfigurationService;
    }

    /**
     * Sesion en vivo. Aqui SI se exige que el entrenamiento siga abierto y sin
     * vencer: no tiene sentido "empezar ahora" algo programado para hace tres
     * dias. Para eso esta el reporte retroactivo.
     */
    @Override
    @Transactional
    public Optional<WorkoutSession> handle(StartWorkoutSessionCommand command) {
        var workout = requireOwnedWorkout(command.userId(), command.plannedWorkoutId());

        if (!workout.isOpen())
            throw WorkoutNotAvailableException.closed(command.plannedWorkoutId(), workout.status());
        if (workout.isExpired(OffsetDateTime.now()))
            throw WorkoutNotAvailableException.expired(command.plannedWorkoutId());
        // Un plan publicado antes de una correccion de seguridad puede contener
        // un ejercicio que hoy no es seguro para esta persona. No se abre: la
        // app lo muestra bloqueado y la sesion espera una version corregida.
        if (workout.blockedForSafety())
            throw WorkoutNotAvailableException.unsafe(command.plannedWorkoutId(), workout.safetyConflicts());

        sessionRepository.findByUserIdAndStatus(command.userId(), SessionStatus.IN_PROGRESS)
                .ifPresent(open -> {
                    throw new SessionAlreadyInProgressException(command.userId());
                });

        short attempt = (short) (sessionRepository.findLastAttemptNumber(command.plannedWorkoutId()) + 1);
        var session = WorkoutSession.start(command.userId(), command.plannedWorkoutId(),
                workout.planId(), attempt);

        var saved = sessionRepository.save(session);
        externalPlanningService.markInProgress(command.plannedWorkoutId());
        return Optional.of(saved);
    }

    @Override
    @Transactional
    public Optional<WorkoutSession> handle(RecordSessionExerciseCommand command) {
        var session = requireOwnedSession(command.userId(), command.sessionId());
        var workout = requireOwnedWorkout(command.userId(), session.getPlannedWorkoutId());

        var target = findTarget(workout, command.plannedExerciseId());
        // Una sesion abierta antes del bloqueo no permite registrar como hecho
        // el ejercicio inseguro; omitirlo, si.
        rechazarSiInseguro(workout, command.plannedExerciseId(), command.skipReason(),
                command.actualSets(), command.actualRepsTotal(), command.actualDurationSeconds());
        session.recordExercise(target, command.actualSets(), command.actualRepsTotal(),
                command.actualDurationSeconds(), command.actualLoadKg(), command.skipReason(),
                externalConfigurationService.thresholds());

        return Optional.of(sessionRepository.save(session));
    }

    @Override
    @Transactional
    public Optional<WorkoutSession> handle(FinishWorkoutSessionCommand command) {
        var session = requireOwnedSession(command.userId(), command.sessionId());
        var workout = requireOwnedWorkout(command.userId(), session.getPlannedWorkoutId());

        var thresholds = externalConfigurationService.thresholds();
        session.finish(command.sessionRpe(), command.satisfaction(), command.activeMinutes(),
                workout.exercises().size(), thresholds);

        return Optional.of(closeAndPropagate(session, thresholds));
    }

    @Override
    @Transactional
    public Optional<WorkoutSession> handle(AbandonWorkoutSessionCommand command) {
        var session = requireOwnedSession(command.userId(), command.sessionId());
        session.abandon();

        var saved = sessionRepository.save(session);
        // Abandonar no cierra el entrenamiento: sigue exigible hasta que venza,
        // porque el usuario todavia puede volver a intentarlo hoy.
        return Optional.of(saved);
    }

    @Override
    @Transactional
    public Optional<WorkoutSession> handle(ReportWorkoutCommand command) {
        var workout = requireOwnedWorkout(command.userId(), command.plannedWorkoutId());

        // report SI admite un entrenamiento ya cerrado: es su razon de ser. Un
        // SKIPPED por vencimiento significa "no se registro", no "no se hizo",
        // y la tarea de las 00:30 los cierra todos cada noche. Sin esto, el
        // reporte retroactivo solo funcionaria el mismo dia, que es justo
        // cuando no hace falta.
        //
        // Lo que si se rechaza es reportar sobre un entrenamiento que ya tiene
        // sesion contando: eso no es un olvido sino una correccion, y para eso
        // esta el reintento, que crea attempt_number 2 y desplaza al anterior.
        //
        // Tampoco sobre un REPLACED: esa version ya no rige y el mismo dia
        // existe en la vigente. Reportarlo cerraria la version vieja y dejaria
        // abierta la nueva, y el dia podria contar dos veces.
        if ("COMPLETED".equals(workout.status()) || "PARTIAL".equals(workout.status())
                || "REPLACED".equals(workout.status()))
            throw WorkoutNotAvailableException.closed(
                    command.plannedWorkoutId(), workout.status());

        if (command.exercises() == null || command.exercises().isEmpty())
            throw new DomainRuleViolationException(
                    "Indica que ejercicios hiciste: un reporte vacio no es un entrenamiento.");

        var thresholds = externalConfigurationService.thresholds();

        short attempt = (short) (sessionRepository.findLastAttemptNumber(command.plannedWorkoutId()) + 1);
        var session = WorkoutSession.reported(command.userId(), command.plannedWorkoutId(),
                workout.planId(), attempt, command.performedAt(),
                externalConfigurationService.retroactiveReportDays());

        for (var reported : command.exercises()) {
            var target = findTarget(workout, reported.plannedExerciseId());
            // Los conflictos llegan tambien para un SKIPPED: reportarlo es
            // registrar ahora un ejercicio que hoy no es seguro.
            rechazarSiInseguro(workout, reported.plannedExerciseId(), reported.skipReason(),
                    reported.actualSets(), reported.actualRepsTotal(), reported.actualDurationSeconds());
            session.recordExercise(target, reported.actualSets(), reported.actualRepsTotal(),
                    reported.actualDurationSeconds(), reported.actualLoadKg(),
                    reported.skipReason(), thresholds);
        }

        session.finish(command.sessionRpe(), command.satisfaction(), command.activeMinutes(),
                workout.exercises().size(), thresholds);

        return Optional.of(closeAndPropagate(session, thresholds));
    }

    // ------------------------------------------------------------------ helpers

    /**
     * Un ejercicio inseguro solo se registra como omitido: con causa y sin
     * trabajo. Basta la causa no: con causa y repeticiones se clasificaria
     * COMPLETED y la causa se descartaria.
     */
    private static void rechazarSiInseguro(PlannedWorkoutView workout, Long plannedExerciseId, Object skipReason,
                                           Short sets, Integer reps, Integer seconds) {
        var causa = workout.safetyConflicts().get(plannedExerciseId);
        boolean conTrabajo = (sets != null && sets > 0) || (reps != null && reps > 0)
                || (seconds != null && seconds > 0);
        if (causa != null && (skipReason == null || conTrabajo))
            throw WorkoutNotAvailableException.unsafeExercise(plannedExerciseId, causa);
    }

    /**
     * Regla del intento valido: solo el ultimo intento finalizado cuenta.
     * <p>
     * El orden importa. ux_session_counted es un indice unico parcial sobre
     * planned_workout_id: hay que desmarcar los intentos anteriores y vaciar el
     * contexto de persistencia ANTES de guardar el nuevo, o la base rechaza el
     * insert por duplicado.
     */
    private WorkoutSession closeAndPropagate(WorkoutSession session, CompletionThresholds thresholds) {
        List<WorkoutSession> previouslyCounted =
                sessionRepository.findByPlannedWorkoutIdAndCountsTowardAdherenceTrue(
                        session.getPlannedWorkoutId());

        for (var previous : previouslyCounted) {
            if (!previous.getId().equals(session.getId())) {
                previous.supersede();
                sessionRepository.saveAndFlush(previous);
            }
        }

        var saved = sessionRepository.saveAndFlush(session);

        // El desenlace del entrenamiento sale del nivel de 17.2, no del estado
        // persistido: una sesion por debajo del 30 % se guarda como PARTIAL pero
        // el entrenamiento del plan se cierra como SKIPPED.
        var outcome = saved.completionLevel(thresholds).workoutOutcome();
        externalPlanningService.recordOutcome(saved.getPlannedWorkoutId(), outcome,
                saved.dominantSkipReason().orElse(null));

        return saved;
    }

    private PlannedWorkoutView requireOwnedWorkout(Long userId, Long plannedWorkoutId) {
        var workout = externalPlanningService.fetchPlannedWorkout(plannedWorkoutId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Entrenamiento planificado", plannedWorkoutId));

        // Mismo 404 que si no existiera: no se confirma la existencia de recursos ajenos.
        if (!workout.userId().equals(userId))
            throw new ResourceNotFoundException("Entrenamiento planificado", plannedWorkoutId);

        return workout;
    }

    /**
     * Con bloqueo de fila: registrar, cerrar y abandonar la misma sesion a la
     * vez (un reintento de la app con mala conexion, la tarea de sesiones
     * colgadas) se ejecutan uno detras de otro. Sin el bloqueo, un registro
     * concurrente con el cierre quedaba fuera del porcentaje, y un abandono
     * concurrente pisaba un cierre ya hecho.
     */
    private WorkoutSession requireOwnedSession(Long userId, Long sessionId) {
        return sessionRepository.findByIdForUpdate(sessionId)
                .filter(session -> session.getUserId().equals(userId))
                .orElseThrow(() -> new SessionNotFoundException(sessionId));
    }

    private ExerciseTarget findTarget(PlannedWorkoutView workout, Long plannedExerciseId) {
        return externalPlanningService.targetsOf(workout).stream()
                .filter(target -> target.plannedExerciseId().equals(plannedExerciseId))
                .findFirst()
                .orElseThrow(() -> new DomainRuleViolationException(
                        "El ejercicio %d no pertenece a este entrenamiento."
                                .formatted(plannedExerciseId)));
    }
}