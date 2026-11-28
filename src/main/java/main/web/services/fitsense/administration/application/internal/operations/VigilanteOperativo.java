package main.web.services.fitsense.administration.application.internal.operations;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import main.web.services.fitsense.shared.infrastructure.observability.ContadoresOperativos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.ToDoubleFunction;

/**
 * B5.1 (P1-01): revisa los indicadores cada {@code fitsense.operations.alerts.interval-ms} (15 min) y escribe una linea
 * {@code ALERTA_OPERATIVA tipo=... valor=... umbral=...} por cada umbral superado, en WARN. Railway (u otra
 * herramienta sobre los logs) avisa con una regla sobre {@code ALERTA_OPERATIVA}. Solo recuentos.
 * <p>
 * Ademas publica los indicadores como medidores de Micrometer ({@code fitsense.operaciones.*}). No se expone ningun
 * endpoint de metricas: {@code management.endpoints.web.exposure.include=health}. Si algun dia se expone
 * {@code /actuator/prometheus}, tiene que ir protegido.
 */
@Component
public class VigilanteOperativo {

    private static final Logger log = LoggerFactory.getLogger(VigilanteOperativo.class);

    private final IndicadoresOperativosService indicadores;
    private final boolean activado;
    private final long umbralGeneraciones;
    private final long umbralAdaptaciones;
    private final AtomicReference<IndicadoresOperativosService.Indicadores> ultima = new AtomicReference<>();
    private long correoFallidoVisto;
    private long cierresSinPlanVistos;

    public VigilanteOperativo(IndicadoresOperativosService indicadores, ObjectProvider<MeterRegistry> registro,
                              @Value("${fitsense.operations.alerts.enabled:true}") boolean activado,
                              @Value("${fitsense.operations.alerts.failed-generations-24h:3}") long umbralGeneraciones,
                              @Value("${fitsense.operations.alerts.failed-adaptations-7d:5}") long umbralAdaptaciones) {
        this.indicadores = indicadores;
        this.activado = activado;
        this.umbralGeneraciones = umbralGeneraciones;
        this.umbralAdaptaciones = umbralAdaptaciones;
        var r = registro.getIfAvailable();
        if (r != null) {
            medidor(r, "participantes_sin_plan", i -> i.participantesSinPlanVigente());
            medidor(r, "cierres_pendientes", i -> i.cierresPendientesSemanaAnterior());
            medidor(r, "cierres_fuera_de_ventana", i -> i.cierresFueraDeVentana());
            medidor(r, "generaciones_fallidas_24h", i -> i.generacionesFallidas24h());
            medidor(r, "adaptaciones_fallidas_7d", i -> i.adaptacionesFallidas7d());
            medidor(r, "operaciones_huerfanas", i -> i.operacionesHuerfanas());
            medidor(r, "cortacircuitos_generacion_abierto", i -> i.cortacircuitosGeneracionAbierto() ? 1 : 0);
            medidor(r, "cortacircuitos_adaptacion_abierto", i -> i.cortacircuitosAdaptacionAbierto() ? 1 : 0);
        }
    }

    private void medidor(MeterRegistry r, String nombre, ToDoubleFunction<IndicadoresOperativosService.Indicadores> f) {
        Gauge.builder("fitsense.operaciones." + nombre, ultima, ref -> ref.get() == null ? 0 : f.applyAsDouble(ref.get()))
                .register(r);
    }

    @Scheduled(fixedDelayString = "${fitsense.operations.alerts.interval-ms:900000}",
            initialDelayString = "${fitsense.operations.alerts.initial-delay-ms:600000}")
    public void programado() {
        if (!activado) return;
        try {
            revisar();
        } catch (RuntimeException e) {
            log.error("ALERTA_OPERATIVA tipo=VIGILANTE_FALLO causa={}", e.getClass().getSimpleName());
        }
    }

    /** Calcula, guarda la instantanea y devuelve las alertas (tambien escritas en el log). */
    public List<String> revisar() {
        var i = indicadores.calcular();
        ultima.set(i);
        var alertas = new ArrayList<String>();
        if (i.participantesSinPlanVigente() > 0) alertas.add(alerta("PARTICIPANTES_SIN_PLAN", i.participantesSinPlanVigente(), 0));
        if (i.cierresPendientesSemanaAnterior() > 0) alertas.add(alerta("CIERRE_SEMANAL_PENDIENTE", i.cierresPendientesSemanaAnterior(), 0));
        // C3: una semana que ya no se recupera sola. Requiere intervencion (04_CONTINUIDAD_CIERRE_SEMANAL.md), en ERROR.
        if (i.cierresFueraDeVentana() > 0) alertas.add(alertaGrave("CIERRE_SEMANAL_FUERA_DE_VENTANA", i.cierresFueraDeVentana()));
        if (i.generacionesFallidas24h() >= umbralGeneraciones) alertas.add(alerta("GENERACIONES_FALLIDAS_24H", i.generacionesFallidas24h(), umbralGeneraciones));
        if (i.adaptacionesFallidas7d() >= umbralAdaptaciones) alertas.add(alerta("ADAPTACIONES_FALLIDAS_7D", i.adaptacionesFallidas7d(), umbralAdaptaciones));
        if (i.operacionesHuerfanas() > 0) alertas.add(alerta("OPERACIONES_HUERFANAS", i.operacionesHuerfanas(), 0));
        if (i.cortacircuitosGeneracionAbierto()) alertas.add(alerta("CORTACIRCUITOS_GENERACION_ABIERTO", 1, 0));
        if (i.cortacircuitosAdaptacionAbierto()) alertas.add(alerta("CORTACIRCUITOS_ADAPTACION_ABIERTO", 1, 0));
        long correo = i.contadores().getOrDefault(ContadoresOperativos.CORREO_FALLIDO, 0L)
                + i.contadores().getOrDefault(ContadoresOperativos.CORREO_DESCARTADO, 0L);
        if (correo > correoFallidoVisto) alertas.add(alerta("CORREO_FALLIDO", correo - correoFallidoVisto, 0));
        correoFallidoVisto = correo;
        long sinPlan = i.contadores().getOrDefault(ContadoresOperativos.CICLO_SIN_PLAN, 0L);
        if (sinPlan > cierresSinPlanVistos) alertas.add(alerta("CIERRES_SIN_PLAN", sinPlan - cierresSinPlanVistos, 0));
        cierresSinPlanVistos = sinPlan;
        return alertas;
    }

    private static String alertaGrave(String tipo, long valor) {
        log.error("ALERTA_OPERATIVA tipo={} valor={} umbral=0 accion=GET /api/v1/admin/operations/weekly-closures/overdue",
                tipo, valor);
        return tipo;
    }

    private static String alerta(String tipo, long valor, long umbral) {
        log.warn("ALERTA_OPERATIVA tipo={} valor={} umbral={}", tipo, valor, umbral);
        return tipo;
    }
}
