package main.web.services.fitsense.planning.application.internal.commandservices;

import main.web.services.fitsense.planning.domain.model.valueobjects.PlanningPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Que version de politica usan los planes NUEVOS.
 * <ul>
 *   <li>{@code fitsense.planning.policy} (por defecto {@code PLAN-HIST-1}): una version del catalogo cerrado de
 *       {@link PlanningPolicy}. Desconocida: el contexto no arranca.</li>
 *   <li>{@code fitsense.planning.policy.allow-candidates} (por defecto {@code false}): una version candidata
 *       (hoy, V4-C) exige ademas este permiso explicito; sin el, el contexto no arranca. Asi una
 *       version candidata no se activa por un error de configuracion.</li>
 * </ul>
 * No hay ningun interruptor que active varias politicas a la vez: cada version del catalogo es una combinacion
 * explicita. Los planes ya guardados se interpretan con su propia version ({@code WeeklyTrainingPlan#politica()}).
 */
@Component
public class PlanningPolicySelector {

    private static final Logger log = LoggerFactory.getLogger(PlanningPolicySelector.class);

    private final PlanningPolicy activa;

    public PlanningPolicySelector(@Value("${fitsense.planning.policy:PLAN-HIST-1}") String politica,
                                  @Value("${fitsense.planning.policy.allow-candidates:false}") boolean candidatas) {
        this.activa = elegir(politica, candidatas);
        if (activa.candidata())
            log.warn("POLITICA_PLANIFICACION candidata ACTIVA: {} ({}). Solo para laboratorio o entorno autorizado.",
                    activa.id(), activa.descripcion());
        else log.info("POLITICA_PLANIFICACION {}", activa.id());
    }

    static PlanningPolicy elegir(String politica, boolean candidatas) {
        var p = PlanningPolicy.deRegistro(politica);
        if (p.candidata() && !candidatas)
            throw new IllegalStateException(("La politica %s es candidata y no esta autorizada: requiere"
                    + " fitsense.planning.policy.allow-candidates=true. No se activa por defecto.").formatted(p.id()));
        return p;
    }

    /** La politica de los planes nuevos. */
    public PlanningPolicy activa() {
        return activa;
    }

    public static PlanningPolicySelector historica() {
        return new PlanningPolicySelector("PLAN-HIST-1", false);
    }
}
