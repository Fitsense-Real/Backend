package main.web.services.fitsense.orchestration.application.internal;

import main.web.services.fitsense.adaptation.interfaces.acl.AdaptationContextFacade;
import main.web.services.fitsense.adaptation.interfaces.acl.AdjustmentOrderView;
import main.web.services.fitsense.analytics.interfaces.acl.AnalyticsContextFacade;
import main.web.services.fitsense.analytics.interfaces.acl.WeeklyMetricsView;
import main.web.services.fitsense.orchestration.infrastructure.locking.CicloSemanalBloqueo;
import main.web.services.fitsense.planning.domain.model.valueobjects.AdjustmentType;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.interfaces.acl.MeasuredWeekView;
import main.web.services.fitsense.planning.interfaces.acl.PlanningContextFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * La tarea semanal de la seccion 22, para un participante:
 * <pre>
 *   1. Cerrar la semana que termino.
 *   2. Calcular sus metricas.
 *   3. Evaluar la intervencion de la semana anterior (adherence_after_pct).
 *   4. Decidir el ajuste de la semana nueva.
 *   5. Generar el plan con ese ajuste y enlazarlo a la intervencion.
 * </pre>
 * Esto NO es un bounded context: no tiene tablas ni lenguaje propio. Es un
 * process manager, y vive aparte porque la alternativa seria que uno de los
 * cuatro contextos coordinara a los otros tres, convirtiendolo en el centro de
 * un grafo de dependencias que el diseno separo a proposito.
 * <p>
 * Solo habla con facades. No conoce ningun agregado ni repositorio.
 */
@Service
public class WeeklyCycleService {

    private static final Logger log = LoggerFactory.getLogger(WeeklyCycleService.class);

    private final PlanningContextFacade planningContextFacade;
    private final AnalyticsContextFacade analyticsContextFacade;
    private final AdaptationContextFacade adaptationContextFacade;
    /** Exclusion por participante entre hilos e instancias. Null solo en pruebas que construyen a mano. */
    private final CicloSemanalBloqueo bloqueo;

    /** Para pruebas que construyen el servicio a mano: sin bloqueo. */
    public WeeklyCycleService(PlanningContextFacade planningContextFacade,
                              AnalyticsContextFacade analyticsContextFacade,
                              AdaptationContextFacade adaptationContextFacade) {
        this(planningContextFacade, analyticsContextFacade, adaptationContextFacade, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public WeeklyCycleService(PlanningContextFacade planningContextFacade,
                              AnalyticsContextFacade analyticsContextFacade,
                              AdaptationContextFacade adaptationContextFacade,
                              CicloSemanalBloqueo bloqueo) {
        this.planningContextFacade = planningContextFacade;
        this.analyticsContextFacade = analyticsContextFacade;
        this.adaptationContextFacade = adaptationContextFacade;
        this.bloqueo = bloqueo;
    }

    /**
     * El resultado explicito de un ciclo.
     * <ul>
     *   <li>{@code EJECUTADO}: el ciclo corrio; {@code planId} es el plan de la
     *       semana nueva (publicado ahora o ya existente), o vacio si no hubo.</li>
     *   <li>{@code EN_CURSO}: otro ciclo del mismo participante se esta
     *       ejecutando; este no hizo nada. Reintentar despues es seguro.</li>
     *   <li>{@code SIN_BLOQUEO}: no se pudo abrir la conexion del bloqueo; no se
     *       hizo nada para no ejecutar sin exclusion.</li>
     * </ul>
     */
    public record ResultadoCiclo(CicloSemanalBloqueo.Estado estado, Optional<Long> planId) {}

    /**
     * El ciclo con exclusion por participante (CicloSemanalBloqueo): el bloqueo se
     * toma antes de cerrar la semana y se suelta al terminar, pase lo que pase.
     * Dentro, el ciclo vuelve a comprobar si la decision ya produjo su plan.
     */
    public ResultadoCiclo ejecutar(Long userId, LocalDate measuredWeekStart, LocalDate newWeekStart) {
        if (bloqueo == null)
            return new ResultadoCiclo(CicloSemanalBloqueo.Estado.EJECUTADO,
                    cicloSinBloqueo(userId, measuredWeekStart, newWeekStart));
        var r = bloqueo.ejecutar(userId, () -> cicloSinBloqueo(userId, measuredWeekStart, newWeekStart));
        return new ResultadoCiclo(r.estado(), r.valor() == null ? Optional.empty() : r.valor());
    }

    /**
     * Ejecuta el ciclo semanal de un participante.
     * <p>
     * SIN @Transactional, deliberadamente. Cada paso llama a un facade que abre
     * su propia transaccion corta, asi que la atomicidad por paso ya esta
     * garantizada. Una transaccion abarcando el ciclo entero mantendria la
     * conexion abierta durante los ~90 s que tarda la generacion, y Postgres la
     * termina por idle-in-transaction llevandose el cierre de semana y las
     * metricas ya escritas.
     * <p>
     * El noRollbackFor que habia aqui trataba el sintoma: si no hay transaccion
     * abarcando, no hay nada que marcar como rollback-only.
     */
    public Optional<Long> runFor(Long userId, LocalDate measuredWeekStart, LocalDate newWeekStart) {
        // Vacio tambien con EN_CURSO o SIN_BLOQUEO: no se publico nada en esta llamada.
        return ejecutar(userId, measuredWeekStart, newWeekStart).planId();
    }

    private Optional<Long> cicloSinBloqueo(Long userId, LocalDate measuredWeekStart, LocalDate newWeekStart) {
        // 1. Cerrar. El plan deja de admitir sesiones nuevas.
        planningContextFacade.closeWeek(userId, measuredWeekStart);

        // 2. Medir. Idempotente: recalcular sobrescribe la misma fila.
        var metrics = analyticsContextFacade.calculateWeek(userId, measuredWeekStart);

        // 3. Cerrar el circulo de la intervencion ANTERIOR antes de decidir la
        //    nueva: la adherencia recien calculada es justamente el resultado
        //    del ajuste que se aplico la semana pasada.
        metrics.ifPresent(view ->
                adaptationContextFacade.recordOutcome(view.planId(), view.weightedAdherencePct()));

        // 4. Decidir. Vacio en la primera semana o si no hubo plan.
        var order = adaptationContextFacade.decideForWeek(userId, measuredWeekStart);

        // 5. Generar, UNA vez por semana medida. Si la orden de esta semana ya
        //    produjo su plan, el ciclo se esta repitiendo (otra instancia, un
        //    relanzamiento manual): adaptar otra vez publicaria una segunda
        //    version de la semana nueva y otra fila de evidencia por la misma
        //    decision. Se devuelve el plan que ya salio.
        var yaPublicado = order.flatMap(view -> adaptationContextFacade.resultingPlanOf(view.interventionId()));
        if (yaPublicado.isPresent()) {
            log.info("Ciclo semanal repetido para el usuario {} y la semana medida {}: el plan {} ya se"
                    + " publico, no se adapta otra vez", userId, measuredWeekStart, yaPublicado.get());
            return yaPublicado;
        }

        // Sin orden (primera semana medida o semana sin plan) puede que la
        // semana nueva YA tenga plan: el primero se genera para el lunes
        // siguiente cuando se pide a mitad de semana. Se respeta.
        if (order.isEmpty()) {
            var existente = planningContextFacade.findActivePlanId(userId, newWeekStart);
            if (existente.isPresent()) return existente;
        }

        var adjustment = order.map(WeeklyCycleService::toAdjustment).orElse(PlanAdjustment.none());


        var previousAdherence = metrics.map(view -> view.weightedAdherencePct()).orElse(null);
        var previousRpe = metrics.map(view -> view.averageSessionRpe()).orElse(null);

        Optional<Long> planId;
        try {
            // LA SEMANA SE DERIVA DE LA ANTERIOR EN CUANTO HAY UNA.
            //
            // decideForWeek devuelve vacio exactamente cuando no hay semana
            // medida con plan: primera semana del participante, o una semana
            // sin plan. Ese es el unico caso de generacion inicial; a partir de
            // ahi la semana nueva se copia de la anterior y se corrige solo lo
            // que fallo, que es la direccion aprobada -«solo se adapta cuando
            // hay baja adherencia; si no, no se hace nada»-.
            //
            // Con NO_CHANGE o INSUFFICIENT_DATA no se le pide nada a la IA: el
            // productor conserva la continuidad y publica igual, porque la
            // semana nueva necesita un plan aunque no se cambie nada de el.
            planId = order.isPresent()
                    ? planningContextFacade.adaptWeeklyPlan(userId, newWeekStart, adjustment,
                    previousAdherence, previousRpe, medidaDe(metrics, order.get()))
                    : planningContextFacade.generateWeeklyPlan(userId, newWeekStart, adjustment,
                    previousAdherence, previousRpe);
        } catch (RuntimeException e) {
            // 19.4: si no hay plan valido la semana queda sin plan y su
            // adherencia sera NULL. Es un dato del estudio, pero no puede tumbar
            // el cierre del resto de la cohorte.
            log.error("No se pudo generar el plan del usuario {} para la semana del {}: {}",
                    userId, newWeekStart, e.getMessage());
            return Optional.empty();
        }

        order.ifPresent(view -> planId.ifPresent(
                id -> adaptationContextFacade.linkResultingPlan(view.interventionId(), id, newWeekStart)));

        return planId;
    }

    /**
     * Los cuatro datos de la semana medida que planning necesita para la
     * evidencia de V32, y que no puede consultar: no tiene ACL hacia analytics.
     * <p>
     * Si no hubiera metricas no habria orden -{@code decideForWeek} las usa
     * para decidir-, asi que llegar aqui sin ellas significaria que el paso 2 y
     * el 4 del ciclo discrepan. Se dice en voz alta en vez de registrar una
     * evidencia con identificadores inventados.
     */
    private static MeasuredWeekView medidaDe(Optional<WeeklyMetricsView> metrics,
                                             AdjustmentOrderView order) {
        var vista = metrics.orElseThrow(() -> new IllegalStateException(
                "Hay orden de ajuste y no hay metricas de la semana medida."));

        return new MeasuredWeekView(
                vista.weeklyMetricId(),
                vista.planId(),
                vista.observedAdherencePct(),
                vista.registrationCoveragePct(),
                order.outcome());
    }

    /**
     * La vista del ACL traducida a lo que entiende planning.
     * <p>
     * VISIBILIDAD DE PAQUETE, no privado, para que {@code WeeklyCycleAdjustmentTest}
     * pueda comprobar la regla de abajo sin montar los tres facades. Es la unica
     * razon: no lo llama nadie mas.
     * <p>
     * LOS DOS MECANISMOS NO PUEDEN VIAJAR JUNTOS. {@code PlanAdjustment} lanza
     * si recibe {@code targetVolume != null} con sesiones senaladas -son
     * irreconciliables: con la dosis fijada por capacidad en 4D, nada puede
     * mover el volumen hasta un objetivo porcentual-. Y la trampa es que
     * {@code AdjustmentOrderView.targetVolume()} es un {@code int} PRIMITIVO:
     * al pasarlo a un parametro {@code Integer} se autoboxea, asi que
     * <b>incluso un cero llega como no nulo</b> y dispararia esa excepcion.
     * <p>
     * Por eso los tres campos de volumen solo viajan cuando el mecanismo
     * porcentual ACTUO de verdad, y eso se reconoce por los tipos: si lo unico
     * que hay es {@code NONE}, no hubo ajuste porcentual y el volumen no
     * significa nada.
     * <p>
     * ESA CONDICION NO ES COSMETICA. Desde que la tabla de decision porcentual
     * salio del flujo, los tipos son {@code NONE} y el volumen es 0 SIEMPRE.
     * Pasar ese cero autoboxeado haria que {@code PlanAdjustment.isActive()}
     * -que es literalmente {@code targetVolume != null}- devolviera cierto cada
     * semana, y {@code persist} marcaria cada plan con
     * {@code adjustment_applied = "NONE"} como si se le hubiera aplicado un
     * ajuste. Con sesiones senaladas, ademas, lanzaria.
     */
    static PlanAdjustment toAdjustment(AdjustmentOrderView order) {
        List<AdjustmentType> types = order.adjustmentTypes().stream()
                .map(AdjustmentType::valueOf)
                .toList();

        List<PlanAdjustment.TargetedSession> senaladas = order.targetedSessions().stream()
                .map(s -> new PlanAdjustment.TargetedSession(
                        s.plannedWorkoutId(), s.intervencion()))
                .toList();

        boolean porcentual = types.stream().anyMatch(tipo -> tipo != AdjustmentType.NONE);

        return new PlanAdjustment(
                types,
                porcentual ? (Integer) order.targetVolume() : null,
                porcentual ? (Integer) order.targetVolumeMin() : null,
                porcentual ? (Integer) order.targetVolumeMax() : null,
                porcentual ? order.targetVolumeChangePct() : 0.0,
                porcentual ? order.loadChangePct() : 0.0,
                order.forcedMaxDifficulty(),
                order.forcedDaysPerWeek(),
                order.forcedSessionMinutes(),
                order.reason(),
                order.dominantSkipReason(),
                order.distributionHint(),
                senaladas);
    }
}