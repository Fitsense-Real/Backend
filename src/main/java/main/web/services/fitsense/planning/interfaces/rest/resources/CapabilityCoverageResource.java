package main.web.services.fitsense.planning.interfaces.rest.resources;

import java.util.List;
import java.util.Map;

/**
 * Que capacidades del objetivo cubre la semana (4A v6 + 4D v4).
 *
 * @param result             COMPLETO o PARCIAL (una semana NO_FACTIBLE no se publica)
 * @param missingPrimaries   principales que faltan, con su motivo
 * @param requiresAcceptance true si la semana es PARCIAL: es una alternativa, no
 *                           el plan completo del objetivo
 * @param message            el aviso para mostrar; null si es completa
 */
public record CapabilityCoverageResource(
        String result,
        List<String> coveredPrimaries,
        Map<String, String> missingPrimaries,
        List<String> includedSecondaries,
        boolean requiresAcceptance,
        String message
) {}
