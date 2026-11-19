package main.web.services.fitsense.orchestation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Control del consumo REAL de {@link EvaluacionK20IT} en modo REPLICATE (DEV-R): coste por tokens con la tarifa
 * declarada, identificador de la prediccion desde la linea REPLICATE_TIMING y lectura de metricas (consulta gratuita).
 */
@DisplayName("DEV-R: control del consumo real de Replicate por tokens")
class PresupuestoReplicateK20Test {

    @Test
    @DisplayName("coste = tokens de entrada x 0,25 + tokens de salida x 2,00 USD por millon (tarifa por defecto)")
    void coste() {
        assertEquals(0.25, EvaluacionK20IT.costeUsd(1_000_000, 0), 1e-12);
        assertEquals(2.00, EvaluacionK20IT.costeUsd(0, 1_000_000), 1e-12);
        // La prediccion mas cara observada el 03/11/2026 (68.186 de entrada, 3.672 de salida): ~0,0244 USD.
        assertEquals(68_186 * 0.25e-6 + 3_672 * 2e-6, EvaluacionK20IT.costeUsd(68_186, 3_672), 1e-12);
        assertTrue(EvaluacionK20IT.costeUsd(68_186, 5_260) < EvaluacionK20IT.COSTE_MAX_POR_LLAMADA_USD,
                "el peor caso observado cabe en la reserva de 0,05 USD");
    }

    @Test
    @DisplayName("el identificador sale de la linea REPLICATE_TIMING; 'desconocido' no cuenta")
    void identificador() {
        assertEquals("abc123xyz", EvaluacionK20IT.idPrediccion("REPLICATE_TIMING user=7 week=1 attempt=2"
                + " prediction_id=abc123xyz post_ms=10 polling_ms=0 polling_requests=0 total_ms=10 resultado=SUCCEEDED"));
        assertNull(EvaluacionK20IT.idPrediccion("REPLICATE_TIMING user=7 week=1 attempt=2 prediction_id=desconocido"
                + " post_ms=10"));
    }

    @Test
    @DisplayName("P-H1: por omision (BLOQUEAR) una prediccion sin metricas bloquea; con RESERVA_SIN_BLOQUEO solo"
            + " deja seguir si fallo en el proveedor")
    void sinMetricas() {
        assertTrue(EvaluacionK20IT.bloqueaSinMetricas("BLOQUEAR", true), "protocolo congelado: bloquea siempre");
        assertTrue(EvaluacionK20IT.bloqueaSinMetricas("BLOQUEAR", false));
        assertFalse(EvaluacionK20IT.bloqueaSinMetricas("RESERVA_SIN_BLOQUEO", true),
                "fallida sin respuesta: se carga la reserva y la comprobacion previa mantiene la cota");
        assertTrue(EvaluacionK20IT.bloqueaSinMetricas("RESERVA_SIN_BLOQUEO", false),
                "respondio sin metricas: su coste no esta acotado por la reserva, bloquea");
        assertTrue(EvaluacionK20IT.bloqueaSinMetricas("otra", true), "valor desconocido: el comportamiento seguro");
        assertEquals("BLOQUEAR", EvaluacionK20IT.SIN_METRICAS, "por omision, el protocolo congelado");
    }

    @Test
    @EnabledIfSystemProperty(named = "fitsense.evaluacion.k20.metricas-de", matches = ".+")
    @DisplayName("opt-in: lee las metricas reales de una prediccion EXISTENTE (GET gratuito, ninguna prediccion nueva)")
    void metricasReales() {
        var m = EvaluacionK20IT.metricasDe(System.getProperty("fitsense.evaluacion.k20.metricas-de"));
        assertNotNull(m, "metricas no disponibles");
        assertTrue(m.path("token_input_count").asLong() > 0);
        assertTrue(m.path("token_output_count").asLong() > 0);
        System.out.println("METRICAS " + m + " usd=" + EvaluacionK20IT.costeUsd(m.path("token_input_count").asLong(),
                m.path("token_output_count").asLong()));
    }
}
