package main.web.services.fitsense.planning.domain.model.aggregates;

import jakarta.persistence.*;
import main.web.services.fitsense.shared.domain.exceptions.DomainRuleViolationException;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * P-02 · MEMORIA PERSISTENTE DEL DOLOR. Un ejercicio que provoco dolor a un usuario queda excluido
 * para el hasta una reactivacion explicita y segura. No caduca: ni el paso de las semanas, ni
 * cumplir entrenamientos, ni la ausencia de nuevos reportes la levantan.
 * <p>
 * No es parte del perfil: el perfil esta congelado y sus {@code blocked_exercise_ids} no se tocan.
 * La identidad es el EVENTO ORIGINAL (usuario, ejercicio, sesion de origen), asi que registrar dos
 * veces el mismo dolor no duplica, y un dolor ya reactivado no se resucita al releer la semana.
 */
@Entity
@Table(name = "user_safety_exclusions")
public class UserSafetyExclusion {

    public enum Reason { DOLOR_IDENTIFICADO, DOLOR_SESION_SIN_IDENTIFICAR }

    public enum RecordedBy { OMISION, CICLO_SEMANAL, SESION }

    public enum Status { ACTIVA, REACTIVADA }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exclusion_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "exercise_id", nullable = false)
    private Long exerciseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 40)
    private Reason reason;

    @Column(name = "source_plan_id", nullable = false)
    private Long sourcePlanId;

    @Column(name = "source_planned_workout_id", nullable = false)
    private Long sourcePlannedWorkoutId;

    @Column(name = "source_planned_exercise_id")
    private Long sourcePlannedExerciseId;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "recorded_by", nullable = false, length = 30)
    private RecordedBy recordedBy;

    @Column(name = "recorded_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime recordedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.ACTIVA;

    @Column(name = "reactivated_at")
    private OffsetDateTime reactivatedAt;

    @Column(name = "reactivation_reason", length = 300)
    private String reactivationReason;

    @Column(name = "reactivation_evidence", length = 500)
    private String reactivationEvidence;

    protected UserSafetyExclusion() {
        // JPA
    }

    public UserSafetyExclusion(Long userId, Long exerciseId, Reason reason, Long sourcePlanId,
                               Long sourcePlannedWorkoutId, Long sourcePlannedExerciseId, LocalDate eventDate,
                               RecordedBy recordedBy) {
        if (userId == null || exerciseId == null || reason == null || sourcePlanId == null
                || sourcePlannedWorkoutId == null || eventDate == null || recordedBy == null)
            throw new IllegalArgumentException("Una exclusion por dolor necesita usuario, ejercicio, motivo y evento.");
        this.userId = userId;
        this.exerciseId = exerciseId;
        this.reason = reason;
        this.sourcePlanId = sourcePlanId;
        this.sourcePlannedWorkoutId = sourcePlannedWorkoutId;
        this.sourcePlannedExerciseId = sourcePlannedExerciseId;
        this.eventDate = eventDate;
        this.recordedBy = recordedBy;
        this.status = Status.ACTIVA;
    }

    /**
     * La reactivacion EXPLICITA. Solo la llama el servicio de reactivacion despues de su control de
     * seguridad; aqui solo se garantiza que deja constancia y que no se reactiva dos veces.
     */
    public void reactivar(OffsetDateTime cuando, String motivo, String evidencia) {
        if (status != Status.ACTIVA)
            throw new DomainRuleViolationException("La exclusion %d ya no esta activa.".formatted(id));
        if (evidencia == null || evidencia.isBlank())
            throw new IllegalArgumentException("Reactivar exige la evidencia de la accion explicita.");
        this.status = Status.REACTIVADA;
        this.reactivatedAt = cuando;
        this.reactivationReason = motivo == null || motivo.isBlank() ? null
                : motivo.strip().substring(0, Math.min(300, motivo.strip().length()));
        this.reactivationEvidence = evidencia.substring(0, Math.min(500, evidencia.length()));
    }

    public boolean activa() {
        return status == Status.ACTIVA;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getExerciseId() { return exerciseId; }
    public Reason getReason() { return reason; }
    public Long getSourcePlanId() { return sourcePlanId; }
    public Long getSourcePlannedWorkoutId() { return sourcePlannedWorkoutId; }
    public Long getSourcePlannedExerciseId() { return sourcePlannedExerciseId; }
    public LocalDate getEventDate() { return eventDate; }
    public RecordedBy getRecordedBy() { return recordedBy; }
    public OffsetDateTime getRecordedAt() { return recordedAt; }
    public Status getStatus() { return status; }
    public OffsetDateTime getReactivatedAt() { return reactivatedAt; }
    public String getReactivationReason() { return reactivationReason; }
    public String getReactivationEvidence() { return reactivationEvidence; }
}
