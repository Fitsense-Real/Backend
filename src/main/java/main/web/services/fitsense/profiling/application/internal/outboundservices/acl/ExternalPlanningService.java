package main.web.services.fitsense.profiling.application.internal.outboundservices.acl;

import main.web.services.fitsense.planning.interfaces.acl.PlanningContextFacade;
import org.springframework.stereotype.Service;

/**
 * Capa anticorrupcion hacia planificacion. El contexto de perfil solo pregunta si el usuario ya
 * tiene algun plan: desde ese momento el perfil no se puede cambiar (regla del estudio).
 */
@Service("profilingExternalPlanningService")
public class ExternalPlanningService {

    private final PlanningContextFacade planningContextFacade;

    public ExternalPlanningService(PlanningContextFacade planningContextFacade) {
        this.planningContextFacade = planningContextFacade;
    }

    public boolean userHasAnyPlan(Long userId) {
        return planningContextFacade.hasAnyPlan(userId);
    }
}
