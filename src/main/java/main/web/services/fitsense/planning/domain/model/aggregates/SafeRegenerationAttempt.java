package main.web.services.fitsense.planning.domain.model.aggregates;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * P-03 · la traza de una regeneracion segura dentro del mismo ciclo, tras una adaptacion por dolor
 * que quedo NO_FACTIBLE. Una fila por medicion semanal: es tambien la llave de idempotencia.
 */
@Entity
@Table(name = "safe_regeneration_attempts")
public class SafeRegenerationAttempt {

    public enum Outcome { PUBLICADO, NO_FACTIBLE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "regeneration_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "weekly_metric_id", nullable = false)
    private Long weeklyMetricId;

    @Column(name = "measured_plan_id", nullable = false)
    private Long measuredPlanId;

    @Column(name = "new_week_start_date", nullable = false)
    private LocalDate newWeekStartDate;

    @Column(name = "adaptation_attempt_id")
    private Long adaptationAttemptId;

    @Column(name = "adaptation_rejection")
    private String adaptationRejection;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "excluded_exercise_ids", nullable = false, columnDefinition = "bigint[]")
    private Long[] excludedExerciseIds;

    @Column(name = "source", nullable = false, length = 20)
    private String source = "RULES";

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 20)
    private Outcome outcome;

    @Column(name = "resulting_plan_id")
    private Long resultingPlanId;

    @Column(name = "validation_problems")
    private String validationProblems;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected SafeRegenerationAttempt() {
        // JPA
    }

    public SafeRegenerationAttempt(Long userId, Long weeklyMetricId, Long measuredPlanId, LocalDate newWeekStartDate,
                                   Long adaptationAttemptId, String adaptationRejection,
                                   List<Long> excluidos, Outcome outcome, Long resultingPlanId,
                                   String validationProblems) {
        this.userId = userId;
        this.weeklyMetricId = weeklyMetricId;
        this.measuredPlanId = measuredPlanId;
        this.newWeekStartDate = newWeekStartDate;
        this.adaptationAttemptId = adaptationAttemptId;
        this.adaptationRejection = adaptationRejection;
        this.excludedExerciseIds = excluidos.toArray(Long[]::new);
        this.outcome = outcome;
        this.resultingPlanId = resultingPlanId;
        this.validationProblems = validationProblems;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getWeeklyMetricId() { return weeklyMetricId; }
    public Long getMeasuredPlanId() { return measuredPlanId; }
    public LocalDate getNewWeekStartDate() { return newWeekStartDate; }
    public Long getAdaptationAttemptId() { return adaptationAttemptId; }
    public String getAdaptationRejection() { return adaptationRejection; }
    public List<Long> getExcludedExerciseIds() { return List.of(excludedExerciseIds); }
    public String getSource() { return source; }
    public Outcome getOutcome() { return outcome; }
    public Long getResultingPlanId() { return resultingPlanId; }
    public String getValidationProblems() { return validationProblems; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
