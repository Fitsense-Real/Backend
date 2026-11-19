package main.web.services.fitsense.planning.application.internal.generation;

import main.web.services.fitsense.planning.application.internal.commandservices.PlanGenerationPipeline;
import main.web.services.fitsense.planning.infrastructure.generation.ai.ReplicateProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * V59 · POLITICA DE TIEMPOS de la generacion asincrona, comprobada al arrancar.
 * <p>
 * Distingue tiempos que #71 trataba como uno solo:
 * <ol>
 *   <li><b>Una prediccion</b>: {@code fitsense.ai.*} (POST corto, sondeo de {@code timeout-seconds}, cancelacion).
 *   Cota por intento: {@link ReplicateProperties#esperaMaximaConCancelacionSegundos()} (~331 s).</li>
 *   <li><b>El pipeline</b>: hasta {@link PlanGenerationPipeline#AI_ATTEMPTS} intentos mas validacion, respaldo y
 *   publicacion (margen fijo de {@link #MARGEN_PIPELINE_S} s). Peor caso ~1.113 s con la configuracion actual.</li>
 *   <li><b>Liveness del worker</b>: latido cada {@code heartbeat-ms} y lease de {@code lease-seconds}. Solo un lease
 *   VENCIDO indica un proceso muerto.</li>
 *   <li><b>La operacion</b>: {@code max-processing-seconds} desde la PRIMERA ejecucion. Red de seguridad, nunca por
 *   debajo del peor caso del pipeline: si lo estuviera, se descartarian resultados ya pagados.</li>
 * </ol>
 * Una configuracion incoherente NO arranca: es preferible a descartar en silencio predicciones facturadas o a tomar
 * por muerta una generacion viva.
 */
@Component
public class PoliticaTiemposGeneracion {

    private static final Logger log = LoggerFactory.getLogger(PoliticaTiemposGeneracion.class);

    /** Preparacion, validaciones, respaldo determinista y publicacion: decenas de ms medidos; margen holgado. */
    public static final long MARGEN_PIPELINE_S = 120;

    private final long peorCasoPipelineSegundos;

    public PoliticaTiemposGeneracion(ReplicateProperties ia,
                                     @Value("${fitsense.plan-generation.max-processing-seconds:1800}") long maxProcessingSeconds,
                                     @Value("${fitsense.plan-generation.lease-seconds:90}") long leaseSeconds,
                                     @Value("${fitsense.plan-generation.heartbeat-ms:20000}") long heartbeatMs) {
        this.peorCasoPipelineSegundos = peorCaso(ia);
        comprobar(ia.isUsable(), peorCasoPipelineSegundos, maxProcessingSeconds, leaseSeconds, heartbeatMs);
        log.info("Generacion asincrona: prediccion <= {} s, pipeline <= {} s, latido {} ms, lease {} s, operacion <= {} s"
                        + " (IA {})", ia.esperaMaximaConCancelacionSegundos(), peorCasoPipelineSegundos, heartbeatMs,
                leaseSeconds, maxProcessingSeconds, ia.isUsable() ? "encendida" : "apagada");
    }

    static long peorCaso(ReplicateProperties ia) {
        return PlanGenerationPipeline.AI_ATTEMPTS * ia.esperaMaximaConCancelacionSegundos() + MARGEN_PIPELINE_S;
    }

    /** Lanza si la politica no es coherente. Estatica para probarla sin contexto. */
    static void comprobar(boolean iaEncendida, long peorCasoPipeline, long maxProcessing, long lease, long heartbeatMs) {
        if (heartbeatMs <= 0 || lease <= 0)
            throw new IllegalStateException("fitsense.plan-generation: heartbeat-ms y lease-seconds deben ser positivos");
        // Al menos dos latidos por lease: un latido perdido (GC, red) no basta para declarar muerto al worker.
        if (heartbeatMs * 2 > lease * 1000)
            throw new IllegalStateException(("fitsense.plan-generation.lease-seconds=%d no deja dos latidos de %d ms:"
                    + " una generacion viva se tomaria por huerfana").formatted(lease, heartbeatMs));
        if (iaEncendida && maxProcessing < peorCasoPipeline)
            throw new IllegalStateException(("fitsense.plan-generation.max-processing-seconds=%d es menor que el peor"
                    + " caso del pipeline con IA (%d s): se descartarian resultados validos ya pagados. Subelo o acota"
                    + " fitsense.ai.timeout-seconds").formatted(maxProcessing, peorCasoPipeline));
    }

    public long peorCasoPipelineSegundos() {
        return peorCasoPipelineSegundos;
    }
}
