package main.web.services.fitsense.planning.infrastructure.validation;

import main.web.services.fitsense.planning.application.internal.outboundservices.acl.ExternalConfigurationService;
import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.valueobjects.ConservationReport;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanGenerationContext;
import main.web.services.fitsense.planning.domain.services.DoseEnforcement;
import main.web.services.fitsense.planning.domain.services.PlanDraftValidator;
import main.web.services.fitsense.planning.domain.services.SessionDurationEstimator;
import main.web.services.fitsense.planning.domain.services.SlotCoverageEnforcement;
import main.web.services.fitsense.planning.domain.services.StrictPlanValidation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * La validacion estricta del camino de adaptacion.
 * <p>
 * TIENE SU PROPIO {@link PlanDraftValidator}, EN {@code ENFORCE} PARA LA DOSIS,
 * y ese es el motivo de que esta clase exista. El validador que Spring inyecta
 * en el pipeline lee {@code fitsense.dose.enforcement}, hoy {@code OBSERVE}, y
 * en ese modo {@code validateMvpDose} escribe el incumplimiento en el log y
 * hace {@code return} <b>sin anadirlo a los problemas</b>: una dosis
 * incompatible con 4D sale aprobada. Para la generacion inicial eso es
 * deliberado -se mide antes de exigir-; para una adaptacion no, porque publicar
 * una dosis que 4D rechaza seria peor que no adaptar.
 * <p>
 * EL RESTO DE MODOS NO SE TOCA. La cobertura 4C sigue el valor configurado:
 * endurecer la dosis no autoriza a endurecer otra cosa por el camino.
 * <p>
 * EL DIVISOR SE PIDE EN CADA LLAMADA, no se guarda. Es el mismo que usa la
 * fase 1 -{@code externalConfigurationService.durationToRepsDivisor()}- y puede
 * cambiar en caliente; cachearlo haria que la adaptacion validara con un valor
 * y la generacion inicial con otro.
 */
@Component
public class StrictValidationAdapter implements StrictPlanValidation {

    private final PlanDraftValidator validador;
    private final ExternalConfigurationService configuracion;

    /**
     * La anotacion es obligatoria, no decoracion: con dos constructores
     * publicos y ninguno marcado, Spring no sabe cual usar y el contexto NO
     * arranca. Es la misma razon por la que {@link PlanDraftValidator} lleva
     * la suya.
     */
    @Autowired
    public StrictValidationAdapter(
            SessionDurationEstimator durationEstimator,
            ExternalConfigurationService configuracion,
            @Value("${fitsense.coverage.enforcement:OBSERVE}") String coverageEnforcement,
            @Value("${fitsense.adaptation.capability-validation.enabled:false}") boolean capacidadesFlag,
            @Value("${fitsense.adaptation.progression.enabled:false}") boolean progresion) {

        // La progresion exige la V24 por capacidades (diseno §4.5): sin ella, una
        // dosis progresada de INCREASE_STRENGTH, LOSE_WEIGHT o GENERAL_FITNESS no
        // la comprobaria nadie.
        boolean capacidades = capacidadesFlag || progresion;

        // D-P11: la V24 por capacidades entra en la adaptacion SOLO con su
        // interruptor, y el productor solo la acepta junto con las reducciones
        // autorizadas. Apagada, el validador es exactamente el de antes.
        this.validador = capacidades
                ? new PlanDraftValidator(durationEstimator, SlotCoverageEnforcement.parse(coverageEnforcement),
                DoseEnforcement.ENFORCE, DoseEnforcement.ENFORCE)
                : new PlanDraftValidator(durationEstimator, SlotCoverageEnforcement.parse(coverageEnforcement),
                DoseEnforcement.ENFORCE);
        this.configuracion = configuracion;
    }

    /** Sin los interruptores de V34: el validador de siempre. */
    public StrictValidationAdapter(SessionDurationEstimator durationEstimator,
                                   ExternalConfigurationService configuracion,
                                   String coverageEnforcement) {
        this(durationEstimator, configuracion, coverageEnforcement, false, false);
    }

    /** Para pruebas: el validador ya construido, sin contenedor. */
    public StrictValidationAdapter(PlanDraftValidator validador,
                                   ExternalConfigurationService configuracion) {
        this.validador = validador;
        this.configuracion = configuracion;
    }

    /**
     * {@inheritDoc}
     * <p>
     * {@code validate} lanza; este puerto devuelve. La diferencia importa: el
     * productor necesita juntar estos problemas con los de V8C para registrarlos
     * como evidencia, y una excepcion perderia los otros.
     */
    @Override
    public List<String> revisar(PlanDraft draft, PlanGenerationContext context,
                                ConservationReport conservacion) {
        try {
            validador.validate(draft, context, configuracion.durationToRepsDivisor(),
                    conservacion == null ? ConservationReport.vacio() : conservacion);
            return List.of();
        } catch (InvalidPlanDraftException e) {
            return List.copyOf(e.problems());
        }
    }

    @Override
    public boolean validaCapacidades() {
        return validador.capabilityEnforcement().bloquea();
    }

    @Override
    public boolean bloqueaDosis4D() {
        // Se pregunta al validador, no se devuelve true a secas: si alguien
        // cambia como se construye, esto deja de mentir y el productor se niega
        // a arrancar, que era el objetivo del guardia.
        return validador.doseEnforcement().bloquea();
    }
}
