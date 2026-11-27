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
    /** V59: lease del propietario; el latido lo renueva. Vencido = proceso muerto. */
    private final Duration lease;
    /** V59: identidad de ESTE proceso como propietario de las operaciones que ejecuta (fencing). */
    private final String owner;
    /** V59: libro de predicciones (opcional para no romper construcciones directas). */
    private main.web.services.fitsense.planning.infrastructure.generation.ai.DiarioDePredicciones diario;
    private final Duration maxProcessing;
    private final Duration maxQueued;
    private final int maxExecutions;

    private main.web.services.fitsense.planning.application.internal.commandservices.LimiteDeGeneraciones limite;

    /** B5.2: opcional, para que las pruebas que construyen el servicio a mano no lo necesiten. */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void limiteDeGeneraciones(
            main.web.services.fitsense.planning.application.internal.commandservices.LimiteDeGeneraciones limite) {
        this.limite = limite;
    }

    public PlanGenerationOperationService(PlanGenerationOperationRepository operations,
                                          WeeklyTrainingPlanRepository plans,
                                          WeeklyTrainingPlanCommandService commandService,
                                          ExternalIamService iam,
                                          ExternalProfilingService profiling,
                                          FirstPlanGenerationRunner runner,
                                          JdbcTemplate jdbc,
                                          PlatformTransactionManager transactionManager,
                                          org.springframework.beans.factory.ObjectProvider<Clock> clock,
                                          @Value("${fitsense.plan-generation.max-processing-seconds:1800}") long maxProcessingSeconds,
                                          @Value("${fitsense.plan-generation.max-queued-seconds:1800}") long maxQueuedSeconds,
                                          @Value("${fitsense.plan-generation.max-executions:3}") int maxExecutions,
                                          @Value("${fitsense.plan-generation.lease-seconds:90}") long leaseSeconds) {
        this.operations = operations;
        this.plans = plans;
        this.commandService = commandService;
        this.iam = iam;
        this.profiling = profiling;
        this.runner = runner;
        this.jdbc = jdbc;
        this.tx = new TransactionTemplate(transactionManager);
        this.clock = clock.getIfAvailable(Clock::systemUTC);
        this.maxProcessing = Duration.ofSeconds(maxProcessingSeconds);
        this.maxQueued = Duration.ofSeconds(maxQueuedSeconds);
        this.maxExecutions = Math.max(1, maxExecutions);
        this.lease = Duration.ofSeconds(Math.max(1, leaseSeconds));
        this.owner = propietario();
    }

    /** Nombre del host (Railway: el contenedor) y un sufijo aleatorio: dos procesos nunca comparten propietario. */
    private static String propietario() {
        String host;
        try {
            host = java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            host = "desconocido";
        }
        if (host.length() > 40) host = host.substring(0, 40);
        return host + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setDiario(main.web.services.fitsense.planning.infrastructure.generation.ai.DiarioDePredicciones diario) {
        this.diario = diario;
    }

    public String owner() {
        return owner;
    }

    public Duration lease() {
        return lease;
    }

    public Duration maxProcessing() {
        return maxProcessing;
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
            var created = tx.execute(s -> {
                var op = operations.saveAndFlush(PlanGenerationOperation.pending(userId, week, now()));
                // B5.2 (P1-07): el mismo limite por usuario que la ruta sincrona, solo para la operacion que SI se
                // crea. Dentro de la transaccion: una peticion simultanea que pierde la carrera (doble toque) no
                // consume cupo, y si el cupo se agota la operacion no queda creada. 429 DEMASIADAS_GENERACIONES.
                if (limite != null) limite.consumir(userId);
                return op;
            });
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
        var ahora = now();
        // V59: el reclamo fija el propietario y su lease en la misma sentencia; first_started_at solo la primera vez.
        var ids = jdbc.queryForList("""
                UPDATE plan_generation_operations
                   SET status = 'PROCESSING', started_at = ?, updated_at = ?, executions = executions + 1,
                       lease_owner = ?, lease_expires_at = ?, first_started_at = coalesce(first_started_at, ?)
                 WHERE operation_id = (SELECT operation_id FROM plan_generation_operations
                                        WHERE status = 'PENDING'
                                        ORDER BY created_at
                                        FOR UPDATE SKIP LOCKED
                                        LIMIT 1)
                RETURNING operation_id
                """, UUID.class, ahora, ahora, owner, ahora.plus(lease), ahora);
        return ids.stream().findFirst();
    }

    /**
     * Ejecuta una operacion ya reclamada. Nunca lanza: todo resultado queda
     * registrado en la operacion.
     */
    public void execute(UUID operationId) {
        execute(operationId, () -> false);
    }

    /**
     * @param stopping verdadero si el proceso se esta apagando (despliegue):
     *        un fallo en ese momento es la interrupcion del apagado, no un
     *        resultado de la generacion, y no se registra. El worker la
     *        devuelve a la cola ({@link #requeueOnShutdown}).
     */
    public void execute(UUID operationId, java.util.function.BooleanSupplier stopping) {
        var op = tx.execute(s -> operations.findById(operationId).orElse(null));
        if (op == null || op.getStatus() != GenerationOperationStatus.PROCESSING) return;
        Long userId = op.getUserId();
        try {
            // V59: el pipeline corre con el libro de predicciones de ESTA operacion. Si es una reanudacion, cada
            // intento consulta primero lo ya pedido a Replicate en lugar de pedirlo otra vez.
            // Antes de cada prediccion y antes de publicar: la operacion sigue siendo de este worker y el proceso no se
            // esta apagando. Un proceso que se apaga no envia ni publica (tampoco el respaldo del motor de reglas): la
            // operacion vuelve a la cola y la reanuda la instancia siguiente con el libro.
            Runnable guarda = () -> {
                if (stopping.getAsBoolean())
                    throw new OperationAbandonedException("el proceso se esta apagando: no se envia ni se publica");
                ensureStillProcessing(operationId);
            };
            var planId = main.web.services.fitsense.planning.infrastructure.generation.ai.DiarioDePredicciones
                    .durante(operationId, userId, guarda, () -> runner.run(userId, guarda));
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
            if (stopping.getAsBoolean()) {
                log.warn("Generacion inicial {} interrumpida por el apagado: se reanudara ({})", operationId, error.getMessage());
                return;
            }
            record(operationId, userId, GenerationFailureClassifier.classify(error), error);
        } finally {
            registrarPredicciones(operationId);
        }
    }

    /** V59: recuento de predicciones de la operacion (coste potencial), en una linea por ejecucion. */
    private void registrarPredicciones(UUID operationId) {
        if (diario == null) return;
        try {
            var r = diario.recuento(operationId);
            log.info("GENERACION_ASINCRONA_PREDICCIONES operacion={} iniciadas={} reutilizadas={} completadas={}"
                            + " fallidas={} canceladas={} vencidas={} no_confirmadas={} descartadas={}", operationId,
                    r.iniciadas(), r.reutilizadas(), r.completadas(), r.fallidas(), r.canceladas(), r.vencidas(),
                    r.noConfirmadas(), r.descartadas());
        } catch (RuntimeException e) {
            log.warn("No se pudo contar las predicciones de la operacion {}: {}", operationId, e.getMessage());
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

    /**
     * Apagado ordenado: las operaciones que este proceso no pudo terminar
     * vuelven a PENDING sin gastar un intento (la interrupcion no fue suya) y
     * la siguiente instancia las reclama al arrancar.
     */
    public int requeueOnShutdown(java.util.Collection<UUID> interrupted) {
        if (interrupted.isEmpty()) return 0;
        int n = jdbc.update("""
                UPDATE plan_generation_operations
                   SET status = 'PENDING', started_at = NULL, updated_at = ?, executions = GREATEST(executions - 1, 0),
                       lease_owner = NULL, lease_expires_at = NULL
                 WHERE status = 'PROCESSING' AND operation_id = ANY(?) AND lease_owner = ?
                """, now(), interrupted.toArray(UUID[]::new), owner);
        if (n > 0) log.warn("Generacion inicial: {} operacion(es) devuelta(s) a la cola por el apagado", n);
        return n;
    }

    /** Antes de publicar: si la operacion ya no es PROCESSING, no se publica nada. */
    void ensureStillProcessing(UUID operationId) {
        // V59 (fencing): no basta con PROCESSING; tiene que seguir siendo NUESTRA. Si el lease vencio y otro worker la
        // reclamo, este no publica: lo hara el propietario actual (con las mismas predicciones, por el libro).
        // FOR UPDATE: dentro de la transaccion de persist (PipelineFirstPlanGenerationRunner), la fila queda bloqueada
        // hasta el commit, asi que nadie puede quitarle el lease a este worker entre la comprobacion y la publicacion.
        // Fuera de una transaccion (guarda antes de cada prediccion) es una lectura puntual.
        var fila = jdbc.queryForList("SELECT status, lease_owner FROM plan_generation_operations"
                + " WHERE operation_id = ? FOR UPDATE", operationId);
        if (fila.isEmpty() || !GenerationOperationStatus.PROCESSING.name().equals(fila.get(0).get("status"))
                || !owner.equals(fila.get(0).get("lease_owner")))
            throw new OperationAbandonedException("la operacion ya no esta en curso para este worker (" + fila + ")");
    }

    /**
     * Latido de las operaciones que este proceso esta ejecutando: mientras
     * llegue, la operacion no se considera huerfana aunque tarde minutos.
     */
    public int heartbeat(java.util.Collection<UUID> running) {
        if (running.isEmpty()) return 0;
        var ahora = now();
        // V59: solo se renueva el lease de las operaciones de las que este proceso sigue siendo propietario.
        return jdbc.update("UPDATE plan_generation_operations SET updated_at = ?, lease_expires_at = ? "
                        + "WHERE status = 'PROCESSING' AND operation_id = ANY(?) AND lease_owner = ?",
                ahora, ahora.plus(lease), running.toArray(UUID[]::new), owner);
    }

    /** Aplica el cierre solo si la operacion sigue PROCESSING (la recuperacion pudo cerrarla). */
    private void finish(UUID operationId, java.util.function.Consumer<PlanGenerationOperation> change) {
        tx.executeWithoutResult(s -> operations.findById(operationId)
                .filter(o -> o.getStatus() == GenerationOperationStatus.PROCESSING)
                // V59 (fencing): solo el propietario cierra; otro worker con un lease vencido no pisa el resultado.
                .filter(o -> owner.equals(o.getLeaseOwner()))
                .ifPresent(o -> {
                    change.accept(o);
                    operations.save(o);
                }));
    }

    /**
     * Barrido periodico del worker.
     * <ul>
     *   <li><b>Huerfanas</b> (V59): PROCESSING con el lease VENCIDO, es decir, sin latido de su propietario desde hace
     *   {@code lease-seconds}
     *   (el proceso que la ejecutaba murio o se reinicio). Vuelve a la cola mientras
     *   executions &lt; maxExecutions; la reanudacion repite el pipeline con el libro de
     *   predicciones, asi que no paga otra vez lo ya pedido. Despues se cierra como
     *   FAILED EJECUCION_INTERRUMPIDA reintentable. Una generacion lenta pero viva
     *   late en su propio hilo (no en el del planificador) y nunca se reencola.</li>
     *   <li><b>Tiempo maximo de la OPERACION</b>: PROCESSING desde hace mas de
     *   {@code maxProcessing} contados desde la PRIMERA ejecucion, o PENDING desde hace mas
     *   de {@code maxQueued}: FAILED TIEMPO_EXCEDIDO reintentable. Es una red de seguridad
     *   por encima del peor caso del pipeline (lo comprueba PoliticaTiemposGeneracion al
     *   arrancar). Si aun asi la generacion seguia viva, no publica (fencing), pero sus
     *   predicciones quedan en el libro y la nueva peticion del usuario las reutiliza.</li>
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
                       finished_at = ?, updated_at = ?, lease_owner = NULL, lease_expires_at = NULL
                 WHERE (status = 'PROCESSING' AND coalesce(first_started_at, started_at) < ?)
                    OR (status = 'PENDING' AND created_at < ?)
                """, timedOut.reasonCode(), timedOut.message(), now, now,
                now.minus(maxProcessing), now.minus(maxQueued));
        // V59: huerfana = lease VENCIDO (su propietario dejo de latir), no "updated_at antiguo". Un lease NULO solo
        // puede ser una PROCESSING anterior a V59 (ck_pgo_lease lo impide en toda escritura nueva): su proceso ya no
        // existe tras el despliegue, asi que tambien es huerfana.

        int requeued = jdbc.update("""
                UPDATE plan_generation_operations
                   SET status = 'PENDING', started_at = NULL, updated_at = ?, lease_owner = NULL, lease_expires_at = NULL
                 WHERE status = 'PROCESSING' AND (lease_expires_at IS NULL OR lease_expires_at < ?) AND executions < ?
                """, now, now, maxExecutions);
        var interrupted = GenerationFailureClassifier.interrupted();
        int failed = jdbc.update("""
                UPDATE plan_generation_operations
                   SET status = 'FAILED', reason_code = ?, message = ?, retryable = TRUE,
                       finished_at = ?, updated_at = ?, lease_owner = NULL, lease_expires_at = NULL
                 WHERE status = 'PROCESSING' AND (lease_expires_at IS NULL OR lease_expires_at < ?) AND executions >= ?
                """, interrupted.reasonCode(), interrupted.message(), now, now, now, maxExecutions);
        if (expired + requeued + failed > 0)
            log.warn("Generacion inicial: {} por tiempo maximo, {} huerfana(s) en cola de nuevo, {} interrumpida(s)",
                    expired, requeued, failed);
        return expired + requeued + failed;
    }
}
