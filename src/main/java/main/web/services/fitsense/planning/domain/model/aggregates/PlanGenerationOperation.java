package main.web.services.fitsense.planning.domain.model.aggregates;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import main.web.services.fitsense.planning.domain.model.valueobjects.GenerationOperationStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Generacion del primer plan como operacion persistente (V58).
 * <p>
 * Las transiciones validas son PENDING -> PROCESSING -> {COMPLETED, FAILED,
 * NO_FACTIBLE}, y PROCESSING -> PENDING solo para reintentar una ejecucion
 * interrumpida por un reinicio. Un estado terminal no cambia mas.
 */
@Entity
@Table(name = "plan_generation_operations")
public class PlanGenerationOperation {

    public static final String KIND_FIRST_PLAN = "FIRST_PLAN";
    /** V66: una peticion SINCRONA del primer plan (POST /users/me/plan/generate). */
    public static final String KIND_FIRST_PLAN_SYNC = "FIRST_PLAN_SYNC";
    /** V66: el cierre semanal de un participante (generacion o adaptacion de la semana nueva). */
    public static final String KIND_WEEKLY_CYCLE = "WEEKLY_CYCLE";

    @Id
    @Column(name = "operation_id", nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "kind", nullable = false, length = 20)
    private String kind;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private GenerationOperationStatus status;

    @Column(name = "week_start_date")
    private LocalDate weekStartDate;

    @Column(name = "plan_id")
    private Long planId;

    @Column(name = "reason_code", length = 40)
    private String reasonCode;

    @Column(name = "message", length = 500)
    private String message;

    @Column(name = "retryable", nullable = false)
    private boolean retryable;

    @Column(name = "executions", nullable = false)
    private short executions;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /** V59: worker propietario mientras esta PROCESSING (fencing). */
    @Column(name = "lease_owner", length = 80)
    private String leaseOwner;

    /** V59: vence si el propietario deja de latir (proceso muerto); entonces se recupera. */
    @Column(name = "lease_expires_at")
    private OffsetDateTime leaseExpiresAt;

    /** V59: inicio de la primera ejecucion; el tiempo maximo de la operacion se mide desde aqui. */
    @Column(name = "first_started_at")
    private OffsetDateTime firstStartedAt;

    /** V66: identidad logica de FIRST_PLAN_SYNC y WEEKLY_CYCLE; nula en FIRST_PLAN. */
    @Column(name = "operation_key", length = 160)
    private String operationKey;

    /** V66: semana medida de un WEEKLY_CYCLE. */
    @Column(name = "measured_week_start")
    private LocalDate measuredWeekStart;

    protected PlanGenerationOperation() {
        // JPA
    }

    private PlanGenerationOperation(Long userId, LocalDate weekStartDate, OffsetDateTime now) {
        this.id = UUID.randomUUID();
        this.userId = Objects.requireNonNull(userId);
        this.kind = KIND_FIRST_PLAN;
        this.status = GenerationOperationStatus.PENDING;
        this.weekStartDate = weekStartDate;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** Nueva solicitud que un worker ejecutara. */
    public static PlanGenerationOperation pending(Long userId, LocalDate weekStartDate, OffsetDateTime now) {
        return new PlanGenerationOperation(userId, weekStartDate, now);
    }

    /**
     * El participante ya tiene su plan: se registra como completada sin
     * ejecutar nada, para que la app reciba la misma respuesta de siempre.
     */
    public static PlanGenerationOperation alreadyCompleted(Long userId, LocalDate weekStartDate, Long planId,
                                                           OffsetDateTime now) {
        var op = new PlanGenerationOperation(userId, weekStartDate, now);
        op.complete(planId, now);
        return op;
    }

    public void complete(Long planId, OffsetDateTime now) {
        Objects.requireNonNull(planId, "Un COMPLETED necesita el plan publicado");
        requireNotTerminal();
        this.status = GenerationOperationStatus.COMPLETED;
        this.planId = planId;
        this.reasonCode = null;
        this.message = null;
        this.retryable = false;
        finish(now);
    }

    public void fail(GenerationOperationStatus terminal, String reasonCode, String message, boolean retryable,
                     OffsetDateTime now) {
        if (terminal != GenerationOperationStatus.FAILED && terminal != GenerationOperationStatus.NO_FACTIBLE)
            throw new IllegalArgumentException("Solo FAILED o NO_FACTIBLE describen un fallo: " + terminal);
        requireNotTerminal();
        this.status = terminal;
        this.reasonCode = reasonCode;
        this.message = message;
        this.retryable = retryable;
        finish(now);
    }

    private void finish(OffsetDateTime now) {
        this.finishedAt = now;
        this.updatedAt = now;
        // V59: una operacion terminada no pertenece a ningun worker.
        this.leaseOwner = null;
        this.leaseExpiresAt = null;
    }

    private void requireNotTerminal() {
        if (status.isTerminal())
            throw new IllegalStateException("La operacion %s ya termino en %s".formatted(id, status));
    }

    public UUID getId() { return id; }
    public Long getUserId() { return userId; }
    public String getKind() { return kind; }
    public GenerationOperationStatus getStatus() { return status; }
    public LocalDate getWeekStartDate() { return weekStartDate; }
    public Long getPlanId() { return planId; }
    public String getReasonCode() { return reasonCode; }
    public String getMessage() { return message; }
    public boolean isRetryable() { return retryable; }
    public short getExecutions() { return executions; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getStartedAt() { return startedAt; }
    public OffsetDateTime getFinishedAt() { return finishedAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public String getLeaseOwner() { return leaseOwner; }
    public OffsetDateTime getLeaseExpiresAt() { return leaseExpiresAt; }
    public OffsetDateTime getFirstStartedAt() { return firstStartedAt; }
    public String getOperationKey() { return operationKey; }
    public LocalDate getMeasuredWeekStart() { return measuredWeekStart; }
}
