package main.web.services.fitsense.planning.domain.model.aggregates;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import main.web.services.fitsense.planning.domain.model.valueobjects.AdaptationAttempt;
import main.web.services.fitsense.planning.domain.model.valueobjects.AdaptationDecision;
import main.web.services.fitsense.planning.domain.model.valueobjects.GenerationSource;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * La evidencia de V32: un intento de adaptacion, con lo que se ordeno y lo que
 * paso.
 * <p>
 * ES DE SOLO ESCRITURA Y SE ACUMULA. No hay setters ni clave unica: una semana
 * puede tener varias filas -el intento de IA rechazado y el respaldo posterior
 * son DOS hechos distintos- y ninguna se corrige despues. Si algo se registro
 * mal, se registra otra fila; no se reescribe la historia.
 * <p>
 * SE CONSTRUYE DESDE {@link AdaptationAttempt}, que ya comprobo las invariantes
 * de la tabla en su constructor. Asi una fila incoherente falla al armarse y no
 * al confirmar la transaccion, que es donde la veria el disparador diferido
 * {@code tg_paa_coherencia}, sin contexto de donde venia el dato.
 */
@Entity
@Table(name = "plan_adaptation_attempts")
public class PlanAdaptationAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attempt_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "weekly_metric_id", nullable = false)
    private Long weeklyMetricId;

    @Column(name = "source_plan_id", nullable = false)
    private Long sourcePlanId;

    @Column(name = "new_week_start_date", nullable = false)
    private LocalDate newWeekStartDate;

    /** Null cuando no se publico nada. {@code ck_paa_plan_segun_estado}. */
    @Column(name = "resulting_plan_id")
    private Long resultingPlanId;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 20)
    private AdaptationDecision decision;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AdaptationAttempt.Status status;

    @Enumerated(EnumType.STRING)
    @Column(name = "generation_source", length = 20)
    private GenerationSource generationSource;

    @Column(name = "ai_calls", nullable = false)
    private short aiCalls;

    // Los cinco arreglos. En la base son _int8 y _varchar; con Hibernate 6 eso
    // es SqlTypes.ARRAY sobre Long[] y String[]. Se usa el arreglo y no una
    // List porque el tipo de la columna es un arreglo de verdad, no una tabla
    // de union: la correspondencia es directa y no hay tabla intermedia.

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "targeted_sessions", nullable = false, columnDefinition = "bigint[]")
    private Long[] targetedSessions;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "targeted_interventions", nullable = false,
            columnDefinition = "varchar(40)[]")
    private String[] targetedInterventions;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "applied_sessions", nullable = false, columnDefinition = "bigint[]")
    private Long[] appliedSessions;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "no_effect_sessions", nullable = false, columnDefinition = "bigint[]")
    private Long[] noEffectSessions;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "conserved_sessions", nullable = false, columnDefinition = "bigint[]")
    private Long[] conservedSessions;

    /**
     * El informe de V8C, entero y sin retocar. Mismo mapeo que
     * {@code WeeklyUserMetrics.riskFactors}, que es el unico jsonb que ya
     * existia en el arbol.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "conservation_report", columnDefinition = "jsonb")
    private String conservationReport;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "observed_adherence_pct")
    private BigDecimal observedAdherencePct;

    @Column(name = "registration_coverage_pct")
    private BigDecimal registrationCoveragePct;

    /** Lo pone la base con {@code now()}: aqui solo se lee. */
    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected PlanAdaptationAttempt() {
        // JPA
    }

    /** La unica forma de crear una fila: desde el intento ya comprobado. */
    public static PlanAdaptationAttempt desde(AdaptationAttempt intento) {
        if (intento == null)
            throw new IllegalArgumentException("No hay intento que registrar.");

        var fila = new PlanAdaptationAttempt();
        fila.userId = intento.userId();
        fila.weeklyMetricId = intento.weeklyMetricId();
        fila.sourcePlanId = intento.sourcePlanId();
        fila.newWeekStartDate = intento.newWeekStartDate();
        fila.resultingPlanId = intento.resultingPlanId();
        fila.decision = intento.decision();
        fila.status = intento.status();
        fila.generationSource = intento.generationSource();
        fila.aiCalls = intento.aiCalls();
        fila.targetedSessions = ids(intento.targetedSessions());
        fila.targetedInterventions = intento.targetedInterventions().toArray(String[]::new);
        fila.appliedSessions = ids(intento.appliedSessions());
        fila.noEffectSessions = ids(intento.noEffectSessions());
        fila.conservedSessions = ids(intento.conservedSessions());
        fila.conservationReport = intento.conservationReportJson();
        fila.rejectionReason = intento.rejectionReason();
        fila.observedAdherencePct = intento.observedAdherencePct();
        fila.registrationCoveragePct = intento.registrationCoveragePct();
        return fila;
    }

    private static Long[] ids(List<Long> lista) {
        return lista.toArray(Long[]::new);
    }

    public Long getId() {
        return id;
    }

    public Long getResultingPlanId() {
        return resultingPlanId;
    }

    public AdaptationAttempt.Status getStatus() {
        return status;
    }

    public AdaptationDecision getDecision() {
        return decision;
    }

    public GenerationSource getGenerationSource() {
        return generationSource;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
