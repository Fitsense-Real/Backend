package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.planning.domain.model.valueobjects.AdaptedWeekResult;
import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.ConservationReport;
import main.web.services.fitsense.planning.domain.model.valueobjects.CorrectedSession;
import main.web.services.fitsense.planning.domain.model.valueobjects.DerivedWorkout;
import main.web.services.fitsense.planning.domain.model.valueobjects.ExclusionesDeSeguridad;
import main.web.services.fitsense.planning.domain.model.valueobjects.GenerationSource;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.model.valueobjects.PasoProgresion;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanGenerationContext;
import main.web.services.fitsense.planning.domain.model.valueobjects.PreviousWeekSummary;
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

    /** P-03: la nota que deja en V32 (correcciones_del_backend) una sesion de dolor resuelta sin IA. */
    public static final String NOTA_REGENERACION_SEGURA = "REGENERACION_SEGURA: ";

    /**
     * P-03: si lo que dolio en cada sesion senalada por seguridad esta excluido con causa verificada. Con
     * dolor identificado, esos ejercicios; con dolor de sesion -o preventivo sin nada en los indices-,
     * todos los de la sesion. Ninguna sesion puede quedar sin algo que excluir: eso no es «cubierto».
     */
    static boolean cubiertoPorMemoriaDelDolor(List<Long> sesiones, PreviousWeekSummary previa,
                                             ExclusionesDeSeguridad exclusiones) {
        if (previa == null || exclusiones == null || exclusiones.vacias()) return false;
        for (var origen : sesiones) {
            var identificados = new java.util.HashSet<Long>();
            var todos = new java.util.HashSet<Long>();
            if (previa.prescriptions() != null)
                for (var p : previa.prescriptions()) {
                    if (!origen.equals(p.plannedWorkoutId()) || p.exerciseId() == null) continue;
                    todos.add(p.exerciseId());
                    if ("PAIN_OR_DISCOMFORT".equals(p.skipReason())) identificados.add(p.exerciseId());
                }
            boolean deSesion = previa.workouts() != null && previa.workouts().stream().anyMatch(w ->
                    origen.equals(w.plannedWorkoutId()) && "PAIN_OR_DISCOMFORT".equals(w.skipReason()));
            var requeridos = new java.util.HashSet<>(identificados);
            if (deSesion || identificados.isEmpty()) requeridos.addAll(todos);
            if (requeridos.isEmpty() || !requeridos.stream().allMatch(exclusiones::incluye)) return false;
        }
        return true;
    }

    /**
     * P-03: las sesiones del dolor cuya version sustituida sube las repeticiones totales o los segundos totales
     * respecto de la semana medida. Las dos magnitudes por separado, sin convertir una en otra.
     */
    static List<String> masTrabajoQueLaMedida(List<Long> sesiones, PreviousWeekSummary previa,
                                              List<DerivedWorkout> semana) {
        var problemas = new ArrayList<String>();
        for (var origen : sesiones) {
            long repsAntes = 0, segAntes = 0, repsDespues = 0, segDespues = 0;
            if (previa != null && previa.prescriptions() != null)
                for (var p : previa.prescriptions()) {
                    if (!origen.equals(p.plannedWorkoutId())) continue;
                    int series = p.sets() == null ? 1 : p.sets();
                    repsAntes += (long) series * (p.reps() == null ? 0 : p.reps());
                    segAntes += (long) series * (p.durationSeconds() == null ? 0 : p.durationSeconds());
                }
            for (var d : semana) {
                if (!origen.equals(d.sourcePlannedWorkoutId())) continue;
                for (var e : d.workout().exercises()) {
                    int series = e.plannedSets() == null ? 1 : e.plannedSets();
                    repsDespues += (long) series * (e.plannedReps() == null ? 0 : e.plannedReps());
                    segDespues += (long) series * (e.plannedDurationSeconds() == null ? 0 : e.plannedDurationSeconds());
                }
            }
            if (repsDespues > repsAntes || segDespues > segAntes)
                problemas.add("sesion %d: repeticiones %d -> %d, segundos %d -> %d".formatted(origen, repsAntes,
                        repsDespues, segAntes, segDespues));
        }
        return problemas;
    }

    /** Sin estado: el mismo calculo de V17 y V20. */
    private static final SessionDurationEstimator ESTIMADOR = new SessionDurationEstimator();

    private final SessionFixApplier applier;
    private final ConservationValidator conservation;
    private final StrictPlanValidation estricta;
    /**
     * Si V8C emite las dosis autorizadas para V24 y rechaza las subidas de la
     * IA: con la progresion encendida o con la V24 por capacidades en la
     * adaptacion (D-P11: nunca la segunda sin las reducciones autorizadas).
     */
    private final boolean autorizarDosis;

    public AdaptedWeekProducer(SessionFixApplier applier,
                               ConservationValidator conservation,
                               StrictPlanValidation estricta) {
        this(applier, conservation, estricta, false);
    }

    /** @param progresion {@code fitsense.adaptation.progression.enabled} */
    public AdaptedWeekProducer(SessionFixApplier applier,
                               ConservationValidator conservation,
                               StrictPlanValidation estricta,
                               boolean progresion) {
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
        this.autorizarDosis = progresion || estricta.validaCapacidades();
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
        return produce(context, derivation, correcciones, eligible, ExclusionesDeSeguridad.ninguna());
    }

    /**
     * Igual, reparando antes los ejercicios HEREDADOS que dejaron de ser
     * compatibles por seguridad ({@link SafetySubstitutionPolicy}). La
     * reparacion se aplica en los tres caminos -continuidad, respaldo y
     * correccion de la IA- y despues todo pasa por V8C, que solo admite esas
     * sustituciones, y por la validacion estricta. Sin sustituto, la semana es
     * NO_FACTIBLE con el motivo.
     */
    public AdaptedWeekResult produce(PlanGenerationContext context,
                                     WeekDerivation derivation,
                                     List<CorrectedSession> correcciones,
                                     Map<Long, CandidateExercise> eligible,
                                     ExclusionesDeSeguridad exclusiones) {
        return produce(context, derivation, correcciones, eligible, exclusiones, null);
    }

    /**
     * Igual, con UN PASO DE PROGRESION declarado (V34): la derivada de esa
     * sesion ya trae {@code paso.contenido()}, y V8C la compara con el paso y
     * con la semana medida. Solo vale sin orden de adaptar (continuidad,
     * {@code SIN_ORDEN}): con una orden no se acepta -una modificacion
     * funcional por ciclo- y el respaldo no lo recibe, porque RESPALDO es una
     * copia exacta.
     */
    public AdaptedWeekResult produce(PlanGenerationContext context,
                                     WeekDerivation derivation,
                                     List<CorrectedSession> correcciones,
                                     Map<Long, CandidateExercise> eligible,
                                     ExclusionesDeSeguridad exclusiones,
                                     PasoProgresion paso) {

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
            return continuidad(context, derivation, AdaptedWeekResult.Motivo.SIN_ORDEN, eligible, exclusiones,
                    paso);
        if (paso != null)
            throw new IllegalArgumentException("Con una orden de adaptar no se progresa en el mismo ciclo.");

        // Fallo de IA. Antes de conservar hay que mirar si conservar es seguro.
        if (correcciones == null)
            return respaldo(context, derivation, adjustment, eligible, exclusiones);

        var aplicada = applier.apply(derivation, correcciones, adjustment);
        // LA DURACION LA DECIDE EL CONTENIDO (correccion 4, 27/10/2026). Una
        // senalada que vuelve con los mismos ejercicios, series, repeticiones,
        // segundos y descansos no tiene por que durar otra cosa: conserva la
        // duracion ya validada y no cuenta como adaptacion. Los minutos solos no
        // son un cambio de entrenamiento.
        var normalizadas = new ArrayList<String>();
        var adaptada = new WeekDerivation(
                minutosSegunContenido(aplicada.workouts(), context, adjustment, normalizadas),
                aplicada.sourcesWithoutDerived());
        // Lo HEREDADO que dejo de ser seguro se repara tambien aqui: una sesion
        // no senalada con el ejercicio no la puede corregir la IA, porque V8C
        // le prohibe tocarla. Lo que trajo la IA no se repara: si no es
        // elegible, V1 lo rechaza.
        var reparada = SafetySubstitutionPolicy.reparar(adaptada.workouts(), context.previousWeek(),
                exclusiones, eligible, context);
        var informe = conservation.validate(
                new WeekDerivation(reparada.semana(), adaptada.sourcesWithoutDerived()),
                context.previousWeek(), adjustment, eligible, exclusiones, reparada.sustituciones(), autorizarDosis,
                estricta.divisorDeVolumen());
        if (!normalizadas.isEmpty()) informe = informe.conCorreccionesDelBackend(normalizadas);

        // El origen lo decide si la IA cambio algo DE VERDAD, no si contesto.
        // Una semana de adaptaciones sin efecto es CONSERVED aunque el modelo
        // haya devuelto sesiones: lo que se publica es la semana anterior.
        boolean huboEfecto = informe.huboAdaptacionEfectiva();
        var source = huboEfecto ? GenerationSource.AI : GenerationSource.CONSERVED;
        var motivo = huboEfecto
                ? AdaptedWeekResult.Motivo.CORRECCION_IA : AdaptedWeekResult.Motivo.SIN_EFECTO;

        return revisarYCerrar(context, reparada.semana(), informe, source, motivo, eligible,
                reparada.problemas());
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
                                       Map<Long, CandidateExercise> eligible,
                                       ExclusionesDeSeguridad exclusiones) {

        var conSeguridad = adjustment.targetedSessions().stream()
                .filter(s -> s.intervencion().exigeRevisionDeSeguridad())
                .map(PlanAdjustment.TargetedSession::plannedWorkoutId)
                .toList();

        // P-03 · VIA DETERMINISTA SEGURA (politica aprobada 31/10/2026). Si TODO lo que dolio en las sesiones
        // del dolor esta en la memoria persistente (P-02) -el ejercicio marcado, o todos los de la sesion si
        // el dolor no se identifico-, la sustitucion obligatoria SUST-SEG-1 puede resolverlo sin la IA: mismo
        // dia, mismo enfoque, mismos minutos, misma clase de slot, dosis recalculada y nunca mayor; V8C
        // comprueba que lo que dolio no vuelve y la validacion estricta revisa la semana entera. Si no
        // cuadra, NO_FACTIBLE como siempre: nunca se publica una sesion insegura.
        if (!conSeguridad.isEmpty() && cubiertoPorMemoriaDelDolor(conSeguridad, context.previousWeek(), exclusiones)) {
            var r = continuidad(context, derivation, AdaptedWeekResult.Motivo.RESPALDO_IA, eligible, exclusiones,
                    null);
            if (!r.publicable()) return r;
            // Ante dolor no se anade trabajo: ninguna sesion del dolor puede salir con mas volumen que la medida.
            var masTrabajo = masTrabajoQueLaMedida(conSeguridad, context.previousWeek(), r.semana());
            if (!masTrabajo.isEmpty())
                return AdaptedWeekResult.noFactible(AdaptedWeekResult.Motivo.RESPALDO_IA, ConservationReport.vacio(),
                        List.of("P-03: la sustitucion determinista de las sesiones %s sube el trabajo (%s); ante"
                                .formatted(conSeguridad, masTrabajo) + " dolor no se anade trabajo."));
            var notas = new ArrayList<>(r.conservacion().correccionesDelBackend());
            notas.add(NOTA_REGENERACION_SEGURA + "sesiones %s resueltas sin IA por sustitucion %s de %s"
                    .formatted(conSeguridad, SafetySubstitutionPolicy.REGLA, r.conservacion().sustituciones().stream()
                            .map(x -> x.quitado() + "->" + x.puesto()).toList()));
            return new AdaptedWeekResult(r.estado(), r.motivo(), r.semana(), r.source(),
                    r.conservacion().conCorreccionesDelBackend(notas), r.problemas());
        }

        if (!conSeguridad.isEmpty())
            return AdaptedWeekResult.noFactible(AdaptedWeekResult.Motivo.RESPALDO_IA,
                    ConservationReport.vacio(), List.of(
                            ("Sin respuesta de la IA no se puede resolver el dolor de las sesiones %s:"
                                    + " conservarlas volveria a publicar el ejercicio que dolio.")
                                    .formatted(conSeguridad)));

        // Sin dolor, se conserva la semana entera. Las senaladas quedan como
        // conservadas: la adaptacion no ocurrio, y eso es un hecho que hay que
        // registrar, no un plan invalido.
        return continuidad(context, derivation, AdaptedWeekResult.Motivo.RESPALDO_IA, eligible, exclusiones,
                null);
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
                                          Map<Long, CandidateExercise> eligible,
                                          ExclusionesDeSeguridad exclusiones,
                                          PasoProgresion paso) {

        // CARGAS QUE V11 YA NO ADMITE (correccion 3, 27/10/2026). Una carga
        // sugerida copiada tal cual puede dejar de valer: la persona falto a esa
        // sesion y no anoto peso, o anoto menos. La copia y el respaldo no la
        // podian corregir y la semana salia NO_FACTIBLE. Se retira SOLO esa
        // sugerencia, a null (nunca a cero, nunca una nueva); todo lo demas se
        // conserva y la semana se revalida entera. Con un paso de progresion no:
        // V8C exige la sesion identica al paso, y si el paso no sale, la copia
        // sin paso que se intenta despues ya pasa por aqui.
        var correcciones = new ArrayList<String>();
        var origen = paso == null ? sinCargasInvalidas(derivation.workouts(), context, correcciones)
                : derivation.workouts();

        // Conservar no es publicar lo que hoy es inseguro: lo heredado que dejo
        // de ser compatible por seguridad se sustituye, o la semana no sale.
        var reparada = SafetySubstitutionPolicy.reparar(origen, context.previousWeek(),
                exclusiones, eligible, context);
        var informe = conservation.validateContinuidad(
                new WeekDerivation(reparada.semana(), derivation.sourcesWithoutDerived()),
                context.previousWeek(), eligible, exclusiones, reparada.sustituciones(), paso, autorizarDosis);
        if (!correcciones.isEmpty()) informe = informe.conCorreccionesDelBackend(correcciones);
        return revisarYCerrar(context, reparada.semana(), informe,
                GenerationSource.CONSERVED, motivo, eligible, reparada.problemas());
    }

    /**
     * Las senaladas cuyo contenido temporal (ejercicios con su tipo, series,
     * repeticiones, segundos y descanso) es el de la semana medida, con la
     * duracion de la semana medida. Las demas, tal cual: si cambio el contenido,
     * la duracion declarada la juzga V17 contra el estimador; si es una no
     * senalada, V8C sigue rechazando que cambie sus minutos.
     */
    private static List<DerivedWorkout> minutosSegunContenido(List<DerivedWorkout> semana, PlanGenerationContext context,
                                                              PlanAdjustment adjustment, List<String> notas) {
        var medida = context.previousWeek();
        if (medida == null || medida.workouts() == null) return semana;
        var salida = new ArrayList<DerivedWorkout>();
        for (var d : semana) {
            Long origen = d.sourcePlannedWorkoutId();
            var anterior = medida.workouts().stream().filter(w -> origen.equals(w.plannedWorkoutId())).findFirst();
            var w = d.workout();
            if (adjustment.intervencionDe(origen).isEmpty() || anterior.isEmpty()) {
                salida.add(d);
                continue;
            }
            if (!contenidoTemporal(w.exercises()).equals(contenidoMedido(medida, origen))) {
                // Orden de optimizacion (31/10/2026), B2: con contenido nuevo, los minutos los calcula
                // el backend con el estimador de V17, no la IA (11 rechazos V17 de 48 predicciones).
                // Tambien cuando la IA repite los minutos de antes: el contenido cambio (U02 S3 en la
                // comparacion con IA real, 23 min declarados para 16 de contenido).
                int calculados = context.prescription() == null || w.exercises().isEmpty()
                        ? w.expectedDurationMinutes() : ESTIMADOR.estimateMinutes(w, context.prescription());
                if (calculados != w.expectedDurationMinutes()) {
                    notas.add(("DURACION: la sesion %d declaraba %d min y su contenido dura %d; se publica %d"
                            + " (la calcula el backend).").formatted(origen, w.expectedDurationMinutes(),
                            calculados, calculados));
                    salida.add(new DerivedWorkout(origen, new PlanDraft.DraftWorkout(w.scheduledDate(), w.focus(),
                            w.name(), calculados, w.exercises())));
                } else salida.add(d);
                continue;
            }
            if (w.expectedDurationMinutes() == anterior.get().expectedDurationMinutes()) {
                salida.add(d);
                continue;
            }
            int minutos = anterior.get().expectedDurationMinutes();
            notas.add(("DURACION: la sesion %d declara %d min con el mismo contenido que la semana medida;"
                    + " se conservan sus %d min (los minutos solos no son una adaptacion).")
                    .formatted(origen, w.expectedDurationMinutes(), minutos));
            salida.add(new DerivedWorkout(origen, new PlanDraft.DraftWorkout(w.scheduledDate(), w.focus(), w.name(),
                    minutos, w.exercises())));
        }
        return salida;
    }

    private static Map<String, Long> contenidoTemporal(List<PlanDraft.DraftExercise> ejercicios) {
        var m = new java.util.TreeMap<String, Long>();
        for (var e : ejercicios)
            m.merge(clave(e.exerciseId(), e.prescriptionType() == null ? null : e.prescriptionType().name(),
                    e.plannedSets(), e.plannedReps(), e.plannedDurationSeconds(), e.restSeconds()), 1L, Long::sum);
        return m;
    }

    private static Map<String, Long> contenidoMedido(PreviousWeekSummary medida, Long origen) {
        var m = new java.util.TreeMap<String, Long>();
        if (medida.prescriptions() == null) return m;
        for (var p : medida.prescriptions())
            if (origen.equals(p.plannedWorkoutId()))
                m.merge(clave(p.exerciseId(), p.prescriptionType(), p.sets(), p.reps(), p.durationSeconds(),
                        p.restSeconds()), 1L, Long::sum);
        return m;
    }

    private static String clave(Long id, String tipo, Short series, Short reps, Integer segundos, Short descanso) {
        return id + "|" + tipo + "|" + series + "|" + reps + "|" + segundos + "|" + descanso;
    }

    /** La semana con la carga sugerida a null en los ejercicios cuya carga rechaza V11, y solo en esos. */
    private static List<DerivedWorkout> sinCargasInvalidas(List<DerivedWorkout> semana, PlanGenerationContext context,
                                                           List<String> correcciones) {
        var salida = new ArrayList<DerivedWorkout>();
        for (var d : semana) {
            var w = d.workout();
            boolean cambio = false;
            var ejercicios = new ArrayList<PlanDraft.DraftExercise>();
            for (var e : w.exercises()) {
                var motivos = PlanDraftValidator.problemasDeCarga(e, context);
                if (motivos.isEmpty()) {
                    ejercicios.add(e);
                    continue;
                }
                cambio = true;
                correcciones.add("V11: carga sugerida de %s kg retirada del ejercicio %d (sesion %d) · %s"
                        .formatted(e.targetLoadKg(), e.exerciseId(), d.sourcePlannedWorkoutId(), motivos.get(0)));
                ejercicios.add(new PlanDraft.DraftExercise(e.exerciseId(), e.prescriptionType(), e.plannedSets(),
                        e.plannedReps(), e.plannedDurationSeconds(), null, e.restSeconds(), e.notes()));
            }
            salida.add(cambio ? new DerivedWorkout(d.sourcePlannedWorkoutId(), new PlanDraft.DraftWorkout(
                    w.scheduledDate(), w.focus(), w.name(), w.expectedDurationMinutes(), ejercicios)) : d);
        }
        return salida;
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
                                             Map<Long, CandidateExercise> eligible,
                                             List<String> sinSustituto) {

        // Primero lo que no tuvo sustituto seguro: es el motivo de fondo, y los
        // V1 que provoca el ejercicio que se quedo son su consecuencia.
        var problemas = new ArrayList<>(sinSustituto);
        problemas.addAll(informe.problems());
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
