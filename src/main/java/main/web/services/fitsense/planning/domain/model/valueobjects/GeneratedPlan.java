package main.web.services.fitsense.planning.domain.model.valueobjects;

import main.web.services.fitsense.planning.domain.services.CapabilityCoveragePolicy;

/**
 * Resultado de la generacion: el borrador, cuantos intentos costo y que
 * capacidades del objetivo cubre (4A v6 + 4D v4).
 * <p>
 * attempts es short porque asi lo esperan nextVersionOf y firstVersion, y asi
 * lo devuelve el pipeline.
 *
 * @param cobertura null si el objetivo no tiene matriz o no se calculo
 */
public record GeneratedPlan(PlanDraft draft, short attempts, CapabilityCoveragePolicy.Cobertura cobertura) {

    public GeneratedPlan(PlanDraft draft, short attempts) {
        this(draft, attempts, null);
    }
}
