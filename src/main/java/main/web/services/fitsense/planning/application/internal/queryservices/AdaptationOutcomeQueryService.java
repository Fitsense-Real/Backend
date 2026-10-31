package main.web.services.fitsense.planning.application.internal.queryservices;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import main.web.services.fitsense.planning.domain.model.aggregates.PlanAdaptationAttempt;
import main.web.services.fitsense.planning.domain.model.valueobjects.AdaptationOutcome;
import main.web.services.fitsense.planning.infrastructure.persistence.jpa.repositories.PlanAdaptationAttemptRepository;
import main.web.services.fitsense.shared.infrastructure.json.JsonSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** Lectura de V32 para el cliente ({@link AdaptationOutcome}). Solo lee: no recalcula ni corrige nada. */
@Service
public class AdaptationOutcomeQueryService {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Informe(List<String> correccionesDelBackend) {}

    private final PlanAdaptationAttemptRepository attempts;
    private final JsonSupport json;

    public AdaptationOutcomeQueryService(PlanAdaptationAttemptRepository attempts, JsonSupport json) {
        this.attempts = attempts;
        this.json = json;
    }

    /** Lo que paso en el ciclo que publico este plan; vacio para una generacion inicial. */
    @Transactional(readOnly = true)
    public Optional<AdaptationOutcome> forPlan(Long planId) {
        var lista = attempts.findByResultingPlanId(planId);
        return lista.size() == 1 ? Optional.of(traducir(lista.get(0))) : Optional.empty();
    }

    /** Todos los ciclos del usuario, publicados o no (NO_FACTIBLE no publica plan). */
    @Transactional(readOnly = true)
    public List<AdaptationOutcome> history(Long userId) {
        return attempts.findByUserIdOrderByIdAsc(userId).stream().map(this::traducir).toList();
    }

    private AdaptationOutcome traducir(PlanAdaptationAttempt a) {
        List<String> correcciones = List.of();
        try {
            var informe = a.getConservationReport() == null ? null : json.read(a.getConservationReport(), Informe.class);
            if (informe != null && informe.correccionesDelBackend() != null) correcciones = informe.correccionesDelBackend();
        } catch (RuntimeException e) {
            // Un informe ilegible no impide decir el estado: solo se pierden los motivos.
        }
        return AdaptationOutcome.de(a.getId(), a.getNewWeekStartDate(), a.getResultingPlanId(),
                a.getStatus() == null ? null : a.getStatus().name(), a.getDecision() == null ? null : a.getDecision().name(),
                a.getAiCalls(), a.getTargetedSessions(), a.getTargetedInterventions(), a.getAppliedSessions(),
                a.getNoEffectSessions(), a.getConservedSessions(), a.getProgressedSessions(), correcciones,
                a.getRejectionReason());
    }
}
