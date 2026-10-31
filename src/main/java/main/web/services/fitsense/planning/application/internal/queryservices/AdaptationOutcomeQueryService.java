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
    /** P-03, nivel 2. Null en pruebas que construyen el servicio a mano. */
    private final main.web.services.fitsense.planning.infrastructure.persistence.jpa.repositories
            .SafeRegenerationAttemptRepository regeneraciones;

    public AdaptationOutcomeQueryService(PlanAdaptationAttemptRepository attempts, JsonSupport json) {
        this(attempts, json, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public AdaptationOutcomeQueryService(PlanAdaptationAttemptRepository attempts, JsonSupport json,
                                         main.web.services.fitsense.planning.infrastructure.persistence.jpa
                                                 .repositories.SafeRegenerationAttemptRepository regeneraciones) {
        this.attempts = attempts;
        this.json = json;
        this.regeneraciones = regeneraciones;
    }

    /** Codigos de P-03, nivel 2 (la regeneracion con reglas tras un NO_FACTIBLE por dolor). */
    public static final String REGENERACION_SEGURA_REGLAS = "REGENERACION_SEGURA_REGLAS";

    /** Lo que paso en el ciclo que publico este plan; vacio para una generacion inicial. */
    @Transactional(readOnly = true)
    public Optional<AdaptationOutcome> forPlan(Long planId) {
        var lista = attempts.findByResultingPlanId(planId);
        if (lista.size() == 1) return Optional.of(traducir(lista.get(0)));
        if (regeneraciones == null) return Optional.empty();
        // P-03, nivel 2: el plan regenerado no tiene fila V32 propia; se cuenta desde el NO_FACTIBLE que lo origino.
        return regeneraciones.findByResultingPlanId(planId).filter(r -> r.getAdaptationAttemptId() != null)
                .flatMap(r -> attempts.findById(r.getAdaptationAttemptId()))
                .map(a -> traducir(a).conRegeneracion(planId, REGENERACION_SEGURA_REGLAS, null));
    }

    /** Todos los ciclos del usuario, publicados o no (NO_FACTIBLE no publica plan). */
    @Transactional(readOnly = true)
    public List<AdaptationOutcome> history(Long userId) {
        var porIntento = new java.util.HashMap<Long, main.web.services.fitsense.planning.domain.model.aggregates
                .SafeRegenerationAttempt>();
        if (regeneraciones != null)
            regeneraciones.findByUserIdOrderByIdAsc(userId).forEach(r -> {
                if (r.getAdaptationAttemptId() != null) porIntento.put(r.getAdaptationAttemptId(), r);
            });
        return attempts.findByUserIdOrderByIdAsc(userId).stream().map(a -> {
            var o = traducir(a);
            var r = porIntento.get(a.getId());
            if (r == null) return o;
            // El NO_FACTIBLE de la adaptacion sigue constando (estado); el resultado dice como acabo la semana.
            return r.getOutcome() == main.web.services.fitsense.planning.domain.model.aggregates
                    .SafeRegenerationAttempt.Outcome.PUBLICADO
                    ? o.conRegeneracion(r.getResultingPlanId(), REGENERACION_SEGURA_REGLAS,
                    "P-03: semana regenerada en el mismo ciclo con el motor de reglas, sin " + r.getExcludedExerciseIds())
                    : o.conRegeneracion(null, o.resultado(), "P-03: sin alternativa segura valida: "
                    + r.getValidationProblems());
        }).toList();
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
