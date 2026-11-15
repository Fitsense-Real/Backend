package main.web.services.fitsense.planning.application.internal.generation;

import main.web.services.fitsense.planning.application.internal.outboundservices.acl.ExternalIamService;
import main.web.services.fitsense.planning.application.internal.outboundservices.acl.ExternalProfilingService;
import main.web.services.fitsense.planning.domain.exceptions.PlanAlreadyExistsException;
import main.web.services.fitsense.planning.domain.model.aggregates.PlanGenerationOperation;
import main.web.services.fitsense.planning.domain.model.aggregates.WeeklyTrainingPlan;
import main.web.services.fitsense.planning.domain.model.valueobjects.GenerationOperationStatus;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
import main.web.services.fitsense.planning.infrastructure.persistence.jpa.repositories.PlanGenerationOperationRepository;
import main.web.services.fitsense.planning.infrastructure.persistence.jpa.repositories.WeeklyTrainingPlanRepository;
import main.web.services.fitsense.shared.domain.exceptions.DomainRuleViolationException;
import main.web.services.fitsense.shared.domain.exceptions.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Generacion del primer plan como operacion asincrona (V58).
 * <p>
 * La peticion HTTP solo registra la operacion; un worker la reclama con
 * {@code FOR UPDATE SKIP LOCKED} y ejecuta el pipeline de siempre a traves de
 * {@link FirstPlanGenerationRunner}. Ninguna transaccion queda abierta durante
 * la llamada a la IA.
 */
@Service
public class PlanGenerationOperationService {

    private static final Logger log = LoggerFactory.getLogger(PlanGenerationOperationService.class);
    static final List<GenerationOperationStatus> ACTIVE =
            List.of(GenerationOperationStatus.PENDING, GenerationOperationStatus.PROCESSING);

    /** Resultado de una solicitud: {@code created=false} si se devolvio una existente. */
    public record Requested(PlanGenerationOperation operation, boolean created) {}

    private final PlanGenerationOperationRepository operations;
    private final WeeklyTrainingPlanRepository plans;
    private final WeeklyTrainingPlanCommandService commandService;
    private final ExternalIamService iam;
    private final ExternalProfilingService profiling;
    private final FirstPlanGenerationRunner runner;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final Clock clock;
    private final Duration staleAfter;
    private final Duration maxProcessing;
    private final Duration maxQueued;
    private final int maxExecutions;

    public PlanGenerationOperationService(PlanGenerationOperationRepository operations,
                                          WeeklyTrainingPlanRepository plans,
                                          WeeklyTrainingPlanCommandService commandService,
                                          ExternalIamService iam,
                                          ExternalProfilingService profiling,
                                          FirstPlanGenerationRunner runner,
                                          JdbcTemplate jdbc,
                                          PlatformTransactionManager transactionManager,
                                          org.springframework.beans.factory.ObjectProvider<Clock> clock,
                                          @Value("${fitsense.plan-generation.stale-seconds:180}") long staleSeconds,
                                          @Value("${fitsense.plan-generation.max-processing-seconds:600}") long maxProcessingSeconds,
                                          @Value("${fitsense.plan-generation.max-queued-seconds:1800}") long maxQueuedSeconds,
                                          @Value("${fitsense.plan-generation.max-executions:2}") int maxExecutions) {
        this.operations = operations;
        this.plans = plans;
        this.commandService = commandService;
        this.iam = iam;
        this.profiling = profiling;
        this.runner = runner;
        this.jdbc = jdbc;
        this.tx = new TransactionTemplate(transactionManager);
        this.clock = clock.getIfAvailable(Clock::systemUTC);
        this.staleAfter = Duration.ofSeconds(staleSeconds);
        this.maxProcessing = Duration.ofSeconds(maxProcessingSeconds);
        this.maxQueued = Duration.ofSeconds(maxQueuedSeconds);
        this.maxExecutions = Math.max(1, maxExecutions);
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }

    // ------------------------------------------------------------ peticiones

    /**
     * Registra la generacion del primer plan, o devuelve la que ya existe.
     * <ul>
     *   <li>Hay una operacion PENDING/PROCESSING: se devuelve esa (doble toque,
     *   reintento de red). No hay una segunda llamada a la IA.</li>
     *   <li>Ya tiene plan activo (esta semana o la siguiente): operacion
     *   COMPLETED con ese plan, sin generar nada.</li>
     *   <li>Si no: valida perfil y elegibilidad y registra una PENDING.</li>
     * </ul>
     */
    public Requested request(Long userId) {
        var existing = tx.execute(s -> existingFor(userId));
        if (existing != null) return existing;
        validateCanGenerate(userId);
        var week = commandService.firstPlanWeekStart(userId);
        try {
            var created = tx.execute(s -> operations.saveAndFlush(PlanGenerationOperation.pending(userId, week, now())));
            log.info("Generacion inicial {} registrada para el usuario {} (semana {})", created.getId(), userId, week);
            return new Requested(created, true);
        } catch (DataIntegrityViolationException race) {
            // Otra peticion simultanea gano ux_pgo_una_activa_por_usuario: se devuelve la suya.
            return tx.execute(s -> operations.findFirstByUserIdAndStatusIn(userId, ACTIVE)
                    .map(op -> new Requested(op, false))
                    .orElseThrow(() -> race));
        }
    }

    private Requested existingFor(Long userId) {
        var active = operations.findFirstByUserIdAndStatusIn(userId, ACTIVE);
        if (active.isPresent()) return new Requested(active.get(), false);
        return activePlan(userId)
                .map(plan -> operations.findFirstByUserIdOrderByCreatedAtDesc(userId)
                        // La ultima operacion ya es la que publico ese plan: no se registra otra.
                        .filter(op -> op.getStatus() == GenerationOperationStatus.COMPLETED
                                && plan.getId().equals(op.getPlanId()))
                        .orElseGet(() -> operations.saveAndFlush(PlanGenerationOperation.alreadyCompleted(
                                userId, plan.getWeekStartDate(), plan.getId(), now()))))
                .map(op -> new Requested(op, false))
                .orElse(null);
    }

    /** El mismo criterio que GET /users/me/plan/current: esta semana local o la siguiente. */
    private Optional<WeeklyTrainingPlan> activePlan(Long userId) {
        var today = LocalDate.now(clock.withZone(iam.timezoneOf(userId)));
        return plans.findActiveOn(userId, today).or(() -> plans.findActiveOn(userId, today.plusWeeks(1)));
    }

    private void validateCanGenerate(Long userId) {
        if (profiling.fetchProfile(userId).isEmpty())
            throw new DomainRuleViolationException(GenerationFailureClassifier.TEXTO_PERFIL_INCOMPLETO);
        if (!iam.isEligibleForPlanGeneration(userId))
            throw new DomainRuleViolationException(GenerationFailureClassifier.TEXTO_NO_GENERABLE);
    }

    /** Solo el dueno la ve; para cualquier otro usuario no existe (404). */
    public PlanGenerationOperation find(Long userId, UUID operationId) {
        return operations.findByIdAndUserId(operationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Operacion de generacion", operationId));
    }

    public Optional<PlanGenerationOperation> latest(Long userId) {
        return operations.findFirstByUserIdOrderByCreatedAtDesc(userId);
    }

    // ---------------------------------------------------------------- worker

    /**
     * Reclama la PENDING mas antigua y la pasa a PROCESSING en una sola
     * sentencia. SKIP LOCKED: dos workers (o dos instancias) nunca toman la
     * misma operacion.
     */
    public Optional<UUID> claimNext() {
        var ids = jdbc.queryForList("""
                UPDATE plan_generation_operations
                   SET status = 'PROCESSING', started_at = ?, updated_at = ?, executions = executions + 1
                 WHERE operation_id = (SELECT operation_id FROM plan_generation_operations
                                        WHERE status = 'PENDING'
                                        ORDER BY created_at
                                        FOR UPDATE SKIP LOCKED
                                        LIMIT 1)
                RETURNING operation_id
                """, UUID.class, now(), now());
        return ids.stream().findFirst();
    }

    /**
     * Ejecuta una operacion ya reclamada. Nunca lanza: todo resultado queda
     * registrado en la operacion.
     */
    public void execute(UUID operationId) {
        var op = tx.execute(s -> operations.findById(operationId).orElse(null));
        if (op == null || op.getStatus() != GenerationOperationStatus.PROCESSING) return;
        Long userId = op.getUserId();
        try {
            var planId = runner.run(userId, () -> ensureStillProcessing(operationId));
            finish(operationId, o -> o.complete(planId, now()));
            log.info("Generacion inicial {} completada: plan {} del usuario {}", operationId, planId, userId);
        } catch (OperationAbandonedException abandoned) {
            // El barrido la cerro (tiempo maximo): su resultado no se publica ni se registra.
            log.warn("Generacion inicial {} descartada antes de publicar: {}", operationId, abandoned.getMessage());
        } catch (PlanAlreadyExistsException already) {
            // Otro camino (el endpoint sincrono, otra operacion) publico primero: es el mismo resultado.
            var plan = tx.execute(s -> activePlan(userId).orElse(null));
            if (plan != null) {
                finish(operationId, o -> o.complete(plan.getId(), now()));
            } else {
                record(operationId, userId, GenerationFailureClassifier.classify(already), already);
            }
        } catch (RuntimeException error) {
            record(operationId, userId, GenerationFailureClassifier.classify(error), error);
        }
    }

    private void record(UUID operationId, Long userId, GenerationFailureClassifier.Outcome outcome, Throwable error) {
        if (outcome.status() == GenerationOperationStatus.NO_FACTIBLE) {
            log.info("Generacion inicial {} NO_FACTIBLE para el usuario {}: {} ({})",
                    operationId, userId, outcome.reasonCode(), error.getMessage());
        } else {
            log.error("Generacion inicial {} FAILED para el usuario {}: {}", operationId, userId, error.getMessage(), error);
        }
        finish(operationId, o -> o.fail(outcome.status(), outcome.reasonCode(), outcome.message(), outcome.retryable(), now()));
    }

    /** Antes de publicar: si la operacion ya no es PROCESSING, no se publica nada. */
    void ensureStillProcessing(UUID operationId) {
        var status = jdbc.queryForList(
                "SELECT status FROM plan_generation_operations WHERE operation_id = ?", String.class, operationId);
        if (status.isEmpty() || !GenerationOperationStatus.PROCESSING.name().equals(status.get(0)))
            throw new OperationAbandonedException("la operacion ya no esta en curso (" + status + ")");
    }

    /**
     * Latido de las operaciones que este proceso esta ejecutando: mientras
     * llegue, la operacion no se considera huerfana aunque tarde minutos.
     */
    public int heartbeat(java.util.Collection<UUID> running) {
        if (running.isEmpty()) return 0;
        return jdbc.update("UPDATE plan_generation_operations SET updated_at = ? "
                        + "WHERE status = 'PROCESSING' AND operation_id = ANY(?)",
                now(), running.toArray(UUID[]::new));
    }

    /** Aplica el cierre solo si la operacion sigue PROCESSING (la recuperacion pudo cerrarla). */
    private void finish(UUID operationId, java.util.function.Consumer<PlanGenerationOperation> change) {
        tx.executeWithoutResult(s -> operations.findById(operationId)
                .filter(o -> o.getStatus() == GenerationOperationStatus.PROCESSING)
                .ifPresent(o -> {
                    change.accept(o);
                    operations.save(o);
                }));
    }

    /**
     * Barrido periodico del worker.
     * <ul>
     *   <li><b>Huerfanas</b>: PROCESSING sin latido desde hace {@code staleAfter}
     *   (el proceso que la ejecutaba murio o se reinicio). Se vuelve a poner en
     *   cola una sola vez (executions &lt; maxExecutions); despues se cierra como
     *   FAILED EJECUCION_INTERRUMPIDA reintentable. Una generacion lenta pero
     *   viva sigue latiendo y nunca se reencola.</li>
     *   <li><b>Tiempo maximo</b>: PROCESSING desde hace mas de {@code maxProcessing},
     *   o PENDING desde hace mas de {@code maxQueued}: FAILED TIEMPO_EXCEDIDO
     *   reintentable. Si la generacion sigue viva, su resultado se descarta
     *   antes de publicar ({@link #ensureStillProcessing}).</li>
     * </ul>
     * Asi la app nunca espera para siempre.
     *
     * @return operaciones recuperadas o cerradas
     */
    public int recoverStale() {
        var now = now();
        var timedOut = GenerationFailureClassifier.timedOut();
        int expired = jdbc.update("""
                UPDATE plan_generation_operations
                   SET status = 'FAILED', reason_code = ?, message = ?, retryable = TRUE,
                       finished_at = ?, updated_at = ?
                 WHERE (status = 'PROCESSING' AND started_at < ?)
                    OR (status = 'PENDING' AND created_at < ?)
                """, timedOut.reasonCode(), timedOut.message(), now, now,
                now.minus(maxProcessing), now.minus(maxQueued));
        var stale = now.minus(staleAfter);
        int requeued = jdbc.update("""
                UPDATE plan_generation_operations
                   SET status = 'PENDING', started_at = NULL, updated_at = ?
                 WHERE status = 'PROCESSING' AND updated_at < ? AND executions < ?
                """, now, stale, maxExecutions);
        var interrupted = GenerationFailureClassifier.interrupted();
        int failed = jdbc.update("""
                UPDATE plan_generation_operations
                   SET status = 'FAILED', reason_code = ?, message = ?, retryable = TRUE,
                       finished_at = ?, updated_at = ?
                 WHERE status = 'PROCESSING' AND updated_at < ? AND executions >= ?
                """, interrupted.reasonCode(), interrupted.message(), now, now, stale, maxExecutions);
        if (expired + requeued + failed > 0)
            log.warn("Generacion inicial: {} por tiempo maximo, {} huerfana(s) en cola de nuevo, {} interrumpida(s)",
                    expired, requeued, failed);
        return expired + requeued + failed;
    }
}
