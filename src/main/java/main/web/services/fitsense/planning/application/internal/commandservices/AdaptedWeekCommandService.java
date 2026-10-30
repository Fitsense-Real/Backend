package main.web.services.fitsense.planning.application.internal.commandservices;

import main.web.services.fitsense.planning.domain.exceptions.AiProviderUnavailableException;
import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.aggregates.WeeklyTrainingPlan;
import main.web.services.fitsense.planning.domain.model.valueobjects.AdaptationDecision;
import main.web.services.fitsense.planning.domain.model.valueobjects.AdaptedPublication;
import main.web.services.fitsense.planning.domain.model.valueobjects.AdaptedWeekResult;
import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.CorrectedSession;
import main.web.services.fitsense.planning.domain.model.valueobjects.DerivedWorkout;
import main.web.services.fitsense.planning.domain.model.valueobjects.ExclusionesDeSeguridad;
import main.web.services.fitsense.planning.domain.model.valueobjects.GenerationSource;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanGenerationContext;
import main.web.services.fitsense.planning.domain.model.valueobjects.PreparedPlanGeneration;
import main.web.services.fitsense.planning.domain.model.valueobjects.PreviousWeekSummary;
import main.web.services.fitsense.planning.domain.model.valueobjects.WeekDerivation;
import main.web.services.fitsense.planning.domain.services.AdaptationEvidenceMapper;
import main.web.services.fitsense.planning.domain.services.AdaptedWeekProducer;
import main.web.services.fitsense.planning.domain.services.WeekDerivationBuilder;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
import main.web.services.fitsense.planning.infrastructure.generation.ai.AiRetryFeedback;
import main.web.services.fitsense.planning.infrastructure.generation.ai.AirCircuitBreaker;
import main.web.services.fitsense.planning.infrastructure.generation.ai.PlanInputSnapshot;
import main.web.services.fitsense.planning.infrastructure.generation.ai.ReplicateSessionFixGenerator;
import main.web.services.fitsense.planning.interfaces.acl.MeasuredWeekView;
import main.web.services.fitsense.shared.infrastructure.json.JsonSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * EL CICLO DE LA SEMANA ADAPTADA, de punta a punta.
 * <p>
 * Deriva la semana anterior, pide a la IA que corrija SOLO las sesiones
 * senaladas, valida, y publica o registra el fallo.
 * <p>
 * SIN {@code @Transactional}, Y NO ES UN OLVIDO. Hace hasta tres llamadas HTTP
 * de ~90 s. Dentro de una transaccion, esos minutos cuentan como
 * idle-in-transaction: Postgres termina la conexion y se lleva lo escrito antes
 * en ella, incluidos el cierre de semana y las metricas. Es el mismo motivo por
 * el que {@code WeeklyTrainingPlanCommandServiceImpl.generate} tampoco la lleva.
 * <p>
 * Las dos escrituras van por dentro, cada una con la suya:
 * {@code publishAdapted} abre la de publicacion, y
 * {@link AdaptationFailureRecorder} abre una NUEVA para el fallo, que es lo
 * unico que funciona cuando la de publicacion no existe o se esta deshaciendo.
 * <p>
 * NO HAY RESPALDO POR MOTOR DE REGLAS, al contrario que la generacion inicial.
 * Agotados los intentos, la adaptacion conserva la semana anterior o no publica:
 * generar una semana nueva con reglas cuando lo que se pidio fue corregir dos
 * sesiones seria cambiar todo lo que funcionaba.
 */
@Service
public class AdaptedWeekCommandService {

    private static final Logger log = LoggerFactory.getLogger(AdaptedWeekCommandService.class);

    /** Lo mismo que la generacion inicial: ver 19.4. */
    private static final int AI_ATTEMPTS = 3;

    private final ReplicateSessionFixGenerator fixGenerator;
    private final WeekDerivationBuilder derivationBuilder;
    private final AdaptedWeekProducer producer;
    private final WeeklyTrainingPlanCommandService planCommandService;
    private final AdaptationEvidenceMapper evidenceMapper;
    private final AdaptationFailureRecorder failureRecorder;
    private final JsonSupport jsonSupport;

    /**
     * CORTACIRCUITOS PROPIO, no compartido con {@code PlanGenerationPipeline}.
     * <p>
     * Aquel construye el suyo con {@code new AirCircuitBreaker()}, asi que
     * tampoco es un bean. La consecuencia hay que tenerla presente: con
     * Replicate caido, la adaptacion de cada participante paga su propio
     * timeout aunque la generacion ya lo supiera. Se acepta para el MVP porque
     * compartirlo exige convertirlo en bean y decidir su ambito, y eso es
     * alcance nuevo.
     */
    private final AirCircuitBreaker breaker = new AirCircuitBreaker();

    /**
     * Que heredados quedaron fuera SOLO por seguridad, para la sustitucion
     * obligatoria de SafetySubstitutionPolicy. Null en las pruebas que
     * construyen el servicio a mano: entonces no se sustituye nada.
     */
    private final SafetyExclusionResolver safetyExclusions;
    private final ProgresionResolver progresiones;
    private final boolean progresionHabilitada;

    /**
     * Para pruebas que construyen el servicio a mano: sin sustitucion por
     * seguridad, exactamente el comportamiento anterior.
     */
    public AdaptedWeekCommandService(ReplicateSessionFixGenerator fixGenerator,
                                     WeekDerivationBuilder derivationBuilder,
                                     AdaptedWeekProducer producer,
                                     WeeklyTrainingPlanCommandService planCommandService,
                                     AdaptationEvidenceMapper evidenceMapper,
                                     AdaptationFailureRecorder failureRecorder,
                                     JsonSupport jsonSupport) {
        this(fixGenerator, derivationBuilder, producer, planCommandService, evidenceMapper,
                failureRecorder, jsonSupport, null);
    }

    public AdaptedWeekCommandService(ReplicateSessionFixGenerator fixGenerator,
                                     WeekDerivationBuilder derivationBuilder,
                                     AdaptedWeekProducer producer,
                                     WeeklyTrainingPlanCommandService planCommandService,
                                     AdaptationEvidenceMapper evidenceMapper,
                                     AdaptationFailureRecorder failureRecorder,
                                     JsonSupport jsonSupport,
                                     SafetyExclusionResolver safetyExclusions) {
        this(fixGenerator, derivationBuilder, producer, planCommandService, evidenceMapper,
                failureRecorder, jsonSupport, safetyExclusions, null, false);
    }

    /**
     * @param progresiones          V34: los pasos de progresion candidatos del ciclo
     * @param progresionHabilitada  {@code fitsense.adaptation.progression.enabled};
     *                              apagada por defecto hasta aprobarla en Neon
     */
    @org.springframework.beans.factory.annotation.Autowired
    public AdaptedWeekCommandService(ReplicateSessionFixGenerator fixGenerator,
                                     WeekDerivationBuilder derivationBuilder,
                                     AdaptedWeekProducer producer,
                                     WeeklyTrainingPlanCommandService planCommandService,
                                     AdaptationEvidenceMapper evidenceMapper,
                                     AdaptationFailureRecorder failureRecorder,
                                     JsonSupport jsonSupport,
                                     SafetyExclusionResolver safetyExclusions,
                                     ProgresionResolver progresiones,
                                     @org.springframework.beans.factory.annotation.Value(
                                             "${fitsense.adaptation.progression.enabled:false}")
                                     boolean progresionHabilitada) {
        this.safetyExclusions = safetyExclusions;
        this.progresiones = progresiones;
        this.progresionHabilitada = progresionHabilitada;

        this.fixGenerator = fixGenerator;
        this.derivationBuilder = derivationBuilder;
        this.producer = producer;
        this.planCommandService = planCommandService;
        this.evidenceMapper = evidenceMapper;
        this.failureRecorder = failureRecorder;
        this.jsonSupport = jsonSupport;
    }

    /**
     * @return el plan publicado, o vacio si no se pudo publicar ninguno. Vacio
     *         NO es un error tecnico: es la respuesta correcta cuando conservar
     *         seria inseguro o la propuesta no pasa la validacion.
     */
    public Optional<WeeklyTrainingPlan> adaptar(PreparedPlanGeneration prepared,
                                                MeasuredWeekView medida) {
        var context = prepared.context();
        var adjustment = prepared.adjustment();
        var decision = AdaptationDecision.valueOf(medida.decision());

        // LOS DOS HECHOS DESCRIBEN LO MISMO Y LLEGAN POR CAMINOS DISTINTOS: la
        // decision viene del desenlace del decisor, las sesiones senaladas de
        // la orden traducida. Si discreparan, seguir seria elegir en silencio
        // cual de los dos vale, y la evidencia quedaria diciendo algo que no
        // paso. El mapper lo rechazaria mas tarde y con menos contexto.
        if (decision.ordenaAdaptar() != adjustment.adapta())
            throw new IllegalStateException(
                    ("La decision %s y la orden no concuerdan para el usuario %d: sesiones"
                            + " senaladas = %d.")
                            .formatted(decision, context.userId(),
                                    adjustment.targetedSessions().size()));

        var derivation = derivar(context);
        var elegibles = porId(context.availableExercises());
        var exclusiones = exclusionesDeSeguridad(context);

        // CON NO_CHANGE O INSUFFICIENT_DATA NO SE LE PIDE NADA A LA IA: se
        // conserva la continuidad. Lista vacia y no null: null significa «no
        // hubo respuesta usable», y aqui no se pregunto nada.
        var intento = decision.ordenaAdaptar()
                ? corregir(context, derivation, elegibles, exclusiones)
                : new Intento(continuidad(context, derivation, elegibles, exclusiones, decision, medida),
                null, (short) 0);

        if (intento.resultado().publicable())
            return publicar(prepared, medida, decision, intento);

        registrarFallo(prepared, medida, decision, intento);
        return Optional.empty();
    }

    // =======================================================================
    // 1 · La semana anterior, con las fechas nuevas
    // =======================================================================

    /**
     * Copia TODAS las sesiones de la semana medida, una semana mas tarde.
     * <p>
     * Se conservan todas y no solo las que no se senalaron: la adaptacion
     * CORRIGE sesiones, no las elimina, y V8C comprueba despues que se cambio
     * la parte senalada y solo esa. Las fechas se desplazan siete dias en vez
     * de recolocarse sobre la semana nueva, para que el patron de dias de la
     * persona se mantenga.
     */
    private WeekDerivation derivar(PlanGenerationContext context) {
        var previa = context.previousWeek();
        if (previa == null || previa.workouts() == null || previa.workouts().isEmpty())
            throw new IllegalStateException(
                    ("No hay semana anterior del usuario %d de la que derivar. La primera"
                            + " semana es generacion inicial, no adaptacion.")
                            .formatted(context.userId()));

        var origenes = new ArrayList<Long>();
        var fechas = new ArrayList<LocalDate>();
        for (PreviousWeekSummary.WorkoutOutcome sesion : previa.workouts()) {
            // Sin identificador no hay linaje que conservar, y sin fecha no hay
            // de donde sacar la nueva. Las dos cosas son datos incompletos de
            // la semana medida, no decisiones de la adaptacion.
            if (sesion.plannedWorkoutId() == null || sesion.scheduledDate() == null) continue;
            origenes.add(sesion.plannedWorkoutId());
            fechas.add(sesion.scheduledDate().plusWeeks(1));
        }

        return derivationBuilder.derive(previa, origenes, fechas);
    }

    /**
     * Las exclusiones de seguridad de los heredados. Si no se pueden calcular,
     * se sigue sin ellas: entonces no se sustituye nada y V1 rechaza lo que ya
     * no es elegible, que es el comportamiento seguro de siempre.
     */
    private ExclusionesDeSeguridad exclusionesDeSeguridad(PlanGenerationContext context) {
        if (safetyExclusions == null) return ExclusionesDeSeguridad.ninguna();
        try {
            return safetyExclusions.resolver(context);
        } catch (RuntimeException e) {
            log.warn("EXCLUSIONES_SEGURIDAD user={} no se pudieron calcular, sin sustitucion: {}",
                    context.userId(), e.toString());
            return ExclusionesDeSeguridad.ninguna();
        }
    }

    /**
     * Sin exclusiones de seguridad, la llamada de siempre; con ellas, la que
     * repara los heredados. Son equivalentes cuando no hay nada que sustituir.
     */
    private AdaptedWeekResult producir(PlanGenerationContext context, WeekDerivation derivation,
                                       List<CorrectedSession> correcciones,
                                       Map<Long, CandidateExercise> elegibles,
                                       ExclusionesDeSeguridad exclusiones) {
        return exclusiones.vacias()
                ? producer.produce(context, derivation, correcciones, elegibles)
                : producer.produce(context, derivation, correcciones, elegibles, exclusiones);
    }

    /**
     * Sin orden de adaptar: se conserva la semana y, con {@code NO_CHANGE} y la
     * progresion encendida, se intenta UN paso (V34). Los candidatos se prueban
     * en su orden de prioridad y se publica el primero que pasa V8C y la
     * validacion estricta; si ninguno pasa, la copia de siempre, con los motivos
     * en el informe de conservacion. Nunca se progresa a medias, y con
     * {@code INSUFFICIENT_DATA} no se progresa: sin informacion, se mantiene.
     */
    private AdaptedWeekResult continuidad(PlanGenerationContext context, WeekDerivation derivation,
                                          Map<Long, CandidateExercise> elegibles,
                                          ExclusionesDeSeguridad exclusiones,
                                          AdaptationDecision decision, MeasuredWeekView medida) {
        var candidatos = progresionHabilitada && progresiones != null && decision == AdaptationDecision.NO_CHANGE
                ? progresiones.candidatos(context, derivation, medida.observedAdherencePct(), elegibles, exclusiones)
                : List.<main.web.services.fitsense.planning.domain.model.valueobjects.PasoProgresion>of();
        if (candidatos.isEmpty())
            return producir(context, derivation, List.of(), elegibles, exclusiones);

        var bloqueados = new ArrayList<String>();
        for (var paso : candidatos) {
            var conPaso = new WeekDerivation(derivation.workouts().stream()
                    .map(d -> d.sourcePlannedWorkoutId().equals(paso.origen())
                            ? new DerivedWorkout(d.sourcePlannedWorkoutId(), paso.contenido()) : d)
                    .toList(), derivation.sourcesWithoutDerived());
            var resultado = producer.produce(context, conPaso, List.of(), elegibles, exclusiones, paso);
            if (resultado.publicable()) {
                log.info("PROGRESION usuario {} · sesion {} · {} del ejercicio {} ({} -> {}), volumen {} -> {}"
                                + " (base {}, techo {}{})", context.userId(), paso.origen(), paso.tipo(),
                        paso.exerciseId(), paso.antes(), paso.despues(), paso.volumenAntes(), paso.volumenDespues(),
                        paso.volumenBase(), paso.techo(),
                        paso.estadoReserva() == null ? "" : ", reserva " + paso.estadoReserva());
                var informe = resultado.conservacion();
                for (var motivo : bloqueados) informe = informe.conPasoBloqueado(motivo);
                return new AdaptedWeekResult(resultado.estado(), resultado.motivo(), resultado.semana(),
                        resultado.source(), informe, resultado.problemas());
            }
            bloqueados.add("%s de la sesion %d (ejercicio %d): %s".formatted(paso.tipo(), paso.origen(),
                    paso.exerciseId(), resultado.problemas()));
        }

        log.warn("PROGRESION_BLOQUEADA usuario {} · {}", context.userId(), bloqueados);
        var copia = producir(context, derivation, List.of(), elegibles, exclusiones);
        var informe = copia.conservacion();
        for (var motivo : bloqueados) informe = informe.conPasoBloqueado(motivo);
        return new AdaptedWeekResult(copia.estado(), copia.motivo(), copia.semana(), copia.source(),
                informe, copia.problemas());
    }

    private static Map<Long, CandidateExercise> porId(List<CandidateExercise> elegibles) {
        var porId = new LinkedHashMap<Long, CandidateExercise>();
        if (elegibles != null)
            elegibles.forEach(e -> porId.put(e.exerciseId(), e));
        return porId;
    }

    // =======================================================================
    // 2 · Los tres intentos
    // =======================================================================

    /**
     * Lo que sale del bucle.
     *
     * @param resultado    lo que decidio el productor. Es el veredicto final del
     *                     bucle, no solo de la ultima respuesta.
     * @param snapshotJson la proyeccion que VIAJO en el prompt del intento que
     *                     salio bien, para persistirla tal cual. {@code null} si
     *                     ninguno salio bien o no se llego a pedir nada.
     * @param llamadas     cuantas peticiones costo. Va a {@code ai_calls}, y
     *                     cuenta tambien las que acabaron rechazadas.
     */
    private record Intento(AdaptedWeekResult resultado, String snapshotJson, short llamadas) {}

    /**
     * Hasta tres intentos de corregir, Y LA VALIDACION ENTRA EN EL BUCLE.
     * <p>
     * ESTO ES LO QUE DISTINGUE UN REINTENTO UTIL DE UNO DESPERDICIADO. Una
     * respuesta puede ser JSON impecable y aun asi proponer una semana que V8C,
     * la seguridad o la dosis de 4D rechazan: cambiar un ejercicio que nadie
     * senalo, insistir en el que causo dolor, pasarse del tope de colocaciones.
     * Si el productor corriera despues del bucle, ese rechazo consumiria la
     * unica oportunidad y quedarian dos sin usar.
     * <p>
     * Es la misma politica de {@code PlanGenerationPipeline}, donde
     * {@code validator.validate(...)} esta dentro del {@code try} por el mismo
     * motivo. La diferencia es de forma, no de fondo: aquel valida lanzando y
     * aqui el productor devuelve {@code NO_FACTIBLE} con sus problemas, que son
     * los de V8C mas los de la validacion estricta.
     * <p>
     * Los dos tipos de rechazo realimentan igual: lo que cambia es de donde
     * salen los motivos.
     * <ul>
     *   <li>{@code InvalidPlanDraftException}: la respuesta no cumple el
     *       contrato -JSON invalido, una sesion que nadie senalo-.
     *   <li>{@code NO_FACTIBLE}: la respuesta se entendio y la semana que
     *       produce no vale.
     * </ul>
     */
    private Intento corregir(PlanGenerationContext context,
                             WeekDerivation derivation,
                             Map<Long, CandidateExercise> elegibles,
                             ExclusionesDeSeguridad exclusiones) {

        // SIN IA CUANDO NO HAY NADA QUE PEDIR (D1 y D6 de DISENO_ADAPTACION_CONTEXTUAL v0.3). Si
        // todas las senaladas son CONSERVAR o CAUSA_NO_REGISTRADA, ninguna puede cambiar: se
        // conservan con una propuesta vacia, que el productor registra como sin efecto, con
        // origen CONSERVED y 0 llamadas.
        if (context.adjustment() != null && context.adjustment().sesionesParaIA().isEmpty())
            return new Intento(producir(context, derivation, List.of(), elegibles, exclusiones), null, (short) 0);

        if (!fixGenerator.isEnabled() || !proveedorDisponible(context.userId()))
            return sinPropuestaUsable(context, derivation, elegibles, exclusiones, (short) 0);

        var full = PlanInputSnapshot.of(context);
        var acumulados = new LinkedHashSet<String>();
        String ultimaPropuesta = null;
        List<String> ultimosMotivos = List.of();
        short llamadas = 0;

        for (int intento = 1; intento <= AI_ATTEMPTS; intento++) {
            llamadas++;

            // FUERA DEL try, Y ESA ES LA RAZON DE QUE ESTE AQUI. La
            // interpretacion lanza InvalidPlanDraftException, que lleva los
            // problemas pero NO la respuesta cruda. Si la variable viviera
            // dentro, el reintento se quedaria sin rejectedOutput y el modelo
            // recibiria «corrige esto» sin el esto.
            String output = null;
            List<CorrectedSession> correcciones;
            String snapshotJson;

            try {
                var anteriores = new ArrayList<>(acumulados);
                anteriores.removeAll(ultimosMotivos);
                var feedback = intento == 1 ? AiRetryFeedback.none()
                        : new AiRetryFeedback(ultimaPropuesta, ultimosMotivos, anteriores);

                var cruda = fixGenerator.pedirRespuesta(context, derivation, full, feedback, intento);
                output = cruda.output();
                snapshotJson = cruda.snapshotJson();
                correcciones = fixGenerator.interpretar(output, context);
                breaker.registrarRespuestaDelProveedor();

            } catch (AiProviderUnavailableException e) {
                // El proveedor esta caido: gastar los otros intentos solo
                // retrasa el cierre del resto de la cohorte.
                registrarFalloDeProveedor(context.userId());
                acumulados.addAll(e.problems());
                break;

            } catch (InvalidPlanDraftException e) {
                // Respondio pero incumplio el contrato. Eso NO cuenta para el
                // cortocircuito: es su capacidad, no una caida.
                breaker.registrarRespuestaDelProveedor();
                ultimaPropuesta = output;
                ultimosMotivos = List.copyOf(e.problems());
                acumulados.addAll(e.problems());
                log.warn("Intento {} para el usuario {}: la respuesta no cumple el contrato · {}",
                        intento, context.userId(), e.problems());
                continue;
            }

            var resultado = producir(context, derivation, correcciones, elegibles, exclusiones);
            if (resultado.publicable())
                return new Intento(resultado, snapshotJson, llamadas);

            // La semana que proponia no vale. Los motivos vuelven al modelo con
            // su propia propuesta, igual que un fallo de contrato.
            ultimaPropuesta = output;
            ultimosMotivos = List.copyOf(resultado.problemas());
            acumulados.addAll(resultado.problemas());
            log.warn("Intento {} para el usuario {}: la semana propuesta no pasa · {}",
                    intento, context.userId(), resultado.problemas());
        }

        log.warn("Sin correccion usable tras {} intento(s) para el usuario {}: {}",
                llamadas, context.userId(), acumulados);
        return sinPropuestaUsable(context, derivation, elegibles, exclusiones, llamadas);
    }

    /**
     * Agotados los intentos, o sin proveedor: el productor decide SIN propuesta.
     * <p>
     * Se le pasa {@code null} y no una lista vacia, y la diferencia es toda:
     * {@code null} significa «no hubo respuesta usable» y lleva al camino de
     * respaldo, que comprueba si conservar es SEGURO. Una lista vacia
     * significaria «la IA no propone nada», que es una respuesta, y conservaria
     * sin esa comprobacion. Con una sesion que causo dolor, conservar no es
     * seguro.
     */
    private Intento sinPropuestaUsable(PlanGenerationContext context,
                                       WeekDerivation derivation,
                                       Map<Long, CandidateExercise> elegibles,
                                       ExclusionesDeSeguridad exclusiones,
                                       short llamadas) {
        return new Intento(producir(context, derivation, null, elegibles, exclusiones), null, llamadas);
    }

    // =======================================================================
    // 3 · Publicar, o registrar que no se pudo
    // =======================================================================

    private Optional<WeeklyTrainingPlan> publicar(PreparedPlanGeneration prepared,
                                                  MeasuredWeekView medida,
                                                  AdaptationDecision decision,
                                                  Intento intento) {
        var resultado = intento.resultado();
        // EL MODELO SE DECLARA SEGUN EL ORIGEN DEL RESULTADO, no segun si se le
        // pregunto. Una semana que acabo CONSERVED no la genero ningun modelo
        // aunque la IA contestara, y AdaptedPublication rechaza un modelo en
        // una semana conservada: contarla como generada por IA inflaria la
        // proporcion de IA frente a reglas, que es un resultado de la tesis.
        String modelo = resultado.source() == GenerationSource.AI
                ? fixGenerator.modelName() : null;

        // El snapshot que viajo, si viajo. Si no se pidio nada, el de
        // generacion: es lo que describe sobre que se decidio conservar.
        String snapshot = intento.snapshotJson() != null
                ? intento.snapshotJson()
                : jsonSupport.write(PlanInputSnapshot.of(prepared.context()));

        var publicacion = new AdaptedPublication(resultado, decision,
                medida.measuredPlanId(), medida.weeklyMetricId(), snapshot, modelo,
                intento.llamadas(), medida.observedAdherencePct(),
                medida.registrationCoveragePct(),
                prepared.adjustment() == null ? null : prepared.adjustment().reason());

        var publicado = planCommandService.publishAdapted(prepared, publicacion);

        // SE DICE QUE SE APLICO, NO QUE SE PIDIO. La decision dice que se
        // ordeno; el informe de conservacion dice que cambio de verdad. Una
        // semana de adaptaciones sin efecto es ADAPT_LOCALIZED y CONSERVED a la
        // vez, y las dos cosas son ciertas.
        log.info("Adaptacion del usuario {} · decision {} · resultado {} · la IA {} · {}",
                prepared.command().userId(), decision, resultado.motivo(),
                resultado.motivo() == AdaptedWeekResult.Motivo.CORRECCION_IA
                        ? "propuso cambios" : "no cambio nada",
                resultado.conservacion().describe());

        return publicado;
    }

    /**
     * NO_FACTIBLE: no hay plan, pero el intento tiene que constar.
     * <p>
     * Va FUERA de la transaccion de publicacion porque esa transaccion no
     * existe: no se llamo a {@code publishAdapted}. El registrador abre una
     * nueva y nunca lanza, asi que un fallo al registrar no puede tumbar el
     * cierre del resto de la cohorte.
     * <p>
     * ESTO NO ES UN REINTENTO. Los tres intentos sirven para obtener y validar
     * una propuesta; una escritura que falle aqui se registra en el log y no se
     * repite.
     */
    private void registrarFallo(PreparedPlanGeneration prepared,
                                MeasuredWeekView medida,
                                AdaptationDecision decision,
                                Intento intento) {
        var resultado = intento.resultado();

        var evidencia = evidenceMapper.map(resultado, prepared.adjustment(), decision,
                new AdaptationEvidenceMapper.PublicationFacts(
                        prepared.command().userId(), medida.weeklyMetricId(),
                        medida.measuredPlanId(), prepared.weekStartDate(),
                        null,                  // no se publico plan
                        intento.llamadas(),    // los intentos ocurrieron: ai_calls es evidencia
                        medida.observedAdherencePct(), medida.registrationCoveragePct(),
                        jsonSupport.write(resultado.conservacion())));

        boolean registrado = failureRecorder.registrar(evidencia);

        log.error("Sin semana adaptada para el usuario {} en la del {} · decision {} ·"
                        + " motivo {} · problemas {} · evidencia {}",
                prepared.command().userId(), prepared.weekStartDate(), decision,
                resultado.motivo(), resultado.problemas(),
                registrado ? "registrada" : "NO REGISTRADA, ver el error anterior");
    }

    // =======================================================================
    // Cortacircuitos
    // =======================================================================

    private boolean proveedorDisponible(Long userId) {
        if (!breaker.abierto()) return true;
        log.warn("Cortacircuitos abierto: no se pide adaptacion para el usuario {} · {}",
                userId, breaker.describe());
        return false;
    }

    private void registrarFalloDeProveedor(Long userId) {
        if (breaker.registrarFalloDeProveedor())
            log.error("Cortacircuitos ABIERTO tras {} fallos seguidos (usuario {}). No se"
                            + " piden adaptaciones durante {} minutos, hasta {}.",
                    breaker.fallosQueAbrieron(), userId,
                    AirCircuitBreaker.DESCANSO.toMinutes(), breaker.reabrirDespuesDe());
        else
            log.warn("Replicate no respondio en la adaptacion del usuario {} ({}/{} fallos)",
                    userId, breaker.fallosSeguidos(), AirCircuitBreaker.UMBRAL);
    }
}
