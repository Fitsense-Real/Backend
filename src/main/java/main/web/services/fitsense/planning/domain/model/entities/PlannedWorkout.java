package main.web.services.fitsense.planning.domain.model.entities;

import jakarta.persistence.*;
import lombok.Getter;
import main.web.services.fitsense.planning.domain.model.aggregates.WeeklyTrainingPlan;
import main.web.services.fitsense.planning.domain.model.valueobjects.PrescriptionType;
import main.web.services.fitsense.planning.domain.model.valueobjects.WorkoutFocus;
import main.web.services.fitsense.planning.domain.model.valueobjects.WorkoutStatus;
import main.web.services.fitsense.shared.domain.exceptions.DomainRuleViolationException;
import main.web.services.fitsense.shared.domain.model.valueobjects.ContextoFaltaDeTiempo;
import main.web.services.fitsense.shared.domain.model.valueobjects.SkipReason;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Un entrenamiento indicado para un dia concreto. Entidad dentro del agregado
 * WeeklyTrainingPlan, no agregado propio: la unidad de consistencia es la
 * version de la semana.
 */
@Getter
@Entity
@Table(name = "planned_workouts")
public class PlannedWorkout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "planned_workout_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private WeeklyTrainingPlan plan;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "focus_code", nullable = false, length = 30)
    private WorkoutFocus focusCode;

    @Column(name = "workout_name", nullable = false, length = 150)
    private String workoutName;

    /** Peso de esta sesion en la adherencia ponderada. */
    @Column(name = "expected_duration_minutes", nullable = false)
    private Short expectedDurationMinutes;

    @Column(name = "display_order", nullable = false)
    private Short displayOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WorkoutStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "skip_reason", length = 40)
    private SkipReason skipReason;

    /**
     * Respuesta opcional tras omitir por falta de tiempo (V42, DISENO_ADAPTACION_CONTEXTUAL v0.3).
     * Null si no respondio o si la causa es otra.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "lack_of_time_context", length = 30)
    private ContextoFaltaDeTiempo lackOfTimeContext;

    /** Minutos que la persona suele tener para ESTE entrenamiento; solo con MENOS_TIEMPO_HABITUAL. */
    @Column(name = "declared_available_minutes")
    private Short declaredAvailableMinutes;

    /** 23:59 hora local del dia programado. Define que entrenamientos son exigibles. */
    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /**
     * V31. El entrenamiento de la semana anterior del que proviene este.
     * <p>
     * NULL en un plan que no deriva de nada, que es todo lo anterior a V31.
     * Lo rellena quien construye el plan derivado AL COPIAR, con
     * {@link #linkSource(Long)}: la relacion se construye, no se deduce al
     * final por fecha, orden o enfoque, porque los tres cambian o se repiten.
     */
    @Column(name = "source_planned_workout_id")
    private Long sourcePlannedWorkoutId;

    @OneToMany(mappedBy = "plannedWorkout", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("exerciseOrder ASC")
    private List<PlannedWorkoutExercise> exercises = new ArrayList<>();

    protected PlannedWorkout() {
        // JPA
    }

    public PlannedWorkout(WeeklyTrainingPlan plan, LocalDate scheduledDate, WorkoutFocus focusCode,
                          String workoutName, int expectedDurationMinutes, short displayOrder,
                          OffsetDateTime expiresAt) {
        this.plan = plan;
        this.scheduledDate = scheduledDate;
        this.focusCode = focusCode;
        this.workoutName = workoutName;
        this.expectedDurationMinutes = (short) expectedDurationMinutes;
        this.displayOrder = displayOrder;
        this.expiresAt = expiresAt;
        this.status = WorkoutStatus.SCHEDULED;
    }

    public PlannedWorkoutExercise addExercise(Long exerciseId, PrescriptionType prescriptionType,
                                              Short plannedSets, Short plannedReps,
                                              Integer plannedDurationSeconds, BigDecimal targetLoadKg,
                                              Short restSeconds, String notes) {
        var order = (short) (exercises.size() + 1);
        var exercise = new PlannedWorkoutExercise(this, exerciseId, order, prescriptionType,
                plannedSets, plannedReps, plannedDurationSeconds, targetLoadKg, restSeconds, notes);
        exercises.add(exercise);
        return exercise;
    }

    public List<PlannedWorkoutExercise> exercisesView() {
        return Collections.unmodifiableList(exercises);
    }

    // ------------------------------------------------------------ transiciones

    public void markInProgress() {
        if (status.isFinished())
            throw new DomainRuleViolationException(
                    "Este entrenamiento ya se cerro como %s.".formatted(status));
        this.status = WorkoutStatus.IN_PROGRESS;
    }

    /**
     * El resultado lo determina execution al cerrar la sesion. Planning solo
     * refleja el desenlace: no calcula porcentajes ni conoce las series reales.
     */
    public void markOutcome(WorkoutStatus outcome, SkipReason reason) {
        if (outcome == null || !outcome.isFinished())
            throw new DomainRuleViolationException(
                    "El desenlace debe ser COMPLETED, PARTIAL o SKIPPED.");
        this.status = outcome;
        this.skipReason = outcome == WorkoutStatus.SKIPPED ? reason : null;
        this.lackOfTimeContext = null;
        this.declaredAvailableMinutes = null;
    }

    /**
     * El usuario declara que no lo hara. Solo sobre un entrenamiento sin
     * desenlace, o sobre uno ya SKIPPED para darle la causa que la tarea de
     * vencimiento deja en NULL. Un COMPLETED o PARTIAL tiene una sesion que
     * cuenta: pisarlo con SKIPPED dejaria el plan y la adherencia diciendo
     * cosas distintas. Un REPLACED pertenece a una version que ya no rige.
     */
    public void markSkipped(SkipReason reason) {
        if (status == WorkoutStatus.COMPLETED || status == WorkoutStatus.PARTIAL
                || status == WorkoutStatus.REPLACED)
            throw new DomainRuleViolationException(
                    "Este entrenamiento ya se cerro como %s: no se puede marcar como omitido."
                            .formatted(status));
        markOutcome(WorkoutStatus.SKIPPED, reason);
    }

    /**
     * Omision por falta de tiempo con su contexto declarado. Los dos datos son opcionales y solo
     * tienen sentido juntos con {@link SkipReason#LACK_OF_TIME}; los minutos, solo con
     * {@link ContextoFaltaDeTiempo#MENOS_TIEMPO_HABITUAL}, y entonces son obligatorios.
     */
    public void markSkipped(SkipReason reason, ContextoFaltaDeTiempo contexto, Integer minutos) {
        if (contexto != null && reason != SkipReason.LACK_OF_TIME)
            throw new DomainRuleViolationException(
                    "El contexto de falta de tiempo solo se indica si el motivo es falta de tiempo.");
        if (minutos != null && contexto != ContextoFaltaDeTiempo.MENOS_TIEMPO_HABITUAL)
            throw new DomainRuleViolationException(
                    "Los minutos disponibles solo se indican si sueles tener menos tiempo para este entrenamiento.");
        if (contexto == ContextoFaltaDeTiempo.MENOS_TIEMPO_HABITUAL && minutos == null)
            throw new DomainRuleViolationException(
                    "Indica cuantos minutos sueles tener disponibles para este entrenamiento.");
        if (minutos != null && (minutos < 15 || minutos > 180))
            throw new DomainRuleViolationException("Los minutos disponibles van de 15 a 180.");
        markSkipped(reason);
        this.lackOfTimeContext = contexto;
        this.declaredAvailableMinutes = minutos == null ? null : minutos.shortValue();
    }

    /**
     * V31. Declara de que entrenamiento de la semana anterior proviene este.
     * <p>
     * Se llama al CONSTRUIR el plan derivado, copiando la sesion anterior. Es
     * lo que permite a la validacion de conservacion comparar semana contra
     * semana aunque hayan cambiado la fecha, el orden y el enfoque.
     * <p>
     * No se puede reasignar: una derivacion es un hecho del momento en que se
     * creo el plan, no un atributo editable. La base lo respalda con
     * uq_pw_source_per_plan, ck_pw_source_not_self y el trigger
     * fn_derivacion_coherente -mismo usuario y semana origen anterior-.
     */
    public void linkSource(Long sourcePlannedWorkoutId) {
        if (this.sourcePlannedWorkoutId != null)
            throw new DomainRuleViolationException(
                    "Este entrenamiento ya declara de cual proviene.");
        if (sourcePlannedWorkoutId == null)
            throw new DomainRuleViolationException(
                    "El entrenamiento de origen no puede ser nulo.");
        this.sourcePlannedWorkoutId = sourcePlannedWorkoutId;
    }

    /** V31. true si este entrenamiento proviene de uno de la semana anterior. */
    public boolean isDerived() {
        return sourcePlannedWorkoutId != null;
    }

    public void markReplaced() {
        if (!status.isFinished()) this.status = WorkoutStatus.REPLACED;
    }

    // --------------------------------------------------------------- consultas

    public boolean isExpired(OffsetDateTime now) {
        return now.isAfter(expiresAt);
    }

    /** Exigible: llego su fecha o ya vencio, y nadie lo cerro todavia. */
    public boolean isPending(OffsetDateTime now) {
        return status == WorkoutStatus.SCHEDULED || status == WorkoutStatus.IN_PROGRESS;
    }

    public int assignedExerciseCount() {
        return exercises.size();
    }

    public int equivalentVolume(int durationToRepsDivisor) {
        return exercises.stream()
                .mapToInt(exercise -> exercise.equivalentVolume(durationToRepsDivisor))
                .sum();
    }

    public int plannedRepsTotal() {
        return exercises.stream().mapToInt(PlannedWorkoutExercise::plannedRepsTotal).sum();
    }

    public int plannedSecondsTotal() {
        return exercises.stream().mapToInt(PlannedWorkoutExercise::plannedSecondsTotal).sum();
    }
}
