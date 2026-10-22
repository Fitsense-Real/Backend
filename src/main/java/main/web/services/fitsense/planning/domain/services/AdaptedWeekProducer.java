package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.planning.domain.model.valueobjects.AdaptedWeekResult;
import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.ConservationReport;
import main.web.services.fitsense.planning.domain.model.valueobjects.CorrectedSession;
import main.web.services.fitsense.planning.domain.model.valueobjects.DerivedWorkout;
import main.web.services.fitsense.planning.domain.model.valueobjects.GenerationSource;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanGenerationContext;
import main.web.services.fitsense.planning.domain.model.valueobjects.WeekDerivation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Produce la semana adaptada: rutea, empalma, valida y dice si es publicable.
 * <p>
 * NO PUBLICA. Devuelve un {@link AdaptedWeekResult}, y publicar es de P2-B-2.
 * La separacion importa porque un resultado NO_FACTIBLE tambien tiene que
 * dejar evidencia en V32, y eso no se puede hacer si el productor se limita a
 * lanzar una excepcion.
 * <p>
 * LA CORRESPONDENCIA NUNCA SE APLANA. Entra como {@link WeekDerivation}, se
 * empalma como {@link WeekDerivation} y sale como {@code List<DerivedWorkout>}.
 * El {@link PlanDraft} se construye SOLO para alimentar a los validadores, que
 * es la forma que entienden, y se descarta. Si saliera de aqui, la
 * correspondencia habria que reconstruirla por fecha o por posicion, que es el
 * problema que V31 resolvio.
 * <p>
 * LA TABLA. Las dos primeras filas no llegan aqui con orden activa:
 * <ul>
 *   <li>NO_CHANGE e INSUFFICIENT_DATA — {@code adapta()} es false: se copia la
 *       semana con las fechas nuevas. Continuidad, nunca regeneracion.
 *   <li>ADAPT_LOCALIZED y ADAPT_EXPANDED — se corrigen las senaladas y solo
 *       esas; la diferencia entre las dos esta en cuantas senalo la orden, no
 *       en lo que hace el productor. Una sola solicitud en ambos casos.
 *   <li>Fallo de IA — continuidad segura, salvo dolor sin resolver.
 *   <li>Dolor no resuelto — NO_FACTIBLE: conservar republicaria el ejercicio
 *       que dolio.
 *   <li>Incompatible con 4D — NO_FACTIBLE, aunque V24 este en OBSERVE.
 * </ul>
 */
/*
 * SIN @Component, Y SIGUE SIENDO A PROPOSITO. Se registra como @Bean en
 * AdaptationWiringConfiguration, igual que AdaptationEvidenceMapper: es dominio
 * puro, no depende del contenedor y sus pruebas lo construyen con new.
 *
 * Hasta P2-B-3 no estaba registrado en ningun sitio, porque nadie implementaba
 * StrictPlanValidation y hacerlo bean habria roto el arranque por una
 * dependencia no satisfecha. StrictValidationAdapter cerro esa condicion.
 */
public class AdaptedWeekProducer {

    private final SessionFixApplier applier;
    private final ConservationValidator conservation;
    private final StrictPlanValidation estricta;

    public AdaptedWeekProducer(SessionFixApplier applier,
                               ConservationValidator conservation,
                               StrictPlanValidation estricta) {
        if (estricta == null)
            throw new IllegalArgumentException("Sin validacion estricta no se adapta nada.");

        // El camino de adaptacion no puede publicar dosis incompatibles con 4D
        // aunque V24 este en OBSERVE. En OBSERVE el validador registra el
        // incumplimiento en el log y NO lo devuelve, asi que una implementacion
        // que no bloquee dejaria pasar la dosis mala en silencio. Falla aqui,
        // al construir, y no en la primera semana de un participante.
        if (!estricta.bloqueaDosis4D())
            throw new IllegalArgumentException(
                    "La validacion del camino de adaptacion tiene que RECHAZAR una dosis"
                            + " que no cumple 4D, no solo registrarla. Con V24 en OBSERVE"
                            + " el incumplimiento no llega a los problemas.");

        this.applier = applier;
        this.conservation = conservation;
        this.estricta = estricta;
    }

    /**
     * @param derivation   la copia de la semana anterior con las fechas nuevas.
     * @param correcciones lo que devolvio la IA. {@code null} significa que NO
     *                     hubo respuesta usable -fallo, JSON invalido, agotados
     *                     los reintentos- y NO equivale a una lista vacia, que
     *                     significa «no propongo correcciones».
     * @param eligible     el catalogo elegible, para V8C y para la dosis.
     */
    public AdaptedWeekResult produce(PlanGenerationContext context,
                                     WeekDerivation derivation,
                                     List<CorrectedSession> correcciones,
                                     Map<Long, CandidateExercise> eligible) {

        if (context == null)
            throw new IllegalArgumentException("No se puede adaptar sin contexto.");
        if (derivation == null)
            throw new IllegalArgumentException(
                    "No hay semana anterior de la que partir. La primera semana es"
                            + " generacion inicial, no adaptacion.");

        var adjustment = context.adjustment();

        // NO_CHANGE / INSUFFICIENT_DATA: no hubo baja adherencia, o no hay datos
        // para afirmar que la hubo. En los dos casos se conserva. La falta de
        // datos NO se interpreta como buena adherencia: se interpreta como que
        // no hay motivo para tocar nada.
        if (adjustment == null || !adjustment.adapta())
            return continuidad(context, derivation, AdaptedWeekResult.Motivo.SIN_ORDEN, eligible);

        // Fallo de IA. Antes de conservar hay que mirar si conservar es seguro.
        if (correcciones == null)
            return respaldo(context, derivation, adjustment, eligible);

        var adaptada = applier.apply(derivation, correcciones, adjustment);
        var informe = conservation.validate(adaptada, context.previousWeek(), adjustment, eligible);

        // El origen lo decide si la IA cambio algo DE VERDAD, no si contesto.
        // Una semana de adaptaciones sin efecto es CONSERVED aunque el modelo
        // haya devuelto sesiones: lo que se publica es la semana anterior.
        boolean huboEfecto = informe.huboAdaptacionEfectiva();
        var source = huboEfecto ? GenerationSource.AI : GenerationSource.CONSERVED;
        var motivo = huboEfecto
                ? AdaptedWeekResult.Motivo.CORRECCION_IA : AdaptedWeekResult.Motivo.SIN_EFECTO;

        return revisarYCerrar(context, adaptada.workouts(), informe, source, motivo, eligible);
    }

    /**
     * Continuidad segura cuando la IA no sirvio.
     * <p>
     * Conservar es la respuesta correcta casi siempre: la semana anterior ya era
     * valida. Pero NO cuando la orden incluye una sesion con revision de
     * seguridad: conservarla volveria a publicar el ejercicio que dolio, y eso
     * es peor que no publicar. No se puede resolver sin la IA, porque sustituir
     * un ejercicio no es algo que el respaldo pueda decidir.
     */
    private AdaptedWeekResult respaldo(PlanGenerationContext context,
                                       WeekDerivation derivation,
                                       PlanAdjustment adjustment,
                                       Map<Long, CandidateExercise> eligible) {

        var conSeguridad = adjustment.targetedSessions().stream()
                .filter(s -> s.intervencion().exigeRevisionDeSeguridad())
                .map(PlanAdjustment.TargetedSession::plannedWorkoutId)
                .toList();

        if (!conSeguridad.isEmpty())
            return AdaptedWeekResult.noFactible(AdaptedWeekResult.Motivo.RESPALDO_IA,
                    ConservationReport.vacio(), List.of(
                            ("Sin respuesta de la IA no se puede resolver el dolor de las sesiones %s:"
                                    + " conservarlas volveria a publicar el ejercicio que dolio.")
                                    .formatted(conSeguridad)));

        // Sin dolor, se conserva la semana entera. Las senaladas quedan como
        // conservadas: la adaptacion no ocurrio, y eso es un hecho que hay que
        // registrar, no un plan invalido.
        return continuidad(context, derivation, AdaptedWeekResult.Motivo.RESPALDO_IA, eligible);
    }

    /**
     * Conservar, COMPROBANDO que lo conservado sea de verdad una copia.
     * <p>
     * {@code WeekDerivationBuilder} deja que quien lo llama elija que sesiones
     * sobreviven: no garantiza que esten todas. Sin esta comprobacion, una
     * semana a la que le falta una sesion -o que trae otra con los ejercicios
     * cambiados- se publicaba como CONSERVED sin que nadie la mirara, porque
     * V8C solo entraba con orden activa. Una semana que se declara copia y no lo
     * es es peor que una adaptacion rechazada.
     * <p>
     * Se reutiliza la comparacion de V8C -{@code validateContinuidad}-, no una
     * segunda implementacion que pudiera separarse de ella.
     */
    private AdaptedWeekResult continuidad(PlanGenerationContext context,
                                          WeekDerivation derivation,
                                          AdaptedWeekResult.Motivo motivo,
                                          Map<Long, CandidateExercise> eligible) {

        var informe = conservation.validateContinuidad(derivation, context.previousWeek(), eligible);
        return revisarYCerrar(context, derivation.workouts(), informe,
                GenerationSource.CONSERVED, motivo, eligible);
    }

    /**
     * El paso comun: validar de verdad y decidir.
     * <p>
     * Se valida TAMBIEN la semana conservada. Era valida cuando se publico, pero
     * las reglas pueden haberse endurecido desde entonces -4D entrando en
     * ENFORCE, un ejercicio que dejo de ser elegible- y publicar sin mirar seria
     * dar por bueno algo que hoy no lo es. La consecuencia es que una semana
     * conservada puede salir NO_FACTIBLE, y es correcto que salga.
     */
    private AdaptedWeekResult revisarYCerrar(PlanGenerationContext context,
                                             List<DerivedWorkout> semana,
                                             ConservationReport informe,
                                             GenerationSource source,
                                             AdaptedWeekResult.Motivo motivo,
                                             Map<Long, CandidateExercise> eligible) {

        var problemas = new ArrayList<>(informe.problems());
        problemas.addAll(estricta.revisar(borradorPara(semana, source, informe), context, informe));

        if (!problemas.isEmpty())
            return AdaptedWeekResult.noFactible(motivo, informe, problemas);

        if (semana.isEmpty())
            return AdaptedWeekResult.noFactible(motivo, informe,
                    List.of("La semana propuesta no tiene ninguna sesion."));

        return new AdaptedWeekResult(AdaptedWeekResult.Estado.PUBLICABLE, motivo,
                semana, source, informe, List.of());
    }

    /**
     * El borrador que entienden los validadores. Se descarta despues.
     * <p>
     * {@code planName}, {@code totalVolume} y {@code rationale} no los lee
     * ningun validador -comprobado-, asi que aqui van valores de traza y los
     * definitivos los pone P2-B-2 al publicar, que es quien tiene el plan.
     */
    private PlanDraft borradorPara(List<DerivedWorkout> semana, GenerationSource source,
                                   ConservationReport informe) {
        return new PlanDraft(source, null, null, null, informe.describe(),
                semana.stream().map(DerivedWorkout::workout).toList());
    }
}
